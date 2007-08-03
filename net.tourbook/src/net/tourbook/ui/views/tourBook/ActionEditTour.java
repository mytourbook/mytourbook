/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.ui.views.tourBook;

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;

public class ActionEditTour extends Action {

	private TourBookView	fTourView;

	public ActionEditTour(TourBookView tourBookView) {

		fTourView = tourBookView;

		setImageDescriptor(TourbookPlugin.getImageDescriptor("write_obj.gif"));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor("write_obj_disabled.gif"));

		setText("&Open Tour");

		setEnabled(false);
	}

	public void run() {

		Long tourId = fTourView.fActiveTourId;

		if (tourId != null) {
			TourManager.getInstance().openTourInEditor(tourId);
		}
	}

}
