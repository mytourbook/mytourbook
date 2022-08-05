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

import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.database.TourDatabase;

import org.eclipse.swt.graphics.RGB;

@Entity
public class TourType implements Comparable<Object> {

   private static final char          NL                                    = UI.NEW_LINE;

   public static final int            DB_LENGTH_NAME                        = 100;

   /** Width/height of the tour type image. */
   public static final int            TOUR_TYPE_IMAGE_SIZE                  = 16;

   /**
    * Color which is transparent in the tour type image.
    * <p>
    * The color is used in the easy import view, the previous color looked really ugly with a dark
    * background.
    */
   public static final RGB            TRANSPARENT_COLOR                     = new RGB(67, 67, 67);

   public static final long           IMAGE_KEY_DIALOG_SELECTION            = -2;

   /**
    * Must be below 0 because a tour type can have a 0 id.
    */
   public static final long           TOUR_TYPE_IS_NOT_USED                 = -10;

   /**
    * Must be below 0 because a tour type can have a 0 id.
    */
   public static final long           TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA = -20;

   /**
    * Manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static final AtomicInteger _createCounter                        = new AtomicInteger();

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       typeId                                = TourDatabase.ENTITY_IS_NOT_SAVED;

   @Basic(optional = false)
   private String                     name;

   private int                        color_Gradient_Bright;
   private int                        color_Gradient_Dark;

   private int                        color_Line_LightTheme;
   private int                        color_Line_DarkTheme;

   private int                        color_Text_LightTheme;
   private int                        color_Text_DarkTheme;

   /**
    * unique id for manually created tour types because the {@link #typeId} is -1 when it's not
    * persisted
    */
   @Transient
   private long                       _createId                             = 0;

   /**
    * Default constructor used in ejb
    */
   public TourType() {}

   public TourType(final String name) {

      this.name = name;

      _createId = _createCounter.incrementAndGet();
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
      return color_Line_DarkTheme;
   }

   public int getColorText_Dark() {
      return color_Text_DarkTheme;
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

   public RGB getRGB_Gradient_Bright() {
      return ColorUtil.createRGB(color_Gradient_Bright);
   }

   public RGB getRGB_Gradient_Dark() {
      return ColorUtil.createRGB(color_Gradient_Dark);
   }

   public RGB getRGB_Line_DarkTheme() {
      return ColorUtil.createRGB(color_Line_DarkTheme);
   }

   public RGB getRGB_Line_LightTheme() {
      return ColorUtil.createRGB(color_Line_LightTheme);
   }

   public RGB getRGB_Line_Themed() {

      return UI.IS_DARK_THEME
            ? getRGB_Line_DarkTheme()
            : getRGB_Line_LightTheme();
   }

   public RGB getRGB_Text_DarkTheme() {
      return ColorUtil.createRGB(color_Text_DarkTheme);
   }

   public RGB getRGB_Text_LightTheme() {
      return ColorUtil.createRGB(color_Text_LightTheme);
   }

   public RGB getRGB_Text_Themed() {

      return UI.IS_DARK_THEME
            ? getRGB_Text_DarkTheme()
            : getRGB_Text_LightTheme();
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
   public void setColor_Gradient_Bright(final RGB rgbBright) {

      color_Gradient_Bright = ColorUtil.getColorValue(rgbBright);
   }

   /**
    * Set dark gradient background color
    *
    * @param rgbDark
    */
   public void setColor_Gradient_Dark(final RGB rgbDark) {

      color_Gradient_Dark = ColorUtil.getColorValue(rgbDark);
   }

   public void setColor_Line(final RGB rgbLine_Light, final RGB rgbLine_Dark) {

      color_Line_LightTheme = ColorUtil.getColorValue(rgbLine_Light);
      color_Line_DarkTheme = ColorUtil.getColorValue(rgbLine_Dark);
   }

   public void setColor_Text(final RGB rgbText_Light, final RGB rgbText_Dark) {

      color_Text_LightTheme = ColorUtil.getColorValue(rgbText_Light);
      color_Text_DarkTheme = ColorUtil.getColorValue(rgbText_Dark);
   }

   /**
    * @param gradient_Bright
    *           Gradient bright color
    * @param gradient_Dark
    *           Gradient dark color
    * @param line_LightTheme
    * @param line_DarkTheme
    * @param text_LightTheme
    * @param text_DarkTheme
    */
   public void setColors(final RGB gradient_Bright,
                         final RGB gradient_Dark,

                         final RGB line_LightTheme,
                         final RGB line_DarkTheme,

                         final RGB text_LightTheme,
                         final RGB text_DarkTheme) {

      setColor_Gradient_Bright(gradient_Bright);
      setColor_Gradient_Dark(gradient_Dark);

      setColor_Line(line_LightTheme, line_DarkTheme);
      setColor_Text(text_LightTheme, text_DarkTheme);
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

      return "TourType [typeId=" + typeId + ", name=" + name + "]" + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      ;
   }

}
