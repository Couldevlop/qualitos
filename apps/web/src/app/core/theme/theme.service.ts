
import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ThemePreference = 'light' | 'dark' | 'system';
export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'qos.theme';
const MEDIA_QUERY = '(prefers-color-scheme: dark)';

/**
 * Service de thème — persiste la préférence utilisateur dans localStorage,
 * applique la classe `qos-theme-{light|dark}` sur <html>, suit
 * `prefers-color-scheme` en mode "system".
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {

  private readonly preference$ = new BehaviorSubject<ThemePreference>('system');
  private readonly mode$ = new BehaviorSubject<ThemeMode>('light');
  private mediaList: MediaQueryList | null = null;
  private readonly mediaListener = (event: MediaQueryListEvent): void => {
    if (this.preference$.value === 'system') {
      this.applyMode(event.matches ? 'dark' : 'light');
    }
  };

  constructor(@Inject(DOCUMENT) private readonly document: Document) {
    const stored = this.readStoredPreference();
    this.preference$.next(stored);
    this.mediaList = this.document.defaultView?.matchMedia(MEDIA_QUERY) ?? null;
    this.attachMediaListener();
    this.applyMode(this.resolveMode(stored));
  }

  preference(): Observable<ThemePreference> { return this.preference$.asObservable(); }
  currentPreference(): ThemePreference { return this.preference$.value; }

  mode(): Observable<ThemeMode> { return this.mode$.asObservable(); }
  currentMode(): ThemeMode { return this.mode$.value; }

  setPreference(preference: ThemePreference): void {
    if (preference === this.preference$.value) return;
    this.preference$.next(preference);
    this.persistPreference(preference);
    this.applyMode(this.resolveMode(preference));
  }

  /**
   * Le mode que le client a choisi pour sa maison — appliqué seulement à qui
   * n'a rien demandé.
   *
   * <p>La préférence enregistrée par la personne l'emporte toujours : un thème
   * sombre posé depuis la console de l'éditeur ne doit pas défaire le choix
   * clair de quelqu'un qui en a besoin pour lire. Le thème du client remplace
   * donc le système d'exploitation, rien de plus — et il n'est pas persisté,
   * pour que le jour où le client en change, l'écran suive.
   */
  applyOrganisationDefault(mode: ThemeMode): void {
    if (this.hasStoredPreference()) {
      return;
    }
    this.applyMode(mode);
  }

  /** Inverse light ↔ dark (utile pour un toggle simple). */
  toggle(): void {
    const next: ThemePreference = this.currentMode() === 'dark' ? 'light' : 'dark';
    this.setPreference(next);
  }

  private resolveMode(preference: ThemePreference): ThemeMode {
    if (preference === 'system') {
      return this.mediaList?.matches ? 'dark' : 'light';
    }
    return preference;
  }

  private applyMode(mode: ThemeMode): void {
    const html = this.document.documentElement;
    html.classList.remove('qos-theme-light', 'qos-theme-dark');
    html.classList.add('qos-theme-' + mode);
    this.mode$.next(mode);
  }

  private attachMediaListener(): void {
    if (!this.mediaList) return;
    if (typeof this.mediaList.addEventListener === 'function') {
      this.mediaList.addEventListener('change', this.mediaListener);
    }
  }

  /**
   * Vrai si la personne a elle-même tranché.
   *
   * <p>Se fier à `currentPreference() === 'system'` ne suffirait pas : « rien
   * d'enregistré » et « j'ai choisi de suivre le système » ont la même valeur
   * en mémoire alors que ce sont deux situations différentes — la seconde est
   * un choix, et un choix ne se remplace pas.
   */
  private hasStoredPreference(): boolean {
    try {
      const stored = this.document.defaultView?.localStorage?.getItem(STORAGE_KEY);
      return stored === 'light' || stored === 'dark' || stored === 'system';
    } catch (_) {
      return false;
    }
  }

  private readStoredPreference(): ThemePreference {
    try {
      const stored = this.document.defaultView?.localStorage?.getItem(STORAGE_KEY);
      if (stored === 'light' || stored === 'dark' || stored === 'system') return stored;
    } catch (_) { /* SSR / disabled storage */ }
    return 'system';
  }

  private persistPreference(preference: ThemePreference): void {
    try {
      this.document.defaultView?.localStorage?.setItem(STORAGE_KEY, preference);
    } catch (_) { /* ignore */ }
  }
}
