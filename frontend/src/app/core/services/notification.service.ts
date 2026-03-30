import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';
import { NotificationDto } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private base = `${environment.apiUrl}/notifications`;
  constructor(private http: HttpClient) {}

  getMes(): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(this.base);
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/unread-count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/read`, null);
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.base}/mark-all-read`, null);
  }

  subscribeSSE(): EventSource {
    const token = localStorage.getItem('ita_token');
    return new EventSource(`${environment.apiUrl}/sse/subscribe?token=${token}`);
  }
}
