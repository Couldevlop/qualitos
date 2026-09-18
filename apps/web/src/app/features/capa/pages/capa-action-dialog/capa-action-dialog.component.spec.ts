import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaActionResponse } from '../../capa.types';
import { CapaActionDialogComponent } from './capa-action-dialog.component';

/**
 * §4.2 / ISO 9001 §10.2 — une action naît toujours sous un cas existant et
 * dans l'état PENDING : c'est son avancement qui débloque la résolution du cas.
 */
describe('CapaActionDialogComponent', () => {
  let component: CapaActionDialogComponent;
  let fixture: ComponentFixture<CapaActionDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaActionDialogComponent, CapaActionResponse>>;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/capa/cases/c1/actions`;

  const created: CapaActionResponse = {
    id: 'a1', capaId: 'c1', title: 'Recalibrer la sonde', status: 'PENDING',
    actionType: 'CORRECTIVE'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaActionDialogComponent, CapaActionResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [CapaActionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule, MatRadioModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { caseId: 'c1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaActionDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige un titre d\'action', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un titre au-delà de 255 caractères', () => {
    component.form.controls.title.setValue('a'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(url);
  });

  it('crée l\'action sous le cas passé en donnée de dialogue', () => {
    component.form.patchValue({
      title: '  Recalibrer la sonde  ', description: '   ', dueDate: ''
    });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Recalibrer la sonde');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.dueDate).toBeUndefined();

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet description et échéance quand elles sont saisies', () => {
    component.form.patchValue({
      title: 'Recalibrer', description: '  Sonde T° hebdo  ', dueDate: '2026-09-15'
    });
    component.submit();
    const req = http.expectOne(url);
    expect(req.request.body.description).toBe('Sonde T° hebdo');
    expect(req.request.body.dueDate).toBe('2026-09-15');
    req.flush(created);
  });

  it('ne ferme pas le dialogue quand le cas n\'accepte plus d\'action (409)', () => {
    component.form.controls.title.setValue('Recalibrer');
    component.submit();
    http.expectOne(url).flush({ title: 'closed case' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.title.setValue('Recalibrer');
    component.submit();
    const req = http.expectOne(url);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(url);
    req.flush(created);
  });

  it('ferme sans rien ajouter à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});

/**
 * Le meme formulaire, ouvert depuis une NON-CONFORMITE : il pose AUSSI la
 * verification d'efficacite du dossier.
 *
 * <p>Pourquoi ici et pas depuis la fiche CAPA : sur ce chemin, le dossier vient
 * peut-etre d'etre cree et personne n'a vu son formulaire d'edition. Sans cela,
 * la question ne serait jamais posee -- et un dossier sans decision est
 * exactement ce que l'ADR 0073 cherche a distinguer d'un dossier delibérément
 * juge non verifiable.
 *
 * <p>Ce bloc apparait CONDITIONNELLEMENT, et c'est la logique qui a coute quatre
 * passes de NG0100 au dialogue d'edition. Les bancs portent donc autant sur
 * l'ordre des appels que sur les champs.
 */
describe('CapaActionDialogComponent — ouvert depuis une non-conformite', () => {
  let component: CapaActionDialogComponent;
  let fixture: ComponentFixture<CapaActionDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaActionDialogComponent, CapaActionResponse>>;
  let prevMock: boolean;

  const urlActions = `${environment.apiBaseUrl}/api/v1/capa/cases/c1/actions`;
  const urlCas = `${environment.apiBaseUrl}/api/v1/capa/cases/c1`;
  const urlAnnuaire = `${environment.apiBaseUrl}/api/v1/users`;

  const hote = () => fixture.nativeElement as HTMLElement;

  /** Un tour de macrotache : `deferredView` ne livre pas avant. */
  const tour = () => new Promise(r => setTimeout(r, 0));

  /** Sert l'annuaire, ou le fait echouer, puis stabilise la vue. */
  async function servirAnnuaire(echec = false): Promise<void> {
    const req = http.expectOne(r => r.url === urlAnnuaire);
    if (echec) {
      req.flush('', { status: 503, statusText: 'Service Unavailable' });
    } else {
      req.flush({
        content: [
          { id: 'u1', tenantId: 't', keycloakId: 'k1', email: 'amina@exemple.fr',
            roles: [], active: true, createdAt: '', updatedAt: '' },
          // Un compte DESACTIVE : il ne doit pas etre proposable.
          { id: 'u2', tenantId: 't', keycloakId: 'k2', email: 'parti@exemple.fr',
            roles: [], active: false, createdAt: '', updatedAt: '' }
        ],
        totalElements: 2, totalPages: 1, number: 0, size: 200
      });
    }
    await tour();
    fixture.detectChanges();
  }

  /** Remplit le minimum pour que l'action soit envoyable. */
  function remplirAction(): void {
    component.form.controls.title.setValue('Recalibrer la sonde');
  }

  /** Repond a la question, et laisse le bloc se rendre. */
  async function repondre(exigee: boolean): Promise<void> {
    component.form.controls.verificationRequired.setValue(exigee);
    await tour();
    fixture.detectChanges();
  }

  const casServi = { id: 'c1', title: 'x', status: 'OPEN', criticity: 'MEDIUM' };
  const actionServie = {
    id: 'a1', capaId: 'c1', title: 'Recalibrer la sonde', status: 'PENDING',
    actionType: 'CORRECTIVE'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaActionDialogComponent, CapaActionResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [CapaActionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule, MatRadioModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { caseId: 'c1', askVerification: true } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaActionDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('pose la question, et les deux reponses sont offertes', async () => {
    await servirAnnuaire();

    expect(hote().querySelector('[data-test=bloc-verification]')).not.toBeNull();
    const choix = hote().querySelector('[data-test=verification-exigee]')!;
    expect(choix.querySelectorAll('mat-radio-button').length).toBe(2);

    // Tant que rien n'est repondu, on ne demande ni a qui ni quoi verifier.
    expect(hote().querySelector('[data-test=verification-qui]')).toBeNull();
    expect(hote().querySelector('[data-test=verification-consignes]')).toBeNull();
  });

  it('exige que la question soit tranchee avant tout envoi', async () => {
    await servirAnnuaire();
    remplirAction();

    // L'asterisque de la demande : sur ce chemin, ne pas repondre n'est pas une
    // option -- c'est le moment ou la question a un sens.
    expect(component.form.controls.verificationRequired.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(urlCas);
    http.expectNone(urlActions);
  });

  it('repondre oui demande a qui, et quoi verifier', async () => {
    await servirAnnuaire();
    await repondre(true);

    expect(hote().querySelector('[data-test=verification-qui]')).not.toBeNull();
    expect(hote().querySelector('[data-test=verification-consignes]')).not.toBeNull();
    // Exiger sans designer ne veut rien dire : le validateur est pose.
    expect(component.form.controls.verificationAssigneeId.hasError('required')).toBeTrue();
  });

  it('ne propose que les comptes ACTIFS comme verificateur', async () => {
    await servirAnnuaire();
    expect(component.membres.map(m => m.email)).toEqual(['amina@exemple.fr']);
  });

  it('revenir a non referme la question et efface ce qui n a plus d objet', async () => {
    await servirAnnuaire();
    await repondre(true);
    component.form.controls.verificationAssigneeId.setValue('u1');
    component.form.controls.verificationInstructions.setValue('Relever 20 pieces');

    await repondre(false);

    expect(hote().querySelector('[data-test=verification-qui]')).toBeNull();
    // Laisser un verificateur derriere soi laisserait croire qu'une
    // verification est encore attendue.
    expect(component.form.controls.verificationAssigneeId.value).toBeNull();
    expect(component.form.controls.verificationInstructions.value).toBe('');
    expect(component.form.controls.verificationAssigneeId.hasError('required')).toBeFalse();
  });

  it('enregistre la decision AVANT de creer l action', async () => {
    await servirAnnuaire();
    remplirAction();
    await repondre(true);
    component.form.controls.verificationAssigneeId.setValue('u1');
    component.form.controls.verificationInstructions.setValue('  Relever 20 pieces  ');

    component.submit();

    // Le dossier d'abord : c'est lui qui porte les gardes du serveur. Creer
    // l'action puis echouer sur la verification laisserait une action que
    // l'utilisateur, en reessayant, DOUBLERAIT.
    const patch = http.expectOne(urlCas);
    expect(patch.request.body.verificationRequired).toBeTrue();
    expect(patch.request.body.verificationAssigneeId).toBe('u1');
    // Le NOM part avec l'identifiant : recopie pour rester lisible meme si le
    // compte disparait de l'annuaire.
    expect(patch.request.body.verificationAssigneeName).toBe('amina@exemple.fr');
    expect(patch.request.body.verificationInstructions).toBe('Relever 20 pieces');
    // Et surtout PAS l'intitule ni la criticite : ce formulaire ne les affiche
    // pas, les envoyer les remplacerait par du vide.
    expect(patch.request.body.title).toBeUndefined();
    expect(patch.request.body.criticity).toBeUndefined();

    // L'action ne part qu'apres.
    http.expectNone(urlActions);
    patch.flush(casServi);
    http.expectOne(urlActions).flush(actionServie);

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('un non enregistre ne traine ni verificateur ni consignes', async () => {
    await servirAnnuaire();
    remplirAction();
    await repondre(false);

    component.submit();

    const patch = http.expectOne(urlCas);
    expect(patch.request.body.verificationRequired).toBeFalse();
    expect(patch.request.body.verificationAssigneeId).toBeUndefined();
    expect(patch.request.body.verificationInstructions).toBeUndefined();
    patch.flush(casServi);
    http.expectOne(urlActions).flush(actionServie);
  });

  it('ne cree AUCUNE action quand la decision est refusee par le serveur', async () => {
    await servirAnnuaire();
    remplirAction();
    await repondre(false);

    component.submit();
    http.expectOne(urlCas).flush('', { status: 422, statusText: 'Unprocessable Entity' });

    // Rien derriere soi : c'est tout l'interet de l'ordre choisi.
    http.expectNone(urlActions);
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('dit que l annuaire est muet au lieu d offrir une liste vide', async () => {
    await servirAnnuaire(true);
    await repondre(true);

    // UN TOUR DE PLUS, et il n'est pas de trop. Le message vit DANS le bloc
    // conditionnel : son `| async` ne s'abonne qu'au moment ou ce bloc
    // s'instancie, et `deferredView` livre en macrotache -- donc apres la passe
    // de rendu qui vient de le creer. C'est le prix du remede a NG0100, et le
    // banc doit le payer comme le navigateur le paie.
    await tour();
    fixture.detectChanges();

    expect(component.annuaireIndisponible).toBeTrue();
    expect(hote().querySelector('[data-test=annuaire-indisponible]')).not.toBeNull();
  });
});
