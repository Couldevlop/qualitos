import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { OfflineQueueComponent } from './pages/offline-queue/offline-queue.component';

const routes: Routes = [
  { path: '', component: OfflineQueueComponent }
];

@NgModule({
  declarations: [OfflineQueueComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class OfflineQueueModule {}
