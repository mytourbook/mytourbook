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

import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;

public class ActionMapColor extends Action {

	private Map3View		_mapView;
	private IDialogSettings	_state;

	private int				_colorId;

	public ActionMapColor(final Map3View mapView, final IDialogSettings state) {

		super(null, AS_PUSH_BUTTON);

		_mapView = mapView;
		_state = state;

		setText(Messages.Map3_Action_MappingColor);
//		setToolTipText(toolTipText);
//
//		setImageDescriptor(TourbookPlugin.getImageDescriptor(imageEnabled));
//		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(imageDisabled));
	}

	/**
	 * Create tour color action, this is done here to separate map2 Messages from map3 Messages.
	 * 
	 * @param map3View
	 * @param colorId
	 * @return
	 */

	@Override
	public void run() {

//		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();
//
//		final ColorDefinition colorDefinition = colorProvider
//				.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE);
//
//		_tourColorId = Util.getStateInt(_state, STATE_TOUR_COLOR_ID, ILegendProvider.TOUR_COLOR_ALTITUDE);
//
//		switch (_tourColorId) {
//		case ILegendProvider.TOUR_COLOR_ALTITUDE:
//			_actionTourColorAltitude.setChecked(true);
//			break;
//
//		case ILegendProvider.TOUR_COLOR_GRADIENT:
//			_actionTourColorGradient.setChecked(true);
//			break;
//
//		case ILegendProvider.TOUR_COLOR_PULSE:
//			_actionTourColorPulse.setChecked(true);
//			break;
//
//		case ILegendProvider.TOUR_COLOR_SPEED:
//			_actionTourColorSpeed.setChecked(true);
//			break;
//
//		case ILegendProvider.TOUR_COLOR_PACE:
//			_actionTourColorPace.setChecked(true);
//			break;
//
//		case ILegendProvider.TOUR_COLOR_HR_ZONE:
//			_actionTourColorHrZone.setChecked(true);
//			break;
//
//		default:
//			_tourColorId = ILegendProvider.TOUR_COLOR_ALTITUDE;
//			_actionTourColorAltitude.setChecked(true);
//			break;
//		}

	}

	public void setColorId(final int colorId) {
		_colorId = colorId;
	}

}
