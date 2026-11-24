import { NgModule } from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { DateAdapter, MatNativeDateModule } from '@angular/material/core';
import { QosDateAdapter } from '../../shared/date/qos-date-adapter';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { CoqEntryDialogComponent } from './pages/coq-entry-dialog/coq-entry-dialog.component';
import { CoqReportComponent } from './pages/coq-report/coq-report.component';

const routes: Routes = [
  { path: '', component: CoqReportComponent }
];

@NgModule({
  declarations: [CoqReportComponent, CoqEntryDialogComponent],
  // Sélecteur de date, liste déroulante filtrable et bascule mois / année : câblés
  // ici seulement, comme dans le module APQP, pour que le coût ne pèse que sur
  // l'écran qui s'en sert.
  imports: [
    SharedModule, UiModule, UserPickerModule,
    MatAutocompleteModule, MatButtonToggleModule, MatDatepickerModule, MatNativeDateModule,
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
export class CostOfQualityModule {}
