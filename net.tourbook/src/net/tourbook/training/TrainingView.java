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
import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.data.ZoneMinMaxBpm;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPagePeople;
import net.tourbook.preferences.PrefPagePeopleData;
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
import org.eclipse.jface.action.ToolBarManager;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TrainingView extends ViewPart {

	public static final String			ID										= "net.tourbook.training.TrainingView"; //$NON-NLS-1$

	private static final int			HR_LEFT_MIN_BORDER						= 0;
	private static final int			HR_RIGHT_MAX_BORDER						= 230;

	private static final String			HEADER_HR_MAX_100						= "100% = ";							//$NON-NLS-1$

	private static final String			STATE_SELECTED_CHART					= "SelectedChart";						//$NON-NLS-1$
	private static final String			STATE_HR_CHART_LEFT_BORDER				= "HrLeftChartBorder";					//$NON-NLS-1$
	private static final String			STATE_HR_CHART_RIGHT_BORDER				= "HrRightChartBorder";				//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_ALL_PULSE_VALUES			= "IsShowAllPulseValues";				//$NON-NLS-1$
	private static final String			STATE_IS_SYNC_VERTICAL_CHART_SCALING	= "IsSyncVerticalChartScaling";		//$NON-NLS-1$

	private static final int			CHART_ID_HR_TIME						= 0;
	private static final int			CHART_ID_HR_ZONES_WITH_TEXT				= 1;

	/**
	 * {@link #_chartId} must be in sync with {@link #_chartNames}
	 */
	private static final int[]			_chartId								= { //
																				CHART_ID_HR_TIME, //
			CHART_ID_HR_ZONES_WITH_TEXT, //
											//
																				};

	private static final String[]		_chartNames								= {
			Messages.Training_Chart_Name_HrTime,
			Messages.Training_Chart_Name_HrZonesText							};

	private final IPreferenceStore		_prefStore								= TourbookPlugin.getDefault()//
																						.getPreferenceStore();

	private final IDialogSettings		_state									= TourbookPlugin.getDefault()//
																						.getDialogSettingsSection(ID);

	private IPartListener2				_partListener;
	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;

	private ModifyListener				_defaultSpinnerModifyListener;
	private SelectionAdapter			_defaultSpinnerSelectionListener;
	private MouseWheelListener			_defaultSpinnerMouseWheelListener;

	private TourPerson					_currentPerson;
	private TourData					_tourData;

	private boolean						_isUpdateUI;
	private boolean						_isShowAllPulseValues;
	private boolean						_isSynchChartVerticalValues;

	private ActionEditHrZones			_actionEditHrZones;
	private ActionShowAllPulseValues	_actionShowAllPulseValues;
	private ActionSynchChartScale		_actionSynchVerticalChartScaling;

	private ArrayList<TourPersonHRZone>	_personHrZones;
	private final BarChartMinMaxKeeper	_minMaxKeeper							= new BarChartMinMaxKeeper();

	private final NumberFormat			_nf1									= NumberFormat.getNumberInstance();
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
	private PageBook					_pageBookChart;
	private Composite					_pageNoTour;
	private Composite					_pageNoPerson;
	private Composite					_pageNoHrZones;
	private Composite					_pageNoPulse;

	private Label						_lblNoHrZone;

	private Combo						_comboTrainingChart;
	private Spinner						_spinnerHrLeft;
	private Spinner						_spinnerHrRight;

	/**
	 * Pagebook pages for the HR charts
	 */
	private Composite[]					_pageHrCharts							= new Composite[_chartId.length];

	private Composite					_hrZoneContainer;
	private ScrolledComposite			_hrZoneTextContainerContent;

	private Label[]						_lblTourMinMaxPercent;
	private Label[]						_lblTourMinMaxHours;

	private Chart						_chartHrTime;

	/*
	 * none UI
	 */

	public TrainingView() {}

	void actionEditHrZones() {

		final TourPerson person = _currentPerson != null ? _currentPerson : TourbookPlugin.getActivePerson();

		PreferencesUtil.createPreferenceDialogOn(
				_pageBookChart.getShell(),
				PrefPagePeople.ID,
				null,
				new PrefPagePeopleData(PrefPagePeople.PREF_DATA_SELECT_HR_ZONES, person)//
				)
				.open();
	}

	void actionShowAllPulseValues() {

		_isShowAllPulseValues = _actionShowAllPulseValues.isChecked();

		updateUI30ValidateData(true);
	}

	void actionSynchChartScale() {

		_isSynchChartVerticalValues = _actionSynchVerticalChartScaling.isChecked();

		if (_isSynchChartVerticalValues == false) {
			_minMaxKeeper.resetMinMax();
		}

		updateUI30ValidateData(true);
	}

	private void addPartListener() {

		getViewSite().getPage().addPartListener(_partListener = new IPartListener2() {

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
		});
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

		_pageBookChart.showPage(_pageNoTour);
		enableControls();
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
		_pageBookChart.showPage(_pageNoTour);
		enableControls();

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

		initUI(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI10HeaderToolbar(container);
			createUI15Charts(container);
		}
	}

	private void createUI10HeaderToolbar(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * combo: hr chart
			 */
			_comboTrainingChart = new Combo(container, SWT.READ_ONLY);
			_comboTrainingChart.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateUI30ValidateData(true);
				}
			});

			// fill combo
			for (final String chartName : _chartNames) {
				_comboTrainingChart.add(chartName);
			}

			/*
			 * label: hr min
			 */
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Training_View_Label_LeftChartBorder);
			label.setToolTipText(Messages.Training_View_Label_LeftChartBorder_Tooltip);

			/*
			 * spinner: hr min
			 */
			_spinnerHrLeft = new Spinner(container, SWT.BORDER);
			_spinnerHrLeft.setMinimum(HR_LEFT_MIN_BORDER);
			_spinnerHrLeft.setMaximum(HR_RIGHT_MAX_BORDER);
			_spinnerHrLeft.addModifyListener(_defaultSpinnerModifyListener);
			_spinnerHrLeft.addSelectionListener(_defaultSpinnerSelectionListener);
			_spinnerHrLeft.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * label: hr min
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
//					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Training_View_Label_RightChartBorder);
			label.setToolTipText(Messages.Training_View_Label_RightChartBorder_Tooltip);

			/*
			 * spinner: hr max
			 */
			_spinnerHrRight = new Spinner(container, SWT.BORDER);
			_spinnerHrRight.setMinimum(HR_LEFT_MIN_BORDER);
			_spinnerHrRight.setMaximum(HR_RIGHT_MAX_BORDER);
			_spinnerHrRight.addModifyListener(_defaultSpinnerModifyListener);
			_spinnerHrRight.addSelectionListener(_defaultSpinnerSelectionListener);
			_spinnerHrRight.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * toolbar actions
			 */
			final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
			GridDataFactory.fillDefaults()//
//					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(toolbar);
			final ToolBarManager tbm = new ToolBarManager(toolbar);

			_actionShowAllPulseValues = new ActionShowAllPulseValues(this);
			_actionSynchVerticalChartScaling = new ActionSynchChartScale(this);

			tbm.add(_actionShowAllPulseValues);
			tbm.add(_actionSynchVerticalChartScaling);

			tbm.update(true);
		}

		// label: horizontal separator
		final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.hint(SWT.DEFAULT, 1)
				.applyTo(label);
		label.setText(UI.EMPTY_STRING);
	}

	private void createUI15Charts(final Composite parent) {

		_pageBookChart = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBookChart);

		_pageNoHrZones = createUI19PageNoHrZones(_pageBookChart);
		_pageNoPerson = UI.createLabel(_tk, _pageBookChart, Messages.UI_Label_PersonIsRequired);
		_pageNoTour = UI.createLabel(_tk, _pageBookChart, Messages.UI_Label_no_chart_is_selected);
		_pageNoPulse = UI.createLabel(_tk, _pageBookChart, Messages.Training_View_Label_NoPulseData);
	}

	private Composite createUI19PageNoHrZones(final Composite parent) {

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
			 * link: create hr zones in the pref page
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

	private Composite createUI26HrChart(final int chartId) {

		switch (chartId) {
		case CHART_ID_HR_TIME:
			return createUI30PageHrTime(_pageBookChart);

		case CHART_ID_HR_ZONES_WITH_TEXT:
			return createUI40PageHrZone(_pageBookChart);

		}

		return null;
	}

	private Composite createUI30PageHrTime(final Composite parent) {

		_chartHrTime = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartHrTime);

		return _chartHrTime;
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
		_pageBookChart.layout(true, true);
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

	private void enableControls() {

		boolean isHrZoneAvailable = false;

		// check tour
		final boolean isTourData = _tourData != null;
		if (isTourData) {

			// check pulse
			final int[] pulseSerie = _tourData.pulseSerie;
			final boolean isPulse = pulseSerie != null && pulseSerie.length > 0;
			if (isPulse) {

				// check person
				final boolean isPerson = _currentPerson != null;
				if (isPerson) {

					// check hr zones
					final Set<TourPersonHRZone> personHrZones = _currentPerson.getHrZones();
					final boolean isHrZones = personHrZones != null && personHrZones.size() > 0;

					isHrZoneAvailable = isHrZones;
				}
			}
		}

		_comboTrainingChart.setEnabled(isHrZoneAvailable);

		_spinnerHrLeft.setEnabled(isHrZoneAvailable && _isShowAllPulseValues == false);
		_spinnerHrRight.setEnabled(isHrZoneAvailable && _isShowAllPulseValues == false);

		_actionShowAllPulseValues.setEnabled(isHrZoneAvailable);
		_actionSynchVerticalChartScaling.setEnabled(isHrZoneAvailable);
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

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_tk = new FormToolkit(parent.getDisplay());
		_fontItalic = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);

		_defaultSpinnerModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifyHrBorder();
			}
		};

		_defaultSpinnerSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifyHrBorder();
			}
		};

		_defaultSpinnerMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				if (_isUpdateUI) {
					return;
				}
				onModifyHrBorder();
			}
		};
	}

	private void onModifyHrBorder() {

		int left = _spinnerHrLeft.getSelection();
		int right = _spinnerHrRight.getSelection();

		_isUpdateUI = true;
		{
			if (left == HR_LEFT_MIN_BORDER && right == HR_LEFT_MIN_BORDER) {

				right++;
				_spinnerHrRight.setSelection(right);

			} else if (left == HR_RIGHT_MAX_BORDER && right == HR_RIGHT_MAX_BORDER) {

				left--;
				_spinnerHrLeft.setSelection(left);

			} else if (left >= right) {

				left = right - 1;
				_spinnerHrLeft.setSelection(left);
			}
		}
		_isUpdateUI = false;

		updateUI30ValidateData(true);
	}

	/**
	 * Person and/or hr zones are modified
	 */
	private void onModifyPerson() {

		// hr zones could be changed
		if (_hrZoneTextContainerContent != null) {
			_hrZoneTextContainerContent.dispose();
		}

		// tour is modified MUST be set to true that disposed resources are recreated
		updateUI30ValidateData(true);
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

		_isShowAllPulseValues = Util.getStateBoolean(_state, STATE_IS_SHOW_ALL_PULSE_VALUES, false);
		_actionShowAllPulseValues.setChecked(_isShowAllPulseValues);

		_isSynchChartVerticalValues = Util.getStateBoolean(_state, STATE_IS_SYNC_VERTICAL_CHART_SCALING, false);
		_actionShowAllPulseValues.setChecked(_isSynchChartVerticalValues);

		_isUpdateUI = true;
		{
			_spinnerHrLeft.setSelection(Util.getStateInt(_state, STATE_HR_CHART_LEFT_BORDER, 60));
			_spinnerHrRight.setSelection(Util.getStateInt(_state, STATE_HR_CHART_RIGHT_BORDER, 200));
		}
		_isUpdateUI = false;
	}

	private void saveState() {

		_state.put(STATE_SELECTED_CHART, getSelectedChartId());

		_state.put(STATE_HR_CHART_LEFT_BORDER, _spinnerHrLeft.getSelection());
		_state.put(STATE_HR_CHART_RIGHT_BORDER, _spinnerHrRight.getSelection());

		_state.put(STATE_IS_SHOW_ALL_PULSE_VALUES, _actionShowAllPulseValues.isChecked());
		_state.put(STATE_IS_SYNC_VERTICAL_CHART_SCALING, _actionSynchVerticalChartScaling.isChecked());
	}

	@Override
	public void setFocus() {

	}

	private void showTourFromTourProvider() {

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBookChart.isDisposed()) {
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

		enableControls();

		if (_tourData == null) {

			// a tour is not selected
			_pageBookChart.showPage(_pageNoTour);

			return;
		}

		final int[] pulseSerie = _tourData.pulseSerie;
		if (pulseSerie == null || pulseSerie.length == 0) {

			// pulse data are not available
			_pageBookChart.showPage(_pageNoPulse);

			return;
		}

		if (_currentPerson == null) {

			// selected tour do not contain a person
			_pageBookChart.showPage(_pageNoPerson);

			return;
		}

		// check HR zones
		final Set<TourPersonHRZone> personHrZones = _currentPerson.getHrZones();
		if (personHrZones == null || personHrZones.size() == 0) {

			// hr zones are required

			_lblNoHrZone.setText(NLS.bind(Messages.Training_View_Label_NoHrZones, _currentPerson.getName()));
			_lblNoHrZone.getParent().layout(true, true);

			_pageBookChart.showPage(_pageNoHrZones);

			return;
		}

		/*
		 * required data are available
		 */
		_comboTrainingChart.setEnabled(true);

		// set tooltip for the part title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));

		// get selected chart id
		int selectedIndex = _comboTrainingChart.getSelectionIndex();
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		final int chartId = _chartId[selectedIndex];

		Composite hrChart = _pageHrCharts[selectedIndex];
		if (hrChart == null) {

			// create UI for the selected chart

			hrChart = createUI26HrChart(chartId);

			_pageHrCharts[selectedIndex] = hrChart;
		}

		if (hrChart == null) {
			// chart should never be null
			return;
		}

		// display page for the selected chart
		_pageBookChart.showPage(hrChart);

		if (isTourModified) {
			switch (chartId) {
			case CHART_ID_HR_TIME:
				updateUI40HrTime();
				break;

			case CHART_ID_HR_ZONES_WITH_TEXT:
				updateUI41HrZoneText();
				break;

			}

		}
	}

	private void updateUI40HrTime() {

		final int[] pulseSerie = _tourData.pulseSerie;
		final int[] timeSerie = _tourData.timeSerie;
		final boolean[] breakTimeSerie = _tourData.getBreakTimeSerie();
		final int serieSize = timeSerie.length;

		final ArrayList<TourPersonHRZone> hrSortedZones = _currentPerson.getHrZonesSorted();
		final int zoneSize = hrSortedZones.size();

		final RGB[] rgbBright = new RGB[zoneSize];
		final RGB[] rgbDark = new RGB[zoneSize];
		final RGB[] rgbLine = new RGB[zoneSize];

		int zoneIndex = 0;
		for (final TourPersonHRZone hrZone : hrSortedZones) {

			rgbBright[zoneIndex] = hrZone.getColor();
			rgbDark[zoneIndex] = hrZone.getColor();
			rgbLine[zoneIndex] = hrZone.getColor();

			zoneIndex++;
		}

		/*
		 * minPulse will be the first x-data point with the x-value = 0
		 */
		int minPulse;
		int maxPulse;

		if (_isShowAllPulseValues) {

			minPulse = maxPulse = pulseSerie[0];

			for (final int pulse : pulseSerie) {
				if (pulse < minPulse) {
					minPulse = pulse;
				} else if (pulse > maxPulse) {
					maxPulse = pulse;
				}
			}

		} else {
			minPulse = _spinnerHrLeft.getSelection();
			maxPulse = _spinnerHrRight.getSelection();
		}

		/*
		 * create x-data series
		 */
		final int pulseRange = maxPulse - minPulse + 1;

		final int[] xSeriePulse = new int[pulseRange];
		final int[] ySeriePulseTime = new int[pulseRange];

		final int[] colorIndex = new int[serieSize];
		final ZoneMinMaxBpm zoneMinMaxBpm = _currentPerson.getHrZoneMinMaxBpm(
				_currentPerson.getHrMaxFormula(),
				_currentPerson.getMaxPulse(),
				_currentPerson.getBirthDayWithDefault(),
				_tourData.getStartDateTime());
		final int[] zoneMinBpm = zoneMinMaxBpm.zoneMinValues;
		final int[] zoneMaxBpm = zoneMinMaxBpm.zoneMaxValues;

		for (int pulseIndex = 0; pulseIndex < pulseRange; pulseIndex++) {

			xSeriePulse[pulseIndex] = pulseIndex;

			// set color index for each pulse value
			zoneIndex = 0;
			for (; zoneIndex < zoneSize; zoneIndex++) {

				final int minValue = zoneMinBpm[zoneIndex];
				final int maxValue = zoneMaxBpm[zoneIndex];

				final int pulse = minPulse + pulseIndex;

				if (pulse >= minValue && pulse <= maxValue) {
					colorIndex[pulseIndex] = zoneIndex;
					break;
				}
			}
		}

//		for (int debugIndex = 0; debugIndex < zoneMaxBpm.length; debugIndex++) {
//			System.out.println(zoneMinBpm[debugIndex] + " - " + zoneMaxBpm[debugIndex]);
//		}
//		System.out.println("\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		int prevTime = 0;

		/*
		 * create y-data serie: get time/color for each pulse value
		 */
		for (int serieIndex = 0; serieIndex < serieSize; serieIndex++) {

			// get time for each pulse value
			final int currentTime = timeSerie[serieIndex];
			final int timeDiff = currentTime - prevTime;
			prevTime = currentTime;

			// check if time is in a break
			if (breakTimeSerie != null) {

				/*
				 * break time requires distance data, so it's possible that break time data are not
				 * available
				 */

				if (breakTimeSerie[serieIndex] == true) {
					// pulse time is not set within a break
					continue;
				}
			}

			final int pulse = pulseSerie[serieIndex];
			final int pulseIndex = pulse - minPulse;

			// check array bounds
			if (pulseIndex >= 0 && pulseIndex < pulseRange) {
				ySeriePulseTime[pulseIndex] += timeDiff;
			}
		}

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		/*
		 * x-axis: pulse
		 */
		final ChartDataXSerie xData = new ChartDataXSerie(xSeriePulse);
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_NUMBER_CENTER);
		xData.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		xData.setStartValue(minPulse);

		chartDataModel.setXData(xData);

		/*
		 * y-axis: time
		 */
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				new int[][] { new int[pulseRange] },
				new int[][] { ySeriePulseTime });
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setUnitLabel(UI.UNIT_LABEL_TIME);
		//		yData.setAllValueColors(0);
		//		yData.setYTitle(title);
		//		yData.setVisibleMinValue(0);

		yData.setColorIndex(new int[][] { colorIndex });
		yData.setRgbLine(rgbLine);
		yData.setRgbBright(rgbBright);
		yData.setRgbDark(rgbDark);

		chartDataModel.addYData(yData);

		if (_isSynchChartVerticalValues) {
			_minMaxKeeper.setMinMaxValues(chartDataModel);
		}
//		// set grid size
//		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
//		_chartHrTime.setGridDistance(
//				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
//				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the new data data model in the chart
		_chartHrTime.updateChart(chartDataModel, false);
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
