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
package net.tourbook.tour;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionCreateSplinePoint extends Action {

   private DialogAdjustAltitude fDialogAdjustAltitude;

   int                          fMouseDownDevPositionX;
   int                          fMouseDownDevPositionY;

   /**
    * @param dialogAdjustAltitude
    */
   public ActionCreateSplinePoint(final DialogAdjustAltitude dialogAdjustAltitude) {

      fDialogAdjustAltitude = dialogAdjustAltitude;

      setText(Messages.adjust_altitude_action_create_spline_point);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.SplinePoint));
   }

   @Override
   public void run() {
      fDialogAdjustAltitude.actionCreateSplinePoint(fMouseDownDevPositionX, fMouseDownDevPositionY);
   }

}
