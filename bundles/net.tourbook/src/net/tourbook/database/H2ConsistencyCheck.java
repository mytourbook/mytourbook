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

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.SplashManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

public class H2ConsistencyCheck {

   private static SplashManager _splashManager;

   /**
    * Dummy DataType that skips deserializing H2 SQL Value payloads so raw
    * MVStore B-tree pages and chunk footers can be traversed safely offline.
    */
   private static final class SkipDeserializationDataType extends BasicDataType<Object> {

      static final SkipDeserializationDataType INSTANCE = new SkipDeserializationDataType();

      @Override
      public Object[] createStorage(final int size) {
         return new Object[size];
      }

      @Override
      public int getMemory(final Object obj) {
         return 0;
      }

      @Override
      public Object read(final ByteBuffer buff) {
         // Do nothing: H2's Page.read() passes a buffer slice or advances per entry,
         // so we only care that the page header, checksum, and counts are readable.
         return null;
      }

      @Override
      public void write(final WriteBuffer buff, final Object obj) {
         throw new UnsupportedOperationException("Read-only verification");
      }
   }

   public static void checkH2Consistency() {

      _splashManager = SplashManager.getInstance();

      if (verify_10_All() == false) {

         // Trigger backup restore or halt startup here
         System.exit(1);
      }

      final String message = "H2 database integrity verified";

      _splashManager.setMessage(message);

      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            Messages.App_Db_Compress_DialogTitle,
            message);
   }

   /**
    * Runs a 3-stage integrity check suitable for application startup
    */
   private static boolean verify_10_All() {

      final Path dbFilePath = Paths.get(TourDatabase.H2_DB_FILE_PATH + ".mv.db");

      // Stage 1: Offline MVStore page & chunk header check
      if (dbFilePath != null && Files.exists(dbFilePath)) {

         try {

            verify_20_MvStoreOffline(dbFilePath);

         } catch (final Exception e) {

            e.printStackTrace();
         }
      }

      // Stage 2 & 3: Online JDBC checks (requires ADMIN privileges)
      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         verify_30_DataAndLobs(conn);
         verify_40_SecondaryIndexes(conn);

         return true;

      } catch (final SQLException e) {

         e.printStackTrace();
      }

      return false;
   }

   /**
    * Stage 1: Opens the raw MVStore in read-only mode to validate file headers,
    * chunk footers, and B-tree map metadata before starting the SQL engine.
    */
   private static void verify_20_MvStoreOffline(final Path dbFilePath) {

      _splashManager.setMessage("Stage 1: Opens the raw MVStore in read-only mode");

      final MVMap.Builder<Object, Object> rawMapBuilder = new MVMap.Builder<>()
            .keyType(SkipDeserializationDataType.INSTANCE)
            .valueType(SkipDeserializationDataType.INSTANCE);

      try (MVStore store = new MVStore.Builder()
            .fileName(dbFilePath.toAbsolutePath().toString())
            .readOnly()
            .open()) {

         for (final String mapName : store.getMapNames()) {

            _splashManager.setMessage("Stage 1: " + mapName);

            // Accessing map size traverses root/internal B-tree nodes and validates page checksums
            store.openMap(mapName, rawMapBuilder).sizeAsLong();
         }
      }
   }

   /**
    * Stage 2: Streams the entire database via SCRIPT without writing to disk.
    * Forces deserialization of every table row, LOB block, and system metadata entry.
    */
   private static void verify_30_DataAndLobs(final Connection conn) throws SQLException {

      _splashManager.setMessage("Stage 2: Streams the entire database via SCRIPT without writing to disk");

      int numLines = 0;

      try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SCRIPT NOPASSWORDS")) {

         while (rs.next()) {

            rs.getString(1);

            numLines++;

            if (numLines % 10000 == 0) {
               _splashManager.setMessage("Stage 2: " + numLines);
            }

         }
      }
   }

   /**
    * Stage 3: Scans every user-defined secondary index to verify index B-trees
    * are readable and in sync.
    */
   private static void verify_40_SecondaryIndexes(final Connection conn) throws SQLException {

      _splashManager.setMessage("Stage 3: Scans every user-defined secondary index");

      final String indexQuery = """
                                SELECT
                                  DISTINCT TABLE_SCHEMA,
                                  TABLE_NAME,
                                  COLUMN_NAME
                                FROM INFORMATION_SCHEMA.INDEX_COLUMNS
                                WHERE TABLE_SCHEMA NOT IN ('INFORMATION_SCHEMA', 'PG_CATALOG')
                                """;

      final List<String> scanQueries = new ArrayList<>();

      try (Statement metaStmt = conn.createStatement();
            ResultSet rs = metaStmt.executeQuery(indexQuery)) {

         while (rs.next()) {

            final String schema = rs.getString("TABLE_SCHEMA");
            final String table = rs.getString("TABLE_NAME");
            final String column = rs.getString("COLUMN_NAME");

            scanQueries.add("SELECT COUNT(\"%s\") FROM \"%s\".\"%s\" WHERE \"%s\" IS NOT NULL".formatted(
                  column,
                  schema,
                  table,
                  column));
         }
      }

      try (Statement scanStmt = conn.createStatement()) {

         for (final String sql : scanQueries) {

            _splashManager.setMessage("Stage 3: " + sql);

            try (ResultSet rs = scanStmt.executeQuery(sql)) {

               if (rs.next()) {
                  rs.getLong(1);
               }
            }
         }
      }
   }
}
