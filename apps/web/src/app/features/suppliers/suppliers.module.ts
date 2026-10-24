import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { SupplierDocumentsDialogComponent } from './pages/supplier-documents-dialog/supplier-documents-dialog.component';
import { SupplierDocumentsPanelComponent } from './pages/supplier-documents-panel/supplier-documents-panel.component';
import { SupplierNcListComponent } from './pages/supplier-nc-list/supplier-nc-list.component';
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
  // Avant ':id', sinon « non-conformites » serait pris pour un identifiant de fournisseur.
  { path: 'non-conformites', component: SupplierNcListComponent },
  { path: ':id', component: SuppliersDetailComponent },
  { path: ':id/non-conformites', component: SupplierNcListComponent }
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
    SuppliersCertDialogComponent,
    SupplierNcListComponent,
    SupplierDocumentsPanelComponent,
    SupplierDocumentsDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class SuppliersModule {}
