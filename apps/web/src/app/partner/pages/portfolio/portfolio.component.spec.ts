import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { PartnerService } from '../../../core/partner/partner.service';
import { PartnerConsultant, PartnerEngagement } from '../../../core/partner/partner.types';
import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { PortfolioComponent } from './portfolio.component';

/**
 * Le portefeuille d'un consultant.
 *
 * <p>Ce que ces tests tiennent, dans l'ordre d'importance :
 *
 * <ul>
 *   <li>le rattachement précède le chargement — sinon une personne bien
 *       inscrite par son cabinet voit un portefeuille vide à sa première
 *       connexion et conclut qu'on ne lui a rien accordé ;</li>
 *   <li>« pas de fiche » ne se dit pas comme une panne — c'est une inscription
 *       qui manque, et le message doit désigner le cabinet ;</li>
 *   <li>une liste vide dit pourquoi : un accès prend fin à sa date, sans
 *       préavis et sans qu'on l'ait retiré.</li>
 * </ul>
 */
describe('PortfolioComponent', () => {
  let fixture: ComponentFixture<PortfolioComponent>;
  let component: PortfolioComponent;

  const fiche: PartnerConsultant = {
    id: 'c-1', firmId: 'f-1', displayName: 'Claire Martin',
    email: 'claire@qualiconseil.fr', active: true, linked: true
  };

  const accord = (over: Partial<PartnerEngagement> = {}): PartnerEngagement => ({
    id: 'e-1', tenantId: 't-1', tenantName: 'Clinique du Parc',
    firmId: 'f-1', firmName: 'QualiConseil',
    scope: 'READ', status: 'GRANTED',
    startsOn: '2026-11-01', endsOn: '2026-12-31',
    purpose: 'Préparation ISO 9001', openToday: true,
    proposedAt: '2026-10-20T10:00:00Z', decidedAt: '2026-10-22T09:00:00Z', revokedAt: null,
    ...over
  });

  const partner = jasmine.createSpyObj<PartnerService>('PartnerService',
    ['linkAccount', 'myPortfolio']);

  beforeEach(async () => {
    partner.linkAccount.and.returnValue(of(fiche));
    partner.myPortfolio.and.returnValue(of([accord()]));
    partner.linkAccount.calls.reset();
    partner.myPortfolio.calls.reset();

    await TestBed.configureTestingModule({
      declarations: [PortfolioComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [{ provide: PartnerService, useValue: partner }]
    }).compileComponents();

    fixture = TestBed.createComponent(PortfolioComponent);
    component = fixture.componentInstance;
  });

  const texte = (): string => fixture.nativeElement.textContent as string;
  const noeud = (test: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-test="${test}"]`);

  // ---------- rattachement ----------

  it('rattache le compte AVANT de charger le portefeuille', () => {
    fixture.detectChanges();

    expect(partner.linkAccount).toHaveBeenCalledTimes(1);
    expect(partner.myPortfolio).toHaveBeenCalledTimes(1);
    expect(component.unlinked).toBeFalse();
  });

  it('dit que l’inscription revient au cabinet quand aucune fiche n’attend le compte', () => {
    partner.linkAccount.and.returnValue(of(null));
    fixture.detectChanges();

    expect(component.unlinked).toBeTrue();
    expect(noeud('non-rattache')).not.toBeNull();
    expect(texte()).toContain('Demandez à votre cabinet de vous inscrire');
    // Et ce n'est pas une erreur : rien ne doit s'afficher en alerte.
    expect(noeud('erreur')).toBeNull();
  });

  it('n’affiche pas le message d’inscription quand le rattachement a échoué', () => {
    // Un échec de rattachement et une absence de fiche ne se confondent pas :
    // le premier est une panne, le second un geste qui reste à faire.
    partner.linkAccount.and.returnValue(throwError(() => new Error('coupure')));
    fixture.detectChanges();

    expect(component.unlinked).toBeFalse();
    expect(noeud('erreur')).not.toBeNull();
    expect(noeud('non-rattache')).toBeNull();
    expect(partner.myPortfolio).not.toHaveBeenCalled();
  });

  // ---------- la liste ----------

  it('montre le client, l’objet et le terme de chaque mission', () => {
    fixture.detectChanges();

    const carte = fixture.nativeElement.querySelector('.mission') as HTMLElement;
    expect(carte.textContent).toContain('Clinique du Parc');
    expect(carte.textContent).toContain('Préparation ISO 9001');
    // Le terme est affiché par le pipe `date`, donc dans la locale du banc
    // (en-US) et non en français : on vérifie qu'il EST rendu, pas sa forme.
    expect(carte.textContent).toContain('31');
    expect(carte.textContent).toContain('2026');
  });

  it('retombe sur l’identifiant du client quand son nom est absent', () => {
    partner.myPortfolio.and.returnValue(of([accord({ tenantName: null })]));
    fixture.detectChanges();

    expect(texte()).toContain('t-1');
  });

  it('distingue l’intervention de la simple consultation', () => {
    fixture.detectChanges();
    expect(component.scopeLabel(accord())).toBe('Consultation');
    expect(component.scopeLabel(accord({ scope: 'INTERVENE' }))).toBe('Intervention');

    // La distinction est aussi portée visuellement : pouvoir écrire chez un
    // client n'est pas la même chose que le consulter.
    partner.myPortfolio.and.returnValue(of([accord({ scope: 'INTERVENE' })]));
    const autre = TestBed.createComponent(PortfolioComponent);
    autre.detectChanges();
    expect(autre.nativeElement.querySelector('.mission__portee--ecriture')).not.toBeNull();
  });

  it('explique une liste vide par la fin de terme, pas par un retrait', () => {
    partner.myPortfolio.and.returnValue(of([]));
    fixture.detectChanges();

    expect(noeud('vide')).not.toBeNull();
    expect(texte()).toContain('sans préavis');
  });

  it('rend l’erreur du serveur et vide la liste', () => {
    fixture.detectChanges();
    partner.myPortfolio.and.returnValue(throwError(() => new Error('indisponible')));

    component.reload();
    fixture.detectChanges();

    expect(component.engagements).toEqual([]);
    expect(noeud('erreur')).not.toBeNull();
  });

  it('recharge sur demande', () => {
    fixture.detectChanges();
    component.reload();

    expect(partner.myPortfolio).toHaveBeenCalledTimes(2);
    // Le rattachement, lui, ne se rejoue pas : il est fait une fois à
    // l'ouverture, et le bouton ne doit pas servir à le retenter en boucle.
    expect(partner.linkAccount).toHaveBeenCalledTimes(1);
  });

  it('n’écrit rien : aucune autre route du service n’est appelée', () => {
    // Un consultant ne décide ni de son cabinet ni de ses accès. Si cet écran
    // se met un jour à accorder quelque chose, c'est ici que ça se verra.
    fixture.detectChanges();
    const appels = Object.keys(partner)
      .filter(cle => (partner as unknown as Record<string, jasmine.Spy>)[cle]?.calls);
    expect(appels.sort()).toEqual(['linkAccount', 'myPortfolio']);
  });
});
