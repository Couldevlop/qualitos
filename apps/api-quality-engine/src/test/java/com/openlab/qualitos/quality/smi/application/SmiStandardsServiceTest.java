package com.openlab.qualitos.quality.smi.application;

import com.openlab.qualitos.quality.smi.domain.StandardUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ajouter une norme au SMI : ce qui est proposé, dans quel ordre, et ce qui est refusé. */
class SmiStandardsServiceTest {

    static final UUID ISO_9001 = UUID.randomUUID();
    static final UUID ISO_14001 = UUID.randomUUID();
    static final UUID ISO_45001 = UUID.randomUUID();
    static final UUID ISO_27001 = UUID.randomUUID();
    static final UUID ISO_50001 = UUID.randomUUID();

    final List<SmiPorts.AdoptedStandard> pilotees = new ArrayList<>();
    final List<SmiPorts.CatalogStandard> catalogue = new ArrayList<>();
    final List<UUID> adoptions = new ArrayList<>();
    SmiStandardsService service;

    /** Le catalogue tel que les migrations le livrent : normes associées comprises. */
    @BeforeEach
    void setUp() {
        catalogue.add(norme(ISO_45001, "iso-45001", "iso-9001,iso-14001", false));
        catalogue.add(norme(ISO_27001, "iso-27001", "iso-27002,iso-27005,iso-27017,iso-27018", false));
        catalogue.add(norme(ISO_9001, "iso-9001", "iso-9000,iso-9004,iso-19011", true));
        catalogue.add(norme(ISO_50001, "iso-50001", "iso-9001,iso-14001", false));
        catalogue.add(norme(ISO_14001, "iso-14001", "iso-9001,iso-45001,iso-14064", false));
        pilotees.add(new SmiPorts.AdoptedStandard(UUID.randomUUID(), "iso-9001", "ISO 9001:2015"));

        SmiPorts.Standards standards = new SmiPorts.Standards() {
            @Override
            public List<SmiPorts.AdoptedStandard> adopted() {
                return pilotees;
            }

            @Override
            public Optional<SmiPorts.Alignment> alignment(UUID adoptionId) {
                return Optional.empty();
            }
        };
        SmiPorts.Catalog port = new SmiPorts.Catalog() {
            @Override
            public List<SmiPorts.CatalogStandard> managementSystems() {
                return catalogue;
            }

            @Override
            public SmiPorts.AdoptedStandard adopt(UUID standardId) {
                adoptions.add(standardId);
                SmiPorts.CatalogStandard s = catalogue.stream()
                        .filter(c -> c.standardId().equals(standardId)).findFirst().orElseThrow();
                return new SmiPorts.AdoptedStandard(UUID.randomUUID(), s.code(), s.name());
            }
        };
        service = new SmiStandardsService(standards, port);
    }

    static SmiPorts.CatalogStandard norme(UUID id, String code, String associees, boolean adoptee) {
        return new SmiPorts.CatalogStandard(id, code, code.toUpperCase(), "2015", List.of(associees.split(",")),
                adoptee);
    }

    @Test
    void avecIso9001PiloteeIso14001EstProposeeEnTete() {
        List<SmiDto.Candidate> c = service.candidates();

        // La norme déjà adoptée n'est pas reproposée.
        assertThat(c).extracting(SmiDto.Candidate::code)
                .containsExactly("iso-14001", "iso-45001", "iso-50001", "iso-27001");
        assertThat(c.get(0).suggested()).isTrue();
        assertThat(c.get(0).sharedWith()).containsExactly("iso-9001");
        // ISO 27001 ne cite pas ISO 9001, et ISO 9001 ne la cite pas : proposée, pas suggérée.
        assertThat(c.get(3).suggested()).isFalse();
        assertThat(c.get(3).sharedWith()).isEmpty();
    }

    @Test
    void lesRattachementsSeLisentDansLesDeuxSens() {
        // ISO 9001 cite ISO 19011 : une norme citée PAR une norme pilotée est rattachée elle aussi.
        UUID iso19011 = UUID.randomUUID();
        catalogue.add(new SmiPorts.CatalogStandard(iso19011, "iso-19011", "ISO 19011", "2018", List.of(), false));

        assertThat(service.candidates()).filteredOn(x -> x.code().equals("iso-19011"))
                .singleElement()
                .satisfies(x -> assertThat(x.sharedWith()).containsExactly("iso-9001"));
    }

    @Test
    void laPlusRattacheePasseDevant() {
        pilotees.add(new SmiPorts.AdoptedStandard(UUID.randomUUID(), "iso-14001", "ISO 14001"));
        catalogue.replaceAll(s -> s.standardId().equals(ISO_14001)
                ? norme(ISO_14001, "iso-14001", "iso-9001,iso-45001,iso-14064", true) : s);

        List<SmiDto.Candidate> c = service.candidates();

        // 45001 et 50001 citent toutes deux 9001 ET 14001 ; 27001 aucune.
        assertThat(c).extracting(SmiDto.Candidate::code).containsExactly("iso-45001", "iso-50001", "iso-27001");
        assertThat(c.get(0).sharedWith()).containsExactly("iso-9001", "iso-14001");
    }

    @Test
    void sansNormePiloteeToutEstProposeDansLOrdreNaturel() {
        pilotees.clear();
        catalogue.replaceAll(s -> s.standardId().equals(ISO_9001)
                ? norme(ISO_9001, "iso-9001", "iso-9000", false) : s);

        assertThat(service.candidates()).extracting(SmiDto.Candidate::code)
                .containsExactly("iso-9001", "iso-14001", "iso-27001", "iso-45001", "iso-50001");
        assertThat(service.candidates()).noneMatch(SmiDto.Candidate::suggested);
    }

    @Test
    void ajouterAdopteParLeStandardsHub() {
        SmiDto.StandardRef ref = service.add(ISO_14001);

        assertThat(adoptions).containsExactly(ISO_14001);
        assertThat(ref.code()).isEqualTo("iso-14001");
        assertThat(ref.adoptionId()).isNotNull();
    }

    @Test
    void uneNormeHorsSystemeDeManagementEstRefusee() {
        UUID iatf = UUID.randomUUID();

        assertThatThrownBy(() -> service.add(iatf))
                .isInstanceOf(StandardUnavailableException.class)
                .extracting(e -> ((StandardUnavailableException) e).reason())
                .isEqualTo(StandardUnavailableException.Reason.NOT_A_MANAGEMENT_SYSTEM);
        assertThat(adoptions).isEmpty();
    }

    @Test
    void uneNormeDejaAdopteeEstRefusee() {
        assertThatThrownBy(() -> service.add(ISO_9001))
                .isInstanceOf(StandardUnavailableException.class)
                .extracting(e -> ((StandardUnavailableException) e).reason())
                .isEqualTo(StandardUnavailableException.Reason.ALREADY_ADOPTED);
        assertThat(adoptions).isEmpty();
    }
}
