import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';

/** Une entrée de la navigation de la console. */
export interface PlatformNavEntry {
  label: string;
  route: string;
  icon: string;
  /** Ce que l'entrée permet de faire, montré sous le libellé. */
  hint: string;
}

/**
 * La coquille de la console de l'éditeur.
 *
 * <p><b>Pourquoi une coquille distincte de celle du métier.</b> La demande était
 * que la console « ne contienne aucun module, rien que les éléments de
 * l'administration ». Filtrer la navigation du métier aurait laissé la coquille
 * du métier : son chip de synchronisation hors ligne, son sélecteur de densité,
 * ses cinquante entrées masquées mais présentes dans le gabarit, et surtout ses
 * gardes de module — qui interrogent les modules D'UN client, alors que
 * l'éditeur n'en est aucun. Une coquille à part est plus petite à lire et ne
 * peut pas, par construction, afficher un écran métier.
 *
 * <p>Elle est délibérément maigre : pas de tiroir repliable, pas de sections, pas
 * d'état à mémoriser. Six entrées au plus, visibles d'un coup d'œil. Ce n'est pas
 * un outil de travail quotidien pour des dizaines d'utilisateurs, c'est le poste
 * de pilotage de la plateforme.
 *
 * <p>Le repère visuel (bandeau « Console éditeur ») n'est pas décoratif : il doit
 * être impossible de croire qu'on regarde les données d'un client.
 */
@Component({
  selector: 'qos-platform-shell',
  templateUrl: './platform-shell.component.html',
  styleUrls: ['./platform-shell.component.scss'],
  standalone: false
})
export class PlatformShellComponent {

  /**
   * La navigation de la console.
   *
   * <p>Seules les entrées qui mènent à un écran livré y figurent. Lister les six
   * surfaces prévues produirait trois liens morts, et un lien mort dans une
   * console d'administration se lit comme une panne.
   */
  readonly entries: readonly PlatformNavEntry[] = [
    {
      label: $localize`:@@platform.nav.clients:Clients`,
      route: '/platform/clients',
      icon: 'apartment',
      hint: $localize`:@@platform.nav.clients.hint:Créer un client, son identité, ses modules`
    },
    {
      label: $localize`:@@platform.nav.firms:Cabinets`,
      route: '/platform/cabinets',
      icon: 'handshake',
      hint: $localize`:@@platform.nav.firms.hint:Cabinets partenaires, consultants, clients proposés`
    },
    {
      label: $localize`:@@platform.nav.audit:Piste d’audit`,
      route: '/platform/piste-audit',
      icon: 'receipt_long',
      hint: $localize`:@@platform.nav.audit.hint:Les actes de l’éditeur sur les dossiers clients`
    },
    {
      label: $localize`:@@platform.nav.modules:Catalogue des modules`,
      route: '/platform/modules',
      icon: 'widgets',
      hint: $localize`:@@platform.nav.modules.hint:Ce que la plateforme sait livrer`
    }
  ];

  readonly user$: Observable<AuthUser | null>;

  constructor(private readonly auth: AuthService) {
    this.user$ = this.auth.user();
  }

  initials(displayName: string): string {
    return displayName
      .split(/[\s._-]+/)
      .filter(part => part.length > 0)
      .slice(0, 2)
      .map(part => part[0].toUpperCase())
      .join('');
  }

  logout(): void {
    this.auth.logout();
  }
}
