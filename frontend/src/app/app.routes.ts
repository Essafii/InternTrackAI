import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'app',
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'stagiaires', loadComponent: () => import('./features/stagiaires/stagiaires.component').then(m => m.StagiairesComponent) },
      { path: 'stagiaires/:id', loadComponent: () => import('./features/stagiaires/stagiaire-detail.component').then(m => m.StagiaireDetailComponent) },
      { path: 'taches', loadComponent: () => import('./features/taches/taches.component').then(m => m.TachesComponent) },
      { path: 'absences', loadComponent: () => import('./features/absences/absences.component').then(m => m.AbsencesComponent) },
      { path: 'evaluations', loadComponent: () => import('./features/evaluations/evaluations.component').then(m => m.EvaluationsComponent) },
      { path: 'livrables', loadComponent: () => import('./features/livrables/livrables.component').then(m => m.LivrablesComponent) },
      { path: 'classement', loadComponent: () => import('./features/classement/classement.component').then(m => m.ClassementComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications.component').then(m => m.NotificationsComponent) },
      { path: 'reporting', loadComponent: () => import('./features/reporting/reporting.component').then(m => m.ReportingComponent), canActivate: [roleGuard('RH', 'ADMIN')] },
      { path: 'export', loadComponent: () => import('./features/export/export.component').then(m => m.ExportComponent), canActivate: [roleGuard('RH', 'ADMIN')] },
      { path: 'audit', loadComponent: () => import('./features/audit/audit.component').then(m => m.AuditComponent), canActivate: [roleGuard('RH', 'ADMIN')] },
      { path: 'users', loadComponent: () => import('./features/users/users.component').then(m => m.UsersComponent), canActivate: [roleGuard('RH', 'ADMIN')] },
    ]
  },
  { path: '**', redirectTo: '/login' }
];
