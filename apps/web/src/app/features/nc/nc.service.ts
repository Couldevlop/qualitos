import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, delay, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import {
  CreateNcRequest,
  EscalateCapaNcRequest,
  NcCategory,
  NcPage,
  NcPhoto,
  NcResponse,
  NcSeverity,
  NcStatus,
  ResolveNcRequest,
  StartAnalysisNcRequest,
  UpdateNcRequest
} from './nc.types';

export interface NcListFilters {
  status?: NcStatus;
  severity?: NcSeverity;
  category?: NcCategory;
}

@Injectable({ providedIn: 'root' })
export class NcService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  private readonly mockStore: NcResponse[] = this.seedMockNcs();

  constructor(
    private readonly http: HttpClient,
    private readonly connectivity: ConnectivityService,
    private readonly offlineQueue: OfflineQueueService
  ) {}

  listNcs(page = 0, size = 50, filters: NcListFilters = {}): Observable<NcPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(filters)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.severity) params = params.set('severity', filters.severity);
    if (filters.category) params = params.set('category', filters.category);
    return this.http.get<NcPage>(this.endpoint, { params });
  }

  getNc(id: string): Observable<NcResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(n => n.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<NcResponse>(`${this.endpoint}/${id}`);
  }

  createNc(input: CreateNcRequest): Observable<NcResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const nc: NcResponse = {
        id: 'nc-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: 'NC-2026-' + String(1000 + this.mockStore.length + 1),
        title: input.title,
        description: input.description,
        category: input.category,
        severity: input.severity,
        status: 'OPEN',
        detectedAt: input.detectedAt ?? now,
        zone: input.zone,
        geoLat: input.geoLat,
        geoLng: input.geoLng,
        photoUrls: input.photoUrls,
        reporterId: input.reporterId,
        createdAt: now,
        updatedAt: now
      };
      this.mockStore.unshift(nc);
      return of(nc).pipe(delay(200));
    }
    // Offline-first terrain (§4.3, §15.2-15.3) : une NC saisie en zone blanche
    // est mise en file et créée pour de vrai à la resynchronisation — réponse
    // optimiste en attendant. Les transitions de workflow restent online-only.
    if (!this.connectivity.isOnline()) {
      return this.enqueueCreate(input);
    }
    return this.http.post<NcResponse>(this.endpoint, input).pipe(
      catchError(err => this.isNetworkError(err) ? this.enqueueCreate(input) : throwError(() => err))
    );
  }

  updateNc(id: string, input: UpdateNcRequest): Observable<NcResponse> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      if (n) {
        if (input.title !== undefined) n.title = input.title;
        if (input.description !== undefined) n.description = input.description;
        if (input.category !== undefined) n.category = input.category;
        if (input.severity !== undefined) n.severity = input.severity;
        if (input.zone !== undefined) n.zone = input.zone;
        if (input.geoLat !== undefined) n.geoLat = input.geoLat;
        if (input.geoLng !== undefined) n.geoLng = input.geoLng;
        if (input.photoUrls !== undefined) n.photoUrls = input.photoUrls;
        n.updatedAt = new Date().toISOString();
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<NcResponse>(`${this.endpoint}/${id}`, input);
  }

  startAnalysis(id: string, input: StartAnalysisNcRequest = {}): Observable<NcResponse> {
    return this.transition(id, 'UNDER_ANALYSIS', 'start-analysis', input);
  }

  defineAction(id: string): Observable<NcResponse> {
    return this.transition(id, 'ACTION_DEFINED', 'define-action', {});
  }

  resolve(id: string, input: ResolveNcRequest): Observable<NcResponse> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (n) {
        n.status = 'RESOLVED';
        n.resolutionNote = input.resolutionNote;
        n.resolvedAt = now;
        n.updatedAt = now;
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<NcResponse>(`${this.endpoint}/${id}/resolve`, input);
  }

  close(id: string): Observable<NcResponse> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (n) {
        n.status = 'CLOSED';
        n.closedAt = now;
        n.updatedAt = now;
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<NcResponse>(`${this.endpoint}/${id}/close`, {});
  }

  cancel(id: string): Observable<NcResponse> {
    return this.transition(id, 'CANCELLED', 'cancel', {});
  }

  escalateToCapa(id: string, input: EscalateCapaNcRequest): Observable<NcResponse> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (n) {
        n.capaCaseId = 'capa-' + Math.random().toString(36).slice(2, 9);
        n.updatedAt = now;
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<NcResponse>(`${this.endpoint}/${id}/escalate-capa`, input);
  }

  // ---- photos (upload binaire, ONLINE-ONLY ce sprint) ------------------------
  // Contrairement à createNc, l'upload de fichiers ne bascule PAS en file
  // hors-ligne : un binaire ne se sérialise pas raisonnablement dans IndexedDB
  // de la file existante (voir docs/web-pwa-offline.md § limites). L'UI désactive
  // le bouton d'ajout hors-ligne ; l'erreur réseau est propagée telle quelle.

  /** Liste les photos d'une NC (URLs présignées ~15 min). */
  listPhotos(ncId: string): Observable<NcPhoto[]> {
    if (environment.useMockApi) {
      return of(this.mockPhotos(ncId)).pipe(delay(120));
    }
    return this.http.get<NcPhoto[]>(`${this.endpoint}/${ncId}/photos`);
  }

  /** Téléverse une photo (multipart, champ 'file'). 201 → métadonnées de la photo. */
  uploadPhoto(ncId: string, file: File): Observable<NcPhoto> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const photo: NcPhoto = {
        id: 'photo-' + Math.random().toString(36).slice(2, 9),
        url: URL.createObjectURL(file),
        contentType: file.type || 'application/octet-stream',
        sizeBytes: file.size,
        originalFilename: file.name,
        createdAt: now
      };
      this.mockPhotoStore(ncId).unshift(photo);
      return of(photo).pipe(delay(250));
    }
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<NcPhoto>(`${this.endpoint}/${ncId}/photos`, form);
  }

  /** Supprime une photo. 204 → void. */
  deletePhoto(ncId: string, photoId: string): Observable<void> {
    if (environment.useMockApi) {
      const store = this.mockPhotoStore(ncId);
      const idx = store.findIndex(p => p.id === photoId);
      if (idx >= 0) store.splice(idx, 1);
      return of(void 0).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${ncId}/photos/${photoId}`);
  }

  private transition(
    id: string,
    targetStatus: NcStatus,
    pathSegment: 'start-analysis' | 'define-action' | 'cancel',
    body: unknown
  ): Observable<NcResponse> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      if (n) {
        n.status = targetStatus;
        n.updatedAt = new Date().toISOString();
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<NcResponse>(`${this.endpoint}/${id}/${pathSegment}`, body ?? {});
  }

  // ---- offline (file d'attente + réponse optimiste) ---------------------------

  private enqueueCreate(input: CreateNcRequest): Observable<NcResponse> {
    const now = new Date().toISOString();
    // Label NON-PII : catégorie + sévérité seulement, jamais le titre saisi.
    return this.offlineQueue
      .enqueue('POST', this.endpoint, input,
        `Création non-conformité — ${input.category} (${input.severity})`)
      .pipe(map(op => ({
        id: 'offline-' + op.id,
        reference: 'NC-EN-ATTENTE',
        title: input.title,
        description: input.description,
        category: input.category,
        severity: input.severity,
        status: 'OPEN' as NcStatus,
        detectedAt: input.detectedAt ?? now,
        zone: input.zone,
        geoLat: input.geoLat,
        geoLng: input.geoLng,
        photoUrls: input.photoUrls,
        reporterId: input.reporterId,
        createdAt: now,
        updatedAt: now,
        pendingSync: true
      })));
  }

  /** status 0 = la requête n'a pas atteint le serveur (coupure pendant l'envoi). */
  private isNetworkError(err: unknown): boolean {
    return err instanceof HttpErrorResponse && err.status === 0;
  }

  /** Mémoire de photos par NC (mock mode uniquement). */
  private readonly mockPhotoStores = new Map<string, NcPhoto[]>();

  private mockPhotoStore(ncId: string): NcPhoto[] {
    let store = this.mockPhotoStores.get(ncId);
    if (!store) { store = []; this.mockPhotoStores.set(ncId, store); }
    return store;
  }

  private mockPhotos(ncId: string): NcPhoto[] {
    return [...this.mockPhotoStore(ncId)];
  }

  private mockPage(filters: NcListFilters): NcPage {
    const filtered = this.mockStore.filter(n =>
      (!filters.status || n.status === filters.status) &&
      (!filters.severity || n.severity === filters.severity) &&
      (!filters.category || n.category === filters.category));
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private seedMockNcs(): NcResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'nc-1', tenantId: 'demo-tenant', reference: 'NC-2026-1001',
        title: 'Étiquetage lot manquant — ligne 1',
        description: 'Palettes expédiées sans étiquette de traçabilité.',
        category: 'PROCESS', severity: 'MAJOR', status: 'OPEN',
        detectedAt: now, zone: 'Atelier conditionnement A',
        createdAt: now, updatedAt: now
      },
      {
        id: 'nc-2', tenantId: 'demo-tenant', reference: 'NC-2026-1002',
        title: 'Fuite hydraulique presse 4',
        description: 'Risque sécurité opérateur, zone glissante.',
        category: 'SAFETY', severity: 'CRITICAL', status: 'UNDER_ANALYSIS',
        detectedAt: now, zone: 'Atelier mécanique', geoLat: 48.8566, geoLng: 2.3522,
        rootCause: 'Joint d\'étanchéité usé.',
        createdAt: now, updatedAt: now
      },
      {
        id: 'nc-3', tenantId: 'demo-tenant', reference: 'NC-2026-1003',
        title: 'Certificat matière non conforme — fournisseur Acme',
        category: 'SUPPLIER', severity: 'MINOR', status: 'RESOLVED',
        detectedAt: now, zone: 'Réception',
        resolutionNote: 'Lot retourné, certificat conforme reçu.', resolvedAt: now,
        createdAt: now, updatedAt: now
      }
    ];
  }
}
