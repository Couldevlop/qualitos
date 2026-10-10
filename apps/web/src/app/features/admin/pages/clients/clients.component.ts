import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ModuleCatalogEntry, OnboardResponse, TenantPlan, TenantSummary } from '../../admin.types';
import { ClientsService } from '../../clients.service';
import { TenantModulesService } from '../../tenant-modules.service';

/** Les étapes de l'assistant, dans l'ordre : c'est une vraie séquence. */
export type WizardStep = 1 | 2 | 3;

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

  clients: TenantSummary[] = [];
  loading = false;
  failed = false;

  wizard = false;
  step: WizardStep = 1;
  families: ModuleFamily[] = [];
  catalogFailed = false;
  selected = new Set<string>();
  creating = false;
  result: OnboardResponse | null = null;

  private catalog: ModuleCatalogEntry[] = [];

  readonly companyForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$/)]],
    plan: ['STARTER' as TenantPlan, Validators.required]
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

  // ---------- assistant ----------

  openWizard(): void {
    this.wizard = true;
    this.step = 1;
    this.result = null;
    this.selected = new Set();
    this.companyForm.reset();
    this.adminForm.reset();
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
      this.step = 3;
    }
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
    this.service.onboard({
      name: c.name.trim(), slug: c.slug.trim(), plan: c.plan, modules: [...this.selected].sort(),
      admin: { email: a.email.trim(), firstName: a.firstName.trim() || null, lastName: a.lastName.trim() || null }
    }).pipe(finalize(() => this.creating = false)).subscribe({
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
