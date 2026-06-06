import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConsentsService } from '../../consents.service';
import { ConsentSource, ConsentStatus, ConsentView } from '../../consents.types';
import { ConsentsWithdrawDialogComponent } from '../consents-withdraw-dialog/consents-withdraw-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-consents-detail',
  templateUrl: './consents-detail.component.html',
  styleUrls: ['./consents-detail.component.scss'],
  standalone: false
})
export class ConsentsDetailComponent implements OnInit {

  consent$!: Observable<ConsentView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly withdrawTooltipTerminal = $localize`:@@consents.detail.withdraw-tooltip-terminal:Statut terminal — retrait impossible`;
  readonly withdrawTooltipAllowed = $localize`:@@consents.detail.withdraw-tooltip-allowed:Art. 7§3 RGPD`;
  readonly notFilledLabel = $localize`:@@consents.detail.not-filled:(non renseignée)`;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: ConsentsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.consent$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID regex on path id, mock-id fallback.
        if (!UUID_REGEX.test(id) && !id.startsWith('cons-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[consents-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  withdraw(c: ConsentView): void {
    // OWASP A04 — only GRANTED + active consents can be withdrawn
    if (c.status !== 'GRANTED') {
      this.snack.open($localize`:@@consents.detail.not-active:Ce consentement n'est plus actif — retrait impossible.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConsentsWithdrawDialogComponent, {
      data: { consent: c }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  sourceLabel(s: ConsentSource): string {
    return ({
      WEB_FORM: $localize`:@@consents.source.web-form:Formulaire web`,
      MOBILE_APP: $localize`:@@consents.source.mobile-app:Application mobile`,
      EMAIL: $localize`:@@consents.source.email:E-mail`,
      PAPER: $localize`:@@consents.source.paper:Papier`,
      PHONE: $localize`:@@consents.source.phone:Téléphone`,
      API: $localize`:@@consents.source.api:API`,
      IMPORT: $localize`:@@consents.source.import:Import`,
      OTHER: $localize`:@@consents.source.other:Autre`
    })[s];
  }
  statusBadge(s: ConsentStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
