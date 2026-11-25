import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Observable, Subscription, combineLatest, of } from 'rxjs';
import { catchError, filter, map, shareReplay, startWith } from 'rxjs/operators';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { ADMIN_CONSOLE_ROLES } from '../../core/auth/tenant-admin';
import { NotificationView } from '../../core/notifications/notification.types';
import { NotificationsService } from '../../core/notifications/notifications.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import { CatalogService } from '../../core/catalogs/catalog.service';
import { BrandingService } from '../../core/branding/branding.service';
import { EditionService } from '../../core/edition/edition.service';
import { Edition } from '../../core/edition/edition.types';
import { TenantModulesService } from '../../features/admin/tenant-modules.service';
import { ThemePreference, ThemeService } from '../../core/theme/theme.service';
import { ADMIN_TABS, adminNavItems, routesDeLOnglet } from './admin-nav';
import { NavHit, searchNav } from './nav-search';

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
  /**
   * L'édition où l'entrée existe (ADR 0082). Absente = les deux. La console
   * éditeur n'a pas de sens dans une installation on-premise ; la page de
   * licence n'en a pas en SaaS.
   */
  edition?: Edition;
  /**
   * Les autres adresses dont cette entree repond, pour l'allumage et pour
   * l'ouverture de son groupe.
   *
   * <p>Une entree de la barre menait jusqu'ici a UNE adresse et s'allumait sur
   * elle seule. L'onglet « Parametrage » mene a `/admin/modules` mais couvre
   * quinze ecrans : sans cette liste, quatorze d'entre eux laissaient la barre
   * eteinte et le groupe replie -- on ne savait plus ou l'on etait.</p>
   */
  couvre?: readonly string[];
}

export interface NavSection {
  label: string;
  items: NavItem[];
  /**
   * Section repliable. Toutes le sont, sauf le Pilotage : repliées par défaut,
   * hormis celle de la page courante, elles ramènent la barre à une douzaine
   * de lignes au lieu de soixante entrées à faire défiler.
   */
  collapsible?: boolean;
}

/**
 * Rôles habilités à l'administration du tenant (§16). Le serveur reste l'autorité :
 * cette liste ne sert qu'à ne pas afficher un lien qui mènerait à un 403. Ce sont
 * les mêmes que ceux de la garde de la route `/admin`.
 */
const ADMIN_ROLES = [...ADMIN_CONSOLE_ROLES];

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
        // Pas d'attribut `module` : le SMI ne fait que relire les autres modules.
        { label: $localize`:@@nav.smi:Tableau de bord SMI`, route: '/smi',                    icon: 'hub' },
        // Le contexte de l'organisme (ISO 9001 §4.2) : qui attend quoi de vous.
        // Pas d'attribut `module` : le registre des parties intéressées est le
        // socle du système qualité, pas une option — c'est le serveur qui tranche
        // qui peut l'écrire.
        { label: $localize`:@@nav.stakeholders:Parties prenantes`, route: '/stakeholders', icon: 'groups' },
        { label: $localize`:@@nav.mes-dashboards:Mes dashboards`,   route: '/dashboard-builder', icon: 'dashboard_customize' },
        { label: $localize`:@@nav.tv-mode:Mode TV / Salle qualité`, route: '/tv',                icon: 'tv' },
        { label: $localize`:@@nav.indicateurs-kpi:Indicateurs (KPI)`, route: '/kpis',            icon: 'monitoring', module: 'kpi' },
        // Juste après les KPI, parce qu'un objectif ne se lit pas sans eux :
        // il dit ce qu'on attend d'un indicateur du catalogue. Même module.
        { label: $localize`:@@nav.objectifs-qualite:Objectifs qualité`, route: '/objectifs',     icon: 'flag', module: 'kpi' },
        // Pas d'attribut `module` : le coût de la qualité n'a pas encore d'entrée
        // au catalogue des modules activables. Lecture ouverte à tous, saisie
        // réservée au pilotage qualité -- c'est le serveur qui tranche.
        { label: $localize`:@@nav.cout-qualite:Coûts de non-qualité`, route: '/cout-qualite', icon: 'savings' }
      ]
    },
    {
      label: $localize`:@@nav.methodes-qualite:Méthodes qualité`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.pdca:PDCA`,         route: '/pdca',     icon: 'autorenew', module: 'pdca' },
        { label: $localize`:@@nav.5s:5S`,             route: '/fives',    icon: 'check_circle', module: 'fives' },
        { label: $localize`:@@nav.dmaic:DMAIC`,       route: '/dmaic',    icon: 'analytics', module: 'dmaic' },
        { label: $localize`:@@nav.cercles:Cercles`,   route: '/circles',  icon: 'groups', module: 'circle' },
        // Pas d'attribut `module` : la boîte à idées ne dépend pas de
        // l'activation du module Cercle. C'est tout son objet -- déposer une
        // idée sans qu'un cercle existe.
        { label: $localize`:@@nav.idees:Boîte à idées`, route: '/idees', icon: 'lightbulb' },
        { label: $localize`:@@nav.workflow-designer:Designer de workflow`, route: '/workflow-designer', icon: 'schema' }
      ]
    },
    {
      label: $localize`:@@nav.analyses-ia:Analyses IA`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.anomaly:Anomalies IA`,          route: '/anomaly',        icon: 'bubble_chart' },
        { label: $localize`:@@nav.nc-clusters:Clustering NC`,     route: '/nc-clusters',    icon: 'hub', module: 'capa' },
        { label: $localize`:@@nav.complaints-nlp:Réclamations IA`, route: '/complaints-nlp', icon: 'rate_review', module: 'complaints' },
        { label: $localize`:@@nav.assistant-ia:Assistant IA`,     route: '/nlq',            icon: 'forum' },
        { label: $localize`:@@nav.nlq-explore:Exploration NLQ`,   route: '/nlq-explore',    icon: 'query_stats' },
        { label: $localize`:@@nav.storyboard:Storyboard IA`,      route: '/storyboard',     icon: 'auto_stories' }
      ]
    },
    {
      // La maîtrise statistique des procédés (ADR 0096), en deux sous-menus :
      // les cartes de contrôle (registre des études Cp / Cpk) et Gage R&R.
      // Module « dmaic » : la capabilité relève du pilotage Six Sigma (§3.4).
      // L'analyse des règles de Nelson n'a plus d'entrée dans les Analyses IA :
      // la SPC se rejoint ici, et `/spc` reste ouvert depuis l'analyse d'un KPI.
      label: $localize`:@@nav.spc-section:SPC`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.spc-control-charts:Cartes de contrôle`, route: '/spc/cartes', icon: 'stacked_line_chart', module: 'dmaic' },
        { label: $localize`:@@nav.spc-gage-rr:Gage R&R`, route: '/spc/gage-rr', icon: 'precision_manufacturing', module: 'dmaic' }
      ]
    },
    {
      // Une seule entrée : les projets. Le cycle en V d'un projet porte lui-même
      // la navigation entre ses phases, et un sous-menu qui doublerait ses cinq
      // jalons demanderait de tenir deux listes d'accord pour le même parcours.
      // Le dossier PPAP n'a plus d'entrée non plus : il appartient désormais à un
      // projet, et une entrée globale n'aurait pas su duquel elle parlait.
      label: $localize`:@@nav.apqp:APQP`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.apqp-projects:Projets`, route: '/apqp', icon: 'account_tree' }
      ]
    },

    {
      // Ni Ishikawa ni les 5 Pourquoi n'ont d'entrée ici, et c'est voulu : ces
      // deux analyses partent d'un écart déjà constaté. On les ouvre depuis la
      // fiche de non-conformité, qui les crée sur son propre sujet et les
      // rattache. Une entrée de menu laissait croire à une analyse hors sol.
      label: $localize`:@@nav.non-conformite:Non-conformité`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.nc-interne:NC interne`,  route: '/nc/interne',  icon: 'home_repair_service', module: 'capa' },
        { label: $localize`:@@nav.nc-externe:NC externe (Réclamations)`,  route: '/nc/externe',  icon: 'campaign',            module: 'capa' },
        // Le troisieme ecart : celui que l'on recoit d'un fournisseur, suivi
        // jusqu'a son 8D. Il vit avec le module fournisseurs, pas avec la CAPA.
        // Le registre des fournisseurs le precede : c'est le meme geste
        // (evaluer le fournisseur, puis traiter ses ecarts).
        { label: $localize`:@@nav.fournisseurs:Fournisseurs`,      route: '/suppliers',      icon: 'factory', module: 'supplier' },
        { label: $localize`:@@nav.nc-fournisseurs:NC fournisseurs`, route: '/suppliers/non-conformites', icon: 'local_shipping', module: 'supplier' },
        // La CAPA suit immediatement la non-conformite parce que c'est la
        // suite du meme geste : on constate un ecart, puis on le traite. La
        // ranger dans « Operations » l'eloignait de ce qui la declenche.
        { label: $localize`:@@nav.capa:CAPA`,                       route: '/capa',      icon: 'engineering', module: 'capa' },
        { label: $localize`:@@nav.capa-efficacite:Efficacité CAPA`,   route: '/capa/efficacite', icon: 'insights', module: 'capa' },
        // La dérogation suit la CAPA parce qu'elle en est le voisin immédiat :
        // on constate un écart, on le traite, et parfois on demande au client
        // l'autorisation d'expédier quand même. Module capa, comme la CAPA :
        // c'est le même registre d'écarts qui la déclenche.
        { label: $localize`:@@nav.derogations:Dérogations client`, route: '/derogations', icon: 'handshake', module: 'capa' }
      ]
    },
    {
      // Un groupe à lui, comme l'APQP et la non-conformité : le registre unique
      // du SMI (ISO 9001 §6.1). Une seule entrée, le registre ; ses deux onglets
      // -- risques, opportunités -- portent la navigation interne. Module
      // « risk », comme l'AMDEC : le serveur refuse les écritures sans lui.
      label: $localize`:@@nav.risques-opportunites:Risques & opportunités`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.registre-risques:Registre`, route: '/risques', icon: 'shield', module: 'risk' }
      ]
    },
    {
      label: $localize`:@@nav.operations:Opérations`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.calibration:Instruments`,         route: '/calibration', icon: 'straighten', module: 'calibration' },
        { label: $localize`:@@nav.iot:Parc IoT`,                    route: '/iot',        icon: 'sensors', module: 'iot' },
        { label: $localize`:@@nav.audits:Audits`,                   route: '/audits',    icon: 'fact_check', module: 'audit' },
        // Le Standards Hub vient juste sous les audits : un audit se prepare
        // contre un referentiel, et c'est la qu'on va chercher l'exigence, le
        // modele de document et la feuille de route de certification. Le ranger
        // avec les referentiels l'eloignait du geste qui l'appelle.
        { label: $localize`:@@nav.standards-hub:Standards Hub`,     route: '/standards',      icon: 'workspace_premium', module: 'standards' },
        // « Planning audits » retire du menu : la programmation se fait depuis
        // le registre des audits, ou l'on voit en meme temps ce qui est deja
        // planifie. Deux portes pour un seul geste, et la seconde arrivait sur
        // un ecran qui ne montrait pas le reste. La route subsiste pour les
        // liens deja partages.
        { label: $localize`:@@nav.standards-ims:Co-couverture IMS`, route: '/standards-ims',  icon: 'grid_view', module: 'standards' },
        { label: $localize`:@@nav.doc-gen-ia:Génération doc IA`,    route: '/standards-doc-gen', icon: 'auto_awesome', module: 'standards' },
        { label: $localize`:@@nav.produits:Produits`,               route: '/products',  icon: 'inventory_2', module: 'product' },
        { label: $localize`:@@nav.risques-fmea:Risques (FMEA)`,     route: '/fmea',      icon: 'warning', module: 'risk' },
        { label: $localize`:@@nav.documents:GED`,                   route: '/documents', icon: 'description', module: 'docs' },
        { label: $localize`:@@nav.moc:MOC`, route: '/changes', icon: 'change_circle', module: 'change' },
        { label: $localize`:@@nav.ehs:EHS`,                         route: '/ehs',       icon: 'health_and_safety', module: 'ehs' },
        // La formation est une activite courante (habiliter, recycler), pas un
        // referentiel : elle rejoint les operations, avec sa matrice.
        { label: $localize`:@@nav.formation:Formation`,            route: '/training',       icon: 'school', module: 'training' },
        { label: $localize`:@@nav.competences:Matrice de compétences`, route: '/training/competences', icon: 'grid_on', module: 'training' }
      ]
    },
    {
      label: $localize`:@@nav.referentiels:Référentiels`,
      collapsible: true,
      items: [
        { label: $localize`:@@nav.packs-sectoriels:Packs sectoriels`, route: '/industry-packs', icon: 'category', module: 'industry' },
        { label: $localize`:@@nav.marketplace:Marketplace`,         route: '/marketplace',    icon: 'storefront', module: 'industry' },
        { label: $localize`:@@nav.academy:Academy`,                route: '/academy',        icon: 'cast_for_education', module: 'training' },
        { label: $localize`:@@nav.mon-apprentissage:Mon apprentissage`, route: '/learning',  icon: 'military_tech' },
        { label: $localize`:@@nav.integrations:Intégrations`,      route: '/itsm',           icon: 'hub', module: 'itsm' }
      ]
    },
    {
      label: $localize`:@@nav.conformite-grc:Conformité (GRC)`,
      collapsible: true,
      items: [
        // Une seule entrée, à dessein : la masse GRC (20 routes) est repliée derrière
        // la page hub /compliance. Y ajouter une entrée de plus ferait revenir le
        // problème que ce regroupement a précisément résolu.
        { label: $localize`:@@nav.conformite-hub:Conformité`, route: '/compliance', icon: 'verified_user' }
      ]
    },
    {
      label: $localize`:@@nav.administration:Administration`,
      collapsible: true,
      // UNE entree, et non dix-huit. L'administration du client est devenue une
      // console a deux onglets -- « Gestion utilisateurs » et « Parametrage » --
      // dont le catalogue vit dans `admin-nav.ts`. Dix-huit lignes dans la barre
      // laterale noyaient le reste du menu et melangeaient des reglages rares
      // avec le travail quotidien.
      //
      // Les dix-neuf ecrans restent trouvables par la RECHERCHE de la barre
      // superieure, qui indexe `sectionsIndexables` : sans cela, « Messagerie »
      // ou « Webhooks » auraient exige de savoir d'avance dans quel onglet
      // regarder.
      // Les deux memes onglets que la console, dans le meme ordre : la barre
      // annonce ce qu'on va y trouver, et le clic atterrit deja sur le bon.
      items: ADMIN_TABS.map(onglet => ({
        label: onglet.label,
        route: onglet.route,
        icon: onglet.icon,
        roles: ADMIN_ROLES,
        // L'onglet repond de TOUS ses ecrans : c'est ce qui le laisse allume
        // sur `/admin/messagerie` comme sur `/admin/modules`.
        couvre: routesDeLOnglet(onglet)
      }))
    }
  ];

  /**
   * Sections filtrées selon les rôles de l'utilisateur. Une section dont toutes les
   * entrées sont masquées disparaît entièrement — pas de titre de groupe orphelin.
   */
  visibleSections$!: Observable<NavSection[]>;
  /**
   * Ce que la recherche indexe : les sections visibles, PLUS les ecrans de la
   * console d'administration, qui ne sont plus dans la barre laterale.
   *
   * <p>Le meme filtre que la barre s'y applique — roles, modules, edition —
   * donc un utilisateur sans droits n'y trouve rien, et la licence reste
   * invisible en SaaS.</p>
   */
  get sectionsIndexables(): NavSection[] {
    return [
      ...this.sections,
      { label: $localize`:@@nav.administration:Administration`, items: adminNavItems() }
    ];
  }

  /** Dernières sections visibles : la recherche ne propose que ce que le menu montre. */
  private searchable: NavSection[] = [];
  private navigationSub?: Subscription;

  /** État réseau + file offline (§15.2-15.3) — chip de synchro dans la topbar. */
  online$!: Observable<boolean>;
  pendingSync$!: Observable<number>;

  /** Préférence de thème, reprise dans le menu du profil sur téléphone. */
  themePreference$!: Observable<ThemePreference>;

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
    private readonly modules: TenantModulesService,
    private readonly editions: EditionService,
    private readonly theme: ThemeService,
    private readonly branding: BrandingService,
    private readonly catalogs: CatalogService
  ) {}

  ngOnInit(): void {
    this.user$ = this.auth.user();
    // La marque du client : son theme et ses couleurs. Charge ici et pas dans un
    // initializer parce qu'elle demande un jeton, qui n'existe pas encore au
    // demarrage de l'application. Un echec ne rend rien et ne casse rien : on
    // reste sur les jetons du socle.
    this.branding.load().subscribe();
    // Les mots du client pour les listes déroulantes (ADR 0111). Ici, et pour la
    // même raison que la marque : un jeton est nécessaire. Un échec ne casse
    // rien — les écrans gardent leurs propres libellés traduits.
    this.catalogs.preload().subscribe();
    this.themePreference$ = this.theme.preference();
    // Les modules du tenant décident de ce qui est montré, au même titre que les
    // rôles. Un échec de chargement ne doit pas vider la navigation : on retombe
    // sur « inconnu », c'est-à-dire sur l'affichage complet, et le serveur reste
    // l'autorité si l'utilisateur ouvre un écran qu'il n'a pas.
    const modules$ = this.modules.enabledCodes().pipe(
      catchError(() => of<string[] | null>(null)),
      startWith(null as string[] | null),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    // L'édition, elle, est connue tôt et ne change pas : tant qu'elle ne l'est pas,
    // rien n'est retiré, comme pour les modules.
    const edition$ = this.editions.edition().pipe(startWith(null as Edition | null));

    this.visibleSections$ = combineLatest([this.user$, modules$, edition$]).pipe(
      map(([user, modules, edition]) => {
        const roles = user?.roles ?? [];
        // Deux filtrages, le meme filtre : la barre montre ses sections, la
        // recherche indexe en plus les ecrans d'administration.
        this.searchable =
          this.filterSections(roles, modules, edition, this.sectionsIndexables);
        return this.filterSections(roles, modules, edition);
      })
    );
    this.online$ = this.connectivity.online$;
    this.pendingSync$ = this.offlineQueue.pendingCount$;
    this.unreadCount$ = this.notificationsService.unread$;
    this.restoreCollapsedSections();
    // Une page atteinte par un lien, la recherche ou une notification se
    // retrouve dans le menu : son groupe s'ouvre s'il était replié.
    this.navigationSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.revealCurrentSection());
    this.watchViewport();
    // Pastille alimentée dès l'ouverture de l'application : la liste complète n'est
    // chargée qu'à l'ouverture du menu (appel léger au démarrage).
    this.notificationsService.refreshUnreadCount();
  }

  ngOnDestroy(): void {
    this.compactMedia?.removeEventListener('change', this.compactListener);
    this.navigationSub?.unsubscribe();
  }

  // ---- Recherche d'écran ----------------------------------------------------

  /** Saisie en cours dans la recherche de la barre supérieure. */
  searchQuery = '';
  /** Écrans proposés pour la saisie courante. */
  searchHits: NavHit[] = [];
  /** Rang de la proposition désignée au clavier, -1 si aucune. */
  activeHit = -1;
  /** La liste des propositions est déroulée. */
  searchOpen = false;
  /** Raccourci affiché dans le champ : ⌘K sur Mac, Ctrl K ailleurs. */
  readonly searchShortcut =
    typeof navigator !== 'undefined' && /mac|iphone|ipad/i.test(navigator.platform) ? '⌘K' : 'Ctrl K';

  @ViewChild('searchInput') private searchInput?: ElementRef<HTMLInputElement>;

  /** Ctrl+K / ⌘K place le curseur dans la recherche, d'où qu'on soit. */
  @HostListener('document:keydown', ['$event'])
  onGlobalKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.searchInput?.nativeElement.focus();
      this.searchInput?.nativeElement.select();
    }
  }

  onSearchInput(query: string): void {
    this.searchQuery = query;
    this.searchHits = searchNav(this.searchable, query);
    this.activeHit = this.searchHits.length ? 0 : -1;
    this.searchOpen = query.trim().length > 0;
  }

  /** Flèches pour parcourir, Entrée pour ouvrir, Échap pour abandonner. */
  onSearchKeydown(event: KeyboardEvent): void {
    const count = this.searchHits.length;
    switch (event.key) {
      case 'ArrowDown':
        if (count) { event.preventDefault(); this.activeHit = (this.activeHit + 1) % count; }
        break;
      case 'ArrowUp':
        if (count) { event.preventDefault(); this.activeHit = (this.activeHit - 1 + count) % count; }
        break;
      case 'Enter':
        if (this.activeHit >= 0 && this.activeHit < count) {
          event.preventDefault();
          this.openHit(this.searchHits[this.activeHit]);
        }
        break;
      case 'Escape':
        event.stopPropagation();
        this.resetSearch();
        this.searchInput?.nativeElement.blur();
        break;
    }
  }

  /**
   * Sur téléphone, le champ est replié derrière un bouton : il se déploie sur
   * toute la barre et reçoit le focus. Le focus attend le rendu suivant, le
   * champ n'étant affiché qu'une fois la classe posée sur la barre.
   */
  searchExpanded = false;

  expandSearch(): void {
    this.searchExpanded = true;
    setTimeout(() => this.searchInput?.nativeElement.focus());
  }

  /** Le champ perd le focus : la liste se referme, la saisie reste. */
  closeSearch(): void {
    this.searchOpen = false;
    this.searchExpanded = false;
  }

  setTheme(preference: ThemePreference): void {
    this.theme.setPreference(preference);
  }

  openHit(hit: NavHit): void {
    this.resetSearch();
    this.onNavigate();
    void this.router.navigateByUrl(hit.item.route);
  }

  private resetSearch(): void {
    this.searchExpanded = false;
    this.searchQuery = '';
    this.searchHits = [];
    this.activeHit = -1;
    this.searchOpen = false;
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
  filterSections(
    roles: string[],
    modules: string[] | null = null,
    edition: Edition | null = null,
    source: NavSection[] = this.sections
  ): NavSection[] {
    // Normaliser AVANT de retirer le préfixe : Keycloak peut émettre `role_admin`
    // en minuscules, auquel cas un strip sensible à la casse laisserait passer le
    // préfixe et ferait échouer la comparaison.
    const owned = new Set(roles.map(r => r.toUpperCase().replace(/^ROLE_/, '')));
    // `null` = modules encore inconnus : on n'ampute rien. Mieux vaut une barre
    // complète pendant le chargement qu'une barre qui se vide puis se remplit.
    const enabled = modules === null ? null : new Set(modules);
    return source
      .map(section => ({
        ...section,
        items: section.items.filter(item =>
          (!item.roles || item.roles.some(r => owned.has(r)))
          && (enabled === null || !item.module || enabled.has(item.module))
          && (edition === null || !item.edition || item.edition === edition))
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
   * L'entree de menu correspondant a l'adresse courante.
   *
   * <p>`routerLinkActive` compare par PREFIXE : des qu'une route en prefixe une
   * autre -- /admin et /admin/modules -- les deux entrees s'allument, et le menu
   * dit qu'on est a deux endroits a la fois. On garde donc la correspondance la
   * plus longue parmi toutes les entrees.
   *
   * <p>Un prefixe reste une correspondance valable : sur /apqp/<projet>/2, qui
   * designe un rang de phase et non une entree de menu, « Projets » doit rester
   * allumee.
   */
  estActif(item: NavItem): boolean {
    const url = this.router.url.split('?')[0].split('#')[0];
    const portee = this.portee(url, item);
    return portee > 0 && portee === this.plusLongueCorrespondance(url);
  }

  /**
   * Longueur de la meilleure adresse dont cette entree repond, 0 si aucune.
   *
   * <p>On prend la PLUS LONGUE : `/admin/modules` et `/admin` correspondraient
   * tous deux a `/admin/modules`, et c'est la plus precise qui doit gagner.</p>
   */
  private portee(url: string, item: NavItem): number {
    return [item.route, ...(item.couvre ?? [])]
      .filter(route => this.correspond(url, route))
      .reduce((max, route) => Math.max(max, route.length), 0);
  }

  /** Longueur de la route la plus longue qui corresponde a cette adresse. */
  private plusLongueCorrespondance(url: string): number {
    return this.sections
      .flatMap(section => section.items)
      .reduce((max, item) => Math.max(max, this.portee(url, item)), 0);
  }

  private correspond(url: string, route: string): boolean {
    return url === route || url.startsWith(route + '/');
  }

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
    const current = this.sectionOf(this.router.url);
    this.sections
      .filter(s => s.collapsible && s.label !== current)
      .forEach(s => this.collapsedSections.add(s.label));
  }

  /** Ouvre le groupe qui porte la page courante, s'il était replié. */
  private revealCurrentSection(): void {
    const current = this.sectionOf(this.router.url);
    if (current && this.collapsedSections.delete(current)) {
      this.persistCollapsedSections();
    }
  }

  /** Le groupe dont une entrée correspond le mieux à l'adresse. */
  private sectionOf(rawUrl: string | undefined): string | null {
    const url = (rawUrl ?? '').split('?')[0].split('#')[0];
    let best: { label: string; length: number } | null = null;
    for (const section of this.sections) {
      for (const item of section.items) {
        const portee = this.portee(url, item);
        if (portee > 0 && portee > (best?.length ?? 0)) {
          best = { label: section.label, length: portee };
        }
      }
    }
    return best?.label ?? null;
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
