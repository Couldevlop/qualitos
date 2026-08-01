import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import {
  CalibrationDetailComponent
} from './pages/calibration-detail/calibration-detail.component';
import {
  CalibrationEquipmentDialogComponent
} from './pages/calibration-equipment-dialog/calibration-equipment-dialog.component';
import {
  CalibrationListComponent
} from './pages/calibration-list/calibration-list.component';
import {
  CalibrationMsaDialogComponent
} from './pages/calibration-msa-dialog/calibration-msa-dialog.component';
import {
  CalibrationPlanDialogComponent
} from './pages/calibration-plan-dialog/calibration-plan-dialog.component';
import {
  CalibrationRecordDialogComponent
} from './pages/calibration-record-dialog/calibration-record-dialog.component';

/**
 * Calibration & gestion des équipements (§4.10).
 *
 * Deux écrans : le parc d'instruments (piloté par les échéances) et la fiche d'un
 * instrument (plan + enregistrements + études MSA). Aucune autre sous-route :
 * l'API n'expose rien d'autre que ces deux niveaux.
 */
const routes: Routes = [
  { path: '', component: CalibrationListComponent },
  { path: ':id', component: CalibrationDetailComponent }
];

@NgModule({
  declarations: [
    CalibrationListComponent,
    CalibrationDetailComponent,
    CalibrationEquipmentDialogComponent,
    CalibrationPlanDialogComponent,
    CalibrationRecordDialogComponent,
    CalibrationMsaDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class CalibrationModule {}
