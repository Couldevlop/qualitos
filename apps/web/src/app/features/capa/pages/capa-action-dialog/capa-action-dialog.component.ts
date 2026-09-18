import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TenantUser } from '../../../admin/admin.types';
import { TenantTeamService } from '../../../admin/tenant-team.service';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaActionType, CapaCaseResponse } from '../../capa.types';

export interface CapaActionDialogData {
  caseId: string;
  /**
   * Vrai quand ce dialogue doit AUSSI poser la vérification d'efficacité.
   *
   * <p>Utilisé depuis une non-conformité, où le dossier CAPA vient peut-être
   * d'être créé et où personne n'a jamais vu son formulaire d'édition : sans
   * cela, la question ne serait jamais posée sur ce chemin.
   *
   * <p>Faux depuis la fiche CAPA, et c'est délibéré : la vérification y est
   * déjà réglable dans « Modifier », et se lit sur la fiche. La poser une
   * seconde fois dans le même écran donnerait deux endroits pour décider de la
   * même chose, dont le dernier enregistré gagnerait en silence.
   */
  askVerification?: boolean;
}

@Component({
  selector: 'qos-capa-action-dialog',
  templateUrl: './capa-action-dialog.component.html',
  styleUrls: ['./capa-action-dialog.component.scss'],
  standalone: false
})
export class CapaActionDialogComponent implements OnInit {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    // Jour de la DÉCISION. Pré-rempli au jour même parce que c'est le cas
    // courant, mais modifiable : une action décidée en comité et saisie plus
    // tard doit pouvoir porter la date du comité (ADR 0052).
    decidedOn: [CapaActionDialogComponent.today()],
    // Corrective par défaut : c'est ce qu'on entend par « action d'une CAPA ».
    // Le choix existe pour dire l'endiguement quand c'en est un — pas pour
    // obliger chacun à requalifier ce que tout le monde comprend déjà.
    actionType: <CapaActionType>'CORRECTIVE',
    assigneeName: ['', [Validators.maxLength(255)]],
    dueDate: [''],

    // La verification d'efficacite du DOSSIER, posee ici quand on arrive d'une
    // non-conformite (cf. `askVerification`). `null` = pas tranche, distinct de
    // `false` : dire « non » est une decision (ADR 0073).
    //
    // Le validateur `required` est pose A LA CONSTRUCTION quand la question est
    // demandee -- donc jamais pendant un rendu. C'est l'attache CONDITIONNELLE
    // en cours de detection qui produit NG0100, pas le validateur lui-meme.
    verificationRequired: [null as boolean | null],
    verificationAssigneeId: [null as string | null],
    verificationInstructions: ['', [Validators.maxLength(4000)]]
  });

  /**
   * Les membres de l'organisation, pour designer le verificateur.
   *
   * <p>L'annuaire du client (`/api/v1/users`) et non une saisie libre : un
   * verificateur est un vrai compte, a qui on peut notifier la tache, et deux
   * orthographes du meme nom ne peuvent pas devenir deux personnes.
   */
  membres: TenantUser[] = [];

  /**
   * Vrai quand l'annuaire n'a pas repondu : on le DIT, on ne fait pas semblant.
   *
   * <p>Une liste deroulante vide ferait croire a l'utilisateur que son
   * organisation ne compte personne, et il chercherait du mauvais cote.
   */
  private readonly annuaireState$ = new BehaviorSubject<boolean>(false);
  readonly annuaireIndisponible$ = deferredView(this.annuaireState$);

  /** Etat courant, pour la logique et les bancs -- le gabarit passe par le flux. */
  get annuaireIndisponible(): boolean {
    return this.annuaireState$.value;
  }

  /**
   * Vrai quand le bloc « a qui » et « quoi verifier » a lieu d'etre rempli.
   *
   * <p>Livre en MACROTACHE (`deferredView`), et non lu directement : le groupe
   * de boutons radio se synchronise PENDANT le premier rendu, si bien qu'une
   * lecture directe change de valeur au milieu de la passe de detection et
   * declenche NG0100. Un microtour ne suffit pas -- sous Zone.js il peut encore
   * retomber dans la meme passe. C'est le remede deja employe par le dialogue
   * d'edition, apres quatre passes de NG0100 sur exactement ce bloc.
   */
  private readonly exigenceState$ = new BehaviorSubject<boolean>(false);
  readonly verificationExigee$ = deferredView(this.exigenceState$);

  /** Etat courant, pour la logique et les bancs. */
  get verificationExigee(): boolean {
    return this.exigenceState$.value;
  }

  /** Natures proposées, dans l'ordre du déroulé réel d'un traitement. */
  readonly actionTypes: { value: CapaActionType; label: string }[] = [
    { value: 'CONTAINMENT', label: $localize`:@@capa.action-type.containment:Endiguement` },
    { value: 'CORRECTIVE',  label: $localize`:@@capa.action-type.corrective:Corrective` },
    { value: 'PREVENTIVE',  label: $localize`:@@capa.action-type.preventive:Préventive` }
  ];

  /** Date du jour au format d'un input[type=date], sans dépendance de fuseau serveur. */
  private static today(): string {
    const d = new Date();
    const mois = String(d.getMonth() + 1).padStart(2, '0');
    const jour = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mois}-${jour}`;
  }

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaActionDialogComponent, CapaActionResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CapaActionDialogData,
    private readonly equipe: TenantTeamService
  ) {
    if (!data.askVerification) return;

    // Trancher est OBLIGATOIRE sur ce chemin. Ailleurs, ne pas repondre laisse
    // la question ouverte et c'est acceptable ; ici, on pose une action
    // corrective, et c'est le moment ou la question a un sens. Pose a la
    // construction : hors de tout rendu.
    this.form.controls.verificationRequired.addValidators(Validators.required);

    // L'abonnement ne fait QUE publier l'etat : aucune mutation du formulaire
    // ici. Le groupe de boutons radio se synchronise pendant le premier rendu,
    // et toucher au formulaire a ce moment ferait basculer `form.invalid` -- lu
    // par le bouton d'envoi -- au milieu de la passe de detection.
    this.form.controls.verificationRequired.valueChanges.subscribe(
      exigee => this.exigenceState$.next(exigee === true));

    // Les mutations, elles, suivent le flux DIFFERE : livrees en macrotache,
    // donc jamais pendant un rendu.
    this.verificationExigee$.subscribe(exigee => this.appliquerExigence(exigee));
  }

  ngOnInit(): void {
    if (!this.data.askVerification) return;
    this.equipe.list(0, 200).subscribe({
      next: page => (this.membres = page.content.filter(m => m.active)),
      error: () => this.annuaireState$.next(true)
    });
  }

  /**
   * Ce qu'entraine l'exigence, ou son retrait.
   *
   * <p>Appele depuis le flux DIFFERE, donc toujours hors d'une passe de
   * detection : c'est ce qui permet de toucher au formulaire sans faire
   * basculer `form.invalid` au milieu d'un rendu.
   *
   * <p>Une seule source pour la regle « exiger sans designer ne veut rien
   * dire » : ce validateur. Le doubler d'un `required` dans le gabarit donnait
   * deux validateurs, dont un seul se retirait.
   */
  private appliquerExigence(exigee: boolean): void {
    const qui = this.form.controls.verificationAssigneeId;

    if (exigee) {
      qui.addValidators(Validators.required);
      qui.updateValueAndValidity({ emitEvent: false });
      return;
    }

    qui.removeValidators(Validators.required);
    // Ne plus exiger efface ce qui n'a plus d'objet : laisser un verificateur
    // derriere soi laisserait croire qu'une verification est encore attendue.
    qui.setValue(null, { emitEvent: false });
    this.form.controls.verificationInstructions.setValue('', { emitEvent: false });
    qui.updateValueAndValidity({ emitEvent: false });
  }

  /** Le libelle du membre, tel qu'il sera fige dans le dossier. */
  private nomDu(id: string | null): string | undefined {
    if (!id) return undefined;
    return this.membres.find(m => m.id === id)?.email;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    const { title, description, decidedOn, actionType, assigneeName, dueDate } = v;

    // La VERIFICATION D'ABORD, l'action ensuite, et cet ordre n'est pas
    // indifferent. C'est la verification qui porte les gardes du serveur
    // (exiger sans designer, designer sans exiger -> 422). Creer l'action
    // d'abord puis echouer sur la verification laisserait une action creee que
    // l'utilisateur, en reessayant, DOUBLERAIT. Echouer avant de rien creer ne
    // laisse rien derriere soi.
    this.decisionDeVerification()
      .pipe(
        switchMap(() => this.capa.addAction(this.data.caseId, {
          title: title.trim(),
          description: description?.trim() || undefined,
          decidedOn: decidedOn || undefined,
          actionType,
          assigneeName: assigneeName?.trim() || undefined,
          dueDate: dueDate || undefined
        })),
        finalize(() => (this.submitting = false)))
      .subscribe({
        next: action => {
          this.snack.open($localize`:@@capa.action.added:Action ajoutée.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(action);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-action-dialog] addAction failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-add:Erreur lors de l'ajout.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  /**
   * Enregistre la decision de verification, ou ne fait rien.
   *
   * <p>Rien du tout quand la question n'est pas posee sur ce chemin : le
   * dialogue ouvert depuis une fiche CAPA ne doit pas toucher a une decision
   * reglee ailleurs, et un envoi « a vide » l'ecraserait.
   *
   * <p>La mise a jour du dossier est PARTIELLE : seuls les champs de
   * verification partent. Envoyer l'intitule ou la criticite depuis ce
   * formulaire, qui ne les affiche pas, les remplacerait par du vide.
   */
  private decisionDeVerification(): Observable<CapaCaseResponse | null> {
    if (!this.data.askVerification) return of(null);

    const v = this.form.getRawValue();
    const exigee = v.verificationRequired === true;
    return this.capa.updateCase(this.data.caseId, {
      verificationRequired: v.verificationRequired ?? undefined,
      verificationAssigneeId: exigee ? (v.verificationAssigneeId ?? undefined) : undefined,
      // Le nom part avec l'identifiant : il est recopie dans le dossier pour
      // rester lisible meme si le compte disparait de l'annuaire (ADR 0073).
      verificationAssigneeName: exigee ? this.nomDu(v.verificationAssigneeId) : undefined,
      verificationInstructions: exigee
        ? (v.verificationInstructions?.trim() || undefined) : undefined
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
