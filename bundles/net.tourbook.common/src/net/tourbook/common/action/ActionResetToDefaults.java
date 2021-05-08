/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.common.action;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.Messages;

import org.eclipse.jface.action.Action;

public class ActionResetToDefaults extends Action {

   private IActionResetToDefault _restoreAction;

   /**
    * Common action to reset values to it's defaults
    *
    * @param restoreAction
    */
   public ActionResetToDefaults(final IActionResetToDefault restoreAction) {

      super();

      _restoreAction = restoreAction;

      setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
      setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_ResetToDefault));
   }

   @Override
   public void run() {
      _restoreAction.resetToDefaults();
   }

}
