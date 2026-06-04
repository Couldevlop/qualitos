import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { ActivityFeedWidgetComponent } from './activity-feed-widget/activity-feed-widget.component';

/**
 * Module du flux d'activité : expose le widget réutilisable `qos-activity-feed-widget`
 * (accueil, dashboards). Le service `ActivityFeedService` est `providedIn: 'root'`.
 */
@NgModule({
  declarations: [ActivityFeedWidgetComponent],
  imports: [SharedModule],
  exports: [ActivityFeedWidgetComponent]
})
export class ActivityFeedModule {}
