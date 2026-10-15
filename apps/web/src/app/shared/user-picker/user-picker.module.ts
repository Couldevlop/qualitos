import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { IconNoTranslateModule } from '../icon-no-translate/icon-no-translate.module';
import { UserPickerComponent } from './user-picker.component';

/**
 * Le sélecteur de personne de l'annuaire (ADR 0091), à importer par les modules
 * qui désignent un responsable (audits, CAPA, changements…).
 *
 * <p>Module à part plutôt que dans `SharedModule` : l'autocomplétion Material
 * n'entre ainsi que dans les modules qui s'en servent.
 */
@NgModule({
  declarations: [UserPickerComponent],
  imports: [
    CommonModule, ReactiveFormsModule,
    MatAutocompleteModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    IconNoTranslateModule
  ],
  exports: [UserPickerComponent]
})
export class UserPickerModule {}
