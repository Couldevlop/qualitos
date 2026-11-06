import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';

/**
 * La coquille de la console d'un cabinet.
 *
 * <p><b>Pourquoi une troisième coquille.</b> Un consultant n'est ni un
 * utilisateur de client ni l'éditeur. Son jeton ne porte aucun client : la
 * coquille métier, dont toutes les gardes interrogent « les modules de MON
 * client », n'a rien à lui montrer. Et la console de l'éditeur lui donnerait
 * une navigation d'administration de plateforme à laquelle il n'a aucun droit.
 *
 * <p>Elle est délibérément minuscule — un bandeau et une page. Un consultant ne
 * vient pas ici travailler : il vient choisir chez quel client entrer. Tout le
 * reste de son travail se fait dans l'espace de ce client, avec les écrans de
 * ce client.
 *
 * <p>Le bandeau dit le cabinet et non le produit : il doit être impossible de
 * croire qu'on regarde son propre espace.
 */
@Component({
  selector: 'qos-partner-shell',
  templateUrl: './partner-shell.component.html',
  styleUrls: ['./partner-shell.component.scss'],
  standalone: false
})
export class PartnerShellComponent {

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
