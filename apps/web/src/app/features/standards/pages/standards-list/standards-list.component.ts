import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { StandardsService } from '../../standards.service';
import { AdoptionResponse, StandardSummary } from '../../standards.types';

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

  constructor(
    private readonly svc: StandardsService,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.catalog$ = this.svc.listCatalog().pipe(map(p => p.content), shareReplay(1));
    this.adoptions$ = this.svc.listAdoptions().pipe(map(p => p.content), shareReplay(1));
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
