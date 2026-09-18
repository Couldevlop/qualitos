import { NgModule } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaActionDialogModule } from './capa-action-dialog.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaCreateDialogComponent } from './pages/capa-create-dialog/capa-create-dialog.component';
import { CapaDetailComponent } from './pages/capa-detail/capa-detail.component';
import {
  CapaEffectivenessComponent
} from './pages/capa-effectiveness/capa-effectiveness.component';
import { CapaEditDialogComponent } from './pages/capa-edit-dialog/capa-edit-dialog.component';
import { CapaListComponent } from './pages/capa-list/capa-list.component';
import {
  CapaRevisionImpactComponent
} from './pages/capa-revision-impact/capa-revision-impact.component';

const routes: Routes = [
  { path: '', component: CapaListComponent },
  // AVANT `:id` : place apres, le parametre avalerait « efficacite » et la
  // page s'ouvrirait sur un dossier introuvable.
  { path: 'efficacite', component: CapaEffectivenessComponent },
  { path: ':id', component: CapaDetailComponent }
];

@NgModule({
  declarations: [
    CapaListComponent,
    CapaDetailComponent,
    CapaCreateDialogComponent,
    CapaEditDialogComponent,
    CapaRevisionImpactComponent,
    CapaEffectivenessComponent
  ],
  // MatButtonToggleModule n'est re-exporte ni par SharedModule ni par UiModule :
  // les specs l'importent d'elles-memes, seul le build de production le voit.
  // MatRadioModule n'est pas reexporte par SharedModule : le choix « vérification
  // exigée : oui / non » est une question fermée a deux reponses, ou un groupe de
  // boutons radio dit mieux qu'une case a cocher qu'AUCUNE des deux n'est cochee
  // tant que la question n'a pas ete tranchee.
  // `CapaActionDialogModule` : le formulaire d'action est partage avec la fiche
  // de non-conformite, qui l'ouvre directement. Il est donc declare a part.
  imports: [SharedModule, UiModule, MatButtonToggleModule, MatRadioModule,
            CapaActionDialogModule, RouterModule.forChild(routes)]
})
export class CapaModule {}
