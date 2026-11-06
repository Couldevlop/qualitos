import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../core/http/error-message';
import { PartnerService } from '../../../core/partner/partner.service';
import { PartnerEngagement } from '../../../core/partner/partner.types';

/**
 * Le portefeuille d'un consultant : les clients où il peut entrer aujourd'hui.
 *
 * <p><b>Pourquoi la liste est si courte.</b> Elle ne contient que des accès
 * <b>ouverts ce jour</b>. Un accord proposé mais pas encore accordé n'y figure
 * pas : le cabinet apprendrait sinon, en consultant son propre écran, la liste
 * des clients à qui l'éditeur l'a proposé et qui n'ont pas répondu — une
 * information qui appartient au client, pas au cabinet. De même, un accord
 * arrivé à son terme disparaît sans qu'aucune tâche de fond n'ait à passer :
 * c'est la date qui ferme l'accès, pas un traitement nocturne.
 *
 * <p><b>Le rattachement.</b> À l'ouverture, la console demande au serveur de
 * rattacher le compte connecté à la fiche que son cabinet a inscrite, par
 * courriel. L'appel est idempotent et ne crée rien : une personne dont aucune
 * fiche ne porte le courriel reste sans portefeuille, et c'est à son cabinet de
 * l'inscrire. C'est voulu — cette route ne doit pas pouvoir servir à s'inviter
 * dans un cabinet.
 *
 * <p>L'écran n'écrit rien d'autre. Un consultant ne décide ni de son cabinet,
 * ni de ses accès : les deux lui sont donnés, par l'éditeur et par le client.
 */
@Component({
  selector: 'qos-partner-portfolio',
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.scss'],
  standalone: false
})
export class PortfolioComponent implements OnInit {

  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  readonly loading$: Observable<boolean> = this.loadingSubject.asObservable();

  engagements: PartnerEngagement[] = [];
  error: string | null = null;

  /**
   * Vrai quand le compte n'est rattaché à aucune fiche de consultant. Ce n'est
   * pas une panne : c'est un rattachement qui reste à faire par le cabinet, et
   * le message doit le dire pour que la personne sache à qui s'adresser.
   */
  unlinked = false;

  constructor(private readonly partner: PartnerService) {}

  ngOnInit(): void {
    // Le rattachement d'abord : sans lui, le portefeuille d'un compte qui se
    // connecte pour la première fois serait vide alors que son cabinet l'a bien
    // inscrit, et la personne conclurait qu'on ne lui a rien accordé.
    this.loadingSubject.next(true);
    this.partner.linkAccount().subscribe({
      next: fiche => {
        this.unlinked = fiche === null;
        this.reload();
      },
      error: (err: unknown) => {
        this.loadingSubject.next(false);
        this.error = safeErrorMessage(err,
          $localize`:@@partner.portfolio.link-failed:Votre compte n’a pas pu être rattaché à votre cabinet.`);
      }
    });
  }

  reload(): void {
    this.error = null;
    this.loadingSubject.next(true);
    this.partner.myPortfolio()
      .pipe(finalize(() => this.loadingSubject.next(false)))
      .subscribe({
        next: accords => this.engagements = accords,
        error: (err: unknown) => {
          this.engagements = [];
          this.error = safeErrorMessage(err,
            $localize`:@@partner.portfolio.load-failed:Votre portefeuille n’a pas pu être chargé.`);
        }
      });
  }

  /** Ce que le cabinet a le droit de faire chez ce client, en clair. */
  scopeLabel(engagement: PartnerEngagement): string {
    return engagement.scope === 'INTERVENE'
      ? $localize`:@@partner.scope.intervene:Intervention`
      : $localize`:@@partner.scope.read:Consultation`;
  }
}
