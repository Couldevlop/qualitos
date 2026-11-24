import { Component, OnInit } from '@angular/core';
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  DECIDABLE, DURATION_ORDER, OCCURRENCE_ORDER, ORIGIN_ORDER, SHIPMENT_ORDER,
  decisionLabel, durationLabel, occurrenceLabel, originLabel, shipmentLabel,
  statusLabel, statusTone
} from '../../derogations.labels';
import { DerogationsService } from '../../derogations.service';
import {
  CustomerDecision, Derogation, DerogationSaveInput
} from '../../derogations.types';
import {
  DerogationSendBackDialogComponent, DerogationSendBackDialogData
} from '../derogation-send-back-dialog/derogation-send-back-dialog.component';
import {
  DerogationSendDialogComponent, DerogationSendDialogData, DerogationSendDialogResult
} from '../derogation-send-dialog/derogation-send-dialog.component';
import {
  DerogationSignDialogComponent, DerogationSignDialogData, DerogationSignDialogResult
} from '../derogation-sign-dialog/derogation-sign-dialog.component';

/** Une ligne d'écart : ce qui était attendu, ce qui a été mesuré. */
function ligneVide(): FormGroup {
  return new FormGroup({
    specification: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(500)] }),
    actualValue: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(500)] }),
    nonConformingQty: new FormControl<number | null>(null, { validators: [Validators.min(0)] })
  });
}

/**
 * Le dossier de dérogation client, ses six sections.
 *
 * <p>QUI PEUT QUOI VIENT DU SERVEUR, et de lui seul : `editable` dit si la
 * fiche se modifie, et l'agrégat vérifie l'état ET l'acteur à chaque geste.
 * L'écran masque donc un bouton pour ne pas tendre un piège, jamais pour tenir
 * une règle — un bouton caché n'a jamais protégé une donnée.
 *
 * <p>Le brouillon s'enregistre INCOMPLET autant de fois qu'on veut : aucun
 * champ n'est requis ici. Les six champs obligatoires ne sont exigés qu'à la
 * demande de signature, et c'est le serveur qui les contrôle — les redire ici
 * ferait deux règles à tenir d'accord, et c'est toujours la copie qui dérive.
 */
@Component({
  selector: 'qos-derogation-form',
  templateUrl: './derogation-form.component.html',
  styleUrls: ['./derogation-form.component.scss'],
  standalone: false
})
export class DerogationFormComponent implements OnInit {

  readonly etapes = SHIPMENT_ORDER;
  readonly origines = ORIGIN_ORDER;
  readonly occurrences = OCCURRENCE_ORDER;
  readonly durees = DURATION_ORDER;
  readonly decisions = DECIDABLE;

  readonly statusLabel = statusLabel;
  readonly statusTone = statusTone;
  readonly shipmentLabel = shipmentLabel;
  readonly originLabel = originLabel;
  readonly occurrenceLabel = occurrenceLabel;
  readonly durationLabel = durationLabel;
  readonly decisionLabel = decisionLabel;

  readonly formulaire = new FormGroup({
    customerName: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(200)] }),
    customerQualityContact: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(200)] }),
    customerOrderNo: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(100)] }),
    partReference: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(200)] }),
    partIndex: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(40)] }),
    batchNumber: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(100)] }),
    quantity: new FormControl<number | null>(null, { validators: [Validators.min(0)] }),
    requestDate: new FormControl<Date | null>(null),

    shipmentStage: new FormControl<'TO_SHIP' | 'ALREADY_SHIPPED'>('TO_SHIP', { nonNullable: true }),
    deviationOrigin: new FormControl<'PRODUCT' | 'PROCESS'>('PRODUCT', { nonNullable: true }),
    occurrence: new FormControl<'FIRST' | 'REPEAT'>('FIRST', { nonNullable: true }),
    durationKind: new FormControl<'TEMPORARY' | 'PERMANENT'>('TEMPORARY', { nonNullable: true }),
    durationDetail: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(300)] }),

    lines: new FormArray<FormGroup>([]),
    justification: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(20000)] }),

    rootCause: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    containment: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    correctiveAction: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    capaReference: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(200)] }),
    attachmentsNote: new FormControl<string>('',
      { nonNullable: true, validators: [Validators.maxLength(500)] })
  });

  /** Le titre d'un dossier qui n'a pas encore de référence. */
  readonly nouveauTitre = $localize`:@@derogation.form.new-title:Nouvelle dérogation`;

  /**
   * Le manager désigné, par son identifiant de compte.
   *
   * <p>C'est un champ de formulaire et non une variable à part, pour une raison
   * précise : il doit se REMPLIR depuis la fiche. Il ne vivait que dans l'écran,
   * si bien qu'une réouverture — qui recharge la fiche — laissait le champ vide
   * alors que le serveur avait gardé le manager, et qu'il fallait le redésigner
   * pour rien. Le domaine est pourtant clair : « la signature est effacée, le
   * manager désigné reste. »
   */
  readonly managerCtrl = new FormControl<string | null>(null);

  /** La réponse du client, saisie par l'initiateur à sa réception. */
  readonly reponseClient = new FormGroup({
    decision: new FormControl<CustomerDecision | null>(null, { validators: [Validators.required] }),
    decidedOn: new FormControl<Date | null>(null, { validators: [Validators.required] })
  });

  dossier?: Derogation;
  /** Vrai sur `/derogations/nouvelle` : rien n'existe encore côté serveur. */
  neuf = false;
  chargement = false;
  envoiEnCours = false;
  echec = false;

  /** Mon compte, pour savoir si je suis l'initiateur ou le manager désigné. */
  private readonly moi: string | null;

  constructor(
    private readonly service: DerogationsService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.moi = auth.snapshot()?.userId ?? null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || id === 'nouvelle') {
      this.neuf = true;
      this.lignes.push(ligneVide());
      // « Pré-remplie à l'ouverture du dossier », dit l'aide du champ, et le
      // domaine le confirme : « le formulaire la propose pré-remplie ». Elle
      // arrivait pourtant vide, et la promesse démentait l'écran. Ici
      // seulement : sur un dossier qui existe, c'est SA date qui fait foi.
      const aujourdhui = new Date();
      this.formulaire.patchValue({
        requestDate: new Date(aujourdhui.getFullYear(), aujourdhui.getMonth(), aujourdhui.getDate())
      }, { emitEvent: false });
      return;
    }
    this.charger(id);
  }

  get lignes(): FormArray<FormGroup> {
    return this.formulaire.controls.lines;
  }

  private charger(id: string): void {
    this.chargement = true;
    this.echec = false;
    this.service.get(id).subscribe({
      next: d => {
        this.appliquer(d);
        this.chargement = false;
      },
      error: erreur => {
        this.chargement = false;
        this.echec = true;
        this.snack.open(safeErrorMessage(erreur,
          $localize`:@@derogation.error.load:Ce dossier de dérogation n'a pas pu être lu.`),
          undefined, { duration: 6000 });
      }
    });
  }

  /** La fiche rendue par le serveur devient l'état de l'écran, et pas l'inverse. */
  private appliquer(d: Derogation): void {
    this.dossier = d;
    this.formulaire.patchValue({
      customerName: d.customerName ?? '',
      customerQualityContact: d.customerQualityContact ?? '',
      customerOrderNo: d.customerOrderNo ?? '',
      partReference: d.partReference ?? '',
      partIndex: d.partIndex ?? '',
      batchNumber: d.batchNumber ?? '',
      quantity: d.quantity,
      requestDate: enDateLocale(d.requestDate),
      shipmentStage: d.shipmentStage,
      deviationOrigin: d.deviationOrigin,
      occurrence: d.occurrence,
      durationKind: d.durationKind,
      durationDetail: d.durationDetail ?? '',
      justification: d.justification ?? '',
      rootCause: d.rootCause ?? '',
      containment: d.containment ?? '',
      correctiveAction: d.correctiveAction ?? '',
      capaReference: d.capaReference ?? '',
      attachmentsNote: d.attachmentsNote ?? ''
    }, { emitEvent: false });

    // Le manager désigné revient avec la fiche : sans cela, rouvrir un dossier
    // vidait le champ et obligeait à redésigner quelqu'un que le serveur
    // connaissait déjà.
    this.managerCtrl.setValue(d.managerAccountId ?? null, { emitEvent: false });

    this.lignes.clear({ emitEvent: false });
    for (const l of d.lines) {
      const g = ligneVide();
      g.patchValue({
        specification: l.specification ?? '',
        actualValue: l.actualValue ?? '',
        nonConformingQty: l.nonConformingQty
      }, { emitEvent: false });
      this.lignes.push(g, { emitEvent: false });
    }
    // Une ligne vide à montrer quand le dossier n'en porte aucune : un tableau
    // sans ligne ne dit pas qu'on peut en ajouter.
    if (this.lignes.length === 0) {
      this.lignes.push(ligneVide(), { emitEvent: false });
    }

    if (d.editable) {
      this.formulaire.enable({ emitEvent: false });
    } else {
      this.formulaire.disable({ emitEvent: false });
    }
  }

  // ---------- droits d'écran, qui ne remplacent pas ceux du serveur ----------

  get modifiable(): boolean {
    return this.neuf || !!this.dossier?.editable;
  }

  get jeSuisLInitiateur(): boolean {
    return !!this.moi && !!this.dossier && this.dossier.createdByUserId === this.moi;
  }

  get jeSuisLeManager(): boolean {
    return !!this.moi && !!this.dossier && this.dossier.managerAccountId === this.moi;
  }

  get peutDemanderLaValidation(): boolean {
    const s = this.dossier?.status;
    return this.jeSuisLInitiateur && (s === 'DRAFT' || s === 'TO_AMEND');
  }

  get peutRetirer(): boolean {
    return this.jeSuisLInitiateur && this.dossier?.status === 'PENDING_INTERNAL';
  }

  get peutSigner(): boolean {
    return this.jeSuisLeManager && this.dossier?.status === 'PENDING_INTERNAL';
  }

  get peutRouvrir(): boolean {
    return (this.jeSuisLInitiateur || this.jeSuisLeManager)
      && this.dossier?.status === 'QUALITY_APPROVED';
  }

  get peutEnvoyer(): boolean {
    return this.jeSuisLInitiateur && this.dossier?.status === 'QUALITY_APPROVED';
  }

  get peutRepondrePourLeClient(): boolean {
    return this.jeSuisLInitiateur && this.dossier?.status === 'PENDING_CUSTOMER';
  }

  // ---------- gestes ----------

  enregistrer(): void {
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const saisie = this.saisie();
    const appel = this.dossier
      ? this.service.update(this.dossier.id, saisie)
      : this.service.create(saisie);
    this.agir(appel, $localize`:@@derogation.saved:Dossier enregistré.`, true);
  }

  surChoixDuManager(personne: DirectoryUser | null): void {
    this.managerCtrl.setValue(personne?.accountId ?? null);
  }

  demanderLaValidation(): void {
    const manager = this.managerCtrl.value;
    if (!this.dossier || !manager) {
      return;
    }
    this.agir(this.service.requestApproval(this.dossier.id, manager),
      $localize`:@@derogation.approval-requested:Demande envoyée au manager qualité.`);
  }

  retirer(): void {
    if (!this.dossier) {
      return;
    }
    this.agir(this.service.withdraw(this.dossier.id),
      $localize`:@@derogation.withdrawn:Demande retirée, le dossier repasse en brouillon.`);
  }

  /** Le renvoi exige un motif : sans lui, l'initiateur ne sait pas quoi corriger. */
  renvoyerPourCorrection(): void {
    const d = this.dossier;
    if (!d) {
      return;
    }
    this.dialog
      .open<DerogationSendBackDialogComponent, DerogationSendBackDialogData, string>(
        DerogationSendBackDialogComponent,
        { data: { reference: d.reference }, panelClass: 'qos-dialog-panel' })
      .afterClosed()
      .subscribe(motif => {
        if (motif) {
          this.agir(this.service.sendBack(d.id, motif),
            $localize`:@@derogation.sent-back:Dossier renvoyé pour correction.`);
        }
      });
  }

  signer(): void {
    const d = this.dossier;
    if (!d) {
      return;
    }
    const donnees: DerogationSignDialogData = {
      reference: d.reference,
      nomPropose: d.managerAccountName ?? ''
    };
    this.dialog
      .open<DerogationSignDialogComponent, DerogationSignDialogData, DerogationSignDialogResult>(
        DerogationSignDialogComponent, { data: donnees, panelClass: 'qos-dialog-panel' })
      .afterClosed()
      .subscribe(resultat => {
        if (resultat) {
          this.agir(this.service.approve(d.id, resultat),
            $localize`:@@derogation.approved:Dossier approuvé et signé.`);
        }
      });
  }

  /**
   * Rouvrir annule la signature, et ne vaut QUE tant que le dossier n'est pas
   * parti. La confirmation le dit : ce n'est pas une modification de plus,
   * c'est le retrait d'un engagement qu'un manager a signé.
   */
  rouvrir(): void {
    const d = this.dossier;
    if (!d) {
      return;
    }
    const donnees: ConfirmDialogData = {
      title: $localize`:@@derogation.reopen.title:Rouvrir le dossier`,
      message: $localize`:@@derogation.reopen.message:La signature sera annulée et le dossier repassera en brouillon. Ce n'est possible que parce qu'il n'est pas encore parti au client.`,
      confirmLabel: $localize`:@@derogation.reopen.confirm:Rouvrir`,
      destructive: true
    };
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(
        ConfirmDialogComponent, { data: donnees, panelClass: 'qos-dialog-panel' })
      .afterClosed()
      .subscribe(oui => {
        if (oui) {
          this.agir(this.service.reopen(d.id),
            $localize`:@@derogation.reopened:Dossier rouvert, signature annulée.`);
        }
      });
  }

  envoyerAuClient(): void {
    const d = this.dossier;
    if (!d) {
      return;
    }
    const donnees: DerogationSendDialogData = {
      reference: d.reference,
      // Le contact qualité du client est une PERSONNE, pas une adresse : le
      // proposer comme destinataire serait deviner. L'adresse se saisit.
      adresseProposee: d.customerEmail
    };
    this.dialog
      .open<DerogationSendDialogComponent, DerogationSendDialogData, DerogationSendDialogResult>(
        DerogationSendDialogComponent, { data: donnees, panelClass: 'qos-dialog-panel' })
      .afterClosed()
      .subscribe(resultat => {
        if (resultat) {
          this.agir(this.service.sendToCustomer(d.id, resultat),
            $localize`:@@derogation.sent:Dossier envoyé au client.`);
        }
      });
  }

  enregistrerLaReponseDuClient(): void {
    const d = this.dossier;
    if (!d || this.reponseClient.invalid) {
      this.reponseClient.markAllAsTouched();
      return;
    }
    const v = this.reponseClient.getRawValue();
    this.agir(this.service.recordCustomerDecision(d.id, {
      decision: v.decision as CustomerDecision,
      decidedOn: enDateIso(v.decidedOn) ?? ''
    }), $localize`:@@derogation.decision-recorded:Réponse du client enregistrée.`);
  }

  /** Le document, ouvert dans un onglet pour être relu AVANT de partir. */
  ouvrirLeDocument(): void {
    const d = this.dossier;
    if (!d) {
      return;
    }
    this.service.pdf(d.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        // L'adresse est libérée après l'ouverture : la garder fuirait une
        // référence par consultation, et le document reste ouvert sans elle.
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: erreur => this.snack.open(safeErrorMessage(erreur,
        $localize`:@@derogation.error.pdf:Le document n'a pas pu être produit.`),
        undefined, { duration: 6000 })
    });
  }

  retourAuRegistre(): void {
    void this.router.navigate(['/derogations']);
  }

  // ---------- lignes d'écart ----------

  ajouterUneLigne(): void {
    this.lignes.push(ligneVide());
  }

  retirerLaLigne(index: number): void {
    this.lignes.removeAt(index);
    if (this.lignes.length === 0) {
      this.lignes.push(ligneVide());
    }
  }

  // ---------- plomberie ----------

  /**
   * Un geste, un appel, une seule réponse : la fiche rendue remplace l'état de
   * l'écran. Relire par une seconde requête ferait voir un instant différent de
   * celui que le serveur vient de décider.
   */
  private agir(appel: Observable<Derogation>, succes: string, creation = false): void {
    this.envoiEnCours = true;
    appel.subscribe({
      next: d => {
        this.envoiEnCours = false;
        const etaitNeuf = this.neuf;
        this.neuf = false;
        this.appliquer(d);
        this.snack.open(succes, undefined, { duration: 4000 });
        // Une création change l'adresse : sans cela, un rechargement de la page
        // rouvrirait un formulaire vide et le dossier paraîtrait perdu.
        if (creation && etaitNeuf) {
          void this.router.navigate(['/derogations', d.id], { replaceUrl: true });
        }
      },
      error: erreur => {
        this.envoiEnCours = false;
        this.snack.open(safeErrorMessage(erreur,
          $localize`:@@derogation.error.action:L'opération n'a pas abouti. Le dossier est inchangé.`),
          undefined, { duration: 8000 });
      }
    });
  }

  /** La saisie, convertie pour le serveur : le vide devient absent. */
  private saisie(): DerogationSaveInput {
    const v = this.formulaire.getRawValue();
    const texte = (s: string): string | null => {
      const t = s.trim();
      return t ? t : null;
    };
    return {
      customerName: texte(v.customerName),
      customerQualityContact: texte(v.customerQualityContact),
      customerOrderNo: texte(v.customerOrderNo),
      partReference: texte(v.partReference),
      partIndex: texte(v.partIndex),
      batchNumber: texte(v.batchNumber),
      quantity: v.quantity,
      requestDate: enDateIso(v.requestDate),
      shipmentStage: v.shipmentStage,
      deviationOrigin: v.deviationOrigin,
      occurrence: v.occurrence,
      durationKind: v.durationKind,
      durationDetail: texte(v.durationDetail),
      // Une ligne entièrement vide n'est pas une ligne : la laisser passer
      // ferait figurer un écart sans contenu sur le document du client.
      lines: this.lignes.controls
        .map(g => g.getRawValue() as {
          specification: string; actualValue: string; nonConformingQty: number | null;
        })
        .filter(l => l.specification.trim() || l.actualValue.trim() || l.nonConformingQty !== null)
        .map(l => ({
          specification: texte(l.specification),
          actualValue: texte(l.actualValue),
          nonConformingQty: l.nonConformingQty
        })),
      justification: texte(v.justification),
      rootCause: texte(v.rootCause),
      containment: texte(v.containment),
      correctiveAction: texte(v.correctiveAction),
      capaReference: texte(v.capaReference),
      attachmentsNote: texte(v.attachmentsNote)
    };
  }
}

/**
 * Un `Date` du calendrier devient « aaaa-mm-jj » sur ses composantes LOCALES.
 *
 * Surtout pas `toISOString()`, qui repasse par UTC et recule d'un jour a l'est
 * de Greenwich : une demande du 25 serait datee du 24 sur le document du
 * client. Meme piege, meme parade que dans le dialogue de cout de la qualite.
 */
export function enDateIso(valeur: Date | string | null | undefined): string | null {
  if (typeof valeur === 'string') {
    return valeur.slice(0, 10) || null;
  }
  if (!(valeur instanceof Date) || Number.isNaN(valeur.getTime())) {
    return null;
  }
  const mois = String(valeur.getMonth() + 1).padStart(2, '0');
  const jour = String(valeur.getDate()).padStart(2, '0');
  return `${valeur.getFullYear()}-${mois}-${jour}`;
}

/** L'inverse, composante par composante : `new Date('2026-09-18')` serait lu en UTC. */
export function enDateLocale(valeur: string | null | undefined): Date | null {
  if (!valeur) {
    return null;
  }
  const p = /^(\d{4})-(\d{2})-(\d{2})/.exec(valeur);
  return p ? new Date(Number(p[1]), Number(p[2]) - 1, Number(p[3])) : null;
}
