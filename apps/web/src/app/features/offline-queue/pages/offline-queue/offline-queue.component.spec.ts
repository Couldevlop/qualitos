import { ComponentFixture, discardPeriodicTasks, fakeAsync, flush, tick, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of, Subject } from 'rxjs';

import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { OfflineQueueEvent, OfflineQueueService } from '../../../../core/offline/offline-queue.service';
import { QueuedOperation } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { OfflineQueueComponent } from './offline-queue.component';

const OP_A: QueuedOperation = {
  id: 'op-a',
  method: 'POST',
  url: '/api/v1/fives/audits',
  body: { zone: 'A' },
  label: 'Création audit 5S — zone A',
  queuedAt: '2026-06-05T08:00:00.000Z'
};
const OP_B: QueuedOperation = {
  id: 'op-b',
  method: 'PUT',
  url: '/api/v1/fives/audits/a1/score',
  body: { pillar: 'SEIRI', score: 7 },
  label: 'Score 5S SEIRI',
  queuedAt: '2026-06-05T08:05:00.000Z'
};

class FakeQueueService {
  ops: QueuedOperation[] = [OP_A, OP_B];
  readonly pendingCount$ = new BehaviorSubject<number>(2);
  readonly events$ = new Subject<OfflineQueueEvent>();
  replayCalls = 0;
  discarded: string[] = [];

  list(): Promise<QueuedOperation[]> {
    return Promise.resolve([...this.ops]);
  }

  replay(): Promise<void> {
    this.replayCalls++;
    return Promise.resolve();
  }

  discard(id: string): Promise<void> {
    this.discarded.push(id);
    this.ops = this.ops.filter(o => o.id !== id);
    return Promise.resolve();
  }
}

class FakeConnectivity {
  readonly online$ = new BehaviorSubject<boolean>(true);
  isOnline(): boolean {
    return this.online$.value;
  }
}

describe('OfflineQueueComponent', () => {
  let fixture: ComponentFixture<OfflineQueueComponent>;
  let component: OfflineQueueComponent;
  let queue: FakeQueueService;
  let connectivity: FakeConnectivity;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    queue = new FakeQueueService();
    connectivity = new FakeConnectivity();
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      declarations: [OfflineQueueComponent],
      providers: [
        { provide: OfflineQueueService, useValue: queue },
        { provide: ConnectivityService, useValue: connectivity },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OfflineQueueComponent);
    component = fixture.componentInstance;
  });

  it('liste les opérations en attente dans l’ordre', fakeAsync(() => {
    fixture.detectChanges();
    // loading$ est désormais une vue différée (asyncScheduler) : tick() draine la
    // macrotâche RxJS pour que le gate `(loading$ | async) === false` libère la table.
    tick();
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Création audit 5S — zone A');
    expect(rows[1].textContent).toContain('Score 5S SEIRI');
    discardPeriodicTasks();
  }));

  it('affiche l’état vide quand la file est vide', fakeAsync(() => {
    queue.ops = [];
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty-card')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('table')).toBeFalsy();
    discardPeriodicTasks();
  }));

  it('se rafraîchit sur événement de la file (replayed)', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    queue.ops = [OP_B];
    queue.events$.next({ type: 'replayed', operation: OP_A });
    tick();
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Score 5S SEIRI');
    discardPeriodicTasks();
  }));

  it('syncNow() déclenche le rejeu quand en ligne', fakeAsync(() => {
    fixture.detectChanges();
    flush();

    void component.syncNow();
    flush();

    expect(queue.replayCalls).toBe(1);
    expect(snack.open).toHaveBeenCalled();
    discardPeriodicTasks();
  }));

  it('syncNow() ne rejoue pas hors-ligne et informe l’utilisateur', fakeAsync(() => {
    connectivity.online$.next(false);
    fixture.detectChanges();
    flush();

    void component.syncNow();
    flush();

    expect(queue.replayCalls).toBe(0);
    expect(snack.open).toHaveBeenCalled();
    discardPeriodicTasks();
  }));

  it('discard() abandonne l’opération après confirmation', fakeAsync(() => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    fixture.detectChanges();
    flush();

    component.discard(OP_A);
    flush();

    expect(queue.discarded).toEqual(['op-a']);
    discardPeriodicTasks();
  }));

  it('discard() ne fait rien si l’utilisateur annule', fakeAsync(() => {
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    fixture.detectChanges();
    flush();

    component.discard(OP_A);
    flush();

    expect(queue.discarded).toEqual([]);
    discardPeriodicTasks();
  }));
});
