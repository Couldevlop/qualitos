import { Component } from '@angular/core';

interface MethodCard {
  title: string;
  description: string;
  icon: string;
  route?: string;
  available: boolean;
}

@Component({
  selector: 'qos-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: false
})
export class HomeComponent {

  readonly methods: MethodCard[] = [
    { title: 'PDCA', description: $localize`:@@home.card.pdca-desc:Roue de Deming — Plan / Do / Check / Act`,
      icon: 'autorenew', route: '/pdca', available: true },
    { title: 'Ishikawa', description: $localize`:@@home.card.ishikawa-desc:Diagramme cause-effet 6M/7M/8M`,
      icon: 'account_tree', route: '/ishikawa', available: true },
    { title: '5S', description: $localize`:@@home.card.fives-desc:Audit terrain Seiri/Seiton/Seiso/Seiketsu/Shitsuke`,
      icon: 'check_circle', route: '/fives', available: true },
    { title: 'CAPA', description: $localize`:@@home.card.capa-desc:Actions correctives & préventives`,
      icon: 'engineering', route: '/capa', available: true },
    { title: $localize`:@@home.card.audits-title:Audits`, description: $localize`:@@home.card.audits-desc:Plans + checklists + findings`,
      icon: 'fact_check', route: '/audits', available: true },
    { title: 'Standards Hub', description: $localize`:@@home.card.standards-desc:Catalogue normatif + adoption + alignment`,
      icon: 'workspace_premium', route: '/standards', available: true },
    { title: $localize`:@@home.card.circles-title:Cercles Qualité`, description: $localize`:@@home.card.circles-desc:Groupes, réunions, propositions, mesure d'impact`,
      icon: 'groups', route: '/circles', available: true },
    { title: 'DMAIC', description: $localize`:@@home.card.dmaic-desc:Six Sigma — Define / Measure / Analyze / Improve / Control`,
      icon: 'science', available: false }
  ];
}
