import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { APQP_PHASES, ApqpPhase, phaseParSlug } from '../../apqp.reference';

/**
 * Une phase du cycle APQP : ce qu'elle établit, et ce qu'elle doit produire.
 *
 * <p>La phase vient de l'URL et non d'un état interne : un lien vers une phase
 * doit pouvoir se coller dans un message ou se mettre en favori. Le segment est
 * un mot (`validation`), pas un rang — renuméroter le référentiel ne casserait
 * pas les liens déjà partagés.
 */
@Component({
  selector: 'qos-apqp-phase',
  templateUrl: './apqp-phase.component.html',
  styleUrls: ['./apqp-phase.component.scss'],
  standalone: false
})
export class ApqpPhaseComponent implements OnInit, OnDestroy {

  phase?: ApqpPhase;
  /** La phase suivante, quand il y en a une : le cycle se parcourt dans l'ordre. */
  suivante?: ApqpPhase;
  precedente?: ApqpPhase;

  private readonly detruit$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    // On s'abonne au paramètre plutôt que de le lire une fois : passer d'une
    // phase à la suivante ne détruit pas le composant, et un instantané
    // laisserait l'écran sur la phase précédente.
    this.route.paramMap
      .pipe(map(params => params.get('phase')), takeUntil(this.detruit$))
      .subscribe(slug => this.situer(slug));
  }

  ngOnDestroy(): void {
    this.detruit$.next();
    this.detruit$.complete();
  }

  private situer(slug: string | null): void {
    const phase = phaseParSlug(slug);
    if (!phase) {
      // Un segment inconnu — lien périmé, faute de frappe — ramène au cycle
      // plutôt qu'à un écran vide qui n'expliquerait rien.
      void this.router.navigate(['/apqp']);
      return;
    }
    this.phase = phase;
    const rang = APQP_PHASES.indexOf(phase);
    this.precedente = APQP_PHASES[rang - 1];
    this.suivante = APQP_PHASES[rang + 1];
  }
}
