package com.openlab.qualitos.quality.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Déclare le module du catalogue dont dépend un point d'entrée (§10.4).
 *
 * <h2>Pourquoi côté serveur</h2>
 *
 * <p>Les modules sont une frontière <b>commerciale</b> : un tenant active — et
 * paye — ce dont il a besoin. Or jusqu'ici cette frontière n'existait que dans
 * la barre de navigation, sur deux entrées, et rien n'empêchait un tenant sans
 * abonnement d'appeler l'API. Une porte qu'on se contente de ne pas afficher
 * n'est pas fermée.
 *
 * <h2>L'écriture est fermée, la lecture ne l'est jamais</h2>
 *
 * <p>Seules les méthodes qui MODIFIENT sont refusées. Un control plan, un PFMEA,
 * un produit sont des enregistrements qualité <b>opposables</b> : si résilier un
 * module rendait illisibles les preuves d'un audit déjà passé, une décision
 * commerciale se transformerait en perte de conformité, et le client ne pourrait
 * plus répondre à son auditeur. On ferme ce qui fait grossir le dossier, jamais
 * ce qui permet de le relire.
 *
 * <p>Le risque de fuite est nul dans l'autre sens : un tenant qui n'a jamais eu
 * le module n'a aucune donnée à lire.
 *
 * <h2>Ce que l'annotation ne fait pas</h2>
 *
 * <p>Elle ne remplace pas {@code @PreAuthorize}. Les deux répondent à des
 * questions différentes et se cumulent : le module dit ce que l'ORGANISATION a
 * souscrit, le rôle dit ce que la PERSONNE a le droit d'y faire. Un manager
 * qualité d'un tenant sans le module est refusé sur le module ; un simple
 * utilisateur d'un tenant qui l'a est refusé sur le rôle.
 *
 * <p>Les modules du <b>socle</b> ne s'annotent pas : ils sont acquis d'office
 * (plancher du catalogue), et les annoter ferait dépendre d'une ligne
 * d'activation ce qui n'en a jamais.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresModule {

    /** Code du module dans {@code ModuleCatalog} — p. ex. {@code "controlplan"}. */
    String value();
}
