import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { RateLimitsService } from '../../rate-limits.service';
import {
  RateLimitDecision, RateLimitOverview, RateLimitPolicy, RateLimitRow
} from '../../rate-limits.types';
import { QuotasComponent } from './quotas.component';

describe('QuotasComponent', () => {
  let component: QuotasComponent;
  let fixture: ComponentFixture<QuotasComponent>;
  let svc: jasmine.SpyObj<RateLimitsService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const policy = (scope: string, over: Partial<RateLimitPolicy> = {}): RateLimitPolicy => ({
    id: 'p-' + scope, tenantId: 't1', scope, windowSeconds: 60, maxRequests: 100,
    enabled: true, createdAt: '2026-07-01T10:00:00Z', updatedAt: '2026-07-01T10:00:00Z', ...over
  });

  const decision = (over: Partial<RateLimitDecision> = {}): RateLimitDecision => ({
    allowed: true, limit: 100, remaining: 20, retryAfterSeconds: 0, resetSeconds: 30, ...over
  });

  const row = (p: RateLimitPolicy, over: Partial<RateLimitRow> = {}): RateLimitRow => ({
    policy: p, usage: decision(), used: 80, ratio: 0.8, level: 'critical', ...over
  });

  const overview = (rows: RateLimitRow[]): RateLimitOverview => ({
    rows,
    totals: {
      policies: rows.length,
      enforced: rows.filter(r => r.policy.enabled).length,
      nearLimit: rows.filter(r => r.level === 'critical').length,
      exceeded: rows.filter(r => r.level === 'exceeded').length
    }
  });

  /** Ligne critique par défaut : c'est le cas que l'écran doit rendre évident. */
  const hot = row(policy('api.documents'));

  beforeEach(async () => {
    svc = jasmine.createSpyObj<RateLimitsService>('RateLimitsService',
      ['overview', 'savePolicy', 'delete']);
    svc.overview.and.returnValue(of(overview([hot])));
    svc.savePolicy.and.returnValue(of(policy('api.documents')));
    svc.delete.and.returnValue(of(undefined));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      declarations: [QuotasComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: RateLimitsService, useValue: svc },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(QuotasComponent);
    component = fixture.componentInstance;
  });

  /** Dernier message d'erreur exposé au template. */
  function currentError(): string | null {
    let msg: string | null = null;
    component.error$.subscribe(v => msg = v).unsubscribe();
    return msg;
  }

  function rowsSnapshot(): RateLimitRow[] {
    let snapshot: RateLimitRow[] = [];
    component.rows$.subscribe(r => snapshot = r).unsubscribe();
    return snapshot;
  }

  // ---- Rendu -----------------------------------------------------------------

  it('affiche une ligne par quota avec sa jauge de consommation', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.scope').length).toBe(1);
    const track = el.querySelector('.meter__track') as HTMLElement;
    expect(track.getAttribute('aria-valuenow')).toBe('80');
    expect(track.getAttribute('data-level')).toBe('critical');
    expect(el.querySelector('.empty')).toBeNull();
  });

  it('rend les tuiles de synthèse à partir des totaux', () => {
    fixture.detectChanges();
    const tiles = (fixture.nativeElement as HTMLElement).querySelectorAll('.tile__value');
    expect(tiles.length).toBe(4);
    expect(Array.from(tiles).map(t => t.textContent?.trim())).toEqual(['1', '1', '1', '0']);
  });

  it('signale l\'absence totale de quota plutôt que d\'afficher un tableau nu', fakeAsync(() => {
    svc.overview.and.returnValue(of(overview([])));
    fixture.detectChanges();
    tick(); // deferredView livre `loading=false` en macrotâche
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.empty')).not.toBeNull();
  }));

  it('n\'affiche pas de jauge pour un quota suspendu : rien n\'est compté', () => {
    svc.overview.and.returnValue(of(overview([
      row(policy('api.nc', { enabled: false }), { usage: null, used: 0, ratio: 0, level: 'unmeasured' })
    ])));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.meter')).toBeNull();
    expect(el.textContent).toContain('Non mesuré');
  });

  it('expose un message lisible quand le chargement échoue', () => {
    svc.overview.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();
    expect(currentError()).toBe('Erreur serveur — réessayez dans un instant.');
  });

  it('recharge la vue à la demande', () => {
    fixture.detectChanges();
    component.refresh();
    expect(svc.overview).toHaveBeenCalledTimes(2);
  });

  it('horodate la mesure affichée', () => {
    fixture.detectChanges();
    let at: Date | null = null;
    component.measuredAt$.subscribe(d => at = d).unsubscribe();
    expect(at).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.measured')).not.toBeNull();
  });

  // ---- Formulaire ------------------------------------------------------------

  it('charge la ligne dans le formulaire et verrouille le périmètre', () => {
    fixture.detectChanges();
    component.edit(hot);
    expect(component.editing?.id).toBe('p-api.documents');
    expect(component.form.controls.scope.disabled).toBeTrue();
    expect(component.form.getRawValue().scope).toBe('api.documents');
    expect(component.form.controls.maxRequests.value).toBe(100);
  });

  it('libère le périmètre en quittant l\'édition', () => {
    fixture.detectChanges();
    component.edit(hot);
    component.cancelEdit();
    expect(component.editing).toBeNull();
    expect(component.form.controls.scope.enabled).toBeTrue();
    expect(component.form.controls.windowSeconds.value).toBe(60);
  });

  it('refuse d\'envoyer un formulaire invalide', () => {
    fixture.detectChanges();
    component.form.controls.scope.setValue('MAJUSCULES INTERDITES');
    component.submit();
    expect(svc.savePolicy).not.toHaveBeenCalled();
    expect(component.form.controls.scope.touched).toBeTrue();
  });

  it('crée un quota sans identifiant précédent, puis vide le formulaire', () => {
    fixture.detectChanges();
    component.form.setValue({
      scope: 'api.nc', windowSeconds: 3600, maxRequests: 500, enabled: true
    });
    component.submit();
    expect(svc.savePolicy).toHaveBeenCalledWith(
      { scope: 'api.nc', windowSeconds: 3600, maxRequests: 500, enabled: true });
    expect(component.form.controls.scope.value).toBe('');
    expect(svc.overview).toHaveBeenCalledTimes(2);
  });

  it('transmet le périmètre verrouillé lors d\'une modification', () => {
    fixture.detectChanges();
    component.edit(hot);
    component.form.controls.maxRequests.setValue(250);
    component.submit();
    expect(svc.savePolicy).toHaveBeenCalledWith(
      { scope: 'api.documents', windowSeconds: 60, maxRequests: 250, enabled: true });
    expect(component.editing).toBeNull();
  });

  it('nettoie les espaces d\'un périmètre collé, plutôt que de le déclarer invalide', () => {
    fixture.detectChanges();
    component.form.controls.scope.setValue('  api.nc  ');
    expect(component.form.controls.scope.valid).toBeFalse();
    component.trimScope();
    expect(component.form.controls.scope.value).toBe('api.nc');
    expect(component.form.controls.scope.valid).toBeTrue();
    // Idempotent : une valeur déjà propre n'est pas réécrite.
    component.trimScope();
    expect(component.form.controls.scope.value).toBe('api.nc');
  });

  it('remonte l\'échec d\'enregistrement sans vider la saisie', () => {
    svc.savePolicy.and.returnValue(throwError(() => ({ status: 400 })));
    fixture.detectChanges();
    component.form.setValue({
      scope: 'api.nc', windowSeconds: 60, maxRequests: 10, enabled: true
    });
    component.submit();
    expect(currentError()).toBe('Champs invalides — vérifiez le formulaire.');
    expect(component.form.controls.scope.value).toBe('api.nc');
    expect(component.saving).toBeFalse();
  });

  it('ignore un second envoi pendant que le premier est en vol', () => {
    const pending = new Subject<RateLimitPolicy>();
    svc.savePolicy.and.returnValue(pending.asObservable());
    fixture.detectChanges();
    component.form.setValue({
      scope: 'api.nc', windowSeconds: 60, maxRequests: 10, enabled: true
    });
    component.submit();
    component.submit();
    expect(svc.savePolicy).toHaveBeenCalledTimes(1);
    pending.complete();
  });

  it('applique une fenêtre usuelle en un clic', () => {
    fixture.detectChanges();
    component.applyPreset(86400);
    expect(component.form.controls.windowSeconds.value).toBe(86400);
    expect(component.form.controls.windowSeconds.dirty).toBeTrue();
  });

  // ---- Actions de ligne ------------------------------------------------------

  it('suspend une limite appliquée en conservant ses paramètres', () => {
    fixture.detectChanges();
    component.setEnabled(hot, false);
    expect(svc.savePolicy).toHaveBeenCalledWith(
      { scope: 'api.documents', windowSeconds: 60, maxRequests: 100, enabled: false });
    expect(svc.overview).toHaveBeenCalledTimes(2);
    expect(component.pendingScope).toBeNull();
  });

  it('remet une limite suspendue en application', () => {
    const off = row(policy('api.nc', { enabled: false }),
      { usage: null, used: 0, ratio: 0, level: 'unmeasured' });
    fixture.detectChanges();
    component.setEnabled(off, true);
    expect(svc.savePolicy.calls.mostRecent().args[0].enabled).toBeTrue();
  });

  it('ignore une seconde action de ligne pendant que la première est en vol', () => {
    const pending = new Subject<RateLimitPolicy>();
    svc.savePolicy.and.returnValue(pending.asObservable());
    fixture.detectChanges();
    component.setEnabled(hot, false);
    expect(component.pendingScope).toBe('api.documents');
    component.setEnabled(hot, true);
    expect(svc.savePolicy).toHaveBeenCalledTimes(1);
    pending.complete();
    expect(component.pendingScope).toBeNull();
  });

  it('affiche un message quand une action de ligne échoue', () => {
    svc.savePolicy.and.returnValue(throwError(() => ({ status: 403 })));
    fixture.detectChanges();
    component.setEnabled(hot, false);
    expect(currentError()).toBe('Vous n\'avez pas les droits pour cette action.');
  });

  it('supprime le quota après confirmation', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    fixture.detectChanges();
    component.remove(hot);
    expect(svc.delete).toHaveBeenCalledWith('p-api.documents');
    expect(svc.overview).toHaveBeenCalledTimes(2);
  });

  it('ne supprime rien si la confirmation est annulée', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    fixture.detectChanges();
    component.remove(hot);
    expect(svc.delete).not.toHaveBeenCalled();
  });

  it('ferme le formulaire quand la ligne éditée est supprimée', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    fixture.detectChanges();
    component.edit(hot);
    component.remove(hot);
    expect(component.editing).toBeNull();
    expect(component.form.controls.scope.enabled).toBeTrue();
  });

  // ---- Présentation ----------------------------------------------------------

  it('exprime la fenêtre dans son unité la plus lisible', () => {
    expect(component.windowLabel(86400)).toBe('24 h');
    expect(component.windowLabel(3600)).toBe('1 h');
    expect(component.windowLabel(120)).toBe('2 min');
    expect(component.windowLabel(45)).toBe('45 s');
  });

  it('n\'annonce pas de remise à zéro pour un quota non mesuré', () => {
    const off = row(policy('api.nc', { enabled: false }),
      { usage: null, used: 0, ratio: 0, level: 'unmeasured' });
    expect(component.resetLabel(off)).toBeNull();
    expect(component.resetLabel(row(policy('a'), { usage: decision({ resetSeconds: 90 }) })))
      .toBe('90 s');
    expect(component.resetLabel(row(policy('a'), {
      usage: decision({ resetSeconds: -5 })
    }))).toBe('0 s');
  });

  it('affiche la part consommée en pourcentage entier', () => {
    expect(component.percent(row(policy('a'), { ratio: 0.836 }))).toBe(84);
  });

  it('donne à chaque niveau un libellé et un ton, la couleur ne suffit pas', () => {
    const levels = ['exceeded', 'critical', 'warning', 'normal', 'idle', 'unmeasured'] as const;
    levels.forEach(l => expect(component.levelLabel(l).length).toBeGreaterThan(0));
    expect(component.levelTone('exceeded')).toBe('danger');
    expect(component.levelTone('critical')).toBe('danger');
    expect(component.levelTone('warning')).toBe('warn');
    expect(component.levelTone('normal')).toBe('success');
    expect(component.levelTone('idle')).toBe('success');
    expect(component.levelTone('unmeasured')).toBe('neutral');
  });

  it('identifie les lignes par leur politique pour ne pas rejouer le DOM', () => {
    expect(component.trackByPolicy(0, hot)).toBe('p-api.documents');
  });

  it('conserve les lignes chargées entre deux abonnements tardifs', () => {
    fixture.detectChanges();
    expect(rowsSnapshot().length).toBe(1);
    // shareReplay(refCount:false) : relire le flux ne doit pas relancer la liste
    // ET toutes les sondes de consommation.
    expect(rowsSnapshot().length).toBe(1);
    expect(svc.overview).toHaveBeenCalledTimes(1);
  });

  it('n\'expose aucune ligne tant que le chargement a échoué', () => {
    svc.overview.and.returnValue(throwError(() => ({ status: 0 })));
    fixture.detectChanges();
    expect(rowsSnapshot()).toEqual([]);
    expect(currentError()).toBe('Service inaccessible — vérifiez votre connexion.');
  });
});
