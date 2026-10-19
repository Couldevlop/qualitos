import { HttpEventType } from '@angular/common/http';
import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, concat, defer, of } from 'rxjs';
import { catchError, last, map, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FolderRow } from '../../documents.folders';
import { DOCUMENT_TYPE_LABELS, DOCUMENT_TYPES, formatBytes } from '../../documents.labels';
import { DocumentsService } from '../../documents.service';
import { DocumentProcess, DocumentResponse, DocumentType, UploadPolicy } from '../../documents.types';

export interface ImportDialogData {
  files: File[];
  folderId: string | null;
  folders: FolderRow[];
  processes: DocumentProcess[];
}

export type ImportState = 'ready' | 'invalid' | 'uploading' | 'done' | 'failed';

/** Un fichier à importer, devenu une ligne éditable. */
export interface ImportItem {
  file: File;
  code: string;
  title: string;
  type: DocumentType;
  state: ImportState;
  progress: number;
  error: string | null;
  /** Le document déjà créé : une reprise ne le recrée pas, elle rejoint le fichier. */
  created: DocumentResponse | null;
}

const CODE_PATTERN = /^[A-Z0-9._-]+$/;
const CODE_MAX = 100;
const TITLE_MAX = 255;

/**
 * Importe des fichiers dans la GED : chaque fichier devient un document en
 * brouillon dont la première version porte le fichier.
 *
 * <p>Les fichiers partent un par un, chacun avec sa progression ; une erreur
 * n'arrête pas les suivants, et une nouvelle tentative reprend là où l'on
 * s'était arrêté.
 */
@Component({
  selector: 'qos-documents-import-dialog',
  templateUrl: './documents-import-dialog.component.html',
  styleUrls: ['./documents-import-dialog.component.scss'],
  standalone: false
})
export class DocumentsImportDialogComponent implements OnInit {

  readonly types = DOCUMENT_TYPES;
  readonly typeLabels = DOCUMENT_TYPE_LABELS;

  readonly common = this.fb.nonNullable.group({
    folderId: [null as string | null],
    processId: [null as string | null],
    mandatoryRead: [false]
  });

  items: ImportItem[] = [];
  policy: UploadPolicy | null = null;
  running = false;
  dragging = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: ImportDialogData,
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<DocumentsImportDialogComponent, DocumentResponse[]>
  ) {}

  ngOnInit(): void {
    this.common.patchValue({ folderId: this.data.folderId ?? null });
    this.svc.uploadPolicy().pipe(catchError(() => of(null))).subscribe(p => {
      this.policy = p;
      this.items.forEach(i => this.validate(i));
    });
    this.addFiles(this.data.files ?? []);
  }

  // ---------- fichiers ----------

  onPick(input: HTMLInputElement): void {
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.dragging = true;
  }

  onDragLeave(e: DragEvent): void {
    e.stopPropagation();
    this.dragging = false;
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.dragging = false;
    this.addFiles(Array.from(e.dataTransfer?.files ?? []));
  }

  addFiles(files: File[]): void {
    if (this.running) return;
    const taken = new Set(this.items.map(i => i.code));
    for (const file of files) {
      const code = uniqueCode(codeFromFileName(file.name), taken);
      taken.add(code);
      const item: ImportItem = {
        file, code, title: titleFromFileName(file.name), type: guessType(file.name),
        state: 'ready', progress: 0, error: null, created: null
      };
      this.validate(item);
      this.items.push(item);
    }
  }

  remove(item: ImportItem): void {
    if (this.running || item.state === 'done') return;
    this.items = this.items.filter(i => i !== item);
  }

  /** La ligne a été corrigée : on la revérifie. */
  edited(item: ImportItem): void {
    if (item.state === 'done' || item.state === 'uploading') return;
    item.code = item.code.toUpperCase();
    this.validate(item);
  }

  size(bytes: number): string {
    return formatBytes(bytes);
  }

  get accept(): string {
    return this.policy?.allowedExtensions.map(e => '.' + e).join(',') ?? '';
  }

  get pending(): ImportItem[] {
    return this.items.filter(i => i.state === 'ready' || i.state === 'failed');
  }

  get doneCount(): number {
    return this.items.filter(i => i.state === 'done').length;
  }

  get hasInvalid(): boolean {
    return this.items.some(i => i.state === 'invalid');
  }

  get finished(): boolean {
    return this.items.length > 0 && this.items.every(i => i.state === 'done');
  }

  // ---------- envoi ----------

  start(): void {
    if (this.running || this.pending.length === 0 || this.hasInvalid) return;
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.pending.forEach(i => {
        i.state = 'failed';
        i.error = $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`;
      });
      return;
    }
    this.running = true;
    concat(...this.pending.map(item => defer(() => this.importOne(item, ownerId)))).subscribe({
      complete: () => {
        this.running = false;
        if (this.finished) this.close();
      }
    });
  }

  close(): void {
    const done = this.items.filter(i => i.state === 'done' && i.created).map(i => i.created!);
    this.dialogRef.close(done);
  }

  private importOne(item: ImportItem, ownerId: string): Observable<void> {
    const c = this.common.getRawValue();
    item.state = 'uploading';
    item.progress = 0;
    item.error = null;

    const created$: Observable<DocumentResponse> = item.created
      ? of(item.created)
      : this.svc.create({
          code: item.code.trim(), title: item.title.trim(), type: item.type, ownerId,
          mandatoryRead: c.mandatoryRead, folderId: c.folderId, processId: c.processId
        }).pipe(tap(d => item.created = d));

    return created$.pipe(
      switchMap(doc => {
        const version = [...doc.versions].sort((a, b) => b.versionNumber - a.versionNumber)[0];
        return this.svc.uploadFile(doc.id, version.id, item.file);
      }),
      tap(event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          item.progress = Math.round(100 * event.loaded / event.total);
        }
      }),
      last(),
      map(() => {
        item.state = 'done';
        item.progress = 100;
      }),
      catchError(err => {
        item.state = 'failed';
        item.error = item.created
          ? safeErrorMessage(err, $localize`:@@documents.import.upload-failed:Document créé, mais le fichier n'a pas été envoyé.`)
          : safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`);
        return of(undefined);
      })
    );
  }

  private validate(item: ImportItem): void {
    item.error = problemOf(item, this.policy);
    // Un document déjà créé attend encore son fichier : la ligne reste à reprendre.
    item.state = item.error ? 'invalid' : item.created ? 'failed' : 'ready';
  }

  trackByItem(_: number, i: ImportItem): File {
    return i.file;
  }
}

/** Ce qui empêche d'importer la ligne, ou null. */
export function problemOf(item: ImportItem, policy: UploadPolicy | null): string | null {
  if (item.file.size === 0) {
    return $localize`:@@documents.import.empty-file:Ce fichier est vide.`;
  }
  if (policy) {
    if (item.file.size > policy.maxFileMb * 1024 * 1024) {
      return $localize`:@@documents.import.too-large:Fichier trop volumineux (maximum ${policy.maxFileMb}:max: Mo).`;
    }
    const ext = extensionOf(item.file.name);
    if (!ext || !policy.allowedExtensions.includes(ext)) {
      return $localize`:@@documents.import.bad-type:Format non accepté.`;
    }
  }
  if (!item.created) {
    if (!item.code || item.code.length > CODE_MAX || !CODE_PATTERN.test(item.code)) {
      return $localize`:@@documents.import.bad-code:Code invalide : lettres majuscules, chiffres, point, tiret, souligné.`;
    }
    if (!item.title.trim() || item.title.length > TITLE_MAX) {
      return $localize`:@@documents.import.bad-title:Le titre est obligatoire.`;
    }
  }
  return null;
}

export function extensionOf(name: string): string | null {
  const dot = name.lastIndexOf('.');
  return dot > 0 && dot < name.length - 1 ? name.slice(dot + 1).toLowerCase() : null;
}

function stemOf(name: string): string {
  const dot = name.lastIndexOf('.');
  return dot > 0 ? name.slice(0, dot) : name;
}

/** « Procédure achats v2.pdf » donne « PROCEDURE-ACHATS-V2 ». */
export function codeFromFileName(name: string): string {
  const code = stemOf(name)
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9._-]+/g, '-')
    .replace(/-{2,}/g, '-')
    .replace(/^[-.]+|[-.]+$/g, '')
    .slice(0, CODE_MAX - 4);
  return code || 'DOC';
}

export function titleFromFileName(name: string): string {
  const title = stemOf(name).replace(/[_]+/g, ' ').replace(/\s{2,}/g, ' ').trim();
  return (title || name).slice(0, TITLE_MAX);
}

export function uniqueCode(code: string, taken: Set<string>): string {
  if (!taken.has(code)) return code;
  let n = 2;
  while (taken.has(`${code}-${n}`)) n++;
  return `${code}-${n}`;
}

/** Un type par défaut, deviné du nom ; l'utilisateur le corrige au besoin. */
export function guessType(name: string): DocumentType {
  const n = name.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  if (/(^|[^a-z])(pol|politique|policy)([^a-z]|$)/.test(n)) return 'POLICY';
  if (/(^|[^a-z])(mq|manuel|manual)([^a-z]|$)/.test(n)) return 'MANUAL';
  if (/(^|[^a-z])(mo|it|instruction|mode operatoire)([^a-z]|$)/.test(n)) return 'WORK_INSTRUCTION';
  if (/(^|[^a-z])(frm|form|formulaire)([^a-z]|$)/.test(n)) return 'FORM';
  if (/(^|[^a-z])(enr|enregistrement|record)([^a-z]|$)/.test(n)) return 'RECORD';
  if (/(^|[^a-z])(pr|proc|procedure)([^a-z]|$)/.test(n)) return 'PROCEDURE';
  return 'OTHER';
}
