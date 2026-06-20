import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Une sous-vue GRC : libellé + icône + route existante (jamais cassée). */
export interface ComplianceView {
  label: string;
  route: string;
  icon: string;
}

/** Un domaine de conformité (IA Act / RGPD / NIS 2) regroupant ses sous-vues. */
export interface ComplianceDomain {
  /** Clé d'accent CSS : `ai` | `gdpr` | `nis2`. */
  key: 'ai' | 'gdpr' | 'nis2';
  title: string;
  description: string;
  icon: string;
  views: ComplianceView[];
}

/**
 * Page hub Conformité (Travail 2) — point d'entrée unique pour la masse GRC.
 *
 * Au lieu d'encombrer la sidebar globale avec 19 routes, on regroupe ici les
 * 3 domaines (IA Act, RGPD, NIS 2) en tuiles premium. Chaque tuile expose ses
 * sous-vues qui mènent aux routes EXISTANTES — aucune route n'est cassée, la
 * navigation profonde reste intégralement accessible.
 */
@Component({
  selector: 'qos-compliance-hub',
  templateUrl: './compliance-hub.component.html',
  styleUrls: ['./compliance-hub.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ComplianceHubComponent {

  readonly eyebrowLabel = $localize`:@@compliance.hub.eyebrow:Gouvernance, risque & conformité`;
  readonly titleLabel = $localize`:@@compliance.hub.title:Conformité`;
  readonly subtitleLabel = $localize`:@@compliance.hub.subtitle:Pilotez l'IA Act, le RGPD et NIS 2 depuis un point d'entrée unique. Chaque domaine ouvre ses registres et procédures dédiés.`;

  readonly domains: ComplianceDomain[] = [
    {
      key: 'ai',
      title: $localize`:@@compliance.ai.title:IA Act`,
      description: $localize`:@@compliance.ai.desc:Système qualité IA, conformité, incidents, base UE et surveillance post-marché du règlement européen sur l’IA.`,
      icon: 'smart_toy',
      views: [
        { label: $localize`:@@compliance.ai.qms:Système qualité IA (QMS)`, route: '/ai-qms', icon: 'memory' },
        { label: $localize`:@@compliance.ai.conformity:Conformité`, route: '/ai-conformity', icon: 'verified_user' },
        { label: $localize`:@@compliance.ai.incidents:Incidents IA`, route: '/ai-incidents', icon: 'warning' },
        { label: $localize`:@@compliance.ai.eudb:Base de données UE`, route: '/ai-eudb', icon: 'storage' },
        { label: $localize`:@@compliance.ai.fria:Analyse d’impact (FRIA)`, route: '/fria', icon: 'balance' },
        { label: $localize`:@@compliance.ai.pmm:Surveillance post-marché (PMM)`, route: '/ai-pmm', icon: 'monitoring' }
      ]
    },
    {
      key: 'gdpr',
      title: $localize`:@@compliance.gdpr.title:RGPD`,
      description: $localize`:@@compliance.gdpr.desc:Registre des traitements, consentements, droits des personnes, analyses d’impact et gestion des violations.`,
      icon: 'shield_person',
      views: [
        { label: $localize`:@@compliance.gdpr.ropa:Registre (RoPA)`, route: '/ropa', icon: 'shield' },
        { label: $localize`:@@compliance.gdpr.consents:Consentements`, route: '/consents', icon: 'how_to_reg' },
        { label: $localize`:@@compliance.gdpr.subject-requests:Demandes (DSAR)`, route: '/subject-requests', icon: 'gavel' },
        { label: $localize`:@@compliance.gdpr.notices:Mentions d’information`, route: '/privacy-notices', icon: 'article' },
        { label: $localize`:@@compliance.gdpr.dpia:Analyses d’impact (DPIA)`, route: '/dpia', icon: 'assessment' },
        { label: $localize`:@@compliance.gdpr.dpo:Désignations DPO`, route: '/dpo-appointments', icon: 'badge' },
        { label: $localize`:@@compliance.gdpr.retention:Rétention`, route: '/retention', icon: 'auto_delete' },
        { label: $localize`:@@compliance.gdpr.cross-border:Transferts hors UE`, route: '/cross-border', icon: 'public' },
        { label: $localize`:@@compliance.gdpr.processor-agreements:Sous-traitants (DPA)`, route: '/processor-agreements', icon: 'handshake' },
        { label: $localize`:@@compliance.gdpr.breaches:Violations de données`, route: '/breaches', icon: 'privacy_tip' },
        { label: $localize`:@@compliance.gdpr.automated-decisions:Décisions automatisées`, route: '/automated-decisions', icon: 'account_tree' }
      ]
    },
    {
      key: 'nis2',
      title: $localize`:@@compliance.nis2.title:NIS 2`,
      description: $localize`:@@compliance.nis2.desc:Mesures de cybersécurité et déclaration des incidents au titre de la directive NIS 2.`,
      icon: 'security',
      views: [
        { label: $localize`:@@compliance.nis2.measures:Mesures de cybersécurité`, route: '/nis2-measures', icon: 'rule' },
        { label: $localize`:@@compliance.nis2.incidents:Incidents cyber`, route: '/cyber-incidents', icon: 'shield' }
      ]
    }
  ];

  /** Domaine actuellement déplié (tuile cliquée). `null` = tous repliés. */
  expandedKey: ComplianceDomain['key'] | null = null;

  toggle(key: ComplianceDomain['key']): void {
    this.expandedKey = this.expandedKey === key ? null : key;
  }

  isExpanded(key: ComplianceDomain['key']): boolean {
    return this.expandedKey === key;
  }

  /** trackBy stable pour le rendu des domaines. */
  trackByKey(_index: number, domain: ComplianceDomain): string {
    return domain.key;
  }

  /** trackBy stable pour le rendu des sous-vues. */
  trackByRoute(_index: number, view: ComplianceView): string {
    return view.route;
  }
}
