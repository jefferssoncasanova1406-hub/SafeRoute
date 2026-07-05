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
  nivelRiesgo?: string | null;
  scoreRiesgo?: number | null;
  cruzaZonasRiesgo?: boolean | null;
  zonasRiesgo?: RouteRiskZone[] | null;
  recomendada?: boolean | null;
}

export interface RouteResponse {
  originResolved: ResolvedPlace;
  destinationResolved: ResolvedPlace;
  transportMode: TransportMode;
  departureTime: string | null;
  routes: RouteOption[];
}

export interface SafeRoutePoint {
  latitud: number;
  longitud: number;
}

export interface SafeRouteRequest {
  origen: SafeRoutePoint;
  destino: SafeRoutePoint;
}

export interface RouteRiskZone {
  idZona: number;
  tipo: string | null;
  nivelRiesgo: number | null;
  nivelRiesgoNombre: string | null;
  color: string | null;
  descripcion: string | null;
}

export interface SafeRouteOption {
  distancia: number | null;
  tiempoEstimado: number | null;
  scoreRiesgo: number | null;
  nivelRiesgo: string | null;
  cruzaZonasRiesgo: boolean | null;
  geometria: RouteGeometry | null;
  zonasRiesgo: RouteRiskZone[] | null;
}

export interface SafeRouteResponse {
  origen: SafeRoutePoint;
  destino: SafeRoutePoint;
  rutaMasRapida: SafeRouteOption | null;
  rutaMasSegura: SafeRouteOption | null;
  rutaRecomendada: SafeRouteOption | null;
  nivelRiesgo: string | null;
  scoreRiesgo: number | null;
  tiempoEstimado: number | null;
  distancia: number | null;
  recomendacion: string | null;
}
