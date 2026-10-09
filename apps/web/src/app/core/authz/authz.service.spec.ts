import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { SharedModule } from '../../shared/shared.module';
import { roleLabel, moduleLabel, permissionLabel } from './authz.labels';
import { AuthzService } from './authz.service';

describe('AuthzService', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/authz`;
  let service: AuthzService;
  let http: HttpTestingController;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthzService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
  });

  it('lit les droits une fois, et répond à chaque demande', () => {
    const vus: boolean[] = [];
    service.can('capa.create').subscribe(v => vus.push(v));
    service.can(['nc.close', 'nc.create']).subscribe(v => vus.push(v));

    http.expectOne(`${endpoint}/me`).flush({ userId: 'u', roles: ['USER'], permissions: ['nc.create'] });

    expect(vus).toEqual([false, true]);
  });

  it('relit après un changement de droits', () => {
    const vus: boolean[] = [];
    service.can('capa.create').subscribe(v => vus.push(v));
    http.expectOne(`${endpoint}/me`).flush({ userId: 'u', roles: ['USER'], permissions: [] });

    service.refresh();
    http.expectOne(`${endpoint}/me`).flush({ userId: 'u', roles: ['PILOTE'], permissions: ['capa.create'] });

    expect(vus).toEqual([false, true]);
  });

  it('une lecture en échec n’affiche aucun droit', () => {
    let vu: boolean | undefined;
    service.can('nc.create').subscribe(v => vu = v);
    http.expectOne(`${endpoint}/me`).flush('x', { status: 503, statusText: 'KO' });
    expect(vu).toBeFalse();
  });

  it('l’administration appelle les bonnes adresses, codes encodés', () => {
    service.catalog().subscribe();
    service.roles().subscribe();
    service.createRole({ code: 'PILOTE', name: 'P', permissions: [] }).subscribe();
    service.updateRole('PILOTE', { permissions: ['nc.close'] }).subscribe();
    service.deleteRole('PILOTE').subscribe();
    service.members().subscribe();
    service.setMemberRoles('a/b', ['PILOTE']).subscribe();

    const req = (method: string, url: string) => http.expectOne(r => r.method === method && r.url === url);
    req('GET', `${endpoint}/catalog`).flush([]);
    req('GET', `${endpoint}/roles`).flush([]);
    const creation = req('POST', `${endpoint}/roles`);
    expect(creation.request.body.code).toBe('PILOTE');
    creation.flush({});
    req('PUT', `${endpoint}/roles/PILOTE`).flush({});
    req('DELETE', `${endpoint}/roles/PILOTE`).flush(null);
    req('GET', `${endpoint}/members`).flush([]);
    const membre = req('PUT', `${endpoint}/members/a%2Fb/roles`);
    expect(membre.request.body).toEqual({ roles: ['PILOTE'] });
    membre.flush({});
  });

  it('en démonstration sans serveur, tout est permis', () => {
    environment.useMockApi = true;
    let vu: boolean | undefined;
    service.can('authz.manage').subscribe(v => vu = v);
    expect(vu).toBeTrue();
  });

  it('libellés : un code inconnu reste lisible', () => {
    expect(permissionLabel('capa.create')).toBe('Ouvrir un dossier CAPA');
    expect(permissionLabel('module.inconnu')).toBe('module.inconnu');
    expect(moduleLabel('nc')).toBe('Non-conformités');
    expect(moduleLabel('autre')).toBe('autre');
    expect(roleLabel('QUALITY_MANAGER')).toBe('Manager qualité');
    expect(roleLabel('QUALITY_MANAGER', 'Pilote qualité')).toBe('Pilote qualité');
    expect(roleLabel('PILOTE')).toBe('PILOTE');
  });
});

@Component({
  template: `
    <button *qosCan="'capa.create'" id="creer">Créer</button>
    <span *qosCan="['nc.close', 'nc.reject']; else interdit" id="clore">Clore</span>
    <ng-template #interdit><span id="interdit">Lecture seule</span></ng-template>`,
  standalone: false
})
class HoteComponent {}

describe('CanDirective', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/authz`;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
  });

  afterEach(() => environment.useMockApi = prevMock);

  function monter(permissions: string[]): HTMLElement {
    TestBed.configureTestingModule({
      declarations: [HoteComponent],
      imports: [SharedModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(HoteComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    // Avant la lecture des droits, rien ne s'affiche — pas même la branche « sinon ».
    expect(el.querySelector('#creer')).toBeNull();
    expect(el.querySelector('#interdit')).toBeNull();
    TestBed.inject(HttpTestingController).expectOne(`${endpoint}/me`)
      .flush({ userId: 'u', roles: [], permissions });
    fixture.detectChanges();
    return el;
  }

  it('affiche ce que l’utilisateur peut faire', () => {
    const el = monter(['capa.create', 'nc.reject']);
    expect(el.querySelector('#creer')).not.toBeNull();
    expect(el.querySelector('#clore')).not.toBeNull();
    expect(el.querySelector('#interdit')).toBeNull();
  });

  it('cache le reste, et montre la branche « sinon »', () => {
    const el = monter([]);
    expect(el.querySelector('#creer')).toBeNull();
    expect(el.querySelector('#clore')).toBeNull();
    expect(el.querySelector('#interdit')).not.toBeNull();
  });
});
