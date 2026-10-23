import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CompetencyMatrixService } from '../../../training/competency-matrix/competency-matrix.service';
import { CompetencyWorkshopsComponent } from './competency-workshops.component';

/** Les ateliers de la matrice : lecture, édition locale, règles, enregistrement. */
describe('CompetencyWorkshopsComponent', () => {
  let fixture: ComponentFixture<CompetencyWorkshopsComponent>;
  let component: CompetencyWorkshopsComponent;
  let service: jasmine.SpyObj<CompetencyMatrixService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  async function setup(lecture: 'ok' | 'erreur' = 'ok'): Promise<void> {
    service = jasmine.createSpyObj<CompetencyMatrixService>('CompetencyMatrixService', ['workshops', 'updateWorkshops']);
    service.workshops.and.returnValue(lecture === 'ok'
      ? of({ names: ['Usinage', 'Assemblage', 'Qualité', 'Maintenance'], defaults: true })
      : throwError(() => ({ status: 500 })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [CompetencyWorkshopsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: CompetencyMatrixService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CompetencyWorkshopsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('lit les ateliers, dit qu\'ils sont ceux de la plateforme, et ne propose rien d\'enregistrer', async () => {
    await setup();
    expect(component.names).toEqual(['Usinage', 'Assemblage', 'Qualité', 'Maintenance']);
    expect(hote().querySelectorAll('[data-test="atelier-nom"]').length).toBe(4);
    expect(hote().textContent).toContain('Ce sont aujourd\'hui les ateliers proposés par la plateforme.');
    expect(component.dirty).toBeFalse();
    expect(un('enregistrer')).toBeNull();
  });

  it('ajoute, renomme, déplace et retire, sans sortir de la liste', async () => {
    await setup();
    component.newName = '   ';
    component.add();
    expect(component.names.length).toBe(4);

    component.newName = ' Peinture ';
    component.add();
    expect(component.names[4]).toBe('Peinture');
    expect(component.newName).toBe('');

    component.rename(0, 'Usinage CN');
    component.move(0, 1);
    expect(component.names.slice(0, 2)).toEqual(['Assemblage', 'Usinage CN']);
    component.move(0, -1);
    component.move(4, 1);
    expect(component.names.slice(0, 2)).toEqual(['Assemblage', 'Usinage CN']);

    component.remove(4);
    expect(component.names).toEqual(['Assemblage', 'Usinage CN', 'Qualité', 'Maintenance']);
    expect(component.dirty).toBeTrue();
    expect(component.trackByIndex(3)).toBe(3);

    component.discard();
    expect(component.names).toEqual(['Usinage', 'Assemblage', 'Qualité', 'Maintenance']);
    expect(component.dirty).toBeFalse();
  });

  it('dit ce qui empêche d\'enregistrer : liste vide, nom vide ou trop long, doublon, trop d\'ateliers', async () => {
    await setup();
    component.names = [];
    expect(component.problems).toEqual(['Il faut au moins un atelier.']);
    component.names = ['Usinage', ' '];
    expect(component.problems).toEqual(['Chaque atelier a un nom de 1 à 60 caractères.']);
    component.names = ['Usinage', 'x'.repeat(61)];
    expect(component.problems.length).toBe(1);
    component.names = ['Qualité', ' qualite '];
    expect(component.problems).toEqual(['Deux ateliers portent le même nom.']);
    component.names = Array.from({ length: 31 }, (_, i) => 'Atelier ' + i);
    expect(component.problems).toEqual(['30 ateliers au plus.']);

    fixture.detectChanges();
    expect(un('problemes')).not.toBeNull();
    expect(un('enregistrer')?.hasAttribute('disabled')).toBeTrue();
    component.save();
    expect(service.updateWorkshops).not.toHaveBeenCalled();
  });

  it('enregistre la liste nettoyée, puis reprend la réponse du serveur', async () => {
    await setup();
    service.updateWorkshops.and.returnValue(of({ names: ['Usinage', 'Peinture'], defaults: false }));
    component.names = ['Usinage ', 'Peinture'];
    fixture.detectChanges();
    un('enregistrer')!.click();
    expect(service.updateWorkshops).toHaveBeenCalledWith(['Usinage', 'Peinture']);
    expect(component.defaults).toBeFalse();
    expect(component.dirty).toBeFalse();
    expect(component.saving).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Ateliers enregistrés.', undefined, { duration: 4000 });
  });

  it('n\'enregistre rien qui n\'ait changé', async () => {
    await setup();
    component.save();
    expect(service.updateWorkshops).not.toHaveBeenCalled();
  });

  it('garde la saisie et signale un enregistrement refusé', async () => {
    await setup();
    service.updateWorkshops.and.returnValue(throwError(() => ({ status: 404 })));
    component.names = ['Peinture'];
    component.save();
    expect(component.names).toEqual(['Peinture']);
    expect(component.saving).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Les ateliers n\'ont pas pu être enregistrés.', undefined, { duration: 5000 });
  });

  it('signale l\'échec de lecture et relit sur demande', async () => {
    await setup('erreur');
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
    service.workshops.and.returnValue(of({ names: ['Usinage'], defaults: false }));
    component.charger();
    fixture.detectChanges();
    expect(component.failed).toBeFalse();
    expect(component.names).toEqual(['Usinage']);
    expect(hote().textContent).not.toContain('proposés par la plateforme');
  });
});
