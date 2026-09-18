import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpService } from '../../apqp.service';
import { ApqpProject } from '../../apqp.types';
import { ApqpProjectListComponent } from './apqp-project-list.component';

/**
 * La liste des projets APQP : l'écran d'entrée du module.
 *
 * <p>Ce que ce banc tient : les compteurs du serveur sont affichés tels quels
 * (un recalcul local finirait par démentir le serveur), une erreur de chargement
 * dit ce qui s'est passé au lieu de laisser une table figée, et les commandes
 * d'écriture ne sont proposées qu'à qui en a le droit.
 *
 * <p>Les bannières passent par `deferredView`, donc en macrotâche : on avance le
 * temps avec `tick()` avant de les lire, faute de quoi on lirait l'écran avant
 * qu'elles n'y soient.
 */
describe('ApqpProjectListComponent', () => {

  let fixture: ComponentFixture<ApqpProjectListComponent>;
  let component: ApqpProjectListComponent;
  let service: jasmine.SpyObj<ApqpService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: Router;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  function projet(partiel: Partial<ApqpProject>): ApqpProject {
    return {
      id: 'pr1', name: 'Support moteur', type: 'NPI', customer: 'Renault',
      reference: 'SM-2026', description: null,
      deliverablesTotal: 12, deliverablesDone: 3, ppapTotal: 5, ppapDone: 1,
      createdAt: '2026-09-01T08:00:00Z', updatedAt: '2026-09-10T08:00:00Z',
      ...partiel
    };
  }

  async function monter(
    projets: ApqpProject[] | Error, roles: string[] = ['QUALITY_MANAGER']
  ): Promise<void> {
    service = jasmine.createSpyObj<ApqpService>(
      'ApqpService', ['projects', 'createProject', 'deleteProject']);
    service.projects.and.returnValue(
      projets instanceof Error ? throwError(() => ({ status: 500 })) : of(projets));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue(
      { afterClosed: () => of(undefined) } as unknown as MatDialogRef<unknown>);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpProjectListComponent],
      imports: [SharedModule, UiModule, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: ApqpService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: AuthService, useValue: { hasAnyRole: () => roles.length > 0 } }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(ApqpProjectListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('affiche les compteurs du serveur, livrables et dossier PPAP', fakeAsync(async () => {
    await monter([
      projet({}),
      projet({ id: 'pr2', name: 'Transfert ligne 4', type: 'TOW', customer: null,
               deliverablesTotal: 8, deliverablesDone: 8, ppapTotal: 0, ppapDone: 0 })
    ]);
    tick();
    fixture.detectChanges();

    const lignes = hote().querySelectorAll('[data-test=ligne-projet]');
    expect(lignes.length).toBe(2);

    // Deux compteurs et non un pourcentage : « 3 / 12 » dit aussi la TAILLE du
    // projet, qu'un « 25 % » effacerait.
    const livrables = hote().querySelectorAll('[data-test=compte-livrables]');
    expect(livrables[0].textContent).toContain('3 / 12');
    const ppap = hote().querySelectorAll('[data-test=compte-ppap-projet]');
    expect(ppap[0].textContent).toContain('1 / 5');

    // Le client se lit sous le nom : deux projets peuvent porter le même nom
    // chez deux donneurs d'ordre.
    expect(lignes[0].textContent).toContain('Renault');
  }));

  it('ouvre le projet cliqué', fakeAsync(async () => {
    await monter([projet({})]);
    tick();
    fixture.detectChanges();

    hote().querySelector<HTMLElement>('[data-test=ligne-projet]')!.click();

    expect(router.navigate).toHaveBeenCalledWith(['/apqp', 'pr1']);
  }));

  it('filtre par type sans redemander la liste au serveur', fakeAsync(async () => {
    await monter([
      projet({}),
      projet({ id: 'pr2', name: 'Transfert ligne 4', type: 'TOW' })
    ]);
    tick();
    fixture.detectChanges();
    expect(hote().querySelectorAll('[data-test=ligne-projet]').length).toBe(2);

    component.typeFilter.setValue('TOW');
    tick();
    fixture.detectChanges();

    expect(hote().querySelectorAll('[data-test=ligne-projet]').length).toBe(1);
    expect(hote().querySelector('[data-test=ligne-projet]')!.textContent)
      .toContain('Transfert ligne 4');
  }));

  it('affiche une bannière quand le chargement échoue, et vide la table', fakeAsync(async () => {
    await monter(new Error('boum'));
    tick();
    fixture.detectChanges();

    // `return []` dans le `catchError` aurait rendu un observable VIDE : la
    // table aurait gardé ses lignes précédentes sous la bannière.
    expect(hote().querySelector('[data-test=banniere-erreur]')).not.toBeNull();
    expect(hote().querySelectorAll('[data-test=ligne-projet]').length).toBe(0);
  }));

  it('dit quoi faire quand aucun projet n\'existe', fakeAsync(async () => {
    await monter([]);
    tick();
    fixture.detectChanges();

    expect(hote().querySelector('[data-test=aucun-projet]')).not.toBeNull();
  }));

  it('crée un projet et l\'ouvre aussitôt', fakeAsync(async () => {
    await monter([]);
    tick();
    fixture.detectChanges();
    dialog.open.and.returnValue({
      afterClosed: () => of({ name: 'Nouveau', type: 'NPI' })
    } as unknown as MatDialogRef<unknown>);
    service.createProject.and.returnValue(of(projet({ id: 'pr9', name: 'Nouveau' })));

    hote().querySelector<HTMLButtonElement>('[data-test=nouveau-projet]')!.click();

    expect(service.createProject).toHaveBeenCalledWith({ name: 'Nouveau', type: 'NPI' });
    // On ouvre le projet créé : son cycle est ce qu'on vient chercher, et
    // revenir à la liste obligerait à le retrouver.
    expect(router.navigate).toHaveBeenCalledWith(['/apqp', 'pr9']);
  }));

  it('ne supprime rien si la question est déclinée', fakeAsync(async () => {
    await monter([projet({})]);
    tick();
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(false);

    component.supprimer(projet({}), new MouseEvent('click'));

    expect(service.deleteProject).not.toHaveBeenCalled();
  }));

  it('supprime sans ouvrir le projet qu\'on détruit', fakeAsync(async () => {
    await monter([projet({})]);
    tick();
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(true);
    service.deleteProject.and.returnValue(of(void 0));

    hote().querySelector<HTMLButtonElement>('[data-test=supprimer-projet]')!.click();
    tick();

    expect(service.deleteProject).toHaveBeenCalledWith('pr1');
    // La ligne entière est cliquable : sans l'arrêt de propagation, supprimer
    // ouvrirait le projet juste avant de le détruire.
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('cache les commandes d\'écriture à qui n\'a pas le droit d\'écrire', fakeAsync(async () => {
    await monter([projet({})], []);
    tick();
    fixture.detectChanges();

    expect(component.editable).toBeFalse();
    expect(hote().querySelector('[data-test=nouveau-projet]')).toBeNull();
    expect(hote().querySelector('[data-test=supprimer-projet]')).toBeNull();
  }));

  it('nomme les quatre types en clair, jamais en constante serveur', fakeAsync(async () => {
    await monter([]);
    tick();

    expect(component.typeLabel('NPI')).toContain('NPI');
    expect(component.typeLabel('TOW')).toContain('ToW');
    expect(component.typeLabel('MAJOR_MODIFICATION')).toBe('Modification majeure');
    expect(component.typeLabel('OTHER')).toBe('Autre');
    expect(component.typeBadge('MAJOR_MODIFICATION')).toBe('tbadge tbadge-major_modification');
  }));
});
