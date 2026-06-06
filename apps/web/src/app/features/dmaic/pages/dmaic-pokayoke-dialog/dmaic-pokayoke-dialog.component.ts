import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import {
  AssignmentResponse,
  DeviceSummary,
  PokaYokeMechanism,
  PokaYokeType
} from '../../dmaic.types';

export interface DmaicPokaYokeDialogData {
  projectId: string;
}

@Component({
  selector: 'qos-dmaic-pokayoke-dialog',
  templateUrl: './dmaic-pokayoke-dialog.component.html',
  styleUrls: ['./dmaic-pokayoke-dialog.component.scss'],
  standalone: false
})
export class DmaicPokaYokeDialogComponent implements OnInit {

  submitting = false;
  loading = true;
  loadError: string | null = null;

  readonly types: PokaYokeType[] = ['PREVENTION', 'DETECTION'];
  readonly mechanisms: PokaYokeMechanism[] = [
    'PHYSICAL_SHAPE','INTERLOCK','LIMIT_SWITCH','SENSOR','VISION',
    'CHECKLIST','COLOR_CODING','POSITION_REFERENCE','COUNTER','SOFTWARE_VALIDATION','OTHER'
  ];

  readonly typeFilter      = new FormControl<PokaYokeType | ''>('');
  readonly mechanismFilter = new FormControl<PokaYokeMechanism | ''>('');

  devices$ = new BehaviorSubject<DeviceSummary[]>([]);

  readonly form = this.fb.nonNullable.group({
    deviceId: ['', [Validators.required]],
    note: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DmaicService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DmaicPokaYokeDialogComponent, AssignmentResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DmaicPokaYokeDialogData
  ) {}

  ngOnInit(): void {
    this.refreshDevices();
    this.typeFilter.valueChanges.subscribe(() => this.refreshDevices());
    this.mechanismFilter.valueChanges.subscribe(() => this.refreshDevices());
  }

  refreshDevices(): void {
    this.loading = true;
    this.svc.listDevices(0, 100, this.typeFilter.value || undefined, this.mechanismFilter.value || undefined)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: page => { this.devices$.next(page.content); this.loadError = null; },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dmaic-pokayoke-dialog] catalog failed', err?.status, err?.error?.title);
          this.loadError = safeErrorMessage(err, $localize`:@@dmaic.pokayoke.catalog-unavailable:Catalogue Poka-Yoke indisponible.`);
        }
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.submitting = true;
    this.svc.assignDevice(this.data.projectId, {
      deviceId: v.deviceId,
      note: v.note?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => {
          this.snack.open($localize`:@@dmaic.pokayoke.assigned:Poka-Yoke assigné.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(a);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dmaic-pokayoke] assign failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dmaic.pokayoke.error-assign:Erreur lors de l'assignation.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
