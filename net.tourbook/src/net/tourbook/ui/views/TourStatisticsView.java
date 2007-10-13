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
package net.tourbook.ui.views;

import java.text.NumberFormat;

import net.tourbook.data.TourPerson;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContainer;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class TourStatisticsView extends ViewPart {

	static public final String		ID			= "net.tourbook.views.StatisticView";	//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	private StatisticContainer		fStatisticContainer;

	private PostSelectionProvider	fPostSelectionProvider;
	private IPartListener2			fPartListener;

	private IPropertyChangeListener	fPrefChangeListener;

	TourPerson						fActivePerson;
	long							fActiveTourTypeId;

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

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {

			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					fStatisticContainer.activateActions(getSite());
				}
			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {}

			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					fStatisticContainer.deactivateActions(getSite());
				}
			}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {

			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
					fActiveTourTypeId = TourbookPlugin.getDefault().getActiveTourType().getTypeId();

					refreshStatistics();
				}

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// force the tour type images to be recreated
					UI.getInstance().disposeTourTypeImages();

					// update statistics
					refreshStatistics();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void propertyChanged(int propertyId, Object propertyData) {
				if (propertyId == TourManager.TOUR_PROPERTY_TOUR_TYPE_CHANGED) {

					// update statistics
					refreshStatistics();
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	@Override
	public void createPartControl(Composite parent) {

		createResources();

		// set selection provider before the statistic container is created
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fStatisticContainer = new StatisticContainer(getViewSite(),
				fPostSelectionProvider,
				parent,
				SWT.NONE);

		addPartListener();
		addPrefListener();
		addTourPropertyListener();

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();
		fActiveTourTypeId = TourbookPlugin.getDefault().getActiveTourType().getTypeId();

		fStatisticContainer.restoreStatistics(fSessionMemento, fActivePerson, fActiveTourTypeId);
	}

	private void createResources() {

		Display display = Display.getCurrent();

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
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);

		fColorYearFg.dispose();
		fColorYearBg.dispose();
		fColorMonthFg.dispose();
		fColorMonthBg.dispose();
		fColorTourFg.dispose();
		fColorTourBg.dispose();

		super.dispose();
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void refreshStatistics() {
		fStatisticContainer.refreshStatistic(fActivePerson, fActiveTourTypeId);
	}

	@Override
	public void saveState(IMemento memento) {
		fStatisticContainer.saveState(memento);
	}

	@Override
	public void setFocus() {}

}
