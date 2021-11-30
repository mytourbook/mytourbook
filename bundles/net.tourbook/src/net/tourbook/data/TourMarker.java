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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.tourChart.ChartLabel;
import net.tourbook.ui.tourChart.ChartLabelMarker;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.graphics.Rectangle;

/**
 * A tour marker has a position within a tour.
 *
 * <pre>
 *
 *  Planned features in 14.8:
 *
 *    - different icons, size, with/without text
 *    - new marker description field
 *    - create marker in the map
 *    - a marker can have another position than the tour track
 *
 *    icons from http://mapicons.nicolasmollet.com/
 * </pre>
 */

@Entity
@XmlType(name = "TourMarker")
@XmlRootElement(name = "TourMarker")
@XmlAccessorType(XmlAccessType.NONE)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "markerId")
public class TourMarker implements Cloneable, Comparable<Object>, IXmlSerializable {

   public static final int      DB_LENGTH_URL_TEXT    = 1024;
   public static final int      DB_LENGTH_URL_ADDRESS = 4096;

   /**
    * Visual position for markers, they must correspond to the position id LABEL_POS_*.
    */
   public static final String[] LABEL_POSITIONS;

   static {

      LABEL_POSITIONS = new String[] { //
            //
            Messages.Tour_Marker_Position_vertical_above, //               0
            Messages.Tour_Marker_Position_vertical_below, //               1
            Messages.Tour_Marker_Position_vertical_chart_top, //           2
            Messages.Tour_Marker_Position_vertical_chart_bottom, //        3
            Messages.Tour_Marker_Position_horizontal_above_left, //        4
            Messages.Tour_Marker_Position_horizontal_above_centered, //    5
            Messages.Tour_Marker_Position_horizontal_above_right, //       6
            Messages.Tour_Marker_Position_horizontal_below_left, //        7
            Messages.Tour_Marker_Position_horizontal_below_centered, //    8
            Messages.Tour_Marker_Position_horizontal_below_right, //       9
            Messages.Tour_Marker_Position_horizontal_left, //              10
            Messages.Tour_Marker_Position_horizontal_right, //             11
      };
   }

   public static final int            LABEL_POS_VERTICAL_ABOVE_GRAPH            = 0;
   public static final int            LABEL_POS_VERTICAL_BELOW_GRAPH            = 1;
   public static final int            LABEL_POS_VERTICAL_TOP_CHART              = 2;
   public static final int            LABEL_POS_VERTICAL_BOTTOM_CHART           = 3;
   public static final int            LABEL_POS_HORIZONTAL_ABOVE_GRAPH_LEFT     = 4;
   public static final int            LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED = 5;
   public static final int            LABEL_POS_HORIZONTAL_ABOVE_GRAPH_RIGHT    = 6;
   public static final int            LABEL_POS_HORIZONTAL_BELOW_GRAPH_LEFT     = 7;
   public static final int            LABEL_POS_HORIZONTAL_BELOW_GRAPH_CENTERED = 8;
   public static final int            LABEL_POS_HORIZONTAL_BELOW_GRAPH_RIGHT    = 9;
   public static final int            LABEL_POS_HORIZONTAL_GRAPH_LEFT           = 10;
   public static final int            LABEL_POS_HORIZONTAL_GRAPH_RIGHT          = 11;

   /**
    *
    */
   private static int                 _defaultSignImageMaxSize                  = -1;

   /**
    * manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static final AtomicInteger _createCounter                            = new AtomicInteger();

//   /**
//    * Marker sign image, can be <code>null</code>.
//    *
//    * @since Db version 24
//    */
//   @ManyToOne
//   private TourSign            tourSign;

   /**
    * Unique id for the {@link TourMarker} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long     markerId        = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData tourData;

   /**
    * Contains the marker type which is defined in {@link ChartLabel} like
    * {@link ChartLabel#MARKER_TYPE_DEVICE}
    */
   private int      type;

   /**
    * Time in seconds relative to the tour start. When value is not available it is set to
    * <code>-1</code>.
    */
   @XmlElement
   private int      time            = -1;

   /**
    * Absolute time of the tour marker in milliseconds since 1970-01-01T00:00:00Z.
    *
    * @since Db version 25
    */
   private long     tourTime        = Long.MIN_VALUE;

   /**
    * Distance field before db version 20, this field is required for data conversion AND <b>to load
    * entities</b> !!!
    */
   private int      distance        = -1;

   /**
    * Distance in meters in the metric system or <code>-1</code> when the distance is not available.
    * <p>
    * 20 == db version 20
    */
   @XmlElement
   private float    distance20      = -1;
   /**
    * Contains the marker label visual position which is defined in {@link ChartLabel} like
    * {@link ChartLabel#LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED}.
    */
   private int      visualPosition  =
         TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED;

   private int      labelXOffset;

   private int      labelYOffset;

   /**
    * Contains the type of the marker, this can be: crossing, hotel, view point.
    * <p>
    * THIS IS NOT USED.
    */
   private long     markerType;

   /**
    * position of this marker in the data serie
    */
   @XmlAttribute
   private int      serieIndex;

   @XmlElement
   private String   label           = UI.EMPTY_STRING;

   /**
    * This field is disabled since db version 24 because a {@link TourSign} can be categorized.
    */
   @SuppressWarnings("unused")
   private String   category        = UI.EMPTY_STRING;

   /**
    * Can be <code>null</code>
    *
    * @since db version 24
    */
   private String   description;

   /**
    * Can be <code>null</code>
    *
    * @since DB version 24
    */
   private String   urlText;

   /**
    * Can be <code>null</code>
    *
    * @since DB version 24
    */
   private String   urlAddress;

   /**
    * @since Db version 25
    */
   private float    altitude        = TourDatabase.DEFAULT_FLOAT;

   /**
    * @since Db version 25
    */
   private double   latitude        = TourDatabase.DEFAULT_DOUBLE;

   /**
    * @since Db version 25
    */
   private double   longitude       = TourDatabase.DEFAULT_DOUBLE;

   private int      isMarkerVisible = 1;

   // NO ENTITY FIELDS

   /**
    * visibleType is used to show the marker with different visible effects (color)
    */
   @Transient
   private int       _visibleType;

   /**
    * Contains <b>width</b> and <b>height</b> of the marker image.
    * <p>
    * <b>x</b> and <b>y</b> contain the width and height of the marker banner which is the marker
    * label text including the border
    */
   @Transient
   private Rectangle _markerBounds;

   /**
    * unique id for manually created markers because the {@link #markerId} is 0 when the marker is
    * not persisted
    */
   @Transient
   private long      _createId            = 0;

   /**
    * Device time in ms.
    */
   @Transient
   private long      _deviceLapTime       = Long.MIN_VALUE;

   /**
    * Serie index in {@link TourData} when tour is {@link TourData#isMultipleTours}.
    */
   @Transient
   private int       _multiTourSerieIndex = -1;

   public TourMarker() {}

   public TourMarker(final TourData tourData, final int markerType) {

      this.tourData = tourData;
      this.type = markerType;

      _createId = _createCounter.incrementAndGet();
   }

   /**
    * @param pc
    * @return Returns the default max size in pixel for a {@link TourSign} image. This is used when
    *         drawing a table column.<br>
    *         When a sign image is drawn in a chart or map, the sign image size from the pref store
    *         {@link ITourbookPreferences#GRAPH_MARKER_SIGN_IMAGE_SIZE} is used. This size is
    *         converted into pixel with the vertical DLU's.
    */
   public static int getSignImageMaxSize(final PixelConverter pc) {

      if (_defaultSignImageMaxSize == -1) {
         _defaultSignImageMaxSize = pc.convertHeightInCharsToPixels(3);
      }

      return _defaultSignImageMaxSize;
   }

   @Override
   public TourMarker clone() {

      TourMarker newTourMarker = null;

      try {
         newTourMarker = (TourMarker) super.clone();
      } catch (final CloneNotSupportedException e) {
         e.printStackTrace();
      }

      return newTourMarker;
   }

   /**
    * Creates a clone of another marker but set's new {@link TourData} and set's the state that the
    * marker is not yet saved
    *
    * @param newTourData
    * @return
    */
   public TourMarker clone(final TourData newTourData) {

      final TourMarker newTourMarker = clone();

      newTourMarker.tourData = newTourData;

      newTourMarker.markerId = TourDatabase.ENTITY_IS_NOT_SAVED;
      newTourMarker._createId = _createCounter.incrementAndGet();

      return newTourMarker;
   }

   @Override
   public int compareTo(final Object other) {

      /*
       * Set sorting for tour markers by (time) index
       */

      if (other instanceof TourMarker) {

         final TourMarker otherTourMarker = (TourMarker) other;

         return serieIndex - otherTourMarker.getSerieIndex();
      }

      return 0;
   }

   /**
    * Tourmarker is compared with the {@link TourMarker#markerId} or {@link TourMarker#_createId}
    * <p>
    * <b> {@link #serieIndex} is not used for equals or hashcode because this is modified when
    * markers are deleted</b>
    *
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TourMarker)) {
         return false;
      }

      final TourMarker otherMarker = (TourMarker) obj;

      if (markerId == TourDatabase.ENTITY_IS_NOT_SAVED) {

         // marker was create or imported

         if (_createId == otherMarker._createId) {
            return true;
         }

      } else {

         // marker is from the database

         if (markerId == otherMarker.markerId) {
            return true;
         }
      }

      return false;
   }

   public float getAltitude() {
      return altitude;
   }

   /**
    * @return Returns description of the marker when available, otherwise an empty string.
    */
   public String getDescription() {

      return description == null ? UI.EMPTY_STRING : description;
   }

   public long getDeviceLapTime() {

      if (_deviceLapTime == Long.MIN_VALUE) {

         // absolute marker lap time is not set -> get alternative from relative time

         return getMarkerTime();
      }

      return _deviceLapTime;
   }

   /**
    * @return Returns distance in meters in the metric system or -1 when the distance is not
    *         available
    */
   public float getDistance() {
      return distance20;
   }

   /**
    * @return Returns the marker label or an empty string when the label is not set.
    */
   public String getLabel() {
      return label;
   }

   /**
    * @return Returns marker label visual position like
    *         {@link TourMarker#LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED}.
    */
   public int getLabelPosition() {
      return visualPosition;
   }

   public int getLabelXOffset() {
      return labelXOffset;
   }

   public int getLabelYOffset() {
      return labelYOffset;
   }

   /**
    * @return Returns latitude or {@link TourDatabase#DEFAULT_DOUBLE} when not available.
    */
   public double getLatitude() {
      return latitude;
   }

   /**
    * @return Returns longitude or {@link TourDatabase#DEFAULT_DOUBLE} when not available.
    */
   public double getLongitude() {
      return longitude;
   }

   /**
    * @return Contains <b>width</b> and <b>height</b> of the marker image.
    *         <p>
    *         <b>x</b> and <b>y</b> contains the width and height of the marker banner which is the
    *         marker label text including the border
    */
   public Rectangle getMarkerBounds() {
      return _markerBounds;
   }

   /**
    * @return Returns {@link TourMarker} entity id.
    */
   public long getMarkerId() {
      return markerId;
   }

   /**
    * @return Return the absolute time of the marker in ms since 1970-01-01T00:00:00Z.
    */
   public long getMarkerTime() {

      if (tourTime != Long.MIN_VALUE) {
         return tourTime;
      }

      if (time == -1) {
         return 0;
      }

      final long markerTime = tourData.getTourStartTimeMS() + time * 1000;

      return markerTime;
   }

   public int getMultiTourSerieIndex() {
      return _multiTourSerieIndex;
   }

   /**
    * @return Returns position of this marker in the data serie.
    */
   public int getSerieIndex() {
      return serieIndex;
   }

   /**
    * @return Returns time in seconds relative to the tour start or -1 when value is not set.
    */
   public int getTime() {
      return time;
   }

   public TourData getTourData() {
      return tourData;
   }

   /**
    * @return Returns the absolute time in ms since 1970-01-01T00:00:00Z or {@link Long#MIN_VALUE}
    *         when value is not set.
    */
   public long getTourTime() {
      return tourTime;
   }

   public int getType() {
      return type;
   }

   public String getUrlAddress() {
      return urlAddress == null ? UI.EMPTY_STRING : urlAddress;
   }

   public String getUrlText() {
      return urlText == null ? UI.EMPTY_STRING : urlText;
   }

   public int getVisibleType() {
      return _visibleType;
   }

   public boolean hasAltitude() {
      return altitude != TourDatabase.DEFAULT_FLOAT;
   }

   /**
    * !!!!!!!!!!!!!!!!!<br>
    * serieIndex is not used for equals or hashcode because this is modified when markers are
    * deleted<br>
    * !!!!!!!!!!!!!!!!!<br>
    *
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      if (markerId == TourDatabase.ENTITY_IS_NOT_SAVED) {
         result = prime * result + (int) (_createId ^ (_createId >>> 32));
      } else {
         result = prime * result + (int) (markerId ^ (markerId >>> 32));
      }

      return result;
   }

   /**
    * @return Returns <code>true</code> when the marker is created with the device.
    */
   public boolean isDeviceMarker() {

      return type == ChartLabelMarker.MARKER_TYPE_DEVICE;
   }

   /**
    * Compares two markers.
    *
    * @param comparedMarker
    * @param isIgnoreType
    *           When <code>true</code> the type field is not compared.
    * @return Returns <code>true</code> when the content of the markers are equal.
    */
   public boolean isEqual(final TourMarker comparedMarker, final boolean isIgnoreType) {

      if (label.compareTo(comparedMarker.label) != 0) {
         return false;
      } else if (distance20 != comparedMarker.distance20) {
         return false;
      } else if (labelXOffset != comparedMarker.labelXOffset) {
         return false;
      } else if (labelYOffset != comparedMarker.labelYOffset) {
         return false;
      } else if (markerId != comparedMarker.markerId) {
         return false;
      } else if (markerType != comparedMarker.markerType) {
         return false;
      } else if (serieIndex != comparedMarker.serieIndex) {
         return false;
      } else if (time != comparedMarker.time) {
         return false;
      } else if (tourTime != comparedMarker.tourTime) {
         return false;
      } else if ((isIgnoreType == false) && (type != comparedMarker.type)) {
         return false;
      } else if (visualPosition != comparedMarker.visualPosition) {
         return false;
      } else if (tourData != comparedMarker.tourData) {
         return false;
      } else if (getDescription().compareTo(comparedMarker.getDescription()) != 0) {
         return false;
//      } else if ((tourSign != null && comparedMarker.tourSign == null)
//            || (tourSign == null && comparedMarker.tourSign != null)
//            || (tourSign != null && comparedMarker.tourSign != null && tourSign.equals(comparedMarker.tourSign) == false)) {
//         return false;
      }

      return true;
   }

   public boolean isMarkerVisible() {
      return isMarkerVisible == 1;
   }

   /**
    * Checks if VARCHAR fields have the correct length
    *
    * @return Returns <code>true</code> when the data are valid and can be saved
    */
   public boolean isValidForSave() {

      /*
       * Check: label
       */
      FIELD_VALIDATION fieldValidation = TourDatabase.isFieldValidForSave(
            label,
            TourWayPoint.DB_LENGTH_NAME,
            Messages.Db_Field_TourData_Title);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         label = label.substring(0, TourWayPoint.DB_LENGTH_NAME);
      }

      /*
       * Check: description
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            description,
            TourWayPoint.DB_LENGTH_DESCRIPTION,
            Messages.Db_Field_TourData_Description);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         description = description.substring(0, TourWayPoint.DB_LENGTH_DESCRIPTION);
      }

      /*
       * Check: url text
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            urlText,
            DB_LENGTH_URL_TEXT,
            Messages.Db_Field_TourMarker_UrlText);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         urlText = urlText.substring(0, DB_LENGTH_URL_TEXT);
      }

      /*
       * Check: url address
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            urlAddress,
            DB_LENGTH_URL_ADDRESS,
            Messages.Db_Field_TourMarker_UrlAddress);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         urlAddress = urlAddress.substring(0, DB_LENGTH_URL_ADDRESS);
      }

      return true;
   }

   /**
    * restore marker data from a marker backup
    *
    * @param backupMarker
    */
   public void restoreMarkerFromBackup(final TourMarker backupMarker) {

      altitude = backupMarker.altitude;
      description = backupMarker.description;
      distance20 = backupMarker.distance20;
      label = backupMarker.label;
      latitude = backupMarker.latitude;
      longitude = backupMarker.longitude;
      labelXOffset = backupMarker.labelXOffset;
      labelYOffset = backupMarker.labelYOffset;
      markerId = backupMarker.markerId;
      markerType = backupMarker.markerType;
      serieIndex = backupMarker.serieIndex;
      time = backupMarker.time;
      tourTime = backupMarker.tourTime;
      type = backupMarker.type;
      urlAddress = backupMarker.urlAddress;
      urlText = backupMarker.urlText;
      visualPosition = backupMarker.visualPosition;

      tourData = backupMarker.tourData;
//      tourSign = backupMarker.tourSign;
   }

   public void setAltitude(final float altitude) {
      this.altitude = altitude;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public void setDeviceLapTime(final long lapTime) {
      _deviceLapTime = lapTime;
   }

   /**
    * Sets the distance with the metric system
    *
    * @param distance
    *           Distance in meter or <code>-1</code> when the distance is not available
    */
   public void setDistance(final float distance) {
      this.distance20 = distance;
   }

   public void setGeoPosition(final double latitude, final double longitude) {

      this.latitude = latitude;
      this.longitude = longitude;
   }

   public void setLabel(final String label) {
      this.label = label;
   }

   /**
    * Set marker label visual position.
    *
    * @param visualPosition
    *           The marker label visual position, default is
    *           {@link #LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED}.
    */
   public void setLabelPosition(final int visualPosition) {
      this.visualPosition = visualPosition;
   }

   public void setLabelXOffset(final int labelXOffset) {
      this.labelXOffset = labelXOffset;
   }

   public void setLabelYOffset(final int labelYOffset) {
      this.labelYOffset = labelYOffset;
   }

   public void setLatitude(final double latitude) {
      this.latitude = latitude;
   }

   public void setLongitude(final double longitude) {
      this.longitude = longitude;
   }

   /**
    * copies the current marker into a backup marker
    *
    * @param backupMarker
    */
   public void setMarkerBackup(final TourMarker backupMarker) {

      backupMarker.altitude = altitude;
      backupMarker.description = description;
      backupMarker.distance20 = distance20;
      backupMarker.label = label;
      backupMarker.labelXOffset = labelXOffset;
      backupMarker.labelYOffset = labelYOffset;
      backupMarker.latitude = latitude;
      backupMarker.longitude = longitude;
      backupMarker.markerId = markerId;
      backupMarker.markerType = markerType;
      backupMarker.serieIndex = serieIndex;
      backupMarker.time = time;
      backupMarker.tourTime = tourTime;
      backupMarker.type = type;
      backupMarker.urlAddress = urlAddress;
      backupMarker.urlText = urlText;
      backupMarker.visualPosition = visualPosition;

      backupMarker.tourData = tourData;
//      backupMarker.tourSign = tourSign;
   }

   /**
    * @param markerBounds
    */
   public void setMarkerBounds(final Rectangle markerBounds) {
      _markerBounds = markerBounds;
   }

   public void setMarkerVisible(final boolean isMarkerVisible) {
      this.isMarkerVisible = isMarkerVisible ? 1 : 0;
   }

   public void setMultiTourSerieIndex(final int multiTourSerieIndex) {
      _multiTourSerieIndex = multiTourSerieIndex;
   }

   /**
    * Set position of this marker in the data serie.
    *
    * @param serieIndex
    */
   public void setSerieIndex(final int serieIndex) {
      this.serieIndex = serieIndex;
   }

   /**
    * @param relativeTourTime
    *           Time in seconds relative to the tour start. When value is not available it is set to
    *           -1.
    * @param absoluteTourTime
    *           Absolute time of the tour marker in milliseconds since 1970-01-01T00:00:00Z. When
    *           value is not available, it is set to {@link Long#MIN_VALUE}.
    */
   public void setTime(final int relativeTourTime, final long absoluteTourTime) {

      this.time = relativeTourTime;
      this.tourTime = absoluteTourTime;
   }

   public void setTourData(final TourData newTourData) {
      this.tourData = newTourData;
   }

   public void setType(final int markerType) {
      this.type = markerType;
   }

   public void setUrlAddress(final String urlAddress) {
      this.urlAddress = urlAddress;
   }

   public void setUrlText(final String urlText) {
      this.urlText = urlText;
   }

   public void setVisibleType(final int visibleType) {
      _visibleType = visibleType;
   }

   /**
    * This method is called in the MT UI in the "Tour Data" view
    */
   @Override
   public String toString() {

      return "TourMarker [" //$NON-NLS-1$
//            + ("markerId=" + markerId + ", ")
//            + ("tourData=" + tourData + ", ")
//            + ("type=" + type + ", ")
            + ("time=" + time + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //            + ("distance=" + distance + ", ")
            //            + ("distance20=" + distance20 + ", ")
            //            + ("visualPosition=" + visualPosition + ", ")
            //            + ("labelXOffset=" + labelXOffset + ", ")
            //            + ("labelYOffset=" + labelYOffset + ", ")
            //            + ("markerType=" + markerType + ", ")
            + ("serieIndex=" + serieIndex + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("label=" + label + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            //            + ("category=" + category + ", ")
            //            + ("isMarkerVisible=" + isMarkerVisible + ", ")
            //            + ("_visibleType=" + _visibleType + ", ")
            //            + ("_markerBounds=" + _markerBounds + ", ")
            //            + ("_createId=" + _createId)
            + "]"; //$NON-NLS-1$
   }

   @Override
   public String toXml() {
      try {
         final JAXBContext context = JAXBContext.newInstance(this.getClass());
         final Marshaller marshaller = context.createMarshaller();
         final StringWriter sw = new StringWriter();
         marshaller.marshal(this, sw);
         return sw.toString();
      } catch (final JAXBException e) {
         e.printStackTrace();
      }

      return null;
   }

   /**
    * Convert fields from old to new data type.
    */
   public void updateDatabase_019_to_020() {

      if (distance > 0) {

         distance20 = distance;
         distance = 0;
      }
   }

}
