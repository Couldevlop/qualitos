import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApiKeyView, IssuedApiKey } from '../../api-keys.types';
import {
  ApiKeyRevealDialogComponent, ApiKeyRevealDialogData
} from './api-key-reveal-dialog.component';

/**
 * Seul endroit où le secret existe côté client : on vérifie qu'il est affiché, qu'il
 * est copiable même quand le presse-papiers est refusé, et que l'échec est dit.
 */
describe('ApiKeyRevealDialogComponent', () => {
  let component: ApiKeyRevealDialogComponent;
  let fixture: ComponentFixture<ApiKeyRevealDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ApiKeyRevealDialogComponent, void>>;
  let clipboardDescriptor: PropertyDescriptor | undefined;

  const view: ApiKeyView = {
    id: 'k1', tenantId: 't1', name: 'Intégration CI', prefix: 'ab12cd34',
    scopes: ['audits.read'], status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z',
    createdBy: 'u1', expiresAt: null, lastUsedAt: null, revokedAt: null, revokedBy: null
  };
  const issued: IssuedApiKey = { view, plaintext: 'qos_ab12cd34_supersecret' };

  const build = async (data: ApiKeyRevealDialogData) => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ApiKeyRevealDialogComponent, void>>(
      'MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [ApiKeyRevealDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ApiKeyRevealDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  /** Le presse-papiers n'est pas modifiable en écriture : on le remplace le temps du test. */
  const setClipboard = (value: unknown) =>
    Object.defineProperty(navigator, 'clipboard', { value, configurable: true, writable: true });

  beforeEach(() => {
    clipboardDescriptor = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
  });

  afterEach(() => {
    if (clipboardDescriptor) Object.defineProperty(navigator, 'clipboard', clipboardDescriptor);
    else Reflect.deleteProperty(navigator, 'clipboard');
  });

  it('affiche le secret et l\'avertissement d\'affichage unique', async () => {
    await build({ issued, rotated: false });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('#qos-api-key-secret')?.textContent).toContain('qos_ab12cd34_supersecret');
    expect(el.querySelector('.warn')?.textContent).toContain('plus jamais affiché');
  });

  it('précise après une rotation que l\'ancienne clé est révoquée', async () => {
    await build({ issued, rotated: true });
    expect((fixture.nativeElement as HTMLElement).querySelector('.rotated-note')?.textContent)
      .toContain('révoquée');
  });

  it('ne mentionne pas la rotation lors d\'une simple émission', async () => {
    await build({ issued, rotated: false });
    expect((fixture.nativeElement as HTMLElement).querySelector('.rotated-note')).toBeNull();
  });

  it('copie le secret via le presse-papiers', async () => {
    await build({ issued, rotated: false });
    const writeText = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
    setClipboard({ writeText });

    component.copy();
    await fixture.whenStable();

    expect(writeText).toHaveBeenCalledWith('qos_ab12cd34_supersecret');
    expect(component.copied).toBeTrue();
    expect(component.copyFailed).toBeFalse();
  });

  it('bascule sur execCommand quand le presse-papiers est refusé', async () => {
    await build({ issued, rotated: false });
    setClipboard({ writeText: () => Promise.reject(new Error('denied')) });
    const exec = spyOn(document, 'execCommand').and.returnValue(true);

    component.copy();
    await fixture.whenStable();

    expect(exec).toHaveBeenCalledWith('copy');
    expect(component.copied).toBeTrue();
    expect(component.copyFailed).toBeFalse();
  });

  it('bascule sur execCommand quand l\'API Clipboard est absente', async () => {
    await build({ issued, rotated: false });
    setClipboard(undefined);
    const exec = spyOn(document, 'execCommand').and.returnValue(true);

    component.copy();

    expect(exec).toHaveBeenCalledWith('copy');
    expect(component.copied).toBeTrue();
  });

  it('demande une copie manuelle quand aucun chemin ne fonctionne', async () => {
    await build({ issued, rotated: false });
    setClipboard({});
    spyOn(document, 'execCommand').and.returnValue(false);

    component.copy();
    fixture.detectChanges();

    expect(component.copied).toBeFalse();
    expect(component.copyFailed).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.copy-failed')).not.toBeNull();
  });

  it('traite une exception d\'execCommand comme un échec de copie', async () => {
    await build({ issued, rotated: false });
    setClipboard(undefined);
    spyOn(document, 'execCommand').and.throwError('bloqué');

    component.copy();

    expect(component.copyFailed).toBeTrue();
  });

  it('ne laisse aucun champ de copie temporaire dans le document', async () => {
    await build({ issued, rotated: false });
    setClipboard(undefined);
    spyOn(document, 'execCommand').and.returnValue(true);

    component.copy();

    expect(document.querySelectorAll('body > textarea').length).toBe(0);
  });

  it('referme le dialogue une fois la clé copiée', async () => {
    await build({ issued, rotated: false });
    component.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
