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
import { ControlChartComponent } from '../../components/control-chart/control-chart.component';
import { DistributionChartComponent } from '../../components/distribution-chart/distribution-chart.component';
import { TURNING_EXAMPLE, WORKBOOK_EXAMPLE } from '../../spc-capability.examples';
import { GOLDEN, GOLDEN_GRID, sheet } from '../../spc-capability.fixtures';
import { SpcCapabilityService } from '../../spc-capability.service';
import { CapabilityStudyComponent, fieldMessage } from './capability-study.component';

/**
 * La fiche d'étude : saisie et collage de la grille, calcul en direct (test
 * doré), verdict au seuil du client ou aux seuils figés de l'étude, formules en
 * rouge quand le procédé n'est pas maîtrisé, table des constantes à droite de
 * la grille, jeux d'exemple, enregistrement, refus du serveur dits sans
 * reprendre son texte, consultation seule.
 */
describe('CapabilityStudyComponent', () => {
  let fixture: ComponentFixture<CapabilityStudyComponent>;
  let component: CapabilityStudyComponent;
  let service: jasmine.SpyObj<SpcCapabilityService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let confirme: boolean;
  let router: Router;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const JEAN: DirectoryUser = { id: 'app-u1', accountId: 'u1', displayName: 'Jean Dupont', email: 'jean@acme.test', active: true };
  const TSV = GOLDEN_GRID.map(r => r.join('\t')).join('\r\n');

  async function setup(opts: { id?: string; canManage?: boolean; settings?: 'erreur' } = {}): Promise<void> {
    service = jasmine.createSpyObj<SpcCapabilityService>('SpcCapabilityService', ['get', 'create', 'update', 'settings']);
    service.settings.and.returnValue(opts.settings === 'erreur'
      ? throwError(() => ({ status: 500 }))
      : of({ capableThreshold: 1.5, watchThreshold: 1.2, defaults: false }));
    service.get.and.returnValue(of(sheet({ capableThreshold: 2, watchThreshold: 1.5, status: 'WATCH' })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    confirme = true;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake((() => ({ afterClosed: () => of(confirme) })) as never);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: [JEAN], page: 0, size: 20, totalElements: 1, totalPages: 1 }));
    directory.getByAccount.and.returnValue(of(JEAN));
    await TestBed.configureTestingModule({
      declarations: [CapabilityStudyComponent, ControlChartComponent, DistributionChartComponent],
      imports: [SharedModule, UiModule, FormsModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: SpcCapabilityService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: DirectoryService, useValue: directory },
        { provide: AuthzService, useValue: { can: () => of(opts.canManage ?? true) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(opts.id ? { id: opts.id } : {}) } } }
      ]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(CapabilityStudyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function remplir(): void {
    component.form.patchValue({ partReference: 'Shaft AX', characteristic: 'Weight', lsl: 475, usl: 525 });
    const evt = new ClipboardEvent('paste', { clipboardData: new DataTransfer() });
    evt.clipboardData!.setData('text', TSV);
    component.onPaste(0, 0, evt);
    fixture.detectChanges();
  }

  it('prend les seuils du client pour une étude nouvelle', async () => {
    await setup();
    expect(service.settings).toHaveBeenCalled();
    expect(component.capableThreshold).toBe(1.5);
    expect(component.watchThreshold).toBe(1.2);
    expect(component.title).toBe('Nouvelle étude de capabilité');
    expect(hote().textContent).toContain('Saisissez au moins un sous-groupe complet.');
  });

  it('garde les seuils de la plateforme quand ceux du client ne se lisent pas', async () => {
    await setup({ settings: 'erreur' });
    expect(component.capableThreshold).toBe(1.67);
  });

  it('colle la grille du classeur et rend le calcul doré', async () => {
    await setup();
    remplir();
    expect(component.calc.completeSubgroups).toBe(20);
    expect(component.calc.cpk!).toBeCloseTo(GOLDEN.cpk, 3);
    expect(un('kpi-cp')?.textContent).toContain('1.84');
    expect(un('kpi-cpk')?.textContent).toContain('1.77');
    expect(un('kpi-cpk')?.textContent).toContain('1.50');
    expect(un('kpi-cpk')?.classList).toContain('kpi--ok');
    expect(un('formule-sigma')?.textContent).toContain('4.5279');
    expect(un('formules')?.classList).not.toContain('formules--ko');
    expect(un('verdict-maitrise')?.textContent).toContain('OUI');
    expect(un('verdict-maitrise')?.classList).toContain('verdict--ok');
    expect(un('verdict-non-maitrise')?.textContent).toContain('NON');
    expect(un('verdict-non-maitrise')?.classList).not.toContain('verdict--ko');
    expect(un('formule-cpu')?.textContent).toContain('1.912');
    expect(un('formule-x-ecart')?.textContent?.trim()).not.toBe('–');
    expect(un('resume')?.textContent).toContain('1.84');
    expect(un('attente')).toBeNull();
    expect(un('diagnostic')?.textContent).toContain('capable');
    expect(un('lectures')?.textContent).toContain('54 %');
    expect(hote().querySelectorAll('qos-spc-control-chart svg').length).toBe(2);
    expect(hote().querySelector('qos-spc-distribution-chart svg')).not.toBeNull();
  });

  it('suit la saisie cellule par cellule et signale une cellule illisible', async () => {
    await setup();
    const cellule = un('cellule-0-0') as HTMLInputElement;
    cellule.value = '4a9';
    cellule.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.cell(0, 0)).toBe('4a9');
    expect(component.cellInvalid(0, 0)).toBeTrue();
    expect(cellule.classList).toContain('cellule--invalide');
    expect(component.cellInvalid(0, 1)).toBeFalse();
    expect(component.cellLabel(0, 1)).toBe('Échantillon 1, sous-groupe 2');
  });

  it('laisse passer un collage d\'une seule valeur', async () => {
    await setup();
    const evt = new ClipboardEvent('paste', { clipboardData: new DataTransfer(), cancelable: true });
    evt.clipboardData!.setData('text', '499');
    component.onPaste(0, 0, evt);
    expect(evt.defaultPrevented).toBeFalse();
    expect(component.cell(0, 0)).toBe('');
  });

  it('marque en rouge les sous-groupes hors limites de contrôle', async () => {
    await setup();
    remplir();
    for (let r = 0; r < 5; r++) component.raw[r][3] = String(540 + r);
    component.form.patchValue({});
    fixture.detectChanges();
    expect(component.meanOut(3)).toBeTrue();
    expect(un('moyenne-3')?.classList).toContain('hors');
    expect(component.meanOut(0)).toBeFalse();
    for (let r = 0; r < 5; r++) component.raw[r][4] = r === 0 ? '440' : '500';
    component.form.patchValue({});
    expect(component.rangeOut(4)).toBeTrue();
    expect(component.rangeOut(0)).toBeFalse();
    expect(component.meanOut(19)).toBeFalse();
  });

  it('change la taille du sous-groupe sans perdre les lignes masquées', async () => {
    await setup();
    remplir();
    component.form.controls.subgroupSize.setValue(3);
    fixture.detectChanges();
    expect(component.rows.length).toBe(3);
    expect(component.calc.d2).toBe(1.69);
    component.form.controls.subgroupSize.setValue(5);
    expect(component.calc.cpk!).toBeCloseTo(GOLDEN.cpk, 3);
  });

  it('tient les pastilles de statut et la table des constantes avant toute mesure', async () => {
    await setup();
    expect(un('verdict-maitrise')?.textContent).toContain('—');
    expect(un('verdict-non-maitrise')?.textContent).toContain('—');
    expect(un('verdict-maitrise')?.classList).not.toContain('verdict--ok');
    expect(un('verdict-non-maitrise')?.classList).not.toContain('verdict--ko');
    expect(un('kpi-cp')?.textContent).toContain('–');
    expect(un('attente')).not.toBeNull();
    expect(un('grille-vide')).not.toBeNull();
    expect(hote().querySelectorAll('[data-test^="constante-"]').length).toBe(9);
    expect(un('constante-5')?.getAttribute('aria-current')).toBe('true');
    expect(un('constante-4')?.hasAttribute('aria-current')).toBeFalse();
    component.form.controls.subgroupSize.setValue(4);
    fixture.detectChanges();
    expect(un('constante-4')?.classList).toContain('actif');
    expect(un('constante-4')?.textContent).toContain('2.06');
  });

  it('nomme les deux jeux d\'exemple', async () => {
    await setup();
    expect(component.examples).toEqual([WORKBOOK_EXAMPLE, TURNING_EXAMPLE]);
    expect(un('exemple-workbook')?.textContent).toContain('500 ± 25 g');
    expect(un('exemple-turning')?.textContent).toContain('Ø 12');
  });

  it('charge l\'exemple du classeur dans une grille vide, sans rien demander', async () => {
    await setup();
    un('exemple-workbook')!.click();
    fixture.detectChanges();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.form.value).toEqual(jasmine.objectContaining({
      characteristic: 'Weight', specification: '500 ± 25 gram', partReference: 'Shaft AX',
      lsl: 475, usl: 525, subgroupSize: 5
    }));
    expect(component.form.dirty).toBeTrue();
    expect(component.calc.cpk!).toBeCloseTo(GOLDEN.cpk, 3);
    expect(un('verdict-maitrise')?.classList).toContain('verdict--ok');
    expect(un('grille-vide')).toBeNull();
    expect(hote().querySelector('qos-spc-distribution-chart svg')).not.toBeNull();
    expect(un('carte-x')?.querySelector('[data-test="ligne-USL"]')).not.toBeNull();
    expect(un('carte-r')?.querySelector('[data-test="ligne-USL"]')).toBeNull();
  });

  it('demande avant de remplacer des mesures par l\'exemple non maîtrisé', async () => {
    await setup();
    remplir();
    confirme = false;
    un('exemple-turning')!.click();
    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(component.form.controls.partReference.value).toBe('Shaft AX');

    confirme = true;
    component.loadExample(TURNING_EXAMPLE);
    fixture.detectChanges();
    expect(dialog.open).toHaveBeenCalledTimes(2);
    expect(component.form.controls.partReference.value).toBe('Axe AX-12');
    expect(component.n).toBe(4);
    expect(component.raw[4].every(v => v === '')).toBeTrue();
    expect(component.calc.cp!).toBeCloseTo(1.42, 2);
    expect(component.calc.cpk!).toBeCloseTo(0.66, 2);
    expect(component.status).toBe('NOT_CAPABLE');
    expect(un('formules')?.classList).toContain('formules--ko');
    expect(un('verdict-non-maitrise')?.classList).toContain('verdict--ko');
    expect(un('moyenne-13')?.classList).toContain('hors');
    expect(un('constante-4')?.classList).toContain('actif');
  });

  it('vide les mesures', async () => {
    await setup();
    remplir();
    un('vider')!.click();
    expect(component.calc.completeSubgroups).toBe(0);
    expect(component.raw.every(r => r.every(v => v === ''))).toBeTrue();
  });

  it('masque et montre les formules', async () => {
    await setup();
    un('formules-bascule')!.click();
    fixture.detectChanges();
    expect(un('formules')).toBeNull();
    expect(un('formules-bascule')?.getAttribute('aria-expanded')).toBe('false');
    component.toggleFormulas();
    fixture.detectChanges();
    expect(un('formules')).not.toBeNull();
  });

  it('crée l\'étude, recopie le nom du meneur et ouvre la fiche enregistrée', async () => {
    await setup();
    remplir();
    component.form.patchValue({ leaderUserId: 'u1', machine: '  ', specification: ' 500 ± 25 ' });
    component.onLeader(JEAN);
    service.create.and.returnValue(of(sheet({ id: 'neuve' })));
    un('enregistrer')!.click();
    const req = service.create.calls.mostRecent().args[0];
    expect(req.partReference).toBe('Shaft AX');
    expect(req.leaderUserId).toBe('u1');
    expect(req.leaderName).toBe('Jean Dupont');
    expect(req.machine).toBeNull();
    expect(req.specification).toBe('500 ± 25');
    expect(req.subgroupSize).toBe(5);
    expect(req.measurements.length).toBe(5);
    expect(req.measurements[0][0]).toBe(504);
    expect(Object.keys(req)).not.toContain('cpk');
    expect(snack.open).toHaveBeenCalledWith('Étude enregistrée.', undefined, { duration: 3000 });
    expect(router.navigate).toHaveBeenCalledWith(['/spc', 'cartes', 'neuve'], { replaceUrl: true });
  });

  it('n\'envoie pas de nom de meneur sans meneur', async () => {
    await setup();
    remplir();
    component.onLeader(JEAN);
    component.onLeader(null);
    service.create.and.returnValue(of(sheet()));
    component.save();
    expect(service.create.calls.mostRecent().args[0].leaderName).toBeNull();
  });

  it('bloque l\'enregistrement d\'un formulaire incomplet ou de limites inversées', async () => {
    await setup();
    component.save();
    expect(service.create).not.toHaveBeenCalled();
    remplir();
    component.form.patchValue({ lsl: 530 });
    fixture.detectChanges();
    expect(component.limitsInvalid).toBeTrue();
    expect(un('limites-invalides')).not.toBeNull();
    component.save();
    expect(service.create).not.toHaveBeenCalled();
    expect(component.readings).toBeNull();
  });

  it('demande un sous-groupe complet avant d\'enregistrer', async () => {
    await setup();
    component.form.patchValue({ partReference: 'Shaft AX', lsl: 475, usl: 525 });
    component.save();
    fixture.detectChanges();
    expect(service.create).not.toHaveBeenCalled();
    expect(un('erreur-champ')?.textContent).toContain('Saisissez au moins un sous-groupe complet');
  });

  it('dit le champ refusé par le serveur sans reprendre son texte', async () => {
    await setup();
    remplir();
    service.create.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { field: 'usl', detail: 'org.hibernate…' }
    })));
    component.save();
    fixture.detectChanges();
    expect(component.saving).toBeFalse();
    expect(un('erreur-champ')?.textContent).toContain('La limite supérieure (USL) doit dépasser');
    expect(hote().textContent).not.toContain('hibernate');
  });

  it('dit un échec d\'enregistrement sans champ', async () => {
    await setup();
    remplir();
    service.create.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.save();
    expect(snack.open).toHaveBeenCalledWith('Erreur serveur — réessayez dans un instant.', undefined, { duration: 5000 });
    expect(component.fieldError).toBeNull();
  });

  it('a un message pour chaque champ refusé', () => {
    expect(fieldMessage('partReference')).toContain('référence');
    expect(fieldMessage('studyDate')).toContain('date');
    expect(fieldMessage('lsl')).toBe(fieldMessage('usl'));
    expect(fieldMessage('leaderName')).toContain('annuaire');
    expect(fieldMessage('measurements')).toContain('sous-groupe');
    expect(fieldMessage('subgroupSize')).toContain('vérifiez');
  });

  describe('une étude enregistrée', () => {
    it('reprend la fiche avec ses seuils figés et lit les formules en rouge', async () => {
      await setup({ id: 's1' });
      fixture.detectChanges();
      expect(service.get).toHaveBeenCalledWith('s1');
      expect(service.settings).not.toHaveBeenCalled();
      expect(component.capableThreshold).toBe(2);
      expect(component.status).toBe('WATCH');
      expect(component.title).toBe('Étude de capabilité Shaft AX');
      expect(un('kpi-cpk')?.classList).toContain('kpi--warn');
      expect(un('formules')?.classList).toContain('formules--ko');
      expect(un('verdict-maitrise')?.textContent).toContain('NON');
      expect(un('verdict-non-maitrise')?.classList).toContain('verdict--ko');
      expect(un('verdict-non-maitrise')?.textContent).toContain('OUI');
      expect(un('diagnostic')?.classList).toContain('diagnostic--ko');
      expect(un('diagnostic')?.textContent).toContain('réduire');
      expect(component.form.controls.partReference.value).toBe('Shaft AX');
    });

    it('met à jour l\'étude et reprend la réponse du serveur', async () => {
      await setup({ id: 's1' });
      service.update.and.returnValue(of(sheet({ capableThreshold: 1.67, watchThreshold: 1.33, actionPlan: true })));
      component.save();
      expect(service.update).toHaveBeenCalledWith('s1', jasmine.objectContaining({ partReference: 'Shaft AX' }));
      expect(router.navigate).not.toHaveBeenCalled();
      expect(component.capableThreshold).toBe(1.67);
      expect(component.form.controls.actionPlan.value).toBeTrue();
      expect(component.form.pristine).toBeTrue();
    });

    it('dit qu\'elle est introuvable, et réessaie', async () => {
      await setup({ id: 'absente' });
      service.get.and.returnValue(throwError(() => ({ status: 404 })));
      component.charger();
      fixture.detectChanges();
      expect(component.failed).toBeTrue();
      expect(snack.open).toHaveBeenCalledWith('Étude introuvable.', undefined, { duration: 5000 });
      expect(un('enregistrer')?.hasAttribute('disabled')).toBeTrue();
    });

    it('revient au registre', async () => {
      await setup({ id: 's1' });
      un('retour')!.click();
      expect(router.navigate).toHaveBeenCalledWith(['/spc', 'cartes']);
    });
  });

  it('se consulte sans rien pouvoir modifier sans le droit spc.manage', async () => {
    await setup({ id: 's1', canManage: false });
    fixture.detectChanges();
    expect(un('lecture-seule')).not.toBeNull();
    expect(un('enregistrer')).toBeNull();
    expect(un('vider')).toBeNull();
    expect(un('exemples')).toBeNull();
    const avant = component.raw.map(r => [...r]);
    component.loadExample(TURNING_EXAMPLE);
    expect(component.raw).toEqual(avant);
    expect(dialog.open).not.toHaveBeenCalled();
    expect((un('cellule-0-0') as HTMLInputElement).readOnly).toBeTrue();
    expect(component.form.disabled).toBeTrue();
    component.save();
    expect(service.update).not.toHaveBeenCalled();
  });
});
