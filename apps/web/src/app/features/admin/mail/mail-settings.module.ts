import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { MailSettingsComponent } from '../pages/mail-settings/mail-settings.component';

/**
 * L'écran de messagerie, pour les **deux** consoles.
 *
 * <p>Il a son module propre parce qu'il sert à deux endroits : la console du
 * client, où un administrateur règle la sienne, et celle de la plateforme, où le
 * super administrateur règle celle d'un cabinet partenaire. Le laisser déclaré
 * dans le module d'administration aurait obligé la console de plateforme à
 * importer tout ce module — c'est-à-dire une vingtaine d'écrans — pour en
 * atteindre un seul.
 */
@NgModule({
  declarations: [MailSettingsComponent],
  imports: [SharedModule, UiModule, FormsModule, RouterModule],
  exports: [MailSettingsComponent]
})
export class MailSettingsModule {}
