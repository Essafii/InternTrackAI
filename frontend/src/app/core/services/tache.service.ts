import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';
import { TacheDto, TacheRequest } from '../models/tache.model';
import { Page } from '../models/stagiaire.model';

@Injectable({ providedIn: 'root' })
export class TacheService {
  private base = `${environment.apiUrl}/taches`;
  constructor(private http: HttpClient) {}

  getByStagiaire(stagiaireId: number, page = 0, size = 20): Observable<Page<TacheDto>> {
    return this.http.get<Page<TacheDto>>(`${this.base}/stagiaire/${stagiaireId}`, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getById(id: number): Observable<TacheDto> {
    return this.http.get<TacheDto>(`${this.base}/${id}`);
  }

  creer(stagiaireId: number, req: TacheRequest): Observable<TacheDto> {
    return this.http.post<TacheDto>(`${this.base}/stagiaire/${stagiaireId}`, req);
  }

  update(id: number, req: TacheRequest): Observable<TacheDto> {
    return this.http.put<TacheDto>(`${this.base}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
