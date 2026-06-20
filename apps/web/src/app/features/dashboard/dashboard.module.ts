import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { DashboardComponent } from './dashboard.component';
import { AnnotationsPanelComponent } from './interactivity/annotations-panel/annotations-panel.component';
import { CrossFilterService } from './interactivity/cross-filter.service';
import { DashboardAnnotationService } from './interactivity/dashboard-annotation.service';
import { TimeTravelService } from './interactivity/time-travel.service';

const routes: Routes = [{ path: '', component: DashboardComponent }];

@NgModule({
  declarations: [DashboardComponent, AnnotationsPanelComponent],
  imports: [SharedModule, UiModule, FormsModule, RouterModule.forChild(routes)],
  providers: [
    // Cross-filter est un état partagé À LA PAGE dashboard : fourni au niveau
    // du module (lazy) pour que tous les widgets de la vue partagent la même
    // instance, et qu'il soit réinitialisé à chaque entrée dans la feature.
    CrossFilterService,
    DashboardAnnotationService,
    TimeTravelService
  ]
})
export class DashboardModule {}
