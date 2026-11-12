import { Component, Input, NgModule } from '@angular/core';

import { FiveWhysAnalysis } from '../../../../five-whys/five-whys.types';
import { IshikawaDiagramResponse } from '../../../../ishikawa/ishikawa.types';

/**
 * Doublure des figures d'analyse de cause, pour les écrans qui les hébergent.
 *
 * <p>Les figures ont leur propre banc : les bancs de leurs hôtes — la fiche de
 * l'écart et le rapport 8D — ne vérifient que leur <b>branchement</b>, c'est-à-dire
 * les chaînes et les diagrammes qu'ils leur passent. Les monter en vrai y ferait
 * calculer deux tracés SVG, qui n'ont rien à voir avec ce qu'on mesure.
 *
 * <p>La doublure expose <b>exactement</b> les mêmes entrées que l'original :
 * plus permissive, elle laisserait passer un branchement sur un attribut qui
 * n'existe pas.
 */
@Component({
  selector: 'qos-nc-cause-analysis',
  template: '',
  standalone: false
})
export class NcCauseAnalysisStubComponent {
  @Input() fiveWhys: FiveWhysAnalysis[] = [];
  @Input() ishikawa: IshikawaDiagramResponse[] = [];
}

@NgModule({
  declarations: [NcCauseAnalysisStubComponent],
  exports: [NcCauseAnalysisStubComponent]
})
export class NcCauseAnalysisStubModule {}
