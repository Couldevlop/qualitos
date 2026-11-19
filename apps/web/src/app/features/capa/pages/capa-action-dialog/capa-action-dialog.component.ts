import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, defer } from 'rxjs';
import { concatMap, finalize } from 'rxjs/operators';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { personName } from '../../../../core/directory/directory-names';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CAPA_ACTION_TYPES } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaActionStatus, CapaActionType, UpdateCapaCaseRequest } from '../../capa.types';

/** Ce qu'il faut pour ouvrir la fenêtre : le dossier, et l'action quand on la relit. */
export interface CapaActionDialogData {
  /** Le dossier qui porte l'action. */
  caseId: string;
  /** L'action à modifier ; absente, la fenêtre en ajoute une. */
  action?: CapaActionResponse;
  /** Le pilote du dossier, proposé comme responsable d'une action nouvelle. */
  ownerId?: string;
  ownerName?: string;
  /** L'échéance du dossier, proposée comme échéance d'une action nouvelle. */
  dueDate?: string;
  /** Vrai quand le dossier est clos ou rejeté : tout est en lecture seule. */
  readOnly?: boolean;
  /**
   * La nature proposée à une action nouvelle, quand l'écran appelant la connaît.
   *
   * <p>C'est le cas du rapport 8D : le D6 ne parle que de prévention, et
   * proposer « corrective » dans la fenêtre qu'il ouvre obligerait à corriger un
   * champ dont la réponse était déjà écrite au-dessus du bouton.
   */
  actionType?: CapaActionType;
  /**
   * Les personnes déjà sur le dossier (l'équipe du D1), proposées EN TÊTE du
   * sélecteur de responsable : c'est presque toujours l'une d'elles qui prend
   * l'action, et la rechercher dans l'annuaire entier fait ressaisir ce que
   * l'écart sait déjà.
   */
  team?: readonly { accountId: string }[];
  /**
   * Le critère d'efficacité DU DOSSIER, tel qu'il est aujourd'hui.
   *
   * <p>Absent, la fenêtre le demande ; présent, elle le rappelle. L'ADR 0108
   * veut ce critère POSÉ À L'AVANCE, et l'avance, c'est ici : au moment où l'on
   * écrit l'action corrective. Le demander à la vérification laisserait juger
   * de l'efficacité en choisissant après coup la mesure qui arrange.
   */
  effectivenessCriterion?: string;
  /**
   * L'EXIGENCE de vérification d'efficacité du dossier, telle qu'elle est
   * aujourd'hui (ADR 0073). {@code null} = la question n'a pas été tranchée,
   * et la fenêtre la pose ; une fois tranchée, elle ne se repose pas.
   */
  verificationRequired?: boolean | null;
}



/** Les trois statuts, dans l'ordre du déroulé réel. */
const STATUTS: ReadonlyArray<CapaActionStatus> = ['PENDING', 'IN_PROGRESS', 'DONE'];

/**
 * UNE action d'un dossier CAPA : on l'ajoute, on la relit, on la corrige.
 *
 * <p>Avant, l'action ne se saisissait qu'à l'ouverture du dossier, et ne
 * s'avançait ensuite que d'un cran à la fois par un bouton. Rien ne permettait
 * de corriger un intitulé, de réassigner un porteur parti, ni de décaler une
 * échéance : il fallait rouvrir un dossier pour changer une phrase.
 *
 * <p>Une seule fenêtre pour l'ajout et l'édition : deux jumelles divergeraient
 * au premier champ ajouté d'un seul côté. Le statut ne s'affiche qu'à
 * l'édition — une action qui vient d'être décidée est « à faire », et proposer
 * d'autre chose laisserait créer une action déjà terminée sans que personne ne
 * l'ait menée.
 */
@Component({
  selector: 'qos-capa-action-dialog',
  templateUrl: './capa-action-dialog.component.html',
  styleUrls: ['./capa-action-dialog.component.scss'],
  standalone: false
})
export class CapaActionDialogComponent {

  readonly natures = CAPA_ACTION_TYPES;
  readonly statuses = STATUTS;
  readonly submitting$ = new BehaviorSubject<boolean>(false);

  /** Le nom du porteur, retenu avec son compte : une personne, jamais un identifiant. */
  private assigneeName: string | null = null;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    actionType: ['CORRECTIVE' as CapaActionType, [Validators.required]],
    assigneeId: [''],
    decidedOn: [''],
    dueDate: [''],
    status: ['PENDING' as CapaActionStatus],
    effectivenessCriterion: ['', [Validators.maxLength(2000)]],
    verificationRequired: [null as boolean | null],
    verificationAssigneeId: [null as string | null],
    verificationInstructions: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaActionDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: CapaActionDialogData
  ) {
    const a = data.action;
    if (a) {
      this.assigneeName = a.assigneeName ?? null;
      this.form.patchValue({
        title: a.title,
        description: a.description ?? '',
        actionType: a.actionType || 'CORRECTIVE',
        assigneeId: a.assigneeId ?? '',
        decidedOn: a.decidedOn ?? '',
        dueDate: a.dueDate ?? '',
        status: a.status
      });
    } else {
      // Le pilote du dossier et son échéance sont PROPOSÉS : c'est le cas le
      // plus fréquent, et les retaper à chaque action serait du recopiage.
      this.assigneeName = data.ownerName ?? null;
      this.form.patchValue({
        assigneeId: data.ownerId ?? '',
        dueDate: data.dueDate ?? '',
        // L'écran appelant impose parfois la nature (le D6 du 8D ne parle que de
        // prévention) ; sinon, « corrective », de loin le cas le plus fréquent.
        actionType: data.actionType ?? 'CORRECTIVE'
      });
    }
    if (data.readOnly) {
      this.form.disable();
    }
  }

  get editing(): boolean {
    return !!this.data.action;
  }

  /**
   * Vrai quand l'échéance saisie précède la date de début.
   *
   * <p>Un calcul et NON un validateur du formulaire : la règle SIGNALE, elle ne
   * bloque pas. C'est presque toujours une faute de frappe sur l'une des deux
   * dates, et refuser l'enregistrement ferait perdre le reste de la saisie pour
   * un chiffre -- alors que la personne est la seule à savoir laquelle des deux
   * dates est la bonne.
   *
   * <p>Comparaison de chaînes : les deux champs sont des jours au format ISO,
   * où l'ordre alphabétique EST l'ordre chronologique. Passer par `new Date()`
   * ferait entrer le fuseau du navigateur dans une comparaison de jours.
   */
  get echeanceAvantDebut(): boolean {
    const { decidedOn, dueDate } = this.form.getRawValue();
    return !!decidedOn && !!dueDate && dueDate < decidedOn;
  }

  /** Le critère déjà posé sur le dossier, s'il y en a un. */
  get critereDejaPose(): string {
    return (this.data.effectivenessCriterion ?? '').trim();
  }

  /**
   * Vrai quand la fenêtre doit DEMANDER le critère d'efficacité.
   *
   * <p>Seulement à l'ajout d'une action CORRECTIVE, et seulement quand le
   * dossier n'en a pas : une action de confinement ne prétend pas supprimer la
   * cause, donc rien n'est à mesurer sur elle (ISO 9001 §10.2) ; et redemander
   * un critère déjà posé serait l'occasion de le réécrire pour qu'il colle au
   * résultat obtenu, ce que l'ADR 0108 veut précisément empêcher.
   */
  get demandeCritere(): boolean {
    return !this.editing
      && !this.data.readOnly
      && this.form.controls.actionType.value === 'CORRECTIVE'
      && this.critereDejaPose === '';
  }

  /**
   * Vrai quand la fenêtre doit POSER la question de la vérification d'efficacité.
   *
   * <p>Même règle que le critère : on ne la pose que tant que le dossier ne l'a
   * pas tranchée. La reposer à chaque action laisserait la dernière saisie
   * décider pour tout le dossier, alors que la décision lui appartient en entier
   * (ADR 0073). Elle n'est pas restreinte aux actions correctives : un dossier
   * ouvert par un endiguement doit pouvoir trancher aussi.
   */
  get demandeVerification(): boolean {
    const tranchee = this.data.verificationRequired !== null
      && this.data.verificationRequired !== undefined;
    return !this.editing && !this.data.readOnly && !tranchee;
  }

  /** Le nom du vérificateur choisi, recopié de l'annuaire pour partir avec la décision. */
  verifierName?: string;

  onVerifier(user: DirectoryUser | null): void {
    this.verifierName = user ? personName(user) : undefined;
  }

  /**
   * La décision de vérification telle qu'elle part : le vérificateur, son nom et
   * les consignes seulement si elle est exigée — le serveur refuse de désigner
   * quelqu'un pour une vérification qui ne l'est pas.
   */
  private decisionDeVerification(v: {
    verificationRequired: boolean | null;
    verificationAssigneeId: string | null;
    verificationInstructions: string;
  }): UpdateCapaCaseRequest {
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

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@capa.action-dialog.title-edit:Modifier l'action`
      : $localize`:@@capa.action-dialog.title-add:Ajouter une action`;
  }

  statusLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : status === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  /** Le sélecteur rend la personne choisie : son nom part avec son compte (ADR 0052). */
  onAssignee(user: DirectoryUser | null): void {
    this.assigneeName = user ? personName(user) : null;
  }

  submit(): void {
    if (this.form.invalid || this.submitting$.value || this.data.readOnly) {
      return;
    }
    const v = this.form.getRawValue();
    const corps = {
      title: v.title.trim(),
      description: v.description.trim() || undefined,
      actionType: v.actionType,
      assigneeId: v.assigneeId || undefined,
      assigneeName: v.assigneeId ? (this.assigneeName ?? undefined) : undefined,
      decidedOn: v.decidedOn || undefined,
      dueDate: v.dueDate || undefined
    };
    this.submitting$.next(true);
    const critere = this.demandeCritere ? v.effectivenessCriterion.trim() : '';
    // `defer` et non l'appel direct : appeler le service CONSTRUIT déjà sa
    // requête. Sans ce report, l'action partait avant le critère, et l'ordre
    // ci-dessous ne décrivait que l'intention.
    const action$ = defer(() => this.editing
      ? this.capa.updateAction(this.data.caseId, this.data.action!.id, { ...corps, status: v.status })
      : this.capa.addAction(this.data.caseId, corps));
    // LE CRITÈRE D'ABORD, l'action ensuite. L'ADR 0108 le veut posé à l'avance :
    // si l'écriture du critère échoue, rien n'a été créé et la personne retente
    // avec son formulaire intact. Dans l'autre ordre, une panne laisserait une
    // action corrective sans la mesure qui dira un jour si elle a marché.
    // Le critere et la decision de verification sont deux champs DU DOSSIER :
    // une seule ecriture, pour qu'une panne ne laisse pas l'un pose sans l'autre.
    const majDossier: UpdateCapaCaseRequest = {
      ...(critere ? { effectivenessCriterion: critere } : {}),
      ...(this.demandeVerification ? this.decisionDeVerification(v) : {})
    };
    const envoi = Object.keys(majDossier).length
      ? this.capa.updateCase(this.data.caseId, majDossier).pipe(concatMap(() => action$))
      : action$;
    envoi.pipe(finalize(() => this.submitting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          this.editing
            ? $localize`:@@capa.action-dialog.saved:Action enregistrée.`
            : $localize`:@@capa.action-dialog.added:Action ajoutée.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.dialogRef.close(true);
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-dialog] save failed', err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(
            err, $localize`:@@capa.action-dialog.failed:L'action n'a pas pu être enregistrée.`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
