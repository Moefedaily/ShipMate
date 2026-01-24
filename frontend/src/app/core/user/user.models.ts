import { Role, UserType } from '../auth/auth.models';


export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  role: Role;
  userType: UserType;
  verified: boolean;
  active: boolean;
}


export interface UpdateUserProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string | null;
}
