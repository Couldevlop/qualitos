import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../shared/shared.module';
import { ActivityFeedModule } from '../activity-feed/activity-feed.module';
import { ActivityFeedService } from '../activity-feed/activity-feed.service';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    const feed = jasmine.createSpyObj<ActivityFeedService>('ActivityFeedService', ['recent']);
    feed.recent.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [HomeComponent],
      imports: [SharedModule, ActivityFeedModule, NoopAnimationsModule],
      providers: [
        { provide: ActivityFeedService, useValue: feed },
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

  it('exposes four key stat cards with a semantic tone each', () => {
    expect(component.stats.length).toBe(4);
    component.stats.forEach(s =>
      expect(['accent', 'success', 'warn', 'danger']).toContain(s.tone));
    const stats = (fixture.nativeElement as HTMLElement).querySelectorAll('.qos-stat');
    expect(stats.length).toBe(4);
  });

  it('offers quick actions that point to real feature routes', () => {
    const routes = component.quickActions.map(a => a.route);
    expect(routes).toEqual(['/pdca', '/fives', '/nc', '/ishikawa']);
  });

  it('renders a hero with the platform slogan', () => {
    const title = (fixture.nativeElement as HTMLElement).querySelector('.home-hero__title');
    expect(title?.textContent).toContain('One platform');
  });

  it('provides stable trackBy helpers', () => {
    expect(component.trackByTitle(0, component.methods[0])).toBe('PDCA');
    expect(component.trackByRoute(0, component.quickActions[0])).toBe('/pdca');
  });
});
