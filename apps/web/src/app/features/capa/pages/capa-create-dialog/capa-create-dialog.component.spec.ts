import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCaseResponse } from '../../capa.types';
import { CapaCreateDialogComponent } from './capa-create-dialog.component';

/**
 * §18.2 #2 — le propriétaire du cas vient du JWT, jamais d'une saisie : si la
 * session est perdue, la création doit être refusée côté client plutôt que de
 * partir sans propriétaire et de revenir en 400.
 */
describe('CapaCreateDialogComponent', () => {
  let component: CapaCreateDialogComponent;
  let fixture: ComponentFixture<CapaCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaCreateDialogComponent, CapaCaseResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;
  const OWNER = '11111111-1111-1111-1111-111111111111';

  const created: CapaCaseResponse = {
    id: 'c1', tenantId: 't1', title: 'Recalibration', type: 'CORRECTIVE',
    criticity: 'MEDIUM', status: 'OPEN', sourceType: 'INTERNAL', ownerId: OWNER,
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', actions: []
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: OWNER, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaCreateDialogComponent, CapaCaseResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [CapaCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('propose des valeurs par défaut exploitables sans saisie supplémentaire', () => {
    expect(component.form.controls.type.value).toBe('CORRECTIVE');
    expect(component.form.controls.criticity.value).toBe('MEDIUM');
    expect(component.form.controls.sourceType.value).toBe('INTERNAL');
    expect(component.types.map(t => t.value))
      .toEqual(['CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE']);
    expect(component.types.every(t => !!t.label)).toBeTrue();
    expect(component.criticities).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
  });

  it('couvre les six origines de cas prévues par le modèle', () => {
    expect(component.sourceTypes.map(s => s.value))
      .toEqual(['NON_CONFORMITY', 'AUDIT', 'COMPLAINT', 'INTERNAL', 'IOT_ALERT', 'OTHER']);
    expect(component.sourceTypes.every(s => !!s.label)).toBeTrue();
  });

  it('exige un titre et n\'envoie rien tant qu\'il manque', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(base);
    expect(component.form.controls.title.touched).toBeTrue();
    expect(component.submitting).toBeFalse();
  });

  it('refuse un titre au-delà de 255 caractères (contrainte serveur rejouée)', () => {
    component.form.controls.title.setValue('x'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(base);
  });

  it('bloque la création quand la session a expiré plutôt que d\'envoyer un cas sans propriétaire', () => {
    currentUser = null;
    component.form.controls.title.setValue('Recalibration');
    component.submit();
    http.expectNone(base);
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('normalise les champs texte et omet les optionnels vides', () => {
    component.form.patchValue({
      title: '  Recalibration robot  ',
      description: '   ',
      type: 'PREVENTIVE',
      criticity: 'CRITICAL',
      sourceType: 'AUDIT',
      sourceRef: '  AUD-2026-Q2  ',
      dueDate: ''
    });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Recalibration robot');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.sourceRef).toBe('AUD-2026-Q2');
    expect(req.request.body.dueDate).toBeUndefined();
    expect(req.request.body.type).toBe('PREVENTIVE');
    expect(req.request.body.criticity).toBe('CRITICAL');
    expect(req.request.body.sourceType).toBe('AUDIT');
    // Le propriétaire provient du JWT, pas du formulaire.
    expect(req.request.body.ownerId).toBe(OWNER);

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet l\'échéance quand elle est saisie', () => {
    component.form.patchValue({ title: 'Cas daté', dueDate: '2026-12-31' });
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.dueDate).toBe('2026-12-31');
    req.flush(created);
  });

  it('garde le dialogue ouvert et le formulaire réutilisable quand le serveur refuse', () => {
    component.form.controls.title.setValue('Recalibration');
    component.submit();
    http.expectOne(base).flush({ title: 'invalid' }, { status: 400, statusText: 'Bad Request' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(component.form.controls.title.value).toBe('Recalibration');
  });

  it('n\'envoie pas deux fois la même création sur un double clic', () => {
    component.form.controls.title.setValue('Recalibration');
    component.submit();
    const req = http.expectOne(base);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(base);

    req.flush(created);
  });

  // --- endiguement (§4.2, 8D étape D3) --------------------------------------

  it('permet d\'ouvrir un dossier d\'ENDIGUEMENT, distinct d\'un correctif', () => {
    // Sans cette valeur, un dossier qui a seulement bloqué le lot partait comme
    // « correctif » et se lisait comme un dossier où la cause avait été traitée.
    component.form.patchValue({ title: 'Lot 4471 bloqué', type: 'CONTAINMENT' });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.body.type).toBe('CONTAINMENT');
    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  // --- pièces jointes déposées dès l'ouverture ------------------------------

  const fichier = (nom: string, octets = 1024): File =>
    new File([new Uint8Array(octets)], nom, { type: 'application/pdf' });

  const choisir = (...files: File[]): void => {
    const input = { files, value: 'C:/faux' } as unknown as HTMLInputElement;
    component.onFilesSelected({ target: input } as unknown as Event);
  };

  const preuve = (id: string, nom: string) => ({
    id, contentType: 'application/pdf', sizeBytes: 1024,
    originalFilename: nom, createdAt: '2026-07-01T00:00:00Z'
  });

  it('retient les fichiers choisis sans rien envoyer avant que le dossier existe', () => {
    choisir(fichier('releve.pdf'));

    expect(component.attachments.length).toBe(1);
    // Rien ne part : le serveur classe une preuve SOUS un dossier, il n'y a
    // encore aucun dossier auquel la rattacher.
    http.expectNone(base);
  });

  it('dépose les pièces sur le dossier une fois celui-ci créé', () => {
    choisir(fichier('releve.pdf'), fichier('photo.pdf'));
    component.form.controls.title.setValue('Défaut étiquetage');
    component.submit();

    http.expectOne(base).flush(created);

    const up1 = http.expectOne(`${base}/${created.id}/evidences`);
    expect(up1.request.method).toBe('POST');
    expect(up1.request.body instanceof FormData).toBeTrue();
    up1.flush(preuve('e1', 'releve.pdf'));

    // Une pièce à la fois : deux dépôts concurrents rendraient l'ordre de la
    // liste imprévisible d'un dossier à l'autre.
    const up2 = http.expectOne(`${base}/${created.id}/evidences`);
    up2.flush(preuve('e2', 'photo.pdf'));

    expect(component.uploaded).toBe(2);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('ferme quand même sur un dépôt refusé : le dossier, lui, est bien créé', () => {
    choisir(fichier('trop-lourd.pdf'));
    component.form.controls.title.setValue('Défaut étiquetage');
    component.submit();

    http.expectOne(base).flush(created);
    http.expectOne(`${base}/${created.id}/evidences`)
        .flush({ title: 'too large' }, { status: 413, statusText: 'Payload Too Large' });

    // Annuler un dossier créé parce qu'une pièce a été refusée perdrait la
    // déclaration elle-même ; la pièce se rejoint depuis la fiche.
    expect(component.uploaded).toBe(0);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('refuse tout de suite une pièce au-delà de 10 Mo, avant la création', () => {
    choisir(fichier('enorme.pdf', 10 * 1024 * 1024 + 1));

    expect(component.attachments.length).toBe(0);
  });

  it('s\'arrête à dix pièces', () => {
    choisir(...Array.from({ length: 12 }, (_, i) => fichier(`p${i}.pdf`)));

    expect(component.attachments.length).toBe(10);
  });

  it('retire une pièce de la file avant l\'envoi', () => {
    const f = fichier('a-retirer.pdf');
    choisir(f, fichier('garde.pdf'));

    component.removeAttachment(f);

    expect(component.attachments.map(a => a.name)).toEqual(['garde.pdf']);
  });

  it('affiche un poids lisible plutôt qu\'un nombre d\'octets', () => {
    expect(component.formatSize(2 * 1024 * 1024)).toBe('2.0 Mo');
    expect(component.formatSize(4096)).toBe('4 Ko');
    expect(component.formatSize(10)).toBe('1 Ko');
  });

  it('ferme le dialogue sans rien créer à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(base);
  });
});
