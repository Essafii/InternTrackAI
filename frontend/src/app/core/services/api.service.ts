import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Stagiaire, Tache, Absence, Evaluation, Livrable,
  Notification, ClassementEntry, ActionLog, DashboardStats,
  PageResponse, ReportJob, User
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // Dashboard
  getDashboard() { return this.http.get<DashboardStats>(`${this.base}/dashboard`); }

  // Stagiaires
  getStagiaires(page = 0, size = 10, search?: string, statut?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    if (statut) params = params.set('statut', statut);
    return this.http.get<PageResponse<Stagiaire>>(`${this.base}/stagiaires`, { params });
  }
  getStagiaire(id: number) { return this.http.get<Stagiaire>(`${this.base}/stagiaires/${id}`); }
  createStagiaire(data: any) { return this.http.post<Stagiaire>(`${this.base}/stagiaires`, data); }
  updateStagiaire(id: number, data: any) { return this.http.put<Stagiaire>(`${this.base}/stagiaires/${id}`, data); }

  // Tâches
  getTaches(page = 0, size = 10, stagiaireId?: number, etat?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (stagiaireId) params = params.set('stagiaireId', stagiaireId);
    if (etat) params = params.set('etat', etat);
    return this.http.get<PageResponse<Tache>>(`${this.base}/taches`, { params });
  }
  createTache(data: any) { return this.http.post<Tache>(`${this.base}/taches`, data); }
  updateTacheEtat(id: number, etat: string) { return this.http.patch<Tache>(`${this.base}/taches/${id}/etat`, { etat }); }
  updateTache(id: number, data: any) { return this.http.put<Tache>(`${this.base}/taches/${id}`, data); }

  // Absences
  getAbsences(page = 0, size = 10, stagiaireId?: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (stagiaireId) params = params.set('stagiaireId', stagiaireId);
    return this.http.get<PageResponse<Absence>>(`${this.base}/absences`, { params });
  }
  createAbsence(data: any) { return this.http.post<Absence>(`${this.base}/absences`, data); }
  getAssiduite(stagiaireId: number) {
    return this.http.get<{ tauxAssiduite: number }>(`${this.base}/absences/assiduite/${stagiaireId}`);
  }

  // Évaluations
  getEvaluations(page = 0, size = 10, stagiaireId?: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (stagiaireId) params = params.set('stagiaireId', stagiaireId);
    return this.http.get<PageResponse<Evaluation>>(`${this.base}/evaluations`, { params });
  }
  createEvaluation(data: any) { return this.http.post<Evaluation>(`${this.base}/evaluations`, data); }
  updateEvaluation(id: number, data: any) { return this.http.put<Evaluation>(`${this.base}/evaluations/${id}`, data); }

  // Livrables
  getLivrables(page = 0, size = 10, stagiaireId?: number, statut?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (stagiaireId) params = params.set('stagiaireId', stagiaireId);
    if (statut) params = params.set('statut', statut);
    return this.http.get<PageResponse<Livrable>>(`${this.base}/livrables`, { params });
  }
  uploadLivrable(data: FormData) { return this.http.post<Livrable>(`${this.base}/livrables`, data); }
  validerLivrable(id: number, statut: string, commentaire?: string) {
    return this.http.patch<Livrable>(`${this.base}/livrables/${id}/valider`, { statut, commentaire });
  }

  // Classement
  getClassement(equipe?: string) {
    let params = new HttpParams();
    if (equipe) params = params.set('equipe', equipe);
    return this.http.get<ClassementEntry[]>(`${this.base}/classement`, { params });
  }

  // Notifications
  getNotifications(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Notification>>(`${this.base}/notifications`, { params });
  }
  markAllRead() { return this.http.post(`${this.base}/notifications/mark-all-read`, {}); }
  markRead(id: number) { return this.http.patch(`${this.base}/notifications/${id}/read`, {}); }

  // Reporting
  getReports() { return this.http.get<ReportJob[]>(`${this.base}/reporting`); }
  generateReport(type: string, params?: any) {
    return this.http.post<ReportJob>(`${this.base}/reporting/generate`, { type, ...params });
  }

  // Export
  exportStagiaires(format: string) {
    return this.http.get(`${this.base}/export/stagiaires?format=${format}`, { responseType: 'blob' });
  }
  exportClassement(format: string) {
    return this.http.get(`${this.base}/export/classement?format=${format}`, { responseType: 'blob' });
  }

  // Audit
  getAuditLogs(page = 0, size = 20, action?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (action) params = params.set('action', action);
    return this.http.get<PageResponse<ActionLog>>(`${this.base}/audit`, { params });
  }

  // Users
  getUsers(page = 0, size = 10, role?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (role) params = params.set('role', role);
    return this.http.get<PageResponse<User>>(`${this.base}/users`, { params });
  }
  createUser(data: any) { return this.http.post<User>(`${this.base}/users`, data); }
  updateUser(id: number, data: any) { return this.http.put<User>(`${this.base}/users/${id}`, data); }
  deleteUser(id: number) { return this.http.delete(`${this.base}/users/${id}`); }
  toggleUser(id: number, enabled: boolean) {
    return this.http.patch(`${this.base}/users/${id}/toggle`, { enabled });
  }
}
