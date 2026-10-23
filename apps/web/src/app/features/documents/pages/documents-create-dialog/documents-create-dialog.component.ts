import { HttpEventType } from '@angular/common/http';
import { Component, Inject, OnInit, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { catchError, filter, finalize, map, switchMap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FolderRow } from '../../documents.folders';
import { DOCUMENT_TYPE_LABELS, DOCUMENT_TYPES, formatBytes } from '../../documents.labels';
import { DocumentsService } from '../../documents.service';
import { DocumentProcess, DocumentResponse, DocumentType, UploadPolicy } from '../../documents.types';
import { extensionOf } from '../documents-import-dialog/documents-import-dialog.component';

export interface CreateDialogData {
  folderId?: string | null;
  folders?: FolderRow[];
  processes?: DocumentProcess[];
}

@Component({
  selector: 'qos-documents-create-dialog',
  templateUrl: './documents-create-dialog.component.html',
  styleUrls: ['./documents-create-dialog.component.scss'],
  standalone: false
})
export class DocumentsCreateDialogComponent implements OnInit {

  submitting = false;
  /** Progression de l'envoi du fichier joint, en pour cent. */
  progress: number | null = null;

  readonly types = DOCUMENT_TYPES.map(value => ({ value, label: DOCUMENT_TYPE_LABELS[value] }));
  readonly folders: FolderRow[];
  readonly processes: DocumentProcess[];

  file: File | null = null;
  fileError: string | null = null;
  /** Créer a été tenté sans fichier : le champ le réclame. */
  fileAsked = false;
  private policy: UploadPolicy | null = null;

  readonly form = this.fb.nonNullable.group({
    code:  ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Z0-9._\-]+$/)]],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    type: ['PROCEDURE' as DocumentType, [Validators.required]],
    folderId: [null as string | null],
    processId: [null as string | null],
    ownerId: [null as string | null, [Validators.required]],
    mandatoryRead: [false],
    initialContent: [''],
    initialChangeNote: ['Création initiale']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DocumentsCreateDialogComponent, DocumentResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) data: CreateDialogData | null
  ) {
    this.folders = data?.folders ?? [];
    this.processes = data?.processes ?? [];
    this.form.patchValue({ folderId: data?.folderId ?? null });
  }

  ngOnInit(): void {
    // Le propriétaire par défaut est celui qui crée ; il peut le confier à un autre.
    const me = this.auth.snapshot()?.userId;
    if (me) this.form.patchValue({ ownerId: me });
    this.svc.uploadPolicy().pipe(catchError(() => of(null))).subscribe(p => {
      this.policy = p;
      if (this.file) this.checkFile(this.file);
    });
  }

  get accept(): string {
    return this.policy?.allowedExtensions.map(e => '.' + e).join(',') ?? '';
  }

  size(bytes: number): string {
    return formatBytes(bytes);
  }

  onPick(input: HTMLInputElement): void {
    const f = input.files?.[0] ?? null;
    input.value = '';
    if (f) this.checkFile(f);
  }

  /** Le fichier est obligatoire : un document se valide sur pièce (ADR 0099). */
  get fileMissing(): boolean {
    return this.fileAsked && !this.file;
  }

  clearFile(): void {
    this.file = null;
    this.fileError = null;
  }

  private checkFile(f: File): void {
    this.file = f;
    this.fileError = null;
    if (f.size === 0) {
      this.fileError = $localize`:@@documents.import.empty-file:Ce fichier est vide.`;
    } else if (this.policy && f.size > this.policy.maxFileMb * 1024 * 1024) {
      this.fileError = $localize`:@@documents.import.too-large:Fichier trop volumineux (maximum ${this.policy.maxFileMb}:max: Mo).`;
    } else if (this.policy && !this.policy.allowedExtensions.includes(extensionOf(f.name) ?? '')) {
      this.fileError = $localize`:@@documents.import.bad-type:Format non accepté.`;
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting || this.fileError || !this.file) {
      this.form.markAllAsTouched();
      this.fileAsked = true;
      return;
    }
    const v = this.form.getRawValue();
    if (!v.ownerId) return;
    this.submitting = true;
    const file: File = this.file;
    this.svc.create({
      code: v.code.trim(),
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      type: v.type,
      ownerId: v.ownerId,
      mandatoryRead: v.mandatoryRead,
      folderId: v.folderId,
      processId: v.processId,
      initialContent:    v.initialContent?.trim() || undefined,
      initialChangeNote: v.initialChangeNote?.trim() || undefined
    }).pipe(
      switchMap(doc => {
        const version = [...doc.versions].sort((a, b) => b.versionNumber - a.versionNumber)[0];
        this.progress = 0;
        return this.svc.uploadFile(doc.id, version.id, file).pipe(
          map(event => {
            if (event.type === HttpEventType.UploadProgress && event.total) {
              this.progress = Math.round(100 * event.loaded / event.total);
            }
            return event.type === HttpEventType.Response ? { doc, fileFailed: null as unknown } : null;
          }),
          filter((r): r is { doc: DocumentResponse; fileFailed: unknown } => r !== null),
          // Le document existe : un échec du fichier ne l'annule pas, on le dit.
          catchError(err => of({ doc, fileFailed: err as unknown }))
        );
      }),
      finalize(() => { this.submitting = false; this.progress = null; })
    ).subscribe({
      next: ({ doc, fileFailed }) => {
        const msg = fileFailed
          ? safeErrorMessage(fileFailed,
              $localize`:@@documents.import.upload-failed:Document créé, mais le fichier n'a pas été envoyé.`)
          : $localize`:@@documents.create.created:Document créé.`;
        this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: fileFailed ? 6000 : 2500 });
        this.dialogRef.close(doc);
      },
      error: err => {
        this.snack.open(safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
