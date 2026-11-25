import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { filter, take } from 'rxjs/operators';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCaseResponse } from '../../capa.types';
import { CapaListComponent } from './capa-list.component';
import { AuthzService } from '../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { CapaService } from '../../capa.service';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';

/**
 * L'annuaire en lecture restreinte (ADR 0091) : le seul compte connu est
 * « kc-1 », les autres répondent 404.
 */
function annuaire(): Pick<DirectoryService, 'getByAccount'> {
  return {
    getByAccount: (id: string) => id === 'kc-1'
      ? of({ id: 'i-1', accountId: 'kc-1', displayName: 'Karim Benali', email: 'karim@client.test', active: true })
      : throwError(() => ({ status: 404 }))
  };
}

describe('CapaListComponent', () => {
  let component: CapaListComponent;
  let fixture: ComponentFixture<CapaListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [CapaListComponent],
      // UserPickerModule : le filtre de pilote choisit la personne dans
      // l'annuaire, et sans lui le champ n'a aucun accesseur de valeur.
      // MatDatepicker : le filtre d'échéance ouvre un calendrier, câblé dans
      // le module du registre et non à la racine (coût par écran).
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
                MatDatepickerModule, MatNativeDateModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: DirectoryService, useValue: annuaire() }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  describe('la colonne des natures', () => {

    /** Un dossier porteur des natures voulues, le reste au plus simple. */
    function dossier(natures: readonly string[], type = 'CORRECTIVE'): never {
      return <never>{
        id: 'c-1', reference: 'CAPA-1', title: 'Palettes sans etiquette',
        type, status: 'IN_PROGRESS', criticity: 'MAJOR',
        actions: natures.map((n, i) => ({ id: 'a-' + i, actionType: n }))
      };
    }

    it('affiche les natures REELLEMENT presentes, pas le type declare du dossier', () => {
      // Un endiguement ajoute depuis le traitement d'un ecart se rangeait sous
      // « Corrective », parce que la colonne lisait le type du DOSSIER.
      expect(component.caseTypeLabel(dossier(['CONTAINMENT', 'CORRECTIVE'])))
        .toBe('Endiguement · Corrective');
    });

    it('garde l’ordre du traitement, quel que soit l’ordre des actions', () => {
      // On endigue, on corrige, on previent : l'ordre dit le raisonnement.
      expect(component.caseTypeLabel(dossier(['PREVENTIVE', 'CONTAINMENT'])))
        .toBe('Endiguement · Préventive');
    });

    it('ne repete pas une nature portee par plusieurs actions', () => {
      expect(component.caseTypeLabel(dossier(['CORRECTIVE', 'CORRECTIVE'])))
        .toBe('Corrective');
    });

    it('se replie sur le type declare tant que le dossier n’a aucune action', () => {
      // Sans action, il n'y a rien a lire : la colonne ne doit pas se vider.
      expect(component.caseTypeLabel(dossier([], 'PREVENTIVE'))).toBe('Préventive');
    });
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('CAPA');
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns).toEqual(
      ['title', 'type', 'criticity', 'status', 'owner', 'createdAt', 'dueDate']);
  });

  it('nomme le pilote, jamais par son identifiant (ADR 0088) ; l\'annuaire nomme les anciens dossiers', async () => {
    const base = {
      id: 'c', tenantId: 't', title: 't', type: 'CORRECTIVE', criticity: 'LOW', status: 'OPEN',
      sourceType: 'INTERNAL', createdAt: '', updatedAt: '', actions: [] as CapaCaseResponse['actions']
    } satisfies Omit<CapaCaseResponse, 'ownerId'>;
    const nomme = { ...base, id: 'c1', ownerId: 'kc-x', ownerName: 'Amina Dridi' };
    const ancien = { ...base, id: 'c2', ownerId: 'kc-1' };
    const inconnu = { ...base, id: 'c3', ownerId: '9c1f2b7e-0000-4000-8000-000000000000' };
    spyOn(TestBed.inject(CapaService), 'listCases').and.returnValue(of({
      content: [nomme, ancien, inconnu], totalElements: 3, totalPages: 1, number: 0, size: 20
    }));
    const getByAccount = spyOn(TestBed.inject(DirectoryService), 'getByAccount').and.callThrough();
    fixture.detectChanges();
    await firstValueFrom(component.cases$);

    expect(component.ownerLabel(nomme)).toBe('Amina Dridi');
    expect(component.ownerLabel(ancien)).toBe('Karim Benali');
    expect(component.ownerLabel(inconnu)).toBe('—');
    // Le nom recopié ne coûte aucune lecture ; chaque compte inconnu, une seule.
    expect(getByAccount.calls.allArgs()).toEqual([['kc-1'], ['9c1f2b7e-0000-4000-8000-000000000000']]);
  });

  it('filtre la liste par criticité, en plus du statut', async () => {
    const list = spyOn(TestBed.inject(CapaService), 'listCases').and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20
    }));
    fixture.detectChanges();
    const sub = component.cases$.subscribe();
    component.statusFilter.setValue('OPEN');
    component.criticityFilter.setValue('HIGH');
    expect(list.calls.mostRecent().args.slice(2))
      .toEqual(['OPEN', 'HIGH', undefined, undefined]);
    component.criticityFilter.setValue('');
    expect(list.calls.mostRecent().args.slice(2))
      .toEqual(['OPEN', undefined, undefined, undefined]);
    sub.unsubscribe();
  });

  /**
   * Le pilote part AU SERVEUR, qui pagine : filtrer la page rendue n'aurait
   * montre que les dossiers de cette personne presents sur la page courante,
   * et le compteur aurait annonce le total de tous les autres.
   */
  it('filtre la liste par pilote, cote serveur', async () => {
    const list = spyOn(TestBed.inject(CapaService), 'listCases').and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20
    }));
    fixture.detectChanges();
    const sub = component.cases$.subscribe();
    component.ownerFilter.setValue('kc-1');
    expect(list.calls.mostRecent().args.slice(2))
      .toEqual([undefined, undefined, 'kc-1', undefined]);
    component.ownerFilter.setValue(null);
    expect(list.calls.mostRecent().args.slice(2))
      .toEqual([undefined, undefined, undefined, undefined]);
    sub.unsubscribe();
  });

  /**
   * L'ECHEANCE LIMITE PART AU SERVEUR, qui pagine : « qu'est-ce qui tombe
   * cette semaine » se tranchait a l'oeil, colonne par colonne, et trier la
   * colonne Echeance ne repondait que pour la page affichee.
   */
  it('filtre la liste par échéance limite, côté serveur', async () => {
    const list = spyOn(TestBed.inject(CapaService), 'listCases').and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20
    }));
    fixture.detectChanges();
    const sub = component.cases$.subscribe();

    component.dueBeforeFilter.setValue(new Date(2026, 11, 15));
    expect(list.calls.mostRecent().args.slice(2))
      .toEqual([undefined, undefined, undefined, '2026-12-15']);

    component.effacerEcheance();
    expect(list.calls.mostRecent().args.slice(2))
      .toEqual([undefined, undefined, undefined, undefined]);
    sub.unsubscribe();
  });

  /**
   * LA DATE SE LIT SUR SES COMPOSANTES LOCALES. `toISOString()` repasse par
   * UTC et recule d'un jour a l'est de Greenwich : le 15 demande partirait
   * comme le 14, et la liste perdrait les dossiers qui echoient ce jour-la.
   */
  it('envoie le jour choisi, sans décalage de fuseau', () => {
    // 1er janvier a minuit : le cas ou UTC change l'annee, pas seulement le jour.
    expect(CapaListComponent.enDateIso(new Date(2027, 0, 1))).toBe('2027-01-01');
    expect(CapaListComponent.enDateIso(null)).toBeNull();
    expect(CapaListComponent.enDateIso(new Date('pas-une-date'))).toBeNull();
  });

  /**
   * TROIS ETATS AU CHOIX : ouvert, en cours, clos. « Rejete » n'est pas une
   * etape du traitement mais la decision de ne pas traiter, et on ne vient pas
   * au registre pour en faire la revue. Les dossiers des etats retires restent
   * visibles sous « Tous » : le choix se reduit, la liste ne se tronque pas.
   */
  it('ne propose au filtre que les trois états du traitement', () => {
    expect(component.statuses).toEqual(['OPEN', 'IN_PROGRESS', 'RESOLVED']);
  });

  it('computes badge classes from status and criticity', () => {
    expect(component.statusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.criticityBadge('CRITICAL')).toBe('crit crit-critical');
  });

  it('clamps page index and size on onPage', () => {
    component.onPage({ pageIndex: -3, pageSize: 999 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);

    component.onPage({ pageIndex: 2, pageSize: 0 } as PageEvent);
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(1);
  });

  it('navigates to the case detail on openCase', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openCase({ id: 'capa-42' } as never);
    expect(nav).toHaveBeenCalledWith(['/capa', 'capa-42']);
  });
});

/**
 * Chargement de la liste contre l'API réelle : filtre, pagination et surtout
 * le chemin d'erreur, qui doit rester lisible sans divulguer le détail serveur
 * (OWASP A09).
 */
describe('CapaListComponent (chargement API)', () => {
  let component: CapaListComponent;
  let fixture: ComponentFixture<CapaListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  const aCase = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: 'c1', tenantId: 't1', title: 'Cas', type: 'CORRECTIVE', criticity: 'HIGH',
    status: 'OPEN', sourceType: 'AUDIT', ownerId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    actions: [], ...over
  });

  const page = (content: CapaCaseResponse[], totalElements = content.length) =>
    ({ content, totalElements, totalPages: 1, number: 0, size: 20 });

  /** Le dialogue de création est remplacé : on ne teste ici que la réaction de la liste. */
  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    await TestBed.configureTestingModule({
      declarations: [CapaListComponent],
      // UserPickerModule : le filtre de pilote choisit la personne dans
      // l'annuaire, et sans lui le champ n'a aucun accesseur de valeur.
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
                MatDatepickerModule, MatNativeDateModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: DirectoryService, useValue: annuaire() }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    component.ngOnInit();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('charge la première page sans filtre au démarrage', (done) => {
    component.cases$.subscribe(list => {
      expect(list.length).toBe(1);
      expect(component.totalElements).toBe(37);
      done();
    });

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([aCase()], 37));
  });

  it('relance la requête avec le statut choisi dans le filtre', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([aCase()]));

    component.statusFilter.setValue('REJECTED');
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('REJECTED');
    req.flush(page([]));
  });

  it('repasse sans filtre quand le statut est remis à vide', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.statusFilter.setValue('OPEN');
    http.expectOne(r => r.url === base).flush(page([]));

    component.statusFilter.setValue('');
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([]));
  });

  it('recharge la page demandée par le paginateur', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.onPage({ pageIndex: 3, pageSize: 50 } as PageEvent);
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(page([]));
  });

  it('affiche un message générique sur erreur serveur, sans détail technique', async () => {
    component.cases$.subscribe();
    const errorPromise = firstValueFrom(component.error$.pipe(filter(Boolean), take(1)));

    http.expectOne(r => r.url === base)
      .flush({ title: 'NullPointerException at CapaService.java:42' },
             { status: 500, statusText: 'Server Error' });

    const message = await errorPromise;
    expect(message).toContain('Erreur serveur');
    expect(message).not.toContain('NullPointer');
  });

  it('signale un accès refusé sans laisser la vue en chargement', async () => {
    component.cases$.subscribe();
    const errorPromise = firstValueFrom(component.error$.pipe(filter(Boolean), take(1)));

    http.expectOne(r => r.url === base).flush({}, { status: 403, statusText: 'Forbidden' });

    expect(await errorPromise).toContain('droits');
    expect(await firstValueFrom(component.loading$)).toBeFalse();
  });

  it('revient en première page et recharge après une création', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.onPage({ pageIndex: 4, pageSize: 20 } as PageEvent);
    http.expectOne(r => r.url === base).flush(page([]));

    expect(component.pageIndex).toBe(4);

    stubDialog(aCase({ id: 'nouveau' }));
    component.openCreate();
    expect(component.pageIndex).toBe(0);

    // Retour page 0 puis rafraîchissement : switchMap annule la requête intermédiaire,
    // seule la dernière atteint le serveur.
    const pending = http.match(r => r.url === base).filter(r => !r.cancelled);
    expect(pending.length).toBe(1);
    expect(pending[0].request.params.get('page')).toBe('0');
    pending[0].flush(page([aCase({ id: 'nouveau' })]));
  });

  it('ne recharge rien quand la création est annulée', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    stubDialog(undefined);
    component.openCreate();
    http.expectNone(r => r.url === base);
    expect(component.pageIndex).toBe(0);
  });
});

describe('CapaListComponent — création ouverte par un lien', () => {

  it('/capa?nouveau=1 ouvre la création préremplie et nettoie l’adresse', async () => {
    const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [CapaListComponent],
      // UserPickerModule : le filtre de pilote choisit la personne dans
      // l'annuaire, et sans lui le champ n'a aucun accesseur de valeur.
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
                MatDatepickerModule, MatNativeDateModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: DirectoryService, useValue: annuaire() },
        { provide: MatDialog, useValue: dialog },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({
          nouveau: '1', titre: 'Écart ISO 45001 · 6', ref: 'iso-45001 §6', description: 'x'.repeat(5000)
        }) } } }
      ]
    }).compileComponents();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    const fixture = TestBed.createComponent(CapaListComponent);
    fixture.componentInstance.ngOnInit();
    await Promise.resolve();

    const data = (dialog.open.calls.mostRecent().args[1]!.data as { prefill: { title: string; sourceRef: string; description: string } }).prefill;
    expect(data.title).toBe('Écart ISO 45001 · 6');
    expect(data.sourceRef).toBe('iso-45001 §6');
    expect(data.description.length).toBe(4000);
    expect(router.navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: {}, replaceUrl: true }));
  });

  it('sans le paramètre, rien ne s’ouvre', async () => {
    const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [CapaListComponent],
      // UserPickerModule : le filtre de pilote choisit la personne dans
      // l'annuaire, et sans lui le champ n'a aucun accesseur de valeur.
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
                MatDatepickerModule, MatNativeDateModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: DirectoryService, useValue: annuaire() },
        { provide: MatDialog, useValue: dialog },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } }
      ]
    }).compileComponents();
    TestBed.createComponent(CapaListComponent).componentInstance.ngOnInit();
    await Promise.resolve();
    expect(dialog.open).not.toHaveBeenCalled();
  });
});
