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
package net.tourbook.common.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;

/**
 * Common class to keep a sql statement and it's parameters together, this can be used as return
 * value from a method.
 */
public class SQLData {

   private static final char NL = UI.NEW_LINE;

   private String            _sqlString;
   private ArrayList<Object> _parameters;

   private int               _lastParameterIndex;

   /**
    * Setup with empty sql data
    */
   public SQLData() {

      _sqlString = UI.EMPTY_STRING;
      _parameters = new ArrayList<>();
   }

   public SQLData(final String sqlString, final ArrayList<Object> parameters) {

      _sqlString = sqlString;
      _parameters = parameters;
   }

   /**
    * @return Returns the last parameter index +1 which was used for setting parameters in
    *         {@link #setParameters(PreparedStatement, int)}
    */
   public int getLastParameterIndex() {
      return _lastParameterIndex;
   }

   public ArrayList<Object> getParameters() {
      return _parameters;
   }

   public String getSqlString() {
      return _sqlString;
   }

   /**
    * Sets the app filter parameters into the filter statement.
    * <p>
    * The last used index can be retrieved with {@link #getLastParameterIndex()}
    *
    * @param statement
    * @param startIndex
    *           Sets the parameter start index, the first parameter is 1
    * @return Returns the next parameter index which is the last paramter index +1
    * @throws SQLException
    */
   public int setParameters(final PreparedStatement statement, final int startIndex) throws SQLException {

      int parameterIndex = startIndex;

      for (final Object parameter : _parameters) {

         if (parameter instanceof Long) {

            statement.setLong(parameterIndex, (Long) parameter);
            parameterIndex++;

         } else if (parameter instanceof Integer) {

            statement.setInt(parameterIndex, (Integer) parameter);
            parameterIndex++;

         } else if (parameter instanceof Float) {

            statement.setFloat(parameterIndex, (Float) parameter);
            parameterIndex++;

         } else if (parameter instanceof Double) {

            statement.setDouble(parameterIndex, (Double) parameter);
            parameterIndex++;

         } else if (parameter instanceof String) {

            statement.setString(parameterIndex, (String) parameter);
            parameterIndex++;

         } else {

            throw new RuntimeException("SQL parameter is not supported, " + parameter.getClass());//$NON-NLS-1$
         }
      }

      return _lastParameterIndex = parameterIndex;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "SQLData [" + NL //                        //$NON-NLS-1$

            + "_sqlString  =" + _sqlString + NL //       //$NON-NLS-1$
            + "_parameters =" + _parameters + NL //      //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }
}
