import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { StatusTone } from '../../../../shared/ui/status-pill/status-pill.component';
import { StandardsDocGenService } from '../../standards-doc-gen.service';
import { NormDocKind, NormDocStatus, NormDocView } from '../../standards-doc-gen.types';

/**
 * Revue & validation humaine d'une pièce générée par l'IA (§8.8, ADR 0032).
 * Le brouillon est soumis à revue, puis approuvé (signature humaine, par un
 * acteur ≠ soumetteur) ou rejeté avec motif. Aucune publication sans signature
 * humaine (§18.2 #5). L'acteur est toujours le sujet du JWT, jamais saisi ici.
 */
@Component({
  selector: 'qos-doc-review',
  templateUrl: './doc-review.component.html',
  styleUrls: ['./doc-review.component.scss'],
  standalone: false
})
export class DocReviewComponent implements OnInit {

  readonly approveForm = this.fb.group({
    signature: ['', [Validators.required, Validators.maxLength(2000)]],
    notes: ['']
  });

  readonly rejectForm = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  doc: NormDocView | null = null;
  loading = false;
  acting = false;
  error: string | null = null;
  showReject = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly service: StandardsDocGenService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.load(id);
    }
  }

  private load(id: string): void {
    this.loading = true;
    this.error = null;
    this.service.getNormDoc(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: d => (this.doc = d),
        error: (err: HttpErrorResponse) => (this.error = this.messageFor(err))
      });
  }

  submit(): void {
    this.act(this.service.submitNormDoc(this.doc!.id),
      $localize`:@@docreview.toast.submitted:Pièce soumise à validation.`);
  }

  approve(): void {
    if (this.acting || this.approveForm.invalid) {
      this.approveForm.markAllAsTouched();
      return;
    }
    this.act(this.service.approveNormDoc(this.doc!.id, {
      signature: this.approveForm.value.signature ?? '',
      notes: this.approveForm.value.notes || undefined
    }), $localize`:@@docreview.toast.approved:Pièce approuvée et signée.`);
  }

  reject(): void {
    if (this.acting || this.rejectForm.invalid) {
      this.rejectForm.markAllAsTouched();
      return;
    }
    this.act(this.service.rejectNormDoc(this.doc!.id, {
      reason: this.rejectForm.value.reason ?? ''
    }), $localize`:@@docreview.toast.rejected:Pièce rejetée — revenue en brouillon.`);
    this.showReject = false;
    this.rejectForm.reset({ reason: '' });
  }

  private act(obs: import('rxjs').Observable<NormDocView>, msg: string): void {
    if (this.acting || !this.doc) {
      return;
    }
    this.acting = true;
    this.error = null;
    obs.pipe(finalize(() => (this.acting = false))).subscribe({
      next: d => {
        this.doc = d;
        this.snack.open(msg, $localize`:@@docreview.toast.close:Fermer`, { duration: 4000 });
      },
      error: (err: HttpErrorResponse) => (this.error = this.messageFor(err))
    });
  }

  kindLabel(kind: NormDocKind): string {
    switch (kind) {
      case 'MANUAL': return $localize`:@@docreview.kind.manual:Manuel Qualité`;
      case 'POLICY': return $localize`:@@docreview.kind.policy:Politique Qualité`;
      default: return $localize`:@@docreview.kind.procedure:Procédure documentée`;
    }
  }

  statusLabel(status: NormDocStatus): string {
    switch (status) {
      case 'BROUILLON_IA': return $localize`:@@docreview.status.draft:Brouillon IA`;
      case 'EN_VALIDATION': return $localize`:@@docreview.status.review:En validation`;
      case 'APPROUVE': return $localize`:@@docreview.status.approved:Approuvé`;
      default: return $localize`:@@docreview.status.rejected:Rejeté`;
    }
  }

  statusTone(status: NormDocStatus): StatusTone {
    switch (status) {
      case 'APPROUVE': return 'success';
      case 'EN_VALIDATION': return 'accent';
      case 'REJETE': return 'danger';
      default: return 'neutral';
    }
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@docreview.err.backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@docreview.err.invalid:Requête invalide (signature/motif requis).`;
      case 403: return $localize`:@@docreview.err.forbidden:Droits insuffisants (approbation réservée au Directeur Qualité).`;
      case 404: return $localize`:@@docreview.err.notfound:Pièce introuvable.`;
      case 409: return $localize`:@@docreview.err.conflict:Transition impossible (séparation des tâches, état invalide).`;
      default: return $localize`:@@docreview.err.generic:Échec de l'opération (HTTP ${err.status}:status:).`;
    }
  }
}
