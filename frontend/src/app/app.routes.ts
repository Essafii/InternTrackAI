import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard',   loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'stagiaires',  loadComponent: () => import('./features/stagiaires/stagiaires.component').then(m => m.StagiairesComponent) },
      { path: 'absences',    loadComponent: () => import('./features/absences/absences.component').then(m => m.AbsencesComponent) },
      { path: 'taches',      loadComponent: () => import('./features/taches/taches.component').then(m => m.TachesComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '/dashboard' }
];
