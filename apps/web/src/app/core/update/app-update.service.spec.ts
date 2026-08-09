import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate, VersionEvent } from '@angular/service-worker';
import { Subject } from 'rxjs';

import { AppUpdateService } from './app-update.service';

/**
 * Ce qui se teste ici n'est pas « le service worker fonctionne-t-il » — c'est
 * qu'une version livrée FINIT PAR SE VOIR, et qu'elle ne s'impose pas au milieu
 * d'une saisie. Sans cette mécanique, un déploiement réussi côté serveur restait
 * faux côté écran, sans que rien ne le signale.
 */
describe('AppUpdateService', () => {

  let versionUpdates: Subject<VersionEvent>;
  let unrecoverable: Subject<unknown>;
  let action: Subject<void>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let swUpdate: { isEnabled: boolean; versionUpdates: unknown; unrecoverable: unknown;
                  checkForUpdate: jasmine.Spy; activateUpdate: jasmine.Spy };
  let service: AppUpdateService;
  let reload: jasmine.Spy;

  function build(options: { enabled?: boolean } = {}): void {
    const { enabled = true } = options;

    versionUpdates = new Subject<VersionEvent>();
    unrecoverable = new Subject<unknown>();
    action = new Subject<void>();

    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    snack.open.and.returnValue({ onAction: () => action.asObservable() } as never);

    swUpdate = {
      isEnabled: enabled,
      versionUpdates: versionUpdates.asObservable(),
      unrecoverable: unrecoverable.asObservable(),
      checkForUpdate: jasmine.createSpy('checkForUpdate').and.returnValue(Promise.resolve(true)),
      activateUpdate: jasmine.createSpy('activateUpdate').and.returnValue(Promise.resolve(true))
    };

    // NgZone n'est PAS substitué : l'ordonnanceur de détection de changements
    // d'Angular en dépend, et le remplacer fait échouer l'injection avant même
    // que le service soit construit. La vraie zone du banc de test exécute
    // `runOutsideAngular` de façon synchrone, ce qui suffit ici.
    TestBed.configureTestingModule({
      providers: [
        { provide: SwUpdate, useValue: swUpdate },
        { provide: MatSnackBar, useValue: snack }
      ]
    });
    service = TestBed.inject(AppUpdateService);
    // Le rechargement est neutralisé : le laisser faire couperait la suite.
    reload = spyOn<never>(service as never, 'reload').and.stub();
  }

  function versionReady(): VersionEvent {
    return {
      type: 'VERSION_READY',
      currentVersion: { hash: 'ancien' },
      latestVersion: { hash: 'nouveau' }
    } as VersionEvent;
  }

  /** Laisse les promesses déjà résolues s'écouler. */
  async function flush(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  afterEach(() => service?.ngOnDestroy());

  it('propose le rechargement quand une version est prête', () => {
    build();
    service.start();

    versionUpdates.next(versionReady());

    expect(snack.open).toHaveBeenCalled();
    // L'invitation n'a pas de durée : elle attend l'utilisateur au lieu de
    // disparaître sous ses yeux.
    const config = snack.open.calls.mostRecent().args[2] as { duration?: number } | undefined;
    expect(config?.duration).toBeUndefined();
  });

  it("n'impose rien : sans geste de l'utilisateur, la page ne bouge pas", () => {
    build();
    service.start();

    versionUpdates.next(versionReady());

    expect(swUpdate.activateUpdate).not.toHaveBeenCalled();
    expect(reload).not.toHaveBeenCalled();
  });

  it("active la version puis recharge quand l'utilisateur accepte", async () => {
    build();
    service.start();
    versionUpdates.next(versionReady());

    action.next();
    await flush();

    expect(swUpdate.activateUpdate).toHaveBeenCalled();
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it("recharge malgré tout si l'activation échoue", async () => {
    build();
    swUpdate.activateUpdate.and.returnValue(Promise.reject(new Error('cache corrompu')));
    service.start();
    versionUpdates.next(versionReady());

    action.next();
    await flush();

    // Rester sur un cache que le worker n'a pas su activer ne mène nulle part.
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('recharge sans demander sur un état irrécupérable', () => {
    build();
    service.start();

    unrecoverable.next({ type: 'UNRECOVERABLE_STATE', reason: 'ressource absente du cache' });

    // Aucune saisie n'est récupérable dans cet état : il n'y a rien à préserver.
    expect(reload).toHaveBeenCalledTimes(1);
    expect(snack.open).not.toHaveBeenCalled();
  });

  describe('vérification périodique', () => {
    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it("interroge le serveur à intervalle régulier, pour les écrans jamais rechargés", () => {
      build();
      service.start();

      expect(swUpdate.checkForUpdate).not.toHaveBeenCalled();
      jasmine.clock().tick(AppUpdateService.CHECK_INTERVAL_MS);
      expect(swUpdate.checkForUpdate).toHaveBeenCalledTimes(1);
      jasmine.clock().tick(AppUpdateService.CHECK_INTERVAL_MS);
      expect(swUpdate.checkForUpdate).toHaveBeenCalledTimes(2);
    });

    it("avale l'échec d'une vérification", () => {
      build();
      swUpdate.checkForUpdate.and.returnValue(Promise.reject(new Error('réseau coupé')));
      service.start();

      // La version en cache reste servie ; le prochain passage réessaiera.
      expect(() => jasmine.clock().tick(AppUpdateService.CHECK_INTERVAL_MS)).not.toThrow();
      expect(swUpdate.checkForUpdate).toHaveBeenCalled();
    });

    it("arrête la minuterie à la destruction", () => {
      build();
      service.start();
      service.ngOnDestroy();

      jasmine.clock().tick(AppUpdateService.CHECK_INTERVAL_MS * 2);
      expect(swUpdate.checkForUpdate).not.toHaveBeenCalled();
    });
  });

  it("ne s'abonne à rien quand le service worker est absent", () => {
    build({ enabled: false });
    service.start();

    versionUpdates.next(versionReady());

    expect(snack.open).not.toHaveBeenCalled();
  });

  it("ne se met en route qu'une fois", () => {
    build();
    service.start();
    service.start();

    versionUpdates.next(versionReady());
    // Deux abonnements auraient produit deux invitations pour une seule version.
    expect(snack.open).toHaveBeenCalledTimes(1);
  });
});
