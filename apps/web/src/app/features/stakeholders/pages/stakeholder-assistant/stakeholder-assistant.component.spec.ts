import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StakeholdersService } from '../../stakeholders.service';
import {
  STAKEHOLDER_LIMITS,
  StakeholderRequirement,
  StakeholderRow,
  StakeholderSheet
} from '../../stakeholders.types';
import { StakeholderAssistantComponent } from './stakeholder-assistant.component';

/**
 * Le banc de l'invariant du module : **l'IA propose, elle n'écrit rien**. Tant
 * que personne n'a cliqué, aucun appel d'écriture ne part — et une liste vide
 * est une réponse, pas une panne.
 */
describe('StakeholderAssistantComponent', () => {

  const ROWS = [
    { id: 'pi-1', name: 'ARS Occitanie' } as StakeholderRow,
    { id: 'pi-2', name: 'Clients hospitaliers' } as StakeholderRow
  ];

  const TEXTE = 'Le prestataire déclare tout événement indésirable grave sous quarante-huit heures.';

  let fixture: ComponentFixture<StakeholderAssistantComponent>;
  let component: StakeholderAssistantComponent;
  let svc: jasmine.SpyObj<StakeholdersService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(): Promise<void> {
    svc = jasmine.createSpyObj<StakeholdersService>('StakeholdersService',
      ['suggest', 'extract', 'changes', 'create', 'addRequirement', 'markToReassess', 'get']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    // Réponses par défaut des trois écritures : chaque banc vérifie SI elles
    // sont appelées, pas ce qu'elles rendent.
    svc.create.and.returnValue(of({ id: 'pi-9' } as StakeholderSheet));
    svc.addRequirement.and.returnValue(of({ id: 'ex-9' } as StakeholderRequirement));
    svc.markToReassess.and.returnValue(of([]));
    svc.get.and.returnValue(of({ id: 'pi-1', requirements: [] } as unknown as StakeholderSheet));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderAssistantComponent],
      // FormsModule, comme le module de production : l'écran pilote ses champs
      // en [(ngModel)], et SharedModule n'apporte que ReactiveFormsModule — sans
      // quoi NgControlStatus se greffe sur l'attribut ngModel sans trouver de
      // NgControl (NG0201) et le gabarit ne se rend pas du tout.
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: StakeholdersService, useValue: svc },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderAssistantComponent);
    component = fixture.componentInstance;
    component.rows = ROWS;
    fixture.detectChanges();
  }

  beforeEach(async () => build());

  // ---------- l'invariant ----------

  it('proposer n’inscrit rien : il faut un geste humain, suggestion par suggestion', () => {
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: 'Autorité de tutelle des produits de santé.' }
    ]));

    component.sector = 'Santé';
    component.suggest();

    expect(component.suggestions?.length).toBe(1);
    // Rien n'est écrit tant que personne n'a cliqué.
    expect(svc.create).not.toHaveBeenCalled();

    component.register(component.suggestions![0]);
    expect(svc.create).toHaveBeenCalledTimes(1);
  });

  it('extraire n’ajoute rien : chaque exigence s’ajoute à la main', () => {
    svc.extract.and.returnValue(of([
      { quote: TEXTE, reference: 'Art. 4', requirement: 'Déclarer tout EIG sous 48 h',
        nature: 'CONTRACTUAL', control: 'Procédure de vigilance' }
    ]));

    component.documentText = TEXTE;
    component.extract();

    expect(component.extracted?.length).toBe(1);
    expect(svc.addRequirement).not.toHaveBeenCalled();
  });

  it('comparer ne marque rien : la sélection est humaine', () => {
    svc.changes.and.returnValue(of([
      { title: 'Délai ramené à 24 h', detail: null, source: 'Art. 4', impact: 'HIGH' }
    ]));

    component.previousText = TEXTE;
    component.currentText = `${TEXTE} Modifié.`;
    component.detect();

    expect(component.detected?.length).toBe(1);
    expect(svc.markToReassess).not.toHaveBeenCalled();
  });

  // ---------- suggestions ----------

  it('affiche la justification, qui rend la proposition discutable', () => {
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: 'Autorité de tutelle.' }
    ]));
    component.sector = 'Santé';
    component.suggest();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Autorité de tutelle.');
  });

  it('ne propose pas d’inscrire ce qui est déjà au registre, casse comprise', () => {
    svc.suggest.and.returnValue(of([
      { name: '  ars occitanie ', category: 'REGULATORS', rationale: null },
      { name: 'ANSM', category: 'REGULATORS', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();

    expect(component.suggestions![0].duplicate).toBeTrue();
    expect(component.suggestions![1].duplicate).toBeFalse();

    component.register(component.suggestions![0]);
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('le côté interne / externe reste un choix : il est proposé, pas imposé', () => {
    svc.suggest.and.returnValue(of([
      { name: 'Comité social et économique', category: 'EMPLOYEES', rationale: null },
      { name: 'ANSM', category: 'REGULATORS', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();

    expect(component.suggestions![0].side).toBe('INTERNAL');
    expect(component.suggestions![1].side).toBe('EXTERNAL');

    svc.create.and.returnValue(of({ id: 'pi-9' } as StakeholderSheet));
    component.suggestions![1].side = 'INTERNAL';
    component.register(component.suggestions![1]);

    expect(svc.create).toHaveBeenCalledWith({
      name: 'ANSM', category: 'REGULATORS', side: 'INTERNAL'
    });
  });

  it('une proposition presque juste se CORRIGE avant d etre inscrite', () => {
    // La rejeter pour la ressaisir a la main perdrait le travail de
    // l'assistant ; l'inscrire telle quelle obligerait a la corriger apres.
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();
    const ligne = component.suggestions![0];

    component.toggleEdit(ligne);
    expect(ligne.editing).toBeTrue();
    ligne.name = '  Agence nationale de sécurité du médicament  ';
    ligne.category = 'CERTIFICATION_BODIES';

    svc.create.and.returnValue(of({ id: 'pi-1' } as StakeholderSheet));
    component.register(ligne);

    expect(svc.create).toHaveBeenCalledWith({
      name: 'Agence nationale de sécurité du médicament',
      category: 'CERTIFICATION_BODIES',
      side: 'EXTERNAL'
    });
  });

  it('une fois inscrite, la correction se referme : elle n a plus de prise', () => {
    // Vu à l'écran : la ligne inscrite gardait ses deux champs ouverts, ce qui
    // donnait à croire qu'on pouvait encore la retoucher là. La fiche existe :
    // elle ne se modifie plus que dans la fiche.
    svc.suggest.and.returnValue(of([
      { name: 'Clients donneurs d ordre', category: 'CUSTOMERS', rationale: null }
    ]));
    component.sector = 'Traitement de surface';
    component.suggest();
    const ligne = component.suggestions![0];
    component.toggleEdit(ligne);
    ligne.name = 'Donneurs d ordre aéronautiques';

    svc.create.and.returnValue(of({ id: 'pi-9' } as StakeholderSheet));
    component.register(ligne);

    expect(ligne.registered).toBeTrue();
    expect(ligne.editing).toBeFalse();
  });

  it('refermer la correction REMET ce qui avait été proposé', () => {
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();
    const ligne = component.suggestions![0];

    component.toggleEdit(ligne);
    ligne.name = 'frappe en cours';
    component.toggleEdit(ligne);

    expect(ligne.editing).toBeFalse();
    expect(ligne.name).toBe('ANSM');
    expect(ligne.category).toBe('REGULATORS');
  });

  it('un nom vide rouvre la correction au lieu d inscrire une fiche sans nom', () => {
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();
    const ligne = component.suggestions![0];
    ligne.name = '   ';

    component.register(ligne);

    expect(svc.create).not.toHaveBeenCalled();
    expect(ligne.editing).toBeTrue();
  });

  it('une proposition hors sujet se REJETTE, et rien n est écrit', () => {
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: null },
      { name: 'Riverains', category: 'LOCAL_COMMUNITIES', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();

    component.reject(component.suggestions![1]);

    expect(component.suggestions!.map(r => r.name)).toEqual(['ANSM']);
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('ni corriger ni rejeter pendant un enregistrement en cours', () => {
    svc.suggest.and.returnValue(of([
      { name: 'ANSM', category: 'REGULATORS', rationale: null }
    ]));
    component.sector = 'Santé';
    component.suggest();
    const ligne = component.suggestions![0];
    ligne.busy = true;

    component.toggleEdit(ligne);
    component.reject(ligne);

    expect(ligne.editing).toBeFalse();
    expect(component.suggestions!.length).toBe(1);
  });

  it('sans secteur, ne demande rien et DIT pourquoi', () => {
    component.suggest();

    expect(svc.suggest).not.toHaveBeenCalled();
    expect(component.suggestBlocked).toBeTruthy();
  });

  it('une liste vide est une réponse, pas un échec', () => {
    svc.suggest.and.returnValue(of([]));
    component.sector = 'Santé';
    component.suggest();
    fixture.detectChanges();

    expect(component.suggestions).toEqual([]);
    expect(component.suggestError).toBeNull();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="suggestions-vides"]')).not.toBeNull();
    expect(el.querySelector('[data-test="echec-suggestions"]')).toBeNull();
  });

  it('un échec du serveur est dit comme tel, et ne se confond pas avec une liste vide', () => {
    svc.suggest.and.returnValue(throwError(() => ({ status: 503 })));
    component.sector = 'Santé';
    component.suggest();
    fixture.detectChanges();

    expect(component.suggestions).toBeNull();
    expect(component.suggestError).toBeTruthy();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="echec-suggestions"]')).not.toBeNull();
  });

  // ---------- extraction ----------

  it('sous le seuil de lecture, bloque avant l’appel et dit le minimum', () => {
    component.documentText = 'trop court';

    expect(component.extractBlocked).toContain(String(STAKEHOLDER_LIMITS.documentMin));
    component.extract();
    expect(svc.extract).not.toHaveBeenCalled();
  });

  it('exige une partie prenante de destination avant d’ajouter', () => {
    svc.extract.and.returnValue(of([
      { quote: TEXTE, reference: null, requirement: 'Déclarer tout EIG sous 48 h',
        nature: null, control: null }
    ]));
    component.documentText = TEXTE;
    component.extract();

    expect(component.addBlocked).toBeTruthy();
    component.add(component.extracted![0]);
    expect(svc.addRequirement).not.toHaveBeenCalled();
  });

  it('ajoute l’exigence à la partie choisie, la citation en preuve', () => {
    svc.extract.and.returnValue(of([
      { quote: TEXTE, reference: 'Art. 4', requirement: 'Déclarer tout EIG sous 48 h',
        nature: 'CONTRACTUAL', control: 'Procédure de vigilance' }
    ]));
    svc.addRequirement.and.returnValue(of({ id: 'ex-1' } as StakeholderRequirement));
    let rechargements = 0;
    component.registered.subscribe(() => (rechargements += 1));

    component.documentText = TEXTE;
    component.extract();
    component.extractTargetId = 'pi-1';
    component.add(component.extracted![0]);

    expect(svc.addRequirement).toHaveBeenCalledWith('pi-1', {
      requirement: 'Déclarer tout EIG sous 48 h',
      origin: 'Art. 4',
      nature: 'CONTRACTUAL',
      control: 'Procédure de vigilance',
      evidence: TEXTE,
      status: 'TO_ANALYSE'
    });
    expect(component.extracted![0].added).toBeTrue();
    expect(rechargements).toBe(1);
  });

  it('affiche toujours la citation : c’est elle qui rend l’exigence vérifiable', () => {
    svc.extract.and.returnValue(of([
      { quote: TEXTE, reference: null, requirement: 'Déclarer tout EIG sous 48 h',
        nature: null, control: null }
    ]));
    component.documentText = TEXTE;
    component.extract();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="citation"]')?.textContent).toContain(TEXTE);
  });

  // ---------- changements ----------

  it('exige les deux versions avant de comparer', () => {
    component.previousText = TEXTE;
    component.currentText = 'court';

    expect(component.detectBlocked).toBeTruthy();
    component.detect();
    expect(svc.changes).not.toHaveBeenCalled();
  });

  it('marque les seules exigences cochées de la partie choisie', () => {
    svc.changes.and.returnValue(of([
      { title: 'Délai ramené à 24 h', detail: null, source: null, impact: 'HIGH' }
    ]));
    svc.get.and.returnValue(of({
      id: 'pi-1',
      requirements: [
        { id: 'ex-1', requirement: 'Déclarer sous 48 h', status: 'APPLICABLE' } as StakeholderRequirement,
        { id: 'ex-2', requirement: 'Archiver cinq ans', status: 'CONTROLLED' } as StakeholderRequirement
      ]
    } as StakeholderSheet));
    svc.markToReassess.and.returnValue(of([
      { id: 'ex-1', requirement: 'Déclarer sous 48 h', status: 'TO_REASSESS' } as StakeholderRequirement
    ]));

    component.previousText = TEXTE;
    component.currentText = `${TEXTE} Modifié.`;
    component.detect();

    component.onReassessTarget('pi-1');
    expect(component.reassessRequirements.length).toBe(2);
    // Rien n'est marqué tant que rien n'est coché.
    expect(component.markBlocked).toBeTruthy();

    component.toggleRequirement('ex-1');
    expect(component.markBlocked).toBeNull();
    component.markToReassess();

    expect(svc.markToReassess).toHaveBeenCalledWith('pi-1', ['ex-1']);
    expect(component.reassessRequirements[0].status).toBe('TO_REASSESS');
    expect(component.reassessRequirements[1].status).toBe('CONTROLLED');
    expect(component.reassessSelection.size).toBe(0);
  });

  it('changer de partie prenante remet la sélection à zéro', () => {
    svc.get.and.returnValue(of({ id: 'pi-1', requirements: [] } as unknown as StakeholderSheet));

    component.onReassessTarget('pi-1');
    component.toggleRequirement('ex-1');
    expect(component.reassessSelection.size).toBe(1);

    component.onReassessTarget('pi-2');
    expect(component.reassessSelection.size).toBe(0);
  });

  it('aucun changement détecté n’est pas une erreur', () => {
    svc.changes.and.returnValue(of([]));
    component.previousText = TEXTE;
    component.currentText = `${TEXTE} Modifié.`;
    component.detect();
    fixture.detectChanges();

    expect(component.changeError).toBeNull();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="changements-vides"]')).not.toBeNull();
  });
});
