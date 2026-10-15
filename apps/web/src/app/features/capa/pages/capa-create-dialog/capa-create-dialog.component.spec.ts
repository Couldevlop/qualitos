import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryPage, DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerComponent } from '../../../../shared/user-picker/user-picker.component';
import { CapaCreateDialogModule } from '../../capa-create-dialog.module';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, OpenCapaRequest } from '../../capa.types';
import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened, localIsoDate
} from './capa-create-dialog.component';

/**
 * LA fenêtre de création d'une CAPA (ADR 0088) : la même depuis la liste, une
 * NC, un constat, un risque ou la matrice SMI. Elle crée le dossier ET son
 * action ; le responsable, choisi dans l'annuaire en lecture restreinte
 * (ADR 0091) — ouvert à tout membre —, pilote le dossier. Soi-même par défaut ;
 * le nom recopié est celui de l'annuaire, jamais d'identifiant à l'écran.
 */
describe('CapaCreateDialogComponent — fenêtre unique', () => {

  const MOI: DirectoryUser = { id: 'i-moi', accountId: 'kc-moi', displayName: 'Moi Même', email: 'moi@client.test', active: true };
  const AMINA: DirectoryUser = { id: 'i-amina', accountId: 'kc-amina', displayName: 'Amina Dridi', email: 'amina.dridi@client.test', active: true };
  const DIALLO: DirectoryUser = { id: 'i-diallo', accountId: 'kc-diallo', displayName: 'Awa Diallo', email: 'a.diallo@client.test', active: true };
  const ANNUAIRE = [MOI, DIALLO, AMINA];
  const page = (content: DirectoryUser[]): DirectoryPage =>
    ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });
  const DEMAIN = localIsoDate(new Date(Date.now() + 86_400_000));
  const HIER = localIsoDate(new Date(Date.now() - 86_400_000));

  let fixture: ComponentFixture<CapaCreateDialogComponent>;
  let component: CapaCreateDialogComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let ref: jasmine.SpyObj<MatDialogRef<CapaCreateDialogComponent, CapaOpened>>;
  let directory: jasmine.SpyObj<DirectoryService>;
  let session: AuthUser | null;

  const ouvert = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: 'c1', tenantId: 't1', title: 'Recalibrer', type: 'CORRECTIVE', criticity: 'MEDIUM',
    status: 'OPEN', sourceType: 'INTERNAL', ownerId: 'kc-moi',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', actions: [], ...over
  });

  async function setup(data: CapaCreateDialogData | null = null): Promise<void> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SharedModule, UiModule, CapaCreateDialogModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: DirectoryService, useValue: directory },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  const picker = (): UserPickerComponent =>
    fixture.debugElement.query(By.directive(UserPickerComponent)).componentInstance as UserPickerComponent;

  /** Choisir une personne dans le sélecteur, comme le ferait un clic dans sa liste. */
  function choisir(user: DirectoryUser): void {
    picker().onSelected({ option: { value: user } } as never);
    fixture.detectChanges();
  }

  function remplir(): void {
    // « Non » est une réponse : sans elle, rien ne part (ADR 0073).
    component.form.patchValue({ title: '  Recalibrer la sonde  ', description: '  ', dueDate: DEMAIN,
      verificationRequired: false });
  }

  beforeEach(() => {
    capa = jasmine.createSpyObj<CapaService>('CapaService', ['openCase', 'addAction', 'uploadEvidence']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    ref = jasmine.createSpyObj<MatDialogRef<CapaCreateDialogComponent, CapaOpened>>('MatDialogRef', ['close']);
    session = { userId: 'kc-moi', tenantId: 't1', displayName: 'Moi Même', roles: ['quality_manager'] };
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.callFake((q: string) => of(page(ANNUAIRE.filter(u =>
      `${u.displayName} ${u.email}`.toLowerCase().includes((q ?? '').toLowerCase())))));
    directory.getByAccount.and.callFake(id => ANNUAIRE.some(u => u.accountId === id)
      ? of(ANNUAIRE.find(u => u.accountId === id)!)
      : throwError(() => ({ status: 404 })));
  });

  // --- les champs ------------------------------------------------------------

  it('ne propose que corrective ou préventive : l\'endiguement seul ne se clôturerait jamais', async () => {
    await setup();
    expect(component.natures.map(n => n.value)).toEqual(['CORRECTIVE', 'PREVENTIVE']);
    expect(component.form.controls.type.value).toBe('CORRECTIVE');
    expect(component.criticities.map(c => c.value)).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
    expect(component.criticities.every(c => !!c.label)).toBeTrue();
  });

  it('en création libre, la source se choisit parmi celles qu\'on peut saisir à la main', async () => {
    await setup();
    expect(component.sourceLocked).toBeFalse();
    expect(component.sourceTypes.map(s => s.value))
      .toEqual(['NON_CONFORMITY', 'AUDIT', 'COMPLAINT', 'INTERNAL', 'IOT_ALERT', 'OTHER']);
    expect(hote().querySelector('[data-test="capa-source"]')).not.toBeNull();
    expect(hote().querySelector('[data-test="source-verrouillee"]')).toBeNull();
    expect(component.dialogTitle).toBe('Nouvelle CAPA');
    expect(component.submitLabel).toBe('Créer la CAPA');
  });

  it('le responsable se choisit dans l\'annuaire, moi par défaut, affiché par mon nom', async () => {
    await setup();
    expect(component.form.controls.assigneeId.value).toBe('kc-moi');
    expect(picker().selected?.displayName).toBe('Moi Même');
    expect(component.matchingOwner).toBeFalse();
    // Plus de message « pas accessible avec tes droits » : l'annuaire est ouvert à tous.
    expect(hote().querySelector('[data-test="annuaire-indisponible"]')).toBeNull();
    expect(directory.search).not.toHaveBeenCalled();
  });

  it('refuse une échéance passée, et l\'exige', async () => {
    await setup();
    component.form.controls.dueDate.setValue('');
    expect(component.form.controls.dueDate.hasError('required')).toBeTrue();
    component.form.controls.dueDate.setValue(HIER);
    expect(component.form.controls.dueDate.hasError('past')).toBeTrue();
    component.form.controls.dueDate.setValue(component.today);
    expect(component.form.controls.dueDate.valid).toBeTrue();
  });

  it('refuse un intitulé fait d\'espaces', async () => {
    await setup();
    component.form.patchValue({ title: '   ', dueDate: DEMAIN });
    component.submit();
    expect(capa.openCase).not.toHaveBeenCalled();
    expect(component.form.controls.title.hasError('required')).toBeTrue();
  });

  it('ne garde pas une échéance proposée déjà passée', async () => {
    await setup({ prefill: { dueDate: HIER } });
    expect(component.form.controls.dueDate.value).toBe('');
  });

  // --- le responsable par défaut ------------------------------------------

  it('par défaut, me désigne avec mon nom de session', async () => {
    await setup();
    capa.openCase.and.returnValue(of(ouvert()));
    remplir();
    component.submit();
    expect(capa.openCase).toHaveBeenCalledWith(jasmine.objectContaining({ ownerId: 'kc-moi', ownerName: 'Moi Même' }));
  });

  it('sans session, ne part pas sans responsable', async () => {
    session = null;
    await setup();
    remplir();
    component.submit();
    expect(capa.openCase).not.toHaveBeenCalled();
    expect(component.form.controls.assigneeId.hasError('required')).toBeTrue();
  });

  it('vider le responsable bloque l\'envoi', async () => {
    await setup();
    remplir();
    picker().clear();
    fixture.detectChanges();
    component.submit();
    expect(capa.openCase).not.toHaveBeenCalled();
  });

  // --- création libre -------------------------------------------------------

  it('crée le dossier et son action en une écriture, pilote nommé', async () => {
    await setup();
    capa.openCase.and.returnValue(of(ouvert()));
    remplir();
    choisir(AMINA);
    component.form.patchValue({ type: 'PREVENTIVE', criticity: 'HIGH',
      sourceType: 'COMPLAINT', sourceRef: ' REC-12 ' });

    component.submit();

    const envoye = capa.openCase.calls.mostRecent().args[0] as OpenCapaRequest;
    expect(envoye).toEqual({
      title: 'Recalibrer la sonde', description: undefined, type: 'PREVENTIVE', criticity: 'HIGH',
      // Le NOM de l'annuaire, et non l'adresse.
      ownerId: 'kc-amina', ownerName: 'Amina Dridi', dueDate: DEMAIN,
      sourceType: 'COMPLAINT', sourceRef: 'REC-12', verificationRequired: false
    });
    expect(ref.close).toHaveBeenCalledWith({ capaId: 'c1' });
    expect(snack.open.calls.mostRecent().args[0]).toBe('CAPA créée.');
    expect(component.submitting).toBeFalse();
  });

  it('dépose les pièces jointes une fois le dossier créé, et dit ce qui est parti', async () => {
    await setup();
    capa.openCase.and.returnValue(of(ouvert()));
    capa.uploadEvidence.and.returnValues(
      of({ id: 'e1', capaId: 'c1', contentType: 'application/pdf', sizeBytes: 1, createdAt: '' }),
      throwError(() => new HttpErrorResponse({ status: 413 })));
    const fichiers = [new File(['a'], 'a.pdf', { type: 'application/pdf' }), new File(['b'], 'b.pdf')];
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: fichiers });
    component.onFilesSelected({ target: input } as unknown as Event);
    remplir();

    component.submit();

    expect(capa.uploadEvidence).toHaveBeenCalledTimes(2);
    expect(capa.uploadEvidence.calls.argsFor(0)[0]).toBe('c1');
    expect(component.uploaded).toBe(1);
    expect(snack.open.calls.mostRecent().args[0]).toContain('pas pu être déposées');
    expect(ref.close).toHaveBeenCalledWith({ capaId: 'c1' });
  });

  it('annonce le dépôt complet quand toutes les pièces sont parties', async () => {
    await setup();
    capa.openCase.and.returnValue(of(ouvert()));
    capa.uploadEvidence.and.returnValue(of({ id: 'e1', capaId: 'c1', contentType: 'a', sizeBytes: 1, createdAt: '' }));
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['a'], 'a.pdf')] });
    component.onFilesSelected({ target: input } as unknown as Event);
    remplir();
    component.submit();
    expect(snack.open.calls.mostRecent().args[0]).toBe('CAPA créée, pièces jointes déposées.');
  });

  it('refuse d\'emblée une pièce trop lourde, et plus de dix pièces', async () => {
    await setup();
    const lourd = new File(['x'], 'lourd.pdf');
    Object.defineProperty(lourd, 'size', { value: 11 * 1024 * 1024 });
    const legers = Array.from({ length: 11 }, (_, i) => new File(['x'], `p${i}.pdf`));
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [lourd, ...legers] });

    component.onFilesSelected({ target: input } as unknown as Event);

    expect(component.attachments.length).toBe(10);
    expect(component.attachments.some(f => f.name === 'lourd.pdf')).toBeFalse();
    component.removeAttachment(component.attachments[0]);
    expect(component.attachments.length).toBe(9);
    expect(component.formatSize(2 * 1024 * 1024)).toBe('2.0 Mo');
    expect(component.formatSize(10)).toBe('1 Ko');
    expect(component.removeAttachmentAria('a.pdf')).toContain('a.pdf');
  });

  it('garde la fenêtre ouverte et le dit quand le serveur refuse', async () => {
    await setup();
    capa.openCase.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));
    remplir();
    component.submit();
    expect(ref.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ne part pas deux fois', async () => {
    await setup();
    remplir();
    component.submitting = true;
    component.submit();
    expect(capa.openCase).not.toHaveBeenCalled();
  });

  it('se referme sans rien créer à l\'annulation', async () => {
    await setup();
    component.cancel();
    expect(ref.close).toHaveBeenCalledWith();
  });

  // --- depuis une section : source verrouillée ------------------------------

  it('depuis une section, affiche la source verrouillée et passe par la route de l\'écran d\'origine', async () => {
    const open = jasmine.createSpy('open').and.callFake((): Observable<CapaOpened> => of({ capaId: 'c-nc' }));
    await setup({
      source: { type: 'NON_CONFORMITY', reference: 'NC-2026-0001' },
      prefill: { title: 'Bavure poste 20', criticity: 'HIGH' },
      open
    });

    const bandeau = hote().querySelector('[data-test="source-verrouillee"]');
    expect(bandeau?.textContent).toContain('Non-conformité');
    expect(bandeau?.textContent).toContain('NC-2026-0001');
    expect(hote().querySelector('[data-test="capa-source"]')).toBeNull();
    expect(component.form.controls.title.value).toBe('Bavure poste 20');
    expect(component.form.controls.criticity.value).toBe('HIGH');

    component.form.patchValue({ dueDate: DEMAIN, verificationRequired: false });
    component.submit();

    const envoye = open.calls.mostRecent().args[0] as OpenCapaRequest;
    // La source n'est pas envoyée : c'est le serveur qui la fixe.
    expect(envoye.sourceType).toBeUndefined();
    expect(envoye.sourceRef).toBeUndefined();
    expect(capa.openCase).not.toHaveBeenCalled();
    expect(ref.close).toHaveBeenCalledWith({ capaId: 'c-nc' });
  });

  it('depuis un risque : criticité de la cotation, propriétaire proposé s\'il est EXACTEMENT dans l\'annuaire', async () => {
    const open = jasmine.createSpy('open').and.returnValue(of({ capaId: 'c-r' }));
    await setup({
      source: { type: 'RISK', reference: 'R-001' }, lockCriticity: true,
      prefill: { criticity: 'CRITICAL', ownerHint: 'awa diallo' },
      open
    });

    expect(directory.search).toHaveBeenCalledWith('awa diallo');
    expect(component.form.controls.criticity.disabled).toBeTrue();
    expect(component.form.controls.criticity.value).toBe('CRITICAL');
    expect(component.form.controls.assigneeId.value).toBe('kc-diallo');
    expect(component.unmatchedOwnerHint).toBeNull();
    expect(component.sourceLabel).toBe('Risque');

    component.form.patchValue({ title: 'Dérive soudure', dueDate: DEMAIN, verificationRequired: false });
    component.submit();
    expect(open).toHaveBeenCalledWith(jasmine.objectContaining({ ownerId: 'kc-diallo', ownerName: 'Awa Diallo' }));
  });

  it('depuis un risque au propriétaire choisi dans l\'annuaire, le propose sans le chercher par son nom', async () => {
    const open = jasmine.createSpy('open').and.returnValue(of({ capaId: 'c-r' }));
    await setup({
      source: { type: 'RISK', reference: 'R-001' },
      prefill: { ownerHint: 'Awa D.', ownerAccountId: 'kc-diallo' }, open
    });

    expect(directory.search).not.toHaveBeenCalledWith('Awa D.');
    expect(component.form.controls.assigneeId.value).toBe('kc-diallo');
    expect(component.unmatchedOwnerHint).toBeNull();

    component.form.patchValue({ title: 'Dérive soudure', dueDate: DEMAIN, verificationRequired: false });
    component.submit();
    expect(open).toHaveBeenCalledWith(jasmine.objectContaining({ ownerId: 'kc-diallo', ownerName: 'Awa D.' }));
  });

  it('depuis un risque, rapproche aussi le propriétaire par son adresse', async () => {
    await setup({
      source: { type: 'RISK', reference: 'R-001' },
      prefill: { ownerHint: 'A.DIALLO@client.test' }, open: () => of({ capaId: 'c-r' })
    });
    expect(component.form.controls.assigneeId.value).toBe('kc-diallo');
  });

  it('depuis un risque, un nom seulement approchant ne désigne personne', async () => {
    await setup({
      source: { type: 'RISK', reference: 'R-001' },
      prefill: { ownerHint: 'Diallo' }, open: () => of({ capaId: 'c-r' })
    });
    expect(component.form.controls.assigneeId.value).toBe('kc-moi');
    expect(component.unmatchedOwnerHint).toBe('Diallo');
  });

  it('depuis un risque, l\'annuaire indisponible ne bloque pas : on me propose', async () => {
    directory.search.and.returnValue(throwError(() => ({ status: 0 })));
    await setup({
      source: { type: 'RISK', reference: 'R-001' },
      prefill: { ownerHint: 'Awa Diallo' }, open: () => of({ capaId: 'c-r' })
    });
    expect(component.matchingOwner).toBeFalse();
    expect(component.form.controls.assigneeId.value).toBe('kc-moi');
  });

  it('depuis un risque dont le propriétaire est introuvable, le dit et me propose', async () => {
    await setup({
      source: { type: 'RISK', reference: 'R-001' }, lockCriticity: true,
      prefill: { ownerHint: 'Service Qualité' }, open: () => of({ capaId: 'c-r' })
    });
    fixture.detectChanges();
    expect(component.form.controls.assigneeId.value).toBe('kc-moi');
    expect(component.unmatchedOwnerHint).toBe('Service Qualité');
    expect(hote().querySelector('[data-test="proprietaire-non-trouve"]')?.textContent).toContain('Service Qualité');
  });

  // --- définir l'action d'un dossier existant -------------------------------

  it('en mode « définir l\'action », enregistre l\'action du dossier, son responsable devenant pilote', async () => {
    await setup({ mode: 'define-action', caseId: 'c9', prefill: { title: 'Dérive SPC', type: 'PREVENTIVE' } });
    capa.addAction.and.returnValue(of({ id: 'a1', capaId: 'c9', title: 'Dérive SPC', status: 'PENDING', actionType: 'PREVENTIVE' }));

    expect(component.dialogTitle).toBe('Définir l\'action');
    expect(component.submitLabel).toBe('Enregistrer l\'action');
    expect(hote().querySelector('[data-test="capa-criticite"]')).toBeNull();
    expect(hote().querySelector('.attachments')).toBeNull();
    // Le dossier existe : sa vérification se règle dans « Modifier », pas ici.
    expect(hote().querySelector('[data-test="bloc-verification"]')).toBeNull();
    expect(component.form.controls.verificationRequired.hasError('required')).toBeFalse();

    choisir(AMINA);
    component.form.patchValue({ dueDate: DEMAIN });
    component.submit();

    expect(capa.addAction).toHaveBeenCalledWith('c9', {
      title: 'Dérive SPC', description: undefined, actionType: 'PREVENTIVE',
      assigneeId: 'kc-amina', assigneeName: 'Amina Dridi', dueDate: DEMAIN
    });
    expect(capa.openCase).not.toHaveBeenCalled();
    expect(ref.close).toHaveBeenCalledWith({ capaId: 'c9' });
    expect(snack.open.calls.mostRecent().args[0]).toBe('Action définie.');
  });

  // --- la vérification d'efficacité, décidée à l'ouverture (ADR 0073) --------

  describe('vérification d\'efficacité', () => {
    const CONTROLE: DirectoryUser = {
      id: 'i-ctrl', accountId: 'kc-controle', displayName: 'Claire Contrôle', email: 'controle@client.test', active: true
    };

    /** Un tour de macrotâche : `deferredView` ne livre pas avant. */
    const macrotache = () => new Promise<void>(r => setTimeout(r, 0));

    async function repondre(exigee: boolean): Promise<void> {
      component.form.controls.verificationRequired.setValue(exigee);
      await macrotache();
      fixture.detectChanges();
    }

    /** Le sélecteur du vérificateur : le second, après celui du responsable. */
    const pickerVerificateur = (): UserPickerComponent =>
      fixture.debugElement.queryAll(By.directive(UserPickerComponent))[1].componentInstance as UserPickerComponent;

    function remplirSansDecision(): void {
      component.form.patchValue({ title: 'Recalibrer la sonde', dueDate: DEMAIN });
    }

    it('pose la question en création libre, sans réponse pré-cochée', async () => {
      await setup();
      await macrotache();
      fixture.detectChanges();

      expect(hote().querySelector('[data-test="bloc-verification"]')).not.toBeNull();
      expect(hote().querySelector('[data-test="verification-obligatoire"]')).not.toBeNull();
      const choix = hote().querySelector('[data-test="verification-exigee"]')!;
      expect(choix.querySelectorAll('mat-radio-button').length).toBe(2);
      expect(choix.getAttribute('aria-required')).toBe('true');
      expect(component.form.controls.verificationRequired.value).toBeNull();
      // Rien de demandé tant que rien n'est répondu.
      expect(hote().querySelector('[data-test="verification-qui"]')).toBeNull();
      expect(hote().querySelector('[data-test="verification-consignes"]')).toBeNull();
    });

    it('la pose aussi depuis une non-conformité, un constat ou un risque', async () => {
      for (const source of [
        { type: 'NON_CONFORMITY' as const, reference: 'NC-2026-0001' },
        { type: 'AUDIT' as const, reference: 'AUD-2026-0003 / constat 2' },
        { type: 'RISK' as const, reference: 'R-001' }
      ]) {
        await setup({ source, open: () => of({ capaId: 'c' }) });
        expect(hote().querySelector('[data-test="bloc-verification"]'))
          .withContext(source.type).not.toBeNull();
        expect(component.form.controls.verificationRequired.hasError('required'))
          .withContext(source.type).toBeTrue();
      }
    });

    it('exige une réponse avant tout envoi', async () => {
      await setup();
      remplirSansDecision();

      expect(component.form.controls.verificationRequired.hasError('required')).toBeTrue();
      expect(component.form.invalid).toBeTrue();
      component.submit();
      expect(capa.openCase).not.toHaveBeenCalled();
    });

    it('répondre oui demande à qui, et quoi vérifier ; le vérificateur devient obligatoire', async () => {
      await setup();
      remplirSansDecision();
      await repondre(true);

      expect(hote().querySelector('[data-test="verification-qui"]')).not.toBeNull();
      expect(hote().querySelector('[data-test="verification-consignes"]')).not.toBeNull();
      expect(component.form.controls.verificationAssigneeId.hasError('required')).toBeTrue();
      // L'erreur attend que le champ ait été touché : rien n'est reproché d'emblée.
      expect(hote().querySelector('[data-test="verification-qui-requis"]')).toBeNull();

      component.submit();
      expect(capa.openCase).not.toHaveBeenCalled();
      fixture.detectChanges();
      expect(hote().querySelector('[data-test="verification-qui-requis"]')).not.toBeNull();
    });

    it('envoie la décision avec le compte ET le nom d\'annuaire du vérificateur, et les consignes', async () => {
      await setup();
      capa.openCase.and.returnValue(of(ouvert()));
      remplirSansDecision();
      await repondre(true);
      pickerVerificateur().onSelected({ option: { value: CONTROLE } } as never);
      component.form.controls.verificationInstructions.setValue('  Relever 20 pièces  ');
      fixture.detectChanges();

      component.submit();

      expect(capa.openCase).toHaveBeenCalledWith(jasmine.objectContaining({
        verificationRequired: true,
        verificationAssigneeId: 'kc-controle',
        // Le NOM, pas l'adresse : recopié pour rester lisible.
        verificationAssigneeName: 'Claire Contrôle',
        verificationInstructions: 'Relever 20 pièces'
      }));
    });

    it('un « oui » sans consignes n\'envoie pas de consignes vides, ni de nom inventé', async () => {
      await setup();
      capa.openCase.and.returnValue(of(ouvert()));
      remplirSansDecision();
      await repondre(true);
      component.form.controls.verificationAssigneeId.setValue('kc-inconnu');

      component.submit();

      const envoye = capa.openCase.calls.mostRecent().args[0] as OpenCapaRequest;
      expect(envoye.verificationAssigneeId).toBe('kc-inconnu');
      expect(envoye.verificationAssigneeName).toBeUndefined();
      expect(envoye.verificationInstructions).toBeUndefined();
    });

    it('revenir à « non » efface le vérificateur et les consignes, et n\'envoie que la décision', async () => {
      const open = jasmine.createSpy('open').and.returnValue(of({ capaId: 'c-nc' }));
      await setup({ source: { type: 'NON_CONFORMITY', reference: 'NC-2026-0001' }, open });
      remplirSansDecision();
      await repondre(true);
      component.form.controls.verificationAssigneeId.setValue('kc-controle');
      component.onVerifier(CONTROLE);
      component.form.controls.verificationInstructions.setValue('Relever');

      await repondre(false);

      expect(hote().querySelector('[data-test="verification-qui"]')).toBeNull();
      expect(component.form.controls.verificationAssigneeId.value).toBeNull();
      expect(component.form.controls.verificationInstructions.value).toBe('');
      expect(component.form.controls.verificationAssigneeId.hasError('required')).toBeFalse();

      component.submit();
      const envoye = open.calls.mostRecent().args[0] as OpenCapaRequest;
      expect(envoye.verificationRequired).toBeFalse();
      expect(envoye.verificationAssigneeId).toBeUndefined();
      expect(envoye.verificationAssigneeName).toBeUndefined();
      expect(envoye.verificationInstructions).toBeUndefined();
    });

    it('le vérificateur retiré du sélecteur ne laisse pas de nom derrière lui', async () => {
      await setup();
      component.onVerifier(CONTROLE);
      component.onVerifier(null);
      capa.openCase.and.returnValue(of(ouvert()));
      remplirSansDecision();
      await repondre(true);
      component.form.controls.verificationAssigneeId.setValue('kc-controle');

      component.submit();

      expect((capa.openCase.calls.mostRecent().args[0] as OpenCapaRequest).verificationAssigneeName).toBeUndefined();
    });
  });
});

describe('localIsoDate', () => {
  it('formate au fuseau du navigateur', () => {
    expect(localIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
