package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.pdca.PdcaDto;
import com.openlab.qualitos.quality.pdca.PdcaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class IshikawaService {

    /** Borne de génération (latence CPU raisonnable ; cf. ADR 0014). */
    private static final int SUGGEST_MAX_TOKENS = 320;
    /** Nombre max de suggestions retournées. */
    private static final int SUGGEST_MAX = 12;
    /** Préfixe catégorie entre accolades/crochets : « {METHODS} - cause » / « [Méthodes] : cause ». */
    private static final java.util.regex.Pattern BRACED =
            java.util.regex.Pattern.compile("^[\\{\\[]\\s*([^\\}\\]]+?)\\s*[\\}\\]]\\s*[-–—:|]*\\s*(.*)$");
    /** Séparateurs catégorie/cause (espacés d'abord pour ne pas casser « Main-d'œuvre »). */
    private static final String[] SEPARATORS = { " | ", " - ", " – ", " — ", " : ", "|", ":" };

    /** Longueur max du titre d'un cycle PDCA (colonne VARCHAR(255)). */
    private static final int PDCA_TITLE_MAX = 255;

    private final IshikawaDiagramRepository diagramRepository;
    private final IshikawaCauseRepository causeRepository;
    private final AiGatewayClient ai;
    private final PdcaService pdcaService;

    public IshikawaService(IshikawaDiagramRepository diagramRepository,
                           IshikawaCauseRepository causeRepository,
                           AiGatewayClient ai,
                           PdcaService pdcaService) {
        this.diagramRepository = diagramRepository;
        this.causeRepository = causeRepository;
        this.ai = ai;
        this.pdcaService = pdcaService;
    }

    @Transactional(readOnly = true)
    public Page<IshikawaDto.DiagramResponse> findAll(IshikawaStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<IshikawaDiagram> page = status != null
                ? diagramRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : diagramRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toDiagramResponse);
    }

    @Transactional(readOnly = true)
    public IshikawaDto.DiagramResponse findById(UUID id) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(id));
        return toDiagramResponse(diagram);
    }

    /**
     * Convertit un diagramme Ishikawa (et, optionnellement, une cause ciblée) en un
     * cycle PDCA (CLAUDE.md §3.6 — référentiel commun : une cause-racine devient un
     * plan d'action). Tenant-scopé (404 si le diagramme/la cause n'appartient pas au
     * tenant). Le cycle hérite de l'owner du diagramme ; son titre/description
     * référencent le problème et la cause d'origine pour la traçabilité.
     */
    public PdcaDto.CycleResponse convertToPdca(UUID diagramId, UUID causeId) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        IshikawaCause cause = null;
        if (causeId != null) {
            // findByIdAndDiagramId garantit l'appartenance au diagramme (lui-même tenant-scopé).
            cause = causeRepository.findByIdAndDiagramId(causeId, diagramId)
                    .orElseThrow(() -> new IshikawaCauseNotFoundException(causeId));
        }

        String problem = diagram.getProblemStatement();
        String title = truncate("PDCA — " + problem, PDCA_TITLE_MAX);
        StringBuilder desc = new StringBuilder()
                .append("Issu du diagramme Ishikawa « ").append(problem).append(" ».");
        if (cause != null) {
            desc.append(" Cause-racine ciblée [").append(cause.getCategory()).append("] : ")
                    .append(cause.getLabel()).append('.');
        }
        return pdcaService.createCycle(
                new PdcaDto.CreateCycleRequest(title, desc.toString(), diagram.getOwnerId()));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    public IshikawaDto.DiagramResponse createDiagram(IshikawaDto.CreateDiagramRequest request) {
        UUID tenantId = requireTenantId();

        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setTenantId(tenantId);
        diagram.setProblemStatement(request.problemStatement());
        diagram.setDescription(request.description());
        diagram.setMode(request.mode() != null ? request.mode() : IshikawaMode.SIX_M);
        diagram.setStatus(IshikawaStatus.DRAFT);
        diagram.setOwnerId(request.ownerId());

        return toDiagramResponse(diagramRepository.save(diagram));
    }

    public IshikawaDto.DiagramResponse updateDiagram(UUID id, IshikawaDto.UpdateDiagramRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(id));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Archived diagram cannot be modified");
        }

        if (request.problemStatement() != null) {
            diagram.setProblemStatement(request.problemStatement());
        }
        if (request.description() != null) {
            diagram.setDescription(request.description());
        }
        if (request.mode() != null) {
            validateModeChange(diagram, request.mode());
            diagram.setMode(request.mode());
        }
        if (request.status() != null) {
            validateStatusTransition(diagram.getStatus(), request.status());
            diagram.setStatus(request.status());
        }

        return toDiagramResponse(diagramRepository.save(diagram));
    }

    public void deleteDiagram(UUID id) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(id));

        if (diagram.getStatus() == IshikawaStatus.VALIDATED) {
            throw new IshikawaStateException(
                    "Validated diagram cannot be deleted; archive it instead");
        }

        diagramRepository.delete(diagram);
    }

    public IshikawaDto.CauseResponse addCause(UUID diagramId, IshikawaDto.CauseRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Cannot add a cause to an archived diagram");
        }

        if (!diagram.getMode().allows(request.category())) {
            throw new IshikawaStateException(
                    "Category " + request.category() + " is not allowed by mode " + diagram.getMode());
        }

        IshikawaCause cause = new IshikawaCause();
        cause.setDiagram(diagram);
        cause.setCategory(request.category());
        cause.setLabel(request.label());
        cause.setDescription(request.description());
        cause.setRootCauseScore(request.rootCauseScore());

        if (request.parentId() != null) {
            IshikawaCause parent = causeRepository.findByIdAndDiagramId(request.parentId(), diagramId)
                    .orElseThrow(() -> new IshikawaCauseNotFoundException(request.parentId()));
            if (parent.getCategory() != request.category()) {
                throw new IshikawaStateException(
                        "Child cause category must match parent category " + parent.getCategory());
            }
            cause.setParent(parent);
        }

        return toCauseResponse(causeRepository.save(cause));
    }

    /**
     * Suggère par l'IA (passerelle → ai-service) des causes racines probables pour le
     * problème du diagramme, réparties par catégorie autorisée par le mode (6M/7M/8M).
     * L'IA suggère, l'humain valide : les suggestions ne sont PAS persistées (§3.5, §12.3).
     */
    @Transactional(readOnly = true)
    public List<IshikawaDto.SuggestedCause> suggestCauses(UUID diagramId) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        List<CauseCategory> allowed = Arrays.stream(CauseCategory.values())
                .filter(c -> diagram.getMode().allows(c)).toList();
        Set<String> existing = diagram.getCauses().stream()
                .map(c -> c.getLabel().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        String allowedList = allowed.stream()
                .map(c -> c.name() + " (" + frLabel(c) + ")").collect(Collectors.joining(", "));

        // Prompt concret + exemple : les petits modèles recopient les gabarits abstraits
        // (« CODE | cause ») au lieu de les instancier — un exemple réel est imité plus fidèlement.
        String system = "Tu es un expert qualité (méthode Ishikawa). Pour le problème donné, "
                + "propose des causes racines PROBABLES. Donne UNE cause par ligne au format "
                + "« [CATÉGORIE] cause concrète ». Exemple : [MACHINES] réglage de l'équipement dérivé. "
                + "Utilise UNIQUEMENT ces catégories : " + allowedList + ". "
                + "Donne 8 à 12 causes variées et concrètes, sans numéro ni autre texte.";
        StringBuilder user = new StringBuilder("Problème : ")
                .append(diagram.getProblemStatement()).append("\n");
        if (diagram.getDescription() != null && !diagram.getDescription().isBlank()) {
            user.append("Contexte : ").append(diagram.getDescription()).append("\n");
        }
        user.append("Liste les causes probables :");

        AiCompletionResult r = ai.complete(system, user.toString(), SUGGEST_MAX_TOKENS);
        return parseSuggestions(r.text(), allowed, existing);
    }

    /**
     * Parse tolérant : le petit modèle ne respecte pas toujours « CODE | cause ».
     * Gère deux formats fréquents :
     *   (a) inline      : « METHODS | cause » ou « Méthodes : cause »
     *   (b) en-tête+liste: « Méthodes : » (en-tête) puis « - cause » sur les lignes suivantes.
     * Une ligne sans catégorie reconnaissable est rattachée à la dernière catégorie vue.
     */
    private List<IshikawaDto.SuggestedCause> parseSuggestions(
            String text, List<CauseCategory> allowed, Set<String> existing) {
        List<IshikawaDto.SuggestedCause> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (text == null) return out;
        CauseCategory current = null;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.strip().replaceFirst("^[-*•·\\d.)\\s]+", "").strip();
            if (line.isBlank()) continue;
            String[] parts = splitCauseLine(line);
            if (parts != null) {
                CauseCategory cat = categoryFromToken(parts[0]);
                if (cat != null) {
                    current = allowed.contains(cat) ? cat : null;
                    String rest = parts[1].strip();
                    if (current != null && !rest.isBlank()) {
                        addSuggestion(out, seen, existing, current, rest);
                    }
                    if (out.size() >= SUGGEST_MAX) break;
                    continue;
                }
            }
            // Ligne de cause rattachée à la catégorie courante (format en-tête + liste).
            if (current != null) {
                addSuggestion(out, seen, existing, current, line);
                if (out.size() >= SUGGEST_MAX) break;
            }
        }
        return out;
    }

    /** Sépare une ligne en [token-catégorie, cause] selon accolades ou un séparateur ; null si aucun. */
    private String[] splitCauseLine(String line) {
        java.util.regex.Matcher m = BRACED.matcher(line);
        if (m.matches()) {
            return new String[] { m.group(1), m.group(2) };
        }
        for (String sep : SEPARATORS) {
            int i = line.indexOf(sep);
            if (i > 0) {
                return new String[] { line.substring(0, i), line.substring(i + sep.length()) };
            }
        }
        return null;
    }

    private void addSuggestion(List<IshikawaDto.SuggestedCause> out, Set<String> seen,
                               Set<String> existing, CauseCategory cat, String labelRaw) {
        String label = labelRaw.strip()
                // Retire un éventuel 2ᵉ préfixe entre crochets/accolades laissé par le modèle.
                .replaceFirst("^[\\{\\[][^\\}\\]]*[\\}\\]]\\s*[-–—:|]*\\s*", "")
                .replaceFirst("^[-*•·:\\s]+", "").strip();
        if (label.length() > 500) label = label.substring(0, 500);
        if (label.isBlank()) return;
        String key = label.toLowerCase(Locale.ROOT);
        if (existing.contains(key) || !seen.add(key)) return;
        out.add(new IshikawaDto.SuggestedCause(cat, label, null));
    }

    /** Tolérant : accepte le code enum ou un synonyme FR/EN. */
    private CauseCategory categoryFromToken(String token) {
        String t = token.strip().toUpperCase(Locale.ROOT);
        if (t.isEmpty()) return null;
        if (t.contains("METHOD") || t.contains("MÉTHOD") || t.contains("METHOD")) return CauseCategory.METHODS;
        if (t.contains("MANPOWER") || t.contains("MAIN") || t.contains("ŒUVRE") || t.contains("OEUVRE")
                || t.contains("PERSONNEL")) return CauseCategory.MANPOWER;
        if (t.contains("MACHINE") || t.contains("MATÉRIEL") || t.contains("MATERIEL")
                || t.contains("ÉQUIP") || t.contains("EQUIP")) return CauseCategory.MACHINES;
        if (t.contains("MATERIAL") || t.contains("MATIÈRE") || t.contains("MATIERE")) return CauseCategory.MATERIALS;
        if (t.contains("MEASURE") || t.contains("MESURE")) return CauseCategory.MEASUREMENTS;
        if (t.contains("ENVIRON") || t.contains("MILIEU")) return CauseCategory.ENVIRONMENT;
        if (t.contains("MANAGEMENT") || t.contains("MANAGE")) return CauseCategory.MANAGEMENT;
        if (t.contains("MONEY") || t.contains("MOYEN") || t.contains("ARGENT")
                || t.contains("FINANC")) return CauseCategory.MONEY;
        return null;
    }

    private String frLabel(CauseCategory c) {
        return switch (c) {
            case METHODS -> "Méthodes";
            case MANPOWER -> "Main-d'œuvre";
            case MACHINES -> "Machines";
            case MATERIALS -> "Matières";
            case MEASUREMENTS -> "Mesures";
            case ENVIRONMENT -> "Milieu";
            case MANAGEMENT -> "Management";
            case MONEY -> "Moyens financiers";
        };
    }

    public IshikawaDto.CauseResponse updateCause(UUID diagramId, UUID causeId,
                                                 IshikawaDto.UpdateCauseRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Cannot modify a cause on an archived diagram");
        }

        IshikawaCause cause = causeRepository.findByIdAndDiagramId(causeId, diagramId)
                .orElseThrow(() -> new IshikawaCauseNotFoundException(causeId));

        if (request.category() != null) {
            if (!diagram.getMode().allows(request.category())) {
                throw new IshikawaStateException(
                        "Category " + request.category() + " is not allowed by mode " + diagram.getMode());
            }
            if (cause.getParent() != null && cause.getParent().getCategory() != request.category()) {
                throw new IshikawaStateException(
                        "Child cause category must match parent category " + cause.getParent().getCategory());
            }
            cause.setCategory(request.category());
        }
        if (request.label() != null) {
            cause.setLabel(request.label());
        }
        if (request.description() != null) {
            cause.setDescription(request.description());
        }
        if (request.rootCauseScore() != null) {
            cause.setRootCauseScore(request.rootCauseScore());
        }

        return toCauseResponse(causeRepository.save(cause));
    }

    public void deleteCause(UUID diagramId, UUID causeId) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Cannot delete a cause on an archived diagram");
        }

        IshikawaCause cause = causeRepository.findByIdAndDiagramId(causeId, diagramId)
                .orElseThrow(() -> new IshikawaCauseNotFoundException(causeId));

        causeRepository.delete(cause);
    }

    private void validateModeChange(IshikawaDiagram diagram, IshikawaMode newMode) {
        // Refuse de réduire le mode si des causes appartiennent à une catégorie qui disparaîtrait.
        boolean hasIncompatibleCause = diagram.getCauses().stream()
                .anyMatch(c -> !newMode.allows(c.getCategory()));
        if (hasIncompatibleCause) {
            throw new IshikawaStateException(
                    "Cannot switch to mode " + newMode + ": diagram has causes outside the allowed categories");
        }
    }

    private void validateStatusTransition(IshikawaStatus current, IshikawaStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == IshikawaStatus.IN_REVIEW || next == IshikawaStatus.ARCHIVED;
            case IN_REVIEW -> next == IshikawaStatus.VALIDATED
                    || next == IshikawaStatus.DRAFT
                    || next == IshikawaStatus.ARCHIVED;
            case VALIDATED -> next == IshikawaStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!valid) {
            throw new IshikawaStateException(
                    "Invalid status transition: " + current + " -> " + next);
        }
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private IshikawaDto.DiagramResponse toDiagramResponse(IshikawaDiagram diagram) {
        return new IshikawaDto.DiagramResponse(
                diagram.getId(),
                diagram.getTenantId(),
                diagram.getProblemStatement(),
                diagram.getDescription(),
                diagram.getMode(),
                diagram.getStatus(),
                diagram.getOwnerId(),
                diagram.getCreatedAt(),
                diagram.getUpdatedAt(),
                diagram.getCauses().stream().map(this::toCauseResponse).toList()
        );
    }

    private IshikawaDto.CauseResponse toCauseResponse(IshikawaCause cause) {
        return new IshikawaDto.CauseResponse(
                cause.getId(),
                cause.getDiagram().getId(),
                cause.getParent() != null ? cause.getParent().getId() : null,
                cause.getCategory(),
                cause.getLabel(),
                cause.getDescription(),
                cause.getRootCauseScore(),
                cause.getCreatedAt(),
                cause.getUpdatedAt()
        );
    }
}
