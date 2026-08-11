import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { FiveWhysCascadeComponent } from './components/five-whys-cascade/five-whys-cascade.component';
import { FiveWhysDetailComponent } from './pages/five-whys-detail/five-whys-detail.component';
import { FiveWhysListComponent } from './pages/five-whys-list/five-whys-list.component';

const routes: Routes = [
  { path: '', component: FiveWhysListComponent },
  { path: ':id', component: FiveWhysDetailComponent }
];

/**
 * 5 Pourquoi (§3.5) — analyse rattachée à une non-conformité. Sortie des
 * sous-causes du diagramme Ishikawa, où elle n'était ni identifiable ni
 * consultable pour elle-même.
 */
@NgModule({
  declarations: [FiveWhysListComponent, FiveWhysDetailComponent, FiveWhysCascadeComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class FiveWhysModule {}
