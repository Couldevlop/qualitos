import { Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';

import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import { DashboardLayout } from '../../domain/dashboard.model';
import { DashboardListComponent } from './dashboard-list.component';

/** La liste des tableaux de bord : les ouvrir, en créer un. */
describe('DashboardListComponent', () => {
  let svc: jasmine.SpyObj<DashboardBuilderService>;
  let router: jasmine.SpyObj<Router>;
  let component: DashboardListComponent;

  beforeEach(() => {
    svc = jasmine.createSpyObj<DashboardBuilderService>('DashboardBuilderService', ['list']);
    svc.list.and.returnValue(of([{ id: 'd-1' } as DashboardLayout]));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    component = new DashboardListComponent(svc, router);
    component.ngOnInit();
  });

  it('charge les tableaux une seule fois pour tous les abonnés', async () => {
    expect((await firstValueFrom(component.layouts$)).length).toBe(1);
    await firstValueFrom(component.layouts$);
    expect(svc.list).toHaveBeenCalledTimes(1);
  });

  it('ouvre un tableau existant, ou un nouveau', () => {
    component.open({ id: 'd-1' } as DashboardLayout);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard-builder', 'd-1']);

    component.create();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard-builder', 'new']);
  });
});
