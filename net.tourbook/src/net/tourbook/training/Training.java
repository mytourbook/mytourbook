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
package net.tourbook.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPagePeople;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class Training extends ViewPart {

	private static final String			HEADER_HR_MAX_100	= "100% = ";

	public static final String			ID					= "net.tourbook.training.Training";				//$NON-NLS-1$

	private final IPreferenceStore		_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener		_prefChangeListener;

	private TourPerson					_activePerson;
	private TourData					_tourData;

	private ArrayList<TourPersonHRZone>	_hrZones;
	private PixelConverter				_pc;
	private Font						_fontItalic;

	/*
	 * UI controls
	 */
	private FormToolkit					_tk;

	private PageBook					_pageBook;
	private Composite					_pageNoTour;
	private Composite					_pageNoPerson;
	private Composite					_pageNoHrZones;
	private Composite					_pageTraining;

	private Composite					_hrZoneContainer;
	private Composite					_hrZoneInnerContainer;

	private Label[]						_lblTourMinMax;

	private Label						_lblNoHrZone;

	public Training() {}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					onModifyPerson();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == Training.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_tourData = null;
//		_tourChart.updateChart(null, false);

		_pageBook.showPage(_pageNoTour);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_activePerson = TourbookPlugin.getActivePerson();

		createUI(parent);

		// show default page
		_pageBook.showPage(_pageNoTour);

		addSelectionListener();
		addPrefListener();

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_tk = new FormToolkit(parent.getDisplay());
		_fontItalic = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoPerson = UI.createLabel(_pageBook, Messages.UI_Label_PersonIsNotSelected);
		_pageNoTour = UI.createLabel(_pageBook, Messages.UI_Label_no_chart_is_selected);
		_pageNoHrZones = createUI05NoHrZones(_pageBook);

		_pageTraining = createUI10Training(_pageBook);
	}

	private Composite createUI05NoHrZones(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.BEGINNING, SWT.CENTER).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_lblNoHrZone = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNoHrZone);
			_lblNoHrZone.setText(UI.EMPTY_STRING);

			final Link link = new Link(container, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(link);
			link.setText(Messages.Training_View_Link_NoHrZones);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(
							parent.getShell(),
							PrefPagePeople.ID,
							null,
							PrefPagePeople.PREF_DATA_SELECT_HR_ZONES).open();
				}
			});

		}

		return container;
	}

	private Composite createUI10Training(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			createUI20HrZoneContainer(container);
		}
		return container;
	}

	private Composite createUI20HrZoneContainer(final Composite parent) {

		_hrZoneContainer = _tk.createComposite(parent);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(_hrZoneContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_hrZoneContainer);

		return _hrZoneContainer;
	}

	private void createUI22HrZoneInnerContainer(final Composite parent) {

		// person and zones are already checked

		_hrZones = new ArrayList<TourPersonHRZone>(_activePerson.getHrZones());
		Collections.sort(_hrZones);

		if (_hrZoneInnerContainer != null) {
			_hrZoneInnerContainer.dispose();
		}

		_hrZoneInnerContainer = _tk.createComposite(parent);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(_hrZoneInnerContainer);
//		hrZoneContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI23HrZoneHeader(_hrZoneInnerContainer);
			createUI24HrZoneFields(_hrZoneInnerContainer);
		}

		// layout is necessary, dependent which other view is previously opened
		_pageBook.layout(true, true);
	}

	private void createUI23HrZoneHeader(final Composite parent) {

		/*
		 * label: zone name
		 */
		Label label = _tk.createLabel(parent, //
				Messages.Training_HRZone_Label_Header_Zone);
//		GridDataFactory.fillDefaults().applyTo(label);
		label.setFont(_fontItalic);

		/*
		 * header label: min/max pulse
		 */
		label = _tk.createLabel(parent, //
				HEADER_HR_MAX_100 + Integer.toString(_activePerson.getHrMax()) + Messages.Graph_Label_Heartbeat_unit);
//		GridDataFactory.fillDefaults().applyTo(label);
		label.setFont(_fontItalic);

		// spacer
		new Label(parent, SWT.NONE);
	}

	private void createUI24HrZoneFields(final Composite parent) {

		final int hrZoneSize = _hrZones.size();

		/*
		 * fields
		 */
		_lblTourMinMax = new Label[hrZoneSize];

		for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(zoneIndex);

			/*
			 * label: hr zone name
			 */
			final Label lblHRZoneName = _tk.createLabel(//
					parent,
					hrZone.getName(),
					SWT.LEAD);
			GridDataFactory.fillDefaults()//
//					.grab(true, false)
					.applyTo(lblHRZoneName);

			/*
			 * label: hr zone min/max zone values
			 */
			final Label lblHRZoneMinMax = _tk.createLabel(parent, null, SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.indent(10, 0)
					.applyTo(lblHRZoneMinMax);

			String minMaxText;

			final int minValue = hrZone.getZoneMinValue();
			final int maxValue = hrZone.getZoneMaxValue();

			if (minValue == Integer.MIN_VALUE) {

				minMaxText = UI.SYMBOL_LESS_THAN
						+ UI.SPACE
						+ Integer.toString(maxValue)
						+ UI.SPACE
						+ UI.SYMBOL_PERCENTAGE;

			} else if (maxValue == Integer.MAX_VALUE) {

				minMaxText = UI.SYMBOL_GREATER_THAN //
						+ UI.SPACE
						+ Integer.toString(minValue)
						+ UI.SPACE
						+ UI.SYMBOL_PERCENTAGE;

			} else {

				minMaxText = Integer.toString(minValue)
						+ UI.SYMBOL_DASH
						+ Integer.toString(maxValue)
						+ UI.SPACE
						+ UI.SYMBOL_PERCENTAGE;
			}

			lblHRZoneMinMax.setText(minMaxText);

			/*
			 * label: tour hr min/max values
			 */
			final Label lblTourMinMax = _lblTourMinMax[zoneIndex] = _tk.createLabel(parent, null, SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
					.applyTo(lblTourMinMax);
		}
	}

	@Override
	public void dispose() {

		if (_tk != null) {
			_tk.dispose();
		}

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
//		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	/**
	 * Person and/or hr zones are modified
	 */
	private void onModifyPerson() {

		_activePerson = TourbookPlugin.getActivePerson();

		// hr zones could be changed
		if (_hrZoneInnerContainer != null) {
			_hrZoneInnerContainer.dispose();
		}

		updateUI();
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
			if (selectionTourData != null) {

				// prevent loading the same tour
				if (_tourData != null && _tourData.equals(selectionTourData)) {
					return;
				}

				updateUI(selectionTourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {
				updateUI(tourIds.get(0));
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateUI(tourId);

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {

				updateUI(((TVICatalogComparedTour) firstElement).getTourId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(
						compareResultItem.getComparedTourData().getTourId());
				updateUI(tourData);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				updateUI(refItem.getTourId());
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	@Override
	public void setFocus() {

	}

	private void showTourFromTourProvider() {

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_tourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
					updateUI(selectedTours.get(0));
				}
			}
		});
	}

	/**
	 * Displays training data when a tour is available
	 */
	private void updateUI() {

		if (_activePerson == null) {
			// a single person is not selected
			_pageBook.showPage(_pageNoPerson);
			return;
		}

		final Set<TourPersonHRZone> hrZones = _activePerson.getHrZones();

		if (hrZones == null || hrZones.size() == 0) {
			// hr zones are required

			_lblNoHrZone.setText(NLS.bind(Messages.Training_View_Label_NoHrZones, _activePerson.getName()));
			_lblNoHrZone.getParent().layout(true, true);

			_pageBook.showPage(_pageNoHrZones);
			return;
		}

		if (_tourData == null) {
			// a tour is not selected
			_pageBook.showPage(_pageNoTour);
			return;
		}

		_pageBook.showPage(_pageTraining);

		updateUITraining();

		// set application window title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));
	}

	private void updateUI(final long tourId) {

		if (_tourData != null && _tourData.getTourId() == tourId) {
			// optimize
			return;
		}

		updateUI(TourManager.getInstance().getTourData(tourId));
	}

	private void updateUI(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

		_tourData = tourData;

		updateUI();
	}

	private void updateUITraining() {

		// create hr zones when not yet done
		if (_hrZoneInnerContainer == null || _hrZoneInnerContainer.isDisposed()) {
			createUI22HrZoneInnerContainer(_hrZoneContainer);
		}

		for (int zoneIndex = 0; zoneIndex < _hrZones.size(); zoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(zoneIndex);

			final int minValue = hrZone.getZoneMinValue();
			final int maxValue = hrZone.getZoneMaxValue();

//			_txtTourMinMax[zoneIndex].setText(minMaxValue);
		}

	}
}
