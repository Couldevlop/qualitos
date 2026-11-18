import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { MailSettings, MailSettingsRequest, MailTestResult } from './mail-settings.types';

/**
 * La messagerie de l'organisation (ADR 0127).
 *
 * <p>Deux surfaces, délibérément distinctes : celle du **client**, qui ne porte
 * aucun identifiant — le client est celui du jeton, côté serveur — et celle d'un
 * **cabinet**, qui le nomme, parce que la console de plateforme en gère
 * plusieurs.
 *
 * <p>Les réunir sous une route paramétrée par le type d'organisation aurait fini
 * par accepter les deux : un administrateur de client aurait pu désigner un
 * cabinet, ou l'inverse. Ici il n'y a rien à vérifier, puisqu'il n'y a rien à
 * choisir.
 */
@Injectable({ providedIn: 'root' })
export class MailSettingsService {

  private readonly client = `${environment.apiBaseUrl}/api/v1/mail-settings`;
  private readonly firms = `${environment.apiBaseUrl}/api/v1/partner-firms`;

  constructor(private readonly http: HttpClient) {}

  // ---------- la messagerie du client connecté ----------

  lire(): Observable<MailSettings> {
    return this.http.get<MailSettings>(this.client);
  }

  enregistrer(req: MailSettingsRequest): Observable<MailSettings> {
    return this.http.put<MailSettings>(this.client, req);
  }

  eprouver(recipient: string): Observable<MailTestResult> {
    return this.http.post<MailTestResult>(`${this.client}/test`, { recipient });
  }

  retirer(): Observable<void> {
    return this.http.delete<void>(this.client);
  }

  // ---------- la messagerie d'un cabinet partenaire ----------

  lireCabinet(firmId: string): Observable<MailSettings> {
    return this.http.get<MailSettings>(this.cabinet(firmId));
  }

  enregistrerCabinet(firmId: string, req: MailSettingsRequest): Observable<MailSettings> {
    return this.http.put<MailSettings>(this.cabinet(firmId), req);
  }

  eprouverCabinet(firmId: string, recipient: string): Observable<MailTestResult> {
    return this.http.post<MailTestResult>(`${this.cabinet(firmId)}/test`, { recipient });
  }

  retirerCabinet(firmId: string): Observable<void> {
    return this.http.delete<void>(this.cabinet(firmId));
  }

  private cabinet(firmId: string): string {
    return `${this.firms}/${encodeURIComponent(firmId)}/mail-settings`;
  }
}
