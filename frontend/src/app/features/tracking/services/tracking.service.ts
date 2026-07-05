import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { appRuntimeConfig } from '../../../core/config/runtime-config';
import { ShareTrackingResponse } from '../models/tracking.model';

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

  stop(token: string): Observable<string> {
    return this.http.put(`${this.baseUrl}/detener/${encodeURIComponent(token)}`, null, {
      headers: this.authService.buildAuthorizedHeaders(),
      responseType: 'text',
    });
  }
}
