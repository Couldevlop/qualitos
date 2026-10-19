import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { DocumentsCreateDialogComponent } from './pages/documents-create-dialog/documents-create-dialog.component';
import { DocumentsDetailComponent } from './pages/documents-detail/documents-detail.component';
import { DocumentsEditDialogComponent } from './pages/documents-edit-dialog/documents-edit-dialog.component';
import { DocumentCircuitComponent } from './components/document-circuit/document-circuit.component';
import { DocumentPanelComponent } from './components/document-panel/document-panel.component';
import { DocumentValidationComponent } from './components/document-validation/document-validation.component';
import { DocumentsImportDialogComponent } from './pages/documents-import-dialog/documents-import-dialog.component';
import { DocumentsListComponent } from './pages/documents-list/documents-list.component';
import { DocumentsRejectDialogComponent } from './pages/documents-reject-dialog/documents-reject-dialog.component';
import { DocumentsVersionDialogComponent } from './pages/documents-version-dialog/documents-version-dialog.component';
import { PersonNameModule } from '../../shared/person-name/person-name.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';

const routes: Routes = [
  { path: '', component: DocumentsListComponent },
  { path: ':id', component: DocumentsDetailComponent }
];

@NgModule({
  declarations: [
    DocumentsListComponent,
    DocumentsDetailComponent,
    DocumentsCreateDialogComponent,
    DocumentsEditDialogComponent,
    DocumentsVersionDialogComponent,
    DocumentsRejectDialogComponent,
    DocumentCircuitComponent,
    DocumentPanelComponent,
    DocumentValidationComponent,
    DocumentsImportDialogComponent
  ],
  imports: [FormsModule, PersonNameModule, SharedModule, UiModule, UserPickerModule, RouterModule.forChild(routes)]
})
export class DocumentsModule {}
