import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should render the nav shell', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.querySelector('nav.nav-shell')).toBeTruthy();
  });

  it('should contain a router-outlet', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('should render the brand text "UPI Fraud Detection"', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('UPI Fraud Detection');
  });

  it('should render a dashboard navigation link', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    const navDashboard = compiled.querySelector('#nav-dashboard');
    expect(navDashboard).toBeTruthy();
  });

  it('should render a transactions navigation link', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    const navTransactions = compiled.querySelector('#nav-transactions');
    expect(navTransactions).toBeTruthy();
  });

  it('should render an analyze navigation link', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    const navAnalyze = compiled.querySelector('#nav-analyze');
    expect(navAnalyze).toBeTruthy();
  });
});
