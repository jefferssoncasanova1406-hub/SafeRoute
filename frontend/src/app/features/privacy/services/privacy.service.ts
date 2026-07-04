import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { appRuntimeConfig } from '../../../core/config/runtime-config';
import {
  PrivacyPreferencesRequest,
  PrivacyPreferencesResponse,
} from '../models/privacy.model';

@Injectable({
  providedIn: 'root',
})
export class PrivacyService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly url = `${appRuntimeConfig.apiBaseUrl}/api/privacy/preferences`;

  getPreferences(): Observable<PrivacyPreferencesResponse> {
    return this.http.get<PrivacyPreferencesResponse>(this.url, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }

  updatePreferences(payload: PrivacyPreferencesRequest): Observable<PrivacyPreferencesResponse> {
    return this.http.put<PrivacyPreferencesResponse>(this.url, payload, {
      headers: this.authService.buildAuthorizedHeaders(),
    });
  }
}
