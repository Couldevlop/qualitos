import { Component, Input, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { NcService } from '../../../nc/nc.service';
import { NcResponse } from '../../../nc/nc.types';

/**
 * Les non-conformités rattachées au produit, en deux blocs.
 *
 * <p>La séparation n'est pas cosmétique : les NC SANS mode de défaillance sont
 * l'information utile. Ce sont des défauts que l'analyse de risque n'explique
 * pas — donc, soit un mode manquant au PFMEA, soit un rattachement qu'on a
 * oublié de faire. Noyées dans la liste, elles ne se verraient jamais.
 */
@Component({
  selector: 'qos-product-nc-tab',
  templateUrl: './product-nc-tab.component.html',
  styleUrls: ['./product-nc-tab.component.scss'],
  standalone: false
})
export class ProductNcTabComponent implements OnInit {

  @Input() productId = '';

  readonly columns = ['reference', 'title', 'severity', 'status', 'detectedAt'];

  explained: NcResponse[] = [];
  unexplained: NcResponse[] = [];
  loading = false;

  constructor(
    private readonly nc: NcService,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : le patron
    // queueMicrotask ne vaut que pour un Subject poussé depuis une souscription.
    // Ici il produirait l'inverse — un flux synchrone rendrait la main avant la
    // micro-tâche, qui rallumerait le voyant sur une liste déjà chargée.
    this.loading = true;
    this.nc.listNcs(0, 100, { productId: this.productId }).subscribe({
      next: page => {
        this.explained = page.content.filter(item => !!item.fmeaItemId);
        this.unexplained = page.content.filter(item => !item.fmeaItemId);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open($localize`:@@product.nc-failed:Non-conformités indisponibles.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  get total(): number {
    return this.explained.length + this.unexplained.length;
  }

  open(item: NcResponse): void {
    this.router.navigate(['/nc', item.id]);
  }
}
