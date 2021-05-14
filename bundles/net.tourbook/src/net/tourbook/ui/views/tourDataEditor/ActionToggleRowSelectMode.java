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
package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

class ActionToggleRowSelectMode extends Action {

   private final TourDataEditorView _tourPropertiesView;

   public ActionToggleRowSelectMode(final TourDataEditorView tourPropertiesView) {

      super(null, AS_CHECK_BOX);

      _tourPropertiesView = tourPropertiesView;

      setToolTipText(Messages.app_action_edit_rows_tooltip);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.EditRow));
      setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.EditRow_Disabled));

      setEnabled(false);
   }

   @Override
   public void run() {
      _tourPropertiesView.actionToggleRowSelectMode();
   }
}
