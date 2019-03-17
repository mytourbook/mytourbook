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
package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;

import net.tourbook.tour.TourLogManager;

public class MesgListener_FileId extends AbstractMesgListener implements FileIdMesgListener {

   public MesgListener_FileId(final FitData fitData) {
      super(fitData);
   }

   @Override
   public void onMesg(final FileIdMesg mesg) {

      /*
       * File Type
       */
      final File type = mesg.getType();

      if (type == null) {

         TourLogManager.logError("Garmin file type is not defined");//$NON-NLS-1$

      } else if (type != File.ACTIVITY) {

         TourLogManager.logError("Garmin file type is not an ACTIVITY, it is " + type.name());//$NON-NLS-1$
      }

      /*
       * Serial Number
       */
      final Long serialNumber = mesg.getSerialNumber();
      if (serialNumber == null) {
         TourLogManager.logError("File serial number is missing, device id cannot not be set");//$NON-NLS-1$
      } else {
         fitData.setDeviceId(serialNumber.toString());
      }

      /*
       * Manufacturer
       */
      final Integer manufacturerId = mesg.getManufacturer();
      if (manufacturerId != null) {

         final String manufacturerText = Manufacturer.getStringFromValue(manufacturerId);

         if (manufacturerText.length() > 0) {
            fitData.setManufacturer(manufacturerText);
         } else {
            fitData.setManufacturer(manufacturerId.toString());
         }
      }

      /*
       * Product
       */
      final Integer garminProductId = mesg.getGarminProduct();
      if (garminProductId != null) {

         final String garminProductName = GarminProduct.getStringFromValue(garminProductId);

         if (garminProductName.length() > 0) {

            fitData.setGarminProduct(garminProductName);

         } else if (garminProductId == 2713) {

            // Garmin Edge 1030 is not yet in the product list

            fitData.setGarminProduct("EDGE 1030");

         } else {
            fitData.setGarminProduct(garminProductId.toString());
         }
      }
   }

}
