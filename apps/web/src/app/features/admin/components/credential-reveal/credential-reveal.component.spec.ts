import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { SharedModule } from '../../../../shared/shared.module';
import { CredentialRevealComponent } from './credential-reveal.component';

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
    component.password = 'xxxx-xxxx-xxxx';
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="remise-mdp"]')!.textContent).toContain('xxxx-xxxx-xxxx');
    const ecrire = spyOn(navigator.clipboard, 'writeText').and.resolveTo();

    (el.querySelector('[data-test="remise-copier"]') as HTMLButtonElement).click();
    tick();

    expect(ecrire).toHaveBeenCalledWith('bob@acme.fr\nxxxx-xxxx-xxxx');
    expect(component.copied).toBeTrue();
    let ferme = false;
    component.closed.subscribe(() => ferme = true);
    (el.querySelector('[data-test="remise-fermer"]') as HTMLButtonElement).click();
    expect(ferme).toBeTrue();
  }));

  it('une copie refusée ne prétend pas avoir copié', fakeAsync(() => {
    component.password = 'xxxx-xxxx-xxxx';
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
