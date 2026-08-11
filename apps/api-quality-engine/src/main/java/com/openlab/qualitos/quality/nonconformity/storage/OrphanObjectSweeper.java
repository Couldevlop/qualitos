package com.openlab.qualitos.quality.nonconformity.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Balayage des binaires orphelins du stockage objet.
 *
 * <p><b>D'où vient un orphelin.</b> Le dépôt écrit la ligne de métadonnées, puis
 * l'objet ; le retrait fait l'inverse. Cet ordre garantit qu'aucune ligne ne
 * pointe jamais vers un objet absent — le cas qui casserait un écran. Il laisse
 * en revanche ouverte la situation symétrique : l'objet est écrit, puis la
 * transaction échoue (validation refusée, journal d'audit indisponible, pod tué
 * entre les deux). La ligne disparaît, l'objet reste. Il devient alors
 * introuvable par l'application — rien, dans la base, ne dit qu'il existe — et
 * il est facturé indéfiniment. Sur des photos de non-conformité prises au
 * téléphone, quelques milliers d'échecs finissent par peser.
 *
 * <p><b>Pourquoi un balayage et non une compensation.</b> Supprimer l'objet dans
 * un {@code catch} supposerait que le processus survive à l'incident, ce qui est
 * précisément ce qu'un pod tué ne fait pas. Le seul mécanisme qui rattrape TOUS
 * les cas est une réconciliation périodique entre ce que contient le stockage et
 * ce que la base revendique.
 *
 * <p><b>Prudence.</b> L'opération efface des octets et rien ne les rappellera.
 * Trois garde-fous : le balayage est DÉSACTIVÉ par défaut ; il ignore tout objet
 * plus récent que le délai de grâce (sans quoi il supprimerait une pièce en cours
 * de dépôt, entre le {@code put} et la validation de la transaction) ; et il ne
 * supprime que ce qu'AUCUN propriétaire déclaré ne revendique — une erreur de
 * dépôt interrompt le balayage plutôt que de faire passer un objet vivant pour
 * un orphelin.
 */
@Service
public class OrphanObjectSweeper {

    private static final Logger log = LoggerFactory.getLogger(OrphanObjectSweeper.class);

    /**
     * Racine des clés produites par la plateforme. Le balayage s'y limite : un
     * bucket peut être partagé, et effacer ce qu'un autre outil y dépose serait
     * exactement le genre de dégât qu'on cherche à éviter.
     */
    static final String KEY_ROOT = "tenants/";

    private final ObjectProvider<ObjectStorage> storageProvider;
    private final List<StoredObjectOwner> owners;
    private final OrphanSweepProperties props;
    private final Clock clock;

    public OrphanObjectSweeper(ObjectProvider<ObjectStorage> storageProvider,
                               List<StoredObjectOwner> owners,
                               OrphanSweepProperties props,
                               Clock clock) {
        this.storageProvider = storageProvider;
        this.owners = owners;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Un passage complet.
     *
     * @return le compte rendu, utile aux tests et au journal.
     */
    public Report sweep() {
        if (!props.isEnabled()) {
            return Report.disabled();
        }
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            // Stockage objet éteint : il n'y a pas d'objet, donc pas d'orphelin.
            return Report.disabled();
        }
        if (owners.isEmpty()) {
            // Aucun propriétaire déclaré : TOUT paraîtrait orphelin. Le cas ne
            // devrait pas se produire, et s'il se produit il vaut mieux ne rien
            // faire que vider le bucket au motif que personne n'a parlé.
            log.warn("[orphan-sweep] aucun propriétaire d'objets déclaré — balayage annulé");
            return Report.disabled();
        }

        Instant cutoff = clock.instant().minus(props.getGracePeriod());
        List<ObjectStorage.StoredObject> candidates = storage.list(KEY_ROOT, props.getBatchSize());

        int examined = 0;
        int tooRecent = 0;
        int deleted = 0;
        long bytesReclaimed = 0;
        int failures = 0;

        for (ObjectStorage.StoredObject object : candidates) {
            examined++;
            if (object.lastModified() == null || object.lastModified().isAfter(cutoff)) {
                // Trop récent : peut-être une pièce dont la transaction n'est pas
                // encore validée. On la reverra au passage suivant, vieillie.
                tooRecent++;
                continue;
            }
            try {
                if (isReferenced(object.key())) {
                    continue;
                }
                storage.delete(object.key());
                deleted++;
                bytesReclaimed += object.sizeBytes();
                // La clé est journalisée : c'est un chemin de stockage, pas une
                // donnée personnelle, et sans elle une suppression injustifiée
                // serait impossible à instruire après coup.
                log.info("[orphan-sweep] binaire orphelin supprimé : {} ({} octets)",
                        object.key(), object.sizeBytes());
            } catch (RuntimeException e) {
                // Un objet qui résiste ne doit pas emporter les suivants ; et en
                // cas de doute sur son appartenance, on ne supprime pas.
                failures++;
                log.error("[orphan-sweep] objet {} laissé en place : {}", object.key(), e.getMessage());
            }
        }

        if (deleted > 0 || failures > 0) {
            log.info("[orphan-sweep] {} objet(s) examiné(s), {} supprimé(s) ({} octets récupérés), "
                            + "{} trop récent(s), {} en échec",
                    examined, deleted, bytesReclaimed, tooRecent, failures);
        }
        return new Report(true, examined, tooRecent, deleted, bytesReclaimed, failures);
    }

    /**
     * Vrai dès qu'un propriétaire revendique la clé. On s'arrête au premier :
     * une clé n'appartient qu'à un module, et interroger les autres ne
     * changerait rien à la décision.
     */
    private boolean isReferenced(String key) {
        for (StoredObjectOwner owner : owners) {
            if (owner.isReferenced(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compte rendu d'un passage. {@code ran} distingue « rien à faire » de
     * « balayage éteint » — deux situations que le même zéro confondrait.
     */
    public record Report(boolean ran, int examined, int tooRecent,
                         int deleted, long bytesReclaimed, int failures) {

        static Report disabled() {
            return new Report(false, 0, 0, 0, 0L, 0);
        }
    }
}
