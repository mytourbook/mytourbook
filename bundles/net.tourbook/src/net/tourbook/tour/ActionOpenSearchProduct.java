/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
import net.tourbook.ui.views.nutrition.DialogSearchProduct;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionOpenSearchProduct extends Action {

   public ActionOpenSearchProduct() {

      setText(Messages.app_action_edit_adjust_altitude);

      //todo fb display a plus symbol, easy to draw in inkscape
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Add));
      // for when a tour is not selected
      //   setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Add_Disabled));
   }

   @Override
   public void run() {
      new DialogSearchProduct(Display.getCurrent().getActiveShell()).open();
   }
}
