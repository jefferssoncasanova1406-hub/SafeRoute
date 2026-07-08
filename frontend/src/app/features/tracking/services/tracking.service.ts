import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { appRuntimeConfig } from '../../../core/config/runtime-config';
import { PublicTrackingResponse, ShareTrackingResponse } from '../models/tracking.model';

@Injectable({
  providedIn: 'root',
})
export class TrackingService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${appRuntimeConfig.apiBaseUrl}/api/tracking`;

  share(): Observable<ShareTrackingResponse> {
    return this.http.post<ShareTrackingResponse>(`${this.baseUrl}/compartir`, null, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  updateLocation(token: string, latitud: number, longitud: number): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/${encodeURIComponent(token)}/ubicacion`,
      { latitud, longitud },
      { headers: this.authService.buildAuthorizedHeaders() },
    );
  }

  stop(token: string): Observable<string> {
    return this.http.put(`${this.baseUrl}/detener/${encodeURIComponent(token)}`, null, {
      headers: this.authService.buildAuthorizedHeaders(),
      responseType: 'text',
    });
  }

  getPublicTracking(token: string): Observable<PublicTrackingResponse> {
    return this.http.get<PublicTrackingResponse>(
      `${this.baseUrl}/public/consultar/${encodeURIComponent(token)}`,
    );
  }
}
