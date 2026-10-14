import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { MocDetailComponent } from './components/moc-detail/moc-detail.component';
import { MocImpactComponent } from './components/moc-impact/moc-impact.component';
import { MocValidationComponent } from './components/moc-validation/moc-validation.component';
import { MocComponent } from './pages/moc/moc.component';
import { MocRequestDialogComponent } from './pages/moc-request-dialog/moc-request-dialog.component';

/**
 * Gestion des changements (MOC, ADR 0087) : un seul écran, la liste à gauche,
 * la demande sélectionnée à droite. `/changes/:id` ouvre une demande
 * sélectionnée — c'est le lien des notifications, du registre des risques et
 * du tableau de bord SMI.
 */
const routes: Routes = [
  { path: '', component: MocComponent },
  { path: ':id', component: MocComponent }
];

@NgModule({
  declarations: [
    MocComponent,
    MocDetailComponent,
    MocImpactComponent,
    MocValidationComponent,
    MocRequestDialogComponent
  ],
  imports: [SharedModule, UiModule, FormsModule, MatButtonToggleModule, RouterModule.forChild(routes)]
})
export class ChangesModule {}
