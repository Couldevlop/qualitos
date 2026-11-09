import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, switchMap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../core/http/error-message';
import {
  ModuleCatalogEntry, OnboardResponse, TenantIdentityRequest, TenantPlan, TenantSummary, TenantTheme
} from '../../../features/admin/admin.types';
import { ConfirmDialogComponent } from '../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { ClientsService } from '../../clients.service';
import { TenantModulesService } from '../../../features/admin/tenant-modules.service';
import {
  ClientEditDialogComponent, ClientEditResult
} from '../client-edit-dialog/client-edit-dialog.component';

/** Les étapes de l'assistant, dans l'ordre : c'est une vraie séquence. */
export type WizardStep = 1 | 2 | 3 | 4;

/** Les types d'image que le serveur accepte pour un logo, et la taille qu'il borne. */
export const LOGO_TYPES: readonly string[] = ['image/png', 'image/jpeg', 'image/webp'];
export const LOGO_MAX_BYTES = 512 * 1024;

export interface ModuleFamily {
  category: string;
  modules: ModuleCatalogEntry[];
}

/**
 * Les clients de la plateforme, et la création d'un client en trois étapes
 * (ADR 0079) : l'entreprise, ses modules, son premier administrateur.
 *
 * <p>Cocher un module coche ce dont il dépend ; un module requis par un autre
 * coché ne se décoche pas. Les modules du socle sont inclus d'office. À la fin,
 * le compte de l'administrateur est créé et ses identifiants s'affichent une
 * fois ; un module que le moteur a refusé est signalé, sans annuler le client.
 */
@Component({
  selector: 'qos-clients',
  templateUrl: './clients.component.html',
  styleUrls: ['./clients.component.scss'],
  standalone: false
})
export class ClientsComponent implements OnInit {

  readonly plans: TenantPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];
  readonly themes: readonly TenantTheme[] = ['AIRY_LIGHT', 'DENSE_LIGHT', 'DARK', 'HIGH_CONTRAST'];

  clients: TenantSummary[] = [];
  loading = false;
  failed = false;

  /** Le client sur lequel une écriture est en cours : évite le double envoi. */
  busyId: string | null = null;

  wizard = false;
  step: WizardStep = 1;
  families: ModuleFamily[] = [];
  catalogFailed = false;
  selected = new Set<string>();
  creating = false;
  result: OnboardResponse | null = null;

  /** Le logo choisi, gardé en mémoire jusqu'à ce que le client existe et ait un identifiant. */
  logoFile: File | null = null;
  logoName: string | null = null;
  logoError: string | null = null;

  /** Ce qui a échoué APRÈS la création : le client existe, son habillage non. */
  identityWarning: string | null = null;
  logoWarning: string | null = null;

  private catalog: ModuleCatalogEntry[] = [];

  readonly companyForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$/)]],
    plan: ['STARTER' as TenantPlan, Validators.required]
  });

  /**
   * L'identité et le thème, posés à la création.
   *
   * <p>Tout y est facultatif sauf le thème : un client se crée en trente
   * secondes quand il faut aller vite, et se complète ensuite depuis sa
   * propre console. Les bornes reprennent celles du serveur ; les motifs
   * acceptent le vide, sans quoi un champ laissé de côté bloquerait l'étape.
   */
  readonly identityForm = this.fb.nonNullable.group({
    slogan: ['', Validators.maxLength(160)],
    legalName: ['', Validators.maxLength(255)],
    legalForm: ['', Validators.maxLength(64)],
    registrationNumber: ['', Validators.maxLength(64)],
    vatNumber: ['', Validators.maxLength(32)],
    addressLine1: ['', Validators.maxLength(255)],
    postalCode: ['', Validators.maxLength(16)],
    city: ['', Validators.maxLength(128)],
    countryCode: ['', Validators.pattern(/^([A-Za-z]{2})?$/)],
    contactEmail: ['', [Validators.email, Validators.maxLength(254)]],
    contactPhone: ['', Validators.maxLength(32)],
    primaryColor: ['', Validators.pattern(/^(#[0-9a-fA-F]{6})?$/)],
    secondaryColor: ['', Validators.pattern(/^(#[0-9a-fA-F]{6})?$/)],
    themeCode: ['AIRY_LIGHT' as TenantTheme, Validators.required]
  });

  readonly adminForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    firstName: ['', Validators.maxLength(100)],
    lastName: ['', Validators.maxLength(100)]
  });

  constructor(
    private readonly service: ClientsService,
    private readonly modules: TenantModulesService,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.list().pipe(finalize(() => this.loading = false)).subscribe({
      next: page => this.clients = page.content,
      error: () => this.failed = true
    });
  }

  // ---------- la vie d'un client, apres sa creation ----------

  /**
   * Corrige le nom ou l'offre d'un client.
   *
   * <p>La liste n'etait que consultable : une raison sociale mal orthographiee
   * restait sur tous les documents du client, et un changement d'offre
   * demandait une ecriture en base. Les deux passent maintenant par une route
   * que le serveur journalise.
   */
  corriger(client: TenantSummary): void {
    this.dialog.open<ClientEditDialogComponent, TenantSummary, ClientEditResult>(
      ClientEditDialogComponent, { data: client, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable' }
    ).afterClosed().subscribe(correction => {
      if (!correction) return;
      this.busyId = client.id;
      this.service.update(client.id, correction)
        .pipe(finalize(() => this.busyId = null))
        .subscribe({
          next: maj => {
            this.remplacer(maj);
            this.snack.open($localize`:@@platform.clients.saved:Client mis à jour.`, undefined, { duration: 4000 });
          },
          error: err => this.snack.open(
            safeErrorMessage(err, $localize`:@@platform.clients.save-failed:La correction n'a pas pu être enregistrée.`),
            undefined, { duration: 6000 })
        });
    });
  }

  /**
   * Suspend un client, ou le reactive.
   *
   * <p>Suspendre coupe l'acces de tout un etablissement : on le demande, et le
   * message dit ce que cela fait et ce que cela ne fait pas. Les donnees du
   * client restent — ce sont des preuves d'audit, souvent soumises a une duree
   * de conservation reglementaire — et l'acte est consigne dans la piste
   * d'audit de la plateforme.
   */
  basculer(client: TenantSummary): void {
    if (client.active) {
      this.dialog.open(ConfirmDialogComponent, {
        panelClass: 'qos-dialog-panel',
        data: {
          title: $localize`:@@platform.clients.suspend-title:Suspendre ce client ?`,
          message: $localize`:@@platform.clients.suspend-message:Ses utilisateurs ne pourront plus se connecter. Rien n'est effacé : ses dossiers, ses preuves et sa piste d'audit restent en place, et tout reprend si vous le réactivez.`,
          confirmLabel: $localize`:@@platform.clients.suspend-action:Suspendre`,
          destructive: true
        }
      }).afterClosed().subscribe(ok => {
        if (ok) this.suspendre(client);
      });
      return;
    }
    this.reactiver(client);
  }

  private suspendre(client: TenantSummary): void {
    this.busyId = client.id;
    this.service.suspend(client.id).pipe(finalize(() => this.busyId = null)).subscribe({
      next: () => {
        this.remplacer({ ...client, active: false });
        this.snack.open($localize`:@@platform.clients.suspended:Client suspendu ; ses données sont conservées.`,
          undefined, { duration: 5000 });
      },
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@platform.clients.suspend-failed:La suspension n'a pas pu être appliquée.`),
        undefined, { duration: 6000 })
    });
  }

  private reactiver(client: TenantSummary): void {
    this.busyId = client.id;
    this.service.update(client.id, { name: client.name, plan: client.plan, active: true })
      .pipe(finalize(() => this.busyId = null))
      .subscribe({
        next: maj => {
          this.remplacer(maj);
          this.snack.open($localize`:@@platform.clients.resumed:Client réactivé.`, undefined, { duration: 4000 });
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@platform.clients.resume-failed:La réactivation n'a pas pu être appliquée.`),
          undefined, { duration: 6000 })
      });
  }

  /** Remplace la ligne sur place : recharger toute la liste ferait sauter la lecture. */
  private remplacer(maj: TenantSummary): void {
    this.clients = this.clients.map(c => c.id === maj.id ? { ...c, ...maj } : c);
  }

  // ---------- assistant ----------

  openWizard(): void {
    this.wizard = true;
    this.step = 1;
    this.result = null;
    this.selected = new Set();
    this.companyForm.reset();
    this.identityForm.reset();
    this.adminForm.reset();
    this.logoFile = null;
    this.logoName = null;
    this.logoError = null;
    this.identityWarning = null;
    this.logoWarning = null;
    if (!this.catalog.length) {
      this.modules.catalog().subscribe({
        next: c => {
          this.catalog = c;
          this.families = familles(c);
          this.catalogFailed = false;
        },
        error: () => this.catalogFailed = true
      });
    }
  }

  closeWizard(): void {
    this.wizard = false;
  }

  /** Le code se déduit du nom tant qu'on ne l'a pas écrit soi-même. */
  suggestSlug(): void {
    const slug = this.companyForm.controls.slug;
    if (slug.dirty) return;
    slug.setValue(slugify(this.companyForm.controls.name.value));
  }

  next(): void {
    if (this.step === 1) {
      if (this.companyForm.invalid) {
        this.companyForm.markAllAsTouched();
        return;
      }
      this.step = 2;
    } else if (this.step === 2) {
      if (this.identityForm.invalid) {
        this.identityForm.markAllAsTouched();
        return;
      }
      this.step = 3;
    } else if (this.step === 3) {
      this.step = 4;
    }
  }

  /**
   * Le logo choisi : refusé ici s'il ne peut pas convenir, pour ne pas
   * découvrir à la création qu'il est trop lourd.
   *
   * <p>Ce contrôle est un confort, pas une garantie : le serveur vérifie le
   * type par les octets du fichier et non par ce que le navigateur déclare.
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
  }

  clearLogo(): void {
    this.logoFile = null;
    this.logoName = null;
    this.logoError = null;
  }

  back(): void {
    if (this.step > 1) this.step = (this.step - 1) as WizardStep;
  }

  isSelected(m: ModuleCatalogEntry): boolean {
    return m.coreModule || this.selected.has(m.code);
  }

  /** Les modules cochés qui ont besoin de celui-ci : tant qu'il y en a, il reste coché. */
  requiredBy(m: ModuleCatalogEntry): string[] {
    return this.catalog.filter(x => this.selected.has(x.code) && x.dependencies.includes(m.code)).map(x => x.name);
  }

  toggle(m: ModuleCatalogEntry): void {
    if (m.coreModule) return;
    const choix = new Set(this.selected);
    if (choix.has(m.code)) {
      if (this.requiredBy(m).length) return;
      choix.delete(m.code);
    } else {
      this.withDependencies(m.code, choix);
    }
    this.selected = choix;
  }

  private withDependencies(code: string, choix: Set<string>): void {
    if (choix.has(code)) return;
    const m = this.catalog.find(x => x.code === code);
    if (!m || m.coreModule) return;
    choix.add(code);
    m.dependencies.forEach(d => this.withDependencies(d, choix));
  }

  create(): void {
    if (this.adminForm.invalid || this.companyForm.invalid || this.creating) {
      this.adminForm.markAllAsTouched();
      return;
    }
    const c = this.companyForm.getRawValue();
    const a = this.adminForm.getRawValue();
    this.creating = true;
    this.identityWarning = null;
    this.logoWarning = null;
    this.service.onboard({
      name: c.name.trim(), slug: c.slug.trim(), plan: c.plan, modules: [...this.selected].sort(),
      admin: { email: a.email.trim(), firstName: a.firstName.trim() || null, lastName: a.lastName.trim() || null }
    }).pipe(
      switchMap(r => this.applyIdentity(r.tenant.id).pipe(map(() => r))),
      finalize(() => this.creating = false)
    ).subscribe({
      next: r => {
        this.result = r;
        this.wizard = false;
        this.clients = [r.tenant, ...this.clients];
      },
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@admin.clients.create-failed:La création du client a échoué.`),
        undefined, { duration: 6000 })
    });
  }

  /**
   * L'identité, puis le logo, une fois le client créé.
   *
   * <p>Un refus n'annule rien : le client existe, son compte administrateur
   * aussi, et ses identifiants doivent s'afficher. Faire échouer la création
   * entière parce qu'un logo a déplu obligerait à tout recommencer avec un
   * identifiant d'adresse déjà pris. L'écart est donc dit, pas caché.
   */
  private applyIdentity(tenantId: string): Observable<void> {
    const identite = this.identityPayload();
    const identite$ = identite
      ? this.service.saveIdentity(tenantId, identite).pipe(
          map(() => void 0),
          catchError((err: unknown) => {
            this.identityWarning = safeErrorMessage(
              err, $localize`:@@admin.clients.identity-failed:L'identité n'a pas été enregistrée : reprenez-la depuis la fiche du client.`);
            return of(void 0);
          }))
      : of(void 0);

    return identite$.pipe(switchMap(() => {
      const logo = this.logoFile;
      if (!logo) {
        return of(void 0);
      }
      return this.service.saveLogo(tenantId, logo).pipe(
        map(() => void 0),
        catchError((err: unknown) => {
          this.logoWarning = safeErrorMessage(
            err, $localize`:@@admin.clients.logo-failed:Le logo n'a pas été déposé : reprenez-le depuis la fiche du client.`);
          return of(void 0);
        }));
    }));
  }

  /** Ce qu'on envoie, ou rien du tout si l'étape a été passée sans y toucher. */
  private identityPayload(): TenantIdentityRequest | null {
    const v = this.identityForm.getRawValue();
    const net = (x: string): string | null => {
      const t = x.trim();
      return t.length ? t : null;
    };
    const corps: TenantIdentityRequest = {
      slogan: net(v.slogan), legalName: net(v.legalName), legalForm: net(v.legalForm),
      registrationNumber: net(v.registrationNumber), vatNumber: net(v.vatNumber),
      addressLine1: net(v.addressLine1), addressLine2: null,
      postalCode: net(v.postalCode), city: net(v.city),
      countryCode: net(v.countryCode)?.toUpperCase() ?? null,
      contactEmail: net(v.contactEmail), contactPhone: net(v.contactPhone),
      primaryColor: net(v.primaryColor), secondaryColor: net(v.secondaryColor),
      themeCode: v.themeCode
    };
    const renseigne = Object.entries(corps).some(([cle, val]) => cle !== 'themeCode' && val !== null);
    // Le thème par défaut tout seul ne vaut pas un appel : c'est déjà ce que
    // le serveur donne à un client qui n'a rien choisi.
    return renseigne || corps.themeCode !== 'AIRY_LIGHT' ? corps : null;
  }

  /** Le libellé d'un thème, pour ne pas montrer `HIGH_CONTRAST` à un humain. */
  themeLabel(theme: TenantTheme): string {
    switch (theme) {
      case 'DENSE_LIGHT': return $localize`:@@admin.clients.theme-dense:Clair dense — poste de travail`;
      case 'DARK': return $localize`:@@admin.clients.theme-dark:Sombre`;
      case 'HIGH_CONTRAST': return $localize`:@@admin.clients.theme-contrast:Contraste renforcé — atelier, extérieur`;
      default: return $localize`:@@admin.clients.theme-airy:Clair aéré — par défaut`;
    }
  }

  requiredText(m: ModuleCatalogEntry): string {
    return $localize`:@@admin.clients.required-by:Requis par ${this.requiredBy(m).join(', ')}:modules:`;
  }

  get failedModules(): number {
    return this.result ? this.result.modules.filter(m => !m.activated).length : 0;
  }

  moduleName(code: string): string {
    return this.catalog.find(m => m.code === code)?.name ?? code;
  }

  trackById(_i: number, c: { id: string }): string {
    return c.id;
  }

  trackByCode(_i: number, m: { code: string }): string {
    return m.code;
  }

  trackByFamily(_i: number, f: ModuleFamily): string {
    return f.category;
  }
}

export function familles(catalog: ModuleCatalogEntry[]): ModuleFamily[] {
  const parFamille = new Map<string, ModuleCatalogEntry[]>();
  for (const m of catalog) {
    parFamille.set(m.category, [...(parFamille.get(m.category) ?? []), m]);
  }
  return [...parFamille.entries()].map(([category, modules]) => ({
    category, modules: [...modules].sort((x, y) => x.name.localeCompare(y.name))
  }));
}

/** « Hôpital Saint-Jean » → « hopital-saint-jean » : un code d'adresse valable. */
export function slugify(name: string): string {
  return name.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 63).replace(/-+$/g, '');
}
