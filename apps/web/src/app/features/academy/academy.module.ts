import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AcademyRoutingModule } from './academy-routing.module';
import { AcademyHomeComponent } from './presentation/academy-home/academy-home.component';
import { CoursePlayerComponent } from './presentation/course-player/course-player.component';
import { CertificateViewComponent } from './presentation/certificate-view/certificate-view.component';
import { PersonNameModule } from '../../shared/person-name/person-name.module';

/**
 * Module Academy (LMS-light + gamification, §19.3). Lazy-loaded sur la route
 * {@code /academy}. NgModules (jamais standalone), HTML/SCSS en fichiers séparés.
 */
@NgModule({
  declarations: [
    AcademyHomeComponent,
    CoursePlayerComponent,
    CertificateViewComponent
  ],
  imports: [PersonNameModule, SharedModule, UiModule, AcademyRoutingModule]
})
export class AcademyModule {}
