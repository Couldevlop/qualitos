import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { APQP_PHASES, ApqpPhase, phaseParSlug } from '../../apqp.reference';

/**
 * Le cycle APQP : le schéma EST l'interface.
 *
 * <p>Les cinq phases se lisent en V — on descend de la planification vers la
 * conception du processus, on remonte vers la production série. Cette forme
 * n'est pas décorative : elle dit que le milieu du V est le point bas du
 * projet, celui où tout se joue avant de pouvoir remonter. Une liste à plat
 * aurait dit les mêmes mots en perdant cela.
 *
 * <p>Le contenu d'une phase s'ouvre SOUS le schéma, sans quitter l'écran : on
 * garde la vue d'ensemble sous les yeux pendant qu'on entre dans le détail.
 * D'où une seule entrée de menu — le schéma remplace le sous-menu.
 *
 * <p>La phase retenue reste dans l'URL (`/apqp/validation`) : un lien vers une
 * phase se partage et se met en favori, et le retour du navigateur défait la
 * sélection au lieu de quitter l'écran.
 */
@Component({
  selector: 'qos-apqp-overview',
  templateUrl: './apqp-overview.component.html',
  styleUrls: ['./apqp-overview.component.scss'],
  standalone: false
})
export class ApqpOverviewComponent implements OnInit, OnDestroy {

  readonly phases: readonly ApqpPhase[] = APQP_PHASES;

  /** La phase ouverte sous le schéma, ou `undefined` tant qu'on n'a rien choisi. */
  choisie?: ApqpPhase;

  private readonly detruit$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    // On s'abonne plutôt que de lire un instantané : passer d'une phase à
    // l'autre ne recrée pas le composant, et un instantané figerait l'écran
    // sur la première.
    this.route.paramMap
      .pipe(map(p => p.get('phase')), takeUntil(this.detruit$))
      .subscribe(slug => this.retenir(slug));
  }

  ngOnDestroy(): void {
    this.detruit$.next();
    this.detruit$.complete();
  }

  /**
   * Ouvre une phase, ou la referme si on reclique dessus.
   *
   * <p>Refermer ramène à `/apqp` : l'URL dit toujours ce que l'écran montre.
   */
  basculer(phase: ApqpPhase): void {
    void this.router.navigate(
      this.choisie?.slug === phase.slug ? ['/apqp'] : ['/apqp', phase.slug]
    );
  }

  /** Rang de la phase dans le V : 1 et 5 en haut, 2 et 4 au milieu, 3 en bas. */
  niveau(phase: ApqpPhase): 1 | 2 | 3 {
    return phase.numero === 3 ? 3
      : (phase.numero === 2 || phase.numero === 4) ? 2
      : 1;
  }

  estChoisie(phase: ApqpPhase): boolean {
    return this.choisie?.slug === phase.slug;
  }

  trackBySlug(_index: number, phase: ApqpPhase): string {
    return phase.slug;
  }

  private retenir(slug: string | null): void {
    if (!slug) {
      this.choisie = undefined;      // `/apqp` : le schéma seul, rien d'ouvert
      return;
    }
    const phase = phaseParSlug(slug);
    if (!phase) {
      // Segment inconnu — lien périmé, faute de frappe : on montre le schéma
      // plutôt qu'un écran vide qui n'expliquerait rien.
      void this.router.navigate(['/apqp']);
      return;
    }
    this.choisie = phase;
  }
}
