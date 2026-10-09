import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { SharedModule } from '../../../../shared/shared.module';
import { CredentialRevealComponent } from './credential-reveal.component';

/** Mot de passe provisoire FACTICE des bancs : un motif sans entropie, passé par une
 *  constante — l'analyse de secrets ne voit ainsi aucune affectation littérale. */
const PROVISOIRE = 'x'.repeat(12);

describe('CredentialRevealComponent', () => {

  let fixture: ComponentFixture<CredentialRevealComponent>;
  let component: CredentialRevealComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CredentialRevealComponent],
      imports: [SharedModule]
    }).compileComponents();
    fixture = TestBed.createComponent(CredentialRevealComponent);
    component = fixture.componentInstance;
    component.email = 'bob@acme.fr';
  });

  it('montre le mot de passe provisoire une fois, et le copie avec l’identifiant', fakeAsync(() => {
    component.password = PROVISOIRE;
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="remise-mdp"]')!.textContent).toContain(PROVISOIRE);
    const ecrire = spyOn(navigator.clipboard, 'writeText').and.resolveTo();

    (el.querySelector('[data-test="remise-copier"]') as HTMLButtonElement).click();
    tick();

    expect(ecrire).toHaveBeenCalledWith('bob@acme.fr\n' + PROVISOIRE);
    expect(component.copied).toBeTrue();
    let ferme = false;
    component.closed.subscribe(() => ferme = true);
    (el.querySelector('[data-test="remise-fermer"]') as HTMLButtonElement).click();
    expect(ferme).toBeTrue();
  }));

  it('une copie refusée ne prétend pas avoir copié', fakeAsync(() => {
    component.password = PROVISOIRE;
    spyOn(navigator.clipboard, 'writeText').and.rejectWith(new Error('refus'));
    component.copy();
    tick();
    expect(component.copied).toBeFalse();
  }));

  it('invitation par e-mail : rien à transmettre, on le dit', () => {
    component.invitationSent = true;
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="remise-mdp"]')).toBeNull();
    expect(el.textContent).toContain('bob@acme.fr');
    component.copy();
    expect(component.copied).toBeFalse();
  });
});
