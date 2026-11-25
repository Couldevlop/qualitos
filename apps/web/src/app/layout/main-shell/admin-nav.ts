import { ADMIN_CONSOLE_ROLES } from '../../core/auth/tenant-admin';
import type { NavItem } from './main-shell.component';

/**
 * Rôles habilités : les mêmes que la garde de la route `/admin`. Ils ne servent
 * pas à l'intérieur de la console — on y est déjà entré — mais à la RECHERCHE
 * de la barre supérieure, qui indexe ces entrées pour tout le monde. Sans eux,
 * un utilisateur sans droits verrait « Journal d'audit » dans ses propositions.
 */
const ADMIN_ROLES = [...ADMIN_CONSOLE_ROLES];

/**
 * Les deux onglets de l'administration du client, et les écrans de chacun.
 *
 * <p><b>Pourquoi cette liste est ici, et non dans le module d'administration.</b>
 * Elle a DEUX lecteurs : l'enveloppe `/admin`, qui en tire ses onglets et son
 * menu, et la recherche de la barre supérieure, qui indexe `sections[].items`.
 * La barre latérale ne montre plus qu'une entrée « Administration » ; si cette
 * liste vivait dans le module d'administration — chargé à la demande — la
 * recherche ne la verrait pas, et « Messagerie » ou « Webhooks » deviendraient
 * introuvables autrement qu'en sachant déjà où elles sont. Une seule liste,
 * deux rendus.</p>
 *
 * <p>Aucun import de composant ici : c'est de la donnée, elle n'entraîne pas le
 * module d'administration dans le paquet principal.</p>
 */
export interface AdminItem extends NavItem {
  /**
   * L'écran vit hors de `/admin` et n'a donc pas la barre d'onglets. Le menu le
   * signale : partir sans le dire donnerait l'impression d'avoir quitté la
   * console par erreur.
   */
  horsConsole?: boolean;
}

export interface AdminGroup {
  label: string;
  items: AdminItem[];
}

export interface AdminTab {
  /** Repère stable, posé dans `data.onglet` des routes enfants. */
  id: 'utilisateurs' | 'parametrage';
  label: string;
  icon: string;
  /** Première adresse de l'onglet : celle où mène son lien. */
  route: string;
  groups: AdminGroup[];
}

/**
 * Onglet 1 — qui travaille dans l'organisation, et ce qu'il a le droit de faire.
 *
 * <p>Les cabinets partenaires y figurent : accorder un accès à un auditeur
 * externe est une habilitation accordée à des personnes, même si ces personnes
 * ne sont pas de la maison. Le terme en est d'ailleurs la seule différence
 * (ADR 0106).</p>
 */
const UTILISATEURS: AdminGroup[] = [
  {
    label: $localize`:@@admin.group.equipe:Équipe et habilitations`,
    items: [
      {
        label: $localize`:@@nav.admin-team:Équipe & habilitations`,
        roles: ADMIN_ROLES,
        route: '/admin/team',
        icon: 'group'
      },
      {
        label: $localize`:@@nav.admin-roles:Rôles et droits`,
        roles: ADMIN_ROLES,
        route: '/admin/roles',
        icon: 'admin_panel_settings'
      }
    ]
  },
  {
    label: $localize`:@@admin.group.externes:Accès externes`,
    items: [
      {
        label: $localize`:@@nav.admin-firms:Cabinets`,
        roles: ADMIN_ROLES,
        route: '/admin/cabinets',
        icon: 'handshake'
      }
    ]
  }
];

/**
 * Onglet 2 — tout ce qui se règle. Groupé, parce que dix-sept écrans en une
 * colonne ne se parcourent pas : on y cherche un réglage précis, pas une liste.
 */
const PARAMETRAGE: AdminGroup[] = [
  {
    label: $localize`:@@admin.group.organisation:Organisation`,
    items: [
      {
        label: $localize`:@@nav.admin-modules:Modules et facturation`,
        roles: ADMIN_ROLES,
        route: '/admin/modules',
        icon: 'tune'
      },
      {
        label: $localize`:@@nav.admin-mail:Messagerie`,
        roles: ADMIN_ROLES,
        route: '/admin/messagerie',
        icon: 'alternate_email'
      },
      {
        // La licence n'a de sens qu'en installation sur site (ADR 0082) : en
        // SaaS, l'abonnement tient lieu de licence et l'écran n'existe pas.
        label: $localize`:@@nav.admin-license:Licence`,
        roles: ADMIN_ROLES,
        route: '/admin/licence',
        icon: 'verified_user',
        edition: 'ONPREM'
      }
    ]
  },
  {
    label: $localize`:@@admin.group.listes:Listes et règles métier`,
    items: [
      {
        label: $localize`:@@nav.admin-catalogs:Listes déroulantes`,
        roles: ADMIN_ROLES,
        route: '/admin/listes',
        icon: 'list_alt'
      },
      {
        label: $localize`:@@nav.admin-nc:Natures de NC`,
        roles: ADMIN_ROLES,
        route: '/admin/nc',
        icon: 'category'
      },
      {
        label: $localize`:@@nav.admin-moc:Paramètres MOC`,
        roles: ADMIN_ROLES,
        route: '/admin/moc',
        icon: 'rule'
      },
      {
        label: $localize`:@@nav.admin-spc:Paramètres SPC`,
        roles: ADMIN_ROLES,
        route: '/admin/spc',
        icon: 'stacked_line_chart'
      },
      {
        label: $localize`:@@nav.admin-dmaic:Paramètres DMAIC`,
        roles: ADMIN_ROLES,
        route: '/admin/dmaic',
        icon: 'fact_check'
      },
      {
        label: $localize`:@@nav.admin-documents:Documentation`,
        roles: ADMIN_ROLES,
        route: '/admin/documents',
        icon: 'folder_copy'
      },
      {
        // Atteint jusqu'ici par un seul bouton de la matrice des compétences :
        // il garde ce chemin, et gagne celui-ci.
        label: $localize`:@@nav.admin-competences:Ateliers de compétence`,
        roles: ADMIN_ROLES,
        route: '/admin/competences',
        icon: 'school'
      }
    ]
  },
  {
    label: $localize`:@@admin.group.ia:Intelligence artificielle`,
    items: [
      {
        label: $localize`:@@nav.admin-ai:Fournisseur d'IA`,
        roles: ADMIN_ROLES,
        route: '/admin/ia',
        icon: 'shield'
      }
    ]
  },
  {
    label: $localize`:@@admin.group.securite:Sécurité et intégrations`,
    items: [
      {
        label: $localize`:@@nav.admin-api-keys:Clés d'API`,
        roles: ADMIN_ROLES,
        route: '/admin/api-keys',
        icon: 'key'
      },
      {
        label: $localize`:@@nav.admin-webhooks:Webhooks`,
        roles: ADMIN_ROLES,
        route: '/admin/webhooks',
        icon: 'webhook'
      },
      {
        label: $localize`:@@nav.admin-quotas:Quotas d'API`,
        roles: ADMIN_ROLES,
        route: '/admin/quotas',
        icon: 'speed'
      },
      {
        label: $localize`:@@nav.admin-audit-log:Journal d'audit`,
        roles: ADMIN_ROLES,
        route: '/admin/audit-log',
        icon: 'receipt_long'
      },
      {
        // Hors de `/admin` : la route est anterieure, des liens la visent, et la
        // deplacer casserait des signets pour un gain d'apparence. Le menu dit
        // qu'on quitte la console.
        label: $localize`:@@nav.admin-connectors:Connecteurs`,
        roles: ADMIN_ROLES,
        route: '/connectors',
        icon: 'cable',
        horsConsole: true
      }
    ]
  }
];

export const ADMIN_TABS: readonly AdminTab[] = [
  {
    id: 'utilisateurs',
    label: $localize`:@@admin.tab.utilisateurs:Gestion utilisateurs`,
    icon: 'manage_accounts',
    route: '/admin/team',
    groups: UTILISATEURS
  },
  {
    id: 'parametrage',
    label: $localize`:@@admin.tab.parametrage:Paramétrage`,
    icon: 'settings',
    route: '/admin/modules',
    groups: PARAMETRAGE
  }
];

/** Toutes les entrées des deux onglets, à plat — ce que la recherche indexe. */
export function adminNavItems(): AdminItem[] {
  return ADMIN_TABS.flatMap(tab => tab.groups.flatMap(group => group.items));
}
