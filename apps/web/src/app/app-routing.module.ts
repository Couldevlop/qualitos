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
        // Mode TV / Salle qualité (§7.3) — affichage mural rotatif des KPIs.
        path: 'tv',
        loadChildren: () => import('./features/tv-mode/tv-mode.module').then(m => m.TvModeModule)
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
        path: 'nc',
        loadChildren: () => import('./features/nc/nc.module').then(m => m.NcModule)
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
        path: 'standards-doc-gen',
        loadChildren: () =>
          import('./features/standards-doc-gen/standards-doc-gen.module')
            .then(m => m.StandardsDocGenModule)
      },
      {
        path: 'industry-packs',
        loadChildren: () =>
          import('./features/industry-packs/industry-packs.module').then(m => m.IndustryPacksModule)
      },
      {
        // Marketplace de packs normatifs (§8.11) : catalogue, soumission partenaire,
        // modération éditeur, installation par tenant.
        path: 'marketplace',
        loadChildren: () =>
          import('./features/marketplace/marketplace.module').then(m => m.MarketplaceModule)
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
        path: 'spc',
        loadChildren: () => import('./features/spc/spc.module').then(m => m.SpcModule)
      },
      {
        path: 'anomaly',
        loadChildren: () => import('./features/anomaly/anomaly.module').then(m => m.AnomalyModule)
      },
      {
        path: 'forecast',
        loadChildren: () => import('./features/forecast/forecast.module').then(m => m.ForecastModule)
      },
      {
        // Storyboard IA (§7.4) : récit narratif autour des KPIs, par la passerelle IA (LLM04).
        path: 'storyboard',
        loadChildren: () => import('./features/storyboard/storyboard.module').then(m => m.StoryboardModule)
      },
      {
        path: 'nc-clusters',
        loadChildren: () => import('./features/nc-cluster/nc-cluster.module').then(m => m.NcClusterModule)
      },
      {
        path: 'complaints-nlp',
        loadChildren: () =>
          import('./features/complaint-nlp/complaint-nlp.module').then(m => m.ComplaintNlpModule)
      },
      {
        // Designer de workflow BPMN no-code (§5.4). Lazy : la lib bpmn-js (lourde)
        // n'est chargée que via import() dynamique DANS le composant éditeur, donc
        // jamais embarquée dans le bundle initial ni dans ce chunk de feature tant
        // que l'éditeur n'est pas ouvert.
        path: 'workflow-designer',
        loadChildren: () =>
          import('./features/workflow-designer/workflow-designer.module').then(m => m.WorkflowDesignerModule)
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
        path: 'learning',
        loadChildren: () => import('./features/training/learning.module').then(m => m.LearningModule)
      },
      {
        // Academy LMS-light + gamification (§19.3) : cours e-learning, quiz notés,
        // badges/ceintures, certificats signés ML-DSA + ancrés blockchain.
        path: 'academy',
        loadChildren: () => import('./features/academy/academy.module').then(m => m.AcademyModule)
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
        path: 'nlq',
        loadChildren: () => import('./features/nlq/nlq.module').then(m => m.NlqModule)
      },
      {
        path: 'nlq-explore',
        loadChildren: () =>
          import('./features/nlq-explore/nlq-explore.module').then(m => m.NlqExploreModule)
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
        path: 'dpia',
        loadChildren: () => import('./features/dpia/dpia.module').then(m => m.DpiaModule)
      },
      {
        path: 'dpo-appointments',
        loadChildren: () => import('./features/dpo-appointments/dpo-appointments.module').then(m => m.DpoAppointmentsModule)
      },
      {
        path: 'retention',
        loadChildren: () => import('./features/retention/retention.module').then(m => m.RetentionModule)
      },
      {
        path: 'cross-border',
        loadChildren: () => import('./features/transfers/transfers.module').then(m => m.TransfersModule)
      },
      {
        path: 'processor-agreements',
        loadChildren: () => import('./features/processor-agreements/processor-agreements.module').then(m => m.ProcessorAgreementsModule)
      },
      {
        path: 'ai-qms',
        loadChildren: () => import('./features/ai-qms/ai-qms.module').then(m => m.AiQmsModule)
      },
      {
        path: 'ai-conformity',
        loadChildren: () => import('./features/ai-conformity/ai-conformity.module').then(m => m.AiConformityModule)
      },
      {
        path: 'ai-incidents',
        loadChildren: () => import('./features/ai-incidents/ai-incidents.module').then(m => m.AiIncidentsModule)
      },
      {
        path: 'ai-eudb',
        loadChildren: () => import('./features/ai-eudb/ai-eudb.module').then(m => m.AiEudbModule)
      },
      {
        path: 'nis2-measures',
        loadChildren: () => import('./features/nis2-measures/nis2-measures.module').then(m => m.Nis2MeasuresModule)
      },
      {
        path: 'cyber-incidents',
        loadChildren: () => import('./features/cyber-incidents/cyber-incidents.module').then(m => m.CyberIncidentsModule)
      },
      {
        path: 'breaches',
        loadChildren: () => import('./features/breach/breach.module').then(m => m.BreachModule)
      },
      {
        path: 'fria',
        loadChildren: () => import('./features/fria/fria.module').then(m => m.FriaModule)
      },
      {
        path: 'ai-pmm',
        loadChildren: () => import('./features/ai-pmm/ai-pmm.module').then(m => m.AiPmmModule)
      },
      {
        path: 'automated-decisions',
        loadChildren: () => import('./features/automated-decisions/automated-decisions.module').then(m => m.AutomatedDecisionsModule)
      },
      {
        // Hub Conformité (GRC) — point d'entrée unique vers les 19 routes
        // IA Act / RGPD / NIS 2 (Travail 2). Les routes profondes restent
        // déclarées individuellement ci-dessus et donc directement accessibles.
        path: 'compliance',
        loadChildren: () => import('./features/compliance/compliance.module').then(m => m.ComplianceModule)
      },
      {
        path: 'dashboard-builder',
        loadChildren: () =>
          import('./features/dashboard-builder/dashboard-builder.module').then(m => m.DashboardBuilderModule)
      },
      {
        // File d'attente offline (§15.2-15.3) — accessible depuis le chip de
        // synchro de la topbar ; pas d'entrée de nav dédiée (page utilitaire).
        path: 'offline-queue',
        loadChildren: () =>
          import('./features/offline-queue/offline-queue.module').then(m => m.OfflineQueueModule)
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
