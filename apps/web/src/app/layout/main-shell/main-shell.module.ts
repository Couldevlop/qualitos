import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { UiModule } from '../../shared/ui/ui.module';
import { MainShellComponent } from './main-shell.component';

@NgModule({
  declarations: [MainShellComponent],
  imports: [CommonModule, RouterModule, UiModule],
  exports: [MainShellComponent]
})
export class MainShellModule {}
