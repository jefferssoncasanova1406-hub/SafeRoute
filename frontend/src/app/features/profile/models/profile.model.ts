export type RiskPreference = 'bajo' | 'medio' | 'alto';

export interface UserProfile {
  nombre: string;
  email: string;
  preferenciasRiesg: RiskPreference | string;
  radioAlerta: number;
  notificacionesActi: boolean;
}

export interface UpdateProfileRequest {
  nombre: string;
  preferenciasRiesg: RiskPreference;
  radioAlerta: number;
  notificacionesActi: boolean;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}
