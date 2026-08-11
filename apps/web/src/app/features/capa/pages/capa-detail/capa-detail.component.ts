import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { CapaService } from '../../capa.service';
import {
  CapaEditDialogComponent,
  CapaEditDialogData
} from '../capa-edit-dialog/capa-edit-dialog.component';
import { CapaActionResponse, CapaActionStatus, CapaActionType, CapaCaseResponse, CapaCriticity, CapaEvidence, CapaStatus, ClosureBlocker, SuggestedAction } from '../../capa.types';
import {
  CapaActionDialogComponent,
  CapaActionDialogData
} from '../capa-action-dialog/capa-action-dialog.component';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-capa-detail',
  templateUrl: './capa-detail.component.html',
  styleUrls: ['./capa-detail.component.scss'],
  standalone: false
})
export class CapaDetailComponent implements OnInit {

  /**
   * Ordre des colonnes = ordre de lecture d'un auditeur : ce qui a été décidé,
   * quand, par qui, sur quel écart, avec quelle preuve, et où ça en est.
   * L'avancement et l'édition partagent la dernière colonne — deux colonnes de
   * boutons repousseraient le contenu hors de l'écran.
   */
  readonly actionColumns = ['title', 'actionType', 'decidedOn', 'assignee', 'nonConformity',
                            'evidence', 'status', 'dueDate', 'rowActions'];

  /** Natures proposées, dans l'ordre du déroulé réel d'un traitement. */
  readonly actionTypes: { value: CapaActionType; label: string }[] = [
    { value: 'CONTAINMENT', label: $localize`:@@capa.action-type.containment:Endiguement` },
    { value: 'CORRECTIVE',  label: $localize`:@@capa.action-type.corrective:Corrective` },
    { value: 'PREVENTIVE',  label: $localize`:@@capa.action-type.preventive:Préventive` }
  ];

  readonly notFoundLabel = $localize`:@@capa.detail.not-found:Cas introuvable`;
  readonly analysingLabel = $localize`:@@capa.detail.analysing:Analyse…`;
  readonly suggestLabel = $localize`:@@capa.detail.suggest:Suggérer (IA)`;
  readonly addLabel = $localize`:@@common.add:Ajouter`;

  case$!: Observable<CapaCaseResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  // Suggestions d'actions par l'IA (non persistées tant que non ajoutées).
  suggestions: SuggestedAction[] = [];
  suggesting = false;
  addingKey: string | null = null;

  // --- preuves du dossier (§4.2, ISO 9001 §10.2) ------------------------------
  /** Bornes tenues par le serveur ; l'écran les énonce au lieu de les faire découvrir. */
  readonly maxEvidences = 10;
  readonly maxEvidenceBytes = 10 * 1024 * 1024;

  evidences$ = new BehaviorSubject<CapaEvidence[]>([]);
  evidencesLoading$ = new BehaviorSubject<boolean>(false);
  uploadingEvidence$ = new BehaviorSubject<boolean>(false);
  /** Vrai quand le serveur répond 503 : le stockage binaire est coupé sur cet environnement. */
  evidenceStorageDisabled$ = new BehaviorSubject<boolean>(false);
  /** Identifiant de la pièce en cours de retrait (désactive sa ligne). */
  removingEvidenceId$ = new BehaviorSubject<string | null>(null);

  // --- preuves d'ACTION (§4.2, ADR 0052) ---------------------------------------
  /** Une action ne porte qu'UNE pièce : la cellule montre un document, pas une liste. */
  readonly actionEvidences$ = new BehaviorSubject<Map<string, CapaEvidence>>(new Map());
  /** Action dont la pièce est en cours de dépôt ou de retrait (verrouille sa cellule). */
  readonly busyEvidenceActionId$ = new BehaviorSubject<string | null>(null);

  // --- édition en ligne (§4.2) ---------------------------------------------------
  // Le libellé et le statut se corrigent DANS le tableau. Un dialogue pour
  // changer deux champs ferait perdre la ligne de vue, et l'utilisateur corrige
  // justement en comparant aux lignes voisines.
  editingActionId: string | null = null;
  savingEdit = false;
  readonly editForm = this.fb.nonNullable.group({
    // `required` seul laisse passer une chaîne d'espaces — elle est « non
    // vide » au sens du validateur. Le serveur la refuse (400) ; l'écran doit
    // la refuser avant l'aller-retour, sinon l'utilisateur voit une erreur
    // technique là où il a simplement effacé un champ.
    title: ['', [Validators.required, CapaDetailComponent.notBlank, Validators.maxLength(255)]],
    status: <CapaActionStatus>'PENDING',
    // La nature se corrige en ligne comme le reste : c'est en relisant le tableau
    // qu'on s'aperçoit qu'une action rangée en « corrective » n'a fait que contenir.
    actionType: <CapaActionType>'CORRECTIVE'
  });

  /** Un libellé fait d'espaces n'est pas un libellé. */
  private static notBlank(control: AbstractControl): ValidationErrors | null {
    return typeof control.value === 'string' && control.value.trim().length === 0
      ? { required: true }
      : null;
  }

  readonly editableStatuses: CapaActionStatus[] = ['PENDING', 'IN_PROGRESS', 'DONE'];

  /**
   * Le champ de libellé prend le focus dès l'ouverture de l'édition.
   *
   * Sans cela, un utilisateur au clavier active « Modifier » et se retrouve
   * avec le focus sur un bouton qui a disparu : le point d'insertion repart en
   * tête de document et la ligne éditée devient introuvable. Le passage par un
   * setter de ViewChild est nécessaire parce que le champ n'existe pas encore
   * au moment du clic — il naît du rendu suivant.
   */
  private focusTitleOnRender = false;

  @ViewChild('inlineTitle')
  set inlineTitle(ref: ElementRef<HTMLInputElement> | undefined) {
    if (ref && this.focusTitleOnRender) {
      this.focusTitleOnRender = false;
      ref.nativeElement.focus();
    }
  }

  private caseId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Ancré des DEUX côtés et jeu de caractères restreint : un simple préfixe
  // laissait passer « capa-1/../../secrets » (traversée de chemin — OWASP A03).
  private isMockId(s: string): boolean { return /^capa-[a-z0-9-]{1,64}$/i.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly capa: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly fb: FormBuilder
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/capa']);
      return;
    }
    this.caseId = raw;
    this.case$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.capa.getCase(this.caseId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] getCase failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@capa.detail.case-not-found:Cas CAPA introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    this.loadEvidences();
    this.loadActionEvidences();
  }

  // --- preuves du dossier ------------------------------------------------------

  /** Chargement non bloquant : la fiche reste utilisable si les preuves manquent. */
  loadEvidences(): void {
    queueMicrotask(() => this.evidencesLoading$.next(true));
    this.capa.listEvidences(this.caseId)
      .pipe(finalize(() => this.evidencesLoading$.next(false)))
      .subscribe({
        next: evidences => {
          this.evidences$.next(evidences);
          this.evidenceStorageDisabled$.next(false);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] listEvidences failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); }
        }
      });
  }

  triggerEvidencePicker(input: HTMLInputElement): void {
    if (this.uploadingEvidence$.value) return;
    input.click();
  }

  onEvidenceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    // Réinitialise pour autoriser la re-sélection du même fichier.
    input.value = '';
    if (!file) return;
    this.uploadEvidence(file);
  }

  private uploadEvidence(file: File): void {
    this.uploadingEvidence$.next(true);
    this.capa.uploadEvidence(this.caseId, file)
      .pipe(finalize(() => this.uploadingEvidence$.next(false)))
      .subscribe({
        next: evidence => {
          this.evidences$.next([...this.evidences$.value, evidence]);
          this.evidenceStorageDisabled$.next(false);
          this.snack.open(
            $localize`:@@capa.evidence.added:Preuve ajoutée au dossier.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] uploadEvidence failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); return; }
          // Chaque refus a sa raison : les confondre reviendrait à dire « non »
          // sans dire quoi corriger.
          const msg =
            err?.status === 413
              ? $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
              : err?.status === 400
              ? $localize`:@@capa.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
              : err?.status === 409
              ? $localize`:@@capa.evidence.limit:Le dossier a atteint sa limite de preuves, ou il est clôturé.`
              : safeErrorMessage(err, $localize`:@@capa.evidence.error:Échec de l'ajout de la preuve.`);
          this.snack.open(msg, 'OK', { duration: 5000 });
        }
      });
  }

  removeEvidence(evidence: CapaEvidence): void {
    if (this.removingEvidenceId$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@capa.evidence.remove-title:Retirer cette preuve ?`,
        message: $localize`:@@capa.evidence.remove-message:La pièce sera définitivement retirée du dossier. Un dossier d'audit se justifie par ses preuves : ce retrait est traçable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.removingEvidenceId$.next(evidence.id);
      this.capa.deleteEvidence(this.caseId, evidence.id)
        .pipe(finalize(() => this.removingEvidenceId$.next(null)))
        .subscribe({
          next: () => {
            this.evidences$.next(this.evidences$.value.filter(e => e.id !== evidence.id));
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-detail] deleteEvidence failed', err?.status, err?.error?.type);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@capa.evidence.remove-error:Échec du retrait de la preuve.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  /** Le dépôt se ferme sur un dossier clos ou rejeté, comme pour les actions. */
  canAddEvidence(status: CapaStatus): boolean {
    return status !== 'CLOSED' && status !== 'REJECTED'
        && this.evidences$.value.length < this.maxEvidences;
  }

  /** Compteur affiché : l'utilisateur voit la borne approcher au lieu de la heurter. */
  evidenceCountLabel(): string {
    return this.evidences$.value.length + ' / ' + this.maxEvidences;
  }

  /** Poids lisible — les octets ne disent rien à personne. */
  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1).replace('.', ',') + ' Mo';
  }

  /** Icône par famille de document : on reconnaît un PDF d'un tableur d'un coup d'œil. */
  evidenceIcon(contentType: string): string {
    if (contentType === 'application/pdf') return 'picture_as_pdf';
    if (contentType.startsWith('image/')) return 'image';
    if (contentType.includes('spreadsheet')) return 'table_chart';
    if (contentType.includes('wordprocessing')) return 'description';
    return 'attach_file';
  }

  /**
   * 503 + ProblemDetail de type 'storage-disabled' → le stockage est coupé sur cet
   * environnement. Message dédié plutôt qu'une erreur brute qui ferait croire à un bug.
   */
  private isStorageDisabled(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('storage-disabled');
  }

  // --- preuves rattachées à une action (§4.2, ADR 0052) ------------------------

  /**
   * Chargement non bloquant, comme les preuves du dossier : le tableau reste
   * lisible même si le stockage est coupé — la colonne « Preuve » affiche alors
   * son état plutôt qu'une erreur en travers de la fiche.
   */
  loadActionEvidences(): void {
    this.capa.listActionEvidences(this.caseId).subscribe({
      next: list => {
        const byAction = new Map<string, CapaEvidence>();
        // Le serveur garantit une pièce par action ; on prend malgré tout la
        // dernière si deux arrivaient, plutôt que d'afficher une cellule vide.
        list.forEach(e => { if (e.actionId) byAction.set(e.actionId, e); });
        this.actionEvidences$.next(byAction);
        // Le drapeau « stockage coupé » n'est PAS remis à faux ici : les deux
        // listes partagent le même stockage, et cet appel-ci peut aboutir
        // pendant que l'autre a déjà diagnostiqué la coupure. L'effacer
        // reviendrait à contredire un diagnostic posé une milliseconde plus tôt.
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-detail] listActionEvidences failed', err?.status, err?.error?.type);
        if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); }
      }
    });
  }

  /** Pièce de cette action, ou undefined — c'est ce que la cellule affiche. */
  actionEvidence(actionId: string): CapaEvidence | undefined {
    return this.actionEvidences$.value.get(actionId);
  }

  /** Le dépôt suit le verrou du dossier : plus rien ne bouge une fois clos ou rejeté. */
  canAttachActionEvidence(actionId: string, caseStatus: CapaStatus): boolean {
    return !this.isTerminal(caseStatus)
        && !this.evidenceStorageDisabled$.value
        && !this.actionEvidence(actionId)
        && this.busyEvidenceActionId$.value !== actionId;
  }

  triggerActionEvidencePicker(input: HTMLInputElement, actionId: string): void {
    if (this.busyEvidenceActionId$.value) return;
    // L'input est partagé par toutes les lignes : sans mémoriser l'action visée,
    // le fichier choisi atterrirait sur la dernière ligne rendue.
    this.pendingEvidenceActionId = actionId;
    input.click();
  }

  onActionEvidenceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    input.value = '';
    const actionId = this.pendingEvidenceActionId;
    this.pendingEvidenceActionId = null;
    if (!file || !actionId) return;

    this.busyEvidenceActionId$.next(actionId);
    this.capa.uploadActionEvidence(this.caseId, actionId, file)
      .pipe(finalize(() => this.busyEvidenceActionId$.next(null)))
      .subscribe({
        next: evidence => {
          const next = new Map(this.actionEvidences$.value);
          next.set(actionId, evidence);
          this.actionEvidences$.next(next);
          this.snack.open(
            $localize`:@@capa.action-evidence.added:Preuve jointe à l'action.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] uploadActionEvidence failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); return; }
          this.snack.open(this.evidenceRefusal(err), 'OK', { duration: 5000 });
        }
      });
  }

  removeActionEvidence(actionId: string, evidence: CapaEvidence): void {
    if (this.busyEvidenceActionId$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@capa.action-evidence.remove-title:Retirer la preuve de cette action ?`,
        message: $localize`:@@capa.action-evidence.remove-message:La pièce sera définitivement retirée. Une action déclarée faite sans preuve s'affirme faite sans le démontrer : ce retrait est traçable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.busyEvidenceActionId$.next(actionId);
      this.capa.deleteActionEvidence(this.caseId, actionId, evidence.id)
        .pipe(finalize(() => this.busyEvidenceActionId$.next(null)))
        .subscribe({
          next: () => {
            const next = new Map(this.actionEvidences$.value);
            next.delete(actionId);
            this.actionEvidences$.next(next);
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-detail] deleteActionEvidence failed', err?.status, err?.error?.type);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@capa.evidence.remove-error:Échec du retrait de la preuve.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  /**
   * Traduction des refus de téléversement, partagée par les deux niveaux.
   * Chaque refus a sa raison : les confondre reviendrait à dire « non » sans
   * dire quoi corriger.
   */
  private evidenceRefusal(err: unknown): string {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    return e?.status === 413
      ? $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
      : e?.status === 400
      ? $localize`:@@capa.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
      : e?.status === 409
      ? $localize`:@@capa.action-evidence.limit:Cette action porte déjà sa preuve, ou le dossier est clôturé.`
      : e?.status === 404
      ? $localize`:@@capa.action-evidence.gone:Cette action n'existe plus — recharge la fiche.`
      : safeErrorMessage(err, $localize`:@@capa.evidence.error:Échec de l'ajout de la preuve.`);
  }

  /** Action visée par le sélecteur de fichier partagé entre toutes les lignes. */
  private pendingEvidenceActionId: string | null = null;

  // --- édition en ligne du libellé et du statut (§4.2) -------------------------

  /** Une ligne s'édite tant que le dossier vit et qu'aucune autre n'est ouverte. */
  canEditAction(caseStatus: CapaStatus): boolean {
    return !this.isTerminal(caseStatus) && !this.savingEdit;
  }

  isEditing(actionId: string): boolean {
    return this.editingActionId === actionId;
  }

  startEdit(a: CapaActionResponse): void {
    if (this.savingEdit) return;
    this.editingActionId = a.id;
    this.editForm.setValue({
      title: a.title,
      status: a.status,
      // Repli sur « corrective » pour les actions antérieures à la colonne :
      // c'est ce qu'elles sont en base, et laisser le contrôle vide requalifierait
      // l'action au premier enregistrement.
      actionType: a.actionType ?? 'CORRECTIVE'
    });
    this.focusTitleOnRender = true;
  }

  /** Sortie sans écrire : l'édition en ligne doit toujours être annulable. */
  cancelEdit(): void {
    if (this.savingEdit) return;
    this.editingActionId = null;
    this.editForm.reset({ title: '', status: 'PENDING', actionType: 'CORRECTIVE' });
  }

  /**
   * Échap annule, Entrée enregistre — l'édition se pilote au clavier seul, sans
   * viser deux boutons à la souris pour corriger un mot.
   */
  onEditKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelEdit();
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.saveEdit();
    }
  }

  saveEdit(): void {
    if (!this.editingActionId || this.savingEdit) return;
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const actionId = this.editingActionId;
    const { title, status, actionType } = this.editForm.getRawValue();
    this.savingEdit = true;
    // PATCH partiel : seuls le libellé, le statut et la nature partent. La date
    // de décision et le porteur, absents de la charge utile, restent intacts.
    this.capa.updateAction(this.caseId, actionId, { title: title.trim(), status, actionType })
      .pipe(finalize(() => (this.savingEdit = false)))
      .subscribe({
        next: () => {
          this.editingActionId = null;
          this.snack.open($localize`:@@capa.detail.action-updated:Action modifiée.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] saveEdit failed', err?.status, err?.error?.title);
          // La ligne RESTE en édition : refermer effacerait la saisie que
          // l'utilisateur doit justement corriger.
          this.snack.open(
            safeErrorMessage(err, $localize`:@@capa.detail.action-update-error:Modification impossible.`),
            $localize`:@@common.close:Fermer`, { duration: 4000 });
        }
      });
  }

  /**
   * Libellés d'accessibilité des boutons du tableau.
   *
   * Une icône seule ou un mot isolé (« Joindre », « × ») ne dit pas SUR QUOI il
   * agit : au lecteur d'écran, huit lignes rendent huit boutons « Joindre »
   * indiscernables. Le libellé de l'action est donc repris dans l'étiquette —
   * et traduit, parce qu'un lecteur d'écran arabe ou japonais lit ce texte.
   */
  attachEvidenceAria(title: string): string {
    return $localize`:@@capa.action-evidence.attach-aria:Joindre une preuve à l'action : ${title}:title:`;
  }

  removeEvidenceAria(title: string): string {
    return $localize`:@@capa.action-evidence.remove-aria:Retirer la preuve de l'action : ${title}:title:`;
  }

  editActionAria(title: string): string {
    return $localize`:@@capa.detail.edit-action-aria:Modifier l'action : ${title}:title:`;
  }

  /** Libellé traduit d'un statut, pour la liste déroulante d'édition. */
  actionStatusLabel(s: CapaActionStatus): string {
    return s === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : s === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  goBack(): void {
    this.router.navigate(['/capa']);
  }

  openEdit(c: CapaCaseResponse): void {
    const data: CapaEditDialogData = { capa: c };
    this.dialog
      .open(CapaEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deleteCase(title: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@capa.detail.delete-title:Supprimer ce cas CAPA ?`,
        message: $localize`:@@capa.detail.delete-message:« ${title}:title: » et toutes ses actions seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.capa.deleteCase(this.caseId).subscribe({
        next: () => {
          this.snack.open($localize`:@@capa.detail.deleted:Cas supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/capa']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
    });
  }

  openAddAction(): void {
    const data: CapaActionDialogData = { caseId: this.caseId };
    this.dialog
      .open(CapaActionDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(action => {
        if (action) this.reload$.next();
      });
  }

  // ---- Suggestion d'actions par l'IA (§4.2) ----

  suggestActions(): void {
    this.suggesting = true;
    this.suggestions = [];
    this.capa.suggestActions(this.caseId).subscribe({
      next: list => {
        this.suggestions = list;
        this.suggesting = false;
        if (!list.length) {
          this.snack.open($localize`:@@capa.detail.no-suggestion:Aucune action exploitable — précisez le problème.`, $localize`:@@common.ok:OK`, { duration: 3000 });
        }
      },
      error: err => {
        this.suggesting = false;
        this.snack.open(
          safeErrorMessage(err, $localize`:@@capa.detail.suggestion-unavailable:Suggestion IA indisponible (ai-service / Ollama).`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  // ---- Avancement du statut des actions (ANO-011, §4.2 / ISO 9001 §10.2) ----

  private static readonly ACTION_FLOW: Record<CapaActionStatus, CapaActionStatus | null> = {
    PENDING: 'IN_PROGRESS',
    IN_PROGRESS: 'DONE',
    DONE: null
  };

  /** Statut suivant d'une action (null si déjà DONE). */
  nextActionStatus(s: CapaActionStatus): CapaActionStatus | null {
    return CapaDetailComponent.ACTION_FLOW[s];
  }

  /** Une action est avançable si elle n'est pas DONE et que la CAPA n'est pas terminale. */
  canAdvanceAction(a: CapaActionResponse, caseStatus: CapaStatus): boolean {
    return a.status !== 'DONE' && !this.isTerminal(caseStatus) && !this.acting$.value;
  }

  /** Libellé du bouton selon le prochain statut. */
  advanceActionLabel(s: CapaActionStatus): string {
    return s === 'PENDING'
      ? $localize`:@@capa.detail.action-start:Démarrer`
      : $localize`:@@capa.detail.action-complete:Terminer`;
  }

  /**
   * Fait avancer une action vers son statut suivant.
   *
   * Seul le statut part : renvoyer le libellé le réécrirait à l'identique et
   * écraserait au passage une correction faite entre-temps par quelqu'un
   * d'autre. Le backend accepte le PATCH partiel.
   */
  advanceAction(a: CapaActionResponse): void {
    const next = this.nextActionStatus(a.status);
    if (!next || this.acting$.value) {
      return;
    }
    this.acting$.next(true);
    this.capa.updateAction(this.caseId, a.id, { status: next })
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@capa.detail.action-advanced:Action mise à jour.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] advanceAction failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@capa.detail.action-advance-error:Mise à jour de l'action impossible.`),
            $localize`:@@common.close:Fermer`, { duration: 4000 });
        }
      });
  }

  addSuggestion(s: SuggestedAction): void {
    this.addingKey = s.title;
    this.capa.addAction(this.caseId, { title: s.title, description: s.description }).subscribe({
      next: () => {
        this.addingKey = null;
        this.suggestions = this.suggestions.filter(x => x !== s);
        this.snack.open($localize`:@@capa.detail.action-added:Action ajoutée à la CAPA.`, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        this.addingKey = null;
        this.snack.open(safeErrorMessage(err, $localize`:@@common.add-failed:Ajout impossible.`), $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  dismissSuggestions(): void { this.suggestions = []; }

  start(): void { this.transition('start'); }
  resolve(): void { this.transition('resolve'); }
  reject(): void { this.transition('reject'); }

  /**
   * ISO 9001 §10.2 — once a CAPA is RESOLVED, the auditor must verify that
   * the action was actually effective at the planned horizon (typically
   * 3 / 6 / 12 months). Only allowed in RESOLVED state; transitions to
   * CLOSED on positive verification.
   */
  verifyEffectiveness(effective: boolean): void {
    if (this.acting$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: effective
          ? $localize`:@@capa.detail.confirm-effective-title:Confirmer efficacité ?`
          : $localize`:@@capa.detail.confirm-not-effective-title:Confirmer non-efficacité ?`,
        message: effective
          ? $localize`:@@capa.detail.confirm-effective-message:Tu confirmes que les actions ont eu l'effet attendu. Le cas sera clôturé (CLOSED).`
          : $localize`:@@capa.detail.confirm-not-effective-message:Tu signales que les actions n'ont pas été efficaces. Le cas reste RESOLVED — il faudra rouvrir ou créer un nouveau CAPA.`,
        confirmLabel: effective
          ? $localize`:@@capa.detail.yes-effective:Oui, efficace`
          : $localize`:@@capa.detail.yes-not-effective:Oui, non efficace`,
        destructive: !effective
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.acting$.next(true);
      this.capa.verifyEffectiveness(this.caseId, effective)
        .pipe(finalize(() => this.acting$.next(false)))
        .subscribe({
          next: () => {
            this.snack.open(
              effective
                ? $localize`:@@capa.detail.effectiveness-validated:Efficacité validée — cas clôturé.`
                : $localize`:@@capa.detail.not-effectiveness-recorded:Non-efficacité enregistrée.`,
              $localize`:@@common.ok:OK`, { duration: 2500 }
            );
            this.reload$.next();
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-detail] effectiveness failed', err?.status, err?.error?.title);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@capa.detail.verification-error:Erreur lors de la vérification.`),
              $localize`:@@common.ok:OK`, { duration: 4000 }
            );
          }
        });
    });
  }

  canVerifyEffectiveness(s: CapaStatus): boolean {
    return s === 'RESOLVED';
  }

  /** Libellé lisible d'une nature d'action, avec repli sur le code inconnu. */
  actionTypeLabel(type: CapaActionType): string {
    return this.actionTypes.find(t => t.value === type)?.label ?? type;
  }

  /**
   * Phrase du motif, construite ICI et non reçue du serveur : elle doit se dire
   * dans la langue de l'utilisateur, et un message serveur ne se traduit pas.
   */
  blockerLabel(b: ClosureBlocker): string {
    switch (b.code) {
      case 'NO_ACTION':
        return $localize`:@@capa.blocker.no-action:Aucune action n'est enregistrée : il n'y a rien dont vérifier l'efficacité.`;
      case 'ACTIONS_NOT_DONE':
        return b.count === 1
          ? $localize`:@@capa.blocker.actions-one:1 action reste à terminer.`
          : $localize`:@@capa.blocker.actions-many:${b.count}:count: actions restent à terminer.`;
      case 'CONTAINMENT_ONLY':
        return $localize`:@@capa.blocker.containment-only:Le dossier ne porte que des mesures d'endiguement : elles arrêtent l'effet sans supprimer la cause. Ajoutez une action corrective ou préventive.`;
      case 'OPEN_NON_CONFORMITIES':
        return b.count === 1
          ? $localize`:@@capa.blocker.nc-one:1 non-conformité liée est encore ouverte.`
          : $localize`:@@capa.blocker.nc-many:${b.count}:count: non-conformités liées sont encore ouvertes.`;
      default:
        // Code inconnu (serveur plus récent que l'écran) : mieux vaut une phrase
        // générique qu'un bouton actif dont on ignore pourquoi il échouera.
        return $localize`:@@capa.blocker.unknown:Un prérequis de clôture n'est pas satisfait.`;
    }
  }

  /** Vrai dès qu'un motif subsiste — c'est ce qui éteint le bouton de clôture. */
  blocksClosure(c: CapaCaseResponse): boolean {
    return (c.closureBlockers?.length ?? 0) > 0;
  }

  /**
   * Infobulle du bouton éteint. Vide quand rien ne bloque : une infobulle qui
   * répète le libellé du bouton n'apprend rien et gêne le survol.
   */
  closureTooltip(c: CapaCaseResponse): string {
    return this.blocksClosure(c)
      ? (c.closureBlockers ?? []).map(b => this.blockerLabel(b)).join(' ')
      : '';
  }

  private transition(action: 'start' | 'resolve' | 'reject'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.capa.startCase(this.caseId)
      : action === 'resolve' ? this.capa.resolveCase(this.caseId)
      : this.capa.rejectCase(this.caseId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const msg = action === 'start'
          ? $localize`:@@capa.detail.case-started:Cas démarré.`
          : action === 'resolve'
            ? $localize`:@@capa.detail.case-resolved:Cas résolu.`
            : $localize`:@@capa.detail.case-rejected:Cas rejeté.`;
        this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@capa.detail.transition-error:Erreur lors de la transition.`),
          $localize`:@@common.ok:OK`, { duration: 4000 }
        );
      }
    });
  }

  canStart(s: CapaStatus): boolean { return s === 'OPEN'; }
  canResolve(s: CapaStatus): boolean { return s === 'IN_PROGRESS'; }
  canReject(s: CapaStatus): boolean { return s === 'OPEN' || s === 'IN_PROGRESS'; }
  isTerminal(s: CapaStatus): boolean { return s === 'CLOSED' || s === 'REJECTED'; }

  effectivenessLabel(verified: boolean): string {
    return verified
      ? $localize`:@@capa.detail.effectiveness-verified:Vérifiée ✓`
      : $localize`:@@capa.detail.effectiveness-not-effective:Non efficace ✗`;
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
  actionBadge(s: 'PENDING' | 'IN_PROGRESS' | 'DONE'): string { return 'badge badge-' + s.toLowerCase(); }
}
