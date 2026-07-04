import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { appRuntimeConfig } from '../../../core/config/runtime-config';
import { AuthService } from '../../../core/services/auth.service';
import { PasswordChangeRequest, UpdateProfileRequest, UserProfile } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly profileUrl = `${appRuntimeConfig.apiBaseUrl}/api/profile`;

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.profileUrl, {
      headers: this.auth.buildAuthorizedHeaders(),
    });
  }

  updateProfile(payload: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(this.profileUrl, payload, {
      headers: this.auth.buildAuthorizedHeaders(),
    });
  }

  changePassword(payload: PasswordChangeRequest): Observable<string> {
    return this.http.patch(`${appRuntimeConfig.apiBaseUrl}/secure/change-password`, payload, {
      headers: this.auth.buildAuthorizedHeaders(),
      responseType: 'text',
    });
  }
}
