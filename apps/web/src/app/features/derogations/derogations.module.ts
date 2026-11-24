import { NgModule } from '@angular/core';
import { DateAdapter, MatNativeDateModule } from '@angular/material/core';
import { QosDateAdapter } from '../../shared/date/qos-date-adapter';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { DerogationFormComponent } from './pages/derogation-form/derogation-form.component';
import { DerogationListComponent } from './pages/derogation-list/derogation-list.component';
import {
  DerogationSendBackDialogComponent
} from './pages/derogation-send-back-dialog/derogation-send-back-dialog.component';
import {
  DerogationSendDialogComponent
} from './pages/derogation-send-dialog/derogation-send-dialog.component';
import {
  DerogationSignDialogComponent
} from './pages/derogation-sign-dialog/derogation-sign-dialog.component';

/**
 * Les routes du module.
 *
 * `nouvelle` PRÉCÈDE `:id`, et il le faut : l'ordre est significatif, et un
 * `:id` placé avant capturerait le mot « nouvelle » comme un identifiant — le
 * serveur répondrait 404 sur un dossier qu'on vient d'ouvrir.
 */
const routes: Routes = [
  { path: '', component: DerogationListComponent },
  { path: 'nouvelle', component: DerogationFormComponent },
  { path: ':id', component: DerogationFormComponent }
];

@NgModule({
  declarations: [
    DerogationListComponent,
    DerogationFormComponent,
    DerogationSignDialogComponent,
    DerogationSendDialogComponent,
    DerogationSendBackDialogComponent
  ],
  // Le sélecteur de date est câblé ici seulement, comme dans le coût de la
  // qualité : le dossier porte deux dates, et son coût ne doit peser que sur
  // l'écran qui s'en sert.
  imports: [
    SharedModule, UiModule, UserPickerModule,
    MatDatepickerModule, MatNativeDateModule,
    RouterModule.forChild(routes)
  ],
  // L'adaptateur de date du produit remplace celui de `MatNativeDateModule`,
  // qui ne sait pas RELIRE ce qu'il affiche : il s'en remet a `Date.parse`, qui
  // ne connait que l'ordre americain. Une date tapee au clavier etait refusee
  // en francais, et pire, lue a l'envers quand les deux lectures etaient
  // possibles. Chaque module differe ayant son propre injecteur, la
  // substitution se pose ici et non a la racine, ou elle serait perdante.
  providers: [{ provide: DateAdapter, useClass: QosDateAdapter }],
})
export class DerogationsModule {}
