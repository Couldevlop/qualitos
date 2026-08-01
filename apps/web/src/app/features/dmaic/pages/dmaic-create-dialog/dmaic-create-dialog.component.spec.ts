import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DmaicProjectResponse } from '../../dmaic.types';
import { DmaicCreateDialogComponent } from './dmaic-create-dialog.component';

/**
 * Le Black Belt du projet n'est jamais saisi : il vient du JWT (§18.2 #2).
 * Sans session valide, la création doit être refusée côté client plutôt que
 * partir au serveur avec un porteur vide.
 */
describe('DmaicCreateDialogComponent', () => {
  let component: DmaicCreateDialogComponent;
  let fixture: ComponentFixture<DmaicCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DmaicCreateDialogComponent, DmaicProjectResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/dmaic/projects`;
  const USER = '11111111-1111-1111-1111-111111111111';

  const created: DmaicProjectResponse = {
    id: 'dm-1', tenantId: 't1', title: 'Rebut ligne A', phase: 'DEFINE', status: 'ACTIVE',
    blackBeltId: USER, measureCount: 0, pokaYokeCount: 0,
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: USER, tenantId: 't1', displayName: 'BB', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<DmaicCreateDialogComponent, DmaicProjectResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DmaicCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DmaicCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige un titre et refuse de soumettre un formulaire vide', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un titre au-delà de la limite serveur', () => {
    component.form.controls.title.setValue('x'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(url);
  });

  it('bloque la création quand la session a expiré', () => {
    currentUser = null;
    component.form.controls.title.setValue('Projet');
    component.submit();
    http.expectNone(url);
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('prend le Black Belt dans le JWT et omet les champs laissés vides', () => {
    component.form.patchValue({
      title: '  Réduire le rebut  ',
      problemStatement: '   ',
      goalStatement: '  Atteindre 1,2 %  ',
      championId: '',
      specLowerLimit: 9.95,
      specUpperLimit: 10.05,
      specUnit: '  mm  ',
      estimatedSavingsEur: 84000
    });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Réduire le rebut');
    expect(req.request.body.blackBeltId).toBe(USER);
    expect(req.request.body.problemStatement).toBeUndefined();
    expect(req.request.body.goalStatement).toBe('Atteindre 1,2 %');
    expect(req.request.body.championId).toBeUndefined();
    expect(req.request.body.specUnit).toBe('mm');
    expect(req.request.body.specLowerLimit).toBe(9.95);
    expect(req.request.body.estimatedSavingsEur).toBe(84000);

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.title.setValue('Projet');
    component.submit();
    component.submit();

    const req = http.expectOne(url);
    expect(component.submitting).toBeTrue();
    req.flush(created);
    expect(component.submitting).toBeFalse();
  });

  it('garde le dialogue ouvert quand le serveur refuse la création', () => {
    component.form.controls.title.setValue('Projet');
    component.submit();
    http.expectOne(url).flush({ title: 'invalid' }, { status: 400, statusText: 'Bad Request' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue sans rien envoyer à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
