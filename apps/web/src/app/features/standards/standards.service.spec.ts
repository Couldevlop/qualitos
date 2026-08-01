import { HttpResponse, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { StandardsService } from './standards.service';
import {
  AdoptionResponse,
  AlignmentReport,
  AuditBlancReport,
  CertificationBlancReport,
  DocumentTemplate,
  DossierResponse,
  EvidenceResponse,
  LinkEvidenceRequest,
  ProcessTemplate,
  RoadmapSummary,
  StandardDetail,
  StandardRevision,
  StoryboardResponse
} from './standards.types';

/**
 * Standards Hub (§8) — catalogue normatif et dossiers de certification.
 *
 * Deux points de vigilance structurent cette spec.
 *
 * D'abord, le service n'est simulé que sur DEUX lectures (catalogue et
 * adoptions) : tout le reste appelle le serveur en toutes circonstances. Sans
 * couverture, une simulation ajoutée par erreur sur une route de preuves ou de
 * dossier passerait inaperçue et rendrait un dossier de certification fabriqué.
 *
 * Ensuite, l'arborescence des chemins distingue ce qui relève de la NORME
 * (`/{standardId}/...` — modèles, processus, révisions) et ce qui relève de
 * l'ADOPTION par le tenant (`/adoptions/{id}/...` — feuille de route, preuves,
 * dossier). Les confondre exposerait les preuves d'un tenant sur une ressource
 * de catalogue.
 */
describe('StandardsService', () => {

  const BASE = `${environment.apiBaseUrl}/api/v1/standards`;

  // ------------------------------------------------------------------------
  // Mode démo
  // ------------------------------------------------------------------------
  describe('en mode démo', () => {
    let service: StandardsService;
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
      service = TestBed.inject(StandardsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('sert un catalogue de démonstration sans appeler le serveur', fakeAsync(() => {
      const page = run(service.listCatalog());

      expect(page.totalElements).toBe(2);
      expect(page.content.map(s => s.code)).toEqual(['iso-9001', 'iso-27001']);
    }));

    it('sert les adoptions de démonstration sans appeler le serveur', fakeAsync(() => {
      const page = run(service.listAdoptions());

      expect(page.totalElements).toBe(1);
      expect(page.content[0].standardCode).toBe('iso-9001');
    }));

    it('interroge malgré tout le serveur pour tout le reste', fakeAsync(() => {
      // Un dossier de certification, des preuves ou un audit blanc simulés
      // seraient des documents fabriqués : ces routes n'ont volontairement
      // aucune branche de démonstration.
      service.getAdoption('ad1').subscribe();
      http.expectOne(`${BASE}/adoptions/ad1`).flush({} as AdoptionResponse);

      service.listEvidence('ad1').subscribe();
      http.expectOne(`${BASE}/adoptions/ad1/evidence`).flush([]);

      service.generateDossier('ad1').subscribe();
      http.expectOne(`${BASE}/adoptions/ad1/dossier`).flush({} as DossierResponse);

      service.getAuditBlanc('ad1').subscribe();
      http.expectOne(`${BASE}/adoptions/ad1/audit-blanc`).flush({} as AuditBlancReport);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: StandardsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(StandardsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    // ---- Catalogue ---------------------------------------------------------

    it('pagine le catalogue', () => {
      service.listCatalog().subscribe();
      const byDefault = http.expectOne(r => r.url === BASE);
      expect(byDefault.request.params.get('page')).toBe('0');
      expect(byDefault.request.params.get('size')).toBe('50');
      byDefault.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.listCatalog(3, 10).subscribe();
      const paged = http.expectOne(r => r.url === BASE);
      expect(paged.request.params.get('page')).toBe('3');
      expect(paged.request.params.get('size')).toBe('10');
      paged.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('lit le détail d\'une norme', () => {
      service.getStandardDetail('s-1').subscribe();

      const req = http.expectOne(`${BASE}/s-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as StandardDetail);
    });

    // ---- Adoptions ---------------------------------------------------------

    it('liste, lit, crée et démarre une adoption', () => {
      service.listAdoptions().subscribe();
      const list = http.expectOne(`${BASE}/adoptions`);
      expect(list.request.method).toBe('GET');
      list.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.getAdoption('a-1').subscribe();
      const one = http.expectOne(`${BASE}/adoptions/a-1`);
      expect(one.request.method).toBe('GET');
      one.flush({} as AdoptionResponse);

      const body = { standardId: 's-1', scopeDescription: 'SMQ siège' };
      service.adopt(body).subscribe();
      const post = http.expectOne(`${BASE}/adoptions`);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as AdoptionResponse);

      service.startProgress('a-1').subscribe();
      const start = http.expectOne(`${BASE}/adoptions/a-1/start`);
      expect(start.request.method).toBe('PATCH');
      expect(start.request.body).toEqual({});
      start.flush({} as AdoptionResponse);
    });

    // ---- Feuille de route --------------------------------------------------

    it('lit la feuille de route et met à jour une étape', () => {
      service.getRoadmap('a-1').subscribe();
      const roadmap = http.expectOne(`${BASE}/adoptions/a-1/roadmap`);
      expect(roadmap.request.method).toBe('GET');
      roadmap.flush({} as RoadmapSummary);

      service.updateStage('a-1', 'st-3', { status: 'DONE' }).subscribe();
      const stage = http.expectOne(`${BASE}/adoptions/a-1/roadmap/st-3`);
      expect(stage.request.method).toBe('PATCH');
      expect(stage.request.body).toEqual({ status: 'DONE' });
      stage.flush({});
    });

    // ---- Alignement, audit blanc, récit -------------------------------------

    it('lit l\'alignement et l\'audit blanc de l\'adoption', () => {
      service.getAlignment('a-1').subscribe();
      const alignment = http.expectOne(`${BASE}/adoptions/a-1/alignment`);
      expect(alignment.request.method).toBe('GET');
      alignment.flush({} as AlignmentReport);

      service.getAuditBlanc('a-1').subscribe();
      const audit = http.expectOne(`${BASE}/adoptions/a-1/audit-blanc`);
      expect(audit.request.method).toBe('GET');
      audit.flush({} as AuditBlancReport);
    });

    it('déclenche la certification à blanc et le récit narratif en POST', () => {
      service.runCertificationBlanc('a-1').subscribe();
      const certif = http.expectOne(`${BASE}/adoptions/a-1/certification-blanc`);
      expect(certif.request.method).toBe('POST');
      expect(certif.request.body).toEqual({});
      certif.flush({} as CertificationBlancReport);

      service.generateStoryboard('a-1').subscribe();
      const story = http.expectOne(`${BASE}/adoptions/a-1/storyboard`);
      expect(story.request.method).toBe('POST');
      story.flush({} as StoryboardResponse);
    });

    // ---- Preuves ------------------------------------------------------------

    it('gère les preuves sous l\'adoption, jamais sous la norme', () => {
      // Les preuves appartiennent au tenant : les rattacher au catalogue les
      // exposerait à tous les tenants qui consultent la même norme.
      service.listEvidence('a-1').subscribe();
      const list = http.expectOne(`${BASE}/adoptions/a-1/evidence`);
      expect(list.request.method).toBe('GET');
      list.flush([]);

      const body: LinkEvidenceRequest = {
        requirementId: 'r-1', evidenceType: 'DOCUMENT', evidenceRefId: 'doc-1', linkedBy: 'u1'
      };
      service.linkEvidence('a-1', body).subscribe();
      const link = http.expectOne(`${BASE}/adoptions/a-1/evidence`);
      expect(link.request.method).toBe('POST');
      expect(link.request.body).toEqual(body);
      link.flush({} as EvidenceResponse);

      service.unlinkEvidence('a-1', 'e-1').subscribe();
      const unlink = http.expectOne(`${BASE}/adoptions/a-1/evidence/e-1`);
      expect(unlink.request.method).toBe('DELETE');
      unlink.flush({});
    });

    it('génère le dossier de certification en POST sur l\'adoption', () => {
      service.generateDossier('a-1').subscribe();

      const req = http.expectOne(`${BASE}/adoptions/a-1/dossier`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({} as DossierResponse);
    });

    // ---- Bibliothèque de la norme --------------------------------------------

    it('lit modèles, processus et révisions sous la NORME', () => {
      service.listDocumentTemplates('s-1').subscribe();
      const docs = http.expectOne(`${BASE}/s-1/document-templates`);
      expect(docs.request.method).toBe('GET');
      docs.flush([] as DocumentTemplate[]);

      service.listProcessTemplates('s-1').subscribe();
      const processes = http.expectOne(`${BASE}/s-1/process-templates`);
      expect(processes.request.method).toBe('GET');
      processes.flush([] as ProcessTemplate[]);

      service.listRevisions('s-1').subscribe();
      const revisions = http.expectOne(`${BASE}/s-1/revisions`);
      expect(revisions.request.method).toBe('GET');
      revisions.flush([] as StandardRevision[]);
    });

    it('télécharge un modèle en binaire, réponse complète observée', () => {
      let response: HttpResponse<Blob> | undefined;

      service.downloadDocumentTemplate('s-1', 't-1').subscribe(r => (response = r));

      const req = http.expectOne(`${BASE}/s-1/document-templates/t-1/download`);
      // `responseType: blob` est indispensable : en JSON par défaut, un .docx
      // serait corrompu à la lecture.
      expect(req.request.responseType).toBe('blob');
      req.flush(new Blob(['contenu']), {
        headers: { 'Content-Disposition': 'attachment; filename="manuel-qualite.docx"' }
      });

      // Recevoir un `HttpResponse` complet — et non le seul corps — prouve que
      // la réponse est observée entièrement, ce qui donne accès aux en-têtes
      // où se trouve le nom du fichier.
      expect(response?.body instanceof Blob).toBeTrue();
      expect(response?.headers.get('Content-Disposition')).toContain('manuel-qualite.docx');
    });

    it('demande un brouillon généré sous le modèle de la norme', () => {
      service.generateAiDraft('s-1', 't-1').subscribe();

      const req = http.expectOne(`${BASE}/s-1/document-templates/t-1/ai-draft`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ content: '' } as never);
    });
  });
});
