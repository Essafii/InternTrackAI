import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private base = `${environment.apiUrl}/dashboard`;
  constructor(private http: HttpClient) {}

  getRh(): Observable<any> {
    return this.http.get<any>(`${this.base}/rh`);
  }

  getEncadrant(id: number): Observable<any> {
    return this.http.get<any>(`${this.base}/encadrant/${id}`);
  }

  getStagiaire(id: number): Observable<any> {
    return this.http.get<any>(`${this.base}/stagiaire/${id}`);
  }
}
