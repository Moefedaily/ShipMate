import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/landing.page';

export const routes: Routes = [
    
    {
        path: '',
        component: LandingPage
    }
    ,{
        path: 'login',
        loadComponent: () =>
            import('./features/auth/pages/login/login.page')
            .then(m => m.LoginPage)
    },
    {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full'
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
   }

];