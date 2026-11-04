import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatSidenavModule } from '@angular/material/sidenav';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { StakeholderAssistantComponent } from './pages/stakeholder-assistant/stakeholder-assistant.component';
import { StakeholderCreateDialogComponent } from './pages/stakeholder-create-dialog/stakeholder-create-dialog.component';
import { StakeholderLinkDialogComponent } from './pages/stakeholder-link-dialog/stakeholder-link-dialog.component';
import { StakeholderMatrixComponent } from './pages/stakeholder-matrix/stakeholder-matrix.component';
import { StakeholderRegisterComponent } from './pages/stakeholder-register/stakeholder-register.component';
import { StakeholderRequirementDialogComponent } from './pages/stakeholder-requirement-dialog/stakeholder-requirement-dialog.component';
import { StakeholderSheetComponent } from './pages/stakeholder-sheet/stakeholder-sheet.component';
import { StakeholdersPageComponent } from './pages/stakeholders-page/stakeholders-page.component';

/**
 * Une seule adresse. Le registre, la matrice et l'assistant sont trois vues des
 * mêmes fiches, portées par des onglets, et la fiche s'ouvre dans un tiroir :
 * rien de tout cela n'est une page à part, et rien ne gagnerait à être
 * atteignable sans le registre qui lui donne son contexte.
 */
const routes: Routes = [
  { path: '', component: StakeholdersPageComponent }
];

@NgModule({
  declarations: [
    StakeholdersPageComponent,
    StakeholderRegisterComponent,
    StakeholderSheetComponent,
    StakeholderMatrixComponent,
    StakeholderAssistantComponent,
    StakeholderCreateDialogComponent,
    StakeholderRequirementDialogComponent,
    StakeholderLinkDialogComponent
  ],
  imports: [
    SharedModule, UiModule, UserPickerModule, FormsModule,
    MatSidenavModule,
    RouterModule.forChild(routes)
  ]
})
export class StakeholdersModule {}
