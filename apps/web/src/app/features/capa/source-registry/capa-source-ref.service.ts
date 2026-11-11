import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { AuditsService } from '../../audits/audits.service';
import { ComplaintsService } from '../../complaints/complaints.service';
import { NcService } from '../../nc/nc.service';
import { RiskRegisterService } from '../../risk-register/risk-register.service';
import { CapaSourceRegister } from '../capa.labels';

/** Une référence proposée au choix : ce qu'on stocke, et ce qu'on lit. */
export interface CapaSourceRefOption {
  /** La référence citable, telle qu'elle sera enregistrée dans le dossier. */
  reference: string;
  /** Ce que l'utilisateur lit dans la liste : la référence et son intitulé. */
  label: string;
}

/**
 * Combien d'objets on propose au choix.
 *
 * <p>Un registre de réclamations ou de non-conformités compte des milliers de
 * lignes au bout de quelques années : tout charger ferait une liste
 * inutilisable et une requête lourde à chaque ouverture de la fenêtre. On
 * charge les plus récentes — l'écart qu'on traite aujourd'hui en fait partie —
 * et la fenêtre dit quand la liste est tronquée, au lieu de laisser croire
 * qu'une référence absente n'existe pas.
 */
export const SOURCE_REF_PAGE_SIZE = 200;

/**
 * Les références qu'une source peut désigner, lues au registre qui les détient.
 *
 * <p>Pourquoi un service dédié plutôt qu'un champ de texte : une CAPA dont la
 * source dit « Audit » sans dire LEQUEL ne se rattache à rien. La référence
 * tapée à la main se perd au premier mot mal recopié, et le dossier devient
 * orphelin de sa cause — ce que tout auditeur cherche en premier quand il
 * remonte une action corrective jusqu'à son constat.
 *
 * <p>Ce service s'appuie sur les services de chaque registre plutôt que sur les
 * URL de leurs API : ce sont eux qui savent déjà leur route, leur pagination et
 * leur mode de démonstration. Les redéclarer ici ferait deux vérités pour la
 * même ressource, et la seconde vieillirait en silence.
 */
@Injectable({ providedIn: 'root' })
export class CapaSourceRefService {

  constructor(
    private readonly nc: NcService,
    private readonly audits: AuditsService,
    private readonly complaints: ComplaintsService,
    private readonly risks: RiskRegisterService
  ) {}

  /**
   * Les références proposées pour ce registre : seulement les fiches ENCORE
   * OUVERTES, les plus récentes d'abord quand le registre les rend ainsi.
   *
   * <p>Proposer une fiche close rattacherait le dossier à une cause que plus
   * personne ne traite, et ferait grossir une liste déjà longue de tout
   * l'historique du client.
   *
   * <p>Aucune erreur n'est avalée : un registre injoignable doit se VOIR dans la
   * fenêtre. L'absorber rendrait une liste vide, et une liste vide dit « il n'y
   * a rien à choisir » — ce qui est faux, et ce qui pousserait l'utilisateur à
   * ouvrir un dossier sans rattachement.
   */
  options(register: CapaSourceRegister): Observable<CapaSourceRefOption[]> {
    switch (register) {
      case 'nc':
        return this.nc.listNcs(0, SOURCE_REF_PAGE_SIZE).pipe(
          map(page => page.content
            .filter(n => !NC_TERMINEES.includes(n.status))
            .map(n => option(n.reference, n.title))));
      case 'audit':
        return this.audits.listPlans(0, SOURCE_REF_PAGE_SIZE).pipe(
          map(page => page.content
            .filter(a => a.status !== 'CANCELLED')
            .map(a => option(a.reference, a.title))));
      case 'complaint':
        return this.complaints.list(0, SOURCE_REF_PAGE_SIZE).pipe(
          map(page => page.content
            .filter(c => !RECLAMATIONS_TERMINEES.includes(c.status))
            .map(c => option(c.code, c.subject))));
      case 'risk':
        return this.risks.risks().pipe(
          map(risks => risks
            .filter(r => r.status !== 'CLOSED')
            .map(r => option(r.reference, r.title))));
      default:
        // Jamais atteint : `CapaSourceRegister` est fermé, et le compilateur
        // refuse d'ajouter un registre sans passer ici. La branche existe pour
        // que l'oubli se voie au banc plutôt qu'en production.
        return of([]);
    }
  }
}

/**
 * Les statuts terminaux d'une non-conformité.
 *
 * <p>On ne propose que les fiches VIVANTES : ouvrir une action corrective sur un
 * écart déjà clos, annulé ou écarté après instruction rattacherait le dossier à
 * une cause que personne ne traite plus. `RESOLVED` reste proposé — l'écart est
 * traité mais pas encore clos, et c'est exactement le moment où une action de
 * fond se décide.
 */
const NC_TERMINEES: ReadonlyArray<string> = ['CLOSED', 'CANCELLED', 'REJECTED'];

/**
 * Les statuts terminaux d'une réclamation : fermée, ou écartée hors périmètre.
 *
 * <p>`REOPENED` n'en fait pas partie : une réclamation rouverte est précisément
 * celle sur laquelle une action de fond manquait.
 */
const RECLAMATIONS_TERMINEES: ReadonlyArray<string> = ['CLOSED', 'REJECTED'];

/** « AUD-2026-0003 — Audit interne du laboratoire ». */
function option(reference: string, intitule: string | undefined | null): CapaSourceRefOption {
  const titre = (intitule ?? '').trim();
  return { reference, label: titre ? `${reference} — ${titre}` : reference };
}
