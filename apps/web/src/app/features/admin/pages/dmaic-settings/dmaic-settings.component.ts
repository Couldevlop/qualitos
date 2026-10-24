import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { ANSWER_TYPE_LABEL, SECTION_TEXT, TOPIC_LABEL } from '../../../dmaic/resolution/dmaic-resolution.labels';
import { DmaicResolutionService } from '../../../dmaic/resolution/dmaic-resolution.service';
import {
  ANSWER_TYPES,
  DEFINE_TOPICS,
  FailureType,
  RESOLUTION_LIMITS,
  ResolutionSettings,
  SHEET_SECTIONS,
  SeverityLevel,
  SheetQuestion,
  SheetSection
} from '../../../dmaic/resolution/dmaic-resolution.types';

/** La forme d'un code, comme le serveur (`ResolutionSettings.CODE`), une fois mis en capitales. */
const CODE = /^[A-Z0-9][A-Z0-9._-]{0,19}$/;

/** Le préfixe proposé aux nouvelles questions de chaque phase, comme les codes du classeur. */
const PREFIX: Readonly<Record<SheetSection, string>> = {
  DEFINE: 'DEF', MEASURE: 'MES', CONTAIN: 'CON', ANALYZE: 'ANA', IMPROVE: 'AME', CONTROL: 'CTL'
};

/**
 * « Paramètres DMAIC » (ADR 0101), réservé à l'administrateur du client : le
 * questionnaire de la fiche de résolution, les types de défaillance et les
 * sévérités. Sans réglage, la plateforme propose les valeurs du classeur.
 *
 * <p>Le paramétrage s'enregistre d'un bloc. Une question garde son CODE : en
 * changer le libellé ne détache pas les réponses déjà données ; la retirer les
 * cache sans les effacer.
 */
@Component({
  selector: 'qos-dmaic-settings',
  templateUrl: './dmaic-settings.component.html',
  styleUrls: ['./dmaic-settings.component.scss'],
  standalone: false
})
export class DmaicSettingsComponent implements OnInit {

  readonly sections = SHEET_SECTIONS;
  readonly topics = DEFINE_TOPICS;
  readonly answerTypes = ANSWER_TYPES;
  readonly answerTypeLabel = ANSWER_TYPE_LABEL;
  readonly topicLabel = TOPIC_LABEL;
  readonly limits = RESOLUTION_LIMITS;

  /** La phase dont on édite les questions : une à la fois, pour une page qui reste lisible. */
  section: SheetSection = 'DEFINE';
  questions: SheetQuestion[] = [];
  failureTypes: FailureType[] = [];
  severities: SeverityLevel[] = [];
  defaults = true;

  loading = false;
  failed = false;
  saving = false;

  private savedJson = '';

  constructor(
    private readonly service: DmaicResolutionService,
    private readonly dialog: MatDialog,
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
        this.echouer(err, $localize`:@@dmaic.settings.load-failed:Les paramètres DMAIC n'ont pas pu être chargés.`);
      }
    });
  }

  sectionTitle(s: SheetSection): string { return SECTION_TEXT[s].title; }

  countOf(s: SheetSection): number { return this.questions.filter(q => q.section === s).length; }

  get sectionQuestions(): SheetQuestion[] { return this.questions.filter(q => q.section === this.section); }

  // ---------- Questions ----------

  addQuestion(): void {
    if (this.questions.length >= RESOLUTION_LIMITS.questions) return;
    this.questions = [...this.questions, {
      code: this.nextCode(this.section), section: this.section,
      topic: this.section === 'DEFINE' ? 'WHAT' : null, label: '', answerType: 'TEXT'
    }];
  }

  removeQuestion(q: SheetQuestion): void { this.questions = this.questions.filter(x => x !== q); }

  /** Monte ou descend une question parmi celles de sa phase : c'est l'ordre de la fiche. */
  moveQuestion(q: SheetQuestion, delta: -1 | 1): void {
    const same = this.questions.filter(x => x.section === q.section);
    const target = same[same.indexOf(q) + delta];
    if (!target) return;
    const list = [...this.questions];
    const a = list.indexOf(q);
    const b = list.indexOf(target);
    [list[a], list[b]] = [list[b], list[a]];
    this.questions = list;
  }

  // ---------- Types de défaillance et sévérités ----------

  addFailureType(): void {
    if (this.failureTypes.length < RESOLUTION_LIMITS.failureTypes) {
      this.failureTypes = [...this.failureTypes, { code: '', label: '' }];
    }
  }

  removeFailureType(f: FailureType): void { this.failureTypes = this.failureTypes.filter(x => x !== f); }

  addSeverity(): void {
    if (this.severities.length >= RESOLUTION_LIMITS.severities) return;
    const next = Math.max(0, ...this.severities.map(s => s.level)) + 1;
    this.severities = [...this.severities, { level: Math.min(next, RESOLUTION_LIMITS.severities), label: '' }];
  }

  removeSeverity(s: SeverityLevel): void { this.severities = this.severities.filter(x => x !== s); }

  // ---------- Enregistrement ----------

  get dirty(): boolean {
    return !!this.savedJson && JSON.stringify(this.payload()) !== this.savedJson;
  }

  /** Ce qui empêche d'enregistrer, dit en clair ; vide si tout va bien. */
  get problems(): string[] {
    const p: string[] = [];
    const { questions, failureTypes, severities } = this.payload();
    const codesOk = (codes: string[]) => codes.every(c => CODE.test(c));
    const unique = (codes: (string | number)[]) => new Set(codes).size === codes.length;

    if (!questions.length) {
      p.push($localize`:@@dmaic.settings.problem.no-question:Gardez au moins une question.`);
    }
    if (!codesOk(questions.map(q => q.code)) || !codesOk(failureTypes.map(f => f.code))) {
      p.push($localize`:@@dmaic.settings.problem.code:Un code fait 1 à 20 caractères : lettres, chiffres, point, tiret ou soulignement.`);
    }
    if (!unique(questions.map(q => q.code))) {
      p.push($localize`:@@dmaic.settings.problem.question-dup:Deux questions portent le même code.`);
    }
    if (!unique(failureTypes.map(f => f.code))) {
      p.push($localize`:@@dmaic.settings.problem.failure-dup:Deux types de défaillance portent le même code.`);
    }
    if (questions.some(q => !q.label) || failureTypes.some(f => !f.label) || severities.some(s => !s.label)) {
      p.push($localize`:@@dmaic.settings.problem.label:Chaque ligne a besoin d'un libellé.`);
    }
    if (!severities.length) {
      p.push($localize`:@@dmaic.settings.problem.no-severity:Gardez au moins un niveau de sévérité.`);
    } else if (severities.some(s => !Number.isInteger(s.level) || s.level < 1 || s.level > RESOLUTION_LIMITS.severities)
      || !unique(severities.map(s => s.level))) {
      p.push($localize`:@@dmaic.settings.problem.level:Chaque sévérité a son propre niveau, de 1 à 10.`);
    }
    return p;
  }

  discard(): void {
    if (this.savedJson) this.reprendre({ ...JSON.parse(this.savedJson), defaults: this.defaults });
  }

  save(): void {
    if (!this.dirty || this.problems.length || this.saving) return;
    this.saving = true;
    const { questions, failureTypes, severities } = this.payload();
    this.service.replaceSettings(questions, failureTypes, severities).subscribe({
      next: s => {
        this.saving = false;
        this.reprendre(s);
        this.snack.open($localize`:@@dmaic.settings.saved:Paramètres DMAIC enregistrés.`, undefined, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@dmaic.settings.save-failed:Les paramètres DMAIC n'ont pas pu être enregistrés.`);
      }
    });
  }

  reset(): void {
    if (this.saving) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dmaic.settings.reset-title:Revenir aux valeurs de la plateforme ?`,
        message: $localize`:@@dmaic.settings.reset-message:Le questionnaire, les types de défaillance et les sévérités reprennent ceux du classeur d'origine. Les réponses déjà données restent en base.`,
        confirmLabel: $localize`:@@dmaic.settings.reset:Rétablir`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.saving = true;
      this.service.resetSettings().subscribe({
        next: s => {
          this.saving = false;
          this.reprendre(s);
          this.snack.open($localize`:@@dmaic.settings.reset-done:Valeurs de la plateforme rétablies.`, undefined, { duration: 4000 });
        },
        error: err => {
          this.saving = false;
          this.echouer(err, $localize`:@@dmaic.settings.save-failed:Les paramètres DMAIC n'ont pas pu être enregistrés.`);
        }
      });
    });
  }

  trackByIndex(index: number): number { return index; }

  /** Le paramétrage tel qu'il part au serveur : textes rognés, codes en capitales, rubrique hors « Définir » effacée. */
  private payload(): { questions: SheetQuestion[]; failureTypes: FailureType[]; severities: SeverityLevel[] } {
    return {
      questions: this.questions.map(q => ({
        code: (q.code ?? '').trim().toUpperCase(), section: q.section,
        topic: q.section === 'DEFINE' ? q.topic : null, label: (q.label ?? '').trim(), answerType: q.answerType
      })),
      failureTypes: this.failureTypes.map(f => ({ code: (f.code ?? '').trim().toUpperCase(), label: (f.label ?? '').trim() })),
      severities: this.severities.map(s => ({ level: s.level, label: (s.label ?? '').trim() }))
    };
  }

  private nextCode(section: SheetSection): string {
    const prefix = PREFIX[section] + '-';
    const used = new Set(this.questions.map(q => q.code.toUpperCase()));
    let n = this.questions.filter(q => q.section === section).length + 1;
    while (used.has(prefix + String(n).padStart(2, '0'))) n++;
    return prefix + String(n).padStart(2, '0');
  }

  private reprendre(s: ResolutionSettings): void {
    this.questions = s.questions.map(q => ({ ...q }));
    this.failureTypes = s.failureTypes.map(f => ({ ...f }));
    this.severities = s.severities.map(v => ({ ...v }));
    this.defaults = s.defaults;
    this.savedJson = JSON.stringify(this.payload());
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}
