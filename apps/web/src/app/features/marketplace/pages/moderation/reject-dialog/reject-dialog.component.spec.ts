import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../../shared/shared.module';
import { RejectDialogComponent } from './reject-dialog.component';

describe('RejectDialogComponent', () => {
  let component: RejectDialogComponent;
  let fixture: ComponentFixture<RejectDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<RejectDialogComponent>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<RejectDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [RejectDialogComponent],
      imports: [SharedModule, FormsModule, NoopAnimationsModule],
      providers: [{ provide: MatDialogRef, useValue: dialogRef }]
    }).compileComponents();
    fixture = TestBed.createComponent(RejectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('confirm with a blank reason does nothing', () => {
    component.reason = '   ';
    component.confirm();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('confirm with a reason closes with the trimmed value', () => {
    component.reason = '  incomplet  ';
    component.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith({ reason: 'incomplet' });
  });

  it('cancel closes with no result', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
