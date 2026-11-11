import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../../core/directory/directory.service';
import { SharedModule } from '../../../../../shared/shared.module';
import { UiModule } from '../../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../../shared/user-picker/user-picker.module';
import { SpcService } from '../../../spc.service';
import { ControlChartComponent } from '../../components/control-chart/control-chart.component';
import { DistributionChartComponent } from '../../components/distribution-chart/distribution-chart.component';
import { sheet } from '../../spc-capability.fixtures';
import { SpcCapabilityService } from '../../spc-capability.service';
import { ImportedGrid } from '../../spc-capability.types';
import { CapabilityStudyComponent } from './capability-study.component';

/**
 * L'import d'un classeur Excel dans la grille de mesures.
 *
 * <p>Le collage suffit pour vingt colonnes ; au-delà, les mesures d'un poste de
 * contrôle vivent dans un fichier que personne ne veut ouvrir pour sélectionner
 * la bonne plage. Ce banc tient surtout deux promesses : <b>rien n'est
 * enregistré</b> par un import — la grille se relit et c'est l'utilisateur qui
 * enregistre — et <b>l'import n'est jamais muet</b> : ce qu'il a écarté se dit.
 */
describe('CapabilityStudyComponent — import Excel', () => {

  let fixture: ComponentFixture<CapabilityStudyComponent>;
  let component: CapabilityStudyComponent;
  let service: jasmine.SpyObj<SpcCapabilityService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let confirme: boolean;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const GRILLE: ImportedGrid = {
    sampleSize: 3, subgroupCount: 4,
    rows: [
      ['49.8', '50.1', '49.9', '50.0'],
      ['50.0', '49.9', '50.2', '49.8'],
      ['49.7', '50.2', '50.1', '49.9']
    ],
    warnings: []
  };

  async function setup(opts: { canManage?: boolean } = {}): Promise<void> {
    service = jasmine.createSpyObj<SpcCapabilityService>('SpcCapabilityService',
      ['get', 'create', 'update', 'settings', 'measurementsTemplate', 'importMeasurements']);
    service.settings.and.returnValue(of({ capableThreshold: 1.5, watchThreshold: 1.2, defaults: false }));
    service.get.and.returnValue(of(sheet({})));
    service.importMeasurements.and.returnValue(of(GRILLE));
    service.measurementsTemplate.and.returnValue(of(new HttpResponse<Blob>({
      body: new Blob(['x'], { type: 'application/octet-stream' }),
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="mesures-capabilite-5x20.xlsx"' })
    })));
    const spc = jasmine.createSpyObj<SpcService>('SpcService', ['analyze']);
    spc.analyze.and.returnValue(throwError(() => ({ status: 503 })));
    confirme = true;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake((() => ({ afterClosed: () => of(confirme) })) as never);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    // L'annuaire n'est pas le sujet : le selecteur de meneur reste vide.
    directory.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [CapabilityStudyComponent, ControlChartComponent, DistributionChartComponent],
      imports: [SharedModule, UiModule, FormsModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: SpcCapabilityService, useValue: service },
        { provide: SpcService, useValue: spc },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: MatDialog, useValue: dialog },
        { provide: DirectoryService, useValue: directory },
        { provide: AuthzService, useValue: { can: () => of(opts.canManage ?? true) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } }
      ]
    }).compileComponents();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(CapabilityStudyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Un faux champ de fichier, tel que l'événement `change` le présente. */
  function choisir(nom: string): Event {
    const fichier = new File(['octets'], nom, { type: 'application/octet-stream' });
    const champ = document.createElement('input');
    champ.type = 'file';
    const transfert = new DataTransfer();
    transfert.items.add(fichier);
    champ.files = transfert.files;
    const evenement = new Event('change');
    Object.defineProperty(evenement, 'target', { value: champ, writable: false });
    return evenement;
  }

  // ---------- le modèle à télécharger ----------

  it('télécharge le modèle aux dimensions de l’étude en cours', async () => {
    await setup();
    component.form.patchValue({ subgroupSize: 3 });
    component.onSubgroupCount(12);
    fixture.detectChanges();

    component.downloadTemplate();
    expect(service.measurementsTemplate).toHaveBeenCalledWith(3, 12);
    expect(component.downloadingTemplate).toBeFalse();
    expect(component.importError).toBeNull();
  });

  it('dit que le modèle n’a pas pu être téléchargé', async () => {
    await setup();
    service.measurementsTemplate.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.downloadTemplate();
    fixture.detectChanges();
    expect(component.downloadingTemplate).toBeFalse();
    expect(un('import-erreur')?.textContent).toContain('modèle');
  });

  // ---------- l'import ----------

  it('pose la grille lue, sa largeur et sa hauteur', async () => {
    await setup();
    component.onWorkbookChosen(choisir('mesures.xlsx'));
    fixture.detectChanges();

    expect(service.importMeasurements).toHaveBeenCalled();
    expect(component.subgroupCount).toBe(4);
    expect(component.form.controls.subgroupSize.value).toBe(3);
    expect(component.cell(0, 0)).toBe('49.8');
    expect(component.cell(2, 3)).toBe('49.9');
    // Les mesures entrent dans le calcul sans ressaisie.
    expect(component.calc.subgroupMeans[0]).toBeCloseTo((49.8 + 50.0 + 49.7) / 3, 6);
  });

  // Un import qui enregistrerait l'étude empêcherait de relire ce qui a été lu
  // avant de le signer.
  it('n’enregistre rien : l’étude reste à enregistrer', async () => {
    await setup();
    component.onWorkbookChosen(choisir('mesures.xlsx'));
    expect(service.create).not.toHaveBeenCalled();
    expect(service.update).not.toHaveBeenCalled();
    expect(component.form.dirty).toBeTrue();
  });

  it('demande confirmation avant d’écraser des mesures déjà saisies', async () => {
    await setup();
    component.onWorkbookChosen(choisir('premier.xlsx'));
    expect(dialog.open).not.toHaveBeenCalled();

    confirme = false;
    component.onWorkbookChosen(choisir('second.xlsx'));
    expect(dialog.open).toHaveBeenCalled();
    expect(service.importMeasurements).toHaveBeenCalledTimes(1);
  });

  it('importe après confirmation quand la grille porte déjà des mesures', async () => {
    await setup();
    component.onWorkbookChosen(choisir('premier.xlsx'));
    confirme = true;
    component.onWorkbookChosen(choisir('second.xlsx'));
    expect(service.importMeasurements).toHaveBeenCalledTimes(2);
  });

  // Choisir deux fois le même fichier ne déclenche aucun événement si le champ
  // garde sa valeur : l'utilisateur croit que l'import ne répond plus.
  it('remet le champ de fichier à zéro après chaque choix', async () => {
    await setup();
    const evenement = choisir('mesures.xlsx');
    component.onWorkbookChosen(evenement);
    expect((evenement.target as HTMLInputElement).value).toBe('');
  });

  it('refuse une extension autre que .xlsx sans appeler le serveur', async () => {
    await setup();
    component.onWorkbookChosen(choisir('mesures.csv'));
    fixture.detectChanges();
    expect(service.importMeasurements).not.toHaveBeenCalled();
    expect(un('import-erreur')?.textContent).toContain('.xlsx');
  });

  it('ne tente rien en consultation seule', async () => {
    await setup({ canManage: false });
    component.onWorkbookChosen(choisir('mesures.xlsx'));
    expect(service.importMeasurements).not.toHaveBeenCalled();
    expect(un('import')).toBeNull();
  });

  // ---------- ce que la lecture a écarté ----------

  it('affiche les avertissements du serveur', async () => {
    await setup();
    service.importMeasurements.and.returnValue(of({
      ...GRILLE,
      warnings: ['2 cellule(s) non numérique(s) ignorée(s).', '30 lignes lues : seules les 25 premières sont conservées.']
    }));
    component.onWorkbookChosen(choisir('mesures.xlsx'));
    fixture.detectChanges();

    const bloc = un('import-avertissements');
    expect(bloc).not.toBeNull();
    expect(bloc!.textContent).toContain('non numérique');
    expect(bloc!.querySelectorAll('li').length).toBe(2);
  });

  it('oublie les avertissements du précédent import', async () => {
    await setup();
    service.importMeasurements.and.returnValue(of({ ...GRILLE, warnings: ['un souci'] }));
    component.onWorkbookChosen(choisir('un.xlsx'));
    expect(component.importWarnings).toEqual(['un souci']);

    service.importMeasurements.and.returnValue(of(GRILLE));
    component.onWorkbookChosen(choisir('deux.xlsx'));
    expect(component.importWarnings).toEqual([]);
  });

  it('dit le refus du serveur sans reprendre son texte brut', async () => {
    await setup();
    service.importMeasurements.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 400, error: { detail: 'Aucune mesure trouvée dans le classeur.' }
    })));
    component.onWorkbookChosen(choisir('vide.xlsx'));
    fixture.detectChanges();

    expect(component.importing).toBeFalse();
    expect(un('import-erreur')).not.toBeNull();
  });

  it('garde la grille intacte quand l’import échoue', async () => {
    await setup();
    component.onWorkbookChosen(choisir('bon.xlsx'));
    const avant = component.cell(0, 0);

    service.importMeasurements.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.onWorkbookChosen(choisir('mauvais.xlsx'));
    expect(component.cell(0, 0)).toBe(avant);
  });
});
