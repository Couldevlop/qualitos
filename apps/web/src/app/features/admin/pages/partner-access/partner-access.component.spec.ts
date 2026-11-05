import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { PartnerService } from '../../../../core/partner/partner.service';
import { PartnerEngagement } from '../../../../core/partner/partner.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PartnerAccessComponent } from './partner-access.component';

/**
 * « Cabinets », côté client.
 *
 * <p>Ce que ces tests tiennent, dans l'ordre d'importance :
 *
 * <ul>
 *   <li><b>un accès ne s'accorde pas sans terme</b>, et un terme passé n'en est
 *       pas un — c'est l'invariant de sécurité de l'écran, et il doit résister
 *       au jour où quelqu'un trouvera le champ pénible ;</li>
 *   <li>reprendre un accès ouvert passe par une confirmation — on reprend
 *       parfois au milieu d'une mission, et jamais par mégarde ;</li>
 *   <li>refuser et reprendre ne se confondent pas, jusque dans l'historique ;</li>
 *   <li>un accord accordé hors de ses dates n'est pas annoncé comme ouvert.</li>
 * </ul>
 */
describe('PartnerAccessComponent', () => {
  let fixture: ComponentFixture<PartnerAccessComponent>;
  let component: PartnerAccessComponent;

  const accord = (over: Partial<PartnerEngagement> = {}): PartnerEngagement => ({
    id: 'e-1', tenantId: 't-1', tenantName: 'Clinique du Parc',
    firmId: 'f-1', firmName: 'QualiConseil',
    scope: 'READ', status: 'PROPOSED',
    startsOn: null, endsOn: null,
    purpose: 'Préparation ISO 9001', openToday: false,
    proposedAt: '2026-10-20T10:00:00Z', decidedAt: null, revokedAt: null,
    ...over
  });

  const partner = jasmine.createSpyObj<PartnerService>('PartnerService',
    ['myEngagements', 'grant', 'decline', 'revoke']);
  const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
  const snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

  const confirmeAvec = (reponse: boolean): void => {
    dialog.open.and.returnValue({ afterClosed: () => of(reponse) } as MatDialogRef<unknown>);
  };

  const demain = (): string => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
  };

  beforeEach(async () => {
    partner.myEngagements.and.returnValue(of([accord()]));
    partner.grant.and.returnValue(of(accord({ status: 'GRANTED', openToday: true })));
    partner.decline.and.returnValue(of(accord({ status: 'DECLINED' })));
    partner.revoke.and.returnValue(of(accord({ status: 'REVOKED' })));
    [partner.myEngagements, partner.grant, partner.decline, partner.revoke, dialog.open, snack.open]
      .forEach(spy => spy.calls.reset());
    confirmeAvec(true);

    await TestBed.configureTestingModule({
      declarations: [PartnerAccessComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: PartnerService, useValue: partner },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PartnerAccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const noeud = (test: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-test="${test}"]`);

  // ---------- les trois groupes ----------

  it('range les accords en attente, accordés, passés', () => {
    partner.myEngagements.and.returnValue(of([
      accord({ id: 'a', status: 'PROPOSED' }),
      accord({ id: 'b', status: 'GRANTED', startsOn: '2026-11-01', endsOn: '2026-12-31' }),
      accord({ id: 'c', status: 'DECLINED' }),
      accord({ id: 'd', status: 'REVOKED' })
    ]));
    component.charger();

    expect(component.pending.map(a => a.id)).toEqual(['a']);
    expect(component.granted.map(a => a.id)).toEqual(['b']);
    expect(component.history.map(a => a.id)).toEqual(['c', 'd']);
  });

  it('dit qu’aucun cabinet n’a été proposé, et par qui il le serait', () => {
    partner.myEngagements.and.returnValue(of([]));
    component.charger();
    fixture.detectChanges();

    expect(noeud('vide')).not.toBeNull();
    expect(fixture.nativeElement.textContent as string)
      .toContain('déclaré par l’éditeur de la plateforme');
  });

  // ---------- le terme, invariant de l'écran ----------

  it('propose un terme à trois mois, jamais vide', () => {
    component.ouvrirOctroi(accord());
    expect(component.draft.endsOn).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(component.draftValid).toBeTrue();
  });

  it('refuse d’accorder sans terme', () => {
    component.ouvrirOctroi(accord());
    component.draft.endsOn = '';

    expect(component.draftValid).toBeFalse();
    component.accorder(accord());
    expect(partner.grant).not.toHaveBeenCalled();
  });

  it('refuse un terme déjà passé ou qui tombe aujourd’hui', () => {
    component.ouvrirOctroi(accord());

    component.draft.endsOn = '2020-01-01';
    expect(component.draftValid).toBeFalse();

    // Aujourd'hui non plus : un accès qui se ferme le soir même n'a pas de sens
    // et masquerait une faute de saisie.
    component.draft.endsOn = new Date().toISOString().slice(0, 10);
    expect(component.draftValid).toBeFalse();

    component.draft.endsOn = demain();
    expect(component.draftValid).toBeTrue();
  });

  it('refuse une date illisible', () => {
    component.ouvrirOctroi(accord());
    component.draft.endsOn = 'pas-une-date';
    expect(component.draftValid).toBeFalse();
  });

  // ---------- accorder ----------

  it('accorde avec la portée, le terme et l’objet saisis', () => {
    const a = accord();
    component.ouvrirOctroi(a);
    component.draft.scope = 'INTERVENE';
    component.draft.endsOn = demain();
    component.draft.purpose = '  audit blanc  ';

    component.accorder(a);

    expect(partner.grant).toHaveBeenCalledWith('e-1', {
      scope: 'INTERVENE', endsOn: demain(), purpose: 'audit blanc'
    });
    // Le formulaire se referme : le laisser ouvert laisserait croire qu'on peut
    // encore retoucher un accès déjà accordé.
    expect(component.grantingId).toBeNull();
  });

  it('propose par défaut la portée que l’éditeur a demandée', () => {
    component.ouvrirOctroi(accord({ scope: 'INTERVENE' }));
    expect(component.draft.scope).toBe('INTERVENE');
  });

  it('envoie un objet vide comme absent, pas comme chaîne vide', () => {
    const a = accord();
    component.ouvrirOctroi(a);
    component.draft.endsOn = demain();
    component.draft.purpose = '   ';

    component.accorder(a);
    expect(partner.grant.calls.mostRecent().args[1].purpose).toBeNull();
  });

  it('remplace l’accord en place après l’octroi', () => {
    const a = accord();
    component.ouvrirOctroi(a);
    component.draft.endsOn = demain();
    component.accorder(a);

    expect(component.granted.length).toBe(1);
    expect(component.pending.length).toBe(0);
  });

  // ---------- refuser ----------

  it('refuse une proposition sans confirmation : rien ne s’ouvre', () => {
    component.refuser(accord());

    expect(partner.decline).toHaveBeenCalledWith('e-1');
    // Refuser ne retire aucun accès : il n'y a rien à confirmer.
    expect(dialog.open).not.toHaveBeenCalled();
    expect(component.history.length).toBe(1);
  });

  // ---------- reprendre ----------

  it('demande confirmation avant de reprendre un accès ouvert', () => {
    component.reprendre(accord({ status: 'GRANTED' }));

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(partner.revoke).toHaveBeenCalledWith('e-1');
  });

  it('ne reprend rien si la confirmation est refusée', () => {
    confirmeAvec(false);
    component.reprendre(accord({ status: 'GRANTED' }));

    expect(partner.revoke).not.toHaveBeenCalled();
  });

  // ---------- libellés ----------

  it('n’annonce pas un accord hors période comme ouvert', () => {
    expect(component.statusLabel(accord({ status: 'GRANTED', openToday: true })))
      .toBe('Ouvert');
    expect(component.statusLabel(accord({ status: 'GRANTED', openToday: false })))
      .toBe('Accordé, hors période');
  });

  it('distingue refusé de repris', () => {
    expect(component.statusLabel(accord({ status: 'DECLINED' }))).toBe('Refusé');
    expect(component.statusLabel(accord({ status: 'REVOKED' }))).toBe('Repris');
    expect(component.statusLabel(accord())).toBe('En attente de votre décision');
  });

  it('nomme la portée en clair', () => {
    expect(component.scopeLabel('READ')).toBe('Consultation');
    expect(component.scopeLabel('INTERVENE')).toBe('Intervention');
  });

  // ---------- pannes ----------

  it('signale l’échec du chargement et propose de réessayer', () => {
    partner.myEngagements.and.returnValue(throwError(() => new Error('coupure')));
    component.charger();
    fixture.detectChanges();

    expect(component.failed).toBeTrue();
    expect(noeud('echec')).not.toBeNull();
  });

  it('libère les boutons quand une décision échoue', () => {
    partner.decline.and.returnValue(throwError(() => new Error('refus serveur')));
    component.refuser(accord());

    expect(component.busyId).toBeNull();
    expect(snack.open).toHaveBeenCalled();
  });

  it('retombe sur l’identifiant du cabinet quand son nom est absent', () => {
    partner.myEngagements.and.returnValue(of([accord({ firmName: null })]));
    component.charger();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent as string).toContain('f-1');
  });
});
