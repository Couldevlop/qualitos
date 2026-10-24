import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ServiceWorkerModule } from '@angular/service-worker';
import { OAuthModule, OAuthService } from 'angular-oauth2-oidc';

import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { initOidc } from './core/auth/oidc.initializer';
import { CoreModule } from './core/core.module';
import { ApiInterceptor } from './core/http/api.interceptor';
import { MainShellModule } from './layout/main-shell/main-shell.module';
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';
import { QOS_DIALOG_DEFAULTS } from './core/ui/dialog-defaults';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    CoreModule,
    MainShellModule,
    OAuthModule.forRoot({
      resourceServer: {
        sendAccessToken: false   // c'est ApiInterceptor qui ajoute Authorization
      }
    }),
    // PWA offline-first (§15.2-15.3) : actif uniquement en build production
    // (le SW ngsw-worker.js n'est généré que par cette configuration).
    // registerWhenStable : n'interfère pas avec le boot (OIDC initializer).
    ServiceWorkerModule.register('ngsw-worker.js', {
      enabled: environment.production,
      registrationStrategy: 'registerWhenStable:30000'
    })
  ],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: ApiInterceptor, multi: true },
    { provide: APP_INITIALIZER, useFactory: initOidc, deps: [OAuthService], multi: true },
    // Une boîte de dialogue ouverte sans options hérite du cadre commun : largeur
    // bornée à l'écran, focus sur le premier champ, focus rendu au déclencheur.
    // Ces options REMPLACENT celles de Material au lieu de s'y ajouter : on part
    // donc de MatDialogConfig, sans quoi `role: 'dialog'` et `ariaModal` disparaissent
    // et la fenêtre n'est plus annoncée comme un dialogue.
    { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: QOS_DIALOG_DEFAULTS }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(iconRegistry: MatIconRegistry) {
    // index.html ne charge QUE Material Symbols Outlined ; sans cette config,
    // <mat-icon>add</mat-icon> cherche la police 'material-icons' absente et
    // rend la ligature en texte brut (Inter) tronqué → libellés de boutons
    // décalés. Aligne MatIcon sur la police effectivement chargée.
    iconRegistry.setDefaultFontSetClass('material-symbols-outlined');
  }
}
