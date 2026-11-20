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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.tourbook.Messages;
import net.tourbook.application.SplashManager;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.h2.mvstore.MVStoreTool;

public class H2StorageTools {

   public static void compressH2Database() {

      final Display display = Display.getDefault();
      final Shell shell = display.getActiveShell();

      if (new MessageDialog(shell,

            Messages.App_Db_Compress_DialogTitle,
            null, // no title image

            "Compress tour database now?",
            MessageDialog.QUESTION,

            1, // default index

            "Com&press Now",
            Messages.App_Action_Cancel

      ).open() != IDialogConstants.OK_ID) {
         return;
      }

      final Path mvDbFile = Paths.get(TourDatabase.H2_DB_FILE_PATH + ".mv.db");
      final Path lockFile = Paths.get(TourDatabase.H2_DB_FILE_PATH + ".lock.db");

      // Only compact if the database already exists
      if (!Files.exists(mvDbFile)) {
         return;
      }

      // Abort if an external tool currently holds the lock
      if (Files.exists(lockFile)) {

         MessageDialog.openInformation(shell,
               Messages.App_Db_Compress_DialogTitle,
               "H2 database is locked by another process; skipping startup compression");

         return;
      }

      try {

         final long sizeBeforeBytes = Files.size(mvDbFile);
         final long startTime = System.nanoTime();

         // BusyIndicator.showWhile is not working
         final SplashManager splashManager = SplashManager.getInstance();
         splashManager.setMessage("Compressing %s".formatted(mvDbFile));

         // Compact offline before opening any JDBC connection (safe with AUTO_SERVER=TRUE)
         MVStoreTool.compact(mvDbFile.toString(), true);

         final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
         final long sizeAfterBytes = Files.size(mvDbFile);
         final long savedBytes = sizeBeforeBytes - sizeAfterBytes;

         final String message = UI.EMPTY_STRING

               + "H2 compression finished in %d ms\n\nBefore:\t%8.2f MB\nAfter:\t%8.2f MB\nSaved:\t%8.2f MB".formatted(

                     durationMs,
                     sizeBeforeBytes / (1024.0 * 1024.0),
                     sizeAfterBytes / (1024.0 * 1024.0),
                     savedBytes / (1024.0 * 1024.0));

         splashManager.setMessage(message);

         MessageDialog.openInformation(shell,
               Messages.App_Db_Compress_DialogTitle,
               message);

      } catch (final Exception e) {

         StatusUtil.log("H2 startup compression failed: " + e.getMessage(), e);
      }
   }

   private static String formatBytes(final long bytes) {

      if (bytes == 0) {
         return "";
      }

      final double value = bytes / 1024.0;
      final long rounded = Math.round(value * 1000.0);

      if (rounded < 1000) {

         return ">0";

      } else {

         return "%,.0f".formatted(value);
      }
   }

   public static String inspectAll() {

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final StringBuilder sbLog = new StringBuilder();
         final Formatter logFormatter = new Formatter(sbLog);

         final long totalTablesAndIndexesBytes = inspectAll_10_Tables(conn, logFormatter);
         final long totalLobBytes = inspectAll_20_Lobs(conn, logFormatter);

         inspectAll_30_Indexes(conn, logFormatter);

         inspectAll_40_Fragmentation(conn, logFormatter, totalTablesAndIndexesBytes, totalLobBytes);

         return sbLog.toString();

      } catch (final SQLException e) {

         e.printStackTrace();
      }

      return null;
   }

   private static long inspectAll_10_Tables(final Connection conn,
                                            final Formatter logFormatter) throws SQLException {

      final String sql = """
                         SELECT
                           TABLE_NAME,
                           ROW_COUNT_ESTIMATE,
                           DB_OBJECT_SIZE('TABLE', TABLE_SCHEMA, TABLE_NAME) 			AS DATA_BYTES,
                           DB_OBJECT_TOTAL_SIZE('TABLE', TABLE_SCHEMA, TABLE_NAME) 	AS TOTAL_BYTES
                         FROM INFORMATION_SCHEMA.TABLES
                         WHERE TABLE_SCHEMA NOT IN ('INFORMATION_SCHEMA')
                           AND TABLE_TYPE = 'BASE TABLE'
                         ORDER BY TABLE_NAME ASC
                         """;

      final List<List<String>> allRows = new ArrayList<>();
      long totalRows = 0, totalData = 0, totalAll = 0;

      try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

         while (rs.next()) {

            final String table = rs.getString("TABLE_NAME");
            final long numRows = rs.getLong("ROW_COUNT_ESTIMATE"),
                  dataBytes = rs.getLong("DATA_BYTES"),
                  totalBytes = rs.getLong("TOTAL_BYTES");

            totalRows += numRows;
            totalData += dataBytes;
            totalAll += totalBytes;

            allRows.add(List.of(

                  table,
                  "%,d".formatted(numRows),

                  formatBytes(dataBytes),
                  formatBytes(totalBytes),
                  formatBytes(numRows > 0 ? dataBytes / numRows : 0),
                  formatBytes(numRows > 0 ? totalBytes / numRows : 0))

            );
         }
      }

      final List<String> allFooter = List.of(

            "TOTAL (" + allRows.size() + " tables)",
            "%,d".formatted(totalRows),
            formatBytes(totalData),
            formatBytes(totalAll));

      final List<List<String>> allHeaders = new ArrayList<>();

      allHeaders.add(List.of(
            "Table",
            "Rows",
            "Data",
            "Total"));

      allHeaders.add(List.of(
            UI.EMPTY_STRING,
            UI.EMPTY_STRING,
            UI.UNIT_KBYTE,
            UI.UNIT_KBYTE));

      printTable(logFormatter, allHeaders, allRows, allFooter, Set.of(1, 2, 3, 4), true);

      return totalAll;
   }

   private static long inspectAll_20_Lobs(final Connection conn,
                                          final Formatter logFormatter) throws SQLException {

      final String findSql = """
                             SELECT
                               TABLE_SCHEMA,
                               TABLE_NAME,
                               COLUMN_NAME
                             FROM INFORMATION_SCHEMA.COLUMNS
                             WHERE TABLE_SCHEMA NOT IN ('INFORMATION_SCHEMA')
                               AND DATA_TYPE IN ('BINARY LARGE OBJECT', 'CHARACTER LARGE OBJECT')
                             """;

      record LobCol(String schema, String table, String col) {}

      final List<LobCol> allColumns = new ArrayList<>();
      try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(findSql)) {
         while (rs.next()) {
            allColumns.add(new LobCol(rs.getString(1), rs.getString(2), rs.getString(3)));
         }
      }

      if (allColumns.isEmpty()) {
         return 0;
      }

      final List<List<String>> allRows = new ArrayList<>();
      long totalLob = 0;

      for (final LobCol column : allColumns) {

         final String sumSql = "SELECT COALESCE(SUM(OCTET_LENGTH(\"%s\")), 0) FROM \"%s\".\"%s\"".formatted(
               column.col(),
               column.schema(),
               column.table());

         try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sumSql)) {

            if (rs.next()) {

               final long bytes = rs.getLong(1);
               totalLob += bytes;

               allRows.add(List.of(column.table(), column.col(), formatBytes(bytes)));
            }
         }
      }

      final List<List<String>> allHeaders = new ArrayList<>();

      allHeaders.add(List.of("BLOB / CLOB", "Column", "Size"));

      allHeaders.add(List.of(
            UI.EMPTY_STRING,
            UI.EMPTY_STRING,
            UI.UNIT_KBYTE));

      printTable(logFormatter, allHeaders, allRows, null, Set.of(2), false);

      return totalLob;
   }

   private static void inspectAll_30_Indexes(final Connection conn,
                                             final Formatter logFormatter) throws SQLException {

      final String sql = """
                         SELECT
                           TABLE_NAME,
                           INDEX_NAME,
                           DB_OBJECT_SIZE('INDEX', TABLE_SCHEMA, INDEX_NAME) AS INDEX_BYTES
                         FROM INFORMATION_SCHEMA.INDEXES
                         WHERE TABLE_SCHEMA NOT IN ('INFORMATION_SCHEMA')
                         ORDER BY TABLE_NAME, INDEX_NAME
                         """;

      final List<List<String>> allRows = new ArrayList<>();
      try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

         while (rs.next()) {

            allRows.add(List.of(

                  rs.getString("TABLE_NAME"),
                  rs.getString("INDEX_NAME"),
                  formatBytes(rs.getLong("INDEX_BYTES"))

            ));
         }
      }

      final List<List<String>> allHeaders = new ArrayList<>();

      allHeaders.add(List.of("Table", "Index", "Size"));

      allHeaders.add(List.of(
            UI.EMPTY_STRING,
            UI.EMPTY_STRING,
            UI.UNIT_KBYTE));

      printTable(logFormatter, allHeaders, allRows, null, Set.of(2), false);
   }

   private static void inspectAll_40_Fragmentation(final Connection conn,
                                                   final Formatter logFormatter,
                                                   final long tablesAndIndexesBytes,
                                                   final long lobBytes) throws SQLException {

      final String sql = """
                         SELECT
                           SETTING_NAME,
                           SETTING_VALUE
                         FROM INFORMATION_SCHEMA.SETTINGS
                         WHERE SETTING_NAME IN (
                           'info.FILE_SIZE',
                           'info.CHUNKS_FILL_RATE',
                           'info.PAGE_COUNT',
                           'info.PAGE_COUNT_LIVE'
                         )
                         """;

      final Map<String, String> stats = new HashMap<>();
      try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
         while (rs.next()) {
            stats.put(rs.getString(1), rs.getString(2));
         }
      }

// SET_FORMATTING_OFF

      final long fileSize     = Long.parseLong(stats.getOrDefault("info.FILE_SIZE", "0"));
      final long numPages     = Long.parseLong(stats.getOrDefault("info.PAGE_COUNT", "0"));
      final long numLivePages = Long.parseLong(stats.getOrDefault("info.PAGE_COUNT_LIVE", "0"));
      final long active       = tablesAndIndexesBytes + lobBytes;
      final long deadBytes    = Math.max(0, fileSize - active);

      final List<List<String>> rows = List.of(

            List.of("Total .mv.db File Size",         formatBytes(fileSize)),
            List.of("Tables & Indexes",               formatBytes(tablesAndIndexesBytes)),
            List.of("BLOBs & CLOBs",                  formatBytes(lobBytes)),
            List.of("Total Active Data",              formatBytes(active)),
            List.of("Unallocated/Dead Space",         formatBytes(deadBytes)),

            List.of(UI.EMPTY_STRING,                  UI.EMPTY_STRING),

            List.of("Chunk Fill Rate",                stats.getOrDefault("info.CHUNKS_FILL_RATE", "N/A") + "%"),
            List.of("Dead/All Pages", "%,d# / %,d#"   .formatted(numPages - numLivePages, numPages) ) //
            );

// SET_FORMATTING_ON

      final List<List<String>> allHeaders = new ArrayList<>();

      allHeaders.add(List.of("Metric", "Value"));
      allHeaders.add(List.of(UI.EMPTY_STRING, UI.UNIT_KBYTE));

      printTable(logFormatter, allHeaders, rows, null, Set.of(1, 2, 3, 4, 5), false);
   }

   private static void printTable(final Formatter logFormatter,
                                  final List<List<String>> allHeaders,
                                  final List<List<String>> allRows,
                                  final List<String> allFooter,
                                  final Set<Integer> allRightCols,
                                  final boolean isFirst) {

      final int numColumns = allHeaders.get(0).size();
      final int[] maxColumnWidth = new int[numColumns];

      for (final List<String> header : allHeaders) {
         for (int i = 0; i < numColumns; i++) {
            maxColumnWidth[i] = Math.max(maxColumnWidth[i], header.get(i).length());
         }
      }

      for (final List<String> row : allRows) {
         for (int i = 0; i < numColumns; i++) {
            maxColumnWidth[i] = Math.max(maxColumnWidth[i], row.get(i).length());
         }
      }

      if (allFooter != null) {
         for (int i = 0; i < numColumns; i++) {
            maxColumnWidth[i] = Math.max(maxColumnWidth[i], allFooter.get(i).length());
         }
      }

      final StringBuilder sb = new StringBuilder();

      sb.append(UI.SPACE);

      for (int i = 0; i < numColumns; i++) {
         if (i > 0) {
            sb.append("   ");
         }
         sb.append("%").append(allRightCols.contains(i) ? "" : "-").append(maxColumnWidth[i]).append("s");
      }

      final String formattedValues = sb.append(" %n").toString();

      if (isFirst) {
         logFormatter.format(UI.NEW_LINE1);
      } else {
         logFormatter.format(UI.NEW_LINE3);
      }

      logFormatter.format(formattedValues, allHeaders.get(0).toArray());
      logFormatter.format(formattedValues, allHeaders.get(1).toArray());
      logFormatter.format(UI.NEW_LINE1);

      for (final List<String> rowText : allRows) {
         logFormatter.format(formattedValues, rowText.toArray());
      }

      if (allFooter != null) {
         logFormatter.format(UI.NEW_LINE1);
         logFormatter.format(formattedValues, allFooter.toArray());
      }
   }
}
