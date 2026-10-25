import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';
import { unauthGuard } from './core/auth/unauth.guard';
import { partnerGuard } from './core/auth/partner.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
    canActivate: [unauthGuard],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/account/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/account/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/account/password/password-reset-request.component').then(
        (m) => m.PasswordResetRequestComponent,
      ),
  },
  {
    path: 'reset/finish',
    loadComponent: () =>
      import('./features/account/password/password-reset-finish.component').then(
        (m) => m.PasswordResetFinishComponent,
      ),
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./features/default/home/home.component').then((m) => m.HomeComponent),
    canActivate: [authGuard],
  },
  {
    path: 'transactions',
    loadComponent: () =>
      import('./features/transactions/transaction-list/transaction-list.component').then(
        (m) => m.TransactionListComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: 'transactions/:id',
    loadComponent: () =>
      import('./features/transactions/transaction-detail/transaction-detail.component').then(
        (m) => m.TransactionDetailComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: 'admin/users',
    loadComponent: () =>
      import('./features/admin/user-management/user-management.component').then(
        (m) => m.UserManagementComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/plugins',
    loadComponent: () =>
      import('./features/admin/plugin-management/plugin-management.component').then(
        (m) => m.PluginManagementComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/recovery',
    loadComponent: () =>
      import('./features/admin/recovery-management/recovery-management.component').then(
        (m) => m.RecoveryManagementComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/metrics',
    loadComponent: () =>
      import('./features/admin/metrics/metrics-dashboard.component').then(
        (m) => m.MetricsDashboardComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/jpos',
    loadComponent: () =>
      import('./features/admin/jpos/jpos-dashboard.component').then(
        (m) => m.JposDashboardComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/services',
    loadComponent: () =>
      import('./features/admin/services/services-dashboard.component').then(
        (m) => m.ServicesDashboardComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/config',
    loadComponent: () =>
      import('./features/admin/config-management/config-management.component').then(
        (m) => m.ConfigManagementComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/api-clients',
    loadComponent: () =>
      import('./features/admin/api-client-management/api-client-management.component').then(
        (m) => m.ApiClientManagementComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/approvals',
    loadComponent: () =>
      import('./features/admin/pending-approvals/pending-approvals.component').then(
        (m) => m.PendingApprovalsComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'partners',
    loadComponent: () =>
      import('./features/partner/partner-list/partner-list.component').then(
        (m) => m.PartnerListComponent,
      ),
    canActivate: [partnerGuard],
  },
  {
    path: 'partners/new',
    loadComponent: () =>
      import('./features/partner/partner-form/partner-form.component').then(
        (m) => m.PartnerFormComponent,
      ),
    canActivate: [partnerGuard],
  },
  {
    path: 'partners/:id/edit',
    loadComponent: () =>
      import('./features/partner/partner-form/partner-form.component').then(
        (m) => m.PartnerFormComponent,
      ),
    canActivate: [partnerGuard],
  },
  {
    path: 'partners/:id',
    loadComponent: () =>
      import('./features/partner/partner-detail/partner-detail.component').then(
        (m) => m.PartnerDetailComponent,
      ),
    canActivate: [partnerGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
