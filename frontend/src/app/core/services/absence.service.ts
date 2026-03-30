import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';
import { AbsenceDto, AbsenceRequest } from '../models/absence.model';
import { Page } from '../models/stagiaire.model';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private base = `${environment.apiUrl}/absences`;
  constructor(private http: HttpClient) {}

  getByStagiaire(stagiaireId: number, page = 0, size = 20): Observable<Page<AbsenceDto>> {
    return this.http.get<Page<AbsenceDto>>(`${this.base}/stagiaire/${stagiaireId}`, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getAssiduite(stagiaireId: number): Observable<number> {
    return this.http.get<number>(`${this.base}/stagiaire/${stagiaireId}/assiduite`);
  }

  enregistrer(stagiaireId: number, req: AbsenceRequest): Observable<AbsenceDto> {
    return this.http.post<AbsenceDto>(`${this.base}/stagiaire/${stagiaireId}`, req);
  }

  valider(id: number, commentaire?: string): Observable<AbsenceDto> {
    let params = new HttpParams();
    if (commentaire) params = params.set('commentaire', commentaire);
    return this.http.patch<AbsenceDto>(`${this.base}/${id}/valider`, null, { params });
  }

  uploadJustificatif(id: number, file: File): Observable<AbsenceDto> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<AbsenceDto>(`${this.base}/${id}/justificatif`, fd);
  }
}
