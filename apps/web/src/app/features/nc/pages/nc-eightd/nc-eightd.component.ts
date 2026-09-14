import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { NcService } from '../../nc.service';
import { EightDReport } from '../../nc.types';

/** OWASP A03 — un identifiant malformé est refusé côté écran, avant tout appel. */
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Qui peut renseigner et émettre. Miroir exact de la liste posée côté serveur :
 * si les deux divergent, l'écran propose un geste que l'API refusera.
 */
const ROLES_ECRITURE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/**
 * Le rapport 8D d'une non-conformité.
 *
 * <p>L'écran ne compose rien : les huit disciplines viennent du serveur, qui en
 * agrège cinq depuis la NC, les analyses de cause, la CAPA escaladée, le PFMEA et
 * les plans de surveillance. Seules trois se saisissent ici — l'équipe, la sécurisation,
 * la reconnaissance — parce que rien, dans la plateforme, ne sait y répondre.
 *
 * <p>Une discipline sans contenu est affichée avec la raison de son absence, jamais
 * comme un bloc vide : c'est ce qui distingue un 8D d'un formulaire à trous.
 */
@Component({
  selector: 'qos-nc-eightd',
  templateUrl: './nc-eightd.component.html',
  styleUrls: ['./nc-eightd.component.scss'],
  standalone: false
})
export class NcEightDComponent implements OnInit {

  report$!: Observable<EightDReport | null>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  /** `deferredView` évite NG0100 : l'état est poussé depuis `tap`/`finalize`. */
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  readonly acting$ = new BehaviorSubject<boolean>(false);

  /**
   * Les trois disciplines saisies.
   *
   * <p>Formulaire réactif et non `ngModel` : c'est la convention du dépôt, et
   * `SharedModule` n'exporte que `ReactiveFormsModule`. La limite de 4000
   * caractères est celle de la colonne — la vérifier ici évite d'aller chercher
   * un 422 pour l'apprendre.
   */
  readonly form = this.fb.nonNullable.group({
    team: ['', [Validators.maxLength(4000)]],
    containment: ['', [Validators.maxLength(4000)]],
    recognition: ['', [Validators.maxLength(4000)]]
  });

  /** Vrai si l'utilisateur peut renseigner et émettre. */
  readonly editable: boolean;

  private ncId = '';
  /** `BehaviorSubject` et non `Subject` : la vue s'abonne après le premier tir. */
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: NcService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_ECRITURE);
  }

  ngOnInit(): void {
    const brut = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(brut) && !this.isMockId(brut)) {
      this.snack.open(
        $localize`:@@common.invalid-id:Identifiant invalide.`,
        $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/nc']);
      return;
    }
    this.ncId = brut;
    this.report$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.getEightDReport(this.ncId).pipe(
        tap(rapport => this.reprendreSaisies(rapport)),
        catchError(err => {
          this.errorState$.next(safeErrorMessage(
            err, $localize`:@@nc.8d.load-failed:Rapport 8D indisponible.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
  }

  /** Retour à la fiche de la non-conformité, d'où l'on vient. */
  retour(): void {
    this.router.navigate(['/nc', this.ncId]);
  }

  /** Un rapport émis est scellé : le formulaire se ferme. */
  saisieOuverte(rapport: EightDReport): boolean {
    return this.editable && rapport.status === 'DRAFT';
  }

  enregistrer(): void {
    if (this.acting$.value || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.acting$.next(true);
    const saisies = this.form.getRawValue();
    this.svc.saveEightDReport(this.ncId, {
      team: saisies.team,
      containment: saisies.containment,
      recognition: saisies.recognition
    }).pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@nc.8d.saved:Rapport 8D enregistré.`,
          $localize`:@@common.ok:OK`, { duration: 3000 });
        this.reload$.next();
      },
      error: err => this.echec(err, $localize`:@@nc.8d.save-failed:Enregistrement impossible.`)
    });
  }

  /**
   * Émet le rapport, après confirmation.
   *
   * <p>Le geste est à sens unique — le contenu est figé, signé et ancré — et la
   * confirmation le dit. Le serveur le refusera de toute façon une seconde fois,
   * mais un document opposable ne se produit pas par inadvertance.
   */
  emettre(rapport: EightDReport): void {
    if (this.acting$.value) { return; }
    const donnees: ConfirmDialogData = {
      title: $localize`:@@nc.8d.issue-confirm-title:Émettre le rapport 8D ?`,
      message: rapport.partial
        ? $localize`:@@nc.8d.issue-confirm-partial:Des disciplines n'ont aucun contenu : le rapport sera marqué « partiel ». Son contenu sera figé, signé et ancré — il ne pourra plus être modifié.`
        : $localize`:@@nc.8d.issue-confirm:Le contenu sera figé, signé et ancré. Il ne pourra plus être modifié.`,
      confirmLabel: $localize`:@@nc.8d.issue:Émettre`,
      destructive: true
    };
    this.dialog.open(ConfirmDialogComponent, {
      data: donnees, panelClass: 'qos-dialog-panel', autoFocus: false, restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (!ok) { return; }
      this.acting$.next(true);
      this.svc.issueEightDReport(this.ncId)
        .pipe(finalize(() => this.acting$.next(false)))
        .subscribe({
          next: () => {
            this.snack.open(
              $localize`:@@nc.8d.issued:Rapport 8D émis, signé et ancré.`,
              $localize`:@@common.ok:OK`, { duration: 4000 });
            this.reload$.next();
          },
          error: err => this.echec(err, $localize`:@@nc.8d.issue-failed:Émission impossible.`)
        });
    });
  }

  /**
   * Exporte le rapport : le PDF signé et ancré.
   *
   * <p>Un brouillon n'a pas de PDF, et c'est voulu : le document n'existe qu'une
   * fois le contenu FIGÉ, sans quoi deux exports du même rapport ne diraient pas
   * la même chose. Plutôt que de masquer le bouton — ce qui laisse croire que
   * l'export n'existe pas —, on l'affiche toujours et on explique ce qu'il
   * manque, en proposant le geste qui débloque.
   */
  exporter(rapport: EightDReport): void {
    if (this.acting$.value) { return; }
    if (rapport.status === 'ISSUED') {
      this.telecharger();
      return;
    }
    const donnees: ConfirmDialogData = {
      title: $localize`:@@nc.8d.export-needs-issue-title:Émettre avant d'exporter ?`,
      message: $localize`:@@nc.8d.export-needs-issue:Le PDF n'existe qu'une fois le rapport émis : l'émission fige le contenu, signe son empreinte et l'ancre. Sans cela, deux exports du même rapport pourraient différer.`,
      confirmLabel: $localize`:@@nc.8d.issue:Émettre`
    };
    this.dialog.open(ConfirmDialogComponent, {
      data: donnees, panelClass: 'qos-dialog-panel', autoFocus: false, restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (ok) { this.emettre(rapport); }
    });
  }

  /** Télécharge le PDF signé, sous le nom que le serveur propose. */
  telecharger(): void {
    if (this.acting$.value) { return; }
    this.acting$.next(true);
    this.svc.downloadEightDPdf(this.ncId)
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: reponse => {
          const blob = reponse.body;
          if (!blob) {
            this.snack.open(
              $localize`:@@nc.8d.download-failed:Téléchargement impossible.`,
              $localize`:@@common.ok:OK`, { duration: 4000 });
            return;
          }
          this.enregistrerSous(blob, this.nomDeFichier(reponse) ?? 'rapport-8d.pdf');
        },
        error: err => this.echec(err, $localize`:@@nc.8d.download-failed:Téléchargement impossible.`)
      });
  }

  /** Le nom proposé par le serveur, lu dans `Content-Disposition`. */
  private nomDeFichier(reponse: HttpResponse<Blob>): string | null {
    const entete = reponse.headers.get('Content-Disposition');
    if (!entete) { return null; }
    const trouve = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(entete);
    return trouve ? decodeURIComponent(trouve[1]) : null;
  }

  private enregistrerSous(blob: Blob, nom: string): void {
    const url = URL.createObjectURL(blob);
    const lien = document.createElement('a');
    lien.href = url;
    lien.download = nom;
    lien.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Aligne les trois champs sur ce que le serveur a retenu.
   *
   * <p>Appelé à chaque (re)lecture du rapport : après un enregistrement, c'est la
   * valeur NORMALISÉE par le serveur qui revient — les espaces en trop ont disparu,
   * une saisie vide est devenue nulle. Réafficher la frappe d'origine laisserait
   * croire que ce qui est stocké en diffère.
   */
  private reprendreSaisies(rapport: EightDReport): void {
    this.form.setValue({
      team: rapport.team ?? '',
      containment: rapport.containment ?? '',
      recognition: rapport.recognition ?? ''
    });
    if (rapport.status === 'ISSUED' || !this.editable) {
      // Un rapport scellé ne se saisit plus, et un lecteur sans habilitation ne
      // doit pas croire qu'il peut écrire : le formulaire se ferme dans les deux cas.
      this.form.disable({ emitEvent: false });
    }
  }

  private echec(err: unknown, defaut: string): void {
    this.snack.open(safeErrorMessage(err, defaut),
      $localize`:@@common.ok:OK`, { duration: 4000 });
  }

  private isMockId(valeur: string): boolean {
    return /^nc-[a-z0-9-]{1,64}$/i.test(valeur);
  }
}
