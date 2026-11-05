import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../../audits/audits.service';
import { AuditPlanResponse, AuditsPage } from '../../../audits/audits.types';
import { DocumentsService } from '../../../documents/documents.service';
import { DocumentPage, DocumentResponse } from '../../../documents/documents.types';
import { NcService } from '../../../nc/nc.service';
import { NcPage, NcResponse } from '../../../nc/nc.types';
import { RiskRegisterService } from '../../../risk-register/risk-register.service';
import { RiskView } from '../../../risk-register/risk-register.types';
import { StakeholderLinkDialogComponent, StakeholderLinkDialogData } from './stakeholder-link-dialog.component';

/**
 * On rattache en CHOISISSANT dans une liste : personne ne connaît par cœur
 * l'UUID d'un risque, et un identifiant tapé de travers fabriquerait un
 * rattachement qui ne mène nulle part.
 */
describe('StakeholderLinkDialogComponent', () => {

  const RISKS = [
    { id: 'r-1', reference: 'R-2026-0001', title: 'Rupture de chaîne du froid' },
    { id: 'r-2', reference: 'R-2026-0002', title: 'Étalonnage périmé' }
  ] as RiskView[];

  function page<T>(content: T[]): { content: T[]; totalElements: number; totalPages: number;
                                    number: number; size: number } {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length };
  }

  let fixture: ComponentFixture<StakeholderLinkDialogComponent>;
  let component: StakeholderLinkDialogComponent;
  let risks: jasmine.SpyObj<RiskRegisterService>;
  let ncs: jasmine.SpyObj<NcService>;
  let audits: jasmine.SpyObj<AuditsService>;
  let documents: jasmine.SpyObj<DocumentsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<StakeholderLinkDialogComponent, string>>;

  async function build(data: StakeholderLinkDialogData): Promise<void> {
    risks = jasmine.createSpyObj<RiskRegisterService>('RiskRegisterService', ['risks']);
    ncs = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['listPlans']);
    documents = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['list']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    risks.risks.and.returnValue(of(RISKS));
    ncs.listNcs.and.returnValue(of(page([
      { id: 'n-1', reference: 'NC-2026-0007', title: 'Lot non étiqueté' } as NcResponse
    ]) as NcPage));
    audits.listPlans.and.returnValue(of(page([
      { id: 'a-1', reference: 'AUD-2026-0003', title: 'Audit interne ISO 9001' } as AuditPlanResponse
    ]) as AuditsPage));
    documents.list.and.returnValue(of(page([
      { id: 'd-1', code: 'PR-QUA-012', title: 'Maîtrise des documents' } as DocumentResponse
    ]) as DocumentPage));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderLinkDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: RiskRegisterService, useValue: risks },
        { provide: NcService, useValue: ncs },
        { provide: AuditsService, useValue: audits },
        { provide: DocumentsService, useValue: documents },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderLinkDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('charge les risques, chacun reconnaissable à sa référence et à son intitulé', async () => {
    await build({ kind: 'RISK', requirement: 'Déclarer tout EIG sous 48 h' });

    expect(risks.risks).toHaveBeenCalled();
    expect(component.loading).toBeFalse();
    expect(component.targets).toEqual([
      { id: 'r-1', label: 'R-2026-0001 — Rupture de chaîne du froid' },
      { id: 'r-2', label: 'R-2026-0002 — Étalonnage périmé' }
    ]);
    // L'exigence est rappelée : on sait ce qu'on rattache.
    expect(component.subtitle).toContain('Déclarer tout EIG sous 48 h');
  });

  it('va chercher la liste du module visé, et pas une autre', async () => {
    await build({ kind: 'NON_CONFORMITY', requirement: 'x' });
    expect(ncs.listNcs).toHaveBeenCalled();
    expect(risks.risks).not.toHaveBeenCalled();
    expect(component.targets[0].label).toBe('NC-2026-0007 — Lot non étiqueté');

    await build({ kind: 'AUDIT', requirement: 'x' });
    expect(audits.listPlans).toHaveBeenCalled();
    expect(component.targets[0].label).toBe('AUD-2026-0003 — Audit interne ISO 9001');

    await build({ kind: 'DOCUMENT', requirement: 'x' });
    expect(documents.list).toHaveBeenCalled();
    expect(component.targets[0].label).toBe('PR-QUA-012 — Maîtrise des documents');
  });

  it('filtre la liste sans tenir compte des accents ni de la casse', async () => {
    await build({ kind: 'RISK', requirement: 'x' });

    component.form.patchValue({ filter: 'ETALONNAGE' });
    expect(component.filtered.map(t => t.id)).toEqual(['r-2']);

    component.form.patchValue({ filter: 'introuvable' });
    expect(component.filtered).toEqual([]);
  });

  it('rend l’identifiant choisi, et rien d’autre', async () => {
    await build({ kind: 'RISK', requirement: 'x' });

    component.form.patchValue({ target: 'r-2' });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith('r-2');
  });

  it('sans choix, ne ferme pas', async () => {
    await build({ kind: 'RISK', requirement: 'x' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.blockedReason).toBeTruthy();
  });

  it('si le chargement échoue, le DIT et laisse annuler', async () => {
    await build({ kind: 'RISK', requirement: 'x' });
    // Le montage a déjà chargé : on rejoue le chargement, service en panne.
    risks.risks.and.returnValue(throwError(() => ({ status: 503 })));
    component.targets = [];
    component.ngOnInit();

    expect(component.failed).toBeTrue();
    expect(component.loading).toBeFalse();
    expect(component.blockedReason).toBeTruthy();

    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  it('quand il n’y a rien à rattacher, le dit plutôt que de proposer une liste vide', async () => {
    await build({ kind: 'RISK', requirement: 'x' });
    component.targets = [];

    expect(component.blockedReason).toContain('rien');
  });
});
