import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { MarketplaceService } from '../../marketplace.service';
import { MarketplacePackView } from '../../marketplace.types';
import { SubmitComponent } from './submit.component';

describe('SubmitComponent', () => {
  let component: SubmitComponent;
  let fixture: ComponentFixture<SubmitComponent>;
  let svc: jasmine.SpyObj<MarketplaceService>;
  let router: Router;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const created: MarketplacePackView = {
    id: 'p1', packId: 'iso', version: '1.0', publisher: 'Pub', title: 'T',
    sector: 's', norms: ['iso-9001'], priceCents: 0, currency: 'EUR',
    status: 'SUBMITTED', manifestUrl: 'https://x/y', ratingAvg: 0, ratingCount: 0
  };

  function fillValid(): void {
    component.form.setValue({
      packId: 'iso-13485-startup', version: '1.0', publisher: 'Acme', title: 'Pack MedTech',
      description: 'desc', sector: 'healthcare', norms: 'iso-13485, iso-14971',
      priceCents: 12000, currency: 'EUR',
      manifestUrl: 'https://packs.example.com/p-1.0.zip',
      manifestJson: '{"name":"x","version":"1.0"}', signatureHash: 'deadbeefdeadbeef'
    });
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<MarketplaceService>('MarketplaceService', ['submit']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [SubmitComponent],
      imports: [SharedModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: MarketplaceService, useValue: svc },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([])
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture = TestBed.createComponent(SubmitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('starts invalid (required fields)', () => {
    expect(component.form.valid).toBeFalse();
  });

  it('rejects an invalid packId pattern', () => {
    fillValid();
    component.form.controls.packId.setValue('Invalid_ID');
    expect(component.form.controls.packId.valid).toBeFalse();
  });

  it('does not submit when invalid', () => {
    component.submit();
    expect(svc.submit).not.toHaveBeenCalled();
  });

  it('parses norms CSV and submits', () => {
    svc.submit.and.returnValue(of(created));
    fillValid();
    component.submit();
    expect(svc.submit).toHaveBeenCalled();
    const req = svc.submit.calls.mostRecent().args[0];
    expect(req.norms).toEqual(['iso-13485', 'iso-14971']);
    expect(router.navigate).toHaveBeenCalledWith(['/marketplace']);
  });

  it('shows an error snackbar on failure', () => {
    svc.submit.and.returnValue(throwError(() => new Error('boom')));
    fillValid();
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });
});
