import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, combineLatest, forkJoin, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CalibrationOverview, CreateEquipmentRequest, CreateMsaRequest, CreateRecordRequest,
  DueState, EquipmentDetail, EquipmentPage, EquipmentResponse, EquipmentRow, EquipmentStatus,
  EquipmentSummary, MsaPage, MsaResponse, PlanPage, PlanResponse, RecordPage, RecordResponse,
  UpdateEquipmentRequest, UpsertPlanRequest
} from './calibration.types';

/**
 * Plafond de plans « à risque » ramenés en une fois pour alimenter la colonne
 * échéance et les compteurs. La route est paginée ; un tenant qui dépasserait ce
 * volume a un problème de pilotage bien avant d'avoir un problème d'affichage.
 */
const AT_RISK_PAGE_SIZE = 500;

/**
 * Calibration & équipements (§4.10).
 *
 * L'API `/api/v1/calibration` (15 routes) n'avait aucun consommateur : les échéances
 * de calibration — l'information critique du module — n'étaient consultables qu'en
 * appelant l'API à la main.
 *
 * Le tenant vient du JWT côté serveur (§18.2 #2) : aucune méthode ne le prend.
 */
@Injectable({ providedIn: 'root' })
export class CalibrationService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/calibration`;

  constructor(private readonly http: HttpClient) {}

  // ---- Équipement ------------------------------------------------------------

  listEquipment(page = 0, size = 50, status?: EquipmentStatus): Observable<EquipmentPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<EquipmentPage>(`${this.endpoint}/equipment`, { params });
  }

  getEquipment(id: string): Observable<EquipmentResponse> {
    return this.http.get<EquipmentResponse>(`${this.endpoint}/equipment/${id}`);
  }

  createEquipment(input: CreateEquipmentRequest): Observable<EquipmentResponse> {
    return this.http.post<EquipmentResponse>(`${this.endpoint}/equipment`, input);
  }

  updateEquipment(id: string, input: UpdateEquipmentRequest): Observable<EquipmentResponse> {
    return this.http.patch<EquipmentResponse>(`${this.endpoint}/equipment/${id}`, input);
  }

  /** Transition d'état. Le serveur refuse toute sortie de `RETIRED` (terminal). */
  setStatus(id: string, target: EquipmentStatus): Observable<EquipmentResponse> {
    return this.http.post<EquipmentResponse>(`${this.endpoint}/equipment/${id}/status/${target}`, {});
  }

  deleteEquipment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/equipment/${id}`);
  }

  summary(id: string): Observable<EquipmentSummary> {
    return this.http.get<EquipmentSummary>(`${this.endpoint}/equipment/${id}/summary`);
  }

  // ---- Plan ------------------------------------------------------------------

  /**
   * Le serveur répond 404 quand l'équipement n'a pas encore de plan : ce n'est pas
   * une erreur mais un état normal de l'écran, donc traduit en `null`. Les autres
   * statuts (403, 500…) restent propagés pour être affichés à l'utilisateur.
   */
  getPlan(equipmentId: string): Observable<PlanResponse | null> {
    return this.http.get<PlanResponse>(`${this.endpoint}/equipment/${equipmentId}/plan`).pipe(
      catchError((err: unknown) => {
        if ((err as HttpErrorResponse)?.status === 404) return of(null);
        return throwError(() => err);
      })
    );
  }

  upsertPlan(equipmentId: string, input: UpsertPlanRequest): Observable<PlanResponse> {
    return this.http.put<PlanResponse>(`${this.endpoint}/equipment/${equipmentId}/plan`, input);
  }

  deletePlan(equipmentId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/equipment/${equipmentId}/plan`);
  }

  /** Plans dont `nextDueOn` précède `cutoff` (par défaut : aujourd'hui côté serveur). */
  overduePlans(cutoff?: string, page = 0, size = 50): Observable<PlanPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (cutoff) params = params.set('cutoff', cutoff);
    return this.http.get<PlanPage>(`${this.endpoint}/plans/overdue`, { params });
  }

  // ---- Enregistrements -------------------------------------------------------

  listRecords(equipmentId: string, page = 0, size = 100): Observable<RecordPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<RecordPage>(`${this.endpoint}/equipment/${equipmentId}/records`, { params });
  }

  addRecord(equipmentId: string, input: CreateRecordRequest): Observable<RecordResponse> {
    return this.http.post<RecordResponse>(`${this.endpoint}/equipment/${equipmentId}/records`, input);
  }

  // ---- MSA -------------------------------------------------------------------

  listMsa(equipmentId: string, page = 0, size = 100): Observable<MsaPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<MsaPage>(`${this.endpoint}/equipment/${equipmentId}/msa`, { params });
  }

  addMsa(equipmentId: string, input: CreateMsaRequest): Observable<MsaResponse> {
    return this.http.post<MsaResponse>(`${this.endpoint}/equipment/${equipmentId}/msa`, input);
  }

  // ---- Vues composées --------------------------------------------------------

  /**
   * Vue de la liste : la page d'équipements, enrichie des échéances à risque.
   *
   * Deux appels sont nécessaires parce que `GET /equipment` ne porte aucune date de
   * calibration : `GET /plans/overdue?cutoff=aujourd'hui+horizon` est la seule route
   * qui expose les échéances en masse. Les plans rapportés sont joints par
   * `equipmentId`, et les lignes triées « en retard d'abord » — l'écran doit faire
   * sauter aux yeux ce qui est expiré, pas respecter l'ordre d'insertion.
   */
  overview(status: EquipmentStatus | null, page: number, size: number,
           horizonDays: number): Observable<CalibrationOverview> {
    const cutoff = toLocalDate(addDays(new Date(), horizonDays));
    return combineLatest([
      this.listEquipment(page, size, status ?? undefined),
      this.overduePlans(cutoff, 0, AT_RISK_PAGE_SIZE)
    ]).pipe(
      map(([equipmentPage, atRisk]) => {
        const byEquipment = new Map(atRisk.content.map(p => [p.equipmentId, p]));
        const rows: EquipmentRow[] = equipmentPage.content
          .map(equipment => {
            const plan = byEquipment.get(equipment.id) ?? null;
            return { equipment, plan, due: dueStateOf(plan) };
          })
          .sort((a, b) => DUE_RANK[a.due] - DUE_RANK[b.due]
            || a.equipment.code.localeCompare(b.equipment.code));
        const overdueCount = atRisk.content.filter(p => p.overdue).length;
        return {
          page: equipmentPage,
          rows,
          overdueCount,
          dueSoonCount: atRisk.content.length - overdueCount
        };
      })
    );
  }

  /**
   * Vue du détail : l'équipement, sa synthèse, son plan et ses historiques.
   * `forkJoin` plutôt que des appels séquentiels — les cinq routes sont
   * indépendantes et l'écran n'a de sens qu'assemblé.
   */
  detail(equipmentId: string): Observable<EquipmentDetail> {
    return forkJoin({
      equipment: this.getEquipment(equipmentId),
      summary: this.summary(equipmentId),
      plan: this.getPlan(equipmentId),
      records: this.listRecords(equipmentId).pipe(map(p => p.content)),
      msa: this.listMsa(equipmentId).pipe(map(p => p.content))
    });
  }
}

/** Ordre d'affichage : ce qui est expiré passe devant tout le reste. */
const DUE_RANK: Record<DueState, number> = { OVERDUE: 0, DUE_SOON: 1, OK: 2 };

/**
 * Le retard n'est jamais recalculé côté navigateur : l'horloge du poste peut être
 * fausse ou dans un autre fuseau. On lit le drapeau `overdue` du serveur.
 */
export function dueStateOf(plan: PlanResponse | null): DueState {
  if (!plan) return 'OK';
  return plan.overdue ? 'OVERDUE' : 'DUE_SOON';
}

export function addDays(from: Date, days: number): Date {
  const d = new Date(from.getTime());
  d.setDate(d.getDate() + days);
  return d;
}

/**
 * Format `LocalDate` attendu par le serveur (ISO_DATE). Construit depuis les champs
 * LOCAUX : `toISOString()` bascule en UTC et décalerait la date d'un jour pour les
 * fuseaux à l'est de Greenwich en soirée.
 */
export function toLocalDate(d: Date): string {
  const month = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}
