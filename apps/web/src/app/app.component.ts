import { Component, Inject, LOCALE_ID, OnInit } from '@angular/core';

import { AppUpdateService } from './core/update/app-update.service';

@Component({
  selector: 'qos-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false
})
export class AppComponent implements OnInit {

  constructor(@Inject(LOCALE_ID) localeId: string,
              private readonly updates: AppUpdateService) {
    // i18n (§15.1) : direction RTL pour l'arabe — chaque locale est un build
    // dédié (Angular i18n natif), la direction se fixe donc au bootstrap.
    const rtl = localeId.toLowerCase().startsWith('ar');
    document.documentElement.dir = rtl ? 'rtl' : 'ltr';
    document.documentElement.lang = localeId;
  }

  ngOnInit(): void {
    // Surveillance des livraisons. Ici plutôt qu'au bootstrap : le service
    // propose un rechargement par une notification, donc il lui faut une
    // interface déjà en place pour être vu.
    this.updates.start();
  }
}
