import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AiIncidentsService } from './ai-inc.service';
import { AiIncView, DetectRequest, EditRequest, SEVERITY_DEADLINE_DAYS } from './ai-inc.types';

/**
 * Incidents graves liés aux systèmes d'IA (AI Act, art. 73).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue ce qui fait la
 * criticité du module : le délai de notification au régulateur dépend de la
 * sévérité (2 jours pour une atteinte à la vie ou à la santé, 10 ou 15 jours
 * sinon), et le retard est recalculé à chaque lecture plutôt que figé. Les deux
 * modes sont testés.
 */
describe('AiIncidentsService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/ai-act/incidents`;
  const DAY = 86400000;
  const ago = (d: number) => new Date(Date.now() - d * DAY).toISOString();

  const detectReq = (over: Partial<DetectRequest> = {}): DetectRequest => ({
    reference: 'AIINC-2026-900',
    aiSystemId: '00000000-0000-0000-0000-000000000009',
    severity: 'CRITICAL_INFRASTRUCTURE_DISRUPTION',
    description: 'Indisponibilité du service d\'inférence pendant 2 h.',
    occurredAt: ago(1),
    detectedAt: ago(1),
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditRequest> = {}): EditRequest => ({
    description: 'Description consolidée après première analyse.',
    ...over
  });

  // ------------------------------------------------------------------------
  // Barème légal
  // ------------------------------------------------------------------------
  describe('délais de notification au régulateur (art. 73)', () => {
    it('impose le délai le plus court aux atteintes à la vie ou à la santé', () => {
      expect(SEVERITY_DEADLINE_DAYS.DEATH_OR_SERIOUS_HARM_TO_HEALTH).toBe(2);
      expect(SEVERITY_DEADLINE_DAYS.SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS).toBe(10);
      expect(SEVERITY_DEADLINE_DAYS.CRITICAL_INFRASTRUCTURE_DISRUPTION).toBe(15);
      expect(SEVERITY_DEADLINE_DAYS.SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE).toBe(15);
    });
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: AiIncidentsService;
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
      service = TestBed.inject(AiIncidentsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les incidents pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('INVESTIGATING')).map(i => i.reference)).toEqual(['AIINC-2026-001']);
      expect(run(service.list('DISMISSED'))).toEqual([]);
    }));

    it('filtre par sévérité', fakeAsync(() => {
      expect(run(service.listBySeverity('DEATH_OR_SERIOUS_HARM_TO_HEALTH')).map(i => i.id))
        .toEqual(['inc-2']);
      expect(run(service.listBySeverity('SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE'))).toEqual([]);
    }));

    it('résout un incident par identifiant, avec repli sur le premier si inconnu', fakeAsync(() => {
      expect(run(service.get('inc-2')).reference).toBe('AIINC-2026-002');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('inc-inexistant')).id).toBe('inc-1');
    }));

    // ---- Retard de notification --------------------------------------------

    it('ne remonte en retard que les incidents non encore notifiés dont l\'échéance est passée', fakeAsync(() => {
      const overdue = run(service.listOverdue());

      // inc-1 est encore dans les temps, inc-2 est déjà notifié : seul inc-3 traîne.
      expect(overdue.map(i => i.id)).toEqual(['inc-3']);
    }));

    it('borne le nombre de retards remontés', fakeAsync(() => {
      expect(run(service.listOverdue(0))).toEqual([]);
    }));

    it('recalcule le retard à chaque lecture plutôt que de le figer', fakeAsync(() => {
      // inc-2 porte une échéance déjà dépassée mais a été notifié : le drapeau
      // hérité du magasin ne doit pas suffire à le faire ressortir en retard.
      expect(run(service.get('inc-2')).overdueForRegulator).toBeFalse();
      expect(run(service.get('inc-3')).overdueForRegulator).toBeTrue();
    }));

    it('lève le retard dès que le régulateur est notifié', fakeAsync(() => {
      run(service.notifyRegulator('inc-3', {
        regulatorReference: 'CNIL-IA-2026-INC-099',
        rootCauseAnalysis: 'Saturation du pool d\'inférence.'
      }));

      expect(run(service.listOverdue())).toEqual([]);
    }));

    it('lève aussi le retard quand l\'incident est écarté', fakeAsync(() => {
      run(service.dismiss('inc-3', { reason: 'Incident applicatif hors périmètre AI Act.' }));

      expect(run(service.listOverdue())).toEqual([]);
    }));

    // ---- Déclaration -------------------------------------------------------

    it('calcule l\'échéance à partir de la sévérité et de la date de détection', fakeAsync(() => {
      const detectedAt = ago(1);

      const grave = run(service.detect(detectReq({
        severity: 'DEATH_OR_SERIOUS_HARM_TO_HEALTH', detectedAt
      })));
      const infra = run(service.detect(detectReq({
        reference: 'AIINC-2026-901',
        severity: 'CRITICAL_INFRASTRUCTURE_DISRUPTION', detectedAt
      })));

      expect(new Date(grave.regulatorNotificationDeadline!).getTime())
        .toBe(new Date(detectedAt).getTime() + 2 * DAY);
      expect(new Date(infra.regulatorNotificationDeadline!).getTime())
        .toBe(new Date(detectedAt).getTime() + 15 * DAY);
    }));

    it('crée l\'incident en tête de liste, à l\'état détecté', fakeAsync(() => {
      const created = run(service.detect(detectReq()));

      expect(created.status).toBe('DETECTED');
      expect(created.overdueForRegulator).toBeFalse();
      expect(run(service.list())[0].reference).toBe('AIINC-2026-900');
    }));

    it('signale immédiatement en retard un incident déclaré hors délai', fakeAsync(() => {
      const late = run(service.detect(detectReq({
        reference: 'AIINC-2026-902',
        severity: 'DEATH_OR_SERIOUS_HARM_TO_HEALTH',
        detectedAt: ago(30)
      })));

      expect(run(service.listOverdue()).map(i => i.id)).toContain(late.id);
    }));

    // ---- Édition -----------------------------------------------------------

    it('remplace les champs descriptifs de l\'incident', fakeAsync(() => {
      const edited = run(service.edit('inc-3', editReq({
        affectedPersonsDescription: '30 000 requêtes clientes en échec.',
        immediateActionsTaken: 'Bascule sur le pool de secours.'
      })));

      expect(edited.description).toBe('Description consolidée après première analyse.');
      expect(edited.affectedPersonsDescription).toBe('30 000 requêtes clientes en échec.');
    }));

    it('efface les champs descriptifs absents de l\'édition', fakeAsync(() => {
      // L'édition porte l'état complet du formulaire : ce qui n'est pas transmis
      // est effacé, sans quoi un champ vidé par l'utilisateur resterait en base.
      expect(run(service.edit('inc-3', editReq())).immediateActionsTaken).toBeUndefined();
    }));

    it('édite sans effet de bord quand l\'incident visé n\'existe pas', fakeAsync(() => {
      const before = run(service.get('inc-1')).description;

      run(service.edit('inc-inexistant', editReq()));

      expect(run(service.get('inc-1')).description).toBe(before);
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('ouvre l\'investigation et retient son pilote', fakeAsync(() => {
      const started = run(service.startInvestigation('inc-3', {
        investigationLeadUserId: 'pilote-1'
      }));

      expect(started.status).toBe('INVESTIGATING');
      expect(started.investigationLeadUserId).toBe('pilote-1');
      expect(started.investigationStartedAt).toBeTruthy();
    }));

    it('enregistre la notification au régulateur avec son analyse de cause', fakeAsync(() => {
      const notified = run(service.notifyRegulator('inc-1', {
        regulatorReference: 'CNIL-IA-2026-INC-042',
        rootCauseAnalysis: 'Biais géographique dans le jeu d\'entraînement.',
        correctiveActions: 'Rééquilibrage du corpus + audit indépendant.'
      }));

      expect(notified.status).toBe('NOTIFIED_REGULATOR');
      expect(notified.regulatorReference).toBe('CNIL-IA-2026-INC-042');
      expect(notified.rootCauseAnalysis).toContain('Biais géographique');
      expect(notified.correctiveActions).toContain('Rééquilibrage');
      expect(notified.regulatorNotifiedAt).toBeTruthy();
    }));

    it('conserve les actions correctives déjà consignées si la notification n\'en porte pas', fakeAsync(() => {
      const before = run(service.get('inc-2')).correctiveActions;

      const notified = run(service.notifyRegulator('inc-2', {
        regulatorReference: 'CNIL-IA-2026-INC-007-BIS',
        rootCauseAnalysis: 'Analyse complétée.'
      }));

      expect(notified.correctiveActions).toBe(before!);
    }));

    it('clôt l\'incident sur ses actions correctives', fakeAsync(() => {
      const closed = run(service.close('inc-1', {
        correctiveActions: 'Modèle V3 déployé, surveillance par sous-groupes activée.'
      }));

      expect(closed.status).toBe('CLOSED');
      expect(closed.correctiveActions).toContain('Modèle V3');
      expect(closed.closedAt).toBeTruthy();
    }));

    it('écarte un incident hors périmètre avec son motif', fakeAsync(() => {
      const dismissed = run(service.dismiss('inc-3', {
        reason: 'Incident d\'infrastructure sans lien avec le système d\'IA.'
      }));

      expect(dismissed.status).toBe('DISMISSED');
      expect(dismissed.dismissReason).toContain('sans lien');
      expect(dismissed.dismissedAt).toBeTruthy();
    }));

    it('laisse le magasin intact quand une transition vise un incident inconnu', fakeAsync(() => {
      run(service.startInvestigation('inc-inexistant', { investigationLeadUserId: 'x' }));
      run(service.notifyRegulator('inc-inexistant', { regulatorReference: 'r', rootCauseAnalysis: 'a' }));
      run(service.close('inc-inexistant', { correctiveActions: 'a' }));
      run(service.dismiss('inc-inexistant', { reason: 'r' }));

      expect(run(service.list()).map(i => i.status))
        .toEqual(['INVESTIGATING', 'NOTIFIED_REGULATOR', 'DETECTED']);
    }));

    // ---- Suppression -------------------------------------------------------

    it('supprime un incident, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('inc-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('inc-inexistant'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: AiIncidentsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(AiIncidentsService);
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

      service.list('CLOSED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('CLOSED');
      filtered.flush([]);
    });

    it('interroge les vues dérivées avec leurs paramètres', () => {
      service.listBySeverity('DEATH_OR_SERIOUS_HARM_TO_HEALTH').subscribe();
      const bySeverity = http.expectOne(r => r.url === `${BASE}/by-severity`);
      expect(bySeverity.request.params.get('severity')).toBe('DEATH_OR_SERIOUS_HARM_TO_HEALTH');
      bySeverity.flush([]);

      service.listOverdue().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/overdue-regulator-notification`);
      expect(byDefault.request.params.get('limit')).toBe('200');
      byDefault.flush([]);

      service.listOverdue(10).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/overdue-regulator-notification`);
      expect(bounded.request.params.get('limit')).toBe('10');
      bounded.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('i-1').subscribe();

      const req = http.expectOne(`${BASE}/i-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as AiIncView);
    });

    it('déclare en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = detectReq();
      service.detect(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as AiIncView);

      const edit = editReq();
      service.edit('i-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/i-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as AiIncView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const notify = { regulatorReference: 'r-1', rootCauseAnalysis: 'a' };
      const transitions: Array<[string, () => void, unknown]> = [
        ['start-investigation',
          () => service.startInvestigation('i-1', { investigationLeadUserId: AUTHOR }).subscribe(),
          { investigationLeadUserId: AUTHOR }],
        ['notify-regulator',
          () => service.notifyRegulator('i-1', notify).subscribe(), notify],
        ['close',
          () => service.close('i-1', { correctiveActions: 'c' }).subscribe(),
          { correctiveActions: 'c' }],
        ['dismiss',
          () => service.dismiss('i-1', { reason: 'r' }).subscribe(), { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/i-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as AiIncView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('i-1').subscribe();

      const req = http.expectOne(`${BASE}/i-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
