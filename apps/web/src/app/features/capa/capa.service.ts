import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CapaActionResponse,
  CapaCaseResponse,
  ClosureBlocker,
  CapaEvidence,
  CapaPage,
  CapaCriticity,
  CapaStatus,
  CreateCapaActionRequest,
  OpenCapaRequest,
  UpdateCapaActionRequest,
  UpdateCapaCaseRequest,
  CapaEffectivenessSummary,
  EffectivenessReview,
  EvaluationBoard,
  EvaluationRow,
  RecordEffectivenessRequest
} from './capa.types';

@Injectable({ providedIn: 'root' })
export class CapaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/capa/cases`;
  private readonly effectivenessEndpoint = `${environment.apiBaseUrl}/api/v1/capa/effectiveness`;

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

  listCases(page = 0, size = 50, status?: CapaStatus, criticity?: CapaCriticity): Observable<CapaPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status, criticity)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (criticity) params = params.set('criticity', criticity);
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

  /**
   * Ouvre un dossier ET son action unique, en une seule écriture (ADR 0088).
   *
   * Création libre depuis la liste : la source est celle choisie, jamais
   * `RISK` (le serveur la refuse — seul le registre la pose). Depuis une NC, un
   * constat ou un risque, l'écran d'origine passe par sa propre route.
   */
  openCase(input: OpenCapaRequest): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      return of(this.mockOpen(input, input.sourceType ?? 'INTERNAL', input.sourceRef)).pipe(delay(200));
    }
    return this.http.post<CapaCaseResponse>(`${this.endpoint}/open`, input);
  }

  /**
   * Le dossier tel que le serveur l'ouvre, pour le mode démonstration — et
   * pour les écrans d'origine (NC, audit) qui le simulent : même action unique,
   * même pilote, de sorte que la démonstration dise ce que dira la production.
   */
  mockOpen(input: OpenCapaRequest, sourceType: CapaCaseResponse['sourceType'], sourceRef?: string): CapaCaseResponse {
    const now = new Date().toISOString();
    const id = 'capa-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7);
    const c: CapaCaseResponse = {
      id,
      tenantId: 'demo-tenant',
      title: input.title,
      description: input.description,
      type: input.type,
      criticity: input.criticity,
      status: 'OPEN',
      sourceType,
      sourceRef,
      ownerId: input.ownerId,
      ownerName: input.ownerName,
      dueDate: input.dueDate,
      // La décision de vérification part avec l'ouverture, comme côté serveur.
      verificationRequired: input.verificationRequired,
      verificationAssigneeId: input.verificationRequired ? input.verificationAssigneeId : undefined,
      verificationAssigneeName: input.verificationRequired ? input.verificationAssigneeName : undefined,
      verificationInstructions: input.verificationRequired ? input.verificationInstructions : undefined,
      createdAt: now,
      updatedAt: now,
      actions: [{
        id: 'act-' + Math.random().toString(36).slice(2, 9),
        capaId: id,
        title: input.title,
        status: 'PENDING',
        actionType: input.type,
        assigneeId: input.ownerId,
        assigneeName: input.ownerName,
        decidedOn: now.slice(0, 10),
        dueDate: input.dueDate
      }]
    };
    this.mockStore.unshift(c);
    return c;
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
        // Même règle que le serveur, sinon le mode démonstration mentirait sur
        // ce que fera la vraie API : ne plus exiger efface ce qui n'a plus d'objet.
        if (input.verificationRequired === false) {
          c.verificationRequired = false;
          c.verificationAssigneeId = undefined;
          c.verificationAssigneeName = undefined;
          c.verificationInstructions = undefined;
        } else {
          if (input.verificationRequired !== undefined) {
            c.verificationRequired = input.verificationRequired;
          }
          if (input.verificationAssigneeId !== undefined) {
            c.verificationAssigneeId = input.verificationAssigneeId;
          }
          if (input.verificationAssigneeName !== undefined) {
            c.verificationAssigneeName = input.verificationAssigneeName;
          }
          if (input.verificationInstructions !== undefined) {
            c.verificationInstructions = input.verificationInstructions;
          }
        }
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

  /**
   * Définit l'action unique d'un dossier qui n'en a pas encore — ancien, ou
   * ouvert par un automate (ADR 0088). Le serveur refuse une seconde action
   * (409) ; le responsable désigné devient le pilote du dossier.
   */
  addAction(caseId: string, input: CreateCapaActionRequest): Observable<CapaActionResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === caseId);
      if (c && c.actions.length > 0) {
        return throwError(() => new HttpErrorResponse({
          status: 409, statusText: 'Conflict',
          error: { title: 'Invalid CAPA State', detail: 'A CAPA carries a single action' }
        }));
      }
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
        if (input.assigneeId) {
          c.ownerId = input.assigneeId;
          c.ownerName = input.assigneeName;
        }
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

  // Ni `startCase` ni `resolveCase` : le statut du dossier se déduit de ses
  // actions (ADR 0117), et les routes serveur ont disparu avec les deux
  // boutons qui ne décidaient rien.

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

  private mockPage(status?: CapaStatus, criticity?: CapaCriticity): CapaPage {
    const filtered = this.mockStore.filter(c => (!status || c.status === status) && (!criticity || c.criticity === criticity));
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
        ownerId: 'demo-user', ownerName: 'Demo User', dueDate: '2026-05-30',
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
        // serveur, où le statut se déduit des actions et où toutes sont faites
        // (ADR 0117) ; seul cas de la démonstration où le bouton de clôture
        // s'allume.
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

  /**
   * L'efficacite mesuree des dossiers clos.
   *
   * <p>La fenetre est un parametre : six mois est l'usage, mais un procede
   * lent — traitement thermique, cycle agricole — demande plus long, et un
   * poste cadence se juge en trois mois.
   */
  effectiveness(months = 6): Observable<CapaEffectivenessSummary> {
    if (environment.useMockApi) {
      return of(this.mockEffectiveness(months)).pipe(delay(150));
    }
    const params = new HttpParams().set('months', months);
    return this.http.get<CapaEffectivenessSummary>(this.effectivenessEndpoint, { params });
  }

  /**
   * Le plan de travail des évaluations (ADR 0108).
   *
   * Aucun paramètre, et c'est voulu : le tri par urgence remplace un filtre de
   * statut. Ce qui est à juger arrive en tête, et personne n'a à savoir quel
   * statut demander pour l'obtenir.
   */
  evaluationBoard(): Observable<EvaluationBoard> {
    if (environment.useMockApi) {
      return of(this.mockEvaluationBoard()).pipe(delay(150));
    }
    return this.http.get<EvaluationBoard>(`${this.effectivenessEndpoint}/board`);
  }

  /**
   * L'historique complet des évaluations d'un dossier, du plus récent au plus ancien.
   *
   * Demandé dossier par dossier et non avec le tableau : afficher tous les
   * historiques chargerait, pour un écran, une table qui ne cesse de grossir.
   */
  evaluationHistory(capaId: string): Observable<EffectivenessReview[]> {
    if (environment.useMockApi) {
      const row = this.mockEvaluationBoard().rows.find(r => r.capaId === capaId);
      return of(row?.lastReview ? [row.lastReview] : []).pipe(delay(120));
    }
    return this.http.get<EffectivenessReview[]>(
      `${this.effectivenessEndpoint}/cases/${capaId}/reviews`);
  }

  /**
   * Rend un verdict d'efficacité sur un dossier.
   *
   * Distinct de `verifyEffectiveness`, qui porte l'ancien booléen et reste
   * utilisé par la fiche du dossier : les deux formes vivent côte à côte côté
   * serveur, et en fusionner une dans l'autre obligerait l'un des deux écrans à
   * envoyer des champs qu'il ne connaît pas.
   */
  recordEffectiveness(
    capaId: string,
    input: RecordEffectivenessRequest
  ): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === capaId) ?? this.mockStore[0];
      const now = new Date().toISOString();
      c.effectivenessVerdict = input.verdict;
      c.effectivenessVerified = input.verdict === 'EFFECTIVE';
      c.effectivenessVerifiedAt = now;
      c.updatedAt = now;
      if (input.verdict === 'EFFECTIVE') {
        c.status = 'CLOSED';
        c.closedAt = now;
      } else {
        c.status = 'IN_PROGRESS';
        c.resolvedAt = undefined;
      }
      return of(c).pipe(delay(140));
    }
    return this.http.patch<CapaCaseResponse>(
      `${this.endpoint}/${capaId}/effectiveness`, input);
  }

  /**
   * Un plan de travail de démonstration qui montre les cinq étapes.
   *
   * Les quatre chiffres sont calculés depuis les lignes, et non écrits à la
   * main : une démonstration dont l'en-tête contredit le tableau apprend à
   * l'utilisateur à ne pas faire confiance à l'en-tête.
   */
  private mockEvaluationBoard(): EvaluationBoard {
    const rows: EvaluationRow[] = [
      {
        capaId: this.mockStore[0]?.id ?? 'demo-1',
        reference: 'NC-2026-0024', title: 'Référence erronée sur 3 livraisons',
        criticity: 'HIGH', ownerName: 'M. Alaoui',
        stage: 'AWAITING_EVALUATION', evaluable: true,
        actionsTotal: 1, actionsDone: 1,
        criterion: 'Zéro erreur de référence sur 90 jours',
        reviewCount: 0, closureBlocked: false, containmentOnly: false, openNcCount: 0,
        updatedAt: new Date(Date.now() - 86_400_000).toISOString()
      },
      {
        capaId: this.mockStore[1]?.id ?? 'demo-2',
        reference: 'NC-2026-0027', title: 'Rebuts élevés sur la ligne 2',
        criticity: 'CRITICAL', ownerName: 'K. Idrissi',
        stage: 'COMPLEMENTARY_ACTION_EXPECTED', evaluable: false,
        actionsTotal: 1, actionsDone: 1,
        criterion: 'Taux de rebut ≤ 2 % sur 3 mois',
        lastReview: {
          id: 'demo-rev-1', verdict: 'PARTIAL',
          criterion: 'Taux de rebut ≤ 2 % sur 3 mois',
          baseline: '4,2 %', target: '≤ 2 %', observed: '3,1 %',
          comment: 'Le recalibrage a réduit le rebut sans atteindre la cible.',
          reviewerName: 'S. Benali',
          reviewedAt: new Date(Date.now() - 5 * 86_400_000).toISOString()
        },
        reviewCount: 1, closureBlocked: false, containmentOnly: false, openNcCount: 0,
        updatedAt: new Date(Date.now() - 5 * 86_400_000).toISOString()
      },
      {
        capaId: this.mockStore[2]?.id ?? 'demo-3',
        reference: 'NC-2026-0031', title: 'Écart de calibrage balance',
        criticity: 'MEDIUM', ownerName: 'N. Chraibi',
        stage: 'REOPENED', evaluable: false,
        actionsTotal: 1, actionsDone: 0,
        criterion: 'Aucun écart lors de 2 contrôles',
        lastReview: {
          id: 'demo-rev-2', verdict: 'INEFFECTIVE',
          criterion: 'Aucun écart lors de 2 contrôles',
          baseline: '0,8 g', target: '≤ 0,1 g', observed: '0,6 g',
          reviewerName: 'N. Chraibi',
          reviewedAt: new Date(Date.now() - 12 * 86_400_000).toISOString()
        },
        reviewCount: 2, closureBlocked: true, containmentOnly: true, openNcCount: 1,
        updatedAt: new Date(Date.now() - 12 * 86_400_000).toISOString()
      },
      {
        capaId: 'demo-4',
        reference: 'NC-2026-0033', title: 'Étiquetage incomplet en expédition',
        criticity: 'LOW', ownerName: 'L. Tazi',
        stage: 'ACTIONS_IN_PROGRESS', evaluable: false,
        actionsTotal: 1, actionsDone: 0,
        reviewCount: 0, closureBlocked: false, containmentOnly: false, openNcCount: 0,
        updatedAt: new Date(Date.now() - 20 * 86_400_000).toISOString()
      },
      {
        capaId: 'demo-5',
        reference: 'NC-2026-0019', title: 'Fournisseur hors délai (3 retards)',
        criticity: 'MEDIUM', ownerName: 'L. Tazi',
        stage: 'CLOSED_EFFECTIVE', evaluable: false,
        actionsTotal: 1, actionsDone: 1,
        criterion: 'Zéro retard sur 6 mois',
        lastReview: {
          id: 'demo-rev-3', verdict: 'EFFECTIVE',
          criterion: 'Zéro retard sur 6 mois',
          baseline: '3 retards', target: '0', observed: '0 retard',
          comment: 'Aucun retard depuis 6 mois.',
          reviewerName: 'L. Tazi',
          reviewedAt: new Date(Date.now() - 40 * 86_400_000).toISOString()
        },
        reviewCount: 1, closureBlocked: false, containmentOnly: false, openNcCount: 0,
        updatedAt: new Date(Date.now() - 40 * 86_400_000).toISOString()
      }
    ];
    const evaluated = rows.filter(r => r.lastReview).length;
    const effective = rows.filter(r => r.lastReview?.verdict === 'EFFECTIVE').length;
    return {
      effectiveRatePercent: evaluated ? Math.round((100 * effective) / evaluated) : undefined,
      evaluated,
      awaitingEvaluation: rows.filter(r => r.stage === 'AWAITING_EVALUATION').length,
      stillOpen: rows.filter(r => r.stage !== 'CLOSED_EFFECTIVE').length,
      reopened: rows.filter(r => r.stage === 'REOPENED').length,
      partial: rows.filter(r => r.stage === 'COMPLEMENTARY_ACTION_EXPECTED').length,
      missingCriterion: rows.filter(r => r.stage !== 'CLOSED_EFFECTIVE' && !r.criterion).length,
      total: rows.length,
      truncated: false,
      rows
    };
  }

  private mockEffectiveness(months: number): CapaEffectivenessSummary {
    const rows = this.mockStore.slice(0, 3).map((c, i) => ({
      capaId: c.id,
      title: c.title,
      criticity: c.criticity,
      closedAt: c.closedAt ?? c.createdAt,
      status: (i === 2 ? 'IN_OBSERVATION' : 'MEASURED') as 'IN_OBSERVATION' | 'MEASURED',
      occurrencesBefore: 4 - i,
      occurrencesAfter: i,
      ratePercent: i === 2 ? undefined : 100 - i * 40,
      aggravated: false,
      daysObserved: i === 2 ? 60 : months * 30,
      daysInWindow: months * 30,
      declaredEffective: i === 1,
      preciseMatch: i !== 1
    }));
    return {
      windowMonths: months, measured: 2, inObservation: 1, notMeasurable: 0,
      averageRatePercent: 80, aggravated: 0, declaredButFailed: 0,
      truncated: false, rows
    };
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
