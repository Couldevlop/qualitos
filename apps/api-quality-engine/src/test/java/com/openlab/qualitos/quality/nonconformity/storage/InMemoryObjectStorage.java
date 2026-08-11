package com.openlab.qualitos.quality.nonconformity.storage;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation en mémoire de {@link ObjectStorage} pour les tests : pas de
 * réseau, presign déterministe. Conserve les octets et le content-type par clé.
 */
public class InMemoryObjectStorage implements ObjectStorage {

    public record Stored(String contentType, byte[] content, Instant lastModified) {}

    private final Map<String, Stored> objects = new LinkedHashMap<>();

    /**
     * Date attribuée aux prochains dépôts. Réglable : le balayage des orphelins
     * distingue un objet ancien d'un objet tout juste écrit, et un test de cette
     * règle doit pouvoir poser un objet « vieux d'une semaine » sans attendre.
     */
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Override
    public void put(String key, String contentType, byte[] content) {
        objects.put(key, new Stored(contentType, content.clone(), now));
    }

    @Override
    public URL presignGet(String key, Duration ttl) {
        try {
            return java.net.URI.create(
                    "https://storage.test/" + key + "?ttl=" + ttl.getSeconds()).toURL();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void delete(String key) {
        objects.remove(key); // idempotent
    }

    @Override
    public List<StoredObject> list(String prefix, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<StoredObject> out = new ArrayList<>();
        for (Map.Entry<String, Stored> e : objects.entrySet()) {
            if (!e.getKey().startsWith(prefix)) {
                continue;
            }
            out.add(new StoredObject(e.getKey(), e.getValue().lastModified(),
                    e.getValue().content().length));
            if (out.size() >= limit) {
                break;
            }
        }
        return List.copyOf(out);
    }

    /** Fixe la date attribuée aux dépôts suivants — cf. {@link #now}. */
    public void setNow(Instant instant) { this.now = instant; }

    public boolean contains(String key) { return objects.containsKey(key); }

    public int size() { return objects.size(); }

    public Stored get(String key) { return objects.get(key); }
}
