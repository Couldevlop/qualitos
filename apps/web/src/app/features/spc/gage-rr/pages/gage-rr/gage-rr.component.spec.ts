import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiModule } from '../../../../../shared/ui/ui.module';
import { GageRrComponent } from './gage-rr.component';

/** Le sous-menu Gage R&R existe et s'ouvre ; son contenu reste à spécifier. */
describe('GageRrComponent', () => {
  let fixture: ComponentFixture<GageRrComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GageRrComponent],
      imports: [UiModule]
    }).compileComponents();
    fixture = TestBed.createComponent(GageRrComponent);
    fixture.detectChanges();
  });

  it('affiche l\'en-tête de la page, et rien d\'autre', () => {
    const hote = fixture.nativeElement as HTMLElement;
    expect(hote.querySelector('qos-page-header')?.textContent).toContain('Gage R&R');
    expect(hote.querySelector('.qos-page')?.children.length).toBe(1);
  });
});
