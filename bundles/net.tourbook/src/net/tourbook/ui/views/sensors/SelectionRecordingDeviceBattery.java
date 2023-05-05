/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import org.eclipse.jface.viewers.ISelection;

/**
 * This selection is fired when a battery SoC is selected.
 */
public class SelectionRecordingDeviceBattery implements ISelection {

   private long tourId;
   private short tourYear;

   /**
    * @param sensor
    * @param tourId
    * @param tourYear
    */
   public SelectionRecordingDeviceBattery(final long tourId, final short tourYear) {

      this.tourId = tourId;
      this.tourYear = tourYear;
   }

   public long getTourId() {
      return tourId;
   }

   public short getTourYear() {
      return tourYear;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

}
