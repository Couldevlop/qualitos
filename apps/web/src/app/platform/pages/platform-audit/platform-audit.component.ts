import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../core/http/error-message';
import { PlatformAuditService } from '../../platform-audit.service';
import {
  PlatformActorType, PlatformAuditAction, PlatformAuditCriterion, PlatformAuditEvent,
  PlatformAuditFilter, PlatformAuditPage, PlatformAuditVerification
} from '../../platform-audit.types';

/**
 * Le serveur rabote toute taille de page au-delà de 100 sans le dire
 * (`spring.data.web.pageable.max-page-size`). Proposer 200 rendait la moitié du
 * journal inatteignable : le paginateur comptait ses pages sur 200 pendant que
 * le serveur en servait 100. Sur une piste d'audit, une preuve silencieusement
 * hors d'atteinte est un défaut de conformité.
 */
const TAILLES_DE_PAGE = [25, 50, 100];
const TAILLE_MAXIMALE = 100;

/** Le serveur type les identifiants en UUID : une saisie libre partirait en 400. */
const MOTIF_UUID = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

/** Page vide servie après une erreur : remet les compteurs à zéro sous le bandeau. */
const PAGE_VIDE: PlatformAuditPage = {
  content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
};

/** Bornes de séquence de la page affichée, pour amorcer une vérification. */
export interface BornesDeSequence {
  from: number;
  to: number;
}

/**
 * Les libellés des actes, en clair.
 *
 * <p>Exporté pour que le banc vérifie la couverture : un acte ajouté côté
 * serveur et absent de cette table s'afficherait sous son code brut
 * (`PARTNER_CONSULTANT_ACTIVE_CHANGED`) dans un dossier destiné à une autorité.
 */
export const LIBELLES_DES_ACTES: Readonly<Record<PlatformAuditAction, string>> = {
  TENANT_CREATED: $localize`:@@platform.audit.act.tenant-created:Client créé`,
  TENANT_UPDATED: $localize`:@@platform.audit.act.tenant-updated:Client modifié`,
  TENANT_DEACTIVATED: $localize`:@@platform.audit.act.tenant-deactivated:Client désactivé`,
  MODULE_SUBSCRIBED: $localize`:@@platform.audit.act.module-subscribed:Module ouvert`,
  MODULE_CANCELLED: $localize`:@@platform.audit.act.module-cancelled:Module fermé`,
  MODULE_PRICE_SET: $localize`:@@platform.audit.act.price-set:Tarif fixé`,
  PARTNER_FIRM_CREATED: $localize`:@@platform.audit.act.firm-created:Cabinet créé`,
  PARTNER_FIRM_UPDATED: $localize`:@@platform.audit.act.firm-updated:Cabinet modifié`,
  PARTNER_FIRM_ACTIVE_CHANGED: $localize`:@@platform.audit.act.firm-active:Cabinet suspendu ou réactivé`,
  PARTNER_CONSULTANT_ADDED: $localize`:@@platform.audit.act.consultant-added:Consultant rattaché`,
  PARTNER_CONSULTANT_ACTIVE_CHANGED: $localize`:@@platform.audit.act.consultant-active:Consultant désactivé ou réactivé`,
  PARTNER_ENGAGEMENT_PROPOSED: $localize`:@@platform.audit.act.engagement-proposed:Mission proposée`,
  PARTNER_ENGAGEMENT_GRANTED: $localize`:@@platform.audit.act.engagement-granted:Accès accordé à un cabinet`,
  PARTNER_ENGAGEMENT_DECLINED: $localize`:@@platform.audit.act.engagement-declined:Mission refusée`,
  PARTNER_ENGAGEMENT_REVOKED: $localize`:@@platform.audit.act.engagement-revoked:Accès retiré à un cabinet`,
  AUDIT_EXPORTED: $localize`:@@platform.audit.act.exported:Dossier d'audit exporté`
};

/** L'ordre du sélecteur : les actes du dossier client, puis les modules, puis les cabinets. */
export const ACTES_PROPOSES: readonly PlatformAuditAction[] = [
  'TENANT_CREATED', 'TENANT_UPDATED', 'TENANT_DEACTIVATED',
  'MODULE_SUBSCRIBED', 'MODULE_CANCELLED', 'MODULE_PRICE_SET',
  'PARTNER_FIRM_CREATED', 'PARTNER_FIRM_UPDATED', 'PARTNER_FIRM_ACTIVE_CHANGED',
  'PARTNER_CONSULTANT_ADDED', 'PARTNER_CONSULTANT_ACTIVE_CHANGED',
  'PARTNER_ENGAGEMENT_PROPOSED', 'PARTNER_ENGAGEMENT_GRANTED',
  'PARTNER_ENGAGEMENT_DECLINED', 'PARTNER_ENGAGEMENT_REVOKED',
  'AUDIT_EXPORTED'
];

/**
 * La piste d'audit de la plateforme, vue par l'éditeur.
 *
 * <p><b>Ce que cet écran répare.</b> Chaque client pouvait consulter SA piste
 * d'audit ; les actes de l'éditeur sur son dossier n'étaient nulle part. Créer
 * un client, lui ouvrir un module, changer son tarif, accorder un accès à un
 * cabinet : ces gestes ne laissaient qu'une ligne de journal applicatif —
 * effaçable, non chaînée, non opposable. Un prestataire qui ne peut pas montrer
 * ses propres actes n'est pas auditable, et c'est exactement ce qu'une autorité
 * (ANSSI, CNIL, un client sous obligation DORA) vient vérifier.
 *
 * <p>Trois partis pris, repris de l'écran jumeau côté client :
 *
 * <ol>
 *   <li><b>Ajout seul assumé jusque dans l'écran</b> : aucune action
 *       d'écriture, de modification ni de suppression n'est offerte, et le
 *       serveur n'en expose aucune.</li>
 *   <li><b>Les filtres ne se combinent pas</b> côté serveur. L'écran annonce
 *       en clair le critère réellement appliqué, plutôt que de laisser déduire
 *       un ET inexistant de ses résultats.</li>
 *   <li><b>La vérification porte sur des numéros de séquence, pas sur des
 *       dates</b> : elle est donc amorcée depuis les bornes de la page
 *       affichée, ou lancée sur la totalité du journal — sans rien à deviner.
 *       </li>
 * </ol>
 */
@Component({
  selector: 'qos-platform-audit',
  templateUrl: './platform-audit.component.html',
  styleUrls: ['./platform-audit.component.scss'],
  standalone: false
})
export class PlatformAuditComponent implements OnInit {

  readonly colonnes = ['seq', 'quand', 'auteur', 'acte', 'cible', 'empreinte', 'detail'];
  readonly taillesDePage = TAILLES_DE_PAGE;
  readonly actesProposes = ACTES_PROPOSES;

  indexDePage = 0;
  taillePage = 50;

  readonly filtres = new FormGroup({
    action: new FormControl<PlatformAuditAction | ''>('', { nonNullable: true }),
    targetTenantId: new FormControl('', {
      nonNullable: true, validators: [Validators.pattern(MOTIF_UUID)]
    }),
    actorUserId: new FormControl('', {
      nonNullable: true, validators: [Validators.pattern(MOTIF_UUID)]
    }),
    from: new FormControl('', { nonNullable: true }),
    to: new FormControl('', { nonNullable: true })
  });

  actes$!: Observable<PlatformAuditEvent[]>;
  total$!: Observable<number>;
  /** `null` quand la page est vide : il n'y a alors aucune fenêtre à vérifier. */
  bornes$!: Observable<BornesDeSequence | null>;
  critere$!: Observable<PlatformAuditCriterion>;

  private readonly chargementEnCours = new BehaviorSubject<boolean>(false);
  readonly chargement$ = this.chargementEnCours.asObservable();
  erreur: string | null = null;

  /** L'acte ouvert dans le panneau de détail. */
  detail: PlatformAuditEvent | null = null;

  verdict: PlatformAuditVerification | null = null;
  verificationEnCours = false;
  erreurDeVerification: string | null = null;
  /** La fenêtre que le verdict affiché concerne, pour ne pas laisser croire à autre chose. */
  fenetreDuVerdict: 'totalite' | 'page' | null = null;

  exportEnCours = false;
  erreurExport: string | null = null;
  dernierSceau: string | null = null;

  private readonly appliques = new BehaviorSubject<PlatformAuditFilter>({});
  private readonly pagination = new BehaviorSubject<{ index: number; taille: number }>(
    { index: 0, taille: 50 });

  constructor(private readonly svc: PlatformAuditService) {}

  ngOnInit(): void {
    const page$ = combineLatest([this.appliques, this.pagination]).pipe(
      tap(() => { this.erreur = null; this.chargementEnCours.next(true); }),
      switchMap(([filtre, p]) => this.svc.list(filtre, p.index, p.taille).pipe(
        catchError((err: unknown) => {
          this.erreur = safeErrorMessage(err,
            $localize`:@@platform.audit.load-failed:La piste d'audit n'a pas pu être chargée.`);
          return of(PAGE_VIDE);
        }),
        finalize(() => this.chargementEnCours.next(false))
      )),
      // refCount:false : le tableau, le compteur et le bloc d'intégrité
      // s'abonnent séparément, certains derrière un *ngIf — un abonnement
      // tardif ne doit pas relancer une requête sur un journal volumineux.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.actes$ = page$.pipe(map(p => p.content));
    this.total$ = page$.pipe(map(p => p.totalElements));
    this.bornes$ = page$.pipe(map(p => this.bornesDe(p.content)));
    this.critere$ = this.appliques.pipe(map(f => this.svc.effectiveCriterion(f)));
  }

  // ---- filtres ---------------------------------------------------------------

  get filtreActif(): boolean {
    return Object.values(this.filtres.getRawValue()).some(valeur => !!valeur);
  }

  appliquer(): void {
    if (this.filtres.invalid) {
      this.filtres.markAllAsTouched();
      return;
    }
    this.fermerDetail();
    this.indexDePage = 0;
    this.pagination.next({ index: 0, taille: this.taillePage });
    this.appliques.next(this.construireFiltre());
  }

  reinitialiser(): void {
    this.filtres.reset();
    this.fermerDetail();
    this.indexDePage = 0;
    this.pagination.next({ index: 0, taille: this.taillePage });
    this.appliques.next({});
  }

  private construireFiltre(): PlatformAuditFilter {
    const brut = this.filtres.getRawValue();
    const filtre: PlatformAuditFilter = {};
    if (brut.action) filtre.action = brut.action;
    if (brut.targetTenantId) filtre.targetTenantId = brut.targetTenantId.trim();
    if (brut.actorUserId) filtre.actorUserId = brut.actorUserId.trim();
    const depuis = this.versInstant(brut.from);
    const jusqua = this.versInstant(brut.to);
    if (depuis) filtre.from = depuis;
    if (jusqua) filtre.to = jusqua;
    return filtre;
  }

  /**
   * Un champ `datetime-local` donne une heure locale sans fuseau
   * (« 2026-11-08T08:00 ») que `Instant` refuserait : on la convertit en
   * instant absolu.
   */
  private versInstant(local: string): string | undefined {
    if (!local) return undefined;
    const date = new Date(local);
    return isNaN(date.getTime()) ? undefined : date.toISOString();
  }

  /** Le critère que le serveur retiendra réellement, dit en clair. */
  libelleDuCritere(critere: PlatformAuditCriterion): string {
    switch (critere) {
      case 'tenant':
        return $localize`:@@platform.audit.criterion-tenant:Filtre appliqué : le client visé. Les autres critères sont ignorés par le serveur.`;
      case 'action':
        return $localize`:@@platform.audit.criterion-action:Filtre appliqué : le type d'acte. Les autres critères sont ignorés par le serveur.`;
      case 'actor':
        return $localize`:@@platform.audit.criterion-actor:Filtre appliqué : l'auteur. Les autres critères sont ignorés par le serveur.`;
      case 'period':
        return $localize`:@@platform.audit.criterion-period:Filtre appliqué : la période. Renseignez les deux bornes pour qu'elle soit prise en compte.`;
      default:
        return $localize`:@@platform.audit.criterion-none:Aucun filtre actif : toute la piste d'audit est affichée, du plus récent au plus ancien.`;
    }
  }

  surChangementDePage(evenement: PageEvent): void {
    this.indexDePage = Math.max(0, evenement.pageIndex);
    this.taillePage = Math.min(TAILLE_MAXIMALE, Math.max(1, evenement.pageSize));
    this.fermerDetail();
    this.pagination.next({ index: this.indexDePage, taille: this.taillePage });
  }

  // ---- détail ----------------------------------------------------------------

  /**
   * Le détail est ouvert depuis la ligne déjà chargée, sans second appel.
   *
   * <p>Différence assumée avec l'écran jumeau côté client, qui relit l'acte
   * pour obtenir son contenu : ici, la liste rend déjà tout — y compris le
   * détail, l'origine et les empreintes. Un appel de plus n'apporterait rien,
   * et le serveur n'expose d'ailleurs pas d'accès à l'unité : une piste d'audit
   * se lit par fenêtres, pas acte par acte.
   */
  ouvrirDetail(acte: PlatformAuditEvent): void {
    this.detail = acte;
  }

  fermerDetail(): void {
    this.detail = null;
  }

  /** Indente le détail JSON quand il est lisible ; sinon le rend tel quel. */
  detailLisible(contenu: string | null): string {
    if (!contenu) return '';
    try {
      return JSON.stringify(JSON.parse(contenu), null, 2);
    } catch {
      return contenu;
    }
  }

  // ---- intégrité (lecture seule) ---------------------------------------------

  private bornesDe(actes: PlatformAuditEvent[]): BornesDeSequence | null {
    if (!actes.length) return null;
    const numeros = actes.map(acte => acte.sequenceNo);
    return { from: Math.min(...numeros), to: Math.max(...numeros) };
  }

  /** Vérifie la totalité du journal : c'est la vérification qui compte. */
  verifierTout(): void {
    this.verifier(undefined, undefined, 'totalite');
  }

  /** Vérifie la fenêtre visible : un clic, zéro saisie. */
  verifierLaPage(bornes: BornesDeSequence): void {
    this.verifier(bornes.from, bornes.to, 'page');
  }

  private verifier(depuis: number | undefined, jusqua: number | undefined,
                   fenetre: 'totalite' | 'page'): void {
    if (this.verificationEnCours) return;
    this.verificationEnCours = true;
    this.erreurDeVerification = null;
    this.verdict = null;
    this.fenetreDuVerdict = null;
    this.svc.verify(depuis, jusqua).pipe(
      finalize(() => { this.verificationEnCours = false; })
    ).subscribe({
      next: verdict => { this.verdict = verdict; this.fenetreDuVerdict = fenetre; },
      error: (err: unknown) => {
        this.erreurDeVerification = safeErrorMessage(err,
          $localize`:@@platform.audit.verify-failed:La vérification d'intégrité a échoué.`);
      }
    });
  }

  // ---- export ----------------------------------------------------------------

  /**
   * Télécharge le dossier scellé.
   *
   * <p>Le fichier est fabriqué dans le navigateur à partir de la réponse, et non
   * ouvert par une navigation vers l'adresse d'export : cette adresse exige le
   * jeton, qu'une navigation de page ne porte pas. Elle rendrait un 401 affiché
   * en pleine page, sur le geste même qui doit produire une preuve.
   */
  exporter(): void {
    if (this.exportEnCours) return;
    this.exportEnCours = true;
    this.erreurExport = null;
    this.svc.export().pipe(
      finalize(() => { this.exportEnCours = false; })
    ).subscribe({
      next: dossier => {
        this.dernierSceau = dossier.seal;
        const nom = `qualitos-piste-audit-${dossier.fromSeq}-${dossier.toSeq}`
          + `-${dossier.seal.slice(0, 12)}.json`;
        this.telecharger(JSON.stringify(dossier, null, 2), nom);
      },
      error: (err: unknown) => {
        this.erreurExport = safeErrorMessage(err,
          $localize`:@@platform.audit.export-failed:Le dossier d'audit n'a pas pu être exporté.`);
      }
    });
  }

  private telecharger(contenu: string, nom: string): void {
    const bloc = new Blob([contenu], { type: 'application/json' });
    const adresse = URL.createObjectURL(bloc);
    const lien = document.createElement('a');
    lien.href = adresse;
    lien.download = nom;
    lien.click();
    // Sans révocation, chaque export laisse le dossier entier en mémoire pour
    // la durée de vie de l'onglet.
    URL.revokeObjectURL(adresse);
  }

  // ---- présentation ----------------------------------------------------------

  libelleDeLActe(action: PlatformAuditAction): string {
    // Un acte absent de la table s'affiche sous son code : montrer le code brut
    // plutôt que rien, c'est ce qui rend l'oubli visible.
    return LIBELLES_DES_ACTES[action] ?? action;
  }

  tonDeLAuteur(type: PlatformActorType): 'neutral' | 'accent' {
    return type === 'USER' ? 'accent' : 'neutral';
  }

  /** Un SHA-256 complet noierait la cellule : on montre une amorce lisible. */
  empreinteCourte(empreinte: string | null): string {
    if (!empreinte) return '—';
    return empreinte.length > 12 ? empreinte.slice(0, 12) + '…' : empreinte;
  }

  estSelectionne(acte: PlatformAuditEvent): boolean {
    return this.detail?.id === acte.id;
  }

  suivreParSequence(_index: number, acte: PlatformAuditEvent): number {
    return acte.sequenceNo;
  }

  suivreParRupture(index: number, rupture: { sequenceNo: number }): string {
    return `${rupture.sequenceNo}-${index}`;
  }
}
