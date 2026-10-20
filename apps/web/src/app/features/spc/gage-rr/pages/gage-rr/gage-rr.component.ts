import { Component } from '@angular/core';

/**
 * Le sous-menu « Gage R&R » de la section SPC. La page existe et se rejoint
 * depuis le menu ; son contenu (étude de répétabilité et de reproductibilité
 * des moyens de mesure) n'est pas encore spécifié, elle reste vide.
 */
@Component({
  selector: 'qos-spc-gage-rr',
  templateUrl: './gage-rr.component.html',
  styleUrls: ['./gage-rr.component.scss'],
  standalone: false
})
export class GageRrComponent {}
