import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CirclesService } from './circles.service';
import {
  AddMeetingRequest,
  AddProposalRequest,
  CircleMeetingResponse,
  CircleMemberResponse,
  CircleProposalResponse,
  CircleResponse,
  CreateCircleRequest,
  MeetingMinutes,
  MeetingTranscript
} from './circles.types';

/**
 * Cercles de qualité (§3.3).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle de vie
 * d'un cercle (ACTIVE ⇄ PAUSED → ARCHIVED) et surtout celui d'une proposition —
 * PROPOSED → UNDER_REVIEW → APPROVED → IMPLEMENTED → MEASURED — qui est ce qui
 * distingue un cercle qui produit des idées d'un cercle qui produit des effets
 * mesurés. Les deux modes sont testés.
 */
describe('CirclesService', () => {

  const BASE = `${environment.apiBaseUrl}/api/v1/circles`;

  const circleReq = (over: Partial<CreateCircleRequest> = {}): CreateCircleRequest => ({
    name: 'Cercle logistique expédition',
    description: 'Réduction des erreurs de préparation',
    topic: 'logistique',
    ...over
  });

  const meetingReq = (over: Partial<AddMeetingRequest> = {}): AddMeetingRequest => ({
    title: 'Réunion de lancement',
    scheduledAt: '2026-09-15T09:00:00Z',
    ...over
  });

  const proposalReq = (over: Partial<AddProposalRequest> = {}): AddProposalRequest => ({
    title: 'Double contrôle des colis fragiles',
    proposedBy: 'u3',
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: CirclesService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(1000);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CirclesService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau — sauf la
      // transcription, qui n'a pas de branche simulée (voir plus bas).
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les cercles pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      const page = run(service.listCircles());
      expect(page.totalElements).toBe(2);

      expect(run(service.listCircles(0, 50, 'PAUSED')).content.map(c => c.id)).toEqual(['c2']);
      expect(run(service.listCircles(0, 50, 'ARCHIVED')).content).toEqual([]);
    }));

    it('résout un cercle par identifiant, avec repli sur le premier si inconnu', fakeAsync(() => {
      expect(run(service.getCircle('c2')).name).toBe('Cercle qualité fournisseurs');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.getCircle('cercle-inconnu')).id).toBe('c1');
    }));

    // ---- Création et édition -------------------------------------------------

    it('crée un cercle actif et vide, en tête de liste', fakeAsync(() => {
      const created = run(service.createCircle(circleReq()));

      expect(created.status).toBe('ACTIVE');
      expect(created.memberCount).toBe(0);
      expect(created.members).toEqual([]);
      expect(created.meetings).toEqual([]);
      expect(created.proposals).toEqual([]);
      expect(run(service.listCircles()).content[0].name).toBe('Cercle logistique expédition');
    }));

    it('ne met à jour que les champs transmis', fakeAsync(() => {
      const updated = run(service.updateCircle('c1', { topic: 'soudure-robotisee' }));

      expect(updated.topic).toBe('soudure-robotisee');
      // Le nom n'était pas transmis : il ne doit pas être effacé pour autant.
      expect(updated.name).toBe('Cercle production ligne 3');
    }));

    it('met à jour sans effet de bord quand le cercle visé n\'existe pas', fakeAsync(() => {
      const before = run(service.getCircle('c1')).name;

      run(service.updateCircle('cercle-inconnu', { name: 'usurpé' }));

      expect(run(service.getCircle('c1')).name).toBe(before);
    }));

    it('supprime un cercle, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.deleteCircle('c2'));
      expect(run(service.listCircles()).totalElements).toBe(1);

      run(service.deleteCircle('cercle-inconnu'));
      expect(run(service.listCircles()).totalElements).toBe(1);
    }));

    // ---- Membres, réunions, propositions ---------------------------------------

    it('rattache un membre au cercle et tient son effectif à jour', fakeAsync(() => {
      const member = run(service.addMember('c2', { userId: 'u9', role: 'MEMBER' }));

      expect(member.userId).toBe('u9');
      const circle = run(service.getCircle('c2'));
      expect(circle.members.map(m => m.userId)).toEqual(['u9']);
      // L'effectif est dérivé de la liste, jamais saisi à part.
      expect(circle.memberCount).toBe(1);
    }));

    it('planifie une réunion à l\'état programmé', fakeAsync(() => {
      const meeting = run(service.addMeeting('c2', meetingReq()));

      expect(meeting.status).toBe('SCHEDULED');
      expect(meeting.circleId).toBe('c2');
      expect(run(service.getCircle('c2')).meetings.map(m => m.id)).toEqual([meeting.id]);
    }));

    it('dépose une proposition à l\'état proposé, rattachée à son auteur', fakeAsync(() => {
      const proposal = run(service.addProposal('c2', proposalReq({ meetingId: 'mt1' })));

      expect(proposal.status).toBe('PROPOSED');
      expect(proposal.proposedBy).toBe('u3');
      expect(proposal.meetingId).toBe('mt1');
      expect(run(service.getCircle('c2')).proposals.map(p => p.id)).toEqual([proposal.id]);
    }));

    it('rend l\'objet créé même quand le cercle visé n\'existe pas', fakeAsync(() => {
      // L'écran doit pouvoir afficher ce qu'il vient de créer sans dépendre de
      // la présence du cercle dans le magasin de démonstration.
      expect(run(service.addMember('cercle-inconnu', { userId: 'u9', role: 'MEMBER' })).userId)
        .toBe('u9');
      expect(run(service.addMeeting('cercle-inconnu', meetingReq())).title)
        .toBe('Réunion de lancement');
      expect(run(service.addProposal('cercle-inconnu', proposalReq())).title)
        .toBe('Double contrôle des colis fragiles');
    }));

    // ---- Cycle de vie d'une proposition ------------------------------------------

    it('conduit une proposition de l\'idée à l\'impact mesuré', fakeAsync(() => {
      expect(run(service.reviewProposal('c1', 'p1')).status).toBe('UNDER_REVIEW');
      expect(run(service.approveProposal('c1', 'p1', { validatedBy: 'u1' })).status).toBe('APPROVED');
      expect(run(service.implementProposal('c1', 'p1')).status).toBe('IMPLEMENTED');

      const measured = run(service.recordImpact('c1', 'p1', {
        impactNote: 'Rebuts divisés par deux sur trois mois.'
      }));
      expect(measured.status).toBe('MEASURED');
      expect(measured.impactNote).toContain('divisés par deux');
    }));

    it('conserve le motif de rejet', fakeAsync(() => {
      const rejected = run(service.rejectProposal('c1', 'p2', {
        validatedBy: 'u1', reason: 'Coût disproportionné au regard du gain.'
      }));

      expect(rejected.status).toBe('REJECTED');
      expect(rejected.rejectionReason).toContain('Coût disproportionné');
    }));

    it('persiste la transition dans le cercle, pas seulement dans la réponse', fakeAsync(() => {
      run(service.approveProposal('c1', 'p1', { validatedBy: 'u1' }));

      const stored = run(service.getCircle('c1')).proposals.find(p => p.id === 'p1');
      expect(stored?.status).toBe('APPROVED');
    }));

    it('rend une proposition de repli quand le cercle ou la proposition est inconnu', fakeAsync(() => {
      const surCercleInconnu = run(service.reviewProposal('cercle-inconnu', 'p1'));
      expect(surCercleInconnu.id).toBe('p1');
      expect(surCercleInconnu.status).toBe('UNDER_REVIEW');

      const propositionInconnue = run(service.implementProposal('c1', 'proposition-inconnue'));
      expect(propositionInconnue.id).toBe('proposition-inconnue');
      expect(propositionInconnue.status).toBe('IMPLEMENTED');
    }));

    // ---- Cycle de vie du cercle ----------------------------------------------------

    it('met en pause, reprend et archive un cercle', fakeAsync(() => {
      expect(run(service.pauseCircle('c1')).status).toBe('PAUSED');
      expect(run(service.resumeCircle('c1')).status).toBe('ACTIVE');
      expect(run(service.archiveCircle('c1')).status).toBe('ARCHIVED');
    }));

    it('laisse le magasin intact quand une transition vise un cercle inconnu', fakeAsync(() => {
      run(service.archiveCircle('cercle-inconnu'));

      expect(run(service.listCircles()).content.map(c => c.status)).toEqual(['ACTIVE', 'PAUSED']);
    }));

    // ---- Compte rendu ---------------------------------------------------------------

    it('génère un compte rendu structuré en décisions et actions', fakeAsync(() => {
      const minutes = run(service.generateMinutes('c1', 'mt1', {
        transcript: 'Discussion sur les rebuts de la ligne 3.'
      }));

      expect(minutes.summary).toBeTruthy();
      expect(minutes.decisions.length).toBeGreaterThan(0);
      // Chaque action porte un responsable suggéré : un compte rendu sans
      // assignation ne produit aucun suivi.
      expect(minutes.actions.every(a => !!a.label && !!a.suggestedAssignee)).toBeTrue();
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: CirclesService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CirclesService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('pagine la liste et n\'ajoute le statut que s\'il est fourni', () => {
      service.listCircles().subscribe();
      const plain = http.expectOne(r => r.url === BASE);
      expect(plain.request.params.get('page')).toBe('0');
      expect(plain.request.params.get('size')).toBe('50');
      expect(plain.request.params.has('status')).toBeFalse();
      plain.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.listCircles(3, 15, 'ARCHIVED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('page')).toBe('3');
      expect(filtered.request.params.get('size')).toBe('15');
      expect(filtered.request.params.get('status')).toBe('ARCHIVED');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('crée en POST, lit en GET, met à jour en PATCH et supprime en DELETE', () => {
      const body = circleReq();
      service.createCircle(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as CircleResponse);

      service.getCircle('c-1').subscribe();
      const get = http.expectOne(`${BASE}/c-1`);
      expect(get.request.method).toBe('GET');
      get.flush({} as CircleResponse);

      service.updateCircle('c-1', { name: 'n' }).subscribe();
      const patch = http.expectOne(`${BASE}/c-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ name: 'n' });
      patch.flush({} as CircleResponse);

      service.deleteCircle('c-1').subscribe();
      const del = http.expectOne(`${BASE}/c-1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });

    it('poste membres, réunions et propositions sous la ressource du cercle', () => {
      service.addMember('c-1', { userId: 'u1', role: 'MEMBER' }).subscribe();
      const member = http.expectOne(`${BASE}/c-1/members`);
      expect(member.request.method).toBe('POST');
      member.flush({} as CircleMemberResponse);

      const meeting = meetingReq();
      service.addMeeting('c-1', meeting).subscribe();
      const meetingReqHttp = http.expectOne(`${BASE}/c-1/meetings`);
      expect(meetingReqHttp.request.body).toEqual(meeting);
      meetingReqHttp.flush({} as CircleMeetingResponse);

      const proposal = proposalReq();
      service.addProposal('c-1', proposal).subscribe();
      const proposalReqHttp = http.expectOne(`${BASE}/c-1/proposals`);
      expect(proposalReqHttp.request.body).toEqual(proposal);
      proposalReqHttp.flush({} as CircleProposalResponse);
    });

    it('fait transiter une proposition en PATCH sur son propre sous-chemin', () => {
      const transitions: Array<[string, () => void, unknown]> = [
        ['review', () => service.reviewProposal('c-1', 'p-1').subscribe(), {}],
        ['approve',
          () => service.approveProposal('c-1', 'p-1', { validatedBy: 'u1' }).subscribe(),
          { validatedBy: 'u1' }],
        ['reject',
          () => service.rejectProposal('c-1', 'p-1', { validatedBy: 'u1', reason: 'r' }).subscribe(),
          { validatedBy: 'u1', reason: 'r' }],
        ['implement', () => service.implementProposal('c-1', 'p-1').subscribe(), {}],
        ['impact',
          () => service.recordImpact('c-1', 'p-1', { impactNote: 'n' }).subscribe(),
          { impactNote: 'n' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/c-1/proposals/p-1/${path}`);
        expect(req.request.method).withContext(path).toBe('PATCH');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as CircleProposalResponse);
      });
    });

    it('fait transiter le cercle en PATCH sur son propre sous-chemin', () => {
      const transitions: Array<[string, () => void]> = [
        ['pause', () => service.pauseCircle('c-1').subscribe()],
        ['resume', () => service.resumeCircle('c-1').subscribe()],
        ['archive', () => service.archiveCircle('c-1').subscribe()]
      ];

      transitions.forEach(([path, call]) => {
        call();
        const req = http.expectOne(`${BASE}/c-1/${path}`);
        expect(req.request.method).withContext(path).toBe('PATCH');
        expect(req.request.body).withContext(path).toEqual({});
        req.flush({} as CircleResponse);
      });
    });

    it('génère le compte rendu sous la réunion', () => {
      service.generateMinutes('c-1', 'mt-1', { transcript: 't' }).subscribe();

      const req = http.expectOne(`${BASE}/c-1/meetings/mt-1/minutes/generate`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ transcript: 't' });
      req.flush({ summary: '', decisions: [], actions: [] } as MeetingMinutes);
    });

    it('envoie l\'enregistrement en multipart, sans poser le type de contenu', () => {
      const file = new File(['audio'], 'reunion.mp3', { type: 'audio/mpeg' });

      service.transcribeMeeting('c-1', 'mt-1', file).subscribe();

      const req = http.expectOne(`${BASE}/c-1/meetings/mt-1/minutes/transcribe`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBeTrue();
      // Le navigateur doit poser lui-même la frontière multipart : un
      // Content-Type explicite la ferait manquer et le corps serait illisible.
      expect(req.request.headers.has('Content-Type')).toBeFalse();
      req.flush({ text: '', language: null, durationMs: 0 } as MeetingTranscript);
    });

    it('transmet la langue de transcription quand elle est précisée', () => {
      const file = new File(['audio'], 'reunion.mp3', { type: 'audio/mpeg' });

      service.transcribeMeeting('c-1', 'mt-1', file, 'fr').subscribe();

      const req = http.expectOne(
        `${BASE}/c-1/meetings/mt-1/minutes/transcribe?language=fr`
      );
      expect(req.request.method).toBe('POST');
      req.flush({ text: '', language: 'fr', durationMs: 0 } as MeetingTranscript);
    });
  });
});
