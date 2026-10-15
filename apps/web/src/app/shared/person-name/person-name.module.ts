import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { PersonNameComponent } from './person-name.component';

/**
 * Le nom affiché d'une personne désignée par un dossier (ADR 0092), à importer
 * par chaque module qui montre un responsable, un auteur ou un approbateur.
 */
@NgModule({
  declarations: [PersonNameComponent],
  imports: [CommonModule],
  exports: [PersonNameComponent]
})
export class PersonNameModule {}
