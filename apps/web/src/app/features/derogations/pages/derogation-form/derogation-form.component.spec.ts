import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { DerogationsService } from '../../derogations.service';
import { Derogation, DerogationStatus } from '../../derogations.types';
import { DerogationFormComponent } from './derogation-form.component';

const MOI = 'kc-paul';
const AUTRE = 'kc-sophie';

function dossier(statut: DerogationStatus, extra: Partial<Derogation> = {}): Derogation {
  return {
    id: 'd-1',
    reference: 'DER-2026-0001',
    status: statut,
    editable: statut === 'DRAFT' || statut === 'TO_AMEND',
    customerName: 'ACME',
    customerQualityContact: 'M. Dupont',
    customerOrderNo: 'CMD-77',
    partReference: 'P-1',
    partIndex: 'B',
    batchNumber: 'L-42',
    quantity: 120,
    requestDate: '2026-11-20',
    shipmentStage: 'TO_SHIP',
    deviationOrigin: 'PRODUCT',
    occurrence: 'FIRST',
    durationKind: 'TEMPORARY',
    durationDetail: null,
    lines: [{ position: 0, specification: 'Ø12 ±0,02', actualValue: 'Ø12,05', nonConformingQty: 8 }],
    justification: 'Aptitude à l\'emploi démontrée',
    rootCause: null,
    containment: null,
    correctiveAction: null,
    capaReference: null,
    attachmentsNote: null,
    managerAccountId: null,
    managerAccountName: null,
    signedByName: null,
    signaturePngBase64: null,
    signedAt: null,
    amendReason: null,
    customerEmail: null,
    sendComment: null,
    language: null,
    sentAt: null,
    customerDecision: 'PENDING',
    customerDecisionDate: null,
    createdByUserId: MOI,
    createdByName: 'Paul Girard',
    createdAt: '2026-11-20T09:00:00Z',
    updatedAt: '2026-11-20T09:00:00Z',
    ...extra
  };
}

describe('DerogationFormComponent', () => {

  let service: jasmine.SpyObj<DerogationsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let router: jasmine.SpyObj<Router>;
  let fixture: ComponentFixture<DerogationFormComponent>;
  let composant: DerogationFormComponent;

  async function monter(id: string | null, charge?: Derogation, moi = MOI): Promise<void> {
    service = jasmine.createSpyObj<DerogationsService>('DerogationsService', [
      'get', 'create', 'update', 'requestApproval', 'withdraw', 'sendBack',
      'approve', 'reopen', 'sendToCustomer', 'recordCustomerDecision', 'pdf'
    ]);
    if (charge) {
      service.get.and.returnValue(of(charge));
    }
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    // Remis a zero : plusieurs bancs remontent l ecran dans deux etats pour
    // comparer les droits, et reconfigurer un TestBed deja instancie echoue.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationFormComponent],
      imports: [
        SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
        // Comme le module du feature : le dossier porte deux dates, et sans
        // ces modules `mat-datepicker` n est pas un element connu.
        MatDatepickerModule, MatNativeDateModule
      ],
      providers: [
        // Le selecteur d'annuaire tire DirectoryService, qui tire HttpClient.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: DerogationsService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: moi, roles: [] }) } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => id } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DerogationFormComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /** Une fenêtre qui se referme sur `resultat`. */
  function fenetreRendant(resultat: unknown): void {
    dialog.open.and.returnValue(
      { afterClosed: () => of(resultat) } as MatDialogRef<unknown, unknown>);
  }

  // ---------- ouverture ----------

  it('sur « nouvelle », n\'appelle rien et offre une ligne d\'écart vide', async () => {
    await monter('nouvelle');
    expect(service.get).not.toHaveBeenCalled();
    expect(composant.neuf).toBeTrue();
    // Un tableau sans ligne ne dit pas qu'on peut en ajouter.
    expect(composant.lignes.length).toBe(1);
  });

  it('un brouillon vide s\'enregistre : AUCUN champ n\'est requis ici', async () => {
    await monter('nouvelle');
    service.create.and.returnValue(of(dossier('DRAFT')));

    composant.enregistrer();

    // Les six champs obligatoires ne sont exigés qu'à la demande de signature,
    // et c'est le serveur qui les contrôle. Les redire ici ferait deux règles
    // à tenir d'accord, et c'est toujours la copie qui dérive.
    expect(service.create).toHaveBeenCalled();
    const envoye = service.create.calls.mostRecent().args[0];
    expect(envoye.customerName).toBeNull();
    // Une ligne entièrement vide n'est PAS une ligne : la laisser passer
    // ferait figurer un écart sans contenu sur le document du client.
    expect(envoye.lines).toEqual([]);
  });

  it('après création, remplace l\'adresse par celle du dossier', async () => {
    await monter('nouvelle');
    service.create.and.returnValue(of(dossier('DRAFT')));

    composant.enregistrer();

    // Sans cela, un rechargement de la page rouvrirait un formulaire vide et
    // le dossier paraîtrait perdu.
    expect(router.navigate).toHaveBeenCalledWith(['/derogations', 'd-1'],
      { replaceUrl: true });
    expect(composant.neuf).toBeFalse();
  });

  it('charge la fiche et la rend telle que le serveur la décrit', async () => {
    await monter('d-1', dossier('DRAFT'));
    expect(composant.formulaire.controls.customerName.value).toBe('ACME');
    expect(composant.lignes.length).toBe(1);
    expect(composant.lignes.at(0).value.actualValue).toBe('Ø12,05');
  });

  it('un dossier non modifiable arrive en lecture seule, DIT PAR LE SERVEUR',
    async () => {
      await monter('d-1', dossier('PENDING_INTERNAL'));
      // `editable` vient du serveur : l'écran ne le déduit pas de l'état, sous
      // peine de deux tables de vérité à tenir d'accord.
      expect(composant.formulaire.disabled).toBeTrue();
      expect(composant.modifiable).toBeFalse();
    });

  it('un dossier sans ligne en montre une vide, pour pouvoir en ajouter', async () => {
    await monter('d-1', dossier('DRAFT', { lines: [] }));
    expect(composant.lignes.length).toBe(1);
  });

  // ---------- le motif du renvoi ----------

  it('le motif du renvoi se lit EN HAUT, avant le formulaire', async () => {
    await monter('d-1', dossier('TO_AMEND', { amendReason: 'La quantité ne correspond pas au lot.' }));
    fixture.detectChanges();

    const bandeau = hote().querySelector('[data-test="motif-renvoi-affiche"]');
    expect(bandeau?.textContent).toContain('La quantité ne correspond pas au lot.');
  });

  // ---------- qui peut quoi ----------

  it('l\'initiateur demande la validation depuis un brouillon ou un dossier à modifier',
    async () => {
      await monter('d-1', dossier('DRAFT'));
      expect(composant.peutDemanderLaValidation).toBeTrue();

      await monter('d-1', dossier('TO_AMEND'));
      expect(composant.peutDemanderLaValidation).toBeTrue();

      await monter('d-1', dossier('PENDING_INTERNAL'));
      expect(composant.peutDemanderLaValidation).toBeFalse();
    });

  it('un tiers ne demande rien, même sur un brouillon', async () => {
    await monter('d-1', dossier('DRAFT'), AUTRE);
    expect(composant.jeSuisLInitiateur).toBeFalse();
    expect(composant.peutDemanderLaValidation).toBeFalse();
    expect(composant.peutEnvoyer).toBeFalse();
  });

  it('SEUL LE MANAGER DÉSIGNÉ signe, et seulement en attente interne', async () => {
    await monter('d-1', dossier('PENDING_INTERNAL', { managerAccountId: MOI }));
    expect(composant.peutSigner).toBeTrue();

    await monter('d-1', dossier('PENDING_INTERNAL', { managerAccountId: AUTRE }));
    expect(composant.peutSigner).toBeFalse();

    await monter('d-1', dossier('QUALITY_APPROVED', { managerAccountId: MOI }));
    expect(composant.peutSigner).toBeFalse();
  });

  it('LE MANAGER NE REFUSE PAS : il signe, ou il renvoie — pas de bouton de refus',
    async () => {
      await monter('d-1', dossier('PENDING_INTERNAL', { managerAccountId: MOI }));
      fixture.detectChanges();

      // Refuser usurperait la décision du client, seul à pouvoir accorder la
      // dérogation. L'écran ne propose donc pas ce geste.
      expect(hote().querySelector('[data-test="signer"]')).not.toBeNull();
      expect(hote().querySelector('[data-test="renvoyer"]')).not.toBeNull();
      expect(hote().querySelector('[data-test="refuser"]')).toBeNull();
    });

  it('rouvrir est offert à l\'initiateur ET au manager qui a signé, pas à un tiers',
    async () => {
      await monter('d-1', dossier('QUALITY_APPROVED'), MOI);
      expect(composant.peutRouvrir).toBeTrue();

      await monter('d-1', dossier('QUALITY_APPROVED', { managerAccountId: MOI }), MOI);
      expect(composant.peutRouvrir).toBeTrue();

      await monter('d-1',
        dossier('QUALITY_APPROVED', { createdByUserId: AUTRE, managerAccountId: AUTRE }), MOI);
      expect(composant.peutRouvrir).toBeFalse();
    });

  it('UN DOSSIER PARTI NE SE ROUVRE PLUS', async () => {
    await monter('d-1', dossier('PENDING_CUSTOMER'));
    // Le client détient un document signé : le modifier en silence le
    // démentirait sans qu'il en sache rien.
    expect(composant.peutRouvrir).toBeFalse();
    expect(composant.peutEnvoyer).toBeFalse();
  });

  // ---------- les gestes ----------

  it('la demande de validation envoie l\'identifiant de COMPTE du manager', async () => {
    await monter('d-1', dossier('DRAFT'));
    service.requestApproval.and.returnValue(of(dossier('PENDING_INTERNAL')));

    composant.surChoixDuManager({
      id: 'interne-9', accountId: 'kc-manager', displayName: 'Sophie Marchand'
    } as never);
    composant.demanderLaValidation();

    expect(service.requestApproval).toHaveBeenCalledWith('d-1', 'kc-manager');
  });

  it('le manager deja designe revient avec la fiche', async () => {
    // Rouvrir un dossier le recharge. Le champ ne vivait que dans l ecran : il
    // repartait vide, et il fallait redesigner quelqu un que le serveur
    // connaissait deja. Le domaine est pourtant clair — « la signature est
    // effacee, le manager designe reste ».
    await monter('d-1', dossier('QUALITY_APPROVED', {
      managerAccountId: 'kc-manager', managerAccountName: 'Sophie Marchand'
    }));

    expect(composant.managerCtrl.value).toBe('kc-manager');
  });

  it('un dossier sans manager laisse le champ vide', async () => {
    await monter('d-1', dossier('DRAFT'));

    expect(composant.managerCtrl.value).toBeNull();
  });

  it('la date de la demande arrive pre-remplie au jour courant sur un dossier NEUF',
    async () => {
      // L aide du champ l annonce — « pre-remplie a l ouverture du dossier » —
      // et le domaine le confirme. Elle arrivait vide : la promesse demetait
      // l ecran.
      await monter(null);

      const posee = composant.formulaire.controls.requestDate.value!;
      const aujourdhui = new Date();

      expect(posee).withContext('aucune date posee').not.toBeNull();
      expect(posee.getFullYear()).toBe(aujourdhui.getFullYear());
      expect(posee.getMonth()).toBe(aujourdhui.getMonth());
      expect(posee.getDate()).toBe(aujourdhui.getDate());
    });

  it('un dossier qui EXISTE garde sa date, et non celle du jour', async () => {
    await monter('d-1', dossier('DRAFT', { requestDate: '2026-09-18' }));

    const posee = composant.formulaire.controls.requestDate.value!;

    expect(posee.getFullYear()).toBe(2026);
    expect(posee.getMonth()).toBe(8);
    expect(posee.getDate()).toBe(18);
  });

  it('sans manager choisi, la demande ne part pas', async () => {
    await monter('d-1', dossier('DRAFT'));
    composant.demanderLaValidation();
    expect(service.requestApproval).not.toHaveBeenCalled();
  });

  it('le renvoi transmet le motif saisi, et rien si la fenêtre est annulée',
    async () => {
      await monter('d-1', dossier('PENDING_INTERNAL', { managerAccountId: MOI }));
      service.sendBack.and.returnValue(of(dossier('TO_AMEND')));

      fenetreRendant('Quantité incohérente');
      composant.renvoyerPourCorrection();
      expect(service.sendBack).toHaveBeenCalledWith('d-1', 'Quantité incohérente');

      service.sendBack.calls.reset();
      fenetreRendant(undefined);
      composant.renvoyerPourCorrection();
      expect(service.sendBack).not.toHaveBeenCalled();
    });

  it('la signature transmet le nom ET le tracé', async () => {
    await monter('d-1', dossier('PENDING_INTERNAL', { managerAccountId: MOI }));
    service.approve.and.returnValue(of(dossier('QUALITY_APPROVED')));

    fenetreRendant({ signedByName: 'Sophie Marchand', signaturePngBase64: 'data:image/png;base64,AAA' });
    composant.signer();

    // Le nom seul ne distingue pas une signature d'une saisie par un tiers ;
    // le tracé seul ne dit pas qui a signé.
    expect(service.approve).toHaveBeenCalledWith('d-1', {
      signedByName: 'Sophie Marchand',
      signaturePngBase64: 'data:image/png;base64,AAA'
    });
  });

  it('l\'envoi au client transmet l\'adresse, le commentaire et LA LANGUE', async () => {
    await monter('d-1', dossier('QUALITY_APPROVED'));
    service.sendToCustomer.and.returnValue(of(dossier('PENDING_CUSTOMER')));

    fenetreRendant({ customerEmail: 'qualite@acme.test', comment: 'Urgence lot 42', language: 'EN' });
    composant.envoyerAuClient();

    // La langue est exigée : aucun document ne doit partir dans une langue que
    // personne n'a choisie.
    expect(service.sendToCustomer).toHaveBeenCalledWith('d-1', {
      customerEmail: 'qualite@acme.test', comment: 'Urgence lot 42', language: 'EN'
    });
  });

  it('la réponse du client exige la décision ET sa date', async () => {
    await monter('d-1', dossier('PENDING_CUSTOMER'));

    composant.enregistrerLaReponseDuClient();
    expect(service.recordCustomerDecision).not.toHaveBeenCalled();

    service.recordCustomerDecision.and.returnValue(of(dossier('ACCEPTED')));
    composant.reponseClient.setValue(
      { decision: 'ACCEPTED', decidedOn: new Date(2026, 10, 25) });
    composant.enregistrerLaReponseDuClient();

    // La date est celle où LE CLIENT a répondu, pas celle de la saisie : c'est
    // elle qui compte pour la validité de l'autorisation.
    expect(service.recordCustomerDecision).toHaveBeenCalledWith('d-1', {
      decision: 'ACCEPTED', decidedOn: '2026-11-25'
    });
  });

  it('« En attente » n\'est pas une réponse qu\'on puisse porter au dossier',
    async () => {
      await monter('d-1', dossier('PENDING_CUSTOMER'));
      expect(composant.decisions).not.toContain('PENDING');
      expect(composant.decisions).toEqual(['ACCEPTED', 'ACCEPTED_WITH_CONDITIONS', 'REFUSED']);
    });

  // ---------- la réponse du serveur fait foi ----------

  it('un geste réussi remplace l\'état de l\'écran par la fiche RENDUE', async () => {
    await monter('d-1', dossier('DRAFT'));
    service.requestApproval.and.returnValue(
      of(dossier('PENDING_INTERNAL', { managerAccountName: 'Sophie Marchand' })));

    composant.surChoixDuManager({ accountId: 'kc-manager' } as never);
    composant.demanderLaValidation();

    // Une seule réponse, pas de seconde lecture : relire ferait voir un
    // instant différent de celui que le serveur vient de décider.
    expect(service.get).toHaveBeenCalledTimes(1);
    expect(composant.dossier?.status).toBe('PENDING_INTERNAL');
    expect(composant.dossier?.managerAccountName).toBe('Sophie Marchand');
  });

  it('un geste refusé dit pourquoi et NE TOUCHE PAS l\'état affiché', async () => {
    await monter('d-1', dossier('DRAFT'));
    service.update.and.returnValue(throwError(() => ({
      status: 422, error: { detail: 'Six champs manquent.' }
    })));

    composant.enregistrer();

    expect(snack.open).toHaveBeenCalled();
    expect(composant.dossier?.status).toBe('DRAFT');
    expect(composant.envoiEnCours).toBeFalse();
  });

  it('un échec de chargement se dit sans laisser un formulaire trompeur', async () => {
    service = jasmine.createSpyObj<DerogationsService>('DerogationsService', ['get']);
    service.get.and.returnValue(throwError(() => ({ status: 404 })));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    // Remis a zero : plusieurs bancs remontent l ecran dans deux etats pour
    // comparer les droits, et reconfigurer un TestBed deja instancie echoue.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationFormComponent],
      imports: [
        SharedModule, UiModule, UserPickerModule, NoopAnimationsModule,
        // Comme le module du feature : le dossier porte deux dates, et sans
        // ces modules `mat-datepicker` n est pas un element connu.
        MatDatepickerModule, MatNativeDateModule
      ],
      providers: [
        // Le selecteur d'annuaire tire DirectoryService, qui tire HttpClient.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: DerogationsService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: MOI, roles: [] }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'd-9' } } } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DerogationFormComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();

    expect(composant.echec).toBeTrue();
    expect(composant.dossier).toBeUndefined();
    expect(snack.open).toHaveBeenCalled();
  });

  // ---------- lignes d'écart ----------

  it('retirer la dernière ligne en laisse une vide plutôt qu\'un tableau mort',
    async () => {
      await monter('d-1', dossier('DRAFT'));
      composant.retirerLaLigne(0);
      expect(composant.lignes.length).toBe(1);
      expect(composant.lignes.at(0).value.specification).toBe('');
    });

  it('une ligne partiellement remplie est envoyée, une ligne vide est écartée',
    async () => {
      await monter('nouvelle');
      service.create.and.returnValue(of(dossier('DRAFT')));

      composant.lignes.at(0).patchValue({ specification: 'Ø12 ±0,02' });
      composant.ajouterUneLigne();
      composant.ajouterUneLigne();
      composant.lignes.at(2).patchValue({ nonConformingQty: 3 });

      composant.enregistrer();

      const envoye = service.create.calls.mostRecent().args[0];
      // Trois lignes saisies, deux envoyées : celle du milieu est vide.
      expect(envoye.lines.length).toBe(2);
      expect(envoye.lines[0].specification).toBe('Ø12 ±0,02');
      expect(envoye.lines[1].nonConformingQty).toBe(3);
    });

  it('le texte est élagué, et le vide devient ABSENT et non chaîne vide',
    async () => {
      await monter('nouvelle');
      service.create.and.returnValue(of(dossier('DRAFT')));

      composant.formulaire.patchValue({ customerName: '  ACME  ', partReference: '   ' });
      composant.enregistrer();

      const envoye = service.create.calls.mostRecent().args[0];
      expect(envoye.customerName).toBe('ACME');
      // Une chaîne d'espaces n'est pas une référence : l'envoyer ferait
      // figurer un champ rempli sur le document du client.
      expect(envoye.partReference).toBeNull();
    });

  // ---------- le document ----------

  it('ouvre le document dans un onglet, et libère l\'adresse créée', async () => {
    await monter('d-1', dossier('QUALITY_APPROVED'));
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    service.pdf.and.returnValue(of(blob));
    spyOn(URL, 'createObjectURL').and.returnValue('blob:faux');
    const liberer = spyOn(URL, 'revokeObjectURL');
    spyOn(window, 'open');
    jasmine.clock().install();

    composant.ouvrirLeDocument();
    expect(window.open).toHaveBeenCalledWith('blob:faux', '_blank');

    jasmine.clock().tick(60_001);
    // Garder l'adresse fuirait une référence par consultation.
    expect(liberer).toHaveBeenCalledWith('blob:faux');
    jasmine.clock().uninstall();
  });

  // ---------- le format du fil ----------

  it('LE CALENDRIER ECRIT UN Date, et le serveur recoit aaaa-mm-jj',
    async () => {
      await monter('nouvelle');
      service.create.and.returnValue(of(dossier('DRAFT')));

      // C'est bien un objet que `mat-datepicker` depose dans le controle.
      // Envoye tel quel, il partait en instant ISO UTC : le serveur attend une
      // date civile et aurait rendu 400, ou recule d'un jour a l'est de
      // Greenwich -- une demande du 25 datee du 24 sur le document du client.
      composant.formulaire.patchValue({ requestDate: new Date(2026, 10, 25) });
      composant.enregistrer();

      expect(service.create.calls.mostRecent().args[0].requestDate)
        .toBe('2026-11-25');
    });

  it('une date de 23 h ne recule PAS d un jour', async () => {
    await monter('nouvelle');
    service.create.and.returnValue(of(dossier('DRAFT')));

    // 23 h 30 locale le 25 : `toISOString()` donnerait le 26 a l'ouest de
    // Greenwich. Les composantes locales, elles, ne bougent pas.
    composant.formulaire.patchValue({
      requestDate: new Date(2026, 10, 25, 23, 30)
    });
    composant.enregistrer();

    expect(service.create.calls.mostRecent().args[0].requestDate)
      .toBe('2026-11-25');
  });

  it('la fiche chargee remplit le calendrier avec un vrai Date local',
    async () => {
      await monter('d-1', dossier('DRAFT', { requestDate: '2026-11-20' }));

      const valeur = composant.formulaire.controls.requestDate.value;
      // Passer la chaine a `new Date(...)` la lirait en UTC, et le calendrier
      // pourrait afficher le 19.
      expect(valeur instanceof Date).toBeTrue();
      expect((valeur as Date).getFullYear()).toBe(2026);
      expect((valeur as Date).getMonth()).toBe(10);
      expect((valeur as Date).getDate()).toBe(20);
    });

  it('la reponse du client part aussi en date civile', async () => {
    await monter('d-1', dossier('PENDING_CUSTOMER'));
    service.recordCustomerDecision.and.returnValue(of(dossier('ACCEPTED')));

    composant.reponseClient.setValue(
      { decision: 'REFUSED', decidedOn: new Date(2026, 11, 3) });
    composant.enregistrerLaReponseDuClient();

    expect(service.recordCustomerDecision).toHaveBeenCalledWith('d-1', {
      decision: 'REFUSED', decidedOn: '2026-12-03'
    });
  });

  it('rouvrir demande confirmation, et ne part que sur un oui', async () => {
    await monter('d-1', dossier('QUALITY_APPROVED'));
    service.reopen.and.returnValue(of(dossier('DRAFT')));

    fenetreRendant(false);
    composant.rouvrir();
    // Annuler la signature d'un manager n'est pas une modification de plus.
    expect(service.reopen).not.toHaveBeenCalled();

    fenetreRendant(true);
    composant.rouvrir();
    expect(service.reopen).toHaveBeenCalledWith('d-1');
    expect(composant.dossier?.status).toBe('DRAFT');
  });

  it('la signature acquise s\'affiche avec son tracé et son auteur', async () => {
    await monter('d-1', dossier('QUALITY_APPROVED', {
      signedByName: 'Sophie Marchand',
      signaturePngBase64: 'AAA',
      signedAt: '2026-11-21T14:30:00Z'
    }));
    fixture.detectChanges();

    const bloc = hote().querySelector('[data-test="signature-acquise"]');
    expect(bloc?.textContent).toContain('Sophie Marchand');
    const image = bloc?.querySelector('img');
    expect(image?.getAttribute('src')).toBe('data:image/png;base64,AAA');
  });
});
