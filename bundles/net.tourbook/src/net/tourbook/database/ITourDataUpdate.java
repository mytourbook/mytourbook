/*******************************************************************************
 * Copyright (C) 2022, 2025 Frédéric Bard
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

import java.util.List;

import net.tourbook.data.TourData;

/**
 * Interface to update tour values for {@link TourData}
 */
public interface ITourDataUpdate {

   /**
    * @return Returns the new database DATA version
    */
   public int getDatabaseVersion();

   /**
    * @return Returns all tour id's which should be processed or <code>null</code> that all tours
    *         are being processed
    */
   public List<Long> getTourIDs();

   /**
    * @param tourData
    *
    * @return Returns <code>true</code> when <code>tourData</code> should be saved
    */
   public boolean updateTourData(TourData tourData);
}
