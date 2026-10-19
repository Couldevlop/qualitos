import { DisplayStatus, DocumentType, DocumentView, VersionStatus } from './documents.types';

/**
 * Les libellés de la GED, en un seul endroit : la liste, le panneau, les
 * dialogues et la fiche nomment les mêmes choses avec les mêmes mots.
 */
export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  POLICY:           $localize`:@@documents.type.policy:Politique`,
  PROCEDURE:        $localize`:@@documents.type.procedure:Procédure`,
  WORK_INSTRUCTION: $localize`:@@documents.type.work-instruction:Mode opératoire`,
  RECORD:           $localize`:@@documents.type.record:Enregistrement`,
  FORM:             $localize`:@@documents.type.form:Formulaire`,
  MANUAL:           $localize`:@@documents.type.manual:Manuel`,
  EXTERNAL:         $localize`:@@documents.type.external:Document externe`,
  OTHER:            $localize`:@@documents.type.other:Autre`
};

export const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[];

export const DISPLAY_STATUS_LABELS: Record<DisplayStatus, string> = {
  IN_FORCE:       $localize`:@@documents.status.in-force:En vigueur`,
  IN_REVISION:    $localize`:@@documents.status.in-revision:En révision`,
  DRAFT:          $localize`:@@documents.status.draft:Brouillon`,
  REVIEW_OVERDUE: $localize`:@@documents.status.review-overdue:Revue dépassée`,
  ARCHIVED:       $localize`:@@documents.status.archived:Archivé`
};

/** Les statuts proposés au filtre ; « Archivé » a sa propre vue. */
export const FILTERABLE_STATUSES: DisplayStatus[] = ['IN_FORCE', 'IN_REVISION', 'DRAFT', 'REVIEW_OVERDUE'];

export const VERSION_STATUS_LABELS: Record<VersionStatus, string> = {
  DRAFT:     $localize`:@@documents.version.draft:Brouillon`,
  IN_REVIEW: $localize`:@@documents.version.in-review:En revue`,
  APPROVED:  $localize`:@@documents.version.approved:Approuvée`,
  PUBLISHED: $localize`:@@documents.version.published:Actuelle`,
  OBSOLETE:  $localize`:@@documents.version.obsolete:Obsolète`
};

export const VIEW_LABELS: Record<DocumentView, string> = {
  ALL:         $localize`:@@documents.view.all:Tous les documents`,
  MINE:        $localize`:@@documents.view.mine:Mes documents`,
  TO_VALIDATE: $localize`:@@documents.view.to-validate:À valider par moi`,
  REVIEW_DUE:  $localize`:@@documents.view.review-due:Revues à échéance`,
  ARCHIVED:    $localize`:@@documents.view.archived:Archives (obsolètes)`
};

/** Le statut affiché d'un document, déduit côté client si le serveur ne l'a pas fourni. */
export function displayStatusOf(d: { status: string; displayStatus?: DisplayStatus }): DisplayStatus {
  if (d.displayStatus) return d.displayStatus;
  return d.status === 'ARCHIVED' ? 'ARCHIVED' : 'DRAFT';
}

/** Taille lisible : 1,2 Mo, 340 Ko. */
export function formatBytes(bytes: number | null | undefined, locale = 'fr-FR'): string {
  if (bytes === null || bytes === undefined) return '—';
  const units = [
    $localize`:@@documents.unit.bytes:o`,
    $localize`:@@documents.unit.kb:Ko`,
    $localize`:@@documents.unit.mb:Mo`,
    $localize`:@@documents.unit.gb:Go`
  ];
  let value = bytes;
  let i = 0;
  while (value >= 1024 && i < units.length - 1) {
    value /= 1024;
    i++;
  }
  const digits = i === 0 || value >= 100 ? 0 : 1;
  return `${value.toLocaleString(locale, { maximumFractionDigits: digits })} ${units[i]}`;
}
