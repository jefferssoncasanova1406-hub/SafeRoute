import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import { RouteRequest } from '../../features/routes/models/route-request.model';
import {
  RouteResponse,
  SafeRouteRequest,
  SafeRouteResponse,
} from '../../features/routes/models/route-response.model';
import { appRuntimeConfig } from '../config/runtime-config';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly endpointUrl = `${appRuntimeConfig.apiBaseUrl}/api/routes/evaluate`;
  private readonly safeRouteUrl = `${appRuntimeConfig.apiBaseUrl}/api/rutas/calcular-segura`;

  evaluateRoute(payload: RouteRequest): Observable<RouteResponse> {
    const authorizationHeader = this.authService.getAuthorizationHeader();
    const headers = authorizationHeader
      ? new HttpHeaders({ Authorization: authorizationHeader })
      : undefined;

    return this.http.post<RouteResponse>(this.endpointUrl, payload, { headers });
  }

  evaluateSafeRoute(payload: SafeRouteRequest): Observable<SafeRouteResponse> {
    return this.http.post<SafeRouteResponse>(this.safeRouteUrl, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }
}
