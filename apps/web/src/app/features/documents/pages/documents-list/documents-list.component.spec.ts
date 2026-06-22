import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentResponse } from '../../documents.types';
import { DocumentsListComponent } from './documents-list.component';

function doc(overrides: Partial<DocumentResponse> = {}): DocumentResponse {
  return {
    id: 'doc-1', tenantId: 't', code: 'PR-001', title: 'Procédure', type: 'PROCEDURE',
    status: 'ACTIVE', mandatoryRead: false, currentVersionId: 'v2',
    versions: [
      { id: 'v1', versionNumber: 1, status: 'SUPERSEDED' } as never,
      { id: 'v2', versionNumber: 2, status: 'APPROVED' } as never
    ],
    createdAt: '2026-06-01T00:00:00Z', updatedAt: '2026-06-01T00:00:00Z',
    ...overrides
  } as DocumentResponse;
}

describe('DocumentsListComponent', () => {
  let component: DocumentsListComponent;
  let fixture: ComponentFixture<DocumentsListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [DocumentsListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing and defaults the filter to ACTIVE', () => {
    fixture.detectChanges();
    expect(component.statusFilter.value).toBe('ACTIVE');
    expect(component.statuses).toEqual(['ACTIVE', 'ARCHIVED']);
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns)
      .toEqual(['code', 'title', 'type', 'currentVersion', 'status', 'mandatoryRead']);
  });

  it('resolves the current version from currentVersionId', () => {
    const v = component.currentVersion(doc());
    expect(v.number).toBe(2);
    expect(v.status).toBe('APPROVED');
  });

  it('falls back to the last version when currentVersionId is unknown', () => {
    const v = component.currentVersion(doc({ currentVersionId: 'missing' }));
    expect(v.number).toBe(2);
  });

  it('builds badge classes for version, status and type', () => {
    expect(component.versionBadge('APPROVED')).toBe('vbadge vbadge-approved');
    expect(component.versionBadge(undefined)).toBe('');
    expect(component.statusBadge('ARCHIVED')).toBe('badge badge-archived');
    expect(component.typeBadge('PROCEDURE')).toBe('tbadge tbadge-procedure');
  });

  it('clamps pagination bounds on onPage', () => {
    component.onPage({ pageIndex: -1, pageSize: 5000 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the document detail on open', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.open(doc({ id: 'doc-9' }));
    expect(nav).toHaveBeenCalledWith(['/documents', 'doc-9']);
  });
});
