import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Exige une valeur qui ne soit pas que des espaces.
 *
 * <p>`Validators.required` laisse passer `'   '` : le formulaire se disait
 * valide, le dialogue rendait un intitulé vide, et le schéma affichait une
 * phase sans nom qu'on ne pouvait plus désigner. La règle rend l'erreur
 * `required` et non une clé à elle, pour que le message déjà écrit dans le
 * gabarit continue de s'afficher.
 */
export function nonBlank(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return typeof value === 'string' && value.trim().length === 0
    ? { required: true }
    : null;
}
