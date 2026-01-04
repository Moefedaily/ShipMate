import { Routes } from '@angular/router';

export const routes: Routes = [
    {
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
   }

];