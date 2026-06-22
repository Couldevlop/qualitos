import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { ConnectivityService } from './connectivity.service';

describe('ConnectivityService', () => {
  let service: ConnectivityService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConnectivityService);
  });

  it('isOnline reflects navigator.onLine', () => {
    const spy = spyOnProperty(navigator, 'onLine', 'get').and.returnValue(false);
    expect(service.isOnline()).toBeFalse();
    spy.and.returnValue(true);
    expect(service.isOnline()).toBeTrue();
  });

  it('online$ emits the current state immediately on subscribe', () => {
    spyOnProperty(navigator, 'onLine', 'get').and.returnValue(true);
    let value: boolean | undefined;
    const sub = service.online$.subscribe(v => (value = v));
    expect(value).toBeTrue();
    sub.unsubscribe();
  });

  it('online$ emits false on the offline window event', fakeAsync(() => {
    const values: boolean[] = [];
    const sub = service.online$.subscribe(v => values.push(v));
    window.dispatchEvent(new Event('offline'));
    tick();
    expect(values[values.length - 1]).toBeFalse();
    sub.unsubscribe();
  }));

  it('online$ emits true on the online window event', fakeAsync(() => {
    const values: boolean[] = [];
    const sub = service.online$.subscribe(v => values.push(v));
    window.dispatchEvent(new Event('online'));
    tick();
    expect(values[values.length - 1]).toBeTrue();
    sub.unsubscribe();
  }));

  it('shares the latest state with late subscribers (shareReplay)', fakeAsync(() => {
    const first: boolean[] = [];
    const sub1 = service.online$.subscribe(v => first.push(v));
    window.dispatchEvent(new Event('offline'));
    tick();

    let lateValue: boolean | undefined;
    const sub2 = service.online$.subscribe(v => (lateValue = v));
    expect(lateValue).toBeFalse();

    sub1.unsubscribe();
    sub2.unsubscribe();
  }));
});
