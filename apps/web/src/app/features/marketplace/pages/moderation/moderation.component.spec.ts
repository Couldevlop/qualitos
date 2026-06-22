import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { MarketplaceService } from '../../marketplace.service';
import { MarketplacePackView } from '../../marketplace.types';
import { ModerationComponent } from './moderation.component';

describe('ModerationComponent', () => {
  let component: ModerationComponent;
  let fixture: ComponentFixture<ModerationComponent>;
  let svc: jasmine.SpyObj<MarketplaceService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const submitted: MarketplacePackView = {
    id: 'p1', packId: 'iso', version: '1.0', publisher: 'Pub', title: 'T',
    sector: 's', norms: ['iso-9001'], priceCents: 0, currency: 'EUR',
    status: 'SUBMITTED', manifestUrl: 'https://x/y', ratingAvg: 0, ratingCount: 0
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<MarketplaceService>('MarketplaceService',
      ['moderationQueue', 'takeForReview', 'publish', 'reject', 'deprecate']);
    svc.moderationQueue.and.returnValue(of([submitted]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ModerationComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: MarketplaceService, useValue: svc },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModerationComponent);
    component = fixture.componentInstance;
  });

  it('renders the moderation queue', () => {
    fixture.detectChanges();
    expect(component.packs.length).toBe(1);
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('.mod-card');
    expect(cards.length).toBe(1);
  });

  it('takeForReview updates the pack in place', () => {
    svc.takeForReview.and.returnValue(of({ ...submitted, status: 'IN_REVIEW' }));
    fixture.detectChanges();
    component.takeForReview(submitted);
    expect(svc.takeForReview).toHaveBeenCalledWith('p1');
    expect(component.packs[0].status).toBe('IN_REVIEW');
  });

  it('publish removes the pack from the queue', () => {
    svc.publish.and.returnValue(of({ ...submitted, status: 'PUBLISHED' }));
    fixture.detectChanges();
    component.publish(submitted);
    expect(component.packs.length).toBe(0);
    expect(snack.open).toHaveBeenCalled();
  });

  it('reject asks for a reason and removes the pack', () => {
    dialog.open.and.returnValue({ afterClosed: () => of({ reason: 'incomplet' }) } as never);
    svc.reject.and.returnValue(of({ ...submitted, status: 'REJECTED' }));
    fixture.detectChanges();
    component.reject(submitted);
    expect(svc.reject).toHaveBeenCalledWith('p1', 'incomplet');
    expect(component.packs.length).toBe(0);
  });

  it('reject does nothing when the dialog is cancelled', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    fixture.detectChanges();
    component.reject(submitted);
    expect(svc.reject).not.toHaveBeenCalled();
    expect(component.packs.length).toBe(1);
  });
});
