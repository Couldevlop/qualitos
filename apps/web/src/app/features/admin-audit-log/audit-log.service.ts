import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuditEvent, AuditEventFilter, AuditEventPage, ChainVerification, EffectiveCriterion
} from './audit-log.types';

/**
 * Consultation du journal d'audit (§11.5).
 *
 * Le tenant est dérivé du JWT côté serveur (`TenantContext`, règle §18.2 #2) : aucune
 * méthode ne le prend en paramètre, et aucune ne peut lire le journal d'un autre tenant.
 *
 * Journal APPEND-ONLY : ce client n'expose volontairement ni écriture (`POST /events`,
 * réservé au back-office machine) ni ancrage (`POST /{id}/anchor`). L'ancrage blockchain
 * n'est ici que LU, via `blockchainTxRef`.
 */
@Injectable({ providedIn: 'root' })
export class AuditLogService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/audit/events`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Page du journal, du plus récent au plus ancien (le serveur impose l'ordre
   * `sequenceNo DESC`). On n'envoie jamais de paramètre `sort` : la requête est figée
   * côté repository et un tri non mappé est rejeté en 400.
   */
  list(filter: AuditEventFilter, page: number, size: number): Observable<AuditEventPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter.action) params = params.set('action', filter.action);
    if (filter.resourceType) params = params.set('resourceType', filter.resourceType);
    if (filter.resourceId) params = params.set('resourceId', filter.resourceId);
    if (filter.actorUserId) params = params.set('actorUserId', filter.actorUserId);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    return this.http.get<AuditEventPage>(this.endpoint, { params });
  }

  /** Détail complet d'un événement (payload, IP, agent, hashes). */
  get(id: string): Observable<AuditEvent> {
    return this.http.get<AuditEvent>(`${this.endpoint}/${id}`);
  }

  /**
   * Vérifie l'intégrité de la chaîne sur la fenêtre [fromSeq, toSeq] inclusive :
   * contiguïté des séquences, recalcul de chaque hash, chaînage des `previousHash`.
   * Lecture seule — aucune donnée n'est modifiée.
   */
  verifyChain(fromSeq: number, toSeq: number): Observable<ChainVerification> {
    const params = new HttpParams().set('fromSeq', fromSeq).set('toSeq', toSeq);
    return this.http.get<ChainVerification>(`${this.endpoint}/verify`, { params });
  }

  /**
   * Reproduit la cascade de priorité du serveur pour pouvoir l'annoncer à l'utilisateur.
   *
   * Le service backend teste dans cet ordre : (resourceType ET resourceId), puis action,
   * puis actorUserId, puis (from ET to). Les critères ne se combinent pas ; sans cette
   * restitution, un utilisateur qui saisit « action + période » croirait à tort filtrer
   * sur les deux.
   */
  effectiveCriterion(filter: AuditEventFilter): EffectiveCriterion {
    if (filter.resourceType && filter.resourceId) return 'resource';
    if (filter.action) return 'action';
    if (filter.actorUserId) return 'actor';
    if (filter.from && filter.to) return 'period';
    return 'none';
  }
}
