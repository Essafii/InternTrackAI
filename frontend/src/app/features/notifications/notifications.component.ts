import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { NotificationDto, TypeNotification } from '../../core/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent implements OnInit {
  api = inject(ApiService);

  notifications = signal<NotificationDto[]>([]);
  loading    = signal(true);
  error      = signal('');
  total      = signal(0);
  page       = signal(0);
  readonly pageSize = 20;
  markingAll = signal(false);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getNotifications(this.page(), this.pageSize).subscribe({
      next: r => { this.notifications.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => { this.error.set('Erreur lors du chargement.'); this.loading.set(false); }
    });
  }

  markRead(notif: NotificationDto) {
    if (notif.lue) return;
    this.api.markRead(notif.id).subscribe({
      next: () => this.notifications.update(list => list.map(n => n.id === notif.id ? { ...n, lue: true } : n)),
      error: () => {}
    });
  }

  markAllRead() {
    this.markingAll.set(true);
    this.api.markAllRead().subscribe({
      next: () => { this.notifications.update(list => list.map(n => ({ ...n, lue: true }))); this.markingAll.set(false); },
      error: () => this.markingAll.set(false)
    });
  }

  get unreadCount(): number { return this.notifications().filter(n => !n.lue).length; }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  typeColor(type: TypeNotification): string {
    const map: Partial<Record<TypeNotification, string>> = {
      RETARD_TACHE:       '#FFAE41',
      RETARD_LIVRABLE:    '#FFAE41',
      ABSENCE:            '#D14600',
      ALERTE_ASSIDUITE:   '#D14600',
      ALERTE_IA:          '#FF7E51',
      NOUVELLE_EVALUATION:'#a9c7ff',
      VALIDATION_LIVRABLE:'#4caf50',
      REJET_LIVRABLE:     '#D14600',
      ONBOARDING:         '#8cd0e9',
      GENERAL:            '#8d909e'
    };
    return map[type] ?? '#8d909e';
  }

  typeIcon(type: TypeNotification): string {
    const map: Partial<Record<TypeNotification, string>> = {
      RETARD_TACHE:       'schedule',
      RETARD_LIVRABLE:    'inventory_2',
      ABSENCE:            'event_busy',
      ALERTE_ASSIDUITE:   'trending_down',
      ALERTE_IA:          'smart_toy',
      NOUVELLE_EVALUATION:'rate_review',
      VALIDATION_LIVRABLE:'check_circle',
      REJET_LIVRABLE:     'cancel',
      ONBOARDING:         'waving_hand',
      GENERAL:            'info'
    };
    return map[type] ?? 'notifications';
  }
}
