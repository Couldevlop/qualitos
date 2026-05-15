import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { MainShellComponent } from './layout/main-shell/main-shell.component';

const routes: Routes = [
  {
    path: '',
    component: MainShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'home' },
      {
        path: 'home',
        loadChildren: () => import('./features/home/home.module').then(m => m.HomeModule)
      },
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
      },
      {
        path: 'pdca',
        loadChildren: () => import('./features/pdca/pdca.module').then(m => m.PdcaModule)
      },
      {
        path: 'ishikawa',
        loadChildren: () => import('./features/ishikawa/ishikawa.module').then(m => m.IshikawaModule)
      },
      {
        path: 'fives',
        loadChildren: () => import('./features/fives/fives.module').then(m => m.FivesModule)
      },
      {
        path: 'capa',
        loadChildren: () => import('./features/capa/capa.module').then(m => m.CapaModule)
      },
      {
        path: 'audits',
        loadChildren: () => import('./features/audits/audits.module').then(m => m.AuditsModule)
      },
      {
        path: 'standards',
        loadChildren: () => import('./features/standards/standards.module').then(m => m.StandardsModule)
      },
      {
        path: 'circles',
        loadChildren: () => import('./features/circles/circles.module').then(m => m.CirclesModule)
      }
    ]
  },
  { path: '**', redirectTo: 'home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'top' })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
