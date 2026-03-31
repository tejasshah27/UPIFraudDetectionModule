import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../core/services/transaction.service';
import { FraudAnalysisResponse, TransactionRequest } from '../../shared/models/transaction.model';
import { RuleBreakdownModalComponent } from '../../shared/components/rule-breakdown-modal.component';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RuleBreakdownModalComponent],
  templateUrl: './transaction-list.component.html'
})
export class TransactionListComponent {
  private txService = inject(TransactionService);

  userId = '';
  transactions: FraudAnalysisResponse[] = [];
  selectedTx: FraudAnalysisResponse | null = null;
  isLoading = false;
  errorMessage = '';

  loadTransactions(): void {
    if (!this.userId.trim()) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.txService.getUserTransactions(this.userId.trim()).subscribe({
      next: (data) => { this.transactions = data; this.isLoading = false; },
      error: (err) => { this.errorMessage = err.message || 'Failed to load'; this.isLoading = false; }
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

  trackById(_: number, tx: FraudAnalysisResponse): string {
    return tx.txId;
  }
}
