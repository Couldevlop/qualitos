import { Component, Inject, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ApqpService } from '../../apqp.service';
import { nonBlank } from '../../apqp.validators';
import {
  ApqpCycle, ApqpDataRow, ApqpDeliverable, ApqpEvidence, ApqpLinkedKind, ApqpPhase
} from '../../apqp.types';

export interface ApqpDeliverableDetailDialogData {
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
 * Ce qu'un livrable APQP demande, et ce qui le prouve.
 *
 * <p>Un popup par GENRE plutôt qu'un formulaire par livrable : le référentiel en
 * compte une cinquantaine, et les coder un par un les figerait. Ils ne diffèrent
 * que par la nature de ce qu'ils produisent — un document, un enregistrement déjà
 * tenu ailleurs dans QualitOS, des mesures, une liste de points à acquitter.
 *
 * <p>Le genre vient du serveur : l'écran ne le devine pas du libellé. Deviner
 * qu'« Control plan » renvoie au module des plans de surveillance marcherait sur
 * le référentiel et sur rien d'autre, et personne ne comprendrait pourquoi son
 * propre libellé n'ouvre pas le même formulaire.
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

  /** Les lignes de contenu : sous-points d'une checklist, ou mesures. */
  readonly rows: FormArray;

  evidences: ApqpEvidence[] = [];
  chargement = false;
  envoi = false;

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
    private readonly dialogRef: MatDialogRef<
      ApqpDeliverableDetailDialogComponent, ApqpCycle | undefined>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ApqpDeliverableDetailDialogData
  ) {
    const livrable = data.deliverable;
    this.rows = this.fb.array(
      (livrable.data ?? []).map(ligne => this.ligneEnGroupe(ligne)));

    this.form = this.fb.group({
      done: [livrable.done],
      comment: [livrable.comment ?? '', [Validators.maxLength(2000)]],
      linkedKind: [livrable.linkedKind ?? null],
      linkedId: [livrable.linkedId ?? '', [Validators.pattern(
        /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/)]],
      rows: this.rows
    }, { validators: [() => this.renvoiCoherent()] });

    if (!data.editable) {
      this.form.disable();
    }
  }

  ngOnInit(): void {
    if (this.data.deliverable.kind === 'ATTACHMENT') {
      this.chargerPieces();
    }
  }

  // ---------- ce que le genre commande ----------

  get kind(): ApqpDeliverable['kind'] {
    return this.data.deliverable.kind;
  }

  get titre(): string {
    return this.data.deliverable.label;
  }

  /**
   * Ce que ce genre de livrable attend, dit en une phrase.
   *
   * <p>Le genre seul ne parle pas à l'utilisateur : « MODULE_LINK » n'explique
   * pas qu'on attend le numéro d'une AMDEC déjà saisie.
   */
  get aide(): string {
    switch (this.kind) {
      case 'ATTACHMENT':
        return $localize`:@@apqp.kind-help.attachment:Joignez le document qui prouve ce livrable — Word, Excel, PDF ou photo.`;
      case 'MODULE_LINK':
        return $localize`:@@apqp.kind-help.module-link:Ce livrable est déjà tenu dans un module de QualitOS : désignez l'enregistrement concerné.`;
      case 'DATA_ENTRY':
        return $localize`:@@apqp.kind-help.data-entry:Saisissez les valeurs mesurées, avec leur unité et leur date.`;
      default:
        return $localize`:@@apqp.kind-help.checklist:Ce livrable n'est acquis que si tous ses points le sont.`;
    }
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
    if (this.form.hasError('renvoiManquant')) {
      return $localize`:@@apqp.deliverable.blocked-link:Désignez l'enregistrement avant de déclarer ce livrable acquis.`;
    }
    return this.form.invalid
      ? $localize`:@@apqp.deliverable.blocked-rows:Chaque ligne a besoin d'un intitulé.`
      : undefined;
  }

  get ariaRetirerLigne(): string {
    return $localize`:@@apqp.data.remove-row:Retirer cette ligne`;
  }

  ariaRetirerPiece(piece: ApqpEvidence): string {
    return $localize`:@@apqp.evidence.remove-aria:Retirer la pièce ${piece.originalFilename}:filename:`;
  }

  // ---------- contenu ----------

  groupe(index: number): FormGroup {
    return this.rows.at(index) as FormGroup;
  }

  ajouterLigne(): void {
    this.rows.push(this.ligneEnGroupe({ label: '' }));
  }

  retirerLigne(index: number): void {
    this.rows.removeAt(index);
  }

  // ---------- pièces jointes ----------

  choisirFichier(input: HTMLInputElement): void {
    const fichier = input.files?.[0];
    // On vide la saisie tout de suite : sans cela, reverser le même fichier après
    // un refus ne déclencherait aucun événement.
    input.value = '';
    if (!fichier) return;

    this.envoi = true;
    this.service.uploadEvidence(this.data.phase.id, this.data.deliverable.id, fichier)
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

    this.service.deleteEvidence(this.data.phase.id, this.data.deliverable.id, piece.id)
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

    this.service.completeDeliverable(this.data.phase.id, this.data.deliverable.id, {
      done: valeurs.done,
      comment: valeurs.comment?.trim() ? valeurs.comment.trim() : null,
      // Le contenu n'est envoyé que par les genres qui en portent : le serveur
      // refuse une liste de mesures sur une pièce jointe, et il a raison.
      data: this.kind === 'CHECKLIST' || this.kind === 'DATA_ENTRY'
        ? this.lignesEnvoyees()
        : null,
      linkedKind: this.kind === 'MODULE_LINK' ? valeurs.linkedKind ?? null : null,
      linkedId: this.kind === 'MODULE_LINK' && valeurs.linkedId
        ? valeurs.linkedId.trim()
        : null
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

  trackByIndex(index: number): number {
    return index;
  }

  trackById(_index: number, piece: ApqpEvidence): string {
    return piece.id;
  }

  // ---------- interne ----------

  private ligneEnGroupe(ligne: ApqpDataRow): FormGroup {
    return this.fb.group({
      // `nonBlank` en plus de `required` : ce dernier laisse passer '   ', et une
      // ligne sans intitule lisible part alors au serveur, qui la refuse en 422.
      label: [ligne.label ?? '', [Validators.required, nonBlank, Validators.maxLength(200)]],
      value: [ligne.value ?? '', [Validators.maxLength(60)]],
      unit: [ligne.unit ?? '', [Validators.maxLength(20)]],
      measuredAt: [ligne.measuredAt ?? null],
      checked: [ligne.checked ?? false]
    });
  }

  private lignesEnvoyees(): ApqpDataRow[] {
    return this.rows.getRawValue()
      .filter((ligne: ApqpDataRow) => (ligne.label ?? '').trim().length > 0)
      .map((ligne: ApqpDataRow) => ({
        label: (ligne.label ?? '').trim(),
        value: ligne.value ?? '',
        unit: ligne.unit ?? '',
        measuredAt: this.enDateIso(ligne.measuredAt),
        checked: ligne.checked ?? false
      }));
  }

  /**
   * La date au format que le serveur attend (yyyy-MM-dd).
   *
   * <p>Le sélecteur Material rend un `Date` ; l'envoyer tel quel produirait un
   * horodatage complet, que la validation refuse.
   */
  private enDateIso(valeur: unknown): string | null {
    if (!valeur) return null;
    const date = valeur instanceof Date ? valeur : new Date(String(valeur));
    return Number.isNaN(date.getTime()) ? null : date.toISOString().slice(0, 10);
  }

  /**
   * Un renvoi coché doit désigner son enregistrement.
   *
   * <p>Vérifié à l'écran aussi, et non seulement au serveur : mieux vaut
   * désactiver le bouton que proposer une action qu'on sait refusée.
   */
  private renvoiCoherent(): { [key: string]: boolean } | null {
    if (!this.form || this.kind !== 'MODULE_LINK') return null;
    const coche = this.form.get('done')?.value === true;
    const cible = this.form.get('linkedId')?.value;
    const genre = this.form.get('linkedKind')?.value;
    return coche && (!cible || !genre) ? { renvoiManquant: true } : null;
  }

  private chargerPieces(): void {
    this.chargement = true;
    this.service.evidences(this.data.phase.id, this.data.deliverable.id)
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
