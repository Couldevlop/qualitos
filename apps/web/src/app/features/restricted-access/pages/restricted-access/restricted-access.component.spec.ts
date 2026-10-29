import { Location } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { SharedModule } from '../../../../shared/shared.module';
import { RestrictedAccessComponent } from './restricted-access.component';

describe('RestrictedAccessComponent', () => {
  let location: jasmine.SpyObj<Location>;

  function create(from: string | null): ComponentFixture<RestrictedAccessComponent> {
    location = jasmine.createSpyObj<Location>('Location', ['back']);
    TestBed.configureTestingModule({
      declarations: [RestrictedAccessComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: Location, useValue: location },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(from === null ? {} : { from }) } }
        }
      ]
    });
    const fixture = TestBed.createComponent(RestrictedAccessComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('dit que la page est réservée, au lieu d’une erreur de chargement', () => {
    const el: HTMLElement = create('/admin/ia').nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain("Accès réservé à l'administrateur");
    expect(el.querySelector('.restricted__path')?.textContent).toContain('/admin/ia');
    expect(el.textContent).not.toContain("n'a pas pu être chargé");
  });

  it('n’affiche pas une adresse externe glissée dans le paramètre', () => {
    const el: HTMLElement = create('https://exemple.test').nativeElement;
    expect(el.querySelector('.restricted__path')).toBeNull();
  });

  it('propose le retour à l’accueil et à la page précédente', () => {
    const fixture = create(null);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('a[href="/home"]')).not.toBeNull();
    el.querySelector('button')?.click();
    expect(location.back).toHaveBeenCalled();
  });
});
