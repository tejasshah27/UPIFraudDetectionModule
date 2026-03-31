import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TransactionListComponent } from './transaction-list.component';
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
  makeResponse('TXN_A', 0.05, 'ACCEPT'),
  makeResponse('TXN_B', 0.55, 'REVIEW'),
  makeResponse('TXN_C', 0.90, 'REJECT')
];

describe('TransactionListComponent', () => {
  let component: TransactionListComponent;
  let fixture: ComponentFixture<TransactionListComponent>;
  let txServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    txServiceSpy = jasmine.createSpyObj<TransactionService>('TransactionService', [
      'getUserTransactions'
    ]);

    await TestBed.configureTestingModule({
      imports: [TransactionListComponent],
      providers: [
        { provide: TransactionService, useValue: txServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── Creation ──────────────────────────────────────────────────────────────
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with empty userId', () => {
    expect(component.userId).toBe('');
  });

  it('should start with empty transactions array', () => {
    expect(component.transactions).toEqual([]);
  });

  it('should start with isLoading = false', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('should start with no error message', () => {
    expect(component.errorMessage).toBe('');
  });

  it('should start with no selectedTx', () => {
    expect(component.selectedTx).toBeNull();
  });

  // ── loadTransactions() ────────────────────────────────────────────────────
  describe('loadTransactions()', () => {
    it('should not call service when userId is empty', () => {
      component.userId = '';
      component.loadTransactions();
      expect(txServiceSpy.getUserTransactions).not.toHaveBeenCalled();
    });

    it('should not call service when userId is whitespace only', () => {
      component.userId = '   ';
      component.loadTransactions();
      expect(txServiceSpy.getUserTransactions).not.toHaveBeenCalled();
    });

    it('should call service with trimmed userId', fakeAsync(() => {
      component.userId = '  USER_001  ';
      txServiceSpy.getUserTransactions.and.returnValue(of(MOCK_TRANSACTIONS));
      component.loadTransactions();
      tick();
      expect(txServiceSpy.getUserTransactions).toHaveBeenCalledWith('USER_001');
    }));

    it('should populate transactions on success', fakeAsync(() => {
      component.userId = 'USER_001';
      txServiceSpy.getUserTransactions.and.returnValue(of(MOCK_TRANSACTIONS));
      component.loadTransactions();
      tick();
      expect(component.transactions).toEqual(MOCK_TRANSACTIONS);
      expect(component.isLoading).toBeFalse();
    }));

    it('should clear errorMessage before loading', fakeAsync(() => {
      component.userId = 'USER_001';
      component.errorMessage = 'previous error';
      txServiceSpy.getUserTransactions.and.returnValue(of(MOCK_TRANSACTIONS));
      component.loadTransactions();
      tick();
      expect(component.errorMessage).toBe('');
    }));

    it('should set errorMessage on failure', fakeAsync(() => {
      component.userId = 'USER_001';
      txServiceSpy.getUserTransactions.and.returnValue(
        throwError(() => new Error('Not found'))
      );
      component.loadTransactions();
      tick();
      expect(component.errorMessage).toBeTruthy();
      expect(component.isLoading).toBeFalse();
    }));

    it('should set isLoading to false after error', fakeAsync(() => {
      component.userId = 'USER_001';
      txServiceSpy.getUserTransactions.and.returnValue(
        throwError(() => new Error('500 Server Error'))
      );
      component.loadTransactions();
      tick();
      expect(component.isLoading).toBeFalse();
    }));

    it('should handle empty transaction list from server', fakeAsync(() => {
      component.userId = 'USER_NEW';
      txServiceSpy.getUserTransactions.and.returnValue(of([]));
      component.loadTransactions();
      tick();
      expect(component.transactions).toEqual([]);
      expect(component.isLoading).toBeFalse();
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
      component.openModal(MOCK_TRANSACTIONS[1], evtSpy);
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
      expect(component.getRiskColor(0.05)).toBe('var(--accent-green)');
      expect(component.getRiskColor(0.0)).toBe('var(--accent-green)');
    });

    it('should return orange for score between 0.30 and 0.70', () => {
      expect(component.getRiskColor(0.30)).toBe('var(--accent-orange)');
      expect(component.getRiskColor(0.55)).toBe('var(--accent-orange)');
      expect(component.getRiskColor(0.699)).toBe('var(--accent-orange)');
    });

    it('should return red for score >= 0.70', () => {
      expect(component.getRiskColor(0.70)).toBe('var(--accent-red)');
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
  });

  // ── trackById() ───────────────────────────────────────────────────────────
  describe('trackById()', () => {
    it('should return the txId of the transaction', () => {
      const tx = makeResponse('TXN_track42', 0.3, 'REVIEW');
      expect(component.trackById(0, tx)).toBe('TXN_track42');
    });
  });
});
