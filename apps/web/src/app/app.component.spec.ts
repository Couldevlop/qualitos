import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { AppComponent } from './app.component';
import { AppUpdateService } from './core/update/app-update.service';

describe('AppComponent', () => {
  let updates: jasmine.SpyObj<AppUpdateService>;

  beforeEach(async () => {
    updates = jasmine.createSpyObj<AppUpdateService>('AppUpdateService', ['start']);
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [AppComponent],
      providers: [{ provide: AppUpdateService, useValue: updates }]
    }).compileComponents();
  });

  it('creates the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  /**
   * Sans cette mise en route, une livraison resterait invisible pour qui a déjà
   * ouvert l'application : le service worker continuerait de servir son cache.
   */
  it('met en route la surveillance des livraisons', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(updates.start).toHaveBeenCalled();
  });
});
