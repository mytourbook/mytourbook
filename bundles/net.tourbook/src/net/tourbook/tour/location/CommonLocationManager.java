/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;
import net.tourbook.map.location.SlideoutMapLocation;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Manage common locations
 */
public class CommonLocationManager {

   private static final String             CONFIG_FILE_NAME                         = "common-locations.xml";                      //$NON-NLS-1$

   private static final Bundle             _bundle                                  = TourbookPlugin.getDefault().getBundle();
   private static final IPath              _stateLocation                           = Platform.getStateLocation(_bundle);

   /**
    * Version number is not yet used.
    */
   private static final int                CONFIG_VERSION                           = 1;
   private static final String             ATTR_CONFIG_VERSION                      = "configVersion";                             //$NON-NLS-1$
   //
   private static final String             TAG_ROOT                                 = "CommonLocations";                           //$NON-NLS-1$
   private static final String             TAG_ALL_LOCATIONS                        = "AllLocations";                              //$NON-NLS-1$
   private static final String             TAG_LOCATION                             = "Location";                                  //$NON-NLS-1$
   //
   private static final String             ATTR_CREATED                             = "created";                                   //$NON-NLS-1$
   private static final String             ATTR_ZOOMLEVEL                           = "zoomLevel";                                 //$NON-NLS-1$
   //
   private static final String             ATTR_LATITUDE_E6                         = "latitude";                                  //$NON-NLS-1$
   private static final String             ATTR_LATITUDE_E6_NORMALIZED              = "latitudeNormalized";                        //$NON-NLS-1$
   private static final String             ATTR_LATITUDE_MIN_E6_NORMALIZED          = "latitudeMinE6_Normalized";                  //$NON-NLS-1$
   private static final String             ATTR_LATITUDE_MAX_E6_NORMALIZED          = "latitudeMaxE6_Normalized";                  //$NON-NLS-1$
   private static final String             ATTR_LATITUDE_MIN_E6_RESIZED_NORMALIZED  = "latitudeMinE6_Resized_Normalized";          //$NON-NLS-1$
   private static final String             ATTR_LATITUDE_MAX_E6_RESIZED_NORMALIZED  = "latitudeMaxE6_Resized_Normalized";          //$NON-NLS-1$
   private static final String             ATTR_LONGITUDE_E6                        = "longitude";                                 //$NON-NLS-1$
   private static final String             ATTR_LONGITUDE_E6_NORMALIZED             = "longitudeNormalized";                       //$NON-NLS-1$
   private static final String             ATTR_LONGITUDE_MIN_E6_NORMALIZED         = "longitudeMinE6_Normalized";                 //$NON-NLS-1$
   private static final String             ATTR_LONGITUDE_MAX_E6_NORMALIZED         = "longitudeMaxE6_Normalized";                 //$NON-NLS-1$
   private static final String             ATTR_LONGITUDE_MIN_E6_RESIZED_NORMALIZED = "longitudeMinE6_Resized_Normalized";         //$NON-NLS-1$
   private static final String             ATTR_LONGITUDE_MAX_E6_RESIZED_NORMALIZED = "longitudeMaxE6_Resized_Normalized";         //$NON-NLS-1$
   //
   private static final String             ATTR_NAME_NAME                           = "name";                                      //$NON-NLS-1$
   private static final String             ATTR_NAME_DISPLAY_NAME                   = "displayName";                               //$NON-NLS-1$

   private static final String             ATTR_COUNTRY_COUNTRY                     = "country";                                   //$NON-NLS-1$
   private static final String             ATTR_COUNTRY__COUNTRY_CODE               = "countryCode";                               //$NON-NLS-1$
   private static final String             ATTR_COUNTRY__CONTINENT                  = "continent";                                 //$NON-NLS-1$

   private static final String             ATTR_STATE_REGION                        = "region";                                    //$NON-NLS-1$
   private static final String             ATTR_STATE_STATE                         = "state";                                     //$NON-NLS-1$
   private static final String             ATTR_STATE_STATE_DISTRICT                = "stateDistrict";                             //$NON-NLS-1$
   private static final String             ATTR_STATE_COUNTY                        = "county";                                    //$NON-NLS-1$

   private static final String             ATTR_CITY_MUNICIPALITY                   = "municipality";                              //$NON-NLS-1$
   private static final String             ATTR_CITY_CITY                           = "city";                                      //$NON-NLS-1$
   private static final String             ATTR_CITY_TOWN                           = "town";                                      //$NON-NLS-1$
   private static final String             ATTR_CITY_VILLAGE                        = "village";                                   //$NON-NLS-1$
   private static final String             ATTR_CITY_POSTCODE                       = "postcode";                                  //$NON-NLS-1$

   private static final String             ATTR_ROAD_ROAD                           = "road";                                      //$NON-NLS-1$
   private static final String             ATTR_ROAD_HOUSE_NUMBER                   = "houseNumber";                               //$NON-NLS-1$
   private static final String             ATTR_ROAD_HOUSE_NAME                     = "houseName";                                 //$NON-NLS-1$

   private static final String             ATTR_AREA1_CITY_DISTRICT                 = "cityDistrict";                              //$NON-NLS-1$
   private static final String             ATTR_AREA1_DISTRICT                      = "district";                                  //$NON-NLS-1$
   private static final String             ATTR_AREA1_BOROUGH                       = "borough";                                   //$NON-NLS-1$
   private static final String             ATTR_AREA1_SUBURB                        = "suburb";                                    //$NON-NLS-1$
   private static final String             ATTR_AREA1_SUBDIVISION                   = "subdivision";                               //$NON-NLS-1$

   private static final String             ATTR_AREA2_HAMLET                        = "hamlet";                                    //$NON-NLS-1$
   private static final String             ATTR_AREA2_CROFT                         = "croft";                                     //$NON-NLS-1$
   private static final String             ATTR_AREA2_ISOLATED_DWELLING             = "isolatedDwelling";                          //$NON-NLS-1$

   private static final String             ATTR_AREA3_NEIGHBOURHOOD                 = "neighbourhood";                             //$NON-NLS-1$
   private static final String             ATTR_AREA3_ALLOTMENTS                    = "allotments";                                //$NON-NLS-1$
   private static final String             ATTR_AREA3_QUARTER                       = "quarter";                                   //$NON-NLS-1$

   private static final String             ATTR_AREA4_CITY_BLOCK                    = "cityBlock";                                 //$NON-NLS-1$
   private static final String             ATTR_AREA4_RESIDETIAL                    = "residential";                               //$NON-NLS-1$
   private static final String             ATTR_AREA4_FARM                          = "farm";                                      //$NON-NLS-1$
   private static final String             ATTR_AREA4_FARMYARD                      = "farmyard";                                  //$NON-NLS-1$
   private static final String             ATTR_AREA4_INDUSTRIAL                    = "industrial";                                //$NON-NLS-1$
   private static final String             ATTR_AREA4_COMMERCIAL                    = "commercial";                                //$NON-NLS-1$
   private static final String             ATTR_AREA4_RETAIL                        = "retail";                                    //$NON-NLS-1$

   private static final String             ATTR_OTHER_AERIALWAY                     = "aerialway";                                 //$NON-NLS-1$
   private static final String             ATTR_OTHER_AEROWAY                       = "aeroway";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_AMENITY                       = "amenity";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_BOUNDARY                      = "boundary";                                  //$NON-NLS-1$
   private static final String             ATTR_OTHER_BRIDGE                        = "bridge";                                    //$NON-NLS-1$
   private static final String             ATTR_OTHER_CLUB                          = "club";                                      //$NON-NLS-1$
   private static final String             ATTR_OTHER_CRAFT                         = "craft";                                     //$NON-NLS-1$
   private static final String             ATTR_OTHER_EMERGENCY                     = "emergency";                                 //$NON-NLS-1$
   private static final String             ATTR_OTHER_HISTORIC                      = "historic";                                  //$NON-NLS-1$
   private static final String             ATTR_OTHER_LANDUSE                       = "landuse";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_LEISURE                       = "leisure";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_MAN_MADE                      = "manMade";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_MILITARY                      = "military";                                  //$NON-NLS-1$
   private static final String             ATTR_OTHER_MOUNTAIN_PASS                 = "mountainPass";                              //$NON-NLS-1$
   private static final String             ATTR_OTHER_NATURAL2                      = "natural2";                                  //$NON-NLS-1$
   private static final String             ATTR_OTHER_OFFICE                        = "office";                                    //$NON-NLS-1$
   private static final String             ATTR_OTHER_PLACE                         = "place";                                     //$NON-NLS-1$
   private static final String             ATTR_OTHER_RAILWAY                       = "railway";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_SHOP                          = "shop";                                      //$NON-NLS-1$
   private static final String             ATTR_OTHER_TOURISM                       = "tourism";                                   //$NON-NLS-1$
   private static final String             ATTR_OTHER_TUNNEL                        = "tunnel";                                    //$NON-NLS-1$
   private static final String             ATTR_OTHER_WATERWAY                      = "waterway";                                  //$NON-NLS-1$
   //
   private static final List<TourLocation> _allMapLocations                         = new ArrayList<>();

   private static SlideoutMapLocation      _mapLocationSlideout;

   private static int                      _locationRequestZoomlevel                = TourLocationManager.DEFAULT_ZOOM_LEVEL_VALUE;

   static {

      // load locations
      readLocationsFromXml();
   }

   public static void addLocation(final TourLocation tourLocation) {

      // update model
      _allMapLocations.add(tourLocation);

      // update UI
      if (_mapLocationSlideout != null) {

         _mapLocationSlideout.open(false);

         // delay to be sure that the slideout is opened
         PlatformUI.getWorkbench().getDisplay().asyncExec(() -> _mapLocationSlideout.updateUI(tourLocation));
      }
   }

   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // config version
      xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

      return xmlRoot;
   }

   public static boolean deleteLocations(final List<TourLocation> mapLocations) {

      _allMapLocations.removeAll(mapLocations);

      return true;
   }

   public static List<TourLocation> getAddressLocations() {
      return _allMapLocations;
   }

   public static int getLocationRequestZoomlevel() {
      return _locationRequestZoomlevel;
   }

   private static File getXmlFile() {

      final File xmlFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return xmlFile;
   }

   /**
    * @param xmlRoot
    *           Can be <code>null</code> when not available
    * @param allLocations
    */
   private static void parse_20_Locations(final XMLMemento xmlRoot,
                                          final List<TourLocation> allLocations) {

      final XMLMemento xmlAllLocations = (XMLMemento) xmlRoot.getChild(TAG_ALL_LOCATIONS);

      if (xmlAllLocations == null) {
         return;
      }

      for (final IMemento mementoLocation : xmlAllLocations.getChildren()) {

         final XMLMemento xmlLocation = (XMLMemento) mementoLocation;

         try {

            final String xmlConfigType = xmlLocation.getType();

            if (xmlConfigType.equals(TAG_LOCATION)) {

               // <Location>

               allLocations.add(parse_22_Locations_One(xmlLocation));
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlLocation), e);
         }
      }
   }

   private static TourLocation parse_22_Locations_One(final XMLMemento xmlLocation) {

      final ZonedDateTime created = Util.getXmlDateTime(xmlLocation, ATTR_CREATED, TimeTools.now());

      final TourLocation tourLocation = new TourLocation(created);

// SET_FORMATTING_OFF

      tourLocation.name                = parseString(xmlLocation,  ATTR_NAME_NAME);
      tourLocation.display_name        = parseString(xmlLocation,  ATTR_NAME_DISPLAY_NAME);

      tourLocation.zoomlevel           = Util.getXmlInteger(xmlLocation, ATTR_ZOOMLEVEL, TourLocationManager.DEFAULT_ZOOM_LEVEL_VALUE);

      tourLocation.country             = parseString(xmlLocation,  ATTR_COUNTRY_COUNTRY);
      tourLocation.country_code        = parseString(xmlLocation,  ATTR_COUNTRY__COUNTRY_CODE);
      tourLocation.continent           = parseString(xmlLocation,  ATTR_COUNTRY__CONTINENT);

      tourLocation.region              = parseString(xmlLocation,  ATTR_STATE_REGION);
      tourLocation.state               = parseString(xmlLocation,  ATTR_STATE_STATE);
      tourLocation.state_district      = parseString(xmlLocation,  ATTR_STATE_STATE_DISTRICT);
      tourLocation.county              = parseString(xmlLocation,  ATTR_STATE_COUNTY);

      tourLocation.municipality        = parseString(xmlLocation,  ATTR_CITY_MUNICIPALITY);
      tourLocation.city                = parseString(xmlLocation,  ATTR_CITY_CITY);
      tourLocation.town                = parseString(xmlLocation,  ATTR_CITY_TOWN);
      tourLocation.village             = parseString(xmlLocation,  ATTR_CITY_VILLAGE);
      tourLocation.postcode            = parseString(xmlLocation,  ATTR_CITY_POSTCODE);

      tourLocation.road                = parseString(xmlLocation,  ATTR_ROAD_ROAD);
      tourLocation.house_number        = parseString(xmlLocation,  ATTR_ROAD_HOUSE_NUMBER);
      tourLocation.house_name          = parseString(xmlLocation,  ATTR_ROAD_HOUSE_NAME);

      tourLocation.city_district       = parseString(xmlLocation,  ATTR_AREA1_CITY_DISTRICT);
      tourLocation.district            = parseString(xmlLocation,  ATTR_AREA1_DISTRICT);
      tourLocation.borough             = parseString(xmlLocation,  ATTR_AREA1_BOROUGH);
      tourLocation.suburb              = parseString(xmlLocation,  ATTR_AREA1_SUBURB);
      tourLocation.subdivision         = parseString(xmlLocation,  ATTR_AREA1_SUBDIVISION);

      tourLocation.hamlet              = parseString(xmlLocation,  ATTR_AREA2_HAMLET);
      tourLocation.croft               = parseString(xmlLocation,  ATTR_AREA2_CROFT);
      tourLocation.isolated_dwelling   = parseString(xmlLocation,  ATTR_AREA2_ISOLATED_DWELLING);

      tourLocation.neighbourhood       = parseString(xmlLocation,  ATTR_AREA3_NEIGHBOURHOOD);
      tourLocation.allotments          = parseString(xmlLocation,  ATTR_AREA3_ALLOTMENTS);
      tourLocation.quarter             = parseString(xmlLocation,  ATTR_AREA3_QUARTER);

      tourLocation.city_block          = parseString(xmlLocation,  ATTR_AREA4_CITY_BLOCK);
      tourLocation.residential         = parseString(xmlLocation,  ATTR_AREA4_RESIDETIAL);
      tourLocation.farm                = parseString(xmlLocation,  ATTR_AREA4_FARM);
      tourLocation.farmyard            = parseString(xmlLocation,  ATTR_AREA4_FARMYARD);
      tourLocation.industrial          = parseString(xmlLocation,  ATTR_AREA4_INDUSTRIAL);
      tourLocation.commercial          = parseString(xmlLocation,  ATTR_AREA4_COMMERCIAL);
      tourLocation.retail              = parseString(xmlLocation,  ATTR_AREA4_RETAIL);

      tourLocation.aerialway           = parseString(xmlLocation,  ATTR_OTHER_AERIALWAY);
      tourLocation.aeroway             = parseString(xmlLocation,  ATTR_OTHER_AEROWAY);
      tourLocation.amenity             = parseString(xmlLocation,  ATTR_OTHER_AMENITY);
      tourLocation.boundary            = parseString(xmlLocation,  ATTR_OTHER_BOUNDARY);
      tourLocation.bridge              = parseString(xmlLocation,  ATTR_OTHER_BRIDGE);
      tourLocation.club                = parseString(xmlLocation,  ATTR_OTHER_CLUB);
      tourLocation.craft               = parseString(xmlLocation,  ATTR_OTHER_CRAFT);
      tourLocation.emergency           = parseString(xmlLocation,  ATTR_OTHER_EMERGENCY);
      tourLocation.historic            = parseString(xmlLocation,  ATTR_OTHER_HISTORIC);
      tourLocation.landuse             = parseString(xmlLocation,  ATTR_OTHER_LANDUSE);
      tourLocation.leisure             = parseString(xmlLocation,  ATTR_OTHER_LEISURE);
      tourLocation.man_made            = parseString(xmlLocation,  ATTR_OTHER_MAN_MADE);
      tourLocation.military            = parseString(xmlLocation,  ATTR_OTHER_MILITARY);
      tourLocation.mountain_pass       = parseString(xmlLocation,  ATTR_OTHER_MOUNTAIN_PASS);
      tourLocation.natural2            = parseString(xmlLocation,  ATTR_OTHER_NATURAL2);
      tourLocation.office              = parseString(xmlLocation,  ATTR_OTHER_OFFICE);
      tourLocation.place               = parseString(xmlLocation,  ATTR_OTHER_PLACE);
      tourLocation.railway             = parseString(xmlLocation,  ATTR_OTHER_RAILWAY);
      tourLocation.shop                = parseString(xmlLocation,  ATTR_OTHER_SHOP);
      tourLocation.tourism             = parseString(xmlLocation,  ATTR_OTHER_TOURISM);
      tourLocation.tunnel              = parseString(xmlLocation,  ATTR_OTHER_TUNNEL);
      tourLocation.waterway            = parseString(xmlLocation,  ATTR_OTHER_WATERWAY);

      tourLocation.latitudeE6                            = parseInt(xmlLocation, ATTR_LATITUDE_E6);
      tourLocation.latitudeE6_Normalized                 = parseInt(xmlLocation, ATTR_LATITUDE_E6_NORMALIZED);

      tourLocation.latitudeMinE6_Normalized              = parseInt(xmlLocation, ATTR_LATITUDE_MIN_E6_NORMALIZED);
      tourLocation.latitudeMaxE6_Normalized              = parseInt(xmlLocation, ATTR_LATITUDE_MAX_E6_NORMALIZED);
      tourLocation.latitudeMinE6_Resized_Normalized      = parseInt(xmlLocation, ATTR_LATITUDE_MIN_E6_RESIZED_NORMALIZED);
      tourLocation.latitudeMaxE6_Resized_Normalized      = parseInt(xmlLocation, ATTR_LATITUDE_MAX_E6_RESIZED_NORMALIZED);

      tourLocation.longitudeE6                           = parseInt(xmlLocation, ATTR_LONGITUDE_E6);
      tourLocation.longitudeE6_Normalized                = parseInt(xmlLocation, ATTR_LONGITUDE_E6_NORMALIZED);

      tourLocation.longitudeMinE6_Normalized             = parseInt(xmlLocation, ATTR_LONGITUDE_MIN_E6_NORMALIZED);
      tourLocation.longitudeMaxE6_Normalized             = parseInt(xmlLocation, ATTR_LONGITUDE_MAX_E6_NORMALIZED);
      tourLocation.longitudeMinE6_Resized_Normalized     = parseInt(xmlLocation, ATTR_LONGITUDE_MIN_E6_RESIZED_NORMALIZED);
      tourLocation.longitudeMaxE6_Resized_Normalized     = parseInt(xmlLocation, ATTR_LONGITUDE_MAX_E6_RESIZED_NORMALIZED);

// SET_FORMATTING_ON

      tourLocation.setTransientValues();

      return tourLocation;
   }

   private static int parseInt(final XMLMemento xmlLocation, final String key) {

      return Util.getXmlInteger(xmlLocation, key, 0);
   }

   private static String parseString(final XMLMemento xmlLocation, final String key) {

      return Util.getXmlString(xmlLocation, key, null);
   }

   /**
    * Read or create configuration a xml file
    *
    * @return
    */
   private static synchronized void readLocationsFromXml() {

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get locations from saved xml file
         final File xmlFile = getXmlFile();
         final String absoluteFilePath = xmlFile.getAbsolutePath();
         final File inputFile = new File(absoluteFilePath);

         if (inputFile.exists()) {

            try {

               reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         if (xmlRoot == null) {
            return;
         }

         parse_20_Locations(xmlRoot, _allMapLocations);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void saveState() {

      final XMLMemento xmlRoot = create_Root();

      saveState_10_Locations(xmlRoot);

      Util.writeXml(xmlRoot, getXmlFile());
   }

   private static void saveState_10_Locations(final XMLMemento xmlRoot) {

      // <AllLocations>
      final IMemento xmlAllLocations = xmlRoot.createChild(TAG_ALL_LOCATIONS);

      for (final TourLocation tourLocation : _allMapLocations) {

         // <Location>
         final IMemento xmlLocation = xmlAllLocations.createChild(TAG_LOCATION);
         {
// SET_FORMATTING_OFF

            xmlLocation.putString( ATTR_NAME_NAME,                tourLocation.name);
            xmlLocation.putString( ATTR_NAME_DISPLAY_NAME,        tourLocation.display_name);

            xmlLocation.putInteger(ATTR_ZOOMLEVEL,                tourLocation.zoomlevel);
            xmlLocation.putString( ATTR_CREATED,                  tourLocation.getCreated().toString());

            saveValue(xmlLocation,  ATTR_COUNTRY_COUNTRY,          tourLocation.country);
            saveValue(xmlLocation,  ATTR_COUNTRY__COUNTRY_CODE,    tourLocation.country_code);
            saveValue(xmlLocation,  ATTR_COUNTRY__CONTINENT,       tourLocation.continent);

            saveValue(xmlLocation,  ATTR_STATE_REGION,             tourLocation.region);
            saveValue(xmlLocation,  ATTR_STATE_STATE,              tourLocation.state);
            saveValue(xmlLocation,  ATTR_STATE_STATE_DISTRICT,     tourLocation.state_district);
            saveValue(xmlLocation,  ATTR_STATE_COUNTY,             tourLocation.county);

            saveValue(xmlLocation,  ATTR_CITY_MUNICIPALITY,        tourLocation.municipality);
            saveValue(xmlLocation,  ATTR_CITY_CITY,                tourLocation.city);
            saveValue(xmlLocation,  ATTR_CITY_TOWN,                tourLocation.town);
            saveValue(xmlLocation,  ATTR_CITY_VILLAGE,             tourLocation.village);
            saveValue(xmlLocation,  ATTR_CITY_POSTCODE,            tourLocation.postcode);

            saveValue(xmlLocation,  ATTR_ROAD_ROAD,                tourLocation.road);
            saveValue(xmlLocation,  ATTR_ROAD_HOUSE_NUMBER,        tourLocation.house_number);
            saveValue(xmlLocation,  ATTR_ROAD_HOUSE_NAME,          tourLocation.house_name);

            saveValue(xmlLocation,  ATTR_AREA1_CITY_DISTRICT,      tourLocation.city_district);
            saveValue(xmlLocation,  ATTR_AREA1_DISTRICT,           tourLocation.district);
            saveValue(xmlLocation,  ATTR_AREA1_BOROUGH,            tourLocation.borough);
            saveValue(xmlLocation,  ATTR_AREA1_SUBURB,             tourLocation.suburb);
            saveValue(xmlLocation,  ATTR_AREA1_SUBDIVISION,        tourLocation.subdivision);

            saveValue(xmlLocation,  ATTR_AREA2_HAMLET,             tourLocation.hamlet);
            saveValue(xmlLocation,  ATTR_AREA2_CROFT,              tourLocation.croft);
            saveValue(xmlLocation,  ATTR_AREA2_ISOLATED_DWELLING,  tourLocation.isolated_dwelling);

            saveValue(xmlLocation,  ATTR_AREA3_NEIGHBOURHOOD,      tourLocation.neighbourhood);
            saveValue(xmlLocation,  ATTR_AREA3_ALLOTMENTS,         tourLocation.allotments);
            saveValue(xmlLocation,  ATTR_AREA3_QUARTER,            tourLocation.quarter);

            saveValue(xmlLocation,  ATTR_AREA4_CITY_BLOCK,         tourLocation.city_block);
            saveValue(xmlLocation,  ATTR_AREA4_RESIDETIAL,         tourLocation.residential);
            saveValue(xmlLocation,  ATTR_AREA4_FARM,               tourLocation.farm);
            saveValue(xmlLocation,  ATTR_AREA4_FARMYARD,           tourLocation.farmyard);
            saveValue(xmlLocation,  ATTR_AREA4_INDUSTRIAL,         tourLocation.industrial);
            saveValue(xmlLocation,  ATTR_AREA4_COMMERCIAL,         tourLocation.commercial);
            saveValue(xmlLocation,  ATTR_AREA4_RETAIL,             tourLocation.retail);

            saveValue(xmlLocation,  ATTR_OTHER_AERIALWAY,          tourLocation.aerialway);
            saveValue(xmlLocation,  ATTR_OTHER_AEROWAY,            tourLocation.aeroway);
            saveValue(xmlLocation,  ATTR_OTHER_AMENITY,            tourLocation.amenity);
            saveValue(xmlLocation,  ATTR_OTHER_BOUNDARY,           tourLocation.boundary);
            saveValue(xmlLocation,  ATTR_OTHER_BRIDGE,             tourLocation.bridge);
            saveValue(xmlLocation,  ATTR_OTHER_CLUB,               tourLocation.club);
            saveValue(xmlLocation,  ATTR_OTHER_CRAFT,              tourLocation.craft);
            saveValue(xmlLocation,  ATTR_OTHER_EMERGENCY,          tourLocation.emergency);
            saveValue(xmlLocation,  ATTR_OTHER_HISTORIC,           tourLocation.historic);
            saveValue(xmlLocation,  ATTR_OTHER_LANDUSE,            tourLocation.landuse);
            saveValue(xmlLocation,  ATTR_OTHER_LEISURE,            tourLocation.leisure);
            saveValue(xmlLocation,  ATTR_OTHER_MAN_MADE,           tourLocation.man_made);
            saveValue(xmlLocation,  ATTR_OTHER_MILITARY,           tourLocation.military);
            saveValue(xmlLocation,  ATTR_OTHER_MOUNTAIN_PASS,      tourLocation.mountain_pass);
            saveValue(xmlLocation,  ATTR_OTHER_NATURAL2,           tourLocation.natural2);
            saveValue(xmlLocation,  ATTR_OTHER_OFFICE,             tourLocation.office);
            saveValue(xmlLocation,  ATTR_OTHER_PLACE,              tourLocation.place);
            saveValue(xmlLocation,  ATTR_OTHER_RAILWAY,            tourLocation.railway);
            saveValue(xmlLocation,  ATTR_OTHER_SHOP,               tourLocation.shop);
            saveValue(xmlLocation,  ATTR_OTHER_TOURISM,            tourLocation.tourism);
            saveValue(xmlLocation,  ATTR_OTHER_TUNNEL,             tourLocation.tunnel);
            saveValue(xmlLocation,  ATTR_OTHER_WATERWAY,           tourLocation.waterway);

            xmlLocation.putInteger(ATTR_LATITUDE_E6,                          tourLocation.latitudeE6);
            xmlLocation.putInteger(ATTR_LATITUDE_E6_NORMALIZED,               tourLocation.latitudeE6_Normalized);

            xmlLocation.putInteger(ATTR_LATITUDE_MIN_E6_NORMALIZED,           tourLocation.latitudeMinE6_Normalized);
            xmlLocation.putInteger(ATTR_LATITUDE_MAX_E6_NORMALIZED,           tourLocation.latitudeMaxE6_Normalized);
            xmlLocation.putInteger(ATTR_LATITUDE_MIN_E6_RESIZED_NORMALIZED,   tourLocation.latitudeMinE6_Resized_Normalized);
            xmlLocation.putInteger(ATTR_LATITUDE_MAX_E6_RESIZED_NORMALIZED,   tourLocation.latitudeMaxE6_Resized_Normalized);

            xmlLocation.putInteger(ATTR_LONGITUDE_E6,                         tourLocation.longitudeE6);
            xmlLocation.putInteger(ATTR_LONGITUDE_E6_NORMALIZED,              tourLocation.longitudeE6_Normalized);

            xmlLocation.putInteger(ATTR_LONGITUDE_MIN_E6_NORMALIZED,          tourLocation.longitudeMinE6_Normalized);
            xmlLocation.putInteger(ATTR_LONGITUDE_MAX_E6_NORMALIZED,          tourLocation.longitudeMaxE6_Normalized);
            xmlLocation.putInteger(ATTR_LONGITUDE_MIN_E6_RESIZED_NORMALIZED,  tourLocation.longitudeMinE6_Resized_Normalized);
            xmlLocation.putInteger(ATTR_LONGITUDE_MAX_E6_RESIZED_NORMALIZED,  tourLocation.longitudeMaxE6_Resized_Normalized);

// SET_FORMATTING_ON
         }
      }
   }

   private static void saveValue(final IMemento xmlLocation, final String key, final String value) {

      if (value != null && value.length() > 0) {

         xmlLocation.putString(key, value);
      }
   }

   public static void setLocationRequestZoomlevel(final int locationRequestZoomlevel) {

      _locationRequestZoomlevel = locationRequestZoomlevel;
   }

   public static void setMapLocationSlideout(final SlideoutMapLocation mapLocationSlideout) {

      _mapLocationSlideout = mapLocationSlideout;
   }
}
