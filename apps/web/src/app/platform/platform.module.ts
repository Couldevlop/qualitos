import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule, Routes } from '@angular/router';

import { CredentialRevealModule } from '../shared/credential-reveal/credential-reveal.module';
import { SharedModule } from '../shared/shared.module';
import { UiModule } from '../shared/ui/ui.module';
import { ClientsComponent } from './pages/clients/clients.component';
import { PartnerFirmsComponent } from './pages/partner-firms/partner-firms.component';
import { PlatformModulesComponent } from './pages/platform-modules/platform-modules.component';
import { PlatformShellComponent } from './platform-shell/platform-shell.component';

/**
 * La console de l'éditeur de la plateforme.
 *
 * <p>Elle est <b>hors de {@code features/}</b> à dessein : {@code features/}
 * contient les écrans d'un client, et la console n'en est pas un. Cette
 * séparation de dossier est ce qui rend visible, à la lecture de l'arborescence,
 * qu'un écran métier n'a rien à faire ici.
 *
 * <p>Trois surfaces livrées :
 *
 * <ul>
 *   <li><b>Clients</b> — créer une organisation, son premier administrateur et
 *       ses modules. L'écran existait sous {@code /admin/clients}, au milieu du
 *       paramétrage DU client, où il n'avait rien à faire : tout paramétrage
 *       sauf celui-là revient à l'administrateur du client.</li>
 *   <li><b>Cabinets</b> — les cabinets partenaires, leurs consultants, et les
 *       clients qui leur sont proposés. L'éditeur propose ; il n'ouvre rien :
 *       l'octroi est chez le client, sous {@code /admin/cabinets}.</li>
 *   <li><b>Catalogue des modules</b> — ce que la plateforme sait livrer, avec le
 *       palier minimal et les dépendances de chaque module. L'éditeur accordait
 *       des modules sans pouvoir consulter cette liste pour elle-même.</li>
 * </ul>
 *
 * <p>Les trois autres surfaces prévues (paramétrage central, piste
 * d'audit, statistiques) arrivent dans leurs lots. Elles ne figurent pas dans la
 * navigation avant d'exister : un lien mort dans une console d'administration se
 * lit comme une panne.
 */
const routes: Routes = [
  {
    path: '',
    component: PlatformShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'clients' },
      { path: 'clients', component: ClientsComponent },
      { path: 'modules', component: PlatformModulesComponent },
      { path: 'cabinets', component: PartnerFirmsComponent },
      // Une adresse inconnue sous /platform revient aux clients plutôt que de
      // sortir vers la page « introuvable » du métier, qui est dans l'autre
      // coquille et renverrait l'éditeur hors de sa console.
      { path: '**', redirectTo: 'clients' }
    ]
  }
];

@NgModule({
  declarations: [PlatformShellComponent, ClientsComponent, PlatformModulesComponent,
    PartnerFirmsComponent],
  imports: [SharedModule, UiModule, FormsModule, MatMenuModule, CredentialRevealModule,
    RouterModule.forChild(routes)]
})
export class PlatformModule {}
