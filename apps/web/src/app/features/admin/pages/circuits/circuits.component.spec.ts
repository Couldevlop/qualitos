import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { RoleView } from '../../../../core/authz/authz.types';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitStep } from '../../../../core/circuits/circuit.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CircuitsComponent, EditableStep } from './circuits.component';

describe('CircuitsComponent', () => {

  let fixture: ComponentFixture<CircuitsComponent>;
  let component: CircuitsComponent;
  let authz: jasmine.SpyObj<AuthzService>;
  let circuits: jasmine.SpyObj<CircuitService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const q = (test: string): HTMLElement | null => hote().querySelector(`[data-test="${test}"]`);

  const role = (o: Partial<RoleView>): RoleView => ({
    code: 'USER', name: null, description: null, system: true, customized: false, permissions: [], ...o
  });

  const MANAGER = role({ code: 'QUALITY_MANAGER', permissions: ['document.approve'] });
  const DIRECTEUR = role({ code: 'QUALITY_DIRECTOR', permissions: ['document.approve'] });
  const PILOTE = role({ code: 'PILOTE', name: 'Pilote de site', system: false, permissions: ['document.approve'] });
  const USER = role({ code: 'USER', permissions: ['nc.create'] });

  async function setup(steps: CircuitStep[] = [], fails = false): Promise<void> {
    authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['roles']);
    authz.roles.and.returnValue(of([MANAGER, DIRECTEUR, PILOTE, USER]));
    circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['circuit', 'save']);
    circuits.circuit.and.returnValue(fails ? throwError(() => new Error('503'))
      : of({ subject: 'document-version', steps }));
    circuits.save.and.callFake((_s, st) => of({ subject: 'document-version' as const, steps: st }));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CircuitsComponent],
      imports: [SharedModule, UiModule, DragDropModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthzService, useValue: authz },
        { provide: CircuitService, useValue: circuits },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CircuitsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Simule un dépôt du glisser-déposer : depuis la palette, ou au sein du parcours. */
  function depot(item: EditableStep | RoleView, from: 'palette' | 'parcours', previousIndex: number,
                 currentIndex: number): void {
    const parcours = { data: component.steps };
    const palette = { data: [] };
    component.drop({
      item: { data: item }, container: parcours, previousContainer: from === 'parcours' ? parcours : palette,
      previousIndex, currentIndex
    } as unknown as CdkDragDrop<EditableStep[], unknown, EditableStep | RoleView>);
    fixture.detectChanges();
  }

  it('sans circuit, dit qu’une approbation suffit et invite à déposer un rôle', async () => {
    await setup();

    expect(q('resume')!.textContent).toContain('une seule approbation suffit');
    expect(q('depot-vide')).not.toBeNull();
    expect(q('barre')).toBeNull();
  });

  it('ne propose que les rôles qui ont le droit d’approuver, et renvoie les autres vers les droits', async () => {
    await setup();

    expect(q('palette-QUALITY_MANAGER')).not.toBeNull();
    expect(q('palette-PILOTE')!.textContent).toContain('Pilote de site');
    expect(q('palette-USER')).toBeNull();
    expect(hote().querySelector('.palette__autres')!.textContent).toContain('Utilisateur');
    expect(hote().querySelector('a[href="/admin/roles"]')).not.toBeNull();
  });

  it('un rôle déposé devient une étape à l’endroit du dépôt, et le résumé suit', async () => {
    await setup();

    depot(DIRECTEUR, 'palette', 0, 0);
    depot(MANAGER, 'palette', 0, 0);

    expect(component.steps.map(s => s.roleCode)).toEqual(['QUALITY_MANAGER', 'QUALITY_DIRECTOR']);
    expect(component.steps[0].name).toBe('Manager qualité');
    expect(q('resume')!.textContent).toContain('une personne « Manager qualité », puis une personne « Directeur qualité »');
    expect(q('barre')).not.toBeNull();
  });

  it('une étape glissée change de place ; les flèches font de même au clavier', async () => {
    await setup([
      { name: 'Relecture', roleCode: 'QUALITY_MANAGER', minApprovals: 2 },
      { name: 'Signature', roleCode: 'QUALITY_DIRECTOR', minApprovals: 1 }
    ]);

    depot(component.steps[1], 'parcours', 1, 0);
    expect(component.steps.map(s => s.name)).toEqual(['Signature', 'Relecture']);

    component.move(1, -1);
    expect(component.steps.map(s => s.name)).toEqual(['Relecture', 'Signature']);
    component.move(0, -1);
    expect(component.steps.map(s => s.name)).toEqual(['Relecture', 'Signature']);
    expect(component.dirty).toBeFalse();
  });

  it('le bouton + d’un rôle ajoute une étape ; le compteur reste entre 1 et 10', async () => {
    await setup();

    (q('ajouter-PILOTE') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.steps.length).toBe(1);

    (q('moins-0') as HTMLButtonElement).click();
    expect(component.steps[0].minApprovals).toBe(1);
    for (let i = 0; i < 12; i++) component.approvals(component.steps[0], 1);
    fixture.detectChanges();
    expect(q('approbations-0')!.textContent!.trim()).toBe('10');
    expect(q('resume')!.textContent).toContain('10 personnes « Pilote de site »');
  });

  it('enregistre les étapes dans leur ordre, noms rognés', async () => {
    await setup();
    component.add(MANAGER);
    component.add(DIRECTEUR);
    component.rename(component.steps[0], '  Relecture  ');
    component.approvals(component.steps[0], 1);
    fixture.detectChanges();

    (q('enregistrer') as HTMLButtonElement).click();

    expect(circuits.save).toHaveBeenCalledWith('document-version', [
      { name: 'Relecture', roleCode: 'QUALITY_MANAGER', minApprovals: 2 },
      { name: 'Directeur qualité', roleCode: 'QUALITY_DIRECTOR', minApprovals: 1 }
    ]);
    fixture.detectChanges();
    expect(component.dirty).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('une étape sans nom bloque l’enregistrement', async () => {
    await setup();
    component.add(MANAGER);
    component.rename(component.steps[0], '   ');
    fixture.detectChanges();

    expect(component.invalid).toBeTrue();
    expect((q('enregistrer') as HTMLButtonElement).disabled).toBeTrue();
    component.save();
    expect(circuits.save).not.toHaveBeenCalled();
  });

  it('retirer toutes les étapes revient à l’approbation simple', async () => {
    await setup([{ name: 'Relecture', roleCode: 'QUALITY_MANAGER', minApprovals: 1 }]);

    (q('retirer-0') as HTMLButtonElement).click();
    fixture.detectChanges();
    (q('enregistrer') as HTMLButtonElement).click();

    expect(circuits.save).toHaveBeenCalledWith('document-version', []);
  });

  it('annuler rend le circuit enregistré', async () => {
    await setup([{ name: 'Relecture', roleCode: 'QUALITY_MANAGER', minApprovals: 1 }]);
    component.add(DIRECTEUR);
    expect(component.dirty).toBeTrue();

    component.discard();

    expect(component.steps.map(s => s.name)).toEqual(['Relecture']);
    expect(component.dirty).toBeFalse();
  });

  it('signale une étape dont le rôle a perdu le droit d’approuver', async () => {
    await setup([{ name: 'Contrôle', roleCode: 'USER', minApprovals: 1 }]);

    expect(component.orphan(component.steps[0])).toBeTrue();
    expect(hote().querySelector('.etape__alerte')).not.toBeNull();
  });

  it('pas plus de dix étapes', async () => {
    await setup();
    for (let i = 0; i < 12; i++) component.add(MANAGER);
    fixture.detectChanges();

    expect(component.steps.length).toBe(10);
    expect((q('ajouter-QUALITY_MANAGER') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('un échec de chargement se dit, et se réessaie', async () => {
    await setup([], true);

    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
    expect(snack.open).toHaveBeenCalled();
  });

  it('un échec d’enregistrement garde les modifications', async () => {
    await setup();
    circuits.save.and.returnValue(throwError(() => new Error('422')));
    component.add(MANAGER);

    component.save();

    expect(component.steps.length).toBe(1);
    expect(component.dirty).toBeTrue();
    expect(component.saving).toBeFalse();
  });
});
