import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { NcService } from '../../../nc/nc.service';
import { NcPage, NcResponse } from '../../../nc/nc.types';
import { ComplaintNlpService } from '../../complaint-nlp.service';
import { ComplaintNlpComponent } from './complaint-nlp.component';

describe('ComplaintNlpComponent — réclamations enregistrées', () => {
  let nc: jasmine.SpyObj<NcService>;
  let component: ComplaintNlpComponent;

  const ncs = (rows: Partial<NcResponse>[], total = rows.length): NcPage =>
    ({ content: rows as NcResponse[], totalElements: total, totalPages: 1, number: 0, size: 50 });

  beforeEach(() => {
    nc = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);
    const nlp = jasmine.createSpyObj<ComplaintNlpService>('ComplaintNlpService', ['analyze']);
    component = new ComplaintNlpComponent(new FormBuilder(), nlp, nc);
  });

  it('reprend les NC externes, une par ligne, titre puis description', () => {
    nc.listNcs.and.returnValue(of(ncs([
      { title: 'Colis abîmé', description: 'Carton écrasé\nà la livraison' },
      { title: 'Retard' }
    ], 12)));

    component.loadComplaints();

    expect(nc.listNcs).toHaveBeenCalledWith(0, ComplaintNlpComponent.SOURCE_SIZE, { origin: 'EXTERNAL' });
    expect(component.form.value.textsText).toBe('Colis abîmé — Carton écrasé à la livraison\nRetard');
    expect(component.sourceInfo).toContain('2');
    expect(component.sourceInfo).toContain('12');
    expect(component.sourceLoading).toBeFalse();
  });

  it('dit qu\'il n\'y a encore aucune réclamation', () => {
    nc.listNcs.and.returnValue(of(ncs([])));
    component.loadComplaints();
    expect(component.sourceInfo).toContain('Aucune réclamation');
  });

  it('signale un échec de chargement sans toucher à la saisie', () => {
    component.form.patchValue({ textsText: 'déjà saisi' });
    nc.listNcs.and.returnValue(throwError(() => new Error('down')));
    component.loadComplaints();
    expect(component.error).toContain('Impossible de charger');
    expect(component.form.value.textsText).toBe('déjà saisi');
  });
});
