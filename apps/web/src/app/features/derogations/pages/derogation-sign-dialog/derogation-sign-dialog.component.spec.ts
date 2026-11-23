import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  DerogationSignDialogComponent, DerogationSignDialogResult
} from './derogation-sign-dialog.component';

describe('DerogationSignDialogComponent', () => {

  let fermeture: jasmine.SpyObj<MatDialogRef<
    DerogationSignDialogComponent, DerogationSignDialogResult>>;
  let fixture: ComponentFixture<DerogationSignDialogComponent>;
  let composant: DerogationSignDialogComponent;

  async function monter(nomPropose = 'Sophie Marchand'): Promise<void> {
    fermeture = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationSignDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: fermeture },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { reference: 'DER-2026-0001', nomPropose }
        }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DerogationSignDialogComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  function toile(): HTMLCanvasElement {
    return (fixture.nativeElement as HTMLElement)
      .querySelector('canvas') as HTMLCanvasElement;
  }

  /** Un geste de pointeur sur la toile, aux coordonnées données. */
  function pointeur(type: string, x: number, y: number): PointerEvent {
    const e = new PointerEvent(type, {
      clientX: x, clientY: y, bubbles: true, cancelable: true
    });
    toile().dispatchEvent(e);
    return e;
  }

  // ---------- le nom ----------

  it('le nom est pré-rempli depuis l\'annuaire, et reste MODIFIABLE', async () => {
    await monter('Sophie Marchand');
    // Pré-rempli pour éviter la faute de frappe, modifiable parce que le
    // compte connecté n'est pas forcément l'identité à faire figurer.
    expect(composant.nom.value).toBe('Sophie Marchand');
    expect(composant.nom.disabled).toBeFalse();
  });

  it('un annuaire muet laisse le champ vide plutôt qu\'« undefined »', async () => {
    // `null` et non `undefined` : le parametre par defaut du montage
    // remplacerait `undefined`, et le banc ne testerait plus la garde.
    await monter(null as never);
    expect(composant.nom.value).toBe('');
    expect(composant.nom.invalid).toBeTrue();
  });

  // ---------- le tracé ----------

  it('la toile part BLANCHE, pas transparente', async () => {
    await monter();
    const ctx = toile().getContext('2d') as CanvasRenderingContext2D;
    const p = ctx.getImageData(5, 5, 1, 1).data;
    // Le PNG part dans un PDF sans thème : un fond transparent y rendrait le
    // trait invisible selon la visionneuse.
    expect([p[0], p[1], p[2], p[3]]).toEqual([255, 255, 255, 255]);
  });

  it('UN POINT POSÉ SANS GLISSER compte comme un trait', async () => {
    await monter();
    expect(composant.tracee).toBeFalse();

    pointeur('pointerdown', 50, 50);
    composant.terminer();

    // Sans cela, une signature réduite à un point ne serait jamais reconnue,
    // et le bouton resterait bloqué sans que rien ne l'explique.
    expect(composant.tracee).toBeTrue();
  });

  it('un déplacement sans appui préalable ne trace RIEN', async () => {
    await monter();
    pointeur('pointermove', 60, 60);
    // Survoler la zone en cherchant le bouton ne doit pas signer.
    expect(composant.tracee).toBeFalse();
  });

  it('le geste de pointeur est annulé, pour ne pas faire défiler la page',
    async () => {
      await monter();
      const e = pointeur('pointerdown', 20, 20);
      // Sur tablette, le même geste ferait défiler la fenêtre sous le doigt.
      expect(e.defaultPrevented).toBeTrue();
    });

  it('effacer remet la toile blanche ET reprend le droit de signer', async () => {
    await monter();
    pointeur('pointerdown', 30, 30);
    pointeur('pointermove', 90, 90);
    composant.terminer();
    expect(composant.tracee).toBeTrue();

    composant.effacer();

    expect(composant.tracee).toBeFalse();
    const ctx = toile().getContext('2d') as CanvasRenderingContext2D;
    const p = ctx.getImageData(60, 60, 1, 1).data;
    expect([p[0], p[1], p[2]]).toEqual([255, 255, 255]);
  });

  // ---------- signer ----------

  it('signer rend le nom ÉLAGUÉ et le tracé en PNG', async () => {
    await monter('  Sophie Marchand  ');
    pointeur('pointerdown', 40, 40);
    composant.terminer();

    composant.signer();

    const rendu = fermeture.close.calls.mostRecent().args[0] as DerogationSignDialogResult;
    expect(rendu.signedByName).toBe('Sophie Marchand');
    expect(rendu.signaturePngBase64.startsWith('data:image/png;base64,')).toBeTrue();
    // Le serveur retire ce préfixe à la frontière et stocke les octets.
    expect(rendu.signaturePngBase64.length).toBeGreaterThan(100);
  });

  it('SANS TRACÉ, on ne signe pas, même avec un nom valide', async () => {
    await monter('Sophie Marchand');
    composant.signer();
    // Le nom seul ne distingue pas une signature d'une saisie par un tiers.
    expect(fermeture.close).not.toHaveBeenCalled();
  });

  it('SANS NOM, on ne signe pas, même avec un tracé', async () => {
    await monter('');
    pointeur('pointerdown', 40, 40);
    composant.terminer();

    composant.signer();

    // Le tracé seul ne dit pas qui a signé.
    expect(fermeture.close).not.toHaveBeenCalled();
    // Et le champ se marque, sinon l'écran refuse sans rien dire.
    expect(composant.nom.touched).toBeTrue();
  });

  it('annuler ferme SANS RIEN rendre', async () => {
    await monter();
    pointeur('pointerdown', 40, 40);
    composant.terminer();

    composant.annuler();

    expect(fermeture.close).toHaveBeenCalledWith();
  });

  it('la référence du dossier est affichée : on signe un dossier précis',
    async () => {
      await monter();
      const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(texte).toContain('DER-2026-0001');
    });
});
