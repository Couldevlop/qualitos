import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../auth/auth.service';

/**
 * Ajoute le Bearer token sur toutes les requêtes API.
 * Le tenant_id est porté par le JWT lui-même (conformément à CLAUDE.md §18.2).
 */
@Injectable()
export class ApiInterceptor implements HttpInterceptor {

  constructor(private readonly auth: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.getAccessToken();
    if (!token) {
      return next.handle(req);
    }
    const authed = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next.handle(authed);
  }
}
