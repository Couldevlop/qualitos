import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { AuditLogService } from '../../audit-log.service';
import { AuditEvent, AuditEventPage, ChainVerification } from '../../audit-log.types';
import { AuditLogComponent } from './audit-log.component';

describe('AuditLogComponent', () => {
  let component: AuditLogComponent;
  let fixture: ComponentFixture<AuditLogComponent>;
  let svc: jasmine.SpyObj<AuditLogService>;

  const UUID = '11111111-1111-1111-1111-111111111111';

  const event = (over: Partial<AuditEvent> = {}): AuditEvent => ({
    id: 'e-1', tenantId: 't-1', sequenceNo: 12,
    occurredAt: '2026-07-30T10:00:00Z', recordedAt: '2026-07-30T10:00:00Z',
    actorType: 'USER', actorUserId: UUID,
    action: 'capa.closed', resourceType: 'capa-case', resourceId: null,
    summary: 'CAPA clôturée', payloadJson: null, ipAddress: '10.0.0.1', userAgent: 'Chrome',
    integrityHash: '0123456789abcdef0123', previousHash: null, blockchainTxRef: null,
    ...over
  });

  const page = (content: AuditEvent[]): AuditEventPage => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 50
  });

  const okVerification: ChainVerification = {
    tenantId: 't-1', fromSequenceNo: 10, toSequenceNo: 12,
    verifiedCount: 3, valid: true, breaks: []
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<AuditLogService>('AuditLogService',
      ['list', 'get', 'verifyChain', 'effectiveCriterion']);
    svc.list.and.returnValue(of(page([event(), event({ id: 'e-2', sequenceNo: 11 })])));
    svc.get.and.returnValue(of(event({ payloadJson: '{"k":1}' })));
    svc.verifyChain.and.returnValue(of(okVerification));
    // `effectiveCriterion` est une fonction pure du contrat serveur : on garde la vraie
    // implémentation, la stubber testerait le stub.
    svc.effectiveCriterion.and.callFake(
      new AuditLogService(null as never).effectiveCriterion);

    await TestBed.configureTestingModule({
      declarations: [AuditLogComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: AuditLogService, useValue: svc },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogComponent);
    component = fixture.componentInstance;
  });

  const rows = (): NodeListOf<Element> =>
    (fixture.nativeElement as HTMLElement).querySelectorAll('tr[mat-row]');

  it('charge la première page sans aucun critère', () => {
    fixture.detectChanges();
    expect(svc.list).toHaveBeenCalledWith({}, 0, 50);
    expect(rows().length).toBe(2);
  });

  it('n\'offre aucune action d\'écriture : le journal est append-only', () => {
    fixture.detectChanges();
    const labels = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .map(b => (b.textContent ?? '').toLowerCase());
    expect(labels.some(l => l.includes('supprim'))).toBeFalse();
    expect(labels.some(l => l.includes('modifi'))).toBeFalse();
    expect(labels.some(l => l.includes('ancr'))).toBeFalse();
  });

  it('affiche l\'état vide quand le journal ne renvoie rien', fakeAsync(() => {
    svc.list.and.returnValue(of(page([])));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(rows().length).toBe(0);
    expect((fixture.nativeElement as HTMLElement).querySelector('.empty')).toBeTruthy();
  }));

  it('affiche un bandeau et remet les compteurs à zéro sur erreur de chargement', fakeAsync(() => {
    svc.list.and.returnValue(throwError(() => ({ status: 500 })));
    let total = -1;
    fixture.detectChanges();
    component.total$.subscribe(t => total = t);
    tick();
    fixture.detectChanges();

    expect(total).toBe(0);
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')).toBeTruthy();
  }));

  it('convertit les bornes locales en instants absolus et revient page 1', () => {
    fixture.detectChanges();
    component.pageIndex = 3;
    component.filterForm.patchValue({ from: '2026-07-01T08:00', to: '2026-07-31T18:30' });
    component.apply();

    const [filter, index] = svc.list.calls.mostRecent().args;
    expect(filter.from).toBe(new Date('2026-07-01T08:00').toISOString());
    expect(filter.to).toBe(new Date('2026-07-31T18:30').toISOString());
    expect(index).toBe(0);
    expect(component.pageIndex).toBe(0);
  });

  it('ignore une borne de date illisible plutôt que d\'envoyer un paramètre invalide', () => {
    fixture.detectChanges();
    component.filterForm.patchValue({ from: 'pas-une-date' });
    component.apply();
    expect(svc.list.calls.mostRecent().args[0].from).toBeUndefined();
  });

  it('nettoie les espaces autour des critères textuels', () => {
    fixture.detectChanges();
    component.filterForm.patchValue({ action: '  capa.closed  ', resourceType: ' capa-case ' });
    component.apply();
    const filter = svc.list.calls.mostRecent().args[0];
    expect(filter.action).toBe('capa.closed');
    expect(filter.resourceType).toBe('capa-case');
  });

  it('transmet les identifiants de ressource et d\'acteur au format UUID', () => {
    fixture.detectChanges();
    component.filterForm.patchValue({ resourceId: UUID, actorUserId: UUID });
    component.apply();
    const filter = svc.list.calls.mostRecent().args[0];
    expect(filter.resourceId).toBe(UUID);
    expect(filter.actorUserId).toBe(UUID);
  });

  it('refuse de requêter avec un identifiant non-UUID (le serveur répondrait 400)', () => {
    fixture.detectChanges();
    const before = svc.list.calls.count();
    component.filterForm.patchValue({ resourceId: 'pas-un-uuid' });
    component.apply();
    expect(svc.list.calls.count()).toBe(before);
    expect(component.filterForm.controls.resourceId.touched).toBeTrue();
  });

  it('n\'active « Réinitialiser » que lorsqu\'un critère est posé', () => {
    fixture.detectChanges();
    expect(component.hasFilter).toBeFalse();
    component.filterForm.patchValue({ action: 'nc.created' });
    expect(component.hasFilter).toBeTrue();
  });

  it('réinitialise les critères et recharge le journal complet', () => {
    fixture.detectChanges();
    component.filterForm.patchValue({ action: 'nc.created' });
    component.apply();
    component.reset();

    expect(component.hasFilter).toBeFalse();
    expect(svc.list.calls.mostRecent().args[0]).toEqual({});
  });

  it('recharge la page demandée en bornant la taille au maximum du serveur', () => {
    // Le serveur rabote silencieusement au-delà de 100 (max-page-size). Demander
    // davantage rendrait une partie du journal inatteignable, sans aucune erreur.
    fixture.detectChanges();
    component.onPage({ pageIndex: 2, pageSize: 500, length: 1000 } as PageEvent);
    expect(component.pageSize).toBe(100);
    expect(svc.list.calls.mostRecent().args).toEqual([{}, 2, 100]);
  });

  it('corrige un index de page négatif', () => {
    fixture.detectChanges();
    component.onPage({ pageIndex: -3, pageSize: 0, length: 0 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(1);
  });

  it('annonce le critère réellement appliqué par le serveur', () => {
    fixture.detectChanges();
    component.filterForm.patchValue({ action: 'nc.created', actorUserId: UUID });
    component.apply();
    fixture.detectChanges();

    const hint = (fixture.nativeElement as HTMLElement).querySelector('.criterion');
    expect(hint?.textContent).toContain(component.criterionLabel('action'));
  });

  it('produit un libellé distinct pour chaque cascade de filtre', () => {
    const labels = (['resource', 'action', 'actor', 'period', 'none'] as const)
      .map(c => component.criterionLabel(c));
    expect(new Set(labels).size).toBe(5);
    labels.forEach(l => expect(l.length).toBeGreaterThan(0));
  });

  it('ouvre le détail en relisant l\'événement pour obtenir sa charge utile', () => {
    fixture.detectChanges();
    component.openDetail(event());
    expect(svc.get).toHaveBeenCalledWith('e-1');
    expect(component.detail?.payloadJson).toBe('{"k":1}');
    expect(component.detailLoading).toBeFalse();
    expect(component.isSelected(event())).toBeTrue();
  });

  it('ne relance pas la lecture du détail déjà en vol', () => {
    let subscribed = 0;
    svc.get.and.returnValue(new Observable<AuditEvent>(() => { subscribed++; }));
    fixture.detectChanges();
    component.openDetail(event());
    component.openDetail(event());
    expect(subscribed).toBe(1);
  });

  it('signale l\'échec de lecture du détail sans vider la ligne sélectionnée', () => {
    svc.get.and.returnValue(throwError(() => ({ status: 404 })));
    fixture.detectChanges();
    component.openDetail(event({ id: 'e-9' }));
    expect(component.detailError).toBeTruthy();
    expect(component.detail?.id).toBe('e-9');
  });

  it('ferme le panneau de détail', () => {
    fixture.detectChanges();
    component.openDetail(event());
    component.closeDetail();
    expect(component.detail).toBeNull();
    expect(component.detailError).toBeNull();
  });

  it('ferme le détail quand la page change, pour ne pas laisser un orphelin à l\'écran', () => {
    fixture.detectChanges();
    component.openDetail(event());
    component.onPage({ pageIndex: 1, pageSize: 50, length: 100 } as PageEvent);
    expect(component.detail).toBeNull();
  });

  it('indente la charge utile JSON et rend telle quelle une charge illisible', () => {
    expect(component.prettyPayload('{"a":1}')).toBe('{\n  "a": 1\n}');
    expect(component.prettyPayload('pas du json')).toBe('pas du json');
    expect(component.prettyPayload(null)).toBe('');
  });

  it('amorce la vérification avec les bornes de la page affichée', () => {
    fixture.detectChanges();
    component.verifyVisible({ from: 11, to: 12 });
    expect(component.verifyForm.getRawValue()).toEqual({ fromSeq: 11, toSeq: 12 });
    expect(svc.verifyChain).toHaveBeenCalledWith(11, 12);
    expect(component.verifyResult?.valid).toBeTrue();
  });

  it('expose les bornes de séquence de la page, et rien quand elle est vide', fakeAsync(() => {
    fixture.detectChanges();
    let range: { from: number; to: number } | null = null;
    component.range$.subscribe(r => range = r);
    tick();
    expect(range).toEqual({ from: 11, to: 12 } as never);

    svc.list.and.returnValue(of(page([])));
    component.reset();
    tick();
    expect(range).toBeNull();
  }));

  it('refuse une fenêtre de séquences désordonnée sans appeler le serveur', () => {
    fixture.detectChanges();
    component.verifyForm.setValue({ fromSeq: 20, toSeq: 5 });
    component.verify();
    expect(svc.verifyChain).not.toHaveBeenCalled();
    expect(component.verifyError).toBeTruthy();
    expect(component.verifyRangeOrdered).toBeFalse();
  });

  it('refuse une fenêtre incomplète', () => {
    fixture.detectChanges();
    component.verifyForm.setValue({ fromSeq: null, toSeq: 5 });
    component.verify();
    expect(svc.verifyChain).not.toHaveBeenCalled();
    expect(component.verifyForm.touched).toBeTrue();
  });

  it('affiche les ruptures détectées par la vérification', () => {
    svc.verifyChain.and.returnValue(of({
      tenantId: 't-1', fromSequenceNo: 1, toSequenceNo: 3, verifiedCount: 3, valid: false,
      breaks: [{ eventId: 'e-3', sequenceNo: 3, reason: 'Integrity hash mismatch (tamper)' }]
    } as ChainVerification));
    fixture.detectChanges();
    component.verifyForm.setValue({ fromSeq: 1, toSeq: 3 });
    component.verify();
    fixture.detectChanges();

    const breaks = (fixture.nativeElement as HTMLElement).querySelectorAll('.breaks li');
    expect(breaks.length).toBe(1);
    expect(breaks[0].textContent).toContain('tamper');
    expect(component.verifyResult?.valid).toBeFalse();
  });

  it('signale l\'échec de la vérification', () => {
    svc.verifyChain.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();
    component.verifyForm.setValue({ fromSeq: 1, toSeq: 3 });
    component.verify();
    expect(component.verifyError).toBeTruthy();
    expect(component.verifyResult).toBeNull();
    expect(component.verifying).toBeFalse();
  });

  it('ignore un second clic pendant une vérification en vol', () => {
    let subscribed = 0;
    svc.verifyChain.and.returnValue(new Observable<ChainVerification>(() => { subscribed++; }));
    fixture.detectChanges();
    component.verifyForm.setValue({ fromSeq: 1, toSeq: 3 });
    component.verify();
    component.verify();
    expect(subscribed).toBe(1);
  });

  it('abrège les empreintes et distingue les événements ancrés', () => {
    expect(component.shortHash('0123456789abcdef')).toBe('0123456789ab…');
    expect(component.shortHash('court')).toBe('court');
    expect(component.shortHash(null)).toBe('—');
    expect(component.isAnchored(event())).toBeFalse();
    expect(component.isAnchored(event({ blockchainTxRef: 'tx-1' }))).toBeTrue();
  });

  it('distingue visuellement un acteur humain d\'un acteur machine', () => {
    expect(component.actorTone('USER')).toBe('accent');
    expect(component.actorTone('SYSTEM')).toBe('neutral');
    expect(component.actorTone('SCHEDULER')).toBe('neutral');
  });

  it('suit les lignes par numéro de séquence, unique par tenant', () => {
    expect(component.trackBySeq(0, event({ sequenceNo: 42 }))).toBe(42);
    expect(component.trackByBreak(1, { sequenceNo: 7 })).toBe('7-1');
  });
});
