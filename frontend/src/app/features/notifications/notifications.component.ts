import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { Notification } from '../../core/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent implements OnInit {
  api = inject(ApiService);

  notifications = signal<Notification[]>([]);
  loading = signal(true);
  error = signal('');
  total = signal(0);
  page = signal(0);
  readonly pageSize = 20;
  markingAll = signal(false);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getNotifications(this.page(), this.pageSize).subscribe({
      next: r => {
        this.notifications.set(r.content);
        this.total.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des notifications.');
        this.loading.set(false);
      }
    });
  }

  markRead(notif: Notification) {
    if (notif.read) return;
    this.api.markRead(notif.id).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => n.id === notif.id ? { ...n, read: true } : n)
        );
      },
      error: () => {}
    });
  }

  markAllRead() {
    this.markingAll.set(true);
    this.api.markAllRead().subscribe({
      next: () => {
        this.notifications.update(list => list.map(n => ({ ...n, read: true })));
        this.markingAll.set(false);
      },
      error: () => this.markingAll.set(false)
    });
  }

  get unreadCount(): number {
    return this.notifications().filter(n => !n.read).length;
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  typeClass(type: string): string {
    const map: Record<string, string> = {
      INFO: 'notif-info',
      WARNING: 'notif-warning',
      ALERT: 'notif-alert',
      SUCCESS: 'notif-success'
    };
    return map[type] ?? '';
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      INFO: 'ℹ️', WARNING: '⚠️', ALERT: '🚨', SUCCESS: '✅'
    };
    return map[type] ?? '•';
  }
}
