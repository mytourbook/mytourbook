/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.action.Action;

class ActionDeleteTimeSlices_AdjustTourStartTime_KeepOtherValues extends Action {

   private final TourDataEditorView _tourDataEditorView;

   public ActionDeleteTimeSlices_AdjustTourStartTime_KeepOtherValues(final TourDataEditorView tourDataEditorView) {

      super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

      setText(Messages.Tour_Editor_Action_DeleteTimeSlices_AdjustTourStartTime_KeepOtherValues);
      setToolTipText(Messages.Tour_Editor_Action_DeleteTimeSlices_AdjustTourStartTime_KeepOtherValues_Tooltip);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));

      _tourDataEditorView = tourDataEditorView;
   }

   @Override
   public void run() {

      _tourDataEditorView.actionDelete_TimeSlices(false, false, true);
   }
}
