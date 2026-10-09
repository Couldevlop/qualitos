import { formatDate } from '@angular/common';
import { Component, Inject, LOCALE_ID, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { EditionService } from '../../../core/edition/edition.service';
import { EditionView, WARN_DAYS } from '../../../core/edition/edition.types';

/** Ce que le bandeau dit, et sur quel ton. */
export interface LicenseNotice {
  tone: 'info' | 'warning' | 'blocked';
  message: string;
}

/**
 * Le bandeau de licence d'une installation on-premise (ADR 0082).
 *
 * <p>Il ne parle que s'il a quelque chose d'utile à dire : l'échéance qui
 * approche, le délai de grâce, ou la lecture seule et sa raison. En SaaS, et
 * pendant les onze premiers mois d'une licence, il se tait.
 */
@Component({
  selector: 'qos-license-banner',
  templateUrl: './license-banner.component.html',
  styleUrls: ['./license-banner.component.scss'],
  standalone: false
})
export class LicenseBannerComponent implements OnInit {

  notice$!: Observable<LicenseNotice | null>;

  constructor(private readonly edition: EditionService, @Inject(LOCALE_ID) private readonly locale: string) {}

  ngOnInit(): void {
    this.notice$ = this.edition.current().pipe(map(v => noticeFor(v, d => this.date(d))));
  }

  private date(iso: string | null): string {
    return iso ? formatDate(iso, 'longDate', this.locale) : '';
  }
}

export function noticeFor(v: EditionView, date: (iso: string | null) => string): LicenseNotice | null {
  if (v.edition !== 'ONPREM') {
    return null;
  }
  const lectureSeule = $localize`:@@license.banner.read-only-tail:Vos données restent consultables et exportables.`;
  switch (v.licenseStatus) {
    case 'VALID':
      return v.daysLeft !== null && v.daysLeft <= WARN_DAYS
        ? {
          tone: 'info',
          message: $localize`:@@license.banner.expires-soon:La licence expire le ${date(v.expiresAt)}:date: : pensez à la renouveler.`
        }
        : null;
    case 'GRACE':
      return {
        tone: 'warning',
        message: $localize`:@@license.banner.grace:La licence a expiré le ${date(v.expiresAt)}:date:. Tout fonctionne jusqu'au ${date(v.graceEndsAt)}:grace: ; renouvelez-la avant cette date.`
      };
    case 'EXPIRED':
      return {
        tone: 'blocked',
        message: $localize`:@@license.banner.expired:Licence expirée : l'installation est en lecture seule.` + ' ' + lectureSeule
      };
    case 'NOT_YET_VALID':
      return {
        tone: 'blocked',
        message: $localize`:@@license.banner.not-yet:La licence n'est pas encore en vigueur : l'installation est en lecture seule.`
      };
    case 'MISSING':
      return {
        tone: 'blocked',
        message: $localize`:@@license.banner.missing:Aucune licence installée : l'installation est en lecture seule.` + ' ' + lectureSeule
      };
    case 'INVALID':
      return {
        tone: 'blocked',
        message: $localize`:@@license.banner.invalid:La licence n'a pas pu être vérifiée : l'installation est en lecture seule.` + ' ' + lectureSeule
      };
    default:
      return null;
  }
}
