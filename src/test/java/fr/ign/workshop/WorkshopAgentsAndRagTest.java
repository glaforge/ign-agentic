/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ign.workshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.SequentialAgent;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import fr.ign.workshop._03_AntigravityDetectiveAgent.QualificationReport;
import fr.ign.workshop.tools.GeoplateformeTools;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.Policies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tests JUnit 6 des Architectures Agents (Actes 1, 2, 3) & Skill IGN")
class WorkshopAgentsAndRagTest {

    @Test
    @DisplayName("Le Skill IGN Géoplateforme existe et contient la documentation SKILL.md")
    void shouldFindIgnGeoplateformeSkillDirectory() throws Exception {
        Path skillDir = Path.of(".agents/skills/ign-geoplateforme");
        assertTrue(Files.isDirectory(skillDir), "Le chemin doit être un répertoire existant : " + skillDir);

        Path skillMd = skillDir.resolve("SKILL.md");
        assertTrue(Files.exists(skillMd), "Le fichier SKILL.md doit exister dans " + skillDir);

        String content = Files.readString(skillMd);
        assertTrue(content.contains("CRS:84"), "SKILL.md doit documenter la projection CRS:84");
        assertTrue(content.contains("BDTOPO_V3:batiment"), "SKILL.md doit documenter la couche BDTOPO_V3:batiment");
    }

    @Test
    @DisplayName("Acte 1 (LangChain4j) : Indexation RAG ONNX locale du PLU & ZAN et recherche sémantique")
    void shouldIndexAndRetrievePluAndZanRulesOffline() throws Exception {
        Path pluPath = Path.of("src", "main", "resources", "plu-saint-mande-extrait.md");
        assertTrue(Files.exists(pluPath), "L'extrait du PLU de Saint-Mandé doit exister");

        Document pluDoc = Document.from(Files.readString(pluPath));
        EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(600, 80))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(pluDoc);

        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .build();

        // Recherche sémantique sur la règle de hauteur en lisière forestière (Bois de Vincennes)
        List<Content> heightResults = retriever.retrieve(
                Query.from("Quelle est la hauteur maximale autorisée à proximité d'une lisière forestière ou du Bois de Vincennes ?"));
        assertFalse(heightResults.isEmpty());
        String combinedHeightText = heightResults.stream()
                .map(c -> c.textSegment().text())
                .collect(Collectors.joining("\n"));
        assertTrue(combinedHeightText.contains("15 mètres") || combinedHeightText.contains("15m"),
                "Le RAG doit retrouver la règle abaissant le plafond à 15 mètres en lisière de forêt");

        // Recherche sémantique sur le critère ZAN de renouvellement urbain
        List<Content> zanResults = retriever.retrieve(
                Query.from("Combien de bâtiments dans un rayon de 100 mètres sont requis pour le renouvellement urbain ZAN ?"));
        assertFalse(zanResults.isEmpty());
        String combinedZanText = zanResults.stream()
                .map(c -> c.textSegment().text())
                .collect(Collectors.joining("\n"));
        assertTrue(combinedZanText.contains("15"),
                "Le RAG doit retrouver le seuil de 15 bâtiments dans la BD TOPO pour le ZAN");
    }

    @Test
    @DisplayName("Acte 2 (Google ADK Java) : Vérifie la topologie SequentialAgent + ParallelAgent de la cellule de crise")
    void shouldVerifyAdkMultiAgentHierarchy() {
        BaseAgent root = _02_AdkMultiAgentCrisisSimulation.ROOT_AGENT;
        assertNotNull(root);
        assertEquals("cellule-crise-ign", root.name());
        assertInstanceOf(SequentialAgent.class, root);

        List<? extends BaseAgent> topSubAgents = root.subAgents();
        assertEquals(3, topSubAgents.size(), "Le SequentialAgent doit orchestrer 3 étapes");

        assertEquals("agent-localisation", topSubAgents.get(0).name());
        assertEquals("collecte-territoriale-parallele", topSubAgents.get(1).name());
        assertEquals("coordinateur-secours", topSubAgents.get(2).name());

        BaseAgent parallelStep = topSubAgents.get(1);
        assertInstanceOf(ParallelAgent.class, parallelStep);
        List<? extends BaseAgent> parallelAgents = parallelStep.subAgents();
        assertEquals(3, parallelAgents.size(), "Le ParallelAgent doit exécuter 3 agents spécialisés en concurrence");

        List<String> parallelNames = parallelAgents.stream().map(BaseAgent::name).toList();
        assertEquals(List.of("agent-topographe", "agent-enjeux-bati", "agent-logistique"), parallelNames);
    }

    @Test
    @DisplayName("Acte 3 (Antigravity SDK) : Configuration de l'agent et sérialisation du record QualificationReport")
    void shouldConfigureAntigravityAgentAndSerializeQualificationReport() throws Exception {
        String skillPath = Path.of(".agents/skills/ign-geoplateforme").toAbsolutePath().toString();

        AgentConfig config = AgentConfig.builder()
                .instructions("Instructions de test Détective Territorial")
                .addTool(new GeoplateformeTools())
                .addSkillPath(skillPath)
                .addPolicy(Policies.allowTools("geocode_address", "query_bdtopo_buildings"))
                .responseSchema(QualificationReport.class)
                .build();

        assertNotNull(config);
        assertNotNull(config.getResponseSchema());
        assertTrue(config.getResponseSchema().contains("adresse_inspectee"));
        assertEquals(1, config.getToolInstances().size());
        assertEquals(List.of(skillPath), config.getSkillsPaths());
        assertEquals(1, config.getPolicies().size());

        QualificationReport sampleReport = new QualificationReport(
                "73 Avenue de Paris, 94160 Saint-Mandé",
                2.4237,
                48.8447,
                50.48,
                77,
                "Deux grands bâtiments tertiaires avec toiture végétalisée et verrière centrale",
                "Parfaite concordance géométrique avec BATIMENT0000000334291975",
                "SUBMITTED (IGN-COLLAB-12345)",
                List.of("Valider l'attribut usage tertiaire", "Maintenir le suivi de la lisière forestière")
        );

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(sampleReport);
        assertTrue(json.contains("\"adresse_inspectee\":\"73 Avenue de Paris, 94160 Saint-Mandé\""));

        QualificationReport deserialized = mapper.readValue(json, QualificationReport.class);
        assertEquals(sampleReport, deserialized);
    }
}
