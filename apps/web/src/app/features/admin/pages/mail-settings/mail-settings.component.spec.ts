import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { MailSettingsService } from '../../mail/mail-settings.service';
import { MailSettings } from '../../mail/mail-settings.types';
import { MailSettingsComponent } from './mail-settings.component';

/**
 * « Messagerie » : les deux règles qui peuvent couper le courrier d'une
 * organisation, et l'aiguillage client / cabinet.
 */
describe('MailSettingsComponent', () => {
  let fixture: ComponentFixture<MailSettingsComponent>;
  let component: MailSettingsComponent;
  let service: jasmine.SpyObj<MailSettingsService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const REGLEE: MailSettings = {
    configured: true,
    fromAddress: 'qualite@client.fr',
    fromName: 'Qualite Client',
    replyTo: 'retour@client.fr',
    host: 'smtp.client.fr',
    port: 587,
    security: 'STARTTLS',
    username: 'qualite@client.fr',
    hasPassword: true,
    enabled: true,
    verifiedAt: '2026-11-10T09:00:00Z'
  };

  const ABSENTE: MailSettings = {
    configured: false, fromAddress: null, fromName: null, replyTo: null,
    host: null, port: null, security: null, username: null,
    hasPassword: false, enabled: false, verifiedAt: null
  };

  async function setup(
    lecture: MailSettings | 'erreur' = ABSENTE,
    firmId: string | null = null
  ): Promise<void> {
    // Un meme banc peut comparer deux etats de l'ecran (reglee / absente) :
    // sans cette remise a zero, le second montage se heurterait au module de
    // test deja instancie.
    TestBed.resetTestingModule();

    service = jasmine.createSpyObj<MailSettingsService>('MailSettingsService', [
      'lire', 'enregistrer', 'eprouver', 'retirer',
      'lireCabinet', 'enregistrerCabinet', 'eprouverCabinet', 'retirerCabinet'
    ]);
    const reponse = lecture === 'erreur'
      ? throwError(() => ({ status: 500 }))
      : of(lecture);
    service.lire.and.returnValue(reponse);
    service.lireCabinet.and.returnValue(reponse);
    service.enregistrer.and.returnValue(of(REGLEE));
    service.enregistrerCabinet.and.returnValue(of(REGLEE));
    service.eprouver.and.returnValue(of({ success: true, detail: null, verifiedAt: 'maintenant' }));
    service.eprouverCabinet.and.returnValue(
      of({ success: true, detail: null, verifiedAt: 'maintenant' }));
    service.retirer.and.returnValue(of(void 0));
    service.retirerCabinet.and.returnValue(of(void 0));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [MailSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: MailSettingsService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(MailSettingsComponent);
    component = fixture.componentInstance;
    component.firmId = firmId;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it("dit d'où partent les courriels quand rien n'est réglé", async () => {
    await setup(ABSENTE);

    // Sans cette phrase, un ecran vide se lirait comme « rien a faire », alors
    // que les courriels partent d'une adresse qui n'est pas celle du client.
    expect(un('etat-plateforme')).not.toBeNull();
    expect(un('etat-propre')).toBeNull();
  });

  it('dit que la messagerie du client est en service, et quand elle a été vérifiée', async () => {
    await setup(REGLEE);

    expect(un('etat-propre')).not.toBeNull();
    expect(un('verifiee')).not.toBeNull();
    expect(un('non-verifiee')).toBeNull();
  });

  it("distingue « réglée » de « éprouvée » : une messagerie jamais testée le dit", async () => {
    await setup({ ...REGLEE, verifiedAt: null });

    // Un ecran qui annoncerait « verifiee » apres une simple sauvegarde
    // mentirait sur ce qu'il sait.
    expect(un('non-verifiee')).not.toBeNull();
    expect(un('verifiee')).toBeNull();
  });

  it('ne réaffiche JAMAIS le mot de passe, même quand il y en a un', async () => {
    await setup(REGLEE);

    // Le serveur ne le rend pas, pas meme chiffre. Un champ pre-rempli de
    // points laisserait croire qu'on le relit.
    expect(component.hasPassword).toBeTrue();
    expect(component.password).toBe('');
  });

  it("envoie `null` comme mot de passe quand le champ est vide : vide ne l'efface pas", async () => {
    await setup(REGLEE);

    component.fromName = 'Service Qualite';
    component.enregistrer();

    const envoye = service.enregistrer.calls.mostRecent().args[0];
    // Sans cette regle, corriger une faute de frappe dans le nom de
    // l'expediteur couperait la messagerie du client.
    expect(envoye.password).toBeNull();
    expect(envoye.clearPassword).toBeFalse();
    expect(envoye.fromName).toBe('Service Qualite');
  });

  it("demande l'effacement EXPLICITEMENT, par un bouton distinct", async () => {
    await setup(REGLEE);

    expect(un('effacer-mdp')).not.toBeNull();
    component.effacerLeMotDePasse();

    const envoye = service.enregistrer.calls.mostRecent().args[0];
    expect(envoye.clearPassword).toBeTrue();
  });

  it("n'offre pas d'effacer un mot de passe qui n'existe pas", async () => {
    await setup(ABSENTE);

    expect(un('effacer-mdp')).toBeNull();
  });

  it('cale le port sur la protection choisie, sans écraser un port saisi à la main', async () => {
    await setup(ABSENTE);

    expect(component.port).toBe(587);
    component.protectionChoisie('TLS');
    expect(component.port).toBe(465);

    // Un port saisi a la main est un CHOIX : l'ecraser ferait perdre un
    // reglage que l'administrateur venait d'etablir.
    component.port = 2525;
    component.protectionChoisie('STARTTLS');
    expect(component.port).toBe(2525);
  });

  it("avertit que le mot de passe circulerait en clair sans protection", async () => {
    await setup(ABSENTE);

    component.security = 'NONE';
    fixture.detectChanges();

    expect(un('avertissement-clair')).not.toBeNull();
  });

  it("garde AFFICHÉ le motif d'un essai échoué, au lieu de le faire passer", async () => {
    await setup(REGLEE);
    service.eprouver.and.returnValue(
      of({ success: false, detail: 'Authentication failed (535)', verifiedAt: null }));

    component.testRecipient = 'admin@client.fr';
    component.eprouver();
    fixture.detectChanges();

    // C'est le texte sur lequel l'administrateur corrige son reglage : une
    // notification fugace l'emporterait avant qu'il l'ait lu.
    expect(un('motif-echec')?.textContent).toContain('535');
    expect(snack.open).not.toHaveBeenCalled();
  });

  it("refuse d'envoyer un essai vers une adresse qui n'en est pas une", async () => {
    await setup(REGLEE);

    component.testRecipient = 'pas-une-adresse';
    component.eprouver();

    expect(service.eprouver).not.toHaveBeenCalled();
  });

  it("refuse d'enregistrer un réglage incomplet", async () => {
    await setup(ABSENTE);

    component.fromAddress = 'qualite@client.fr';
    component.host = '';
    expect(component.complet).toBeFalse();
    component.enregistrer();
    expect(service.enregistrer).not.toHaveBeenCalled();

    component.host = 'smtp.client.fr';
    expect(component.complet).toBeTrue();
  });

  it("n'offre de retirer que ce qui est réglé", async () => {
    await setup(ABSENTE);
    expect(un('retirer')).toBeNull();

    await setup(REGLEE);
    expect(un('retirer')).not.toBeNull();
  });

  it('dit que les courriels repartent de la plateforme après un retrait', async () => {
    await setup(REGLEE);

    component.retirer();
    fixture.detectChanges();

    expect(component.configured).toBeFalse();
    expect(un('etat-plateforme')).not.toBeNull();
  });

  it("signale l'échec de chargement sans faire semblant d'être vide", async () => {
    await setup('erreur');

    expect(component.failed).toBeTrue();
    expect(un('etat-plateforme')).toBeNull();
  });

  it("appelle les routes du CABINET quand un cabinet est désigné", async () => {
    await setup(REGLEE, 'cabinet-42');

    expect(service.lireCabinet).toHaveBeenCalledWith('cabinet-42');
    expect(service.lire).not.toHaveBeenCalled();

    component.enregistrer();
    expect(service.enregistrerCabinet).toHaveBeenCalled();
    expect(service.enregistrer).not.toHaveBeenCalled();

    component.testRecipient = 'admin@cabinet.fr';
    component.eprouver();
    expect(service.eprouverCabinet).toHaveBeenCalledWith('cabinet-42', 'admin@cabinet.fr');

    component.retirer();
    expect(service.retirerCabinet).toHaveBeenCalledWith('cabinet-42');
    expect(service.retirer).not.toHaveBeenCalled();
  });

  it("préremplit le destinataire de l'essai avec l'expéditeur", async () => {
    await setup(REGLEE);

    // Neuf fois sur dix c'est la bonne adresse, et la retaper est une friction
    // de plus avant la seule etape qui prouve quelque chose.
    expect(component.testRecipient).toBe('qualite@client.fr');
  });
});
