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
  /** Transcription audio en cours (§3.3) — désactive les deux actions du dialogue. */
  readonly transcribing$ = new BehaviorSubject<boolean>(false);
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

  /**
   * Transcrit l'enregistrement choisi et pré-remplit le champ transcript (§3.3).
   * Le texte n'est jamais soumis automatiquement à la génération : l'animateur le relit
   * et déclenche lui-même le compte-rendu (l'IA propose, l'humain décide, §12.3).
   */
  onAudioSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // Réinitialise la valeur : sans cela, re-sélectionner le même fichier n'émet rien.
    input.value = '';
    if (!file || this.transcribing$.value) return;

    this.error = null;
    this.transcribing$.next(true);
    this.circles.transcribeMeeting(this.data.circleId, this.data.meetingId, file)
      .pipe(finalize(() => this.transcribing$.next(false)))
      .subscribe({
        next: result => {
          // Concatène si l'animateur a déjà saisi du texte, plutôt que de l'écraser.
          this.transcript = this.transcript.trim()
            ? `${this.transcript.trim()}\n${result.text}`
            : result.text;
        },
        error: err => {
          this.error = safeErrorMessage(err,
            $localize`:@@circles.minutes.transcribe-error:Transcription audio indisponible.`);
        }
      });
  }

  close(): void { this.dialogRef.close(this.minutes); }
}
