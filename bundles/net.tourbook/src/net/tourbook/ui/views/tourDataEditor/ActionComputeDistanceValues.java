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

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;

class ActionComputeDistanceValues extends Action {

   /**
    *
    */
   private final TourDataEditorView _tourDataEditor;

   public ActionComputeDistanceValues(final TourDataEditorView tourDataEditor) {

      super(null, AS_PUSH_BUTTON);

      _tourDataEditor = tourDataEditor;

      setText(Messages.TourEditor_Action_ComputeDistanceValuesFromGeoPosition);
   }

   @Override
   public void run() {
      _tourDataEditor.actionComputeDistanceValuesFromGeoPosition();
   }
}
