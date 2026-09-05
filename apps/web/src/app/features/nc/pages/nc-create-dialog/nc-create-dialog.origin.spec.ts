import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../../products/products.service';
import { NcOrigin, NcResponse } from '../../nc.types';
import { NcCreateDialogComponent, NcCreateDialogData } from './nc-create-dialog.component';

/**
 * L'origine de la liste suit la déclaration.
 *
 * <p>Sans elle, une non-conformité déclarée depuis l'écran « externes » repartait
 * sans origine, le serveur retombait sur son défaut INTERNAL, et la fiche
 * atterrissait dans le tableau des internes — donc hors de la liste d'où on
 * venait de la créer. Vu de l'utilisateur, elle s'était perdue.
 */
describe('NcCreateDialogComponent — origine héritée de la liste', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  const created: NcResponse = {
    id: 'nc-1', reference: 'NC-2026-0400', title: 'Retard fournisseur',
    category: 'SUPPLIER', severity: 'MAJOR', status: 'OPEN', origin: 'EXTERNAL',
    detectedAt: '2026-09-01T08:00:00Z',
    createdAt: '2026-09-01T08:00:00Z', updatedAt: '2026-09-01T08:00:00Z'
  };

  let http: HttpTestingController;
  let prevMock: boolean;

  /** Monte le dialogue tel que la liste l'ouvre, avec (ou sans) son origine. */
  async function monte(data: NcCreateDialogData | null): Promise<NcCreateDialogComponent> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [NcCreateDialogComponent],
      imports: [SharedModule, UiModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u-1' }) } },
        {
          provide: ProductsService,
          useValue: { list: () => of([]), failureModeSuggestions: () => of([]) }
        }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(NcCreateDialogComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('transmet au serveur l\'origine de la liste appelante', async () => {
    const component = await monte({ origin: 'EXTERNAL' as NcOrigin });
    component.form.patchValue({
      title: 'Retard fournisseur',
      description: 'Livraison du lot 88 reçue avec quinze jours de retard.'
    });

    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.origin).toBe('EXTERNAL');
    req.flush(created);
  });

  it('n\'impose aucune origine quand la liste n\'en porte pas', async () => {
    // Entrée historique `/nc`, qui montre les deux origines : c'est au serveur
    // de trancher, et surtout pas au dialogue d'inventer un défaut divergent.
    const component = await monte(null);
    component.form.patchValue({
      title: 'Écart de process',
      description: 'Paramètre de four relevé hors plage au poste 12.'
    });

    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.origin).toBeUndefined();
    req.flush(created);
  });

  it('reste ouvrable sans donnée de dialogue', async () => {
    // `@Optional()` : un appelant qui n'injecte rien du tout ne doit pas faire
    // échouer la construction du composant.
    const component = await monte(null);
    expect(component.origin).toBeUndefined();
    expect(component.form.controls.title.value).toBe('');
  });
});
