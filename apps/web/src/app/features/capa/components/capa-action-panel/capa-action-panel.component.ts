import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, forkJoin } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { actionTypeLabel } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaActionStatus, CapaActionType, CapaCaseResponse, CapaEvidence,
  CapaSourceType, CapaStatus
} from '../../capa.types';
import {
  CapaActionDialogComponent, CapaActionDialogData
} from '../../pages/capa-action-dialog/capa-action-dialog.component';
import {
  CapaActionVerificationDialogComponent, CapaActionVerificationDialogData
} from '../../pages/capa-action-verification-dialog/capa-action-verification-dialog.component';
import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../../pages/capa-create-dialog/capa-create-dialog.component';

/** Le pas suivant d'une action : il n'y en a jamais deux. */
const SUITE: Record<CapaActionStatus, CapaActionStatus | null> = {
  PENDING: 'IN_PROGRESS',
  IN_PROGRESS: 'DONE',
  DONE: null
};

/**
 * L'action du dossier CAPA, lue et avancée <b>hors de la fiche CAPA</b>.
 *
 * <p>Depuis la fiche d'un écart, « Voir la CAPA » emmenait dans un autre écran
 * pour une seule question : où en est l'action ? On revenait donc en arrière
 * pour la poser, et on ne la posait plus. Ce panneau la montre sur place, et
 * laisse la faire avancer sans quitter l'écart — c'est aussi ce que le D5 du
 * rapport 8D rassemble.
 *
 * <p><b>Une CAPA porte UNE action</b>, et une seule : c'est le modèle de
 * l'ADR 0088 §1, que l'ADR 0115 avait révisé à tort. Le tableau d'une discipline
 * ne liste donc pas les actions d'un dossier : il rassemble les actions de SA
 * nature portees par TOUS les dossiers de l'écart -- une ligne, une CAPA. Une
 * ligne s'ouvre d'un clic pour se corriger, et le bouton d'ajout <b>ouvre une
 * CAPA de plus</b> sur l'écart, de la nature de la discipline.
 *
 * <p><b>Il porte aussi la PREUVE de chaque action</b> (ADR 0052), en colonne du
 * tableau : la pièce qui atteste que ce geste-là a été fait. Elle vivait dans
 * un pavé vertical de la fiche CAPA, qui dépliait chaque action sur six lignes
 * en répétant la nature, l'échéance et le statut déjà lus en tête de fiche ;
 * ce pavé a disparu au profit de ce tableau (ADR 0117), qui devait donc
 * reprendre ce qu'il savait faire de plus.
 *
 * <p>À ne pas confondre avec les preuves du DOSSIER, restées sur la fiche :
 * une pièce par action dit <i>« ce geste a été fait »</i>, jusqu'à dix pièces
 * de dossier disent <i>« le problème a cessé »</i> (ISO 9001 §10.2). Un dossier
 * peut très bien avoir toutes ses actions prouvées et aucune preuve
 * d'efficacité — c'est même le cas trois mois avant sa clôture.
 */
@Component({
  selector: 'qos-capa-action-panel',
  templateUrl: './capa-action-panel.component.html',
  styleUrls: ['./capa-action-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class CapaActionPanelComponent implements OnChanges {

  /**
   * Les dossiers à lire. Le panneau les charge lui-même : l'appelant n'a que
   * les identifiants.
   *
   * <p><b>Plusieurs</b>, et c'est tout l'objet de cette entrée. Une CAPA naît
   * avec son action : en ajouter une depuis la section CAPA ouvre donc un
   * NOUVEAU dossier sur le même écart. L'hôte rendait un panneau par dossier,
   * et chacun son tableau — l'action écrite depuis la section CAPA apparaissait
   * dans un second tableau sous le premier, comme si elle refusait de rejoindre
   * ses pareilles. Une discipline n'a qu'un tableau : il rassemble les actions
   * de SA nature, quel que soit le dossier qui les porte.
   *
   * <p>En lecture seule, et pour la même raison que {@link #natures} : l'hôte
   * doit passer un tableau STABLE, sinon `ngOnChanges` relit tout par le réseau
   * à chaque passe de détection.
   */
  @Input() capaCaseIds: readonly string[] = [];

  /**
   * Les natures à montrer. Vide — le défaut — montre toutes les actions.
   *
   * <p>Le rapport 8D nomme les trois gestes séparément : endiguer (D3), corriger
   * la cause (D5), prévenir la récidive (D6). Chaque discipline reçoit donc le
   * tableau de SA nature ; un tableau complet répété sous trois titres laisserait
   * chercher, dans chacun, les lignes qui le concernent.
   */
  // En lecture seule, et pour une raison : l'hôte doit passer un tableau STABLE
  // (le même objet d'une passe de détection à l'autre), sans quoi `ngOnChanges`
  // relirait le dossier par le réseau à chaque passe. Le type le dit.
  @Input() natures: readonly CapaActionType[] = [];

  /**
   * Montre ou non la colonne « Preuve », la pièce de CHAQUE action (ADR 0052).
   *
   * <p>Vraie par défaut : la fiche CAPA, le D3 et le D7 du traitement 8D
   * attestent ainsi que tel geste a bien été fait — un endiguement qu'on
   * montre, une barrière qu'on installe. Le D5 seul la met à faux, non par
   * économie d'écran, mais parce que ce qu'il a à démontrer n'est pas
   * « l'action a été faite » : c'est « la cause a disparu », et cela se prouve
   * au D6, sur le dossier, où le modèle de traitement 8D l'attend.
   */
  @Input() colonnePreuve = true;

  /**
   * Les personnes déjà sur l'écart (l'équipe du D1), proposées en tête du
   * sélecteur de responsable quand on ajoute ou corrige une action.
   *
   * <p>Le panneau ne les cherche pas lui-même : il sert aussi la fiche d'un
   * dossier CAPA ouvert sans écart, où il n'y a pas d'équipe 8D. L'écran qui en
   * a une la donne, les autres n'en donnent pas.
   */
  @Input() team: readonly { accountId: string }[] = [];

  /**
   * L'écart à citer quand ce tableau ouvre une CAPA de plus.
   *
   * <p>Sans lui, pas d'ajout : une CAPA porte une action et se rattache à
   * l'écart qu'elle traite ; en ouvrir une sans savoir lequel donnerait un
   * dossier orphelin, que ni la fiche de l'écart ni son rapport 8D ne
   * verraient. Le bouton d'ajout se retire alors plutôt que d'échouer au clic.
   *
   * <p>L'hôte doit passer un objet STABLE, pour la raison de {@link #natures}.
   */
  @Input() source?: { type: CapaSourceType; reference: string };

  /**
   * L'écart accepte-t-il encore qu'on décide des actions ?
   *
   * <p>LE FREIN DE L'AJOUT, et il vient de l'écart. Tant qu'ajouter voulait dire
   * « une action de plus dans ce dossier », le verrou de clôture du dossier
   * faisait office de frein. Il n'en fait plus : la CAPA ouverte est NOUVELLE, et
   * le dossier clos d'à côté ne la concerne pas. Sans cette entrée, on pourrait
   * ouvrir une action sur un écart terminé.
   */
  @Input() ajoutAutorise = true;

  /** Émis après chaque changement enregistré : l'écran appelant peut se relire. */
  @Output() readonly changed = new EventEmitter<void>();

  /**
   * Combien de lignes ce panneau MONTRE, émis à chaque lecture du dossier.
   *
   * <p>Pour l'hôte qui résume l'avancement sans refaire la requête : le sommaire
   * des huit disciplines coche « endiguer » et « prévenir » d'après le nombre
   * d'actions de CETTE nature, et seul le panneau le connaît -- la fiche de
   * l'écart sait quels dossiers existent, pas ce qu'ils portent.
   *
   * <p>Zéro est une valeur : « lu, et il n'y en a aucune » n'est pas « pas encore
   * lu », et le sommaire ne doit pas cocher la seconde.
   */
  @Output() readonly counted = new EventEmitter<number>();

  /** Les dossiers lus, dans l'ordre où l'hôte les a donnés. */
  dossiers: CapaCaseResponse[] = [];
  /** Vrai dès qu'une lecture a abouti : c'est le feu vert du tableau. */
  pret = false;
  /**
   * Le dossier qui porte chaque action, indexé par identifiant d'action.
   *
   * <p>Le tableau mêle les actions de plusieurs dossiers, et chaque geste —
   * avancer, joindre une pièce, juger — s'adresse au dossier de SA ligne. Un
   * seul dossier courant ne suffit donc plus.
   */
  private porteur = new Map<string, CapaCaseResponse>();
  readonly loading$ = new BehaviorSubject<boolean>(false);
  readonly acting$ = new BehaviorSubject<boolean>(false);
  /** Ce qui empêche de lire le dossier, dit plutôt que laissé en blanc. */
  error: string | null = null;

  /** La pièce de chaque action, indexée par identifiant d'action. */
  private preuves = new Map<string, CapaEvidence>();
  /** L'action dont la pièce part ou s'en va : sa cellule l'annonce. */
  preuveEnCours: string | null = null;
  /** Stockage coupé sur l'environnement : la colonne le dit, sans crier à la panne. */
  stockageCoupe = false;
  /**
   * Action visée par le sélecteur de fichier, unique et partagé par toutes les
   * lignes. Sans cette mémoire, le fichier choisi atterrirait sur la dernière
   * ligne rendue plutôt que sur celle où l'on a cliqué.
   */
  private actionVisee: string | null = null;

  constructor(
    private readonly capaService: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(): void {
    this.reload();
  }

  reload(): void {
    const ids = this.capaCaseIds.filter(id => !!id);
    if (ids.length === 0) {
      this.oublier();
      this.error = null;
      this.cdr.markForCheck();
      return;
    }
    this.error = null;
    this.loading$.next(true);
    // TOUT ou rien, et en parallèle. Rendre le tableau dossier par dossier le
    // ferait grandir sous les yeux, et le décompte émis à chaque arrivée ferait
    // clignoter la coche de la discipline. Un dossier illisible arrête la
    // lecture entière : c'est déjà ce que faisait le panneau d'avant, à ceci
    // près qu'il n'en portait qu'un -- et un tableau qui cache des lignes sans
    // le dire vaut moins qu'un tableau qui annonce sa panne.
    forkJoin(ids.map(id => this.capaService.getCase(id))).pipe(
      finalize(() => this.loading$.next(false))
    ).subscribe({
      next: lus => {
        this.dossiers = lus;
        this.porteur = new Map(
          lus.flatMap(d => (d.actions ?? []).map(a => [a.id, d] as [string, CapaCaseResponse])));
        this.pret = true;
        this.cdr.markForCheck();
        // Après coup : le décompte porte sur ce que le panneau MONTRE, donc sur
        // les dossiers déjà posés et la nature déjà filtrée.
        this.counted.emit(this.actions.length);
        this.chargerPreuves();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] getCase failed', err?.status, err?.error?.title);
        this.oublier();
        // En panne, le panneau ne montre RIEN : l'hôte ne doit pas garder le
        // décompte d'avant, qui lui ferait cocher une discipline illisible.
        this.counted.emit(0);
        this.error = safeErrorMessage(
          err, $localize`:@@capa.action-panel.failed:L'action de la CAPA n'a pas pu être lue.`);
        this.cdr.markForCheck();
      }
    });
  }

  /** Repart de rien : plus de dossier, plus de porteur, plus de tableau. */
  private oublier(): void {
    this.dossiers = [];
    this.porteur = new Map();
    this.pret = false;
  }

  /**
   * Le dossier qui porte cette action.
   *
   * <p>{@code undefined} seulement entre deux lectures : l'index est refait à
   * chaque chargement, en même temps que les lignes qu'il indexe.
   */
  dossierDe(action: CapaActionResponse): CapaCaseResponse | undefined {
    return this.porteur.get(action.id);
  }

  /**
   * Le statut du dossier qui porte cette action.
   *
   * <p>Le verrou de clôture s'applique donc LIGNE PAR LIGNE, et non au tableau
   * entier : les actions d'un dossier clos ne bougent plus, leurs voisines
   * continuent. C'est la conséquence directe d'un tableau qui rassemble
   * plusieurs dossiers.
   */
  statutDe(action: CapaActionResponse): CapaStatus | undefined {
    return this.dossierDe(action)?.status;
  }

  /**
   * Vrai quand plus rien ne peut bouger sur cette ligne.
   *
   * <p>Dossier inconnu compris : sans savoir à qui parler, on n'offre pas le
   * geste plutôt que de l'envoyer au hasard.
   */
  verrouillee(action: CapaActionResponse): boolean {
    const statut = this.statutDe(action);
    return statut === undefined || this.isTerminal(statut);
  }

  /**
   * Le dossier qui recevra une action AJOUTÉE ici : le plus ancien de l'écart.
   *
   * <p>C'est celui que porte le lien de l'écart, et c'est l'ordre que rend le
   * serveur (du plus ancien au plus récent). Ajouter au dernier arrivé ferait
   * dépendre la destination de l'historique des clics.
   */
  get dossierDAjout(): CapaCaseResponse | undefined {
    return this.dossiers[0];
  }

  /**
   * Vrai quand on ne peut pas ouvrir une CAPA de plus ici.
   *
   * <p>Deux cas : aucun écart à citer ({@link #source}), ou aucun dossier d'où
   * hériter la criticité. Le verrou de clôture d'un dossier, lui, ne ferme plus
   * l'ajout : la CAPA ouverte est NOUVELLE, et celle qui est close ne la
   * concerne pas.
   */
  get ajoutFerme(): boolean {
    return !this.ajoutAutorise || !this.source || !this.dossierDAjout;
  }

  // ---------- lecture ----------

  get actions(): CapaActionResponse[] {
    const toutes = this.dossiers.flatMap(d => d.actions ?? []);
    if (this.natures.length === 0) {
      return toutes;
    }
    // Une action sans nature est corrective : c'est le défaut du serveur, et
    // l'exclure du D5 ferait disparaître les anciennes actions du rapport.
    return toutes.filter(a => this.natures.includes(a.actionType || 'CORRECTIVE'));
  }

  trackById(_: number, action: CapaActionResponse): string {
    return action.id;
  }

  /**
   * Le libellé du bouton d'ajout : il NOMME la nature quand le tableau n'en
   * montre qu'une.
   *
   * <p>« Ajouter une action » sous le tableau des actions de confinement, puis
   * le même mot sous celui des correctives et sous celui des préventives :
   * trois boutons identiques pour trois gestes différents, et rien, avant le
   * clic, ne disait lequel on allait écrire. Le bouton dit désormais ce qu'il
   * ajoute — c'est aussi ce que la fenêtre qu'il ouvre a maintenant de cochée,
   * sans autre choix.
   */
  get libelleAjout(): string {
    if (this.natures.length !== 1) {
      return $localize`:@@capa.actions.add:Ajouter une action`;
    }
    switch (this.natures[0]) {
      case 'CONTAINMENT':
        return $localize`:@@capa.actions.add-containment:Ajouter une action de confinement`;
      case 'PREVENTIVE':
        return $localize`:@@capa.actions.add-preventive:Ajouter une action préventive`;
      default:
        return $localize`:@@capa.actions.add-corrective:Ajouter une action corrective`;
    }
  }

  /** Un dossier clos ou rejeté ne bouge plus : le serveur tient la même garde. */
  isTerminal(status: CapaStatus): boolean {
    return status === 'CLOSED' || status === 'REJECTED';
  }

  actionTypeLabel(type: CapaActionResponse['actionType']): string {
    return actionTypeLabel(type || 'CORRECTIVE');
  }

  actionStatusLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : status === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  actionBadge(status: CapaActionStatus): string {
    return 'badge badge-' + status.toLowerCase().replace('_', '-');
  }

  // ---------- preuve d'action (ADR 0052) ----------

  /**
   * Chargement NON bloquant : le tableau reste lisible même si le stockage est
   * coupé, et c'est la colonne qui dit son état plutôt qu'une erreur en travers
   * de l'écran. Le dossier, lui, se lit sans ses pièces.
   */
  private chargerPreuves(): void {
    if (this.dossiers.length === 0) {
      return;
    }
    // Un appel PAR DOSSIER : les pièces se demandent dossier par dossier, et le
    // tableau en mêle plusieurs. Les listes se fondent en un seul index, closé
    // par action -- c'est l'action qui porte sa pièce, pas son dossier.
    forkJoin(this.dossiers.map(d => this.capaService.listActionEvidences(d.id))).subscribe({
      next: listes => {
        const par = new Map<string, CapaEvidence>();
        // Le serveur garantit une pièce par action ; on garde malgré tout la
        // dernière si deux arrivaient, plutôt que d'afficher une case vide.
        listes.flat().forEach(e => { if (e.actionId) { par.set(e.actionId, e); } });
        this.preuves = par;
        this.cdr.markForCheck();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] listActionEvidences failed', err?.status, err?.error?.type);
        if (this.stockageEstCoupe(err)) {
          this.stockageCoupe = true;
          this.cdr.markForCheck();
        }
      }
    });
  }

  /** La pièce de cette action, ou {@code undefined} — c'est ce que la case montre. */
  preuve(actionId: string): CapaEvidence | undefined {
    return this.preuves.get(actionId);
  }

  /**
   * Le dépôt suit le verrou du dossier DE CETTE ACTION : plus rien ne bouge une
   * fois ce dossier clos ou rejeté, même si ses voisins du même tableau sont
   * encore ouverts.
   */
  peutJoindre(action: CapaActionResponse): boolean {
    return !this.verrouillee(action)
      && !this.stockageCoupe
      && !this.preuve(action.id)
      && this.preuveEnCours !== action.id;
  }

  /** Icône par famille de document : on reconnaît un PDF d'un tableur d'un coup d'œil. */
  iconePreuve(contentType: string): string {
    if (contentType === 'application/pdf') return 'picture_as_pdf';
    if (contentType.startsWith('image/')) return 'image';
    if (contentType.includes('spreadsheet')) return 'table_chart';
    if (contentType.includes('wordprocessing')) return 'description';
    return 'attach_file';
  }

  /**
   * Libellés d'accessibilité des boutons de preuve.
   *
   * <p>Une icône seule ou un mot isolé (« Joindre », « × ») ne dit pas SUR QUOI
   * il agit : au lecteur d'écran, huit lignes rendent huit boutons « Joindre »
   * indiscernables. L'intitulé de l'action est donc repris dans l'étiquette —
   * et traduit, parce qu'un lecteur d'écran arabe ou japonais lit ce texte.
   */
  joindreAria(titre: string): string {
    return $localize`:@@capa.action-evidence.attach-aria:Joindre une preuve à l'action : ${titre}:title:`;
  }

  retirerAria(titre: string): string {
    return $localize`:@@capa.action-evidence.remove-aria:Retirer la preuve de l'action : ${titre}:title:`;
  }

  /** Ouvre le sélecteur de fichier en retenant l'action visée. */
  choisirFichier(input: HTMLInputElement, actionId: string): void {
    if (this.preuveEnCours) {
      return;
    }
    this.actionVisee = actionId;
    input.click();
  }

  fichierChoisi(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files && input.files[0];
    // Vidé tout de suite : rechoisir le MÊME fichier après un refus ne
    // déclencherait sinon aucun événement, et l'écran paraîtrait bloqué.
    input.value = '';
    const actionId = this.actionVisee;
    this.actionVisee = null;
    const dossier = actionId ? this.porteur.get(actionId) : undefined;
    if (!fichier || !actionId || !dossier) {
      return;
    }
    this.preuveEnCours = actionId;
    this.cdr.markForCheck();
    this.capaService.uploadActionEvidence(dossier.id, actionId, fichier).pipe(
      finalize(() => { this.preuveEnCours = null; this.cdr.markForCheck(); })
    ).subscribe({
      next: piece => {
        this.preuves = new Map(this.preuves).set(actionId, piece);
        this.cdr.markForCheck();
        this.snack.open(
          $localize`:@@capa.action-evidence.added:Preuve jointe à l'action.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] uploadActionEvidence failed', err?.status, err?.error?.type);
        if (this.stockageEstCoupe(err)) {
          this.stockageCoupe = true;
          this.cdr.markForCheck();
          return;
        }
        this.snack.open(this.refusDePreuve(err), $localize`:@@common.ok:OK`, { duration: 5000 });
      }
    });
  }

  /**
   * Vrai quand cette action peut encore être effacée.
   *
   * <p>Un dossier clos ou rejeté est un écrit arrêté : en retirer une action
   * réécrirait ce qui a été déclaré fait. Le serveur refuse de même ; l'écran
   * ne fait que ne pas proposer ce qu'il refusera.
   */
  peutSupprimer(action: CapaActionResponse): boolean {
    return !this.verrouillee(action);
  }

  /** L'étiquette NOMME l'action : huit poubelles sans nom sont indiscernables. */
  ariaSupprimer(action: CapaActionResponse): string {
    const titre = action.title;
    return $localize`:@@capa.action.remove-aria:Supprimer l'action : ${titre}:title:`;
  }

  /**
   * Efface une action, après confirmation.
   *
   * <p>Une action saisie par erreur — mauvaise nature, doublon, mauvais
   * dossier — restait inscrite au registre faute de geste pour la retirer,
   * et retenait la clôture du dossier tant qu'on ne l'avait pas menée. La
   * corriger n'est pas toujours possible : une action qui n'avait pas lieu
   * d'être ne devient pas juste en changeant d'intitulé.
   */
  supprimerAction(action: CapaActionResponse): void {
    const porteur = this.dossierDe(action);
    if (!porteur || !this.peutSupprimer(action)) {
      return;
    }
    const dossier = porteur.id;
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@capa.action.remove-title:Supprimer cette action ?`,
        message: $localize`:@@capa.action.remove-message:L'action et ses preuves seront retirées du dossier. La suppression est tracée au journal, mais l'action ne se restaure pas.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirme => {
      if (!confirme) {
        return;
      }
      this.acting$.next(true);
      this.capaService.deleteAction(dossier, action.id).pipe(
        finalize(() => { this.acting$.next(false); this.cdr.markForCheck(); })
      ).subscribe({
        next: () => {
          this.snack.open(
            $localize`:@@capa.action.removed:Action supprimée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.changed.emit();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-action-panel] deleteAction failed', err?.status, err?.error?.type);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@capa.action.remove-failed:L'action n'a pas pu être supprimée.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
    });
  }

  retirerPreuve(action: CapaActionResponse, piece: CapaEvidence): void {
    const porteur = this.dossierDe(action);
    if (this.preuveEnCours || !porteur) {
      return;
    }
    const dossier = porteur.id;
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@capa.action-evidence.remove-title:Retirer la preuve de cette action ?`,
        message: $localize`:@@capa.action-evidence.remove-message:La pièce sera définitivement retirée. Une action déclarée faite sans preuve s'affirme faite sans le démontrer : ce retrait est traçable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirme => {
      if (!confirme) {
        return;
      }
      this.preuveEnCours = action.id;
      this.cdr.markForCheck();
      this.capaService.deleteActionEvidence(dossier, action.id, piece.id).pipe(
        finalize(() => { this.preuveEnCours = null; this.cdr.markForCheck(); })
      ).subscribe({
        next: () => {
          const reste = new Map(this.preuves);
          reste.delete(action.id);
          this.preuves = reste;
          this.cdr.markForCheck();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-action-panel] deleteActionEvidence failed', err?.status, err?.error?.type);
          this.snack.open(
            safeErrorMessage(
              err, $localize`:@@capa.evidence.remove-error:Échec du retrait de la preuve.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
    });
  }

  /**
   * 503 + ProblemDetail de type {@code storage-disabled} : le stockage est
   * coupé sur cet environnement. Un message dédié, plutôt qu'une erreur brute
   * qui ferait chercher un bug là où il y a une option éteinte.
   */
  private stockageEstCoupe(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('storage-disabled');
  }

  /**
   * Traduction des refus de téléversement. Chaque refus a sa raison : les
   * confondre reviendrait à dire « non » sans dire quoi corriger.
   */
  private refusDePreuve(err: unknown): string {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    return e?.status === 413
      ? $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
      : e?.status === 400
      ? $localize`:@@capa.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
      : e?.status === 409
      ? $localize`:@@capa.action-evidence.limit:Cette action porte déjà sa preuve, ou le dossier est clôturé.`
      : e?.status === 404
      ? $localize`:@@capa.action-evidence.gone:Cette action n'existe plus — recharge la fiche.`
      : safeErrorMessage(err, $localize`:@@capa.evidence.error:Échec de l'ajout de la preuve.`);
  }

  // ---------- verifier l'efficacite d'une action (ADR 0132) ----------

  /**
   * Cette action attend-elle encore la verification qu'elle a PROMISE ?
   *
   * <p>Trois conditions : elle a promis une verification
   * ({@code verificationRequired}), le geste a eu lieu ({@code DONE}), et son
   * dernier verdict ne leve pas l'obstacle. Un verdict PARTIEL ou INEFFICACE
   * compte donc comme en attente : l'action a ete regardee, et ce qu'on a vu ne
   * permet pas de clore.
   *
   * <p>C'est exactement ce que le serveur refuse a la cloture : une liste
   * d'obstacles qui n'engage pas le serveur ne vaut rien, et l'inverse --
   * l'ecran qui n'annonce pas ce que l'API refusera -- ne vaut pas mieux.
   */
  attendVerification(action: CapaActionResponse): boolean {
    return Boolean(action.verificationRequired)
      && action.status === 'DONE'
      && action.verificationVerdict !== 'EFFECTIVE';
  }

  /** Vrai quand l'effet de cette action a ete constate, et juge suffisant. */
  efficaciteConstatee(action: CapaActionResponse): boolean {
    return action.verificationVerdict === 'EFFECTIVE';
  }

  /**
   * Infobulle de l'efficacite constatee : qui a juge, et quand.
   *
   * <p>Un nom et une date, parce qu'un jugement sans auteur ne vaut rien devant
   * un auditeur. Le nom est celui FIGE au moment du jugement (ADR 0052) : un
   * compte desactive ne doit pas transformer la trace en inconnu.
   */
  verdictTooltip(action: CapaActionResponse): string {
    const quand = action.verifiedAt ?? '';
    const qui = action.verifiedByName ?? '';
    return [qui, quand].filter(x => x).join(' \u2014 ');
  }

  /**
   * Ouvre la fenetre de jugement de CETTE action.
   *
   * <p>Le critere du dossier part avec : les actions anterieures aux colonnes
   * de la V177 n'ont pas le leur, et la fenetre doit pouvoir dire a quoi elle
   * confronte le releve -- ou dire qu'il n'y a rien, plutot que de laisser
   * decouvrir le refus apres le clic.
   */
  verifier(action: CapaActionResponse): void {
    const porteur = this.dossierDe(action);
    if (!porteur || this.acting$.value) {
      return;
    }
    const donnees: CapaActionVerificationDialogData = {
      caseId: porteur.id,
      action,
      caseCriterion: porteur.effectivenessCriterion
    };
    this.dialog
      .open<CapaActionVerificationDialogComponent, CapaActionVerificationDialogData, boolean>(
        CapaActionVerificationDialogComponent, {
          data: donnees, autoFocus: 'first-tabbable', restoreFocus: true,
          panelClass: 'qos-dialog-panel'
        })
      .afterClosed()
      .subscribe(enregistre => {
      if (enregistre) {
        this.reload();
        this.changed.emit();
      }
    });
  }

  // ---------- avancer ----------

  nextStatus(status: CapaActionStatus): CapaActionStatus | null {
    return SUITE[status];
  }

  canAdvance(action: CapaActionResponse): boolean {
    return action.status !== 'DONE'
      && !this.verrouillee(action)
      && !this.acting$.value;
  }

  advanceLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.detail.action-start:Démarrer`
      : $localize`:@@capa.detail.action-complete:Terminer`;
  }

  /**
   * Fait avancer l'action d'un cran.
   *
   * <p>Seul le statut part : renvoyer le libellé le réécrirait à l'identique et
   * écraserait une correction faite entre-temps par quelqu'un d'autre.
   */
  advance(action: CapaActionResponse): void {
    const suivant = this.nextStatus(action.status);
    const porteur = this.dossierDe(action);
    if (!suivant || !porteur || this.acting$.value) {
      return;
    }
    this.acting$.next(true);
    this.capaService.updateAction(porteur.id, action.id, { status: suivant }).pipe(
      finalize(() => this.acting$.next(false))
    ).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@capa.detail.action-advanced:Action mise à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload();
        this.changed.emit();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] advance failed', err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(
            err, $localize`:@@capa.detail.action-advance-error:Mise à jour de l'action impossible.`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  // ---------- ajouter, ouvrir ----------

  /** Libellé lisible par un lecteur d'écran pour la ligne qui s'ouvre. */
  openLabel(action: CapaActionResponse): string {
    return $localize`:@@capa.actions.open-aria:Ouvrir l'action : ${action.title}:titre:`;
  }

  /**
   * Ouvre une CAPA DE PLUS sur l'écart, de la nature de cette discipline.
   *
   * <p>Et non une action de plus dans le dossier voisin : une CAPA porte une
   * action (ADR 0088 §1). Ajouter dans le dossier existant rendait l'action
   * invisible dans la liste des CAPA -- le registre montrait son dossier, pas
   * elle -- et l'aller-retour entre les deux écrans ne se faisait que dans un
   * sens.
   *
   * <p>La nature est IMPOSÉE par la discipline d'où l'on part : choisie dans la
   * fenêtre, une action de confinement écrite au D3 pouvait naître corrective et
   * ce D3 restait vide. La criticité, elle, est héritée du dossier déjà là :
   * c'est celle de l'écart, et la redécider dossier par dossier
   * ferait diverger deux chiffres sur un seul fait.
   */
  addAction(): void {
    const cible = this.dossierDAjout;
    const source = this.source;
    if (!cible || !source || !this.ajoutAutorise) {
      return;
    }
    const nature: CapaActionType | undefined =
      this.natures.length === 1 ? this.natures[0] : undefined;
    const donnees: CapaCreateDialogData = {
      source,
      prefill: {
        type: nature ?? 'CORRECTIVE',
        criticity: cible.criticity
      },
      lockType: !!nature,
      open: request => this.capaService.openCase(request).pipe(
        map(ouvert => ({ capaId: ouvert.id })))
    };
    this.dialog
      .open<CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened>(
        CapaCreateDialogComponent, {
          data: donnees, autoFocus: 'first-tabbable', restoreFocus: true,
          panelClass: 'qos-dialog-panel'
        })
      .afterClosed()
      .subscribe(ouvert => {
        // Rien n'a été créé si la fenêtre a été refermée : on ne relit pas.
        if (ouvert) {
          this.changed.emit();
        }
      });
  }

  /**
   * Ouvre une action pour la relire et la corriger.
   *
   * <p>Un dossier terminé s'ouvre en lecture seule plutôt que de ne pas
   * s'ouvrir : relire ce qui a été fait est légitime après la clôture, et une
   * ligne qui ne réagit pas au clic passe pour une panne.
   */
  openAction(action: CapaActionResponse): void {
    const porteur = this.dossierDe(action);
    if (!porteur) {
      return;
    }
    this.openDialog({
      caseId: porteur.id,
      action,
      ownerId: porteur.ownerId,
      ownerName: porteur.ownerName,
      dueDate: porteur.dueDate,
      team: this.team,
      effectivenessCriterion: porteur.effectivenessCriterion,
      verificationRequired: porteur.verificationRequired,
      readOnly: this.isTerminal(porteur.status)
    });
  }

  /** Le titre et la mention lue par les lecteurs d'écran, écrits une seule fois. */
  readonly titreEcheanceAvantDebut =
    $localize`:@@capa.actions.due-before-start-title:L'échéance précède la date de début.`;

  /**
   * Vrai quand l'échéance de cette action précède sa date de début.
   *
   * <p>Comparaison de chaînes et non de dates : les deux champs sont des jours
   * au format ISO (`2026-11-18`), où l'ordre alphabétique EST l'ordre
   * chronologique. Passer par `new Date()` ferait entrer le fuseau du
   * navigateur dans une comparaison de jours, et deux dates égales pourraient
   * s'ordonner.
   */
  echeanceAvantDebut(action: CapaActionResponse): boolean {
    return !!action.decidedOn && !!action.dueDate && action.dueDate < action.decidedOn;
  }

  private openDialog(data: CapaActionDialogData): void {
    this.dialog
      .open<CapaActionDialogComponent, CapaActionDialogData, boolean>(CapaActionDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(saved => {
        if (saved) {
          this.reload();
          this.changed.emit();
        }
      });
  }
}
