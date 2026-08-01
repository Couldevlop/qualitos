import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule } from '@angular/router';

import { UiModule } from '../../shared/ui/ui.module';
import { MainShellComponent } from './main-shell.component';

@NgModule({
  declarations: [MainShellComponent],
  // MatMenuModule : menus de la cloche de notifications et du compte utilisateur
  // dans la barre supérieure (boutons auparavant inertes).
  imports: [CommonModule, RouterModule, UiModule, MatButtonModule, MatMenuModule],
  exports: [MainShellComponent]
})
export class MainShellModule {}
