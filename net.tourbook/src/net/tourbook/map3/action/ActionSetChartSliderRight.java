/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;

public class ActionSetChartSliderRight extends Action {

	private Map3View	_mapView;

	public ActionSetChartSliderRight(final Map3View mapView) {

		super(Messages.Map3_Action_SetChartSliderRight, AS_PUSH_BUTTON);

		_mapView = mapView;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(//
				net.tourbook.map2.Messages.Image_Action_ShowSliderInMap));
	}

	@Override
	public void run() {
		_mapView.actionSetTrackSlider(false);
	}

}
