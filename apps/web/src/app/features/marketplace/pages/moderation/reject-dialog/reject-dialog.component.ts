import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

export interface RejectDialogResult {
  reason: string;
}

/**
 * Dialogue de rejet motivé d'un pack par l'éditeur. Le motif est obligatoire
 * (le backend l'exige également).
 */
@Component({
  selector: 'qos-marketplace-reject-dialog',
  templateUrl: './reject-dialog.component.html',
  styleUrls: ['./reject-dialog.component.scss'],
  standalone: false
})
export class RejectDialogComponent {

  reason = '';

  constructor(private readonly dialogRef: MatDialogRef<RejectDialogComponent, RejectDialogResult>) {}

  cancel(): void { this.dialogRef.close(); }

  confirm(): void {
    const reason = this.reason.trim();
    if (!reason) { return; }
    this.dialogRef.close({ reason });
  }
}
