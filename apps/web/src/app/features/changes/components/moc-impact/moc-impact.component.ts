import { Component, ElementRef, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mocClassificationLabel, mocDomainLabel, mocErrorMessage, mocLevelLabel } from '../../moc.labels';
import { MocService } from '../../moc.service';
import { IMPACT_LEVELS, ImpactDomain, ImpactLevel, MocDetail, MocImpact } from '../../moc.types';

/**
 * L'analyse d'impact : huit domaines, un niveau chacun (boutons segmentés,
 * groupe de boutons radio au clavier) et la nature de l'impact avec ses mesures
 * de maîtrise. En tête : la classification, la barre de score et « 54 / 100 ».
 *
 * <p>Un niveau choisi s'enregistre aussitôt ; un texte s'enregistre en quittant
 * le champ (et avec le niveau s'il n'en avait pas). Le score, la classification
 * et le verrou de soumission viennent du serveur, jamais d'un calcul local.
 */
@Component({
  selector: 'qos-moc-impact',
  templateUrl: './moc-impact.component.html',
  styleUrls: ['./moc-impact.component.scss'],
  standalone: false
})
export class MocImpactComponent implements OnChanges {

  @Input({ required: true }) detail!: MocDetail;
  @Output() readonly changed = new EventEmitter<MocDetail>();

  readonly levels = IMPACT_LEVELS;
  readonly notesMax = 2000;

  /** Les textes en cours de saisie, par domaine — non écrasés par un rechargement. */
  notes: Partial<Record<ImpactDomain, string>> = {};
  private readonly dirty = new Set<ImpactDomain>();
  private readonly saving = new Set<ImpactDomain>();

  constructor(
    private readonly service: MocService,
    private readonly snack: MatSnackBar,
    private readonly host: ElementRef<HTMLElement>
  ) {}

  ngOnChanges(): void {
    for (const i of this.detail.impacts) {
      if (!this.dirty.has(i.domain)) {
        this.notes[i.domain] = i.notes ?? '';
      }
    }
  }

  get editable(): boolean {
    return this.detail.canManage && this.detail.status === 'IN_ANALYSIS';
  }

  get evaluated(): number {
    return this.detail.impacts.filter(i => !!i.level).length;
  }

  get scoreText(): string {
    return this.detail.score === null ? '—' : String(this.detail.score);
  }

  isSaving(d: ImpactDomain): boolean {
    return this.saving.has(d);
  }

  domainLabel = mocDomainLabel;
  levelLabel = mocLevelLabel;
  classificationLabel = mocClassificationLabel;

  /** Le bouton qui reçoit le focus de tabulation : le niveau choisi, sinon le premier. */
  tabIndex(i: MocImpact, l: ImpactLevel): number {
    return (i.level ?? 'NONE') === l ? 0 : -1;
  }

  setLevel(i: MocImpact, level: ImpactLevel): void {
    if (!this.editable || this.saving.has(i.domain)) return;
    if (level === i.level && !this.dirty.has(i.domain)) return;
    this.save(i.domain, level);
  }

  /** Flèches, Début et Fin parcourent le groupe et choisissent, comme un groupe radio natif. */
  onKey(event: KeyboardEvent, i: MocImpact, index: number): void {
    const last = this.levels.length - 1;
    let cible: number;
    switch (event.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        cible = index === last ? 0 : index + 1;
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        cible = index === 0 ? last : index - 1;
        break;
      case 'Home':
        cible = 0;
        break;
      case 'End':
        cible = last;
        break;
      default:
        return;
    }
    event.preventDefault();
    const level = this.levels[cible];
    this.host.nativeElement.querySelector<HTMLButtonElement>(`[data-radio="${i.domain}-${level}"]`)?.focus();
    this.setLevel(i, level);
  }

  noteInput(d: ImpactDomain, value: string): void {
    this.notes[d] = value;
    this.dirty.add(d);
  }

  /** En quittant le champ : enregistré avec le niveau ; sans niveau, gardé pour le premier choix. */
  saveNotes(i: MocImpact): void {
    if (!this.dirty.has(i.domain) || !i.level || !this.editable) return;
    this.save(i.domain, i.level);
  }

  private save(domain: ImpactDomain, level: ImpactLevel): void {
    this.saving.add(domain);
    const texte = (this.notes[domain] ?? '').trim();
    this.service.assess(this.detail.id, domain, { level, notes: texte || null }).subscribe({
      next: d => {
        this.saving.delete(domain);
        this.dirty.delete(domain);
        this.changed.emit(d);
      },
      error: err => {
        this.saving.delete(domain);
        this.snack.open(mocErrorMessage(err, $localize`:@@moc.impact-failed:L'évaluation n'a pas été enregistrée.`),
          undefined, { duration: 5000 });
      }
    });
  }

  trackByDomain(_i: number, x: MocImpact): string {
    return x.domain;
  }
}
