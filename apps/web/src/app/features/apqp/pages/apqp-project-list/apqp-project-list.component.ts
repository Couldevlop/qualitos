import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ApqpService } from '../../apqp.service';
import { ApqpProject, ApqpProjectType } from '../../apqp.types';
import {
  ApqpProjectDialogComponent, ApqpProjectDialogData
} from '../apqp-project-dialog/apqp-project-dialog.component';

/** Qui peut ouvrir, refondre ou supprimer un projet. Miroir du contrôle serveur. */
const ROLES_ECRITURE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/**
 * Les projets APQP du client.
 *
 * <p>C'est l'écran d'entrée du module : le cycle n'appartient plus au client mais
 * à un PROJET. Un même client mène de front un lancement de produit, un transfert
 * d'activité et l'ouverture d'un nouveau client — un cycle unique les mélangeait,
 * et le dossier PPAP qu'on en tirait n'était plus remettable à personne.
 *
 * <p>Le filtre porte sur le TYPE et non sur un statut : un projet APQP n'a pas
 * d'état propre, son avancement se lit dans ses compteurs de livrables.
 */
@Component({
  selector: 'qos-apqp-project-list',
  templateUrl: './apqp-project-list.component.html',
  styleUrls: ['./apqp-project-list.component.scss'],
  standalone: false
})
export class ApqpProjectListComponent implements OnInit {

  readonly displayedColumns = ['name', 'type', 'deliverables', 'ppap', 'createdAt'];

  readonly types: ApqpProjectType[] = ['NPI', 'TOW', 'NEW_CUSTOMER', 'OTHER'];

  readonly typeFilter = new FormControl<ApqpProjectType | ''>('');

  projects$!: Observable<ApqpProject[]>;

  // `deferredView` plutôt qu'un `BehaviorSubject` lu tel quel : ces états sont
  // poussés depuis `tap`/`finalize`, donc PENDANT la passe de détection — les
  // lire directement rendait un NG0100 sur une bannière qui apparaît et
  // disparaît dans le même cycle.
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Rafraîchissement : un `BehaviorSubject` émet tout de suite, un `Subject` non. */
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  readonly editable: boolean;

  constructor(
    private readonly service: ApqpService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_ECRITURE);
  }

  ngOnInit(): void {
    this.projects$ = combineLatest([
      this.typeFilter.valueChanges.pipe(startWith(this.typeFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([type]) => this.service.projects().pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(
            err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
          // `return []` renverrait un observable VIDE : RxJS convertit le
          // tableau en source, donc la liste n'émettrait RIEN et la table
          // garderait les lignes précédentes sous la bannière d'erreur.
          return of([] as ApqpProject[]);
        }),
        finalize(() => this.loadingState$.next(false)),
        map(projets => type ? projets.filter(p => p.type === type) : projets)
      )),
      // `refCount: true` referme la souscription quand la vue se détache, et la
      // rouvre à la réouverture : sans lui, une liste quittée puis rouverte
      // rejouerait indéfiniment la dernière page.
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  open(projet: ApqpProject): void {
    void this.router.navigate(['/apqp', projet.id]);
  }

  openCreate(): void {
    const ref = this.dialog.open(ApqpProjectDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      data: {} as ApqpProjectDialogData
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.createProject(saisie).subscribe({
        next: cree => void this.router.navigate(['/apqp', cree.id]),
        error: err => this.errorState$.next(safeErrorMessage(
          err, $localize`:@@apqp.failed:Opération impossible sur le cycle APQP.`))
      });
    });
  }

  /**
   * Supprime un projet, avec ce qu'il emporte dit dans la question même.
   *
   * <p>`confirm` natif plutôt qu'un dialogue de plus : la question est fermée, et
   * la perte est décrite dans le texte.
   */
  supprimer(projet: ApqpProject, evenement: Event): void {
    // La ligne entière est cliquable : sans cela, supprimer ouvrirait le projet
    // juste avant de le détruire.
    evenement.stopPropagation();

    const question = $localize`:@@apqp.projects.confirm-delete:Supprimer le projet « ${projet.name}:name: » ? Son cycle, ses livrables et les pièces qui les prouvent seront définitivement perdus.`;
    if (!confirm(question)) return;

    this.service.deleteProject(projet.id).subscribe({
      next: () => this.refresh$.next(),
      error: err => this.errorState$.next(safeErrorMessage(
        err, $localize`:@@apqp.failed:Opération impossible sur le cycle APQP.`))
    });
  }

  typeLabel(type: ApqpProjectType): string {
    return ({
      NPI: $localize`:@@apqp.project.type.npi:NPI — nouveau produit`,
      TOW: $localize`:@@apqp.project.type.tow:ToW — transfert d'activité`,
      NEW_CUSTOMER: $localize`:@@apqp.project.type.new-customer:Nouveau client`,
      OTHER: $localize`:@@apqp.project.type.other:Autre`
    })[type];
  }

  typeBadge(type: ApqpProjectType): string {
    return 'tbadge tbadge-' + type.toLowerCase();
  }

  ariaSupprimer(projet: ApqpProject): string {
    return $localize`:@@apqp.projects.delete-aria:Supprimer le projet ${projet.name}:name:`;
  }

  trackById(_index: number, projet: ApqpProject): string {
    return projet.id;
  }
}
