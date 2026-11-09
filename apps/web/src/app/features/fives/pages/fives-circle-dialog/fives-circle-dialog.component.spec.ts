import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CirclesService } from '../../../circles/circles.service';
import { CircleResponse, CirclesPage } from '../../../circles/circles.types';
import {
  FivesCircleDialogComponent, FivesCircleDialogData
} from './fives-circle-dialog.component';

/**
 * « Un audit 5S peut alimenter un Cercle de Qualité. »
 *
 * <p>Ce que ces bancs tiennent : la fenêtre ne propose que des cercles actifs,
 * parce que le serveur refuse les autres et qu'une commande qui échoue après
 * coup vaut moins qu'une commande absente ; elle dit en clair qu'aucun cercle
 * n'est actif plutôt que de montrer une liste vide ; et elle rappelle en
 * lecture la note relevée, sur laquelle l'idée sera défendue en séance.
 */
describe('FivesCircleDialogComponent', () => {

  let fixture: ComponentFixture<FivesCircleDialogComponent>;
  let component: FivesCircleDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FivesCircleDialogComponent>>;
  let circles: jasmine.SpyObj<CirclesService>;

  const cercle = (id: string, name: string): CircleResponse => ({
    id, tenantId: 't1', name, status: 'ACTIVE',
    createdAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z'
  } as CircleResponse);

  const page = (content: CircleResponse[]): CirclesPage => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 50
  } as CirclesPage);

  const donnees: FivesCircleDialogData = {
    zone: 'Atelier B', pillar: 'SEIRI', pillarLabel: 'Seiri', score: 2,
    note: 'Palettes vides stockées dans l’allée'
  };

  async function monter(reponse = of(page([cercle('c-1', 'Cercle ligne 1'),
                                           cercle('c-2', 'Cercle maintenance')]))): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<FivesCircleDialogComponent>>('MatDialogRef', ['close']);
    circles = jasmine.createSpyObj<CirclesService>('CirclesService', ['listCircles']);
    circles.listCircles.and.returnValue(reponse);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [FivesCircleDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CirclesService, useValue: circles },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: donnees }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(FivesCircleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => monter());

  // ---------- les cercles proposés ----------

  it('ne demande que les cercles actifs', () => {
    expect(circles.listCircles).toHaveBeenCalledWith(0, 100, 'ACTIVE');
  });

  it('dit qu’aucun cercle n’est actif, au lieu d’une liste vide', async () => {
    await monter(of(page([])));

    expect(component.empty).toBeTrue();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="aucun-cercle"]')).not.toBeNull();
    // Le choix du cercle disparaît : il n'y a rien à choisir.
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="cercle"]')).toBeNull();
  });

  it('dit l’échec du chargement sans se prétendre vide', async () => {
    await monter(throwError(() => ({ status: 500 })));

    expect(component.loadError).not.toBeNull();
    expect(component.empty).toBeFalse();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="erreur-cercles"]')).not.toBeNull();
  });

  // ---------- ce que l'idée reprend ----------

  it('rappelle en lecture la zone, le pilier et la note relevée', () => {
    const releve = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="releve"]')!.textContent ?? '';
    expect(releve).toContain('Atelier B');
    expect(releve).toContain('Seiri');
    expect(releve).toContain('2 / 10');
    // Le pilier s'affiche traduit, jamais en code brut.
    expect(releve).not.toContain('SEIRI');
  });

  it('propose un intitulé, et reprend la remarque de l’auditeur', () => {
    const v = component.form.getRawValue();
    expect(v.title).toBe('Améliorer Seiri en zone Atelier B');
    expect(v.description).toBe('Palettes vides stockées dans l’allée');
  });

  it('ramène un intitulé trop long à la limite du serveur', () => {
    const titre = FivesCircleDialogComponent.titleFor({
      ...donnees, zone: 'Z'.repeat(400)
    });
    expect(titre.length).toBe(255);
    expect(titre.endsWith('…')).toBeTrue();
  });

  // ---------- ce qui part ----------

  it('ne dépose rien sans cercle', () => {
    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rend le cercle, le pilier et ce qui est proposé', () => {
    component.form.patchValue({ circleId: 'c-2' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      circleId: 'c-2',
      pillar: 'SEIRI',
      title: 'Améliorer Seiri en zone Atelier B',
      description: 'Palettes vides stockées dans l’allée'
    });
  });

  it('laisse le constat vide à vide, sans chaîne blanche', () => {
    component.form.patchValue({ circleId: 'c-1', description: '   ' });

    component.submit();

    expect(dialogRef.close.calls.mostRecent().args[0]!.description).toBeUndefined();
  });

  it('ferme sans rien rendre quand on renonce', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
