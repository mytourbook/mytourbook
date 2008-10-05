/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContainer;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionNewTours;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperties;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class TourStatisticsView extends ViewPart implements ISelectedTours {

	static public final String		ID			= "net.tourbook.views.StatisticView";	//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	private StatisticContainer		fStatisticContainer;

	private PostSelectionProvider	fPostSelectionProvider;
	private IPartListener2			fPartListener;

	private IPropertyChangeListener	fPrefChangeListener;

	TourPerson						fActivePerson;
	TourTypeFilter					fActiveTourTypeFilter;

	public NumberFormat				fNF			= NumberFormat.getNumberInstance();

	private RGB						fRGBYearFg	= new RGB(255, 255, 255);
	private RGB						fRGBMonthFg	= new RGB(128, 64, 0);
	private RGB						fRGBTourFg	= new RGB(0, 0, 128);

	private RGB						fRGBYearBg	= new RGB(111, 130, 197);
	private RGB						fRGBMonthBg	= new RGB(220, 220, 255);
	private RGB						fRGBTourBg	= new RGB(240, 240, 255);

	private Color					fColorYearFg;
	private Color					fColorMonthFg;
	private Color					fColorTourFg;

	private Color					fColorYearBg;
	private Color					fColorMonthBg;
	private Color					fColorTourBg;

	public Font						fFontNormal;
	public Font						fFontBold;

	protected Long					fActiveTourId;

	private ITourPropertyListener	fTourPropertyListener;
	private ISelectionListener		fPostSelectionListener;

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					fStatisticContainer.activateActions(getSite());
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					fStatisticContainer.deactivateActions(getSite());
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {

			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
					fActiveTourTypeFilter = TourbookPlugin.getDefault().getActiveTourTypeFilter();

					refreshStatistics();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update statistics
					refreshStatistics();

				} else if (property.equals(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS)) {

					fStatisticContainer.refreshStatisticProvider();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					refreshStatistics();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionNewTours) {

					refreshStatistics();

				} else if (selection instanceof SelectionDeletedTours) {

					refreshStatistics();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {
				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED && propertyData instanceof TourProperties) {

					final TourProperties tourProperties = (TourProperties) propertyData;

					if (tourProperties.isTourEdited) {
						// ignore edit changes
						return;
					}

					// update statistics
					refreshStatistics();
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createResources();

		// this view is a selection provider, set it before the statistics container is created
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fStatisticContainer = new StatisticContainer(getViewSite(), fPostSelectionProvider, parent, SWT.NONE);

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourPropertyListener();

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();
		fActiveTourTypeFilter = TourbookPlugin.getDefault().getActiveTourTypeFilter();

		fStatisticContainer.restoreStatistics(fSessionMemento, fActivePerson, fActiveTourTypeFilter);
	}

	private void createResources() {

		final Display display = Display.getCurrent();

		fColorYearFg = new Color(display, fRGBYearFg);
		fColorYearBg = new Color(display, fRGBYearBg);
		fColorMonthFg = new Color(display, fRGBMonthFg);
		fColorMonthBg = new Color(display, fRGBMonthBg);
		fColorTourFg = new Color(display, fRGBTourFg);
		fColorTourBg = new Color(display, fRGBTourBg);

		fFontNormal = JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT);
		fFontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(fPartListener);
		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		fColorYearFg.dispose();
		fColorYearBg.dispose();
		fColorMonthFg.dispose();
		fColorMonthBg.dispose();
		fColorTourFg.dispose();
		fColorTourBg.dispose();

		super.dispose();
	}

	public ArrayList<TourData> getSelectedTours() {

		final TourbookStatistic selectedStatistic = fStatisticContainer.getSelectedStatistic();
		if (selectedStatistic == null) {
			return null;
		}

		final Long selectedTour = selectedStatistic.getSelectedTour();
		if (selectedTour == null) {
			return null;
		}

		final TourData tourInDb = TourDatabase.getTourFromDb(selectedTour);
		if (tourInDb == null) {
			return null;
		} else {
			final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
			selectedTours.add(tourInDb);
			return selectedTours;
		}
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	private void refreshStatistics() {
		fStatisticContainer.refreshStatistic(fActivePerson, fActiveTourTypeFilter);
	}

	@Override
	public void saveState(final IMemento memento) {
		fStatisticContainer.saveState(memento);
	}

	@Override
	public void setFocus() {}

}
