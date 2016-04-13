/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.tour.DialogAdjustTemperature;
import net.tourbook.tour.DialogAdjustTemperature_Wizard;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ActionAdjustTemperature extends Action {

	private final ITourProvider2	_tourProvider;

	public ActionAdjustTemperature(final ITourProvider2 tourProvider) {

		super(null, AS_PUSH_BUTTON);

		_tourProvider = tourProvider;

		setText(Messages.Tour_Action_AdjustTemperature);
	}

	@Override
	public void run() {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

		final Shell shell = Display.getCurrent().getActiveShell();
		if (selectedTours == null || selectedTours.size() < 1) {

			// a tour is not selected

			MessageDialog.openInformation(
					shell,
					Messages.Dialog_AdjustTemperature_Dialog_Title,
					Messages.UI_Label_TourIsNotSelected);

			return;
		}

		final DialogAdjustTemperature_Wizard wizard = new DialogAdjustTemperature_Wizard(selectedTours, _tourProvider);
		final DialogAdjustTemperature dialog = new DialogAdjustTemperature(shell, wizard);

		dialog.open();
	}
}
