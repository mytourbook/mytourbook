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
package net.tourbook.ui.tourChart.action;

import net.tourbook.Messages;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;

public class ActionSetMarkerVisible extends Action {

	private TourChart	_tourChart;
	private boolean		_isMarkerVisible;
	private TourMarker	_tourMarker;

	public ActionSetMarkerVisible(final TourChart tourChart) {

		_tourChart = tourChart;
	}

	@Override
	public void run() {

		_tourChart.actionSetMarkerVisible(_tourMarker, _isMarkerVisible);
	}

	/**
	 * Set {@link TourMarker} which is modified.
	 * 
	 * @param tourMarker
	 * @param isMarkerVisible
	 */
	public void setTourMarker(final TourMarker tourMarker, final boolean isMarkerVisible) {

		_tourMarker = tourMarker;
		_isMarkerVisible = isMarkerVisible;

		if (isMarkerVisible) {
			setText(Messages.Tour_Action_Marker_SetVisible);
		} else {
			setText(Messages.Tour_Action_Marker_SetHidden);
		}
	}

}
