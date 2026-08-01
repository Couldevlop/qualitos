import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { connectorStatusLabel } from '../../connectors.labels';
import { ConnectorsService } from '../../connectors.service';
import { CommConnection, CommProvider, ConnectorEditableStatus } from '../../connectors.types';

export interface CommConnectionDialogData {
  /** `null` en création ; la connexion à modifier sinon. */
  connection: CommConnection | null;
}

interface ProviderOption {
  value: CommProvider;
  label: string;
  /** Chemin type de l'URL fournie par l'outil — aide au repérage lors du collage. */
  urlExample: string;
}

interface CommFormValue {
  name: string;
  provider: CommProvider;
  webhookUrl: string;
  channel: string;
  status: ConnectorEditableStatus;
}

/**
 * Formulaire de connexion de communication (Teams / Slack / Mattermost).
 *
 * Particularité de cette famille : l'URL d'incoming-webhook EST le secret (elle porte
 * un jeton non devinable). Le serveur ne la renvoie donc jamais — elle est traitée
 * exactement comme un mot de passe : champ masqué, jamais pré-rempli en modification,
 * envoyée uniquement quand on la remplace.
 *
 * Le fournisseur est figé après création : la requête de mise à jour serveur ne
 * l'accepte pas.
 */
@Component({
  selector: 'qos-comm-connection-dialog',
  templateUrl: './comm-connection-dialog.component.html',
  styleUrls: ['./comm-connection-dialog.component.scss'],
  standalone: false
})
export class CommConnectionDialogComponent {

  readonly editing: boolean;
  submitting = false;
  showWebhook = false;
  errorMessage: string | null = null;

  readonly providers: ProviderOption[] = [
    { value: 'TEAMS', label: 'Microsoft Teams', urlExample: 'https://…webhook.office.com/webhookb2/…' },
    { value: 'SLACK', label: 'Slack', urlExample: 'https://hooks.slack.com/services/…' },
    { value: 'MATTERMOST', label: 'Mattermost', urlExample: 'https://chat.example.com/hooks/…' }
  ];

  readonly statuses: ConnectorEditableStatus[] = ['ACTIVE', 'DISABLED'];

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ConnectorsService,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<CommConnectionDialogComponent, CommConnection>,
    @Inject(MAT_DIALOG_DATA) private readonly data: CommConnectionDialogData
  ) {
    const c = data.connection;
    this.editing = c !== null;
    this.form = this.fb.nonNullable.group({
      name: [c?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      provider: [{ value: c?.provider ?? ('TEAMS' as CommProvider), disabled: this.editing },
        [Validators.required]],
      // Miroir des contraintes serveur : 8..1024 et https:// obligatoire (SSRF + confidentialité).
      webhookUrl: ['', this.editing
        ? [Validators.minLength(8), Validators.maxLength(1024), Validators.pattern(/^https:\/\/.+$/)]
        : [Validators.required, Validators.minLength(8), Validators.maxLength(1024),
           Validators.pattern(/^https:\/\/.+$/)]],
      channel: [c?.channel ?? '', [Validators.maxLength(200)]],
      status: [(c?.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE') as ConnectorEditableStatus]
    });
  }

  get title(): string {
    return this.editing
      ? $localize`:@@connectors.comm.dialog-edit-title:Modifier la destination`
      : $localize`:@@connectors.comm.dialog-create-title:Nouvelle destination de notification`;
  }

  get submitLabel(): string {
    return this.editing
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;
  }

  get webhookLabel(): string {
    return this.editing
      ? $localize`:@@connectors.comm.webhook-rotate-label:Nouvelle URL de webhook (vide = inchangée)`
      : $localize`:@@connectors.comm.webhook-label:URL du webhook entrant`;
  }

  get webhookToggleLabel(): string {
    return this.showWebhook
      ? $localize`:@@connectors.comm.webhook-hide:Masquer l'URL du webhook`
      : $localize`:@@connectors.comm.webhook-show:Afficher l'URL du webhook`;
  }

  /** Exemple d'URL du fournisseur choisi : évite de coller l'URL du mauvais outil. */
  get selectedUrlExample(): string {
    const value = this.form.controls.provider.value;
    return this.providers.find(p => p.value === value)?.urlExample ?? '';
  }

  toggleWebhook(): void {
    this.showWebhook = !this.showWebhook;
  }

  statusLabel(status: ConnectorEditableStatus): string {
    return connectorStatusLabel(status);
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue() as CommFormValue;
    const request$ = this.editing ? this.buildUpdate(v) : this.buildCreate(v);
    if (!request$) return;

    this.submitting = true;
    this.errorMessage = null;
    request$.pipe(finalize(() => { this.submitting = false; })).subscribe({
      next: connection => this.dialogRef.close(connection),
      error: err => {
        this.errorMessage = safeErrorMessage(err,
          $localize`:@@connectors.save-error:L'enregistrement de la connexion a échoué.`);
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private buildUpdate(v: CommFormValue): Observable<CommConnection> {
    return this.svc.updateComm(this.data.connection!.id, {
      name: v.name.trim(),
      // Champ vidé = URL conservée : le serveur n'applique que les clés présentes.
      webhookUrl: v.webhookUrl.trim() || undefined,
      channel: v.channel.trim() || undefined,
      status: v.status
    });
  }

  private buildCreate(v: CommFormValue): Observable<CommConnection> | null {
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.errorMessage = $localize`:@@connectors.no-session:Session expirée — reconnectez-vous pour créer une connexion.`;
      return null;
    }
    return this.svc.createComm({
      name: v.name.trim(),
      provider: v.provider,
      webhookUrl: v.webhookUrl.trim(),
      channel: v.channel.trim() || undefined,
      createdBy
    });
  }
}
