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
package net.tourbook.ui.views.collateTours;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourItem;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;

public abstract class TVICollatedTour extends TreeViewerItem implements ITourItem {

   static ZonedDateTime calendar8 = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);
   static final String  SQL_SUM_COLUMNS;

   static {

      SQL_SUM_COLUMNS = UI.EMPTY_STRING
            //
            + "SUM(TOURDISTANCE)," // 								0	//$NON-NLS-1$
            + "SUM(tourDeviceTime_Elapsed)," //					1	//$NON-NLS-1$
            + "SUM(TourComputedTime_Moving)," //				2	//$NON-NLS-1$
            + "SUM(TOURALTUP)," //									3	//$NON-NLS-1$
            + "SUM(TOURALTDOWN)," //								4	//$NON-NLS-1$
            + "SUM(1)," //											5	//$NON-NLS-1$
            //
            + "MAX(MAXSPEED)," //									6	//$NON-NLS-1$
            + "SUM(TOURDISTANCE)," //								7	//$NON-NLS-1$
            + "MAX(MAXALTITUDE)," //								8	//$NON-NLS-1$
            + "MAX(MAXPULSE)," //									9	//$NON-NLS-1$
            //
            + "AVG( CASE WHEN AVGPULSE = 0			THEN NULL ELSE AVGPULSE END)," //			10	//$NON-NLS-1$
            + "AVG( CASE WHEN AVGCADENCE = 0		THEN NULL ELSE AVGCADENCE END )," //		11	//$NON-NLS-1$
            + "AVG( CASE WHEN AvgTemperature = 0	THEN NULL ELSE DOUBLE(AvgTemperature) / TemperatureScale END )," //	12	//$NON-NLS-1$
            + "AVG( CASE WHEN WEATHERWINDDIR = 0	THEN NULL ELSE WEATHERWINDDIR END )," //	13	//$NON-NLS-1$
            + "AVG( CASE WHEN WEATHERWINDSPD = 0	THEN NULL ELSE WEATHERWINDSPD END )," //	14	//$NON-NLS-1$
            + "AVG( CASE WHEN RESTPULSE = 0			THEN NULL ELSE RESTPULSE END )," //			15	//$NON-NLS-1$
            //
            + "SUM(CALORIES)," //									16	//$NON-NLS-1$
            + "SUM(numberOfTimeSlices)," //							17	//$NON-NLS-1$
            + "SUM(numberOfPhotos)," //								18	//$NON-NLS-1$
            //
            + "SUM(frontShiftCount)," //							19	//$NON-NLS-1$
            + "SUM(rearShiftCount)," //								20	//$NON-NLS-1$
            + "SUM(TourDeviceTime_Recorded)"; //							21//$NON-NLS-1$
   }

   protected final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   CollatedToursView                collateToursView;

   String                           treeColumn;

   /**
    * Id's for the tags or <code>null</code> when tags are not available.
    */
   private ArrayList<Long>          _tagIds;
   HashSet<Long>                    sqlTagIds;

   /**
    * Tour start time in ms.
    */
   long                             colTourStartTime;

   String                           colTourTitle;
   long                             colPersonId;                               // tourPerson_personId
   long                             colCounter;
   long                             colCalories;

   long                             colDistance;
   long                             colRecordedTime;
   long                             colElapsedTime;
   long                             colMovingTime;

   long                             colBreakTime;
   long                             colPausedTime;
   long                             colAltitudeUp;
   long                             colAltitudeDown;

   float                            colMaxSpeed;
   long                             colMaxAltitude;

   long                             colMaxPulse;
   float                            colAvgSpeed;
   float                            colAvgPace;

   float                            colAvgPulse;
   float                            colAvgCadence;
   float                            colAvgTemperature;
   int                              colWindSpd;
   int                              colWindDir;

   String                           colClouds;
   int                              colRestPulse;
   int                              colWeekNo;
   String                           colWeekDay;

   int                              colWeekYear;
   int                              colNumberOfTimeSlices;
   int                              colNumberOfPhotos;

   int                              colDPTolerance;
   int                              colFrontShiftCount;

   int                              colRearShiftCount;

   TVICollatedTour(final CollatedToursView view) {

      collateToursView = view;
   }

   void addSumColumns(final ResultSet result, final int startIndex) throws SQLException {

      colDistance = result.getLong(startIndex + 0);

      colElapsedTime = result.getLong(startIndex + 1);
      colMovingTime = result.getLong(startIndex + 2);

      colAltitudeUp = result.getLong(startIndex + 3);
      colAltitudeDown = result.getLong(startIndex + 4);

      colCounter = result.getLong(startIndex + 5);

      colMaxSpeed = result.getFloat(startIndex + 6);

      // compute average speed/pace, prevent divide by 0
      final long dbDistance = result.getLong(startIndex + 7);

      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final long time = isPaceAndSpeedFromRecordedTime ? colRecordedTime : colMovingTime;
      colAvgSpeed = time == 0 ? 0 : 3.6f * dbDistance / time;
      colAvgPace = dbDistance == 0 ? 0 : time * 1000f / dbDistance;

      colMaxAltitude = result.getLong(startIndex + 8);
      colMaxPulse = result.getLong(startIndex + 9);

      colAvgPulse = result.getFloat(startIndex + 10);
      colAvgCadence = result.getFloat(startIndex + 11);
      colAvgTemperature = result.getFloat(startIndex + 12);

      colWindDir = result.getInt(startIndex + 13);
      colWindSpd = result.getInt(startIndex + 14);
      colRestPulse = result.getInt(startIndex + 15);

      colCalories = result.getLong(startIndex + 16);

      colNumberOfTimeSlices = result.getInt(startIndex + 17);
      colNumberOfPhotos = result.getInt(startIndex + 18);

      colFrontShiftCount = result.getInt(startIndex + 19);
      colRearShiftCount = result.getInt(startIndex + 20);

      colRecordedTime = result.getLong(startIndex + 21);

      colBreakTime = colElapsedTime - colMovingTime;
      colPausedTime = colElapsedTime - colRecordedTime;
   }

   public ArrayList<Long> getTagIds() {

      if (sqlTagIds != null && _tagIds == null) {
         _tagIds = new ArrayList<>(sqlTagIds);
      }

      return _tagIds;
   }

   @Override
   public Long getTourId() {
      return null;
   }

   public void setTagIds(final HashSet<Long> tagIds) {
      sqlTagIds = tagIds;
   }

}
