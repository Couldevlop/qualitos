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
import { summary } from '../../spc-capability.fixtures';
import { SpcCapabilityService } from '../../spc-capability.service';
import { StudySummary } from '../../spc-capability.types';
import { CapabilityRegisterComponent } from './capability-register.component';

/**
 * Le registre : indicateurs sur le verdict du serveur, filtres, tri, plan
 * d'actions depuis la ligne, suppression confirmée, export CSV, et ce qu'un
 * lecteur sans le droit `spc.manage` ne voit pas.
 */
describe('CapabilityRegisterComponent', () => {
  let fixture: ComponentFixture<CapabilityRegisterComponent>;
  let component: CapabilityRegisterComponent;
  let service: jasmine.SpyObj<SpcCapabilityService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: Router;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const tous = (sel: string) => Array.from(hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`));
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const ETUDES: StudySummary[] = [
    summary({ id: 'a', partReference: 'Shaft AX', studyDate: '2026-10-01', cpk: 1.769, status: 'CAPABLE' }),
    summary({ id: 'b', partReference: 'Gear B2', leaderName: 'Marie Curie', machine: 'M-7', studyDate: '2026-10-05',
      cpk: 1.4, cp: 1.5, status: 'WATCH', actionPlan: true }),
    summary({ id: 'c', partReference: 'Pin C3', leaderName: null, machine: null, studyDate: '2026-09-20',
      cpk: 0.9, cp: 1.1, status: 'NOT_CAPABLE', actionPlan: false })
  ];

  async function setup(canManage = true, liste: StudySummary[] | 'erreur' = ETUDES): Promise<void> {
    service = jasmine.createSpyObj<SpcCapabilityService>('SpcCapabilityService', ['list', 'setActionPlan', 'delete']);
    service.list.and.returnValue(liste === 'erreur'
      ? throwError(() => ({ status: 500 }))
      : of(liste.map(s => ({ ...s }))));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    await TestBed.configureTestingModule({
      declarations: [CapabilityRegisterComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: SpcCapabilityService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: AuthzService, useValue: { can: (code: string) => of(canManage && code === 'spc.manage') } }
      ]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(CapabilityRegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('calcule les indicateurs sur le verdict rendu par le serveur', async () => {
    await setup();
    expect(un('kpi-total')?.textContent).toContain('3');
    expect(un('kpi-maitrises')?.querySelector('b')?.textContent?.trim()).toBe('1');
    expect(un('kpi-plans')?.textContent).toContain('1');
    expect(un('kpi-surveiller')?.textContent).toContain('1');
    expect(un('kpi-surveiller')?.textContent).toContain('1.33 – 1.67');
    expect(un('kpi-sans-plan')?.textContent).toContain('1');
    expect(un('kpi-sans-plan')?.classList).toContain('kpi--alerte');
  });

  it('affiche les indicateurs d\'un registre vide', async () => {
    await setup(true, []);
    expect(component.controlledCount).toBe(0);
    expect(un('kpi-maitrises')?.querySelector('b')?.textContent?.trim()).toBe('0');
    expect(component.watchRange).toBe('');
    expect(hote().textContent).toContain('Aucune étude pour le moment');
  });

  it('trie par date décroissante, colore le Cpk et signale les études sans décision', async () => {
    await setup();
    const lignes = tous('ligne-etude');
    expect(lignes.map(l => l.querySelector('.piece')?.textContent?.trim())).toEqual(['Gear B2', 'Shaft AX', 'Pin C3']);
    expect(tous('cpk').map(c => c.className)).toEqual([
      jasmine.stringContaining('pastille--warn'), jasmine.stringContaining('pastille--ok'),
      jasmine.stringContaining('pastille--bad')
    ]);
    expect(tous('statut').map(s => s.textContent?.trim())).toEqual(['À surveiller', 'Maîtrisé', 'Non maîtrisé']);
    expect(tous('statut').map(s => s.className)).toEqual([
      jasmine.stringContaining('statut--warn'), jasmine.stringContaining('statut--ok'),
      jasmine.stringContaining('statut--bad')
    ]);
    expect(lignes[2].classList).toContain('ligne--alerte');
    expect(lignes[0].classList).not.toContain('ligne--alerte');
  });

  it('trie par colonne, et inverse au second clic', async () => {
    await setup();
    un('tri-cpk')!.click();
    fixture.detectChanges();
    expect(component.rows.map(r => r.id)).toEqual(['c', 'b', 'a']);
    expect(component.ariaSort('cpk')).toBe('ascending');
    un('tri-cpk')!.click();
    fixture.detectChanges();
    expect(component.rows.map(r => r.id)).toEqual(['a', 'b', 'c']);
    expect(component.ariaSort('cpk')).toBe('descending');
    expect(component.ariaSort('studyDate')).toBe('none');
    component.sortBy('actionPlan');
    expect(component.rows[2].id).toBe('b');
    component.sortBy('leaderName');
    expect(component.rows[0].id).toBe('c');
  });

  it('filtre par recherche, statut et plan d\'actions, puis efface les filtres', async () => {
    await setup();
    component.search = 'curie';
    expect(component.rows.map(r => r.id)).toEqual(['b']);
    component.search = 'm-7';
    expect(component.rows.map(r => r.id)).toEqual(['b']);
    component.search = '';
    component.statusFilter = 'CAPABLE';
    expect(component.rows.map(r => r.id)).toEqual(['a']);
    component.statusFilter = 'WATCH';
    expect(component.rows.map(r => r.id)).toEqual(['b']);
    component.statusFilter = 'NOT_CAPABLE';
    expect(component.rows.map(r => r.id)).toEqual(['c']);
    component.planFilter = 'yes';
    fixture.detectChanges();
    expect(hote().textContent).toContain('Aucune étude ne correspond aux filtres.');
    component.resetFilters();
    expect(component.rows.length).toBe(3);
  });

  it('ouvre une étude et en crée une nouvelle', async () => {
    await setup();
    tous('ligne-etude')[1].click();
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'cartes', 'a']);
    un('nouvelle')!.click();
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'cartes', 'nouvelle']);
  });

  it('enregistre le plan d\'actions depuis la ligne', async () => {
    await setup();
    const c = component.studies.find(s => s.id === 'c')!;
    service.setActionPlan.and.returnValue(of(summary({ ...c, actionPlan: true })));
    component.setPlan(c, true);
    expect(service.setActionPlan).toHaveBeenCalledWith('c', true);
    expect(component.studies.find(s => s.id === 'c')!.actionPlan).toBeTrue();
    expect(component.uncontrolledWithoutPlan).toBe(0);
  });

  it('revient à l\'état du serveur quand le plan d\'actions est refusé', async () => {
    await setup();
    const c = component.studies.find(s => s.id === 'c')!;
    service.setActionPlan.and.returnValue(throwError(() => ({ status: 403 })));
    component.setPlan(c, true);
    expect(c.actionPlan).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Vous n\'avez pas les droits pour cette action.', undefined, { duration: 5000 });
  });

  it('supprime une étude après confirmation, et pas sans', async () => {
    await setup();
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    component.remove(component.studies[0]);
    expect(service.delete).not.toHaveBeenCalled();

    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    service.delete.and.returnValue(of(undefined));
    component.remove(component.studies[0]);
    expect(service.delete).toHaveBeenCalledWith('a');
    expect(component.studies.map(s => s.id)).toEqual(['b', 'c']);
  });

  it('dit quand la suppression échoue', async () => {
    await setup();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    service.delete.and.returnValue(throwError(() => ({ status: 0 })));
    component.remove(component.studies[0]);
    expect(component.studies.length).toBe(3);
    expect(snack.open).toHaveBeenCalled();
  });

  it('exporte les lignes affichées, en CSV neutralisé', async () => {
    await setup();
    let blob: Blob | undefined;
    spyOn(URL, 'createObjectURL').and.callFake((b: Blob | MediaSource) => {
      blob = b as Blob;
      return 'blob:x';
    });
    spyOn(URL, 'revokeObjectURL');
    const clic = spyOn(HTMLAnchorElement.prototype, 'click');
    component.statusFilter = 'CAPABLE';
    component.studies[0].partReference = '=CMD()';
    component.exporter();
    expect(clic).toHaveBeenCalled();
    const texte = await blob!.text();
    const lignes = texte.replace('\uFEFF', '').split('\r\n');
    expect(lignes.length).toBe(2);
    expect(lignes[0]).toContain('"Référence pièce"');
    expect(lignes[1]).toContain(`"'=CMD()"`);
    expect(lignes[1]).toContain('"Maîtrisé"');
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:x');
  });

  it('dit quand le registre ne se charge pas, et réessaie', async () => {
    await setup(true, 'erreur');
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')?.textContent).toContain('Le registre des études n\'a pas pu être chargé.');
    service.list.and.returnValue(of(ETUDES));
    component.charger();
    expect(component.failed).toBeFalse();
    expect(component.studies.length).toBe(3);
  });

  it('ne montre ni création, ni suppression, ni sélection de plan sans le droit spc.manage', async () => {
    await setup(false);
    expect(un('nouvelle')).toBeNull();
    expect(tous('supprimer').length).toBe(0);
    expect(tous('plan').length).toBe(0);
    expect(tous('ouvrir').length).toBe(3);
    expect(un('exporter')).not.toBeNull();
  });
});
