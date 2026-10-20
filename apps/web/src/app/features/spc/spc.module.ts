import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { ControlChartComponent } from './capability/components/control-chart/control-chart.component';
import {
  DistributionChartComponent
} from './capability/components/distribution-chart/distribution-chart.component';
import {
  CapabilityRegisterComponent
} from './capability/pages/capability-register/capability-register.component';
import { CapabilityStudyComponent } from './capability/pages/capability-study/capability-study.component';
import { GageRrComponent } from './gage-rr/pages/gage-rr/gage-rr.component';
import { SpcAnalyzeComponent } from './pages/spc-analyze/spc-analyze.component';

/**
 * La section SPC. `''` reste l'analyse des règles de Nelson (Analyses IA) ;
 * les sous-menus de la section SPC vivent sous `cartes` (études de
 * capabilité, ADR 0096) et `gage-rr`.
 */
const routes: Routes = [
  { path: '', component: SpcAnalyzeComponent },
  { path: 'cartes', component: CapabilityRegisterComponent },
  { path: 'cartes/nouvelle', component: CapabilityStudyComponent },
  { path: 'cartes/:id', component: CapabilityStudyComponent },
  { path: 'gage-rr', component: GageRrComponent }
];

@NgModule({
  declarations: [
    SpcAnalyzeComponent, CapabilityRegisterComponent, CapabilityStudyComponent,
    ControlChartComponent, DistributionChartComponent, GageRrComponent
  ],
  imports: [SharedModule, UiModule, FormsModule, ReactiveFormsModule, UserPickerModule,
    RouterModule.forChild(routes)]
})
export class SpcModule {}
