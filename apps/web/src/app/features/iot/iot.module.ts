import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import {
  IotDeviceDetailComponent
} from './pages/iot-device-detail/iot-device-detail.component';
import { IotDeviceDialogComponent } from './pages/iot-device-dialog/iot-device-dialog.component';
import { IotDevicesComponent } from './pages/iot-devices/iot-devices.component';
import {
  IotTelemetryDialogComponent
} from './pages/iot-telemetry-dialog/iot-telemetry-dialog.component';
import {
  IotThresholdDialogComponent
} from './pages/iot-threshold-dialog/iot-threshold-dialog.component';

/**
 * Parc IoT et télémétrie (§9).
 *
 * Deux écrans : la flotte (pilotée par la fraîcheur du signal) et la fiche d'un
 * équipement (santé, télémétrie, seuils). Aucune autre sous-route : l'API de
 * l'engine n'expose rien au-delà de ces deux niveaux.
 */
const routes: Routes = [
  { path: '', component: IotDevicesComponent },
  { path: ':id', component: IotDeviceDetailComponent }
];

@NgModule({
  declarations: [
    IotDevicesComponent,
    IotDeviceDetailComponent,
    IotDeviceDialogComponent,
    IotTelemetryDialogComponent,
    IotThresholdDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class IotModule {}
