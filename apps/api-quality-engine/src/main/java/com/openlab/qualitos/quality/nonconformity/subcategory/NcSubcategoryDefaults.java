package com.openlab.qualitos.quality.nonconformity.subcategory;

import java.util.List;

import static com.openlab.qualitos.quality.nonconformity.NcCategory.DOCUMENTATION;
import static com.openlab.qualitos.quality.nonconformity.NcCategory.ENVIRONMENT;
import static com.openlab.qualitos.quality.nonconformity.NcCategory.OTHER;
import static com.openlab.qualitos.quality.nonconformity.NcCategory.PROCESS;
import static com.openlab.qualitos.quality.nonconformity.NcCategory.PRODUCT;
import static com.openlab.qualitos.quality.nonconformity.NcCategory.SAFETY;
import static com.openlab.qualitos.quality.nonconformity.NcCategory.SUPPLIER;

/**
 * Le catalogue de sous-catégories de la plateforme : la nature d'un écart, sous
 * chacune des sept catégories.
 *
 * <p>Il est volontairement TRANSVERSE. Une même plateforme sert l'industrie, la
 * santé, le laboratoire, la banque, l'informatique, l'agroalimentaire, la
 * logistique, le BTP, l'énergie, le secteur public et l'éducation : chaque
 * nature retenue ici se rencontre dans plusieurs de ces métiers, et celles qui
 * n'existent que dans un seul (sécurité du patient, point critique HACCP,
 * suspicion de contrefaçon aéronautique) sont nommées sans emprunter le
 * vocabulaire d'un référentiel particulier.
 *
 * <p>C'est un POINT DE DÉPART, pas une vérité : un client qui n'a rien réglé
 * classe ses écarts dès le premier jour, et celui qui a sa propre nomenclature
 * la substitue entièrement (paramétrage réservé à l'administrateur du client).
 */
public final class NcSubcategoryDefaults {

    private NcSubcategoryDefaults() {}

    public static final List<NcSubcategory> CATALOGUE = List.of(
            // ---- Produit ou prestation : ce que le client reçoit ----
            s("PRD-DIM", PRODUCT, "Cote ou tolérance hors spécification"),
            s("PRD-ASP", PRODUCT, "Aspect, finition ou état de surface"),
            s("PRD-FON", PRODUCT, "Fonctionnement ou performance insuffisants"),
            s("PRD-MAT", PRODUCT, "Matière ou composant non conforme"),
            s("PRD-ASS", PRODUCT, "Assemblage, montage ou réglage"),
            s("PRD-EMB", PRODUCT, "Emballage, conditionnement ou étiquetage"),
            s("PRD-QTE", PRODUCT, "Quantité : manquant, surplus ou mélange"),
            s("PRD-CTA", PRODUCT, "Contamination ou corps étranger"),
            s("PRD-PER", PRODUCT, "Péremption ou durée de vie dépassée"),
            s("PRD-SRV", PRODUCT, "Prestation de service non conforme à l'engagement"),
            s("PRD-LOG", PRODUCT, "Anomalie logicielle ou défaut applicatif"),
            s("PRD-SOI", PRODUCT, "Acte de soin ou examen non conforme au protocole"),

            // ---- Processus : la façon de produire ----
            s("PRC-DER", PROCESS, "Dérive de paramètre ou sortie des limites de contrôle"),
            s("PRC-PRO", PROCESS, "Procédure ou mode opératoire non respecté"),
            s("PRC-EQP", PROCESS, "Défaillance d'équipement ou d'installation"),
            s("PRC-ETA", PROCESS, "Étalonnage ou métrologie hors validité"),
            s("PRC-MSA", PROCESS, "Système de mesure non capable (MSA, Gage R&R)"),
            s("PRC-MNT", PROCESS, "Maintenance préventive non réalisée"),
            s("PRC-HYG", PROCESS, "Nettoyage, hygiène ou désinfection insuffisants"),
            s("PRC-TRA", PROCESS, "Traçabilité ou identification perdue"),
            s("PRC-DEL", PROCESS, "Délai, retard ou engagement de service non tenu"),
            s("PRC-CHG", PROCESS, "Changement non maîtrisé ou non validé"),
            s("PRC-CMP", PROCESS, "Compétence, habilitation ou autorisation manquante"),
            s("PRC-INF", PROCESS, "Interruption ou indisponibilité de service"),
            s("PRC-SAI", PROCESS, "Erreur de saisie ou de transcription de données"),

            // ---- Documentation : la preuve écrite ----
            s("DOC-MAN", DOCUMENTATION, "Enregistrement ou preuve manquante"),
            s("DOC-OBS", DOCUMENTATION, "Version périmée ou document obsolète en usage"),
            s("DOC-INC", DOCUMENTATION, "Renseignement incomplet ou illisible"),
            s("DOC-SIG", DOCUMENTATION, "Signature, revue ou approbation manquante"),
            s("DOC-INT", DOCUMENTATION, "Intégrité des données en défaut (ALCOA+)"),
            s("DOC-ARC", DOCUMENTATION, "Archivage ou durée de conservation non respectés"),
            s("DOC-DIF", DOCUMENTATION, "Diffusion, accès ou lecture obligatoire non assurés"),
            s("DOC-NOT", DOCUMENTATION, "Notice, instruction ou mention réglementaire erronée"),

            // ---- Fournisseur et sous-traitance ----
            s("FOU-REC", SUPPLIER, "Matière ou pièce réceptionnée non conforme"),
            s("FOU-CER", SUPPLIER, "Certificat matière, CoA ou rapport de contrôle manquant"),
            s("FOU-RET", SUPPLIER, "Retard de livraison"),
            s("FOU-QTE", SUPPLIER, "Écart de quantité livrée"),
            s("FOU-STR", SUPPLIER, "Prestation sous-traitée non conforme"),
            s("FOU-MOD", SUPPLIER, "Modification de produit ou de procédé non notifiée"),
            s("FOU-CTF", SUPPLIER, "Pièce suspecte ou soupçon de contrefaçon"),
            s("FOU-8D", SUPPLIER, "Réponse 8D absente, tardive ou insuffisante"),

            // ---- Sécurité, au sens large ----
            s("SEC-PRQ", SAFETY, "Presqu'accident ou situation dangereuse"),
            s("SEC-ACC", SAFETY, "Accident avec blessure"),
            s("SEC-EPI", SAFETY, "Équipement de protection absent ou inadapté"),
            s("SEC-MAC", SAFETY, "Protection machine ou consignation défaillante"),
            s("SEC-CHM", SAFETY, "Exposition chimique ou biologique"),
            s("SEC-INC", SAFETY, "Incendie, explosion ou atmosphère explosive"),
            s("SEC-ERG", SAFETY, "Contrainte ergonomique ou trouble musculo-squelettique"),
            s("SEC-EIG", SAFETY, "Événement indésirable touchant un patient ou un usager"),
            s("SEC-ALM", SAFETY, "Sécurité des aliments : point critique en excursion"),
            s("SEC-SSI", SAFETY, "Incident de sécurité de l'information"),
            s("SEC-SUR", SAFETY, "Sûreté : intrusion, vol ou malveillance"),
            s("SEC-RTE", SAFETY, "Sécurité routière ou du transport"),

            // ---- Environnement ----
            s("ENV-DEV", ENVIRONMENT, "Déversement, fuite ou épandage accidentel"),
            s("ENV-DEC", ENVIRONMENT, "Gestion ou tri des déchets non conforme"),
            s("ENV-AIR", ENVIRONMENT, "Émission atmosphérique hors seuil"),
            s("ENV-EAU", ENVIRONMENT, "Rejet aqueux hors seuil"),
            s("ENV-BRU", ENVIRONMENT, "Bruit, odeur ou nuisance de voisinage"),
            s("ENV-NRG", ENVIRONMENT, "Surconsommation d'énergie ou de ressource"),
            s("ENV-SOL", ENVIRONMENT, "Atteinte au sol, à l'eau ou à la biodiversité"),

            // ---- Autres origines ----
            s("AUT-REC", OTHER, "Réclamation client ou usager"),
            s("AUT-REG", OTHER, "Constat d'inspection ou d'autorité réglementaire"),
            s("AUT-AUD", OTHER, "Constat d'audit interne ou de certification"),
            s("AUT-RGP", OTHER, "Donnée personnelle : violation ou usage non prévu"),
            s("AUT-OPE", OTHER, "Erreur opérationnelle ou financière"),
            s("AUT-ETH", OTHER, "Manquement éthique ou de conformité"),
            s("AUT-NCL", OTHER, "Autre, à classer"));

    private static NcSubcategory s(String code, com.openlab.qualitos.quality.nonconformity.NcCategory category,
                                  String label) {
        return new NcSubcategory(code, category, label);
    }
}
