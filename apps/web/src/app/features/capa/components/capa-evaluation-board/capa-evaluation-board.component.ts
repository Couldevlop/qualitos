import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CapaService } from '../../capa.service';
import {
  EffectivenessVerdict,
  EvaluationBoard,
  EvaluationRow,
  EvaluationStage
} from '../../capa.types';

/**
 * Le plan de travail des évaluations d'efficacité (ADR 0108).
 *
 * <p>L'onglet voisin MESURE l'efficacité : il compte les récidives après la
 * clôture et dit ce que le terrain a fait de l'action. Cet écran-ci la JUGE :
 * il confronte un relevé au critère posé à l'avance et rend un verdict qui
 * décide de la suite du dossier. Les deux sont complémentaires, et le second ne
 * remplace pas le premier — c'est pourquoi ils vivent sur le même écran.
 *
 * <p>Le verdict a trois états et non deux. Une action qui a réduit le problème
 * sans le supprimer n'est ni efficace ni inefficace ; forcer le choix obligeait
 * à clore un dossier qui méritait une action de plus, ou à rouvrir un dossier
 * qui progressait. « Partiellement » est l'état qui manquait.
 *
 * <p>Le tableau n'offre AUCUN filtre de statut, et c'est voulu : le tri par
 * urgence le remplace. Ce qui est à juger arrive en tête, et personne n'a à
 * savoir quel statut demander pour l'obtenir.
 */
@Component({
  selector: 'qos-capa-evaluation-board',
  templateUrl: './capa-evaluation-board.component.html',
  styleUrls: ['./capa-evaluation-board.component.scss'],
  standalone: false
})
export class CapaEvaluationBoardComponent implements OnInit {

  readonly columns = ['reference', 'stage', 'actions', 'verdict'];

  /** L'ordre des trois choix va du meilleur au pire, comme on les lit. */
  readonly verdicts: EffectivenessVerdict[] = ['EFFECTIVE', 'PARTIAL', 'INEFFECTIVE'];

  board?: EvaluationBoard;
  loading = false;
  saving = false;

  /**
   * Le dossier à ouvrir d'emblée, quand on arrive d'une fiche CAPA par son
   * renvoi « Évaluer contre le critère » : le lecteur y venait pour juger CE
   * dossier, et le lui faire rechercher dans le tableau était un pas de trop.
   */
  @Input() focusCapaId?: string;

  /** La ligne ouverte dans le panneau de droite. */
  selected?: EvaluationRow;

  /**
   * Le formulaire du panneau, vidé à chaque changement de ligne.
   *
   * <p>Réactif et non `ngModel` : c'est `ReactiveFormsModule` que le module
   * partagé réexporte, et mélanger les deux familles dans un même écran oblige
   * chaque lecteur suivant à vérifier laquelle est en jeu.
   *
   * <p>Le verdict vit hors du formulaire : trois boutons à état pressé disent
   * mieux qu'un champ que l'un des trois est retenu, et leur couleur annonce ce
   * que chacun déclenche.
   */
  verdict?: EffectivenessVerdict;
  readonly form: FormGroup;

  constructor(
    private readonly service: CapaService,
    private readonly snack: MatSnackBar,
    forms: FormBuilder
  ) {
    this.form = forms.group({
      baseline: ['', Validators.maxLength(120)],
      target: ['', Validators.maxLength(120)],
      // Obligatoire : un verdict sans mesure n'est pas un jugement. La borne
      // de 120 est celle de la colonne — refusée, jamais tronquée en silence.
      observed: ['', [Validators.required, Validators.maxLength(120)]],
      comment: ['', Validators.maxLength(4000)]
    });
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : le flux de
    // démonstration est quasi synchrone, et une micro-tâche rallumerait le
    // voyant sur un tableau déjà affiché.
    this.loading = true;
    this.service.evaluationBoard().subscribe({
      next: board => {
        this.board = board;
        this.loading = false;
        this.resync();
        this.openFocus();
      },
      error: () => {
        this.loading = false;
        this.snack.open(
          $localize`:@@capa.evaluation.load-failed:Impossible de charger les évaluations d'efficacité.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  /**
   * Après un rechargement, rouvre la même ligne — à jour.
   *
   * <p>Garder l'objet d'avant afficherait l'ancienne étape et l'ancien verdict
   * dans le panneau, juste à côté du tableau qui, lui, montre les nouveaux :
   * deux réponses à la même question sur le même écran.
   */
  private resync(): void {
    if (!this.selected) return;
    const again = this.board?.rows.find(r => r.capaId === this.selected?.capaId);
    this.selected = again;
    if (!again) this.resetForm();
  }

  /**
   * Ouvre le dossier demandé par l'adresse, une seule fois : au rechargement
   * suivant, c'est le choix de l'utilisateur qui prime, pas l'URL d'arrivée.
   */
  private openFocus(): void {
    if (!this.focusCapaId || this.selected) return;
    const row = this.board?.rows.find(r => r.capaId === this.focusCapaId);
    if (row) this.select(row);
  }

  select(row: EvaluationRow): void {
    if (this.selected?.capaId === row.capaId) return;
    this.selected = row;
    // Les repères d'une évaluation précédente pré-remplissent le formulaire :
    // on rejuge contre les mêmes bornes, et les ressaisir de mémoire est le
    // meilleur moyen de les changer sans le vouloir. Le relevé constaté, lui,
    // repart toujours vide — c'est la mesure du jour.
    this.resetForm({
      baseline: row.lastReview?.baseline ?? '',
      target: row.lastReview?.target ?? ''
    });
  }

  private resetForm(seed: { baseline: string; target: string } = { baseline: '', target: '' }): void {
    this.verdict = undefined;
    this.form.reset({ ...seed, observed: '', comment: '' });
  }

  choose(verdict: EffectivenessVerdict): void {
    this.verdict = verdict;
  }

  /**
   * Le formulaire est-il complet ?
   *
   * <p>Un verdict ET un relevé. Le serveur refuse l'un sans l'autre — un verdict
   * sans mesure n'est pas un jugement mais une opinion — et le bouton le dit
   * avant le clic plutôt que de faire découvrir le refus après.
   */
  get complete(): boolean {
    return !!this.verdict && this.form.valid
      && String(this.form.value.observed ?? '').trim().length > 0;
  }

  submit(): void {
    const row = this.selected;
    if (!row || !this.verdict || !this.complete || !this.canJudge || this.saving) return;

    const v = this.form.value;
    this.saving = true;
    this.service.recordEffectiveness(row.capaId, {
      verdict: this.verdict,
      baseline: this.trimmed(v.baseline),
      target: this.trimmed(v.target),
      observed: String(v.observed ?? '').trim(),
      comment: this.trimmed(v.comment)
    }).subscribe({
      next: () => {
        this.saving = false;
        this.snack.open(this.confirmation(), $localize`:@@common.ok:OK`, { duration: 5000 });
        this.resetForm();
        this.reload();
      },
      error: () => {
        this.saving = false;
        this.snack.open(
          $localize`:@@capa.evaluation.save-failed:L'évaluation n'a pas pu être enregistrée.`,
          $localize`:@@common.ok:OK`, { duration: 5000 });
      }
    });
  }

  /** Un champ laissé vide part absent, jamais comme une chaîne vide. */
  private trimmed(value: unknown): string | undefined {
    const clean = String(value ?? '').trim();
    return clean.length > 0 ? clean : undefined;
  }

  /**
   * Ce que le verdict déclenche, dit en clair après l'enregistrement.
   *
   * <p>Le message nomme la SUITE et non l'acte : « enregistré » laisserait
   * l'utilisateur découvrir plus tard que son dossier s'est rouvert.
   */
  private confirmation(): string {
    switch (this.verdict) {
      case 'EFFECTIVE':
        return $localize`:@@capa.evaluation.done-effective:Dossier clôturé : l'action est jugée efficace.`;
      case 'PARTIAL':
        return $localize`:@@capa.evaluation.done-partial:Efficacité partielle : une action complémentaire est attendue sur le dossier.`;
      default:
        return $localize`:@@capa.evaluation.done-ineffective:Dossier rouvert : l'analyse des causes est à reprendre.`;
    }
  }

  // ---- lecture ---------------------------------------------------------------

  /** La couleur de la pastille d'étape, alignée sur le sens du mot. */
  stageClass(stage: EvaluationStage): string {
    switch (stage) {
      case 'CLOSED_EFFECTIVE': return 'tone-ok';
      case 'AWAITING_EVALUATION': return 'tone-info';
      case 'COMPLEMENTARY_ACTION_EXPECTED': return 'tone-warn';
      case 'REOPENED': return 'tone-bad';
      default: return 'tone-idle';
    }
  }

  verdictClass(verdict: EffectivenessVerdict): string {
    switch (verdict) {
      case 'EFFECTIVE': return 'tone-ok';
      case 'PARTIAL': return 'tone-warn';
      default: return 'tone-bad';
    }
  }

  /** Combien d'actions restent à mener sur la ligne ouverte. */
  get remaining(): number {
    const row = this.selected;
    return row ? Math.max(0, row.actionsTotal - row.actionsDone) : 0;
  }

  /**
   * Pourquoi le panneau d'évaluation est-il fermé sur cette ligne ?
   *
   * <p>Un code, pas une phrase : le gabarit en fait une phrase traduisible, et
   * un motif renvoyé ici en français ne se traduirait jamais.
   */
  get lockReason(): 'DONE' | 'ACTIONS' | 'NO_CRITERION' | 'JUDGED' | undefined {
    const row = this.selected;
    if (!row || this.canJudge) return undefined;
    if (row.stage === 'CLOSED_EFFECTIVE') return 'DONE';
    if (row.actionsTotal === 0 || row.actionsDone < row.actionsTotal) return 'ACTIONS';
    if (!row.criterion) return 'NO_CRITERION';
    return 'JUDGED';
  }

  /**
   * Le panneau d'évaluation est-il réellement ouvert sur la ligne choisie ?
   *
   * <p>Deux conditions, et PAS seulement `evaluable`. Le serveur ouvre
   * l'évaluation dès que les actions sont faites, mais il refuse le verdict si
   * aucun critère n'a été posé — deux règles distinctes, et le drapeau ne porte
   * que la première. S'en tenir à lui offrirait un formulaire que le serveur
   * refuserait à l'enregistrement, ce qui est la pire façon de l'apprendre.
   */
  get canJudge(): boolean {
    const row = this.selected;
    return !!row && row.evaluable && !!row.criterion;
  }

  /**
   * Le critère manque-t-il alors que tout le reste est prêt ?
   *
   * <p>Ce cas mérite son propre message : il ne se répare pas sur cet écran. Le
   * critère se pose sur la fiche du dossier, et il doit l'être AVANT le
   * jugement — sinon on choisit le critère en regardant le résultat.
   */
  get criterionMissing(): boolean {
    const row = this.selected;
    return !!row && !row.criterion;
  }
}
