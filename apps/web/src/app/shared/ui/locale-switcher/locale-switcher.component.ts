import { Component } from '@angular/core';

/** Langue servie par la plateforme : code de locale et nom dans sa propre langue. */
export interface SupportedLocale {
  code: string;
  /** Nom écrit dans la langue elle-même : c'est ainsi qu'on le reconnaît. */
  label: string;
}

/**
 * Sélecteur de langue.
 *
 * Chaque locale est un **build distinct**, servi sous son propre préfixe
 * (`/fr/`, `/en/`, …) avec son propre `<base href>`. Changer de langue ne peut
 * donc pas être une navigation interne du routeur : il faut charger l'autre
 * application. C'est pourquoi ce composant fabrique une URL et provoque un
 * chargement complet, plutôt que d'appeler `Router.navigate`.
 *
 * Jusqu'ici rien ne permettait ce changement : le serveur déduisait la langue
 * de l'en-tête du navigateur, une seule fois, à la racine. Un utilisateur arrivé
 * en français y restait — sauf à modifier l'URL à la main.
 */
@Component({
  selector: 'qos-locale-switcher',
  templateUrl: './locale-switcher.component.html',
  styleUrls: ['./locale-switcher.component.scss'],
  standalone: false
})
export class LocaleSwitcherComponent {

  /** Les six langues construites et servies (cf. `angular.json` et nginx). */
  readonly locales: SupportedLocale[] = [
    { code: 'fr', label: 'Français' },
    { code: 'en', label: 'English' },
    { code: 'es', label: 'Español' },
    { code: 'ar', label: 'العربية' },
    { code: 'ja', label: '日本語' },
    { code: 'zh', label: '中文' }
  ];

  /** Locale source du projet, servie quand l'URL ne porte aucun préfixe connu. */
  private static readonly FALLBACK = 'fr';

  // Emplacement courant, isolé derrière des propriétés pour rester vérifiable
  // sans manipuler l'objet `location` du navigateur.
  currentPath = typeof location !== 'undefined' ? location.pathname : '/';
  currentSearch = typeof location !== 'undefined' ? location.search : '';
  currentHash = typeof location !== 'undefined' ? location.hash : '';

  /** Chargement de l'autre application ; substitué en test. */
  navigate: (url: string) => void = url => { location.href = url; };

  get currentCode(): string {
    const prefix = this.currentPath.split('/')[1];
    return this.locales.some(l => l.code === prefix)
      ? prefix
      : LocaleSwitcherComponent.FALLBACK;
  }

  get currentLabel(): string {
    return this.locales.find(l => l.code === this.currentCode)?.label ?? '';
  }

  /**
   * Recharge la même page dans la langue demandée. Le chemin, les paramètres de
   * requête et l'ancre sont conservés : changer de langue ne doit pas renvoyer
   * l'utilisateur à l'accueil.
   */
  switchTo(code: string): void {
    if (code === this.currentCode) {
      return;   // un rechargement complet pour rien
    }
    const segments = this.currentPath.split('/');
    const hasKnownPrefix = this.locales.some(l => l.code === segments[1]);
    const rest = hasKnownPrefix
      ? segments.slice(2).join('/')
      : segments.slice(1).join('/');
    this.navigate(`/${code}/${rest}${this.currentSearch}${this.currentHash}`);
  }
}
