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
package de.byteholder.geoclipse.mapprovider;

import de.byteholder.geoclipse.Messages;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ThemeUtil;

import org.eclipse.jface.action.Action;

public class ActionZoomIn extends Action {

   private IMapDefaultActions _mapActionProvider;

   public ActionZoomIn(final IMapDefaultActions mapActionProvider) {

      super(null, AS_PUSH_BUTTON);

      _mapActionProvider = mapActionProvider;

      setToolTipText(Messages.Map_Action_ZoomIn_Tooltip);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(ThemeUtil.getThemedImageName(Images.Zoom_In)));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(ThemeUtil.getThemedImageName(Images.Zoom_In_Disabled)));
   }

   @Override
   public void run() {
      _mapActionProvider.actionZoomIn();
   }

}
