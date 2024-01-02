/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionEditQuick extends Action {

   /**
    * Is <code>true</code> when start location is hovered, <code>false</code> when
    * endlocation is hovered, <code>null</code> when a location is not hovered
    */
   private static Boolean      _tourLocationFocus;

   private final ITourProvider _tourProvider;

   public ActionEditQuick(final ITourProvider tourProvider) {

      setText(Messages.app_action_quick_edit);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit_Disabled));

      _tourProvider = tourProvider;
   }

   public static void doAction(final ITourProvider tourProvider) {

      // check tour, make sure only one tour is selected
      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
      if (selectedTours == null || selectedTours.size() != 1) {
         return;
      }

      final DialogQuickEdit dialogQuickEdit = new DialogQuickEdit(Display.getCurrent().getActiveShell(), selectedTours.get(0));

      dialogQuickEdit.setTourLocationFocus(_tourLocationFocus);

      // reset focus that the next quick edit dialog is ignoring it when it's not set
      _tourLocationFocus = null;

      if (dialogQuickEdit.open() == Window.OK) {

         // save all tours with the new tour type
         TourManager.saveModifiedTours(selectedTours);
      }
   }

   /**
    * @param tourLocationFocus
    *
    *           Is <code>true</code> when start location is hovered, <code>false</code> when
    *           endlocation is hovered, <code>null</code> when a location is not hovered
    */
   public static void setTourLocationFocus(final Boolean tourLocationFocus) {

      _tourLocationFocus = tourLocationFocus;
   }

   @Override
   public void run() {
      doAction(_tourProvider);
   }

}
