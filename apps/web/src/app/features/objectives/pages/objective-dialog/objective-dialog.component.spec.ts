import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { DocumentsService } from '../../../documents/documents.service';
import { KpisService } from '../../../kpis/kpis.service';
import { KpiPage, KpiResponse } from '../../../kpis/kpis.types';
import { ObjectivesService } from '../../objectives.service';
import { QualityObjective } from '../../objectives.types';
import { ObjectiveDialogComponent, ObjectiveDialogData } from './objective-dialog.component';

/**
 * La fenêtre « Nouvel objectif qualité ».
 *
 * <p>Deux choses s'y jouent et sont vérifiées ici : l'unité et le sens
 * d'amélioration viennent du KPI et ne sont jamais écrits en dur, et les seuils
 * doivent être ordonnés dans le sens de ce KPI — un seuil d'alerte du bon côté
 * de la cible ne déclencherait jamais, et l'objectif aurait l'air réglé.
 */
describe('ObjectiveDialogComponent — définir un objectif (ADR 0133)', () => {

  const PPM: KpiResponse = {
    id: 'k-1', code: 'NC-CLI', name: 'NC client', category: 'Qualité',
    unit: 'ppm', direction: 'LOWER_IS_BETTER', frequency: 'MONTHLY',
    status: 'ACTIVE'
  } as KpiResponse;

  const TAUX: KpiResponse = {
    ...PPM, id: 'k-2', code: 'CONF', name: 'Taux de conformité',
    unit: '%', direction: 'HIGHER_IS_BETTER'
  } as KpiResponse;

  /** La personne que l'annuaire renvoie : le pilote désigné dans les bancs. */
  const AWA: DirectoryUser = {
    id: 'i', accountId: 'u-1', displayName: 'Awa Diallo',
    email: 'a@d.fr', active: true
  };

  /** L'annuaire du choix de pilote : il ne rend personne, ce n'est pas le sujet. */
  function annuaireVide(): jasmine.SpyObj<DirectoryService> {
    const directory = jasmine.createSpyObj<DirectoryService>(
      'DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({
      content: [AWA], page: 0, size: 20, totalElements: 1, totalPages: 1
    }));
    directory.getByAccount.and.returnValue(of(AWA));
    return directory;
  }

  function page(content: KpiResponse[]): KpiPage {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 50 };
  }

  let fixture: ComponentFixture<ObjectiveDialogComponent>;
  let component: ObjectiveDialogComponent;
  let svc: jasmine.SpyObj<ObjectivesService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ObjectiveDialogComponent, QualityObjective>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(data: ObjectiveDialogData = {}): Promise<void> {
    svc = jasmine.createSpyObj<ObjectivesService>('ObjectivesService', ['create', 'update']);
    svc.create.and.returnValue(of({ id: 'o-1' } as QualityObjective));
    svc.update.and.returnValue(of({ id: 'o-1' } as QualityObjective));

    const kpis = jasmine.createSpyObj<KpisService>('KpisService', ['list']);
    kpis.list.and.returnValue(of(page([PPM, TAUX])));

    const docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['processes']);
    docs.processes.and.returnValue(of([{ id: 'p-1', name: 'Production', position: 0 }]));

    const directory = annuaireVide();

    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectiveDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: ObjectivesService, useValue: svc },
        { provide: KpisService, useValue: kpis },
        { provide: DocumentsService, useValue: docs },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: DirectoryService, useValue: directory }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ObjectiveDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Les trois sections numérotées de la maquette, dans l'ordre. */
  it('présente les trois sections de la maquette', async () => {
    await build();
    const titres = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.section'))
      .map(e => e.textContent?.trim());
    expect(titres.length).toBe(3);
    expect(titres[0]).toContain('1.');
    expect(titres[2]).toContain('3.');
  });

  it('n\'affiche aucune unité tant qu\'aucun KPI n\'est choisi', async () => {
    await build();
    expect(component.unit).toBe('');
    expect(component.directionLabel).toBe('Choisissez un KPI');
  });

  /**
   * L'unité est REPRISE du KPI : l'écrire sur le champ la ferait mentir dès le
   * deuxième objectif. C'est l'annotation de la maquette, et c'est aussi ce que
   * le serveur impose (il ne stocke pas d'unité sur l'objectif).
   */
  it('reprend l\'unité et le sens du KPI choisi', async () => {
    await build();
    component.form.controls.kpiId.setValue('k-1');
    expect(component.unit).toBe('ppm');
    expect(component.directionLabel).toBe('Diminuer');

    component.form.controls.kpiId.setValue('k-2');
    expect(component.unit).toBe('%');
    expect(component.directionLabel).toBe('Augmenter');
  });

  it('ne reproche rien sur un formulaire à moitié rempli', async () => {
    await build();
    component.form.controls.kpiId.setValue('k-1');
    component.form.controls.targetValue.setValue(500);
    expect(component.thresholdsOutOfOrder).toBeFalse();
    expect(component.blockedReason).toBeUndefined();
  });

  it('accepte des seuils ordonnés sur un KPI à diminuer', async () => {
    await build();
    component.form.patchValue({
      kpiId: 'k-1', targetValue: 500, thresholdWarning: 600, thresholdCritical: 800
    });
    expect(component.thresholdsOutOfOrder).toBeFalse();
  });

  it('refuse des seuils à l\'envers du sens du KPI, et dit pourquoi', async () => {
    await build();
    component.form.patchValue({
      kpiId: 'k-1', targetValue: 500, thresholdWarning: 400, thresholdCritical: 300
    });
    expect(component.thresholdsOutOfOrder).toBeTrue();
    expect(component.blockedReason).toContain('diminuer');
  });

  it('inverse l\'ordre attendu sur un KPI à augmenter', async () => {
    await build();
    component.form.patchValue({
      kpiId: 'k-2', targetValue: 95, thresholdWarning: 90, thresholdCritical: 85
    });
    expect(component.thresholdsOutOfOrder).toBeFalse();

    component.form.patchValue({ thresholdWarning: 98 });
    expect(component.thresholdsOutOfOrder).toBeTrue();
    expect(component.blockedReason).toContain('augmenter');
  });

  it('tolère l\'égalité : cible et seuil confondus restent ordonnés', async () => {
    await build();
    component.form.patchValue({
      kpiId: 'k-1', targetValue: 500, thresholdWarning: 500, thresholdCritical: 500
    });
    expect(component.thresholdsOutOfOrder).toBeFalse();
  });

  it('n\'envoie rien quand les seuils sont désordonnés', async () => {
    await build();
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 500, thresholdWarning: 400,
      thresholdCritical: 300, capaOwnerId: 'u-1'
    });
    component.submit();
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('crée l\'objectif avec le nom du processus repris, pas ressaisi', async () => {
    await build();
    component.form.patchValue({
      title: '  Taux de NC client  ', processId: 'p-1', kpiId: 'k-1', periodMonths: 3,
      targetValue: 500, thresholdWarning: 600, thresholdCritical: 800, capaOwnerId: 'u-1'
    });
    component.submit();
    expect(svc.create).toHaveBeenCalled();
    const body = svc.create.calls.mostRecent().args[0];
    expect(body.title).toBe('Taux de NC client');
    expect(body.processName).toBe('Production');
    expect(body.periodMonths).toBe(3);
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('envoie null et non une chaîne vide pour un processus non choisi', async () => {
    await build();
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 500, thresholdWarning: 600,
      thresholdCritical: 800, capaOwnerId: 'u-1', isoClause: ''
    });
    component.submit();
    const body = svc.create.calls.mostRecent().args[0];
    expect(body.processId).toBeNull();
    expect(body.processName).toBeNull();
    expect(body.isoClause).toBeNull();
  });

  /** Le nom du responsable est RECOPIÉ au choix : la revue n'affiche pas un UUID. */
  it('recopie le nom du responsable choisi dans l\'annuaire', async () => {
    await build();
    component.onCapaOwner(AWA);
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 500, thresholdWarning: 600,
      thresholdCritical: 800, capaOwnerId: 'u-1'
    });
    component.submit();
    expect(svc.create.calls.mostRecent().args[0].capaOwnerName).toBe('Awa Diallo');
  });

  it('garde l\'écran ouvert et dit l\'erreur quand le serveur refuse', async () => {
    await build();
    svc.create.and.returnValue(throwError(() => ({ status: 400 })));
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 500, thresholdWarning: 600,
      thresholdCritical: 800, capaOwnerId: 'u-1'
    });
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  /**
   * À la modification, le KPI est VERROUILLÉ : un objectif qui changerait
   * d'indicateur rendrait incomparable son propre historique — c'est pourquoi le
   * serveur ne l'accepte pas non plus.
   */
  it('verrouille le KPI en modification, et reprend les valeurs existantes', async () => {
    const o = {
      id: 'o-1', title: 'Existant', kpiId: 'k-2', processId: 'p-1', isoClause: '6.2',
      periodMonths: 6, targetValue: 95, thresholdWarning: 90, thresholdCritical: 85,
      warningPeriods: 3, criticalPeriods: 2, recoveryPeriods: 2,
      capaOwnerId: 'u-7', capaOwnerName: 'Jean Koffi', rootCauseDueDays: 10
    } as QualityObjective;
    await build({ objective: o });
    expect(component.editing).toBeTrue();
    expect(component.form.controls.kpiId.disabled).toBeTrue();
    expect(component.form.controls.targetValue.value).toBe(95);
    expect(component.form.controls.periodMonths.value).toBe(6);
  });

  it('modifie par update, et non par create', async () => {
    await build({ objective: {
      id: 'o-1', title: 'Existant', kpiId: 'k-1', periodMonths: 1, targetValue: 500,
      thresholdWarning: 600, thresholdCritical: 800, warningPeriods: 2,
      criticalPeriods: 1, recoveryPeriods: 1, capaOwnerId: 'u-1', rootCauseDueDays: 5
    } as QualityObjective });
    component.form.controls.targetValue.setValue(400);
    component.submit();
    expect(svc.update).toHaveBeenCalled();
    expect(svc.create).not.toHaveBeenCalled();
    expect(svc.update.calls.mostRecent().args[0]).toBe('o-1');
  });

  it('ferme sans rien envoyer à l\'annulation', async () => {
    await build();
    component.cancel();
    expect(svc.create).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  it('survit à un catalogue de KPI indisponible', async () => {
    svc = jasmine.createSpyObj<ObjectivesService>('ObjectivesService', ['create', 'update']);
    const kpis = jasmine.createSpyObj<KpisService>('KpisService', ['list']);
    kpis.list.and.returnValue(throwError(() => new Error('boom')));
    const docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['processes']);
    docs.processes.and.returnValue(throwError(() => new Error('boom')));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectiveDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: ObjectivesService, useValue: svc },
        { provide: KpisService, useValue: kpis },
        { provide: DocumentsService, useValue: docs },
        { provide: MatDialogRef, useValue: jasmine.createSpyObj('MatDialogRef', ['close']) },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: MAT_DIALOG_DATA, useValue: {} },
        { provide: DirectoryService, useValue: annuaireVide() }
      ]
    }).compileComponents();

    const f = TestBed.createComponent(ObjectiveDialogComponent);
    f.detectChanges();
    expect(f.componentInstance.kpis).toEqual([]);
    expect(f.componentInstance.processes).toEqual([]);
  });
});
