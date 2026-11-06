import { HttpClient } from '@angular/common/http';
import { DOCUMENT, Inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { TenantIdentityResponse, TenantTheme } from '../../features/admin/admin.types';
import { ThemeService } from '../theme/theme.service';
import {
  hoverVariant, mix, parseHex, readableForeground, Rgb, softForeground, toHex, withAlpha
} from './brand-color';

/** La classe que porte `<html>` pour chacun des quatre thèmes livrés. */
const CLASSES: Record<TenantTheme, string> = {
  DENSE_LIGHT: 'qos-client-dense',
  AIRY_LIGHT: 'qos-client-airy',
  DARK: 'qos-client-dark',
  HIGH_CONTRAST: 'qos-client-contrast'
};

/** Les jetons que la marque d'un client peut surcharger, et eux seuls. */
const SURCHARGES = [
  '--qos-accent-bg',
  '--qos-accent-bg-hover',
  '--qos-accent-fg',
  '--qos-accent-bg-soft',
  '--qos-accent-fg-soft',
  '--qos-fg-link',
  '--qos-border-focus',
  '--qos-chart-1',
  '--qos-chart-2'
];

/**
 * La marque du client, posée sur l'application au chargement (spécification
 * console de l'éditeur, D4).
 *
 * <p><b>Ce que ce service fait, et ce qu'il ne fait pas.</b> Il lit
 * `/api/v1/branding` — qui tire le client du jeton, jamais du corps ni du
 * chemin (`CLAUDE.md` §18.2.2) — et traduit deux choses en CSS :
 *
 * <ul>
 *   <li>le <b>thème choisi</b> devient une classe sur {@code <html>}, qui règle
 *       la densité et le durcissement des contrastes ;</li>
 *   <li>les <b>couleurs choisies</b> surchargent une liste fermée de jetons
 *       {@code --qos-*} déjà existants. Pas d'éditeur de jetons libre : la
 *       surface reste éprouvable et le contraste vérifiable.</li>
 * </ul>
 *
 * <p><b>Pourquoi la préférence de la personne l'emporte.</b> Le thème du client
 * est le réglage de la maison, pas une contrainte. Quelqu'un qui a
 * explicitement choisi clair ou sombre garde son choix ; le thème du client ne
 * décide qu'à la place du système d'exploitation, pour la personne qui n'a rien
 * demandé. L'inverse — imposer le sombre à qui a choisi le clair — reviendrait
 * à défaire un réglage d'accessibilité depuis la console de l'éditeur.
 *
 * <p><b>Un échec ne casse rien.</b> Marque absente, serveur muet, couleur mal
 * formée : on garde les jetons du socle, lisibles par construction. Une
 * application qui refuserait de s'afficher faute de logo serait un défaut bien
 * pire que l'absence de logo.
 */
@Injectable({ providedIn: 'root' })
export class BrandingService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/branding`;
  private marque$: Observable<TenantIdentityResponse | null> | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly theme: ThemeService,
    @Inject(DOCUMENT) private readonly document: Document
  ) {}

  /**
   * Lit la marque du client courant et l'applique. Idempotent : les appels
   * suivants rendent la même lecture, sans refaire l'appel réseau.
   */
  load(): Observable<TenantIdentityResponse | null> {
    if (!this.marque$) {
      this.marque$ = this.http.get<TenantIdentityResponse>(this.endpoint).pipe(
        catchError(() => of(null)),
        tap(marque => this.apply(marque)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.marque$;
  }

  /** Le slogan du client, ou `null` : de quoi coiffer un en-tête sans le forcer. */
  slogan(): Observable<string | null> {
    return this.load().pipe(map(marque => marque?.slogan?.trim() || null));
  }

  /**
   * Pose le thème et les couleurs sur le document. Public pour être exerçable
   * seule, sans réseau.
   */
  apply(marque: TenantIdentityResponse | null): void {
    const html = this.document.documentElement;
    Object.values(CLASSES).forEach(classe => html.classList.remove(classe));
    SURCHARGES.forEach(jeton => html.style.removeProperty(jeton));
    if (!marque) {
      return;
    }

    const theme = CLASSES[marque.themeCode] ? marque.themeCode : 'AIRY_LIGHT';
    html.classList.add(CLASSES[theme]);
    // Le sombre du client n'est qu'un défaut : il ne touche pas la personne qui
    // a déjà choisi. `ThemeService` tranche, c'est lui qui connaît ce choix.
    this.theme.applyOrganisationDefault(theme === 'DARK' ? 'dark' : 'light');

    const sombre = this.theme.currentMode() === 'dark';
    const principale = parseHex(marque.primaryColor);
    if (principale) {
      this.poser(html, principale, sombre);
    }
    const secondaire = parseHex(marque.secondaryColor);
    if (secondaire) {
      html.style.setProperty('--qos-chart-2', toHex(secondaire));
    }
  }

  private poser(html: HTMLElement, couleur: Rgb, sombre: boolean): void {
    const doux = softForeground(couleur, sombre);
    const regle = (jeton: string, valeur: string) => html.style.setProperty(jeton, valeur);
    regle('--qos-accent-bg', toHex(couleur));
    regle('--qos-accent-bg-hover', toHex(hoverVariant(couleur, sombre)));
    // Le libellé posé SUR la couleur : le serveur garantit la lisibilité de la
    // couleur sur la page, pas celle du texte sur la couleur.
    regle('--qos-accent-fg', toHex(readableForeground(couleur)));
    regle('--qos-accent-bg-soft', withAlpha(couleur, sombre ? 0.18 : 0.12));
    regle('--qos-accent-fg-soft', toHex(doux));
    regle('--qos-fg-link', toHex(doux));
    regle('--qos-border-focus', toHex(sombre ? mix(couleur, { r: 255, g: 255, b: 255 }, 0.2) : couleur));
    regle('--qos-chart-1', toHex(couleur));
  }
}
