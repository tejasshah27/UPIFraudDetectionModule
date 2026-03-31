export interface RuleBreakdownItem {
  rule: string;
  triggered: boolean;
  detail: string;
}

export interface FraudAnalysisResponse {
  txId: string;
  riskScore: number;
  decision: 'ACCEPT' | 'REVIEW' | 'REJECT';
  scoringEngine: string;
  reasons: string[];
  ruleBreakdown: RuleBreakdownItem[];
}

export interface UserProfile {
  userId: string;
  avgAmount: number;
  stdDevAmount: number;
  totalTransactions: number;
  knownIpAddresses: string[];
  knownCities: string[];
  lastUpdated: string;
}

export interface TransactionRequest {
  senderId: string;
  receiverId: string;
  amount: number;
  txType: 'P2P' | 'P2M' | 'BILL_PAY' | 'RECHARGE';
  mccCode?: string;
  ipAddress: string;
  city: string;
  currency?: string;
}
