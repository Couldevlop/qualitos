import { MatDialogConfig } from '@angular/material/dialog';

/**
 * Options communes à toutes les boîtes de dialogue.
 *
 * <p>Material ne fusionne pas ces options avec les siennes : il les prend à la
 * place de son `MatDialogConfig`. On repart donc de celui-ci pour garder
 * `role: 'dialog'`, `ariaModal` et le reste, et on n'ajoute que le cadre commun.</p>
 */
export const QOS_DIALOG_DEFAULTS: MatDialogConfig = {
  ...new MatDialogConfig(),
  panelClass: 'qos-dialog-panel',
  maxWidth: '94vw',
  autoFocus: 'first-tabbable',
  restoreFocus: true
};
