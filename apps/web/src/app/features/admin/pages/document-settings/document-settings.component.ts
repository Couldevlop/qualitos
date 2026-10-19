import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FolderRow, buildFolderRows } from '../../../documents/documents.folders';
import { formatBytes } from '../../../documents/documents.labels';
import { DocumentsService } from '../../../documents/documents.service';
import {
  DocumentFolder, DocumentProcess, DocumentSettings, StorageUsage
} from '../../../documents/documents.types';

/** Profondeur maximale de l'arborescence, comme côté serveur. */
export const MAX_FOLDER_DEPTH = 4;
const NAME_MAX = 120;

/** Une ligne en cours de saisie : un nouveau dossier (sous un parent) ou un renommage. */
interface Draft {
  kind: 'folder' | 'process';
  /** L'élément renommé, ou null pour une création. */
  id: string | null;
  parentId: string | null;
  name: string;
}

/**
 * « Documentation » (ADR 0094) — réservé à l'administrateur du client.
 *
 * <p>La périodicité des revues, les limites des fichiers et leur conservation,
 * l'arborescence et la liste des processus de la GED, et ce que le stockage
 * occupe réellement une fois les doublons et la compression déduits.
 */
@Component({
  selector: 'qos-document-settings',
  templateUrl: './document-settings.component.html',
  styleUrls: ['./document-settings.component.scss'],
  standalone: false
})
export class DocumentSettingsComponent implements OnInit {

  readonly nameMax = NAME_MAX;
  readonly maxDepth = MAX_FOLDER_DEPTH;

  settings: DocumentSettings | null = null;
  form = {
    reviewPeriodMonths: 24,
    reviewWarningDays: 30,
    maxFileMb: 25,
    quotaMb: null as number | null,
    obsoleteRetentionYears: null as number | null
  };
  usage: StorageUsage | null = null;
  folderRows: FolderRow[] = [];
  processes: DocumentProcess[] = [];
  draft: Draft | null = null;

  loading = false;
  failed = false;
  saving = false;
  busy = false;
  private saved = '';

  constructor(
    private readonly svc: DocumentsService,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.failed = false;
    forkJoin({
      settings: this.svc.settings(),
      usage: this.svc.storage(),
      folders: this.svc.folders(),
      processes: this.svc.processes()
    }).pipe(finalize(() => this.loading = false)).subscribe({
      next: r => {
        this.applySettings(r.settings);
        this.usage = r.usage;
        this.folderRows = buildFolderRows(r.folders);
        this.processes = r.processes;
      },
      error: () => this.failed = true
    });
  }

  // ---------- réglages ----------

  get dirty(): boolean {
    return JSON.stringify(this.form) !== this.saved;
  }

  /** Ce qui empêche d'enregistrer, ou null. */
  get problem(): string | null {
    const f = this.form;
    const max = this.settings?.platformMaxFileMb ?? 50;
    if (!isInt(f.reviewPeriodMonths, 1, 120)) {
      return $localize`:@@documents.settings.bad-period:La périodicité va de 1 à 120 mois.`;
    }
    if (!isInt(f.reviewWarningDays, 0, 365)) {
      return $localize`:@@documents.settings.bad-warning:Le préavis va de 0 à 365 jours.`;
    }
    if (!isInt(f.maxFileMb, 1, max)) {
      return $localize`:@@documents.settings.bad-max-file:La taille maximale va de 1 à ${max}:max: Mo.`;
    }
    if (f.quotaMb !== null && !isInt(f.quotaMb, 1, 10_000_000)) {
      return $localize`:@@documents.settings.bad-quota:Le quota est un nombre entier de Mo, ou vide pour aucun.`;
    }
    if (f.obsoleteRetentionYears !== null && !isInt(f.obsoleteRetentionYears, 1, 100)) {
      return $localize`:@@documents.settings.bad-retention:La conservation va de 1 à 100 ans, ou vide pour tout garder.`;
    }
    return null;
  }

  save(): void {
    if (this.saving || this.problem || !this.dirty) return;
    this.saving = true;
    this.svc.saveSettings({ ...this.form }).pipe(finalize(() => this.saving = false)).subscribe({
      next: s => {
        this.applySettings(s);
        this.snack.open($localize`:@@documents.settings.saved:Paramètres enregistrés.`, $localize`:@@common.ok:OK`, { duration: 2500 });
        this.svc.storage().subscribe({ next: u => this.usage = u, error: () => undefined });
      },
      error: err => this.fail(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`)
    });
  }

  reset(): void {
    if (this.settings) this.applySettings(this.settings);
  }

  /** Un champ facultatif vidé vaut « aucun ». */
  optional(value: number | string | null): number | null {
    return value === null || value === '' ? null : Number(value);
  }

  // ---------- stockage ----------

  size(bytes: number | null | undefined): string {
    return formatBytes(bytes);
  }

  /** La part du quota occupée, en pour cent, ou null sans quota. */
  get quotaPercent(): number | null {
    const u = this.usage;
    if (!u?.quotaBytes) return null;
    return Math.min(100, Math.round(100 * u.storedBytes / u.quotaBytes));
  }

  /** L'économie due aux doublons et à la compression, en pour cent de ce qui aurait été stocké. */
  get savedPercent(): number {
    const u = this.usage;
    if (!u || u.logicalBytes === 0) return 0;
    return Math.max(0, Math.round(100 * (1 - u.storedBytes / u.logicalBytes)));
  }

  // ---------- arborescence et processus ----------

  startFolder(parent: FolderRow | null): void {
    this.draft = { kind: 'folder', id: null, parentId: parent?.folder.id ?? null, name: '' };
  }

  renameFolder(row: FolderRow): void {
    this.draft = { kind: 'folder', id: row.folder.id, parentId: row.folder.parentId, name: row.folder.name };
  }

  startProcess(): void {
    this.draft = { kind: 'process', id: null, parentId: null, name: '' };
  }

  renameProcess(p: DocumentProcess): void {
    this.draft = { kind: 'process', id: p.id, parentId: null, name: p.name };
  }

  cancelDraft(): void {
    this.draft = null;
  }

  isDraft(kind: 'folder' | 'process', id: string | null, parentId: string | null = null): boolean {
    const d = this.draft;
    return !!d && d.kind === kind && d.id === id && (id !== null || d.parentId === parentId);
  }

  commitDraft(): void {
    const d = this.draft;
    if (!d || this.busy) return;
    const name = d.name.trim();
    if (!name || name.length > NAME_MAX) return;
    let call: Observable<unknown>;
    if (d.kind === 'folder') {
      const position = d.id
        ? this.folderRows.find(r => r.folder.id === d.id)?.folder.position ?? null
        : this.folderRows.filter(r => r.folder.parentId === d.parentId).length;
      const body = { name, parentId: d.parentId, position };
      call = d.id ? this.svc.updateFolder(d.id, body) : this.svc.createFolder(body);
    } else {
      const position = d.id ? this.processes.find(p => p.id === d.id)?.position ?? null : this.processes.length;
      call = d.id ? this.svc.updateProcess(d.id, { name, position }) : this.svc.createProcess({ name, position });
    }
    this.busy = true;
    call.pipe(finalize(() => this.busy = false)).subscribe({
      next: () => {
        this.draft = null;
        this.reloadLists();
      },
      error: err => this.fail(err, $localize`:@@common.error-create:Erreur lors de la création.`)
    });
  }

  deleteFolder(row: FolderRow): void {
    this.confirm(
      $localize`:@@documents.settings.delete-folder:Supprimer le dossier « ${row.folder.name}:name: » ?`,
      $localize`:@@documents.settings.delete-folder-help:Un dossier qui contient des sous-dossiers ou des documents ne peut pas être supprimé.`,
      () => this.svc.deleteFolder(row.folder.id));
  }

  deleteProcess(p: DocumentProcess): void {
    this.confirm(
      $localize`:@@documents.settings.delete-process:Supprimer le processus « ${p.name}:name: » ?`,
      $localize`:@@documents.settings.delete-process-help:Un processus rattaché à des documents ne peut pas être supprimé.`,
      () => this.svc.deleteProcess(p.id));
  }

  /** On peut créer un sous-dossier tant que la profondeur maximale n'est pas atteinte. */
  canNest(row: FolderRow): boolean {
    return row.depth + 1 < MAX_FOLDER_DEPTH;
  }

  trackByFolder(_: number, r: FolderRow): string {
    return r.folder.id;
  }

  trackByProcess(_: number, p: DocumentProcess): string {
    return p.id;
  }

  private confirm(title: string, message: string, action: () => Observable<void>): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title, message,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.busy = true;
      action().pipe(finalize(() => this.busy = false)).subscribe({
        next: () => this.reloadLists(),
        error: err => this.fail(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`)
      });
    });
  }

  private reloadLists(): void {
    forkJoin({ folders: this.svc.folders(), processes: this.svc.processes() }).subscribe({
      next: r => {
        this.folderRows = buildFolderRows(r.folders as DocumentFolder[]);
        this.processes = r.processes;
      },
      error: err => this.fail(err, $localize`:@@common.error-loading:Erreur lors du chargement.`)
    });
  }

  private applySettings(s: DocumentSettings): void {
    this.settings = s;
    this.form = {
      reviewPeriodMonths: s.reviewPeriodMonths,
      reviewWarningDays: s.reviewWarningDays,
      maxFileMb: s.maxFileMb,
      quotaMb: s.quotaMb,
      obsoleteRetentionYears: s.obsoleteRetentionYears
    };
    this.saved = JSON.stringify(this.form);
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4500 });
  }
}

function isInt(v: number | null, min: number, max: number): boolean {
  return v !== null && Number.isInteger(Number(v)) && Number(v) >= min && Number(v) <= max;
}
