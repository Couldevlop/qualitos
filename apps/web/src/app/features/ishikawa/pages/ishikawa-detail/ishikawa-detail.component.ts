import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FishboneBranchInput } from '../../components/ishikawa-fishbone/ishikawa-fishbone.layout';
import { IshikawaService } from '../../ishikawa.service';
import {
  CauseCategory,
  IshikawaActionResponse,
  IshikawaActionStatus,
  IshikawaCauseResponse,
  IshikawaDiagramResponse,
  IshikawaStatus,
  SuggestedCause
} from '../../ishikawa.types';
import {
  IshikawaCauseDialogComponent,
  IshikawaCauseDialogData
} from '../ishikawa-cause-dialog/ishikawa-cause-dialog.component';
import {
  IshikawaEditDialogComponent,
  IshikawaEditDialogData
} from '../ishikawa-edit-dialog/ishikawa-edit-dialog.component';

// OWASP A03 — refuse malformed route params client-side.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface CauseNode {
  cause: IshikawaCauseResponse;
  children: CauseNode[];
}

interface BranchView {
  category: CauseCategory;
  label: string;
  /** Top-level causes only (no parentId); children nested via tree(). */
  roots: CauseNode[];
}

@Component({
  selector: 'qos-ishikawa-detail',
  templateUrl: './ishikawa-detail.component.html',
  styleUrls: ['./ishikawa-detail.component.scss'],
  standalone: false
})
export class IshikawaDetailComponent implements OnInit {

  diagram$!: Observable<IshikawaDiagramResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  // Suggestions de causes par l'IA (non persistées tant que l'utilisateur ne les ajoute pas).
  suggestions: SuggestedCause[] = [];
  suggesting = false;
  addingKey: string | null = null;
  converting = false;

  // ---- Plan d'actions (§3.5) --------------------------------------------------
  //
  // Un diagramme s'arrêtait aux causes : les décisions prises devant lui vivaient
  // dans un compte rendu, un tableur, une mémoire. Le tableau les ramène là où
  // elles ont été prises, et se modifie cellule par cellule.

  actions: IshikawaActionResponse[] = [];
  actionsLoading = false;
  /** Intitulé de l'action en cours de saisie dans la ligne d'ajout. */
  newActionLabel = '';
  newActionResponsible = '';
  newActionDecidedOn = '';
  /** Action dont le libellé est en cours d'édition (une seule à la fois). */
  editingId: string | null = null;
  editLabel = '';
  /** Ligne en attente de réponse du serveur : évite le double envoi. */
  pendingActionId: string | null = null;

  readonly actionStatuses: IshikawaActionStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

  private diagramId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  /** Arbre des branches mémorisé (cf. `branches`), indexé par référence. */
  private branchCache: {
    source: IshikawaDiagramResponse;
    view: BranchView[];
    bones: FishboneBranchInput[];
  } | null = null;

  private readonly CATEGORY_LABELS: Record<CauseCategory, string> = {
    METHODS: 'Méthodes', MANPOWER: 'Main-d\'œuvre', MACHINES: 'Machines',
    MATERIALS: 'Matières', MEASUREMENTS: 'Mesures', ENVIRONMENT: 'Milieu',
    MANAGEMENT: 'Management', MONEY: 'Moyens financiers'
  };

  // For accepting either a UUID or the demo mock-store ids ("ish-1" …) so
  // the page works both against the real backend AND useMockApi=true.
  // Ancré des DEUX côtés et jeu de caractères restreint : un simple préfixe
  // laissait passer « ish-1/../../secrets » (traversée de chemin — OWASP A03).
  private isMockId(s: string): boolean {
    return /^ish-[a-z0-9-]{1,64}$/i.test(s);
  }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly ishikawa: IshikawaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/ishikawa']);
      return;
    }
    this.diagramId = raw;
    this.diagram$ = this.reload$.pipe(
      // Le cache de branches est indexé par référence ; on le vide quand même
      // ici, au cas où le serveur (ou le magasin de démonstration) renverrait
      // le même objet muté — un dessin figé serait pire qu'un recalcul.
      tap(() => { this.branchCache = null; this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.ishikawa.getDiagram(this.diagramId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-detail] getDiagram failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@ishikawa.detail.not-found:Diagramme introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    this.loadActions();
  }

  // ---- Plan d'actions ---------------------------------------------------------

  private loadActions(): void {
    this.actionsLoading = true;
    this.ishikawa.listActions(this.diagramId).subscribe({
      next: list => { this.actions = list; this.actionsLoading = false; },
      error: () => {
        this.actionsLoading = false;
        this.snack.open(
          $localize`:@@ishikawa.actions.load-failed:Impossible de charger le plan d'actions.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  addAction(): void {
    const label = this.newActionLabel.trim();
    if (!label) {
      return;   // une ligne muette n'apporte rien au plan
    }
    this.ishikawa.addAction(this.diagramId, {
      label,
      responsible: this.newActionResponsible.trim() || undefined,
      decidedOn: this.newActionDecidedOn || undefined
    }).subscribe({
      next: created => {
        this.actions = [...this.actions, created];
        // On vide la ligne de saisie : sans cela, un second clic recréerait la
        // même action à l'identique.
        this.newActionLabel = '';
        this.newActionResponsible = '';
        this.newActionDecidedOn = '';
      },
      error: () => this.snack.open(
        $localize`:@@ishikawa.actions.add-failed:Ajout de l'action refusé.`,
        $localize`:@@common.ok:OK`, { duration: 4000 })
    });
  }

  startEdit(action: IshikawaActionResponse): void {
    this.editingId = action.id;
    this.editLabel = action.label;
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editLabel = '';
  }

  /** Valide la cellule : n'envoie que si le texte a réellement changé. */
  commitEdit(action: IshikawaActionResponse): void {
    const label = this.editLabel.trim();
    if (!label) {
      this.snack.open(
        $localize`:@@ishikawa.actions.label-required:L'intitulé de l'action est obligatoire.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    if (label === action.label) {
      this.cancelEdit();   // un clic à côté ne doit pas produire un appel inutile
      return;
    }
    this.pendingActionId = action.id;
    this.ishikawa.updateAction(action.id, { label }).subscribe({
      next: updated => {
        this.replace(updated);
        this.pendingActionId = null;
        this.cancelEdit();
      },
      error: () => {
        this.pendingActionId = null;
        this.cancelEdit();
        this.snack.open(
          $localize`:@@ishikawa.actions.update-failed:Modification refusée.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  changeStatus(action: IshikawaActionResponse, status: IshikawaActionStatus): void {
    const previous = action.status;
    action.status = status;   // réponse immédiate à l'œil
    this.pendingActionId = action.id;
    this.ishikawa.updateAction(action.id, { status }).subscribe({
      next: updated => { this.replace(updated); this.pendingActionId = null; },
      error: () => {
        // Laisser « fait » à l'écran alors que rien n'est enregistré serait pire
        // que l'erreur : l'utilisateur croirait son action close.
        action.status = previous;
        this.pendingActionId = null;
        this.snack.open(
          $localize`:@@ishikawa.actions.update-failed:Modification refusée.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  changeResponsible(action: IshikawaActionResponse, responsible: string): void {
    const value = responsible.trim();
    if (value === (action.responsible ?? '')) {
      return;
    }
    this.ishikawa.updateAction(action.id, { responsible: value }).subscribe({
      next: updated => this.replace(updated),
      error: () => this.snack.open(
        $localize`:@@ishikawa.actions.update-failed:Modification refusée.`,
        $localize`:@@common.ok:OK`, { duration: 4000 })
    });
  }

  changeDecidedOn(action: IshikawaActionResponse, value: string): void {
    // Le champ date natif rend déjà AAAA-MM-JJ : aucune conversion, donc aucun
    // décalage de fuseau à craindre. Une date effacée ne déclenche rien.
    if (!value || value === (action.decidedOn ?? '')) {
      return;
    }
    this.ishikawa.updateAction(action.id, { decidedOn: value }).subscribe({
      next: updated => this.replace(updated),
      error: () => this.snack.open(
        $localize`:@@ishikawa.actions.update-failed:Modification refusée.`,
        $localize`:@@common.ok:OK`, { duration: 4000 })
    });
  }

  deleteAction(action: IshikawaActionResponse): void {
    this.ishikawa.deleteAction(action.id).subscribe({
      next: () => { this.actions = this.actions.filter(a => a.id !== action.id); },
      error: () => this.snack.open(
        $localize`:@@ishikawa.actions.delete-failed:Suppression refusée.`,
        $localize`:@@common.ok:OK`, { duration: 4000 })
    });
  }

  actionStatusLabel(status: IshikawaActionStatus): string {
    const labels: Record<IshikawaActionStatus, string> = {
      TODO: $localize`:@@ishikawa.actions.todo:À faire`,
      IN_PROGRESS: $localize`:@@ishikawa.actions.in-progress:En cours`,
      DONE: $localize`:@@ishikawa.actions.done:Fait`
    };
    return labels[status];
  }

  private replace(updated: IshikawaActionResponse): void {
    this.actions = this.actions.map(a => (a.id === updated.id ? updated : a));
  }


  goBack(): void {
    this.router.navigate(['/ishikawa']);
  }

  /**
   * OWASP A04 — destructive actions go through a confirm dialog. The backend
   * also revalidates ownership via the JWT tenant_id claim; the front guard
   * is UX defense-in-depth (prevent accidental clicks).
   */
  deleteDiagram(label: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@ishikawa.detail.delete-confirm-title:Supprimer ce diagramme ?`,
        message: `« ${label} » et toutes ses causes seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.ishikawa.deleteDiagram(this.diagramId).subscribe({
        next: () => {
          this.snack.open($localize`:@@ishikawa.detail.deleted:Diagramme supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/ishikawa']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`),
            'OK', { duration: 4000 }
          );
        }
      });
    });
  }

  openEdit(d: IshikawaDiagramResponse): void {
    const data: IshikawaEditDialogData = { diagram: d };
    this.dialog
      .open(IshikawaEditDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => {
        if (updated) this.reload$.next();
      });
  }

  openAddCause(d: IshikawaDiagramResponse): void {
    const data: IshikawaCauseDialogData = { diagramId: d.id, mode: d.mode };
    this.dialog
      .open(IshikawaCauseDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(cause => {
        if (cause) {
          this.reload$.next();
        }
      });
  }

  /** Add a sub-cause (5-Whys child) of an existing cause. */
  /**
   * Une cause a été activée SUR LE DESSIN (clic, « Entrée », barre d'espace).
   *
   * <p>Le diagramme n'émet qu'un identifiant : il ne connaît ni le dialogue, ni
   * l'arbre complet — il ne dessine que le premier niveau. C'est donc ici qu'on
   * retrouve la cause, dans les données déjà chargées.
   *
   * <p>Une cause introuvable est ignorée en silence, et c'est voulu : le seul
   * cas où cela arrive est une activation qui arrive après un rechargement qui
   * l'a supprimée. Ouvrir un dialogue sur une cause disparue, ou afficher une
   * erreur pour un clic devenu sans objet, serait pire que ne rien faire.
   */
  onCauseActivate(d: IshikawaDiagramResponse, causeId: string): void {
    const cause = d.causes.find(c => c.id === causeId);
    if (cause) {
      this.openAddSubCause(d, cause);
    }
  }

  openAddSubCause(d: IshikawaDiagramResponse, parent: IshikawaCauseResponse): void {
    const data: IshikawaCauseDialogData = {
      diagramId: d.id,
      mode: d.mode,
      parent: { id: parent.id, label: parent.label, category: parent.category }
    };
    this.dialog
      .open(IshikawaCauseDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(cause => {
        if (cause) this.reload$.next();
      });
  }

  // ---- Suggestion de causes par l'IA (§3.5) ----

  /**
   * Convertit le diagramme en cycle PDCA (§3.6 — référentiel commun) et navigue vers
   * le cycle créé. Une cause-racine peut être ciblée (optionnel).
   */
  convertToPdca(d: IshikawaDiagramResponse, causeId?: string): void {
    if (this.converting) {
      return;
    }
    this.converting = true;
    this.ishikawa.convertToPdca(d.id, causeId)
      .pipe(finalize(() => (this.converting = false)))
      .subscribe({
        next: cycle => {
          this.snack.open(
            $localize`:@@ishikawa.detail.convert-success:Cycle PDCA créé à partir de ce diagramme.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.router.navigate(['/pdca', cycle.id]);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-detail] convertToPdca failed', err?.status);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@ishikawa.detail.convert-error:Échec de la conversion en PDCA.`),
            'Fermer', { duration: 4000 });
        }
      });
  }

  suggestCauses(d: IshikawaDiagramResponse): void {
    this.suggesting = true;
    this.suggestions = [];
    this.ishikawa.suggestCauses(d.id).subscribe({
      next: list => {
        this.suggestions = list;
        this.suggesting = false;
        if (!list.length) {
          this.snack.open($localize`:@@ishikawa.detail.no-suggestions:Aucune suggestion exploitable — reformulez le problème.`, $localize`:@@common.ok:OK`, { duration: 3500 });
        }
      },
      error: err => {
        this.suggesting = false;
        // eslint-disable-next-line no-console
        console.warn('[ishikawa-detail] suggestCauses failed', err?.status);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@ishikawa.detail.suggest-unavailable:Suggestion IA indisponible (ai-service / Ollama).`),
          'Fermer', { duration: 4000 });
      }
    });
  }

  /** Ajoute une cause suggérée au diagramme (l'humain valide). */
  addSuggestion(d: IshikawaDiagramResponse, s: SuggestedCause): void {
    this.addingKey = s.label;
    this.ishikawa.addCause(d.id, {
      category: s.category,
      label: s.label,
      description: s.rationale
    }).subscribe({
      next: () => {
        this.addingKey = null;
        this.suggestions = this.suggestions.filter(x => x !== s);
        this.snack.open($localize`:@@ishikawa.detail.cause-added:Cause ajoutée au diagramme.`, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        this.addingKey = null;
        this.snack.open(safeErrorMessage(err, $localize`:@@common.add-failed:Ajout impossible.`), 'Fermer', { duration: 3500 });
      }
    });
  }

  dismissSuggestions(): void {
    this.suggestions = [];
  }

  categoryLabel(c: CauseCategory): string {
    return this.CATEGORY_LABELS[c] ?? c;
  }

  /**
   * Group causes by category and build a parent→children tree per branch.
   *
   * <p>Le résultat est mémorisé sur la RÉFÉRENCE du diagramme : le gabarit
   * appelle cette méthode à chaque cycle de détection, et depuis que l'arête
   * de poisson en consomme la sortie, recalculer l'arbre reviendrait à
   * recalculer aussi tout le tracé (repli de texte compris) à chaque frappe
   * dans le plan d'actions. Le cache tombe de lui-même au rechargement, qui
   * ramène un nouvel objet.
   */
  branches(d: IshikawaDiagramResponse): BranchView[] {
    if (this.branchCache && this.branchCache.source === d) {
      return this.branchCache.view;
    }
    const view = this.computeBranches(d);
    this.branchCache = { source: d, view, bones: toFishboneBranches(view) };
    return view;
  }

  /**
   * Les mêmes branches, réduites à ce que le dessin consomme : premier niveau
   * de causes + nombre de descendants. Le tracé n'a pas besoin de l'arbre
   * complet — il ne le représente pas (cf. arbitrage dans la couche géométrie).
   */
  fishboneBranches(d: IshikawaDiagramResponse): FishboneBranchInput[] {
    this.branches(d);
    return this.branchCache!.bones;
  }

  private computeBranches(d: IshikawaDiagramResponse): BranchView[] {
    const order: { value: CauseCategory; label: string }[] = [
      { value: 'METHODS',      label: 'Méthodes' },
      { value: 'MANPOWER',     label: 'Main-d\'œuvre' },
      { value: 'MACHINES',     label: 'Machines' },
      { value: 'MATERIALS',    label: 'Matières' },
      { value: 'MEASUREMENTS', label: 'Mesures' },
      { value: 'ENVIRONMENT',  label: 'Milieu' }
    ];
    if (d.mode === 'SEVEN_M' || d.mode === 'EIGHT_M') {
      order.push({ value: 'MANAGEMENT', label: 'Management' });
    }
    if (d.mode === 'EIGHT_M') {
      order.push({ value: 'MONEY', label: 'Moyens financiers' });
    }
    const byParent = new Map<string, IshikawaCauseResponse[]>();
    for (const c of d.causes) {
      const key = c.parentId ?? '__root__';
      const arr = byParent.get(key);
      if (arr) arr.push(c); else byParent.set(key, [c]);
    }
    const buildNode = (c: IshikawaCauseResponse): CauseNode => ({
      cause: c,
      children: (byParent.get(c.id) ?? []).map(buildNode)
    });
    return order.map(o => ({
      category: o.value,
      label: o.label,
      roots: (byParent.get('__root__') ?? [])
        .filter(c => c.category === o.value)
        .map(buildNode)
    }));
  }

  statusBadge(status: IshikawaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

}

/** Réduit l'arbre de causes à ce que le dessin sait représenter. */
function toFishboneBranches(view: BranchView[]): FishboneBranchInput[] {
  return view.map(b => ({
    key: b.category,
    label: b.label,
    causes: b.roots.map(n => ({
      id: n.cause.id,
      label: n.cause.label,
      // Le score suit la cause jusqu'au dessin : depuis le retrait des cartes
      // de branche, l'arête est le seul endroit où on le lit.
      score: n.cause.rootCauseScore,
      descendants: countDescendants(n)
    }))
  }));
}

/** Nombre de sous-causes, tous niveaux confondus — le dessin n'en montre que le compte. */
function countDescendants(node: CauseNode): number {
  return node.children.reduce((sum, child) => sum + 1 + countDescendants(child), 0);
}
