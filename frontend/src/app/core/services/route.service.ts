import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import { RouteRequest } from '../../features/routes/models/route-request.model';
import { RouteResponse } from '../../features/routes/models/route-response.model';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly endpointUrl = 'http://localhost:8080/api/routes/evaluate';

  evaluateRoute(payload: RouteRequest): Observable<RouteResponse> {
    const authorizationHeader = this.authService.getAuthorizationHeader();
    const headers = authorizationHeader
      ? new HttpHeaders({ Authorization: authorizationHeader })
      : undefined;

    return this.http.post<RouteResponse>(this.endpointUrl, payload, { headers });
  }
}
