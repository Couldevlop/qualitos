import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { FiveWhysService } from '../../five-whys.service';
import { FiveWhysAnalysis } from '../../five-whys.types';

/**
 * Liste des analyses des 5 Pourquoi.
 *
 * <p>Chaque analyse part d'une non-conformité : la liste affiche donc d'abord sa
 * référence et le problème énoncé, puis dit d'un coup d'œil si la cause racine a
 * été conclue — c'est la seule question qui compte devant un tableau d'analyses.
 */
@Component({
  selector: 'qos-five-whys-list',
  templateUrl: './five-whys-list.component.html',
  styleUrls: ['./five-whys-list.component.scss'],
  standalone: false
})
export class FiveWhysListComponent implements OnInit {

  readonly columns = ['nc', 'problem', 'depth', 'rootCause'];

  analyses: FiveWhysAnalysis[] = [];
  loading = false;

  constructor(
    private readonly service: FiveWhysService,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.service.list().subscribe({
      next: page => { this.analyses = page.content; this.loading = false; },
      error: () => {
        this.loading = false;
        this.snack.open(
          $localize`:@@fivewhys.list-failed:Impossible de charger les analyses.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  open(analysis: FiveWhysAnalysis): void {
    this.router.navigate(['/five-whys', analysis.id]);
  }
}
