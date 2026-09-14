import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ApqpService } from '../../apqp.service';
import {
  ApqpCycle, ApqpDeliverable, ApqpDeliverableStatus, ApqpEvidence, ApqpLinkedKind, ApqpPhase
} from '../../apqp.types';

export interface ApqpDeliverableDetailDialogData {
  /** Le projet dont relève la phase : toutes les routes du cycle en dépendent. */
  projectId: string;
  phase: ApqpPhase;
  deliverable: ApqpDeliverable;
  /** Vrai si l'utilisateur peut écrire : sinon le popup se lit, il ne se remplit pas. */
  editable: boolean;
}

/** Les modules vers lesquels un livrable peut renvoyer, avec leur libellé traduit. */
interface ChoixModule {
  value: ApqpLinkedKind;
  label: string;
}

/**
 * Le formulaire UNIQUE d'un livrable APQP.
 *
 * <p>Il y avait quatre corps, choisis par un « genre » posé à la création :
 * pièces, renvoi, mesures, points. C'était une erreur de fond. Le genre obligeait
 * à décider de ce qu'un livrable produirait AVANT de l'avoir travaillé, il
 * interdisait de joindre une preuve à un livrable qualifié « renvoi » — ce que
 * l'auditeur demande en premier — et il rendait sa case incochable.
 *
 * <p>Un seul corps, donc, où chaque champ est facultatif sauf la case : le
 * responsable, l'échéance, l'état, l'avancement, les notes, les pièces et le
 * renvoi cohabitent, et c'est l'usage qui décide de ce qu'on remplit.
 *
 * <p>La case PILOTE l'état et l'avancement, ici comme au serveur : cocher fixe
 * « acquis » et 100 %, décocher ramène sous les 100. Les trois contrôles sont
 * synchronisés à l'écran pour que l'utilisateur voie la règle plutôt que de la
 * découvrir après enregistrement.
 */
@Component({
  selector: 'qos-apqp-deliverable-detail-dialog',
  templateUrl: './apqp-deliverable-detail-dialog.component.html',
  styleUrls: ['./apqp-deliverable-detail-dialog.component.scss'],
  standalone: false
})
export class ApqpDeliverableDetailDialogComponent implements OnInit {

  /** Les types que le serveur accepte, pour que le sélecteur de fichier les propose. */
  static readonly TYPES_ACCEPTES = [
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/pdf',
    'image/png',
    'image/jpeg'
  ].join(',');

  readonly accept = ApqpDeliverableDetailDialogComponent.TYPES_ACCEPTES;

  readonly form: FormGroup;

  evidences: ApqpEvidence[] = [];
  chargement = false;
  envoi = false;

  readonly statuts: ApqpDeliverableStatus[] =
    ['NOT_STARTED', 'IN_PROGRESS', 'BLOCKED', 'DONE'];

  readonly modules: ChoixModule[] = [
    { value: 'FMEA', label: $localize`:@@apqp.link.fmea:AMDEC (DFMEA / PFMEA)` },
    { value: 'CONTROL_PLAN', label: $localize`:@@apqp.link.control-plan:Plan de surveillance` },
    { value: 'PDCA', label: $localize`:@@apqp.link.pdca:Cycle PDCA` },
    { value: 'CAPA', label: $localize`:@@apqp.link.capa:Action corrective (CAPA)` }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: ApqpService,
    private readonly snack: MatSnackBar,
    private readonly router: Router,
    private readonly dialogRef: MatDialogRef<
      ApqpDeliverableDetailDialogComponent, ApqpCycle | undefined>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ApqpDeliverableDetailDialogData
  ) {
    const livrable = data.deliverable;

    this.form = this.fb.group({
      done: [livrable.done],
      expectedArtifact: [livrable.expectedArtifact ?? '', [Validators.maxLength(1000)]],
      ppap: [livrable.ppap],
      owner: [livrable.owner ?? '', [Validators.maxLength(150)]],
      dueDate: [livrable.dueDate ?? null],
      status: [livrable.status],
      percentComplete: [
        livrable.percentComplete, [Validators.min(0), Validators.max(100)]],
      comment: [livrable.comment ?? '', [Validators.maxLength(2000)]],
      linkedKind: [livrable.linkedKind ?? null],
      linkedId: [livrable.linkedId ?? '', [Validators.pattern(
        /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/)]]
    }, { validators: [() => this.renvoiCoherent()] });

    // La case pilote : on reflète la règle du serveur DANS le formulaire, pour
    // que l'utilisateur la voie au lieu de la découvrir après enregistrement.
    this.form.get('done')!.valueChanges.subscribe(
      (coche: boolean) => this.refleterLaCase(coche));

    if (!data.editable) {
      // `emitEvent: false` : desactiver emet sinon un changement par controle,
      // qui rejouerait la regle de la case sur un formulaire qu'on ne remplit
      // pas -- et modifierait l'affichage d'un livrable en simple lecture.
      this.form.disable({ emitEvent: false });
    }
  }

  ngOnInit(): void {
    // TOUJOURS : un livrable quelconque peut avoir une preuve, et c'est ce que
    // l'auditeur demande en premier. L'ancien popup ne chargeait les pièces que
    // pour un genre, et les autres semblaient n'en porter aucune.
    this.chargerPieces();
  }

  get titre(): string {
    return this.data.deliverable.label;
  }

  /**
   * Pourquoi le bouton d'enregistrement est barré, s'il l'est.
   *
   * <p>Un bouton désactivé sans explication laisse l'utilisateur chercher : la
   * raison se lit à côté.
   */
  get blocage(): string | undefined {
    if (!this.data.editable) {
      return $localize`:@@apqp.deliverable.read-only:Vous pouvez consulter ce livrable, pas le modifier.`;
    }
    if (this.form.hasError('renvoiIncomplet')) {
      return $localize`:@@apqp.deliverable.blocked-link:Un renvoi se pose entier : le module ET son identifiant, ou ni l'un ni l'autre.`;
    }
    return this.form.invalid
      ? $localize`:@@apqp.deliverable.blocked-fields:Un champ du formulaire est hors limites.`
      : undefined;
  }

  ariaRetirerPiece(piece: ApqpEvidence): string {
    return $localize`:@@apqp.evidence.remove-aria:Retirer la pièce ${piece.originalFilename}:filename:`;
  }

  statutLabel(statut: ApqpDeliverableStatus): string {
    return ({
      NOT_STARTED: $localize`:@@apqp.status.not-started:Non commencé`,
      IN_PROGRESS: $localize`:@@apqp.status.in-progress:En cours`,
      BLOCKED: $localize`:@@apqp.status.blocked:Bloqué`,
      DONE: $localize`:@@apqp.status.done:Acquis`
    })[statut];
  }

  /**
   * La route de l'enregistrement visé, s'il en a une.
   *
   * <p>Un plan de surveillance n'en a pas : il vit dans l'onglet d'un produit, et
   * rien ne l'atteint par son seul identifiant. On le dit plutôt que d'offrir un
   * lien qui tomberait à côté.
   */
  get routeEnregistrement(): string[] | null {
    const { linkedKind, linkedId } = this.form.getRawValue();
    if (!linkedKind || !linkedId) {
      return null;
    }
    switch (linkedKind) {
      case 'FMEA': return ['/fmea', linkedId];
      case 'PDCA': return ['/pdca', linkedId];
      case 'CAPA': return ['/capa', linkedId];
      default: return null;   // CONTROL_PLAN : pas de route par identifiant
    }
  }

  /** Vrai quand le renvoi est posé mais qu'aucune route ne mène à la fiche. */
  get renvoiSansRoute(): boolean {
    const valeurs = this.form.getRawValue();
    return !!valeurs.linkedKind && !!valeurs.linkedId && this.routeEnregistrement === null;
  }

  /**
   * Ouvre la fiche visée, en refermant le popup.
   *
   * <p>Sans la fermeture, le dialogue resterait par-dessus l'écran d'arrivée et
   * l'utilisateur croirait que rien n'a bougé.
   */
  ouvrirEnregistrement(): void {
    const route = this.routeEnregistrement;
    if (!route) {
      return;
    }
    this.dialogRef.close();
    void this.router.navigate(route);
  }

  // ---------- pièces jointes ----------

  choisirFichier(input: HTMLInputElement): void {
    const fichier = input.files?.[0];
    // On vide la saisie tout de suite : sans cela, reverser le même fichier après
    // un refus ne déclencherait aucun événement.
    input.value = '';
    if (!fichier) return;

    this.envoi = true;
    this.service.uploadEvidence(
      this.data.projectId, this.data.phase.id, this.data.deliverable.id, fichier)
      .pipe(finalize(() => (this.envoi = false)))
      .subscribe({
        next: () => {
          this.chargerPieces();
          this.annoncer($localize`:@@apqp.evidence.added:Pièce versée au livrable.`);
        },
        error: err => this.echouer(err)
      });
  }

  retirerPiece(piece: ApqpEvidence): void {
    const question = $localize`:@@apqp.evidence.confirm-delete:Retirer « ${piece.originalFilename}:filename: » de ce livrable ?`;
    if (!confirm(question)) return;

    this.service.deleteEvidence(
      this.data.projectId, this.data.phase.id, this.data.deliverable.id, piece.id)
      .subscribe({
        next: () => this.chargerPieces(),
        error: err => this.echouer(err)
      });
  }

  /** Taille lisible : 1,2 Mo plutôt que 1258291. */
  taille(octets: number): string {
    return octets >= 1024 * 1024
      ? `${(octets / (1024 * 1024)).toFixed(1)} Mo`
      : `${Math.max(1, Math.round(octets / 1024))} Ko`;
  }

  // ---------- enregistrement ----------

  submit(): void {
    if (this.form.invalid || this.envoi) {
      this.form.markAllAsTouched();
      return;
    }
    const valeurs = this.form.getRawValue();
    this.envoi = true;

    this.service.completeDeliverable(
      this.data.projectId, this.data.phase.id, this.data.deliverable.id, {
        done: valeurs.done,
        expectedArtifact: this.rogne(valeurs.expectedArtifact),
        ppap: valeurs.ppap,
        owner: this.rogne(valeurs.owner),
        dueDate: this.enDateIso(valeurs.dueDate),
        // Le serveur applique la règle « la case pilote » ; l'écran envoie ce
        // qu'il affiche, et les deux disent alors la même chose.
        status: valeurs.status,
        percentComplete: valeurs.percentComplete,
        comment: this.rogne(valeurs.comment),
        // Un renvoi à moitié posé vaut un 422 : le validateur l'a déjà refusé,
        // et l'un sans l'autre ne part donc jamais.
        linkedKind: valeurs.linkedKind ?? null,
        linkedId: this.rogne(valeurs.linkedId)
      })
      .pipe(finalize(() => (this.envoi = false)))
      .subscribe({
        next: cycle => this.dialogRef.close(cycle),
        error: err => this.echouer(err)
      });
  }

  fermer(): void {
    this.dialogRef.close();
  }

  trackById(_index: number, piece: ApqpEvidence): string {
    return piece.id;
  }

  // ---------- interne ----------

  /**
   * Aligne l'état et l'avancement sur la case, comme le fera le serveur.
   *
   * <p>`emitEvent: false` : ces deux contrôles ne pilotent rien en retour, et une
   * émission relancerait la validation croisée au milieu de sa propre exécution.
   */
  private refleterLaCase(coche: boolean): void {
    const statut = this.form.get('status')!;
    const avancement = this.form.get('percentComplete')!;

    if (coche) {
      statut.setValue('DONE', { emitEvent: false });
      avancement.setValue(100, { emitEvent: false });
      return;
    }
    if (statut.value === 'DONE') {
      statut.setValue('IN_PROGRESS', { emitEvent: false });
    }
    if (Number(avancement.value) >= 100) {
      avancement.setValue(0, { emitEvent: false });
    }
  }

  private rogne(valeur: unknown): string | null {
    const texte = typeof valeur === 'string' ? valeur.trim() : '';
    return texte.length > 0 ? texte : null;
  }

  /**
   * La date au format que le serveur attend (yyyy-MM-dd).
   *
   * <p>Le champ natif rend déjà « aaaa-mm-jj » ; la conversion couvre le cas où
   * un sélecteur rendrait un `Date`, dont l'horodatage complet serait refusé.
   */
  private enDateIso(valeur: unknown): string | null {
    if (!valeur) return null;
    const date = valeur instanceof Date ? valeur : new Date(String(valeur));
    return Number.isNaN(date.getTime()) ? null : date.toISOString().slice(0, 10);
  }

  /**
   * Un renvoi se pose ENTIER, ou pas du tout.
   *
   * <p>Vérifié à l'écran aussi, et non seulement au serveur : mieux vaut
   * désactiver le bouton que proposer une action qu'on sait refusée par un 422.
   */
  private renvoiCoherent(): { [key: string]: boolean } | null {
    if (!this.form) return null;
    const genre = this.form.get('linkedKind')?.value;
    const cible = this.form.get('linkedId')?.value;
    const poseGenre = !!genre;
    const poseCible = !!(typeof cible === 'string' ? cible.trim() : cible);
    return poseGenre !== poseCible ? { renvoiIncomplet: true } : null;
  }

  private chargerPieces(): void {
    this.chargement = true;
    this.service.evidences(
      this.data.projectId, this.data.phase.id, this.data.deliverable.id)
      .pipe(finalize(() => (this.chargement = false)))
      .subscribe({
        next: pieces => (this.evidences = pieces),
        error: err => this.echouer(err)
      });
  }

  private annoncer(message: string): void {
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 2500 });
  }

  private echouer(err: unknown): void {
    this.snack.open(
      safeErrorMessage(err, $localize`:@@apqp.deliverable.failed:Opération impossible sur ce livrable.`),
      $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
