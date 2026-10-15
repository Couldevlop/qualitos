import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerComponent } from '../../../../shared/user-picker/user-picker.component';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { CapaCaseResponse } from '../../capa.types';
import { CapaVerificationFieldsModule } from '../../capa-verification-fields.module';
import {
  CapaVerificationFieldsComponent
} from '../../components/capa-verification-fields/capa-verification-fields.component';
import { CapaEditDialogComponent, CapaEditDialogData } from './capa-edit-dialog.component';

/**
 * L'édition ne porte que sur les champs descriptifs : le statut, le type et le
 * propriétaire suivent le cycle de vie du cas et ne sont pas modifiables ici.
 */
describe('CapaEditDialogComponent', () => {
  let component: CapaEditDialogComponent;
  let fixture: ComponentFixture<CapaEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>>;
  let prevMock: boolean;
  let directory: jasmine.SpyObj<DirectoryService>;

  const CONTROLE: DirectoryUser = {
    id: 'db-verif', accountId: 'u-verif', displayName: 'Claire Contrôle', email: 'controle@exemple.fr', active: true
  };

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  const existing: CapaCaseResponse = {
    id: 'c1', tenantId: 't1', title: 'Recalibration robot', description: 'Suite NC ligne 3.',
    type: 'CORRECTIVE', criticity: 'HIGH', status: 'IN_PROGRESS',
    sourceType: 'NON_CONFORMITY', sourceRef: 'NC-2026-018', ownerId: 'u1',
    dueDate: '2026-05-30',
    createdAt: '2026-04-01T00:00:00Z', updatedAt: '2026-04-02T00:00:00Z', actions: []
  };

  async function build(data: CapaEditDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>>(
      'MatDialogRef', ['close']);
    // L'annuaire en lecture restreinte (ADR 0091), et non `/api/v1/users`
    // réservé aux administrateurs : `http.verify()` garantit qu'aucun appel
    // à ce dernier ne part plus.
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: [CONTROLE], page: 0, size: 20, totalElements: 1, totalPages: 1 }));
    directory.getByAccount.and.callFake(id => id === CONTROLE.accountId ? of(CONTROLE) : throwError(() => ({ status: 404 })));
    await TestBed.configureTestingModule({
      declarations: [CapaEditDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, MatRadioModule, CapaVerificationFieldsModule,
                NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: DirectoryService, useValue: directory },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaEditDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  /** Exige la vérification et laisse le bloc « à qui » apparaître. */
  async function exiger(): Promise<void> {
    component.form.controls.verificationRequired.setValue(true);
    await macrotache();   // `deferredView` livre en macrotâche
    fixture.detectChanges();
  }

  const bloc = (): CapaVerificationFieldsComponent =>
    fixture.debugElement.query(By.directive(CapaVerificationFieldsComponent))
      .componentInstance as CapaVerificationFieldsComponent;

  const picker = (): UserPickerComponent =>
    fixture.debugElement.query(By.directive(UserPickerComponent)).componentInstance as UserPickerComponent;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('reprend les valeurs existantes du cas', async () => {
    await build({ capa: existing });
    expect(component.form.controls.title.value).toBe('Recalibration robot');
    expect(component.form.controls.description.value).toBe('Suite NC ligne 3.');
    expect(component.form.controls.criticity.value).toBe('HIGH');
    expect(component.form.controls.sourceRef.value).toBe('NC-2026-018');
    expect(component.form.controls.dueDate.value).toBe('2026-05-30');
    expect(component.criticities).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
  });

  it('remplace les champs optionnels absents par du vide plutôt que « undefined »', async () => {
    await build({ capa: { ...existing, description: undefined, sourceRef: undefined, dueDate: undefined } });
    expect(component.form.controls.description.value).toBe('');
    expect(component.form.controls.sourceRef.value).toBe('');
    expect(component.form.controls.dueDate.value).toBe('');
  });

  it('n\'envoie rien quand le titre a été vidé', async () => {
    await build({ capa: existing });
    component.form.controls.title.setValue('');
    component.submit();
    http.expectNone(`${base}/c1`);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('envoie une mise à jour partielle sur l\'identifiant du cas ouvert', async () => {
    await build({ capa: existing });
    component.form.patchValue({
      title: '  Recalibration robot cobot-3  ',
      description: '   ',
      criticity: 'CRITICAL',
      sourceRef: '  ',
      dueDate: '2026-06-30'
    });
    component.submit();

    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.title).toBe('Recalibration robot cobot-3');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.sourceRef).toBeUndefined();
    expect(req.request.body.criticity).toBe('CRITICAL');
    expect(req.request.body.dueDate).toBe('2026-06-30');

    const updated = { ...existing, criticity: 'CRITICAL' as const };
    req.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('garde le dialogue ouvert quand le cas a changé entre-temps (409)', async () => {
    await build({ capa: existing });
    component.submit();
    http.expectOne(`${base}/c1`).flush({ title: 'stale' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build({ capa: existing });
    component.submit();
    const req = http.expectOne(`${base}/c1`);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(`${base}/c1`);
    req.flush(existing);
  });

  it('ferme sans rien modifier à l\'annulation', async () => {
    await build({ capa: existing });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/c1`);
  });

  // ---------- verification d'efficacite ----------

  it('ne propose le verificateur que si la verification est exigee', async () => {
    await build({ capa: existing });

    // Question non tranchee : le bloc « a qui » n'a pas lieu d'etre.
    expect(bloc().verificationExigee).toBeFalse();
    expect(hote().querySelector('[data-test=verification-qui]')).toBeNull();

    component.form.controls.verificationRequired.setValue(true);
    await macrotache();   // `deferredView` livre en macrotâche
    fixture.detectChanges();

    expect(bloc().verificationExigee).toBeTrue();
    expect(hote().querySelector('[data-test=verification-qui]')).not.toBeNull();
  });

  it('rend le verificateur obligatoire des que la verification l est', async () => {
    await build({ capa: existing });

    component.form.controls.verificationRequired.setValue(true);
    await macrotache();   // `deferredView` livre en macrotâche
    fixture.detectChanges();

    // Exiger sans designer est refuse par le serveur : on le dit ici plutot que
    // d'aller chercher un 422 pour l'apprendre.
    expect(component.form.controls.verificationAssigneeId.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(`${base}/c1`);
  });

  it('le verificateur se choisit dans l annuaire en lecture restreinte', async () => {
    await build({ capa: existing });
    await exiger();

    expect(hote().querySelector('qos-user-picker[data-test=verification-qui]')).not.toBeNull();
    expect(picker().selected).toBeNull();
  });

  it('envoie l identifiant ET le NOM d annuaire du verificateur choisi', async () => {
    await build({ capa: existing });
    await exiger();
    picker().onSelected({ option: { value: CONTROLE } } as never);
    component.form.patchValue({ verificationInstructions: '  Reprendre 30 pieces au calibre.  ' });

    component.submit();

    const req = http.expectOne(`${base}/c1`);
    expect(req.request.body.verificationRequired).toBeTrue();
    expect(req.request.body.verificationAssigneeId).toBe('u-verif');
    // Le nom part avec l'identifiant : il est recopie dans le dossier pour rester
    // lisible meme si le compte disparait de l'annuaire. Le nom, pas l'adresse.
    expect(req.request.body.verificationAssigneeName).toBe('Claire Contrôle');
    expect(req.request.body.verificationInstructions)
        .toBe('Reprendre 30 pieces au calibre.');
    req.flush(existing);
  });

  it('garde le nom enregistre quand le verificateur ne change pas', async () => {
    await build({ capa: {
      ...existing, verificationRequired: true,
      verificationAssigneeId: 'u-verif', verificationAssigneeName: 'C. Contrôle (qualité)'
    } });
    await macrotache();
    fixture.detectChanges();
    expect(picker().selected?.displayName).toBe('Claire Contrôle');

    component.submit();

    const req = http.expectOne(`${base}/c1`);
    expect(req.request.body.verificationAssigneeId).toBe('u-verif');
    expect(req.request.body.verificationAssigneeName).toBe('C. Contrôle (qualité)');
    req.flush(existing);
  });

  it('efface le verificateur et les consignes quand la verification cesse d etre exigee',
     async () => {
    await build({ capa: {
      ...existing, verificationRequired: true,
      verificationAssigneeId: 'u-verif', verificationAssigneeName: 'controle@exemple.fr',
      verificationInstructions: 'Reprendre 30 pieces.'
    } });
    expect(component.form.controls.verificationAssigneeId.value).toBe('u-verif');

    component.form.controls.verificationRequired.setValue(false);
    await macrotache();
    fixture.detectChanges();

    // Laisser trainer un verificateur laisserait croire qu'une verification est
    // encore attendue.
    expect(component.form.controls.verificationAssigneeId.value).toBeNull();
    expect(component.form.controls.verificationInstructions.value).toBe('');

    component.submit();
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.body.verificationRequired).toBeFalse();
    expect(req.request.body.verificationAssigneeId).toBeUndefined();
    req.flush(existing);
  });

  it('n envoie rien sur la verification quand la question reste ouverte', async () => {
    await build({ capa: existing });
    component.form.controls.title.setValue('Autre titre');

    component.submit();

    // `null` veut dire « non tranche » : l'envoyer effacerait une decision prise
    // ailleurs, et ne pas l'envoyer laisse le dossier tel qu'il est.
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.body.verificationRequired).toBeUndefined();
    req.flush(existing);
  });

  /**
   * Rend la main a la boucle d'evenements.
   *
   * <p>Les etats d'affichage passent par `deferredView`, qui livre en
   * MACROTACHE : un microtour ne suffit pas a les voir arriver, et un banc qui
   * s'en contenterait jugerait un ecran qui n'a pas fini de se mettre a jour.
   */
  function macrotache(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 0));
  }

  /** L'element hote du composant, pour interroger le rendu. */
  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
});
