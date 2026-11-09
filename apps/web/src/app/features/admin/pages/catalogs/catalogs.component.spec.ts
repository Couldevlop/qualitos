import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { CatalogService } from '../../../../core/catalogs/catalog.service';
import { Catalog, CatalogSummary } from '../../../../core/catalogs/catalog.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CatalogsComponent } from './catalogs.component';

const RESUME: CatalogSummary[] = [
  { catalogue: 'fives-zone', mode: 'OPEN', defaults: false, entryCount: 2 },
  { catalogue: 'audit-type', mode: 'RENAME', defaults: true, entryCount: 6 }
];

function zones(over: Partial<Catalog> = {}): Catalog {
  return {
    catalogue: 'fives-zone', mode: 'OPEN', defaults: false,
    entries: [
      { code: 'BLOC-A', label: 'Bloc opératoire A', active: true },
      { code: 'ANCIEN', label: 'Ancienne aile', active: false }
    ],
    ...over
  };
}

function typesDAudit(over: Partial<Catalog> = {}): Catalog {
  return {
    catalogue: 'audit-type', mode: 'RENAME', defaults: true,
    entries: [
      { code: 'INTERNAL', label: 'Interne', active: true },
      { code: 'EXTERNAL', label: 'Externe', active: true }
    ],
    ...over
  };
}

/**
 * « Listes déroulantes » : lecture, édition d'une liste à la fois, ce que le
 * mode permet, enregistrement d'un bloc, retour aux valeurs de la plateforme.
 */
describe('CatalogsComponent', () => {
  let fixture: ComponentFixture<CatalogsComponent>;
  let component: CatalogsComponent;
  let service: jasmine.SpyObj<CatalogService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`);

  /** La fenêtre de confirmation répond `reponse` sans être affichée. */
  function confirmer(reponse: boolean): void {
    dialog.open.and.returnValue({ afterClosed: () => of(reponse) } as MatDialogRef<unknown>);
  }

  async function setup(resume: CatalogSummary[] | 'erreur' = RESUME,
                       premiere: Catalog | 'erreur' = zones()): Promise<void> {
    service = jasmine.createSpyObj<CatalogService>('CatalogService',
      ['summaries', 'catalog', 'replace', 'reset', 'invalidate']);
    service.summaries.and.returnValue(
      resume === 'erreur' ? throwError(() => ({ status: 500 })) : of(resume));
    service.catalog.and.returnValue(
      premiere === 'erreur' ? throwError(() => ({ status: 500 })) : of(premiere));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    confirmer(true);
    await TestBed.configureTestingModule({
      declarations: [CatalogsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: CatalogService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CatalogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('liste les listes paramétrables, ouvre la première, et signale celles déjà réglées', async () => {
    await setup();

    expect(component.summaries.length).toBe(2);
    expect(component.selected).toBe('fives-zone');
    expect(component.entries.length).toBe(2);
    const texte = hote().textContent!;
    expect(texte).toContain("Zones d'audit 5S");
    expect(texte).toContain("Types d'audit");
    // Une liste réglée se repère sans l'ouvrir.
    expect(hote().querySelectorAll('.liste__reglee').length).toBe(1);
    expect(tous('entree').length).toBe(2);
  });

  it('une liste libre s’ajoute, se retire, se renomme et se réordonne', async () => {
    await setup();

    component.ajouter();
    expect(component.entries.length).toBe(3);
    expect(component.problemes.length).toBeGreaterThan(0); // code et libellé vides

    const neuve = component.entries[2];
    component.majCode(neuve, ' bloc-c ');
    component.majLibelle(neuve, 'Bloc opératoire C');
    expect(component.entries[2].code).toBe('BLOC-C');
    expect(component.problemes).toEqual([]);

    component.monter(2);
    expect(component.entries.map(e => e.code)).toEqual(['BLOC-A', 'BLOC-C', 'ANCIEN']);
    component.descendre(0);
    expect(component.entries.map(e => e.code)).toEqual(['BLOC-C', 'BLOC-A', 'ANCIEN']);
    // Les bords ne bougent pas, et ne cassent rien.
    component.monter(0);
    component.descendre(2);
    expect(component.entries.map(e => e.code)).toEqual(['BLOC-C', 'BLOC-A', 'ANCIEN']);

    component.retirer(component.entries[2]);
    expect(component.entries.length).toBe(2);
  });

  it('une entrée se retire des nouvelles saisies sans disparaître', async () => {
    await setup();

    component.basculerService(component.entries[0]);
    fixture.detectChanges();

    expect(component.entries[0].active).toBeFalse();
    expect(component.serviceLabel(component.entries[0])).toContain('Remettre');
    expect(hote().querySelectorAll('.ligne--retiree').length).toBe(2);
  });

  /**
   * Le cœur de l'écran : sur une liste adossée au code, on renomme, on
   * réordonne, et c'est tout. Le bouton d'ajout n'existe même pas.
   */
  it('une liste adossée au code ne s’ajoute pas, ne se retire pas, ne se désactive pas', async () => {
    await setup(RESUME, typesDAudit());
    fixture.detectChanges();

    expect(component.renameOnly).toBeTrue();
    expect(un('ajouter')).toBeNull();
    expect(hote().textContent).toContain('Codes de la plateforme');
    expect(hote().querySelector('.ligne__code-fixe')!.textContent).toContain('INTERNAL');

    component.ajouter();
    component.retirer(component.entries[0]);
    component.basculerService(component.entries[0]);
    component.majCode(component.entries[0], 'AUTRE');
    expect(component.entries.map(e => e.code)).toEqual(['INTERNAL', 'EXTERNAL']);
    expect(component.entries[0].active).toBeTrue();

    // Renommer et réordonner, en revanche, sont son usage même.
    component.majLibelle(component.entries[0], 'Audit interne');
    component.monter(1);
    expect(component.entries.map(e => e.code)).toEqual(['EXTERNAL', 'INTERNAL']);
    expect(component.dirty).toBeTrue();
    expect(component.problemes).toEqual([]);
  });

  it('dit en clair ce qui empêche d’enregistrer', async () => {
    await setup();

    component.entries = [{ code: 'bloc a', label: '', active: true }];
    expect(component.problemes.join(' ')).toContain('capitales');
    expect(component.problemes.join(' ')).toContain('libellé');

    component.entries = [
      { code: 'BLOC-A', label: 'A', active: true },
      { code: 'BLOC-A', label: 'A bis', active: true }
    ];
    expect(component.problemes.join(' ')).toContain('même code');

    component.entries = [{ code: 'BLOC-A', label: 'A', active: false }];
    expect(component.problemes.join(' ')).toContain('en service');

    component.entries = [];
    expect(component.problemes.join(' ')).toContain('vide');

    component.entries = Array.from({ length: component.limits.entries + 1 },
      (_, i) => ({ code: `Z${i}`, label: `Zone ${i}`, active: true }));
    expect(component.problemes.join(' ')).toContain(String(component.limits.entries));
  });

  it('enregistre la liste d’un bloc, dans l’ordre affiché, et met le résumé à jour', async () => {
    await setup();
    service.replace.and.returnValue(of(zones({ entries: [
      { code: 'BLOC-A', label: 'Bloc A', active: true }
    ] })));

    component.majLibelle(component.entries[0], 'Bloc A');
    component.retirer(component.entries[1]);
    fixture.detectChanges();
    un('enregistrer')!.click();

    expect(service.replace).toHaveBeenCalledWith('fives-zone',
      [{ code: 'BLOC-A', label: 'Bloc A', active: true }]);
    expect(component.dirty).toBeFalse();
    expect(component.summaries[0].entryCount).toBe(1);
    expect(snack.open).toHaveBeenCalled();
  });

  it('n’enregistre pas une liste inchangée, ni une liste en faute', async () => {
    await setup();

    component.enregistrer();
    expect(service.replace).not.toHaveBeenCalled();

    component.majCode(component.entries[0], 'BLOC_A');
    component.entries = [...component.entries, { code: '', label: '', active: true }];
    component.enregistrer();
    expect(service.replace).not.toHaveBeenCalled();
  });

  it('annuler les changements remet la liste telle qu’elle était', async () => {
    await setup();

    component.majLibelle(component.entries[0], 'Autre nom');
    expect(component.dirty).toBeTrue();

    component.annuler();

    expect(component.dirty).toBeFalse();
    expect(component.entries[0].label).toBe('Bloc opératoire A');
  });

  it('rétablir demande confirmation, puis revient aux valeurs de la plateforme', async () => {
    await setup();
    service.reset.and.returnValue(of(zones({ defaults: true })));

    confirmer(false);
    component.retablir();
    expect(service.reset).not.toHaveBeenCalled();

    confirmer(true);
    component.retablir();

    expect(service.reset).toHaveBeenCalledWith('fives-zone');
    expect(component.defaults).toBeTrue();
    expect(component.summaries[0].defaults).toBeTrue();
  });

  it('ouvrir une autre liste la charge ; des changements non enregistrés sont confirmés d’abord', async () => {
    await setup();

    component.majLibelle(component.entries[0], 'Changé');
    confirmer(false);
    component.ouvrir('audit-type');
    expect(component.selected).toBe('fives-zone');
    expect(component.entries[0].label).toBe('Changé');

    confirmer(true);
    service.catalog.and.returnValue(of(typesDAudit()));
    component.ouvrir('audit-type');

    expect(service.catalog).toHaveBeenCalledWith('audit-type');
    expect(component.selected).toBe('audit-type');
    expect(component.mode).toBe('RENAME');
    expect(component.dirty).toBeFalse();
  });

  it('un échec de lecture le dit et laisse réessayer', async () => {
    await setup('erreur');
    fixture.detectChanges();

    expect(component.failed).toBeTrue();
    expect(hote().textContent).toContain("n'ont pas pu être chargées");
    expect(snack.open).toHaveBeenCalled();

    service.summaries.and.returnValue(of(RESUME));
    component.charger();
    expect(component.failed).toBeFalse();
  });

  it('un échec d’enregistrement ne perd pas la saisie', async () => {
    await setup();
    service.replace.and.returnValue(throwError(() => ({ status: 422 })));

    component.majLibelle(component.entries[0], 'Bloc A');
    component.enregistrer();

    expect(component.saving).toBeFalse();
    expect(component.dirty).toBeTrue();
    expect(component.entries[0].label).toBe('Bloc A');
    expect(snack.open).toHaveBeenCalled();
  });

  it('chaque liste a un nom lisible, et aucun code brut ne reste affiché', async () => {
    await setup();

    const listes = ([
      'nc-category', 'audit-type', 'supplier-type', 'ehs-incident-type', 'apqp-project-type',
      'capa-nature', 'moc-change-type', 'ishikawa-category', 'kpi-frequency',
      'pokayoke-mechanism', 'fives-zone', 'stakeholder-sector'
    ] as const);
    const noms = listes.map(n => component.titre(n));

    // Aucune liste ne reste affichee sous son nom d URL : ce serait du jargon
    // d API dans un ecran de parametrage.
    listes.forEach((n, i) => expect(noms[i]).not.toBe(n));
    expect(new Set(noms).size).toBe(noms.length);
  });
});
