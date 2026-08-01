import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CommConnectionDialogComponent } from './pages/comm-connection-dialog/comm-connection-dialog.component';
import { ConnectorsHomeComponent } from './pages/connectors-home/connectors-home.component';
import { EhrConnectionDialogComponent } from './pages/ehr-connection-dialog/ehr-connection-dialog.component';
import { ErpConnectionDialogComponent } from './pages/erp-connection-dialog/erp-connection-dialog.component';

/**
 * Une seule route : les trois familles de connecteurs (ERP, EHR/FHIR, Communication)
 * sont trois onglets d'un même écran d'administration, pas trois destinations.
 * Les formulaires sont des dialogues, ils n'ont donc pas d'URL propre.
 */
const routes: Routes = [
  { path: '', component: ConnectorsHomeComponent }
];

@NgModule({
  declarations: [
    ConnectorsHomeComponent,
    ErpConnectionDialogComponent,
    EhrConnectionDialogComponent,
    CommConnectionDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ConnectorsModule {}
