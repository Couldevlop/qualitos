package com.openlab.qualitos.core.secret;

/**
 * Chiffre et déchiffre un secret que la plateforme doit pouvoir RELIRE.
 *
 * <p>C'est le premier de ce genre ici, et la distinction mérite d'être posée.
 * Un mot de passe d'utilisateur ou une clé d'API se <b>hachent</b> : on ne les
 * relit jamais, on compare une empreinte, et c'est pour cela que leur vol en
 * base ne donne rien. Un mot de passe SMTP, lui, doit être présenté tel quel au
 * serveur de messagerie : il faut pouvoir le <b>retrouver</b>, donc le
 * chiffrer.
 *
 * <p>La conséquence est que la sécurité du secret ne tient plus à la base mais
 * à <b>la clé, qui ne doit jamais s'y trouver</b>. Une copie de la base volée
 * sans la clé ne donne rien ; la clé vit dans l'environnement (§18.2.3 : aucun
 * secret en clair dans le dépôt).
 *
 * <p>Le port existe pour que l'algorithme reste remplaçable — c'est l'exigence
 * de crypto-agilité de {@code CLAUDE.md} §11.4 : le jour où un coffre externe
 * prend le relais, les appelants ne changent pas.
 */
public interface SecretCipher {

    /**
     * Chiffre un secret en clair.
     *
     * @param clair le secret, non nul et non vide — chiffrer du vide
     *              produirait un cryptogramme qui se déchiffre en vide, ce qui
     *              se lirait comme « un mot de passe est configuré » alors
     *              qu'il n'y en a pas
     * @return le cryptogramme, prêt à stocker (texte, pour qu'il traverse une
     *         colonne et un export sans encodage de plus)
     */
    String chiffrer(String clair);

    /**
     * Déchiffre un cryptagramme produit par {@link #chiffrer}.
     *
     * @throws SecretCipherException si le cryptogramme a été altéré, tronqué,
     *                               ou chiffré avec une autre clé. On ne rend
     *                               JAMAIS une valeur approximative : un mot de
     *                               passe à demi déchiffré partirait vers un
     *                               serveur tiers.
     */
    String dechiffrer(String cryptogramme);
}
