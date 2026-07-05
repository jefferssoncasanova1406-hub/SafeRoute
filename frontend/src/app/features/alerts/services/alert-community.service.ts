import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { appRuntimeConfig } from '../../../core/config/runtime-config';
import {
  AlertHistoryFilters,
  AlertHistoryItem,
  CommunityVoteRequest,
  IncidentReportRequest,
  ModerationRequest,
} from '../models/alert-community.model';

@Injectable({
  providedIn: 'root',
})
export class AlertCommunityService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${appRuntimeConfig.apiBaseUrl}/api/alertas`;

  getHistory(filters?: AlertHistoryFilters): Observable<AlertHistoryItem[]> {
    let params = new HttpParams();

    for (const key of ['tipoIncidente', 'estado', 'fechaInicio', 'fechaFin'] as const) {
      const value = filters?.[key]?.trim();
      if (value) {
        params = params.set(key, value);
      }
    }

    return this.http.get<AlertHistoryItem[]>(`${this.baseUrl}/historial`, {
      headers: this.authService.buildAuthorizedHeaders(),
      params,
    });
  }

  getHistoryDetail(id: number): Observable<AlertHistoryItem> {
    return this.http.get<AlertHistoryItem>(`${this.baseUrl}/detalle/${id}`, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  createReport(payload: IncidentReportRequest): Observable<AlertHistoryItem> {
    return this.http.post<AlertHistoryItem>(`${this.baseUrl}/reportar`, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  verifyIncident(payload: CommunityVoteRequest): Observable<AlertHistoryItem> {
    return this.http.post<AlertHistoryItem>(`${this.baseUrl}/verificar`, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  getPendingReports(): Observable<AlertHistoryItem[]> {
    return this.http.get<AlertHistoryItem[]>(`${this.baseUrl}/moderacion/pendientes`, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  processModeration(payload: ModerationRequest): Observable<AlertHistoryItem> {
    return this.http.put<AlertHistoryItem>(`${this.baseUrl}/moderacion/procesar`, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }
}
