import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiPolicyService } from '../../ai-policy.service';
import { AiPolicy, AiProviderName, AiProviderOption } from '../../ai-policy.types';

/** Le choix à l'écran : un fournisseur, ou le défaut de la plateforme. */
export type AiPolicyChoice = AiProviderName | 'default';

/** Ce que l'écran dit de chaque fournisseur ; l'ordre est celui des cartes. */
interface ProviderText { label: string; description: string; }

/**
 * « Fournisseur d'IA » (ADR 0102, §12.2), réservé à l'administrateur du client :
 * à qui partent les demandes faites à l'IA — rapports, comptes-rendus, causes
 * Ishikawa, questions sur les données, audit blanc, documents générés.
 *
 * <p>Le modèle local garde les demandes, et les données qu'elles portent, dans
 * l'infrastructure du client. L'écran le dit à chaque choix, et refuse un
 * fournisseur que le service d'IA déclare non configuré.
 */
@Component({
  selector: 'qos-ai-policy',
  templateUrl: './ai-policy.component.html',
  styleUrls: ['./ai-policy.component.scss'],
  standalone: false
})
export class AiPolicyComponent implements OnInit {

  policy: AiPolicy | null = null;
  choice: AiPolicyChoice = 'default';

  loading = false;
  failed = false;
  saving = false;

  readonly texts: Record<AiProviderName, ProviderText> = {
    ollama: {
      label: $localize`:@@admin.ai.provider.ollama:Modèle local (Ollama)`,
      description: $localize`:@@admin.ai.provider.ollama-desc:Le modèle tourne sur l'infrastructure de votre installation. Les demandes, et les données qu'elles portent, ne la quittent pas.`
    },
    mistral: {
      label: $localize`:@@admin.ai.provider.mistral:Mistral AI`,
      description: $localize`:@@admin.ai.provider.mistral-desc:Service externe. Les demandes lui sont transmises, une fois retirées les données personnelles détectées.`
    },
    anthropic: {
      label: $localize`:@@admin.ai.provider.anthropic:Anthropic`,
      description: $localize`:@@admin.ai.provider.anthropic-desc:Service externe. Les demandes lui sont transmises, une fois retirées les données personnelles détectées.`
    }
  };

  constructor(
    private readonly service: AiPolicyService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.get().subscribe({
      next: p => {
        this.loading = false;
        this.reprendre(p);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@admin.ai.load-failed:Le fournisseur d'IA n'a pas pu être chargé.`);
      }
    });
  }

  /** Le choix enregistré, tel que l'écran le représente. */
  get savedChoice(): AiPolicyChoice {
    return this.policy?.pinned && this.policy.provider ? this.policy.provider : 'default';
  }

  get dirty(): boolean {
    return !!this.policy && this.choice !== this.savedChoice;
  }

  /** Le fournisseur qui répondrait avec le choix affiché (pas encore enregistré). */
  get chosenProvider(): AiProviderName | null {
    return this.choice === 'default' ? this.policy?.platformDefault ?? null : this.choice;
  }

  /** Vrai si les demandes restent chez le client avec le choix affiché ; null si on ne le sait pas. */
  get sovereign(): boolean | null {
    const p = this.chosenProvider;
    return p ? this.option(p)?.onPremise ?? null : null;
  }

  option(name: AiProviderName): AiProviderOption | undefined {
    return this.policy?.providers.find(o => o.name === name);
  }

  label(name: AiProviderName | null): string {
    return name ? this.texts[name].label : $localize`:@@admin.ai.unknown-provider:inconnu`;
  }

  /** Un fournisseur que le service d'IA déclare non configuré ne se choisit pas. */
  unavailable(name: AiProviderName): boolean {
    return this.option(name)?.configured === false;
  }

  /** L'épinglage enregistré pointe vers un fournisseur qui ne peut plus répondre. */
  get pinnedBroken(): boolean {
    return !!this.policy?.pinned && !!this.policy.provider && this.unavailable(this.policy.provider);
  }

  select(choice: AiPolicyChoice): void {
    if (choice !== 'default' && this.unavailable(choice)) return;
    this.choice = choice;
  }

  discard(): void {
    this.choice = this.savedChoice;
  }

  save(): void {
    if (!this.dirty || this.saving) return;
    this.saving = true;
    const appel = this.choice === 'default' ? this.service.release() : this.service.pin(this.choice);
    appel.subscribe({
      next: p => {
        this.saving = false;
        this.reprendre(p);
        this.snack.open($localize`:@@admin.ai.saved:Fournisseur d'IA enregistré. Il s'applique aux prochaines demandes.`,
          undefined, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@admin.ai.save-failed:Le fournisseur d'IA n'a pas pu être enregistré.`);
      }
    });
  }

  private reprendre(p: AiPolicy): void {
    this.policy = p;
    this.choice = this.savedChoice;
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}
