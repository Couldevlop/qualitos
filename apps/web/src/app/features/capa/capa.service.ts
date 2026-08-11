import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CapaActionResponse,
  CapaCaseResponse,
  ClosureBlocker,
  CapaEvidence,
  CapaPage,
  CapaStatus,
  CreateCapaActionRequest,
  CreateCapaCaseRequest,
  SuggestedAction,
  UpdateCapaActionRequest,
  UpdateCapaCaseRequest
} from './capa.types';

@Injectable({ providedIn: 'root' })
export class CapaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  private readonly mockStore: CapaCaseResponse[] = this.seedMockCases();

  /** Pièces de preuve du mode démonstration, par dossier. */
  private readonly mockEvidences = new Map<string, CapaEvidence[]>();

  /**
   * Pièces d'ACTIONS du mode démonstration, rangées à part.
   *
   * Deux réserves distinctes et non un filtre sur une seule : côté serveur les
   * deux niveaux se lisent par deux routes, et une réserve unique ferait
   * remonter les pièces d'actions dans le bloc « Preuves » du dossier — le
   * défaut exact que le filtre serveur existe pour éviter.
   */
  private readonly mockActionEvidences = new Map<string, CapaEvidence[]>();

  constructor(private readonly http: HttpClient) {}

  listCases(page = 0, size = 50, status?: CapaStatus): Observable<CapaPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<CapaPage>(this.endpoint, { params });
  }

  getCase(id: string): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(c => c.id === id);
      const dossier = found ?? this.mockStore[0];
      // Les obstacles se CALCULENT à la lecture, exactement comme le serveur les
      // calcule sur la fiche. Les stocker dans le jeu d'essai les figerait : on
      // pourrait mener toutes les actions à DONE et le bandeau continuerait
      // d'annoncer « 2 actions restent à terminer », bouton éteint pour toujours.
      return of({ ...dossier, closureBlockers: mockClosureBlockers(dossier) }).pipe(delay(120));
    }
    return this.http.get<CapaCaseResponse>(`${this.endpoint}/${id}`);
  }

  createCase(input: CreateCapaCaseRequest): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const c: CapaCaseResponse = {
        id: 'capa-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        title: input.title,
        description: input.description,
        type: input.type,
        criticity: input.criticity,
        status: 'OPEN',
        sourceType: input.sourceType,
        sourceRef: input.sourceRef,
        ownerId: input.ownerId,
        dueDate: input.dueDate,
        createdAt: now,
        updatedAt: now,
        actions: []
      };
      this.mockStore.unshift(c);
      return of(c).pipe(delay(200));
    }
    return this.http.post<CapaCaseResponse>(this.endpoint, input);
  }

  updateCase(id: string, input: UpdateCapaCaseRequest): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        if (input.title !== undefined) c.title = input.title;
        if (input.description !== undefined) c.description = input.description;
        if (input.criticity !== undefined) c.criticity = input.criticity;
        if (input.sourceRef !== undefined) c.sourceRef = input.sourceRef;
        if (input.dueDate !== undefined) c.dueDate = input.dueDate;
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CapaCaseResponse>(`${this.endpoint}/${id}`, input);
  }

  deleteCase(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(c => c.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  addAction(caseId: string, input: CreateCapaActionRequest): Observable<CapaActionResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === caseId);
      const action: CapaActionResponse = {
        id: 'act-' + Math.random().toString(36).slice(2, 9),
        capaId: caseId,
        title: input.title,
        status: input.status ?? 'PENDING',
        // Même défaut explicite que côté serveur : une action non qualifiée est
        // corrective, et la démonstration doit dire la même chose que la production.
        actionType: input.actionType ?? 'CORRECTIVE',
        assigneeId: input.assigneeId,
        assigneeName: input.assigneeName,
        // Même déduction explicite que côté serveur : à défaut de date saisie,
        // c'est le jour de l'enregistrement — jamais un champ technique renommé.
        decidedOn: input.decidedOn ?? new Date().toISOString().slice(0, 10),
        dueDate: input.dueDate
      };
      if (c) {
        c.actions = [...c.actions, action];
        c.updatedAt = new Date().toISOString();
      }
      return of(action).pipe(delay(120));
    }
    return this.http.post<CapaActionResponse>(`${this.endpoint}/${caseId}/actions`, input);
  }

  /**
   * Met à jour une action (§4.2) — avancement de statut, ou édition en ligne du
   * libellé depuis le tableau.
   *
   * C'est un PATCH : seuls les champs envoyés changent. L'appelant n'a donc pas
   * à renvoyer le libellé pour faire avancer un statut, ce qui évitait
   * jusqu'ici d'écraser une correction faite entre-temps par quelqu'un d'autre.
   */
  updateAction(caseId: string, actionId: string, input: UpdateCapaActionRequest): Observable<CapaActionResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === caseId);
      const a = c?.actions.find(x => x.id === actionId);
      if (a) {
        if (input.status !== undefined) a.status = input.status;
        if (input.title !== undefined) a.title = input.title;
        if (input.actionType !== undefined) a.actionType = input.actionType;
        if (input.assigneeName !== undefined) a.assigneeName = input.assigneeName;
        if (input.decidedOn !== undefined) a.decidedOn = input.decidedOn;
        if (input.status === 'DONE') a.completedAt = new Date().toISOString();
        if (c) c.updatedAt = new Date().toISOString();
      }
      return of(a ?? {
        id: actionId, capaId: caseId, title: input.title ?? '',
        status: input.status ?? 'PENDING',
        actionType: input.actionType ?? 'CORRECTIVE'
      }).pipe(delay(120));
    }
    return this.http.patch<CapaActionResponse>(`${this.endpoint}/${caseId}/actions/${actionId}`, input);
  }

  /** Suggestions d'actions correctives/préventives par l'IA (via api-quality-engine → ai-service). §4.2 */
  suggestActions(caseId: string): Observable<SuggestedAction[]> {
    if (environment.useMockApi) {
      return of(<SuggestedAction[]>[
        { title: 'Auditer le fournisseur sur site et revoir le PPAP' },
        { title: 'Renforcer le plan de contrôle réception (échantillonnage 100%)' },
        { title: 'Mettre à jour la fiche de non-conformité et informer les parties prenantes' },
        { title: 'Former les opérateurs au nouveau critère de contrôle' }
      ]).pipe(delay(500));
    }
    return this.http.post<SuggestedAction[]>(`${this.endpoint}/${caseId}/suggest-actions`, {});
  }

  verifyEffectiveness(id: string, effective: boolean): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.effectivenessVerified = effective;
        c.updatedAt = now;
        if (effective) c.status = 'CLOSED';
        c.closedAt = effective ? now : c.closedAt;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CapaCaseResponse>(
      `${this.endpoint}/${id}/effectiveness`,
      { effective }
    );
  }

  // ---- preuves du dossier (§4.2, ISO 9001 §10.2) ------------------------------
  // Une CAPA se clôt sur une vérification d'efficacité, et l'efficacité se
  // prouve. Les pièces se rattachent au dossier, jamais à une action isolée.

  listEvidences(caseId: string): Observable<CapaEvidence[]> {
    if (environment.useMockApi) {
      return of([...this.mockEvidenceStore(caseId)]).pipe(delay(120));
    }
    return this.http.get<CapaEvidence[]>(`${this.endpoint}/${caseId}/evidences`);
  }

  /** Dépose une pièce (multipart, champ 'file'). 201 → métadonnées de la pièce. */
  uploadEvidence(caseId: string, file: File): Observable<CapaEvidence> {
    if (environment.useMockApi) {
      // En démonstration, la pièce reste dans l'onglet : l'URL d'objet rend le
      // fichier réellement consultable, plutôt que de simuler une preuve.
      const evidence: CapaEvidence = {
        id: 'evd-' + Math.random().toString(36).slice(2, 9),
        capaId: caseId,
        contentType: file.type || 'application/octet-stream',
        sizeBytes: file.size,
        originalFilename: file.name,
        createdAt: new Date().toISOString(),
        url: URL.createObjectURL(file)
      };
      this.mockEvidenceStore(caseId).push(evidence);
      return of(evidence).pipe(delay(250));
    }
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<CapaEvidence>(`${this.endpoint}/${caseId}/evidences`, form);
  }

  deleteEvidence(caseId: string, evidenceId: string): Observable<void> {
    if (environment.useMockApi) {
      const store = this.mockEvidenceStore(caseId);
      const idx = store.findIndex(e => e.id === evidenceId);
      if (idx >= 0) {
        // Libère l'URL d'objet : sans cela le binaire reste en mémoire jusqu'au
        // rechargement de la page.
        const url = store[idx].url;
        if (url) URL.revokeObjectURL(url);
        store.splice(idx, 1);
      }
      return of(void 0).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${caseId}/evidences/${evidenceId}`);
  }

  // ---- preuves d'ACTION (§4.2, ADR 0052) --------------------------------------
  // Mêmes bornes et mêmes refus que les preuves de dossier ; ce qui change,
  // c'est le chemin — et une action ne porte qu'UNE pièce, parce qu'une cellule
  // de tableau montre un document, pas une liste.

  /** Toutes les pièces d'actions du dossier, en un appel : le tableau les range ensuite. */
  listActionEvidences(caseId: string): Observable<CapaEvidence[]> {
    if (environment.useMockApi) {
      return of([...this.mockActionEvidenceStore(caseId)]).pipe(delay(120));
    }
    return this.http.get<CapaEvidence[]>(`${this.endpoint}/${caseId}/action-evidences`);
  }

  uploadActionEvidence(caseId: string, actionId: string, file: File): Observable<CapaEvidence> {
    if (environment.useMockApi) {
      const evidence: CapaEvidence = {
        id: 'evd-act-' + Math.random().toString(36).slice(2, 9),
        capaId: caseId,
        actionId,
        contentType: file.type || 'application/octet-stream',
        sizeBytes: file.size,
        originalFilename: file.name,
        createdAt: new Date().toISOString(),
        url: URL.createObjectURL(file)
      };
      this.mockActionEvidenceStore(caseId).push(evidence);
      return of(evidence).pipe(delay(250));
    }
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<CapaEvidence>(
      `${this.endpoint}/${caseId}/actions/${actionId}/evidences`, form);
  }

  deleteActionEvidence(caseId: string, actionId: string, evidenceId: string): Observable<void> {
    if (environment.useMockApi) {
      const store = this.mockActionEvidenceStore(caseId);
      const idx = store.findIndex(e => e.id === evidenceId);
      if (idx >= 0) {
        // Libère l'URL d'objet : sans cela le binaire reste en mémoire jusqu'au
        // rechargement de la page.
        const url = store[idx].url;
        if (url) URL.revokeObjectURL(url);
        store.splice(idx, 1);
      }
      return of(void 0).pipe(delay(120));
    }
    return this.http.delete<void>(
      `${this.endpoint}/${caseId}/actions/${actionId}/evidences/${evidenceId}`);
  }

  private mockActionEvidenceStore(caseId: string): CapaEvidence[] {
    let store = this.mockActionEvidences.get(caseId);
    if (!store) {
      store = [];
      this.mockActionEvidences.set(caseId, store);
    }
    return store;
  }

  private mockEvidenceStore(caseId: string): CapaEvidence[] {
    let store = this.mockEvidences.get(caseId);
    if (!store) {
      store = [];
      this.mockEvidences.set(caseId, store);
    }
    return store;
  }

  startCase(id: string): Observable<CapaCaseResponse> {
    return this.transition(id, 'IN_PROGRESS', 'start');
  }

  resolveCase(id: string): Observable<CapaCaseResponse> {
    return this.transition(id, 'RESOLVED', 'resolve');
  }

  rejectCase(id: string): Observable<CapaCaseResponse> {
    return this.transition(id, 'REJECTED', 'reject');
  }

  private transition(
    id: string,
    targetStatus: CapaStatus,
    pathSegment: 'start' | 'resolve' | 'reject'
  ): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        c.status = targetStatus;
        c.updatedAt = new Date().toISOString();
        if (targetStatus === 'RESOLVED') c.resolvedAt = c.updatedAt;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CapaCaseResponse>(`${this.endpoint}/${id}/${pathSegment}`, {});
  }

  private mockPage(status?: CapaStatus): CapaPage {
    const filtered = status ? this.mockStore.filter(c => c.status === status) : this.mockStore;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private seedMockCases(): CapaCaseResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'capa-1', tenantId: 'demo-tenant',
        title: 'Recalibration robot soudure cobot-3',
        description: 'Suite NC répétée sur ligne 3.',
        type: 'CORRECTIVE', criticity: 'HIGH', status: 'IN_PROGRESS',
        sourceType: 'NON_CONFORMITY', sourceRef: 'NC-2026-018',
        ownerId: 'demo-user', dueDate: '2026-05-30',
        // L'écart d'origine et le plan d'actions sont garnis : un dossier de
        // démonstration sans action ne montrait jamais le tableau, donc jamais
        // ce qu'il sait faire. Les dates de décision précèdent délibérément les
        // échéances — c'est la distinction que la colonne « Date » porte.
        sourceNonConformity: {
          id: 'nc-2026-018',
          reference: 'NC-2026-018',
          title: 'Cordons de soudure hors tolérance sur la ligne 3'
        },
        createdAt: now, updatedAt: now,
        // Le jeu de démonstration porte les TROIS natures : sans mesure
        // d'endiguement, l'écran ne montrerait jamais ce que la colonne distingue.
        actions: [
          {
            id: 'act-demo-0', capaId: 'capa-1',
            title: 'Bloquer et trier les lots soudés depuis le 12 avril',
            status: 'DONE', actionType: 'CONTAINMENT', assigneeName: 'Karim Benali',
            decidedOn: '2026-04-13', dueDate: '2026-04-16',
            completedAt: '2026-04-15T09:10:00Z'
          },
          {
            id: 'act-demo-1', capaId: 'capa-1',
            title: 'Recalibrer la cellule de soudure et revalider les paramètres',
            status: 'DONE', actionType: 'CORRECTIVE', assigneeName: 'Amina Dridi',
            decidedOn: '2026-04-14', dueDate: '2026-05-02',
            completedAt: '2026-04-29T14:20:00Z'
          },
          {
            id: 'act-demo-2', capaId: 'capa-1',
            title: 'Renforcer le contrôle réception (échantillonnage 100 %)',
            status: 'IN_PROGRESS', actionType: 'CORRECTIVE', assigneeName: 'Marc Lefèvre',
            decidedOn: '2026-04-14', dueDate: '2026-05-20'
          },
          {
            id: 'act-demo-3', capaId: 'capa-1',
            title: 'Former les opérateurs au nouveau critère de contrôle',
            status: 'PENDING', actionType: 'PREVENTIVE', assigneeName: 'Sofia Marques',
            decidedOn: '2026-04-28', dueDate: '2026-06-10'
          }
        ]
      },
      {
        id: 'capa-2', tenantId: 'demo-tenant',
        title: 'Audit anti-fraude LCB-FT trimestriel — déficit contrôle KYC',
        type: 'PREVENTIVE', criticity: 'CRITICAL', status: 'OPEN',
        sourceType: 'AUDIT', sourceRef: 'AUD-2026-Q2',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now, actions: []
      },
      {
        id: 'capa-3', tenantId: 'demo-tenant',
        title: 'Mise à jour procédure stérilisation autoclave 4',
        type: 'CORRECTIVE', criticity: 'MEDIUM', status: 'RESOLVED',
        sourceType: 'INTERNAL',
        ownerId: 'demo-user',
        // RESOLVED avec une action faite : état réellement atteignable côté
        // serveur (`resolveCase` exige au moins une action, toutes DONE), et
        // seul cas de la démonstration où le bouton de clôture s'allume.
        resolvedAt: now, createdAt: now, updatedAt: now,
        actions: [
          {
            id: 'act-demo-9', capaId: 'capa-3',
            title: 'Réviser la procédure et requalifier le cycle',
            status: 'DONE', actionType: 'CORRECTIVE', assigneeName: 'Léa Bertin',
            decidedOn: '2026-05-04', completedAt: '2026-05-21T10:00:00Z'
          }
        ]
      }
    ];
  }
}

/**
 * Reproduit `CapaService.closureBlockers` du serveur pour le mode démonstration.
 *
 * Deux sources de vérité finiraient par se contredire à l'écran : ici, la
 * fonction suit la même règle et le même ORDRE que le serveur — actions
 * manquantes, puis endiguement seul, puis écarts ouverts — de sorte que la
 * démonstration montre exactement ce que la production montrera.
 *
 * Les non-conformités liées ne sont pas simulées : le magasin de démonstration
 * n'en porte pas. Leur absence produit une liste plus courte, jamais fausse.
 */
function mockClosureBlockers(c: CapaCaseResponse): ClosureBlocker[] {
  if (c.status === 'CLOSED' || c.status === 'REJECTED') {
    return [];
  }
  const blockers: ClosureBlocker[] = [];
  if (c.actions.length === 0) {
    blockers.push({ code: 'NO_ACTION', count: 0 });
    return blockers;
  }
  const notDone = c.actions.filter(a => a.status !== 'DONE').length;
  if (notDone > 0) {
    blockers.push({ code: 'ACTIONS_NOT_DONE', count: notDone });
  }
  if (c.actions.every(a => a.actionType === 'CONTAINMENT')) {
    blockers.push({ code: 'CONTAINMENT_ONLY', count: c.actions.length });
  }
  return blockers;
}
