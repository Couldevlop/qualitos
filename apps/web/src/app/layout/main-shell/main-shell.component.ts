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
        { label: 'Accueil',         route: '/home',      icon: 'home' }
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
      label: 'Compliance UE',
      items: [
        { label: 'AI Act', route: '/ai-act', icon: 'smart_toy', badge: '7' },
        { label: 'GDPR',   route: '/gdpr',   icon: 'shield' },
        { label: 'NIS 2',  route: '/nis2',   icon: 'security' }
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
