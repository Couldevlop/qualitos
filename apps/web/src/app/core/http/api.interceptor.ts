import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Inject, Injectable, LOCALE_ID } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../auth/auth.service';

/**
 * Ajoute le Bearer token et la LANGUE DE L'APPLICATION sur toutes les requêtes API.
 *
 * <p>Le tenant_id est porté par le JWT lui-même (conformément à CLAUDE.md §18.2).
 *
 * <p>`Accept-Language` porte la langue du BUILD, pas celle du navigateur. Le
 * navigateur envoie déjà la sienne, mais elle dit ce que l'utilisateur a réglé
 * dans Chrome, et non ce qu'il a choisi dans QualitOS : un utilisateur au
 * navigateur français qui passe l'application en anglais recevrait alors un
 * contenu français dans une interface anglaise. Ce qui compte est le choix fait
 * ICI, et `LOCALE_ID` est exactement cela — chaque langue étant un build distinct.
 *
 * <p>Le serveur s'en sert pour le contenu qu'il peut traduire : le référentiel
 * APQP, tant que le client ne l'a pas réécrit.
 */
@Injectable()
export class ApiInterceptor implements HttpInterceptor {

  constructor(
    private readonly auth: AuthService,
    @Inject(LOCALE_ID) private readonly locale: string
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const entetes: Record<string, string> = {
      // « fr-FR » devient « fr » : le serveur raisonne par langue, et une variante
      // régionale qu'il ne connaît pas le ferait retomber sur son défaut.
      'Accept-Language': (this.locale || 'fr').split('-')[0]
    };
    const token = this.auth.getAccessToken();
    if (token) {
      entetes['Authorization'] = `Bearer ${token}`;
    }
    return next.handle(req.clone({ setHeaders: entetes }));
  }
}
