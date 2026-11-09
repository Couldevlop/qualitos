import { Component, OnInit } from '@angular/core';
import { Observable, combineLatest, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuditsService } from '../audits/audits.service';
import { CapaService } from '../capa/capa.service';
import { CapaCriticity, CapaStatus } from '../capa/capa.types';
import { FivesService } from '../fives/fives.service';
import { NcService } from '../nc/nc.service';

interface MethodCard {
  title: string;
  description: string;
  icon: string;
  route?: string;
  available: boolean;
}

interface StatCard {
  label: string;
  /** Valeur lue dans le module source ; `null` si elle n'a pas pu être lue. */
  value: string | null;
  icon: string;
  /** Accent sémantique de la pastille d'icône. */
  tone: 'accent' | 'success' | 'warn' | 'danger';
  /** Écran source de la valeur : la carte y mène, ce qui en donne la provenance. */
  route: string;
  /** Précision de périmètre affichée sous le libellé (ex. nombre d'audits pris en compte). */
  hint?: string;
}

interface QuickAction {
  label: string;
  icon: string;
  route: string;
}

/**
 * Accueil premium (Travail 3) — hero + 5 méthodes + KPI clés + actions
 * rapides + activité récente. Mise en scène sobre, beaucoup d'espace blanc,
 * tout via les tokens du design system.
 *
 * Les quatre indicateurs sont lus dans les modules eux-mêmes (réclamations
 * clients, CAPA majeures, audits 5S, plans d'audit) : aucun chiffre n'est écrit
 * en dur. Une valeur qui n'a pas pu être lue s'affiche « — », avec le lien vers
 * le module.
 *
 * Ce qu'un dirigeant regarde ici, c'est ce qui sort de l'organisation et ce qui
 * coûte cher : l'écart vu par le client, et les actions correctives dont la
 * criticité engage. Le compte des cycles PDCA et des écarts internes, lui, est
 * un volume d'activité : il appartient au tableau de bord de son module.
 */
@Component({
  selector: 'qos-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: false
})
export class HomeComponent implements OnInit {

  readonly heroEyebrow = $localize`:@@home.hero.eyebrow:Plateforme qualité totale`;
  readonly heroTitle = $localize`:@@home.welcome.title:Cinq méthodes. Un seul référentiel.`;

  /** Slogan de marque, dans la langue du lecteur. */
  readonly heroSlogan = $localize`:@@home.hero.slogan:Une plateforme. Cinq méthodes. Tous les secteurs.`;

  /**
   * Quatre preuves courtes plutôt qu'un paragraphe : ce que la plateforme tient,
   * lisible d'un coup d'œil, sans promesse invérifiable.
   */
  readonly heroProofs = [
    { icon: 'hub', label: $localize`:@@home.hero.proof-methods:5 méthodes fondamentales` },
    { icon: 'verified', label: $localize`:@@home.hero.proof-standards:60+ normes outillées` },
    { icon: 'psychology', label: $localize`:@@home.hero.proof-ai:IA explicable` },
    { icon: 'lock_clock', label: $localize`:@@home.hero.proof-proof:Preuves horodatées` }
  ];
  readonly heroSubtitle = $localize`:@@home.welcome.subtitle:QualitOS agrège les méthodes fondamentales de la qualité totale dans un référentiel unique, augmenté par l'IA et certifié par blockchain.`;

  readonly methods: MethodCard[] = [
    { title: 'PDCA', description: $localize`:@@home.card.pdca-desc:Roue de Deming — Plan / Do / Check / Act`,
      icon: 'autorenew', route: '/pdca', available: true },
    { title: 'Ishikawa', description: $localize`:@@home.card.ishikawa-desc:Diagramme cause-effet 6M/7M/8M`,
      icon: 'account_tree', route: '/ishikawa', available: true },
    { title: '5S', description: $localize`:@@home.card.fives-desc:Audit terrain Seiri / Seiton / Seiso / Seiketsu / Shitsuke`,
      icon: 'check_circle', route: '/fives', available: true },
    { title: 'DMAIC', description: $localize`:@@home.card.dmaic-desc:Six Sigma — Define / Measure / Analyze / Improve / Control`,
      icon: 'analytics', route: '/dmaic', available: true },
    { title: $localize`:@@home.card.circles-title:Cercles Qualité`, description: $localize`:@@home.card.circles-desc:Groupes, réunions, propositions, mesure d'impact`,
      icon: 'groups', route: '/circles', available: true }
  ];

  /** Statuts d'une CAPA encore à traiter : ni résolue, ni clôturée, ni rejetée. */
  static readonly OPEN_CAPA: readonly CapaStatus[] = ['OPEN', 'IN_PROGRESS'];
  /** Niveaux de criticité qui engagent : ceux que la Direction suit nommément. */
  static readonly MAJOR_CAPA: readonly CapaCriticity[] = ['HIGH', 'MEDIUM'];
  /** Audits 5S terminés pris en compte dans la moyenne affichée. */
  static readonly FIVES_SAMPLE = 100;

  stats$!: Observable<StatCard[]>;

  constructor(
    private readonly nc: NcService,
    private readonly capa: CapaService,
    private readonly fives: FivesService,
    private readonly audits: AuditsService
  ) {}

  ngOnInit(): void {
    // Chaque indicateur échoue seul : un module indisponible n'efface pas les autres.
    const openNcExternal$ = this.nc.statistics('EXTERNAL').pipe(
      map(s => String(s.open)),
      catchError(() => of(null))
    );
    // Le serveur ne filtre qu'un statut et qu'une criticité par appel : on
    // somme les quatre croisements plutôt que de compter côté client une page
    // tronquée. `size: 1` suffit, seul `totalElements` est lu.
    const combinaisons = HomeComponent.OPEN_CAPA.flatMap(
      status => HomeComponent.MAJOR_CAPA.map(criticity => ({ status, criticity }))
    );
    const openMajorCapa$ = forkJoin(
      combinaisons.map(c => this.capa.listCases(0, 1, c.status, c.criticity))
    ).pipe(
      map(pages => String(pages.reduce((sum, p) => sum + p.totalElements, 0))),
      catchError(() => of(null))
    );
    const fivesScore$ = this.fives.listAudits(0, HomeComponent.FIVES_SAMPLE, 'COMPLETED').pipe(
      map(page => {
        const scores = page.content
          .map(a => a.overallScore)
          .filter((v): v is number => typeof v === 'number');
        if (!scores.length) {
          return { value: null, count: 0 };
        }
        const mean = Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
        return { value: `${mean}/100`, count: scores.length };
      }),
      catchError(() => of({ value: null, count: 0 }))
    );
    const plannedAudits$ = this.audits.listPlans(0, 1, 'PLANNED').pipe(
      map(page => String(page.totalElements)),
      catchError(() => of(null))
    );

    this.stats$ = combineLatest([openNcExternal$, openMajorCapa$, fivesScore$, plannedAudits$]).pipe(
      map(([ncExterne, capaMajeures, fives, audits]) => [
        { label: $localize`:@@home.kpi.open-nc-external:Réclamations clients ouvertes`, value: ncExterne,
          icon: 'campaign', tone: 'danger', route: '/nc/externe' },
        { label: $localize`:@@home.kpi.open-capa-major:CAPA majeures ouvertes`, value: capaMajeures,
          icon: 'engineering', tone: 'warn', route: '/capa',
          hint: $localize`:@@home.kpi.capa-major-scope:Criticité haute et moyenne` },
        { label: $localize`:@@home.kpi.fives-score:Score 5S moyen`, value: fives.value,
          icon: 'check_circle', tone: 'success', route: '/fives',
          hint: fives.count
            ? $localize`:@@home.kpi.fives-sample:Sur ${fives.count}:count: audits terminés`
            : $localize`:@@home.kpi.fives-none:Aucun audit terminé`
        },
        { label: $localize`:@@home.kpi.audits-planned:Audits planifiés`, value: audits,
          icon: 'fact_check', tone: 'accent', route: '/audits' }
      ] as StatCard[])
    );
  }

  readonly quickActions: QuickAction[] = [
    { label: $localize`:@@home.actions.new-pdca:Créer un cycle PDCA`, icon: 'autorenew', route: '/pdca' },
    { label: $localize`:@@home.actions.new-audit:Lancer un audit 5S`, icon: 'check_circle', route: '/fives' },
    // Pas d'action « Analyser une cause » ici : une analyse de cause part d'un
    // écart constaté. On déclare la NC, et c'est sa fiche qui ouvre l'Ishikawa
    // ou les 5 Pourquoi sur son propre sujet. La carte de méthode ci-dessus
    // reste, elle, la porte de consultation des diagrammes existants.
    { label: $localize`:@@home.actions.new-nc:Déclarer une NC`, icon: 'report_problem', route: '/nc' }
  ];

  trackByTitle(_index: number, m: MethodCard): string { return m.title; }
  trackByLabel(_index: number, s: StatCard): string { return s.label; }
  trackByRoute(_index: number, a: QuickAction): string { return a.route; }
}
