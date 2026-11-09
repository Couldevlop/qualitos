package com.openlab.qualitos.quality.stakeholders.application;

import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ce dont le registre des parties intéressées a besoin du dehors : qui agit, à
 * qui ouvrir une CAPA, et ce que l'IA propose.
 */
public final class StakeholderPorts {

    private StakeholderPorts() {}

    /**
     * Qui agit, et pour quel client — les deux venant du jeton validé, jamais
     * du corps de la requête (§18.2 #2).
     */
    public interface Context {

        UUID requireTenantId();

        UUID requireActorId();

        /** Le nom d'annuaire de l'acteur, recopié sur ce qu'il crée. */
        String actorName();
    }

    /**
     * Port vers le module CAPA : ouvrir un dossier depuis une exigence non
     * tenue.
     *
     * <p>Le dossier est ouvert par le module CAPA lui-même (ADR 0088) : il naît
     * avec son action, son journal et ses notifications, comme depuis n'importe
     * quel autre écran.
     */
    public interface CapaGateway {

        /**
         * @param reference ce que l'utilisateur reconnaîtra dans le registre CAPA :
         *                  « PI-0003 / exigence 2 »
         * @return l'identifiant du dossier ouvert
         */
        UUID open(String reference, String title, String description, UUID ownerId, String ownerName,
                  LocalDate dueDate);
    }

    /**
     * Port vers le registre des risques : inscrire le risque qu'une exigence
     * fait courir si elle n'est pas tenue.
     *
     * <p>La ligne est créée par le module du registre lui-même, comme depuis
     * n'importe quel autre écran : elle naît donc avec sa référence, sa
     * cotation, son journal et sa tracabilite. Le registre des parties
     * intéressées n'écrit pas dans celui des risques ; il le lui demande.
     */
    public interface RiskGateway {

        /**
         * @param originRef ce qu'on lira dans le registre des risques pour savoir
         *                  d'où vient la ligne : « Client X / exigence … »
         * @return l'identifiant de la ligne inscrite
         */
        UUID open(NewRisk risk);
    }

    /**
     * Le risque qu'une exigence fait courir.
     *
     * <p>Le titre, la cause et l'effet sont tirés de l'exigence, car les
     * ressaisir produirait deux énoncés du même risque. L'écran apporte ce que
     * l'exigence ne contient pas et que le registre exige de toute ligne : la
     * nature, les deux notes, le processus concerné et la personne qui en
     * répond.
     *
     * @param originRef   la citation de l'exigence, pour le registre des risques
     * @param title       le risque en une ligne
     * @param cause       ce qui le déclenche — ici, l'exigence elle-même
     * @param effect      ce qu'on redoute si elle n'est pas tenue
     * @param type        la nature du risque, par le nom de l'énumération du registre
     * @param process     le processus où le risque se matérialise
     * @param severity    la gravité, de 1 à 5
     * @param probability la probabilité, de 1 à 5
     * @param ownerId     le compte qui en répond
     * @param ownerName   son nom d'annuaire, tel que le registre l'affichera
     */
    public record NewRisk(String originRef, String title, String cause, String effect, String type,
                          String process, int severity, int probability, UUID ownerId,
                          String ownerName) {}

    /**
     * Port vers l'IA : elle PROPOSE, le responsable qualité décide. Rien de ce
     * que rend ce port n'entre dans le registre sans un appel de validation
     * explicite — c'est la règle de §12.3, et ici elle est structurelle : le
     * port ne sait pas écrire.
     */
    public interface Assistant {

        /**
         * Le texte d'un document soumis à l'assistant, en caractères.
         *
         * <p>La borne vient de la chaîne, pas d'un choix de confort : l'invite
         * assemblée doit tenir sous {@code qualitos.ai.guard.max-prompt-chars}
         * (20 000, ADR 0017), qui est aussi la borne de {@code ai-service}. On
         * réserve la consigne système et l'en-tête du document.
         *
         * <p>Elle est déclarée ici pour que le contrat d'API et l'écran annoncent
         * la MÊME valeur. Quand ils annonçaient 200 000, l'utilisateur collait un
         * contrat entier et l'appel partait pour être refusé en bloc par la
         * passerelle : il lisait « aucune exigence trouvée » sur un document
         * parfaitement lisible.
         */
        int DOCUMENT_MAX = 17_000;

        /**
         * Chacune des deux versions comparées. Elles voyagent dans la même invite,
         * donc chacune n'a droit qu'à la moitié du budget, en-têtes déduits.
         */
        int COMPARED_MAX = 8_000;

        /**
         * Des parties intéressées plausibles pour ce secteur et cette
         * organisation. Aucune n'est inscrite : l'écran les propose une à une.
         */
        List<SuggestedStakeholder> suggestStakeholders(String sector, String characteristics);

        /**
         * Les exigences qu'un contrat ou un cahier des charges porte, chacune
         * CITÉE dans le texte d'origine : une exigence sans citation ne se
         * vérifie pas, et l'auditeur demandera d'où elle vient.
         */
        List<ExtractedRequirement> extractRequirements(String documentName, String documentText);

        /**
         * Ce qui a changé entre deux versions d'un même document, et ce que cela
         * remet en cause.
         */
        List<DetectedChange> detectChanges(String documentName, String previousText, String currentText);
    }

    /** Une partie intéressée proposée par l'IA, pas encore inscrite. */
    public record SuggestedStakeholder(String name, StakeholderCategory category, String rationale) {}

    /**
     * Une exigence tirée d'un document.
     *
     * @param quote      le passage exact du document — la preuve de la lecture
     * @param reference  où le retrouver : article, paragraphe, page
     * @param requirement l'exigence reformulée, telle qu'on s'engagerait à la tenir
     * @param nature     ce qui la rend opposable
     * @param control    la mesure de maîtrise suggérée
     */
    public record ExtractedRequirement(String quote, String reference, String requirement,
                                       MandatoryNature nature, String control) {}

    /**
     * Un changement détecté entre deux versions.
     *
     * @param title  ce qui a changé, en une ligne
     * @param detail ce que dit le nouveau texte
     * @param source où le constater
     * @param impact la portée estimée sur les exigences en place
     */
    public record DetectedChange(String title, String detail, String source, ImpactLevel impact) {}
}
