package com.openlab.qualitos.quality.nonconformity;

/**
 * Le préfixe d'une référence d'écart, qui en dit le registre et l'origine.
 *
 * <p>Trois registres se citent dans les mêmes réunions : les constats internes,
 * les réclamations clients, les écarts fournisseurs. Une référence commune à
 * tous — {@code NC-2026-0017} — obligeait à ouvrir la fiche pour savoir de quoi
 * on parlait, et l'on ne pouvait pas trier un courriel, une pièce jointe ou une
 * ligne de tableur sans son contexte. Le préfixe porte l'information là où elle
 * est lue.
 *
 * <p>Ici et non dans chaque service : les trois préfixes doivent rester
 * distincts, et c'est une propriété de l'ENSEMBLE, qu'aucun des trois registres
 * ne peut vérifier seul.
 */
public final class NcReferencePrefix {

    /** Constat détecté par l'organisation elle-même. */
    public static final String INTERNAL = "Int-NC-";

    /** Écart signalé du dehors : client, autorité, organisme certificateur. */
    public static final String EXTERNAL = "Ext-NC-";

    /** Écart imputé à un fournisseur, suivi dans le registre fournisseurs. */
    public static final String SUPPLIER = "Sup-NC-";

    private NcReferencePrefix() {
    }

    /**
     * Le préfixe de cette origine. Une origine absente compte comme interne,
     * comme {@link NcOrigin#orDefault} : classer d'office en « externe » un
     * écart dont personne n'a précisé la provenance gonflerait l'indicateur le
     * plus exposé vis-à-vis des clients.
     */
    public static String of(NcOrigin origin) {
        return NcOrigin.orDefault(origin) == NcOrigin.EXTERNAL ? EXTERNAL : INTERNAL;
    }
}
