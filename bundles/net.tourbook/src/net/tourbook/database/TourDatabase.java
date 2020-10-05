/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import net.tourbook.common.util.Util;
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
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.apache.derby.drda.NetworkServerControl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
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
    */
   private static final int TOURBOOK_DB_VERSION = 42;

//   private static final int TOURBOOK_DB_VERSION = 42; // 20.?
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
//   private static final int TOURBOOK_DB_VERSION = 8;  // 10.2.1 Mod by Kenny
//   private static final int TOURBOOK_DB_VERSION = 7;  // 9.01
//   private static final int TOURBOOK_DB_VERSION = 6;  // 8.12
//   private static final int TOURBOOK_DB_VERSION = 5;  // 8.11

//   private static final String SQL_STATE_XJ004_DATABASE_NOT_FOUND         = "XJ004";                                                 //$NON-NLS-1$

   public static boolean       IS_POST_UPDATE_019_to_020                  = false;

   private static final int    MAX_TRIES_TO_PING_SERVER                   = 10;

   /**
    * <b> !!! Table names are set to uppercase otherwise conn.getMetaData().getColumns() would not
    * work !!! </b>
    */
   public static final String  TABLE_SCHEMA                               = "USER";                                                  //$NON-NLS-1$

   private static final String TABLE_DB_VERSION                           = "DBVERSION";                                             //$NON-NLS-1$

   public static final String  TABLE_TOUR_BIKE                            = "TOURBIKE";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_COMPARED                        = "TOURCOMPARED";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_DATA                            = "TOURDATA";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_MARKER                          = "TOURMARKER";                                            //$NON-NLS-1$
   public static final String  TABLE_TOUR_PERSON                          = "TOURPERSON";                                            //$NON-NLS-1$
   public static final String  TABLE_TOUR_PERSON_HRZONE                   = "TOURPERSONHRZONE";                                      //$NON-NLS-1$
   public static final String  TABLE_TOUR_PHOTO                           = "TOURPHOTO";                                             //$NON-NLS-1$
   public static final String  TABLE_TOUR_REFERENCE                       = "TOURREFERENCE";                                         //$NON-NLS-1$
   public static final String  TABLE_TOUR_TAG                             = "TOURTAG";                                               //$NON-NLS-1$
   public static final String  TABLE_TOUR_TAG_CATEGORY                    = "TOURTAGCATEGORY";                                       //$NON-NLS-1$
   public static final String  TABLE_TOUR_TYPE                            = "TOURTYPE";                                              //$NON-NLS-1$
   public static final String  TABLE_TOUR_WAYPOINT                        = "TOURWAYPOINT";                                          //$NON-NLS-1$
   public static final String  TABLE_TOUR_GEO_PARTS                       = "TourGeoParts";                                          //$NON-NLS-1$

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

   // never used tables, is needed to drop them
   private final static String                            TABLE_TOUR_CATEGORY             = "TourCategory";                                         //$NON-NLS-1$
   private final static String                            TABLE_TOURCATEGORY__TOURDATA    = TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA;            //$NON-NLS-1$

   /**
    * Is <code>-1</code>, this is the id for a not saved entity
    */
   public static final int                                ENTITY_IS_NOT_SAVED             = -1;
   //
   public static final String                             ENTITY_ID_BIKE                  = "BikeID";                                               //$NON-NLS-1$
   public static final String                             ENTITY_ID_COMPARED              = "ComparedID";                                           //$NON-NLS-1$
   public static final String                             ENTITY_ID_HR_ZONE               = "HrZoneID";                                             //$NON-NLS-1$
   public static final String                             ENTITY_ID_MARKER                = "MarkerID";                                             //$NON-NLS-1$
   public static final String                             ENTITY_ID_PERSON                = "PersonID";                                             //$NON-NLS-1$
   public static final String                             ENTITY_ID_PHOTO                 = "PhotoID";                                              //$NON-NLS-1$
   public static final String                             ENTITY_ID_REF                   = "RefID";                                                //$NON-NLS-1$
   public static final String                             ENTITY_ID_TAG                   = "TagID";                                                //$NON-NLS-1$
   public static final String                             ENTITY_ID_TAG_CATEGORY          = "TagCategoryID";                                        //$NON-NLS-1$
   public static final String                             ENTITY_ID_TOUR                  = "TourID";                                               //$NON-NLS-1$
   public static final String                             ENTITY_ID_TYPE                  = "TypeID";                                               //$NON-NLS-1$
   public static final String                             ENTITY_ID_WAY_POINT             = "WayPointID";                                           //$NON-NLS-1$
   //
   private static final String                            KEY_BIKE                        = TABLE_TOUR_BIKE + "_" + ENTITY_ID_BIKE;                 //$NON-NLS-1$
   private static final String                            KEY_PERSON                      = TABLE_TOUR_PERSON + "_" + ENTITY_ID_PERSON;             //$NON-NLS-1$
   public static final String                             KEY_TAG                         = TABLE_TOUR_TAG + "_" + ENTITY_ID_TAG;                   //$NON-NLS-1$
   private static final String                            KEY_TAG_CATEGORY                = TABLE_TOUR_TAG_CATEGORY + "_" + ENTITY_ID_TAG_CATEGORY; //$NON-NLS-1$
   public static final String                             KEY_TOUR                        = TABLE_TOUR_DATA + "_" + ENTITY_ID_TOUR;                 //$NON-NLS-1$
   private static final String                            KEY_TYPE                        = TABLE_TOUR_TYPE + "_" + ENTITY_ID_TYPE;                 //$NON-NLS-1$

   private static final String                            DEFAULT_0                       = "0";                                                    //$NON-NLS-1$
   private static final String                            DEFAULT_1_0                     = "1.0";                                                  //$NON-NLS-1$
   private static final String                            DEFAULT_FALSE                   = "false";                                                //$NON-NLS-1$
   private static final String                            DEFAULT_IGNORED                 = "-1";                                                   //$NON-NLS-1$

   private static final String                            PERSISTENCE_UNIT_NAME           = "tourdatabase";                                         //$NON-NLS-1$

   private static final String                            DERBY_DATABASE                  = "derby-database";                                       //$NON-NLS-1$
   private static final String                            DERBY_DB_TOURBOOK               = "tourbook";                                             //$NON-NLS-1$
   private static String                                  DERBY_DRIVER_CLASS;
   private static String                                  DERBY_URL;
   private static final String                            DERBY_URL_COMMAND_CREATE_TRUE   = ";create=true";                                         //$NON-NLS-1$
   private static final String                            DERBY_URL_COMMAND_SHUTDOWN_TRUE = ";shutdown=true";                                       //$NON-NLS-1$
   private static final String                            DERBY_URL_COMMAND_UPGRADE_TRUE  = ";upgrade=true";                                        //$NON-NLS-1$
   //
   private static volatile TourDatabase                   _instance;

   private static ArrayList<TourType>                     _activeTourTypes;

   private static volatile ArrayList<TourType>            _dbTourTypes;

   /**
    * Key is tour type ID
    */
   private static HashMap<Long, TourType>                 _dbTourTypeIds;

   /**
    * Key is tag ID.
    */
   private static volatile HashMap<Long, TourTag>         _allTourTags;

   /**
    * Key is tag category ID.
    */
   private static volatile HashMap<Long, TourTagCategory> _allTourTagCategories;

   /**
    * Key is category ID or <code>-1</code> for the root.
    */
   private static HashMap<Long, TagCollection>            _tagCollections                 = new HashMap<>();

   /*
    * Cached distinct fields
    */
   private static TreeSet<String>                _dbTourTitles;
   private static TreeSet<String>                _dbTourStartPlace;
   private static TreeSet<String>                _dbTourEndPlace;
   private static TreeSet<String>                _dbTourMarkerNames;

   private static final IPreferenceStore         _prefStore    = TourbookPlugin.getPrefStore();

   private final static String                   _databasePath = Platform.getInstanceLocation().getURL().getPath() + DERBY_DATABASE;

   private static NetworkServerControl           _server;

   private static volatile EntityManagerFactory  _emFactory;
   private static volatile ComboPooledDataSource _pooledDataSource;

   static {

      // set storage location for the database
      System.setProperty("derby.system.home", _databasePath); //$NON-NLS-1$

// set derby debug properties
//      System.setProperty("derby.language.logStatementText", "true");
//      System.setProperty("derby.language.logQueryPlan", "true");
   }

   private static final Object DB_LOCK = new Object();

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

   private boolean                               _isDbInitialized;
   private boolean                               _isTableChecked;
   private boolean                               _isVersionChecked;

   private final ListenerList<IPropertyListener> _propertyListeners = new ListenerList<>(ListenerList.IDENTITY);

   private boolean                               _isSQLUpdateError  = false;

   /**
    * Database version before a db update is performed
    */
   private int                                   _dbVersionBeforeUpdate;
   private int                                   _dbVersionAfterUpdate;

   private boolean                               _isDerbyEmbedded;
   private boolean                               _isChecked_DbUpgraded_Before;
   private boolean                               _isChecked_DbUpgraded_After;
   private boolean                               _isChecked_DbCreated;

   /**
    * SQL utilities.
    */
   private static class SQL {

      private static void AddCol_BigInt(final Statement stmt,
                                        final String table,
                                        final String columnName,
                                        final String defaultValue) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //                                                //$NON-NLS-1$
               + "   ADD COLUMN   " + columnName + " BIGINT DEFAULT " + defaultValue; //   //$NON-NLS-1$ //$NON-NLS-2$

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
      private static void AddCol_Boolean(final Statement stmt,
                                         final String table,
                                         final String columnName,
                                         final String defaultValue) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //                                                //$NON-NLS-1$
               + "   ADD COLUMN " + columnName + " BOOLEAN DEFAULT " + defaultValue; //   //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      private static void AddCol_Double(final Statement stmt,
                                        final String table,
                                        final String columnName,
                                        final String defaultValue) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //                                                //$NON-NLS-1$
               + "   ADD COLUMN   " + columnName + " DOUBLE DEFAULT " + defaultValue; //   //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      private static void AddCol_Float(final Statement stmt,
                                       final String table,
                                       final String columnName,
                                       final String defaultValue) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //                                       //$NON-NLS-1$
               + "   ADD COLUMN   " + columnName + " FLOAT DEFAULT  " + defaultValue; //      //$NON-NLS-1$ //$NON-NLS-2$

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
      private static void AddCol_Int(final Statement stmt,
                                     final String table,
                                     final String columnName,
                                     final String defaultValue) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //                                       //$NON-NLS-1$
               + "   ADD COLUMN   " + columnName + " INTEGER DEFAULT " + defaultValue; //      //$NON-NLS-1$ //$NON-NLS-2$

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
      private static void AddCol_SmallInt(final Statement stmt,
                                          final String table,
                                          final String columnName,
                                          final String defaultValue) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //                                       //$NON-NLS-1$
               + "   ADD COLUMN   " + columnName + " SMALLINT DEFAULT " + defaultValue; //      //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      /**
       * Creates a SQL statement to add a column for VARCHAR.
       *
       * @param stmt
       * @param table
       * @param columnName
       * @param columnWidth
       * @return Returns this sql statement:
       *         <p>
       *         <code>
       *         "ALTER TABLE " + table + " ADD COLUMN   " + columnName + " " + " VARCHAR(" + columnWidth + ")\n"
       *         </code>
       * @throws SQLException
       */
      private static void AddCol_VarCar(final Statement stmt,
                                        final String table,
                                        final String columnName,
                                        final int columnWidth) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table //$NON-NLS-1$
               + "   ADD COLUMN   " + columnName + "   VARCHAR(" + columnWidth + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

         exec(stmt, sql);
      }

      private static void AlterCol_VarChar_Width(final Statement stmt,
                                                 final String table,
                                                 final String field,
                                                 final int newWidth) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + "ALTER TABLE " + table + "\n" //$NON-NLS-1$ //$NON-NLS-2$
               + "   ALTER COLUMN " + field + "\n" //$NON-NLS-1$ //$NON-NLS-2$
               + "   SET DATA TYPE   VARCHAR(" + newWidth + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      private static void Cleanup_DropConstraint(final Statement stmt,
                                                 final String tableName,
                                                 final String constraintName) throws SQLException {

         try {

            exec(stmt, "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName); //$NON-NLS-1$ //$NON-NLS-2$

         } catch (final SQLException e) {

            final String sqlState = e.getSQLState();
            if (sqlState.equals("42X86")) { //$NON-NLS-1$

               // Caused by: ERROR 42X86: ALTER TABLE failed. There is no constraint 'USER.FK_TOURDATA_TOURTAG_TOURTAG_TAGID' on table '"USER"."TOURDATA_TOURTAG"'.

               /*
                * Ignore not existing constraints
                */
               StatusUtil.log(e);

            } else {
               throw e;
            }
         }
      }

      private static void Cleanup_DropTable(final Statement stmt, final String tableName) throws SQLException {

         try {

            exec(stmt, "DROP TABLE " + tableName); //$NON-NLS-1$

         } catch (final SQLException e) {

            final String sqlState = e.getSQLState();
            if (sqlState.equals("42Y55")) { //$NON-NLS-1$

               // Caused by: ERROR 42Y55: 'DROP TABLE' cannot be performed on 'TOURCATEGORY' because it does not exist.

               /*
                * This case occurred because table TOURCATEGORY was created until version 1.6 but do
                * not exist in later versions.
                */
               StatusUtil.log(e);

            } else {
               throw e;
            }
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

         return "   " //$NON-NLS-1$
               + fieldName + " BIGINT NOT NULL " //$NON-NLS-1$
               + generateID
               + " CONSTRAINT " + fieldName + "_pk PRIMARY KEY" //$NON-NLS-1$ //$NON-NLS-2$
               + ",\n";//$NON-NLS-1$
      }

      /**
       * @param stmt
       * @param tableName
       * @param indexAndColumnName
       * @throws SQLException
       */
      private static void CreateIndex(final Statement stmt, final String tableName, final String indexAndColumnName)
            throws SQLException {

         final String sql = "CREATE INDEX " + indexAndColumnName //$NON-NLS-1$
               + " ON " + tableName //$NON-NLS-1$
               + " (" + indexAndColumnName + ")"; //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      /**
       * @param stmt
       * @param table
       * @param columnName
       * @param newColumnName
       * @throws SQLException
       */
      private static void RenameCol(final Statement stmt,
                                        final String table,
                                        final String columnName,
                                        final String newColumnName) throws SQLException {

         final String sql = UI.EMPTY_STRING//
               + " RENAME COLUMN " + table + "." + columnName + " TO " + newColumnName; //   //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
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
    * @param tourData
    * @return Returns <code>true</code> when new tags are created.
    */
   private static boolean check_Tags(final TourData tourData) {

      final Set<TourTag> tourTags = tourData.getTourTags();

      if (tourTags.size() == 0) {
         return false;
      }

      final ArrayList<TourTag> oldTags = new ArrayList<>();
      final ArrayList<TourTag> newTags = new ArrayList<>();

      HashMap<Long, TourTag> allDbTags = TourDatabase.getAllTourTags();

      for (final TourTag tourTag : tourTags) {

         if (tourTag.getTagId() != TourDatabase.ENTITY_IS_NOT_SAVED) {
            // tag is saved
            continue;
         }

         // tag is not yet saved
         // 1. tag can still be new
         // 2. tag is already created but not updated in the not yet saved tour

         final TourTag dbTag = findTourTag(tourTag.getTagName(), allDbTags.values());

         if (dbTag != null) {

            // use found tag

            oldTags.add(tourTag);
            newTags.add(dbTag);

         } else {

            // create new tag

            final TourTag savedTag = TourDatabase.saveEntity(//
                  tourTag,
                  TourDatabase.ENTITY_IS_NOT_SAVED,
                  TourTag.class);

            if (savedTag != null) {

               oldTags.add(tourTag);
               newTags.add(savedTag);

               // reload db tags
               TourDatabase.clearTourTags();
               allDbTags = TourDatabase.getAllTourTags();
            }
         }
      }

      final boolean isNewTags = newTags.size() > 0;

      if (isNewTags) {

         // replace tags in the tour

         tourTags.removeAll(oldTags);
         tourTags.addAll(newTags);
      }

      return isNewTags;
   }

   /**
    * @param tourData
    * @return Returns <code>true</code> when a new tour type is created.
    */
   private static boolean check_TourType(final TourData tourData) {

      final TourType tourType = tourData.getTourType();

      if (tourType == null) {
         return false;
      }

      if (tourType.getTypeId() != TourDatabase.ENTITY_IS_NOT_SAVED) {
         // type is saved
         return false;
      }

      TourType newType = null;

      final Collection<TourType> allDbTypes = TourDatabase.getAllTourTypes();

      // type is not yet saved
      // 1. type can still be new
      // 2. type is already created but not updated in the not yet saved tour

      final TourType dbType = findTourType(tourType.getName(), allDbTypes);

      if (dbType != null) {

         // use found tag

         newType = dbType;

      } else {

         // create new tag

         final TourType savedType = TourDatabase.saveEntity(//
               tourType,
               TourDatabase.ENTITY_IS_NOT_SAVED,
               TourTag.class);

         if (savedType != null) {

            newType = savedType;

            // force reload of the db tour types
            TourDatabase.clearTourTypes();
            TourManager.getInstance().clearTourDataCache();
         }
      }

      final boolean isNewTourType = newType != null;

      if (isNewTourType) {

         // replace tour type in the tour

         tourData.setTourType(newType);
      }

      return isNewTourType;
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

      final boolean isNewTag = check_Tags(tourData);
      final boolean isNewTourType = check_TourType(tourData);

      if (isNewTag) {

         // fire modify event

         Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
               TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
            }
         });
      }

      if (isNewTourType) {

         // fire modify event

         Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
               TourbookPlugin.getPrefStore()
                     .setValue(
                           ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED,
                           Math.random());
            }
         });
      }
   }

   /**
    * Removes all tour tags which are loaded from the database so the next time they will be
    * reloaded.
    */
   public static synchronized void clearTourTags() {

      if (_allTourTags != null) {
         _allTourTags.clear();
         _allTourTags = null;
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
   public static synchronized void clearTourTypes() {

      if (_dbTourTypes != null) {

         _dbTourTypes.clear();
         _dbTourTypeIds.clear();

         _dbTourTypes = null;
         _dbTourTypeIds = null;
      }

      TourTypeImage.setTourTypeImagesDirty();
   }

   public static void closeConnection(final Connection conn) {

      if (conn != null) {
         try {
            conn.close();
         } catch (final SQLException e) {
            UI.showSQLException(e);
         }
      }
   }

   /**
    * @return
    */
   /**
    * @param runner
    *           {@link IComputeTourValues} interface to compute values for one tour
    * @param tourIds
    *           Tour ID's which should be computed, when <code>null</code>, ALL tours will be
    *           computed.
    * @return
    */
   public static boolean computeAnyValues_ForAllTours(final IComputeTourValues runner, final ArrayList<Long> tourIds) {

      final int[] tourCounter = new int[] { 0 };
      final int[] tourListSize = new int[] { 0 };
      final boolean[] isCanceled = new boolean[] { false };

      /*
       * Runnable to compute values
       */
      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            ArrayList<Long> tourList;
            if (tourIds == null) {
               tourList = getAllTourIds();
            } else {
               tourList = tourIds;
            }
            tourListSize[0] = tourList.size();

            long lastUIUpdateTime = 0;

            monitor.beginTask(Messages.tour_database_computeComputeValues_mainTask, tourList.size());

            // loop over all tours and compute values
            for (final Long tourId : tourList) {

               final TourData dbTourData = getTourFromDb(tourId);
               TourData savedTourData = null;

               if (dbTourData != null) {
                  if (runner.computeTourValues(dbTourData)) {

                     // ensure that all computed values are set
                     dbTourData.computeComputedValues();

                     savedTourData = saveTour(dbTourData, false);
                  }
               }

               tourCounter[0]++;

               /*
                * This must be called in every iteration because it can compute values ! ! !
                */
               final String runnerSubTaskText = runner.getSubTaskText(savedTourData);

               final long currentTime = System.currentTimeMillis();
               if (currentTime > lastUIUpdateTime + 200) {

                  lastUIUpdateTime = currentTime;

                  // create sub task text
                  final StringBuilder sb = new StringBuilder();
                  sb.append(NLS.bind(Messages.tour_database_computeComputeValues_subTask,
                        new Object[] { tourCounter[0], tourListSize[0], }));

                  sb.append(UI.DASH_WITH_DOUBLE_SPACE);
                  sb.append(tourCounter[0] * 100 / tourListSize[0]);
                  sb.append(UI.SYMBOL_PERCENTAGE);

                  if (runnerSubTaskText != null) {
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
         sb.append(
               NLS.bind(
                     Messages.tour_database_computeComputedValues_resultMessage,
                     tourCounter[0],
                     tourListSize[0]));

         final String runnerResultText = runner.getResultText();
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

      final ArrayList<Long> tourList = getAllTourIds();

      // loop: all tours, compute computed fields and save the tour
      int tourCounter = 1;
      for (final Long tourId : tourList) {

         if (splashManager != null) {
            splashManager.setMessage(
                  NLS.bind(
                        Messages.Tour_Database_update_tour, //
                        new Object[] { tourCounter++, tourList.size() }));
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
   public static boolean computeNoDataserieValues_ForAllTours(final IComputeNoDataserieValues tourRunner, final ArrayList<Long> tourIds) {

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

            ArrayList<Long> allTourIds;
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

         FTSearchManager.deleteFromIndex(tourId);

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

         final String allSql[] = {

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

      StatusUtil.logInfo(sql);
      System.out.println();

      stmt.execute(sql);
   }

   private static void exec(final Statement stmt, final String[] sqlStatements) throws SQLException {

      for (final String sql : sqlStatements) {
         exec(stmt, sql);
      }
   }

   private static void execUpdate(final Statement stmt, final String sql) throws SQLException {

      StatusUtil.logInfo(sql);
      System.out.println();

      stmt.executeUpdate(sql);
   }

   /**
    * Find tag by name.
    *
    * @param tagName
    * @param allTags
    * @return Returns found {@link TourTag} or <code>null</code> when not available.
    */
   public static TourTag findTourTag(final String tagName, final Collection<TourTag> allTags) {

      for (final TourTag tourTag : allTags) {

         if (tourTag.getTagName().equalsIgnoreCase(tagName)) {

            // existing tag is found

            return tourTag;
         }
      }

      return null;
   }

   /**
    * Find tour type in other tour types.
    *
    * @param tourTypeName
    * @param allDbTypes
    * @return Returns found {@link TourType} or <code>null</code> when not available.
    */
   public static TourType findTourType(final String tourTypeName, final Collection<TourType> allDbTypes) {

      for (final TourType tourType : allDbTypes) {

         if (tourTypeName.equalsIgnoreCase(tourType.getName())) {

            // existing type is found

            return tourType;
         }
      }

      return null;
   }

   /**
    * @param tourTypeList
    * @return Returns a list with all {@link TourType}'s which are currently used (with filter) to
    *         display tours.<br>
    *         Returns <code>null</code> when {@link TourType}'s are not defined.<br>
    *         Return an empty list when the {@link TourType} is not set within the {@link TourData}
    */
   public static ArrayList<TourType> getActiveTourTypes() {
      return _activeTourTypes;
   }

   private static ArrayList<Long> getAllTourIds() {

      final ArrayList<Long> tourIds = new ArrayList<>();

      try (Connection conn = getInstance().getConnection(); //
            Statement stmt = conn.createStatement()) {

         final ResultSet result = stmt.executeQuery("SELECT tourId FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

         while (result.next()) {
            tourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return tourIds;
   }

   public static TreeSet<String> getAllTourMarkerNames() {

      if (_dbTourMarkerNames == null) {
         _dbTourMarkerNames = getDistinctValues(TourDatabase.TABLE_TOUR_MARKER, "label"); //$NON-NLS-1$
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
   public static TreeSet<String> getAllTourPlaceEnds() {

      if (_dbTourEndPlace == null) {
         _dbTourEndPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourEndPlace"); //$NON-NLS-1$
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
   public static TreeSet<String> getAllTourPlaceStarts() {

      if (_dbTourStartPlace == null) {
         _dbTourStartPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourStartPlace"); //$NON-NLS-1$
      }

      return _dbTourStartPlace;
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

            final Query emQuery = em.createQuery(
                  UI.EMPTY_STRING
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
    * This method is synchronized to conform to FindBugs
    *
    * @return Returns all tour tags which are stored in the database, the hash key is the tag id
    */
   public static HashMap<Long, TourTag> getAllTourTags() {

      if (_allTourTags != null) {
         return _allTourTags;
      }

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allTourTags != null) {
            return _allTourTags;
         }

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(
                  UI.EMPTY_STRING
                        + "SELECT tourTag" //$NON-NLS-1$
                        + " FROM " + TourTag.class.getSimpleName() + " AS tourTag"); //$NON-NLS-1$ //$NON-NLS-2$

            _allTourTags = new HashMap<>();

            final List<?> resultList = emQuery.getResultList();
            for (final Object result : resultList) {

               if (result instanceof TourTag) {
                  final TourTag tourTag = (TourTag) result;
                  _allTourTags.put(tourTag.getTagId(), tourTag);
               }
            }

            em.close();
         }
      }

      return _allTourTags;
   }

   /**
    * Getting all tour titles from the database sorted by alphabet and without any double entries.
    *
    * @author Stefan F.
    * @return titles as string array.
    */
   public static TreeSet<String> getAllTourTitles() {

      if (_dbTourTitles == null) {
         _dbTourTitles = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourTitle"); //$NON-NLS-1$
      }

      return _dbTourTitles;
   }

   /**
    * @return Returns the backend of all tour types which are stored in the database sorted by name.
    */
   @SuppressWarnings("unchecked")
   public static ArrayList<TourType> getAllTourTypes() {

      if (_dbTourTypes != null) {
         return _dbTourTypes;
      }

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_dbTourTypes != null) {
            return _dbTourTypes;
         }

         // create empty list
         _dbTourTypes = new ArrayList<>();
         _dbTourTypeIds = new HashMap<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query emQuery = em.createQuery(//
                  //
                  "SELECT tourType" //$NON-NLS-1$
                        + " FROM TourType AS tourType" //$NON-NLS-1$
                        + " ORDER  BY tourType.name"); //$NON-NLS-1$

            _dbTourTypes = (ArrayList<TourType>) emQuery.getResultList();

            for (final TourType tourType : _dbTourTypes) {
               _dbTourTypeIds.put(tourType.getTypeId(), tourType);
            }

            em.close();
         }
      }

      return _dbTourTypes;
   }

   public static String getDatabasePath() {
      return _databasePath;
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
   private static TreeSet<String> getDistinctValues(final String db, final String fieldname) {

      final TreeSet<String> sortedValues = new TreeSet<>(new Comparator<String>() {
         @Override
         public int compare(final String s1, final String s2) {
            // sort without case
            return s1.compareToIgnoreCase(s2);
         }
      });

      /*
       * run in UI thread otherwise the busyindicator fails
       */
      final Display display = Display.getDefault();

      display.syncExec(new Runnable() {
         @Override
         public void run() {

            BusyIndicator.showWhile(display, new Runnable() {
               @Override
               public void run() {

                  try (Connection conn = getInstance().getConnection(); //
                        Statement stmt = conn.createStatement()) {

                     final String sqlQuery = UI.EMPTY_STRING //
                           + "SELECT" //$NON-NLS-1$
                           + " DISTINCT" //$NON-NLS-1$
                           + " " + fieldname //$NON-NLS-1$
                           + " FROM " + db //$NON-NLS-1$
                           + " ORDER BY " + fieldname; //$NON-NLS-1$

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
               }
            });
         }
      });

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
      Query emQuery = em.createQuery(//
            //
            "SELECT ttCategory" //$NON-NLS-1$
                  + " FROM " + TourTagCategory.class.getSimpleName() + " AS ttCategory" //$NON-NLS-1$ //$NON-NLS-2$
                  + " WHERE ttCategory.isRoot=1" //$NON-NLS-1$
                  + " ORDER BY ttCategory.name"); //$NON-NLS-1$

      rootEntry.tourTagCategories = (ArrayList<TourTagCategory>) emQuery.getResultList();

      /*
       * read tour tags from db
       */
      emQuery = em.createQuery(//
            //
            "SELECT tourTag" //$NON-NLS-1$
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
            throw new MyTourbookException("tag category id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
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
   public static String getTagNames(final ArrayList<Long> tagIds) {

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

      if (tourTags.size() == 0) {
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

      if (tourTags.size() == 0) {
         return UI.EMPTY_STRING;
      }

      final ArrayList<String> tagNames = new ArrayList<>();

      // get tag name for each tag id
      for (final TourTag tag : tourTags) {
         tagNames.add(tag.getTagName());
      }

      return getTagNamesText(tagNames, isVertical);
   }

   public static String getTagNamesText(final ArrayList<String> tagNames, final boolean isVertical) {

      // sort tags by name
      Collections.sort(tagNames);

      // convert list into visible string
      int tagIndex = 0;
      final StringBuilder sb = new StringBuilder();

      for (final String tagName : tagNames) {

         if (tagIndex++ > 0) {
            if (isVertical) {
               sb.append(UI.NEW_LINE);
            } else {
               sb.append(UI.COMMA_SPACE);
            }
         }

         if (isVertical) {
            // prefix a bullet
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
         final TourTag tourTag = _allTourTags.get(tagId);
         tagNames.add(tourTag.getTagName());
      }

      return getTagNamesText(tagNames, isVertical);
   }

   /**
    * @param tagId
    * @return Returns the tag notes
    *         <code>null</code>
    */
   public static String getTagNotes(final Long tagId) {

      if (tagId == null) {
         return UI.EMPTY_STRING;
      }

      final HashMap<Long, TourTag> hashAllTags = getAllTourTags();
      final TourTag tag = hashAllTags.get(tagId);

      if (tag != null) {
         return tag.getNotes();
      } else {
         try {
            throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
         } catch (final MyTourbookException e) {
            e.printStackTrace();
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

         final Query emQuery = em.createQuery(//
               //
               "SELECT tourBike" //$NON-NLS-1$
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
    * @return Returns the name for the {@link TourType} or an empty string when the tour type id was
    *         not found
    */
   public static String getTourTypeName(final long tourTypeId) {

      // ensure tour types are loaded
      getAllTourTypes();

      final TourType tourType = _dbTourTypeIds.get(tourTypeId);
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

         Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {

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
                     NLS.bind(
                           Messages.Tour_Database_Dialog_ValidateFields_Message, //
                           new Object[] { uiFieldName, field.length(), maxLength }))) {

                  returnValue[0] = FIELD_VALIDATION.TRUNCATE;
               } else {
                  returnValue[0] = FIELD_VALIDATION.IS_INVALID;
               }
            }
         });
      }

      return returnValue[0];
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
               Display.getCurrent().getActiveShell(), //
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
   public static <T> T saveEntity(final T entity, final long id, final Class<T> entityClass, final EntityManager em) {

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
               Display.getCurrent().getActiveShell(), //
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

      /*
       * prevent saving a tour which was deleted before
       */
      if (tourData.isTourDeleted) {
         return null;
      }

      /*
       * History tour or multiple tours cannot be saved
       */
      if (tourData.isHistoryTour || tourData.isMultipleTours()) {
         return null;
      }

      /*
       * prevent saving a tour when a person is not set, this check is for internal use that all
       * data are valid
       */
      if (tourData.getTourPerson() == null) {
         StatusUtil.log("Cannot save a tour without a person: " + tourData); //$NON-NLS-1$
         return null;
      }

      /*
       * check size of varcar fields
       */
      if (tourData.isValidForSave() == false) {
         return null;
      }

      /*
       * Removed cached data
       */
      TourManager.clearMultipleTourData();

      /**
       * ensure HR zones are computed, it requires that a person is set which is not the case when a
       * device importer calls the method {@link TourData#computeComputedValues()}
       */
      tourData.getNumberOfHrZones();

      final long dtSaved = TimeTools.createdNowAsYMDhms();

      checkUnsavedTransientInstances(tourData);

      EntityManager em = TourDatabase.getInstance().getEntityManager();

      TourData persistedEntity = null;

      if (em != null) {

         final EntityTransaction ts = em.getTransaction();

         try {

            tourData.onPrePersist();

            ts.begin();
            {
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

         TourManager.getInstance().updateTourInCache(persistedEntity);

         updateCachedFields(persistedEntity);

         saveTour_GeoParts(persistedEntity);

         // update ft index
         final ArrayList<TourData> allTours = new ArrayList<>();
         allTours.add(persistedEntity);
         FTSearchManager.updateIndex(allTours);
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
//  remove SYSTEM.OUT.PRINTLN
   }

   public static void updateActiveTourTypeList(final TourTypeFilter tourTypeFilter) {

      switch (tourTypeFilter.getFilterType()) {
      case TourTypeFilter.FILTER_TYPE_SYSTEM:

         if (tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {

            // all tour types are selected

            _activeTourTypes = _dbTourTypes;
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

      // cache tour title
      final TreeSet<String> allTitles = getAllTourTitles();
      final String tourTitle = tourData.getTourTitle();
      if (tourTitle.length() > 0) {
         allTitles.add(tourTitle);
      }

      // cache tour start place
      final TreeSet<String> allPlaceStarts = getAllTourPlaceStarts();
      final String tourStartPlace = tourData.getTourStartPlace();
      if (tourStartPlace.length() > 0) {
         allPlaceStarts.add(tourStartPlace);
      }

      // cache tour end place
      final TreeSet<String> allPlaceEnds = getAllTourPlaceEnds();
      final String tourEndPlace = tourData.getTourEndPlace();
      if (tourEndPlace.length() > 0) {
         allPlaceEnds.add(tourEndPlace);
      }

      // cache tour marker names
      final TreeSet<String> allMarkerNames = getAllTourMarkerNames();
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
            + " StartYear," //            // 1  //$NON-NLS-1$
            + " StartMonth," //           // 2  //$NON-NLS-1$
            + " StartDay" //              // 3  //$NON-NLS-1$
            + " FROM " + TABLE_TOUR_DATA //      //$NON-NLS-1$
            + " WHERE TourId=?"); //             //$NON-NLS-1$

      final PreparedStatement stmtUpdate = conn.prepareStatement(UI.EMPTY_STRING

            + "UPDATE " + TABLE_TOUR_DATA //    //$NON-NLS-1$
            + " SET" //                         //$NON-NLS-1$
            + " startWeek=?, " //               //$NON-NLS-1$
            + " startWeekYear=? " //            //$NON-NLS-1$
            + " WHERE tourId=?"); //            //$NON-NLS-1$

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

//   /**
//    * Create index for {@link TourData}. *
//    * <p>
//    *
//    * @param stmt
//    * @throws SQLException
//    * @since Db version 35
//    */
//   private void createIndex_TourData_035(final Statement stmt) throws SQLException {
//
//      String sql;
//
//      sql = "CREATE INDEX LatitudeMinE6 ON " + TABLE_TOUR_DATA + " (LatitudeMinE6)"; //$NON-NLS-1$ //$NON-NLS-2$
//      exec(stmt, sql);
//
//      sql = "CREATE INDEX LatitudeMaxE6 ON " + TABLE_TOUR_DATA + " (LatitudeMaxE6)"; //$NON-NLS-1$ //$NON-NLS-2$
//      exec(stmt, sql);
//
//      sql = "CREATE INDEX LongitudeMinE6 ON " + TABLE_TOUR_DATA + " (LongitudeMinE6)"; //$NON-NLS-1$ //$NON-NLS-2$
//      exec(stmt, sql);
//
//      sql = "CREATE INDEX LongitudeMaxE6 ON " + TABLE_TOUR_DATA + " (LongitudeMaxE6)"; //$NON-NLS-1$ //$NON-NLS-2$
//      exec(stmt, sql);
//   }

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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_BIKE + "   (                             \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_BIKE, true)
            //
            + "   Name           VARCHAR(" + TourBike.DB_LENGTH_NAME + "),             \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   Weight         FLOAT,                                                \n" //$NON-NLS-1$ // kg
            + "   TypeId         INTEGER,                                              \n" //$NON-NLS-1$
            + "   FrontTyreId    INTEGER,                                              \n" //$NON-NLS-1$
            + "   RearTyreId     INTEGER                                               \n" //$NON-NLS-1$
            //
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_COMPARED + "   (                         \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_COMPARED, true)
            //
            + "   RefTourId          BIGINT,                                           \n" //$NON-NLS-1$
            + "   TourId             BIGINT,                                           \n" //$NON-NLS-1$
            //
            + "   StartIndex         INTEGER NOT NULL,                                 \n" //$NON-NLS-1$
            + "   EndIndex           INTEGER NOT NULL,                                 \n" //$NON-NLS-1$
            + "   TourDate           DATE NOT NULL,                                    \n" //$NON-NLS-1$
            + "   StartYear          INTEGER NOT NULL,                                 \n" //$NON-NLS-1$
            + "   TourSpeed          FLOAT,                                            \n" //$NON-NLS-1$
            //
            // version 28 start
            //
            + "   AvgPulse           FLOAT,                                            \n" //$NON-NLS-1$
            //
            // version 28 end ---------
            //
            // version 40 start
            //
            + "   tourDeviceTime_Elapsed  INTEGER DEFAULT 0                                 \n" //$NON-NLS-1$
            //
            // version 40 end ---------
            //
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_DATA + "   (                             \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TOUR, false)
            //
            + " StartYear           SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartMonth          SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartDay            SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartHour           SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartMinute         SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartWeek           SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartDistance       INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " Distance            INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " StartAltitude       SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " StartPulse          SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " DpTolerance         SMALLINT NOT NULL,                                 \n" //$NON-NLS-1$
            + " TourDistance        INTEGER NOT NULL,                                  \n" //$NON-NLS-1$

            // replaced with BIGINT values in version 22
            //            + " tourRecordingTime    INTEGER NOT NULL,                    \n" //$NON-NLS-1$
            //            + " tourDrivingTime      INTEGER NOT NULL,                    \n" //$NON-NLS-1$

            + " tourAltUp           INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " tourAltDown         INTEGER NOT NULL,                                  \n" //$NON-NLS-1$

            + " deviceTourType      VARCHAR(" + TourData.DB_LENGTH_DEVICE_TOUR_TYPE + "),         \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " devicePluginId      VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_ID + "),         \n" //$NON-NLS-1$ //$NON-NLS-2$

            + " deviceTravelTime    BIGINT NOT NULL,                                   \n" //$NON-NLS-1$
            + " deviceDistance      INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " deviceWheel         INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " deviceWeight        INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " deviceTotalUp       INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + " deviceTotalDown     INTEGER NOT NULL,                                  \n" //$NON-NLS-1$

            // version 3 start
            + " deviceMode          SMALLINT,                                          \n" //$NON-NLS-1$
            + " deviceTimeInterval  SMALLINT,                                          \n" //$NON-NLS-1$
            // version 3 end

            // version 4 start

            // from markus
            // replaced with FLOAT values in version 21
            //            + " maxAltitude            INTEGER,                           \n" //$NON-NLS-1$
            //            + " maxPulse               INTEGER,                           \n" //$NON-NLS-1$
            //            + " avgPulse               INTEGER,                           \n" //$NON-NLS-1$
            //            + " avgCadence             INTEGER,                           \n" //$NON-NLS-1$
            //            + " avgTemperature         INTEGER,                           \n" //$NON-NLS-1$

            + " maxSpeed            FLOAT,                                                   \n" //$NON-NLS-1$
            + " tourTitle           VARCHAR(" + TourData.DB_LENGTH_TOUR_TITLE + "),          \n" //$NON-NLS-1$ //$NON-NLS-2$

            // OLD + " tourDescription         VARCHAR(4096),                                   \n" // version <= 9
            + " tourDescription     VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + "),   \n" // modified in version 10 //$NON-NLS-1$ //$NON-NLS-2$

            + " tourStartPlace      VARCHAR(" + TourData.DB_LENGTH_TOUR_START_PLACE + "),    \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " tourEndPlace        VARCHAR(" + TourData.DB_LENGTH_TOUR_END_PLACE + "),      \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " calories            INTEGER,                                                 \n" //$NON-NLS-1$
            + " bikerWeight         FLOAT,                                                   \n" //$NON-NLS-1$
            + " " + KEY_BIKE + "    BIGINT,                                                  \n" //$NON-NLS-1$ //$NON-NLS-2$

            // from wolfgang
            + " devicePluginName    VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + "),  \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " deviceModeName      VARCHAR(" + TourData.DB_LENGTH_DEVICE_MODE_NAME + "),    \n" //$NON-NLS-1$ //$NON-NLS-2$

            // version 4 end

            + " " + KEY_TYPE + "    BIGINT,                                                  \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " " + KEY_PERSON + "  BIGINT,                                                  \n" //$NON-NLS-1$ //$NON-NLS-2$

            // version 6 start
            //
            + " tourImportFilePath  VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + "),      \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 6 end

            // version 7 start
            //
            + " mergeSourceTourId      BIGINT,                                               \n" //$NON-NLS-1$
            + " mergeTargetTourId      BIGINT,                                               \n" //$NON-NLS-1$
            + " mergedTourTimeOffset   INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " mergedAltitudeOffset   INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " startSecond            SMALLINT DEFAULT 0,                                   \n" //$NON-NLS-1$
            //
            // version 7 end

            // version 8 start
            //
            + " weatherWindDir         INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " weatherWindSpd         INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " weatherClouds          VARCHAR(" + TourData.DB_LENGTH_WEATHER_CLOUDS + "),   \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " restPulse              INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " isDistanceFromSensor   SMALLINT DEFAULT 0,                                   \n" //$NON-NLS-1$
            //
            // version 8 end

            // version 9 start
            //
            + " startWeekYear          SMALLINT DEFAULT 1977,                                \n" //$NON-NLS-1$
            //
            // version 9 end

            // version 10 start
            //
            // tourWayPoints is mapped in TourData
            //
            // version 10 end----------

            // version 11 start
            //
            + " DateTimeCreated        BIGINT DEFAULT 0,                                     \n" //$NON-NLS-1$
            + " DateTimeModified       BIGINT DEFAULT 0,                                     \n" //$NON-NLS-1$
            //
            // version 11 end ---------

            // version 12 start
            //
            + " IsPulseSensorPresent   INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " IsPowerSensorPresent   INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " DeviceAvgSpeed         FLOAT DEFAULT 0,                                      \n" //$NON-NLS-1$
            + " DeviceFirmwareVersion  VARCHAR(" + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + "),   \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 12 end ---------

            // version 13 start
            //
            + " TemperatureScale       INTEGER DEFAULT 1,                                    \n" //$NON-NLS-1$
            + " Weather                VARCHAR(" + TourData.DB_LENGTH_WEATHER + "),          \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 13 end ---------

            // version 14 start
            //
            + " ConconiDeflection      INTEGER DEFAULT 0,                                    \n" //$NON-NLS-1$
            //
            // version 14 end ---------

            // version 17 start
            //
            + " hrZone0                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone1                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone2                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone3                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone4                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone5                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone6                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone7                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone8                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            + " hrZone9                INTEGER DEFAULT -1,                                   \n" //$NON-NLS-1$
            //
            // version 17 end ---------

            // version 18 start
            //
            + " NumberOfHrZones         INTEGER DEFAULT 0,                                  \n" //$NON-NLS-1$
            //
            // version 18 end ---------

            // version 21 start
            //
            + " maxAltitude            FLOAT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " maxPulse               FLOAT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " avgPulse               FLOAT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " avgCadence             FLOAT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " avgTemperature         FLOAT DEFAULT 0,                                    \n" //$NON-NLS-1$
            //
            // version 21 end ---------

            // version 22 start  -  12.12
            //
            + " TourStartTime          BIGINT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " TourEndTime            BIGINT DEFAULT 0,                                    \n" //$NON-NLS-1$

            + " TourDeviceTime_Elapsed  BIGINT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + " TourComputedTime_Moving BIGINT DEFAULT 0,                                    \n" //$NON-NLS-1$
            //
            // version 22 end ---------

            // version 23 start  -  13.2
            //
            + " numberOfTimeSlices     INTEGER DEFAULT 0,                                 \n" //$NON-NLS-1$
            + " numberOfPhotos         INTEGER DEFAULT 0,                                 \n" //$NON-NLS-1$
            + " photoTimeAdjustment    INTEGER DEFAULT 0,                                 \n" //$NON-NLS-1$
            //
            // version 23 end ---------

            // version 27 start  -  15.5
            //
            + " frontShiftCount        INTEGER DEFAULT 0,                                 \n" //$NON-NLS-1$
            + " rearShiftCount         INTEGER DEFAULT 0,                                 \n" //$NON-NLS-1$
            //
            // version 27 end ---------

            // version 29 start  -  15.12
            //
            + " TourImportFileName     VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_NAME + "),      \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 29 end ---------

            // version 30 start  -  16.1
            //
            + " power_Avg                             FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$
            + " power_Max                             INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            + " power_Normalized                      INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            + " power_FTP                             INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$

            + " power_TotalWork                       BIGINT DEFAULT 0,                     \n" //$NON-NLS-1$
            + " power_TrainingStressScore             FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$
            + " power_IntensityFactor                 FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$

            + " power_PedalLeftRightBalance           INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            + " power_AvgLeftTorqueEffectiveness      FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$
            + " power_AvgRightTorqueEffectiveness     FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$
            + " power_AvgLeftPedalSmoothness          FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$
            + " power_AvgRightPedalSmoothness         FLOAT DEFAULT 0,                     \n" //$NON-NLS-1$
            //
            // version 30 end ---------

            // version 31 start  -  16.5
            //
            + " CadenceMultiplier                     FLOAT DEFAULT 1.0,                  \n" //$NON-NLS-1$
            + " IsStrideSensorPresent                 INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            //
            // version 31 end ---------

            // version 32 start  -  >16.8 ???
            //
            + " TimeZoneId            VARCHAR(" + TourData.DB_LENGTH_TIME_ZONE_ID + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 32 end ---------

            // version 35 start  -  18.7
            //
            + " runDyn_StanceTime_Min                 SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_StanceTime_Max                 SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_StanceTime_Avg                 FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$

            + " runDyn_StanceTimeBalance_Min          SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_StanceTimeBalance_Max          SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_StanceTimeBalance_Avg          FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$

            + " runDyn_StepLength_Min                 SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_StepLength_Max                 SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_StepLength_Avg                 FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$

            + " runDyn_VerticalOscillation_Min        SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_VerticalOscillation_Max        SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_VerticalOscillation_Avg        FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$

            + " runDyn_VerticalRatio_Min              SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_VerticalRatio_Max              SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " runDyn_VerticalRatio_Avg              FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            //
            // version 35 end ---------

            // version 36 start  -  18.12
            //
            + " surfing_NumberOfEvents                SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " surfing_MinSpeed_StartStop            SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$
            + " surfing_MinSpeed_Surfing              SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$
            + " surfing_MinTimeDuration               SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$

            + " surfing_IsMinDistance                 BOOLEAN  DEFAULT FALSE,             \n" //$NON-NLS-1$
            + " surfing_MinDistance                   SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$

            //
            // version 36 end ---------

            // version 37 start  -  19.3
            //
            + " hasGeoData                            BOOLEAN  DEFAULT FALSE,             \n" //$NON-NLS-1$
            //
            // version 37 end ---------

            // version 38 start  -  19.5
            //
            + " training_TrainingEffect_Aerob         FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            + " training_TrainingEffect_Anaerob       FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            + " training_TrainingPerformance          FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            //
            // version 38 end

            // version 39 start  -  19.7
            //
            + " isWeatherDataFromApi                  BOOLEAN  DEFAULT FALSE,             \n" //$NON-NLS-1$
            + " weather_Humidity                      SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
            + " weather_Precipitation                 FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            + " weather_Pressure                      FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            + " weather_Temperature_Min               FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            + " weather_Temperature_Max               FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            + " weather_Temperature_WindChill         FLOAT    DEFAULT 0,                 \n" //$NON-NLS-1$
            //
            // version 39 end

            // version 40 start  -  19.10
            //
            + " power_DataSource     VARCHAR(" + TourData.DB_LENGTH_POWER_DATA_SOURCE + "),      \n" //$NON-NLS-1$ //$NON-NLS-2$
            + " cadenceZone_SlowTime                  INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            + " cadenceZone_FastTime                  INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            + " cadenceZones_DelimiterValue           INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            + " avgAltitudeChange                     INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
            //
            // version 40 end

            // version 41 start  -  20.8
            //
            + " maxPace                               FLOAT DEFAULT 0,                  \n" //$NON-NLS-1$
            //
            // version 41 end

            // version 42 start  -  20.X
            //
            + " tourDeviceTime_Recorded                       BIGINT,                  \n" //$NON-NLS-1$
            + " tourDeviceTime_Paused                         BIGINT,                  \n" //$NON-NLS-1$
            //
            // version 42 end

            //            // version 35 start  -  18.?
            //            //
            //            + " LatitudeMinE6         INTEGER DEFAULT 0,                        \n" //$NON-NLS-1$
            //            + " LatitudeMaxE6         INTEGER DEFAULT 0,                        \n" //$NON-NLS-1$
            //            + " LongitudeMinE6        INTEGER DEFAULT 0,                        \n" //$NON-NLS-1$
            //            + " LongitudeMaxE6        INTEGER DEFAULT 0,                        \n" //$NON-NLS-1$
            //            //
            //            // version 35 end ---------

            // version 5 start
            /**
             * Disabled because when two blob object's are deserialized then the error occurs:
             * <p>
             * java.io.StreamCorruptedException: invalid stream header: 00ACED00
             * <p>
             * -> the gpsData are put into the serieData object
             */
            //   + "gpsData                         BLOB,                                    \n" //$NON-NLS-1$

            // version 5 end

            + " serieData              BLOB                                               \n" //$NON-NLS-1$

            + ")"); //$NON-NLS-1$

      createIndex_TourData_005(stmt);
      createIndex_TourData_022(stmt);
      createIndex_TourData_029(stmt);
      createIndex_TourData_033(stmt);
//    createIndex_TourData_035(stmt);
      createIndex_TourData_037(stmt);
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_GEO_PARTS + "   (                           \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   TourId         BIGINT   NOT NULL,                                       \n" //$NON-NLS-1$
            + "   GeoPart        INTEGER  NOT NULL,                                       \n" //$NON-NLS-1$

            + "   CONSTRAINT     PK_TourId_GeoPart PRIMARY KEY (TourId, GeoPart)          \n" //$NON-NLS-1$

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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_MARKER + "   (                              \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_MARKER, true)
            //
            + "   " + KEY_TOUR + "     BIGINT,                                            \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            + "   time                 INTEGER NOT NULL,                                  \n" //$NON-NLS-1$

            // before version 20
            // + "   distance          INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + "   distance             INTEGER,                                           \n" //$NON-NLS-1$

            // Version 20 - begin
            //
            + "   distance20           FLOAT DEFAULT 0,                                   \n" //$NON-NLS-1$
            //
            // Version 20 - end

            // Version 22 - begin
            //
            + "   IsMarkerVisible      INTEGER DEFAULT 1,                                 \n" //$NON-NLS-1$
            //
            // Version 22 - end
            //
            // Version 24 - begin
            //
            + "   description          VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),        \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlText              VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),             \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlAddress           VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + "),          \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // Version 24 - end
            //
            // Version 25 - begin
            //
            + "   tourTime             BIGINT DEFAULT " + Long.MIN_VALUE + ",             \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // When DEFAULT value is NOT set, this exception occurs:
            //
            //java.lang.IllegalArgumentException: Can not set float field net.tourbook.data.TourMarker.altitude to null value
            //   at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:176)
            //   at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:180)
            //   at sun.reflect.UnsafeFloatFieldAccessorImpl.set(UnsafeFloatFieldAccessorImpl.java:92)
            //   at java.lang.reflect.Field.set(Field.java:753)
            //   at org.hibernate.property.DirectPropertyAccessor$DirectSetter.set(DirectPropertyAccessor.java:102)
            //
            + "   altitude             FLOAT DEFAULT " + SQL_FLOAT_MIN_VALUE + ",         \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   latitude             DOUBLE DEFAULT " + SQL_DOUBLE_MIN_VALUE + ",       \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   longitude            DOUBLE DEFAULT " + SQL_DOUBLE_MIN_VALUE + ",       \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // Version 25 - end
            //
            + "   serieIndex           INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + "   type                 INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + "   visualPosition       INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + "   label                VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),      \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   category             VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),  \n" //$NON-NLS-1$ //$NON-NLS-2$

            //
            // Version 2
            + "   labelXOffset         INTEGER,                                           \n" //$NON-NLS-1$
            + "   labelYOffset         INTEGER,                                           \n" //$NON-NLS-1$
            + "   markerType           BIGINT                                             \n" //$NON-NLS-1$
            //
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON + "   (                             \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_PERSON, true)
            //
            + "   lastName               VARCHAR(" + TourPerson.DB_LENGTH_LAST_NAME + "),         \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   firstName              VARCHAR(" + TourPerson.DB_LENGTH_FIRST_NAME + "),        \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   weight                 FLOAT,                                                   \n" //$NON-NLS-1$ // kg
            + "   height                 FLOAT,                                                   \n" //$NON-NLS-1$ // m

            // version 15 start
            //
            + "   BirthDay               BIGINT DEFAULT 0,                                        \n" //$NON-NLS-1$
            //
            // version 15 end ---------

            // version 16 start
            //
            + "   Gender                 INTEGER DEFAULT 0,                                       \n" //$NON-NLS-1$
            + "   RestPulse              INTEGER DEFAULT 0,                                       \n" //$NON-NLS-1$
            + "   MaxPulse               INTEGER DEFAULT 0,                                       \n" //$NON-NLS-1$
            + "   HrMaxFormula           INTEGER DEFAULT 0,                                       \n" //$NON-NLS-1$
            //
            // version 16 end ---------

            + "   rawDataPath            VARCHAR(" + TourPerson.DB_LENGTH_RAW_DATA_PATH + "),     \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   deviceReaderId         VARCHAR(" + TourPerson.DB_LENGTH_DEVICE_READER_ID + "),  \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            + "   " + KEY_BIKE + "       BIGINT                                                  \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON_HRZONE + "   (                        \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_HR_ZONE, true)
            //
            + "   " + KEY_PERSON + "   BIGINT,                                             \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            + "   zoneName         VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   nameShortcut      VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),         \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   description         VARCHAR(" + TourPersonHRZone.DB_LENGTH_DESCRIPTION + "),         \n" //$NON-NLS-1$ //$NON-NLS-2$

            // version 18 start
            //
            + "   ColorRed         INTEGER DEFAULT 0,                                        \n" //$NON-NLS-1$
            + "   ColorGreen         INTEGER DEFAULT 0,                                     \n" //$NON-NLS-1$
            + "   ColorBlue         INTEGER DEFAULT 0,                                     \n" //$NON-NLS-1$
            //
            // version 18 end ---------

            //
            + "   zoneMinValue      INTEGER NOT NULL,                                       \n" //$NON-NLS-1$
            + "   zoneMaxValue      INTEGER NOT NULL                                       \n" //$NON-NLS-1$
            //
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_PHOTO + "   (                                 \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_PHOTO, true)
            //
            + "   " + KEY_TOUR + "         BIGINT,                                          \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 23 start
            //
            + "   imageFileName            VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFileExt            VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFilePath            VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageFilePathName         VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   imageExifTime            BIGINT DEFAULT 0,                                 \n" //$NON-NLS-1$
            + "   imageFileLastModified      BIGINT DEFAULT 0,                                 \n" //$NON-NLS-1$
            //
            + "   adjustedTime            BIGINT DEFAULT 0,                                 \n" //$NON-NLS-1$
            //
            + "   ratingStars               INT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + "   isGeoFromPhoto            INT DEFAULT 0,                                    \n" //$NON-NLS-1$
            + "   latitude                DOUBLE DEFAULT 0,                                 \n" //$NON-NLS-1$
            + "   longitude                DOUBLE DEFAULT 0                                 \n" //$NON-NLS-1$
            //
            // version 23 end
            + ")"); //$NON-NLS-1$

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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_REFERENCE + "   (                           \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_REF, true)
            //
            + "   " + KEY_TOUR + "     BIGINT,                                            \n"//$NON-NLS-1$ //$NON-NLS-2$
            //
            + "   startIndex           INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + "   endIndex             INTEGER NOT NULL,                                  \n" //$NON-NLS-1$
            + "   label                VARCHAR(" + TourReference.DB_LENGTH_LABEL + ")     \n" //$NON-NLS-1$ //$NON-NLS-2$
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG + "   (                                 \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TAG, true)
            //
            + "   isRoot               INTEGER,                                           \n" //$NON-NLS-1$
            + "   expandType           INTEGER,                                           \n" //$NON-NLS-1$
            + "   name                 VARCHAR(" + TourTag.DB_LENGTH_NAME + "),           \n" //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 start
            //
            + "   notes                VARCHAR(" + TourTag.DB_LENGTH_NOTES + ")           \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 38 end ---------

            //
            + ")"); //$NON-NLS-1$

      /**
       * Create table: TOURDATA_TOURTAG
       */
      exec(stmt,

            "CREATE TABLE " + JOINTABLE__TOURDATA__TOURTAG + "   (                        \n" //$NON-NLS-1$ //$NON-NLS-2$

                  + "   " + KEY_TAG + "      BIGINT NOT NULL,                             \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + KEY_TOUR + "     BIGINT NOT NULL                              \n"//$NON-NLS-1$ //$NON-NLS-2$

                  + ")"); //$NON-NLS-1$

      // Add Constraint
      final String fkName = "fk_" + JOINTABLE__TOURDATA__TOURTAG + "_" + KEY_TOUR; //         //$NON-NLS-1$ //$NON-NLS-2$

      exec(stmt,

            "ALTER TABLE " + JOINTABLE__TOURDATA__TOURTAG + "                             \n" //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkName + "                                     \n" //$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TOUR + ")                                    \n" //$NON-NLS-1$ //$NON-NLS-2$
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG_CATEGORY + "   (                                    \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TAG_CATEGORY, true)
            //
            + "   isRoot               INTEGER,                                                       \n" //$NON-NLS-1$
            + "   name                 VARCHAR(" + TourTag.DB_LENGTH_NAME + "),                       \n" //$NON-NLS-1$ //$NON-NLS-2$

            // version 38 start
            //
            + "   notes                VARCHAR(" + TourTag.DB_LENGTH_NOTES + ")                       \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 38 end ---------

            //
            + ")" //$NON-NLS-1$
      );

      /**
       * Create table: TOURTAGCATEGORY_TOURTAG
       */
      final String jtabTag = JOINTABLE__TOURTAGCATEGORY_TOURTAG;

      exec(stmt, "CREATE TABLE " + jtabTag + "   (                                                    \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   " + KEY_TAG + "               BIGINT NOT NULL,                                      \n"//$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_TAG_CATEGORY + "      BIGINT NOT NULL                                       \n"//$NON-NLS-1$ //$NON-NLS-2$
            //
            + ")"); //$NON-NLS-1$

      // add constraints
      final String fkTag = "fk_" + jtabTag + "_" + KEY_TAG; //                                           //$NON-NLS-1$ //$NON-NLS-2$
      final String fkCat = "fk_" + jtabTag + "_" + TABLE_TOUR_TAG_CATEGORY + "_" + KEY_TAG_CATEGORY; //  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      exec(
            stmt,
            "ALTER TABLE " + jtabTag + "                                                              \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkTag + "                                                  \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG + ")                                                 \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG + " (" + ENTITY_ID_TAG + ")                     \n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      exec(
            stmt,
            "ALTER TABLE " + jtabTag + "                                                              \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fkCat + "                                                  \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG_CATEGORY + ")                                        \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")   \n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      /**
       * Create table: TOURTAGCATEGORY_TOURTAGCATEGORY
       */
      final String jtabCategory = JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY;

      exec(stmt, "CREATE TABLE " + jtabCategory + "   (                                               \n"//$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   " + KEY_TAG_CATEGORY + "1   BIGINT NOT NULL,                                        \n"//$NON-NLS-1$ //$NON-NLS-2$
            + "   " + KEY_TAG_CATEGORY + "2   BIGINT NOT NULL                                         \n"//$NON-NLS-1$ //$NON-NLS-2$
            //
            + ")"); //$NON-NLS-1$

      // add constraints
      final String fk1 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "1"; //                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final String fk2 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "2"; //                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      exec(
            stmt,
            "ALTER TABLE " + jtabCategory + "                                                         \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fk1 + "                                                    \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG_CATEGORY + "1)                                       \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")   \n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      );

      exec(
            stmt,
            "ALTER TABLE " + jtabCategory + "                                                         \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   ADD CONSTRAINT " + fk2 + "                                                    \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   FOREIGN KEY (" + KEY_TAG_CATEGORY + "2)                                       \n"//$NON-NLS-1$ //$NON-NLS-2$
                  + "   REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")   \n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
      //                                             //$NON-NLS-1$

      exec(stmt, "CREATE TABLE " + TABLE_TOUR_TYPE + "   (                                      \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_TYPE, true)
            //
            + "   name                 VARCHAR(" + TourType.DB_LENGTH_NAME + "),                \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            + "   colorBrightRed       SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorBrightGreen     SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorBrightBlue      SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorDarkRed         SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorDarkGreen       SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorDarkBlue        SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorLineRed         SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorLineGreen       SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$
            + "   colorLineBlue        SMALLINT NOT NULL,                                       \n" //$NON-NLS-1$

            // version 19 start
            //
            + "   colorTextRed         SMALLINT DEFAULT 0,                                      \n" //$NON-NLS-1$
            + "   colorTextGreen       SMALLINT DEFAULT 0,                                      \n" //$NON-NLS-1$
            + "   colorTextBlue        SMALLINT DEFAULT 0                                       \n" //$NON-NLS-1$
            //
            // version 19 end ---------
            //
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
      exec(stmt, "CREATE TABLE " + TABLE_TOUR_WAYPOINT + "   (                                  \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + SQL.CreateField_EntityId(ENTITY_ID_WAY_POINT, true)
            //
            + "   " + KEY_TOUR + "     BIGINT,                                                  \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            + "   latitude             DOUBLE NOT NULL,                                         \n" //$NON-NLS-1$
            + "   longitude            DOUBLE NOT NULL,                                         \n" //$NON-NLS-1$
            + "   time                 BIGINT,                                                  \n" //$NON-NLS-1$
            + "   altitude             FLOAT,                                                   \n" //$NON-NLS-1$
            + "   name                 VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),            \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   description          VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),     \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   comment              VARCHAR(" + TourWayPoint.DB_LENGTH_COMMENT + "),         \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   symbol               VARCHAR(" + TourWayPoint.DB_LENGTH_SYMBOL + "),          \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   category             VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),        \n" //$NON-NLS-1$ //$NON-NLS-2$

            // version 28 start - create common fields with TourMarker
            //
            + "   urlText              VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),          \n" //$NON-NLS-1$ //$NON-NLS-2$
            + "   urlAddress           VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + ")        \n" //$NON-NLS-1$ //$NON-NLS-2$
            //
            // version 28 end ---------
            //
            + ")"); //$NON-NLS-1$
   }

   /**
    * create table {@link #TABLE_DB_VERSION}
    *
    * @param stmt
    * @throws SQLException
    */
   private void createTable_Version(final Statement stmt) throws SQLException {

      /*
       * CREATE TABLE Version
       */
      exec(stmt, "CREATE TABLE " + TABLE_DB_VERSION + " (                                       \n" //$NON-NLS-1$ //$NON-NLS-2$
      //
            + "   version    INTEGER   NOT NULL                                                 \n" //$NON-NLS-1$
            //
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

      if (sqlInit_10_IsDbInitialized()) {
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

      logDriverManager_GetConnection(dbUrl);

      return DriverManager.getConnection(dbUrl, TABLE_SCHEMA, TABLE_SCHEMA);
   }

   /**
    * Creates an entity manager which is used to persist entities
    *
    * @return
    */
   public EntityManager getEntityManager() {

      if (_emFactory == null) {

         // ensure db is valid BEFOR entity manager is inizialized which can shutdown the database
         if (sqlInit_10_IsDbInitialized() == false) {
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

   private boolean isColumnAvailable(final Connection conn, final String table, final String column) {

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

   private boolean isIndexAvailable(final Connection conn, final String table, final String column) {

      try {

         final String requestedIndexName = column.toUpperCase();

         final DatabaseMetaData meta = conn.getMetaData();
         final ResultSet result = meta.getIndexInfo(null, TABLE_SCHEMA, table, false, false);

         while (result.next()) {

            final String dbIndexName = result.getString("COLUMN_NAME"); //$NON-NLS-1$

            if (requestedIndexName.equals(dbIndexName.toUpperCase())) {

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

   private boolean isPrimaryKeyAvailable(final Connection conn, final String table, final String key) {

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

   private boolean isTableAvailable(final Connection conn, final String table) {

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

   private void logDb_UpdateEnd(final int dbVersion) {

      System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, dbVersion));
      System.out.println();
   }

   private void logDb_UpdateStart(final int dbVersion) {

      System.out.println();
      System.out.println(NLS.bind(Messages.Tour_Database_Update, dbVersion));
   }

   private void logDriverManager_GetConnection(final String dbUrl) {

      StatusUtil.logInfo("Derby command executed: " + dbUrl); //$NON-NLS-1$
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
   }

   public void removePropertyListener(final IPropertyListener listener) {
      _propertyListeners.remove(listener);
   }

   private boolean sqlInit_10_IsDbInitialized() {

      if (_isDbInitialized) {
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

         /*
          * Check or setup sql
          */
         final Runnable runnable = new Runnable() {
            @Override
            public void run() {

               final SplashManager splashManager = SplashManager.getInstance();

               splashManager.setMessage(Messages.App_SplashMessage_StartingDatabase);
               try {

                  sqlInit_20_CheckServer(splashManager);
                  sqlInit_30_Check_DbIsCreated();

               } catch (final Throwable e) {

                  StatusUtil.log(e);
                  return;
               }

               sqlInit_40_CheckTable(splashManager);

               if (sqlInit_60_IsVersionValid(splashManager) == false) {
                  return;
               }

               sqlInit_80_Check_DbIsUpgraded_After(splashManager);

               sqlInit_90_SetupEntityManager(splashManager);

               splashManager.setMessage(Messages.App_SplashMessage_Finalize);

               returnState[0] = true;
            }

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
   private void sqlInit_20_CheckServer(final SplashManager splashManager) throws Throwable {

      if (_isDerbyEmbedded) {
         return;
      }

      // when the server is started, nothing is to do here
      if (_server != null) {
         return;
      }

      try {

         sqlInit_22_CheckServer_CreateRunnable(splashManager).run(new NullProgressMonitor());

      } catch (final InvocationTargetException e) {

         StatusUtil.log(e);

         MessageDialog.openError(
               Display.getDefault().getActiveShell(),
               Messages.Tour_Database_CannotConnectToDerbyServer_Title,
               NLS.bind(
                     Messages.Tour_Database_CannotConnectToDerbyServer_Message,
                     e
                           .getTargetException()
                           .getMessage()));

         PlatformUI.getWorkbench().close();

         throw e.getTargetException();

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
   private IRunnableWithProgress sqlInit_22_CheckServer_CreateRunnable(final SplashManager splashManager) {

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
               StatusUtil.logInfo("checking if derby server is already running before server.start");//$NON-NLS-1$
               _server.ping();

            } catch (final Exception e) {

               try {
                  StatusUtil.logInfo("starting derby server");//$NON-NLS-1$

                  _server.start(null);

               } catch (final Exception e2) {
                  StatusUtil.log(e2);
               }

               StatusUtil.logInfo("checking if derby server is running after server.start");//$NON-NLS-1$

               int pingCounter = 1;
               final int threadSleepTime = 100;

               // wait until the server is started
               while (true) {

                  try {

                     if (splashManager != null) {
                        splashManager.setMessage(createUIServerStateMessage(pingCounter));
                     }
                     _server.ping();

                     StatusUtil.logInfo("derby server has started");//$NON-NLS-1$

                     break;

                  } catch (final Exception e1) {

                     if (pingCounter > MAX_TRIES_TO_PING_SERVER) {

                        StatusUtil.log("Cannot connect to derby server", e1);//$NON-NLS-1$

                        throw new InvocationTargetException(e1);
                     }

                     StatusUtil.logInfo(
                           NLS.bind(
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
                  StatusUtil.logInfo("Database path: " + _databasePath); //$NON-NLS-1$

               } catch (final SQLException e1) {
                  UI.showSQLException(e1);
               }
            }
         }
      };

      return runnable;
   }

   private void sqlInit_30_Check_DbIsCreated() {

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

         logDriverManager_GetConnection(dbUrl);

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
   private void sqlInit_40_CheckTable(final SplashManager splashManager) {

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

            createTable_Version(stmt);

            createTable_TourTag(stmt);
            createTable_TourTag_Category(stmt);

            createTable_TourWayPoint(stmt);
//            createTable_SharedMarker(stmt);

//            createTable_TourSign(stmt);
//            createTable_TourSignCategory(stmt);

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
    * @return
    */
   private boolean sqlInit_60_IsVersionValid(final SplashManager splashManager) {

      if (_isVersionChecked) {
         return true;
      }

      if (_isSQLUpdateError) {
         return false;
      }

      Connection conn2 = null;
      Statement stmt1 = null;
      Statement stmt2 = null;

      try (Connection conn1 = getConnection_Simple()) {

         {
            String sql = "SELECT * FROM " + TABLE_DB_VERSION; //$NON-NLS-1$

            stmt1 = conn1.createStatement();
            final ResultSet result = stmt1.executeQuery(sql);

            if (result.next()) {

               // version record was found, check if the database contains the correct version

               _dbVersionBeforeUpdate = result.getInt(1);
               _dbVersionAfterUpdate = _dbVersionBeforeUpdate;

               StatusUtil.logInfo("Current database version: " + _dbVersionBeforeUpdate); //$NON-NLS-1$

               if (_dbVersionBeforeUpdate < TOURBOOK_DB_VERSION) {

                  conn2 = sqlInit_70_Check_DbIsUpgraded_Before(_dbVersionBeforeUpdate, splashManager);

                  if (updateDbDesign(conn2, _dbVersionBeforeUpdate, splashManager) == false) {
                     return false;
                  }

               } else if (_dbVersionBeforeUpdate > TOURBOOK_DB_VERSION) {

                  MessageDialog.openInformation(
                        Display.getCurrent().getActiveShell(),
                        Messages.tour_database_version_info_title,
                        NLS.bind(
                              Messages.tour_database_version_info_message,
                              _dbVersionBeforeUpdate,
                              TOURBOOK_DB_VERSION));
               }

            } else {

               // a version record is not available

               /*
                * insert the version for the current database design into the database
                */
               sql = "INSERT INTO " + TABLE_DB_VERSION //                        //$NON-NLS-1$
                     + " VALUES (" + Integer.toString(TOURBOOK_DB_VERSION) + ")"; //         //$NON-NLS-1$ //$NON-NLS-2$

               stmt2 = conn1.createStatement();
               stmt2.executeUpdate(sql);
            }
         }

         _isVersionChecked = true;

      } catch (final SQLException e) {

         UI.showSQLException(e);
         PlatformUI.getWorkbench().close();

      } catch (final Exception e) {

         StatusUtil.showStatus(e);
         PlatformUI.getWorkbench().close();

      } finally {

         Util.closeSql(conn2);
         Util.closeSql(stmt1);
         Util.closeSql(stmt2);
      }

      return _isVersionChecked;
   }

   /**
    * Check BEFORE the data structure is modified otherwise this can fail when a new feature is use,
    * e.g. BOOLEAN
    *
    * @param dbVersionBeforeUpdate
    * @param splashManager
    * @return
    */
   private Connection sqlInit_70_Check_DbIsUpgraded_Before(final int dbVersionBeforeUpdate, final SplashManager splashManager) {

      if (_isChecked_DbUpgraded_Before) {

         Connection conn = null;
         try {
            conn = getConnection_Simple();
         } catch (final SQLException e) {
            UI.showSQLException(e);
         }
         return conn;
      }

      boolean isUpgradeNeeded = false;

      if (dbVersionBeforeUpdate < 36) {

         // db version 36: update to derby 10.14.2 to use BOOLEAN datatype

         isUpgradeNeeded = true;
      }

      if (isUpgradeNeeded == false) {

         _isChecked_DbUpgraded_Before = true;

         Connection conn = null;
         try {
            conn = getConnection_Simple();
         } catch (final SQLException e) {
            UI.showSQLException(e);
         }
         return conn;
      }

      StatusUtil.logInfo(String.format("DB upgrade BEFORE is needed %d", _dbVersionBeforeUpdate)); //$NON-NLS-1$

      Connection conn = null;

      /*
       * Shutdown db
       */
      try {

         // shutdown database that all connections are closed, THIS WILL ALWAYS CREATE AN EXCEPTION
         final String dbUrl_ShutDown = DERBY_URL + DERBY_URL_COMMAND_SHUTDOWN_TRUE;

         logDriverManager_GetConnection(dbUrl_ShutDown);

         conn = DriverManager.getConnection(dbUrl_ShutDown, TABLE_SCHEMA, TABLE_SCHEMA);

      } catch (final SQLException e) {

         final String sqlExceptionText = Util.getSQLExceptionText(e);

         // log also the stacktrace
         StatusUtil.log(sqlExceptionText + Util.getStackTrace(e));

      } finally {
         Util.closeSql(conn);
      }

      /*
       * Upgrade database
       */
      try {

         final String dbUrl_Upgrade = DERBY_URL + DERBY_URL_COMMAND_UPGRADE_TRUE;

         logDriverManager_GetConnection(dbUrl_Upgrade);

         splashManager.setMessage(Messages.Database_Monitor_UpgradeDatabase);

         conn = DriverManager.getConnection(dbUrl_Upgrade, TABLE_SCHEMA, TABLE_SCHEMA);

         _isChecked_DbUpgraded_Before = true;

      } catch (final SQLException e) {
         UI.showSQLException(e);
      } finally {
         Util.closeSql(conn);
      }

      /*
       * Return simple connection
       */
      try {
         conn = getConnection_Simple();
      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
      return conn;
   }

   private void sqlInit_80_Check_DbIsUpgraded_After(final SplashManager splashManager) {

      if (_isChecked_DbUpgraded_After) {
         return;
      }

      boolean isUpgradeNeeded = false;

      if (_dbVersionBeforeUpdate < 26 && _dbVersionAfterUpdate >= 26) {

         // db version 26: update to derby 10.11.1.1 to implement text search with lucene

         isUpgradeNeeded = true;
      }

      if (isUpgradeNeeded == false) {

         _isChecked_DbUpgraded_After = true;

         return;
      }

      StatusUtil.logInfo(String.format("DB upgrade AFTER is needed %d -> %d", _dbVersionBeforeUpdate, _dbVersionAfterUpdate)); //$NON-NLS-1$

      Connection conn = null;

      /*
       * Shutdown db
       */
      try {

         // shutdown database that all connections are closed, THIS WILL ALWAYS CREATE AN EXCEPTION
         final String dbUrl_ShutDown = DERBY_URL + DERBY_URL_COMMAND_SHUTDOWN_TRUE;

         logDriverManager_GetConnection(dbUrl_ShutDown);

         conn = DriverManager.getConnection(dbUrl_ShutDown, TABLE_SCHEMA, TABLE_SCHEMA);

      } catch (final SQLException e) {

         final String sqlExceptionText = Util.getSQLExceptionText(e);

         // log also the stacktrace
         StatusUtil.log(sqlExceptionText + Util.getStackTrace(e));

      } finally {
         Util.closeSql(conn);
      }

      /*
       * Upgrade database
       */
      try {

         final String dbUrl_Upgrade = DERBY_URL + DERBY_URL_COMMAND_UPGRADE_TRUE;

         logDriverManager_GetConnection(dbUrl_Upgrade);

         splashManager.setMessage(Messages.Database_Monitor_UpgradeDatabase);

         conn = DriverManager.getConnection(dbUrl_Upgrade, TABLE_SCHEMA, TABLE_SCHEMA);

         _isChecked_DbUpgraded_After = true;

      } catch (final SQLException e) {
         UI.showSQLException(e);
      } finally {
         Util.closeSql(conn);
      }
   }

   private synchronized void sqlInit_90_SetupEntityManager(final SplashManager splashManager) {

      final Map<String, Object> configOverrides = new HashMap<>();

      configOverrides.put("hibernate.connection.url", DERBY_URL); //$NON-NLS-1$
      configOverrides.put("hibernate.connection.driver_class", DERBY_DRIVER_CLASS); //$NON-NLS-1$

      splashManager.setMessage(Messages.Database_Monitor_persistent_service_task);

      _emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, configOverrides);
   }

   /**
    * this must be implemented or updated when the database version must be updated
    */
   private boolean updateDbDesign(final Connection conn, int currentDbVersion, final SplashManager splashManager) {

      /*
       * confirm update
       */

      // define buttons with default to NO
      final String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };

      if ((new MessageDialog(
            Display.getDefault().getActiveShell(),
            Messages.Database_Confirm_update_title,
            null,
            NLS.bind(
                  Messages.Database_Confirm_update,
                  new Object[] {
                        currentDbVersion,
                        TOURBOOK_DB_VERSION,
                        _databasePath }),
            MessageDialog.QUESTION,
            buttons,
            1).open()) != Window.OK) {

         // no update -> close application
         PlatformUI.getWorkbench().close();

         return false;
      }

      /*
       * do an additional check because version 20 is restructuring the data series
       */
      if (currentDbVersion < 20) {

         if ((new MessageDialog(
               Display.getDefault().getActiveShell(),
               Messages.Database_Confirm_update_title,
               null,
               NLS.bind(Messages.Database_Confirm_Update20, _databasePath),
               MessageDialog.QUESTION,
               buttons,
               1).open()) != Window.OK) {

            // no update -> close application
            PlatformUI.getWorkbench().close();

            return false;
         }
      }

      int newVersion = currentDbVersion;
      final int oldVersion = currentDbVersion;

      /*
       * database update
       */
      try {

         if (currentDbVersion == 1) {
            updateDbDesign_001_002(conn);
            currentDbVersion = newVersion = 2;
         }

         if (currentDbVersion == 2) {
            updateDbDesign_002_003(conn);
            currentDbVersion = newVersion = 3;
         }

         if (currentDbVersion == 3) {
            updateDbDesign_003_004(conn, splashManager);
            currentDbVersion = newVersion = 4;
         }

         boolean isPostUpdate5 = false;
         if (currentDbVersion == 4) {
            updateDbDesign_004_005(conn, splashManager);
            currentDbVersion = newVersion = 5;
            isPostUpdate5 = true;
         }

         if (currentDbVersion == 5) {
            updateDbDesign_005_006(conn, splashManager);
            currentDbVersion = newVersion = 6;
         }

         if (currentDbVersion == 6) {
            updateDbDesign_006_007(conn, splashManager);
            currentDbVersion = newVersion = 7;
         }

         if (currentDbVersion == 7) {
            updateDbDesign_007_008(conn, splashManager);
            currentDbVersion = newVersion = 8;
         }

         boolean isPostUpdate9 = false;
         if (currentDbVersion == 8) {
            updateDbDesign_008_009(conn, splashManager);
            currentDbVersion = newVersion = 9;
            isPostUpdate9 = true;
         }

         if (currentDbVersion == 9) {
            updateDbDesign_009_010(conn, splashManager);
            currentDbVersion = newVersion = 10;
         }

         boolean isPostUpdate11 = false;
         if (currentDbVersion == 10) {
            currentDbVersion = newVersion = updateDbDesign_010_011(conn, splashManager);
            isPostUpdate11 = true;
         }

         if (currentDbVersion == 11) {
            currentDbVersion = newVersion = updateDbDesign_011_012(conn, splashManager);
         }

         boolean isPostUpdate13 = false;
         if (currentDbVersion == 12) {
            currentDbVersion = newVersion = updateDbDesign_012_013(conn, splashManager);
            isPostUpdate13 = true;
         }

         if (currentDbVersion == 13) {
            currentDbVersion = newVersion = updateDbDesign_013_014(conn, splashManager);
         }

         if (currentDbVersion == 14) {
            currentDbVersion = newVersion = updateDbDesign_014_015(conn, splashManager);
         }

         if (currentDbVersion == 15) {
            currentDbVersion = newVersion = updateDbDesign_015_to_016(conn, splashManager);
         }

         if (currentDbVersion == 16) {
            currentDbVersion = newVersion = updateDbDesign_016_to_017(conn, splashManager);
         }

         if (currentDbVersion == 17) {
            currentDbVersion = newVersion = updateDbDesign_017_to_018(conn, splashManager);
         }

         if (currentDbVersion == 18) {
            currentDbVersion = newVersion = updateDbDesign_018_to_019(conn, splashManager);
         }

         boolean isPostUpdate20 = false;
         if (currentDbVersion == 19) {
            currentDbVersion = newVersion = updateDbDesign_019_to_020(conn, splashManager);
            isPostUpdate20 = true;
         }

         /*
          * 21
          */
         if (currentDbVersion == 20) {
            currentDbVersion = newVersion = updateDbDesign_020_to_021(conn, splashManager);
         }

         /*
          * 22
          */
         boolean isPostUpdate22 = false;

         if (currentDbVersion == 21) {
            currentDbVersion = newVersion = updateDbDesign_021_to_022(conn, splashManager);
            isPostUpdate22 = true;
         }

         /*
          * 23
          */
         boolean isPostUpdate23 = false;

         if (currentDbVersion == 22) {
            currentDbVersion = newVersion = updateDbDesign_022_to_023(conn, splashManager);
            isPostUpdate23 = true;
         }

         /*
          * 24
          */
         if (currentDbVersion == 23) {
            currentDbVersion = newVersion = updateDbDesign_023_to_024(conn, splashManager);
         }

         // 24 -> 25
         boolean isPostUpdate25 = false;

         if (currentDbVersion == 24) {
            isPostUpdate25 = true;
            currentDbVersion = newVersion = updateDbDesign_024_to_025(conn, splashManager);
         }

         // 25 -> 26
         if (currentDbVersion == 25) {
            currentDbVersion = newVersion = updateDbDesign_025_to_026(conn, splashManager);
         }

         // 26 -> 27
         if (currentDbVersion == 26) {
            currentDbVersion = newVersion = updateDbDesign_026_to_027(conn, splashManager);
         }

         // 27 -> 28
         boolean isPostUpdate28 = false;
         if (currentDbVersion == 27) {
            isPostUpdate28 = true;
            currentDbVersion = newVersion = updateDbDesign_027_to_028(conn, splashManager);
         }

         // 28 -> 29
         boolean isPostUpdate29 = false;
         if (currentDbVersion == 28) {
            isPostUpdate29 = true;
            currentDbVersion = newVersion = updateDbDesign_028_to_029(conn, splashManager);
         }

         // 29 -> 30
         if (currentDbVersion == 29) {
            currentDbVersion = newVersion = updateDbDesign_029_to_030(conn, splashManager);
         }

         // 30 -> 31
         if (currentDbVersion == 30) {
            currentDbVersion = newVersion = updateDbDesign_030_to_031(conn, splashManager);
         }

         // 31 -> 32
         boolean isPostUpdate32 = false;
         if (currentDbVersion == 31) {
            isPostUpdate32 = true;
            currentDbVersion = newVersion = updateDbDesign_031_to_032(conn, splashManager);
         }

         // 32 -> 33
         if (currentDbVersion == 32) {
            currentDbVersion = newVersion = updateDbDesign_032_to_033(conn, splashManager);
         }

         // 33 -> 34
         boolean isPostUpdate34 = false;
         if (currentDbVersion == 33) {
            isPostUpdate34 = true;
            currentDbVersion = newVersion = updateDbDesign_033_to_034(conn, splashManager);
         }

         // 34 -> 35
         if (currentDbVersion == 34) {
            currentDbVersion = newVersion = updateDbDesign_034_to_035(conn, splashManager);
         }

         // 35 -> 36
         if (currentDbVersion == 35) {
            currentDbVersion = newVersion = updateDbDesign_035_to_036(conn, splashManager);
         }

         // 36 -> 37
         boolean isPostUpdate37 = false;
         if (currentDbVersion == 36) {
            currentDbVersion = newVersion = updateDbDesign_036_to_037(conn, splashManager);
            isPostUpdate37 = true;
         }

         // 37 -> 38
         if (currentDbVersion == 37) {
            currentDbVersion = newVersion = updateDbDesign_037_to_038(conn, splashManager);
         }

         // 38 -> 39
         if (currentDbVersion == 38) {
            currentDbVersion = newVersion = updateDbDesign_038_to_039(conn, splashManager);
         }

         // 39 -> 40
         boolean isPostUpdate40 = false;
         if (currentDbVersion == 39) {
            currentDbVersion = newVersion = updateDbDesign_039_to_040(conn, splashManager);
            isPostUpdate40 = true;
         }

         // 40 -> 41
         if (currentDbVersion == 40) {
            currentDbVersion = newVersion = updateDbDesign_040_to_041(conn, splashManager);
         }

         // 41 -> 42
         boolean isPostUpdate42 = false;
         if (currentDbVersion == 41) {
            currentDbVersion = newVersion = updateDbDesign_041_to_042(conn, splashManager);
            isPostUpdate42 = true;
         }

         /*
          * Update version number
          */
         updateDbDesign_VersionNumber(conn, newVersion);

         /**
          * Do post update after the version number is updated because the post update uses
          * connections and entitymanager which is checking the version number.
          * <p>
          * Also the data structure must be updated otherwise the entity manager fails because the
          * data structure in the program code MUST be the same as in the database.
          */
         if (isPostUpdate5) {
            TourDatabase.computeAnyValues_ForAllTours(splashManager);
            TourManager.getInstance().removeAllToursFromCache();
         }
         if (isPostUpdate9) {
            updateDbDesign_008_009_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate11) {
            updateDbDesign_010_011_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate13) {
            updateDbDesign_012_013_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate20) {
            updateDbDesign_019_to_020_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate22) {
            updateDbDesign_021_to_022_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate23) {
            updateDbDesign_022_to_023_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate25) {
            updateDbDesign_024_to_025_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate28) {
            updateDbDesign_027_to_028_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate29) {
            updateDbDesign_028_to_029_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate32) {
            updateDbDesign_031_to_032_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate34) {
            updateDbDesign_033_to_034_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate37) {
            updateDbDesign_036_to_037_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate40) {
            updateDbDesign_039_to_040_PostUpdate(conn, splashManager);
         }
         if (isPostUpdate42) {
            updateDbDesign_041_to_042_PostUpdate(conn, splashManager);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
         _isSQLUpdateError = true;
         return false;
      }

      // display info for the successful update
      MessageDialog.openInformation(
            Display.getCurrent().getActiveShell(),
            Messages.tour_database_version_info_title,
            NLS.bind(Messages.Tour_Database_UpdateInfo, oldVersion, newVersion));

      return true;
   }

   private void updateDbDesign_001_002(final Connection conn) throws SQLException {

      final int dbVersion = 2;

      logDb_UpdateStart(dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType      BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_002_003(final Connection conn) throws SQLException {

      final int dbVersion = 3;

      logDb_UpdateStart(dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode         SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval   SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_003_004(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 4;

      logDb_UpdateStart(dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   maxAltitude            INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   maxPulse               INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   avgPulse               INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   avgCadence            INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   avgTemperature         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   maxSpeed               FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourTitle         VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_TOUR_TITLE + ")\n"); //$NON-NLS-1$
         exec(stmt, sql);

         sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourDescription   VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_TOUR_DESCRIPTION + ")\n"); //$NON-NLS-1$
         exec(stmt, sql);

         sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourStartPlace      VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_TOUR_START_PLACE + ")\n"); //$NON-NLS-1$
         exec(stmt, sql);

         sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourEndPlace      VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_TOUR_END_PLACE + ")\n"); //$NON-NLS-1$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   calories               INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   bikerWeight            FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   " + KEY_BIKE + "      BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         exec(stmt, sql);

         // from wolfgang
         sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   devicePluginName   VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + ")\n"); //$NON-NLS-1$
         exec(stmt, sql);

         // from wolfgang
         sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   deviceModeName      VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_DEVICE_MODE_NAME + ")\n"); //$NON-NLS-1$
         exec(stmt, sql);
      }

      stmt.close();

      // Create a EntityManagerFactory here, so we can access TourData with EJB
      if (splashManager != null) {
         splashManager.setMessage(Messages.Database_Monitor_persistent_service_task);
      }
      _emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

      if (splashManager != null) {
         splashManager.setMessage(Messages.Tour_Database_load_all_tours);
      }
      final ArrayList<Long> tourList = getAllTourIds();

      // loop over all tours and calculate and set new columns
      int tourIdx = 1;
      for (final Long tourId : tourList) {

         final TourData tourData = getTourFromDb(tourId);

         if (splashManager != null) {
            final String msg = NLS.bind(
                  Messages.Tour_Database_update_tour,
                  new Object[] { tourIdx++, tourList.size() });
            splashManager.setMessage(msg);
         }

         tourData.computeComputedValues();

         final TourPerson person = tourData.getTourPerson();
         tourData.setTourBike(person.getTourBike());
         tourData.setBodyWeight(person.getWeight());

         saveTour(tourData, false);
      }

      // cleanup everything as if nothing has happened
      _emFactory.close();
      _emFactory = null;

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_004_005(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 5;

      logDb_UpdateStart(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 5));
      }

      final Statement stmt = conn.createStatement();
      {
         createTable_TourTag(stmt);
         createTable_TourTag_Category(stmt);
         createIndex_TourData_005(stmt);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_005_006(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 6;

      logDb_UpdateStart(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 6));
      }

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   tourImportFilePath   VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + ")\n"; //$NON-NLS-1$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_006_007(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 7;

      logDb_UpdateStart(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 7));
      }

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeSourceTourId      BIGINT"; //            //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeTargetTourId      BIGINT"; //            //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedTourTimeOffset   INTEGER DEFAULT 0"; //   //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedAltitudeOffset   INTEGER DEFAULT 0"; //   //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startSecond            SMALLINT DEFAULT 0"; //   //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_007_008(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 8;

      logDb_UpdateStart(dbVersion);

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, 8));
      }

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   weatherWindDir         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   weatherWindSpd         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   isDistanceFromSensor   SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   weatherClouds         VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_WEATHER_CLOUDS + ")\n"; //$NON-NLS-1$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   restPulse            INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_008_009(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 9;

      logDb_UpdateStart(dbVersion);

      updateMonitor(splashManager, dbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startWeekYear      SMALLINT DEFAULT 1977 "; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private void updateDbDesign_008_009_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      // set app week number/year
      if (updateTourWeek(conn, splashManager)) {

         MessageDialog.openInformation(
               Display.getDefault().getActiveShell(),
               Messages.Database_Confirm_update_title,
               Messages.Tour_Database_Update_TourWeek_Info);
      }
   }

   private void updateDbDesign_009_010(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 10;

      logDb_UpdateStart(dbVersion);

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

         final String sql = //
               "ALTER TABLE " + TABLE_TOUR_DATA + "                                             \n" //$NON-NLS-1$ //$NON-NLS-2$
                     + "   ALTER COLUMN tourDescription                                          \n" //$NON-NLS-1$
                     + "   SET DATA TYPE   VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + ")         \n"; //$NON-NLS-1$ //$NON-NLS-2$

         exec(stmt, sql);
      }

      stmt.close();

      logDb_UpdateEnd(dbVersion);
   }

   private int updateDbDesign_010_011(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int dbVersion = 11;

      logDb_UpdateStart(dbVersion);

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

      logDb_UpdateEnd(dbVersion);

      return dbVersion;
   }

   /**
    * Set create date/time from the tour date
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_010_011_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final PreparedStatement stmtSelect = conn.prepareStatement( //
            //
            "SELECT" //                           //$NON-NLS-1$
                  //
                  + " StartYear," //                // 1 //$NON-NLS-1$
                  + " StartMonth," //             // 2 //$NON-NLS-1$
                  + " StartDay," //                // 3 //$NON-NLS-1$
                  + " StartHour," //                // 4 //$NON-NLS-1$
                  + " StartMinute," //             // 5 //$NON-NLS-1$
                  + " StartSecond" //             // 6 //$NON-NLS-1$
                  //
                  + " FROM " + TABLE_TOUR_DATA //      //$NON-NLS-1$
                  + " WHERE TourId=?" //            $NON-NLS-1$ //$NON-NLS-1$
      );

      final PreparedStatement stmtUpdate = conn.prepareStatement( //
            //
            "UPDATE " + TABLE_TOUR_DATA //            //$NON-NLS-1$
            //
                  + " SET" //                     //$NON-NLS-1$
                  //
                  + " DateTimeCreated=?" //         // 1 //$NON-NLS-1$
                  //
                  + " WHERE tourId=?"); //         // 2 //$NON-NLS-1$

      int tourIdx = 1;
      final ArrayList<Long> tourList = getAllTourIds();

      // loop: all tours
      for (final Long tourId : tourList) {

         if (splashManager != null) {
            splashManager.setMessage(
                  NLS.bind(//
                        Messages.Tour_Database_PostUpdate011_SetTourCreateTime,
                        new Object[] { tourIdx++, tourList.size() }));
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
   }

   private int updateDbDesign_011_012(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 12;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
//            + "   IsPulseSensorPresent      INTEGER DEFAULT 0,             \n" //$NON-NLS-1$
//            + "   IsPowerSensorPresent      INTEGER DEFAULT 0,             \n" //$NON-NLS-1$
//            + "   DeviceAvgSpeed            FLOAT DEFAULT 0,            \n" //$NON-NLS-1$
//            + "   DeviceFirmwareVersion   " + varCharKomma(TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION)) //$NON-NLS-1$

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   IsPulseSensorPresent   INTEGER   DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   IsPowerSensorPresent   INTEGER   DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   DeviceAvgSpeed         FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   DeviceFirmwareVersion   VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + ")\n"; //$NON-NLS-1$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_012_013(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 13;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

//         + "   TemperatureScale         INTEGER DEFAULT 1,             \n" //$NON-NLS-1$
//         + " Weather                " + varCharNoKomma(TourData.DB_LENGTH_WEATHER) //$NON-NLS-1$

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   TemperatureScale      INTEGER   DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   Weather               VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
               + TourData.DB_LENGTH_WEATHER + ")\n"; //$NON-NLS-1$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set temperature scale default value
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_012_013_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final String sql = "UPDATE " + TABLE_TOUR_DATA + " SET TemperatureScale=1"; //$NON-NLS-1$ //$NON-NLS-2$

      System.out.println(sql);
      System.out.println();

      conn.createStatement().executeUpdate(sql);
   }

   private int updateDbDesign_013_014(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 14;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

//      + "   ConconiDeflection         INTEGER DEFAULT 0,             \n" //$NON-NLS-1$

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN ConconiDeflection         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_014_015(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 15;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

//      TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//      + "   BirthDay         BIGINT DEFAULT 0,                  \n" //$NON-NLS-1$

      String sql;
      final Statement stmt = conn.createStatement();
      {
         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN BirthDay         BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_015_to_016(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 16;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {

         createTable_TourPersonHRZone(stmt);

//      TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//      + "   Gender            INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
//      + "   RestPulse         INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
//      + "   MaxPulse         INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
//      + "   HrMaxFormula      INTEGER DEFAULT 0,                  \n" //$NON-NLS-1$
//
//      TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN Gender            INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN RestPulse         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN MaxPulse         INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN HrMaxFormula      INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_016_to_017(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 17;

      logDb_UpdateStart(newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {

//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//
//         + "   hrZone0                  INTEGER DEFAULT -1,            \n" //$NON-NLS-1$
//         + "   hrZone1                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone2                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone3                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone4                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone5                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone6                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone7                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone8                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
//         + "   hrZone9                  INTEGER DEFAULT -1,          \n" //$NON-NLS-1$
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

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_017_to_018(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 18;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {

         if (_dbVersionBeforeUpdate > 16) {

            /*
             * db update 16 creates the HR zone db, doing this update causes an sql exception
             * because the fields are already available
             */

//            TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE
//
//            // version 18 start
//            //
//            + "   ColorRed         INTEGER DEFAULT 0,                \n" //$NON-NLS-1$
//            + "   ColorGreen         INTEGER DEFAULT 0,                \n" //$NON-NLS-1$
//            + "   ColorBlue         INTEGER DEFAULT 0,                \n" //$NON-NLS-1$
//            //
//            // version 18 end ---------
//
//            TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE   TABLE_TOUR_PERSON_HRZONE

            sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorRed      INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorGreen   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorBlue   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);
         }

//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//
//         // version 18 start
//         //
//         + "   NumberOfHrZones            INTEGER DEFAULT 0,             \n" //$NON-NLS-1$
//         //
//         // version 18 end ---------
//
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA

         sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN NumberOfHrZones   INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_018_to_019(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 19;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      String sql;
      final Statement stmt = conn.createStatement();
      {
//         TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE
//         //
//         // version 19 start
//         //
//         + "   colorTextRed       SMALLINT DEFAULT 0,                  \n" //$NON-NLS-1$
//         + "   colorTextGreen       SMALLINT DEFAULT 0,                  \n" //$NON-NLS-1$
//         + "   colorTextBlue       SMALLINT DEFAULT 0                  \n" //$NON-NLS-1$
//         //
//         // version 19 end ---------
//         //
//         TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE   TOUR_TYPE

         sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextRed      SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextGreen   SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextBlue      SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_019_to_020(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 20;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      {
         updateDbDesign_019_to_020_10DataSerieBlobSize(conn);
         updateDbDesign_019_to_020_20AlterColumns(conn);
      }

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Increase {@link TourData#serieData} blob size.
    *
    * @param conn
    * @throws SQLException
    */
   private void updateDbDesign_019_to_020_10DataSerieBlobSize(final Connection conn) throws SQLException {

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

   private void updateDbDesign_019_to_020_20AlterColumns(final Connection conn) throws SQLException {

      String sql;
      final Statement stmt = conn.createStatement();
      {
         // remove the NOT NULL constraint from the "distance" column
         sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ALTER COLUMN distance   NULL"; //$NON-NLS-1$ //$NON-NLS-2$
         exec(stmt, sql);

         //         + "   distance20                FLOAT DEFAULT 0,               \n" //$NON-NLS-1$
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
   private void updateDbDesign_019_to_020_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      IS_POST_UPDATE_019_to_020 = true;

      final long startTime = System.currentTimeMillis();

      int tourIdx = 1;
      final ArrayList<Long> tourList = getAllTourIds();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      try {

         // loop: all tours
         for (final Long tourId : tourList) {

            final long currentTime = System.currentTimeMillis();

            if (splashManager != null) {

               final float durationInSeconds = (currentTime - startTime) / 1000;

               splashManager.setMessage(
                     NLS.bind(//
                           Messages.Tour_Database_PostUpdate020_ConvertIntToFloat,
                           new Object[] { tourIdx, tourList.size(), (int) durationInSeconds }));

               tourIdx++;
            }

            final TourData tourData = em.find(TourData.class, tourId);
            if (tourData != null) {

               tourData.updateDatabase_019_to_020();

               TourDatabase.saveEntity(tourData, tourId, TourData.class);
            }

         }

      } catch (final Exception e) {
         e.printStackTrace();
      } finally {

         IS_POST_UPDATE_019_to_020 = false;

         em.close();
      }
   }

   private int updateDbDesign_020_to_021(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 21;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
//         TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA   TOURDATA
//         // version 21 start
//         //
//         + "   maxAltitude               FLOAT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   maxPulse               FLOAT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   avgPulse               FLOAT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   avgCadence               FLOAT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   avgTemperature            FLOAT DEFAULT 0,            \n" //$NON-NLS-1$
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

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_021_to_022(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 22;

      logDb_UpdateStart(newDbVersion);

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
//         + "   TourStartTime            BIGINT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   TourEndTime               BIGINT DEFAULT 0,            \n" //$NON-NLS-1$
//
//         + "   TourRecordingTime          BIGINT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   TourDrivingTime          BIGINT DEFAULT 0,            \n" //$NON-NLS-1$
            //
            // version 22 end ---------

            sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TourStartTime      BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TourEndTime      BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);

            /*
             * modify columns
             */
            int no = 0;
            final int max = 2;

            modifyColumn_Type(TABLE_TOUR_DATA, "TourRecordingTime", "BIGINT DEFAULT 0", stmt, splashManager, ++no, max); //         //$NON-NLS-1$ //$NON-NLS-2$
            modifyColumn_Type(TABLE_TOUR_DATA, "TourDrivingTime", "BIGINT DEFAULT 0", stmt, splashManager, ++no, max); //            //$NON-NLS-1$ //$NON-NLS-2$

            createIndex_TourData_022(stmt);
         }

         if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "IsMarkerVisible") == false) {//$NON-NLS-1$

            // table columns are not yet created

//         TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER   TOURMARKER
//
//         // Version 22 - begin
//         //
//         + "   IsMarkerVisible            INTEGER DEFAULT 0,             \n" //$NON-NLS-1$
//         //
//         // Version 22 - end

            sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN IsMarkerVisible      INTEGER DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
            exec(stmt, sql);
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set tour start/end time from the tour date and duration
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_021_to_022_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final PreparedStatement stmtSelect = conn.prepareStatement( //
            //
            "SELECT" //                           //$NON-NLS-1$
                  //
                  + " StartYear," //                // 1 //$NON-NLS-1$
                  + " StartMonth," //             // 2 //$NON-NLS-1$
                  + " StartDay," //                // 3 //$NON-NLS-1$
                  + " StartHour," //                // 4 //$NON-NLS-1$
                  + " StartMinute," //             // 5 //$NON-NLS-1$
                  + " StartSecond," //             // 6 //$NON-NLS-1$
                  + " tourRecordingTime" //          // 7 //$NON-NLS-1$
                  //
                  + " FROM " + TABLE_TOUR_DATA //      //$NON-NLS-1$
                  + " WHERE TourId=?" //            $NON-NLS-1$ //$NON-NLS-1$
      );

      final PreparedStatement stmtUpdate = conn.prepareStatement( //
            //
            "UPDATE " + TABLE_TOUR_DATA //            //$NON-NLS-1$
            //
                  + " SET" //                     //$NON-NLS-1$
                  //
                  + " TourStartTime=?," //         // 1 //$NON-NLS-1$
                  + " TourEndTime=?" //         // 2 //$NON-NLS-1$
                  //
                  + " WHERE tourId=?"); //         // 3 //$NON-NLS-1$

      int tourIndex = 1;
      final ArrayList<Long> tourList = getAllTourIds();

      // loop: all tours
      for (final Long tourId : tourList) {

         if (splashManager != null) {
            splashManager.setMessage(
                  NLS.bind(//
                        Messages.Tour_Database_PostUpdate021_SetTourStartEndTime,
                        new Object[] { tourIndex++, tourList.size() }));
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
   }

   private int updateDbDesign_022_to_023(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 23;

      logDb_UpdateStart(newDbVersion);

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
//         + "   imageExifTime            BIGINT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   imageFileLastModified      BIGINT DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   adjustedTime            BIGINT DEFAULT 0,               \n" //$NON-NLS-1$
//         //
//         + "   ratingStars               INT DEFAULT 0,               \n" //$NON-NLS-1$
//         //
//         + "   isGeoFromPhoto            INT DEFAULT 0,               \n" //$NON-NLS-1$
//         + "   latitude                DOUBLE DEFAULT 0,            \n" //$NON-NLS-1$
//         + "   longitude                DOUBLE DEFAULT 0            \n" //$NON-NLS-1$
//         //
//         // version 23 end

            final String sqlTourPhoto[] = {
                  //
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFileName         VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
                        + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFileExt         VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
                        + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFilePath         VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
                        + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFilePathName      VARCHAR(" //$NON-NLS-1$//$NON-NLS-2$
                        + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$
                  //
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageExifTime         BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   imageFileLastModified   BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   adjustedTime         BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  //
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   ratingStars            INT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  //
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   isGeoFromPhoto         INT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   latitude             DOUBLE DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN   longitude             DOUBLE DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
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
//         + " numberOfTimeSlices         INTEGER DEFAULT 0,            \n" //$NON-NLS-1$
//         + " numberOfPhotos            INTEGER DEFAULT 0,            \n" //$NON-NLS-1$
//         + " photoTimeAdjustment         INTEGER DEFAULT 0,            \n" //$NON-NLS-1$
//         //
//         // version 23 end ---------

            final String sqlTourData[] = {
                  //
                  "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   numberOfTimeSlices      INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   numberOfPhotos         INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN   photoTimeAdjustment      INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
                  //
            };

            exec(stmt, sqlTourData);
         }
      }

      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_022_to_023_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      int tourIdx = 1;
      final ArrayList<Long> tourList = getAllTourIds();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      try {

         // loop: all tours
         for (final Long tourId : tourList) {

            if (splashManager != null) {

               splashManager.setMessage(
                     NLS.bind(//
                           Messages.Tour_Database_PostUpdate023_SetTimeSliceNumbers,
                           new Object[] { tourIdx, tourList.size() }));

               tourIdx++;
            }

            final TourData tourData = em.find(TourData.class, tourId);
            if (tourData != null) {

               // compute number of time slices
               tourData.onPrePersist();

               TourDatabase.saveEntity(tourData, tourId, TourData.class);
            }
         }

      } catch (final Exception e) {
         e.printStackTrace();
      } finally {

         em.close();
      }
   }

   private int updateDbDesign_023_to_024(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 24;

      logDb_UpdateStart(newDbVersion);

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
               SQL.Cleanup_DropConstraint(stmt, JOINTABLE__TOURDATA__TOURTAG, "FK_TOURDATA_TOURTAG_TOURTAG_TagID"); //$NON-NLS-1$
            }

            /*
             * Table: TOURMARKER
             */
            {
               /*
                * Adjust column with to TourWayPoint width, that both have the same max size.
                */
               SQL.AlterCol_VarChar_Width(stmt, TABLE_TOUR_MARKER, "label", TourWayPoint.DB_LENGTH_NAME); //$NON-NLS-1$
               SQL.AlterCol_VarChar_Width(stmt, TABLE_TOUR_MARKER, "category", TourWayPoint.DB_LENGTH_CATEGORY); //$NON-NLS-1$

               /*
                * Add new columns
                */
               SQL.AddCol_VarCar(stmt, TABLE_TOUR_MARKER, "description", TourWayPoint.DB_LENGTH_DESCRIPTION); //$NON-NLS-1$
               SQL.AddCol_VarCar(stmt, TABLE_TOUR_MARKER, "urlText", TourMarker.DB_LENGTH_URL_TEXT); //$NON-NLS-1$
               SQL.AddCol_VarCar(stmt, TABLE_TOUR_MARKER, "urlAddress", TourMarker.DB_LENGTH_URL_ADDRESS); //$NON-NLS-1$
            }
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_024_to_025(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 25;

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 25
         if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "tourTime") == false) { //$NON-NLS-1$

            // Table: TOURMARKER
            {
               // Add new columns
               SQL.AddCol_BigInt(stmt, TABLE_TOUR_MARKER, "tourTime", SQL_LONG_MIN_VALUE);//$NON-NLS-1$
               SQL.AddCol_Float(stmt, TABLE_TOUR_MARKER, "altitude", SQL_FLOAT_MIN_VALUE); //$NON-NLS-1$
               SQL.AddCol_Double(stmt, TABLE_TOUR_MARKER, "latitude", SQL_DOUBLE_MIN_VALUE); //$NON-NLS-1$
               SQL.AddCol_Double(stmt, TABLE_TOUR_MARKER, "longitude", SQL_DOUBLE_MIN_VALUE); //$NON-NLS-1$
            }
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   public void updateDbDesign_024_to_025_PostUpdate(final Connection conn, final Object progress)
         throws SQLException {

      int tourIdx = 1;
      final ArrayList<Long> tourList = getAllTourIds();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      try {

         // loop: all tours
         for (final Long tourId : tourList) {

            if (progress instanceof SplashManager) {

               final SplashManager splashManager = (SplashManager) progress;

               splashManager.setMessage(
                     NLS.bind(//
                           Messages.Tour_Database_PostUpdate025_SetMarkerFields,
                           new Object[] { tourIdx, tourList.size() }));

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

                  final String message = String
                        .format(
                              "Tour: %s - Tour marker: %s - serieIndex: %d - serie length: %d - relative time: %d sec", //$NON-NLS-1$
                              TourManager.getTourDateTimeShort(tourData),
                              tourMarker.getLabel(),
                              tourMarker.getSerieIndex(),
                              serieLength,
                              tourMarker.getTime());

                  StatusUtil.showStatus(message, e);
               }

            }

            // check before tour is saved

            if (progress instanceof IProgressMonitor) {

               final IProgressMonitor progressMonitor = (IProgressMonitor) em;

               if (progressMonitor.isCanceled()) {
                  return;
               }
            }

            TourDatabase.saveEntity(tourData, tourId, TourData.class);
         }

      } catch (final Exception e) {

         StatusUtil.showStatus(e);

         throw e;

      } finally {

         em.close();
      }
   }

   private int updateDbDesign_025_to_026(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 26;

      /**
       * This version is never used but is not deleted to keep this info.
       */

      logDb_UpdateStart(newDbVersion);

      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 26
//         if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "tourTime") == false) { //$NON-NLS-1$
//
//            // Table: TOURMARKER
//            {
//               // Add new columns
//               SQL.AddCol_BigInt(stmt, TABLE_TOUR_MARKER, "tourTime", SQL_LONG_MIN_VALUE);//$NON-NLS-1$
//               SQL.AddCol_Float(stmt, TABLE_TOUR_MARKER, "altitude", SQL_FLOAT_MIN_VALUE); //$NON-NLS-1$
//               SQL.AddCol_Double(stmt, TABLE_TOUR_MARKER, "latitude", SQL_DOUBLE_MIN_VALUE); //$NON-NLS-1$
//               SQL.AddCol_Double(stmt, TABLE_TOUR_MARKER, "longitude", SQL_DOUBLE_MIN_VALUE); //$NON-NLS-1$
//
//            }
//         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_026_to_027(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 27;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 27
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "frontShiftCount") == false) { //$NON-NLS-1$

            // Table: TABLE_TOUR_DATA
            {
               // Add new columns
               SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "frontShiftCount", DEFAULT_0);//$NON-NLS-1$
               SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "rearShiftCount", DEFAULT_0); //$NON-NLS-1$
            }
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_027_to_028(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 28;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 28
         if (isColumnAvailable(conn, TABLE_TOUR_COMPARED, "AvgPulse") == false) { //$NON-NLS-1$

            /*
             * Table: TABLE_TOUR_COMPARED
             */
            {
               /*
                * Add new columns
                */
               SQL.AddCol_Float(stmt, TABLE_TOUR_COMPARED, "AvgPulse", DEFAULT_0); //$NON-NLS-1$
            }
         }

         // check if db is updated to version 28
         if (isColumnAvailable(conn, TABLE_TOUR_WAYPOINT, "urlText") == false) { //$NON-NLS-1$

            /*
             * Table: TABLE_TOUR_WAYPOINT
             */
            {
               /*
                * Add new columns
                */
               SQL.AddCol_VarCar(stmt, TABLE_TOUR_WAYPOINT, "urlText", TourMarker.DB_LENGTH_URL_TEXT); //$NON-NLS-1$
               SQL.AddCol_VarCar(stmt, TABLE_TOUR_WAYPOINT, "urlAddress", TourMarker.DB_LENGTH_URL_ADDRESS); //$NON-NLS-1$
            }
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_027_to_028_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      // get number of compared tours
      final String sql = "SELECT COUNT(*) FROM " + TourDatabase.TABLE_TOUR_COMPARED; //$NON-NLS-1$

      final PreparedStatement stmt = conn.prepareStatement(sql);
      ResultSet result = stmt.executeQuery();

      // get first result
      result.next();

      // get first value
      final int numberOfComparedTours = result.getInt(1);
      if (numberOfComparedTours == 0) {
         return;
      }

      final PreparedStatement stmtSelect = conn.prepareStatement( //
            //
            "SELECT" //                           //$NON-NLS-1$
                  //
                  + " comparedId," //             // 1 //$NON-NLS-1$
                  + " tourId," //                // 2 //$NON-NLS-1$
                  + " startIndex," //             // 3 //$NON-NLS-1$
                  + " endIndex" //                // 4 //$NON-NLS-1$
                  //
                  + " FROM " + TourDatabase.TABLE_TOUR_COMPARED //$NON-NLS-1$
      );

      final PreparedStatement stmtUpdate = conn.prepareStatement( //
            //
            "UPDATE " + TABLE_TOUR_COMPARED //         //$NON-NLS-1$
            //
                  + " SET" //                     //$NON-NLS-1$
                  //
                  + " avgPulse=?" //               // 1 //$NON-NLS-1$
                  //
                  + " WHERE comparedId=?"); //      // 2 //$NON-NLS-1$

      result = stmtSelect.executeQuery();

      int compTourCounter = 0;

      while (result.next()) {

         if (splashManager != null) {
            splashManager.setMessage(
                  NLS.bind(//
                        Messages.Tour_Database_PostUpdate_028_SetAvgPulse,
                        new Object[] { ++compTourCounter, numberOfComparedTours }));
         }

         // get date from database
         final long compareId = result.getLong(1);
         final long tourId = result.getLong(2);
         final int startIndex = result.getInt(3);
         final int endIndex = result.getInt(4);

         final TourData tourData = TourManager.getTour(tourId);

         if (tourData == null) {

            StatusUtil.log(
                  NLS.bind(
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

   private int updateDbDesign_028_to_029(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 29;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (final Statement stmt = conn.createStatement()) {

         // check if db is updated to version 29
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "TourImportFileName") == false) { //$NON-NLS-1$

            // TABLE_TOUR_DATA: add column TourImportFileName
            SQL.AddCol_VarCar(
                  stmt, //
                  TABLE_TOUR_DATA,
                  "TourImportFileName", //$NON-NLS-1$
                  TourData.DB_LENGTH_TOUR_IMPORT_FILE_NAME);

            createIndex_TourData_029(stmt);
         }
      }

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set tour start/end time from the tour date and duration
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_028_to_029_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final int numTours = getAllTourIds().size();

      final PreparedStatement stmtSelect = conn.prepareStatement(
            UI.EMPTY_STRING//
                  //
                  + "SELECT" //                           //$NON-NLS-1$
                  //
                  + " TourID," //                // 1 //$NON-NLS-1$
                  + " TourImportFilePath" //          // 2 //$NON-NLS-1$
                  //
                  + " FROM " + TABLE_TOUR_DATA //      //$NON-NLS-1$
      );

      final PreparedStatement stmtUpdate = conn.prepareStatement(
            UI.EMPTY_STRING//
                  //
                  + "UPDATE " + TABLE_TOUR_DATA //            //$NON-NLS-1$
                  //
                  + " SET" //                     //$NON-NLS-1$
                  //
                  + " TourImportFileName=?," //      // 1 //$NON-NLS-1$
                  + " TourImportFilePath=?" //      // 1 //$NON-NLS-1$
                  //
                  + " WHERE TourID=?"); //         // 2 //$NON-NLS-1$

      int tourIndex = 0;

      final ResultSet result = stmtSelect.executeQuery();

      while (result.next()) {

         if (splashManager != null) {
            splashManager.setMessage(
                  NLS.bind(//
                        Messages.Tour_Database_PostUpdate_029_SetImportFileName,
                        new Object[] { ++tourIndex, numTours }));
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
               stmtUpdate.setString(
                     2,
                     folderPath == null //
                           ? UI.EMPTY_STRING
                           : folderPath.toString());

               stmtUpdate.setLong(3, dbTourId);

               stmtUpdate.executeUpdate();
            }
         }
      }
   }

   private int updateDbDesign_029_to_030(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 30;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 30
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "power_Avg") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_Avg",                         DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Int   (stmt, TABLE_TOUR_DATA, "power_Max",                         DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Int   (stmt, TABLE_TOUR_DATA, "power_Normalized",                  DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Int   (stmt, TABLE_TOUR_DATA, "power_FTP",                         DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_BigInt(stmt, TABLE_TOUR_DATA, "power_TotalWork",                   DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_TrainingStressScore",         DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_IntensityFactor",             DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_Int   (stmt, TABLE_TOUR_DATA, "power_PedalLeftRightBalance",       DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_AvgLeftTorqueEffectiveness",  DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_AvgRightTorqueEffectiveness", DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_AvgLeftPedalSmoothness",      DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float (stmt, TABLE_TOUR_DATA, "power_AvgRightPedalSmoothness",     DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_030_to_031(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 31;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 31
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "CadenceMultiplier") == false) { //$NON-NLS-1$

            // Add new columns
            SQL.AddCol_Float(stmt, TABLE_TOUR_DATA, "CadenceMultiplier", DEFAULT_1_0); //$NON-NLS-1$
            SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "IsStrideSensorPresent", DEFAULT_0); //$NON-NLS-1$
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_031_to_032(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 32;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 32
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "TimeZoneId") == false) { //$NON-NLS-1$

            // TABLE_TOUR_DATA: add column TimeZoneId
            SQL.AddCol_VarCar(stmt, TABLE_TOUR_DATA, "TimeZoneId", TourData.DB_LENGTH_TIME_ZONE_ID); //$NON-NLS-1$
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set timezone in a tour to the tour starting point when lat/lon is available.
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_031_to_032_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      int tourIdx = 1;
      final ArrayList<Long> tourList = getAllTourIds();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      try {

         long lastUpdateTime = System.currentTimeMillis();

         // loop: all tours
         for (final Long tourId : tourList) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > 500) {

                  lastUpdateTime = currentTime;

                  final String percent = String.format("%.1f", (float) tourIdx / tourList.size() * 100.0);//$NON-NLS-1$

                  splashManager.setMessage(
                        NLS.bind(//
                              Messages.Tour_Database_PostUpdate_032_SetTourTimeZone,
                              new Object[] { tourIdx, tourList.size(), percent }));
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

               TourDatabase.saveEntity(tourData, tourId, TourData.class);
            }
         }

      } catch (final Exception e) {
         e.printStackTrace();
      } finally {

         em.close();
      }
   }

   private int updateDbDesign_032_to_033(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 33;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      try (final Statement stmt = conn.createStatement()) {

         // check if db already contains the index
         if (isIndexAvailable(conn, TABLE_TOUR_DATA, "StartWeek") == false) { //$NON-NLS-1$

            createIndex_TourData_033(stmt);
         }
      }

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_033_to_034(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 34;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // double check if db already updated
         if (isTableAvailable(conn, TABLE_TOUR_GEO_PARTS) == false) {

            createTable_TourGeoParts(stmt);
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set geo parts
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_033_to_034_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final ArrayList<Long> allTours = getAllTourIds();

      final int numTours = allTours.size();
      int tourIndex = 1;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      final long startTime = System.currentTimeMillis();

      try {

         long lastUpdateTime = startTime;

         // loop: all tours
         for (final Long tourId : allTours) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > 500) {

                  lastUpdateTime = currentTime;

                  final String percent = String.format("%.1f", (float) tourIndex / numTours * 100.0);//$NON-NLS-1$

                  splashManager.setMessage(
                        NLS.bind(//
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
         splashManager.setMessage(
               NLS.bind(//
                     Messages.Tour_Database_PostUpdate_034_SetTourGeoParts,
                     new Object[] { tourIndex - 1, numTours, 100 }));

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         em.close();
      }

      final long timeDiff = System.currentTimeMillis() - startTime;

      StatusUtil.logInfo(
            String.format(
                  "Database postupdate 33->34 in %s mm:ss", //$NON-NLS-1$
                  net.tourbook.common.UI.formatHhMmSs(timeDiff / 1000)));
   }

   private int updateDbDesign_034_to_035(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 35;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 35
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "runDyn_StanceTime_Min") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTime_Min",             DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTime_Max",             DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float   (stmt, TABLE_TOUR_DATA, "runDyn_StanceTime_Avg",             DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTimeBalance_Min",    DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StanceTimeBalance_Max",    DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float   (stmt, TABLE_TOUR_DATA, "runDyn_StanceTimeBalance_Avg",    DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StepLength_Min",             DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_StepLength_Max",             DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float   (stmt, TABLE_TOUR_DATA, "runDyn_StepLength_Avg",             DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalOscillation_Min",    DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalOscillation_Max",    DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float   (stmt, TABLE_TOUR_DATA, "runDyn_VerticalOscillation_Avg",    DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalRatio_Min",          DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "runDyn_VerticalRatio_Max",          DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float   (stmt, TABLE_TOUR_DATA, "runDyn_VerticalRatio_Avg",          DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_035_to_036(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 36;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 36
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "surfing_NumberOfEvents") == false) { //$NON-NLS-1$

//          // version 36 start  -  18.12
//          //
//            + "   surfing_NumberOfEvents                 SMALLINT DEFAULT 0,                 \n" //$NON-NLS-1$
//            + "   surfing_MinSpeed_StartStop             SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$
//            + "   surfing_MinSpeed_Surfing               SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$
//            + "   surfing_MinTimeDuration                SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$
//
//            + "   surfing_IsMinDistance                  BOOLEAN  DEFAULT FALSE,             \n" //$NON-NLS-1$
//            + "   surfing_MinDistance                    SMALLINT DEFAULT -1,                \n" //$NON-NLS-1$
//
//          //
//          // version 36 end ---------

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_NumberOfEvents",          DEFAULT_0); //$NON-NLS-1$

            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinDistance",             DEFAULT_IGNORED); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinSpeed_StartStop",      DEFAULT_IGNORED); //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinSpeed_Surfing",        DEFAULT_IGNORED); //$NON-NLS-1$

            SQL.AddCol_Boolean (stmt, TABLE_TOUR_DATA, "surfing_IsMinDistance",           DEFAULT_FALSE);   //$NON-NLS-1$
            SQL.AddCol_SmallInt(stmt, TABLE_TOUR_DATA, "surfing_MinTimeDuration",         DEFAULT_IGNORED); //$NON-NLS-1$

// SET_FORMATTING_ON

         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_036_to_037(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 37;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if column is available
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "HasGeoData") == false) { //$NON-NLS-1$

            SQL.AddCol_Boolean(stmt, TABLE_TOUR_DATA, "HasGeoData", DEFAULT_FALSE); //$NON-NLS-1$

            createIndex_TourData_037(stmt);
         }

         // check if db is available
         if (isTableAvailable(conn, TABLE_TOUR_GEO_PARTS)) {

            // check if db already contains the constraint
            if (isPrimaryKeyAvailable(conn, TABLE_TOUR_GEO_PARTS, "PK_TourId_GeoPart") == false) { //$NON-NLS-1$

               final String sql = UI.EMPTY_STRING//

                     + "ALTER TABLE " + TABLE_TOUR_GEO_PARTS //                                 //$NON-NLS-1$
                     + "   ADD CONSTRAINT PK_TourId_GeoPart PRIMARY KEY (TourId, GeoPart)"; //  //$NON-NLS-1$

               exec(stmt, sql);
            }
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set hasGeoData
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_036_to_037_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final ArrayList<Long> allTours = getAllTourIds();

      final int numTours = allTours.size();
      int tourIndex = 1;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      final long startTime = System.currentTimeMillis();

      try {

         long lastUpdateTime = startTime;

         final PreparedStatement stmtUpdate = conn.prepareStatement( //

               "UPDATE " + TABLE_TOUR_DATA //         //$NON-NLS-1$

                     + " SET" //                      //$NON-NLS-1$

                     + " HasGeoData=?" //             // 1 //$NON-NLS-1$

                     + " WHERE tourId=?"); //         //$NON-NLS-1$

         // loop: all tours
         for (final Long tourId : allTours) {

            if (splashManager != null) {

               final long currentTime = System.currentTimeMillis();
               final float timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > 500) {

                  lastUpdateTime = currentTime;

                  final String percent = String.format("%.1f", (float) tourIndex / numTours * 100.0);//$NON-NLS-1$

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
         splashManager.setMessage(
               NLS.bind(//
                     Messages.Tour_Database_PostUpdate_037_SetHasGeoData,
                     new Object[] { tourIndex - 1, numTours, 100 }));

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         em.close();
      }

      final long timeDiff = System.currentTimeMillis() - startTime;

      StatusUtil.logInfo(String.format(
            "Database postupdate 36 -> 37 in %s mm:ss", //$NON-NLS-1$
            net.tourbook.common.UI.formatHhMmSs(timeDiff / 1000)));
   }

   private int updateDbDesign_037_to_038(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 38;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 38
         if (isColumnAvailable(conn, TABLE_TOUR_TAG, "notes") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_VarCar (stmt, TABLE_TOUR_TAG,              "notes", TourTag.DB_LENGTH_NOTES);                 //$NON-NLS-1$
            SQL.AddCol_VarCar (stmt, TABLE_TOUR_TAG_CATEGORY,     "notes", TourTag.DB_LENGTH_NOTES);                 //$NON-NLS-1$

            SQL.AddCol_Float  (stmt, TABLE_TOUR_DATA,             "training_TrainingEffect_Aerob",       DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float  (stmt, TABLE_TOUR_DATA,             "training_TrainingEffect_Anaerob",     DEFAULT_0); //$NON-NLS-1$
            SQL.AddCol_Float  (stmt, TABLE_TOUR_DATA,             "training_TrainingPerformance",        DEFAULT_0); //$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_038_to_039(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 39;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 39
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "isWeatherDataFromApi") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_Boolean   (stmt, TABLE_TOUR_DATA, "isWeatherDataFromApi",          DEFAULT_FALSE);   //$NON-NLS-1$
            SQL.AddCol_SmallInt  (stmt, TABLE_TOUR_DATA, "weather_Humidity",              DEFAULT_0);       //$NON-NLS-1$
            SQL.AddCol_Float     (stmt, TABLE_TOUR_DATA, "weather_Precipitation",         DEFAULT_0);       //$NON-NLS-1$
            SQL.AddCol_Float     (stmt, TABLE_TOUR_DATA, "weather_Pressure",              DEFAULT_0);       //$NON-NLS-1$
            SQL.AddCol_Float     (stmt, TABLE_TOUR_DATA, "weather_Temperature_Min",       DEFAULT_0);       //$NON-NLS-1$
            SQL.AddCol_Float     (stmt, TABLE_TOUR_DATA, "weather_Temperature_Max",       DEFAULT_0);       //$NON-NLS-1$
            SQL.AddCol_Float     (stmt, TABLE_TOUR_DATA, "weather_Temperature_WindChill", DEFAULT_0);       //$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_039_to_040(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 40;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 40
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "power_DataSource") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_VarCar (stmt, TABLE_TOUR_DATA, "power_DataSource",              TourData.DB_LENGTH_POWER_DATA_SOURCE); //$NON-NLS-1$
            SQL.AddCol_Int    (stmt, TABLE_TOUR_DATA, "cadenceZone_SlowTime",          DEFAULT_0);                            //$NON-NLS-1$
            SQL.AddCol_Int    (stmt, TABLE_TOUR_DATA, "cadenceZone_FastTime",          DEFAULT_0);                            //$NON-NLS-1$
            SQL.AddCol_Int    (stmt, TABLE_TOUR_DATA, "cadenceZones_DelimiterValue",   DEFAULT_0);                            //$NON-NLS-1$
            SQL.AddCol_Int    (stmt, TABLE_TOUR_DATA, "avgAltitudeChange",             DEFAULT_0);                            //$NON-NLS-1$

            SQL.AddCol_Int    (stmt, TABLE_TOUR_COMPARED, "tourRecordingTime",         DEFAULT_0);                            //$NON-NLS-1$

            // Create index in table: TOURDATA_TOURTAG - Index: TOURTAG_TAGID
            SQL.CreateIndex   (stmt, JOINTABLE__TOURDATA__TOURTAG, KEY_TAG);

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   /**
    * Set tourRecordingTime
    *
    * @param conn
    * @param splashManager
    * @throws SQLException
    */
   private void updateDbDesign_039_to_040_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      // get number of compared tours
      final String sql = "SELECT COUNT(*) FROM " + TourDatabase.TABLE_TOUR_COMPARED; //$NON-NLS-1$

      final PreparedStatement stmt = conn.prepareStatement(sql);
      ResultSet result = stmt.executeQuery();

      // get first result
      result.next();

      // get first value
      final int numberOfComparedTours = result.getInt(1);
      if (numberOfComparedTours == 0) {
         return;
      }

      final long startTime = System.currentTimeMillis();

      final PreparedStatement stmtSelect = conn.prepareStatement( //
            //
            "SELECT" //                           //$NON-NLS-1$
                  //
                  + " comparedId," //             // 1 //$NON-NLS-1$
                  + " tourId," //                 // 2 //$NON-NLS-1$
                  + " startIndex," //             // 3 //$NON-NLS-1$
                  + " endIndex" //                // 4 //$NON-NLS-1$
                  //
                  + " FROM " + TourDatabase.TABLE_TOUR_COMPARED //$NON-NLS-1$
      );

      final PreparedStatement stmtUpdate = conn.prepareStatement( //
            //
            "UPDATE " + TABLE_TOUR_COMPARED //    //$NON-NLS-1$
            //
                  + " SET" //                     //$NON-NLS-1$
                  //
                  + " tourRecordingTime=?" //     // 1 //$NON-NLS-1$
                  //
                  + " WHERE comparedId=?"); //    // 2 //$NON-NLS-1$

      result = stmtSelect.executeQuery();

      int compTourCounter = 0;
      long lastUpdateTime = startTime;

      while (result.next()) {

         if (splashManager != null) {

            ++compTourCounter;

            final long currentTime = System.currentTimeMillis();
            final float timeDiff = currentTime - lastUpdateTime;

            // reduce logging
            if (timeDiff > 500) {

               lastUpdateTime = currentTime;

               splashManager.setMessage(
                     NLS.bind(
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

            StatusUtil.log(
                  NLS.bind(
                        "Cannot get tour {0} from database to update the recording time in the compared tour {1}.", //$NON-NLS-1$
                        tourId,
                        compareId));

         } else {

            final int tourRecordingTime = TourManager.computeTourDeviceTime_Elapsed(tourData, startIndex, endIndex);

            // update tour recording time for the compared tour
            stmtUpdate.setInt(1, tourRecordingTime);
            stmtUpdate.setLong(2, compareId);
            stmtUpdate.executeUpdate();
         }
      }

      final long timeDiff = System.currentTimeMillis() - startTime;

      StatusUtil.logInfo(String.format(
            "Database postupdate 39 -> 40 in %s mm:ss", //$NON-NLS-1$
            net.tourbook.common.UI.formatHhMmSs(timeDiff / 1000)));
   }

   private int updateDbDesign_040_to_041(final Connection conn, final SplashManager splashManager) throws SQLException {

      final int newDbVersion = 41;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 41
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "maxPace") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_Float(stmt, TABLE_TOUR_DATA, "maxPace",          DEFAULT_0);                            //$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

   private int updateDbDesign_041_to_042(final Connection conn, final SplashManager splashManager) throws SQLException {
      final int newDbVersion = 42;

      logDb_UpdateStart(newDbVersion);
      updateMonitor(splashManager, newDbVersion);

      final Statement stmt = conn.createStatement();
      {
         // check if db is updated to version 42
         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "tourDeviceTime_Recorded") == false) { //$NON-NLS-1$

// SET_FORMATTING_OFF

            // Add new columns
            SQL.AddCol_BigInt(stmt, TABLE_TOUR_DATA, "tourDeviceTime_Recorded", DEFAULT_0);                            //$NON-NLS-1$
            SQL.AddCol_BigInt(stmt, TABLE_TOUR_DATA, "tourDeviceTime_Paused",   DEFAULT_0);                            //$NON-NLS-1$

            SQL.RenameCol(stmt, TABLE_TOUR_DATA, "tourRecordingTime", "TourDeviceTime_Elapsed");
            SQL.RenameCol(stmt, TABLE_TOUR_DATA, "tourDrivingTime", "TourComputedTime_Moving");

            SQL.RenameCol(stmt, TABLE_TOUR_COMPARED, "tourRecordingTime", "TourDeviceTime_Elapsed");

// SET_FORMATTING_ON
         }
      }
      stmt.close();

      logDb_UpdateEnd(newDbVersion);

      return newDbVersion;
   }

//   private int updateDbDesign_034_to_035(final Connection conn, final IProgressMonitor monitor) throws SQLException {
//
//      final int newDbVersion = 35;
//
//      logDb_UpdateStart(newDbVersion);
//      updateMonitor(monitor, newDbVersion);
//
//      final Statement stmt = conn.createStatement();
//      {
//         // double check if db is already updated
//         if (isColumnAvailable(conn, TABLE_TOUR_DATA, "LatitudeMinE6") == false) { //$NON-NLS-1$
//
//            /*
//             * Create geo bound fields/index
//             */
//            SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "LatitudeMinE6", DEFAULT_0); //$NON-NLS-1$
//            SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "LatitudeMaxE6", DEFAULT_0); //$NON-NLS-1$
//
//            SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "LongitudeMinE6", DEFAULT_0); //$NON-NLS-1$
//            SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "LongitudeMaxE6", DEFAULT_0); //$NON-NLS-1$
//
//            createIndex_TourData_035(stmt);
//         }
//      }
//      stmt.close();
//
//      logDb_UpdateEnd(newDbVersion);
//
//      return newDbVersion;
//   }

//   /**
//    * Set lat/lon min/max values
//    *
//    * @param conn
//    * @param monitor
//    * @throws SQLException
//    */
//   private void updateDbDesign_034_to_035_PostUpdate(final Connection conn, final IProgressMonitor monitor)
//         throws SQLException {
//
//      final ArrayList<Long> allTours = getAllTourIds();
//
//      final int numTours = allTours.size();
//      int tourIndex = 1;
//
//      final EntityManager em = TourDatabase.getInstance().getEntityManager();
//
//      final long startTime = System.currentTimeMillis();
//
//      try {
//
//         long lastUpdateTime = startTime;
//
//         // loop: all tours
//         for (final Long tourId : allTours) {
//
//            if (monitor != null) {
//
//               final long currentTime = System.currentTimeMillis();
//               final float timeDiff = currentTime - lastUpdateTime;
//
//               // reduce logging
//               if (timeDiff > 200) {
//
//                  lastUpdateTime = currentTime;
//
//                  final String percent = String.format("%.1f", (float) tourIndex / numTours * 100.0);//$NON-NLS-1$
//
//                  monitor.subTask(
//                        NLS.bind(//
//                              Messages.Tour_Database_PostUpdate_035_SetGeoMinMax,
//                              new Object[] { tourIndex, numTours, percent }));
//               }
//
//               tourIndex++;
//            }
//
//            final TourData tourData = em.find(TourData.class, tourId);
//
//            if (tourData != null) {
//
//               tourData.computeGeo_Bounds();
//
//               saveTour(tourData, false);
//            }
//         }
//
//         // update UI otherwise < 100% is displayed
//         monitor.subTask(
//               NLS.bind(//
//                     Messages.Tour_Database_PostUpdate_035_SetGeoMinMax,
//                     new Object[] { tourIndex - 1, numTours, 100 }));
//
//      } catch (final Exception e) {
//         StatusUtil.log(e);
//      } finally {
//         em.close();
//      }
//
//      final long timeDiff = System.currentTimeMillis() - startTime;
//
//      StatusUtil.logInfo(
//            String.format(
//                  "Database postupdate 34->35 in %s mm:ss", //$NON-NLS-1$
//                  net.tourbook.common.UI.formatHhMmSs(timeDiff / 1000)));
//   }

   private void updateDbDesign_041_to_042_PostUpdate(final Connection conn, final SplashManager splashManager)
         throws SQLException {

      final long startTime = System.currentTimeMillis();

      final PreparedStatement stmtUpdate = conn.prepareStatement( //
            //
            "UPDATE " + TABLE_TOUR_DATA //    //$NON-NLS-1$
            //
                  + " SET" //                     //$NON-NLS-1$
                  //
                  + " tourDeviceTime_Recorded=tourDeviceTime_Elapsed"); //$NON-NLS-1$

      stmtUpdate.executeUpdate();

      final long timeDiff = System.currentTimeMillis() - startTime;

      StatusUtil.logInfo(String.format(
            "Database postupdate 39 -> 40 in %s mm:ss", //$NON-NLS-1$
            net.tourbook.common.UI.formatHhMmSs(timeDiff / 1000)));
   }

   private void updateDbDesign_VersionNumber(final Connection conn, final int newVersion) throws SQLException {

      final String sql = "UPDATE " + TABLE_DB_VERSION + " SET VERSION=" + newVersion; //$NON-NLS-1$ //$NON-NLS-2$

      conn.createStatement().executeUpdate(sql);

      StatusUtil.logInfo("New database version is set: " + newVersion); //$NON-NLS-1$

      _dbVersionAfterUpdate = newVersion;
   }

   private void updateMonitor(final SplashManager splashManager, final int newDbVersion) {

      if (splashManager != null) {
         splashManager.setMessage(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
      }
   }

}
