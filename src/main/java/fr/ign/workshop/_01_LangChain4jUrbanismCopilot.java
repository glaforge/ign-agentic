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

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import fr.ign.workshop.tools.GeoplateformeTools;
import io.github.glaforge.ansiren.MarkdownRenderer;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.glaforge.ansiren.Ansi.blue;
import static io.github.glaforge.ansiren.Ansi.bold;
import static io.github.glaforge.ansiren.Ansi.cyan;
import static io.github.glaforge.ansiren.Ansi.green;
import static io.github.glaforge.ansiren.Ansi.ready;

/**
 * ACTE 1 (LangChain4j) : Copilote d'Urbanisme « Text-to-GIS » & Suivi du ZAN.
 *
 * <p>Démontre comment combiner dans un agent Java déclaratif ({@link AiServices}) :
 * <ol>
 *   <li><b>Du RAG réglementaire</b> : indexation en mémoire d'un extrait de Plan Local d'Urbanisme (PLU)
 *       et des règles ZAN (Zéro Artificialisation Nette).</li>
 *   <li><b>Du Tool Calling Géospatial</b> : appels temps réel aux APIs de la Géoplateforme IGN
 *       (Géocodage, RGE ALTI pentes, WFS BD TOPO Bâtiments/Forêts/Cours d'eau, Isochrones) via {@link GeoplateformeTools}.</li>
 * </ol>
 */
public class _01_LangChain4jUrbanismCopilot {

    public interface UrbanismCopilot {
        @SystemMessage("""
                Tu es un Ingénieur Géomaticien et Instructeur d'Urbanisme expert de l'IGN.
                Ta mission est d'instruire la faisabilité réglementaire d'un projet d'aménagement en croisant :
                1. Les règles du Plan Local d'Urbanisme (PLU) et du ZAN fournies par ton système de recherche documentaire (RAG).
                2. Les données géospatiales réelles de la Géoplateforme IGN que tu DOIS interroger via tes outils :
                   - `geocode_address` pour obtenir les coordonnées (lon, lat) exactes du projet.
                   - `get_elevation_and_slope` (RGE ALTI) pour vérifier l'altitude et la pente locale (%) vis-à-vis de l'Article 3 du PLU.
                   - `query_bdtopo_buildings` (BD TOPO) dans un rayon de 100m pour vérifier le critère de renouvellement urbain ZAN (> 15 bâtiments, Article 1).
                   - `query_bdtopo_environment` (BD TOPO) dans un rayon de 150m pour vérifier la proximité d'un bois/forêt (Article 2.2 lisière forestière) et des cours d'eau (Article 3.2).
                   - `compute_isochrone` pour vérifier l'accessibilité piéton (600 secondes = 10 min, Article 4).

                Structure ta réponse finale en français avec :
                - 📍 Localisation & Coordonnées (WGS84 & Lambert-93)
                - 📊 Mesures Géoplateforme IGN relevées sur le terrain
                - ⚖️ Confrontation article par article avec le PLU & ZAN (Conforme / Non Conforme)
                - 💡 Préconisation de modification du projet pour obtenir la conformité.
                """)
        String evaluateProject(String userQuery);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(ready().bold().brightCyan()
                .append("\n================================================================================\n")
                .append("  🏗️ ACTE 1 (LangChain4j) : Copilote d'Urbanisme « Text-to-GIS » & ZAN (RAG+Tools)\n")
                .append("================================================================================\n")
                .reset());


        // 1. Indexation RAG du règlement PLU & ZAN en mémoire (Embeddings locaux ONNX)
        System.out.println(bold(blue("1. Indexation RAG locale du règlement PLU & ZAN (src/main/resources/plu-saint-mande-extrait.md)...")));
        String pluText = Files.readString(Path.of("src", "main", "resources", "plu-saint-mande-extrait.md"));
        Document pluDoc = Document.from(pluText);

        EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(600, 80))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(pluDoc);

        ContentRetriever pluRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(6)
                .build();
        System.out.println("  ✅ Règlement PLU & ZAN vectorisé en mémoire.\n");

        // 2. Configuration du modèle Gemini & de l'AiService LangChain4j avec GeoplateformeTools
        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-3.8-flash")
                .temperature(0.1)
                .returnThinking(true)
                .sendThinking(true)
                .build();

        UrbanismCopilot copilot = AiServices.builder(UrbanismCopilot.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .contentRetriever(pluRetriever)
                .tools(new GeoplateformeTools())
                // Auto-correction des arguments d'outils par le LLM & protection des exceptions internes :
                .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.sendExceptionMessageToLlm())
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocationUnlessVisibleToLlm())
                .build();

        // 3. Requête métier d'instruction de permis de construire
        String prompt = """
                Nous instruisons un permis de construire au 73 Avenue de Paris, 94160 Saint-Mandé :
                - Bâtiment tertiaire de 650 m²
                - Hauteur prévue au faîtage : 18 mètres (R+5)
                - Emprise pleine terre prévue : 35% de la parcelle.
                Vérifie la conformité complète de ce projet avec le PLU et le ZAN en interrogeant la Géoplateforme IGN.
                """;

        System.out.println(bold(blue("Requête Instructeur d'Urbanisme :\n")) + cyan(prompt));
        System.out.println(ready().faint().italic()
                .append("L'agent recherche dans le PLU (RAG) et chaîne les appels API Géoplateforme IGN...\n")
                .reset());

        String response = copilot.evaluateProject(prompt);

        System.out.println("\n" + bold(green("📋 Rapport d'Instruction du Copilote d'Urbanisme :")));
        System.out.println(new MarkdownRenderer().render(response));
    }
}
