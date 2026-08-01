import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { FriaService } from './fria.service';
import { FriaDraftRequest, FriaEditRequest, FriaView } from './fria.types';

/**
 * Analyses d'impact sur les droits fondamentaux (AI Act, art. 27).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → SUBMITTED → APPROVED → ARCHIVED, le renvoi en correction depuis
 * SUBMITTED, et le figement de l'évaluation dès qu'elle quitte le brouillon —
 * une analyse soumise ne s'édite plus, faute de quoi elle perdrait toute valeur
 * probatoire. Les deux modes sont testés.
 */
describe('FriaService', () => {

  const AUTHOR = '00000000-0000-0000-0000-000000000999';
  const APPROVER = '00000000-0000-0000-0000-000000000888';
  const BASE = `${environment.apiBaseUrl}/api/v1/ai-act/fria`;

  const draftReq = (over: Partial<FriaDraftRequest> = {}): FriaDraftRequest => ({
    reference: 'FRIA-2026-NEW-900',
    aiSystemId: '44444444-4444-4444-4444-444444444444',
    processDescription: 'Tri automatisé des demandes d\'aide sociale.',
    affectedPersonsCategories: 'Demandeurs d\'aide sociale du département.',
    specificRisks: 'Refus injustifié, biais socio-économique, opacité de la décision.',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<FriaEditRequest> = {}): FriaEditRequest => ({
    processDescription: 'Tri automatisé des demandes d\'aide sociale — périmètre réduit.',
    affectedPersonsCategories: 'Demandeurs d\'aide sociale, hors renouvellements.',
    specificRisks: 'Refus injustifié, biais socio-économique.',
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: FriaService;
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
      service = TestBed.inject(FriaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les analyses pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(2);

      expect(run(service.list('APPROVED')).value?.map(f => f.reference)).toEqual(['FRIA-2026-DIAG-001']);
      expect(run(service.list('ARCHIVED')).value).toEqual([]);
    }));

    it('filtre par système d\'IA évalué', fakeAsync(() => {
      expect(run(service.listByAiSystem('22222222-2222-2222-2222-222222222222')).value?.map(f => f.reference))
        .toEqual(['FRIA-2026-HRGD-002']);
      expect(run(service.listByAiSystem('système-inconnu')).value).toEqual([]);
    }));

    it('résout une analyse par identifiant et par référence, et refuse une clé inconnue', fakeAsync(() => {
      expect(run(service.get('fria-seed-001')).value?.reference).toBe('FRIA-2026-DIAG-001');
      expect(run(service.getByReference('FRIA-2026-HRGD-002')).value?.id).toBe('fria-seed-002');

      expect(run(service.get('fria-inexistante')).error?.status).toBe(404);
      expect(run(service.getByReference('FRIA-ABSENTE')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('fria-seed-001')).value!;
      first.processDescription = 'description falsifiée';

      expect(run(service.get('fria-seed-001')).value?.processDescription).toContain('diagnostic radiologique');
    }));

    // ---- Création ----------------------------------------------------------

    it('refuse une référence déjà utilisée', fakeAsync(() => {
      expect(run(service.draft(draftReq({ reference: 'FRIA-2026-DIAG-001' }))).error?.status).toBe(409);
    }));

    it('crée un brouillon en tête de liste, sans trace de soumission', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      expect(created.status).toBe('DRAFT');
      expect(created.submittedAt).toBeNull();
      expect(created.approvedAt).toBeNull();
      expect(run(service.list()).value?.[0].reference).toBe('FRIA-2026-NEW-900');
    }));

    it('normalise en null les champs facultatifs omis', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      expect(created.deploymentDurationDescription).toBeNull();
      expect(created.mitigationMeasures).toBeNull();
      expect(created.humanOversightMeasures).toBeNull();
      expect(created.complaintMechanismDescription).toBeNull();
    }));

    // ---- Édition -----------------------------------------------------------

    it('n\'édite qu\'un brouillon, et seulement s\'il existe', fakeAsync(() => {
      expect(run(service.edit('fria-inexistante', editReq())).error?.status).toBe(404);
      // seed-002 est SUBMITTED : l'évaluation est figée pour rester probante.
      expect(run(service.edit('fria-seed-002', editReq())).error?.status).toBe(409);
    }));

    it('remplace les champs du brouillon, une omission valant effacement', fakeAsync(() => {
      const created = run(service.draft(draftReq({
        mitigationMeasures: 'Recours humain systématique.'
      }))).value!;

      const edited = run(service.edit(created.id, editReq({
        humanOversightMeasures: 'Validation par un agent instructeur.'
      }))).value!;

      expect(edited.processDescription).toContain('périmètre réduit');
      expect(edited.humanOversightMeasures).toBe('Validation par un agent instructeur.');
      // L'édition porte l'état complet du formulaire.
      expect(edited.mitigationMeasures).toBeNull();
    }));

    // ---- Soumission --------------------------------------------------------

    it('ne soumet qu\'un brouillon existant', fakeAsync(() => {
      const req = { submittedByUserId: AUTHOR };

      expect(run(service.submit('fria-inexistante', req)).error?.status).toBe(404);
      expect(run(service.submit('fria-seed-002', req)).error?.status).toBe(409);
    }));

    it('soumet le brouillon et horodate la soumission', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      const submitted = run(service.submit(created.id, { submittedByUserId: AUTHOR })).value!;

      expect(submitted.status).toBe('SUBMITTED');
      expect(submitted.submittedByUserId).toBe(AUTHOR);
      expect(submitted.submittedAt).not.toBeNull();
    }));

    // ---- Approbation -------------------------------------------------------

    it('n\'approuve qu\'une analyse soumise et existante', fakeAsync(() => {
      const req = { approvedByUserId: APPROVER };

      expect(run(service.approve('fria-inexistante', req)).error?.status).toBe(404);
      expect(run(service.approve('fria-seed-001', req)).error?.status).toBe(409);
    }));

    it('approuve avec ou sans note, et trace l\'approbateur', fakeAsync(() => {
      const approved = run(service.approve('fria-seed-002', {
        approvedByUserId: APPROVER,
        approvalNotes: 'Conforme à l\'article 27, revue annuelle recommandée.'
      })).value!;

      expect(approved.status).toBe('APPROVED');
      expect(approved.approvedByUserId).toBe(APPROVER);
      expect(approved.approvalNotes).toContain('article 27');
      expect(approved.approvedAt).not.toBeNull();
    }));

    it('accepte une approbation sans note et la stocke à null', fakeAsync(() => {
      expect(run(service.approve('fria-seed-002', { approvedByUserId: APPROVER })).value?.approvalNotes)
        .toBeNull();
    }));

    // ---- Renvoi en correction ----------------------------------------------

    it('ne renvoie en correction qu\'une analyse soumise et existante', fakeAsync(() => {
      const req = { reason: 'Risques spécifiques insuffisamment détaillés.' };

      expect(run(service.returnToDraft('fria-inexistante', req)).error?.status).toBe(404);
      expect(run(service.returnToDraft('fria-seed-001', req)).error?.status).toBe(409);
    }));

    it('efface la trace de soumission au renvoi et conserve le motif', fakeAsync(() => {
      const returned = run(service.returnToDraft('fria-seed-002', {
        reason: 'Risques spécifiques insuffisamment détaillés.'
      })).value!;

      expect(returned.status).toBe('DRAFT');
      expect(returned.approvalNotes).toContain('insuffisamment détaillés');
      // Sans cet effacement, la fiche afficherait une soumission qui n'a plus cours.
      expect(returned.submittedAt).toBeNull();
      expect(returned.submittedByUserId).toBeNull();
    }));

    it('rend le brouillon renvoyé de nouveau éditable', fakeAsync(() => {
      run(service.returnToDraft('fria-seed-002', { reason: 'À compléter.' }));

      expect(run(service.edit('fria-seed-002', editReq())).value?.processDescription)
        .toContain('périmètre réduit');
    }));

    // ---- Archivage / suppression -------------------------------------------

    it('n\'archive qu\'une analyse approuvée et existante', fakeAsync(() => {
      const req = { reason: 'Système retiré du service.' };

      expect(run(service.archive('fria-inexistante', req)).error?.status).toBe(404);
      expect(run(service.archive('fria-seed-002', req)).error?.status).toBe(409);

      const archived = run(service.archive('fria-seed-001', req)).value!;
      expect(archived.status).toBe('ARCHIVED');
      expect(archived.archivedReason).toBe('Système retiré du service.');
      expect(archived.effectiveTo).not.toBeNull();
    }));

    it('supprime une analyse et refuse une suppression inconnue', fakeAsync(() => {
      expect(run(service.delete('fria-inexistante')).error?.status).toBe(404);

      run(service.delete('fria-seed-002'));

      expect(run(service.list()).value?.length).toBe(1);
      expect(run(service.get('fria-seed-002')).error?.status).toBe(404);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: FriaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(FriaService);
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

      service.list('SUBMITTED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('SUBMITTED');
      filtered.flush([]);
    });

    it('interroge les vues dérivées avec leurs paramètres', () => {
      service.listByAiSystem('sys-1').subscribe();
      const bySystem = http.expectOne(r => r.url === `${BASE}/by-system`);
      expect(bySystem.request.params.get('aiSystemId')).toBe('sys-1');
      bySystem.flush([]);

      service.getByReference('FRIA-2026-DIAG-001').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('FRIA-2026-DIAG-001');
      byRef.flush({} as FriaView);
    });

    it('lit une fiche par identifiant', () => {
      service.get('f-1').subscribe();

      const req = http.expectOne(`${BASE}/f-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as FriaView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = draftReq();
      service.draft(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as FriaView);

      const edit = editReq();
      service.edit('f-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/f-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as FriaView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const transitions: Array<[string, () => void, unknown]> = [
        ['submit',
          () => service.submit('f-1', { submittedByUserId: AUTHOR }).subscribe(),
          { submittedByUserId: AUTHOR }],
        ['approve',
          () => service.approve('f-1', { approvedByUserId: APPROVER }).subscribe(),
          { approvedByUserId: APPROVER }],
        // Le renvoi en correction est posté sur « return » : le verbe métier
        // (returnToDraft) et le chemin HTTP ne coïncident pas.
        ['return',
          () => service.returnToDraft('f-1', { reason: 'r' }).subscribe(),
          { reason: 'r' }],
        ['archive',
          () => service.archive('f-1', { reason: 'r' }).subscribe(),
          { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/f-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as FriaView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('f-1').subscribe();

      const req = http.expectOne(`${BASE}/f-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
