import { Component, Inject, OnInit, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { catchError, finalize, map, switchMap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService } from '../../../fmea/fmea.service';
import { FmeaItemResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../../products/products.service';
import { FailureModeSuggestion, ProductResponse } from '../../../products/products.types';
import { NcService } from '../../nc.service';
import { NcCategory, NcOrigin, NcResponse, NcSeverity } from '../../nc.types';

/**
 * Ce que la liste appelante transmet au dialogue.
 *
 * <p>`origin` porte l'origine de la LISTE d'où l'on déclare. Sans elle, le
 * dialogue n'envoyait aucune origine et le serveur retombait sur INTERNAL :
 * une non-conformité déclarée depuis l'écran « externes » atterrissait dans le
 * tableau des internes, et n'était plus jamais visible là où on l'avait créée.
 */
export interface NcCreateDialogData {
  origin?: NcOrigin;
}

/** Une suggestion, enrichie de l'intitulé du mode de défaillance qu'elle désigne. */
export interface FailureModeChoice {
  fmeaItemId: string;
  label: string;
  matchedTerms: string;
}

@Component({
  selector: 'qos-nc-create-dialog',
  templateUrl: './nc-create-dialog.component.html',
  styleUrls: ['./nc-create-dialog.component.scss'],
  standalone: false
})
export class NcCreateDialogComponent implements OnInit {

  /**
   * Valeur du choix « aucun mode ne correspond ». Une valeur explicite, et non
   * l'absence de valeur : c'est elle qui déclenchera la proposition de créer une
   * ligne de PFMEA, alors qu'un champ laissé vide ne dit rien du tout.
   */
  static readonly NO_FAILURE_MODE = 'NONE';
  readonly noFailureMode = NcCreateDialogComponent.NO_FAILURE_MODE;

  submitting = false;
  locating = false;
  suggesting = false;

  /**
   * Origine de la liste d'où la déclaration part, transmise au serveur telle
   * quelle. `undefined` sur l'entrée `/nc` : le serveur décide alors.
   */
  readonly origin?: NcOrigin;

  products: ProductResponse[] = [];
  suggestions: FailureModeChoice[] = [];

  readonly categories: NcCategory[] = ['PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER'];
  readonly severities: NcSeverity[] = ['MINOR', 'MAJOR', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    category: ['PROCESS' as NcCategory, [Validators.required]],
    severity: ['MAJOR' as NcSeverity, [Validators.required]],
    zone: ['', [Validators.maxLength(255)]],
    // Obligatoire : un titre seul ne dit ni ce qui a été constaté, ni où, ni
    // ce que ça a produit — l'analyse de cause racine partirait de rien.
    description: ['', [Validators.required]],
    geoLat: [null as number | null],
    geoLng: [null as number | null],
    photoUrls: [''],
    productId: [''],
    /** '' = pas encore répondu, 'NONE' = aucun mode, sinon l'identifiant de la ligne. */
    failureMode: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly productsService: ProductsService,
    private readonly fmea: FmeaService,
    private readonly dialogRef: MatDialogRef<NcCreateDialogComponent, NcResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) data?: NcCreateDialogData
  ) {
    // `@Optional()` : le dialogue reste ouvrable sans donnée (entrée `/nc`,
    // qui montre les deux origines). L'origine est alors laissée au serveur,
    // qui applique son défaut INTERNAL.
    this.origin = data?.origin;
  }

  ngOnInit(): void {
    // Le référentiel produit est une aide à la saisie : son indisponibilité ne
    // doit pas empêcher de déclarer un défaut constaté au poste.
    this.productsService.list().pipe(catchError(() => of([] as ProductResponse[])))
      .subscribe(products => (this.products = products));
  }

  /**
   * Le sélecteur de mode de défaillance n'a de sens qu'une fois le produit
   * choisi : sans produit, la question ne se pose pas.
   */
  get productChosen(): boolean {
    return !!this.form.getRawValue().productId;
  }

  /** Recharge les suggestions depuis ce qui est déjà saisi du défaut. */
  refreshSuggestions(): void {
    const { productId, title, description } = this.form.getRawValue();
    this.form.patchValue({ failureMode: '' });
    this.suggestions = [];
    const text = `${title ?? ''} ${description ?? ''}`.trim();
    if (!productId || !text) return;

    this.suggesting = true;
    this.productsService.failureModeSuggestions(productId, text).pipe(
      switchMap(found => found.length === 0
        ? of([] as FailureModeChoice[])
        : this.fmea.list(0, 50, 'ACTIVE', 'PROCESS_FMEA', productId).pipe(
            switchMap(page => page.content.length > 0
              ? this.fmea.listItems(page.content[0].id).pipe(
                  map(items => this.decorate(found, items.content)))
              : of([] as FailureModeChoice[])))),
      catchError(() => of([] as FailureModeChoice[])),
      finalize(() => (this.suggesting = false))
    ).subscribe(choices => (this.suggestions = choices));
  }

  /**
   * L'intitulé du mode de défaillance, à côté des termes qui ont motivé la
   * suggestion : une suggestion sans raison n'est pas contestable, donc pas
   * confirmable en conscience.
   */
  private decorate(found: FailureModeSuggestion[], items: FmeaItemResponse[]): FailureModeChoice[] {
    return found.map(suggestion => {
      const item = items.find(candidate => candidate.id === suggestion.fmeaItemId);
      return {
        fmeaItemId: suggestion.fmeaItemId,
        label: item?.failureMode ?? suggestion.fmeaItemId,
        matchedTerms: suggestion.matchedTerms
      };
    });
  }

  /** Terrain : récupère la position GPS et la pose dans le formulaire (affichée). */
  useMyLocation(): void {
    if (this.locating) return;
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.snack.open(
        $localize`:@@nc.create.geo-unsupported:La géolocalisation n'est pas disponible sur cet appareil.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.locating = true;
    navigator.geolocation.getCurrentPosition(
      pos => {
        this.form.patchValue({
          geoLat: Math.round(pos.coords.latitude * 1e6) / 1e6,
          geoLng: Math.round(pos.coords.longitude * 1e6) / 1e6
        });
        this.locating = false;
      },
      () => {
        this.locating = false;
        this.snack.open(
          $localize`:@@nc.create.geo-error:Impossible de récupérer la position.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  clearLocation(): void {
    this.form.patchValue({ geoLat: null, geoLng: null });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    // reporterId facultatif : la session courante si disponible (terrain non bloquant).
    const reporterId = this.auth.snapshot()?.userId;
    this.submitting = true;
    const {
      title, category, severity, zone, description, geoLat, geoLng, photoUrls,
      productId, failureMode
    } = this.form.getRawValue();
    const cleanedPhotos = this.cleanPhotoUrls(photoUrls);
    this.nc
      .createNc({
        title: title.trim(),
        category,
        severity,
        zone: zone?.trim() || undefined,
        description: description.trim(),
        origin: this.origin,
        geoLat: geoLat ?? undefined,
        geoLng: geoLng ?? undefined,
        photoUrls: cleanedPhotos || undefined,
        detectedAt: new Date().toISOString(),
        reporterId: reporterId || undefined,
        productId: productId || undefined,
        fmeaItemId: failureMode && failureMode !== NcCreateDialogComponent.NO_FAILURE_MODE
          ? failureMode
          : undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: created => {
          const msg = created.pendingSync
            ? $localize`:@@nc.create.success-offline:NC enregistrée hors-ligne — elle sera synchronisée au retour du réseau.`
            : $localize`:@@nc.create.success:Non-conformité déclarée.`;
          this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(created);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
            'OK',
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /** Normalise le textarea : une URL par ligne, espaces et lignes vides retirés. */
  private cleanPhotoUrls(raw: string): string {
    return raw
      .split(/\r?\n/)
      .map(s => s.trim())
      .filter(Boolean)
      .join('\n');
  }
}
