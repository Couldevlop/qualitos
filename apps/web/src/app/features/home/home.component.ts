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
    { title: 'PDCA', description: 'Roue de Deming — Plan / Do / Check / Act',
      icon: 'autorenew', route: '/pdca', available: true },
    { title: 'Ishikawa', description: 'Diagramme cause-effet 6M/7M/8M',
      icon: 'account_tree', route: '/ishikawa', available: true },
    { title: '5S', description: 'Audit terrain Seiri/Seiton/Seiso/Seiketsu/Shitsuke',
      icon: 'check_circle', route: '/fives', available: true },
    { title: 'CAPA', description: 'Actions correctives & préventives',
      icon: 'engineering', route: '/capa', available: true },
    { title: 'Audits', description: 'Plans + checklists + findings',
      icon: 'fact_check', route: '/audits', available: true },
    { title: 'Standards Hub', description: 'Catalogue normatif + adoption + alignment',
      icon: 'workspace_premium', route: '/standards', available: true },
    { title: 'DMAIC', description: 'Six Sigma — Define / Measure / Analyze / Improve / Control',
      icon: 'science', available: false },
    { title: 'Cercle Qualité', description: 'Groupes, réunions, propositions',
      icon: 'group', available: false }
  ];
}
