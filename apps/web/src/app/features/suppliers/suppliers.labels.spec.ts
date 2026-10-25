import { HttpHeaders, HttpResponse } from '@angular/common/http';

import {
  CATEGORIES_BY_OWNER,
  attachmentName,
  documentCategoryLabel,
  documentIcon,
  fileSize,
  ncLockReason,
  ncSubject,
  ownerTypeLabel,
  saveBlob
} from './suppliers.labels';

describe('suppliers.labels', () => {

  it('verrouille les documents d\'une NC résolue ou rejetée, pas d\'une NC ouverte', () => {
    expect(ncLockReason({ status: 'RESOLVED' })).toContain('close');
    expect(ncLockReason({ status: 'REJECTED' })).toBeTruthy();
    expect(ncLockReason({ status: 'OPEN' })).toBeNull();
    expect(ncLockReason({ status: 'IN_REVIEW' })).toBeNull();
  });

  it('résume une NC par sa pièce, sa quantité et sa date', () => {
    expect(ncSubject({ partNumber: 'PN-1', partQuantity: 5, detectedOn: '2026-10-20' }))
      .toBe('NC PN-1 · 5 pièce(s) · 2026-10-20');
    expect(ncSubject({ partNumber: undefined, partQuantity: undefined, detectedOn: '2026-10-20' }))
      .toBe('NC — · 2026-10-20');
  });

  it('nomme natures et porteurs, et rend tel quel ce qu\'il ne connaît pas', () => {
    expect(documentCategoryLabel('CAPA_EVIDENCE')).toBe('Preuve de CAPA');
    expect(documentCategoryLabel('INCONNU')).toBe('INCONNU');
    expect(documentCategoryLabel(null)).toBe('—');
    expect(ownerTypeLabel('NON_CONFORMITY')).toBe('Non-conformité');
    expect(ownerTypeLabel('X')).toBe('X');
    expect(ownerTypeLabel(undefined)).toBe('—');
    expect(CATEGORIES_BY_OWNER.CERTIFICATE).not.toContain('EIGHT_D');
  });

  it('écrit une taille lisible, jamais zéro', () => {
    expect(fileSize(10)).toBe('1 Ko');
    expect(fileSize(5 * 1024)).toBe('5 Ko');
    expect(fileSize(1.5 * 1024 * 1024)).toBe('1.5 Mo');
  });

  it('choisit l\'icône selon le type', () => {
    expect(documentIcon('image/jpeg')).toBe('image');
    expect(documentIcon('application/pdf')).toBe('picture_as_pdf');
    expect(documentIcon('application/zip')).toBe('description');
  });

  it('reprend le nom de fichier annoncé par le serveur, sinon celui proposé', () => {
    const named = new HttpResponse<Blob>({
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="nc-fournisseur-1.pdf"' })
    });
    const encoded = new HttpResponse<Blob>({
      headers: new HttpHeaders({ 'Content-Disposition': "attachment; filename*=UTF-8''fiche%20NC.pdf" })
    });
    expect(attachmentName(named, 'x.pdf')).toBe('nc-fournisseur-1.pdf');
    expect(attachmentName(encoded, 'x.pdf')).toBe('fiche NC.pdf');
    expect(attachmentName(new HttpResponse<Blob>({}), 'x.pdf')).toBe('x.pdf');
  });

  it('fait enregistrer un fichier reçu sans quitter la page', () => {
    const click = spyOn(HTMLAnchorElement.prototype, 'click');
    const revoke = spyOn(URL, 'revokeObjectURL').and.callThrough();
    saveBlob(new Blob(['x']), 'a.pdf');
    expect(click).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalled();
  });
});
