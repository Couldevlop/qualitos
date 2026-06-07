import {
  AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild
} from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { WorkflowDesignerService } from '../../workflow-designer.service';
import {
  CreateWorkflowRequest, UpdateWorkflowRequest, WorkflowDefinition, WorkflowStatus
} from '../../workflow-designer.types';

/**
 * Forme minimale du BpmnModeler utilisée par ce composant. On ne type pas la
 * lib entière : seule cette surface nous intéresse, ce qui rend le wrapper
 * testable avec un mock (cf. workflow-editor.component.spec.ts).
 */
export interface BpmnModelerLike {
  importXML(xml: string): Promise<{ warnings: unknown[] }>;
  saveXML(opts?: { format?: boolean }): Promise<{ xml?: string }>;
  destroy(): void;
}

/** Constructeur de modeler — injecté ou résolu par import() dynamique. */
export type BpmnModelerCtor = new (opts: { container: HTMLElement }) => BpmnModelerLike;

/**
 * Éditeur BPMN no-code (§5.4).
 *
 * <p>bpmn-js est une dépendance LOURDE : elle n'est JAMAIS importée
 * statiquement. {@link loadModelerCtor} la résout par {@code import('bpmn-js/...')}
 * dynamique, au moment de l'ouverture de l'éditeur uniquement. Le bundle initial
 * et le chunk de feature restent légers ; le code de bpmn-js part dans un chunk
 * asynchrone séparé créé par le bundler à partir du {@code import()}.</p>
 *
 * <p>Testabilité : l'init est isolée dans {@link initModeler}, qui s'appuie sur
 * {@link loadModelerCtor} — la spec override cette méthode pour injecter un
 * faux modeler sans charger la vraie lib.</p>
 */
@Component({
  selector: 'qos-workflow-editor',
  templateUrl: './workflow-editor.component.html',
  styleUrls: ['./workflow-editor.component.scss'],
  standalone: false
})
export class WorkflowEditorComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('canvas', { static: false }) canvasRef?: ElementRef<HTMLElement>;

  id: string | null = null;
  isNew = false;
  name = '';
  description = '';
  status: WorkflowStatus = 'DRAFT';
  version = 0;

  loading = true;
  saving = false;
  modelerReady = false;
  errorMessage: string | null = null;

  private modeler?: BpmnModelerLike;
  private pendingXml = '';

  constructor(
    private readonly svc: WorkflowDesignerService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');
    if (param === 'new' || param === null) {
      this.isNew = true;
      this.name = $localize`:@@workflow.editor.untitled:Nouveau workflow`;
      this.pendingXml = this.svc.emptyDiagram();
      this.loading = false;
    } else {
      this.id = param;
      this.svc.get(param).subscribe({
        next: wf => { this.applyDefinition(wf); this.loading = false; this.tryInit(); },
        error: err => {
          this.loading = false;
          this.errorMessage = safeErrorMessage(
            err, $localize`:@@workflow.editor.load-error:Workflow introuvable.`);
        }
      });
    }
  }

  ngAfterViewInit(): void {
    // Pour un nouveau workflow, le canvas est déjà dans le DOM (pas de chargement).
    this.tryInit();
  }

  ngOnDestroy(): void {
    this.modeler?.destroy();
  }

  /** Lecture seule si le workflow n'est plus en DRAFT (publié / archivé figés). */
  get readonly(): boolean {
    return !this.isNew && this.status !== 'DRAFT';
  }

  async save(): Promise<void> {
    if (this.readonly || this.saving) return;
    this.saving = true;
    try {
      const bpmnXml = await this.currentXml();
      if (this.isNew) {
        const req: CreateWorkflowRequest = { name: this.name, description: this.description, bpmnXml };
        const created = await firstValueFrom(this.svc.create(req));
        this.applyDefinition(created);
        this.isNew = false;
        this.id = created.id;
        this.router.navigate(['/workflow-designer', created.id], { replaceUrl: true });
      } else if (this.id) {
        const req: UpdateWorkflowRequest = { name: this.name, description: this.description, bpmnXml };
        const updated = await firstValueFrom(this.svc.update(this.id, req));
        this.applyDefinition(updated);
      }
      this.snackBar.open($localize`:@@workflow.editor.saved:Workflow enregistré.`, '', { duration: 2500 });
    } catch (err) {
      this.snackBar.open(
        safeErrorMessage(err, $localize`:@@workflow.editor.save-error:Enregistrement impossible.`),
        '', { duration: 3500 });
    } finally {
      this.saving = false;
    }
  }

  publish(): void {
    if (!this.id) return;
    this.svc.publish(this.id).subscribe({
      next: wf => {
        this.applyDefinition(wf);
        this.snackBar.open($localize`:@@workflow.editor.published:Workflow publié.`, '', { duration: 2500 });
      },
      error: err => this.snackBar.open(
        safeErrorMessage(err, $localize`:@@workflow.editor.publish-error:Publication impossible.`),
        '', { duration: 3500 })
    });
  }

  back(): void {
    this.router.navigate(['/workflow-designer']);
  }

  // --- bpmn-js wiring (lazy) --------------------------------------------------

  /**
   * Résout dynamiquement le constructeur BpmnModeler. SEUL point d'entrée vers
   * bpmn-js ; aucun import statique → pas de fuite dans le bundle initial.
   * Overridable en test pour injecter un faux modeler.
   */
  protected async loadModelerCtor(): Promise<BpmnModelerCtor> {
    const mod = await import('bpmn-js/lib/Modeler');
    return (mod as unknown as { default: BpmnModelerCtor }).default;
  }

  /** Initialise le modeler une fois le canvas dispo et le XML chargé. */
  private tryInit(): void {
    if (this.modelerReady || this.loading || !this.canvasRef || !this.pendingXml) return;
    void this.initModeler(this.canvasRef.nativeElement, this.pendingXml);
  }

  private async initModeler(container: HTMLElement, xml: string): Promise<void> {
    try {
      const Ctor = await this.loadModelerCtor();
      this.modeler = new Ctor({ container });
      await this.modeler.importXML(xml);
      this.modelerReady = true;
      this.pendingXml = '';
    } catch (err) {
      this.errorMessage = safeErrorMessage(
        err, $localize`:@@workflow.editor.canvas-error:Impossible de charger l'éditeur de diagramme.`);
    }
  }

  /** XML courant : depuis le modeler si prêt, sinon le XML en attente. */
  private async currentXml(): Promise<string> {
    if (this.modeler && this.modelerReady) {
      const { xml } = await this.modeler.saveXML({ format: true });
      return xml ?? this.pendingXml;
    }
    return this.pendingXml;
  }

  private applyDefinition(wf: WorkflowDefinition): void {
    this.name = wf.name;
    this.description = wf.description ?? '';
    this.status = wf.status;
    this.version = wf.version;
    if (wf.bpmnXml) this.pendingXml = wf.bpmnXml;
  }
}
