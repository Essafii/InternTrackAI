import { Component, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import { NotificationDto } from '../../core/models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent implements OnDestroy {
  auth = inject(AuthService);
  api = inject(ApiService);

  notifications = signal<NotificationDto[]>([]);
  showPanel = signal(false);
  private eventSource?: EventSource;

  constructor() { this.loadNotifications(); this.connectSSE(); }

  loadNotifications() {
    this.api.getNotifications(0, 10).subscribe({
      next: r => this.notifications.set(r.content),
      error: () => {}
    });
  }

  get unreadCount(): number {
    return this.notifications().filter(n => !n.lue).length;
  }

  connectSSE() {
    try {
      const token = this.auth.getToken();
      if (!token) return;
      this.eventSource = new EventSource(
        `${environment.apiUrl}/sse/subscribe?token=${token}`
      );
      this.eventSource.onmessage = () => this.loadNotifications();
    } catch {}
  }

  togglePanel() { this.showPanel.update(v => !v); }

  markAllRead() {
    this.api.markAllRead().subscribe(() => this.loadNotifications());
  }

  ngOnDestroy() { this.eventSource?.close(); }
}
