import { TransportMode } from './route-request.model';

export interface ResolvedPlace {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}

export interface RouteGeometry {
  type: 'LineString' | string;
  coordinates: number[][];
}

export interface RouteStep {
  order: number;
  instruction: string | null;
  streetName: string | null;
  distanceMeters: number | null;
  durationSeconds: number | null;
  maneuverType: string | null;
  modifier: string | null;
}

export interface RouteOption {
  routeId: string;
  summary: string;
  durationMinutes: number;
  distanceKm: number;
  geometry: RouteGeometry;
  steps: RouteStep[];
}

export interface RouteResponse {
  originResolved: ResolvedPlace;
  destinationResolved: ResolvedPlace;
  transportMode: TransportMode;
  departureTime: string | null;
  routes: RouteOption[];
}
