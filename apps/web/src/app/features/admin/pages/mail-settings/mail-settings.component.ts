import { Component, Input, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { MailSettingsService } from '../../mail/mail-settings.service';
import { MailSecurity, MailSettings, MailSettingsRequest } from '../../mail/mail-settings.types';

/** Les ports usuels, pour que le choix de protection règle aussi le port. */
const PORT_PAR_PROTECTION: Record<MailSecurity, number> = {
  STARTTLS: 587,
  TLS: 465,
  NONE: 25
};

/**
 * « Messagerie » (ADR 0127), réservé à l'administrateur du client.
 *
 * <p>Tant qu'une organisation n'a rien réglé, ses courriels partent de l'adresse
 * de la **plateforme** : une adresse qui n'est pas la sienne, qui ne passe pas
 * ses règles SPF/DKIM, et que ses destinataires ne reconnaissent pas. Cet écran
 * lui rend cette adresse.
 *
 * <p>Deux partis portent tout l'écran.
 *
 * <p>**Le mot de passe ne se réaffiche jamais** — le serveur ne le rend pas, pas
 * même chiffré. Le champ reste donc vide à l'ouverture, et le laisser vide ne
 * l'efface pas : sans cette règle, corriger une faute de frappe dans le nom de
 * l'expéditeur couperait la messagerie du client. L'effacer se demande.
 *
 * <p>**Enregistrer n'est pas vérifier.** La pastille « vérifiée » ne s'allume
 * qu'après un envoi d'essai réellement abouti, et s'éteint dès qu'on change le
 * serveur, le port, le compte ou l'expéditeur. Un écran qui annonce « vérifiée »
 * après une simple sauvegarde ment sur ce qu'il sait.
 */
@Component({
  selector: 'qos-mail-settings',
  templateUrl: './mail-settings.component.html',
  styleUrls: ['./mail-settings.component.scss'],
  standalone: false
})
export class MailSettingsComponent implements OnInit {

  /**
   * Le cabinet dont on regle la messagerie, ou `null` pour le client connecte.
   *
   * <p>Un seul ecran pour les deux, parce que les champs, les regles et les
   * pieges sont les memes : seule l'adresse appelee change. En ecrire deux
   * aurait fait diverger celui des deux qu'on ouvre le moins -- et la moitie
   * qui diverge est celle qu'on teste le moins.
   *
   * <p>Ce qui ne se partage PAS, c'est l'autorisation : le serveur garde la
   * surface du client par le role d'administrateur de client, et celle d'un
   * cabinet par celui de super administrateur. L'ecran ne decide rien.
   */
  @Input() firmId: string | null = null;

  readonly protections: MailSecurity[] = ['STARTTLS', 'TLS', 'NONE'];

  fromAddress = '';
  fromName = '';
  replyTo = '';
  host = '';
  port = 587;
  security: MailSecurity = 'STARTTLS';
  username = '';
  password = '';
  enabled = false;

  /** Vrai quand une messagerie est déjà réglée côté serveur. */
  configured = false;
  /** Vrai quand un mot de passe est en place, sans qu'on sache lequel. */
  hasPassword = false;
  verifiedAt: string | null = null;

  loading = false;
  failed = false;
  saving = false;
  testing = false;
  removing = false;

  /** Le destinataire de l'essai, prérempli avec l'expéditeur. */
  testRecipient = '';
  testFailure: string | null = null;

  constructor(
    private readonly service: MailSettingsService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.lecture().subscribe({
      next: s => {
        this.loading = false;
        this.reprendre(s);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@mail.settings.load-failed:La messagerie n'a pas pu être chargée.`);
      }
    });
  }

  /** Le choix de protection règle le port, tant que l'administrateur n'y a pas touché. */
  protectionChoisie(valeur: MailSecurity): void {
    const attendu = PORT_PAR_PROTECTION[this.security];
    this.security = valeur;
    // On ne remplace que le port USUEL de l'ancienne protection : un port saisi
    // a la main est un choix, et l'ecraser ferait perdre un reglage que
    // l'administrateur venait d'etablir.
    if (this.port === attendu) {
      this.port = PORT_PAR_PROTECTION[valeur];
    }
  }

  get complet(): boolean {
    return this.fromAddress.trim().length > 0
      && this.fromAddress.includes('@')
      && this.host.trim().length > 0
      && this.port > 0 && this.port <= 65535;
  }

  enregistrer(): void {
    if (!this.complet || this.saving) {
      return;
    }
    this.saving = true;
    this.ecriture({
      fromAddress: this.fromAddress.trim(),
      fromName: this.vide(this.fromName),
      replyTo: this.vide(this.replyTo),
      host: this.host.trim(),
      port: this.port,
      security: this.security,
      username: this.vide(this.username),
      // Vide signifie « n'y touche pas » : le serveur garde celui en place.
      password: this.vide(this.password),
      clearPassword: false,
      enabled: this.enabled
    }).subscribe({
      next: s => {
        this.saving = false;
        this.reprendre(s);
        this.snack.open(
          $localize`:@@mail.settings.saved:Messagerie enregistrée.`,
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@mail.settings.save-failed:La messagerie n'a pas pu être enregistrée.`);
      }
    });
  }

  /** Efface le mot de passe : un relais interne peut authentifier par adresse IP. */
  effacerLeMotDePasse(): void {
    if (this.saving) {
      return;
    }
    this.saving = true;
    this.ecriture({
      fromAddress: this.fromAddress.trim(),
      fromName: this.vide(this.fromName),
      replyTo: this.vide(this.replyTo),
      host: this.host.trim(),
      port: this.port,
      security: this.security,
      username: this.vide(this.username),
      password: null,
      clearPassword: true,
      enabled: this.enabled
    }).subscribe({
      next: s => {
        this.saving = false;
        this.password = '';
        this.reprendre(s);
        this.snack.open(
          $localize`:@@mail.settings.password-cleared:Mot de passe effacé.`,
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@mail.settings.save-failed:La messagerie n'a pas pu être enregistrée.`);
      }
    });
  }

  eprouver(): void {
    const destinataire = this.testRecipient.trim();
    if (!destinataire.includes('@') || this.testing) {
      return;
    }
    this.testing = true;
    this.testFailure = null;
    this.essai(destinataire).subscribe({
      next: r => {
        this.testing = false;
        this.verifiedAt = r.verifiedAt;
        if (r.success) {
          this.snack.open(
            $localize`:@@mail.settings.test-ok:Courriel d'essai envoyé. La messagerie est vérifiée.`,
            $localize`:@@common.close:Fermer`, { duration: 6000 });
        } else {
          // Le motif reste AFFICHE, il ne passe pas dans une notification
          // fugace : c'est le texte sur lequel l'administrateur corrige son
          // reglage, et il a besoin de le relire.
          this.testFailure = r.detail;
        }
      },
      error: err => {
        this.testing = false;
        this.echouer(err, $localize`:@@mail.settings.test-failed:L'essai n'a pas pu être lancé.`);
      }
    });
  }

  retirer(): void {
    if (this.removing) {
      return;
    }
    this.removing = true;
    this.suppression().subscribe({
      next: () => {
        this.removing = false;
        this.reprendre({
          configured: false, fromAddress: null, fromName: null, replyTo: null,
          host: null, port: null, security: null, username: null,
          hasPassword: false, enabled: false, verifiedAt: null
        });
        this.snack.open(
          $localize`:@@mail.settings.removed:Messagerie retirée. Les courriels repartent de l'adresse de la plateforme.`,
          $localize`:@@common.close:Fermer`, { duration: 6000 });
      },
      error: err => {
        this.removing = false;
        this.echouer(err, $localize`:@@mail.settings.remove-failed:La messagerie n'a pas pu être retirée.`);
      }
    });
  }

  // ---------- l'aiguillage client / cabinet, en un seul endroit ----------

  private lecture() {
    return this.firmId ? this.service.lireCabinet(this.firmId) : this.service.lire();
  }

  private ecriture(req: MailSettingsRequest) {
    return this.firmId
      ? this.service.enregistrerCabinet(this.firmId, req)
      : this.service.enregistrer(req);
  }

  private essai(destinataire: string) {
    return this.firmId
      ? this.service.eprouverCabinet(this.firmId, destinataire)
      : this.service.eprouver(destinataire);
  }

  private suppression() {
    return this.firmId ? this.service.retirerCabinet(this.firmId) : this.service.retirer();
  }

  private reprendre(s: MailSettings): void {
    this.configured = s.configured;
    this.fromAddress = s.fromAddress ?? '';
    this.fromName = s.fromName ?? '';
    this.replyTo = s.replyTo ?? '';
    this.host = s.host ?? '';
    this.port = s.port ?? 587;
    this.security = s.security ?? 'STARTTLS';
    this.username = s.username ?? '';
    this.hasPassword = s.hasPassword;
    this.enabled = s.enabled;
    this.verifiedAt = s.verifiedAt;
    // Le champ repart VIDE : le serveur ne rend pas le mot de passe, et un
    // formulaire qui afficherait des points de suspension laisserait croire
    // qu'on le relit.
    this.password = '';
    this.testFailure = null;
    if (!this.testRecipient) {
      this.testRecipient = this.fromAddress;
    }
  }

  private vide(valeur: string): string | null {
    const net = valeur.trim();
    return net.length === 0 ? null : net;
  }

  private echouer(err: unknown, parDefaut: string): void {
    this.snack.open(safeErrorMessage(err, parDefaut),
      $localize`:@@common.close:Fermer`, { duration: 7000 });
  }
}
