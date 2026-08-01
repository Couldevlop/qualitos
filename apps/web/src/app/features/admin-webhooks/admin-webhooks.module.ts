import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { WebhookDeliveryDialogComponent } from './pages/webhook-delivery-dialog/webhook-delivery-dialog.component';
import { WebhookFormDialogComponent } from './pages/webhook-form-dialog/webhook-form-dialog.component';
import { WebhookSecretDialogComponent } from './pages/webhook-secret-dialog/webhook-secret-dialog.component';
import { WebhooksListComponent } from './pages/webhooks-list/webhooks-list.component';

const routes: Routes = [
  { path: '', component: WebhooksListComponent }
];

/**
 * Administration des webhooks sortants (§13.2), chargé en lazy loading : un tenant qui
 * n'expose aucune intégration ne télécharge pas ce bundle (§10.4).
 */
@NgModule({
  declarations: [
    WebhooksListComponent,
    WebhookFormDialogComponent,
    WebhookSecretDialogComponent,
    WebhookDeliveryDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminWebhooksModule {}
