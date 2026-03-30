import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';
import { StagiaireDto, Page } from '../models/stagiaire.model';

@Injectable({ providedIn: 'root' })
export class StagiaireService {
  private base = `${environment.apiUrl}/stagiaires`;
  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 20): Observable<Page<StagiaireDto>> {
    return this.http.get<Page<StagiaireDto>>(this.base, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getById(id: number): Observable<StagiaireDto> {
    return this.http.get<StagiaireDto>(`${this.base}/${id}`);
  }

  getByUserId(userId: number): Observable<StagiaireDto> {
    return this.http.get<StagiaireDto>(`${this.base}/user/${userId}`);
  }

  update(id: number, data: Partial<StagiaireDto>): Observable<StagiaireDto> {
    return this.http.put<StagiaireDto>(`${this.base}/${id}`, data);
  }

  onboard(data: any): Observable<StagiaireDto> {
    return this.http.post<StagiaireDto>(`${this.base}/onboarding`, data);
  }

  archive(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
