import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

import { AuditsService } from '../../../audits/audits.service';
import { DocumentsService } from '../../../documents/documents.service';
import { NcService } from '../../../nc/nc.service';
import { RiskRegisterService } from '../../../risk-register/risk-register.service';
import { RequirementLink } from '../../stakeholders.types';

export interface StakeholderLinkDialogData {
  kind: RequirementLink;
  /** L'exigence qu'on rattache, rappelée en sous-titre. */
  requirement: string;
}

/** Une cible possible : son identifiant, et ce qu'un humain lit pour la reconnaître. */
export interface LinkTarget {
  id: string;
  label: string;
}

/** Combien de cibles on va chercher : au-delà, le filtre fait le travail. */
const PAGE = 200;

/**
 * Rattacher une exigence à un objet d'un autre module : un risque, un écart, un
 * audit, un document.
 *
 * <p>La cible se **choisit dans une liste filtrable**, jamais par un champ
 * d'identifiant libre : personne ne connaît par cœur l'UUID d'un risque, et un
 * identifiant tapé de travers fabriquerait un rattachement qui ne mène nulle
 * part.
 *
 * <p>La fenêtre ne rattache pas elle-même : elle rend l'identifiant choisi, et
 * la fiche appelle le serveur — c'est elle qui tient l'exigence et sa liste.
 */
@Component({
  selector: 'qos-stakeholder-link-dialog',
  templateUrl: './stakeholder-link-dialog.component.html',
  styleUrls: ['./stakeholder-link-dialog.component.scss'],
  standalone: false
})
export class StakeholderLinkDialogComponent implements OnInit {

  loading = true;
  /** Renseigné quand la liste n'a pas pu être chargée : on le dit, et on annule. */
  failed = false;

  targets: LinkTarget[] = [];

  readonly form = this.fb.nonNullable.group({
    filter: [''],
    target: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly risks: RiskRegisterService,
    private readonly ncs: NcService,
    private readonly audits: AuditsService,
    private readonly documents: DocumentsService,
    private readonly dialogRef: MatDialogRef<StakeholderLinkDialogComponent, string>,
    @Inject(MAT_DIALOG_DATA) public readonly data: StakeholderLinkDialogData
  ) {}

  ngOnInit(): void {
    this.load().pipe(
      catchError(() => {
        this.failed = true;
        return of([] as LinkTarget[]);
      }),
      finalize(() => (this.loading = false))
    ).subscribe(targets => (this.targets = targets));
  }

  get title(): string {
    switch (this.data.kind) {
      case 'RISK':
        return $localize`:@@stakeholders.link.title-risk:Rattacher à un risque`;
      case 'NON_CONFORMITY':
        return $localize`:@@stakeholders.link.title-nc:Rattacher à un écart`;
      case 'AUDIT':
        return $localize`:@@stakeholders.link.title-audit:Rattacher à un audit`;
      default:
        return $localize`:@@stakeholders.link.title-document:Rattacher à un document`;
    }
  }

  get subtitle(): string {
    return $localize`:@@stakeholders.link.subtitle:Exigence : ${this.data.requirement}:exigence:`;
  }

  /** Le libellé du choix, dans les mots du module visé. */
  get targetLabel(): string {
    switch (this.data.kind) {
      case 'RISK':
        return $localize`:@@stakeholders.link.risk:Risque`;
      case 'NON_CONFORMITY':
        return $localize`:@@stakeholders.link.nc:Écart`;
      case 'AUDIT':
        return $localize`:@@stakeholders.link.audit:Audit`;
      default:
        return $localize`:@@stakeholders.link.document:Document`;
    }
  }

  /** Les cibles qui correspondent au filtre tapé, accents et casse ignorés. */
  get filtered(): LinkTarget[] {
    const needle = this.normalize(this.form.controls.filter.value);
    if (!needle) {
      return this.targets;
    }
    return this.targets.filter(t => this.normalize(t.label).includes(needle));
  }

  get blockedReason(): string | undefined {
    if (this.failed) {
      return $localize`:@@stakeholders.link.load-failed:La liste n'a pas pu être chargée ; fermez la fenêtre et réessayez.`;
    }
    if (this.loading) {
      return $localize`:@@stakeholders.link.loading:Chargement de la liste…`;
    }
    if (!this.targets.length) {
      return $localize`:@@stakeholders.link.none:Il n'y a rien à rattacher : ce module ne contient encore aucun élément.`;
    }
    return $localize`:@@stakeholders.link.choose:Choisissez la cible dans la liste.`;
  }

  submit(): void {
    const target = this.form.controls.target.value;
    if (!target) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(target);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  // ---------- internes ----------

  /**
   * Va chercher la liste du module visé. Chaque module a sa propre méthode de
   * liste et sa propre façon de nommer ses objets : la conversion en couple
   * « identifiant, libellé » se fait ici, une fois.
   */
  private load(): Observable<LinkTarget[]> {
    switch (this.data.kind) {
      case 'RISK':
        return this.risks.risks().pipe(
          map(rows => rows.map(r => ({ id: r.id, label: `${r.reference} — ${r.title}` }))));
      case 'NON_CONFORMITY':
        return this.ncs.listNcs(0, PAGE).pipe(
          map(page => page.content.map(n => ({ id: n.id, label: `${n.reference} — ${n.title}` }))));
      case 'AUDIT':
        return this.audits.listPlans(0, PAGE).pipe(
          map(page => page.content.map(a => ({ id: a.id, label: `${a.reference} — ${a.title}` }))));
      default:
        return this.documents.list(0, PAGE).pipe(
          map(page => page.content.map(d => ({ id: d.id, label: `${d.code} — ${d.title}` }))));
    }
  }

  private normalize(value: string): string {
    return value.trim().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }
}
