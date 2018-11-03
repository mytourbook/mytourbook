/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.IHoveredArea;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

/**
 * A waypoint is associated with a tour but the way point position is independently from a tour, it
 * has it's own lat/lon position.
 */
@Entity
public class TourWayPoint implements Cloneable, Comparable<Object>, IHoveredArea {

   private static final String        IMAGE_MAP_WAY_POINT_HOVERED =
         net.tourbook.map2.Messages.Image_Map_WayPoint_Hovered;

   public static final int            DB_LENGTH_NAME              = 1024;
   public static final int            DB_LENGTH_DESCRIPTION       = 4096;
   public static final int            DB_LENGTH_COMMENT           = 4096;
   public static final int            DB_LENGTH_SYMBOL            = 1024;
   public static final int            DB_LENGTH_CATEGORY          = 1024;

   @Transient
   private static Image               _twpHoveredImage;

   /**
    * Manually created way points or imported way points need a unique id to identify them, saved
    * way points are compared with the way point id.
    */
   @Transient
   private static final AtomicInteger _createCounter              = new AtomicInteger();

   /**
    * Unique id for the {@link TourWayPoint} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       wayPointId                  = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData                   tourData;

   // initialize with invalid values
   private double      longitude = Double.MIN_VALUE;

   private double      latitude  = Double.MIN_VALUE;

   /**
    * Absolute time in milliseconds since 1970-01-01T00:00:00Z with the default time zone.
    */
   private long        time;

   /**
    * Altitude in meters or {@link Float#MIN_VALUE} when not available.
    */
   private float       altitude  = Float.MIN_VALUE;
   private String      name;
   private String      description;
   private String      comment;

   private String      symbol;

   private String      category;

   /**
    * Text to display on the hyperlink, can be <code>null</code>
    *
    * @since DB version 28
    */
   // <urlname> Text to display on the <url> hyperlink
   private String      urlText;

   /**
    * URL associated with the waypoint, can be <code>null</code>
    *
    * @since DB version 28
    */
   // <url> URL associated with the waypoint
   private String      urlAddress;

   @Transient
   private GeoPosition _geoPosition;

   /**
    * Unique id for manually created waypoints because the {@link #wayPointId} is 0 when the
    * waypoint is not persisted.
    */
   @Transient
   private long        _createId = _createCounter.incrementAndGet();

   public TourWayPoint() {}

   public TourWayPoint clone(final TourData wpTourData) {

      try {

         // create a shallow copy
         final TourWayPoint newWayPoint = (TourWayPoint) super.clone();

         // set create id to uniquely identify the way point
         newWayPoint._createId = _createCounter.incrementAndGet();

         newWayPoint.wayPointId = TourDatabase.ENTITY_IS_NOT_SAVED;

         newWayPoint.tourData = wpTourData;

         return newWayPoint;

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   @Override
   public int compareTo(final Object other) {

      /*
       * Set default sorting by time or by id (creation time)
       */

      if (other instanceof TourWayPoint) {

         final TourWayPoint otherWP = (TourWayPoint) other;

         /*
          * Compare by time
          */
         if (time != 0 && otherWP.time != 0) {

            return time > otherWP.time ? 1 : -1;
         }

         /*
          * Compare by id
          */
         if (wayPointId == TourDatabase.ENTITY_IS_NOT_SAVED) {

            // waypoint is not yet persisted

            if (otherWP.wayPointId == TourDatabase.ENTITY_IS_NOT_SAVED) {

               // both way points are created and not persisted

               return _createId > otherWP._createId ? 1 : -1;
            }

            return -1;

         } else {

            // waypoint is persisted

            if (otherWP.wayPointId != TourDatabase.ENTITY_IS_NOT_SAVED) {

               // both way point is also persisted
               return wayPointId > otherWP.wayPointId ? 1 : -1;
            }

            return 1;
         }
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

      final TourWayPoint otherWP = (TourWayPoint) obj;

      if (wayPointId == TourDatabase.ENTITY_IS_NOT_SAVED) {

         // tour was create or imported

         if (_createId == otherWP._createId) {
            return true;
         }

      } else {

         // tour is from the database

         if (wayPointId == otherWP.wayPointId) {
            return true;
         }
      }

      return false;
   }

   /**
    * @return Returns altitude in meters or {@link Float#MIN_VALUE} when altitude is not set.
    */
   public float getAltitude() {
      return altitude;
   }

   /**
    * @return Returns altitude in meters as {@link Double} or {@link Float#MIN_VALUE} when altitude
    *         is not set.
    *         <p>
    *         <b>Have absolute <i>NO</i> idea why velocity is not working with float values.</b>
    */
   public double getAltitudeD() {
      return altitude;
   }

   public String getCategory() {
      return category;
   }

   public String getComment() {
      return comment;
   }

   /**
    * @return Returns a unique id for manually created way points because the {@link #wayPointId} is
    *         {@link TourDatabase#ENTITY_IS_NOT_SAVED} when it's not yet persisted
    */
   public long getCreateId() {
      return _createId;
   }

   public String getDescription() {
      return description;
   }

   @Override
   public Image getHoveredImage() {

      if (_twpHoveredImage != null) {
         return _twpHoveredImage;
      }

      final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();

      imageRegistry.put(IMAGE_MAP_WAY_POINT_HOVERED, TourbookPlugin.getImageDescriptor(IMAGE_MAP_WAY_POINT_HOVERED));

      _twpHoveredImage = imageRegistry.get(IMAGE_MAP_WAY_POINT_HOVERED);

      return _twpHoveredImage;
   }

   public double getLatitude() {
      return latitude;
   }

   public double getLongitude() {
      return longitude;
   }

   public String getName() {
      return name;
   }

   public GeoPosition getPosition() {

      if (_geoPosition == null) {
         _geoPosition = new GeoPosition(latitude, longitude);
      }

      return _geoPosition;
   }

   public String getSymbol() {
      return symbol;
   }

   /**
    * @return Returns the waypoint time in milliseconds since 1970-01-01T00:00:00Z with the default
    *         time zone.
    */
   public long getTime() {
      return time;
   }

   public TourData getTourData() {
      return tourData;
   }

   public String getUrlAddress() {
      return urlAddress;
   }

   public String getUrlText() {
      return urlText;
   }

   /**
    * @return Returns the persistence id or {@link TourDatabase#ENTITY_IS_NOT_SAVED} when the entity
    *         is not yet saved
    */
   public long getWayPointId() {
      return wayPointId;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      if (wayPointId == TourDatabase.ENTITY_IS_NOT_SAVED) {
         result = prime * result + (int) (_createId ^ (_createId >>> 32));
      } else {
         result = prime * result + (int) (wayPointId ^ (wayPointId >>> 32));
      }

      return result;
   }

   /**
    * @return Returns <code>true</code> when altitude is available.
    *         <p>
    *         This method is used when exporting waypoints.
    */
   public boolean hasValidAltitude() {
      return altitude != Float.MIN_VALUE;
   }

   /**
    * Checks if VARCHAR fields have the correct length
    *
    * @return Returns <code>true</code> when the data are valid and can be saved
    */
   public boolean isValidForSave() {

      /*
       * Check: url text
       */
      FIELD_VALIDATION fieldValidation = TourDatabase.isFieldValidForSave(
            urlText,
            TourMarker.DB_LENGTH_URL_TEXT,
            Messages.Db_Field_TourMarker_UrlText);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         urlText = urlText.substring(0, TourMarker.DB_LENGTH_URL_TEXT);
      }

      /*
       * Check: url address
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            urlAddress,
            TourMarker.DB_LENGTH_URL_ADDRESS,
            Messages.Db_Field_TourMarker_UrlAddress);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         urlAddress = urlAddress.substring(0, TourMarker.DB_LENGTH_URL_ADDRESS);
      }

      return true;
   }

   public void setAltitude(final Float altitude) {
      this.altitude = altitude;
   }

   public void setCategory(final String category) {
      this.category = category;
   }

   public void setComment(final String comment) {
      this.comment = comment;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public void setLatitude(final double latitude) {

      this.latitude = latitude;

      if (_geoPosition != null) {
         _geoPosition.latitude = latitude;
      }
   }

   public void setLongitude(final double longitude) {

      this.longitude = longitude;

      if (_geoPosition != null) {
         _geoPosition.longitude = longitude;
      }
   }

   public void setName(final String name) {
      this.name = name;
   }

   public void setSymbol(final String symbol) {
      this.symbol = symbol;
   }

   public void setTime(final long time) {
      this.time = time;
   }

   public void setTourData(final TourData newTourData) {
      tourData = newTourData;
   }

   public void setUrlAddress(final String urlAddress) {
      this.urlAddress = urlAddress;
   }

   public void setUrlText(final String urlText) {
      this.urlText = urlText;
   }

   @Override
   public String toString() {
      return "TourWayPoint [" //$NON-NLS-1$

            + ("time=" + Instant.ofEpochMilli(time) + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("_createId=" + _createId + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("wayPointId=" + wayPointId + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("latitude=" + latitude + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("longitude=" + longitude + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //				+ ("altitude=" + altitude + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //				+ ("name=" + name + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //				+ ("description=" + description + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //				+ ("comment=" + comment + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //				+ ("symbol=" + symbol + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //				+ ("category=" + category) //$NON-NLS-1$

            + "]\n"; //$NON-NLS-1$
   }
}
