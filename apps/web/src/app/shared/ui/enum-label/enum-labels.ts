/**
 * Libellés des énumérations métier renvoyées par l'API, hors statuts et
 * gravités (qui ont leurs propres tables : `statusLabel`, `severityLabel`).
 *
 * <p>Une énumération ne s'affiche jamais telle quelle (« SIX_M », « FREE »,
 * « integrations ») : elle se lit par {@link enumLabel} ou le pipe `enumLabel`,
 * avec le domaine qui lève l'ambiguïté d'un même code d'un écran à l'autre.
 * Les traductions vivent dans `scripts/i18n/design_audit.py`.
 */
export type EnumDomain = 'ishikawaMode' | 'moduleName' | 'moduleCategory' | 'billingTier';

const TABLES: Readonly<Record<EnumDomain, Readonly<Record<string, string>>>> = {
  ishikawaMode: {
    SIX_M: $localize`:@@enum.ishikawa-mode.six-m:6M`,
    SEVEN_M: $localize`:@@enum.ishikawa-mode.seven-m:7M`,
    EIGHT_M: $localize`:@@enum.ishikawa-mode.eight-m:8M`
  },
  // Les noms du catalogue serveur sont des intitulés techniques en anglais ; le
  // code du module est stable, c'est lui qui porte la traduction.
  moduleName: {
    pdca: $localize`:@@enum.module.pdca:Cycles PDCA`,
    ishikawa: $localize`:@@enum.module.ishikawa:Diagrammes d'Ishikawa`,
    fives: $localize`:@@enum.module.fives:Audits 5S`,
    circle: $localize`:@@enum.module.circle:Cercles de qualité`,
    dmaic: $localize`:@@enum.module.dmaic:DMAIC et Poka-Yoke`,
    capa: $localize`:@@enum.module.capa:CAPA`,
    docs: $localize`:@@enum.module.docs:Maîtrise documentaire`,
    audit: $localize`:@@enum.module.audit:Gestion des audits`,
    risk: $localize`:@@enum.module.risk:Risques et AMDEC`,
    product: $localize`:@@enum.module.product:Référentiel produit`,
    controlplan: $localize`:@@enum.module.controlplan:Plan de surveillance`,
    supplier: $localize`:@@enum.module.supplier:Qualité fournisseurs`,
    training: $localize`:@@enum.module.training:Formation et compétences`,
    change: $localize`:@@enum.module.change:Gestion des changements`,
    complaints: $localize`:@@enum.module.complaints:Réclamations clients`,
    calibration: $localize`:@@enum.module.calibration:Étalonnage et équipements`,
    ehs: $localize`:@@enum.module.ehs:Incidents HSE`,
    standards: $localize`:@@enum.module.standards:Standards Hub`,
    kpi: $localize`:@@enum.module.kpi:Moteur de KPI`,
    industry: $localize`:@@enum.module.industry:Packs sectoriels`,
    iot: $localize`:@@enum.module.iot:Hub IoT`,
    auditlog: $localize`:@@enum.module.auditlog:Journal d'audit`,
    blockchain: $localize`:@@enum.module.blockchain:Ancrage blockchain`,
    webhooks: $localize`:@@enum.module.webhooks:Webhooks sortants`,
    itsm: $localize`:@@enum.module.itsm:Connecteurs ITSM`
  },
  moduleCategory: {
    methodes: $localize`:@@enum.module-category.methodes:Méthodes qualité`,
    transverse: $localize`:@@enum.module-category.transverse:Modules transverses`,
    platform: $localize`:@@enum.module-category.platform:Plateforme`,
    integrations: $localize`:@@enum.module-category.integrations:Intégrations`
  },
  billingTier: {
    FREE: $localize`:@@enum.billing-tier.free:Gratuit`,
    STANDARD: $localize`:@@enum.billing-tier.standard:Standard`,
    PRO: $localize`:@@enum.billing-tier.pro:Pro`,
    ENTERPRISE: $localize`:@@enum.billing-tier.enterprise:Entreprise`
  }
};

/**
 * Le libellé d'une valeur d'énumération dans son domaine. Une valeur inconnue
 * n'est pas laissée en capitales : elle se lit en minuscules, mots séparés,
 * première lettre majuscule — lisible en attendant son entrée dans la table.
 */
export function enumLabel(code: string | null | undefined, domain: EnumDomain): string {
  if (!code) {
    return '—';
  }
  const known = TABLES[domain][code];
  if (known) {
    return known;
  }
  const words = code.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** Les valeurs connues d'un domaine — pour les tests et les sélecteurs. */
export function knownEnumCodes(domain: EnumDomain): readonly string[] {
  return Object.keys(TABLES[domain]);
}
