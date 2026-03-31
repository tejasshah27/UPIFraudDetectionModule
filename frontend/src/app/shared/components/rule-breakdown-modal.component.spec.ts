import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RuleBreakdownModalComponent } from './rule-breakdown-modal.component';
import { FraudAnalysisResponse } from '../models/transaction.model';

const MOCK_TRANSACTION: FraudAnalysisResponse = {
  txId: 'TXN_modal_001',
  riskScore: 0.85,
  decision: 'REJECT',
  scoringEngine: 'ONNX_ML',
  reasons: ['velocity_breach', 'new_city'],
  ruleBreakdown: [
    { rule: 'VelocityCheck', triggered: true, detail: '6 tx in last 10 min' },
    { rule: 'LocationAnomaly', triggered: true, detail: 'New city: Delhi (usual: Mumbai)' },
    { rule: 'AmountThreshold', triggered: false, detail: 'Within normal limits' },
    { rule: 'OddHour', triggered: false, detail: 'Transaction at normal hours' }
  ]
};

const LOW_RISK_TRANSACTION: FraudAnalysisResponse = {
  txId: 'TXN_modal_002',
  riskScore: 0.10,
  decision: 'ACCEPT',
  scoringEngine: 'ONNX_ML',
  reasons: [],
  ruleBreakdown: []
};

describe('RuleBreakdownModalComponent', () => {
  let component: RuleBreakdownModalComponent;
  let fixture: ComponentFixture<RuleBreakdownModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuleBreakdownModalComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RuleBreakdownModalComponent);
    component = fixture.componentInstance;

    // Provide a default @Input value before detectChanges
    component.transaction = MOCK_TRANSACTION;
    fixture.detectChanges();
  });

  // ── Creation ──────────────────────────────────────────────────────────────
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── @Input() transaction ──────────────────────────────────────────────────
  describe('@Input() transaction', () => {
    it('should accept a FraudAnalysisResponse as input', () => {
      expect(component.transaction).toEqual(MOCK_TRANSACTION);
    });

    it('should reflect updated input binding', () => {
      component.transaction = LOW_RISK_TRANSACTION;
      fixture.detectChanges();
      expect(component.transaction.txId).toBe('TXN_modal_002');
      expect(component.transaction.decision).toBe('ACCEPT');
    });

    it('should expose the txId from the transaction', () => {
      expect(component.transaction.txId).toBe('TXN_modal_001');
    });

    it('should expose the ruleBreakdown array', () => {
      expect(component.transaction.ruleBreakdown.length).toBe(4);
    });

    it('should expose the reasons array', () => {
      expect(component.transaction.reasons).toContain('velocity_breach');
    });
  });

  // ── @Output() close ───────────────────────────────────────────────────────
  describe('@Output() close', () => {
    it('should emit close when onOverlayClick is called', () => {
      const closeSpy = jasmine.createSpy('closeSpy');
      component.close.subscribe(closeSpy);

      const event = new MouseEvent('click');
      component.onOverlayClick(event);

      expect(closeSpy).toHaveBeenCalled();
    });

    it('should emit close when close.emit() is called directly', () => {
      const closeSpy = jasmine.createSpy('closeSpy');
      component.close.subscribe(closeSpy);

      component.close.emit();

      expect(closeSpy).toHaveBeenCalled();
    });
  });

  // ── onOverlayClick() ──────────────────────────────────────────────────────
  describe('onOverlayClick()', () => {
    it('should emit the close event', () => {
      let emitted = false;
      component.close.subscribe(() => (emitted = true));

      component.onOverlayClick(new MouseEvent('click'));

      expect(emitted).toBeTrue();
    });
  });

  // ── getRiskColor() ────────────────────────────────────────────────────────
  describe('getRiskColor()', () => {
    it('should return green for score < 0.30', () => {
      expect(component.getRiskColor(0.0)).toBe('var(--accent-green)');
      expect(component.getRiskColor(0.15)).toBe('var(--accent-green)');
      expect(component.getRiskColor(0.29)).toBe('var(--accent-green)');
    });

    it('should return orange for score between 0.30 and 0.70', () => {
      expect(component.getRiskColor(0.30)).toBe('var(--accent-orange)');
      expect(component.getRiskColor(0.50)).toBe('var(--accent-orange)');
      expect(component.getRiskColor(0.699)).toBe('var(--accent-orange)');
    });

    it('should return red for score >= 0.70', () => {
      expect(component.getRiskColor(0.70)).toBe('var(--accent-red)');
      expect(component.getRiskColor(0.85)).toBe('var(--accent-red)');
      expect(component.getRiskColor(1.0)).toBe('var(--accent-red)');
    });
  });

  // ── getDecisionClass() ────────────────────────────────────────────────────
  describe('getDecisionClass()', () => {
    it('should return badge-accept for ACCEPT', () => {
      expect(component.getDecisionClass('ACCEPT')).toBe('badge-accept');
    });

    it('should return badge-review for REVIEW', () => {
      expect(component.getDecisionClass('REVIEW')).toBe('badge-review');
    });

    it('should return badge-reject for REJECT', () => {
      expect(component.getDecisionClass('REJECT')).toBe('badge-reject');
    });

    it('should lowercase the decision value', () => {
      expect(component.getDecisionClass('ACCEPT')).toBe('badge-accept');
    });
  });
});
