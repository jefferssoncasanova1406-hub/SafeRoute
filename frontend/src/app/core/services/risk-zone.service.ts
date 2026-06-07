import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import {
  RiskZoneListResponse,
  RiskZoneMapResponse,
  RiskZoneOperationResponse,
  RiskZoneRequest,
} from '../../features/risk-zones/models/risk-zone.model';

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
  private readonly managementUrl = 'http://localhost:8080/api/risk-zones';
  private readonly mapUrl = 'http://localhost:8080/api/mapa/zonas-riesgo/activas';

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

  createRiskZone(payload: RiskZoneRequest): Observable<RiskZoneOperationResponse> {
    return this.http.post<RiskZoneOperationResponse>(this.managementUrl, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  updateRiskZone(
    riskZoneId: number,
    payload: RiskZoneRequest,
  ): Observable<RiskZoneOperationResponse> {
    return this.http.put<RiskZoneOperationResponse>(`${this.managementUrl}/${riskZoneId}`, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
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
