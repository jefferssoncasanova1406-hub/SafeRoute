export interface PrivacyPreferencesRequest {
  realTimeLocationEnabled: boolean;
  personalDataSharingEnabled: boolean;
}

export interface PrivacyPreferencesResponse {
  userId: number;
  realTimeLocationEnabled: boolean;
  personalDataSharingEnabled: boolean;
  message: string;
}
