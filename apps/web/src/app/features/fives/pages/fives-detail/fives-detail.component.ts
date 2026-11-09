import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FivesService } from '../../fives.service';
import {
  FiveSAuditResponse,
  FiveSAuditStatus,
  FiveSItemPhoto,
  FiveSPillar,
  VisionAnalysis
} from '../../fives.types';
import {
  FivesCircleChoice,
  FivesCircleDialogComponent,
  FivesCircleDialogData
} from '../fives-circle-dialog/fives-circle-dialog.component';
import {
  FivesEditDialogComponent,
  FivesEditDialogData
} from '../fives-edit-dialog/fives-edit-dialog.component';

// OWASP A03 — refuse malformed UUIDs client-side. Demo mock ids ("5s-1"…)
// are also accepted so the page stays usable with useMockApi=true.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface PillarRow {
  pillar: FiveSPillar;
  label: string;
  hint: string;
}

@Component({
  selector: 'qos-fives-detail',
  templateUrl: './fives-detail.component.html',
  styleUrls: ['./fives-detail.component.scss'],
  standalone: false
})
export class FivesDetailComponent implements OnInit {

  audit$!: Observable<FiveSAuditResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  readonly pillars: PillarRow[] = [
    { pillar: 'SEIRI',    label: $localize`:@@fives.pillar.seiri.label:Seiri`,    hint: $localize`:@@fives.pillar.seiri.hint:Trier — éliminer l'inutile` },
    { pillar: 'SEITON',   label: $localize`:@@fives.pillar.seiton.label:Seiton`,   hint: $localize`:@@fives.pillar.seiton.hint:Ranger — une place pour chaque chose` },
    { pillar: 'SEISO',    label: $localize`:@@fives.pillar.seiso.label:Seiso`,    hint: $localize`:@@fives.pillar.seiso.hint:Nettoyer — détecter les anomalies en nettoyant` },
    { pillar: 'SEIKETSU', label: $localize`:@@fives.pillar.seiketsu.label:Seiketsu`, hint: $localize`:@@fives.pillar.seiketsu.hint:Standardiser — figer les pratiques` },
    { pillar: 'SHITSUKE', label: $localize`:@@fives.pillar.shitsuke.label:Shitsuke`, hint: $localize`:@@fives.pillar.shitsuke.hint:Maintenir — discipliner & auditer` }
  ];

  /** Per-pillar score form (one FormGroup per pillar). */
  forms: Record<FiveSPillar, FormGroup> = {} as Record<FiveSPillar, FormGroup>;
  saving: Record<FiveSPillar, boolean> = {} as Record<FiveSPillar, boolean>;
  /** Le dépôt d'une idée en cours, par pilier. */
  proposing: Record<FiveSPillar, boolean> = {} as Record<FiveSPillar, boolean>;

  /**
   * Les photos de terrain de l'audit, tous piliers confondus (§3.2).
   *
   * <p>Une seule liste, découpée par pilier à l'affichage : cinq requêtes pour
   * cinq vignettes feraient cinq allers et retours sur un réseau d'atelier.
   */
  photos: FiveSItemPhoto[] = [];
  /** La prise de vue en cours, par pilier. */
  photographing: Record<FiveSPillar, boolean> = {} as Record<FiveSPillar, boolean>;

  /** Analyse Vision CV (§3.2) : état + dernier résultat. */
  readonly visionAnalyzing$ = new BehaviorSubject<boolean>(false);
  visionResult: VisionAnalysis | null = null;
  visionError: string | null = null;

  private auditId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Ancré des DEUX côtés et jeu de caractères restreint : un simple préfixe
  // laissait passer « 5s-1/../../secrets » (traversée de chemin — OWASP A03).
  private isMockId(s: string): boolean { return /^5s-[a-z0-9-]{1,64}$/i.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fives: FivesService,
    private readonly snack: MatSnackBar,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog
  ) {
    for (const p of this.pillars) {
      this.forms[p.pillar] = this.fb.nonNullable.group({
        score: [0, [Validators.required, Validators.min(0), Validators.max(10)]],
        note: ['']
      });
      this.saving[p.pillar] = false;
      this.proposing[p.pillar] = false;
      this.photographing[p.pillar] = false;
    }
  }

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/fives']);
      return;
    }
    this.auditId = raw;
    this.audit$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.fives.getAudit(this.auditId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] getAudit failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@fives.detail.not-found:Audit introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      tap(a => {
        if (a) {
          // En microtâche : charger les vignettes depuis le tap du flux qui rend
          // la page ferait muter la vue pendant son propre cycle de contrôle.
          queueMicrotask(() => this.loadPhotos());
          for (const p of this.pillars) {
            const item = a.items.find(i => i.pillar === p.pillar);
            if (item) {
              this.forms[p.pillar].setValue({
                score: item.score,
                note: item.note ?? ''
              });
            }
          }
        }
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
  }

  goBack(): void {
    this.router.navigate(['/fives']);
  }

  openEdit(a: FiveSAuditResponse): void {
    const data: FivesEditDialogData = { audit: a };
    this.dialog
      .open(FivesEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deleteAudit(zone: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@fives.detail.delete-confirm-title:Supprimer cet audit ?`,
        message: `L'audit de « ${zone} » et ses scores seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.fives.deleteAudit(this.auditId).subscribe({
        next: () => {
          this.snack.open($localize`:@@fives.detail.delete-success:Audit supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/fives']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@fives.detail.delete-error:Erreur lors de la suppression.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
    });
  }

  score(pillar: FiveSPillar): void {
    if (this.saving[pillar]) return;
    const form = this.forms[pillar];
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.saving[pillar] = true;
    const { score, note } = form.getRawValue();
    this.fives
      .scorePillar(this.auditId, {
        pillar,
        score,
        note: note?.trim() || undefined
      })
      .pipe(finalize(() => (this.saving[pillar] = false)))
      .subscribe({
        next: () => {
          this.snack.open(`${pillar} enregistré.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] score failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@fives.detail.score-error:Erreur lors de l'enregistrement.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  /**
   * Porter un pilier de cet audit à un cercle de qualité (§3.6).
   *
   * <p>Un audit dit ce qui ne va pas ; le cercle cherche comment y remédier.
   * Le geste n'est offert que sur un audit terminé et un pilier noté : le
   * serveur refuse le reste, et une commande qui échoue après coup vaut moins
   * qu'une commande absente.
   *
   * <p>L'audit n'enregistre rien en retour : c'est l'origine citée par l'idée
   * qui tient le lien. Un audit qui porterait la liste de ses idées devrait la
   * tenir à jour quand elles sont rejetées, et il n'en sait rien.
   */
  proposeToCircle(audit: FiveSAuditResponse, row: PillarRow): void {
    const item = audit.items.find(i => i.pillar === row.pillar);
    if (!this.canPropose(audit, row.pillar) || !item || item.score == null) return;
    const data: FivesCircleDialogData = {
      zone: audit.zone,
      pillar: row.pillar,
      pillarLabel: row.label,
      score: item.score,
      note: item.note
    };
    this.dialog.open<FivesCircleDialogComponent, FivesCircleDialogData, FivesCircleChoice>(
      FivesCircleDialogComponent,
      { data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel' }
    ).afterClosed().subscribe(choix => {
      if (!choix) return;
      this.proposing[row.pillar] = true;
      this.fives.proposeToCircle(audit.id, choix)
        .pipe(finalize(() => (this.proposing[row.pillar] = false)))
        .subscribe({
          next: () => this.snack.open(
            $localize`:@@fives.circle.done:Idée déposée au cercle. Elle y arrive avec la note relevée.`,
            $localize`:@@common.ok:OK`, { duration: 5000 }),
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[fives-detail] circle proposal failed', err?.status, err?.error?.title);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@fives.circle.failed:L'idée n'a pas pu être déposée.`),
              $localize`:@@common.ok:OK`, { duration: 5000 });
          }
        });
    });
  }

  /** Un audit terminé, un pilier noté : sans quoi l'idée n'a pas de chiffre à montrer. */
  canPropose(audit: FiveSAuditResponse, pillar: FiveSPillar): boolean {
    if (audit.status !== 'COMPLETED') return false;
    const item = audit.items.find(i => i.pillar === pillar);
    return !!item && item.score != null;
  }

  // ---- photos de terrain (§3.2, §15.3) ---------------------------------------

  /** Les types qu'un téléphone produit, et que le serveur accepte. */
  private static readonly PHOTO_TYPES =
    ['image/jpeg', 'image/png', 'image/webp', 'image/heic'];

  /** Même plafond que le serveur : le dire tout de suite évite un aller-retour. */
  private static readonly PHOTO_MAX_BYTES = 10 * 1024 * 1024;

  /** Au plus cinq photos par pilier, comme le serveur. */
  private static readonly PHOTO_MAX_PER_PILLAR = 5;

  /** Les vignettes d'un pilier, dans l'ordre de prise. */
  photosOf(pillar: FiveSPillar): FiveSItemPhoto[] {
    return this.photos.filter(p => p.pillar === pillar);
  }

  /**
   * Photographier suppose un pilier noté : la photo documente un constat, et
   * un pilier qu'on n'a pas évalué n'en porte aucun. Un audit annulé n'accepte
   * plus rien ; un audit clos, si — l'auditeur a pu photographier en zone
   * blanche et la file ne se vide qu'au retour du réseau.
   */
  canPhotograph(audit: FiveSAuditResponse, pillar: FiveSPillar): boolean {
    if (audit.status === 'CANCELLED') return false;
    const item = audit.items.find(i => i.pillar === pillar);
    if (!item || item.score == null) return false;
    return this.photosOf(pillar).length < FivesDetailComponent.PHOTO_MAX_PER_PILLAR;
  }

  /** Retirer une preuve s'arrête à la clôture : un dossier qu'on vide ne prouve plus rien. */
  canRemovePhoto(audit: FiveSAuditResponse): boolean {
    return audit.status !== 'COMPLETED' && audit.status !== 'CANCELLED';
  }

  /** Ouvre l'appareil photo (ou le sélecteur de fichier sur un poste fixe). */
  triggerPhotoPicker(input: HTMLInputElement, pillar: FiveSPillar): void {
    if (this.photographing[pillar]) return;
    input.click();
  }

  onPhotoFile(event: Event, audit: FiveSAuditResponse, pillar: FiveSPillar): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // permet de reprendre une photo du même nom
    if (!file) return;
    if (!FivesDetailComponent.PHOTO_TYPES.includes((file.type || '').toLowerCase())) {
      this.snack.open(
        $localize`:@@fives.photo.err-type:Seules les photos (JPEG, PNG, WebP, HEIC) sont acceptées.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    if (file.size > FivesDetailComponent.PHOTO_MAX_BYTES) {
      this.snack.open(
        $localize`:@@fives.photo.err-too-large:Photo trop volumineuse (10 Mo maximum).`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.uploadPhoto(audit, pillar, file);
  }

  private uploadPhoto(audit: FiveSAuditResponse, pillar: FiveSPillar, file: File): void {
    this.photographing[pillar] = true;
    this.fives.uploadItemPhoto(audit.id, pillar, file)
      .pipe(finalize(() => (this.photographing[pillar] = false)))
      .subscribe({
        next: photo => {
          // La photo prise hors ligne n'existe que localement : on l'ajoute à la
          // liste au lieu de relire le serveur, qui ne la connaît pas encore.
          if (photo.pendingSync) {
            this.photos = [...this.photos, photo];
            this.snack.open(
              $localize`:@@fives.photo.queued:Photo gardée sur l'appareil. Elle partira au retour du réseau.`,
              $localize`:@@common.ok:OK`, { duration: 5000 });
            return;
          }
          this.loadPhotos();
          this.snack.open($localize`:@@fives.photo.done:Photo ajoutée au pilier.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] photo upload failed', err?.status, err?.error?.title);
          this.snack.open(this.photoErrorMessage(err?.status),
            $localize`:@@common.ok:OK`, { duration: 5000 });
        }
      });
  }

  removePhoto(audit: FiveSAuditResponse, photo: FiveSItemPhoto): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@fives.photo.remove-title:Retirer cette photo ?`,
        message: $localize`:@@fives.photo.remove-message:La photo disparaîtra de l'audit. Le retrait est consigné au journal.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      // Une photo encore en file n'existe pas côté serveur : la demander
      // reviendrait à faire échouer un retrait sur une ressource absente.
      if (photo.pendingSync) {
        this.photos = this.photos.filter(p => p.id !== photo.id);
        return;
      }
      this.fives.deleteItemPhoto(audit.id, photo.id).subscribe({
        next: () => this.loadPhotos(),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] photo delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@fives.photo.remove-error:La photo n'a pas pu être retirée.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
    });
  }

  /**
   * Les vignettes du serveur, relues à chaque changement.
   *
   * <p>Les URL sont présignées pour un quart d'heure : les garder plus
   * longtemps en mémoire afficherait des images mortes. Les photos encore en
   * file, elles, sont conservées : le serveur ne les connaît pas.
   */
  private loadPhotos(): void {
    const enAttente = this.photos.filter(p => p.pendingSync);
    this.fives.listPhotos(this.auditId).subscribe({
      next: photos => { this.photos = [...photos, ...enAttente]; },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[fives-detail] photo list failed', err?.status, err?.error?.title);
        // On garde ce qui est en file : une liste vide laisserait croire que la
        // photo prise à l'instant a été perdue.
        this.photos = enAttente;
      }
    });
  }

  private photoErrorMessage(status?: number): string {
    switch (status) {
      case 409: return $localize`:@@fives.photo.err-state:Ce pilier doit d'abord être noté, et l'audit ne doit pas être annulé.`;
      case 413: return $localize`:@@fives.photo.err-quota:Plafond atteint : 10 Mo par photo, 5 photos par pilier, 40 Mo par audit.`;
      case 422: return $localize`:@@fives.photo.err-type:Seules les photos (JPEG, PNG, WebP, HEIC) sont acceptées.`;
      case 503: return $localize`:@@fives.photo.err-storage:Le stockage des photos est momentanément indisponible.`;
      default:  return $localize`:@@fives.photo.err-generic:La photo n'a pas pu être envoyée.`;
    }
  }

  start(): void { this.transition('start'); }
  complete(): void { this.transition('complete'); }
  cancel(): void { this.transition('cancel'); }

  private transition(action: 'start' | 'complete' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.fives.startAudit(this.auditId)
      : action === 'complete' ? this.fives.completeAudit(this.auditId)
      : this.fives.cancelAudit(this.auditId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const label = action === 'start' ? 'démarré' : action === 'complete' ? 'clôturé' : 'annulé';
        this.snack.open(`Audit ${label}.`, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[fives-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@fives.detail.transition-error:Erreur lors de la transition.`),
          $localize`:@@common.ok:OK`, { duration: 4000 }
        );
      }
    });
  }

  // ---- Analyse Vision CV (YOLOv8) d'une photo de zone (§3.2) -----------------

  /** Ouvre le sélecteur de fichier (déclenché par le bouton « Analyser une photo »). */
  triggerVisionPicker(input: HTMLInputElement): void {
    if (this.visionAnalyzing$.value) return;
    input.click();
  }

  /** Sur sélection d'un fichier image : lance l'analyse vision. */
  onVisionFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // permet de re-sélectionner le même fichier
    if (file) this.analyzeVisionPhoto(file);
  }

  private analyzeVisionPhoto(file: File): void {
    this.visionError = null;
    this.visionAnalyzing$.next(true);
    this.fives.analyzeAuditPhoto(this.auditId, file)
      .pipe(finalize(() => this.visionAnalyzing$.next(false)))
      .subscribe({
        next: result => { this.visionResult = result; },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] vision analyze failed', err?.status, err?.error?.title);
          this.visionResult = null;
          this.visionError = this.visionErrorMessage(err?.status);
        }
      });
  }

  /** Efface le dernier résultat de vision. */
  clearVision(): void {
    this.visionResult = null;
    this.visionError = null;
  }

  private visionErrorMessage(status?: number): string {
    switch (status) {
      case 400: return $localize`:@@fives.vision.err-invalid:Image invalide (format non supporté).`;
      case 413: return $localize`:@@fives.vision.err-too-large:Image trop volumineuse (10 Mo max).`;
      case 503: return $localize`:@@fives.vision.err-unavailable:Le service d'analyse vision est momentanément indisponible.`;
      default:  return $localize`:@@fives.vision.err-generic:Échec de l'analyse vision.`;
    }
  }

  /** Classe de badge selon la sévérité d'un finding. */
  findingSeverityClass(severity: string): string {
    return 'badge badge-' + (severity || '').toLowerCase();
  }

  isTerminal(status: FiveSAuditStatus): boolean {
    return status === 'COMPLETED' || status === 'CANCELLED';
  }

  canStart(status: FiveSAuditStatus): boolean { return status === 'DRAFT'; }
  canComplete(status: FiveSAuditStatus): boolean { return status === 'IN_PROGRESS'; }
  canScore(status: FiveSAuditStatus): boolean { return status === 'IN_PROGRESS' || status === 'DRAFT'; }

  badgeClass(status: FiveSAuditStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  scoreColor(score: number): string {
    if (score >= 8) return 'pillar-score pillar-score-high';
    if (score >= 5) return 'pillar-score pillar-score-mid';
    return 'pillar-score pillar-score-low';
  }

  /** Typed accessor for the score value of a given pillar (avoids TS4111 in the template). */
  scoreOf(pillar: FiveSPillar): number {
    const ctrl = this.forms[pillar].get('score');
    const v = ctrl?.value;
    return typeof v === 'number' ? v : 0;
  }
}
