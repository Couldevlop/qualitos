import { NgModule } from '@angular/core';

import { SharedModule } from '../shared.module';
import { CredentialRevealComponent } from './credential-reveal.component';

/**
 * Le bloc qui révèle un mot de passe temporaire, une seule fois.
 *
 * <p>Il a son propre module parce que DEUX consoles l'emploient : celle de
 * l'éditeur, quand il crée un client et son premier administrateur, et celle du
 * client, quand son administrateur invite un membre. Un composant ne peut être
 * déclaré que dans un seul NgModule ; le laisser dans la console
 * d'administration du client aurait obligé la console de l'éditeur à importer
 * dix-sept écrans de paramétrage pour en obtenir un.
 */
@NgModule({
  declarations: [CredentialRevealComponent],
  imports: [SharedModule],
  exports: [CredentialRevealComponent]
})
export class CredentialRevealModule {}
