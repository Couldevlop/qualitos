import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';

import { ThemeMode, ThemePreference, ThemeService } from '../../../core/theme/theme.service';
import { ThemeToggleComponent } from './theme-toggle.component';

/** Le choix du thème : trois boutons radio, l'actif est annoncé et marqué. */
describe('ThemeToggleComponent', () => {
  let fixture: ComponentFixture<ThemeToggleComponent>;
  let preference: BehaviorSubject<ThemePreference>;
  let theme: jasmine.SpyObj<ThemeService>;

  beforeEach(async () => {
    preference = new BehaviorSubject<ThemePreference>('system');
    theme = jasmine.createSpyObj<ThemeService>('ThemeService', ['preference', 'mode', 'setPreference']);
    theme.preference.and.returnValue(preference.asObservable());
    theme.mode.and.returnValue(new BehaviorSubject<ThemeMode>('light').asObservable());
    theme.setPreference.and.callFake(p => preference.next(p));

    await TestBed.configureTestingModule({
      declarations: [ThemeToggleComponent],
      providers: [{ provide: ThemeService, useValue: theme }]
    }).compileComponents();
    fixture = TestBed.createComponent(ThemeToggleComponent);
    fixture.detectChanges();
  });

  const boutons = (): HTMLButtonElement[] =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));

  it('annonce la préférence active', () => {
    expect(boutons().map(b => b.getAttribute('aria-checked'))).toEqual(['false', 'true', 'false']);
  });

  it('change de préférence au clic', () => {
    boutons()[2].click();
    fixture.detectChanges();

    expect(theme.setPreference).toHaveBeenCalledWith('dark');
    expect(boutons()[2].classList).toContain('qos-theme-toggle__btn--active');
  });
});
