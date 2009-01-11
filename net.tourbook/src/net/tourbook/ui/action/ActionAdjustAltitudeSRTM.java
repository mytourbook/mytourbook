/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.DialogAdjustAltitudeSRTM;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class ActionAdjustAltitudeSRTM extends Action {

	private ITourProvider	fTourProvider;

	public ActionAdjustAltitudeSRTM(final ITourProvider tourProvider) {

		setText(Messages.app_action_adjust_altitude_srtm);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image__adjust_altitude_srtm));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image__adjust_altitude_srtm_disabled));

		fTourProvider = tourProvider;
	}

	@Override
	public void run() {

		if (UI.isTourEditorModified()) {
			return;
		}

		// get selected tour, make sure only one tour is selected
		final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
		if (selectedTours == null || selectedTours.size() != 1) {
			return;
		}

		final TourData tourData = selectedTours.get(0);

		if (tourData.latitudeSerie == null
				|| tourData.latitudeSerie.length == 0
				|| tourData.longitudeSerie == null
				|| tourData.longitudeSerie.length == 0) {

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.adjust_alti_srtm_dialog_title,
					Messages.adjust_alti_srtm_dialog_invalid_data);

			return;
		}
		
		new DialogAdjustAltitudeSRTM(Display.getCurrent().getActiveShell(), tourData).open();
	}

}
