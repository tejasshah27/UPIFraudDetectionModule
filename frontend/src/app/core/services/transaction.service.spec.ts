import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';
import {
  FraudAnalysisResponse,
  TransactionRequest,
  UserProfile
} from '../../shared/models/transaction.model';

const BASE_URL = 'http://localhost:8080';

const mockRequest: TransactionRequest = {
  senderId: 'USER_001',
  receiverId: 'USER_002',
  amount: 5000,
  txType: 'P2P',
  mccCode: '5411',
  ipAddress: '192.168.1.1',
  city: 'Mumbai',
  currency: 'INR'
};

const mockFraudResponse: FraudAnalysisResponse = {
  txId: 'TXN_abc123',
  riskScore: 0.15,
  decision: 'ACCEPT',
  scoringEngine: 'ONNX_ML',
  reasons: [],
  ruleBreakdown: [
    { rule: 'AmountThreshold', triggered: false, detail: 'Within limits' }
  ]
};

const mockHighRiskResponse: FraudAnalysisResponse = {
  txId: 'TXN_xyz789',
  riskScore: 0.85,
  decision: 'REJECT',
  scoringEngine: 'ONNX_ML',
  reasons: ['velocity_breach', 'new_city'],
  ruleBreakdown: [
    { rule: 'VelocityCheck', triggered: true, detail: '6 tx in last 10 min' },
    { rule: 'LocationAnomaly', triggered: true, detail: 'New city: Delhi' }
  ]
};

const mockUserProfile: UserProfile = {
  userId: 'USER_001',
  avgAmount: 3000,
  stdDevAmount: 1500,
  totalTransactions: 42,
  knownIpAddresses: ['192.168.1.1'],
  knownCities: ['Mumbai'],
  lastUpdated: '2026-03-30T10:00:00Z'
};

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TransactionService]
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ── Creation ──────────────────────────────────────────────────────────────
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── analyze() ─────────────────────────────────────────────────────────────
  describe('analyze()', () => {
    it('should POST to /api/v1/transactions/analyze and return response', () => {
      service.analyze(mockRequest).subscribe(res => {
        expect(res).toEqual(mockFraudResponse);
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/analyze`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockFraudResponse);
    });

    it('should send the correct request body', () => {
      service.analyze(mockRequest).subscribe();

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/analyze`);
      expect(req.request.body.senderId).toBe('USER_001');
      expect(req.request.body.amount).toBe(5000);
      expect(req.request.body.txType).toBe('P2P');
      req.flush(mockFraudResponse);
    });

    it('should return a high-risk REJECT response', () => {
      service.analyze(mockRequest).subscribe(res => {
        expect(res.decision).toBe('REJECT');
        expect(res.riskScore).toBeGreaterThan(0.70);
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/analyze`);
      req.flush(mockHighRiskResponse);
    });
  });

  // ── submit() ──────────────────────────────────────────────────────────────
  describe('submit()', () => {
    it('should POST to /api/v1/transactions and return response', () => {
      service.submit(mockRequest).subscribe(res => {
        expect(res).toEqual(mockFraudResponse);
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockFraudResponse);
    });

    it('should use a different endpoint than analyze()', () => {
      service.submit(mockRequest).subscribe();
      const submitReq = httpMock.expectOne(`${BASE_URL}/api/v1/transactions`);
      expect(submitReq.request.url).not.toContain('/analyze');
      submitReq.flush(mockFraudResponse);
    });
  });

  // ── getTransaction() ──────────────────────────────────────────────────────
  describe('getTransaction()', () => {
    it('should GET /api/v1/transactions/:id', () => {
      service.getTransaction('TXN_abc123').subscribe(res => {
        expect(res).toEqual(mockFraudResponse);
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/TXN_abc123`);
      expect(req.request.method).toBe('GET');
      req.flush(mockFraudResponse);
    });

    it('should include the transaction id in the URL', () => {
      const txId = 'TXN_xyz789';
      service.getTransaction(txId).subscribe();

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/${txId}`);
      expect(req.request.url).toContain(txId);
      req.flush(mockHighRiskResponse);
    });
  });

  // ── getUserTransactions() ─────────────────────────────────────────────────
  describe('getUserTransactions()', () => {
    it('should GET /api/v1/transactions/user/:userId and return array', () => {
      const mockList = [mockFraudResponse, mockHighRiskResponse];

      service.getUserTransactions('USER_001').subscribe(res => {
        expect(res.length).toBe(2);
        expect(res[0].txId).toBe('TXN_abc123');
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/user/USER_001`);
      expect(req.request.method).toBe('GET');
      req.flush(mockList);
    });

    it('should return an empty array when no transactions exist', () => {
      service.getUserTransactions('USER_NEW').subscribe(res => {
        expect(res).toEqual([]);
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/transactions/user/USER_NEW`);
      req.flush([]);
    });
  });

  // ── getUserProfile() ──────────────────────────────────────────────────────
  describe('getUserProfile()', () => {
    it('should GET /api/v1/users/:userId/profile', () => {
      service.getUserProfile('USER_001').subscribe(res => {
        expect(res).toEqual(mockUserProfile);
      });

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/users/USER_001/profile`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUserProfile);
    });

    it('should include userId in the URL path', () => {
      service.getUserProfile('USER_999').subscribe();

      const req = httpMock.expectOne(`${BASE_URL}/api/v1/users/USER_999/profile`);
      expect(req.request.url).toContain('USER_999');
      req.flush(mockUserProfile);
    });
  });

  // ── generateTransactions() ────────────────────────────────────────────────
  describe('generateTransactions()', () => {
    it('should POST to /api/v1/data/generate with count query param', () => {
      const mockList = [mockFraudResponse, mockHighRiskResponse];

      service.generateTransactions(50).subscribe(res => {
        expect(res.length).toBe(2);
      });

      const req = httpMock.expectOne(r =>
        r.url === `${BASE_URL}/api/v1/data/generate` &&
        r.params.get('count') === '50'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBeNull();
      req.flush(mockList);
    });

    it('should pass null body and count=10 as query param', () => {
      service.generateTransactions(10).subscribe();

      const req = httpMock.expectOne(r =>
        r.url === `${BASE_URL}/api/v1/data/generate` &&
        r.params.get('count') === '10'
      );
      expect(req.request.params.get('count')).toBe('10');
      req.flush([]);
    });

    it('should return an array of FraudAnalysisResponse objects', () => {
      const generated = Array.from({ length: 5 }, (_, i) => ({
        ...mockFraudResponse,
        txId: `TXN_gen_${i}`
      }));

      service.generateTransactions(5).subscribe(res => {
        expect(Array.isArray(res)).toBeTrue();
        expect(res.length).toBe(5);
      });

      const req = httpMock.expectOne(r => r.url === `${BASE_URL}/api/v1/data/generate`);
      req.flush(generated);
    });
  });
});
