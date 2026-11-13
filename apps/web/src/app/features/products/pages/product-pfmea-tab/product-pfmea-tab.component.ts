import { Component, Input, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FmeaService } from '../../../fmea/fmea.service';
import { FmeaItemDialogComponent } from '../../../fmea/pages/fmea-item-dialog/fmea-item-dialog.component';
import {
  FmeaReferenceDialogComponent
} from '../../../fmea/pages/fmea-reference-dialog/fmea-reference-dialog.component';
import { ActionPriority, FmeaItemResponse, FmeaProjectResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../products.service';
import { RevisionRequestView } from '../../products.types';

/**
 * L'analyse de risque du produit, triée par priorité d'action — et modifiable.
 *
 * <p>Le tri se fait par AP décroissante PUIS par RPN décroissant. Trier par le
 * seul RPN reproduirait exactement le défaut que l'AP corrige : le produit des
 * trois notes donne le même 120 pour une défaillance grave et pour une
 * défaillance fréquente et bénigne.
 *
 * <p><b>Les lignes s'ajoutent, se modifient et se suppriment ici</b>, avec le
 * dialogue du module FMEA ({@link FmeaItemModule}) et non une copie : une règle
 * de cotation ajoutée là-bas vaut ici, et la même saisie ne peut pas donner deux
 * RPN différents selon la porte prise. L'onglet était auparavant en lecture
 * seule, ce qui obligeait à quitter la fiche produit, retrouver le projet dans
 * le module FMEA et y revenir — pour corriger une note de 4 en 5.
 *
 * <p>Ce que cet onglet garde en propre, et que l'écran FMEA n'a pas : la
 * priorité d'action (AP) qui porte le tri, et le drapeau des lignes visées par
 * une demande de révision du produit.
 */
@Component({
  selector: 'qos-product-pfmea-tab',
  templateUrl: './product-pfmea-tab.component.html',
  styleUrls: ['./product-pfmea-tab.component.scss'],
  standalone: false
})
export class ProductPfmeaTabComponent implements OnInit {

  @Input() productId = '';

  readonly columns = [
    'sequenceNo', 'function', 'failureMode', 'failureEffect',
    'sod', 'rpn', 'sodAfter', 'rpnAfter', 'ap', 'flag', 'actions'
  ];

  project?: FmeaProjectResponse;
  items: FmeaItemResponse[] = [];
  /** Identifiants des lignes visées par une proposition en attente. */
  flagged = new Set<string>();
  loading = false;

  private static readonly PRIORITY_ORDER: Record<ActionPriority, number> = {
    HIGH: 0, MEDIUM: 1, LOW: 2
  };

  constructor(
    private readonly fmea: FmeaService,
    private readonly products: ProductsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  /**
   * Ouvre le référentiel de cotation : les barèmes S/O/D du tenant et l'exemple
   * de PFMEA.
   *
   * <p>Il s'ouvre ICI parce que c'est ici qu'on cote. Renvoyer l'évaluateur
   * chercher l'échelle ailleurs revient à lui demander un chiffre de 1 à 10
   * sans lui dire ce que le 8 signifie dans SON organisation.
   */
  openReference(): void {
    this.dialog.open(FmeaReferenceDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: ['qos-dialog-panel', 'qos-dialog-panel--wide']
    });
  }

  ngOnInit(): void {
    this.load();
  }

  /**
   * Un PFMEA archivé se lit mais ne se modifie plus, et l'absence de projet
   * n'est pas un projet vide : dans les deux cas la saisie est fermée.
   */
  get editable(): boolean {
    return !!this.project && this.project.status !== 'ARCHIVED';
  }

  /**
   * Une ligne au-delà du seuil du projet peut devenir un risque du registre.
   *
   * <p>Le seuil est relu sur le PROJET et non sur le drapeau `critical` porté
   * par la ligne : ce drapeau a été calculé à l'écriture, et le seuil du projet
   * a pu bouger depuis. Le serveur revérifie de toute façon.
   */
  isCritical(item: FmeaItemResponse): boolean {
    return !!this.project && item.rpn >= this.project.criticalRpnThreshold;
  }

  openAddItem(): void {
    if (!this.project) return;
    const ref = this.dialog.open(FmeaItemDialogComponent, {
      data: { projectId: this.project.id, criticalRpnThreshold: this.project.criticalRpnThreshold },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => { if (created) this.load(); });
  }

  openEditItem(item: FmeaItemResponse): void {
    if (!this.project) return;
    const ref = this.dialog.open(FmeaItemDialogComponent, {
      data: { projectId: this.project.id, item, criticalRpnThreshold: this.project.criticalRpnThreshold },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.load(); });
  }

  removeItem(item: FmeaItemResponse): void {
    const projet = this.project;
    if (!projet) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@fmea.detail.delete-item-title:Supprimer l'item ?`,
        message: $localize`:@@fmea.detail.delete-item-message:Item #${item.sequenceNo}:seq: (RPN = ${item.rpn}:rpn:) sera supprimé.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.fmea.deleteItem(projet.id, item.id).subscribe({
        // On RECHARGE plutôt que de retirer la ligne du tableau : les rangs et
        // le marquage de révision viennent du serveur, et une liste rafistolée
        // localement finirait par diverger de ce qu'il porte.
        next: () => this.load(),
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@fmea.detail.delete-item-failed:Suppression impossible.`),
          $localize`:@@common.ok:OK`, { duration: 4000 })
      });
    });
  }

  isFlagged(item: FmeaItemResponse): boolean {
    return this.flagged.has(item.id);
  }

  tone(priority?: ActionPriority): string {
    if (priority === 'HIGH') return 'danger';
    if (priority === 'MEDIUM') return 'warning';
    return 'neutral';
  }

  /** Charge le projet, ses lignes et les demandes de révision qui les visent. */
  private load(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : le patron
    // queueMicrotask ne vaut que pour un Subject poussé depuis une souscription.
    // Ici il produirait l'inverse — un flux synchrone rendrait la main avant la
    // micro-tâche, qui rallumerait le voyant sur une liste déjà chargée.
    this.loading = true;
    forkJoin({
      items: this.fmea.list(0, 50, undefined, 'PROCESS_FMEA', this.productId).pipe(
        switchMap(page => {
          this.project = page.content.find(p => p.status === 'ACTIVE') ?? page.content[0];
          return this.project
            ? this.fmea.listItems(this.project.id).pipe(map(items => items.content))
            : of([] as FmeaItemResponse[]);
        })
      ),
      // Le marquage est un confort : s'il tombe, le tableau reste lisible.
      requests: this.products.revisionRequests(this.productId).pipe(
        catchError(() => of([] as RevisionRequestView[])))
    }).subscribe({
      next: ({ items, requests }) => {
        this.items = this.sorted(items);
        this.flagged = new Set(requests
          .filter(request => request.targetType === 'PFMEA_ITEM' && !!request.targetId)
          .map(request => request.targetId as string));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open($localize`:@@pfmea.load-failed:PFMEA indisponible.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  private sorted(items: FmeaItemResponse[]): FmeaItemResponse[] {
    return [...items].sort((a, b) => {
      const byPriority = this.rank(a.actionPriority) - this.rank(b.actionPriority);
      return byPriority !== 0 ? byPriority : b.rpn - a.rpn;
    });
  }

  /** Une ligne non cotée passe en dernier : elle ne dit rien, pas même « faible ». */
  private rank(priority?: ActionPriority): number {
    return priority ? ProductPfmeaTabComponent.PRIORITY_ORDER[priority] : 3;
  }
}
