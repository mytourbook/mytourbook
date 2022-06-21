/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.map3.layer;

import java.util.HashMap;
import java.util.Locale;

import net.tourbook.map3.Messages;

/**
 * Contains WorldWind default layers.
 */
public class MapDefaultLayer {

//   private static final String                  REG_EX_NONE_WORD_CHARACTERS      = "\\W";  //$NON-NLS-1$

//   Stars                          gov.nasa.worldwind.layers.StarsLayer                     true
//   Atmosphere                     gov.nasa.worldwind.layers.SkyGradientLayer               true
//   NASA Blue Marble Image         gov.nasa.worldwind.layers.Earth.BMNGOneImage             true
//   Blue Marble (WMS) 2004         gov.nasa.worldwind.wms.WMSTiledImageLayer                true
//   i-cubed Landsat                gov.nasa.worldwind.wms.WMSTiledImageLayer                true
//   USDA NAIP                      gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   USDA NAIP USGS                 gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   MS Virtual Earth Aerial        gov.nasa.worldwind.layers.BasicTiledImageLayer           false
//   Bing Imagery                   gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   USGS Topo Scanned Maps 1:250K  gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   USGS Topo Scanned Maps 1:100K  gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   USGS Topo Scanned Maps 1:24K   gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   USGS Urban Area Ortho          gov.nasa.worldwind.layers.BasicTiledImageLayer           false
//   Political Boundaries           gov.nasa.worldwind.layers.Earth.CountryBoundariesLayer   false
//   Open Street Map                gov.nasa.worldwind.wms.WMSTiledImageLayer                false
//   Place Names                    gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer    true
//   World Map                      gov.nasa.worldwind.layers.WorldMapLayer                  true
//   Scale bar                      gov.nasa.worldwind.layers.ScalebarLayer                  true
//   Compass                        gov.nasa.worldwind.layers.CompassLayer                   true

//    *     <Layer className="gov.nasa.worldwind.layers.StarsLayer">
//    *         <!--Individual properties can be specified within Layer entries, like this:-->
//    *         <Property name="Name" value="Stars"/>
//    *     </Layer>
//    *     <Layer className="gov.nasa.worldwind.layers.SkyGradientLayer"/>
//    *     <Layer className="gov.nasa.worldwind.layers.Earth.BMNGOneImage">
//    *         <Property name="MinActiveAltitude" value="3e6"/>
//    *     </Layer>
//    *     <Layer href="config/Earth/BMNGWMSLayer2.xml" actuate="onLoad"/>
//    *     <Layer href="config/Earth/LandsatI3WMSLayer2.xml" actuate="onLoad"/>
//    *     <Layer href="config/Earth/USDANAIPUSGSWMSImageLayer.xml" actuate="onRequest"/>
//    *     <Layer href="config/Earth/BingImagery.xml" actuate="onRequest"/>
//    *     <Layer href="config/Earth/USGSUrbanAreaOrthoLayer.xml" actuate="onRequest"/>
//    *     <!--<Layer className="gov.nasa.worldwind.layers.Earth.OSMMapnikLayer" actuate="onRequest"/>-->
//    *     <!--<Layer className="gov.nasa.worldwind.layers.Earth.OSMCycleMapLayer" actuate="onRequest"/>-->
//    *     <Layer className="gov.nasa.worldwind.layers.Earth.CountryBoundariesLayer" actuate="onRequest"/>
//    *     <Layer href="config/Earth/OpenStreetMap.xml" actuate="onRequest"/>
//    *     <Layer href="config/Earth/EarthAtNightLayer.xml" actuate="onRequest"/>
//    *     <Layer className="gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer"/>
//    *     <Layer className="gov.nasa.worldwind.layers.WorldMapLayer"/>
//    *     <Layer className="gov.nasa.worldwind.layers.ScalebarLayer"/>
//    *     <Layer className="gov.nasa.worldwind.layers.CompassLayer"/>

   /**
    * <code>
    *
    *
    *  These names are translated and needs to be used as a layer id.
    *
    *    fr
    *
    *       �toiles
    *       Atmosph�re
    *       NASA Blue Marble Image
    *       Blue Marble May 2004
    *       i-cubed Landsat
    *       USDA NAIP
    *       USDA NAIP USGS
    *       MS Virtual Earth Aerial
    *       Bing Imagery
    *       USGS Topo Scanned Maps 1:250K
    *       USGS Topo Scanned Maps 1:100K
    *       USGS Topo Scanned Maps 1:24K
    *       USGS Urban Area Ortho
    *       Political Boundaries
    *       Open Street Map
    *       Earth at Night
    *
    *       Toponymes
    *       Carte du monde
    *       �chelle
    *       Boussole
    *
    *
    *  de
    *    en
    *  es
    *  it
    *  nl
    *
    *       Stars
    *       Atmosphere
    *       NASA Blue Marble Image
    *       Blue Marble May 2004
    *       i-cubed Landsat
    *       USDA NAIP
    *       USDA NAIP USGS
    *       MS Virtual Earth Aerial
    *       Bing Imagery
    *       USGS Topo Scanned Maps 1:250K
    *       USGS Topo Scanned Maps 1:100K
    *       USGS Topo Scanned Maps 1:24K
    *       USGS Urban Area Ortho
    *       Political Boundaries
    *       Open Street Map
    *       Earth at Night
    *
    *       Place Names
    *       World Map
    *       Scale bar
    *       Compass
    *
    * </code>
    */

   /*
    * default layer id's
    */
   public static final String ID_STARS                  = getNormalizedLayerKey("Stars");                  //$NON-NLS-1$
   public static final String ID_ATMOSPHERE             = getNormalizedLayerKey("Atmosphere");             //$NON-NLS-1$

   public static final String ID_NASA_BLUE_MARBLE_IMAGE = getNormalizedLayerKey("NASA Blue Marble Image"); //$NON-NLS-1$
   public static final String ID_BLUE_MARBLE_WMS_2004   = getNormalizedLayerKey("Blue Marble May 2004");   //$NON-NLS-1$
   public static final String ID_I_CUBED_LANDSAT        = getNormalizedLayerKey("i-cubed Landsat");        //$NON-NLS-1$
   public static final String ID_USGS_NAIP_PLUS         = getNormalizedLayerKey("USGS NAIP Plus");         //$NON-NLS-1$

   // Commenting this layer as it is commented in the WorldWind 2.1 and hence not available
   // Here is the error message displayed to the user :
   // !MESSAGE NTMV_MM_001 Layer "MS Virtual Earth Aerial" is not a ww default layer.
   // see here : https://github.com/NASAWorldWind/WorldWindJava/blob/7a329a40861f02af5c16293c2c695c3cc1493469/src/config/worldwind.layers.xml#L26
   //public static final String                         ID_MS_VIRTUAL_EARTH_AERIAL       = getNormalizedLayerKey("MS Virtual Earth Aerial");       //$NON-NLS-1$

   public static final String                         ID_BING_IMAGERY         = getNormalizedLayerKey("Bing Imagery");         //$NON-NLS-1$

   public static final String                         ID_OPEN_STREET_MAP      = getNormalizedLayerKey("Open Street Map");      //$NON-NLS-1$
   public static final String                         ID_EARTH_AT_NIGHT       = getNormalizedLayerKey("Earth at Night");       //$NON-NLS-1$

   public static final String                         ID_POLITICAL_BOUNDARIES = getNormalizedLayerKey("Political Boundaries"); //$NON-NLS-1$
   public static final String                         ID_PLACE_NAMES          = getNormalizedLayerKey("Place Names");          //$NON-NLS-1$
   public static final String                         ID_WORLD_MAP            = getNormalizedLayerKey("World Map");            //$NON-NLS-1$
   public static final String                         ID_SCALE_BAR            = getNormalizedLayerKey("Scale bar");            //$NON-NLS-1$
   public static final String                         ID_COMPASS              = getNormalizedLayerKey("Compass");              //$NON-NLS-1$

   private static final HashMap<String, DefaultLayer> _wwDefaultLayer         = new HashMap<>();

   /**
    * Key is the global (english) layer id, value is the locale layer id.
    */
   private static final HashMap<String, String>       _wwLocaleLayerNames     = new HashMap<>();

   static {

      /*
       * Overwrite layer id's when they are translated. :-(((
       */

      if (Locale.getDefault().getLanguage().equals(new Locale("fr").getLanguage())) { //            //$NON-NLS-1$

         /**
          * French layers
          */

         _wwLocaleLayerNames.put(ID_ATMOSPHERE, "Atmosph\u00E8re"); //$NON-NLS-1$

         _wwLocaleLayerNames.put(ID_PLACE_NAMES, "Toponymes"); //$NON-NLS-1$
         _wwLocaleLayerNames.put(ID_WORLD_MAP, "Carte du monde"); //$NON-NLS-1$
         _wwLocaleLayerNames.put(ID_SCALE_BAR, "\u00C9chelle"); //$NON-NLS-1$
         _wwLocaleLayerNames.put(ID_COMPASS, "Boussole"); //$NON-NLS-1$

      } else {

         // all other languages do not contain translated layer id's

         _wwLocaleLayerNames.put(ID_ATMOSPHERE, ID_ATMOSPHERE);

         _wwLocaleLayerNames.put(ID_PLACE_NAMES, ID_PLACE_NAMES);
         _wwLocaleLayerNames.put(ID_WORLD_MAP, ID_WORLD_MAP);
         _wwLocaleLayerNames.put(ID_SCALE_BAR, ID_SCALE_BAR);
         _wwLocaleLayerNames.put(ID_COMPASS, ID_COMPASS);
      }

// SET_FORMATTING_OFF

      // Common layer for all languages
      _wwLocaleLayerNames.put(ID_STARS, ID_STARS);

      _wwLocaleLayerNames.put(ID_NASA_BLUE_MARBLE_IMAGE,          ID_NASA_BLUE_MARBLE_IMAGE);
      _wwLocaleLayerNames.put(ID_BLUE_MARBLE_WMS_2004,            ID_BLUE_MARBLE_WMS_2004);

      _wwLocaleLayerNames.put(ID_I_CUBED_LANDSAT,                 ID_I_CUBED_LANDSAT);
      _wwLocaleLayerNames.put(ID_USGS_NAIP_PLUS,                  ID_USGS_NAIP_PLUS);

      _wwLocaleLayerNames.put(ID_BING_IMAGERY,                    ID_BING_IMAGERY);

      _wwLocaleLayerNames.put(ID_OPEN_STREET_MAP,                 ID_OPEN_STREET_MAP);
      _wwLocaleLayerNames.put(ID_EARTH_AT_NIGHT,                  ID_EARTH_AT_NIGHT);

      _wwLocaleLayerNames.put(ID_POLITICAL_BOUNDARIES,            ID_POLITICAL_BOUNDARIES);

      _wwDefaultLayer.put(ID_STARS,                            new DefaultLayer(ID_STARS,                            Messages.Default_Layer_Stars));
      _wwDefaultLayer.put(ID_ATMOSPHERE,                       new DefaultLayer(ID_ATMOSPHERE,                       Messages.Default_Layer_Atmosphere));
      _wwDefaultLayer.put(ID_NASA_BLUE_MARBLE_IMAGE,           new DefaultLayer(ID_NASA_BLUE_MARBLE_IMAGE,           Messages.Default_Layer_NASABlueMarble));
      _wwDefaultLayer.put(ID_BLUE_MARBLE_WMS_2004,             new DefaultLayer(ID_BLUE_MARBLE_WMS_2004,             Messages.Default_Layer_WMS_NASABlueMarble2004));
      _wwDefaultLayer.put(ID_I_CUBED_LANDSAT,                  new DefaultLayer(ID_I_CUBED_LANDSAT,                  Messages.Default_Layer_WMS_i_cubed_Landsat));
      _wwDefaultLayer.put(ID_USGS_NAIP_PLUS,                   new DefaultLayer(ID_USGS_NAIP_PLUS,                   Messages.Default_Layer_WMS_USGS_NAIP_PLUS));


      _wwDefaultLayer.put(ID_BING_IMAGERY,                     new DefaultLayer(ID_BING_IMAGERY,                     Messages.Default_Layer_WMS_BingImagery));

      _wwDefaultLayer.put(ID_POLITICAL_BOUNDARIES,             new DefaultLayer(ID_POLITICAL_BOUNDARIES,             Messages.Default_Layer_PoliticalBoundaries));

      _wwDefaultLayer.put(ID_OPEN_STREET_MAP,                  new DefaultLayer(ID_OPEN_STREET_MAP,                  Messages.Default_Layer_WMS_OpenStreetMap));

      _wwDefaultLayer.put(ID_EARTH_AT_NIGHT,                   new DefaultLayer(ID_EARTH_AT_NIGHT,                   Messages.Default_Layer_WMS_EarthAtNight));

      _wwDefaultLayer.put(ID_PLACE_NAMES,                      new DefaultLayer(ID_PLACE_NAMES,                      Messages.Default_Layer_PlaceNames));
      _wwDefaultLayer.put(ID_WORLD_MAP,                        new DefaultLayer(ID_WORLD_MAP,                        Messages.Default_Layer_WorldMap));
      _wwDefaultLayer.put(ID_SCALE_BAR,                        new DefaultLayer(ID_SCALE_BAR,                        Messages.Default_Layer_ScaleBar));
      _wwDefaultLayer.put(ID_COMPASS,                          new DefaultLayer(ID_COMPASS,                          Messages.Default_Layer_Compass));

// SET_FORMATTING_ON
   }

   /**
    * @param defaultLayerId
    * @return Returns <code>null</code> when layer is not found.
    */
   public static DefaultLayer getLayer(final String defaultLayerId) {
      return _wwDefaultLayer.get(defaultLayerId);
   }

   public static String getLocaleLayerId(final String globalLayerId) {
      return _wwLocaleLayerNames.get(globalLayerId);
   }

   /**
    * Create a layer key from the layer name by removing none-word characters.
    *
    * @param layerName
    * @return
    */
   private static String getNormalizedLayerKey(final String layerName) {

      return layerName;

//      return layerName.replaceAll(REG_EX_NONE_WORD_CHARACTERS, UI.EMPTY_STRING);
   }
}
