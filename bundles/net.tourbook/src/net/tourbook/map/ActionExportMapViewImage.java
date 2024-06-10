/*******************************************************************************
 * Copyright (C) 2021, 2024 Frédéric Bard
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
package net.tourbook.map;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.map2.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionExportMapViewImage extends Action {

   private IMapView _mapView;

   public ActionExportMapViewImage(final IMapView mapView) {

      super(Messages.Map_Action_Export_Map_View_Image, AS_PUSH_BUTTON);

      setToolTipText(Messages.Map_Action_Export_Map_View_Image_Tooltip);

      setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Save));

      _mapView = mapView;
   }

   @Override
   public void run() {

      new DialogMapExportViewImage(Display.getCurrent().getActiveShell(), _mapView).open();

   }

}
