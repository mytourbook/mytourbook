/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

public class ActionDuplicateTour extends Action {

	private final ITourProvider _tourProvider;

	public ActionDuplicateTour(final ITourProvider tourProvider) {

		super(null, AS_PUSH_BUTTON);

		setText(Messages.Tour_Action_DuplicateTour);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Tour_Duplicate));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Tour_Duplicate_disabled));

		_tourProvider = tourProvider;
	}

	@Override
	public void run() {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

		if (selectedTours == null || selectedTours.size() < 1) {

			// a tour is not selected -> this should not happen, action should be disabled

			return;
		}

		final TourDataEditorView tourEditorView = TourManager.openTourEditor(true);
		if (tourEditorView != null) {

			final TourData selectedTour = selectedTours.get(0);
			tourEditorView.actionCreateTour(selectedTour);
		}
	}
}
