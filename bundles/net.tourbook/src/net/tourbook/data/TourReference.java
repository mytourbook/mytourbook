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
package net.tourbook.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

/**
 * Represents a reference tour which is between the {@link #startIndex} and {@link #endIndex} in the
 * {@link TourData} of a tour
 */
@Entity
public class TourReference implements Serializable {

   private static final long          serialVersionUID = 1L;

   private static final String        NL               = UI.NEW_LINE;

   public static final int            DB_LENGTH_LABEL  = 80;

   private static final AtomicInteger _createIDCounter = new AtomicInteger();

   /**
    * Entity ID of the reference tour
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       refId            = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * {@link TourData} which is referenced
    */
   @ManyToOne(optional = false)
   private TourData                   tourData;

   /**
    * Value index position for the reference tour in the original tour
    */
   private int                        startIndex;

   private int                        endIndex;

   private String                     label            = UI.EMPTY_STRING;

   private boolean                    isTourFilter_ElevationDiff;
   private boolean                    isTourFilter_GeoDiff;
   private boolean                    isTourFilter_MaxResults;

   /**
    * Tour filter for the elevation differences in the compare result
    */
   private float                      tourFilter_ElevationDiff;

   /**
    * Tour filter for the geo differences in the compare result
    */
   private float                      tourFilter_GeoDiff;

   /**
    * Tour filter for the max results in the compare result
    */
   private int                        tourFilter_MaxResults;

   /**
    * Unique id for created entities because the {@link #refId} is -1 when it's not persisted
    */
   @Transient
   private long                       _createId        = 0;

   /**
    * When <code>true</code> then this reference tour is never saved
    */
   @Transient
   private boolean                    _isVirtualRefTour;

   public TourReference() {}

   /**
    * @param label
    * @param tourData
    *           Contains the tour which is referenced
    * @param startIndex
    * @param endIndex
    */
   public TourReference(final String label, final TourData tourData, final int startIndex, final int endIndex) {

      this.label = label;
      this.tourData = tourData;

      this.startIndex = startIndex;
      this.endIndex = endIndex;

      _createId = _createIDCounter.incrementAndGet();
   }

   /**
    * Used for MT import/export
    *
    * @param tourData
    */
   public TourReference(final TourData tourData) {

      this.tourData = tourData;

      _createId = _createIDCounter.incrementAndGet();
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

      final TourReference other = (TourReference) obj;

      if (_createId == 0) {

         // entity is from the database

         if (refId != other.refId) {
            return false;
         }

      } else {

         // entity was created

         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
   }

   public int getEndIndex() {
      return endIndex;
   }

   public int getEndValueIndex() {
      return endIndex;
   }

   /**
    * @return Return the name for the reference tour
    */
   public String getLabel() {
      return label;
   }

   /**
    * @return Returns the primary key for a {@link TourReference} entity
    */
   public long getRefId() {
      return refId;
   }

   public int getStartIndex() {
      return startIndex;
   }

   public int getStartValueIndex() {
      return startIndex;
   }

   public TourData getTourData() {

      /*
       * Ensure to have the correct tour data, load tour data because tour data in the ref tour
       * could be changed, this is a wrong concept which could be changed but requires additonal
       * work
       */
      return TourManager.getInstance().getTourData(tourData.getTourId());
   }

   public float getTourFilter_ElevationDiff() {
      return tourFilter_ElevationDiff;
   }

   public float getTourFilter_GeoDiff() {
      return tourFilter_GeoDiff;
   }

   public int getTourFilter_MaxResults() {
      return tourFilter_MaxResults;
   }

   @Override
   public int hashCode() {
      return Objects.hash(_createId, refId);
   }

   public boolean isTourFilter_ElevationDiff() {
      return isTourFilter_ElevationDiff;
   }

   public boolean isTourFilter_GeoDiff() {
      return isTourFilter_GeoDiff;
   }

   public boolean isTourFilter_MaxResults() {
      return isTourFilter_MaxResults;
   }

   public boolean isVirtualRefTour() {
      return _isVirtualRefTour;
   }

   public void setEndValueIndex(final int endIndex) {
      this.endIndex = endIndex;
   }

   public void setIsVirtualRefTour() {
      _isVirtualRefTour = true;
   }

   public void setLabel(final String label) {
      this.label = label;
   }

   public void setStartValueIndex(final int startIndex) {
      this.startIndex = startIndex;
   }

   public void setTourFilter_ElevationDiff(final float tourFilter_ElevationDiff) {
      this.tourFilter_ElevationDiff = tourFilter_ElevationDiff;
   }

   public void setTourFilter_GeoDiff(final float tourFilter_GeoDiff) {
      this.tourFilter_GeoDiff = tourFilter_GeoDiff;
   }

   public void setTourFilter_IsElevationDiff(final boolean isTourFilter_ElevationDiff) {
      this.isTourFilter_ElevationDiff = isTourFilter_ElevationDiff;
   }

   public void setTourFilter_IsGeoDiff(final boolean isTourFilter_GeoDiff) {
      this.isTourFilter_GeoDiff = isTourFilter_GeoDiff;
   }

   public void setTourFilter_IsMaxResults(final boolean isTourFilter_MaxResults) {
      this.isTourFilter_MaxResults = isTourFilter_MaxResults;
   }

   public void setTourFilter_MaxResults(final int tourFilter_MaxResults) {
      this.tourFilter_MaxResults = tourFilter_MaxResults;
   }

   /**
    * This method is called in the MT UI in the "Tour Data" view
    */
   @Override
   public String toString() {

      return "TourReference" + NL //                     //$NON-NLS-1$

            + "[" + NL //                                //$NON-NLS-1$

            + "   label       = " + label + NL //        //$NON-NLS-1$
            + "   refId       = " + refId + NL //        //$NON-NLS-1$
            + "   startIndex  = " + startIndex + NL //   //$NON-NLS-1$
            + "   endIndex    = " + endIndex + NL //     //$NON-NLS-1$

//          + "   tourData    = " + tourData + NL //     //$NON-NLS-1$

            + "]" + NL //                                //$NON-NLS-1$
      ;
   }
}
