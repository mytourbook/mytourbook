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
import net.tourbook.util.Util;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TrainingView extends ViewPart {

	private static final String			HEADER_HR_MAX_100			= "100% = ";							//$NON-NLS-1$

	public static final String			ID							= "net.tourbook.training.TrainingView"; //$NON-NLS-1$

	private static final String			STATE_SELECTED_CHART		= "SelectedChart";						//$NON-NLS-1$

	private static final int			CHART_ID_HR_TIME			= 0;
	private static final int			CHART_ID_HR_ZONES_WITH_TEXT	= 1;

	/**
	 * {@link #_chartId} must be in sync with {@link #_chartNames}
	 */
	private static final int[]			_chartId					= { //
																	CHART_ID_HR_TIME, //
			CHART_ID_HR_ZONES_WITH_TEXT, //
											//
																	};

	private static final String[]		_chartNames					= {
			Messages.Training_Chart_Name_HrTime,
			Messages.Training_Chart_Name_HrZonesText				};

	private final IPreferenceStore		_prefStore					= TourbookPlugin.getDefault()//
																			.getPreferenceStore();

	private final IDialogSettings		_state						= TourbookPlugin.getDefault()//
																			.getDialogSettingsSection(ID);

	private IPartListener2				_partListener;
	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;

	private TourPerson					_currentPerson;
	private TourData					_tourData;

	private ActionEditHrZones			_actionEditHrZones;

	private ArrayList<TourPersonHRZone>	_personHrZones;

	private final NumberFormat			_nf1						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/*
	 * UI controls
	 */
	private PixelConverter				_pc;
	private Font						_fontItalic;
	private FormToolkit					_tk;

	/**
	 * Pagebook for the training view
	 */
	private PageBook					_pageBookView;
	private Composite					_pageNoTour;
	private Composite					_pageNoPerson;
	private Composite					_pageNoHrZones;
	private Composite					_pageNoPulse;
	private Composite					_pageTraining;

	private Label						_lblNoHrZone;

	/**
	 * Pagebook for the HR charts
	 */
	private PageBook					_pageBookCharts;
	private Composite[]					_chartContainer				= new Composite[_chartId.length];

	private Composite					_hrZoneContainer;
	private ScrolledComposite			_hrZoneTextContainerContent;

	private Label[]						_lblTourMinMaxPercent;
	private Label[]						_lblTourMinMaxHours;

	private Combo						_comboTrainingChart;

	/*
	 * none UI
	 */

	public TrainingView() {}

	void actionEditHrZones() {

		PreferencesUtil.createPreferenceDialogOn(
				_pageBookView.getShell(),
				PrefPagePeople.ID,
				null,
				PrefPagePeople.PREF_DATA_SELECT_HR_ZONES).open();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TrainingView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
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

		_pageBookView.showPage(_pageNoTour);
	}

	private void createActions() {

		_actionEditHrZones = new ActionEditHrZones(this);

		fillActionBars();
	}

	@Override
	public void createPartControl(final Composite parent) {

		_currentPerson = TourbookPlugin.getActivePerson();

		createUI(parent);
		createActions();

		// show default page
		_pageBookView.showPage(_pageNoTour);

		addSelectionListener();
		addPrefListener();
		addTourEventListener();
		addPartListener();

		restoreState();

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_tk = new FormToolkit(parent.getDisplay());
		_fontItalic = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);

		_pageBookView = new PageBook(parent, SWT.NONE);

		_pageNoPerson = UI.createLabel(_tk, _pageBookView, Messages.UI_Label_PersonIsRequired);
		_pageNoTour = UI.createLabel(_tk, _pageBookView, Messages.UI_Label_no_chart_is_selected);
		_pageNoPulse = UI.createLabel(_tk, _pageBookView, Messages.Training_View_Label_NoPulseData);
		_pageNoHrZones = createUI10PageNoHrZones(_pageBookView);

		_pageTraining = createUI20Training(_pageBookView);
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

	private Composite createUI20Training(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI22Header(container);

			// label: horizontal separator
			final Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(SWT.DEFAULT, 1)
					.applyTo(label);
			label.setText(UI.EMPTY_STRING);

			createUI24Charts(container);
		}

		return container;
	}

	private void createUI22Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_comboTrainingChart = new Combo(container, SWT.READ_ONLY);
			_comboTrainingChart.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateUI30ValidateData(false);
				}
			});

			// fill combo
			for (final String chartName : _chartNames) {
				_comboTrainingChart.add(chartName);
			}
		}
	}

	private void createUI24Charts(final Composite parent) {

		_pageBookCharts = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBookCharts);
	}

	private Composite createUI26ChartContent(final int chartId) {

		switch (chartId) {
		case CHART_ID_HR_TIME:
			return createUI30PageHrTime(_pageBookCharts);

		case CHART_ID_HR_ZONES_WITH_TEXT:
			return createUI40PageHrZone(_pageBookCharts);

		default:
			break;
		}

		return null;
	}

	private Composite createUI30PageHrTime(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText("test"); //$NON-NLS-1$

		}

		return container;
	}

	private Composite createUI40PageHrZone(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			_hrZoneContainer = _tk.createComposite(container);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneContainer);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_hrZoneContainer);
		}

		return container;
	}

	private void createUI42HrZoneContent() {

		// person and zones are already checked

		_personHrZones = new ArrayList<TourPersonHRZone>(_currentPerson.getHrZones());
		Collections.sort(_personHrZones);

		if (_hrZoneTextContainerContent != null) {
			_hrZoneTextContainerContent.dispose();
		}

		final Composite scrolledContent;

		_hrZoneTextContainerContent = new ScrolledComposite(_hrZoneContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneTextContainerContent);
		{
			scrolledContent = createUI44HrZone(_hrZoneTextContainerContent);
		}

		_hrZoneTextContainerContent.setContent(scrolledContent);
		_hrZoneTextContainerContent.setExpandVertical(true);
		_hrZoneTextContainerContent.setExpandHorizontal(true);
		_hrZoneTextContainerContent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_hrZoneTextContainerContent.setMinSize(scrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// layout is necessary, dependent which other view is previously opened
		_pageBookView.layout(true, true);
	}

	private Composite createUI44HrZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI46HrZoneHeader(container);
			createUI48HrZoneFields(container);
		}
		_tk.adapt(container);

		return container;
	}

	private void createUI46HrZoneHeader(final Composite parent) {

		/*
		 * label: zone name
		 */
		Label label = _tk.createLabel(parent, Messages.Training_HRZone_Label_Header_Zone);
		label.setFont(_fontItalic);

		/*
		 * label: min/max pulse
		 */
		label = _tk.createLabel(parent, //
				HEADER_HR_MAX_100 + UI.SPACE
//						+ Integer.toString((int) _hrMax)
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

	private void createUI48HrZoneFields(final Composite parent) {

		final int hrZoneSize = _personHrZones.size();

		/*
		 * fields
		 */
		_lblTourMinMaxPercent = new Label[hrZoneSize];
		_lblTourMinMaxHours = new Label[hrZoneSize];

		for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

			final TourPersonHRZone hrZone = _personHrZones.get(zoneIndex);

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
		getViewSite().getPage().removePartListener(_partListener);

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

	private int getChartIndex(final int requestedChartId) {

		for (final int chartId : _chartId) {
			if (chartId == requestedChartId) {
				return chartId;
			}
		}

		// return default, this case should not happen
		return CHART_ID_HR_TIME;
	}

	/**
	 * @return Returns the id of the selected chart
	 */
	private int getSelectedChartId() {

		final int selectedIndex = _comboTrainingChart.getSelectionIndex();

		if (selectedIndex == -1) {
			return _chartId[0];
		}

		return _chartId[selectedIndex];
	}

	private String getUIMinMaxBpm(final TourPersonHRZone hrZone) {

		final String minMaxBpmText = UI.EMPTY_STRING;
		final double minValue = hrZone.getZoneMinValue();
		final double maxValue = hrZone.getZoneMaxValue();

//		if (minValue == Integer.MIN_VALUE) {
//
//			minMaxBpmText = UI.SYMBOL_LESS_THAN //
//					+ UI.SPACE
//					+ Integer.toString((int) (maxValue * _hrMax / 100));
//
//		} else if (maxValue == Integer.MAX_VALUE) {
//
//			minMaxBpmText = UI.SYMBOL_GREATER_THAN //
//					+ UI.SPACE
//					+ Integer.toString((int) (minValue * _hrMax / 100));
//
//		} else {
//
//			minMaxBpmText = Integer.toString((int) (minValue * _hrMax / 100))
//					+ UI.SYMBOL_DASH
//					+ Integer.toString((int) (maxValue * _hrMax / 100));
//		}

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

		// hr zones could be changed
		if (_hrZoneTextContainerContent != null) {
			_hrZoneTextContainerContent.dispose();
		}

		updateUI30ValidateData(false);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
			if (selectionTourData != null) {

				// prevent loading the same tour
				if (_tourData != null && _tourData.equals(selectionTourData)) {
					return;
				}

				updateUI20(selectionTourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {
				updateUI10(tourIds.get(0));
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateUI10(tourId);

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {

				updateUI10(((TVICatalogComparedTour) firstElement).getTourId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(
						compareResultItem.getComparedTourData().getTourId());
				updateUI20(tourData);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				updateUI10(refItem.getTourId());
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void restoreState() {

		final int stateSelectedChart = Util.getStateInt(_state, STATE_SELECTED_CHART, CHART_ID_HR_TIME);
		_comboTrainingChart.select(getChartIndex(stateSelectedChart));
	}

	private void saveState() {

		_state.put(STATE_SELECTED_CHART, getSelectedChartId());
	}

	@Override
	public void setFocus() {

	}

	private void showTourFromTourProvider() {

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBookView.isDisposed()) {
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
					updateUI20(selectedTours.get(0));
				}
			}
		});
	}

	private void updateUI10(final long tourId) {

		if (_tourData != null && _tourData.getTourId() == tourId) {
			// optimize
			return;
		}

		updateUI20(TourManager.getInstance().getTourData(tourId));
	}

	private void updateUI20(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

		_tourData = tourData;

		final TourPerson tourPerson = tourData.getTourPerson();
		if (tourPerson != _currentPerson) {

			// another person is contained in the tour

			/*
			 * dispose resources which depends on the current person
			 */
			// hr zones could be changed
			if (_hrZoneTextContainerContent != null) {
				_hrZoneTextContainerContent.dispose();
			}

			_currentPerson = tourPerson;
		}

		updateUI30ValidateData(true);
	}

	/**
	 * Displays training data when a tour is available
	 */
	private void updateUI30ValidateData(final boolean isTourModified) {

		if (_currentPerson == null) {

			// selected tour do not contain a person
			_pageBookView.showPage(_pageNoPerson);

			_comboTrainingChart.setEnabled(false);
			return;
		}

		// check HR zones
		final Set<TourPersonHRZone> personHrZones = _currentPerson.getHrZones();
		if (personHrZones == null || personHrZones.size() == 0) {

			// hr zones are required

			_lblNoHrZone.setText(NLS.bind(Messages.Training_View_Label_NoHrZones, _currentPerson.getName()));
			_lblNoHrZone.getParent().layout(true, true);

			_pageBookView.showPage(_pageNoHrZones);

			_comboTrainingChart.setEnabled(false);
			return;
		}

		if (_tourData == null) {

			// a tour is not selected
			_pageBookView.showPage(_pageNoTour);

			_comboTrainingChart.setEnabled(false);
			return;
		}

		final int[] pulseSerie = _tourData.pulseSerie;
		if (pulseSerie == null || pulseSerie.length == 0) {

			// pulse data are not available
			_pageBookView.showPage(_pageNoPulse);

			_comboTrainingChart.setEnabled(false);
			return;
		}

		/*
		 * required data are available
		 */
		_pageBookView.showPage(_pageTraining);
		_comboTrainingChart.setEnabled(true);

		// set tooltip for the part title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));

		// get selected chart id
		int selectedIndex = _comboTrainingChart.getSelectionIndex();
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		final int chartId = _chartId[selectedIndex];

		Composite chartContainer = _chartContainer[selectedIndex];
		if (chartContainer == null) {

			// create UI for the selected chart

			chartContainer = createUI26ChartContent(chartId);

			_chartContainer[selectedIndex] = chartContainer;
		}

		if (chartContainer == null) {
			// chart should never be null
			return;
		}

		// display page for the selected chart
		_pageBookCharts.showPage(chartContainer);

		if (isTourModified) {
			switch (chartId) {
			case CHART_ID_HR_TIME:
				updateUI40HrTime();

			case CHART_ID_HR_ZONES_WITH_TEXT:
				updateUI41HrZoneText();

			default:
				break;
			}

		}
	}

	private void updateUI40HrTime() {
		// TODO Auto-generated method stub

	}

	private void updateUI41HrZoneText() {

		// create hr zones when not yet done or disposed
		if (_hrZoneTextContainerContent == null || _hrZoneTextContainerContent.isDisposed()) {
			createUI42HrZoneContent();
		}

		final int personZoneSize = _personHrZones.size();
		final int[] tourHrZoneTimes = _tourData.getHrZones();
		final int[] timeSerie = _tourData.timeSerie;

		// display zone values
		final int time100 = timeSerie[timeSerie.length - 1];

		for (int tourZoneIndex = 0; tourZoneIndex < tourHrZoneTimes.length; tourZoneIndex++) {

			if (tourZoneIndex >= personZoneSize) {

				/*
				 * when zone data in the tours are not consistent, number of zones in the tour and
				 * in the person can be different
				 */
				break;
			}

			final double zoneTime = tourHrZoneTimes[tourZoneIndex];
			final double zoneTimePercent = zoneTime * 100.0 / time100;

			_lblTourMinMaxPercent[tourZoneIndex].setText(_nf1.format(zoneTimePercent));
			_lblTourMinMaxHours[tourZoneIndex].setText(UI.format_hh_mm((long) (zoneTime + 30)).toString());
		}
	}
}
