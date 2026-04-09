import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
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
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  auth = inject(AuthService);

  navItems: NavItem[] = [
    { label: 'Tableau de bord', icon: 'dashboard',        route: '/app/dashboard',      roles: ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'] },
    { label: 'Stagiaires',      icon: 'group',             route: '/app/stagiaires',     roles: ['RH', 'ENCADRANT', 'ADMIN'] },
    { label: 'Tâches',          icon: 'assignment',        route: '/app/taches',         roles: ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'] },
    { label: 'Absences',        icon: 'event_busy',        route: '/app/absences',       roles: ['RH', 'ENCADRANT', 'ADMIN'] },
    { label: 'Évaluations',     icon: 'rate_review',       route: '/app/evaluations',    roles: ['RH', 'ENCADRANT'] },
    { label: 'Livrables',       icon: 'inventory_2',       route: '/app/livrables',      roles: ['RH', 'ENCADRANT', 'STAGIAIRE'] },
    { label: 'Classement',      icon: 'leaderboard',       route: '/app/classement',     roles: ['RH', 'ADMIN', 'ENCADRANT'] },
    { label: 'Notifications',   icon: 'notifications',     route: '/app/notifications',  roles: ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'] },
    { label: 'Reporting',       icon: 'assessment',        route: '/app/reporting',      roles: ['RH', 'ADMIN', 'ENCADRANT'] },
    { label: 'Export',          icon: 'file_download',     route: '/app/export',         roles: ['RH', 'ADMIN'] },
    { label: 'Audit',           icon: 'manage_search',     route: '/app/audit',          roles: ['ADMIN'] },
    { label: 'Utilisateurs',    icon: 'manage_accounts',   route: '/app/users',          roles: ['RH', 'ADMIN'] },
  ];

  get visibleItems(): NavItem[] {
    const role = this.auth.currentUser()?.role;
    return this.navItems.filter(i => role && i.roles.includes(role));
  }

  get userInitial(): string {
    return this.auth.currentUser()?.fullName?.charAt(0)?.toUpperCase() ?? '?';
  }

  get userRole(): string {
    const role = this.auth.currentUser()?.role;
    const labels: Record<string, string> = {
      RH: 'Responsable RH',
      ENCADRANT: 'Encadrant',
      STAGIAIRE: 'Stagiaire',
      ADMIN: 'Administrateur'
    };
    return role ? (labels[role] ?? role) : '';
  }
}
