package com.openlab.qualitos.quality.commconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client Microsoft Teams (incoming webhook). Format <b>MessageCard</b> (Office 365
 * Connector Card) : largement supporté, themeColor pour la sévérité, sections de facts
 * et potentialAction OpenUri pour le bouton de lien.
 */
@Component
public class TeamsWebhookClient extends AbstractWebhookCommClient {

    @Autowired
    public TeamsWebhookClient(ObjectMapper mapper) {
        super(mapper, defaultClient());
    }

    // Exposé pour les tests : RestClient pointé sur un serveur local.
    TeamsWebhookClient(ObjectMapper mapper, RestClient client) {
        super(mapper, client);
    }

    @Override
    public CommProvider provider() { return CommProvider.TEAMS; }

    @Override
    protected Object buildPayload(CommConnection connection, CommMessage message) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("@type", "MessageCard");
        card.put("@context", "https://schema.org/extensions");
        card.put("themeColor", message.severity().hex());
        card.put("summary", message.title());

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("activityTitle", "**" + message.severity().emoji() + " " + message.title() + "**");
        if (message.text() != null) {
            section.put("text", message.text());
        }
        if (!message.facts().isEmpty()) {
            List<Map<String, String>> facts = new ArrayList<>();
            for (Map.Entry<String, String> f : message.facts()) {
                facts.add(Map.of("name", f.getKey(), "value", f.getValue() == null ? "" : f.getValue()));
            }
            section.put("facts", facts);
        }
        card.put("sections", List.of(section));

        if (message.linkUrl() != null) {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("@type", "OpenUri");
            action.put("name", message.linkLabel() != null ? message.linkLabel() : "Ouvrir dans QualitOS");
            action.put("targets", List.of(Map.of("os", "default", "uri", message.linkUrl())));
            card.put("potentialAction", List.of(action));
        }
        return card;
    }
}
