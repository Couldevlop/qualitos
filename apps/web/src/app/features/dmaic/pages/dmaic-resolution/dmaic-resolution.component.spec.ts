import { DmaicPhase } from '../../dmaic.types';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { CapaCreateDialogData } from '../../../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { IshikawaDiagramResponse, IshikawaPage } from '../../../ishikawa/ishikawa.types';
import { DmaicResolutionService } from '../../resolution/dmaic-resolution.service';
import {
  ResolutionContent,
  ResolutionSheet,
  SheetPhoto,
  emptyContent
} from '../../resolution/dmaic-resolution.types';
import { DmaicResolutionComponent, clean, missingOf } from './dmaic-resolution.component';

const PROJET = '11111111-1111-4111-8111-111111111111';

function personne(n: number): DirectoryUser {
  return { id: 'u' + n, accountId: `00000000-0000-4000-8000-00000000000${n}`, displayName: 'Personne ' + n,
    email: `p${n}@acme.test`, active: true };
}

function photo(slot: number, remark: string | null = null): SheetPhoto {
  return { slot, contentType: 'image/png', sizeBytes: 12, remark, url: `https://objets.test/${slot}.png` };
}

function fiche(over: Partial<ResolutionSheet> = {}, content: Partial<ResolutionContent> = {}): ResolutionSheet {
  return {
    projectId: PROJET,
    projectTitle: 'Rebut ligne A',
    problemStatement: 'Rebut à 3,2 %',
    saved: false,
    content: { ...emptyContent(), ...content },
    settings: {
      defaults: true,
      questions: [
        { code: 'DEF-01', section: 'DEFINE', topic: 'WHAT', label: 'Quel est le problème ?', answerType: 'TEXT' },
        { code: 'DEF-05', section: 'DEFINE', topic: 'WHO', label: 'Qui est concerné ?', answerType: 'TEXT' },
        { code: 'DEF-X', section: 'DEFINE', topic: null, label: 'Hors rubrique', answerType: 'TEXT' },
        { code: 'MES-01', section: 'MEASURE', topic: null, label: 'Moyen de mesure vérifié ?', answerType: 'YES_NO' },
        { code: 'CON-01', section: 'CONTAIN', topic: null, label: 'Produits suspects identifiés ?', answerType: 'YES_NO_UNKNOWN' },
        { code: 'CTL-04', section: 'CONTROL', topic: null, label: 'Plan de surveillance ?', answerType: 'YES_NO_NA' }
      ],
      failureTypes: [{ code: 'V10', label: 'Dimensional issue' }],
      severities: [{ level: 1, label: 'Sévère' }, { level: 2, label: 'Modérée' }]
    },
    photos: [],
    updatedAt: null,
    ...over
  };
}

describe('DmaicResolutionComponent', () => {
  let fixture: ComponentFixture<DmaicResolutionComponent>;
  let component: DmaicResolutionComponent;
  let svc: jasmine.SpyObj<DmaicResolutionService>;
  let ishikawa: jasmine.SpyObj<IshikawaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let droits: BehaviorSubject<boolean>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  async function setup(sheet: ResolutionSheet | 'erreur' = fiche(), peutModifier = true): Promise<void> {
    svc = jasmine.createSpyObj<DmaicResolutionService>('DmaicResolutionService',
      ['sheet', 'save', 'uploadPhoto', 'updateRemark', 'deletePhoto', 'claimReport']);
    svc.sheet.and.returnValue(sheet === 'erreur' ? throwError(() => ({ status: 500 })) : of(sheet));
    ishikawa = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['listDiagrams', 'createDiagram']);
    ishikawa.listDiagrams.and.returnValue(of({
      content: [{ id: 'ish-1', problemStatement: 'Rebut', causes: [] }],
      totalElements: 1, totalPages: 1, number: 0, size: 50
    } as unknown as IshikawaPage));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    droits = new BehaviorSubject<boolean>(peutModifier);
    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(droits.asObservable());

    await TestBed.configureTestingModule({
      declarations: [DmaicResolutionComponent],
      imports: [SharedModule, UiModule, FormsModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideDirectoryStub(),
        { provide: DmaicResolutionService, useValue: svc },
        { provide: IshikawaService, useValue: ishikawa },
        { provide: AuthzService, useValue: authz },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u-1' }) } },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DmaicResolutionComponent);
    component = fixture.componentInstance;
    component.projectId = PROJET;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  /** Ouvre une phase : la fiche n'affiche que celle qu'on regarde. */
  function voir(phase: DmaicPhase): void {
    component.projectPhase = 'CONTROL';
    component.view = phase;
    fixture.detectChanges();
  }

  /** Une ligne d'action telle que la fiche la tient, sans passer par la fenêtre CAPA. */
  function ajouterLigne(containment = false): void {
    component.draft.actions = [...component.draft.actions, {
      createdOn: null, action: null, responsible: null, status: 'OPEN', plannedOn: null, doneOn: null,
      percent: 0, notes: null, capaId: null, containment
    }];
  }

  function confirmer(reponse: boolean): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(reponse)
    } as MatDialogRef<unknown>);
  }

  it('lit une fiche vierge : sommaire, phases, rapport éteint tant que rien n\'est enregistré', async () => {
    await setup();
    expect(svc.sheet).toHaveBeenCalledWith(PROJET);
    expect(un('etat')?.textContent).toContain('Fiche vierge');
    expect(component.sections).toEqual(['DEFINE', 'MEASURE', 'CONTAIN', 'ANALYZE', 'IMPROVE', 'CONTROL']);
    expect(component.phases).toEqual(['DEFINE', 'MEASURE', 'ANALYZE', 'IMPROVE', 'CONTROL']);
    expect(component.view).toBe('DEFINE');
    expect(component.answeredIn('DEFINE')).toBe(0);
    expect(un('rapport')?.hasAttribute('disabled')).toBeTrue();
    expect(component.ishikawas).toEqual([{ id: 'ish-1', label: 'Rebut' }]);
    expect(component.dirty).toBeFalse();
    expect(un('enregistrer')).toBeNull();
    voir('MEASURE');
    expect(un('q-MES-01')?.textContent).toContain('Moyen de mesure vérifié ?');
    voir('ANALYZE');
    expect(un('choix-CON-01-UNKNOWN')?.textContent).toContain('Inconnu');
    voir('CONTROL');
    expect(un('choix-CTL-04-NA')?.textContent).toContain('Sans objet');
  });

  it('dispose l\'en-tête comme le gabarit, et les actions juste au-dessus des phases', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const [gauche, droite] = Array.from(el.querySelectorAll('.entete__col'));
    expect(gauche.querySelector('[data-test="client"]')).not.toBeNull();
    expect(gauche.querySelector('[data-test="owner"]')).not.toBeNull();
    expect(gauche.querySelector('.equipe')).not.toBeNull();
    expect(droite.querySelector('[data-test="claim"]')).not.toBeNull();
    expect(droite.querySelector('[data-test="opened"]')).not.toBeNull();
    expect(droite.querySelector('[data-test="closed"]')).not.toBeNull();
    expect(el.querySelector('.entete__qualif [data-test="failure"]')).not.toBeNull();

    const actions = el.querySelectorAll('.barre-projet');
    expect(actions.length).toBe(1);
    const stepper = el.querySelector('.phase-stepper') as HTMLElement;
    expect(actions[0].compareDocumentPosition(stepper) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(el.querySelector('.entete')!.compareDocumentPosition(actions[0]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('range « Définir » en rubriques, les questions sans rubrique à la fin', async () => {
    await setup();
    const groupes = component.groupsOf('DEFINE');
    expect(groupes.map(g => g.label)).toEqual(['Quoi', 'Qui', null]);
    expect(groupes[2].questions.map(q => q.code)).toEqual(['DEF-X']);
    expect(component.groupsOf('MEASURE')).toEqual([jasmine.objectContaining({ topic: null, label: null })]);
    expect(component.groupsOf('IMPROVE')).toEqual([]);
  });

  it('rend les mêmes groupes d\'une détection à l\'autre, sans quoi chaque question serait recréée sans fin', async () => {
    await setup();
    const avant = component.groupsOf('DEFINE');
    fixture.detectChanges();
    expect(component.groupsOf('DEFINE')).toBe(avant);
    expect(component.groupsOf('DEFINE')[0]).toBe(avant[0]);
  });

  it('une réponse fermée se donne et se retire d\'un clic ; elle part en code', async () => {
    await setup();
    voir('MEASURE');
    un('choix-MES-01-YES')!.click();
    fixture.detectChanges();
    expect(component.draft.answers['MES-01']).toBe('YES');
    expect(un('choix-MES-01-YES')?.getAttribute('aria-checked')).toBe('true');
    expect(component.dirty).toBeTrue();
    un('choix-MES-01-YES')!.click();
    expect(component.draft.answers['MES-01']).toBeUndefined();
    component.setAnswer('DEF-01', '   ');
    expect(component.answerOf('DEF-01')).toBe('');
    component.setAnswer('DEF-01', 'Rayure');
    expect(component.answerOf('DEF-01')).toBe('Rayure');
    expect(component.choicesOf(component.questionsOf('CONTROL')[0])).toEqual(['YES', 'NO', 'NA']);
  });

  it('enregistre la fiche nettoyée, puis reprend la réponse du serveur', async () => {
    await setup();
    component.draft.clientName = '  Acme  ';
    component.setAnswer('MES-01', 'NO');
    component.setSummary('ANALYZE', 'Outil usé');
    fixture.detectChanges();
    const enregistree = fiche({ saved: true, updatedAt: '2026-10-24T08:00:00Z' },
      { clientName: 'Acme', answers: { 'MES-01': 'NO' }, rootCause: 'Outil usé' });
    svc.save.and.returnValue(of(enregistree));
    un('enregistrer')!.click();
    fixture.detectChanges();

    const envoye = svc.save.calls.mostRecent().args[1];
    expect(svc.save.calls.mostRecent().args[0]).toBe(PROJET);
    expect(envoye.clientName).toBe('Acme');
    expect(envoye.answers).toEqual({ 'MES-01': 'NO' });
    expect(envoye.rootCause).toBe('Outil usé');
    expect(component.dirty).toBeFalse();
    expect(un('etat')?.textContent).toContain('Enregistrée le');
    expect(un('rapport')?.hasAttribute('disabled')).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Fiche de résolution enregistrée.', undefined, { duration: 3000 });
  });

  it('dit l\'échec d\'un enregistrement et garde le brouillon', async () => {
    await setup();
    component.draft.claimNumber = 'CL-1';
    svc.save.and.returnValue(throwError(() => ({ status: 403 })));
    component.save();
    expect(component.saving).toBeFalse();
    expect(component.draft.claimNumber).toBe('CL-1');
    expect(snack.open).toHaveBeenCalledWith('Vous n\'avez pas les droits pour cette action.', undefined, { duration: 5000 });
  });

  it('refuse d\'enregistrer une fiche incohérente, en disant pourquoi', async () => {
    await setup();
    component.draft.openedOn = '2026-10-10';
    component.draft.closedOn = '2026-10-01';
    ajouterLigne();
    component.draft.actions[0].percent = 120;
    component.draft.actions[0].createdOn = '2026-10-10';
    component.draft.actions[0].plannedOn = '2026-10-02';
    fixture.detectChanges();
    expect(component.problems.length).toBe(3);
    expect(un('problemes')?.textContent).toContain('La date de fermeture ne peut pas précéder la date de création.');
    expect(un('enregistrer')?.hasAttribute('disabled')).toBeTrue();
    component.save();
    expect(svc.save).not.toHaveBeenCalled();
    component.draft.actions[0].percent = 2.5;
    expect(component.problems).toContain('L\'avancement d\'une action est un nombre entier de 0 à 100.');
  });

  it('annule les changements', async () => {
    await setup(fiche({ saved: true }, { clientName: 'Acme' }));
    component.draft.clientName = 'Autre';
    fixture.detectChanges();
    un('annuler')!.click();
    expect(component.draft.clientName).toBe('Acme');
    expect(component.dirty).toBeFalse();
  });

  it('sans l\'action dmaic.manage, la fiche se lit sans se modifier', async () => {
    await setup(fiche({ saved: true }, { answers: { 'MES-01': 'YES' } }), false);
    expect(component.canEdit).toBeFalse();
    expect(component.teamPicker.disabled).toBeTrue();
    expect(un('etat')?.textContent).toContain('Lecture seule');
    voir('MEASURE');
    expect(un('choix-MES-01-NO')?.hasAttribute('disabled')).toBeTrue();
    component.toggleAnswer('MES-01', 'NO');
    expect(component.draft.answers['MES-01']).toBe('YES');
    component.draft.clientName = 'x';
    component.save();
    expect(svc.save).not.toHaveBeenCalled();
    expect(un('enregistrer')).toBeNull();
    voir('DEFINE');
    expect(un('photo-input-1')).toBeNull();
    expect(un('photo-1')?.textContent).toContain('Pas de photo');
    droits.next(true);
    expect(component.teamPicker.enabled).toBeTrue();
  });

  it('désigne les personnes dans l\'annuaire : propriétaire, équipe sans doublon, responsable', async () => {
    await setup();
    component.onOwner(personne(1));
    expect(component.draft.problemOwner).toEqual({ accountId: personne(1).accountId, name: 'Personne 1' });
    component.onOwner(null);
    expect(component.draft.problemOwner).toBeNull();

    component.addTeam(personne(1));
    component.addTeam(personne(1));
    component.addTeam(null);
    component.addTeam(personne(2));
    expect(component.draft.team.map(p => p.name)).toEqual(['Personne 1', 'Personne 2']);
    expect(component.teamIds).toEqual([personne(1).accountId, personne(2).accountId]);
    await fixture.whenStable();
    expect(component.teamPicker.value).toBeNull();
    fixture.detectChanges();
    expect(hote().querySelectorAll('[data-test="team-member"]').length).toBe(2);

    component.removeTeam(personne(1).accountId);
    expect(component.teamIds).toEqual([personne(2).accountId]);

    for (let i = 0; i < 20; i++) component.addTeam({ ...personne(3), accountId: 'a' + i });
    expect(component.draft.team.length).toBe(15);

    ajouterLigne();
    component.onResponsible(component.draft.actions[0], STUB_PERSON);
    expect(component.draft.actions[0].responsible).toEqual({ accountId: STUB_PERSON.accountId, name: STUB_PERSON.displayName });
    component.onResponsible(component.draft.actions[0], null);
    expect(component.draft.actions[0].responsible).toBeNull();
  });

  it('tient les chaînes de 5 pourquoi, cinq pourquoi au plus, trente chaînes au plus', async () => {
    await setup(fiche({}, { whys: [{ branch: 'SYSTEMIC', cause: 'c', whys: [], rootCause: true, action: null }] }));
    expect(component.chainsOf('SYSTEMIC')[0].whys).toEqual(['']);
    component.addChain('OCCURRENCE');
    const chaine = component.chainsOf('OCCURRENCE')[0];
    for (let i = 0; i < 8; i++) component.addWhy(chaine);
    const c = component.chainsOf('OCCURRENCE')[0];
    expect(c.whys.length).toBe(5);
    component.setWhy(c, 0, 'Pourquoi 1');
    component.removeWhy(c, 4);
    expect(c.whys.length).toBe(4);
    expect(clean(component.draft).whys.find(w => w.branch === 'OCCURRENCE')?.whys).toEqual(['Pourquoi 1']);

    component.removeChain(c);
    expect(component.chainsOf('OCCURRENCE')).toEqual([]);
    for (let i = 0; i < 40; i++) component.addChain('DETECTION');
    expect(component.draft.whys.length).toBe(30);
    expect(component.canAddChain).toBeFalse();
  });

  it('« Ajouter une action » ouvre la fenêtre CAPA ; la ligne reprend ce qui y est saisi', async () => {
    await setup();
    voir('IMPROVE');
    // Le serveur renvoie la fiche telle qu'enregistrée : la ligne ajoutée y figure.
    svc.save.and.callFake((_id, content) => of(fiche({ saved: true }, content)));
    const open = spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of({
        capaId: 'capa-1',
        action: { title: 'Changer le gabarit', ownerId: 'kc-1', ownerName: 'Alice', dueDate: '2026-11-30' }
      })
    } as MatDialogRef<unknown>);
    un('action-ajout')!.click();

    const data = open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.prefill?.type).toBe('CORRECTIVE');
    expect(component.draft.actions).toEqual([jasmine.objectContaining({
      action: 'Changer le gabarit', responsible: { accountId: 'kc-1', name: 'Alice' }, plannedOn: '2026-11-30',
      capaId: 'capa-1', containment: false, status: 'OPEN', percent: 0, createdOn: new Date().toISOString().slice(0, 10)
    })]);
    expect(svc.save).toHaveBeenCalled();
    expect(component.improveActions.length).toBe(1);
    expect(component.containmentActions).toEqual([]);
  });

  it('une fenêtre CAPA fermée sans créer n\'ajoute aucune action', async () => {
    await setup();
    spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.addAction(true);
    expect(component.draft.actions).toEqual([]);
    expect(svc.save).not.toHaveBeenCalled();
  });

  it('l\'action de confinement se range dans Analyser et suffit à tenir le confinement', async () => {
    await setup();
    voir('ANALYZE');
    // Le serveur renvoie la fiche telle qu'enregistrée : la ligne ajoutée y figure.
    svc.save.and.callFake((_id, content) => of(fiche({ saved: true }, content)));
    const open = spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of({ capaId: 'capa-2', action: { title: 'Trier le stock' } })
    } as MatDialogRef<unknown>);
    expect(missingOf(component.sheet, component.draft, ['CONTAIN'])).toContain('Action de confinement');
    un('confinement-ajout')!.click();
    fixture.detectChanges();

    const data = open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.prefill?.description).toContain('confinement');
    expect(component.containmentActions).toEqual([jasmine.objectContaining({
      action: 'Trier le stock', responsible: null, plannedOn: null, capaId: 'capa-2', containment: true
    })]);
    expect(component.improveActions).toEqual([]);
    expect(fixture.nativeElement.querySelectorAll('[data-test="confinement"]').length).toBe(1);
    expect(missingOf(component.sheet, component.draft, ['CONTAIN'])).not.toContain('Action de confinement');
  });

  it('retire une action et s\'arrête à cent actions', async () => {
    await setup();
    ajouterLigne();
    component.removeAction(component.draft.actions[0]);
    expect(component.draft.actions).toEqual([]);
    for (let i = 0; i < 100; i++) ajouterLigne();
    expect(component.canAddAction).toBeFalse();
    const open = spyOn(TestBed.inject(MatDialog), 'open');
    component.addAction();
    expect(open).not.toHaveBeenCalled();
    expect(component.actionStatusLabel('IN_REVIEW')).toBe('En revue');
  });

  it('chaque phase a sa synthèse ; « Définir » n\'en a pas', async () => {
    await setup();
    for (const s of ['MEASURE', 'CONTAIN', 'ANALYZE', 'IMPROVE', 'CONTROL'] as const) {
      component.setSummary(s, 'texte ' + s);
      expect(component.summaryOf(s)).toBe('texte ' + s);
    }
    component.setSummary('DEFINE', 'rien');
    expect(component.summaryOf('DEFINE')).toBeNull();
    expect(component.sectionText('CONTAIN').summary).toBe('Action de confinement');
    expect(component.draft.surveillancePlan).toBe('texte CONTROL');
  });

  it('verse une photo dans son emplacement, refuse un mauvais format ou un fichier trop lourd', async () => {
    await setup();
    const evenement = (f: File) => ({ target: { files: [f], value: 'x' } } as unknown as Event);
    component.onFile(1, evenement(new File(['x'], 'a.pdf', { type: 'application/pdf' })));
    expect(snack.open).toHaveBeenCalledWith('Format refusé : JPEG, PNG, GIF ou WEBP uniquement.', undefined, { duration: 5000 });
    const lourd = new File(['x'], 'a.png', { type: 'image/png' });
    Object.defineProperty(lourd, 'size', { value: 11 * 1024 * 1024 });
    component.onFile(1, evenement(lourd));
    expect(snack.open).toHaveBeenCalledWith('La photo dépasse 10 Mo.', undefined, { duration: 5000 });
    component.onFile(1, { target: { files: [], value: '' } } as unknown as Event);
    expect(svc.uploadPhoto).not.toHaveBeenCalled();

    svc.uploadPhoto.and.returnValue(of(photo(2, 'vue de face')));
    const png = new File(['x'], 'b.png', { type: 'image/png' });
    component.onFile(2, evenement(png));
    expect(svc.uploadPhoto).toHaveBeenCalledWith(PROJET, 2, png, null);
    expect(component.photoAt(2)?.remark).toBe('vue de face');
    expect(component.remarks[2]).toBe('vue de face');
    expect(component.photoBusy.has(2)).toBeFalse();
    fixture.detectChanges();
    expect(un('photo-2')?.querySelector('img')?.getAttribute('alt')).toBe('vue de face');
  });

  it('dit que le stockage est éteint, ou que le versement a échoué', async () => {
    await setup();
    const png = { target: { files: [new File(['x'], 'b.png', { type: 'image/png' })], value: 'x' } } as unknown as Event;
    svc.uploadPhoto.and.returnValue(throwError(() => ({ status: 503 })));
    component.onFile(3, png);
    expect(snack.open.calls.mostRecent().args[0]).toContain('stockage des photos');
    svc.uploadPhoto.and.returnValue(throwError(() => ({ status: 0 })));
    component.onFile(3, { target: { files: [new File(['x'], 'b.png', { type: 'image/png' })], value: 'x' } } as unknown as Event);
    expect(component.photoBusy.has(3)).toBeFalse();
  });

  it('enregistre la remarque d\'une photo à la sortie du champ, si elle a changé', async () => {
    await setup(fiche({ photos: [photo(1, 'avant')] }));
    component.saveRemark(1);
    component.saveRemark(4);
    expect(svc.updateRemark).not.toHaveBeenCalled();

    component.remarks[1] = '  après ';
    svc.updateRemark.and.returnValue(of(photo(1, 'après')));
    component.saveRemark(1);
    expect(svc.updateRemark).toHaveBeenCalledWith(PROJET, 1, 'après');
    expect(component.photoAt(1)?.remark).toBe('après');

    component.remarks[1] = '';
    svc.updateRemark.and.returnValue(throwError(() => ({ status: 500 })));
    component.saveRemark(1);
    expect(svc.updateRemark).toHaveBeenCalledWith(PROJET, 1, null);
    expect(component.remarks[1]).toBe('après');
  });

  it('retire une photo seulement après confirmation', async () => {
    await setup(fiche({ photos: [photo(1), photo(5)] }));
    confirmer(false);
    component.removePhoto(1);
    expect(svc.deletePhoto).not.toHaveBeenCalled();

    (TestBed.inject(MatDialog).open as jasmine.Spy).and.returnValue({ afterClosed: () => of(true) });
    svc.deletePhoto.and.returnValue(of(undefined));
    component.removePhoto(1);
    expect(svc.deletePhoto).toHaveBeenCalledWith(PROJET, 1);
    expect(component.sheet?.photos.map(p => p.slot)).toEqual([5]);

    svc.deletePhoto.and.returnValue(throwError(() => ({ status: 500 })));
    component.removePhoto(5);
    expect(component.photoAt(5)).toBeDefined();
    expect(snack.open).toHaveBeenCalled();
  });

  it('télécharge le rapport client sous le nom donné par le serveur', async () => {
    await setup(fiche({ saved: true }));
    const clic = spyOn(HTMLAnchorElement.prototype, 'click');
    spyOn(URL, 'createObjectURL').and.returnValue('blob:rapport');
    spyOn(URL, 'revokeObjectURL');
    svc.claimReport.and.returnValue(of(new HttpResponse({
      body: new Blob(['%PDF']), status: 200,
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="rapport-client-dmaic.pdf"' })
    })));
    un('rapport')!.click();
    expect(svc.claimReport).toHaveBeenCalledWith(PROJET);
    expect(clic).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:rapport');
    expect(component.downloading).toBeFalse();

    svc.claimReport.and.returnValue(of(new HttpResponse<Blob>({ body: null, status: 200 })));
    component.downloadReport();
    expect(snack.open).toHaveBeenCalledWith('Le rapport n\'a pas pu être produit.', undefined, { duration: 5000 });

    svc.claimReport.and.returnValue(throwError(() => ({ status: 500 })));
    component.downloadReport();
    expect(component.downloading).toBeFalse();

    svc.claimReport.calls.reset();
    component.draft.clientName = 'modifié';
    fixture.detectChanges();
    expect(un('rapport-aide')).not.toBeNull();
    component.downloadReport();
    expect(svc.claimReport).not.toHaveBeenCalled();
  });

  it('dit l\'échec de la lecture et réessaie', async () => {
    await setup('erreur');
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
    expect(component.dirty).toBeFalse();
    component.discard();
    svc.sheet.and.returnValue(of(fiche()));
    component.load();
    expect(component.failed).toBeFalse();
    expect(component.sheet).not.toBeNull();
  });

  it('se passe de la liste des Ishikawa quand elle ne se lit pas', async () => {
    TestBed.resetTestingModule();
    await setup();
    ishikawa.listDiagrams.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    expect(component.ishikawas).toEqual([]);
  });

  it("crée le diagramme 6 M du classeur, avec le problème du projet pour effet, et l'enregistre lié", async () => {
    await setup();
    component.problemStatement = '  Fuite au joint  ';
    ishikawa.createDiagram.and.returnValue(of({ id: 'ish-9', problemStatement: 'Fuite au joint' } as IshikawaDiagramResponse));
    svc.save.and.returnValue(of(fiche({ saved: true }, { ishikawaId: 'ish-9' })));
    voir('ANALYZE');
    un('ishikawa-create')!.click();

    expect(ishikawa.createDiagram).toHaveBeenCalledWith(jasmine.objectContaining({
      problemStatement: 'Fuite au joint', mode: 'SIX_M', ownerId: 'u-1'
    }));
    expect(component.ishikawas[0]).toEqual({ id: 'ish-9', label: 'Fuite au joint' });
    expect(svc.save.calls.mostRecent().args[1].ishikawaId).toBe('ish-9');
    expect(component.creatingIshikawa).toBeFalse();
  });

  it('prend un effet par défaut sans charte, et ne lie rien quand la création échoue', async () => {
    await setup();
    ishikawa.createDiagram.and.returnValue(throwError(() => ({ status: 500 })));
    component.createIshikawa();

    expect(ishikawa.createDiagram.calls.mostRecent().args[0].problemStatement).toBe('Problème du projet DMAIC');
    expect(component.draft.ishikawaId).toBeNull();
    expect(svc.save).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.creatingIshikawa).toBeFalse();
  });

  it('ne propose pas la création sans le droit de modifier', async () => {
    await setup(fiche(), false);
    expect(un('ishikawa-create')).toBeNull();
    component.createIshikawa();
    expect(ishikawa.createDiagram).not.toHaveBeenCalled();
  });

  it('ouvre les phases passées et la phase en cours, jamais celles qui suivent', async () => {
    await setup();
    component.projectPhase = 'ANALYZE';
    fixture.detectChanges();
    expect(component.view).toBe('ANALYZE');
    component.show('IMPROVE');
    expect(component.view).toBe('ANALYZE');
    un('phase-DEFINE')!.click();
    expect(component.view).toBe('DEFINE');
    fixture.detectChanges();
    expect(un('phase-CONTROL')?.hasAttribute('disabled')).toBeTrue();
    expect(component.phaseLetter('IMPROVE')).toBe('I');
    expect(component.trackByIndex(3)).toBe(3);
    expect(component.photoAlt(4)).toBe('Photo 4');
  });

  it('signale les réponses qui manquent à la phase en cours', async () => {
    await setup();
    const manque = jasmine.createSpy('manque');
    component.missingChange.subscribe(manque);
    component.projectPhase = 'DEFINE';
    await Promise.resolve();
    expect(manque).toHaveBeenCalled();
    expect((manque.calls.mostRecent().args[0] as string[]).length).toBeGreaterThan(0);
    const complet = missingOf(component.sheet!, { ...component.sheet!.content,
      answers: Object.fromEntries(component.questionsOf('DEFINE').map(q => [q.code, 'x'])) }, ['DEFINE']);
    expect(complet).toEqual([]);
  });

  it('verse une pièce jointe PDF dans les emplacements de la phase', async () => {
    await setup();
    expect(component.attachmentSlots).toEqual([11, 12, 13, 14, 15]);
    const pdf = new File(['x'], 'plan.pdf', { type: 'application/pdf' });
    svc.uploadPhoto.and.returnValue(of(photo(11, 'plan.pdf')));
    component.onFile(11, { target: { files: [pdf], value: 'x' } } as unknown as Event);
    expect(svc.uploadPhoto).toHaveBeenCalledWith(PROJET, 11, pdf, null);
    const exe = new File(['x'], 'a.exe', { type: 'application/x-msdownload' });
    component.onFile(12, { target: { files: [exe], value: 'x' } } as unknown as Event);
    expect(svc.uploadPhoto).toHaveBeenCalledTimes(1);
  });

  it('les études Cp/Cpk et Gage R&R partent pré-remplies du projet', async () => {
    await setup();
    component.projectTitle = 'Rebut';
    component.spec = { lsl: 9.9, usl: 10.1 };
    expect(component.studyParams).toEqual(jasmine.objectContaining({ dmaic: PROJET, lsl: '9.9', usl: '10.1' }));
  });

  it('crée une CAPA depuis une action, puis garde son lien', async () => {
    await setup();
    ajouterLigne();
    const ligne = component.draft.actions[0];
    ligne.action = 'Changer le gabarit';
    svc.save.and.returnValue(of(fiche()));
    spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of({ capaId: 'capa-9' })
    } as MatDialogRef<unknown>);
    component.openCapa(ligne);
    expect(ligne.capaId).toBe('capa-9');
    expect(svc.save).toHaveBeenCalled();
  });

  it('clean() rogne les textes, efface les vides et range les réponses', () => {
    const c = clean({
      ...emptyContent(), clientName: '  ', claimNumber: ' CL-7 ', answers: { 'Z-1': 'x', 'A-1': ' ', 'B-2': 'YES' },
      actions: [{ createdOn: '', action: ' a ', responsible: null, status: 'OPEN', plannedOn: null, doneOn: null,
        percent: null as unknown as number, notes: '', capaId: null }]
    });
    expect(c.clientName).toBeNull();
    expect(c.claimNumber).toBe('CL-7');
    expect(Object.keys(c.answers)).toEqual(['B-2', 'Z-1']);
    expect(c.actions[0]).toEqual(jasmine.objectContaining({ createdOn: null, action: 'a', percent: 0, notes: null }));
    const vide = clean({ answers: undefined, team: undefined, whys: undefined, actions: undefined } as unknown as ResolutionContent);
    expect(vide.answers).toEqual({});
    expect(vide.team).toEqual([]);
  });
});
