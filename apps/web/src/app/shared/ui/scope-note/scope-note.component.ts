import { Component, Input, OnChanges } from '@angular/core';
import { Observable, of } from 'rxjs';

import { AuthzService } from '../../../core/authz/authz.service';

/**
 * Dit à l'utilisateur qu'il ne voit qu'une partie d'un registre (ADR 0081).
 *
 * <p>Sans l'action « voir tout » du module, une liste ne rend que ce qui le
 * concerne. Une liste courte sans explication ressemble à une panne ; cette
 * note dit pourquoi, avec les mots du module :
 *
 * <pre>
 *   &lt;qos-scope-note permission="nc.view.all"&gt;Vous voyez les NC que vous avez déclarées.&lt;/qos-scope-note&gt;
 * </pre>
 */
@Component({
  selector: 'qos-scope-note',
  templateUrl: './scope-note.component.html',
  styleUrls: ['./scope-note.component.scss'],
  standalone: false
})
export class ScopeNoteComponent implements OnChanges {

  @Input({ required: true }) permission!: string;

  seesAll$: Observable<boolean> = of(true);

  constructor(private readonly authz: AuthzService) {}

  ngOnChanges(): void {
    this.seesAll$ = this.authz.can(this.permission);
  }
}
