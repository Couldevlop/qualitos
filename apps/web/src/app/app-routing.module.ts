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
      },
      {
        path: 'dmaic',
        loadChildren: () => import('./features/dmaic/dmaic.module').then(m => m.DmaicModule)
      },
      {
        path: 'documents',
        loadChildren: () => import('./features/documents/documents.module').then(m => m.DocumentsModule)
      },
      {
        path: 'fmea',
        loadChildren: () => import('./features/fmea/fmea.module').then(m => m.FmeaModule)
      },
      {
        path: 'suppliers',
        loadChildren: () => import('./features/suppliers/suppliers.module').then(m => m.SuppliersModule)
      },
      {
        path: 'training',
        loadChildren: () => import('./features/training/training.module').then(m => m.TrainingModule)
      },
      {
        path: 'changes',
        loadChildren: () => import('./features/changes/changes.module').then(m => m.ChangesModule)
      },
      {
        path: 'ehs',
        loadChildren: () => import('./features/ehs/ehs.module').then(m => m.EhsModule)
      },
      {
        path: 'itsm',
        loadChildren: () => import('./features/itsm/itsm.module').then(m => m.ItsmModule)
      },
      {
        path: 'kpis',
        loadChildren: () => import('./features/kpis/kpis.module').then(m => m.KpisModule)
      },
      {
        path: 'ropa',
        loadChildren: () => import('./features/ropa/ropa.module').then(m => m.RopaModule)
      },
      {
        path: 'consents',
        loadChildren: () => import('./features/consents/consents.module').then(m => m.ConsentsModule)
      },
      {
        path: 'subject-requests',
        loadChildren: () => import('./features/subject-requests/subject-requests.module').then(m => m.SubjectRequestsModule)
      },
      {
        path: 'privacy-notices',
        loadChildren: () => import('./features/privacy-notices/privacy-notices.module').then(m => m.PrivacyNoticesModule)
      },
      {
        path: 'dashboard-builder',
        loadChildren: () =>
          import('./features/dashboard-builder/dashboard-builder.module').then(m => m.DashboardBuilderModule)
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
