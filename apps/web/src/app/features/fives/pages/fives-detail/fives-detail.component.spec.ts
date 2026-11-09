import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';
import { CircleProposalResponse } from '../../../circles/circles.types';
import { FivesService } from '../../fives.service';
import { FiveSAuditResponse, FiveSAuditStatus, FiveSItemResponse } from '../../fives.types';
import { FivesDetailComponent } from './fives-detail.component';

const AUDIT = '11111111-1111-4111-8111-111111111111';

function item(pillar: FiveSItemResponse['pillar'], score: number | null,
              note: string | null = null): FiveSItemResponse {
  return {
    id: 'i-' + pillar, auditId: AUDIT, pillar, score: score as number, note: note ?? undefined,
    createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z'
  } as FiveSItemResponse;
}

function audit(status: FiveSAuditStatus, items: FiveSItemResponse[] = []): FiveSAuditResponse {
  return {
    id: AUDIT, tenantId: 't1', zone: 'Atelier B', description: 'Audit mensuel',
    status, auditorId: 'u1', overallScore: 40,
    completedAt: status === 'COMPLETED' ? '2026-11-09T10:00:00Z' : undefined,
    createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z', items
  } as FiveSAuditResponse;
}

/**
 * « Un audit 5S peut alimenter un Cercle de Qualité. »
 *
 * <p>Ce que ces bancs tiennent : la commande n'apparaît que sur un audit
 * terminé et un pilier noté — le serveur refuse le reste, et une commande qui
 * échoue après coup vaut moins qu'une commande absente ; la fenêtre reçoit la
 * note relevée, qui est ce sur quoi l'idée sera défendue ; et l'audit
 * n'enregistre rien en retour, parce que c'est l'origine citée par l'idée qui
 * tient le lien.
 */
describe('FivesDetailComponent — porter un pilier au cercle', () => {

  let fixture: ComponentFixture<FivesDetailComponent>;
  let component: FivesDetailComponent;
  let svc: jasmine.SpyObj<FivesService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const idee: CircleProposalResponse = {
    id: 'p-1', circleId: 'c-1', title: 'Vider la zone', status: 'PROPOSED',
    originRef: 'Audit 5S zone Atelier B — SEIRI noté 2/10'
  } as CircleProposalResponse;

  async function monter(a: FiveSAuditResponse): Promise<void> {
    svc = jasmine.createSpyObj<FivesService>('FivesService',
      ['getAudit', 'scorePillar', 'proposeToCircle', 'startAudit', 'completeAudit',
       'cancelAudit', 'updateAudit', 'deleteAudit', 'analyzeAuditPhoto']);
    svc.getAudit.and.returnValue(of(a));
    svc.proposeToCircle.and.returnValue(of(idee));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [FivesDetailComponent],
      imports: [SharedModule, UiModule, PersonNameModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideDirectoryStub(),
        { provide: FivesService, useValue: svc },
        { provide: MatSnackBar, useValue: snack },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', AUDIT]]) } } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(FivesDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function ouvreLaFenetre(rendu: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(rendu)
    } as MatDialogRef<unknown>);
  }

  const seiri = () => component.pillars[0];

  // ---------- quand la commande existe ----------

  it('n’offre rien sur un audit en cours', async () => {
    await monter(audit('IN_PROGRESS', [item('SEIRI', 2)]));

    expect(component.canPropose(audit('IN_PROGRESS', [item('SEIRI', 2)]), 'SEIRI')).toBeFalse();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="cercle-SEIRI"]')).toBeNull();
  });

  it('n’offre rien sur un pilier qui n’a pas été noté', async () => {
    const a = audit('COMPLETED', [item('SEIRI', 2)]);
    await monter(a);

    expect(component.canPropose(a, 'SEITON')).toBeFalse();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="cercle-SEITON"]')).toBeNull();
  });

  it('offre la commande sur un pilier noté d’un audit terminé', async () => {
    const a = audit('COMPLETED', [item('SEIRI', 2)]);
    await monter(a);

    expect(component.canPropose(a, 'SEIRI')).toBeTrue();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="cercle-SEIRI"]')).not.toBeNull();
  });

  // ---------- ce qui part, et ce qui revient ----------

  it('passe à la fenêtre la zone, le pilier traduit et la note relevée', async () => {
    const a = audit('COMPLETED', [item('SEIRI', 2, 'Palettes vides dans l’allée')]);
    await monter(a);
    const ouvert = ouvreLaFenetre({ circleId: 'c-1', pillar: 'SEIRI', title: 'Vider la zone' });

    component.proposeToCircle(a, seiri());

    const data = (ouvert.calls.mostRecent().args[1] as { data: Record<string, unknown> }).data;
    expect(data).toEqual(jasmine.objectContaining({
      zone: 'Atelier B', pillar: 'SEIRI', score: 2, note: 'Palettes vides dans l’allée'
    }));
    expect(data['pillarLabel']).not.toBe('SEIRI');
    expect(svc.proposeToCircle).toHaveBeenCalledWith(AUDIT,
      { circleId: 'c-1', pillar: 'SEIRI', title: 'Vider la zone' });
    expect(snack.open).toHaveBeenCalled();
    expect(component.proposing['SEIRI']).toBeFalse();
  });

  it('n’enregistre rien sur l’audit : l’idée cite son origine', async () => {
    const a = audit('COMPLETED', [item('SEIRI', 2)]);
    await monter(a);
    ouvreLaFenetre({ circleId: 'c-1', pillar: 'SEIRI', title: 'Vider' });

    component.proposeToCircle(a, seiri());

    expect(svc.updateAudit).not.toHaveBeenCalled();
    expect(svc.scorePillar).not.toHaveBeenCalled();
  });

  it('ne dépose rien quand on renonce à la fenêtre', async () => {
    const a = audit('COMPLETED', [item('SEIRI', 2)]);
    await monter(a);
    ouvreLaFenetre(undefined);

    component.proposeToCircle(a, seiri());

    expect(svc.proposeToCircle).not.toHaveBeenCalled();
  });

  it('n’ouvre même pas la fenêtre sur un pilier non noté', async () => {
    const a = audit('COMPLETED', []);
    await monter(a);
    const ouvert = ouvreLaFenetre(undefined);

    component.proposeToCircle(a, seiri());

    expect(ouvert).not.toHaveBeenCalled();
  });

  it('dit l’échec et rend la main', async () => {
    const a = audit('COMPLETED', [item('SEIRI', 2)]);
    await monter(a);
    ouvreLaFenetre({ circleId: 'c-1', pillar: 'SEIRI', title: 'Vider' });
    svc.proposeToCircle.and.returnValue(throwError(() => ({ status: 409 })));

    component.proposeToCircle(a, seiri());

    expect(snack.open).toHaveBeenCalled();
    // La commande doit redevenir cliquable : un échec qui l'éteint oblige à
    // recharger la page.
    expect(component.proposing['SEIRI']).toBeFalse();
  });
});
