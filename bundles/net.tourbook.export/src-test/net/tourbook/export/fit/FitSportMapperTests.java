/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.export.fit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

import net.tourbook.data.TourType;

import org.junit.jupiter.api.Test;

public class FitSportMapperTests {

   @Test
   void testFitSportMapper() {

      assertAll(
            () -> assertEquals(Sport.CYCLING, FitSportMapper.mapTourTypeToSport(new TourType("Cycling"))), //$NON-NLS-1$
            () -> assertEquals(Sport.WALKING, FitSportMapper.mapTourTypeToSport(new TourType("WALKING"))), //$NON-NLS-1$
            () -> assertEquals(Sport.GENERIC, FitSportMapper.mapTourTypeToSport(new TourType("crosscountry"))), //$NON-NLS-1$
            () -> assertEquals(Sport.HIKING, FitSportMapper.mapTourTypeToSport(new TourType("Hiking"))), //$NON-NLS-1$
            () -> assertEquals(Sport.RUNNING, FitSportMapper.mapTourTypeToSport(new TourType("Trail"))), //$NON-NLS-1$
            () -> assertEquals(Sport.SNOWSHOEING, FitSportMapper.mapTourTypeToSport(new TourType("Snowshoeing"))), //$NON-NLS-1$
            () -> assertEquals(Sport.CROSS_COUNTRY_SKIING, FitSportMapper.mapTourTypeToSport(new TourType("cross-country"))), //$NON-NLS-1$
            () -> assertEquals(Sport.GENERIC, FitSportMapper.mapTourTypeToSport(null)));
   }

   @Test
   void testFitSubSportMapper() {

      assertAll(
            () -> assertEquals(SubSport.SKATE_SKIING, FitSportMapper.mapTourTypeToSubSport(new TourType("Skating"))), //$NON-NLS-1$
            () -> assertEquals(SubSport.TRAIL, FitSportMapper.mapTourTypeToSubSport(new TourType("Trail"))), //$NON-NLS-1$
            () -> assertEquals(SubSport.VIRTUAL_ACTIVITY, FitSportMapper.mapTourTypeToSubSport(new TourType("Trainer"))), //$NON-NLS-1$
            () -> assertEquals(SubSport.GENERIC, FitSportMapper.mapTourTypeToSubSport(null)));
   }
}
