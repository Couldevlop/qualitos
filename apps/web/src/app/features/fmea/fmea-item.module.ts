import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { FmeaItemDialogComponent } from './pages/fmea-item-dialog/fmea-item-dialog.component';

/**
 * Le dialogue d'une ligne d'analyse FMEA, isolé dans son propre module.
 *
 * <p>Même raison que {@link FmeaReferenceModule}, et même conséquence : la
 * ligne s'édite depuis DEUX écrans — le projet FMEA et l'onglet PFMEA d'une
 * fiche produit. Sans ce module, la fiche produit devrait importer tout le
 * module FMEA, <b>ses routes comprises</b>, pour un seul dialogue ; les routes
 * `''` et `:id` du FMEA se superposeraient alors à celles du produit.
 *
 * <p>Le choix de partager le dialogue plutôt que d'en écrire un second est ce
 * qui garantit que les deux écrans cotent de la même façon : une règle de
 * cotation ajoutée ici vaut aux deux endroits, et on ne peut pas obtenir deux
 * RPN différents pour la même saisie selon la porte qu'on a prise.
 *
 * <p>`UserPickerModule` est importé parce que le responsable de l'action est
 * une personne de l'annuaire, jamais un texte libre (ADR 0092).
 */
@NgModule({
  declarations: [FmeaItemDialogComponent],
  imports: [SharedModule, UiModule, UserPickerModule],
  exports: [FmeaItemDialogComponent]
})
export class FmeaItemModule {}
