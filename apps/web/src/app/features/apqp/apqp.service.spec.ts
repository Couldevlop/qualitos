import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ApqpService } from './apqp.service';
import { ApqpCycle, ApqpProject } from './apqp.types';

/**
 * Le contrat HTTP des projets APQP.
 *
 * <p>Ce que ce banc tient : les routes du cycle sont NICHÉES sous leur projet
 * (une route globale n'aurait pas su duquel elle parlait), la lecture rend le
 * cycle ET l'état du dossier PPAP (deux appels séparés pourraient se répondre
 * sur deux états différents), et une pièce part en multipart sous le champ que le
 * serveur lit — un autre nom produirait un 400 sans rien dire de lisible.
 */
describe('ApqpService', () => {

  let service: ApqpService;
  let http: HttpTestingController;

  const projets = `${environment.apiBaseUrl}/api/v1/apqp/projects`;
  const racine = `${projets}/pr1/phases`;

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

  it('liste les projets du client', () => {
    let recus: ApqpProject[] | undefined;
    service.projects().subscribe(projs => (recus = projs));

    const req = http.expectOne(projets);
    expect(req.request.method).toBe('GET');
    req.flush([{
      id: 'pr1', name: 'Support moteur', type: 'NPI', customer: 'Renault',
      reference: 'SM-2026', description: null,
      deliverablesTotal: 12, deliverablesDone: 3, ppapTotal: 5, ppapDone: 1,
      createdAt: '2026-09-01T08:00:00Z', updatedAt: '2026-09-10T08:00:00Z'
    }]);

    expect(recus?.length).toBe(1);
    expect(recus?.[0].ppapTotal).toBe(5);
  });

  it('crée, modifie et supprime un projet', () => {
    service.createProject({ name: 'Transfert ligne 4', type: 'TOW' }).subscribe();
    const creation = http.expectOne(projets);
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ name: 'Transfert ligne 4', type: 'TOW' });
    creation.flush({});

    service.updateProject('pr1', { name: 'Transfert ligne 4', type: 'TOW' }).subscribe();
    const mise = http.expectOne(`${projets}/pr1`);
    expect(mise.request.method).toBe('PUT');
    mise.flush({});

    service.deleteProject('pr1').subscribe();
    const retrait = http.expectOne(`${projets}/pr1`);
    expect(retrait.request.method).toBe('DELETE');
    retrait.flush(null);
  });

  it('rend le cycle, son projet et l\'état du dossier PPAP ensemble', () => {
    let recu: ApqpCycle | undefined;
    service.cycle('pr1').subscribe(cycle => (recu = cycle));

    const req = http.expectOne(racine);
    expect(req.request.method).toBe('GET');
    req.flush({
      projectId: 'pr1', projectName: 'Support moteur', projectType: 'NPI',
      customer: 'Renault', phases: [], ppapDone: 2, ppapTotal: 12
    });

    // Le projet voyage avec le cycle : l'en-tête l'affiche sans second appel.
    expect(recu?.projectName).toBe('Support moteur');
    expect(recu?.ppapDone).toBe(2);
    expect(recu?.ppapTotal).toBe(12);
  });

  it('déclare où en est un livrable et reçoit le cycle entier', () => {
    service.completeDeliverable('pr1', 'p1', 'd1', { done: true, comment: 'reçu' }).subscribe();

    const req = http.expectOne(`${racine}/p1/deliverables/d1/completion`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.done).toBeTrue();
    // Ni l'heure ni l'auteur : le serveur les pose depuis le jeton.
    expect('doneAt' in req.request.body).toBeFalse();
    expect('doneBy' in req.request.body).toBeFalse();
    req.flush({
      projectId: 'pr1', projectName: 'Support moteur', projectType: 'NPI',
      customer: null, phases: [], ppapDone: 1, ppapTotal: 12
    });
  });

  it('verse une pièce en multipart, sous le champ que le serveur lit', () => {
    const fichier = new File([new Uint8Array([1, 2, 3])], 'plan.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    });

    service.uploadEvidence('pr1', 'p1', 'd1', fichier).subscribe();

    const req = http.expectOne(`${racine}/p1/deliverables/d1/evidences`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('file')).toBe(fichier);
    req.flush({});
  });

  it('lit et retire les pièces d\'un livrable', () => {
    service.evidences('pr1', 'p1', 'd1').subscribe();
    http.expectOne(`${racine}/p1/deliverables/d1/evidences`).flush([]);

    service.deleteEvidence('pr1', 'p1', 'd1', 'e9').subscribe();
    const req = http.expectOne(`${racine}/p1/deliverables/d1/evidences/e9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('réinitialise le cycle du projet par un POST dédié, pas par une suppression en boucle', () => {
    service.reset('pr1').subscribe();

    const req = http.expectOne(`${racine}/reset`);
    expect(req.request.method).toBe('POST');
    req.flush({
      projectId: 'pr1', projectName: 'Support moteur', projectType: 'NPI',
      customer: null, phases: [], ppapDone: 0, ppapTotal: 12
    });
  });

  it('crée un livrable avec son artefact attendu et sa marque PPAP', () => {
    service.addDeliverable('pr1', 'p1', {
      label: 'Accord client', expectedArtifact: 'Formulaire PSW signé', ppap: true
    }).subscribe();

    const req = http.expectOne(`${racine}/p1/deliverables`);
    // Le genre a disparu : un livrable n'est plus qualifié avant qu'on sache ce
    // qu'on en fera, et tous ouvrent le même formulaire.
    expect(req.request.body).toEqual({
      label: 'Accord client', expectedArtifact: 'Formulaire PSW signé', ppap: true
    });
    req.flush({});
  });
});
