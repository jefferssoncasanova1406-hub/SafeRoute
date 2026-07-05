export interface AlertHistoryItem {
  idAlerta: number;
  tipoIncidente: string;
  descripcion: string;
  nivelRiesgo?: string | null;
  fechaEmision: string;
  estado: string;
  zonaAfectada: string;
  message?: string | null;
}

export interface AlertHistoryFilters {
  tipoIncidente?: string;
  estado?: string;
  fechaInicio?: string;
  fechaFin?: string;
}

export interface IncidentReportRequest {
  tipoIncidente: string;
  ubicacion: string;
  descripcion: string;
}

export interface CommunityVoteRequest {
  idIncidente: number;
  verificado: boolean;
}

export interface ModerationRequest {
  idIncidente: number;
  nuevoEstado: 'APROBADO' | 'RECHAZADO' | 'FALSO';
}
