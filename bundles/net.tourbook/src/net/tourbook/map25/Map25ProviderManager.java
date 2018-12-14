/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.oscim.theme.VtmThemes;
import org.osgi.framework.Bundle;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.MapsforgeStyleParser;
import net.tourbook.preferences.MapsforgeThemeStyle;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

public class Map25ProviderManager {

   private static final Bundle                             _bundle                         = TourbookPlugin.getDefault().getBundle();
   private static final IPath                              _stateLocation                  = Platform.getStateLocation(_bundle);

   private static final String                             MAP_PROVIDER_FILE_NAME          = "map25-provider.xml";                     //$NON-NLS-1$
   private static final int                                MAP_PROVIDER_VERSION            = 1;

   public static final String                              MAPSFORGE_MAP_FILE_EXTENTION    = "map";                                    //$NON-NLS-1$
   public static final String                              MAPSFORGE_STYLE_FILE_EXTENTION  = "xml";                                    //$NON-NLS-1$

   private static final String                             TAG_ROOT                        = "Map25Providers";                         //$NON-NLS-1$
   private static final String                             TAG_MAP_PROVIDER                = "MapProvider";                            //$NON-NLS-1$

   private static final String                             ATTR_DESCRIPTION                = "Description";                            //$NON-NLS-1$
   private static final String                             ATTR_IS_DEFAULT                 = "IsDefault";                              //$NON-NLS-1$
   private static final String                             ATTR_IS_ENABLED                 = "IsEnabled";                              //$NON-NLS-1$
   private static final String                             ATTR_IS_OFFLINE_MAP             = "IsOfflineMap";                           //$NON-NLS-1$
   private static final String                             ATTR_MAP_PROVIDER_VERSION       = "Version";                                //$NON-NLS-1$
   private static final String                             ATTR_NAME                       = "Name";                                   //$NON-NLS-1$
   private static final String                             ATTR_THEME                      = "Theme";                                  //$NON-NLS-1$
   private static final String                             ATTR_TILE_ENCODING              = "TileEncoding";                           //$NON-NLS-1$
   private static final String                             ATTR_UUID                       = "UUID";                                   //$NON-NLS-1$

   private static final String                             ATTR_OFFLINE_IS_THEME_FROM_FILE = "Offline_IsThemeFromFile";                //$NON-NLS-1$
   private static final String                             ATTR_OFFLINE_MAP_FILEPATH       = "Offline_MapFilepath";                    //$NON-NLS-1$
   private static final String                             ATTR_OFFLINE_THEME_FILEPATH     = "Offline_ThemeFilepath";                  //$NON-NLS-1$
   private static final String                             ATTR_OFFLINE_THEME_STYLE        = "Offline_ThemeStyle";                     //$NON-NLS-1$

   private static final String                             ATTR_ONLINE_API_KEY            = "Online_APIKey";                          //$NON-NLS-1$
   private static final String                             ATTR_ONLINE_TILE_PATH          = "Online_TilePath";                        //$NON-NLS-1$
   private static final String                             ATTR_ONLINE_URL                = "Online_Url";                             //$NON-NLS-1$

   private static boolean                                  _isDebugViewVisible;
   private static Map25DebugView                           _map25DebugView;

   private static ArrayList<Map25Provider>                 _allMapProvider;

   /**
    * Contains the default default map provider
    */
   private static Map25Provider                            _defaultMapProvider             = createMapProvider_Default();

   private static final ListenerList<IMapProviderListener> _mapProviderListeners           = new ListenerList<>(ListenerList.IDENTITY);

   public static void addMapProviderListener(final IMapProviderListener listener) {
      _mapProviderListeners.add(listener);
   }

   /**
    * opensciencemap.org
    */
   private static Map25Provider createMapProvider_Default() {

      final Map25Provider mapProvider = new Map25Provider();

      mapProvider.isDefault = true;
      mapProvider.isEnabled = true;
      mapProvider.name = Messages.Map25_Provider_OpenScienceMap_Name;
      mapProvider.online_url = "http://opensciencemap.org/tiles/vtm"; //$NON-NLS-1$
      mapProvider.online_TilePath = "/{Z}/{X}/{Y}.vtm"; //$NON-NLS-1$
      mapProvider.tileEncoding = TileEncoding.VTM;
      mapProvider.theme = VtmThemes.DEFAULT;
      mapProvider.description = Messages.Map25_Provider_OpenScienceMap_Description;

      _defaultMapProvider = mapProvider;

      return mapProvider;
   }

   /**
    * Mapsforge
    */
   private static Map25Provider createMapProvider_Mapsforge() {

      final Map25Provider mapProvider = new Map25Provider();

      mapProvider.isEnabled = false;
      mapProvider.name = "0_OpenandromapV4Map_OpenandromapTheme_Switzerland"; //$NON-NLS-1$
      mapProvider.offline_MapFilepath = "C:\\OfflineMaps\\mapfiles\\www.openandromaps.org\\Switzerland_ML.map"; //$NON-NLS-1$
      mapProvider.offline_ThemeFilepath = "C:\\OfflineMaps\\mapstyles\\www.openandromaps.org\\Elements.xml"; //$NON-NLS-1$
      mapProvider.tileEncoding = TileEncoding.MF;
      mapProvider.offline_ThemeStyle = "elv-mtb"; //$NON-NLS-1$
      mapProvider.description = "This popular maps are made for outdoor."
      		+ "They shows cycling and higing routes and also contour lines.\n"
      		+ "In some countries this contour lines are made from ultra precise LIDAR data, rest via strm.\n"
      		+ "before using, you must download maps and themes\n"
      		+ "Map: https://www.openandromaps.org/en/downloads/europe\n"
      		+ "Search youre country, expand the entry via plus sign and select Multilingual Map\n"
      		+ "Theme: https://www.openandromaps.org/en/legend/elevate-mountain-hike-theme\n"
      		+ "Search \"Elevate 4\", \"manual download\" and select \"normal version\"";

      return mapProvider;
   }

   /**
    * mapzen
    */
   private static Map25Provider createMapProvider_Mapzen() {

      final Map25Provider mapProvider = new Map25Provider();

      mapProvider.isEnabled = false;
      mapProvider.name = Messages.Map25_Provider_MapzenVectorTiles_Name;
      mapProvider.online_url = "https://tile.mapzen.com/mapzen/vector/v1/all"; //$NON-NLS-1$
      mapProvider.online_TilePath = "/{Z}/{X}/{Y}.mvt"; //$NON-NLS-1$
      mapProvider.tileEncoding = TileEncoding.MVT;
      mapProvider.online_ApiKey = "mapzen-xxxxxxx"; //$NON-NLS-1$
      mapProvider.description = Messages.Map25_Provider_MapzenVectorTiles_Description;

      return mapProvider;
   }

   /**
    * Own map tile server
    */
   private static Map25Provider createMapProvider_MyTileServer() {

      final Map25Provider mapProvider = new Map25Provider();

      mapProvider.isEnabled = false;
      mapProvider.name = Messages.Map25_Provider_MyTileServer_Name;
      mapProvider.online_url = "http://192.168.99.99:8080/all"; //$NON-NLS-1$
      mapProvider.online_TilePath = "/{Z}/{X}/{Y}.mvt"; //$NON-NLS-1$
      mapProvider.tileEncoding = TileEncoding.MVT;
      mapProvider.description = Messages.Map25_Provider_MyTileServer_Description;

      return mapProvider;
   }

   private static void fireChangeEvent() {

      final Object[] allListeners = _mapProviderListeners.getListeners();
      for (final Object listener : allListeners) {
         ((IMapProviderListener) listener).mapProviderListChanged();
      }
   }

   /**
    * @return Returns all available {@link Map25Provider}.
    */
   public static ArrayList<Map25Provider> getAllMapProviders() {

      if (_allMapProvider == null) {
         _allMapProvider = loadMapProvider();
      }

      return _allMapProvider;
   }

   public static Map25Provider getDefaultMapProvider() {
      return _defaultMapProvider;
   }

   public static Enum<VtmThemes> getDefaultTheme(final TileEncoding tileEncoding) {

      switch (tileEncoding) {
      case MVT:
         return VtmThemes.MAPZEN;

      // Open Science Map
      case VTM:
      default:
         return VtmThemes.DEFAULT;
      }
   }

   /**
    * @return Returns the map vtm debug view when it is visible, otherwise <code>null</code>
    */
   public static Map25DebugView getMap25DebugView() {

      if (_map25DebugView != null && _isDebugViewVisible) {
         return _map25DebugView;
      }

      return null;
   }

   public static Map25Provider getMapProvider(final String mapProviderId) {

      for (final Map25Provider map25Provider : getAllMapProviders()) {

         if (mapProviderId.equals(map25Provider.getId())) {
            return map25Provider;
         }
      }

      return getDefaultMapProvider();
   }

   public static int getThemeIndex(final Enum<VtmThemes> requestedTheme, final TileEncoding tileEncoding) {

      final VtmThemes[] allThemes = VtmThemes.values();

      for (int themeIndex = 0; themeIndex < allThemes.length; themeIndex++) {

         final VtmThemes themeItem = allThemes[themeIndex];

         if (requestedTheme.equals(themeItem)) {
            return themeIndex;
         }
      }

      /*
       * Return default
       */
      final Enum<VtmThemes> defaultTheme = getDefaultTheme(tileEncoding);

      for (int encodingIndex = 0; encodingIndex < allThemes.length; encodingIndex++) {

         final VtmThemes themeItem = allThemes[encodingIndex];

         if (themeItem.equals(defaultTheme)) {
            return encodingIndex;
         }
      }

      return 0;
   }

   private static File getXmlFile() {

      return _stateLocation.append(MAP_PROVIDER_FILE_NAME).toFile();
   }

   public static boolean isDebugViewVisible() {
      return _isDebugViewVisible;
   }

   /**
    * This can be called also from the map app thread.
    *
    * @return
    */
   private static synchronized ArrayList<Map25Provider> loadMapProvider() {

      if (_allMapProvider != null) {
         return _allMapProvider;
      }

      final ArrayList<Map25Provider> allMapProvider = new ArrayList<>();

      final File xmlFile = getXmlFile();

      if (xmlFile.exists()) {

         System.out.println("#################### xml exists"); //$NON-NLS-1$

         try (BufferedReader reader = Files.newBufferedReader(Paths.get(xmlFile.toURI()))) {

            final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
            for (final IMemento mementoChild : xmlRoot.getChildren()) {

               final XMLMemento xml = (XMLMemento) mementoChild;
               if (TAG_MAP_PROVIDER.equals(xml.getType())) {

// SET_FORMATTING_OFF
                  final String xmlUUID = Util.getXmlString(xml, ATTR_UUID, UI.EMPTY_STRING);

                  final Map25Provider mapProvider = new Map25Provider(xmlUUID);

                  mapProvider.isDefault      = Util.getXmlBoolean(xml, ATTR_IS_DEFAULT, false);
                  mapProvider.isEnabled      = Util.getXmlBoolean(xml, ATTR_IS_ENABLED, false);

                  mapProvider.name           = Util.getXmlString(xml, ATTR_NAME, UI.EMPTY_STRING);
                  mapProvider.description    = Util.getXmlString(xml, ATTR_DESCRIPTION, UI.EMPTY_STRING);

                  mapProvider.isOfflineMap   = Util.getXmlBoolean(xml, ATTR_IS_OFFLINE_MAP, false);

                  if (mapProvider.isOfflineMap) {

                     mapProvider.offline_IsThemeFromFile    = Util.getXmlBoolean(xml, ATTR_OFFLINE_IS_THEME_FROM_FILE, true);
                     mapProvider.offline_MapFilepath        = Util.getXmlString(xml, ATTR_OFFLINE_MAP_FILEPATH, UI.EMPTY_STRING);
                     mapProvider.offline_ThemeFilepath      = Util.getXmlString(xml, ATTR_OFFLINE_THEME_FILEPATH, UI.EMPTY_STRING);
                     mapProvider.offline_ThemeStyle         = Util.getXmlString(xml, ATTR_OFFLINE_THEME_STYLE, UI.EMPTY_STRING);

                  } else {

                     mapProvider.online_ApiKey              = Util.getXmlString(xml, ATTR_ONLINE_API_KEY, UI.EMPTY_STRING);
                     mapProvider.online_TilePath            = Util.getXmlString(xml, ATTR_ONLINE_TILE_PATH, UI.EMPTY_STRING);
                     mapProvider.online_url                 = Util.getXmlString(xml, ATTR_ONLINE_URL, UI.EMPTY_STRING);
                  }
// SET_FORMATTING_ON

                  final TileEncoding tileEncoding = (TileEncoding) Util.getXmlEnum(xml, ATTR_TILE_ENCODING, TileEncoding.VTM);
                  mapProvider.tileEncoding = tileEncoding;
                  mapProvider.theme = Util.getXmlEnum(xml, ATTR_THEME, getDefaultTheme(tileEncoding));

                  System.out.println("################## Name, Url and online_TilePath: " + mapProvider.name + " " + mapProvider.online_url //$NON-NLS-1$//$NON-NLS-2$
                        + mapProvider.online_TilePath);

                  allMapProvider.add(mapProvider);
               }
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }

         replaceDefault(allMapProvider);

      } else {

         /*
          * Create default map providers
          */
         allMapProvider.add(_defaultMapProvider);
         allMapProvider.add(createMapProvider_Mapzen());
         allMapProvider.add(createMapProvider_Mapsforge());
         allMapProvider.add(createMapProvider_MyTileServer());
      }

      /*
       * Ensure that at least one map provider is enabled
       */
      boolean isOneEnabled = false;
      for (final Map25Provider map25Provider : allMapProvider) {
         if (map25Provider.isEnabled) {
            isOneEnabled = true;
            break;
         }
      }

      if (!isOneEnabled) {
         // enable default
         _defaultMapProvider.isEnabled = true;
      }

      return allMapProvider;
   }

   /**
    * @param themeFilePathname
    * @return Returns all styles in the theme file or <code>null</code> when the theme file is not
    *         available.
    */
   public static List<MapsforgeThemeStyle> loadMapsforgeThemeStyles(final String themeFilePathname) {

      if (themeFilePathname == null || themeFilePathname.length() == 0) {
         return null;
      }

      final Path themeFilePath = NIO.getPath(themeFilePathname);
      if (themeFilePath == null) {
         return null;
      }

      final MapsforgeStyleParser mfStyleParser = new MapsforgeStyleParser();

      final List<MapsforgeThemeStyle> mfStyles = mfStyleParser.readXML(themeFilePathname);

      return mfStyles;
   }

   public static void removeMapProviderListener(final IMapProviderListener listener) {

      if (listener != null) {
         _mapProviderListeners.remove(listener);
      }
   }

   /**
    * Replace default with new default provider (cloned/loaded) that the uuid is correctly setup
    *
    * @param allMapProvider
    */
   private static void replaceDefault(final ArrayList<Map25Provider> allMapProvider) {

      for (final Map25Provider map25Provider1 : allMapProvider) {

         if (map25Provider1.isDefault) {
            _defaultMapProvider = map25Provider1;
            break;
         }
      }
   }

   /**
    * Save all map providers from the model {@link #_allMapProvider}
    */
   public static void saveMapProvider() {

      final XMLMemento xmlRoot = saveMapProvider_10_CreateXml();
      final File xmlFile = getXmlFile();

      Util.writeXml(xmlRoot, xmlFile);

      fireChangeEvent();
   }

   /**
    * @return
    */
   private static XMLMemento saveMapProvider_10_CreateXml() {

      XMLMemento xmlRoot = null;

      try {

         xmlRoot = saveMapProvider_20_CreateRoot();

         // loop: profiles
         for (final Map25Provider mapProvider : _allMapProvider) {

            final IMemento xml = xmlRoot.createChild(TAG_MAP_PROVIDER);

            final boolean isOfflineMap = mapProvider.isOfflineMap;

            xml.putString(ATTR_UUID, mapProvider.getId().toString());

            xml.putBoolean(ATTR_IS_ENABLED, mapProvider.isEnabled);
            xml.putBoolean(ATTR_IS_DEFAULT, mapProvider.isDefault);

            xml.putString(ATTR_NAME, mapProvider.name);
            xml.putString(ATTR_DESCRIPTION, mapProvider.description);

            xml.putBoolean(ATTR_IS_OFFLINE_MAP, isOfflineMap);

            if (isOfflineMap) {

               xml.putBoolean(ATTR_OFFLINE_IS_THEME_FROM_FILE, mapProvider.offline_IsThemeFromFile);
               xml.putString(ATTR_OFFLINE_MAP_FILEPATH, mapProvider.offline_MapFilepath);
               xml.putString(ATTR_OFFLINE_THEME_FILEPATH, mapProvider.offline_ThemeFilepath);
               xml.putString(ATTR_OFFLINE_THEME_STYLE, mapProvider.offline_ThemeStyle);

            } else {

               xml.putString(ATTR_ONLINE_API_KEY, mapProvider.online_ApiKey);
               xml.putString(ATTR_ONLINE_TILE_PATH, mapProvider.online_TilePath);
               xml.putString(ATTR_ONLINE_URL, mapProvider.online_url);
            }

            Util.setXmlEnum(xml, ATTR_THEME, mapProvider.theme);
            Util.setXmlEnum(xml, ATTR_TILE_ENCODING, mapProvider.tileEncoding);
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento saveMapProvider_20_CreateRoot() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      Util.setXmlDefaultHeader(xmlRoot, _bundle);

      // map provider version
      xmlRoot.putInteger(ATTR_MAP_PROVIDER_VERSION, MAP_PROVIDER_VERSION);

      return xmlRoot;
   }

   /**
    * Save all map provider with new model
    *
    * @param allMapProvider
    */
   public static void saveMapProvider_WithNewModel(final ArrayList<Map25Provider> allMapProvider) {

      replaceDefault(allMapProvider);

      // update model
      _allMapProvider.clear();
      _allMapProvider.addAll(allMapProvider);

      saveMapProvider();
   }

   static void setDebugView(final Map25DebugView map25DebugView) {
      _map25DebugView = map25DebugView;
   }

   static void setDebugViewVisible(final boolean isDebugVisible) {
      _isDebugViewVisible = isDebugVisible;
   }

   public static void updateOfflineLocation() {
      // TODO Auto-generated method stub

   }
}
