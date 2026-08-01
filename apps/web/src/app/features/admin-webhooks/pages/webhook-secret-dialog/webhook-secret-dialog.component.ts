import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface WebhookSecretDialogData {
  /** Nom de l'abonnement, pour que l'admin sache à quoi rattacher la clé copiée. */
  subscriptionName: string;
  secret: string;
  /** Vrai lors d'une rotation : le message insiste alors sur l'invalidation de l'ancien secret. */
  rotated: boolean;
}

/**
 * Révélation unique du secret HMAC (§13.2), même règle que les clés d'API : le serveur
 * ne le renvoie plus jamais, donc si l'admin ferme sans copier, la seule issue est une
 * rotation. D'où l'affichage plein cadre, la copie explicite et l'avertissement.
 */
@Component({
  selector: 'qos-webhook-secret-dialog',
  templateUrl: './webhook-secret-dialog.component.html',
  styleUrls: ['./webhook-secret-dialog.component.scss'],
  standalone: false
})
export class WebhookSecretDialogComponent {

  copied = false;
  /** La copie programmatique est refusée hors contexte sécurisé : on le dit au lieu de faire semblant. */
  copyFailed = false;

  constructor(
    private readonly dialogRef: MatDialogRef<WebhookSecretDialogComponent, void>,
    @Inject(MAT_DIALOG_DATA) readonly data: WebhookSecretDialogData
  ) {}

  copy(): void {
    const clipboard = navigator.clipboard;
    if (!clipboard) {
      this.copied = false;
      this.copyFailed = true;
      return;
    }
    clipboard.writeText(this.data.secret).then(
      () => { this.copied = true; this.copyFailed = false; },
      () => { this.copied = false; this.copyFailed = true; }
    );
  }

  close(): void {
    this.dialogRef.close();
  }
}
