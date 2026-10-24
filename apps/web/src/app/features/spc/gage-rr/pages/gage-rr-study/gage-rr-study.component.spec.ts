import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../../core/directory/directory.types';
import { SharedModule } from '../../../../../shared/shared.module';
import { UiModule } from '../../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../../shared/user-picker/user-picker.module';
import { GOLDEN, GOLDEN_GRID, OPERATORS, goldenCalculation, sheet } from '../../gage-rr.fixtures';
import { GageRrService } from '../../gage-rr.service';
import { GageRrStudyComponent, stepThree } from './gage-rr-study.component';

/**
 * La fiche d'étude R&R : grille comme l'onglet « Données », calcul en direct
 * (test doré du classeur), étapes 1 à 4, ANOVA, graphiques, guide, jeu
 * d'exemple, choix des personnes dans l'annuaire, enregistrement, refus du
 * serveur dits sans reprendre son texte, consultation seule.
 */
describe('GageRrStudyComponent', () => {
  let fixture: ComponentFixture<GageRrStudyComponent>;
  let component: GageRrStudyComponent;
  let service: jasmine.SpyObj<GageRrService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let confirme: boolean;
  let router: Router;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const personne = (accountId: string, displayName: string): DirectoryUser =>
    ({ id: `app-${accountId}`, accountId, displayName, email: `${accountId}@acme.test`, active: true });
  const PERSONNES = [...OPERATORS.map(o => personne(o.accountId, o.name)), personne('resp-1', 'Jeanne Leroy')];

  async function setup(opts: { id?: string; canManage?: boolean; get?: 'erreur' } = {}): Promise<void> {
    service = jasmine.createSpyObj<GageRrService>('GageRrService', ['get', 'create', 'update']);
    service.get.and.returnValue(opts.get === 'erreur' ? throwError(() => ({ status: 404 })) : of(sheet()));
    service.create.and.returnValue(of(sheet({ id: 'nouveau' })));
    service.update.and.returnValue(of(sheet({ partReference: 'Arbre AX-211' })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    confirme = true;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake((() => ({ afterClosed: () => of(confirme) })) as never);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: PERSONNES, page: 0, size: 20, totalElements: 4, totalPages: 1 }));
    directory.getByAccount.and.callFake((id: string) => of(PERSONNES.find(p => p.accountId === id) ?? PERSONNES[0]));
    await TestBed.configureTestingModule({
      declarations: [GageRrStudyComponent],
      imports: [SharedModule, UiModule, FormsModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: GageRrService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: DirectoryService, useValue: directory },
        { provide: AuthzService, useValue: { can: () => of(opts.canManage ?? true) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(opts.id ? { id: opts.id } : {}) } } }
      ]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(GageRrStudyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function choisirOperateurs(): void {
    component.form.patchValue({ op0: 'op-a', op1: 'op-b', op2: 'op-c' });
    PERSONNES.slice(0, 3).forEach((p, o) => component.onOperator(o, p));
  }

  function chargerExemple(): void {
    un('exemple')!.click();
    fixture.detectChanges();
  }

  it('ouvre une étude nouvelle sur le plan standard, en attente des mesures', async () => {
    await setup();
    expect(component.title).toBe('Nouvelle étude R&R');
    expect(service.get).not.toHaveBeenCalled();
    expect([component.k, component.n, component.r, component.multiplier]).toEqual([3, 10, 3, 5.15]);
    expect(component.calc).toBeNull();
    expect(un('attente')?.textContent).toContain('90');
    expect(un('synthese')).toBeNull();
    expect(hote().querySelectorAll('[data-test^="cellule-"]').length).toBe(90);
    expect(un('entete-operateur-1')?.textContent).toContain('Opérateur B');
  });

  it('charge l\'exemple du classeur dans une grille vide et rend le calcul doré', async () => {
    await setup();
    chargerExemple();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.form.controls.partReference.value).toBe('Arbre AX-210');
    expect(component.tolerance).toBeCloseTo(0.2, 10);
    expect(component.calc!.grr.value).toBeCloseTo(GOLDEN.grr, 6);
    expect(un('kpi-grr')?.textContent).toContain('21.0 %');
    expect(un('kpi-grr')?.classList).toContain('kpi--warn');
    expect(un('kpi-tolerance')?.textContent).toContain('24.7 %');
    expect(un('kpi-ndc')?.textContent).toContain('6');
    expect(un('kpi-ndc')?.classList).toContain('kpi--ok');
    expect(un('kpi-anova')?.textContent).toContain('21.2 %');
    expect(un('decision')?.textContent).toContain('Acceptable sous conditions');
    expect(un('xbar-0')?.textContent).toContain('10.0102');
    expect(un('rbar-1')?.textContent).toContain('0.0154');
    expect(un('moyenne-generale')?.textContent).toContain('10.0122');
    expect(un('moyenne-0-0')?.textContent).toContain('9.9487');
    expect(un('etendue-0-0')?.textContent).toContain('0.0150');
    expect(un('moyenne-piece-0')?.textContent).toContain('9.9446');
    expect(un('xdiff')?.textContent).toContain('0.0104');
    expect(un('ucl')?.textContent).toContain('0.0350');
    expect(un('controle-etendues')?.textContent).toContain('aucune étendue hors limite');
    expect(un('k1')?.textContent).toContain('3.050');
    expect(un('ligne-grr')?.textContent).toContain('√(0.0414² + 0.0271²)');
    expect(un('grr-total')?.textContent).toContain('21.00 %');
    expect(un('grr-tolerance')?.textContent).toContain('24.74 %');
    expect(un('ndc')?.textContent).toContain('6');
    expect(un('decision-ndc')?.textContent).toContain('Discrimination suffisante');
    expect(un('interpretation')?.querySelectorAll('li').length).toBe(5);
    expect(un('regroupement')?.textContent).toContain('regroupée');
    expect(un('anova-INTERACTION')?.textContent).toContain('0.3584');
    expect(un('anova-regroupee')).not.toBeNull();
    expect(un('anova-composantes')?.querySelectorAll('tbody tr').length).toBe(7);
    expect(un('comparaison')?.textContent).toContain('21.16 %');
    expect(component.componentsChart).not.toBeNull();
    expect(component.rangeChart).not.toBeNull();
    expect(component.meanChart).not.toBeNull();
    expect(component.partChart).not.toBeNull();
    expect(hote().querySelectorAll('qos-echart').length).toBe(4);
  });

  it('demande confirmation avant de remplacer des mesures saisies', async () => {
    await setup();
    component.onCell(0, 0, 0, { target: { value: '9.5' } } as unknown as Event);
    confirme = false;
    chargerExemple();
    expect(dialog.open).toHaveBeenCalled();
    expect(component.cell(0, 0, 0)).toBe('9.5');
    confirme = true;
    chargerExemple();
    expect(component.cell(0, 0, 0)).toBe('9.955');
  });

  it('vide la grille', async () => {
    await setup();
    chargerExemple();
    un('vider')!.click();
    fixture.detectChanges();
    expect(component.hasMeasurements).toBeFalse();
    expect(component.calc).toBeNull();
  });

  it('suit la saisie cellule par cellule et signale une cellule illisible', async () => {
    await setup();
    const cellule = un('cellule-0-0-0') as HTMLInputElement;
    cellule.value = '4a9';
    cellule.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.cellInvalid(0, 0, 0)).toBeTrue();
    expect(cellule.classList).toContain('cellule--invalide');
    expect(component.cellInvalid(0, 0, 1)).toBeFalse();
    expect(component.rowMean(0, 0)).toBeNull();
    expect(component.partMean(0)).toBeNull();
    expect(component.cellLabel(1, 2, 0)).toBe('Opérateur B, pièce 3, essai 1');
  });

  it('colle un bloc de tableur, mais laisse passer une valeur seule', async () => {
    await setup();
    const seule = new ClipboardEvent('paste', { clipboardData: new DataTransfer(), cancelable: true });
    seule.clipboardData!.setData('text', '10.01');
    component.onPaste(0, 0, 0, seule);
    expect(seule.defaultPrevented).toBeFalse();
    expect(component.cell(0, 0, 0)).toBe('');

    const tsv = GOLDEN_GRID[0].map((_, p) => GOLDEN_GRID.flatMap(b => b[p]).join('\t')).join('\r\n');
    const bloc = new ClipboardEvent('paste', { clipboardData: new DataTransfer(), cancelable: true });
    bloc.clipboardData!.setData('text', tsv);
    component.onPaste(0, 0, 0, bloc);
    expect(bloc.defaultPrevented).toBeTrue();
    expect(component.cell(2, 9, 2)).toBe('10.077');
    expect(component.calc!.xDiff).toBeCloseTo(GOLDEN.xDiff, 6);
  });

  it('change le plan sans perdre les mesures masquées', async () => {
    await setup();
    chargerExemple();
    component.form.patchValue({ operatorCount: 2, partCount: 5, trialCount: 2 });
    fixture.detectChanges();
    expect(hote().querySelectorAll('[data-test^="cellule-"]').length).toBe(20);
    expect(component.calc!.k2).toBeCloseTo(3.65, 10);
    expect(component.calc!.k3).toBeCloseTo(2.08, 10);
    expect(component.calc!.d4).toBe(3.27);
    expect(component.excludedFor(0)).toEqual([]);
    component.form.patchValue({ operatorCount: 3, partCount: 10, trialCount: 3, sigmaMultiplier: 6 });
    expect(component.calc!.grr.percentTolerance!).toBeCloseTo(0.28825, 4);
  });

  it('signale une étendue au-dessus de UCLR', async () => {
    await setup();
    chargerExemple();
    component.raw[0][0] = ['9.9', '10.2', '10'];
    component.form.patchValue({});
    fixture.detectChanges();
    expect(component.rangeOut(0, 0)).toBeTrue();
    expect(un('etendue-0-0')?.classList).toContain('hors');
    expect(un('controle-etendues')?.textContent).toContain('1 étendue(s)');
  });

  it('dit qu\'il n\'y a rien à décomposer quand toutes les mesures sont égales', async () => {
    await setup();
    component.form.patchValue({ operatorCount: 2, partCount: 2, trialCount: 2 });
    component.raw = component.raw.map(b => b.map(e => e.map(() => '1')));
    component.form.patchValue({});
    fixture.detectChanges();
    expect(un('sans-dispersion')).not.toBeNull();
    expect(un('synthese')).toBeNull();
    expect(component.componentsChart).toBeNull();
  });

  it('signale des limites inversées et laisse le % de tolérance vide', async () => {
    await setup();
    chargerExemple();
    component.form.patchValue({ lsl: 10.1, usl: 9.9 });
    fixture.detectChanges();
    expect(component.limitsInvalid).toBeTrue();
    expect(un('limites-invalides')).not.toBeNull();
    expect(component.calc!.grr.percentTolerance).toBeNull();
    component.save();
    expect(service.create).not.toHaveBeenCalled();
  });

  it('exige chaque opérateur, choisi dans l\'annuaire et sans doublon', async () => {
    await setup();
    chargerExemple();
    component.save();
    expect(component.fieldError?.field).toBe('operators');
    choisirOperateurs();
    component.form.patchValue({ op2: 'op-a' });
    component.save();
    expect(component.fieldError?.field).toBe('operators');
    expect(component.excludedFor(1)).toEqual(['op-a', 'op-a']);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('exige une grille complète avant l\'envoi', async () => {
    await setup();
    chargerExemple();
    choisirOperateurs();
    component.raw[1][4][2] = '';
    component.form.patchValue({});
    component.save();
    expect(component.fieldError?.field).toBe('measurements');
    expect(service.create).not.toHaveBeenCalled();
  });

  it('ne part pas sans référence de pièce', async () => {
    await setup();
    chargerExemple();
    choisirOperateurs();
    component.form.patchValue({ partReference: '' });
    component.save();
    expect(component.form.controls.partReference.touched).toBeTrue();
    expect(service.create).not.toHaveBeenCalled();
  });

  it('enregistre une étude nouvelle, personnes comprises, puis rouvre sa fiche', async () => {
    await setup();
    chargerExemple();
    choisirOperateurs();
    component.form.patchValue({ responsibleUserId: 'resp-1' });
    component.onResponsible(PERSONNES[3]);
    component.form.patchValue({ instrument: '  ' });
    fixture.detectChanges();
    expect(un('entete-operateur-0')?.textContent).toContain('Alice Martin');
    un('enregistrer')!.click();
    const req = service.create.calls.mostRecent().args[0];
    expect(req.partReference).toBe('Arbre AX-210');
    expect(req.instrument).toBeNull();
    expect(req.operators).toEqual(OPERATORS);
    expect(req.responsibleUserId).toBe('resp-1');
    expect(req.responsibleName).toBe('Jeanne Leroy');
    expect([req.operatorCount, req.partCount, req.trialCount, req.sigmaMultiplier]).toEqual([3, 10, 3, 5.15]);
    expect(req.measurements).toEqual(GOLDEN_GRID);
    expect(snack.open).toHaveBeenCalledWith('Étude R&R enregistrée.', undefined, { duration: 3000 });
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'gage-rr', 'nouveau'], { replaceUrl: true });
  });

  it('n\'envoie pas le nom d\'un responsable qu\'on a retiré', async () => {
    await setup();
    chargerExemple();
    choisirOperateurs();
    component.onResponsible(PERSONNES[3]);
    component.onResponsible(null);
    component.save();
    expect(service.create.calls.mostRecent().args[0].responsibleName).toBeNull();
  });

  it('dit le champ refusé par le serveur sans reprendre son texte', async () => {
    await setup();
    chargerExemple();
    choisirOperateurs();
    service.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 422, error: { field: 'operators', message: '<script>' } })));
    component.save();
    fixture.detectChanges();
    expect(component.saving).toBeFalse();
    expect(un('erreur-champ')?.textContent).toContain('Choisissez chaque opérateur');
    expect(un('erreur-champ')?.textContent).not.toContain('<script>');
  });

  it('dit l\'échec d\'un enregistrement', async () => {
    await setup();
    chargerExemple();
    choisirOperateurs();
    service.create.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.save();
    expect(component.fieldError).toBeNull();
    expect(snack.open).toHaveBeenCalled();
  });

  it('rouvre une étude enregistrée et la révise', async () => {
    await setup({ id: 'g1' });
    fixture.detectChanges();
    expect(service.get).toHaveBeenCalledWith('g1');
    expect(component.title).toBe('Étude R&R Arbre AX-210');
    expect(component.form.controls.op1.value).toBe('op-b');
    expect(component.operatorName(2)).toBe('Chloé Durand');
    expect(component.responsibleName).toBe('Jeanne Leroy');
    expect(component.calc!.ndc).toBe(6);
    expect(component.form.pristine).toBeTrue();
    component.save();
    expect(service.update).toHaveBeenCalledWith('g1', jasmine.objectContaining({ partReference: 'Arbre AX-210' }));
    expect(router.navigate).not.toHaveBeenCalled();
    expect(component.form.controls.partReference.value).toBe('Arbre AX-211');
  });

  it('dit une étude introuvable et propose de réessayer', async () => {
    await setup({ id: 'g1', get: 'erreur' });
    expect(component.failed).toBeTrue();
    expect(snack.open).toHaveBeenCalled();
    expect(un('enregistrer')?.hasAttribute('disabled')).toBeTrue();
    service.get.and.returnValue(of(sheet()));
    component.charger();
    expect(component.failed).toBeFalse();
  });

  it('ouvre et ferme le guide', async () => {
    await setup();
    expect(un('guide')).toBeNull();
    un('guide-bascule')!.click();
    fixture.detectChanges();
    expect(un('guide')?.textContent).toContain('AIAG');
    expect(un('guide-bascule')?.getAttribute('aria-expanded')).toBe('true');
    component.toggleGuide();
    fixture.detectChanges();
    expect(un('guide')).toBeNull();
  });

  it('revient au registre', async () => {
    await setup();
    un('retour')!.click();
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'gage-rr']);
  });

  it('se consulte seulement sans le droit spc.manage', async () => {
    await setup({ id: 'g1', canManage: false });
    fixture.detectChanges();
    expect(un('lecture-seule')).not.toBeNull();
    expect(un('enregistrer')).toBeNull();
    expect(un('exemple')).toBeNull();
    expect(component.form.disabled).toBeTrue();
    expect((un('cellule-0-0-0') as HTMLInputElement).readOnly).toBeTrue();
    component.loadExample();
    component.save();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(service.update).not.toHaveBeenCalled();
  });

  it('écrit l\'application numérique de l\'étape 3', () => {
    const lignes = stepThree(goldenCalculation(), 10, 3);
    expect(lignes.map(l => l.key)).toEqual(['ev', 'av', 'grr', 'pv', 'tv']);
    expect(lignes[0].application).toBe('0.0136 × 3.050');
    expect(lignes[1].application).toBe('√[(0.0104 × 2.700)² − 0.0414² / (10 × 3)]');
    expect(lignes[3].application).toBe('0.1422 × 1.620');
    expect(lignes[4].component.value).toBeCloseTo(GOLDEN.tv, 6);
  });
});
