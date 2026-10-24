/** Nom du lot de styles bpmn-js émis à part par le build (`inject: false` dans angular.json). */
export const BPMN_STYLES_HREF = 'bpmn.css';

const LINK_ID = 'qos-bpmn-styles';

/**
 * Ajoute une seule fois la feuille de styles bpmn-js au document.
 *
 * <p>Ces styles (≈ 118 Ko, police incluse) ne servent qu'au designer de
 * workflow : ils sont sortis du lot global pour ne pas peser sur le premier
 * affichage de toutes les autres pages. Le lien est relatif, donc résolu
 * contre le {@code <base href>} de la locale servie.</p>
 */
export function ensureBpmnStyles(doc: Document): void {
  if (doc.getElementById(LINK_ID)) {
    return;
  }
  const link = doc.createElement('link');
  link.id = LINK_ID;
  link.rel = 'stylesheet';
  link.href = BPMN_STYLES_HREF;
  doc.head.appendChild(link);
}
