import { Component, Inject, LOCALE_ID } from '@angular/core';

@Component({
  selector: 'qos-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false
})
export class AppComponent {

  constructor(@Inject(LOCALE_ID) localeId: string) {
    // i18n (§15.1) : direction RTL pour l'arabe — chaque locale est un build
    // dédié (Angular i18n natif), la direction se fixe donc au bootstrap.
    const rtl = localeId.toLowerCase().startsWith('ar');
    document.documentElement.dir = rtl ? 'rtl' : 'ltr';
    document.documentElement.lang = localeId;
  }
}
