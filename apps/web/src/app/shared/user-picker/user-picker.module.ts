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
import { UserTeamPickerComponent } from './user-team-picker.component';

/**
 * Le sélecteur de personne de l'annuaire (ADR 0091), à importer par les modules
 * qui désignent un responsable (audits, CAPA, changements…).
 *
 * <p>Module à part plutôt que dans `SharedModule` : l'autocomplétion Material
 * n'entre ainsi que dans les modules qui s'en servent.
 *
 * <p>Il exporte aussi `qos-user-team-picker`, qui désigne PLUSIEURS personnes
 * (l'équipe d'un écart, les participants d'un cercle) en s'appuyant sur le même
 * sélecteur.
 */
@NgModule({
  declarations: [UserPickerComponent, UserTeamPickerComponent],
  imports: [
    CommonModule, ReactiveFormsModule,
    MatAutocompleteModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    IconNoTranslateModule
  ],
  exports: [UserPickerComponent, UserTeamPickerComponent]
})
export class UserPickerModule {}
