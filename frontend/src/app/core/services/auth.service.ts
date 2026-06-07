import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import {
  LoginRequest,
  LoginResponse,
} from '../../features/auth/models/login-response.model';
import {
  RegisterRequest,
  RegisterResponse,
} from '../../features/auth/models/register.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = 'http://localhost:8080/auth/login';
  private readonly registerUrl = 'http://localhost:8080/auth/register';
  private readonly storageKey = 'saferoute_auth_session';
  private readonly sessionState = signal<LoginResponse | null>(this.readSession());

  readonly session = this.sessionState.asReadonly();

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginUrl, payload);
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(this.registerUrl, payload);
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
