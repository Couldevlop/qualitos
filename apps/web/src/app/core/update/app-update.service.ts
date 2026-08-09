import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate } from '@angular/service-worker';
import { filter } from 'rxjs';

/**
 * Prise en compte d'une nouvelle version livrée (§15.2).
 *
 * <p>L'application est une PWA : son service worker sert le paquet depuis le
 * cache, ce qui fait tout l'intérêt du mode hors-ligne du terrain. La contrepartie
 * est qu'un paquet mis en cache y reste. Sans ce service, un utilisateur qui a
 * ouvert l'application une fois continuait de voir l'ancienne version après chaque
 * déploiement — un bouton ajouté restait invisible, un défaut corrigé restait
 * présent, et rien ne le signalait. C'est la panne la plus trompeuse qui soit :
 * la livraison est réussie côté serveur, et fausse côté écran.
 *
 * <p>Le rechargement est PROPOSÉ, jamais imposé : recharger d'office ferait
 * perdre une saisie en cours — un constat de non-conformité à moitié rempli, un
 * plan d'actions en cours d'écriture. L'exception est l'état irrécupérable, où
 * le cache ne peut plus servir la page : là, il n'y a plus rien à préserver.
 */
@Injectable({ providedIn: 'root' })
export class AppUpdateService implements OnDestroy {

  /**
   * Rythme de la vérification périodique. Le service worker regarde de lui-même
   * à son enregistrement ; or les écrans de pilotage de la salle qualité (§7.3,
   * mode TV) restent ouverts des journées entières et ne rechargent jamais.
   * Trente minutes suffisent à ce qu'une livraison se voie le jour même sans
   * interroger le serveur sans raison.
   */
  static readonly CHECK_INTERVAL_MS = 30 * 60 * 1000;

  private started = false;
  private timer: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly updates: SwUpdate,
              private readonly snack: MatSnackBar,
              private readonly zone: NgZone) {}

  /**
   * Met la surveillance en place. Sans effet quand le service worker est absent
   * — développement, tests, navigateur qui le refuse : il n'y a alors aucun cache
   * à renouveler, et s'y abonner échouerait.
   */
  start(): void {
    if (this.started || !this.updates.isEnabled) {
      return;
    }
    this.started = true;

    this.updates.versionUpdates
      .pipe(filter(event => event.type === 'VERSION_READY'))
      .subscribe(() => this.offerReload());

    // Le cache ne peut plus servir l'application : aucune saisie n'est
    // récupérable, et rester sur place n'offre qu'une page cassée.
    this.updates.unrecoverable.subscribe(() => this.reload());

    // HORS de la zone Angular, délibérément : une minuterie à l'intérieur
    // empêcherait l'application d'être jamais considérée comme stable, et
    // relancerait une détection de changements toutes les demi-heures pour un
    // appel qui ne touche à rien de l'affichage.
    this.zone.runOutsideAngular(() => {
      this.timer = setInterval(() => this.checkQuietly(), AppUpdateService.CHECK_INTERVAL_MS);
    });
  }

  ngOnDestroy(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  /**
   * Une vérification qui échoue — réseau coupé, serveur indisponible, worker pas
   * encore enregistré — est sans conséquence : la version en cache reste servie
   * et le prochain passage réessaiera. On l'avale donc au lieu de la remonter.
   */
  private checkQuietly(): void {
    this.updates.checkForUpdate().catch(() => undefined);
  }

  private offerReload(): void {
    this.snack
      .open(
        $localize`:@@app.update.available:Une nouvelle version de QualitOS est disponible.`,
        $localize`:@@app.update.reload:Recharger`,
        // Pas de durée : l'invitation reste tant que l'utilisateur ne l'a pas
        // traitée. Une notification qui disparaît toute seule ne serait vue que
        // par celui qui regardait l'écran au bon moment.
        { politeness: 'polite' })
      .onAction()
      .subscribe(() => {
        this.updates.activateUpdate().then(() => this.reload(), () => this.reload());
      });
  }

  /** Isolé pour être substituable en test — recharger la page y couperait la suite. */
  protected reload(): void {
    document.location.reload();
  }
}
