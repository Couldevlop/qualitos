import { HttpResponse } from '@angular/common/http';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import {
  ANSWER_TYPE_LABEL,
  BRANCH_TEXT,
  SECTION_TEXT,
  SectionText,
  TOPIC_LABEL,
  actionStatusLabel,
  answerLabel
} from '../../resolution/dmaic-resolution.labels';
import { DmaicResolutionService } from '../../resolution/dmaic-resolution.service';
import {
  ACTION_STATUSES,
  ANSWER_CHOICES,
  ActionLine,
  AnswerCode,
  DEFINE_TOPICS,
  DefineTopic,
  PHOTO_TYPES,
  RESOLUTION_LIMITS,
  ResolutionContent,
  ResolutionSheet,
  SHEET_SECTIONS,
  SheetPhoto,
  SheetQuestion,
  SheetSection,
  WHY_BRANCHES,
  WhyBranch,
  WhyChain,
  emptyContent
} from '../../resolution/dmaic-resolution.types';

/** Une entrée du sommaire : une ancre de la fiche et son avancement. */
interface NavEntry {
  id: string;
  letter: string;
  label: string;
  done: number;
  total: number;
}

interface TopicGroup {
  topic: DefineTopic | null;
  label: string | null;
  questions: SheetQuestion[];
}

interface IshikawaOption {
  id: string;
  label: string;
}

/**
 * La fiche de résolution d'un projet DMAIC (ADR 0101) : le classeur client à
 * l'écran, d'un seul tenant. Réclamation, questionnaire de chaque phase et sa
 * synthèse, 5 pourquoi, plan d'actions, photos ; un sommaire collant mène à
 * chaque partie en un clic.
 *
 * <p>La fiche se tient en brouillon et s'enregistre d'un bloc. Les photos, elles,
 * partent tout de suite : un fichier n'attend pas un bouton. Le rapport client
 * reprend la fiche ENREGISTRÉE, d'où son bouton éteint tant qu'il reste des
 * changements.
 *
 * <p>Sans l'action `dmaic.manage`, la fiche se lit sans se modifier.
 */
@Component({
  selector: 'qos-dmaic-resolution',
  templateUrl: './dmaic-resolution.component.html',
  styleUrls: ['./dmaic-resolution.component.scss'],
  standalone: false
})
export class DmaicResolutionComponent implements OnInit, OnDestroy {

  @Input({ required: true }) projectId!: string;

  readonly limits = RESOLUTION_LIMITS;
  readonly branches = WHY_BRANCHES;
  readonly actionStatuses = ACTION_STATUSES;
  readonly photoSlots = Array.from({ length: RESOLUTION_LIMITS.photoSlots }, (_, i) => i + 1);
  readonly photoAccept = PHOTO_TYPES.join(',');
  readonly branchText = BRANCH_TEXT;
  readonly answerTypeLabel = ANSWER_TYPE_LABEL;
  readonly answerLabel = answerLabel;
  readonly actionStatusLabel = actionStatusLabel;
  readonly reportDirtyHint = $localize`:@@dmaic.res.report-dirty:Enregistrez la fiche : le rapport reprend ce qui est enregistré.`;

  sheet: ResolutionSheet | null = null;
  draft: ResolutionContent = emptyContent();
  sections: SheetSection[] = [];
  ishikawas: IshikawaOption[] = [];
  /** Les remarques des photos en cours de frappe, par emplacement. */
  remarks: Record<number, string> = {};
  teamIds: string[] = [];
  readonly teamPicker = new FormControl<string | null>(null);

  loading = false;
  failed = false;
  saving = false;
  downloading = false;
  canEdit = false;
  readonly photoBusy = new Set<number>();

  private savedJson = '';
  private readonly subs = new Subscription();

  constructor(
    private readonly svc: DmaicResolutionService,
    private readonly ishikawa: IshikawaService,
    private readonly authz: AuthzService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.subs.add(this.authz.can('dmaic.manage').subscribe(ok => {
      this.canEdit = ok;
      if (ok) this.teamPicker.enable(); else this.teamPicker.disable();
    }));
    this.subs.add(this.ishikawa.listDiagrams(0, 50).pipe(
      map(page => page.content.map(d => ({ id: d.id, label: d.problemStatement }))),
      catchError(() => of([] as IshikawaOption[]))
    ).subscribe(list => (this.ishikawas = list)));
    this.load();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.failed = false;
    this.svc.sheet(this.projectId).subscribe({
      next: s => {
        this.loading = false;
        this.take(s);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.fail(err, $localize`:@@dmaic.res.load-failed:La fiche de résolution n'a pas pu être chargée.`);
      }
    });
  }

  // ---------- Lecture du paramétrage ----------

  sectionText(s: SheetSection): SectionText { return SECTION_TEXT[s]; }

  questionsOf(section: SheetSection): SheetQuestion[] {
    return this.sheet?.settings.questions.filter(q => q.section === section) ?? [];
  }

  /** « Définir » se range en rubriques Quoi, Qui… ; les autres phases forment un seul groupe. */
  groupsOf(section: SheetSection): TopicGroup[] {
    const questions = this.questionsOf(section);
    if (section !== 'DEFINE') return questions.length ? [{ topic: null, label: null, questions }] : [];
    const groups: TopicGroup[] = DEFINE_TOPICS
      .map(t => ({ topic: t as DefineTopic | null, label: TOPIC_LABEL[t] as string | null, questions: questions.filter(q => q.topic === t) }))
      .filter(g => g.questions.length);
    const loose = questions.filter(q => !q.topic);
    return loose.length ? [...groups, { topic: null, label: null, questions: loose }] : groups;
  }

  choicesOf(q: SheetQuestion): readonly AnswerCode[] { return ANSWER_CHOICES[q.answerType]; }

  summaryOf(section: SheetSection): string | null {
    switch (section) {
      case 'MEASURE': return this.draft.measureSummary;
      case 'CONTAIN': return this.draft.containmentMeasures;
      case 'ANALYZE': return this.draft.rootCause;
      case 'IMPROVE': return this.draft.retainedSolutions;
      case 'CONTROL': return this.draft.surveillancePlan;
      default: return null;
    }
  }

  setSummary(section: SheetSection, value: string): void {
    switch (section) {
      case 'MEASURE': this.draft.measureSummary = value; break;
      case 'CONTAIN': this.draft.containmentMeasures = value; break;
      case 'ANALYZE': this.draft.rootCause = value; break;
      case 'IMPROVE': this.draft.retainedSolutions = value; break;
      case 'CONTROL': this.draft.surveillancePlan = value; break;
    }
  }

  get severities() { return this.sheet?.settings.severities ?? []; }
  get failureTypes() { return this.sheet?.settings.failureTypes ?? []; }

  /** Le sommaire, avec l'avancement de chaque partie : ce qui est rempli sur ce qui est demandé. */
  get nav(): NavEntry[] {
    const d = this.draft;
    const filled = (v: unknown) => v !== null && v !== undefined && String(v).trim() !== '';
    const claim = [d.clientName, d.claimNumber, d.problemOwner, d.openedOn, d.severity, d.failureTypeCode, d.customerSymptoms];
    const entries: NavEntry[] = [{
      id: 'res-claim', letter: '#', label: $localize`:@@dmaic.res.claim:Réclamation`,
      done: claim.filter(filled).length + (d.team.length ? 1 : 0), total: claim.length + 1
    }];
    for (const s of this.sections) {
      const qs = this.questionsOf(s);
      const hasSummary = SECTION_TEXT[s].summary !== null;
      entries.push({
        id: 'res-' + s.toLowerCase(), letter: SECTION_TEXT[s].letter, label: SECTION_TEXT[s].title,
        done: qs.filter(q => filled(d.answers[q.code])).length + (hasSummary && filled(this.summaryOf(s)) ? 1 : 0),
        total: qs.length + (hasSummary ? 1 : 0)
      });
      if (s === 'ANALYZE') {
        entries.push({
          id: 'res-whys', letter: '5', label: $localize`:@@dmaic.res.whys:5 pourquoi`,
          done: WHY_BRANCHES.filter(b => this.chainsOf(b).length).length, total: WHY_BRANCHES.length
        });
      }
      if (s === 'IMPROVE') {
        entries.push({
          id: 'res-actions', letter: '✓', label: $localize`:@@dmaic.res.actions:Plan d'actions`,
          done: d.actions.filter(a => a.status === 'CLOSED').length, total: d.actions.length
        });
      }
    }
    entries.push({
      id: 'res-photos', letter: '▣', label: $localize`:@@dmaic.res.photos:Photos`,
      done: this.sheet?.photos.length ?? 0, total: RESOLUTION_LIMITS.photoSlots
    });
    return entries;
  }

  goTo(id: string): void {
    const reduce = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    document.getElementById(id)?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' });
  }

  // ---------- Réponses ----------

  answerOf(code: string): string { return this.draft.answers[code] ?? ''; }

  setAnswer(code: string, value: string | null): void {
    const answers = { ...this.draft.answers };
    if (value === null || value.trim() === '') delete answers[code]; else answers[code] = value;
    this.draft.answers = answers;
  }

  /** Recliquer le choix retenu l'efface : une réponse fermée se retire comme elle se donne. */
  toggleAnswer(code: string, choice: AnswerCode): void {
    if (!this.canEdit) return;
    this.setAnswer(code, this.draft.answers[code] === choice ? null : choice);
  }

  // ---------- Personnes ----------

  onOwner(user: DirectoryUser | null): void {
    this.draft.problemOwner = user ? { accountId: user.accountId, name: user.displayName } : null;
  }

  addTeam(user: DirectoryUser | null): void {
    if (!user) return;
    if (!this.teamIds.includes(user.accountId) && this.draft.team.length < RESOLUTION_LIMITS.team) {
      this.draft.team = [...this.draft.team, { accountId: user.accountId, name: user.displayName }];
      this.syncTeamIds();
    }
    queueMicrotask(() => this.teamPicker.setValue(null));
  }

  removeTeam(accountId: string): void {
    this.draft.team = this.draft.team.filter(p => p.accountId !== accountId);
    this.syncTeamIds();
  }

  onResponsible(line: ActionLine, user: DirectoryUser | null): void {
    line.responsible = user ? { accountId: user.accountId, name: user.displayName } : null;
  }

  // ---------- 5 pourquoi ----------

  chainsOf(branch: WhyBranch): WhyChain[] { return this.draft.whys.filter(c => c.branch === branch); }

  get canAddChain(): boolean { return this.draft.whys.length < RESOLUTION_LIMITS.chains; }

  addChain(branch: WhyBranch): void {
    if (!this.canAddChain) return;
    this.draft.whys = [...this.draft.whys, { branch, cause: null, whys: [''], rootCause: false, action: null }];
  }

  removeChain(chain: WhyChain): void { this.draft.whys = this.draft.whys.filter(c => c !== chain); }

  addWhy(chain: WhyChain): void {
    if (chain.whys.length < RESOLUTION_LIMITS.whysPerChain) chain.whys = [...chain.whys, ''];
  }

  removeWhy(chain: WhyChain, index: number): void { chain.whys = chain.whys.filter((_, i) => i !== index); }

  setWhy(chain: WhyChain, index: number, value: string): void {
    chain.whys = chain.whys.map((w, i) => (i === index ? value : w));
  }

  // ---------- Plan d'actions ----------

  get canAddAction(): boolean { return this.draft.actions.length < RESOLUTION_LIMITS.actions; }

  addAction(): void {
    if (!this.canAddAction) return;
    this.draft.actions = [...this.draft.actions, {
      createdOn: new Date().toISOString().slice(0, 10), action: null, responsible: null, status: 'OPEN',
      plannedOn: null, doneOn: null, percent: 0, notes: null
    }];
  }

  removeAction(line: ActionLine): void { this.draft.actions = this.draft.actions.filter(a => a !== line); }

  // ---------- Brouillon ----------

  get dirty(): boolean {
    return !!this.sheet && JSON.stringify(clean(this.draft)) !== this.savedJson;
  }

  /** Ce qui empêche d'enregistrer, dit en clair ; vide si tout va bien. */
  get problems(): string[] {
    const p: string[] = [];
    const d = this.draft;
    if (d.openedOn && d.closedOn && d.closedOn < d.openedOn) {
      p.push($localize`:@@dmaic.res.problem.dates:La date de fermeture ne peut pas précéder la date de création.`);
    }
    if (d.actions.some(a => a.percent === null || a.percent < 0 || a.percent > 100 || !Number.isInteger(a.percent))) {
      p.push($localize`:@@dmaic.res.problem.percent:L'avancement d'une action est un nombre entier de 0 à 100.`);
    }
    if (d.actions.some(a => a.plannedOn && a.createdOn && a.plannedOn < a.createdOn)) {
      p.push($localize`:@@dmaic.res.problem.planned:Une action ne peut pas être prévue avant sa date de création.`);
    }
    return p;
  }

  discard(): void {
    if (this.sheet) this.take(this.sheet);
  }

  save(): void {
    if (!this.canEdit || !this.dirty || this.problems.length || this.saving) return;
    this.saving = true;
    this.svc.save(this.projectId, clean(this.draft)).subscribe({
      next: s => {
        this.saving = false;
        this.take(s);
        this.snack.open($localize`:@@dmaic.res.saved:Fiche de résolution enregistrée.`, undefined, { duration: 3000 });
      },
      error: err => {
        this.saving = false;
        this.fail(err, $localize`:@@dmaic.res.save-failed:La fiche n'a pas pu être enregistrée.`);
      }
    });
  }

  // ---------- Photos ----------

  photoAlt(slot: number): string { return $localize`:@@dmaic.res.photo.alt:Photo ${slot}:slot:`; }

  photoAt(slot: number): SheetPhoto | undefined { return this.sheet?.photos.find(p => p.slot === slot); }

  onFile(slot: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.canEdit) return;
    if (!PHOTO_TYPES.includes(file.type)) {
      this.snack.open($localize`:@@dmaic.res.photo.bad-type:Format refusé : JPEG, PNG, GIF ou WEBP uniquement.`, undefined, { duration: 5000 });
      return;
    }
    if (file.size > RESOLUTION_LIMITS.photoBytes) {
      this.snack.open($localize`:@@dmaic.res.photo.too-big:La photo dépasse 10 Mo.`, undefined, { duration: 5000 });
      return;
    }
    this.photoBusy.add(slot);
    this.svc.uploadPhoto(this.projectId, slot, file, this.photoAt(slot)?.remark ?? null).subscribe({
      next: photo => {
        this.photoBusy.delete(slot);
        this.putPhoto(photo);
      },
      error: err => {
        this.photoBusy.delete(slot);
        this.fail(err, err?.status === 503
          ? $localize`:@@dmaic.res.photo.storage-off:Le stockage des photos n'est pas disponible sur cette installation.`
          : $localize`:@@dmaic.res.photo.upload-failed:La photo n'a pas pu être versée.`);
      }
    });
  }

  saveRemark(slot: number): void {
    const photo = this.photoAt(slot);
    const typed = (this.remarks[slot] ?? '').trim();
    if (!photo || !this.canEdit || typed === (photo.remark ?? '')) return;
    this.photoBusy.add(slot);
    this.svc.updateRemark(this.projectId, slot, typed || null).subscribe({
      next: updated => {
        this.photoBusy.delete(slot);
        this.putPhoto(updated);
      },
      error: err => {
        this.photoBusy.delete(slot);
        this.remarks[slot] = photo.remark ?? '';
        this.fail(err, $localize`:@@dmaic.res.photo.remark-failed:La remarque n'a pas pu être enregistrée.`);
      }
    });
  }

  removePhoto(slot: number): void {
    if (!this.canEdit) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dmaic.res.photo.delete-title:Retirer la photo ?`,
        message: $localize`:@@dmaic.res.photo.delete-message:La photo et sa remarque seront retirées de la fiche.`,
        confirmLabel: $localize`:@@dmaic.res.photo.delete:Retirer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.photoBusy.add(slot);
      this.svc.deletePhoto(this.projectId, slot).subscribe({
        next: () => {
          this.photoBusy.delete(slot);
          if (this.sheet) this.sheet = { ...this.sheet, photos: this.sheet.photos.filter(p => p.slot !== slot) };
          delete this.remarks[slot];
        },
        error: err => {
          this.photoBusy.delete(slot);
          this.fail(err, $localize`:@@dmaic.res.photo.delete-failed:La photo n'a pas pu être retirée.`);
        }
      });
    });
  }

  // ---------- Rapport client ----------

  downloadReport(): void {
    if (this.downloading || this.dirty) return;
    this.downloading = true;
    this.svc.claimReport(this.projectId).subscribe({
      next: response => {
        this.downloading = false;
        if (!response.body) {
          this.snack.open($localize`:@@dmaic.res.report-failed:Le rapport n'a pas pu être produit.`, undefined, { duration: 5000 });
          return;
        }
        saveAs(response.body, fileName(response) ?? 'rapport-client-dmaic.pdf');
      },
      error: err => {
        this.downloading = false;
        this.fail(err, $localize`:@@dmaic.res.report-failed:Le rapport n'a pas pu être produit.`);
      }
    });
  }

  trackByIndex(index: number): number { return index; }

  private take(s: ResolutionSheet): void {
    this.sheet = s;
    this.draft = withEditableWhys(structuredClone(s.content));
    this.savedJson = JSON.stringify(clean(s.content));
    this.sections = SHEET_SECTIONS.filter(sec => s.settings.questions.some(q => q.section === sec)
      || SECTION_TEXT[sec].summary !== null);
    this.remarks = Object.fromEntries(s.photos.map(p => [p.slot, p.remark ?? '']));
    this.syncTeamIds();
  }

  private putPhoto(photo: SheetPhoto): void {
    if (!this.sheet) return;
    const photos = [...this.sheet.photos.filter(p => p.slot !== photo.slot), photo].sort((a, b) => a.slot - b.slot);
    this.sheet = { ...this.sheet, photos };
    this.remarks[photo.slot] = photo.remark ?? '';
  }

  private syncTeamIds(): void {
    this.teamIds = this.draft.team.map(p => p.accountId);
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}

/** Une chaîne sans pourquoi s'ouvre avec un champ vide, prêt à la frappe. */
function withEditableWhys(c: ResolutionContent): ResolutionContent {
  c.whys = c.whys.map(w => ({ ...w, whys: w.whys.length ? w.whys : [''] }));
  return c;
}

const text = (v: string | null | undefined): string | null => {
  const t = (v ?? '').trim();
  return t === '' ? null : t;
};

/**
 * La fiche telle qu'elle part au serveur : textes rognés, vides devenus nuls,
 * pourquoi blancs retirés. Sert aussi à dire si le brouillon diffère de ce qui
 * est enregistré, sans qu'un espace en trop compte pour un changement.
 */
export function clean(c: ResolutionContent): ResolutionContent {
  const answers: Record<string, string> = {};
  for (const [code, value] of Object.entries(c.answers ?? {})) {
    const t = text(value);
    if (t !== null) answers[code] = t;
  }
  return {
    clientName: text(c.clientName),
    claimNumber: text(c.claimNumber),
    problemOwner: c.problemOwner ?? null,
    team: [...(c.team ?? [])],
    openedOn: text(c.openedOn),
    closedOn: text(c.closedOn),
    severity: c.severity ?? null,
    failureTypeCode: text(c.failureTypeCode),
    customerSymptoms: text(c.customerSymptoms),
    answers: Object.fromEntries(Object.entries(answers).sort(([a], [b]) => a.localeCompare(b))),
    measureSummary: text(c.measureSummary),
    containmentMeasures: text(c.containmentMeasures),
    rootCause: text(c.rootCause),
    retainedSolutions: text(c.retainedSolutions),
    surveillancePlan: text(c.surveillancePlan),
    whys: (c.whys ?? []).map(w => ({
      branch: w.branch,
      cause: text(w.cause),
      whys: w.whys.map(x => text(x)).filter((x): x is string => x !== null),
      rootCause: !!w.rootCause,
      action: text(w.action)
    })),
    actions: (c.actions ?? []).map(a => ({
      createdOn: text(a.createdOn),
      action: text(a.action),
      responsible: a.responsible ?? null,
      status: a.status,
      plannedOn: text(a.plannedOn),
      doneOn: text(a.doneOn),
      percent: a.percent ?? 0,
      notes: text(a.notes)
    })),
    ishikawaId: text(c.ishikawaId)
  };
}

function fileName(response: HttpResponse<Blob>): string | null {
  const header = response.headers.get('Content-Disposition');
  const found = header ? /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(header) : null;
  return found ? decodeURIComponent(found[1]) : null;
}

function saveAs(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}
