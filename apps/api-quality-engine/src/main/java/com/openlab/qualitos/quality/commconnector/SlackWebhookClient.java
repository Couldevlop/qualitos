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
 * Client Slack (incoming webhook). Combine un attachment coloré (barre latérale =
 * sévérité, supporté de longue date et stable) avec des Block Kit blocks pour un rendu
 * premium (titre en header, facts en section markdown, bouton d'action).
 */
@Component
public class SlackWebhookClient extends AbstractWebhookCommClient {

    @Autowired
    public SlackWebhookClient(ObjectMapper mapper) {
        super(mapper, defaultClient());
    }

    SlackWebhookClient(ObjectMapper mapper, RestClient client) {
        super(mapper, client);
    }

    @Override
    public CommProvider provider() { return CommProvider.SLACK; }

    @Override
    protected Object buildPayload(CommConnection connection, CommMessage message) {
        List<Map<String, Object>> blocks = new ArrayList<>();

        // Header (titre + emoji sévérité).
        blocks.add(Map.of(
                "type", "header",
                "text", Map.of("type", "plain_text",
                        "text", message.severity().emoji() + " " + message.title(), "emoji", true)));

        if (message.text() != null) {
            blocks.add(Map.of("type", "section",
                    "text", Map.of("type", "mrkdwn", "text", message.text())));
        }

        if (!message.facts().isEmpty()) {
            List<Map<String, String>> fields = new ArrayList<>();
            for (Map.Entry<String, String> f : message.facts()) {
                fields.add(Map.of("type", "mrkdwn",
                        "text", "*" + f.getKey() + "*\n" + (f.getValue() == null ? "" : f.getValue())));
            }
            blocks.add(Map.of("type", "section", "fields", fields));
        }

        if (message.linkUrl() != null) {
            Map<String, Object> button = new LinkedHashMap<>();
            button.put("type", "button");
            button.put("text", Map.of("type", "plain_text",
                    "text", message.linkLabel() != null ? message.linkLabel() : "Ouvrir dans QualitOS"));
            button.put("url", message.linkUrl());
            blocks.add(Map.of("type", "actions", "elements", List.of(button)));
        }

        // L'attachment porte la couleur (barre latérale = sévérité) et embarque les blocks.
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", "#" + message.severity().hex());
        attachment.put("blocks", blocks);

        Map<String, Object> payload = new LinkedHashMap<>();
        // Fallback texte (notifications, clients sans blocks).
        payload.put("text", message.severity().emoji() + " " + message.title());
        if (connection.getChannel() != null && !connection.getChannel().isBlank()) {
            payload.put("channel", connection.getChannel());
        }
        payload.put("attachments", List.of(attachment));
        return payload;
    }
}
