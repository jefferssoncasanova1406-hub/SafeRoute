import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { LoginRequest, LoginResponse } from '../../features/auth/models/login-response.model';
import { RegisterRequest, RegisterResponse } from '../../features/auth/models/register.model';
import { appRuntimeConfig } from '../config/runtime-config';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = `${appRuntimeConfig.apiBaseUrl}/auth/login`;
  private readonly registerUrl = `${appRuntimeConfig.apiBaseUrl}/auth/register`;
  private readonly storageKey = 'saferoute_auth_session';
  private readonly sessionState = signal<LoginResponse | null>(this.readSession());

  readonly session = this.sessionState.asReadonly();

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginUrl, payload);
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(this.registerUrl, payload);
  }

  requestPasswordReset(email: string): Observable<string> {
    return this.http.post(`${appRuntimeConfig.apiBaseUrl}/auth/forgot-password`, null, {
      params: { email },
      responseType: 'text',
    });
  }

  resetPassword(token: string, newPassword: string): Observable<string> {
    return this.http.post(`${appRuntimeConfig.apiBaseUrl}/auth/reset-password`, null, {
      params: { token, newPassword },
      responseType: 'text',
    });
  }

  updateSessionName(nombre: string): void {
    const session = this.sessionState();
    if (!session) return;
    this.saveSession({ ...session, user: { ...session.user, nombre } });
  }

  saveSession(session: LoginResponse): void {
    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionState.set(session);
  }

  getSession(): LoginResponse | null {
    return this.sessionState();
  }

  clearSession(): void {
    localStorage.removeItem(this.storageKey);
    this.sessionState.set(null);
  }

  isAdminSession(): boolean {
    return (this.sessionState()?.user.rol ?? '').trim().toUpperCase().includes('ADMIN');
  }

  getAuthorizationHeader(): string | null {
    const session = this.sessionState();

    if (!session?.token) {
      return null;
    }

    const tokenType = session.tokenType?.trim() || 'Bearer';
    return `${tokenType} ${session.token}`;
  }

  buildAuthorizedHeaders(): HttpHeaders | undefined {
    const authorizationHeader = this.getAuthorizationHeader();

    return authorizationHeader
      ? new HttpHeaders({ Authorization: authorizationHeader })
      : undefined;
  }

  logout(): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${appRuntimeConfig.apiBaseUrl}/auth/logout`,
      {},
      { headers: this.buildAuthorizedHeaders() },
    );
  }

  private readSession(): LoginResponse | null {
    const rawSession = localStorage.getItem(this.storageKey);

    if (!rawSession) {
      return null;
    }

    try {
      return JSON.parse(rawSession) as LoginResponse;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}
