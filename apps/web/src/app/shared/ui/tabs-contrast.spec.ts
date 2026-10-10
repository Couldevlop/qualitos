import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTabsModule } from '@angular/material/tabs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

/**
 * Le nom de l'onglet où l'on se trouve reste lisible, dans les deux thèmes et
 * dans TOUS ses états — au repos, avec le focus (juste après le clic) et
 * survolé. Mesuré sur les styles réels (thème Material + styles.scss), pas sur
 * leur lecture : c'est la couleur calculée par le navigateur qui compte.
 */
@Component({
  template: `
    <mat-tab-group>
      <mat-tab label="Risques"><p>contenu</p></mat-tab>
      <mat-tab label="Opportunités"><p>contenu</p></mat-tab>
    </mat-tab-group>`,
  standalone: false
})
class OngletsComponent {}

describe('Onglets — lisibilité de l’onglet actif', () => {

  let fixture: ComponentFixture<OngletsComponent>;
  const root = document.documentElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [OngletsComponent],
      imports: [MatTabsModule, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(OngletsComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    root.classList.remove('qos-theme-dark', 'qos-theme-light');
  });

  function actif(): HTMLElement {
    return (fixture.nativeElement as HTMLElement).querySelector('.mdc-tab--active') as HTMLElement;
  }

  function couleurLibelle(): string {
    return getComputedStyle(actif().querySelector('.mdc-tab__text-label') as HTMLElement).color;
  }

  /** La couleur que le jeton --qos-fg-primary prend dans le thème courant. */
  function textePrincipal(): string {
    const sonde = document.createElement('span');
    sonde.style.color = 'var(--qos-fg-primary)';
    document.body.appendChild(sonde);
    const c = getComputedStyle(sonde).color;
    sonde.remove();
    return c;
  }

  for (const theme of ['qos-theme-light', 'qos-theme-dark']) {
    it(`garde le libellé à la couleur du texte principal, même avec le focus (${theme})`, () => {
      root.classList.add(theme);
      expect(couleurLibelle()).withContext('au repos').toBe(textePrincipal());

      actif().focus();
      expect(document.activeElement).toBe(actif());
      expect(couleurLibelle()).withContext('avec le focus').toBe(textePrincipal());
    });
  }
});
