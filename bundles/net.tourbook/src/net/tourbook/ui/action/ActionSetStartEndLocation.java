/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;

public class ActionSetStartEndLocation extends Action {

   private ITourProvider _tourProvider;

   /**
    * @param tourProvider
    */
   public ActionSetStartEndLocation(final ITourProvider tourProvider) {

      super(Messages.Tour_Action_SetStartEndLocation, AS_PUSH_BUTTON);

      setToolTipText(Messages.Tour_Action_SetStartEndLocation_Tooltip);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd));

      _tourProvider = tourProvider;
   }

   @Override
   public void run() {

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();
      final ArrayList<TourData> modifiedTours = new ArrayList<>();

      TourLocationManager.setLocationNames(selectedTours, modifiedTours);

      if (modifiedTours.size() > 0) {

         TourManager.saveModifiedTours(modifiedTours);
      }
   }
}
