/**
 * Déclaration de module minimale pour bpmn-js/lib/Modeler.
 *
 * bpmn-js ne publie pas de types TypeScript pour ses sous-chemins (`lib/...`),
 * et le paquet peut être absent en local (installé en CI — cf. package.json).
 * Cette déclaration permet la compilation TS du `import()` dynamique sans
 * dépendre de la présence du paquet ni de types tiers. La surface réellement
 * utilisée est typée par {@code BpmnModelerLike} dans le composant éditeur.
 */
declare module 'bpmn-js/lib/Modeler' {
  export default class BpmnModeler {
    constructor(options: { container: HTMLElement; [key: string]: unknown });
    importXML(xml: string): Promise<{ warnings: unknown[] }>;
    saveXML(options?: { format?: boolean }): Promise<{ xml?: string }>;
    destroy(): void;
  }
}
