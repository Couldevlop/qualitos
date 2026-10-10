import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { RiskCapaOpener } from './risk-capa-opener.service';
import { RiskRegisterService } from './risk-register.service';
import { ActionDialogResult } from './pages/action-dialog/action-dialog.component';

describe('RiskCapaOpener', () => {

  let service: jasmine.SpyObj<RiskRegisterService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let opener: RiskCapaOpener;

  const RISQUE = { id: 'r1', reference: 'R-014', owner: 'M. Kone' };
  const SAISIE: ActionDialogResult = {
    title: 'Carte SPC', description: null, dueDate: '2026-12-01', status: 'TO_START',
    kind: 'CORRECTIVE', assignee: 'A. Diallo'
  };

  function fenetre(result: ActionDialogResult | undefined): void {
    dialog.open.and.returnValue({ afterClosed: () => of(result) } as MatDialogRef<unknown>);
  }

  beforeEach(() => {
    service = jasmine.createSpyObj<RiskRegisterService>('RiskRegisterService', ['suggestions', 'openCapa']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    TestBed.configureTestingModule({
      providers: [
        { provide: RiskRegisterService, useValue: service },
        { provide: MatDialog, useValue: dialog }
      ]
    });
    opener = TestBed.inject(RiskCapaOpener);
  });

  it('propose le propriétaire et les noms connus, puis ouvre le dossier saisi', () => {
    service.suggestions.and.returnValue(of({ processes: [], sites: [], owners: ['M. Kone', 'A. Diallo'] }));
    service.openCapa.and.returnValue(of({
      id: 'c1', title: 'Carte SPC', dueDate: '2026-12-01', status: 'OPEN', kind: 'CORRECTIVE', assignee: 'A. Diallo'
    }));
    fenetre(SAISIE);

    let ouvert: unknown;
    opener.open(RISQUE).subscribe(c => ouvert = c);

    expect(dialog.open.calls.mostRecent().args[1]!.data).toEqual(jasmine.objectContaining({
      mode: 'capa', reference: 'R-014', assignee: 'M. Kone', assignees: ['M. Kone', 'A. Diallo']
    }));
    expect(service.openCapa).toHaveBeenCalledWith('r1', {
      title: 'Carte SPC', description: null, kind: 'CORRECTIVE', assignee: 'A. Diallo', dueDate: '2026-12-01'
    });
    expect(ouvert).toEqual(jasmine.objectContaining({ id: 'c1' }));
  });

  it('sans suggestions, la fenêtre s’ouvre quand même', () => {
    service.suggestions.and.returnValue(throwError(() => new Error('503')));
    fenetre(undefined);

    let rendu: unknown = 'rien';
    opener.open(RISQUE).subscribe(c => rendu = c);

    expect(dialog.open.calls.mostRecent().args[1]!.data).toEqual(jasmine.objectContaining({ assignees: [] }));
    expect(service.openCapa).not.toHaveBeenCalled();
    expect(rendu).toBeNull();
  });

  it('une saisie incomplète n’ouvre rien', () => {
    service.suggestions.and.returnValue(of({ processes: [], sites: [], owners: [] }));
    fenetre({ ...SAISIE, dueDate: null });

    let rendu: unknown = 'rien';
    opener.open(RISQUE).subscribe(c => rendu = c);

    expect(service.openCapa).not.toHaveBeenCalled();
    expect(rendu).toBeNull();
  });
});
