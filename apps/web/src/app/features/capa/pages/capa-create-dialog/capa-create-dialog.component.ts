import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { catalogOptions } from '../../../../core/catalogs/catalog-overrides';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subject, from, of } from 'rxjs';
import { catchError, concatMap, finalize, map, switchMap, takeUntil, tap, toArray } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { findExactPerson, personName } from '../../../../core/directory/directory-names';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CAPA_CRITICITIES, CAPA_SOURCES, CapaSourceRegister, capaSourceLabel, capaSourceRegister } from '../../capa.labels';
import { CapaSourceRefOption, CapaSourceRefService, SOURCE_REF_PAGE_SIZE } from '../../source-registry/capa-source-ref.service';
import { CapaService } from '../../capa.service';
import { CapaActionType, CapaCriticity, CapaSourceType, CapaType, OpenCapaRequest } from '../../capa.types';

/**
 * Nature de l'action que porte la CAPA : endiguement, corrective ou préventive.
 *
 * <p>L'endiguement qualifie l'ACTION, pas le dossier : un dossier de pur
 * endiguement ne se clôturerait jamais (ISO 9001 §10.2), il naîtrait bloqué.
 * Choisi ici, il ouvre donc un dossier correctif dont l'action est typée
 * endiguement — la distinction même que porte l'étape D3 de la 8D.
 */
export type CapaNature = CapaActionType;

/** Ce qu'un autre écran peut proposer à la création — l'utilisateur garde la main sur tout. */
export interface CapaCreatePrefill {
  title?: string;
  description?: string;
  sourceRef?: string;
  type?: CapaNature;
  criticity?: CapaCriticity;
  dueDate?: string;
  /**
   * Un nom écrit en clair ailleurs — le propriétaire d'un risque —, proposé
   * comme responsable s'il désigne EXACTEMENT une personne de l'annuaire (nom
   * ou adresse, accents et casse ignorés).
   */
  ownerHint?: string;
  /**
   * Le compte de cette personne quand l'autre écran le connaît (propriétaire
   * d'un risque choisi dans l'annuaire, ADR 0092) : proposé tel quel, sans
   * rapprochement par le nom ; `ownerHint` sert alors de nom affiché.
   */
  ownerAccountId?: string;
}

/** La source d'où l'on ouvre la CAPA : fixée, affichée, jamais modifiable ici. */
export interface CapaCreateSource {
  type: CapaSourceType;
  /** Ce que l'utilisateur reconnaît : « NC-2026-0001 », « AUD-2026-0003 / constat 2 », « R-001 ». */
  reference: string;
}

/** Ce que rend la fenêtre : l'identifiant du dossier, plus ce que l'écran d'origine y a joint. */
export interface CapaOpened {
  capaId: string;
  /**
   * L'action telle qu'elle vient d'être saisie, pour l'écran qui reprend la
   * ligne — nature et criticité comprises : la fiche DMAIC les recopie pour que
   * son rapport client les lise sans interroger le module CAPA.
   */
  action?: {
    title: string;
    ownerId?: string;
    ownerName?: string;
    dueDate?: string;
    nature?: CapaActionType;
    criticity?: CapaCriticity;
  };
}

export interface CapaCreateDialogData {
  /**
   * `open` (défaut) : ouvrir un dossier et son action. `define-action` : définir
   * l'action d'un dossier qui n'en a pas — ancien, ou ouvert par un automate.
   * Le même formulaire, réduit : un quatrième formulaire divergerait.
   */
  mode?: 'open' | 'define-action';
  /** Le dossier dont on définit l'action (mode `define-action`). */
  caseId?: string;
  /** Absente : création libre depuis la liste, la source se choisit. */
  source?: CapaCreateSource;
  prefill?: CapaCreatePrefill;
  /** La criticité découle de l'écran d'origine (cotation d'un risque) : affichée, non modifiable. */
  lockCriticity?: boolean;
  /**
   * Comment l'écran d'origine ouvre le dossier — sa propre route serveur, qui
   * fixe la source et garde le lien (NC, constat, risque). Absent : la route
   * de création libre du module CAPA.
   */
  open?: (request: OpenCapaRequest) => Observable<CapaOpened>;
}

/**
 * LA fenêtre de création d'une CAPA, d'où qu'on l'ouvre (ADR 0088).
 *
 * Une CAPA porte une action : intitulé, nature, responsable et échéance sont à
 * la fois ceux du dossier et de son action, saisis une fois. Le responsable se
 * choisit dans l'annuaire du client en lecture restreinte (ADR 0091), ouvert à
 * tout membre, et devient le pilote du dossier. Par défaut : soi-même.
 */
@Component({
  selector: 'qos-capa-create-dialog',
  templateUrl: './capa-create-dialog.component.html',
  styleUrls: ['./capa-create-dialog.component.scss'],
  standalone: false
})
export class CapaCreateDialogComponent implements OnInit, OnDestroy {

  submitting = false;

  /**
   * Pièces choisies AVANT que le dossier existe. Elles ne partent qu'après la
   * création : le serveur classe une preuve sous un dossier, il n'y a rien à
   * quoi la rattacher tant que le dossier n'a pas d'identifiant.
   */
  attachments: File[] = [];

  /** Nombre de pièces déjà envoyées, pour dire où en est le dépôt. */
  uploaded = 0;

  /** Mêmes limites que la carte « Preuves » de la fiche : 10 Mo, 10 pièces. */
  readonly maxEvidenceBytes = 10 * 1024 * 1024;
  readonly maxEvidences = 10;
  readonly acceptedTypes =
    'application/pdf,image/jpeg,image/png,image/webp,image/heic,.docx,.xlsx';

  /**
   * L'endiguement d'abord : c'est l'action qu'on prend le jour même, avant même
   * de connaître la cause. Le mettre en tête, c'est suivre l'ordre réel du
   * terrain plutôt que l'ordre alphabétique d'une énumération.
   */
  /** Les valeurs de la plateforme : le repli quand le client n'a rien réglé. */
  private readonly naturesPlateforme: { value: CapaNature; label: string }[] = [
    { value: 'CONTAINMENT', label: $localize`:@@capa.action-type.containment:Endiguement` },
    { value: 'CORRECTIVE', label: $localize`:@@capa.type.corrective:Corrective` },
    { value: 'PREVENTIVE', label: $localize`:@@capa.type.preventive:Préventive` }
  ];

    /** Dans l'ordre et les mots du client (ADR 0111). */
  readonly natures = catalogOptions<CapaNature>('capa-nature', this.naturesPlateforme);
  readonly criticities = CAPA_CRITICITIES;
  readonly sourceTypes = CAPA_SOURCES.filter(s => s.manual);

  /** Pas d'échéance dans le passé : le dossier naîtrait en retard (le serveur le refuse aussi). */
  readonly today = localIsoDate(new Date());

  /** Vrai le temps de chercher dans l'annuaire le propriétaire indiqué par l'écran d'origine. */
  matchingOwner = false;
  /** Le propriétaire indiqué n'a pas pu être rapproché d'un compte : on le dit. */
  private ownerHintUnmatched = false;

  /**
   * Le nom recopié avec chaque compte rencontré : le sien pour l'utilisateur
   * connecté, celui de l'annuaire pour les personnes choisies. Jamais l'adresse
   * quand un nom existe.
   */
  private readonly names = new Map<string, string>();

  /**
   * Les références proposées pour la source choisie, quand elle en désigne une.
   * `null` tant que rien n'est chargé — distinct de la liste vide, qui dit que
   * le registre est bien là mais n'a rien à proposer.
   */
  sourceRefOptions: CapaSourceRefOption[] | null = null;

  /** Vrai le temps d'interroger le registre. */
  loadingSourceRefs = false;

  /**
   * Le registre n'a pas répondu. On le DIT, et on propose de réessayer : une
   * liste vide laisserait croire qu'il n'y a rien à choisir, et pousserait à
   * ouvrir un dossier sans rattachement — exactement ce que ce champ empêche.
   */
  sourceRefsFailed = false;

  /** Coupe les chargements en cours quand la fenêtre se ferme. */
  private readonly destroyed = new Subject<void>();

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, notBlank, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(4000)]],
    type: ['CORRECTIVE' as CapaNature, [Validators.required]],
    criticity: ['MEDIUM' as CapaCriticity, [Validators.required]],
    // Le responsable : l'identifiant du COMPTE DE CONNEXION (`sub`), rendu par
    // le sélecteur de l'annuaire ; son nom est recopié à l'envoi.
    assigneeId: ['', [Validators.required]],
    dueDate: ['', [Validators.required, notInPast]],
    sourceType: ['INTERNAL' as CapaSourceType, [Validators.required]],
    // Obligatoire dès que la source désigne un objet de la plateforme : le
    // validateur est posé par `applySourceRegister`, pas ici, car il suit la
    // source choisie.
    sourceRef: ['', [Validators.maxLength(255)]],
    // La vérification d'efficacité (ADR 0073), décidée à l'ouverture : `null`
    // tant que la question n'est pas tranchée — aucune réponse pré-cochée.
    verificationRequired: [null as boolean | null],
    verificationAssigneeId: [null as string | null],
    verificationInstructions: ['', [Validators.maxLength(4000)]],
    // Le critère d'efficacité : facultatif à l'ouverture, mais c'est ici
    // qu'il a le plus de valeur — posé avant la mesure, il ne peut pas
    // être ajusté en voyant le résultat (ADR 0108).
    effectivenessCriterion: ['', [Validators.maxLength(2000)]]
  });

  /** Le nom d'annuaire du vérificateur choisi, recopié dans le dossier avec son compte. */
  private verifierName: string | undefined;

  readonly data: CapaCreateDialogData;

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly directory: DirectoryService,
    private readonly sourceRefs: CapaSourceRefService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaCreateDialogComponent, CapaOpened>,
    @Optional() @Inject(MAT_DIALOG_DATA) data: CapaCreateDialogData | null
  ) {
    this.data = data ?? {};
    const p = this.data.prefill ?? {};
    this.form.patchValue({
      title: p.title ?? '',
      description: p.description ?? '',
      type: p.type ?? 'CORRECTIVE',
      criticity: p.criticity ?? 'MEDIUM',
      dueDate: p.dueDate && p.dueDate >= this.today ? p.dueDate : '',
      sourceRef: p.sourceRef ?? ''
    });
    // Posés à la construction, hors de tout rendu : le mode ne change pas
    // pendant la vie de la fenêtre.
    if (this.data.source) {
      this.form.patchValue({ sourceType: this.data.source.type, sourceRef: this.data.source.reference });
    }
    if (this.data.lockCriticity || this.isDefineAction) {
      this.form.controls.criticity.disable();
    }
    // À l'ouverture, d'où qu'on vienne, la question de la vérification se
    // TRANCHE : c'est la même décision de clôture pour toutes les sources, et
    // personne ne repasserait par « Modifier » pour la poser. Le validateur est
    // posé ICI, à la construction — jamais pendant un rendu, où il ferait
    // basculer `form.invalid` au milieu de la passe (NG0100). Le mode « définir
    // l'action » ne la pose pas : le dossier existe, sa décision se règle dans
    // « Modifier » — deux endroits pour la même décision, le dernier gagnerait
    // en silence.
    if (!this.isDefineAction) {
      this.form.controls.verificationRequired.addValidators(Validators.required);
      this.form.controls.verificationRequired.updateValueAndValidity({ emitEvent: false });
    }
  }

  ngOnInit(): void {
    // La référence de la source : chargée pour la source initiale, puis
    // rechargée à chaque changement. Branché ICI et non dans le constructeur :
    // le premier chargement doit pouvoir rendre sa réponse après le premier
    // rendu sans faire varier `form.invalid` au milieu d'une passe (NG0100).
    this.applySourceRegister(this.sourceRegister, false);
    this.form.controls.sourceType.valueChanges
      .pipe(takeUntil(this.destroyed))
      .subscribe(type => this.applySourceRegister(capaSourceRegister(type), true));
    // Soi-même par défaut : c'est ce qui se faisait avant, et c'est modifiable.
    const me = this.auth.snapshot();
    if (me?.userId) {
      if (me.displayName) {
        this.names.set(me.userId, me.displayName);
      }
      this.form.controls.assigneeId.setValue(me.userId);
    }
    // Le propriétaire désigné ailleurs, s'il désigne exactement une personne de
    // l'annuaire, prend la place de « soi-même ».
    const hint = this.data.prefill?.ownerHint?.trim();
    const account = this.data.prefill?.ownerAccountId;
    if (account) {
      if (hint) {
        this.names.set(account, hint);
      }
      this.form.controls.assigneeId.setValue(account);
    } else if (hint) {
      this.matchingOwner = true;
      findExactPerson(this.directory, hint).subscribe(person => {
        this.matchingOwner = false;
        if (person) {
          this.names.set(person.accountId, personName(person));
          this.form.controls.assigneeId.setValue(person.accountId);
        } else {
          this.ownerHintUnmatched = true;
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.destroyed.next();
    this.destroyed.complete();
  }

  // ---------- la référence de la source ----------

  /**
   * Le registre d'où se tire la référence de la source choisie, ou `undefined`
   * quand la source ne désigne aucun objet (détection interne, alerte IoT,
   * « autre ») : là, le texte libre reste la seule réponse honnête.
   */
  get sourceRegister(): CapaSourceRegister | undefined {
    return capaSourceRegister(this.form.controls.sourceType.value);
  }

  /** Combien de fiches la liste propose au plus — repris dans l'indication. */
  readonly pageSize = SOURCE_REF_PAGE_SIZE;

  /**
   * La liste est pleine : il y a vraisemblablement des fiches plus anciennes
   * qu'elle ne montre pas. Le dire évite de conclure qu'une référence absente
   * n'existe pas.
   */
  get sourceRefTruncated(): boolean {
    return (this.sourceRefOptions?.length ?? 0) >= SOURCE_REF_PAGE_SIZE;
  }

  /**
   * Compare deux références. Déclaré pour que `mat-select` retrouve la valeur
   * héritée de l'écran d'origine malgré les espaces de recopie.
   */
  readonly sameRef = (a: string | null, b: string | null): boolean =>
    (a ?? '').trim() === (b ?? '').trim();

  /** Vrai quand la référence se CHOISIT au registre au lieu de se taper. */
  get sourceRefIsPicked(): boolean {
    return !!this.sourceRegister && !this.sourceRefsFailed;
  }

  /**
   * Charge les références du registre et pose — ou retire — l'obligation.
   *
   * <p>La valeur déjà saisie est effacée au changement de source : « NC-2026-018 »
   * conservé sous la source « Risque » désignerait un objet qui n'existe pas
   * dans le registre interrogé, et le dossier mentirait sur sa cause. Elle est
   * en revanche CONSERVÉE quand l'écran d'origine l'a fournie, ou quand on
   * revient sur la même source.
   */
  private applySourceRegister(register: CapaSourceRegister | undefined, clearValue: boolean): void {
    const control = this.form.controls.sourceRef;
    if (clearValue) {
      control.setValue('');
    }
    this.sourceRefOptions = null;
    this.sourceRefsFailed = false;
    this.loadingSourceRefs = false;

    control.setValidators(register
      ? [Validators.required, notBlank, Validators.maxLength(255)]
      : [Validators.maxLength(255)]);
    control.updateValueAndValidity({ emitEvent: false });

    if (!register) {
      return;
    }
    this.loadingSourceRefs = true;
    this.sourceRefs.options(register).pipe(takeUntil(this.destroyed)).subscribe({
      next: options => {
        this.loadingSourceRefs = false;
        this.sourceRefOptions = options;
        // Une référence héritée de l'écran d'origine qui ne figure pas dans la
        // page chargée est AJOUTÉE à la liste plutôt que perdue : la page ne
        // porte que les plus récentes, et l'objet désigné peut être plus ancien.
        const herite = control.value?.trim();
        if (herite && !options.some(o => o.reference === herite)) {
          this.sourceRefOptions = [{ reference: herite, label: herite }, ...options];
        }
      },
      error: () => {
        this.loadingSourceRefs = false;
        this.sourceRefsFailed = true;
        // Le registre est muet : on retombe sur la saisie libre, toujours
        // obligatoire. Bloquer l'ouverture du dossier parce qu'une liste n'a pas
        // répondu punirait l'utilisateur d'une panne qui n'est pas la sienne.
        control.setValidators([Validators.required, notBlank, Validators.maxLength(255)]);
        control.updateValueAndValidity({ emitEvent: false });
      }
    });
  }

  /** Nouvel essai après un registre muet. */
  retrySourceRefs(): void {
    this.applySourceRegister(this.sourceRegister, false);
  }

  /** Le sélecteur rend la personne choisie : son nom part avec son compte. */
  onOwner(user: DirectoryUser | null): void {
    if (user) {
      this.names.set(user.accountId, personName(user));
    }
  }

  /** Le vérificateur choisi : son nom d'annuaire part avec son compte, comme pour l'édition. */
  onVerifier(user: DirectoryUser | null): void {
    this.verifierName = user ? personName(user) : undefined;
  }

  get isDefineAction(): boolean {
    return this.data.mode === 'define-action';
  }

  /** L'endiguement est choisi : on explique que le dossier reste correctif. */
  get isContainment(): boolean {
    return this.form.controls.type.value === 'CONTAINMENT';
  }

  get dialogTitle(): string {
    return this.isDefineAction
      ? $localize`:@@capa.open.define-title:Définir l'action`
      : $localize`:@@capa.open.title:Nouvelle CAPA`;
  }

  get submitLabel(): string {
    return this.isDefineAction
      ? $localize`:@@capa.open.define-submit:Enregistrer l'action`
      : $localize`:@@capa.open.submit:Créer la CAPA`;
  }

  /** La source est fixée par l'écran d'origine : ni la choisir ni la référencer ici. */
  get sourceLocked(): boolean {
    return !!this.data.source;
  }

  get sourceLabel(): string {
    return this.data.source ? capaSourceLabel(this.data.source.type) : '';
  }

  /**
   * Le nom du propriétaire désigné ailleurs, quand il n'a pas pu être rapproché
   * d'un compte : l'utilisateur sait ainsi qui chercher, au lieu de croire que
   * la fenêtre a ignoré l'information.
   */
  get unmatchedOwnerHint(): string | null {
    const hint = this.data.prefill?.ownerHint?.trim();
    return hint && this.ownerHintUnmatched ? hint : null;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.submitting = true;
    // L'endiguement qualifie l'ACTION ; le DOSSIER, lui, reste correctif pour
    // rester clôturable (ISO 9001 §10.2).
    const containment = v.type === 'CONTAINMENT';
    const request: OpenCapaRequest = {
      title: v.title.trim(),
      description: v.description.trim() || undefined,
      type: containment ? 'CORRECTIVE' : (v.type as Exclude<CapaType, 'CONTAINMENT'>),
      actionType: v.type,
      criticity: v.criticity,
      ownerId: v.assigneeId,
      ownerName: this.names.get(v.assigneeId),
      dueDate: v.dueDate
    };
    if (!this.isDefineAction) {
      Object.assign(request, this.verificationDecision(v));
      // La clé n'apparaît que si le critère est saisi. Poser
      // `effectivenessCriterion: undefined` enverrait un champ vide au serveur
      // et l'écran d'évaluation prendrait une chaîne vide pour un critère posé.
      const critere = v.effectivenessCriterion.trim();
      if (critere) {
        request.effectivenessCriterion = critere;
      }
    }
    if (!this.sourceLocked) {
      request.sourceType = v.sourceType;
      request.sourceRef = v.sourceRef.trim() || undefined;
    }

    this.persist(request)
      .pipe(
        // Le dossier d'abord, ses pièces ensuite : l'inverse n'existe pas.
        switchMap(opened => this.uploadAttachments(opened)),
        finalize(() => (this.submitting = false))
      )
      .subscribe({
        next: opened => {
          this.snack.open(this.doneMessage(), $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close({
            ...opened,
            action: {
              title: request.title, ownerId: request.ownerId, ownerName: request.ownerName,
              dueDate: request.dueDate, nature: v.type, criticity: v.criticity
            }
          });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
            $localize`:@@common.ok:OK`,
            { duration: 4000 }
          );
        }
      });
  }

  /**
   * La décision de vérification telle qu'elle part avec l'ouverture : le
   * vérificateur, son nom et les consignes seulement si elle est exigée — le
   * serveur refuse de désigner quelqu'un pour une vérification non exigée.
   */
  private verificationDecision(v: {
    verificationRequired: boolean | null;
    verificationAssigneeId: string | null;
    verificationInstructions: string;
  }): Partial<OpenCapaRequest> {
    if (v.verificationRequired === null) {
      return {};
    }
    if (!v.verificationRequired) {
      return { verificationRequired: false };
    }
    return {
      verificationRequired: true,
      verificationAssigneeId: v.verificationAssigneeId ?? undefined,
      verificationAssigneeName: v.verificationAssigneeId ? this.verifierName : undefined,
      verificationInstructions: v.verificationInstructions.trim() || undefined
    };
  }

  /** Une écriture : le dossier et son action, ou l'action d'un dossier existant. */
  private persist(request: OpenCapaRequest): Observable<CapaOpened> {
    if (this.isDefineAction && this.data.caseId) {
      const caseId = this.data.caseId;
      return this.capa.addAction(caseId, {
        title: request.title,
        description: request.description,
        // La nature CHOISIE, endiguement compris : c'est l'action qu'on définit.
        actionType: request.actionType ?? request.type,
        assigneeId: request.ownerId,
        assigneeName: request.ownerName,
        dueDate: request.dueDate
      }).pipe(map(() => ({ capaId: caseId })));
    }
    if (this.data.open) {
      return this.data.open(request);
    }
    return this.capa.openCase(request).pipe(map(c => ({ capaId: c.id })));
  }

  /**
   * Ajoute les fichiers choisis à la file, en refusant tout de suite ce que le
   * serveur refuserait de toute façon — un fichier trop lourd rejeté après la
   * création laisserait un dossier créé et une pièce perdue.
   */
  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const picked = Array.from(input.files ?? []);
    // Réinitialise pour autoriser la re-sélection du même fichier.
    input.value = '';

    for (const file of picked) {
      if (this.attachments.length >= this.maxEvidences) {
        this.snack.open(
          $localize`:@@capa.create.evidence-limit:Dix pièces au maximum par dossier.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        return;
      }
      if (file.size > this.maxEvidenceBytes) {
        this.snack.open(
          $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        continue;
      }
      this.attachments = [...this.attachments, file];
    }
  }

  removeAttachment(file: File): void {
    this.attachments = this.attachments.filter(f => f !== file);
  }

  removeAttachmentAria(name: string): string {
    return $localize`:@@capa.open.attachment-remove-aria:Retirer ${name}:name:`;
  }

  /** Taille lisible, en Ko ou Mo selon l'ordre de grandeur. */
  formatSize(bytes: number): string {
    return bytes >= 1024 * 1024
      ? `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
      : `${Math.max(1, Math.round(bytes / 1024))} Ko`;
  }

  /**
   * Dépose les pièces une à une sur le dossier créé.
   *
   * <p>Un échec de dépôt ne détruit pas le dossier et ne bloque pas la
   * fermeture : le dossier, lui, est bien créé. On le dit, et les pièces
   * manquantes se rejoignent depuis la fiche.
   */
  private uploadAttachments(opened: CapaOpened): Observable<CapaOpened> {
    if (this.attachments.length === 0) {
      return of(opened);
    }
    this.uploaded = 0;
    return from(this.attachments).pipe(
      concatMap(file =>
        this.capa.uploadEvidence(opened.capaId, file).pipe(
          tap(() => this.uploaded++),
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-create] uploadEvidence failed', err?.status, err?.error?.type);
            return of(null);
          })
        )
      ),
      toArray(),
      map(() => opened)
    );
  }

  /** Le message dit ce qui est réellement joint, pas ce qui était prévu. */
  private doneMessage(): string {
    if (this.isDefineAction) {
      return $localize`:@@capa.open.action-defined:Action définie.`;
    }
    const total = this.attachments.length;
    if (total === 0) {
      return $localize`:@@capa.open.success:CAPA créée.`;
    }
    if (this.uploaded === total) {
      return $localize`:@@capa.open.success-with-evidence:CAPA créée, pièces jointes déposées.`;
    }
    return $localize`:@@capa.open.success-partial-evidence:CAPA créée, mais des pièces n'ont pas pu être déposées — reprends-les depuis la fiche.`;
  }

  cancel(): void {
    this.dialogRef.close();
  }
}

/** Un intitulé fait d'espaces n'est pas un intitulé. */
function notBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.length > 0 && control.value.trim().length === 0
    ? { required: true }
    : null;
}

/** Une échéance déjà passée ferait naître le dossier en retard. */
function notInPast(control: AbstractControl): ValidationErrors | null {
  const v = control.value;
  return typeof v === 'string' && v !== '' && v < localIsoDate(new Date()) ? { past: true } : null;
}

/** La date du jour au fuseau du navigateur, au format d'un `input type=date`. */
export function localIsoDate(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
