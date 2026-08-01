import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApiKeyView, IssuedApiKey } from '../../api-keys.types';
import {
  ApiKeyCreateDialogComponent, futureInstantValidator, scopesFormatValidator, toExpiryInstant
} from './api-key-create-dialog.component';

/**
 * Le formulaire rejoue les règles du serveur (format des scopes, échéance future) pour
 * expliquer un refus pendant la saisie plutôt qu'après un 400.
 */
describe('ApiKeyCreateDialogComponent', () => {
  let component: ApiKeyCreateDialogComponent;
  let fixture: ComponentFixture<ApiKeyCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ApiKeyCreateDialogComponent, IssuedApiKey>>;
  let currentUser: AuthUser | null;

  const base = `${environment.apiBaseUrl}/api/v1/api-keys`;

  const view: ApiKeyView = {
    id: 'k1', tenantId: 't1', name: 'CI', prefix: 'pfx', scopes: [], status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z', createdBy: 'u1', expiresAt: null,
    lastUsedAt: null, revokedAt: null, revokedBy: null
  };
  const issued: IssuedApiKey = { view, plaintext: 'qos_pfx_secret' };

  beforeEach(async () => {
    currentUser = {
      userId: '11111111-1111-1111-1111-111111111111',
      tenantId: 't1', displayName: 'Admin', roles: ['ADMIN_TENANT']
    };
    dialogRef = jasmine.createSpyObj<MatDialogRef<ApiKeyCreateDialogComponent, IssuedApiKey>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ApiKeyCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApiKeyCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('avertit que le secret ne sera affiché qu\'une fois', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('.hint')?.textContent)
      .toContain('une seule fois');
  });

  it('exige un nom', () => {
    expect(component.form.controls.name.hasError('required')).toBeTrue();
    expect(component.form.invalid).toBeTrue();
  });

  it('refuse un nom trop long', () => {
    component.form.controls.name.setValue('x'.repeat(121));
    expect(component.form.controls.name.hasError('maxlength')).toBeTrue();
  });

  it('n\'appelle pas le serveur tant que le formulaire est invalide', () => {
    component.submit();
    expect(component.form.controls.name.touched).toBeTrue();
    http.expectNone(base);
  });

  it('normalise les portées saisies en vrac', () => {
    component.form.controls.scopes.setValue('capa.write, audits.read;capa.write  audits.read');
    expect(component.previewScopes).toEqual(['audits.read', 'capa.write']);
  });

  it('refuse une portée hors format serveur et nomme la fautive', () => {
    component.form.controls.scopes.setValue('audits.read Capa.Write');
    expect(component.form.controls.scopes.getError('scopeFormat')).toBe('Capa.Write');
  });

  it('accepte une échéance future et refuse une échéance passée', () => {
    expect(futureInstantValidator(new FormControl(''))).toBeNull();
    expect(futureInstantValidator(new FormControl('2999-01-01'))).toBeNull();
    expect(futureInstantValidator(new FormControl('2000-01-01'))).toEqual({ pastDate: true });
    expect(futureInstantValidator(new FormControl('pas-une-date'))).toEqual({ dateFormat: true });
  });

  it('convertit la date choisie en fin de journée UTC', () => {
    expect(toExpiryInstant('2030-01-02')).toBe('2030-01-02T23:59:59.999Z');
    expect(toExpiryInstant('  ')).toBeNull();
    expect(toExpiryInstant('n\'importe quoi')).toBeNull();
  });

  it('ne signale aucune portée invalide sur une saisie vide', () => {
    expect(scopesFormatValidator(new FormControl(''))).toBeNull();
    expect(scopesFormatValidator(new FormControl(null))).toBeNull();
  });

  it('émet la clé puis referme en remontant le secret', () => {
    component.form.setValue({ name: '  CI  ', scopes: 'audits.read', expiresAt: '2999-05-04' });

    component.submit();

    const req = http.expectOne(base);
    expect(req.request.body).toEqual({
      name: 'CI',
      scopes: ['audits.read'],
      expiresAt: '2999-05-04T23:59:59.999Z',
      actor: currentUser!.userId
    });
    req.flush(issued);

    expect(dialogRef.close).toHaveBeenCalledWith(issued);
    expect(component.submitting).toBeFalse();
  });

  it('envoie null quand aucune échéance n\'est choisie', () => {
    component.form.setValue({ name: 'Perenne', scopes: '', expiresAt: '' });

    component.submit();

    const req = http.expectOne(base);
    expect(req.request.body).toEqual({
      name: 'Perenne', scopes: [], expiresAt: null, actor: currentUser!.userId
    });
    req.flush(issued);
  });

  it('ignore une seconde soumission pendant l\'appel', () => {
    component.form.setValue({ name: 'CI', scopes: '', expiresAt: '' });

    component.submit();
    expect(component.submitting).toBeTrue();
    component.submit();

    const requests = http.match(base);
    expect(requests.length).toBe(1);
    requests[0].flush(issued);
  });

  it('garde le formulaire ouvert et affiche l\'erreur si le serveur refuse', () => {
    component.form.setValue({ name: 'CI', scopes: '', expiresAt: '' });

    component.submit();
    http.expectOne(base).flush('nope', { status: 400, statusText: 'Bad Request' });

    expect(component.error).toContain('Champs invalides');
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('invite à se reconnecter si la session ne porte plus d\'acteur', () => {
    currentUser = null;
    component.form.setValue({ name: 'CI', scopes: '', expiresAt: '' });

    component.submit();

    expect(component.error).toContain('Session expirée');
    http.expectNone(base);
  });

  it('referme sans rien émettre à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
