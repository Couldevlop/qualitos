import { NgModule } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaActionDialogComponent } from './pages/capa-action-dialog/capa-action-dialog.component';

/**
 * Le formulaire d'action CAPA, seul, pour que deux modules puissent l'ouvrir.
 *
 * <p><b>Pourquoi ce module minuscule existe.</b> « Ajouter une action » se fait
 * depuis deux endroits : la fiche CAPA, et la fiche de non-conformité, qui n'a
 * plus à « escalader » vers un dossier abstrait mais à poser directement
 * l'action qu'on a décidée. Les deux écrans doivent ouvrir LE MÊME formulaire —
 * deux formulaires jumeaux divergeraient au premier champ ajouté d'un seul côté.
 *
 * <p>Il n'était pas possible d'importer simplement {@link CapaModule} depuis le
 * module des non-conformités : celui-ci porte ses propres routes via
 * {@code RouterModule.forChild}, et les charger une seconde fois sous
 * {@code /nc} aurait greffé les écrans CAPA à une adresse qui n'est pas la
 * leur. Sortir le seul composant partagé dans son module évite ça, sans rien
 * dupliquer.
 */
@NgModule({
  declarations: [CapaActionDialogComponent],
  // MatRadioModule n'est pas reexporte par SharedModule. « Verification
  // exigee : oui / non » est une question fermee a deux reponses, ou un groupe
  // de boutons radio dit mieux qu'une case a cocher qu'AUCUNE des deux n'est
  // cochee tant que la question n'a pas ete tranchee (ADR 0073).
  imports: [SharedModule, UiModule, MatRadioModule],
  exports: [CapaActionDialogComponent]
})
export class CapaActionDialogModule {}
