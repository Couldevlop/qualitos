import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AiSystemsService } from '../../ai-systems.service';
import { AiSystemView } from '../../ai-systems.types';
import {
  AiSystemFormData, AiSystemFormDialogComponent, parseUuidList, uuidList
} from './ai-system-form-dialog.component';

describe('AiSystemFormDialogComponent', () => {
  let fixture: ComponentFixture<AiSystemFormDialogComponent>;
  let component: AiSystemFormDialogComponent;
  let svc: jasmine.SpyObj<AiSystemsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AiSystemFormDialogComponent, AiSystemView | undefined>>;
  let user: AuthUser | null;

  const UUID_A = '33333333-3333-4333-8333-333333333333';
  const UUID_B = '44444444-4444-4444-8444-444444444444';

  const view = (over: Partial<AiSystemView> = {}): AiSystemView => ({
    id: 'id-1', tenantId: 't-1', reference: 'AISYS-TRIAGE', name: 'Triage urgences',
    description: null, providerName: null, intendedPurpose: 'Priorisation',
    riskClassification: 'LIMITED', role: 'DEPLOYER', generalPurpose: false, status: 'DRAFT',
    conformityAssessmentEvidenceUrl: null, ceMarkingNumber: null,
    humanOversightDescription: null, transparencyMeasures: null, dataGovernanceNotes: null,
    linkedDpiaId: null, linkedProcessingActivityIds: [], linkedAutomatedDecisionIds: [],
    effectiveFrom: null, effectiveTo: null, withdrawalReason: null,
    createdByUserId: 'u-1', createdAt: '2026-07-01T09:00:00Z', updatedAt: '2026-07-01T09:00:00Z',
    prohibited: false, requiresConformityAssessment: false, requiresTransparency: true,
    ...over
  });

  async function build(data: AiSystemFormData): Promise<void> {
    await TestBed.configureTestingModule({
      declarations: [AiSystemFormDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AiSystemsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(AiSystemFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Remplit le minimum exigé par le serveur pour une création. */
  function fillMinimal(): void {
    component.form.patchValue({
      reference: 'AISYS-TRIAGE',
      name: 'Triage urgences',
      intendedPurpose: 'Priorisation des passages aux urgences'
    });
  }

  beforeEach(() => {
    svc = jasmine.createSpyObj<AiSystemsService>('AiSystemsService', ['draft', 'edit']);
    svc.draft.and.returnValue(of(view()));
    svc.edit.and.returnValue(of(view({ name: 'Modifié' })));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AiSystemFormDialogComponent, AiSystemView | undefined>>(
      'MatDialogRef', ['close']);
    user = {
      userId: 'u-1', tenantId: 't-1', displayName: 'Demo', roles: ['quality_manager']
    };
  });

  // ---- Création ---------------------------------------------------------------

  it('refuse d\'envoyer un formulaire incomplet et le signale', async () => {
    await build({ mode: 'create' });

    component.submit();

    expect(svc.draft).not.toHaveBeenCalled();
    expect(component.error).toContain('Corrigez les champs');
    expect(component.form.controls.name.touched).toBeTrue();
  });

  it('impose le format de référence attendu par le serveur', async () => {
    await build({ mode: 'create' });
    fillMinimal();

    component.form.controls.reference.setValue('minuscules');
    expect(component.form.controls.reference.hasError('pattern')).toBeTrue();

    component.form.controls.reference.setValue('AISYS_TRIAGE-2');
    expect(component.form.controls.reference.valid).toBeTrue();
  });

  it('crée un brouillon avec l\'auteur issu de la session, jamais saisi', async () => {
    await build({ mode: 'create' });
    fillMinimal();

    component.submit();

    expect(svc.draft).toHaveBeenCalledTimes(1);
    const body = svc.draft.calls.mostRecent().args[0];
    expect(body.reference).toBe('AISYS-TRIAGE');
    expect(body.createdByUserId).toBe('u-1');
    expect(dialogRef.close).toHaveBeenCalledWith(view());
  });

  it('convertit les champs laissés vides en null plutôt qu\'en chaîne vide', async () => {
    await build({ mode: 'create' });
    fillMinimal();

    component.submit();

    const body = svc.draft.calls.mostRecent().args[0];
    expect(body.description).toBeNull();
    expect(body.providerName).toBeNull();
    expect(body.conformityAssessmentEvidenceUrl).toBeNull();
    expect(body.linkedDpiaId).toBeNull();
    expect(body.linkedProcessingActivityIds).toEqual([]);
  });

  it('refuse de créer sans session valide', async () => {
    user = null;
    await build({ mode: 'create' });
    fillMinimal();

    component.submit();

    expect(svc.draft).not.toHaveBeenCalled();
    expect(component.error).toContain('Session expirée');
  });

  it('remonte un message sûr quand le serveur refuse la création', async () => {
    svc.draft.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    await build({ mode: 'create' });
    fillMinimal();

    component.submit();

    expect(component.error).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  // ---- Validation des rattachements ------------------------------------------

  it('valide l\'URL de preuve de conformité comme le domaine serveur', async () => {
    await build({ mode: 'create' });
    const control = component.form.controls.conformityAssessmentEvidenceUrl;

    control.setValue('ftp://exemple.tld/preuve');
    expect(control.hasError('pattern')).toBeTrue();

    control.setValue('https://exemple.tld/preuve');
    expect(control.valid).toBeTrue();

    // Vide reste accepté : la preuve n'est exigée qu'à la mise en service.
    control.setValue('');
    expect(control.valid).toBeTrue();
  });

  it('rejette une liste d\'identifiants dont un seul n\'est pas un UUID', () => {
    expect(uuidList(new FormControl(''))).toBeNull();
    expect(uuidList(new FormControl(`${UUID_A}\n${UUID_B}`))).toBeNull();
    expect(uuidList(new FormControl(`${UUID_A}, pas-un-uuid`)))
      .toEqual({ uuidList: ['pas-un-uuid'] });
  });

  it('accepte les séparateurs qu\'un utilisateur emploie réellement', () => {
    expect(parseUuidList(` ${UUID_A} , ${UUID_B}\n;${UUID_A} `))
      .toEqual([UUID_A, UUID_B, UUID_A]);
    expect(parseUuidList('')).toEqual([]);
  });

  it('envoie les rattachements sous forme de tableau', async () => {
    await build({ mode: 'create' });
    fillMinimal();
    component.form.controls.linkedProcessingActivityIds.setValue(`${UUID_A}\n${UUID_B}`);

    component.submit();

    expect(svc.draft.calls.mostRecent().args[0].linkedProcessingActivityIds)
      .toEqual([UUID_A, UUID_B]);
  });

  // ---- Aide à la décision -----------------------------------------------------

  it('n\'affiche aucun prérequis pour un risque minimal', async () => {
    await build({ mode: 'create' });
    component.form.controls.riskClassification.setValue('MINIMAL_OR_NO');
    expect(component.requirements()).toEqual([]);
    expect(component.prohibited()).toBeFalse();
  });

  it('suit en direct les prérequis du haut risque', async () => {
    await build({ mode: 'create' });
    component.form.controls.riskClassification.setValue('HIGH');
    expect(component.requirements().map(r => r.satisfied)).toEqual([false, false, false]);

    component.form.patchValue({
      conformityAssessmentEvidenceUrl: 'https://exemple.tld/preuve',
      humanOversightDescription: 'Arrêt manuel',
      transparencyMeasures: 'Bandeau'
    });
    expect(component.requirements().every(r => r.satisfied)).toBeTrue();
  });

  it('avertit qu\'une pratique interdite ne pourra jamais être exploitée', async () => {
    await build({ mode: 'create' });
    component.form.controls.riskClassification.setValue('UNACCEPTABLE');
    fixture.detectChanges();

    expect(component.prohibited()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Pratique interdite (Art. 5)');
  });

  // ---- Édition ----------------------------------------------------------------

  it('recharge la fiche et verrouille la référence en édition', async () => {
    await build({
      mode: 'edit',
      system: view({
        description: 'Modèle interne', providerName: 'MedAI',
        linkedProcessingActivityIds: [UUID_A, UUID_B]
      })
    });

    expect(component.editing).toBeTrue();
    expect(component.form.controls.reference.disabled).toBeTrue();
    expect(component.form.controls.reference.value).toBe('AISYS-TRIAGE');
    expect(component.form.controls.linkedProcessingActivityIds.value)
      .toBe(`${UUID_A}\n${UUID_B}`);
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Référence non modifiable après création');
  });

  it('met à jour sans jamais renvoyer la référence ni l\'auteur', async () => {
    await build({ mode: 'edit', system: view() });
    component.form.controls.name.setValue('Modifié');

    component.submit();

    expect(svc.edit).toHaveBeenCalledTimes(1);
    const [id, body] = svc.edit.calls.mostRecent().args;
    expect(id).toBe('id-1');
    expect(body.name).toBe('Modifié');
    const raw = body as unknown as Record<string, unknown>;
    expect(raw['reference']).toBeUndefined();
    expect(raw['createdByUserId']).toBeUndefined();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('ferme sans rien envoyer à l\'annulation', async () => {
    await build({ mode: 'create' });

    component.cancel();

    expect(svc.draft).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(undefined);
  });

  it('identifie les lignes de prérequis pour un rendu stable', async () => {
    await build({ mode: 'create' });
    expect(component.trackByKey(0, { key: 'transparency', label: 'x', satisfied: false }))
      .toBe('transparency');
  });
});
