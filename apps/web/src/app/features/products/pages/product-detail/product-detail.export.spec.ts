import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { ProductResponse } from '../../products.types';
import { ProductDetailComponent } from './product-detail.component';

/**
 * Le dossier du produit, sorti de la plateforme.
 *
 * <p>Un audit client réclame « le PFMEA et le plan de surveillance » : sans
 * export, ils ne sont transmissibles qu'à quelqu'un qui a un compte. Ce banc
 * tient les trois choses qui font qu'un téléchargement arrive vraiment chez
 * l'utilisateur : le nom vient du serveur, l'URL objet est libérée, et un
 * refus laisse le bouton réarmé plutôt que verrouillé.
 */
describe('ProductDetailComponent — export du dossier produit', () => {

  const product: ProductResponse = {
    id: 'p-1', code: 'REF-4471', designation: 'Support moteur', status: 'ACTIVE',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  let component: ProductDetailComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let lien: HTMLAnchorElement;

  beforeEach(async () => {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['get', 'components', 'operations', 'exportXlsx']);
    service.get.and.returnValue(of(product));
    service.components.and.returnValue(of([]));
    service.operations.and.returnValue(of([]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ProductDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: { open: jasmine.createSpy('open') } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'p-1' } } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ProductDetailComponent);
    component = fixture.componentInstance;
    component.productId = 'p-1';

    // Le téléchargement passe par un <a download> cliqué : on l'intercepte
    // plutôt que de le laisser naviguer dans le contexte de test.
    lien = document.createElement('a');
    spyOn(lien, 'click');
    spyOn(document, 'createElement').and.returnValue(lien);
  });

  /** Une réponse serveur portant le classeur et son en-tête de nom de fichier. */
  function reponse(disposition: string | null): HttpResponse<Blob> {
    const headers = disposition
      ? new HttpHeaders({ 'Content-Disposition': disposition })
      : new HttpHeaders();
    return new HttpResponse({ body: new Blob(['classeur']), headers, status: 200 });
  }

  it('enregistre le classeur sous le nom que le serveur propose', () => {
    // Le nom vient du SERVEUR : le refabriquer côté navigateur ferait diverger
    // les deux à la première évolution du format.
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    const revoke = spyOn(URL, 'revokeObjectURL');
    service.exportXlsx.and.returnValue(
      of(reponse('attachment; filename="ref-4471-pfmea-plan-surveillance.xlsx"')));

    component.exportXlsx();

    expect(lien.download).toBe('ref-4471-pfmea-plan-surveillance.xlsx');
    expect(lien.click).toHaveBeenCalled();
    // Sans révocation, chaque export garderait le classeur en mémoire jusqu'au
    // rechargement de la page.
    expect(revoke).toHaveBeenCalledWith('blob:x');
    expect(component.exporting).toBeFalse();
  });

  it('décode un nom de fichier encodé par le serveur', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    spyOn(URL, 'revokeObjectURL');
    service.exportXlsx.and.returnValue(
      of(reponse("attachment; filename*=UTF-8''r%C3%A9f-4471.xlsx")));

    component.exportXlsx();

    expect(lien.download).toBe('réf-4471.xlsx');
  });

  it('retombe sur un nom neutre quand l\'en-tête n\'est pas visible', () => {
    // `Content-Disposition` n'atteint le navigateur que s'il est exposé par
    // CORS : le repli couvre le jour où l'API passe sur un autre domaine. Sans
    // lui, le fichier arriverait sans extension et Excel refuserait de l'ouvrir.
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    spyOn(URL, 'revokeObjectURL');
    service.exportXlsx.and.returnValue(of(reponse(null)));

    component.exportXlsx();

    expect(lien.download).toBe('export.xlsx');
  });

  it('signale un refus du serveur et réarme le bouton', () => {
    service.exportXlsx.and.returnValue(throwError(() => new Error('500')));

    component.exportXlsx();

    expect(component.exporting).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
    expect(lien.click).not.toHaveBeenCalled();
  });

  it('signale une réponse vide plutôt que d\'enregistrer un fichier de zéro octet', () => {
    service.exportXlsx.and.returnValue(of(new HttpResponse<Blob>({ body: null, status: 200 })));

    component.exportXlsx();

    expect(snack.open).toHaveBeenCalled();
    expect(lien.click).not.toHaveBeenCalled();
  });

  it('ignore un second clic tant que le premier n\'a pas répondu', () => {
    // Deux clics rapides sur un export long dupliqueraient le téléchargement.
    const enVol = new Subject<HttpResponse<Blob>>();
    service.exportXlsx.and.returnValue(enVol.asObservable());

    component.exportXlsx();
    component.exportXlsx();

    expect(service.exportXlsx).toHaveBeenCalledTimes(1);
    expect(component.exporting).toBeTrue();
    enVol.complete();
  });
});
