export type UserType = 'DRIVER' | 'SENDER' | 'BOTH';
export type Role = 'ADMIN' | 'USER';

export interface AuthUser {
  id: string;
  email: string;
  role: Role;
  firstName: string;
  lastName: string;
  phone?: string | null;
  userType: UserType;
  verified: boolean;
  active: boolean;
}

export interface JwtPayload {
  sub: string;        // user id
  email: string;
  userType: UserType;
  exp: number;
}

export interface LoginRequest {
  email: string;
  password: string;
  deviceId: string;
  sessionId: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  userType: UserType;
  phone?: string;
}

