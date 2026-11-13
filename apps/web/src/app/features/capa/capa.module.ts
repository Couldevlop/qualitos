import { NgModule } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaCreateDialogModule } from './capa-create-dialog.module';
import { CapaVerificationFieldsModule } from './capa-verification-fields.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import {
  CapaEvaluationBoardComponent
} from './components/capa-evaluation-board/capa-evaluation-board.component';
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
    CapaEditDialogComponent,
    CapaRevisionImpactComponent,
    CapaEffectivenessComponent,
    // Le plan de travail des évaluations (ADR 0108), hébergé par le premier
    // onglet de l'écran d'efficacité : la mesure par récidive et le jugement
    // d'efficacité répondent à la même question, et les séparer en deux écrans
    // les aurait laissées se contredire sans que personne ne les compare.
    CapaEvaluationBoardComponent
  ],
  // MatButtonToggleModule n'est re-exporte ni par SharedModule ni par UiModule :
  // les specs l'importent d'elles-memes, seul le build de production le voit.
  // MatRadioModule n'est pas reexporte par SharedModule : le choix « vérification
  // exigée : oui / non » est une question fermée a deux reponses, ou un groupe de
  // boutons radio dit mieux qu'une case a cocher qu'AUCUNE des deux n'est cochee
  // tant que la question n'a pas ete tranchee.
  // `CapaCreateDialogModule` : la fenetre de creation est partagee avec toutes
  // les sections qui ouvrent une CAPA (ADR 0088). Elle est donc declaree a part.
  // UserPickerModule : le vérificateur se choisit dans l'annuaire (ADR 0091).
  // `CapaVerificationFieldsModule` : le bloc de vérification, partagé avec la
  // fenêtre d'ouverture.
  // Plus de `CapaActionPanelModule` ici : la fiche CAPA ne montre plus de
  // tableau d'actions. Une CAPA EST une action (ADR 0088), et une liste
  // d'actions à l'intérieur d'une action n'a pas d'objet. Le tableau reste là
  // où il en a un : la fiche d'un écart, qui peut porter plusieurs CAPA, et le
  // rapport 8D.
  imports: [SharedModule, UiModule, UserPickerModule, MatButtonToggleModule, MatRadioModule,
            CapaCreateDialogModule, CapaVerificationFieldsModule,
            RouterModule.forChild(routes)]
})
export class CapaModule {}
