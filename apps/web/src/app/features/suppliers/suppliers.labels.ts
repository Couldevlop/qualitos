import { HttpResponse } from '@angular/common/http';

import {
  NonConformityResponse,
  NonConformitySeverity,
  NonConformityStatus,
  SupplierDocumentCategory,
  SupplierDocumentOwnerType
} from './suppliers.types';

export const NC_STATUSES: NonConformityStatus[] = ['OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED'];
export const NC_SEVERITIES: NonConformitySeverity[] = ['MINOR', 'MAJOR', 'CRITICAL'];

/**
 * Une NC close (résolue ou rejetée) ne reçoit ni ne perd plus de pièce — le
 * miroir de la règle du serveur, dit plutôt qu'échoué en silence.
 */
export function ncLockReason(nc: Pick<NonConformityResponse, 'status'>): string | null {
  return nc.status === 'RESOLVED' || nc.status === 'REJECTED'
    ? $localize`:@@suppliers.documents.locked-closed:NC close : ses documents sont figés. Rouvrez-la pour en joindre.`
    : null;
}

/** La NC en clair, pour un titre : sa pièce et sa quantité. */
export function ncSubject(nc: Pick<NonConformityResponse, 'partNumber' | 'partQuantity' | 'detectedOn'>): string {
  const part = nc.partNumber ?? '—';
  return nc.partQuantity != null
    ? $localize`:@@suppliers.nc.subject:NC ${part}:part: · ${nc.partQuantity}:quantity: pièce(s) · ${nc.detectedOn}:date:`
    : $localize`:@@suppliers.nc.subject-no-quantity:NC ${part}:part: · ${nc.detectedOn}:date:`;
}

/**
 * Natures de document, dans l'ordre où on les propose. Le 8D en tête : c'est la
 * pièce qu'on attend du fournisseur sur une NC.
 */
export const DOCUMENT_CATEGORIES: ReadonlyArray<{ value: SupplierDocumentCategory; label: string }> = [
  { value: 'EIGHT_D',       label: $localize`:@@suppliers.documents.category-eight-d:8D fournisseur` },
  { value: 'CAPA_EVIDENCE', label: $localize`:@@suppliers.documents.category-capa:Preuve de CAPA` },
  { value: 'CERTIFICATE',   label: $localize`:@@suppliers.documents.category-certificate:Certificat` },
  { value: 'OTHER',         label: $localize`:@@suppliers.documents.category-other:Autre` }
];

/** Les natures qu'admet chaque porteur — le miroir de la règle du serveur. */
export const CATEGORIES_BY_OWNER: Record<SupplierDocumentOwnerType, SupplierDocumentCategory[]> = {
  NON_CONFORMITY: ['EIGHT_D', 'CAPA_EVIDENCE', 'OTHER'],
  CERTIFICATE: ['CERTIFICATE', 'OTHER']
};

export function documentCategoryLabel(c: SupplierDocumentCategory | string | null | undefined): string {
  if (!c) return '—';
  return DOCUMENT_CATEGORIES.find(x => x.value === c)?.label ?? String(c);
}

export const OWNER_TYPES: ReadonlyArray<{ value: SupplierDocumentOwnerType; label: string }> = [
  { value: 'NON_CONFORMITY', label: $localize`:@@suppliers.documents.owner-nc:Non-conformité` },
  { value: 'CERTIFICATE',    label: $localize`:@@suppliers.documents.owner-certificate:Certificat` }
];

export function ownerTypeLabel(t: SupplierDocumentOwnerType | string | null | undefined): string {
  if (!t) return '—';
  return OWNER_TYPES.find(x => x.value === t)?.label ?? String(t);
}

/** Taille lisible : 1,2 Mo plutôt que 1258291. */
export function fileSize(bytes: number): string {
  return bytes >= 1024 * 1024
    ? `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
    : `${Math.max(1, Math.round(bytes / 1024))} Ko`;
}

/** L'icône d'un document selon son type. */
export function documentIcon(contentType: string): string {
  if (contentType.startsWith('image/')) return 'image';
  if (contentType === 'application/pdf') return 'picture_as_pdf';
  return 'description';
}

/** Le nom de fichier qu'annonce le serveur, sinon celui proposé. */
export function attachmentName(response: HttpResponse<Blob>, fallback: string): string {
  const header = response.headers.get('Content-Disposition');
  const found = header ? /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(header) : null;
  return found ? decodeURIComponent(found[1]) : fallback;
}

/** Fait enregistrer un fichier reçu, sans quitter la page. */
export function saveBlob(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}
