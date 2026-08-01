import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AiRiskClassification,
  AiSystemDraftRequest,
  AiSystemEditRequest,
  AiSystemFilter,
  AiSystemRegistry,
  AiSystemStatus,
  AiSystemView,
  AiSystemWithdrawRequest,
  RISK_SEVERITY
} from './ai-systems.types';

/**
 * Registre des systèmes d'IA — AI Act (§8, règlement UE 2024/1689).
 *
 * `/api/v1/ai-act/systems` (11 routes) n'avait aucun consommateur alors que les
 * écrans AI Act existants (QMS, conformité, incidents, base UE, FRIA, suivi
 * post-marché) référencent tous un système d'IA : il était impossible d'en créer
 * un depuis l'interface. Ce service est le socle qui débloque la chaîne.
 *
 * Le tenant vient du JWT côté serveur (règle §18.2 #2) : aucune méthode ne le prend
 * en paramètre.
 */
@Injectable({ providedIn: 'root' })
export class AiSystemsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/systems`;

  constructor(private readonly http: HttpClient) {}

  list(status?: AiSystemStatus): Observable<AiSystemView[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<AiSystemView[]>(this.endpoint, { params });
  }

  listByRisk(classification: AiRiskClassification): Observable<AiSystemView[]> {
    const params = new HttpParams().set('classification', classification);
    return this.http.get<AiSystemView[]>(`${this.endpoint}/by-risk`, { params });
  }

  get(id: string): Observable<AiSystemView> {
    return this.http.get<AiSystemView>(`${this.endpoint}/${id}`);
  }

  /**
   * Résolution par référence lisible (AISYS-…), telle qu'elle figure sur les
   * rapports d'audit et dans la base de données UE : un lien profond porte la
   * référence, pas un UUID.
   */
  getByReference(reference: string): Observable<AiSystemView> {
    const params = new HttpParams().set('reference', reference);
    return this.http.get<AiSystemView>(`${this.endpoint}/by-reference`, { params });
  }

  draft(req: AiSystemDraftRequest): Observable<AiSystemView> {
    return this.http.post<AiSystemView>(this.endpoint, req);
  }

  edit(id: string, req: AiSystemEditRequest): Observable<AiSystemView> {
    return this.http.put<AiSystemView>(`${this.endpoint}/${id}`, req);
  }

  register(id: string): Observable<AiSystemView> {
    return this.http.post<AiSystemView>(`${this.endpoint}/${id}/register`, {});
  }

  putInUse(id: string): Observable<AiSystemView> {
    return this.http.post<AiSystemView>(`${this.endpoint}/${id}/put-in-use`, {});
  }

  decommission(id: string): Observable<AiSystemView> {
    return this.http.post<AiSystemView>(`${this.endpoint}/${id}/decommission`, {});
  }

  withdraw(id: string, req: AiSystemWithdrawRequest): Observable<AiSystemView> {
    return this.http.post<AiSystemView>(`${this.endpoint}/${id}/withdraw`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  /**
   * Vue de l'écran de liste.
   *
   * Deux jeux distincts sont nécessaires : les compteurs de tête doivent décrire
   * le registre ENTIER (sinon filtrer sur « haut risque » afficherait « 3 systèmes,
   * dont 3 à haut risque », ce qui est faux), tandis que le tableau montre le
   * résultat filtré. Sans filtre, une seule requête suffit et les deux jeux sont
   * la même liste.
   *
   * Le serveur n'expose pas de route croisant statut ET classification : quand les
   * deux filtres sont posés, on part de la route par classification et on affine le
   * statut côté client, sur un jeu déjà réduit.
   */
  registry(filter: AiSystemFilter): Observable<AiSystemRegistry> {
    const all$ = this.list().pipe(map(sortByRiskThenReference));
    if (!filter.risk && !filter.status) {
      return all$.pipe(map(all => ({ all, rows: all })));
    }
    const rows$ = filter.risk
      ? this.listByRisk(filter.risk).pipe(
        map(rows => filter.status ? rows.filter(s => s.status === filter.status) : rows))
      : this.list(filter.status ?? undefined);
    return combineLatest([all$, rows$]).pipe(
      map(([all, rows]) => ({ all, rows: sortByRiskThenReference(rows) }))
    );
  }

  /**
   * Résolution d'un paramètre de route : UUID ou référence lisible.
   * Retourne `null` quand le paramètre n'est ni l'un ni l'autre — inutile
   * d'interroger le serveur pour une valeur qu'il rejettera.
   */
  resolve(idOrReference: string): Observable<AiSystemView | null> {
    const value = (idOrReference ?? '').trim();
    if (UUID.test(value)) return this.get(value);
    if (REFERENCE.test(value)) return this.getByReference(value);
    return of(null);
  }
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const REFERENCE = /^[A-Z][A-Z0-9_-]{1,63}$/;

/**
 * Tri d'affichage : le plus risqué d'abord, puis par référence.
 * Le registre sert à surveiller les obligations — un système à haut risque ne doit
 * jamais se retrouver en bas de page par hasard d'insertion.
 */
function sortByRiskThenReference(rows: AiSystemView[]): AiSystemView[] {
  return [...rows].sort((a, b) => {
    const severity = RISK_SEVERITY.indexOf(a.riskClassification)
      - RISK_SEVERITY.indexOf(b.riskClassification);
    return severity !== 0 ? severity : a.reference.localeCompare(b.reference);
  });
}
