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
package fr.ign.workshop.mcp;

import dev.langchain4j.community.mcp.server.McpServer;
import dev.langchain4j.community.mcp.server.transport.StdioMcpServerTransport;
import fr.ign.workshop.tools.GeoplateformeTools;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Serveur MCP (Model Context Protocol) STDIO exposant l'intégralité des outils
 * de la Géoplateforme IGN ({@link GeoplateformeTools}) en 20 lignes de Java
 * grâce à {@code langchain4j-community-mcp-server}.
 *
 * <p>Outils exposés sur le protocole MCP :
 * <ul>
 *   <li>{@code geocode_address}</li>
 *   <li>{@code get_elevation_and_slope} (RGE ALTI)</li>
 *   <li>{@code compute_isochrone} (Navigation Valhalla)</li>
 *   <li>{@code query_bdtopo_buildings} (WFS BDTOPO_V3:batiment)</li>
 *   <li>{@code query_bdtopo_environment} (WFS végétation &amp; cours d'eau)</li>
 *   <li>{@code download_bdortho_aerial_image} (WMS BD ORTHO 20cm)</li>
 *   <li>{@code submit_bdtopo_collaborative_report} (Espace Collaboratif IGN)</li>
 *   <li>{@code search_wfs_layers} (Découverte dynamique GetCapabilities)</li>
 *   <li>{@code describe_wfs_layer_schema} (Introspection dynamique DescribeFeatureType)</li>
 *   <li>{@code query_any_wfs_layer} (Interrogation générique WFS 2.0)</li>
 * </ul>
 */
public class IgnGeoplateformeMcpServer {

    static {
        // Indispensable en mode MCP STDIO : tous les logs doivent aller sur System.err
        // afin que System.out reste exclusivement réservé aux messages JSON-RPC 2.0.
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
        System.setProperty("ign.mcp.stdio", "true");
    }

    public static void main(String[] args) throws Exception {
        System.err.println("[IGN-MCP] Démarrage du Serveur MCP Géoplateforme IGN sur STDIO...");

        GeoplateformeTools tools = new GeoplateformeTools();
        McpServer server = new McpServer(List.of(tools));
        new StdioMcpServerTransport(server);

        System.err.println("[IGN-MCP] Serveur MCP prêt (10 outils Géoplateforme enregistrés).");
        new CountDownLatch(1).await();
    }
}
