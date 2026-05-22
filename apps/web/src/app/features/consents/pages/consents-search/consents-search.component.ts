import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConsentsService } from '../../consents.service';
import {
  ConsentSource,
  ConsentStatus,
  ConsentView
} from '../../consents.types';
import { ConsentsGrantDialogComponent } from '../consents-grant-dialog/consents-grant-dialog.component';

type SearchMode = 'subject' | 'purpose';

@Component({
  selector: 'qos-consents-search',
  templateUrl: './consents-search.component.html',
  styleUrls: ['./consents-search.component.scss'],
  standalone: false
})
export class ConsentsSearchComponent {

  readonly displayedColumns = ['subjectLabel', 'purposeCode', 'purposeVersion', 'source', 'status', 'grantedAt'];

  readonly modes: { value: SearchMode; label: string }[] = [
    { value: 'subject', label: 'Par identifiant de personne (sera hashé côté serveur)' },
    { value: 'purpose', label: 'Par code de finalité' }
  ];

  // OWASP A03 — patterns mirror backend @Pattern / @Size constraints.
  readonly form = this.fb.nonNullable.group({
    mode: ['subject' as SearchMode, [Validators.required]],
    subjectIdentifier: ['', [Validators.maxLength(320)]],
    purposeCode: ['', [Validators.maxLength(64), Validators.pattern(/^[a-z][a-z0-9._-]{1,63}$/)]]
  });

  results: ConsentView[] = [];
  loading = false;
  error: string | null = null;
  searched = false;

  expiring = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ConsentsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  isSubjectMode(): boolean { return this.form.controls.mode.value === 'subject'; }

  submit(): void {
    const v = this.form.getRawValue();
    if (v.mode === 'subject' && !v.subjectIdentifier?.trim()) {
      this.snack.open('Renseignez un identifiant.', 'OK', { duration: 3000 });
      return;
    }
    if (v.mode === 'purpose' && !v.purposeCode?.trim()) {
      this.snack.open('Renseignez un code de finalité.', 'OK', { duration: 3000 });
      return;
    }
    this.loading = true;
    this.error = null;
    this.searched = true;
    const op$ = v.mode === 'subject'
      ? this.svc.searchBySubject(v.subjectIdentifier.trim())
      : this.svc.searchByPurpose(v.purposeCode.trim());
    op$
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: arr => (this.results = arr),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[consents-search] failed', err?.status, err?.error?.title);
          this.error = safeErrorMessage(err, 'Recherche impossible.');
          this.results = [];
        }
      });
  }

  openGrant(): void {
    const ref = this.dialog.open(ConsentsGrantDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(c => { if (c) this.router.navigate(['/consents', c.id]); });
  }

  expireDue(): void {
    if (this.expiring) return;
    this.expiring = true;
    this.svc.expireDue(200).subscribe({
      next: r => {
        this.expiring = false;
        this.snack.open('Maintenance : ' + r.expired + ' consentement(s) expiré(s).', 'OK', { duration: 3500 });
        if (this.searched) this.submit();
      },
      error: err => {
        this.expiring = false;
        // eslint-disable-next-line no-console
        console.warn('[consents-search] expire-due failed', err?.status, err?.error?.title);
        this.snack.open(safeErrorMessage(err, 'Maintenance impossible.'), 'OK', { duration: 4000 });
      }
    });
  }

  open(c: ConsentView): void { this.router.navigate(['/consents', c.id]); }

  sourceLabel(s: ConsentSource): string {
    return ({
      WEB_FORM: 'Formulaire web', MOBILE_APP: 'Application mobile', EMAIL: 'E-mail',
      PAPER: 'Papier', PHONE: 'Téléphone', API: 'API', IMPORT: 'Import', OTHER: 'Autre'
    })[s];
  }
  statusBadge(s: ConsentStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
