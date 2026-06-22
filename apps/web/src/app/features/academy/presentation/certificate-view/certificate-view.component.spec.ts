import { of, throwError } from 'rxjs';

import { CertificateViewComponent } from './certificate-view.component';
import { AcademyService } from '../../infrastructure/academy.service';
import { CertificateResponse } from '../../domain/academy.types';

describe('CertificateViewComponent', () => {
  let academy: jasmine.SpyObj<AcademyService>;
  let router: jasmine.SpyObj<any>;
  let sanitizer: jasmine.SpyObj<any>;
  let snack: jasmine.SpyObj<any>;
  let component: CertificateViewComponent;

  const route: any = { snapshot: { paramMap: { get: (_: string) => 'enr-1' } } };

  const cert: CertificateResponse = {
    id: 'cert-1', enrollmentId: 'enr-1', courseId: 'c1', code: 'CODE-1',
    courseCode: 'iso', courseTitle: 'ISO 9001', finalScore: 90,
    sha256: 'a'.repeat(64), anchorTxRef: 'tx-1', issuedAt: '2026-06-22T10:00:00Z',
    verifyUrl: 'https://qualitos.io/verify/academy/CODE-1',
    htmlContent: '<html>cert</html>'
  };

  beforeEach(() => {
    academy = jasmine.createSpyObj<AcademyService>('AcademyService', ['certificate']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    sanitizer = jasmine.createSpyObj('DomSanitizer', ['bypassSecurityTrustHtml']);
    snack = jasmine.createSpyObj('MatSnackBar', ['open']);
    sanitizer.bypassSecurityTrustHtml.and.returnValue('SAFE');
    component = new CertificateViewComponent(academy, route, router, sanitizer, snack);
  });

  it('loads and sanitizes the certificate html', () => {
    academy.certificate.and.returnValue(of(cert));
    component.ngOnInit();
    expect(academy.certificate).toHaveBeenCalledWith('enr-1');
    expect(component.certificate).toEqual(cert);
    expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith('<html>cert</html>');
    expect(component.safeHtml).toBe('SAFE');
  });

  it('shows a snackbar on error', () => {
    academy.certificate.and.returnValue(throwError(() => new Error('x')));
    component.ngOnInit();
    expect(snack.open).toHaveBeenCalled();
  });

  it('copyVerifyUrl writes to the clipboard', async () => {
    academy.certificate.and.returnValue(of(cert));
    component.ngOnInit();
    const writeText = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
    // @ts-expect-error override navigator.clipboard for test
    spyOnProperty(navigator, 'clipboard', 'get').and.returnValue({ writeText });
    component.copyVerifyUrl();
    expect(writeText).toHaveBeenCalledWith(cert.verifyUrl);
  });
});
