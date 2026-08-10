import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { AuditsService } from '../../audits.service';
import { AuditPlanningEntry, AuditType } from '../../audits.types';

/** Seuil d'alerte, aligné sur le rappel serveur (30 jours) : les deux doivent dire la même chose. */
const REMINDER_THRESHOLD_DAYS = 30;

/** Seuil « à préparer » : au-delà, l'échéance est connue mais rien ne presse. */
const SOON_THRESHOLD_DAYS = 60;

@Component({
  selector: 'qos-audits-planning',
  templateUrl: './audits-planning.component.html',
  styleUrls: ['./audits-planning.component.scss'],
  standalone: false
})
export class AuditsPlanningComponent implements OnInit {

  readonly displayedColumns = ['scheduledDate', 'countdown', 'title', 'type', 'standard', 'reminder'];

  /**
   * Le filtre porte sur les six types, mais la demande métier vise « interne /
   * externe » : ces deux-là sont donc en tête, avant les autres.
   */
  readonly types: { value: AuditType | ''; label: string }[] = [
    { value: '',              label: $localize`:@@common.all:Tous` },
    { value: 'INTERNAL',      label: $localize`:@@audits.type.internal:Interne` },
    { value: 'EXTERNAL',      label: $localize`:@@audits.type.external:Externe` },
    { value: 'SUPPLIER',      label: $localize`:@@audits.type.supplier:Fournisseur` },
    { value: 'LPA',           label: $localize`:@@audits.type.lpa:LPA (Layered Process Audit)` },
    { value: 'CERTIFICATION', label: $localize`:@@audits.type.certification:Certification` },
    { value: 'SURVEILLANCE',  label: $localize`:@@audits.type.surveillance:Surveillance` }
  ];

  readonly horizons = [30, 90, 180, 365];

  readonly typeFilter = new FormControl<AuditType | ''>('', { nonNullable: true });
  readonly horizonFilter = new FormControl<number>(90, { nonNullable: true });

  entries$!: Observable<AuditPlanningEntry[]>;

  /** Compteurs de l'en-tête, recalculés à chaque chargement. */
  overdueCount = 0;
  approachingCount = 0;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  constructor(
    private readonly svc: AuditsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.entries$ = combineLatest([
      this.typeFilter.valueChanges.pipe(startWith(this.typeFilter.value)),
      this.horizonFilter.valueChanges.pipe(startWith(this.horizonFilter.value))
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([type, horizon]) => this.svc.listPlanning(type || undefined, horizon).pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(
            err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
          // Liste vide EXPLICITE, et non `return []` : ce dernier ne ferait rien
          // émettre et la table garderait ses lignes précédentes à côté de la
          // bannière d'erreur — un planning périmé présenté comme à jour.
          return of([] as AuditPlanningEntry[]);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      map(entries => {
        this.overdueCount = entries.filter(e => e.overdue).length;
        this.approachingCount = entries.filter(
          e => !e.overdue && e.daysUntil <= REMINDER_THRESHOLD_DAYS).length;
        return entries;
      }),
      // refCount:false — sans quoi le *ngIf de chargement démonte le dernier
      // abonné et relance la requête en boucle.
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  /** Urgence d'une ligne : pilote la pastille et la couleur du décompte. */
  urgency(e: AuditPlanningEntry): 'overdue' | 'due' | 'soon' | 'later' {
    if (e.overdue) return 'overdue';
    if (e.daysUntil <= REMINDER_THRESHOLD_DAYS) return 'due';
    if (e.daysUntil <= SOON_THRESHOLD_DAYS) return 'soon';
    return 'later';
  }

  /**
   * Décompte lisible. Le retard s'écrit en positif précédé de « retard », plutôt
   * qu'en « J+-3 » : un signe négatif au milieu d'un tableau se lit mal et se
   * confond avec un tiret de valeur absente.
   */
  countdownLabel(e: AuditPlanningEntry): string {
    if (e.overdue) {
      const late = Math.abs(e.daysUntil);
      return late === 1
        ? $localize`:@@audits.planning.late-one:1 jour de retard`
        : $localize`:@@audits.planning.late-many:${late}:days: jours de retard`;
    }
    if (e.daysUntil === 0) return $localize`:@@audits.planning.today:Aujourd'hui`;
    if (e.daysUntil === 1) return $localize`:@@audits.planning.tomorrow:Demain`;
    return $localize`:@@audits.planning.in-days:Dans ${e.daysUntil}:days: jours`;
  }

  open(e: AuditPlanningEntry): void {
    this.router.navigate(['/audits', e.id]);
  }

  backToList(): void {
    this.router.navigate(['/audits']);
  }
}
