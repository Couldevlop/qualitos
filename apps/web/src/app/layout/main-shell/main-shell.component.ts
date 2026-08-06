import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, shareReplay, startWith } from 'rxjs/operators';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { NotificationView } from '../../core/notifications/notification.types';
import { NotificationsService } from '../../core/notifications/notifications.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import { TenantModulesService } from '../../features/admin/tenant-modules.service';

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
  /**
   * Code du module du catalogue dont dépend l'entrée (§10.4). Absent = entrée
   * de plateforme, toujours visible (accueil, tableau de bord, administration).
   * Le serveur reste l'autorité : ce filtrage évite d'annoncer une porte qui se
   * refermera, il ne remplace pas l'autorisation.
   */
  module?: string;
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
export class MainShellComponent implements OnInit, OnDestroy {

  user$!: Observable<AuthUser | null>;
  collapsed = false;

  /**
   * Navigation en écran étroit. Le shell était une grille à deux colonnes à TOUTES
   * les largeurs : sur un téléphone la barre latérale occupait les deux tiers de
   * l'écran, et la replier laissait encore un rail de 64 px devant le contenu.
   * Sous ce seuil, elle devient un tiroir superposé, fermé par défaut, qui glisse
   * par translation (donc composée par le GPU, sans recalcul de mise en page).
   */
  compact = false;
  navOpen = false;

  // 900 px, et non 1024 : c'est le seuil que le shell utilisait DÉJÀ pour faire
  // apparaître le bouton de navigation dans la barre supérieure. En choisir un
  // autre faisait disparaître la barre latérale sur des fenêtres où elle avait
  // encore toute sa place — un demi-écran de portable, par exemple — et la
  // navigation devenait méconnaissable pour qui travaille ainsi.
  private static readonly COMPACT_QUERY = '(max-width: 900px)';
  private compactMedia: MediaQueryList | null = null;
  private readonly compactListener = (event: MediaQueryListEvent): void =>
    this.onViewportChange(event.matches);

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
        { label: $localize`:@@nav.indicateurs-kpi:Indicateurs (KPI)`, route: '/kpis',            icon: 'monitoring', module: 'kpi' }
      ]
    },
    {
      label: $localize`:@@nav.methodes-qualite:Méthodes qualité`,
      items: [
        { label: $localize`:@@nav.pdca:PDCA`,         route: '/pdca',     icon: 'autorenew', module: 'pdca' },
        { label: $localize`:@@nav.ishikawa:Ishikawa`, route: '/ishikawa', icon: 'account_tree', module: 'ishikawa' },
        { label: $localize`:@@nav.5s:5S`,             route: '/fives',    icon: 'check_circle', module: 'fives' },
        { label: $localize`:@@nav.dmaic:DMAIC`,       route: '/dmaic',    icon: 'analytics', module: 'dmaic' },
        { label: $localize`:@@nav.cercles:Cercles`,   route: '/circles',  icon: 'groups', module: 'circle' },
        { label: $localize`:@@nav.workflow-designer:Designer de workflow`, route: '/workflow-designer', icon: 'schema' }
      ]
    },
    {
      label: $localize`:@@nav.analyses-ia:Analyses IA`,
      items: [
        { label: $localize`:@@nav.spc:SPC`,                       route: '/spc',            icon: 'show_chart', module: 'dmaic' },
        { label: $localize`:@@nav.anomaly:Anomalies IA`,          route: '/anomaly',        icon: 'bubble_chart' },
        { label: $localize`:@@nav.forecast:Prévision KPI`,        route: '/forecast',       icon: 'trending_up' },
        { label: $localize`:@@nav.nc-clusters:Clustering NC`,     route: '/nc-clusters',    icon: 'hub', module: 'capa' },
        { label: $localize`:@@nav.complaints-nlp:Réclamations IA`, route: '/complaints-nlp', icon: 'rate_review', module: 'complaints' },
        { label: $localize`:@@nav.assistant-ia:Assistant IA`,     route: '/nlq',            icon: 'forum' },
        { label: $localize`:@@nav.nlq-explore:Exploration NLQ`,   route: '/nlq-explore',    icon: 'query_stats' },
        { label: $localize`:@@nav.storyboard:Storyboard IA`,      route: '/storyboard',     icon: 'auto_stories' }
      ]
    },
    {
      label: $localize`:@@nav.non-conformite:Non-conformité`,
      items: [
        { label: $localize`:@@nav.nc-interne:NC interne`,  route: '/nc/interne',  icon: 'home_repair_service', module: 'capa' },
        { label: $localize`:@@nav.nc-externe:NC externe`,  route: '/nc/externe',  icon: 'campaign',            module: 'capa' }
      ]
    },
    {
      label: $localize`:@@nav.operations:Opérations`,
      items: [
        { label: $localize`:@@nav.capa:CAPA`,                       route: '/capa',      icon: 'engineering', module: 'capa' },
        { label: $localize`:@@nav.reclamations:Réclamations`,       route: '/complaints', icon: 'support_agent', module: 'complaints' },
        { label: $localize`:@@nav.calibration:Calibration`,         route: '/calibration', icon: 'straighten', module: 'calibration' },
        { label: $localize`:@@nav.iot:Parc IoT`,                    route: '/iot',        icon: 'sensors', module: 'iot' },
        { label: $localize`:@@nav.audits:Audits`,                   route: '/audits',    icon: 'fact_check', module: 'audit' },
        { label: $localize`:@@nav.risques-fmea:Risques (FMEA)`,     route: '/fmea',      icon: 'warning', module: 'risk' },
        { label: $localize`:@@nav.documents:Documents`,             route: '/documents', icon: 'description', module: 'docs' },
        { label: $localize`:@@nav.changements:Changements`,         route: '/changes',   icon: 'change_circle', module: 'change' },
        { label: $localize`:@@nav.ehs:EHS`,                         route: '/ehs',       icon: 'health_and_safety', module: 'ehs' }
      ]
    },
    {
      label: $localize`:@@nav.referentiels:Référentiels`,
      items: [
        { label: $localize`:@@nav.standards-hub:Standards Hub`,     route: '/standards',      icon: 'workspace_premium', module: 'standards' },
        { label: $localize`:@@nav.standards-ims:Co-couverture IMS`, route: '/standards-ims',  icon: 'grid_view', module: 'standards' },
        { label: $localize`:@@nav.doc-gen-ia:Génération doc IA`,    route: '/standards-doc-gen', icon: 'auto_awesome', module: 'standards' },
        { label: $localize`:@@nav.packs-sectoriels:Packs sectoriels`, route: '/industry-packs', icon: 'category', module: 'industry' },
        { label: $localize`:@@nav.marketplace:Marketplace`,         route: '/marketplace',    icon: 'storefront', module: 'industry' },
        { label: $localize`:@@nav.fournisseurs:Fournisseurs`,      route: '/suppliers',      icon: 'local_shipping', module: 'supplier' },
        { label: $localize`:@@nav.formation:Formation`,            route: '/training',       icon: 'school', module: 'training' },
        { label: $localize`:@@nav.academy:Academy`,                route: '/academy',        icon: 'cast_for_education', module: 'training' },
        { label: $localize`:@@nav.mon-apprentissage:Mon apprentissage`, route: '/learning',  icon: 'military_tech' },
        { label: $localize`:@@nav.integrations:Intégrations`,      route: '/itsm',           icon: 'hub', module: 'itsm' }
      ]
    },
    {
      label: $localize`:@@nav.conformite-grc:Conformité (GRC)`,
      items: [
        // Une seule entrée, à dessein : la masse GRC (20 routes) est repliée derrière
        // la page hub /compliance. Y ajouter une entrée de plus ferait revenir le
        // problème que ce regroupement a précisément résolu.
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
        },
        {
          label: $localize`:@@nav.admin-team:Équipe & habilitations`,
          route: '/admin/team',
          icon: 'group',
          roles: ADMIN_ROLES
        },
        {
          label: $localize`:@@nav.admin-api-keys:Clés d'API`,
          route: '/admin/api-keys',
          icon: 'key',
          roles: ADMIN_ROLES
        },
        {
          label: $localize`:@@nav.admin-webhooks:Webhooks`,
          route: '/admin/webhooks',
          icon: 'webhook',
          roles: ADMIN_ROLES
        },
        {
          label: $localize`:@@nav.admin-quotas:Quotas d'API`,
          route: '/admin/quotas',
          icon: 'speed',
          roles: ADMIN_ROLES
        },
        {
          label: $localize`:@@nav.admin-audit-log:Journal d'audit`,
          route: '/admin/audit-log',
          icon: 'receipt_long',
          roles: ADMIN_ROLES
        },
        {
          label: $localize`:@@nav.admin-connectors:Connecteurs`,
          route: '/connectors',
          icon: 'cable',
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
    private readonly router: Router,
    private readonly modules: TenantModulesService
  ) {}

  ngOnInit(): void {
    this.user$ = this.auth.user();
    // Les modules du tenant décident de ce qui est montré, au même titre que les
    // rôles. Un échec de chargement ne doit pas vider la navigation : on retombe
    // sur « inconnu », c'est-à-dire sur l'affichage complet, et le serveur reste
    // l'autorité si l'utilisateur ouvre un écran qu'il n'a pas.
    const modules$ = this.modules.enabledCodes().pipe(
      catchError(() => of<string[] | null>(null)),
      startWith(null as string[] | null),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.visibleSections$ = combineLatest([this.user$, modules$]).pipe(
      map(([user, modules]) => this.filterSections(user?.roles ?? [], modules))
    );
    this.online$ = this.connectivity.online$;
    this.pendingSync$ = this.offlineQueue.pendingCount$;
    this.unreadCount$ = this.notificationsService.unread$;
    this.restoreCollapsedSections();
    this.watchViewport();
    // Pastille alimentée dès l'ouverture de l'application : la liste complète n'est
    // chargée qu'à l'ouverture du menu (appel léger au démarrage).
    this.notificationsService.refreshUnreadCount();
  }

  ngOnDestroy(): void {
    this.compactMedia?.removeEventListener('change', this.compactListener);
  }

  /**
   * Suit la largeur disponible par `matchMedia` plutôt que par un écouteur de
   * `resize` : le navigateur ne notifie qu'aux franchissements du seuil, là où un
   * `resize` déclencherait des dizaines de passages de détection de changement
   * pendant un simple redimensionnement.
   */
  private watchViewport(): void {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    this.compactMedia = window.matchMedia(MainShellComponent.COMPACT_QUERY);
    this.onViewportChange(this.compactMedia.matches);
    this.compactMedia.addEventListener('change', this.compactListener);
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
  filterSections(roles: string[], modules: string[] | null = null): NavSection[] {
    // Normaliser AVANT de retirer le préfixe : Keycloak peut émettre `role_admin`
    // en minuscules, auquel cas un strip sensible à la casse laisserait passer le
    // préfixe et ferait échouer la comparaison.
    const owned = new Set(roles.map(r => r.toUpperCase().replace(/^ROLE_/, '')));
    // `null` = modules encore inconnus : on n'ampute rien. Mieux vaut une barre
    // complète pendant le chargement qu'une barre qui se vide puis se remplit.
    const enabled = modules === null ? null : new Set(modules);
    return this.sections
      .map(section => ({
        ...section,
        items: section.items.filter(item =>
          (!item.roles || item.roles.some(r => owned.has(r)))
          && (enabled === null || !item.module || enabled.has(item.module)))
      }))
      .filter(section => section.items.length > 0);
  }

  /**
   * Bouton de navigation de la barre supérieure. Le même geste n'a pas le même
   * sens selon la place disponible : en écran large il replie le rail, en écran
   * étroit il ouvre le tiroir — replier un rail déjà superposé n'aurait aucun sens.
   */
  toggle(): void {
    if (this.compact) {
      this.navOpen = !this.navOpen;
      return;
    }
    this.collapsed = !this.collapsed;
  }

  /** Ferme le tiroir (voile, Échap, bouton de fermeture). */
  @HostListener('document:keydown.escape')
  closeNav(): void { this.navOpen = false; }

  /**
   * Un lien de navigation vient d'être suivi : en écran étroit le tiroir masque
   * la page que l'on vient de demander, il doit donc s'effacer.
   */
  onNavigate(): void { this.navOpen = false; }

  /**
   * Bascule entre disposition large et étroite. Appelé au démarrage puis à chaque
   * changement de largeur. Repasser en large referme le tiroir : laissé ouvert, il
   * resterait superposé à un contenu qui a désormais toute la place.
   */
  onViewportChange(compact: boolean): void {
    this.compact = compact;
    if (!compact) {
      this.navOpen = false;
    }
  }

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
