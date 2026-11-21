import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { ObjectiveDialogComponent } from './pages/objective-dialog/objective-dialog.component';
import { ObjectiveRejectDialogComponent } from './pages/objective-reject-dialog/objective-reject-dialog.component';
import { ObjectivesReviewComponent } from './pages/objectives-review/objectives-review.component';

/**
 * Une seule adresse : la revue. Définir un objectif et trancher la CAPA qu'il
 * propose sont des gestes qui se font DEPUIS elle, dans des fenêtres — les
 * sortir sur leurs propres routes ferait perdre la ligne qu'on regardait.
 */
const routes: Routes = [
  { path: '', component: ObjectivesReviewComponent }
];

@NgModule({
  declarations: [
    ObjectivesReviewComponent,
    ObjectiveDialogComponent,
    ObjectiveRejectDialogComponent
  ],
  imports: [
    SharedModule, UiModule, UserPickerModule, FormsModule,
    RouterModule.forChild(routes)
  ]
})
export class ObjectivesModule {}
