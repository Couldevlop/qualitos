import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, from, of } from 'rxjs';
import { catchError, concatMap, finalize, map, switchMap, tap, toArray } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CAPA_TYPES } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import {
  CapaCaseResponse,
  CapaCriticity,
  CapaSourceType,
  CapaType
} from '../../capa.types';

@Component({
  selector: 'qos-capa-create-dialog',
  templateUrl: './capa-create-dialog.component.html',
  styleUrls: ['./capa-create-dialog.component.scss'],
  standalone: false
})
export class CapaCreateDialogComponent {

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

  readonly types = CAPA_TYPES;
  readonly criticities: CapaCriticity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly sourceTypes: { value: CapaSourceType; label: string }[] = [
    { value: 'NON_CONFORMITY', label: $localize`:@@capa.source.non-conformity:Non-conformité` },
    { value: 'AUDIT',          label: $localize`:@@capa.source.audit:Audit` },
    { value: 'COMPLAINT',      label: $localize`:@@capa.source.complaint:Réclamation client` },
    { value: 'INTERNAL',       label: $localize`:@@capa.source.internal:Détection interne` },
    { value: 'IOT_ALERT',      label: $localize`:@@capa.source.iot-alert:Alerte IoT` },
    { value: 'OTHER',          label: $localize`:@@capa.source.other:Autre` }
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    type: ['CORRECTIVE' as CapaType, [Validators.required]],
    criticity: ['MEDIUM' as CapaCriticity, [Validators.required]],
    sourceType: ['INTERNAL' as CapaSourceType, [Validators.required]],
    sourceRef: ['', [Validators.maxLength(255)]],
    dueDate: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaCreateDialogComponent, CapaCaseResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.capa
      .createCase({
        title: v.title.trim(),
        description: v.description?.trim() || undefined,
        type: v.type,
        criticity: v.criticity,
        sourceType: v.sourceType,
        sourceRef: v.sourceRef?.trim() || undefined,
        dueDate: v.dueDate || undefined,
        ownerId
      })
      .pipe(
        // Le dossier d'abord, ses pièces ensuite : l'inverse n'existe pas.
        switchMap(c => this.uploadAttachments(c)),
        finalize(() => (this.submitting = false))
      )
      .subscribe({
        next: c => {
          this.snack.open(this.createdMessage(), $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
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
  private uploadAttachments(created: CapaCaseResponse): Observable<CapaCaseResponse> {
    if (this.attachments.length === 0) {
      return of(created);
    }
    this.uploaded = 0;
    return from(this.attachments).pipe(
      concatMap(file =>
        this.capa.uploadEvidence(created.id, file).pipe(
          tap(() => this.uploaded++),
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-create] uploadEvidence failed', err?.status, err?.error?.type);
            return of(null);
          })
        )
      ),
      toArray(),
      map(() => created)
    );
  }

  /** Le message dit ce qui est réellement joint, pas ce qui était prévu. */
  private createdMessage(): string {
    const total = this.attachments.length;
    if (total === 0) {
      return $localize`:@@capa.create.success:Cas CAPA créé.`;
    }
    if (this.uploaded === total) {
      return $localize`:@@capa.create.success-with-evidence:Cas CAPA créé, pièces jointes déposées.`;
    }
    return $localize`:@@capa.create.success-partial-evidence:Cas CAPA créé, mais des pièces n'ont pas pu être déposées — reprends-les depuis la fiche.`;
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
