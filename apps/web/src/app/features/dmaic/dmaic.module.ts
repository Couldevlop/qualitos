import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { DmaicCreateDialogComponent } from './pages/dmaic-create-dialog/dmaic-create-dialog.component';
import { DmaicDetailComponent } from './pages/dmaic-detail/dmaic-detail.component';
import { DmaicEditDialogComponent } from './pages/dmaic-edit-dialog/dmaic-edit-dialog.component';
import { DmaicListComponent } from './pages/dmaic-list/dmaic-list.component';
import { DmaicMeasureDialogComponent } from './pages/dmaic-measure-dialog/dmaic-measure-dialog.component';
import { DmaicPokaYokeDialogComponent } from './pages/dmaic-pokayoke-dialog/dmaic-pokayoke-dialog.component';

const routes: Routes = [
  { path: '', component: DmaicListComponent },
  { path: ':id', component: DmaicDetailComponent }
];

@NgModule({
  declarations: [
    DmaicListComponent,
    DmaicDetailComponent,
    DmaicCreateDialogComponent,
    DmaicEditDialogComponent,
    DmaicMeasureDialogComponent,
    DmaicPokaYokeDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class DmaicModule {}
