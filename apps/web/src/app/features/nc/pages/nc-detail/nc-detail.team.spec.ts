import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { NcService } from '../../nc.service';
import { NcResponse, NcStatus, TeamMemberRef } from '../../nc.types';
import { NcDetailComponent } from './nc-detail.component';
import {
  NcCauseAnalysisStubModule
} from '../../components/nc-cause-analysis/testing/nc-cause-analysis.stub';
import { CapaActionPanelStubModule } from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';

const EQUIPE: TeamMemberRef[] = [
  { accountId: 'u-alice', displayName: 'Alice Martin' },
  { accountId: 'u-bruno', displayName: 'Bruno Lefèvre' }
];

function ecart(overrides: Partial<NcResponse> = {}): NcResponse {
  return {
    id: 'nc-1', reference: 'NC-2026-1001', title: 'Étiquetage manquant',
    category: 'PROCESS', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
    detectedAt: '2026-06-06T00:00:00Z', createdAt: '2026-06-06T00:00:00Z',
    updatedAt: '2026-06-06T00:00:00Z', teamMembers: EQUIPE, ...overrides
  };
}

/**
 * L'équipe en charge, lue et remaniée depuis la fiche de l'écart.
 *
 * <p>C'est le D1 du 8D : il se retapait dans le rapport alors que la fiche sait
 * depuis le premier jour qui s'en occupe. Ce banc tient trois promesses : les
 * personnes sont nommées par leur NOM, un écart sans équipe le dit au lieu de
 * laisser un blanc, et on ne retire pas la dernière personne d'un écart vivant.
 */
describe('NcDetailComponent — équipe en charge', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => Array.from(hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`));

  function setup(nc: NcResponse): void {
    svc.getNc.and.returnValue(of(nc));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    // reload$ : l'async pipe ne s'abonne qu'au cycle suivant (parité avec le flux réel).
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getNc', 'listPhotos', 'updateNc', 'subcategories'
    ]);
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    svc.listPhotos.and.returnValue(of([]));
    svc.updateNc.and.callFake((_id, input) => of(ecart({ teamMembers: input.teamMembers })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const annuaire = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    annuaire.search.and.returnValue(of({
      content: [{
        id: 'id-chloe', accountId: 'u-chloe', displayName: 'Chloé Nguyen',
        email: 'chloe@exemple.fr', active: true
      }],
      page: 0, size: 20, totalElements: 1, totalPages: 1
    }));
    annuaire.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
                CapaActionPanelStubModule, NcCauseAnalysisStubModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: MatSnackBar, useValue: snack },
        { provide: DirectoryService, useValue: annuaire },
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u-alice' }) } },
        { provide: ConnectivityService, useValue: { online$: of(true), isOnline: () => true } },
        {
          provide: FiveWhysService,
          useValue: jasmine.createSpyObj<FiveWhysService>('FiveWhysService', { listForNc: of([]) })
        },
        {
          provide: IshikawaService,
          useValue: jasmine.createSpyObj<IshikawaService>('IshikawaService', { listForNc: of([]) })
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'nc-1' }) } }
        }
      ]
    }).compileComponents();
  });

  // ---------- la lecture ----------

  it('nomme l’équipe par ses noms, le premier en pilote', () => {
    setup(ecart());
    const lignes = tous('equipe-membre');
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('Alice Martin');
    expect(lignes[1].textContent).toContain('Bruno Lefèvre');
    expect(tous('equipe-pilote').length).toBe(1);
    // Jamais l'identifiant de compte à l'écran.
    expect(hote().textContent).not.toContain('u-alice');
  });

  // Les écarts ouverts par une machine (constat d'audit, connecteur EHR) et ceux
  // saisis avant cette donnée n'ont pas d'équipe : un blanc passerait pour un bug.
  it('dit qu’aucune équipe n’est nommée au lieu de laisser un blanc', () => {
    setup(ecart({ teamMembers: [] }));
    expect(un('equipe-vide')).not.toBeNull();
    expect(un('equipe-vide')!.textContent).toContain('8D');
    expect(tous('equipe-membre').length).toBe(0);
  });

  it('supporte une fiche ancienne qui ne porte pas la donnée', () => {
    setup(ecart({ teamMembers: undefined }));
    expect(un('equipe-vide')).not.toBeNull();
  });

  // ---------- le remaniement ----------

  it('ouvre le remaniement sur l’équipe en place, pas sur une liste vide', () => {
    setup(ecart());
    un('equipe-modifier')!.click();
    fixture.detectChanges();
    expect(component.teamEditing).toBeTrue();
    expect(component.teamDraft).toEqual(EQUIPE);
    expect(un('equipe-champ')).not.toBeNull();
  });

  it('enregistre l’équipe remaniée et recharge la fiche', () => {
    setup(ecart());
    un('equipe-modifier')!.click();
    fixture.detectChanges();
    component.teamDraftControl.setValue([EQUIPE[1], EQUIPE[0]]);
    fixture.detectChanges();

    un('equipe-enregistrer')!.click();
    fixture.detectChanges();

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1', { teamMembers: [EQUIPE[1], EQUIPE[0]] });
    expect(component.teamEditing).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
    // Deux chargements : l'ouverture de la fiche, puis le rechargement.
    expect(svc.getNc.calls.count()).toBeGreaterThan(1);
  });

  // Abandonner doit rendre l'équipe d'avant, sans recharger la page.
  it('abandonne le remaniement sans rien envoyer', () => {
    setup(ecart());
    un('equipe-modifier')!.click();
    fixture.detectChanges();
    component.teamDraftControl.setValue([]);
    un('equipe-annuler')!.click();
    fixture.detectChanges();

    expect(svc.updateNc).not.toHaveBeenCalled();
    expect(component.teamEditing).toBeFalse();
    expect(tous('equipe-membre').length).toBe(2);
  });

  // Un écart dont plus personne ne répond ne se revoit pas : le serveur refuse
  // une équipe vide, et l'écran ne propose pas de l'envoyer.
  it('n’enregistre pas une équipe vidée', () => {
    setup(ecart());
    un('equipe-modifier')!.click();
    fixture.detectChanges();
    component.teamDraftControl.setValue([]);
    fixture.detectChanges();

    expect((un('equipe-enregistrer') as HTMLButtonElement).disabled).toBeTrue();
    component.saveTeam();
    expect(svc.updateNc).not.toHaveBeenCalled();
  });

  it('dit l’échec de l’enregistrement et garde le remaniement ouvert', () => {
    setup(ecart());
    un('equipe-modifier')!.click();
    fixture.detectChanges();
    svc.updateNc.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.saveTeam();
    fixture.detectChanges();

    expect(snack.open).toHaveBeenCalled();
    expect(component.teamEditing).toBeTrue();
    expect(component.teamDraft).toEqual(EQUIPE);
  });

  // ---------- les écarts clos ----------

  const termines: NcStatus[] = ['CLOSED', 'CANCELLED', 'REJECTED'];
  termines.forEach(status => {
    it(`ne propose pas de remanier l’équipe d’un écart ${status}`, () => {
      setup(ecart({ status }));
      expect(component.canEditTeam(status)).toBeFalse();
      expect(un('equipe-modifier')).toBeNull();
      // L'équipe reste LISIBLE : c'est l'archive du D1.
      expect(tous('equipe-membre').length).toBe(2);
    });
  });

  it('propose de remanier l’équipe tant que l’écart vit', () => {
    setup(ecart());
    (['OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED'] as NcStatus[]).forEach(status => {
      expect(component.canEditTeam(status)).withContext(status).toBeTrue();
    });
    expect(un('equipe-modifier')).not.toBeNull();
  });
});
