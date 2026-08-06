import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcResponse } from '../../nc.types';
import { NcResolveDialogComponent, NcResolveDialogData } from './nc-resolve-dialog.component';

/**
 * La note de résolution est la preuve d'efficacité attachée à la NC (§4.3) :
 * elle est obligatoire, et une résolution refusée par le serveur ne doit pas
 * fermer le dialogue (sinon la saisie de l'opérateur est perdue).
 */
describe('NcResolveDialogComponent', () => {
  let component: NcResolveDialogComponent;
  let fixture: ComponentFixture<NcResolveDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<NcResolveDialogComponent, NcResponse>>;
  let prevMock: boolean;

  const data: NcResolveDialogData = { ncId: 'a1', reference: 'NC-2026-1001' };
  const resolveUrl = `${environment.apiBaseUrl}/api/v1/nc/a1/resolve`;

  const resolved: NcResponse = {
    id: 'a1', reference: 'NC-2026-1001', title: 'Étiquetage manquant',
    category: 'PROCESS', severity: 'MAJOR', status: 'RESOLVED', origin: 'INTERNAL',
    detectedAt: '2026-07-01T00:00:00Z', createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z', resolutionNote: 'Étiquettes reposées.'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<NcResolveDialogComponent, NcResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [NcResolveDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NcResolveDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('rappelle la référence de la NC traitée', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.ref')?.textContent).toContain('NC-2026-1001');
  });

  it('exige une note de résolution avant tout envoi', () => {
    expect(component.form.controls.resolutionNote.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(resolveUrl);
    expect(component.form.controls.resolutionNote.touched).toBeTrue();
  });

  it('refuse une note au-delà de 2000 caractères', () => {
    component.form.controls.resolutionNote.setValue('x'.repeat(2001));
    expect(component.form.controls.resolutionNote.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(resolveUrl);
  });

  it('poste la note nettoyée sur la NC et renvoie la NC résolue', () => {
    component.form.controls.resolutionNote.setValue('  Étiquettes reposées.  ');
    component.submit();

    const req = http.expectOne(resolveUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resolutionNote: 'Étiquettes reposées.' });

    req.flush(resolved);
    expect(dialogRef.close).toHaveBeenCalledWith(resolved);
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.resolutionNote.setValue('Note');
    component.submit();
    component.submit();

    http.expectOne(resolveUrl).flush(resolved);
    expect(dialogRef.close).toHaveBeenCalledTimes(1);
  });

  it('conserve la saisie quand le serveur refuse la transition (409)', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.controls.resolutionNote.setValue('Note');
    component.submit();
    http.expectOne(resolveUrl).flush({ title: 'Conflict' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.resolutionNote.value).toBe('Note');
    expect(component.submitting).toBeFalse();
    expect(snackSpy).toHaveBeenCalledWith(
      'État incompatible — rechargez la page.', 'OK', { duration: 4000 });
  });

  it('affiche un message de droits sur 403 sans détail technique', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.controls.resolutionNote.setValue('Note');
    component.submit();
    http.expectOne(resolveUrl).flush(
      { title: 'AccessDeniedException at com.qualitos...' },
      { status: 403, statusText: 'Forbidden' });

    expect(snackSpy).toHaveBeenCalledWith(
      'Vous n\'avez pas les droits pour cette action.', 'OK', { duration: 4000 });
  });

  it('ferme le dialogue sans résultat à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
