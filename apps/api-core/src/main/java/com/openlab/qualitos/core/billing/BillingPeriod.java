package com.openlab.qualitos.core.billing;

import java.time.LocalDate;

/**
 * La périodicité d'un abonnement : mensuelle ou annuelle.
 *
 * <p>Le calcul de la prochaine échéance s'appuie sur {@link LocalDate#plusMonths}
 * et {@link LocalDate#plusYears}, qui ramènent au dernier jour du mois cible
 * quand le jour de départ n'existe pas dans ce mois (31 janvier + 1 mois =
 * 28 février, pas un 31 février qui n'existe pas). C'est ce comportement,
 * pas un arrondi maison, qu'on veut ici : toute autre règle romprait avec ce
 * que fait déjà le JDK pour les mêmes dates ailleurs dans la plateforme.
 */
public enum BillingPeriod {

    MONTHLY {
        @Override
        public LocalDate nextRenewal(LocalDate from) {
            return from.plusMonths(1);
        }
    },
    ANNUAL {
        @Override
        public LocalDate nextRenewal(LocalDate from) {
            return from.plusYears(1);
        }
    };

    public abstract LocalDate nextRenewal(LocalDate from);
}
