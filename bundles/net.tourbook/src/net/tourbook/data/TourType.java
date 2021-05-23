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
package net.tourbook.data;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import net.tourbook.common.color.ColorUtil;
import net.tourbook.database.TourDatabase;

import org.eclipse.swt.graphics.RGB;

@Entity
public class TourType implements Comparable<Object> {

   public static final int  DB_LENGTH_NAME                        = 100;

   /** Width/height of the tour type image. */
   public static final int  TOUR_TYPE_IMAGE_SIZE                  = 16;

   /** Color which is transparent in the tour type image. */
   public static final RGB  TRANSPARENT_COLOR                     = new RGB(0x01, 0xfe, 0x00);

   public static final long IMAGE_KEY_DIALOG_SELECTION            = -2;

   /**
    * Must be below 0 because a tour type can have a 0 id.
    */
   public static final long TOUR_TYPE_IS_NOT_USED                 = -10;

   /**
    * Must be below 0 because a tour type can have a 0 id.
    */
   public static final long TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA = -20;

   /**
    * Manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static int       _createCounter                        = 0;

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long             typeId                                = TourDatabase.ENTITY_IS_NOT_SAVED;

   @Basic(optional = false)
   private String           name;
   //
   private short            colorBrightRed;
   private short            colorBrightGreen;
   private short            colorBrightBlue;
   //
   private short            colorDarkRed;
   private short            colorDarkGreen;
   private short            colorDarkBlue;
   //
   private short            colorLineRed;
   private short            colorLineGreen;
   private short            colorLineBlue;
   //
   private short            colorTextRed;
   private short            colorTextGreen;
   private short            colorTextBlue;
   //
   private int              colorLine_Dark;
   private int              colorText_Dark;

   /**
    * unique id for manually created tour types because the {@link #typeId} is -1 when it's not
    * persisted
    */
   @Transient
   private long             _createId                             = 0;

   /**
    * default constructor used in ejb
    */
   public TourType() {}

   public TourType(final String name) {

      this.name = name;

      _createId = ++_createCounter;
   }

   @Override
   public int compareTo(final Object other) {

      // default sorting for tour types is by name

      if (other instanceof TourType) {
         final TourType otherTourType = (TourType) other;
         return name.compareTo(otherTourType.getName());
      }

      return 0;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }

      final TourType other = (TourType) obj;

      if (_createId == 0) {

         // tour type is from the database
         if (typeId != other.typeId) {
            return false;
         }
      } else {

         // tour type was create or imported
         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
   }

   public int getColorLine_Dark() {
      return colorLine_Dark;
   }

   public int getColorText_Dark() {
      return colorText_Dark;
   }

   public long getCreateId() {
      return _createId;
   }

   /**
    * @return Returns the name for the tour type
    */
   public String getName() {
      return name;
   }

   public RGB getRGBBright() {

      return new RGB(colorBrightRed, colorBrightGreen, colorBrightBlue);
   }

   public RGB getRGBDark() {

      return new RGB(colorDarkRed, colorDarkGreen, colorDarkBlue);
   }

   public RGB getRGBLine() {
      return new RGB(colorLineRed, colorLineGreen, colorLineBlue);
   }

   public RGB getRGBText() {
      return new RGB(colorTextRed, colorTextGreen, colorTextBlue);
   }

   /**
    * @return Returns the type id, this can be the saved type id or
    *         {@link TourDatabase#ENTITY_IS_NOT_SAVED}
    */
   public long getTypeId() {
      return typeId;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (_createId ^ (_createId >>> 32));
      result = prime * result + (int) (typeId ^ (typeId >>> 32));
      return result;
   }

   /**
    * Set bright gradient background color
    *
    * @param rgbBright
    */
   public void setColorBright(final RGB rgbBright) {

      colorBrightRed = (short) rgbBright.red;
      colorBrightGreen = (short) rgbBright.green;
      colorBrightBlue = (short) rgbBright.blue;
   }

   /**
    * Set dark gradient background color
    *
    * @param rgbDark
    */
   public void setColorDark(final RGB rgbDark) {

      colorDarkRed = (short) rgbDark.red;
      colorDarkGreen = (short) rgbDark.green;
      colorDarkBlue = (short) rgbDark.blue;
   }

   public void setColorLine(final RGB rgbLine_Light, final RGB rgbLine_Dark) {

      colorLineRed = (short) rgbLine_Light.red;
      colorLineGreen = (short) rgbLine_Light.green;
      colorLineBlue = (short) rgbLine_Light.blue;

      colorLine_Dark = ColorUtil.getColorValue(rgbLine_Dark);
   }

   /**
    * @param bright
    *           Gradient bright color
    * @param dark
    *           Gradient dark color
    * @param line_Light
    * @param line_Dark
    * @param text_Light
    * @param text_Dark
    */
   public void setColors(final RGB bright,
                         final RGB dark,

                         final RGB line_Light,
                         final RGB line_Dark,

                         final RGB text_Light,
                         final RGB text_Dark) {

      setColorBright(bright);
      setColorDark(dark);

      setColorLine(line_Light, line_Dark);
      setColorText(text_Light, text_Dark);
   }

   public void setColorText(final RGB rgbText_Light, final RGB rgbText_Dark) {

      colorTextRed = (short) rgbText_Light.red;
      colorTextGreen = (short) rgbText_Light.green;
      colorTextBlue = (short) rgbText_Light.blue;

      colorText_Dark = ColorUtil.getColorValue(rgbText_Dark);
   }

   public void setName(final String name) {
      this.name = name;
   }

   /**
    * This is a very special case for a not saved tour type
    */
   public void setTourId_NotDefinedInTourData() {

      typeId = TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA;
   }

   @Override
   public String toString() {
      return "TourType [typeId=" + typeId + ", name=" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

}
