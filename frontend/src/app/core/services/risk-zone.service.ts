import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import {
  RiskProximityResponse,
  RiskZoneDetail,
  RiskZoneListResponse,
  RiskZoneMapResponse,
  RiskZoneOperationResponse,
  RiskZoneRequest,
} from '../../features/risk-zones/models/risk-zone.model';
import { appRuntimeConfig } from '../config/runtime-config';

interface RiskZoneFilters {
  estado?: string;
  nivelRiesgo?: number;
}

interface RiskZoneMapFilters {
  ciudad?: string;
  distrito?: string;
}

@Injectable({
  providedIn: 'root',
})
export class RiskZoneService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly managementUrl = `${appRuntimeConfig.apiBaseUrl}/api/risk-zones`;
  private readonly mapUrl = `${appRuntimeConfig.apiBaseUrl}/api/mapa/zonas-riesgo/activas`;

  getRiskZones(filters?: RiskZoneFilters): Observable<RiskZoneListResponse> {
    let params = new HttpParams();

    if (filters?.estado) {
      params = params.set('estado', filters.estado);
    }

    if (typeof filters?.nivelRiesgo === 'number') {
      params = params.set('nivelRiesgo', filters.nivelRiesgo.toString());
    }

    return this.http.get<RiskZoneListResponse>(this.managementUrl, {
      headers: this.authService.buildAuthorizedHeaders(),
      params,
    });
  }

  getActiveRiskZones(filters?: RiskZoneMapFilters): Observable<RiskZoneMapResponse> {
    let params = new HttpParams();

    if (filters?.ciudad) {
      params = params.set('ciudad', filters.ciudad);
    }

    if (filters?.distrito) {
      params = params.set('distrito', filters.distrito);
    }

    return this.http.get<RiskZoneMapResponse>(this.mapUrl, {
      headers: this.authService.buildAuthorizedHeaders(),
      params,
    });
  }

  getActiveAlerts(): Observable<RiskZoneListResponse> {
    return this.http.get<RiskZoneListResponse>(`${this.managementUrl}/active-alerts`, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  getRiskZoneDetail(riskZoneId: number): Observable<RiskZoneDetail> {
    return this.http.get<RiskZoneDetail>(`${this.managementUrl}/${riskZoneId}`, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  checkProximity(latitude?: number, longitude?: number): Observable<RiskProximityResponse> {
    let params = new HttpParams();

    if (typeof latitude === 'number' && typeof longitude === 'number') {
      params = params.set('lat', latitude.toString()).set('lon', longitude.toString());
    }

    return this.http.get<RiskProximityResponse>(`${this.managementUrl}/check-proximity`, {
      headers: this.authService.buildAuthorizedHeaders(),
      params,
    });
  }

  createRiskZone(payload: RiskZoneRequest): Observable<RiskZoneOperationResponse> {
    return this.http.post<RiskZoneOperationResponse>(this.managementUrl, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  updateRiskZone(
    riskZoneId: number,
    payload: RiskZoneRequest,
  ): Observable<RiskZoneOperationResponse> {
    return this.http.put<RiskZoneOperationResponse>(
      `${this.managementUrl}/${riskZoneId}`,
      payload,
      {
        headers: this.authService.buildAuthorizedHeaders(),
      },
    );
  }

  deactivateRiskZone(riskZoneId: number): Observable<RiskZoneOperationResponse> {
    return this.http.patch<RiskZoneOperationResponse>(
      `${this.managementUrl}/${riskZoneId}/deactivate`,
      {},
      {
        headers: this.authService.buildAuthorizedHeaders(),
      },
    );
  }
}
