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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionTourFilterWithoutGPS extends Action {

	private TourMarkerAllView	_tourMarkerAllView;

	public ActionTourFilterWithoutGPS(final TourMarkerAllView tourMarkerAllView) {

		super(null, AS_CHECK_BOX);

		setToolTipText(Messages.Action_TourFilter_WithoutGPS_Tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter_WithoutGPS));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter_WithoutGPS_Disabled));

		_tourMarkerAllView = tourMarkerAllView;
	}

	@Override
	public void run() {
		_tourMarkerAllView.actionTourFilterGPS(this);
	}
}
