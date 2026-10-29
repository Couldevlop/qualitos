import { Component, Inject, OnInit, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { from, Observable, of } from 'rxjs';
import { catchError, concatMap, finalize, map, switchMap, tap, toArray } from 'rxjs/operators';

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

  /**
   * Photos choisies AVANT que la NC existe : elles ne partent qu'après la
   * création, le serveur classant une photo sous une NC identifiée.
   */
  photos: File[] = [];

  /** Photos déjà déposées, pour dire où en est l'envoi. */
  uploaded = 0;

  /** Mêmes limites que le serveur (NcPhotoService) : 10 Mo, JPEG/PNG/WebP/HEIC. */
  readonly maxPhotoBytes = 10 * 1024 * 1024;
  readonly maxPhotos = 10;
  readonly acceptedPhotoTypes = 'image/jpeg,image/png,image/webp,image/heic';

  /**
   * Catégorie et sévérité partent vides : une valeur pré-remplie (« Majeure »)
   * était validée sans être lue, et faussait la criticité des NC déclarées.
   */
  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    category: [null as NcCategory | null, [Validators.required]],
    severity: [null as NcSeverity | null, [Validators.required]],
    zone: ['', [Validators.maxLength(255)]],
    // Obligatoire : un titre seul ne dit ni ce qui a été constaté, ni où, ni
    // ce que ça a produit — l'analyse de cause racine partirait de rien.
    description: ['', [Validators.required]],
    geoLat: [null as number | null],
    geoLng: [null as number | null],
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

  /**
   * Ce qui empêche de déclarer, nommé plutôt que laissé à deviner.
   *
   * <p>Le formulaire a quatre champs obligatoires sans valeur par défaut :
   * on peut donc dire LEQUEL manque, ce qui vaut mieux qu'un « des champs
   * obligatoires restent à renseigner » que l'utilisateur devrait traduire en
   * parcourant l'écran. Les erreurs des champs jamais touchés ne s'affichant
   * pas, c'est souvent la seule indication à l'écran.
   */
  get blockedReason(): string | undefined {
    if (this.form.controls.title.invalid) {
      return $localize`:@@nc.create.blocked-title:Renseignez le titre pour pouvoir déclarer.`;
    }
    if (this.form.controls.description.invalid) {
      return $localize`:@@nc.create.blocked-description:Renseignez la description pour pouvoir déclarer.`;
    }
    if (this.form.controls.category.invalid) {
      return $localize`:@@nc.create.blocked-category:Choisissez la catégorie pour pouvoir déclarer.`;
    }
    if (this.form.controls.severity.invalid) {
      return $localize`:@@nc.create.blocked-severity:Choisissez la sévérité pour pouvoir déclarer.`;
    }
    return undefined;   // repli générique du gabarit
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
      title, category, severity, zone, description, geoLat, geoLng,
      productId, failureMode
    } = this.form.getRawValue();
    this.nc
      .createNc({
        title: title.trim(),
        // Non nuls ici : le formulaire est valide (required).
        category: category as NcCategory,
        severity: severity as NcSeverity,
        zone: zone?.trim() || undefined,
        description: description.trim(),
        origin: this.origin,
        geoLat: geoLat ?? undefined,
        geoLng: geoLng ?? undefined,
        detectedAt: new Date().toISOString(),
        reporterId: reporterId || undefined,
        productId: productId || undefined,
        fmeaItemId: failureMode && failureMode !== NcCreateDialogComponent.NO_FAILURE_MODE
          ? failureMode
          : undefined
      })
      .pipe(
        // La NC d'abord, ses photos ensuite : l'inverse n'existe pas.
        switchMap(created => this.uploadPhotos(created).pipe(map(() => created))),
        finalize(() => (this.submitting = false))
      )
      .subscribe({
        next: created => {
          this.snack.open(this.doneMessage(created), $localize`:@@common.ok:OK`, { duration: 4000 });
          this.dialogRef.close(created);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
            $localize`:@@common.ok:OK`,
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Ajoute les photos prises ou choisies, en refusant tout de suite ce que le
   * serveur refuserait : une photo rejetée après la création serait perdue.
   */
  onPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const picked = Array.from(input.files ?? []);
    // Réinitialise pour autoriser la re-sélection de la même photo.
    input.value = '';

    for (const file of picked) {
      if (this.photos.length >= this.maxPhotos) {
        this.snack.open(
          $localize`:@@nc.create.photo-limit:Dix photos au maximum par déclaration.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        return;
      }
      if (!this.acceptedPhotoTypes.split(',').includes(file.type)) {
        this.snack.open(
          $localize`:@@nc.create.photo-type:Format non accepté — JPEG, PNG, WebP ou HEIC.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        continue;
      }
      if (file.size > this.maxPhotoBytes) {
        this.snack.open(
          $localize`:@@nc.create.photo-too-large:Photo trop lourde — 10 Mo au maximum.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
        continue;
      }
      this.photos = [...this.photos, file];
    }
  }

  removePhoto(file: File): void {
    this.photos = this.photos.filter(f => f !== file);
  }

  removePhotoAria(name: string): string {
    return $localize`:@@nc.create.photo-remove-aria:Retirer ${name}:name:`;
  }

  /** Taille lisible, en Ko ou Mo selon l'ordre de grandeur. */
  formatSize(bytes: number): string {
    return bytes >= 1024 * 1024
      ? `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
      : `${Math.max(1, Math.round(bytes / 1024))} Ko`;
  }

  /**
   * Dépose les photos une à une sur la NC créée.
   *
   * <p>Une NC mise en file hors-ligne n'a pas encore d'identifiant serveur et
   * un fichier ne se met pas en file : ses photos ne partent pas, et le message
   * de fin le dit. Un échec d'envoi ne détruit pas la NC, déjà déclarée.
   */
  private uploadPhotos(created: NcResponse): Observable<unknown> {
    if (this.photos.length === 0 || created.pendingSync) {
      return of(null);
    }
    this.uploaded = 0;
    return from(this.photos).pipe(
      concatMap(file =>
        this.nc.uploadPhoto(created.id, file).pipe(
          tap(() => this.uploaded++),
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-create] uploadPhoto failed', err?.status, err?.error?.type);
            return of(null);
          })
        )
      ),
      toArray()
    );
  }

  /** Le message dit ce qui est réellement joint, pas ce qui était prévu. */
  private doneMessage(created: NcResponse): string {
    const total = this.photos.length;
    if (created.pendingSync) {
      return total === 0
        ? $localize`:@@nc.create.success-offline:NC enregistrée hors-ligne — elle sera synchronisée au retour du réseau.`
        : $localize`:@@nc.create.success-offline-photos:NC enregistrée hors-ligne — les photos n'ont pas pu être jointes : ajoutez-les depuis la fiche au retour du réseau.`;
    }
    if (total === 0) {
      return $localize`:@@nc.create.success:Non-conformité déclarée.`;
    }
    if (this.uploaded === total) {
      return $localize`:@@nc.create.success-with-photos:Non-conformité déclarée, photos jointes.`;
    }
    return $localize`:@@nc.create.success-partial-photos:Non-conformité déclarée, mais des photos n'ont pas pu être envoyées — ajoutez-les depuis la fiche.`;
  }
}
