import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { TransactionService } from '../../core/services/transaction.service';
import { FraudAnalysisResponse } from '../../shared/models/transaction.model';

const makeResponse = (
  txId: string,
  riskScore: number,
  decision: 'ACCEPT' | 'REVIEW' | 'REJECT'
): FraudAnalysisResponse => ({
  txId,
  riskScore,
  decision,
  scoringEngine: 'ONNX_ML',
  reasons: [],
  ruleBreakdown: []
});

const MOCK_TRANSACTIONS: FraudAnalysisResponse[] = [
  makeResponse('TXN_1', 0.10, 'ACCEPT'),
  makeResponse('TXN_2', 0.50, 'REVIEW'),
  makeResponse('TXN_3', 0.85, 'REJECT'),
  makeResponse('TXN_4', 0.20, 'ACCEPT'),
  makeResponse('TXN_5', 0.95, 'REJECT')
];

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let txServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    txServiceSpy = jasmine.createSpyObj<TransactionService>('TransactionService', [
      'generateTransactions'
    ]);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, RouterTestingModule],
      providers: [
        { provide: TransactionService, useValue: txServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── Creation ──────────────────────────────────────────────────────────────
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with empty transactions array', () => {
    expect(component.transactions).toEqual([]);
  });

  it('should start with isGenerating = false', () => {
    expect(component.isGenerating).toBeFalse();
  });

  it('should start with no error message', () => {
    expect(component.errorMessage).toBe('');
  });

  it('should have default generateCount of 50', () => {
    expect(component.generateCount).toBe(50);
  });

  // ── Stats computed properties — empty state ───────────────────────────────
  describe('computed stats — empty transactions', () => {
    it('totalCount should be 0', () => {
      expect(component.totalCount).toBe(0);
    });

    it('acceptCount should be 0', () => {
      expect(component.acceptCount).toBe(0);
    });

    it('reviewCount should be 0', () => {
      expect(component.reviewCount).toBe(0);
    });

    it('rejectCount should be 0', () => {
      expect(component.rejectCount).toBe(0);
    });

    it('avgRisk should be 0 when no transactions', () => {
      expect(component.avgRisk).toBe(0);
    });

    it('acceptFlex should return 33 as fallback', () => {
      expect(component.acceptFlex).toBe(33);
    });

    it('reviewFlex should return 34 as fallback', () => {
      expect(component.reviewFlex).toBe(34);
    });

    it('rejectFlex should return 33 as fallback', () => {
      expect(component.rejectFlex).toBe(33);
    });
  });

  // ── Stats computed properties — with data ─────────────────────────────────
  describe('computed stats — with transactions', () => {
    beforeEach(() => {
      component.transactions = MOCK_TRANSACTIONS;
    });

    it('totalCount should equal the number of transactions', () => {
      expect(component.totalCount).toBe(5);
    });

    it('acceptCount should count ACCEPT decisions', () => {
      expect(component.acceptCount).toBe(2);
    });

    it('reviewCount should count REVIEW decisions', () => {
      expect(component.reviewCount).toBe(1);
    });

    it('rejectCount should count REJECT decisions', () => {
      expect(component.rejectCount).toBe(2);
    });

    it('avgRisk should be the mean of all riskScores', () => {
      const expectedAvg = (0.10 + 0.50 + 0.85 + 0.20 + 0.95) / 5;
      expect(component.avgRisk).toBeCloseTo(expectedAvg, 5);
    });

    it('acceptFlex should be 40 (2 out of 5)', () => {
      expect(component.acceptFlex).toBeCloseTo(40, 0);
    });

    it('reviewFlex should be 20 (1 out of 5)', () => {
      expect(component.reviewFlex).toBeCloseTo(20, 0);
    });

    it('rejectFlex should be 40 (2 out of 5)', () => {
      expect(component.rejectFlex).toBeCloseTo(40, 0);
    });
  });

  // ── generateData() ────────────────────────────────────────────────────────
  describe('generateData()', () => {
    it('should call service.generateTransactions with generateCount', fakeAsync(() => {
      txServiceSpy.generateTransactions.and.returnValue(of(MOCK_TRANSACTIONS));
      component.generateData();
      tick();
      expect(txServiceSpy.generateTransactions).toHaveBeenCalledWith(50);
    }));

    it('should populate transactions on success', fakeAsync(() => {
      txServiceSpy.generateTransactions.and.returnValue(of(MOCK_TRANSACTIONS));
      component.generateData();
      tick();
      expect(component.transactions).toEqual(MOCK_TRANSACTIONS);
      expect(component.isGenerating).toBeFalse();
    }));

    it('should clear errorMessage before generating', fakeAsync(() => {
      component.errorMessage = 'previous error';
      txServiceSpy.generateTransactions.and.returnValue(of(MOCK_TRANSACTIONS));
      component.generateData();
      tick();
      expect(component.errorMessage).toBe('');
    }));

    it('should set errorMessage containing backend message on HTTP failure', fakeAsync(() => {
      txServiceSpy.generateTransactions.and.returnValue(
        throwError(() => new Error('Network error'))
      );
      component.generateData();
      tick();
      expect(component.errorMessage).toContain('Failed to connect to backend');
      expect(component.isGenerating).toBeFalse();
    }));

    it('should pass custom generateCount to the service', fakeAsync(() => {
      component.generateCount = 100;
      txServiceSpy.generateTransactions.and.returnValue(of([]));
      component.generateData();
      tick();
      expect(txServiceSpy.generateTransactions).toHaveBeenCalledWith(100);
    }));

    it('should set isGenerating to false after error', fakeAsync(() => {
      txServiceSpy.generateTransactions.and.returnValue(
        throwError(() => new Error('Timeout'))
      );
      component.generateData();
      tick();
      expect(component.isGenerating).toBeFalse();
    }));
  });

  // ── openModal() / closeModal() ────────────────────────────────────────────
  describe('openModal() and closeModal()', () => {
    it('should set selectedTx when openModal is called', () => {
      const evtSpy = { stopPropagation: jasmine.createSpy() } as unknown as Event;
      const tx = MOCK_TRANSACTIONS[0];
      component.openModal(tx, evtSpy);
      expect(component.selectedTx).toEqual(tx);
    });

    it('should call stopPropagation on the event', () => {
      const evtSpy = { stopPropagation: jasmine.createSpy('stopPropagation') } as unknown as Event;
      component.openModal(MOCK_TRANSACTIONS[0], evtSpy);
      expect((evtSpy as any).stopPropagation).toHaveBeenCalled();
    });

    it('should clear selectedTx when closeModal is called', () => {
      component.selectedTx = MOCK_TRANSACTIONS[0];
      component.closeModal();
      expect(component.selectedTx).toBeNull();
    });
  });

  // ── getRiskColor() ────────────────────────────────────────────────────────
  describe('getRiskColor()', () => {
    it('should return green for score < 0.30', () => {
      expect(component.getRiskColor(0.10)).toBe('var(--accent-green)');
      expect(component.getRiskColor(0.29)).toBe('var(--accent-green)');
    });

    it('should return orange for score 0.30 to 0.69', () => {
      expect(component.getRiskColor(0.30)).toBe('var(--accent-orange)');
      expect(component.getRiskColor(0.69)).toBe('var(--accent-orange)');
    });

    it('should return red for score >= 0.70', () => {
      expect(component.getRiskColor(0.70)).toBe('var(--accent-red)');
      expect(component.getRiskColor(0.99)).toBe('var(--accent-red)');
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

  // ── getAvgRiskColor() ─────────────────────────────────────────────────────
  describe('getAvgRiskColor()', () => {
    it('should return green when no transactions (avgRisk = 0)', () => {
      expect(component.getAvgRiskColor()).toBe('var(--accent-green)');
    });

    it('should return red when avgRisk is high', () => {
      component.transactions = [makeResponse('TXN_X', 0.90, 'REJECT')];
      expect(component.getAvgRiskColor()).toBe('var(--accent-red)');
    });
  });

  // ── trackById() ───────────────────────────────────────────────────────────
  describe('trackById()', () => {
    it('should return the txId of the transaction', () => {
      const tx = makeResponse('TXN_track', 0.5, 'REVIEW');
      expect(component.trackById(0, tx)).toBe('TXN_track');
    });
  });
});
