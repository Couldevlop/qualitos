import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { IshikawaService } from './ishikawa.service';
import { IshikawaActionResponse } from './ishikawa.types';

/**
 * Plan d'actions d'un diagramme : identifier les causes est un moyen, décider qui
 * fait quoi est la fin. Ces appels sont ce qui fait vivre le tableau éditable du
 * détail — chaque cellule modifiée part seule.
 */
describe('IshikawaService (plan d’actions)', () => {
  let service: IshikawaService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/ishikawa`;
  const DIAGRAM = 'd-1';

  const action = (over: Partial<IshikawaActionResponse> = {}): IshikawaActionResponse => ({
    id: 'a-1', diagramId: DIAGRAM, label: 'Refaire le réglage', responsible: 'Karim',
    decidedOn: '2026-08-06', status: 'TODO',
    createdAt: '2026-08-06T08:00:00Z', updatedAt: '2026-08-06T08:00:00Z', ...over
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        IshikawaService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(IshikawaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les actions d’un diagramme', () => {
    let received: IshikawaActionResponse[] = [];
    service.listActions(DIAGRAM).subscribe(a => (received = a));

    const req = http.expectOne(`${base}/diagrams/${DIAGRAM}/actions`);
    expect(req.request.method).toBe('GET');
    req.flush([action()]);

    expect(received.length).toBe(1);
    expect(received[0].responsible).toBe('Karim');
  });

  it('ajoute une action au diagramme', () => {
    service.addAction(DIAGRAM, {
      label: 'Contrôler le lot', responsible: 'Sophie', decidedOn: '2026-08-06'
    }).subscribe();

    const req = http.expectOne(`${base}/diagrams/${DIAGRAM}/actions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.label).toBe('Contrôler le lot');
    req.flush(action());
  });

  it('n’envoie QUE le champ modifié', () => {
    // L'édition se fait cellule par cellule : transmettre tout l'objet écraserait
    // une valeur qu'un collègue vient de changer sur la ligne voisine.
    service.updateAction('a-1', { status: 'DONE' }).subscribe();

    const req = http.expectOne(`${base}/actions/a-1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'DONE' });
    req.flush(action({ status: 'DONE' }));
  });

  it('renomme une action sans toucher au reste', () => {
    service.updateAction('a-1', { label: 'Nouveau libellé' }).subscribe();

    const req = http.expectOne(`${base}/actions/a-1`);
    expect(req.request.body).toEqual({ label: 'Nouveau libellé' });
    req.flush(action({ label: 'Nouveau libellé' }));
  });

  it('supprime une action', () => {
    service.deleteAction('a-1').subscribe();

    const req = http.expectOne(`${base}/actions/a-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
