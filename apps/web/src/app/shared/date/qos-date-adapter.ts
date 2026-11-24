import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/** Les trois champs d'une date, dans l'ordre où une langue les écrit. */
type ChampDeDate = 'year' | 'month' | 'day';

/**
 * L'adaptateur de date du produit : il LIT ce qu'il affiche.
 *
 * <p>`NativeDateAdapter` ne sait pas lire une date saisie au clavier, et il le
 * dit lui-même : « We have no way using the native JS Date to set the parse
 * format or locale, so we ignore these parameters. » Il s'en remet à
 * `Date.parse`, qui ne connaît que l'ordre américain. Dans un build français,
 * `Date.parse('24/11/2026')` vaut `NaN` : le champ passait au rouge et le
 * formulaire se bloquait, alors même qu'il venait d'afficher cette date sous
 * cette forme. Seul le calendrier s'en sortait.
 *
 * <p>Pire que le refus : `05/06/2026` était bien lu par `Date.parse`, mais
 * comme le 6 MAI. Une date acceptée et fausse sur une fiche qui part au client
 * est plus grave qu'une date refusée. C'est pourquoi une saisie en chiffres ne
 * retombe JAMAIS sur `Date.parse` ici : elle passe par l'ordre de la langue, ou
 * elle est invalide.
 *
 * <p><b>L'ordre des champs vient de la langue, pas d'une constante.</b> Le
 * produit se construit en six langues et elles ne s'accordent pas : jour-mois
 * en français et en espagnol, mois-jour en anglais, année d'abord en japonais
 * et en chinois. Il se déduit donc de `Intl.DateTimeFormat`, la même source que
 * celle qui a servi à écrire la date (§22 #11 : rien de régional en dur).
 *
 * <p><b>Les chiffres ne sont pas tous arabes-européens.</b> En arabe, la date
 * s'affiche en chiffres arabes-indiens et porte des marques de sens d'écriture
 * invisibles. Relire ce qu'on vient d'écrire suppose donc d'ôter ces marques et
 * de ramener les chiffres à leur valeur, sans quoi l'arabe serait la seule
 * langue où l'on ne peut pas retaper ce qui est à l'écran.
 */
@Injectable()
export class QosDateAdapter extends NativeDateAdapter {

  /**
   * Une saisie devient une date, ou une date invalide — jamais une autre date.
   *
   * @param value ce que l'utilisateur a tapé, ou ce que le formulaire a posé
   */
  override parse(value: unknown, parseFormat?: unknown): Date | null {
    if (typeof value !== 'string') {
      return super.parse(value, parseFormat);
    }
    const texte = normaliser(value);
    if (!texte) {
      return null;
    }
    // Une date ISO porte elle aussi trois nombres : elle se reconnaît à sa
    // forme, et non à leur compte. Sans ce test, « 2026-11-24 » serait rangé
    // dans l'ordre de la langue, et le français y lirait le jour 2026.
    if (/^\d{4}-\d{2}-\d{2}/.test(texte)) {
      return super.parse(texte, parseFormat);
    }
    const nombres = texte.match(/\d+/g);
    // Trois nombres et pas une lettre : c'est une date écrite en chiffres, et
    // elle se lit dans l'ordre de la langue. Tout le reste — un mois en toutes
    // lettres — garde la lecture d'origine, qui le traite bien.
    if (!nombres || nombres.length !== 3 || /\p{L}/u.test(texte)) {
      return super.parse(texte, parseFormat);
    }
    return this.selonLOrdreDeLaLangue(nombres);
  }

  /**
   * Trois nombres rangés selon la langue, puis vérifiés.
   *
   * <p>La vérification n'est pas une politesse : `new Date(2026, 1, 31)` rend
   * le 3 mars sans rien signaler. Sur une demande de dérogation, une date
   * décalée de deux jours sans que personne ne l'ait voulu est exactement
   * l'erreur qu'un contrôle qualité doit rendre impossible.
   */
  private selonLOrdreDeLaLangue(nombres: readonly string[]): Date {
    const ordre = ordreDesChamps(this.locale);
    const lu: Record<ChampDeDate, number> = { year: 0, month: 0, day: 0 };
    ordre.forEach((champ, rang) => (lu[champ] = Number(nombres[rang])));

    // Une année s'écrit en entier : « 24/11/26 » ne dit pas si 26 est l'année
    // ou le jour d'un autre ordre, et deviner ici serait décider à la place de
    // celui qui tape.
    const anneeEntiere = nombres[ordre.indexOf('year')].length === 4;
    const dansLesBornes = lu.month >= 1 && lu.month <= 12 && lu.day >= 1 && lu.day <= 31;
    if (!anneeEntiere || !dansLesBornes) {
      return this.invalid();
    }
    // `createDate` LÈVE sur un 31 février plutôt que de rendre une date
    // invalide. Ici, une saisie impossible est un cas ordinaire — quelqu'un
    // tape — et non une erreur de programmation : elle se rend, elle ne se
    // jette pas, et le champ passe simplement au rouge.
    try {
      const date = this.createDate(lu.year, lu.month - 1, lu.day);
      const telQueTape = this.getYear(date) === lu.year
        && this.getMonth(date) === lu.month - 1
        && this.getDate(date) === lu.day;
      return telQueTape ? date : this.invalid();
    } catch {
      return this.invalid();
    }
  }
}

/**
 * L'ordre des trois champs dans la langue donnée.
 *
 * <p>Il se lit sur le format numérique court, celui-là même que l'adaptateur
 * affiche. Si la langue est inconnue du navigateur, on retient jour-mois-année,
 * l'ordre de la langue source du produit.
 */
function ordreDesChamps(locale: string): readonly ChampDeDate[] {
  try {
    const ordre = new Intl.DateTimeFormat(locale, {
      year: 'numeric', month: 'numeric', day: 'numeric', timeZone: 'utc'
    })
      .formatToParts(new Date(Date.UTC(2026, 10, 24)))
      .map(partie => partie.type)
      .filter((type): type is ChampDeDate =>
        type === 'year' || type === 'month' || type === 'day');
    return ordre.length === 3 ? ordre : ['day', 'month', 'year'];
  } catch {
    return ['day', 'month', 'year'];
  }
}

/**
 * La saisie ramenée à des chiffres arabes-européens.
 *
 * <p>Ôte les marques de sens d'écriture que les langues de droite à gauche
 * insèrent dans une date formatée, et ramène tout chiffre décimal à sa valeur.
 */
function normaliser(valeur: string): string {
  let sortie = '';
  for (const caractere of valeur.replace(/[‎‏؜]/gu, '').trim()) {
    const valeurDuChiffre = chiffre(caractere);
    sortie += valeurDuChiffre === null ? caractere : String(valeurDuChiffre);
  }
  return sortie;
}

/**
 * La valeur d'un caractère s'il est un chiffre décimal, sinon `null`.
 *
 * <p>Unicode range les chiffres décimaux par dix consécutifs à partir d'un
 * zéro. La distance au zéro de SON écriture donne donc la valeur, quelle que
 * soit la langue, sans table à tenir à jour. Le zéro se reconnaît à ce que le
 * caractère qui le précède n'est pas un chiffre.
 */
function chiffre(caractere: string): number | null {
  if (!/\p{Nd}/u.test(caractere)) {
    return null;
  }
  const point = caractere.codePointAt(0)!;
  for (let valeur = 0; valeur < 10; valeur++) {
    const zeroCandidat = point - valeur;
    if (!/\p{Nd}/u.test(String.fromCodePoint(zeroCandidat - 1))) {
      return valeur;
    }
  }
  return null;
}
