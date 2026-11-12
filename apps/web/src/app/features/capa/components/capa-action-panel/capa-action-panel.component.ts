import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { actionTypeLabel } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaActionStatus, CapaActionType, CapaCaseResponse, CapaEvidence, CapaStatus
} from '../../capa.types';
import {
  CapaActionDialogComponent, CapaActionDialogData
} from '../../pages/capa-action-dialog/capa-action-dialog.component';

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
 * <p><b>Un dossier porte plusieurs actions</b> (ADR 0115, qui révise l'ADR 0088
 * sur ce seul point) : on endigue, on corrige la cause, on prévient la récidive,
 * et le 8D nomme ces étapes séparément (D3, D5, D6). Le tableau les montre
 * toutes, chacune avec son porteur, son échéance et son avancement ; une ligne
 * s'ouvre d'un clic pour la corriger, et le bouton d'ajout en ajoute une.
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

  /** Le dossier à lire. Le panneau le charge lui-même : l'appelant n'a que l'identifiant. */
  @Input() capaCaseId = '';

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

  /** Émis après chaque changement enregistré : l'écran appelant peut se relire. */
  @Output() readonly changed = new EventEmitter<void>();

  capa: CapaCaseResponse | null = null;
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
    if (!this.capaCaseId) {
      this.capa = null;
      this.error = null;
      this.cdr.markForCheck();
      return;
    }
    this.error = null;
    this.loading$.next(true);
    this.capaService.getCase(this.capaCaseId).pipe(
      finalize(() => this.loading$.next(false))
    ).subscribe({
      next: capa => {
        this.capa = capa;
        this.cdr.markForCheck();
        this.chargerPreuves();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] getCase failed', err?.status, err?.error?.title);
        this.capa = null;
        this.error = safeErrorMessage(
          err, $localize`:@@capa.action-panel.failed:L'action de la CAPA n'a pas pu être lue.`);
        this.cdr.markForCheck();
      }
    });
  }

  // ---------- lecture ----------

  get actions(): CapaActionResponse[] {
    const toutes = this.capa?.actions ?? [];
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
    if (!this.capaCaseId) {
      return;
    }
    this.capaService.listActionEvidences(this.capaCaseId).subscribe({
      next: liste => {
        const par = new Map<string, CapaEvidence>();
        // Le serveur garantit une pièce par action ; on garde malgré tout la
        // dernière si deux arrivaient, plutôt que d'afficher une case vide.
        liste.forEach(e => { if (e.actionId) { par.set(e.actionId, e); } });
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

  /** Le dépôt suit le verrou du dossier : plus rien ne bouge une fois clos ou rejeté. */
  peutJoindre(actionId: string, statutDossier: CapaStatus): boolean {
    return !this.isTerminal(statutDossier)
      && !this.stockageCoupe
      && !this.preuve(actionId)
      && this.preuveEnCours !== actionId;
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
    if (!fichier || !actionId || !this.capa) {
      return;
    }
    this.preuveEnCours = actionId;
    this.cdr.markForCheck();
    this.capaService.uploadActionEvidence(this.capa.id, actionId, fichier).pipe(
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

  retirerPreuve(action: CapaActionResponse, piece: CapaEvidence): void {
    if (this.preuveEnCours || !this.capa) {
      return;
    }
    const dossier = this.capa.id;
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

  // ---------- avancer ----------

  nextStatus(status: CapaActionStatus): CapaActionStatus | null {
    return SUITE[status];
  }

  canAdvance(action: CapaActionResponse): boolean {
    return !!this.capa
      && action.status !== 'DONE'
      && !this.isTerminal(this.capa.status)
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
    if (!suivant || !this.capa || this.acting$.value) {
      return;
    }
    this.acting$.next(true);
    this.capaService.updateAction(this.capa.id, action.id, { status: suivant }).pipe(
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
   * Ajoute une action au dossier.
   *
   * <p>Le pilote et l'échéance du dossier sont proposés — c'est le cas le plus
   * fréquent — et restent modifiables : une action préventive portée par
   * quelqu'un d'autre, pour plus tard, est exactement ce que la liste permet
   * désormais d'écrire.
   */
  addAction(): void {
    if (!this.capa || this.isTerminal(this.capa.status)) {
      return;
    }
    this.openDialog({
      caseId: this.capa.id,
      ownerId: this.capa.ownerId,
      ownerName: this.capa.ownerName,
      dueDate: this.capa.dueDate,
      // Tableau restreint à une nature : l'action ajoutée là est de cette nature.
      actionType: this.natures.length === 1 ? this.natures[0] : undefined
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
    if (!this.capa) {
      return;
    }
    this.openDialog({
      caseId: this.capa.id,
      action,
      ownerId: this.capa.ownerId,
      ownerName: this.capa.ownerName,
      dueDate: this.capa.dueDate,
      readOnly: this.isTerminal(this.capa.status)
    });
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
