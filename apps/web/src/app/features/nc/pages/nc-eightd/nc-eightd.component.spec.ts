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
import { EightDDiscipline, EightDReport, NcResponse } from '../../nc.types';
import { By } from '@angular/platform-browser';

import {
  CapaActionPanelStubComponent, CapaActionPanelStubModule
} from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';
import {
  NcCauseAnalysisStubComponent, NcCauseAnalysisStubModule
} from '../../components/nc-cause-analysis/testing/nc-cause-analysis.stub';
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
  let fiveWhys: jasmine.SpyObj<FiveWhysService>;
  let ishikawa: jasmine.SpyObj<IshikawaService>;

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

  /** Une chaine 5 Pourquoi rattachee a l'ecart. */
  function chaine(): FiveWhysAnalysis {
    return {
      id: 'fw-1', ncId: NC_ID, ncReference: 'NC-2026-0007',
      problem: 'Fuite au presse-étoupe', rootCause: 'Pas de plan préventif',
      steps: [{ id: 's1', position: 1, answer: 'Le joint a cédé', createdAt: '', updatedAt: '' }],
      createdAt: '', updatedAt: ''
    } as FiveWhysAnalysis;
  }

  /** Un diagramme d'Ishikawa rattache a l'ecart. */
  function diagramme(): IshikawaDiagramResponse {
    return {
      id: 'ish-1', tenantId: 't', problemStatement: 'Fuite au presse-étoupe',
      mode: 'SIX_M', status: 'DRAFT', ownerId: 'u1', ncId: NC_ID,
      createdAt: '', updatedAt: '', causes: []
    } as IshikawaDiagramResponse;
  }

  async function monter(roles: string[], capaCaseId: string | null = 'capa-9'): Promise<void> {
    svc = jasmine.createSpyObj<NcService>('NcService',
      ['getEightDReport', 'saveEightDReport', 'issueEightDReport', 'downloadEightDPdf', 'getNc']);
    const ecart: NcResponse = {
      id: NC_ID, reference: 'NC-2026-0007', title: 'Fuite au presse-étoupe',
      category: 'PROCESS', severity: 'MAJOR', status: 'ACTION_DEFINED', origin: 'INTERNAL',
      detectedAt: '', createdAt: '', updatedAt: ''
    };
    if (capaCaseId) { ecart.capaCaseId = capaCaseId; }
    svc.getNc.and.returnValue(of(ecart));
    fiveWhys = jasmine.createSpyObj<FiveWhysService>('FiveWhysService', ['listForNc']);
    fiveWhys.listForNc.and.returnValue(of([]));
    ishikawa = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['listForNc']);
    ishikawa.listForNc.and.returnValue(of([]));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    svc.getEightDReport.and.returnValue(of(rapport()));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [NcEightDComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule,
                CapaActionPanelStubModule, NcCauseAnalysisStubModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: FiveWhysService, useValue: fiveWhys },
        { provide: IshikawaService, useValue: ishikawa },
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

  // ---------- les blocs vivants : D4, D5, D6 (ADR 0115) ----------

  /**
   * Le 8D disait le D4 en lignes de texte : on y lisait la cause retenue sans
   * voir le raisonnement qui y mene, alors que c'est le raisonnement qu'un
   * auditeur vient regarder.
   */
  it('dessine l\u2019analyse de cause sous le D4, et nulle part ailleurs', async () => {
    await monter(['QUALITY_MANAGER']);
    fiveWhys.listForNc.and.returnValue(of([chaine()]));
    ishikawa.listForNc.and.returnValue(of([diagramme()]));
    component.ngOnInit();
    fixture.detectChanges();

    const figures = fixture.debugElement.queryAll(By.directive(NcCauseAnalysisStubComponent));
    expect(figures.length).toBe(1);
    const instance = figures[0].componentInstance as NcCauseAnalysisStubComponent;
    expect(instance.fiveWhys.length).toBe(1);
    expect(instance.ishikawa.length).toBe(1);
    expect(fixture.nativeElement.querySelector('[data-test="8d-analyse-de-cause"]')).not.toBeNull();
  });

  // Une carte vide se lirait comme une panne : sans analyse rattachee, le bloc
  // n'apparait pas -- le texte agrege de la discipline, lui, dit deja ce qu'il sait.
  it('n\u2019ouvre aucun bloc d\u2019analyse quand l\u2019\u00e9cart n\u2019en porte aucune', async () => {
    await monter(['QUALITY_MANAGER']);

    expect(fixture.debugElement.queryAll(By.directive(NcCauseAnalysisStubComponent)).length).toBe(0);
  });

  /**
   * Le D3, le D5 et le D6 nomment trois gestes DIFFERENTS : proteger le client,
   * corriger la cause, prevenir la recidive. Chacun recoit donc le tableau de SA
   * nature -- un tableau complet repete sous trois titres laisserait chercher,
   * sous chacun, ses lignes.
   *
   * <p>Le D3 garde en plus son champ de saisie : un endiguement est souvent
   * decide avant qu'aucun dossier CAPA n'existe, et ce texte-la est le seul que
   * le PDF portera. Sans son tableau, une action de nature CONTAINMENT
   * (ADR 0115) ne s'affichait nulle part dans le rapport.
   *
   * <p>Le D5, lui, en nomme TROIS : c'est la discipline ou l'on vient lire ce
   * qui a ete decide, et son titre le dit (« defined containment, corrective
   * and preventive actions »). Restreint aux correctives, il laissait dehors
   * l'endiguement et la prevention saisis depuis l'ecart -- on les avait
   * ecrits, ils etaient dans le dossier, et le rapport ne les montrait pas.
   */
  it("donne au D3 l'endiguement, au D5 les trois natures, au D6 les pr\u00e9ventives", async () => {
    await monter(['QUALITY_MANAGER']);

    const tableaux = fixture.debugElement.queryAll(By.directive(CapaActionPanelStubComponent));
    expect(tableaux.length).toBe(3);
    const natures = tableaux.map(t => (t.componentInstance as CapaActionPanelStubComponent).natures);
    expect(natures).toEqual([
      ['CONTAINMENT'], ['CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE'], ['PREVENTIVE']]);
    const dossiers = tableaux.map(t => (t.componentInstance as CapaActionPanelStubComponent).capaCaseId);
    expect(dossiers).toEqual(['capa-9', 'capa-9', 'capa-9']);
  });

  /**
   * L'entree passee au tableau doit etre LE MEME OBJET d'une passe de detection
   * a l'autre. Un gabarit qui appelle `naturesDe(code)` recevrait sinon un
   * tableau neuf a chaque passe : Angular y verrait une entree changee, le
   * panneau relirait le dossier CAPA par le reseau, et sa reponse declencherait
   * la passe suivante -- une requete en boucle pour un ecran immobile. Le meme
   * defaut, sur la liste des ecarts, figeait la page entiere.
   */
  /**
   * Le D5 listait le dossier (« CAPA : ... », son type, sa criticite, son
   * echeance) puis une ligne de phrase par action, JUSTE AU-DESSUS du tableau
   * qui montre ces memes actions avec leur porteur, leur avancement et leur
   * preuve -- et qu'on peut, lui, faire avancer. Deux lectures du meme fait,
   * dont une inerte.
   *
   * <p>Le texte reste dans le document emis : le PDF n'a pas de tableau, et
   * c'est ce texte-la qui est fige, signe et ancre a l'emission. On ne le
   * retire donc que de l'ecran.
   */
  it('ne redit pas, sous le D5, les lignes d’action que le tableau montre', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.getEightDReport.and.returnValue(of(rapport({
      disciplines: [
        { ...discipline('D2', true, false), lines: ['Constat au presse-etoupe'] },
        {
          ...discipline('D5', true, false),
          lines: [
            'CAPA: Reprise du presse-etoupe',
            'Action [CORRECTIVE] Changer le joint — IN_PROGRESS'
          ]
        }
      ]
    })));
    component.reload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Le D2 garde son texte : lui n'a pas de tableau pour le dire.
    expect(hote().textContent).toContain('Constat au presse-etoupe');
    // Le D5 ne rend plus la ligne d'ACTION : le tableau, juste en dessous, la
    // montre avec son porteur, son avancement et sa preuve.
    expect(hote().textContent).not.toContain('Action [CORRECTIVE] Changer le joint');
    // Mais il garde ce qui decrit le DOSSIER : le tableau n'en porte rien, et
    // masquer le bloc entier faisait disparaitre l'intitule, le type, la
    // criticite, le statut et l'echeance de la CAPA.
    expect(hote().textContent).toContain('CAPA: Reprise du presse-etoupe');
    expect(component.texteRemplaceParLeTableau('D5')).toBe(true);
    expect(component.texteRemplaceParLeTableau('D2')).toBe(false);
  });

  /** Les disciplines qui portent un tableau sont exactement D3, D5 et D6. */
  it('ne montre un tableau d\u2019actions que sous D3, D5 et D6', async () => {
    await monter(['QUALITY_MANAGER']);

    ['D3', 'D5', 'D6'].forEach(code => expect(component.montreLesActions(code)).toBe(true));
    ['D1', 'D2', 'D4', 'D7', 'D8'].forEach(
      code => expect(component.montreLesActions(code)).toBe(false));
  });

  it('passe toujours le meme tableau de natures a une discipline donnee', async () => {
    await monter(['QUALITY_MANAGER']);

    expect(component.naturesDe('D3')).toBe(component.naturesDe('D3'));
    expect(component.naturesDe('D5')).toBe(component.naturesDe('D5'));
    expect(component.naturesDe('D6')).toBe(component.naturesDe('D6'));
    // Y compris pour les disciplines qui ne montrent pas d'actions.
    expect(component.naturesDe('D1')).toBe(component.naturesDe('D2'));
    expect(component.naturesDe('D1')).toEqual([]);
  });

  it('n\u2019affiche aucun tableau d\u2019actions quand l\u2019\u00e9cart n\u2019a pas de dossier CAPA', async () => {
    await monter(['QUALITY_MANAGER'], null);

    expect(fixture.debugElement.queryAll(By.directive(CapaActionPanelStubComponent)).length).toBe(0);
  });

  // Le rapport et l'ecart se chargent par deux requetes distinctes, et le
  // rapport arrive le premier. Parti de `null`, l'ecran affirmait \u00ab aucun
  // dossier CAPA \u00bb le temps que la seconde reponde -- et pour toujours si elle
  // echouait, l'erreur n'etant qu'une trace de console.
  it('ne dit pas qu\u2019il n\u2019y a pas de dossier CAPA tant que l\u2019\u00e9cart n\u2019a pas r\u00e9pondu', async () => {
    const vides = () => fixture.debugElement
      .queryAll(By.css('[data-test^="8d-actions-vide-"]')).length;
    await monter(['QUALITY_MANAGER'], null);

    // L'ecart a repondu \u00ab pas de dossier \u00bb : la discipline le dit, avec la
    // porte pour y aller.
    expect(vides()).toBeGreaterThan(0);

    // On revient a \u00ab on ne sait pas encore \u00bb : elle doit se taire.
    component.capaCaseId$.next(undefined);
    fixture.detectChanges();
    expect(vides()).toBe(0);
  });

  // Le tableau vivant ne remplace QUE les lignes d'action. Les premieres lignes
  // du D5 decrivent le dossier -- intitule, type, criticite, statut, echeance --
  // et le tableau n'en porte aucune : masquer le bloc entier les perdait.
  it('garde les lignes de dossier du D5 et retire les lignes d\u2019action', async () => {
    await monter(['QUALITY_MANAGER']);

    const d5: EightDDiscipline = {
      code: 'D5', title: 'D5', sourced: true, sourceLabel: '', editable: false,
      lines: [
        'CAPA: Reprendre le presse-\u00e9toupe',
        'Type: CORRECTIVE',
        'Criticality: MAJOR',
        'Action [CONTAINMENT] Bloquer le lot \u2014 DONE \u2014 Claire Martin',
        '    Tri a 100 % avant expedition',
        'Action [CORRECTIVE] Changer le joint \u2014 IN_PROGRESS'
      ]
    };

    expect(component.lignesVisibles(d5)).toEqual([
      'CAPA: Reprendre le presse-\u00e9toupe',
      'Type: CORRECTIVE',
      'Criticality: MAJOR'
    ]);
  });

  it('garde toutes les lignes d\u2019une discipline que le tableau ne redit pas', async () => {
    await monter(['QUALITY_MANAGER']);

    // Le D6 porte des dates d'achevement et des comptes de preuves : rien que
    // le tableau des actions preventives ne montre.
    const d6: EightDDiscipline = {
      code: 'D6', title: 'D6', sourced: true, sourceLabel: '', editable: false,
      lines: ['Changer le joint \u2014 completed on 02/11/2026 \u2014 2 evidence item(s)']
    };

    expect(component.lignesVisibles(d6)).toEqual(d6.lines);
  });

  // Sans aucune ligne d'action, il n'y a rien a retirer : le D5 garde tout.
  it('garde tout le D5 quand le dossier ne porte encore aucune action', async () => {
    await monter(['QUALITY_MANAGER']);

    const d5: EightDDiscipline = {
      code: 'D5', title: 'D5', sourced: false, sourceLabel: '', editable: false,
      lines: ['CAPA: Reprendre le presse-\u00e9toupe', 'Status: OPEN']
    };

    expect(component.lignesVisibles(d5)).toEqual(d5.lines);
  });

  // Faire avancer une action change le texte agrege des disciplines : un rapport
  // qui ne se relit pas afficherait l'etat d'avant.
  it('relit le rapport quand une action a boug\u00e9 dans un tableau', async () => {
    await monter(['QUALITY_MANAGER']);
    svc.getEightDReport.calls.reset();

    const tableau = fixture.debugElement
      .query(By.directive(CapaActionPanelStubComponent)).componentInstance as CapaActionPanelStubComponent;
    tableau.changed.emit();

    expect(svc.getEightDReport).toHaveBeenCalledTimes(1);
  });

  // Une panne d'un service d'analyse ne doit pas emporter le rapport : son texte
  // agrege est deja la, et c'est lui qui sera scelle.
  it('reste lisible quand une analyse de cause est indisponible', async () => {
    await monter(['QUALITY_MANAGER']);
    fiveWhys.listForNc.and.returnValue(throwError(() => ({ status: 503 })));
    ishikawa.listForNc.and.returnValue(throwError(() => ({ status: 503 })));
    component.ngOnInit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[data-test="discipline"]').length).toBe(8);
  });

  // ---------- garde d'identifiant ----------

  it('refuse un identifiant malformé avant tout appel', async () => {
    svc = jasmine.createSpyObj<NcService>('NcService',
      ['getEightDReport', 'saveEightDReport', 'issueEightDReport', 'downloadEightDPdf']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [NcEightDComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule,
                CapaActionPanelStubModule, NcCauseAnalysisStubModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: FiveWhysService, useValue: { listForNc: () => of([]) } },
        { provide: IshikawaService, useValue: { listForNc: () => of([]) } },
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
