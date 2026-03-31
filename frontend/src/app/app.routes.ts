import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'transactions',
    loadComponent: () => import('./features/transactions/transaction-list.component').then(m => m.TransactionListComponent)
  },
  {
    path: 'analyze',
    loadComponent: () => import('./features/analyze/analyze.component').then(m => m.AnalyzeComponent)
  },
  { path: '**', redirectTo: 'dashboard' }
];
