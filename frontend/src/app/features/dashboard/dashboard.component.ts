import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  objectKeys = Object.keys;
  data: any = null;
  loading = true;

  constructor(private dashboardService: DashboardService, public auth: AuthService) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser()!;
    const obs = user.role === 'RH' || user.role === 'ADMIN'
      ? this.dashboardService.getRh()
      : user.role === 'ENCADRANT'
        ? this.dashboardService.getEncadrant(user.userId)
        : this.dashboardService.getStagiaire(user.userId);

    obs.subscribe({ next: d => { this.data = d; this.loading = false; }, error: () => this.loading = false });
  }
}
