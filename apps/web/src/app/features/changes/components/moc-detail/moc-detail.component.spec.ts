import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { MocService } from '../../moc.service';
import { MocDetail } from '../../moc.types';
import { demande, impacts } from '../../testing/moc.fixtures';
import { MocDetailComponent } from './moc-detail.component';

/** Le panneau : frise, actions selon le statut et selon qui conduit la demande, suppression confirmée. */
describe('MocDetailComponent', () => {
  let fixture: ComponentFixture<MocDetailComponent>;
  let component: MocDetailComponent;
  let service: jasmine.SpyObj<MocService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let emis: MocDetail[];
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const bouton = (t: string) => hote().querySelector(`[data-test="${t}"]`) as HTMLButtonElement | null;

  async function setup(d: MocDetail, droits: string[] = []): Promise<void> {
    service = jasmine.createSpyObj<MocService>('MocService', ['sendToAnalysis', 'submit', 'reopen', 'delete']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [MocDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: MocService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthzService, useValue: { can: (c: string) => of(droits.includes(c)) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(MocDetailComponent);
    component = fixture.componentInstance;
    component.detail = d;
    emis = [];
    component.changed.subscribe(x => emis.push(x));
    fixture.detectChanges();
  }

  it('affiche le code, le titre et la frise avec l’étape courante', async () => {
    await setup(demande({ status: 'IN_VALIDATION' }));
    expect(bouton('code')?.textContent).toContain('MOC-2026-014');
    expect(hote().querySelector('h2')?.textContent).toContain('Remplacement du four 3');
    const courante = hote().querySelector('[aria-current="step"]');
    expect(courante?.textContent).toContain('En validation');
    expect(component.progress).toBe(67);
    expect(hote().textContent).toContain('Atelier 2');
  });

  it('un rejet occupe la dernière étape, en rouge', async () => {
    await setup(demande({ status: 'REJECTED' }));
    const courante = hote().querySelector('[aria-current="step"]');
    expect(courante?.textContent).toContain('Rejeté');
    expect(courante?.classList).toContain('frise__etape--rejet');
    expect(component.progress).toBe(100);
    service.reopen.and.returnValue(of(demande({ status: 'IN_ANALYSIS' })));
    bouton('rouvrir')!.click();
    expect(service.reopen).toHaveBeenCalledWith('c1');
  });

  it('un brouillon se modifie, s’envoie en analyse ou se supprime après confirmation', async () => {
    await setup(demande({ status: 'DRAFT', classification: 'NOT_ASSESSED', score: null, impacts: impacts([]) }));
    expect(bouton('modifier')).not.toBeNull();
    expect(bouton('soumettre')).toBeNull();
    service.sendToAnalysis.and.returnValue(of(demande({ status: 'IN_ANALYSIS' })));
    bouton('analyse')!.click();
    expect(emis[0].status).toBe('IN_ANALYSIS');
    expect(component.tab).toBe(1);

    let supprime = '';
    component.deleted.subscribe(id => supprime = id);
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    bouton('supprimer')!.click();
    expect(service.delete).not.toHaveBeenCalled();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    service.delete.and.returnValue(of(undefined));
    bouton('supprimer')!.click();
    expect(service.delete).toHaveBeenCalledWith('c1');
    expect(supprime).toBe('c1');
  });

  it('la soumission reste bloquée tant que les huit domaines ne sont pas évalués', async () => {
    await setup(demande({ classification: 'NOT_ASSESSED', score: null,
      impacts: impacts(['LOW', 'LOW', null, null, null, null, null, null]) }));
    expect(bouton('soumettre')!.disabled).toBeTrue();
    expect(bouton('bloque')?.textContent).toContain('6');
    expect(bouton('supprimer')).toBeNull();
  });

  it('une analyse complète se soumet ; un refus du serveur est expliqué', async () => {
    await setup(demande());
    expect(bouton('bloque')).toBeNull();
    service.submit.and.returnValue(of(demande({ status: 'IN_VALIDATION' })));
    bouton('soumettre')!.click();
    expect(emis[0].status).toBe('IN_VALIDATION');
    expect(component.tab).toBe(2);

    service.submit.and.returnValue(throwError(() => ({ status: 409, error: { code: 'moc.role-without-holder', role: 'HSE' } })));
    component.submit();
    expect(snack.open.calls.mostRecent().args[0]).toContain('« HSE »');
    expect(component.busy).toBeFalse();
  });

  it('qui ne conduit pas la demande ne voit aucune action', async () => {
    await setup(demande({ status: 'DRAFT', canManage: false }));
    expect(bouton('modifier')).toBeNull();
    expect(bouton('supprimer')).toBeNull();
    expect(bouton('analyse')).toBeNull();
    expect(component.editable).toBeFalse();
  });

  it('un échec de suppression est expliqué et rien n’est retiré', async () => {
    await setup(demande({ status: 'DRAFT' }));
    let supprime = '';
    component.deleted.subscribe(id => supprime = id);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    service.delete.and.returnValue(throwError(() => ({ status: 409, error: { code: 'moc.delete-not-draft' } })));
    component.remove();
    expect(supprime).toBe('');
    expect(snack.open.calls.mostRecent().args[0]).toContain('brouillon');
    expect(component.busy).toBeFalse();
    expect(component.trackHistory(0, { occurredAt: 't', event: 'CREATED', actorName: null, detail: null })).toBe('tCREATED0');
  });

  it('le lien « Créer un risque » suit le droit du registre', async () => {
    await setup(demande(), ['risk.manage']);
    expect(bouton('creer-risque')).not.toBeNull();
  });

  it('l’historique nomme l’acteur quand il est connu', async () => {
    await setup(demande());
    expect(component.historyText({ occurredAt: 'x', event: 'CREATED', actorName: 'S. Benali', detail: null }))
      .toBe('Demande créée par S. Benali');
    expect(component.historyText({ occurredAt: 'x', event: 'REMINDER_SENT', actorName: null, detail: 'HSE' }))
      .toBe('Rappel envoyé aux validateurs');
  });

  it('affiche la classification du détail d'historique dans la langue de l'écran', () => {
    expect(component.historyDetail({ occurredAt: 'x', event: 'SUBMITTED', actorName: null, detail: 'CRITICAL · 50/100' }))
      .toBe(component.classificationLabel('CRITICAL') + ' · 50/100');
    expect(component.historyDetail({ occurredAt: 'x', event: 'VALIDATION_APPROVED', actorName: null, detail: 'HSE — conforme' }))
      .toBe('HSE — conforme');
    expect(component.historyDetail({ occurredAt: 'x', event: 'CREATED', actorName: null, detail: null })).toBeNull();
  });

  it('modifier ouvre la saisie pré-remplie et relaie la demande modifiée', async () => {
    await setup(demande());
    const modifiee = demande({ title: 'Four 3 bis' });
    dialog.open.and.returnValue({ afterClosed: () => of(modifiee) } as never);
    bouton('modifier')!.click();
    expect(dialog.open.calls.mostRecent().args[1]?.data).toEqual({ detail: component.detail });
    expect(emis[0].title).toBe('Four 3 bis');
  });
});
