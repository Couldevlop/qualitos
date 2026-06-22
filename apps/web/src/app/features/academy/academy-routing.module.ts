import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AcademyHomeComponent } from './presentation/academy-home/academy-home.component';
import { CoursePlayerComponent } from './presentation/course-player/course-player.component';
import { CertificateViewComponent } from './presentation/certificate-view/certificate-view.component';

const routes: Routes = [
  { path: '', component: AcademyHomeComponent },
  { path: 'learn/:enrollmentId', component: CoursePlayerComponent },
  { path: 'certificate/:enrollmentId', component: CertificateViewComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AcademyRoutingModule {}
