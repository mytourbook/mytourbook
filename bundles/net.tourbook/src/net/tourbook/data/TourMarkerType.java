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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

@Entity
public class TourMarkerType implements Comparable<Object>, Serializable {

   private static final long          serialVersionUID       = 1L;

   private static final char          NL                     = UI.NEW_LINE;

   public static final int            DB_LENGTH_NAME         = 1000;
   public static final int            DB_LENGTH_DESCRIPTION  = 10000;

   private static final AtomicInteger _createCounter         = new AtomicInteger();

   /** Width/height of the marker type image */
   public static final int            MARKER_TYPE_IMAGE_SIZE = 16;

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @JsonProperty
   private long                       markerTypeID           = TourDatabase.ENTITY_IS_NOT_SAVED;

   @Basic(optional = false)
   @JsonProperty
   private String                     name;

   private String                     description;

   /**
    * Foreground color value
    */
   private int                        foregroundColor        = 0x0;

   /**
    * Background color value
    */
   private int                        backgroundColor        = 0xffffff;

   /**
    * Unique id for manually created tour marker types because the {@link #markerTypeID} is -1 when
    * it's not persisted
    */
   @Transient
   private long                       _createId              = 0;

   @Transient
   private RGB                        _foregroundRGB;

   @Transient
   private RGB                        _backgroundRGB;

   @Transient
   private Color                      _foregroundColorSWT;

   @Transient
   private Color                      _backgroundColorSWT;

   @Transient
   private java.awt.Color             _foregroundColorAWT;

   @Transient
   private java.awt.Color             _backgroundColorAWT;

   /**
    * Default constructor used in ejb
    */
   public TourMarkerType() {}

   public TourMarkerType(final String name) {

      this.name = name;

      _createId = _createCounter.incrementAndGet();
   }

   @Override
   public int compareTo(final Object other) {

      // default sorting for tour marker types is by name

      if (other instanceof TourMarkerType) {

         final TourMarkerType otherTourMarkerType = (TourMarkerType) other;

         return name.compareTo(otherTourMarkerType.getTypeName());
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

      final TourMarkerType other = (TourMarkerType) obj;

      if (_createId == 0) {

         // tour marker type is from the database
         if (markerTypeID != other.markerTypeID) {
            return false;
         }
      } else {

         // tour marker type was create or imported
         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
   }

   public java.awt.Color getBackgroundColorAWT() {

      if (_backgroundColorAWT == null) {

         _backgroundColorAWT = ColorUtil.getColorAWT(backgroundColor);
      }

      return _backgroundColorAWT;
   }

   public Color getBackgroundColorSWT() {

      if (_backgroundColorSWT == null) {

         _backgroundColorSWT = ColorUtil.getColorSWT(backgroundColor);
      }

      return _backgroundColorSWT;
   }

   public RGB getBackgroundRGB() {

      if (_backgroundRGB == null) {

         _backgroundRGB = ColorUtil.createRGB(backgroundColor);
      }

      return _backgroundRGB;
   }

   public long getCreateId() {

      return _createId;
   }

   public String getDescription() {

      return description == null ? UI.EMPTY_STRING : description;
   }

   public java.awt.Color getForegroundColorAWT() {

      if (_foregroundColorAWT == null) {

         _foregroundColorAWT = ColorUtil.getColorAWT(foregroundColor);
      }

      return _foregroundColorAWT;
   }

   public Color getForegroundColorSWT() {

      if (_foregroundColorSWT == null) {

         _foregroundColorSWT = ColorUtil.getColorSWT(foregroundColor);
      }

      return _foregroundColorSWT;
   }

   public RGB getForegroundRGB() {

      if (_foregroundRGB == null) {

         _foregroundRGB = ColorUtil.createRGB(foregroundColor);
      }

      return _foregroundRGB;
   }

   /**
    * @return Returns the entity id, this can be the saved marker type id or
    *         {@link TourDatabase#ENTITY_IS_NOT_SAVED}
    */
   public long getId() {

      return markerTypeID;
   }

   /**
    * @return Returns the name for the tour marker type
    */
   public String getTypeName() {

      return name == null ? UI.EMPTY_STRING : name;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      result = prime * result + (int) (_createId ^ (_createId >>> 32));
      result = prime * result + (int) (markerTypeID ^ (markerTypeID >>> 32));

      return result;
   }

   public void setBackgroundColor(final RGB colorRGB) {

      backgroundColor = ColorUtil.getColorValue(colorRGB);

      _backgroundRGB = colorRGB;
      _backgroundColorSWT = new Color(colorRGB);
   }

   public void setDescription(final String description) {

      this.description = description;
   }

   public void setForegroundColor(final RGB colorRGB) {

      foregroundColor = ColorUtil.getColorValue(colorRGB);

      _foregroundRGB = colorRGB;
      _foregroundColorSWT = new Color(colorRGB);
   }

   public void setName(final String name) {
      this.name = name;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourMarkerType" + NL //                   //$NON-NLS-1$
            + " markerTypeID = " + markerTypeID + NL //  //$NON-NLS-1$
            + " name         = " + name + NL //          //$NON-NLS-1$

      ;
   }

}
