/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.ArrayList;
 
import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;

public class ActionSetAltitudeValuesFromSRTM extends Action {

	private final ITourProvider	_tourProvider;

	public ActionSetAltitudeValuesFromSRTM(final ITourProvider tourDataEditor) {

		super(null, AS_PUSH_BUTTON);

		_tourProvider = tourDataEditor;

		setText(Messages.TourEditor_Action_SetAltitudeValuesFromSRTM);
	}

	@Override
	public void run() {

		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

		if (TourManager.setAltitudeValuesFromSRTM(selectedTours)) {

			// save all modified tours
			TourManager.saveModifiedTours(selectedTours);
		}
	}
}
