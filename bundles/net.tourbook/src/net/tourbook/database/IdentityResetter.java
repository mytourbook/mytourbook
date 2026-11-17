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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.tourbook.application.SplashManager;

public class IdentityResetter {

   private static long getNextId(final Connection conn,
                                 final String table,
                                 final String column,
                                 final String quote) throws SQLException {

      final String sql = "SELECT COALESCE(MAX(%s%s%s), 0) + 1 FROM %s%s%s".formatted(
            quote,
            column,
            quote,
            quote,
            table,
            quote);

      try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

         return rs.next() ? rs.getLong(1) : 1L;
      }
   }

   /**
    * Scans all tables in the connected schema, finds columns marked as IS_AUTOINCREMENT,
    * and resets their identity/auto-increment counter to MAX(id) + 1.
    */
   public static void resetAllIdentities(final Connection conn) throws SQLException {

      final long startTime = System.currentTimeMillis();

      final SplashManager splashManager = SplashManager.getInstance();

      final DatabaseMetaData meta = conn.getMetaData();
      final String product = meta.getDatabaseProductName();
      final String productLower = product.toLowerCase(Locale.ROOT);
      final String catalog = conn.getCatalog();
      final String schema = conn.getSchema();

      String quote = meta.getIdentifierQuoteString();
      if (quote == null || quote.isBlank()) {
         quote = "\"";
      }

      splashManager.setMessage("Resetting database identity sequences on %s (schema: %s)".formatted(product, schema));

      final List<String> allTables = new ArrayList<>();

      try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[] { "TABLE" })) {

         while (rs.next()) {

            allTables.add(rs.getString("TABLE_NAME"));
         }
      }

      int resetCount = 0;

      for (final String table : allTables) {

         try (ResultSet cols = meta.getColumns(catalog, schema, table, "%")) {

            while (cols.next()) {

               if ("YES".equalsIgnoreCase(cols.getString("IS_AUTOINCREMENT"))) {

                  final String column = cols.getString("COLUMN_NAME");
                  final long nextVal = getNextId(conn, table, column, quote);

                  try {

                     final boolean isUpdated = resetColumnIdentity(conn,
                           productLower,
                           schema,
                           table,
                           column,
                           nextVal,
                           quote);

                     if (isUpdated) {

                        resetCount++;

                        splashManager.setMessage("Reset identity %-25s.%-15s -> next ID: %d".formatted(table, column, nextVal));
                     }

                  } catch (final SQLException e) {

                     splashManager.setMessage("Failed to reset identity for %s.%s to %d: %s".formatted(
                           table,
                           column,
                           nextVal,
                           e.getMessage()));

                     throw e;
                  }
               }
            }
         }
      }

      final long elapsedMs = System.currentTimeMillis() - startTime;

      splashManager.setMessage("Completed identity sequence reset: %d columns updated across %d tables in %d ms".formatted(
            resetCount,
            allTables.size(),
            elapsedMs));
   }

   private static boolean resetColumnIdentity(final Connection conn,
                                              final String productLower,
                                              final String schema,
                                              final String table,
                                              final String column,
                                              final long nextVal,
                                              final String quote) throws SQLException {

      try (Statement stmt = conn.createStatement()) {

         if (productLower.contains("mariadb") || productLower.contains("mysql")) {

            stmt.execute("ALTER TABLE %s%s%s AUTO_INCREMENT = %d".formatted(
                  quote,
                  table,
                  quote,
                  nextVal));

            return true;

         } else if (productLower.contains("postgresql")) {

            final String qualifiedTable = (schema != null && !schema.isBlank())
                  ? "%s%s%s.%s%s%s".formatted(quote, schema, quote, quote, table, quote)
                  : "%s%s%s".formatted(quote, table, quote);

            stmt.execute("SELECT setval(pg_get_serial_sequence('%s', '%s'), %d, false)".formatted(
                  qualifiedTable,
                  column,
                  nextVal));

            return true;

         } else if (productLower.contains("h2") || productLower.contains("derby")) {

            stmt.execute("ALTER TABLE %s%s%s ALTER COLUMN %s%s%s RESTART WITH %d".formatted(
                  quote,
                  table,
                  quote,
                  quote,
                  column,
                  quote,
                  nextVal));

            return true;

         } else if (productLower.contains("microsoft") || productLower.contains("sql server")) {

            final long reseedVal = Math.max(0L, nextVal - 1L);

            stmt.execute("DBCC CHECKIDENT ('%s', RESEED, %d)".formatted(table, reseedVal));

            return true;

         } else {

            SplashManager.getInstance().setMessage("Unsupported database dialect '%s' for identity reset on %s.%s"
                  .formatted(
                        productLower,
                        table,
                        column));

            return false;
         }
      }
   }
}
