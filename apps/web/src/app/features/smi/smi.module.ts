import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { RiskRegisterSharedModule } from '../risk-register/risk-register-shared.module';
import { RequirementsMatrixComponent } from './pages/requirements-matrix/requirements-matrix.component';
import { RiskCellComponent } from './pages/risk-cell/risk-cell.component';
import { SmiDashboardComponent } from './pages/smi-dashboard/smi-dashboard.component';

/**
 * Le tableau de bord du système de management intégré : la page d'accueil,
 * la liste des risques d'une case de la matrice, et la matrice des exigences.
 */
const routes: Routes = [
  { path: '', component: SmiDashboardComponent },
  { path: 'risques', component: RiskCellComponent },
  { path: 'exigences', component: RequirementsMatrixComponent }
];

@NgModule({
  declarations: [SmiDashboardComponent, RiskCellComponent, RequirementsMatrixComponent],
  imports: [
    SharedModule, UiModule, FormsModule, MatButtonToggleModule,
    // La fenêtre « Créer une action CAPA » du registre, ouverte depuis le détail d'une case.
    RiskRegisterSharedModule,
    RouterModule.forChild(routes)
  ]
})
export class SmiModule {}
