import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { connectorStatusLabel } from '../../connectors.labels';
import { ConnectorsService } from '../../connectors.service';
import { ConnectorEditableStatus, ErpConnection, ErpProvider } from '../../connectors.types';

export interface ErpConnectionDialogData {
  /** `null` en création ; la connexion à modifier sinon. */
  connection: ErpConnection | null;
}

/** Fournisseurs ERP couverts par un client serveur (§13.3). */
interface ProviderOption {
  value: ErpProvider;
  label: string;
  hint: string;
}

/** Valeur brute du formulaire (le fournisseur est désactivé en édition, donc « raw »). */
interface ErpFormValue {
  name: string;
  provider: ErpProvider;
  baseUrl: string;
  username: string;
  secret: string;
  externalScope: string;
  status: ConnectorEditableStatus;
}

/**
 * Formulaire de connexion ERP (SAP / Oracle Fusion / Dynamics).
 *
 * Traitement du secret : il n'est JAMAIS relu depuis le serveur (la réponse ne le
 * contient pas). En modification, le champ reste donc vide et n'est envoyé que si
 * l'administrateur saisit une nouvelle valeur — ce qui rend la rotation possible
 * sans jamais exposer l'ancienne.
 *
 * Le fournisseur est figé après création : la requête de mise à jour serveur ne
 * l'accepte pas, l'afficher modifiable promettrait une action sans effet.
 */
@Component({
  selector: 'qos-erp-connection-dialog',
  templateUrl: './erp-connection-dialog.component.html',
  styleUrls: ['./erp-connection-dialog.component.scss'],
  standalone: false
})
export class ErpConnectionDialogComponent {

  readonly editing: boolean;
  submitting = false;
  showSecret = false;
  /** Message d'échec de l'enregistrement, affiché dans le formulaire lui-même. */
  errorMessage: string | null = null;

  readonly providers: ProviderOption[] = [
    { value: 'SAP', label: 'SAP S/4HANA', hint: 'OData V2/V4' },
    { value: 'ORACLE_FUSION', label: 'Oracle Fusion Cloud', hint: 'REST API' },
    { value: 'DYNAMICS', label: 'Microsoft Dynamics 365', hint: 'OData v4' }
  ];

  readonly statuses: ConnectorEditableStatus[] = ['ACTIVE', 'DISABLED'];

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ConnectorsService,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<ErpConnectionDialogComponent, ErpConnection>,
    @Inject(MAT_DIALOG_DATA) private readonly data: ErpConnectionDialogData
  ) {
    const c = data.connection;
    this.editing = c !== null;
    this.form = this.fb.nonNullable.group({
      name: [c?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      provider: [{ value: c?.provider ?? ('SAP' as ErpProvider), disabled: this.editing },
        [Validators.required]],
      // Miroir du motif serveur `^https://.+` : refuser en clair côté client évite
      // un aller-retour 400 pour la faute la plus fréquente (http:// au lieu de https://).
      baseUrl: [c?.baseUrl ?? '', [
        Validators.required, Validators.maxLength(512), Validators.pattern(/^https:\/\/.+$/)
      ]],
      username: [c?.username ?? '', [Validators.maxLength(200)]],
      secret: ['', this.editing
        ? [Validators.minLength(4), Validators.maxLength(1024)]
        : [Validators.required, Validators.minLength(4), Validators.maxLength(1024)]],
      externalScope: [c?.externalScope ?? '', [Validators.maxLength(200)]],
      status: [(c?.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE') as ConnectorEditableStatus]
    });
  }

  get title(): string {
    return this.editing
      ? $localize`:@@connectors.erp.dialog-edit-title:Modifier la connexion ERP`
      : $localize`:@@connectors.erp.dialog-create-title:Nouvelle connexion ERP`;
  }

  get submitLabel(): string {
    return this.editing
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;
  }

  get secretLabel(): string {
    return this.editing
      ? $localize`:@@connectors.secret-rotate-label:Nouveau secret (vide = inchangé)`
      : $localize`:@@connectors.secret-label:Secret / jeton d'API`;
  }

  get secretToggleLabel(): string {
    return this.showSecret
      ? $localize`:@@connectors.secret-hide:Masquer le secret`
      : $localize`:@@connectors.secret-show:Afficher le secret`;
  }

  toggleSecret(): void {
    this.showSecret = !this.showSecret;
  }

  /** Passe par une méthode typée : le tableau et le formulaire doivent nommer l'état pareil. */
  statusLabel(status: ConnectorEditableStatus): string {
    return connectorStatusLabel(status);
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue() as ErpFormValue;
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

  private buildUpdate(v: ErpFormValue): Observable<ErpConnection> {
    return this.svc.updateErp(this.data.connection!.id, {
      name: v.name.trim(),
      baseUrl: v.baseUrl.trim(),
      // Champ vidé = champ omis : le serveur n'applique que les clés présentes, envoyer
      // une chaîne vide écraserait la valeur existante au lieu de la conserver.
      username: v.username.trim() || undefined,
      secret: v.secret.trim() || undefined,
      externalScope: v.externalScope.trim() || undefined,
      status: v.status
    });
  }

  /**
   * `createdBy` est exigé par le serveur et doit désigner l'auteur réel : on le prend
   * dans la session, jamais dans un champ saisissable. Sans session valide, la création
   * est refusée ici plutôt que d'envoyer un identifiant inventé.
   */
  private buildCreate(v: ErpFormValue): Observable<ErpConnection> | null {
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.errorMessage = $localize`:@@connectors.no-session:Session expirée — reconnectez-vous pour créer une connexion.`;
      return null;
    }
    return this.svc.createErp({
      name: v.name.trim(),
      provider: v.provider,
      baseUrl: v.baseUrl.trim(),
      username: v.username.trim() || undefined,
      secret: v.secret.trim(),
      externalScope: v.externalScope.trim() || undefined,
      createdBy
    });
  }
}
