import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { IshikawaListComponent } from './pages/ishikawa-list/ishikawa-list.component';

const routes: Routes = [{ path: '', component: IshikawaListComponent }];

@NgModule({
  declarations: [IshikawaListComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class IshikawaModule {}
