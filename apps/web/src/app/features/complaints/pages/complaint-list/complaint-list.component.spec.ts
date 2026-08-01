import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Complaint, ComplaintStatistics, SpringPage } from '../../complaints.types';
import { ComplaintListComponent } from './complaint-list.component';

describe('ComplaintListComponent', () => {
  let component: ComplaintListComponent;
  let fixture: ComponentFixture<ComplaintListComponent>;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/complaints`;

  const complaint = (over: Partial<Complaint> = {}): Complaint => ({
    id: 'c1', tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: 'Acme', customerEmail: null, customerExternalId: null,
    subject: 'Colis endommagé', description: null,
    severity: 'HIGH', category: 'DELIVERY', status: 'RECEIVED',
    supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: '2026-07-01T08:00:00Z', firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: '2026-07-01T08:00:00Z', updatedAt: null, ...over
  });

  const pageOf = (content: Complaint[]): SpringPage<Complaint> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 20
  });

  const statistics = (over: Partial<ComplaintStatistics> = {}): ComplaintStatistics => ({
    tenantId: 't1', total: 4, received: 2, underInvestigation: 1, responded: 0,
    resolved: 1, closed: 0, rejected: 0,
    product: 1, service: 0, delivery: 3, billing: 0, quality: 0, safety: 0, other: 0, ...over
  });

  /** Une passe de chargement = liste + compteurs (les deux partent en parallèle). */
  function flushLoad(rows: Complaint[] = [complaint()], stats = statistics()): { statusParam: string | null } {
    const listReq = http.expectOne(r => r.url === base);
    const statusParam = listReq.request.params.get('status');
    listReq.flush(pageOf(rows));
    http.expectOne(`${base}/statistics`).flush(stats);
    fixture.detectChanges();
    return { statusParam };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ComplaintListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ComplaintListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge la première page sans filtre et affiche le titre', () => {
    fixture.detectChanges();
    const { statusParam } = flushLoad();
    expect(statusParam).toBeNull();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Réclamations');
  });

  it('expose les lignes, le total et les compteurs', (done) => {
    fixture.detectChanges();
    flushLoad([complaint(), complaint({ id: 'c2', code: 'REC-2' })]);

    component.rows$.subscribe(rows => {
      expect(rows.length).toBe(2);
      component.total$.subscribe(total => {
        expect(total).toBe(2);
        component.tiles$.subscribe(tiles => {
          expect(tiles.length).toBe(7);
          expect(tiles[0].value).toBe(4);
          expect(tiles[1].status).toBe('RECEIVED');
          done();
        });
      });
    });
  });

  it('ne garde que les catégories réellement présentes, triées par volume', (done) => {
    fixture.detectChanges();
    flushLoad();
    component.categoryShares$.subscribe(shares => {
      expect(shares.map(s => s.category)).toEqual(['DELIVERY', 'PRODUCT']);
      expect(shares[0].count).toBe(3);
      expect(shares[0].percent).toBe(75);
      done();
    });
  });

  it('filtre par statut depuis une tuile puis revient à la liste complète', () => {
    fixture.detectChanges();
    flushLoad();

    component.filterByStatus('RESOLVED');
    expect(component.hasActiveFilter()).toBeTrue();
    expect(flushLoad([]).statusParam).toBe('RESOLVED');

    component.clearFilter();
    expect(component.hasActiveFilter()).toBeFalse();
    expect(flushLoad().statusParam).toBeNull();
  });

  it('remet le filtre à zéro quand la tuile n\'a pas de statut (Total)', () => {
    fixture.detectChanges();
    flushLoad();

    component.filterByStatus('CLOSED');
    flushLoad([]);

    component.filterByStatus(null);
    expect(component.filterModeCtrl.value).toBe('NONE');
    expect(component.statusCtrl.value).toBe('');
    expect(flushLoad().statusParam).toBeNull();
  });

  it('signale un identifiant fournisseur incomplet et ne filtre pas dessus', () => {
    fixture.detectChanges();
    flushLoad();

    // `emitEvent: false` : on teste la règle, pas le debounce de la saisie.
    component.filterModeCtrl.setValue('SUPPLIER', { emitEvent: false });
    component.supplierCtrl.setValue('3f1b5c8e-9a2d', { emitEvent: false });
    expect(component.supplierFilterIncomplete()).toBeTrue();

    component.onFilterValueChange();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.has('supplierId')).toBeFalse();
    req.flush(pageOf([]));
    http.expectOne(`${base}/statistics`).flush(statistics());

    component.supplierCtrl.setValue('3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f', { emitEvent: false });
    expect(component.supplierFilterIncomplete()).toBeFalse();

    component.onFilterValueChange();
    const filtered = http.expectOne(r => r.url === base);
    expect(filtered.request.params.get('supplierId')).toBe('3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f');
    filtered.flush(pageOf([]));
    http.expectOne(`${base}/statistics`).flush(statistics());
  });

  it('filtre par catégorie et vide la valeur précédente au changement de critère', () => {
    fixture.detectChanges();
    flushLoad();

    component.filterModeCtrl.setValue('CATEGORY', { emitEvent: false });
    component.categoryCtrl.setValue('BILLING', { emitEvent: false });
    component.onFilterValueChange();

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('category')).toBe('BILLING');
    req.flush(pageOf([]));
    http.expectOne(`${base}/statistics`).flush(statistics());

    component.filterModeCtrl.setValue('STATUS', { emitEvent: false });
    component.onFilterModeChange();
    expect(component.categoryCtrl.value).toBe('');
    const cleared = http.expectOne(r => r.url === base);
    expect(cleared.request.params.has('category')).toBeFalse();
    cleared.flush(pageOf([]));
    http.expectOne(`${base}/statistics`).flush(statistics());
  });

  it('borne l\'index et la taille de page', () => {
    fixture.detectChanges();
    flushLoad();

    component.onPage({ pageIndex: -3, pageSize: 999 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
    flushLoad();

    component.onPage({ pageIndex: 2, pageSize: 0 } as PageEvent);
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(1);
    flushLoad();
  });

  it('affiche un message d\'erreur quand le chargement échoue', (done) => {
    fixture.detectChanges();
    // Les compteurs sont servis d'abord : l'échec de la liste annulerait sinon
    // leur requête, et une requête annulée reste « ouverte » pour le harnais.
    http.expectOne(`${base}/statistics`).flush(statistics());
    http.expectOne(r => r.url === base)
      .flush({ title: 'boom' }, { status: 500, statusText: 'Server Error' });

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('serveur');
      done();
    });
  });

  it('recharge la liste après une création', () => {
    fixture.detectChanges();
    flushLoad();

    component.filterByStatus('CLOSED');
    flushLoad([]);

    const dialog = TestBed.inject(MatDialog);
    spyOn(dialog, 'open').and.returnValue({
      afterClosed: () => of(complaint({ id: 'c9' }))
    } as MatDialogRef<unknown>);

    component.openCreate();
    // La création remet la liste à plat : le filtre est levé, la page revient à 1.
    expect(component.filterModeCtrl.value).toBe('NONE');
    expect(component.pageIndex).toBe(0);
    expect(flushLoad().statusParam).toBeNull();
  });

  it('n\'appelle rien quand la création est annulée', () => {
    fixture.detectChanges();
    flushLoad();

    const dialog = TestBed.inject(MatDialog);
    spyOn(dialog, 'open').and.returnValue({
      afterClosed: () => of(undefined)
    } as MatDialogRef<unknown>);

    component.openCreate();
    http.expectNone(r => r.url === base);
    expect(component.hasActiveFilter()).toBeFalse();
  });

  it('navigue vers le détail au clic sur une ligne', () => {
    fixture.detectChanges();
    flushLoad();
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.open(complaint({ id: 'c42' }));
    expect(nav).toHaveBeenCalledWith(['/complaints', 'c42']);
  });

  it('associe un ton sémantique à chaque statut et à chaque gravité', () => {
    expect(component.statusTone('RECEIVED')).toBe('info');
    expect(component.statusTone('UNDER_INVESTIGATION')).toBe('warn');
    expect(component.statusTone('REOPENED')).toBe('warn');
    expect(component.statusTone('RESPONDED')).toBe('info');
    expect(component.statusTone('RESOLVED')).toBe('success');
    expect(component.statusTone('CLOSED')).toBe('neutral');
    expect(component.statusTone('REJECTED')).toBe('danger');

    expect(component.severityTone('LOW')).toBe('success');
    expect(component.severityTone('MEDIUM')).toBe('info');
    expect(component.severityTone('HIGH')).toBe('warn');
    expect(component.severityTone('CRITICAL')).toBe('danger');
  });

  it('identifie les lignes et les catégories pour le trackBy', () => {
    expect(component.trackById(0, complaint({ id: 'c7' }))).toBe('c7');
    expect(component.trackByCategory(0, {
      category: 'SAFETY', label: 'Sécurité', count: 1, percent: 100
    })).toBe('SAFETY');
  });
});
