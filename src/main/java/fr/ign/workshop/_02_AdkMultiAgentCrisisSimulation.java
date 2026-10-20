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

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.adk.web.AdkWebServer;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import fr.ign.workshop.tools.GeoplateformeTools;
import io.github.glaforge.ansiren.MarkdownRenderer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Arrays;

import static io.github.glaforge.ansiren.Ansi.blue;
import static io.github.glaforge.ansiren.Ansi.bold;
import static io.github.glaforge.ansiren.Ansi.cyan;
import static io.github.glaforge.ansiren.Ansi.green;
import static io.github.glaforge.ansiren.Ansi.magenta;
import static io.github.glaforge.ansiren.Ansi.ready;

/**
 * ACTE 2 (Google ADK Java) : Cellule de Crise Multi-Agents sur le Jumeau Numérique IGN.
 *
 * <p>Architecture d'orchestration hybride combinant {@link SequentialAgent} et {@link ParallelAgent} :
 * <pre>
 * SequentialAgent ("cellule-crise-ign")
 *  ├── 1. LlmAgent ("agent-localisation")         -> geocode_address (API Géocodage IGN)
 *  ├── 2. ParallelAgent ("collecte-territoriale") -> Exécution concurrente sur la Géoplateforme :
 *  │       ├── LlmAgent ("agent-topographe")      -> get_elevation_and_slope (RGE ALTI)
 *  │       ├── LlmAgent ("agent-enjeux-bati")     -> query_bdtopo_buildings + query_bdtopo_environment (BD TOPO WFS)
 *  │       └── LlmAgent ("agent-logistique")      -> compute_isochrone (Navigation IGN bdtopo-valhalla)
 *  └── 3. LlmAgent ("coordinateur-secours")       -> Synthèse opérationnelle et plan d'évacuation
 * </pre>
 *
 * <p>Peut être lancé en mode console ou avec l'interface graphique ADK Dev UI ({@code --ui}).
 */
public class _02_AdkMultiAgentCrisisSimulation {

    private static final String MODEL_NAME = "gemini-3.8-flash";

    public static BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        GeoplateformeTools geoTools = new GeoplateformeTools();

        // Étape 1 : Agent de géolocalisation précise de l'alerte
        LlmAgent agentLocalisation = LlmAgent.builder()
                .name("agent-localisation")
                .description("Géolocalise le point de départ de crise (incendie ou inondation) via la Géoplateforme IGN")
                .model(MODEL_NAME)
                .instruction("""
                        Tu es l'opérateur de localisation d'urgence de la Sécurité Civile et de l'IGN.
                        Utilise OBLIGATOIREMENT l'outil `geocode_address` pour géocoder le lieu d'alerte mentionné par l'utilisateur.
                        Retourne un résumé concis indiquant l'adresse normalisée, la longitude (lon), la latitude (lat) en WGS84,
                        les coordonnées Lambert-93 (x, y) et le code INSEE de la commune.
                        """)
                .tools(FunctionTool.create(geoTools, "geocodeAddress"))
                .outputKey("localisation_sinistre")
                .build();

        // Étape 2A (Parallèle) : Agent Topographe (RGE ALTI)
        LlmAgent agentTopographe = LlmAgent.builder()
                .name("agent-topographe")
                .description("Analyse le relief RGE ALTI, la pente et le sens de propagation (feu) ou d'écoulement (crue)")
                .model(MODEL_NAME)
                .instruction("""
                        Tu es l'Agent Topographe expert RGE ALTI de l'IGN.
                        À partir des coordonnées identifiées dans : {localisation_sinistre}
                        Appelle OBLIGATOIREMENT l'outil `get_elevation_and_slope` avec la longitude (lon) et la latitude (lat).
                        Déduis-en :
                        1. L'altitude exacte NGF et le pourcentage de pente.
                        2. En cas d'incendie : la direction de propagation privilégiée (le feu monte les pentes).
                        3. En cas de crue/ruissellement : la direction d'écoulement gravitaire (l'eau descend les pentes).
                        """)
                .tools(FunctionTool.create(geoTools, "getElevationAndSlope"))
                .outputKey("rapport_topographie")
                .build();

        // Étape 2B (Parallèle) : Agent Enjeux Bâti, Forêt & Hydrographie (BD TOPO WFS)
        LlmAgent agentEnjeuxBati = LlmAgent.builder()
                .name("agent-enjeux-bati")
                .description("Recense les bâtiments, logements, forêts et cours d'eau menacés dans la BD TOPO V3")
                .model(MODEL_NAME)
                .instruction("""
                        Tu es l'Agent d'Analyse des Enjeux Territoriaux (BD TOPO IGN).
                        À partir des coordonnées identifiées dans : {localisation_sinistre}
                        1. Appelle `query_bdtopo_buildings` dans un rayon de 150 mètres pour recenser les bâtiments, logements et usages.
                        2. Appelle `query_bdtopo_environment` dans un rayon de 300 mètres pour identifier les massifs boisés (combustible végétal) et les cours d'eau.
                        Synthétise la vulnérabilité humaine (nombre de bâtiments/logements) et environnementale.
                        """)
                .tools(
                        FunctionTool.create(geoTools, "queryBdTopoBuildings"),
                        FunctionTool.create(geoTools, "queryBdTopoEnvironment")
                )
                .outputKey("rapport_enjeux")
                .build();

        // Étape 2C (Parallèle) : Agent Logistique & Secours (Isochrone IGN)
        LlmAgent agentLogistique = LlmAgent.builder()
                .name("agent-logistique")
                .description("Calcule les périmètres d'accessibilité des engins de secours via l'API Isochrone IGN")
                .model(MODEL_NAME)
                .instruction("""
                        Tu es l'Agent Logistique & Mobilité Opérationnelle de l'IGN.
                        À partir des coordonnées identifiées dans : {localisation_sinistre}
                        Appelle OBLIGATOIREMENT `compute_isochrone` avec durationSeconds=300 (5 minutes) et profile="car"
                        pour évaluer l'enveloppe d'intervention rapide des véhicules de secours et d'évacuation.
                        """)
                .tools(FunctionTool.create(geoTools, "computeIsochrone"))
                .outputKey("rapport_logistique")
                .build();

        // Regroupement Parallèle des 3 agents spécialisés
        ParallelAgent collecteTerritoriale = ParallelAgent.builder()
                .name("collecte-territoriale-parallele")
                .description("Exécute en parallèle l'analyse RGE ALTI, l'inventaire BD TOPO et le calcul d'isochrone IGN")
                .subAgents(agentTopographe, agentEnjeuxBati, agentLogistique)
                .build();

        // Étape 3 : Agent Coordinateur de Cellule de Crise (Synthèse)
        LlmAgent agentCoordinateur = LlmAgent.builder()
                .name("coordinateur-secours")
                .description("Fusionne les analyses topographiques, BD TOPO et isochrones en un plan d'action opérationnel")
                .model(MODEL_NAME)
                .instruction("""
                        Tu es le Directeur des Opérations de Secours (DOS) utilisant le Jumeau Numérique de l'IGN.
                        Synthétise les rapports de tes 4 unités spécialisées :
                        - Localisation : {localisation_sinistre}
                        - Topographie RGE ALTI : {rapport_topographie}
                        - Enjeux BD TOPO (Bâti, Forêt, Eau) : {rapport_enjeux}
                        - Accessibilité & Isochrone 5 min : {rapport_logistique}

                        Produis un Plan d'Intervention Opérationnel structuré en Markdown :
                        1. 🚨 **Point de situation & Coordonnées (WGS84 / Lambert-93)**
                        2. ⛰️ **Dynamique spatiale (RGE ALTI)** : Direction de propagation du feu / écoulement des eaux selon la pente.
                        3. 🏘️ **Enjeux menacés (BD TOPO)** : Nombre de bâtiments, logements, étages max, présence de forêt/bois et cours d'eau.
                        4. 🚒 **Plan d'accès & Évacuation (Isochrone IGN)** : Périmètre couvert en 5 minutes et priorités de mise en sécurité.
                        """)
                .outputKey("plan_intervention_final")
                .build();

        // Pipeline Séquentiel Global
        return SequentialAgent.builder()
                .name("cellule-crise-ign")
                .description("Système Multi-Agents IGN de simulation de crise environnementale (RGE ALTI + BD TOPO + Isochrones)")
                .subAgents(agentLocalisation, collecteTerritoriale, agentCoordinateur)
                .build();
    }

    public static void main(String[] args) {
        System.out.println(ready().bold().brightCyan()
                .append("\n================================================================================\n")
                .append("  🚒 ACTE 2 (ADK Java) : Simulation Multi-Agents de Crise sur la Géoplateforme\n")
                .append("================================================================================\n")
                .reset());

        if (Arrays.asList(args).contains("--ui")) {
            System.out.println(bold(green("🌐 Démarrage de l'interface graphique ADK Dev Web UI sur http://localhost:8080/dev-ui/ ...")));
            AdkWebServer.start(ROOT_AGENT);
            return;
        }

        RunConfig runConfig = RunConfig.builder().build();
        InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);

        try {
            Session session = runner.sessionService()
                    .createSession(runner.appName(), "operateur-ign")
                    .blockingGet();

            String alertPrompt = "ALERTE : Départ de feu de végétation et risque d'orage violent signalés à la lisière de la Forêt de Fontainebleau, Boulevard Magenta, 77300 Fontainebleau. Établissez le diagnostic multi-agents et le plan d'intervention.";

            System.out.println(bold(blue("Signalement Cellule de Crise :\n")) + cyan(alertPrompt) + "\n");

            Content userMsg = Content.fromParts(Part.fromText(alertPrompt));
            Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);

            MarkdownRenderer renderer = new MarkdownRenderer();
            events.blockingForEach(event -> {
                if (event.finalResponse() && event.content().isPresent()) {
                    String text = event.stringifyContent();
                    if (text != null && !text.isBlank()) {
                        System.out.println("\n" + bold(magenta("🤖 [" + event.author() + "]")) + " :");
                        System.out.println(renderer.render(text));
                    }
                }
            });
        } finally {
            // Fermeture propre du Runner ADK et arrêt des pools de threads RxJava (ParallelAgent)
            runner.close().blockingAwait();
            Schedulers.shutdown();
        }
    }
}
