import { Role, UserType } from "../../auth/auth.models";
import { PhotoResponse } from "../../../shared/models/photo.models";


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
  avatar?: PhotoResponse | null;
}


export interface UpdateUserProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string | null;
}
