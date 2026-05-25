import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AlignmentReport, AuditBlancReport, DossierResponse,
  RoadmapStageResponse, RoadmapSummary, StageStatus, StandardDetail
} from '../../standards.types';

@Component({
  selector: 'qos-standards-detail',
  templateUrl: './standards-detail.component.html',
  styleUrls: ['./standards-detail.component.scss'],
  standalone: false
})
export class StandardsDetailComponent implements OnInit {

  adoptionId!: string;
  loading = true;
  error?: string;

  adoption?: AdoptionResponse;
  standard?: StandardDetail;
  alignment?: AlignmentReport;
  roadmap?: RoadmapSummary;
  audit?: AuditBlancReport;

  dossier?: DossierResponse;
  generatingDossier = false;

  readonly stageStatuses: StageStatus[] = ['NOT_STARTED', 'IN_PROGRESS', 'DONE', 'SKIPPED'];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly svc: StandardsService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.adoptionId = this.route.snapshot.paramMap.get('id')!;
    this.reloadAll();
  }

  private reloadAll(): void {
    this.loading = true;
    this.svc.getAdoption(this.adoptionId).subscribe({
      next: a => {
        this.adoption = a;
        this.svc.getStandardDetail(a.standardId).subscribe(s => this.standard = s);
        this.loadReports();
        this.loading = false;
      },
      error: () => { this.error = "Impossible de charger l'adoption."; this.loading = false; }
    });
  }

  private loadReports(): void {
    this.svc.getAlignment(this.adoptionId).subscribe(r => this.alignment = r);
    this.svc.getRoadmap(this.adoptionId).subscribe(r => this.roadmap = r);
    this.svc.getAuditBlanc(this.adoptionId).subscribe(r => this.audit = r);
  }

  changeStageStatus(stage: RoadmapStageResponse, status: StageStatus): void {
    if (stage.status === status) return;
    this.svc.updateStage(this.adoptionId, stage.id, { status }).subscribe({
      next: () => {
        this.snack.open(`Étape ${stage.stepNumber} → ${status}`, 'OK', { duration: 2000 });
        this.svc.getRoadmap(this.adoptionId).subscribe(r => this.roadmap = r);
      },
      error: () => this.snack.open('Échec de la mise à jour', 'Fermer', { duration: 3000 })
    });
  }

  generateDossier(): void {
    this.generatingDossier = true;
    this.svc.generateDossier(this.adoptionId).subscribe({
      next: d => {
        this.dossier = d;
        this.generatingDossier = false;
        this.snack.open('Dossier généré et ancré (SHA-256)', 'OK', { duration: 2500 });
      },
      error: () => {
        this.generatingDossier = false;
        this.snack.open('Échec de la génération du dossier', 'Fermer', { duration: 3000 });
      }
    });
  }

  downloadDossier(): void {
    if (!this.dossier) return;
    const blob = new Blob([this.dossier.htmlContent], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = this.dossier.fileName;
    a.click();
    URL.revokeObjectURL(url);
  }

  severityClass(sev: string): string {
    return sev === 'CRITICAL' ? 'sev-crit' : sev === 'MAJOR' ? 'sev-maj' : 'sev-min';
  }

  scoreClass(score: number): string {
    return score >= 80 ? 'ok' : score >= 50 ? 'warn' : 'bad';
  }
}
