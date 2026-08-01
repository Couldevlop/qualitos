import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { WebhookSecretDialogComponent, WebhookSecretDialogData } from './webhook-secret-dialog.component';

describe('WebhookSecretDialogComponent', () => {
  let dialogRef: jasmine.SpyObj<MatDialogRef<WebhookSecretDialogComponent>>;

  const SECRET = 'c'.repeat(64);

  /**
   * On masque `navigator.clipboard` par une propriété propre plutôt que d'espionner le
   * prototype : l'API n'existe pas dans tous les contextes d'exécution, et une propriété
   * propre supprimable restaure l'état d'origine quel que soit le navigateur du runner.
   */
  function stubClipboard(value: Clipboard | undefined): void {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value });
  }

  async function setup(data: WebhookSecretDialogData): Promise<ComponentFixture<WebhookSecretDialogComponent>> {
    await TestBed.configureTestingModule({
      declarations: [WebhookSecretDialogComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    return TestBed.createComponent(WebhookSecretDialogComponent);
  }

  beforeEach(() => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<WebhookSecretDialogComponent>>('MatDialogRef', ['close']);
  });

  afterEach(() => {
    delete (navigator as unknown as Record<string, unknown>)['clipboard'];
  });

  it('affiche le secret en clair — c\'est la seule occasion de le lire', async () => {
    const fixture = await setup({ subscriptionName: 'ERP', secret: SECRET, rotated: false });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain(SECRET);
    expect(text).toContain('ERP');
  });

  it('copie le secret dans le presse-papiers et le confirme', async () => {
    const fixture = await setup({ subscriptionName: 'ERP', secret: SECRET, rotated: false });
    const c = fixture.componentInstance;
    const writeText = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
    stubClipboard({ writeText } as unknown as Clipboard);

    c.copy();
    await fixture.whenStable();

    expect(writeText).toHaveBeenCalledWith(SECRET);
    expect(c.copied).toBeTrue();
    expect(c.copyFailed).toBeFalse();
  });

  it('signale l\'échec de copie plutôt que de laisser croire au succès', async () => {
    const fixture = await setup({ subscriptionName: 'ERP', secret: SECRET, rotated: false });
    const c = fixture.componentInstance;
    const writeText = jasmine.createSpy('writeText').and.returnValue(Promise.reject(new Error('denied')));
    stubClipboard({ writeText } as unknown as Clipboard);

    c.copy();
    await fixture.whenStable();

    expect(c.copied).toBeFalse();
    expect(c.copyFailed).toBeTrue();
  });

  it('bascule sur la copie manuelle quand l\'API presse-papiers est absente', async () => {
    const fixture = await setup({ subscriptionName: 'ERP', secret: SECRET, rotated: false });
    const c = fixture.componentInstance;
    stubClipboard(undefined);

    c.copy();

    expect(c.copied).toBeFalse();
    expect(c.copyFailed).toBeTrue();
  });

  it('avertit de l\'invalidation de l\'ancien secret lors d\'une rotation', async () => {
    const fixture = await setup({ subscriptionName: 'ERP', secret: SECRET, rotated: true });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('remplace le précédent');
  });

  it('ferme le dialogue', async () => {
    const fixture = await setup({ subscriptionName: 'ERP', secret: SECRET, rotated: false });
    fixture.componentInstance.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
