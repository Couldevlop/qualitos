import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { DocGenComponent } from './pages/doc-gen/doc-gen.component';
import { DocReviewComponent } from './pages/doc-review/doc-review.component';

const routes: Routes = [
  { path: '', component: DocGenComponent },
  { path: 'documents/:id', component: DocReviewComponent }
];

/**
 * Feature lazy-loaded — génération documentaire IA AVANCÉE multi-documents
 * (Standards Hub §8.8). Route racine : génération en lot d'un dossier ; route
 * documents/:id : revue & validation humaine d'une pièce.
 */
@NgModule({
  declarations: [DocGenComponent, DocReviewComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class StandardsDocGenModule {}
