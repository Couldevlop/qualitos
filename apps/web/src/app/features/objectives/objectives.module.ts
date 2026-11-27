import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaCreateDialogModule } from '../capa/capa-create-dialog.module';
import { UiModule } from '../../shared/ui/ui.module';
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
  // `CapaCreateDialogModule` : « Ajouter une action » sur une ligne de la revue
  // ouvre LA fenêtre de création commune (ADR 0088), et non une fenêtre propre
  // aux objectifs — deux formulaires pour la même action divergeraient.
  imports: [
    SharedModule, UiModule, FormsModule, CapaCreateDialogModule,
    RouterModule.forChild(routes)
  ]
})
export class ObjectivesModule {}
