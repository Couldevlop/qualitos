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
 * Client Mattermost (incoming webhook). Mattermost accepte un {@code text} markdown +
 * des {@code attachments} de style Slack (color, fields, title/title_link). On produit
 * un message premium : titre coloré, facts en short fields, lien d'action en title_link.
 */
@Component
public class MattermostWebhookClient extends AbstractWebhookCommClient {

    @Autowired
    public MattermostWebhookClient(ObjectMapper mapper) {
        super(mapper, defaultClient());
    }

    MattermostWebhookClient(ObjectMapper mapper, RestClient client) {
        super(mapper, client);
    }

    @Override
    public CommProvider provider() { return CommProvider.MATTERMOST; }

    @Override
    protected Object buildPayload(CommConnection connection, CommMessage message) {
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", "#" + message.severity().hex());
        attachment.put("title", message.severity().emoji() + " " + message.title());
        if (message.linkUrl() != null) {
            attachment.put("title_link", message.linkUrl());
        }
        if (message.text() != null) {
            attachment.put("text", message.text());
        }
        if (!message.facts().isEmpty()) {
            List<Map<String, Object>> fields = new ArrayList<>();
            for (Map.Entry<String, String> f : message.facts()) {
                fields.add(Map.of("title", f.getKey(),
                        "value", f.getValue() == null ? "" : f.getValue(), "short", true));
            }
            attachment.put("fields", fields);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        // text = ligne de notification (toujours rendue, même sans support attachments).
        payload.put("text", "**" + message.severity().emoji() + " " + message.title() + "**");
        if (connection.getChannel() != null && !connection.getChannel().isBlank()) {
            payload.put("channel", connection.getChannel());
        }
        payload.put("attachments", List.of(attachment));
        return payload;
    }
}
