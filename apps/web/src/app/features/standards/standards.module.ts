import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { StandardsListComponent } from './pages/standards-list/standards-list.component';
import { StandardsDetailComponent } from './pages/standards-detail/standards-detail.component';

const routes: Routes = [
  { path: '', component: StandardsListComponent },
  { path: 'adoptions/:id', component: StandardsDetailComponent }
];

@NgModule({
  declarations: [StandardsListComponent, StandardsDetailComponent],
  imports: [SharedModule, FormsModule, RouterModule.forChild(routes)]
})
export class StandardsModule {}
