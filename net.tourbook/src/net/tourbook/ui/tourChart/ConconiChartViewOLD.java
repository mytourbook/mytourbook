/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.data.TourData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourInfoToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.ui.ViewerDetailForm;

/**
 * Shows the selected tours in a conconi test chart
 */
public class ConconiChartViewOLD extends ViewPart {

	public static final String		ID									= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private static final String		STATE_CONCONIT_TOURS_VIEWER_WIDTH	= "STATE_CONCONIT_TOURS_VIEWER_WIDTH";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault() //
																				.getPreferenceStore();
	private final IDialogSettings	_state								= TourbookPlugin.getDefault().//
																				getDialogSettingsSection(ID);

	private TourData				_tourData;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;

	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private TourInfoToolTipProvider	_conconiTourInfoToolTipProvider;
	private ChartDataYSerie			_yDataPulse;

	private ConconiData				_conconiData;
	/*
	 * UI controls
	 */
	private PageBook				_pageBook;

//	private boolean							_isTourDirty					= false;
//	private boolean							_isDirtyDisabled				= true;
//	private int								_savedDpTolerance;
//	private int								_dpTolerance;

	private Label					_pageNoChart;

	private Composite				_pageConconiTest;
	private Chart					_chartConconiTest;
	private Scale					_scaleDeflection;

	private Label					_lblDeflactionPulse;
	private Label					_lblDeflactionPower;
	private ChartLayerConconiTest	_conconiLayer;

	private ViewerDetailForm		_detailFormConconi;

	private TableViewer				_viewerConconiTours;

	public class ConconiTourViewerContentProvicer implements IContentProvider {

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Tour chart type is selected
	 * 
	 * @param tourChartType
	 */
	void actionTourChartType(final TourChartType tourChartType) {

		_requestedTourChartType = tourChartType;

		updateChart10(_tourData);
	}

	@Override
	public void createPartControl(final Composite parent) {

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void restoreState() {

	}

//	private void savePreviousTour(final long tourId) {
//
//	}

//	private void savePreviousTour(final TourData newTourData) {
//
////		savedTour = TourDatabase.saveTour(tourData);
//	}

	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

//		if (_isDirtyDisabled) {
//			return;
//		}
//
//		if (_tourData != null && _savedDpTolerance != _tourData.getConconiDeflection()) {
//			_isTourDirty = true;
//		}
	}

}
