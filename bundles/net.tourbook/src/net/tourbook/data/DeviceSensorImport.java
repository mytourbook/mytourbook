/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;

import net.tourbook.common.UI;

public class DeviceSensorImport {

   private static final char      NL           = UI.NEW_LINE;

   public long                    dateTime;

   /**
    * Device index number in the import file, subsequent messages for the same device have the same
    * index number
    */
   public Short                   deviceIndex;
   public Short                   deviceType;
   public Short                   antPlusDeviceType;

   public Integer                 manufacturerNumber;
   private String                 manufacturerName;

   public Integer                 productNumber;
   private String                 productName;
   public Integer                 garminProductNumber;

   public String                  serialNumber;
   public Float                   softwareVersion;

   /**
    * Set values for ANY sensor, to do not skip sensors, which have no sensor values at all,
    * otherwise they are not displayed in the sensor view !!!
    */
   public DeviceSensorValueImport sensorValues = new DeviceSensorValueImport();

   public DeviceSensorImport(final Short deviceIndex) {

      this.deviceIndex = deviceIndex;
   }

   private String createManufacturerName_Text() {

      String manufacturerName = UI.EMPTY_STRING;

      if (manufacturerNumber != null) {
         manufacturerName = Manufacturer.getStringFromValue(manufacturerNumber);
      }

      if (manufacturerName.length() == 0 && manufacturerNumber != null) {
         manufacturerName = manufacturerNumber.toString();
      }

      return manufacturerName;
   }

   private String createProductName_Text() {

      String sensorProductName = UI.EMPTY_STRING;

      if (garminProductNumber != null) {
         sensorProductName = GarminProduct.getStringFromValue(garminProductNumber);
      }

      if (sensorProductName.length() == 0 && productName != null) {
         sensorProductName = productName;
      }

      if (sensorProductName.length() == 0 && productNumber != null) {
         sensorProductName = productNumber.toString();
      }

      return sensorProductName;
   }

   public String getManufacturerName() {

      if (manufacturerName == null) {

         manufacturerName = createManufacturerName_Text();
      }

      return manufacturerName;
   }

   public String getProductName() {

      if (productName == null) {

         productName = createProductName_Text();
      }

      return productName;
   }

   public void setManufacturerName(final String manufacturerName) {

      this.manufacturerName = manufacturerName;
   }

   public void setProductName(final String productName) {

      this.productName = productName;
   }

   @Override
   public String toString() {

      return "DeviceSensorImport" + NL //                                     //$NON-NLS-1$

            + "      deviceIndex          = " + deviceIndex + NL //           //$NON-NLS-1$

            + "      manufacturerNumber   = " + manufacturerNumber + NL //    //$NON-NLS-1$
            + "      manufacturerName     = " + manufacturerName + NL //      //$NON-NLS-1$

            + "      productNumber        = " + productNumber + NL //         //$NON-NLS-1$
            + "      productName          = " + productName + NL //           //$NON-NLS-1$
            + "      garminProductNumber  = " + garminProductNumber + NL //   //$NON-NLS-1$

            + "      serialNumber         = " + serialNumber + NL //          //$NON-NLS-1$
            + "      deviceType           = " + deviceType + NL //            //$NON-NLS-1$

            + "      softwareVersion      = " + softwareVersion + NL //       //$NON-NLS-1$
            + "      sensorValues         = " + sensorValues + NL //          //$NON-NLS-1$

      ;
   }

}
