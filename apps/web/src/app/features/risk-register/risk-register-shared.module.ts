import { NgModule } from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatRadioModule } from '@angular/material/radio';

import { SharedModule } from '../../shared/shared.module';
import { ActionDialogComponent } from './pages/action-dialog/action-dialog.component';

/**
 * Ce que le registre prête aux autres écrans : la fenêtre d'action, que le
 * tableau de bord SMI ouvre depuis le panneau d'une case de la matrice. Sans
 * les routes du registre — les importer ailleurs les dupliquerait.
 */
@NgModule({
  declarations: [ActionDialogComponent],
  imports: [SharedModule, MatAutocompleteModule, MatRadioModule],
  exports: [ActionDialogComponent]
})
export class RiskRegisterSharedModule {}
