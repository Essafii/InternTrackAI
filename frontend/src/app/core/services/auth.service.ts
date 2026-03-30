import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment.development';
import { AuthResponse, CurrentUser, LoginRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'ita_token';
  private readonly USER_KEY  = 'ita_user';

  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap(res => this.storeSession(res))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): CurrentUser | null {
    return this.currentUserSubject.value;
  }

  hasRole(role: string): boolean {
    return this.getCurrentUser()?.role === role;
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    const user: CurrentUser = { ...res };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private loadUser(): CurrentUser | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
