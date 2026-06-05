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

  /** Labels des sections repliées (persistées). */
  collapsedSections = new Set<string>();

  private static readonly COLLAPSED_KEY = 'qos.nav.collapsed';

  // Architecture d'information par domaine (Qualité vs Gouvernance/Conformité GRC),
  // inspirée des leaders QMS (MasterControl, ETQ, Veeva, Qualio). Les 3 blocs de
  // conformité sont repliables pour éviter la surcharge cognitive (règle 7±2).
  readonly sections: NavSection[] = [
    {
      label: 'Pilotage',
      items: [
        { label: 'Tableau de bord', route: '/dashboard',         icon: 'dashboard' },
        { label: 'Accueil',         route: '/home',              icon: 'home' },
        { label: 'Mes dashboards',  route: '/dashboard-builder', icon: 'dashboard_customize' },
        { label: 'Indicateurs (KPI)', route: '/kpis',            icon: 'monitoring' },
        { label: 'Assistant IA',    route: '/nlq',               icon: 'forum' }
      ]
    },
    {
      label: 'Méthodes qualité',
      items: [
        { label: 'PDCA',     route: '/pdca',     icon: 'autorenew' },
        { label: 'Ishikawa', route: '/ishikawa', icon: 'account_tree' },
        { label: '5S',       route: '/fives',    icon: 'check_circle' },
        { label: 'DMAIC',    route: '/dmaic',    icon: 'analytics' },
        { label: 'SPC',      route: '/spc',      icon: 'show_chart' },
        { label: 'Cercles',  route: '/circles',  icon: 'groups' }
      ]
    },
    {
      label: 'Qualité opérationnelle',
      items: [
        { label: 'CAPA',          route: '/capa',      icon: 'engineering' },
        { label: 'Audits',        route: '/audits',    icon: 'fact_check' },
        { label: 'Risques (FMEA)', route: '/fmea',     icon: 'warning' },
        { label: 'Documents',     route: '/documents', icon: 'description' },
        { label: 'Changements',   route: '/changes',   icon: 'change_circle' },
        { label: 'EHS',           route: '/ehs',       icon: 'health_and_safety' }
      ]
    },
    {
      label: 'Fournisseurs & compétences',
      items: [
        { label: 'Fournisseurs', route: '/suppliers', icon: 'local_shipping' },
        { label: 'Formation',    route: '/training',  icon: 'school' }
      ]
    },
    {
      label: 'Normes & certification',
      items: [
        { label: 'Standards Hub', route: '/standards', icon: 'workspace_premium' }
      ]
    },
    {
      label: 'Conformité — IA (AI Act)',
      collapsible: true,
      items: [
        { label: 'QMS',        route: '/ai-qms',        icon: 'memory' },
        { label: 'Conformité', route: '/ai-conformity', icon: 'verified_user' },
        { label: 'Incidents',  route: '/ai-incidents',  icon: 'warning' },
        { label: 'EUDB',       route: '/ai-eudb',       icon: 'storage' },
        { label: 'FRIA',       route: '/fria',          icon: 'balance' },
        { label: 'PMM',        route: '/ai-pmm',        icon: 'monitoring' }
      ]
    },
    {
      label: 'Conformité — Données (RGPD)',
      collapsible: true,
      items: [
        { label: 'Registre (RoPA)',   route: '/ropa',                 icon: 'shield' },
        { label: 'Consentements',     route: '/consents',             icon: 'how_to_reg' },
        { label: 'Demandes (DSAR)',   route: '/subject-requests',     icon: 'gavel' },
        { label: 'Mentions',          route: '/privacy-notices',      icon: 'article' },
        { label: 'DPIA',              route: '/dpia',                 icon: 'assessment' },
        { label: 'DPO',               route: '/dpo-appointments',     icon: 'badge' },
        { label: 'Rétention',         route: '/retention',            icon: 'auto_delete' },
        { label: 'Transferts',        route: '/cross-border',         icon: 'public' },
        { label: 'Sous-traitants (DPA)', route: '/processor-agreements', icon: 'handshake' },
        { label: 'Violations',        route: '/breaches',             icon: 'privacy_tip' },
        { label: 'Décisions auto.',   route: '/automated-decisions',  icon: 'account_tree' }
      ]
    },
    {
      label: 'Conformité — Cyber (NIS 2)',
      collapsible: true,
      items: [
        { label: 'Mesures',         route: '/nis2-measures',  icon: 'rule' },
        { label: 'Incidents cyber', route: '/cyber-incidents', icon: 'shield' }
      ]
    },
    {
      label: 'Intégrations',
      items: [
        { label: 'ITSM', route: '/itsm', icon: 'hub' }
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
