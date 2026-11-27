import {
  AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren
} from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of, Subject, Subscription } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { gateReasons, safeErrorMessage } from '../../../../core/http/error-message';
import {
  CapaActionsDialogComponent, CapaActionsDialogData
} from '../../../capa/pages/capa-actions-dialog/capa-actions-dialog.component';
import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../../../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { CapaActionType, CapaCriticity, CapaSourceType } from '../../../capa/capa.types';
import { CapaService } from '../../../capa/capa.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  NcCloseDialogComponent, NcCloseDialogData, NcCloseDialogResult
} from '../nc-close-dialog/nc-close-dialog.component';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';
import { ProductsService } from '../../../products/products.service';
import { ProductResponse } from '../../../products/products.types';
import { NcService } from '../../nc.service';
import { TeamMemberRef } from '../../../../shared/user-picker/user-team-picker.component';
import {
  CloseNcRequest,
  NC_MAX_TEAM_MEMBERS,
  NcPhoto,
  NcBlocker,
  NcResponse,
  NcSeverity,
  NcStatus,
  NcSubcategory,
  VisionAnalysis,
  VisionScore,
  NcBlockingAction,
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
  NcSendBackDialogComponent,
  NcSendBackDialogData
} from '../nc-send-back-dialog/nc-send-back-dialog.component';

/**
 * Les huit disciplines, par leur code. Un type ferme plutot qu une chaine :
 * le sommaire, les ancres du document et les decomptes se designent par le
 * meme vocabulaire, et une faute de frappe ne passe pas.
 */
/**
 * Ce que le champ de depot du constat accepte, et que le serveur verifie a la
 * signature binaire.
 *
 * <p>La meme liste que les preuves d'un dossier : deux listes voisines mais
 * differentes laisseraient un fichier passer d'un ecran et pas de l'autre, sans
 * qu'on sache lequel a raison.
 */
export const TYPES_DE_PIECE_ACCEPTES =
  'application/pdf,image/jpeg,image/png,image/webp,image/heic,.docx,.xlsx';

export type EtapeCode = 'D1' | 'D2' | 'D3' | 'D4' | 'D5' | 'D6' | 'D7' | 'D8';

// OWASP A03 — refuse malformed UUIDs client-side. Demo mock ids ("nc-1"…)
// are also accepted so the page stays usable with useMockApi=true.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Un motif qui retient l'ecart, redige, avec les dossiers ou aller le lever.
 *
 * <p>`dossiers` est vide pour les motifs qui ne designent pas de dossier --
 * celui du second regard, celui du droit manquant.
 */
export interface MotifDeBlocage {
  code: string;
  phrase: string;
  dossiers: NcCapaCaseRef[];
}

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
   * Les preuves du D6, comptees PAR DOSSIER puis additionnees.
   *
   * <p>Le D6 pose un panneau par dossier, et chacun annonce ce qu il porte.
   * Tous ecrivaient sous la meme cle : le dernier arrive effacait le compte des
   * precedents. Un ecart dont le premier dossier portait trois preuves et le
   * dernier aucune se declarait donc vide, et le sommaire affirmait qu il
   * restait a faire ce qui venait d etre fait.
   *
   * <p>Les autres disciplines n ont qu un panneau, qui agrege lui-meme les
   * actions de tous les dossiers : elles n ont pas ce besoin.
   */
  private readonly preuvesParDossier = new Map<string, number>();

  /**
   * Le commentaire d'efficacité en cours de frappe, par dossier.
   *
   * <p>Il n'existe que tant qu'il diffère de l'enregistré : une entrée restée
   * là après un échec réseau est justement ce qui évite de réécrire la note.
   */
  private readonly brouillonsCommentaire = new Map<string, string>();

  /**
   * Ce que le champ de depot accepte, et que le serveur verifie a la signature.
   *
   * <p>La meme liste que les preuves d'un dossier : deux listes voisines mais
   * differentes laisseraient un fichier passer d'un ecran et pas de l'autre,
   * sans qu'on sache lequel a raison. Elle est declaree ici et non dans le
   * gabarit pour que le banc puisse l'affirmer.
   */
  readonly typesAcceptes = TYPES_DE_PIECE_ACCEPTES;

  /** Une image se montre ; tout le reste se nomme. */
  estUneImage(piece: NcPhoto): boolean {
    return (piece.contentType ?? '').toLowerCase().startsWith('image/');
  }

  /** L'icone qui dit de quel document il s'agit, sans l'ouvrir. */
  iconeDuType(contentType: string | undefined): string {
    const type = (contentType ?? '').toLowerCase();
    if (type === 'application/pdf') {
      return 'picture_as_pdf';
    }
    if (type.includes('spreadsheetml')) {
      return 'table_chart';
    }
    if (type.includes('wordprocessingml')) {
      return 'description';
    }
    return 'insert_drive_file';
  }

  /** Un panneau a fini de lire : le sommaire peut dire vrai pour sa discipline. */
  compter(code: EtapeCode, combien: number): void {
    this.comptesParEtape.set(code, combien);
  }

  /**
   * Un panneau de preuves a fini de lire : son compte s AJOUTE a celui des
   * autres dossiers, il ne les remplace pas.
   *
   * <p>La cle est le dossier, et non l ordre d arrivee : un panneau qui relit
   * apres un depot met a jour SON compte, sans toucher aux autres.
   */
  compterPreuves(dossierId: string, combien: number): void {
    this.preuvesParDossier.set(dossierId, combien);
    let total = 0;
    this.preuvesParDossier.forEach(n => { total += n; });
    this.comptesParEtape.set('D6', total);
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
      // LE D6 COCHE SUR CE QU'IL MONTRE. Il ne regardait que la note de
      // résolution -- qui s'écrit à l'étape « Résoudre », et pas ici. On
      // déposait donc une preuve, on écrivait le commentaire d'efficacité, et
      // la discipline restait grise : le sommaire affirmait qu'il restait à
      // faire ce qui venait d'être fait.
      //
      // Les trois signes que cette carte porte, et aucun autre : une pièce
      // déposée sur le dossier, le commentaire d'efficacité, la note de
      // résolution quand elle existe. Les photos du CONSTAT, elles, restent au
      // D2 : elles décrivent le problème et ne démontrent rien de l'efficacité.
      case 'D6': return (this.comptesParEtape.get('D6') ?? 0) > 0
        || this.dossiersAPreuve(n).some(d => !!this.commentaireEfficacite(d).trim())
        || !!n.resolutionNote;
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
    this.abonnements.forEach(a => a.unsubscribe());
  }

  /**
   * Ce que CE lecteur a le droit de faire, lu une fois.
   *
   * <p>Jusqu'ici, un lecteur sans le droit de clore ne voyait tout simplement
   * pas le bouton : rien ne distinguait « il n'y a plus rien a faire » de
   * « ce geste ne vous appartient pas ». Le bandeau le dit, et c'est la seule
   * raison pour laquelle l'ecran lit les droits lui-meme.
   *
   * <p>`lus` separe « pas encore charge » de « refuse » : annoncer un droit
   * manquant avant d'avoir lu la reponse accuserait a tort.
   */
  private droits = { lus: false, resoudre: false, clore: false };
  private readonly abonnements: Subscription[] = [];

  // ===================== Le bandeau d etat de l ecart =======================

  /**
   * Ce qui est arrive a l ecart, en une phrase, par son fait le PLUS RECENT.
   *
   * <p>Ces dates etaient six lignes d une grille d informations, lues apres le
   * reste : on ne voyait pas, en ouvrant la fiche, qu un ecart clos avait ete
   * repris. L ordre est celui de l histoire a l envers -- une reprise passe
   * devant la cloture qu elle defait.
   */
  /**
   * UN ECART RESOLU N'EN PORTE PAS : la note de resolution se lit au D6, sous
   * les preuves qu'elle commente, avec le nom de qui l'a ecrite. Repetee en
   * tete de fiche, elle poussait sous la ligne de flottaison le bandeau qui
   * dit ce qui RESTE a faire -- le seul des deux qui appelle un geste.
   */
  bandeau(n: NcResponse): string | null {
    // LE RENVOI PASSE PAR LES DATES, et non par l'ordre des branches comme les
    // autres faits. Une reprise suit toujours la cloture qu'elle defait ; un
    // renvoi, lui, PRECEDE la cloture qui finit par venir -- le mettre en tete
    // sans regarder l'horloge ferait porter « renvoye le 3 » a une fiche close
    // le 12.
    if (n.sentBackAt && this.estLePlusRecent(
          n.sentBackAt, [n.reopenedAt, n.closedAt, n.rejectedAt])) {
      return this.phrase(
        $localize`:@@nc.detail.banner-sent-back:Traitement renvoyé au pilote le`,
        n.sentBackAt, n.sentBackReason);
    }
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
    if (n.status === 'CANCELLED') {
      return $localize`:@@nc.8d.banner-cancelled:Écart annulé.`;
    }
    return null;
  }

  /** Vrai si cette date est postérieure à toutes les autres qui existent. */
  private estLePlusRecent(date: string, autres: (string | undefined)[]): boolean {
    const quand = new Date(date).getTime();
    return autres.every(a => !a || new Date(a).getTime() <= quand);
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
    batchNumber: new FormControl<string>('', { nonNullable: true }),
    /**
     * Le produit incrimine, CHOISI dans le referentiel et non retape.
     *
     * <p>Le modele de traitement 8D en fait une donnee du D2, aux cotes du
     * client et du lot. La fiche portait l'identifiant depuis toujours et ne
     * l'affichait pas : on ne savait pas, en lisant un ecart, sur quel produit
     * il portait -- et le rapport remis au client ne le disait pas non plus.
     *
     * <p>Une reference saisie a la main (« PRD-4821 ») n'aurait rattache l'ecart
     * a rien : ni au PFMEA du produit, ni a son plan de surveillance, ni aux
     * autres ecarts qu'il a connus.
     */
    productId: new FormControl<string>('', { nonNullable: true }),
    /**
     * Le constat lui-meme, corrigeable.
     *
     * <p>Il etait fige a la declaration : une faute de frappe, une quantite
     * fausse ou un detail manquant le restaient pour toujours, sur la piece
     * meme que le client lit. Le serveur l'acceptait deja ; aucun ecran ne
     * l'offrait.
     *
     * <p>Pas de validateur fixe : il est POSE A LA LECTURE de la fiche, et
     * seulement si le constat en porte un (voir {@link rempliContexte}). C'est
     * la regle du serveur a la lettre -- on ne peut pas EFFACER une description
     * deja ecrite, ce qui contournerait l'obligation posee a la creation, mais
     * un ecart ancien qui n'en a jamais eu ne doit pas pour autant bloquer
     * l'enregistrement du client ou du lot.
     */
    description: new FormControl<string>('', { nonNullable: true }),
    /**
     * La date du fait -- detecte en interne, recu du client dehors.
     *
     * <p>Une Date et non un Instant : le selecteur Material travaille au jour.
     * L'heure du constat, elle, est PRESERVEE a l'enregistrement (voir
     * {@link enregistrerContexte}) -- corriger un jour ne doit pas deplacer
     * silencieusement l'heure a minuit.
     */
    detectedOn: new FormControl<Date | null>(null)
  });

  /** Pas de constat a venir : la borne haute du selecteur de date. */
  readonly aujourdhui = new Date();
  /** L'instant du constat tel que la fiche le porte, pour en garder l'heure. */
  private heureDuConstat: string | null = null;

  /**
   * Le referentiel produit, pour NOMMER celui de l'ecart.
   *
   * <p>Charge une fois, sans bloquer : un lecteur qui n'a pas le droit de lire
   * le referentiel voit la fiche entiere, simplement sans le champ -- un
   * identifiant technique a la place du nom n'apprendrait rien a personne.
   */
  readonly produits$ = new BehaviorSubject<ProductResponse[]>([]);
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
    // L'heure du constat, retenue meme quand le formulaire ne la montre pas :
    // le selecteur travaille au jour, et corriger le jour ne doit pas deplacer
    // l'heure a minuit.
    this.heureDuConstat = n.detectedAt ?? null;
    if (this.contexteModifie) return;
    this.contexteForm.setValue({
      customerName: n.customerName ?? '',
      batchNumber: n.batchNumber ?? '',
      productId: n.productId ?? '',
      description: n.description ?? '',
      detectedOn: n.detectedAt ? new Date(n.detectedAt) : null
    }, { emitEvent: false });
    this.exigeLeConstat(!!(n.description ?? '').trim());
  }

  /**
   * Le constat devient obligatoire des lors qu'il existe.
   *
   * <p>Un champ blanc ne dit pas la meme chose selon la fiche : sur un ecart
   * qui porte un constat, le vider l'EFFACERAIT -- ce que le serveur refuse ;
   * sur un ecart ancien qui n'en a jamais eu, il veut dire « je n'y touche
   * pas ». Le validateur suit donc la fiche, et non le formulaire.
   *
   * <p>Pas `Validators.required` : il tient une chaine d'espaces pour une
   * valeur, et c'est precisement ce que le serveur rejette.
   */
  private exigeLeConstat(exige: boolean): void {
    const champ = this.contexteForm.controls.description;
    champ.setValidators(exige
      ? [c => (String(c.value ?? '').trim() ? null : { required: true })]
      : []);
    champ.updateValueAndValidity({ emitEvent: false });
  }

  /**
   * Le jour choisi, a l'heure du constat d'origine.
   *
   * <p>Le selecteur rend minuit. Garder l'heure deja enregistree evite de
   * deplacer le fait de plusieurs heures parce qu'on a corrige le jour -- et
   * sur un ecart detecte a 23 h, ce deplacement changeait la date elle-meme
   * dans un autre fuseau.
   */
  private auJour(jour: Date): string {
    const source = this.heureDuConstat ? new Date(this.heureDuConstat) : null;
    const corrige = new Date(jour.getTime());
    if (source) {
      corrige.setHours(source.getHours(), source.getMinutes(),
                       source.getSeconds(), source.getMilliseconds());
    }
    return corrige.toISOString();
  }

  /** Le produit de l'ecart, nomme par le referentiel ; vide s'il n'y en a pas. */
  produitLibelle(n: NcResponse): string {
    const trouve = this.produits$.value.find(p => p.id === n.productId);
    return trouve ? trouve.code + ' — ' + trouve.designation : '';
  }

  /** Le referentiel, pour la liste deroulante du D2. */
  private chargerProduits(): void {
    this.produits.list()
      .pipe(catchError(() => of([] as ProductResponse[])))
      .subscribe(liste => {
        this.produits$.next(liste);
        this.cdr.markForCheck();
      });
  }

  enregistrerContexte(): void {
    if (this.contexteSaving$.value) return;
    const valeurs = this.contexteForm.getRawValue();
    this.contexteSaving$.next(true);
    // Les chaines VIDES partent telles quelles : c est ainsi qu on EFFACE un
    // nom de client saisi par erreur. Le serveur distingue « vide » (effacer)
    // de « absent » (inchange).
    // Le produit ne part QUE s'il est choisi : le serveur lit l'identifiant nul
    // comme « inchange », et non comme « retire le produit ». Envoyer une
    // valeur vide n'effacerait donc rien, et echouerait a la lecture de l'UUID.
    this.svc.updateNc(this.ncId, {
      customerName: valeurs.customerName.trim(),
      batchNumber: valeurs.batchNumber.trim(),
      ...(valeurs.productId ? { productId: valeurs.productId } : {}),
      // Le constat ne part que s'il dit quelque chose : le serveur refuse une
      // description blanche, et le formulaire l'interdit deja.
      ...(valeurs.description.trim() ? { description: valeurs.description.trim() } : {}),
      ...(valeurs.detectedOn ? { detectedAt: this.auJour(valeurs.detectedOn) } : {})
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

  // ============ Le D4 : le commentaire de l analyse des causes =============

  /**
   * Le brouillon du commentaire du D4, tant qu il n est pas enregistre.
   *
   * <p>Null quand rien n a ete saisi depuis la derniere lecture : le champ lit
   * alors la fiche. Un brouillon refuse par le serveur RESTE -- perdre une
   * explication d analyse parce que le reseau a cede serait le pire moment.
   */
  private brouillonCause: string | null = null;

  /** Ce qui s affiche dans le champ : le brouillon s il y en a un, sinon la fiche. */
  commentaireCause(n: NcResponse): string {
    return this.brouillonCause ?? n.causeAnalysisComment ?? '';
  }

  saisirCommentaireCause(texte: string): void {
    this.brouillonCause = texte;
  }

  /**
   * Enregistre le commentaire du D4 en quittant le champ.
   *
   * <p>Pas de bouton : le champ est facultatif, et un bouton « Enregistrer »
   * sous une note libre se lit comme une etape de plus dans une discipline qui
   * n en demande pas. Rien ne part tant que le texte n a pas change -- quitter
   * un champ qu on n a pas touche ne doit pas ecrire sur l ecart.
   */
  enregistrerCommentaireCause(n: NcResponse): void {
    const texte = this.brouillonCause;
    if (texte === null || texte === (n.causeAnalysisComment ?? '')) {
      return;
    }
    this.svc.updateNc(this.ncId, { causeAnalysisComment: texte }).subscribe({
      next: () => {
        n.causeAnalysisComment = texte;
        this.brouillonCause = null;
        this.cdr.markForCheck();
      },
      error: () => {
        // Le brouillon RESTE : il contient peut-etre la seule trace du
        // raisonnement, et le champ doit pouvoir etre quitte a nouveau.
        this.snack.open(
          $localize`:@@nc.8d.d4-comment-failed:Le commentaire n'a pas été enregistré. Réessayez en quittant le champ.`,
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
    // Les comptes de preuves repartent de zero : un dossier retire laisserait
    // sinon son compte dans le total, et le D6 se dirait fait sans rien porter.
    // Les panneaux encore a l ecran reannoncent le leur en se rechargeant.
    this.preuvesParDossier.clear();
    // Le TOTAL aussi : vider la carte par dossier sans le recalculer laissait
    // l ancien total en place, et le D6 restait coche sur des preuves parties.
    this.comptesParEtape.set('D6', 0);
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
    private readonly produits: ProductsService,
    private readonly authz: AuthzService,
    private readonly cdr: ChangeDetectorRef,
    // La fiche elle-meme : le bandeau de blocage y cherche la ligne de
    // l'action qu'il nomme, sans passer par le document global.
    private readonly hote: ElementRef<HTMLElement>
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
    this.lireLesDroits();
    this.chargerProduits();
  }

  /**
   * Les deux droits dont le bandeau parle, et eux seuls.
   *
   * <p>Les deux reponses sont attendues ENSEMBLE : traitees separement, la
   * premiere arrivee ferait passer `lus` a vrai, et le bandeau aurait annonce
   * un droit manquant que la seconde reponse accordait.
   */
  private lireLesDroits(): void {
    this.abonnements.push(
      combineLatest([this.authz.can('nc.process'), this.authz.can('nc.close')])
        .subscribe(([resoudre, clore]) => {
          this.droits = { lus: true, resoudre, clore };
          this.cdr.markForCheck();
        })
    );
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

  /**
   * CLORE UN ÉCART, C'EST AUSSI DIRE CE QU'IL A ENSEIGNÉ.
   *
   * <p>Une fenêtre et non un franchissement muet : l'écart apprend quelque
   * chose sur l'analyse des risques du produit — un mode de défaillance que le
   * PFMEA n'avait pas prévu, un contrôle que le plan de surveillance ne
   * portait pas — et l'IATF 16949 en fait une exigence. La clôture ne demandait
   * rien, et le PFMEA restait muet sur un défaut survenu pour de bon.
   *
   * <p>Les réponses déjà données sont repassées à la fenêtre : une clôture
   * retenue par le verrou qu'elles ont posé n'a pas à les redemander.
   */
  close(nc: NcResponse): void {
    if (this.acting$.value) return;
    this.dialog.open(NcCloseDialogComponent, {
      width: '560px',
      autoFocus: 'first-tabbable',
      data: {
        reference: nc.reference,
        pfmeaImpacted: nc.pfmeaImpacted,
        controlPlanImpacted: nc.controlPlanImpacted
      } as NcCloseDialogData
    }).afterClosed().subscribe((reponse: NcCloseDialogResult | undefined) => {
      if (reponse) this.transition('close', reponse);
    });
  }

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

  /**
   * Résoudre, ou DIRE CE QUI MANQUE pour pouvoir le faire.
   *
   * <p>Aux deux premières étapes, le bouton était éteint et muet : le serveur
   * n'énumère volontairement aucun motif tant que l'étape n'est pas atteinte —
   * il ne reproche pas ce qu'il ne propose pas encore —, et le bandeau restait
   * donc vide. On voyait un bouton gris sans savoir quoi faire pour l'allumer.
   *
   * <p>Il reste donc cliquable, et il explique l'étape qui manque en proposant
   * le geste qui y mène — le même parti que l'export du rapport 8D. Le serveur
   * garde l'autorité : il refuserait de toute façon une résolution prématurée.
   */
  openResolve(n: NcResponse): void {
    if (this.acting$.value) return;
    if (!this.canResolve(n.status)) {
      this.expliquerEtapeAvantResolution(n);
      return;
    }
    this.transition('resolve');
  }

  /**
   * L'étape qui manque avant de pouvoir résoudre, et le geste qui y mène.
   *
   * <p>Deux étapes seulement, parce qu'il n'y en a pas d'autre avant la
   * résolution : l'écart n'est pas instruit, ou il l'est sans qu'aucune action
   * n'y soit inscrite. C'est l'OUVERTURE DU DOSSIER D'ACTIONS qui porte l'écart
   * à l'étape où il se résout — le serveur tient les deux d'un seul geste —,
   * et c'est donc elle que la phrase nomme, pas un franchissement d'état que
   * l'écran ne propose nulle part.
   */
  private expliquerEtapeAvantResolution(n: NcResponse): void {
    const instruire = n.status === 'OPEN';
    const data: ConfirmDialogData = {
      title: instruire
        ? $localize`:@@nc.detail.resolve-needs-analysis-title:Analysez l'écart d'abord`
        : $localize`:@@nc.detail.resolve-needs-action-title:Inscrivez une action d'abord`,
      message: instruire
        ? $localize`:@@nc.detail.resolve-needs-analysis:Cet écart n'est pas encore instruit. Lancez « Analyser », puis inscrivez au moins une action : c'est l'ouverture du dossier d'actions qui porte l'écart à l'étape où il se résout.`
        : $localize`:@@nc.detail.resolve-needs-action:Aucune action n'est encore inscrite sur cet écart. « Nouvelle action » ouvre son dossier et le porte à l'étape où il se résout.`,
      confirmLabel: instruire
        ? $localize`:@@nc.detail.start-analysis:Analyser`
        : $localize`:@@nc.detail.new-action:Nouvelle action`
    };
    this.dialog.open(ConfirmDialogComponent, {
      data, panelClass: 'qos-dialog-panel', autoFocus: false, restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      if (instruire) {
        this.startAnalysis();
      } else {
        this.addActions(n);
      }
    });
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
   *
   * @param nature la nature IMPOSÉE à cette première action, quand la
   *        discipline qui ouvre la fenêtre la connaît (D3, D5, D7). Absente,
   *        les trois natures restent proposées — c'est le cas du bouton
   *        d'ouverture générique.
   */
  openCapa(nc: NcResponse, nature?: CapaActionType): void {
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
        // La nature de la DISCIPLINE d'où l'on vient, et non « corrective » en
        // dur : la première action de confinement d'un écart, écrite depuis le
        // D3, naissait corrective — le registre CAPA l'affichait sous ce faux
        // type, et le D3 restait vide puisqu'il ne montre que l'endiguement.
        type: nature ?? 'CORRECTIVE',
        criticity: NcDetailComponent.criticityOf(nc.severity)
      },
      // Imposée, pas proposée : la discipline a déjà répondu à la question.
      lockType: !!nature,
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
  private transition(action: 'start-analysis' | 'resolve' | 'close' | 'cancel',
                     cloture?: CloseNcRequest): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start-analysis' ? this.svc.startAnalysis(this.ncId)
      // La resolution ne reclame plus rien : ce qui a resolu l'ecart est deja
      // ecrit, et mieux -- les actions menees au D5 et au D7, les preuves de
      // leur efficacite au D6. La note libre redisait tout cela en travers du
      // geste, dans une fenetre qu'il fallait remplir pour avancer.
      : action === 'resolve' ? this.svc.resolve(this.ncId, {})
      : action === 'close' ? this.svc.close(
          this.ncId, cloture ?? { pfmeaImpacted: false, controlPlanImpacted: false })
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
        // Les motifs que le serveur ÉNUMÈRE, s'il en donne : « le PFMEA attend
        // sa révision » dit quoi faire, là où un 409 nu se traduisait par
        // « rechargez la page » — un conseil faux, puisque recharger ne révise
        // rien. La fiche relue porte ensuite le même motif dans son bandeau.
        const motifs = gateReasons(err);
        this.snack.open(
          motifs.length
            ? motifs.join(' ')
            : safeErrorMessage(err, $localize`:@@nc.detail.transition-error:Erreur lors de la transition.`),
          // Plus long quand il y a plusieurs motifs a lire : une phrase
          // generique se saisit d'un coup d'oeil, une liste non.
          $localize`:@@common.ok:OK`, { duration: motifs.length ? 6000 : 4000 });
        // La fiche relue porte le meme motif dans son bandeau : sans cela le
        // bouton resterait allume au-dessus d'un verrou qu'on vient d'annoncer.
        if (motifs.length) this.reload$.next();
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

  /**
   * L'écart accepte-t-il encore qu'on ouvre une CAPA de plus ?
   *
   * <p>Et non {@link #canAddActions}, qui répond « oui » dès qu'un dossier
   * existe : il sert les boutons d'invitation d'un écart qui n'en a pas encore,
   * et ne dit rien d'un écart terminé qui en porte un. Tant qu'ajouter voulait
   * dire « une action de plus dans ce dossier », le verrou de clôture du
   * dossier servait de frein ; depuis que l'ajout ouvre une CAPA NOUVELLE
   * (ADR 0135), il n'a plus rien à dire, et sans ceci on déciderait une action
   * sur un écart clos -- dont la clôture l'attendrait ensuite.
   */
  accepteUneCapaDePlus(nc: NcResponse): boolean {
    return !this.isTerminal(nc.status);
  }
  canResolve(status: NcStatus): boolean { return status === 'ACTION_DEFINED'; }

  /**
   * Les étapes d'où l'on va ENCORE vers la résolution.
   *
   * <p>Le bouton y reste cliquable pour dire ce qui manque. Au-delà — écart
   * résolu, clos, annulé, rejeté — il n'y a plus rien à expliquer : le geste
   * n'a plus de sens, et le bouton s'éteint pour de bon.
   */
  avantResolution(status: NcStatus): boolean {
    return status === 'OPEN' || status === 'UNDER_ANALYSIS';
  }

  /**
   * Ce que dit le survol du bouton « Résoudre » : les motifs, l'étape, ou rien
   * de plus que son intitulé.
   */
  resolveHint(n: NcResponse): string {
    if (this.blocked(n)) {
      return this.blockersHint(n);
    }
    if (this.avantResolution(n.status)) {
      return n.status === 'OPEN'
        ? $localize`:@@nc.detail.resolve-hint-analysis:Cet écart n'est pas encore instruit : commencez par « Analyser ».`
        : $localize`:@@nc.detail.resolve-hint-action:Aucune action n'est encore inscrite : commencez par « Nouvelle action ».`;
    }
    return this.resolveTooltip;
  }
  canClose(status: NcStatus): boolean { return status === 'RESOLVED'; }

  /**
   * LA SECONDE REPONSE DU SECOND REGARD : renvoyer le traitement au pilote.
   *
   * <p>Au meme etat que la cloture -- resolu -- et sans la liste d'obstacles qui
   * eteint le bouton de cloture. Ces obstacles (un dossier CAPA encore en
   * travail, une revision d'analyse de risque qui manque) sont justement ce qui
   * appelle un renvoi : les opposer ici n'aurait laisse au valideur aucune
   * reponse du tout.
   *
   * <p>La personne qui a resolu l'ecart ne le voit pas : elle ne juge pas son
   * propre travail, dans un sens comme dans l'autre, et le serveur la refuse.
   */
  canSendBack(n: NcResponse): boolean {
    return n.status === 'RESOLVED' && !this.jeSuisLeResolveur(n);
  }

  /** Ouvre la fenetre du renvoi : il y a un message a ecrire, meme facultatif. */
  sendBack(n: NcResponse): void {
    if (this.acting$.value) return;
    this.dialog.open<NcSendBackDialogComponent, NcSendBackDialogData, NcResponse | undefined>(
      NcSendBackDialogComponent, {
        data: { ncId: n.id, reference: n.reference },
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      }
    ).afterClosed().subscribe(renvoi => {
      if (renvoi) this.reload$.next();
    });
  }

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
          ? $localize`:@@nc.blocker.actions-one:1 action est encore ouverte : terminez-la avant de résoudre cet écart.`
          : $localize`:@@nc.blocker.actions-many:${b.count}:count: actions sont encore ouvertes : terminez-les avant de résoudre cet écart.`;
      case 'CAPA_CASES_UNSETTLED':
        return b.count === 1
          ? $localize`:@@nc.blocker.cases-one:1 dossier CAPA n'est pas clôturé. Clôturez-le — après vérification de son efficacité — pour pouvoir clôturer cet écart.`
          : $localize`:@@nc.blocker.cases-many:${b.count}:count: dossiers CAPA ne sont pas clôturés. Clôturez-les — après vérification de leur efficacité — pour pouvoir clôturer cet écart.`;
      case 'NO_CAPA_CASE':
        return $localize`:@@nc.blocker.no-capa:Aucune action n'est encore inscrite sur cet écart. Ajoutez au moins une action pour pouvoir le résoudre.`;

      case 'PFMEA_REVISION_REQUIRED':
        // La phrase dit le GESTE : accepter la revision proposee sur le PFMEA.
        // Dire « le PFMEA attend sa revision » sans dire ou l'acter laisse
        // chercher dans un autre module ce qu'on vient de nous refuser.
        return $localize`:@@nc.blocker.pfmea-revision:Cet écart a été déclaré impactant le PFMEA du produit. Acceptez la révision du PFMEA qui le concerne pour pouvoir le clôturer — ou corrigez la réponse si le PFMEA n'était pas en cause.`;

      case 'CONTROL_PLAN_REVISION_REQUIRED':
        return $localize`:@@nc.blocker.control-plan-revision:Cet écart a été déclaré impactant le plan de surveillance. Acceptez la révision du plan qui le concerne pour pouvoir le clôturer — ou corrigez la réponse si le plan n'était pas en cause.`;

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
   * Les dossiers dont les preuves se déposent au D6.
   *
   * <p>Un dossier d'ENDIGUEMENT SEUL en est exclu. Sa preuve est celle d'un
   * geste de protection — le lot bloqué, les cartons repris — et elle se
   * dépose sur sa ligne du tableau du D3, qui porte une colonne « Preuve »
   * pour cela. Le D6 démontre autre chose : que le problème a CESSÉ. Y faire
   * figurer un dossier d'endiguement demandait de prouver l'efficacité d'une
   * action qui n'a jamais prétendu traiter la cause, et la même pièce se
   * serait réclamée à deux endroits.
   *
   * <p>`containmentOnly !== true` et non `=== false` : absent veut dire que le
   * serveur n'a pas répondu à la question, et on ne cache pas un dossier sur
   * une supposition.
   *
   * <p>Le rapport 8D appliquait déjà cette règle ({@code nc-eightd}) ; la fiche
   * de l'écart, non. Les deux écrans montraient donc le même D6 autrement.
   */
  dossiersAPreuve(n: NcResponse): NcCapaCaseRef[] {
    return this.dossiers(n).filter(d => d.containmentOnly !== true);
  }

  /**
   * L'écart n'a que des dossiers d'endiguement : le D6 le dit, et renvoie au D3.
   *
   * <p>Distinct de « aucun dossier ouvert » : les deux situations appellent un
   * geste différent, et les confondre enverrait ouvrir un dossier qui existe.
   */
  queDesEndiguements(n: NcResponse): boolean {
    return this.dossiers(n).length > 0 && this.dossiersAPreuve(n).length === 0;
  }

  /**
   * Les identifiants des dossiers de l'écart, en tableau STABLE.
   *
   * <p>Le panneau d'actions relit ses dossiers par le réseau dès que son entrée
   * change d'IDENTITÉ (`ngOnChanges`), et non de contenu. Un getter qui
   * recomposerait le tableau à chaque passe de détection le ferait donc requêter
   * en boucle : on rend le même objet tant que la liste ne bouge pas vraiment.
   */
  idsDesDossiers(n: NcResponse): readonly string[] {
    const ids = this.dossiers(n).map(d => d.id);
    const change = ids.length !== this.idsDeDossiers.length
      || ids.some((id, i) => id !== this.idsDeDossiers[i]);
    if (change) {
      this.idsDeDossiers = ids;
    }
    return this.idsDeDossiers;
  }

  private idsDeDossiers: string[] = [];

  /**
   * L'écart à citer quand une discipline ouvre une CAPA de plus.
   *
   * <p>Le TYPE suit l'origine de l'écart, comme le serveur la dérivera : un
   * dossier né d'une réclamation client s'affichait « non-conformité interne »
   * sur l'écran même qui sert à répondre à ce client.
   *
   * <p>Objet mis en cache, pour la raison de {@link #idsDesDossiers} : le
   * panneau relit ses dossiers dès que son entrée change d'identité, et un
   * objet neuf à chaque passe de détection le ferait requêter sans fin.
   */
  sourceDeLEcart(n: NcResponse): { type: CapaSourceType; reference: string } | undefined {
    if (!n.reference) {
      return undefined;
    }
    const type: CapaSourceType =
      n.origin === 'EXTERNAL' ? 'EXTERNAL_NON_CONFORMITY' : 'NON_CONFORMITY';
    if (this.sourceEnCache?.type !== type || this.sourceEnCache?.reference !== n.reference) {
      this.sourceEnCache = { type, reference: n.reference };
    }
    return this.sourceEnCache;
  }

  private sourceEnCache?: { type: CapaSourceType; reference: string };


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
  /**
   * Ce qui retient l'ecart, PRET A LIRE : la phrase, et ou aller.
   *
   * <p>Le bandeau enoncait les motifs et s'arretait la. « 2 dossiers CAPA ne
   * sont pas clotures » laissait chercher lesquels dans un registre qui en
   * compte des centaines ; le geste nomme sans son chemin est un travail rendu
   * a l'utilisateur.
   *
   * <p>Les dossiers nommes sont ceux que la fiche porte deja -- donc ceux que
   * CE lecteur a le droit de voir (ADR 0081). Le DECOMPTE, lui, vient du
   * serveur et compte tous les dossiers, y compris ceux qu'on ne voit pas :
   * c'est un invariant, il ne depend pas de qui regarde. Les deux chiffres
   * peuvent donc differer, et c'est voulu -- on ne divulgue pas le titre d'un
   * dossier hors de portee par la fiche d'un ecart.
   */
  motifsDeBlocage(n: NcResponse): MotifDeBlocage[] {
    const motifs: MotifDeBlocage[] = (n.blockers ?? []).map(b => ({
      code: String(b.code),
      phrase: this.blockerLabel(b),
      dossiers: this.dossiersDuMotif(b, n)
    }));
    const droit = this.motifDeDroit(n);
    return droit ? [...motifs, droit] : motifs;
  }

  /** Les dossiers que CE motif-la designe ; aucun pour les autres motifs. */
  private dossiersDuMotif(b: NcBlocker, n: NcResponse): NcCapaCaseRef[] {
    if (b.code === 'CAPA_ACTIONS_TO_CARRY_OUT') {
      return this.dossiers(n).filter(d => d.status === 'OPEN' || d.status === 'IN_PROGRESS');
    }
    if (b.code === 'CAPA_CASES_UNSETTLED') {
      return this.dossiers(n).filter(d => d.status !== 'CLOSED' && d.status !== 'REJECTED');
    }
    return [];
  }

  /**
   * Le motif qui ne vient pas du serveur : le droit qui manque A CE LECTEUR.
   *
   * <p>Le serveur n'a pas a l'enumerer -- il repondrait 403, et il ne sait pas
   * quel geste la personne allait tenter. L'ecran, lui, le sait : l'ecart est a
   * l'etape ou l'on resout, ou a celle ou l'on clot, et le bouton de ce geste
   * est absent parce que le droit manque. Sans cette ligne, la fiche se lisait
   * comme s'il n'y avait plus rien a faire.
   *
   * <p>Un seul motif a la fois : c'est le geste du moment qui compte, pas la
   * liste de tous ceux qu'on ne pourra pas faire plus tard.
   */
  private motifDeDroit(n: NcResponse): MotifDeBlocage | null {
    if (!this.droits.lus) {
      return null;
    }
    if (this.canResolve(n.status) && !this.droits.resoudre) {
      return {
        code: 'RIGHT_MISSING',
        phrase: $localize`:@@nc.blocker.no-right-resolve:Vous n'avez pas le droit de résoudre un écart. Demandez à un responsable qualité de porter ce geste.`,
        dossiers: []
      };
    }
    if (this.canClose(n.status) && !this.droits.clore) {
      return {
        code: 'RIGHT_MISSING',
        phrase: $localize`:@@nc.blocker.no-right-close:Vous n'avez pas le droit de clôturer un écart. Demandez à un responsable qualité de porter ce geste.`,
        dossiers: []
      };
    }
    return null;
  }

  /**
   * Est-ce MOI qui ai resolu cet ecart ?
   *
   * <p>Lu du motif que le serveur a deja calcule, et non d'une comparaison
   * d'identifiants refaite ici : `SECOND_LOOK_REQUIRED` est le seul motif qui
   * depende de QUI LIT la fiche, et il ne parait qu'a cette personne-la. Deux
   * lectures de la meme regle finiraient par diverger ; celle-ci n'en a
   * qu'une.
   */
  jeSuisLeResolveur(n: NcResponse): boolean {
    return (n.blockers ?? []).some(b => String(b.code) === 'SECOND_LOOK_REQUIRED');
  }

  /**
   * Les actions de CE dossier que CE motif-la retient.
   *
   * <p>Au motif de la CLOTURE, toutes : une action faite dont la verification
   * promise n'a pas conclu retient le dossier, et donc l'ecart (ADR 0132).
   *
   * <p>Au motif de la RESOLUTION, les seules qui restent A FAIRE. Une action
   * terminee depuis le 8D y figurait, sous une phrase qui demandait de la
   * terminer alors qu'elle l'etait, et le decompte de la ligne contredisait
   * celui du motif. La verification, elle, se dit au moment de clore.
   *
   * <p>Le dossier n'est pas recopie pour autant : un clone par passe de
   * detection recreerait le bloc entier et lui ferait perdre le focus.
   */
  actionsQuiRetiennent(motif: MotifDeBlocage, dossier: NcCapaCaseRef): NcBlockingAction[] {
    const actions = dossier.blockingActions ?? [];
    return motif.code === 'CAPA_ACTIONS_TO_CARRY_OUT'
      ? actions.filter(a => a.reason === 'TO_DO')
      : actions;
  }

  /**
   * COMBIEN d'actions retiennent ce dossier.
   *
   * <p>Le decompte du motif compte les DOSSIERS ; celui-ci compte ce qu'il
   * reste a faire dedans, qui est le travail reel. Un dossier peut n'en
   * retenir qu'une comme en retenir dix, et les deux se lisaient pareil.
   *
   * <p>Deux phrases plutot qu'un pluriel calcule : « 1 actions » est une faute
   * que personne ne pardonne a un logiciel de qualite.
   */
  compteDesActionsQuiBloquent(retenues: NcBlockingAction[]): string {
    const nb = retenues.length;
    return nb === 1
      ? $localize`:@@nc.blocker.actions-count-one:1 action le retient`
      : $localize`:@@nc.blocker.actions-count-many:${nb}:count: actions le retiennent`;
  }

  /**
   * MENER A L'ACTION, et pas seulement a son dossier.
   *
   * <p>L'action nommee dans le bandeau se traite dans le tableau de sa
   * discipline, plus bas sur cette meme fiche -- c'est la qu'elle a ete
   * decidee, et la fiche du dossier renvoie d'ailleurs ici (ADR 0115). Le lien
   * y amene donc et POSE LE FOCUS sur sa ligne : le clavier reprend la ou l'oeil
   * s'arrete, et une ligne de tableau s'ouvre avec Entree.
   *
   * <p>La ligne peut ne pas encore etre dessinee -- un panneau d'actions qui
   * charge encore. On ne fait alors rien plutot que de sauter n'importe ou.
   */
  allerALAction(action: NcBlockingAction): void {
    const ligne = (this.hote.nativeElement as HTMLElement)
      .querySelector<HTMLElement>('#capa-action-' + action.id);
    if (!ligne) {
      return;
    }
    ligne.scrollIntoView({ behavior: 'smooth', block: 'center' });
    ligne.focus({ preventScroll: true });
  }

  /** Le motif lui-meme sert de cle : deux motifs du meme code ne coexistent pas. */
  trackByMotif(_: number, motif: MotifDeBlocage): string {
    return motif.code;
  }

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
