import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationDto } from '../../../core/models/notification.model';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  notifications: NotificationDto[] = [];
  showPanel = false;
  private sse?: EventSource;

  constructor(private notifService: NotificationService, public auth: AuthService) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.connectSSE();
  }

  ngOnDestroy(): void {
    this.sse?.close();
  }

  loadNotifications(): void {
    this.notifService.getMes().subscribe(notifs => {
      this.notifications = notifs.slice(0, 10);
      this.unreadCount = notifs.filter(n => !n.lue).length;
    });
  }

  connectSSE(): void {
    this.sse = this.notifService.subscribeSSE();
    this.sse.onmessage = () => this.loadNotifications();
  }

  markAllRead(): void {
    this.notifService.markAllAsRead().subscribe(() => this.loadNotifications());
  }

  togglePanel(): void {
    this.showPanel = !this.showPanel;
  }
}
