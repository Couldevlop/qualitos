import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';

/**
 * Page affichée pour une adresse inconnue, dans le cadre de l'application.
 *
 * <p>Auparavant une adresse erronée renvoyait sans un mot vers l'accueil : un
 * lien périmé ou mal recopié passait inaperçu. La page dit ce qui s'est passé,
 * rappelle l'adresse demandée et propose les deux sorties utiles.</p>
 */
@Component({
  selector: 'qos-not-found',
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.scss'],
  standalone: false
})
export class NotFoundComponent implements OnInit {
  requestedPath = '';

  constructor(private readonly router: Router, private readonly location: Location) {}

  ngOnInit(): void {
    this.requestedPath = this.router.url;
  }

  back(): void {
    this.location.back();
  }
}
