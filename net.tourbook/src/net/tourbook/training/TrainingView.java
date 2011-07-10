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
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPagePeople;
import net.tourbook.preferences.PrefPagePeopleData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
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

	private static final String			STATE_HR_CHART_LEFT_BORDER				= "HrLeftChartBorder";					//$NON-NLS-1$
	private static final String			STATE_HR_CHART_RIGHT_BORDER				= "HrRightChartBorder";				//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_ALL_PULSE_VALUES			= "IsShowAllPulseValues";				//$NON-NLS-1$
	private static final String			STATE_IS_SYNC_VERTICAL_CHART_SCALING	= "IsSyncVerticalChartScaling";		//$NON-NLS-1$

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

	private int							_pulseStart;
	private int[]						_xSeriePulse;

	private ArrayList<TourPersonHRZone>	_personHrZones							= new ArrayList<TourPersonHRZone>();
	private final BarChartMinMaxKeeper	_minMaxKeeper							= new BarChartMinMaxKeeper();

	private final NumberFormat			_nf1									= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/*
	 * UI controls/resources
	 */
	private PixelConverter				_pc;
	private Font						_fontItalic;
	private FormToolkit					_tk;

	private Color[]						_hrZoneColors;
	private Image						_hrZoneImage;

	/*
	 * Pagebook for the training view
	 */
	private PageBook					_pageBookHrZones;
	private Composite					_pageHrZones;
	private Composite					_pageNoTour;
	private Composite					_pageNoPerson;
	private Composite					_pageNoHrZones;
	private Composite					_pageNoPulse;

	private Label						_lblNoHrZone;

	private Composite					_headerToolbar;
	private Chart						_chartHrTime;

	private Spinner						_spinnerHrLeft;
	private Spinner						_spinnerHrRight;
	private Label						_lblHrMin;
	private Label						_lblHrMax;

	private Composite					_hrZoneDataContainer;
	private Composite					_hrZoneDataContainerContent;

	private Canvas						_canvasHrZoneImage;
	private Label[]						_lblTourMinMaxValue;
	private Label[]						_lblTourMinMaxHours;
	private Label[]						_lblHRZoneName;
	private Label[]						_lblHRZoneColor;
	private Label[]						_lblHrZonePercent;

	/*
	 * none UI
	 */
	private int							_imageCounter							= 0;

	/**
	 * Percentage for current tour and for each HR zone
	 */
	private double[]					_tourHrZonePercent;

	public TrainingView() {}

	void actionEditHrZones() {

		final TourPerson person = _currentPerson != null ? _currentPerson : TourbookPlugin.getActivePerson();

		PreferencesUtil.createPreferenceDialogOn(
				_pageBookHrZones.getShell(),
				PrefPagePeople.ID,
				null,
				new PrefPagePeopleData(PrefPagePeople.PREF_DATA_SELECT_HR_ZONES, person)//
				)
				.open();
	}

	void actionShowAllPulseValues() {

		_isShowAllPulseValues = _actionShowAllPulseValues.isChecked();

		updateUI30HrZonesFromModel();
	}

	void actionSynchChartScale() {

		_isSynchChartVerticalValues = _actionSynchVerticalChartScaling.isChecked();

		if (_isSynchChartVerticalValues == false) {
			_minMaxKeeper.resetMinMax();
		}

		updateUI30HrZonesFromModel();
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

				} else if (property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES)) {

					_chartHrTime.setGrid(
							_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
							_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
							_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
							_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

					// grid has changed, update chart
					updateUI30HrZonesFromModel();
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

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();

					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
						updateUI20(modifiedTours.get(0));
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;
		_currentPerson = null;

//		_tourChart.updateChart(null, false);

		_pageBookHrZones.showPage(_pageNoTour);
		enableControls();
	}

	private void createActions() {

		_actionEditHrZones = new ActionEditHrZones(this);

		fillActionBars();
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		createActions();

		// show default page
		_pageBookHrZones.showPage(_pageNoTour);

		addSelectionListener();
		addPrefListener();
		addTourEventListener();
		addPartListener();

		restoreState();

		showTour();
	}

//	private ChartToolTipInfo createToolTipInfo(final int valueIndex) {
//
//		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
//		toolTipInfo.setTitle(tourTitle);
//		toolTipInfo.setLabel(toolTipLabel);
//
//		return toolTipInfo;
//	}

	private void createUI(final Composite parent) {

		initUI(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI10HeaderToolbar(container);
			createUI12PageBook(container);
		}
	}

	private void createUI10HeaderToolbar(final Composite parent) {

		_headerToolbar = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_headerToolbar);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(_headerToolbar);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: hr min
			 */
			_lblHrMin = new Label(_headerToolbar, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblHrMin);
			_lblHrMin.setText(Messages.Training_View_Label_LeftChartBorder);
			_lblHrMin.setToolTipText(Messages.Training_View_Label_LeftChartBorder_Tooltip);

			/*
			 * spinner: hr min
			 */
			_spinnerHrLeft = new Spinner(_headerToolbar, SWT.BORDER);
			_spinnerHrLeft.setMinimum(HR_LEFT_MIN_BORDER);
			_spinnerHrLeft.setMaximum(HR_RIGHT_MAX_BORDER);
			_spinnerHrLeft.addModifyListener(_defaultSpinnerModifyListener);
			_spinnerHrLeft.addSelectionListener(_defaultSpinnerSelectionListener);
			_spinnerHrLeft.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * label: hr max
			 */
			_lblHrMax = new Label(_headerToolbar, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblHrMax);
			_lblHrMax.setText(Messages.Training_View_Label_RightChartBorder);
			_lblHrMax.setToolTipText(Messages.Training_View_Label_RightChartBorder_Tooltip);

			/*
			 * spinner: hr max
			 */
			_spinnerHrRight = new Spinner(_headerToolbar, SWT.BORDER);
			_spinnerHrRight.setMinimum(HR_LEFT_MIN_BORDER);
			_spinnerHrRight.setMaximum(HR_RIGHT_MAX_BORDER);
			_spinnerHrRight.addModifyListener(_defaultSpinnerModifyListener);
			_spinnerHrRight.addSelectionListener(_defaultSpinnerSelectionListener);
			_spinnerHrRight.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * toolbar actions
			 */
			final ToolBar toolbar = new ToolBar(_headerToolbar, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(toolbar);
			final ToolBarManager tbm = new ToolBarManager(toolbar);

			_actionShowAllPulseValues = new ActionShowAllPulseValues(this);
			_actionSynchVerticalChartScaling = new ActionSynchChartScale(this);

			tbm.add(_actionSynchVerticalChartScaling);
			tbm.add(_actionShowAllPulseValues);

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

	private void createUI12PageBook(final Composite parent) {

		_pageBookHrZones = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBookHrZones);

		_pageHrZones = createUI20PageHrZones(_pageBookHrZones);
		_pageNoHrZones = createUI14PageNoHrZones(_pageBookHrZones);

		_pageNoPerson = UI.createLabel(_tk, _pageBookHrZones, Messages.UI_Label_PersonIsRequired);
		_pageNoTour = UI.createLabel(_tk, _pageBookHrZones, Messages.UI_Label_no_chart_is_selected);
		_pageNoPulse = UI.createLabel(_tk, _pageBookHrZones, Messages.Training_View_Label_NoPulseData);
	}

	private Composite createUI14PageNoHrZones(final Composite parent) {

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

	private Composite createUI20PageHrZones(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.spacing(0, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		container.setBackground(_tk.getColors().getBackground());
		{
			createUI30HrZoneChart(container);
			createUI40HrZoneImage(container);
			createUI50HrZoneDataContainer(container);
		}

		return container;
	}

	private void createUI30HrZoneChart(final Composite parent) {

		/*
		 * chart
		 */
		_chartHrTime = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartHrTime);

		// set grid size
		_chartHrTime.setGrid(
				_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		_chartHrTime.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {

//					System.out.println(serieIndex + "\t" + valueIndex);
//					// TODO remove SYSTEM.OUT.PRINTLN

//					_postSelectionProvider.setSelection(selection);
			}
		});
	}

	private void createUI32HrZoneDataContainerContent() {

		// person and zones are already checked

		_personHrZones.clear();
		_personHrZones.addAll(_currentPerson.getHrZones());
		Collections.sort(_personHrZones);

		if (_hrZoneDataContainerContent != null) {
			_hrZoneDataContainerContent.dispose();
		}

		_hrZoneDataContainerContent = new Composite(_hrZoneDataContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_hrZoneDataContainerContent);
		GridLayoutFactory.fillDefaults().applyTo(_hrZoneDataContainerContent);
//		_hrZoneDataContainerContent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			createUI34HrZoneDataContent(_hrZoneDataContainerContent);
		}

		// layout is necessary, dependent which other view is previously opened
		_pageBookHrZones.layout(true, true);
	}

	private Composite createUI34HrZoneDataContent(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(container);
		{
			createUI36HrZoneHeader(container);
			createUI38HrZoneFields(container);
		}
		_tk.adapt(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		return container;
	}

	private void createUI36HrZoneHeader(final Composite parent) {

		/*
		 * label: zone name
		 */
		Label label = _tk.createLabel(parent, Messages.Training_HRZone_Label_Header_Zone);
		GridDataFactory.fillDefaults()//
//				.span(2, 1)
				.align(SWT.CENTER, SWT.FILL)
				.applyTo(label);
		label.setFont(_fontItalic);

		/*
		 * label: hr zone %
		 */
		label = _tk.createLabel(parent, UI.SYMBOL_PERCENTAGE);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
//				.align(SWT.CENTER, SWT.FILL)
				.applyTo(label);

		/*
		 * label: h:mm
		 */
		label = _tk.createLabel(parent, Messages.App_Label_H_MM);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
				.applyTo(label);

		/*
		 * label: bpm
		 */
		label = _tk.createLabel(parent, Messages.Graph_Label_Heartbeat_unit);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
//				.align(SWT.CENTER, SWT.FILL)
				.applyTo(label);
		/*
		 * color
		 */
		label = _tk.createLabel(parent, UI.EMPTY_STRING);
	}

	private void createUI38HrZoneFields(final Composite parent) {

		final int hrZoneSize = _personHrZones.size();

		/*
		 * fields
		 */
		_lblTourMinMaxValue = new Label[hrZoneSize];
		_lblTourMinMaxHours = new Label[hrZoneSize];
		_lblHRZoneName = new Label[hrZoneSize];
		_lblHRZoneColor = new Label[hrZoneSize];
		_lblHrZonePercent = new Label[hrZoneSize];

		// set hr zone colors
		disposeHrZoneResources();
		_hrZoneColors = new Color[hrZoneSize];

		final Display display = parent.getDisplay();

		// the sequence of the zones are inverted
		for (int zoneIndex = hrZoneSize - 1; zoneIndex >= 0; zoneIndex--) {

			final TourPersonHRZone hrZone = _personHrZones.get(zoneIndex);
			final Color hrZoneColor = _hrZoneColors[zoneIndex] = new Color(display, hrZone.getColor());

			/*
			 * label: hr zone name
			 */
			final Label lblHRZoneName = _lblHRZoneName[zoneIndex] = _tk.createLabel(//
					parent,
					hrZone.getNameShort(),
					SWT.LEAD);
			GridDataFactory.fillDefaults().applyTo(lblHRZoneName);

			/*
			 * label: hr zone %
			 */
			final Label lblHrZonePercent = _lblHrZonePercent[zoneIndex] = _tk.createLabel(//
					parent,
					null,
					SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
					.applyTo(lblHrZonePercent);

			/*
			 * label: tour hr min/max h:mm
			 */
			final Label lblTourMinMaxHours = _lblTourMinMaxHours[zoneIndex] = _tk.createLabel(parent, null, SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.hint(_pc.convertWidthInCharsToPixels(6), SWT.DEFAULT)
					.applyTo(lblTourMinMaxHours);

			/*
			 * label: tour hr min/max %
			 */
			final Label lblTourMinMaxBpm = _lblTourMinMaxValue[zoneIndex] = _tk.createLabel(//
					parent,
					null,
					SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					.hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
					.applyTo(lblTourMinMaxBpm);

			/*
			 * label: color
			 */
			final Label label = _lblHRZoneColor[zoneIndex] = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().hint(16, 16).applyTo(label);
			label.setBackground(hrZoneColor);
		}
	}

	private void createUI40HrZoneImage(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.extendedMargins(5, 0, 5, 5)
				.applyTo(container);
		{
			_canvasHrZoneImage = new Canvas(container, SWT.DOUBLE_BUFFERED);
			GridDataFactory.fillDefaults()//
					.grab(false, true)
					// force image width
					.hint(20, SWT.DEFAULT)
					.applyTo(_canvasHrZoneImage);

			_canvasHrZoneImage.addPaintListener(new PaintListener() {
				public void paintControl(final PaintEvent e) {

					if (_hrZoneImage == null || _hrZoneImage.isDisposed()) {
						return;
					}

					final GC gc = e.gc;

					/*
					 * fill background because when the part is resized, a gray background would
					 * appear
					 */
					gc.setBackground(_tk.getColors().getBackground());
					gc.fillRectangle(e.x, e.y, e.width, e.height);

					gc.drawImage(_hrZoneImage, 0, 0);
				}
			});

			_canvasHrZoneImage.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					updateUI44HrZoneImage();
				}
			});

			_canvasHrZoneImage.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(final DisposeEvent e) {
					if (_hrZoneImage != null) {
						_hrZoneImage.dispose();
					}
				}
			});
		}
	}

	private void createUI50HrZoneDataContainer(final Composite parent) {

		_hrZoneDataContainer = _tk.createComposite(parent);
		GridDataFactory.fillDefaults()//
//				.grab(false, true)
//				.minSize(SWT.DEFAULT, 1)
				.applyTo(_hrZoneDataContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_hrZoneDataContainer);
	}

	@Override
	public void dispose() {

		disposeHrZoneResources();

		if (_tk != null) {
			_tk.dispose();
		}

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void disposeHrZoneResources() {

		if (_hrZoneColors != null) {

			for (final Color hrZoneColor : _hrZoneColors) {
				hrZoneColor.dispose();
			}

			_hrZoneColors = null;
		}
	}

	private void enableControls() {

		final boolean isHrZoneAvailable = TrainingManager.isHrZoneDataAvailable(_tourData);
		final boolean isCustomScaling = isHrZoneAvailable && _isShowAllPulseValues == false;

//		_comboTrainingChart.setEnabled(canShowHrZones);

		_spinnerHrLeft.setEnabled(isCustomScaling);
		_spinnerHrRight.setEnabled(isCustomScaling);
		_lblHrMin.setEnabled(isCustomScaling);
		_lblHrMax.setEnabled(isCustomScaling);

		_actionSynchVerticalChartScaling.setEnabled(isCustomScaling);
		_actionShowAllPulseValues.setEnabled(isHrZoneAvailable);
	}

	private void fillActionBars() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(_actionEditHrZones);
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

//		_chartToolTipProvider = new IChartInfoProvider() {
//			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
//				return createToolTipInfo(valueIndex);
//			}
//		};
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

		updateUI30HrZonesFromModel();
	}

	/**
	 * Person and/or hr zones are modified
	 */
	private void onModifyPerson() {

		clearView();
		showTour();
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

//		final int stateSelectedChart = Util.getStateInt(_state, STATE_SELECTED_CHART, CHART_ID_HR_TIME);
//		_comboTrainingChart.select(getChartIndex(stateSelectedChart));

		_isShowAllPulseValues = Util.getStateBoolean(_state, STATE_IS_SHOW_ALL_PULSE_VALUES, false);
		_actionShowAllPulseValues.setChecked(_isShowAllPulseValues);

		_isSynchChartVerticalValues = Util.getStateBoolean(_state, STATE_IS_SYNC_VERTICAL_CHART_SCALING, false);
		_actionSynchVerticalChartScaling.setChecked(_isSynchChartVerticalValues);

		_isUpdateUI = true;
		{
			_spinnerHrLeft.setSelection(Util.getStateInt(_state, STATE_HR_CHART_LEFT_BORDER, 60));
			_spinnerHrRight.setSelection(Util.getStateInt(_state, STATE_HR_CHART_RIGHT_BORDER, 200));
		}
		_isUpdateUI = false;
	}

	private void saveState() {

//		_state.put(STATE_SELECTED_CHART, getSelectedChartId());

		_state.put(STATE_HR_CHART_LEFT_BORDER, _spinnerHrLeft.getSelection());
		_state.put(STATE_HR_CHART_RIGHT_BORDER, _spinnerHrRight.getSelection());

		_state.put(STATE_IS_SHOW_ALL_PULSE_VALUES, _actionShowAllPulseValues.isChecked());
		_state.put(STATE_IS_SYNC_VERTICAL_CHART_SCALING, _actionSynchVerticalChartScaling.isChecked());
	}

	@Override
	public void setFocus() {

	}

	private void showTour() {

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}

		enableControls();
	}

	private void showTourFromTourProvider() {

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBookHrZones.isDisposed()) {
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

			/*
			 * another person is contained in the tour, dispose resources which depends on the
			 * current person
			 */
			if (_hrZoneDataContainerContent != null) {
				_hrZoneDataContainerContent.dispose();
			}

			_currentPerson = tourPerson;
		}

		updateUI30HrZonesFromModel();
	}

	/**
	 * Displays training data when a tour is available
	 */
	private void updateUI30HrZonesFromModel() {

		enableControls();

		/*
		 * check tour data
		 */
		if (_tourData == null) {

			// a tour is not selected
			_pageBookHrZones.showPage(_pageNoTour);

			return;
		}

		/*
		 * check pulse
		 */
		final int[] pulseSerie = _tourData.pulseSerie;
		if (pulseSerie == null || pulseSerie.length == 0) {

			// pulse data are not available
			_pageBookHrZones.showPage(_pageNoPulse);

			return;
		}

		/*
		 * check person
		 */
		if (_currentPerson == null) {

			// selected tour do not contain a person
			_pageBookHrZones.showPage(_pageNoPerson);

			return;
		}

		/*
		 * check HR zones
		 */
		final Set<TourPersonHRZone> personHrZones = _currentPerson.getHrZones();
		if (personHrZones == null || personHrZones.size() == 0) {

			// hr zones are required

			_lblNoHrZone.setText(NLS.bind(Messages.Training_View_Label_NoHrZones, _currentPerson.getName()));
			_lblNoHrZone.getParent().layout(true, true);

			_pageBookHrZones.showPage(_pageNoHrZones);

			return;
		}

		/*
		 * required data are available
		 */

		// set tooltip for the part title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));

		// display page for the selected chart
		_pageBookHrZones.showPage(_pageHrZones);

		final HrZoneContext zoneMinMaxBpm = _currentPerson.getHrZoneContext(
				_currentPerson.getHrMaxFormula(),
				_currentPerson.getMaxPulse(),
				_currentPerson.getBirthDayWithDefault(),
				_tourData.getStartDateTime());

		updateUI40HrZoneChart(zoneMinMaxBpm);
		updateUI42HrZoneData(zoneMinMaxBpm);

		// update hr zone image
		updateUI44HrZoneImage();
	}

	private void updateUI40HrZoneChart(final HrZoneContext zoneMinMaxBpm) {

		final int[] pulseSerie = _tourData.pulseSerie;
		final int[] timeSerie = _tourData.timeSerie;
		final boolean[] breakTimeSerie = _tourData.getBreakTimeSerie();
		final int serieSize = timeSerie.length;

		final ArrayList<TourPersonHRZone> hrSortedZones = _currentPerson.getHrZonesSorted();
		final int zoneSize = hrSortedZones.size();

		final RGB[] rgbBright = new RGB[zoneSize];
		final RGB[] rgbDark = new RGB[zoneSize];
		final RGB[] rgbLine = new RGB[zoneSize];

		final RGB rgbWhite = new RGB(0xff, 0xff, 0xff);

		int zoneIndex = 0;
		for (final TourPersonHRZone hrZone : hrSortedZones) {

			rgbDark[zoneIndex] = hrZone.getColor();
			rgbBright[zoneIndex] = rgbWhite;
			rgbLine[zoneIndex] = hrZone.getColorDark();

			zoneIndex++;
		}

		/*
		 * minPulse will be the first x-data point with the x-value = 0
		 */
		int maxPulse;

		if (_isShowAllPulseValues) {

			_pulseStart = maxPulse = pulseSerie[0];

			for (final int pulse : pulseSerie) {
				if (pulse < _pulseStart) {
					_pulseStart = pulse;
				} else if (pulse > maxPulse) {
					maxPulse = pulse;
				}
			}

		} else {
			_pulseStart = _spinnerHrLeft.getSelection();
			maxPulse = _spinnerHrRight.getSelection();
		}

		/*
		 * create x-data series
		 */
		final int pulseRange = maxPulse - _pulseStart + 1;

		_xSeriePulse = new int[pulseRange];
		final int[] ySeriePulseTime = new int[pulseRange];

		final int[] colorIndex = new int[serieSize];

		final int[] zoneMinBpm = zoneMinMaxBpm.zoneMinBpm;
		final int[] zoneMaxBpm = zoneMinMaxBpm.zoneMaxBpm;

		for (int pulseIndex = 0; pulseIndex < pulseRange; pulseIndex++) {

			_xSeriePulse[pulseIndex] = pulseIndex;

			// set color index for each pulse value
			zoneIndex = 0;
			for (; zoneIndex < zoneSize; zoneIndex++) {

				final int minValue = zoneMinBpm[zoneIndex];
				final int maxValue = zoneMaxBpm[zoneIndex];

				final int pulse = _pulseStart + pulseIndex;

				if (pulse >= minValue && pulse <= maxValue) {
					colorIndex[pulseIndex] = zoneIndex;
					break;
				}
			}
		}

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
			final int pulseIndex = pulse - _pulseStart;

			// check array bounds
			if (pulseIndex >= 0 && pulseIndex < pulseRange) {
				ySeriePulseTime[pulseIndex] += timeDiff;
			}
		}

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

//		chartDataModel.setTitle(TourManager.getTourDateTimeFull(_tourData));
		chartDataModel.setTitle(TourManager.getTourDateTimeShort(_tourData));

		// set tool tip info provider
//		chartDataModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, _chartToolTipProvider);

		/*
		 * x-axis: pulse
		 */
		final ChartDataXSerie xData = new ChartDataXSerie(_xSeriePulse);
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_NUMBER_CENTER);
		xData.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		xData.setStartValue(_pulseStart);

		chartDataModel.setXData(xData);

		/*
		 * y-axis: time
		 */
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				new int[][] { new int[pulseRange] },
				new int[][] { ySeriePulseTime });

		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setYTitle(Messages.App_Label_H_MM);

		yData.setColorIndex(new int[][] { colorIndex });
		yData.setRgbLine(rgbLine);
		yData.setRgbBright(rgbBright);
		yData.setRgbDark(rgbDark);
		yData.setDefaultRGB(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());

		chartDataModel.addYData(yData);

		if (_isSynchChartVerticalValues && _isShowAllPulseValues == false) {
			_minMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the new data data model in the chart
		_chartHrTime.updateChart(chartDataModel, false);
	}

	/**
	 * @param zoneContext
	 *            Contains age and HR max values.
	 */
	private void updateUI42HrZoneData(final HrZoneContext zoneContext) {

		// create hr zones when not yet done or disposed
		if (_hrZoneDataContainerContent == null || _hrZoneDataContainerContent.isDisposed()) {
			createUI32HrZoneDataContainerContent();
		}

		final int personZoneSize = _personHrZones.size();
		final int[] tourHrZoneTimes = _tourData.getHrZones();
		final int drivingTime = _tourData.getTourDrivingTime();

		_tourHrZonePercent = new double[personZoneSize];

		final int tourHrZoneSize = tourHrZoneTimes.length;

		for (int tourZoneIndex = 0; tourZoneIndex < tourHrZoneSize; tourZoneIndex++) {

			if (tourZoneIndex >= personZoneSize) {

				/*
				 * when zone data in the tours are not consistent, number of zones in the tour and
				 * in the person can be different
				 */
				break;
			}

			final double zoneTime = tourHrZoneTimes[tourZoneIndex];
			final double zoneTimePercent = drivingTime == 0 //
					? 0
					: zoneTime * 100.0 / drivingTime;

			if (zoneTime == -1) {
				// this zone and following zones are not available
				break;
			}

			_tourHrZonePercent[tourZoneIndex] = zoneTimePercent;

			final TourPersonHRZone hrZone = _personHrZones.get(tourZoneIndex);

			final int zoneMaxValue = hrZone.getZoneMaxValue();
			final String zoneMaxValueText = zoneMaxValue == Integer.MAX_VALUE //
					? Messages.App_Label_max
					: Integer.toString(zoneMaxValue);

			final int zoneMinBpm = zoneContext.zoneMinBpm[tourZoneIndex];
			final int zoneMaxBmp = zoneContext.zoneMaxBpm[tourZoneIndex];
			final String zoneMaxBpmText = zoneMaxBmp == Integer.MAX_VALUE //
					? Messages.App_Label_max
					: Integer.toString(zoneMaxBmp);

			final int ageYears = zoneContext.age;
			final String ageText = UI.SPACE + ageYears + UI.SPACE2 + Messages.Pref_People_Label_Years;

			final String hrZoneTooltip =
			//
			hrZone.getNameLongShortcutFirst()
			//
					+ UI.NEW_LINE
					+ UI.NEW_LINE
					//
					+ hrZone.getZoneMinValue()
					+ UI.DASH
					+ zoneMaxValueText
					+ UI.SPACE
					+ UI.SYMBOL_PERCENTAGE
					//
					+ UI.SPACE
					+ UI.SYMBOL_EQUAL
					+ UI.SPACE
					//
					+ zoneMinBpm
					+ UI.DASH
					+ zoneMaxBpmText
					+ UI.SPACE
					+ Messages.Graph_Label_Heartbeat_unit
					//
					+ UI.NEW_LINE
					+ UI.NEW_LINE
					//
					+ Messages.Pref_People_Label_Age
					+ ageText
					//
					+ UI.DASH_WITH_DOUBLE_SPACE
					//
					+ Messages.HRMax_Label
					+ UI.SPACE
					+ zoneContext.hrMax
					+ Messages.Graph_Label_Heartbeat_unit
			//
			;

			// % values
			_lblHrZonePercent[tourZoneIndex].setText(_nf1.format(zoneTimePercent));
			_lblHrZonePercent[tourZoneIndex].setToolTipText(hrZoneTooltip);

			// bpm values
			_lblTourMinMaxValue[tourZoneIndex].setText(zoneMinBpm + UI.DASH + zoneMaxBpmText);
			_lblTourMinMaxValue[tourZoneIndex].setToolTipText(hrZoneTooltip);

			_lblTourMinMaxHours[tourZoneIndex].setText(UI.format_hh_mm((long) (zoneTime + 30)).toString());
			_lblTourMinMaxHours[tourZoneIndex].setToolTipText(hrZoneTooltip);

			_lblHRZoneName[tourZoneIndex].setToolTipText(hrZoneTooltip);
			_lblHRZoneColor[tourZoneIndex].setToolTipText(hrZoneTooltip);
		}
	}

	private void updateUI44HrZoneImage() {

		Display.getDefault().asyncExec(new Runnable() {

			final int	__counter	= ++_imageCounter;

			public void run() {

				// check if this is the newest request to create an image
				if (__counter < _imageCounter) {
					return;
				}

				final boolean isHrZoneDataAvailable = TrainingManager.isHrZoneDataAvailable(_tourData);

				final Point imageSize = _canvasHrZoneImage.getSize();
				final int devImageWidth = imageSize.x;
				final int devImageHeight = imageSize.y;

				if (_hrZoneImage != null) {
					_hrZoneImage.dispose();
				}

				final Image image = _hrZoneImage = new Image(Display.getDefault(), devImageWidth, devImageHeight);

				final int hrZoneSize = _personHrZones.size();

				final GC gc = new GC(image);
				{
					int devYPos = devImageHeight;

					if (isHrZoneDataAvailable) {

						for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

							final double hrZonePercent = _tourHrZonePercent[zoneIndex];
							final int devZoneHeight = (int) (hrZonePercent / 100.0 * devImageHeight);

							gc.setBackground(_hrZoneColors[zoneIndex]);
							gc.fillRectangle(0, devYPos - devZoneHeight, devImageWidth - 1, devZoneHeight);

							devYPos -= devZoneHeight;
						}

					} else {
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
						gc.fillRectangle(image.getBounds());
					}
				}
				gc.dispose();

				_canvasHrZoneImage.redraw();
			}
		});
	}
}
