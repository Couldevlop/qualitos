import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ComplaintsService } from '../../complaints.service';
import {
  COMPLAINT_CATEGORIES, COMPLAINT_CATEGORY_LABEL, COMPLAINT_CHANNELS, COMPLAINT_CHANNEL_LABEL,
  COMPLAINT_SEVERITIES, COMPLAINT_SEVERITY_LABEL, Complaint, ComplaintCategory, ComplaintChannel,
  ComplaintSeverity, UUID_PATTERN
} from '../../complaints.types';

/** Code métier : alphanumérique en tête, puis [A-Za-z0-9._-], 2 à 100 caractères. */
const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]{1,99}$/;

/**
 * Saisie d'une réclamation (§4.9 — canal e-mail, téléphone, formulaire, ITSM…).
 *
 * `createdBy` est imposé par le serveur (@NotNull UUID) : il vient du JWT via
 * AuthService, jamais d'une saisie. Sans identité exploitable la création est
 * bloquée plutôt que d'envoyer une requête vouée au 400.
 */
@Component({
  selector: 'qos-complaint-create-dialog',
  templateUrl: './complaint-create-dialog.component.html',
  styleUrls: ['./complaint-create-dialog.component.scss'],
  standalone: false
})
export class ComplaintCreateDialogComponent {

  submitting = false;

  readonly channels = COMPLAINT_CHANNELS;
  readonly categories = COMPLAINT_CATEGORIES;
  readonly severities = COMPLAINT_SEVERITIES;
  readonly channelLabel = COMPLAINT_CHANNEL_LABEL;
  readonly categoryLabel = COMPLAINT_CATEGORY_LABEL;
  readonly severityLabel = COMPLAINT_SEVERITY_LABEL;

  readonly form = this.fb.nonNullable.group({
    code: [this.suggestCode(), [Validators.required, Validators.maxLength(100), Validators.pattern(CODE_PATTERN)]],
    channel: ['EMAIL' as ComplaintChannel, Validators.required],
    subject: ['', [Validators.required, Validators.maxLength(250)]],
    description: ['', Validators.maxLength(4000)],
    severity: ['MEDIUM' as ComplaintSeverity, Validators.required],
    category: ['OTHER' as ComplaintCategory, Validators.required],
    customerName: ['', Validators.maxLength(250)],
    customerEmail: ['', [Validators.email, Validators.maxLength(320)]],
    customerExternalId: ['', Validators.maxLength(200)],
    supplierId: ['', Validators.pattern(UUID_PATTERN)],
    assignedToUserId: ['', Validators.pattern(UUID_PATTERN)],
    receivedAt: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ComplaintsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ComplaintCreateDialogComponent, Complaint>
  ) {}

  /** Identité de l'auteur, seule source acceptée pour `createdBy`. */
  get authorId(): string | null {
    const id = this.auth.snapshot()?.userId;
    return id && UUID_PATTERN.test(id) ? id : null;
  }

  submit(): void {
    if (this.form.invalid || this.submitting || !this.authorId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.create({
      code: v.code.trim(),
      channel: v.channel,
      customerName: blankToNull(v.customerName),
      customerEmail: blankToNull(v.customerEmail),
      customerExternalId: blankToNull(v.customerExternalId),
      subject: v.subject.trim(),
      description: blankToNull(v.description),
      severity: v.severity,
      category: v.category,
      supplierId: blankToNull(v.supplierId),
      assignedToUserId: blankToNull(v.assignedToUserId),
      createdBy: this.authorId,
      receivedAt: toInstant(v.receivedAt)
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: created => {
          this.snack.open($localize`:@@complaints.create.success:Réclamation enregistrée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(created);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[complaint-create] failed', (err as { status?: number })?.status);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@complaints.create.error:Création impossible — le code est peut-être déjà utilisé.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Code proposé par défaut : REC-AAAAMMJJ-XXXX. L'utilisateur reste libre de le
   * remplacer par sa propre référence (n° de ticket ITSM, dossier client…).
   */
  private suggestCode(): string {
    const now = new Date();
    const day = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`;
    const suffix = Math.floor(Math.random() * 0x10000).toString(16).toUpperCase().padStart(4, '0');
    return `REC-${day}-${suffix}`;
  }
}

function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n);
}

/** Un champ vide ne doit pas partir en chaîne vide : le serveur attend `null`. */
function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

/**
 * `datetime-local` produit une heure locale sans fuseau ; le serveur attend un
 * Instant. Une saisie non interprétable est ignorée plutôt que de partir en 400
 * (le serveur horodate alors la réception à maintenant).
 */
function toInstant(localValue: string): string | null {
  if (!localValue) return null;
  const parsed = new Date(localValue);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString();
}
