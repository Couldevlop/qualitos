import {
  AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren
} from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  CapaActionsDialogComponent, CapaActionsDialogData
} from '../../../capa/pages/capa-actions-dialog/capa-actions-dialog.component';
import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../../../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { CapaActionType, CapaCriticity } from '../../../capa/capa.types';
import { CapaService } from '../../../capa/capa.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';
import { NcService } from '../../nc.service';
import { TeamMemberRef } from '../../../../shared/user-picker/user-team-picker.component';
import {
  NC_MAX_TEAM_MEMBERS,
  NcPhoto,
  NcBlocker,
  NcResponse,
  NcSeverity,
  NcStatus,
  NcSubcategory,
  VisionAnalysis,
  VisionScore,
  NcCapaCaseRef,
} from '../../nc.types';
import {
  NcConvertChoice,
  NcConvertDialogComponent,
  NcConvertDialogData
} from '../nc-convert-dialog/nc-convert-dialog.component';
import {
  NcRejectDialogComponent,
  NcRejectDialogData
} from '../nc-reject-dialog/nc-reject-dialog.component';
import {
  NcReopenDialogComponent,
  NcReopenDialogData
} from '../nc-reopen-dialog/nc-reopen-dialog.component';
import {
  NcResolveDialogComponent,
  NcResolveDialogData
} from '../nc-resolve-dialog/nc-resolve-dialog.component';

/**
 * Les huit disciplines, par leur code. Un type ferme plutot qu une chaine :
 * le sommaire, les ancres du document et les decomptes se designent par le
 * meme vocabulaire, et une faute de frappe ne passe pas.
 */
export type EtapeCode = 'D1' | 'D2' | 'D3' | 'D4' | 'D5' | 'D6' | 'D7' | 'D8';

// OWASP A03 — refuse malformed UUIDs client-side. Demo mock ids ("nc-1"…)
// are also accepted so the page stays usable with useMockApi=true.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-nc-detail',
  templateUrl: './nc-detail.component.html',
  styleUrls: ['./nc-detail.component.scss'],
  standalone: false
})
export class NcDetailComponent implements OnInit, AfterViewInit, OnDestroy {

  nc$!: Observable<NcResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  // --- photos (upload binaire, online-only) ---------------------------------
  photos$ = new BehaviorSubject<NcPhoto[]>([]);
  photosLoading$ = new BehaviorSubject<boolean>(false);
  uploading$ = new BehaviorSubject<boolean>(false);
  /** Bascule à true quand le backend renvoie 503 type 'storage-disabled'. */
  storageDisabled$ = new BehaviorSubject<boolean>(false);
  /** id de la photo en cours de suppression (désactive sa vignette). */
  deletingId$ = new BehaviorSubject<string | null>(null);
  /** État réseau pour désactiver le bouton d'ajout hors-ligne. */
  online$ = this.connectivity.online$;
  /** Tooltip du bouton d'ajout quand hors-ligne (interpolation non marquable i18n). */
  readonly photosOfflineTooltip = $localize`:@@nc.photos.offline-tooltip:Photos disponibles en ligne uniquement`;

  // --- analyse Vision 5S par IA (online-only) -------------------------------
  /** Résultat de la dernière analyse vision (null tant qu'aucune n'a tourné). */
  visionResult$ = new BehaviorSubject<VisionAnalysis | null>(null);
  /** true pendant l'inférence (spinner). */
  visionAnalyzing$ = new BehaviorSubject<boolean>(false);
  /** Bascule à true sur 503 vision-unavailable → état UI doux dédié. */
  visionUnavailable$ = new BehaviorSubject<boolean>(false);
  /** Tooltip du bouton d'analyse quand hors-ligne. */
  readonly visionOfflineTooltip = $localize`:@@nc.vision.offline-tooltip:Analyse disponible en ligne uniquement`;
  /** Ordre d'affichage des 5 piliers (clé du score + label localisé). */
  readonly visionPillars: { key: keyof VisionScore; label: string }[] = [
    { key: 'seiri', label: $localize`:@@nc.vision.pillar.seiri:Seiri (Trier)` },
    { key: 'seiton', label: $localize`:@@nc.vision.pillar.seiton:Seiton (Ranger)` },
    { key: 'seiso', label: $localize`:@@nc.vision.pillar.seiso:Seiso (Nettoyer)` },
    { key: 'seiketsu', label: $localize`:@@nc.vision.pillar.seiketsu:Seiketsu (Standardiser)` },
    { key: 'shitsuke', label: $localize`:@@nc.vision.pillar.shitsuke:Shitsuke (Maintenir)` }
  ];

  // --- 5 Pourquoi (§3.5) ------------------------------------------------------
  /**
   * Analyses des 5 Pourquoi déjà ouvertes sur cette non-conformité. La méthode
   * PART d'un écart constaté : c'est ici qu'on la déroule, pas depuis un écran
   * qui ne saurait pas de quoi il parle.
   */
  fiveWhys$ = new BehaviorSubject<FiveWhysAnalysis[]>([]);
  /** true le temps d'ouvrir ou de créer l'analyse (évite le double clic). */
  fiveWhysBusy$ = new BehaviorSubject<boolean>(false);

  /**
   * Diagrammes d'Ishikawa déjà ouverts sur cette non-conformité. Même raison que
   * pour les 5 Pourquoi : la recherche de causes part de l'écart constaté, et
   * c'est d'ici qu'on la lance sans avoir à recopier l'énoncé du problème.
   */
  ishikawa$ = new BehaviorSubject<IshikawaDiagramResponse[]>([]);
  /** true le temps d'ouvrir ou de créer le diagramme (évite le double clic). */
  ishikawaBusy$ = new BehaviorSubject<boolean>(false);

  // ================= Les huit disciplines, en sommaire ======================

  /**
   * Les huit etapes du traitement, dans leur ordre.
   *
   * <p>L ecran suit la trame 8D de bout en bout : constituer l equipe, decrire,
   * endiguer, chercher la cause, corriger, prouver, prevenir, remercier. La
   * fiche presentait les memes elements en cartes sans ordre -- on y lisait
   * l equipe, les photos et les actions, sans voir ce qui restait a faire ni
   * dans quel ordre. Le sommaire repond a « ou en est-on ».
   *
   * <p>RIEN N Y EST OBLIGATOIRE : une etape vide n empeche pas la suivante. Le
   * marqueur dit ce qui est fait, il n interdit pas d avancer -- un ecart
   * mineur n a pas de confinement a decrire, et l exiger ferait inventer une
   * ligne pour passer.
   */
  readonly etapes: ReadonlyArray<{ code: EtapeCode; libelle: string }> = [
    { code: 'D1', libelle: $localize`:@@nc.8d.d1-short:Équipe` },
    { code: 'D2', libelle: $localize`:@@nc.8d.d2-short:Problème` },
    { code: 'D3', libelle: $localize`:@@nc.8d.d3-short:Confinement` },
    { code: 'D4', libelle: $localize`:@@nc.8d.d4-short:Causes` },
    { code: 'D5', libelle: $localize`:@@nc.8d.d5-short:Correctives` },
    { code: 'D6', libelle: $localize`:@@nc.8d.d6-short:Preuves` },
    { code: 'D7', libelle: $localize`:@@nc.8d.d7-short:Préventives` },
    { code: 'D8', libelle: $localize`:@@nc.8d.d8-short:Remerciements` }
  ];

  /**
   * Les natures d actions de chaque discipline, en tableaux FIGES.
   *
   * <p>Des constantes du composant et non des litteraux de gabarit : le panneau
   * d actions compare la reference de son entree pour savoir s il doit relire le
   * dossier, et un `[natures]="['CONTAINMENT']"` ecrit dans le gabarit
   * fabriquerait un tableau neuf a chaque passe de detection -- donc une requete
   * reseau par passe.
   */
  readonly naturesD3: readonly CapaActionType[] = ['CONTAINMENT'];
  readonly naturesD5: readonly CapaActionType[] = ['CORRECTIVE'];
  readonly naturesD7: readonly CapaActionType[] = ['PREVENTIVE'];


  /** L etape sous les yeux, suivie au defilement : le sommaire la souligne. */
  etapeActive: EtapeCode | null = null;

  @ViewChildren('etape') private sections?: QueryList<ElementRef<HTMLElement>>;
  private observateur?: IntersectionObserver;

  /**
   * Combien d actions chaque discipline d actions montre.
   *
   * <p>Rempli par les panneaux eux-memes (leur sortie `counted`) : la fiche sait
   * quels dossiers CAPA existent, pas ce qu ils portent. Sans ce retour, le
   * sommaire cochait « endiguer » des qu un dossier existait, meme s il ne
   * portait que des actions correctives.
   */
  private readonly comptesParEtape = new Map<EtapeCode, number>();

  /**
   * Le commentaire d'efficacité en cours de frappe, par dossier.
   *
   * <p>Il n'existe que tant qu'il diffère de l'enregistré : une entrée restée
   * là après un échec réseau est justement ce qui évite de réécrire la note.
   */
  private readonly brouillonsCommentaire = new Map<string, string>();

  /** Un panneau a fini de lire : le sommaire peut dire vrai pour sa discipline. */
  compter(code: EtapeCode, combien: number): void {
    this.comptesParEtape.set(code, combien);
  }

  /** Amene l etape sous les yeux. Le sommaire est une aide, pas un filtre. */
  allerA(code: EtapeCode): void {
    const cible = this.sections?.find(s => s.nativeElement.id === code);
    cible?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  /**
   * Cette etape porte-t-elle quelque chose ?
   *
   * <p>Un marqueur de LECTURE : il dit ce qui est renseigne, il ne conditionne
   * rien. Chaque reponse vient de ce que l ecran sait deja -- aucune requete
   * n est faite pour dessiner un sommaire.
   */
  etapeFaite(code: EtapeCode, n: NcResponse): boolean {
    switch (code) {
      case 'D1': return (n.teamMembers?.length ?? 0) > 0;
      case 'D2': return !!(n.description || n.customerName || n.batchNumber || n.productId);
      case 'D3': return (this.comptesParEtape.get('D3') ?? 0) > 0;
      case 'D4': return !!n.rootCause
        || this.ishikawa$.value.length > 0
        || this.fiveWhys$.value.length > 0;
      case 'D5': return (this.comptesParEtape.get('D5') ?? 0) > 0;
      // Les photos du CONSTAT appartiennent au D2 : elles décrivent le
      // problème et ne démontrent rien de l'efficacité des actions. Les
      // preuves du D6 se joignent action par action, et la note de
      // résolution porte le jugement d'ensemble.
      case 'D6': return !!n.resolutionNote;
      case 'D7': return (this.comptesParEtape.get('D7') ?? 0) > 0;
      case 'D8': return !!this.remerciementEnregistre;
    }
  }

  ngAfterViewInit(): void {
    // Hors navigateur (bancs, rendu serveur), le sommaire reste utilisable : il
    // perd seulement le soulignement de l etape sous les yeux.
    if (typeof IntersectionObserver === 'undefined' || !this.sections) {
      return;
    }
    this.observateur = new IntersectionObserver(entrees => {
      const vue = entrees.find(e => e.isIntersecting);
      if (vue) {
        this.etapeActive = vue.target.id as EtapeCode;
      }
    // La fenetre de visee est volontairement etroite et haute : sans elle, deux
    // sections se declarent visibles en meme temps et le soulignement saute.
    }, { rootMargin: '-30% 0px -60% 0px' });
    this.sections.forEach(s => this.observateur?.observe(s.nativeElement));
  }

  ngOnDestroy(): void {
    this.observateur?.disconnect();
  }

  // ===================== Le bandeau d etat de l ecart =======================

  /**
   * Ce qui est arrive a l ecart, en une phrase, par son fait le PLUS RECENT.
   *
   * <p>Ces dates etaient six lignes d une grille d informations, lues apres le
   * reste : on ne voyait pas, en ouvrant la fiche, qu un ecart clos avait ete
   * repris. L ordre est celui de l histoire a l envers -- une reprise passe
   * devant la cloture qu elle defait.
   */
  bandeau(n: NcResponse): string | null {
    if (n.reopenedAt) {
      return this.phrase(
        $localize`:@@nc.8d.banner-reopened:Écart repris le`, n.reopenedAt, n.reopenReason);
    }
    if (n.closedAt) {
      // Le bandeau NOMME qui a clos quand on le sait : c'est la moitié visible
      // du second regard, et la question que pose tout auditeur devant une
      // fiche close. Les écarts d'avant la règle n'ont pas ce nom.
      return this.phrase(
        $localize`:@@nc.8d.banner-closed:Clôturé le`, n.closedAt,
        n.closedByName
          ? $localize`:@@nc.8d.banner-closed-by:par ${n.closedByName}:name:`
          : null);
    }
    if (n.rejectedAt) {
      return this.phrase(
        $localize`:@@nc.8d.banner-rejected:Écarté le`, n.rejectedAt, n.rejectionReason);
    }
    if (n.resolvedAt) {
      return this.phrase(
        $localize`:@@nc.8d.banner-resolved:Résolu le`, n.resolvedAt, n.resolutionNote);
    }
    if (n.status === 'CANCELLED') {
      return $localize`:@@nc.8d.banner-cancelled:Écart annulé.`;
    }
    return null;
  }

  /** « Clôturé le 12/03/2026 : joint remplacé ». Le motif ne vient que s il existe. */
  private phrase(quoi: string, quand: string, motif: string | null | undefined): string {
    const date = new Date(quand).toLocaleDateString();
    const net = motif?.trim();
    return net ? `${quoi} ${date} : ${net}` : `${quoi} ${date}`;
  }

  // ======================== La reprise d un ecart ===========================

  /**
   * Un ecart REFERME se reprend : recidive constatee, client qui relance,
   * constat ecarte a tort.
   *
   * <p>Sans elle, il fallait ouvrir un SECOND ecart -- qui perdait l analyse de
   * cause, les actions deja menees et l equipe du premier, et comptait deux fois
   * le meme probleme. Le motif est demande, jamais exige : l ecart doit pouvoir
   * etre repris tout de suite, on ecrira pourquoi ensuite.
   */
  canReopen(status: NcStatus): boolean {
    return status === 'RESOLVED' || this.isTerminal(status);
  }

  reopen(n: NcResponse): void {
    if (this.acting$.value) return;
    this.dialog.open<NcReopenDialogComponent, NcReopenDialogData, NcResponse | undefined>(
      NcReopenDialogComponent, {
        data: { ncId: n.id, reference: n.reference, status: n.status },
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      }
    ).afterClosed().subscribe(reprise => {
      // La fenetre a fait l'appel, comme celle du rejet : l'ecart repris revient
      // ici, et la fiche se relit. Fermee sans confirmer, elle ne rend rien.
      if (reprise) this.reload$.next();
    });
  }

  // ============== Le D2 : ce que la fiche ne savait pas porter ==============

  /**
   * Le client qui reclame et le lot incrimine, saisis ICI.
   *
   * <p>Ils s ecrivaient dans la description, en texte libre, d ou ils ne
   * ressortaient ni en filtre, ni en indicateur, ni dans le rapport remis au
   * client : la question « quels ecarts sur le lot L2611-08 ? » n avait pas de
   * reponse. Facultatifs tous les deux -- un constat interne n a pas de client,
   * et un ecart de service n a pas de lot.
   */
  readonly contexteForm = new FormGroup({
    customerName: new FormControl<string>('', { nonNullable: true }),
    batchNumber: new FormControl<string>('', { nonNullable: true })
  });
  readonly contexteSaving$ = new BehaviorSubject<boolean>(false);
  /** Vrai des que le formulaire du D2 differe de la fiche : le bouton apparait. */
  contexteModifie = false;

  /**
   * Recopie la fiche dans le formulaire, sans la marquer modifiee.
   *
   * <p>Appele a chaque relecture. Le garde-fou `contexteModifie` evite d ecraser
   * une saisie en cours par une relecture declenchee ailleurs dans l ecran --
   * un panneau d actions qui s annonce, par exemple.
   */
  private rempliContexte(n: NcResponse): void {
    if (this.contexteModifie) return;
    this.contexteForm.setValue({
      customerName: n.customerName ?? '',
      batchNumber: n.batchNumber ?? ''
    }, { emitEvent: false });
  }

  enregistrerContexte(): void {
    if (this.contexteSaving$.value) return;
    const valeurs = this.contexteForm.getRawValue();
    this.contexteSaving$.next(true);
    // Les chaines VIDES partent telles quelles : c est ainsi qu on EFFACE un
    // nom de client saisi par erreur. Le serveur distingue « vide » (effacer)
    // de « absent » (inchange).
    this.svc.updateNc(this.ncId, {
      customerName: valeurs.customerName.trim(),
      batchNumber: valeurs.batchNumber.trim()
    }).pipe(finalize(() => this.contexteSaving$.next(false)))
      .subscribe({
        next: () => {
          this.contexteModifie = false;
          this.snack.open(
            $localize`:@@nc.8d.context-saved:Description enregistrée.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] context save failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err,
              $localize`:@@nc.8d.context-error:La description n'a pas pu être enregistrée.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  // ===================== Le D8 : remercier l equipe ========================

  /**
   * Le mot de la fin, range dans le rapport 8D (sa discipline D8).
   *
   * <p>Il n a pas de place sur l ecart : c est une donnee du RAPPORT, aux cotes
   * du D1 et du D3 saisis. On le tapait donc dans l ecran du rapport, apres
   * avoir quitte l ecart -- et on ne le tapait pas. Facultatif, comme tout ici.
   */
  readonly remerciementControl = new FormControl<string>('', { nonNullable: true });
  readonly remerciementSaving$ = new BehaviorSubject<boolean>(false);
  /** Ce qui est DEJA enregistre, pour que le sommaire coche sans se tromper. */
  remerciementEnregistre = '';
  remerciementModifie = false;

  /** Lit le rapport pour en tirer le seul D8 ; son echec ne casse pas l ecran. */
  private chargerRemerciement(): void {
    this.svc.getEightDReport(this.ncId).pipe(
      catchError(() => of(null))
    ).subscribe(rapport => {
      if (!rapport) return;
      this.remerciementEnregistre = rapport.recognition ?? '';
      if (!this.remerciementModifie) {
        this.remerciementControl.setValue(this.remerciementEnregistre, { emitEvent: false });
      }
    });
  }

  enregistrerRemerciement(): void {
    if (this.remerciementSaving$.value) return;
    const texte = this.remerciementControl.value.trim();
    this.remerciementSaving$.next(true);
    this.svc.saveEightDReport(this.ncId, { recognition: texte })
      .pipe(finalize(() => this.remerciementSaving$.next(false)))
      .subscribe({
        next: rapport => {
          this.remerciementEnregistre = rapport.recognition ?? '';
          this.remerciementModifie = false;
          this.snack.open(
            $localize`:@@nc.8d.d8-saved:Remerciements enregistrés dans le rapport 8D.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] D8 save failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err,
              $localize`:@@nc.8d.d8-error:Les remerciements n'ont pas pu être enregistrés.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  /**
   * Relit la fiche.
   *
   * <p>Public : le panneau d'action CAPA, qui fait avancer l'action sans quitter
   * l'écart, annonce ses changements — et le statut de la non-conformité peut en
   * dépendre.
   */
  reload(): void {
    this.reload$.next();
  }

  // --- l'équipe en charge (D1 du 8D) -----------------------------------------

  readonly maxTeamMembers = NC_MAX_TEAM_MEMBERS;
  /** Vrai quand l'équipe est en cours de remaniement : la liste devient un champ. */
  teamEditing = false;
  /**
   * L'équipe telle qu'elle est en train d'être remaniée.
   *
   * <p>Un brouillon, et non la liste de la fiche : abandonner le remaniement doit
   * rendre l'équipe d'avant, sans recharger la page.
   */
  readonly teamDraftControl = new FormControl<TeamMemberRef[]>([], { nonNullable: true });
  readonly teamSaving$ = new BehaviorSubject<boolean>(false);

  get teamDraft(): TeamMemberRef[] {
    return this.teamDraftControl.value;
  }

  private ncId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Ancré des DEUX côtés et jeu de caractères restreint : un simple préfixe
  // laissait passer « nc-1/../../secrets » (traversée de chemin — OWASP A03).
  private isMockId(s: string): boolean { return /^nc-[a-z0-9-]{1,64}$/i.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: NcService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly connectivity: ConnectivityService,
    private readonly fiveWhys: FiveWhysService,
    private readonly ishikawa: IshikawaService,
    private readonly capaService: CapaService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  /** Le catalogue des natures, pour traduire le code porté par la NC. */
  subcategories: NcSubcategory[] = [];

  /** Le libellé d'une nature ; son code si le client l'a retirée de son catalogue. */
  subcategoryLabel(code: string | undefined): string {
    if (!code) return '';
    return this.subcategories.find(s => s.code === code)?.label ?? code;
  }

  ngOnInit(): void {
    // Une aide à la lecture : son absence laisse voir le code, pas une page vide.
    this.svc.subcategories().pipe(catchError(() => of({ entries: [] as NcSubcategory[], defaults: true })))
      .subscribe(c => (this.subcategories = c.entries));
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/nc']);
      return;
    }
    this.ncId = raw;
    this.nc$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.getNc(this.ncId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] getNc failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@nc.detail.not-found:Non-conformité introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // Recopie la fiche dans les champs saisissables du D2 a chaque relecture.
      // DANS le flux et non dans un abonnement a part : un second abonnement
      // declencherait une seconde requete (`refCount: true`), et les deux
      // lectures pourraient se croiser.
      tap(n => {
      if (n) {
        this.rempliContexte(n);
        this.remplitValideur(n);
      }
    }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    this.loadPhotos();
    this.loadFiveWhys();
    this.loadIshikawa();
    this.chargerRemerciement();
  }

  // --- Ishikawa ---------------------------------------------------------------

  /** Chargement non bloquant : la fiche reste utilisable si l'appel échoue. */
  private loadIshikawa(): void {
    this.ishikawa.listForNc(this.ncId).subscribe({
      next: diagrams => this.ishikawa$.next(diagrams),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[nc-detail] ishikawa listForNc failed', err?.status, err?.error?.title);
      }
    });
  }

  /**
   * Ouvre le diagramme d'Ishikawa de cette non-conformité — et le crée s'il
   * n'existe pas encore, en reprenant l'énoncé du problème depuis la NC plutôt
   * qu'en demandant de le retaper. L'utilisateur n'a pas à savoir laquelle des
   * deux choses se produit : il veut voir les causes de son écart.
   */
  openIshikawa(nc: NcResponse): void {
    if (this.ishikawaBusy$.value) return;
    const existing = this.ishikawa$.value;
    if (existing.length > 0) {
      this.router.navigate(['/ishikawa', existing[0].id]);
      return;
    }
    this.ishikawaBusy$.next(true);
    this.ishikawa.createDiagram({
      problemStatement: nc.title,
      description: nc.description,
      mode: 'SIX_M',
      // Le porteur par défaut est celui qui ouvre l'analyse : c'est lui qui la
      // mène, et un diagramme sans porteur n'avance pas.
      ownerId: this.auth.snapshot()?.userId ?? '',
      ncId: this.ncId
    })
      .pipe(finalize(() => this.ishikawaBusy$.next(false)))
      .subscribe({
        next: diagram => {
          this.ishikawa$.next([diagram, ...this.ishikawa$.value]);
          this.router.navigate(['/ishikawa', diagram.id]);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] create ishikawa failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@nc.detail.ishikawa-error:Impossible d'ouvrir le diagramme d'Ishikawa.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  /**
   * Même règle que les 5 Pourquoi : une NC close ne lance plus d'analyse, mais
   * laisse relire celle qui a conclu — c'est elle qui explique la décision.
   */
  canOpenIshikawa(status: NcStatus): boolean {
    return !this.isTerminal(status) || this.ishikawa$.value.length > 0;
  }

  // --- 5 Pourquoi -------------------------------------------------------------

  /**
   * Chargement non bloquant : la fiche reste utilisable si l'appel échoue.
   *
   * <p>Public, parce que le D4 le rappelle : conclure une cause racine depuis la
   * cascade des 5 Pourquoi change ce que la figure doit montrer, et la discipline
   * se relit sans recharger l'écart entier.
   */
  loadFiveWhys(): void {
    this.fiveWhys.listForNc(this.ncId).subscribe({
      next: analyses => this.fiveWhys$.next(analyses),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[nc-detail] listForNc failed', err?.status, err?.error?.title);
      }
    });
  }

  /**
   * Ouvre l'analyse des 5 Pourquoi de cette non-conformité — et la crée si elle
   * n'existe pas encore. L'utilisateur n'a pas à savoir laquelle des deux choses
   * se produit : dans les deux cas il veut la même chose, dérouler la chaîne.
   *
   * <p>Une seule analyse est créée par clic ; les analyses déjà ouvertes sont
   * listées par le serveur de la plus récente à la plus ancienne, on ouvre donc
   * la première.
   */
  openFiveWhys(): void {
    if (this.fiveWhysBusy$.value) return;
    const existing = this.fiveWhys$.value;
    if (existing.length > 0) {
      this.router.navigate(['/five-whys', existing[0].id]);
      return;
    }
    this.fiveWhysBusy$.next(true);
    this.fiveWhys.create({ ncId: this.ncId })
      .pipe(finalize(() => this.fiveWhysBusy$.next(false)))
      .subscribe({
        next: analysis => {
          this.fiveWhys$.next([analysis, ...this.fiveWhys$.value]);
          this.router.navigate(['/five-whys', analysis.id]);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] create five-whys failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@nc.detail.five-whys-error:Impossible d'ouvrir l'analyse des 5 Pourquoi.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  /**
   * Sur une NC clôturée ou annulée, on ne lance plus d'analyse — mais on peut
   * toujours relire celle qui a conclu : c'est elle qui explique la décision.
   */
  canOpenFiveWhys(status: NcStatus): boolean {
    return !this.isTerminal(status) || this.fiveWhys$.value.length > 0;
  }

  // --- photos -----------------------------------------------------------------

  loadPhotos(): void {
    queueMicrotask(() => this.photosLoading$.next(true));
    this.svc.listPhotos(this.ncId)
      .pipe(finalize(() => this.photosLoading$.next(false)))
      .subscribe({
        next: photos => { this.photos$.next(photos); this.storageDisabled$.next(false); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] listPhotos failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.storageDisabled$.next(true); return; }
          // Échec non bloquant : la fiche reste utilisable, les photos juste absentes.
        }
      });
  }

  /** Déclenche l'<input type="file"> masqué (caméra / galerie). */
  triggerFilePicker(input: HTMLInputElement): void {
    if (this.uploading$.value) return;
    input.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    // Réinitialise l'input pour autoriser la re-sélection du même fichier.
    input.value = '';
    if (!file) return;
    this.uploadPhoto(file);
  }

  private uploadPhoto(file: File): void {
    this.uploading$.next(true);
    this.svc.uploadPhoto(this.ncId, file)
      .pipe(finalize(() => this.uploading$.next(false)))
      .subscribe({
        next: photo => {
          this.photos$.next([photo, ...this.photos$.value]);
          this.storageDisabled$.next(false);
          this.snack.open(
            $localize`:@@nc.photos.upload-success:Photo ajoutée.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] uploadPhoto failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.storageDisabled$.next(true); return; }
          const msg =
            err?.status === 413 || err?.status === 400
              ? $localize`:@@nc.photos.upload-rejected:Fichier refusé — image (JPEG, PNG, WebP, HEIC) de 10 Mo maximum.`
              : err?.status === 409
              ? $localize`:@@nc.photos.upload-closed:Impossible d'ajouter une photo à une non-conformité clôturée ou annulée.`
              : safeErrorMessage(err, $localize`:@@nc.photos.upload-error:Échec de l'ajout de la photo.`);
          this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  // --- analyse Vision 5S par IA ---------------------------------------------

  /** Déclenche l'<input type="file"> masqué dédié à l'analyse vision. */
  triggerVisionPicker(input: HTMLInputElement): void {
    if (this.visionAnalyzing$.value) return;
    input.click();
  }

  onVisionFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    // Réinitialise pour autoriser la re-sélection du même fichier.
    input.value = '';
    if (!file) return;
    this.analyzeVision(file);
  }

  private analyzeVision(file: File): void {
    this.visionAnalyzing$.next(true);
    this.svc.analyzePhotoVision(file)
      .pipe(finalize(() => this.visionAnalyzing$.next(false)))
      .subscribe({
        next: result => {
          this.visionResult$.next(result);
          this.visionUnavailable$.next(false);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] analyzePhotoVision failed', err?.status, err?.error?.type);
          if (this.isVisionUnavailable(err)) { this.visionUnavailable$.next(true); return; }
          const msg =
            err?.status === 413
              ? $localize`:@@nc.vision.too-large:Image trop volumineuse pour l'analyse vision.`
              : err?.status === 400
              ? $localize`:@@nc.vision.image-invalid:Image invalide — formats acceptés : JPEG, PNG, WebP.`
              : err?.status === 502
              ? $localize`:@@nc.vision.gateway-error:Le service d'analyse vision est momentanément indisponible.`
              : safeErrorMessage(err, $localize`:@@nc.vision.error:Échec de l'analyse vision.`);
          this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  /** Réinitialise le panneau (efface le dernier résultat). */
  clearVision(): void {
    this.visionResult$.next(null);
  }

  /**
   * 503 + ProblemDetail dont le type contient 'vision-unavailable' → service
   * coupé sur cet environnement, message UX doux plutôt qu'erreur brute.
   */
  private isVisionUnavailable(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('vision-unavailable');
  }

  /** Classe de jauge selon le score (vert/orange/rouge), sobre. */
  visionScoreClass(value: number): string {
    if (value >= 80) return 'score-good';
    if (value >= 60) return 'score-warn';
    return 'score-bad';
  }

  /** Confiance d'un finding en pourcentage entier. */
  visionConfidencePct(confidence: number): number {
    return Math.round(confidence * 100);
  }

  /** Classe de pastille de sévérité d'un finding (insensible à la casse). */
  visionSeverityClass(severity: string): string {
    return 'vsev vsev-' + (severity || 'unknown').toLowerCase();
  }

  deletePhoto(photo: NcPhoto): void {
    if (this.deletingId$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.photos.delete-confirm-title:Supprimer cette photo ?`,
        message: $localize`:@@nc.photos.delete-confirm-message:La photo sera définitivement supprimée du stockage. Cette action est irréversible.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.deletingId$.next(photo.id);
      this.svc.deletePhoto(this.ncId, photo.id)
        .pipe(finalize(() => this.deletingId$.next(null)))
        .subscribe({
          next: () => {
            this.photos$.next(this.photos$.value.filter(p => p.id !== photo.id));
            this.snack.open(
              $localize`:@@nc.photos.delete-success:Photo supprimée.`,
              $localize`:@@common.ok:OK`, { duration: 2000 });
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-detail] deletePhoto failed', err?.status, err?.error?.type);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@nc.photos.delete-error:Échec de la suppression de la photo.`),
              $localize`:@@common.ok:OK`, { duration: 4000 });
          }
        });
    });
  }

  /**
   * 503 + ProblemDetail dont le type contient 'storage-disabled' → message UX
   * dédié, pas une erreur brute. Le backend émet le type en URI complète
   * (https://qualitos.io/errors/storage-disabled) — détection par inclusion.
   */
  private isStorageDisabled(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('storage-disabled');
  }

  /** Le bouton d'ajout est masqué dès que la NC est en état terminal. */
  canAddPhoto(status: NcStatus): boolean {
    return !this.isTerminal(status);
  }

  /**
   * Ouvre le rapport 8D de cette non-conformité.
   *
   * <p>Accessible à tout stade : le rapport se renseigne au fil du traitement et ne
   * s'émet qu'à la clôture — le serveur tient cette seconde règle. Fermer l'écran
   * avant la clôture obligerait à ressaisir D1, D3 et D8 le jour J.
   */
  openEightD(): void {
    this.router.navigate(['/nc', this.ncIdForNavigation(), '8d']);
  }

  /** L'identifiant de la NC ouverte, tel que la route l'a fourni. */
  private ncIdForNavigation(): string {
    return this.route.snapshot.paramMap.get('id') ?? '';
  }

  goBack(): void {
    this.router.navigate(['/nc']);
  }

  photoList(photoUrls?: string): string[] {
    if (!photoUrls) return [];
    return photoUrls.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  }

  startAnalysis(): void { this.transition('start-analysis'); }
  close(): void { this.transition('close'); }

  /**
   * Le bouton d'actions de l'en-tête : « Nouvelle action », ou
   * « Voir les actions » dès qu'un dossier existe.
   *
   * <p>Un seul libellé, quel que soit l'avancement. Le bouton disait
   * « Définir action » et ne faisait que franchir une étape du cycle de vie :
   * l'écart passait à « action définie » sans qu'aucune action n'existe nulle
   * part, et il fallait trouver « Créer une CAPA », ailleurs dans l'écran, pour
   * écrire l'action que le statut prétendait déjà définie.
   *
   * <p>Deux chemins derrière le même mot, selon ce qui existe déjà :
   * <ul>
   *   <li>pas de dossier : la fenêtre de création ouvre le dossier, et le
   *       serveur fait avancer l'écart du même geste ;</li>
   *   <li>un dossier : la fenêtre qui liste ses actions, où l'on en ajoute et où
   *       une ligne s'ouvre pour se corriger (ADR 0115).</li>
   * </ul>
   */
  addActions(nc: NcResponse): void {
    if (this.acting$.value) return;
    if (!nc.capaCaseId) {
      this.openCapa(nc);
      return;
    }
    this.dialog.open<CapaActionsDialogComponent, CapaActionsDialogData, boolean>(
      CapaActionsDialogComponent, {
        data: { capaCaseId: nc.capaCaseId, reference: nc.reference },
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        // Large, parce que la fenêtre porte un tableau de cinq colonnes. La
        // largeur se décide ICI, par la classe du panneau, et nulle part
        // ailleurs : la feuille du dialogue réclamait de son côté 760 px de
        // contenu dans un panneau qui en fait 680, et la surface, qui masque ce
        // qui déborde, coupait net les phrases et la dernière colonne.
        panelClass: ['qos-dialog-panel', 'qos-dialog-panel--wide']
      }
    ).afterClosed().subscribe(touche => {
      // La fiche porte le statut de l'écart, qui dépend de celui de l'action
      // côté serveur : elle se relit dès qu'une action a bougé.
      if (touche) this.reload();
    });
  }

  cancel(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.detail.cancel-confirm-title:Annuler cette non-conformité ?`,
        message: $localize`:@@nc.detail.cancel-confirm-message:La non-conformité sera marquée comme annulée. Cette décision est traçable.`,
        confirmLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.transition('cancel');
    });
  }

  /**
   * Écarte la réclamation, motif obligatoire.
   *
   * <p>Un dialogue de saisie et non une simple confirmation : le motif est ce que
   * le client lira, et ce que l'auditeur demandera.
   */
  openReject(n: NcResponse): void {
    const data: NcRejectDialogData = { ncId: n.id, reference: n.reference };
    this.dialog
      .open(NcRejectDialogComponent, {
        panelClass: 'qos-dialog-panel',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        data
      })
      .afterClosed()
      .subscribe(rejetee => {
        if (rejetee) this.reload$.next();
      });
  }

  openResolve(n: NcResponse): void {
    const data: NcResolveDialogData = { ncId: n.id, reference: n.reference };
    this.dialog
      .open(NcResolveDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(resolved => { if (resolved) this.reload$.next(); });
  }

  /**
   * Ouvre LA fenêtre de création CAPA, source verrouillée sur cette NC (ADR 0088).
   *
   * <p>Un écart a UN dossier CAPA, et ce dossier porte autant d'actions qu'il
   * en faut (ADR 0115). Cette fenêtre ne pose que la première : intitulé,
   * nature, responsable et échéance se saisissent une fois, et le serveur crée
   * le dossier, son action et le lien en une écriture. Les suivantes s'ajoutent
   * dans le tableau des actions de la fiche. Si l'écart a déjà sa CAPA, on
   * l'ouvre au lieu d'en proposer une seconde.
   */
  openCapa(nc: NcResponse): void {
    if (this.acting$.value) return;
    if (nc.capaCaseId) {
      this.router.navigate(['/capa', nc.capaCaseId]);
      return;
    }
    const data: CapaCreateDialogData = {
      // La source SUIT L'ORIGINE DE L'ECART, comme le serveur la derivera
      // (NcService.escalateToCapa). Elle etait ecrite « NON_CONFORMITY » en
      // dur : la fenetre annoncait donc « Non-conformite interne » avant de
      // creer un dossier marque « externe » des que l'ecart venait d'une
      // reclamation. Le champ est verrouille, et un champ verrouille qui
      // affiche autre chose que ce qui sera ecrit est pire qu'un champ absent.
      source: {
        type: nc.origin === 'EXTERNAL' ? 'EXTERNAL_NON_CONFORMITY' : 'NON_CONFORMITY',
        reference: nc.reference
      },
      // Ni titre ni description preremplis : recopier ceux de l'ecart donnait un
      // dossier qui redisait le constat au lieu de nommer ce qu'on va faire, et
      // le champ pre-rempli se validait tel quel. La source, elle, reste
      // verrouillee : c'est une donnee, pas une phrase a ecrire.
      prefill: {
        type: 'CORRECTIVE',
        criticity: NcDetailComponent.criticityOf(nc.severity)
      },
      open: request => this.svc.escalateToCapa(nc.id, request).pipe(
        map(updated => {
          if (!updated.capaCaseId) {
            // Le serveur a accepté sans rendre le lien : on le dit plutôt que de
            // prétendre ouvrir un dossier introuvable.
            throw new Error('capaCaseId absent');
          }
          return { capaId: updated.capaCaseId };
        }))
    };
    this.dialog.open<CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened>(CapaCreateDialogComponent, {
      data,
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(opened => {
      // Rien n'a été créé si l'utilisateur a refermé : on ne recharge pas.
      if (opened) this.reload$.next();
    });
  }

  /**
   * Ouvre, depuis l'écart, le cycle PDCA ou le projet DMAIC qui le traitera
   * (§3.6).
   *
   * <p>La CAPA traite l'écart lui-même : on contient, on corrige, on vérifie
   * que ça a tenu. Elle ne couvre pas tout — un écart qui révèle un procédé
   * instable demande des mesures et une phase de maîtrise, un écart qui révèle
   * une pratique à corriger demande un cycle. Les deux modules existaient, sans
   * porte depuis ici.
   *
   * <p>Si le travail est déjà ouvert, le bouton y emmène au lieu d'en proposer
   * un second : deux chantiers sur le même écart le traiteraient deux fois, et
   * chacun compterait dans les indicateurs.
   */
  convert(nc: NcResponse): void {
    if (this.acting$.value) return;
    if (nc.pdcaCycleId) {
      this.router.navigate(['/pdca', nc.pdcaCycleId]);
      return;
    }
    if (nc.dmaicProjectId) {
      this.router.navigate(['/dmaic', nc.dmaicProjectId]);
      return;
    }
    const data: NcConvertDialogData = {
      reference: nc.reference, title: nc.title, severity: nc.severity
    };
    this.dialog.open<NcConvertDialogComponent, NcConvertDialogData, NcConvertChoice>(
      NcConvertDialogComponent,
      { data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel' }
    ).afterClosed().subscribe(choix => {
      // Rien n'a été ouvert si l'utilisateur a refermé : on ne recharge pas.
      if (!choix) return;
      this.acting$.next(true);
      this.svc.convert(nc.id, choix)
        .pipe(finalize(() => this.acting$.next(false)))
        .subscribe({
          next: maj => {
            this.snack.open(
              choix.method === 'DMAIC'
                ? $localize`:@@nc.convert.dmaic-opened:Projet DMAIC ouvert.`
                : $localize`:@@nc.convert.pdca-opened:Cycle PDCA ouvert.`,
              $localize`:@@common.ok:OK`, { duration: 3000 });
            // On emmène où le travail se fait : s'arrêter sur la fiche de
            // l'écart obligerait à retrouver à la main un dossier qu'on vient
            // de créer.
            const cible = maj.dmaicProjectId
              ? ['/dmaic', maj.dmaicProjectId]
              : maj.pdcaCycleId ? ['/pdca', maj.pdcaCycleId] : null;
            if (cible) {
              this.router.navigate(cible);
            } else {
              this.reload$.next();
            }
          },
          error: err => this.snack.open(
            safeErrorMessage(err, $localize`:@@nc.convert.failed:Le dossier n'a pas pu être ouvert.`),
            $localize`:@@common.ok:OK`, { duration: 6000 })
        });
    });
  }

  /** La gravité de l'écart propose la criticité du dossier ; l'utilisateur la corrige s'il le faut. */
  static criticityOf(severity: NcSeverity): CapaCriticity {
    return severity === 'CRITICAL' ? 'CRITICAL' : severity === 'MAJOR' ? 'HIGH' : 'LOW';
  }

  /**
   * Les franchissements qui n'ont rien à saisir.
   *
   * <p>« Définir l'action » n'en fait plus partie : c'est l'ouverture de la CAPA
   * qui fait avancer l'écart, et le serveur tient les deux d'un seul geste.
   */
  private transition(action: 'start-analysis' | 'close' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start-analysis' ? this.svc.startAnalysis(this.ncId)
      : action === 'close' ? this.svc.close(this.ncId)
      : this.svc.cancel(this.ncId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@nc.detail.transition-success:Statut mis à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[nc-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@nc.detail.transition-error:Erreur lors de la transition.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  isTerminal(status: NcStatus): boolean {
    // REJECTED est terminal comme les deux autres : plus rien ne bouge sur un
    // constat écarté, et le serveur refuse toute modification.
    return status === 'CLOSED' || status === 'CANCELLED' || status === 'REJECTED';
  }

  // --- l'équipe en charge ------------------------------------------------------

  /**
   * L'équipe se remanie tant que l'écart vit : les gens changent de poste, et
   * une équipe figée à l'ouverture ne répond plus de rien six mois plus tard.
   * Sur un écart clos ou écarté, elle est une archive — le serveur refuse de la
   * modifier, et l'écran ne le propose donc pas.
   */
  canEditTeam(status: NcStatus): boolean { return !this.isTerminal(status); }

  // ===================== Le valideur de la cloture =========================

  /**
   * LA PERSONNE DÉSIGNÉE pour porter le second regard sur cet écart.
   *
   * <p>Facultative — et c'est pour cela qu'elle existe. Sans elle, clore
   * revient aux rôles de pilotage, ce qui suffit à une organisation qui en
   * compte plusieurs. Une petite structure, elle, n'a qu'un responsable
   * qualité : c'est lui qui résout, et il serait alors le seul à pouvoir clore
   * ce qu'il vient de résoudre. La désignation ouvre ce regard à quelqu'un
   * d'autre — un chef d'atelier, un pilote de processus, un confrère d'un autre
   * service — sans lui donner le moindre droit sur les autres écarts.
   *
   * <p>Choisie dans l'ANNUAIRE, jamais tapée : l'identifiant doit être le
   * compte de connexion, sans quoi le serveur ne reconnaîtra pas la personne au
   * moment où elle clôture.
   */
  readonly valideurControl = new FormControl<string | null>(null);
  readonly valideurSaving$ = new BehaviorSubject<boolean>(false);
  private valideurNom: string | null = null;
  valideurModifie = false;

  /** Le nom retenu avec l identifiant : la fiche montre un nom, jamais un UUID. */
  valideurChoisi(choix: DirectoryUser | null): void {
    this.valideurNom = choix?.displayName ?? null;
    this.valideurModifie = true;
  }

  enregistrerValideur(): void {
    if (this.valideurSaving$.value) {
      return;
    }
    const id = this.valideurControl.value;
    this.valideurSaving$.next(true);
    this.svc.updateNc(this.ncId, {
      validatorId: id ?? undefined,
      // Le NOM porte la décision : vide, il retire la désignation. Le serveur
      // suit le nom pour distinguer « retirer » de « ne pas y toucher », un
      // record ne sachant pas dire « absent ».
      validatorName: id ? (this.valideurNom ?? '') : ''
    }).pipe(
      finalize(() => this.valideurSaving$.next(false))
    ).subscribe({
      next: () => {
        this.valideurModifie = false;
        this.snack.open(
          $localize`:@@nc.8d.validator-saved:Valideur enregistré.`,
          $localize`:@@common.ok:OK`, { duration: 3000 });
        this.reload$.next();
      },
      error: err => {
        this.snack.open(
          safeErrorMessage(err,
            $localize`:@@nc.8d.validator-failed:Le valideur n'a pas pu être enregistré.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  /** Repose le champ sur ce que porte la fiche, sauf si on est en train d y ecrire. */
  private remplitValideur(n: NcResponse): void {
    if (this.valideurModifie) {
      return;
    }
    this.valideurNom = n.validatorName ?? null;
    this.valideurControl.setValue(n.validatorId ?? null, { emitEvent: false });
  }

    editTeam(nc: NcResponse): void {
    this.teamDraftControl.setValue([...(nc.teamMembers ?? [])]);
    this.teamEditing = true;
  }

  cancelTeamEdit(): void {
    this.teamEditing = false;
    this.teamDraftControl.setValue([]);
  }

  /**
   * Enregistre l'équipe remaniée.
   *
   * <p>Jamais vide : le serveur refuse une équipe sans personne, et un écart
   * dont plus personne ne répond ne se revoit pas. Le bouton est éteint dans ce
   * cas, et cette garde tient si l'appel vient d'ailleurs.
   */
  saveTeam(): void {
    if (this.teamDraft.length === 0 || this.teamSaving$.value) {
      return;
    }
    this.teamSaving$.next(true);
    this.svc.updateNc(this.ncId, { teamMembers: this.teamDraft }).pipe(
      finalize(() => this.teamSaving$.next(false))
    ).subscribe({
      next: () => {
        this.teamEditing = false;
        this.teamDraftControl.setValue([]);
        this.snack.open(
          $localize`:@@nc.detail.team-saved:Équipe enregistrée.`,
          $localize`:@@common.ok:OK`, { duration: 3000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[nc-detail] updateNc team failed', err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@nc.detail.team-failed:L'équipe n'a pas pu être enregistrée.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  canStartAnalysis(status: NcStatus): boolean { return status === 'OPEN'; }
  /**
   * Le bouton d'actions de l'en-tête.
   *
   * <p>Avec un dossier, toujours : relire ce qui reste ouvert est légitime à
   * tout moment, clôture comprise. Sans dossier, dès que l'écart n'est pas
   * terminé — exactement la garde du serveur, qui ouvre le dossier quel que
   * soit le statut et ne fait avancer l'écart que s'il était en analyse
   * (`NcService.escalateToCapa`).
   *
   * <p>Il a un temps exigé « en analyse » : c'était la garde de l'ancien
   * bouton « Définir action », qui franchissait une étape du cycle de vie. Ce
   * bouton-là n'existe plus, et la garde qu'il portait empêchait d'ouvrir une
   * CAPA sur un écart tout juste signalé — ce que le serveur accepte.
   */
  canAddActions(nc: NcResponse): boolean {
    return nc.capaCaseId ? true : !this.isTerminal(nc.status);
  }
  canResolve(status: NcStatus): boolean { return status === 'ACTION_DEFINED'; }
  canClose(status: NcStatus): boolean { return status === 'RESOLVED'; }

  /**
   * Le motif, rédigé à partir de son code.
   *
   * <p>Le refus existait déjà, et il était juste ; il n'arrivait qu'APRÈS le
   * clic, en anglais, construit par le service. L'utilisateur voyait un bouton
   * éteint sans savoir quoi faire pour l'allumer. La phrase dit donc le geste
   * qui débloque, pas seulement l'état.
   */
  blockerLabel(b: NcBlocker): string {
    switch (b.code) {
      case 'CAPA_ACTIONS_TO_CARRY_OUT':
        return b.count === 1
          ? $localize`:@@nc.blocker.actions-one:1 dossier CAPA porte encore des actions à mener. Terminez ses actions pour pouvoir résoudre cet écart.`
          : $localize`:@@nc.blocker.actions-many:${b.count}:count: dossiers CAPA portent encore des actions à mener. Terminez leurs actions pour pouvoir résoudre cet écart.`;
      case 'CAPA_CASES_UNSETTLED':
        return b.count === 1
          ? $localize`:@@nc.blocker.cases-one:1 dossier CAPA n'est pas clôturé. Clôturez-le — après vérification de son efficacité — pour pouvoir clôturer cet écart.`
          : $localize`:@@nc.blocker.cases-many:${b.count}:count: dossiers CAPA ne sont pas clôturés. Clôturez-les — après vérification de leur efficacité — pour pouvoir clôturer cet écart.`;
      case 'NO_CAPA_CASE':
        return $localize`:@@nc.blocker.no-capa:Aucune action n'est encore inscrite sur cet écart. Ajoutez au moins une action pour pouvoir le résoudre.`;

      case 'SECOND_LOOK_REQUIRED':
        // La phrase dit le GESTE qui débloque, et surtout que l'écart n'est pas
        // en défaut : c'est la personne qui ne peut pas le clôturer, pas l'écart
        // qui résiste. Sans cette nuance, on cherche ce qu'il reste à corriger.
        return $localize`:@@nc.blocker.second-look:Vous avez résolu cet écart : sa clôture demande un second regard. Un responsable qualité, ou le valideur désigné, doit la porter.`;
      default:
        // Code inconnu (serveur plus récent que l'écran) : mieux vaut une phrase
        // générique qu'un bouton éteint sans explication.
        return $localize`:@@nc.blocker.unknown:Un prérequis n'est pas satisfait sur cet écart.`;
    }
  }

  /**
   * TOUS les dossiers CAPA de l'écart, du plus ancien au plus récent.
   *
   * <p>La fiche n'en montrait qu'UN — celui que porte `capaCaseId`. Un second
   * dossier ouvert depuis la liste en citant la référence de l'écart ne
   * remplace pas ce lien : ses actions retenaient donc la clôture de l'écart
   * sans apparaître nulle part, et on lisait « 2 dossiers non réglés » au-dessus
   * d'un tableau qui n'en montrait qu'un.
   *
   * <p>Repli sur `capaCaseId` quand le serveur ne renvoie pas encore la liste :
   * l'écran continue d'afficher le dossier qu'il connaît plutôt que rien.
   */
  /**
   * Le commentaire d'efficacité d'un dossier : son brouillon s'il a été touché,
   * sinon ce que le serveur en sait.
   */
  commentaireEfficacite(d: NcCapaCaseRef): string {
    return this.brouillonsCommentaire.get(d.id) ?? d.effectivenessComment ?? '';
  }

  saisirCommentaire(d: NcCapaCaseRef, texte: string): void {
    this.brouillonsCommentaire.set(d.id, texte);
  }

  /**
   * Enregistre au départ du champ, et seulement si le texte a changé.
   *
   * <p>Enregistrer à chaque frappe ferait une requête par lettre ; attendre un
   * bouton ferait perdre la note à qui ferme la fiche. Le départ du champ est le
   * moment où l'on a fini d'écrire, et c'est le seul qui ne demande rien.
   */
  enregistrerCommentaire(d: NcCapaCaseRef): void {
    const texte = this.brouillonsCommentaire.get(d.id);
    if (texte === undefined || texte === (d.effectivenessComment ?? '')) {
      return;
    }
    this.capaService.updateCase(d.id, { effectivenessComment: texte }).subscribe({
      next: () => {
        d.effectivenessComment = texte;
        this.brouillonsCommentaire.delete(d.id);
        this.cdr.markForCheck();
      },
      error: () => {
        // Le brouillon RESTE : perdre la note sur un échec réseau la ferait
        // réécrire de mémoire, et personne ne réécrit deux fois la même.
        this.snack.open(
          $localize`:@@nc.8d.d6-comment-failed:Le commentaire n'a pas été enregistré. Réessayez en quittant le champ.`,
          $localize`:@@common.ok:OK`, { duration: 5000 });
      }
    });
  }

  dossiers(n: NcResponse): NcCapaCaseRef[] {
    if (n.capaCases?.length) {
      return n.capaCases;
    }
    return n.capaCaseId ? [{ id: n.capaCaseId }] : [];
  }

  /**
   * « 2 dossiers sur cet écart » — écrit SEULEMENT quand il y en a plusieurs.
   *
   * <p>Annoncer « 1 dossier » sur le cas ordinaire serait du bruit ; c'est la
   * pluralité qui est inattendue, et c'est elle qu'il faut dire.
   */
  compteDesDossiers(n: NcResponse): string {
    const nb = this.dossiers(n).length;
    return $localize`:@@nc.detail.capa-cases-count:${nb}:count: dossiers CAPA traitent cet écart`;
  }

  /** Le titre du bloc dépend de l'étape : résoudre ou clôturer. */
  blockersTitle(n: NcResponse): string {
    return n.status === 'RESOLVED'
      ? $localize`:@@nc.detail.blockers-close:Cet écart ne peut pas encore être clôturé`
      : $localize`:@@nc.detail.blockers-resolve:Cet écart ne peut pas encore être résolu`;
  }

  /**
   * Vrai dès qu'un motif subsiste — c'est ce qui éteint le bouton.
   *
   * <p>Le serveur reste l'autorité : il refuse de toute façon. Éteindre ici
   * évite seulement de proposer un geste qu'on sait repoussé.
   */
  blocked(n: NcResponse): boolean {
    return (n.blockers?.length ?? 0) > 0;
  }

  /**
   * Les motifs en une phrase, pour l'infobulle du bouton éteint.
   *
   * <p>Un bouton désactivé ne reçoit pas le survol sur tous les navigateurs ;
   * c'est pourquoi l'encart reste la source, et l'infobulle le rappel.
   */
  blockersHint(n: NcResponse): string {
    return (n.blockers ?? []).map(b => this.blockerLabel(b)).join(' ');
  }

  /** Infobulles nominales, déplacées dans le composant : le gabarit choisit
   *  désormais entre elles et les motifs, et une liaison ne peut pas porter
   *  d'attribut `i18n-matTooltip`. */
  readonly resolveTooltip = $localize`:@@nc.detail.resolve-tooltip:Résoudre la NC`;
  readonly closeTooltip = $localize`:@@nc.detail.close-tooltip:Clôturer la NC`;
  canCancel(status: NcStatus): boolean { return !this.isTerminal(status); }

  /**
   * Rejeter ne se propose que sur un écart signalé DU DEHORS, et tant qu'il n'a
   * pas reçu de réponse.
   *
   * <p>Miroir exact de la garde du serveur : exposer le bouton ailleurs
   * reviendrait à proposer une action qu'on sait refusée.
   */
  canReject(nc: NcResponse): boolean {
    return nc.origin === 'EXTERNAL'
      && (nc.status === 'OPEN' || nc.status === 'UNDER_ANALYSIS');
  }
  canEscalate(status: NcStatus): boolean { return !this.isTerminal(status); }

  statusBadgeClass(status: NcStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  severityBadgeClass(severity: NcSeverity): string {
    return 'sev sev-' + severity.toLowerCase();
  }
}
