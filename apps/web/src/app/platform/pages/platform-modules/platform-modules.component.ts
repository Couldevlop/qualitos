import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../core/http/error-message';
import { ModuleCatalogEntry } from '../../../features/admin/admin.types';
import { TenantModulesService } from '../../../features/admin/tenant-modules.service';

/** Une famille de modules, avec ses entrées, telle qu'elle s'affiche. */
export interface ModuleFamily {
  /** La catégorie telle que le catalogue la nomme (`methodes`, `transverse`…). */
  key: string;
  label: string;
  entries: ModuleCatalogEntry[];
}

/**
 * Les libellés des familles du catalogue.
 *
 * <p>Exporté pour que le banc vérifie la couverture : une catégorie que le
 * serveur ajoute et que cette table ignore s'afficherait sous son code brut.
 */
export const FAMILY_LABELS: Readonly<Record<string, string>> = {
  methodes: $localize`:@@platform.modules.family.methodes:Méthodes de la qualité`,
  transverse: $localize`:@@platform.modules.family.transverse:Modules transverses`,
  platform: $localize`:@@platform.modules.family.platform:Plateforme`,
  integrations: $localize`:@@platform.modules.family.integrations:Intégrations`
};

/**
 * Le catalogue des modules de la plateforme, vu par l'éditeur.
 *
 * <p><b>Ce que cet écran répare.</b> L'éditeur accordait des modules à un client
 * sans pouvoir consulter la liste de ce que la plateforme sait livrer : elle
 * n'existait qu'au moment de créer un client, noyée dans l'assistant, et nulle
 * part pour elle-même. Les dépendances entre modules — `risk` exige `capa`,
 * `standards` exige `docs` et `audit` — n'étaient donc visibles qu'au moment où
 * une activation était refusée.
 *
 * <p>La source est le catalogue du moteur qualité
 * ({@code GET /api/v1/tenant-modules/catalog}), et non une liste tenue ici : une
 * seconde liste aurait fini par diverger de celle qui ouvre réellement les
 * écrans. C'est aussi pourquoi cet écran n'écrit rien — le catalogue est du
 * domaine, pas du paramétrage.
 */
@Component({
  selector: 'qos-platform-modules',
  templateUrl: './platform-modules.component.html',
  styleUrls: ['./platform-modules.component.scss'],
  standalone: false
})
export class PlatformModulesComponent implements OnInit {

  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  readonly loading$: Observable<boolean> = this.loadingSubject.asObservable();

  families: ModuleFamily[] = [];
  total = 0;
  coreCount = 0;
  error: string | null = null;

  constructor(private readonly modules: TenantModulesService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.error = null;
    this.loadingSubject.next(true);
    this.modules.catalog()
      .pipe(finalize(() => this.loadingSubject.next(false)))
      .subscribe({
        next: entries => this.group(entries),
        error: (err: unknown) => {
          this.families = [];
          this.total = 0;
          this.coreCount = 0;
          this.error = safeErrorMessage(err, $localize`:@@platform.modules.load-failed:Le catalogue des modules n’a pas pu être chargé.`);
        }
      });
  }

  /** Le libellé d'un module requis, pour une dépendance lisible. */
  labelOf(code: string): string {
    for (const family of this.families) {
      const found = family.entries.find(entry => entry.code === code);
      if (found) {
        return found.name;
      }
    }
    // Une dépendance vers un code absent du catalogue : montrer le code brut
    // plutôt que rien, c'est ce qui rend l'incohérence visible.
    return code;
  }

  private group(entries: ModuleCatalogEntry[]): void {
    const byKey = new Map<string, ModuleCatalogEntry[]>();
    for (const entry of entries) {
      const bucket = byKey.get(entry.category);
      if (bucket) {
        bucket.push(entry);
      } else {
        byKey.set(entry.category, [entry]);
      }
    }
    // L'ordre des familles suit celui du catalogue, qui va des méthodes aux
    // intégrations : un tri alphabétique mettrait « Intégrations » en tête.
    this.families = [...byKey.entries()].map(([key, group]) => ({
      key,
      label: FAMILY_LABELS[key] ?? key,
      entries: group
    }));
    this.total = entries.length;
    this.coreCount = entries.filter(entry => entry.coreModule).length;
  }
}
