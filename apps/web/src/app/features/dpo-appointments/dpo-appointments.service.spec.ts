import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DpoAppointmentsService } from './dpo-appointments.service';
import {
  ActivateDpoRequest,
  DpoAppointmentView,
  EditDpoRequest,
  ProposeDpoRequest
} from './dpo-appointments.types';

/**
 * Désignations du délégué à la protection des données (RGPD art. 37 à 39).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * PROPOSED → ACTIVE → ENDED (ou CANCELLED avant activation) et l'invariant qui
 * fait la valeur du registre : un seul délégué actif par périmètre à un instant
 * donné, le précédent étant clos automatiquement avec le motif du remplacement.
 * Les deux modes sont testés.
 */
describe('DpoAppointmentsService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/dpo-appointments`;

  const proposeReq = (over: Partial<ProposeDpoRequest> = {}): ProposeDpoRequest => ({
    reference: 'DPO-FILIALE-LAB',
    dpoFullName: 'Claire Moreau',
    dpoEmail: 'dpo-lab@qualitos.io',
    dpoType: 'INTERNAL',
    scope: 'LABORATOIRE',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditDpoRequest> = {}): EditDpoRequest => ({
    dpoFullName: 'Jean-Pierre Laurent',
    dpoEmail: 'jp.laurent@privacymd.example',
    dpoType: 'EXTERNAL',
    ...over
  });

  const activateReq = (over: Partial<ActivateDpoRequest> = {}): ActivateDpoRequest => ({
    effectiveFrom: '2026-09-01',
    regulatorNotifiedAt: '2026-08-20',
    regulatorNotificationReference: 'CNIL-DPO-2026-DESIG-0099',
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: DpoAppointmentsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(300);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(DpoAppointmentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les désignations pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('ACTIVE')).map(a => a.reference)).toEqual(['DPO-GROUPE-2026']);
      expect(run(service.list('CANCELLED'))).toEqual([]);
    }));

    it('résout une désignation par identifiant et par référence', fakeAsync(() => {
      expect(run(service.get('dpo-2')).dpoFullName).toBe('Jean-Pierre Laurent');
      expect(run(service.getByReference('DPO-GROUPE-2024')).id).toBe('dpo-3');
    }));

    it('retombe sur la première désignation quand la clé est inconnue', fakeAsync(() => {
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('dpo-inexistante')).id).toBe('dpo-1');
      expect(run(service.getByReference('DPO-ABSENTE')).id).toBe('dpo-1');
    }));

    // ---- Délégué en fonction -------------------------------------------------

    it('ne rend en fonction que la désignation active du périmètre demandé', fakeAsync(() => {
      expect(run(service.findActiveByScope('GROUPE'))?.reference).toBe('DPO-GROUPE-2026');
    }));

    it('ne rend rien quand le périmètre n\'a pas de délégué en fonction', fakeAsync(() => {
      // HOPITAL_SUD n'a qu'une proposition, pas encore de désignation active.
      expect(run(service.findActiveByScope('HOPITAL_SUD'))).toBeNull();
      expect(run(service.findActiveByScope('PERIMETRE-INCONNU'))).toBeNull();
    }));

    // ---- Proposition ---------------------------------------------------------

    it('crée une proposition en tête de liste, sans effet ni notification', fakeAsync(() => {
      const proposed = run(service.propose(proposeReq()));

      expect(proposed.status).toBe('PROPOSED');
      expect(proposed.effectiveFrom).toBeUndefined();
      expect(proposed.regulatorNotifiedAt).toBeUndefined();
      expect(run(service.list())[0].reference).toBe('DPO-FILIALE-LAB');
    }));

    it('retient la société pour un délégué externe', fakeAsync(() => {
      const proposed = run(service.propose(proposeReq({
        dpoType: 'EXTERNAL', externalCompanyName: 'PrivacyMD Consulting'
      })));

      expect(proposed.dpoType).toBe('EXTERNAL');
      expect(proposed.externalCompanyName).toBe('PrivacyMD Consulting');
    }));

    it('normalise en tableau vide les traitements liés non fournis', fakeAsync(() => {
      expect(run(service.propose(proposeReq())).linkedProcessingActivityIds).toEqual([]);

      expect(run(service.propose(proposeReq({
        reference: 'DPO-AVEC-LIENS', linkedProcessingActivityIds: ['ropa-1']
      }))).linkedProcessingActivityIds).toEqual(['ropa-1']);
    }));

    // ---- Édition -------------------------------------------------------------

    it('remplace les coordonnées du délégué', fakeAsync(() => {
      const edited = run(service.edit('dpo-2', editReq({
        qualifications: 'DPO certifié AFCDP, spécialisation santé.'
      })));

      expect(edited.dpoEmail).toBe('jp.laurent@privacymd.example');
      expect(edited.qualifications).toContain('AFCDP');
    }));

    it('remet à vide les traitements liés absents de l\'édition', fakeAsync(() => {
      run(service.edit('dpo-2', editReq({ linkedProcessingActivityIds: ['ropa-9'] })));
      expect(run(service.get('dpo-2')).linkedProcessingActivityIds).toEqual(['ropa-9']);

      run(service.edit('dpo-2', editReq()));
      expect(run(service.get('dpo-2')).linkedProcessingActivityIds).toEqual([]);
    }));

    it('édite sans effet de bord quand la désignation visée n\'existe pas', fakeAsync(() => {
      const before = run(service.get('dpo-1')).dpoFullName;

      run(service.edit('dpo-inexistante', editReq()));

      expect(run(service.get('dpo-1')).dpoFullName).toBe(before);
    }));

    // ---- Activation : un seul délégué actif par périmètre ---------------------

    it('active la désignation et consigne la notification à l\'autorité', fakeAsync(() => {
      const activated = run(service.activate('dpo-2', activateReq()));

      expect(activated.status).toBe('ACTIVE');
      expect(activated.effectiveFrom).toBe('2026-09-01');
      // Art. 37§7 : la désignation n'est opposable qu'une fois notifiée.
      expect(activated.regulatorNotifiedAt).toBe('2026-08-20');
      expect(activated.regulatorNotificationReference).toBe('CNIL-DPO-2026-DESIG-0099');
    }));

    it('clôt le délégué précédent du même périmètre, en nommant son remplaçant', fakeAsync(() => {
      const successeur = run(service.propose(proposeReq({
        reference: 'DPO-GROUPE-2027', scope: 'GROUPE'
      })));

      run(service.activate(successeur.id, activateReq({ effectiveFrom: '2027-01-01' })));

      const precedent = run(service.get('dpo-1'));
      expect(precedent.status).toBe('ENDED');
      expect(precedent.effectiveTo).toBe('2027-01-01');
      // Le motif doit désigner le successeur : sans cela, la chaîne des
      // désignations devient illisible pour l'autorité de contrôle.
      expect(precedent.endReason).toContain('DPO-GROUPE-2027');
      expect(run(service.findActiveByScope('GROUPE'))?.id).toBe(successeur.id);
    }));

    it('ne touche pas aux délégués des autres périmètres', fakeAsync(() => {
      run(service.activate('dpo-2', activateReq()));

      // HOPITAL_SUD est un périmètre distinct : le délégué du groupe reste actif.
      expect(run(service.get('dpo-1')).status).toBe('ACTIVE');
    }));

    it('ne réveille pas une désignation déjà close du même périmètre', fakeAsync(() => {
      const successeur = run(service.propose(proposeReq({
        reference: 'DPO-GROUPE-2027', scope: 'GROUPE'
      })));

      run(service.activate(successeur.id, activateReq()));

      // dpo-3 était déjà ENDED : son motif d'origine ne doit pas être réécrit.
      expect(run(service.get('dpo-3')).endReason).toContain('Départ retraite');
    }));

    it('active sans effet de bord quand la désignation visée n\'existe pas', fakeAsync(() => {
      run(service.activate('dpo-inexistante', activateReq()));

      expect(run(service.get('dpo-2')).status).toBe('PROPOSED');
    }));

    // ---- Fin et annulation ---------------------------------------------------

    it('clôt une désignation avec son motif et sa date de fin', fakeAsync(() => {
      const ended = run(service.end('dpo-1', {
        reason: 'Démission.', effectiveTo: '2026-12-31'
      }));

      expect(ended.status).toBe('ENDED');
      expect(ended.endReason).toBe('Démission.');
      expect(ended.effectiveTo).toBe('2026-12-31');
      expect(run(service.findActiveByScope('GROUPE'))).toBeNull();
    }));

    it('annule une proposition sans lui donner de date d\'effet', fakeAsync(() => {
      const cancelled = run(service.cancel('dpo-2', { reason: 'Candidature retirée.' }));

      expect(cancelled.status).toBe('CANCELLED');
      expect(cancelled.endReason).toBe('Candidature retirée.');
      // Une désignation annulée n'a jamais pris effet : pas de date de fin.
      expect(cancelled.effectiveTo).toBeUndefined();
    }));

    it('clôt et annule sans effet de bord quand la désignation n\'existe pas', fakeAsync(() => {
      run(service.end('dpo-inexistante', { reason: 'r', effectiveTo: '2026-12-31' }));
      run(service.cancel('dpo-inexistante', { reason: 'r' }));

      expect(run(service.get('dpo-1')).status).toBe('ACTIVE');
      expect(run(service.get('dpo-2')).status).toBe('PROPOSED');
    }));

    // ---- Suppression ---------------------------------------------------------

    it('supprime une désignation, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('dpo-2'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('dpo-inexistante'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: DpoAppointmentsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(DpoAppointmentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('liste sans paramètre, et ajoute le statut quand il est demandé', () => {
      service.list().subscribe();
      const plain = http.expectOne(BASE);
      expect(plain.request.method).toBe('GET');
      expect(plain.request.params.has('status')).toBeFalse();
      plain.flush([]);

      service.list('ACTIVE').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('ACTIVE');
      filtered.flush([]);
    });

    it('lit une fiche par identifiant, par référence et par périmètre actif', () => {
      service.get('d-1').subscribe();
      const byId = http.expectOne(`${BASE}/d-1`);
      expect(byId.request.method).toBe('GET');
      byId.flush({} as DpoAppointmentView);

      service.getByReference('DPO-GROUPE-2026').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('DPO-GROUPE-2026');
      byRef.flush({} as DpoAppointmentView);

      service.findActiveByScope('GROUPE').subscribe();
      const active = http.expectOne(r => r.url === `${BASE}/active`);
      expect(active.request.params.get('scope')).toBe('GROUPE');
      active.flush(null);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = proposeReq();
      service.propose(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as DpoAppointmentView);

      const edit = editReq();
      service.edit('d-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/d-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as DpoAppointmentView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const activate = activateReq();
      const endBody = { reason: 'r', effectiveTo: '2026-12-31' };
      const transitions: Array<[string, () => void, unknown]> = [
        ['activate', () => service.activate('d-1', activate).subscribe(), activate],
        ['end', () => service.end('d-1', endBody).subscribe(), endBody],
        ['cancel', () => service.cancel('d-1', { reason: 'r' }).subscribe(), { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/d-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as DpoAppointmentView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('d-1').subscribe();

      const req = http.expectOne(`${BASE}/d-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
