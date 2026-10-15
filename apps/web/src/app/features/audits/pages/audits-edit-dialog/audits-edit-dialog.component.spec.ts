import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerComponent } from '../../../../shared/user-picker/user-picker.component';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse } from '../../audits.types';
import { AuditsEditDialogComponent, AuditsEditDialogData } from './audits-edit-dialog.component';

/**
 * Modifier un plan d'audit : l'auditeur responsable se change dans l'annuaire
 * en lecture restreinte (ADR 0091), et ne part au serveur que s'il change.
 */
describe('AuditsEditDialogComponent', () => {
  let fixture: ComponentFixture<AuditsEditDialogComponent>;
  let component: AuditsEditDialogComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsEditDialogComponent>>;
  let directory: jasmine.SpyObj<DirectoryService>;

  const ALICE: DirectoryUser = { id: 'i1', accountId: 'u1', displayName: 'Alice Martin', email: 'alice@exemple.test', active: true };
  const BRUNO: DirectoryUser = { id: 'i2', accountId: 'u2', displayName: 'Bruno Petit', email: 'bruno@exemple.test', active: true };

  const plan = (over: Partial<AuditPlanResponse> = {}): AuditPlanResponse => ({
    id: 'p1', reference: 'AUD-2026-0001', tenantId: 't', title: 'Audit interne', type: 'INTERNAL',
    status: 'PLANNED', leadAuditorId: 'u1', leadAuditorName: 'Alice Martin', createdAt: '', updatedAt: '',
    checklist: [], findings: [], ...over
  });

  async function render(p: AuditPlanResponse): Promise<void> {
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['updatePlan']);
    svc.updatePlan.and.returnValue(of(p));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AuditsEditDialogComponent>>('MatDialogRef', ['close']);
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.getByAccount.and.callFake(id =>
      [ALICE, BRUNO].some(u => u.accountId === id)
        ? of([ALICE, BRUNO].find(u => u.accountId === id)!)
        : throwError(() => ({ status: 404 })));
    directory.search.and.returnValue(of({ content: [ALICE, BRUNO], page: 0, size: 20, totalElements: 2, totalPages: 1 }));

    await TestBed.configureTestingModule({
      declarations: [AuditsEditDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: DirectoryService, useValue: directory },
        { provide: MAT_DIALOG_DATA, useValue: { plan: p } as AuditsEditDialogData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditsEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const picker = (): UserPickerComponent =>
    fixture.debugElement.query(By.directive(UserPickerComponent)).componentInstance as UserPickerComponent;

  it('montre l\'auditeur en place par son nom d\'annuaire', async () => {
    await render(plan());
    expect(component.form.getRawValue().leadAuditorId).toBe('u1');
    expect(picker().selected?.displayName).toBe('Alice Martin');
  });

  it('corriger le titre ne renvoie pas l\'auditeur', async () => {
    await render(plan());
    component.form.patchValue({ title: 'Audit corrigé' });

    component.submit();

    const body = svc.updatePlan.calls.mostRecent().args[1];
    expect(body.title).toBe('Audit corrigé');
    expect('leadAuditorId' in body).toBeFalse();
    expect('leadAuditorName' in body).toBeFalse();
  });

  it('réattribuer l\'audit envoie le compte et le NOM d\'annuaire du collègue', async () => {
    await render(plan());
    picker().onSelected({ option: { value: BRUNO } } as never);

    component.submit();

    expect(svc.updatePlan).toHaveBeenCalledWith('p1', jasmine.objectContaining({
      leadAuditorId: 'u2', leadAuditorName: 'Bruno Petit'
    }));
  });

  it('un auditeur sorti de l\'annuaire garde sa place, sans identifiant affiché', async () => {
    await render(plan({ leadAuditorId: 'kc-0042-ancien', leadAuditorName: undefined }));
    expect(component.form.getRawValue().leadAuditorId).toBe('kc-0042-ancien');
    expect(picker().unknownSelection).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('kc-0042-ancien');
  });
});
