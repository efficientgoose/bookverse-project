import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpHeaders,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { TokenService } from '../token/token.service';

@Injectable()
export class HttpTokenInterceptor implements HttpInterceptor {
  constructor(private tokenService: TokenService) {}

  intercept(
    request: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    let token = this.tokenService.token;

    token =
      'eyJhbGciOiJIUzM4NCJ9.eyJmdWxsbmFtZSI6IkFqaW5reWEgS2FsZSIsInN1YiI6InNvbWVlbWFpbEBnbWFpbC5jb20iLCJpYXQiOjE3Mzc4MTE4OTEsImV4cCI6MTczNzgyMDUzMSwiYXV0aG9yaXRpZXMiOlsiVVNFUiJdfQ.E-HzX_IK36cK94R9WmSAKGZNVPxfJC6NyMmV61nAOdgwoRysed7XIgRFy9ZPg9kf';
    console.log('The JWT token is: ' + token);

    if (token) {
      const authReq = request.clone({
        headers: new HttpHeaders({
          Authorization: 'Bearer ' + token,
        }),
      });
      return next.handle(authReq);
    }

    return next.handle(request);
  }
}
