import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AnalyzeComponent } from './analyze.component';
import { TransactionService } from '../../core/services/transaction.service';
import { FraudAnalysisResponse } from '../../shared/models/transaction.model';

const MOCK_RESPONSE: FraudAnalysisResponse = {
  txId: 'TXN_analyze_001',
  riskScore: 0.12,
  decision: 'ACCEPT',
  scoringEngine: 'ONNX_ML',
  reasons: [],
  ruleBreakdown: [
    { rule: 'AmountThreshold', triggered: false, detail: 'Within limits' }
  ]
};

const REJECT_RESPONSE: FraudAnalysisResponse = {
  txId: 'TXN_analyze_002',
  riskScore: 0.92,
  decision: 'REJECT',
  scoringEngine: 'ONNX_ML',
  reasons: ['velocity_breach'],
  ruleBreakdown: [
    { rule: 'VelocityCheck', triggered: true, detail: '7 tx in last 10 min' }
  ]
};

describe('AnalyzeComponent', () => {
  let component: AnalyzeComponent;
  let fixture: ComponentFixture<AnalyzeComponent>;
  let txServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    txServiceSpy = jasmine.createSpyObj<TransactionService>('TransactionService', ['analyze']);

    await TestBed.configureTestingModule({
      imports: [AnalyzeComponent],
      providers: [
        { provide: TransactionService, useValue: txServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyzeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── Creation ──────────────────────────────────────────────────────────────
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have a pre-filled form with default values', () => {
    expect(component.form.senderId).toBeTruthy();
    expect(component.form.amount).toBeGreaterThan(0);
    expect(component.form.txType).toBeTruthy();
    expect(component.form.city).toBeTruthy();
  });

  it('should start with result = null', () => {
    expect(component.result).toBeNull();
  });

  it('should start with isLoading = false', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('should start with no error message', () => {
    expect(component.errorMessage).toBe('');
  });

  it('should start with showModal = false', () => {
    expect(component.showModal).toBeFalse();
  });

  it('should expose txTypes list containing all four types', () => {
    expect(component.txTypes).toContain('P2P');
    expect(component.txTypes).toContain('P2M');
    expect(component.txTypes).toContain('BILL_PAY');
    expect(component.txTypes).toContain('RECHARGE');
  });

  it('should expose a non-empty cities list', () => {
    expect(component.cities.length).toBeGreaterThan(0);
    expect(component.cities).toContain('Mumbai');
  });

  // ── analyze() ─────────────────────────────────────────────────────────────
  describe('analyze()', () => {
    it('should call transactionService.analyze with the current form data', fakeAsync(() => {
      txServiceSpy.analyze.and.returnValue(of(MOCK_RESPONSE));
      component.analyze();
      tick();
      expect(txServiceSpy.analyze).toHaveBeenCalledWith(component.form);
    }));

    it('should set result on success', fakeAsync(() => {
      txServiceSpy.analyze.and.returnValue(of(MOCK_RESPONSE));
      component.analyze();
      tick();
      expect(component.result).toEqual(MOCK_RESPONSE);
      expect(component.isLoading).toBeFalse();
    }));

    it('should clear result and errorMessage before analyzing', fakeAsync(() => {
      component.result = REJECT_RESPONSE;
      component.errorMessage = 'old error';
      txServiceSpy.analyze.and.returnValue(of(MOCK_RESPONSE));
      component.analyze();
      tick();
      expect(component.errorMessage).toBe('');
    }));

    it('should set errorMessage on failure', fakeAsync(() => {
      txServiceSpy.analyze.and.returnValue(
        throwError(() => new Error('Analysis failed'))
      );
      component.analyze();
      tick();
      expect(component.errorMessage).toBeTruthy();
      expect(component.isLoading).toBeFalse();
    }));

    it('should set isLoading to false after error', fakeAsync(() => {
      txServiceSpy.analyze.and.returnValue(
        throwError(() => new Error('Server error'))
      );
      component.analyze();
      tick();
      expect(component.isLoading).toBeFalse();
    }));

    it('should handle REJECT result correctly', fakeAsync(() => {
      txServiceSpy.analyze.and.returnValue(of(REJECT_RESPONSE));
      component.analyze();
      tick();
      expect(component.result!.decision).toBe('REJECT');
      expect(component.result!.riskScore).toBeGreaterThan(0.70);
    }));
  });

  // ── resetForm() ───────────────────────────────────────────────────────────
  describe('resetForm()', () => {
    it('should clear result', () => {
      component.result = MOCK_RESPONSE;
      component.resetForm();
      expect(component.result).toBeNull();
    });

    it('should clear errorMessage', () => {
      component.errorMessage = 'some error';
      component.resetForm();
      expect(component.errorMessage).toBe('');
    });
  });

  // ── getRiskColor() ────────────────────────────────────────────────────────
  describe('getRiskColor()', () => {
    it('should return green for score < 0.30', () => {
      expect(component.getRiskColor(0.12)).toBe('var(--accent-green)');
    });

    it('should return orange for score between 0.30 and 0.70', () => {
      expect(component.getRiskColor(0.50)).toBe('var(--accent-orange)');
    });

    it('should return red for score >= 0.70', () => {
      expect(component.getRiskColor(0.92)).toBe('var(--accent-red)');
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
  });
});
