import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';

import { AuthzService } from '../../../core/authz/authz.service';
import { UiModule } from '../ui.module';
import { ScopeNoteComponent } from './scope-note.component';

describe('ScopeNoteComponent', () => {

  let fixture: ComponentFixture<ScopeNoteComponent>;
  const droit$ = new BehaviorSubject<boolean>(true);
  let authz: jasmine.SpyObj<AuthzService>;

  beforeEach(async () => {
    authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(droit$);
    await TestBed.configureTestingModule({
      imports: [UiModule],
      providers: [{ provide: AuthzService, useValue: authz }]
    }).compileComponents();
    fixture = TestBed.createComponent(ScopeNoteComponent);
    fixture.componentRef.setInput('permission', 'nc.view.all');
    fixture.detectChanges();
  });

  const note = (): HTMLElement | null =>
    (fixture.nativeElement as HTMLElement).querySelector('[data-test="scope-note"]');

  it('se tait quand l’utilisateur voit tout', () => {
    expect(authz.can).toHaveBeenCalledWith('nc.view.all');
    expect(note()).toBeNull();
  });

  it('dit qu’une partie seulement est visible quand « voir tout » manque', () => {
    droit$.next(false);
    fixture.detectChanges();
    expect(note()).not.toBeNull();
    droit$.next(true);
  });
});
