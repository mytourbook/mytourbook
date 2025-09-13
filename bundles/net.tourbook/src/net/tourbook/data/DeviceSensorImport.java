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

import net.tourbook.common.UI;

public class DeviceSensorImport {

   private static final char      NL = UI.NEW_LINE;

   public long                    dateTime;

   /**
    * Device index number in the import file, subsequent messages for the same device have the same
    * index number
    */
   public Short                   deviceIndex;
   public Short                   deviceType;
   public Short                   antPlusDeviceType;

   public String                  manufacturerName;
   public Integer                 manufacturerNumber;

   public String                  productName;
   public Integer                 productNumber;
   public Integer                 garminProductNumber;

   public String                  serialNumber;
   public Float                   softwareVersion;

   public DeviceSensorValueImport sensorValues;

   public DeviceSensorImport(final Short deviceIndex) {

      this.deviceIndex = deviceIndex;
   }

   @Override
   public String toString() {

      return "DeviceSensorRaw" + NL //                                        //$NON-NLS-1$

//            + "[" + NL //                                                   //$NON-NLS-1$

            + "      manufacturerNumber   = " + manufacturerNumber + NL //    //$NON-NLS-1$
            + "      manufacturerName     = " + manufacturerName + NL //      //$NON-NLS-1$
            + "      productNumber        = " + productNumber + NL //         //$NON-NLS-1$
            + "      productName          = " + productName + NL //           //$NON-NLS-1$
            + "      serialNumber         = " + serialNumber + NL //          //$NON-NLS-1$

//            + "]" + NL //                                                   //$NON-NLS-1$
      ;
   }

}
