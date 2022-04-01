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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

/**
 * Represents a reference tour which is between the {@link #startIndex} and {@link #endIndex} in the
 * {@link TourData} of a tour
 */
@Entity
public class TourReference {

   private static final String NL              = UI.NEW_LINE;

   public static final int     DB_LENGTH_LABEL = 80;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                refId;

   @ManyToOne(optional = false)
   private TourData            tourData;

   /**
    * value index position for the reference tour in the original tour
    */
   private int                 startIndex;

   private int                 endIndex;

   private String              label           = UI.EMPTY_STRING;

   public TourReference() {}

   public TourReference(final String label, final TourData tourData, final int startIndex, final int endIndex) {

      this.tourData = tourData;
      this.label = label;

      this.startIndex = startIndex;
      this.endIndex = endIndex;
   }

   /**
    * Used for MT import/export
    *
    * @param tourData
    */
   public TourReference(final TourData tourData) {

      this.tourData = tourData;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TourReference)) {
         return false;
      }
      final TourReference other = (TourReference) obj;
      if (refId != other.refId) {
         return false;
      }
      return true;
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

   public int getStartValueIndex() {
      return startIndex;
   }

   public TourData getTourData() {

      /*
       * ensure to have the correct tour data, load tour data because tour data in the ref tour
       * could be changed, this is a wrong concept which could be changed but requires additonal
       * work
       */
      return TourManager.getInstance().getTourData(tourData.getTourId());
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (refId ^ (refId >>> 32));
      return result;
   }

   public void setEndValueIndex(final int endIndex) {
      this.endIndex = endIndex;
   }

   public void setGeneratedId(final long generatedId) {
      this.refId = generatedId;
   }

   public void setLabel(final String label) {
      this.label = label;
   }

   public void setStartValueIndex(final int startIndex) {
      this.startIndex = startIndex;
   }

   public void setTourData(final TourData tourData) {
      this.tourData = tourData;
   }

   /**
    * This method is called in the MT UI in the "Tour Data" view
    */
   @Override
   public String toString() {

      return "TourReference" + NL //                     //$NON-NLS-1$

            + "   label       = " + label + NL //        //$NON-NLS-1$
            + "   refId       = " + refId + NL //        //$NON-NLS-1$
            + "   startIndex  = " + startIndex + NL //   //$NON-NLS-1$
            + "   endIndex    = " + endIndex + NL //     //$NON-NLS-1$

//          + "   tourData    = " + tourData + NL //     //$NON-NLS-1$

      ;
   }
}
