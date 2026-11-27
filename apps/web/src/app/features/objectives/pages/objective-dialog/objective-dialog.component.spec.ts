import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
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

  function page(content: KpiResponse[]): KpiPage {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 50 };
  }

  let fixture: ComponentFixture<ObjectiveDialogComponent>;
  let component: ObjectiveDialogComponent;
  let svc: jasmine.SpyObj<ObjectivesService>;
  let kpis: jasmine.SpyObj<KpisService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ObjectiveDialogComponent, QualityObjective>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(
    data: ObjectiveDialogData = {}, peutEcrireAuxKpis = true, reportAccepte = true
  ): Promise<void> {
    svc = jasmine.createSpyObj<ObjectivesService>('ObjectivesService', ['create', 'update']);
    svc.create.and.returnValue(of({ id: 'o-1' } as QualityObjective));
    svc.update.and.returnValue(of({ id: 'o-1' } as QualityObjective));

    kpis = jasmine.createSpyObj<KpisService>('KpisService', ['list', 'update']);
    kpis.list.and.returnValue(of(page([PPM, TAUX])));
    kpis.update.and.returnValue(of(PPM));

    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(of(peutEcrireAuxKpis));

    // La confirmation du report sur le KPI : elle répond ce que le banc veut.
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue(
      { afterClosed: () => of(reportAccepte) } as never);

    const docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['processes']);
    docs.processes.and.returnValue(of([{ id: 'p-1', name: 'Production', position: 0 }]));

    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectiveDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ObjectivesService, useValue: svc },
        { provide: KpisService, useValue: kpis },
        { provide: DocumentsService, useValue: docs },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: AuthzService, useValue: authz },
        { provide: MatDialog, useValue: dialog }
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
      thresholdCritical: 300
    });
    component.submit();
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('crée l\'objectif avec le nom du processus repris, pas ressaisi', async () => {
    await build();
    component.form.patchValue({
      title: '  Taux de NC client  ', processId: 'p-1', kpiId: 'k-1', periodMonths: 3,
      targetValue: 500, thresholdWarning: 600, thresholdCritical: 800
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
      thresholdCritical: 800, isoClause: ''
    });
    component.submit();
    const body = svc.create.calls.mostRecent().args[0];
    expect(body.processId).toBeNull();
    expect(body.processName).toBeNull();
    expect(body.isoClause).toBeNull();
  });

  /**
   * La recette ne veut plus saisir deux fois la même cible : choisir le KPI
   * descend ses seuils dans les trois champs, qui restent modifiables.
   */
  it('reprend les seuils du KPI choisi, à la création', async () => {
    await build();
    component.form.controls.kpiId.setValue('k-1');
    component.onKpiChosen();
    expect(component.form.controls.targetValue.value).toBe(500);
    expect(component.form.controls.thresholdWarning.value).toBe(600);
    expect(component.form.controls.thresholdCritical.value).toBe(800);
  });

  /**
   * Un KPI sans cible n'efface pas ce qui est déjà saisi : ce serait perdre le
   * travail de l'utilisateur au moment où il change d'avis d'indicateur.
   */
  it('ne vide rien quand le KPI choisi ne porte pas de seuils', async () => {
    await build();
    component.form.patchValue({ targetValue: 42, thresholdWarning: 43, thresholdCritical: 44 });
    component.form.controls.kpiId.setValue('k-2');
    component.onKpiChosen();
    expect(component.form.controls.targetValue.value).toBe(42);
    expect(component.form.controls.thresholdCritical.value).toBe(44);
  });

  /**
   * En modification le KPI est verrouillé : écraser les seuils par ceux du
   * catalogue réécrirait un engagement déjà pris.
   */
  it('ne reprend pas les seuils du KPI en modification', async () => {
    await build({ objective: {
      id: 'o-1', title: 'Existant', kpiId: 'k-1', periodMonths: 1, targetValue: 95,
      thresholdWarning: 90, thresholdCritical: 85, warningPeriods: 2,
      criticalPeriods: 1, recoveryPeriods: 1
    } as QualityObjective });
    component.onKpiChosen();
    expect(component.form.controls.targetValue.value).toBe(95);
  });

  /**
   * Les seuils modifiés s'écartent de ceux du KPI : la fenêtre PROPOSE de les
   * reporter. Elle ne le fait pas d'office — un même KPI peut porter deux
   * objectifs de cibles différentes (ADR 0133 §1), et écrire sans demander
   * écraserait silencieusement le premier.
   */
  it('propose de reporter les seuils modifiés sur le KPI, et le fait si on accepte',
    async () => {
      await build();
      component.form.patchValue({
        title: 'T', kpiId: 'k-1', targetValue: 400, thresholdWarning: 500,
        thresholdCritical: 700
      });
      expect(component.thresholdsDifferFromKpi).toBeTrue();
      component.submit();
      expect(dialog.open).toHaveBeenCalled();
      expect(kpis.update).toHaveBeenCalled();
      const [id, corps] = kpis.update.calls.mostRecent().args as [string, Record<string, number>];
      expect(id).toBe('k-1');
      expect(corps['targetValue']).toBe(400);
      expect(corps['thresholdCritical']).toBe(700);
    });

  it('n\'écrit pas au KPI si on refuse le report', async () => {
    await build({}, true, false);
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 400, thresholdWarning: 500,
      thresholdCritical: 700
    });
    component.submit();
    expect(kpis.update).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  /** Sans le droit d'écrire au catalogue, il n'y a rien à proposer. */
  it('ne propose rien à qui ne peut pas écrire au catalogue des KPI', async () => {
    await build({}, false);
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 400, thresholdWarning: 500,
      thresholdCritical: 700
    });
    expect(component.thresholdsDifferFromKpi).toBeFalse();
    component.submit();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(kpis.update).not.toHaveBeenCalled();
  });

  /**
   * Le refus du catalogue ne perd pas l'objectif : il est déjà enregistré, et
   * c'est lui que l'utilisateur venait créer.
   */
  it('garde l\'objectif enregistré même si le report au KPI échoue', async () => {
    await build();
    kpis.update.and.returnValue(throwError(() => ({ status: 403 })));
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 400, thresholdWarning: 500,
      thresholdCritical: 700
    });
    component.submit();
    expect(svc.create).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  /** Seuils identiques à ceux du KPI : aucune question à poser. */
  it('ne demande rien quand les seuils sont ceux du KPI', async () => {
    await build();
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 500, thresholdWarning: 600,
      thresholdCritical: 800
    });
    expect(component.thresholdsDifferFromKpi).toBeFalse();
    component.submit();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('garde l\'écran ouvert et dit l\'erreur quand le serveur refuse', async () => {
    await build();
    svc.create.and.returnValue(throwError(() => ({ status: 400 })));
    component.form.patchValue({
      title: 'T', kpiId: 'k-1', targetValue: 500, thresholdWarning: 600,
      thresholdCritical: 800
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
      effectivenessCriterion: 'Le KPI revient sous 95 %…'
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
      criticalPeriods: 1, recoveryPeriods: 1
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
    const catalogue = jasmine.createSpyObj<KpisService>('KpisService', ['list', 'update']);
    catalogue.list.and.returnValue(throwError(() => new Error('boom')));
    const docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['processes']);
    docs.processes.and.returnValue(throwError(() => new Error('boom')));
    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(of(true));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectiveDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ObjectivesService, useValue: svc },
        { provide: KpisService, useValue: catalogue },
        { provide: DocumentsService, useValue: docs },
        { provide: MatDialogRef, useValue: jasmine.createSpyObj('MatDialogRef', ['close']) },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: MAT_DIALOG_DATA, useValue: {} },
        { provide: AuthzService, useValue: authz },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) }
      ]
    }).compileComponents();

    const f = TestBed.createComponent(ObjectiveDialogComponent);
    f.detectChanges();
    expect(f.componentInstance.kpis).toEqual([]);
    expect(f.componentInstance.processes).toEqual([]);
  });
});
