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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

/**
 * This class contains different data when a tour event is fired.
 */
public class TourEvent {

   private static final char   NL             = UI.NEW_LINE;

   /**
    * Contains tours which have been modified
    */
   private ArrayList<TourData> _modifiedTours;

   /**
    * When <code>true</code>, tour data have been reverted and {@link TourEvent#_modifiedTours}
    * contains the reverted {@link TourData}
    */
   public boolean              isReverted     = false;

   /**
    * When <code>true</code>, tour data have been modified in the {@link TourDataEditorView},
    * {@link TourEvent#_modifiedTours} contains the modified {@link TourData}
    */
   public boolean              isTourModified = false;

   /**
    * Contains the {@link TourData} which is edited in the {@link TourDataEditorView}
    */
   public TourData             tourDataEditorSavedTour;

   @SuppressWarnings("unused")
   private TourEvent() {}

   public TourEvent(final ArrayList<TourData> modifiedTour) {
      _modifiedTours = modifiedTour;
   }

   public TourEvent(final TourData tourData) {

      _modifiedTours = new ArrayList<>();

      if (tourData != null) {
         _modifiedTours.add(tourData);
      }
   }

   /**
    * @return Returns all tours which have been modified
    */
   public ArrayList<TourData> getModifiedTours() {
      return _modifiedTours;
   }

   @Override
   public String toString() {

      return "TourEvent" + NL //$NON-NLS-1$

            + "[" + NL //$NON-NLS-1$

            + "_modifiedTours          = " + _modifiedTours + NL //$NON-NLS-1$
            + "isReverted              = " + isReverted + NL //$NON-NLS-1$
            + "isTourModified          = " + isTourModified + NL //$NON-NLS-1$
            + "tourDataEditorSavedTour = " + tourDataEditorSavedTour + NL //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
