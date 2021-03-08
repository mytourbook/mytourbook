/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.PersonManager;
import net.tourbook.tour.DialogExtractTour;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionExtractTour extends Action {

   private TourDataEditorView _tourDataEditor;

   private int                _tourStartIndex;
   private int                _tourEndIndex;

   /**
    * @param tourDataEditor
    * @param tourProvider
    */
   public ActionExtractTour(final TourDataEditorView tourDataEditor) {

      _tourDataEditor = tourDataEditor;

      setText(Messages.App_Action_ExtractTour);
   }

   @Override
   public void run() {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      // get tour
      final ArrayList<TourData> selectedTours = _tourDataEditor.getSelectedTours();
      if (selectedTours == null || selectedTours.isEmpty()) {
         return;
      }

      // check person
      if (PersonManager.isPersonAvailable() == false) {
         return;
      }

      new DialogExtractTour(
            Display.getCurrent().getActiveShell(),
            selectedTours.get(0),
            _tourStartIndex,
            _tourEndIndex,
            _tourDataEditor).open();
   }

   public void setTourRange(final int tourStartIndex, final int tourEndIndex) {
      _tourStartIndex = tourStartIndex;
      _tourEndIndex = tourEndIndex;
   }
}
