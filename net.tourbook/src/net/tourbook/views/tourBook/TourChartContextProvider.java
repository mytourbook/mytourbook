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
package net.tourbook.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.chart.ChartContextProvider;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.data.TourReference;
import net.tourbook.views.tourMap.ReferenceTourManager;
import net.tourbook.views.tourMap.SelectionNewRefTours;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Display;

class TourChartContextProvider implements ChartContextProvider {

	TourBookView	fView;

	public ChartXSlider		fSlider;


	/**
	 * add a new reference tour to all reference tours
	 */
	class ActionAddTourReference extends Action {

		ActionAddTourReference() {
			setText(Messages.TourMap_Action_create_reference_tour);
		}

		public void run() {

			TourReference refTour = ReferenceTourManager.getInstance().addReferenceTour(
					fView.getTourChart());

			if (refTour != null) {

				SelectionNewRefTours selection = new SelectionNewRefTours();
				selection.newRefTours.add(refTour);

				fView.firePostSelection(selection);
			}
		}
	}

	 class SliderAction extends Action {

		SliderAction(String text, ChartXSlider slider) {
			super(text);

			fSlider = slider;
		}

		public void run() {

			// create a new marker
			new MarkerDialog(TourChartContextProvider.this, Display.getCurrent().getActiveShell()).open();
		}
	}

	public TourChartContextProvider(TourBookView view) {
		fView = view;
	}

	public void fillBarChartContextMenu(IMenuManager menuMgr) {}

	public void fillXSliderContextMenu(	IMenuManager menuMgr,
										ChartXSlider leftSlider,
										ChartXSlider rightSlider) {

		if (leftSlider != null || rightSlider != null) {

			// marker menu
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(new SliderAction(Messages.TourMap_Action_create_marker, leftSlider));
			} else {
				menuMgr
						.add(new SliderAction(
								Messages.TourMap_Action_create_left_marker,
								leftSlider));
				menuMgr.add(new SliderAction(
						Messages.TourMap_Action_create_right_marker,
						rightSlider));
			}

			// add to chart reference
			menuMgr.add(new ActionAddTourReference());
		}
	}

}
