import { Routes } from '@angular/router';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout.component';
import { DashboardLayoutComponent } from './layouts/dashboard-layout/dashboard-layout.component';
import { LandingPage } from './features/landing/landing.page';
import { authGuard } from './core/guards/auth.guard';
import { senderGuard } from './core/guards/sender.guard';
import { driverGuard } from './core/guards/driver.guard';
import { driverDashboardResolver } from './core/services/driver/driver-dashboard.resolver';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout-component';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [

  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: LandingPage },
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/pages/login/login.page')
            .then(m => m.LoginPage)
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/pages/register/register.page')
            .then(m => m.RegisterPage)
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./features/auth/pages/forgot-password/forgot-password.page')
            .then(m => m.ForgotPasswordPage)
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./features/auth/pages/reset-password/reset-password.page')
            .then(m => m.ResetPasswordPage)
      },
      {
        path: 'verify-email',
        loadComponent: () =>
          import('./features/auth/pages/verify-email/verify-email.page')
            .then(m => m.VerifyEmailPage)
      },
      {
        path: 'check-email',
        loadComponent: () =>
          import('./features/auth/pages/check-email/check-email.page')
            .then(m => m.CheckEmailPage)
      }

    ]
  },

  /* ================= DASHBOARD ================= */
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'sender',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/dashboard/sender/sender-home.page')
            .then(m => m.SenderHomePage)
      },
      {
        path: 'shipments',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/shipments/history/shipment-history.page')
            .then(m => m.ShipmentHistoryPage)
      },
      {
        path: 'shipments/new',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/shipments/new/shipment-create.page')
            .then(m => m.ShipmentCreatePage)
      },
      {
        path: 'shipments/:id',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/shipments/detail/shipment-detail.page')
            .then(m => m.ShipmentDetailPage)
      },
      {
        path: 'shipments/:id/edit',
        canActivate: [senderGuard],
        loadComponent: () =>
          import('./features/shipments/edit/shipment-edit.page')
            .then(m => m.ShipmentEditPage)
      },
      {
        path: 'shipments/:id/payment',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/shipments/payment/shipment-payment.page')
            .then(m => m.ShipmentPaymentPage)
      },
      {
        path: 'shipments/:id/claim',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/insurance/new/claim-create.page')
            .then(m => m.ClaimCreatePage)
      },
      {
        path: 'shipments/:id/claim/details',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/insurance/detail/claim-details.page')
            .then(m => m.ClaimDetailsPage)
      },
      {
        path: 'claims',
        canActivate: [senderGuard],
        data: { dashboardRole: 'SENDER' },
        loadComponent: () =>
          import('./features/insurance/list/claims-list.page')
            .then(m => m.ClaimsListPage)
      },
      {
        path: 'driver',
        canActivate: [driverGuard],
        resolve: { state: driverDashboardResolver },
        data: { dashboardRole: 'DRIVER' },
        runGuardsAndResolvers: 'always',
        loadComponent: () =>
          import('./features/dashboard/driver/driver-home.page')
            .then(m => m.DriverHomePage)
      },
      {
        path: 'driver/matching',
        canActivate: [driverGuard],
        data: { dashboardRole: 'DRIVER' },
        loadComponent: () =>
          import('./features/dashboard/driver/matching/driver-matching.page')
            .then(m => m.DriverMatchingPage)
      },
      {
        path: 'earnings',
        canActivate: [driverGuard],
        data: { dashboardRole: 'DRIVER' },
        loadComponent: () =>
          import('./features/earnings/earnings.page')
            .then(m => m.EarningsPage)
      },
      {
        path: 'bookings/:id',
        canActivate: [driverGuard],
        data: { dashboardRole: 'DRIVER' },
        loadComponent: () =>
          import('./features/bookings/booking.page')
            .then(m => m.BookingPage)
      },
      {
        path: 'trip/:id',
        canActivate: [driverGuard],
        data: { dashboardRole: 'DRIVER' },
        loadComponent: () =>
          import('./features/trip/trip.page')
            .then(m => m.TripPage)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.page')
            .then(m => m.ProfilePage)
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'sender'
      }
    ]
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/dashboard/admin/page/admin-dashboard.page')
            .then(m => m.AdminDashboardPage),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/dashboard/admin/page/user/admin-user.page')
            .then(m => m.AdminUsersPage),
      },
      {
        path: 'users/:id',
        loadComponent: () =>
          import('./features/dashboard/admin/page/user/detail/admin-user-detail.page')
            .then(m => m.AdminUserDetailPage),
      },
      {
        path: 'drivers',
        loadComponent: () =>
          import('./features/dashboard/admin/page/driver/admin-drivers.page')
            .then(m => m.AdminDriversPage),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('./features/dashboard/admin/page/booking/admin-bookings.page')
            .then(m => m.AdminBookingsPage),
      },
      {
        path: 'earnings',
        loadComponent: () =>
          import('./features/dashboard/admin/page/earning/admin-earnings.page')
            .then(m => m.AdminEarningsPage),
      },
      {
        path: 'shipments',
        loadComponent: () =>
          import('./features/dashboard/admin/page/shipments/admin-shipments.page')
            .then(m => m.AdminShipmentsPage),
      },
      {
        path: 'shipments/:id',
        loadComponent: () =>
          import('./features/dashboard/admin/page/shipments/detail/admin-shipment-detail.page')
            .then(m => m.AdminShipmentDetailPage),
      },
      {
        path: 'claims',
        loadComponent: () =>
          import('./features/dashboard/admin/page/claims/admin-claims.page')
            .then(m => m.AdminClaimsPage),
      },
      {
        path: 'claims/:id',
        loadComponent: () =>
          import('./features/dashboard/admin/page/claims/detail/admin-claim-detail.page')
            .then(m => m.AdminClaimDetailPage),
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./features/dashboard/admin/page/payments/admin-payments.page')
            .then(m => m.AdminPaymentsPage),
      },
      {
        path: 'payments/:id',
        loadComponent: () =>
          import('./features/dashboard/admin/page/payments/detail/admin-payment-detail.page')
            .then(m => m.AdminPaymentDetailPage),
      }
    ]
  }
];
