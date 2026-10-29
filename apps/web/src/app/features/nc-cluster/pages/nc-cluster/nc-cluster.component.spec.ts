import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../../nc/nc.service';
import { NcPage } from '../../../nc/nc.types';
import { SuppliersService } from '../../../suppliers/suppliers.service';
import { NonConformityPage } from '../../../suppliers/suppliers.types';
import { NcClusterService } from '../../nc-cluster.service';
import { NcClusterResponse } from '../../nc-cluster.types';
import { NcClusterComponent, REGISTER_SIZE } from './nc-cluster.component';

/**
 * Deux fenêtres, comme le registre des risques et opportunités : les NC
 * fournisseurs et les NC internes se regroupent chacune à part, chaque
 * fenêtre gardant sa saisie et son résultat quand on passe à l'autre.
 */
describe('NcClusterComponent', () => {
  let fixture: ComponentFixture<NcClusterComponent>;
  let component: NcClusterComponent;
  let cluster: jasmine.SpyObj<NcClusterService>;
  let suppliers: jasmine.SpyObj<SuppliersService>;
  let internal: jasmine.SpyObj<NcService>;
  const un = (sel: string) => (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>(`[data-test="${sel}"]`);

  const page = <T>(content: T[]) => ({ content, totalElements: content.length, totalPages: 1, number: 0, size: 100 });
  const response: NcClusterResponse = {
    n: 3, clusters: [{ clusterId: 0, size: 2, indices: [0, 1], topTerms: ['fuite'] }],
    noiseIndices: [2], clusteredRatio: 2 / 3
  } as NcClusterResponse;

  beforeEach(async () => {
    cluster = jasmine.createSpyObj<NcClusterService>('NcClusterService', ['cluster']);
    suppliers = jasmine.createSpyObj<SuppliersService>('SuppliersService', ['listAllNcs']);
    internal = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);
    suppliers.listAllNcs.and.returnValue(of(page([
      { description: '  Fuite  au joint ', partNumber: 'PN-1' }, { description: '', partNumber: 'PN-2' },
      { description: null, partNumber: null }
    ]) as unknown as NonConformityPage));
    internal.listNcs.and.returnValue(of(page([{ title: 'Rayure', description: 'capot' }]) as unknown as NcPage));
    await TestBed.configureTestingModule({
      declarations: [NcClusterComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcClusterService, useValue: cluster },
        { provide: SuppliersService, useValue: suppliers },
        { provide: NcService, useValue: internal }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcClusterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('ouvre sur les NC externes et reprend le registre fournisseurs, une NC par ligne', () => {
    expect(component.scope).toBe('external');
    expect(un('onglet-externes')?.getAttribute('aria-selected')).toBe('true');
    un('charger-registre')!.click();
    expect(suppliers.listAllNcs).toHaveBeenCalledWith({}, 0, REGISTER_SIZE);
    expect(component.form.value.textsText).toBe('Fuite au joint — PN-1\nPN-2');
    expect(internal.listNcs).not.toHaveBeenCalled();
  });

  it('reprend les NC internes dans leur fenêtre, et chaque fenêtre garde son résultat', () => {
    cluster.cluster.and.returnValue(of(response));
    component.form.patchValue({ textsText: 'a\nb\nc' });
    component.run();
    expect(component.result).toBe(response);

    un('onglet-internes')!.click();
    expect(component.scope).toBe('internal');
    expect(component.result).toBeNull();
    expect(component.form.value.textsText).toBe('');
    component.loadRegister();
    expect(internal.listNcs).toHaveBeenCalledWith(0, REGISTER_SIZE);
    expect(component.form.value.textsText).toBe('Rayure — capot');

    component.setScope('external');
    expect(component.result).toBe(response);
    expect(component.form.value.textsText).toBe('a\nb\nc');
    expect(component.membersOf(response.clusters[0])).toEqual(['a', 'b']);
    component.setScope('internal');
    expect(component.form.value.textsText).toBe('Rayure — capot');
  });

  it('dit un registre vide ou illisible', () => {
    internal.listNcs.and.returnValue(of(page([]) as unknown as NcPage));
    component.setScope('internal');
    component.loadRegister();
    expect(component.error).toContain('aucune');
    internal.listNcs.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.loadRegister();
    expect(component.error).toContain('ne se lit pas');
    expect(component.loadingRegister).toBeFalse();
  });

  it('ne change pas de fenêtre pendant un regroupement', () => {
    component.loading = true;
    component.setScope('internal');
    expect(component.scope).toBe('external');
  });

  it('demande au moins deux NC, et dit pourquoi le regroupement échoue', () => {
    component.form.patchValue({ textsText: 'seule' });
    component.run();
    expect(component.error).toContain('2');
    expect(cluster.cluster).not.toHaveBeenCalled();
    cluster.cluster.and.returnValue(throwError(() => new HttpErrorResponse({ status: 429 })));
    component.loadExample();
    component.run();
    expect(component.error).toContain('quota');
    expect(component.clusteredPct(0.666)).toBe('67 %');
    expect(component.noiseTexts({ ...response, noiseIndices: [] })).toEqual([]);
  });
});
