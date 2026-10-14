import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mocClassificationLabel, mocDomainLabel, mocErrorMessage } from '../../../changes/moc.labels';
import { MocService } from '../../../changes/moc.service';
import {
  IMPACT_DOMAINS,
  ImpactDomain,
  MocClassification,
  MocHolder,
  MocSettings,
  REQUIRED_CLASSES
} from '../../../changes/moc.types';
import { TenantUser } from '../../admin.types';
import { TenantTeamService } from '../../tenant-team.service';

type RequiredClass = Exclude<MocClassification, 'NOT_ASSESSED'>;

/** Un rôle en cours d'édition ; la clé ne sert qu'à suivre la carte quand elle bouge. */
export interface EditableRole {
  key: number;
  id: string | null;
  label: string;
  requiredFor: RequiredClass[];
  holders: MocHolder[];
  /** La saisie de recherche dans l'annuaire, propre à chaque rôle. */
  search: string;
}

const LABEL_MAX = 100;
const MAX_ROLES = 20;
const MAX_HOLDERS = 20;

/**
 * « Paramètres MOC » (ADR 0087) — réservé à l'administrateur du client.
 *
 * <p>Seuils de classification, délai de rappel des validateurs, poids des huit
 * domaines, et les rôles de validation : leur libellé, leur ordre, les
 * classifications qui les exigent et leurs titulaires, choisis dans l'annuaire
 * du client (jamais saisis à la main). Rien ne part avant « Enregistrer ».
 *
 * <p>Ce qu'un enregistrement change est dit à l'écran : les demandes en
 * brouillon ou en analyse sont reclassées, celles déjà en validation gardent
 * leur classification et leurs validateurs.
 */
@Component({
  selector: 'qos-moc-settings',
  templateUrl: './moc-settings.component.html',
  styleUrls: ['./moc-settings.component.scss'],
  standalone: false
})
export class MocSettingsComponent implements OnInit {

  readonly domains = IMPACT_DOMAINS;
  readonly classes = REQUIRED_CLASSES;
  readonly labelMax = LABEL_MAX;
  readonly maxRoles = MAX_ROLES;

  majorThreshold = 25;
  criticalThreshold = 50;
  reminderDays = 3;
  weights = {} as Record<ImpactDomain, number>;
  roles: EditableRole[] = [];

  members: TenantUser[] = [];
  directoryFailed = false;

  loading = false;
  failed = false;
  saving = false;

  private saved = '';
  private nextKey = 0;

  constructor(
    private readonly service: MocService,
    private readonly team: TenantTeamService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
    this.team.list(0, 500).subscribe({
      next: page => this.members = page.content,
      error: () => this.directoryFailed = true
    });
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.settings().subscribe({
      next: s => {
        this.loading = false;
        this.reprendre(s);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err);
      }
    });
  }

  domainLabel = mocDomainLabel;
  classLabel = mocClassificationLabel;

  // ---------- rôles ----------

  addRole(): void {
    if (this.roles.length >= MAX_ROLES) return;
    this.roles.push({ key: this.nextKey++, id: null, label: '', requiredFor: ['CRITICAL'], holders: [], search: '' });
  }

  removeRole(index: number): void {
    this.roles.splice(index, 1);
  }

  move(index: number, delta: -1 | 1): void {
    const cible = index + delta;
    if (cible < 0 || cible >= this.roles.length) return;
    const [r] = this.roles.splice(index, 1);
    this.roles.splice(cible, 0, r);
  }

  isRequired(r: EditableRole, c: RequiredClass): boolean {
    return r.requiredFor.includes(c);
  }

  toggleRequired(r: EditableRole, c: RequiredClass, on: boolean): void {
    r.requiredFor = on ? [...new Set([...r.requiredFor, c])] : r.requiredFor.filter(x => x !== c);
  }

  /** Les membres de l'annuaire que la recherche désigne, hors titulaires déjà nommés. */
  candidates(r: EditableRole): TenantUser[] {
    const q = r.search.trim().toLowerCase();
    return this.members
      .filter(m => m.active)
      .filter(m => !r.holders.some(h => h.userId === m.keycloakId))
      .filter(m => !q || m.email.toLowerCase().includes(q))
      .slice(0, 8);
  }

  addHolder(r: EditableRole, m: TenantUser): void {
    if (r.holders.length >= MAX_HOLDERS || r.holders.some(h => h.userId === m.keycloakId)) return;
    r.holders = [...r.holders, { userId: m.keycloakId, label: m.email }];
    r.search = '';
  }

  removeHolder(r: EditableRole, h: MocHolder): void {
    r.holders = r.holders.filter(x => x.userId !== h.userId);
  }

  /** Un titulaire désactivé ou disparu de l'annuaire ne peut plus statuer : on le signale. */
  holderState(h: MocHolder): 'ok' | 'inactive' | 'unknown' {
    if (this.directoryFailed || !this.members.length) return 'ok';
    const m = this.members.find(x => x.keycloakId === h.userId);
    if (!m) return 'unknown';
    return m.active ? 'ok' : 'inactive';
  }

  removeHolderLabel(h: MocHolder): string {
    return $localize`:@@moc.settings.remove-holder:Retirer ${h.label}:member:`;
  }

  // ---------- validation ----------

  /** Ce qui empêche d'enregistrer, dit en clair ; vide si tout va bien. */
  get problems(): string[] {
    const p: string[] = [];
    const entier = (n: number, min: number, max: number) => Number.isInteger(n) && n >= min && n <= max;
    if (!entier(this.majorThreshold, 1, 99) || !entier(this.criticalThreshold, 2, 100)
        || this.majorThreshold >= this.criticalThreshold) {
      p.push($localize`:@@moc.settings.problem.thresholds:Les seuils doivent respecter 0 < seuil majeur < seuil critique ≤ 100.`);
    }
    if (!entier(this.reminderDays, 1, 30)) {
      p.push($localize`:@@moc.settings.problem.reminder:Le délai de rappel est un nombre de jours de 1 à 30.`);
    }
    const poids = this.domains.map(d => this.weights[d]);
    if (poids.some(w => !entier(w, 0, 10))) {
      p.push($localize`:@@moc.settings.problem.weights:Chaque poids est un entier de 0 à 10.`);
    } else if (poids.every(w => w === 0)) {
      p.push($localize`:@@moc.settings.problem.weights-zero:Au moins un domaine doit avoir un poids supérieur à 0.`);
    }
    if (!this.roles.length) {
      p.push($localize`:@@moc.settings.problem.no-role:Il faut au moins un rôle de validation.`);
    }
    const libelles = this.roles.map(r => r.label.trim().toLowerCase());
    if (libelles.some(l => !l || l.length > LABEL_MAX)) {
      p.push($localize`:@@moc.settings.problem.label:Chaque rôle porte un libellé de 1 à 100 caractères.`);
    } else if (new Set(libelles).size !== libelles.length) {
      p.push($localize`:@@moc.settings.problem.duplicate:Deux rôles portent le même libellé.`);
    }
    for (const c of this.classes) {
      if (!this.roles.some(r => r.requiredFor.includes(c))) {
        const classe = this.classLabel(c);
        p.push($localize`:@@moc.settings.problem.required:La classification « ${classe}:classe: » n'exige aucun rôle.`);
      }
    }
    return p;
  }

  /** Un rôle requis sans titulaire bloquera toute soumission de cette classe : avertissement, pas blocage. */
  warnNoHolder(r: EditableRole): boolean {
    return r.requiredFor.length > 0 && !r.holders.length;
  }

  /** Résumé lisible des seuils, comme sur la maquette. */
  get thresholdSummary(): string {
    const a = this.majorThreshold;
    const b = this.criticalThreshold;
    return $localize`:@@moc.settings.threshold-summary:Mineur < ${a}:major: ≤ Majeur < ${b}:critical: ≤ Critique`;
  }

  // ---------- enregistrement ----------

  get dirty(): boolean {
    return JSON.stringify(this.payload()) !== this.saved;
  }

  discard(): void {
    this.reprendre(JSON.parse(this.saved) as MocSettings);
  }

  save(): void {
    if (!this.dirty || this.problems.length || this.saving) return;
    this.saving = true;
    this.service.saveSettings(this.payload()).subscribe({
      next: s => {
        this.saving = false;
        this.reprendre(s);
        this.snack.open($localize`:@@moc.settings.saved:Paramètres MOC enregistrés. Les demandes en brouillon ou en analyse sont reclassées.`,
          undefined, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err);
      }
    });
  }

  private payload(): MocSettings {
    const weights = {} as Record<ImpactDomain, number>;
    for (const d of this.domains) weights[d] = this.weights[d];
    return {
      majorThreshold: this.majorThreshold,
      criticalThreshold: this.criticalThreshold,
      reminderDays: this.reminderDays,
      weights,
      roles: this.roles.map(r => ({
        id: r.id, label: r.label.trim(),
        requiredFor: this.classes.filter(c => r.requiredFor.includes(c)),
        holders: r.holders.map(h => ({ userId: h.userId, label: h.label }))
      }))
    };
  }

  private reprendre(s: MocSettings): void {
    this.majorThreshold = s.majorThreshold;
    this.criticalThreshold = s.criticalThreshold;
    this.reminderDays = s.reminderDays;
    this.weights = { ...s.weights };
    this.roles = s.roles.map(r => ({
      key: this.nextKey++, id: r.id, label: r.label, requiredFor: [...r.requiredFor],
      holders: r.holders.map(h => ({ ...h })), search: ''
    }));
    this.saved = JSON.stringify(this.payload());
  }

  trackByKey(_i: number, r: EditableRole): number {
    return r.key;
  }

  private echouer(err: unknown): void {
    this.snack.open(mocErrorMessage(err, $localize`:@@moc.settings.failed:Les paramètres MOC n'ont pas pu être enregistrés.`),
      undefined, { duration: 6000 });
  }
}
