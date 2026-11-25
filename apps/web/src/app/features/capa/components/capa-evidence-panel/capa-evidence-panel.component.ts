import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { CapaService } from '../../capa.service';
import { CapaEvidence, CapaStatus } from '../../capa.types';

/** Au-delà, un dossier ne se relit plus ; le serveur applique la même borne. */
const MAX_PIECES = 10;

/**
 * Les preuves DU DOSSIER : ce qui démontre que le problème a cessé.
 *
 * <p>À ne pas confondre avec la pièce d'une ACTION (ADR 0052), qui atteste que
 * tel geste a été fait et vit dans la colonne « Preuve » du tableau du D3. Les
 * deux coexistent et ne se remplacent pas : « le tri a été fait » n'est pas
 * « le client ne reçoit plus de palettes sans étiquette » (ISO 9001 §10.2).
 *
 * <p>Plusieurs pièces, sans action visée : c'est le dépôt libre du D6 du modèle
 * de traitement 8D.
 */
@Component({
  selector: 'qos-capa-evidence-panel',
  templateUrl: './capa-evidence-panel.component.html',
  styleUrls: ['./capa-evidence-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class CapaEvidencePanelComponent implements OnChanges {

  @Input() capaCaseId = '';

  /** Le statut du dossier : clos, ses preuves se relisent mais ne changent plus. */
  @Input() statut: CapaStatus | undefined;

  /**
   * Tait les phrases qui EXPLIQUENT le dépôt, et ne garde que les pièces.
   *
   * <p>Le rapport 8D rappelle au D6 le titre de l'action et celui de ses
   * pièces, rien d'autre : trois panneaux de preuve y répétaient chacun ce
   * qu'est une preuve, ce qu'il advient d'un dossier clos et ce que vaut un
   * dossier sans pièce — un paragraphe d'aide par dossier, dans un document
   * remis au client.
   *
   * <p>Sur l'écran du dossier CAPA, où la preuve se DÉPOSE, ces phrases
   * restent : c'est là qu'elles servent.
   */
  @Input() sansAide = false;

  /**
   * Combien de pièces ce panneau porte, dès qu'il a fini de lire.
   *
   * <p>Même convention que le panneau d'actions, et pour la même raison : le
   * sommaire des huit disciplines coche « preuves » d'après ce nombre, et seul
   * le panneau le connaît — la fiche de l'écart sait quels dossiers existent,
   * pas ce qu'ils portent.
   *
   * <p>Zéro est une valeur : « lu, et il n'y en a aucune » n'est pas « pas
   * encore lu », et le sommaire ne doit pas cocher la seconde.
   */
  @Output() readonly counted = new EventEmitter<number>();

  readonly pieces$ = new BehaviorSubject<CapaEvidence[]>([]);
  readonly chargement$ = new BehaviorSubject<boolean>(false);
  readonly envoi$ = new BehaviorSubject<boolean>(false);

  /** Le stockage est coupé sur l'environnement : on le dit, sans crier à la panne. */
  stockageCoupe = false;

  /** Ce qui empêche de lire les pièces, dit plutôt que laissé en blanc. */
  erreur: string | null = null;

  constructor(
    private readonly capa: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['capaCaseId'] && this.capaCaseId) {
      this.charger();
    }
  }

  private charger(): void {
    this.chargement$.next(true);
    this.erreur = null;
    this.capa.listEvidences(this.capaCaseId).pipe(
      finalize(() => { this.chargement$.next(false); this.cdr.markForCheck(); })
    ).subscribe({
      next: pieces => {
        // Les pièces d'ACTION ne sont pas celles du dossier : elles portent une
        // action visée, et elles se lisent dans le tableau du D3.
        const duDossier = pieces.filter(p => !p.actionId);
        this.pieces$.next(duDossier);
        this.counted.emit(duDossier.length);
        this.cdr.markForCheck();
      },
      error: err => {
        if (this.estStockageCoupe(err)) {
          this.stockageCoupe = true;
        } else {
          this.erreur = safeErrorMessage(err,
            $localize`:@@capa.evidence.load-error:Les preuves n'ont pas pu être lues.`);
        }
        // Lu, meme en echec : le sommaire ne doit pas rester en attente d'un
        // nombre qui ne viendra plus. Zero dit « rien a montrer ici », ce qui
        // est exactement ce qu'on sait.
        this.counted.emit(0);
        this.cdr.markForCheck();
      }
    });
  }

  /** Vrai quand une pièce de plus est possible : dossier ouvert, et place restante. */
  get peutDeposer(): boolean {
    return !this.stockageCoupe
      && !this.estTermine
      && this.pieces$.value.length < MAX_PIECES;
  }

  get estTermine(): boolean {
    return this.statut === 'CLOSED' || this.statut === 'REJECTED';
  }

  get estPlein(): boolean {
    return this.pieces$.value.length >= MAX_PIECES;
  }

  choisirFichier(input: HTMLInputElement): void {
    if (!this.envoi$.value) {
      input.click();
    }
  }

  fichierChoisi(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files && input.files[0];
    // Vidé tout de suite : rechoisir le MÊME fichier après un refus ne
    // déclencherait aucun évènement si la valeur restait en place.
    input.value = '';
    if (!fichier || !this.capaCaseId) {
      return;
    }
    this.envoi$.next(true);
    this.capa.uploadEvidence(this.capaCaseId, fichier).pipe(
      finalize(() => { this.envoi$.next(false); this.cdr.markForCheck(); })
    ).subscribe({
      next: piece => {
        this.pieces$.next([...this.pieces$.value, piece]);
        this.counted.emit(this.pieces$.value.length);
        this.snack.open($localize`:@@capa.evidence.added:Preuve jointe au dossier.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.cdr.markForCheck();
      },
      error: err => {
        if (this.estStockageCoupe(err)) {
          this.stockageCoupe = true;
          this.cdr.markForCheck();
          return;
        }
        this.snack.open(this.refus(err), $localize`:@@common.ok:OK`, { duration: 5000 });
      }
    });
  }

  retirer(piece: CapaEvidence): void {
    if (this.envoi$.value) {
      return;
    }
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@capa.evidence.remove-title:Retirer cette preuve ?`,
        message: $localize`:@@capa.evidence.remove-message:La pièce sera définitivement retirée du dossier. Ce retrait est traçable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirme => {
      if (!confirme) {
        return;
      }
      this.envoi$.next(true);
      this.capa.deleteEvidence(this.capaCaseId, piece.id).pipe(
        finalize(() => { this.envoi$.next(false); this.cdr.markForCheck(); })
      ).subscribe({
        next: () => {
          this.pieces$.next(this.pieces$.value.filter(p => p.id !== piece.id));
          this.counted.emit(this.pieces$.value.length);
          this.cdr.markForCheck();
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@capa.evidence.remove-failed:La preuve n'a pas été retirée.`),
          $localize`:@@common.ok:OK`, { duration: 5000 })
      });
    });
  }

  /** L'icône par famille de document : on reconnaît un PDF d'une photo au premier coup d'œil. */
  icone(contentType: string): string {
    if (contentType?.startsWith('image/')) return 'image';
    if (contentType === 'application/pdf') return 'picture_as_pdf';
    if (contentType?.includes('spreadsheet') || contentType?.includes('excel')) return 'table_chart';
    return 'description';
  }

  /** Le poids en unités lisibles : « 2,4 Mo » se lit, « 2516582 » non. */
  poids(octets: number): string {
    return octets < 1048576
      ? Math.max(1, Math.round(octets / 1024)) + ' Ko'
      : (octets / 1048576).toFixed(1) + ' Mo';
  }

  retirerAria(piece: CapaEvidence): string {
    return $localize`:@@capa.evidence.remove-aria:Retirer la preuve ${piece.originalFilename || piece.id}:nom:`;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private estStockageCoupe(err: any): boolean {
    return err?.status === 501 || err?.error?.type?.includes?.('storage-unavailable');
  }

  private refus(err: unknown): string {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    return e?.status === 413
      ? $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
      : e?.status === 400
      ? $localize`:@@capa.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
      : e?.status === 409
      ? $localize`:@@capa.evidence.full:Dix preuves, c'est la limite : retirez-en une pour en joindre une autre.`
      : safeErrorMessage(err, $localize`:@@capa.evidence.error:Échec de l'ajout de la preuve.`);
  }
}
