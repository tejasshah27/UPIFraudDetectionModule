import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../core/services/transaction.service';
import { FraudAnalysisResponse, TransactionRequest } from '../../shared/models/transaction.model';
import { RuleBreakdownModalComponent } from '../../shared/components/rule-breakdown-modal.component';

@Component({
  selector: 'app-analyze',
  standalone: true,
  imports: [CommonModule, FormsModule, RuleBreakdownModalComponent],
  templateUrl: './analyze.component.html'
})
export class AnalyzeComponent {
  private txService = inject(TransactionService);

  form: TransactionRequest = {
    senderId: 'USER_001234',
    receiverId: 'USER_056789',
    amount: 25000,
    txType: 'P2P',
    mccCode: '5411',
    ipAddress: '192.168.1.100',
    city: 'Mumbai',
    currency: 'INR'
  };

  result: FraudAnalysisResponse | null = null;
  isLoading = false;
  errorMessage = '';
  showModal = false;

  readonly txTypes = ['P2P', 'P2M', 'BILL_PAY', 'RECHARGE'];
  readonly cities = ['Mumbai', 'Delhi', 'Bangalore', 'Hyderabad', 'Chennai', 'Kolkata', 'Pune', 'Ahmedabad'];

  analyze(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.result = null;
    this.txService.analyze(this.form).subscribe({
      next: (data) => { this.result = data; this.isLoading = false; },
      error: (err) => { this.errorMessage = err.message || 'Analysis failed'; this.isLoading = false; }
    });
  }

  resetForm(): void {
    this.result = null;
    this.errorMessage = '';
  }

  getRiskColor(score: number): string {
    if (score < 0.30) return 'var(--accent-green)';
    if (score < 0.70) return 'var(--accent-orange)';
    return 'var(--accent-red)';
  }

  getDecisionClass(decision: string): string {
    return `badge-${decision.toLowerCase()}`;
  }
}
