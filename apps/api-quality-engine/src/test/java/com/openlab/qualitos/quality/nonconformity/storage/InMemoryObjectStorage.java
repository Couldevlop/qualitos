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

    /**
     * Énumère dans l'ORDRE DES CLÉS, comme S3 — et non dans l'ordre d'insertion.
     * Sans ce tri, le curseur de reprise du balayeur ne pourrait pas se tester :
     * « après cette clé » n'a de sens que sur une énumération ordonnée.
     */
    @Override
    public List<StoredObject> list(String prefix, String startAfter, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<StoredObject> out = new ArrayList<>();
        List<String> keys = new ArrayList<>(objects.keySet());
        java.util.Collections.sort(keys);
        for (String key : keys) {
            Map.Entry<String, Stored> e = Map.entry(key, objects.get(key));
            if (!e.getKey().startsWith(prefix)) {
                continue;
            }
            if (startAfter != null && e.getKey().compareTo(startAfter) <= 0) {
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
