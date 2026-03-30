export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  email: string;
  role: 'RH' | 'ENCADRANT' | 'STAGIAIRE' | 'ADMIN';
  fullName: string;
  userId: number;
}

export interface CurrentUser {
  token: string;
  refreshToken: string;
  email: string;
  role: string;
  fullName: string;
  userId: number;
}
