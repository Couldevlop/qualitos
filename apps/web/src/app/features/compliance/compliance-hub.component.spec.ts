import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ComplianceHubComponent } from './compliance-hub.component';

/** Toutes les routes GRC qui DOIVENT rester atteignables depuis le hub. */
const EXPECTED_ROUTES = [
  '/ai-qms', '/ai-conformity', '/ai-incidents', '/ai-eudb', '/fria', '/ai-pmm',
  '/ropa', '/consents', '/subject-requests', '/privacy-notices', '/dpia',
  '/dpo-appointments', '/retention', '/cross-border', '/processor-agreements',
  '/breaches', '/automated-decisions',
  '/nis2-measures', '/cyber-incidents'
];

describe('ComplianceHubComponent', () => {
  let component: ComplianceHubComponent;
  let fixture: ComponentFixture<ComplianceHubComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ComplianceHubComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ComplianceHubComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('exposes exactly the three GRC domains', () => {
    expect(component.domains.map(d => d.key)).toEqual(['ai', 'gdpr', 'nis2']);
  });

  it('covers ALL 19 GRC routes across the domain tiles (no route dropped)', () => {
    const routes = component.domains.flatMap(d => d.views.map(v => v.route));
    expect(routes.length).toBe(EXPECTED_ROUTES.length);
    EXPECTED_ROUTES.forEach(r => expect(routes).withContext(r).toContain(r));
  });

  it('renders one tile per domain', () => {
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('.grc-card');
    expect(cards.length).toBe(3);
  });

  it('starts collapsed and toggles a single domain open', () => {
    expect(component.expandedKey).toBeNull();
    component.toggle('ai');
    expect(component.isExpanded('ai')).toBeTrue();
    expect(component.isExpanded('gdpr')).toBeFalse();
  });

  it('collapses the domain when toggled twice (accordion behaviour)', () => {
    component.toggle('gdpr');
    expect(component.isExpanded('gdpr')).toBeTrue();
    component.toggle('gdpr');
    expect(component.expandedKey).toBeNull();
  });

  it('switches the open domain when another is toggled', () => {
    component.toggle('ai');
    component.toggle('nis2');
    expect(component.isExpanded('ai')).toBeFalse();
    expect(component.isExpanded('nis2')).toBeTrue();
  });

  it('reveals the sub-view links once a tile is expanded', () => {
    // OnPush : on déclenche l'expansion via un vrai clic (événement) pour que
    // la détection de changements rende les sous-vues.
    const heads = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.grc-card__head');
    heads[2].click(); // NIS 2 = 3ᵉ tuile
    fixture.detectChanges();
    const links = (fixture.nativeElement as HTMLElement).querySelectorAll('.grc-card--expanded .grc-view');
    expect(links.length).toBe(2);
  });

  it('provides stable trackBy keys', () => {
    expect(component.trackByKey(0, component.domains[0])).toBe('ai');
    expect(component.trackByRoute(0, component.domains[0].views[0])).toBe('/ai-qms');
  });
});
