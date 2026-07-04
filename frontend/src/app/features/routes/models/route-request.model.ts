export type TransportMode = 'driving' | 'walking' | 'cycling';

export interface RouteRequest {
  origin: string;
  destination: string;
  transportMode: TransportMode;
  departureTime?: string;
}
