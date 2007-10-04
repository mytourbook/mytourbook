/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMap;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

/**
 *
 */
public class ReferenceTourManager {

	private static ReferenceTourManager				instance			= null;

	private final HashMap<Long, TourCompareConfig>	fCompareConfigCache	= new HashMap<Long, TourCompareConfig>();

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
	 * @param tourEditor
	 */
	public TourReference addReferenceTour(TourEditor tourEditor) {

		// get the reference tour name
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
				Messages.Tour_Map_dlg_add_reference_tour_title,
				Messages.Tour_Map_dlg_add_reference_tour_msg,
				"", //$NON-NLS-1$
				null);

		if (dialog.open() != Window.OK) {
			return null;
		}

		TourChart tourChart = tourEditor.getTourChart();
		SelectionChartInfo chartInfo = tourChart.getChartInfo();
		TourData tourData = tourChart.getTourData();

		// create new tour reference
		TourReference newTourReference = new TourReference(dialog.getValue(),
				tourData,
				chartInfo.leftSliderValuesIndex,
				chartInfo.rightSliderValuesIndex);

		// add the tour reference into the tour data collection
		tourData.getTourReferences().add(newTourReference);

		tourEditor.setTourDirty();
		tourEditor.setRefTourIsCreated();

		return newTourReference;
	}

	/**
	 * @return Returns an array with all reference tours
	 */
	public Object[] getReferenceTours() {

		List<?> referenceTours = null;

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			referenceTours = em.createQuery("SELECT refTour \n" //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_REFERENCE + " refTour")).getResultList(); //$NON-NLS-1$ //$NON-NLS-2$

			em.close();
		}

		return referenceTours.toArray();
	}

	/**
	 * @param refId
	 * @return Returns the compare configuration for the reference id
	 */
	public TourCompareConfig getTourCompareConfig(Long refId) {
		return fCompareConfigCache.get(refId);
	}

	/**
	 * Sets the compare configuration for the reference id
	 * 
	 * @param refId
	 * @param refTourConfig
	 */
	public void setTourCompareConfig(long refId, TourCompareConfig refTourConfig) {
		fCompareConfigCache.put(refId, refTourConfig);
	}
}
