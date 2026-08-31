import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import {
  FmeaReferenceDialogComponent
} from './pages/fmea-reference-dialog/fmea-reference-dialog.component';

/**
 * Le référentiel de cotation, isolé dans son propre module.
 *
 * <p>Il s'ouvre depuis DEUX endroits : les projets FMEA et l'onglet PFMEA d'un
 * produit. Sans ce module, la fiche produit aurait dû importer tout le module
 * FMEA — ses routes comprises — pour un seul dialogue.
 *
 * <p>`FormsModule` est importé ici et pas ailleurs : l'édition du barème se fait
 * en `ngModel` sur un tableau de dix lignes, là où un formulaire réactif aurait
 * demandé de construire trente contrôles pour la même chose.
 */
@NgModule({
  declarations: [FmeaReferenceDialogComponent],
  imports: [SharedModule, UiModule, FormsModule],
  exports: [FmeaReferenceDialogComponent]
})
export class FmeaReferenceModule {}
