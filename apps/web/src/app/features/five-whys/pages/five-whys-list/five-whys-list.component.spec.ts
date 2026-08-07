import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SpringPage } from '../../../pdca/pdca.types';
import { FiveWhysService } from '../../five-whys.service';
import { FiveWhysAnalysis } from '../../five-whys.types';
import { FiveWhysListComponent } from './five-whys-list.component';

/**
 * Liste des analyses des 5 Pourquoi.
 *
 * <p>Devant un tableau d'analyses, une seule question compte : la cause racine
 * a-t-elle été conclue ? L'écran doit donc répondre d'un coup d'œil, et rester
 * lisible quand il n'y a rien à montrer — une analyse ne se crée pas ici, elle
 * part d'une non-conformité, et l'écran vide doit le dire au lieu de laisser
 * chercher un bouton qui n'existe pas.
 */
describe('FiveWhysListComponent', () => {

  let fixture: ComponentFixture<FiveWhysListComponent>;
  let component: FiveWhysListComponent;
  let service: jasmine.SpyObj<FiveWhysService>;
  let router: jasmine.SpyObj<Router>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const analysis = (over: Partial<FiveWhysAnalysis> = {}): FiveWhysAnalysis => ({
    id: 'a-1',
    ncId: 'nc-1',
    ncReference: 'NC-2026-014',
    problem: 'Arrêt de ligne récurrent',
    rootCause: null,
    steps: [],
    createdAt: '2026-08-06T10:00:00Z',
    updatedAt: '2026-08-06T10:00:00Z',
    ...over
  });

  const page = (content: FiveWhysAnalysis[]): SpringPage<FiveWhysAnalysis> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 20
  });

  function setup(): void {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [FiveWhysListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: FiveWhysService, useValue: service },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snack }
      ]
    });
    fixture = TestBed.createComponent(FiveWhysListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    service = jasmine.createSpyObj<FiveWhysService>('FiveWhysService', ['list']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
  });

  it('charge les analyses à l\'ouverture et referme l\'indicateur de chargement', () => {
    service.list.and.returnValue(of(page([analysis()])));

    setup();

    expect(service.list).toHaveBeenCalled();
    expect(component.analyses.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('dit d\'un coup d\'œil si la cause racine est conclue', () => {
    service.list.and.returnValue(of(page([
      analysis({ id: 'a-1', rootCause: 'Presse mal réglée' }),
      analysis({ id: 'a-2', ncReference: 'NC-2026-015', rootCause: null })
    ])));

    setup();

    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('Conclue');
    expect(texte).toContain('En cours');
  });

  it('affiche la profondeur atteinte par chaque chaîne', () => {
    // Trois pourquoi qui atteignent la cause racine valent mieux que cinq qui la
    // dépassent : c'est la profondeur réelle qu'on montre, pas un « 5 » décoratif.
    service.list.and.returnValue(of(page([analysis({
      steps: [
        { id: 's1', position: 1, answer: 'a', createdAt: '', updatedAt: '' },
        { id: 's2', position: 2, answer: 'b', createdAt: '', updatedAt: '' },
        { id: 's3', position: 3, answer: 'c', createdAt: '', updatedAt: '' }
      ]
    })])));

    setup();

    const cellules = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('td')
    ).map(td => td.textContent?.trim());
    expect(cellules).toContain('3');
  });

  it('explique l\'écran vide au lieu de le laisser muet', () => {
    service.list.and.returnValue(of(page([])));

    setup();

    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('non-conformité');
    expect((fixture.nativeElement as HTMLElement).querySelector('table')).toBeNull();
  });

  it('ouvre l\'analyse choisie', () => {
    service.list.and.returnValue(of(page([analysis({ id: 'a-42' })])));

    setup();
    component.open(analysis({ id: 'a-42' }));

    expect(router.navigate).toHaveBeenCalledWith(['/five-whys', 'a-42']);
  });

  it('signale l\'échec de chargement et ne laisse pas tourner le spinner', () => {
    service.list.and.returnValue(throwError(() => new Error('502')));

    setup();

    expect(component.loading).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
    expect(component.analyses).toEqual([]);
  });
});
