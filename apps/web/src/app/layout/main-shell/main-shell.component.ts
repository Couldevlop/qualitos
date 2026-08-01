import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { NotificationView } from '../../core/notifications/notification.types';
import { NotificationsService } from '../../core/notifications/notifications.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
  badge?: string;
  /**
   * Rôles autorisés à voir l'entrée. Absent = visible par tous.
   * Ce filtrage est un confort d'interface : l'autorisation réelle reste appliquée
   * par le serveur (§16). Afficher un lien qui mène systématiquement à un 403 est une
   * mauvaise expérience, pas une faille.
   */
  roles?: string[];
}

export interface NavSection {
  label: string;
  items: NavItem[];
  /** Section repliable (groupes de conformité GRC), repliée par défaut. */
  collapsible?: boolean;
}

/**
 * Rôles habilités à l'administration du tenant (§16). Le serveur reste l'autorité :
 * cette liste ne sert qu'à ne pas afficher un lien qui mènerait à un 403.
 */
const ADMIN_ROLES = ['ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN'];

@Component({
  selector: 'qos-main-shell',
  templateUrl: './main-shell.component.html',
  styleUrls: ['./main-shell.component.scss'],
  standalone: false
})
export class MainShellComponent implements OnInit {

  user$!: Observable<AuthUser | null>;
  collapsed = false;

  /** Année courante pour la signature de pied de page (éditeur de la plateforme). */
  readonly currentYear = new Date().getFullYear();

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
        { label: $localize`:@@nav.marketplace:Marketplace`,         route: '/marketplace',    icon: 'storefront' },
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
    },
    {
      label: $localize`:@@nav.administration:Administration`,
      items: [
        {
          label: $localize`:@@nav.admin-modules:Modules du tenant`,
          route: '/admin/modules',
          icon: 'tune',
          roles: ADMIN_ROLES
        }
      ]
    }
  ];

  /**
   * Sections filtrées selon les rôles de l'utilisateur. Une section dont toutes les
   * entrées sont masquées disparaît entièrement — pas de titre de groupe orphelin.
   */
  visibleSections$!: Observable<NavSection[]>;

  /** État réseau + file offline (§15.2-15.3) — chip de synchro dans la topbar. */
  online$!: Observable<boolean>;
  pendingSync$!: Observable<number>;

  /** Notifications in-app : pastille de non-lues + contenu du menu de la cloche. */
  unreadCount$!: Observable<number>;
  notifications: NotificationView[] = [];
  notificationsLoading = false;

  constructor(
    private readonly auth: AuthService,
    private readonly connectivity: ConnectivityService,
    private readonly offlineQueue: OfflineQueueService,
    private readonly notificationsService: NotificationsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.user$ = this.auth.user();
    this.visibleSections$ = this.user$.pipe(
      map(user => this.filterSections(user?.roles ?? []))
    );
    this.online$ = this.connectivity.online$;
    this.pendingSync$ = this.offlineQueue.pendingCount$;
    this.unreadCount$ = this.notificationsService.unread$;
    this.restoreCollapsedSections();
    // Pastille alimentée dès l'ouverture de l'application : la liste complète n'est
    // chargée qu'à l'ouverture du menu (appel léger au démarrage).
    this.notificationsService.refreshUnreadCount();
  }

  // ---- Notifications --------------------------------------------------------

  /** Charge les dernières notifications à l'ouverture du menu de la cloche. */
  openNotifications(): void {
    this.notificationsLoading = true;
    this.notificationsService.recent().subscribe({
      next: list => {
        this.notifications = list;
        this.notificationsLoading = false;
      },
      error: () => { this.notificationsLoading = false; }
    });
  }

  /**
   * Ouvre la ressource concernée et marque la notification comme lue.
   * Une notification sans lien reste cliquable : elle est simplement marquée lue.
   */
  onNotificationClick(notification: NotificationView): void {
    if (!notification.read) {
      this.notificationsService.markRead(notification.id).subscribe(() => {
        notification.read = true;
      });
    }
    if (notification.link) {
      void this.router.navigateByUrl(notification.link);
    }
  }

  markAllNotificationsRead(): void {
    this.notificationsService.markAllRead().subscribe(() => {
      this.notifications = this.notifications.map(n => ({ ...n, read: true }));
    });
  }

  /** Icône Material correspondant à la nature de la notification. */
  notificationIcon(type: NotificationView['type']): string {
    switch (type) {
      case 'SUCCESS': return 'check_circle';
      case 'WARNING': return 'warning';
      case 'ALERT': return 'error';
      default: return 'info';
    }
  }

  // ---- Compte utilisateur ---------------------------------------------------

  logout(): void {
    this.auth.logout();
  }

  /**
   * Retient les entrées visibles pour ces rôles, puis élimine les sections vides.
   * La comparaison ignore un éventuel préfixe `ROLE_` : Keycloak le pose, pas nous.
   */
  filterSections(roles: string[]): NavSection[] {
    // Normaliser AVANT de retirer le préfixe : Keycloak peut émettre `role_admin`
    // en minuscules, auquel cas un strip sensible à la casse laisserait passer le
    // préfixe et ferait échouer la comparaison.
    const owned = new Set(roles.map(r => r.toUpperCase().replace(/^ROLE_/, '')));
    return this.sections
      .map(section => ({
        ...section,
        items: section.items.filter(item =>
          !item.roles || item.roles.some(r => owned.has(r)))
      }))
      .filter(section => section.items.length > 0);
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
