import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
});
