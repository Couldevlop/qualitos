import { Component } from '@angular/core';

interface MethodCard {
  title: string;
  description: string;
  icon: string;
  route?: string;
  available: boolean;
}

interface StatCard {
  label: string;
  value: string;
  icon: string;
  /** Accent sémantique de la pastille d'icône. */
  tone: 'accent' | 'success' | 'warn' | 'danger';
  /** Tendance optionnelle (libellé court + sens). */
  trend?: { label: string; dir: 'up' | 'down' };
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
 * Note : les valeurs de KPI affichées ici sont indicatives (vitrine d'accueil) ;
 * le détail réel et sourcé vit dans le tableau de bord et le catalogue KPI.
 */
@Component({
  selector: 'qos-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: false
})
export class HomeComponent {

  readonly heroEyebrow = $localize`:@@home.hero.eyebrow:Plateforme qualité totale`;
  readonly heroTitle = $localize`:@@home.welcome.title:Cinq méthodes. Un seul référentiel.`;

  /** Slogan de marque — inchangé dans toutes les langues, comme un nom propre. */
  readonly heroSlogan = $localize`:@@home.hero.slogan:One platform. Five methods. Every industry.`;

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

  readonly trendUp = $localize`:@@home.kpi.trend-up:en hausse`;
  readonly trendDown = $localize`:@@home.kpi.trend-down:en baisse`;

  readonly stats: StatCard[] = [
    { label: $localize`:@@home.kpi.open-pdca:Cycles PDCA en cours`, value: '12', icon: 'autorenew',
      tone: 'accent', trend: { label: this.trendUp, dir: 'up' } },
    { label: $localize`:@@home.kpi.open-nc:Non-conformités ouvertes`, value: '7', icon: 'report_problem',
      tone: 'warn', trend: { label: this.trendDown, dir: 'down' } },
    { label: $localize`:@@home.kpi.fives-score:Score 5S moyen`, value: '82/100', icon: 'check_circle',
      tone: 'success', trend: { label: this.trendUp, dir: 'up' } },
    { label: $localize`:@@home.kpi.audits-planned:Audits planifiés`, value: '4', icon: 'fact_check',
      tone: 'accent' }
  ];

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
