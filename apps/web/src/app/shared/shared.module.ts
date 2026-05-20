import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';

import { ConfirmDialogComponent } from './ui/confirm-dialog/confirm-dialog.component';

const MATERIAL = [
  MatButtonModule, MatCardModule, MatCheckboxModule, MatChipsModule, MatDialogModule,
  MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule,
  MatProgressBarModule, MatProgressSpinnerModule, MatSelectModule, MatSnackBarModule,
  MatTableModule, MatTabsModule, MatTooltipModule
];

/**
 * Module utilitaire — re-export des Material modules et primitives Angular
 * largement utilisées dans les features. Inclus une seule fois par feature
 * via son `imports`.
 *
 * Composants partagés (ConfirmDialogComponent) sont déclarés ici pour être
 * disponibles dans tout feature qui importe SharedModule.
 */
@NgModule({
  declarations: [ConfirmDialogComponent],
  imports: [CommonModule, ...MATERIAL],
  exports: [CommonModule, ReactiveFormsModule, RouterModule, ConfirmDialogComponent, ...MATERIAL]
})
export class SharedModule {}
