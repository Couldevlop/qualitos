import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'qos-main-shell',
  templateUrl: './main-shell.component.html',
  styleUrls: ['./main-shell.component.scss'],
  standalone: false
})
export class MainShellComponent {

  readonly user$: Observable<AuthUser | null>;

  readonly nav: NavItem[] = [
    { label: 'Accueil',       route: '/home',      icon: 'dashboard' },
    { label: 'PDCA',          route: '/pdca',      icon: 'autorenew' },
    { label: 'Ishikawa',      route: '/ishikawa',  icon: 'account_tree' },
    { label: '5S',            route: '/fives',     icon: 'check_circle' },
    { label: 'CAPA',          route: '/capa',      icon: 'engineering' },
    { label: 'Audits',        route: '/audits',    icon: 'fact_check' },
    { label: 'Standards Hub', route: '/standards', icon: 'workspace_premium' }
  ];

  constructor(private readonly auth: AuthService) {
    this.user$ = this.auth.user();
  }
}
