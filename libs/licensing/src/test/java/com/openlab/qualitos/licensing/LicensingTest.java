package com.openlab.qualitos.licensing;

import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.licensing.application.LicenseCodec;
import com.openlab.qualitos.licensing.application.LicenseFile;
import com.openlab.qualitos.licensing.application.LicenseIssuer;
import com.openlab.qualitos.licensing.application.LicenseVerifier;
import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.Edition;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.licensing.domain.LicenseException;
import com.openlab.qualitos.licensing.domain.LicenseState;
import com.openlab.qualitos.licensing.domain.LicenseStatus;
import com.openlab.qualitos.licensing.infrastructure.EditorKeys;
import com.openlab.qualitos.licensing.infrastructure.LicensingFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La licence on-premise : émise par l'éditeur, vérifiée hors ligne, lue au fil de l'eau (ADR 0082). */
class LicensingTest {

    static final Instant T0 = Instant.parse("2026-10-08T08:00:00Z");
    static final UUID TENANT = UUID.fromString("7a1c2e40-0000-4000-8000-000000000001");

    static Map<SignatureAlgorithm, KeyMaterial> editeur;
    static Map<SignatureAlgorithm, KeyMaterial> faussaire;

    @BeforeAll
    static void cles() {
        editeur = paire();
        faussaire = paire();
    }

    static Map<SignatureAlgorithm, KeyMaterial> paire() {
        Map<SignatureAlgorithm, KeyMaterial> k = new EnumMap<>(SignatureAlgorithm.class);
        k.put(SignatureAlgorithm.ED25519, new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519).generateKeyPair());
        k.put(SignatureAlgorithm.ML_DSA_65,
                new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65).generateKeyPair());
        return k;
    }

    static Map<SignatureAlgorithm, byte[]> publiques(Map<SignatureAlgorithm, KeyMaterial> k) {
        Map<SignatureAlgorithm, byte[]> p = new EnumMap<>(SignatureAlgorithm.class);
        k.forEach((a, km) -> p.put(a, km.publicKey()));
        return p;
    }

    static License licence(Set<String> modules, int maxUsers) {
        return new License("LIC-2026-001", "Hôpital Saint-Louis", TENANT, "ENTERPRISE", modules, maxUsers,
                T0, T0, T0.plus(Duration.ofDays(365)), 30);
    }

    static String emettre(License l, Map<SignatureAlgorithm, KeyMaterial> cles) {
        return new LicenseIssuer(cles, Clock.fixed(T0, ZoneOffset.UTC)).issue(l);
    }

    final LicenseVerifier verificateur = new LicenseVerifier(publiques(editeur));

    @Nested
    class Signature {

        @Test
        void uneLicenceDeLEditeurSeVerifieEtSeRelitALIdentique() {
            License l = licence(Set.of("pdca", "capa"), 50);
            assertThat(verificateur.verify(emettre(l, editeur))).isEqualTo(l);
        }

        @Test
        void uneLicenceSigneeParUnAutreEstRefusee() {
            assertThatThrownBy(() -> verificateur.verify(emettre(licence(Set.of("*"), 0), faussaire)))
                    .isInstanceOf(LicenseException.class)
                    .hasMessageContaining("pas été signée par l'éditeur");
        }

        @Test
        void unContenuModifieApresSignatureEstRefuse() {
            String fichier = emettre(licence(Set.of("pdca"), 10), editeur);
            LicenseFile f = LicenseFile.parse(fichier);
            String json = new String(f.payload(), StandardCharsets.UTF_8).replace("\"maxUsers\":10", "\"maxUsers\":0");
            String trafique = new LicenseFile(json.getBytes(StandardCharsets.UTF_8), f.signature()).write();

            assertThatThrownBy(() -> verificateur.verify(trafique))
                    .isInstanceOf(LicenseException.class).hasMessageContaining("modifié");
        }

        @Test
        void uneSeuleSignatureNeSuffitPas() {
            String fichier = emettre(licence(Set.of("pdca"), 10), editeur);
            LicenseFile f = LicenseFile.parse(fichier);
            SignatureEnvelope env = SignatureEnvelope.decode(f.signature());
            SignatureEnvelope classiqueSeule = new SignatureEnvelope(env.version(), env.suiteName(), env.keyRef(),
                    env.signedAt(), env.parts().stream()
                    .filter(p -> p.algorithm() == SignatureAlgorithm.ED25519).toList());
            String amputee = new LicenseFile(f.payload(), classiqueSeule.encode()).write();

            assertThatThrownBy(() -> verificateur.verify(amputee))
                    .isInstanceOf(LicenseException.class).hasMessageContaining("ML-DSA-65");
        }

        @Test
        void unFichierQuiNEstPasUneLicenceEstRefuse() {
            assertThatThrownBy(() -> verificateur.verify("")).isInstanceOf(LicenseException.class);
            assertThatThrownBy(() -> verificateur.verify("{ pas du json")).isInstanceOf(LicenseException.class);
            assertThatThrownBy(() -> verificateur.verify("{\"format\":\"autre\"}"))
                    .hasMessageContaining("pas une licence");
            assertThatThrownBy(() -> verificateur.verify(
                    "{\"format\":\"qualitos-license/1\",\"payload\":\"%%%\",\"signature\":\"x\"}"))
                    .hasMessageContaining("mal encodé");
            assertThatThrownBy(() -> verificateur.verify(
                    "{\"format\":\"qualitos-license/1\",\"payload\":\"e30\",\"signature\":\"%%%\"}"))
                    .hasMessageContaining("illisible");
            assertThatThrownBy(() -> verificateur.verify("x".repeat(LicenseFile.MAX_BYTES + 1)))
                    .hasMessageContaining("volumineux");
        }

        @Test
        void leVerificateurExigeLesDeuxClesDeLEditeur() {
            Map<SignatureAlgorithm, byte[]> une = new EnumMap<>(SignatureAlgorithm.class);
            une.put(SignatureAlgorithm.ED25519, editeur.get(SignatureAlgorithm.ED25519).publicKey());
            assertThatThrownBy(() -> new LicenseVerifier(une)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new LicenseIssuer(Map.of(SignatureAlgorithm.ED25519,
                    editeur.get(SignatureAlgorithm.ED25519)), Clock.systemUTC()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Contenu {

        @Test
        void lesRèglesDeLaLicence() {
            assertThatThrownBy(() -> new License(" ", "C", TENANT, "PRO", Set.of("*"), 0, T0, T0, T0.plusSeconds(1), 0))
                    .hasMessageContaining("licenseId");
            assertThatThrownBy(() -> new License("L", "C", null, "PRO", Set.of("*"), 0, T0, T0, T0.plusSeconds(1), 0))
                    .hasMessageContaining("tenantId");
            assertThatThrownBy(() -> new License("L", "C", TENANT, "GOLD", Set.of("*"), 0, T0, T0, T0.plusSeconds(1), 0))
                    .hasMessageContaining("Palier");
            assertThatThrownBy(() -> new License("L", "C", TENANT, "PRO", Set.of(), 0, T0, T0, T0.plusSeconds(1), 0))
                    .hasMessageContaining("aucun module");
            assertThatThrownBy(() -> new License("L", "C", TENANT, "PRO", Set.of("PDCA"), 0, T0, T0, T0.plusSeconds(1), 0))
                    .hasMessageContaining("module invalide");
            assertThatThrownBy(() -> new License("L", "C", TENANT, "PRO", Set.of("*"), -1, T0, T0, T0.plusSeconds(1), 0))
                    .hasMessageContaining("maxUsers");
            assertThatThrownBy(() -> new License("L", "C", TENANT, "PRO", Set.of("*"), 0, T0, T0, T0, 0))
                    .hasMessageContaining("échéance");
            assertThatThrownBy(() -> new License("L", "C", TENANT, "PRO", Set.of("*"), 0, T0, T0, T0.plusSeconds(1), 91))
                    .hasMessageContaining("grâce");
            assertThatThrownBy(() -> new License("L", "x".repeat(201), TENANT, "PRO", Set.of("*"), 0, T0, T0,
                    T0.plusSeconds(1), 0)).hasMessageContaining("trop long");
        }

        @Test
        void lesModulesEtLesUtilisateurs() {
            License tout = licence(Set.of("*"), 0);
            assertThat(tout.allowsModule("dmaic")).isTrue();
            assertThat(tout.unlimitedUsers()).isTrue();
            License deux = licence(Set.of("pdca", "capa"), 25);
            assertThat(deux.allowsModule("capa")).isTrue();
            assertThat(deux.allowsModule("dmaic")).isFalse();
            assertThat(deux.unlimitedUsers()).isFalse();
        }

        @Test
        void lEtatSuitLeCalendrier() {
            License l = licence(Set.of("*"), 0);
            assertThat(l.statusAt(T0.minusSeconds(1))).isEqualTo(LicenseStatus.NOT_YET_VALID);
            assertThat(l.statusAt(T0.plus(Duration.ofDays(10)))).isEqualTo(LicenseStatus.VALID);
            assertThat(l.statusAt(T0.plus(Duration.ofDays(370)))).isEqualTo(LicenseStatus.GRACE);
            assertThat(l.statusAt(T0.plus(Duration.ofDays(400)))).isEqualTo(LicenseStatus.EXPIRED);
            assertThat(LicenseStatus.GRACE.allowsWrites()).isTrue();
            assertThat(LicenseStatus.EXPIRED.allowsWrites()).isFalse();
        }

        @Test
        void uneDemandeSansVersionEstAcceptee_uneVersionInconnueNon() {
            String demande = """
                    {"licenseId":"LIC-1","customer":"Usine","tenantId":"%s","tier":"PRO","modules":["pdca"],
                     "maxUsers":5,"issuedAt":"2026-10-08T00:00:00Z","notBefore":"2026-10-08T00:00:00Z",
                     "expiresAt":"2027-10-08T00:00:00Z","graceDays":15}""".formatted(TENANT);
            assertThat(LicenseCodec.fromRequest(demande).maxUsers()).isEqualTo(5);
            assertThatThrownBy(() -> LicenseCodec.fromRequest(demande.replace("{\"licenseId\"",
                    "{\"version\":9,\"licenseId\""))).hasMessageContaining("Version");
            assertThatThrownBy(() -> LicenseCodec.fromRequest(demande.replace("\"maxUsers\":5",
                    "\"maxUsers\":5,\"illimite\":true"))).isInstanceOf(LicenseException.class);
            assertThatThrownBy(() -> LicenseCodec.fromRequest("[]")).hasMessageContaining("objet JSON");
            assertThatThrownBy(() -> LicenseCodec.fromRequest("{")).hasMessageContaining("illisible");
        }
    }

    @Nested
    class Installation {

        final AtomicReference<Optional<String>> fichier = new AtomicReference<>(Optional.empty());
        final AtomicInteger lectures = new AtomicInteger();
        final MutableClock horloge = new MutableClock(T0.plus(Duration.ofDays(1)));

        Licensing onPrem() {
            return Licensing.onPrem(() -> {
                lectures.incrementAndGet();
                return fichier.get();
            }, verificateur, horloge);
        }

        @Test
        void enSaasLaLicenceNeSAppliquePas() {
            Licensing saas = Licensing.saas(horloge);
            LicenseState s = saas.state();
            assertThat(saas.edition()).isEqualTo(Edition.SAAS);
            assertThat(saas.isOnPrem()).isFalse();
            assertThat(s.status()).isEqualTo(LicenseStatus.NOT_REQUIRED);
            assertThat(s.allowsWrites()).isTrue();
            assertThat(s.allowsModule("tout")).isTrue();
        }

        @Test
        void sansFichierLInstallationEstEnLectureSeule() {
            LicenseState s = onPrem().state();
            assertThat(s.status()).isEqualTo(LicenseStatus.MISSING);
            assertThat(s.allowsWrites()).isFalse();
            assertThat(s.allowsModule("pdca")).isFalse();
            assertThat(s.reason()).isNotBlank();
        }

        @Test
        void uneLicenceInvalideDitPourquoiSansEmpecherLeDemarrage() {
            fichier.set(Optional.of(emettre(licence(Set.of("*"), 0), faussaire)));
            LicenseState s = onPrem().state();
            assertThat(s.status()).isEqualTo(LicenseStatus.INVALID);
            assertThat(s.reason()).contains("éditeur");
        }

        @Test
        void uneLectureQuiEchoueRendLaLicenceInvalide() {
            Licensing l = Licensing.onPrem(() -> {
                throw new IllegalStateException("disque");
            }, verificateur, horloge);
            assertThat(l.state().status()).isEqualTo(LicenseStatus.INVALID);
        }

        @Test
        void leFichierEstReluAuPlusUneFoisParMinute_etLEcheanceTombeALHeure() {
            fichier.set(Optional.of(emettre(licence(Set.of("pdca"), 0), editeur)));
            Instant echeance = T0.plus(Duration.ofDays(365));
            horloge.set(echeance.minusSeconds(10));
            Licensing l = onPrem();
            assertThat(l.state().status()).isEqualTo(LicenseStatus.VALID);
            assertThat(l.state().allowsModule("pdca")).isTrue();
            assertThat(l.state().allowsModule("dmaic")).isFalse();

            // Vingt secondes plus tard, sans relire le fichier, l'échéance est passée : grâce.
            fichier.set(Optional.empty());
            horloge.set(echeance.plusSeconds(10));
            assertThat(l.state().status()).isEqualTo(LicenseStatus.GRACE);
            assertThat(lectures.get()).isEqualTo(1);

            // Une minute après la première lecture, le fichier est relu : il a disparu.
            horloge.set(echeance.minusSeconds(10).plus(Licensing.RECHECK));
            assertThat(l.state().status()).isEqualTo(LicenseStatus.MISSING);
            assertThat(lectures.get()).isEqualTo(2);
        }

        @Test
        void auDelaDeLaGraceLInstallationPasseEnLectureSeule() {
            fichier.set(Optional.of(emettre(licence(Set.of("pdca"), 0), editeur)));
            horloge.set(T0.plus(Duration.ofDays(400)));
            LicenseState s = onPrem().state();
            assertThat(s.status()).isEqualTo(LicenseStatus.EXPIRED);
            assertThat(s.allowsWrites()).isFalse();
            // Les lectures restent possibles : le module reste reconnu.
            assertThat(s.allowsModule("pdca")).isTrue();
        }

        @Test
        void unRenouvellementEstPrisEnCompteSansRedemarrer() {
            Licensing l = onPrem();
            assertThat(l.state().status()).isEqualTo(LicenseStatus.MISSING);
            fichier.set(Optional.of(emettre(licence(Set.of("*"), 0), editeur)));
            horloge.set(horloge.instant().plus(Licensing.RECHECK));
            assertThat(l.state().status()).isEqualTo(LicenseStatus.VALID);
            assertThat(l.state().licenseOpt()).isPresent();
        }
    }

    @Nested
    class Configuration {

        @Test
        void lEditionSeLitDepuisLaConfiguration() {
            assertThat(Edition.parse(null)).isEqualTo(Edition.SAAS);
            assertThat(Edition.parse(" ")).isEqualTo(Edition.SAAS);
            assertThat(Edition.parse("saas")).isEqualTo(Edition.SAAS);
            assertThat(Edition.parse("onprem")).isEqualTo(Edition.ONPREM);
            assertThat(Edition.parse("on-prem")).isEqualTo(Edition.ONPREM);
            assertThat(Edition.parse("On-Premise")).isEqualTo(Edition.ONPREM);
            assertThatThrownBy(() -> Edition.parse("cloud")).hasMessageContaining("QUALITOS_EDITION");
        }

        @Test
        void lesClesPubliquesDeLEditeurSontEpingleesDansLApplication() {
            Map<SignatureAlgorithm, byte[]> pinned = EditorKeys.pinned();
            assertThat(pinned).containsOnlyKeys(SignatureAlgorithm.ED25519, SignatureAlgorithm.ML_DSA_65);
            assertThat(new LicenseVerifier(pinned)).isNotNull();
        }

        @Test
        void laFabriqueLitLeFichierMonte(@TempDir Path dir) throws Exception {
            Path cles = dir.resolve("publiques.properties");
            Files.writeString(cles, "ED25519.public=" + b64(editeur, SignatureAlgorithm.ED25519)
                    + "\nML_DSA_65.public=" + b64(editeur, SignatureAlgorithm.ML_DSA_65) + "\n");
            assertThat(EditorKeys.readPublic(cles)).containsOnlyKeys(SignatureAlgorithm.ED25519,
                    SignatureAlgorithm.ML_DSA_65);

            assertThat(LicensingFactory.create("saas", null, Clock.systemUTC()).isOnPrem()).isFalse();
            Licensing absente = LicensingFactory.create("onprem", dir.resolve("absente.lic").toString(),
                    Clock.systemUTC());
            assertThat(absente.state().status()).isEqualTo(LicenseStatus.MISSING);
            assertThat(LicensingFactory.create("onprem", " ", Clock.systemUTC()).state().status())
                    .isEqualTo(LicenseStatus.MISSING);

            // Signée par une clé de test : les clés épinglées la refusent, comme il se doit.
            Path lic = dir.resolve("client.lic");
            Files.writeString(lic, emettre(licence(Set.of("*"), 0), editeur));
            assertThat(LicensingFactory.create("onprem", lic.toString(), Clock.systemUTC()).state().status())
                    .isEqualTo(LicenseStatus.INVALID);

            Path enorme = dir.resolve("enorme.lic");
            Files.writeString(enorme, "x".repeat(LicenseFile.MAX_BYTES + 1));
            assertThat(LicensingFactory.create("onprem", enorme.toString(), Clock.systemUTC()).state().status())
                    .isEqualTo(LicenseStatus.INVALID);
        }

        @Test
        void lesClesSeGenerentEtSeRelisent(@TempDir Path dir) throws Exception {
            Path priv = dir.resolve("p/privees.properties");
            Path pub = dir.resolve("publiques.properties");
            EditorKeys.generate(priv, pub);
            Map<SignatureAlgorithm, KeyMaterial> relues = EditorKeys.readPrivate(priv);
            License l = licence(Set.of("*"), 0);
            assertThat(new LicenseVerifier(EditorKeys.readPublic(pub)).verify(emettre(l, relues))).isEqualTo(l);
            Path vide = dir.resolve("vide.properties");
            Files.writeString(vide, "");
            assertThatThrownBy(() -> EditorKeys.readPublic(vide)).hasMessageContaining("absente");
        }
    }

    static String b64(Map<SignatureAlgorithm, KeyMaterial> k, SignatureAlgorithm a) {
        return Base64.getEncoder().encodeToString(k.get(a).publicKey());
    }

    /** Une horloge qu'on avance à la main. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant i) {
            now = i;
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
