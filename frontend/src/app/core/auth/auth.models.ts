export type UserType = 'DRIVER' | 'SENDER' | 'BOTH';
export type Role = 'ADMIN' | 'USER';

export interface AuthUser {
  id: string;
  email: string;
  role: Role;
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
