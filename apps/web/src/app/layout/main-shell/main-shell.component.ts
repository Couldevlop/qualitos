import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
  badge?: string;
}

export interface NavSection {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'qos-main-shell',
  templateUrl: './main-shell.component.html',
  styleUrls: ['./main-shell.component.scss'],
  standalone: false
})
export class MainShellComponent implements OnInit {

  user$!: Observable<AuthUser | null>;
  collapsed = false;

  readonly sections: NavSection[] = [
    {
      label: 'Pilotage',
      items: [
        { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard' },
        { label: 'Accueil',         route: '/home',      icon: 'home' },
        { label: 'KPI catalog',     route: '/kpis',      icon: 'monitoring' }
      ]
    },
    {
      label: 'Méthodes qualité',
      items: [
        { label: 'PDCA',     route: '/pdca',     icon: 'autorenew' },
        { label: 'Ishikawa', route: '/ishikawa', icon: 'account_tree' },
        { label: '5S',       route: '/fives',    icon: 'check_circle' },
        { label: 'DMAIC',    route: '/dmaic',    icon: 'analytics' },
        { label: 'Cercles',  route: '/circles',  icon: 'groups' }
      ]
    },
    {
      label: 'Processus',
      items: [
        { label: 'CAPA',          route: '/capa',      icon: 'engineering' },
        { label: 'Audits',        route: '/audits',    icon: 'fact_check' },
        { label: 'FMEA / Risk',   route: '/fmea',      icon: 'warning' },
        { label: 'Fournisseurs',  route: '/suppliers', icon: 'local_shipping' },
        { label: 'Formation',     route: '/training',  icon: 'school' },
        { label: 'Changements',   route: '/changes',   icon: 'change_circle' },
        { label: 'EHS',           route: '/ehs',       icon: 'health_and_safety' },
        { label: 'Documents',     route: '/documents', icon: 'description' },
        { label: 'Standards Hub', route: '/standards', icon: 'workspace_premium' }
      ]
    },
    {
      label: 'Intégrations',
      items: [
        { label: 'ITSM',         route: '/itsm',         icon: 'hub' }
      ]
    },
    {
      label: 'Compliance UE',
      items: [
        { label: 'AI Act',       route: '/ai-act', icon: 'smart_toy', badge: '7' },
        { label: 'AI Act · QMS',        route: '/ai-qms',         icon: 'memory' },
        { label: 'AI Act · Conformité', route: '/ai-conformity',  icon: 'verified_user' },
        { label: 'AI Act · Incidents',  route: '/ai-incidents',   icon: 'warning' },
        { label: 'GDPR · RoPA',      route: '/ropa',     icon: 'shield' },
        { label: 'GDPR · Consents',  route: '/consents',         icon: 'how_to_reg' },
        { label: 'GDPR · DSAR',      route: '/subject-requests', icon: 'gavel' },
        { label: 'GDPR · Notices',   route: '/privacy-notices',  icon: 'article' },
        { label: 'GDPR · DPIA',      route: '/dpia',              icon: 'assessment' },
        { label: 'GDPR · DPO',       route: '/dpo-appointments',  icon: 'badge' },
        { label: 'GDPR · Rétention', route: '/retention',         icon: 'auto_delete' },
        { label: 'GDPR · Transferts',route: '/cross-border',      icon: 'public' },
        { label: 'GDPR · DPA Art.28', route: '/processor-agreements', icon: 'handshake' },
        { label: 'GDPR',             route: '/gdpr',              icon: 'shield_lock' },
        { label: 'NIS 2',        route: '/nis2',   icon: 'security' }
      ]
    }
  ];

  constructor(private readonly auth: AuthService) {}

  ngOnInit(): void {
    this.user$ = this.auth.user();
  }

  toggle(): void { this.collapsed = !this.collapsed; }

  initials(name?: string | null): string {
    if (!name) return '··';
    return name.trim().split(/\s+/).slice(0, 2).map(p => p[0] ?? '').join('').toUpperCase();
  }
}
