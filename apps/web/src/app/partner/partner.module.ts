import { NgModule } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../shared/shared.module';
import { UiModule } from '../shared/ui/ui.module';
import { PartnerShellComponent } from './partner-shell/partner-shell.component';
import { PortfolioComponent } from './pages/portfolio/portfolio.component';

/**
 * La console d'un cabinet partenaire.
 *
 * <p><b>Pourquoi une troisième coquille, à côté de {@code features/} et de
 * {@code platform/}.</b> Le jeton d'un consultant ne porte aucun client : il
 * n'est l'utilisateur de personne. La coquille métier, dont chaque garde
 * demande « ce module est-il actif chez MON client ? », n'a donc rien à lui
 * montrer, et la console de l'éditeur lui donnerait une navigation
 * d'administration de plateforme à laquelle il n'a aucun droit. Le tenir hors
 * des deux par la <b>structure</b> plutôt qu'en masquant des entrées de menu
 * est ce qui rend l'erreur impossible à commettre plus tard.
 *
 * <p>Elle n'a qu'une page, et c'est voulu : un consultant ne vient pas y
 * travailler, il vient voir chez quels clients son accès est ouvert. Son
 * travail se fait ensuite dans l'espace du client, avec les écrans du client.
 */
const routes: Routes = [
  {
    path: '',
    component: PartnerShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'portefeuille' },
      { path: 'portefeuille', component: PortfolioComponent },
      // Une adresse inconnue sous /partner revient au portefeuille plutôt que
      // de sortir vers la page « introuvable » du métier, qui est dans l'autre
      // coquille et renverrait le consultant hors de sa console.
      { path: '**', redirectTo: 'portefeuille' }
    ]
  }
];

@NgModule({
  declarations: [PartnerShellComponent, PortfolioComponent],
  imports: [SharedModule, UiModule, MatMenuModule, RouterModule.forChild(routes)]
})
export class PartnerModule {}
