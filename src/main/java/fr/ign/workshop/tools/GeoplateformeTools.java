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
import com.google.adk.tools.Annotations.Schema;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.glaforge.ansiren.Ansi.bold;
import static io.github.glaforge.ansiren.Ansi.cyan;
import static io.github.glaforge.ansiren.Ansi.yellow;

/**
 * Boîte à outils Java unifiée pour interroger les APIs ouvertes de la Géoplateforme IGN (data.geopf.fr).
 *
 * <p>Cette classe unique est annotée pour fonctionner <b>sans modification</b> avec :
 * <ul>
 *   <li><b>LangChain4j</b> ({@code @Tool}, {@code @P}) — Acte 1</li>
 *   <li><b>Google ADK Java</b> ({@code @Schema}, {@code FunctionTool}) — Acte 2</li>
 *   <li><b>Antigravity SDK for Java</b> (support natif Tier-3 des annotations {@code @Tool}/{@code @P}) — Acte 3</li>
 * </ul>
 */
public class GeoplateformeTools {

    private static final String BASE_URL = "https://data.geopf.fr";
    private static final Path MAP_DATA_DIR = Path.of("target", "map-data");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =========================================================================
    // 1. GEOCODING (Adresse / Lieu -> Coordonnées WGS84 & Lambert-93)
    // =========================================================================

    @Tool(
        name = "geocode_address",
        value = "Géocode une adresse postale, une commune ou un lieu en France via l'API de géocodage de la Géoplateforme IGN. Retourne la longitude (lon), latitude (lat) en WGS84 (CRS:84), les coordonnées Lambert-93 (x, y) et le code INSEE."
    )
    @Schema(
        name = "geocode_address",
        description = "Géocode une adresse postale, une commune ou un lieu en France via l'API de géocodage de la Géoplateforme IGN. Retourne la longitude (lon), latitude (lat) en WGS84 (CRS:84), les coordonnées Lambert-93 (x, y) et le code INSEE."
    )
    public Map<String, Object> geocodeAddress(
            @P("Adresse postale ou nom de lieu en France (ex: '73 Avenue de Paris, 94160 Saint-Mandé')")
            @Schema(name = "query", description = "Adresse postale ou nom de lieu en France (ex: '73 Avenue de Paris, 94160 Saint-Mandé')")
            String query
    ) {
        logToolCall("geocode_address", "query=\"" + query + "\"");
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = BASE_URL + "/geocodage/search?q=" + encoded + "&limit=1";
            JsonNode root = getJson(url);
            JsonNode features = root.path("features");

            if (features.isEmpty()) {
                return Map.of("status", "NOT_FOUND", "query", query);
            }

            JsonNode first = features.get(0);
            JsonNode coords = first.path("geometry").path("coordinates");
            JsonNode props = first.path("properties");

            double lon = coords.get(0).asDouble();
            double lat = coords.get(1).asDouble();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("label", props.path("label").asText());
            result.put("lon", lon);
            result.put("lat", lat);
            result.put("crs", "CRS:84 (WGS84 lon,lat)");
            result.put("lambert93_x", props.path("x").asDouble());
            result.put("lambert93_y", props.path("y").asDouble());
            result.put("city", props.path("city").asText());
            result.put("postcode", props.path("postcode").asText());
            result.put("citycode_insee", props.path("citycode").asText());
            result.put("score", props.path("score").asDouble());
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // 2. ALTIMETRY & SLOPE (RGE ALTI -> Altitude & Pente locale en %)
    // =========================================================================

    @Tool(
        name = "get_elevation_and_slope",
        value = "Interroge le Modèle Numérique de Terrain RGE ALTI de l'IGN sur 5 points (centre, Nord, Sud, Est, Ouest à 50m) pour calculer l'altitude exacte (en mètres NGF), la pente locale du terrain (en %) et l'orientation de la pente."
    )
    @Schema(
        name = "get_elevation_and_slope",
        description = "Interroge le Modèle Numérique de Terrain RGE ALTI de l'IGN sur 5 points (centre, Nord, Sud, Est, Ouest à 50m) pour calculer l'altitude exacte (en mètres NGF), la pente locale du terrain (en %) et l'orientation de la pente."
    )
    public Map<String, Object> getElevationAndSlope(
            @P("Longitude WGS84 (ex: 2.4237)")
            @Schema(name = "lon", description = "Longitude WGS84 (ex: 2.4237)")
            double lon,
            @P("Latitude WGS84 (ex: 48.8447)")
            @Schema(name = "lat", description = "Latitude WGS84 (ex: 48.8447)")
            double lat
    ) {
        logToolCall("get_elevation_and_slope", String.format(Locale.US, "lon=%.5f, lat=%.5f", lon, lat));
        try {
            // Décalage ~50m en degrés
            double dLat = 50.0 / 111_320.0;
            double dLon = 50.0 / (111_320.0 * Math.cos(Math.toRadians(lat)));

            // 5 points : Centre, Nord, Sud, Est, Ouest
            String lons = String.format(Locale.US, "%.6f|%.6f|%.6f|%.6f|%.6f", lon, lon, lon, lon + dLon, lon - dLon);
            String lats = String.format(Locale.US, "%.6f|%.6f|%.6f|%.6f|%.6f", lat, lat + dLat, lat - dLat, lat, lat);

            String url = BASE_URL + "/altimetrie/1.0/calcul/alti/rest/elevation.json?lon="
                    + URLEncoder.encode(lons, StandardCharsets.UTF_8)
                    + "&lat=" + URLEncoder.encode(lats, StandardCharsets.UTF_8)
                    + "&resource=ign_rge_alti_wld&delimiter=%7C";

            JsonNode root = getJson(url);
            JsonNode elevations = root.path("elevations");
            if (elevations.size() < 5) {
                return Map.of("status", "ERROR", "message", "Réponse RGE ALTI incomplète");
            }

            double zCenter = elevations.get(0).path("z").asDouble();
            double zNorth = elevations.get(1).path("z").asDouble();
            double zSouth = elevations.get(2).path("z").asDouble();
            double zEast = elevations.get(3).path("z").asDouble();
            double zWest = elevations.get(4).path("z").asDouble();

            // Gradient sur 100m (N-S et E-W)
            double dzDy = (zNorth - zSouth) / 100.0;
            double dzDx = (zEast - zWest) / 100.0;
            double slopePercent = Math.hypot(dzDx, dzDy) * 100.0;

            String downhillDirection = "Terrain quasi-plat";
            if (slopePercent >= 0.5) {
                String ns = dzDy > 0 ? "Sud" : "Nord";
                String ew = dzDx > 0 ? "Ouest" : "Est";
                downhillDirection = "Écoulement descendant vers le " + ns + "-" + ew
                        + " (montée vers le " + (dzDy > 0 ? "Nord" : "Sud") + "-" + (dzDx > 0 ? "Est" : "Ouest") + ")";
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("source", "IGN RGE ALTI (ign_rge_alti_wld)");
            result.put("altitude_center_m", Math.round(zCenter * 100.0) / 100.0);
            result.put("altitude_north_50m", zNorth);
            result.put("altitude_south_50m", zSouth);
            result.put("altitude_east_50m", zEast);
            result.put("altitude_west_50m", zWest);
            result.put("slope_percent", Math.round(slopePercent * 100.0) / 100.0);
            result.put("slope_category", slopePercent < 3.0 ? "FAIBLE (< 3%)" : (slopePercent <= 8.0 ? "MODÉRÉE (3% à 8%)" : "FORTE (> 8%)"));
            result.put("orientation_ecoulement", downhillDirection);
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // 3. ISOCHRONE & ACCESSIBILITY (Navigation IGN bdtopo-valhalla)
    // =========================================================================

    @Tool(
        name = "compute_isochrone",
        value = "Calcule un polygone isochrone d'accessibilité via l'API de navigation IGN (bdtopo-valhalla) à partir d'un point (lon, lat), d'une durée en secondes et d'un mode de déplacement ('car' ou 'pedestrian')."
    )
    @Schema(
        name = "compute_isochrone",
        description = "Calcule un polygone isochrone d'accessibilité via l'API de navigation IGN (bdtopo-valhalla) à partir d'un point (lon, lat), d'une durée en secondes et d'un mode de déplacement ('car' ou 'pedestrian')."
    )
    public Map<String, Object> computeIsochrone(
            @P("Longitude WGS84 du point de départ ou d'intervention")
            @Schema(name = "lon", description = "Longitude WGS84 du point de départ ou d'intervention")
            double lon,
            @P("Latitude WGS84 du point de départ ou d'intervention")
            @Schema(name = "lat", description = "Latitude WGS84 du point de départ ou d'intervention")
            double lat,
            @P("Durée du trajet en secondes (ex: 300 pour 5 minutes, 600 pour 10 minutes)")
            @Schema(name = "durationSeconds", description = "Durée du trajet en secondes (ex: 300 pour 5 minutes, 600 pour 10 minutes)")
            int durationSeconds,
            @P("Profil de déplacement : 'car' (voiture/secours) ou 'pedestrian' (piéton)")
            @Schema(name = "profile", description = "Profil de déplacement : 'car' (voiture/secours) ou 'pedestrian' (piéton)")
            String profile
    ) {
        String normalizedProfile = (profile == null || profile.isBlank()) ? "car" : profile.toLowerCase(Locale.ROOT).trim();
        if (!normalizedProfile.equals("car") && !normalizedProfile.equals("pedestrian")) {
            normalizedProfile = "car";
        }
        logToolCall("compute_isochrone", String.format(Locale.US, "lon=%.5f, lat=%.5f, duration=%ds, profile=%s",
                lon, lat, durationSeconds, normalizedProfile));

        try {
            String url = String.format(Locale.US,
                    "%s/navigation/isochrone?point=%.6f,%.6f&resource=bdtopo-valhalla&costValue=%d&costType=time&profile=%s&direction=departure",
                    BASE_URL, lon, lat, durationSeconds, normalizedProfile);

            JsonNode root = getJson(url);
            JsonNode geometry = root.path("geometry");
            JsonNode ring = geometry.path("coordinates").path(0);

            double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
            double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
            for (JsonNode pt : ring) {
                double x = pt.get(0).asDouble();
                double y = pt.get(1).asDouble();
                minLon = Math.min(minLon, x);
                maxLon = Math.max(maxLon, x);
                minLat = Math.min(minLat, y);
                maxLat = Math.max(maxLat, y);
            }

            double widthKm = (maxLon - minLon) * 111.32 * Math.cos(Math.toRadians(lat));
            double heightKm = (maxLat - minLat) * 111.32;

            Path savedFile = saveMapJson("isochrone.geojson", root);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("resource", "IGN bdtopo-valhalla");
            result.put("profile", normalizedProfile);
            result.put("duration_seconds", durationSeconds);
            result.put("duration_minutes", Math.round(durationSeconds / 6.0) / 10.0);
            result.put("polygon_vertices_count", ring.size());
            result.put("envelope_ew_km", Math.round(widthKm * 100.0) / 100.0);
            result.put("envelope_ns_km", Math.round(heightKm * 100.0) / 100.0);
            result.put("bbox_crs84", List.of(minLon, minLat, maxLon, maxLat));
            result.put("saved_geojson_path", savedFile.toString());
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // 4. BD TOPO V3 BUILDINGS (WFS 2.0 BDTOPO_V3:batiment)
    // =========================================================================

    @Tool(
        name = "query_bdtopo_buildings",
        value = "Interroge le flux WFS 2.0 de la Géoplateforme IGN (couche BDTOPO_V3:batiment en projection CRS:84) dans un rayon en mètres autour de (lon, lat). Retourne le nombre de bâtiments, leurs usages, hauteurs, étages, identifiants RNB/cleabs et sauvegarde le GeoJSON."
    )
    @Schema(
        name = "query_bdtopo_buildings",
        description = "Interroge le flux WFS 2.0 de la Géoplateforme IGN (couche BDTOPO_V3:batiment en projection CRS:84) dans un rayon en mètres autour de (lon, lat). Retourne le nombre de bâtiments, leurs usages, hauteurs, étages, identifiants RNB/cleabs et sauvegarde le GeoJSON."
    )
    public Map<String, Object> queryBdTopoBuildings(
            @P("Longitude WGS84 du centre de recherche")
            @Schema(name = "lon", description = "Longitude WGS84 du centre de recherche")
            double lon,
            @P("Latitude WGS84 du centre de recherche")
            @Schema(name = "lat", description = "Latitude WGS84 du centre de recherche")
            double lat,
            @P("Rayon de recherche en mètres (ex: 100 ou 150)")
            @Schema(name = "radiusMeters", description = "Rayon de recherche en mètres (ex: 100 ou 150)")
            double radiusMeters
    ) {
        double effectiveRadius = (radiusMeters <= 0 || radiusMeters > 1000) ? 120.0 : radiusMeters;
        logToolCall("query_bdtopo_buildings", String.format(Locale.US, "lon=%.5f, lat=%.5f, radius=%.0fm",
                lon, lat, effectiveRadius));

        try {
            double[] bbox = computeBbox(lon, lat, effectiveRadius);
            String url = String.format(Locale.US,
                    "%s/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES=BDTOPO_V3:batiment&SRSNAME=CRS:84&BBOX=%.6f,%.6f,%.6f,%.6f,CRS:84&COUNT=100&OUTPUTFORMAT=application/json",
                    BASE_URL, bbox[0], bbox[1], bbox[2], bbox[3]);

            JsonNode root = getJson(url);
            JsonNode features = root.path("features");

            Map<String, Integer> usageCounts = new LinkedHashMap<>();
            double maxHeight = 0.0;
            int maxFloors = 0;
            int totalDwellings = 0;
            List<Map<String, Object>> sampleBuildings = new ArrayList<>();

            for (JsonNode feat : features) {
                JsonNode p = feat.path("properties");
                String usage = p.path("usage_1").asText("Indifférencié");
                usageCounts.merge(usage, 1, Integer::sum);

                double h = p.path("hauteur").isNumber() ? p.path("hauteur").asDouble() : 0.0;
                int floors = p.path("nombre_d_etages").isNumber() ? p.path("nombre_d_etages").asInt() : 0;
                int dwellings = p.path("nombre_de_logements").isNumber() ? p.path("nombre_de_logements").asInt() : 0;

                maxHeight = Math.max(maxHeight, h);
                maxFloors = Math.max(maxFloors, floors);
                totalDwellings += dwellings;

                if (sampleBuildings.size() < 8) {
                    Map<String, Object> b = new LinkedHashMap<>();
                    b.put("cleabs", p.path("cleabs").asText());
                    b.put("nature", p.path("nature").asText());
                    b.put("usage_1", usage);
                    b.put("hauteur_m", h > 0 ? h : (floors > 0 ? floors * 3.0 : "non renseignée"));
                    b.put("nombre_d_etages", floors);
                    b.put("nombre_de_logements", dwellings);
                    b.put("construction_legere", p.path("construction_legere").asBoolean(false));
                    b.put("date_modification", p.path("date_modification").asText("inconnue"));
                    b.put("rnb_id", p.path("identifiants_rnb").asText(""));
                    sampleBuildings.add(b);
                }
            }

            Path savedFile = saveMapJson("buildings.geojson", root);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("layer", "BDTOPO_V3:batiment");
            result.put("crs", "CRS:84");
            result.put("radius_meters", effectiveRadius);
            result.put("buildings_count", features.size());
            result.put("usages_breakdown", usageCounts);
            result.put("max_height_meters", maxHeight);
            result.put("max_floors", maxFloors);
            result.put("total_dwellings_in_zone", totalDwellings);
            result.put("sample_buildings", sampleBuildings);
            result.put("wfs_geojson_url", url);
            result.put("saved_geojson_path", savedFile.toString());
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // 5. BD TOPO V3 ENVIRONMENT (Forêts / Végétation & Hydrographie)
    // =========================================================================

    @Tool(
        name = "query_bdtopo_environment",
        value = "Interroge les couches environnementales de la BD TOPO V3 (BDTOPO_V3:zone_de_vegetation et BDTOPO_V3:cours_d_eau) dans un rayon en mètres autour de (lon, lat) pour détecter les bois, forêts, haies et cours d'eau à proximité."
    )
    @Schema(
        name = "query_bdtopo_environment",
        description = "Interroge les couches environnementales de la BD TOPO V3 (BDTOPO_V3:zone_de_vegetation et BDTOPO_V3:cours_d_eau) dans un rayon en mètres autour de (lon, lat) pour détecter les bois, forêts, haies et cours d'eau à proximité."
    )
    public Map<String, Object> queryBdTopoEnvironment(
            @P("Longitude WGS84 du centre de recherche")
            @Schema(name = "lon", description = "Longitude WGS84 du centre de recherche")
            double lon,
            @P("Latitude WGS84 du centre de recherche")
            @Schema(name = "lat", description = "Latitude WGS84 du centre de recherche")
            double lat,
            @P("Rayon de recherche en mètres (ex: 150 ou 500)")
            @Schema(name = "radiusMeters", description = "Rayon de recherche en mètres (ex: 150 ou 500)")
            double radiusMeters
    ) {
        double effectiveRadius = (radiusMeters <= 0 || radiusMeters > 3000) ? 250.0 : radiusMeters;
        logToolCall("query_bdtopo_environment", String.format(Locale.US, "lon=%.5f, lat=%.5f, radius=%.0fm",
                lon, lat, effectiveRadius));

        try {
            double[] bbox = computeBbox(lon, lat, effectiveRadius);

            // 1. Zones de végétation (Bois, Forêts, Haies)
            String vegUrl = String.format(Locale.US,
                    "%s/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES=BDTOPO_V3:zone_de_vegetation&SRSNAME=CRS:84&BBOX=%.6f,%.6f,%.6f,%.6f,CRS:84&COUNT=30&OUTPUTFORMAT=application/json",
                    BASE_URL, bbox[0], bbox[1], bbox[2], bbox[3]);
            JsonNode vegRoot = getJson(vegUrl);
            JsonNode vegFeatures = vegRoot.path("features");

            List<Map<String, Object>> vegetationZones = new ArrayList<>();
            for (JsonNode f : vegFeatures) {
                JsonNode p = f.path("properties");
                vegetationZones.add(Map.of(
                        "cleabs", p.path("cleabs").asText(),
                        "nature", p.path("nature").asText("Végétation"),
                        "source_acquisition", p.path("methode_d_acquisition_planimetrique").asText("Orthophotographie")
                ));
            }

            // 2. Cours d'eau (dans un rayon élargi d'au moins 400m pour détecter les ruisseaux proches)
            double[] hydroBbox = computeBbox(lon, lat, Math.max(effectiveRadius, 500.0));
            String hydroUrl = String.format(Locale.US,
                    "%s/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES=BDTOPO_V3:cours_d_eau&SRSNAME=CRS:84&BBOX=%.6f,%.6f,%.6f,%.6f,CRS:84&COUNT=20&OUTPUTFORMAT=application/json",
                    BASE_URL, hydroBbox[0], hydroBbox[1], hydroBbox[2], hydroBbox[3]);
            JsonNode hydroRoot = getJson(hydroUrl);
            JsonNode hydroFeatures = hydroRoot.path("features");

            List<Map<String, Object>> watercourses = new ArrayList<>();
            for (JsonNode f : hydroFeatures) {
                JsonNode p = f.path("properties");
                watercourses.add(Map.of(
                        "cleabs", p.path("cleabs").asText(),
                        "toponyme", p.path("toponyme").asText("Cours d'eau non nommé"),
                        "statut", p.path("statut").asText("")
                ));
            }

            saveMapJson("vegetation.geojson", vegRoot);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("radius_meters", effectiveRadius);
            result.put("vegetation_zones_count", vegetationZones.size());
            result.put("has_forest_or_wood_nearby", !vegetationZones.isEmpty());
            result.put("vegetation_zones", vegetationZones);
            result.put("watercourses_within_500m_count", watercourses.size());
            result.put("watercourses", watercourses);
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // 6. BD ORTHO AERIAL IMAGERY (WMS 1.3.0 HR.ORTHOIMAGERY.ORTHOPHOTOS -> PNG)
    // =========================================================================

    @Tool(
        name = "download_bdortho_aerial_image",
        value = "Télécharge une prise de vue aérienne haute résolution (BD ORTHO IGN, couche HR.ORTHOIMAGERY.ORTHOPHOTOS) au format PNG 512x512 via le service WMS de la Géoplateforme autour de (lon, lat). Retourne le chemin local du fichier image pour l'inspection visuelle multimodale."
    )
    @Schema(
        name = "download_bdortho_aerial_image",
        description = "Télécharge une prise de vue aérienne haute résolution (BD ORTHO IGN, couche HR.ORTHOIMAGERY.ORTHOPHOTOS) au format PNG 512x512 via le service WMS de la Géoplateforme autour de (lon, lat). Retourne le chemin local du fichier image pour l'inspection visuelle multimodale."
    )
    public Map<String, Object> downloadBdOrthoAerialImage(
            @P("Longitude WGS84 du centre de la tuile aérienne")
            @Schema(name = "lon", description = "Longitude WGS84 du centre de la tuile aérienne")
            double lon,
            @P("Latitude WGS84 du centre de la tuile aérienne")
            @Schema(name = "lat", description = "Latitude WGS84 du centre de la tuile aérienne")
            double lat,
            @P("Rayon de l'emprise en mètres (ex: 100 pour voir les bâtiments et toitures en détail)")
            @Schema(name = "radiusMeters", description = "Rayon de l'emprise en mètres (ex: 100 pour voir les bâtiments et toitures en détail)")
            double radiusMeters
    ) {
        double effectiveRadius = (radiusMeters <= 0 || radiusMeters > 1000) ? 100.0 : radiusMeters;
        logToolCall("download_bdortho_aerial_image", String.format(Locale.US, "lon=%.5f, lat=%.5f, radius=%.0fm",
                lon, lat, effectiveRadius));

        try {
            double[] bbox = computeBbox(lon, lat, effectiveRadius);
            String wmsUrl = String.format(Locale.US,
                    "%s/wms-r/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=HR.ORTHOIMAGERY.ORTHOPHOTOS&STYLES=&CRS=CRS:84&BBOX=%.6f,%.6f,%.6f,%.6f&WIDTH=512&HEIGHT=512&FORMAT=image/png",
                    BASE_URL, bbox[0], bbox[1], bbox[2], bbox[3]);

            Files.createDirectories(MAP_DATA_DIR);
            Path outputPath = MAP_DATA_DIR.resolve("bd_ortho_tile.png");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(wmsUrl))
                    .header("User-Agent", "IGN-Agentic-Workshop/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                return Map.of("status", "ERROR", "http_status", resp.statusCode());
            }

            Files.write(outputPath, resp.body());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("layer", "HR.ORTHOIMAGERY.ORTHOPHOTOS (BD ORTHO 20cm)");
            result.put("image_path", outputPath.toAbsolutePath().toString());
            result.put("image_size_bytes", resp.body().length);
            result.put("dimensions", "512x512 px");
            result.put("bbox_crs84", List.of(bbox[0], bbox[1], bbox[2], bbox[3]));
            result.put("wms_url", wmsUrl);
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // 7. COLLABORATIVE DB UPDATE (Espace Collaboratif IGN - Action Sensible !)
    // =========================================================================

    @Tool(
        name = "submit_bdtopo_collaborative_report",
        value = "ACTION SENSIBLE : Soumet un signalement qualifié de mise à jour de la base nationale BD TOPO sur l'Espace Collaboratif IGN (ajout/modification de bâtiment, changement d'usage ou anomalie topologique)."
    )
    @Schema(
        name = "submit_bdtopo_collaborative_report",
        description = "ACTION SENSIBLE : Soumet un signalement qualifié de mise à jour de la base nationale BD TOPO sur l'Espace Collaboratif IGN (ajout/modification de bâtiment, changement d'usage ou anomalie topologique)."
    )
    public Map<String, Object> submitBdTopoCollaborativeReport(
            @P("Identifiant cleabs de l'objet BD TOPO concerné (ex: 'BATIMENT0000000334291947') ou coordonnées")
            @Schema(name = "cleabsOrLocation", description = "Identifiant cleabs de l'objet BD TOPO concerné (ex: 'BATIMENT0000000334291947') ou coordonnées")
            String cleabsOrLocation,
            @P("Type d'anomalie qualifiée : 'CONSTRUCTION_NON_RECENSEE', 'EXTENSION_BATIMENT', 'CHANGEMENT_USAGE', ou 'COHERENCE_VALIDEE'")
            @Schema(name = "anomalyType", description = "Type d'anomalie qualifiée : 'CONSTRUCTION_NON_RECENSEE', 'EXTENSION_BATIMENT', 'CHANGEMENT_USAGE', ou 'COHERENCE_VALIDEE'")
            String anomalyType,
            @P("Rapport technique détaillé croisant l'observation visuelle BD ORTHO et les attributs BD TOPO")
            @Schema(name = "technicalReport", description = "Rapport technique détaillé croisant l'observation visuelle BD ORTHO et les attributs BD TOPO")
            String technicalReport
    ) {
        logToolCall("submit_bdtopo_collaborative_report",
                "target=\"" + cleabsOrLocation + "\", type=\"" + anomalyType + "\"");

        String ticketId = "IGN-COLLAB-" + (System.currentTimeMillis() % 100_000);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", "SUBMITTED_TO_IGN_COLLABORATIVE_SPACE");
        report.put("ticket_id", ticketId);
        report.put("timestamp", Instant.now().toString());
        report.put("target_object", cleabsOrLocation);
        report.put("anomaly_type", anomalyType);
        report.put("technical_report", technicalReport);

        try {
            saveMapJson("collaborative-ticket.json", MAPPER.valueToTree(report));
        } catch (Exception ignored) {
        }
        return report;
    }

    // =========================================================================
    // 8. DYNAMIC OGC WFS DISCOVERY (GetCapabilities, DescribeFeatureType, Generic WFS)
    // =========================================================================

    @Tool(
        name = "search_wfs_layers",
        value = "Recherche dynamiquement des couches vectorielles WFS 2.0 dans le catalogue GetCapabilities de la Géoplateforme IGN par mot-clé (ex: 'batiment', 'foret', 'commune', 'parcelle', 'route', 'ocs', 'hydro'). Retourne les noms techniques (TYPENAMES) et titres des couches."
    )
    @Schema(
        name = "search_wfs_layers",
        description = "Recherche dynamiquement des couches vectorielles WFS 2.0 dans le catalogue GetCapabilities de la Géoplateforme IGN par mot-clé (ex: 'batiment', 'foret', 'commune', 'parcelle', 'route', 'ocs', 'hydro'). Retourne les noms techniques (TYPENAMES) et titres des couches."
    )
    public Map<String, Object> searchWfsLayers(
            @P("Mot-clé pour filtrer les couches WFS de la Géoplateforme (ex: 'batiment', 'commune', 'parcelle', 'foret')")
            @Schema(name = "keyword", description = "Mot-clé pour filtrer les couches WFS de la Géoplateforme (ex: 'batiment', 'commune', 'parcelle', 'foret')")
            String keyword
    ) {
        String filter = (keyword == null) ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        logToolCall("search_wfs_layers", "keyword=\"" + filter + "\"");
        try {
            String url = BASE_URL + "/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetCapabilities";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "IGN-Agentic-Workshop/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String xml = resp.body();

            List<Map<String, String>> matches = new ArrayList<>();
            int idx = 0;
            while ((idx = xml.indexOf("<FeatureType", idx)) != -1) {
                if (xml.startsWith("<FeatureTypeList", idx)) {
                    idx += 16;
                    continue;
                }
                int endIdx = xml.indexOf("</FeatureType>", idx);
                if (endIdx == -1) break;
                String block = xml.substring(idx, endIdx);
                String name = extractXmlTag(block, "Name");
                String title = extractXmlTag(block, "Title");
                if (filter.isEmpty()
                        || name.toLowerCase(Locale.ROOT).contains(filter)
                        || title.toLowerCase(Locale.ROOT).contains(filter)) {
                    matches.add(Map.of("typeName", name, "title", title));
                    if (matches.size() >= 25) break;
                }
                idx = endIdx + 14;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("keyword", filter);
            result.put("matches_returned", matches.size());
            result.put("layers", matches);
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @Tool(
        name = "describe_wfs_layer_schema",
        value = "Inspecte dynamiquement le schéma (colonnes et types d'attributs) de n'importe quelle couche WFS 2.0 de la Géoplateforme IGN via DescribeFeatureType (ex: 'BDTOPO_V3:batiment', 'LIMITES_ADMINISTRATIVES_EXPRESS.LATEST:commune')."
    )
    @Schema(
        name = "describe_wfs_layer_schema",
        description = "Inspecte dynamiquement le schéma (colonnes et types d'attributs) de n'importe quelle couche WFS 2.0 de la Géoplateforme IGN via DescribeFeatureType (ex: 'BDTOPO_V3:batiment', 'LIMITES_ADMINISTRATIVES_EXPRESS.LATEST:commune')."
    )
    public Map<String, Object> describeWfsLayerSchema(
            @P("Nom technique complet de la couche WFS (ex: 'BDTOPO_V3:troncon_de_route' ou 'CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle')")
            @Schema(name = "typeName", description = "Nom technique complet de la couche WFS (ex: 'BDTOPO_V3:troncon_de_route' ou 'CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle')")
            String typeName
    ) {
        logToolCall("describe_wfs_layer_schema", "typeName=\"" + typeName + "\"");
        try {
            String url = BASE_URL + "/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=DescribeFeatureType&TYPENAMES="
                    + URLEncoder.encode(typeName, StandardCharsets.UTF_8)
                    + "&OUTPUTFORMAT=application/json";
            JsonNode root = getJson(url);
            JsonNode propsNode = root.path("featureTypes").path(0).path("properties");

            List<Map<String, String>> attributes = new ArrayList<>();
            for (JsonNode p : propsNode) {
                attributes.add(Map.of(
                        "name", p.path("name").asText(),
                        "type", p.path("localType").asText()
                ));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("typeName", typeName);
            result.put("attributes_count", attributes.size());
            result.put("attributes", attributes);
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @Tool(
        name = "query_any_wfs_layer",
        value = "Interroge n'importe quelle couche WFS 2.0 de la Géoplateforme IGN (TYPENAMES) dans un rayon en mètres autour de (lon, lat) en projection CRS:84 et retourne les objets GeoJSON et leurs propriétés."
    )
    @Schema(
        name = "query_any_wfs_layer",
        description = "Interroge n'importe quelle couche WFS 2.0 de la Géoplateforme IGN (TYPENAMES) dans un rayon en mètres autour de (lon, lat) en projection CRS:84 et retourne les objets GeoJSON et leurs propriétés."
    )
    public Map<String, Object> queryAnyWfsLayer(
            @P("Nom technique de la couche WFS (ex: 'BDTOPO_V3:troncon_de_route', 'CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle')")
            @Schema(name = "typeName", description = "Nom technique de la couche WFS (ex: 'BDTOPO_V3:troncon_de_route', 'CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle')")
            String typeName,
            @P("Longitude WGS84 du centre de recherche")
            @Schema(name = "lon", description = "Longitude WGS84 du centre de recherche")
            double lon,
            @P("Latitude WGS84 du centre de recherche")
            @Schema(name = "lat", description = "Latitude WGS84 du centre de recherche")
            double lat,
            @P("Rayon de recherche en mètres (ex: 150)")
            @Schema(name = "radiusMeters", description = "Rayon de recherche en mètres (ex: 150)")
            double radiusMeters,
            @P("Nombre maximum d'objets à retourner (ex: 10)")
            @Schema(name = "limit", description = "Nombre maximum d'objets à retourner (ex: 10)")
            int limit
    ) {
        double effectiveRadius = (radiusMeters <= 0 || radiusMeters > 5000) ? 150.0 : radiusMeters;
        int effectiveLimit = (limit <= 0 || limit > 50) ? 10 : limit;
        logToolCall("query_any_wfs_layer", String.format(Locale.US, "typeName=%s, lon=%.5f, lat=%.5f, radius=%.0fm",
                typeName, lon, lat, effectiveRadius));
        try {
            double[] bbox = computeBbox(lon, lat, effectiveRadius);
            String url = String.format(Locale.US,
                    "%s/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES=%s&SRSNAME=CRS:84&BBOX=%.6f,%.6f,%.6f,%.6f,CRS:84&COUNT=%d&OUTPUTFORMAT=application/json",
                    BASE_URL, URLEncoder.encode(typeName, StandardCharsets.UTF_8),
                    bbox[0], bbox[1], bbox[2], bbox[3], effectiveLimit);

            JsonNode root = getJson(url);
            JsonNode features = root.path("features");
            List<JsonNode> propertiesList = new ArrayList<>();
            for (JsonNode f : features) {
                propertiesList.add(f.path("properties"));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OK");
            result.put("typeName", typeName);
            result.put("features_returned", features.size());
            result.put("wfs_geojson_url", url);
            result.put("features_properties", propertiesList);
            return result;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private static String extractXmlTag(String xmlBlock, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int i = xmlBlock.indexOf(open);
        if (i == -1) return "";
        int j = xmlBlock.indexOf(close, i + open.length());
        if (j == -1) return "";
        String val = xmlBlock.substring(i + open.length(), j).trim();
        if (val.startsWith("<![CDATA[") && val.endsWith("]]>")) {
            val = val.substring(9, val.length() - 3).trim();
        }
        return val;
    }

    private static double[] computeBbox(double lon, double lat, double radiusMeters) {
        double dLat = radiusMeters / 111_320.0;
        double dLon = radiusMeters / (111_320.0 * Math.cos(Math.toRadians(lat)));
        return new double[]{lon - dLon, lat - dLat, lon + dLon, lat + dLat};
    }

    private static JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "IGN-Agentic-Workshop/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " sur " + url + " : " + resp.body());
        }
        return MAPPER.readTree(resp.body());
    }

    private static Path saveMapJson(String filename, JsonNode content) throws IOException {
        Files.createDirectories(MAP_DATA_DIR);
        Path file = MAP_DATA_DIR.resolve(filename);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), content);
        return file.toAbsolutePath();
    }

    private static void logToolCall(String toolName, String argsSummary) {
        String msg = bold(yellow("  🛰️ [Géoplateforme IGN Tool] "))
                + cyan(toolName) + yellow("(" + argsSummary + ")");
        if (Boolean.getBoolean("ign.mcp.stdio")) {
            System.err.println(msg);
        } else {
            System.out.println(msg);
        }
    }
}
