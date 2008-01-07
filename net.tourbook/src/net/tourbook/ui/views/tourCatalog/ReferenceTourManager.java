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
package net.tourbook.ui.views.tourCatalog;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;

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
				Messages.tourCatalog_view_dlg_add_reference_tour_title,
				Messages.tourCatalog_view_dlg_add_reference_tour_msg,
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
	 * Returns a {@link TourCompareConfig} or <code>null</code> when the reference tour cannot be
	 * loaded from the database
	 * 
	 * @param refId
	 *        Reference Id
	 * @return
	 */
	public TourCompareConfig getTourCompareConfig(final long refId) {

		TourCompareConfig compareConfig = fCompareConfigCache.get(refId);

		if (compareConfig != null) {
			return compareConfig;
		}

		// load the reference tour from the database
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final TourReference refTour = em.find(TourReference.class, refId);
		em.close();

		if (refTour == null) {
			return null;
		} else {

			/*
			 * create a new reference tour configuration
			 */

			final TourData refTourData = refTour.getTourData();
			final TourChartConfiguration refTourChartConfig = TourManager.createTourChartConfiguration();

			final TourChartConfiguration compTourchartConfig = TourManager.createTourChartConfiguration();

			final ChartDataModel chartDataModel = TourManager.getInstance()
					.createChartDataModel(refTourData, refTourChartConfig);

			compareConfig = new TourCompareConfig(refTour,
					chartDataModel,
					refTourData,
					refTourChartConfig,
					compTourchartConfig);

			// keep ref config in the cache
			fCompareConfigCache.put(refId, compareConfig);
		}

		return compareConfig;
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
}
