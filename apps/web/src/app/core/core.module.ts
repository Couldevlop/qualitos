import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule, Optional, SkipSelf } from '@angular/core';

import { AuthService } from './auth/auth.service';
import { ApiInterceptor } from './http/api.interceptor';

/**
 * Module singleton. Doit être importé UNE seule fois (dans AppModule).
 * Contient services applicatifs partagés (auth, interceptor, etc.).
 */
@NgModule({
  imports: [CommonModule],
  providers: [AuthService, ApiInterceptor]
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parent?: CoreModule) {
    if (parent) {
      throw new Error('CoreModule is already loaded — import it only in AppModule.');
    }
  }

  static forRoot(): ModuleWithProviders<CoreModule> {
    return { ngModule: CoreModule };
  }
}
