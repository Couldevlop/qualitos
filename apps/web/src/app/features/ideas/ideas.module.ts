import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { IdeaDialogComponent } from './pages/idea-dialog/idea-dialog.component';
import { IdeaRejectDialogComponent } from './pages/idea-reject-dialog/idea-reject-dialog.component';
import { IdeasBoardComponent } from './pages/ideas-board/ideas-board.component';

// Un seul ecran : la route paresseuse et l'entree de menu sont la tache 8, pas
// celle-ci -- ce module se limite a declarer l'ecran et ses deux dialogues.
const routes: Routes = [
  { path: '', component: IdeasBoardComponent }
];

@NgModule({
  declarations: [
    IdeasBoardComponent,
    IdeaDialogComponent,
    IdeaRejectDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class IdeasModule {}
