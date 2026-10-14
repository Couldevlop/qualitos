import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, ParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { MocService } from '../../moc.service';
import { demande, ligne } from '../../testing/moc.fixtures';
import { MocComponent } from './moc.component';

/** L'écran MOC : liste filtrée, sélection par l'adresse, création, extraction, accès aux paramètres. */
describe('MocComponent', () => {
  let fixture: ComponentFixture<MocComponent>;
  let component: MocComponent;
  let service: jasmine.SpyObj<MocService>;
  let router: Router;
  let params: BehaviorSubject<ParamMap>;
  let dialog: jasmine.SpyObj<MatDialog>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const LIGNES = [
    ligne({ id: 'c1', code: 'MOC-2026-014', status: 'IN_VALIDATION', awaitingMe: true }),
    ligne({ id: 'c2', code: 'MOC-2026-013', title: 'Nouveau solvant', status: 'IN_ANALYSIS', classification: 'MAJOR' }),
    ligne({ id: 'c3', code: 'MOC-2026-012', title: 'Gamme', status: 'DRAFT', classification: 'NOT_ASSESSED', score: null })
  ];

  async function setup(id: string | null = null, droits: string[] = ['change.request']): Promise<void> {
    service = jasmine.createSpyObj<MocService>('MocService', ['list', 'get', 'exportXlsx']);
    service.list.and.returnValue(of(LIGNES));
    service.get.and.callFake(i => of(demande({ id: i })));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    params = new BehaviorSubject(convertToParamMap(id ? { id } : {}));
    await TestBed.configureTestingModule({
      declarations: [MocComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: MocService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: ActivatedRoute, useValue: { paramMap: params } },
        { provide: AuthzService, useValue: { can: (c: string) => of(droits.includes(c)) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(MocComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('sans sélection, ouvre la demande la plus récente sans empiler l’historique', async () => {
    await setup();
    expect(router.navigate).toHaveBeenCalledWith(['/changes', 'c1'], { replaceUrl: true });
    expect(hote().querySelectorAll('.carte').length).toBe(3);
  });

  it('avec une demande dans l’adresse, la charge et la surligne', async () => {
    await setup('c2');
    fixture.detectChanges();
    expect(service.get).toHaveBeenCalledWith('c2');
    expect(component.detail?.id).toBe('c2');
    expect(router.navigate).not.toHaveBeenCalled();
    const active = hote().querySelector('.carte--active');
    expect(active?.getAttribute('aria-current')).toBe('true');
    expect(active?.textContent).toContain('MOC-2026-013');
    expect(hote().querySelector('qos-moc-detail')).not.toBeNull();
  });

  it('filtre par statut et offre « À valider par moi »', async () => {
    await setup('c1');
    expect(component.count('ALL')).toBe(3);
    expect(component.count('DRAFT')).toBe(1);
    expect(component.count('MINE')).toBe(1);
    (hote().querySelector('[data-test="filtre-IN_ANALYSIS"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.visible.map(v => v.id)).toEqual(['c2']);
    expect(hote().querySelector('[data-test="filtre-IN_ANALYSIS"]')?.getAttribute('aria-pressed')).toBe('true');
    (hote().querySelector('[data-test="filtre-MINE"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.visible.map(v => v.id)).toEqual(['c1']);
    component.setFilter('REJECTED');
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="vide"]')?.textContent).toContain('Aucune demande dans ce statut');
  });

  it('sélectionner une carte change l’adresse', async () => {
    await setup('c1');
    (hote().querySelector('[data-test="carte-MOC-2026-012"]') as HTMLButtonElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/changes', 'c3'], { replaceUrl: false });
    (router.navigate as jasmine.Spy).calls.reset();
    component.select(LIGNES[0]);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('le bouton « Nouvelle demande » suit le droit, et la demande créée s’ouvre', async () => {
    await setup('c1', []);
    expect(hote().querySelector('[data-test="nouvelle"]')).toBeNull();
    expect(hote().querySelector('[data-test="parametres"]')).toBeNull();
    TestBed.resetTestingModule();
    await setup('c1', ['change.request', 'authz.manage']);
    expect(hote().querySelector('[data-test="parametres"]')).not.toBeNull();
    const creee = demande({ id: 'c9', code: 'MOC-2026-015', status: 'DRAFT' });
    dialog.open.and.returnValue({ afterClosed: () => of(creee) } as never);
    (hote().querySelector('[data-test="nouvelle"]') as HTMLButtonElement).click();
    expect(component.items[0].id).toBe('c9');
    expect(router.navigate).toHaveBeenCalledWith(['/changes', 'c9']);
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.create();
    expect(component.items.length).toBe(4);
  });

  it('suit les changements du panneau et la suppression', async () => {
    await setup('c3');
    component.onChanged(demande({ id: 'c3', title: 'Gamme v2', status: 'IN_ANALYSIS', classification: 'MINOR', score: 10 }));
    expect(component.items.find(i => i.id === 'c3')).toEqual(jasmine.objectContaining({
      title: 'Gamme v2', status: 'IN_ANALYSIS', classification: 'MINOR', score: 10, awaitingMe: false
    }));
    component.onDeleted('c3');
    expect(component.items.map(i => i.id)).toEqual(['c1', 'c2']);
    expect(router.navigate).toHaveBeenCalledWith(['/changes', 'c1'], { replaceUrl: true });
    component.items = [];
    component.onDeleted('x');
    expect(router.navigate).toHaveBeenCalledWith(['/changes'], { replaceUrl: true });
  });

  it('extrait le classeur sous le nom donné par le serveur', async () => {
    await setup('c1');
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    spyOn(URL, 'revokeObjectURL');
    const clic = spyOn(HTMLAnchorElement.prototype, 'click');
    service.exportXlsx.and.returnValue(of(new HttpResponse({
      body: new Blob(['x']),
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="demandes-de-changement-2026-10-14.xlsx"' })
    })));
    component.exportXlsx();
    expect(clic).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:x');
    expect(component.exporting).toBeFalse();

    service.exportXlsx.and.returnValue(throwError(() => new Error('x')));
    component.exportXlsx();
    expect(component.exporting).toBeFalse();
  });

  it('sans corps ni nom de fichier : repli, et une demande introuvable vide le panneau', async () => {
    await setup('c1');
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    spyOn(URL, 'revokeObjectURL');
    const clic = spyOn(HTMLAnchorElement.prototype, 'click');
    service.exportXlsx.and.returnValue(of(new HttpResponse<Blob>({ body: null })));
    component.exportXlsx();
    expect(clic).not.toHaveBeenCalled();
    service.exportXlsx.and.returnValue(of(new HttpResponse({ body: new Blob(['x']) })));
    component.exportXlsx();
    expect(clic).toHaveBeenCalled();
    expect(component.trackById(0, LIGNES[1])).toBe('c2');

    service.get.and.returnValue(throwError(() => ({ status: 404 })));
    params.next(convertToParamMap({ id: 'zz' }));
    expect(component.detail).toBeNull();
    expect(component.detailLoading).toBeFalse();
    params.next(convertToParamMap({}));
    expect(component.selectedId).toBeNull();
  });

  it('dit quand la liste ne se charge pas', async () => {
    await setup('c1');
    service.list.and.returnValue(throwError(() => ({ status: 500 })));
    component.reload();
    fixture.detectChanges();
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('.echec')).not.toBeNull();
  });
});
