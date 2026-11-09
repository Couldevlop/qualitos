package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.riskregister.application.RiskRegisterDto;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.riskregister.domain.Identification;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderValidationException;

import java.util.UUID;

/**
 * Port vers le registre des risques : inscrire le risque qu'une exigence de
 * partie intéressée fait courir.
 *
 * <p>L'inscription passe par {@link RiskRegisterService#createRisk} et non par
 * le dépôt : la ligne naît ainsi avec sa référence attribuée, sa cotation
 * calculée, son journal et sa trace d'audit, exactement comme depuis l'écran du
 * registre.
 *
 * <p>L'origine est {@link RegisterOrigin#STAKEHOLDER}, qui ne porte pas de
 * {@code sourceId} : l'exigence n'est pas un objet autonome que le registre
 * pourrait relire, c'est une ligne dans la fiche d'une partie intéressée. La
 * citation en tient lieu, et le rattachement inverse — l'exigence qui porte
 * l'identifiant du risque — ferme la boucle.
 *
 * <p>Le processus et le propriétaire viennent de l'écran : le registre les
 * exige de toute ligne, et le registre des parties intéressées ne les porte
 * pas. Les déduire — « Parties intéressées » comme processus, le responsable
 * en clair comme propriétaire — inscrirait un processus qui n'existe pas dans
 * la cartographie et un propriétaire que personne n'a désigné.
 *
 * <p>Ni décision ni statut ne sont imposés : ils se prononcent dans la fiche du
 * risque, qui est l'écran qui les pilote. Inventer ici une décision que
 * personne n'a prise ferait apparaître un risque déjà arbitré.
 */
public class RiskStakeholderGateway implements StakeholderPorts.RiskGateway {

    private final RiskRegisterService riskRegister;

    public RiskStakeholderGateway(RiskRegisterService riskRegister) {
        this.riskRegister = riskRegister;
    }

    @Override
    public UUID open(StakeholderPorts.NewRisk risk) {
        RiskRegisterDto.RiskCommand command = new RiskRegisterDto.RiskCommand(
                abrege(risk.title(), Identification.TITLE_MAX),
                nature(risk.type()), risk.process(), null, risk.ownerName(),
                risk.cause(), risk.effect(), RegisterOrigin.STAKEHOLDER,
                abrege(risk.originRef(), Identification.ORIGIN_REF_MAX), null,
                risk.severity(), risk.probability(), null, null,
                null, null, null, null, null, risk.ownerId());
        return riskRegister.createRisk(command).id();
    }

    /**
     * Les champs d'une exigence vont jusqu'à 500 caractères, l'intitulé du
     * registre s'arrête à 255 et sa citation d'origine à 120.
     *
     * <p>On abrège plutôt que de refuser : l'énoncé complet part dans l'effet
     * redouté, que le registre stocke sur 4 000 caractères, et l'exigence
     * elle-même reste lisible à un clic. Refuser l'inscription parce qu'une
     * phrase est longue laisserait le risque hors du registre, ce qui est
     * exactement ce que ce chemin existe pour éviter. Le caractère de
     * continuation prévient le lecteur que la phrase est tronquée.
     */
    private static String abrege(String value, int max) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        return v.length() <= max ? v : v.substring(0, max - 1).strip() + "…";
    }

    /**
     * La nature arrive par son nom plutôt que par l'énumération du registre :
     * le module des parties intéressées ne connaît pas les types de l'autre, et
     * c'est volontaire. Un nom inconnu est un refus explicite, pas un
     * rabattement silencieux sur « Qualité » : le registre afficherait alors
     * une nature que personne n'a choisie.
     */
    private static RegisterType nature(String name) {
        if (name == null || name.isBlank()) {
            throw new StakeholderValidationException("A risk needs its kind");
        }
        try {
            return RegisterType.valueOf(name.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new StakeholderValidationException("Unknown risk kind: " + name);
        }
    }
}
