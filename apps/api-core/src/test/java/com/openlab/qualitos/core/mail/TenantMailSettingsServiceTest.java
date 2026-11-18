package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.secret.AesGcmSecretCipher;
import com.openlab.qualitos.core.secret.SecretCipher;
import com.openlab.qualitos.core.secret.SecretProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("La messagerie d'un client")
class TenantMailSettingsServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final String CLE =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final SecretCipher coffre = new AesGcmSecretCipher(new SecretProperties(CLE));
    private final MailProbePort sonde = mock(MailProbePort.class);
    /**
     * Politique NEUTRE pour les bancs de comportement.
     *
     * <p>Les hotes de ces bancs sont fictifs. Avec la politique du SaaS, chacun
     * serait refuse faute de resoudre -- et ces bancs mesureraient alors le DNS
     * de la machine qui les joue, pas la regle qu'ils decrivent. La garde
     * elle-meme a ses bancs dedies, ci-dessous et dans MailHostPolicyTest.
     */
    private static final MailHostPolicy HOTES = new MailHostPolicy("onprem", null);
    private DepotEnMemoire depot;
    private TenantMailSettingsService service;

    @BeforeEach
    void setUp() {
        depot = new DepotEnMemoire();
        service = new TenantMailSettingsService(
                depot, new MailAccountAssembler(coffre, HOTES), sonde);
    }

    private static MailSettingsDto.Request requete(String motDePasse) {
        return new MailSettingsDto.Request("qualite@client.fr", "Qualite Client", "retour@client.fr",
                "smtp.client.fr", 587, MailSecurity.STARTTLS, "qualite@client.fr",
                motDePasse, false, true);
    }

    @Nested
    @DisplayName("enregistrer")
    class Enregistrer {

        @Test
        @DisplayName("Le mot de passe est CHIFFRE, jamais stocke en clair")
        void chiffre() {
            service.enregistrer(TENANT, requete("s3cr3t"));

            String stocke = depot.lignes.get(TENANT).getAccount().getPasswordCipher();
            assertThat(stocke).isNotNull().doesNotContain("s3cr3t").startsWith("v1.");
            assertThat(coffre.dechiffrer(stocke)).isEqualTo("s3cr3t");
        }

        @Test
        @DisplayName("La reponse ne rend NI le mot de passe NI son cryptogramme")
        void jamaisRenvoye() {
            MailSettingsDto.Response r = service.enregistrer(TENANT, requete("s3cr3t"));

            // Rendre le cryptogramme offrirait a qui lit l'ecran une cible a
            // attaquer hors ligne, pour aucun gain : personne n'a besoin de le
            // voir. Un booleen suffit a dire qu'il y en a un.
            assertThat(r.hasPassword()).isTrue();
            assertThat(r.toString()).doesNotContain("s3cr3t").doesNotContain("v1.");
        }

        @Test
        @DisplayName("Un mot de passe ABSENT a la modification n'efface pas celui qui est en place")
        void absentNEffacePas() {
            service.enregistrer(TENANT, requete("s3cr3t"));

            // Le formulaire reaffiche le mot de passe VIDE -- l'API ne le rend
            // jamais. Prendre ce vide pour un effacement couperait la
            // messagerie du client a chaque correction de faute de frappe.
            MailSettingsDto.Response r = service.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", "Service Qualite", "retour@client.fr", "smtp.client.fr",
                    587, MailSecurity.STARTTLS, "qualite@client.fr", null, false, true));

            assertThat(r.hasPassword()).isTrue();
            assertThat(coffre.dechiffrer(depot.lignes.get(TENANT).getAccount().getPasswordCipher()))
                    .isEqualTo("s3cr3t");
            assertThat(r.fromName()).isEqualTo("Service Qualite");
        }

        @Test
        @DisplayName("L'effacer se demande EXPLICITEMENT")
        void effacementExplicite() {
            service.enregistrer(TENANT, requete("s3cr3t"));

            MailSettingsDto.Response r = service.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", "Qualite Client", "retour@client.fr", "relais.interne",
                    25, MailSecurity.NONE, null, null, true, true));

            // Un relais interne authentifie parfois par adresse IP : exiger un
            // mot de passe interdirait ce cas legitime.
            assertThat(r.hasPassword()).isFalse();
            assertThat(depot.lignes.get(TENANT).getAccount().getPasswordCipher()).isNull();
        }

        @Test
        @DisplayName("Changer le serveur EFFACE la preuve d'essai")
        void changerEffaceLaPreuve() {
            service.enregistrer(TENANT, requete("s3cr3t"));
            given(sonde.essayer(any(), anyString(), eq("admin@client.fr"))).willReturn(null);
            service.eprouver(TENANT, "admin@client.fr");
            assertThat(depot.lignes.get(TENANT).getAccount().getVerifiedAt()).isNotNull();

            MailSettingsDto.Response r = service.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", "Qualite Client", "retour@client.fr",
                    "smtp.AUTRE.fr", 587, MailSecurity.STARTTLS, "qualite@client.fr",
                    null, false, true));

            // Garder « verifiee » serait pire que de ne rien afficher :
            // l'ecran affirmerait une chose qu'il n'a pas verifiee, et personne
            // ne relancerait l'essai.
            assertThat(r.verifiedAt()).isNull();
        }

        @Test
        @DisplayName("Changer le NOM de l'expediteur garde la preuve : il ne change rien au serveur")
        void renommerGardeLaPreuve() {
            service.enregistrer(TENANT, requete("s3cr3t"));
            given(sonde.essayer(any(), anyString(), anyString())).willReturn(null);
            service.eprouver(TENANT, "admin@client.fr");

            MailSettingsDto.Response r = service.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", "Direction Qualite", "autre@client.fr", "smtp.client.fr",
                    587, MailSecurity.STARTTLS, "qualite@client.fr", null, false, true));

            // Effacer la preuve ici ferait relancer un essai pour un changement
            // qui ne peut pas l'invalider -- et apprendrait a l'ignorer.
            assertThat(r.verifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("Un hote INTERNE est refuse des l'enregistrement, pas seulement a l'essai")
        void hoteInterneRefuseALEnregistrement() {
            TenantMailSettingsService saas = new TenantMailSettingsService(
                    depot, new MailAccountAssembler(coffre, new MailHostPolicy("saas", null)),
                    sonde);

            // Poser la garde dans la seule sonde laissait un contournement
            // evident : enregistrer un hote interne, activer la messagerie, et
            // ne jamais appuyer sur « essai ». Le premier courriel emis faisait
            // alors la connexion qu'on voulait interdire.
            assertThatThrownBy(() -> saas.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", null, null, "10.0.0.7", 25,
                    MailSecurity.NONE, null, null, false, true)))
                    .isInstanceOf(MailHostRefusedException.class);
            // Et rien n'est ecrit : un reglage refuse ne laisse pas de trace a
            // demi posee.
            assertThat(depot.lignes).doesNotContainKey(TENANT);
        }

        @Test
        @DisplayName("Un champ vide vaut « pas de valeur », pas une chaine vide")
        void videVautNull() {
            service.enregistrer(TENANT, new MailSettingsDto.Request(
                    " qualite@client.fr ", "   ", null, " smtp.client.fr ", 587,
                    MailSecurity.STARTTLS, "  ", "s3cr3t", false, false));

            MailAccount c = depot.lignes.get(TENANT).getAccount();
            assertThat(c.getFromName()).isNull();
            assertThat(c.getUsername()).isNull();
            assertThat(c.getFromAddress()).isEqualTo("qualite@client.fr");
            assertThat(c.getHost()).isEqualTo("smtp.client.fr");
        }
    }

    @Nested
    @DisplayName("eprouver")
    class Eprouver {

        @Test
        @DisplayName("Un essai REUSSI marque la messagerie verifiee")
        void reussite() {
            service.enregistrer(TENANT, requete("s3cr3t"));
            given(sonde.essayer(any(), anyString(), anyString())).willReturn(null);

            MailSettingsDto.TestResult r = service.eprouver(TENANT, "admin@client.fr");

            assertThat(r.success()).isTrue();
            assertThat(r.verifiedAt()).isNotNull()
                    .isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("La sonde recoit le mot de passe DECHIFFRE : c'est ce que le serveur attend")
        void sondeRecoitLeClair() {
            service.enregistrer(TENANT, requete("s3cr3t"));
            given(sonde.essayer(any(), anyString(), anyString())).willReturn(null);

            service.eprouver(TENANT, "admin@client.fr");

            verify(sonde).essayer(any(), eq("s3cr3t"), eq("admin@client.fr"));
        }

        @Test
        @DisplayName("Un echec rend son MOTIF, et ne marque rien")
        void echecRendLeMotif() {
            service.enregistrer(TENANT, requete("s3cr3t"));
            given(sonde.essayer(any(), anyString(), anyString()))
                    .willReturn("Authentication failed (535)");

            MailSettingsDto.TestResult r = service.eprouver(TENANT, "admin@client.fr");

            // Un « echec » sans motif oblige a deviner entre un nom de serveur
            // faux, un port ferme, un mot de passe refuse et un pare-feu -- et
            // c'est ce qui fait abandonner le reglage d'une messagerie.
            assertThat(r.success()).isFalse();
            assertThat(r.detail()).contains("535");
            assertThat(depot.lignes.get(TENANT).getAccount().getVerifiedAt()).isNull();
        }

        @Test
        @DisplayName("Un echec N'EFFACE PAS une verification anterieure")
        void echecNEffacePasLaPreuve() {
            service.enregistrer(TENANT, requete("s3cr3t"));
            given(sonde.essayer(any(), anyString(), anyString())).willReturn(null);
            service.eprouver(TENANT, "admin@client.fr");

            given(sonde.essayer(any(), anyString(), anyString())).willReturn("Connection timed out");
            MailSettingsDto.TestResult r = service.eprouver(TENANT, "admin@client.fr");

            // Un serveur momentanement injoignable ne rend pas faux un essai
            // qui a abouti hier ; effacer la preuve ferait clignoter l'ecran au
            // rythme des incidents reseau.
            assertThat(r.success()).isFalse();
            assertThat(r.verifiedAt()).isNotNull();
            assertThat(depot.lignes.get(TENANT).getAccount().getVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("Eprouver une messagerie qu'on n'a pas reglee est refuse")
        void rienARegler() {
            assertThatThrownBy(() -> service.eprouver(TENANT, "admin@client.fr"))
                    .isInstanceOf(MailSettingsNotConfiguredException.class);
        }
    }

    @Nested
    @DisplayName("pourEnvoi et retirer")
    class Usage {

        @Test
        @DisplayName("Une messagerie reglee mais NON ACTIVEE ne detourne pas le courrier")
        void nonActiveeNEstPasUtilisee() {
            service.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", null, null, "smtp.client.fr", 587,
                    MailSecurity.STARTTLS, "u", "p", false, false));

            // Sans cet interrupteur, enregistrer un reglage incomplet
            // detournerait aussitot tout le courrier du client vers un serveur
            // qui le refuse.
            assertThat(service.pourEnvoi(TENANT)).isEmpty();
        }

        @Test
        @DisplayName("Une messagerie activee sert a l'envoi")
        void activeeEstUtilisee() {
            service.enregistrer(TENANT, requete("s3cr3t"));

            assertThat(service.pourEnvoi(TENANT))
                    .get().extracting(MailAccount::getHost).isEqualTo("smtp.client.fr");
        }

        @Test
        @DisplayName("Sans reglage, le client emprunte celle de la plateforme")
        void sansReglage() {
            assertThat(service.pourEnvoi(TENANT)).isEmpty();
            assertThat(service.lire(TENANT).configured()).isFalse();
        }

        @Test
        @DisplayName("Retirer SUPPRIME la ligne, secret compris")
        void retirerSupprime() {
            service.enregistrer(TENANT, requete("s3cr3t"));

            service.retirer(TENANT);

            // Garder un mot de passe chiffre dont plus personne ne se sert,
            // c'est conserver un secret sans raison -- et un secret conserve
            // sans raison finit par fuir.
            assertThat(depot.lignes).doesNotContainKey(TENANT);
            assertThat(service.lire(TENANT).configured()).isFalse();
        }

        @Test
        @DisplayName("Retirer ce qui n'existe pas ne casse rien")
        void retirerDeuxFois() {
            service.retirer(TENANT);
            service.retirer(TENANT);

            assertThat(depot.lignes).isEmpty();
        }

        @Test
        @DisplayName("Sans mot de passe, la sonde recoit null, pas une chaine vide")
        void sansMotDePasse() {
            service.enregistrer(TENANT, new MailSettingsDto.Request(
                    "qualite@client.fr", null, null, "relais.interne", 25,
                    MailSecurity.NONE, null, null, false, true));

            assertThat(service.motDePasseClair(depot.lignes.get(TENANT).getAccount())).isNull();
        }
    }

    /**
     * Un depot en memoire plutot qu'un double : le service enchaine
     * enregistrement, lecture et re-enregistrement, et un double aurait demande
     * de reprogrammer la reponse a chaque etape -- en decidant soi-meme ce que
     * la base est censee contenir, c'est-a-dire en testant sa propre idee du
     * comportement plutot que le comportement.
     */
    private static final class DepotEnMemoire implements TenantMailSettingsRepository {

        private final Map<UUID, TenantMailSettings> lignes = new HashMap<>();

        @Override
        public Optional<TenantMailSettings> findById(UUID id) {
            return Optional.ofNullable(lignes.get(id));
        }

        @Override
        public boolean existsById(UUID id) {
            return lignes.containsKey(id);
        }

        @Override
        public <S extends TenantMailSettings> S save(S entity) {
            lignes.put(entity.getTenantId(), entity);
            return entity;
        }

        @Override
        public void deleteById(UUID id) {
            lignes.remove(id);
        }

        // --- le reste du contrat JpaRepository, inutilise ici ---

        @Override
        public java.util.List<TenantMailSettings> findAll() {
            return java.util.List.copyOf(lignes.values());
        }

        @Override
        public java.util.List<TenantMailSettings> findAll(org.springframework.data.domain.Sort sort) {
            return findAll();
        }

        @Override
        public org.springframework.data.domain.Page<TenantMailSettings> findAll(
                org.springframework.data.domain.Pageable pageable) {
            return new org.springframework.data.domain.PageImpl<>(findAll());
        }

        @Override
        public java.util.List<TenantMailSettings> findAllById(Iterable<UUID> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return lignes.size();
        }

        @Override
        public void delete(TenantMailSettings entity) {
            lignes.remove(entity.getTenantId());
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(Iterable<? extends TenantMailSettings> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            lignes.clear();
        }

        @Override
        public <S extends TenantMailSettings> java.util.List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends TenantMailSettings> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends TenantMailSettings> java.util.List<S> saveAllAndFlush(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch(Iterable<TenantMailSettings> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch() {
            lignes.clear();
        }

        @Override
        public TenantMailSettings getOne(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TenantMailSettings getById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TenantMailSettings getReferenceById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings> Optional<S> findOne(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings> java.util.List<S> findAll(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings> java.util.List<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings> org.springframework.data.domain.Page<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings> long count(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings> boolean exists(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends TenantMailSettings, R> R findBy(
                org.springframework.data.domain.Example<S> example,
                java.util.function.Function<org.springframework.data.repository.query.FluentQuery
                        .FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException();
        }
    }
}
