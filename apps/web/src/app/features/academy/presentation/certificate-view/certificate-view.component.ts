import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AcademyService } from '../../infrastructure/academy.service';
import { CertificateResponse } from '../../domain/academy.types';

/**
 * Affichage du certificat de complétion (§19.3) : rendu HTML signé, empreinte
 * SHA-256, référence d'ancrage blockchain et lien public de vérification (QR).
 *
 * <p>Le HTML du certificat provient du backend (échappé côté serveur, OWASP A03) ;
 * il est passé par {@link DomSanitizer#bypassSecurityTrustHtml} pour l'affichage
 * fidèle dans un conteneur isolé.</p>
 */
@Component({
  selector: 'qos-certificate-view',
  templateUrl: './certificate-view.component.html',
  styleUrls: ['./certificate-view.component.scss'],
  standalone: false
})
export class CertificateViewComponent implements OnInit {

  loading = false;
  certificate?: CertificateResponse;
  safeHtml?: SafeHtml;

  constructor(
    private readonly academy: AcademyService,
    private readonly route: ActivatedRoute,
    readonly router: Router,
    private readonly sanitizer: DomSanitizer,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const enrollmentId = this.route.snapshot.paramMap.get('enrollmentId') ?? '';
    this.loading = true;
    this.academy.certificate(enrollmentId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: cert => {
          this.certificate = cert;
          this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(cert.htmlContent);
        },
        error: () => this.snack.open(
          $localize`:@@academy.error.cert:Certificat indisponible.`, 'OK', { duration: 4000 })
      });
  }

  copyVerifyUrl(): void {
    if (this.certificate && navigator.clipboard) {
      navigator.clipboard.writeText(this.certificate.verifyUrl).then(() =>
        this.snack.open($localize`:@@academy.cert.copied:Lien de vérification copié.`, 'OK', { duration: 2500 }));
    }
  }

  print(): void {
    window.print();
  }
}
