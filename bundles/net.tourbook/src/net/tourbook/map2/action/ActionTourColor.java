/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import net.tourbook.common.color.MapGraphId;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;

public class ActionTourColor extends Action {

	private Map2View	_map2View;
	private MapGraphId	_colorId;

	public ActionTourColor(	final Map2View mapView,
							final MapGraphId colorId,
							final String toolTipText,
							final String imageEnabled,
							final String imageDisabled) {

		super(null, AS_RADIO_BUTTON);

		_map2View = mapView;
		_colorId = colorId;

		setId(colorId.name());

		setToolTipText(toolTipText);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(imageEnabled));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(imageDisabled));
	}

	@Override
	public void run() {

		// !!! this method is also called when the button is unchecked !!!
		if (isChecked()) {
			_map2View.actionSetTourColor(_colorId);
		}
	}

}
