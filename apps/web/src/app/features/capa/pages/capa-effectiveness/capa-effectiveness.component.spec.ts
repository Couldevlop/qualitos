import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaEffectivenessRow, CapaEffectivenessSummary } from '../../capa.types';
import { CapaEffectivenessComponent } from './capa-effectiveness.component';

/**
 * L'écran d'efficacité mesurée.
 *
 * <p>Ce qu'il doit garantir : ne jamais afficher un taux là où le serveur a
 * refusé d'en calculer un — une fenêtre en cours ou l'absence d'occurrence
 * antérieure se disent en toutes lettres — et mettre en avant l'écart entre ce
 * qui a été déclaré à la clôture et ce que le terrain a montré.
 */
describe('CapaEffectivenessComponent', () => {

  const row = (over: Partial<CapaEffectivenessRow> = {}): CapaEffectivenessRow => ({
    capaId: 'c-1', title: 'Dérive dimensionnelle', criticity: 'MAJOR',
    closedAt: '2026-03-12T09:00:00Z', status: 'MEASURED',
    occurrencesBefore: 12, occurrencesAfter: 1, ratePercent: 92,
    aggravated: false, daysObserved: 180, daysInWindow: 180,
    declaredEffective: true, preciseMatch: true, ...over
  });

  const summary = (over: Partial<CapaEffectivenessSummary> = {}): CapaEffectivenessSummary => ({
    windowMonths: 6, measured: 1, inObservation: 0, notMeasurable: 0,
    averageRatePercent: 92, aggravated: 0, declaredButFailed: 0,
    truncated: false, rows: [row()], ...over
  });

  let fixture: ComponentFixture<CapaEffectivenessComponent>;
  let component: CapaEffectivenessComponent;
  let service: jasmine.SpyObj<CapaService>;
  let snack: { open: jasmine.Spy };

  beforeEach(async () => {
    service = jasmine.createSpyObj<CapaService>('CapaService', ['effectiveness']);
    snack = { open: jasmine.createSpy('open') };
    await TestBed.configureTestingModule({
      declarations: [CapaEffectivenessComponent],
      imports: [SharedModule, UiModule, MatButtonToggleModule, RouterTestingModule,
        NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaEffectivenessComponent);
    component = fixture.componentInstance;
  });

  it('demande six mois par défaut et affiche la synthèse', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary()));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(service.effectiveness).toHaveBeenCalledWith(6);
    expect(fixture.nativeElement.textContent).toContain('92 %');
    expect(component.loading).toBeFalse();
  }));

  it('change de fenêtre sans rejouer la même', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary()));
    fixture.detectChanges();
    tick();

    component.changeWindow(12);
    tick();
    component.changeWindow(12);
    tick();

    expect(service.effectiveness).toHaveBeenCalledWith(12);
    expect(service.effectiveness).toHaveBeenCalledTimes(2);
  }));

  it('dit qu’il est trop tôt plutôt que d’afficher un taux partiel', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({
      measured: 0, inObservation: 1, averageRatePercent: undefined,
      rows: [row({ status: 'IN_OBSERVATION', ratePercent: undefined,
        occurrencesAfter: 0, daysObserved: 60 })]
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const texte = fixture.nativeElement.textContent;
    expect(texte).toContain('Trop tôt pour conclure');
    expect(texte).not.toContain('%');
  }));

  it('nomme l’absence d’occurrence antérieure au lieu de conclure', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({
      measured: 0, notMeasurable: 1, averageRatePercent: undefined,
      rows: [row({ status: 'NOT_MEASURABLE', ratePercent: undefined, occurrencesBefore: 0 })]
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune occurrence antérieure');
  }));

  it('signale un dossier déclaré efficace que la mesure dément', fakeAsync(() => {
    const dementie = row({ ratePercent: 0, declaredEffective: true, occurrencesAfter: 12 });
    service.effectiveness.and.returnValue(of(summary({
      declaredButFailed: 1, averageRatePercent: 0, rows: [dementie]
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.contradicted(dementie)).toBeTrue();
    expect(fixture.nativeElement.querySelector('.contradicted')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.card.alarm')).toBeTruthy();
  }));

  it('avertit quand le périmètre a été tronqué', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({ truncated: true })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.notice')).toBeTruthy();
  }));

  it('dit qu’un rapprochement par catégorie ne vaut qu’indication', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({ rows: [row({ preciseMatch: false })] })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Rapprochement par catégorie');
  }));

  it('affiche un état vide explicite quand rien n’est mesurable', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({
      measured: 0, averageRatePercent: undefined, rows: []
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  }));

  it('cesse de faire patienter quand le chargement échoue', fakeAsync(() => {
    service.effectiveness.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();

    expect(component.loading).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Impossible de charger');
  }));

  it('arrondit les mois observés plutôt que d’afficher une fausse précision', () => {
    expect(component.observedMonths(row({ daysObserved: 59 }))).toBe(2);
    expect(component.windowMonths(row({ daysInWindow: 180 }))).toBe(6);
  });

  it('classe le taux par palier et non par nuance continue', () => {
    expect(component.rateClass(row({ ratePercent: 92 }))).toBe('rate-good');
    expect(component.rateClass(row({ ratePercent: 50 }))).toBe('rate-fair');
    expect(component.rateClass(row({ ratePercent: 10 }))).toBe('rate-poor');
    expect(component.rateClass(row({ ratePercent: undefined }))).toBe('rate-unknown');
  });
});
