import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AuditsService } from '../../audits/audits.service';
import { NcService } from '../../nc/nc.service';
import { RiskRegisterService } from '../../risk-register/risk-register.service';
import { CapaSourceRefService, SOURCE_REF_PAGE_SIZE } from './capa-source-ref.service';

/** Une page Spring minimale : seul `content` est lu ici. */
function page<T>(content: T[]): any {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length };
}

describe('CapaSourceRefService', () => {

  let nc: jasmine.SpyObj<NcService>;
  let audits: jasmine.SpyObj<AuditsService>;
  let risks: jasmine.SpyObj<RiskRegisterService>;
  let service: CapaSourceRefService;

  beforeEach(() => {
    nc = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['listPlans']);
    risks = jasmine.createSpyObj<RiskRegisterService>('RiskRegisterService', ['risks']);

    nc.listNcs.and.returnValue(of(page([])));
    audits.listPlans.and.returnValue(of(page([])));
    risks.risks.and.returnValue(of([]));

    TestBed.configureTestingModule({
      providers: [
        CapaSourceRefService,
        { provide: NcService, useValue: nc },
        { provide: AuditsService, useValue: audits },
        { provide: RiskRegisterService, useValue: risks }
      ]
    });
    service = TestBed.inject(CapaSourceRefService);
  });

  // ---------- non-conformités internes ----------

  it('lit les non-conformités internes et rend « référence — titre »', () => {
    nc.listNcs.and.returnValue(of(page([
      { reference: 'NC-2026-018', title: 'Fuite au joint de la pompe', status: 'OPEN' }
    ])));
    let rendu: any[] = [];
    service.options('nc-internal').subscribe(o => (rendu = o));
    expect(rendu).toEqual([
      { reference: 'NC-2026-018', label: 'NC-2026-018 — Fuite au joint de la pompe' }
    ]);
    expect(nc.listNcs).toHaveBeenCalledWith(0, SOURCE_REF_PAGE_SIZE, { origin: 'INTERNAL' });
  });

  // Rattacher une CAPA à un écart clos, annulé ou écarté après instruction la
  // rattacherait à une cause que plus personne ne traite.
  it('écarte les non-conformités closes, annulées et rejetées, garde celles qui vivent', () => {
    nc.listNcs.and.returnValue(of(page([
      { reference: 'NC-1', title: 'Ouverte', status: 'OPEN' },
      { reference: 'NC-2', title: 'En analyse', status: 'UNDER_ANALYSIS' },
      { reference: 'NC-3', title: 'Action définie', status: 'ACTION_DEFINED' },
      { reference: 'NC-4', title: 'Traitée', status: 'RESOLVED' },
      { reference: 'NC-5', title: 'Close', status: 'CLOSED' },
      { reference: 'NC-6', title: 'Annulée', status: 'CANCELLED' },
      { reference: 'NC-7', title: 'Rejetée', status: 'REJECTED' }
    ])));
    let rendu: any[] = [];
    service.options('nc-internal').subscribe(o => (rendu = o));
    expect(rendu.map(o => o.reference)).toEqual(['NC-1', 'NC-2', 'NC-3', 'NC-4']);
  });

  // ---------- non-conformités externes ----------

  /**
   * Une réclamation client se constate comme un écart EXTERNE : c'est cet
   * écart que la CAPA cite, et c'est lui qui porte l'enquête, le 8D et la
   * clôture. Proposer la réclamation elle-même ouvrait un dossier qui
   * n'apparaissait sur aucune fiche d'écart.
   */
  it('lit les écarts externes quand la source est la réclamation client', () => {
    nc.listNcs.and.returnValue(of(page([
      { reference: 'NC-2026-040', title: 'Colis livré endommagé', status: 'OPEN' }
    ])));
    let rendu: any[] = [];
    service.options('nc-external').subscribe(o => (rendu = o));
    expect(rendu).toEqual([
      { reference: 'NC-2026-040', label: 'NC-2026-040 — Colis livré endommagé' }
    ]);
    expect(nc.listNcs).toHaveBeenCalledWith(0, SOURCE_REF_PAGE_SIZE, { origin: 'EXTERNAL' });
  });

  // Les deux registres d'écarts ne se mélangent pas : une plainte client ne
  // doit pas proposer un écart constaté en interne, ni l'inverse.
  it("ne demande l'origine qu'une fois, et la bonne", () => {
    service.options('nc-external').subscribe();
    expect(nc.listNcs).toHaveBeenCalledTimes(1);
    expect(nc.listNcs).toHaveBeenCalledWith(0, SOURCE_REF_PAGE_SIZE, { origin: 'EXTERNAL' });
  });

  // ---------- audits ----------

  it("garde les audits terminés — c'est leur rapport qui engendre les CAPA — et écarte les annulés", () => {
    audits.listPlans.and.returnValue(of(page([
      { reference: 'AUD-1', title: 'Planifié', status: 'PLANNED' },
      { reference: 'AUD-2', title: 'En cours', status: 'IN_PROGRESS' },
      { reference: 'AUD-3', title: 'Terminé', status: 'COMPLETED' },
      { reference: 'AUD-4', title: 'Annulé', status: 'CANCELLED' }
    ])));
    let rendu: any[] = [];
    service.options('audit').subscribe(o => (rendu = o));
    expect(rendu.map(o => o.reference)).toEqual(['AUD-1', 'AUD-2', 'AUD-3']);
  });

  // ---------- risques ----------

  it('lit les risques encore ouverts du registre', () => {
    risks.risks.and.returnValue(of([
      { reference: 'R-014', title: 'Rupture fournisseur unique', status: 'TO_TREAT' },
      { reference: 'R-015', title: 'Sous surveillance', status: 'MONITORED' },
      { reference: 'R-016', title: 'Soldé', status: 'CLOSED' }
    ] as any));
    let rendu: any[] = [];
    service.options('risk').subscribe(o => (rendu = o));
    expect(rendu.map(o => o.reference)).toEqual(['R-014', 'R-015']);
    expect(rendu[0].label).toBe('R-014 — Rupture fournisseur unique');
  });

  // ---------- intitulé manquant ----------

  it("rend la seule référence quand l'intitulé manque, sans tiret orphelin", () => {
    nc.listNcs.and.returnValue(of(page([
      { reference: 'NC-2026-019', title: '   ', status: 'OPEN' }
    ])));
    let rendu: any[] = [];
    service.options('nc-internal').subscribe(o => (rendu = o));
    expect(rendu[0].label).toBe('NC-2026-019');
  });

  // ---------- panne du registre ----------

  // Avaler l'erreur rendrait une liste vide, et une liste vide dit « il n'y a
  // rien à choisir » : l'utilisateur ouvrirait un dossier sans rattachement.
  it('laisse remonter la panne du registre au lieu de rendre une liste vide', () => {
    risks.risks.and.returnValue(throwError(() => new Error('503')));
    let erreur: unknown = null;
    let rendu: unknown = null;
    service.options('risk').subscribe({ next: o => (rendu = o), error: e => (erreur = e) });
    expect(erreur).toBeTruthy();
    expect(rendu).toBeNull();
  });
});
