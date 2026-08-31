import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';

import { IconNoTranslateModule } from './icon-no-translate.module';

@Component({
  standalone: false,
  template: `
    <mat-icon>home</mat-icon>
    <mat-icon>chevron_left</mat-icon>
    <span class="material-symbols-outlined">inventory_2</span>
    <span class="material-icons">settings</span>
    <span class="libelle">Accueil</span>
  `
})
class HostComponent {}

/**
 * Les icônes Material échappent à la traduction automatique, le reste non.
 *
 * <p>Une icône Material s'écrit en LIGATURE : son nom EST son contenu textuel,
 * et la police le rend sous forme de dessin. Un traducteur de navigateur y voit
 * du texte, remplace `chevron_left` par « CHEVRON_GAUCHE », et la barre de
 * navigation se retrouve criblée de mots en capitales.
 *
 * <p>Le second banc est le plus important : il interdit la correction PARESSEUSE
 * qui consisterait à soustraire la page entière à la traduction. QualitOS sert
 * six langues, mais un lecteur qui en veut une septième a le droit de la
 * demander à son navigateur.
 */
describe('IconNoTranslateDirective', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [MatIconModule, IconNoTranslateModule]
    }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('pose translate="no" sur les icones balise ET sur les icones classe', () => {
    // Les deux formes existent dans l'application, et la seconde porte TOUTE la
    // barre de navigation — celle-la meme dont les noms de glyphes s'affichaient
    // en toutes lettres. Ne viser que `<mat-icon>` ne corrigeait rien la ou le
    // defaut se voyait le plus.
    const icons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll(
      'mat-icon, .material-symbols-outlined, .material-icons'));

    expect(icons.length).toBe(4);
    icons.forEach(icon => expect(icon.getAttribute('translate')).toBe('no'));
  });

  it('ne touche pas au texte de l\'interface, qui doit rester traduisible', () => {
    const label: HTMLElement = fixture.nativeElement.querySelector('.libelle');

    expect(label.getAttribute('translate')).toBeNull();
  });
});
