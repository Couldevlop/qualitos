package com.openlab.qualitos.licensing.infrastructure;

import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * Les clés de l'éditeur.
 *
 * <p>Les clés PUBLIQUES sont épinglées dans l'application
 * ({@code /qualitos-license/editor-public-keys.properties}) : c'est elles que toute
 * installation exige. Les clés PRIVÉES ne quittent jamais le poste de l'éditeur ;
 * on les génère et on les relit ici pour émettre les licences.
 */
public final class EditorKeys {

    public static final String PINNED_RESOURCE = "/qualitos-license/editor-public-keys.properties";

    private EditorKeys() {}

    /** Les clés publiques épinglées dans l'application. */
    public static Map<SignatureAlgorithm, byte[]> pinned() {
        try (InputStream in = EditorKeys.class.getResourceAsStream(PINNED_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Clés publiques de l'éditeur absentes : " + PINNED_RESOURCE);
            }
            Properties p = new Properties();
            p.load(in);
            return publicKeys(p);
        } catch (IOException e) {
            throw new IllegalStateException("Clés publiques de l'éditeur illisibles", e);
        }
    }

    public static Map<SignatureAlgorithm, byte[]> readPublic(Path file) throws IOException {
        return publicKeys(load(file));
    }

    public static Map<SignatureAlgorithm, KeyMaterial> readPrivate(Path file) throws IOException {
        Properties p = load(file);
        Map<SignatureAlgorithm, KeyMaterial> keys = new EnumMap<>(SignatureAlgorithm.class);
        for (SignatureAlgorithm a : new SignatureAlgorithm[] {SignatureAlgorithm.ED25519,
                SignatureAlgorithm.ML_DSA_65}) {
            keys.put(a, new KeyMaterial(decode(p, a.name() + ".public"), decode(p, a.name() + ".private")));
        }
        return keys;
    }

    /**
     * Génère une nouvelle paire Ed25519 + ML-DSA-65 : le fichier privé (à garder
     * hors ligne) et le fichier public (à épingler dans l'application).
     */
    public static void generate(Path privateFile, Path publicFile) throws IOException {
        Properties priv = new Properties();
        Properties pub = new Properties();
        for (SignatureAlgorithm a : new SignatureAlgorithm[] {SignatureAlgorithm.ED25519,
                SignatureAlgorithm.ML_DSA_65}) {
            KeyMaterial km = new BouncyCastleSignatureProvider(a).generateKeyPair();
            String pubB64 = Base64.getEncoder().encodeToString(km.publicKey());
            priv.setProperty(a.name() + ".public", pubB64);
            priv.setProperty(a.name() + ".private", Base64.getEncoder().encodeToString(km.privateKey()));
            pub.setProperty(a.name() + ".public", pubB64);
        }
        restrictToOwner(privateFile);
        store(priv, privateFile, "QualitOS - cles PRIVEES de l'editeur. Ne jamais versionner ni copier sur un serveur.");
        store(pub, publicFile, "QualitOS - cles PUBLIQUES de l'editeur, epinglees dans l'application.");
    }

    /**
     * Crée le fichier des clés privées lisible par son seul propriétaire, AVANT d'y
     * écrire quoi que ce soit : jamais de fenêtre pendant laquelle la clé serait
     * lisible par d'autres comptes. POSIX : {@code rw-------}. Ailleurs (Windows),
     * les droits « tout le monde » sont retirés et réservés au propriétaire.
     */
    static void restrictToOwner(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(file, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
            return;
        }
        Files.createFile(file);
        java.nio.file.attribute.AclFileAttributeView acl =
                Files.getFileAttributeView(file, java.nio.file.attribute.AclFileAttributeView.class);
        if (acl == null) {
            Files.delete(file);
            throw new IOException("Impossible de restreindre les droits de " + file
                    + " : ni POSIX ni ACL sur ce système de fichiers.");
        }
        // Une seule entrée : le propriétaire, tous les droits. Rien d'hérité du dossier.
        acl.setAcl(java.util.List.of(java.nio.file.attribute.AclEntry.newBuilder()
                .setType(java.nio.file.attribute.AclEntryType.ALLOW)
                .setPrincipal(Files.getOwner(file))
                .setPermissions(java.util.EnumSet.allOf(java.nio.file.attribute.AclEntryPermission.class))
                .build()));
    }

    private static Map<SignatureAlgorithm, byte[]> publicKeys(Properties p) {
        Map<SignatureAlgorithm, byte[]> keys = new EnumMap<>(SignatureAlgorithm.class);
        keys.put(SignatureAlgorithm.ED25519, decode(p, SignatureAlgorithm.ED25519.name() + ".public"));
        keys.put(SignatureAlgorithm.ML_DSA_65, decode(p, SignatureAlgorithm.ML_DSA_65.name() + ".public"));
        return keys;
    }

    private static byte[] decode(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Clé absente : " + key);
        }
        return Base64.getDecoder().decode(v.strip());
    }

    private static Properties load(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    private static void store(Properties p, Path file, String comment) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            p.store(w, comment);
        }
    }
}
