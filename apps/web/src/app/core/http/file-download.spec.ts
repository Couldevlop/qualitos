import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { filenameFromResponse, saveBlobAs } from './file-download';

/**
 * Le nom du fichier téléchargé vient du serveur, pas du navigateur : codé en dur
 * côté écran, il garde l'ancien nom le jour où le serveur le change.
 */
describe('file-download', () => {

  function reponse(disposition: string | null): HttpResponse<Blob> {
    return new HttpResponse<Blob>({
      body: new Blob(['x']),
      headers: disposition === null
        ? new HttpHeaders()
        : new HttpHeaders({ 'Content-Disposition': disposition })
    });
  }

  describe('filenameFromResponse', () => {

    it('lit le nom entre guillemets', () => {
      expect(filenameFromResponse(reponse('attachment; filename="rapport.xlsx"')))
        .toBe('rapport.xlsx');
    });

    it('lit le nom sans guillemets', () => {
      expect(filenameFromResponse(reponse('attachment; filename=rapport.xlsx')))
        .toBe('rapport.xlsx');
    });

    // Un nom accentué voyage encodé : le rendre tel quel donnerait « rapport%20qualit%C3%A9 ».
    it('décode un nom encodé en UTF-8', () => {
      expect(filenameFromResponse(reponse("attachment; filename*=UTF-8''rapport%20qualit%C3%A9.xlsx")))
        .toBe('rapport qualité.xlsx');
    });

    it('rend null quand le serveur ne nomme rien', () => {
      expect(filenameFromResponse(reponse(null))).toBeNull();
      expect(filenameFromResponse(reponse('attachment'))).toBeNull();
    });
  });

  describe('saveBlobAs', () => {

    // L'URL temporaire retenue ferait fuir le contenu du fichier en mémoire
    // pour toute la durée de la session.
    it('libère l’URL temporaire aussitôt le clic déclenché', () => {
      const lien = document.createElement('a');
      spyOn(document, 'createElement').and.returnValue(lien);
      const clic = spyOn(lien, 'click');
      spyOn(URL, 'createObjectURL').and.returnValue('blob:faux');
      const liberee = spyOn(URL, 'revokeObjectURL');

      saveBlobAs(new Blob(['x']), 'rapport.xlsx');

      expect(lien.download).toBe('rapport.xlsx');
      expect(lien.href).toContain('blob:faux');
      expect(clic).toHaveBeenCalled();
      expect(liberee).toHaveBeenCalledWith('blob:faux');
    });
  });
});
