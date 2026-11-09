import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../../nc/nc.service';
import { NcSubcategoryCatalogue } from '../../../nc/nc.types';
import { AuditsService } from '../../audits.service';
import { FindingResponse } from '../../audits.types';
import { AuditsDeclareNcDialogComponent } from './audits-declare-nc-dialog.component';

/**
 * « Un écart découvert lors d'un audit devient une NC à traiter. »
 *
 * Ce dialogue est le seul chemin de cette phrase. Ce qui est vérifié ici, c'est
 * surtout ce qu'il NE fait pas : il ne renvoie ni la description, ni la
 * gravité, ni la date du constat. Les redemander, ou les recopier en silence,
 * ouvrirait deux versions du même fait — exactement ce que ce chemin ferme.
 */
describe('AuditsDeclareNcDialogComponent', () => {
  let fixture: ComponentFixture<AuditsDeclareNcDialogComponent>;
  let component: AuditsDeclareNcDialogComponent;
  let audits: jasmine.SpyObj<AuditsService>;
  let nc: jasmine.SpyObj<NcService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsDeclareNcDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const constat = {
    id: 'f1',
    type: 'MAJOR_NC',
    description: 'Les enregistrements de formation ne sont pas signés',
    clauseRef: '7.2',
    raisedAt: '2026-11-09T10:00:00Z'
  } as FindingResponse;

  async function monter(finding: FindingResponse = constat,
                        catalogue: Observable<NcSubcategoryCatalogue> = of({
                          defaults: false,
                          entries: [
                            { code: 'DOC-SIGN', label: 'Signature manquante', category: 'DOCUMENTATION' },
                            { code: 'PRO-FLUX', label: 'Flux non respecté', category: 'PROCESS' }
                          ]
                        })): Promise<void> {
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['declareFindingNc']);
    nc = jasmine.createSpyObj<NcService>('NcService', ['subcategories']);
    nc.subcategories.and.returnValue(catalogue);
    dialogRef = jasmine.createSpyObj<MatDialogRef<AuditsDeclareNcDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [AuditsDeclareNcDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: audits },
        { provide: NcService, useValue: nc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { planId: 'p1', finding, sourceRef: 'AUD-2026-0003 / constat 2' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditsDeclareNcDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => monter());

  it('pré-remplit l\'intitulé par la description du constat', () => {
    expect(component.form.getRawValue().title).toBe(constat.description);
  });

  it('n\'envoie que l\'intitulé, la catégorie et la nature', () => {
    audits.declareFindingNc.and.returnValue(of({
      findingId: 'f1', ncId: 'nc1', ncReference: 'NC-2026-0042', sourceRef: 'AUD-2026-0003 / constat 2'
    }));
    component.form.patchValue({ title: '  Formations non signées  ', category: 'DOCUMENTATION' });
    component.form.controls.subcategoryCode.setValue('DOC-SIGN');

    component.submit();

    expect(audits.declareFindingNc).toHaveBeenCalledWith('p1', 'f1', {
      title: 'Formations non signées',
      category: 'DOCUMENTATION',
      subcategoryCode: 'DOC-SIGN'
    });
    const envoye = audits.declareFindingNc.calls.mostRecent().args[2] as unknown as Record<string, unknown>;
    // Rien du constat n'est recopié : le serveur le relit lui-même.
    for (const champ of ['description', 'severity', 'detectedAt', 'origin', 'clauseRef']) {
      expect(champ in envoye).withContext(champ).toBeFalse();
    }
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('omet la nature quand elle n\'est pas renseignée', () => {
    audits.declareFindingNc.and.returnValue(of({
      findingId: 'f1', ncId: 'nc1', ncReference: 'NC-1', sourceRef: 'x'
    }));
    component.submit();
    expect(audits.declareFindingNc.calls.mostRecent().args[2].subcategoryCode).toBeUndefined();
  });

  it('ne propose que les natures de la catégorie retenue', () => {
    component.form.controls.category.setValue('DOCUMENTATION');
    expect(component.subcategoryChoices.map(s => s.code)).toEqual(['DOC-SIGN']);
    component.form.controls.category.setValue('PROCESS');
    expect(component.subcategoryChoices.map(s => s.code)).toEqual(['PRO-FLUX']);
  });

  it('oublie la nature quand la catégorie change, le serveur refusant un code croisé', () => {
    component.form.controls.category.setValue('DOCUMENTATION');
    component.form.controls.subcategoryCode.setValue('DOC-SIGN');
    component.form.controls.category.setValue('PROCESS');
    expect(component.form.getRawValue().subcategoryCode).toBe('');
  });

  it('montre la gravité que le serveur déduira, sans la demander', () => {
    expect(component.severityLabel).toContain('Majeure');
    expect('severity' in component.form.controls).toBeFalse();
  });

  it('un constat mineur annonce une gravité mineure', async () => {
    await monter({ ...constat, type: 'MINOR_NC' } as FindingResponse);
    expect(component.severityLabel).toContain('Mineure');
  });

  it('un catalogue illisible ne bloque pas la déclaration', async () => {
    await monter(constat, throwError(() => ({ status: 500 })));
    expect(component.subcategoryChoices).toEqual([]);
    expect(component.form.valid).toBeTrue();
  });

  it('409 : dit que le travail est déjà fait, sans envoyer recharger', () => {
    audits.declareFindingNc.and.returnValue(throwError(() => ({ status: 409 })));
    component.submit();
    const message = snack.open.calls.mostRecent().args[0];
    expect(message).toContain('déjà');
    expect(message).not.toContain('rechargez');
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('autre erreur : dialogue laissé ouvert', () => {
    audits.declareFindingNc.and.returnValue(throwError(() => ({ status: 500 })));
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('refuse un intitulé vide', () => {
    component.form.controls.title.setValue('');
    component.submit();
    expect(audits.declareFindingNc).not.toHaveBeenCalled();
  });
});
