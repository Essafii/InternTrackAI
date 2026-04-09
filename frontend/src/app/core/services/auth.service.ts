import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly TOKEN_KEY = 'ita_token';
  private readonly USER_KEY = 'ita_user';

  currentUser = signal<AuthResponse | null>(this.loadUser());

  private loadUser(): AuthResponse | null {
    try {
      const u = localStorage.getItem(this.USER_KEY);
      return u ? JSON.parse(u) : null;
    } catch { return null; }
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(res));
        this.currentUser.set(res);
      })
    );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }
  isLoggedIn(): boolean { return !!this.getToken(); }
  hasRole(...roles: string[]): boolean {
    const user = this.currentUser();
    return user ? roles.includes(user.role) : false;
  }
}
