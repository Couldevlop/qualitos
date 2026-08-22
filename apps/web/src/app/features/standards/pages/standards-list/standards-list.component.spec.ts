import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AdoptionsPage, StandardsPage, StandardSummary
} from '../../standards.types';
import { StandardsListComponent } from './standards-list.component';

function summary(overrides: Partial<StandardSummary> = {}): StandardSummary {
  return { id: 'iso-9001', code: 'ISO 9001', fullName: 'QMS', family: 'HLS', ...overrides } as StandardSummary;
}

function page<T>(content: T[]): { content: T[] } {
  return { content };
}

const ADOPTION = { id: 'adopt-1' } as AdoptionResponse;

describe('StandardsListComponent', () => {
  let component: StandardsListComponent;
  let fixture: ComponentFixture<StandardsListComponent>;
  let svc: jasmine.SpyObj<StandardsService>;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<StandardsService>('StandardsService',
      ['listCatalog', 'listAdoptions', 'adopt']);
    svc.listCatalog.and.returnValue(of(page([summary()]) as StandardsPage));
    svc.listAdoptions.and.returnValue(of(page<AdoptionResponse>([]) as AdoptionsPage));

    await TestBed.configureTestingModule({
      declarations: [StandardsListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsService, useValue: svc },
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StandardsListComponent);
    component = fixture.componentInstance;
  });

  it('declares the catalog and adoption table columns', () => {
    expect(component.catalogCols).toEqual(['code', 'fullName', 'family', 'status', 'cycle', 'actions']);
    expect(component.adoptCols).toEqual(['code', 'status', 'scope', 'body', 'target']);
  });

  it('loads catalog and adoptions on init', () => {
    fixture.detectChanges();
    expect(svc.listCatalog).toHaveBeenCalled();
    expect(svc.listAdoptions).toHaveBeenCalled();
    let catalog: StandardSummary[] = [];
    component.catalog$.subscribe(c => (catalog = c));
    expect(catalog.length).toBe(1);
  });

  it('navigates to an adoption detail on open', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.open({ id: 'adopt-9' } as AdoptionResponse);
    expect(nav).toHaveBeenCalledWith(['/standards/adoptions', 'adopt-9']);
  });

  it('adopt success clears the in-flight flag and navigates to the new adoption', () => {
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    svc.adopt.and.returnValue(of(ADOPTION));

    component.adopt(summary());
    expect(svc.adopt).toHaveBeenCalledWith({ standardId: 'iso-9001' });
    expect(component.adopting).toBeUndefined();
    expect(nav).toHaveBeenCalledWith(['/standards/adoptions', 'adopt-1']);
  });

  it('adopt 409 conflict surfaces the already-adopted message and reloads', () => {
    fixture.detectChanges();
    svc.listCatalog.calls.reset();
    const snack = TestBed.inject(MatSnackBar);
    const snackSpy = spyOn(snack, 'open');
    svc.adopt.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.adopt(summary());
    expect(component.adopting).toBeUndefined();
    expect(snackSpy).toHaveBeenCalled();
    // reload re-fetches the catalog after a conflict
    expect(svc.listCatalog).toHaveBeenCalled();
  });

  it('adopt generic error surfaces a failure message', () => {
    fixture.detectChanges();
    const snack = TestBed.inject(MatSnackBar);
    const snackSpy = spyOn(snack, 'open');
    svc.adopt.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.adopt(summary());
    expect(component.adopting).toBeUndefined();
    expect(snackSpy).toHaveBeenCalled();
  });

  // ---- Référentiels du tenant (§8) ------------------------------------------

  it('distingue à l\'œil un référentiel maison d\'une norme livrée', () => {
    // Sans ce repère, un utilisateur croit pouvoir éditer ISO 9001, ou cherche
    // en vain sa procédure au milieu de soixante normes.
    svc.listCatalog.and.returnValue(of(page([
      summary({ id: '1', code: 'iso-9001', fullName: 'ISO 9001', owned: false }),
      summary({ id: '2', code: 'PRO-002', fullName: 'Audit interne', owned: true })
    ]) as StandardsPage));

    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const badges = el.querySelectorAll('.owned-badge');
    expect(badges.length).toBe(1);
    expect(badges[0].textContent).toContain('Procédure interne');
  });

  it('filtre le catalogue sur ce que l\'utilisateur cherche', () => {
    svc.listCatalog.and.returnValue(of(page([
      summary({ id: '1', owned: false }),
      summary({ id: '2', owned: true })
    ]) as StandardsPage));
    fixture.detectChanges();

    const seen: StandardSummary[][] = [];
    component.catalog$.subscribe(c => seen.push(c));

    component.setScope('OWNED');
    component.setScope('PLATFORM');
    component.setScope('ALL');

    expect(seen[0].length).toBe(2);
    expect(seen[1].map(s => s.id)).toEqual(['2']);
    expect(seen[2].map(s => s.id)).toEqual(['1']);
    expect(seen[3].length).toBe(2);
  });

  it('ouvre le choix de la procédure source et recharge après une création', () => {
    fixture.detectChanges();
    const dialog = TestBed.inject(MatDialog);
    const ref = { afterClosed: () => of(true) } as MatDialogRef<unknown>;
    const open = spyOn(dialog, 'open').and.returnValue(ref as MatDialogRef<unknown, unknown>);
    svc.listCatalog.calls.reset();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.create-procedure-btn')!.click();

    expect(open).toHaveBeenCalled();
    // Rechargé, et non reconstruit à la main : le serveur seul connaît le code
    // et la version que le référentiel a réellement reçus.
    expect(svc.listCatalog).toHaveBeenCalled();
  });

  it('ne recharge rien si le choix de la procédure est abandonné', () => {
    fixture.detectChanges();
    const dialog = TestBed.inject(MatDialog);
    spyOn(dialog, 'open').and.returnValue(
      { afterClosed: () => of(undefined) } as MatDialogRef<unknown, unknown>);
    svc.listCatalog.calls.reset();

    component.createFromProcedure();

    expect(svc.listCatalog).not.toHaveBeenCalled();
  });
});
