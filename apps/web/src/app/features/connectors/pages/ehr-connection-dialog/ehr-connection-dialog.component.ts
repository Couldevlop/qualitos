import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { connectorStatusLabel, ehrAuthModeLabel } from '../../connectors.labels';
import { ConnectorsService } from '../../connectors.service';
import {
  ConnectorEditableStatus, EhrAuthMode, EhrConnection, EhrProvider
} from '../../connectors.types';

export interface EhrConnectionDialogData {
  /** `null` en création ; la connexion à modifier sinon. */
  connection: EhrConnection | null;
}

interface ProviderOption {
  value: EhrProvider;
  label: string;
}

interface EhrFormValue {
  name: string;
  provider: EhrProvider;
  fhirBaseUrl: string;
  authMode: EhrAuthMode;
  username: string;
  secret: string;
  resourceCategory: string;
  status: ConnectorEditableStatus;
}

/**
 * Formulaire de connexion EHR / HL7 FHIR.
 *
 * Le mode d'authentification décide de ce que « secret » signifie : mot de passe du
 * compte de service en BASIC, jeton porteur en BEARER. Le compte de service n'a donc
 * de sens qu'en BASIC — on ne le propose pas en BEARER plutôt que de laisser saisir
 * une valeur que le serveur ignorera au moment de forger l'en-tête.
 *
 * Le fournisseur (version FHIR) est figé après création : la requête de mise à jour
 * serveur ne l'accepte pas.
 */
@Component({
  selector: 'qos-ehr-connection-dialog',
  templateUrl: './ehr-connection-dialog.component.html',
  styleUrls: ['./ehr-connection-dialog.component.scss'],
  standalone: false
})
export class EhrConnectionDialogComponent {

  readonly editing: boolean;
  submitting = false;
  showSecret = false;
  errorMessage: string | null = null;

  readonly providers: ProviderOption[] = [
    { value: 'FHIR_R4', label: 'HL7 FHIR R4' },
    { value: 'FHIR_R5', label: 'HL7 FHIR R5' }
  ];

  readonly authModes: EhrAuthMode[] = ['BASIC', 'BEARER'];
  readonly statuses: ConnectorEditableStatus[] = ['ACTIVE', 'DISABLED'];

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ConnectorsService,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<EhrConnectionDialogComponent, EhrConnection>,
    @Inject(MAT_DIALOG_DATA) private readonly data: EhrConnectionDialogData
  ) {
    const c = data.connection;
    this.editing = c !== null;
    this.form = this.fb.nonNullable.group({
      name: [c?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      provider: [{ value: c?.provider ?? ('FHIR_R5' as EhrProvider), disabled: this.editing },
        [Validators.required]],
      fhirBaseUrl: [c?.fhirBaseUrl ?? '', [
        Validators.required, Validators.maxLength(512), Validators.pattern(/^https:\/\/.+$/)
      ]],
      authMode: [c?.authMode ?? ('BASIC' as EhrAuthMode), [Validators.required]],
      username: [c?.username ?? '', [Validators.maxLength(200)]],
      secret: ['', this.editing
        ? [Validators.minLength(4), Validators.maxLength(1024)]
        : [Validators.required, Validators.minLength(4), Validators.maxLength(1024)]],
      resourceCategory: [c?.resourceCategory ?? '', [Validators.maxLength(120)]],
      status: [(c?.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE') as ConnectorEditableStatus]
    });
  }

  get title(): string {
    return this.editing
      ? $localize`:@@connectors.ehr.dialog-edit-title:Modifier la connexion FHIR`
      : $localize`:@@connectors.ehr.dialog-create-title:Nouvelle connexion FHIR`;
  }

  get submitLabel(): string {
    return this.editing
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;
  }

  /** En BEARER le secret EST le jeton : le nommer autrement induirait en erreur. */
  get secretLabel(): string {
    const bearer = this.form.controls.authMode.value === 'BEARER';
    if (this.editing) {
      return bearer
        ? $localize`:@@connectors.ehr.token-rotate-label:Nouveau jeton (vide = inchangé)`
        : $localize`:@@connectors.secret-rotate-label:Nouveau secret (vide = inchangé)`;
    }
    return bearer
      ? $localize`:@@connectors.ehr.token-label:Jeton porteur`
      : $localize`:@@connectors.secret-label:Secret / jeton d'API`;
  }

  get secretToggleLabel(): string {
    return this.showSecret
      ? $localize`:@@connectors.secret-hide:Masquer le secret`
      : $localize`:@@connectors.secret-show:Afficher le secret`;
  }

  /** Le compte de service n'est utilisé que par l'en-tête Basic. */
  get usernameRelevant(): boolean {
    return this.form.controls.authMode.value === 'BASIC';
  }

  toggleSecret(): void {
    this.showSecret = !this.showSecret;
  }

  authModeLabel(mode: EhrAuthMode): string {
    return ehrAuthModeLabel(mode);
  }

  statusLabel(status: ConnectorEditableStatus): string {
    return connectorStatusLabel(status);
  }

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue() as EhrFormValue;
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

  private buildUpdate(v: EhrFormValue): Observable<EhrConnection> {
    return this.svc.updateEhr(this.data.connection!.id, {
      name: v.name.trim(),
      fhirBaseUrl: v.fhirBaseUrl.trim(),
      authMode: v.authMode,
      username: this.usernameForPayload(v),
      // Champ vidé = champ omis : le serveur n'applique que les clés présentes.
      secret: v.secret.trim() || undefined,
      resourceCategory: v.resourceCategory.trim() || undefined,
      status: v.status
    });
  }

  private buildCreate(v: EhrFormValue): Observable<EhrConnection> | null {
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.errorMessage = $localize`:@@connectors.no-session:Session expirée — reconnectez-vous pour créer une connexion.`;
      return null;
    }
    return this.svc.createEhr({
      name: v.name.trim(),
      provider: v.provider,
      fhirBaseUrl: v.fhirBaseUrl.trim(),
      authMode: v.authMode,
      username: this.usernameForPayload(v),
      secret: v.secret.trim(),
      resourceCategory: v.resourceCategory.trim() || undefined,
      createdBy
    });
  }

  /** En BEARER l'en-tête est forgé sans utilisateur : on n'envoie pas une valeur morte. */
  private usernameForPayload(v: EhrFormValue): string | undefined {
    return v.authMode === 'BASIC' ? (v.username.trim() || undefined) : undefined;
  }
}
