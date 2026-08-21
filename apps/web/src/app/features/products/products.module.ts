import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ComponentDialogComponent } from './pages/component-dialog/component-dialog.component';
import {
  ControlPlanLineDialogComponent
} from './pages/control-plan-line-dialog/control-plan-line-dialog.component';
import {
  ProductControlPlanTabComponent
} from './pages/product-control-plan-tab/product-control-plan-tab.component';
import { ProductDetailComponent } from './pages/product-detail/product-detail.component';
import { ProductFormDialogComponent } from './pages/product-form-dialog/product-form-dialog.component';
import { ProductNcTabComponent } from './pages/product-nc-tab/product-nc-tab.component';
import { ProductPfmeaTabComponent } from './pages/product-pfmea-tab/product-pfmea-tab.component';
import { ProductsListComponent } from './pages/products-list/products-list.component';
import { OperationDialogComponent } from './pages/operation-dialog/operation-dialog.component';
import {
  RevisionRequestsPanelComponent
} from './pages/revision-requests-panel/revision-requests-panel.component';

const routes: Routes = [
  { path: '', component: ProductsListComponent },
  { path: ':id', component: ProductDetailComponent }
];

@NgModule({
  declarations: [
    ProductsListComponent,
    ProductDetailComponent,
    ProductFormDialogComponent,
    ComponentDialogComponent,
    OperationDialogComponent,
    ProductPfmeaTabComponent,
    ProductControlPlanTabComponent,
    ProductNcTabComponent,
    ControlPlanLineDialogComponent,
    RevisionRequestsPanelComponent
  ],
  // FormsModule et MatMenuModule ne sont réexportés ni par SharedModule ni par
  // UiModule : les specs les importent d'eux-mêmes et ne voient donc pas leur
  // absence — seul le build de production la voit.
  imports: [SharedModule, UiModule, FormsModule, MatMenuModule, RouterModule.forChild(routes)]
})
export class ProductsModule {}
