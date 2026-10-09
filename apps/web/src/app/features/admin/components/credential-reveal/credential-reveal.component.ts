import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * La remise des identifiants d'un nouveau compte : l'adresse, et le mot de
 * passe provisoire s'il y en a un.
 *
 * <p>Il n'est montré qu'ici, une fois : ni le serveur ni l'écran ne le relisent
 * plus tard. D'où le bouton « Copier » et l'avertissement. Quand l'invitation
 * est partie par e-mail, il n'y a rien à transmettre — on le dit.
 */
@Component({
  selector: 'qos-credential-reveal',
  templateUrl: './credential-reveal.component.html',
  styleUrls: ['./credential-reveal.component.scss'],
  standalone: false
})
export class CredentialRevealComponent {

  @Input() email = '';
  @Input() password: string | null = null;
  @Input() invitationSent = false;
  @Output() readonly closed = new EventEmitter<void>();

  copied = false;

  copy(): void {
    if (!this.password) return;
    const texte = `${this.email}\n${this.password}`;
    const presse = navigator.clipboard?.writeText(texte);
    if (presse) {
      presse.then(() => this.copied = true).catch(() => this.copied = false);
    }
  }
}
