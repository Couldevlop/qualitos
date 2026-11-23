import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  DerogationSendDialogComponent, DerogationSendDialogResult
} from './derogation-send-dialog.component';

describe('DerogationSendDialogComponent', () => {

  let fermeture: jasmine.SpyObj<MatDialogRef<
    DerogationSendDialogComponent, DerogationSendDialogResult>>;
  let fixture: ComponentFixture<DerogationSendDialogComponent>;
  let composant: DerogationSendDialogComponent;

  async function monter(adresseProposee: string | null = null): Promise<void> {
    fermeture = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationSendDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: fermeture },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { reference: 'DER-2026-0001', adresseProposee }
        }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DerogationSendDialogComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  function rendu(): DerogationSendDialogResult {
    return fermeture.close.calls.mostRecent().args[0] as DerogationSendDialogResult;
  }

  // ---------- l'adresse ----------

  it('l\'adresse du contact qualité est proposée, et reste MODIFIABLE', async () => {
    await monter('qualite@acme.test');
    // Proposée parce qu'elle a déjà été saisie au dossier, modifiable parce
    // que l'interlocuteur de la dérogation n'est pas toujours le même.
    expect(composant.formulaire.controls.customerEmail.value)
      .toBe('qualite@acme.test');
    expect(composant.formulaire.controls.customerEmail.disabled).toBeFalse();
  });

  it('sans contact saisi au dossier, le champ reste vide et REQUIS', async () => {
    await monter(null);
    expect(composant.formulaire.controls.customerEmail.value).toBe('');
    expect(composant.formulaire.invalid).toBeTrue();
  });

  it('une adresse mal formée ne part pas', async () => {
    await monter();
    composant.formulaire.patchValue({ customerEmail: 'acme.test' });

    composant.envoyer();

    // Partie à une adresse invalide, la dérogation paraîtrait envoyée alors
    // que personne ne l'a reçue.
    expect(fermeture.close).not.toHaveBeenCalled();
    expect(composant.formulaire.controls.customerEmail.touched).toBeTrue();
  });

  // ---------- les trois champs du fil ----------

  it('les deux champs demandés partent, plus LA LANGUE', async () => {
    await monter();
    composant.formulaire.patchValue({
      customerEmail: 'qualite@acme.test',
      comment: '  Urgence lot 42  ',
      language: 'EN'
    });

    composant.envoyer();

    expect(rendu()).toEqual({
      customerEmail: 'qualite@acme.test',
      comment: 'Urgence lot 42',
      language: 'EN'
    });
  });

  it("une adresse ENTOURÉE D'ESPACES est refusée, pas élaguée", async () => {
    await monter();
    composant.formulaire.patchValue({ customerEmail: ' q@acme.test ' });

    composant.envoyer();

    // Le validateur `email` d'Angular refuse les espaces de bord AVANT tout
    // élagage. Conséquence à connaître : une adresse collée depuis un courriel
    // est refusée avec « Cette adresse n'est pas valide ». Le collage avec
    // espace est courant ; élaguer à la saisie reste à arbitrer.
    expect(fermeture.close).not.toHaveBeenCalled();
  });

  it('la langue vaut FR par défaut, et le serveur la REFUSE absente', async () => {
    await monter();
    composant.formulaire.patchValue({ customerEmail: 'q@acme.test' });

    composant.envoyer();

    // Aucun document ne doit partir dans une langue que personne n'a choisie :
    // un défaut explicite vaut mieux qu'un champ vide refusé côté serveur.
    expect(rendu().language).toBe('FR');
  });

  it('un commentaire vide devient ABSENT, et non chaîne vide', async () => {
    await monter();
    composant.formulaire.patchValue({
      customerEmail: 'q@acme.test', comment: '   '
    });

    composant.envoyer();

    // Une chaîne d'espaces ferait figurer un commentaire vide dans le
    // courriel du client.
    expect(rendu().comment).toBeNull();
  });

  // ---------- ce que l'envoi engage ----------

  it('la fenêtre DIT que le dossier ne se rouvrira plus', async () => {
    await monter();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    // Le client détiendra un document signé : le modifier en silence le
    // démentirait sans qu'il en sache rien. L'écran le prévient AVANT.
    expect(texte).toMatch(/rouvert|rouvrir|modifié/i);
    expect(texte).toContain('DER-2026-0001');
  });

  it('annuler ferme SANS RIEN rendre', async () => {
    await monter();
    composant.formulaire.patchValue({ customerEmail: 'q@acme.test' });

    composant.annuler();

    expect(fermeture.close).toHaveBeenCalledWith();
  });
});
