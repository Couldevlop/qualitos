import { NgModule } from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
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
    SharedModule, UiModule,
    MatAutocompleteModule, MatButtonToggleModule, MatDatepickerModule, MatNativeDateModule,
    RouterModule.forChild(routes)
  ]
})
export class CostOfQualityModule {}
