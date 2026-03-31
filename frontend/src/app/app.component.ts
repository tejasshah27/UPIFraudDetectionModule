import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="nav-shell">
      <div class="nav-inner">
        <a class="nav-brand" routerLink="/dashboard">
          <div class="nav-brand-icon">🛡️</div>
          <div>
            <div class="nav-brand-text">UPI Fraud Detection</div>
            <div class="nav-brand-sub">Antigravity · POC System</div>
          </div>
        </a>
        <div class="nav-links">
          <a class="nav-link" routerLink="/dashboard" routerLinkActive="active" id="nav-dashboard">
            📊 Dashboard
          </a>
          <a class="nav-link" routerLink="/transactions" routerLinkActive="active" id="nav-transactions">
            📋 Transactions
          </a>
          <a class="nav-link" routerLink="/analyze" routerLinkActive="active" id="nav-analyze">
            🔍 Analyze
            <span class="nav-badge">LIVE</span>
          </a>
        </div>
      </div>
    </nav>
    <div class="page-content">
      <router-outlet></router-outlet>
    </div>
  `
})
export class AppComponent {}
