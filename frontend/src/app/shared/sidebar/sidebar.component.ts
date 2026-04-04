import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  auth = inject(AuthService);

  navItems: NavItem[] = [
    { label: 'Tableau de bord', icon: '📊', route: '/app/dashboard', roles: ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'] },
    { label: 'Stagiaires', icon: '👥', route: '/app/stagiaires', roles: ['RH', 'ENCADRANT', 'ADMIN'] },
    { label: 'Tâches', icon: '✅', route: '/app/taches', roles: ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'] },
    { label: 'Absences', icon: '📅', route: '/app/absences', roles: ['RH', 'ENCADRANT', 'ADMIN'] },
    { label: 'Évaluations', icon: '⭐', route: '/app/evaluations', roles: ['RH', 'ENCADRANT'] },
    { label: 'Livrables', icon: '📁', route: '/app/livrables', roles: ['RH', 'ENCADRANT', 'STAGIAIRE'] },
    { label: 'Classement', icon: '🏆', route: '/app/classement', roles: ['RH', 'ADMIN'] },
    { label: 'Notifications', icon: '🔔', route: '/app/notifications', roles: ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'] },
    { label: 'Reporting', icon: '📈', route: '/app/reporting', roles: ['RH', 'ADMIN'] },
    { label: 'Export', icon: '⬇️', route: '/app/export', roles: ['RH', 'ADMIN'] },
    { label: 'Audit', icon: '🔍', route: '/app/audit', roles: ['RH', 'ADMIN'] },
    { label: 'Utilisateurs', icon: '🔧', route: '/app/users', roles: ['RH', 'ADMIN'] },
  ];

  get visibleItems(): NavItem[] {
    const role = this.auth.currentUser()?.role;
    return this.navItems.filter(i => role && i.roles.includes(role));
  }
}
