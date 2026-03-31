import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FraudAnalysisResponse, TransactionRequest, UserProfile } from '../../shared/models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiBaseUrl;

  analyze(request: TransactionRequest): Observable<FraudAnalysisResponse> {
    return this.http.post<FraudAnalysisResponse>(`${this.baseUrl}/api/v1/transactions/analyze`, request);
  }

  submit(request: TransactionRequest): Observable<FraudAnalysisResponse> {
    return this.http.post<FraudAnalysisResponse>(`${this.baseUrl}/api/v1/transactions`, request);
  }

  getTransaction(id: string): Observable<FraudAnalysisResponse> {
    return this.http.get<FraudAnalysisResponse>(`${this.baseUrl}/api/v1/transactions/${id}`);
  }

  getAllTransactions(limit: number = 100): Observable<FraudAnalysisResponse[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<FraudAnalysisResponse[]>(`${this.baseUrl}/api/v1/transactions`, { params });
  }

  getUserTransactions(userId: string): Observable<FraudAnalysisResponse[]> {
    return this.http.get<FraudAnalysisResponse[]>(`${this.baseUrl}/api/v1/transactions/user/${userId}`);
  }

  getUserProfile(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/api/v1/users/${userId}/profile`);
  }

  generateTransactions(count: number): Observable<FraudAnalysisResponse[]> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.post<FraudAnalysisResponse[]>(`${this.baseUrl}/api/v1/data/generate`, null, { params });
  }
}
