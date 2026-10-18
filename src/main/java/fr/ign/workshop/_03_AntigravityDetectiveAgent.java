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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ign.workshop.tools.GeoplateformeTools;
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.AgentInput;
import io.github.glaforge.antigravity.AgentResponse;
import io.github.glaforge.antigravity.CapabilitiesConfig;
import io.github.glaforge.antigravity.Policies;
import io.github.glaforge.antigravity.hooks.HookResult;
import io.github.glaforge.antigravity.tools.Param;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
 * ACTE 3 (Antigravity SDK for Java) : Le « Détective Territorial » Multimodal & Gouverné.
 *
 * <p>Cas d'usage IGN : Qualification et réconciliation collaborative entre l'imagerie aérienne
 * haute résolution (<b>BD ORTHO</b>) et la base vectorielle 3D (<b>BD TOPO</b>).
 *
 * <p>Fonctionnalités mises en oeuvre :
 * <ol>
 *   <li><b>Portabilité Zéro-Dépendance</b> : réutilisation directe de {@link GeoplateformeTools}
 *       (pourtant annotée avec {@code @Tool} et {@code @P} de LangChain4j !).</li>
 *   <li><b>Agent Skill IGN</b> : chargement dynamique de {@code skills/ign-geoplateforme/SKILL.md}.</li>
 *   <li><b>Multimodalité Vision + Vecteur</b> : envoi de la tuile aérienne BD ORTHO ({@link AgentInput.Image})
 *       combinée aux requêtes WFS BD TOPO et RGE ALTI.</li>
 *   <li><b>Gouvernance & Sécurité Souveraine</b> : {@link Policies} et {@code PreToolCallDecideHook}
 *       exigeant une validation humaine avant d'écrire dans l'Espace Collaboratif IGN.</li>
 *   <li><b>Sortie Structurée Typée</b> : désérialisation directe en {@code record} Java {@link QualificationReport}.</li>
 * </ol>
 */
public class _03_AntigravityDetectiveAgent {

    /**
     * Rapport structuré produit par l'agent Détective Territorial IGN.
     */
    @Param(description = "Rapport de qualification croisant l'inspection visuelle BD ORTHO et les vecteurs BD TOPO")
    public record QualificationReport(
            @JsonProperty("adresse_inspectee")
            @Param(description = "Adresse normalisée inspectée")
            String adresseInspectee,

            @Param(name = "lon", description = "Longitude WGS84")
            double lon,

            @Param(name = "lat", description = "Latitude WGS84")
            double lat,

            @Param(name = "altitude_ngf_m", description = "Altitude RGE ALTI en mètres NGF")
            double altitudeNgfMeters,

            @Param(name = "nb_batiments_bdtopo", description = "Nombre de bâtiments recensés dans la BD TOPO sur la zone")
            int nbBatimentsBdTopo,

            @Param(name = "observations_visuelles_bdortho", description = "Description précise de ce qui est visible sur l'orthophotographie aérienne BD ORTHO (toitures, cours, végétation, voirie)")
            String observationsVisuellesBdOrtho,

            @Param(name = "analyse_concordance_topo_ortho", description = "Comparaison entre les polygones/attributs BD TOPO et la réalité terrain visible sur la BD ORTHO")
            String analyseConcordanceTopoOrtho,

            @Param(name = "statut_signalement_collaboratif", description = "Statut de la soumission dans l'Espace Collaboratif IGN (ex: SUBMITTED avec numéro de ticket ou BLOCKED)")
            String statutSignalementCollaboratif,

            @Param(name = "recommandations_geomaticien", description = "Liste des actions recommandées aux opérateurs de mise à jour BD TOPO / OCS GE")
            List<String> recommandationsGeomaticien
    ) {}

    public static void main(String[] args) {
        System.out.println(ready().bold().brightCyan()
                .append("\n================================================================================\n")
                .append("  🕵️ ACTE 3 (Antigravity SDK Java) : Détective Territorial Multimodal & Gouverné\n")
                .append("================================================================================\n")
                .reset());

        boolean autoApprove = List.of(args).contains("--auto-approve") || System.console() == null;
        GeoplateformeTools geoTools = new GeoplateformeTools();

        // 1. Pré-téléchargement de l'orthophotographie aérienne BD ORTHO (512x512 PNG) sur la zone d'inspection
        System.out.println(bold(blue("1. Acquisition de la prise de vue aérienne BD ORTHO (WMS 20cm) sur le secteur Saint-Mandé / IGN...")));
        Map<String, Object> geocode = geoTools.geocodeAddress("73 Avenue de Paris, 94160 Saint-Mandé");
        double lon = (double) geocode.get("lon");
        double lat = (double) geocode.get("lat");
        Map<String, Object> orthoResult = geoTools.downloadBdOrthoAerialImage(lon, lat, 100);
        Path orthoImagePath = Path.of(orthoResult.get("image_path").toString());
        System.out.println("  ✅ Orthophoto haute résolution téléchargée : " + cyan(orthoImagePath.toString())
                + " (" + orthoResult.get("image_size_bytes") + " octets)\n");

        // 2. Chargement du Skill Agent IGN Géoplateforme
        String skillPath = Path.of(".agents/skills/ign-geoplateforme").toAbsolutePath().toString();
        System.out.println(bold(blue("2. Chargement du Skill Agent : ")) + cyan(skillPath));

        // 3. Configuration de l'Agent Antigravity avec Hooks, Security Policies & Structured Output
        AgentConfig config = AgentConfig.builder()
                .instructions("""
                        Tu es le « Détective Territorial » de l'IGN, chargé de qualifier les signalements citoyens
                        et de vérifier la concordance entre l'imagerie aérienne BD ORTHO et la base vectorielle BD TOPO.

                        Lorsqu'un citoyen signale une évolution sur une zone et que l'on te fournit la tuile aérienne BD ORTHO :
                        1. Analyse visuellement la photographie aérienne BD ORTHO jointe (toitures, bâtiments complexes, cours intérieures, lisière boisée, voirie).
                        2. Appelle `query_bdtopo_buildings` (rayon 100m) et `get_elevation_and_slope` autour des coordonnées pour récupérer la vérité BD TOPO et RGE ALTI.
                        3. Appelle `query_bdtopo_environment` (rayon 200m) pour vérifier les zones de végétation adjacentes.
                        4. Appelle `submit_bdtopo_collaborative_report` pour enregistrer ton diagnostic technique dans l'Espace Collaboratif IGN.
                        5. Produis ton rapport final structuré complet en français.
                        """)
                // Réutilisation directe de la classe annotée LangChain4j !
                .addTool(geoTools)
                .addSkillPath(skillPath)
                .capabilities(CapabilitiesConfig.builder().enableViewFile(true).build())
                // Lifecycle Hook : Audit en temps réel des appels aux flux OGC / Géoplateforme
                .addPreToolCallDecideHook((toolCall, context) -> {
                    System.out.println(bold(magenta("  🔍 [Hook Audit OGC] "))
                            + "Vérification de l'appel outil `" + cyan(toolCall.name()) + "`");
                    return CompletableFuture.completedFuture(HookResult.allowed());
                })
                // Security Policy #1 : Garde-fou humain sur la modification de l'Espace Collaboratif IGN
                .addPolicy(Policies.askUser((toolName, argsNode) -> {
                    if ("submit_bdtopo_collaborative_report".equals(toolName)) {
                        System.out.println("\n" + bold(yellow("🛡️ [Security Policy IGN - Espace Collaboratif]")));
                        System.out.println(yellow("  L'agent souhaite écrire un signalement officiel dans la base collaborative nationale :"));
                        System.out.println("  • Cible    : " + cyan(argsNode.path("cleabsOrLocation").asText()));
                        System.out.println("  • Anomalie : " + cyan(argsNode.path("anomalyType").asText()));
                        System.out.println("  • Rapport  : " + argsNode.path("technicalReport").asText());

                        if (autoApprove) {
                            System.out.println(bold(green("  -> [Auto-Approve activé pour la démo] Validation opérateur accordée ✅\n")));
                            return true;
                        }
                        System.out.print(bold(yellow("  Autoriser l'écriture dans l'Espace Collaboratif IGN ? (o/n) [o] : ")));
                        Scanner scanner = new Scanner(System.in);
                        String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "o";
                        boolean approved = input.isEmpty() || input.toLowerCase().startsWith("o") || input.toLowerCase().startsWith("y");
                        System.out.println(approved
                                ? bold(green("  -> Validation opérateur accordée ✅\n"))
                                : bold(red("  -> Écriture refusée par l'opérateur ❌\n")));
                        return approved;
                    }
                    return true;
                }))
                // Security Policy #2 : Autoriser explicitement les outils de lecture Géoplateforme
                .addPolicy(Policies.allowTools(
                        "geocode_address",
                        "get_elevation_and_slope",
                        "query_bdtopo_buildings",
                        "query_bdtopo_environment",
                        "compute_isochrone",
                        "download_bdortho_aerial_image",
                        "submit_bdtopo_collaborative_report",
                        "view_file"
                ))
                // Sortie structurée fortement typée
                .responseSchema(QualificationReport.class)
                .build();

        try (Agent agent = new Agent(config)) {
            String prompt = String.format("""
                    SIGNALEMENT CITOYEN #94160-A :
                    Lieu : 73 Avenue de Paris, 94160 Saint-Mandé (lon=%.6f, lat=%.6f).
                    Message du contributeur : "Sur l'îlot du Géoroom / Avenue de Paris en lisière du Bois de Vincennes,
                    plusieurs grands bâtiments tertiaires et résidentiels entourent des cours végétalisées. Pouvez-vous vérifier
                    la cohérence entre la vue aérienne BD ORTHO ci-jointe et les bâtiments/hauteurs enregistrés dans la BD TOPO,
                    puis soumettre un rapport de qualification sur l'Espace Collaboratif ?"
                    """, lon, lat);

            System.out.println("\n" + bold(blue("3. Envoi de la requête Multimodale (Texte + Image aérienne BD ORTHO 512x512)...\n")));

            AgentResponse response = agent.chat(List.of(
                    AgentInput.Text.of(prompt),
                    AgentInput.Image.fromFile(orthoImagePath)
            )).get(180, TimeUnit.SECONDS);

            QualificationReport report = response.getStructuredOutput(QualificationReport.class);

            // Sauvegarde du rapport structuré pour la visionneuse cartographique 3D (Acte 4)
            Path reportPath = Path.of("target", "map-data", "agent-report.json");
            Files.createDirectories(reportPath.getParent());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);

            System.out.println("\n" + bold(green("================================================================================")));
            System.out.println(bold(green("  ✅ RAPPORT STRUCTURÉ DU DÉTECTIVE TERRITORIAL (Java Record QualificationReport)")));
            System.out.println(bold(green("================================================================================")));
            System.out.println("  • Adresse inspectée      : " + bold(report.adresseInspectee()));
            System.out.println("  • Coordonnées WGS84      : (" + report.lon() + ", " + report.lat() + ")");
            System.out.println("  • Altitude RGE ALTI      : " + report.altitudeNgfMeters() + " m NGF");
            System.out.println("  • Bâtiments BD TOPO      : " + report.nbBatimentsBdTopo() + " bâtiments dans un rayon de 100m");
            System.out.println("  • Observation BD ORTHO   : " + cyan(report.observationsVisuellesBdOrtho()));
            System.out.println("  • Concordance TOPO/ORTHO : " + report.analyseConcordanceTopoOrtho());
            System.out.println("  • Espace Collaboratif    : " + bold(yellow(report.statutSignalementCollaboratif())));
            System.out.println("  • Recommandations IGN    :");
            if (report.recommandationsGeomaticien() != null) {
                for (String rec : report.recommandationsGeomaticien()) {
                    System.out.println("      - " + rec);
                }
            }
            System.out.println("\n  💾 Rapport JSON exporté vers : " + cyan(reportPath.toAbsolutePath().toString()));

        } catch (Exception e) {
            System.err.println(red("Erreur lors de l'exécution de l'agent Antigravity : " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
