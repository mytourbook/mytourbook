/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.tourMarker;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class ActionClearRecentMarkers extends Action {

   public ActionClearRecentMarkers() {

      super(Messages.Action_TourMarker_ClearRecentMarkers, AS_PUSH_BUTTON);

      setToolTipText(Messages.Action_TourMarker_ClearRecentMarkers_Tooltip);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Delete));
   }

   @Override
   public void run() {

      final MessageDialog dialog = new MessageDialog(

            Display.getDefault().getActiveShell(),

            Messages.Action_TourMarker_Dialog_ClearRecentMarkers_Title,
            null, // no title image

            Messages.Action_TourMarker_Dialog_ClearRecentMarkers_Message.formatted(TourMarkerManager.getRecentMarkers().size()),

            MessageDialog.CONFIRM,

            0, // default index

            Messages.App_Action_RemoveAll,
            Messages.App_Action_Cancel);

      if (dialog.open() == IDialogConstants.OK_ID) {

         TourMarkerManager.clearRecentMarkers();
      }
   }
}