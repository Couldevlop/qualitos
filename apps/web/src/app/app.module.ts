import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { OAuthModule, OAuthService } from 'angular-oauth2-oidc';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { initOidc } from './core/auth/oidc.initializer';
import { CoreModule } from './core/core.module';
import { ApiInterceptor } from './core/http/api.interceptor';
import { MainShellModule } from './layout/main-shell/main-shell.module';

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
    })
  ],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: ApiInterceptor, multi: true },
    { provide: APP_INITIALIZER, useFactory: initOidc, deps: [OAuthService], multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
