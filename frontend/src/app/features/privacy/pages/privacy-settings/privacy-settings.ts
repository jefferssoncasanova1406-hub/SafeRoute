import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { finalize } from 'rxjs';

import { PrivacyService } from '../../services/privacy.service';
import { PrivacyPreferencesRequest } from '../../models/privacy.model';

interface ApiErrorBody {
  message?: string;
  error?: string;
}

@Component({
  selector: 'app-privacy-settings-page',
  imports: [],
  templateUrl: './privacy-settings.html',
  styleUrl: './privacy-settings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacySettingsPage implements OnInit, OnDestroy {
  private readonly privacyService = inject(PrivacyService);
  private successTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly isLoading = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);
  protected readonly saveSuccess = signal(false);
  protected readonly realTimeLocation = signal(false);
  protected readonly personalDataSharing = signal(false);

  ngOnInit(): void {
    this.loadPreferences();
  }

  ngOnDestroy(): void {
    if (this.successTimer !== null) {
      clearTimeout(this.successTimer);
    }
  }

  protected loadPreferences(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    this.privacyService
      .getPreferences()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.realTimeLocation.set(response.realTimeLocationEnabled);
          this.personalDataSharing.set(response.personalDataSharingEnabled);
        },
        error: (error: HttpErrorResponse) => {
          this.loadError.set(this.parseError(error, 'No se pudieron cargar las preferencias.'));
        },
      });
  }

  protected toggleRealTimeLocation(event: Event): void {
    this.realTimeLocation.set((event.target as HTMLInputElement).checked);
  }

  protected togglePersonalDataSharing(event: Event): void {
    this.personalDataSharing.set((event.target as HTMLInputElement).checked);
  }

  protected savePreferences(): void {
    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.isSaving.set(true);

    if (this.successTimer !== null) {
      clearTimeout(this.successTimer);
    }

    const payload: PrivacyPreferencesRequest = {
      realTimeLocationEnabled: this.realTimeLocation(),
      personalDataSharingEnabled: this.personalDataSharing(),
    };

    this.privacyService
      .updatePreferences(payload)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: () => {
          this.saveSuccess.set(true);
          this.successTimer = setTimeout(() => this.saveSuccess.set(false), 3000);
        },
        error: (error: HttpErrorResponse) => {
          this.saveError.set(this.parseError(error, 'No se pudieron guardar las preferencias.'));
        },
      });
  }

  private parseError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    const body = error.error as ApiErrorBody | string | null;

    if (typeof body === 'string' && body.trim()) {
      return body;
    }

    if (body && typeof body === 'object' && body.message) {
      return body.message;
    }

    return fallback;
  }
}
