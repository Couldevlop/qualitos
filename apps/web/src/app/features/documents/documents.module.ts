import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { DocumentsCreateDialogComponent } from './pages/documents-create-dialog/documents-create-dialog.component';
import { DocumentsDetailComponent } from './pages/documents-detail/documents-detail.component';
import { DocumentsEditDialogComponent } from './pages/documents-edit-dialog/documents-edit-dialog.component';
import { DocumentsListComponent } from './pages/documents-list/documents-list.component';
import { DocumentsVersionDialogComponent } from './pages/documents-version-dialog/documents-version-dialog.component';

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
    DocumentsVersionDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class DocumentsModule {}
