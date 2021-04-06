/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionRemoveComparedTourSaveStatus extends Action {

   private TourCompareResultView fResultView;

   public ActionRemoveComparedTourSaveStatus(final TourCompareResultView resultView) {

      fResultView = resultView;

      setText(Messages.Compare_Result_Action_remove_save_result);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));

      setEnabled(false);
   }

   @Override
   public void run() {
      fResultView.removeComparedTourFromDb();
   }

}
