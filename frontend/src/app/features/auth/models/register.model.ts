export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
}

export interface RegisteredUser {
  id: number;
  nombre: string;
  email: string;
  rol: string;
}

export interface RegisterResponse {
  message: string;
  user: RegisteredUser;
}
