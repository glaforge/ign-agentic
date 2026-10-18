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
package fr.ign.workshop.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.tools.FunctionTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tests JUnit 6 de GeoplateformeTools (Multi-SDK & APIs Géoplateforme IGN)")
class GeoplateformeToolsTest {

    private static final Set<String> EXPECTED_TOOL_NAMES = Set.of(
            "geocode_address",
            "get_elevation_and_slope",
            "compute_isochrone",
            "query_bdtopo_buildings",
            "query_bdtopo_environment",
            "download_bdortho_aerial_image",
            "submit_bdtopo_collaborative_report",
            "search_wfs_layers",
            "describe_wfs_layer_schema",
            "query_any_wfs_layer"
    );

    // Coordonnées de référence : Siège de l'IGN / Géoroom (73 Avenue de Paris, 94160 Saint-Mandé)
    private static final double IGN_LON = 2.4237;
    private static final double IGN_LAT = 48.8447;

    private GeoplateformeTools tools;

    @BeforeEach
    void setUp() {
        tools = new GeoplateformeTools();
    }

    @Nested
    @DisplayName("1. Interopérabilité Multi-SDK & MCP (LangChain4j, ADK Java, Antigravity SDK, MCP Server)")
    class MultiSdkInteroperabilityTests {

        @Test
        @DisplayName("LangChain4j détecte les 10 méthodes annotées @Tool")
        void shouldExtractAllLangChain4jToolSpecifications() {
            List<ToolSpecification> specs = ToolSpecifications.toolSpecificationsFrom(tools);

            assertEquals(10, specs.size(), "10 outils doivent être extraits par LangChain4j");
            Set<String> names = specs.stream()
                    .map(ToolSpecification::name)
                    .collect(Collectors.toSet());
            assertEquals(EXPECTED_TOOL_NAMES, names);

            for (ToolSpecification spec : specs) {
                assertNotNull(spec.description(), "La description de " + spec.name() + " ne doit pas être nulle");
                assertFalse(spec.description().isBlank(), "La description de " + spec.name() + " ne doit pas être vide");
            }
        }

        @ParameterizedTest(name = "ADK FunctionTool : méthode Java ''{0}'' -> outil ''{1}''")
        @CsvSource({
                "geocodeAddress, geocode_address",
                "getElevationAndSlope, get_elevation_and_slope",
                "computeIsochrone, compute_isochrone",
                "queryBdTopoBuildings, query_bdtopo_buildings",
                "queryBdTopoEnvironment, query_bdtopo_environment",
                "downloadBdOrthoAerialImage, download_bdortho_aerial_image",
                "submitBdTopoCollaborativeReport, submit_bdtopo_collaborative_report",
                "searchWfsLayers, search_wfs_layers",
                "describeWfsLayerSchema, describe_wfs_layer_schema",
                "queryAnyWfsLayer, query_any_wfs_layer"
        })
        @DisplayName("Google ADK Java crée un FunctionTool valide pour chaque méthode annotée @Schema")
        void shouldCreateAdkFunctionTools(String methodName, String expectedToolName) {
            FunctionTool functionTool = FunctionTool.create(tools, methodName);

            assertNotNull(functionTool);
            assertEquals(expectedToolName, functionTool.name());
            assertFalse(functionTool.description().isBlank());
            assertTrue(functionTool.declaration().isPresent(), "La déclaration de schéma Gemini doit être présente");
        }

        @Test
        @DisplayName("Antigravity SDK enregistre les 10 outils par réflexion Tier-3 (@Tool / @P)")
        void shouldRegisterAllToolsInAntigravityToolRegistry() {
            ToolRegistry registry = new ToolRegistry();
            registry.registerToolsFromObject(tools);

            List<ToolDefinition> definitions = registry.getToolDefinitions();
            assertEquals(10, definitions.size(), "Antigravity ToolRegistry doit enregistrer les 10 outils");

            Set<String> registeredNames = definitions.stream()
                    .map(ToolDefinition::getName)
                    .collect(Collectors.toSet());
            assertEquals(EXPECTED_TOOL_NAMES, registeredNames);
        }

        @Test
        @DisplayName("Serveur MCP (McpServer) expose les 10 outils en JSON-RPC 2.0")
        void shouldExposeAllToolsViaMcpServer() throws Exception {
            dev.langchain4j.community.mcp.server.McpServer mcpServer =
                    new dev.langchain4j.community.mcp.server.McpServer(List.of(tools));
            ObjectMapper mapper = new ObjectMapper();

            JsonNode listReq = mapper.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
            Object listResp = mcpServer.handle(listReq);
            JsonNode listJson = mapper.valueToTree(listResp);

            JsonNode mcpTools = listJson.path("result").path("tools");
            assertEquals(10, mcpTools.size(), "Le serveur MCP doit exposer les 10 outils dans tools/list");
        }
    }

    @Nested
    @DisplayName("2. Espace Collaboratif IGN (Test unitaire hors-ligne)")
    class CollaborativeReportUnitTests {

        @Test
        @DisplayName("submitBdTopoCollaborativeReport génère un ticket et sauvegarde collaborative-ticket.json")
        void shouldSubmitCollaborativeReportAndWriteJsonTicket() throws Exception {
            Map<String, Object> report = tools.submitBdTopoCollaborativeReport(
                    "BATIMENT0000000334291975",
                    "COHERENCE_VALIDEE",
                    "Concordance vérifiée entre l'emprise BD TOPO et la toiture végétalisée BD ORTHO."
            );

            assertEquals("SUBMITTED_TO_IGN_COLLABORATIVE_SPACE", report.get("status"));
            String ticketId = (String) report.get("ticket_id");
            assertNotNull(ticketId);
            assertTrue(ticketId.startsWith("IGN-COLLAB-"), "Le ticket doit commencer par IGN-COLLAB-");
            assertEquals("BATIMENT0000000334291975", report.get("target_object"));
            assertEquals("COHERENCE_VALIDEE", report.get("anomaly_type"));

            Path ticketFile = Path.of("target", "map-data", "collaborative-ticket.json");
            assertTrue(Files.exists(ticketFile), "Le fichier collaborative-ticket.json doit exister");

            JsonNode savedJson = new ObjectMapper().readTree(ticketFile.toFile());
            assertEquals(ticketId, savedJson.path("ticket_id").asText());
            assertEquals("BATIMENT0000000334291975", savedJson.path("target_object").asText());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("3. Appels d'intégration aux APIs ouvertes de la Géoplateforme IGN (data.geopf.fr)")
    class LiveGeoplateformeApiTests {

        @Test
        @Order(1)
        @DisplayName("geocodeAddress : géocode le 73 Avenue de Paris, 94160 Saint-Mandé en WGS84 et Lambert-93")
        void shouldGeocodeIgnHeadquarters() {
            Map<String, Object> result = tools.geocodeAddress("73 Avenue de Paris, 94160 Saint-Mandé");

            assertEquals("OK", result.get("status"));
            assertEquals("94160", result.get("postcode"));
            assertEquals("94067", result.get("citycode_insee"));
            assertTrue(result.get("label").toString().toLowerCase().contains("saint-mandé"));

            double lon = (double) result.get("lon");
            double lat = (double) result.get("lat");
            assertEquals(IGN_LON, lon, 0.01, "Longitude WGS84 attendue autour de 2.4237");
            assertEquals(IGN_LAT, lat, 0.01, "Latitude WGS84 attendue autour de 48.8447");

            double lambertX = (double) result.get("lambert93_x");
            double lambertY = (double) result.get("lambert93_y");
            assertTrue(lambertX > 650_000 && lambertX < 665_000, "X Lambert-93 valide en Île-de-France");
            assertTrue(lambertY > 6_850_000 && lambertY < 6_870_000, "Y Lambert-93 valide en Île-de-France");
        }

        @Test
        @Order(2)
        @DisplayName("geocodeAddress : retourne NOT_FOUND pour une adresse introuvable")
        void shouldReturnNotFoundForNonExistentAddress() {
            Map<String, Object> result = tools.geocodeAddress("zzzzzzzzzzzzzzzzzzzzzzzz999999999");

            assertEquals("NOT_FOUND", result.get("status"));
        }

        @Test
        @Order(3)
        @DisplayName("getElevationAndSlope : calcule l'altitude RGE ALTI et la pente locale sur 5 points")
        void shouldComputeElevationAndSlopeFromRgeAlti() {
            Map<String, Object> result = tools.getElevationAndSlope(IGN_LON, IGN_LAT);

            assertEquals("OK", result.get("status"));
            assertEquals("IGN RGE ALTI (ign_rge_alti_wld)", result.get("source"));

            double altitude = (double) result.get("altitude_center_m");
            assertTrue(altitude > 35.0 && altitude < 75.0,
                    "L'altitude à Saint-Mandé doit être comprise entre 35m et 75m NGF (reçu: " + altitude + ")");

            double slope = (double) result.get("slope_percent");
            assertTrue(slope >= 0.0 && slope < 30.0, "La pente en % doit être positive et réaliste");
            assertNotNull(result.get("slope_category"));
            assertNotNull(result.get("orientation_ecoulement"));
        }

        @Test
        @Order(4)
        @DisplayName("queryBdTopoBuildings : interroge BDTOPO_V3:batiment en WFS 2.0 (CRS:84) et sauvegarde buildings.geojson")
        void shouldQueryBdTopoBuildingsAndSaveGeoJson() throws Exception {
            Map<String, Object> result = tools.queryBdTopoBuildings(IGN_LON, IGN_LAT, 120);

            assertEquals("OK", result.get("status"));
            assertEquals("BDTOPO_V3:batiment", result.get("layer"));
            assertEquals("CRS:84", result.get("crs"));

            int count = (int) result.get("buildings_count");
            assertTrue(count > 15, "Il doit y avoir plus de 15 bâtiments dans un rayon de 120m (seuil ZAN)");

            double maxHeight = (double) result.get("max_height_meters");
            assertTrue(maxHeight > 10.0, "La hauteur maximale des bâtiments doit dépasser 10m");

            List<?> samples = (List<?>) result.get("sample_buildings");
            assertFalse(samples.isEmpty(), "L'échantillon de bâtiments ne doit pas être vide");

            Path savedGeoJson = Path.of(result.get("saved_geojson_path").toString());
            assertTrue(Files.exists(savedGeoJson), "buildings.geojson doit être sauvegardé sur disque");
            assertTrue(Files.size(savedGeoJson) > 500, "buildings.geojson ne doit pas être vide");
        }

        @Test
        @Order(5)
        @DisplayName("queryBdTopoEnvironment : détecte la lisière du Bois de Vincennes (BDTOPO_V3:zone_de_vegetation)")
        void shouldQueryBdTopoEnvironmentAndDetectForestNearby() {
            Map<String, Object> result = tools.queryBdTopoEnvironment(IGN_LON, IGN_LAT, 250);

            assertEquals("OK", result.get("status"));
            int vegCount = (int) result.get("vegetation_zones_count");
            assertTrue(vegCount > 0, "Des zones de végétation (Bois de Vincennes) doivent être détectées à moins de 250m");
            assertEquals(Boolean.TRUE, result.get("has_forest_or_wood_nearby"));

            Path vegFile = Path.of("target", "map-data", "vegetation.geojson");
            assertTrue(Files.exists(vegFile), "vegetation.geojson doit être sauvegardé");
        }

        @Test
        @Order(6)
        @DisplayName("computeIsochrone : calcule une isochrone piéton de 5 min via bdtopo-valhalla")
        void shouldComputePedestrianIsochrone() {
            Map<String, Object> result = tools.computeIsochrone(IGN_LON, IGN_LAT, 300, "pedestrian");

            assertEquals("OK", result.get("status"));
            assertEquals("pedestrian", result.get("profile"));
            assertEquals(300, result.get("duration_seconds"));
            assertEquals(5.0, (double) result.get("duration_minutes"), 0.01);

            int vertices = (int) result.get("polygon_vertices_count");
            assertTrue(vertices > 3, "Le polygone isochrone doit comporter plus de 3 sommets");

            double ewKm = (double) result.get("envelope_ew_km");
            double nsKm = (double) result.get("envelope_ns_km");
            assertTrue(ewKm > 0.1 && ewKm < 5.0, "Enveloppe Est-Ouest cohérente pour 5 min à pied");
            assertTrue(nsKm > 0.1 && nsKm < 5.0, "Enveloppe Nord-Sud cohérente pour 5 min à pied");

            Path isoFile = Path.of(result.get("saved_geojson_path").toString());
            assertTrue(Files.exists(isoFile), "isochrone.geojson doit exister");
        }

        @Test
        @Order(7)
        @DisplayName("downloadBdOrthoAerialImage : télécharge une tuile WMS BD ORTHO 512x512 valide au format PNG")
        void shouldDownloadBdOrthoPngTile() throws Exception {
            Map<String, Object> result = tools.downloadBdOrthoAerialImage(IGN_LON, IGN_LAT, 100);

            assertEquals("OK", result.get("status"));
            assertEquals("512x512 px", result.get("dimensions"));

            int sizeBytes = (int) result.get("image_size_bytes");
            assertTrue(sizeBytes > 10_000, "La tuile PNG 512x512 doit peser plus de 10 Ko");

            Path imagePath = Path.of(result.get("image_path").toString());
            assertTrue(Files.exists(imagePath), "Le fichier bd_ortho_tile.png doit exister");

            // Vérification de la signature binaire PNG (\x89PNG\r\n\x1a\n)
            byte[] header = Files.readAllBytes(imagePath);
            assertEquals((byte) 0x89, header[0]);
            assertEquals((byte) 'P', header[1]);
            assertEquals((byte) 'N', header[2]);
            assertEquals((byte) 'G', header[3]);
        }
    }
}
