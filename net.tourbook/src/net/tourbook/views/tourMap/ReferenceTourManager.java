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
/**
 * 
 */
package net.tourbook.views.tourMap;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourChart;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

/**
 *
 */
public class ReferenceTourManager {

	private static ReferenceTourManager	instance	= null;

	private ReferenceTourManager() {}

	public static ReferenceTourManager getInstance() {
		if (instance == null) {
			instance = new ReferenceTourManager();
		}
		return instance;
	}

	/**
	 * persists a new reference tour
	 * 
	 * @param tourChart
	 */
	public TourReference addReferenceTour(TourChart tourChart) {

		// ask for the reference tour name
		InputDialog dialog = new InputDialog(
				Display.getCurrent().getActiveShell(),
				Messages.TourMap_Dlg_add_reference_tour_title,
				Messages.TourMap_Dlg_add_reference_tour_msg,
				"", //$NON-NLS-1$
				null);

		if (dialog.open() != Window.OK) {
			return null;
		}

		SelectionChartInfo chartInfo = tourChart.getChartInfo();
		TourData tourData = tourChart.getTourData();

		// create new tour reference
		TourReference newTourReference = new TourReference(
				dialog.getValue(),
				tourData,
				chartInfo.leftSlider.getValuesIndex(),
				chartInfo.rightSlider.getValuesIndex());

		// add the tour reference into the tour data collection
		tourData.getTourReferences().add(newTourReference);
		TourDatabase.saveTour(tourData);

		return newTourReference;
	}

	/**
	 * @return Returns an array with all reference tours
	 */
	public Object[] getReferenceTours() {

		ArrayList<TourReference> referenceTours = null;

		if (referenceTours == null) {

			EntityManager em = TourDatabase.getInstance().getEntityManager();

			if (em != null) {

				Query query = em.createQuery("SELECT refTour \n" //$NON-NLS-1$
						+ ("FROM " + TourDatabase.TABLE_TOUR_REFERENCE + " refTour")); //$NON-NLS-1$ //$NON-NLS-2$

				referenceTours = (ArrayList<TourReference>) query.getResultList();

				em.close();
			}
		}

		return referenceTours.toArray();
	}
}
