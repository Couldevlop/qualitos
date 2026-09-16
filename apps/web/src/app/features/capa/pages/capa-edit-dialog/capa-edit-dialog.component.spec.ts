import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { TenantUser } from '../../../admin/admin.types';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCaseResponse } from '../../capa.types';
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
    await TestBed.configureTestingModule({
      declarations: [CapaEditDialogComponent],
      imports: [SharedModule, UiModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaEditDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    repondreAnnuaire();
  }

  /**
   * Sert l'annuaire interroge par `ngOnInit`.
   *
   * <p>Sans cela, `http.verify()` echoue sur une requete en suspens a la fin de
   * CHAQUE banc : le composant va chercher les membres des sa construction, et
   * un banc qui l'ignore ne teste pas le composant reel.
   */
  function repondreAnnuaire(membres: Partial<TenantUser>[] = [
    { id: 'u-verif', email: 'controle@exemple.fr', active: true },
    { id: 'u-parti', email: 'parti@exemple.fr', active: false }
  ]): void {
    const req = http.expectOne(r => r.url.endsWith('/api/v1/users'));
    req.flush({ content: membres, totalElements: membres.length,
                totalPages: 1, number: 0, size: 200 });
  }

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
    expect(component.verificationExigee).toBeFalse();
    expect(hote().querySelector('[data-test=verification-qui]')).toBeNull();

    component.form.controls.verificationRequired.setValue(true);
    await macrotache();   // `deferredView` livre en macrotâche
    fixture.detectChanges();

    expect(component.verificationExigee).toBeTrue();
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

  it('n offre que les membres actifs de l organisation', async () => {
    await build({ capa: existing });

    // Un compte desactive ne peut plus verifier quoi que ce soit : le proposer
    // reviendrait a confier la tache a personne.
    expect(component.membres.map(m => m.email)).toEqual(['controle@exemple.fr']);
  });

  it('envoie l identifiant ET le nom du verificateur', async () => {
    await build({ capa: existing });
    component.form.patchValue({
      verificationRequired: true,
      verificationAssigneeId: 'u-verif',
      verificationInstructions: '  Reprendre 30 pieces au calibre.  '
    });

    component.submit();

    const req = http.expectOne(`${base}/c1`);
    expect(req.request.body.verificationRequired).toBeTrue();
    expect(req.request.body.verificationAssigneeId).toBe('u-verif');
    // Le nom part avec l'identifiant : il est recopie dans le dossier pour rester
    // lisible meme si le compte disparait de l'annuaire.
    expect(req.request.body.verificationAssigneeName).toBe('controle@exemple.fr');
    expect(req.request.body.verificationInstructions)
        .toBe('Reprendre 30 pieces au calibre.');
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

  it('dit que l annuaire est indisponible plutot que d afficher une liste vide',
     async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>>(
      'MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [CapaEditDialogComponent],
      imports: [SharedModule, UiModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { capa: existing } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaEditDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne(r => r.url.endsWith('/api/v1/users'))
        .flush('indisponible', { status: 503, statusText: 'Service Unavailable' });
    component.form.controls.verificationRequired.setValue(true);
    await macrotache();   // `deferredView` livre en macrotâche
    fixture.detectChanges();
    // DEUX tours : le paragraphe vit DANS le bloc conditionnel, donc son tuyau
    // `async` ne s'abonne qu'une fois le bloc rendu, et sa premiere valeur
    // n'arrive qu'au tour suivant. L'ecran fait exactement cela, un battement
    // plus tard et sans que personne ne le remarque.
    await macrotache();
    fixture.detectChanges();

    expect(component.annuaireIndisponible).toBeTrue();
    expect(hote().querySelector('[data-test=annuaire-indisponible]')).not.toBeNull();
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
