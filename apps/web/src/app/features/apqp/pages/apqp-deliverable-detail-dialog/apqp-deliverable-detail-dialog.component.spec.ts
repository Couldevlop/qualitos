import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpService } from '../../apqp.service';
import { ApqpDeliverable, ApqpPhase } from '../../apqp.types';
import {
  ApqpDeliverableDetailDialogComponent
} from './apqp-deliverable-detail-dialog.component';

/**
 * Le popup d'un livrable, composé selon son GENRE.
 *
 * <p>Ce que ce banc tient : chaque genre demande ce qu'il exige et rien d'autre,
 * un renvoi ne se coche pas sans son enregistrement, et l'écran n'envoie jamais
 * l'heure ni l'auteur — que le serveur pose depuis le jeton.
 */
describe('ApqpDeliverableDetailDialogComponent', () => {

  let fixture: ComponentFixture<ApqpDeliverableDetailDialogComponent>;
  let component: ApqpDeliverableDetailDialogComponent;
  let service: jasmine.SpyObj<ApqpService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ApqpDeliverableDetailDialogComponent>>;
  let routeur: jasmine.SpyObj<Router>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const phase: ApqpPhase = {
    id: 'p1', position: 3, level: 3, title: 'Process Design & Development',
    purpose: null, question: null, deliverables: []
  };

  function livrable(partiel: Partial<ApqpDeliverable>): ApqpDeliverable {
    return {
      id: 'd1', position: 1, label: 'Control plan', ppap: true,
      kind: 'ATTACHMENT', done: false, evidenceCount: 0, ...partiel
    };
  }

  async function ouvrir(
    partiel: Partial<ApqpDeliverable>, editable = true
  ): Promise<void> {
    service = jasmine.createSpyObj<ApqpService>('ApqpService', [
      'completeDeliverable', 'evidences', 'uploadEvidence', 'deleteEvidence'
    ]);
    service.evidences.and.returnValue(of([]));
    service.completeDeliverable.and.returnValue(
      of({ phases: [], ppapDone: 1, ppapTotal: 12 }));
    dialogRef = jasmine.createSpyObj<MatDialogRef<ApqpDeliverableDetailDialogComponent>>(
      'MatDialogRef', ['close']);
    routeur = jasmine.createSpyObj<Router>('Router', ['navigate']);
    routeur.navigate.and.resolveTo(true);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpDeliverableDetailDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ApqpService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: Router, useValue: routeur },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { phase, deliverable: livrable(partiel), editable }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpDeliverableDetailDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // ---------- un corps par genre ----------

  it('demande des pièces pour un livrable documentaire, et rien d\'autre', async () => {
    await ouvrir({ kind: 'ATTACHMENT' });

    expect(hote().querySelector('[data-test=zone-pieces]')).not.toBeNull();
    expect(hote().querySelector('[data-test=table-mesures]')).toBeNull();
    expect(hote().querySelector('[data-test=renvoi-module]')).toBeNull();
    expect(hote().querySelector('[data-test=liste-points]')).toBeNull();
    // Les pièces sont demandées à l'ouverture : un popup qui n'afficherait rien
    // laisserait croire qu'aucune preuve n'a été versée.
    expect(service.evidences).toHaveBeenCalledWith('p1', 'd1');
  });

  it('demande l\'enregistrement visé pour un renvoi, et ne charge aucune pièce', async () => {
    await ouvrir({ kind: 'MODULE_LINK' });

    expect(hote().querySelector('[data-test=renvoi-module]')).not.toBeNull();
    expect(hote().querySelector('[data-test=zone-pieces]')).toBeNull();
    expect(service.evidences).not.toHaveBeenCalled();
  });

  it('amorce la table des mesures avec les intitulés venus du serveur', async () => {
    await ouvrir({
      kind: 'DATA_ENTRY',
      data: [
        { label: 'Cp', value: '', unit: '', measuredAt: null },
        { label: 'Cpk', value: '1.42', unit: '', measuredAt: '2026-09-12' }
      ]
    });

    expect(hote().querySelector('[data-test=table-mesures]')).not.toBeNull();
    expect(component.rows.length).toBe(2);
    expect(component.rows.at(0).getRawValue().label).toBe('Cp');
    expect(component.rows.at(1).getRawValue().value).toBe('1.42');
  });

  it('coche les sous-points d\'une checklist et les renvoie tels quels', async () => {
    await ouvrir({
      kind: 'CHECKLIST',
      data: [{ label: 'safety', checked: false }, { label: 'cost', checked: false }]
    });

    component.rows.at(0).patchValue({ checked: true });
    component.submit();

    expect(service.completeDeliverable).toHaveBeenCalled();
    const envoye = service.completeDeliverable.calls.mostRecent().args[2];
    expect(envoye.data).toEqual([
      { label: 'safety', value: '', unit: '', measuredAt: null, checked: true },
      { label: 'cost', value: '', unit: '', measuredAt: null, checked: false }
    ]);
  });

  it('ouvre la fiche visee, en refermant le popup', async () => {
    const cible = '11111111-2222-3333-4444-555555555555';
    await ouvrir({ kind: 'MODULE_LINK', linkedKind: 'FMEA', linkedId: cible });

    hote().querySelector<HTMLButtonElement>('[data-test=ouvrir-enregistrement]')!.click();

    // Sans la fermeture, le dialogue resterait par-dessus l'ecran d'arrivee et
    // l'utilisateur croirait que rien n'a bouge.
    expect(dialogRef.close).toHaveBeenCalled();
    expect(routeur.navigate).toHaveBeenCalledWith(['/fmea', cible]);
  });

  it('dit pourquoi un plan de surveillance ne s’ouvre pas d’ici', async () => {
    await ouvrir({
      kind: 'MODULE_LINK', linkedKind: 'CONTROL_PLAN',
      linkedId: '11111111-2222-3333-4444-555555555555'
    });

    // Il vit dans l'onglet d'un produit : aucune route ne l'atteint par son seul
    // identifiant, et un lien qui tomberait a cote vaudrait moins qu'une phrase.
    expect(hote().querySelector('[data-test=ouvrir-enregistrement]')).toBeNull();
    expect(hote().querySelector('[data-test=renvoi-sans-route]')).not.toBeNull();
  });

  it('n’offre pas d’ouverture tant qu’aucun enregistrement n’est designe', async () => {
    await ouvrir({ kind: 'MODULE_LINK' });

    expect(hote().querySelector('[data-test=ouvrir-enregistrement]')).toBeNull();
    expect(hote().querySelector('[data-test=renvoi-sans-route]')).toBeNull();
  });

  // ---------- ce que l'écran refuse ----------

  it('refuse de cocher un renvoi sans son enregistrement', async () => {
    await ouvrir({ kind: 'MODULE_LINK' });

    component.form.patchValue({ done: true, linkedId: '', linkedKind: null });
    component.submit();

    // Mieux vaut désactiver le bouton que proposer une action qu'on sait refusée.
    expect(component.form.invalid).toBeTrue();
    expect(component.blocage).toContain('enregistrement');
    expect(service.completeDeliverable).not.toHaveBeenCalled();
  });

  it('refuse un identifiant de renvoi mal formé', async () => {
    await ouvrir({ kind: 'MODULE_LINK' });

    component.form.patchValue({ linkedKind: 'FMEA', linkedId: 'pas-un-uuid' });

    expect(component.form.get('linkedId')?.hasError('pattern')).toBeTrue();
  });

  it('refuse une ligne sans intitulé plutôt que de l\'envoyer au serveur', async () => {
    await ouvrir({ kind: 'CHECKLIST', data: [{ label: 'safety', checked: false }] });

    component.rows.at(0).patchValue({ label: '   ' });
    component.submit();

    expect(component.form.invalid).toBeTrue();
    expect(service.completeDeliverable).not.toHaveBeenCalled();
  });

  it('se lit sans se remplir quand l\'utilisateur ne peut pas écrire', async () => {
    await ouvrir({ kind: 'ATTACHMENT' }, false);

    expect(component.form.disabled).toBeTrue();
    expect(component.blocage).toContain('consulter');
    expect(hote().querySelector('[data-test=verser-piece]')).toBeNull();
  });

  // ---------- ce que l'écran envoie ----------

  it('n\'envoie ni l\'heure ni l\'auteur : le serveur les pose', async () => {
    await ouvrir({ kind: 'ATTACHMENT' });

    component.form.patchValue({ done: true });
    component.submit();

    const envoye = service.completeDeliverable.calls.mostRecent().args[2];
    expect('doneAt' in envoye).toBeFalse();
    expect('doneBy' in envoye).toBeFalse();
    // Une pièce jointe ne porte pas de contenu : l'envoyer vaudrait un 422.
    expect(envoye.data).toBeNull();
    expect(envoye.linkedId).toBeNull();
  });

  it('rend le cycle au parent en se fermant, pour qu\'il recompte le dossier PPAP', async () => {
    await ouvrir({ kind: 'ATTACHMENT' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(
      jasmine.objectContaining({ ppapDone: 1, ppapTotal: 12 }));
  });

  // ---------- pièces jointes ----------

  it('verse le fichier choisi, puis relit la liste', async () => {
    await ouvrir({ kind: 'ATTACHMENT' });
    service.uploadEvidence.and.returnValue(of({
      id: 'e1', phaseId: 'p1', deliverableId: 'd1', contentType: 'application/pdf',
      sizeBytes: 2048, originalFilename: 'ppap.pdf', createdAt: '2026-09-12T10:00:00Z'
    }));

    const fichier = new File([new Uint8Array([1])], 'ppap.pdf', { type: 'application/pdf' });
    const input = { files: [fichier], value: 'ppap.pdf' } as unknown as HTMLInputElement;
    component.choisirFichier(input);

    expect(service.uploadEvidence).toHaveBeenCalledWith('p1', 'd1', fichier);
    // La saisie est vidée : sans cela, reverser le même fichier après un refus ne
    // déclencherait aucun événement.
    expect(input.value).toBe('');
    expect(service.evidences).toHaveBeenCalledTimes(2);
  });

  it('explique un refus du serveur sans perdre le popup', async () => {
    await ouvrir({ kind: 'ATTACHMENT' });
    service.uploadEvidence.and.returnValue(
      throwError(() => ({ status: 413, error: { detail: 'trop lourd' } })));

    const fichier = new File([new Uint8Array([1])], 'gros.pdf', { type: 'application/pdf' });
    component.choisirFichier(
      { files: [fichier], value: '' } as unknown as HTMLInputElement);

    expect(component.envoi).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('affiche la taille en unités lisibles', async () => {
    await ouvrir({ kind: 'ATTACHMENT' });

    expect(component.taille(2 * 1024 * 1024)).toBe('2.0 Mo');
    expect(component.taille(4096)).toBe('4 Ko');
    // Un fichier minuscule ne doit pas s'afficher « 0 Ko », ce qui se lirait
    // comme un fichier vide.
    expect(component.taille(120)).toBe('1 Ko');
  });
});
