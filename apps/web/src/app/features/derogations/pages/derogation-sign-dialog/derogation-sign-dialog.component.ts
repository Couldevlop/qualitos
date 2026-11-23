import { AfterViewInit, Component, ElementRef, Inject, ViewChild } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface DerogationSignDialogData {
  reference: string;
  /** Le nom du signataire, pré-rempli depuis l'annuaire. Modifiable. */
  nomPropose: string;
}

export interface DerogationSignDialogResult {
  signedByName: string;
  /** Le tracé en PNG, préfixe `data:` compris : le serveur le retire. */
  signaturePngBase64: string;
}

/** La zone de tracé, en points par pouce de l'écran. */
const LARGEUR = 520;
const HAUTEUR = 180;

/**
 * La signature du manager qualité.
 *
 * Un nom ET un tracé. Le nom seul ne distingue pas une signature d'une saisie
 * par un tiers ; le tracé seul ne dit pas qui a signé — c'est le couple que le
 * serveur exige, et que le PDF reproduit.
 *
 * Le tracé se fait à la souris, au doigt ou au stylet : les événements de
 * pointeur couvrent les trois, là où `mousedown` laisserait les tablettes de
 * côté, et le terrain travaille sur tablette.
 */
@Component({
  selector: 'qos-derogation-sign-dialog',
  templateUrl: './derogation-sign-dialog.component.html',
  styleUrls: ['./derogation-sign-dialog.component.scss'],
  standalone: false
})
export class DerogationSignDialogComponent implements AfterViewInit {

  @ViewChild('zone') private zone?: ElementRef<HTMLCanvasElement>;

  readonly nom: FormControl<string>;

  /** Vrai dès qu'un trait a été posé : sans trait, on ne signe pas. */
  tracee = false;

  private contexte: CanvasRenderingContext2D | null = null;
  private enCours = false;

  constructor(
    private readonly dialogRef: MatDialogRef<
      DerogationSignDialogComponent, DerogationSignDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: DerogationSignDialogData
  ) {
    this.nom = new FormControl(data.nomPropose ?? '',
      { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] });
  }

  ngAfterViewInit(): void {
    const toile = this.zone?.nativeElement;
    if (!toile) {
      return;
    }
    // La résolution suit celle de l'écran : sur un affichage dense, un tracé
    // rendu à une seule unité par point sortirait flou dans le PDF.
    const densite = window.devicePixelRatio || 1;
    toile.width = LARGEUR * densite;
    toile.height = HAUTEUR * densite;
    const ctx = toile.getContext('2d');
    if (!ctx) {
      return;
    }
    ctx.scale(densite, densite);
    // Un fond BLANC et non transparent : le PNG part dans un PDF, et un fond
    // transparent y rendrait le trait invisible selon la visionneuse.
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, LARGEUR, HAUTEUR);
    ctx.strokeStyle = '#111111';
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    this.contexte = ctx;
  }

  commencer(evenement: PointerEvent): void {
    if (!this.contexte) {
      return;
    }
    evenement.preventDefault();
    this.enCours = true;
    const point = this.point(evenement);
    this.contexte.beginPath();
    this.contexte.moveTo(point.x, point.y);
    // Un point posé sans glisser est déjà un trait : sans cela, une signature
    // réduite à un point ne serait jamais reconnue.
    this.contexte.lineTo(point.x, point.y);
    this.contexte.stroke();
    this.tracee = true;
  }

  continuer(evenement: PointerEvent): void {
    if (!this.enCours || !this.contexte) {
      return;
    }
    evenement.preventDefault();
    const point = this.point(evenement);
    this.contexte.lineTo(point.x, point.y);
    this.contexte.stroke();
  }

  terminer(): void {
    this.enCours = false;
  }

  effacer(): void {
    const toile = this.zone?.nativeElement;
    if (!toile || !this.contexte) {
      return;
    }
    this.contexte.fillStyle = '#ffffff';
    this.contexte.fillRect(0, 0, LARGEUR, HAUTEUR);
    this.contexte.strokeStyle = '#111111';
    this.tracee = false;
  }

  signer(): void {
    const toile = this.zone?.nativeElement;
    if (!toile || this.nom.invalid || !this.tracee) {
      this.nom.markAsTouched();
      return;
    }
    this.dialogRef.close({
      signedByName: this.nom.value.trim(),
      signaturePngBase64: toile.toDataURL('image/png')
    });
  }

  annuler(): void {
    this.dialogRef.close();
  }

  /** La position du pointeur dans la toile, et non dans la page. */
  private point(evenement: PointerEvent): { x: number; y: number } {
    const cadre = (evenement.target as HTMLCanvasElement).getBoundingClientRect();
    return {
      x: ((evenement.clientX - cadre.left) / cadre.width) * LARGEUR,
      y: ((evenement.clientY - cadre.top) / cadre.height) * HAUTEUR
    };
  }
}
