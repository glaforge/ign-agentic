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

import io.github.glaforge.ansiren.MarkdownRenderer;
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.AgentResponse;
import io.github.glaforge.antigravity.McpServerConfig;
import io.github.glaforge.antigravity.hooks.HookResult;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.glaforge.ansiren.Ansi.blue;
import static io.github.glaforge.ansiren.Ansi.bold;
import static io.github.glaforge.ansiren.Ansi.cyan;
import static io.github.glaforge.ansiren.Ansi.green;
import static io.github.glaforge.ansiren.Ansi.magenta;
import static io.github.glaforge.ansiren.Ansi.ready;
import static io.github.glaforge.ansiren.Ansi.red;

/**
 * ACTE 4 (Serveur MCP Géoplateforme IGN) : Découverte Dynamique OGC via Model Context Protocol.
 *
 * <p>Démontre comment :
 * <ol>
 *   <li>La classe {@link fr.ign.workshop.tools.GeoplateformeTools} est exposée en tant que
 *       <b>Serveur MCP STDIO</b> via {@link fr.ign.workshop.mcp.IgnGeoplateformeMcpServer}
 *       ({@code langchain4j-community-mcp-server}).</li>
 *   <li>Un agent {@code antigravity-sdk-for-java} (ou Antigravity IDE via {@code .agents/plugins/ign-geoplateforme-plugin/mcp_config.json})
 *       se connecte à ce serveur MCP via {@link McpServerConfig#stdio(String, List)}.</li>
 *   <li>L'agent utilise les outils MCP d'introspection OGC ({@code search_wfs_layers},
 *       {@code describe_wfs_layer_schema}, {@code query_any_wfs_layer}) pour découvrir et interroger
 *       à la volée les parcelles cadastrales ou le réseau routier sans code spécifique !</li>
 * </ol>
 */
public class _04_McpGeoplateformeDemo {

    public static void main(String[] args) {
        System.out.println(ready().bold().brightCyan()
                .append("\n================================================================================\n")
                .append("  🔌 ACTE 4 (MCP STDIO) : Serveur MCP Géoplateforme & Découverte Dynamique OGC\n")
                .append("================================================================================\n")
                .reset());

        String launcherScript = Path.of("mcp", "run-ign-mcp-server.sh").toAbsolutePath().toString();
        String cartoLauncherScript = Path.of("mcp", "run-ign-mcp-carto.sh").toAbsolutePath().toString();
        System.out.println(bold(blue("1. Connexion au Serveur MCP Géoplateforme IGN (STDIO) : ")) + cyan(launcherScript));
        System.out.println(bold(blue("2. Connexion au Serveur MCP Carto IGN Fab (STDIO)     : ")) + cyan(cartoLauncherScript));

        McpServerConfig ignMcpServer = McpServerConfig.stdio(launcherScript, List.of())
                .withName("ign-geoplateforme")
                .withForceAllToolsEager(true);

        McpServerConfig ignCartoMcpServer = McpServerConfig.stdio(cartoLauncherScript, List.of())
                .withName("ign-mcp-carto")
                .withForceAllToolsEager(true);

        AgentConfig config = AgentConfig.builder()
                .instructions("""
                        Tu es un agent géomaticien connecté à deux serveurs MCP de l'IGN :
                        - `ign-geoplateforme` (requêtes REST & OGC WFS/WMS sur data.geopf.fr)
                        - `ign-mcp-carto` (`ignfab/mcp-carto`, affichage de carte interactive Plan IGN v2 / Orthophotos avec surcouches GeoJSON via `show_plan_ign_map`).

                        Exécute les étapes suivantes :
                        1. Cherche dans le catalogue WFS (`search_wfs_layers`) les couches liées au mot-clé `parcelle`.
                        2. Inspecte le schéma (`describe_wfs_layer_schema`) de la couche `CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle`.
                        3. Interroge cette couche (`query_any_wfs_layer`) dans un rayon de 80m autour du siège de l'IGN (lon=2.424573, lat=48.845726, limit=5) et note le champ `wfs_geojson_url` retourné.
                        4. Appelle `show_plan_ign_map` sur le serveur `ign-mcp-carto` centré sur (lon=2.424573, lat=48.845726, zoom=17, background="plan_ign_v2") en lui passant dans `layers` la couche GeoJSON des parcelles cadastrales (`data_url` = le `wfs_geojson_url` obtenu à l'étape 3, `color` = "#2563eb", `title` = "Parcelles Cadastrales Saint-Mandé").
                        5. Résume les parcelles cadastrales trouvées (section, numéro, contenance en m²) et confirme l'affichage de la carte MCP.
                        """)
                .addMcpServer(ignMcpServer)
                .addMcpServer(ignCartoMcpServer)
                .addPreToolCallDecideHook((toolCall, context) -> {
                    System.out.println(bold(magenta("  🔌 [Appel Outil MCP STDIO] ")) + cyan(toolCall.name()));
                    return CompletableFuture.completedFuture(HookResult.allowed());
                })
                .build();

        try (Agent agent = new Agent(config)) {
            String prompt = "Explore dynamiquement le serveur MCP Géoplateforme pour trouver la couche des parcelles cadastrales, inspecte son schéma, récupère les parcelles au 73 Avenue de Paris à Saint-Mandé, et affiche-les sur la carte via le serveur MCP Carto de l'IGN.";

            System.out.println("\n" + bold(blue("Requête Utilisateur :\n")) + cyan(prompt) + "\n");

            AgentResponse response = agent.chat(prompt).get(180, TimeUnit.SECONDS);

            System.out.println("\n" + bold(green("📋 Réponse de l'Agent via les Serveurs MCP Géoplateforme & MCP Carto :")));
            System.out.println(new MarkdownRenderer().render(response.text()));
        } catch (Exception e) {
            System.err.println(red("Erreur lors de l'exécution de la démo MCP : " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
