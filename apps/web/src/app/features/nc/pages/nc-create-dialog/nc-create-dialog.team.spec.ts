import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { ProductsService } from '../../../products/products.service';
import { NcResponse } from '../../nc.types';
import { NcCreateDialogComponent } from './nc-create-dialog.component';

/**
 * L'équipe en charge, nommée à l'ouverture de l'écart — le D1 du 8D.
 *
 * <p>Elle se retapait jusqu'ici dans le rapport, alors que la fiche sait depuis
 * le premier jour qui s'en occupe. Deux promesses tiennent ici : on ne déclare
 * pas un écart dont personne ne répond, et le déclarant est posé d'office en
 * tête plutôt que laissé à ressaisir.
 */
describe('NcCreateDialogComponent — équipe en charge', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  const created: NcResponse = {
    id: 'nc-1', reference: 'NC-2026-0401', title: 'Écart de process',
    category: 'PROCESS', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
    detectedAt: '2026-11-01T08:00:00Z',
    createdAt: '2026-11-01T08:00:00Z', updatedAt: '2026-11-01T08:00:00Z'
  };

  let http: HttpTestingController;
  let fixture: ComponentFixture<NcCreateDialogComponent>;
  let component: NcCreateDialogComponent;
  let prevMock: boolean;

  async function monte(moi: { userId: string; displayName?: string } | null): Promise<void> {
    TestBed.resetTestingModule();
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({
      content: [{
        id: 'id-bruno', accountId: 'u-bruno', displayName: 'Bruno Lefèvre',
        email: 'bruno@exemple.fr', active: true
      }],
      page: 0, size: 20, totalElements: 1, totalPages: 1
    }));
    directory.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [NcCreateDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: null },
        { provide: AuthService, useValue: { snapshot: () => moi } },
        { provide: DirectoryService, useValue: directory },
        { provide: ProductsService, useValue: { list: () => of([]), failureModeSuggestions: () => of([]) } }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(NcCreateDialogComponent);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/api/v1/nc/subcategories`)
      .flush({ entries: [], defaults: true });
    component = fixture.componentInstance;
  }

  /** Le constat rempli : il ne reste que l'équipe à discuter. */
  function remplitLeConstat(): void {
    component.form.patchValue({
      title: 'Écart de process',
      description: 'Paramètre de four relevé hors plage au poste 12.',
      category: 'PROCESS',
      severity: 'MAJOR'
    });
  }

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  // ---------- le pré-remplissage ----------

  it('pose le déclarant en tête de l’équipe', async () => {
    await monte({ userId: 'u-alice', displayName: 'Alice Martin' });
    expect(component.form.controls.teamMembers.value)
      .toEqual([{ accountId: 'u-alice', displayName: 'Alice Martin' }]);
  });

  // Un jeton sans `preferred_username` ne doit pas produire une ligne muette.
  it('se rabat sur le compte quand le jeton ne porte pas de nom', async () => {
    await monte({ userId: 'u-alice' });
    expect(component.form.controls.teamMembers.value)
      .toEqual([{ accountId: 'u-alice', displayName: 'u-alice' }]);
  });

  it('laisse l’équipe vide quand aucune session n’est connue', async () => {
    await monte(null);
    expect(component.form.controls.teamMembers.value).toEqual([]);
  });

  // ---------- ce qui empêche de déclarer ----------

  it('refuse de déclarer sans personne en charge, et dit laquelle manque', async () => {
    await monte(null);
    remplitLeConstat();
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
    expect(component.blockedReason).toContain('équipe');

    component.submit();
    http.expectNone(endpoint);
  });

  it('ne reproche l’équipe qu’une fois le constat renseigné', async () => {
    await monte(null);
    // Les quatre champs du constat passent d'abord : on ne reproche pas l'équipe
    // à quelqu'un qui n'a pas encore écrit ce qu'il a vu.
    expect(component.blockedReason).toContain('titre');
  });

  // ---------- l'envoi ----------

  it('transmet l’équipe au serveur, dans l’ordre où elle a été nommée', async () => {
    await monte({ userId: 'u-alice', displayName: 'Alice Martin' });
    remplitLeConstat();
    component.form.patchValue({
      teamMembers: [
        { accountId: 'u-alice', displayName: 'Alice Martin' },
        { accountId: 'u-bruno', displayName: 'Bruno Lefèvre' }
      ]
    });

    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.teamMembers).toEqual([
      { accountId: 'u-alice', displayName: 'Alice Martin' },
      { accountId: 'u-bruno', displayName: 'Bruno Lefèvre' }
    ]);
    req.flush(created);
  });

  // ---------- l'écran ----------

  it('montre le champ d’équipe et sa borne haute', async () => {
    await monte({ userId: 'u-alice', displayName: 'Alice Martin' });
    fixture.detectChanges();
    const champ = (fixture.nativeElement as HTMLElement).querySelector('[data-test="nc-team"]');
    expect(champ).not.toBeNull();
    expect(component.maxTeamMembers).toBe(12);
    // Le nom du déclarant s'affiche, pas son identifiant de compte.
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Alice Martin');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('u-alice');
  });
});
