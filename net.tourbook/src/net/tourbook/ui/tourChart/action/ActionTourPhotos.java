/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;

public class ActionTourPhotos extends Action {

	private TourChart	_tourChart;

	public ActionTourPhotos(final TourChart tourChart) {

		super(Messages.Tour_Action_TourPhotos, AS_CHECK_BOX);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__PhotoPhotos));

		_tourChart = tourChart;
	}

	@Override
	public void run() {
		_tourChart.actionShowTourPhotos(isChecked());
	}
}
