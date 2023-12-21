/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.tourbook.application.ApplicationVersion;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.Messages;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DLItem;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Source: <a href=
 * "https://nominatim.org/release-docs/develop/api/Reverse/">https://nominatim.org/release-docs/develop/api/Reverse/</a>
 * <p>
 *
 * The main format of the reverse API is
 * <a href=
 * "https://nominatim.openstreetmap.org/reverse?lat=<value>&lon=<value>&<params>">https://nominatim.openstreetmap.org/reverse?lat=<value>&lon=<value>&<params></a>
 * <p>
 *
 * Limits and download policy:
 * <a href=
 * "https://operations.osmfoundation.org/policies/nominatim/">https://operations.osmfoundation.org/policies/nominatim/</a>
 * <p>
 *
 * Requested feature: <a href=
 * "https://github.com/mytourbook/mytourbook/issues/878">https://github.com/mytourbook/mytourbook/issues/878</a>
 * <p>
 */
public class TourLocationManager {

   private static final char   NL                              = UI.NEW_LINE;

   private static final String SYS_PROP__LOG_ADDRESS_RETRIEVAL = "logAddressRetrieval";                                      //$NON-NLS-1$
   private static boolean      _isLogging_AddressRetrieval     = System.getProperty(SYS_PROP__LOG_ADDRESS_RETRIEVAL) != null;

   static {

      if (_isLogging_AddressRetrieval) {
         Util.logSystemProperty_IsEnabled(TourManager.class, SYS_PROP__LOG_ADDRESS_RETRIEVAL, "OSM address retrieval is logged"); //$NON-NLS-1$
      }
   }

   private static final Bundle                    _bundle                     = TourbookPlugin.getDefault().getBundle();
   private static final IPath                     _stateLocation              = Platform.getStateLocation(_bundle);

   private static final String                    TOUR_LOCATION_FILE_NAME     = "tour-location.xml";                                        //$NON-NLS-1$
   private static final int                       TOUR_LOCATION_VERSION       = 1;

   private static final String                    TAG_ROOT                    = "TourLocationProfiles";                                     //$NON-NLS-1$
   private static final String                    TAG_PROFILE                 = "Profile";                                                  //$NON-NLS-1$
   private static final String                    TAG_PARTS                   = "Parts";                                                    //$NON-NLS-1$
   private static final String                    TAG_PART                    = "Part";                                                     //$NON-NLS-1$

   private static final String                    ATTR_IS_DEFAULT_PROFILE     = "isDefaultProfile";                                         //$NON-NLS-1$
   private static final String                    ATTR_NAME                   = "name";                                                     //$NON-NLS-1$
   private static final String                    ATTR_PROFILE_NAME           = "profileName";                                              //$NON-NLS-1$
   private static final String                    ATTR_TOUR_LOCATION_VERSION  = "tourLocationVersion";                                      //$NON-NLS-1$
   private static final String                    ATTR_ZOOMLEVEL              = "zoomlevel";                                                //$NON-NLS-1$

   static final String                            KEY_LOCATION_PART_ID        = "KEY_LOCATION_PART_ID";                                     //$NON-NLS-1$
   static final String                            KEY_IS_NOT_AVAILABLE        = "KEY_IS_NOT_AVAILABLE";                                     //$NON-NLS-1$

   private static final String                    COUNTRY_CODE_US             = "us";                                                       //$NON-NLS-1$

   private static final String                    SUB_TASK_RETRIEVE_LOCATIONS = Messages.Tour_Location_Task_RetrievingTourLocations_Subtask;
   private static final String                    SUB_TASK_NTH_OF_ALL         = "%d / %d";                                                  //$NON-NLS-1$

   private static final String                    _userAgent                  = "MyTourbook/" + ApplicationVersion.getVersionSimple();      //$NON-NLS-1$

   private static final HttpClient                _httpClient                 = HttpClient

         .newBuilder()
         .connectTimeout(Duration.ofSeconds(10))
         .build();

   private static final Duration                  _httpTimeoutDuration        = Duration.ofSeconds(5);

   private static final StringBuilder             _displayNameBuffer          = new StringBuilder();
   private static final Set<String>               _usedDisplayNames           = new HashSet<>();

   /**
    * Contains all available profiles
    */
   private static final List<TourLocationProfile> _allLocationProfiles        = new ArrayList<>();
   private static TourLocationProfile             _defaultProfile;

   private static long                            _lastRetrievalTimeMS;

// SET_FORMATTING_OFF

   /**
    * Zoom address detail
    *
    * 3 country
    * 5 state
    * 8 county
    * 10 city
    * 12 town / borough
    * 13 village / suburb
    * 14 neighbourhood
    * 15 any settlement
    * 16 major streets
    * 17 major and minor streets
    * 18 building
    */
   static Zoomlevel[]         ALL_ZOOM_LEVEL              = {

         new Zoomlevel( 3, Messages.Tour_Location_Zoomlevel_03_Country), //            0
         new Zoomlevel( 5, Messages.Tour_Location_Zoomlevel_05_State), //              1
         new Zoomlevel( 8, Messages.Tour_Location_Zoomlevel_08_County), //             2
         new Zoomlevel(10, Messages.Tour_Location_Zoomlevel_10_City), //               3
         new Zoomlevel(12, Messages.Tour_Location_Zoomlevel_12_TownBorough), //        4
         new Zoomlevel(13, Messages.Tour_Location_Zoomlevel_13_VillageSuburb), //      5 DEFAULT
         new Zoomlevel(14, Messages.Tour_Location_Zoomlevel_14_Neighbourhood), //      6
         new Zoomlevel(15, Messages.Tour_Location_Zoomlevel_15_AnySettlement), //      7
         new Zoomlevel(16, Messages.Tour_Location_Zoomlevel_16_MajorStreets), //       8
         new Zoomlevel(17, Messages.Tour_Location_Zoomlevel_17_MajorMinorStreets), //  9
         new Zoomlevel(18, Messages.Tour_Location_Zoomlevel_18_Building) //            10
   };

   public static Zoomlevel    DEFAULT_ZOOM_LEVEL          = ALL_ZOOM_LEVEL[5];
   public static int          DEFAULT_ZOOM_LEVEL_VALUE    = DEFAULT_ZOOM_LEVEL.zoomlevel;

   public static Map<LocationPartID, String> ALL_LOCATION_PART_AND_LABEL = Map.ofEntries(

         Map.entry(LocationPartID.OSM_DEFAULT_NAME,                  Messages.Tour_Location_Part_OsmDefaultName),
         Map.entry(LocationPartID.OSM_NAME,                          Messages.Tour_Location_Part_OsmName),

         Map.entry(LocationPartID.CUSTOM_STREET_WITH_HOUSE_NUMBER,   Messages.Tour_Location_Part_StreeWithHouseNumber),

         // this is a computed part
         Map.entry(LocationPartID.settlementSmall,                   UI.SYMBOL_STAR + UI.SPACE + Messages.Tour_Location_Part_SettlementSmall),
         Map.entry(LocationPartID.settlementLarge,                   UI.SYMBOL_STAR + UI.SPACE + Messages.Tour_Location_Part_SettlementLarge),


         Map.entry(LocationPartID.continent,                         Messages.Tour_Location_Part_Continent),
         Map.entry(LocationPartID.country,                           Messages.Tour_Location_Part_Country),
         Map.entry(LocationPartID.country_code,                      Messages.Tour_Location_Part_CountryCode),

         Map.entry(LocationPartID.region,                            Messages.Tour_Location_Part_Region),
         Map.entry(LocationPartID.state,                             Messages.Tour_Location_Part_State),
         Map.entry(LocationPartID.state_district,                    Messages.Tour_Location_Part_StateDistrict),
         Map.entry(LocationPartID.county,                            Messages.Tour_Location_Part_County),

         Map.entry(LocationPartID.municipality,                      Messages.Tour_Location_Part_Municipality),
         Map.entry(LocationPartID.city,                              Messages.Tour_Location_Part_City),
         Map.entry(LocationPartID.town,                              Messages.Tour_Location_Part_Town),
         Map.entry(LocationPartID.village,                           Messages.Tour_Location_Part_Village),

         Map.entry(LocationPartID.city_district,                     Messages.Tour_Location_Part_CityDistrict),
         Map.entry(LocationPartID.district,                          Messages.Tour_Location_Part_District),
         Map.entry(LocationPartID.borough,                           Messages.Tour_Location_Part_Borough),
         Map.entry(LocationPartID.suburb,                            Messages.Tour_Location_Part_Suburb),
         Map.entry(LocationPartID.subdivision,                       Messages.Tour_Location_Part_Subdivision),

         Map.entry(LocationPartID.hamlet,                            Messages.Tour_Location_Part_Hamlet),
         Map.entry(LocationPartID.croft,                             Messages.Tour_Location_Part_Croft),
         Map.entry(LocationPartID.isolated_dwelling,                 Messages.Tour_Location_Part_IsolatedDwelling),

         Map.entry(LocationPartID.neighbourhood,                     Messages.Tour_Location_Part_Neighbourhood),
         Map.entry(LocationPartID.allotments,                        Messages.Tour_Location_Part_Allotments),
         Map.entry(LocationPartID.quarter,                           Messages.Tour_Location_Part_Quarter),

         Map.entry(LocationPartID.city_block,                        Messages.Tour_Location_Part_CityBlock),
         Map.entry(LocationPartID.residential,                       Messages.Tour_Location_Part_Residential),
         Map.entry(LocationPartID.farm,                              Messages.Tour_Location_Part_Farm),
         Map.entry(LocationPartID.farmyard,                          Messages.Tour_Location_Part_Farmyard),
         Map.entry(LocationPartID.industrial,                        Messages.Tour_Location_Part_Industrial),
         Map.entry(LocationPartID.commercial,                        Messages.Tour_Location_Part_Commercial),
         Map.entry(LocationPartID.retail,                            Messages.Tour_Location_Part_Retail),

         Map.entry(LocationPartID.road,                              Messages.Tour_Location_Part_Road),

         Map.entry(LocationPartID.house_name,                        Messages.Tour_Location_Part_HouseName),
         Map.entry(LocationPartID.house_number,                      Messages.Tour_Location_Part_HouseNumber),

         Map.entry(LocationPartID.aerialway,                         Messages.Tour_Location_Part_Aerialway),
         Map.entry(LocationPartID.aeroway,                           Messages.Tour_Location_Part_Aeroway),
         Map.entry(LocationPartID.amenity,                           Messages.Tour_Location_Part_Amenity),
         Map.entry(LocationPartID.boundary,                          Messages.Tour_Location_Part_Boundary),
         Map.entry(LocationPartID.bridge,                            Messages.Tour_Location_Part_Bridge),
         Map.entry(LocationPartID.club,                              Messages.Tour_Location_Part_Club),
         Map.entry(LocationPartID.craft,                             Messages.Tour_Location_Part_Craft),
         Map.entry(LocationPartID.emergency,                         Messages.Tour_Location_Part_Emergency),
         Map.entry(LocationPartID.historic,                          Messages.Tour_Location_Part_Historic),
         Map.entry(LocationPartID.landuse,                           Messages.Tour_Location_Part_Landuse),
         Map.entry(LocationPartID.leisure,                           Messages.Tour_Location_Part_Leisure),
         Map.entry(LocationPartID.man_made,                          Messages.Tour_Location_Part_ManMade),
         Map.entry(LocationPartID.military,                          Messages.Tour_Location_Part_Military),
         Map.entry(LocationPartID.mountain_pass,                     Messages.Tour_Location_Part_MountainPass),
         Map.entry(LocationPartID.natural2,                          Messages.Tour_Location_Part_Natural),
         Map.entry(LocationPartID.office,                            Messages.Tour_Location_Part_Office),
         Map.entry(LocationPartID.place,                             Messages.Tour_Location_Part_Place),
         Map.entry(LocationPartID.railway,                           Messages.Tour_Location_Part_Railway),
         Map.entry(LocationPartID.shop,                              Messages.Tour_Location_Part_Shop),
         Map.entry(LocationPartID.tourism,                           Messages.Tour_Location_Part_Tourism),
         Map.entry(LocationPartID.tunnel,                            Messages.Tour_Location_Part_Tunnel),
         Map.entry(LocationPartID.waterway,                          Messages.Tour_Location_Part_Waterway),

         Map.entry(LocationPartID.postcode,                          Messages.Tour_Location_Part_Postcode)

      );

// SET_FORMATTING_ON

   static class Zoomlevel {

      int    zoomlevel;
      String label;

      public Zoomlevel(final int zoomlevel, final String label) {

         this.zoomlevel = zoomlevel;
         this.label = label;
      }
   }

   /**
    * Append start/end part to the existing start/end places
    *
    * @param allTourData
    * @param partID_Start
    * @param partID_End
    * @param isSetStartLocation
    * @param isSetEndLocation
    */
   public static void appendLocationPart(final ArrayList<TourData> allTourData,
                                         final LocationPartID partID_Start,
                                         final LocationPartID partID_End,
                                         final boolean isSetStartLocation,
                                         final boolean isSetEndLocation) {

      final ArrayList<TourData> modifiedTours = new ArrayList<>();

      for (final TourData tourData : allTourData) {

         final TourLocation tourLocationStart = tourData.getTourLocationStart();
         final TourLocation tourLocationEnd = tourData.getTourLocationEnd();

         String startPart = null;
         String endPart = null;

         final boolean isStartLocationAvailable = tourLocationStart != null;
         final boolean isEndLocationAvailable = tourLocationEnd != null;

         if (isStartLocationAvailable) {
            startPart = tourLocationStart.getPartValue(partID_Start);
         }

         if (isEndLocationAvailable) {
            endPart = tourLocationEnd.getPartValue(partID_End);
         }

         boolean isModified = false;

         if (isSetStartLocation && startPart != null) {

            String tourStartPlace = tourData.getTourStartPlace();

            if (tourStartPlace.length() > 0) {

               tourStartPlace += UI.COMMA_SPACE;
            }

            tourStartPlace += startPart;

            tourData.setTourStartPlace(tourStartPlace);

            isModified = true;
         }

         if (isSetEndLocation && endPart != null) {

            String tourEndPlace = tourData.getTourEndPlace();

            if (tourEndPlace.length() > 0) {

               tourEndPlace += UI.COMMA_SPACE;
            }

            tourEndPlace += endPart;

            tourData.setTourEndPlace(tourEndPlace);

            isModified = true;
         }

         if (isModified) {

            modifiedTours.add(tourData);
         }
      }

      if (modifiedTours.size() > 0) {

         TourManager.saveModifiedTours(modifiedTours);
      }
   }

   /**
    * Append text to the display name in {@link #_displayNameBuffer} but prevent duplicate part
    * labels
    *
    * @param text
    */
   private static void appendPart(final String text) {

      if (StringUtils.isNullOrEmpty(text)) {
         return;
      }

      // prevent to show duplicated fields, this can happen when the "name" field contains also e.g. the road name
      if (_usedDisplayNames.contains(text)) {
         return;
      }

      if (_displayNameBuffer.length() > 0) {
         _displayNameBuffer.append(UI.SYMBOL_COMMA + UI.SPACE);
      }

      _displayNameBuffer.append(text);
      _usedDisplayNames.add(text);
   }

   private static void createDefaultProfiles() {

      final TourLocationProfile[] allProfiles = {

// SET_FORMATTING_OFF

         new TourLocationProfile("0 : Name",   18,

            LocationPartID.OSM_NAME),

         new TourLocationProfile("1 : Street + House #",   18,

            LocationPartID.CUSTOM_STREET_WITH_HOUSE_NUMBER),

         new TourLocationProfile("A : State",   18,

            LocationPartID.state,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("B : City",   10,

            LocationPartID.settlementLarge,
            LocationPartID.state,
            LocationPartID.city,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("C : Town / Borough",   12,

            LocationPartID.settlementLarge,
            LocationPartID.OSM_NAME,
            LocationPartID.town),

         new TourLocationProfile("D : Village / Suburb",   13,

            LocationPartID.settlementLarge,
            LocationPartID.suburb,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("E : Neighbourhood",   14,

            LocationPartID.settlementLarge,
            LocationPartID.neighbourhood,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("F : Settlement",   15,

            LocationPartID.settlementLarge,
            LocationPartID.settlementSmall,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("G : Major Street",   16,

            LocationPartID.settlementLarge,
            LocationPartID.settlementSmall,
            LocationPartID.road,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("H : Minor Street",   17,

            LocationPartID.settlementLarge,
            LocationPartID.settlementSmall,
            LocationPartID.road,
            LocationPartID.OSM_NAME),

         new TourLocationProfile("I : Building",   18,

            LocationPartID.CUSTOM_STREET_WITH_HOUSE_NUMBER,
            LocationPartID.settlementSmall,
            LocationPartID.OSM_NAME)

// SET_FORMATTING_ON

      };

      _allLocationProfiles.addAll(Arrays.asList(allProfiles));

      _defaultProfile = allProfiles[4];
   }

   /**
    * Generate Java code to easily define the default profiles
    */
   public static void createDefaultProfiles_JavaCode() {

      final StringBuilder sb = new StringBuilder();

      sb.append("      final TourLocationProfile[] allProfiles = {"); //$NON-NLS-1$
      sb.append(NL);
      sb.append(NL);

      sb.append("// SET_FORMATTING_OFF"); //$NON-NLS-1$
      sb.append(NL);
      sb.append(NL);

      final int numProfiles = _allLocationProfiles.size();
      for (int profileIndex = 0; profileIndex < numProfiles; profileIndex++) {

         final TourLocationProfile profile = _allLocationProfiles.get(profileIndex);

         sb.append("         new TourLocationProfile(\"" + profile.name + "\",   " + profile.zoomlevel + ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         sb.append(NL);

         final List<LocationPartID> allParts = profile.allParts;
         final int numParts = allParts.size();

         for (int partIndex = 0; partIndex < numParts; partIndex++) {

            sb.append(NL);

            final LocationPartID partID = allParts.get(partIndex);

            sb.append("            LocationPartID." + partID.name()); //$NON-NLS-1$

            if (partIndex < numParts - 1) {
               sb.append(","); //$NON-NLS-1$
            }
         }

         sb.append(")"); //$NON-NLS-1$

         if (profileIndex < numProfiles - 1) {
            sb.append(","); //$NON-NLS-1$
         }

         sb.append(NL);
         sb.append(NL);
      }

      sb.append("// SET_FORMATTING_ON"); //$NON-NLS-1$
      sb.append(NL);
      sb.append(NL);

      sb.append("      };"); //$NON-NLS-1$
      sb.append(NL);
      sb.append(NL);

      System.out.println(sb.toString());
   }

   public static String createJoinedPartNames(final TourLocationProfile profile, final String delimiter) {

      final List<LocationPartID> allParts = profile.allParts;

      final String joinedParts = allParts.stream()

            .map(locationPart -> {

               String label;

               switch (locationPart) {
               case OSM_DEFAULT_NAME:
               case OSM_NAME:
               case CUSTOM_STREET_WITH_HOUSE_NUMBER:

                  label = TourLocationManager.createPartName_Combined(locationPart);
                  break;

               default:

                  label = TourLocationManager.ALL_LOCATION_PART_AND_LABEL.get(locationPart);
                  break;
               }

               return label;
            })

            .collect(Collectors.joining(delimiter));

      return joinedParts;
   }

   public static String createLocationDisplayName(final List<MT_DLItem> allSelectedItems) {

      // reset buffers
      _displayNameBuffer.setLength(0);
      _usedDisplayNames.clear();

      for (final MT_DLItem partItem : allSelectedItems) {

         final Boolean isNotAvailable = (Boolean) partItem.getData(KEY_IS_NOT_AVAILABLE);

         if (isNotAvailable != null && isNotAvailable) {

            /*
             * Skip parts which are not available in the downloaded address data, this happens
             * when a profile was created with this part
             */

            continue;
         }

         appendPart(partItem.getText2());
      }

      return _displayNameBuffer.toString();
   }

   /**
    * Creates the location name from different name parts.
    *
    * @return Returns an empty string when a display name not available
    */
   public static String createLocationDisplayName(final TourLocation tourLocation) {

      if (tourLocation == null) {

         return UI.EMPTY_STRING;
      }

      if (getDefaultProfile() != null) {

         // create name from a profile

         return createLocationDisplayName(tourLocation, getDefaultProfile());

      } else {

         // use osm default name

         return tourLocation.display_name;
      }
   }

   /**
    * Creates location display name by applying the provided profile
    *
    * @param tourLocation
    *           OSM location data
    * @param profile
    *
    * @return
    */
   public static String createLocationDisplayName(final TourLocation tourLocation,
                                                  final TourLocationProfile profile) {

      // reset buffers
      _displayNameBuffer.setLength(0);
      _usedDisplayNames.clear();

      for (final LocationPartID locationPart : profile.allParts) {

         switch (locationPart) {

// SET_FORMATTING_OFF

         case OSM_DEFAULT_NAME:                 appendPart(tourLocation.display_name);                         break;
         case OSM_NAME:                         appendPart(tourLocation.name);                                 break;

         case CUSTOM_STREET_WITH_HOUSE_NUMBER:  appendPart(getCombined_StreetWithHouseNumber(tourLocation));   break;

         // ignore
         case NONE:                             break;
         case ISO3166_2_lvl4:                   break;

// SET_FORMATTING_ON

         default:

            /*
             * Append all other fields
             */
            try {

               final String fieldName = locationPart.name();
               final Field addressField = tourLocation.getClass().getField(fieldName);

               final Object fieldValue = addressField.get(tourLocation);

               if (fieldValue instanceof final String textValue) {

                  appendPart(textValue);
               }

            } catch (NoSuchFieldException
                  | SecurityException
                  | IllegalArgumentException
                  | IllegalAccessException e) {

               StatusUtil.log(e);
            }

            break;
         }
      }

      return _displayNameBuffer.toString();
   }

   static String createPartName_Combined(final LocationPartID locationPart) {

      final String label = ALL_LOCATION_PART_AND_LABEL.get(locationPart);

      return UI.SYMBOL_STAR + UI.SPACE + label;
   }

   static String createPartName_NotAvailable(final LocationPartID locationPart) {

      final String label = ALL_LOCATION_PART_AND_LABEL.get(locationPart);

      return UI.SYMBOL_STAR + UI.SYMBOL_STAR + UI.SPACE + label;
   }

   /**
    * Create a {@link TourLocation}
    *
    * @param osmLocation
    * @param latitude
    * @param longitude
    * @param zoomlevel
    *
    * @return
    */
   private static TourLocation createTourLocation(final OSMLocation osmLocation,
                                                  final double latitude,
                                                  final double longitude,
                                                  final int zoomlevel) {

      if (osmLocation == null) {
         return null;
      }

      final OSMAddress osmAddress = osmLocation.address;

      // "boundingbox":
      // [
      //    "47.1159171",
      //    "47.1163167",
      //    "7.9895150",
      //    "7.9897759"
      // ]
      final double[] boundingbox = osmLocation.boundingbox;
      if (boundingbox == null || boundingbox.length != 4) {
         return null;
      }

      final int[] boundingBoxE6 = Util.convertDoubleSeries_ToE6(boundingbox);

      // convert possible negative values into positive values, it's easier to math it
      final int latitudeMinE6_Normalized = boundingBoxE6[0] + 90_000_000;
      final int latitudeMaxE6_Normalized = boundingBoxE6[1] + 90_000_000;
      final int longitudeMinE6_Normalized = boundingBoxE6[2] + 180_000_000;
      final int longitudeMaxE6_Normalized = boundingBoxE6[3] + 180_000_000;

      final TourLocation tourLocation = new TourLocation(latitude, longitude);

// SET_FORMATTING_OFF

      tourLocation.latitudeMinE6_Normalized  = tourLocation.latitudeMinE6_Resized_Normalized  = latitudeMinE6_Normalized;
      tourLocation.latitudeMaxE6_Normalized  = tourLocation.latitudeMaxE6_Resized_Normalized  = latitudeMaxE6_Normalized;
      tourLocation.longitudeMinE6_Normalized = tourLocation.longitudeMinE6_Resized_Normalized = longitudeMinE6_Normalized;
      tourLocation.longitudeMaxE6_Normalized = tourLocation.longitudeMaxE6_Resized_Normalized = longitudeMaxE6_Normalized;

      tourLocation.name                      = validString(osmLocation.name);
      tourLocation.display_name              = validString(osmLocation.display_name);
      tourLocation.zoomlevel                 = zoomlevel;

      if (osmAddress != null) {

         tourLocation.continent              = validString(osmAddress.continent);
         tourLocation.country                = validString(osmAddress.country);
         tourLocation.country_code           = validString(osmAddress.country_code);

         tourLocation.region                 = validString(osmAddress.region);
         tourLocation.state                  = validString(osmAddress.state);
         tourLocation.state_district         = validString(osmAddress.state_district);
         tourLocation.county                 = validString(osmAddress.county);

         tourLocation.municipality           = validString(osmAddress.municipality);
         tourLocation.city                   = validString(osmAddress.city);
         tourLocation.town                   = validString(osmAddress.town);
         tourLocation.village                = validString(osmAddress.village);
         tourLocation.postcode               = validString(osmAddress.postcode);

         tourLocation.road                   = validString(osmAddress.road);
         tourLocation.house_number           = validString(osmAddress.house_number);
         tourLocation.house_name             = validString(osmAddress.house_name);

         // Area I
         tourLocation.city_district          = validString(osmAddress.city_district);
         tourLocation.district               = validString(osmAddress.district);
         tourLocation.borough                = validString(osmAddress.borough);
         tourLocation.suburb                 = validString(osmAddress.suburb);
         tourLocation.subdivision            = validString(osmAddress.subdivision);

         // Area II
         tourLocation.hamlet                 = validString(osmAddress.hamlet);
         tourLocation.croft                  = validString(osmAddress.croft);
         tourLocation.isolated_dwelling      = validString(osmAddress.isolated_dwelling);

         // Area III
         tourLocation.neighbourhood          = validString(osmAddress.neighbourhood);
         tourLocation.allotments             = validString(osmAddress.allotments);
         tourLocation.quarter                = validString(osmAddress.quarter);

         // Area IV
         tourLocation.city_block             = validString(osmAddress.city_block);
         tourLocation.residential            = validString(osmAddress.residential);
         tourLocation.farm                   = validString(osmAddress.farm);
         tourLocation.farmyard               = validString(osmAddress.farmyard);
         tourLocation.industrial             = validString(osmAddress.industrial);
         tourLocation.commercial             = validString(osmAddress.commercial);
         tourLocation.retail                 = validString(osmAddress.retail);

         tourLocation.aerialway              = validString(osmAddress.aerialway);
         tourLocation.aeroway                = validString(osmAddress.aeroway);
         tourLocation.amenity                = validString(osmAddress.amenity);
         tourLocation.boundary               = validString(osmAddress.boundary);
         tourLocation.bridge                 = validString(osmAddress.bridge);
         tourLocation.club                   = validString(osmAddress.club);
         tourLocation.craft                  = validString(osmAddress.craft);
         tourLocation.emergency              = validString(osmAddress.emergency);
         tourLocation.historic               = validString(osmAddress.historic);
         tourLocation.landuse                = validString(osmAddress.landuse);
         tourLocation.leisure                = validString(osmAddress.leisure);
         tourLocation.man_made               = validString(osmAddress.man_made);
         tourLocation.military               = validString(osmAddress.military);
         tourLocation.mountain_pass          = validString(osmAddress.mountain_pass);
         tourLocation.natural2               = validString(osmAddress.natural2);
         tourLocation.office                 = validString(osmAddress.office);
         tourLocation.place                  = validString(osmAddress.place);
         tourLocation.railway                = validString(osmAddress.railway);
         tourLocation.shop                   = validString(osmAddress.shop);
         tourLocation.tourism                = validString(osmAddress.tourism);
         tourLocation.tunnel                 = validString(osmAddress.tunnel);
         tourLocation.waterway               = validString(osmAddress.waterway);
      }

// SET_FORMATTING_ON

      tourLocation.setTransientValues();

      return tourLocation;
   }

   /**
    * Firstly the locations are deleted and the contained tours are retrieved again
    *
    * @param allLocations
    * @param isSkipFirstLocation
    *
    * @return
    */
   public static boolean deleteAndReapply(final List<TourLocation> allLocations,
                                          final boolean isSkipFirstLocation) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return false;
      }

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();
      if (defaultProfile == null) {
         return false;
      }

      List<TourLocation> allDelReapplyLocations;

      if (isSkipFirstLocation) {

         final int numLocations = allLocations.size();

         if (numLocations < 2) {

            return false;
         }

         // skip 1st location

         allDelReapplyLocations = allLocations.subList(1, numLocations);

      } else {

         allDelReapplyLocations = allLocations;
      }

      // get all tour IDs before locations are deleted !!!
      final ArrayList<Long> allTourIds = getToursWithLocations(allDelReapplyLocations);

      if (deleteTourLocations(allDelReapplyLocations) == false) {
         return false;
      }

      final List<TourData> allTourData = new ArrayList<>();

      TourManager.loadTourData(allTourIds, allTourData, false);

      setTourLocations(allTourData,

            defaultProfile,

            true, // is set start
            true, // is set end

            true // isForceReloadLocation
      );

      return true;
   }

   /**
    * @param allLocations
    *
    * @return Returns <code>true</code> when tour locations were deleted
    */
   public static boolean deleteTourLocations(final List<TourLocation> allLocations) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return false;
      }

      final ArrayList<Long> allTourIds = getToursWithLocations(allLocations);

      String dialogMessage;
      String actionDeleteTags;

      final int numTours = allTourIds.size();
      final int numLocations = allLocations.size();

      if (numLocations == 1) {

         // delete one location

         dialogMessage = Messages.Tour_Location_Dialog_DeleteLocation_Message.formatted(

               allLocations.get(0).display_name,
               numTours);

         actionDeleteTags = Messages.Tour_Location_Action_DeleteLocation;

      } else {

         // remove multiple locations

         dialogMessage = Messages.Tour_Location_Dialog_DeleteLocations_Message.formatted(

               numLocations,
               numTours);

         actionDeleteTags = Messages.Tour_Location_Action_DeleteLocations;
      }

      final Display display = Display.getDefault();

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            Messages.Tour_Location_Dialog_DeleteLocation_Title,
            null,
            dialogMessage,
            MessageDialog.QUESTION,
            new String[] {
                  actionDeleteTags,
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean[] returnValue = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (deleteTourLocations_10(allLocations)) {

               TourManager.getInstance().clearTourDataCache();

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean deleteTourLocations_10(final List<TourLocation> allLocations) {

      boolean returnResult = false;

      PreparedStatement prepStmt_TourData_Start = null;
      PreparedStatement prepStmt_TourData_End = null;
      PreparedStatement prepStmt_TourLocation = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove locations from TOURDATA
         final String sqlStartLocation = UI.EMPTY_STRING

               + "UPDATE " + TourDatabase.TABLE_TOUR_DATA + NL //          //$NON-NLS-1$

               + "SET" + NL //                                             //$NON-NLS-1$

               + " tourStartPlace               = NULL," + NL //           //$NON-NLS-1$
               + " tourLocationStart_LocationID = NULL" + NL //            //$NON-NLS-1$

               + "WHERE tourLocationStart_LocationID = ?" + NL //       1  //$NON-NLS-1$
         ;

         final String sqlEndLocation = UI.EMPTY_STRING

               + "UPDATE " + TourDatabase.TABLE_TOUR_DATA + NL //          //$NON-NLS-1$

               + "SET" + NL //                                             //$NON-NLS-1$

               + " tourEndPlace               = NULL," + NL //             //$NON-NLS-1$
               + " tourLocationEnd_LocationID = NULL" + NL //              //$NON-NLS-1$

               + "WHERE tourLocationEnd_LocationID = ?" + NL //         1  //$NON-NLS-1$
         ;

         // remove location from TOURLOCATION
         final String sqlTourLocation = UI.EMPTY_STRING

               + "DELETE" + NL //                                          //$NON-NLS-1$

               + " FROM " + TourDatabase.TABLE_TOUR_LOCATION + NL //       //$NON-NLS-1$

               + " WHERE locationID = ?"; //                            1  //$NON-NLS-1$

         prepStmt_TourData_Start = conn.prepareStatement(sqlStartLocation);
         prepStmt_TourData_End = conn.prepareStatement(sqlEndLocation);
         prepStmt_TourLocation = conn.prepareStatement(sqlTourLocation);

         int[] returnValue_TourData_Start;
         int[] returnValue_TourData_End;
         int[] returnValue_TourLocation;

         conn.setAutoCommit(false);
         {
            for (final TourLocation location : allLocations) {

               final long locationId = location.getLocationId();

               prepStmt_TourData_Start.setLong(1, locationId);
               prepStmt_TourData_Start.addBatch();

               prepStmt_TourData_End.setLong(1, locationId);
               prepStmt_TourData_End.addBatch();

               prepStmt_TourLocation.setLong(1, locationId);
               prepStmt_TourLocation.addBatch();
            }

            returnValue_TourData_Start = prepStmt_TourData_Start.executeBatch();
            returnValue_TourData_End = prepStmt_TourData_End.executeBatch();
            returnValue_TourLocation = prepStmt_TourLocation.executeBatch();
         }
         conn.commit();

         // log result
         TourLogManager.showLogView(AutoOpenEvent.DELETE_SOMETHING);

         for (int locationIndex = 0; locationIndex < allLocations.size(); locationIndex++) {

            // Location is deleted from %d start locations, %d end locations, %d location - "%s"

            TourLogManager.log_INFO(Messages.Tour_Location_Log_LocationIsDeleted.formatted(
                  returnValue_TourData_Start[locationIndex],
                  returnValue_TourData_End[locationIndex],
                  returnValue_TourLocation[locationIndex],
                  allLocations.get(locationIndex).display_name));
         }

         returnResult = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_TourData_Start);
         Util.closeSql(prepStmt_TourData_End);
         Util.closeSql(prepStmt_TourLocation);
      }

      return returnResult;
   }

   static String getCombined_StreetWithHouseNumber(final TourLocation tourLocation) {

      final String adrRoad = tourLocation.road;
      final String adrHouseNumber = tourLocation.house_number;

      if (adrRoad == null && adrHouseNumber == null) {

         return null;

      } else if (adrRoad == null) {

         return adrHouseNumber;

      } else if (adrHouseNumber == null) {

         return adrRoad;
      }

      // road and house number are available

      final String countryCode = tourLocation.country_code;

      if (COUNTRY_CODE_US.equals(countryCode)) {

         return adrHouseNumber + UI.SPACE + adrRoad;

      } else {

         return adrRoad + UI.SPACE + adrHouseNumber;
      }
   }

   /**
    * @return Returns the default profile or <code>null</code> when a profile is not set.
    */
   public static TourLocationProfile getDefaultProfile() {

      // ensure that a default profile is set
      if (_defaultProfile == null && _allLocationProfiles.size() > 0) {

         // select first profile
         _defaultProfile = _allLocationProfiles.get(0);
      }

      return _defaultProfile;
   }

   /**
    * Retrieve location data when not yet available
    *
    * @param latitude
    * @param longitude
    * @param existingLocationData
    * @param zoomlevel
    *
    * @return
    */
   public static TourLocationData getLocationData(final double latitude,
                                                  final double longitude,
                                                  final TourLocationData existingLocationData,
                                                  final int zoomlevel) {

// enable logging when debugging
//    _isLogging_AddressRetrieval = true;

      /*
       * Check if tour is contained in existing location data (which is not yet saved)
       */
      if (isLatLonInLocation(latitude, longitude, existingLocationData)) {
         return existingLocationData;
      }

      /*
       * Check if a tour location is already saved
       */
      final TourLocation dbTourLocation = TourDatabase.getTourLocation(latitude, longitude, zoomlevel);
      if (dbTourLocation != null) {

         return new TourLocationData(dbTourLocation);
      }

      /*
       * Retrieve location
       */
      final TourLocationData tourLocationData = getLocationData_10_Download_Prepare(latitude, longitude, zoomlevel);

      if (tourLocationData == null) {
         return null;
      }

      final OSMLocation osmLocation = getLocationData_20_DeserializeData(tourLocationData.downloadedData);

      tourLocationData.tourLocation = createTourLocation(osmLocation, latitude, longitude, zoomlevel);

      if (_isLogging_AddressRetrieval && osmLocation != null) {

         System.out.println("Default name      " + osmLocation.display_name); //$NON-NLS-1$

         if (osmLocation.name != null && osmLocation.name.length() > 0) {
            System.out.println("name              " + osmLocation.name); //$NON-NLS-1$
         }

         System.out.println(" Waiting time     %d ms".formatted(tourLocationData.waitingTime)); //$NON-NLS-1$
         System.out.println(" Download time    %d ms".formatted(tourLocationData.downloadTime)); //$NON-NLS-1$

         if (osmLocation.address != null) {
            System.out.println(osmLocation.address.logAddress());
         }
      }

      return tourLocationData;
   }

   /**
    * Limits and download policy:
    * <a href=
    * "https://operations.osmfoundation.org/policies/nominatim/">https://operations.osmfoundation.org/policies/nominatim/</a>
    * <p>
    *
    * @param latitude
    * @param longitude
    * @param zoomLevel
    *
    * @return Returns <code>null</code> or {@link TourLocationData}
    */
   private static TourLocationData getLocationData_10_Download_Prepare(final double latitude,
                                                                       final double longitude,
                                                                       final int zoomLevel) {

      final long now = System.currentTimeMillis();
      long waitingTime = now - _lastRetrievalTimeMS;

      if (waitingTime < 1000) {

         /*
          * Max requests are limited to 1 per second, we have to wait
          * https://operations.osmfoundation.org/policies/nominatim/
          */

         waitingTime = 1000 - waitingTime;

         try {

            Thread.sleep(waitingTime);

         } catch (final InterruptedException e) {
            StatusUtil.showStatus(e);
            Thread.currentThread().interrupt();
         }

      } else {

         // waiting time >= 1000 ms -> adjust value for log message

         waitingTime = 0;
      }

      final long retrievalStartTime = System.currentTimeMillis();
      _lastRetrievalTimeMS = retrievalStartTime;

      final String language = Locale.getDefault().getLanguage();

      final String requestUrl = UI.EMPTY_STRING

            + "https://nominatim.openstreetmap.org/reverse?" //$NON-NLS-1$

            + "format=json" //                     //$NON-NLS-1$
            + "&addressdetails=1" //               //$NON-NLS-1$

            + "&lat=" + latitude //                //$NON-NLS-1$
            + "&lon=" + longitude //               //$NON-NLS-1$
            + "&zoom=" + zoomLevel //              //$NON-NLS-1$

            + "&accept-language=" + language //    //$NON-NLS-1$

//          + "&polygon_text=1" //                 //$NON-NLS-1$
//          + "&polygon_geojson=1" //              //$NON-NLS-1$

//          + "&extratags=1" //                    //$NON-NLS-1$
//          + "&namedetails=1" //                  //$NON-NLS-1$

//          + "&layer=address,poi,railway,natural,manmade" //$NON-NLS-1$

      ;

      if (_isLogging_AddressRetrieval) {

         System.out.println(requestUrl);
      }

      final String[] downloadedData = { null };

      if (Display.getCurrent() == null) {

         // this code is running not in the display thread, potentially in the progress thread

         getLocationData_15_Download_HttpRequest(requestUrl, downloadedData);

      } else {

         BusyIndicator.showWhile(Display.getDefault(), () -> {

            getLocationData_15_Download_HttpRequest(requestUrl, downloadedData);
         });
      }

      if (downloadedData[0] == null) {
         return null;
      }

      final long retrievalEndTime = System.currentTimeMillis();
      final long retrievalDuration = retrievalEndTime - retrievalStartTime;

      return new TourLocationData(downloadedData[0], retrievalDuration, waitingTime);
   }

   private static void getLocationData_15_Download_HttpRequest(final String requestUrl, final String[] downloadedData) {

      try {

         final HttpRequest request = HttpRequest
               .newBuilder(URI.create(requestUrl))
               .header(WEB.HTTP_HEADER_USER_AGENT, _userAgent)
               .timeout(_httpTimeoutDuration)
               .GET()
               .build();

         final HttpResponse<String> response = _httpClient.send(request, BodyHandlers.ofString());

         downloadedData[0] = response.body();

         if (response.statusCode() != HttpURLConnection.HTTP_OK) {

            logError(downloadedData[0]);

            downloadedData[0] = null;
         }

      } catch (final HttpTimeoutException ex) {

         StatusUtil.showStatus(requestUrl, ex);

         logException(requestUrl, ex);

         downloadedData[0] = null;

      } catch (final Exception ex) {

         logException(requestUrl, ex);

//       Thread.currentThread().interrupt();

         downloadedData[0] = null;
      }
   }

   private static OSMLocation getLocationData_20_DeserializeData(final String osmLocationString) {

      OSMLocation osmLocation = null;

      try {

         osmLocation = new ObjectMapper().readValue(osmLocationString, OSMLocation.class);

      } catch (final Exception e) {

         StatusUtil.logError(

               TourLocationManager.class.getSimpleName() + ".deserializeLocationData : " //$NON-NLS-1$
                     + "Error while deserializing the location JSON object : " //$NON-NLS-1$
                     + osmLocationString + NL + e.getMessage());
      }

      return osmLocation;
   }

   public static List<TourLocationProfile> getProfiles() {

      return _allLocationProfiles;
   }

   /**
    * @return Returns the zoomlevel of the default profile, when not available then the default
    *         zoomlevel
    */
   public static int getProfileZoomlevel() {

      return _defaultProfile == null

            ? DEFAULT_ZOOM_LEVEL_VALUE

            : _defaultProfile.getZoomlevel();
   }

   /**
    * Collects all {@link TourLocation} from the provided {@link TourData}
    *
    * @param allTourData
    *
    * @return Returns all {@link TourLocation}s
    */
   public static List<TourLocation> getTourLocations(final List<TourData> allTourData) {

      final List<TourLocation> allTourLocations = new ArrayList<>();

      for (final TourData tourData : allTourData) {

         final TourLocation tourLocationStart = tourData.getTourLocationStart();
         final TourLocation tourLocationEnd = tourData.getTourLocationEnd();

         if (tourLocationStart != null) {
            allTourLocations.add(tourLocationStart);
         }

         if (tourLocationEnd != null) {
            allTourLocations.add(tourLocationEnd);
         }
      }

      return allTourLocations;
   }

   /**
    * @param allLocations
    *
    * @return Returns a list with all tour id's which are containing the tour locations.
    */
   public static ArrayList<Long> getToursWithLocations(final List<TourLocation> allLocations) {

      final ArrayList<Long> allTourIds = new ArrayList<>();

      final ArrayList<Long> allSqlParameter = new ArrayList<>();
      final StringBuilder sqlParameterPlaceholder = new StringBuilder();

      boolean isFirst = true;

      for (final TourLocation tourLocation : allLocations) {

         if (isFirst) {
            isFirst = false;
            sqlParameterPlaceholder.append(TourDatabase.PARAMETER_FIRST);
         } else {
            sqlParameterPlaceholder.append(TourDatabase.PARAMETER_FOLLOWING);
         }

         allSqlParameter.add(tourLocation.getLocationId());
      }

      final String sqlParameter = sqlParameterPlaceholder.toString();

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                             //$NON-NLS-1$

            + " DISTINCT TourId" + NL //                                   //$NON-NLS-1$

            + " FROM TourData" + NL //                                     //$NON-NLS-1$

            + " WHERE tourLocationStart_LocationID IN (" + sqlParameter + ")" + NL //  //$NON-NLS-1$ //$NON-NLS-2$
            + "    OR tourLocationEnd_LocationID   IN (" + sqlParameter + ")" + NL //  //$NON-NLS-1$ //$NON-NLS-2$

            + " ORDER BY tourId" //                                        //$NON-NLS-1$
      ;

      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         // fillup parameters for 2 fields
         int sqlIndex = 1;
         final int numParameters = allSqlParameter.size();

         for (int parameterIndex = 0; parameterIndex < numParameters; parameterIndex++) {
            statement.setLong(sqlIndex++, allSqlParameter.get(parameterIndex));
         }

         for (int parameterIndex = 0; parameterIndex < numParameters; parameterIndex++) {
            statement.setLong(sqlIndex++, allSqlParameter.get(parameterIndex));
         }

         final ResultSet result = statement.executeQuery();

         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         UI.showSQLException(e);

      } finally {
         Util.closeSql(statement);
      }

      return allTourIds;
   }

   private static File getXmlFile() {

      final File layerFile = _stateLocation.append(TOUR_LOCATION_FILE_NAME).toFile();

      return layerFile;
   }

   /**
    * Returns the index of the requested zoomlevel
    *
    * @param requestedZoomlevel
    *
    * @return
    */
   public static int getZoomlevelIndex(final int requestedZoomlevel) {

      for (int zoomlevelIndex = 0; zoomlevelIndex < ALL_ZOOM_LEVEL.length; zoomlevelIndex++) {

         if (ALL_ZOOM_LEVEL[zoomlevelIndex].zoomlevel >= requestedZoomlevel) {
            return zoomlevelIndex;
         }
      }

      final int defaultZoomlevel = DEFAULT_ZOOM_LEVEL.zoomlevel;

      for (int zoomlevelIndex = 0; zoomlevelIndex < ALL_ZOOM_LEVEL.length; zoomlevelIndex++) {

         if (ALL_ZOOM_LEVEL[zoomlevelIndex].zoomlevel >= defaultZoomlevel) {
            return zoomlevelIndex;
         }
      }

      return 0;
   }

   private static boolean isLatLonInLocation(final double latitude,
                                             final double longitude,
                                             final TourLocationData existingLocationData) {

      if (existingLocationData == null) {
         return false;
      }

      final TourLocation tourLocation = existingLocationData.tourLocation;

      if (true

            && tourLocation.latitudeMin <= latitude
            && tourLocation.latitudeMax >= latitude

            && tourLocation.longitudeMin <= longitude
            && tourLocation.longitudeMax >= longitude) {

         return true;
      }

      return false;
   }

   private static void logError(final String exceptionMessage) {

      TourLogManager.log_ERROR(NLS.bind(
            "Error while retrieving tour location data: \"{1}\"", //$NON-NLS-1$
            exceptionMessage));
   }

   private static void logException(final String requestUrl, final Exception ex) {

      TourLogManager.log_EXCEPTION_WithStacktrace("Error while retrieving tour location data: " + requestUrl + NL, ex); //$NON-NLS-1$
   }

   public static void removeTourLocations(final List<TourData> requestedTours,
                                          final boolean isSetStartLocation,
                                          final boolean isSetEndLocation,
                                          final boolean isCompleteRemoval) {

      final ArrayList<TourData> savedTours = new ArrayList<>();

      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                  throws InvocationTargetException, InterruptedException {

               final int numTours = requestedTours.size();
               final int numRequests = numTours * (isSetStartLocation && isSetEndLocation ? 2 : 1);
               int numWorked = 0;

               monitor.beginTask(Messages.Tour_Location_Task_RemovingTourLocations.formatted(numRequests), numRequests);

               for (final TourData tourData : requestedTours) {

                  if (monitor.isCanceled()) {
                     break;
                  }

                  boolean isModified = false;

                  /*
                   * Start location
                   */
                  if (isSetStartLocation) {

                     tourData.setTourStartPlace(null);

                     if (isCompleteRemoval) {
                        tourData.setTourLocationStart(null);
                     }

                     isModified = true;

                     ++numWorked;
                     monitor.worked(1);
                  }

                  /*
                   * End location
                   */
                  if (isSetEndLocation) {

                     tourData.setTourEndPlace(null);

                     if (isCompleteRemoval) {
                        tourData.setTourLocationEnd(null);
                     }

                     isModified = true;

                     ++numWorked;
                     monitor.worked(1);
                  }

                  monitor.subTask(SUB_TASK_NTH_OF_ALL.formatted(numWorked, numRequests));

                  if (isModified) {

                     TourManager.saveModifiedTour(tourData, false);

                     savedTours.add(tourData);
                  }
               }
            }
         };

         new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {

         StatusUtil.showStatus(e);
         Thread.currentThread().interrupt();
      }

      if (savedTours.size() > 0) {

         final TourEvent tourEvent = new TourEvent(savedTours);

         // this must be fired in the UI thread
         TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);
      }
   }

   public static void restoreState() {

      xmlRead_Profiles();
   }

   public static void saveState() {

      final XMLMemento xmlRoot = xmlWrite_Profiles();
      final File xmlFile = getXmlFile();

      Util.writeXml(xmlRoot, xmlFile);
   }

   static void setDefaultProfile(final TourLocationProfile defaultProfile) {

      _defaultProfile = defaultProfile;
   }

   /**
    * Set tour locations for the requested tours, when not available, then download and save tour
    * locations
    *
    * @param requestedTours
    * @param locationProfile
    * @param isSetStartLocation
    * @param isSetEndLocation
    * @param isForceReloadLocation
    *           When <code>true</code> then existing locations are ignored and retrieved again from
    *           the DB or location provider
    */
   public static void setTourLocations(final List<TourData> requestedTours,
                                       final TourLocationProfile locationProfile,
                                       final boolean isSetStartLocation,
                                       final boolean isSetEndLocation,
                                       final boolean isForceReloadLocation) {

      final ArrayList<TourData> savedTours = new ArrayList<>();

      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {

            @Override
            public void run(final IProgressMonitor monitor)
                  throws InvocationTargetException, InterruptedException {

               final int numTours = requestedTours.size();
               final int numRequests = numTours * (isSetStartLocation && isSetEndLocation ? 2 : 1);
               int numWorked = 0;

               monitor.beginTask(Messages.Tour_Location_Task_RetrievingTourLocations.formatted(numRequests), numRequests);

               for (final TourData tourData : requestedTours) {

                  if (monitor.isCanceled()) {
                     break;
                  }

                  final double[] latitudeSerie = tourData.latitudeSerie;
                  final double[] longitudeSerie = tourData.longitudeSerie;

                  if (latitudeSerie == null || latitudeSerie.length == 0) {

                     // needed data are not available

                     numWorked++;
                     numWorked++;

                     monitor.worked(2);
                     monitor.subTask(SUB_TASK_NTH_OF_ALL.formatted(numWorked, numRequests));

                     continue;
                  }

                  boolean isModified = false;
                  long waitingTime;

                  /*
                   * Start location
                   */
                  TourLocationData startLocationData = null;

                  if (isSetStartLocation) {

                     waitingTime = 0;

                     if (isForceReloadLocation) {
                        tourData.setTourStartPlace(null);
                        tourData.setTourLocationStart(null);
                     }

                     TourLocation tourLocationStart = tourData.getTourLocationStart();
                     if (tourLocationStart == null) {

                        startLocationData = getLocationData(

                              latitudeSerie[0],
                              longitudeSerie[0],
                              null,
                              locationProfile.getZoomlevel());

                        if (startLocationData != null) {

                           waitingTime = startLocationData.waitingTime;
                           tourLocationStart = startLocationData.tourLocation;
                        }
                     }

                     if (tourLocationStart != null) {

                        final String startLocationText = createLocationDisplayName(tourLocationStart, locationProfile);

                        tourData.setTourStartPlace(startLocationText);
                        tourData.setTourLocationStart(tourLocationStart);

                        isModified = true;
                     }

                     monitor.worked(1);
                     monitor.subTask(SUB_TASK_RETRIEVE_LOCATIONS.formatted(++numWorked, numRequests, waitingTime));
                  }

                  /*
                   * End location
                   */
                  if (isSetEndLocation) {

                     waitingTime = 0;

                     if (isForceReloadLocation) {
                        tourData.setTourEndPlace(null);
                        tourData.setTourLocationEnd(null);
                     }

                     TourLocation tourLocationEnd = tourData.getTourLocationEnd();
                     if (tourLocationEnd == null) {

                        final int lastIndex = latitudeSerie.length - 1;

                        final TourLocationData endLocationData = getLocationData(

                              latitudeSerie[lastIndex],
                              longitudeSerie[lastIndex],

                              startLocationData,
                              locationProfile.getZoomlevel());

                        if (endLocationData != null) {

                           waitingTime = endLocationData.waitingTime;
                           tourLocationEnd = endLocationData.tourLocation;
                        }
                     }

                     if (tourLocationEnd != null) {

                        final String endLocationText = createLocationDisplayName(tourLocationEnd, locationProfile);

                        tourData.setTourEndPlace(endLocationText);
                        tourData.setTourLocationEnd(tourLocationEnd);

                        isModified = true;
                     }

                     monitor.worked(1);
                     monitor.subTask(SUB_TASK_RETRIEVE_LOCATIONS.formatted(++numWorked, numRequests, waitingTime));
                  }

                  if (isModified) {

                     TourManager.saveModifiedTour(tourData, false);

                     savedTours.add(tourData);
                  }
               }
            }
         };

         new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {

         StatusUtil.showStatus(e);
         Thread.currentThread().interrupt();
      }

      if (savedTours.size() > 0) {

         final TourEvent tourEvent = new TourEvent(savedTours);

         // this must be fired in the UI thread
         TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);
      }
   }

   private static String validString(final String stringValue) {

      if (stringValue == null) {

         return null;

      } else if (stringValue.length() <= TourLocation.DB_FIELD_LENGTH) {

         return stringValue;

      } else {

         return stringValue.substring(0, TourLocation.DB_FIELD_LENGTH);
      }
   }

   /**
    * Read tour location xml file.
    *
    * @return
    */
   private static void xmlRead_Profiles() {

      final File xmlFile = getXmlFile();

      if (xmlFile.exists() == false) {

         createDefaultProfiles();

         return;
      }

      try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

         // <TourLocationProfiles>
         final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);

         // loop: all location profiles
         for (final IMemento mementoChild : xmlRoot.getChildren()) {

            final XMLMemento xmlProfile = (XMLMemento) mementoChild;

            if (TAG_PROFILE.equals(xmlProfile.getType())) {

               // <Profile>

               final TourLocationProfile profile = new TourLocationProfile();

               profile.zoomlevel = Util.getXmlInteger(xmlProfile, ATTR_ZOOMLEVEL, DEFAULT_ZOOM_LEVEL_VALUE);
               profile.name = Util.getXmlString(xmlProfile, ATTR_PROFILE_NAME, UI.EMPTY_STRING);

               if (Util.getXmlBoolean(xmlProfile, ATTR_IS_DEFAULT_PROFILE, false)) {
                  _defaultProfile = profile;
               }

               final IMemento xmlParts = xmlProfile.getChild(TAG_PARTS);

               if (xmlParts != null) {

                  // <Parts>

                  for (final IMemento xmlPart : xmlParts.getChildren()) {

                     final LocationPartID part = (LocationPartID) Util.getXmlEnum(xmlPart, ATTR_NAME, LocationPartID.NONE);

                     if (part.equals(LocationPartID.NONE) == false) {

                        profile.allParts.add(part);
                     }
                  }
               }

               _allLocationProfiles.add(profile);
            }
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }
   }

   private static XMLMemento xmlWrite_Profiles() {

      XMLMemento xmlRoot = null;

      try {

         // <TourLocationProfiles>
         xmlRoot = xmlWrite_Profiles_10_Root();

         // loop: all location profiles
         for (final TourLocationProfile locationProfile : _allLocationProfiles) {

            // <Profile>
            final IMemento xmlLocation = xmlRoot.createChild(TAG_PROFILE);

            xmlLocation.putInteger(ATTR_ZOOMLEVEL, locationProfile.getZoomlevel());
            xmlLocation.putString(ATTR_PROFILE_NAME, locationProfile.getName());

            if (_defaultProfile == locationProfile) {
               xmlLocation.putBoolean(ATTR_IS_DEFAULT_PROFILE, true);
            }

            // <Parts>
            final IMemento xmlParts = xmlLocation.createChild(TAG_PARTS);

            // loop: all location parts
            for (final LocationPartID locationPart : locationProfile.allParts) {

               // <Part>
               final IMemento xmlPart = xmlParts.createChild(TAG_PART);

               Util.setXmlEnum(xmlPart, ATTR_NAME, locationPart);
            }
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento xmlWrite_Profiles_10_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // layer structure version
      xmlRoot.putInteger(ATTR_TOUR_LOCATION_VERSION, TOUR_LOCATION_VERSION);

      return xmlRoot;
   }

}
