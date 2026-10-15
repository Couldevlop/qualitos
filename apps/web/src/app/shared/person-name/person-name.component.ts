import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnDestroy
} from '@angular/core';
import { Subscription } from 'rxjs';

import { personName } from '../../core/directory/directory-names';
import { DirectoryService } from '../../core/directory/directory.service';

export type PersonNameState = 'empty' | 'named' | 'resolving' | 'unknown';

/** Forme d'un compte de connexion : seule une telle valeur est cherchée dans l'annuaire. */
const ACCOUNT_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Le nom d'une personne désignée par un dossier (ADR 0092) : responsable,
 * auteur, approbateur, auditeur… — jamais son identifiant.
 *
 * <pre>
 *   &lt;qos-person-name [accountId]="row.createdByUserId"&gt;&lt;/qos-person-name&gt;
 *   &lt;qos-person-name [accountId]="row.ownerUserId" [name]="row.owner"&gt;&lt;/qos-person-name&gt;
 * </pre>
 *
 * <p>Dans l'ordre : le nom recopié par le dossier quand il en a un, sinon le
 * nom lu dans l'annuaire en lecture restreinte (ADR 0091, mis en cache par
 * `DirectoryService` : vingt lignes au même auteur ne font qu'un appel), sinon
 * un libellé neutre traduit. La lecture part au changement d'entrée, jamais à
 * chaque cycle de détection.
 */
@Component({
  selector: 'qos-person-name',
  templateUrl: './person-name.component.html',
  styleUrls: ['./person-name.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class PersonNameComponent implements OnChanges, OnDestroy {

  /** Le compte de connexion (`sub` du jeton) de la personne. */
  @Input() accountId: string | null | undefined;
  /** Le nom recopié par le dossier, s'il en a un : il prime sur l'annuaire. */
  @Input() name: string | null | undefined;
  /** Ce qui s'affiche quand le dossier ne désigne personne. */
  @Input() emptyLabel = '—';

  state: PersonNameState = 'empty';
  label = '—';
  /** L'adresse, en infobulle, pour départager deux homonymes. */
  email: string | null = null;

  private sub?: Subscription;

  constructor(private readonly directory: DirectoryService, private readonly cdr: ChangeDetectorRef) {}

  ngOnChanges(): void {
    this.sub?.unsubscribe();
    this.email = null;
    const stored = this.name?.trim();
    if (stored) {
      this.show('named', stored);
      return;
    }
    const id = this.accountId?.trim();
    if (!id) {
      this.show('empty', this.emptyLabel);
      return;
    }
    if (!ACCOUNT_ID.test(id)) {
      this.show('unknown', personUnknownLabel());
      return;
    }
    this.show('resolving', personResolvingLabel());
    this.sub = this.directory.getByAccount(id).subscribe({
      next: user => {
        this.email = user.email || null;
        this.show('named', personName(user));
      },
      error: () => this.show('unknown', personUnknownLabel())
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private show(state: PersonNameState, label: string): void {
    this.state = state;
    this.label = label;
    this.cdr.markForCheck();
  }
}

/** Libellé neutre d'une personne que l'annuaire ne connaît pas (ou plus). */
export function personUnknownLabel(): string {
  return $localize`:@@directory.person.unknown:Personne inconnue`;
}

function personResolvingLabel(): string {
  return $localize`:@@directory.picker.resolving:Chargement du nom…`;
}
