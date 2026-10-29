import { Location } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Écran « accès réservé » : la page demandée existe, mais le rôle de
 * l'utilisateur ne l'ouvre pas (garde {@link roleGuard}).
 *
 * <p>Il remplace l'erreur générique que la page affichait faute de droits
 * (« … n'a pas pu être chargé. Réessayer »), qui laissait croire à une panne et
 * invitait à réessayer en vain. L'écran dit la raison, rappelle l'adresse
 * demandée et oriente vers l'administrateur.</p>
 */
@Component({
  selector: 'qos-restricted-access',
  templateUrl: './restricted-access.component.html',
  styleUrls: ['./restricted-access.component.scss'],
  standalone: false
})
export class RestrictedAccessComponent implements OnInit {
  /** Adresse que l'utilisateur voulait ouvrir ; vide s'il arrive directement ici. */
  requestedPath = '';

  constructor(private readonly route: ActivatedRoute, private readonly location: Location) {}

  ngOnInit(): void {
    const from = this.route.snapshot.queryParamMap.get('from') ?? '';
    // Seule une adresse interne s'affiche : la valeur vient de l'URL et reste du texte.
    this.requestedPath = from.startsWith('/') ? from : '';
  }

  back(): void {
    this.location.back();
  }
}
