import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Idea, IdeaColumn } from '../../ideas.types';
import { IdeasBoardComponent } from './ideas-board.component';

/**
 * Le tableau des idées.
 *
 * <p>Trois choses se vérifient ici. Le rangement en colonnes d'abord, et sa
 * fusion IMPLEMENTED+MEASURED sous « Réalisée ». Le vote ensuite, et surtout
 * l'ABSENCE de compteur optimiste : le serveur est la seule source du
 * décompte, l'incrémenter localement afficherait un chiffre faux. L'arbitrage
 * enfin, offert à un rôle et masqué à qui n'en a pas — le serveur reste
 * l'autorité, l'écran ne propose simplement pas un geste qui répondrait 403.
 */
describe('IdeasBoardComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/ideas`;

  let fixture: ComponentFixture<IdeasBoardComponent>;
  let component: IdeasBoardComponent;
  let http: HttpTestingController;
  let dialog: MatDialog;
  let snack: MatSnackBar;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  /** Une idée par défaut, telle que le serveur la rend. */
  function idee(id: string, overrides: Partial<Idea> = {}): Idea {
    return {
      id,
      title: `Idée ${id}`,
      description: null,
      status: 'PROPOSED',
      authorId: 'u1',
      authorName: 'Alice Dupont',
      votes: 8,
      votedByMe: false,
      voteOpen: true,
      circleId: null,
      rejectionReason: null,
      impactNote: null,
      createdAt: '2026-09-01T08:00:00Z',
      ...overrides
    };
  }

  /** Le tableau par défaut : deux idées soumises, les autres colonnes vides. */
  function tableauParDefaut(): IdeaColumn[] {
    return [
      { status: 'PROPOSED', ideas: [idee('i1'), idee('i2')] },
      { status: 'UNDER_REVIEW', ideas: [] },
      { status: 'APPROVED', ideas: [] },
      { status: 'REJECTED', ideas: [] },
      { status: 'IMPLEMENTED', ideas: [] },
      { status: 'MEASURED', ideas: [] }
    ];
  }

  /** Répond au chargement du tableau et laisse l'écran se peindre. */
  function servirTableau(columns: IdeaColumn[] = tableauParDefaut()): void {
    http.expectOne(endpoint).flush({ columns });
    fixture.detectChanges();
  }

  async function setup(roles: string[] = ['QUALITY_MANAGER']): Promise<void> {
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [IdeasBoardComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { hasAnyRole: () => roles.length > 0 } }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    dialog = TestBed.inject(MatDialog);
    snack = TestBed.inject(MatSnackBar);

    fixture = TestBed.createComponent(IdeasBoardComponent);
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

  it('range chaque idée dans sa colonne', async () => {
    await setup();
    servirTableau();

    const colonnes = Array.from(hote().querySelectorAll('.colonne'));
    expect(colonnes.length).toBe(4);
    expect(colonnes[0].querySelectorAll('.carte').length).toBe(2);
  });

  it('affiche le nombre de voix et l’auteur de chaque idée', async () => {
    await setup();
    servirTableau();

    const carte = hote().querySelector('.carte')!;
    expect(carte.querySelector('.carte__voix')!.textContent).toContain('8');
    expect(carte.querySelector('.carte__auteur')!.textContent!.trim()).not.toBe('');
  });

  it('voter envoie un POST et reprend l’idée que rend le serveur', async () => {
    await setup();
    servirTableau();

    component.basculerVote(component.colonnes[0].ideas[0]);

    const requete = http.expectOne({ url: `${endpoint}/i1/vote`, method: 'POST' });
    requete.flush({ ...idee('i1'), votes: 9, votedByMe: true });
    fixture.detectChanges();

    expect(component.colonnes[0].ideas[0].votes).toBe(9);
  });

  it('recliquer retire la voix', async () => {
    await setup();
    servirTableau([{ status: 'PROPOSED', ideas: [{ ...idee('i1'), votedByMe: true }] }]);

    component.basculerVote(component.colonnes[0].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/vote`, method: 'DELETE' }).flush(idee('i1'));
    fixture.detectChanges();
  });

  it('cache le bouton de vote sur une idée déjà tranchée', async () => {
    await setup();
    servirTableau([{ status: 'APPROVED', ideas: [{ ...idee('i1'), voteOpen: false }] }]);

    expect(hote().querySelector('.carte__vote')).toBeNull();
  });

  it('cache les commandes d’arbitrage à qui n’a pas le droit', async () => {
    await setup([]);            // aucun rôle
    servirTableau();

    expect(component.editable).toBeFalse();
    expect(hote().querySelector('.carte__arbitrage')).toBeNull();
  });

  it('recharge le tableau après un dépôt, car les colonnes bougent', async () => {
    await setup();
    servirTableau();
    repondre({ title: 'Bac de tri' });

    component.deposer();

    http.expectOne({ url: endpoint, method: 'POST' }).flush(idee('i9'));
    http.expectOne(endpoint).flush({ columns: [] });
  });

  it('n’envoie rien quand le dépôt est annulé', async () => {
    await setup();
    servirTableau();
    repondre(undefined);

    component.deposer();

    http.expectNone({ url: endpoint, method: 'POST' });
  });

  it('fusionne IMPLEMENTED et MEASURED sous « Réalisée »', async () => {
    await setup();
    servirTableau([
      { status: 'PROPOSED', ideas: [] },
      { status: 'UNDER_REVIEW', ideas: [] },
      { status: 'APPROVED', ideas: [] },
      { status: 'REJECTED', ideas: [] },
      { status: 'IMPLEMENTED', ideas: [idee('i1')] },
      { status: 'MEASURED', ideas: [idee('i2')] }
    ]);

    expect(component.colonnes[3].ideas.map(i => i.id)).toEqual(['i1', 'i2']);
    const colonnes = Array.from(hote().querySelectorAll('.colonne'));
    expect(colonnes[3].querySelectorAll('.carte').length).toBe(2);
  });

  it('ne montre pas l’auteur quand il est absent', async () => {
    await setup();
    servirTableau([{ status: 'PROPOSED', ideas: [{ ...idee('i1'), authorName: null }] }]);

    expect(hote().querySelector('.carte__auteur')).toBeNull();
  });

  it('étudier une idée soumise envoie un PATCH et recharge', async () => {
    await setup();
    servirTableau();

    component.etudier(component.colonnes[0].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/review`, method: 'PATCH' })
      .flush(idee('i1', { status: 'UNDER_REVIEW' }));
    http.expectOne(endpoint).flush({ columns: [] });
  });

  it('retenir une idée à l’étude envoie un PATCH et recharge', async () => {
    await setup();
    servirTableau([{ status: 'UNDER_REVIEW', ideas: [idee('i1')] }]);

    component.retenir(component.colonnes[1].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/approve`, method: 'PATCH' })
      .flush(idee('i1', { status: 'APPROVED' }));
    http.expectOne(endpoint).flush({ columns: [] });
  });

  it('écarter une idée demande un motif et l’envoie au serveur', async () => {
    await setup();
    servirTableau([{ status: 'UNDER_REVIEW', ideas: [idee('i1')] }]);
    repondre({ text: 'Déjà tenté sans succès' });

    component.ecarter(component.colonnes[1].ideas[0]);

    const requete = http.expectOne({ url: `${endpoint}/i1/reject`, method: 'PATCH' });
    expect(requete.request.body).toEqual({ reason: 'Déjà tenté sans succès' });
    requete.flush(idee('i1', { status: 'REJECTED' }));
    http.expectOne(endpoint).flush({ columns: [] });
  });

  it('n’envoie rien quand on renonce à écarter', async () => {
    await setup();
    servirTableau([{ status: 'UNDER_REVIEW', ideas: [idee('i1')] }]);
    repondre(undefined);

    component.ecarter(component.colonnes[1].ideas[0]);

    http.expectNone({ url: `${endpoint}/i1/reject`, method: 'PATCH' });
  });

  it('réaliser une idée retenue envoie un PATCH et recharge', async () => {
    await setup();
    servirTableau([{ status: 'APPROVED', ideas: [idee('i1')] }]);

    component.realiser(component.colonnes[2].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/implement`, method: 'PATCH' })
      .flush(idee('i1', { status: 'IMPLEMENTED' }));
    http.expectOne(endpoint).flush({ columns: [] });
  });

  it('consigner l’impact envoie un PATCH avec la note et recharge', async () => {
    await setup();
    servirTableau([
      { status: 'PROPOSED', ideas: [] }, { status: 'UNDER_REVIEW', ideas: [] },
      { status: 'APPROVED', ideas: [] }, { status: 'REJECTED', ideas: [] },
      { status: 'IMPLEMENTED', ideas: [idee('i1')] }
    ]);
    repondre({ text: 'Gain de dix minutes par poste' });

    component.mesurer(component.colonnes[3].ideas[0]);

    const requete = http.expectOne({ url: `${endpoint}/i1/impact`, method: 'PATCH' });
    expect(requete.request.body).toEqual({ impactNote: 'Gain de dix minutes par poste' });
    requete.flush(idee('i1', { status: 'MEASURED' }));
    http.expectOne(endpoint).flush({ columns: [] });
  });

  it('n’envoie rien quand on renonce à consigner l’impact', async () => {
    await setup();
    servirTableau([
      { status: 'PROPOSED', ideas: [] }, { status: 'UNDER_REVIEW', ideas: [] },
      { status: 'APPROVED', ideas: [] }, { status: 'REJECTED', ideas: [] },
      { status: 'IMPLEMENTED', ideas: [idee('i1')] }
    ]);
    repondre(undefined);

    component.mesurer(component.colonnes[3].ideas[0]);

    http.expectNone({ url: `${endpoint}/i1/impact`, method: 'PATCH' });
  });

  it('propose de découvrir les idées écartées, repliées par défaut', async () => {
    await setup();
    servirTableau([{ status: 'REJECTED', ideas: [idee('i1', { rejectionReason: 'Coût trop élevé' })] }]);

    expect(component.ecartees.length).toBe(1);
    expect(hote().querySelector('.ecartees__cartes')).toBeNull();

    component.montrerEcartees = true;
    fixture.detectChanges();

    expect(hote().querySelectorAll('.ecartees__cartes .carte').length).toBe(1);
    expect(hote().querySelector('.carte__motif')!.textContent).toContain('Coût trop élevé');
  });

  it('n’affiche pas la section des idées écartées si elle est vide', async () => {
    await setup();
    servirTableau();

    expect(hote().querySelector('.ecartees')).toBeNull();
  });

  it('signale une erreur sans planter si le tableau ne charge pas', async () => {
    await setup();
    spyOn(snack, 'open');

    http.expectOne(endpoint).flush('boom', { status: 500, statusText: 'Server Error' });

    expect(component.loading).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si un vote échoue', async () => {
    await setup();
    servirTableau();
    spyOn(snack, 'open');

    component.basculerVote(component.colonnes[0].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/vote`, method: 'POST' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si le dépôt échoue', async () => {
    await setup();
    servirTableau();
    repondre({ title: 'Bac de tri' });
    spyOn(snack, 'open');

    component.deposer();

    http.expectOne({ url: endpoint, method: 'POST' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si la mise à l’étude échoue', async () => {
    await setup();
    servirTableau();
    spyOn(snack, 'open');

    component.etudier(component.colonnes[0].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/review`, method: 'PATCH' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si retenir échoue', async () => {
    await setup();
    servirTableau([{ status: 'UNDER_REVIEW', ideas: [idee('i1')] }]);
    spyOn(snack, 'open');

    component.retenir(component.colonnes[1].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/approve`, method: 'PATCH' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si écarter échoue', async () => {
    await setup();
    servirTableau([{ status: 'UNDER_REVIEW', ideas: [idee('i1')] }]);
    repondre({ text: 'Motif' });
    spyOn(snack, 'open');

    component.ecarter(component.colonnes[1].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/reject`, method: 'PATCH' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si réaliser échoue', async () => {
    await setup();
    servirTableau([{ status: 'APPROVED', ideas: [idee('i1')] }]);
    spyOn(snack, 'open');

    component.realiser(component.colonnes[2].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/implement`, method: 'PATCH' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });

  it('signale une erreur sans planter si consigner l’impact échoue', async () => {
    await setup();
    servirTableau([
      { status: 'PROPOSED', ideas: [] }, { status: 'UNDER_REVIEW', ideas: [] },
      { status: 'APPROVED', ideas: [] }, { status: 'REJECTED', ideas: [] },
      { status: 'IMPLEMENTED', ideas: [idee('i1')] }
    ]);
    repondre({ text: 'Gain de dix minutes par poste' });
    spyOn(snack, 'open');

    component.mesurer(component.colonnes[3].ideas[0]);

    http.expectOne({ url: `${endpoint}/i1/impact`, method: 'PATCH' })
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snack.open).toHaveBeenCalled();
  });
});
