import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ApqpService } from '../../apqp.service';
import {
  ApqpCycle, ApqpDeliverable, ApqpDeliverableStatus, ApqpPhase, ApqpProjectType
} from '../../apqp.types';
import {
  ApqpDeliverableDetailDialogComponent, ApqpDeliverableDetailDialogData
} from '../apqp-deliverable-detail-dialog/apqp-deliverable-detail-dialog.component';
import {
  ApqpDeliverableDialogComponent, ApqpDeliverableDialogData
} from '../apqp-deliverable-dialog/apqp-deliverable-dialog.component';
import {
  ApqpPhaseDialogComponent, ApqpPhaseDialogData
} from '../apqp-phase-dialog/apqp-phase-dialog.component';

/** Qui peut refondre le cycle. Miroir exact du contrôle posé côté serveur. */
const ROLES_ECRITURE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/**
 * Le cycle APQP d'un PROJET : le schéma EST l'interface, et il s'édite.
 *
 * <p>Les cinq phases se lisent en V — on descend de la planification vers la
 * conception du processus, on remonte vers la production série. Cette forme
 * n'est pas décorative : elle dit que le milieu du V est le point bas du
 * projet, celui où tout se joue avant de pouvoir remonter.
 *
 * <p>Le cycle vient du SERVEUR et appartient au projet : le manuel AIAG ne fait
 * que l'amorcer. On renomme une phase, on en retire une, on en ajoute une
 * sixième — et le V se redessine, son rang étant calculé par le serveur.
 *
 * <p>Les commandes d'édition vivent dans le panneau d'une phase ouverte, pas
 * sur les jalons : le schéma doit rester lisible d'un coup d'œil, et six
 * boutons posés dessus le transformeraient en barre d'outils.
 */
@Component({
  selector: 'qos-apqp-overview',
  templateUrl: './apqp-overview.component.html',
  styleUrls: ['./apqp-overview.component.scss'],
  standalone: false
})
export class ApqpOverviewComponent implements OnInit, OnDestroy {

  phases: ApqpPhase[] = [];
  loading = false;

  /** Le projet dont on lit le cycle. Vient de l'URL, jamais d'un corps de requête. */
  projetId = '';
  projetNom = '';
  projetType?: ApqpProjectType;
  client?: string | null;

  /** La phase ouverte sous le schéma, ou `undefined` tant qu'on n'a rien choisi. */
  choisie?: ApqpPhase;

  /** Vrai si l'utilisateur peut refondre le cycle. */
  readonly editable: boolean;

  private slugDemande: string | null = null;
  private readonly detruit$ = new Subject<void>();

  constructor(
    private readonly service: ApqpService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_ECRITURE);
  }

  ngOnInit(): void {
    // On s'abonne plutôt que de lire un instantané : passer d'une phase à
    // l'autre ne recrée pas le composant, et un instantané figerait l'écran.
    this.route.paramMap
      .pipe(takeUntil(this.detruit$))
      .subscribe(params => {
        const projet = params.get('projetId') ?? '';
        this.slugDemande = params.get('phase');
        if (projet !== this.projetId) {
          // Changer de projet change TOUT le cycle : on recharge plutôt que de
          // réaligner une phase choisie qui n'existe plus.
          this.projetId = projet;
          this.charger();
          return;
        }
        this.retenir();
      });
  }

  ngOnDestroy(): void {
    this.detruit$.next();
    this.detruit$.complete();
  }

  // ---------- navigation dans le schéma ----------

  /**
   * Ouvre une phase, ou la referme si on reclique dessus.
   *
   * <p>L'URL suit : un lien vers une phase se partage, et le retour du
   * navigateur referme au lieu de quitter l'écran.
   */
  basculer(phase: ApqpPhase): void {
    void this.router.navigate(
      this.choisie?.id === phase.id
        ? ['/apqp', this.projetId]
        : ['/apqp', this.projetId, this.slug(phase)]
    );
  }

  estChoisie(phase: ApqpPhase): boolean {
    return this.choisie?.id === phase.id;
  }

  /**
   * Segment d'URL d'une phase : son rang.
   *
   * <p>Un mot tiré du titre aurait changé au premier renommage, cassant les
   * liens déjà partagés. Le rang, lui, décrit une place dans le cycle — ce qui
   * est justement ce qu'on partage.
   */
  slug(phase: ApqpPhase): string {
    return String(phase.position);
  }

  trackById(_index: number, element: { id: string }): string {
    return element.id;
  }

  /**
   * Les colonnes du V : autant que de phases.
   *
   * <p>Posé en style plutôt qu'en feuille : le nombre appartient au cycle du
   * projet, et une règle CSS ne peut pas le connaître. La bascule mobile le
   * reprend avec `!important` — c'est la seule façon de battre un style en
   * ligne, et elle est assumée là-bas.
   */
  get colonnes(): string {
    return `repeat(${Math.max(this.phases.length, 1)}, minmax(0, 1fr))`;
  }

  get cycleAria(): string {
    return $localize`:@@apqp.cycle-aria:Cycle APQP`;
  }

  /** Le sur-titre de la page : le projet, pas le module. */
  get sousTitreProjet(): string | undefined {
    return this.client ? this.client : undefined;
  }

  typeLabel(type: ApqpProjectType): string {
    return ({
      NPI: $localize`:@@apqp.project.type.npi:NPI — nouveau produit`,
      TOW: $localize`:@@apqp.project.type.tow:ToW — transfert d'activité`,
      MAJOR_MODIFICATION: $localize`:@@apqp.project.type.major-modification:Modification majeure`,
      OTHER: $localize`:@@apqp.project.type.other:Autre`
    })[type];
  }

  ariaCocher(livrable: ApqpDeliverable): string {
    return $localize`:@@apqp.toggle-deliverable-aria:Déclarer « ${livrable.label}:label: » acquis`;
  }

  get ariaPpap(): string {
    return $localize`:@@apqp.ppap-mark-aria:Livrable requis au dossier PPAP`;
  }

  ariaPreuves(livrable: ApqpDeliverable): string {
    return $localize`:@@apqp.evidence-count-aria:${livrable.evidenceCount}:count: pièce(s) jointe(s)`;
  }

  ariaModifier(livrable: ApqpDeliverable): string {
    return $localize`:@@apqp.edit-deliverable-aria:Modifier le livrable ${livrable.label}`;
  }

  ariaSupprimer(livrable: ApqpDeliverable): string {
    return $localize`:@@apqp.delete-deliverable-aria:Supprimer le livrable ${livrable.label}`;
  }

  get ariaAvancer(): string {
    return $localize`:@@apqp.move-earlier:Avancer la phase d'un rang`;
  }

  get ariaReculer(): string {
    return $localize`:@@apqp.move-later:Reculer la phase d'un rang`;
  }

  /** L'état d'un livrable, dit en clair plutôt qu'en constante serveur. */
  statutLabel(statut: ApqpDeliverableStatus): string {
    return ({
      NOT_STARTED: $localize`:@@apqp.status.not-started:Non commencé`,
      IN_PROGRESS: $localize`:@@apqp.status.in-progress:En cours`,
      BLOCKED: $localize`:@@apqp.status.blocked:Bloqué`,
      DONE: $localize`:@@apqp.status.done:Acquis`
    })[statut];
  }

  statutClasse(statut: ApqpDeliverableStatus): string {
    return 'statut statut--' + statut.toLowerCase().replace('_', '-');
  }

  // ---------- ordre du cycle ----------

  peutAvancer(phase: ApqpPhase): boolean {
    return phase.position > 1;
  }

  peutReculer(phase: ApqpPhase): boolean {
    return phase.position < this.phases.length;
  }

  /**
   * Déplace une phase d'un rang dans le cycle.
   *
   * <p>Sans cela, une phase ajoutée resterait à jamais en fin de cycle : le
   * serveur la pose à la suite, et rien ne permettrait de l'intercaler là où
   * elle a lieu d'être — ce qui vide de sa portée le fait que le cycle
   * appartienne au projet.
   *
   * <p>On envoie le cycle ENTIER dans son nouvel ordre, jamais un déplacement
   * isolé : le serveur refuse un ordre partiel, et le V se lit comme un
   * ensemble.
   */
  deplacer(phase: ApqpPhase, decalage: -1 | 1): void {
    const depuis = this.phases.findIndex(p => p.id === phase.id);
    const vers = depuis + decalage;
    if (depuis < 0 || vers < 0 || vers >= this.phases.length) return;

    const ordre = this.phases.map(p => p.id);
    [ordre[depuis], ordre[vers]] = [ordre[vers], ordre[depuis]];

    this.service.reorder(this.projetId, { phaseIds: ordre }).subscribe({
      next: phases => {
        this.phases = phases;
        // Le rang EST le segment d'URL : la phase déplacée a changé d'adresse.
        // On aligne l'écran avant de naviguer, pour qu'il ne dépende pas de
        // l'aller-retour du routeur.
        const deplacee = phases.find(p => p.id === phase.id);
        if (deplacee) {
          this.slugDemande = this.slug(deplacee);
        }
        this.retenir();
        void this.router.navigate(['/apqp', this.projetId, this.slugDemande]);
      },
      error: err => this.echouer(err)
    });
  }

  // ---------- édition des phases ----------

  ajouterPhase(): void {
    const ref = this.dialog.open(ApqpPhaseDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      data: {} as ApqpPhaseDialogData
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.createPhase(this.projetId, saisie).subscribe({
        // On recharge tout : ajouter une phase change le rang de TOUTES les
        // autres dans le V. Insérer la nouvelle dans la liste locale
        // laisserait le schéma faux jusqu'au prochain chargement.
        next: () => this.charger(),
        error: err => this.echouer(err)
      });
    });
  }

  modifierPhase(phase: ApqpPhase): void {
    const ref = this.dialog.open(ApqpPhaseDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      data: { phase } as ApqpPhaseDialogData
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.updatePhase(this.projetId, phase.id, saisie).subscribe({
        next: mise => this.remplacer(mise),
        error: err => this.echouer(err)
      });
    });
  }

  supprimerPhase(phase: ApqpPhase): void {
    // `confirm` natif plutôt qu'un dialogue de plus : la question est fermée, et
    // la perte est décrite dans le texte même.
    const question = $localize`:@@apqp.confirm-delete-phase:Supprimer « ${phase.title} » et ses ${phase.deliverables.length} livrables ? Cette suppression est définitive.`;
    if (!confirm(question)) return;

    this.service.deletePhase(this.projetId, phase.id).subscribe({
      next: () => {
        // Retour au schéma : la phase ouverte n'existe plus, et le rang des
        // suivantes a changé — l'URL ne désigne plus ce qu'elle désignait.
        void this.router.navigate(['/apqp', this.projetId]);
        this.charger();
      },
      error: err => this.echouer(err)
    });
  }

  // ---------- édition des livrables ----------

  ajouterLivrable(phase: ApqpPhase): void {
    const ref = this.dialog.open(ApqpDeliverableDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      data: { phaseTitle: phase.title } as ApqpDeliverableDialogData
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.addDeliverable(this.projetId, phase.id, saisie).subscribe({
        next: mise => this.remplacer(mise),
        error: err => this.echouer(err)
      });
    });
  }

  modifierLivrable(phase: ApqpPhase, livrable: ApqpDeliverable): void {
    const ref = this.dialog.open(ApqpDeliverableDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      data: { phaseTitle: phase.title, deliverable: livrable } as ApqpDeliverableDialogData
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.updateDeliverable(this.projetId, phase.id, livrable.id, saisie).subscribe({
        next: mise => this.remplacer(mise),
        error: err => this.echouer(err)
      });
    });
  }

  /**
   * Ouvre le livrable : ce qu'il attend, où il en est, et ce qui le prouve.
   *
   * <p>Lecture ouverte à tous, écriture non : le popup reçoit le droit plutôt
   * que de le redéduire, pour que les deux écrans ne divergent pas.
   */
  ouvrirLivrable(phase: ApqpPhase, livrable: ApqpDeliverable): void {
    const ref = this.dialog.open(ApqpDeliverableDetailDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      width: '44rem',
      data: {
        projectId: this.projetId, phase, deliverable: livrable, editable: this.editable
      } as ApqpDeliverableDetailDialogData
    });
    ref.afterClosed().subscribe(cycle => {
      if (cycle) this.appliquer(cycle);
    });
  }

  /**
   * Coche ou décoche un livrable depuis la liste.
   *
   * <p>TOUT livrable se coche désormais : le genre qui interdisait la case aux
   * « renvois » a disparu, et il obligeait à ouvrir le popup pour un geste d'une
   * seconde.
   *
   * <p>Ce qui a déjà été saisi repart tel quel — sans cela, un clic sur la case
   * effacerait ce que le popup avait enregistré. Le statut et l'avancement, eux,
   * partent À VIDE : c'est la case qui les pilote, et c'est le serveur qui
   * applique la règle, pour que les deux écrans ne la recalculent pas chacun de
   * son côté.
   */
  basculerLivrable(phase: ApqpPhase, livrable: ApqpDeliverable, coche: boolean): void {
    this.service.completeDeliverable(this.projetId, phase.id, livrable.id, {
      done: coche,
      expectedArtifact: livrable.expectedArtifact ?? null,
      ppap: livrable.ppap,
      owner: livrable.owner ?? null,
      dueDate: livrable.dueDate ?? null,
      status: null,
      percentComplete: null,
      comment: livrable.comment ?? null,
      linkedKind: livrable.linkedKind ?? null,
      linkedId: livrable.linkedId ?? null
    }).subscribe({
      next: cycle => this.appliquer(cycle),
      error: err => this.echouer(err)
    });
  }

  /**
   * Rend au projet le cycle du référentiel, en effaçant le sien.
   *
   * <p>`confirm` natif plutôt qu'un dialogue de plus : la question est fermée, et
   * la perte est décrite dans le texte même.
   */
  reinitialiser(): void {
    const question = $localize`:@@apqp.confirm-reset:Remplacer le cycle de ce projet par celui du référentiel ? Ses phases, ses livrables et les pièces qui les prouvent seront définitivement perdus.`;
    if (!confirm(question)) return;

    this.service.reset(this.projetId).subscribe({
      next: cycle => {
        void this.router.navigate(['/apqp', this.projetId]);
        this.appliquer(cycle);
      },
      error: err => this.echouer(err)
    });
  }

  supprimerLivrable(phase: ApqpPhase, livrable: ApqpDeliverable): void {
    const question = $localize`:@@apqp.confirm-delete-deliverable:Retirer « ${livrable.label} » des livrables ?`;
    if (!confirm(question)) return;

    this.service.deleteDeliverable(this.projetId, phase.id, livrable.id).subscribe({
      next: mise => this.remplacer(mise),
      error: err => this.echouer(err)
    });
  }

  // ---------- interne ----------

  private charger(): void {
    if (!this.projetId) {
      // Adresse sans projet : la liste est le seul écran qui ait un sens.
      void this.router.navigate(['/apqp']);
      return;
    }
    this.loading = true;
    this.service.cycle(this.projetId).subscribe({
      next: cycle => {
        this.appliquer(cycle);
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.echouer(err);
      }
    });
  }

  /**
   * Prend le cycle rendu par le serveur.
   *
   * <p>La réponse porte aussi le projet et l'état de son dossier PPAP ; l'écran
   * affiche le premier et pas le second — c'est l'écran « Dossier PPAP » qui s'en
   * charge — mais il reçoit les deux du même appel, ce qui évite que les vues se
   * répondent sur deux états.
   */
  private appliquer(cycle: ApqpCycle): void {
    this.phases = cycle.phases;
    this.projetNom = cycle.projectName;
    this.projetType = cycle.projectType;
    this.client = cycle.customer ?? null;
    this.retenir();
  }

  /**
   * Remplace une phase dans la liste sans tout recharger.
   *
   * <p>Modifier un intitulé ou un livrable ne change ni l'ordre ni le nombre de
   * phases : le V reste le même, et un rechargement complet ferait clignoter le
   * schéma pour rien.
   */
  private remplacer(mise: ApqpPhase): void {
    this.phases = this.phases.map(p => p.id === mise.id ? mise : p);
    if (this.choisie?.id === mise.id) {
      this.choisie = mise;
    }
  }

  /** Aligne la phase ouverte sur ce que demande l'URL. */
  private retenir(): void {
    if (!this.slugDemande) {
      this.choisie = undefined;
      return;
    }
    const trouvee = this.phases.find(p => this.slug(p) === this.slugDemande);
    if (!trouvee && this.phases.length > 0) {
      // Rang inconnu — lien périmé, phase supprimée depuis : on montre le
      // schéma plutôt qu'un écran vide qui n'expliquerait rien.
      void this.router.navigate(['/apqp', this.projetId]);
      return;
    }
    this.choisie = trouvee;
  }

  private echouer(err: unknown): void {
    this.snack.open(
      safeErrorMessage(err, $localize`:@@apqp.failed:Opération impossible sur le cycle APQP.`),
      $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
