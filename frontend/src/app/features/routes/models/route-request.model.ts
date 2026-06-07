export interface RoutePointRequest {
  latitud: number;
  longitud: number;
}

export interface RouteRequest {
  origen: RoutePointRequest;
  destino: RoutePointRequest;
}
