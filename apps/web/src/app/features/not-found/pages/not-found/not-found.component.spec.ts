import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  let fixture: ComponentFixture<NotFoundComponent>;
  let location: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    location = jasmine.createSpyObj<Location>('Location', ['back']);
    await TestBed.configureTestingModule({
      declarations: [NotFoundComponent],
      imports: [SharedModule, RouterTestingModule, NoopAnimationsModule],
      providers: [{ provide: Location, useValue: location }]
    }).compileComponents();
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue('/pdca/inconnu');
    fixture = TestBed.createComponent(NotFoundComponent);
    fixture.detectChanges();
  });

  it("affiche l'adresse demandée", () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain("Cette page n'existe pas");
    expect(el.querySelector('.not-found__path')?.textContent).toContain('/pdca/inconnu');
  });

  it("propose le retour à l'accueil", () => {
    const link = fixture.nativeElement.querySelector('a[href="/home"]');
    expect(link).not.toBeNull();
  });

  it('revient à la page précédente', () => {
    fixture.nativeElement.querySelector('button').click();
    expect(location.back).toHaveBeenCalled();
  });
});
