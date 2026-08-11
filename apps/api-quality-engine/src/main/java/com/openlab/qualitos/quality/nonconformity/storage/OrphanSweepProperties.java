package com.openlab.qualitos.quality.nonconformity.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Réglages du balayage des binaires orphelins (§4.3).
 *
 * <p>Classe à part de {@link StorageProperties}, dont le préfixe est
 * {@code qualitos.storage.s3} : le balayage n'a rien de propre à S3 — il vaudrait
 * pour n'importe quel adaptateur du port — et le loger sous « s3 » aurait fait
 * croire le contraire à qui lit la configuration.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.storage.orphan-sweep")
public class OrphanSweepProperties {

    /** Délai de grâce minimal accepté. En dessous, le risque d'effacer une pièce en cours de dépôt devient réel. */
    private static final Duration MIN_GRACE = Duration.ofHours(1);

    /** Plafond d'objets examinés par passage — garde-fou, pas une cible. */
    private static final int MAX_BATCH = 5000;

    /**
     * Interrupteur du balayage. OFF par défaut, et cette valeur est la seule
     * défendable : l'opération EFFACE des octets qu'aucune sauvegarde applicative
     * ne rappellera. Un administrateur doit l'allumer sciemment, après avoir
     * vérifié que son bucket ne sert qu'à cette plateforme.
     */
    private boolean enabled = false;

    /**
     * Âge minimal d'un objet pour être considéré comme orphelin.
     *
     * <p>24 heures, et non quelques minutes : un objet vient d'être écrit par un
     * {@code put} dont la transaction n'est pas encore validée, et le supprimer
     * ferait disparaître une preuve au moment même où on la verse. Le coût d'un
     * jour d'attente est négligeable — l'orphelin ne bouge pas.
     */
    private Duration gracePeriod = Duration.ofHours(24);

    /**
     * Nombre d'objets examinés par passage. Le balayage est une réconciliation,
     * pas une urgence : mieux vaut plusieurs petits passages qu'un seul qui
     * énumère un bucket entier et tient une connexion ouverte pendant ce temps.
     */
    private int batchSize = 1000;

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Duration getGracePeriod() { return gracePeriod; }

    /**
     * Un délai de grâce trop court est refusé au DÉMARRAGE. L'accepter
     * reviendrait à laisser configurer une suppression de preuves en cours de
     * dépôt — une erreur qui ne se voit qu'une fois les octets perdus.
     */
    public void setGracePeriod(Duration gracePeriod) {
        if (gracePeriod == null || gracePeriod.compareTo(MIN_GRACE) < 0) {
            throw new IllegalArgumentException(
                    "qualitos.storage.orphan-sweep.grace-period doit valoir au moins " + MIN_GRACE);
        }
        this.gracePeriod = gracePeriod;
    }

    public int getBatchSize() { return batchSize; }

    public void setBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "qualitos.storage.orphan-sweep.batch-size doit être strictement positif");
        }
        this.batchSize = Math.min(batchSize, MAX_BATCH);
    }
}
