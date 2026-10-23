import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { CompetencyMatrixService } from './competency-matrix.service';
import { CompetencyMatrixView } from './competency-matrix.types';

/** Les appels de la matrice : verbes, chemins, corps — et jamais de client dans le corps. */
describe('CompetencyMatrixService', () => {
  let service: CompetencyMatrixService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/training/competencies`;
  const vue: CompetencyMatrixView = {
    workshops: ['Usinage'], defaultWorkshops: true, operators: [], skills: [], cells: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CompetencyMatrixService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lit la matrice', () => {
    let lu: CompetencyMatrixView | undefined;
    service.matrix().subscribe(v => lu = v);
    const req = http.expectOne(`${base}/matrix`);
    expect(req.request.method).toBe('GET');
    req.flush(vue);
    expect(lu).toEqual(vue);
  });

  it('inscrit et retire un opérateur', () => {
    service.addOperator({ userId: 'u-1', userName: 'Martin', workshop: 'Usinage' }).subscribe();
    const post = http.expectOne(`${base}/matrix/operators`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ userId: 'u-1', userName: 'Martin', workshop: 'Usinage' });
    expect(post.request.body.tenantId).toBeUndefined();
    post.flush(vue);

    service.removeOperator('op/1').subscribe();
    const del = http.expectOne(`${base}/matrix/operators/op%2F1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(vue);
  });

  it('ajoute et retire une compétence avec son formateur', () => {
    const corps = { name: 'Tournage', category: 'Usinage', trainerUserId: 'u-9', trainerName: 'Karim' };
    service.addSkill(corps).subscribe();
    const post = http.expectOne(`${base}/matrix/skills`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(corps);
    post.flush(vue);

    service.removeSkill('s-1').subscribe();
    const del = http.expectOne(`${base}/matrix/skills/s-1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(vue);
  });

  it('pose un niveau absolu', () => {
    service.setLevel({ userId: 'u-1', skillId: 's-1', level: 3 }).subscribe();
    const put = http.expectOne(`${base}/matrix/levels`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ userId: 'u-1', skillId: 's-1', level: 3 });
    put.flush({ userId: 'u-1', skillId: 's-1', level: 3 });
  });

  it('lit et remplace les ateliers', () => {
    service.workshops().subscribe();
    http.expectOne(`${base}/workshops`).flush({ names: ['Usinage'], defaults: true });

    service.updateWorkshops(['Peinture']).subscribe();
    const put = http.expectOne(`${base}/workshops`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ names: ['Peinture'] });
    put.flush({ names: ['Peinture'], defaults: false });
  });
});
