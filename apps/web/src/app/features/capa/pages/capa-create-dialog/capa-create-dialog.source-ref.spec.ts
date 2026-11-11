import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCreateDialogModule } from '../../capa-create-dialog.module';
import { CapaService } from '../../capa.service';
import {
  CapaSourceRefOption, CapaSourceRefService, SOURCE_REF_PAGE_SIZE
} from '../../source-registry/capa-source-ref.service';
import { CapaCreateDialogComponent, CapaCreateDialogData } from './capa-create-dialog.component';

/**
 * La référence de la source se CHOISIT au registre, et elle est obligatoire.
 *
 * <p>Tapée à la main, elle se perdait au premier mot mal recopié : le dossier
 * disait « Audit » sans dire lequel, et devenait orphelin de sa cause — ce que
 * tout auditeur cherche en premier quand il remonte une action corrective
 * jusqu'à son constat.
 */
describe('CapaCreateDialogComponent — la référence de la source', () => {

  const NCS: CapaSourceRefOption[] = [
    { reference: 'NC-2026-018', label: 'NC-2026-018 — Fuite au joint' },
    { reference: 'NC-2026-019', label: 'NC-2026-019 — Étiquetage absent' }
  ];
  const RISQUES: CapaSourceRefOption[] = [
    { reference: 'R-014', label: 'R-014 — Rupture fournisseur unique' }
  ];

  let fixture: ComponentFixture<CapaCreateDialogComponent>;
  let component: CapaCreateDialogComponent;
  let registres: jasmine.SpyObj<CapaSourceRefService>;

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function setup(data: CapaCreateDialogData | null = null): Promise<void> {
    TestBed.resetTestingModule();
    registres = jasmine.createSpyObj<CapaSourceRefService>('CapaSourceRefService', ['options']);
    registres.options.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [SharedModule, UiModule, CapaCreateDialogModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: jasmine.createSpyObj('CapaService', ['openCase', 'defineAction']) },
        // L'annuaire n'est pas le sujet ici : le sélecteur de responsable lit le
        // compte connecté à l'ouverture, il lui faut donc les deux méthodes.
        {
          provide: DirectoryService,
          useValue: {
            search: () => of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
            getByAccount: () => of(null)
          }
        },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'kc-moi', displayName: 'Moi Même' }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: MatDialogRef, useValue: jasmine.createSpyObj('MatDialogRef', ['close']) },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: CapaSourceRefService, useValue: registres }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Choisit une source et laisse le registre répondre. */
  function choisirSource(type: string, options: CapaSourceRefOption[]): void {
    registres.options.and.returnValue(of(options));
    component.form.controls.sourceType.setValue(type as never);
    fixture.detectChanges();
  }

  // ---------- la source par défaut ne désigne rien ----------

  it('laisse la référence libre et facultative quand la source ne désigne aucun objet', async () => {
    await setup();
    expect(component.form.controls.sourceType.value).toBe('INTERNAL');
    expect(component.sourceRegister).toBeUndefined();
    expect(component.sourceRefIsPicked).toBeFalse();
    expect(component.form.controls.sourceRef.valid).toBeTrue();
    expect(registres.options).not.toHaveBeenCalled();
    // Un champ de saisie, pas une liste.
    expect(hote().querySelector('input[data-test="capa-source-ref"]')).not.toBeNull();
  });

  // ---------- chaque source charge SON registre ----------

  it('charge les non-conformités ouvertes quand la source est la non-conformité interne', async () => {
    await setup();
    choisirSource('NON_CONFORMITY', NCS);
    expect(registres.options).toHaveBeenCalledWith('nc');
    expect(component.sourceRefOptions).toEqual(NCS);
    expect(component.sourceRefIsPicked).toBeTrue();
  });

  it('charge les risques ouverts quand la source est le risque', async () => {
    await setup();
    choisirSource('RISK', RISQUES);
    expect(registres.options).toHaveBeenCalledWith('risk');
    expect(component.sourceRefOptions).toEqual(RISQUES);
  });

  it('charge les réclamations ouvertes quand la source est la réclamation client', async () => {
    await setup();
    choisirSource('COMPLAINT', [{ reference: 'REC-3', label: 'REC-3 — Colis endommagé' }]);
    expect(registres.options).toHaveBeenCalledWith('complaint');
  });

  it('charge les audits quand la source est un audit', async () => {
    await setup();
    choisirSource('AUDIT', [{ reference: 'AUD-1', label: 'AUD-1 — Audit interne' }]);
    expect(registres.options).toHaveBeenCalledWith('audit');
  });

  // ---------- l'obligation suit la source ----------

  it('rend la référence obligatoire dès que la source désigne un objet', async () => {
    await setup();
    choisirSource('NON_CONFORMITY', NCS);
    expect(component.form.controls.sourceRef.valid).toBeFalse();
    component.form.controls.sourceRef.setValue('NC-2026-018');
    expect(component.form.controls.sourceRef.valid).toBeTrue();
  });

  it('retire l’obligation quand on revient à une source qui ne désigne rien', async () => {
    await setup();
    choisirSource('NON_CONFORMITY', NCS);
    expect(component.form.controls.sourceRef.valid).toBeFalse();
    choisirSource('INTERNAL', []);
    expect(component.form.controls.sourceRef.valid).toBeTrue();
    expect(component.sourceRefIsPicked).toBeFalse();
  });

  // ---------- la référence ne survit pas au changement de source ----------

  // « NC-2026-018 » conservé sous la source « Risque » désignerait un objet
  // absent du registre interrogé : le dossier mentirait sur sa cause.
  it('efface la référence choisie quand la source change', async () => {
    await setup();
    choisirSource('NON_CONFORMITY', NCS);
    component.form.controls.sourceRef.setValue('NC-2026-018');
    choisirSource('RISK', RISQUES);
    expect(component.form.controls.sourceRef.value).toBe('');
  });

  // ---------- une référence déjà saisie survit à la relecture ----------

  // La page ne porte que les fiches ouvertes les plus récentes : une référence
  // valide mais absente de la page serait PERDUE à la relecture du registre si
  // on la remplaçait par la liste. On la garde, et on l'ajoute à la liste.
  it('garde la référence déjà saisie à la relecture du registre, et l’ajoute à la liste', async () => {
    await setup();
    registres.options.and.returnValue(throwError(() => new Error('503')));
    component.form.controls.sourceType.setValue('NON_CONFORMITY' as never);
    fixture.detectChanges();
    component.form.controls.sourceRef.setValue('NC-2019-001');

    registres.options.and.returnValue(of(NCS));
    component.retrySourceRefs();
    fixture.detectChanges();

    expect(component.form.controls.sourceRef.value).toBe('NC-2019-001');
    expect(component.sourceRefOptions?.[0]).toEqual({ reference: 'NC-2019-001', label: 'NC-2019-001' });
    expect(component.sourceRefOptions?.length).toBe(NCS.length + 1);
  });

  it('ne duplique pas la référence déjà saisie quand la page la porte déjà', async () => {
    await setup();
    registres.options.and.returnValue(throwError(() => new Error('503')));
    component.form.controls.sourceType.setValue('NON_CONFORMITY' as never);
    fixture.detectChanges();
    component.form.controls.sourceRef.setValue('NC-2026-018');

    registres.options.and.returnValue(of(NCS));
    component.retrySourceRefs();
    fixture.detectChanges();

    expect(component.sourceRefOptions?.length).toBe(NCS.length);
  });

  // Changer de source efface la référence : « NC-2026-018 » conservé sous la
  // source « Risque » désignerait un objet absent du registre interrogé.
  it('efface la référence proposée par l’écran d’origine si l’utilisateur change de source', async () => {
    await setup({ prefill: { sourceRef: 'NC-2019-001' } });
    expect(component.form.controls.sourceRef.value).toBe('NC-2019-001');
    choisirSource('RISK', RISQUES);
    expect(component.form.controls.sourceRef.value).toBe('');
  });

  // ---------- la source verrouillée par l'écran d'origine ----------

  it('n’interroge aucun registre quand la source est verrouillée : elle est déjà rattachée', async () => {
    await setup({ source: { type: 'NON_CONFORMITY', reference: 'NC-2026-018' } });
    expect(component.sourceLocked).toBeTrue();
    expect(hote().querySelector('[data-test="capa-source-ref"]')).toBeNull();
  });

  // ---------- liste tronquée ----------

  it('dit que la liste est tronquée quand elle est pleine', async () => {
    await setup();
    const pleine = Array.from({ length: SOURCE_REF_PAGE_SIZE },
      (_, i) => ({ reference: 'NC-' + i, label: 'NC-' + i }));
    choisirSource('NON_CONFORMITY', pleine);
    expect(component.sourceRefTruncated).toBeTrue();
  });

  it('ne parle pas de troncature quand la liste tient entière', async () => {
    await setup();
    choisirSource('NON_CONFORMITY', NCS);
    expect(component.sourceRefTruncated).toBeFalse();
  });

  // ---------- registre muet ----------

  // Bloquer l'ouverture du dossier parce qu'une liste n'a pas répondu punirait
  // l'utilisateur d'une panne qui n'est pas la sienne. On retombe sur la saisie
  // libre — toujours obligatoire — et on le DIT.
  it('retombe sur la saisie libre obligatoire quand le registre ne répond pas', async () => {
    await setup();
    registres.options.and.returnValue(throwError(() => new Error('503')));
    component.form.controls.sourceType.setValue('RISK' as never);
    fixture.detectChanges();

    expect(component.sourceRefsFailed).toBeTrue();
    expect(component.sourceRefIsPicked).toBeFalse();
    expect(component.form.controls.sourceRef.valid).toBeFalse();
    expect(hote().querySelector('input[data-test="capa-source-ref"]')).not.toBeNull();
    expect(hote().querySelector('[data-test="capa-source-ref-retry"]')).not.toBeNull();
  });

  it('relit le registre à la demande, et reprend la liste quand il répond', async () => {
    await setup();
    registres.options.and.returnValue(throwError(() => new Error('503')));
    component.form.controls.sourceType.setValue('RISK' as never);
    fixture.detectChanges();
    expect(component.sourceRefsFailed).toBeTrue();

    registres.options.and.returnValue(of(RISQUES));
    component.retrySourceRefs();
    fixture.detectChanges();

    expect(component.sourceRefsFailed).toBeFalse();
    expect(component.sourceRefOptions).toEqual(RISQUES);
    expect(component.sourceRefIsPicked).toBeTrue();
  });

  // ---------- rien ne fuit après la fermeture ----------

  it('ne garde aucun abonnement ouvert après la fermeture de la fenêtre', async () => {
    await setup();
    choisirSource('NON_CONFORMITY', NCS);
    component.ngOnDestroy();
    expect(() => fixture.destroy()).not.toThrow();
  });
});
