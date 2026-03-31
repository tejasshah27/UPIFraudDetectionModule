import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FraudAnalysisResponse } from '../models/transaction.model';

@Component({
  selector: 'app-rule-breakdown-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)" id="rule-breakdown-modal">
      <div class="modal-panel" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div>
            <div class="modal-title">🔍 Rule Breakdown</div>
            <div class="fs-xs text-muted mt-1">
              <span class="font-mono">{{ transaction.txId }}</span>
            </div>
          </div>
          <button class="modal-close" (click)="close.emit()" id="btn-modal-close">✕</button>
        </div>
        <div class="modal-body">

          <!-- Summary Row -->
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:1rem;margin-bottom:1.5rem">
            <div style="text-align:center">
              <div class="fs-xs text-muted mb-1" style="text-transform:uppercase;letter-spacing:.06em">Risk Score</div>
              <div style="font-size:2rem;font-weight:800;letter-spacing:-.04em"
                   [style.color]="getRiskColor(transaction.riskScore)">
                {{ (transaction.riskScore * 100).toFixed(1) }}%
              </div>
            </div>
            <div style="text-align:center">
              <div class="fs-xs text-muted mb-1" style="text-transform:uppercase;letter-spacing:.06em">Decision</div>
              <div style="margin-top:.5rem">
                <span class="badge" [ngClass]="getDecisionClass(transaction.decision)"
                      style="font-size:.85rem;padding:5px 14px">
                  {{ transaction.decision }}
                </span>
              </div>
            </div>
            <div style="text-align:center">
              <div class="fs-xs text-muted mb-1" style="text-transform:uppercase;letter-spacing:.06em">Engine</div>
              <div style="margin-top:.5rem">
                <span class="badge badge-engine" style="font-size:.8rem;padding:5px 12px">
                  {{ transaction.scoringEngine }}
                </span>
              </div>
            </div>
          </div>

          <!-- Reason Tags -->
          <div *ngIf="transaction.reasons && transaction.reasons.length > 0" style="margin-bottom:1.25rem">
            <div class="breakdown-label">Triggered Reason Codes</div>
            <div class="d-flex flex-wrap gap-2">
              <span *ngFor="let r of transaction.reasons"
                    class="badge badge-reason"
                    style="background:rgba(239,68,68,.1);color:#fca5a5;border-color:rgba(239,68,68,.25)">
                ⚠ {{ r }}
              </span>
            </div>
          </div>

          <!-- Rule Breakdown Grid -->
          <div *ngIf="transaction.ruleBreakdown && transaction.ruleBreakdown.length > 0">
            <div class="breakdown-label">Rule Evaluation Results</div>
            <div class="breakdown-grid">
              <div *ngFor="let rb of transaction.ruleBreakdown"
                   class="breakdown-item"
                   [class.triggered]="rb.triggered">
                <div class="d-flex align-items-center gap-2">
                  <span style="font-size:.9rem">{{ rb.triggered ? '🔴' : '🟢' }}</span>
                  <span class="breakdown-item-rule">{{ rb.rule }}</span>
                </div>
                <div class="breakdown-item-detail">{{ rb.detail }}</div>
              </div>
            </div>
          </div>

          <!-- Empty state -->
          <div *ngIf="!transaction.ruleBreakdown || transaction.ruleBreakdown.length === 0"
               style="text-align:center;padding:2rem;color:var(--text-muted)">
            No rule breakdown available.
          </div>

        </div>
      </div>
    </div>
  `
})
export class RuleBreakdownModalComponent {
  @Input() transaction!: FraudAnalysisResponse;
  @Output() close = new EventEmitter<void>();

  onOverlayClick(event: Event): void {
    this.close.emit();
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
