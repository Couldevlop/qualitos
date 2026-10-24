import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { SharedModule } from '../../../../../shared/shared.module';
import { UiModule } from '../../../../../shared/ui/ui.module';
import { summary } from '../../gage-rr.fixtures';
import { GageRrService } from '../../gage-rr.service';
import { GageRrSummary } from '../../gage-rr.types';
import { GageRrRegisterComponent } from './gage-rr-register.component';

/**
 * Le registre R&R : indicateurs sur le verdict du serveur, filtres, tri,
 * suppression confirmée, export CSV protégé, et ce qu'un lecteur sans le droit
 * `spc.manage` ne voit pas.
 */
describe('GageRrRegisterComponent', () => {
  let fixture: ComponentFixture<GageRrRegisterComponent>;
  let component: GageRrRegisterComponent;
  let service: jasmine.SpyObj<GageRrService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let confirme: boolean;
  let router: Router;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const tous = (sel: string) => Array.from(hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`));
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const ETUDES: GageRrSummary[] = [
    summary({ id: 'a', partReference: 'Arbre AX-210', studyDate: '2026-10-19', grrPercent: 21, decision: 'CONDITIONAL' }),
    summary({ id: 'b', partReference: 'Bague B2', instrument: 'Pied à coulisse', studyDate: '2026-10-05', grrPercent: 8.4,
      grrTolerancePercent: 9.1, ndc: 12, decision: 'ACCEPTABLE', responsibleName: 'Marie Curie' }),
    summary({ id: 'c', partReference: 'Carter C3', instrument: null, characteristic: null, studyDate: '2026-09-20',
      grrPercent: 42.5, grrTolerancePercent: 51, ndc: 2, decision: 'NOT_ACCEPTABLE', responsibleName: null,
      operators: [] })
  ];

  async function setup(canManage = true, liste: GageRrSummary[] | 'erreur' = ETUDES): Promise<void> {
    service = jasmine.createSpyObj<GageRrService>('GageRrService', ['list', 'delete']);
    service.list.and.returnValue(liste === 'erreur'
      ? throwError(() => ({ status: 500 }))
      : of(liste.map(s => ({ ...s }))));
    service.delete.and.returnValue(of(undefined));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    confirme = true;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake((() => ({ afterClosed: () => of(confirme) })) as never);
    await TestBed.configureTestingModule({
      declarations: [GageRrRegisterComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: GageRrService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: AuthzService, useValue: { can: (code: string) => of(canManage && code === 'spc.manage') } }
      ]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(GageRrRegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('calcule les indicateurs sur le verdict rendu par le serveur', async () => {
    await setup();
    expect(un('kpi-total')?.textContent).toContain('3');
    expect(un('kpi-acceptables')?.textContent).toContain('1');
    expect(un('kpi-conditions')?.textContent).toContain('1');
    expect(un('kpi-non-acceptables')?.textContent).toContain('1');
    expect(un('kpi-ndc')?.textContent).toContain('1');
    expect(un('kpi-ndc')?.classList).toContain('kpi--alerte');
  });

  it('affiche une ligne par étude, triée de la plus récente à la plus ancienne', async () => {
    await setup();
    expect(tous('ligne-etude').length).toBe(3);
    expect(component.rows.map(r => r.id)).toEqual(['a', 'b', 'c']);
    expect(tous('grr')[0].textContent).toContain('21.0 %');
    expect(tous('grr')[0].classList).toContain('pastille--warn');
    expect(tous('decision')[2].textContent).toContain('Non acceptable');
    expect(tous('decision')[2].classList).toContain('statut--bad');
    expect(tous('ndc')[2].classList).toContain('faible');
    expect(tous('ligne-etude')[2].classList).toContain('ligne--alerte');
    expect(hote().textContent).toContain('Alice Martin, Bruno Petit, Chloé Durand');
    expect(hote().textContent).toContain('3 × 10 × 3');
  });

  it('filtre par texte, opérateur et décision, puis efface les filtres', async () => {
    await setup();
    component.search = 'coulisse';
    expect(component.rows.map(r => r.id)).toEqual(['b']);
    component.search = 'chloé';
    expect(component.rows.map(r => r.id)).toEqual(['a', 'b']);
    component.search = '';
    component.decisionFilter = 'NOT_ACCEPTABLE';
    expect(component.rows.map(r => r.id)).toEqual(['c']);
    component.search = 'introuvable';
    fixture.detectChanges();
    expect(hote().textContent).toContain('Aucune étude ne correspond aux filtres.');
    component.resetFilters();
    expect(component.rows.length).toBe(3);
  });

  it('trie par colonne, nombre, texte ou gravité du verdict', async () => {
    await setup();
    un('tri-grr')!.click();
    fixture.detectChanges();
    expect(component.rows.map(r => r.id)).toEqual(['b', 'a', 'c']);
    expect(component.ariaSort('grrPercent')).toBe('ascending');
    un('tri-grr')!.click();
    expect(component.rows.map(r => r.id)).toEqual(['c', 'a', 'b']);
    expect(component.ariaSort('grrPercent')).toBe('descending');
    expect(component.ariaSort('ndc')).toBe('none');
    un('tri-decision')!.click();
    expect(component.rows.map(r => r.id)).toEqual(['b', 'a', 'c']);
    component.sortBy('instrument');
    expect(component.rows.map(r => r.id)).toEqual(['c', 'a', 'b']);
  });

  it('ouvre une étude, en crée une', async () => {
    await setup();
    tous('ligne-etude')[1].click();
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'gage-rr', 'b']);
    un('nouvelle')!.click();
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'gage-rr', 'nouvelle']);
  });

  it('supprime une étude après confirmation, et pas sans', async () => {
    await setup();
    confirme = false;
    tous('supprimer')[0].click();
    expect(service.delete).not.toHaveBeenCalled();
    confirme = true;
    tous('supprimer')[0].click();
    expect(service.delete).toHaveBeenCalledWith('a');
    expect(component.studies.map(s => s.id)).toEqual(['b', 'c']);
  });

  it('dit l\'échec d\'une suppression', async () => {
    await setup();
    service.delete.and.returnValue(throwError(() => ({ status: 500 })));
    component.remove(component.studies[0]);
    expect(snack.open).toHaveBeenCalled();
    expect(component.studies.length).toBe(3);
  });

  it('dit l\'échec du chargement et propose de réessayer', async () => {
    await setup(true, 'erreur');
    expect(component.failed).toBeTrue();
    expect(hote().textContent).toContain('Le registre des études R&R n\'a pas pu être chargé.');
    expect(snack.open).toHaveBeenCalled();
    service.list.and.returnValue(of([]));
    component.charger();
    fixture.detectChanges();
    expect(component.failed).toBeFalse();
    expect(hote().textContent).toContain('Aucune étude R&R pour le moment.');
  });

  it('exporte les lignes affichées en CSV, cellules protégées contre l\'injection', async () => {
    await setup();
    let blob: Blob | undefined;
    spyOn(URL, 'createObjectURL').and.callFake((b: Blob | MediaSource) => {
      blob = b as Blob;
      return 'blob:x';
    });
    spyOn(URL, 'revokeObjectURL');
    const clic = spyOn(HTMLAnchorElement.prototype, 'click');
    component.decisionFilter = 'CONDITIONAL';
    component.studies[0].partReference = '=CMD()';
    component.exporter();
    expect(clic).toHaveBeenCalled();
    const lignes = (await blob!.text()).replace('﻿', '').split('\r\n');
    expect(lignes.length).toBe(2);
    expect(lignes[0]).toContain('"%GRR ANOVA"');
    expect(lignes[1]).toContain('"\'=CMD()"');
    expect(lignes[1]).toContain('"Alice Martin, Bruno Petit, Chloé Durand"');
    expect(lignes[1]).toContain('"Acceptable sous conditions"');
    expect(lignes[1]).toContain('"21.16"');
  });

  it('cache la création et la suppression sans le droit spc.manage', async () => {
    await setup(false);
    expect(un('nouvelle')).toBeNull();
    expect(tous('supprimer').length).toBe(0);
    expect(un('exporter')).not.toBeNull();
  });
});
