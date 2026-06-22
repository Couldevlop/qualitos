import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { MarketplaceService } from '../../marketplace.service';
import { SubmitRequest } from '../../marketplace.types';

/**
 * Formulaire de soumission d'un pack par un partenaire. Le pack démarre en
 * SUBMITTED et entre dans la file de modération de l'éditeur (validation humaine
 * avant publication). L'acteur soumissionnaire est dérivé du JWT côté serveur.
 */
@Component({
  selector: 'qos-marketplace-submit',
  templateUrl: './submit.component.html',
  styleUrls: ['./submit.component.scss'],
  standalone: false
})
export class SubmitComponent {

  submitting = false;

  readonly currencies = ['EUR', 'USD', 'GBP', 'CHF', 'JPY'];

  readonly form = this.fb.group({
    packId: ['', [Validators.required, Validators.pattern('^[a-z][a-z0-9_-]{1,63}$')]],
    version: ['', [Validators.required, Validators.pattern('^\\d+\\.\\d+(\\.\\d+)?$')]],
    publisher: ['', [Validators.required, Validators.maxLength(120)]],
    title: ['', [Validators.required, Validators.maxLength(250)]],
    description: ['', [Validators.maxLength(4000)]],
    sector: ['', [Validators.required, Validators.maxLength(80)]],
    norms: ['', [Validators.maxLength(1000)]],
    priceCents: [0, [Validators.required, Validators.min(0)]],
    currency: ['EUR', [Validators.required]],
    manifestUrl: ['', [Validators.required, Validators.pattern('^(https://|oci://).+')]],
    manifestJson: ['', [Validators.required, Validators.maxLength(65536)]],
    signatureHash: ['', [Validators.required, Validators.minLength(16), Validators.maxLength(128)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: MarketplaceService,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  /** Découpe le champ normes (CSV/espaces) en liste de slugs. */
  private parseNorms(raw: string | null | undefined): string[] {
    if (!raw) { return []; }
    return raw.split(/[\s,]+/).map(s => s.trim()).filter(s => s.length > 0);
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: SubmitRequest = {
      packId: v.packId!,
      version: v.version!,
      publisher: v.publisher!,
      title: v.title!,
      description: v.description || undefined,
      sector: v.sector!,
      norms: this.parseNorms(v.norms),
      priceCents: Number(v.priceCents ?? 0),
      currency: v.currency!,
      manifestUrl: v.manifestUrl!,
      manifestJson: v.manifestJson!,
      signatureHash: v.signatureHash!
    };
    this.submitting = true;
    this.svc.submit(req).subscribe({
      next: () => {
        this.submitting = false;
        this.snack.open(
          $localize`:@@marketplace.submit.success:Pack soumis — en attente de validation de l'éditeur`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        this.router.navigate(['/marketplace']);
      },
      error: () => {
        this.submitting = false;
        this.snack.open(
          $localize`:@@marketplace.submit.error:Échec de la soumission (manifeste ou doublon)`,
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }
}
