import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TransactionService } from '../../core/services/transaction.service';
import { FraudAnalysisResponse } from '../../shared/models/transaction.model';
import { RuleBreakdownModalComponent } from '../../shared/components/rule-breakdown-modal.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RuleBreakdownModalComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private txService = inject(TransactionService);

  transactions: FraudAnalysisResponse[] = [];
  isGenerating = false;
  generateCount = 50;
  errorMessage = '';
  selectedTx: FraudAnalysisResponse | null = null;

  ngOnInit(): void {}

  get totalCount()  { return this.transactions.length; }
  get acceptCount() { return this.transactions.filter(t => t.decision === 'ACCEPT').length; }
  get reviewCount() { return this.transactions.filter(t => t.decision === 'REVIEW').length; }
  get rejectCount() { return this.transactions.filter(t => t.decision === 'REJECT').length; }
  get avgRisk()     { return this.totalCount ? this.transactions.reduce((s, t) => s + t.riskScore, 0) / this.totalCount : 0; }

  get acceptFlex()  { return this.totalCount ? this.acceptCount / this.totalCount * 100 : 33; }
  get reviewFlex()  { return this.totalCount ? this.reviewCount / this.totalCount * 100 : 34; }
  get rejectFlex()  { return this.totalCount ? this.rejectCount / this.totalCount * 100 : 33; }

  generateData(): void {
    this.isGenerating = true;
    this.errorMessage = '';
    this.txService.generateTransactions(this.generateCount).subscribe({
      next: (data) => {
        this.transactions = data;
        this.isGenerating = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to connect to backend: ' + (err.message || 'Unknown error');
        this.isGenerating = false;
      }
    });
  }

  openModal(tx: FraudAnalysisResponse, event: Event): void {
    event.stopPropagation();
    this.selectedTx = tx;
  }

  closeModal(): void {
    this.selectedTx = null;
  }

  getRiskColor(score: number): string {
    if (score < 0.30) return 'var(--accent-green)';
    if (score < 0.70) return 'var(--accent-orange)';
    return 'var(--accent-red)';
  }

  getDecisionClass(decision: string): string {
    return `badge-${decision.toLowerCase()}`;
  }

  getAvgRiskColor(): string {
    return this.getRiskColor(this.avgRisk);
  }

  trackById(_: number, tx: FraudAnalysisResponse): string {
    return tx.txId;
  }
}
