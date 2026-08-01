import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApiKeyView, IssuedApiKey } from '../../api-keys.types';
import { ApiKeyCreateDialogComponent } from '../api-key-create-dialog/api-key-create-dialog.component';
import { ApiKeyRevealDialogComponent } from '../api-key-reveal-dialog/api-key-reveal-dialog.component';
import { ApiKeysListComponent } from './api-keys-list.component';

/**
 * L'écran est la seule voie de révocation d'urgence d'une clé : ses garde-fous
 * (confirmation, actions filtrées par statut, secret jamais listé) sont testés ici.
 */
describe('ApiKeysListComponent', () => {
  let component: ApiKeysListComponent;
  let fixture: ComponentFixture<ApiKeysListComponent>;
  let http: HttpTestingController;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;

  const base = `${environment.apiBaseUrl}/api/v1/api-keys`;
  const DAY = 24 * 60 * 60 * 1000;

  const key = (id: string, over: Partial<ApiKeyView> = {}): ApiKeyView => ({
    id, tenantId: 't1', name: 'Clé ' + id, prefix: 'pfx' + id, scopes: ['audits.read'],
    status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', createdBy: 'u1',
    expiresAt: null, lastUsedAt: null, revokedAt: null, revokedBy: null, ...over
  });

  const issued: IssuedApiKey = { view: key('new'), plaintext: 'qos_pfx_secret' };

  /** Amorce l'écran et sert la première liste. */
  const start = (keys: ApiKeyView[] = [key('a1')]) => {
    fixture.detectChanges();
    http.expectOne(base).flush(keys);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    currentUser = {
      userId: '11111111-1111-1111-1111-111111111111',
      tenantId: 't1', displayName: 'Admin', roles: ['ADMIN_TENANT']
    };
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ApiKeysListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApiKeysListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('affiche le titre de l\'écran', () => {
    start();
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent)
      .toContain('Clés d\'API');
  });

  it('déclare les colonnes attendues et ne montre jamais de secret', () => {
    expect(component.displayedColumns)
      .toEqual(['name', 'scopes', 'status', 'created', 'lastUsed', 'expiry', 'actions']);
    expect(component.displayedColumns).not.toContain('plaintext');
  });

  it('charge la liste au démarrage', (done) => {
    component.ngOnInit();
    component.keys$.subscribe(keys => {
      expect(keys.map(k => k.id)).toEqual(['a1']);
      done();
    });
    http.expectOne(base).flush([key('a1')]);
  });

  it('calcule les tuiles de synthèse', (done) => {
    component.ngOnInit();
    component.tiles$.subscribe(tiles => {
      expect(tiles.length).toBe(5);
      expect(tiles[0].value).toBe(1);
      expect(tiles[3].value).toBe(1);
      done();
    });
    http.expectOne(base).flush([key('a1'), key('r1', { status: 'REVOKED' })]);
  });

  it('affiche un bandeau quand la liste est refusée', (done) => {
    component.ngOnInit();
    // La requête n'est émise qu'à l'abonnement de la vue : `error$` seul ne déclenche rien.
    component.keys$.subscribe();
    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('droits');
      done();
    });
    http.expectOne(base).flush('nope', { status: 403, statusText: 'Forbidden' });
  });

  // `loading$` passe par `deferredView` (macrotâche anti-NG0100) : l'état vide n'est
  // rendu qu'une fois cette émission délivrée, d'où le `tick`.
  it('affiche l\'état vide quand le tenant n\'a aucune clé', fakeAsync(() => {
    start([]);
    tick();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.empty')).not.toBeNull();
  }));

  it('traduit le statut en tonalité', () => {
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('EXPIRED')).toBe('warn');
    expect(component.statusTone('REVOKED')).toBe('danger');
    expect(component.statusTone('AUTRE' as never)).toBe('neutral');
  });

  it('n\'autorise rotation et révocation que sur une clé active', () => {
    expect(component.canRotate(key('a'))).toBeTrue();
    expect(component.canRevoke(key('a'))).toBeTrue();
    expect(component.canRotate(key('b', { status: 'REVOKED' }))).toBeFalse();
    expect(component.canRevoke(key('c', { status: 'EXPIRED' }))).toBeFalse();
  });

  it('signale les clés dont l\'échéance approche', () => {
    expect(component.isExpiringSoon(key('a', { expiresAt: new Date(Date.now() + 5 * DAY).toISOString() }))).toBeTrue();
    expect(component.isExpiringSoon(key('b', { expiresAt: new Date(Date.now() + 90 * DAY).toISOString() }))).toBeFalse();
    expect(component.isExpiringSoon(key('c'))).toBeFalse();
    expect(component.isExpiringSoon(key('d', { status: 'REVOKED', expiresAt: new Date(Date.now() + DAY).toISOString() }))).toBeFalse();
    expect(component.isExpiringSoon(key('e', { expiresAt: 'illisible' }))).toBeFalse();
  });

  it('identifie une ligne par son identifiant', () => {
    expect(component.trackById(0, key('a1'))).toBe('a1');
  });

  it('révèle le secret une seule fois après une émission, puis recharge', () => {
    start();
    dialog.open.and.returnValue({ afterClosed: () => of(issued) } as never);

    component.create();

    expect(dialog.open.calls.argsFor(0)[0]).toBe(ApiKeyCreateDialogComponent as never);
    expect(dialog.open.calls.argsFor(1)[0]).toBe(ApiKeyRevealDialogComponent as never);
    // Le dialogue de révélation ne doit pas pouvoir être fermé par inadvertance.
    const revealConfig = dialog.open.calls.argsFor(1)[1] as { disableClose?: boolean } | undefined;
    expect(revealConfig?.disableClose).toBeTrue();
    http.expectOne(base).flush([key('a1')]);
  });

  it('ne révèle rien si l\'émission est annulée', () => {
    start();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);

    component.create();

    expect(dialog.open.calls.count()).toBe(1);
    http.expectNone(base);
  });

  it('fait tourner une clé après confirmation et révèle le nouveau secret', () => {
    start();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);

    component.rotate(key('a1'));

    const req = http.expectOne(`${base}/a1/rotate`);
    expect(req.request.body).toEqual({ actor: currentUser!.userId });
    req.flush(issued);

    expect(dialog.open.calls.argsFor(1)[0]).toBe(ApiKeyRevealDialogComponent as never);
    expect(component.pendingKeyId).toBeNull();
    http.expectOne(base).flush([key('a1')]);
  });

  it('n\'appelle pas le serveur si la rotation n\'est pas confirmée', () => {
    start();
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);

    component.rotate(key('a1'));

    http.expectNone(`${base}/a1/rotate`);
    expect(dialog.open).toHaveBeenCalledTimes(1);
  });

  it('ne propose aucune opération sur une clé qui n\'est plus active', () => {
    start();

    component.rotate(key('a1', { status: 'REVOKED' }));
    component.revoke(key('a1', { status: 'EXPIRED' }));

    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('révoque après confirmation, notifie et recharge', () => {
    start();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);

    component.revoke(key('a1'));

    const req = http.expectOne(`${base}/a1/revoke`);
    expect(req.request.body).toEqual({ actor: currentUser!.userId });
    req.flush(key('a1', { status: 'REVOKED' }));

    expect(snack.open).toHaveBeenCalled();
    http.expectOne(base).flush([key('a1', { status: 'REVOKED' })]);
  });

  it('affiche un message si la révocation échoue', (done) => {
    start();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('rechargez');
      done();
    });

    component.revoke(key('a1'));
    http.expectOne(`${base}/a1/revoke`).flush('conflit', { status: 409, statusText: 'Conflict' });
  });

  it('invite à se reconnecter quand la session ne porte plus d\'acteur', (done) => {
    start();
    currentUser = null;
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Session expirée');
      done();
    });

    component.revoke(key('a1'));
    http.expectNone(`${base}/a1/revoke`);
  });

  it('traite les clés échues et rend compte du nombre traité', () => {
    start();

    component.expireDue();
    http.expectOne(r => r.url === `${base}/expire-due`).flush({ expired: 3 });

    expect(snack.open).toHaveBeenCalled();
    expect(component.maintenanceRunning).toBeFalse();
    http.expectOne(base).flush([key('a1')]);
  });

  it('indique explicitement qu\'aucune clé n\'était échue', () => {
    start();

    component.expireDue();
    http.expectOne(r => r.url === `${base}/expire-due`).flush({ expired: 0 });

    expect(snack.open.calls.mostRecent().args[0]).toContain('Aucune');
    http.expectOne(base).flush([key('a1')]);
  });

  it('ignore un second déclenchement de maintenance pendant le traitement', () => {
    start();

    component.expireDue();
    component.expireDue();

    const requests = http.match(r => r.url === `${base}/expire-due`);
    expect(requests.length).toBe(1);
    requests[0].flush({ expired: 0 });
    http.expectOne(base).flush([key('a1')]);
  });

  it('signale l\'échec du traitement des clés échues', (done) => {
    start();
    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('serveur');
      done();
    });

    component.expireDue();
    http.expectOne(r => r.url === `${base}/expire-due`)
      .flush('boom', { status: 500, statusText: 'Server Error' });
  });

  it('charge le détail d\'une clé à la sélection, puis le ferme', (done) => {
    start();
    const seen: (ApiKeyView | null)[] = [];
    component.detail$.subscribe(d => {
      seen.push(d);
      if (seen.length === 3) {
        expect(seen[0]).toBeNull();
        expect(seen[1]?.id).toBe('a1');
        expect(seen[2]).toBeNull();
        done();
      }
    });

    component.select(key('a1'));
    expect(component.selectedId).toBe('a1');
    http.expectOne(`${base}/a1`).flush(key('a1', { lastUsedAt: '2026-07-01T10:00:00Z' }));

    component.clearSelection();
    expect(component.selectedId).toBeNull();
  });

  it('signale un détail illisible sans casser la liste', () => {
    start();
    component.detail$.subscribe();

    component.select(key('a1'));
    http.expectOne(`${base}/a1`).flush('nope', { status: 404, statusText: 'Not Found' });

    expect(component.detailError).toBeTruthy();
  });

  it('rafraîchit aussi le détail ouvert lors d\'un rechargement', () => {
    start();
    const seen: (ApiKeyView | null)[] = [];
    component.detail$.subscribe(d => seen.push(d));

    component.select(key('a1'));
    http.expectOne(`${base}/a1`).flush(key('a1'));

    component.refresh();
    http.expectOne(base).flush([key('a1')]);
    http.expectOne(`${base}/a1`).flush(key('a1', { lastUsedAt: '2026-07-02T08:00:00Z' }));

    expect(seen.map(d => d?.lastUsedAt ?? null)).toEqual([null, null, '2026-07-02T08:00:00Z']);
  });
});
