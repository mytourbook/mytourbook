/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.SplashManager;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public class MigrateDerbyToH2 {

   private static final String           DERBY_DRIVER       = "org.apache.derby.jdbc.EmbeddedDriver";
   private static final String           H2_DRIVER          = "org.h2.Driver";

   /**
    * The tour db needs this version to migrate to H2, otherwise the user must update MT to 26.9 or
    * 26.11 which is updating the db version to 63
    */
   private static final int              MIGRATE_DB_VERSION = 63;

   private static SplashManager          _splashManager;

   private static int                    _allRowsDiff;

   private static final IPreferenceStore _prefStore         = TourbookPlugin.getPrefStore();

   private static int check_GetRowCount(final Connection conn, final String tableName) throws SQLException {

      final String sql = "SELECT COUNT(*) FROM " + tableName;

      try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

         if (rs.next()) {
            return rs.getInt(1);
         }
      }

      return 0;
   }

   private static String check_GetTableBlobCheckSum(final Connection conn,
                                                    final String tableName,
                                                    final String idColumn,
                                                    final String blobColumn) throws Exception {

      // Order by ID explicitly to guarantee identical streaming order between databases
      final String sql = "SELECT " + idColumn + ", " + blobColumn + " FROM " + tableName + " ORDER BY " + idColumn;
      final MessageDigest md = MessageDigest.getInstance("SHA-256");

      try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

         final byte[] buffer = new byte[4096];
         while (rs.next()) {

            // Hash the identifier to catch missing/shuffled keys
            md.update(rs.getString(idColumn).getBytes());

            // Hash the physical stream bytes
            try (InputStream is = rs.getBinaryStream(blobColumn)) {

               if (is != null) {

                  int bytesRead;
                  while ((bytesRead = is.read(buffer)) != -1) {
                     md.update(buffer, 0, bytesRead);
                  }

               } else {

                  md.update((byte) 0); // Handle Null consistency explicitly
               }
            }
         }
      }

      // Convert digest to Hex String layout
      final byte[] digest = md.digest();
      final StringBuilder hexString = new StringBuilder();
      for (final byte b : digest) {
         hexString.append(String.format("%02x", b));
      }

      return hexString.toString();
   }

   private static List<String> getAllDerbyTables(final Connection conn) throws SQLException {

      final List<String> allTables = new ArrayList<>();
      final String sql = "SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE = 'T'";

      try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

         while (rs.next()) {
            allTables.add(rs.getString("TABLENAME"));
         }
      }

      return allTables;
   }

   private static boolean isDbVersionOK() {

      // set also the DERBY_DRIVER_CLASS
      final TourDatabase dbInstance = TourDatabase.getInstance();

      try {

         // ensure derby jar is available
         Class.forName(TourDatabase.DERBY_DRIVER_CLASS);

      } catch (final ClassNotFoundException e) {

         StatusUtil.showStatus(e.getMessage(), e);

         return false;
      }

      try (Connection conn = dbInstance.getConnection_Simple_Derby()) {

         final int dbVersionDesign = dbInstance.getDbVersion(conn, TourDatabase.TABLE_DB_VERSION_DESIGN);
         final int dbVersionData = dbInstance.getDbVersion(conn, TourDatabase.TABLE_DB_VERSION_DATA);

         _splashManager.setMessage("Design Version %d - Data Version %d".formatted(dbVersionDesign, dbVersionData));

         if (dbVersionDesign == MIGRATE_DB_VERSION && dbVersionData == MIGRATE_DB_VERSION) {

            return true;
         }

         final String message = "You are using a version of MyTourbook which cannot migrate to the new tour database H2.\n\n"
               + "Your database versions are %d and %d (design and data) but both versions must be 63.\n\n".formatted(dbVersionDesign, dbVersionData)
               + "To fix this requirement, install and run MyTourbook 26.11, this will update to the correct database versions.\n\n"
               + "After that, install and run this version and you will be happy to have a faster tour database.";

         MessageDialog.openError(
               Display.getDefault().getActiveShell(),
               "Migrate to H2",
               message);

      } catch (final SQLException e) {

         UI.showSQLException(e);
      }

      return false;
   }

   public static boolean isMigrated() {

      // debug h2 migration
//      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_H2_MIGRATED, false);
//      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_H2_MIGRATED, true);

      final boolean isH2Migrated = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_H2_MIGRATED);

      if (isH2Migrated) {

         // MT is already migrated to H2

         return true;
      }

      /*
       * MT is not yet migrated
       */
      _splashManager = SplashManager.getInstance();
      _splashManager.open();

      _splashManager.setMessage("Migrating Derby to H2");

      if (isDbVersionOK() == false) {

         // db versions are not valid, 63 is required

         return false;
      }

      if (isMigrated_All()) {

         // set flag that the h2 migration is done
         _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_H2_MIGRATED, true);

         return true;
      }

      return false;
   }

   private static boolean isMigrated_All() {

      final String sqlDbTourbook = TourDatabase.SQL_DB_TOURBOOK;

      String derbyDatabasePath = TourDatabase.derbyDatabasePath + "/" + sqlDbTourbook;
      String h2DatabasePath = TourDatabase.h2DatabasePath + "/" + sqlDbTourbook;

      if (derbyDatabasePath.startsWith("/")) {
         derbyDatabasePath = derbyDatabasePath.substring(1);
      }
      if (h2DatabasePath.startsWith("/")) {
         h2DatabasePath = h2DatabasePath.substring(1);
      }

      final long start = System.nanoTime();

      migrate_10_DerbyToH2(derbyDatabasePath, h2DatabasePath);

      final long end = System.nanoTime();

      System.out.println(UI.timeStamp() + " Migration duration: %7.3f ms".formatted((float) (end - start) / 1000000));

      return false;
   }

   private static void migrate_10_DerbyToH2(final String derbyDatabasePath, final String h2DatabasePath) {

      try {

         Class.forName(DERBY_DRIVER);
         Class.forName(H2_DRIVER);

      } catch (final Exception e) {

         StatusUtil.log(e);

         return;
      }

      final String derbyUrl = "jdbc:derby:" + derbyDatabasePath;
      final String h2Url = "jdbc:h2:" + h2DatabasePath + ";AUTO_SERVER=TRUE";

      try (Connection derbyConn = DriverManager.getConnection(derbyUrl, "USER", "USER");
            Connection h2Conn = DriverManager.getConnection(h2Url)) {

         migrate_20_CreateH2Database(h2Conn);
         migrate_30_MigrateTableData(derbyConn, h2Conn);

      } catch (final SQLException e) {

         UI.showSQLException(e);

      } catch (final Exception e) {

         StatusUtil.log(e);

      } finally {

         // 3. Shutdown Derby embedded cleanly to release file locks
         try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
         } catch (final SQLException se) {
            // Derby always throws SQLState XJ015 on successful shutdown
         }
      }
   }

   private static void migrate_20_CreateH2Database(final Connection h2Conn) throws SQLException {

      /*
       * Check if TOURDATA table exists
       */
      final DatabaseMetaData metaData = h2Conn.getMetaData();
      final ResultSet tables = metaData.getTables(null, null, null, null);
      while (tables.next()) {

         final String tableName = tables.getString(3);

         if (tableName.equalsIgnoreCase(TourDatabase.TABLE_TOUR_DATA)) {

            // table exists

            return;
         }
      }

      _splashManager.setMessage(Messages.Database_Monitor_CreateDatabase);

      try (Statement stmt = h2Conn.createStatement()) {

         TourDatabase.getInstance().sqlStartup_42_CreateAllTables(stmt);

      } catch (final SQLException e) {

         UI.showSQLException(e);
      }
   }

   private static void migrate_30_MigrateTableData(final Connection derbyConn,
                                                   final Connection h2Conn) throws Exception {

      h2Conn.setAutoCommit(false); // Enable manual transaction management

      // 1. Fetch all user tables from the Derby system catalog
      final List<String> allDerbyTableNames = getAllDerbyTables(derbyConn);

      // 2. Iterate through each table and migrate its content
      for (final String tableName : allDerbyTableNames) {

         _splashManager.setMessage("Migrating table: " + tableName);

         if (tableName.equalsIgnoreCase(TourDatabase.TABLE_DB_VERSION_DESIGN)
               || tableName.equalsIgnoreCase(TourDatabase.TABLE_DB_VERSION_DATA)) {

            // skip these tables because their values are set when they are created

         } else {

            migrate_40_MigrateTableData_One(derbyConn, h2Conn, tableName);
            migrate_50_CheckData(derbyConn, h2Conn, tableName);
         }
      }

      _splashManager.setMessage("Derby vs H2 rows diff: %d".formatted(_allRowsDiff));
      _splashManager.setMessage("Database migration successfully completed!");
   }

   private static void migrate_40_MigrateTableData_One(final Connection srcConn,
                                                       final Connection destConn,
                                                       final String tableName) throws SQLException {

      long lastLogTime = System.currentTimeMillis();

      final String selectSql = "SELECT * FROM " + tableName;

      try (Statement srcStmt = srcConn.createStatement();
            ResultSet srcResultSet = srcStmt.executeQuery(selectSql)) {

         final ResultSetMetaData metaData = srcResultSet.getMetaData();
         final int columnCount = metaData.getColumnCount();

         // 1. Extract all column names from the table structure
         final List<String> allColumnNames = new ArrayList<>();
         for (int i = 1; i <= columnCount; i++) {
            allColumnNames.add(metaData.getColumnName(i));
         }

         // 2. Build an explicit, bulletproof INSERT statement specifying column names
         // e.g., INSERT INTO TOURPERSON (ID, NAME, BIRTHDAY) VALUES (?, ?, ?)
         final StringBuilder insertBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
         final StringBuilder valuesBuilder = new StringBuilder(" VALUES (");

         for (int i = 0; i < allColumnNames.size(); i++) {

            if (i > 0) {
               insertBuilder.append(", ");
               valuesBuilder.append(", ");
            }

            // Quote to handle mixed-case or reserved names safely
            insertBuilder.append("\"").append(allColumnNames.get(i)).append("\"");
            valuesBuilder.append("?");
         }
         insertBuilder.append(")").append(valuesBuilder.append(")"));

         // Disable referential integrity checks during batch processing
         try (Statement constraintStmt = destConn.createStatement()) {
            constraintStmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
         }

         try (PreparedStatement destPstmt = destConn.prepareStatement(insertBuilder.toString())) {

            int numBatches = 0;
            final int maxBatches = 500;

            while (srcResultSet.next()) {

               // 3. Map parameters by walking through the exact names list
               for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {

                  final String colName = allColumnNames.get(columnIndex - 1);
                  final int colType = metaData.getColumnType(columnIndex);

                  switch (colType) {
                  case java.sql.Types.BLOB:
                  case java.sql.Types.BINARY:
                  case java.sql.Types.VARBINARY:
                  case java.sql.Types.LONGVARBINARY:
                     final InputStream blobStream = srcResultSet.getBinaryStream(colName);
                     if (blobStream != null) {
                        destPstmt.setBinaryStream(columnIndex, blobStream);
                     } else {
                        destPstmt.setNull(columnIndex, colType);
                     }
                     break;

                  case java.sql.Types.CLOB:
                  case java.sql.Types.LONGVARCHAR:
                     destPstmt.setCharacterStream(columnIndex, srcResultSet.getCharacterStream(colName));
                     break;

                  case java.sql.Types.INTEGER:
                     final int intVal = srcResultSet.getInt(colName);
                     if (srcResultSet.wasNull()) {
                        destPstmt.setNull(columnIndex, colType);
                     } else {
                        destPstmt.setInt(columnIndex, intVal);
                     }
                     break;

                  case java.sql.Types.BIGINT:
                     final long longVal = srcResultSet.getLong(colName);
                     if (srcResultSet.wasNull()) {
                        destPstmt.setNull(columnIndex, colType);
                     } else {
                        destPstmt.setLong(columnIndex, longVal);
                     }
                     break;

                  case java.sql.Types.BOOLEAN:
                  case java.sql.Types.BIT:
                     final boolean boolVal = srcResultSet.getBoolean(colName);
                     if (srcResultSet.wasNull()) {
                        destPstmt.setNull(columnIndex, colType);
                     } else {
                        destPstmt.setBoolean(columnIndex, boolVal);
                     }
                     break;

                  case java.sql.Types.TIMESTAMP:
                     destPstmt.setTimestamp(columnIndex, srcResultSet.getTimestamp(colName));
                     break;

                  case java.sql.Types.DATE:
                     destPstmt.setDate(columnIndex, srcResultSet.getDate(colName));
                     break;

                  case java.sql.Types.DOUBLE:
                  case java.sql.Types.FLOAT:
                     final double doubleVal = srcResultSet.getDouble(colName);
                     if (srcResultSet.wasNull()) {
                        destPstmt.setNull(columnIndex, colType);
                     } else {
                        destPstmt.setDouble(columnIndex, doubleVal);
                     }
                     break;

                  default:
                     final Object value = srcResultSet.getObject(colName);
                     if (value == null) {
                        destPstmt.setNull(columnIndex, colType);
                     } else {
                        destPstmt.setObject(columnIndex, value);
                     }
                     break;
                  }
               }

               destPstmt.addBatch();
               numBatches++;

               if (numBatches % maxBatches == 0) {

                  final long currentTime = System.currentTimeMillis();
                  final long timeDiff = currentTime - lastLogTime;

                  // reduce logging
                  if (timeDiff > 500) {

                     lastLogTime = currentTime;

                     _splashManager.setMessage("   %d rows in %s".formatted(numBatches, tableName));
                  }

                  destPstmt.executeBatch();
                  destConn.commit();
               }
            }

            if (numBatches % maxBatches != 0) {

               _splashManager.setMessage("   %d rows in %s".formatted(numBatches, tableName));

               destPstmt.executeBatch();
               destConn.commit();
            }

         } finally {

            // Restore constraint checking validation

            try (Statement constraintStmt = destConn.createStatement()) {
               constraintStmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
         }
      }
   }

   private static void migrate_50_CheckData(final Connection derbyConn, final Connection h2Conn, final String tableName) throws Exception {

      /*
       * Quick row check
       */
      final int derbyRows = check_GetRowCount(derbyConn, tableName);
      final int h2Rows = check_GetRowCount(h2Conn, tableName);

      final int rowsDiff = h2Rows - derbyRows;

      _allRowsDiff += rowsDiff;

      if (rowsDiff > 0) {

         _splashManager.setMessage("   Check: Derby rows: %d - H2 rows: %d                                      Rows diff: %d".formatted(
               derbyRows,
               h2Rows,
               rowsDiff));

      } else {

         _splashManager.setMessage("   Check: Derby rows: %d - H2 rows: %d".formatted(derbyRows, h2Rows));
      }

      /*
       * Blob check
       */
      if (tableName.equalsIgnoreCase("TOURDATA")) {

         final String derbyHash = check_GetTableBlobCheckSum(derbyConn, "TOURDATA", "tourId", "SerieData");
         final String h2Hash = check_GetTableBlobCheckSum(h2Conn, "TOURDATA", "tourId", "SerieData");

         if (derbyHash.equals(h2Hash)) {
            _splashManager.setMessage("   Check: TOURDATA Blobs perfectly matched via SHA-256 validation");
         } else {
            _splashManager.setMessage("   Check: Critical Error: Data mismatch detected inside TOURDATA blobs !");
         }
      }
   }
}
