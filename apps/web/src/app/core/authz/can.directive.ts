import { Directive, Input, OnDestroy, TemplateRef, ViewContainerRef } from '@angular/core';
import { Subscription } from 'rxjs';

import { AuthzService } from './authz.service';
import { PermissionCode } from './authz.types';

/**
 * N'affiche son contenu que si l'utilisateur a l'action (ou l'une des actions).
 *
 * <pre>
 *   &lt;button *qosCan="'capa.create'" …&gt;Nouveau&lt;/button&gt;
 *   &lt;div *qosCan="['nc.close', 'nc.reject']; else lectureSeule"&gt;…&lt;/div&gt;
 * </pre>
 *
 * <p>Tant que les droits ne sont pas lus, rien n'est affiché : un bouton qui
 * apparaîtrait puis disparaîtrait serait pire qu'un bouton qui arrive.
 */
@Directive({
  selector: '[qosCan]',
  standalone: false
})
export class CanDirective implements OnDestroy {

  private sub?: Subscription;
  private elseTemplate: TemplateRef<unknown> | null = null;
  private shown: boolean | null = null;

  constructor(
    private readonly template: TemplateRef<unknown>,
    private readonly view: ViewContainerRef,
    private readonly authz: AuthzService
  ) {}

  @Input()
  set qosCan(codes: PermissionCode | PermissionCode[]) {
    this.sub?.unsubscribe();
    this.sub = this.authz.can(codes).subscribe(ok => this.render(ok));
  }

  @Input()
  set qosCanElse(template: TemplateRef<unknown> | null) {
    this.elseTemplate = template;
    if (this.shown === false) {
      this.shown = null;
      this.render(false);
    }
  }

  private render(ok: boolean): void {
    if (this.shown === ok) return;
    this.shown = ok;
    this.view.clear();
    if (ok) {
      this.view.createEmbeddedView(this.template);
    } else if (this.elseTemplate) {
      this.view.createEmbeddedView(this.elseTemplate);
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
