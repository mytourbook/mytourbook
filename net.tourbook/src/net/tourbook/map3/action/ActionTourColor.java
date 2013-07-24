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
import net.tourbook.common.color.ILegendProvider;
import net.tourbook.map2.Messages;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;

public class ActionTourColor extends Action {

	private Map3View	_mapView;
	private int			_colorId;

	public ActionTourColor(	final Map3View mapView,
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

	/**
	 * Create tour color action, this is done here to separate map2 Messages from map3 Messages.
	 * 
	 * @param map3View
	 * @param colorId
	 * @return
	 */
	public static ActionTourColor createAction(final Map3View map3View, final int colorId) {

		switch (colorId) {
		case ILegendProvider.TOUR_COLOR_ALTITUDE:

			return new ActionTourColor(
					map3View,
					ILegendProvider.TOUR_COLOR_ALTITUDE,
					Messages.map_action_tour_color_altitude_tooltip,
					Messages.image_action_tour_color_altitude,
					Messages.image_action_tour_color_altitude_disabled);

		case ILegendProvider.TOUR_COLOR_GRADIENT:

			return new ActionTourColor(
					map3View,
					ILegendProvider.TOUR_COLOR_GRADIENT,
					Messages.map_action_tour_color_gradient_tooltip,
					Messages.image_action_tour_color_gradient,
					Messages.image_action_tour_color_gradient_disabled);

		case ILegendProvider.TOUR_COLOR_PULSE:

			return new ActionTourColor(
					map3View,
					ILegendProvider.TOUR_COLOR_PULSE,
					Messages.map_action_tour_color_pulse_tooltip,
					Messages.image_action_tour_color_pulse,
					Messages.image_action_tour_color_pulse_disabled);

		case ILegendProvider.TOUR_COLOR_SPEED:

			return new ActionTourColor(
					map3View,
					ILegendProvider.TOUR_COLOR_SPEED,
					Messages.map_action_tour_color_speed_tooltip,
					Messages.image_action_tour_color_speed,
					Messages.image_action_tour_color_speed_disabled);

		case ILegendProvider.TOUR_COLOR_PACE:

			return new ActionTourColor(
					map3View,
					ILegendProvider.TOUR_COLOR_PACE,
					Messages.map_action_tour_color_pase_tooltip,
					Messages.image_action_tour_color_pace,
					Messages.image_action_tour_color_pace_disabled);

		case ILegendProvider.TOUR_COLOR_HR_ZONE:

			return new ActionTourColor(
					map3View,
					ILegendProvider.TOUR_COLOR_HR_ZONE,
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
