import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../shared/shared.module';
import { ActivityFeedModule } from '../activity-feed/activity-feed.module';
import { ActivityFeedService } from '../activity-feed/activity-feed.service';
import { AuditsService } from '../audits/audits.service';
import { FivesService } from '../fives/fives.service';
import { NcService } from '../nc/nc.service';
import { FiveSAuditResponse } from '../fives/fives.types';
import { NcStatistics } from '../nc/nc.types';
import { PdcaService } from '../pdca/pdca.service';
import { AuditPlanResponse } from '../audits/audits.types';
import { PdcaCycleResponse, SpringPage } from '../pdca/pdca.types';
import { HomeComponent } from './home.component';

/** Page Spring minimale : seul le total et le contenu comptent ici. */
function page<T>(content: Partial<T>[], totalElements = content.length): SpringPage<T> {
  return { content: content as T[], totalElements, totalPages: 1, number: 0, size: content.length };
}

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let pdca: jasmine.SpyObj<PdcaService>;
  let nc: jasmine.SpyObj<NcService>;
  let fives: jasmine.SpyObj<FivesService>;
  let audits: jasmine.SpyObj<AuditsService>;

  beforeEach(async () => {
    pdca = jasmine.createSpyObj<PdcaService>('PdcaService', ['listCycles']);
    // 3 cycles en Plan, 2 en Do, 1 en Check, 0 en Act : 6 cycles en cours.
    const parStatut: Record<string, number> = { PLAN: 3, DO: 2, CHECK: 1, ACT: 0 };
    pdca.listCycles.and.callFake((_p?: number, _s?: number, status?: string) =>
      of(page<PdcaCycleResponse>([], parStatut[status ?? ''] ?? 0)));
    nc = jasmine.createSpyObj<NcService>('NcService', ['statistics']);
    nc.statistics.and.returnValue(of({ open: 6 } as NcStatistics));
    fives = jasmine.createSpyObj<FivesService>('FivesService', ['listAudits']);
    fives.listAudits.and.returnValue(of(page<FiveSAuditResponse>([{ overallScore: 80 }, { overallScore: 91 }, {}])));
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['listPlans']);
    audits.listPlans.and.returnValue(of(page<AuditPlanResponse>([], 4)));

    const feed = jasmine.createSpyObj<ActivityFeedService>('ActivityFeedService', ['recent']);
    feed.recent.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [HomeComponent],
      imports: [SharedModule, ActivityFeedModule, NoopAnimationsModule],
      providers: [
        { provide: ActivityFeedService, useValue: feed },
        { provide: PdcaService, useValue: pdca },
        { provide: NcService, useValue: nc },
        { provide: FivesService, useValue: fives },
        { provide: AuditsService, useValue: audits },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('lists the five quality methods, all routed and available', () => {
    expect(component.methods.length).toBe(5);
    expect(component.methods.every(m => m.available && !!m.route)).toBeTrue();
    expect(component.methods.map(m => m.title))
      .toEqual(['PDCA', 'Ishikawa', '5S', 'DMAIC', jasmine.any(String)]);
  });

  it('renders one clickable method card per method', () => {
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('.home-method');
    expect(cards.length).toBe(5);
  });

  it('lit les quatre indicateurs dans les modules, sans chiffre écrit en dur', () => {
    let values: (string | null)[] = [];
    component.stats$.subscribe(s => (values = s.map(c => c.value)));
    // 3 + 2 + 1 + 0 cycles ouverts ; 6 NC internes ouvertes ; moyenne de 80 et 91
    // (l'audit sans score est ignoré) ; 4 audits planifiés.
    expect(values).toEqual(['6', '6', '86/100', '4']);
    expect(nc.statistics).toHaveBeenCalledWith('INTERNAL');
    expect(fives.listAudits).toHaveBeenCalledWith(0, HomeComponent.FIVES_SAMPLE, 'COMPLETED');
    expect(audits.listPlans).toHaveBeenCalledWith(0, 1, 'PLANNED');
    const stats = (fixture.nativeElement as HTMLElement).querySelectorAll('.qos-stat');
    expect(stats.length).toBe(4);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Valeurs indicatives');
  });

  it('chaque carte mène au module qui fournit la valeur', () => {
    const links = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a.home-stat'))
      .map(a => a.getAttribute('href'));
    expect(links).toEqual(['/pdca', '/nc/interne', '/fives', '/audits']);
  });

  it('affiche « — » pour un indicateur illisible, sans effacer les autres', () => {
    nc.statistics.and.returnValue(throwError(() => new Error('503')));
    fives.listAudits.and.returnValue(of(page<FiveSAuditResponse>([])));
    component.ngOnInit();
    fixture.detectChanges();
    let values: (string | null)[] = [];
    component.stats$.subscribe(s => (values = s.map(c => c.value)));
    expect(values).toEqual(['6', null, null, '4']);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('—');
    expect(text).toContain('Aucun audit terminé');
    expect(text).toContain('Indisponible');
  });

  it('offers quick actions that point to real feature routes', () => {
    const routes = component.quickActions.map(a => a.route);
    expect(routes).toEqual(['/pdca', '/fives', '/nc']);
  });

  it("ne propose plus de lancer une analyse de cause depuis l'accueil", () => {
    // Chercher une cause suppose un ecart constate : on part de la non-conformite,
    // qui ouvre l'Ishikawa ou les 5 Pourquoi sur son propre sujet. La carte de
    // methode reste, elle, pour consulter les diagrammes existants.
    const routes = component.quickActions.map(a => a.route);
    expect(routes).not.toContain('/ishikawa');
    expect(routes).not.toContain('/five-whys');
  });

  // Le titre s'adresse au lecteur dans sa langue ; le slogan de marque vient en
  // accroche, pas à la place du message.
  it('renders the value proposition as the heading', () => {
    const title = (fixture.nativeElement as HTMLElement).querySelector('.home-hero__title');
    expect(title?.textContent).toContain('méthodes');
  });

  it('keeps the brand slogan alongside, not instead of, the heading', () => {
    // Le slogan suit la langue du lecteur (la version anglaise reste celle du build en).
    const slogan = (fixture.nativeElement as HTMLElement).querySelector('.home-hero__slogan');
    expect(slogan?.textContent).toContain('Une plateforme');
  });

  it('shows short proofs rather than a wall of promises', () => {
    const proofs = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('.home-hero__proofs li');
    expect(proofs.length).toBe(4);
  });

  it('provides stable trackBy helpers', () => {
    expect(component.trackByTitle(0, component.methods[0])).toBe('PDCA');
    expect(component.trackByRoute(0, component.quickActions[0])).toBe('/pdca');
  });
});
