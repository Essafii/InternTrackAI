import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  roles: string[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  navItems: NavItem[] = [
    { label: 'Dashboard',    route: '/dashboard',    icon: '▦', roles: ['RH','ENCADRANT','STAGIAIRE','ADMIN'] },
    { label: 'Stagiaires',   route: '/stagiaires',   icon: '👤', roles: ['RH','ENCADRANT','ADMIN'] },
    { label: 'Absences',     route: '/absences',     icon: '📅', roles: ['RH','ENCADRANT','ADMIN'] },
    { label: 'Tâches',       route: '/taches',       icon: '✓',  roles: ['RH','ENCADRANT','STAGIAIRE','ADMIN'] },
    { label: 'Évaluations',  route: '/evaluations',  icon: '★',  roles: ['RH','ENCADRANT','STAGIAIRE','ADMIN'] },
    { label: 'Livrables',    route: '/livrables',    icon: '📎', roles: ['RH','ENCADRANT','STAGIAIRE','ADMIN'] },
    { label: 'Classement',   route: '/classement',   icon: '🏆', roles: ['RH','ENCADRANT','ADMIN'] },
    { label: 'Audit',        route: '/audit',        icon: '🔒', roles: ['ADMIN'] },
  ];

  constructor(public auth: AuthService) {}

  get visibleItems(): NavItem[] {
    const role = this.auth.getCurrentUser()?.role ?? '';
    return this.navItems.filter(item => item.roles.includes(role));
  }
}
