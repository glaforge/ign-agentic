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

import com.google.adk.tools.FunctionTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import fr.ign.workshop.tools.GeoplateformeTools;
import io.github.glaforge.antigravity.tools.ToolRegistry;

import java.util.List;
import java.util.Map;

import static io.github.glaforge.ansiren.Ansi.blue;
import static io.github.glaforge.ansiren.Ansi.bold;
import static io.github.glaforge.ansiren.Ansi.cyan;
import static io.github.glaforge.ansiren.Ansi.green;
import static io.github.glaforge.ansiren.Ansi.ready;

/**
 * Pré-lude du Workshop : Vérifie en direct les appels aux APIs de la Géoplateforme IGN (data.geopf.fr)
 * et démontre que la classe unique {@link GeoplateformeTools} est compatible nativement avec
 * LangChain4j, Google ADK Java et Antigravity SDK for Java.
 */
public class _00_TestGeoplateformeTools {

    public static void main(String[] args) {
        System.out.println(ready().bold().brightCyan()
                .append("\n================================================================================\n")
                .append("  🌍 PRÉLUDE : Test des Outils Géoplateforme IGN & Interopérabilité Multi-SDK\n")
                .append("================================================================================\n")
                .reset());

        GeoplateformeTools tools = new GeoplateformeTools();

        // 1. Vérification de l'interopérabilité des annotations sur les 3 SDKs Java
        System.out.println(bold(blue("1. Vérification de la portabilité de GeoplateformeTools.java :")));

        List<ToolSpecification> lc4jSpecs = ToolSpecifications.toolSpecificationsFrom(tools);
        System.out.println("  • " + bold(green("LangChain4j")) + " : " + lc4jSpecs.size()
                + " @Tool détectés -> " + lc4jSpecs.stream().map(ToolSpecification::name).toList());

        FunctionTool adkGeocode = FunctionTool.create(tools, "geocodeAddress");
        FunctionTool adkAlti = FunctionTool.create(tools, "getElevationAndSlope");
        FunctionTool adkTopo = FunctionTool.create(tools, "queryBdTopoBuildings");
        System.out.println("  • " + bold(green("ADK Java   ")) + " : FunctionTools créés -> ["
                + adkGeocode.name() + ", " + adkAlti.name() + ", " + adkTopo.name() + ", ...]");

        ToolRegistry agyRegistry = new ToolRegistry();
        agyRegistry.registerToolsFromObject(tools);
        System.out.println("  • " + bold(green("Antigravity")) + " : "
                + agyRegistry.getToolDefinitions().size() + " outils enregistrés (Tier-3 Reflection) -> "
                + agyRegistry.getToolDefinitions().stream().map(d -> d.getName()).toList());

        // 2. Test en direct sur Clermont-Ferrand (Place de Jaude) — déclenche un flyTo depuis Saint-Mandé dans le Cockpit 3D
        String targetAddress = (args.length > 0 && !String.join(" ", args).isBlank())
                ? String.join(" ", args).trim()
                : "Place de Jaude, 63000 Clermont-Ferrand";
        System.out.println("\n" + bold(blue("2. Appels temps réel aux APIs ouvertes de la Géoplateforme (data.geopf.fr) :")));

        Map<String, Object> geo = tools.geocodeAddress(targetAddress);
        System.out.println("    -> Géocodage : " + cyan(geo.get("label").toString())
                + " | WGS84=(" + geo.get("lon") + ", " + geo.get("lat") + ")"
                + " | Lambert-93=(" + geo.get("lambert93_x") + ", " + geo.get("lambert93_y") + ")");

        double lon = (double) geo.get("lon");
        double lat = (double) geo.get("lat");

        Map<String, Object> alti = tools.getElevationAndSlope(lon, lat);
        System.out.println("    -> RGE ALTI  : Altitude = " + cyan(alti.get("altitude_center_m") + " m NGF")
                + " | Pente = " + cyan(alti.get("slope_percent") + "% (" + alti.get("slope_category") + ")")
                + " | " + alti.get("orientation_ecoulement"));

        Map<String, Object> bati = tools.queryBdTopoBuildings(lon, lat, 120);
        System.out.println("    -> BD TOPO Bâti (120m) : " + cyan(bati.get("buildings_count") + " bâtiments")
                + " | Hauteur max = " + bati.get("max_height_meters") + "m (" + bati.get("max_floors") + " étages)"
                + " | Usages = " + bati.get("usages_breakdown"));

        Map<String, Object> env = tools.queryBdTopoEnvironment(lon, lat, 250);
        System.out.println("    -> BD TOPO Environnement : " + cyan(env.get("vegetation_zones_count") + " zones de végétation")
                + " | " + cyan(env.get("watercourses_within_500m_count") + " cours d'eau à proximité"));

        Map<String, Object> iso = tools.computeIsochrone(lon, lat, 300, "pedestrian");
        System.out.println("    -> Isochrone 5 min piéton : Enveloppe "
                + cyan(iso.get("envelope_ew_km") + " km x " + iso.get("envelope_ns_km") + " km")
                + " (" + iso.get("polygon_vertices_count") + " sommets)");

        Map<String, Object> ortho = tools.downloadBdOrthoAerialImage(lon, lat, 100);
        System.out.println("    -> BD ORTHO WMS (512x512 PNG) : " + cyan(ortho.get("image_size_bytes") + " octets")
                + " sauvegardés dans " + ortho.get("image_path"));

        System.out.println(ready().bold().brightGreen()
                .append("\n✅ Toutes les APIs de la Géoplateforme IGN répondent et les données GeoJSON/PNG sont prêtes dans target/map-data/ !\n")
                .reset());
    }
}
