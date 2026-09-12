import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ApqpService } from './apqp.service';
import { ApqpCycle } from './apqp.types';

/**
 * Le contrat HTTP du cycle APQP.
 *
 * <p>Ce que ce banc tient : la lecture rend le cycle ET l'état du dossier PPAP
 * (deux appels séparés pourraient se répondre sur deux états différents), et une
 * pièce part en multipart sous le champ que le serveur lit — un autre nom
 * produirait un 400 sans rien dire de lisible.
 */
describe('ApqpService', () => {

  let service: ApqpService;
  let http: HttpTestingController;

  const racine = `${environment.apiBaseUrl}/api/v1/apqp/phases`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ApqpService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('rend le cycle et l\'état du dossier PPAP ensemble', () => {
    let recu: ApqpCycle | undefined;
    service.cycle().subscribe(cycle => (recu = cycle));

    const req = http.expectOne(racine);
    expect(req.request.method).toBe('GET');
    req.flush({ phases: [], ppapDone: 2, ppapTotal: 12 });

    expect(recu?.ppapDone).toBe(2);
    expect(recu?.ppapTotal).toBe(12);
  });

  it('déclare l\'achèvement d\'un livrable et reçoit le cycle entier', () => {
    service.completeDeliverable('p1', 'd1', { done: true, comment: 'reçu' }).subscribe();

    const req = http.expectOne(`${racine}/p1/deliverables/d1/completion`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.done).toBeTrue();
    // Ni l'heure ni l'auteur : le serveur les pose depuis le jeton.
    expect('doneAt' in req.request.body).toBeFalse();
    expect('doneBy' in req.request.body).toBeFalse();
    req.flush({ phases: [], ppapDone: 1, ppapTotal: 12 });
  });

  it('verse une pièce en multipart, sous le champ que le serveur lit', () => {
    const fichier = new File([new Uint8Array([1, 2, 3])], 'plan.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    });

    service.uploadEvidence('p1', 'd1', fichier).subscribe();

    const req = http.expectOne(`${racine}/p1/deliverables/d1/evidences`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('file')).toBe(fichier);
    req.flush({});
  });

  it('lit et retire les pièces d\'un livrable', () => {
    service.evidences('p1', 'd1').subscribe();
    http.expectOne(`${racine}/p1/deliverables/d1/evidences`).flush([]);

    service.deleteEvidence('p1', 'd1', 'e9').subscribe();
    const req = http.expectOne(`${racine}/p1/deliverables/d1/evidences/e9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('réinitialise le cycle par un POST dédié, pas par une suppression en boucle', () => {
    service.reset().subscribe();

    const req = http.expectOne(`${racine}/reset`);
    expect(req.request.method).toBe('POST');
    req.flush({ phases: [], ppapDone: 0, ppapTotal: 12 });
  });

  it('crée un livrable avec son genre et sa marque PPAP', () => {
    service.addDeliverable('p1', {
      label: 'Customer sign-off', ppap: true, kind: 'ATTACHMENT'
    }).subscribe();

    const req = http.expectOne(`${racine}/p1/deliverables`);
    expect(req.request.body).toEqual({
      label: 'Customer sign-off', ppap: true, kind: 'ATTACHMENT'
    });
    req.flush({});
  });
});
