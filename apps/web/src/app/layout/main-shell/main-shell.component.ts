import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
  badge?: string;
}

export interface NavSection {
  label: string;
  items: NavItem[];
  /** Section repliable (groupes de conformité GRC), repliée par défaut. */
  collapsible?: boolean;
}

@Component({
  selector: 'qos-main-shell',
  templateUrl: './main-shell.component.html',
  styleUrls: ['./main-shell.component.scss'],
  standalone: false
})
export class MainShellComponent implements OnInit {

  user$!: Observable<AuthUser | null>;
  collapsed = false;

  /** Libellés a11y du bouton repli (binding dynamique → $localize côté TS). */
  readonly expandNavLabel = $localize`:@@shell.expand-nav.aria:Déplier la navigation`;
  readonly collapseNavLabel = $localize`:@@shell.collapse-nav.aria:Replier la navigation`;

  /** Labels des sections repliées (persistées). */
  collapsedSections = new Set<string>();

  private static readonly COLLAPSED_KEY = 'qos.nav.collapsed';

  // Refonte navigation (Travail 1) — 6 groupes lisibles inspirés des leaders QMS
  // (MasterControl, ETQ, Veeva, Qualio). Objectif : pas de scroll pour l'usage
  // courant, charge cognitive maîtrisée (règle 7±2 par groupe), et la masse
  // GRC (19 routes) repliée derrière une seule entrée → page hub /compliance.
  // Les groupes sont repliables, l'état est persisté en localStorage.
  readonly sections: NavSection[] = [
    {
      label: $localize`:@@nav.pilotage:Pilotage`,
      items: [
        { label: $localize`:@@nav.accueil:Accueil`,                route: '/home',              icon: 'home' },
        { label: $localize`:@@nav.tableau-de-bord:Tableau de bord`, route: '/dashboard',         icon: 'dashboard' },
        { label: $localize`:@@nav.mes-dashboards:Mes dashboards`,   route: '/dashboard-builder', icon: 'dashboard_customize' },
        { label: $localize`:@@nav.tv-mode:Mode TV / Salle qualité`, route: '/tv',                icon: 'tv' },
        { label: $localize`:@@nav.indicateurs-kpi:Indicateurs (KPI)`, route: '/kpis',            icon: 'monitoring' }
      ]
    },
    {
      label: $localize`:@@nav.methodes-qualite:Méthodes qualité`,
      items: [
        { label: $localize`:@@nav.pdca:PDCA`,         route: '/pdca',     icon: 'autorenew' },
        { label: $localize`:@@nav.ishikawa:Ishikawa`, route: '/ishikawa', icon: 'account_tree' },
        { label: $localize`:@@nav.5s:5S`,             route: '/fives',    icon: 'check_circle' },
        { label: $localize`:@@nav.dmaic:DMAIC`,       route: '/dmaic',    icon: 'analytics' },
        { label: $localize`:@@nav.cercles:Cercles`,   route: '/circles',  icon: 'groups' },
        { label: $localize`:@@nav.workflow-designer:Designer de workflow`, route: '/workflow-designer', icon: 'schema' }
      ]
    },
    {
      label: $localize`:@@nav.analyses-ia:Analyses IA`,
      items: [
        { label: $localize`:@@nav.spc:SPC`,                       route: '/spc',            icon: 'show_chart' },
        { label: $localize`:@@nav.anomaly:Anomalies IA`,          route: '/anomaly',        icon: 'bubble_chart' },
        { label: $localize`:@@nav.forecast:Prévision KPI`,        route: '/forecast',       icon: 'trending_up' },
        { label: $localize`:@@nav.nc-clusters:Clustering NC`,     route: '/nc-clusters',    icon: 'hub' },
        { label: $localize`:@@nav.complaints-nlp:Réclamations IA`, route: '/complaints-nlp', icon: 'rate_review' },
        { label: $localize`:@@nav.assistant-ia:Assistant IA`,     route: '/nlq',            icon: 'forum' },
        { label: $localize`:@@nav.nlq-explore:Exploration NLQ`,   route: '/nlq-explore',    icon: 'query_stats' },
        { label: $localize`:@@nav.storyboard:Storyboard IA`,      route: '/storyboard',     icon: 'auto_stories' }
      ]
    },
    {
      label: $localize`:@@nav.operations:Opérations`,
      items: [
        { label: $localize`:@@nav.non-conformites:Non-conformités`, route: '/nc',        icon: 'report_problem' },
        { label: $localize`:@@nav.capa:CAPA`,                       route: '/capa',      icon: 'engineering' },
        { label: $localize`:@@nav.audits:Audits`,                   route: '/audits',    icon: 'fact_check' },
        { label: $localize`:@@nav.risques-fmea:Risques (FMEA)`,     route: '/fmea',      icon: 'warning' },
        { label: $localize`:@@nav.documents:Documents`,             route: '/documents', icon: 'description' },
        { label: $localize`:@@nav.changements:Changements`,         route: '/changes',   icon: 'change_circle' },
        { label: $localize`:@@nav.ehs:EHS`,                         route: '/ehs',       icon: 'health_and_safety' }
      ]
    },
    {
      label: $localize`:@@nav.referentiels:Référentiels`,
      items: [
        { label: $localize`:@@nav.standards-hub:Standards Hub`,     route: '/standards',      icon: 'workspace_premium' },
        { label: $localize`:@@nav.doc-gen-ia:Génération doc IA`,    route: '/standards-doc-gen', icon: 'auto_awesome' },
        { label: $localize`:@@nav.packs-sectoriels:Packs sectoriels`, route: '/industry-packs', icon: 'category' },
        { label: $localize`:@@nav.fournisseurs:Fournisseurs`,      route: '/suppliers',      icon: 'local_shipping' },
        { label: $localize`:@@nav.formation:Formation`,            route: '/training',       icon: 'school' },
        { label: $localize`:@@nav.academy:Academy`,                route: '/academy',        icon: 'cast_for_education' },
        { label: $localize`:@@nav.mon-apprentissage:Mon apprentissage`, route: '/learning',  icon: 'military_tech' },
        { label: $localize`:@@nav.integrations:Intégrations`,      route: '/itsm',           icon: 'hub' }
      ]
    },
    {
      label: $localize`:@@nav.conformite-grc:Conformité (GRC)`,
      items: [
        { label: $localize`:@@nav.conformite-hub:Conformité`, route: '/compliance', icon: 'verified_user' }
      ]
    }
  ];

  /** État réseau + file offline (§15.2-15.3) — chip de synchro dans la topbar. */
  online$!: Observable<boolean>;
  pendingSync$!: Observable<number>;

  constructor(
    private readonly auth: AuthService,
    private readonly connectivity: ConnectivityService,
    private readonly offlineQueue: OfflineQueueService
  ) {}

  ngOnInit(): void {
    this.user$ = this.auth.user();
    this.online$ = this.connectivity.online$;
    this.pendingSync$ = this.offlineQueue.pendingCount$;
    this.restoreCollapsedSections();
  }

  toggle(): void { this.collapsed = !this.collapsed; }

  isSectionCollapsed(label: string): boolean {
    return this.collapsedSections.has(label);
  }

  toggleSection(label: string): void {
    if (this.collapsedSections.has(label)) {
      this.collapsedSections.delete(label);
    } else {
      this.collapsedSections.add(label);
    }
    this.persistCollapsedSections();
  }

  /** Restaure l'état replié depuis localStorage ; par défaut, replie les sections GRC. */
  private restoreCollapsedSections(): void {
    try {
      const raw = localStorage.getItem(MainShellComponent.COLLAPSED_KEY);
      if (raw) {
        this.collapsedSections = new Set<string>(JSON.parse(raw) as string[]);
        return;
      }
    } catch {
      // localStorage indisponible / JSON corrompu → on retombe sur le défaut
    }
    this.sections
      .filter(s => s.collapsible)
      .forEach(s => this.collapsedSections.add(s.label));
  }

  private persistCollapsedSections(): void {
    try {
      localStorage.setItem(
        MainShellComponent.COLLAPSED_KEY,
        JSON.stringify([...this.collapsedSections]));
    } catch {
      // best-effort : pas de persistance si localStorage indisponible
    }
  }

  initials(name?: string | null): string {
    if (!name) return '··';
    return name.trim().split(/\s+/).slice(0, 2).map(p => p[0] ?? '').join('').toUpperCase();
  }
}
