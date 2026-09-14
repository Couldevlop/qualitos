import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../nc.service';
import { EightDDiscipline, EightDReport } from '../../nc.types';
import { NcEightDComponent } from './nc-eightd.component';

/**
 * L'écran du rapport 8D.
 *
 * <p>Ce qui est vérifié en priorité : une discipline sans source est AFFICHÉE comme
 * telle, le « partiel » se voit, et les gestes d'écriture n'apparaissent que pour qui
 * y a droit — miroir du partage des rôles posé côté serveur.
 */
describe('NcEightDComponent', () => {

  let svc: jasmine.SpyObj<NcService>;
  let router: jasmine.SpyObj<Router>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let fixture: ComponentFixture<NcEightDComponent>;
  let component: NcEightDComponent;

  const NC_ID = 'nc-1';

  function discipline(code: string, servi: boolean, saisie: boolean): EightDDiscipline {
    return {
      code,
      title: 'Discipline ' + code,
      sourced: servi,
      sourceLabel: servi ? 'Agrégé depuis le dossier' : "Aucune source — cette discipline se saisit",
      lines: servi ? ['une ligne de contenu'] : [],
      editable: saisie
    };
  }

  function rapport(partiel: Partial<EightDReport> = {}): EightDReport {
    return {
      ncId: NC_ID,
      ncReference: 'NC-2026-0007',
      ncTitle: 'Fuite au presse-étoupe',
      status: 'DRAFT',
      issuable: true,
      partial: true,
      missingCodes: ['D1', 'D3', 'D8'],
      team: null,
      containment: null,
      recognition: null,
      disciplines: [
        discipline('D1', false, true),
        discipline('D2', true, false),
        discipline('D3', false, true),
        discipline('D4', true, false),
        discipline('D5', true, false),
        discipline('D6', true, false),
        discipline('D7', true, false),
        discipline('D8', false, true)
      ],
      seal: null,
      ...partiel
    };
  }

  async function monter(roles: string[]): Promise<void> {
    svc = jasmine.createSpyObj<NcService>('NcService',
      ['getEightDReport', 'saveEightDReport', 'issueEightDReport', 'downloadEightDPdf']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    svc.getEightDReport.and.returnValue(of(rapport()));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [NcEightDComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: Router, useValue: router },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { hasAnyRole: () => roles.length > 0 } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: NC_ID }) } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NcEightDComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  // ---------- lecture ----------

  it('affiche les huit disciplines du serveur, sans en recomposer aucune', async () => {
    await monter(['QUALITY_MANAGER']);

    expect(hote().querySelectorAll('[data-test="discipline"]').length).toBe(8);
    expect(hote().textContent).toContain('NC-2026-0007');
  });

  it('dit qu\'une discipline n\'a pas de source plutôt que de laisser un bloc vide', async () => {
    await monter(['QUALITY_MANAGER']);

    const vides = hote().querySelectorAll('[data-test="discipline-vide"]');
    expect(vides.length).toBe(3);
    expect(hote().textContent).toContain('cette discipline se saisit');
  });

  it('signale un rapport partiel et nomme les disciplines manquantes', async () => {
    await monter(['QUALITY_MANAGER']);

    expect(hote().querySelector('[data-test="badge-partiel"]')).toBeTruthy();
    expect(hote().textContent).toContain('D1, D3, D8');
  });

  it('reprend les trois saisies déjà enregistrées', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.getEightDReport.and.returnValue(of(rapport({ team: 'Ada\nGrace', containment: 'Tri' })));
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();

    expect(component.form.getRawValue().team).toBe('Ada\nGrace');
    expect(component.form.getRawValue().containment).toBe('Tri');
  });

  // ---------- rôles ----------

  it('cache les gestes d\'écriture à qui n\'a pas le rôle', async () => {
    await monter([]);

    expect(hote().querySelector('[data-test="enregistrer"]')).toBeNull();
    expect(hote().querySelector('[data-test="emettre"]')).toBeNull();
    expect(component.form.disabled).toBeTrue();
  });

  it('propose l\'émission au manager qualité quand la NC est clôturée', async () => {
    await monter(['QUALITY_MANAGER']);

    expect(hote().querySelector('[data-test="emettre"]')).toBeTruthy();
  });

  it('ne propose pas l\'émission quand la NC n\'est pas clôturée', async () => {
    svc = jasmine.createSpyObj<NcService>('NcService',
      ['getEightDReport', 'saveEightDReport', 'issueEightDReport', 'downloadEightDPdf']);
    await monter(['QUALITY_MANAGER']);
    svc.getEightDReport.and.returnValue(of(rapport({ issuable: false })));
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="emettre"]')).toBeNull();
  });

  // ---------- écriture ----------

  it('enregistre les trois disciplines saisies', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.saveEightDReport.and.returnValue(of(rapport({ team: 'Ada' })));
    component.form.setValue({ team: 'Ada', containment: 'Tri à 100 %', recognition: 'Merci' });

    component.enregistrer();

    expect(svc.saveEightDReport).toHaveBeenCalledWith(NC_ID, {
      team: 'Ada', containment: 'Tri à 100 %', recognition: 'Merci'
    });
  });

  it('n\'émet rien si la confirmation est refusée', async () => {
    await monter(['QUALITY_MANAGER']);
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as MatDialogRef<unknown>);

    component.emettre(rapport());

    expect(svc.issueEightDReport).not.toHaveBeenCalled();
  });

  it('émet après confirmation, et prévient que le contenu sera figé', async () => {
    await monter(['DIRECTOR_QUALITY']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    svc.issueEightDReport.and.returnValue(of(rapport({ status: 'ISSUED', issuable: false })));

    component.emettre(rapport());

    expect(svc.issueEightDReport).toHaveBeenCalledWith(NC_ID);
    const donnees = dialog.open.calls.mostRecent().args[1]?.data as { message: string };
    expect(donnees.message).toContain('partiel');
  });

  it('affiche le sceau et propose le PDF quand le rapport est émis', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.getEightDReport.and.returnValue(of(rapport({
      status: 'ISSUED',
      issuable: false,
      seal: {
        sha256Hex: 'f'.repeat(64), anchorTxRef: 'tx-1', verificationCode: 'CODE1234567890123456',
        issuedAt: '2026-09-13T10:00:00Z', issuedByName: 'Ada Lovelace'
      }
    })));
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="sceau"]')).toBeTruthy();
    expect(hote().querySelector('[data-test="exporter"]')).toBeTruthy();
    // Scellé : le formulaire de saisie disparaît.
    expect(hote().querySelector('[data-test="enregistrer"]')).toBeNull();
  });

  // ---------- téléchargement ----------

  it('enregistre le PDF sous le nom que le serveur propose', async () => {
    await monter(['QUALITY_MANAGER']);
    const lien = document.createElement('a');
    spyOn(lien, 'click');
    spyOn(document, 'createElement').and.returnValue(lien);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    const revoke = spyOn(URL, 'revokeObjectURL');
    svc.downloadEightDPdf.and.returnValue(of(new HttpResponse({
      body: new Blob(['%PDF']),
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="8d-nc-2026-0007.pdf"' }),
      status: 200
    })));

    component.telecharger();

    expect(lien.download).toBe('8d-nc-2026-0007.pdf');
    expect(lien.click).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalledWith('blob:x');
  });

  it('prévient quand le téléchargement échoue, sans casser l\'écran', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.downloadEightDPdf.and.returnValue(throwError(() => ({ status: 409 })));

    component.telecharger();

    expect(snack.open).toHaveBeenCalled();
  });

  it('revient à la fiche de la non-conformité', async () => {
    await monter(['QUALITY_MANAGER']);

    component.retour();

    expect(router.navigate).toHaveBeenCalledWith(['/nc', NC_ID]);
  });

  it('prévient quand l\'enregistrement échoue, et la saisie reste à l\'écran', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.saveEightDReport.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.setValue({ team: 'Ada', containment: '', recognition: '' });

    component.enregistrer();

    expect(snack.open).toHaveBeenCalled();
    expect(component.form.getRawValue().team).toBe('Ada');
  });

  it('n\'enregistre rien quand une saisie dépasse la taille de la colonne', async () => {
    await monter(['QUALITY_MANAGER']);
    component.form.setValue({ team: 'x'.repeat(4001), containment: '', recognition: '' });

    component.enregistrer();

    // Inutile d'aller chercher un 422 pour l'apprendre : la limite est celle de la colonne.
    expect(svc.saveEightDReport).not.toHaveBeenCalled();
  });

  it('prévient quand l\'émission échoue', async () => {
    await monter(['QUALITY_MANAGER']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    svc.issueEightDReport.and.returnValue(throwError(() => ({ status: 409 })));

    component.emettre(rapport());

    expect(snack.open).toHaveBeenCalled();
  });

  it('retombe sur un nom par défaut quand l\'en-tête n\'est pas exposé', async () => {
    await monter(['QUALITY_MANAGER']);
    const lien = document.createElement('a');
    spyOn(lien, 'click');
    spyOn(document, 'createElement').and.returnValue(lien);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:y');
    spyOn(URL, 'revokeObjectURL');
    svc.downloadEightDPdf.and.returnValue(of(new HttpResponse({
      body: new Blob(['%PDF']), headers: new HttpHeaders(), status: 200
    })));

    component.telecharger();

    // `Content-Disposition` n'est lisible du navigateur que s'il est EXPOSÉ par CORS.
    expect(lien.download).toBe('rapport-8d.pdf');
  });

  it('prévient quand la réponse ne porte aucun document', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.downloadEightDPdf.and.returnValue(of(new HttpResponse<Blob>({ body: null, status: 200 })));

    component.telecharger();

    expect(snack.open).toHaveBeenCalled();
  });

  // ---------- garde d'identifiant ----------

  it('refuse un identifiant malformé avant tout appel', async () => {
    svc = jasmine.createSpyObj<NcService>('NcService',
      ['getEightDReport', 'saveEightDReport', 'issueEightDReport', 'downloadEightDPdf']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [NcEightDComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: Router, useValue: router },
        { provide: MatDialog, useValue: jasmine.createSpyObj<MatDialog>('MatDialog', ['open']) },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'pas-un-identifiant!' }) } }
        }
      ]
    }).compileComponents();

    const f = TestBed.createComponent(NcEightDComponent);
    f.detectChanges();

    expect(svc.getEightDReport).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/nc']);
  });
});
