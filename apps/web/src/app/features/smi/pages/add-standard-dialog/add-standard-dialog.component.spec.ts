import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardCandidate } from '../../smi.types';
import { AddStandardDialogComponent } from './add-standard-dialog.component';

describe('AddStandardDialogComponent', () => {

  const candidatesUrl = `${environment.apiBaseUrl}/api/v1/smi/standards/candidates`;
  const addUrl = `${environment.apiBaseUrl}/api/v1/smi/standards`;
  let fixture: ComponentFixture<AddStandardDialogComponent>;
  let component: AddStandardDialogComponent;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AddStandardDialogComponent>>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  /** Ce que rend le serveur pour un client qui pilote déjà l'ISO 9001. */
  const CANDIDATES: StandardCandidate[] = [
    { standardId: 's14001', code: 'iso-14001', name: 'ISO 14001:2015 — Systèmes de management environnemental',
      version: '2015', suggested: true, sharedWith: ['iso-9001'] },
    { standardId: 's45001', code: 'iso-45001', name: 'ISO 45001:2018 — Santé et sécurité au travail',
      version: '2018', suggested: true, sharedWith: ['iso-9001'] },
    { standardId: 's27001', code: 'iso-27001', name: 'ISO/IEC 27001:2022 — Sécurité de l\'information',
      version: '2022', suggested: false, sharedWith: [] }
  ];

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<AddStandardDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [AddStandardDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AddStandardDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('propose les suggestions en tête, ISO 14001 déjà choisie', () => {
    http.expectOne(candidatesUrl).flush(CANDIDATES);
    fixture.detectChanges();

    expect(component.form.controls.standardId.value).toBe('s14001');
    const legendes = Array.from(hote().querySelectorAll('legend')).map(l => l.textContent!.trim());
    expect(legendes).toEqual(['Suggérées pour votre SMI', 'Autres normes de système de management']);
    const options = Array.from(hote().querySelectorAll('.option')).map(o => o.getAttribute('data-test'));
    expect(options).toEqual(['option-iso-14001', 'option-iso-45001', 'option-iso-27001']);
    const premiere = hote().querySelector('[data-test="option-iso-14001"]')!;
    expect(premiere.textContent).toContain('ISO 14001:2015');
    expect(premiere.textContent).toContain('Rattachée à ISO 9001');
    expect(premiere.textContent).toContain('Suggérée');
    expect((premiere.querySelector('input') as HTMLInputElement).checked).toBeTrue();
  });

  it('ajoute la norme choisie et se ferme sur l’adoption créée', () => {
    http.expectOne(candidatesUrl).flush(CANDIDATES);
    fixture.detectChanges();
    (hote().querySelector('[data-test="option-iso-45001"] input') as HTMLInputElement).click();
    fixture.detectChanges();

    component.submit();

    const req = http.expectOne(addUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ standardId: 's45001' });
    const ref = { adoptionId: 'a45001', code: 'iso-45001', name: 'ISO 45001:2018' };
    req.flush(ref, { status: 201, statusText: 'Created' });
    expect(dialogRef.close).toHaveBeenCalledWith(ref);
  });

  it('sans suggestion, une seule liste et la première norme choisie', () => {
    http.expectOne(candidatesUrl).flush(CANDIDATES.map(c => ({ ...c, suggested: false, sharedWith: [] })));
    fixture.detectChanges();

    const legendes = Array.from(hote().querySelectorAll('legend')).map(l => l.textContent!.trim());
    expect(legendes).toEqual(['Normes de système de management']);
    expect(component.form.controls.standardId.value).toBe('s14001');
  });

  it('tout est déjà adopté : la fenêtre le dit et rien ne se valide', () => {
    http.expectOne(candidatesUrl).flush([]);
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="add-none"]')).not.toBeNull();
    expect(component.form.invalid).toBeTrue();
    component.submit();
    http.expectNone(addUrl);
  });

  it('chaque refus dit quoi faire', () => {
    http.expectOne(candidatesUrl).flush(CANDIDATES);
    fixture.detectChanges();

    const refus: Array<[number, string]> = [
      [409, 'déjà adoptée'], [403, 'administrateur'], [404, 'système de management'], [500, 'Erreur serveur']
    ];
    for (const [status, attendu] of refus) {
      component.submit();
      http.expectOne(addUrl).flush('x', { status, statusText: 'KO' });
      fixture.detectChanges();
      expect(component.submitError).toContain(attendu);
      expect(hote().querySelector('[data-test="add-error"]')).not.toBeNull();
      expect(component.submitting).toBeFalse();
    }
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('un catalogue illisible s’annonce et se recharge', () => {
    http.expectOne(candidatesUrl).flush('x', { status: 503, statusText: 'KO' });
    fixture.detectChanges();
    expect(component.loadError).toBeTruthy();
    expect(hote().querySelector('[data-test="add-load-error"]')).not.toBeNull();

    component.charger();
    http.expectOne(candidatesUrl).flush(CANDIDATES);
    expect(component.loadError).toBeNull();
    expect(component.candidates.length).toBe(3);
  });

  it('annuler ferme sans rien ajouter', () => {
    http.expectOne(candidatesUrl).flush(CANDIDATES);
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
