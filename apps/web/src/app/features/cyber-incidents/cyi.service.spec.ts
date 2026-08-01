import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CyberIncidentsService } from './cyi.service';
import { CyiDetectRequest, CyiView } from './cyi.types';

/**
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Les deux sont couvertes ici, car
 * c'est le magasin qui rejoue les règles NIS 2 (échéances 24h/72h/1 mois,
 * transitions d'état interdites) que le back applique en production — une
 * divergence entre les deux rendrait la démo mensongère.
 */
describe('CyberIncidentsService', () => {

  const REPORTER = '00000000-0000-0000-0000-000000000999';
  const HOUR = 3600000;
  const DAY = 86400000;

  const detectReq = (over: Partial<CyiDetectRequest> = {}): CyiDetectRequest => ({
    reference: 'NIS2-INC-2026-900',
    title: 'Exfiltration suspectée sur le SI RH',
    detectedAt: new Date(Date.now() - HOUR).toISOString(),
    incidentType: 'DATA_BREACH',
    severity: 'MEDIUM',
    estimatedAffectedUsers: 3,
    reportedByUserId: REPORTER,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: CyberIncidentsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): { value?: T; error?: { status: number } } {
      let value: T | undefined;
      let error: { status: number } | undefined;
      source.subscribe({ next: v => (value = v), error: e => (error = e) });
      tick(300);
      return { value, error };
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CyberIncidentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    it('liste les incidents pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(3);

      const detected = run(service.list('DETECTED')).value ?? [];
      expect(detected.length).toBe(1);
      expect(detected.every(i => i.status === 'DETECTED')).toBeTrue();

      expect(run(service.list('CLOSED')).value).toEqual([]);
    }));

    it('résout un incident par identifiant, et refuse un identifiant inconnu', fakeAsync(() => {
      expect(run(service.get('cyi-seed-001')).value?.reference).toBe('NIS2-INC-2026-001');
      expect(run(service.get('cyi-inexistant')).error?.status).toBe(404);
    }));

    it('résout un incident par référence, et refuse une référence inconnue', fakeAsync(() => {
      expect(run(service.getByReference('NIS2-INC-2026-002')).value?.id).toBe('cyi-seed-002');
      expect(run(service.getByReference('NIS2-INC-9999')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('cyi-seed-001')).value!;
      first.title = 'titre falsifié';

      expect(run(service.get('cyi-seed-001')).value?.title).toBe('Tentative ransomware sur partage RH');
    }));

    // ---- Signalement -------------------------------------------------------

    it('refuse une référence déjà utilisée', fakeAsync(() => {
      expect(run(service.detect(detectReq({ reference: 'NIS2-INC-2026-001' }))).error?.status).toBe(409);
    }));

    it('ouvre les trois échéances NIS 2 pour un incident significatif', fakeAsync(() => {
      const detectedAt = new Date(Date.now() - 2 * HOUR).toISOString();
      const created = run(service.detect(detectReq({ severity: 'HIGH', detectedAt }))).value!;

      const t0 = new Date(detectedAt).getTime();
      expect(created.significant).toBeTrue();
      expect(created.status).toBe('DETECTED');
      expect(new Date(created.earlyWarningDeadlineAt!).getTime()).toBe(t0 + 24 * HOUR);
      expect(new Date(created.initialAssessmentDeadlineAt!).getTime()).toBe(t0 + 72 * HOUR);
      expect(new Date(created.finalReportDeadlineAt!).getTime()).toBe(t0 + 30 * DAY);
    }));

    it('n\'ouvre aucune échéance CSIRT pour un incident non significatif', fakeAsync(() => {
      const created = run(service.detect(detectReq({ severity: 'LOW' }))).value!;

      expect(created.significant).toBeFalse();
      expect(created.earlyWarningDeadlineAt).toBeNull();
      expect(created.initialAssessmentDeadlineAt).toBeNull();
      expect(created.finalReportDeadlineAt).toBeNull();
    }));

    it('normalise les champs optionnels absents du signalement', fakeAsync(() => {
      const created = run(service.detect(detectReq())).value!;

      expect(created.description).toBeNull();
      expect(created.occurredAt).toBeNull();
      expect(created.linkedBreachId).toBeNull();
      expect(created.affectedAssets).toEqual([]);
      expect(created.affectedServices).toEqual([]);
      expect(created.handledByUserId).toBeNull();
    }));

    it('place le nouvel incident en tête de liste', fakeAsync(() => {
      run(service.detect(detectReq()));
      expect(run(service.list()).value?.[0].reference).toBe('NIS2-INC-2026-900');
    }));

    // ---- Retards -----------------------------------------------------------

    it('signale en retard un incident significatif dont l\'échéance est dépassée', fakeAsync(() => {
      const created = run(service.detect(detectReq({
        severity: 'CRITICAL', detectedAt: new Date(Date.now() - 4 * DAY).toISOString()
      }))).value!;

      expect(run(service.earlyWarningOverdue()).value?.map(i => i.id)).toEqual([created.id]);
      expect(run(service.initialAssessmentOverdue()).value?.map(i => i.id)).toEqual([created.id]);
      // Le rapport final court sur 1 mois : 4 jours ne suffisent pas à le mettre en retard.
      expect(run(service.finalReportOverdue()).value).toEqual([]);
    }));

    it('éteint les retards dès que la notification est enregistrée', fakeAsync(() => {
      const created = run(service.detect(detectReq({
        severity: 'CRITICAL', detectedAt: new Date(Date.now() - 4 * DAY).toISOString()
      }))).value!;

      run(service.recordEarlyWarning(created.id, {
        sentAt: new Date().toISOString(), reference: 'CSIRT-FR-2026-Z1'
      }));

      expect(run(service.earlyWarningOverdue()).value).toEqual([]);
      expect(run(service.initialAssessmentOverdue()).value?.length).toBe(1);
    }));

    it('n\'exige plus rien d\'un incident rejeté, même significatif et en retard', fakeAsync(() => {
      const created = run(service.detect(detectReq({
        severity: 'CRITICAL', detectedAt: new Date(Date.now() - 4 * DAY).toISOString()
      }))).value!;

      run(service.reject(created.id, { reason: 'Faux positif confirmé par le SOC.' }));

      expect(run(service.earlyWarningOverdue()).value).toEqual([]);
      expect(run(service.initialAssessmentOverdue()).value).toEqual([]);
    }));

    it('borne les listes de retard au nombre demandé', fakeAsync(() => {
      const past = () => new Date(Date.now() - 4 * DAY).toISOString();
      run(service.detect(detectReq({ reference: 'NIS2-INC-A', severity: 'CRITICAL', detectedAt: past() })));
      run(service.detect(detectReq({ reference: 'NIS2-INC-B', severity: 'CRITICAL', detectedAt: past() })));

      expect(run(service.earlyWarningOverdue(1)).value?.length).toBe(1);
    }));

    // ---- Transitions -------------------------------------------------------

    it('démarre l\'évaluation depuis DETECTED et enregistre le prenant en charge', fakeAsync(() => {
      const updated = run(service.startAssessment('cyi-seed-003', { handledByUserId: REPORTER })).value!;

      expect(updated.status).toBe('ASSESSING');
      expect(updated.handledByUserId).toBe(REPORTER);
    }));

    it('refuse de démarrer une évaluation hors DETECTED', fakeAsync(() => {
      expect(run(service.startAssessment('cyi-seed-002', { handledByUserId: REPORTER })).error?.status).toBe(409);
      expect(run(service.startAssessment('inconnu', { handledByUserId: REPORTER })).error?.status).toBe(404);
    }));

    it('endigue depuis ASSESSING en consignant mesures et impact', fakeAsync(() => {
      const updated = run(service.mitigate('cyi-seed-001', {
        containmentMeasures: 'Poste isolé du réseau, comptes réinitialisés.',
        impactDescription: 'Aucun fichier exfiltré.'
      })).value!;

      expect(updated.status).toBe('MITIGATED');
      expect(updated.containmentMeasures).toContain('Poste isolé');
      expect(updated.impactDescription).toBe('Aucun fichier exfiltré.');
    }));

    it('conserve le prenant en charge existant quand l\'endiguement n\'en fournit pas', fakeAsync(() => {
      const updated = run(service.mitigate('cyi-seed-001', { containmentMeasures: 'Isolation.' })).value!;

      expect(updated.handledByUserId).toBe(REPORTER);
      expect(updated.impactDescription).toBeNull();
    }));

    it('refuse d\'endiguer un incident qui n\'est pas en évaluation', fakeAsync(() => {
      expect(run(service.mitigate('cyi-seed-003', { containmentMeasures: 'x' })).error?.status).toBe(409);
      expect(run(service.mitigate('inconnu', { containmentMeasures: 'x' })).error?.status).toBe(404);
    }));

    it('clôture depuis MITIGATED en horodatant la fermeture', fakeAsync(() => {
      const updated = run(service.close('cyi-seed-002', { closureNotes: 'RETEX diffusé.' })).value!;

      expect(updated.status).toBe('CLOSED');
      expect(updated.closureNotes).toBe('RETEX diffusé.');
      expect(updated.closedAt).not.toBeNull();
    }));

    it('accepte une clôture sans notes', fakeAsync(() => {
      expect(run(service.close('cyi-seed-002', {})).value?.closureNotes).toBeNull();
    }));

    it('refuse une clôture avant endiguement', fakeAsync(() => {
      expect(run(service.close('cyi-seed-001', {})).error?.status).toBe(409);
      expect(run(service.close('inconnu', {})).error?.status).toBe(404);
    }));

    it('rejette un incident encore en DETECTED ou ASSESSING', fakeAsync(() => {
      const rejected = run(service.reject('cyi-seed-001', { reason: 'Doublon de NIS2-INC-2026-002.' })).value!;

      expect(rejected.status).toBe('REJECTED');
      expect(rejected.rejectionReason).toBe('Doublon de NIS2-INC-2026-002.');
      expect(rejected.closedAt).not.toBeNull();
    }));

    it('refuse de rejeter un incident déjà endigué', fakeAsync(() => {
      expect(run(service.reject('cyi-seed-002', { reason: 'x' })).error?.status).toBe(409);
      expect(run(service.reject('inconnu', { reason: 'x' })).error?.status).toBe(404);
    }));

    // ---- Notifications CSIRT ------------------------------------------------

    it('enregistre les trois notifications CSIRT d\'un incident significatif', fakeAsync(() => {
      const sentAt = new Date().toISOString();

      const ew = run(service.recordEarlyWarning('cyi-seed-001', { sentAt, reference: 'CSIRT-1' })).value!;
      expect(ew.earlyWarningSentAt).toBe(sentAt);
      expect(ew.earlyWarningReference).toBe('CSIRT-1');

      const ia = run(service.recordInitialAssessment('cyi-seed-001', { sentAt, reference: 'CSIRT-2' })).value!;
      expect(ia.initialAssessmentSentAt).toBe(sentAt);
      expect(ia.initialAssessmentReference).toBe('CSIRT-2');

      const fr = run(service.recordFinalReport('cyi-seed-001', { sentAt, reference: 'CSIRT-3' })).value!;
      expect(fr.finalReportSentAt).toBe(sentAt);
      expect(fr.finalReportReference).toBe('CSIRT-3');
    }));

    it('refuse toute notification CSIRT sur un incident non significatif', fakeAsync(() => {
      const payload = { sentAt: new Date().toISOString(), reference: 'CSIRT-X' };

      expect(run(service.recordEarlyWarning('cyi-seed-003', payload)).error?.status).toBe(409);
      expect(run(service.recordInitialAssessment('cyi-seed-003', payload)).error?.status).toBe(409);
      expect(run(service.recordFinalReport('cyi-seed-003', payload)).error?.status).toBe(409);
    }));

    it('refuse une notification sur un incident inconnu', fakeAsync(() => {
      const payload = { sentAt: new Date().toISOString(), reference: 'CSIRT-X' };

      expect(run(service.recordEarlyWarning('inconnu', payload)).error?.status).toBe(404);
      expect(run(service.recordInitialAssessment('inconnu', payload)).error?.status).toBe(404);
      expect(run(service.recordFinalReport('inconnu', payload)).error?.status).toBe(404);
    }));

    // ---- Sévérité et rattachement ------------------------------------------

    it('ouvre les échéances quand la requalification rend l\'incident significatif', fakeAsync(() => {
      const updated = run(service.updateSeverity('cyi-seed-003', { severity: 'HIGH' })).value!;

      expect(updated.significant).toBeTrue();
      const t0 = new Date(updated.detectedAt).getTime();
      expect(new Date(updated.earlyWarningDeadlineAt!).getTime()).toBe(t0 + 24 * HOUR);
      expect(new Date(updated.initialAssessmentDeadlineAt!).getTime()).toBe(t0 + 72 * HOUR);
      expect(new Date(updated.finalReportDeadlineAt!).getTime()).toBe(t0 + 30 * DAY);
    }));

    it('conserve les échéances déjà ouvertes lors d\'une requalification à la baisse', fakeAsync(() => {
      const before = run(service.get('cyi-seed-001')).value!;

      const updated = run(service.updateSeverity('cyi-seed-001', { severity: 'LOW' })).value!;

      expect(updated.significant).toBeFalse();
      expect(updated.earlyWarningDeadlineAt).toBe(before.earlyWarningDeadlineAt!);
    }));

    it('fige la sévérité d\'un incident terminal', fakeAsync(() => {
      run(service.close('cyi-seed-002', {}));
      expect(run(service.updateSeverity('cyi-seed-002', { severity: 'LOW' })).error?.status).toBe(409);

      run(service.reject('cyi-seed-003', { reason: 'Hors champ NIS 2.' }));
      expect(run(service.updateSeverity('cyi-seed-003', { severity: 'HIGH' })).error?.status).toBe(409);

      expect(run(service.updateSeverity('inconnu', { severity: 'HIGH' })).error?.status).toBe(404);
    }));

    it('rattache une violation RGPD à l\'incident', fakeAsync(() => {
      const breachId = '11111111-1111-4111-8111-111111111111';

      expect(run(service.linkBreach('cyi-seed-003', { breachId })).value?.linkedBreachId).toBe(breachId);
      expect(run(service.linkBreach('inconnu', { breachId })).error?.status).toBe(404);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: CyberIncidentsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    const base = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents`;
    const view = { id: 'i-1' } as CyiView;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CyberIncidentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('n\'envoie pas de filtre de statut quand aucun n\'est demandé', () => {
      service.list().subscribe();
      const req = http.expectOne(r => r.url === base);
      expect(req.request.params.has('status')).toBeFalse();
      req.flush([]);
    });

    it('transmet le statut demandé en paramètre de requête', () => {
      service.list('MITIGATED').subscribe();
      const req = http.expectOne(r => r.url === base);
      expect(req.request.params.get('status')).toBe('MITIGATED');
      req.flush([]);
    });

    it('interroge les trois registres de retard avec leur limite', () => {
      service.earlyWarningOverdue(25).subscribe();
      const ew = http.expectOne(r => r.url === `${base}/early-warning-overdue`);
      expect(ew.request.params.get('limit')).toBe('25');
      ew.flush([]);

      service.initialAssessmentOverdue().subscribe();
      const ia = http.expectOne(r => r.url === `${base}/initial-assessment-overdue`);
      expect(ia.request.params.get('limit')).toBe('100');
      ia.flush([]);

      service.finalReportOverdue(5).subscribe();
      const fr = http.expectOne(r => r.url === `${base}/final-report-overdue`);
      expect(fr.request.params.get('limit')).toBe('5');
      fr.flush([]);
    });

    it('résout une fiche par identifiant et par référence', () => {
      service.get('i-1').subscribe(i => expect(i.id).toBe('i-1'));
      http.expectOne(`${base}/i-1`).flush(view);

      service.getByReference('NIS2-INC-2026-002').subscribe();
      const byRef = http.expectOne(r => r.url === `${base}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('NIS2-INC-2026-002');
      byRef.flush(view);
    });

    it('publie le signalement tel quel sur la racine du registre', () => {
      const body = { reference: 'NIS2-INC-1', title: 'T' };
      service.detect(body as CyiDetectRequest).subscribe();
      const req = http.expectOne(r => r.url === base && r.method === 'POST');
      expect(req.request.body).toEqual(body);
      req.flush(view);
    });

    it('adresse chaque transition à sa propre route', () => {
      const routes: Array<[() => void, string, unknown]> = [
        [() => service.startAssessment('i-1', { handledByUserId: 'u' }).subscribe(), 'start-assessment', { handledByUserId: 'u' }],
        [() => service.mitigate('i-1', { containmentMeasures: 'c' }).subscribe(), 'mitigate', { containmentMeasures: 'c' }],
        [() => service.recordEarlyWarning('i-1', { sentAt: 's', reference: 'r' }).subscribe(), 'early-warning', { sentAt: 's', reference: 'r' }],
        [() => service.recordInitialAssessment('i-1', { sentAt: 's', reference: 'r' }).subscribe(), 'initial-assessment', { sentAt: 's', reference: 'r' }],
        [() => service.recordFinalReport('i-1', { sentAt: 's', reference: 'r' }).subscribe(), 'final-report', { sentAt: 's', reference: 'r' }],
        [() => service.close('i-1', { closureNotes: 'n' }).subscribe(), 'close', { closureNotes: 'n' }],
        [() => service.reject('i-1', { reason: 'r' }).subscribe(), 'reject', { reason: 'r' }],
        [() => service.updateSeverity('i-1', { severity: 'HIGH' }).subscribe(), 'severity', { severity: 'HIGH' }],
        [() => service.linkBreach('i-1', { breachId: 'b' }).subscribe(), 'link-breach', { breachId: 'b' }]
      ];

      for (const [call, path, body] of routes) {
        call();
        const req = http.expectOne(`${base}/i-1/${path}`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(body);
        req.flush(view);
      }
    });

    it('propage l\'erreur serveur au lieu de la masquer', () => {
      let status = 0;
      service.get('i-1').subscribe({ error: e => (status = e.status) });
      http.expectOne(`${base}/i-1`).flush({}, { status: 403, statusText: 'Forbidden' });
      expect(status).toBe(403);
    });
  });
});
