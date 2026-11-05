import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StakeholdersService } from '../../stakeholders.service';
import { ImpactLevel, StakeholderRow, StakeholderStatistics } from '../../stakeholders.types';
import { StakeholdersPageComponent, csvCell } from './stakeholders-page.component';

/**
 * L'écran du registre : ce qu'il charge, et l'export qu'il produit.
 *
 * <p>L'export est testé pour ce qu'il garantit — le contenu lisible, et la
 * neutralisation des formules. Un registre dont les noms viennent de tiers ne
 * peut pas les recopier tels quels dans un fichier qu'un tableur ouvrira.
 */
describe('StakeholdersPageComponent', () => {

  function row(id: string, name: string, quality: ImpactLevel = 'HIGH'): StakeholderRow {
    return {
      id, name, category: 'CUSTOMERS', side: 'EXTERNAL',
      qualityImpact: quality, regulatoryImpact: 'LOW', criticality: quality,
      relevance: 'RELEVANT', lastReviewedOn: '2026-10-01', nextReviewOn: '2027-04-01',
      reviewOverdue: false, requirementCount: 3
    };
  }

  const STATS: StakeholderStatistics = {
    total: 2, internal: 0, external: 2, critical: 1, overdue: 0,
    toDetermine: 0, requirementsToReassess: 0
  };

  let fixture: ComponentFixture<StakeholdersPageComponent>;
  let component: StakeholdersPageComponent;
  let svc: jasmine.SpyObj<StakeholdersService>;

  async function build(rows: StakeholderRow[]): Promise<void> {
    svc = jasmine.createSpyObj<StakeholdersService>('StakeholdersService',
      ['list', 'statistics', 'get']);
    svc.list.and.returnValue(of(rows));
    svc.statistics.and.returnValue(of(STATS));

    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(of(true));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholdersPageComponent],
      imports: [SharedModule, UiModule, MatSidenavModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: StakeholdersService, useValue: svc },
        { provide: AuthzService, useValue: authz },
        {
          provide: MatDialog,
          useValue: jasmine.createSpyObj<MatDialog>('MatDialog', ['open'])
        },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) }
      ],
      // Les vues filles ont leurs propres specs : ici on éprouve l'écran hôte.
      errorOnUnknownElements: false,
      errorOnUnknownProperties: false
    }).compileComponents();

    fixture = TestBed.createComponent(StakeholdersPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('charge le registre et ses statistiques', async () => {
    await build([row('a', 'Client A'), row('b', 'Client B')]);

    expect(component.rows.length).toBe(2);
    expect(component.statistics).toEqual(STATS);
    expect(component.loading).toBeFalse();
  });

  it('exporte les lignes affichées, en-tête compris', async () => {
    await build([row('a', 'Aeronef Systemes')]);
    const blobs: Blob[] = [];
    spyOn(URL, 'createObjectURL').and.callFake((b: Blob | MediaSource) => {
      blobs.push(b as Blob);
      return 'blob:x';
    });
    spyOn(URL, 'revokeObjectURL');
    // Le clic réel déclencherait un téléchargement dans le navigateur de test.
    const lien = document.createElement('a');
    spyOn(lien, 'click');
    spyOn(document, 'createElement').and.returnValue(lien);

    component.exportRegister();

    expect(lien.click).toHaveBeenCalled();
    expect(lien.download).toMatch(/^qualitos-parties-prenantes-\d{4}-\d{2}-\d{2}\.csv$/);
    expect(blobs.length).toBe(1);
    // `Blob.text()` RETIRE la marque d'ordre des octets en decodant : on la
    // verifie donc sur les octets, qui sont ce qu'Excel lira.
    const octets = new Uint8Array(await blobs[0].arrayBuffer());
    expect([octets[0], octets[1], octets[2]]).toEqual([0xef, 0xbb, 0xbf]);
    const texte = await blobs[0].text();
    expect(texte).toContain('"Aeronef Systemes"');
    // Une ligne d'en-tête et une ligne de données.
    expect(texte.trim().split('\r\n').length).toBe(2);
  });

  it("n'exporte rien quand le registre est vide", async () => {
    await build([]);
    spyOn(URL, 'createObjectURL');

    const bouton = fixture.nativeElement.querySelector('[data-test="exporter"]');

    expect(bouton.disabled).toBeTrue();
  });

  describe('csvCell', () => {

    it('entoure la valeur de guillemets et double ceux qu elle contient', () => {
      expect(csvCell('Client A')).toBe('"Client A"');
      expect(csvCell('Client "A"')).toBe('"Client ""A"""');
    });

    it('désamorce ce qu un tableur lirait comme une formule', () => {
      // Sans le guillemet d'ouverture, Excel exécuterait la cellule.
      expect(csvCell('=1+1')).toBe('"\'=1+1"');
      expect(csvCell('+33 1 23 45')).toBe('"\'+33 1 23 45"');
      expect(csvCell('-5')).toBe('"\'-5"');
      expect(csvCell('@SUM(A1)')).toBe('"\'@SUM(A1)"');
      expect(csvCell('\tonglet')).toBe('"\'\tonglet"');
    });

    it('rend une chaîne vide pour une valeur absente', () => {
      expect(csvCell(null as unknown as string)).toBe('""');
      expect(csvCell(undefined as unknown as string)).toBe('""');
    });
  });
});
