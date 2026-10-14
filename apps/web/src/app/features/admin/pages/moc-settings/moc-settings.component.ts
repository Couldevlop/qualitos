import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { personName } from '../../../../core/directory/directory-names';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { UserPickerComponent } from '../../../../shared/user-picker/user-picker.component';

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

type RequiredClass = Exclude<MocClassification, 'NOT_ASSESSED'>;

/** Un rôle en cours d'édition ; la clé ne sert qu'à suivre la carte quand elle bouge. */
export interface EditableRole {
  key: number;
  id: string | null;
  label: string;
  requiredFor: RequiredClass[];
  holders: MocHolder[];
}

/** Ce que l'annuaire dit d'un titulaire : membre actif, ancien membre, ou inconnu. */
export type HolderState = 'ok' | 'inactive' | 'unknown';

const LABEL_MAX = 100;
const MAX_ROLES = 20;
const MAX_HOLDERS = 20;

/**
 * « Paramètres MOC » (ADR 0087) — réservé à l'administrateur du client.
 *
 * <p>Seuils de classification, délai de rappel des validateurs, poids des huit
 * domaines, et les rôles de validation : leur libellé, leur ordre, les
 * classifications qui les exigent et leurs titulaires, choisis dans l'annuaire
 * du client en lecture restreinte (ADR 0091) — jamais saisis à la main. Le nom
 * de l'annuaire est recopié, avec l'adresse qui sert au courriel des
 * validateurs. Rien ne part avant « Enregistrer ».
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

  /** L'état de chaque titulaire dans l'annuaire, par compte. */
  private readonly holderStates = new Map<string, HolderState>();
  private readonly holderIdsCache = new WeakMap<MocHolder[], string[]>();

  loading = false;
  failed = false;
  saving = false;

  private saved = '';
  private nextKey = 0;

  constructor(
    private readonly service: MocService,
    private readonly directory: DirectoryService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
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
    this.roles.push({ key: this.nextKey++, id: null, label: '', requiredFor: ['CRITICAL'], holders: [] });
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

  /**
   * Les comptes déjà titulaires du rôle : le sélecteur ne les propose plus.
   * Même tableau tant que les titulaires ne changent pas (ils sont remplacés, jamais
   * modifiés en place) : un nouveau tableau à chaque passe ferait lever NG0100.
   */
  holderIds(r: EditableRole): string[] {
    let ids = this.holderIdsCache.get(r.holders);
    if (!ids) {
      ids = r.holders.map(h => h.userId);
      this.holderIdsCache.set(r.holders, ids);
    }
    return ids;
  }

  /**
   * La personne choisie dans le sélecteur devient titulaire : son nom (jamais
   * l'adresse quand un nom existe) et son adresse sont recopiés. Le sélecteur
   * est ensuite vidé, prêt pour le suivant.
   */
  addHolder(r: EditableRole, user: DirectoryUser | null, picker?: UserPickerComponent): void {
    if (!user) return;
    picker?.writeValue(null);
    if (r.holders.length >= MAX_HOLDERS || r.holders.some(h => h.userId === user.accountId)) return;
    r.holders = [...r.holders, { userId: user.accountId, label: personName(user), email: user.email }];
    this.holderStates.set(user.accountId, user.active ? 'ok' : 'inactive');
  }

  removeHolder(r: EditableRole, h: MocHolder): void {
    r.holders = r.holders.filter(x => x.userId !== h.userId);
  }

  /**
   * Un titulaire désactivé ou disparu de l'annuaire ne peut plus statuer : on
   * le signale. Tant que l'annuaire n'a pas répondu — ou s'il est indisponible —,
   * rien n'est signalé à tort.
   */
  holderState(h: MocHolder): HolderState {
    return this.holderStates.get(h.userId) ?? 'ok';
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
        holders: r.holders.map(h => ({ userId: h.userId, label: h.label, email: h.email ?? null }))
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
      holders: r.holders.map(h => ({ ...h }))
    }));
    this.saved = JSON.stringify(this.payload());
    this.verifierTitulaires();
  }

  /** L'état de chaque titulaire, lu dans l'annuaire (une lecture par compte, gardée en cache). */
  private verifierTitulaires(): void {
    const comptes = new Set(this.roles.flatMap(r => r.holders.map(h => h.userId)));
    for (const id of comptes) {
      if (this.holderStates.has(id)) continue;
      this.directory.getByAccount(id).subscribe({
        next: u => this.holderStates.set(id, u.active ? 'ok' : 'inactive'),
        // 404 : la personne n'est pas (ou plus) dans l'annuaire du client. Toute
        // autre erreur (réseau) ne dit rien d'elle : on ne signale rien.
        error: err => { if (err?.status === 404) this.holderStates.set(id, 'unknown'); }
      });
    }
  }

  trackByKey(_i: number, r: EditableRole): number {
    return r.key;
  }

  private echouer(err: unknown): void {
    this.snack.open(mocErrorMessage(err, $localize`:@@moc.settings.failed:Les paramètres MOC n'ont pas pu être enregistrés.`),
      undefined, { duration: 6000 });
  }
}
