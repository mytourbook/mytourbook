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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.ITourMarkerUpdater;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;

public class ActionDeleteMarker extends Action {

	private ITourMarkerUpdater	_tourMarkerUpdater;
	private TourMarker			_tourMarker;

	public ActionDeleteMarker(final ITourMarkerUpdater tourMarkerUpdater) {

		_tourMarkerUpdater = tourMarkerUpdater;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete));
	}

	@Override
	public void run() {

		_tourMarkerUpdater.deleteTourMarker(_tourMarker);
	}

	/**
	 * Set {@link TourMarker} which is modified.
	 * 
	 * @param tourMarker
	 * @param menuMgr
	 */
	public void setTourMarker(final TourMarker tourMarker) {

		_tourMarker = tourMarker;

		String label = tourMarker.getLabel();

		label = UI.shortenText(label, 30, true);

		setText(NLS.bind(Messages.Tour_Action_Marker_Delete, label));
	}

}
