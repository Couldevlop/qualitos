import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom, of } from 'rxjs';

import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { OpenCapaRequest } from '../capa/capa.types';
import { RiskCapaOpener, RiskForCapa } from './risk-capa-opener.service';
import { RiskRegisterService } from './risk-register.service';
import { CapaLink } from './risk-register.types';

describe('RiskCapaOpener — LA fenêtre CAPA depuis un risque (ADR 0088)', () => {

  const RISQUE: RiskForCapa = {
    id: 'r1', reference: 'R-014', title: 'Dérive soudure', owner: 'A. Diallo', grossLevel: 'HIGH'
  };
  const LIEN: CapaLink = {
    id: 'c9', title: 'Carte SPC', dueDate: '2099-01-01', status: 'OPEN', kind: 'CORRECTIVE',
    assignee: 'a.diallo@client.test'
  };
  const DEMANDE: OpenCapaRequest = {
    title: 'Carte SPC', description: 'Sur le cordon', type: 'CORRECTIVE', criticity: 'HIGH',
    ownerId: 'kc-diallo', ownerName: 'a.diallo@client.test', dueDate: '2099-01-01'
  };

  let dialog: jasmine.SpyObj<MatDialog>;
  let service: jasmine.SpyObj<RiskRegisterService>;
  let opener: RiskCapaOpener;
  let ouvert: CapaOpened | undefined;

  beforeEach(() => {
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    service = jasmine.createSpyObj<RiskRegisterService>('RiskRegisterService', ['openCapa']);
    service.openCapa.and.returnValue(of(LIEN));
    ouvert = undefined;
    // La fenêtre appelle `open` à la validation : on rejoue ce geste.
    dialog.open.and.callFake(((_c: unknown, config: { data: CapaCreateDialogData }) => ({
      afterClosed: () => ouvert === undefined
        ? config.data.open!(DEMANDE)
        : of(ouvert)
    })) as never);
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialog, useValue: dialog },
        { provide: RiskRegisterService, useValue: service }
      ]
    });
    opener = TestBed.inject(RiskCapaOpener);
  });

  it('ouvre la fenêtre commune, source verrouillée sur le risque, criticité de la cotation', async () => {
    await firstValueFrom(opener.open(RISQUE));

    expect(dialog.open).toHaveBeenCalledWith(CapaCreateDialogComponent, jasmine.anything());
    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.source).toEqual({ type: 'RISK', reference: 'R-014' });
    expect(data.lockCriticity).toBeTrue();
    expect(data.prefill).toEqual(jasmine.objectContaining({
      title: 'Dérive soudure', criticity: 'HIGH', ownerHint: 'A. Diallo', type: 'PREVENTIVE'
    }));
  });

  it('propose directement le compte du propriétaire choisi dans l\'annuaire (ADR 0092)', async () => {
    await firstValueFrom(opener.open({ ...RISQUE, ownerUserId: 'kc-diallo' }));

    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.prefill?.ownerAccountId).toBe('kc-diallo');
    expect(data.prefill?.ownerHint).toBe('A. Diallo');
  });

  it('sans compte de propriétaire, ne propose que le nom à rapprocher', async () => {
    await firstValueFrom(opener.open(RISQUE));

    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.prefill?.ownerAccountId).toBeUndefined();
  });

  it('ouvre le dossier par le registre, avec le responsable choisi comme pilote, et rend le lien', async () => {
    const lien = await firstValueFrom(opener.open(RISQUE));

    expect(service.openCapa).toHaveBeenCalledWith('r1', {
      title: 'Carte SPC', description: 'Sur le cordon', kind: 'CORRECTIVE',
      ownerId: 'kc-diallo', ownerName: 'a.diallo@client.test', dueDate: '2099-01-01'
    });
    expect(lien).toEqual(LIEN);
  });

  it('envoie des valeurs nulles plutôt qu\'absentes quand la description et le nom manquent', async () => {
    const data = await new Promise<CapaCreateDialogData>(resolve => {
      dialog.open.and.callFake(((_c: unknown, config: { data: CapaCreateDialogData }) => {
        resolve(config.data);
        return { afterClosed: () => of(undefined) };
      }) as never);
      opener.open(RISQUE).subscribe();
    });
    await firstValueFrom(data.open!({ ...DEMANDE, description: undefined, ownerName: undefined }));
    expect(service.openCapa).toHaveBeenCalledWith('r1', jasmine.objectContaining({ description: null, ownerName: null }));
  });

  it('rend null quand l\'utilisateur renonce', async () => {
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    expect(await firstValueFrom(opener.open(RISQUE))).toBeNull();
    expect(service.openCapa).not.toHaveBeenCalled();
  });

  it('rend null si la fenêtre se ferme sans lien', async () => {
    ouvert = { capaId: 'x' };
    expect(await firstValueFrom(opener.open(RISQUE))).toBeNull();
  });
});
