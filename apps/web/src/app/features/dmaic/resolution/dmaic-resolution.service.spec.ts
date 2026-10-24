import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { DmaicResolutionService } from './dmaic-resolution.service';
import { emptyContent } from './dmaic-resolution.types';

const PROJET = '11111111-1111-4111-8111-111111111111';

describe('DmaicResolutionService', () => {
  let service: DmaicResolutionService;
  let http: HttpTestingController;
  let prevMock: boolean;
  const base = `${environment.apiBaseUrl}/api/v1/dmaic`;

  function setup(mock: boolean): void {
    prevMock = environment.useMockApi;
    environment.useMockApi = mock;
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DmaicResolutionService);
    http = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  describe('avec le serveur', () => {
    beforeEach(() => setup(false));

    it('lit et enregistre la fiche du projet', () => {
      service.sheet(PROJET).subscribe();
      expect(http.expectOne(`${base}/projects/${PROJET}/resolution`).request.method).toBe('GET');

      const contenu = emptyContent();
      service.save(PROJET, contenu).subscribe();
      const put = http.expectOne(`${base}/projects/${PROJET}/resolution`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toBe(contenu);
    });

    it('verse une photo en multipart, avec ou sans remarque, puis la commente et la retire', () => {
      const f = new File(['x'], 'a.png', { type: 'image/png' });
      service.uploadPhoto(PROJET, 2, f, 'vue').subscribe();
      const post = http.expectOne(`${base}/projects/${PROJET}/resolution/photos/2`);
      expect(post.request.method).toBe('POST');
      const corps = post.request.body as FormData;
      expect(corps.get('file')).toBeTruthy();
      expect(corps.get('remark')).toBe('vue');

      service.uploadPhoto(PROJET, 3, f, null).subscribe();
      expect((http.expectOne(`${base}/projects/${PROJET}/resolution/photos/3`).request.body as FormData).has('remark')).toBeFalse();

      service.updateRemark(PROJET, 2, 'face').subscribe();
      const patch = http.expectOne(`${base}/projects/${PROJET}/resolution/photos/2`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ remark: 'face' });

      service.deletePhoto(PROJET, 2).subscribe();
      expect(http.expectOne(`${base}/projects/${PROJET}/resolution/photos/2`).request.method).toBe('DELETE');
    });

    it('télécharge le rapport client en binaire', () => {
      service.claimReport(PROJET).subscribe();
      const req = http.expectOne(`${base}/projects/${PROJET}/resolution/claim-report`);
      expect(req.request.responseType).toBe('blob');
    });

    it('lit, remplace et rétablit le paramétrage', () => {
      service.settings().subscribe();
      expect(http.expectOne(`${base}/resolution-settings`).request.method).toBe('GET');

      service.replaceSettings([], [], []).subscribe();
      const put = http.expectOne(`${base}/resolution-settings`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual({ questions: [], failureTypes: [], severities: [] });

      service.resetSettings().subscribe();
      expect(http.expectOne(`${base}/resolution-settings/reset`).request.method).toBe('POST');
    });

    it('échappe l\'identifiant du projet dans l\'adresse', () => {
      service.sheet('a/b').subscribe();
      http.expectOne(`${base}/projects/a%2Fb/resolution`);
    });
  });

  describe('sans serveur (démonstration)', () => {
    beforeEach(() => setup(true));

    it('rend une fiche vierge, puis celle qu\'on a enregistrée', async () => {
      const vierge = await firstValueFrom(service.sheet(PROJET));
      expect(vierge.saved).toBeFalse();
      expect(vierge.settings.questions.length).toBeGreaterThan(0);

      const enregistree = await firstValueFrom(service.save(PROJET, { ...emptyContent(), clientName: 'Acme' }));
      expect(enregistree.saved).toBeTrue();
      expect(enregistree.content.clientName).toBe('Acme');
    });

    it('tient les photos par emplacement', async () => {
      spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
      const f = new File(['x'], 'a.png', { type: 'image/png' });
      await firstValueFrom(service.uploadPhoto(PROJET, 4, f, null));
      await firstValueFrom(service.uploadPhoto(PROJET, 1, f, 'r'));
      expect((await firstValueFrom(service.sheet(PROJET))).photos.map(p => p.slot)).toEqual([1, 4]);

      expect((await firstValueFrom(service.updateRemark(PROJET, 4, 'face'))).remark).toBe('face');
      await expectAsync(firstValueFrom(service.updateRemark(PROJET, 6, 'x'))).toBeRejected();

      await firstValueFrom(service.deletePhoto(PROJET, 1));
      expect((await firstValueFrom(service.sheet(PROJET))).photos.map(p => p.slot)).toEqual([4]);
    });

    it('produit un rapport et garde le paramétrage remplacé jusqu\'au retour aux défauts', async () => {
      const rapport = await firstValueFrom(service.claimReport(PROJET));
      expect(rapport.headers.get('Content-Disposition')).toContain('rapport-client-dmaic.pdf');

      const remplace = await firstValueFrom(service.replaceSettings([], [], [{ level: 1, label: 'Grave' }]));
      expect(remplace.defaults).toBeFalse();
      expect(remplace.severities).toEqual([{ level: 1, label: 'Grave' }]);
      const retabli = await firstValueFrom(service.resetSettings());
      expect(retabli.defaults).toBeTrue();
      expect(retabli.severities.length).toBe(3);
    });
  });
});
