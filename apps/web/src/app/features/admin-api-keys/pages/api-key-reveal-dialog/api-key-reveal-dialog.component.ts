import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { IssuedApiKey } from '../../api-keys.types';

export interface ApiKeyRevealDialogData {
  issued: IssuedApiKey;
  /** Vraie après une rotation : l'ancienne clé vient d'être révoquée, il faut le dire. */
  rotated: boolean;
}

/**
 * Affichage UNIQUE du secret en clair (§13.1).
 *
 * Le serveur ne stocke qu'un bcrypt : ce dialogue est le seul et dernier endroit où le
 * secret existe côté client. Il est donc ouvert en `disableClose` par l'appelant — une
 * fermeture accidentelle (Échap, clic hors cadre) coûterait une nouvelle rotation.
 *
 * Rien n'est mis en cache ici : la valeur vit dans les données du dialogue, détruites
 * à la fermeture.
 */
@Component({
  selector: 'qos-api-key-reveal-dialog',
  templateUrl: './api-key-reveal-dialog.component.html',
  styleUrls: ['./api-key-reveal-dialog.component.scss'],
  standalone: false
})
export class ApiKeyRevealDialogComponent {

  copied = false;
  /** Le presse-papiers a refusé : on bascule sur une consigne de copie manuelle. */
  copyFailed = false;

  constructor(
    private readonly dialogRef: MatDialogRef<ApiKeyRevealDialogComponent, void>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ApiKeyRevealDialogData
  ) {}

  get plaintext(): string {
    return this.data.issued.plaintext;
  }

  copy(): void {
    this.copyFailed = false;
    const clipboard = navigator.clipboard;
    if (!clipboard || typeof clipboard.writeText !== 'function') {
      this.fallbackCopy();
      return;
    }
    clipboard.writeText(this.plaintext).then(
      () => { this.copied = true; },
      () => this.fallbackCopy()
    );
  }

  close(): void {
    this.dialogRef.close();
  }

  /**
   * L'API Clipboard est refusée hors contexte sécurisé (HTTP interne, iframe non
   * autorisée) — cas courant sur une instance on-premise. `execCommand` reste le seul
   * chemin universel ; s'il échoue aussi, on demande une copie manuelle plutôt que de
   * laisser croire que la clé est au presse-papiers.
   */
  private fallbackCopy(): void {
    const holder = document.createElement('textarea');
    holder.value = this.plaintext;
    holder.setAttribute('readonly', '');
    holder.style.position = 'fixed';
    holder.style.top = '-1000px';
    holder.style.opacity = '0';
    document.body.appendChild(holder);
    holder.select();
    let ok = false;
    try {
      ok = document.execCommand('copy');
    } catch {
      ok = false;
    }
    document.body.removeChild(holder);
    this.copied = ok;
    this.copyFailed = !ok;
  }
}
