import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Observable, Subscription, map, startWith } from 'rxjs';
import { filter } from 'rxjs/operators';

import { EditionService } from '../../../../core/edition/edition.service';
import { Edition } from '../../../../core/edition/edition.types';
import {
  ADMIN_TABS, AdminGroup, AdminItem, AdminTab
} from '../../../../layout/main-shell/admin-nav';

/**
 * L'enveloppe de l'administration du client : deux onglets, et le menu de
 * l'onglet courant a cote de l'ecran qu'il ouvre.
 *
 * <p><b>Pourquoi une enveloppe routee, et non un `mat-tab-group`.</b> Les
 * dix-neuf ecrans d'administration sont des ROUTES, quatre d'entre elles
 * chargees a la demande, et des signets les visent. Mettre leur contenu dans
 * les panneaux d'un groupe d'onglets aurait fait perdre chaque adresse. Ici les
 * onglets sont des liens (`mat-tab-nav-bar`) : aucune adresse ne change, et
 * l'onglet courant se deduit de la route — chaque route enfant porte son
 * `data.onglet`.</p>
 *
 * <p>Cette enveloppe ne porte PAS de titre : chaque ecran enfant a deja son
 * `qos-page-header`. Deux titres empiles se disputeraient la hierarchie.</p>
 */
@Component({
  selector: 'qos-admin-shell',
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.scss'],
  standalone: false
})
export class AdminShellComponent implements OnInit, OnDestroy {

  /**
   * Les onglets visibles. L'edition filtre : la licence n'existe qu'en
   * installation sur site (ADR 0082), et un onglet dont tous les ecrans
   * disparaitraient disparait avec eux.
   */
  onglets$!: Observable<AdminTab[]>;

  /**
   * L'onglet courant LUI-MEME, et non son repere.
   *
   * <p>Garder l'objet supprime le `find` qui le retrouvait a chaque
   * rafraichissement, et avec lui un repli defensif qu'aucun etat atteignable
   * ne produisait -- donc intestable, donc faux confort.</p>
   */
  private onglet: AdminTab = ADMIN_TABS[0];

  /** Repere de l'onglet courant, pour le gabarit. */
  get ongletCourant(): AdminTab['id'] {
    return this.onglet.id;
  }

  /** Menu de l'onglet courant, filtre par edition. */
  groupesCourants: AdminGroup[] = [];

  /**
   * Libelles poses en attribut : un `i18n` de gabarit ne traduit pas un
   * attribut `aria-label` ni un `matTooltip`, ils passent donc par le code.
   */
  readonly libelleOnglets = $localize`:@@admin.shell.tabs-aria:Sections de l'administration`;
  readonly libelleHorsConsole =
    $localize`:@@admin.shell.hors-console:Cet ecran s'ouvre hors de la console d'administration`;

  private routeSub?: Subscription;
  private edition: Edition | null = null;

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly editions: EditionService
  ) {}

  ngOnInit(): void {
    this.onglets$ = this.editions.edition().pipe(
      startWith(null as Edition | null),
      map(edition => {
        this.edition = edition;
        this.rafraichirMenu();
        return ADMIN_TABS
          .map(tab => ({ ...tab, groups: this.filtrer(tab.groups, edition) }))
          .filter(tab => tab.groups.length > 0);
      })
    );
    // La route enfant designe l'onglet. On l'ecoute : passer d'un ecran a
    // l'autre par la recherche ou un lien profond doit allumer le bon onglet.
    this.relireLaRoute();
    this.routeSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        this.relireLaRoute();
        this.rafraichirMenu();
      });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  /** Vrai si l'adresse courante est celle de cette entree. */
  estActif(item: AdminItem): boolean {
    const url = this.router.url.split('?')[0].split('#')[0];
    return url === item.route || url.startsWith(item.route + '/');
  }

  /**
   * L'onglet de la route enfant. Faute de `data.onglet` — un ecran atteint
   * avant que sa route ne soit etiquetee — on retombe sur l'onglet qui contient
   * l'adresse, puis sur le premier.
   */
  private relireLaRoute(): void {
    const parDonnee = this.route.firstChild?.snapshot.data?.['onglet'] as AdminTab['id'] | undefined;
    const parLAdresse = () => {
      const url = this.router.url.split('?')[0];
      return ADMIN_TABS.find(tab =>
        tab.groups.some(g => g.items.some(i => url === i.route || url.startsWith(i.route + '/'))));
    };
    this.onglet = ADMIN_TABS.find(tab => tab.id === parDonnee) ?? parLAdresse() ?? ADMIN_TABS[0];
  }

  /** Le menu de l'onglet courant, filtre par l'edition connue a cet instant. */
  private rafraichirMenu(): void {
    this.groupesCourants = this.filtrer(this.onglet.groups, this.edition);
  }

  /** Retire les entrees d'une autre edition, puis les groupes devenus vides. */
  private filtrer(groups: AdminGroup[], edition: Edition | null): AdminGroup[] {
    return groups
      .map(group => ({
        ...group,
        items: group.items.filter(i => edition === null || !i.edition || i.edition === edition)
      }))
      .filter(group => group.items.length > 0);
  }
}
