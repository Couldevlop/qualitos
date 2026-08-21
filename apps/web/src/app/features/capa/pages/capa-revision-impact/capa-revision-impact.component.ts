import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { ProductsService } from '../../../products/products.service';
import { RevisionRequestView } from '../../../products/products.types';

/**
 * Ce que la clôture de cette CAPA a fait bouger — ou proposé de faire bouger —
 * dans le PFMEA et le control plan du produit.
 *
 * <p>L'encart n'apparaît que s'il y a quelque chose à montrer : un bloc vide sur
 * chaque fiche CAPA n'apprendrait rien et ferait descendre le reste. Le lien mène
 * à l'onglet du produit concerné, pas à la liste des produits — l'utilisateur
 * vient de lire une CAPA, pas de chercher un produit.
 */
@Component({
  selector: 'qos-capa-revision-impact',
  templateUrl: './capa-revision-impact.component.html',
  styleUrls: ['./capa-revision-impact.component.scss'],
  standalone: false
})
export class CapaRevisionImpactComponent implements OnInit {

  @Input() capaId = '';

  requests: RevisionRequestView[] = [];

  constructor(
    private readonly products: ProductsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (!this.capaId) return;
    // L'encart est un supplément d'information : son échec ne doit pas dégrader
    // la lecture du dossier CAPA lui-même.
    this.products.revisionRequestsForTrigger(this.capaId)
      .pipe(catchError(() => of([] as RevisionRequestView[])))
      .subscribe(requests => (this.requests = requests));
  }

  get visible(): boolean {
    return this.requests.length > 0;
  }

  summary(request: RevisionRequestView): string {
    if (request.field) return `${request.field} : ${request.from} → ${request.to}`;
    return request.targetType === 'PFMEA_ITEM_CREATE'
      ? $localize`:@@revision.create-pfmea:Créer une ligne de PFMEA`
      : $localize`:@@revision.create-line:Créer une ligne de control plan`;
  }

  open(request: RevisionRequestView): void {
    this.router.navigate(['/products', request.productId]);
  }
}
