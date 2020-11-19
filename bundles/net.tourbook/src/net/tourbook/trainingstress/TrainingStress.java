/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard and Contributors
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
package net.tourbook.trainingstress;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;

public abstract class TrainingStress {

   protected TourPerson _tourPerson;
   protected TourData   _tourData;

   public TrainingStress(final TourPerson tourPerson, final TourData tourData) {
      this._tourPerson = tourPerson;
      this._tourData = tourData;
   }

   /**
    * Method that calculates the training stress for a given tour and athlete.
    *
    * @return The training stress value
    */
   public int Compute() {
      if (_tourData == null || _tourData.timeSerie == null || _tourData.timeSerie.length < 2) {
         return 0;
      }

      return Compute(0, _tourData.timeSerie.length);
   }

   abstract int Compute(final int startIndex, final int endIndex);
}
