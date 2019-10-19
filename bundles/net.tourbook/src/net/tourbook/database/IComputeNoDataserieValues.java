/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.tourbook.data.TourData;

/**
 * Interface to compute tour values for {@link TourData}
 */
public interface IComputeNoDataserieValues {

   /**
    * @param originalTourData
    *           {@link TourData} which is not yet modified
    * @param sqlUpdateStatement
    * @return Returns <code>true</code> when {@link TourData} was modified and the tour needs to be
    *         saved
    * @throws SQLException
    */
   public boolean computeTourValues(TourData originalTourData, PreparedStatement sqlUpdateStatement) throws SQLException;

   /**
    * @return Returns the text which is displayed at the end of the task to the user to show the
    *         result of the computation. When <code>null</code> it will be ignored.
    */
   public String getResultText();

   /**
    * @return Returns a SQL statement which must be a INSERT, UPDATE or DELETE.
    */
   public String getSQLUpdateStatement();
}
