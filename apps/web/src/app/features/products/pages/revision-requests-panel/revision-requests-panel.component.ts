import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';

import { ProductsService } from '../../products.service';
import { RevisionRequestView } from '../../products.types';

/**
 * Les révisions que la boucle NC / CAPA propose sur ce produit.
 *
 * <p>La justification est affichée en clair : une proposition qu'on ne peut pas
 * contester n'est pas une proposition qu'on peut accepter en conscience. Et le
 * refus exige une note — le bouton reste désactivé tant qu'elle est vide, parce
 * que « on n'a pas bougé » sans raison écrite est précisément l'écart qu'un
 * auditeur cherche.
 */
@Component({
  selector: 'qos-revision-requests-panel',
  templateUrl: './revision-requests-panel.component.html',
  styleUrls: ['./revision-requests-panel.component.scss'],
  standalone: false
})
export class RevisionRequestsPanelComponent implements OnInit {

  @Input() productId = '';
  @Output() decided = new EventEmitter<void>();

  requests: RevisionRequestView[] = [];
  /** Note de refus saisie, par identifiant de demande. */
  notes: Record<string, string> = {};
  loading = false;
  deciding = '';

  constructor(
    private readonly service: ProductsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : le patron
    // queueMicrotask ne vaut que pour un Subject poussé depuis une souscription.
    // Ici il produirait l'inverse — un flux synchrone rendrait la main avant la
    // micro-tâche, qui rallumerait le voyant sur une liste déjà chargée.
    this.loading = true;
    this.service.revisionRequests(this.productId).subscribe({
      next: requests => { this.requests = requests; this.loading = false; },
      error: () => this.fail($localize`:@@revision.load-failed:Propositions indisponibles.`)
    });
  }

  canReject(request: RevisionRequestView): boolean {
    return (this.notes[request.id] ?? '').trim().length > 0;
  }

  accept(request: RevisionRequestView): void {
    this.deciding = request.id;
    this.service.acceptRevision(request.id).subscribe({
      next: () => this.afterDecision(),
      error: err => {
        if (this.isStepUpRequired(err)) {
          this.askForSecondFactor();
          return;
        }
        this.fail(err?.status === 409
          ? $localize`:@@revision.already-decided:Cette proposition a déjà été tranchée.`
          : $localize`:@@revision.accept-failed:Acceptation impossible.`);
      }
    });
  }

  /**
   * Accepter écrit dans un document approuvé : le serveur exige un second
   * facteur. La session reste valide — c'est son palier qui ne suffit pas.
   */
  private isStepUpRequired(err: unknown): boolean {
    const problem = err as { status?: number; error?: { type?: string } };
    return problem?.status === 403
        && problem?.error?.type === 'https://qualitos.io/errors/step-up-required';
  }

  private askForSecondFactor(): void {
    this.deciding = '';
    const snack = this.snack.open(
      $localize`:@@stepup.required:Cette signature exige votre code à usage unique.`,
      $localize`:@@stepup.reauthenticate:Se réauthentifier`,
      { duration: 10000 });
    snack.onAction().subscribe(() => {
      if (!this.auth.stepUp(`/products/${this.productId}`)) {
        this.fail($localize`:@@stepup.unavailable:Second facteur indisponible sur cet environnement.`);
      }
    });
  }

  reject(request: RevisionRequestView): void {
    if (!this.canReject(request)) return;
    this.deciding = request.id;
    this.service.rejectRevision(request.id, this.notes[request.id].trim()).subscribe({
      next: () => this.afterDecision(),
      error: () => this.fail($localize`:@@revision.reject-failed:Refus impossible.`)
    });
  }

  /** Un intitulé lisible de ce que la proposition changerait. */
  summary(request: RevisionRequestView): string {
    if (request.field) return `${request.field} : ${request.from} → ${request.to}`;
    return request.targetType === 'PFMEA_ITEM_CREATE'
      ? $localize`:@@revision.create-pfmea:Créer une ligne de PFMEA`
      : $localize`:@@revision.create-line:Créer une ligne de control plan`;
  }

  private afterDecision(): void {
    this.deciding = '';
    this.decided.emit();
    this.reload();
  }

  private fail(message: string): void {
    this.loading = false;
    this.deciding = '';
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
