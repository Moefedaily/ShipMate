import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  UserProfile,
  UpdateUserProfileRequest
} from './user.models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;
  


  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.api}/users/me`);
  }


  updateMyProfile(
    request: UpdateUserProfileRequest
  ): Observable<UserProfile> {
    return this.http.put<UserProfile>(
      `${this.api}/users/me`,
      request
    );
  }
}
