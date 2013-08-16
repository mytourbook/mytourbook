/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import net.tourbook.common.color.MapColorId;
import net.tourbook.map2.Messages;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;

public class ActionTourColor extends Action {

	private Map3View	_mapView;
	private MapColorId	_colorId;

	public ActionTourColor(	final Map3View mapView,
							final MapColorId colorId,
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

	/**
	 * Create tour color action, this is done here to separate map2 Messages from map3 Messages.
	 * 
	 * @param map3View
	 * @param colorId
	 * @return
	 */
	public static ActionTourColor createAction(final Map3View map3View, final MapColorId colorId) {

		switch (colorId) {
		case Altitude:

			return new ActionTourColor(
					map3View,
					MapColorId.Altitude,
					Messages.map_action_tour_color_altitude_tooltip,
					Messages.image_action_tour_color_altitude,
					Messages.image_action_tour_color_altitude_disabled);

		case Gradient:

			return new ActionTourColor(
					map3View,
					MapColorId.Gradient,
					Messages.map_action_tour_color_gradient_tooltip,
					Messages.image_action_tour_color_gradient,
					Messages.image_action_tour_color_gradient_disabled);

		case Pace:

			return new ActionTourColor(
					map3View,
					MapColorId.Pace,
					Messages.map_action_tour_color_pase_tooltip,
					Messages.image_action_tour_color_pace,
					Messages.image_action_tour_color_pace_disabled);

		case Pulse:

			return new ActionTourColor(
					map3View,
					MapColorId.Pulse,
					Messages.map_action_tour_color_pulse_tooltip,
					Messages.image_action_tour_color_pulse,
					Messages.image_action_tour_color_pulse_disabled);

		case Speed:

			return new ActionTourColor(
					map3View,
					MapColorId.Speed,
					Messages.map_action_tour_color_speed_tooltip,
					Messages.image_action_tour_color_speed,
					Messages.image_action_tour_color_speed_disabled);

		case HrZone:

			return new ActionTourColor(
					map3View,
					MapColorId.HrZone,
					Messages.Tour_Action_ShowHrZones_Tooltip,
					Messages.Image__PulseZones,
					Messages.Image__PulseZones_Disabled);

		default:
			break;
		}

		return null;
	}

	@Override
	public void run() {

		// !!! this method is also called when the button is unchecked !!!
		if (isChecked()) {
			_mapView.actionSetTourColor(_colorId);
		}
	}

}
