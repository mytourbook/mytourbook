/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import de.byteholder.geoclipse.map.CenterMapBy;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;

public class ActionZoomCenterBy extends Action {

   private Map2View _mapView;

   public ActionZoomCenterBy(final Map2View mapView) {

      super(null, AS_PUSH_BUTTON);

      _mapView = mapView;

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Tour));
      setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Tour_Disabled));
   }

   @Override
   public void runWithEvent(final Event event) {

      _mapView.actionSetZoomCentered(event);
   }

   /**
    * Update image and tooltip to the center mode
    *
    * @param centerMapBy
    */
   public void setCenterMode(final CenterMapBy centerMapBy) {

      switch (centerMapBy) {

      case Map:

         setToolTipText(Messages.Map_Action_Zoom_CenteredBy_Map_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Map));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Map_Disabled));

         break;

      case Tour:

         setToolTipText(Messages.Map_Action_Zoom_CenteredBy_Tour_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Tour));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Tour_Disabled));

         break;

      case Mouse:
      default:

         setToolTipText(Messages.Map_Action_Zoom_CenteredBy_Mouse_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Mouse));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Zoom_CenterBy_Mouse_Disabled));

         break;
      }
   }

}
