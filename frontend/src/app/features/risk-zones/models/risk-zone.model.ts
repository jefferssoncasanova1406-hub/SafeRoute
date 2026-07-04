export interface RiskZoneLocation {
  latitud: number;
  longitud: number;
  distrito: string;
  ciudad: string;
}

export interface RiskZoneGeometry {
  type: string;
  coordinates: number[][][];
}

export interface RiskZoneRequest {
  tipo: string;
  nivelRiesgo: number;
  descripcion: string;
  geometria: RiskZoneGeometry;
  ubicacion: RiskZoneLocation;
}

export interface RiskZoneDetail {
  idZona: number;
  tipo: string;
  nivelRiesgo: number;
  descripcion: string;
  estado: string;
  fechaActualizacion: string;
  geometria: RiskZoneGeometry;
  ubicacion: RiskZoneLocation;
}

export interface RiskZoneListResponse {
  message: string;
  zonas: RiskZoneDetail[];
}

export interface RiskZoneOperationResponse {
  message: string;
  zona: RiskZoneDetail;
}

export interface RiskProximityResponse {
  alertaGenerada: boolean;
  mensaje?: string;
  detalle?: RiskZoneDetail;
}

export interface RiskZoneMapLocation {
  ciudad: string;
  distrito: string;
}

export interface RiskZoneMapZone {
  idZona: number;
  tipo: string;
  nivelRiesgo: number;
  nivelRiesgoNombre: string;
  color: string;
  descripcion: string;
  fechaActualizacion: string;
  geometria: RiskZoneGeometry;
  centro: RiskZoneLocation;
}

export interface RiskZoneMapResponse {
  ubicacion: RiskZoneMapLocation;
  totalZonas: number;
  mensaje: string;
  zonas: RiskZoneMapZone[];
}
