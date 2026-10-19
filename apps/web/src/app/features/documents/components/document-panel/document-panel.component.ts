import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { DocumentsService } from '../../documents.service';
import {
  DISPLAY_STATUS_LABELS, DOCUMENT_TYPE_LABELS, VERSION_STATUS_LABELS, displayStatusOf, formatBytes
} from '../../documents.labels';
import {
  DisplayStatus, DocumentProcess, DocumentResponse, DocumentVersionResponse, VersionStatus
} from '../../documents.types';
import { FolderRow } from '../../documents.folders';

/** Au-delà, l'aperçu cède la place au téléchargement : on ne charge pas 50 Mo pour un coup d'œil. */
export const MAX_PREVIEW_BYTES = 15 * 1024 * 1024;

export type PreviewKind = 'pdf' | 'image' | 'text';
type PreviewState = 'none' | 'loading' | 'ready' | 'failed' | 'too-large' | 'purged';

export interface CircuitStepView {
  label: string;
  state: 'done' | 'current' | 'todo';
}

/** Les quatre temps d'une version, de la rédaction à la diffusion. */
const STEP_LABELS = [
  $localize`:@@documents.panel.step-draft:Rédaction`,
  $localize`:@@documents.panel.step-review:Vérification`,
  $localize`:@@documents.panel.step-approval:Approbation`,
  $localize`:@@documents.panel.step-publish:Diffusion`
];

const STEP_OF: Record<VersionStatus, number> = {
  DRAFT: 0, IN_REVIEW: 1, APPROVED: 2, PUBLISHED: 4, OBSOLETE: 4
};

/**
 * La fiche d'un document dans la GED : l'aperçu de son fichier, où en est sa
 * dernière version, ses métadonnées et l'historique des versions.
 *
 * <p>L'aperçu lit le fichier par un lien présigné et l'affiche depuis la
 * mémoire du navigateur ; seuls le PDF, les images et le texte s'y prêtent.
 */
@Component({
  selector: 'qos-document-panel',
  templateUrl: './document-panel.component.html',
  styleUrls: ['./document-panel.component.scss'],
  standalone: false
})
export class DocumentPanelComponent implements OnChanges, OnDestroy {

  @Input({ required: true }) document!: DocumentResponse;
  @Input() folders: FolderRow[] = [];
  @Input() processes: DocumentProcess[] = [];

  @Output() readonly closed = new EventEmitter<void>();
  /** Le document a changé ici : la liste se relit. */
  @Output() readonly changed = new EventEmitter<void>();

  readonly typeLabels = DOCUMENT_TYPE_LABELS;
  readonly statusLabels = DISPLAY_STATUS_LABELS;
  readonly versionLabels = VERSION_STATUS_LABELS;

  version: DocumentVersionResponse | null = null;
  history: DocumentVersionResponse[] = [];
  steps: CircuitStepView[] = [];

  preview: PreviewState = 'none';
  previewKind: PreviewKind | null = null;
  previewUrl: SafeResourceUrl | null = null;
  imageUrl: string | null = null;
  previewText: string | null = null;
  private objectUrl: string | null = null;

  acknowledgments: number | null = null;
  downloading = false;
  linkError: string | null = null;

  private previewSub?: Subscription;
  private ackSub?: Subscription;

  constructor(
    private readonly svc: DocumentsService,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['document'] || !this.document) return;
    const previous: DocumentResponse | undefined = changes['document'].previousValue;
    this.history = [...this.document.versions].sort((a, b) => b.versionNumber - a.versionNumber);
    // La version qu'on présente : celle en cours de travail si elle existe, sinon celle qui fait foi.
    this.version = this.history[0] ?? null;
    this.steps = stepsOf(this.version?.status ?? null);
    this.linkError = null;

    // Même version, même fichier, toujours présent : on garde l'aperçu au lieu de le relire.
    // Une version nouvelle, absente de l'état précédent, se relit toujours.
    const before = previous?.id === this.document.id
      ? previous.versions.find(v => v.id === this.version?.id)
      : undefined;
    const sameFile = !!before && !!this.version
      && before.file?.fileName === this.version.file?.fileName
      && before.file?.purged === this.version.file?.purged;
    if (!sameFile) this.loadPreview();
    this.loadAcknowledgments();
  }

  ngOnDestroy(): void {
    this.previewSub?.unsubscribe();
    this.ackSub?.unsubscribe();
    this.revoke();
  }

  get displayStatus(): DisplayStatus {
    return displayStatusOf(this.document);
  }

  get folderName(): string | null {
    const id = this.document.folderId;
    return id ? this.folders.find(r => r.folder.id === id)?.folder.name ?? null : null;
  }

  get processName(): string | null {
    const id = this.document.processId;
    return id ? this.processes.find(p => p.id === id)?.name ?? null : null;
  }

  size(bytes: number | null | undefined): string {
    return formatBytes(bytes);
  }

  /** Télécharge le fichier de la version présentée, sous son nom d'origine. */
  download(): void {
    if (!this.version?.file || this.downloading) return;
    this.downloading = true;
    this.linkError = null;
    this.svc.fileLink(this.document.id, this.version.id, true).subscribe({
      next: link => {
        this.downloading = false;
        const a = window.document.createElement('a');
        a.href = link.url;
        a.rel = 'noopener';
        a.click();
      },
      error: () => {
        this.downloading = false;
        this.linkError = $localize`:@@documents.panel.download-failed:Le fichier n'a pas pu être téléchargé. Réessayez.`;
      }
    });
  }

  retryPreview(): void {
    this.loadPreview();
  }

  trackByVersion(_: number, v: DocumentVersionResponse): string {
    return v.id;
  }

  private loadPreview(): void {
    this.previewSub?.unsubscribe();
    this.revoke();
    this.previewUrl = null;
    this.imageUrl = null;
    this.previewText = null;
    this.previewKind = null;

    const v = this.version;
    const file = v?.file;
    if (!v || !file) { this.preview = 'none'; return; }
    if (file.purged) { this.preview = 'purged'; return; }
    const kind = previewKindOf(file.contentType);
    if (!file.previewable || !kind) { this.preview = 'none'; return; }
    if (file.sizeBytes > MAX_PREVIEW_BYTES) { this.preview = 'too-large'; return; }

    this.preview = 'loading';
    this.previewKind = kind;
    this.previewSub = this.svc.fileLink(this.document.id, v.id, false).pipe(
      switchMap(link => this.svc.fetchFile(link.url)),
      catchError(() => of(null))
    ).subscribe(blob => {
      if (!blob) { this.preview = 'failed'; return; }
      // Le type déclaré fait foi : un aperçu n'interprète jamais le fichier autrement.
      const typed = new Blob([blob], { type: file.contentType ?? 'application/octet-stream' });
      if (kind === 'text') {
        typed.text().then(t => { this.previewText = t.slice(0, 20000); this.preview = 'ready'; },
          () => this.preview = 'failed');
        return;
      }
      this.objectUrl = URL.createObjectURL(typed);
      if (kind === 'pdf') {
        this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
      } else {
        this.imageUrl = this.objectUrl;
      }
      this.preview = 'ready';
    });
  }

  private loadAcknowledgments(): void {
    this.ackSub?.unsubscribe();
    this.acknowledgments = null;
    const current = this.document.currentVersionId;
    if (!this.document.mandatoryRead || !current) return;
    this.ackSub = this.svc.countAcknowledgments(this.document.id, current).pipe(
      catchError(() => of(null))
    ).subscribe(r => this.acknowledgments = r?.count ?? null);
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}

export function previewKindOf(contentType: string | null | undefined): PreviewKind | null {
  if (!contentType) return null;
  if (contentType === 'application/pdf') return 'pdf';
  if (contentType.startsWith('image/')) return 'image';
  if (contentType.startsWith('text/')) return 'text';
  return null;
}

export function stepsOf(status: VersionStatus | null): CircuitStepView[] {
  const reached = status ? STEP_OF[status] : -1;
  return STEP_LABELS.map((label, i) => ({
    label,
    state: i < reached ? 'done' : i === reached ? 'current' : 'todo'
  }));
}
