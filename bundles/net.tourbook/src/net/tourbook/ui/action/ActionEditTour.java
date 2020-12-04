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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;

public class ActionEditTour extends Action {

	private ITourProvider	_tourProvider;

	public ActionEditTour(final ITourProvider tourProvider) {

		setText(Messages.App_Action_edit_tour);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_disabled));

		setEnabled(false);

		_tourProvider = tourProvider;
	}

	public static void doAction(final ITourProvider tourProvider) {

		final TourDataEditorView tourEditorView = TourManager.openTourEditor(true);

		if (tourEditorView != null) {

			final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
			if (selectedTours != null && selectedTours.size() > 0) {
				tourEditorView.setTourData(selectedTours.get(0));
			}
		}
	}

	@Override
	public void run() {
		doAction(_tourProvider);
	}

}
