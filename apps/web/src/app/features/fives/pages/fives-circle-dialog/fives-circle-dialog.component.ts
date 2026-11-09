import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CirclesService } from '../../../circles/circles.service';
import { CircleResponse } from '../../../circles/circles.types';
import { FiveSPillar, ProposeToCircleRequest } from '../../fives.types';

export interface FivesCircleDialogData {
  /** La zone auditée, rappelée : c'est elle que l'idée citera. */
  zone: string;
  /** Le pilier dont la note motive l'idée. */
  pillar: FiveSPillar;
  /** Son libellé, dans la langue de l'écran. */
  pillarLabel: string;
  /** Sa note, telle qu'elle a été relevée. */
  score: number;
  /** La remarque de l'auditeur, qui propose la description. */
  note?: string | null;
}

/** Ce que l'écran a retenu, tel que le serveur l'attend. */
export type FivesCircleChoice = ProposeToCircleRequest;

/**
 * Porter un pilier faible d'un audit 5S à un cercle de qualité.
 *
 * <p>Un audit dit ce qui ne va pas ; il ne dit pas comment y remédier. Le
 * cercle, lui, est fait pour ça : des gens du terrain qui cherchent ensemble.
 * Cette fenêtre fait le passage, et ne demande que ce que l'audit ne porte
 * pas : <b>quel cercle</b> prend le sujet.
 *
 * <p>Seuls les cercles actifs sont proposés : le serveur refuse les autres, et
 * un écran qui les offrirait ferait échouer la demande après coup. Quand aucun
 * cercle n'est actif, la fenêtre le dit plutôt que de montrer une liste vide.
 *
 * <p>L'origine n'est pas saisie : le serveur la construit à partir de l'audit,
 * de la zone, du pilier et de sa note. La fenêtre montre cependant ce qu'elle
 * reprend, pour que la personne voie sur quoi l'idée sera défendue en séance.
 */
@Component({
  selector: 'qos-fives-circle-dialog',
  templateUrl: './fives-circle-dialog.component.html',
  styleUrls: ['./fives-circle-dialog.component.scss'],
  standalone: false
})
export class FivesCircleDialogComponent implements OnInit {

  readonly circles$ = new BehaviorSubject<CircleResponse[]>([]);
  loading = true;
  loadError: string | null = null;

  readonly form = this.fb.nonNullable.group({
    circleId: ['', [Validators.required]],
    title: [FivesCircleDialogComponent.titleFor(this.data), [Validators.required, Validators.maxLength(255)]],
    description: [this.data.note?.trim() ?? '']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly circles: CirclesService,
    private readonly dialogRef: MatDialogRef<FivesCircleDialogComponent, FivesCircleChoice>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FivesCircleDialogData
  ) {}

  /** Ce qui est proposé, par défaut : améliorer ce pilier, dans cette zone. */
  static titleFor(data: FivesCircleDialogData): string {
    const titre = $localize`:@@fives.circle.title-default:Améliorer ${data.pillarLabel}:pillar: en zone ${data.zone}:zone:`;
    return titre.length > 255 ? titre.slice(0, 254) + '…' : titre;
  }

  ngOnInit(): void {
    // Un cercle en pause ou archivé ne prend pas d'idée : le serveur le refuse,
    // et la liste ne doit pas le proposer.
    this.circles.listCircles(0, 100, 'ACTIVE')
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: page => { this.circles$.next(page.content); this.loadError = null; },
        error: err => {
          this.loadError = safeErrorMessage(err,
            $localize`:@@fives.circle.load-failed:La liste des cercles n'a pas pu être chargée.`);
        }
      });
  }

  /** Vrai quand aucun cercle actif n'existe : la fenêtre le dit alors en clair. */
  get empty(): boolean {
    return !this.loading && !this.loadError && this.circles$.value.length === 0;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      circleId: v.circleId,
      pillar: this.data.pillar,
      title: v.title.trim(),
      description: v.description.trim() || undefined
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
