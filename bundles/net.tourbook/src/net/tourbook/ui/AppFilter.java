/*******************************************************************************
 * Copyright (C) 2008, 2026 Wolfgang Schramm and Contributors
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
import java.util.List;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQLData;
import net.tourbook.data.TourPerson;
import net.tourbook.equipment.EquipmentFilter;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilter;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tour.filter.TourFilterManager;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;

/**
 * The filter provides a sql WHERE which contains all tour filter, e.g. selected person, tour type,
 * photo and advanced tour filter
 */
public class AppFilter {

   private static final char               NL                    = UI.NEW_LINE;

   /**
    * Contains any available app filters
    */
   public static final Set<AppFilterType>  ANY_APP_FILTERS       = new HashSet<>();

   /**
    * Contains mostly fast app filters
    */
   private static final Set<AppFilterType> DEFAULT_APP_FILTERS   = new HashSet<>();

   /**
    * Contains only app filters which performed very fast
    */
   public static final Set<AppFilterType>  ONLY_FAST_APP_FILTERS = new HashSet<>();

   /**
    * Exclude all special app filters, so only default filters are applied, which are person, tour
    * type and tour data
    */
   public static final Set<AppFilterType>  NO_PHOTOS             = new HashSet<>();

   static {

      ANY_APP_FILTERS.add(AppFilterType.Photo);
      ANY_APP_FILTERS.add(AppFilterType.GeoLocation);
      ANY_APP_FILTERS.add(AppFilterType.Tag);
      ANY_APP_FILTERS.add(AppFilterType.Equipment);

      DEFAULT_APP_FILTERS.add(AppFilterType.Photo);
      DEFAULT_APP_FILTERS.add(AppFilterType.GeoLocation);

      ONLY_FAST_APP_FILTERS.add(AppFilterType.Photo);
   }

   private String       _sqlWhereClause = UI.EMPTY_STRING;

   private List<Object> _allParameters  = new ArrayList<>();

   /**
    * Create sql app filter which contains the mostly fast app filters
    */
   public AppFilter() {

      this(DEFAULT_APP_FILTERS);
   }

   /**
    * Creates the WHERE statement for the provided app filters
    *
    * @param appFilters
    */
   public AppFilter(final AppFilterType... appFilters) {

      final StringBuilder sql = new StringBuilder();

      for (final AppFilterType appFilter : appFilters) {

         if (AppFilterType.Person.equals(appFilter)) {

            /*
             * App filter: Person
             */
            final TourPerson activePerson = TourbookPlugin.getActivePerson();
            if (activePerson == null) {

               // select all people

            } else {

               // select only one person

               sql.append(" AND TourData.tourPerson_personId = ?" + NL); //$NON-NLS-1$

               _allParameters.add(activePerson.getPersonId());
            }

         } else if (AppFilterType.TourType.equals(appFilter)) {

            /*
             * App filter: Tour type
             */
            final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();
            if (activeTourTypeFilter != null) {

               final TourTypeSQLData sqlData = activeTourTypeFilter.getSQLData();

               sql.append(sqlData.getWhereString());

               _allParameters.addAll(sqlData.getParameters());
            }
         }
      }

      _sqlWhereClause = sql.toString();
   }

   /**
    * Creates the WHERE statement for the selected app filters by appending an AND to an existing
    * sql statement.
    * <p>
    * Tours are always filtered by
    * <p>
    * <li>Person</li>
    * <li>Photo</li>
    * <li>Tour Type</li>
    * <li>Tour Data</li>
    * <p>
    *
    * @param additionalAppFilter
    */
   public AppFilter(final Set<AppFilterType> additionalAppFilter) {

      final StringBuilder sqlWhere = new StringBuilder();

      /*
       * App filter: Person
       */
      final TourPerson activePerson = TourbookPlugin.getActivePerson();
      if (activePerson == null) {

         // select all people

      } else {

         // select only one person

         sqlWhere.append(" AND TourData.tourPerson_personId = ?" + NL); //$NON-NLS-1$

         _allParameters.add(activePerson.getPersonId());
      }

      /*
       * App filter: Photo
       */
      if (additionalAppFilter.contains(AppFilterType.Photo) && TourbookPlugin.getActivePhotoFilter()) {

         sqlWhere.append(" AND TourData.NumberOfPhotos > 0" + NL); //$NON-NLS-1$
      }

      /*
       * App filter: Tour type
       */
      final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();
      if (activeTourTypeFilter != null) {

         final TourTypeSQLData sqlData = activeTourTypeFilter.getSQLData();

         sqlWhere.append(sqlData.getWhereString());

         _allParameters.addAll(sqlData.getParameters());
      }

      /*
       * App Filter: Tour data
       */
      final SQLData tourSqlData = TourFilterManager.getSQL();
      if (tourSqlData != null) {

         sqlWhere.append(tourSqlData.getSqlString());

         _allParameters.addAll(tourSqlData.getParameters());
      }

      /*
       * App Filter: Tour geo location
       */
      if (additionalAppFilter.contains(AppFilterType.GeoLocation)) {

         final SQLData tourSqlGeoData = TourGeoFilter_Manager.getSQL();

         if (tourSqlGeoData != null) {

            sqlWhere.append(tourSqlGeoData.getSqlString());

            _allParameters.addAll(tourSqlGeoData.getParameters());
         }
      }

      /*
       * App Filter: Tour tags
       */
      if (additionalAppFilter.contains(AppFilterType.Tag)) {

         if (TourTagFilterManager.isFilterEnabled()) {

            final SQLData tagSqlData = new TourTagFilter().getSqlData();

            sqlWhere.append(tagSqlData.getSqlString());

            _allParameters.addAll(tagSqlData.getParameters());
         }
      }

      /*
       * App Filter: Equipment
       */
      if (additionalAppFilter.contains(AppFilterType.Equipment)) {

         if (TourEquipmentFilterManager.isFilterEnabled()) {

            final SQLData equipmentSqlData = new EquipmentFilter().getSqlData();

            sqlWhere.append(equipmentSqlData.getSqlString());

            _allParameters.addAll(equipmentSqlData.getParameters());
         }
      }

      _sqlWhereClause = sqlWhere.toString();
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
    * Sets the app filter parameters into the filter statement.
    *
    * @param statement
    * @param startIndex
    *           Sets the parameter start index, the first parameter is 1
    *
    * @return Returns the next parameter index which is the last paramter index +1
    *
    * @throws SQLException
    */
   public int setParameters(final PreparedStatement statement, final int startIndex) throws SQLException {

      int parameterIndex = startIndex;

      for (final Object parameter : _allParameters) {

         if (parameter instanceof Long) {

            statement.setLong(parameterIndex++, (Long) parameter);

         } else if (parameter instanceof Integer) {

            statement.setInt(parameterIndex++, (Integer) parameter);

         } else if (parameter instanceof Float) {

            statement.setFloat(parameterIndex++, (Float) parameter);

         } else if (parameter instanceof Double) {

            statement.setDouble(parameterIndex++, (Double) parameter);

         } else if (parameter instanceof String) {

            statement.setString(parameterIndex++, (String) parameter);

         } else {

            throw new RuntimeException("This SQL filter parameter class is not supported: " + parameter.getClass());//$NON-NLS-1$
         }
      }

      return parameterIndex;
   }

   @Override
   public String toString() {

      final int maxLen = 50;

      final List<Object> parameters = _allParameters != null
            ? _allParameters.subList(0, Math.min(_allParameters.size(), maxLen))
            : null;

      return UI.EMPTY_STRING

            + "SQLFilter" + NL //                                             //$NON-NLS-1$

            + " _sqlWhereClause      = " + _sqlWhereClause + NL //            //$NON-NLS-1$

            + " _allParameters       = " + parameters + NL //                 //$NON-NLS-1$
      ;
   }
}
