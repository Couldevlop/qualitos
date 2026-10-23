package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrix;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixConflictException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixNotFoundException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixValidationException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;
import com.openlab.qualitos.quality.training.competencymatrix.domain.SkillLevel;
import com.openlab.qualitos.quality.training.competencymatrix.domain.Workshops;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La matrice de compétences éditable (ADR 0098) : la lire, y inscrire et en
 * retirer des opérateurs et des compétences, y poser des niveaux ; et les
 * ateliers du client.
 *
 * <p>Sans Spring ni JPA. Les transactions sont posées par le contrôleur : une
 * écriture et sa trace d'audit sont validées ensemble ou pas du tout.
 *
 * <p>Inscrire ou retirer quelqu'un rend la matrice entière : une personne
 * réinscrite retrouve ses évaluations, une compétence reprise du catalogue
 * arrive avec les siennes — la couverture de toutes les colonnes peut en
 * changer, et l'écran n'a pas à le deviner.
 */
public class CompetencyMatrixService {

    private final CompetencyMatrixPorts.Operators operators;
    private final CompetencyMatrixPorts.Skills skills;
    private final CompetencyMatrixPorts.Levels levels;
    private final CompetencyMatrixPorts.WorkshopSettings workshopSettings;
    private final CompetencyMatrixPorts.Context context;
    private final CompetencyMatrixPorts.Audit audit;
    private final Clock clock;

    public CompetencyMatrixService(CompetencyMatrixPorts.Operators operators,
                                   CompetencyMatrixPorts.Skills skills,
                                   CompetencyMatrixPorts.Levels levels,
                                   CompetencyMatrixPorts.WorkshopSettings workshopSettings,
                                   CompetencyMatrixPorts.Context context,
                                   CompetencyMatrixPorts.Audit audit,
                                   Clock clock) {
        this.operators = operators;
        this.skills = skills;
        this.levels = levels;
        this.workshopSettings = workshopSettings;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- lecture ----------

    public CompetencyMatrixDto.MatrixView view() {
        return view(context.requireTenantId());
    }

    // ---------- opérateurs ----------

    public CompetencyMatrixDto.MatrixView addOperator(CompetencyMatrixDto.AddOperatorCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        String nom = requireText(commande.userName(), "userName");
        String atelier = workshops(tenant).canonical(commande.workshop())
                .orElseThrow(() -> new CompetencyMatrixValidationException("workshop",
                        "Unknown workshop: " + commande.workshop()));
        if (operators.findByUser(tenant, commande.userId()).isPresent()) {
            throw new CompetencyMatrixConflictException("userId", "This person is already in the matrix");
        }
        MatrixOperator ajoute = operators.add(tenant, commande.userId(), nom, atelier, acteur, clock.instant());
        audit.operatorAdded(tenant, ajoute, acteur);
        return view(tenant);
    }

    /** Retire la ligne ; les évaluations de la personne restent à son dossier de formation. */
    public CompetencyMatrixDto.MatrixView removeOperator(UUID operatorId) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        MatrixOperator operateur = operators.findById(tenant, operatorId)
                .orElseThrow(() -> new CompetencyMatrixNotFoundException("Operator not in the matrix: " + operatorId));
        operators.remove(tenant, operatorId);
        audit.operatorRemoved(tenant, operateur, acteur);
        return view(tenant);
    }

    // ---------- compétences ----------

    /**
     * Ajoute une colonne. La compétence du catalogue qui porte déjà cet intitulé
     * est reprise, avec ses évaluations ; sinon elle est créée au catalogue. Le
     * formateur est obligatoire : c'est la personne vers qui se tourner quand la
     * compétence passe à risque.
     */
    public CompetencyMatrixDto.MatrixView addSkill(CompetencyMatrixDto.AddSkillCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        String intitule = requireText(commande.name(), "name");
        String categorie = requireText(commande.category(), "category");
        String formateur = requireText(commande.trainerName(), "trainerName");
        if (commande.trainerUserId() == null) {
            throw new CompetencyMatrixValidationException("trainerUserId", "A trainer is required");
        }
        String cle = intitule.toLowerCase(Locale.ROOT);
        if (skills.findAll(tenant).stream().anyMatch(s -> s.name().toLowerCase(Locale.ROOT).equals(cle))) {
            throw new CompetencyMatrixConflictException("name", "This competence is already in the matrix");
        }
        UUID skillId = skills.findCatalogueSkillByName(tenant, intitule)
                .map(existante -> {
                    skills.categorizeIfMissing(tenant, existante, categorie);
                    return existante;
                })
                .orElseGet(() -> skills.createCatalogueSkill(tenant, intitule, categorie));
        // Une compétence du catalogue déjà en colonne porte cet intitulé : le
        // contrôle ci-dessus l'a écartée, la base (unicité tenant + compétence)
        // tient le reste en cas d'ajouts concurrents.
        MatrixSkill ajoutee = skills.add(tenant, skillId, commande.trainerUserId(), formateur, acteur, clock.instant());
        audit.skillAdded(tenant, ajoutee, acteur);
        return view(tenant);
    }

    /** Retire la colonne ; la compétence reste au catalogue, et ses évaluations aux dossiers. */
    public CompetencyMatrixDto.MatrixView removeSkill(UUID skillId) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        MatrixSkill competence = skills.find(tenant, skillId)
                .orElseThrow(() -> new CompetencyMatrixNotFoundException("Competence not in the matrix: " + skillId));
        skills.remove(tenant, skillId);
        audit.skillRemoved(tenant, competence, acteur);
        return view(tenant);
    }

    // ---------- niveaux ----------

    /**
     * Pose le niveau d'une case. La valeur est absolue, pas un incrément : un
     * double envoi (double clic, nouvelle tentative réseau) ne fait pas tourner
     * le niveau deux fois.
     */
    public CompetencyMatrixDto.CellView setLevel(CompetencyMatrixDto.SetLevelCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        int niveau = SkillLevel.require(commande.level());
        MatrixOperator operateur = operators.findByUser(tenant, commande.userId())
                .orElseThrow(() -> new CompetencyMatrixNotFoundException("Operator not in the matrix"));
        skills.find(tenant, commande.skillId())
                .orElseThrow(() -> new CompetencyMatrixNotFoundException("Competence not in the matrix"));
        OptionalInt avant = levels.set(tenant, operateur.userId(), operateur.name(), commande.skillId(), niveau,
                acteur, LocalDate.now(clock));
        LevelCell cellule = new LevelCell(operateur.userId(), commande.skillId(), niveau);
        audit.levelSet(tenant, cellule, avant, acteur);
        return new CompetencyMatrixDto.CellView(cellule.userId(), cellule.skillId(), cellule.level());
    }

    // ---------- ateliers ----------

    public CompetencyMatrixDto.WorkshopsView workshops() {
        Workshops w = workshops(context.requireTenantId());
        return new CompetencyMatrixDto.WorkshopsView(w.names(), w.defaults());
    }

    /**
     * Remplace la liste. Un opérateur rattaché à un atelier retiré le garde :
     * on ne réaffecte pas quelqu'un en son absence ; le filtre le propose encore.
     */
    public CompetencyMatrixDto.WorkshopsView replaceWorkshops(List<String> names) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Workshops w = Workshops.configured(names);
        workshopSettings.replace(tenant, w.names(), acteur);
        audit.workshopsChanged(tenant, w.names(), acteur);
        return new CompetencyMatrixDto.WorkshopsView(w.names(), false);
    }

    // ---------- interne ----------

    private Workshops workshops(UUID tenant) {
        return workshopSettings.find(tenant)
                .filter(list -> !list.isEmpty())
                .map(list -> new Workshops(list, false))
                .orElseGet(Workshops::platformDefaults);
    }

    private CompetencyMatrixDto.MatrixView view(UUID tenant) {
        Workshops w = workshops(tenant);
        CompetencyMatrix matrice = CompetencyMatrix.assemble(w, operators.findAll(tenant), skills.findAll(tenant),
                levels.findAll(tenant));
        Map<UUID, CompetencyMatrix.Coverage> couverture = matrice.coverage().stream()
                .collect(Collectors.toMap(CompetencyMatrix.Coverage::skillId, Function.identity()));
        return new CompetencyMatrixDto.MatrixView(
                w.names(), w.defaults(),
                matrice.operators().stream()
                        .map(o -> new CompetencyMatrixDto.OperatorView(o.id(), o.userId(), o.name(), o.workshop()))
                        .toList(),
                matrice.skills().stream()
                        .map(s -> {
                            CompetencyMatrix.Coverage c = couverture.get(s.skillId());
                            return new CompetencyMatrixDto.SkillView(s.skillId(), s.name(), s.category(),
                                    s.trainerUserId(), s.trainerName(), c.autonomous(), c.atRisk());
                        })
                        .toList(),
                matrice.cells().stream()
                        .map(c -> new CompetencyMatrixDto.CellView(c.userId(), c.skillId(), c.level()))
                        .toList());
    }

    private static String requireText(String value, String field) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            throw new CompetencyMatrixValidationException(field, field + " is required");
        }
        return v;
    }
}
