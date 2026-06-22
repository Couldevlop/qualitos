import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { StatusTone } from '../../../../shared/ui/status-pill/status-pill.component';
import { StandardsService } from '../../../standards/standards.service';
import { StandardSummary } from '../../../standards/standards.types';
import { StandardsDocGenService } from '../../standards-doc-gen.service';
import {
  DossierDocStatus, DossierDocumentView, DossierStatus, DossierView, NormDocKind
} from '../../standards-doc-gen.types';

/**
 * Génération documentaire IA AVANCÉE multi-documents (Standards Hub §8.8) :
 * à partir du contexte tenant (organisation, secteur, taille) et de la norme
 * visée, l'IA génère en lot un dossier complet (Manuel Qualité multi-sections,
 * Politique, procédures documentées). Chaque pièce devient un brouillon soumis
 * au workflow de validation humaine ; une fois toutes approuvées, le dossier est
 * finalisé (signature ML-DSA + ancrage blockchain). Différenciateur vs
 * MasterControl/ETQ : génération bout-en-bout pré-remplie en minutes.
 */
@Component({
  selector: 'qos-doc-gen',
  templateUrl: './doc-gen.component.html',
  styleUrls: ['./doc-gen.component.scss'],
  standalone: false
})
export class DocGenComponent implements OnInit {

  readonly form = this.fb.group({
    standardId: ['', [Validators.required]],
    organizationName: ['', [Validators.required, Validators.maxLength(500)]],
    industry: [''],
    size: [''],
    language: ['fr']
  });

  readonly finalizeForm = this.fb.group({
    signature: ['', [Validators.required, Validators.maxLength(2000)]],
    notes: ['']
  });

  standards: StandardSummary[] = [];
  catalog: DossierDocumentView[] = [];
  /** Clés des pièces sélectionnées ; vide = plan complet par défaut. */
  selectedKeys = new Set<string>();

  dossiers: DossierView[] = [];
  active: DossierView | null = null;

  readonly cols = ['standard', 'org', 'progress', 'status', 'actions'];

  loadingStandards = false;
  loadingCatalog = false;
  generating = false;
  retrying = false;
  finalizing = false;
  error: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: StandardsDocGenService,
    private readonly standardsService: StandardsService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadStandards();
    this.loadCatalog();
    this.loadDossiers();
  }

  // ---- chargements ----

  private loadStandards(): void {
    this.loadingStandards = true;
    this.standardsService.listCatalog(0, 100)
      .pipe(finalize(() => (this.loadingStandards = false)))
      .subscribe({
        next: page => (this.standards = page.content),
        error: () => (this.standards = [])
      });
  }

  private loadCatalog(): void {
    this.loadingCatalog = true;
    this.service.catalog()
      .pipe(finalize(() => (this.loadingCatalog = false)))
      .subscribe({
        next: cat => (this.catalog = cat),
        error: () => (this.catalog = [])
      });
  }

  private loadDossiers(): void {
    this.service.list().subscribe({
      next: list => (this.dossiers = list),
      error: () => (this.dossiers = [])
    });
  }

  // ---- sélection de pièces ----

  toggleKey(key: string): void {
    if (this.selectedKeys.has(key)) {
      this.selectedKeys.delete(key);
    } else {
      this.selectedKeys.add(key);
    }
  }

  isSelected(key: string): boolean {
    return this.selectedKeys.has(key);
  }

  selectionLabel(): string {
    return this.selectedKeys.size === 0
      ? $localize`:@@docgen.selection.all:Plan complet (toutes les pièces)`
      : $localize`:@@docgen.selection.count:${this.selectedKeys.size}:count: pièce(s) sélectionnée(s)`;
  }

  // ---- génération ----

  start(): void {
    if (this.generating || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.generating = true;
    this.error = null;
    const v = this.form.value;
    this.service.start({
      standardId: v.standardId ?? '',
      tenantProfile: {
        organizationName: v.organizationName ?? '',
        industry: v.industry || undefined,
        size: v.size || undefined,
        language: v.language || 'fr'
      },
      documentKeys: this.selectedKeys.size > 0 ? Array.from(this.selectedKeys) : undefined
    }).pipe(finalize(() => (this.generating = false)))
      .subscribe({
        next: dossier => {
          this.active = dossier;
          this.loadDossiers();
          this.snack.open(
            $localize`:@@docgen.toast.generated:Dossier généré : ${dossier.generatedCount}:n: pièce(s) en brouillon.`,
            $localize`:@@docgen.toast.close:Fermer`, { duration: 5000 });
        },
        error: (err: HttpErrorResponse) => (this.error = this.messageFor(err))
      });
  }

  open(dossier: DossierView): void {
    this.error = null;
    this.service.get(dossier.id).subscribe({
      next: d => (this.active = d),
      error: (err: HttpErrorResponse) => (this.error = this.messageFor(err))
    });
  }

  retry(): void {
    if (!this.active || this.retrying) {
      return;
    }
    this.retrying = true;
    this.error = null;
    this.service.retry(this.active.id)
      .pipe(finalize(() => (this.retrying = false)))
      .subscribe({
        next: d => {
          this.active = d;
          this.loadDossiers();
        },
        error: (err: HttpErrorResponse) => (this.error = this.messageFor(err))
      });
  }

  finalize(): void {
    if (!this.active || this.finalizing || this.finalizeForm.invalid) {
      this.finalizeForm.markAllAsTouched();
      return;
    }
    this.finalizing = true;
    this.error = null;
    this.service.finalize(this.active.id, {
      signature: this.finalizeForm.value.signature ?? '',
      notes: this.finalizeForm.value.notes || undefined
    }).pipe(finalize(() => (this.finalizing = false)))
      .subscribe({
        next: d => {
          this.active = d;
          this.finalizeForm.reset({ signature: '', notes: '' });
          this.loadDossiers();
          this.snack.open(
            $localize`:@@docgen.toast.finalized:Dossier finalisé, signé et ancré.`,
            $localize`:@@docgen.toast.close:Fermer`, { duration: 5000 });
        },
        error: (err: HttpErrorResponse) => (this.error = this.messageFor(err))
      });
  }

  // ---- helpers d'affichage ----

  canFinalize(d: DossierView): boolean {
    return d.status !== 'FINALISE' && d.generatedCount > 0;
  }

  hasFailures(d: DossierView): boolean {
    return d.failedCount > 0;
  }

  kindLabel(kind: NormDocKind): string {
    switch (kind) {
      case 'MANUAL': return $localize`:@@docgen.kind.manual:Manuel`;
      case 'POLICY': return $localize`:@@docgen.kind.policy:Politique`;
      default: return $localize`:@@docgen.kind.procedure:Procédure`;
    }
  }

  docStatusLabel(status: DossierDocStatus): string {
    switch (status) {
      case 'EN_ATTENTE': return $localize`:@@docgen.docstatus.pending:En attente`;
      case 'EN_GENERATION': return $localize`:@@docgen.docstatus.generating:Génération…`;
      case 'GENERE': return $localize`:@@docgen.docstatus.generated:Brouillon généré`;
      case 'ECHEC': return $localize`:@@docgen.docstatus.failed:Échec`;
      default: return $localize`:@@docgen.docstatus.reused:Réutilisable`;
    }
  }

  docStatusTone(status: DossierDocStatus): StatusTone {
    switch (status) {
      case 'GENERE': return 'success';
      case 'ECHEC': return 'danger';
      case 'EN_GENERATION': return 'accent';
      default: return 'neutral';
    }
  }

  dossierStatusLabel(status: DossierStatus): string {
    switch (status) {
      case 'GENERATION_EN_COURS': return $localize`:@@docgen.status.inprogress:Génération en cours`;
      case 'GENERE': return $localize`:@@docgen.status.generated:Généré — à valider`;
      default: return $localize`:@@docgen.status.finalized:Finalisé`;
    }
  }

  dossierStatusTone(status: DossierStatus): StatusTone {
    switch (status) {
      case 'FINALISE': return 'success';
      case 'GENERE': return 'accent';
      default: return 'warn';
    }
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@docgen.err.backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@docgen.err.invalid:Requête invalide (norme + organisation requises).`;
      case 403: return $localize`:@@docgen.err.forbidden:Droits insuffisants pour cette action.`;
      case 404: return $localize`:@@docgen.err.notfound:Norme ou dossier introuvable.`;
      case 409: return $localize`:@@docgen.err.conflict:Action impossible dans l'état actuel du dossier.`;
      case 422: return $localize`:@@docgen.err.unprocessable:Génération IA impossible pour cette requête.`;
      case 429: return $localize`:@@docgen.err.quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 503: return $localize`:@@docgen.err.unavailable:Service IA momentanément indisponible.`;
      default: return $localize`:@@docgen.err.generic:Échec de l'opération (HTTP ${err.status}:status:).`;
    }
  }
}
