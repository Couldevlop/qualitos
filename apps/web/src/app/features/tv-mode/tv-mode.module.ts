import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { TvModeComponent } from './tv-mode.component';
import { TvModeService } from './tv-mode.service';

const routes: Routes = [{ path: '', component: TvModeComponent }];

/**
 * Mode TV / Salle qualité (§7.3) — module lazy non-standalone.
 * Réutilise DashboardService (providedIn root) via TvModeService.
 */
@NgModule({
  declarations: [TvModeComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)],
  providers: [TvModeService]
})
export class TvModeModule {}
