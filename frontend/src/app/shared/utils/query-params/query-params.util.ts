import { HttpParams } from '@angular/common/http';

export function buildHttpParams(obj: any): HttpParams {

  let params = new HttpParams();

  Object.keys(obj).forEach(key => {

    const value = obj[key];

    if (value !== undefined && value !== null) {
      params = params.set(key, value);
    }

  });

  return params;

}