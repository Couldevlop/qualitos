import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpService } from '../../apqp.service';
import { ApqpCycle, ApqpDeliverable, ApqpPhase } from '../../apqp.types';
import {
  ApqpDeliverableDetailDialogComponent
} from './apqp-deliverable-detail-dialog.component';

/**
 * Le formulaire UNIQUE d'un livrable APQP.
 *
 * <p>Ce que ce banc tient : il n'y a plus qu'un corps, quel que soit le livrable
 * — les pièces sont TOUJOURS proposées, le renvoi est FACULTATIF — et la case
 * pilote l'état et l'avancement à l'écran comme au serveur. L'écran n'envoie
 * jamais l'heure ni l'auteur : le serveur les pose depuis le jeton.
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

  const cycle: ApqpCycle = {
    projectId: 'pr1', projectName: 'Support moteur', projectType: 'NPI',
    customer: 'Renault', phases: [], ppapDone: 1, ppapTotal: 12
  };

  function livrable(partiel: Partial<ApqpDeliverable>): ApqpDeliverable {
    return {
      id: 'd1', position: 1, label: 'Control plan', expectedArtifact: null,
      ppap: true, owner: null, dueDate: null, status: 'NOT_STARTED',
      percentComplete: 0, done: false, evidenceCount: 0, ...partiel
    };
  }

  async function ouvrir(
    partiel: Partial<ApqpDeliverable>, editable = true
  ): Promise<void> {
    service = jasmine.createSpyObj<ApqpService>('ApqpService', [
      'completeDeliverable', 'evidences', 'uploadEvidence', 'deleteEvidence'
    ]);
    service.evidences.and.returnValue(of([]));
    service.completeDeliverable.and.returnValue(of(cycle));
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
          useValue: {
            projectId: 'pr1', phase, deliverable: livrable(partiel), editable
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpDeliverableDetailDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // ---------- un seul corps, pour tous ----------

  it('rend le corps entier pour un livrable quelconque', async () => {
    await ouvrir({});

    // Il y avait quatre corps choisis par un « genre » ; il n'en reste qu'un.
    expect(hote().querySelector('[data-test=zone-pieces]')).not.toBeNull();
    // Le renvoi vers un module a ete retire du formulaire.
    expect(hote().querySelector('[data-test=renvoi-module]')).toBeNull();
    expect(hote().querySelector('[data-test=responsable]')).not.toBeNull();
    expect(hote().querySelector('[data-test=echeance]')).not.toBeNull();
    expect(hote().querySelector('[data-test=statut]')).not.toBeNull();
    expect(hote().querySelector('[data-test=avancement]')).not.toBeNull();
  });

  it('charge TOUJOURS les pièces, quel que soit le livrable', async () => {
    await ouvrir({});

    // L'ancien popup ne les chargeait que pour un genre, et les autres
    // semblaient n'en porter aucune — ce que l'auditeur demande en premier.
    expect(service.evidences).toHaveBeenCalledWith('pr1', 'p1', 'd1');
    expect(hote().querySelector('[data-test=zone-pieces]')).not.toBeNull();
  });

  // ---------- la case pilote ----------

  it('cocher fixe l\'état à « acquis » et l\'avancement à 100', async () => {
    await ouvrir({ status: 'IN_PROGRESS', percentComplete: 40 });

    component.form.patchValue({ done: true });

    expect(component.form.getRawValue().status).toBe('DONE');
    expect(component.form.getRawValue().percentComplete).toBe(100);
  });

  it('décocher ramène l\'avancement en arrière et quitte l\'état « acquis »', async () => {
    await ouvrir({ done: true, status: 'DONE', percentComplete: 100 });

    component.form.patchValue({ done: false });

    // Laisser « acquis » à 100 % sur un livrable décoché ferait dire deux
    // choses contraires au même écran.
    expect(component.form.getRawValue().status).toBe('IN_PROGRESS');
    expect(component.form.getRawValue().percentComplete).toBe(0);
  });

  it('décocher laisse un avancement partiel tel quel', async () => {
    await ouvrir({ done: false, status: 'BLOCKED', percentComplete: 60 });

    component.form.patchValue({ done: false });

    expect(component.form.getRawValue().status).toBe('BLOCKED');
    expect(component.form.getRawValue().percentComplete).toBe(60);
  });

  // ---------- ce que l'écran envoie ----------

  it('envoie le suivi complet, sans l\'heure ni l\'auteur', async () => {
    await ouvrir({});

    component.form.patchValue({
      done: true,
      expectedArtifact: '  Plan signé  ',
      ppap: true,
      owner: '  R. Martin  ',
      dueDate: '2026-10-15',
      comment: '  reçu le 3  '
    });
    component.submit();

    expect(service.completeDeliverable).toHaveBeenCalled();
    const appel = service.completeDeliverable.calls.mostRecent().args;
    expect(appel[0]).toBe('pr1');
    expect(appel[1]).toBe('p1');
    expect(appel[2]).toBe('d1');
    const envoye = appel[3];
    expect(envoye.done).toBeTrue();
    expect(envoye.expectedArtifact).toBe('Plan signé');
    expect(envoye.ppap).toBeTrue();
    expect(envoye.owner).toBe('R. Martin');
    expect(envoye.dueDate).toBe('2026-10-15');
    expect(envoye.comment).toBe('reçu le 3');
    // La case pilote : l'ecran envoie ce qu'il affiche, et le serveur applique
    // la meme regle -- les deux disent alors la meme chose.
    expect(envoye.status).toBe('DONE');
    expect(envoye.percentComplete).toBe(100);
    expect(envoye.linkedKind).toBeNull();
    expect(envoye.linkedId).toBeNull();
    expect('doneAt' in envoye).toBeFalse();
    expect('doneBy' in envoye).toBeFalse();
  });

  it('rend le cycle au parent en se fermant, pour qu\'il recompte le dossier PPAP', async () => {
    await ouvrir({});

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(
      jasmine.objectContaining({ ppapDone: 1, ppapTotal: 12 }));
  });

  it('refuse un avancement hors de 0-100', async () => {
    await ouvrir({});

    component.form.patchValue({ percentComplete: 140 });

    expect(component.form.invalid).toBeTrue();
    component.submit();
    expect(service.completeDeliverable).not.toHaveBeenCalled();
  });

  it('se lit sans se remplir quand l\'utilisateur ne peut pas écrire', async () => {
    await ouvrir({}, false);

    expect(component.form.disabled).toBeTrue();
    expect(component.blocage).toContain('consulter');
    expect(hote().querySelector('[data-test=verser-piece]')).toBeNull();
  });

  // ---------- pièces jointes ----------

  it('verse le fichier choisi, puis relit la liste', async () => {
    await ouvrir({});
    service.uploadEvidence.and.returnValue(of({
      id: 'e1', phaseId: 'p1', deliverableId: 'd1', contentType: 'application/pdf',
      sizeBytes: 2048, originalFilename: 'ppap.pdf', createdAt: '2026-09-12T10:00:00Z'
    }));

    const fichier = new File([new Uint8Array([1])], 'ppap.pdf', { type: 'application/pdf' });
    const input = { files: [fichier], value: 'ppap.pdf' } as unknown as HTMLInputElement;
    component.choisirFichier(input);

    expect(service.uploadEvidence).toHaveBeenCalledWith('pr1', 'p1', 'd1', fichier);
    // La saisie est vidée : sans cela, reverser le même fichier après un refus ne
    // déclencherait aucun événement.
    expect(input.value).toBe('');
    expect(service.evidences).toHaveBeenCalledTimes(2);
  });

  it('explique un refus du serveur sans perdre le popup', async () => {
    await ouvrir({});
    service.uploadEvidence.and.returnValue(
      throwError(() => ({ status: 413, error: { detail: 'trop lourd' } })));

    const fichier = new File([new Uint8Array([1])], 'gros.pdf', { type: 'application/pdf' });
    component.choisirFichier(
      { files: [fichier], value: '' } as unknown as HTMLInputElement);

    expect(component.envoi).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('affiche la taille en unités lisibles', async () => {
    await ouvrir({});

    expect(component.taille(2 * 1024 * 1024)).toBe('2.0 Mo');
    expect(component.taille(4096)).toBe('4 Ko');
    // Un fichier minuscule ne doit pas s'afficher « 0 Ko », ce qui se lirait
    // comme un fichier vide.
    expect(component.taille(120)).toBe('1 Ko');
  });

  it('nomme les quatre états en clair, jamais en constante serveur', async () => {
    await ouvrir({});

    expect(component.statutLabel('NOT_STARTED')).toBe('Non commencé');
    expect(component.statutLabel('BLOCKED')).toBe('Bloqué');
    expect(component.statuts.length).toBe(4);
  });
});
