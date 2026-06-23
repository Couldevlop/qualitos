import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { CirclesService } from '../../circles.service';
import { MeetingMinutes } from '../../circles.types';
import { safeErrorMessage } from '../../../../core/http/error-message';

export interface CirclesMinutesDialogData {
  circleId: string;
  meetingId: string;
  meetingTitle: string;
}

@Component({
  selector: 'qos-circles-minutes-dialog',
  templateUrl: './circles-minutes-dialog.component.html',
  styleUrls: ['./circles-minutes-dialog.component.scss'],
  standalone: false
})
export class CirclesMinutesDialogComponent {

  transcript = '';
  minutes: MeetingMinutes | null = null;
  readonly generating$ = new BehaviorSubject<boolean>(false);
  error: string | null = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: CirclesMinutesDialogData,
    private readonly dialogRef: MatDialogRef<CirclesMinutesDialogComponent>,
    private readonly circles: CirclesService
  ) {}

  generate(): void {
    if (!this.transcript.trim() || this.generating$.value) return;
    this.error = null;
    this.minutes = null;
    this.generating$.next(true);
    this.circles.generateMinutes(this.data.circleId, this.data.meetingId, { transcript: this.transcript })
      .pipe(finalize(() => this.generating$.next(false)))
      .subscribe({
        next: minutes => { this.minutes = minutes; },
        error: err => {
          this.error = safeErrorMessage(err,
            $localize`:@@circles.minutes.generate-error:Erreur lors de la génération du compte-rendu.`);
        }
      });
  }

  close(): void { this.dialogRef.close(this.minutes); }
}
