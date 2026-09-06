import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../shared.module';
import { UiModule } from '../ui.module';

/**
 * Le gabarit de dialogue-formulaire, sur le seul point qui change quelque chose
 * pour l'utilisateur : la barre d'actions DIT ce qui bloque.
 *
 * <p>Un bouton grisé sans explication est une impasse. Les erreurs des champs
 * jamais touchés ne s'affichent pas ; le motif porté par la barre est donc
 * souvent la seule indication à l'écran de ce qu'il reste à faire.
 */
@Component({
  template: `
    <qos-form-dialog [formGroup]="form" title="Test"
                     [submitting]="submitting"
                     [submitDisabled]="form.invalid"
                     [blockedReason]="motif">
      <input formControlName="nom">
    </qos-form-dialog>`,
  standalone: false
})
class HoteTestComponent {
  form: FormGroup;
  submitting = false;
  motif?: string;

  constructor(fb: FormBuilder) {
    this.form = fb.group({ nom: ['', [Validators.required]] });
  }
}

describe('FormDialogComponent — ce qui bloque', () => {

  let fixture: ComponentFixture<HoteTestComponent>;
  let hote: HoteTestComponent;

  const motifAffiche = (): string | null => {
    const el = fixture.nativeElement.querySelector('.qfd-blocked');
    return el ? (el as HTMLElement).textContent!.trim() : null;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HoteTestComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(HoteTestComponent);
    hote = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nomme le champ manquant quand l’hôte sait le nommer', () => {
    // « Renseignez la description » vaut mieux qu'une formule passe-partout que
    // l'utilisateur devrait traduire en parcourant l'écran.
    hote.motif = 'Renseignez le nom pour pouvoir enregistrer.';
    fixture.detectChanges();

    expect(motifAffiche()).toContain('Renseignez le nom');
  });

  it('retombe sur une formule générique quand l’hôte n’en fournit pas', () => {
    // Un dialogue qui ne sait pas nommer son champ fautif doit tout de même
    // expliquer le blocage : rien du tout serait le défaut d'origine.
    hote.motif = undefined;
    fixture.detectChanges();

    expect(motifAffiche()).toContain('obligatoires');
  });

  it('se tait dès que le formulaire est valide', () => {
    hote.form.controls['nom'].setValue('Une valeur');
    fixture.detectChanges();

    expect(motifAffiche()).toBeNull();
  });

  it('se tait pendant l’envoi : le spinner parle déjà', () => {
    // Annoncer un blocage au moment même où l'on valide serait un contresens.
    hote.form.controls['nom'].setValue('Une valeur');
    hote.submitting = true;
    fixture.detectChanges();

    expect(motifAffiche()).toBeNull();
  });

  it('garde les boutons à droite, le motif à gauche', () => {
    // `margin-left: auto` sur le groupe de boutons absorbe l'espace libre : le
    // motif n'a pas à pousser les actions hors de leur coin habituel.
    hote.motif = 'Renseignez le nom.';
    fixture.detectChanges();

    const actions = fixture.nativeElement.querySelector('.mat-mdc-dialog-actions') as HTMLElement;
    const enfants = Array.from(actions.children).map(e => e.className);

    expect(enfants[0]).toContain('qfd-blocked');
    expect(enfants[1]).toContain('qfd-buttons');
  });
});
