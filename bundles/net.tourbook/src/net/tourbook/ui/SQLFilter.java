/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.SQLData;
import net.tourbook.data.TourPerson;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tour.filter.TourFilterManager;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;

/**
 * The filter provides a sql WHERE which contains all tour filter, e.g. selected person, tour type,
 * photo and advanced tour filter.
 */
public class SQLFilter {

   private static final String            NL                 = UI.NEW_LINE;

   private static final Set<SQLAppFilter> DEFAULT_APP_FILTER = new HashSet<>();

   /**
    * Contains all app tour filters which are performed very fast, e.g. person, tour type.
    * <p>
    * This filter do not contain e.g. geo compare or tag filters.
    */
   public static final Set<SQLAppFilter>  FAST_APP_FILTER    = new HashSet<>();

   /**
    * Exclude all special app filter
    */
   public static final Set<SQLAppFilter>  NO_PHOTOS          = new HashSet<>();

   public static final Set<SQLAppFilter>  NO_GEO_LOCATION    = new HashSet<>();

   /**
    * Use sql app filter with {@link SQLAppFilter#Photo} and {@link SQLAppFilter#Tag}
    */
   public static final Set<SQLAppFilter>  TAG_FILTER         = new HashSet<>();

   static {

      // default is using the photo filter
      DEFAULT_APP_FILTER.add(SQLAppFilter.Photo);
      DEFAULT_APP_FILTER.add(SQLAppFilter.GeoLocation);

      FAST_APP_FILTER.add(SQLAppFilter.Photo);

      NO_GEO_LOCATION.add(SQLAppFilter.Photo);

      TAG_FILTER.add(SQLAppFilter.Photo);
      TAG_FILTER.add(SQLAppFilter.GeoLocation);
      TAG_FILTER.add(SQLAppFilter.Tag);

   }

   private String            _sqlWhereClause = UI.EMPTY_STRING;
   private ArrayList<Object> _parameters     = new ArrayList<>();

   private boolean           _isTagFilterActive;

   private int               _lastParameterIndex;

   /**
    * Create sql app filter with the photo filter
    */
   public SQLFilter() {
      this(DEFAULT_APP_FILTER);
   }

   /**
    * Creates the WHERE statement for the selected app filters by appending an AND to an existing
    * sql statement.
    *
    * @param appFilter
    */
   public SQLFilter(final Set<SQLAppFilter> appFilter) {

      final StringBuilder sb = new StringBuilder();

      /*
       * App filter: Person
       */
      final TourPerson activePerson = TourbookPlugin.getActivePerson();
      if (activePerson == null) {

         // select all people

      } else {

         // select only one person

         sb.append(" AND TourData.tourPerson_personId = ?" + NL); //$NON-NLS-1$
         _parameters.add(activePerson.getPersonId());
      }

      /*
       * App filter: Photo
       */
      if (appFilter.contains(SQLAppFilter.Photo) && TourbookPlugin.getActivePhotoFilter()) {

         sb.append(" AND TourData.NumberOfPhotos > 0" + NL); //$NON-NLS-1$
      }

      /*
       * App filter: Tour type
       */
      final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();
      if (activeTourTypeFilter != null) {

         final TourTypeSQLData sqlData = activeTourTypeFilter.getSQLData();

         sb.append(sqlData.getWhereString());
         _parameters.addAll(sqlData.getParameters());
      }

      /*
       * App Filter: Tour data
       */
      final SQLData tourSqlData = TourFilterManager.getSQL();
      if (tourSqlData != null) {

         sb.append(tourSqlData.getSqlString());
         _parameters.addAll(tourSqlData.getParameters());
      }

      /*
       * App Filter: Tour geo location
       */
      if (appFilter.contains(SQLAppFilter.GeoLocation)) {

         final SQLData tourSqlGeoData = TourGeoFilter_Manager.getSQL();
         if (tourSqlGeoData != null) {

            sb.append(tourSqlGeoData.getSqlString());
            _parameters.addAll(tourSqlGeoData.getParameters());
         }
      }

      /*
       * App Filter: Tour tags
       */
      _isTagFilterActive = false;

      if (appFilter.contains(SQLAppFilter.Tag)) {

         if (TourTagFilterManager.isTourTagFilterEnabled() && TourTagFilterManager.getSelectedProfile().isOrOperator) {

            final SQLData tourTagSqlData = TourTagFilterManager.getSQL_WherePart();

            if (tourTagSqlData != null) {

               _isTagFilterActive = true;

               sb.append(tourTagSqlData.getSqlString());
               _parameters.addAll(tourTagSqlData.getParameters());
            }
         }
      }

      _sqlWhereClause = sb.toString();
   }

   /**
    * @return Returns the last parameter index +1 which was used for setting parameters in
    *         {@link #setParameters(PreparedStatement, int)}
    */
   public int getLastParameterIndex() {
      return _lastParameterIndex;
   }

   /**
    * @return Returns the WHERE clause to filter tours by the app filter, e.g. person, tour types,
    *         ...
    *         <p>
    *         This WHERE clause contains the tag filter sql statements ONLY, when a tag filter is
    *         enabled and the tag's are combined with OR.
    *         ...
    */
   public String getWhereClause() {
      return _sqlWhereClause;
   }

   /**
    * @return Returns <code>true</code> when the tag filter is being used, it is enabled and has at
    *         least 1 tag
    */
   public boolean isTagFilterActive() {
      return _isTagFilterActive;
   }

   /**
    * Sets the app filter parameters into the filter statement.
    * <p>
    * The last used index can be retrieved with {@link #getLastParameterIndex()}
    *
    * @param statement
    * @param startIndex
    *           Sets the parameter start index, the first parameter is 1
    * @return Returns the last parameter index +1 which was used for setting parameters
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

            throw new RuntimeException("SQL filter parameter is not supported, " + parameter.getClass());//$NON-NLS-1$
         }
      }

      _lastParameterIndex = parameterIndex;

      return parameterIndex;
   }

   @Override
   public String toString() {
      return _sqlWhereClause;
   }
}
