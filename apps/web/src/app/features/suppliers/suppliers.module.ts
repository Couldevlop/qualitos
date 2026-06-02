import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { SuppliersAuditDialogComponent } from './pages/suppliers-audit-dialog/suppliers-audit-dialog.component';
import { SuppliersCertDialogComponent } from './pages/suppliers-cert-dialog/suppliers-cert-dialog.component';
import { SuppliersCreateDialogComponent } from './pages/suppliers-create-dialog/suppliers-create-dialog.component';
import { SuppliersDetailComponent } from './pages/suppliers-detail/suppliers-detail.component';
import { SuppliersEditDialogComponent } from './pages/suppliers-edit-dialog/suppliers-edit-dialog.component';
import { SuppliersListComponent } from './pages/suppliers-list/suppliers-list.component';
import { SuppliersNcDialogComponent } from './pages/suppliers-nc-dialog/suppliers-nc-dialog.component';
import { SuppliersStatusDialogComponent } from './pages/suppliers-status-dialog/suppliers-status-dialog.component';

const routes: Routes = [
  { path: '', component: SuppliersListComponent },
  { path: ':id', component: SuppliersDetailComponent }
];

@NgModule({
  declarations: [
    SuppliersListComponent,
    SuppliersDetailComponent,
    SuppliersCreateDialogComponent,
    SuppliersEditDialogComponent,
    SuppliersStatusDialogComponent,
    SuppliersAuditDialogComponent,
    SuppliersNcDialogComponent,
    SuppliersCertDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class SuppliersModule {}
