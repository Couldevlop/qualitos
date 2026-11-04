import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../../nc/nc.service';
import { NcSubcategory, NcSubcategoryCatalogue } from '../../../nc/nc.types';
import { NcSettingsComponent } from './nc-settings.component';

/** Un catalogue complet : une nature au moins sous chacune des sept catégories. */
function catalogue(over: Partial<NcSubcategoryCatalogue> = {}): NcSubcategoryCatalogue {
  const entries: NcSubcategory[] = [
    { code: 'PRD-DIM', category: 'PRODUCT', label: 'Cote hors spécification' },
    { code: 'PRD-ASP', category: 'PRODUCT', label: 'Aspect' },
    { code: 'PRC-DER', category: 'PROCESS', label: 'Dérive de paramètre' },
    { code: 'DOC-MAN', category: 'DOCUMENTATION', label: 'Enregistrement manquant' },
    { code: 'FOU-REC', category: 'SUPPLIER', label: 'Réception non conforme' },
    { code: 'SEC-PRQ', category: 'SAFETY', label: 'Presqu’accident' },
    { code: 'ENV-DEC', category: 'ENVIRONMENT', label: 'Déchets' },
    { code: 'AUT-REC', category: 'OTHER', label: 'Réclamation client' }
  ];
  return { entries, defaults: true, ...over };
}

/**
 * « Natures de non-conformité » : lecture, édition d'une catégorie à la fois,
 * règles locales, enregistrement d'un bloc, retour au catalogue de la plateforme.
 */
describe('NcSettingsComponent', () => {
  let fixture: ComponentFixture<NcSettingsComponent>;
  let component: NcSettingsComponent;
  let service: jasmine.SpyObj<NcService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`);

  async function setup(lecture: NcSubcategoryCatalogue | 'erreur' = catalogue()): Promise<void> {
    service = jasmine.createSpyObj<NcService>('NcService',
      ['subcategories', 'replaceSubcategories', 'resetSubcategories']);
    service.subcategories.and.returnValue(
      lecture === 'erreur' ? throwError(() => ({ status: 500 })) : of(lecture));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [NcSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: NcService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function confirmer(reponse: boolean): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(reponse)
    } as MatDialogRef<unknown>);
  }

  it("lit le catalogue, montre une catégorie à la fois, sans rien proposer d'enregistrer", async () => {
    await setup();

    expect(hote().textContent).toContain('les natures de la plateforme');
    expect(component.category).toBe('PRODUCT');
    expect(tous('nature').length).toBe(2);
    expect(un('categorie-PROCESS')?.textContent).toContain('1');

    un('categorie-PROCESS')!.click();
    fixture.detectChanges();

    expect(component.category).toBe('PROCESS');
    expect(tous('nature').length).toBe(1);
    expect(component.dirty).toBeFalse();
    expect(un('enregistrer')).toBeNull();
  });

  it('dit que le catalogue est illisible, et laisse réessayer', async () => {
    await setup('erreur');

    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
  });

  it('met les codes en capitales : le serveur les compare ainsi', async () => {
    await setup();

    component.majCode(component.courantes[0], ' prd-neuf ');

    expect(component.courantes[0].code).toBe('PRD-NEUF');
  });

  it("ajoute une nature dans la catégorie affichée, et la retire", async () => {
    await setup();
    un('categorie-PROCESS')!.click();
    fixture.detectChanges();

    un('ajouter-nature')!.click();
    fixture.detectChanges();

    expect(component.courantes.length).toBe(2);
    expect(component.courantes[1].category).toBe('PROCESS');
    expect(component.dirty).toBeTrue();

    component.retirer(component.courantes[1]);

    expect(component.courantes.length).toBe(1);
  });

  it('refuse un code mal formé, un libellé vide, un doublon', async () => {
    await setup();

    component.ajouter();
    component.majCode(component.courantes[2], 'prd neuf');
    fixture.detectChanges();
    expect(component.problemes.join(' ')).toContain('capitales');

    component.majCode(component.courantes[2], 'PRD-NEUF');
    fixture.detectChanges();
    expect(component.problemes.join(' ')).toContain('libellé');

    component.majLibelle(component.courantes[2], 'Autre chose');
    component.majCode(component.courantes[2], 'PRD-DIM');
    fixture.detectChanges();
    expect(component.problemes.join(' ')).toContain('même code');
    expect((un('enregistrer') as HTMLButtonElement).disabled).toBeTrue();
  });

  it("refuse de laisser une catégorie sans aucune nature : la saisie n'y proposerait rien", async () => {
    await setup();

    component.entries = component.entries.filter(e => e.category !== 'SAFETY');

    expect(component.problemes.join(' ')).toContain('Catégorie sans aucune nature');
  });

  it('refuse un catalogue vide : il faut rétablir celui de la plateforme', async () => {
    await setup();

    component.entries = [];

    expect(component.problemes.join(' ')).toContain('vide');
  });

  it("enregistre le catalogue d'un bloc, rogné, et repart propre", async () => {
    await setup();
    service.replaceSubcategories.and.returnValue(of(catalogue({ defaults: false })));

    component.ajouter();
    component.majCode(component.courantes[2], 'PRD-NEUF');
    component.majLibelle(component.courantes[2], '  Rayure profonde  ');
    fixture.detectChanges();
    un('enregistrer')!.click();

    const envoye = service.replaceSubcategories.calls.mostRecent().args[0];
    expect(envoye.length).toBe(9);
    expect(envoye[envoye.length - 1]).toEqual(
      { code: 'PRD-NEUF', category: 'PRODUCT', label: 'Rayure profonde' });
    expect(component.dirty).toBeFalse();
    expect(component.defaults).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('enregistré');
  });

  it('annule les changements sans rien envoyer', async () => {
    await setup();

    component.ajouter();
    expect(component.dirty).toBeTrue();
    component.annuler();

    expect(component.dirty).toBeFalse();
    expect(component.entries.length).toBe(8);
    expect(service.replaceSubcategories).not.toHaveBeenCalled();
  });

  it('dit pourquoi l’enregistrement a échoué, et garde les changements', async () => {
    await setup();
    service.replaceSubcategories.and.returnValue(throwError(() => ({ status: 400 })));

    component.ajouter();
    component.majCode(component.courantes[2], 'PRD-NEUF');
    component.majLibelle(component.courantes[2], 'Rayure');
    component.enregistrer();

    expect(component.saving).toBeFalse();
    expect(component.dirty).toBeTrue();
    expect(snack.open).toHaveBeenCalled();
  });

  it('rétablit le catalogue de la plateforme après confirmation', async () => {
    await setup(catalogue({ defaults: false }));
    service.resetSubcategories.and.returnValue(of(catalogue()));
    const dialogue = confirmer(true);

    un('retablir')!.click();

    expect(dialogue).toHaveBeenCalled();
    expect(service.resetSubcategories).toHaveBeenCalled();
    expect(component.defaults).toBeTrue();
  });

  it('ne rétablit rien si la confirmation est refusée', async () => {
    await setup(catalogue({ defaults: false }));
    confirmer(false);

    un('retablir')!.click();

    expect(service.resetSubcategories).not.toHaveBeenCalled();
  });

  it("dit pourquoi le rétablissement a échoué", async () => {
    await setup(catalogue({ defaults: false }));
    service.resetSubcategories.and.returnValue(throwError(() => ({ status: 500 })));
    confirmer(true);

    un('retablir')!.click();

    expect(component.saving).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it("ne propose pas de rétablir quand le catalogue est déjà celui de la plateforme", async () => {
    await setup();

    expect((un('retablir') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('borne le catalogue : au-delà, ce serait un champ libre déguisé', async () => {
    await setup();
    for (let i = 0; i < component.limits.entries; i++) {
      component.entries = [...component.entries, { code: 'C' + i, category: 'PRODUCT', label: 'N' + i }];
    }

    const avant = component.entries.length;
    component.ajouter();

    expect(component.entries.length).toBe(avant);
    expect(component.problemes.join(' ')).toContain(String(component.limits.entries));
  });
});
