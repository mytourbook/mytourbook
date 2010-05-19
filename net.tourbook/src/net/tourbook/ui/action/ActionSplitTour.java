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
import net.tourbook.tour.DialogExtractTour;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionSplitTour extends Action {

	private TourDataEditorView	_tourDataEditor;

	private int					_tourSplitIndex;

	/**
	 * @param tourDataEditor
	 * @param tourProvider
	 */
	public ActionSplitTour(final TourDataEditorView tourDataEditor) {

		_tourDataEditor = tourDataEditor;

		setText(Messages.App_Action_SplitTour);
	}

	@Override
	public void run() {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		// get tour
		final ArrayList<TourData> selectedTours = _tourDataEditor.getSelectedTours();
		if (selectedTours == null || selectedTours.size() == 0) {
			return;
		}

		// check person
		if (TourManager.isPersonSelected() == false) {
			return;
		}

		new DialogExtractTour(Display.getCurrent().getActiveShell(), //
				selectedTours.get(0),
				_tourSplitIndex).open();
	}

	public void setTourRange(final int tourSplitIndex) {
		_tourSplitIndex = tourSplitIndex;
	}
}
