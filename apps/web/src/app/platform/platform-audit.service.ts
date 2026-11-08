import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  PlatformAuditCriterion, PlatformAuditExport, PlatformAuditFilter, PlatformAuditPage,
  PlatformAuditVerification
} from './platform-audit.types';

/**
 * La piste d'audit de la plateforme, en lecture.
 *
 * <p><b>Aucune méthode d'écriture, et ce n'est pas un oubli.</b> Le serveur
 * n'expose aucun `POST` sur cette surface : une surface d'écriture, même
 * réservée à l'éditeur, permettrait de FABRIQUER un acte qui n'a pas eu lieu —
 * exactement ce qu'un auditeur cherche à exclure. Les actes n'y entrent que
 * depuis le code qui les exécute.
 *
 * <p>Contrairement à la piste d'audit d'un client, aucune méthode ne prend de
 * client en paramètre pour SE RESTREINDRE : `targetTenantId` est un FILTRE de
 * lecture, et le journal est délibérément transverse. C'est ce qui permet de
 * reconstituer l'enchaînement de deux actes visant deux clients différents.
 */
@Injectable({ providedIn: 'root' })
export class PlatformAuditService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/platform-audit`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Une page du journal, du plus récent au plus ancien.
   *
   * <p>Aucun paramètre `sort` n'est envoyé : l'ordre est figé côté dépôt
   * (`sequenceNo DESC`) et un tri non mappé partirait en 400.
   */
  list(filter: PlatformAuditFilter, page: number, size: number): Observable<PlatformAuditPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter.action) params = params.set('action', filter.action);
    if (filter.targetTenantId) params = params.set('targetTenantId', filter.targetTenantId);
    if (filter.actorUserId) params = params.set('actorUserId', filter.actorUserId);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    return this.http.get<PlatformAuditPage>(this.endpoint, { params });
  }

  /**
   * Vérifie l'intégrité de la chaîne sur une fenêtre.
   *
   * <p>Sans bornes, le serveur couvre TOUT le journal : c'est la vérification
   * qu'on veut en pratique, et une fenêtre étroite par défaut rendrait un
   * « intact » rassurant sur trois lignes.
   */
  verify(fromSeq?: number, toSeq?: number): Observable<PlatformAuditVerification> {
    let params = new HttpParams();
    if (fromSeq != null) params = params.set('fromSeq', fromSeq);
    if (toSeq != null) params = params.set('toSeq', toSeq);
    return this.http.get<PlatformAuditVerification>(`${this.endpoint}/verify`, { params });
  }

  /**
   * Le dossier scellé à remettre à une autorité.
   *
   * <p>L'export est lui-même un acte consigné côté serveur : deux appels ne
   * rendent donc pas le même journal, et la réponse est servie sans cache.
   */
  export(fromSeq?: number, toSeq?: number): Observable<PlatformAuditExport> {
    let params = new HttpParams();
    if (fromSeq != null) params = params.set('fromSeq', fromSeq);
    if (toSeq != null) params = params.set('toSeq', toSeq);
    return this.http.get<PlatformAuditExport>(`${this.endpoint}/export`, { params });
  }

  /**
   * Reproduit la cascade de priorité du serveur, pour pouvoir l'annoncer.
   *
   * <p>Elle est dupliquée ici en toute connaissance de cause : l'alternative
   * serait de laisser l'utilisateur déduire de ses résultats qu'un seul de ses
   * trois filtres a servi. Si l'ordre change côté serveur, c'est cette méthode
   * qu'il faut suivre — son banc le rappelle.
   */
  effectiveCriterion(filter: PlatformAuditFilter): PlatformAuditCriterion {
    if (filter.targetTenantId) return 'tenant';
    if (filter.action) return 'action';
    if (filter.actorUserId) return 'actor';
    if (filter.from && filter.to) return 'period';
    return 'none';
  }
}
