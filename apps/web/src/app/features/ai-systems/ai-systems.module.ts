import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AiSystemDetailComponent } from './pages/ai-system-detail/ai-system-detail.component';
import { AiSystemFormDialogComponent } from './pages/ai-system-form-dialog/ai-system-form-dialog.component';
import { AiSystemWithdrawDialogComponent } from './pages/ai-system-withdraw-dialog/ai-system-withdraw-dialog.component';
import { AiSystemsListComponent } from './pages/ai-systems-list/ai-systems-list.component';

/**
 * `:id` accepte aussi bien un UUID que la référence lisible du système
 * (AISYS-…) : c'est cette dernière qui circule dans les rapports d'audit et la
 * base de données UE, un lien profond doit pouvoir la porter.
 */
const routes: Routes = [
  { path: '', component: AiSystemsListComponent },
  { path: ':id', component: AiSystemDetailComponent }
];

@NgModule({
  declarations: [
    AiSystemsListComponent,
    AiSystemDetailComponent,
    AiSystemFormDialogComponent,
    AiSystemWithdrawDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AiSystemsModule {}
