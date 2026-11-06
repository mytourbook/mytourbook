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
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class DriverShim implements Driver {

   private final Driver _delegate;

   public DriverShim(final Driver delegate) {
      this._delegate = delegate;
   }

   @Override
   public boolean acceptsURL(final String url) throws SQLException {
      return _delegate.acceptsURL(url);
   }

   @Override
   public Connection connect(final String url, final Properties info) throws SQLException {
      return _delegate.connect(url, info);
   }

   @Override
   public int getMajorVersion() {
      return _delegate.getMajorVersion();
   }

   @Override
   public int getMinorVersion() {
      return _delegate.getMinorVersion();
   }

   @Override
   public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return _delegate.getParentLogger();
   }

   @Override
   public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
      return _delegate.getPropertyInfo(url, info);
   }

   @Override
   public boolean jdbcCompliant() {
      return _delegate.jdbcCompliant();
   }

}
