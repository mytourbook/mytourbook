/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.io.Serializable;
import java.time.LocalDateTime;

import net.tourbook.common.UI;

/**
 * Metadata for the original photo image file.
 */
public class PhotoImageMetadata implements Serializable {

   private static final char NL               = UI.NEW_LINE;

   private static final long serialVersionUID = 1L;

   /**
    * Is <code>true</code> when the image data are loaded from the image file, otherwise it is
    * <code>false</code>.
    */
   public boolean            isExifFromImage;

   /**
    * Last modified date/time of the image file which is provided by the file system with the
    * system time zone.
    */
   public LocalDateTime      fileDateTime;

   /**
    * Exif date/time which has no time zone.
    */
   public LocalDateTime      exifDateTime;

   public int                imageWidth       = Integer.MIN_VALUE;
   public int                imageHeight      = Integer.MIN_VALUE;

   public int                orientation      = 1;

   public double             imageDirection   = Double.MIN_VALUE;

   public double             altitude         = Double.MIN_VALUE;

   /**
    * Double.MIN_VALUE cannot be used, it cannot be saved in the database. 0 is the value when the
    * value is not set !!!
    */
   public double             latitude         = 0;
   public double             longitude        = 0;

   public String             gpsAreaInfo;

   /**
    * Title
    */
   public String             objectName;

   /**
    * Description
    */
   public String             captionAbstract;

   /**
    * Camera or scanner name
    */
   public String             model;

   public PhotoImageMetadata() {}

   @Override
   public String toString() {

      return "PhotoImageMetadata [" + NL //                       //$NON-NLS-1$

            + "fileDateTime      =" + fileDateTime + NL //        //$NON-NLS-1$
            + "exifDateTime      =" + exifDateTime + NL //        //$NON-NLS-1$
            + "imageWidth        =" + imageWidth + NL //          //$NON-NLS-1$
            + "imageHeight       =" + imageHeight + NL //         //$NON-NLS-1$
            + "orientation       =" + orientation + NL //         //$NON-NLS-1$
            + "imageDirection    =" + imageDirection + NL //      //$NON-NLS-1$
            + "altitude          =" + altitude + NL //            //$NON-NLS-1$
            + "latitude          =" + latitude + NL //            //$NON-NLS-1$
            + "longitude         =" + longitude + NL //           //$NON-NLS-1$
            + "gpsAreaInfo       =" + gpsAreaInfo + NL //         //$NON-NLS-1$
            + "objectName        =" + objectName + NL //          //$NON-NLS-1$
            + "captionAbstract   =" + captionAbstract + NL //     //$NON-NLS-1$
            + "model             =" + model + NL //               //$NON-NLS-1$

            + "]"; //                                             //$NON-NLS-1$
   }
}
