/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.ApplicationWorkbenchAdvisor;
import net.tourbook.application.SplashManager;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.data.TourPhoto;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.equipment.EquipmentManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.search.FTSearchManager;
import net.tourbook.tag.TagCollection;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.AppFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TreeColumnFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.hibernate.ejb.Ejb3Configuration;

public class TourDatabase {

   /**
    * Version for the database which is required that the tourbook application works successfully
    * <p>
    * When an entity structure is modified, a field is added/removed/renamed, then the .mt
    * export/import <b>MUST</b> be adjusted which is done mainly in
    * <p>
    * <li>/net.tourbook.export/format-templates/mt-1.0.vm</li>
    * <li>net.tourbook.device.mt.MT_StAXHandler</li>
    */
   private static final int TOURBOOK_DB_VERSION = 63; // 26.9

//   private static final int TOURBOOK_DB_VERSION = 63; // 26.9
//   private static final int TOURBOOK_DB_VERSION = 62; // 26.8
//   private static final int TOURBOOK_DB_VERSION = 61; // 26.6
//   private static final int TOURBOOK_DB_VERSION = 60; // 26.3
//   private static final int TOURBOOK_DB_VERSION = 59; // 25.11
//   private static final int TOURBOOK_DB_VERSION = 58; // 25.6
//   private static final int TOURBOOK_DB_VERSION = 57; // 25.4
//   private static final int TOURBOOK_DB_VERSION = 56; // 24.11.3
//   private static final int TOURBOOK_DB_VERSION = 55; // 24.5
//   private static final int TOURBOOK_DB_VERSION = 54; // 24.1 fixed db data update bug 47 -> 48
//   private static final int TOURBOOK_DB_VERSION = 53; // 24.1 added new fields
//   private static final int TOURBOOK_DB_VERSION = 52; // 24.1
//   private static final int TOURBOOK_DB_VERSION = 51; // 23.8
//   private static final int TOURBOOK_DB_VERSION = 50; // 23.5
//   private static final int TOURBOOK_DB_VERSION = 49; // 23.3
//   private static final int TOURBOOK_DB_VERSION = 48; // 22.6
//   private static final int TOURBOOK_DB_VERSION = 47; // 22.3
//   private static final int TOURBOOK_DB_VERSION = 46; // 21.12
//   private static final int TOURBOOK_DB_VERSION = 45; // 21.9
//   private static final int TOURBOOK_DB_VERSION = 44; // 21.6
//   private static final int TOURBOOK_DB_VERSION = 43; // 21.3
//   private static final int TOURBOOK_DB_VERSION = 42; // 20.11.1
//   private static final int TOURBOOK_DB_VERSION = 41; // 20.8
//   private static final int TOURBOOK_DB_VERSION = 40; // 19.10
//   private static final int TOURBOOK_DB_VERSION = 39; // 19.7
//   private static final int TOURBOOK_DB_VERSION = 38; // 19.6
//   private static final int TOURBOOK_DB_VERSION = 37; // 19.2
//   private static final int TOURBOOK_DB_VERSION = 36; // 18.12
//   private static final int TOURBOOK_DB_VERSION = 35; // 18.7
//   private static final int TOURBOOK_DB_VERSION = 34; // 18.5
//   private static final int TOURBOOK_DB_VERSION = 33; // 17.12
//   private static final int TOURBOOK_DB_VERSION = 32; // 16.10
//   private static final int TOURBOOK_DB_VERSION = 31; // 16.5
//   private static final int TOURBOOK_DB_VERSION = 30; // 16.1
//   private static final int TOURBOOK_DB_VERSION = 29; // 15.12
//   private static final int TOURBOOK_DB_VERSION = 28; // 15.6
//   private static final int TOURBOOK_DB_VERSION = 27; // 15.3.1
//   private static final int TOURBOOK_DB_VERSION = 26; // 14.14 / 15.3
//   private static final int TOURBOOK_DB_VERSION = 25; // 14.10
//   private static final int TOURBOOK_DB_VERSION = 24; // 14.7
//   private static final int TOURBOOK_DB_VERSION = 23; // 13.2.0
//   private static final int TOURBOOK_DB_VERSION = 22; // 12.12.0
//   private static final int TOURBOOK_DB_VERSION = 21; // 12.1.1
//   private static final int TOURBOOK_DB_VERSION = 20; // 12.1
//   private static final int TOURBOOK_DB_VERSION = 19; // 11.8
//   private static final int TOURBOOK_DB_VERSION = 18; // 11.8
//   private static final int TOURBOOK_DB_VERSION = 17; // 11.8
//   private static final int TOURBOOK_DB_VERSION = 16; // 11.8
//   private static final int TOURBOOK_DB_VERSION = 15; // 11.8
//   private static final int TOURBOOK_DB_VERSION = 14; // 11.3
//   private static final int TOURBOOK_DB_VERSION = 13; // 10.11
//   private static final int TOURBOOK_DB_VERSION = 12; // 10.9.1
//   private static final int TOURBOOK_DB_VERSION = 11; // 10.7.0 - 11-07-2010
//   private static final int TOURBOOK_DB_VERSION = 10; // 10.5.0 not released
//   private static final int TOURBOOK_DB_VERSION = 9;  // 10.3.0
//   private static final int TOURBOOK_DB_VERSION = 8;  // 10.2.1 Modified by Kenny
//   private static final int TOURBOOK_DB_VERSION = 7;  // 9.01
//   private static final int TOURBOOK_DB_VERSION = 6;  // 8.12
//   private static final int TOURBOOK_DB_VERSION = 5;  // 8.11

//   private static final String SQL_STATE_XJ004_DATABASE_NOT_FOUND         = "XJ004";                                                 //$NON-NLS-1$

   private static final char   NL                                         = UI.NEW_LINE;

   public static final int     VARCHAR_MAX_LENGTH                         = 32_672;

   /**
    * Common text lengths
    */
   public static final int     DB_LENGTH_DESCRIPTION                      = 32000;
   public static final int     DB_LENGTH_NOTES                            = 32000;
   public static final int     DB_LENGTH_FILE_PATH                        = 260;
   public static final int     DB_LENGTH_NAME                             = 1000;
   public static final int     DB_LENGTH_URL_ADDRESS                      = 4096;

   private static final String NUMBER_FORMAT_1F                           = "%.1f";                                                  //$NON-NLS-1$

   /**
    * Milliseconds how long the splash message is delayed before it is updated again.
    */
   private static final int    DELAY_SPLASH_LOGGING                       = 500;

   /**
    * <b> !!! Table names are set to uppercase otherwise conn.getMetaData().getColumns() would not
    * work !!! </b>
    */
   private static final String TABLE_SCHEMA                               = "USER";                                                  //$NON-NLS-1$

   static final String         TABLE_DB_VERSION_DESIGN                    = "DBVERSION";                                             //$NON-NLS-1$
   static final String         TABLE_DB_VERSION_DATA                      = "DB_VERSION_DATA";                                       //$NON-NLS-1$

   public static final String  TABLE_DEVICE_SENSOR                        = "DeviceSensor";                                          //$NON-NLS-1$
   public static final String  TABLE_DEVICE_SENSOR_VALUE                  = "DeviceSensorValue";                                     //$NON-NLS-1$
   public static final String  TABLE_EQUIPMENT                            = "Equipment";                                             //$NON-NLS-1$
   public static final String  TABLE_EQUIPMENT_PART                       = "EquipmentPart";                                         //$NON-NLS-1$
   public static final String  TABLE_TOUR_BEVERAGE_CONTAINER              = "TOURBEVERAGECONTAINER";                                 //$NON-NLS-1$
   public static final String  TABLE_TOUR_BIKE                            = "TOURBIKE";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_COMPARED                        = "TOURCOMPARED";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_DATA                            = "TOURDATA";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_NUTRITION_PRODUCT               = "TOURNUTRITIONPRODUCT";                                  //$NON-NLS-1$
   public static final String  TABLE_TOUR_GEO_PARTS                       = "TourGeoParts";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_LOCATION                        = "TourLocation";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_MARKER                          = "TOURMARKER";                                            //$NON-NLS-1$
   public static final String  TABLE_TOUR_MARKER_TYPE                     = "TourMarkerType";                                        //$NON-NLS-1$
   public static final String  TABLE_TOUR_PERSON                          = "TOURPERSON";                                            //$NON-NLS-1$
   public static final String  TABLE_TOUR_PERSON_HRZONE                   = "TOURPERSONHRZONE";                                      //$NON-NLS-1$
   public static final String  TABLE_TOUR_PHOTO                           = "TOURPHOTO";                                             //$NON-NLS-1$
   public static final String  TABLE_TOUR_REFERENCE                       = "TOURREFERENCE";                                         //$NON-NLS-1$
   public static final String  TABLE_TOUR_TAG                             = "TOURTAG";                                               //$NON-NLS-1$
   public static final String  TABLE_TOUR_TAG_CATEGORY                    = "TOURTAGCATEGORY";                                       //$NON-NLS-1$
   public static final String  TABLE_TOUR_TYPE                            = "TOURTYPE";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_WAYPOINT                        = "TOURWAYPOINT";                                          //$NON-NLS-1$

   public static final String  JOINTABLE__TOURDATA__TOURTAG               = TABLE_TOUR_DATA + "_" + TABLE_TOUR_TAG;                  //$NON-NLS-1$
   public static final String  JOINTABLE__TOURTAGCATEGORY_TOURTAG         = TABLE_TOUR_TAG_CATEGORY + "_" + TABLE_TOUR_TAG;          //$NON-NLS-1$
   public static final String  JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY = TABLE_TOUR_TAG_CATEGORY + "_" + TABLE_TOUR_TAG_CATEGORY; //$NON-NLS-1$

   public static final String  JOINTABLE__TOURDATA__EQUIPMENT             = TABLE_TOUR_DATA + "_" + TABLE_EQUIPMENT;                 //$NON-NLS-1$

   /**
    * Is <code>-1</code>, this is the id for a not saved entity
    */
   public static final int     ENTITY_IS_NOT_SAVED                        = -1;
   //
   public static final String  ENTITY_ID_BEVERAGECONTAINER                = "ContainerID";                                           //$NON-NLS-1$
   private static final String ENTITY_ID_BIKE                             = "BikeID";                                                //$NON-NLS-1$
   private static final String ENTITY_ID_COMPARED                         = "ComparedID";                                            //$NON-NLS-1$
   private static final String ENTITY_ID_DEVICE_SENSOR                    = "SensorId";                                              //$NON-NLS-1$
   public static final String  ENTITY_ID_EQUIPMENT                        = "EquipmentId";                                           //$NON-NLS-1$
   private static final String ENTITY_ID_EQUIPMENT_PART                   = "PartId";                                                //$NON-NLS-1$
   private static final String ENTITY_ID_DEVICE_SENSOR_VALUE              = "SensorValueId";                                         //$NON-NLS-1$
   private static final String ENTITY_ID_HR_ZONE                          = "HrZoneID";                                              //$NON-NLS-1$
   private static final String ENTITY_ID_LOCATION                         = "LocationID";                                            //$NON-NLS-1$
   private static final String ENTITY_ID_MARKER                           = "MarkerID";                                              //$NON-NLS-1$
   private static final String ENTITY_ID_MARKER_TYPE                      = "MarkerTypeID";                                          //$NON-NLS-1$
   private static final String ENTITY_ID_NUTRITIONPRODUCT                 = "ProductID";                                             //$NON-NLS-1$
   private static final String ENTITY_ID_PERSON                           = "PersonID";                                              //$NON-NLS-1$
   private static final String ENTITY_ID_PHOTO                            = "PhotoID";                                               //$NON-NLS-1$
   private static final String ENTITY_ID_REF                              = "RefID";                                                 //$NON-NLS-1$
   public static final String  ENTITY_ID_TAG                              = "TagID";                                                 //$NON-NLS-1$
   public static final String  ENTITY_ID_TAG_CATEGORY                     = "TagCategoryID";                                         //$NON-NLS-1$
   private static final String ENTITY_ID_TOUR                             = "TourID";                                                //$NON-NLS-1$
   private static final String ENTITY_ID_TOUR_TYPE                        = "TypeID";                                                //$NON-NLS-1$
   public static final String  ENTITY_ID_WAY_POINT                        = "WayPointID";                                            //$NON-NLS-1$

// SET_FORMATTING_OFF

   public  static final String KEY_BEVERAGE_CONTAINER       = TABLE_TOUR_BEVERAGE_CONTAINER  + "_" + ENTITY_ID_BEVERAGECONTAINER;   //$NON-NLS-1$
   private static final String KEY_BIKE                     = TABLE_TOUR_BIKE                + "_" + ENTITY_ID_BIKE;                //$NON-NLS-1$
   private static final String KEY_DEVICE_SENSOR            = TABLE_DEVICE_SENSOR            + "_" + ENTITY_ID_DEVICE_SENSOR;       //$NON-NLS-1$
   public static final String  KEY_EQUIPMENT                = TABLE_EQUIPMENT                + "_" + ENTITY_ID_EQUIPMENT;           //$NON-NLS-1$
   public static final String  KEY_MARKER_TYPE              = TABLE_TOUR_MARKER_TYPE         + "_" + ENTITY_ID_MARKER_TYPE;         //$NON-NLS-1$
   private static final String KEY_PERSON                   = TABLE_TOUR_PERSON              + "_" + ENTITY_ID_PERSON;              //$NON-NLS-1$
   public static final String  KEY_TAG                      = TABLE_TOUR_TAG                 + "_" + ENTITY_ID_TAG;                 //$NON-NLS-1$
   private static final String KEY_TAG_CATEGORY             = TABLE_TOUR_TAG_CATEGORY        + "_" + ENTITY_ID_TAG_CATEGORY;        //$NON-NLS-1$
   public static final String  KEY_TOUR                     = TABLE_TOUR_DATA                + "_" + ENTITY_ID_TOUR;                //$NON-NLS-1$
   public static final String  KEY_TOUR_LOCATION            = TABLE_TOUR_LOCATION            + "_" + ENTITY_ID_LOCATION;            //$NON-NLS-1$
   private static final String KEY_TOUR_TYPE                = TABLE_TOUR_TYPE                + "_" + ENTITY_ID_TOUR_TYPE;           //$NON-NLS-1$

// SET_FORMATTING_ON

   public static final String  PARAMETER_FIRST     = "?";     //$NON-NLS-1$
   public static final String  PARAMETER_FOLLOWING = ", ?";   //$NON-NLS-1$

   private static final String DEFAULT_0           = "0";     //$NON-NLS-1$
   private static final String DEFAULT_1_0         = "1.0";   //$NON-NLS-1$
   private static final String DEFAULT_FALSE       = "false"; //$NON-NLS-1$
   private static final String DEFAULT_IGNORED     = "-1";    //$NON-NLS-1$

   /**
    * ERROR XCL13: The parameter position '13' is out of range. The number of parameters for
    * this prepared statement is '12'.
    */
   public static final String  SQL_ERROR_XCL13     = "XCL13"; //$NON-NLS-1$

   /**
    * ERROR XCL14: The column position '3' is out of range. The number of columns for this ResultSet
    * is '2'.
    */
   public static final String  SQL_ERROR_XCL14     = "XCL14"; //$NON-NLS-1$
   //
   //
   private static volatile TourDatabase                   _instance;

   private static ArrayList<TourType>                     _activeTourTypes;

   private static volatile ArrayList<TourType>            _allDbTourTypes;

   /**
    * Key is tour type ID
    */
   private static HashMap<Long, TourType>                 _allDbTourTypes_ById;

   /**
    * Key is the UPPERCASE tour type name
    */
   private static HashMap<String, TourType>               _allDbTourTypes_ByName;

   /**
    * Key is tag ID.
    */
   private static volatile HashMap<Long, TourTag>         _allTourTags_ByTagId;

   /**
    * Key is the UPPERCASE tag name
    */
   private static volatile HashMap<String, TourTag>       _allTourTags_ByTagName;

   /**
    * Key is tag category ID.
    */
   private static volatile HashMap<Long, TourTagCategory> _allTourTagCategoriesByCategory;

   /**
    * Key is tag id
    */
   private static volatile HashMap<Long, TourTagCategory> _allTourTagCategoriesByTag;

   /**
    * Key is category ID or <code>-1</code> for the root.
    */
   private static HashMap<Long, TagCollection>            _tagCollections = new HashMap<>();

   /**
    * Key is sensor entity ID
    */
   private static volatile Map<Long, DeviceSensor>        _allDbDeviceSensors_BySensorID;

   /**
    * Key is a combination of manufacturer/product/device type/serial number. The device type was
    * introduced later because a sensor can have multiple device types
    */
   private static volatile Map<String, DeviceSensor>      _allDbDeviceSensors_BySensorKey_WithDevType;

   /**
    * Key is a combination of manufacturer/product/serial number but without a device type
    */
   private static volatile Map<String, DeviceSensor>      _allDbDeviceSensors_BySensorKey_NoDevType;

   private static volatile List<TourMarkerType>           _allDbTourMarkerTypes;

   /**
    * Key is tour marker type ID
    */
   private static Map<Long, TourMarkerType>               _allDbTourMarkerTypes_ById;

   /*
    * Cached distinct fields
    */
   private static ConcurrentSkipListSet<String> _dbPhotoImageFilePath;
   private static ConcurrentSkipListSet<String> _dbTourTitles;
   private static ConcurrentSkipListSet<String> _dbTourStartPlace;
   private static ConcurrentSkipListSet<String> _dbTourEndPlace;
   private static ConcurrentSkipListSet<String> _dbTourMarkerNames;

   private static final IPreferenceStore        _prefStore                  = TourbookPlugin.getPrefStore();

   /** H2 jdbc jar driver */
   static final String                          H2_DRIVER                   = "org.h2.Driver";                   //$NON-NLS-1$

   static final String                          H2_JDBC_CONNECTION_PROTOCOL = "jdbc:h2:";                        //$NON-NLS-1$
   private static final String                  H2_EJB_HIBERNATE_DIALECT    = "org.hibernate.dialect.H2Dialect"; //$NON-NLS-1$

   /** H2 folder name in the instance location */
   static final String                          H2_DATABASE_FOLDER_NAME     = "h2-database";                     //$NON-NLS-1$

   /** SQL tour database name */
   static final String                          SQL_TOUR_DB_NAME            = "tourbook";                        //$NON-NLS-1$

   /** H2 database file path, without tour db */
   static String                                H2_DATABASE_FILEPATH;

   /** H2 jdbc connection url, without tour db */
   static String                                H2_DB_CONNECTION_URL;

   /** H2 jdbc connection url, with tour db */
   static String                                H2_DB_TOUR_DB_URL;

   private static String                        SQL_DB_CONNECTION_URL;
   private static String                        SQL_DB_TOUR_DB_URL;
   private static String                        SQL_DB_DRIVER_CLASS;
   private static String                        SQL_DB_USERNAME;
   private static String                        SQL_DB_PASSWORD;
   private static String                        SQL_DB_HIBERNATE_DIALECT;

//   private static NetworkServerControl           _server;

   private static volatile EntityManagerFactory _emFactory;

   private static ThreadPoolExecutor            _dbUpdateExecutor;
   private static ArrayBlockingQueue<Long>      _dbUpdateQueue = new ArrayBlockingQueue<>(Util.NUMBER_OF_PROCESSORS);

   static {

      H2_DATABASE_FILEPATH = Platform.getInstanceLocation().getURL().getPath() + H2_DATABASE_FOLDER_NAME;

      if (UI.IS_WIN && H2_DATABASE_FILEPATH.startsWith("/")) {

         // a leading / is causing an exception, e.g. /c:/...

         H2_DATABASE_FILEPATH = H2_DATABASE_FILEPATH.substring(1);
      }

      H2_DB_CONNECTION_URL = H2_JDBC_CONNECTION_PROTOCOL + H2_DATABASE_FILEPATH + ";AUTO_SERVER=TRUE";

      H2_DB_TOUR_DB_URL = H2_JDBC_CONNECTION_PROTOCOL + H2_DATABASE_FILEPATH + "/" + SQL_TOUR_DB_NAME + ";AUTO_SERVER=TRUE";

// FOR DEBUGGING - START
      /*
       * Set derby debug properties, this is helpful when debugging, the log is written into
       * derby.log
       */
//      System.setProperty("derby.language.logStatementText", "true");
//      System.setProperty("derby.language.logQueryPlan", "true");

// FOR DEBUGGING - END

      final ThreadFactory updateThreadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Saving database entities");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _dbUpdateExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Util.NUMBER_OF_PROCESSORS, updateThreadFactory);
   }

   private static final Object CACHE_LOCK     = new Object();
   private static final Object DB_LOCK        = new Object();
   private static final Object TRANSIENT_LOCK = new Object();

//   Derby Limitations
//
//   Smallest           DOUBLE    -1.79769E+308
//   Largest            DOUBLE     1.79769E+308
//   Smallest positive  DOUBLE       2.225E-307
//   Largest negative   DOUBLE      -2.225E-307
//
//   Smallest           REAL         -3.402E+38
//   Largest            REAL          3.402E+38
//   Smallest positive  REAL          1.175E-37
//   Largest negative   REAL         -1.175E-37

   public static final float   DEFAULT_FLOAT  = -1E+35f;
   // This is Float.MIN_VALUE
   public static final double  DEFAULT_DOUBLE = -1E+300;

   private static final String SQL_LONG_MIN_VALUE;
// private static final String   SQL_INT_MIN_VALUE;
   private static final String SQL_FLOAT_MIN_VALUE;
   private static final String SQL_DOUBLE_MIN_VALUE;

   static {

//      !ENTRY net.tourbook.common 4 0 2014-07-30 11:05:18.419
//      !MESSAGE ALTER TABLE TOURMARKER   ADD COLUMN   latitude DOUBLE DEFAULT 4.9E-324
//
//      !ENTRY net.tourbook.common 4 0 2014-07-30 11:05:18.440
//      !MESSAGE SQLException
//
//      SQLState: 22003
//      Severity: 30000
//      Message: The resulting value is outside the range for the data type DOUBLE.
//
////////////////////////////////////////////////////////////////////////////////////////

//      SQL_INT_MIN_VALUE = Integer.toString(Integer.MIN_VALUE);
      SQL_LONG_MIN_VALUE = Long.toString(Long.MIN_VALUE);

      SQL_FLOAT_MIN_VALUE = Float.toString(DEFAULT_FLOAT);
      SQL_DOUBLE_MIN_VALUE = Double.toString(DEFAULT_DOUBLE);
   }

   private static final String  SYS_PROP__SILENT_DATABASE_UPDATE = "silentDatabaseUpdate";                                      //$NON-NLS-1$
   private static final boolean _isSilentDatabaseUpdate          = System.getProperty(SYS_PROP__SILENT_DATABASE_UPDATE) != null;

   static {

      if (_isSilentDatabaseUpdate) {

         Util.logSystemProperty_IsEnabled(TourDatabase.class,
               SYS_PROP__SILENT_DATABASE_UPDATE,
               "A database update is done silently"); //$NON-NLS-1$
      }
   }

   private boolean                               _isDbInitialized;
   private boolean                               _isDbInDataUpdate;
   private boolean                               _isInDesignUpdate;
   private boolean                               _isTableChecked;
   private boolean                               _isDataVersionChecked;
   private boolean                               _isDesignVersionChecked;

   private int                                   _dbDesignVersion_New;
   private int                                   _dbDesignVersion_Old;

   private final ListenerList<IPropertyListener> _propertyListeners      = new ListenerList<>(ListenerList.IDENTITY);

   private boolean                               _isSQLDesignUpdateError = false;
   private boolean                               _isSQLDataUpdateError   = false;

   /**
    * Database version before a db design update is performed
    */
   private int                                   _dbVersion_BeforeDesignUpdate;

   private boolean                               _isChecked_DbCreated;

   private boolean                               _isCustomFunctionSetup_AvgSpeedPace;

   /**
    * SQL utilities.
    */
   private static class SQL {

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Can be <code>null</code> then a default is not set
       *
       * @throws SQLException
       */
      private static void addColumn_BigInt(final Statement stmt,
                                           final String table,
                                           final String columnName,
                                           final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         if (defaultValue == null) {

            // ignore default value

            exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " BIGINT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

         } else {

            exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " BIGINT DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         }
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Default value.
       *
       * @throws SQLException
       */
      private static void addColumn_Boolean(final Statement stmt,
                                            final String table,
                                            final String columnName,
                                            final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " BOOLEAN DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      private static void addColumn_Double(final Statement stmt,
                                           final String table,
                                           final String columnName,
                                           final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " DOUBLE DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      private static void addColumn_Float(final Statement stmt,
                                          final String table,
                                          final String columnName,
                                          final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " FLOAT DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Default value.
       *
       * @throws SQLException
       */
      private static void addColumn_Int(final Statement stmt,
                                        final String table,
                                        final String columnName,
                                        final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " INTEGER DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Default value.
       *
       * @throws SQLException
       */
      private static void addColumn_SmallInt(final Statement stmt,
                                             final String table,
                                             final String columnName,
                                             final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " SMALLINT DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      /**
       * Creates a SQL statement to add a column for VARCHAR.
       *
       * @param stmt
       * @param table
       * @param columnName
       * @param columnWidth
       *
       * @throws SQLException
       */
      private static void addColumn_VarCar(final Statement stmt,
                                           final String table,
                                           final String columnName,
                                           final int columnWidth) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " VARCHAR(" + columnWidth + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }

      private static void alterColumn_VarChar_Width(final Statement stmt,
                                                    final String table,
                                                    final String columnName,
                                                    final int newWidth) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName) == false) {

            // column do not exist -> this should not happen

            throw new RuntimeException("Column do not exist: \"%s\"".formatted(columnName)); //$NON-NLS-1$
         }

         exec(stmt, "ALTER TABLE " + table + " ALTER COLUMN " + columnName + " SET DATA TYPE VARCHAR(" + newWidth + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Default value.
       *
       * @throws SQLException
       */
      private static void cleanup_DropColumn(final Statement stmt,
                                             final String table,
                                             final String columnName) {

         try {

            if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

               // column exist -> drop it

               exec(stmt, "ALTER TABLE " + table + " DROP COLUMN " + columnName); //$NON-NLS-1$ //$NON-NLS-2$
            }

         } catch (final SQLException e) {

            StatusUtil.log(e);
         }
      }

      private static void cleanup_DropConstraint(final Statement stmt,
                                                 final String tableName,
                                                 final String constraintName) throws SQLException {

         if (isConstraintAvailable(stmt.getConnection(), tableName, constraintName) == false) {

            // constraint is not available -> nothing to do

            return;
         }

         try {

            exec(stmt, "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName); //$NON-NLS-1$ //$NON-NLS-2$

         } catch (final SQLException e) {

            StatusUtil.log(e);
         }
      }

      private static void cleanup_DropTable(final Statement stmt, final String tableName) throws SQLException {

         if (isTableAvailable(stmt.getConnection(), tableName) == false) {

            // table do NOT exist -> nothing to do

            return;
         }

         try {

            exec(stmt, "DROP TABLE " + tableName); //$NON-NLS-1$

         } catch (final SQLException e) {

            StatusUtil.log(e);
         }
      }

      /**
       * Creates an ID field and set's the primary key.
       *
       * @param fieldName
       * @param isGenerateID
       *           When <code>true</code> an identity ID is created.
       *
       * @return
       */
      private static String createField_EntityId(final String fieldName, final boolean isGenerateID) {

         String generateID = UI.EMPTY_STRING;

         if (isGenerateID) {

            /**
             * Use GENERATED BY DEFAULT AS IDENTITY: Make sure your target table in H2 uses
             * GENERATED BY DEFAULT AS IDENTITY rather than GENERATED ALWAYS AS IDENTITY. GENERATED
             * BY DEFAULT allows you to insert explicit values (such as 0) directly into the
             * identity column, whereas GENERATED ALWAYS rejects user-supplied values unless system
             * overrides are enabled
             */
//          generateID = "GENERATED BY DEFAULT AS IDENTITY (START WITH 0 ,INCREMENT BY 1)"; //$NON-NLS-1$
            generateID = "GENERATED BY DEFAULT AS IDENTITY (START WITH 0)"; //$NON-NLS-1$
         }

//       SUBJECT CHAR(64) NOT NULL CONSTRAINT OUT_TRAY_PK PRIMARY KEY,

         return "   " //                                                //$NON-NLS-1$

               + fieldName + " BIGINT NOT NULL " //                     //$NON-NLS-1$
               + generateID
               + " CONSTRAINT " + fieldName + "_pk PRIMARY KEY" //      //$NON-NLS-1$ //$NON-NLS-2$
               + "," + NL; //                                           //$NON-NLS-1$
      }

      /**
       * Create a composite index
       *
       * @param stmt
       * @param tableName
       * @param allColumnNames
       *
       * @throws SQLException
       */
      private static void createIndex_Composite(final Statement stmt,
                                                final String tableName,
                                                final String... allColumnNames) throws SQLException {

         // an exception will be thrown when the index name is larger than the max which is 128 characters
         final String joinedNames = String.join("__", allColumnNames); //        //$NON-NLS-1$
         final String joinedSQLNames = String.join(", ", allColumnNames); //     //$NON-NLS-1$

         final String indexName = tableName + "___" + joinedNames; //            //$NON-NLS-1$

         if (isIndexAvailable(stmt.getConnection(), tableName, indexName)) {

            // index already exist -> nothing to do

            return;
         }

         final String sql = UI.EMPTY_STRING

               + "CREATE INDEX " + indexName //       //$NON-NLS-1$
               + " ON " + tableName //                //$NON-NLS-1$
               + " (" + joinedSQLNames + ")"; //      //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      /**
       * @param stmt
       * @param tableName
       * @param indexAndColumnName
       *
       * @throws SQLException
       */
      private static void createIndex_Simple(final Statement stmt,
                                             final String tableName,
                                             final String indexAndColumnName) throws SQLException {

         if (isIndexAvailable(stmt.getConnection(), tableName, indexAndColumnName)) {

            // index already exist -> nothing to do

            return;
         }

         final String sql = UI.EMPTY_STRING

               + "CREATE INDEX " + indexAndColumnName //    //$NON-NLS-1$
               + " ON " + tableName //                      //$NON-NLS-1$
               + " (" + indexAndColumnName + ")"; //        //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      /**
       * Combine tableName and columnName to the indexName, e.g. Table__Column
       *
       * @param stmt
       * @param tableName
       * @param columnName
       *
       * @throws SQLException
       */
      private static void createIndex_Table__Column(final Statement stmt,
                                                    final String tableName,
                                                    final String columnName) throws SQLException {

         final String combinedIndexName = tableName + "__" + columnName; //$NON-NLS-1$

         if (isIndexAvailable(stmt.getConnection(), tableName, combinedIndexName)) {

            // index already exist -> nothing to do

            return;
         }

         final String sql = UI.EMPTY_STRING

               + "CREATE INDEX " + combinedIndexName //     //$NON-NLS-1$
               + " ON " + tableName //                      //$NON-NLS-1$
               + " (" + columnName + ")"; //                //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      /**
       * @param stmt
       * @param table
       * @param oldColumnName
       * @param newColumnName
       *
       * @throws SQLException
       */
      private static void renameCol(final Statement stmt,
                                    final String table,
                                    final String oldColumnName,
                                    final String newColumnName) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, oldColumnName) == false) {

            // old column do not exist (is already renamed) -> nothing to do

            return;
         }

         exec(stmt, "RENAME COLUMN " + table + "." + oldColumnName + " TO " + newColumnName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
   }

   private TourDatabase() {

      final boolean isExternalDatabase = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      if (isExternalDatabase) {

// SET_FORMATTING_OFF

//       final String externalJarDownloadUrl             = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_DOWNLOAD_URL);
         final String externalJdbcDriver                 = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER);
         final String externalConnectionUrl              = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PROTOCOL);
         final String externalConnectionUrl_Parameters   = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PARAMETERS);
         final String externalDbUsername                 = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME);
         final String externalDbPassword                 = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD);
         final String externalDbName                     = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_NAME);
         final String externalHibernateDialect           = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_HIBERNATE_DIALECT);

         SQL_DB_DRIVER_CLASS        = externalJdbcDriver;
         SQL_DB_CONNECTION_URL      = externalConnectionUrl;
         SQL_DB_TOUR_DB_URL         = externalConnectionUrl + externalDbName + externalConnectionUrl_Parameters;

         SQL_DB_USERNAME            = externalDbUsername;
         SQL_DB_PASSWORD            = externalDbPassword;

         SQL_DB_HIBERNATE_DIALECT   = externalHibernateDialect;

      } else {

         // H2 database

         String h2DatabasePath = H2_DATABASE_FILEPATH ;

         if (UI.IS_WIN && h2DatabasePath.startsWith("/")) {
            h2DatabasePath = h2DatabasePath.substring(1);
         }

         // Log to standard console output.text
         // jdbc:h2:mem:testdb;TRACE_LEVEL_SYSTEM_OUT=2

         // Log to a trace file. This writes logs to a <databaseName>.trace.db file in the same directory as your database files.text
         // jdbc:h2:file:~/testdb;TRACE_LEVEL_FILE=2


         // H2 Trace Levels Explained
         //
         // Level 0. Tracing is completely disabled.
         // Level 1. Logs errors only (this is the default setting for file tracing).
         // Level 2. Logs errors and info messages, which includes executed SQL statements.
         // Level 3. Logs debug details, including SQL statements along with their bound parameter values.
         // Level 4. Delegates logging directly to SLF4J.

         String logging ;

         logging = ";TRACE_LEVEL_SYSTEM_OUT=3";                         //$NON-NLS-1$
         logging = ";TRACE_LEVEL_FILE=3";                               //$NON-NLS-1$
         logging = ";TRACE_LEVEL_SYSTEM_OUT=0;TRACE_LEVEL_FILE=2";      //$NON-NLS-1$

         SQL_DB_DRIVER_CLASS        = H2_DRIVER;
         SQL_DB_CONNECTION_URL      = H2_JDBC_CONNECTION_PROTOCOL;
         SQL_DB_TOUR_DB_URL         = H2_DB_TOUR_DB_URL + logging;

         SQL_DB_USERNAME            = UI.EMPTY_STRING;
         SQL_DB_PASSWORD            = UI.EMPTY_STRING;

         SQL_DB_HIBERNATE_DIALECT   = H2_EJB_HIBERNATE_DIALECT;
      }

// SET_FORMATTING_ON
   }

   /**
    * Removes all sensors which are loaded from the database so the next time they will be
    * reloaded.
    */
   public static synchronized void clearDeviceSensors() {

      if (_allDbDeviceSensors_BySensorID != null) {

         _allDbDeviceSensors_BySensorID.clear();
         _allDbDeviceSensors_BySensorID = null;
      }

      if (_allDbDeviceSensors_BySensorKey_WithDevType != null) {

         _allDbDeviceSensors_BySensorKey_WithDevType.clear();
         _allDbDeviceSensors_BySensorKey_WithDevType = null;
      }

      if (_allDbDeviceSensors_BySensorKey_NoDevType != null) {

         _allDbDeviceSensors_BySensorKey_NoDevType.clear();
         _allDbDeviceSensors_BySensorKey_NoDevType = null;
      }
   }

   /**
    * Remove all tour marker types
    */
   public static synchronized void clearTourMarkerTypes() {

      if (_allDbTourMarkerTypes != null) {

         _allDbTourMarkerTypes.clear();
         _allDbTourMarkerTypes_ById.clear();

         _allDbTourMarkerTypes = null;
         _allDbTourMarkerTypes_ById = null;
      }
   }

   /**
    * Removes all tour tags which are loaded from the database so the next time they will be
    * reloaded.
    */
   public static synchronized void clearTourTags() {

      if (_allTourTags_ByTagId != null) {

         _allTourTags_ByTagId.clear();
         _allTourTags_ByTagId = null;

         _allTourTags_ByTagName.clear();
         _allTourTags_ByTagName = null;
      }

      if (_allTourTagCategoriesByCategory != null) {
         _allTourTagCategoriesByCategory.clear();
         _allTourTagCategoriesByCategory = null;
      }

      if (_allTourTagCategoriesByTag != null) {
         _allTourTagCategoriesByTag.clear();
         _allTourTagCategoriesByTag = null;
      }

      if (_tagCollections != null) {
         _tagCollections.clear();
      }
   }

   /**
    * Remove all tour types and set their images dirty that the next time they have to be loaded
    * from the database and the images are recreated.
    */
   private static synchronized void clearTourTypes() {

      if (_allDbTourTypes != null) {

         _allDbTourTypes.clear();
         _allDbTourTypes_ByName.clear();
         _allDbTourTypes_ById.clear();

         _allDbTourTypes = null;
         _allDbTourTypes_ByName = null;
         _allDbTourTypes_ById = null;
      }

      TourTypeImage.setTourTypeImagesDirty();
   }

   /**
    * @return
    */
   /**
    * @param computeValuesRunner
    *           {@link IComputeTourValues} interface to compute values for one tour
    * @param tourIds
    *           Tour ID's which should be computed, when <code>null</code>, ALL tours will be
    *           computed.
    *
    * @return
    */
   public static boolean computeAnyValues_ForAllTours(final IComputeTourValues computeValuesRunner,
                                                      final List<Long> tourIds) {

      final int[] tourCounter = new int[] { 0 };
      final int[] tourListSize = new int[] { 0 };
      final boolean[] isCanceled = new boolean[] { false };

      /*
       * Runnable to compute values
       */
      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            List<Long> allTourIds;
            if (tourIds == null) {
               allTourIds = getAllTourIds();
            } else {
               allTourIds = tourIds;
            }
            tourListSize[0] = allTourIds.size();

            long lastUIUpdateTime = 0;

            monitor.beginTask(Messages.tour_database_computeComputeValues_mainTask, allTourIds.size());

            // loop over all tours and compute values
            for (final Long tourId : allTourIds) {

               final TourData dbTourData = getTourFromDb(tourId);
               TourData savedTourData = null;

               if (dbTourData != null) {
                  if (computeValuesRunner.computeTourValues(dbTourData)) {

                     // ensure that all computed values are set
                     dbTourData.computeComputedValues();

                     savedTourData = saveTour(dbTourData, false);
                  }
               }

               tourCounter[0]++;

               /*
                * This must be called in every iteration because it can compute values ! ! !
                */
               final String runnerSubTaskText = computeValuesRunner.getSubTaskText(savedTourData);

               final long currentTime = System.currentTimeMillis();
               if (currentTime > lastUIUpdateTime + 200) {

                  lastUIUpdateTime = currentTime;

                  // create sub task text
                  final StringBuilder sb = new StringBuilder();

                  // append: Processed tours: {0} of {1}
                  sb.append(NLS.bind(Messages.tour_database_computeComputeValues_subTask,
                        tourCounter[0],
                        tourListSize[0]));

                  // append: % of performed task
                  sb.append(UI.DASH_WITH_DOUBLE_SPACE);
                  sb.append(tourCounter[0] * 100 / tourListSize[0]);
                  sb.append(UI.SYMBOL_PERCENTAGE);

                  // append subsubtask text when available
                  if (StringUtils.hasContent(runnerSubTaskText)) {
                     sb.append(UI.DASH_WITH_DOUBLE_SPACE);
                     sb.append(runnerSubTaskText);
                  }

                  monitor.subTask(sb.toString());
               }
               monitor.worked(1);

               // check if canceled
               if (monitor.isCanceled()) {
                  isCanceled[0] = true;
                  break;
               }
            }
         }
      };

      final Shell shell = Display.getDefault().getActiveShell();

      try {

         new ProgressMonitorDialog(shell).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         e.printStackTrace();
      } finally {

         // create result text
         final StringBuilder sb = new StringBuilder();
         sb.append(NLS.bind(Messages.tour_database_computeComputedValues_resultMessage,
               tourCounter[0],
               tourListSize[0]));

         final String runnerResultText = computeValuesRunner.getResultText();
         if (runnerResultText != null) {
            sb.append(UI.NEW_LINE2);
            sb.append(runnerResultText);
         }

         MessageDialog.openInformation(
               shell,
               Messages.tour_database_computeComputedValues_resultTitle,
               sb.toString());
      }

      return isCanceled[0];
   }

   /**
    * @param tourRunner
    *           {@link IComputeNoDataserieValues} interface to compute values for one tour
    * @param tourIds
    *           Tour ID's which should be computed, when <code>null</code>, ALL tours will be
    *           computed.
    *
    * @return
    */
   public static boolean computeNoDataserieValues_ForAllTours(final IComputeNoDataserieValues tourRunner,
                                                              final List<Long> tourIds) {

      final int[] numCurrentlyProcessedTours = new int[] { 0 };
      final int[] numAllTours = new int[] { 0 };
      final boolean[] isCanceled = new boolean[] { false };

      /*
       * Runnable to compute values
       */
      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            try (Connection conn = TourDatabase.getInstance().getConnection()) {

               run_AllTours(conn, monitor);

            } catch (final SQLException e) {

               net.tourbook.common.util.SQL.showException(e);
            }
         }

         private void run_AllTours(final Connection conn, final IProgressMonitor monitor) throws SQLException {

            List<Long> allTourIds;
            if (tourIds == null) {
               allTourIds = getAllTourIds();
            } else {
               allTourIds = tourIds;
            }

            final int numTours = allTourIds.size();
            numAllTours[0] = numTours;

//            int numComputedTour = 0;
//            int numNotComputedTour = 0;
            long lastUIUpdateTime = 0;

            monitor.beginTask(Messages.tour_database_computeComputeValues_mainTask, numTours);

            final String sql = tourRunner.getSQLUpdateStatement();
            final PreparedStatement stmtUpdate = conn.prepareStatement(sql);

            // loop over all tours and compute values
            for (final Long tourId : allTourIds) {

               final TourData dbTourData = getTourFromDb(tourId);

               if (dbTourData != null) {

                  if (tourRunner.computeTourValues(dbTourData, stmtUpdate)) {

                     stmtUpdate.executeUpdate();

//                     numComputedTour++;

                  } else {

//                     numNotComputedTour++;
                  }
               }

               numCurrentlyProcessedTours[0]++;

               final long currentTime = System.currentTimeMillis();
               if (currentTime > lastUIUpdateTime + 200) {

                  lastUIUpdateTime = currentTime;

                  // create sub task text
                  final StringBuilder sb = new StringBuilder();
                  sb.append(NLS.bind(Messages.tour_database_computeComputeValues_subTask,
                        new Object[] { numCurrentlyProcessedTours[0], numTours, }));

                  sb.append(UI.DASH_WITH_DOUBLE_SPACE);
                  sb.append(numCurrentlyProcessedTours[0] * 100 / numTours);
                  sb.append(UI.SYMBOL_PERCENTAGE);

                  monitor.subTask(sb.toString());
               }
               monitor.worked(1);

               // check if canceled
               if (monitor.isCanceled()) {
                  isCanceled[0] = true;
                  break;
               }
            }
         }
      };

      final Display display = Display.getDefault();
      final Shell shell = display.getActiveShell();

      try {

         new ProgressMonitorDialog(shell).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
      } finally {

         // show final result delayed that the user can set the update UI
         display.asyncExec(() -> {

            // create result text
            final StringBuilder sb = new StringBuilder();
            sb.append(NLS.bind(Messages.tour_database_computeComputedValues_resultMessage,
                  numCurrentlyProcessedTours[0],
                  numAllTours[0]));

            // optional: add result from tour runner
            final String runnerResultText = tourRunner.getResultText();
            if (runnerResultText != null) {
               sb.append(UI.NEW_LINE2);
               sb.append(runnerResultText);
            }

            MessageDialog.openInformation(
                  shell,
                  Messages.tour_database_computeComputedValues_resultTitle,
                  sb.toString());
         });
      }

      return isCanceled[0];
   }

   /**
    * Remove a tour from the database
    *
    * @param tourId
    */
   public static boolean deleteTour(final long tourId) {

      boolean isTourRemovedFromEJB = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {
         final TourData tourData = em.find(TourData.class, tourId);

         if (tourData != null) {

            ts.begin();
            em.remove(tourData);
            ts.commit();
         }

      } catch (final Exception e) {

         e.printStackTrace();

         /*
          * an error could have been occurred when loading the tour with em.find, remove the tour
          * with sql commands
          */
         deleteTour_WithSQL(tourId);

      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            isTourRemovedFromEJB = true;
         }
         em.close();
      }

      if (isTourRemovedFromEJB) {

         deleteTour_WithSQL(tourId);

         FTSearchManager.deleteTourFromIndex(tourId);

         TourManager.getInstance().removeTourFromCache(tourId);
      }

      return true;
   }

   /**
    * Remove tour from all tables which contain data for the removed tour
    *
    * @param tourId
    *           Tour Id for the tour which is removed
    */
   private static void deleteTour_WithSQL(final long tourId) {

      PreparedStatement prepStmt = null;

      String sql = UI.EMPTY_STRING;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

// SET_FORMATTING_OFF

         final String sqlWhere_TourId           = " WHERE tourId=?";                         //$NON-NLS-1$
         final String sqlWhere_TourData_TourId  = " WHERE " + TABLE_TOUR_DATA + "_tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$

         final String[] allSql = {

            "DELETE FROM " + TABLE_TOUR_DATA                + sqlWhere_TourId,            //$NON-NLS-1$
            "DELETE FROM " + TABLE_TOUR_MARKER              + sqlWhere_TourData_TourId,   //$NON-NLS-1$
            "DELETE FROM " + TABLE_TOUR_PHOTO               + sqlWhere_TourData_TourId,   //$NON-NLS-1$
            "DELETE FROM " + TABLE_TOUR_WAYPOINT            + sqlWhere_TourData_TourId,   //$NON-NLS-1$
            "DELETE FROM " + TABLE_TOUR_REFERENCE           + sqlWhere_TourData_TourId,   //$NON-NLS-1$
            "DELETE FROM " + JOINTABLE__TOURDATA__TOURTAG   + sqlWhere_TourData_TourId,   //$NON-NLS-1$
            "DELETE FROM " + JOINTABLE__TOURDATA__EQUIPMENT + sqlWhere_TourData_TourId,   //$NON-NLS-1$
            "DELETE FROM " + TABLE_TOUR_COMPARED            + sqlWhere_TourId,            //$NON-NLS-1$
            "DELETE FROM " + TABLE_TOUR_GEO_PARTS           + sqlWhere_TourId,            //$NON-NLS-1$
         };

// SET_FORMATTING_ON

         for (final String sqlExec : allSql) {

            sql = sqlExec;

            prepStmt = conn.prepareStatement(sql);
            prepStmt.setLong(1, tourId);
            prepStmt.execute();
            prepStmt.close();
         }

      } catch (final SQLException e) {
         System.out.println(sql);
         UI.showSQLException(e);
      }
   }

   /**
    * Disable runtime statistics by putting this statement after the result set was read
    *
    * @param conn
    *
    * @throws SQLException
    */
   public static void disableRuntimeStatistic(final Connection conn) throws SQLException {

      CallableStatement cs;

      cs = conn.prepareCall("VALUES SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS()"); //$NON-NLS-1$
      cs.execute();

      // log runtime statistics
      final ResultSet rs = cs.getResultSet();

      System.out.println("--------------------------------"); //$NON-NLS-1$
      System.out.println("DERBY RUNTIME STATISTICS - START"); //$NON-NLS-1$
      System.out.println("--------------------------------"); //$NON-NLS-1$

      while (rs.next()) {
         System.out.println(rs.getString(1));
      }

      System.out.println("------------------------------"); //$NON-NLS-1$
      System.out.println("DERBY RUNTIME STATISTICS - END"); //$NON-NLS-1$
      System.out.println("------------------------------"); //$NON-NLS-1$

      cs.close();

      cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(0)"); //$NON-NLS-1$
      cs.execute();
      cs.close();

      cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(0)"); //$NON-NLS-1$
      cs.execute();
      cs.close();
   }

   /**
    * Get runtime statistics by putting this statement before the query is executed
    *
    * @param conn
    *
    * @throws SQLException
    */
   public static void enableRuntimeStatistics(final Connection conn) throws SQLException {

      CallableStatement cs;

      cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1)"); //$NON-NLS-1$
      cs.execute();
      cs.close();

      cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(1)"); //$NON-NLS-1$
      cs.execute();
      cs.close();
   }

   private static void exec(final Statement stmt, final String sql) throws SQLException {

      System.out.println("exec: " + sql); //$NON-NLS-1$

      stmt.execute(sql);
   }

   private static void exec(final Statement stmt, final String[] sqlStatements) throws SQLException {

      for (final String sql : sqlStatements) {
         exec(stmt, sql);
      }
   }

   private static void exec_IgnoreException(final Statement stmt, final String sql) {

      System.out.println("exec_IgnoreException: " + sql); //$NON-NLS-1$

      try {

         stmt.execute(sql);

      } catch (final SQLException e) {

         StatusUtil.logError(e.getMessage());
      }
   }

   private static void execUpdate(final Statement stmt, final String sql) throws SQLException {

      System.out.println("execUpdate: " + sql); //$NON-NLS-1$

      stmt.executeUpdate(sql);
   }

   /**
    * @param tourTypeList
    *
    * @return Returns a list with all {@link TourType}'s which are currently used (with filter) to
    *         display tours.<br>
    */
   public static ArrayList<TourType> getActiveTourTypes() {

      return _activeTourTypes;
   }

   /**
    * @return Returns a map with all {@link DeviceSensor} which are stored in the database, key is
    *         sensor ID
    */
   public static Map<Long, DeviceSensor> getAllDeviceSensors_BySensorID() {

      if (_allDbDeviceSensors_BySensorID != null) {
         return _allDbDeviceSensors_BySensorID;
      }

      loadAllDeviceSensors();

      return _allDbDeviceSensors_BySensorID;
   }

   /**
    * @return Returns a map with all {@link DeviceSensor} which are stored in the database, key is a
    *         combination of manufacturer/product and serial number but without a device type
    *         {@link #_allDbDeviceSensors_BySensorKey_NoDevType}
    */
   public static Map<String, DeviceSensor> getAllDeviceSensors_BySensorKey_NoDevType() {

      if (_allDbDeviceSensors_BySensorKey_NoDevType != null) {
         return _allDbDeviceSensors_BySensorKey_NoDevType;
      }

      loadAllDeviceSensors();

      return _allDbDeviceSensors_BySensorKey_NoDevType;
   }

   /**
    * @return Returns a map with all {@link DeviceSensor} which are stored in the database, key is a
    *         combination of manufacturer/product/device type and serial number
    */
   public static Map<String, DeviceSensor> getAllDeviceSensors_BySensorKey_WithDevType() {

      if (_allDbDeviceSensors_BySensorKey_WithDevType != null) {
         return _allDbDeviceSensors_BySensorKey_WithDevType;
      }

      loadAllDeviceSensors();

      return _allDbDeviceSensors_BySensorKey_WithDevType;
   }

   /**
    * @return Returns all tour id's without any filter, sorted by tour start date/time
    */
   public static ArrayList<Long> getAllTourIds() {

      final ArrayList<Long> tourIds = new ArrayList<>();

      try (Connection conn = getInstance().getConnection();
            Statement stmt = conn.createStatement()) {

         final String sql = UI.EMPTY_STRING

               + "SELECT tourId" + NL //                             //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //     //$NON-NLS-1$
               + " ORDER BY TourStartTime" + NL //                   //$NON-NLS-1$
         ;

         final ResultSet result = stmt.executeQuery(sql);

         while (result.next()) {
            tourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return tourIds;
   }

   public static ArrayList<Long> getAllTourIds_BetweenTwoDates(final LocalDate dateFrom, final LocalDate dateUntil) {

      final ArrayList<Long> tourIds = new ArrayList<>();

      PreparedStatement stmt = null;

      try (Connection conn = getInstance().getConnection()) {

         final ZoneId defaultTimeZone = TimeTools.getDefaultTimeZone();

         final ZonedDateTime dateStart = dateFrom.atStartOfDay(defaultTimeZone);
         final ZonedDateTime dateEnd = dateUntil.atStartOfDay(defaultTimeZone).plusDays(1);

         final long dateFromMS = dateStart.toInstant().toEpochMilli();
         final long dateUntilMS = dateEnd.toInstant().toEpochMilli();

         final String sql = UI.EMPTY_STRING

               + "SELECT tourId" //                                     //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA //             //$NON-NLS-1$
               + " WHERE TourStartTime >= ? AND TourStartTime < ?" //   //$NON-NLS-1$
               + " ORDER BY TourStartTime"; //                          //$NON-NLS-1$

         stmt = conn.prepareStatement(sql);
         stmt.setLong(1, dateFromMS);
         stmt.setLong(2, dateUntilMS);

         final ResultSet result = stmt.executeQuery();

         while (result.next()) {
            tourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      } finally {
         Util.closeSql(stmt);
      }

      return tourIds;
   }

   /**
    * @return Returns tour id's which are filtered by the fast app tour filter.
    *         <p>
    *         <b>Fast app tour filter</b>
    *         <p>
    *         Contains all app tour filters which are performed very fast, e.g. person, tour type.
    *         This filter do not contain e.g. geo compare or tag filters
    */
   public static ArrayList<Long> getAllTourIds_WithFastAppFilter() {

      final ArrayList<Long> tourIds = new ArrayList<>();

      try (Connection conn = getInstance().getConnection()) {

         // get app filter without geo location
         final AppFilter appFilter = new AppFilter(AppFilter.ONLY_FAST_APP_FILTERS);

         final String sql = UI.EMPTY_STRING

               + "SELECT tourId" + NL //                                   //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //           //$NON-NLS-1$
               + " WHERE 1=1 " + appFilter.getWhereClause() + NL //        //$NON-NLS-1$
               + " ORDER BY TourStartTime" + NL //                         //$NON-NLS-1$
         ;

         final PreparedStatement stmt = conn.prepareStatement(sql);

         appFilter.setParameters(stmt, 1);

         final ResultSet result = stmt.executeQuery();

         while (result.next()) {
            tourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return tourIds;
   }

   /**
    * @return Returns the backend of all tour marker types which are stored in the database sorted
    *         by name.
    */
   public static List<TourMarkerType> getAllTourMarkerTypes() {

      if (_allDbTourMarkerTypes != null) {
         return _allDbTourMarkerTypes;
      }

      loadAllTourMarkerTypes();

      return _allDbTourMarkerTypes;
   }

   public static Map<Long, TourMarkerType> getAllTourMarkerTypes_ById() {

      return _allDbTourMarkerTypes_ById;
   }

   /**
    * This method is synchronized to conform to FindBugs
    *
    * @return Returns all tour tag categories which are stored in the database, the hash key is the
    *         tag
    *         category id
    */
   public static HashMap<Long, TourTagCategory> getAllTourTagCategories() {

      if (_allTourTagCategoriesByCategory != null) {
         return _allTourTagCategoriesByCategory;
      }

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allTourTagCategoriesByCategory != null) {
            return _allTourTagCategoriesByCategory;
         }

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT tourTagCategory" //$NON-NLS-1$
                  + " FROM " + TourTagCategory.class.getSimpleName() + " AS tourTagCategory"); //$NON-NLS-1$ //$NON-NLS-2$

            _allTourTagCategoriesByCategory = new HashMap<>();

            final List<?> resultList = emQuery.getResultList();
            for (final Object result : resultList) {

               if (result instanceof final TourTagCategory tourTagCategory) {
                  _allTourTagCategoriesByCategory.put(tourTagCategory.getCategoryId(), tourTagCategory);
               }
            }

            em.close();
         }
      }

      return _allTourTagCategoriesByCategory;
   }

   /**
    * @return Returns all tour tags categories which are stored in the database, the hash key is the
    *         tag id
    */
   public static HashMap<Long, TourTagCategory> getAllTourTagCategoriesByTag() {

      if (_allTourTagCategoriesByTag != null) {
         return _allTourTagCategoriesByTag;
      }

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allTourTagCategoriesByTag != null) {
            return _allTourTagCategoriesByTag;
         }

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT tourTagCategory" //$NON-NLS-1$
                  + " FROM " + TourTagCategory.class.getSimpleName() + " AS tourTagCategory"); //$NON-NLS-1$ //$NON-NLS-2$

            _allTourTagCategoriesByTag = new HashMap<>();

            final List<?> allTagCategories = emQuery.getResultList();

            for (final Object result : allTagCategories) {

               if (result instanceof final TourTagCategory tourTagCategory) {

                  for (final TourTag tag : tourTagCategory.getTourTags()) {

                     _allTourTagCategoriesByTag.put(tag.getTagId(), tourTagCategory);
                  }
               }
            }

            em.close();
         }
      }

      return _allTourTagCategoriesByTag;
   }

   /**
    * @return Returns all tour tags which are stored in the database, the key is the tag id
    */
   public static HashMap<Long, TourTag> getAllTourTags() {

      if (_allTourTags_ByTagId != null) {
         return _allTourTags_ByTagId;
      }

      loadAllTourTags();

      return _allTourTags_ByTagId;
   }

   /**
    * @return Returns all tour tags, the key is the tag name in UPPERCASE
    */
   public static HashMap<String, TourTag> getAllTourTags_ByTagName() {

      if (_allTourTags_ByTagName != null) {
         return _allTourTags_ByTagName;
      }

      loadAllTourTags();

      return _allTourTags_ByTagName;
   }

   /**
    * @return Returns the backend of all tour types which are stored in the database sorted by name.
    */
   public static ArrayList<TourType> getAllTourTypes() {

      if (_allDbTourTypes != null) {
         return _allDbTourTypes;
      }

      loadAllTourTypes();

      return _allDbTourTypes;
   }

   /**
    * @return Returns the backend of all tour types which are stored in the database sorted by name.
    */
   private static HashMap<String, TourType> getAllTourTypes_ByName() {

      if (_allDbTourTypes_ByName != null) {
         return _allDbTourTypes_ByName;
      }

      loadAllTourTypes();

      return _allDbTourTypes_ByName;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllPhotoImageFilePath() {

      if (_dbPhotoImageFilePath == null) {

         synchronized (CACHE_LOCK) {

            // recheck again, another thread could have it created
            if (_dbPhotoImageFilePath == null) {

               _dbPhotoImageFilePath = getDistinctValues(TourDatabase.TABLE_TOUR_PHOTO, "imageFilePath"); //$NON-NLS-1$
            }
         }
      }

      return _dbPhotoImageFilePath;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllTourMarkerNames() {

      if (_dbTourMarkerNames == null) {

         synchronized (CACHE_LOCK) {

            // recheck again, another thread could have it created
            if (_dbTourMarkerNames == null) {

               _dbTourMarkerNames = getDistinctValues(TourDatabase.TABLE_TOUR_MARKER, "label"); //$NON-NLS-1$
            }
         }
      }

      return _dbTourMarkerNames;
   }

   /**
    * Getting all tour place ends from the database sorted by alphabet and without any double
    * entries.
    *
    * @author Stefan F.
    *
    * @return places as string array.
    */
   public static ConcurrentSkipListSet<String> getCachedFields_AllTourPlaceEnds() {

      if (_dbTourEndPlace == null) {

         synchronized (CACHE_LOCK) {

            // recheck again, another thread could have it created
            if (_dbTourEndPlace == null) {

               _dbTourEndPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourEndPlace"); //$NON-NLS-1$
            }
         }
      }

      return _dbTourEndPlace;
   }

   /**
    * Getting all tour start places from the database sorted by alphabet and without any double
    * entries.
    *
    * @author Stefan F.
    *
    * @return titles as string array.
    */
   public static ConcurrentSkipListSet<String> getCachedFields_AllTourPlaceStarts() {

      if (_dbTourStartPlace == null) {

         synchronized (CACHE_LOCK) {

            // recheck again, another thread could have it created
            if (_dbTourStartPlace == null) {

               _dbTourStartPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourStartPlace"); //$NON-NLS-1$
            }
         }
      }

      return _dbTourStartPlace;
   }

   /**
    * Getting all tour titles from the database sorted by alphabet and without any double entries.
    *
    * @author Stefan F.
    *
    * @return titles as string array.
    */
   public static ConcurrentSkipListSet<String> getCachedFields_AllTourTitles() {

      if (_dbTourTitles == null) {

         synchronized (CACHE_LOCK) {

            // recheck again, another thread could have it created
            if (_dbTourTitles == null) {

               _dbTourTitles = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourTitle"); //$NON-NLS-1$
            }
         }
      }

      return _dbTourTitles;
   }

   /**
    * Getting one row from the database sorted by alphabet and without any double entries.
    *
    * @author Stefan F.
    *
    * @param db
    *           Database table
    * @param fieldname
    *           Db field name
    *
    * @return Returns a sorted list with unique db field contents
    */
   public static ConcurrentSkipListSet<String> getDistinctValues(final String db, final String fieldname) {

      final ConcurrentSkipListSet<String> allSortedValues = new ConcurrentSkipListSet<>((text1, text2) -> {
         {
            // sort without case
            return text1.compareToIgnoreCase(text2);
         }
      });

      /*
       * Run in UI thread otherwise the busyindicator fails
       */
      final Display display = Display.getDefault();

      display.syncExec(() -> BusyIndicator.showWhile(display, () -> {

         try (Connection conn = getInstance().getConnection();
               Statement stmt = conn.createStatement()) {

            final String sqlQuery = UI.EMPTY_STRING

                  + "SELECT" + NL //                     //$NON-NLS-1$
                  + " DISTINCT " + fieldname + NL //     //$NON-NLS-1$
                  + " FROM " + db + NL //                //$NON-NLS-1$
                  + " ORDER BY " + fieldname + NL //     //$NON-NLS-1$
            ;

            final ResultSet result = stmt.executeQuery(sqlQuery);

            while (result.next()) {

               String dbValue = result.getString(1);
               if (dbValue != null) {

                  dbValue = dbValue.trim();

                  if (dbValue.length() > 0) {
                     allSortedValues.add(dbValue);
                  }
               }
            }

         } catch (final SQLException e) {
            UI.showSQLException(e);
         }

         /*
          * log existing values
          */
//                  final StringBuilder sb = new StringBuilder();
//                  for (final String text : sortedValues) {
//                     sb.append(text);
//                     sb.append(UI.NEW_LINE);
//                  }
//                  System.out.println(UI.NEW_LINE2);
//                  System.out.println(sqlQuery);
//                  System.out.println(UI.NEW_LINE);
//                  System.out.println(sb.toString());
      }));

      return allSortedValues;
   }

   /**
    * Getting one row from multiple databases sorted by alphabet and without any double entries
    *
    * @param fieldname
    *           DB field name
    * @param allDBNames
    *           Database tables
    *
    * @return Returns a sorted list with unique db field contents
    */
   public static ConcurrentSkipListSet<String> getDistinctValues(final String fieldname,
                                                                 final String... allDBNames) {

      final ConcurrentSkipListSet<String> allSortedValues = new ConcurrentSkipListSet<>((text1, text2) -> {
         {
            // sort without case
            return text1.compareToIgnoreCase(text2);
         }
      });

      /*
       * Run in UI thread otherwise the busyindicator fails
       */
      final Display display = Display.getDefault();

      display.syncExec(() -> BusyIndicator.showWhile(display, () -> {

         String sql = UI.EMPTY_STRING;

         try (Connection conn = getInstance().getConnection();
               Statement stmt = conn.createStatement()) {

            final StringBuilder sb = new StringBuilder();

            for (int dbIndex = 0; dbIndex < allDBNames.length; dbIndex++) {

               final String dbName = allDBNames[dbIndex];

               if (dbIndex > 0) {
                  sb.append("UNION" + NL); //$NON-NLS-1$
               }

               sb.append("SELECT " + fieldname + " FROM " + dbName + NL); //$NON-NLS-1$ //$NON-NLS-2$
            }

            sb.append("ORDER BY " + fieldname + NL); //$NON-NLS-1$

            sql = sb.toString();

            final ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {

               String dbValue = result.getString(1);
               if (dbValue != null) {

                  dbValue = dbValue.trim();

                  if (dbValue.length() > 0) {
                     allSortedValues.add(dbValue);
                  }
               }
            }

         } catch (final SQLException e) {

            StatusUtil.logError(NL + sql);
            UI.showSQLException(e);
         }
      }));

      return allSortedValues;
   }

   /**
    * Getting one row from multiple databases sorted by alphabet and without any double entries
    *
    * @param fieldname
    *           DB field name
    * @param exclude
    *           Startstring which excluded
    * @param allDBNames
    *           Database tables
    *
    * @return Returns a sorted list with unique db field contents
    */
   public static ConcurrentSkipListSet<String> getDistinctValuesWithExclude(final String fieldname,
                                                                            final String exclude,
                                                                            final String... allDBNames) {

      final ConcurrentSkipListSet<String> allSortedValues = new ConcurrentSkipListSet<>((text1, text2) -> {
         {
            // sort without case
            return text1.compareToIgnoreCase(text2);
         }
      });

      /*
       * Run in UI thread otherwise the busyindicator fails
       */
      final Display display = Display.getDefault();

      display.syncExec(() -> BusyIndicator.showWhile(display, () -> {

         String sql = UI.EMPTY_STRING;

         try (Connection conn = getInstance().getConnection();
               Statement stmt = conn.createStatement()) {

            final StringBuilder sb = new StringBuilder();

            for (int dbIndex = 0; dbIndex < allDBNames.length; dbIndex++) {

               final String dbName = allDBNames[dbIndex];

               if (dbIndex > 0) {
                  sb.append("UNION" + NL); //$NON-NLS-1$
               }

               sb.append(UI.EMPTY_STRING

                     + "SELECT " + fieldname //                                           //$NON-NLS-1$
                     + " FROM " + dbName //                                               //$NON-NLS-1$

               // % is a placeholder in SQL for 0..n characters
                     + " WHERE " + fieldname + " NOT LIKE '" + exclude + "%'" + NL); //   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            sb.append("ORDER BY " + fieldname + NL); //$NON-NLS-1$

            sql = sb.toString();

            final ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {

               String dbValue = result.getString(1);
               if (dbValue != null) {

                  dbValue = dbValue.trim();

                  if (dbValue.length() > 0) {
                     allSortedValues.add(dbValue);
                  }
               }
            }

         } catch (final SQLException e) {

            StatusUtil.logError(NL + sql);
            UI.showSQLException(e);
         }
      }));

      return allSortedValues;
   }

   public static TourDatabase getInstance() {

      if (_instance != null) {
         return _instance;
      }

      synchronized (DB_LOCK) {

         // check again

         if (_instance == null) {
            _instance = new TourDatabase();
         }
      }

      return _instance;
   }

   @SuppressWarnings("unchecked")
   public static TagCollection getRootTags() {

      final long rootTagId = -1L;

      TagCollection rootEntry = _tagCollections.get(Long.valueOf(rootTagId));
      if (rootEntry != null) {
         return rootEntry;
      }

      /*
       * read root tags from the database
       */
      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em == null) {
         return null;
      }

      rootEntry = new TagCollection();

      /*
       * read tag categories from db
       */
      Query emQuery = em.createQuery(UI.EMPTY_STRING

            + "SELECT ttCategory" //$NON-NLS-1$
            + " FROM " + TourTagCategory.class.getSimpleName() + " AS ttCategory" //$NON-NLS-1$ //$NON-NLS-2$
            + " WHERE ttCategory.isRoot=1" //$NON-NLS-1$
            + " ORDER BY ttCategory.name"); //$NON-NLS-1$

      rootEntry.tourTagCategories = (ArrayList<TourTagCategory>) emQuery.getResultList();

      /*
       * read tour tags from db
       */
      emQuery = em.createQuery(UI.EMPTY_STRING

            + "SELECT tourTag" //$NON-NLS-1$
            + " FROM " + TourTag.class.getSimpleName() + " AS tourTag " //$NON-NLS-1$ //$NON-NLS-2$
            + " WHERE tourTag.isRoot=1" //$NON-NLS-1$
            + " ORDER BY tourTag.name"); //$NON-NLS-1$

      rootEntry.tourTags = (ArrayList<TourTag>) emQuery.getResultList();

      em.close();

      _tagCollections.put(rootTagId, rootEntry);

      return rootEntry;
   }

   /**
    * @param tagCategoryId
    *
    * @return Returns the tag category notes
    *         <code>null</code>
    */
   public static String getTagCategoryNotes(final Long tagCategoryId) {

      if (tagCategoryId == null) {
         return UI.EMPTY_STRING;
      }

      final HashMap<Long, TourTagCategory> hashAllTagCategories = getAllTourTagCategories();
      final TourTagCategory tagCategory = hashAllTagCategories.get(tagCategoryId);

      if (tagCategory != null) {
         return tagCategory.getNotes();
      } else {
         try {
            throw new MyTourbookException("Tag category id '" + tagCategoryId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
         } catch (final MyTourbookException e) {
            e.printStackTrace();
         }
      }

      return UI.EMPTY_STRING;
   }

   /**
    * @param categoryId
    *
    * @return Returns a {@link TagCollection} with all tags and categories for the category Id
    */
   public static TagCollection getTagEntries(final long categoryId) {

      final Long categoryIdValue = Long.valueOf(categoryId);

      TagCollection categoryEntries = _tagCollections.get(categoryIdValue);
      if (categoryEntries != null) {
         return categoryEntries;
      }

      /*
       * read tag entries from the database
       */

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em == null) {
         return null;
      }

      categoryEntries = new TagCollection();

      final TourTagCategory tourTagCategory = em.find(TourTagCategory.class, categoryIdValue);

      // get tags
      final Set<TourTag> lazyTourTags = tourTagCategory.getTourTags();
      categoryEntries.tourTags = new ArrayList<>(lazyTourTags);
      Collections.sort(categoryEntries.tourTags);

      // get categories
      final Set<TourTagCategory> lazyTourTagCategories = tourTagCategory.getTagCategories();
      categoryEntries.tourTagCategories = new ArrayList<>(lazyTourTagCategories);
      Collections.sort(categoryEntries.tourTagCategories);

      em.close();

      _tagCollections.put(categoryIdValue, categoryEntries);

      return categoryEntries;
   }

   /**
    * @param allTagIds
    *
    * @return Returns the tag names separated with a comma or an empty string when tagIds are
    *         <code>null</code>
    */
   public static String getTagNames(final List<Long> allTagIds) {

      if (allTagIds == null) {
         return UI.EMPTY_STRING;
      }

      final HashMap<Long, TourTag> allTourTags = getAllTourTags();
      final ArrayList<String> allTagNames = new ArrayList<>();

      // get tag name for each tag id
      for (final Long tagId : allTagIds) {
         final TourTag tag = allTourTags.get(tagId);

         if (tag != null) {
            allTagNames.add(tag.getTagName());
         } else {
            try {
               throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (final MyTourbookException e) {
               e.printStackTrace();
            }
         }
      }

      return getTagNamesText(allTagNames, false);
   }

   /**
    * @param tourTags
    *
    * @return Returns the tag names separated with a comma or an empty string when not available.
    */
   public static String getTagNames(final Set<TourTag> tourTags) {

      if (tourTags.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final ArrayList<String> tagNames = new ArrayList<>();

      // get tag name for each tag id
      for (final TourTag tag : tourTags) {
         tagNames.add(tag.getTagName());
      }

      return getTagNamesText(tagNames, false);
   }

   public static String getTagNames(final Set<TourTag> tourTags, final boolean isVertical) {

      if (tourTags.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final ArrayList<String> tagNames = new ArrayList<>();

      // get tag name for each tag id
      for (final TourTag tag : tourTags) {
         tagNames.add(tag.getTagName());
      }

      return getTagNamesText(tagNames, isVertical);
   }

   private static String getTagNamesText(final List<String> tagNames, final boolean isVertical) {

      // sort tags by name
      Collections.sort(tagNames);

      final int numTags = tagNames.size();

      // convert list into visible string
      int tagIndex = 0;
      final StringBuilder sb = new StringBuilder();

      for (final String tagName : tagNames) {

         if (tagIndex++ > 0) {
            if (isVertical) {
               sb.append(NL);
            } else {
               sb.append(UI.COMMA_SPACE);
            }
         }

         if (isVertical && numTags > 1) {

            // prefix a bullet but only when multiple tags are available

            sb.append(net.tourbook.common.UI.SYMBOL_BULLET + UI.SPACE);
         }
         sb.append(tagName);
      }

      String tagNamesText = sb.toString();

      if (net.tourbook.common.UI.IS_SCRAMBLE_DATA) {

         tagNamesText = net.tourbook.common.UI.scrambleText(tagNamesText);
      }

      return tagNamesText;
   }

   public static String getTagNamesText(final Set<Long> alltagIds, final boolean isVertical) {

      // ensure tour tags are loaded
      getAllTourTags();

      final ArrayList<String> tagNames = new ArrayList<>();

      for (final Long tagId : alltagIds) {
         final TourTag tourTag = _allTourTags_ByTagId.get(tagId);
         tagNames.add(tourTag.getTagName());
      }

      return getTagNamesText(tagNames, isVertical);
   }

   /**
    * @param tagId
    * @param tagPropertyId
    *
    * @return Returns the tag's specified property
    */
   public static String getTagPropertyValue(final Long tagId, final String tagPropertyId) {

      if (tagId == null) {
         return UI.EMPTY_STRING;
      }

      final HashMap<Long, TourTag> hashAllTags = getAllTourTags();
      final TourTag tag = hashAllTags.get(tagId);

      if (tag != null) {

         switch (tagPropertyId) {

         case TreeColumnFactory.TOUR_TAG_IMAGE_FILE_PATH_ID:
            return tag.getImageFilePath();

         case TreeColumnFactory.TOUR_TAG_AND_CATEGORY_NOTES_ID:
            return tag.getNotes();

         default:
            return UI.EMPTY_STRING;
         }
      } else {
         try {
            throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
         } catch (final MyTourbookException e) {
            StatusUtil.log(e);
         }
      }

      return UI.EMPTY_STRING;
   }

   /**
    * @return Returns all tour beverage containers in the db sorted by name
    */
   public static List<TourBeverageContainer> getTourBeverageContainers() {

      final List<TourBeverageContainer> beverageContainersList = new ArrayList<>();

      final EntityManager entityManager = TourDatabase.getInstance().getEntityManager();

      if (entityManager != null) {

         final Query query = entityManager.createQuery(UI.EMPTY_STRING

               + "SELECT tourBeverageContainer" //$NON-NLS-1$
               + " FROM TourBeverageContainer AS tourBeverageContainer " //$NON-NLS-1$
               + " ORDER  BY tourBeverageContainer.name"); //$NON-NLS-1$

         for (final Object element : query.getResultList()) {

            if (element instanceof final TourBeverageContainer tourBeverageContainer) {
               beverageContainersList.add(tourBeverageContainer);
            }
         }

         entityManager.close();
      }

      return beverageContainersList;
   }

   /**
    * @return Returns all tour types in the db sorted by name
    */
   @SuppressWarnings("unchecked")
   public static ArrayList<TourBike> getTourBikes() {

      ArrayList<TourBike> bikeList = new ArrayList<>();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em != null) {

         final Query emQuery = em.createQuery(UI.EMPTY_STRING

               + "SELECT tourBike" //$NON-NLS-1$
               + " FROM TourBike AS tourBike " //$NON-NLS-1$
               + " ORDER  BY tourBike.name"); //$NON-NLS-1$

         bikeList = (ArrayList<TourBike>) emQuery.getResultList();

         em.close();
      }

      return bikeList;
   }

   /**
    * Get a tour from the database
    *
    * @param tourId
    *
    * @return Returns the tour data or <code>null</code> if the tour is not in the database
    */
   public static TourData getTourFromDb(final Long tourId) {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      final TourData tourData = em.find(TourData.class, tourId);

      em.close();

      return tourData;
   }

   public static TourLocation getTourLocation(final double latitude, final double longitude, final int zoomlevel) {

      final int latitudeE6 = Util.convertDouble_ToE6(latitude);
      final int longitudeE6 = Util.convertDouble_ToE6(longitude);

      return getTourLocation(latitudeE6, longitudeE6, zoomlevel);
   }

   /**
    * Searches for a tour location which is within all saved bounding boxes
    *
    * @param latitudeE6
    * @param longitudeE6
    * @param zoomlevel
    *
    * @return
    */
   private static TourLocation getTourLocation(final int latitudeE6, final int longitudeE6, final int zoomlevel) {

      TourLocation dbTourLocation = null;

      // convert possible negative values into positive values to make math easier
      final int latitudeE6_Normalized = latitudeE6 + 90_000_000;
      final int longitudeE6_Normalized = longitudeE6 + 180_000_000;

      synchronized (DB_LOCK) {

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         {
            final String sql = UI.EMPTY_STRING

                  + "SELECT TourLocation" + NL //                                               //$NON-NLS-1$

                  + " FROM TourLocation AS tourLocation" + NL //                                //$NON-NLS-1$

                  + " WHERE " + NL //                                                           //$NON-NLS-1$

                  + "   tourLocation.latitudeMinE6_Resized_Normalized  <= ? AND " + NL //    1  //$NON-NLS-1$
                  + "   tourLocation.latitudeMaxE6_Resized_Normalized  >= ? AND " + NL //    2  //$NON-NLS-1$

                  + "   tourLocation.longitudeMinE6_Resized_Normalized <= ? AND " + NL //    3  //$NON-NLS-1$
                  + "   tourLocation.longitudeMaxE6_Resized_Normalized >= ? AND " + NL //    4  //$NON-NLS-1$

                  + "   tourLocation.zoomlevel >= ?" + NL //                                 5  //$NON-NLS-1$
            ;

            final Query emQuery = em.createQuery(sql);

            emQuery.setParameter(1, latitudeE6_Normalized);
            emQuery.setParameter(2, latitudeE6_Normalized);
            emQuery.setParameter(3, longitudeE6_Normalized);
            emQuery.setParameter(4, longitudeE6_Normalized);
            emQuery.setParameter(5, zoomlevel);

            try {

               @SuppressWarnings("unchecked")
               final List<TourLocation> allDbTourLocations = emQuery.getResultList();

               final int numLocations = allDbTourLocations.size();

               if (numLocations == 0) {

                  // a new location will be created

               } else if (numLocations == 1) {

                  dbTourLocation = allDbTourLocations.get(0);

               } else {

                  // numLocations > 1

//                  for (final TourLocation dbTourLocation2 : allDbTourLocations) {
//
//                     final int latitudeMinE6_Normalized = dbTourLocation2.latitudeMinE6_Normalized;
//                     final int latitudeMaxE6_Normalized = dbTourLocation2.latitudeMaxE6_Normalized;
//                     final int longitudeMinE6_Normalized = dbTourLocation2.longitudeMinE6_Normalized;
//                     final int longitudeMaxE6_Normalized = dbTourLocation2.longitudeMaxE6_Normalized;
//
//                     System.out.println("""
//                           %s    %s
//                           %s    %s
//                           %s    %s     """.formatted( //$NON-NLS-1$
//
//                           FormatManager.formatNumber_0(latitudeMinE6_Normalized),
//                           FormatManager.formatNumber_0(longitudeMinE6_Normalized),
//
//                           FormatManager.formatNumber_0(latitudeE6_Normalized),
//                           FormatManager.formatNumber_0(longitudeE6_Normalized),
//
//                           FormatManager.formatNumber_0(latitudeMaxE6_Normalized),
//                           FormatManager.formatNumber_0(longitudeMaxE6_Normalized) //
//                     ));
//                  }
//
//                  System.out.println("Found %d locations, using first location".formatted(allDbTourLocations.size()));

                  dbTourLocation = allDbTourLocations.get(0);
               }

            } catch (final Exception e) {

               StatusUtil.log(e);
            }
         }
         em.close();
      }

      // set transient values
      if (dbTourLocation != null) {
         dbTourLocation.setTransientValues();
      }

      return dbTourLocation;
   }

   /**
    * Get {@link TourType} from all available tour type by it's id.
    *
    * @param tourTypeId
    *
    * @return Returns a {@link TourType} from the id or <code>null</code> when tour type is not
    *         available for the id.
    */
   public static TourType getTourType(final Long tourTypeId) {

      if (tourTypeId == null) {
         return null;
      }

      final long tourTypeL = tourTypeId.longValue();

      for (final TourType tourType : getAllTourTypes()) {
         if (tourType.getTypeId() == tourTypeL) {
            return tourType;
         }
      }

      return null;
   }

   /**
    * @param tourTypeId
    *
    * @return Returns the name for the {@link TourType} or "- No Tour Type -" when the tour type id
    *         was not found
    */
   public static String getTourTypeName(final long tourTypeId) {

      // ensure tour types are loaded
      getAllTourTypes();

      final TourType tourType = _allDbTourTypes_ById.get(tourTypeId);
      if (tourType == null) {

         /*
          * Use default text for the case when tour type is not defined and
          * net.tourbook.data.TourType.TOUR_TYPE_IS_NOT_USED or
          * net.tourbook.data.TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA is set.
          */
         return Messages.ui_tour_not_defined;

      } else {

         return tourType.getName();
      }
   }

   private static boolean isColumnAvailable(final Connection conn, final String table, final String column) {

      try {

         final DatabaseMetaData meta = conn.getMetaData();

         // very IMPORTANT: The table name must be UPPERCASE
         final ResultSet result = meta.getColumns(null, TABLE_SCHEMA, table.toUpperCase(), column.toUpperCase());

         while (result.next()) {
            return true;
         }

//         /*
//          * Dump all columns
//          */
//         final DatabaseMetaData meta2 = conn.getMetaData();
//         final ResultSet result2 = meta2.getColumns(null, TABLE_SCHEMA, table.toUpperCase(), null);
//
//         while (result2.next()) {
//
//            System.out.println("  "
//                  + result2.getString("TABLE_SCHEM") + ", "
//                  + result2.getString("TABLE_NAME") + ", "
//                  + result2.getString("COLUMN_NAME") + ", "
//                  + result2.getString("TYPE_NAME") + ", "
//                  + result2.getInt("COLUMN_SIZE") + ", "
//                  + result2.getString("NULLABLE"));
//         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   /**
    * @param conn
    * @param tableName
    * @param constraintName
    *
    * @return
    */
   private static boolean isConstraintAvailable(final Connection conn, final String tableName, final String constraintName) {

      try (Statement stmt = conn.createStatement()) {

         final String sqlQuery = UI.EMPTY_STRING

               + "SELECT" + NL //                                             //$NON-NLS-1$
               + "   C.CONSTRAINTNAME," + NL //                               //$NON-NLS-1$
               + "   T.TABLENAME," + NL //                                    //$NON-NLS-1$
               + "   C.TYPE" + NL //                                          //$NON-NLS-1$
               + "FROM" + NL //                                               //$NON-NLS-1$
               + "   SYS.SYSCONSTRAINTS C," + NL //                           //$NON-NLS-1$
               + "   SYS.SYSSCHEMAS S," + NL //                               //$NON-NLS-1$
               + "   SYS.SYSTABLES T" + NL //                                 //$NON-NLS-1$
               + "WHERE" + NL //                                              //$NON-NLS-1$
               + "   C.SCHEMAID = S.SCHEMAID" + NL //                         //$NON-NLS-1$
               + "   AND C.TABLEID = T.TABLEID" + NL //                       //$NON-NLS-1$

               // NAMES must be UPPERCASE !!!
               + "   AND T.TABLENAME  = '" + tableName.toUpperCase() + "'" + NL //          //$NON-NLS-1$ //$NON-NLS-2$
               + "   AND C.CONSTRAINTNAME ='" + constraintName.toUpperCase() + "'" //       //$NON-NLS-1$ //$NON-NLS-2$
         ;

         final ResultSet result = stmt.executeQuery(sqlQuery);

         while (result.next()) {
            return true;
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   /**
    * Checks if a field exceeds the max length
    *
    * @param field
    * @param maxLength
    * @param uiFieldName
    *
    * @return Returns {@link FIELD_VALIDATION} status
    */
   public static FIELD_VALIDATION isFieldValidForSave(final String field,
                                                      final int maxLength,
                                                      final String uiFieldName) {

      return isFieldValidForSave(field, maxLength, uiFieldName, false);
   }

   /**
    * Checks if a field exceeds the max length
    *
    * @param field
    * @param maxLength
    * @param uiFieldName
    * @param isForceTruncation
    *
    * @return Returns {@link FIELD_VALIDATION} status
    */
   public static FIELD_VALIDATION isFieldValidForSave(final String field,
                                                      final int maxLength,
                                                      final String uiFieldName,
                                                      final boolean isForceTruncation) {

      final FIELD_VALIDATION[] returnValue = { FIELD_VALIDATION.IS_VALID };

      if (field != null && field.length() > maxLength) {

         Display.getDefault().syncExec(() -> {

            if (isForceTruncation) {
               returnValue[0] = FIELD_VALIDATION.TRUNCATE;
               StatusUtil.log(
                     new Exception(
                           NLS.bind(
                                 "Field \"{0}\" with content \"{1}\" is truncated to {2} characters.", //$NON-NLS-1$
                                 new Object[] { uiFieldName, field, maxLength })));
               return;
            }

            if (MessageDialog.openConfirm(
                  Display.getDefault().getActiveShell(),
                  Messages.Tour_Database_Dialog_ValidateFields_Title,
                  NLS.bind(Messages.Tour_Database_Dialog_ValidateFields_Message,
                        new Object[] { uiFieldName, field.length(), maxLength }))) {

               returnValue[0] = FIELD_VALIDATION.TRUNCATE;
            } else {
               returnValue[0] = FIELD_VALIDATION.IS_INVALID;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean isIndexAvailable(final Connection conn, final String table, final String columnName) {

      try {

         final String requestedIndexColumnName = columnName.toUpperCase();

         final DatabaseMetaData meta = conn.getMetaData();
         final ResultSet result = meta.getIndexInfo(null, TABLE_SCHEMA, table, false, false);

         while (result.next()) {

            final String dbIndexColumnName = result.getString("COLUMN_NAME"); //$NON-NLS-1$

            if (requestedIndexColumnName.equals(dbIndexColumnName.toUpperCase())) {

               return true;
            }

//            System.out.println("Table Indices\n");
//            System.out.println(
//
//                  String.format(
//                        "%-20s",
//                        columnName
//
//                  ) //
//            );
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   private static boolean isPrimaryKeyAvailable(final Connection conn, final String table, final String key) {

      try {

         /**
          * Finally found a solution here
          * https://github.com/splicemachine/spliceengine/blob/master/splice_machine/src/test/java/com/splicemachine/derby/impl/sql/catalog/SqlProcedureColsIT.java
          * to get the primary keys
          */

         /**
          * Table MUST be UPPERCASE, otherwise it is not working !!!
          */
         final String tableUpper = table.toUpperCase();
         final String sql = "CALL SYSIBM.SQLPRIMARYKEYS(NULL, NULL, '" + tableUpper + "', NULL)"; //$NON-NLS-1$ //$NON-NLS-2$

         final Statement stmt = conn.createStatement();
         final ResultSet rs = stmt.executeQuery(sql);

         final String keyUpper = key.toUpperCase();

         while (rs.next()) {

            final String dbKeyName = rs.getString("PK_NAME"); //$NON-NLS-1$

            if (keyUpper.equals(dbKeyName.toUpperCase())) {

               return true;
            }
         }

         stmt.close();

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   private static boolean isTableAvailable(final Connection conn, final String table) {

      try {

         final DatabaseMetaData meta = conn.getMetaData();
         final ResultSet result = meta.getTables(null, TABLE_SCHEMA, table.toUpperCase(), null);

         while (result.next()) {
            return true;
         }

//         /*
//          * dump all columns
//          */
//         final DatabaseMetaData meta = conn.getMetaData();
////         final ResultSet result = meta.getTables(null, null, null, new String[] {"TABLE"});
//         final ResultSet result = meta.getTables(null, null, null, null);
//
//         while (result.next()) {
//            System.out.println(("   " + result.getString("TABLE_CAT"))
//                  + (", " + result.getString("TABLE_SCHEM"))
//                  + (", " + result.getString("TABLE_NAME"))
//                  + (", " + result.getString("TABLE_TYPE"))
//                  + (", " + result.getString("REMARKS")));
//         }
//
////       TABLE_CAT String                   => table catalog (may be null)
////       TABLE_SCHEM String                 => table schema (may be null)
////       TABLE_NAME String                  => table name
////       TABLE_TYPE String                  => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
////       REMARKS String                     => explanatory comment on the table
////       TYPE_CAT String                    => the types catalog (may be null)
////       TYPE_SCHEM String                  => the types schema (may be null)
////       TYPE_NAME String                   => type name (may be null)
////       SELF_REFERENCING_COL_NAME String   => name of the designated "identifier" column of a typed table (may be null)
////       REF_GENERATION String              => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   private static void loadAllDeviceSensors() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allDbDeviceSensors_BySensorKey_WithDevType != null) {
            return;
         }

         final Map<Long, DeviceSensor> allDbDeviceSensors_BySensorID = new HashMap<>();
         final Map<String, DeviceSensor> allDbDeviceSensors_BySensorKey_NoDevType = new HashMap<>();
         final Map<String, DeviceSensor> allDbDeviceSensors_BySensorKey_WithDevType = new HashMap<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT DeviceSensor" //                      //$NON-NLS-1$
                  + " FROM DeviceSensor AS DeviceSensor" //       //$NON-NLS-1$
            );

            final List<?> resultList = emQuery.getResultList();

            for (final Object result : resultList) {

               if (result instanceof final DeviceSensor sensor) {

                  allDbDeviceSensors_BySensorID.put(sensor.getSensorId(), sensor);

                  final String sensorKey_WithDevType = sensor.getSensorKey_WithDevType();
                  final String sensorKey_NoDevType = sensor.getSensorKey_NoDevType();

                  if (StringUtils.hasContent(sensorKey_WithDevType)) {
                     allDbDeviceSensors_BySensorKey_WithDevType.put(sensorKey_WithDevType.toUpperCase(), sensor);
                  }

                  if (StringUtils.hasContent(sensorKey_NoDevType)) {
                     allDbDeviceSensors_BySensorKey_NoDevType.put(sensorKey_NoDevType.toUpperCase(), sensor);
                  }
               }
            }

            em.close();
         }

         _allDbDeviceSensors_BySensorID = allDbDeviceSensors_BySensorID;
         _allDbDeviceSensors_BySensorKey_WithDevType = allDbDeviceSensors_BySensorKey_WithDevType;
         _allDbDeviceSensors_BySensorKey_NoDevType = allDbDeviceSensors_BySensorKey_NoDevType;
      }
   }

   @SuppressWarnings("unchecked")
   private static void loadAllTourMarkerTypes() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allDbTourMarkerTypes != null) {
            return;
         }

         List<TourMarkerType> allDbTourMarkerTypes = new ArrayList<>();
         final Map<Long, TourMarkerType> allDbTourMarkerTypes_ById = new HashMap<>();
         final Map<String, TourMarkerType> allDbTourMarkerTypes_ByName = new HashMap<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT TourMarkerType" + NL //                     //$NON-NLS-1$

                  + " FROM " + TourMarkerType.class.getSimpleName() + " AS tourMarkerType" + NL //    //$NON-NLS-1$ //$NON-NLS-2$

                  // sort by name
                  + " ORDER  BY tourMarkerType.name" + NL //            //$NON-NLS-1$
            );

            allDbTourMarkerTypes = emQuery.getResultList();

            for (final TourMarkerType tourMarkerType : allDbTourMarkerTypes) {

               allDbTourMarkerTypes_ById.put(tourMarkerType.getId(), tourMarkerType);
               allDbTourMarkerTypes_ByName.put(tourMarkerType.getTypeName().toUpperCase(), tourMarkerType);
            }

            em.close();
         }

         _allDbTourMarkerTypes = allDbTourMarkerTypes;
         _allDbTourMarkerTypes_ById = allDbTourMarkerTypes_ById;
      }
   }

   private static void loadAllTourTags() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allTourTags_ByTagId != null) {
            return;
         }

         final HashMap<Long, TourTag> allTourTags_ByTagId = new HashMap<>();
         final HashMap<String, TourTag> allTourTags_ByTagName = new HashMap<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT tourTag" //                                             //$NON-NLS-1$
                  + " FROM " + TourTag.class.getSimpleName() + " AS tourTag"); //   //$NON-NLS-1$ //$NON-NLS-2$

            final List<?> resultList = emQuery.getResultList();
            for (final Object result : resultList) {

               if (result instanceof final TourTag tourTag) {

                  allTourTags_ByTagId.put(tourTag.getTagId(), tourTag);
                  allTourTags_ByTagName.put(tourTag.getTagName().toUpperCase(), tourTag);
               }
            }

            em.close();
         }

         _allTourTags_ByTagId = allTourTags_ByTagId;
         _allTourTags_ByTagName = allTourTags_ByTagName;
      }
   }

   @SuppressWarnings("unchecked")
   private static void loadAllTourTypes() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allDbTourTypes != null) {
            return;
         }

         ArrayList<TourType> allDbTourTypes = new ArrayList<>();
         final HashMap<Long, TourType> allDbTourTypes_ById = new HashMap<>();
         final HashMap<String, TourType> allDbTourTypes_ByName = new HashMap<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT tourType" //              //$NON-NLS-1$
                  + " FROM TourType AS tourType" //   //$NON-NLS-1$
                  + " ORDER  BY tourType.name"); //   //$NON-NLS-1$

            allDbTourTypes = (ArrayList<TourType>) emQuery.getResultList();

            for (final TourType tourType : allDbTourTypes) {

               allDbTourTypes_ById.put(tourType.getTypeId(), tourType);
               allDbTourTypes_ByName.put(tourType.getName().toUpperCase(), tourType);
            }

            em.close();
         }

         _allDbTourTypes = allDbTourTypes;
         _allDbTourTypes_ById = allDbTourTypes_ById;
         _allDbTourTypes_ByName = allDbTourTypes_ByName;
      }
   }

   static void logDbUpdate(final String info) {

      System.out.println(net.tourbook.common.UI.timeStamp() + "[DB Update] " + info); //$NON-NLS-1$
   }

   private static void logDbUpdate_End(final int dbVersion) {

      System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, dbVersion));
      System.out.println();
   }

   private static void logDbUpdate_Start(final int dbVersion) {

      System.out.println();
      System.out.println(NLS.bind(Messages.Tour_Database_Update, dbVersion));
   }

   private static void logSqlCommand(final String dbUrl) {

      System.out.println(net.tourbook.common.UI.timeStamp() + "[Executing SQL command] " + dbUrl); //$NON-NLS-1$
   }

   /**
    * Persists a new entity or merge an entity
    * <p>
    * This method is <b>much faster</b> than using this
    * {@link #saveEntity(Object, long, Class, EntityManager)}
    * <p>
    *
    * @param entity
    * @param id
    * @param entityClass
    *
    * @return Returns the saved entity.
    */
   public static <T> T saveEntity(final T entity, final long id, final Class<?> entityClass) {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      T savedEntity = null;
      boolean isSaved = false;

      try {

         ts.begin();
         {
            final Object entityInDB = em.find(entityClass, id);

            if (entityInDB == null) {

               // entity is not persisted

               em.persist(entity);
               savedEntity = entity;

            } else {

               savedEntity = em.merge(entity);
            }
         }
         ts.commit();

      } catch (final Exception e) {

         StatusUtil.showStatus(e);

      } finally {

         if (ts.isActive()) {
            ts.rollback();
         } else {
            isSaved = true;
         }
         em.close();
      }

      if (isSaved == false) {

         Display.getDefault().asyncExec(() -> {

            MessageDialog.openError(Display.getDefault().getActiveShell(),
                  "Error", //$NON-NLS-1$
                  "Error occurred when saving entity: " + entity); //$NON-NLS-1$
         });
      }

      return savedEntity;
   }

   /**
    * Persists an entity, an error is logged when saving fails.
    * <p>
    * This method is <b>much slower</b> than using this {@link #saveEntity(Object, long, Class)}
    * method without using the same EntityManager.
    *
    * @param entity
    * @param id
    * @param entityClass
    *
    * @return Returns the saved entity
    */
   @SuppressWarnings("unused")
   private static <T> T saveEntity(final T entity, final long id, final Class<T> entityClass, final EntityManager em) {

      final EntityTransaction ts = em.getTransaction();

      T savedEntity = null;
      boolean isSaved = false;

      try {

         ts.begin();
         {
            final T entityInDB = em.find(entityClass, id);

            if (entityInDB == null) {

               // entity is not persisted

               em.persist(entity);
               savedEntity = entity;

            } else {

               savedEntity = em.merge(entity);
            }
         }
         ts.commit();

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            isSaved = true;
         }
      }

      if (isSaved == false) {

         MessageDialog.openError(Display.getDefault().getActiveShell(),
               "Error", //$NON-NLS-1$
               "Error occurred when saving an entity"); //$NON-NLS-1$
      }

      return savedEntity;
   }

   /**
    * Persist {@link TourData} in the database and updates the tour data cache with the persisted
    * tour<br>
    * <br>
    * When a tour has no person the tour will not be saved, a person must be set first before the
    * tour can be saved
    *
    * @param tourData
    * @param isUpdateModifiedDate
    *           When <code>true</code> the modified date is updated. For updating computed field it
    *           does not make sense to set the modified date.
    *
    * @return persisted {@link TourData} or <code>null</code> when saving fails
    */
   public static TourData saveTour(final TourData tourData, final boolean isUpdateModifiedDate) {

      if (saveTour_PreSaveActions(tourData) == false) {
         return null;
      }

      EntityManager em = TourDatabase.getInstance().getEntityManager();

      TourData persistedEntity = null;

      if (em != null) {

         final EntityTransaction ts = em.getTransaction();

         try {

            tourData.onPrePersist();

            ts.begin();
            {
               final long dtSaved = TimeTools.createdNowAsYMDhms();

               final TourData tourDataEntity = em.find(TourData.class, tourData.getTourId());
               if (tourDataEntity == null) {

                  // tour is not yet persisted

                  tourData.setDateTimeCreated(dtSaved);

                  em.persist(tourData);

                  persistedEntity = tourData;

               } else {

                  if (isUpdateModifiedDate) {
                     tourData.setDateTimeModified(dtSaved);
                  }

                  persistedEntity = em.merge(tourData);
               }
            }
            ts.commit();

         } catch (final Exception e) {

            StatusUtil.logError("Exception in tour " + TourManager.getTourDateTimeShort(tourData));//$NON-NLS-1$
            StatusUtil.showStatus(Messages.Tour_Database_TourSaveError, e);

         } finally {
            if (ts.isActive()) {
               ts.rollback();
            }
            em.close();
         }
      }

      if (persistedEntity != null) {

         em = TourDatabase.getInstance().getEntityManager();
         try {

            persistedEntity = em.find(TourData.class, tourData.getTourId());

         } catch (final Exception e) {
            StatusUtil.log(e);
         }

         em.close();

         saveTour_PostSaveActions(persistedEntity, tourData);
      }

      return persistedEntity;
   }

   /**
    * This method {@link #saveTour_PostSaveActions_Concurrent_2_ForAllTours(long[])} <b>MUST</b> be
    * called <b>AFTER</b> all tours are saved
    *
    * @param tourData
    * @param isUpdateModifiedDate
    *
    * @return
    */
   public static TourData saveTour_Concurrent(final TourData tourData,
                                              final boolean isUpdateModifiedDate) {

      if (saveTour_PreSaveActions(tourData) == false) {
         return null;
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      TourData persistedEntity = null;

      if (em != null) {

         final EntityTransaction ts = em.getTransaction();

         try {

            tourData.onPrePersist();

            ts.begin();
            {
               final long dtSaved = TimeTools.createdNowAsYMDhms();

               // get tour data by tour id
               final TourData dbTourData = em.find(TourData.class, tourData.getTourId());
               if (dbTourData == null) {

                  // tour is not yet persisted

                  tourData.setDateTimeCreated(dtSaved);

                  em.persist(tourData);

                  persistedEntity = tourData;

               } else {

                  if (isUpdateModifiedDate) {
                     tourData.setDateTimeModified(dtSaved);
                  }

                  persistedEntity = em.merge(tourData);
               }
            }
            ts.commit();

         } catch (final Exception e) {

            StatusUtil.logError("Exception in tour %s - ID: %s".formatted( //$NON-NLS-1$
                  TourManager.getTourDateTimeFull(tourData),
                  tourData.getTourId()));

            StatusUtil.showStatus(e);

         } finally {

            if (ts.isActive()) {
               ts.rollback();
            }

            em.close();
         }

         // do post save actions for only ONE tour
         saveTour_PostSaveActions_Concurrent_1_ForOneTour(persistedEntity, tourData);

         // !!! This method MUST be called AFTER all tours are saved !!!
         // !!! This method MUST be called AFTER all tours are saved !!!
         // !!! This method MUST be called AFTER all tours are saved !!!

//       saveTour_PostSaveActions_Concurrent_2_ForAllTours(allTourIds);
      }

      return persistedEntity;
   }

   private static void saveTour_GeoParts(final TourData tourData) {

//      final long startTime = System.nanoTime();

      PreparedStatement deleteStmt = null;
      PreparedStatement insertStmt = null;

      String sql = UI.EMPTY_STRING;

      int[] tourGeoParts = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final long tourId = tourData.getTourId();

         /*
          * Delete old geo parts
          */
         sql = "DELETE FROM " + TABLE_TOUR_GEO_PARTS + " WHERE tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$

         deleteStmt = conn.prepareStatement(sql);
         deleteStmt.setLong(1, tourId);
         deleteStmt.execute();

         /*
          * Save new geo parts
          */
         tourData.computeGeo_Grid();
         tourGeoParts = tourData.geoGrid;

         if (tourGeoParts != null) {

            sql = "INSERT INTO " + TABLE_TOUR_GEO_PARTS //$NON-NLS-1$
                  + " (TourId, GeoPart)" //$NON-NLS-1$
                  + " VALUES (?, ?)"; //$NON-NLS-1$

            insertStmt = conn.prepareStatement(sql);

            conn.setAutoCommit(false);
            {
               for (final int geoPart : tourGeoParts) {

                  insertStmt.setLong(1, tourId);
                  insertStmt.setInt(2, geoPart);

                  insertStmt.addBatch();
               }

               insertStmt.executeBatch();
            }
            conn.commit();
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      } finally {
         Util.closeSql(deleteStmt);
         Util.closeSql(insertStmt);
      }

//      System.out.println(
//            String.format(
//                  "%s" // tour date //$NON-NLS-1$
//                        + "  %s tour" // tour time //$NON-NLS-1$
//
//                        + "  %5d time slices" // time slices //$NON-NLS-1$
//                        + "  %5d parts" // parts //$NON-NLS-1$
//                        + "  %5.2f ms", // sql time //$NON-NLS-1$
//
//                  TourManager.getTourDateShort(tourData),
//                  TourManager.getTourTimeShort(tourData),
//
//                  tourData.timeSerie == null ? 0 : tourData.timeSerie.length,
//                  tourGeoParts == null ? 0 : tourGeoParts.length,
//                  (System.nanoTime() - startTime) / 1_000_000.0)
//
//      );
   }

   /**
    * Keep transient values
    *
    * @param persistedEntity
    * @param oldTourData
    */
   private static void saveTour_KeepTransientValues(final TourData persistedEntity,
                                                    final TourData oldTourData) {

      persistedEntity.tourLocationData_Start = oldTourData.tourLocationData_Start;
      persistedEntity.tourLocationData_End = oldTourData.tourLocationData_End;
   }

   private static void saveTour_PostSaveActions(final TourData persistedEntity, final TourData oldTourData) {

      TourManager.getInstance().updateTourInCache(persistedEntity);

      updateCachedFields(persistedEntity);

      saveTour_GeoParts(persistedEntity);
      saveTour_KeepTransientValues(persistedEntity, oldTourData);

      /*
       * Update ft index
       */
      final ArrayList<Long> allTourIds = new ArrayList<>();
      allTourIds.add(persistedEntity.getTourId());

      FTSearchManager.updateIndex(allTourIds);
   }

   /**
    * Perform concurrent actions after a tour is saved
    *
    * @param persistedEntity
    * @param oldTourData
    */
   private static void saveTour_PostSaveActions_Concurrent_1_ForOneTour(final TourData persistedEntity,
                                                                        final TourData oldTourData) {

      TourManager.getInstance().updateTourInCache(persistedEntity);

      updateCachedFields(persistedEntity);

      saveTour_GeoParts(persistedEntity);
      saveTour_KeepTransientValues(persistedEntity, oldTourData);
   }

   /**
    * Perform concurrent actions after multiple tours are saved, e.g. update fulltext index
    *
    * @param allTourIDs
    */
   public static void saveTour_PostSaveActions_Concurrent_2_ForAllTours(final List<Long> allTourIDs) {

      // do this expensive action only once for all tours
      FTSearchManager.updateIndex(allTourIDs);
   }

   /**
    * Validates a tour before it is saved
    *
    * @param tourData
    *
    * @return Returns <code>true</code> when validation is OK, otherwise <code>false</code>
    */
   private static boolean saveTour_PreSaveActions(final TourData tourData) {

      /*
       * Prevent saving a tour which was deleted before
       */
      if (tourData.isTourDeleted) {
         return false;
      }

      /*
       * History tour or multiple tours cannot be saved
       */
      if (tourData.isHistoryTour || tourData.isMultipleTours()) {
         return false;
      }

      /*
       * Prevent saving a tour when a person is not set, this check is for internal use that all
       * data are valid
       */
      if (tourData.getTourPerson() == null) {
         StatusUtil.logInfo("Cannot save a tour without a person: " + tourData); //$NON-NLS-1$
         return false;
      }

      /*
       * Check size of VARCAR sql fields
       */
      if (tourData.isValidForSave() == false) {
         return false;
      }

      /*
       * Removed cached data
       */
      TourManager.clearMultipleTourData();

      /**
       * Ensure HR zones are computed, it requires that a person is set which is not the case when a
       * device importer calls the method {@link TourData#computeComputedValues()}
       */
      tourData.getNumberOfHrZones();

      saveTransientInstances(tourData);

      return true;
   }

   /**
    * This error can occur when transient instances are not yet saved which belong to
    * {@link TourData}.
    *
    * <pre>
    *
    * !ENTRY net.tourbook.common 4 0 2015-05-08 16:10:55.578
    * !MESSAGE Tour cannot be saved in the database
    * !STACK 0
    * org.hibernate.TransientObjectException:
    *
    *    object references an unsaved transient instance - save the transient instance before flushing:
    *      net.tourbook.data.TourData.tourType -> net.tourbook.data.TourType
    *
    *    at org.hibernate.engine.CascadingAction$9.noCascade(CascadingAction.java:376)
    *    at org.hibernate.engine.Cascade.cascade(Cascade.java:163)
    *    at org.hibernate.event.def.AbstractFlushingEventListener.cascadeOnFlush(AbstractFlushingEventListener.java:154)
    *    at org.hibernate.event.def.AbstractFlushingEventListener.prepareEntityFlushes(AbstractFlushingEventListener.java:145)
    *    at org.hibernate.event.def.AbstractFlushingEventListener.flushEverythingToExecutions(AbstractFlushingEventListener.java:88)
    *    at org.hibernate.event.def.DefaultFlushEventListener.onFlush(DefaultFlushEventListener.java:49)
    *    at org.hibernate.impl.SessionImpl.flush(SessionImpl.java:1028)
    *    at org.hibernate.impl.SessionImpl.managedFlush(SessionImpl.java:366)
    *    at org.hibernate.transaction.JDBCTransaction.commit(JDBCTransaction.java:137)
    *    at org.hibernate.ejb.TransactionImpl.commit(TransactionImpl.java:54)
    *    at net.tourbook.database.TourDatabase.saveTour(TourDatabase.java:1731)
    * </pre>
    *
    * @param tourData
    */
   private static void saveTransientInstances(final TourData tourData) {

      try {

         saveTransientInstances_Tags(tourData);
         saveTransientInstances_TourLocation(tourData);
         saveTransientInstances_TourType(tourData);
         saveTransientInstances_Sensors(tourData);

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }
   }

   /**
    * Save sensor instances which are not yet saved, they have to be saved before the tour is saved.
    *
    * @param tourData
    */
   private static void saveTransientInstances_Sensors(final TourData tourData) {

      final Set<DeviceSensorValue> allSensorValues = tourData.getDeviceSensorValues();

      if (allSensorValues.isEmpty()) {
         return;
      }

      synchronized (TRANSIENT_LOCK) {

         final List<DeviceSensor> allNotSavedSensors = new ArrayList<>();

         saveTransientInstances_Sensors_1_GetNotSaved(allSensorValues, allNotSavedSensors);

         if (allNotSavedSensors.size() > 0) {

            // there are sensors which are not yet saved -> save new sensors

            saveTransientInstances_Sensors_2_Save(allSensorValues, allNotSavedSensors);
         }
      }
   }

   /**
    * Find sensors which are not yet saved, which are transient
    *
    * @param allTourData_SensorValues
    * @param allNotSavedSensors
    *           Returns all sensors which are not yet saved
    */
   private static void saveTransientInstances_Sensors_1_GetNotSaved(final Set<DeviceSensorValue> allTourData_SensorValues,
                                                                    final List<DeviceSensor> allNotSavedSensors) {

      // loop: all sensor values in the tour -> find sensors which are not yet saved

      for (final DeviceSensorValue tourData_SensorValue : allTourData_SensorValues) {

         final DeviceSensor tourData_Sensor = tourData_SensorValue.getDeviceSensor();

         final long sensorId = tourData_Sensor.getSensorId();

         if (sensorId == ENTITY_IS_NOT_SAVED) {

            // sensor is NOT yet saved

            allNotSavedSensors.add(tourData_Sensor);
         }
      }
   }

   /**
    * <pre>
    *
    * org.hibernate.PersistentObjectException: detached entity passed to persist: net.tourbook.data.DeviceSensor
    *    at org.hibernate.event.def.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:102)
    *    at org.hibernate.event.def.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:61)
    *    at org.hibernate.impl.SessionImpl.firePersist(SessionImpl.java:646)
    *    at org.hibernate.impl.SessionImpl.persist(SessionImpl.java:620)
    *    at org.hibernate.impl.SessionImpl.persist(SessionImpl.java:624)
    *    at org.hibernate.ejb.AbstractEntityManagerImpl.persist(AbstractEntityManagerImpl.java:220)
    *    at net.tourbook.database.TourDatabase.saveEntity(TourDatabase.java:2999)
    *
    *    at net.tourbook.database.TourDatabase.saveTransientInstances_Sensors_2_Save(TourDatabase.java:3588)
    *
    *    at net.tourbook.database.TourDatabase.saveTransientInstances_Sensors(TourDatabase.java:3529)
    *    at net.tourbook.database.TourDatabase.saveTransientInstances(TourDatabase.java:3500)
    *    at net.tourbook.database.TourDatabase.saveTour_PreSaveActions(TourDatabase.java:3459)
    *    at net.tourbook.database.TourDatabase.saveTour_Concurrent(TourDatabase.java:3198)
    *    at net.tourbook.ui.views.rawData.RawDataView.saveImportedTours_20_Concurrent_OneTour(RawDataView.java:6619)
    *    at net.tourbook.ui.views.rawData.RawDataView.lambda$31(RawDataView.java:6579)
    *
    * </pre>
    *
    * @param allTourData_SensorValues
    *           All sensor values in one tour
    * @param allNotSavedSensors
    *           Contains all sensors which are not yet saved
    */
   private static void saveTransientInstances_Sensors_2_Save(final Set<DeviceSensorValue> allTourData_SensorValues,
                                                             final List<DeviceSensor> allNotSavedSensors) {

      HashMap<Long, DeviceSensor> allDbSensors_BySensorID = new HashMap<>(getAllDeviceSensors_BySensorID());
      HashMap<String, DeviceSensor> allDbSensors_BySensorKey_WithDevType = new HashMap<>(getAllDeviceSensors_BySensorKey_WithDevType());

      for (final DeviceSensor notSavedSensor : allNotSavedSensors) {

         final String sensorKey_WithDevType = notSavedSensor.getSensorKey_WithDevType();

         if (StringUtils.hasContent(sensorKey_WithDevType)) {

            // sensor key WITH device type

            final DeviceSensor dbSensor = allDbSensors_BySensorKey_WithDevType.get(sensorKey_WithDevType);

            if (dbSensor == null) {

               // sensor is not yet in db -> create it

               saveEntity(notSavedSensor, ENTITY_IS_NOT_SAVED, DeviceSensor.class);
            }

         } else {

            // sensor WITHOUT device type

            final String tourData_SensorKey_NoDevType = notSavedSensor.getSensorKey_NoDevType();

            boolean isSensorInDb = false;

            for (final DeviceSensor dbSensor : allDbSensors_BySensorID.values()) {

               if (dbSensor.getSensorKey_NoDevType().equals(tourData_SensorKey_NoDevType)) {

                  isSensorInDb = true;

                  break;
               }
            }

            if (isSensorInDb == false) {

               // sensor is not yet in db -> create it

               saveEntity(notSavedSensor, ENTITY_IS_NOT_SAVED, DeviceSensor.class);
            }
         }
      }

      /*
       * Replace sensor in sensor values to ensure that all sensor values contain valid sensors
       */

      // force to reload db sensors
      clearDeviceSensors();
      TourManager.getInstance().clearTourDataCache();

      // RELOAD db sensors
      allDbSensors_BySensorID = new HashMap<>(getAllDeviceSensors_BySensorID());
      allDbSensors_BySensorKey_WithDevType = new HashMap<>(getAllDeviceSensors_BySensorKey_WithDevType());

      // loop: all sensor values in a tour -> set sensor which is saved in the db
      for (final DeviceSensorValue tourData_SensorValue : allTourData_SensorValues) {

         final DeviceSensor tourData_Sensor = tourData_SensorValue.getDeviceSensor();

         final String sensorKey_WithDevType = tourData_Sensor.getSensorKey_WithDevType();

         DeviceSensor dbSensor = null;

         // 1. Check WITH device type
         if (StringUtils.hasContent(sensorKey_WithDevType)) {

            dbSensor = allDbSensors_BySensorKey_WithDevType.get(sensorKey_WithDevType);
         }

         // 2. Check WITHOUT device type
         if (dbSensor == null) {

            final String tourData_SensorKey_NoDevType = tourData_Sensor.getSensorKey_NoDevType();

            for (final DeviceSensor dbSensorByID : allDbSensors_BySensorID.values()) {

               if (dbSensorByID.getSensorKey_NoDevType().equals(tourData_SensorKey_NoDevType)) {

                  dbSensor = dbSensorByID;

                  break;
               }
            }
         }

         tourData_SensorValue.setDeviceSensor(dbSensor);
      }
   }

   /**
    * @param tourData
    */
   private static void saveTransientInstances_Tags(final TourData tourData) {

      final Set<TourTag> allTourDataTags = tourData.getTourTags();

      if (allTourDataTags.isEmpty()) {
         return;
      }

      final ArrayList<TourTag> allAppliedTags = new ArrayList<>();
      final ArrayList<TourTag> allNewTags = new ArrayList<>();

      final HashMap<String, TourTag> allDbTags_ByName = new HashMap<>(getAllTourTags_ByTagName());

      // loop: all tags in the tour -> find tags which are not yet saved
      for (final TourTag tourDataTag : allTourDataTags) {

         final long tagId = tourDataTag.getTagId();

         if (tagId != ENTITY_IS_NOT_SAVED) {

            // tag is saved

            allAppliedTags.add(tourDataTag);

            continue;
         }

         // tag is not yet saved
         // 1. tag can still be new
         // 2. tag is already created but not updated in the not yet saved tour

         final TourTag dbTag = allDbTags_ByName.get(tourDataTag.getTagName().toUpperCase());

         if (dbTag == null) {

            // tag is not available -> create a new tag

            allNewTags.add(tourDataTag);

         } else {

            // use found tag

            allAppliedTags.add(dbTag);
         }
      }

      boolean isNewTagSaved = false;

      if (allNewTags.size() > 0) {

         // create new tags

         synchronized (TRANSIENT_LOCK) {

            final HashMap<String, TourTag> allDbTags_ByName_InLock = new HashMap<>(getAllTourTags_ByTagName());

            for (final TourTag newTag : allNewTags) {

               // check again, tour tag list could be updated in another thread
               final TourTag dbTag = allDbTags_ByName_InLock.get(newTag.getTagName().toUpperCase());

               if (dbTag == null) {

                  // tag is not yet in db -> create it

                  final TourTag savedTag = saveEntity(
                        newTag,
                        ENTITY_IS_NOT_SAVED,
                        TourTag.class);

                  isNewTagSaved = true;

                  allAppliedTags.add(savedTag);

               } else {

                  allAppliedTags.add(dbTag);
               }
            }

            if (isNewTagSaved) {

               // force to reload db tags

               clearTourTags();
               TourManager.getInstance().clearTourDataCache();
            }
         }
      }

      // replace tags in the tour, either with the old tags and/or with newly created tags
      allTourDataTags.clear();
      allTourDataTags.addAll(allAppliedTags);
   }

   /**
    * Save {@link TourLocation} instances which are not yet saved
    *
    * @param tourData
    */
   private static void saveTransientInstances_TourLocation(final TourData tourData) {

      final TourLocation tourLocationStart = tourData.getTourLocationStart();
      final TourLocation checkedLocationStart = saveTransientInstances_TourLocation_OneLocation(tourLocationStart);
      if (checkedLocationStart != null) {

         // replace tour location in the tour
         tourData.setTourLocationStart(checkedLocationStart);
      }

      final TourLocation tourLocationEnd = tourData.getTourLocationEnd();
      final TourLocation checkedLocationEnd = saveTransientInstances_TourLocation_OneLocation(tourLocationEnd);
      if (checkedLocationEnd != null) {

         // replace tour location in the tour
         tourData.setTourLocationEnd(checkedLocationEnd);
      }
   }

   /**
    * @param requestedTourLocation
    *           {@link TourLocation} which needs to be checked
    *
    * @return Returns <code>null</code> when the provided {@link TourLocation} is OK -> needs no
    *         action
    */
   private static TourLocation saveTransientInstances_TourLocation_OneLocation(final TourLocation requestedTourLocation) {

      if (requestedTourLocation == null) {

         // a tour location is not set -> nothing to do

         return null;
      }

      if (requestedTourLocation.getLocationId() != ENTITY_IS_NOT_SAVED) {

         // tour location is saved

         return null;
      }

      TourLocation appliedLocation = null;

      synchronized (TRANSIENT_LOCK) {

         // location is not yet saved
         // 1. location can still be new
         // 2. location is already created but not updated in the not yet saved tour

         final TourLocation loadedTourLocation = getTourLocation(
               requestedTourLocation.latitudeE6,
               requestedTourLocation.longitudeE6,
               requestedTourLocation.zoomlevel);

         if (loadedTourLocation != null) {

            // use found tour location

            appliedLocation = loadedTourLocation;

         } else {

            // save requested tour location

            final TourLocation savedLocation = saveEntity(

                  requestedTourLocation,
                  ENTITY_IS_NOT_SAVED,
                  TourLocation.class);

            if (savedLocation != null) {

               appliedLocation = savedLocation;

               // force reload of cached tour locations
//               clearTourTypes();
//               TourManager.getInstance().clearTourDataCache();
            }
         }
      }

      return appliedLocation;
   }

   /**
    * @param tourData
    */
   private static void saveTransientInstances_TourType(final TourData tourData) {

      final TourType tourType = tourData.getTourType();

      if (tourType == null) {

         // a tour type is not set -> nothing to do

         return;
      }

      if (tourType.getTypeId() != ENTITY_IS_NOT_SAVED) {

         // tour type is saved

         return;
      }

      TourType appliedType = null;

      synchronized (TRANSIENT_LOCK) {

         // type is not yet saved
         // 1. type can still be new
         // 2. type is already created but not updated in the not yet saved tour

         final String tourTypeNameKEY = tourType.getName().toUpperCase();
         final TourType dbType = getAllTourTypes_ByName().get(tourTypeNameKEY);

         if (dbType != null) {

            // use found tag

            appliedType = dbType;

         } else {

            // create new tag

            final TourType savedType = saveEntity(
                  tourType,
                  ENTITY_IS_NOT_SAVED,
                  TourType.class);

            if (savedType != null) {

               appliedType = savedType;

               // force reload of the db tour types
               clearTourTypes();
               TourManager.getInstance().clearTourDataCache();
            }
         }
      }

      // replace tour type in the tour
      tourData.setTourType(appliedType);
   }

   public static void showTourSaveError(final TourData tourData) {

      MessageDialog.openError(
            Display.getDefault().getActiveShell(),
            "Error", //$NON-NLS-1$
            String.format("Error occurred when saving tour '%s'", TourManager.getTourTitleDetailed(tourData))); //$NON-NLS-1$
   }

   public static void updateActiveTourTypeList(final TourTypeFilter tourTypeFilter) {

      switch (tourTypeFilter.getFilterType()) {
      case TourTypeFilter.FILTER_TYPE_SYSTEM:

         if (tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {

            // all tour types are selected

            _activeTourTypes = _allDbTourTypes;
            return;

         } else {

            // tour type is not defined

         }

         break;

      case TourTypeFilter.FILTER_TYPE_DB:

         _activeTourTypes = new ArrayList<>();
         _activeTourTypes.add(tourTypeFilter.getTourType());

         return;

      case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

         final Object[] tourTypes = tourTypeFilter.getTourTypeSet().getTourTypes();

         if (tourTypes.length != 0) {

            // create a list with all tour types from the set

            _activeTourTypes = new ArrayList<>();

            for (final Object item : tourTypes) {
               _activeTourTypes.add((TourType) item);
            }
            return;
         }

         break;

      default:
         break;
      }

      // set default empty list
      _activeTourTypes = new ArrayList<>();
   }

   private static void updateCachedFields(final TourData tourData) {

      final ConcurrentSkipListSet<String> allTitles = getCachedFields_AllTourTitles();
      final ConcurrentSkipListSet<String> allPlaceStarts = getCachedFields_AllTourPlaceStarts();
      final ConcurrentSkipListSet<String> allPlaceEnds = getCachedFields_AllTourPlaceEnds();
      final ConcurrentSkipListSet<String> allMarkerNames = getCachedFields_AllTourMarkerNames();

      // cache tour title
      final String tourTitle = tourData.getTourTitle();
      if (tourTitle.length() > 0) {
         allTitles.add(tourTitle);
      }

      // cache tour start place
      final String tourStartPlace = tourData.getTourStartPlace();
      if (tourStartPlace.length() > 0) {
         allPlaceStarts.add(tourStartPlace);
      }

      // cache tour end place
      final String tourEndPlace = tourData.getTourEndPlace();
      if (tourEndPlace.length() > 0) {
         allPlaceEnds.add(tourEndPlace);
      }

      // cache tour marker names
      final Set<TourMarker> allTourMarker = tourData.getTourMarkers();
      for (final TourMarker tourMarker : allTourMarker) {
         final String label = tourMarker.getLabel();
         if (label.length() > 0) {
            allMarkerNames.add(label);
         }
      }
   }

   /**
    * Update calendar week for all tours with the app week settings from
    * {@link TimeTools#calendarWeek}
    *
    * @param conn
    * @param splashManager
    *
    * @return Returns <code>true</code> when the week is computed
    *
    * @throws SQLException
    */
   public static boolean updateTourWeek(final Connection conn,
                                        final Object progress) throws SQLException {

      final ArrayList<Long> tourList = getAllTourIds();

      boolean isUpdated = false;

      final PreparedStatement stmtSelect = conn.prepareStatement(UI.EMPTY_STRING

            + "SELECT" //                       //$NON-NLS-1$

            + " StartYear," //               1  //$NON-NLS-1$
            + " StartMonth," //              2  //$NON-NLS-1$
            + " StartDay" //                 3  //$NON-NLS-1$

            + " FROM " + TABLE_TOUR_DATA //     //$NON-NLS-1$
            + " WHERE TourId=?" //              //$NON-NLS-1$
      );

      final PreparedStatement stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

            + "UPDATE " + TABLE_TOUR_DATA //    //$NON-NLS-1$
            + " SET" //                         //$NON-NLS-1$
            + " startWeek=?," //                //$NON-NLS-1$
            + " startWeekYear=? " //            //$NON-NLS-1$
            + " WHERE tourId=?" //              //$NON-NLS-1$
      );

      int tourIdx = 1;

      // use week settings from the app
      final WeekFields cw = TimeTools.calendarWeek;
      final TemporalField year_WeekBasedYear_Field = cw.weekBasedYear();
      final TemporalField week_OfWeekBasedYear_Field = cw.weekOfWeekBasedYear();

      // loop over all tours and calculate and set new columns
      for (final Long tourId : tourList) {

         final String msg = NLS.bind(
               Messages.Tour_Database_Update_TourWeek,
               new Object[] { tourIdx++, tourList.size() });

         if (progress instanceof final IProgressMonitor monitor) {

            monitor.subTask(msg);

         } else if (progress instanceof final SplashManager splashManager) {

            splashManager.setMessage(msg);
         }

         // get tour date
         stmtSelect.setLong(1, tourId);

         final ResultSet result = stmtSelect.executeQuery();
         while (result.next()) {

            // get date from database
            final short dbYear = result.getShort(1);
            final short dbMonth = result.getShort(2);
            final short dbDay = result.getShort(3);

//            if (dbYear == 2012 && dbMonth == 12 && dbDay == 31) {
//
//               int a = 0;
//               a++;
//            }

            // get week no/year
            final LocalDate tourDate = LocalDate.of(dbYear, dbMonth, dbDay);
            final short weekNo = (short) tourDate.get(week_OfWeekBasedYear_Field);
            final short weekYear = (short) tourDate.get(year_WeekBasedYear_Field);

            // update week number/week year in the database
            stmtUpdate.setShort(1, weekNo);
            stmtUpdate.setShort(2, weekYear);
            stmtUpdate.setLong(3, tourId);

            stmtUpdate.executeUpdate();

            isUpdated = true;
         }
      }

      return isUpdated;
   }

   public void addPropertyListener(final IPropertyListener listener) {
      _propertyListeners.add(listener);
   }

   /**
    * Create index for {@link TourData} will dramatically improve performance *
    * <p>
    * since db version 5
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createIndex_TourData_005(final Statement stmt) throws SQLException {

      String sql;

      /*
       * CREATE INDEX YearMonth
       */
      sql = "CREATE INDEX YearMonth ON " + TABLE_TOUR_DATA + " (startYear, startMonth)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);

      /*
       * CREATE INDEX TourType
       */
      sql = "CREATE INDEX TourType ON " + TABLE_TOUR_DATA + " (" + KEY_TOUR_TYPE + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      exec(stmt, sql);

      /*
       * CREATE INDEX TourPerson
       */
      sql = "CREATE INDEX TourPerson ON " + TABLE_TOUR_DATA + " (" + KEY_PERSON + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      exec(stmt, sql);
   }

   /**
    * Create index for {@link TourData} will dramatically improve performance *
    * <p>
    *
    * @param stmt
    *
    * @throws SQLException
    *
    * @since db version 22
    */
   private void createIndex_TourData_022(final Statement stmt) throws SQLException {

      String sql;

      /*
       * CREATE INDEX TourStartTime
       */
      sql = "CREATE INDEX TourStartTime ON " + TABLE_TOUR_DATA + " (TourStartTime)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);

      /*
       * CREATE INDEX TourEndTime
       */
      sql = "CREATE INDEX TourEndTime ON " + TABLE_TOUR_DATA + " (TourEndTime)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);
   }

   /**
    * Create index for {@link TourData}. *
    * <p>
    *
    * @param stmt
    *
    * @throws SQLException
    *
    * @since Db version 29
    */
   private void createIndex_TourData_029(final Statement stmt) throws SQLException {

      String sql;

      /*
       * CREATE INDEX TourImportFileName
       */
      sql = "CREATE INDEX TourImportFileName ON " + TABLE_TOUR_DATA + " (TourImportFileName)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);
   }

   /**
    * Create index for {@link TourData}. *
    * <p>
    *
    * @param stmt
    *
    * @throws SQLException
    *
    * @since Db version 33
    */
   private void createIndex_TourData_033(final Statement stmt) throws SQLException {

      String sql;

      /*
       * CREATE INDEX StartWeek
       */
      sql = "CREATE INDEX StartWeek ON " + TABLE_TOUR_DATA + " (StartWeek)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);

      /*
       * CREATE INDEX StartWeekYear
       */
      sql = "CREATE INDEX StartWeekYear ON " + TABLE_TOUR_DATA + " (StartWeekYear)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);
   }

   /**
    * Create index for {@link TourData}. *
    * <p>
    *
    * @param stmt
    *
    * @throws SQLException
    *
    * @since Db version 37
    */
   private void createIndex_TourData_037(final Statement stmt) throws SQLException {

      /*
       * CREATE INDEX HasGeoData
       */
      final String sql = "CREATE INDEX HasGeoData ON " + TABLE_TOUR_DATA + " (HasGeoData)"; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);
   }

   private String createLog_DataUpdate(final int toVersion, final long startTime) {

      final long timeDiff = System.currentTimeMillis() - startTime;

      return String.format(
            "Database data update %d -> %d in %s mm:ss", //$NON-NLS-1$
            toVersion - 1,
            toVersion,
            net.tourbook.common.UI.format_mm_ss(timeDiff / 1000));
   }

   /**
    * Create table {@link #TABLE_DB_VERSION_DATA}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_DbVersion_Data(final Statement stmt, final int initialVersionNumber) throws SQLException {

      // ensure table do not exist
      if (isTableAvailable(stmt.getConnection(), TABLE_DB_VERSION_DATA)) {
         return;
      }

      String sql;

      /*
       * Create table
       */
      sql = UI.EMPTY_STRING

            + "CREATE TABLE " + TABLE_DB_VERSION_DATA //       //$NON-NLS-1$
            + " (" + NL //                                     //$NON-NLS-1$
            + "   version INTEGER NOT NULL " + NL //           //$NON-NLS-1$
            + " )"; //                                         //$NON-NLS-1$

      exec(stmt, sql);

      /*
       * Create 1 record which contains the db data version
       */

      sql = "INSERT INTO " + TABLE_DB_VERSION_DATA + " VALUES (" + Integer.toString(initialVersionNumber) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      execUpdate(stmt, sql);
   }

   /**
    * Create table {@link #TABLE_DB_VERSION_DESIGN}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_DbVersion_Design(final Statement stmt) throws SQLException {

      /*
       * Create table
       */
      String sql = UI.EMPTY_STRING

            + "CREATE TABLE " + TABLE_DB_VERSION_DESIGN //     //$NON-NLS-1$
            + " (" + NL //                                     //$NON-NLS-1$
            + "   version INTEGER NOT NULL " + NL //           //$NON-NLS-1$
            + " )"; //                                         //$NON-NLS-1$

      exec(stmt, sql);

      /*
       * Create 1 record which contains the db design version number
       */

      sql = "INSERT INTO " + TABLE_DB_VERSION_DESIGN + " VALUES (" + Integer.toString(TOURBOOK_DB_VERSION) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      execUpdate(stmt, sql);
   }

   /**
    * Create table {@link #TABLE_DEVICE_SENSOR}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_DeviceSensor(final Statement stmt) throws SQLException {

      exec(stmt, "CREATE TABLE " + TABLE_DEVICE_SENSOR + "   (                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_DEVICE_SENSOR, true)

            // version 46 start

            + "   SensorType           VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   SensorName           VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   Description          VARCHAR(" + DeviceSensor.DB_LENGTH_DESCRIPTION + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   ManufacturerNumber   INTEGER,                                                 " + NL //$NON-NLS-1$
            + "   ManufacturerName     VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   ProductNumber        INTEGER,                                                 " + NL //$NON-NLS-1$
            + "   ProductName          VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   SerialNumber         VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 46 end

            // version 59 start

            + "   DeviceType           SMALLINT DEFAULT -1,                                     " + NL //$NON-NLS-1$
            + "   DeviceName           VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + ")             " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 59 end

            + ")" //                                                                                  //$NON-NLS-1$
      );

      SQL.createIndex_Simple(stmt, TABLE_DEVICE_SENSOR, "SerialNumber"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_DEVICE_SENSOR_VALUE}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_DeviceSensorValues(final Statement stmt) throws SQLException {

      exec(stmt, "CREATE TABLE " + TABLE_DEVICE_SENSOR_VALUE + "   (                   " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_DEVICE_SENSOR_VALUE, true)

            + "   " + KEY_TOUR + "           BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_DEVICE_SENSOR + "  BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 46 start

            + "   TourStartTime              BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
            + "   TourEndTime                BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$

            + "   BatteryLevel_Start         SMALLINT DEFAULT -1,                      " + NL //$NON-NLS-1$
            + "   BatteryLevel_End           SMALLINT DEFAULT -1,                      " + NL //$NON-NLS-1$

            + "   BatteryStatus_Start        SMALLINT DEFAULT -1,                      " + NL //$NON-NLS-1$
            + "   BatteryStatus_End          SMALLINT DEFAULT -1,                      " + NL //$NON-NLS-1$

            + "   BatteryVoltage_Start       FLOAT DEFAULT -1,                         " + NL //$NON-NLS-1$
            + "   BatteryVoltage_End         FLOAT DEFAULT -1                          " + NL //$NON-NLS-1$

            // version 46 end

            + ")" //                                                                          //$NON-NLS-1$
      );

      SQL.createIndex_Table__Column(stmt, TABLE_DEVICE_SENSOR_VALUE, KEY_TOUR);
      SQL.createIndex_Table__Column(stmt, TABLE_DEVICE_SENSOR_VALUE, "TourStartTime"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_EQUIPMENT}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_Equipment(final Statement stmt) throws SQLException {

      exec(stmt,

            "CREATE TABLE " + TABLE_EQUIPMENT + "   (                                     " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + SQL.createField_EntityId(ENTITY_ID_EQUIPMENT, true)

                  + "   Brand                   VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Model                   VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Type                    VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   Description             VARCHAR(" + DB_LENGTH_DESCRIPTION + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ImageFilePath           VARCHAR(" + DB_LENGTH_FILE_PATH + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   UrlAddress              VARCHAR(" + DB_LENGTH_URL_ADDRESS + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   DistanceFirstUse        FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
                  + "   Price                   FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
                  + "   PriceUnit               VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Size                    VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Weight                  FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$

                  + "   IsCollate               BOOLEAN DEFAULT FALSE,                    " + NL //$NON-NLS-1$
                  + "   ExpandType              INTEGER,                                  " + NL //$NON-NLS-1$

                  + "   DateUsed                BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateBuilt               BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateRetired             BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateCollateFrom         BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateCollateUntil        BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$

                  // version 61 start

                  + "   IsRetired               BOOLEAN DEFAULT FALSE,                    " + NL //$NON-NLS-1$
                  + "   PurchaseLocation        VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   WeightUnit              SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$

                  // version 61 end

                  // version 62 start

                  + "   DatePurchased           BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   InitialValue            FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
                  + "   InitialValueUnit        VARCHAR(" + DB_LENGTH_NAME + ")          	" + NL //$NON-NLS-1$ //$NON-NLS-2$

                  // version 62 end

                  + ")" //                                                                       //$NON-NLS-1$
      );

      /**
       * Create table: TOURDATA_Equipment
       */
      exec(stmt,

            "CREATE TABLE " + JOINTABLE__TOURDATA__EQUIPMENT + "   (                      " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   " + KEY_EQUIPMENT + "   BIGINT NOT NULL,                          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + KEY_TOUR + "        BIGINT NOT NULL                           " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + ")"); //$NON-NLS-1$

      // Add Constraint
      final String fkName = "fk_" + JOINTABLE__TOURDATA__EQUIPMENT + "_" + KEY_TOUR; //          //$NON-NLS-1$ //$NON-NLS-2$

      exec(stmt,

            "ALTER TABLE " + JOINTABLE__TOURDATA__EQUIPMENT + "                           " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkName + "                                     " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TOUR + ")                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_DATA + " (" + ENTITY_ID_TOUR + ")       " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      // Create index "TOURDATA_Equipment"
      SQL.createIndex_Composite(stmt,

            JOINTABLE__TOURDATA__EQUIPMENT,

            KEY_EQUIPMENT,
            KEY_TOUR);

      // Create indices "Equipment__..."
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT, "IsCollate"); //            //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT, "DateUsed"); //             //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT, "DateCollateFrom"); //      //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT, "DateCollateUntil"); //     //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT, "Type"); //                 //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_EQUIPMENT_PART}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_EquipmentPart(final Statement stmt) throws SQLException {

      exec(stmt,

            "CREATE TABLE " + TABLE_EQUIPMENT_PART + "   (                                " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + SQL.createField_EntityId(ENTITY_ID_EQUIPMENT_PART, true)

                  + "   " + KEY_EQUIPMENT + "   BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   ItemType                SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$
                  + "   ExpandType              SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$

                  + "   Brand                   VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Model                   VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Type                    VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   Name                    VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Company                 VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   Description             VARCHAR(" + DB_LENGTH_DESCRIPTION + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ImageFilePath           VARCHAR(" + DB_LENGTH_FILE_PATH + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   UrlAddress              VARCHAR(" + DB_LENGTH_URL_ADDRESS + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   DistanceFirstUse        FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
                  + "   Price                   FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
                  + "   PriceUnit               VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Size                    VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   Weight                  FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$

                  + "   IsCollate               BOOLEAN DEFAULT TRUE,                     " + NL //$NON-NLS-1$
                  + "   CollateBetween          SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$

                  + "   DateUsed                BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateBuilt               BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateRetired             BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateCollateFrom         BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   DateCollateUntil        BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$

                  // version 61 start

                  + "   IsRetired               BOOLEAN DEFAULT FALSE,                    " + NL //$NON-NLS-1$
                  + "   PurchaseLocation        VARCHAR(" + DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   WeightUnit              SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$

                  // version 61 end

                  // version 62 start

                  + "   DatePurchased           BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
                  + "   InitialValue            FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
                  + "   InitialValueUnit        VARCHAR(" + DB_LENGTH_NAME + ")           " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  // version 62 end

                  + ")" //                                                                       //$NON-NLS-1$
      );

      // Create indices "EquipmentPart__..."
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, KEY_EQUIPMENT);
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "PartID"); //             //$NON-NLS-1$

      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "IsCollate"); //          //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "ItemType"); //           //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "DateUsed"); //           //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "DateCollateFrom"); //    //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "DateCollateUntil"); //   //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_EQUIPMENT_PART, "Type"); //               //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_BEVERAGE_CONTAINER} for {@link TourBeverageContainer}.
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourBeverageContainer(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourBeverageContainer
       */

      exec(stmt,
            "CREATE TABLE " + TABLE_TOUR_BEVERAGE_CONTAINER + "   (                             " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + SQL.createField_EntityId(ENTITY_ID_BEVERAGECONTAINER, true)

                  // Version 55 - begin

                  + "   name                VARCHAR(" + TourBeverageContainer.DB_LENGTH_NAME + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   capacity            FLOAT                                                       " + NL //$NON-NLS-1$

                  // Version 55 - end

                  + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_BIKE}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourBike(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourBike
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_BIKE + "   (                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_BIKE, true)

            + "   Name                       VARCHAR(" + TourBike.DB_LENGTH_NAME + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   Weight                     FLOAT,                                    " + NL //$NON-NLS-1$ // kg
            + "   TypeId                     INTEGER,                                  " + NL //$NON-NLS-1$
            + "   FrontTyreId                INTEGER,                                  " + NL //$NON-NLS-1$
            + "   RearTyreId                 INTEGER                                   " + NL //$NON-NLS-1$

            + ")");//$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_COMPARED}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourCompared(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourCompared
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_COMPARED + "   (                         " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_COMPARED, true)

            + "   RefTourId                  BIGINT,                                   " + NL //$NON-NLS-1$
            + "   TourId                     BIGINT,                                   " + NL //$NON-NLS-1$

            + "   StartIndex                 INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   EndIndex                   INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   TourDate                   DATE NOT NULL,                            " + NL //$NON-NLS-1$
            + "   StartYear                  INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   TourSpeed                  FLOAT,                                    " + NL //$NON-NLS-1$

            // version 28 start

            + "   AvgPulse                   FLOAT,                                    " + NL //$NON-NLS-1$

            // version 28 end ---------

            // version 40 start
//
// RENAMED FIELD - from "tourRecordingTime"
//
            + "   tourDeviceTime_Elapsed     INTEGER     DEFAULT 0,                    " + NL //$NON-NLS-1$

            // version 40 end ---------

            // version 50 start

            + "   AvgAltimeter               FLOAT       DEFAULT 0,                    " + NL //$NON-NLS-1$
            + "   MaxPulse                   FLOAT       DEFAULT 0,                    " + NL //$NON-NLS-1$

            // version 50 end ---------

            // version 51 start

            + "   TourPace                   FLOAT       DEFAULT 0                     " + NL //$NON-NLS-1$

            // version 51 end ---------

            + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_DATA}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourData(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourData
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_DATA + "   (                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_TOUR, false)

            + "   StartYear                              SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartMonth                             SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartDay                               SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartHour                              SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartMinute                            SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartWeek                              SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartDistance                          INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   Distance                               INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   StartAltitude                          SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   StartPulse                             SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   DpTolerance                            SMALLINT NOT NULL,            " + NL //$NON-NLS-1$
            + "   TourDistance                           INTEGER NOT NULL,             " + NL //$NON-NLS-1$

            // replaced with BIGINT values in version 22
            // + "   tourRecordingTime                   INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            // + "   tourDrivingTime                     INTEGER NOT NULL,             " + NL //$NON-NLS-1$

            + "   tourAltUp                              INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   tourAltDown                            INTEGER NOT NULL,             " + NL //$NON-NLS-1$

            + "   deviceTourType                         VARCHAR(" + TourData.DB_LENGTH_DEVICE_TOUR_TYPE + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   devicePluginId                         VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_ID + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   deviceTravelTime                       BIGINT NOT NULL,              " + NL //$NON-NLS-1$
            + "   deviceDistance                         INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   deviceWheel                            INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   deviceWeight                           INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   deviceTotalUp                          INTEGER NOT NULL,             " + NL //$NON-NLS-1$
            + "   deviceTotalDown                        INTEGER NOT NULL,             " + NL //$NON-NLS-1$

            // version 3 start
            + "   deviceMode                             SMALLINT,                     " + NL //$NON-NLS-1$
            + "   deviceTimeInterval                     SMALLINT,                     " + NL //$NON-NLS-1$
            // version 3 end

            // version 4 start

            // from markus
            // replaced with FLOAT values in version 21
            // + "   maxAltitude            INTEGER,                          " + NL //$NON-NLS-1$
            // + "   maxPulse               INTEGER,                          " + NL //$NON-NLS-1$
            // + "   avgPulse               INTEGER,                          " + NL //$NON-NLS-1$
            // + "   avgCadence             INTEGER,                          " + NL //$NON-NLS-1$
            // + "   avgTemperature         INTEGER,                          " + NL //$NON-NLS-1$

            + "   maxSpeed                               FLOAT,                                                   " + NL //$NON-NLS-1$
            + "   tourTitle                              VARCHAR(" + TourData.DB_LENGTH_TOUR_TITLE + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // OLD + "tourDescription                    VARCHAR(4096),                                           " + NL // version <= 9
            + "   tourDescription                        VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + ")," + NL // modified in version 10 //$NON-NLS-1$ //$NON-NLS-2$

            + "   tourStartPlace                         VARCHAR(" + TourData.DB_LENGTH_TOUR_START_PLACE_V52 + ")," + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   tourEndPlace                           VARCHAR(" + TourData.DB_LENGTH_TOUR_END_PLACE_V52 + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   calories                               INTEGER,                                                 " + NL //$NON-NLS-1$
//
// RENAMED FIELD - bikerWeight
//
            + "   bodyWeight                             FLOAT,                                                   " + NL //$NON-NLS-1$
            + "   " + KEY_BIKE + "                       BIGINT,                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // from wolfgang
            + "   devicePluginName                       VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   deviceModeName                         VARCHAR(" + TourData.DB_LENGTH_DEVICE_MODE_NAME + "),    " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 4 end

            + "   " + KEY_TOUR_TYPE + "                  BIGINT,                       " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_PERSON + "                     BIGINT,                       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 6 start

            + "   tourImportFilePath                     VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 6 end

            // version 7 start

            + "   mergeSourceTourId                      BIGINT,                       " + NL //$NON-NLS-1$
            + "   mergeTargetTourId                      BIGINT,                       " + NL //$NON-NLS-1$
            + "   mergedTourTimeOffset                   INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   mergedAltitudeOffset                   INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   startSecond                            SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$

            // version 7 end

            // version 8 start

            + "   weather_Wind_Direction                 INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   weather_Wind_Speed                     INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   weather_Clouds                         VARCHAR(" + TourData.DB_LENGTH_WEATHER_CLOUDS + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   restPulse                              INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   isDistanceFromSensor                   SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$

            // version 8 end

            // version 9 start

            + "   startWeekYear                          SMALLINT DEFAULT 1977,        " + NL //$NON-NLS-1$

            // version 9 end

            // version 10 start

            // tourWayPoints is mapped in TourData

            // version 10 end----------

            // version 11 start
            //
            + "   DateTimeCreated                        BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$
            + "   DateTimeModified                       BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$

            // version 11 end ---------

            // version 12 start
            //
            + "   IsPulseSensorPresent                   INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   IsPowerSensorPresent                   INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   DeviceAvgSpeed                         FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   DeviceFirmwareVersion                  VARCHAR(" + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 12 end ---------

            // version 13 start

            + "   TemperatureScale                       INTEGER DEFAULT 1,            " + NL //$NON-NLS-1$
            + "   Weather                                VARCHAR(" + TourData.DB_LENGTH_WEATHER_V48 + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 13 end ---------

            // version 14 start

            + "   ConconiDeflection                      INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 14 end ---------

            // version 17 start

            + "   hrZone0                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone1                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone2                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone3                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone4                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone5                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone6                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone7                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone8                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$
            + "   hrZone9                                INTEGER DEFAULT -1,           " + NL //$NON-NLS-1$

            // version 17 end ---------

            // version 18 start

            + "   NumberOfHrZones                        INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 18 end ---------

            // version 21 start

            + "   maxAltitude                            FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   maxPulse                               FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   avgPulse                               FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   avgCadence                             FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   weather_Temperature_Average_Device     FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$

            // version 21 end ---------

            // version 22 start  -  12.12

            + "   TourStartTime                          BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$
            + "   TourEndTime                            BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$

//
// RENAMED FIELDs - from TourRecordingTime, TourDrivingTime
//
            + "   TourDeviceTime_Elapsed                 BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$
            + "   TourComputedTime_Moving                BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$

            // version 22 end ---------

            // version 23 start  -  13.2

            + "   numberOfTimeSlices                     INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   numberOfPhotos                         INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   photoTimeAdjustment                    INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 23 end ---------

            // version 27 start  -  15.5

            + "   frontShiftCount                        INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   rearShiftCount                         INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 27 end ---------

            // version 29 start  -  15.12

            + "   TourImportFileName                     VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_NAME + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 29 end ---------

            // version 30 start  -  16.1

            + "   power_Avg                              FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   power_Max                              INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   power_Normalized                       INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   power_FTP                              INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            + "   power_TotalWork                        BIGINT DEFAULT 0,             " + NL //$NON-NLS-1$
            + "   power_TrainingStressScore              FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   power_IntensityFactor                  FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$

            + "   power_PedalLeftRightBalance            INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   power_AvgLeftTorqueEffectiveness       FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   power_AvgRightTorqueEffectiveness      FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   power_AvgLeftPedalSmoothness           FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   power_AvgRightPedalSmoothness          FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$

            // version 30 end ---------

            // version 31 start  -  16.5

            + "   CadenceMultiplier                      FLOAT DEFAULT 1.0,            " + NL //$NON-NLS-1$
            + "   IsStrideSensorPresent                  INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 31 end ---------

            // version 32 start  -  >16.8 ???

            + "   TimeZoneId                             VARCHAR(" + TourData.DB_LENGTH_TIME_ZONE_ID + "),    " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 32 end ---------

            // version 35 start  -  18.7

            + "   runDyn_StanceTime_Min                  SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_StanceTime_Max                  SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_StanceTime_Avg                  FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            + "   runDyn_StanceTimeBalance_Min           SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_StanceTimeBalance_Max           SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_StanceTimeBalance_Avg           FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            + "   runDyn_StepLength_Min                  SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_StepLength_Max                  SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_StepLength_Avg                  FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            + "   runDyn_VerticalOscillation_Min         SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_VerticalOscillation_Max         SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_VerticalOscillation_Avg         FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            + "   runDyn_VerticalRatio_Min               SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_VerticalRatio_Max               SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   runDyn_VerticalRatio_Avg               FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            // version 35 end ---------

            // version 36 start  -  18.12

            + "   surfing_NumberOfEvents                 SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   surfing_MinSpeed_StartStop             SMALLINT DEFAULT -1,          " + NL //$NON-NLS-1$
            + "   surfing_MinSpeed_Surfing               SMALLINT DEFAULT -1,          " + NL //$NON-NLS-1$
            + "   surfing_MinTimeDuration                SMALLINT DEFAULT -1,          " + NL //$NON-NLS-1$

            + "   surfing_IsMinDistance                  BOOLEAN  DEFAULT FALSE,       " + NL //$NON-NLS-1$
            + "   surfing_MinDistance                    SMALLINT DEFAULT -1,          " + NL //$NON-NLS-1$

            // version 36 end ---------

            // version 37 start  -  19.3

            + "   hasGeoData                             BOOLEAN  DEFAULT FALSE,       " + NL //$NON-NLS-1$

            // version 37 end ---------

            // version 38 start  -  19.5

            + "   training_TrainingEffect_Aerob          FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   training_TrainingEffect_Anaerob        FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   training_TrainingPerformance           FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            // version 38 end

            // version 39 start  -  19.7

            + "   isWeatherDataFromProvider              BOOLEAN  DEFAULT FALSE,       " + NL //$NON-NLS-1$
            + "   weather_Humidity                       SMALLINT DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   weather_Precipitation                  FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   weather_Pressure                       FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   weather_Temperature_Min_Device         FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   weather_Temperature_Max_Device         FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$
            + "   weather_Temperature_WindChill          FLOAT    DEFAULT 0,           " + NL //$NON-NLS-1$

            // version 39 end

            // version 40 start  -  19.10

            + "   power_DataSource                       VARCHAR(" + TourData.DB_LENGTH_POWER_DATA_SOURCE + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   cadenceZone_SlowTime                   INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   cadenceZone_FastTime                   INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   cadenceZones_DelimiterValue            INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
            + "   avgAltitudeChange                      INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 40 end

            // version 41 start  -  20.8

            + "   maxPace                                FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$

            // version 41 end

            // version 42 start  -  20.11.1

            + "   tourDeviceTime_Recorded                BIGINT,                       " + NL //$NON-NLS-1$
            + "   tourDeviceTime_Paused                  BIGINT,                       " + NL //$NON-NLS-1$
            + "   bodyFat                                FLOAT,                        " + NL //$NON-NLS-1$

            // version 42 end

            // version 45 start  -  21.9

            /*
             * The first default values were 0 but only when a new tour db was created
             * and not when the tour db was updated !!!
             */
            + "   Battery_Percentage_Start               SMALLINT DEFAULT -1,          " + NL //$NON-NLS-1$
            + "   Battery_Percentage_End                 SMALLINT DEFAULT -1,          " + NL //$NON-NLS-1$

            // version 45 end

            // version 46 start  -  after 21.9
            // version 46 end

            // version 47 start  -  after 21.12

            + "   weather_Temperature_Average            FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   weather_Temperature_Max                FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   weather_Temperature_Min                FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$
            + "   weather_Snowfall                       FLOAT DEFAULT 0,              " + NL //$NON-NLS-1$

            // version 47 end

            // version 50 start  -  23.5

            + "   weather_AirQuality                     VARCHAR(" + TourData.DB_LENGTH_WEATHER_AIRQUALITY + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 50 end

            // version 52 start  -  23.12

            + "   tourLocationStart_LocationID           BIGINT,                       " + NL //$NON-NLS-1$
            + "   tourLocationEnd_LocationID             BIGINT,                       " + NL //$NON-NLS-1$

            // version 52 end

            // version 58 start  -  25.6

            + "   poolLength                             INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 58 end

            // version 59 start  -  25.11

            + "   numberOfPassedVehicles                 INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 59 end

            // version 63 start  -  26.9

            + "   nutrition_TotalCarbohydrates           FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$

            // version 63 end

            // version 5 start
            /**
             * Disabled because when two blob object's are deserialized then the error occurs:
             * <p>
             * java.io.StreamCorruptedException: invalid stream header: 00ACED00
             * <p>
             * -> the gpsData are put into the serieData object
             */
            //   + "gpsData                              BLOB,                         " + NL //$NON-NLS-1$

            // version 5 end

            + "   serieData                              BLOB                          " + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$

      createIndex_TourData_005(stmt);
      createIndex_TourData_022(stmt);
      createIndex_TourData_029(stmt);
      createIndex_TourData_033(stmt);
      createIndex_TourData_037(stmt);

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_DATA, "Battery_Percentage_Start"); //$NON-NLS-1$

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_DATA, "TourLocationStart_LocationID"); //$NON-NLS-1$
      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_DATA, "TourLocationEnd_LocationID"); //$NON-NLS-1$
   }

   /**
    * create table {@link #}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourGeoParts(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourGeoParts
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_GEO_PARTS + "   (                        " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   TourId                     BIGINT   NOT NULL,                        " + NL //$NON-NLS-1$
            + "   GeoPart                    INTEGER  NOT NULL,                        " + NL //$NON-NLS-1$

            + "   CONSTRAINT                 PK_TourId_GeoPart PRIMARY KEY (TourId, GeoPart)" + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$

      SQL.createIndex_Simple(stmt, TABLE_TOUR_GEO_PARTS, "TourId"); //$NON-NLS-1$
      SQL.createIndex_Simple(stmt, TABLE_TOUR_GEO_PARTS, "GeoPart"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_LOCATION}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourLocation(final Statement stmt) throws SQLException {

      exec(stmt, "CREATE TABLE " + TABLE_TOUR_LOCATION + "   (                         " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_LOCATION, true)

            + "   name                                VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   display_name                        VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   zoomlevel                           INTEGER,                               " + NL //$NON-NLS-1$

            + "   latitudeE6_Normalized               INTEGER,                               " + NL //$NON-NLS-1$
            + "   longitudeE6_Normalized              INTEGER,                               " + NL //$NON-NLS-1$

            + "   latitudeMinE6_Normalized            INTEGER,                               " + NL //$NON-NLS-1$
            + "   latitudeMaxE6_Normalized            INTEGER,                               " + NL //$NON-NLS-1$
            + "   longitudeMinE6_Normalized           INTEGER,                               " + NL //$NON-NLS-1$
            + "   longitudeMaxE6_Normalized           INTEGER,                               " + NL //$NON-NLS-1$

            + "   latitudeMinE6_Resized_Normalized    INTEGER,                               " + NL //$NON-NLS-1$
            + "   latitudeMaxE6_Resized_Normalized    INTEGER,                               " + NL //$NON-NLS-1$
            + "   longitudeMinE6_Resized_Normalized   INTEGER,                               " + NL //$NON-NLS-1$
            + "   longitudeMaxE6_Resized_Normalized   INTEGER,                               " + NL //$NON-NLS-1$

            + "   lastModified                        BIGINT DEFAULT 0,                      " + NL //$NON-NLS-1$

            + "   appliedName                VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            /*
             * Address fields
             */

            + "   continent                  VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   country                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   country_code               VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   region                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   state                      VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   state_district             VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   county                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   municipality               VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   city                       VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   town                       VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   village                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   city_district              VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   district                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   borough                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   suburb                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   subdivision                VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   hamlet                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   croft                      VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   isolated_dwelling          VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   neighbourhood              VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   allotments                 VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   quarter                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   city_block                 VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   residential                VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   farm                       VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   farmyard                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   industrial                 VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   commercial                 VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   retail                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   road                       VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   house_number               VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   house_name                 VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   aerialway                  VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   aeroway                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   amenity                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   boundary                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   bridge                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   club                       VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   craft                      VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   emergency                  VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   historic                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   landuse                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   leisure                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   man_made                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   military                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   mountain_pass              VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

//          fix for ERROR 42X01: Syntax error: Encountered "natural" at line 55, column 4.
            + "   natural2                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   office                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   place                      VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   railway                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   shop                       VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   tourism                    VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   tunnel                     VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   waterway                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   postcode                   VARCHAR(" + TourLocation.DB_FIELD_LENGTH + ")   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")" //                                                                                //$NON-NLS-1$
      );
   }

   /**
    * Create table {@link #TABLE_TOUR_MARKER} for {@link TourMarker}.
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourMarker(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourMarker
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_MARKER + "   (                           " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_MARKER, true)

            + "   " + KEY_TOUR + "           BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 57 - begin

            + "   " + KEY_MARKER_TYPE + "    BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 57 - end

            + "   time                       INTEGER NOT NULL,                         " + NL //$NON-NLS-1$

            // before version 20
            // + "   distance                INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   distance                   INTEGER,                                  " + NL //$NON-NLS-1$

            // Version 20 - begin

            + "   distance20                 FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$

            // Version 20 - end

            // Version 22 - begin

            + "   IsMarkerVisible            INTEGER DEFAULT 1,                        " + NL //$NON-NLS-1$

            // Version 22 - end

            // Version 24 - begin

            + "   description                VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),        " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlText                    VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),             " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlAddress                 VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 24 - end

            // Version 25 - begin

            + "   tourTime                   BIGINT DEFAULT " + Long.MIN_VALUE + ",             " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // When DEFAULT value is NOT set, this exception occurs:
            //
            //java.lang.IllegalArgumentException: Can not set float field net.tourbook.data.TourMarker.altitude to null value
            //   at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:176)
            //   at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:180)
            //   at sun.reflect.UnsafeFloatFieldAccessorImpl.set(UnsafeFloatFieldAccessorImpl.java:92)
            //   at java.lang.reflect.Field.set(Field.java:753)
            //   at org.hibernate.property.DirectPropertyAccessor$DirectSetter.set(DirectPropertyAccessor.java:102)
            //
            + "   altitude                   FLOAT DEFAULT " + SQL_FLOAT_MIN_VALUE + ",         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   latitude                   DOUBLE DEFAULT " + SQL_DOUBLE_MIN_VALUE + ",       " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   longitude                  DOUBLE DEFAULT " + SQL_DOUBLE_MIN_VALUE + ",       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 25 - end

            + "   serieIndex                 INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   type                       INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   visualPosition             INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   label                      VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   category                   VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 2
            + "   labelXOffset               INTEGER,                                  " + NL //$NON-NLS-1$
            + "   labelYOffset               INTEGER,                                  " + NL //$NON-NLS-1$
            + "   markerType                 BIGINT                                    " + NL //$NON-NLS-1$

            + ")" //$NON-NLS-1$
      );

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_MARKER, KEY_TOUR);
   }

   /**
    * Create table {@link #TABLE_TOUR_MARKER_TYPE}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourMarkerType(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourMarkerType
       */

      exec(stmt,

            "CREATE TABLE " + TABLE_TOUR_MARKER_TYPE + "   (                           " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + SQL.createField_EntityId(ENTITY_ID_MARKER_TYPE, true)

                  + "   name              VARCHAR(" + TourMarkerType.DB_LENGTH_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   description       VARCHAR(" + TourMarkerType.DB_LENGTH_DESCRIPTION + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   foregroundColor   INTEGER DEFAULT 0,                           " + NL //$NON-NLS-1$
                  + "   backgroundColor   INTEGER DEFAULT 0                            " + NL //$NON-NLS-1$

                  + ")"); //$NON-NLS-1$

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_MARKER, KEY_MARKER_TYPE);
   }

   /**
    * Create table {@link #TABLE_TOUR_NUTRITION_PRODUCT} for {@link TourNutritionProduct}.
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourNutritionProduct(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourNutritionProduct
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_NUTRITION_PRODUCT + "   (                           " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_NUTRITIONPRODUCT, true)

            + "   " + KEY_TOUR + "           BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_BEVERAGE_CONTAINER + "  BIGINT,                              " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 55 - begin

            + "   productCode              VARCHAR(" + TourNutritionProduct.DB_LENGTH_CODE + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   consumedQuantity         FLOAT,                                                " + NL //$NON-NLS-1$
            + "   quantityType             VARCHAR(" + TourNutritionProduct.DB_LENGTH_NAME + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   brand                    VARCHAR(" + TourNutritionProduct.DB_LENGTH_NAME + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   name                     VARCHAR(" + TourNutritionProduct.DB_LENGTH_NAME + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   calories                 INTEGER,                                              " + NL //$NON-NLS-1$
            + "   calories_Serving         INTEGER,                                              " + NL //$NON-NLS-1$
            + "   sodium                   INTEGER,                                              " + NL //$NON-NLS-1$
            + "   sodium_Serving           INTEGER,                                              " + NL //$NON-NLS-1$
            + "   isBeverage               BOOLEAN DEFAULT FALSE,                                " + NL //$NON-NLS-1$
            + "   isCustomProduct          BOOLEAN DEFAULT FALSE,                                " + NL //$NON-NLS-1$
            + "   beverageQuantity         INTEGER,                                              " + NL //$NON-NLS-1$
            + "   beverageQuantity_Serving INTEGER,                                              " + NL //$NON-NLS-1$
            + "   containersConsumed       FLOAT,                                                " + NL //$NON-NLS-1$

            // Version 55 - end

            // Version 58 - begin

            + "   carbohydrates            INTEGER,                                              " + NL //$NON-NLS-1$
            + "   carbohydrates_Serving    INTEGER                                               " + NL //$NON-NLS-1$

            // Version 58 - end

            + ")" //$NON-NLS-1$
      );

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_NUTRITION_PRODUCT, KEY_TOUR);
   }

   /**
    * create table {@link #TABLE_TOUR_PERSON}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourPerson(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourPerson
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON + "   (                           " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_PERSON, true)

            + "   lastName                   VARCHAR(" + TourPerson.DB_LENGTH_LAST_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   firstName                  VARCHAR(" + TourPerson.DB_LENGTH_FIRST_NAME + "),         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   weight                     FLOAT,                                                    " + NL //$NON-NLS-1$ // kg
            + "   height                     FLOAT,                                                    " + NL //$NON-NLS-1$ // m

            // version 15 start

            + "   BirthDay                   BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$

            // version 15 end ---------

            // version 16 start

            + "   Gender                     INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   RestPulse                  INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   MaxPulse                   INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   HrMaxFormula               INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$

            // version 16 end ---------

            + "   rawDataPath                VARCHAR(" + TourPerson.DB_LENGTH_RAW_DATA_PATH + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   deviceReaderId             VARCHAR(" + TourPerson.DB_LENGTH_DEVICE_READER_ID + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   " + KEY_BIKE + "           BIGINT                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_PERSON_HRZONE} for {@link TourPersonHRZone}.
    * <p>
    * Table is available since db version 16
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourPersonHRZone(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourPersonHRZone
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON_HRZONE + "   (                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_HR_ZONE, true)

            + "   " + KEY_PERSON + "   BIGINT,                                         " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   zoneName                   VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   nameShortcut               VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   description                VARCHAR(" + TourPersonHRZone.DB_LENGTH_DESCRIPTION + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 18 start

            + "   ColorRed                   INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   ColorGreen                 INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   ColorBlue                  INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$

            // version 18 end ------         ---

            + "   zoneMinValue               INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   zoneMaxValue               INTEGER NOT NULL                          " + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_PHOTO}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourPhoto(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourPhoto
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PHOTO + "   (                            " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_PHOTO, true)

            + "   " + KEY_TOUR + "           BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 23 start

            + "   imageFileName              VARCHAR(" + DB_LENGTH_FILE_PATH + "),       " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFileExt               VARCHAR(" + DB_LENGTH_FILE_PATH + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFilePath              VARCHAR(" + DB_LENGTH_FILE_PATH + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFilePathName          VARCHAR(" + DB_LENGTH_FILE_PATH + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageExifTime              BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$
            + "   imageFileLastModified      BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$

            + "   adjustedTime               BIGINT DEFAULT 0,                         " + NL //$NON-NLS-1$

            + "   ratingStars                INT DEFAULT 0,                            " + NL //$NON-NLS-1$
            + "   isGeoFromPhoto             INT DEFAULT 0,                            " + NL //$NON-NLS-1$
            + "   latitude                   DOUBLE DEFAULT 0,                         " + NL //$NON-NLS-1$
            + "   longitude                  DOUBLE DEFAULT 0,                         " + NL //$NON-NLS-1$

            // version 23 end

            // version 56 start

            + "   photoAdjustmentsJSON       VARCHAR(" + VARCHAR_MAX_LENGTH + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 56 end

            // version 57 start

            + "   photoLabel                 VARCHAR(" + VARCHAR_MAX_LENGTH + ")       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 57 end

            + ")" //                                                                          //$NON-NLS-1$
      );

      // Create index for {@link TourPhoto}, it will dramatically improve performance.
      SQL.createIndex_Simple(stmt, TABLE_TOUR_PHOTO, "ImageFilePathName"); //$NON-NLS-1$

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_PHOTO, KEY_TOUR);
   }

   /**
    * create table {@link #TABLE_TOUR_REFERENCE}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourReference(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourReference
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_REFERENCE + "   (                        " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_REF, true)

            + "   " + KEY_TOUR + "           BIGINT,                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   startIndex                 INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   endIndex                   INTEGER NOT NULL,                         " + NL //$NON-NLS-1$
            + "   label                      VARCHAR(" + TourReference.DB_LENGTH_LABEL + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 51 start

            + "   isTourFilter_ElevationDiff BOOLEAN DEFAULT FALSE,                    " + NL //$NON-NLS-1$
            + "   isTourFilter_GeoDiff       BOOLEAN DEFAULT FALSE,                    " + NL //$NON-NLS-1$
            + "   isTourFilter_MaxResults    BOOLEAN DEFAULT FALSE,                    " + NL //$NON-NLS-1$

            + "   tourFilter_ElevationDiff   FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
            + "   tourFilter_GeoDiff         FLOAT DEFAULT 0,                          " + NL //$NON-NLS-1$
            + "   tourFilter_MaxResults      INT   DEFAULT 0                           " + NL //$NON-NLS-1$

            // version 51 end ---------

            + ")" //$NON-NLS-1$
      );

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_REFERENCE, KEY_TOUR);
   }

   /**
    * Create table {@link #TABLE_TOUR_TAG} which contains {@link TourTag} entities.
    *
    * @param stmt
    *
    * @throws SQLException
    *
    * @since DB version 5
    */
   private void createTable_TourTag(final Statement stmt) throws SQLException {

      /*
       * Create table: TOURTAG
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG + "   (                              " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_TAG, true)

            + "   isRoot                     INTEGER,                                  " + NL //$NON-NLS-1$
            + "   expandType                 INTEGER,                                  " + NL //$NON-NLS-1$
            + "   name                       VARCHAR(" + DB_LENGTH_NAME + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 start

            + "   notes                      VARCHAR(" + DB_LENGTH_NOTES + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 end ---------

            // version 49 start

            + "   imageFilePath              VARCHAR(" + DB_LENGTH_FILE_PATH + ")       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 49 end ---------

            + ")"); //$NON-NLS-1$

      /**
       * Create table: TOURDATA_TOURTAG
       */
      exec(stmt,

            "CREATE TABLE " + JOINTABLE__TOURDATA__TOURTAG + "   (                     " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   " + KEY_TAG + "      BIGINT NOT NULL,                          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + KEY_TOUR + "     BIGINT NOT NULL                           " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + ")" //                                                                    //$NON-NLS-1$
      );

      // Add Constraint
      final String fkName = "fk_" + JOINTABLE__TOURDATA__TOURTAG + "_" + KEY_TOUR; //         //$NON-NLS-1$ //$NON-NLS-2$

      exec(stmt,

            "ALTER TABLE " + JOINTABLE__TOURDATA__TOURTAG + "                          " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkName + "                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TOUR + ")                                 " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_DATA + " (" + ENTITY_ID_TOUR + ")    " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      // Create index TOURTAG_TagID
      SQL.createIndex_Simple(stmt, JOINTABLE__TOURDATA__TOURTAG, KEY_TAG);
   }

   /**
    * Create table {@link #TABLE_TOUR_TAG_CATEGORY} for which contains {@link TourTagCategory}
    * entities.
    *
    * @param stmt
    *
    * @throws SQLException
    *
    * @since DB version 5
    */
   private void createTable_TourTag_Category(final Statement stmt) throws SQLException {

      /**
       * Create table: TOURTAGCATEGORY
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG_CATEGORY + "   (                     " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_TAG_CATEGORY, true)

            + "   isRoot                     INTEGER,                                  " + NL //$NON-NLS-1$
            + "   name                       VARCHAR(" + DB_LENGTH_NAME + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 start

            + "   notes                      VARCHAR(" + DB_LENGTH_NOTES + ")           " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 end ---------

            + ")" //$NON-NLS-1$
      );

      /**
       * Create table: TOURTAGCATEGORY_TOURTAG
       */
      final String jtabTag = JOINTABLE__TOURTAGCATEGORY_TOURTAG;

      exec(stmt, "CREATE TABLE " + jtabTag + "   (                                     " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   " + KEY_TAG + "               BIGINT NOT NULL,                       " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_TAG_CATEGORY + "      BIGINT NOT NULL                        " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")"); //$NON-NLS-1$

      // add constraints
      final String fkTag = "fk_" + jtabTag + "_" + KEY_TAG; //                                           //$NON-NLS-1$ //$NON-NLS-2$
      final String fkCat = "fk_" + jtabTag + "_" + TABLE_TOUR_TAG_CATEGORY + "_" + KEY_TAG_CATEGORY; //  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      exec(
            stmt,
            "ALTER TABLE " + jtabTag + "                                               " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkTag + "                                   " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG + ")                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG + " (" + ENTITY_ID_TAG + ")      " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      exec(
            stmt,
            "ALTER TABLE " + jtabTag + "                                                              " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkCat + "                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG_CATEGORY + ")                                        " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")   " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      /**
       * Create table: TOURTAGCATEGORY_TOURTAGCATEGORY
       */
      final String jtabCategory = JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY;

      exec(stmt, "CREATE TABLE " + jtabCategory + "   (                                " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   " + KEY_TAG_CATEGORY + "1   BIGINT NOT NULL,                         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_TAG_CATEGORY + "2   BIGINT NOT NULL                          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")"); //$NON-NLS-1$

      // add constraints
      final String fk1 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "1"; //             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final String fk2 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "2"; //             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      exec(
            stmt,
            "ALTER TABLE " + jtabCategory + "                                                         " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fk1 + "                                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG_CATEGORY + "1)                                       " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")   " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      exec(
            stmt,
            "ALTER TABLE " + jtabCategory + "                                                         " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fk2 + "                                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG_CATEGORY + "2)                                       " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")   " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );
   }

   /**
    * create table {@link #TABLE_TOUR_TYPE}
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourType(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourType
       */

      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TYPE + "   (                             " + NL //$NON-NLS-1$ //$NON-NLS-2$

      //

            + SQL.createField_EntityId(ENTITY_ID_TOUR_TYPE, true)

            + "   name                       VARCHAR(" + TourType.DB_LENGTH_NAME + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

// renamed/converted rgb fields into one field with version 44
//
//          + "   colorBrightRed             SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//          + "   colorBrightGreen           SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//          + "   colorBrightBlue            SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//
//          + "   colorDarkRed               SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//          + "   colorDarkGreen             SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//          + "   colorDarkBlue              SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//
//          + "   colorLineRed               SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//          + "   colorLineGreen             SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//          + "   colorLineBlue              SMALLINT NOT NULL,                        " + NL //$NON-NLS-1$
//
//          // version 19 start
//
//          + "   colorTextRed               SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$
//          + "   colorTextGreen             SMALLINT DEFAULT 0,                       " + NL //$NON-NLS-1$
//          + "   colorTextBlue              SMALLINT DEFAULT 0                        " + NL //$NON-NLS-1$
//
//          // version 19 end ---------

            // version 44 start

            + "   Color_Gradient_Bright      INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   Color_Gradient_Dark        INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$

            + "   Color_Line_LightTheme      INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   Color_Line_DarkTheme       INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$

            + "   Color_Text_LightTheme      INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$
            + "   Color_Text_DarkTheme       INTEGER DEFAULT 0,                        " + NL //$NON-NLS-1$

            // version 44 end ---------

            // version 57 start

            + "   importCategory             VARCHAR(" + VARCHAR_MAX_LENGTH + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   importSubCategory          VARCHAR(" + VARCHAR_MAX_LENGTH + ")       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 57 end

            + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_WAYPOINT} *
    * <p>
    * since db version 10
    *
    * @param stmt
    *
    * @throws SQLException
    */
   private void createTable_TourWayPoint(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourWayPoint
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_WAYPOINT + "   (                                        " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.createField_EntityId(ENTITY_ID_WAY_POINT, true)

            + "   " + KEY_TOUR + "           BIGINT,                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   latitude                   DOUBLE NOT NULL,                                         " + NL //$NON-NLS-1$
            + "   longitude                  DOUBLE NOT NULL,                                         " + NL //$NON-NLS-1$
            + "   time                       BIGINT,                                                  " + NL //$NON-NLS-1$
            + "   altitude                   FLOAT,                                                   " + NL //$NON-NLS-1$
            + "   name                       VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   description                VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   comment                    VARCHAR(" + TourWayPoint.DB_LENGTH_COMMENT + "),         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   symbol                     VARCHAR(" + TourWayPoint.DB_LENGTH_SYMBOL + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   category                   VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),        " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 28 start - create common fields with TourMarker

            + "   urlText                    VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlAddress                 VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + ")        " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 28 end ---------

            + ")" //$NON-NLS-1$
      );

      SQL.createIndex_Table__Column(stmt, TABLE_TOUR_WAYPOINT, KEY_TOUR);
   }

   public void firePropertyChange(final int propertyId) {

      final Object[] allListeners = _propertyListeners.getListeners();

      for (final Object allListener : allListeners) {
         final IPropertyListener listener = (IPropertyListener) allListener;
         listener.propertyChanged(TourDatabase.this, propertyId);
      }
   }

   public Connection getConnection() throws SQLException {

      if (sqlStartup_10_IsSqlServerUpAndRunning()) {
         return getConnection_NotPooled();
      } else {
         return null;
      }
   }

   /**
    * @return Returns a connection to the tour database.
    *         <p>
    *         <b> The pooled connection is not used because the database could be shutdown when it
    *         needs to be upgraded. </b>
    *
    * @throws SQLException
    */
   private Connection getConnection_NotPooled() throws SQLException {

      return DriverManager.getConnection(SQL_DB_TOUR_DB_URL, SQL_DB_USERNAME, SQL_DB_PASSWORD);
   }

   /**
    * <b>Hikari or c3p0 did NOT work with H2,</b> the tour book view did not properly update when
    * changing the filter for recent time
    *
    * @return
    *
    * @throws SQLException
    */
   @SuppressWarnings("unused")
   private Connection getConnection_Pooled() throws SQLException {

      return null;

//      if (_pooledDataSource == null) {
//
//         synchronized (DB_LOCK) {
//
//            // check again
//            if (_pooledDataSource == null) {
//
//               // 1. Configure the pool
//               final HikariConfig config = new HikariConfig();
//
//               // Swap this URL for any SQL server (H2, PostgreSQL, MySQL, etc.)
//               config.setJdbcUrl(SQL_DB_TOUR_DB_URL);
//               config.setDriverClassName(SQL_DB_DRIVER_CLASS);
//               config.setUsername(SQL_DB_USERNAME);
//               config.setPassword(SQL_DB_PASSWORD);
//
//               // 2. Pool sizing & timeouts
//               config.setMaximumPoolSize(10); //         Max concurrent connections
//               config.setMinimumIdle(2); //              Keep at least 2 connections ready
//               config.setConnectionTimeout(30000); //    Wait max 30s for a connection
//               config.setIdleTimeout(600000); //         Close idle connections after 10 mins
//               config.setMaxLifetime(1800000); //        Recycle connections every 30 mins
//               config.setAutoCommit(true); //            Explicitly enforce autoCommit on checkout
//
//               // 3. Optional: Detect connection leaks (logs a warning if not closed within 5s)
//               config.setLeakDetectionThreshold(5000);
//
//               // 4. Create the DataSource (keep a single instance for your entire app)
//               _pooledDataSource = new HikariDataSource(config);
//            }
//         }
//      }
//
//      try {
//
//         return _pooledDataSource.getConnection();
//
//      } catch (final SQLException e) {
//
//         UI.showSQLException(e);
//
//         throw e;
//      }
   }

   public int getDbVersion(final Connection conn, final String table) {

      try (Statement stmt = conn.createStatement()) {

         final ResultSet result = stmt.executeQuery("SELECT * FROM " + table); //$NON-NLS-1$

         if (result.next()) {

            return result.getInt(1);

         } else {

            // this case should not happen

            throw new RuntimeException(String.format("Table '%s' do not contain any records", table));//$NON-NLS-1$
         }

      } catch (final SQLException e) {

         UI.showSQLException(e);
         PlatformUI.getWorkbench().close();
      }

      return -1;
   }

   /**
    * Creates an entity manager which is used to persist entities
    *
    * @return
    */
   public EntityManager getEntityManager() {

      if (_emFactory == null) {

         // ensure db is valid BEFORE entity manager is initialized which can shutdown the database
         if (sqlStartup_10_IsSqlServerUpAndRunning() == false) {
            return null;
         }
      }

      if (_emFactory == null) {
         try {
            throw new Exception("Cannot get EntityManagerFactory"); //$NON-NLS-1$
         } catch (final Exception e) {
            StatusUtil.log(e);
         }
         return null;
      } else {
         return _emFactory.createEntityManager();
      }
   }

   public void removePropertyListener(final IPropertyListener listener) {
      _propertyListeners.remove(listener);
   }

   public void setupDerbyCustomFunctions_AvgSpeedPace() {

//      if (_isCustomFunctionSetup_AvgSpeedPace) {
//         return;
//      }
//
//      try (Connection conn = getInstance().getConnection();
//            Statement stmt = conn.createStatement()) {
//
//         final Bundle derbyBundle = Platform.getBundle("net.tourbook.ext.apache"); //$NON-NLS-1$
//
//         // bundleentry://26.fwk1091632842/mytourbook-derby-functions.jar
//         final URL bundleUrl = derbyBundle.getEntry("/mytourbook-derby-functions.jar"); //$NON-NLS-1$
//
//         // C:/E/e-431/eclipse/../../../DAT/MT/mytourbook/bundles/net.tourbook.ext.apache/mytourbook-derby-functions.jar
//         final String sqlCustomJarFilePath = NIO.getAbsolutePathFromBundleUrl(bundleUrl);
//
//         /*
//          * Install (replace) jar file, hints are from
//          * https://stackoverflow.com/questions/38369703/regex-in-apache-derby#answer-38369704
//          */
//         exec_IgnoreException(stmt, "CALL SQLJ.REMOVE_JAR('App.StoredProcedures', 0)"); //$NON-NLS-1$
//         exec(stmt, "CALL SQLJ.INSTALL_JAR('%s', 'App.StoredProcedures', 0)".formatted(sqlCustomJarFilePath)); //$NON-NLS-1$
//         exec(stmt, "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.classpath', 'App.StoredProcedures')"); //$NON-NLS-1$
//
//         /*
//          * Replace functions
//          */
//         exec_IgnoreException(stmt, "DROP FUNCTION avgSpeed"); //$NON-NLS-1$
//         exec(stmt,
//
//               UI.EMPTY_STRING
//
//                     + "CREATE FUNCTION avgSpeed (tourTime BIGINT, tourDistance BIGINT)" + NL //                  //$NON-NLS-1$
//                     + "RETURNS REAL" + NL //                                                                     //$NON-NLS-1$
//                     + "PARAMETER STYLE JAVA" + NL //                                                             //$NON-NLS-1$
//                     + "NO SQL" + NL //                                                                           //$NON-NLS-1$
//                     + "LANGUAGE JAVA" + NL //                                                                    //$NON-NLS-1$
//                     + "EXTERNAL NAME 'net.tourbook.ext.apache.custom.DerbyCustomFunctions.avgSpeed'" + NL //     //$NON-NLS-1$
//         );
//
//         exec_IgnoreException(stmt, "DROP FUNCTION avgPace"); //$NON-NLS-1$
//         exec(stmt,
//
//               UI.EMPTY_STRING
//
//                     + "CREATE FUNCTION avgPace (tourTime BIGINT, tourDistance BIGINT)" + NL //                   //$NON-NLS-1$
//                     + "RETURNS REAL" + NL //                                                                     //$NON-NLS-1$
//                     + "PARAMETER STYLE JAVA" + NL //                                                             //$NON-NLS-1$
//                     + "NO SQL" + NL //                                                                           //$NON-NLS-1$
//                     + "LANGUAGE JAVA" + NL //                                                                    //$NON-NLS-1$
//                     + "EXTERNAL NAME 'net.tourbook.ext.apache.custom.DerbyCustomFunctions.avgPace'" + NL //      //$NON-NLS-1$
//         );
//
//         _isCustomFunctionSetup_AvgSpeedPace = true;
//
//      } catch (final SQLException e) {
//         UI.showSQLException(e);
//
//      } catch (final IOException e) {
//         StatusUtil.log(e);
//      }
   }

   /**
    * Shutdown database server
    */
   public void shutdownDatabaseServer() {

//      if (_pooledDataSource != null) {
//
//         StatusUtil.logInfo("Closing pooled connections");
//
//         _pooledDataSource.close();
//      }

//      Connection conn = null;
//
//      try {
//
//         // shutdown database that all connections are closed
//
//      if (connection != null) {
//         try (Statement stmt = connection.createStatement()) {
//             // SHUTDOWN closes the database immediately
//             // Use "SHUTDOWN COMPACT" if you want to defragment the file on exit
//             stmt.execute("SHUTDOWN");
//         } catch (SQLException e) {
//             // Handle or log exception
//         } finally {
//             try {
//                 if (!connection.isClosed()) {
//                     connection.close();
//                 }
//             } catch (SQLException e) {
//                 // Handle close exception
//             }
//         }
//     }
//
//
//      } catch (final SQLException e) {
//
//         final String sqlExceptionText = Util.getSQLExceptionText(e);
//
//         // log also the stacktrace
//         StatusUtil.log(sqlExceptionText + Util.getStackTrace(e), e);
//
//      } finally {
//
//         Util.closeSql(conn);
//      }

   }

   /**
    * Is checking that the tour sql server is up and running and the tour database design and data
    * are updated to the current {@link #TOURBOOK_DB_VERSION}
    *
    * @return <code>true</code> when the tour sql server can be used.
    */
   private boolean sqlStartup_10_IsSqlServerUpAndRunning() {

      if (_isDbInitialized || _isDbInDataUpdate || _isInDesignUpdate) {
         return true;
      }

      // check if the driver can be loaded
      try {

         Class.forName(SQL_DB_DRIVER_CLASS);

      } catch (final ClassNotFoundException e) {

         StatusUtil.showStatus(e.getMessage(), e);
         return false;
      }

      final boolean[] returnState = { false };

      try {

         final Runnable runnable = () -> {

            final SplashManager splashManager = SplashManager.getInstance();

            splashManager.setMessage(Messages.App_SplashMessage_StartingDatabase);
            try {

               sqlStartup_30_Check_DbIsCreated();

            } catch (final Throwable e) {

               StatusUtil.log(e);
               return;
            }

            sqlStartup_40_CheckTable(splashManager);

            if (sqlStartup_50_IsDesignVersionValid(splashManager) == false) {
               return;
            }

            sqlStartup_60_SetupEntityManager(splashManager);

            _isDbInDataUpdate = true;
            {
               if (sqlStartup_70_IsDataVersionValid(splashManager) == false) {
                  return;
               }
            }
            _isDbInDataUpdate = false;

            if (!_isSilentDatabaseUpdate &&
                  _dbDesignVersion_Old != _dbDesignVersion_New) {

               // display info for the successful update

               MessageDialog.openInformation(
                     splashManager.getShell(),
                     Messages.tour_database_version_info_title,
                     NLS.bind(Messages.Tour_Database_UpdateInfo, _dbDesignVersion_Old, _dbDesignVersion_New));
            }

            splashManager.setMessage(Messages.App_SplashMessage_Finalize);

            returnState[0] = true;
         };

         runnable.run();

      } finally {
         _isDbInitialized = returnState[0];
      }

      return returnState[0];
   }

   private void sqlStartup_30_Check_DbIsCreated() {

      if (_isChecked_DbCreated) {
         return;
      }

      // ensure driver is loaded
      try {
         Class.forName(SQL_DB_DRIVER_CLASS);
      } catch (final ClassNotFoundException e) {
         StatusUtil.showStatus(e);
      }

      /*
       * Get a connection, this also creates the database when not yet available. The embedded
       * driver displays a warning when database already exist.
       */
      try (Connection conn = getConnection_NotPooled()) {

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } finally {

         _isChecked_DbCreated = true;
      }
   }

   /**
    * Check if the table in the database exist
    *
    * @param splashManager
    */
   private void sqlStartup_40_CheckTable(final SplashManager splashManager) {

      if (_isTableChecked) {
         return;
      }

      try (Connection conn = getConnection_NotPooled()) {

         /*
          * Check if the tourdata table exists
          */
         final DatabaseMetaData metaData = conn.getMetaData();
         final ResultSet tables = metaData.getTables(null, null, null, null);
         while (tables.next()) {

            final String tableName = tables.getString(3);

            if (tableName.equalsIgnoreCase(TABLE_TOUR_DATA)) {

               // table exists

               _isTableChecked = true;

               return;
            }
         }

         /*
          * Create tables
          */
         if (splashManager != null) {
            splashManager.setMessage(Messages.Database_Monitor_CreateDatabase);
         }

         try (Statement stmt = conn.createStatement()) {

            sqlStartup_42_CreateAllTables(stmt);

         } catch (final SQLException e) {
            UI.showSQLException(e);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   void sqlStartup_42_CreateAllTables(final Statement stmt) throws SQLException {

      createTable_TourData(stmt);

      createTable_TourPerson(stmt);
      createTable_TourPersonHRZone(stmt);
      createTable_TourType(stmt);
      createTable_TourBeverageContainer(stmt);
      createTable_TourNutritionProduct(stmt);
      createTable_TourMarker(stmt);
      createTable_TourMarkerType(stmt);
      createTable_TourPhoto(stmt);
      createTable_TourReference(stmt);
      createTable_TourCompared(stmt);
      createTable_TourBike(stmt);
      createTable_TourGeoParts(stmt);
      createTable_DeviceSensor(stmt);
      createTable_DeviceSensorValues(stmt);
      createTable_TourLocation(stmt);

      createTable_DbVersion_Design(stmt);
      createTable_DbVersion_Data(stmt, TOURBOOK_DB_VERSION);

      createTable_TourTag(stmt);
      createTable_TourTag_Category(stmt);

      createTable_Equipment(stmt);
      createTable_EquipmentPart(stmt);

      createTable_TourWayPoint(stmt);
   }

   /**
    * @param splashManager
    *           Progress monitor or <code>null</code> when the monitor is not available
    *
    * @return <code>true</code> when design version is updated to the last version, returns
    *         <code>false</code> when an error occurred
    */
   private boolean sqlStartup_50_IsDesignVersionValid(final SplashManager splashManager) {

      if (_isDesignVersionChecked) {
         return true;
      }

      if (_isSQLDesignUpdateError) {
         return false;
      }

      try (Connection conn = getConnection_NotPooled()) {

         // check if the database contains the correct version

         _dbVersion_BeforeDesignUpdate = getDbVersion(conn, TABLE_DB_VERSION_DESIGN);

         logDbUpdate("Current database DESIGN version: " + _dbVersion_BeforeDesignUpdate); //$NON-NLS-1$

         if (_dbVersion_BeforeDesignUpdate < TOURBOOK_DB_VERSION) {

            if (updateDb__1_Design(_dbVersion_BeforeDesignUpdate, splashManager) == false) {
               return false;

            }

         } else if (_dbVersion_BeforeDesignUpdate > TOURBOOK_DB_VERSION) {

            /*
             * Current db version is HIGHER than the db version which the code is created for,
             * this can occur during the development and should not happen by the end users.
             */

            MessageDialog.openInformation(
                  splashManager.getShell(),
                  Messages.tour_database_version_info_title,
                  NLS.bind(Messages.tour_database_version_info_message,
                        _dbVersion_BeforeDesignUpdate,
                        TOURBOOK_DB_VERSION));

            PlatformUI.getWorkbench().close();
         }

         _isDesignVersionChecked = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);
         PlatformUI.getWorkbench().close();
      }

      return _isDesignVersionChecked;
   }

   private synchronized void sqlStartup_60_SetupEntityManager(final SplashManager splashManager) {

      splashManager.setMessage(Messages.Database_Monitor_persistent_service_task);

      final Ejb3Configuration cfg = new Ejb3Configuration();

      // 1. Define EJB3 / Hibernate properties
      final Properties props = new Properties();

//SET_FORMATTING_OFF

      // Database Connection Settings
      props.put("hibernate.connection.driver_class",  SQL_DB_DRIVER_CLASS);
      props.put("hibernate.connection.url",           SQL_DB_TOUR_DB_URL);
      props.put("hibernate.connection.username",      SQL_DB_USERNAME);
      props.put("hibernate.connection.password",      SQL_DB_PASSWORD);

      // Hibernate Dialect and SQL tuning
      props.put("hibernate.dialect",                  SQL_DB_HIBERNATE_DIALECT);
      props.put("hibernate.archive.autodetection",    "none");

      // debug sql
//    props.put("hibernate.show_sql",                 "true");
//    props.put("hibernate.format_sql",               "true");

      // DDL Schema generation strategies
//    props.put("hibernate.hbm2ddl.auto",             "update");

      // Inject properties into the configuration instance
      cfg.addProperties(props);

      // Manually register annotated Entity classes
      cfg.addAnnotatedClass(DeviceSensor           .class);
      cfg.addAnnotatedClass(DeviceSensorValue      .class);
      cfg.addAnnotatedClass(Equipment              .class);
      cfg.addAnnotatedClass(EquipmentPart          .class);
      cfg.addAnnotatedClass(TourBeverageContainer  .class);
      cfg.addAnnotatedClass(TourBike               .class);
      cfg.addAnnotatedClass(TourCompared           .class);
      cfg.addAnnotatedClass(TourData               .class);
      cfg.addAnnotatedClass(TourLocation           .class);
      cfg.addAnnotatedClass(TourMarker             .class);
      cfg.addAnnotatedClass(TourMarkerType         .class);
      cfg.addAnnotatedClass(TourNutritionProduct   .class);
      cfg.addAnnotatedClass(TourPerson             .class);
      cfg.addAnnotatedClass(TourPersonHRZone       .class);
      cfg.addAnnotatedClass(TourPhoto              .class);
      cfg.addAnnotatedClass(TourReference          .class);
      cfg.addAnnotatedClass(TourTag                .class);
      cfg.addAnnotatedClass(TourTagCategory        .class);
      cfg.addAnnotatedClass(TourType               .class);
      cfg.addAnnotatedClass(TourWayPoint           .class);

//SET_FORMATTING_ON

      // Build the Persistence Unit Factory
      _emFactory = cfg.buildEntityManagerFactory();
   }

   /**
    * Is checking the tour database data is updated to the current {@link #TOURBOOK_DB_VERSION}
    *
    * @param splashManager
    *           Progress monitor or <code>null</code> when the monitor is not available
    *
    * @return <code>true</code> when data version is updated to the last version, returns
    *         <code>false</code> when an error occurred
    */
   private boolean sqlStartup_70_IsDataVersionValid(final SplashManager splashManager) {

      if (_isDataVersionChecked) {
         return true;
      }

      if (_isSQLDataUpdateError) {
         return false;
      }

      try (Connection conn = getConnection_NotPooled()) {

         // check if the database contains the correct version

         final int dbVersion_BeforeDataUpdate = getDbVersion(conn, TABLE_DB_VERSION_DATA);

         logDbUpdate("Current database DATA version: " + dbVersion_BeforeDataUpdate); //$NON-NLS-1$

         if (dbVersion_BeforeDataUpdate < TOURBOOK_DB_VERSION) {

            if (updateDb__2_Data(conn, splashManager) == false) {
               return false;
            }

         } else if (dbVersion_BeforeDataUpdate > TOURBOOK_DB_VERSION) {

            /*
             * Current db version is HIGHER than the db version which the code is created for,
             * this can occur during the development and should not happen by the end users.
             */

            MessageDialog.openInformation(
                  splashManager.getShell(),
                  Messages.tour_database_version_info_title,
                  NLS.bind(Messages.tour_database_version_info_message,
                        dbVersion_BeforeDataUpdate,
                        TOURBOOK_DB_VERSION));

            PlatformUI.getWorkbench().close();
         }

         _isDataVersionChecked = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);
         PlatformUI.getWorkbench().close();
      }

      return _isDataVersionChecked;
   }

   private void testConnection() {

      final String sql = UI.EMPTY_STRING

            + "SELECT SETTING_NAME, SETTING_VALUE" + NL
            + "FROM INFORMATION_SCHEMA.SETTINGS " + NL
            + "ORDER BY SETTING_NAME" + NL;

      try (Connection conn = getConnection();
            Statement stmt = conn.createStatement()) {

         final ResultSet result = stmt.executeQuery(sql);

         while (result.next()) {

            final String name = result.getString(1);
            final String value = result.getString(2);

            System.out.println();
            System.out.println();
            System.out.println(" %s, %s".formatted(name, value));
            System.out.println();
// TODO remove SYSTEM.OUT.PRINTLN

         }

      } catch (final SQLException e) {

         UI.showSQLException(e);
      }
   }

   /**
    * This method must be updated when the database version is updated
    */
   private boolean updateDb__1_Design(int currentDbVersion, final SplashManager splashManager) {

      if (_isSilentDatabaseUpdate == false) {

         /*
          * Confirm update
          */

         // define buttons with default to "Close App"
         final String[] buttons = new String[] {
               Messages.Tour_Database_Action_UpdateDatabase,
               Messages.Tour_Database_Action_CloseApp };

         final String dialogMessage = NLS.bind(Messages.Tour_Database_Dialog_ConfirmUpdate_Message,
               new Object[] {
                     currentDbVersion,
                     TOURBOOK_DB_VERSION,
                     SQL_DB_TOUR_DB_URL });

         if ((new MessageDialog(
               splashManager.getShell(),
               Messages.Tour_Database_Dialog_ConfirmUpdate_Title,
               null,
               dialogMessage,
               MessageDialog.QUESTION,
               buttons,
               1).open()) != Window.OK) {

            // the user will not update -> close application

            ApplicationWorkbenchAdvisor.isInDbUpdateShutdown = true;

            PlatformUI.getWorkbench().close();

            return false;
         }
      }

      _isInDesignUpdate = true;

      _dbDesignVersion_New = currentDbVersion;
      _dbDesignVersion_Old = currentDbVersion;

      try (final Connection conn = getConnection_NotPooled()) {

//
//         // 60 -> 61    26.6
//         if (currentDbVersion == 60) {
//            currentDbVersion = _dbDesignVersion_New = updateDb_060_To_061(conn, splashManager);
//         }
//
//         // 61 -> 62    26.8
//         if (currentDbVersion == 61) {
//            currentDbVersion = _dbDesignVersion_New = updateDb_061_To_062(conn, splashManager);
//         }

         // 62 -> 63    26.9
         if (currentDbVersion == 62) {
            currentDbVersion = _dbDesignVersion_New = updateDb_062_To_063(conn, splashManager);
         }

         // update db design version number
         updateVersionNumber_10_AfterDesignUpdate(conn, _dbDesignVersion_New);

      } catch (final SQLException e) {

         UI.showSQLException(e);

         _isSQLDesignUpdateError = true;

         return false;

      } finally {

         _isInDesignUpdate = false;

      }

      return true;
   }

   /**
    * This method may be updated when the database version is updated
    */
   private boolean updateDb__2_Data(final Connection conn, final SplashManager splashManager) {

      /**
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       * <p>
       * Run data update AFTER the design version number is updated because the data update uses
       * connections and entitymanager which is checking the version number.
       * <p>
       * Also the data structure must be updated otherwise the entity manager fails because the
       * data structure in the database MUST be the same as in the program code.
       * <p>
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       */

      try {

//         updateDb_003_To_004_DataUpdate(conn, splashManager);
//         updateDb_004_To_005_DataUpdate(conn, splashManager);
//         updateDb_008_To_009_DataUpdate(conn, splashManager);
//         updateDb_010_To_011_DataUpdate(conn, splashManager);
//         updateDb_012_To_013_DataUpdate(conn);
//         updateDb_019_To_020_DataUpdate(conn, splashManager);
//         updateDb_021_To_022_DataUpdate(conn, splashManager);
//         updateDb_022_To_023_DataUpdate(conn, splashManager);
//         updateDb_024_To_025_DataUpdate(conn, splashManager);
//         updateDb_027_To_028_DataUpdate(conn, splashManager);
//         updateDb_028_To_029_DataUpdate(conn, splashManager);
//         updateDb_031_To_032_DataUpdate(conn, splashManager);
//         updateDb_033_To_034_DataUpdate(conn, splashManager);
//         updateDb_036_To_037_DataUpdate(conn, splashManager); //                                   37 - 19.2
//         updateDb_039_To_040_DataUpdate(conn, splashManager); //                                   40 - 19.10
//         updateDb_041_To_042_DataUpdate(conn); //                                                  42 - 20.11.1
//         updateDb_042_to_043_DataUpdate(conn, splashManager); //                                   43 - 21.3
//         updateDb_046_to_047_DataUpdate(conn, splashManager); //                                   47 - 22.3
//         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_047_to_048()); //     48 - 22.6
//         updateDb_049_To_050_DataUpdate(conn, splashManager); //                                   50 - 23.5
//         updateDb_050_To_051_DataUpdate(conn, splashManager); //                                   51 - 23.8
//         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_051_to_052()); //     52 - 24.1
//         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_053_to_054()); //     54 - 24.1
//         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_057_to_058()); //     58 - 25.6
//
//         // this must be run BEFORE the subsequent update
//         updateDb_058_To_059_DataUpdate(conn, splashManager); //                                   59 - 25.11
//         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_058_to_059()); //     59 - 25.11

// THIS CODE IS JUST KEPT FOR FUTURE USE
         updateDb_060_To_061_DataUpdate(conn, splashManager); //                                   61 - 26.6

         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_062_to_063()); //     63 - 26.9

      } catch (final SQLException e) {

         UI.showSQLException(e);
         _isSQLDataUpdateError = true;

         return false;
      }

      return true;
   }

   /**
    * This is updating {@link TourData} concurrently
    *
    * @param connection
    * @param splashManager
    * @param tourDataUpdater
    *
    * @throws SQLException
    */
   private void updateDb__3_Data_Concurrent(final Connection connection,
                                            final SplashManager splashManager,
                                            final ITourDataUpdate tourDataUpdater) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = tourDataUpdater.getDatabaseVersion();

      if (getDbVersion(connection, TABLE_DB_VERSION_DATA) >= dbDataVersion) {

         // data version is higher -> nothing to do

         return;
      }

      long lastUpdateTime = startTime;

      int tourIndex = 1;
      int lastUpdateNumItems = 1;
      int sumUpdatedTours = 0;

      List<Long> allTourIDs = tourDataUpdater.getTourIDs();

      if (allTourIDs == null) {

         // use default -> all

         allTourIDs = getAllTourIds();
      }

      final int numAllTourIds = allTourIDs.size();
      final Future<?>[] allUpdateTasks = new Future[numAllTourIds];

      for (int taskIndex = 0; taskIndex < numAllTourIds; taskIndex++) {

         final Long tourId = allTourIDs.get(taskIndex);

         if (splashManager != null) {

            final long currentTime = System.currentTimeMillis();
            final long timeDiff = currentTime - lastUpdateTime;

            // reduce logging
            if (timeDiff > DELAY_SPLASH_LOGGING

                  // update UI for the last tour otherwise it looks like that not all data are converted
                  || tourIndex == numAllTourIds) {

               lastUpdateTime = currentTime;

               final long numTourDiff = tourIndex - lastUpdateNumItems;
               lastUpdateNumItems = tourIndex;
               sumUpdatedTours += numTourDiff;

               final String percentValue = String.format(NUMBER_FORMAT_1F, (float) tourIndex / numAllTourIds * 100.0);

               splashManager.setMessage(String.format(
                     Messages.Tour_Database_PostUpdate,
                     dbDataVersion,
                     sumUpdatedTours,
                     numAllTourIds,
                     percentValue,
                     numTourDiff));
            }

            tourIndex++;
         }

         allUpdateTasks[taskIndex] = updateDb__4_Data_Concurrent_OneTour(tourId, tourDataUpdater);
      }

      // wait until all tasks are processed
      Util.waitTasks(allUpdateTasks);

      updateVersionNumber_20_AfterDataUpdate(connection, dbDataVersion, startTime);
   }

   /**
    * Do data updates concurrently with all available processor threads, this is reducing time
    * significantly.
    *
    * @param tourDataUpdater
    *           {@link ITourDataUpdate} interface to update a tour
    * @param tourId
    *           Tour ID of the tour to be updated
    *
    * @return Returns the submitted task
    */
   private Future<?> updateDb__4_Data_Concurrent_OneTour(final long tourId,
                                                         final ITourDataUpdate tourDataUpdater) {

      try {

         // put tour ID (queue item) into the queue AND wait when it is full

         _dbUpdateQueue.put(tourId);

      } catch (final InterruptedException e) {

         _isSQLDataUpdateError = true;

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      final Future<?> updateTask = _dbUpdateExecutor.submit(() -> {

         // get last added item
         final Long queueItem_TourId = _dbUpdateQueue.poll();

         if (queueItem_TourId == null) {
            return;
         }

         final EntityManager entityManager = TourDatabase.getInstance().getEntityManager();

         try {

            // get tour data by tour id
            final TourData tourData = entityManager.find(TourData.class, queueItem_TourId);
            if (tourData == null) {
               return;
            }

            /*
             * Tour update
             */
            final boolean isTourUpdated = tourDataUpdater.updateTourData(tourData);

            if (isTourUpdated == false) {
               return;
            }

            boolean isSaved = false;

            final EntityTransaction transaction = entityManager.getTransaction();
            try {

               transaction.begin();
               {
                  entityManager.merge(tourData);
               }
               transaction.commit();

            } catch (final Exception e) {

               _isSQLDataUpdateError = true;
               StatusUtil.showStatus(e);

            } finally {
               if (transaction.isActive()) {
                  transaction.rollback();
               } else {
                  isSaved = true;
               }
            }

            if (!isSaved) {
               showTourSaveError(tourData);
            }

         } finally {

            entityManager.close();
         }
      });

      return updateTask;
   }

   /**
    * Update field <code>isRetired</code> in the equipment/parts
    *
    * @param conn
    * @param splashManager
    *
    * @throws SQLException
    */
   private void updateDb_060_To_061_DataUpdate(final Connection conn,
                                               final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 61;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {

         // data version is higher -> nothing to do
         return;
      }

      EntityManager em = null;

      try {

         em = TourDatabase.getInstance().getEntityManager();

         if (em != null) {

            final Query query = em.createQuery(UI.EMPTY_STRING

                  + "SELECT Equipment" + NL //                                               //$NON-NLS-1$
                  + " FROM " + Equipment.class.getSimpleName() + " AS Equipment" + NL //     //$NON-NLS-1$ //$NON-NLS-2$

                  // sort by name
                  + " ORDER BY Equipment.brand, Equipment.model" + NL //                     //$NON-NLS-1$
            );

            final Set<String> allEquipmentTypes = new HashSet<>();

            final List<?> allResults = query.getResultList();

            for (final Object result : allResults) {

               if (result instanceof final Equipment equipment) {

                  final Set<String> allPartTypes = new HashSet<>();

                  if (equipment.isCollate()) {

                     allEquipmentTypes.add(equipment.getCollateID());

                  } else {

                     // equipment is not collated but parts can be

                     // get all collated part types
                     for (final EquipmentPart part : equipment.getParts()) {
                        if (part.isCollate()) {
                           allPartTypes.add(part.getPartCollateID());
                        }
                     }

                     for (final String partType : allPartTypes) {

                        EquipmentManager.updateUntilDate_Parts_OneCollateID(equipment, partType, (short) -1);
                     }
                  }
               }
            }

            EquipmentManager.updateUntilDate_Equipment(allEquipmentTypes);
         }

      } finally {

         if (em != null) {
            em.close();
         }
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }
//
//   private int updateDb_061_To_062(final Connection conn, final SplashManager splashManager) throws SQLException {
//
//      final int newDbVersion = 62;
//
//      logDbUpdate_Start(newDbVersion);
//      updateMonitor(splashManager, newDbVersion);
//
//      // double check if column already exists
//      if (isColumnAvailable(conn, TABLE_EQUIPMENT, "initialValue") == false) { //$NON-NLS-1$
//
//         final Statement stmt = conn.createStatement();
//         {
//            // alter columns
//
//// SET_FORMATTING_OFF
//
//            SQL.addColumn_BigInt    (stmt, TABLE_EQUIPMENT,       "DatePurchased",     DEFAULT_0);       //$NON-NLS-1$
//            SQL.addColumn_Float     (stmt, TABLE_EQUIPMENT,       "initialValue",      DEFAULT_0);       //$NON-NLS-1$
//            SQL.addColumn_VarCar    (stmt, TABLE_EQUIPMENT,       "initialValueUnit",  DB_LENGTH_NAME);  //$NON-NLS-1$
//
//            SQL.addColumn_BigInt    (stmt, TABLE_EQUIPMENT_PART,  "DatePurchased",     DEFAULT_0);       //$NON-NLS-1$
//            SQL.addColumn_Float     (stmt, TABLE_EQUIPMENT_PART,  "initialValue",      DEFAULT_0);       //$NON-NLS-1$
//            SQL.addColumn_VarCar    (stmt, TABLE_EQUIPMENT_PART,  "initialValueUnit",  DB_LENGTH_NAME);  //$NON-NLS-1$
//
//// SET_FORMATTING_ON
//         }
//         stmt.close();
//      }
//
//      logDbUpdate_End(newDbVersion);
//
//      return newDbVersion;
//   }

   private int updateDb_062_To_063(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 63;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {

// SET_FORMATTING_OFF

         SQL.addColumn_Float(stmt, TABLE_TOUR_DATA, "nutrition_TotalCarbohydrates", DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
      }

      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private void updateMonitor(final SplashManager splashManager, final int newDbVersion) {

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
      }
   }

   private void updateVersionNumber_10_AfterDesignUpdate(final Connection conn, final int newVersion) throws SQLException {

      final String sql = "UPDATE " + TABLE_DB_VERSION_DESIGN + " SET VERSION=" + newVersion; //$NON-NLS-1$ //$NON-NLS-2$

      conn.createStatement().executeUpdate(sql);

      logDbUpdate("Database design version is set to: " + newVersion); //$NON-NLS-1$
   }

   private void updateVersionNumber_20_AfterDataUpdate(final Connection conn,
                                                       final int dbDataVersion,
                                                       final long startTime) throws SQLException {

      final String sql = "UPDATE " + TABLE_DB_VERSION_DATA + " SET VERSION=" + dbDataVersion; //$NON-NLS-1$ //$NON-NLS-2$

      conn.createStatement().executeUpdate(sql);

      logDbUpdate(createLog_DataUpdate(dbDataVersion, startTime));
   }

}
