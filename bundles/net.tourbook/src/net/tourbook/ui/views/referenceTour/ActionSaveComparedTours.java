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
package net.tourbook.ui.views.referenceTour;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;

import org.eclipse.jface.action.Action;

class ActionSaveComparedTours extends Action {

   private final ElevationCompareResultView _compareResultView;

   ActionSaveComparedTours(final ElevationCompareResultView compareResultView) {

      _compareResultView = compareResultView;

      setText(Messages.Compare_Result_Action_save_checked_tours);
      setToolTipText(Messages.Compare_Result_Action_save_checked_tours_tooltip);

      setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Save));
      setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Save_Disabled));
   }

   @Override
   public void run() {
      _compareResultView.saveCompareResults();
   }
}
