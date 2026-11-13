import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import {
  TenantIdentityRequest, TenantIdentityResponse, TenantPlan, TenantSummary, TenantTheme,
  TenantUpdateRequest
} from '../../../features/admin/admin.types';

/** Les formats que le serveur accepte pour un logo. */
const LOGO_TYPES = ['image/png', 'image/jpeg', 'image/webp'];
const LOGO_MAX_BYTES = 512 * 1024;

/**
 * Ce que la fenêtre rend : la correction de l'entreprise, celle de son
 * identité, et ce qu'il faut faire de son logo.
 *
 * <p>Les trois voyagent ensemble parce qu'ils se corrigent ensemble, mais
 * partent par trois routes distinctes — c'est l'appelant qui les enchaîne.
 * `logo` vaut `undefined` quand rien n'a bougé, un fichier quand on en dépose
 * un, et `null` quand on retire celui qui était là.
 */
export interface ClientEditResult {
  tenant: TenantUpdateRequest | null;
  identity: TenantIdentityRequest | null;
  logo: File | null | undefined;
}

/** Ce que la fenêtre reçoit : le client, et l'identité déjà posée sur lui. */
export interface ClientEditData {
  client: TenantSummary;
  identity: TenantIdentityResponse | null;
}

/**
 * Corriger un client existant — tout ce qui a servi à le créer.
 *
 * <p>La fenêtre ne portait que le nom et l'offre. Tout le reste — raison
 * sociale, immatriculation, TVA, adresse, contacts, couleurs, thème, logo — se
 * saisissait à la création et ne se reprenait plus : une adresse qui change,
 * un numéro de TVA mal recopié, un logo refait restaient tels quels, et la
 * seule issue était une écriture en base. Un assistant de création dont rien
 * ne se corrige n'est pas un assistant, c'est un piège.
 *
 * <p>Le formulaire part de L'EXISTANT, chargé avant l'ouverture. Un formulaire
 * de correction qui s'ouvre vide efface, au premier enregistrement, tout ce
 * que l'éditeur n'a pas retapé.
 *
 * <p>Le repère (`slug`) est montré mais pas modifiable. Il nomme le client
 * dans les adresses, les jetons et les journaux : le changer rendrait
 * illisible tout ce qui a déjà été écrit sous l'ancien, et c'est exactement ce
 * qu'un auditeur vient relire.
 *
 * <p>L'état ne se change pas ici non plus : suspendre un client coupe l'accès
 * d'un établissement entier, et cette décision se prend sur un bouton qui le
 * dit — pas sur une case au milieu d'un formulaire de correction.
 */
@Component({
  selector: 'qos-client-edit-dialog',
  templateUrl: './client-edit-dialog.component.html',
  styleUrls: ['./client-edit-dialog.component.scss'],
  standalone: false
})
export class ClientEditDialogComponent {

  readonly plans: readonly TenantPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];
  readonly themes: readonly TenantTheme[] = ['AIRY_LIGHT', 'DENSE_LIGHT', 'DARK', 'HIGH_CONTRAST'];

  readonly client: TenantSummary;
  /** Vrai si le client porte déjà un logo : on ne propose « Retirer » que là. */
  readonly logoDejaLa: boolean;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    plan: ['STARTER' as TenantPlan, [Validators.required]],
    slogan: ['', [Validators.maxLength(160)]],
    legalName: ['', [Validators.maxLength(255)]],
    legalForm: ['', [Validators.maxLength(64)]],
    registrationNumber: ['', [Validators.maxLength(64)]],
    vatNumber: ['', [Validators.maxLength(32)]],
    addressLine1: ['', [Validators.maxLength(255)]],
    postalCode: ['', [Validators.maxLength(16)]],
    city: ['', [Validators.maxLength(128)]],
    countryCode: ['', [Validators.pattern(/^$|^[A-Za-z]{2}$/)]],
    contactEmail: ['', [Validators.email, Validators.maxLength(254)]],
    contactPhone: ['', [Validators.maxLength(32)]],
    primaryColor: [''],
    secondaryColor: [''],
    themeCode: ['AIRY_LIGHT' as TenantTheme]
  });

  logoFile: File | null = null;
  logoName: string | null = null;
  logoError: string | null = null;
  /** L'éditeur a demandé le retrait du logo en place. */
  logoRetire = false;

  /** L'identité de départ, à laquelle on compare pour n'écrire que si ça bouge. */
  private readonly depart: ReturnType<ClientEditDialogComponent['form']['getRawValue']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ClientEditDialogComponent, ClientEditResult>,
    @Inject(MAT_DIALOG_DATA) data: ClientEditData
  ) {
    this.client = data.client;
    const i = data.identity;
    this.logoDejaLa = i?.logoAvailable ?? false;
    this.form.setValue({
      name: data.client.name,
      plan: data.client.plan,
      slogan: i?.slogan ?? '',
      legalName: i?.legalName ?? '',
      legalForm: i?.legalForm ?? '',
      registrationNumber: i?.registrationNumber ?? '',
      vatNumber: i?.vatNumber ?? '',
      addressLine1: i?.addressLine1 ?? '',
      postalCode: i?.postalCode ?? '',
      city: i?.city ?? '',
      countryCode: i?.countryCode ?? '',
      contactEmail: i?.contactEmail ?? '',
      contactPhone: i?.contactPhone ?? '',
      primaryColor: i?.primaryColor ?? '',
      secondaryColor: i?.secondaryColor ?? '',
      themeCode: i?.themeCode ?? 'AIRY_LIGHT'
    });
    this.depart = this.form.getRawValue();
  }

  /**
   * Rien à envoyer tant que rien n'a bougé : une écriture sans changement
   * serait un acte de plus dans la piste d'audit, pour rien.
   */
  get unchanged(): boolean {
    if (this.logoFile || this.logoRetire) {
      return false;
    }
    const v = this.form.getRawValue();
    return (Object.keys(this.depart) as (keyof typeof v)[])
      .every(cle => String(v[cle]).trim() === String(this.depart[cle]).trim());
  }

  /**
   * Le fichier choisi, contrôlé ici par confort. Le serveur, lui, vérifie le
   * type par les OCTETS du fichier et non par ce que le navigateur déclare :
   * ce contrôle-ci évite un aller-retour, il ne garantit rien.
   */
  pickLogo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.logoFile = null;
    this.logoName = null;
    this.logoError = null;
    if (!file) {
      return;
    }
    if (!LOGO_TYPES.includes(file.type)) {
      this.logoError = $localize`:@@admin.clients.logo-type:Formats acceptés : PNG, JPEG, WebP.`;
      return;
    }
    if (file.size > LOGO_MAX_BYTES) {
      this.logoError = $localize`:@@admin.clients.logo-size:Le logo ne doit pas dépasser 512 Ko.`;
      return;
    }
    this.logoFile = file;
    this.logoName = file.name;
    this.logoRetire = false;
  }

  /** Annule le fichier qu'on vient de choisir, sans toucher à celui en place. */
  clearLogo(): void {
    this.logoFile = null;
    this.logoName = null;
    this.logoError = null;
  }

  /** Demande le retrait du logo déjà posé : il partira à l'enregistrement. */
  retirerLogo(): void {
    this.logoFile = null;
    this.logoName = null;
    this.logoError = null;
    this.logoRetire = true;
  }

  annulerRetrait(): void { this.logoRetire = false; }

  submit(): void {
    if (this.form.invalid || this.unchanged) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const net = (x: string): string | null => {
      const t = x.trim();
      return t.length ? t : null;
    };

    const entrepriseBouge = v.name.trim() !== this.depart.name.trim() || v.plan !== this.depart.plan;
    const identiteBouge = (Object.keys(this.depart) as (keyof typeof v)[])
      .filter(cle => cle !== 'name' && cle !== 'plan')
      .some(cle => String(v[cle]).trim() !== String(this.depart[cle]).trim());

    this.dialogRef.close({
      tenant: entrepriseBouge ? { name: v.name.trim(), plan: v.plan } : null,
      identity: identiteBouge ? {
        slogan: net(v.slogan), legalName: net(v.legalName), legalForm: net(v.legalForm),
        registrationNumber: net(v.registrationNumber), vatNumber: net(v.vatNumber),
        // La seconde ligne d'adresse n'est pas à l'écran : l'y ajouter sans la
        // montrer l'effacerait à chaque enregistrement.
        addressLine1: net(v.addressLine1), addressLine2: null,
        postalCode: net(v.postalCode), city: net(v.city),
        countryCode: net(v.countryCode)?.toUpperCase() ?? null,
        contactEmail: net(v.contactEmail), contactPhone: net(v.contactPhone),
        primaryColor: net(v.primaryColor), secondaryColor: net(v.secondaryColor),
        themeCode: v.themeCode
      } : null,
      logo: this.logoFile ?? (this.logoRetire ? null : undefined)
    });
  }

  cancel(): void { this.dialogRef.close(); }

  /** Le libellé d'un thème, pour ne pas montrer `HIGH_CONTRAST` à un humain. */
  themeLabel(theme: TenantTheme): string {
    switch (theme) {
      case 'DENSE_LIGHT': return $localize`:@@admin.clients.theme-dense:Clair dense — poste de travail`;
      case 'DARK': return $localize`:@@admin.clients.theme-dark:Sombre`;
      case 'HIGH_CONTRAST': return $localize`:@@admin.clients.theme-contrast:Contraste renforcé — atelier, extérieur`;
      default: return $localize`:@@admin.clients.theme-airy:Clair aéré — par défaut`;
    }
  }
}
