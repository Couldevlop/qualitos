package com.openlab.qualitos.quality.nonconformity.storage;

import java.net.URL;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implémentation en mémoire de {@link ObjectStorage} pour les tests : pas de
 * réseau, presign déterministe. Conserve les octets et le content-type par clé.
 */
public class InMemoryObjectStorage implements ObjectStorage {

    public record Stored(String contentType, byte[] content) {}

    private final Map<String, Stored> objects = new LinkedHashMap<>();

    @Override
    public void put(String key, String contentType, byte[] content) {
        objects.put(key, new Stored(contentType, content.clone()));
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

    public boolean contains(String key) { return objects.containsKey(key); }

    public int size() { return objects.size(); }

    public Stored get(String key) { return objects.get(key); }
}
