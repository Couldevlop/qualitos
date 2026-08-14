import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { ProcedureCreateDialogComponent } from '../procedure-create-dialog/procedure-create-dialog.component';
import { StandardsService } from '../../standards.service';
import { AdoptionResponse, StandardSummary } from '../../standards.types';

/** Ce que la liste montre du catalogue : tout, mes procédures, ou les normes livrées. */
export type CatalogScope = 'ALL' | 'OWNED' | 'PLATFORM';

@Component({
  selector: 'qos-standards-list',
  templateUrl: './standards-list.component.html',
  styleUrls: ['./standards-list.component.scss'],
  standalone: false
})
export class StandardsListComponent implements OnInit {

  readonly catalogCols = ['code', 'fullName', 'family', 'status', 'cycle', 'actions'];
  readonly adoptCols = ['code', 'status', 'scope', 'body', 'target'];

  catalog$!: Observable<StandardSummary[]>;
  adoptions$!: Observable<AdoptionResponse[]>;
  adopting?: string;

  /**
   * Le catalogue mêle désormais soixante normes livrées et les quelques
   * procédures du tenant : sans ce filtre, ces dernières se cherchent à l'œil
   * dans une liste où elles sont noyées.
   */
  readonly scope$ = new BehaviorSubject<CatalogScope>('ALL');

  constructor(
    private readonly svc: StandardsService,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    const all$ = this.svc.listCatalog().pipe(map(p => p.content), shareReplay(1));
    this.catalog$ = combineLatest([all$, this.scope$]).pipe(
      map(([items, scope]) => items.filter(s => this.matches(s, scope))),
      shareReplay(1)
    );
    this.adoptions$ = this.svc.listAdoptions().pipe(map(p => p.content), shareReplay(1));
  }

  private matches(s: StandardSummary, scope: CatalogScope): boolean {
    if (scope === 'OWNED') return s.owned;
    if (scope === 'PLATFORM') return !s.owned;
    return true;
  }

  setScope(scope: CatalogScope): void {
    this.scope$.next(scope);
  }

  /**
   * Un référentiel naît d'une procédure de la GED, jamais d'un formulaire vierge :
   * c'est la procédure qui lui donne son code, son titre et sa version, et sans
   * elle il n'auditerait rien d'opposable.
   */
  createFromProcedure(): void {
    this.dialog.open(ProcedureCreateDialogComponent, { autoFocus: 'dialog' })
      .afterClosed()
      .subscribe(created => {
        if (created) this.load();
      });
  }

  open(a: AdoptionResponse): void {
    this.router.navigate(['/standards/adoptions', a.id]);
  }

  adopt(s: StandardSummary): void {
    this.adopting = s.id;
    this.svc.adopt({ standardId: s.id }).subscribe({
      next: a => {
        this.adopting = undefined;
        this.snack.open($localize`:@@standards.list.adopt-success:${s.code}:code: adopté — roadmap générée`, $localize`:@@common.ok:OK`, { duration: 2500 });
        this.router.navigate(['/standards/adoptions', a.id]);
      },
      error: err => {
        this.adopting = undefined;
        const msg = err?.status === 409
          ? $localize`:@@standards.list.adopt-conflict:Norme déjà adoptée`
          : $localize`:@@standards.list.adopt-error:Échec de l'adoption`;
        this.snack.open(msg, $localize`:@@common.close:Fermer`, { duration: 3000 });
        this.load();
      }
    });
  }
}
