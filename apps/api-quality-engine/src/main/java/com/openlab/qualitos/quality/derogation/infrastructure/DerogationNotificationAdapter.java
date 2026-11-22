package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.DerogationLinkBuilder;
import com.openlab.qualitos.quality.derogation.application.DerogationNotificationPort;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.notifications.application.NotificationDto;
import com.openlab.qualitos.quality.notifications.application.NotificationService;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pose les notifications internes du processus sur le dispositif existant.
 *
 * <p>Le <b>lien</b> porté par chaque notification est ce qui fait tout le
 * parcours demandé : « quand il clique il est redirigé vers l'interface de
 * dérogation mais le dossier ouvert ». Une notification sans lien obligerait à
 * retrouver le dossier à la main dans le registre.
 *
 * <p>Le type n'est pas décoratif : {@code ALERT} pour ce qui attend un geste,
 * {@code SUCCESS} pour ce qui en libère un, {@code WARNING} pour ce qui revient
 * à corriger. C'est lui qui donne la couleur du compteur.
 */
@Component
public class DerogationNotificationAdapter implements DerogationNotificationPort {

    private final NotificationService notifications;
    private final DerogationLinkBuilder links;

    public DerogationNotificationAdapter(NotificationService notifications,
                                         DerogationLinkBuilder links) {
        this.notifications = notifications;
        this.links = links;
    }

    @Override
    public void approvalRequested(Derogation d) {
        UUID manager = d.getApproval().managerAccountId();
        if (manager == null) {
            return;
        }
        poser(manager, NotificationType.ALERT,
                "Dérogation à valider : " + d.getReference(),
                corps(d, "Une demande de dérogation attend votre validation."), d);
    }

    @Override
    public void approved(Derogation d) {
        poser(d.getCreatedByUserId(), NotificationType.SUCCESS,
                "Dérogation approuvée : " + d.getReference(),
                corps(d, "Votre demande est approuvée par la qualité."
                        + " Vous pouvez l'envoyer au client."), d);
    }

    @Override
    public void sentBack(Derogation d) {
        String motif = d.getApproval().amendReason();
        poser(d.getCreatedByUserId(), NotificationType.WARNING,
                "Dérogation à corriger : " + d.getReference(),
                corps(d, "Votre demande revient pour correction."
                        + (motif == null ? "" : " Motif : " + motif)), d);
    }

    private void poser(UUID destinataire, NotificationType type, String titre,
                       String corps, Derogation d) {
        notifications.create(new NotificationDto.CreateCommand(
                destinataire.toString(), type, titre, corps, links.linkTo(d.getId())));
    }

    /** Le corps nomme la pièce : sans elle, dix alertes se ressemblent. */
    private static String corps(Derogation d, String phrase) {
        String piece = d.getIdentification() == null
                ? null : d.getIdentification().partReference();
        return piece == null ? phrase : phrase + " Pièce : " + piece + ".";
    }
}
