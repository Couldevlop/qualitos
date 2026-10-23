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
import { GageRrRegisterComponent } from './gage-rr/pages/gage-rr-register/gage-rr-register.component';
import { GageRrStudyComponent } from './gage-rr/pages/gage-rr-study/gage-rr-study.component';
import { SpcAnalyzeComponent } from './pages/spc-analyze/spc-analyze.component';

/**
 * La section SPC. `''` reste l'analyse des règles de Nelson (Analyses IA) ;
 * les sous-menus de la section SPC vivent sous `cartes` (études de
 * capabilité, ADR 0096) et `gage-rr` (études R&R, ADR 0097).
 */
const routes: Routes = [
  { path: '', component: SpcAnalyzeComponent },
  { path: 'cartes', component: CapabilityRegisterComponent },
  { path: 'cartes/nouvelle', component: CapabilityStudyComponent },
  { path: 'cartes/:id', component: CapabilityStudyComponent },
  { path: 'gage-rr', component: GageRrRegisterComponent },
  { path: 'gage-rr/nouvelle', component: GageRrStudyComponent },
  { path: 'gage-rr/:id', component: GageRrStudyComponent }
];

@NgModule({
  declarations: [
    SpcAnalyzeComponent, CapabilityRegisterComponent, CapabilityStudyComponent,
    ControlChartComponent, DistributionChartComponent, GageRrRegisterComponent, GageRrStudyComponent
  ],
  imports: [SharedModule, UiModule, FormsModule, ReactiveFormsModule, UserPickerModule,
    RouterModule.forChild(routes)]
})
export class SpcModule {}
