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
package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;

import net.tourbook.device.garmin.fit.FitData;
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

         TourLogManager.subLog_INFO(String.format("[FIT] %s - Garmin file type is not defined", //$NON-NLS-1$
               fitData.getImportFilePathName()));

      } else if (type != File.ACTIVITY) {

         TourLogManager.subLog_INFO(String.format("[FIT] %s - Garmin file type is not an ACTIVITY, it is %s", //$NON-NLS-1$
               fitData.getImportFilePathName(),
               type.name()));
      }

      /*
       * Serial Number
       */
      final Long serialNumber = mesg.getSerialNumber();
      if (serialNumber == null) {

         TourLogManager.subLog_INFO(String.format("[FIT] %s - File serial number is missing, device id cannot not be set", //$NON-NLS-1$
               fitData.getImportFilePathName()));

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

            fitData.setGarminProduct("EDGE 1030"); //$NON-NLS-1$

         } else {
            fitData.setGarminProduct(garminProductId.toString());
         }
      }
   }

}
