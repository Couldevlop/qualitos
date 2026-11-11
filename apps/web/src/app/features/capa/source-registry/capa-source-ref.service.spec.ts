import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AuditsService } from '../../audits/audits.service';
import { ComplaintsService } from '../../complaints/complaints.service';
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
  let complaints: jasmine.SpyObj<ComplaintsService>;
  let risks: jasmine.SpyObj<RiskRegisterService>;
  let service: CapaSourceRefService;

  beforeEach(() => {
    nc = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['listPlans']);
    complaints = jasmine.createSpyObj<ComplaintsService>('ComplaintsService', ['list']);
    risks = jasmine.createSpyObj<RiskRegisterService>('RiskRegisterService', ['risks']);

    nc.listNcs.and.returnValue(of(page([])));
    audits.listPlans.and.returnValue(of(page([])));
    complaints.list.and.returnValue(of(page([])));
    risks.risks.and.returnValue(of([]));

    TestBed.configureTestingModule({
      providers: [
        CapaSourceRefService,
        { provide: NcService, useValue: nc },
        { provide: AuditsService, useValue: audits },
        { provide: ComplaintsService, useValue: complaints },
        { provide: RiskRegisterService, useValue: risks }
      ]
    });
    service = TestBed.inject(CapaSourceRefService);
  });

  // ---------- non-conformités ----------

  it('lit les non-conformités et rend « référence — titre »', () => {
    nc.listNcs.and.returnValue(of(page([
      { reference: 'NC-2026-018', title: 'Fuite au joint de la pompe', status: 'OPEN' }
    ])));
    let rendu: any[] = [];
    service.options('nc').subscribe(o => (rendu = o));
    expect(rendu).toEqual([
      { reference: 'NC-2026-018', label: 'NC-2026-018 — Fuite au joint de la pompe' }
    ]);
    expect(nc.listNcs).toHaveBeenCalledWith(0, SOURCE_REF_PAGE_SIZE);
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
    service.options('nc').subscribe(o => (rendu = o));
    expect(rendu.map(o => o.reference)).toEqual(['NC-1', 'NC-2', 'NC-3', 'NC-4']);
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

  // ---------- réclamations ----------

  it('lit les réclamations par leur code et leur objet', () => {
    complaints.list.and.returnValue(of(page([
      { id: '1', code: 'REC-2026-003', subject: 'Colis livré endommagé', status: 'RECEIVED' }
    ])));
    let rendu: any[] = [];
    service.options('complaint').subscribe(o => (rendu = o));
    expect(rendu).toEqual([
      { reference: 'REC-2026-003', label: 'REC-2026-003 — Colis livré endommagé' }
    ]);
  });

  // Une réclamation rouverte est précisément celle sur laquelle une action de
  // fond a manqué : l'écarter serait le contraire du but.
  it('écarte les réclamations fermées et rejetées, garde les rouvertes', () => {
    complaints.list.and.returnValue(of(page([
      { id: '1', code: 'REC-1', subject: 'Reçue', status: 'RECEIVED' },
      { id: '2', code: 'REC-2', subject: 'Rouverte', status: 'REOPENED' },
      { id: '3', code: 'REC-3', subject: 'Fermée', status: 'CLOSED' },
      { id: '4', code: 'REC-4', subject: 'Rejetée', status: 'REJECTED' }
    ])));
    let rendu: any[] = [];
    service.options('complaint').subscribe(o => (rendu = o));
    expect(rendu.map(o => o.reference)).toEqual(['REC-1', 'REC-2']);
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
    service.options('nc').subscribe(o => (rendu = o));
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
