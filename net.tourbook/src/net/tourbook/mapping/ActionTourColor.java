/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionTourColor extends Action {

	private TourMapView	_mapView;
	private int			_colorId;

	public ActionTourColor(	final TourMapView mapView,
							final int colorId,
							final String toolTipText,
							final String imageEnabled,
							final String imageDisabled) {

		super(null, AS_RADIO_BUTTON);

		_mapView = mapView;
		_colorId = colorId;

		setToolTipText(toolTipText);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(imageEnabled));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(imageDisabled));
	}

	@Override
	public void run() {

		// !!! this method is also called when the button is unchecked !!!
		if (isChecked()) {
			_mapView.actionSetTourColor(_colorId);
		}
	}

}
