import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { CapaActionType } from '../../../capa/capa.types';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { NcService } from '../../nc.service';
import { EightDDiscipline, EightDReport } from '../../nc.types';

/** OWASP A03 — un identifiant malformé est refusé côté écran, avant tout appel. */
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Le préfixe que l'assembleur du rapport écrit devant chaque ligne d'action.
 *
 * <p>Il marque la frontière entre ce qui décrit le DOSSIER — intitulé, type,
 * criticité, statut, échéance — et ce que le tableau vivant montre déjà. Voir
 * {@link NcEightDComponent#lignesVisibles} pour le couplage que cela crée.
 */
const PREFIXE_ACTION = 'Action [';

/**
 * Qui peut renseigner et émettre. Miroir exact de la liste posée côté serveur :
 * si les deux divergent, l'écran propose un geste que l'API refusera.
 */
const ROLES_ECRITURE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/**
 * Les trois disciplines qui parlent d'actions, et la nature qui leur correspond.
 *
 * <p>Le 8D nomme les gestes séparément : le D3 est l'endiguement qui protège le
 * client, le D5 l'action corrective qui supprime la cause, le D6 celle qui
 * empêche la récidive. Chaque discipline reçoit donc le tableau de SA nature, et
 * non le tableau complet répété trois fois — qui laisserait chercher, sous
 * chaque titre, les lignes concernées.
 *
 * <p>Le D3 garde en plus son champ de saisie : un endiguement est souvent décidé
 * avant qu'aucun dossier CAPA n'existe, et ce texte-là est le seul à figurer
 * dans le PDF. Le tableau montre les actions d'endiguement que le dossier porte
 * vraiment ; sans lui, une action de nature CONTAINMENT (ADR 0115) ne
 * s'affichait nulle part dans le rapport.
 */
const NATURES_PAR_DISCIPLINE: Readonly<Record<string, readonly CapaActionType[]>> = {
  D3: Object.freeze<CapaActionType[]>(['CONTAINMENT']),
  // Le D5 nomme les TROIS natures. Restreint a « corrective », il laissait
  // dehors l'action d'endiguement et l'action preventive ajoutees depuis
  // l'ecart : on les avait saisies, elles existaient dans le dossier, et la
  // discipline ou l'on vient lire ce qui a ete decide ne les montrait pas.
  D5: Object.freeze<CapaActionType[]>(['CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE']),
  D6: Object.freeze<CapaActionType[]>(['PREVENTIVE'])
};

/** Le tableau vide partagé des disciplines qui ne montrent pas d'actions. */
const AUCUNE_NATURE: readonly CapaActionType[] = Object.freeze<CapaActionType[]>([]);

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

  /**
   * Ce que le rapport montre VIVANT, à côté du texte agrégé.
   *
   * <p>Le 8D disait le D4 et le D5 en lignes de texte : on y lisait la cause
   * retenue et l'intitulé d'une action, sans voir le raisonnement qui mène à
   * l'une ni l'avancement de l'autre. Le texte reste ce que le PDF scellera —
   * il doit être une fonction pure de ce qui est stocké — mais l'écran, lui,
   * peut montrer les figures et le tableau, et laisser agir sur place.
   */
  readonly fiveWhys$ = new BehaviorSubject<FiveWhysAnalysis[]>([]);
  readonly ishikawa$ = new BehaviorSubject<IshikawaDiagramResponse[]>([]);
  /**
   * Le dossier CAPA de l'écart, s'il existe : le tableau n'a que son identifiant.
   *
   * <p>TROIS états, et non deux. {@code undefined} veut dire « on ne sait pas
   * encore » — le rapport et l'écart se chargent par deux requêtes distinctes,
   * et le rapport arrive le premier. Parti de {@code null}, le gabarit
   * affirmait « aucun dossier CAPA sur cet écart » le temps que la seconde
   * réponde, dans les trois disciplines qui portent un tableau ; et si cette
   * requête échouait — l'erreur n'est qu'une trace de console — l'affirmation
   * fausse restait à l'écran pour de bon. On ne dit plus rien tant qu'on ne
   * sait pas.
   */
  readonly capaCaseId$ = new BehaviorSubject<string | null | undefined>(undefined);

  /** Lu par le gabarit : la discipline sans dossier CAPA renvoie vers l'ecart. */
  ncId = '';
  /** `BehaviorSubject` et non `Subject` : la vue s'abonne après le premier tir. */
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: NcService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly fiveWhys: FiveWhysService,
    private readonly ishikawa: IshikawaService,
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
    this.chargerAnalyses();
    this.chargerDossierCapa();
  }

  /**
   * Charge les figures d'analyse de cause (D4).
   *
   * <p>Chargement non bloquant, et panne silencieuse : le rapport reste lisible
   * si l'une des deux méthodes est indisponible — son texte agrégé, lui, est
   * déjà là.
   */
  private chargerAnalyses(): void {
    this.fiveWhys.listForNc(this.ncId).subscribe({
      next: chaines => this.fiveWhys$.next(chaines),
      // eslint-disable-next-line no-console
      error: err => console.warn('[nc-8d] five-whys listForNc failed', err?.status)
    });
    this.ishikawa.listForNc(this.ncId).subscribe({
      next: diagrammes => this.ishikawa$.next(diagrammes),
      // eslint-disable-next-line no-console
      error: err => console.warn('[nc-8d] ishikawa listForNc failed', err?.status)
    });
  }

  /** L'identifiant du dossier CAPA, lu sur l'écart : le tableau charge le reste. */
  private chargerDossierCapa(): void {
    this.svc.getNc(this.ncId).subscribe({
      next: nc => this.capaCaseId$.next(nc.capaCaseId ?? null),
      // On laisse `undefined` : l'écart n'a pas répondu, donc on ne SAIT pas
      // s'il porte un dossier. Écrire `null` ici reviendrait à affirmer qu'il
      // n'en a pas — l'écran dirait à l'auditeur qu'aucune action n'est
      // décidée alors qu'on a seulement échoué à le lire.
      // eslint-disable-next-line no-console
      error: err => console.warn('[nc-8d] getNc failed', err?.status)
    });
  }

  /** La nature d'action que cette discipline montre, ou null si elle n'en montre pas. */
  natureDe(code: string): CapaActionType | null {
    return NATURES_PAR_DISCIPLINE[code]?.[0] ?? null;
  }

  /**
   * Les natures à passer au tableau : une seule, celle de la discipline.
   *
   * <p>Le tableau renvoyé est TOUJOURS LE MÊME OBJET pour une discipline donnée.
   * Un `[natures]="naturesDe(code)"` qui fabriquerait un tableau neuf à chaque
   * appel changerait l'entrée à chaque passe de détection : le panneau relirait
   * le dossier CAPA par le réseau à chaque passe, et sa réponse déclencherait la
   * passe suivante — une requête en boucle pour un écran immobile.
   */
  naturesDe(code: string): readonly CapaActionType[] {
    return NATURES_PAR_DISCIPLINE[code] ?? AUCUNE_NATURE;
  }

  /** Vrai pour les disciplines qui portent un tableau d'actions. */
  montreLesActions(code: string): boolean {
    return this.naturesDe(code).length > 0;
  }

  /**
   * Vrai quand le tableau d'actions tient lieu des LIGNES D'ACTION, à l'écran.
   *
   * <p>Le D5 listait une ligne de phrase par action, juste au-dessus du tableau
   * qui montre ces mêmes actions — avec leur porteur, leur avancement, leur
   * preuve, et de quoi les faire avancer. Deux lectures du même fait, dont une
   * inerte.
   *
   * <p>Le document émis, lui, garde ce texte : le PDF n'a pas de tableau, et
   * c'est ce texte-là qui est figé, signé et ancré à l'émission.
   */
  texteRemplaceParLeTableau(code: string): boolean {
    return code === 'D5';
  }

  /**
   * Les lignes du texte agrégé qui restent à lire à l'écran.
   *
   * <p>Le tableau ne remplace que les lignes d'ACTION. Les premières lignes du
   * D5 décrivent le DOSSIER — son intitulé, son type, sa criticité, son statut,
   * son échéance — et le tableau n'en porte aucune. Masquer le bloc entier les
   * faisait disparaître de l'écran : on avait retiré, avec le doublon, cinq
   * lignes qui ne doublaient rien.
   *
   * <p>Le repère est le préfixe que l'assembleur écrit devant chaque action.
   * C'est un couplage de forme entre un service Java et un gabarit, et il est
   * assumé faute de mieux : porter la coupure dans l'instantané demanderait un
   * champ de plus dans le contrat, sa conversion et sa migration, pour un
   * découpage qui n'intéresse que l'affichage. Le banc d'essai qui le vérifie
   * est là pour que le jour où l'assembleur change de forme, c'est lui qui le
   * dise, et non un utilisateur.
   */
  lignesVisibles(discipline: EightDDiscipline): string[] {
    if (!this.texteRemplaceParLeTableau(discipline.code)) {
      return discipline.lines;
    }
    const premiere = discipline.lines.findIndex(l => l.startsWith(PREFIXE_ACTION));
    return premiere < 0 ? discipline.lines : discipline.lines.slice(0, premiere);
  }

  /** Vrai pour la discipline qui porte l'analyse de cause. */
  estCauseRacine(code: string): boolean {
    return code === 'D4';
  }

  /**
   * Relit le rapport.
   *
   * <p>Public : le tableau d'actions, qui en fait avancer une sans quitter le
   * rapport, annonce ses changements -- et le texte agrégé des disciplines en
   * dépend. Un rapport émis ne bougera pas pour autant : son contenu est figé.
   */
  reload(): void {
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
