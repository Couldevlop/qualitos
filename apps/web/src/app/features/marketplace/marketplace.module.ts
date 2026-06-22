import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { ModerationComponent } from './pages/moderation/moderation.component';
import { RejectDialogComponent } from './pages/moderation/reject-dialog/reject-dialog.component';
import { SubmitComponent } from './pages/submit/submit.component';

const routes: Routes = [
  { path: '', component: CatalogComponent },
  { path: 'submit', component: SubmitComponent },
  { path: 'moderation', component: ModerationComponent }
];

@NgModule({
  declarations: [CatalogComponent, SubmitComponent, ModerationComponent, RejectDialogComponent],
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class MarketplaceModule {}
