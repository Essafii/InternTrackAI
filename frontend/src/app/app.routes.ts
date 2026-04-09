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

      // All authenticated roles
      { path: 'dashboard',     loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications.component').then(m => m.NotificationsComponent) },

      // RH, ENCADRANT, ADMIN — not STAGIAIRE
      { path: 'stagiaires',    loadComponent: () => import('./features/stagiaires/stagiaires.component').then(m => m.StagiairesComponent),          canActivate: [roleGuard('RH', 'ENCADRANT', 'ADMIN')] },
      { path: 'stagiaires/:id',loadComponent: () => import('./features/stagiaires/stagiaire-detail.component').then(m => m.StagiaireDetailComponent), canActivate: [roleGuard('RH', 'ENCADRANT', 'ADMIN')] },
      { path: 'absences',      loadComponent: () => import('./features/absences/absences.component').then(m => m.AbsencesComponent),                canActivate: [roleGuard('RH', 'ENCADRANT', 'ADMIN')] },
      { path: 'classement',    loadComponent: () => import('./features/classement/classement.component').then(m => m.ClassementComponent),          canActivate: [roleGuard('RH', 'ENCADRANT', 'ADMIN')] },
      { path: 'reporting',     loadComponent: () => import('./features/reporting/reporting.component').then(m => m.ReportingComponent),             canActivate: [roleGuard('RH', 'ENCADRANT', 'ADMIN')] },

      // RH, ENCADRANT — evaluations
      { path: 'evaluations',   loadComponent: () => import('./features/evaluations/evaluations.component').then(m => m.EvaluationsComponent),       canActivate: [roleGuard('RH', 'ENCADRANT')] },

      // RH, ENCADRANT, STAGIAIRE — tasks and deliverables
      { path: 'taches',        loadComponent: () => import('./features/taches/taches.component').then(m => m.TachesComponent),                     canActivate: [roleGuard('RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN')] },
      { path: 'livrables',     loadComponent: () => import('./features/livrables/livrables.component').then(m => m.LivrablesComponent),             canActivate: [roleGuard('RH', 'ENCADRANT', 'STAGIAIRE')] },

      // RH, ADMIN only
      { path: 'export',        loadComponent: () => import('./features/export/export.component').then(m => m.ExportComponent),                     canActivate: [roleGuard('RH', 'ADMIN')] },
      { path: 'audit',         loadComponent: () => import('./features/audit/audit.component').then(m => m.AuditComponent),                        canActivate: [roleGuard('ADMIN')] },
      { path: 'users',         loadComponent: () => import('./features/users/users.component').then(m => m.UsersComponent),                        canActivate: [roleGuard('RH', 'ADMIN')] },
    ]
  },
  { path: '**', redirectTo: '/login' }
];
