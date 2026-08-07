import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../nc.service';
import { NcPage } from '../../nc.types';
import { NcListComponent } from './nc-list.component';

/**
 * Deux entrées de navigation, un seul écran : « NC interne » et « NC externe »
 * ouvrent la même liste, bornée à leur origine. L'origine vient de la ROUTE et
 * non d'un filtre que l'utilisateur pourrait changer — sinon les deux entrées
 * mèneraient au même endroit dès le premier clic sur un menu déroulant.
 */
describe('NcListComponent (origine portée par la route)', () => {
  let fixture: ComponentFixture<NcListComponent>;
  let svc: jasmine.SpyObj<NcService>;

  const emptyPage: NcPage = {
    content: [], totalElements: 0, totalPages: 0, number: 0, size: 20
  };

  function setup(routeOrigin: string | null): void {
    svc = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);
    svc.listNcs.and.returnValue(of(emptyPage));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [NcListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ActivatedRoute, useValue: { snapshot: { data: { origin: routeOrigin } } } }
      ]
    });
    fixture = TestBed.createComponent(NcListComponent);
    fixture.detectChanges();
    // La liste n'est chargée qu'à la souscription : le gabarit la consomme par
    // `async` derrière un `*ngIf`, on s'y abonne donc explicitement.
    fixture.componentInstance.ncs$.subscribe();
    fixture.detectChanges();
  }

  it('borne la liste aux non-conformités internes', () => {
    setup('INTERNAL');

    expect(svc.listNcs).toHaveBeenCalled();
    const filters = svc.listNcs.calls.mostRecent().args[2];
    expect(filters?.origin).toBe('INTERNAL');
  });

  it('borne la liste aux non-conformités externes', () => {
    setup('EXTERNAL');

    const filters = svc.listNcs.calls.mostRecent().args[2];
    expect(filters?.origin).toBe('EXTERNAL');
  });

  it('sans origine dans la route, les deux origines remontent', () => {
    // L'ancienne entrée « Non-conformités » doit continuer de tout montrer.
    setup(null);

    const filters = svc.listNcs.calls.mostRecent().args[2];
    expect(filters?.origin).toBeUndefined();
  });

  it('annonce l’origine consultée dans le titre de l’écran', () => {
    setup('EXTERNAL');

    const titre = (fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent ?? '';
    expect(titre.toLowerCase()).toContain('externe');
  });
});
