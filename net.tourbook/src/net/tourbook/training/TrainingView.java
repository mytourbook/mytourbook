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

import java.text.NumberFormat;
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
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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

public class TrainingView extends ViewPart {

	private static final String			HEADER_HR_MAX_100	= "100% = ";							//$NON-NLS-1$

	public static final String			ID					= "net.tourbook.training.TrainingView"; //$NON-NLS-1$

	private final NumberFormat			_nf1				= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	private final IPreferenceStore		_prefStore			= TourbookPlugin.getDefault()//
																	.getPreferenceStore();

	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;

	private TourPerson					_currentPerson;
	private TourData					_tourData;

	/**
	 * HR Max for the current person
	 */
	private double						_hrMax;

	private ActionEditHrZones			_actionEditHrZones;

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
	private Composite					_pageNoPulse;
	private Composite					_pageTraining;

	private Label						_lblNoHrZone;

	private Composite					_hrZoneContainer;
	private ScrolledComposite			_hrZoneContainerContent;

	private Label[]						_lblTourMinMaxPercent;
	private Label[]						_lblTourMinMaxHours;

	public TrainingView() {}

	void actionEditHrZones() {

		PreferencesUtil.createPreferenceDialogOn(
				_pageBook.getShell(),
				PrefPagePeople.ID,
				null,
				PrefPagePeople.PREF_DATA_SELECT_HR_ZONES).open();
	}

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

				if (part == TrainingView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {

			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TrainingView.this) {
					return;
				}

				if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;
//		_tourChart.updateChart(null, false);

		_pageBook.showPage(_pageNoTour);
	}

	private void createActions() {

		_actionEditHrZones = new ActionEditHrZones(this);

		fillActionBars();
	}

	@Override
	public void createPartControl(final Composite parent) {

		_currentPerson = TourbookPlugin.getActivePerson();
		if (_currentPerson != null) {
			_hrMax = _currentPerson.getHrMax();
		}

		createUI(parent);
		createActions();

		// show default page
		_pageBook.showPage(_pageNoTour);

		addSelectionListener();
		addPrefListener();
		addTourEventListener();

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

		_pageNoPerson = UI.createLabel(_tk, _pageBook, Messages.UI_Label_PersonIsNotSelected);
		_pageNoTour = UI.createLabel(_tk, _pageBook, Messages.UI_Label_no_chart_is_selected);
		_pageNoPulse = UI.createLabel(_tk, _pageBook, Messages.Training_View_Label_NoPulseData);
		_pageNoHrZones = createUI10PageNoHrZones(_pageBook);

		_pageTraining = createUI30PageHrZone(_pageBook);
	}

	private Composite createUI10PageNoHrZones(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.BEGINNING, SWT.CENTER).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: user name
			 */
			_lblNoHrZone = _tk.createLabel(container, UI.EMPTY_STRING, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNoHrZone);

			/*
			 * link: create hr zones
			 */
			final Link link = new Link(container, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(link);
			link.setText(Messages.Training_View_Link_NoHrZones);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionEditHrZones();
				}
			});
			_tk.adapt(link, true, true);
		}

		return container;
	}

	private Composite createUI30PageHrZone(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			_hrZoneContainer = _tk.createComposite(container);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneContainer);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_hrZoneContainer);
		}

		return container;
	}

	private void createUI35HrZoneContent() {

		// person and zones are already checked

		_hrZones = new ArrayList<TourPersonHRZone>(_currentPerson.getHrZones());
		Collections.sort(_hrZones);

		if (_hrZoneContainerContent != null) {
			_hrZoneContainerContent.dispose();
		}

		final Composite scrolledContent;

		_hrZoneContainerContent = new ScrolledComposite(_hrZoneContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneContainerContent);
		{
			scrolledContent = createUI36HrZone(_hrZoneContainerContent);
		}

		_hrZoneContainerContent.setContent(scrolledContent);
		_hrZoneContainerContent.setExpandVertical(true);
		_hrZoneContainerContent.setExpandHorizontal(true);
		_hrZoneContainerContent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_hrZoneContainerContent.setMinSize(scrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// layout is necessary, dependent which other view is previously opened
		_pageBook.layout(true, true);
	}

	private Composite createUI36HrZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(container);
		{
			createUI37HrZoneHeader(container);
			createUI38HrZoneFields(container);
		}
		_tk.adapt(container);

		return container;
	}

	private void createUI37HrZoneHeader(final Composite parent) {

		/*
		 * label: zone name
		 */
		Label label = _tk.createLabel(parent, Messages.Training_HRZone_Label_Header_Zone);
		label.setFont(_fontItalic);

		/*
		 * label: min/max pulse
		 */
		label = _tk.createLabel(parent, //
				HEADER_HR_MAX_100
						+ UI.SPACE
						+ Integer.toString((int) _hrMax)
						+ UI.SPACE
						+ Messages.Graph_Label_Heartbeat_unit);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.END, SWT.FILL).applyTo(label);
		label.setFont(_fontItalic);

		// label: %
		label = _tk.createLabel(parent, UI.SYMBOL_PERCENTAGE);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
				.applyTo(label);

		// label: h:mm
		label = _tk.createLabel(parent, Messages.App_Label_H_MM);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
				.applyTo(label);
	}

	private void createUI38HrZoneFields(final Composite parent) {

		final int hrZoneSize = _hrZones.size();

		/*
		 * fields
		 */
		_lblTourMinMaxPercent = new Label[hrZoneSize];
		_lblTourMinMaxHours = new Label[hrZoneSize];

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
			 * label: hr zone min/max % values
			 */
			final Label lblHRZoneMinMax = _tk.createLabel(parent, getUIMinMaxPercent(hrZone), SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.indent(10, 0)
					.applyTo(lblHRZoneMinMax);

			/*
			 * label: hr zone min/max bpm values
			 */
			final Label lblHRZoneMinMaxBpm = _tk.createLabel(parent, getUIMinMaxBpm(hrZone), SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.indent(10, 0)
					.applyTo(lblHRZoneMinMaxBpm);

			/*
			 * label: tour hr min/max %
			 */
			final Label lblTourMinMaxPercent = _lblTourMinMaxPercent[zoneIndex] = _tk.createLabel(
					parent,
					null,
					SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.hint(_pc.convertWidthInCharsToPixels(6), SWT.DEFAULT)
					.applyTo(lblTourMinMaxPercent);

			/*
			 * label: tour hr min/max h:mm
			 */
			final Label lblTourMinMaxHours = _lblTourMinMaxHours[zoneIndex] = _tk.createLabel(parent, null, SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.hint(_pc.convertWidthInCharsToPixels(6), SWT.DEFAULT)
					.applyTo(lblTourMinMaxHours);
		}
	}

	@Override
	public void dispose() {

		if (_tk != null) {
			_tk.dispose();
		}

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActionBars() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(_actionEditHrZones);
	}

	private String getUIMinMaxBpm(final TourPersonHRZone hrZone) {

		String minMaxBpmText;
		final double minValue = hrZone.getZoneMinValue();
		final double maxValue = hrZone.getZoneMaxValue();

		if (minValue == Integer.MIN_VALUE) {

			minMaxBpmText = UI.SYMBOL_LESS_THAN //
					+ UI.SPACE
					+ Integer.toString((int) (maxValue * _hrMax / 100));

		} else if (maxValue == Integer.MAX_VALUE) {

			minMaxBpmText = UI.SYMBOL_GREATER_THAN //
					+ UI.SPACE
					+ Integer.toString((int) (minValue * _hrMax / 100));

		} else {

			minMaxBpmText = Integer.toString((int) (minValue * _hrMax / 100))
					+ UI.SYMBOL_DASH
					+ Integer.toString((int) (maxValue * _hrMax / 100));
		}

		return minMaxBpmText;
	}

	private String getUIMinMaxPercent(final TourPersonHRZone hrZone) {

		String minMaxText;
		final int minValue = hrZone.getZoneMinValue();
		final int maxValue = hrZone.getZoneMaxValue();

		if (minValue == Integer.MIN_VALUE) {

			minMaxText = UI.SYMBOL_LESS_THAN //
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

		return minMaxText;
	}

	/**
	 * Person and/or hr zones are modified
	 */
	private void onModifyPerson() {

		_currentPerson = TourbookPlugin.getActivePerson();

		if (_currentPerson != null) {
			_hrMax = _currentPerson.getHrMax();
		}

		// hr zones could be changed
		if (_hrZoneContainerContent != null) {
			_hrZoneContainerContent.dispose();
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

		if (_currentPerson == null) {
			// a single person is not selected
			_pageBook.showPage(_pageNoPerson);
			return;
		}

		final Set<TourPersonHRZone> hrZones = _currentPerson.getHrZones();

		if (hrZones == null || hrZones.size() == 0) {
			// hr zones are required

			_lblNoHrZone.setText(NLS.bind(Messages.Training_View_Label_NoHrZones, _currentPerson.getName()));
			_lblNoHrZone.getParent().layout(true, true);

			_pageBook.showPage(_pageNoHrZones);
			return;
		}

		if (_tourData == null) {
			// a tour is not selected
			_pageBook.showPage(_pageNoTour);
			return;
		}

		final int[] pulseSerie = _tourData.pulseSerie;
		if (pulseSerie == null || pulseSerie.length == 0) {
			// pulse data are not available
			_pageBook.showPage(_pageNoPulse);
			return;
		}

		_pageBook.showPage(_pageTraining);

//		updateUITraining();

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

//	private void updateUITraining() {
//
//		// create hr zones when not yet done
//		if (_hrZoneContainerContent == null || _hrZoneContainerContent.isDisposed()) {
//			createUI35HrZoneContent();
//		}
//
////		final int zoneSize = _hrZones.size();
////		final int[] zoneMinBpm = _currentPerson.getHrZoneMinBpm();
////		final int[] zoneMaxBpm = _currentPerson.getHrZoneMaxBpm();
////
//		final int[] zoneTimes = _tourData.computeHrZones(_currentPerson);
////		final int[] pulseSerie = _tourData.pulseSerie;
//		final int[] timeSerie = _tourData.timeSerie;
////		int prevTime = 0;
////
////		// compute zone values
////		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {
////
////			final int pulse = pulseSerie[serieIndex];
////			final int time = timeSerie[serieIndex];
////
////			final int timeDiff = time - prevTime;
////
////			for (int zoneIndex = 0; zoneIndex < zoneMinBpm.length; zoneIndex++) {
////
////				final int minValue = zoneMinBpm[zoneIndex];
////				final int maxValue = zoneMaxBpm[zoneIndex];
////
////				if (pulse >= minValue && pulse < maxValue) {
////					zoneTimes[zoneIndex] += timeDiff;
////					break;
////				}
////			}
////
////			prevTime = time;
////		}
//
//		// display zone values
//		final int time100 = timeSerie[timeSerie.length - 1];
//
//		for (int zoneIndex = 0; zoneIndex < zoneTimes.length; zoneIndex++) {
//
//			final double zoneTime = zoneTimes[zoneIndex];
//			final double zoneTimePercent = zoneTime * 100.0 / time100;
//
//			_lblTourMinMaxPercent[zoneIndex].setText(_nf1.format(zoneTimePercent));
//			_lblTourMinMaxHours[zoneIndex].setText(UI.format_hh_mm((long) (zoneTime + 30)).toString());
//		}
//	}
}
