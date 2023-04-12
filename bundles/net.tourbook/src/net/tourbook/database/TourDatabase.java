/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.skedgo.converter.TimezoneMapper;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.file.Path;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.SplashManager;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.data.TourPhoto;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.search.FTSearchManager;
import net.tourbook.tag.TagCollection;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;

import org.apache.derby.drda.NetworkServerControl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
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
   private static final int TOURBOOK_DB_VERSION = 50;

//   private static final int TOURBOOK_DB_VERSION = 50; // 23.x ??????

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

   private static final String NL                                         = UI.NEW_LINE;
   private static final String TIME_STAMP                                 = net.tourbook.common.UI.timeStamp();

   private static final int    MAX_TRIES_TO_PING_SERVER                   = 10;

   private static final String NUMBER_FORMAT_1F                           = "%.1f";                                                  //$NON-NLS-1$

   /**
    * Milliseconds how log the splash message is delayed before it is updated again.
    */
   private static final int    DELAY_SPLASH_LOGGING                       = 1000;

   /**
    * <b> !!! Table names are set to uppercase otherwise conn.getMetaData().getColumns() would not
    * work !!! </b>
    */
   private static final String TABLE_SCHEMA                               = "USER";                                                  //$NON-NLS-1$

   private static final String TABLE_DB_VERSION_DESIGN                    = "DBVERSION";                                             //$NON-NLS-1$
   private static final String TABLE_DB_VERSION_DATA                      = "DB_VERSION_DATA";                                       //$NON-NLS-1$

   private static final String TABLE_DEVICE_SENSOR                        = "DeviceSensor";                                          //$NON-NLS-1$
   public static final String  TABLE_DEVICE_SENSOR_VALUE                  = "DeviceSensorValue";                                     //$NON-NLS-1$
   private static final String TABLE_TOUR_BIKE                            = "TOURBIKE";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_COMPARED                        = "TOURCOMPARED";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_DATA                            = "TOURDATA";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_GEO_PARTS                       = "TourGeoParts";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_MARKER                          = "TOURMARKER";                                            //$NON-NLS-1$
   public static final String  TABLE_TOUR_PERSON                          = "TOURPERSON";                                            //$NON-NLS-1$
   private static final String TABLE_TOUR_PERSON_HRZONE                   = "TOURPERSONHRZONE";                                      //$NON-NLS-1$
   public static final String  TABLE_TOUR_PHOTO                           = "TOURPHOTO";                                             //$NON-NLS-1$
   public static final String  TABLE_TOUR_REFERENCE                       = "TOURREFERENCE";                                         //$NON-NLS-1$
   public static final String  TABLE_TOUR_TAG                             = "TOURTAG";                                               //$NON-NLS-1$
   public static final String  TABLE_TOUR_TAG_CATEGORY                    = "TOURTAGCATEGORY";                                       //$NON-NLS-1$
   private static final String TABLE_TOUR_TYPE                            = "TOURTYPE";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_WAYPOINT                        = "TOURWAYPOINT";                                          //$NON-NLS-1$

   public static final String  JOINTABLE__TOURDATA__TOURTAG               = TABLE_TOUR_DATA + "_" + TABLE_TOUR_TAG;                  //$NON-NLS-1$
   public static final String  JOINTABLE__TOURTAGCATEGORY_TOURTAG         = TABLE_TOUR_TAG_CATEGORY + "_" + TABLE_TOUR_TAG;          //$NON-NLS-1$
   public static final String  JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY = TABLE_TOUR_TAG_CATEGORY + "_" + TABLE_TOUR_TAG_CATEGORY; //$NON-NLS-1$

   /*
    * Tables which never have been used, they are dropped in db version 24
    */
   private static final String JOINTABLE__TOURDATA__TOURMARKER          = TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER;          //$NON-NLS-1$
   private static final String JOINTABLE__TOURDATA__TOURPHOTO           = TABLE_TOUR_DATA + "_" + TABLE_TOUR_PHOTO;           //$NON-NLS-1$
   private static final String JOINTABLE__TOURDATA__TOURREFERENCE       = TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE;       //$NON-NLS-1$
   private static final String JOINTABLE__TOURDATA__TOURWAYPOINT        = TABLE_TOUR_DATA + "_" + TABLE_TOUR_WAYPOINT;        //$NON-NLS-1$
   private static final String JOINTABLE__TOURPERSON__TOURPERSON_HRZONE = TABLE_TOUR_PERSON + "_" + TABLE_TOUR_PERSON_HRZONE; //$NON-NLS-1$

   // never used tables, they are needed to drop them
   private static final String TABLE_TOUR_CATEGORY                        = "TourCategory";                                         //$NON-NLS-1$
   private static final String TABLE_TOURCATEGORY__TOURDATA               = TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA;            //$NON-NLS-1$

   /**
    * Is <code>-1</code>, this is the id for a not saved entity
    */
   public static final int     ENTITY_IS_NOT_SAVED                        = -1;
   //
   private static final String ENTITY_ID_BIKE                             = "BikeID";                                               //$NON-NLS-1$
   private static final String ENTITY_ID_COMPARED                         = "ComparedID";                                           //$NON-NLS-1$
   private static final String ENTITY_ID_DEVICE_SENSOR                    = "SensorId";                                             //$NON-NLS-1$
   private static final String ENTITY_ID_DEVICE_SENSOR_VALUE              = "SensorValueId";                                        //$NON-NLS-1$
   private static final String ENTITY_ID_HR_ZONE                          = "HrZoneID";                                             //$NON-NLS-1$
   private static final String ENTITY_ID_MARKER                           = "MarkerID";                                             //$NON-NLS-1$
   private static final String ENTITY_ID_PERSON                           = "PersonID";                                             //$NON-NLS-1$
   private static final String ENTITY_ID_PHOTO                            = "PhotoID";                                              //$NON-NLS-1$
   private static final String ENTITY_ID_REF                              = "RefID";                                                //$NON-NLS-1$
   public static final String  ENTITY_ID_TAG                              = "TagID";                                                //$NON-NLS-1$
   public static final String  ENTITY_ID_TAG_CATEGORY                     = "TagCategoryID";                                        //$NON-NLS-1$
   private static final String ENTITY_ID_TOUR                             = "TourID";                                               //$NON-NLS-1$
   private static final String ENTITY_ID_TYPE                             = "TypeID";                                               //$NON-NLS-1$
   public static final String  ENTITY_ID_WAY_POINT                        = "WayPointID";                                           //$NON-NLS-1$
   //
   private static final String KEY_BIKE                                   = TABLE_TOUR_BIKE + "_" + ENTITY_ID_BIKE;                 //$NON-NLS-1$
   private static final String KEY_DEVICE_SENSOR                          = TABLE_DEVICE_SENSOR + "_" + ENTITY_ID_DEVICE_SENSOR;    //$NON-NLS-1$
   private static final String KEY_PERSON                                 = TABLE_TOUR_PERSON + "_" + ENTITY_ID_PERSON;             //$NON-NLS-1$
   public static final String  KEY_TAG                                    = TABLE_TOUR_TAG + "_" + ENTITY_ID_TAG;                   //$NON-NLS-1$
   private static final String KEY_TAG_CATEGORY                           = TABLE_TOUR_TAG_CATEGORY + "_" + ENTITY_ID_TAG_CATEGORY; //$NON-NLS-1$
   public static final String  KEY_TOUR                                   = TABLE_TOUR_DATA + "_" + ENTITY_ID_TOUR;                 //$NON-NLS-1$
   private static final String KEY_TYPE                                   = TABLE_TOUR_TYPE + "_" + ENTITY_ID_TYPE;                 //$NON-NLS-1$

   /**
    * Renaming existing fields in the tour database causes lots of troubles and additional work to
    * fix and test it for ALL possible cases -> It is not a good idea to rename fields
    */
   private static final String RENAMED__BIKER_WEIGHT__FROM                = "bikerWeight";                                          //$NON-NLS-1$
   private static final String RENAMED__BIKER_WEIGHT__INTO                = "BodyWeight";                                           //$NON-NLS-1$
   private static final String RENAMED__TOUR_DRIVING_TIME__FROM           = "tourDrivingTime";                                      //$NON-NLS-1$
   private static final String RENAMED__TOUR_DRIVING_TIME__INTO           = "TourComputedTime_Moving";                              //$NON-NLS-1$
   private static final String RENAMED__TOUR_RECORDING_TIME__FROM         = "tourRecordingTime";                                    //$NON-NLS-1$
   private static final String RENAMED__TOUR_RECORDING_TIME__INTO         = "TourDeviceTime_Elapsed";                               //$NON-NLS-1$
   private static final String RENAMED__TOUR_AVG_TEMPERATURE__FROM        = "avgTemperature";                                       //$NON-NLS-1$
   private static final String RENAMED__TOUR_AVG_TEMPERATURE__INTO        = "weather_Temperature_Average_Device";                   //$NON-NLS-1$
   private static final String RENAMED__TOUR_MAX_TEMPERATURE__FROM        = "weather_Temperature_Max";                              //$NON-NLS-1$
   private static final String RENAMED__TOUR_MAX_TEMPERATURE__INTO        = "weather_Temperature_Max_Device";                       //$NON-NLS-1$
   private static final String RENAMED__TOUR_MIN_TEMPERATURE__FROM        = "weather_Temperature_Min";                              //$NON-NLS-1$
   private static final String RENAMED__TOUR_MIN_TEMPERATURE__INTO        = "weather_Temperature_Min_Device";                       //$NON-NLS-1$
   private static final String RENAMED__TOUR_ISWEATHERDATAFROMAPI__FROM   = "isWeatherDataFromApi";                                 //$NON-NLS-1$
   private static final String RENAMED__TOUR_ISWEATHERDATAFROMAPI__INTO   = "isWeatherDataFromProvider";                            //$NON-NLS-1$
   private static final String RENAMED__TOUR_WEATHER_CLOUDS__FROM         = "weatherClouds";                                        //$NON-NLS-1$
   private static final String RENAMED__TOUR_WEATHER_CLOUDS__INTO         = "weather_Clouds";                                       //$NON-NLS-1$
   private static final String RENAMED__TOUR_WEATHER_WIND_DIRECTION__FROM = "weatherWindDir";                                       //$NON-NLS-1$
   private static final String RENAMED__TOUR_WEATHER_WIND_DIRECTION__INTO = "weather_Wind_Direction";                               //$NON-NLS-1$
   private static final String RENAMED__TOUR_WEATHER_WIND_SPEED__FROM     = "weatherWindSpd";                                       //$NON-NLS-1$
   private static final String RENAMED__TOUR_WEATHER_WIND_SPEED__INTO     = "weather_Wind_Speed";                                   //$NON-NLS-1$

   private static final String DEFAULT_0                                  = "0";                                                    //$NON-NLS-1$
   private static final String DEFAULT_1_0                                = "1.0";                                                  //$NON-NLS-1$
   private static final String DEFAULT_FALSE                              = "false";                                                //$NON-NLS-1$
   private static final String DEFAULT_IGNORED                            = "-1";                                                   //$NON-NLS-1$

   private static final String PERSISTENCE_UNIT_NAME                      = "tourdatabase";                                         //$NON-NLS-1$

   private static final String DERBY_DATABASE                             = "derby-database";                                       //$NON-NLS-1$
   private static final String DERBY_DB_TOURBOOK                          = "tourbook";                                             //$NON-NLS-1$
   private static String       DERBY_DRIVER_CLASS;
   private static String       DERBY_URL;
   private static final String DERBY_URL_COMMAND_CREATE_TRUE              = ";create=true";                                         //$NON-NLS-1$
   private static final String DERBY_URL_COMMAND_SHUTDOWN_TRUE            = ";shutdown=true";                                       //$NON-NLS-1$
   private static final String DERBY_URL_COMMAND_UPGRADE_TRUE             = ";upgrade=true";                                        //$NON-NLS-1$
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
   private static volatile HashMap<Long, TourTagCategory> _allTourTagCategories;

   /**
    * Key is category ID or <code>-1</code> for the root.
    */
   private static HashMap<Long, TagCollection>            _tagCollections = new HashMap<>();

   /**
    * Key is sensor ID
    */
   private static volatile Map<Long, DeviceSensor>        _allDbDeviceSensors_BySensorID;

   /**
    * Key is the serial number in UPPERCASE
    */
   private static volatile Map<String, DeviceSensor>      _allDbDeviceSensors_BySerialNo;

   /*
    * Cached distinct fields
    */
   private static ConcurrentSkipListSet<String>  _dbTourTitles;
   private static ConcurrentSkipListSet<String>  _dbTourStartPlace;
   private static ConcurrentSkipListSet<String>  _dbTourEndPlace;
   private static ConcurrentSkipListSet<String>  _dbTourMarkerNames;

   private static final IPreferenceStore         _prefStore          = TourbookPlugin.getPrefStore();

   private static final String                   _databasePath       = Platform.getInstanceLocation().getURL().getPath() + DERBY_DATABASE;

   private static NetworkServerControl           _server;

   private static volatile EntityManagerFactory  _emFactory;
   private static volatile ComboPooledDataSource _pooledDataSource;

   private static int                            _dbVersionOnStartup = -1;

   private static ThreadPoolExecutor             _dbUpdateExecutor;
   private static ArrayBlockingQueue<Long>       _dbUpdateQueue      = new ArrayBlockingQueue<>(Util.NUMBER_OF_PROCESSORS);

   static {

      // set storage location for the database
      System.setProperty("derby.system.home", _databasePath); //$NON-NLS-1$

// FOR DEBUGGING - START
      /*
       * Set derby debug properties, this is helpful when debugging, the log is written into
       * derby.log
       */
//    System.setProperty("derby.language.logStatementText", "true");
//    System.setProperty("derby.language.logQueryPlan", "true");

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
   //   private static final String   SQL_INT_MIN_VALUE;
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

   private static final String                   SYS_PROP__SILENT_DATABASE_UPDATE = "silentDatabaseUpdate";                                      //$NON-NLS-1$
   private static final boolean                  _isSilentDatabaseUpdate          = System.getProperty(SYS_PROP__SILENT_DATABASE_UPDATE) != null;

   private boolean                               _isDbInitialized;
   private boolean                               _isDbInDataUpdate;
   private boolean                               _isTableChecked;
   private boolean                               _isDataVersionChecked;
   private boolean                               _isDesignVersionChecked;

   private int                                   _dbDesignVersion_New;
   private int                                   _dbDesignVersion_Old;

   private final ListenerList<IPropertyListener> _propertyListeners               = new ListenerList<>(ListenerList.IDENTITY);

   private boolean                               _isSQLDesignUpdateError          = false;
   private boolean                               _isSQLDataUpdateError            = false;

   /**
    * Database version before a db design update is performed
    */
   private int                                   _dbVersion_BeforeDesignUpdate;

   /**
    * Database version after a db design update is performed but before optional data updates (data
    * updates)
    */
   private int                                   _dbVersion_AfterDesignUpdate;

   private boolean                               _isDerbyEmbedded;
   private boolean                               _isChecked_DbCreated;
   private boolean                               _isChecked_UpgradeDB_Before;
   private boolean                               _isChecked_UpgradeDB_After;

   /**
    * SQL utilities.
    */
   private static class SQL {

      private static void AddColumn_BigInt(final Statement stmt,
                                           final String table,
                                           final String columnName,
                                           final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " BIGINT DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Default value.
       * @throws SQLException
       */
      private static void AddColumn_Boolean(final Statement stmt,
                                            final String table,
                                            final String columnName,
                                            final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " BOOLEAN DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      private static void AddColumn_Double(final Statement stmt,
                                           final String table,
                                           final String columnName,
                                           final String defaultValue) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " DOUBLE DEFAULT " + defaultValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      private static void AddColumn_Float(final Statement stmt,
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
       * @throws SQLException
       */
      private static void AddColumn_Int(final Statement stmt,
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
       * @throws SQLException
       */
      private static void AddColumn_SmallInt(final Statement stmt,
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
       * @throws SQLException
       */
      private static void AddColumn_VarCar(final Statement stmt,
                                           final String table,
                                           final String columnName,
                                           final int columnWidth) throws SQLException {

         if (isColumnAvailable(stmt.getConnection(), table, columnName)) {

            // column already exist -> nothing to do

            return;
         }

         exec(stmt, "ALTER TABLE " + table + " ADD COLUMN " + columnName + " VARCHAR(" + columnWidth + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }

      private static void AlterColumn_VarChar_Width(final Statement stmt,
                                                    final String table,
                                                    final String field,
                                                    final int newWidth) throws SQLException {

         final String sql = UI.EMPTY_STRING
               + "ALTER TABLE " + table + NL //                         //$NON-NLS-1$
               + "   ALTER COLUMN " + field + NL //                     //$NON-NLS-1$
               + "   SET DATA TYPE   VARCHAR(" + newWidth + ")"; //     //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param defaultValue
       *           Default value.
       * @throws SQLException
       */
      private static void Cleanup_DropColumn(final Statement stmt,
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

      private static void Cleanup_DropConstraint(final Statement stmt,
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

      private static void Cleanup_DropTable(final Statement stmt, final String tableName) throws SQLException {

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
       * @return
       */
      private static String CreateField_EntityId(final String fieldName, final boolean isGenerateID) {

         String generateID = UI.EMPTY_STRING;

         if (isGenerateID) {
            generateID = "GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)"; //$NON-NLS-1$
         }

//       SUBJECT CHAR(64) NOT NULL CONSTRAINT OUT_TRAY_PK PRIMARY KEY,

         return "   " //                                                //$NON-NLS-1$

               + fieldName + " BIGINT NOT NULL " //                     //$NON-NLS-1$
               + generateID
               + " CONSTRAINT " + fieldName + "_pk PRIMARY KEY" //      //$NON-NLS-1$ //$NON-NLS-2$
               + "," + NL; //                                           //$NON-NLS-1$
      }

      /**
       * @param stmt
       * @param tableName
       * @param indexAndColumnName
       * @throws SQLException
       */
      private static void CreateIndex(final Statement stmt, final String tableName, final String indexAndColumnName) throws SQLException {

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
       * Combine tableName and columnName to the indexName
       *
       * @param stmt
       * @param tableName
       * @param columnName
       * @throws SQLException
       */
      private static void CreateIndex_Combined(final Statement stmt,
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
       * @throws SQLException
       */
      private static void RenameCol(final Statement stmt,
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

      _isDerbyEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);

      if (_isDerbyEmbedded) {

         // use embedded server

         DERBY_URL = "jdbc:derby:" + DERBY_DB_TOURBOOK; //$NON-NLS-1$
         DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver"; //$NON-NLS-1$

      } else {

         // use network server

         DERBY_URL = "jdbc:derby://localhost:1527/" + DERBY_DB_TOURBOOK; //$NON-NLS-1$
         DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.ClientDriver"; //$NON-NLS-1$
      }
   }

   /**
    * This error can occur when transient instances are not saved.
    *
    * <pre>
    *
    * !ENTRY net.tourbook.common 4 0 2015-05-08 16:10:55.578
    * !MESSAGE Tour cannot be saved in the database
    * !STACK 0
    * org.hibernate.TransientObjectException: object references an unsaved transient instance - save the transient instance before flushing: net.tourbook.data.TourData.tourType -> net.tourbook.data.TourType
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
   private static void checkUnsavedTransientInstances(final TourData tourData) {

      checkUnsavedTransientInstances_Tags(tourData);
      checkUnsavedTransientInstances_TourType(tourData);
      checkUnsavedTransientInstances_Sensors(tourData);
   }

   /**
    * @param tourData
    */
   private static void checkUnsavedTransientInstances_Sensors(final TourData tourData) {

      final Set<DeviceSensorValue> allTourData_SensorValues = tourData.getDeviceSensorValues();

      if (allTourData_SensorValues.isEmpty()) {
         return;
      }

      final ArrayList<DeviceSensor> allNotSavedSensors = new ArrayList<>();

      final HashMap<String, DeviceSensor> allDbSensors = new HashMap<>(getAllDeviceSensors_BySerialNo());

      // loop: all sensor values in the tour -> find sensors which are not yet saved
      for (final DeviceSensorValue tourData_SensorValue : allTourData_SensorValues) {

         final DeviceSensor tourData_Sensor = tourData_SensorValue.getDeviceSensor();

         final long sensorId = tourData_Sensor.getSensorId();

         if (sensorId != ENTITY_IS_NOT_SAVED) {

            // sensor is saved

            continue;
         }

         // sensor is not yet saved
         // 1. sensor can still be new
         // 2. sensor is already created but not updated in the not yet saved tour

         final DeviceSensor dbSensor = allDbSensors.get(tourData_Sensor.getSerialNumber().toUpperCase());

         if (dbSensor == null) {

            // sensor not available -> create a new sensor

            allNotSavedSensors.add(tourData_Sensor);
         }
      }

      boolean isNewSensorSaved = false;

      if (allNotSavedSensors.size() > 0) {

         // create new sensors

         synchronized (TRANSIENT_LOCK) {

            HashMap<String, DeviceSensor> allDbSensors_InLock = new HashMap<>(getAllDeviceSensors_BySerialNo());

            for (final DeviceSensor newSensor : allNotSavedSensors) {

               // check again, sensor list could be updated in another thread
               final DeviceSensor dbSensor = allDbSensors_InLock.get(newSensor.getSerialNumber().toUpperCase());

               if (dbSensor == null) {

                  // sensor is not yet in db -> create it

                  saveEntity(
                        newSensor,
                        ENTITY_IS_NOT_SAVED,
                        DeviceSensor.class);

                  isNewSensorSaved = true;
               }
            }

            if (isNewSensorSaved) {

               /*
                * Replace sensor in sensor values
                */

               // force to reload db sensors
               clearDeviceSensors();
               TourManager.getInstance().clearTourDataCache();

               allDbSensors_InLock = new HashMap<>(getAllDeviceSensors_BySerialNo());

               // loop: all sensor values in the tour -> find sensors which are not yet saved
               for (final DeviceSensorValue tourData_SensorValue : allTourData_SensorValues) {

                  final DeviceSensor tourData_Sensor = tourData_SensorValue.getDeviceSensor();

                  final String serialNumberKey = tourData_Sensor.getSerialNumber().toUpperCase();

                  final DeviceSensor deviceSensor = allDbSensors_InLock.get(serialNumberKey);

                  tourData_SensorValue.setDeviceSensor(deviceSensor);
               }
            }
         }
      }

   }

   /**
    * @param tourData
    */
   private static void checkUnsavedTransientInstances_Tags(final TourData tourData) {

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

            // tag not available -> create a new tag

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
    * @param tourData
    */
   private static void checkUnsavedTransientInstances_TourType(final TourData tourData) {

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

   /**
    * Removes all sensors which are loaded from the database so the next time they will be
    * reloaded.
    */
   private static synchronized void clearDeviceSensors() {

      if (_allDbDeviceSensors_BySerialNo != null) {
         _allDbDeviceSensors_BySerialNo.clear();
         _allDbDeviceSensors_BySerialNo = null;
      }

      if (_allDbDeviceSensors_BySensorID != null) {
         _allDbDeviceSensors_BySensorID.clear();
         _allDbDeviceSensors_BySensorID = null;
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

      if (_allTourTagCategories != null) {
         _allTourTagCategories.clear();
         _allTourTagCategories = null;
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

   private static void computeAnyValues_ForAllTours(final SplashManager splashManager) {

      final ArrayList<Long> allTourIds = getAllTourIds();

      // loop: all tours, compute computed fields and save the tour
      int tourCounter = 1;
      for (final Long tourId : allTourIds) {

         if (splashManager != null) {
            splashManager.setMessage(
                  NLS.bind(
                        Messages.Tour_Database_update_tour,
                        new Object[] { tourCounter++, allTourIds.size() }));
         }

         final TourData tourData = getTourFromDb(tourId);
         if (tourData != null) {

            tourData.computeComputedValues();
            saveTour(tourData, false);
         }
      }
   }

   /**
    * @param tourRunner
    *           {@link IComputeNoDataserieValues} interface to compute values for one tour
    * @param tourIds
    *           Tour ID's which should be computed, when <code>null</code>, ALL tours will be
    *           computed.
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
    * Disable runtime statistics by putting this stagement after the result set was read
    *
    * @param conn
    * @throws SQLException
    */
   public static void disableRuntimeStatistic(final Connection conn) throws SQLException {

      CallableStatement cs;

      cs = conn.prepareCall("VALUES SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS()"); //$NON-NLS-1$
      cs.execute();

      // log runtime statistics
      final ResultSet rs = cs.getResultSet();
      while (rs.next()) {
         System.out.println(rs.getString(1));
      }

      cs.close();

      cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(0)"); //$NON-NLS-1$
      cs.execute();
      cs.close();

      cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(0)"); //$NON-NLS-1$
      cs.execute();
      cs.close();
   }

   /**
    * Get runtime statistics by putting this stagement before the query is executed
    *
    * @param conn
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

      System.out.println(sql);

      stmt.execute(sql);
   }

   private static void exec(final Statement stmt, final String[] sqlStatements) throws SQLException {

      for (final String sql : sqlStatements) {
         exec(stmt, sql);
      }
   }

   private static void execUpdate(final Statement stmt, final String sql) throws SQLException {

      System.out.println(sql);

      stmt.executeUpdate(sql);
   }

   /**
    * @param tourTypeList
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
    * @return Returns a map with all {@link DeviceSensor} which are stored in the database, key is
    *         the serial number in UPPERCASE
    */
   public static Map<String, DeviceSensor> getAllDeviceSensors_BySerialNo() {

      if (_allDbDeviceSensors_BySerialNo != null) {
         return _allDbDeviceSensors_BySerialNo;
      }

      loadAllDeviceSensors();

      return _allDbDeviceSensors_BySerialNo;
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
         final SQLFilter appFilter = new SQLFilter(SQLFilter.FAST_APP_FILTER);

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
    * This method is synchronized to conform to FindBugs
    *
    * @return Returns all tour tags which are stored in the database, the hash key is the tag id
    */
   public static HashMap<Long, TourTagCategory> getAllTourTagCategories() {

      if (_allTourTagCategories != null) {
         return _allTourTagCategories;
      }

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allTourTagCategories != null) {
            return _allTourTagCategories;
         }

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT tourTagCategory" //$NON-NLS-1$
                  + " FROM " + TourTagCategory.class.getSimpleName() + " AS tourTagCategory"); //$NON-NLS-1$ //$NON-NLS-2$

            _allTourTagCategories = new HashMap<>();

            final List<?> resultList = emQuery.getResultList();
            for (final Object result : resultList) {

               if (result instanceof TourTagCategory) {
                  final TourTagCategory tourTagCategory = (TourTagCategory) result;
                  _allTourTagCategories.put(tourTagCategory.getCategoryId(), tourTagCategory);
               }
            }

            em.close();
         }
      }

      return _allTourTagCategories;
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

   public static String getDatabasePath() {
      return _databasePath;
   }

   /**
    * @return Database version on app startup before any db update is started
    */
   public static int getDbVersionOnStartup() {
      return _dbVersionOnStartup;
   }

   /**
    * Getting one row from the database sorted by alphabet and without any double entries.
    *
    * @author Stefan F.
    * @param sqlQuery
    *           must look like: "SELECT tourTitle FROM " + TourDatabase.TABLE_TOUR_DATA + " ORDER BY
    *           tourTitle"
    * @return places as string array.
    */
   private static ConcurrentSkipListSet<String> getDistinctValues(final String db, final String fieldname) {

      final ConcurrentSkipListSet<String> sortedValues = new ConcurrentSkipListSet<>((text1, text2) -> {
         {
            // sort without case
            return text1.compareToIgnoreCase(text2);
         }
      });

      /*
       * run in UI thread otherwise the busyindicator fails
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
                     sortedValues.add(dbValue);
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

      return sortedValues;
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
    * @param tagId
    * @return Returns the tag category notes
    *         <code>null</code>
    */
   public static String getTagCategoryNotes(final Long tagId) {

      if (tagId == null) {
         return UI.EMPTY_STRING;
      }

      final HashMap<Long, TourTagCategory> hashAllTagCategories = getAllTourTagCategories();
      final TourTagCategory tagCategory = hashAllTagCategories.get(tagId);

      if (tagCategory != null) {
         return tagCategory.getNotes();
      } else {
         try {
            throw new MyTourbookException("Tag category id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
         } catch (final MyTourbookException e) {
            e.printStackTrace();
         }
      }

      return UI.EMPTY_STRING;
   }

   /**
    * @param categoryId
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
    * @param tagIds
    * @return Returns the tag names separated with a comma or an empty string when tagIds are.
    *         <code>null</code>
    */
   public static String getTagNames(final List<Long> tagIds) {

      if (tagIds == null) {
         return UI.EMPTY_STRING;
      }

      final HashMap<Long, TourTag> hashTags = getAllTourTags();
      final ArrayList<String> tagNames = new ArrayList<>();

      // get tag name for each tag id
      for (final Long tagId : tagIds) {
         final TourTag tag = hashTags.get(tagId);

         if (tag != null) {
            tagNames.add(tag.getTagName());
         } else {
            try {
               throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (final MyTourbookException e) {
               e.printStackTrace();
            }
         }
      }

      return getTagNamesText(tagNames, false);
   }

   /**
    * @param tourTags
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

      return sb.toString();
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
    * @return Returns the tour data or <code>null</code> if the tour is not in the database
    */
   public static TourData getTourFromDb(final Long tourId) {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      final TourData tourData = em.find(TourData.class, tourId);

      em.close();

      return tourData;
   }

   /**
    * Get {@link TourType} from all available tour type by it's id.
    *
    * @param tourTypeId
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

         final ResultSet result = meta.getColumns(null, TABLE_SCHEMA, table, column.toUpperCase());

         while (result.next()) {
            return true;
         }

         /*
          * dump all columns
          */
//         final DatabaseMetaData meta = conn.getMetaData();
//         final ResultSet result = meta.getColumns(null, TABLE_SCHEMA, table, NULL);
//
//         while (result.next()) {
//            System.out.println("  "
//                  + result.getString("TABLE_SCHEM")
//                  + ", "
//                  + result.getString("TABLE_NAME")
//                  + ", "
//                  + result.getString("COLUMN_NAME")
//                  + ", "
//                  + result.getString("TYPE_NAME")
//                  + ", "
//                  + result.getInt("COLUMN_SIZE")
//                  + ", "
//                  + result.getString("NULLABLE"));
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

   private static boolean isIndexAvailable(final Connection conn, final String table, final String column) {

      try {

         final String requestedIndexColumnName = column.toUpperCase();

         final DatabaseMetaData meta = conn.getMetaData();
         final ResultSet result = meta.getIndexInfo(null, TABLE_SCHEMA, table, false, false);

         while (result.next()) {

            final String dbIndexColumnName = result.getString("COLUMN_NAME"); //$NON-NLS-1$

            if (requestedIndexColumnName.equals(dbIndexColumnName.toUpperCase())) {

               return true;
            }

//            System.out.println("Table Indicies\n");
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
////            TABLE_CAT String => table catalog (may be null)
////            TABLE_SCHEM String => table schema (may be null)
////            TABLE_NAME String => table name
////            TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
////            REMARKS String => explanatory comment on the table
////            TYPE_CAT String => the types catalog (may be null)
////            TYPE_SCHEM String => the types schema (may be null)
////            TYPE_NAME String => type name (may be null)
////            SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
////            REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return false;
   }

   private static void loadAllDeviceSensors() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allDbDeviceSensors_BySerialNo != null) {
            return;
         }

         final Map<Long, DeviceSensor> allDbDeviceSensors_BySensorID = new HashMap<>();
         final Map<String, DeviceSensor> allDbDeviceSensors_BySerialNo = new HashMap<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(UI.EMPTY_STRING

                  + "SELECT DeviceSensor" //                      //$NON-NLS-1$
                  + " FROM DeviceSensor AS DeviceSensor" //       //$NON-NLS-1$
            );

            final List<?> resultList = emQuery.getResultList();

            for (final Object result : resultList) {

               if (result instanceof DeviceSensor) {

                  final DeviceSensor sensor = (DeviceSensor) result;

                  allDbDeviceSensors_BySensorID.put(sensor.getSensorId(), sensor);
                  allDbDeviceSensors_BySerialNo.put(sensor.getSerialNumber().toUpperCase(), sensor);
               }
            }

            em.close();
         }

         _allDbDeviceSensors_BySensorID = allDbDeviceSensors_BySensorID;
         _allDbDeviceSensors_BySerialNo = allDbDeviceSensors_BySerialNo;
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

               if (result instanceof TourTag) {

                  final TourTag tourTag = (TourTag) result;

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

   /**
    * Persists an entity.
    * <p>
    * This method is <b>much faster</b> than using this
    * {@link #saveEntity(Object, long, Class, EntityManager)}
    * <p>
    *
    * @param entity
    * @param id
    * @param entityClass
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
         MessageDialog.openError(
               Display.getDefault().getActiveShell(),
               "Error", //$NON-NLS-1$
               "Error occurred when saving an entity"); //$NON-NLS-1$
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
         MessageDialog.openError(
               Display.getDefault().getActiveShell(),
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

         saveTour_PostSaveActions(persistedEntity);
      }

      return persistedEntity;
   }

   /**
    * This method {@link #saveTour_PostSaveActions_Concurrent_2_ForAllTours(long[])} <b>MUST</b> be
    * called <b>AFTER</b> all tours are saved
    *
    * @param tourData
    * @return
    */
   public static TourData saveTour_Concurrent(final TourData tourData, final boolean isUpdateModifiedDate) {

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

            StatusUtil.logError("Exception in tour " + TourManager.getTourDateTimeShort(tourData));//$NON-NLS-1$
            StatusUtil.showStatus(e);

         } finally {

            if (ts.isActive()) {
               ts.rollback();
            }

            em.close();
         }

         // do post save actions for only ONE tour
         saveTour_PostSaveActions_Concurrent_1_ForOneTour(persistedEntity);

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

   private static void saveTour_PostSaveActions(final TourData persistedEntity) {

      TourManager.getInstance().updateTourInCache(persistedEntity);

      updateCachedFields(persistedEntity);

      saveTour_GeoParts(persistedEntity);

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
    */
   private static void saveTour_PostSaveActions_Concurrent_1_ForOneTour(final TourData persistedEntity) {

      TourManager.getInstance().updateTourInCache(persistedEntity);

      updateCachedFields(persistedEntity);

      saveTour_GeoParts(persistedEntity);
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

      checkUnsavedTransientInstances(tourData);

      return true;
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
    * @return Returns <code>true</code> when the week is computed
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

         if (progress instanceof IProgressMonitor) {

            final IProgressMonitor monitor = (IProgressMonitor) progress;

            monitor.subTask(msg);

         } else if (progress instanceof SplashManager) {

            final SplashManager splashManager = (SplashManager) progress;

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
      sql = "CREATE INDEX TourType ON " + TABLE_TOUR_DATA + " (" + KEY_TYPE + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
    * @throws SQLException
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
    * @throws SQLException
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
    * @throws SQLException
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
    * @throws SQLException
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
    * @throws SQLException
    */
   private void createTable_DeviceSensor(final Statement stmt) throws SQLException {

      exec(stmt, "CREATE TABLE " + TABLE_DEVICE_SENSOR + "   (                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_DEVICE_SENSOR, true)

            // version 46 start

            + "   SensorType           VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   SensorName           VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   Description          VARCHAR(" + DeviceSensor.DB_LENGTH_DESCRIPTION + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   ManufacturerNumber   INTEGER,                                                 " + NL //$NON-NLS-1$
            + "   ManufacturerName     VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   ProductNumber        INTEGER,                                                 " + NL //$NON-NLS-1$
            + "   ProductName          VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   SerialNumber         VARCHAR(" + DeviceSensor.DB_LENGTH_NAME + ")             " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 46 end

            + ")" //                                                                          //$NON-NLS-1$
      );

      SQL.CreateIndex(stmt, TABLE_DEVICE_SENSOR, "SerialNumber"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_DEVICE_SENSOR_VALUE}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_DeviceSensorValues(final Statement stmt) throws SQLException {

      exec(stmt, "CREATE TABLE " + TABLE_DEVICE_SENSOR_VALUE + "   (                   " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_DEVICE_SENSOR_VALUE, true)

            + "   " + KEY_TOUR + "                 BIGINT,                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_DEVICE_SENSOR + "        BIGINT,                             " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 46 start

            + "   TourStartTime                    BIGINT DEFAULT 0,                   " + NL //$NON-NLS-1$
            + "   TourEndTime                      BIGINT DEFAULT 0,                   " + NL //$NON-NLS-1$

            + "   BatteryLevel_Start               SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
            + "   BatteryLevel_End                 SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$

            + "   BatteryStatus_Start              SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
            + "   BatteryStatus_End                SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$

            + "   BatteryVoltage_Start             FLOAT DEFAULT -1,                   " + NL //$NON-NLS-1$
            + "   BatteryVoltage_End               FLOAT DEFAULT -1                    " + NL //$NON-NLS-1$

            // version 46 end

            + ")" //                                                                          //$NON-NLS-1$
      );

      SQL.CreateIndex_Combined(stmt, TABLE_DEVICE_SENSOR_VALUE, "TourStartTime"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_BIKE}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourBike(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourBike
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_BIKE + "   (                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_BIKE, true)

            + "   Name           VARCHAR(" + TourBike.DB_LENGTH_NAME + "),             " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   Weight         FLOAT,                                                " + NL //$NON-NLS-1$ // kg
            + "   TypeId         INTEGER,                                              " + NL //$NON-NLS-1$
            + "   FrontTyreId    INTEGER,                                              " + NL //$NON-NLS-1$
            + "   RearTyreId     INTEGER                                               " + NL //$NON-NLS-1$

            + ")");//$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_COMPARED}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourCompared(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourCompared
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_COMPARED + "   (                         " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_COMPARED, true)

            + "   RefTourId          BIGINT,                                           " + NL //$NON-NLS-1$
            + "   TourId             BIGINT,                                           " + NL //$NON-NLS-1$

            + "   StartIndex         INTEGER NOT NULL,                                 " + NL //$NON-NLS-1$
            + "   EndIndex           INTEGER NOT NULL,                                 " + NL //$NON-NLS-1$
            + "   TourDate           DATE NOT NULL,                                    " + NL //$NON-NLS-1$
            + "   StartYear          INTEGER NOT NULL,                                 " + NL //$NON-NLS-1$
            + "   TourSpeed          FLOAT,                                            " + NL //$NON-NLS-1$

            // version 28 start

            + "   AvgPulse           FLOAT,                                            " + NL //$NON-NLS-1$

            // version 28 end ---------

            // version 40 start
//
// RENAMED FIELD - from "tourRecordingTime"
//
            + "   tourDeviceTime_Elapsed  INTEGER DEFAULT 0                            " + NL //$NON-NLS-1$

            // version 40 end ---------

            + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_DATA}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourData(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourData
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_DATA + "   (                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TOUR, false)

            + " StartYear           SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartMonth          SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartDay            SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartHour           SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartMinute         SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartWeek           SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartDistance       INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " Distance            INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " StartAltitude       SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " StartPulse          SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " DpTolerance         SMALLINT NOT NULL,                                 " + NL //$NON-NLS-1$
            + " TourDistance        INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$

            // replaced with BIGINT values in version 22
            //            + " tourRecordingTime    INTEGER NOT NULL,                   " + NL //$NON-NLS-1$
            //            + " tourDrivingTime      INTEGER NOT NULL,                   " + NL //$NON-NLS-1$

            + " tourAltUp           INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " tourAltDown         INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$

            + " deviceTourType      VARCHAR(" + TourData.DB_LENGTH_DEVICE_TOUR_TYPE + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " devicePluginId      VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_ID + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + " deviceTravelTime    BIGINT NOT NULL,                                   " + NL //$NON-NLS-1$
            + " deviceDistance      INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " deviceWheel         INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " deviceWeight        INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " deviceTotalUp       INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + " deviceTotalDown     INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$

            // version 3 start
            + " deviceMode          SMALLINT,                                          " + NL //$NON-NLS-1$
            + " deviceTimeInterval  SMALLINT,                                          " + NL //$NON-NLS-1$
            // version 3 end

            // version 4 start

            // from markus
            // replaced with FLOAT values in version 21
            //            + " maxAltitude            INTEGER,                          " + NL //$NON-NLS-1$
            //            + " maxPulse               INTEGER,                          " + NL //$NON-NLS-1$
            //            + " avgPulse               INTEGER,                          " + NL //$NON-NLS-1$
            //            + " avgCadence             INTEGER,                          " + NL //$NON-NLS-1$
            //            + " avgTemperature         INTEGER,                          " + NL //$NON-NLS-1$

            + " maxSpeed            FLOAT,                                                   " + NL //$NON-NLS-1$
            + " tourTitle           VARCHAR(" + TourData.DB_LENGTH_TOUR_TITLE + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // OLD + " tourDescription         VARCHAR(4096),                                   " + NL //// version <= 9
            + " tourDescription     VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + "),   " + NL //// modified in version 10 //$NON-NLS-1$ //$NON-NLS-2$

            + " tourStartPlace      VARCHAR(" + TourData.DB_LENGTH_TOUR_START_PLACE + "),    " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " tourEndPlace        VARCHAR(" + TourData.DB_LENGTH_TOUR_END_PLACE + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " calories            INTEGER,                                                 " + NL //$NON-NLS-1$
//
// RENAMED FIELD - bikerWeight
//
            + " bodyWeight          FLOAT,                                                   " + NL //$NON-NLS-1$
            + " " + KEY_BIKE + "    BIGINT,                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // from wolfgang
            + " devicePluginName    VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " deviceModeName      VARCHAR(" + TourData.DB_LENGTH_DEVICE_MODE_NAME + "),    " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 4 end

            + " " + KEY_TYPE + "    BIGINT,                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " " + KEY_PERSON + "  BIGINT,                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 6 start

            + " tourImportFilePath  VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 6 end

            // version 7 start

            + " mergeSourceTourId      BIGINT,                                               " + NL //$NON-NLS-1$
            + " mergeTargetTourId      BIGINT,                                               " + NL //$NON-NLS-1$
            + " mergedTourTimeOffset   INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " mergedAltitudeOffset   INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " startSecond            SMALLINT DEFAULT 0,                                   " + NL //$NON-NLS-1$

            // version 7 end

            // version 8 start

            + " weather_Wind_Direction INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " weather_Wind_Speed     INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " weather_Clouds         VARCHAR(" + TourData.DB_LENGTH_WEATHER_CLOUDS + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " restPulse              INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " isDistanceFromSensor   SMALLINT DEFAULT 0,                                   " + NL //$NON-NLS-1$

            // version 8 end

            // version 9 start

            + " startWeekYear          SMALLINT DEFAULT 1977,                                " + NL //$NON-NLS-1$

            // version 9 end

            // version 10 start

            // tourWayPoints is mapped in TourData

            // version 10 end----------

            // version 11 start
            //
            + " DateTimeCreated        BIGINT DEFAULT 0,                                     " + NL //$NON-NLS-1$
            + " DateTimeModified       BIGINT DEFAULT 0,                                     " + NL //$NON-NLS-1$

            // version 11 end ---------

            // version 12 start
            //
            + " IsPulseSensorPresent   INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " IsPowerSensorPresent   INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " DeviceAvgSpeed         FLOAT DEFAULT 0,                                      " + NL //$NON-NLS-1$
            + " DeviceFirmwareVersion  VARCHAR(" + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 12 end ---------

            // version 13 start

            + " TemperatureScale       INTEGER DEFAULT 1,                                    " + NL //$NON-NLS-1$
            + " Weather                VARCHAR(" + TourData.DB_LENGTH_WEATHER_V48 + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 13 end ---------

            // version 14 start

            + " ConconiDeflection      INTEGER DEFAULT 0,                                    " + NL //$NON-NLS-1$

            // version 14 end ---------

            // version 17 start

            + " hrZone0                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone1                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone2                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone3                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone4                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone5                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone6                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone7                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone8                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$
            + " hrZone9                INTEGER DEFAULT -1,                                   " + NL //$NON-NLS-1$

            // version 17 end ---------

            // version 18 start

            + " NumberOfHrZones         INTEGER DEFAULT 0,                                  " + NL //$NON-NLS-1$

            // version 18 end ---------

            // version 21 start

            + " maxAltitude            FLOAT DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " maxPulse               FLOAT DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " avgPulse               FLOAT DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " avgCadence             FLOAT DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " weather_Temperature_Average_Device  FLOAT DEFAULT 0,                                    " + NL //$NON-NLS-1$

            // version 21 end ---------

            // version 22 start  -  12.12

            + " TourStartTime          BIGINT DEFAULT 0,                                    " + NL //$NON-NLS-1$
            + " TourEndTime            BIGINT DEFAULT 0,                                    " + NL //$NON-NLS-1$

//
// RENAMED FIELDs - from TourRecordingTime, TourDrivingTime
//
            + " TourDeviceTime_Elapsed  BIGINT DEFAULT 0,                                 " + NL //$NON-NLS-1$
            + " TourComputedTime_Moving BIGINT DEFAULT 0,                                 " + NL //$NON-NLS-1$

            // version 22 end ---------

            // version 23 start  -  13.2

            + " numberOfTimeSlices     INTEGER DEFAULT 0,                                 " + NL //$NON-NLS-1$
            + " numberOfPhotos         INTEGER DEFAULT 0,                                 " + NL //$NON-NLS-1$
            + " photoTimeAdjustment    INTEGER DEFAULT 0,                                 " + NL //$NON-NLS-1$

            // version 23 end ---------

            // version 27 start  -  15.5

            + " frontShiftCount        INTEGER DEFAULT 0,                                 " + NL //$NON-NLS-1$
            + " rearShiftCount         INTEGER DEFAULT 0,                                 " + NL //$NON-NLS-1$

            // version 27 end ---------

            // version 29 start  -  15.12

            + " TourImportFileName     VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_NAME + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 29 end ---------

            // version 30 start  -  16.1

            + " power_Avg                             FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$
            + " power_Max                             INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$
            + " power_Normalized                      INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$
            + " power_FTP                             INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$

            + " power_TotalWork                       BIGINT DEFAULT 0,                   " + NL //$NON-NLS-1$
            + " power_TrainingStressScore             FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$
            + " power_IntensityFactor                 FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$

            + " power_PedalLeftRightBalance           INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$
            + " power_AvgLeftTorqueEffectiveness      FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$
            + " power_AvgRightTorqueEffectiveness     FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$
            + " power_AvgLeftPedalSmoothness          FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$
            + " power_AvgRightPedalSmoothness         FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$

            // version 30 end ---------

            // version 31 start  -  16.5

            + " CadenceMultiplier                     FLOAT DEFAULT 1.0,                  " + NL //$NON-NLS-1$
            + " IsStrideSensorPresent                 INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$

            // version 31 end ---------

            // version 32 start  -  >16.8 ???

            + " TimeZoneId            VARCHAR(" + TourData.DB_LENGTH_TIME_ZONE_ID + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 32 end ---------

            // version 35 start  -  18.7

            + " runDyn_StanceTime_Min                 SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_StanceTime_Max                 SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_StanceTime_Avg                 FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            + " runDyn_StanceTimeBalance_Min          SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_StanceTimeBalance_Max          SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_StanceTimeBalance_Avg          FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            + " runDyn_StepLength_Min                 SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_StepLength_Max                 SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_StepLength_Avg                 FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            + " runDyn_VerticalOscillation_Min        SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_VerticalOscillation_Max        SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_VerticalOscillation_Avg        FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            + " runDyn_VerticalRatio_Min              SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_VerticalRatio_Max              SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " runDyn_VerticalRatio_Avg              FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            // version 35 end ---------

            // version 36 start  -  18.12

            + " surfing_NumberOfEvents                SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " surfing_MinSpeed_StartStop            SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
            + " surfing_MinSpeed_Surfing              SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
            + " surfing_MinTimeDuration               SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$

            + " surfing_IsMinDistance                 BOOLEAN  DEFAULT FALSE,             " + NL //$NON-NLS-1$
            + " surfing_MinDistance                   SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$

            // version 36 end ---------

            // version 37 start  -  19.3

            + " hasGeoData                            BOOLEAN  DEFAULT FALSE,             " + NL //$NON-NLS-1$

            // version 37 end ---------

            // version 38 start  -  19.5

            + " training_TrainingEffect_Aerob         FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " training_TrainingEffect_Anaerob       FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " training_TrainingPerformance          FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            // version 38 end

            // version 39 start  -  19.7

            + " isWeatherDataFromProvider             BOOLEAN  DEFAULT FALSE,             " + NL //$NON-NLS-1$
            + " weather_Humidity                      SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " weather_Precipitation                 FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " weather_Pressure                      FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " weather_Temperature_Min_Device        FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " weather_Temperature_Max_Device        FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$
            + " weather_Temperature_WindChill         FLOAT    DEFAULT 0,                 " + NL //$NON-NLS-1$

            // version 39 end

            // version 40 start  -  19.10

            + " power_DataSource   VARCHAR(" + TourData.DB_LENGTH_POWER_DATA_SOURCE + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + " cadenceZone_SlowTime                  INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$
            + " cadenceZone_FastTime                  INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$
            + " cadenceZones_DelimiterValue           INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$
            + " avgAltitudeChange                     INTEGER DEFAULT 0,                  " + NL //$NON-NLS-1$

            // version 40 end

            // version 41 start  -  20.8

            + " maxPace                               FLOAT DEFAULT 0,                    " + NL //$NON-NLS-1$

            // version 41 end

            // version 42 start  -  20.11.1

            + " tourDeviceTime_Recorded               BIGINT,                            " + NL //$NON-NLS-1$
            + " tourDeviceTime_Paused                 BIGINT,                            " + NL //$NON-NLS-1$
            + " bodyFat                               FLOAT,                             " + NL //$NON-NLS-1$

            // version 42 end

            // version 45 start  -  21.9

            /*
             * The first default values were 0 but only when a new tour db was created
             * and not when the tour db was updated !!!
             */
            + " Battery_Percentage_Start              SMALLINT DEFAULT -1,               " + NL //$NON-NLS-1$
            + " Battery_Percentage_End                SMALLINT DEFAULT -1,               " + NL //$NON-NLS-1$

            // version 45 end

            // version 46 start  -  after 21.9
            // version 46 end

            // version 47 start  -  after 21.12

            + " weather_Temperature_Average           FLOAT DEFAULT 0,                   " + NL //$NON-NLS-1$
            + " weather_Temperature_Max               FLOAT DEFAULT 0,                   " + NL //$NON-NLS-1$
            + " weather_Temperature_Min               FLOAT DEFAULT 0,                   " + NL //$NON-NLS-1$
            + " weather_Snowfall                      FLOAT DEFAULT 0,                   " + NL //$NON-NLS-1$

            // version 47 end

            // version 50 start  -  after 23.3

            + " weather_AirQuality   VARCHAR(" + TourData.DB_LENGTH_WEATHER_AIRQUALITY + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 50 end

            // version 5 start
            /**
             * Disabled because when two blob object's are deserialized then the error occurs:
             * <p>
             * java.io.StreamCorruptedException: invalid stream header: 00ACED00
             * <p>
             * -> the gpsData are put into the serieData object
             */
            //   + "gpsData                           BLOB,                               " + NL //$NON-NLS-1$

            // version 5 end

            + " serieData                             BLOB                                " + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$

      createIndex_TourData_005(stmt);
      createIndex_TourData_022(stmt);
      createIndex_TourData_029(stmt);
      createIndex_TourData_033(stmt);
      createIndex_TourData_037(stmt);

      SQL.CreateIndex_Combined(stmt, TABLE_TOUR_DATA, "Battery_Percentage_Start"); //$NON-NLS-1$
   }

   /**
    * create table {@link #}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourGeoParts(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourGeoParts
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_GEO_PARTS + "   (                           " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   TourId         BIGINT   NOT NULL,                                       " + NL //$NON-NLS-1$
            + "   GeoPart        INTEGER  NOT NULL,                                       " + NL //$NON-NLS-1$

            + "   CONSTRAINT     PK_TourId_GeoPart PRIMARY KEY (TourId, GeoPart)          " + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$

      SQL.CreateIndex(stmt, TABLE_TOUR_GEO_PARTS, "TourId"); //$NON-NLS-1$
      SQL.CreateIndex(stmt, TABLE_TOUR_GEO_PARTS, "GeoPart"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_MARKER} for {@link TourMarker}.
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourMarker(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourMarker
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_MARKER + "   (                              " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_MARKER, true)

            + "   " + KEY_TOUR + "     BIGINT,                                            " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   time                 INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$

            // before version 20
            // + "   distance          INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   distance             INTEGER,                                           " + NL //$NON-NLS-1$

            // Version 20 - begin

            + "   distance20           FLOAT DEFAULT 0,                                   " + NL //$NON-NLS-1$

            // Version 20 - end

            // Version 22 - begin

            + "   IsMarkerVisible      INTEGER DEFAULT 1,                                 " + NL //$NON-NLS-1$

            // Version 22 - end

            // Version 24 - begin

            + "   description          VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),        " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlText              VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),             " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlAddress           VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 24 - end

            // Version 25 - begin

            + "   tourTime             BIGINT DEFAULT " + Long.MIN_VALUE + ",             " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // When DEFAULT value is NOT set, this exception occurs:
            //
            //java.lang.IllegalArgumentException: Can not set float field net.tourbook.data.TourMarker.altitude to null value
            //   at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:176)
            //   at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:180)
            //   at sun.reflect.UnsafeFloatFieldAccessorImpl.set(UnsafeFloatFieldAccessorImpl.java:92)
            //   at java.lang.reflect.Field.set(Field.java:753)
            //   at org.hibernate.property.DirectPropertyAccessor$DirectSetter.set(DirectPropertyAccessor.java:102)
            //
            + "   altitude             FLOAT DEFAULT " + SQL_FLOAT_MIN_VALUE + ",         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   latitude             DOUBLE DEFAULT " + SQL_DOUBLE_MIN_VALUE + ",       " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   longitude            DOUBLE DEFAULT " + SQL_DOUBLE_MIN_VALUE + ",       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 25 - end

            + "   serieIndex           INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   type                 INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   visualPosition       INTEGER NOT NULL,                                  " + NL //$NON-NLS-1$
            + "   label                VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   category             VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // Version 2
            + "   labelXOffset         INTEGER,                                           " + NL //$NON-NLS-1$
            + "   labelYOffset         INTEGER,                                           " + NL //$NON-NLS-1$
            + "   markerType           BIGINT                                             " + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_PERSON}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourPerson(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourPerson
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON + "   (                                       " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_PERSON, true)

            + "   lastName               VARCHAR(" + TourPerson.DB_LENGTH_LAST_NAME + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   firstName              VARCHAR(" + TourPerson.DB_LENGTH_FIRST_NAME + "),         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   weight                 FLOAT,                                                    " + NL //$NON-NLS-1$ // kg
            + "   height                 FLOAT,                                                    " + NL //$NON-NLS-1$ // m

            // version 15 start

            + "   BirthDay               BIGINT DEFAULT 0,                                         " + NL //$NON-NLS-1$

            // version 15 end ---------

            // version 16 start

            + "   Gender                 INTEGER DEFAULT 0,                                        " + NL //$NON-NLS-1$
            + "   RestPulse              INTEGER DEFAULT 0,                                        " + NL //$NON-NLS-1$
            + "   MaxPulse               INTEGER DEFAULT 0,                                        " + NL //$NON-NLS-1$
            + "   HrMaxFormula           INTEGER DEFAULT 0,                                        " + NL //$NON-NLS-1$

            // version 16 end ---------

            + "   rawDataPath            VARCHAR(" + TourPerson.DB_LENGTH_RAW_DATA_PATH + "),      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   deviceReaderId         VARCHAR(" + TourPerson.DB_LENGTH_DEVICE_READER_ID + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   " + KEY_BIKE + "       BIGINT                                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_PERSON_HRZONE} for {@link TourPersonHRZone}.
    * <p>
    * Table is available since db version 16
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourPersonHRZone(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourPersonHRZone
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON_HRZONE + "   (                          " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_HR_ZONE, true)

            + "   " + KEY_PERSON + "   BIGINT,                                               " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   zoneName          VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   nameShortcut      VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),   " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   description       VARCHAR(" + TourPersonHRZone.DB_LENGTH_DESCRIPTION + "), " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 18 start

            + "   ColorRed          INTEGER DEFAULT 0,                                       " + NL //$NON-NLS-1$
            + "   ColorGreen        INTEGER DEFAULT 0,                                       " + NL //$NON-NLS-1$
            + "   ColorBlue         INTEGER DEFAULT 0,                                       " + NL //$NON-NLS-1$

            // version 18 end ---------

            + "   zoneMinValue      INTEGER NOT NULL,                                        " + NL //$NON-NLS-1$
            + "   zoneMaxValue      INTEGER NOT NULL                                         " + NL //$NON-NLS-1$

            + ")"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_PHOTO}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourPhoto(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourPhoto
       */
      final int dbLengthFilePath = TourPhoto.DB_LENGTH_FILE_PATH;

      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PHOTO + "   (                            " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_PHOTO, true)

            + "   " + KEY_TOUR + "         BIGINT,                                     " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 23 start

            + "   imageFileName           VARCHAR(" + dbLengthFilePath + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFileExt            VARCHAR(" + dbLengthFilePath + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFilePath           VARCHAR(" + dbLengthFilePath + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFilePathName       VARCHAR(" + dbLengthFilePath + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageExifTime           BIGINT DEFAULT 0,                            " + NL //$NON-NLS-1$
            + "   imageFileLastModified   BIGINT DEFAULT 0,                            " + NL //$NON-NLS-1$

            + "   adjustedTime            BIGINT DEFAULT 0,                            " + NL //$NON-NLS-1$

            + "   ratingStars             INT DEFAULT 0,                               " + NL //$NON-NLS-1$
            + "   isGeoFromPhoto          INT DEFAULT 0,                               " + NL //$NON-NLS-1$
            + "   latitude                DOUBLE DEFAULT 0,                            " + NL //$NON-NLS-1$
            + "   longitude               DOUBLE DEFAULT 0                             " + NL //$NON-NLS-1$

            // version 23 end

            + ")" //                                                                          //$NON-NLS-1$
      );

      // Create index for {@link TourPhoto}, it will dramatically improve performance.
      SQL.CreateIndex(stmt, TABLE_TOUR_PHOTO, "ImageFilePathName"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_REFERENCE}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourReference(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourReference
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_REFERENCE + "   (                        " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_REF, true)

            + "   " + KEY_TOUR + "     BIGINT,                                         " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   startIndex           INTEGER NOT NULL,                               " + NL //$NON-NLS-1$
            + "   endIndex             INTEGER NOT NULL,                               " + NL //$NON-NLS-1$
            + "   label                VARCHAR(" + TourReference.DB_LENGTH_LABEL + ")  " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + ")"); //$NON-NLS-1$
   }

   /**
    * Create table {@link #TABLE_TOUR_TAG} which contains {@link TourTag} entities.
    *
    * @param stmt
    * @throws SQLException
    * @since DB version 5
    */
   private void createTable_TourTag(final Statement stmt) throws SQLException {

      /*
       * Create table: TOURTAG
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG + "   (                                 " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TAG, true)

            + "   isRoot               INTEGER,                                           " + NL //$NON-NLS-1$
            + "   expandType           INTEGER,                                           " + NL //$NON-NLS-1$
            + "   name                 VARCHAR(" + TourTag.DB_LENGTH_NAME + "),           " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 start

            + "   notes                VARCHAR(" + TourTag.DB_LENGTH_NOTES + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 end ---------

            // version 49 start

            + "   imageFilePath        VARCHAR(" + TourTag.DB_LENGTH_FILE_PATH + ")       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 49 end ---------

            + ")"); //$NON-NLS-1$

      /**
       * Create table: TOURDATA_TOURTAG
       */
      exec(stmt,

            "CREATE TABLE " + JOINTABLE__TOURDATA__TOURTAG + "   (                        " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + "   " + KEY_TAG + "      BIGINT NOT NULL,                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + KEY_TOUR + "     BIGINT NOT NULL                              " + NL //$NON-NLS-1$ //$NON-NLS-2$

                  + ")"); //$NON-NLS-1$

      // Add Constraint
      final String fkName = "fk_" + JOINTABLE__TOURDATA__TOURTAG + "_" + KEY_TOUR; //         //$NON-NLS-1$ //$NON-NLS-2$

      exec(stmt,

            "ALTER TABLE " + JOINTABLE__TOURDATA__TOURTAG + "                             " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkName + "                                     " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TOUR + ")                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_DATA + " (" + ENTITY_ID_TOUR + ")       "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      // Create index TOURTAG_TAGID
      SQL.CreateIndex(stmt, JOINTABLE__TOURDATA__TOURTAG, KEY_TAG);
   }

   /**
    * Create table {@link #TABLE_TOUR_TAG_CATEGORY} for which contains {@link TourTagCategory}
    * entities.
    *
    * @param stmt
    * @throws SQLException
    * @since DB version 5
    */
   private void createTable_TourTag_Category(final Statement stmt) throws SQLException {

      /**
       * Create table: TOURTAGCATEGORY
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG_CATEGORY + "   (                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TAG_CATEGORY, true)

            + "   isRoot               INTEGER,                                                       " + NL //$NON-NLS-1$
            + "   name                 VARCHAR(" + TourTag.DB_LENGTH_NAME + "),                       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 start

            + "   notes                VARCHAR(" + TourTag.DB_LENGTH_NOTES + ")                       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 end ---------

            + ")" //$NON-NLS-1$
      );

      /**
       * Create table: TOURTAGCATEGORY_TOURTAG
       */
      final String jtabTag = JOINTABLE__TOURTAGCATEGORY_TOURTAG;

      exec(stmt, "CREATE TABLE " + jtabTag + "   (                                                    " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   " + KEY_TAG + "               BIGINT NOT NULL,                                      " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_TAG_CATEGORY + "      BIGINT NOT NULL                                       " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")"); //$NON-NLS-1$

      // add constraints
      final String fkTag = "fk_" + jtabTag + "_" + KEY_TAG; //                                           //$NON-NLS-1$ //$NON-NLS-2$
      final String fkCat = "fk_" + jtabTag + "_" + TABLE_TOUR_TAG_CATEGORY + "_" + KEY_TAG_CATEGORY; //  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      exec(
            stmt,
            "ALTER TABLE " + jtabTag + "                                                              " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkTag + "                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG + ")                                                 " + NL //$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG + " (" + ENTITY_ID_TAG + ")                     " + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

      exec(stmt, "CREATE TABLE " + jtabCategory + "   (                                               " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   " + KEY_TAG_CATEGORY + "1   BIGINT NOT NULL,                                        " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_TAG_CATEGORY + "2   BIGINT NOT NULL                                         " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + ")"); //$NON-NLS-1$

      // add constraints
      final String fk1 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "1"; //                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final String fk2 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "2"; //                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
    * @throws SQLException
    */
   private void createTable_TourType(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourType
       */

      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TYPE + "   (                                      " + NL //$NON-NLS-1$ //$NON-NLS-2$

      //

            + SQL.CreateField_EntityId(ENTITY_ID_TYPE, true)

            + "   name                 VARCHAR(" + TourType.DB_LENGTH_NAME + "),                " + NL //$NON-NLS-1$ //$NON-NLS-2$

// renamed/converted rgb fields into one field with version 44
//
//          + "   colorBrightRed       SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//          + "   colorBrightGreen     SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//          + "   colorBrightBlue      SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//
//          + "   colorDarkRed         SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//          + "   colorDarkGreen       SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//          + "   colorDarkBlue        SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//
//          + "   colorLineRed         SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//          + "   colorLineGreen       SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//          + "   colorLineBlue        SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//
//          // version 19 start
//
//          + "   colorTextRed         SMALLINT DEFAULT 0,                                      " + NL //$NON-NLS-1$
//          + "   colorTextGreen       SMALLINT DEFAULT 0,                                      " + NL //$NON-NLS-1$
//          + "   colorTextBlue        SMALLINT DEFAULT 0                                       " + NL //$NON-NLS-1$
//
//          // version 19 end ---------

            // version 44 start

            + "   Color_Gradient_Bright    INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
            + "   Color_Gradient_Dark      INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$

            + "   Color_Line_LightTheme    INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
            + "   Color_Line_DarkTheme     INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$

            + "   Color_Text_LightTheme    INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
            + "   Color_Text_DarkTheme     INTEGER DEFAULT 0                                    " + NL //$NON-NLS-1$

            // version 44 end ---------

            + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_TOUR_WAYPOINT} *
    * <p>
    * since db version 10
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_TourWayPoint(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE TourWayPoint
       */
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_WAYPOINT + "   (                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_WAY_POINT, true)

            + "   " + KEY_TOUR + "     BIGINT,                                                  " + NL //$NON-NLS-1$ //$NON-NLS-2$

            + "   latitude             DOUBLE NOT NULL,                                         " + NL //$NON-NLS-1$
            + "   longitude            DOUBLE NOT NULL,                                         " + NL //$NON-NLS-1$
            + "   time                 BIGINT,                                                  " + NL //$NON-NLS-1$
            + "   altitude             FLOAT,                                                   " + NL //$NON-NLS-1$
            + "   name                 VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),            " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   description          VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),     " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   comment              VARCHAR(" + TourWayPoint.DB_LENGTH_COMMENT + "),         " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   symbol               VARCHAR(" + TourWayPoint.DB_LENGTH_SYMBOL + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   category             VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),        " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 28 start - create common fields with TourMarker

            + "   urlText              VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),          " + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlAddress           VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + ")        " + NL //$NON-NLS-1$ //$NON-NLS-2$

            // version 28 end ---------

            + ")"); //$NON-NLS-1$
   }

   private String createUIServerStateMessage(final int stateCounter) {

      final StringBuilder sb = new StringBuilder();

      for (int stateIndex = 1; stateIndex <= MAX_TRIES_TO_PING_SERVER + 1; stateIndex++) {
         sb.append(stateIndex <= stateCounter ? ':' : '.');
      }

      return NLS.bind(Messages.Database_Monitor_db_service_task, sb.toString());
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
         return getConnection_Pooled();
      } else {
         return null;
      }
   }

   private Connection getConnection_Pooled() throws SQLException {

      if (_pooledDataSource == null) {

         synchronized (DB_LOCK) {

            // check again
            if (_pooledDataSource == null) {

               try {
                  _pooledDataSource = new ComboPooledDataSource();

                  //loads the jdbc driver
                  _pooledDataSource.setDriverClass(DERBY_DRIVER_CLASS);
                  _pooledDataSource.setJdbcUrl(DERBY_URL);
                  _pooledDataSource.setUser(TABLE_SCHEMA);
                  _pooledDataSource.setPassword(TABLE_SCHEMA);

                  _pooledDataSource.setMaxPoolSize(100);
                  _pooledDataSource.setMaxStatements(100);
                  _pooledDataSource.setMaxStatementsPerConnection(20);

               } catch (final PropertyVetoException e) {
                  StatusUtil.log(e);
               }
            }
         }
      }

      Connection conn = null;
      try {
         conn = _pooledDataSource.getConnection();
      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return conn;
   }

   /**
    * @return Returns a connection to the derby database but do not create it.
    *         <p>
    *         <b> The pooled connection is not used because the database could be shutdown when it
    *         needs to be upgraded. </b>
    * @throws SQLException
    */
   private Connection getConnection_Simple() throws SQLException {

      final String dbUrl = DERBY_URL;

      logDerbyCommand(dbUrl);

      return DriverManager.getConnection(dbUrl, TABLE_SCHEMA, TABLE_SCHEMA);
   }

   private int getDbVersion(final Connection conn, final String table) {

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

   private String getRenamedField_TourRecordingTime(final Connection conn) {

      return isColumnAvailable(conn, TABLE_TOUR_DATA, RENAMED__TOUR_RECORDING_TIME__FROM)
            ? RENAMED__TOUR_RECORDING_TIME__FROM
            : RENAMED__TOUR_RECORDING_TIME__INTO;
   }

   private void logDbUpdate(final String info) {

      System.out.println(TIME_STAMP + "[DB Update] " + info); //$NON-NLS-1$
   }

   private void logDbUpdate_End(final int dbVersion) {

      System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, dbVersion));
      System.out.println();
   }

   private void logDbUpdate_Start(final int dbVersion) {

      System.out.println();
      System.out.println(NLS.bind(Messages.Tour_Database_Update, dbVersion));
   }

   private void logDerbyCommand(final String dbUrl) {

      System.out.println(TIME_STAMP + "[Derby command executed] " + dbUrl); //$NON-NLS-1$
   }

   private void logDerbyInfo(final String info) {

      System.out.println(TIME_STAMP + "[Derby] " + info); //$NON-NLS-1$
   }

   private void modifyColumn_Type(final String table,
                                  final String fieldName,
                                  final String newFieldType,
                                  final Statement stmt,
                                  final SplashManager splashManager,
                                  final int no,
                                  final int max) throws SQLException {

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_ModifyColumn, new Object[] { no, max }));
      }

      //   "ALTER TABLE t ADD COLUMN c1_newtype NEWTYPE"
      //   "UPDATE t SET c1_newtype = c1"
      //   "ALTER TABLE t DROP COLUMN c1"
      //   "RENAME COLUMN t.c1_newtype TO c1"

      String sql;
      final String tempFieldName = fieldName + "_temp";//$NON-NLS-1$

      sql = "ALTER TABLE " + table + " ADD COLUMN " + tempFieldName + " " + newFieldType; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      exec(stmt, sql);

      sql = "UPDATE " + table + " SET " + tempFieldName + " = " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      execUpdate(stmt, sql);

      sql = "ALTER TABLE " + table + " DROP COLUMN " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);

      sql = "RENAME COLUMN " + table + UI.SYMBOL_DOT + tempFieldName + " TO " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$
      exec(stmt, sql);

      System.out.println();
   }

   public void removePropertyListener(final IPropertyListener listener) {
      _propertyListeners.remove(listener);
   }

   private void showTourSaveError(final TourData tourData) {

      MessageDialog.openError(
            Display.getDefault().getActiveShell(),
            "Error", //$NON-NLS-1$
            String.format("Error occurred when saving tour '%s'", TourManager.getTourTitleDetailed(tourData))); //$NON-NLS-1$
   }

   /**
    * Shutdown database server
    */
   private void shutdownDatabaseServer() {

      Connection conn = null;

      try {

         // shutdown database that all connections are closed, THIS WILL ALWAYS CREATE AN EXCEPTION

         final String dbUrl_ShutDown = DERBY_URL + DERBY_URL_COMMAND_SHUTDOWN_TRUE;

         logDerbyCommand(dbUrl_ShutDown);

         conn = DriverManager.getConnection(dbUrl_ShutDown, TABLE_SCHEMA, TABLE_SCHEMA);

      } catch (final SQLException e) {

         logDerbyInfo("Derby server is shutdown"); //$NON-NLS-1$

// DO NOT SHOW THIS EXCEPTION, IT IS ALWAYS THROWN AND IS IRRITATING
//
//       final String sqlExceptionText = Util.getSQLExceptionText(e);
//
//       // log also the stacktrace
//       StatusUtil.log(sqlExceptionText + Util.getStackTrace(e));

      } finally {
         Util.closeSql(conn);
      }
   }

   /**
    * Is checking that the tour sql server is up and running and the tour database design and data
    * are updated to the current {@link #TOURBOOK_DB_VERSION}
    *
    * @return <code>true</code> when the tour sql server can be used.
    */
   private boolean sqlStartup_10_IsSqlServerUpAndRunning() {

      if (_isDbInitialized || _isDbInDataUpdate) {
         return true;
      }

      // check if the derby driver can be loaded
      try {

         Class.forName(DERBY_DRIVER_CLASS);

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

               sqlStartup_20_CheckServer(splashManager);
               sqlStartup_30_Check_DbIsCreated();

            } catch (final Throwable e) {

               StatusUtil.log(e);
               return;
            }

            sqlStartup_40_CheckTable(splashManager);

            if (sqlStartup_50_IsDesignVersionValid(splashManager) == false) {
               return;
            }

            sqlStartup_UpgradedDb_2_AfterDbDesignUpdate(splashManager);

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

   /**
    * Check if the server is available
    *
    * @param splashManager
    * @throws Throwable
    * @throws MyTourbookException
    */
   private void sqlStartup_20_CheckServer(final SplashManager splashManager) throws Throwable {

      if (_isDerbyEmbedded) {
         return;
      }

      // when the server is started, nothing is to do here
      if (_server != null) {
         return;
      }

      try {

         sqlStartup_22_CheckServer_CreateRunnable(splashManager).run(new NullProgressMonitor());

      } catch (final InvocationTargetException exception) {

         StatusUtil.log(exception);

         MessageDialog.openError(
               Display.getDefault().getActiveShell(),
               Messages.Tour_Database_CannotConnectToDerbyServer_Title,
               NLS.bind(Messages.Tour_Database_CannotConnectToDerbyServer_Message,
                     exception.getTargetException().getMessage()));

         PlatformUI.getWorkbench().close();

         throw exception.getTargetException();

      } catch (final InterruptedException e) {
         StatusUtil.log(e);
      }
   }

   /**
    * Checks if the database server is running, if not it will start the server. startServerJob has
    * a job when the server is not yet started
    *
    * @param splashManager
    */
   private IRunnableWithProgress sqlStartup_22_CheckServer_CreateRunnable(final SplashManager splashManager) {

      // create runnable for stating the derby server

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            if (splashManager != null) {
               splashManager.setMessage(createUIServerStateMessage(0));
            }

            try {
               _server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527); //$NON-NLS-1$
            } catch (final Exception e) {
               StatusUtil.log(e);
            }

            try {

               /*
                * check if another derby server is already running (this can happen during
                * development)
                */

               logDerbyInfo("Checking if derby server is already running before server.start"); //$NON-NLS-1$

               _server.ping();

            } catch (final Exception e) {

               try {

                  logDerbyInfo("Starting derby server");//$NON-NLS-1$

                  _server.start(null);

               } catch (final Exception e2) {
                  StatusUtil.log(e2);
               }

               logDerbyInfo("Checking if derby server is running after server.start");//$NON-NLS-1$

               int pingCounter = 1;
               final int threadSleepTime = 100;

               // wait until the server is started
               while (true) {

                  try {

                     if (splashManager != null) {
                        splashManager.setMessage(createUIServerStateMessage(pingCounter));
                     }
                     _server.ping();

                     logDerbyInfo("Derby server has started");//$NON-NLS-1$

                     break;

                  } catch (final Exception e1) {

                     if (pingCounter > MAX_TRIES_TO_PING_SERVER) {

                        StatusUtil.log("Cannot connect to derby server", e1);//$NON-NLS-1$

                        throw new InvocationTargetException(e1);
                     }

                     logDerbyInfo(NLS.bind(
                           "...waiting ({0} ms) for derby server startup: {1}", //$NON-NLS-1$
                           threadSleepTime,
                           pingCounter++));

                     try {
                        Thread.sleep(threadSleepTime);
                     } catch (final InterruptedException e2) {
                        StatusUtil.log(e2);
                     }
                  }
               }

               // make the first connection, this takes longer as the subsequent ones
               try {

                  if (splashManager != null) {
                     splashManager.setMessage(Messages.Database_Monitor_SetupPooledConnection);
                  }

                  final Connection connection = getConnection_Simple();
                  connection.close();

                  // log database path
                  logDerbyInfo("Database path: " + _databasePath); //$NON-NLS-1$

               } catch (final SQLException e1) {
                  UI.showSQLException(e1);
               }
            }
         }
      };

      return runnable;
   }

   private void sqlStartup_30_Check_DbIsCreated() {

      if (_isChecked_DbCreated) {
         return;
      }

      Connection conn = null;

      // ensure driver is loaded
      try {
         Class.forName(DERBY_DRIVER_CLASS);
      } catch (final ClassNotFoundException e) {
         StatusUtil.showStatus(e);
      }

      /*
       * Get a connection, this also creates the database when not yet available. The embedded
       * driver displays a warning when database already exist.
       */
      try {

         final String dbUrl = DERBY_URL + DERBY_URL_COMMAND_CREATE_TRUE;

         logDerbyCommand(dbUrl);

         conn = DriverManager.getConnection(dbUrl, TABLE_SCHEMA, TABLE_SCHEMA);

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } finally {

         Util.closeSql(conn);

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

      try (Connection conn = getConnection_Simple()) {

         /*
          * Check if the tourdata table exists
          */
         final DatabaseMetaData metaData = conn.getMetaData();
         final ResultSet tables = metaData.getTables(null, null, null, null);
         while (tables.next()) {
            if (tables.getString(3).equalsIgnoreCase(TABLE_TOUR_DATA)) {

               // table exists

               _isTableChecked = true;

               return;
            }
         }

         if (splashManager != null) {
            splashManager.setMessage(Messages.Database_Monitor_CreateDatabase);
         }

         try (Statement stmt = conn.createStatement()) {

            createTable_TourData(stmt);

            createTable_TourPerson(stmt);
            createTable_TourPersonHRZone(stmt);
            createTable_TourType(stmt);
            createTable_TourMarker(stmt);
            createTable_TourPhoto(stmt);
            createTable_TourReference(stmt);
            createTable_TourCompared(stmt);
            createTable_TourBike(stmt);
            createTable_TourGeoParts(stmt);
            createTable_DeviceSensor(stmt);
            createTable_DeviceSensorValues(stmt);

            createTable_DbVersion_Design(stmt);
            createTable_DbVersion_Data(stmt, TOURBOOK_DB_VERSION);

            createTable_TourTag(stmt);
            createTable_TourTag_Category(stmt);

            createTable_TourWayPoint(stmt);

         } catch (final SQLException e) {
            UI.showSQLException(e);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   /**
    * @param splashManager
    *           Progress monitor or <code>null</code> when the monitor is not available
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

      try (Connection conn = getConnection_Simple()) {

         // check if the database contains the correct version

         _dbVersion_BeforeDesignUpdate = getDbVersion(conn, TABLE_DB_VERSION_DESIGN);
         _dbVersion_AfterDesignUpdate = _dbVersion_BeforeDesignUpdate;

         if (_dbVersionOnStartup == -1) {

            // keep db version from app startup, _dbVersionBeforeUpdate is updated twice !!!
            _dbVersionOnStartup = _dbVersion_BeforeDesignUpdate;
         }

         logDbUpdate("Current database DESIGN version: " + _dbVersion_BeforeDesignUpdate); //$NON-NLS-1$

         if (_dbVersion_BeforeDesignUpdate < TOURBOOK_DB_VERSION) {

            sqlStartup_UpgradedDb_1_BeforeDbDesignUpdate(_dbVersion_BeforeDesignUpdate, splashManager);

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

      final Map<String, Object> configOverrides = new HashMap<>();

      configOverrides.put("hibernate.connection.url", DERBY_URL); //$NON-NLS-1$
      configOverrides.put("hibernate.connection.driver_class", DERBY_DRIVER_CLASS); //$NON-NLS-1$

      splashManager.setMessage(Messages.Database_Monitor_persistent_service_task);

      _emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, configOverrides);
   }

   /**
    * Is checking the tour database data is updated to the current {@link #TOURBOOK_DB_VERSION}
    *
    * @param splashManager
    *           Progress monitor or <code>null</code> when the monitor is not available
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

      try (Connection conn = getConnection_Simple()) {

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

   /**
    * Check BEFORE the data structure is modified otherwise this can fail when a new feature is use,
    * e.g. BOOLEAN
    *
    * @param dbVersionBeforeUpdate
    * @param splashManager
    * @return
    */
   private void sqlStartup_UpgradedDb_1_BeforeDbDesignUpdate(final int dbVersionBeforeUpdate, final SplashManager splashManager) {

      if (_isChecked_UpgradeDB_Before) {
         return;
      }

      boolean isUpgradeNeeded = false;

      if (dbVersionBeforeUpdate < 36) {

         // db version 36: update to derby 10.14.2 to use BOOLEAN datatype

         isUpgradeNeeded = true;
      }

      if (isUpgradeNeeded == false) {

         _isChecked_UpgradeDB_Before = true;

         return;
      }

      logDbUpdate(String.format("DB upgrade BEFORE db design update is needed %d", _dbVersion_BeforeDesignUpdate)); //$NON-NLS-1$

      shutdownDatabaseServer();

      /*
       * Upgrade database with ";upgrade=true" in the derby url
       */
      try {

         sqlStartup_UpgradeDerbyDatabase(splashManager);

         _isChecked_UpgradeDB_Before = true;

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   private void sqlStartup_UpgradedDb_2_AfterDbDesignUpdate(final SplashManager splashManager) {

      if (_isChecked_UpgradeDB_After) {
         return;
      }

      boolean isUpgradeNeeded = false;

      if (_dbVersion_BeforeDesignUpdate < 26 && _dbVersion_AfterDesignUpdate >= 26) {

         // db version 26: update to derby 10.11.1.1 to implement text search with lucene

         isUpgradeNeeded = true;
      }

      if (isUpgradeNeeded == false) {

         _isChecked_UpgradeDB_After = true;

         return;
      }

      // upgrade is needed

      logDbUpdate(String.format("DB upgrade AFTER db design update is needed %d -> %d", //$NON-NLS-1$
            _dbVersion_BeforeDesignUpdate,
            _dbVersion_AfterDesignUpdate));

      shutdownDatabaseServer();

      /*
       * Upgrade database
       */
      try {

         sqlStartup_UpgradeDerbyDatabase(splashManager);

         _isChecked_UpgradeDB_After = true;

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   /**
    * Upgrade Derby Database, this is necessary when a new Derby version has a new feature, e.g.
    * BOOLEAN datatype or Lucene fulltext index is used
    *
    * @param splashManager
    * @return
    * @throws SQLException
    */
   private void sqlStartup_UpgradeDerbyDatabase(final SplashManager splashManager) throws SQLException {

      final String dbUrl_Upgrade = DERBY_URL + DERBY_URL_COMMAND_UPGRADE_TRUE;

      logDerbyCommand(dbUrl_Upgrade);

      splashManager.setMessage(Messages.Database_Monitor_UpgradeDatabase);

      try (Connection conn = DriverManager.getConnection(dbUrl_Upgrade, TABLE_SCHEMA, TABLE_SCHEMA)) {

      }
   }

   /**
    * This method must be updated when the database version is updated
    */
   private boolean updateDb__1_Design(int currentDbVersion, final SplashManager splashManager) {

      if (!_isSilentDatabaseUpdate) {
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
                     _databasePath });

         if ((new MessageDialog(
               splashManager.getShell(),
               Messages.Tour_Database_Dialog_ConfirmUpdate_Title,
               null,
               dialogMessage,
               MessageDialog.QUESTION,
               buttons,
               1).open()) != Window.OK) {

            // the user will not update -> close application
            PlatformUI.getWorkbench().close();

            return false;
         }
      }

      /*
       * Do an additional check because version 20 is restructuring the data series
       */
      if (currentDbVersion < 20) {

// After almost 10 years this messagebox is not necessary anymore
//
//       if ((new MessageDialog(
//             Display.getDefault().getActiveShell(),
//             Messages.Tour_Database_Dialog_ConfirmUpdate_Title,
//             null,
//             NLS.bind(Messages.Database_Confirm_Update20, _databasePath),
//             MessageDialog.QUESTION,
//             buttons,
//             1).open()) != Window.OK) {
//
//          // no update -> close application
//          PlatformUI.getWorkbench().close();
//
//          return false;
//       }
      }

      _dbDesignVersion_New = currentDbVersion;
      _dbDesignVersion_Old = currentDbVersion;

      try (Connection conn = getConnection_Simple()) {

         // 1 -> 2
         if (currentDbVersion == 1) {
            updateDb_001_To_002(conn);
            currentDbVersion = _dbDesignVersion_New = 2;
         }

         // 2 -> 3
         if (currentDbVersion == 2) {
            updateDb_002_To_003(conn);
            currentDbVersion = _dbDesignVersion_New = 3;
         }

         // 3 -> 4
         if (currentDbVersion == 3) {
            updateDb_003_To_004(conn);
            currentDbVersion = _dbDesignVersion_New = 4;
         }

         // 4 -> 5      8.11
         if (currentDbVersion == 4) {
            updateDb_004_To_005(conn, splashManager);
            currentDbVersion = _dbDesignVersion_New = 5;
         }

         // 5 -> 6      8.12
         if (currentDbVersion == 5) {
            updateDb_005_To_006(conn, splashManager);
            currentDbVersion = _dbDesignVersion_New = 6;
         }

         // 6 -> 7      9.01
         if (currentDbVersion == 6) {
            updateDb_006_To_007(conn, splashManager);
            currentDbVersion = _dbDesignVersion_New = 7;
         }

         // 7 -> 8      10.2.1 Mod by Kenny
         if (currentDbVersion == 7) {
            updateDb_007_To_008(conn, splashManager);
            currentDbVersion = _dbDesignVersion_New = 8;
         }

         // 8 -> 9      10.3.0
         if (currentDbVersion == 8) {
            updateDb_008_To_009(conn, splashManager);
            currentDbVersion = _dbDesignVersion_New = 9;
         }

         // 9 -> 10     10.5.0 not released
         if (currentDbVersion == 9) {
            updateDb_009_To_010(conn, splashManager);
            currentDbVersion = _dbDesignVersion_New = 10;
         }

         // 10 -> 11    10.7.0 - 11-07-2010
         if (currentDbVersion == 10) {
            currentDbVersion = _dbDesignVersion_New = updateDb_010_To_011(conn, splashManager);
         }

         // 11 -> 12    10.9.1
         if (currentDbVersion == 11) {
            currentDbVersion = _dbDesignVersion_New = updateDb_011_To_012(conn, splashManager);
         }

         // 12 -> 13    10.11
         if (currentDbVersion == 12) {
            currentDbVersion = _dbDesignVersion_New = updateDb_012_To_013(conn, splashManager);
         }

         // 13 -> 14    11.3
         if (currentDbVersion == 13) {
            currentDbVersion = _dbDesignVersion_New = updateDb_013_To_014(conn, splashManager);
         }

         // 14 -> 15    11.8
         if (currentDbVersion == 14) {
            currentDbVersion = _dbDesignVersion_New = updateDb_014_To_015(conn, splashManager);
         }

         // 15 -> 16    11.8
         if (currentDbVersion == 15) {
            currentDbVersion = _dbDesignVersion_New = updateDb_015_To_016(conn, splashManager);
         }

         // 16 -> 17    11.8
         if (currentDbVersion == 16) {
            currentDbVersion = _dbDesignVersion_New = updateDb_016_To_017(conn, splashManager);
         }

         // 17 -> 18    11.8
         if (currentDbVersion == 17) {
            currentDbVersion = _dbDesignVersion_New = updateDb_017_To_018(conn, splashManager);
         }

         // 18 -> 19    11.8
         if (currentDbVersion == 18) {
            currentDbVersion = _dbDesignVersion_New = updateDb_018_To_019(conn, splashManager);
         }

         // 19 -> 20    12.1
         if (currentDbVersion == 19) {
            currentDbVersion = _dbDesignVersion_New = updateDb_019_To_020(conn, splashManager);
         }

         // 20 -> 21    12.1.1
         if (currentDbVersion == 20) {
            currentDbVersion = _dbDesignVersion_New = updateDb_020_To_021(conn, splashManager);
         }

         // 21 -> 22    12.12.0
         if (currentDbVersion == 21) {
            currentDbVersion = _dbDesignVersion_New = updateDb_021_To_022(conn, splashManager);
         }

         // 22 -> 23    13.2.0
         if (currentDbVersion == 22) {
            currentDbVersion = _dbDesignVersion_New = updateDb_022_To_023(conn, splashManager);
         }

         // 23 -> 24    14.7
         if (currentDbVersion == 23) {
            currentDbVersion = _dbDesignVersion_New = updateDb_023_To_024(conn, splashManager);
         }

         // 24 -> 25    14.10
         if (currentDbVersion == 24) {
            currentDbVersion = _dbDesignVersion_New = updateDb_024_To_025(conn, splashManager);
         }

         // 25 -> 26    14.14 / 15.3
         if (currentDbVersion == 25) {
            currentDbVersion = _dbDesignVersion_New = updateDb_025_To_026(splashManager);
         }

         // 26 -> 27    15.3.1
         if (currentDbVersion == 26) {
            currentDbVersion = _dbDesignVersion_New = updateDb_026_To_027(conn, splashManager);
         }

         // 27 -> 28    15.6
         if (currentDbVersion == 27) {
            currentDbVersion = _dbDesignVersion_New = updateDb_027_To_028(conn, splashManager);
         }

         // 28 -> 29    15.12
         if (currentDbVersion == 28) {
            currentDbVersion = _dbDesignVersion_New = updateDb_028_To_029(conn, splashManager);
         }

         // 29 -> 30    16.1
         if (currentDbVersion == 29) {
            currentDbVersion = _dbDesignVersion_New = updateDb_029_To_030(conn, splashManager);
         }

         // 30 -> 31    16.5
         if (currentDbVersion == 30) {
            currentDbVersion = _dbDesignVersion_New = updateDb_030_To_031(conn, splashManager);
         }

         // 31 -> 32    16.10
         if (currentDbVersion == 31) {
            currentDbVersion = _dbDesignVersion_New = updateDb_031_To_032(conn, splashManager);
         }

         // 32 -> 33    17.12
         if (currentDbVersion == 32) {
            currentDbVersion = _dbDesignVersion_New = updateDb_032_To_033(conn, splashManager);
         }

         // 33 -> 34    18.5
         if (currentDbVersion == 33) {
            currentDbVersion = _dbDesignVersion_New = updateDb_033_To_034(conn, splashManager);
         }

         // 34 -> 35    18.7
         if (currentDbVersion == 34) {
            currentDbVersion = _dbDesignVersion_New = updateDb_034_To_035(conn, splashManager);
         }

         // 35 -> 36    18.12
         if (currentDbVersion == 35) {
            currentDbVersion = _dbDesignVersion_New = updateDb_035_To_036(conn, splashManager);
         }

         // 36 -> 37    19.2
         if (currentDbVersion == 36) {
            currentDbVersion = _dbDesignVersion_New = updateDb_036_To_037(conn, splashManager);
         }

         // 37 -> 38    19.6
         if (currentDbVersion == 37) {
            currentDbVersion = _dbDesignVersion_New = updateDb_037_To_038(conn, splashManager);
         }

         // 38 -> 39    19.7
         if (currentDbVersion == 38) {
            currentDbVersion = _dbDesignVersion_New = updateDb_038_To_039(conn, splashManager);
         }

         // 39 -> 40    19.10
         if (currentDbVersion == 39) {
            currentDbVersion = _dbDesignVersion_New = updateDb_039_To_040(conn, splashManager);
         }

         // 40 -> 41    20.8
         if (currentDbVersion == 40) {
            currentDbVersion = _dbDesignVersion_New = updateDb_040_To_041(conn, splashManager);
         }

         // 41 -> 42    20.11.1 -> db fields are renamed
         if (currentDbVersion == 41) {
            currentDbVersion = _dbDesignVersion_New = updateDb_041_To_042(conn, splashManager);
         }

         // 42 -> 43    21.3
         if (currentDbVersion == 42) {
            currentDbVersion = _dbDesignVersion_New = updateDb_042_To_043(conn, splashManager);
         }

         // 43 -> 44    21.6
         if (currentDbVersion == 43) {
            currentDbVersion = _dbDesignVersion_New = updateDb_043_To_044(conn, splashManager);
         }

         // 44 -> 45    21.9
         if (currentDbVersion == 44) {
            currentDbVersion = _dbDesignVersion_New = updateDb_044_To_045(conn, splashManager);
         }

         // 45 -> 46    21.12
         if (currentDbVersion == 45) {
            currentDbVersion = _dbDesignVersion_New = updateDb_045_To_046(conn, splashManager);
         }

         // 46 -> 47    22.3.0
         if (currentDbVersion == 46) {
            currentDbVersion = _dbDesignVersion_New = updateDb_046_To_047(conn, splashManager);
         }

         // 47 -> 48    22.6
         if (currentDbVersion == 47) {
            currentDbVersion = _dbDesignVersion_New = updateDb_047_To_048(conn, splashManager);
         }

         // 48 -> 49    23.3
         if (currentDbVersion == 48) {
            currentDbVersion = _dbDesignVersion_New = updateDb_048_To_049(conn, splashManager);
         }

         // 49 -> 50    23.X
         if (currentDbVersion == 49) {
            currentDbVersion = _dbDesignVersion_New = updateDb_049_To_050(conn, splashManager);
         }

         // update db design version number
         updateVersionNumber_10_AfterDesignUpdate(conn, _dbDesignVersion_New);

      } catch (final SQLException e) {

         UI.showSQLException(e);

         _isSQLDesignUpdateError = true;

         return false;
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

         updateDb_003_To_004_DataUpdate(conn, splashManager);
         updateDb_004_To_005_DataUpdate(conn, splashManager);
         updateDb_008_To_009_DataUpdate(conn, splashManager);
         updateDb_010_To_011_DataUpdate(conn, splashManager);
         updateDb_012_To_013_DataUpdate(conn);
         updateDb_019_To_020_DataUpdate(conn, splashManager);
         updateDb_021_To_022_DataUpdate(conn, splashManager);
         updateDb_022_To_023_DataUpdate(conn, splashManager);
         updateDb_024_To_025_DataUpdate(conn, splashManager);
         updateDb_027_To_028_DataUpdate(conn, splashManager);
         updateDb_028_To_029_DataUpdate(conn, splashManager);
         updateDb_031_To_032_DataUpdate(conn, splashManager);
         updateDb_033_To_034_DataUpdate(conn, splashManager);
         updateDb_036_To_037_DataUpdate(conn, splashManager);
         updateDb_039_To_040_DataUpdate(conn, splashManager);
         updateDb_041_To_042_DataUpdate(conn);
         updateDb_042_to_043_DataUpdate(conn, splashManager);
         updateDb_046_to_047_DataUpdate(conn, splashManager);

         updateDb__3_Data_Concurrent(conn, splashManager, new TourDataUpdate_047_to_048());

      } catch (final SQLException e) {

         UI.showSQLException(e);
         _isSQLDataUpdateError = true;

         return false;
      }

      return true;
   }

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

      final List<Long> allTourIds = getAllTourIds();
      final int numAllTourIds = allTourIds.size();

      for (final Long tourId : allTourIds) {

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

         updateDb__4_Data_Concurrent_OneTour(tourId, tourDataUpdater);
      }

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
    * @return
    */
   private void updateDb__4_Data_Concurrent_OneTour(final long tourId,
                                                    final ITourDataUpdate tourDataUpdater) {

      try {

         // put tour ID (queue item) into the queue AND wait when it is full

         _dbUpdateQueue.put(tourId);

      } catch (final InterruptedException e) {

         _isSQLDataUpdateError = true;

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _dbUpdateExecutor.submit(() -> {

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
            if (!isTourUpdated) {
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
   }

   private void updateDb_001_To_002(final Connection conn) throws SQLException {

      final int dbVersion = 2;

      logDbUpdate_Start(dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset       INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset       INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType         BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_002_To_003(final Connection conn) throws SQLException {

      final int dbVersion = 3;

      logDbUpdate_Start(dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode           SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval   SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_003_To_004(final Connection conn) throws SQLException {

      final int dbVersion = 4;

      logDbUpdate_Start(dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   maxAltitude        INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   maxPulse           INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   avgPulse           INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   avgCadence         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   avgTemperature     INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   maxSpeed           FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourTitle          VARCHAR(" + TourData.DB_LENGTH_TOUR_TITLE + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourDescription    VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourStartPlace     VARCHAR(" + TourData.DB_LENGTH_TOUR_START_PLACE + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourEndPlace       VARCHAR(" + TourData.DB_LENGTH_TOUR_END_PLACE + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   calories           INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   bikerWeight        FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   " + KEY_BIKE + "   BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         // from wolfgang
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   devicePluginName   VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         // from wolfgang
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   deviceModeName     VARCHAR(" + TourData.DB_LENGTH_DEVICE_MODE_NAME + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);
      }

      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_003_To_004_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 4;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      if (splashManager != null) {
         splashManager.setMessage(Messages.Tour_Database_load_all_tours);
      }

      final ArrayList<Long> allTourIds = getAllTourIds();

      // loop over all tours and calculate and set new columns
      int tourIdx = 1;
      for (final Long tourId : allTourIds) {

         final TourData tourData = getTourFromDb(tourId);

         if (splashManager != null) {

            final String msg = NLS.bind(
                  Messages.Tour_Database_update_tour,
                  new Object[] { tourIdx++, allTourIds.size() });

            splashManager.setMessage(msg);
         }

         tourData.computeComputedValues();

         final TourPerson person = tourData.getTourPerson();

         tourData.setTourBike(person.getTourBike());
         tourData.setBodyWeight(person.getWeight());

         saveTour(tourData, false);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private void updateDb_004_To_005(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 5;

      logDbUpdate_Start(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 5));
      }

      Statement stmt = null;

      try {

         stmt = conn.createStatement();

         createTable_TourTag(stmt);
         createTable_TourTag_Category(stmt);
         createIndex_TourData_005(stmt);

      } finally {

         net.tourbook.common.util.SQL.close(stmt);
      }

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_004_To_005_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 5;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      computeAnyValues_ForAllTours(splashManager);
      TourManager.getInstance().removeAllToursFromCache();

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private void updateDb_005_To_006(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 6;

      logDbUpdate_Start(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 6));
      }

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourImportFilePath   VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_006_To_007(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 7;

      logDbUpdate_Start(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 7));
      }

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeSourceTourId       BIGINT"; //            //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeTargetTourId       BIGINT"; //            //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedTourTimeOffset    INTEGER DEFAULT 0"; //   //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedAltitudeOffset    INTEGER DEFAULT 0"; //   //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startSecond             SMALLINT DEFAULT 0"; //   //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_007_To_008(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 8;

      logDbUpdate_Start(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 8));
      }

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   weatherWindDir        INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   weatherWindSpd        INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   isDistanceFromSensor  SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   weatherClouds         VARCHAR(" + TourData.DB_LENGTH_WEATHER_CLOUDS + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   restPulse             INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private void updateDb_008_To_009(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 9;

      logDbUpdate_Start(dbVersion);

      updateMonitor(splashManager, dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startWeekYear      SMALLINT DEFAULT 1977 "; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   // 8 -> 9      10.3.0
   private void updateDb_008_To_009_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 9;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      // set app week number/year
      if (updateTourWeek(conn, splashManager)) {

// After more than 10 years this messagebox is not necessary anymore
//
//       MessageDialog.openInformation(
//          Display.getDefault().getActiveShell(),
//          Messages.Tour_Database_Dialog_ConfirmUpdate_Title,
//          Messages.Tour_Database_Update_TourWeek_Info);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private void updateDb_009_To_010(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 10;

      logDbUpdate_Start(dbVersion);

      updateMonitor(splashManager, dbVersion);

      final Statement stmt = conn.createStatement();
      {
         createTable_TourWayPoint(stmt);

         /**
          * Resize description column: ref derby docu page 24
          *
          * <pre>
          *
          * ALTER TABLE table-Name
          * {
          *     ADD COLUMN column-definition |
          *     ADD CONSTRAINT clause |
          *     DROP [ COLUMN ] column-name [ CASCADE | RESTRICT ]
          *     DROP { PRIMARY KEY | FOREIGN KEY constraint-name | UNIQUE
          *         constraint-name | CHECK constraint-name | CONSTRAINT constraint-name }
          *     ALTER [ COLUMN ] column-alteration |
          *     LOCKSIZE { ROW | TABLE }
          *
          *     column-alteration
          *
          *       column-Name SET DATA TYPE VARCHAR(integer) |
          *       column-Name SET DATA TYPE VARCHAR FOR BIT DATA(integer) |
          *       column-name SET INCREMENT BY integer-constant |
          *       column-name RESTART WITH integer-constant |
          *       column-name [ NOT ] NULL |
          *       column-name [ WITH | SET ] DEFAULT default-value |
          *       column-name DROP DEFAULT
          * }
          * </pre>
          */

         final String sql = UI.EMPTY_STRING

               + "ALTER TABLE " + TABLE_TOUR_DATA + NL //$NON-NLS-1$
               + "   ALTER COLUMN tourDescription" + NL//$NON-NLS-1$
               + "   SET DATA TYPE   VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + ")"; //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      stmt.close();

      logDbUpdate_End(dbVersion);
   }

   private int updateDb_010_To_011(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 11;

      logDbUpdate_Start(dbVersion);

      updateMonitor(splashManager, dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DateTimeCreated      BIGINT   DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DateTimeModified      BIGINT   DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(dbVersion);

      return dbVersion;
   }

   /**
    * Set create date/time from the tour date
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_010_To_011_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 11;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      PreparedStatement stmtSelect = null;
      PreparedStatement stmtUpdate = null;

      try {
         stmtSelect = conn.prepareStatement(UI.EMPTY_STRING

               + "SELECT" //                          //$NON-NLS-1$
               //
               + " StartYear," //                  1  //$NON-NLS-1$
               + " StartMonth," //                 2  //$NON-NLS-1$
               + " StartDay," //                   3  //$NON-NLS-1$
               + " StartHour," //                  4  //$NON-NLS-1$
               + " StartMinute," //                5  //$NON-NLS-1$
               + " StartSecond" //                 6  //$NON-NLS-1$
               //
               + " FROM " + TABLE_TOUR_DATA //        //$NON-NLS-1$
               + " WHERE TourId=?" //              1  //$NON-NLS-1$
         );

         stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

               + "UPDATE " + TABLE_TOUR_DATA //       //$NON-NLS-1$

               + " SET" //                            //$NON-NLS-1$
               + " DateTimeCreated=?" //           1  //$NON-NLS-1$
               + " WHERE tourId=?" //              2  //$NON-NLS-1$
         );

         int tourIdx = 1;
         long lastUpdateTime = System.currentTimeMillis();

         final ArrayList<Long> allTourIds = getAllTourIds();

         // loop: all tours
         for (final Long tourId : allTourIds) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate011_SetTourCreateTime,
                        new Object[] { tourIdx++, allTourIds.size() }));
               }
            }

            // get tour date
            stmtSelect.setLong(1, tourId);
            final ResultSet result = stmtSelect.executeQuery();

            while (result.next()) {

               // get date from database
               final short dbYear = result.getShort(1);
               final short dbMonth = result.getShort(2);
               final short dbDay = result.getShort(3);
               final short dbHour = result.getShort(4);
               final short dbMinute = result.getShort(5);
               final short dbSecond = result.getShort(6);

               final long dtCreated = (dbYear * 10000000000L)
                     + (dbMonth * 100000000L)
                     + (dbDay * 1000000L)
                     + (dbHour * 10000L)
                     + (dbMinute * 100L)
                     + dbSecond;

               // update DateTimeCreated in the database
               stmtUpdate.setLong(1, dtCreated);
               stmtUpdate.setLong(2, tourId);
               stmtUpdate.executeUpdate();
            }
         }

      } finally {

         net.tourbook.common.util.SQL.close(stmtSelect);
         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_011_To_012(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 12;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
//            + "   IsPulseSensorPresent      INTEGER DEFAULT 0,          " + NL //$NON-NLS-1$
//            + "   IsPowerSensorPresent      INTEGER DEFAULT 0,          " + NL //$NON-NLS-1$
//            + "   DeviceAvgSpeed            FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$
//            + "   DeviceFirmwareVersion   " + varCharKomma(TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION)) //$NON-NLS-1$

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   IsPulseSensorPresent  INTEGER   DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   IsPowerSensorPresent  INTEGER   DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   DeviceAvgSpeed        FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   DeviceFirmwareVersion VARCHAR(" + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_012_To_013(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 13;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

//         + "   TemperatureScale         INTEGER DEFAULT 1,             " + NL //$NON-NLS-1$
//         + " Weather                " + varCharNoKomma(TourData.DB_LENGTH_WEATHER) //$NON-NLS-1$

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   TemperatureScale      INTEGER   DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   Weather               VARCHAR(" + TourData.DB_LENGTH_WEATHER + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set temperature scale default value
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_012_To_013_DataUpdate(final Connection conn) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 13;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final String sql = "UPDATE " + TABLE_TOUR_DATA + " SET TemperatureScale=1"; //$NON-NLS-1$ //$NON-NLS-2$

      System.out.println(sql);
      System.out.println();

      conn.createStatement().executeUpdate(sql);

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_013_To_014(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 14;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

//      + "   ConconiDeflection         INTEGER DEFAULT 0,             " + NL //$NON-NLS-1$

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN ConconiDeflection         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_014_To_015(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 15;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

//      TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//      + "   BirthDay         BIGINT DEFAULT 0,                  " + NL //$NON-NLS-1$

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN BirthDay         BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_015_To_016(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 16;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {

         createTable_TourPersonHRZone(stmt);

//      TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//      + "   Gender            INTEGER DEFAULT 0,                " + NL //$NON-NLS-1$
//      + "   RestPulse         INTEGER DEFAULT 0,                " + NL //$NON-NLS-1$
//      + "   MaxPulse         INTEGER DEFAULT 0,                 " + NL //$NON-NLS-1$
//      + "   HrMaxFormula      INTEGER DEFAULT 0,                " + NL //$NON-NLS-1$
//
//      TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN Gender          INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN RestPulse       INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN MaxPulse        INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN HrMaxFormula    INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_016_To_017(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 17;

      logDbUpdate_Start(newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {

//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//
//         + "   hrZone0                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone1                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone2                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone3                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone4                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone5                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone6                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone7                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone8                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//         + "   hrZone9                  INTEGER DEFAULT -1,          " + NL //$NON-NLS-1$
//
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 0));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone0      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 1));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone1      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 2));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone2      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 3));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone3      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 4));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone4      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 5));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone5      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 6));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone6      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 7));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone7      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 8));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone8      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         if (splashManager != null) {
            splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 9));
         }
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone9      INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_017_To_018(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 18;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {

         if (_dbVersion_BeforeDesignUpdate > 16) {

            /*
             * db update 16 creates the HR zone db, doing this update causes an sql exception
             * because the fields are already available
             */

//            TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE
//
//            // version 18 start
//            //
//            + "   ColorRed         INTEGER DEFAULT 0,                " + NL //$NON-NLS-1$
//            + "   ColorGreen         INTEGER DEFAULT 0,                " + NL //$NON-NLS-1$
//            + "   ColorBlue         INTEGER DEFAULT 0,                " + NL //$NON-NLS-1$
//            //
//            // version 18 end ---------
//
//            TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE

            sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorRed    INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorGreen  INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorBlue   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);
         }

//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//
//         // version 18 start
//         //
//         + "   NumberOfHrZones            INTEGER DEFAULT 0,             " + NL //$NON-NLS-1$
//         //
//         // version 18 end ---------
//
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN NumberOfHrZones   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_018_To_019(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 19;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
//         TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE
//         //
//         // version 19 start
//         //
//         + "   colorTextRed       SMALLINT DEFAULT 0,                  " + NL //$NON-NLS-1$
//         + "   colorTextGreen       SMALLINT DEFAULT 0,                  " + NL //$NON-NLS-1$
//         + "   colorTextBlue       SMALLINT DEFAULT 0                  " + NL //$NON-NLS-1$
//         //
//         // version 19 end ---------
//         //
//         TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE

         sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextRed      SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextGreen    SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextBlue     SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_019_To_020(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 20;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      {
         updateDb_019_To_020_10_DataSerieBlobSize(conn);
         updateDb_019_To_020_20_AlterColumns(conn);
      }

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Increase {@link TourData#serieData} blob size.
    *
    * @param conn
    * @throws SQLException
    */
   private void updateDb_019_To_020_10_DataSerieBlobSize(final Connection conn) throws SQLException {

      final DatabaseMetaData meta = conn.getMetaData();

      final ResultSet rsColumns = meta.getColumns(null, TABLE_SCHEMA, TABLE_TOUR_DATA, "SERIEDATA"); //$NON-NLS-1$

      while (rsColumns.next()) {

         final int size = rsColumns.getInt("COLUMN_SIZE"); //$NON-NLS-1$
         if (size == 1048576) {

            /*
             * database is from a derby version before 10.5 which creates BLOB's with a default size
             * of 1M, increase size to 2G because a tour with 53000 can not be saved and causes an
             * exception
             */

            String sql;
            final Statement stmt = conn.createStatement();
            {
               // ALTER TABLE TourData ALTER COLUMN SerieData SET DATA TYPE BLOB(2G)

               sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ALTER COLUMN SerieData SET DATA TYPE BLOB(2G)"; //$NON-NLS-1$ //$NON-NLS-2$
               exec(stmt, sql);
            }
            stmt.close();
         }

         break;
      }
   }

   private void updateDb_019_To_020_20_AlterColumns(final Connection conn) throws SQLException {

      String sql;
      final Statement stmt = conn.createStatement();
      {
         // remove the NOT NULL constraint from the "distance" column
         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ALTER COLUMN distance   NULL"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         //         + "   distance20                FLOAT DEFAULT 0,               " + NL //$NON-NLS-1$
         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN distance20   FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_019_To_020_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 20;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         int tourIdx = 1;
         long lastUpdateTime = System.currentTimeMillis();

         final ArrayList<Long> allTourIds = getAllTourIds();

         // loop: all tours
         for (final Long tourId : allTourIds) {

            final long currentTime = System.currentTimeMillis();

            if (splashManager != null) {

               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  final float durationInSeconds = (currentTime - startTime) / 1000;

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate020_ConvertIntToFloat,
                        new Object[] { tourIdx, allTourIds.size(), (int) durationInSeconds }));
               }

               tourIdx++;
            }

            final TourData tourData = em.find(TourData.class, tourId);
            if (tourData != null) {

               /*
                * Ensure data series are converted
                */
               tourData.convertDataSeries();

               saveEntity(tourData, tourId, TourData.class);
            }
         }

      } finally {

         em.close();
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_020_To_021(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 21;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//         // version 21 start
//         //
//         + "   maxAltitude           FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   maxPulse              FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   avgPulse              FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   avgCadence            FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   avgTemperature        FLOAT DEFAULT 0,            " + NL //$NON-NLS-1$
//         //
//         // version 21 end ---------
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA

         int no = 0;
         final int max = 5;

         modifyColumn_Type(TABLE_TOUR_DATA, "maxAltitude", "FLOAT DEFAULT 0", stmt, splashManager, ++no, max); //         //$NON-NLS-1$ //$NON-NLS-2$
         modifyColumn_Type(TABLE_TOUR_DATA, "maxPulse", "FLOAT DEFAULT 0", stmt, splashManager, ++no, max); //            //$NON-NLS-1$ //$NON-NLS-2$
         modifyColumn_Type(TABLE_TOUR_DATA, "avgPulse", "FLOAT DEFAULT 0", stmt, splashManager, ++no, max); //            //$NON-NLS-1$ //$NON-NLS-2$
         modifyColumn_Type(TABLE_TOUR_DATA, "avgCadence", "FLOAT DEFAULT 0", stmt, splashManager, ++no, max); //         //$NON-NLS-1$ //$NON-NLS-2$
         modifyColumn_Type(TABLE_TOUR_DATA, "avgTemperature", "FLOAT DEFAULT 0", stmt, splashManager, ++no, max); //      //$NON-NLS-1$ //$NON-NLS-2$
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   // 21 -> 22    12.12.0
   private int updateDb_021_To_022(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 22;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         String sql;

         createTable_TourPhoto(stmt);

         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "TourStartTime") == false) {//$NON-NLS-1$

            // table columns are not yet created

//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//         // version 22 start  -  12.12.0
//         //
//         + "   TourStartTime            BIGINT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   TourEndTime              BIGINT DEFAULT 0,            " + NL //$NON-NLS-1$
//
//         + "   TourRecordingTime        BIGINT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   TourDrivingTime          BIGINT DEFAULT 0,            " + NL //$NON-NLS-1$
            //
            // version 22 end ---------

            sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TourStartTime        BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TourEndTime          BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            /*
             * modify columns
             */
            int no = 0;
            final int max = 2;

            modifyColumn_Type(TABLE_TOUR_DATA, "TourRecordingTime", "BIGINT DEFAULT 0", stmt, splashManager, ++no, max); //$NON-NLS-1$ //$NON-NLS-2$
            modifyColumn_Type(TABLE_TOUR_DATA, "TourDrivingTime", "BIGINT DEFAULT 0", stmt, splashManager, ++no, max); //$NON-NLS-1$ //$NON-NLS-2$

            createIndex_TourData_022(stmt);
         }

         if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "IsMarkerVisible") == false) {//$NON-NLS-1$

            // table columns are not yet created

//         TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER
//
//         // Version 22 - begin
//         //
//         + "   IsMarkerVisible          INTEGER DEFAULT 0,             " + NL //$NON-NLS-1$
//         //
//         // Version 22 - end

            sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN IsMarkerVisible      INTEGER DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set tour start/end time from the tour date and duration
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_021_To_022_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 22;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      PreparedStatement stmtSelect = null;
      PreparedStatement stmtUpdate = null;

      try {

         final String renamedTourRecordingTime = getRenamedField_TourRecordingTime(conn);

         stmtSelect = conn.prepareStatement(UI.EMPTY_STRING

               + "SELECT" //                             //$NON-NLS-1$
               //
               + " StartYear," //                     1  //$NON-NLS-1$
               + " StartMonth," //                    2  //$NON-NLS-1$
               + " StartDay," //                      3  //$NON-NLS-1$
               + " StartHour," //                     4  //$NON-NLS-1$
               + " StartMinute," //                   5  //$NON-NLS-1$
               + " StartSecond," //                   6  //$NON-NLS-1$
               + " " + renamedTourRecordingTime //    7  //$NON-NLS-1$
               //
               + " FROM " + TABLE_TOUR_DATA //           //$NON-NLS-1$
               + " WHERE TourId=?" //                    //$NON-NLS-1$
         );

         stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

               + "UPDATE " + TABLE_TOUR_DATA //          //$NON-NLS-1$
               //
               + " SET" //                               //$NON-NLS-1$
               //
               + " TourStartTime=?," //               1  //$NON-NLS-1$
               + " TourEndTime=?" //                  2  //$NON-NLS-1$
               //
               + " WHERE tourId=?"); //               3  //$NON-NLS-1$

         int tourIndex = 1;
         final ArrayList<Long> allTourIds = getAllTourIds();

         long lastUpdateTime = System.currentTimeMillis();

         // loop: all tours
         for (final Long tourId : allTourIds) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate021_SetTourStartEndTime,
                        new Object[] { tourIndex++, allTourIds.size() }));
               }
            }

            // get tour date for 1 tour
            stmtSelect.setLong(1, tourId);
            final ResultSet result = stmtSelect.executeQuery();

            while (result.next()) {

               // get date from database
               final short dbYear = result.getShort(1);
               final short dbMonth = result.getShort(2);
               final short dbDay = result.getShort(3);
               final short dbHour = result.getShort(4);
               final short dbMinute = result.getShort(5);
               final short dbSecond = result.getShort(6);

               final long recordingTime = result.getLong(7);

               final ZonedDateTime dtStart = ZonedDateTime.of(
                     dbYear,
                     dbMonth,
                     dbDay,
                     dbHour,
                     dbMinute,
                     dbSecond,
                     0,
                     TimeTools.getDefaultTimeZone());

               final ZonedDateTime dtEnd = dtStart.plusSeconds(recordingTime);

               // update tour start/end in the database
               stmtUpdate.setLong(1, dtStart.toInstant().toEpochMilli());
               stmtUpdate.setLong(2, dtEnd.toInstant().toEpochMilli());
               stmtUpdate.setLong(3, tourId);
               stmtUpdate.executeUpdate();
            }
         }

      } finally {

         net.tourbook.common.util.SQL.close(stmtSelect);
         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_022_To_023(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 23;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {

         if (isColumnAvailable(conn, TABLE_TOUR_PHOTO, "imageFileName") == false) {//$NON-NLS-1$

            // table columns are not yet created

//
//         TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO   TOUR_PHOTO
//
//         // version 23 start
//         //
//         + ("   imageFileName         " + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//         + ("   imageFileExt         " + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//         + ("   imageFilePath         " + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//         + ("   imageFilePathName      " + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//         //
//         + "   imageExifTime            BIGINT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   imageFileLastModified      BIGINT DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   adjustedTime            BIGINT DEFAULT 0,               " + NL //$NON-NLS-1$
//         //
//         + "   ratingStars               INT DEFAULT 0,               " + NL //$NON-NLS-1$
//         //
//         + "   isGeoFromPhoto            INT DEFAULT 0,               " + NL //$NON-NLS-1$
//         + "   latitude                DOUBLE DEFAULT 0,            " + NL //$NON-NLS-1$
//         + "   longitude                DOUBLE DEFAULT 0            " + NL //$NON-NLS-1$
//         //
//         // version 23 end

            final String[] sqlTourPhoto = {

                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFileName           VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFileExt            VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFilePath           VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFilePathName       VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageExifTime           BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFileLastModified   BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   adjustedTime            BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$

                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   ratingStars             INT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$

                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   isGeoFromPhoto          INT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   latitude                DOUBLE DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   longitude               DOUBLE DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
            };

            exec(stmt, sqlTourPhoto);
         }

         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "numberOfTimeSlices") == false) {//$NON-NLS-1$

            // table columns are not yet created
//
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//
//         // version 23 start
//         //
//         + " numberOfTimeSlices         INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
//         + " numberOfPhotos            INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
//         + " photoTimeAdjustment         INTEGER DEFAULT 0,            " + NL //$NON-NLS-1$
//         //
//         // version 23 end ---------

            final String[] sqlTourData = {

                  "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   numberOfTimeSlices       INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   numberOfPhotos           INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   photoTimeAdjustment      INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
            };

            exec(stmt, sqlTourData);
         }
      }

      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_022_To_023_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 23;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         int tourIdx = 1;
         long lastUpdateTime = System.currentTimeMillis();

         final ArrayList<Long> allTourIds = getAllTourIds();

         // loop: all tours
         for (final Long tourId : allTourIds) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate023_SetTimeSliceNumbers,
                        new Object[] { tourIdx, allTourIds.size() }));
               }

               tourIdx++;
            }

            final TourData tourData = em.find(TourData.class, tourId);
            if (tourData != null) {

               // compute number of time slices
               tourData.onPrePersist();

               saveEntity(tourData, tourId, TourData.class);
            }
         }

      } finally {

         em.close();
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_023_To_024(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 24;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 24
         if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "description") == false) {//$NON-NLS-1$

            // description column is not yet created -> do db update 24

            /**
             * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
             * <p>
             * Drop tables first, when something goes wrong the existing tables are not yet
             * modified.
             * <p>
             * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
             */

            /*
             * Drop tables which will never be used, they exist since many years but it is unknown
             * why they has been created.
             */
            SQL.Cleanup_DropTable(stmt, TABLE_TOUR_CATEGORY);
            SQL.Cleanup_DropTable(stmt, TABLE_TOURCATEGORY__TOURDATA);

            SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURMARKER);
            SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURPHOTO);
            SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURREFERENCE);
            SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURWAYPOINT);
            SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURPERSON__TOURPERSON_HRZONE);

            /*
             * Table: TOURTAG
             */
            {
               /**
                * Changed TagCategory from @ManyToMany to @ManyToOne because a tag can be associated
                * only with ONE category and not with multiple.
                */
               SQL.Cleanup_DropConstraint(stmt, JOINTABLE__TOURDATA__TOURTAG, "FK_TOURDATA_TOURTAG_TOURTAG_TAGID"); //$NON-NLS-1$
            }

            /*
             * Table: TOURMARKER
             */
            {
               /*
                * Adjust column with to TourWayPoint width, that both have the same max size.
                */
               SQL.AlterColumn_VarChar_Width(stmt, TABLE_TOUR_MARKER, "label", TourWayPoint.DB_LENGTH_NAME); //$NON-NLS-1$
               SQL.AlterColumn_VarChar_Width(stmt, TABLE_TOUR_MARKER, "category", TourWayPoint.DB_LENGTH_CATEGORY); //$NON-NLS-1$

               /*
                * Add new columns
                */
               SQL.AddColumn_VarCar(stmt, TABLE_TOUR_MARKER, "description", TourWayPoint.DB_LENGTH_DESCRIPTION); //$NON-NLS-1$
               SQL.AddColumn_VarCar(stmt, TABLE_TOUR_MARKER, "urlText", TourMarker.DB_LENGTH_URL_TEXT); //$NON-NLS-1$
               SQL.AddColumn_VarCar(stmt, TABLE_TOUR_MARKER, "urlAddress", TourMarker.DB_LENGTH_URL_ADDRESS); //$NON-NLS-1$
            }
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   // 24 -> 25    14.10
   private int updateDb_024_To_025(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 25;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // Table: TOURMARKER
         {
            // Add new columns
            SQL.AddColumn_BigInt(stmt, TABLE_TOUR_MARKER, "tourTime", SQL_LONG_MIN_VALUE);//$NON-NLS-1$
            SQL.AddColumn_Float(stmt, TABLE_TOUR_MARKER, "altitude", SQL_FLOAT_MIN_VALUE); //$NON-NLS-1$
            SQL.AddColumn_Double(stmt, TABLE_TOUR_MARKER, "latitude", SQL_DOUBLE_MIN_VALUE); //$NON-NLS-1$
            SQL.AddColumn_Double(stmt, TABLE_TOUR_MARKER, "longitude", SQL_DOUBLE_MIN_VALUE); //$NON-NLS-1$
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   public void updateDb_024_To_025_DataUpdate(final Connection conn, final Object progress) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 25;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         int tourIdx = 1;
         long lastUpdateTime = System.currentTimeMillis();

         final ArrayList<Long> allTourIds = getAllTourIds();

         // loop: all tours
         for (final Long tourId : allTourIds) {

            if (progress instanceof SplashManager) {

               final SplashManager splashManager = (SplashManager) progress;

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate025_SetMarkerFields,
                        new Object[] { tourIdx, allTourIds.size() }));
               }

               tourIdx++;
            }

            final TourData tourData = em.find(TourData.class, tourId);
            if (tourData == null) {
               continue;
            }

            /*
             * set absolute time/lat/lon/altitude in the tour marker from tour data
             */

            final float[] altitudeSerie = tourData.altitudeSerie;
            final double[] latitudeSerie = tourData.latitudeSerie;
            final double[] longitudeSerie = tourData.longitudeSerie;

            int serieLength = -1;
            if (altitudeSerie != null) {
               serieLength = altitudeSerie.length;
            } else if (latitudeSerie != null) {
               serieLength = latitudeSerie.length;
            }

            final long tourStartTime = tourData.getTourStartTimeMS();

            for (final TourMarker tourMarker : tourData.getTourMarkers()) {

               try {

                  final int serieIndex = tourMarker.getSerieIndex();

// this is used for debugging to force an error
//                  serieIndex += 100;

                  if (altitudeSerie != null) {
                     tourMarker.setAltitude(altitudeSerie[serieIndex]);
                  }

                  if (latitudeSerie != null) {
                     tourMarker.setGeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);
                  }

                  final int relativeTime = tourMarker.getTime();

                  tourMarker.setTime(relativeTime, tourStartTime + (relativeTime * 1000));

               } catch (final Exception e) {

                  /*
                   * This ArrayIndexOutOfBoundsException occurred during the update after this
                   * version was released. Therefore it's captured and detailed logged, other
                   * markers are not affected.
                   */

                  final String message = String.format(
                        "Tour: %s - Tour marker: %s - serieIndex: %d - serie length: %d - relative time: %d sec", //$NON-NLS-1$
                        TourManager.getTourDateTimeShort(tourData),
                        tourMarker.getLabel(),
                        tourMarker.getSerieIndex(),
                        serieLength,
                        tourMarker.getTime());

                  StatusUtil.showStatus(message, e);
               }

            }

            saveEntity(tourData, tourId, TourData.class);
         }

      } finally {

         em.close();
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   // 25 -> 26    14.14 / 15.3
   private int updateDb_025_To_026(final SplashManager splashManager) {

      final int newDbVersion = 26;

      /*
       * Version 26 is used to upgrade the derby database in
       * sqlStartup_70_Check_DbIsUpgraded_Before() and sqlStartup_80_Check_DbIsUpgraded_After() to
       * support fulltext search with apache lucene
       */

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   // 26 -> 27    15.3.1
   private int updateDb_026_To_027(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 27;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // Table: TABLE_TOUR_DATA
         {
            // Add new columns
            SQL.AddColumn_Int(stmt, TABLE_TOUR_DATA, "frontShiftCount", DEFAULT_0);//$NON-NLS-1$
            SQL.AddColumn_Int(stmt, TABLE_TOUR_DATA, "rearShiftCount", DEFAULT_0); //$NON-NLS-1$
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_027_To_028(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 28;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         /*
          * Table: TABLE_TOUR_COMPARED
          */
         {
            /*
             * Add new columns
             */
            SQL.AddColumn_Float(stmt, TABLE_TOUR_COMPARED, "AvgPulse", DEFAULT_0); //$NON-NLS-1$
         }

         /*
          * Table: TABLE_TOUR_WAYPOINT
          */
         {
            /*
             * Add new columns
             */
            SQL.AddColumn_VarCar(stmt, TABLE_TOUR_WAYPOINT, "urlText", TourMarker.DB_LENGTH_URL_TEXT); //$NON-NLS-1$
            SQL.AddColumn_VarCar(stmt, TABLE_TOUR_WAYPOINT, "urlAddress", TourMarker.DB_LENGTH_URL_ADDRESS); //$NON-NLS-1$
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_027_To_028_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 28;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      PreparedStatement stmt = null;
      PreparedStatement stmtSelect = null;
      PreparedStatement stmtUpdate = null;

      try {

         // get number of compared tours
         final String sql = "SELECT COUNT(*) FROM " + TourDatabase.TABLE_TOUR_COMPARED; //$NON-NLS-1$

         stmt = conn.prepareStatement(sql);
         ResultSet result = stmt.executeQuery();

         // get first result
         result.next();

         // get first value
         final int numComparedTours = result.getInt(1);
         if (numComparedTours != 0) {

            stmtSelect = conn.prepareStatement(UI.EMPTY_STRING

                  + "SELECT" //                             //$NON-NLS-1$

                  + " comparedId," //                    1  //$NON-NLS-1$
                  + " tourId," //                        2  //$NON-NLS-1$
                  + " startIndex," //                    3  //$NON-NLS-1$
                  + " endIndex" //                       4  //$NON-NLS-1$

                  + " FROM " + TourDatabase.TABLE_TOUR_COMPARED); //$NON-NLS-1$

            stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

                  + "UPDATE " + TABLE_TOUR_COMPARED //      //$NON-NLS-1$

                  + " SET" //                               //$NON-NLS-1$
                  + " avgPulse=?" //                     1  //$NON-NLS-1$
                  + " WHERE comparedId=?" //             2  //$NON-NLS-1$
            );

            result = stmtSelect.executeQuery();

            int compTourCounter = 0;

            long lastUpdateTime = System.currentTimeMillis();

            while (result.next()) {

               if (splashManager != null) {

                  final long currentTime = System.currentTimeMillis();
                  final float timeDiff = currentTime - lastUpdateTime;

                  // reduce logging
                  if (timeDiff > DELAY_SPLASH_LOGGING) {

                     lastUpdateTime = currentTime;

                     splashManager.setMessage(NLS.bind(
                           Messages.Tour_Database_PostUpdate_028_SetAvgPulse,
                           new Object[] { ++compTourCounter, numComparedTours }));
                  }
               }

               // get date from database
               final long compareId = result.getLong(1);
               final long tourId = result.getLong(2);
               final int startIndex = result.getInt(3);
               final int endIndex = result.getInt(4);

               final TourData tourData = TourManager.getTour(tourId);

               if (tourData == null) {

                  StatusUtil.logError(NLS.bind(
                        "Cannot get tour {0} from database to update the average pulse in the compared tour {1}.", //$NON-NLS-1$
                        tourId,
                        compareId));

               } else {

                  final float avgPulse = tourData.computeAvg_PulseSegment(startIndex, endIndex);

                  // update average pulse for the compared tour
                  stmtUpdate.setFloat(1, avgPulse);
                  stmtUpdate.setLong(2, compareId);
                  stmtUpdate.executeUpdate();
               }
            }

         }

      } finally {

         net.tourbook.common.util.SQL.close(stmt);
         net.tourbook.common.util.SQL.close(stmtSelect);
         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_028_To_029(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 29;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (final Statement stmt = conn.createStatement()) {

         // check if db is updated to version 29
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "TourImportFileName") == false) { //$NON-NLS-1$

            // TABLE_TOUR_DATA: add column TourImportFileName
            SQL.AddColumn_VarCar(stmt, TABLE_TOUR_DATA, "TourImportFileName", TourData.DB_LENGTH_TOUR_IMPORT_FILE_NAME); //$NON-NLS-1$

            createIndex_TourData_029(stmt);
         }
      }

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set tour start/end time from the tour date and duration
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_028_To_029_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 29;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final int numTours = getAllTourIds().size();

      PreparedStatement stmtSelect = null;
      PreparedStatement stmtUpdate = null;

      try {

         stmtSelect = conn.prepareStatement(UI.EMPTY_STRING

               + "SELECT" //                          //$NON-NLS-1$

               + " TourID," //                     1  //$NON-NLS-1$
               + " TourImportFilePath" //          2  //$NON-NLS-1$

               + " FROM " + TABLE_TOUR_DATA //        //$NON-NLS-1$
         );

         stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

               + "UPDATE " + TABLE_TOUR_DATA //       //$NON-NLS-1$

               + " SET" //                            //$NON-NLS-1$

               + " TourImportFileName=?," //       1  //$NON-NLS-1$
               + " TourImportFilePath=?" //        2  //$NON-NLS-1$

               + " WHERE TourID=?"); //            3 //$NON-NLS-1$

         int tourIndex = 0;
         long lastUpdateTime = System.currentTimeMillis();

         final ResultSet result = stmtSelect.executeQuery();

         while (result.next()) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate_029_SetImportFileName,
                        new Object[] { ++tourIndex, numTours }));
               }
            }

            // get data from database
            final long dbTourId = result.getLong(1);
            final String dbFilePath = result.getString(2);

            // get NIO path
            final Path filePath = NIO.getPath(dbFilePath);
            if (filePath != null) {

               // extract file name
               final Path fileName = filePath.getFileName();
               if (fileName != null) {

                  // extract folder
                  final Path folderPath = filePath.getParent();

                  // set file name
                  stmtUpdate.setString(1, fileName.toString());

                  //
                  stmtUpdate.setString(2,
                        folderPath == null
                              ? UI.EMPTY_STRING
                              : folderPath.toString());

                  stmtUpdate.setLong(3, dbTourId);

                  stmtUpdate.executeUpdate();
               }
            }
         }

      } finally {

         net.tourbook.common.util.SQL.close(stmtSelect);
         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_029_To_030(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 30;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_Avg",                         DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Int   (stmt, TABLE_TOUR_DATA, "power_Max",                         DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Int   (stmt, TABLE_TOUR_DATA, "power_Normalized",                  DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Int   (stmt, TABLE_TOUR_DATA, "power_FTP",                         DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_BigInt(stmt, TABLE_TOUR_DATA, "power_TotalWork",                   DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_TrainingStressScore",         DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_IntensityFactor",             DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_Int   (stmt, TABLE_TOUR_DATA, "power_PedalLeftRightBalance",       DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_AvgLeftTorqueEffectiveness",  DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_AvgRightTorqueEffectiveness", DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_AvgLeftPedalSmoothness",      DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float (stmt, TABLE_TOUR_DATA, "power_AvgRightPedalSmoothness",     DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_030_To_031(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 31;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // Add new columns
         SQL.AddColumn_Float(stmt, TABLE_TOUR_DATA, "CadenceMultiplier", DEFAULT_1_0); //$NON-NLS-1$
         SQL.AddColumn_Int(stmt, TABLE_TOUR_DATA, "IsStrideSensorPresent", DEFAULT_0); //$NON-NLS-1$
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_031_To_032(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 32;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // TABLE_TOUR_DATA: add column TimeZoneId
         SQL.AddColumn_VarCar(stmt, TABLE_TOUR_DATA, "TimeZoneId", TourData.DB_LENGTH_TIME_ZONE_ID); //$NON-NLS-1$
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set timezone in a tour to the tour starting point when lat/lon is available.
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_031_To_032_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 32;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         int tourIdx = 1;
         long lastUpdateTime = System.currentTimeMillis();

         final ArrayList<Long> allTourIds = getAllTourIds();

         // loop: all tours
         for (final Long tourId : allTourIds) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  final String percent = String.format(NUMBER_FORMAT_1F, (float) tourIdx / allTourIds.size() * 100.0);

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate_032_SetTourTimeZone,
                        new Object[] { tourIdx, allTourIds.size(), percent }));
               }

               tourIdx++;
            }

            final TourData tourData = em.find(TourData.class, tourId);

            if (tourData != null && tourData.latitudeSerie != null) {

               // get time zone from lat/lon
               final double lat = tourData.latitudeSerie[0];
               final double lon = tourData.longitudeSerie[0];

               final String rawZoneId = TimezoneMapper.latLngToTimezoneString(lat, lon);
               final ZoneId zoneId = ZoneId.of(rawZoneId);

               tourData.setTimeZoneId(zoneId.getId());

               saveEntity(tourData, tourId, TourData.class);
            }
         }

      } finally {

         em.close();
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_032_To_033(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 33;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (final Statement stmt = conn.createStatement()) {

         // check if db already contains the index
         if (isIndexAvailable(conn, TABLE_TOUR_DATA, "StartWeek") == false) { //$NON-NLS-1$

            createIndex_TourData_033(stmt);
         }
      }

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_033_To_034(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 34;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // double check if db already updated
         if (isTableAvailable(conn, TABLE_TOUR_GEO_PARTS) == false) {

            createTable_TourGeoParts(stmt);
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set geo parts
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_033_To_034_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 34;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         long lastUpdateTime = startTime;

         final ArrayList<Long> allTours = getAllTourIds();

         final int numTours = allTours.size();
         int tourIndex = 1;

         // loop: all tours
         for (final Long tourId : allTours) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  final String percent = String.format(NUMBER_FORMAT_1F, (float) tourIndex / numTours * 100.0);

                  splashManager.setMessage(NLS.bind(
                        Messages.Tour_Database_PostUpdate_034_SetTourGeoParts,
                        new Object[] { tourIndex, numTours, percent }));
               }

               tourIndex++;
            }

            final TourData tourData = em.find(TourData.class, tourId);

            if (tourData != null) {
               saveTour_GeoParts(tourData);
            }
         }

         // update UI otherwise < 100% is displayed
         splashManager.setMessage(NLS.bind(
               Messages.Tour_Database_PostUpdate_034_SetTourGeoParts,
               new Object[] { tourIndex - 1, numTours, 100 }));

      } finally {
         em.close();
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_034_To_035(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 35;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTime_Min",           DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTime_Max",           DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float   (stmt, TABLE_TOUR_DATA, "runDyn_StanceTime_Avg",           DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTimeBalance_Min",    DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTimeBalance_Max",    DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float   (stmt, TABLE_TOUR_DATA, "runDyn_StanceTimeBalance_Avg",    DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StepLength_Min",           DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StepLength_Max",           DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float   (stmt, TABLE_TOUR_DATA, "runDyn_StepLength_Avg",           DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalOscillation_Min",  DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalOscillation_Max",  DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float   (stmt, TABLE_TOUR_DATA, "runDyn_VerticalOscillation_Avg",  DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalRatio_Min",        DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalRatio_Max",        DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float   (stmt, TABLE_TOUR_DATA, "runDyn_VerticalRatio_Avg",        DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_035_To_036(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 36;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
//          // version 36 start  -  18.12
//          //
//            + "   surfing_NumberOfEvents                 SMALLINT DEFAULT 0,                 " + NL //$NON-NLS-1$
//            + "   surfing_MinSpeed_StartStop             SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
//            + "   surfing_MinSpeed_Surfing               SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
//            + "   surfing_MinTimeDuration                SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
//
//            + "   surfing_IsMinDistance                  BOOLEAN  DEFAULT FALSE,             " + NL //$NON-NLS-1$
//            + "   surfing_MinDistance                    SMALLINT DEFAULT -1,                " + NL //$NON-NLS-1$
//
//          //
//          // version 36 end ---------

// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_NumberOfEvents",          DEFAULT_0); //$NON-NLS-1$

         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinDistance",             DEFAULT_IGNORED); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinSpeed_StartStop",      DEFAULT_IGNORED); //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinSpeed_Surfing",        DEFAULT_IGNORED); //$NON-NLS-1$

         SQL.AddColumn_Boolean (stmt, TABLE_TOUR_DATA, "surfing_IsMinDistance",           DEFAULT_FALSE);   //$NON-NLS-1$
         SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinTimeDuration",         DEFAULT_IGNORED); //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_036_To_037(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 37;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if column is available
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "HasGeoData") == false) { //$NON-NLS-1$

            SQL.AddColumn_Boolean(stmt, TABLE_TOUR_DATA, "HasGeoData", DEFAULT_FALSE); //$NON-NLS-1$

            createIndex_TourData_037(stmt);
         }

         // check if db is available
         if (isTableAvailable(conn, TABLE_TOUR_GEO_PARTS)) {

            // check if db already contains the constraint
            if (isPrimaryKeyAvailable(conn, TABLE_TOUR_GEO_PARTS, "PK_TourId_GeoPart") == false) { //$NON-NLS-1$

               final String sql = UI.EMPTY_STRING

                     + "ALTER TABLE " + TABLE_TOUR_GEO_PARTS //                                 //$NON-NLS-1$
                     + "   ADD CONSTRAINT PK_TourId_GeoPart PRIMARY KEY (TourId, GeoPart)"; //  //$NON-NLS-1$

               exec(stmt, sql);
            }
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set hasGeoData
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_036_To_037_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 37;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      PreparedStatement stmtUpdate = null;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         final ArrayList<Long> allTours = getAllTourIds();

         final int numTours = allTours.size();
         int tourIndex = 1;

         long lastUpdateTime = startTime;

         stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

               + "UPDATE " + TABLE_TOUR_DATA //       //$NON-NLS-1$

               + " SET" //                            //$NON-NLS-1$
               + " HasGeoData=?" //                1  //$NON-NLS-1$
               + " WHERE tourId=?"); //            2  //$NON-NLS-1$

         // loop: all tours
         for (final Long tourId : allTours) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > DELAY_SPLASH_LOGGING) {

                  lastUpdateTime = currentTime;

                  final String percent = String.format(NUMBER_FORMAT_1F, (float) tourIndex / numTours * 100.0);

                  final String message = NLS.bind(Messages.Tour_Database_PostUpdate_037_SetHasGeoData,
                        new Object[] { tourIndex, numTours, percent });

                  splashManager.setMessage(message);
               }

               tourIndex++;
            }

            final TourData tourData = em.find(TourData.class, tourId);

            // set hasGeoData flag when lat/lon is available
            if (tourData != null && tourData.latitudeSerie != null && tourData.latitudeSerie.length > 0) {

               stmtUpdate.setBoolean(1, true);
               stmtUpdate.setLong(2, tourId);

               stmtUpdate.executeUpdate();
            }
         }

         // update UI otherwise < 100% is displayed
         splashManager.setMessage(NLS.bind(
               Messages.Tour_Database_PostUpdate_037_SetHasGeoData,
               new Object[] { tourIndex - 1, numTours, 100 }));

      } finally {

         em.close();

         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_037_To_038(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 38;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_VarCar (stmt, TABLE_TOUR_TAG,              "notes", TourTag.DB_LENGTH_NOTES);                 //$NON-NLS-1$
         SQL.AddColumn_VarCar (stmt, TABLE_TOUR_TAG_CATEGORY,     "notes", TourTag.DB_LENGTH_NOTES);                 //$NON-NLS-1$

         SQL.AddColumn_Float  (stmt, TABLE_TOUR_DATA,             "training_TrainingEffect_Aerob",       DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float  (stmt, TABLE_TOUR_DATA,             "training_TrainingEffect_Anaerob",     DEFAULT_0); //$NON-NLS-1$
         SQL.AddColumn_Float  (stmt, TABLE_TOUR_DATA,             "training_TrainingPerformance",        DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_038_To_039(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 39;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_Boolean   (stmt, TABLE_TOUR_DATA, "isWeatherDataFromApi",          DEFAULT_FALSE);   //$NON-NLS-1$
         SQL.AddColumn_SmallInt  (stmt, TABLE_TOUR_DATA, "weather_Humidity",              DEFAULT_0);       //$NON-NLS-1$
         SQL.AddColumn_Float     (stmt, TABLE_TOUR_DATA, "weather_Precipitation",         DEFAULT_0);       //$NON-NLS-1$
         SQL.AddColumn_Float     (stmt, TABLE_TOUR_DATA, "weather_Pressure",              DEFAULT_0);       //$NON-NLS-1$
         SQL.AddColumn_Float     (stmt, TABLE_TOUR_DATA, "weather_Temperature_Min",       DEFAULT_0);       //$NON-NLS-1$
         SQL.AddColumn_Float     (stmt, TABLE_TOUR_DATA, "weather_Temperature_Max",       DEFAULT_0);       //$NON-NLS-1$
         SQL.AddColumn_Float     (stmt, TABLE_TOUR_DATA, "weather_Temperature_WindChill", DEFAULT_0);       //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_039_To_040(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 40;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_VarCar (stmt, TABLE_TOUR_DATA, "power_DataSource",              TourData.DB_LENGTH_POWER_DATA_SOURCE); //$NON-NLS-1$
         SQL.AddColumn_Int    (stmt, TABLE_TOUR_DATA, "cadenceZone_SlowTime",          DEFAULT_0);                            //$NON-NLS-1$
         SQL.AddColumn_Int    (stmt, TABLE_TOUR_DATA, "cadenceZone_FastTime",          DEFAULT_0);                            //$NON-NLS-1$
         SQL.AddColumn_Int    (stmt, TABLE_TOUR_DATA, "cadenceZones_DelimiterValue",   DEFAULT_0);                            //$NON-NLS-1$
         SQL.AddColumn_Int    (stmt, TABLE_TOUR_DATA, "avgAltitudeChange",             DEFAULT_0);                            //$NON-NLS-1$

         SQL.AddColumn_Int    (stmt, TABLE_TOUR_COMPARED, RENAMED__TOUR_RECORDING_TIME__FROM,         DEFAULT_0);

         // Create index in table: TOURDATA_TOURTAG - Index: TOURTAG_TAGID
         SQL.CreateIndex   (stmt, JOINTABLE__TOURDATA__TOURTAG, KEY_TAG);

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set tourRecordingTime
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDb_039_To_040_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 40;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      PreparedStatement stmt = null;
      PreparedStatement stmtSelect = null;
      PreparedStatement stmtUpdate = null;

      try {

         // get number of compared tours
         final String sql = "SELECT COUNT(*) FROM " + TourDatabase.TABLE_TOUR_COMPARED; //$NON-NLS-1$

         stmt = conn.prepareStatement(sql);
         ResultSet result = stmt.executeQuery();

         // only the first result is needed
         result.next();

         // get first value
         final int numberOfComparedTours = result.getInt(1);
         if (numberOfComparedTours != 0) {

            stmtSelect = conn.prepareStatement(UI.EMPTY_STRING

                  + "SELECT" //                             //$NON-NLS-1$

                  + " comparedId," //                    1  //$NON-NLS-1$
                  + " tourId," //                        2  //$NON-NLS-1$
                  + " startIndex," //                    3  //$NON-NLS-1$
                  + " endIndex" //                       4  //$NON-NLS-1$

                  + " FROM " + TourDatabase.TABLE_TOUR_COMPARED //$NON-NLS-1$
            );

            final String renamedField_TourRecordingTime = getRenamedField_TourRecordingTime(conn);

            stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

                  + "UPDATE " + TABLE_TOUR_COMPARED //                  //$NON-NLS-1$

                  + " SET" //                                           //$NON-NLS-1$
                  + " " + renamedField_TourRecordingTime + "=?" //   1  //$NON-NLS-1$ //$NON-NLS-2$

                  + " WHERE comparedId=?" //                         2 //$NON-NLS-1$
            );

            result = stmtSelect.executeQuery();

            int compTourCounter = 0;
            long lastUpdateTime = startTime;

            while (result.next()) {

               if (splashManager != null) {

                  ++compTourCounter;

                  final long currentTime = System.currentTimeMillis();
                  final float timeDiff = currentTime - lastUpdateTime;

                  // reduce logging
                  if (timeDiff > DELAY_SPLASH_LOGGING) {

                     lastUpdateTime = currentTime;

                     splashManager.setMessage(NLS.bind(
                           Messages.Tour_Database_PostUpdate_040_SetTourRecordingTime,
                           new Object[] { compTourCounter, numberOfComparedTours }));
                  }
               }

               final long compareId = result.getLong(1);
               final long tourId = result.getLong(2);
               final int startIndex = result.getInt(3);
               final int endIndex = result.getInt(4);

               final TourData tourData = TourManager.getTour(tourId);

               if (tourData == null) {

                  StatusUtil.logError(NLS.bind(
                        "Cannot get tour {0} from database to update the recording time in the compared tour {1}.", //$NON-NLS-1$
                        tourId,
                        compareId));

               } else {

                  final int tourDeviceTime_Elapsed = TourManager.computeTourDeviceTime_Elapsed(tourData, startIndex, endIndex);

                  // update tour recording time for the compared tour
                  stmtUpdate.setInt(1, tourDeviceTime_Elapsed);
                  stmtUpdate.setLong(2, compareId);
                  stmtUpdate.executeUpdate();
               }
            }
         }

      } finally {

         net.tourbook.common.util.SQL.close(stmt);
         net.tourbook.common.util.SQL.close(stmtSelect);
         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   /**
    * 40 -> 41 ... 20.8
    *
    * @param conn
    * @param splashManager
    * @return
    * @throws SQLException
    */
   private int updateDb_040_To_041(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 41;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // Add new columns
         SQL.AddColumn_Float(stmt, TABLE_TOUR_DATA, "maxPace", DEFAULT_0); //$NON-NLS-1$
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   // 41 -> 42    20.11.1
   private int updateDb_041_To_042(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 42;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (Statement stmt = conn.createStatement()) {

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddColumn_BigInt(stmt, TABLE_TOUR_DATA,     "tourDeviceTime_Recorded", DEFAULT_0);                   //$NON-NLS-1$
            SQL.AddColumn_BigInt(stmt, TABLE_TOUR_DATA,     "tourDeviceTime_Paused",   DEFAULT_0);                   //$NON-NLS-1$
            SQL.AddColumn_Float(stmt,  TABLE_TOUR_DATA,     "bodyFat",                 DEFAULT_0);                   //$NON-NLS-1$

            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_RECORDING_TIME__FROM,    RENAMED__TOUR_RECORDING_TIME__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_DRIVING_TIME__FROM,      RENAMED__TOUR_DRIVING_TIME__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__BIKER_WEIGHT__FROM,           RENAMED__BIKER_WEIGHT__INTO);

            SQL.RenameCol(stmt,     TABLE_TOUR_COMPARED, RENAMED__TOUR_RECORDING_TIME__FROM,    RENAMED__TOUR_RECORDING_TIME__INTO);

// SET_FORMATTING_ON
      }

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private void updateDb_041_To_042_DataUpdate(final Connection conn) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 42;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      PreparedStatement stmtUpdate = null;

      try {

         stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

               + "UPDATE " + TABLE_TOUR_DATA //                      //$NON-NLS-1$
               + " SET" //                                           //$NON-NLS-1$
               + " tourDeviceTime_Recorded=tourDeviceTime_Elapsed"); //$NON-NLS-1$

         stmtUpdate.executeUpdate();

      } finally {

         net.tourbook.common.util.SQL.close(stmtUpdate);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   private int updateDb_042_To_043(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 43;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (Statement stmt = conn.createStatement()) {

         createTable_DbVersion_Data(stmt, _dbVersionOnStartup);
      }

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @return Data update version when the data update is successfully run
    * @throws MyTourbookException
    * @throws SQLException
    * @throws Exception
    */
   private void updateDb_042_to_043_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 43;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      long lastUpdateTime = startTime;

      int tourIndex = 1;
      int lastUpdateNumItems = 1;
      int sumUpdatedTours = 0;

      final List<Long> allTourIds = getAllTourIds();
      final int numAllTourIds = allTourIds.size();

      // loop: all tours
      for (final Long tourId : allTourIds) {

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

               splashManager.setMessage(NLS.bind(

                     // Data update 43: Converting lat/lon \u2192 E6 - {0} of {1} - {2} % - {3} \u0394
                     Messages.Tour_Database_PostUpdate_043_LatLonE6,

                     new Object[] {
                           sumUpdatedTours,
                           numAllTourIds,
                           percentValue,
                           numTourDiff,
                     }));
            }

            tourIndex++;
         }

         updateDb_042_To_043_DataUpdate_Concurrent(tourId);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   /**
    * Do data updates concurrently with all available processor threads, this is reducing time
    * significantly.
    *
    * @param tourId
    * @param <T>
    * @param entity
    * @param tourId
    * @param entityClass
    */
   private void updateDb_042_To_043_DataUpdate_Concurrent(final Long tourId) {

      // put tour ID (queue item) into the queue AND wait when it is full

      try {

         _dbUpdateQueue.put(tourId);

      } catch (final InterruptedException e) {

         _isSQLDataUpdateError = true;

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _dbUpdateExecutor.submit(() -> {

         // get last added item
         final Long queueItem_TourId = _dbUpdateQueue.poll();

         if (queueItem_TourId == null) {
            return;
         }

         final EntityManager em = TourDatabase.getInstance().getEntityManager();

         try {

            // get tour data by tour id
            final TourData tourData = em.find(TourData.class, queueItem_TourId);
            if (tourData == null) {
               return;
            }

            // ignore tours which having no geo data
            if (tourData.altitudeSerie == null) {
               return;
            }

            tourData.updateDatabaseDesign_042_to_043();

            boolean isSaved = false;

            final EntityTransaction ts = em.getTransaction();
            try {

               ts.begin();
               {
                  em.merge(tourData);
               }
               ts.commit();

            } catch (final Exception e) {

               _isSQLDataUpdateError = true;
               StatusUtil.showStatus(e);

            } finally {
               if (ts.isActive()) {
                  ts.rollback();
               } else {
                  isSaved = true;
               }
            }

            if (isSaved == false) {
               showTourSaveError(tourData);
            }

         } finally {

            em.close();
         }
      });
   }

   /**
    * DB version 43 -> 44 ... MT version 21.6
    *
    * @param conn
    * @param splashManager
    * @return
    * @throws SQLException
    */
   private int updateDb_043_To_044(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 44;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {

// renamed/converted rgb fields into one field with version 44
//
//       + "   colorBrightRed       SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//       + "   colorBrightGreen     SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//       + "   colorBrightBlue      SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//
//       + "   colorDarkRed         SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//       + "   colorDarkGreen       SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//       + "   colorDarkBlue        SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//
//       + "   colorLineRed         SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//       + "   colorLineGreen       SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//       + "   colorLineBlue        SMALLINT NOT NULL,                                       " + NL //$NON-NLS-1$
//
//       // version 19 start
//
//       + "   colorTextRed         SMALLINT DEFAULT 0,                                      " + NL //$NON-NLS-1$
//       + "   colorTextGreen       SMALLINT DEFAULT 0,                                      " + NL //$NON-NLS-1$
//       + "   colorTextBlue        SMALLINT DEFAULT 0                                       " + NL //$NON-NLS-1$
//
//       // version 19 end ---------
//
//       // version 44 start
//
//       + "   Color_Gradient_Bright    INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
//       + "   Color_Gradient_Dark      INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
//
//       + "   Color_Line_LightTheme    INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
//       + "   Color_Line_DarkTheme     INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
//
//       + "   Color_Text_LightTheme    INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
//       + "   Color_Text_DarkTheme     INTEGER DEFAULT 0,                                   " + NL //$NON-NLS-1$
//
//       // version 44 end ---------

// SET_FORMATTING_OFF

         // add new fields
         SQL.AddColumn_Int(stmt, TABLE_TOUR_TYPE, "Color_Gradient_Bright",    DEFAULT_0);    //$NON-NLS-1$
         SQL.AddColumn_Int(stmt, TABLE_TOUR_TYPE, "Color_Gradient_Dark",      DEFAULT_0);    //$NON-NLS-1$

         SQL.AddColumn_Int(stmt, TABLE_TOUR_TYPE, "Color_Line_LightTheme",    DEFAULT_0);    //$NON-NLS-1$
         SQL.AddColumn_Int(stmt, TABLE_TOUR_TYPE, "Color_Line_DarkTheme",     DEFAULT_0);    //$NON-NLS-1$

         SQL.AddColumn_Int(stmt, TABLE_TOUR_TYPE, "Color_Text_LightTheme",    DEFAULT_0);    //$NON-NLS-1$
         SQL.AddColumn_Int(stmt, TABLE_TOUR_TYPE, "Color_Text_DarkTheme",     DEFAULT_0);    //$NON-NLS-1$

         // update new fields with old values
         execUpdate(stmt,

            "UPDATE " + TABLE_TOUR_TYPE + NL //$NON-NLS-1$

            + "SET" + NL //$NON-NLS-1$

            + " COLOR_GRADIENT_BRIGHT  = (COLORBRIGHTRED * 65536) + (COLORBRIGHTGREEN * 256) + COLORBRIGHTBLUE,"  + NL  //$NON-NLS-1$
            + " COLOR_GRADIENT_DARK    = (COLORDARKRED   * 65536) + (COLORDARKGREEN   * 256) + COLORDARKBLUE,"    + NL  //$NON-NLS-1$

            + " COLOR_LINE_LIGHTTHEME  = (COLORLINERED   * 65536) + (COLORLINEGREEN   * 256) + COLORLINEBLUE,"    + NL  //$NON-NLS-1$
            + " COLOR_TEXT_LIGHTTHEME  = (COLORTEXTRED   * 65536) + (COLORTEXTGREEN   * 256) + COLORTEXTBLUE,"    + NL  //$NON-NLS-1$

            // set defaults for the dark theme that it is not displayed with black color
            + " COLOR_LINE_DARKTHEME   = (COLORLINERED   * 65536) + (COLORLINEGREEN   * 256) + COLORLINEBLUE,"    + NL  //$NON-NLS-1$
            + " COLOR_TEXT_DARKTHEME   = (COLORTEXTRED   * 65536) + (COLORTEXTGREEN   * 256) + COLORTEXTBLUE"     + NL  //$NON-NLS-1$

         );

         // drop old fields
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorBrightRed");                    //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorBrightGreen");                  //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorBrightBlue");                   //$NON-NLS-1$

         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorDarkRed");                      //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorDarkGreen");                    //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorDarkBlue");                     //$NON-NLS-1$

         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorLineRed");                      //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorLineGreen");                    //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorLineBlue");                     //$NON-NLS-1$

         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorTextRed");                      //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorTextGreen");                    //$NON-NLS-1$
         SQL.Cleanup_DropColumn(stmt, TABLE_TOUR_TYPE, "colorTextBlue");                     //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * DB version 44 -> 45 ... MT version 21.9
    *
    * @param conn
    * @param splashManager
    * @return
    * @throws SQLException
    */
   private int updateDb_044_To_045(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 45;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      // new fields are added to the tour data index
      FTSearchManager.deleteIndex();

      final Statement stmt = conn.createStatement();
      {
         // double check if db already exists
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "Battery_Percentage_Start") == false) { //$NON-NLS-1$

            // add new fields
            SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "Battery_Percentage_Start", DEFAULT_IGNORED); //$NON-NLS-1$
            SQL.AddColumn_SmallInt(stmt, TABLE_TOUR_DATA, "Battery_Percentage_End", DEFAULT_IGNORED); //$NON-NLS-1$

            SQL.CreateIndex_Combined(stmt, TABLE_TOUR_DATA, "Battery_Percentage_Start"); //$NON-NLS-1$
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * DB version 45 -> 46 ... MT version 21.12
    *
    * @param conn
    * @param splashManager
    * @return
    * @throws SQLException
    */
   private int updateDb_045_To_046(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 46;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // double check if db already exists
         if (isTableAvailable(conn, TABLE_DEVICE_SENSOR) == false) {
            createTable_DeviceSensor(stmt);
         }

         if (isTableAvailable(conn, TABLE_DEVICE_SENSOR_VALUE) == false) {
            createTable_DeviceSensorValues(stmt);
         }
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * DB version 46 -> 47 ... MT version 22.?
    *
    * @param conn
    * @param splashManager
    * @return
    * @throws SQLException
    */
   private int updateDb_046_To_047(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 47;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (Statement stmt = conn.createStatement()) {

// SET_FORMATTING_OFF

            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_AVG_TEMPERATURE__FROM,        RENAMED__TOUR_AVG_TEMPERATURE__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_MAX_TEMPERATURE__FROM,        RENAMED__TOUR_MAX_TEMPERATURE__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_MIN_TEMPERATURE__FROM,        RENAMED__TOUR_MIN_TEMPERATURE__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_ISWEATHERDATAFROMAPI__FROM,   RENAMED__TOUR_ISWEATHERDATAFROMAPI__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_WEATHER_CLOUDS__FROM,         RENAMED__TOUR_WEATHER_CLOUDS__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_WEATHER_WIND_DIRECTION__FROM, RENAMED__TOUR_WEATHER_WIND_DIRECTION__INTO);
            SQL.RenameCol(stmt,     TABLE_TOUR_DATA,     RENAMED__TOUR_WEATHER_WIND_SPEED__FROM,     RENAMED__TOUR_WEATHER_WIND_SPEED__INTO);

            // Add new columns
            SQL.AddColumn_Float(stmt, TABLE_TOUR_DATA,   "weather_Temperature_Average", DEFAULT_0);               //$NON-NLS-1$
            SQL.AddColumn_Float(stmt, TABLE_TOUR_DATA,   "weather_Temperature_Max", DEFAULT_0);                   //$NON-NLS-1$
            SQL.AddColumn_Float(stmt, TABLE_TOUR_DATA,   "weather_Temperature_Min", DEFAULT_0);                   //$NON-NLS-1$
            SQL.AddColumn_Float(stmt, TABLE_TOUR_DATA,   "weather_Snowfall", DEFAULT_0);                   //$NON-NLS-1$

// SET_FORMATTING_ON
      }
      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   /**
    * If the previous average, max, min temperatures were retrieved by
    * a weather provider, they are copied into the new fields.
    * If necessary, the average, max, min temperatures measured from the device
    * are recomputed
    *
    * @param splashManager
    */
   private void updateDb_046_to_047_DataUpdate(final Connection conn, final SplashManager splashManager) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final int dbDataVersion = 47;

      if (getDbVersion(conn, TABLE_DB_VERSION_DATA) >= dbDataVersion) {
         // data version is higher -> nothing to do
         return;
      }

      long lastUpdateTime = startTime;

      int tourIndex = 1;
      int lastUpdateNumItems = 1;
      int sumUpdatedTours = 0;

      final List<Long> allTourIds = getAllTourIds();
      final int numAllTourIds = allTourIds.size();

      // If necessary, recomputing the temperature values (average/max/min) measured from the device
      for (final Long tourId : allTourIds) {

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

               splashManager.setMessage(NLS.bind(

                     // Data update 47: Converting weather data - {0} of {1} - {2} % - {3}
                     Messages.Tour_Database_PostUpdate_047_Weather,

                     new Object[] {
                           sumUpdatedTours,
                           numAllTourIds,
                           percentValue,
                           numTourDiff,
                     }));
            }

            tourIndex++;
         }

         updateDb_046_To_047_DataUpdate_Concurrent(tourId);
      }

      updateVersionNumber_20_AfterDataUpdate(conn, dbDataVersion, startTime);
   }

   /**
    * Do data updates concurrently with all available processor threads, this is reducing time
    * significantly.
    *
    * @param tourId
    */
   private void updateDb_046_To_047_DataUpdate_Concurrent(final Long tourId) {

      try {

         // put tour ID (queue item) into the queue AND wait when it is full

         _dbUpdateQueue.put(tourId);

      } catch (final InterruptedException e) {

         _isSQLDataUpdateError = true;

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _dbUpdateExecutor.submit(() -> {

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
             * Temperature Migration
             */
            if (tourData.temperatureSerie == null || tourData.isWeatherDataFromProvider()) {

               /**
                * If the device has NO temperature data or the weather was retrieved from WWO:
                * - copy the temperatures (DB 46) to the new non-device fields (DB 47)
                */
               tourData.setWeather_Temperature_Average(tourData.getWeather_Temperature_Average_Device());
               tourData.setWeather_Temperature_Max(tourData.getWeather_Temperature_Max_Device());
               tourData.setWeather_Temperature_Min(tourData.getWeather_Temperature_Min_Device());
            }

            /**
             * If the device has NO temperature data:
             * - set the device temperatures to 0
             */
            if (tourData.temperatureSerie == null) {

               tourData.setWeather_Temperature_Average_Device(0);
               tourData.setWeather_Temperature_Max_Device(0);
               tourData.setWeather_Temperature_Min_Device(0);

            } else {

               /**
                * If the device has temperature data:
                * - recalculate the device temperatures
                */
               tourData.computeAvg_Temperature();
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
   }

   private int updateDb_047_To_048(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 48;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         final String sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ALTER COLUMN   Weather SET DATA TYPE VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_WEATHER_V48 + ")"; //$NON-NLS-1$

         exec(stmt, sql);
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_048_To_049(final Connection connection,
                                   final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 49;

      logDbUpdate_Start(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = connection.createStatement();
      {
         SQL.AddColumn_VarCar(stmt, TABLE_TOUR_TAG, "imageFilePath", TourTag.DB_LENGTH_FILE_PATH); //$NON-NLS-1$
      }
      stmt.close();

      logDbUpdate_End(newDbVersion);

      return newDbVersion;
   }

   private int updateDb_049_To_050(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 50;

      logDbUpdate_Start(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
// SET_FORMATTING_OFF

         // Add new columns
         SQL.AddColumn_VarCar (stmt, TABLE_TOUR_DATA, "weather_AirQuality", TourData.DB_LENGTH_WEATHER_AIRQUALITY);       //$NON-NLS-1$

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

      _dbVersion_AfterDesignUpdate = newVersion;
   }

   private void updateVersionNumber_20_AfterDataUpdate(final Connection conn,
                                                       final int dbDataVersion,
                                                       final long startTime) throws SQLException {

      final String sql = "UPDATE " + TABLE_DB_VERSION_DATA + " SET VERSION=" + dbDataVersion; //$NON-NLS-1$ //$NON-NLS-2$

      conn.createStatement().executeUpdate(sql);

      logDbUpdate(createLog_DataUpdate(dbDataVersion, startTime));
   }

}
