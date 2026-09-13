package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDDiscipline;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReport;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportNotFoundException;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportRepository;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDStateException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage — extraire le rapport 8D d'une non-conformité clôturée.
 *
 * <p>Trois gestes, et un seul document :
 * <ol>
 *   <li><b>consulter</b> : les huit disciplines, agrégées à la volée tant que le
 *       rapport est en brouillon, relues de l'instantané dès qu'il est émis ;</li>
 *   <li><b>saisir</b> : D1, D3 et D8, les trois seules qui n'ont aucune source ;</li>
 *   <li><b>émettre</b> : à la clôture seulement. Fige le contenu, calcule
 *       l'empreinte du PDF, la signe (Ed25519 + ML-DSA-65), l'ancre, et journalise.</li>
 * </ol>
 *
 * <p><b>Pourquoi un document FIGÉ et non une vue recalculée.</b> Une vue
 * changerait après coup — une CAPA rouverte, un Ishikawa complété, un plan de
 * surveillance révisé — et un 8D qui change n'est plus un 8D : c'est ce que le
 * client a lu qui compte, pas ce que la base dit aujourd'hui. L'instantané est donc
 * stocké, et le rendu PDF en est une fonction pure, de sorte que l'empreinte
 * scellée reste vérifiable.
 *
 * <p>Multi-tenant strict : tenant et acteur viennent du jeton, jamais du corps
 * (§18.2 #2). Aucune dépendance framework ici : tout passe par des ports.
 */
public class EightDService {

    /** Horodatage du nom de fichier proposé au navigateur. */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final EightDReportRepository repository;
    private final EightDSourcePort sources;
    private final EightDSnapshotAssembler assembler;
    private final EightDPdfRenderPort renderer;
    private final EightDSealPort seals;
    private final EightDSupportPorts.SnapshotCodec codec;
    private final EightDSupportPorts.VerifyUrlBuilder verifyUrlBuilder;
    private final EightDSupportPorts.AuditPort audit;
    private final EightDSupportPorts.TenantProvider tenants;
    private final EightDSupportPorts.ActorProvider actors;
    private final SecureRandom random;
    private final Clock clock;

    public EightDService(EightDReportRepository repository,
                         EightDSourcePort sources,
                         EightDSnapshotAssembler assembler,
                         EightDPdfRenderPort renderer,
                         EightDSealPort seals,
                         EightDSupportPorts.SnapshotCodec codec,
                         EightDSupportPorts.VerifyUrlBuilder verifyUrlBuilder,
                         EightDSupportPorts.AuditPort audit,
                         EightDSupportPorts.TenantProvider tenants,
                         EightDSupportPorts.ActorProvider actors,
                         SecureRandom random,
                         Clock clock) {
        this.repository = repository;
        this.sources = sources;
        this.assembler = assembler;
        this.renderer = renderer;
        this.seals = seals;
        this.codec = codec;
        this.verifyUrlBuilder = verifyUrlBuilder;
        this.audit = audit;
        this.tenants = tenants;
        this.actors = actors;
        this.random = random;
        this.clock = clock;
    }

    /** Le rapport tel qu'il est : agrégé si brouillon, relu de l'instantané si émis. */
    public EightDDto.ReportView consulter(UUID ncId) {
        UUID tenantId = tenants.requireTenantId();
        EightDSources collecte = sources.collect(tenantId, ncId);
        EightDReport report = repository.findByNc(tenantId, ncId).orElse(null);
        return vue(tenantId, ncId, collecte, report);
    }

    /**
     * Renseigne D1, D3 et D8. Crée le brouillon au premier enregistrement — il n'y
     * a pas de geste « créer un rapport 8D » séparé, qui n'aurait rien créé.
     */
    public EightDDto.ReportView enregistrer(UUID ncId, EightDDto.SaveCommand command) {
        UUID tenantId = tenants.requireTenantId();
        EightDSources collecte = sources.collect(tenantId, ncId);
        Instant now = Instant.now(clock);
        EightDReport report = repository.findByNc(tenantId, ncId)
                .orElseGet(() -> EightDReport.brouillon(tenantId, ncId, now));
        report.saisir(command.team(), command.containment(), command.recognition(), now);
        EightDReport saved = repository.save(report);
        return vue(tenantId, ncId, collecte, saved);
    }

    /**
     * Émet le rapport : contenu figé, empreinte signée, ancrage posé, journal écrit.
     *
     * @throws EightDStateException si la non-conformité n'est pas clôturée, ou si le
     *                             rapport a déjà été émis
     */
    public EightDDto.ReportView emettre(UUID ncId) {
        UUID tenantId = tenants.requireTenantId();
        UUID actorId = actors.currentUserId();
        String actorName = actors.currentUserName();
        EightDSources collecte = sources.collect(tenantId, ncId);

        // La clôture est la condition, pas une convention d'écran : un 8D émis sur un
        // écart encore ouvert affirmerait que le problème est réglé alors qu'il court.
        if (collecte.nc().status() != NcStatus.CLOSED) {
            throw new EightDStateException(
                    "Un rapport 8D ne s'émet qu'à la clôture de la non-conformité"
                    + " (statut actuel : " + collecte.nc().status() + ")");
        }

        Instant now = Instant.now(clock);
        EightDReport report = repository.findByNc(tenantId, ncId)
                .orElseGet(() -> EightDReport.brouillon(tenantId, ncId, now));
        if (report.estEmis()) {
            throw new EightDStateException(
                    "Le rapport 8D de cette non-conformité a déjà été émis le "
                    + report.getIssuedAt());
        }

        EightDSnapshot snapshot = assembler.assemble(collecte,
                report.getTeam(), report.getContainment(), report.getRecognition(),
                tenantId.toString(), now, actorName);
        String json = codec.encode(snapshot);
        String code = nouveauCode();

        // Le PDF est rendu AVANT la signature : c'est son empreinte qu'on signe, et
        // signer l'instantané aurait laissé le document lui-même hors de la preuve.
        byte[] pdf = renderer.render(snapshot, verifyUrlBuilder.verifyUrl(code));
        String sha256 = sha256(pdf);
        EightDSealPort.Seal seal = seals.seal(tenantId, sha256);

        report.emettre(json, sha256, seal.signature(), seal.anchorTxRef(), code,
                actorId, actorName, now);
        EightDReport saved = repository.save(report);

        audit.recordIssued(tenantId, actorId, ncId,
                "Rapport 8D émis pour " + collecte.nc().reference(),
                detailsJson(collecte.nc().reference(), sha256, seal.anchorTxRef(), snapshot));

        return vue(tenantId, ncId, collecte, saved);
    }

    /**
     * Rend le PDF du rapport émis.
     *
     * <p>Re-rendu depuis l'instantané plutôt que stocké en binaire : le document
     * reste disponible même quand le stockage objet est coupé, et l'unique source de
     * vérité demeure l'instantané signé. L'empreinte est recalculée et <b>comparée</b>
     * à celle qui a été scellée : plutôt que de remettre un document dont le sceau ne
     * répondrait plus, on refuse et on le dit.
     */
    public EightDDto.PdfResult pdf(UUID ncId) {
        UUID tenantId = tenants.requireTenantId();
        EightDReport report = repository.findByNc(tenantId, ncId)
                .orElseThrow(() -> new EightDReportNotFoundException(ncId));
        if (!report.estEmis()) {
            throw new EightDStateException(
                    "Le rapport 8D n'est pas encore émis : il n'y a pas de document à remettre");
        }
        EightDSnapshot snapshot = codec.decode(report.getSnapshotJson());
        byte[] pdf = renderer.render(snapshot, verifyUrlBuilder.verifyUrl(report.getVerificationCode()));
        String sha256 = sha256(pdf);
        if (!sha256.equals(report.getSha256Hex())) {
            throw new EightDStateException(
                    "Le rendu du rapport 8D ne correspond plus à l'empreinte scellée le "
                    + report.getIssuedAt() + " : le document n'est pas remis");
        }
        String fileName = "8d-" + stem(snapshot.ncReference()) + "-"
                + STAMP.format(report.getIssuedAt()) + ".pdf";
        return new EightDDto.PdfResult(pdf, fileName, sha256, report.getVerificationCode());
    }

    /**
     * Vérification publique : revalide la signature stockée sur l'empreinte stockée.
     * Un code inconnu répond {@code valid=false} sans détail — pas une erreur, pour
     * que la route ne serve pas à énumérer les tenants (OWASP A01).
     */
    public EightDDto.VerificationResult verifier(String code) {
        Optional<EightDReport> trouve = repository.findByVerificationCode(code);
        if (trouve.isEmpty()) {
            return EightDDto.VerificationResult.unknown(code);
        }
        EightDReport report = trouve.get();
        boolean valide = seals.verify(report.getSignature(), report.getSha256Hex());
        String reference;
        try {
            reference = codec.decode(report.getSnapshotJson()).ncReference();
        } catch (RuntimeException ex) {
            // Un instantané illisible ne doit pas faire tomber la vérification :
            // l'empreinte et la signature, elles, restent lisibles et font foi.
            reference = null;
        }
        return new EightDDto.VerificationResult(valide, code, report.getSha256Hex(),
                report.getAnchorTxRef(), reference, report.getIssuedAt());
    }

    // ---------- assemblage de la vue ----------

    private EightDDto.ReportView vue(UUID tenantId, UUID ncId, EightDSources collecte,
                                     EightDReport report) {
        EightDSnapshot snapshot;
        if (report != null && report.estEmis()) {
            // Un rapport émis se relit de son instantané, jamais des sources : c'est
            // tout l'intérêt de l'avoir figé.
            snapshot = codec.decode(report.getSnapshotJson());
        } else {
            snapshot = assembler.assemble(collecte,
                    report == null ? null : report.getTeam(),
                    report == null ? null : report.getContainment(),
                    report == null ? null : report.getRecognition(),
                    tenantId.toString(), null, null);
        }

        List<EightDDto.DisciplineView> disciplines = new ArrayList<>();
        Map<String, EightDDiscipline> parCode = new HashMap<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            parCode.put(discipline.code(), discipline);
        }
        for (EightDSnapshot.Section section : snapshot.sections()) {
            EightDDiscipline discipline = parCode.get(section.code());
            disciplines.add(new EightDDto.DisciplineView(
                    section.code(), section.title(), section.sourced(), section.sourceLabel(),
                    section.lines(), discipline != null && discipline.estSaisie()));
        }

        boolean emis = report != null && report.estEmis();
        return new EightDDto.ReportView(
                ncId,
                collecte.nc().reference(),
                collecte.nc().title(),
                emis ? "ISSUED" : "DRAFT",
                !emis && collecte.nc().status() == NcStatus.CLOSED,
                snapshot.partial(),
                snapshot.missingCodes(),
                report == null ? null : report.getTeam(),
                report == null ? null : report.getContainment(),
                report == null ? null : report.getRecognition(),
                disciplines,
                emis ? new EightDDto.SealView(report.getSha256Hex(), report.getAnchorTxRef(),
                        report.getVerificationCode(), report.getIssuedAt(), report.getIssuedByName())
                     : null);
    }

    // ---------- utilitaires ----------

    /** Détails journalisés : des faits, aucune donnée nominative (§11.3, §22.9). */
    private String detailsJson(String reference, String sha256, String anchorTxRef,
                               EightDSnapshot snapshot) {
        return "{\"reference\":\"" + echapper(reference) + "\","
                + "\"sha256\":\"" + echapper(sha256) + "\","
                + "\"anchorTxRef\":\"" + echapper(anchorTxRef) + "\","
                + "\"partial\":" + snapshot.partial() + ","
                + "\"missing\":\"" + echapper(String.join(",", snapshot.missingCodes())) + "\"}";
    }

    private static String echapper(String valeur) {
        return valeur == null ? "" : valeur.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Code de vérification : 24 octets aléatoires, 32 caractères Base64URL. */
    private String nouveauCode() {
        byte[] buffer = new byte[24];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static String stem(String reference) {
        String s = reference.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return s.isBlank() ? "rapport" : s;
    }

    private static String sha256(byte[] contenu) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(contenu);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                   .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
