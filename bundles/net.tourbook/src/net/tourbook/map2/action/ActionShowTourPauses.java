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
package net.tourbook.map2.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;

public class ActionShowTourPauses extends Action {

   private Map2View _mapView;

   public ActionShowTourPauses(final Map2View mapView) {

      super(null, AS_CHECK_BOX);

      _mapView = mapView;

      setText(Messages.map_action_show_tour_pauses);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__TourPauses));
      setDisabledImageDescriptor(TourbookPlugin
            .getImageDescriptor(net.tourbook.Messages.Image__TourPauses_disabled));
   }

   @Override
   public void run() {
      _mapView.actionSetShowTourPausesInMap();
   }
}
