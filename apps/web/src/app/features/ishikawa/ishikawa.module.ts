import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { IshikawaSharedModule } from './ishikawa-shared.module';
import { IshikawaCauseDialogComponent } from './pages/ishikawa-cause-dialog/ishikawa-cause-dialog.component';
import { IshikawaCreateDialogComponent } from './pages/ishikawa-create-dialog/ishikawa-create-dialog.component';
import { IshikawaDetailComponent } from './pages/ishikawa-detail/ishikawa-detail.component';
import { IshikawaEditDialogComponent } from './pages/ishikawa-edit-dialog/ishikawa-edit-dialog.component';
import { IshikawaListComponent } from './pages/ishikawa-list/ishikawa-list.component';
import { PersonNameModule } from '../../shared/person-name/person-name.module';

const routes: Routes = [
  { path: '', component: IshikawaListComponent },
  { path: ':id', component: IshikawaDetailComponent }
];

@NgModule({
  declarations: [
    IshikawaListComponent,
    IshikawaDetailComponent,
    IshikawaCreateDialogComponent,
    IshikawaCauseDialogComponent,
    IshikawaEditDialogComponent
  ],
  imports: [PersonNameModule, SharedModule, UiModule, IshikawaSharedModule,
            RouterModule.forChild(routes)]
})
export class IshikawaModule {}
