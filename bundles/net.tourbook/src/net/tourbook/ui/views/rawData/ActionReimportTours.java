/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.ui.views.rawData;

import net.tourbook.Messages;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionReimportTours extends Action {

   private final ITourViewer3 _tourViewer;

   public ActionReimportTours(final ITourViewer3 tourViewer) {

      _tourViewer = tourViewer;

      setText(Messages.Dialog_ReimportTours_Action_OpenDialog);
   }

   @Override
   public void run() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      new DialogReimportTours(Display.getCurrent().getActiveShell(), _tourViewer).open();
   }
}
