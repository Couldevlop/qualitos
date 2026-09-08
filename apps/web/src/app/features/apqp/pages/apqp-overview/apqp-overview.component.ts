import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ApqpService } from '../../apqp.service';
import { ApqpDeliverable, ApqpPhase } from '../../apqp.types';
import {
  ApqpDeliverableDialogComponent, ApqpDeliverableDialogData
} from '../apqp-deliverable-dialog/apqp-deliverable-dialog.component';
import {
  ApqpPhaseDialogComponent, ApqpPhaseDialogData
} from '../apqp-phase-dialog/apqp-phase-dialog.component';

/** Qui peut refondre le cycle. Miroir exact du contrôle posé côté serveur. */
const ROLES_ECRITURE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/**
 * Le cycle APQP : le schéma EST l'interface, et il s'édite.
 *
 * <p>Les cinq phases se lisent en V — on descend de la planification vers la
 * conception du processus, on remonte vers la production série. Cette forme
 * n'est pas décorative : elle dit que le milieu du V est le point bas du
 * projet, celui où tout se joue avant de pouvoir remonter.
 *
 * <p>Le cycle vient du SERVEUR et appartient au client : le manuel AIAG ne fait
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
      .pipe(map(p => p.get('phase')), takeUntil(this.detruit$))
      .subscribe(slug => {
        this.slugDemande = slug;
        this.retenir();
      });

    this.charger();
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
      this.choisie?.id === phase.id ? ['/apqp'] : ['/apqp', this.slug(phase)]
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
   * client, et une règle CSS ne peut pas le connaître. La bascule mobile le
   * reprend avec `!important` — c'est la seule façon de battre un style en
   * ligne, et elle est assumée là-bas.
   */
  get colonnes(): string {
    return `repeat(${Math.max(this.phases.length, 1)}, minmax(0, 1fr))`;
  }

  get cycleAria(): string {
    return $localize`:@@apqp.cycle-aria:Cycle APQP`;
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
   * appartienne au client.
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

    this.service.reorder({ phaseIds: ordre }).subscribe({
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
        void this.router.navigate(['/apqp', this.slugDemande]);
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
      this.service.createPhase(saisie).subscribe({
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
      this.service.updatePhase(phase.id, saisie).subscribe({
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

    this.service.deletePhase(phase.id).subscribe({
      next: () => {
        // Retour au schéma : la phase ouverte n'existe plus, et le rang des
        // suivantes a changé — l'URL ne désigne plus ce qu'elle désignait.
        void this.router.navigate(['/apqp']);
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
      this.service.addDeliverable(phase.id, saisie).subscribe({
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
      this.service.updateDeliverable(phase.id, livrable.id, saisie).subscribe({
        next: mise => this.remplacer(mise),
        error: err => this.echouer(err)
      });
    });
  }

  supprimerLivrable(phase: ApqpPhase, livrable: ApqpDeliverable): void {
    const question = $localize`:@@apqp.confirm-delete-deliverable:Retirer « ${livrable.label} » des livrables ?`;
    if (!confirm(question)) return;

    this.service.deleteDeliverable(phase.id, livrable.id).subscribe({
      next: mise => this.remplacer(mise),
      error: err => this.echouer(err)
    });
  }

  // ---------- interne ----------

  private charger(): void {
    this.loading = true;
    this.service.cycle().subscribe({
      next: phases => {
        this.phases = phases;
        this.loading = false;
        this.retenir();
      },
      error: err => {
        this.loading = false;
        this.echouer(err);
      }
    });
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
      void this.router.navigate(['/apqp']);
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
