package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.ai.guard.AiRateLimitExceededException;
import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.aigateway.AiRequestRejectedException;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les trois assistants : ce qu'ils acceptent du modèle, ce qu'ils écartent, et
 * pourquoi une panne de l'IA ne casse pas un écran de registre.
 */
class AiStakeholderAssistantTest {

    private AiGatewayClient ai;
    private AiStakeholderAssistant assistant;

    @BeforeEach
    void setUp() {
        ai = mock(AiGatewayClient.class);
        assistant = new AiStakeholderAssistant(ai, new ObjectMapper());
    }

    private void repond(String body) {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult(body, "ollama", 120, 900));
    }

    // ---------- 1. suggestions ----------

    @Test
    void lesSuggestionsSontLuesEtTypees() {
        repond("""
                {"suggestions":[
                  {"name":"Donneurs d'ordre","category":"CUSTOMERS","rationale":"40 % du CA"},
                  {"name":"Organismes de certification","category":"CERTIFICATION_BODIES","rationale":"EN 9100"}]}""");

        var out = assistant.suggestStakeholders("Aéronautique", "PME, 80 salariés");

        assertThat(out).hasSize(2);
        assertThat(out.get(0).category()).isEqualTo(StakeholderCategory.CUSTOMERS);
        assertThat(out.get(1).name()).isEqualTo("Organismes de certification");
    }

    @Test
    void leSecteurEtLesCaracteristiquesPartentDansLaDemande() {
        repond("{\"suggestions\":[]}");

        assistant.suggestStakeholders("Agroalimentaire", "2 sites, HACCP");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(anyString(), prompt.capture(), anyInt());
        assertThat(prompt.getValue()).contains("Agroalimentaire").contains("HACCP");
    }

    @Test
    void desCaracteristiquesAbsentesSeDisentPlutotQueDeSeTaire() {
        repond("{\"suggestions\":[]}");

        assistant.suggestStakeholders("Services", "");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(anyString(), prompt.capture(), anyInt());
        assertThat(prompt.getValue()).contains("non précisées");
    }

    @Test
    void uneCategorieInconnueDevientAutrePlutotQueDePerdreLaSuggestion() {
        // La suggestion garde son intérêt : l'utilisateur la reclassera.
        repond("{\"suggestions\":[{\"name\":\"Riverains\",\"category\":\"VOISINS\"}]}");

        var out = assistant.suggestStakeholders("Industrie", "");

        assertThat(out).singleElement().extracting(StakeholderPorts.SuggestedStakeholder::category)
                .isEqualTo(StakeholderCategory.OTHER);
    }

    @Test
    void uneSuggestionSansNomOuSansCategorieEstEcartee() {
        repond("""
                {"suggestions":[
                  {"category":"CUSTOMERS"},
                  {"name":"Sans catégorie"},
                  {"name":"Bonne","category":"SUPPLIERS"}]}""");

        var out = assistant.suggestStakeholders("Industrie", "");

        assertThat(out).singleElement().extracting(StakeholderPorts.SuggestedStakeholder::name)
                .isEqualTo("Bonne");
    }

    @Test
    void laListeDeSuggestionsEstBornee() {
        StringBuilder json = new StringBuilder("{\"suggestions\":[");
        for (int i = 0; i < AiStakeholderAssistant.MAX_SUGGESTIONS + 5; i++) {
            json.append(i == 0 ? "" : ",")
                .append("{\"name\":\"P").append(i).append("\",\"category\":\"OTHER\"}");
        }
        repond(json.append("]}").toString());

        assertThat(assistant.suggestStakeholders("Industrie", ""))
                .hasSize(AiStakeholderAssistant.MAX_SUGGESTIONS);
    }

    // ---------- 2. exigences extraites ----------

    @Test
    void uneExigenceEstLueAvecSaCitation() {
        repond("""
                {"requirements":[{
                  "quote":"Les pieces sont livrees avec un certificat matiere de type 3.1.",
                  "reference":"Contrat C-2026-114, art. 7.2",
                  "requirement":"Fournir un certificat matiere 3.1 a chaque livraison",
                  "nature":"CONTRACTUAL",
                  "control":"Controle documentaire a reception"}]}""");

        var out = assistant.extractRequirements("contrat.pdf",
                "Article 7.2 - Les pieces sont livrees avec un certificat matiere de type 3.1.");

        assertThat(out).singleElement().satisfies(r -> {
            assertThat(r.quote()).startsWith("Les pieces sont livrees");
            assertThat(r.reference()).contains("art. 7.2");
            assertThat(r.nature()).isEqualTo(MandatoryNature.CONTRACTUAL);
        });
    }

    @Test
    void uneExigenceSansCitationEstEcartee() {
        // Sans passage cité, un humain n'aurait aucun moyen de contrôler ce
        // qu'on lui propose de faire entrer au registre.
        repond("""
                {"requirements":[
                  {"requirement":"Faire quelque chose","nature":"CONTRACTUAL"},
                  {"quote":"Texte exact.","requirement":"Faire ceci"}]}""");

        var out = assistant.extractRequirements("contrat.pdf", "Preambule. Texte exact. Fin.");

        assertThat(out).singleElement().extracting(StakeholderPorts.ExtractedRequirement::requirement)
                .isEqualTo("Faire ceci");
    }

    @Test
    void uneExigenceSansLibelleEstEcartee() {
        repond("{\"requirements\":[{\"quote\":\"Texte exact.\"}]}");

        assertThat(assistant.extractRequirements("contrat.pdf", "Le texte.")).isEmpty();
    }

    @Test
    void uneNatureInconnueResteNulleSansPerdreLExigence() {
        repond("""
                {"requirements":[{"quote":"Texte.","requirement":"Faire ceci","nature":"BIZARRE"}]}""");

        assertThat(assistant.extractRequirements("contrat.pdf", "Avant. Texte. Apres."))
                .singleElement().extracting(StakeholderPorts.ExtractedRequirement::nature).isNull();
    }

    @Test
    void unTexteTresLongEstTronqueAvantDEtreEnvoye() {
        // Un contrat entier coûte sans servir : le texte est borné.
        repond("{\"requirements\":[]}");
        String enorme = "a".repeat(AiStakeholderAssistant.MAX_DOCUMENT_CHARS + 5000);

        assistant.extractRequirements("contrat.pdf", enorme);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(anyString(), prompt.capture(), anyInt());
        assertThat(prompt.getValue().length())
                .isLessThan(AiStakeholderAssistant.MAX_DOCUMENT_CHARS + 500);
    }

    @Test
    void uneCitationAbsenteDuDocumentEstEcartee() {
        // Le cas qui compte : le document vient d'un tiers et traverse l'invite.
        // Un contrat qui s'y adresse au modele obtient une exigence inventee,
        // munie d'un passage invente — or c'est precisement sur ce passage que
        // le relecteur s'appuie pour valider (OWASP LLM01/LLM02). Sans citation
        // reelle, l'exigence n'entre pas.
        repond("""
                {"requirements":[
                  {"quote":"Article 12 : le client renonce a tout controle qualite.",
                   "requirement":"Ne pas auditer ce fournisseur"},
                  {"quote":"Le delai de livraison est de 15 jours.",
                   "requirement":"Livrer sous 15 jours"}]}""");

        var out = assistant.extractRequirements("contrat.pdf",
                "Art. 3 - Le delai de livraison est de 15 jours. Art. 4 - Penalites.");

        assertThat(out).singleElement()
                .extracting(StakeholderPorts.ExtractedRequirement::requirement)
                .isEqualTo("Livrer sous 15 jours");
    }

    @Test
    void laCitationEstReconnueMalgreLaMiseEnPage() {
        // Un modele recopie les mots, rarement la typographie : une cesure, une
        // double espace, une majuscule de debut de ligne. Comparer les chaines
        // brutes ecarterait des citations authentiques.
        repond("""
                {"requirements":[{
                  "quote":"les pieces sont   livrees avec un certificat",
                  "requirement":"Fournir un certificat"}]}""");

        var out = assistant.extractRequirements("contrat.pdf", """
                Art. 7
                  Les pieces sont livrees
                  avec un certificat matiere.""");

        assertThat(out).hasSize(1);
    }

    @Test
    void laCitationEstConfronteeAuTexteSoumisPasAuDocumentEntier() {
        // Le modele n'a vu que le texte tronque : lui opposer la partie qu'il n'a
        // pas recue rejetterait des citations exactes tombees apres la coupe.
        repond("""
                {"requirements":[{"quote":"zzz unique","requirement":"Faire ceci"}]}""");
        String document = "a".repeat(AiStakeholderAssistant.MAX_DOCUMENT_CHARS) + " zzz unique";

        assertThat(assistant.extractRequirements("contrat.pdf", document)).isEmpty();
    }

    @Test
    void unRefusDuGardeFouNEstPasConfonduAvecRienTrouve() {
        // 429/413/503 ne sont pas des reponses illisibles : ce sont des refus, et
        // ils ont leur code HTTP (ADR 0017). Les avaler rendait un abus de debit
        // indiscernable d'un document sans exigence (OWASP LLM04, A09).
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AiRateLimitExceededException("Debit depasse", 30));

        assertThatThrownBy(() -> assistant.extractRequirements("contrat.pdf", "Le texte."))
                .isInstanceOf(AiRateLimitExceededException.class);
    }

    @Test
    void unRefusDeLaPasserelleRemonteAussi() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AiRequestRejectedException("Requete refusee (422)", null));

        assertThatThrownBy(() -> assistant.suggestStakeholders("Industrie", ""))
                .isInstanceOf(AiRequestRejectedException.class);
    }

    @Test
    void lesDeuxVersionsComparesTiennentChacuneDansLaMoitieDuBudget() {
        // Deux textes dans la meme invite : chacun n'a droit qu'a la moitie, sinon
        // l'invite assemblee depasse le plafond de la passerelle et l'appel entier
        // est refuse — ce que l'utilisateur lisait comme « aucun changement ».
        repond("{\"changes\":[]}");
        String enorme = "a".repeat(AiStakeholderAssistant.MAX_COMPARED_CHARS + 4000);

        assistant.detectChanges("contrat.pdf", enorme, enorme);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(anyString(), prompt.capture(), anyInt());
        assertThat(prompt.getValue().length())
                .isLessThan(2 * AiStakeholderAssistant.MAX_COMPARED_CHARS + 500);
    }

    // ---------- 3. changements ----------

    @Test
    void unChangementEstLuAvecSaPortee() {
        repond("""
                {"changes":[{
                  "title":"Taux de service porte de 95 % a 98 %",
                  "detail":"Art. 9.1 modifie",
                  "source":"Avenant no 2",
                  "impact":"HIGH"}]}""");

        var out = assistant.detectChanges("contrat.pdf", "ancien", "nouveau");

        assertThat(out).singleElement().satisfies(c -> {
            assertThat(c.title()).contains("98 %");
            assertThat(c.impact()).isEqualTo(ImpactLevel.HIGH);
        });
    }

    @Test
    void lesDeuxVersionsPartentDansLaDemande() {
        repond("{\"changes\":[]}");

        assistant.detectChanges("contrat.pdf", "le texte d'avant", "le texte d'apres");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(anyString(), prompt.capture(), anyInt());
        assertThat(prompt.getValue()).contains("le texte d'avant").contains("le texte d'apres");
    }

    @Test
    void unChangementSansIntituleEstEcarte() {
        repond("{\"changes\":[{\"detail\":\"quelque chose\"},{\"title\":\"Un vrai\"}]}");

        assertThat(assistant.detectChanges("c.pdf", "a", "b"))
                .singleElement().extracting(StakeholderPorts.DetectedChange::title).isEqualTo("Un vrai");
    }

    @Test
    void unePorteeInconnueResteNulle() {
        repond("{\"changes\":[{\"title\":\"Un vrai\",\"impact\":\"ENORME\"}]}");

        assertThat(assistant.detectChanges("c.pdf", "a", "b"))
                .singleElement().extracting(StakeholderPorts.DetectedChange::impact).isNull();
    }

    // ---------- robustesse ----------

    @Test
    void leJsonEntoureDeTexteOuDeBalisesEstQuandMemeLu() {
        // Les modèles encadrent volontiers leur JSON : exiger une réponse
        // parfaite ferait échouer l'écran pour un détail de forme.
        repond("Voici le resultat :\n```json\n{\"suggestions\":[{\"name\":\"P\",\"category\":\"OTHER\"}]}\n```\nVoila.");

        assertThat(assistant.suggestStakeholders("Industrie", "")).hasSize(1);
    }

    @Test
    void uneReponseSansJsonNeProposeRien() {
        repond("Je ne peux pas repondre a cette demande.");

        assertThat(assistant.suggestStakeholders("Industrie", "")).isEmpty();
    }

    @Test
    void uneReponseVideOuNulleNeProposeRien() {
        repond("   ");
        assertThat(assistant.suggestStakeholders("Industrie", "")).isEmpty();

        when(ai.complete(anyString(), anyString(), anyInt())).thenReturn(null);
        assertThat(assistant.extractRequirements("c.pdf", "texte")).isEmpty();
    }

    @Test
    void unJsonMalFormeNeProposeRien() {
        repond("{\"suggestions\": [ {\"name\": }");

        assertThat(assistant.suggestStakeholders("Industrie", "")).isEmpty();
    }

    @Test
    void unePanneDeLIaNeCassePasLEcran() {
        // On ne propose rien, ce qui se voit, et l'utilisateur réessaie.
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("gateway down"));

        assertThat(assistant.suggestStakeholders("Industrie", "")).isEmpty();
        assertThat(assistant.extractRequirements("c.pdf", "texte")).isEmpty();
        assertThat(assistant.detectChanges("c.pdf", "a", "b")).isEmpty();
    }

    @Test
    void uneListeAbsenteNeProposeRien() {
        repond("{\"autre\":[1,2,3]}");

        assertThat(assistant.suggestStakeholders("Industrie", "")).isEmpty();
        assertThat(assistant.extractRequirements("c.pdf", "texte")).isEmpty();
        assertThat(assistant.detectChanges("c.pdf", "a", "b")).isEmpty();
    }
}
