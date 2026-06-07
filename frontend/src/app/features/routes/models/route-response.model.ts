export interface RoutePointResponse {
  latitud: number;
  longitud: number;
}

export interface RouteGeometry {
  type: string;
  coordinates: number[][];
}

export interface RouteRiskZoneCenter {
  latitud: number;
  longitud: number;
  distrito: string;
  ciudad: string;
}

export interface RouteRiskZone {
  idZona: number;
  tipo: string;
  nivelRiesgo: number;
  nivelRiesgoNombre: string;
  color: string;
  descripcion: string;
  centro: RouteRiskZoneCenter;
}

export interface RouteOption {
  distancia: number;
  tiempoEstimado: number;
  scoreRiesgo: number;
  nivelRiesgo: string;
  cruzaZonasRiesgo: boolean;
  geometria: RouteGeometry;
  zonasRiesgo: RouteRiskZone[];
}

export interface RouteResponse {
  origen: RoutePointResponse;
  destino: RoutePointResponse;
  rutaMasRapida: RouteOption;
  rutaMasSegura: RouteOption;
  rutaRecomendada: RouteOption;
  nivelRiesgo: string;
  scoreRiesgo: number;
  tiempoEstimado: number;
  distancia: number;
  recomendacion: string;
}
