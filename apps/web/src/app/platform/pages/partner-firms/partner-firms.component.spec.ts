import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { PartnerFirmPage, PartnerService } from '../../../core/partner/partner.service';
import {
  PartnerConsultant, PartnerEngagement, PartnerFirm
} from '../../../core/partner/partner.types';
import { TenantPage } from '../../../features/admin/admin.types';
import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { ClientsService } from '../../clients.service';
import { PartnerFirmsComponent } from './partner-firms.component';

/**
 * « Cabinets partenaires », côté éditeur.
 *
 * <p>Ce que ces tests tiennent, dans l'ordre d'importance :
 *
 * <ul>
 *   <li><b>proposer n'ouvre rien</b> — c'est la frontière de l'écran, et la
 *       seule chose qui empêche l'éditeur de s'inviter chez un client ;</li>
 *   <li>désactiver un cabinet passe par une confirmation, parce que le geste
 *       ferme d'un coup tous les accès de tous ses consultants ;</li>
 *   <li>un consultant jamais connecté se distingue d'un consultant rattaché —
 *       sinon on ne sait pas si l'inscription a pris ;</li>
 *   <li>un repère mal formé ne part pas au serveur.</li>
 * </ul>
 */
describe('PartnerFirmsComponent', () => {
  let fixture: ComponentFixture<PartnerFirmsComponent>;
  let component: PartnerFirmsComponent;

  const cabinet = (over: Partial<PartnerFirm> = {}): PartnerFirm => ({
    id: 'f-1', slug: 'qualiconseil', name: 'QualiConseil',
    registrationNumber: '812 345 678', contactEmail: 'contact@qualiconseil.fr',
    contactPhone: null, active: true, consultantCount: 2, grantedCount: 1,
    createdAt: '2026-10-01T09:00:00Z', ...over
  });

  const fiche = (over: Partial<PartnerConsultant> = {}): PartnerConsultant => ({
    id: 'c-1', firmId: 'f-1', displayName: 'Claire Martin',
    email: 'claire@qualiconseil.fr', active: true, linked: true, ...over
  });

  const accord = (over: Partial<PartnerEngagement> = {}): PartnerEngagement => ({
    id: 'e-1', tenantId: 't-1', tenantName: null, firmId: 'f-1', firmName: 'QualiConseil',
    scope: 'READ', status: 'PROPOSED', startsOn: null, endsOn: null,
    purpose: null, openToday: false,
    proposedAt: '2026-10-20T10:00:00Z', decidedAt: null, revokedAt: null, ...over
  });

  const page = (firms: PartnerFirm[]): PartnerFirmPage =>
    ({ content: firms, totalElements: firms.length });

  const clientsPage: TenantPage = {
    content: [
      { id: 't-1', slug: 'clinique', name: 'Clinique du Parc', plan: 'STARTER',
        active: true, createdAt: '', updatedAt: '' },
      { id: 't-9', slug: 'ferme', name: 'Client fermé', plan: 'STARTER',
        active: false, createdAt: '', updatedAt: '' }
    ],
    totalElements: 2, totalPages: 1, number: 0, size: 100
  };

  const partner = jasmine.createSpyObj<PartnerService>('PartnerService',
    ['listFirms', 'createFirm', 'setFirmActive', 'listConsultants', 'addConsultant',
      'setConsultantActive', 'portfolioOf', 'propose']);
  const clients = jasmine.createSpyObj<ClientsService>('ClientsService', ['list']);
  const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
  const snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

  const confirmeAvec = (reponse: boolean): void => {
    dialog.open.and.returnValue({ afterClosed: () => of(reponse) } as MatDialogRef<unknown>);
  };

  beforeEach(async () => {
    partner.listFirms.and.returnValue(of(page([cabinet()])));
    partner.createFirm.and.returnValue(of(cabinet({ id: 'f-2', slug: 'audit-plus' })));
    partner.setFirmActive.and.returnValue(of(cabinet({ active: false })));
    partner.listConsultants.and.returnValue(of([fiche()]));
    partner.addConsultant.and.returnValue(of(fiche({ id: 'c-2', linked: false })));
    partner.setConsultantActive.and.returnValue(of(fiche({ active: false })));
    partner.portfolioOf.and.returnValue(of([accord()]));
    partner.propose.and.returnValue(of(accord({ id: 'e-2' })));
    clients.list.and.returnValue(of(clientsPage));
    [partner.listFirms, partner.createFirm, partner.setFirmActive, partner.listConsultants,
      partner.addConsultant, partner.setConsultantActive, partner.portfolioOf, partner.propose,
      clients.list, dialog.open, snack.open].forEach(spy => spy.calls.reset());
    confirmeAvec(true);

    await TestBed.configureTestingModule({
      declarations: [PartnerFirmsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: PartnerService, useValue: partner },
        { provide: ClientsService, useValue: clients },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PartnerFirmsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const noeud = (test: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-test="${test}"]`);

  // ---------- la frontière ----------

  it('dit, à l’écran, que l’éditeur n’ouvre aucun accès', () => {
    const texte = fixture.nativeElement.textContent as string;
    expect(texte).toContain('Vous n’ouvrez aucun accès');
  });

  it('proposer n’accorde rien : seule la proposition part', () => {
    component.basculerDetail(cabinet());
    component.proposeTenantId = 't-1';
    component.proposeScope = 'INTERVENE';
    component.proposePurpose = '  audit blanc ';

    component.proposer();

    expect(partner.propose).toHaveBeenCalledWith('f-1', {
      tenantId: 't-1', scope: 'INTERVENE', purpose: 'audit blanc'
    });
    // Et le message le redit au moment du geste : la frontière doit être
    // énoncée là où elle pourrait être oubliée.
    expect(snack.open.calls.mostRecent().args[0] as string)
      .toContain('Aucun accès n’est ouvert');
  });

  it('ne propose que des clients actifs', () => {
    component.basculerDetail(cabinet());
    expect(component.clients.map(c => c.id)).toEqual(['t-1']);
  });

  it('ne propose rien sans client choisi', () => {
    component.basculerDetail(cabinet());
    component.proposeTenantId = '';

    expect(component.proposeValid).toBeFalse();
    component.proposer();
    expect(partner.propose).not.toHaveBeenCalled();
  });

  // ---------- créer un cabinet ----------

  it('refuse un repère mal formé', () => {
    component.ouvrirCreation();
    component.draft.name = 'Audit Plus';

    component.draft.slug = 'Audit Plus';
    expect(component.firmDraftValid).toBeFalse();
    component.draft.slug = 'a';
    expect(component.firmDraftValid).toBeFalse();
    component.draft.slug = 'audit-plus';
    expect(component.firmDraftValid).toBeTrue();

    // Et un nom d'une lettre non plus.
    component.draft.name = 'A';
    expect(component.firmDraftValid).toBeFalse();
  });

  it('crée le cabinet et le place en tête, formulaire refermé', () => {
    component.ouvrirCreation();
    component.draft = {
      slug: 'audit-plus', name: '  Audit Plus  ', registrationNumber: '',
      contactEmail: '', contactPhone: ''
    };

    component.creer();

    expect(partner.createFirm).toHaveBeenCalledWith({
      slug: 'audit-plus', name: 'Audit Plus',
      registrationNumber: null, contactEmail: null, contactPhone: null
    });
    expect(component.firms[0].id).toBe('f-2');
    expect(component.creating).toBeFalse();
  });

  it('ne crée rien quand le brouillon est invalide', () => {
    component.ouvrirCreation();
    component.creer();
    expect(partner.createFirm).not.toHaveBeenCalled();
  });

  // ---------- activer, désactiver ----------

  it('demande confirmation avant de désactiver un cabinet', () => {
    component.basculer(cabinet({ active: true }));

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(partner.setFirmActive).toHaveBeenCalledWith('f-1', false);
  });

  it('ne désactive rien si la confirmation est refusée', () => {
    confirmeAvec(false);
    component.basculer(cabinet({ active: true }));
    expect(partner.setFirmActive).not.toHaveBeenCalled();
  });

  it('réactive sans confirmation : le geste n’enlève rien', () => {
    partner.setFirmActive.and.returnValue(of(cabinet({ active: true })));
    component.basculer(cabinet({ active: false }));

    expect(dialog.open).not.toHaveBeenCalled();
    expect(partner.setFirmActive).toHaveBeenCalledWith('f-1', true);
  });

  // ---------- déplier ----------

  it('charge consultants et portefeuille à l’ouverture, et se referme au second clic', () => {
    component.basculerDetail(cabinet());
    expect(partner.listConsultants).toHaveBeenCalledWith('f-1');
    expect(partner.portfolioOf).toHaveBeenCalledWith('f-1');
    expect(component.openFirmId).toBe('f-1');

    component.basculerDetail(cabinet());
    expect(component.openFirmId).toBeNull();
  });

  it('ne recharge pas la liste des clients à chaque ouverture', () => {
    component.basculerDetail(cabinet());
    component.basculerDetail(cabinet());
    component.basculerDetail(cabinet());

    expect(clients.list).toHaveBeenCalledTimes(1);
  });

  it('reste utilisable quand la liste des clients ne se charge pas', () => {
    clients.list.and.returnValue(throwError(() => new Error('coupure')));
    component.basculerDetail(cabinet());

    expect(component.clients).toEqual([]);
    // La proposition est indisponible, mais rien n'est signalé en alerte : le
    // reste de l'écran fonctionne.
    expect(component.proposeValid).toBeFalse();
  });

  // ---------- consultants ----------

  it('refuse d’inscrire sans nom ni courriel plausible', () => {
    component.basculerDetail(cabinet());

    component.consultantName = 'C';
    component.consultantEmail = 'claire@x.fr';
    expect(component.consultantValid).toBeFalse();

    component.consultantName = 'Claire Martin';
    component.consultantEmail = 'claire';
    expect(component.consultantValid).toBeFalse();

    component.consultantEmail = 'claire@x.fr';
    expect(component.consultantValid).toBeTrue();
  });

  it('inscrit un consultant, vide le formulaire et suit le compteur', () => {
    component.basculerDetail(cabinet());
    component.consultantName = ' Claire Martin ';
    component.consultantEmail = ' claire@qualiconseil.fr ';

    component.inscrire();

    expect(partner.addConsultant).toHaveBeenCalledWith('f-1', {
      displayName: 'Claire Martin', email: 'claire@qualiconseil.fr'
    });
    expect(component.consultantName).toBe('');
    expect(component.firms[0].consultantCount).toBe(3);
  });

  it('distingue un consultant rattaché d’un consultant jamais connecté', () => {
    partner.listConsultants.and.returnValue(of([fiche({ linked: false })]));
    component.basculerDetail(cabinet());
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent as string).toContain('Jamais connecté');
  });

  it('suspend un consultant et décrémente le compteur', () => {
    component.basculerDetail(cabinet());
    component.basculerConsultant(fiche({ active: true }));

    expect(partner.setConsultantActive).toHaveBeenCalledWith('f-1', 'c-1', false);
    expect(component.firms[0].consultantCount).toBe(1);
  });

  it('ne descend pas le compteur sous zéro', () => {
    partner.listFirms.and.returnValue(of(page([cabinet({ consultantCount: 0 })])));
    component.charger();
    component.basculerDetail(cabinet());
    component.basculerConsultant(fiche({ active: true }));

    expect(component.firms[0].consultantCount).toBe(0);
  });

  // ---------- libellés ----------

  it('n’annonce pas un accord hors période comme ouvert', () => {
    expect(component.statusLabel(accord({ status: 'GRANTED', openToday: true }))).toBe('Ouvert');
    expect(component.statusLabel(accord({ status: 'GRANTED', openToday: false })))
      .toBe('Accordé, hors période');
    expect(component.statusLabel(accord())).toBe('En attente du client');
    expect(component.statusLabel(accord({ status: 'DECLINED' }))).toBe('Refusé');
    expect(component.statusLabel(accord({ status: 'REVOKED' }))).toBe('Repris');
  });

  it('nomme la portée en clair', () => {
    expect(component.scopeLabel('READ')).toBe('Consultation');
    expect(component.scopeLabel('INTERVENE')).toBe('Intervention');
  });

  it('nomme le client par son nom, et retombe sur son identifiant sinon', () => {
    component.basculerDetail(cabinet());
    expect(component.clientName('t-1')).toBe('Clinique du Parc');
    expect(component.clientName('t-inconnu')).toBe('t-inconnu');
  });

  // ---------- pannes ----------

  it('signale l’échec du chargement et propose de réessayer', () => {
    partner.listFirms.and.returnValue(throwError(() => new Error('coupure')));
    component.charger();
    fixture.detectChanges();

    expect(component.failed).toBeTrue();
    expect(noeud('echec')).not.toBeNull();
  });

  it('libère les boutons quand une écriture échoue', () => {
    partner.createFirm.and.returnValue(throwError(() => new Error('conflit')));
    component.ouvrirCreation();
    component.draft.slug = 'audit-plus';
    component.draft.name = 'Audit Plus';

    component.creer();

    expect(component.busy).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('dit, quand la liste est vide, ce qu’est un cabinet', () => {
    partner.listFirms.and.returnValue(of(page([])));
    component.charger();
    fixture.detectChanges();

    expect(noeud('vide')).not.toBeNull();
    expect(fixture.nativeElement.textContent as string).toContain('accès daté');
  });
});
