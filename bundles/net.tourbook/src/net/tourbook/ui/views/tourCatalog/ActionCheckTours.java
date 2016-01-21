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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;

final class ActionCheckTours extends Action {

	private final TourCompareResultView	fCompareResultView;

	ActionCheckTours(final TourCompareResultView compareResultView) {
		super(Messages.Compare_Result_Action_check_selected_tours);
		fCompareResultView = compareResultView;
	}

	@Override
	public void run() {

		// check all selected compared tours which are not yet stored

		final CheckboxTreeViewer viewer = fCompareResultView.getViewer();
		final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
		if (selection.size() > 0) {

			for (final Object tour : selection.toArray()) {

				if (tour instanceof TVICompareResultComparedTour) {
					final TVICompareResultComparedTour comparedTour = (TVICompareResultComparedTour) tour;
					if (comparedTour.isSaved() == false) {
						viewer.setChecked(tour, true);
					}
				}
			}
		}
	}
}
