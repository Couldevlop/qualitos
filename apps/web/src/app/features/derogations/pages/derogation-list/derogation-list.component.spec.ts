import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { DerogationsService } from '../../derogations.service';
import { DerogationPage, DerogationRow } from '../../derogations.types';
import { DerogationListComponent } from './derogation-list.component';

function ligne(ref: string, extra: Partial<DerogationRow> = {}): DerogationRow {
  return {
    id: 'id-' + ref,
    reference: ref,
    status: 'DRAFT',
    createdAt: '2026-11-20T09:00:00Z',
    partReference: 'P-1',
    customerName: 'ACME',
    createdByName: 'Paul Girard',
    firstDeviation: 'Ø12 ±0,02 → Ø12,05',
    attachmentsNote: 'Relevé dimensionnel',
    customerDecision: 'PENDING',
    ...extra
  };
}

function page(lignes: DerogationRow[], total = lignes.length): DerogationPage {
  return { content: lignes, total, page: 0, size: 20 };
}

describe('DerogationListComponent', () => {

  let service: jasmine.SpyObj<DerogationsService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let router: jasmine.SpyObj<Router>;
  let fixture: ComponentFixture<DerogationListComponent>;
  let composant: DerogationListComponent;

  async function monter(roles: string[] = ['QUALITY_MANAGER']): Promise<void> {
    service = jasmine.createSpyObj<DerogationsService>('DerogationsService',
      ['search', 'myApprovals', 'myApprovalCount']);
    service.search.and.returnValue(of(page([ligne('DER-2026-0001')])));
    service.myApprovals.and.returnValue(of(page([])));
    service.myApprovalCount.and.returnValue(of({ count: 0 }));

    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    // Remis a zero : un meme banc remonte l ecran deux fois pour comparer deux
    // jeux de roles, et reconfigurer un TestBed deja instancie echoue.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationListComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        // Le selecteur d'annuaire tire DirectoryService, qui tire HttpClient.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: DerogationsService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: Router, useValue: router },
        {
          provide: AuthService,
          useValue: { hasAnyRole: (r: string[]) => r.some(x => roles.includes(x)) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DerogationListComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('charge le registre ET le compteur des validations dès l\'arrivée', async () => {
    await monter();
    expect(service.search).toHaveBeenCalled();
    // Le compteur se charge MÊME si l'onglet n'est pas ouvert : c'est tout
    // l'intérêt d'une alerte, elle se voit sans qu'on la cherche.
    expect(service.myApprovalCount).toHaveBeenCalled();
  });

  it('montre l\'alerte rouge et son nombre quand des dossiers attendent', async () => {
    await monter();
    service.myApprovalCount.and.returnValue(of({ count: 3 }));
    composant['compterMesValidations']();
    fixture.detectChanges();

    const alerte = hote().querySelector('[data-test="compteur-validations"]');
    expect(alerte?.textContent?.trim()).toBe('3');
    // Le nombre est aussi annoncé aux lecteurs d'écran : une pastille colorée
    // ne dit rien à qui ne la voit pas.
    expect(alerte?.getAttribute('aria-label')).toContain('3');
  });

  it('ne montre aucune alerte à zéro : une pastille vide inquiète pour rien', async () => {
    await monter();
    expect(hote().querySelector('[data-test="compteur-validations"]')).toBeNull();
  });

  it('un échec du compteur reste silencieux, le registre restant utilisable', async () => {
    service = jasmine.createSpyObj<DerogationsService>('DerogationsService',
      ['search', 'myApprovals', 'myApprovalCount']);
    service.search.and.returnValue(of(page([ligne('DER-2026-0001')])));
    service.myApprovals.and.returnValue(of(page([])));
    service.myApprovalCount.and.returnValue(throwError(() => ({ status: 500 })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    // Remis a zero : un meme banc remonte l ecran deux fois pour comparer deux
    // jeux de roles, et reconfigurer un TestBed deja instancie echoue.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationListComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        // Le selecteur d'annuaire tire DirectoryService, qui tire HttpClient.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: DerogationsService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: { hasAnyRole: () => true } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DerogationListComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();

    expect(composant.aValider).toBe(0);
    // Pas de bandeau d'erreur : un compteur absent vaut mieux qu'une alarme
    // sur un écran par ailleurs fonctionnel.
    expect(snack.open).not.toHaveBeenCalled();
    expect(composant.lignes.length).toBe(1);
  });

  it('l\'onglet « Mes validations » appelle SA route, sans les filtres du registre',
    async () => {
      await monter();
      service.myApprovals.and.returnValue(of(page([ligne('DER-2026-0009')])));
      composant.filtres.controls.customerName.setValue('ACME');

      composant.ouvrirOnglet('validations');
      fixture.detectChanges();

      // Ce qu'on doit signer nous concerne PAR DÉFINITION : y appliquer un
      // filtre de client ferait disparaître une signature attendue.
      expect(service.myApprovals).toHaveBeenCalledWith(0, 20);
      expect(composant.lignes[0].reference).toBe('DER-2026-0009');
      expect(hote().querySelector('.filtres')).toBeNull();
    });

  it('revenir au registre réapplique les filtres saisis', async () => {
    await monter();
    composant.ouvrirOnglet('validations');
    composant.filtres.controls.status.setValue('TO_AMEND');
    composant.ouvrirOnglet('registre');

    expect(service.search).toHaveBeenCalledWith(
      jasmine.objectContaining({ status: 'TO_AMEND', page: 0, size: 20 }));
  });

  it('changer d\'onglet revient à la première page', async () => {
    await monter();
    service.search.and.returnValue(of(page([ligne('DER-2026-0001')], 100)));
    composant.charger();
    composant.pageSuivante();
    expect(composant.page).toBe(1);

    composant.ouvrirOnglet('validations');
    // Sans cela, on atterrit sur la page 2 d'une liste qui n'en a qu'une, et
    // l'onglet paraît vide alors qu'il porte des dossiers.
    expect(composant.page).toBe(0);
  });

  it('le filtre « créateur » envoie l\'identifiant de compte, jamais un nom tapé',
    async () => {
      await monter();
      composant.surChoixDuCreateur({
        id: 'interne-1', accountId: 'kc-paul', displayName: 'Paul Girard'
      } as never);

      expect(service.search).toHaveBeenCalledWith(
        jasmine.objectContaining({ owner: 'kc-paul' }));
    });

  it('vider le choix du créateur retire le filtre au lieu d\'envoyer du vide',
    async () => {
      await monter();
      composant.surChoixDuCreateur(null);
      expect(service.search).toHaveBeenCalledWith(
        jasmine.objectContaining({ owner: null }));
    });

  it('n\'offre « Nouvelle dérogation » qu\'aux profils qui peuvent en ouvrir',
    async () => {
      await monter(['QUALITY_USER']);
      expect(hote().querySelector('[data-test="nouvelle-derogation"]')).toBeNull();

      await monter(['QUALITY_MANAGER']);
      expect(hote().querySelector('[data-test="nouvelle-derogation"]')).not.toBeNull();
    });

  it('montre la nuance « acceptée sous conditions », que l\'état ne porte pas',
    async () => {
      service = jasmine.createSpyObj<DerogationsService>('DerogationsService',
        ['search', 'myApprovals', 'myApprovalCount']);
      service.search.and.returnValue(of(page([ligne('DER-2026-0002',
        { status: 'ACCEPTED', customerDecision: 'ACCEPTED_WITH_CONDITIONS' })])));
      service.myApprovals.and.returnValue(of(page([])));
      service.myApprovalCount.and.returnValue(of({ count: 0 }));
      snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
      router = jasmine.createSpyObj<Router>('Router', ['navigate']);

      await TestBed.configureTestingModule({
        declarations: [DerogationListComponent],
        imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
        providers: [
        // Le selecteur d'annuaire tire DirectoryService, qui tire HttpClient.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
          { provide: DerogationsService, useValue: service },
          { provide: MatSnackBar, useValue: snack },
          { provide: Router, useValue: router },
          { provide: AuthService, useValue: { hasAnyRole: () => true } }
        ]
      }).compileComponents();
      fixture = TestBed.createComponent(DerogationListComponent);
      fixture.detectChanges();

      // « Acceptée sous conditions » VAUT un accord, et ce n'est pas la même
      // chose qu'une acceptation : le ramener à l'état perdrait la condition.
      expect(hote().textContent).toContain('sous conditions');
    });

  it('un échec du registre se dit, et ne laisse pas de lignes périmées à l\'écran',
    async () => {
      await monter();
      service.search.and.returnValue(throwError(() => ({ status: 503 })));
      composant.charger();
      fixture.detectChanges();

      expect(composant.echec).toBeTrue();
      expect(composant.lignes.length).toBe(0);
      expect(composant.chargement).toBeFalse();
      expect(snack.open).toHaveBeenCalled();
    });

  it('ouvre le dossier au clic sur sa ligne', async () => {
    await monter();
    composant.ouvrir(ligne('DER-2026-0003'));
    expect(router.navigate).toHaveBeenCalledWith(['/derogations', 'id-DER-2026-0003']);
  });

  it('la pagination se borne au total et ne dépasse ni en avant ni en arrière',
    async () => {
      await monter();
      // Regle SUR LE BOUCHON et non sur le champ : charger() relit le total
      // aupres du serveur a chaque page, et c est juste -- une liste bouge
      // pendant qu on la parcourt. Ecrire le champ le faisait ecraser au
      // premier appel, et le banc mesurait le bouchon par defaut.
      service.search.and.returnValue(of(page([ligne('DER-2026-0001')], 25)));
      composant.charger();

      composant.pagePrecedente();
      expect(composant.page).toBe(0);
      expect(composant.depuis).toBe(1);

      composant.pageSuivante();
      expect(composant.page).toBe(1);
      expect(composant.jusqua).toBe(25);

      composant.pageSuivante();
      // Pas de page 3 pour 25 lignes de 20 : la borne évite une page vide.
      expect(composant.page).toBe(1);
    });

  it('un registre vide le dit, et le dit DIFFÉREMMENT selon l\'onglet', async () => {
    await monter();
    service.search.and.returnValue(of(page([])));
    composant.charger();
    fixture.detectChanges();
    expect(hote().textContent).toContain('Aucune dérogation ne correspond');

    composant.ouvrirOnglet('validations');
    fixture.detectChanges();
    // « Aucun dossier n'attend votre signature » est une bonne nouvelle ;
    // « aucune dérogation ne correspond » invite à élargir la recherche.
    expect(hote().textContent).toContain('Aucun dossier n\'attend votre signature');
  });

  it('effacer les filtres les vide ET relance la recherche', async () => {
    await monter();
    composant.filtres.controls.customerName.setValue('ACME');
    service.search.calls.reset();

    composant.effacerLesFiltres();

    expect(composant.filtres.controls.customerName.value).toBeNull();
    expect(service.search).toHaveBeenCalledTimes(1);
  });

  it('ouvrir l\'onglet déjà ouvert ne relance rien', async () => {
    await monter();
    service.search.calls.reset();
    composant.ouvrirOnglet('registre');
    // Sans cette garde, chaque clic sur l'onglet courant refait un appel et
    // fait scintiller la liste sous le curseur.
    expect(service.search).not.toHaveBeenCalled();
  });
});
