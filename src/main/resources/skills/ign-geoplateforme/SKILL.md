---
name: ign-geoplateforme
description: Expert reference for French IGN (Institut national de l'information géographique et forestière) Géoplateforme APIs (data.geopf.fr), including Geocoding, RGE ALTI Altimetry, Navigation/Isochrones, WFS 2.0 BD TOPO V3 vector layers, WMS 1.3.0 BD ORTHO aerial imagery, WMTS Plan IGN V2, and CRS projection rules (CRS:84 vs EPSG:4326 vs EPSG:2154 Lambert-93). Activate whenever building geospatial agents, calling IGN APIs, or rendering IGN maps.
---

# IGN Géoplateforme (`data.geopf.fr`) — Agent Skill & Developer Reference

The **Géoplateforme** (`https://data.geopf.fr`) is the sovereign French public infrastructure operated by **IGN** that replaces the legacy Géoportail (`wxs.ign.fr`). Most core REST and OGC services are **open and require no API key**.

---

## 1. Critical Rule: Coordinate Reference Systems (CRS) & Axis Order

> [!IMPORTANT]
> One of the most common pitfalls when querying OGC WFS 2.0 and WMS 1.3.0 on the Géoplateforme is **axis order inversion** between `EPSG:4326` and `CRS:84`.

| CRS Code | Axis Order | Units | When to Use |
| :--- | :--- | :--- | :--- |
| **`CRS:84`** | **`lon, lat`** (East, North) | Degrees (WGS84) | **RECOMMENDED** for all WFS `BBOX`, WMS 1.3.0 `BBOX`, and GeoJSON output. Avoids lat/lon inversion bugs. |
| `EPSG:4326` | `lat, lon` in OGC 1.3/2.0! | Degrees (WGS84) | Used by REST Isochrone/Geocoding, but **avoid in WFS 2.0 / WMS 1.3.0 `BBOX`** unless you invert coordinates. |
| `EPSG:2154` | `X, Y` (Easting, Northing) | Meters (Lambert-93) | Official French legal projection (RGF93 / Lambert-93) for metric area/distance calculations in mainland France. |

---

## 2. REST APIs (No API Key Required)

### 2.1 Geocoding & Reverse Geocoding (`/geocodage`)
* **Search (Address/Place $\rightarrow$ Coordinates)**:
  ```http
  GET https://data.geopf.fr/geocodage/search?q={encoded_query}&limit=3
  ```
  * Returns a GeoJSON `FeatureCollection` where `geometry.coordinates` is `[lon, lat]` and `properties` includes `label`, `score`, `postcode`, `citycode` (INSEE code), `city`, `x` and `y` (Lambert-93).
* **Reverse Geocoding (Coordinates $\rightarrow$ Address / Parcel)**:
  ```http
  GET https://data.geopf.fr/geocodage/reverse?lon={lon}&lat={lat}&index=address,parcel
  ```

### 2.2 Altimetry — RGE ALTI (`/altimetrie`)
* **Point Elevation (`ign_rge_alti_wld`)**:
  ```http
  GET https://data.geopf.fr/altimetrie/1.0/calcul/alti/rest/elevation.json?lon={lon1|lon2}&lat={lat1|lat2}&resource=ign_rge_alti_wld&delimiter=|
  ```
  * Supports multiple points separated by `|` to compute local **slope (%)** and **aspect/orientation** in a single HTTP request.
  * Response format:
    ```json
    {
      "elevations": [
        { "lon": 2.4237, "lat": 48.8447, "z": 51.69, "acc": "Variable suivant la source de mesure" }
      ]
    }
    ```

### 2.3 Isochrones & Routing (`/navigation`)
* **Isochrone Calculation (Reachability Polygon)**:
  ```http
  GET https://data.geopf.fr/navigation/isochrone?point={lon},{lat}&resource=bdtopo-valhalla&costValue={seconds}&costType=time&profile={car|pedestrian}&direction=departure
  ```
  * `point`: Always **`lon,lat`** (e.g. `2.4237,48.8447`).
  * `resource`: `bdtopo-valhalla` (supports both `car` and `pedestrian`) or `bdtopo-pgr`.
  * `costValue`: Duration in **seconds** (when `costType=time`, e.g., `300` for 5 minutes) or distance in **meters** (when `costType=distance`).
  * Returns a JSON object containing a `geometry` field (`Polygon` in `EPSG:4326` `[lon, lat]`).

---

## 3. Vector Data — OGC WFS 2.0 (`/wfs/ows`)

Base URL: `https://data.geopf.fr/wfs/ows`

### Standard GeoJSON Query Template
```http
GET https://data.geopf.fr/wfs/ows?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES={layer_name}&SRSNAME=CRS:84&BBOX={minLon},{minLat},{maxLon},{maxLat},CRS:84&COUNT={limit}&OUTPUTFORMAT=application/json
```

### Key BD TOPO V3 & Cadastral WFS Layers (`TYPENAMES`)

| Layer Name (`TYPENAMES`) | Description | Key Properties (`properties.*`) |
| :--- | :--- | :--- |
| **`BDTOPO_V3:batiment`** | 3D/2D Buildings footprint and attributes | `cleabs`, `usage_1` (`Résidentiel`, `Commercial et services`, `Industriel`, `Agricole`, `Annexe`), `nature`, `hauteur` (m), `nombre_d_etages`, `nombre_de_logements`, `altitude_minimale_sol`, `altitude_maximale_toit`, `construction_legere`, `date_creation`, `date_modification`, `identifiants_rnb` |
| **`BDTOPO_V3:zone_de_vegetation`** | Forests, woods, hedges, wooded areas | `cleabs`, `nature` (`Bois`, `Forêt fermée de feuillus`, `Forêt fermée de conifères`, `Haie`, `Lande ligneuse`), `methode_d_acquisition_planimetrique` |
| **`BDTOPO_V3:cours_d_eau`** | Rivers, streams, hydrographic network | `cleabs`, `toponyme`, `code_hydrographique`, `caractere_permanent`, `importance` |
| **`BDTOPO_V3:plan_d_eau`** | Lakes, ponds, reservoirs, basins | `cleabs`, `nature`, `toponyme`, `altitude_moyenne` |
| **`BDTOPO_V3:troncon_de_route`** | Road network segments with width & importance | `cleabs`, `nature`, `numero`, `largeur_de_chaussee`, `sens_de_circulation`, `vitesse_moyenne_vl`, `acces_vehicule_leger` |
| **`BDTOPO_V3:equipement_de_transport`** | Stations, airports, parking, transport hubs | `cleabs`, `nature`, `toponyme` |
| **`BDTOPO_V3:zone_d_activite_ou_d_interet`** | Schools, fire stations, hospitals, public services | `cleabs`, `categorie`, `nature` (`Caserne de pompiers`, `Enseignement primaire`, `Mairie`, etc.), `toponyme` |
| **`CADASTRALPARCELS.PARCELLAIRE_EXPRESS:parcelle`** | Official cadastral parcels | `idu`, `numero`, `section`, `code_insee`, `contenance` (area in $m^2$) |

---

## 4. Raster Aerial Imagery — OGC WMS 1.3.0 (`/wms-r/wms`)

Used to fetch high-resolution **BD ORTHO** aerial photographs as PNG/JPEG for **Multimodal Vision (VLM) inspection**:

```http
GET https://data.geopf.fr/wms-r/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=HR.ORTHOIMAGERY.ORTHOPHOTOS&STYLES=&CRS=CRS:84&BBOX={minLon},{minLat},{maxLon},{maxLat}&WIDTH=512&HEIGHT=512&FORMAT=image/png
```

* Other useful WMS Raster layers:
  * `ORTHOIMAGERY.ORTHOPHOTOS` / `HR.ORTHOIMAGERY.ORTHOPHOTOS`: 20cm High-Resolution Aerial Ortho-imagery.
  * `GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2`: Plan IGN v2 multi-scale topographic map.
  * `ELEVATION.SLOPES`: Digital Elevation Model shaded slopes visualization.

---

## 5. Web Map Tiles — WMTS (`Plan IGN V2` & `BD ORTHO`) for MapLibre GL JS

To display IGN basemaps in **MapLibre GL JS** or **Leaflet** without an API key, use the WMTS `GetTile` endpoint in `EPSG:3857` (`PM` TileMatrixSet):

* **Plan IGN V2 (PNG Tiles)**:
  ```text
  https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=image/png
  ```
* **BD ORTHO Aerial Imagery (JPEG Tiles)**:
  ```text
  https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=image/jpeg
  ```
* **Cadastral Parcels Overlay (PNG Transparent Tiles)**:
  ```text
  https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=CADASTRALPARCELS.PARCELLAIRE_EXPRESS&STYLE=PCI%20vecteur&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=image/png
  ```
