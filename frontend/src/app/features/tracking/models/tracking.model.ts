export interface ShareTrackingResponse {
  tokenSeguimiento: string;
  urlCompleta: string;
  fechaExpiracionEstimada: string | null;
  estadoLink: string | null;
}

export interface PublicTrackingResponse {
  nombreUsuario: string | null;
  latitudActual: number | null;
  longitudActual: number | null;
  ultimaActualizacion: string | null;
  estadoRuta: string | null;
}
