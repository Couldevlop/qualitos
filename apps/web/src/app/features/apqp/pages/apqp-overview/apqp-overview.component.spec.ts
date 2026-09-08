import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpPhase } from '../../apqp.types';
import { ApqpOverviewComponent } from './apqp-overview.component';

/**
 * Le cycle APQP, dont le schéma EST l'interface — et qui s'édite.
 *
 * <p>Trois choses se vérifient ici. La forme en V d'abord : le rang que rend le
 * serveur devient une ligne de grille, et une grille mal câblée dirait le cycle
 * faux sans qu'aucun autre test ne s'en aperçoive. L'ordre du DOM ensuite : les
 * phases doivent y rester dans leur ordre de lecture quelle que soit la
 * disposition visuelle, sans quoi un lecteur d'écran entend le cycle dans le
 * désordre. L'édition enfin, et surtout ce qu'elle recharge : ajouter ou
 * supprimer une phase renumérote tout le cycle, et se contenter de la réponse
 * laisserait le V faux jusqu'au prochain chargement.
 */
describe('ApqpOverviewComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/apqp/phases`;

  let fixture: ComponentFixture<ApqpOverviewComponent>;
  let component: ApqpOverviewComponent;
  let http: HttpTestingController;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let router: Router;
  let dialog: MatDialog;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  /** Un cycle de cinq phases, tel que le serveur le rend : le rang y est calculé. */
  function cycle(): ApqpPhase[] {
    const niveaux = [1, 2, 3, 2, 1];
    return niveaux.map((level, i) => ({
      id: `p${i + 1}`,
      position: i + 1,
      level,
      title: `Phase ${i + 1}`,
      purpose: `Objet ${i + 1}`,
      question: `Question ${i + 1} ?`,
      deliverables: [
        { id: `l${i + 1}a`, position: 1, label: `Livrable ${i + 1}A` },
        { id: `l${i + 1}b`, position: 2, label: `Livrable ${i + 1}B` }
      ]
    }));
  }

  /** Répond au chargement du cycle et laisse l'écran se peindre. */
  function servirCycle(phases: ApqpPhase[] = cycle()): void {
    http.expectOne(endpoint).flush(phases);
    fixture.detectChanges();
  }

  async function setup(phase?: string, roles: string[] = ['QUALITY_MANAGER']): Promise<void> {
    params = new BehaviorSubject(convertToParamMap(phase ? { phase } : {}));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpOverviewComponent],
      imports: [SharedModule, UiModule, RouterTestingModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: params.asObservable() } },
        { provide: AuthService, useValue: { hasAnyRole: () => roles.length > 0 } }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    dialog = TestBed.inject(MatDialog);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(ApqpOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Fait répondre au prochain dialogue ouvert la saisie donnée. */
  function repondre<T>(saisie: T | undefined): void {
    spyOn(dialog, 'open').and.returnValue({
      afterClosed: () => of(saisie)
    } as MatDialogRef<unknown>);
  }

  afterEach(() => http.verify());

  it('dessine le V à partir du rang que rend le serveur', async () => {
    // La forme n'est pas décorative — elle dit que le milieu du V est le point
    // où tout se joue avant de pouvoir remonter. Le rang vient du serveur ; ce
    // qui se vérifie ici, c'est qu'il atteigne bien la grille.
    await setup();
    servirCycle();

    const rangees = Array.from(hote().querySelectorAll('.v__case'))
      .map(e => (e as HTMLElement).style.gridRow);
    expect(rangees).toEqual(['1', '2', '3', '2', '1']);
  });

  it('donne une colonne à chaque phase, quel qu’en soit le nombre', async () => {
    // Six phases ne doivent pas se tasser sur cinq colonnes : c'est exactement
    // ce qui écrasait le V en L quand le nombre était figé dans la feuille.
    const six = cycle();
    six.push({
      id: 'p6', position: 6, level: 1, title: 'Phase 6',
      purpose: null, question: null, deliverables: []
    });
    await setup();
    servirCycle(six);

    // Le navigateur normalise le style relu (`0` devient `0px`) : on compare
    // donc ce que POSE le composant, et sur le DOM le seul point qui compte —
    // six colonnes, une par phase.
    expect(component.colonnes).toBe('repeat(6, minmax(0, 1fr))');
    expect(hote().querySelector<HTMLElement>('.v')!.style.gridTemplateColumns)
      .toContain('repeat(6,');
    const colonnes = Array.from(hote().querySelectorAll('.v__case'))
      .map(e => (e as HTMLElement).style.gridColumn);
    expect(colonnes).toEqual(['1', '2', '3', '4', '5', '6']);
  });

  it('garde les phases dans l’ordre de lecture, quelle que soit la disposition', async () => {
    // La grille les place en V ; le DOM, lui, reste la séquence 1→5, sinon un
    // lecteur d'écran entend le cycle dans le désordre.
    await setup();
    servirCycle();

    const rangs = Array.from(hote().querySelectorAll('.jalon__rang'))
      .map(e => (e as HTMLElement).textContent!.trim());
    expect(rangs).toEqual(['1', '2', '3', '4', '5']);
  });

  it('n’ouvre rien sur /apqp, et invite à choisir', async () => {
    await setup();
    servirCycle();

    expect(component.choisie).toBeUndefined();
    expect(hote().querySelector('.detail')).toBeNull();
    expect(hote().querySelector('.invite')).not.toBeNull();
  });

  it('ouvre la phase que porte l’URL, avec ses livrables', async () => {
    await setup('4');
    servirCycle();

    expect(component.choisie!.position).toBe(4);
    expect(hote().querySelectorAll('.livrables li').length).toBe(2);
    expect(hote().querySelector('.invite')).toBeNull();
  });

  it('suit un changement de segment sans être recréé', async () => {
    await setup('1');
    servirCycle();
    expect(component.choisie!.position).toBe(1);

    params.next(convertToParamMap({ phase: '5' }));
    fixture.detectChanges();

    expect(component.choisie!.position).toBe(5);
  });

  it('referme la phase quand on reclique sur son jalon', async () => {
    // Recliquer doit défaire, pas laisser l'écran dans le même état : c'est ce
    // qu'attend quiconque a déjà refermé un accordéon.
    await setup('4');
    servirCycle();

    component.basculer(component.choisie!);
    expect(router.navigate).toHaveBeenCalledWith(['/apqp']);
  });

  it('ouvre une autre phase quand on clique sur un autre jalon', async () => {
    await setup('4');
    servirCycle();

    component.basculer(component.phases[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/apqp', '1']);
  });

  it('revient au schéma quand le segment ne désigne aucune phase', async () => {
    // Lien périmé, phase supprimée depuis : un écran vide n'expliquerait rien.
    await setup('42');
    servirCycle();

    expect(router.navigate).toHaveBeenCalledWith(['/apqp']);
    expect(component.choisie).toBeUndefined();
  });

  it('recharge tout le cycle après un ajout de phase', async () => {
    // Une phase de plus décale le rang de TOUTES les autres dans le V : se fier
    // à la seule réponse laisserait le schéma faux.
    await setup();
    servirCycle();
    repondre({ title: 'Phase 6' });

    component.ajouterPhase();

    http.expectOne({ url: endpoint, method: 'POST' }).flush(cycle()[0]);
    http.expectOne(endpoint).flush(cycle());
  });

  it('recharge tout le cycle après une suppression, et revient au schéma', async () => {
    await setup('3');
    servirCycle();
    spyOn(window, 'confirm').and.returnValue(true);

    component.supprimerPhase(component.choisie!);

    http.expectOne({ url: `${endpoint}/p3`, method: 'DELETE' }).flush(null);
    expect(router.navigate).toHaveBeenCalledWith(['/apqp']);
    http.expectOne(endpoint).flush(cycle());
  });

  it('ne supprime rien si la question est déclinée', async () => {
    // Le geste est définitif : un « Annuler » doit vraiment tout arrêter.
    await setup('3');
    servirCycle();
    spyOn(window, 'confirm').and.returnValue(false);

    component.supprimerPhase(component.choisie!);

    http.expectNone({ url: `${endpoint}/p3`, method: 'DELETE' });
  });

  it('remplace la phase sur place quand on la renomme, sans tout recharger', async () => {
    // Renommer ne change ni l'ordre ni le nombre : recharger ferait clignoter
    // le schéma pour rien.
    await setup('2');
    servirCycle();
    repondre({ title: 'Rebaptisée' });

    component.modifierPhase(component.choisie!);

    const mise = { ...cycle()[1], title: 'Rebaptisée' };
    http.expectOne({ url: `${endpoint}/p2`, method: 'PUT' }).flush(mise);
    fixture.detectChanges();

    expect(component.choisie!.title).toBe('Rebaptisée');
    expect(hote().querySelector('.detail__titre')!.textContent).toContain('Rebaptisée');
    http.expectNone(endpoint);
  });

  it('reprend la phase entière que rend l’ajout d’un livrable', async () => {
    // Le serveur renumérote les livrables : l'écran reprend ce qu'il rend au
    // lieu de deviner un ordre.
    await setup('1');
    servirCycle();
    repondre({ label: 'Nouveau livrable' });

    component.ajouterLivrable(component.choisie!);

    const mise = { ...cycle()[0] };
    mise.deliverables = [...mise.deliverables,
      { id: 'l1c', position: 3, label: 'Nouveau livrable' }];
    http.expectOne({ url: `${endpoint}/p1/deliverables`, method: 'POST' }).flush(mise);
    fixture.detectChanges();

    expect(hote().querySelectorAll('.livrables li').length).toBe(3);
  });

  it('retire un livrable après confirmation', async () => {
    await setup('1');
    servirCycle();
    spyOn(window, 'confirm').and.returnValue(true);

    const livrable = component.choisie!.deliverables[0];
    component.supprimerLivrable(component.choisie!, livrable);

    const mise = { ...cycle()[0] };
    mise.deliverables = mise.deliverables.slice(1);
    http.expectOne({ url: `${endpoint}/p1/deliverables/l1a`, method: 'DELETE' }).flush(mise);
    fixture.detectChanges();

    expect(hote().querySelectorAll('.livrables li').length).toBe(1);
  });

  it('n’envoie rien quand le dialogue est annulé', async () => {
    await setup('1');
    servirCycle();
    repondre(undefined);

    component.modifierPhase(component.choisie!);

    http.expectNone({ url: `${endpoint}/p1`, method: 'PUT' });
  });

  it('cache toute commande d’édition à qui n’a pas le droit d’écrire', async () => {
    // Le serveur reste l'autorité ; l'écran ne propose simplement pas un geste
    // qui répondrait 403.
    await setup('1', []);
    servirCycle();

    expect(component.editable).toBeFalse();
    expect(hote().querySelector('.detail__commandes')).toBeNull();
    expect(hote().querySelector('.livrables__actions')).toBeNull();
  });

  it('envoie le cycle entier quand on avance une phase, et suit son nouveau rang', async () => {
    // Une phase ajoutée arrive en fin de cycle : sans déplacement, elle y
    // resterait à jamais. L'ordre part ENTIER — le serveur refuse un ordre
    // partiel — et le rang étant le segment d'URL, l'adresse doit suivre.
    await setup('3');
    servirCycle();

    component.deplacer(component.choisie!, -1);

    const requete = http.expectOne({ url: `${endpoint}/order`, method: 'PUT' });
    expect(requete.request.body).toEqual({ phaseIds: ['p1', 'p3', 'p2', 'p4', 'p5'] });

    const permute = cycle();
    permute[1] = { ...cycle()[2], position: 2, level: 2 };
    permute[2] = { ...cycle()[1], position: 3, level: 3 };
    requete.flush(permute);

    expect(component.choisie!.id).toBe('p3');
    expect(component.choisie!.position).toBe(2);
    expect(router.navigate).toHaveBeenCalledWith(['/apqp', '2']);
  });

  it('n’envoie rien quand la phase est déjà au bout du cycle', async () => {
    // Le bouton est désactivé aux extrémités ; la garde tient aussi si l'appel
    // vient d'ailleurs, faute de quoi le serveur recevrait l'ordre inchangé.
    await setup('1');
    servirCycle();

    expect(component.peutAvancer(component.choisie!)).toBeFalse();
    component.deplacer(component.choisie!, -1);

    http.expectNone({ url: `${endpoint}/order`, method: 'PUT' });
  });

  it('désactive les flèches aux deux extrémités du cycle', async () => {
    await setup('5');
    servirCycle();

    const fleches = Array.from(
      hote().querySelectorAll<HTMLButtonElement>('.detail__commandes button[mat-icon-button]'));
    expect(fleches.length).toBe(2);
    expect(fleches[0].disabled).toBeFalse();  // la dernière phase peut avancer
    expect(fleches[1].disabled).toBeTrue();   // mais pas reculer davantage
  });

  it('propose de repartir quand le cycle est vide', async () => {
    await setup();
    servirCycle([]);

    expect(hote().querySelector('.invite')!.textContent).toContain('vide');
  });
});
