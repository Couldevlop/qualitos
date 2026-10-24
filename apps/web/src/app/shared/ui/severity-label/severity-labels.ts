/**
 * Libellés des niveaux de gravité renvoyés par l'API (LOW…CRITICAL pour les
 * incidents, MINOR…CRITICAL pour les non-conformités) et des catégories de NC.
 *
 * <p>Les listes, fiches et sélecteurs affichaient le code brut (« HIGH »,
 * « MAJOR », « DOCUMENTATION »), qui ne se traduit dans aucune langue. Les
 * traductions vivent dans `scripts/i18n/severity_labels.py`.
 */
const SEVERITIES: Readonly<Record<string, string>> = {
  LOW: $localize`:@@severity.low:Faible`,
  MEDIUM: $localize`:@@severity.medium:Moyenne`,
  HIGH: $localize`:@@severity.high:Élevée`,
  CRITICAL: $localize`:@@severity.critical:Critique`,
  MINOR: $localize`:@@severity.minor:Mineure`,
  MAJOR: $localize`:@@severity.major:Majeure`
};

const NC_CATEGORIES: Readonly<Record<string, string>> = {
  PRODUCT: $localize`:@@nc.category.product:Produit`,
  PROCESS: $localize`:@@nc.category.process:Processus`,
  DOCUMENTATION: $localize`:@@nc.category.documentation:Documentation`,
  SUPPLIER: $localize`:@@nc.category.supplier:Fournisseur`,
  SAFETY: $localize`:@@nc.category.safety:Sécurité`,
  ENVIRONMENT: $localize`:@@nc.category.environment:Environnement`,
  OTHER: $localize`:@@nc.category.other:Autre`
};

function lookup(table: Readonly<Record<string, string>>, code: string | null | undefined): string {
  if (!code) return '—';
  return table[code] ?? code;
}

/** Libellé d'une gravité ; le code s'il est inconnu, un tiret s'il manque. */
export function severityLabel(code: string | null | undefined): string {
  return lookup(SEVERITIES, code);
}

/** Libellé d'une catégorie de non-conformité ; le code s'il est inconnu, un tiret s'il manque. */
export function ncCategoryLabel(code: string | null | undefined): string {
  return lookup(NC_CATEGORIES, code);
}
