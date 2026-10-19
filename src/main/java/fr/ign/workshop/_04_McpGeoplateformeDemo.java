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
import io.github.glaforge.antigravity.CapabilitiesConfig;
import io.github.glaforge.antigravity.LiteRTAgentConfig;
import io.github.glaforge.antigravity.McpServerConfig;
import io.github.glaforge.antigravity.hooks.HookResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
import static io.github.glaforge.ansiren.Ansi.yellow;

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

    private static final int LITERT_PORT = 9379;

    public static void main(String[] args) {
        boolean useLiteRT = List.of(args).contains("--litert")
                || Boolean.parseBoolean(System.getProperty("litert", "false"));

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

        String instructions = """
                Tu es un agent géomaticien connecté à deux serveurs MCP de l'IGN :
                - `ign-geoplateforme` (requêtes REST & OGC WFS/WMS sur data.geopf.fr)
                - `ign-mcp-carto` (`ignfab/mcp-carto`, affichage de carte interactive Plan IGN v2 / Orthophotos avec surcouches GeoJSON via `show_plan_ign_map`).

                Exécute les étapes suivantes :
                1. Cherche dans le catalogue WFS (`search_wfs_layers`) les couches liées au mot-clé `parcelle`.
                2. Inspecte le schéma (`describe_wfs_layer_schema`) de la couche `CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle`.
                3. Interroge cette couche (`query_any_wfs_layer`) dans un rayon de 80m autour du siège de l'IGN (lon=2.424573, lat=48.845726, limit=5) et note le champ `wfs_geojson_url` retourné.
                4. Appelle `show_plan_ign_map` sur le serveur `ign-mcp-carto` centré sur (lon=2.424573, lat=48.845726, zoom=17, background="plan_ign_v2") en lui passant dans `layers` la couche GeoJSON des parcelles cadastrales (`data_url` = le `wfs_geojson_url` obtenu à l'étape 3, `color` = "#2563eb", `title` = "Parcelles Cadastrales Saint-Mandé").
                5. Résume les parcelles cadastrales trouvées (section, numéro, contenance en m²) et confirme l'affichage de la carte MCP.
                """;

        AgentConfig.Builder configBuilder = AgentConfig.builder()
                .instructions(instructions)
                .addMcpServer(ignMcpServer)
                .addMcpServer(ignCartoMcpServer)
                .addPreToolCallDecideHook((toolCall, context) -> {
                    System.out.println(bold(magenta("  🔌 [Appel Outil MCP STDIO] ")) + cyan(toolCall.name()));
                    return CompletableFuture.completedFuture(HookResult.allowed());
                });

        Process managedLiteRTProcess = null;
        try {
            if (useLiteRT) {
                String modelName = System.getProperty("litert.model", "gemma4-e2b");
                String modelPath = System.getProperty("litert.modelPath",
                        System.getProperty("user.home") + "/.litert-lm/models/" + modelName + "/model.litertlm");
                LiteRTAgentConfig litertConfig = LiteRTAgentConfig.builder()
                        .modelPath(modelPath)
                        .modelName(modelName)
                        .port(LITERT_PORT)
                        .backend(LiteRTAgentConfig.Backend.GPU)
                        .lightweight()
                        .build();
                System.out.println(bold(yellow("3. Modèle Local On-Device (LiteRT-LM)                 : "))
                        + cyan(litertConfig.getAgentConfig().getModelName()) + " via " + cyan(litertConfig.getBaseUrl()));

                managedLiteRTProcess = ensureLiteRTServerRunning(LITERT_PORT);

                configBuilder
                        .lightweight()
                        .capabilities(CapabilitiesConfig.builder()
                                .enableSubagents(false)
                                .allowUserQuestions(false)
                                .enableWebSearch(false)
                                .enableUrlReading(false)
                                .enableShell(false)
                                .enableViewFile(false)
                                .enableWriteFile(false)
                                .enableFileEdit(false)
                                .enableListDir(false)
                                .enableGrepSearch(false)
                                .build())
                        .modelName(litertConfig.getAgentConfig().getModelName())
                        .baseUrl(litertConfig.getBaseUrl());
            }

            AgentConfig config = configBuilder.build();

            try (Agent agent = new Agent(config)) {
                String prompt = "Explore dynamiquement le serveur MCP Géoplateforme pour trouver la couche des parcelles cadastrales, inspecte son schéma, récupère les parcelles au 73 Avenue de Paris à Saint-Mandé, et affiche-les sur la carte via le serveur MCP Carto de l'IGN.";

                System.out.println("\n" + bold(blue("Requête Utilisateur :\n")) + cyan(prompt) + "\n");

                AgentResponse response = agent.chat(prompt).get(180, TimeUnit.SECONDS);

                System.out.println("\n" + bold(green("📋 Réponse de l'Agent via les Serveurs MCP Géoplateforme & MCP Carto :")));
                System.out.println(new MarkdownRenderer().render(response.text()));
            }
        } catch (Exception e) {
            System.err.println(red("Erreur lors de l'exécution de la démo MCP : " + e.getMessage()));
            e.printStackTrace();
        } finally {
            stopLiteRTServer(managedLiteRTProcess);
        }
    }

    /**
     * Vérifie si {@code litert-lm serve} écoute déjà sur {@code port} ; sinon, garantit la
     * configuration {@code ~/.litert-lm/config.json} (GPU + fenêtre de contexte 32k) et démarre
     * automatiquement le serveur en sous-processus.
     */
    private static Process ensureLiteRTServerRunning(int port) throws IOException, InterruptedException {
        if (isLiteRTServerUp(port)) {
            System.out.println(bold(green("   ✔ Serveur LiteRT-LM déjà actif sur le port " + port)));
            return null;
        }

        ensureLiteRTConfig();
        String litertExecutable = resolveLiteRTExecutable();
        System.out.println(bold(yellow("   🚀 Démarrage automatique de `litert-lm serve --port " + port + "` ("))
                + cyan(litertExecutable) + bold(yellow(")...")));

        Process process = new ProcessBuilder(litertExecutable, "serve", "--port", String.valueOf(port))
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopLiteRTServer(process)));

        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                throw new IOException("Le processus `litert-lm serve` s'est arrêté prématurément (code " + process.exitValue() + ").");
            }
            if (isLiteRTServerUp(port)) {
                System.out.println(bold(green("   ✔ Serveur LiteRT-LM prêt sur http://127.0.0.1:" + port)));
                return process;
            }
            Thread.sleep(250);
        }
        stopLiteRTServer(process);
        throw new IOException("Délai d'attente dépassé (20s) lors du démarrage de `litert-lm serve` sur le port " + port);
    }

    private static boolean isLiteRTServerUp(int port) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(500))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/v1/models"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static void ensureLiteRTConfig() throws IOException {
        Path configFile = Path.of(System.getProperty("user.home"), ".litert-lm", "config.json");
        if (!Files.exists(configFile)) {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, """
                    {
                      "default": {
                        "backend": "gpu",
                        "max_num_tokens": 32768
                      }
                    }
                    """);
        }
    }

    private static String resolveLiteRTExecutable() {
        String customBin = System.getProperty("litert.bin");
        if (customBin != null && !customBin.isBlank()) {
            return customBin;
        }
        String home = System.getProperty("user.home");
        List<Path> candidates = List.of(
                Path.of(home, ".local", "bin", "litert-lm"),
                Path.of(home, ".litert-lm", "venv", "bin", "litert-lm"),
                Path.of("/opt/homebrew/bin/litert-lm"),
                Path.of("/usr/local/bin/litert-lm")
        );
        for (Path candidate : candidates) {
            if (Files.isExecutable(candidate)) {
                return candidate.toString();
            }
        }
        return "litert-lm";
    }

    private static void stopLiteRTServer(Process process) {
        if (process != null && process.isAlive()) {
            System.out.println(bold(yellow("\n   🛑 Arrêt du serveur LiteRT-LM démarré par la démo...")));
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
