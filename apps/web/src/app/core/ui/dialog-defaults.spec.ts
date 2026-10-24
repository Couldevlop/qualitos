import { MatDialogConfig } from '@angular/material/dialog';

import { QOS_DIALOG_DEFAULTS } from './dialog-defaults';

describe('QOS_DIALOG_DEFAULTS', () => {
  it('garde les options d’accessibilité de Material', () => {
    const material = new MatDialogConfig();
    expect(QOS_DIALOG_DEFAULTS.role).toBe('dialog');
    expect(QOS_DIALOG_DEFAULTS.role).toBe(material.role);
    expect(QOS_DIALOG_DEFAULTS.hasBackdrop).toBe(material.hasBackdrop);
    expect(QOS_DIALOG_DEFAULTS.closeOnNavigation).toBe(material.closeOnNavigation);
  });

  it('ajoute le cadre commun', () => {
    expect(QOS_DIALOG_DEFAULTS.panelClass).toBe('qos-dialog-panel');
    expect(QOS_DIALOG_DEFAULTS.maxWidth).toBe('94vw');
    expect(QOS_DIALOG_DEFAULTS.autoFocus).toBe('first-tabbable');
    expect(QOS_DIALOG_DEFAULTS.restoreFocus).toBeTrue();
  });
});
