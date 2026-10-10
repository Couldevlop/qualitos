import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { RiskRegisterSharedModule } from './risk-register-shared.module';
import { ItemDetailComponent } from './pages/item-detail/item-detail.component';
import { ItemFormComponent } from './pages/item-form/item-form.component';
import { RegisterComponent } from './pages/register/register.component';

/**
 * Les adresses du registre. L'ordre compte : « opportunites » et « nouveau »
 * passent avant « :id », sans quoi ils seraient lus comme des identifiants.
 */
const routes: Routes = [
  { path: '', component: RegisterComponent, data: { tab: 'risks' } },
  { path: 'nouveau', component: ItemFormComponent, data: { kind: 'risk' } },
  { path: 'opportunites', component: RegisterComponent, data: { tab: 'opportunities' } },
  { path: 'opportunites/nouvelle', component: ItemFormComponent, data: { kind: 'opportunity' } },
  { path: 'opportunites/:id/modifier', component: ItemFormComponent, data: { kind: 'opportunity' } },
  { path: 'opportunites/:id', component: ItemDetailComponent, data: { kind: 'opportunity' } },
  { path: ':id/modifier', component: ItemFormComponent, data: { kind: 'risk' } },
  { path: ':id', component: ItemDetailComponent, data: { kind: 'risk' } }
];

@NgModule({
  declarations: [RegisterComponent, ItemFormComponent, ItemDetailComponent],
  imports: [
    SharedModule, UiModule, FormsModule, RiskRegisterSharedModule,
    MatAutocompleteModule, MatButtonToggleModule,
    RouterModule.forChild(routes)
  ]
})
export class RiskRegisterModule {}
