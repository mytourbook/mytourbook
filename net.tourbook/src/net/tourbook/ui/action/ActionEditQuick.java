/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionEditQuick extends Action {

	private ITourProvider	fTourProvider;

	public ActionEditQuick(final ITourProvider tourProvider) {

		setText(Messages.app_action_quick_edit);
//		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour));
//		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_disabled));

		fTourProvider = tourProvider;
	}

	@Override
	public void run() {

		// get selected tour, make sure only one tour is selected
		final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
		if (selectedTours == null || selectedTours.size() != 1) {
			return;
		}

		final DialogQuickEdit dialog = new DialogQuickEdit(Display.getCurrent().getActiveShell(), selectedTours.get(0));
		if (dialog.open() == Window.OK) {

			// save all tours with the new tour type
			TourManager.saveModifiedTours(selectedTours);
		}
	}

}
