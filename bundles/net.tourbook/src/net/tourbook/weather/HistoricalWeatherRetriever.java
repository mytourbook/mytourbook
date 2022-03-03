/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.weather;

import net.tourbook.data.TourData;

public abstract class HistoricalWeatherRetriever {

   //todo fb I wanted to put the htpclient here but the unit tests can't access
   //the field form the mother class, is there a fix for it ??
   //      final Field field = OpenWeatherMapRetriever.class.getDeclaredField("httpClient"); //$NON-NLS-1$

   public TourData _tour;

   protected HistoricalWeatherRetriever(final TourData tourData) {
      _tour = tourData;
   }

   public abstract boolean retrieveHistoricalWeatherData();
}
