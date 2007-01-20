/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.tour;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IMemento;

public class Tour extends ViewForm {

	private static final String					MEMENTO_TOUR_SASH_TOURINFO		= "tour.sash.tourInfo"; //$NON-NLS-1$
	private static final String					MEMENTO_TOUR_TOURINFO_ISVISIBLE	= "tour.tourInfo.visible"; //$NON-NLS-1$

	private SashForm							fTourSash;
	private ToolBarManager						fToolbarMgr;
	private TourChart							fTourChart;

	// private ActionShowTourInfo fActionShowTourInfo;

	private Composite							fTourInfo;
	private Combo								fComboTourType;
	private ArrayList<TourType>					fTourTypes;
	private TourData							fTourData;
	private Preferences.IPropertyChangeListener	fPrefChangeListener;

	protected ListenerList						fPropertyListeners				= new ListenerList();

	/**
	 * Listener for tour changes
	 */
	public interface ITourChangeListener extends EventListener {

		public void tourChanged(TourChangeEvent event);
	}

	public static class TourChangeEvent extends EventObject {

		private static final long	serialVersionUID	= 1L;

		protected TourChangeEvent(Tour source) {
			super(source);
		}
	}

	public Tour(Composite parent, int style) {
		super(parent, style);
		createContent();
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					updateTourTypes();
				}
			}

		};
		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(
				fPrefChangeListener);
	}

	public void addTourChangedListener(ITourChangeListener listener) {
		fPropertyListeners.add(listener);
	}

	private void createActions() {

	// fActionShowTourInfo = new ActionShowTourInfo(this);
	//
	// fToolbarMgr.add(fActionShowTourInfo);
	}

	private void createContent() {

		// viewform topleft: toolbar
		final ToolBar toolBarControl = new ToolBar(this, SWT.FLAT | SWT.WRAP);

		// wrap the tool bar on resize
		// toolBarControl.addListener(SWT.Resize, new Listener() {
		// public void handleEvent(Event e) {
		//
		// Rectangle rect = getClientArea();
		//
		// Point size = toolBarControl.computeSize(rect.width, SWT.DEFAULT);
		// toolBarControl.setSize(size);
		// }
		// });

		fToolbarMgr = new ToolBarManager(toolBarControl);

		// viewform content: sash
		fTourSash = new SashForm(this, SWT.HORIZONTAL);
		fTourSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// fTourSash.addListener(SWT.Resize, new Listener() {
		// public void handleEvent(Event e) {
		//
		// Rectangle rect = getClientArea();
		//
		// Point size = toolBarControl.computeSize(rect.width, rect.height);
		// fTourSash.setSize(size);
		// }
		// });

		// set view form controls
		setTopLeft(toolBarControl);
		setContent(fTourSash);

		fTourInfo = createTourInfo(fTourSash);

		// tour chart
		fTourChart = new TourChart(fTourSash, SWT.FLAT, true);
		fTourChart.setToolBarManager(fToolbarMgr);

		createActions();

		addPrefListener();

		showTourInfo(false);
	}

	private Composite createTourInfo(Composite parent) {

		Composite tourInfoContainer = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginLeft = 5;
		gridLayout.marginTop = 5;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		tourInfoContainer.setLayout(gridLayout);

		Label tourInfo = new Label(tourInfoContainer, SWT.NONE);
		tourInfo.setText(Messages.Tour_Label_tour_type);

		fComboTourType = new Combo(tourInfoContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboTourType.setVisibleItemCount(10);
		fComboTourType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTourType();
			}
		});

		updateTourTypes();

		return tourInfoContainer;
	}

	public void dispose() {

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(
				fPrefChangeListener);

		super.dispose();
	}

	private void fireTourChanged() {
		Object[] listeners = fPropertyListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ITourChangeListener listener = (ITourChangeListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.tourChanged(new TourChangeEvent(Tour.this));
				}
			});
		}
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	public void removeTourChangedListener(ITourChangeListener listener) {
		fPropertyListeners.remove(listener);
	}

	/**
	 * set the memento and restore saved data
	 * 
	 * @param memento
	 */
	public void restoreState(IMemento memento) {

		if (memento == null) {
			return;
		}

		// restore sash weights
		UI
				.restoreSashWeight(fTourSash, memento, MEMENTO_TOUR_SASH_TOURINFO, (new int[] {
						10,
						10 }));

		Integer mementoIsVisible = memento.getInteger(MEMENTO_TOUR_TOURINFO_ISVISIBLE);
		if (mementoIsVisible != null) {

			// show/hide tour info
			// boolean isInfoVisible = mementoIsVisible == 1;
			// fActionShowTourInfo.setChecked(isInfoVisible);
			// showTourInfo(isInfoVisible);
		}
	}

	/**
	 * save the tour settings
	 * 
	 * @param memento
	 */
	public void saveState(IMemento memento) {
		// save sash weights
		UI.saveSashWeight(fTourSash, memento, MEMENTO_TOUR_SASH_TOURINFO);

		// save tour info status
		// memento
		// .putInteger(MEMENTO_TOUR_TOURINFO_ISVISIBLE,
		// fActionShowTourInfo.isChecked()
		// ? 1
		// : 0);
	}

	/**
	 * @param showTourInfo
	 *        <code>true</code> to show the sash part for the tour info
	 */
	void showTourInfo(boolean showTourInfo) {
		fTourSash.setMaximizedControl(showTourInfo ? null : fTourChart);
	}

	/**
	 * Update the tour data
	 * 
	 * @param tourData
	 */
	public void refreshTourData(TourData tourData) {

		fTourData = tourData;

		fComboTourType.deselectAll();

		// select the tour type in the combo box
		TourType dataTourType = tourData.getTourType();
		if (dataTourType != null) {

			long typeId = dataTourType.getTypeId();

			int typeIndex = 0;
			for (TourType tourType : fTourTypes) {
				if (tourType.getTypeId() == typeId) {
					fComboTourType.select(typeIndex);
					break;
				}
				typeIndex++;
			}
		}
	}

	private void updateTourType() {

		if (fTourData == null) {
			return;
		}

		int typeIndex = fComboTourType.getSelectionIndex();

		if (typeIndex == -1) {
			return;
		}

		fTourData.setTourType(fTourTypes.get(typeIndex));

		TourDatabase.saveTour(fTourData);

		fireTourChanged();
	}

	/**
	 * read the tour types from the db and update the ui list
	 */
	private void updateTourTypes() {

		fTourTypes = TourDatabase.getTourTypes();

		if (fTourTypes == null || fComboTourType.isDisposed()) {
			return;
		}

		fComboTourType.removeAll();
		for (TourType tourType : fTourTypes) {
			fComboTourType.add(tourType.getName());
		}
	}

	public TourData getTourData() {
		return fTourData;
	}
}
