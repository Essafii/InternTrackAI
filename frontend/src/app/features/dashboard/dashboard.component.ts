import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import {
  DashboardRhDto, DashboardTrendsDto,
  DashboardEncadrantDto, DashboardStagiaireDto
} from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  api = inject(ApiService);
  auth = inject(AuthService);

  loading = signal(true);
  today = new Date();

  // Role-specific data
  rhStats    = signal<DashboardRhDto | null>(null);
  rhTrends   = signal<DashboardTrendsDto | null>(null);
  encStats   = signal<DashboardEncadrantDto | null>(null);
  stgStats   = signal<DashboardStagiaireDto | null>(null);

  role = computed(() => this.auth.currentUser()?.role);
  isRhOrAdmin  = computed(() => ['RH', 'ADMIN'].includes(this.role() ?? ''));
  isEncadrant  = computed(() => this.role() === 'ENCADRANT');
  isStagiaire  = computed(() => this.role() === 'STAGIAIRE');

  ngOnInit() {
    const user = this.auth.currentUser();
    if (!user) return;

    if (this.isRhOrAdmin()) {
      this.api.getDashboardRh().subscribe({ next: d => this.rhStats.set(d), error: () => {} });
      this.api.getDashboardTrends().subscribe({ next: d => this.rhTrends.set(d), error: () => {} });
      this.loading.set(false);
    } else if (this.isEncadrant()) {
      this.api.getDashboardEncadrant(user.userId).subscribe({
        next: d => { this.encStats.set(d); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    } else if (this.isStagiaire()) {
      // Need stagiaireId — fetch from user profile
      this.api.getStagiaireByUserId(user.userId).subscribe({
        next: s => {
          this.api.getDashboardStagiaire(s.id).subscribe({
            next: d => { this.stgStats.set(d); this.loading.set(false); },
            error: () => this.loading.set(false)
          });
        },
        error: () => this.loading.set(false)
      });
    }
  }

  assiduiteColor(taux: number): string {
    if (taux >= 80) return '#4caf50';
    if (taux >= 60) return '#FFAE41';
    return '#D14600';
  }

  // Build bar chart data from trends
  maxTrendVal = computed(() => {
    const trends = this.rhTrends()?.monthlyTrends ?? [];
    return Math.max(...trends.map(t => Math.max(t.absences, t.taches)), 1);
  });
}
