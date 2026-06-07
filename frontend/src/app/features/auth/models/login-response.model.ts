export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthenticatedUser {
  id: number;
  nombre: string;
  email: string;
  rol: string;
}

export interface LoginResponse {
  message: string;
  token: string;
  tokenType: string;
  user: AuthenticatedUser;
}
