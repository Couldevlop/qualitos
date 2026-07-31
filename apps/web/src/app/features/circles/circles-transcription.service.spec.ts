import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CirclesService } from './circles.service';

/**
 * Transcription audio des réunions de Cercle (§3.3).
 *
 * L'endpoint Whisper existait côté ai-service mais n'était appelé par personne :
 * l'animateur devait coller le transcript à la main. Ces tests verrouillent le contrat
 * multipart désormais emprunté par le front.
 */
describe('CirclesService — transcription audio', () => {
  let service: CirclesService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/circles`;

  beforeEach(() => {
    // La transcription passe toujours par le vrai backend : rien à simuler côté front.
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

  function audioFile(): File {
    return new File([new Uint8Array([1, 2, 3])], 'reunion.webm', { type: 'audio/webm' });
  }

  it('poste l’enregistrement en multipart sur la réunion visée', (done) => {
    service.transcribeMeeting('c1', 'm1', audioFile()).subscribe(result => {
      expect(result.text).toBe('Bonjour à tous.');
      expect(result.language).toBe('fr');
      expect(result.durationMs).toBe(4200);
      done();
    });

    const req = http.expectOne(`${base}/c1/meetings/m1/minutes/transcribe`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('file')).toBeTruthy();
    // Le navigateur doit poser lui-même la frontière multipart.
    expect(req.request.headers.get('Content-Type')).toBeNull();
    req.flush({ text: 'Bonjour à tous.', language: 'fr', durationMs: 4200 });
  });

  it('transmet la langue demandée en paramètre de requête', (done) => {
    service.transcribeMeeting('c1', 'm1', audioFile(), 'fr').subscribe(() => done());

    const req = http.expectOne(r => r.url.startsWith(`${base}/c1/meetings/m1/minutes/transcribe`));
    expect(req.request.urlWithParams).toContain('language=fr');
    req.flush({ text: 'ok', language: 'fr', durationMs: 10 });
  });

  it('remonte l’indisponibilité du backend Whisper à l’appelant', (done) => {
    service.transcribeMeeting('c1', 'm1', audioFile()).subscribe({
      next: () => fail('la transcription ne devait pas aboutir'),
      error: err => {
        // 501 côté ai-service quand l'extra ml n'est pas installé : surtout ne pas
        // fabriquer un faux transcript.
        expect(err.status).toBe(501);
        done();
      }
    });

    http.expectOne(`${base}/c1/meetings/m1/minutes/transcribe`)
      .flush('transcription non disponible', { status: 501, statusText: 'Not Implemented' });
  });
});
