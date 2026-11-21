import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { personName } from '../../../../core/directory/directory-names';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CAPA_ACTION_TYPES } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaActionStatus, CapaActionType, CreateCapaActionRequest } from '../../capa.types';

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

  /**
   * Les natures proposées : toutes, ou LA SEULE que l'écran appelant impose.
   *
   * <p>Une discipline du traitement 8D ne parle que d'une nature — le D3
   * endigue, le D5 corrige, le D7 prévient —, et la question était pourtant
   * reposée en trois choix sous un titre qui portait déjà la réponse. Un clic
   * de trop, et surtout une action classée sous la discipline voisine dès que
   * la main dérapait.
   *
   * <p>À l'édition, les trois restent : on relit alors une action existante, et
   * corriger une nature mal choisie est précisément ce qu'on vient faire.
   */
  get natures(): ReadonlyArray<{ value: CapaActionType; label: string }> {
    const imposee = this.editing ? undefined : this.data.actionType;
    return imposee ? CAPA_ACTION_TYPES.filter(n => n.value === imposee) : CAPA_ACTION_TYPES;
  }
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
   * <p>À l'ajout d'une action qui PRÉTEND SUPPRIMER LA CAUSE — corrective ou
   * préventive —, et seulement quand le dossier n'a pas encore de critère. Une
   * action de confinement ne le prétend pas, donc rien n'est à mesurer sur elle
   * (ISO 9001 §10.2) ; et redemander un critère déjà posé serait l'occasion de
   * le réécrire pour qu'il colle au résultat obtenu, ce que l'ADR 0108 veut
   * précisément empêcher — il se RAPPELLE alors, en lecture, juste en dessous.
   *
   * <p>La préventive était exclue, et c'était une erreur : « plus aucune
   * récidive sur les trois autres lignes pendant six mois » est un critère
   * d'efficacité au même titre qu'un autre, et le D7 n'en demandait aucun.
   */
  get demandeCritere(): boolean {
    return !this.editing && !this.data.readOnly && this.mesureUnEffet;
  }

  /**
   * Vrai quand la nature de l'action prétend supprimer la cause — et qu'il y a
   * donc un effet à mesurer. L'endiguement en est seul exclu.
   */
  get mesureUnEffet(): boolean {
    return this.form.controls.actionType.value !== 'CONTAINMENT';
  }

  /**
   * Vrai quand la fenêtre doit POSER la question de la vérification d'efficacité.
   *
   * <p>Même règle que le critère : on ne la pose que tant que le dossier ne l'a
   * pas tranchée. La reposer à chaque action laisserait la dernière saisie
   * décider pour tout le dossier, alors que la décision lui appartient en entier
   * (ADR 0073).
   *
   * <p>Pas sur un endiguement : le dossier ouvert par la seule mesure
   * d'endiguement ne se clôturera pas sans action corrective (le serveur le
   * refuse, motif {@code CONTAINMENT_ONLY}), et c'est à l'écriture de celle-ci
   * que la question se pose, devant quelqu'un qui promet un effet.
   */
  get demandeVerification(): boolean {
    return !this.editing && !this.data.readOnly && this.mesureUnEffet;
  }

  /**
   * La vérification d'efficacité DÉJÀ tranchée sur le dossier, à rappeler.
   *
   * <p>Quand elle l'a été — presque toujours à l'ouverture (ADR 0073) —, la
   * question ne se repose pas. Mais ne RIEN dire laissait écrire une action
   * corrective sans savoir si son effet serait vérifié, et donnait à croire que
   * la plateforme ne le demandait nulle part. On le dit, en lecture.
   */
  get verificationDejaTranchee(): boolean | null {
    const propre = this.data.action?.verificationRequired;
    const v = this.editing && propre !== null && propre !== undefined
      ? propre
      : this.data.verificationRequired;
    return v === null || v === undefined ? null : v;
  }

  /**
   * Le critère à RAPPELER : celui de l'action qu'on relit, sinon celui du
   * dossier.
   *
   * <p>À l'édition, c'est la mesure de CETTE action qui intéresse ; à l'ajout,
   * c'est ce que le dossier attend déjà, affiché à côté de la question posée
   * pour la nouvelle — les deux coexistent (ADR 0131), et les montrer ensemble
   * est ce qui permet d'écrire la seconde en connaissance de la première.
   */
  get critereRappele(): string {
    const propre = (this.data.action?.effectivenessCriterion ?? '').trim();
    return this.editing ? (propre || this.critereDejaPose) : this.critereDejaPose;
  }

  /** Le critère déjà posé se RAPPELLE, en plus de celui qu'on demande. */
  get rappelDuCritere(): boolean {
    return this.critereRappele !== '';
  }

  /**
   * Vrai quand il y a quelque chose à RAPPELER de l'évaluation d'efficacité.
   *
   * <p>Le critère déjà posé, la décision déjà prise, ou les deux : chacun des
   * deux peut être réglé sans l'autre, et l'un se rappelle pendant que l'autre
   * se demande. Rien sous un endiguement : il ne promet aucun effet.
   */
  get rappelDEfficacite(): boolean {
    return this.mesureUnEffet
      && (this.rappelDuCritere || this.verificationDejaTranchee !== null);
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
  }): Partial<CreateCapaActionRequest> {
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
    // L'ÉVALUATION D'EFFICACITÉ PART AVEC L'ACTION, dans la même écriture
    // (ADR 0131). Elle partait avant sur le DOSSIER, en deux appels enchaînés :
    // la question ne se posait donc qu'une fois, et la deuxième action d'un
    // dossier s'écrivait sans qu'on sache si son effet serait vérifié. Une
    // seule écriture ferme aussi la fenêtre où une panne laissait le critère
    // posé sans l'action qu'il mesure.
    const critere = this.demandeCritere ? v.effectivenessCriterion.trim() : '';
    const efficacite = {
      ...(critere ? { effectivenessCriterion: critere } : {}),
      ...(this.demandeVerification ? this.decisionDeVerification(v) : {})
    };
    const envoi = this.editing
      ? this.capa.updateAction(this.data.caseId, this.data.action!.id, { ...corps, status: v.status })
      : this.capa.addAction(this.data.caseId, { ...corps, ...efficacite });
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
