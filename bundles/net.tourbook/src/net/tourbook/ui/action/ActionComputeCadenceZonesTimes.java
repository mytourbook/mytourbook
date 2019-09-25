/*******************************************************************************
 * Copyright (C) 2005, 2019  Wolfgang Schramm and Contributors
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
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionComputeCadenceZonesTimes extends Action {

   private final ITourProvider _tourProvider;

   public ActionComputeCadenceZonesTimes(final ITourProvider tourDataEditor) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourDataEditor;

      setText(Messages.TourEditor_Action_ComputeCadenceZonesTimes);
   }

   @Override
   public void run() {

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.TourEditor_Dialog_ComputeCadenceZonesTimes_Title,
            NLS.bind(Messages.TourEditor_Dialog_ComputeCadenceZonesTimes_Message, selectedTours.size())) == false) {
         return;
      }

      final long start = System.currentTimeMillis();

      TourLogManager.showLogView();

      if (TourManager.computeCadenceZonesTimes(selectedTours)) {

         // save all modified tours
         TourManager.saveModifiedTours(selectedTours);
      }

      TourLogManager.logDefault(String.format(
            Messages.Log_ComputeCadenceZonesTimes_002_End,
            (System.currentTimeMillis() - start) / 1000.0));

   }
}
