import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject } from 'rxjs';

import { SharedModule } from '../../shared.module';
import { UiModule } from '../ui.module';
import { filterRows, normalizeSearch, searchRows } from './list-filter';
import { ListSearchComponent } from './list-search.component';

interface Row { reference: string; title: string; zone?: string | null; }

const ROWS: Row[] = [
  { reference: 'NC-2026-001', title: 'Étiquetage lot manquant — ligne 1', zone: 'Atelier A' },
  { reference: 'NC-2026-002', title: 'Défaut peinture', zone: null },
  { reference: 'NC-2026-003', title: 'Température chambre froide', zone: 'Pharmacie' }
];
const FIELDS = (r: Row) => [r.reference, r.title, r.zone];

describe('filtre de liste', () => {
  it('compare sans casse ni accents', () => {
    expect(normalizeSearch('  ÉTIQUETAGE ')).toBe('etiquetage');
    expect(filterRows(ROWS, 'etiquetage', FIELDS).map(r => r.reference)).toEqual(['NC-2026-001']);
    expect(filterRows(ROWS, 'TEMPÉRATURE', FIELDS).map(r => r.reference)).toEqual(['NC-2026-003']);
  });

  it('exige tous les mots, dans n\'importe quel champ', () => {
    expect(filterRows(ROWS, 'lot atelier', FIELDS).length).toBe(1);
    expect(filterRows(ROWS, 'lot pharmacie', FIELDS).length).toBe(0);
    expect(filterRows(ROWS, '002', FIELDS).map(r => r.title)).toEqual(['Défaut peinture']);
  });

  it('rend la liste entière pour une requête vide, sans la muter', () => {
    const result = filterRows(ROWS, '   ', FIELDS);
    expect(result).toEqual(ROWS);
    expect(result).not.toBe(ROWS);
  });

  it('refiltre quand la page ou la requête change', () => {
    const rows$ = new BehaviorSubject<Row[]>(ROWS);
    const control = new FormControl('', { nonNullable: true });
    const seen: number[] = [];
    const sub = searchRows(rows$, control, FIELDS).subscribe(rows => seen.push(rows.length));

    control.setValue('peinture');
    rows$.next([ROWS[0]]);
    control.setValue('');
    sub.unsubscribe();

    expect(seen).toEqual([3, 1, 0, 1]);
  });
});

describe('ListSearchComponent', () => {
  let fixture: ComponentFixture<ListSearchComponent>;
  let control: FormControl<string>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedModule, UiModule, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(ListSearchComponent);
    control = new FormControl('', { nonNullable: true });
    fixture.componentInstance.control = control;
    fixture.detectChanges();
  });

  it('écrit dans le contrôle et propose l\'effacement une fois une saisie faite', () => {
    const el: HTMLElement = fixture.nativeElement;
    const input = el.querySelector('input') as HTMLInputElement;
    expect(el.querySelector('button')).toBeNull();

    input.value = 'lot';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(control.value).toBe('lot');

    const clear = el.querySelector('button') as HTMLButtonElement;
    expect(clear.getAttribute('aria-label')).toBe('Effacer la recherche');
    clear.click();
    fixture.detectChanges();
    expect(control.value).toBe('');
  });

  it('affiche son aide : le filtre porte sur la page affichée', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('page affichée');
  });
});
