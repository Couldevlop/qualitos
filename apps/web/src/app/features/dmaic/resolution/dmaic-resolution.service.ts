import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import {
  FailureType,
  ResolutionContent,
  ResolutionSettings,
  ResolutionSheet,
  SeverityLevel,
  SheetPhoto,
  SheetQuestion,
  emptyContent
} from './dmaic-resolution.types';

/**
 * La fiche de résolution d'un projet DMAIC et son paramétrage (ADR 0101).
 *
 * <p>Sans serveur (`useMockApi`), une fiche par projet vit en mémoire, mise en
 * forme par un paramétrage de démonstration resserré : de quoi parcourir
 * l'écran, pas le catalogue complet du classeur, que seul le serveur porte.
 */
@Injectable({ providedIn: 'root' })
export class DmaicResolutionService {

  private readonly projects = `${environment.apiBaseUrl}/api/v1/dmaic/projects`;
  private readonly settingsUrl = `${environment.apiBaseUrl}/api/v1/dmaic/resolution-settings`;

  private mockSettings: ResolutionSettings = DmaicResolutionService.demoSettings();
  private readonly mockSheets = new Map<string, { content: ResolutionContent; photos: SheetPhoto[]; updatedAt: string }>();

  constructor(private readonly http: HttpClient) {}

  sheet(projectId: string): Observable<ResolutionSheet> {
    if (environment.useMockApi) {
      return of(this.mockView(projectId)).pipe(delay(120));
    }
    return this.http.get<ResolutionSheet>(this.sheetUrl(projectId));
  }

  save(projectId: string, content: ResolutionContent): Observable<ResolutionSheet> {
    if (environment.useMockApi) {
      const prev = this.mockSheets.get(projectId);
      this.mockSheets.set(projectId, {
        content: structuredClone(content), photos: prev?.photos ?? [], updatedAt: new Date().toISOString()
      });
      return of(this.mockView(projectId)).pipe(delay(150));
    }
    return this.http.put<ResolutionSheet>(this.sheetUrl(projectId), content);
  }

  uploadPhoto(projectId: string, slot: number, file: File, remark: string | null): Observable<SheetPhoto> {
    if (environment.useMockApi) {
      const photo: SheetPhoto = {
        slot, contentType: file.type, sizeBytes: file.size, remark, url: URL.createObjectURL(file)
      };
      this.mockPhotos(projectId, photos => [...photos.filter(p => p.slot !== slot), photo]);
      return of(photo).pipe(delay(150));
    }
    const body = new FormData();
    body.append('file', file, file.name);
    if (remark) body.append('remark', remark);
    return this.http.post<SheetPhoto>(`${this.sheetUrl(projectId)}/photos/${slot}`, body);
  }

  updateRemark(projectId: string, slot: number, remark: string | null): Observable<SheetPhoto> {
    if (environment.useMockApi) {
      const found = this.mockSheets.get(projectId)?.photos.find(p => p.slot === slot);
      if (!found) return throwError(() => ({ status: 404 }));
      const updated = { ...found, remark };
      this.mockPhotos(projectId, photos => photos.map(p => (p.slot === slot ? updated : p)));
      return of(updated).pipe(delay(120));
    }
    return this.http.patch<SheetPhoto>(`${this.sheetUrl(projectId)}/photos/${slot}`, { remark });
  }

  deletePhoto(projectId: string, slot: number): Observable<void> {
    if (environment.useMockApi) {
      this.mockPhotos(projectId, photos => photos.filter(p => p.slot !== slot));
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.sheetUrl(projectId)}/photos/${slot}`);
  }

  /**
   * Le rapport d'action corrective client, en PDF. `observe: 'response'` pour
   * lire `Content-Disposition` : c'est le serveur qui nomme le fichier.
   */
  claimReport(projectId: string): Observable<HttpResponse<Blob>> {
    if (environment.useMockApi) {
      const pdf = new Blob(['%PDF-1.4\n%demo\n'], { type: 'application/pdf' });
      return of(new HttpResponse({
        body: pdf, status: 200,
        headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="rapport-client-dmaic.pdf"' })
      })).pipe(delay(150));
    }
    return this.http.get(`${this.sheetUrl(projectId)}/claim-report`, { responseType: 'blob', observe: 'response' });
  }

  settings(): Observable<ResolutionSettings> {
    if (environment.useMockApi) {
      return of(structuredClone(this.mockSettings)).pipe(delay(120));
    }
    return this.http.get<ResolutionSettings>(this.settingsUrl);
  }

  replaceSettings(questions: SheetQuestion[], failureTypes: FailureType[],
                  severities: SeverityLevel[]): Observable<ResolutionSettings> {
    if (environment.useMockApi) {
      this.mockSettings = structuredClone({ questions, failureTypes, severities, defaults: false });
      return this.settings();
    }
    return this.http.put<ResolutionSettings>(this.settingsUrl, { questions, failureTypes, severities });
  }

  resetSettings(): Observable<ResolutionSettings> {
    if (environment.useMockApi) {
      this.mockSettings = DmaicResolutionService.demoSettings();
      return this.settings();
    }
    return this.http.post<ResolutionSettings>(`${this.settingsUrl}/reset`, {});
  }

  private sheetUrl(projectId: string): string {
    return `${this.projects}/${encodeURIComponent(projectId)}/resolution`;
  }

  private mockPhotos(projectId: string, change: (photos: SheetPhoto[]) => SheetPhoto[]): void {
    const prev = this.mockSheets.get(projectId);
    this.mockSheets.set(projectId, {
      content: prev?.content ?? emptyContent(),
      photos: change(prev?.photos ?? []).sort((a, b) => a.slot - b.slot),
      updatedAt: prev?.updatedAt ?? new Date().toISOString()
    });
  }

  private mockView(projectId: string): ResolutionSheet {
    const stored = this.mockSheets.get(projectId);
    return {
      projectId,
      projectTitle: 'Projet DMAIC de démonstration',
      problemStatement: null,
      saved: !!stored,
      content: structuredClone(stored?.content ?? emptyContent()),
      settings: structuredClone(this.mockSettings),
      photos: [...(stored?.photos ?? [])],
      updatedAt: stored?.updatedAt ?? null
    };
  }

  private static demoSettings(): ResolutionSettings {
    return {
      defaults: true,
      questions: [
        { code: 'DEF-01', section: 'DEFINE', topic: 'WHAT', label: 'Quel est le problème et comment a-t-il été identifié ?', answerType: 'TEXT' },
        { code: 'DEF-04', section: 'DEFINE', topic: 'WHAT', label: 'Avons-nous des preuves matérielles du problème ?', answerType: 'YES_NO' },
        { code: 'DEF-05', section: 'DEFINE', topic: 'WHO', label: 'Qui est concerné par le problème ?', answerType: 'TEXT' },
        { code: 'DEF-09', section: 'DEFINE', topic: 'WHERE', label: 'Où le problème s\'est-il produit ?', answerType: 'TEXT' },
        { code: 'DEF-14', section: 'DEFINE', topic: 'HOW_MANY', label: 'Quelle est la quantité concernée ?', answerType: 'TEXT' },
        { code: 'MES-01', section: 'MEASURE', topic: null, label: 'Avez-vous vérifié le moyen de mesure et les données ?', answerType: 'YES_NO' },
        { code: 'CON-01', section: 'CONTAIN', topic: null, label: 'Avons-nous identifié tous les produits suspects ?', answerType: 'YES_NO_UNKNOWN' },
        { code: 'ANA-02', section: 'ANALYZE', topic: null, label: 'Vous êtes-vous affranchi de l\'« erreur humaine » pour trouver la cause racine ?', answerType: 'YES_NO' },
        { code: 'AME-07', section: 'IMPROVE', topic: null, label: 'Les solutions permettent-elles d\'éviter la récurrence du problème ?', answerType: 'YES_NO' },
        { code: 'CTL-04', section: 'CONTROL', topic: null, label: 'Un plan de surveillance a-t-il été développé ?', answerType: 'YES_NO_NA' }
      ],
      failureTypes: [
        { code: 'V01', label: 'Wrong part' },
        { code: 'V10', label: 'Dimensional issue' },
        { code: 'V20', label: 'Damaged part' }
      ],
      severities: [
        { level: 1, label: 'Sévère' },
        { level: 2, label: 'Modérée' },
        { level: 3, label: 'Mineure' }
      ]
    };
  }
}
