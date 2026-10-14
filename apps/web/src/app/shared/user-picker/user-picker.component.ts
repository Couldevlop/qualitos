import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  booleanAttribute,
  forwardRef
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Observable, Subject, Subscription, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';

import { DirectoryService } from '../../core/directory/directory.service';
import { DirectoryPage, DirectoryUser } from '../../core/directory/directory.types';

export type UserPickerState = 'idle' | 'loading' | 'ready' | 'error';

/** Délai de frappe avant d'interroger l'annuaire. */
export const USER_PICKER_DEBOUNCE_MS = 250;

let nextId = 0;

/**
 * Désigner une personne du client : responsable d'un audit, d'une CAPA, d'un
 * changement… (ADR 0091).
 *
 * <p>Champ de formulaire à part entière (`ControlValueAccessor`) : sa valeur est
 * le COMPTE DE CONNEXION de la personne (`accountId`, le `sub` du jeton), ou
 * `null`. C'est l'identité que portent les dossiers qualité (pilote CAPA,
 * auditeur responsable, décideur d'un changement) et `AuthService` : comparer
 * « est-ce moi ? » reste une simple égalité. L'écran, lui, ne montre que des
 * noms — jamais un identifiant, y compris quand la personne n'est plus dans
 * l'annuaire.
 *
 * <pre>
 *   &lt;qos-user-picker formControlName="ownerId" label="Responsable" required
 *                    (userSelected)="onOwner($event)"&gt;&lt;/qos-user-picker&gt;
 * </pre>
 *
 * <p>Pas de saisie libre : un texte qui ne correspond à aucune personne choisie
 * est remplacé, à la sortie du champ, par la personne déjà choisie (ou vidé).
 * Deux orthographes du même nom ne peuvent donc pas devenir deux personnes.
 */
@Component({
  selector: 'qos-user-picker',
  templateUrl: './user-picker.component.html',
  styleUrls: ['./user-picker.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => UserPickerComponent), multi: true }],
  standalone: false
})
export class UserPickerComponent implements ControlValueAccessor, OnInit, OnDestroy {

  /** Libellé du champ (« Responsable », « Auditeur principal »…). */
  @Input() label = $localize`:@@directory.picker.label:Personne`;
  @Input() placeholder = $localize`:@@directory.picker.placeholder:Rechercher par nom ou adresse`;
  /** Aide sous le champ, facultative. */
  @Input() hint = '';
  @Input({ transform: booleanAttribute }) required = false;
  /** Comptes (`accountId`) à ne pas proposer (ex. : le responsable, quand on choisit son vérificateur). */
  @Input() excludeIds: readonly string[] = [];
  /** Nombre de propositions affichées (borné à 50 par le serveur). */
  @Input() pageSize = 20;

  /** La personne choisie, ou `null` quand le champ est vidé. */
  @Output() readonly userSelected = new EventEmitter<DirectoryUser | null>();

  @ViewChild(MatAutocompleteTrigger) private trigger?: MatAutocompleteTrigger;

  readonly inputId = `qos-user-picker-${nextId++}`;
  readonly text = new FormControl<string | DirectoryUser>('', { nonNullable: true });

  options: DirectoryUser[] = [];
  /** Résultats au-delà de ceux affichés : il faut préciser la recherche. */
  hiddenCount = 0;
  state: UserPickerState = 'idle';
  selected: DirectoryUser | null = null;
  /** L'identifiant reçu ne correspond à personne de l'annuaire du client. */
  unknownSelection = false;
  resolving = false;
  disabled = false;
  liveMessage = '';

  private onChange: (value: string | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;
  private readonly terms$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();
  private resolveSub?: Subscription;

  constructor(private readonly directory: DirectoryService, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.text.valueChanges.pipe(
      filter((v): v is string => typeof v === 'string'),
      takeUntil(this.destroy$)
    ).subscribe(term => this.terms$.next(term));

    this.terms$.pipe(
      map(term => term.trim()),
      debounceTime(USER_PICKER_DEBOUNCE_MS),
      distinctUntilChanged(),
      tap(() => this.setState('loading')),
      switchMap(term => this.lookup(term)),
      takeUntil(this.destroy$)
    ).subscribe(page => this.showResults(page));
  }

  ngOnDestroy(): void {
    this.resolveSub?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** À l'ouverture, les premiers noms de l'annuaire, sans attendre une frappe. */
  onFocus(): void {
    if (this.state === 'idle') {
      const current = this.text.value;
      this.terms$.next(typeof current === 'string' ? current : '');
    }
  }

  readonly displayWith = (value: DirectoryUser | string | null): string =>
    typeof value === 'string' ? value : value?.displayName ?? '';

  trackById(_: number, user: DirectoryUser): string {
    return user.accountId;
  }

  onSelected(event: MatAutocompleteSelectedEvent): void {
    const user = event.option.value as DirectoryUser;
    this.choose(user);
  }

  /** Vide le champ : la valeur devient `null`. */
  clear(): void {
    this.text.setValue('', { emitEvent: false });
    this.choose(null);
    this.terms$.next('');
  }

  /**
   * Sortie du champ (ou fermeture de la liste) : pas de saisie libre. Un texte
   * vide efface le choix ; un texte quelconque est remplacé par le choix en cours.
   */
  reconcile(): void {
    if (this.trigger?.panelOpen) {
      return;
    }
    this.onTouched();
    const value = this.text.value;
    if (typeof value !== 'string') {
      return;
    }
    if (value.trim() === '') {
      if (this.selected || this.unknownSelection) {
        this.choose(null);
      }
      return;
    }
    this.text.setValue(this.selected ?? '', { emitEvent: false });
    this.cdr.markForCheck();
  }

  // ---------- ControlValueAccessor ----------

  writeValue(accountId: string | null | undefined): void {
    this.resolveSub?.unsubscribe();
    this.unknownSelection = false;
    if (!accountId) {
      this.selected = null;
      this.resolving = false;
      this.text.setValue('', { emitEvent: false });
      this.cdr.markForCheck();
      return;
    }
    if (this.selected?.accountId === accountId) {
      return;
    }
    this.selected = null;
    this.resolving = true;
    this.text.setValue('', { emitEvent: false });
    this.resolveSub = this.directory.getByAccount(accountId).subscribe({
      next: user => {
        this.selected = user;
        this.resolving = false;
        this.text.setValue(user, { emitEvent: false });
        this.cdr.markForCheck();
      },
      error: () => {
        // On ne montre jamais l'identifiant : on dit que la personne est introuvable.
        this.resolving = false;
        this.unknownSelection = true;
        this.cdr.markForCheck();
      }
    });
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
    if (disabled) {
      this.text.disable({ emitEvent: false });
    } else {
      this.text.enable({ emitEvent: false });
    }
    this.cdr.markForCheck();
  }

  // ---------- interne ----------

  private choose(user: DirectoryUser | null): void {
    this.selected = user;
    this.unknownSelection = false;
    this.onChange(user ? user.accountId : null);
    this.onTouched();
    this.userSelected.emit(user);
    this.cdr.markForCheck();
  }

  private lookup(term: string): Observable<DirectoryPage | null> {
    return this.directory.search(term, this.pageSize).pipe(catchError(() => of(null)));
  }

  private showResults(page: DirectoryPage | null): void {
    if (!page) {
      this.options = [];
      this.hiddenCount = 0;
      this.liveMessage = $localize`:@@directory.picker.live-error:Annuaire indisponible`;
      this.setState('error');
      return;
    }
    const excluded = new Set(this.excludeIds);
    this.options = page.content.filter(u => !excluded.has(u.accountId));
    this.hiddenCount = Math.max(0, page.totalElements - page.content.length);
    const count = this.options.length;
    this.liveMessage = count === 0
      ? $localize`:@@directory.picker.live-none:Aucune personne trouvée`
      : $localize`:@@directory.picker.live-count:${count}:COUNT: personne(s) proposée(s)`;
    this.setState('ready');
  }

  private setState(state: UserPickerState): void {
    this.state = state;
    this.cdr.markForCheck();
  }
}
