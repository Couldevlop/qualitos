import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { AuditsService } from '../../audits.service';
import { AuditConclusion, AuditPlanResponse, AuditReport } from '../../audits.types';

/**
 * Section « Rapport » d'un audit (§4.4, ISO 19011 §6.5).
 *
 * Un brouillon qui se retouche — synthèse, points forts, axes d'amélioration,
 * conclusion — puis une émission qui le fige. La synthèse est celle que la
 * génération par IA écrivait déjà : le bouton IA vit donc ici, et remplit le
 * même champ que l'utilisateur relit et corrige avant d'émettre.
 *
 * L'émission n'est proposée que sur un audit clôturé, avec une synthèse et une
 * conclusion ; quand elle ne l'est pas, la raison est écrite sous le bouton.
 */
@Component({
  selector: 'qos-audit-report-panel',
  templateUrl: './audit-report-panel.component.html',
  styleUrls: ['./audit-report-panel.component.scss'],
  standalone: false
})
export class AuditReportPanelComponent implements OnChanges {

  @Input({ required: true }) plan!: AuditPlanResponse;

  /** Annonce l'état du rapport : un rapport émis verrouille aussi les documents. */
  @Output() readonly issuedChange = new EventEmitter<boolean>();

  report: AuditReport | null = null;
  loading = false;
  loadError: string | null = null;
  saving = false;
  generating = false;

  readonly form = this.fb.group({
    summary: ['', [Validators.maxLength(20000)]],
    strengths: ['', [Validators.maxLength(10000)]],
    improvements: ['', [Validators.maxLength(10000)]],
    conclusion: [null as AuditConclusion | null]
  });

  private loadedFor: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    // Relire le rapport à chaque rechargement du plan : la clôture ou un constat
    // de plus changent les chiffres clés et ce qui est permis. Mais pas si rien
    // n'a bougé, pour ne pas écraser une saisie en cours.
    if (!changes['plan'] || !this.plan) return;
    const key = this.plan.id + '|' + this.plan.updatedAt + '|' + (this.plan.findings?.length ?? 0);
    if (key === this.loadedFor) return;
    this.loadedFor = key;
    this.load();
  }

  get issued(): boolean {
    return this.report?.status === 'ISSUED';
  }

  get editable(): boolean {
    return !this.issued && this.plan.status !== 'CANCELLED';
  }

  /** Libellés de conclusion : la langue d'un audit de certification n'est pas celle d'un audit interne. */
  get conclusions(): { value: AuditConclusion; label: string }[] {
    const certification = this.plan.type === 'CERTIFICATION' || this.plan.type === 'SURVEILLANCE';
    return certification
      ? [
          { value: 'FAVORABLE', label: $localize`:@@audits.report.conclusion.cert-favorable:Certification recommandée` },
          { value: 'FAVORABLE_WITH_RESERVATIONS', label: $localize`:@@audits.report.conclusion.cert-reservations:Certification recommandée avec réserves` },
          { value: 'UNFAVORABLE', label: $localize`:@@audits.report.conclusion.cert-unfavorable:Certification non recommandée` }
        ]
      : [
          { value: 'FAVORABLE', label: $localize`:@@audits.report.conclusion.favorable:Favorable — système conforme` },
          { value: 'FAVORABLE_WITH_RESERVATIONS', label: $localize`:@@audits.report.conclusion.reservations:Favorable avec réserves` },
          { value: 'UNFAVORABLE', label: $localize`:@@audits.report.conclusion.unfavorable:Défavorable — système non conforme` }
        ];
  }

  conclusionLabel(value: AuditConclusion | null | undefined): string {
    return this.conclusions.find(c => c.value === value)?.label ?? '—';
  }

  /**
   * Pourquoi l'émission est barrée, s'il faut le dire. Un bouton désactivé sans
   * raison laisse l'utilisateur chercher — c'est exactement ce qui s'était passé
   * avec l'ajout de constats.
   */
  get issueBlocker(): string | null {
    if (this.issued || !this.report) return null;
    if (this.plan.status === 'CANCELLED') {
      return $localize`:@@audits.report.blocker-cancelled:Un audit annulé n'a pas de rapport.`;
    }
    if (this.plan.status !== 'COMPLETED') {
      return $localize`:@@audits.report.blocker-not-completed:Le rapport s'émet une fois l'audit clôturé. Vous pouvez déjà rédiger le brouillon.`;
    }
    const v = this.form.getRawValue();
    if (!v.summary?.trim()) {
      return $localize`:@@audits.report.blocker-summary:Rédigez la synthèse (ou générez-la) avant d'émettre.`;
    }
    if (!v.conclusion) {
      return $localize`:@@audits.report.blocker-conclusion:Choisissez la conclusion avant d'émettre.`;
    }
    return null;
  }

  saveDraft(): void {
    if (!this.editable || this.saving || this.form.invalid) return;
    this.saving = true;
    this.persistDraft().pipe(finalize(() => (this.saving = false))).subscribe({
      next: report => {
        this.apply(report);
        this.snack.open($localize`:@@audits.report.saved:Brouillon du rapport enregistré.`,
          $localize`:@@common.ok:OK`, { duration: 2500 });
      },
      error: err => this.fail(err, $localize`:@@audits.report.save-error:Le brouillon n'a pas pu être enregistré.`)
    });
  }

  /** Génère la synthèse par IA et la dépose dans le brouillon, à relire avant d'émettre. */
  generateSummary(): void {
    if (!this.editable || this.generating) return;
    this.generating = true;
    this.audits.generateReport(this.plan.id)
      .pipe(finalize(() => (this.generating = false)))
      .subscribe({
        next: plan => {
          this.form.controls.summary.setValue(plan.reportSummary ?? '');
          this.form.markAsDirty();
          this.snack.open($localize`:@@audits.report.generated:Synthèse générée : relisez-la avant d'émettre.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
        },
        error: err => this.fail(err,
          $localize`:@@audits.detail.report-error:Génération du rapport indisponible (service IA).`)
      });
  }

  issue(): void {
    if (this.issueBlocker || this.issued || this.saving) return;
    const data: ConfirmDialogData = {
      title: $localize`:@@audits.report.issue-confirm-title:Émettre le rapport ?`,
      message: $localize`:@@audits.report.issue-confirm-message:Le rapport sera figé — texte, conclusion et chiffres clés — et l'émission inscrite au journal d'audit. Il ne pourra plus être modifié, ni les documents de l'audit.`,
      confirmLabel: $localize`:@@audits.report.issue:Émettre le rapport`
    };
    this.dialog.open(ConfirmDialogComponent, { data, autoFocus: false, restoreFocus: true })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.saving = true;
        // Le brouillon d'abord : on émet ce qui est à l'écran, pas la dernière
        // version enregistrée.
        this.persistDraft()
          .pipe(
            switchMap(() => this.audits.issueReport(this.plan.id)),
            finalize(() => (this.saving = false)))
          .subscribe({
            next: report => {
              this.apply(report);
              this.snack.open($localize`:@@audits.report.issued-msg:Rapport émis.`,
                $localize`:@@common.ok:OK`, { duration: 2500 });
            },
            error: err => this.isStepUpRequired(err)
              ? this.askForSecondFactor()
              : this.fail(err, $localize`:@@audits.report.issue-error:Le rapport n'a pas pu être émis.`)
          });
      });
  }

  // ---- interne ---------------------------------------------------------------

  private load(): void {
    this.loading = true;
    this.loadError = null;
    this.audits.getReport(this.plan.id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        // Une saisie en cours n'est pas écrasée par un rechargement du plan
        // (un constat ajouté entre-temps) : seuls l'état et les chiffres suivent.
        next: report => this.apply(report, this.form.dirty && report.status !== 'ISSUED'),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audit-report] load failed', err?.status);
          this.loadError = safeErrorMessage(err,
            $localize`:@@audits.report.load-error:Le rapport n'a pas pu être chargé.`);
        }
      });
  }

  private persistDraft(): Observable<AuditReport> {
    if (!this.editable || !this.form.dirty) {
      return of(this.report as AuditReport);
    }
    const v = this.form.getRawValue();
    return this.audits.saveReportDraft(this.plan.id, {
      summary: v.summary?.trim() || null,
      strengths: v.strengths?.trim() || null,
      improvements: v.improvements?.trim() || null,
      conclusion: v.conclusion ?? null
    });
  }

  private apply(report: AuditReport, keepEdits = false): void {
    this.report = report;
    this.issuedChange.emit(report.status === 'ISSUED');
    if (keepEdits) return;
    this.form.reset({
      summary: report.summary ?? '',
      strengths: report.strengths ?? '',
      improvements: report.improvements ?? '',
      conclusion: report.conclusion ?? null
    });
    if (this.editable) {
      this.form.enable({ emitEvent: false });
    } else {
      this.form.disable({ emitEvent: false });
    }
  }

  private isStepUpRequired(err: unknown): boolean {
    const problem = err as { status?: number; error?: { type?: string } };
    return problem?.status === 403
      && problem?.error?.type === 'https://qualitos.io/errors/step-up-required';
  }

  /** L'émission exige un second facteur : on redemande le palier, sans déconnecter. */
  private askForSecondFactor(): void {
    const ref = this.snack.open(
      $localize`:@@audits.report.step-up:L'émission du rapport exige votre code à usage unique.`,
      $localize`:@@stepup.reauthenticate:Se réauthentifier`,
      { duration: 10000 });
    ref.onAction().subscribe(() => {
      if (!this.auth.stepUp(`/audits/${this.plan.id}`)) {
        this.snack.open($localize`:@@stepup.unavailable:Second facteur indisponible sur cet environnement.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  private fail(err: unknown, fallback: string): void {
    const status = (err as { status?: number })?.status;
    // eslint-disable-next-line no-console
    console.warn('[audit-report] failed', status);
    // 409 / 422 ont ici un sens précis, que le message générique masquerait.
    const message = status === 409
      ? $localize`:@@audits.report.conflict:Le rapport ne peut plus être modifié dans l'état actuel de l'audit — rechargez pour voir son état.`
      : status === 422
        ? $localize`:@@audits.report.incomplete:Le rapport est incomplet : synthèse et conclusion sont requises.`
        : safeErrorMessage(err, fallback);
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 5000 });
  }
}
