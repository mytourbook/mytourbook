/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourDataEditor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.swimming.SwimStroke;
import net.tourbook.common.swimming.SwimStrokeText;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TimeZoneData;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer2;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map2.view.SelectionMapPosition;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourSaveListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.MessageManager;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionExtractTour;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.action.ActionSplitTour;
import net.tourbook.ui.tourChart.ChartLabel;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

// author: Wolfgang Schramm
// create: 24.08.2007

/**
 * This editor can edit (when all is implemented) all data for a tour
 */
public class TourDataEditorView extends ViewPart implements ISaveablePart2, ITourProvider2 {

	public static final String ID = "net.tourbook.views.TourDataEditorView"; //$NON-NLS-1$

	//
	private static final String		GRAPH_LABEL_HEARTBEAT_UNIT		= net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
	private static final String		VALUE_UNIT_K_CALORIES			= net.tourbook.ui.Messages.Value_Unit_KCalories;
	//
	private static final int			COLUMN_SPACING						= 20;
	//
	private static final String		WIDGET_KEY							= "widgetKey";																//$NON-NLS-1$
	private static final String		WIDGET_KEY_TOURDISTANCE			= "tourDistance";															//$NON-NLS-1$
	private static final String		WIDGET_KEY_ALTITUDE_UP			= "altitudeUp";															//$NON-NLS-1$
	private static final String		WIDGET_KEY_ALTITUDE_DOWN		= "altitudeDown";															//$NON-NLS-1$
	private static final String		WIDGET_KEY_PERSON					= "tourPerson";															//$NON-NLS-1$
	//
	private static final String		MESSAGE_KEY_ANOTHER_SELECTION	= "anotherSelection";													//$NON-NLS-1$
	//
	/**
	 * shows the busy indicator to load the slice viewer when there are more items as this value
	 */
	private static final int			BUSY_INDICATOR_ITEMS				= 5000;
	//
	private static final String		STATE_SELECTED_TAB				= "tourDataEditor.selectedTab";										//$NON-NLS-1$
	private static final String		STATE_ROW_EDIT_MODE				= "tourDataEditor.rowEditMode";										//$NON-NLS-1$
	private static final String		STATE_IS_EDIT_MODE				= "tourDataEditor.isEditMode";										//$NON-NLS-1$
	private static final String		STATE_CSV_EXPORT_PATH			= "tourDataEditor.csvExportPath";									//$NON-NLS-1$
	//
	private static final String		STATE_SECTION_CHARACTERISTICS	= "STATE_SECTION_CHARACTERISTICS";									//$NON-NLS-1$
	private static final String		STATE_SECTION_DATE_TIME			= "STATE_SECTION_DATE_TIME";											//$NON-NLS-1$
	private static final String		STATE_SECTION_PERSONAL			= "STATE_SECTION_PERSONAL";											//$NON-NLS-1$
	private static final String		STATE_SECTION_TITLE				= "STATE_SECTION_TITLE";												//$NON-NLS-1$
	private static final String		STATE_SECTION_WEATHER			= "STATE_SECTION_WEATHER";												//$NON-NLS-1$
	//
	static final String					STATE_LAT_LON_DIGITS				= "STATE_LAT_LON_DIGITS";												//$NON-NLS-1$
	static final int						DEFAULT_LAT_LON_DIGITS			= 5;
	//
	private final IPreferenceStore	_prefStore							= TourbookPlugin.getPrefStore();
	private final IPreferenceStore	_prefStoreCommon					= CommonActivator.getPrefStore();
	private final IDialogSettings		_state								= TourbookPlugin.getState(ID);
	private final IDialogSettings		_stateTimeSlice					= TourbookPlugin.getState(ID + ".slice");							//$NON-NLS-1$
	private final IDialogSettings		_stateSwimSlice					= TourbookPlugin.getState(ID + ".swimSlice");					//$NON-NLS-1$
	//
	private final boolean				_isOSX								= UI.IS_OSX;
	private final boolean				_isLinux								= UI.IS_LINUX;
	//
	/**
	 * Tour start daytime in seconds
	 */
	private int								_tourStartDayTime;
	//
	/*
	 * Data series which are displayed in the viewer, all are metric system
	 */
	private int[]							_serieTime;
	private float[]						_serieDistance;
	private float[]						_serieAltitude;
	private float[]						_serieTemperature;
	private float[]						_serieCadence;
	private float[]						_serieGradient;
	private float[]						_serieSpeed;
	private float[]						_seriePace;
	private float[]						_seriePower;
	private float[]						_seriePulse;
	private double[]						_serieLatitude;
	private double[]						_serieLongitude;
	private float[][]						_serieGears;
	private boolean[]						_serieBreakTime;
	//
	private short[]						_swimSerie_Cadence;
	private short[]						_swimSerie_LengthType;
	private short[]						_swimSerie_Strokes;
	private short[]						_swimSerie_StrokeStyle;
	private int[]							_swimSerie_Time;
	//
	private ColumnDefinition			_timeSlice_ColDef_Altitude;
	private ColumnDefinition			_timeSlice_ColDef_Cadence;
	private ColumnDefinition			_timeSlice_ColDef_Pulse;
	private ColumnDefinition			_timeSlice_ColDef_Temperature;
	private ColumnDefinition			_timeSlice_ColDef_Latitude;
	private ColumnDefinition			_timeSlice_ColDef_Longitude;
	//
	private ColumnDefinition			_swimSlice_ColDef_Cadence;
	private ColumnDefinition			_swimSlice_ColDef_Strokes;
	private ColumnDefinition			_swimSlice_ColDef_StrokeStyle;
	//
	private MessageManager				_messageManager;
	private PostSelectionProvider		_postSelectionProvider;
	private ISelectionListener			_postSelectionListener;
	private IPartListener2				_partListener;
	private IPropertyChangeListener	_prefChangeListener;
	private IPropertyChangeListener	_prefChangeListenerCommon;
	private ITourEventListener			_tourEventListener;
	private ITourSaveListener			_tourSaveListener;
	//
	private final NumberFormat			_nf1			= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf1NoGroup	= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf2			= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf3			= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf6			= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf3NoGroup	= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
		_nf6.setMinimumFractionDigits(6);
		_nf6.setMaximumFractionDigits(6);

		_nf1NoGroup.setMinimumFractionDigits(1);
		_nf1NoGroup.setMaximumFractionDigits(1);
		_nf1NoGroup.setGroupingUsed(false);

		_nf3NoGroup.setMinimumFractionDigits(3);
		_nf3NoGroup.setMaximumFractionDigits(3);
		_nf3NoGroup.setGroupingUsed(false);
	}
	//
	private long											_timeSlice_ViewerTourId	= -1;
	private long											_swimSlice_ViewerTourId	= -1;
	//
	/**
	 * <code>true</code>: rows can be selected in the viewer<br>
	 * <code>false</code>: cell can be selected in the viewer
	 */
	private boolean										_isRowEditMode				= true;
	private boolean										_isEditMode;
	private boolean										_isTourDirty				= false;
	private boolean										_isTourWithSwimData;
	//
	/**
	 * is <code>true</code> when the tour is currently being saved to prevent a modify event or the
	 * onSelectionChanged event
	 */
	private boolean										_isSavingInProgress		= false;

	/**
	 * when <code>true</code> data are loaded into fields
	 */
	private boolean										_isSetField					= false;

	/**
	 * contains the tour id from the last selection event
	 */
	private Long											_selectionTourId;
	//
	private KeyAdapter									_keyListener;
	private ModifyListener								_modifyListener;
	private ModifyListener								_verifyFloatValue;
	private ModifyListener								_verifyIntValue;
	private MouseWheelListener							_mouseWheelListener;
	private SelectionAdapter							_selectionListener;
	private SelectionAdapter							_tourTimeListener;
	private SelectionAdapter							_dateTimeListener;
	//
	private PixelConverter								_pc;

	/**
	 * this width is used as a hint for the width of the description field, this value also
	 * influences the width of the columns in this editor
	 */
	private final int										_hintTextColumnWidth		= _isOSX ? 200 : 150;
	private int												_hintValueFieldWidth;
	private int												_hintDefaultSpinnerWidth;

	/**
	 * is <code>true</code> when {@link #_tourChart} contains reference tours
	 */
	private boolean										_isReferenceTourAvailable;

	/**
	 * range for the reference tours, is <code>null</code> when reference tours are not available<br>
	 * 1st index = ref tour<br>
	 * 2nd index: 0:start, 1:end
	 */
	private int[][]										_refTourRange;

	private boolean										_isPartVisible				= false;

	/**
	 * when <code>true</code> additional info is displayed in the title area
	 */
	private boolean										_isInfoInTitle;

	/**
	 * is <code>true</code> when a cell editor is activ, otherwise <code>false</code>
	 */
	private boolean										_isCellEditorActive		= false;

	/**
	 * every requested UI update increased this counter
	 */
	private int												_uiUpdateCounter;

	/**
	 * counter when the UI update runnable is run, this will optimize performance to not update the
	 * UI when the part is hidden
	 */
	private int												_uiRunnableCounter		= 0;
	private int												_uiUpdateTitleCounter	= 0;
	private TourData										_uiRunnableTourData;
	private boolean										_uiRunnableForce_TimeSliceReload;
	private boolean										_uiRunnableForce_SwimSliceReload;
	private boolean										_uiRunnableIsDirtyDisabled;
	//
	private SliceEditingSupport_Float				_timeSlice_AltitudeEditingSupport;
	private SliceEditingSupport_Float				_timeSlice_PulseEditingSupport;
	private SliceEditingSupport_Float				_timeSlice_TemperatureEditingSupport;
	private SliceEditingSupport_Float				_timeSlice_CadenceEditingSupport;
	private SliceEditingSupport_Double				_timeSlice_LatitudeEditingSupport;
	private SliceEditingSupport_Double				_timeSlice_LongitudeEditingSupport;
	//
	private SliceEditingSupport_Short				_swimSlice_CadenceEditingSupport;
	private SliceEditingSupport_Short				_swimSlice_StrokesEditingSupport;
	private SliceEditingSupport_ComboBox			_swimSlice_StrokeStyleEditingSupport;
	//
	private int												_enableActionCounter		= 0;

	/**
	 * contains all markers with the data serie index as key
	 */
	private final HashMap<Integer, TourMarker>	_markerMap					= new HashMap<>();

	/**
	 * When <code>true</code> the tour is created with the tour editor
	 */
	private boolean										_isManualTour;
	private boolean										_isTitleModified;
	private boolean										_isAltitudeManuallyModified;
	private boolean										_isDistManuallyModified;
	private boolean										_isLocationStartModified;
	private boolean										_isLocationEndModified;
	private boolean										_isTimeZoneManuallyModified;
	private boolean										_isTemperatureManuallyModified;
	private boolean										_isWindSpeedManuallyModified;
	private boolean										_isSetDigits				= false;

	/*
	 * measurement unit values
	 */
	private float	_unitValueAltitude;
	private float	_unitValueDistance;
	private int[]	_unitValueWindSpeed;
	//
	/*
	 * actions
	 */
	private ActionComputeDistanceValues			_actionComputeDistanceValues;
	private ActionCreateTour						_actionCreateTour;
	private ActionCreateTourMarker				_actionCreateTourMarker;
	private ActionCSVTimeSliceExport				_actionCsvTimeSliceExport;
	private ActionDeleteDistanceValues			_actionDeleteDistanceValues;
	private ActionDeleteTimeSlicesKeepTime		_actionDeleteTimeSlicesKeepTime;
	private ActionDeleteTimeSlicesRemoveTime	_actionDeleteTimeSlicesRemoveTime;
	private ActionExport								_actionExportTour;
	private ActionExtractTour						_actionExtractTour;
	private ActionModifyColumns					_actionModify_TimeSliceColumns;
	private ActionModifyColumns					_actionModify_SwimSliceColumns;
	private ActionOpenAdjustAltitudeDialog		_actionOpenAdjustAltitudeDialog;
	private ActionOpenMarkerDialog				_actionOpenMarkerDialog;
	private ActionOpenPrefDialog					_actionOpenTourTypePrefs;
	private ActionSaveTour							_actionSaveTour;
	private ActionSetStartDistanceTo0			_actionSetStartDistanceTo_0;
	private ActionSplitTour							_actionSplitTour;
	private ActionToggleReadEditMode				_actionToggleReadEditMode;
	private ActionToggleRowSelectMode			_actionToggleRowSelectMode;
	private ActionUndoChanges						_actionUndoChanges;
	private ActionViewSettings						_actionViewSettings;
	//
	private TagMenuManager							_tagMenuMgr;

	/**
	 * Number of digits for the lat/lon columns.
	 */
	private int											_latLonDigits;

	private final NumberFormat						_nfLatLon	= NumberFormat.getNumberInstance();

	private TourData									_tourData;

	//
	// ################################################## UI controls ##################################################
	//

	private PageBook						_pageBook;
	private Composite						_page_NoTourData;
	private Form							_page_EditorForm;

	private PageBook						_pageBook_Swim;
	private Composite						_pageSwim_NoData;
	private Composite						_pageSwim_Data;
	//
	private CTabFolder					_tabFolder;
	private CTabItem						_tab_10_Tour;
	private CTabItem						_tab_20_TimeSlices;
	private CTabItem						_tab_30_SwimSlices;
	//
	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>	_firstColumnControls				= new ArrayList<>();
	private final ArrayList<Control>	_firstColumnContainerControls	= new ArrayList<>();
	private final ArrayList<Control>	_secondColumnControls			= new ArrayList<>();
	//
	private TourChart						_tourChart;

	private Composite						_tourContainer;
	//
	private ScrolledComposite			_tab1Container;
	private Composite						_tab2_TimeSlice_Container;
	//
	private Composite						_swimSliceViewerContainer;
	private Composite						_timeSliceViewerContainer;
	//
	private Section						_sectionTitle;
	private Section						_sectionDateTime;
	private Section						_sectionPersonal;
	private Section						_sectionWeather;
	private Section						_sectionCharacteristics;
	//
	private Label							_timeSlice_Label;
	private TableViewer					_timeSlice_Viewer;
	private Object[]						_timeSlice_ViewerItems;
	private ColumnManager				_timeSlice_ColumnManager;
	private TimeSlice_TourViewer		_timeSlice_TourViewer			= new TimeSlice_TourViewer();
	//
	private TableViewer					_swimSlice_Viewer;
	private Object[]						_swimSlice_ViewerItems;
	private ColumnManager				_swimSlice_ColumnManager;
	private SwimSlice_TourViewer		_swimSlice_TourViewer			= new SwimSlice_TourViewer();

	private FormToolkit					_tk;

	/*
	 * tab: tour
	 */
	private Combo					_comboTitle;
	//
	private Button					_rdoCadence_Rpm;
	private Button					_rdoCadence_Spm;
	//
	private CLabel					_lblCloudIcon;
	private CLabel					_lblTourType;
	//
	private ControlDecoration	_decoTimeZone;
	//
	private Combo					_comboClouds;
	private Combo					_comboLocation_Start;
	private Combo					_comboLocation_End;
	private Combo					_comboTimeZone;
	private Combo					_comboWindDirectionText;
	private Combo					_comboWindSpeedText;
	//
	private DateTime				_dtStartTime;
	private DateTime				_dtTourDate;
	//
	private Label					_lblAltitudeUpUnit;
	private Label					_lblAltitudeDownUnit;
	private Label					_lblDistanceUnit;
	private Label					_lblSpeedUnit;
	private Label					_lblStartTime;
	private Label					_lblTags;
	private Label					_lblTemperatureUnit;
	private Label					_lblTimeZone;
	//
	private Link					_linkDefaultTimeZone;
	private Link					_linkGeoTimeZone;
	private Link					_linkRemoveTimeZone;
	private Link					_linkTag;
	private Link					_linkTourType;
	//
	private Spinner				_spinBodyWeight;
	private Spinner				_spinCalories;
	private Spinner				_spinFTP;
	private Spinner				_spinRestPuls;
	private Spinner				_spinTemperature;
	private Spinner				_spinWindDirectionValue;
	private Spinner				_spinWindSpeedValue;
	//
	private Text					_txtAltitudeDown;
	private Text					_txtAltitudeUp;
	private Text					_txtDescription;
	private Text					_txtDistance;
	private Text					_txtWeather;
	//
	private TimeDuration			_timeDriving;
	private TimeDuration			_timePaused;
	private TimeDuration			_timeRecording;

	private class ActionViewSettings extends ActionToolbarSlideout {

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			return new SlideoutViewSettings(_pageBook, toolbar, _state, TourDataEditorView.this);
		}
	}

	private final class CellEditor_ComboBox_Customized extends ComboBoxCellEditor {

		public CellEditor_ComboBox_Customized(final Composite composite, final String[] allItemText) {
			super(composite, allItemText);
		}

		@Override
		public void activate() {

			super.activate();

			_isCellEditorActive = true;
			enableActionsDelayed();
		}

		@Override
		public void deactivate() {

			super.deactivate();

			_isCellEditorActive = false;
			enableActionsDelayed();
		}
	}

	/**
	 * It took me hours to find this location where the editor is activated/deactivated without using
	 * TableViewerEditor which is activated in setCellEditingSupport but not in the row edit mode.
	 */
	private final class CellEditor_Text_Customized extends TextCellEditor {

		private CellEditor_Text_Customized(final Composite parent) {
			super(parent);
		}

		@Override
		public void activate() {

			super.activate();

			_isCellEditorActive = true;
			enableActionsDelayed();
		}

		@Override
		public void deactivate() {

			super.deactivate();

			_isCellEditorActive = false;
			enableActionsDelayed();
		}
	}

	private final class SliceEditingSupport_ComboBox extends EditingSupport {

		private final ComboBoxCellEditor	__cellEditor;
		private short[]						__dataSerie;

		private boolean						__canEditSlice	= true;

		private SliceEditingSupport_ComboBox(final ComboBoxCellEditor comboBoxCellEditor, final short[] dataSerie) {

			super(_timeSlice_Viewer);

			__cellEditor = comboBoxCellEditor;
			__dataSerie = dataSerie;
		}

		@Override
		protected boolean canEdit(final Object element) {

			if ((__dataSerie == null)
					|| (isTourInDb() == false)
					|| (_isEditMode == false)
					|| (__canEditSlice == false)

			) {
				return false;
			}

			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return __cellEditor;
		}

		@Override
		protected Object getValue(final Object element) {

			final int intValue = __dataSerie[((TimeSlice) element).serieIndex];

			// combobox needs an integer value
			return intValue;
		}

		public void setDataSerie(final short[] dataSerie) {
			__dataSerie = dataSerie;
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof Integer) {

				try {

					// convert int -> short
					final short enteredValue = ((Integer) value).shortValue();

					final int serieIndex = ((TimeSlice) element).serieIndex;
					if (enteredValue != __dataSerie[serieIndex]) {

						// value has changed

						// update dataserie
						__dataSerie[serieIndex] = enteredValue < 0

								// an invalid item is selected -> hide item
								? Short.MIN_VALUE
								: enteredValue;

						updateUI_AfterSliceEdit();
					}

				} catch (final Exception e) {
					StatusUtil.log(e);
				}
			}
		}
	}

	private final class SliceEditingSupport_Double extends EditingSupport {

		private final TextCellEditor	__cellEditor;
		private double[]					__dataSerie;

		private SliceEditingSupport_Double(final TextCellEditor cellEditor, final double[] dataSerie) {
			super(_timeSlice_Viewer);
			__cellEditor = cellEditor;
			__dataSerie = dataSerie;
		}

		@Override
		protected boolean canEdit(final Object element) {

			if ((__dataSerie == null) || (isTourInDb() == false) || (_isEditMode == false)) {
				return false;
			}

			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return __cellEditor;
		}

		@Override
		protected Object getValue(final Object element) {
			return Double.valueOf(__dataSerie[((TimeSlice) element).serieIndex]).toString();
		}

		public void setDataSerie(final double[] dataSerie) {
			__dataSerie = dataSerie;
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				try {

					final double enteredValue = Double.parseDouble((String) value);
					final int serieIndex = ((TimeSlice) element).serieIndex;

					if (enteredValue != __dataSerie[serieIndex]) {

						// value has changed

						// update dataserie
						__dataSerie[serieIndex] = enteredValue;

						/*
						 * worldposition has changed, this is an absolute overkill, wenn only one position
						 * has changed
						 */
						_tourData.clearWorldPositions();

						updateUI_AfterSliceEdit();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				} finally {}
			}
		}
	}

	private final class SliceEditingSupport_Float extends EditingSupport {

		private final TextCellEditor	__cellEditor;
		private float[]					__dataSerie;

		private boolean					__canEditSlice	= true;

		private SliceEditingSupport_Float(final TextCellEditor cellEditor, final float[] dataSerie) {

			super(_timeSlice_Viewer);

			__cellEditor = cellEditor;
			__dataSerie = dataSerie;

		}

		@Override
		protected boolean canEdit(final Object element) {

			if ((__dataSerie == null)
					|| (isTourInDb() == false)
					|| (_isEditMode == false)
					|| (__canEditSlice == false)

			) {
				return false;
			}

			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return __cellEditor;
		}

		@Override
		protected Object getValue(final Object element) {

			final float metricValue = __dataSerie[((TimeSlice) element).serieIndex];
			float displayedValue = metricValue;

			/*
			 * convert current measurement system into metric
			 */
			if (__dataSerie == _serieAltitude) {

				if (_unitValueAltitude != 1) {

					// none metric measurement systemm

					displayedValue /= _unitValueAltitude;
				}

			} else if (__dataSerie == _serieTemperature) {

				displayedValue = UI.convertTemperatureFromMetric(metricValue);
			}

			return Float.toString(displayedValue);
		}

		void setCanEditSlices(final boolean canEditSlices) {

			__canEditSlice = canEditSlices;
		}

		public void setDataSerie(final float[] dataSerie) {
			__dataSerie = dataSerie;
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				try {

					/*
					 * convert entered value into metric value
					 */
					final float enteredValue = Float.parseFloat((String) value);
					float metricValue = enteredValue;

					if (__dataSerie == _serieAltitude) {

						if (_unitValueAltitude != 1) {

							// none metric measurement systemm

							// ensure float is used
							final float noneMetricValue = enteredValue;
							metricValue = Math.round(noneMetricValue * _unitValueAltitude);
						}

					}
					final boolean isTemperatureSerie = __dataSerie == _serieTemperature;

					if (isTemperatureSerie) {
						metricValue = UI.convertTemperatureToMetric(enteredValue);
					}

					final int serieIndex = ((TimeSlice) element).serieIndex;
					if (metricValue != __dataSerie[serieIndex]) {

						// value has changed

						// update dataserie
						__dataSerie[serieIndex] = metricValue;

						updateUI_AfterSliceEdit();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				} finally {}
			}
		}
	}

	private final class SliceEditingSupport_Short extends EditingSupport {

		private final TextCellEditor	__cellEditor;
		private short[]					__dataSerie;

		private SliceEditingSupport_Short(final TextCellEditor cellEditor, final short[] dataSerie) {

			super(_timeSlice_Viewer);

			__cellEditor = cellEditor;
			__dataSerie = dataSerie;
		}

		@Override
		protected boolean canEdit(final Object element) {

			if ((__dataSerie == null)
					|| (isTourInDb() == false)
					|| (_isEditMode == false)

			) {
				return false;
			}

			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return __cellEditor;
		}

		@Override
		protected Object getValue(final Object element) {

			final short metricValue = __dataSerie[((TimeSlice) element).serieIndex];
			final short displayedValue = metricValue;

			return Short.toString(displayedValue);
		}

		public void setDataSerie(final short[] dataSerie) {
			__dataSerie = dataSerie;
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				try {

					final short enteredValue = Short.parseShort((String) value);
					final short metricValue = enteredValue;

					final int serieIndex = ((TimeSlice) element).serieIndex;
					if (metricValue != __dataSerie[serieIndex]) {

						// value has changed

						// update dataserie
						__dataSerie[serieIndex] = metricValue;

						updateUI_AfterSliceEdit();

						// when swim cadence is modified the time slice viewer cadence is wrong
						// -> reload this viewer when tab is selected
						invalidateSliceViewers();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				} finally {}
			}
		}
	}

	private class SliceViewerItems {

		Object[]	__timeSlice_ViewerItems;
		Object[]	__swimSlice_ViewerItems;

		public SliceViewerItems() {

			__timeSlice_ViewerItems = new Object[0];
			__swimSlice_ViewerItems = new Object[0];
		}

		public SliceViewerItems(final Object[] timeSlice_ViewerItems, final Object[] swimSlice_ViewerItems) {

			__timeSlice_ViewerItems = timeSlice_ViewerItems;
			__swimSlice_ViewerItems = swimSlice_ViewerItems;
		}
	}

	private class SwimSlice_TourViewer implements ITourViewer2 {

		@Override
		public ColumnManager getColumnManager() {

			final CTabItem selectedTab = _tabFolder.getSelection();

			if (selectedTab == _tab_30_SwimSlices) {
				return _swimSlice_ColumnManager;
			}

			return null;
		}

		@Override
		public ColumnViewer getViewer() {

			final CTabItem selectedTab = _tabFolder.getSelection();

			if (selectedTab == _tab_30_SwimSlices) {
				return _swimSlice_Viewer;
			}

			return null;
		}

		@Override
		public boolean isColumn0Visible(final ColumnViewer columnViewer) {

			if (columnViewer == _swimSlice_Viewer) {
				// first column is hidden, this is a super hack that the second column can be right aligned
				return false;
			}

			return true;
		}

		@Override
		public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

			final ColumnViewer[] newColumnViewer = new ColumnViewer[1];

			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

				private void recreateSwimSliceViewer() {

					// preserve column width, selection and focus
					final ISelection selection = _swimSlice_Viewer.getSelection();

					final Table table = _swimSlice_Viewer.getTable();
					final boolean isFocus = table.isFocusControl();

					_swimSliceViewerContainer.setRedraw(false);
					{
						table.dispose();

						createUI_Tab_32_SwimSliceViewer(_swimSliceViewerContainer);

						_swimSliceViewerContainer.layout();

						// update the viewer
						_swimSlice_ViewerItems = getSliceViewerItems().__swimSlice_ViewerItems;
						_swimSlice_Viewer.setInput(_swimSlice_ViewerItems);
					}
					_swimSliceViewerContainer.setRedraw(true);

					_swimSlice_Viewer.setSelection(selection, true);

					if (isFocus) {
						_swimSlice_Viewer.getTable().setFocus();
					}

					newColumnViewer[0] = _swimSlice_Viewer;
				}

				@Override
				public void run() {

					if (columnViewer == _swimSlice_Viewer) {
						recreateSwimSliceViewer();
					}
				}
			});

			return newColumnViewer[0];
		}

		/**
		 * reload the content of the viewer
		 */
		@Override
		public void reloadViewer() {

			Display.getCurrent().asyncExec(new Runnable() {

				private void reloadSwimSliceViewer() {

					final ISelection previousSelection = _swimSlice_Viewer.getSelection();

					final Table table = _swimSlice_Viewer.getTable();
					if (table.isDisposed()) {
						return;
					}

					table.setRedraw(false);
					{
						/*
						 * update the viewer, show busy indicator when it's a large tour or the previous
						 * tour was large because it takes time to remove the old items
						 */
						if (((_tourData != null) && (_tourData.timeSerie != null)
								&& (_tourData.timeSerie.length > BUSY_INDICATOR_ITEMS))
								|| (table.getItemCount() > BUSY_INDICATOR_ITEMS)) {

							BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
								@Override
								public void run() {
									_swimSlice_ViewerItems = getSliceViewerItems().__swimSlice_ViewerItems;
									_swimSlice_Viewer.setInput(_swimSlice_ViewerItems);
								}
							});
						} else {
							_swimSlice_ViewerItems = getSliceViewerItems().__swimSlice_ViewerItems;
							_swimSlice_Viewer.setInput(_swimSlice_ViewerItems);
						}

						_swimSlice_Viewer.setSelection(previousSelection, true);
					}
					table.setRedraw(true);
				}

				@Override
				public void run() {

					final CTabItem selectedTab = _tabFolder.getSelection();

					if (selectedTab == _tab_30_SwimSlices) {
						reloadSwimSliceViewer();
					}
				}
			});
		}

		@Override
		public void updateColumnHeader(final ColumnDefinition colDef) {}
	}

	private class SwimSlice_ViewerContentProvider implements IStructuredContentProvider {

		public SwimSlice_ViewerContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return _swimSlice_ViewerItems;
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private class TimeDuration {

		private static final String	timeFormat			= "%5d:%02d:%02d";	//$NON-NLS-1$

		private PageBook					_pageBook;
		private Composite					_pageReadMode;
		private Composite					_pageEditMode;

		private Text						_txtTime;
		private Spinner					_spinHours;
		private Spinner					_spinMinutes;
		private Spinner					_spinSeconds;

		private boolean					_isTimeEditMode	= false;

		public TimeDuration(final Composite parent) {

			createUI(parent);
		}

		private void createUI(final Composite parent) {

			// fixed bug: https://sourceforge.net/tracker/index.php?func=detail&aid=3292465&group_id=179799&atid=890601
			// let the system decide which field width is used by setting min/max values
//			final int spinnerWidthHour = _pc.convertWidthInCharsToPixels(_isOSX ? 8 : 4);
//			final int spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 6 : 3);

			_pageBook = new PageBook(parent, SWT.NONE);

			/*
			 * page: read mode
			 */

			_pageReadMode = new Composite(_pageBook, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageReadMode);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_pageReadMode);
			{
				_txtTime = _tk.createText(_pageReadMode, UI.EMPTY_STRING, SWT.READ_ONLY);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtTime);
				_txtTime.setEnabled(false);

				_tk.createLabel(_pageReadMode, UI.UNIT_LABEL_TIME, SWT.READ_ONLY);
			}

			/*
			 * page: edit mode
			 */

			_pageEditMode = new Composite(_pageBook, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageEditMode);
			GridLayoutFactory.fillDefaults().numColumns(3).spacing(0, 0).applyTo(_pageEditMode);
			{
				/*
				 * hour
				 */
				_spinHours = new Spinner(_pageEditMode, SWT.BORDER);
				GridDataFactory.fillDefaults()//
//						.hint(spinnerWidthHour, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinHours);
				_spinHours.setMinimum(-1);
				_spinHours.setMaximum(999);
				_spinHours.setToolTipText(Messages.Tour_Editor_Label_Hours_Tooltip);

				_spinHours.addMouseWheelListener(_mouseWheelListener);
				_spinHours.addSelectionListener(_tourTimeListener);
				_tk.adapt(_spinHours, true, true);

				/*
				 * minute
				 */
				_spinMinutes = new Spinner(_pageEditMode, SWT.BORDER);
				GridDataFactory.fillDefaults()//
//						.hint(spinnerWidth, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinMinutes);
				_spinMinutes.setMinimum(-1);
				_spinMinutes.setMaximum(60);
				_spinMinutes.setToolTipText(Messages.Tour_Editor_Label_Minutes_Tooltip);

				_spinMinutes.addMouseWheelListener(_mouseWheelListener);
				_spinMinutes.addSelectionListener(_tourTimeListener);
				_tk.adapt(_spinMinutes, true, true);

				/*
				 * seconds
				 */
				_spinSeconds = new Spinner(_pageEditMode, SWT.BORDER);
				GridDataFactory.fillDefaults()//
//						.hint(spinnerWidth, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinSeconds);
				_spinSeconds.setMinimum(-1);
				_spinSeconds.setMaximum(60);
				_spinSeconds.setToolTipText(Messages.Tour_Editor_Label_Seconds_Tooltip);

				_spinSeconds.addMouseWheelListener(_mouseWheelListener);
				_spinSeconds.addSelectionListener(_tourTimeListener);
				_tk.adapt(_spinSeconds, true, true);
			}

			// default is read mode
			_pageBook.showPage(_pageReadMode);
		}

		/**
		 * @return Returns time in seconds
		 */
		public int getTime() {
			return (_spinHours.getSelection() * 3600) //
					+ (_spinMinutes.getSelection() * 60)
					+ _spinSeconds.getSelection();
		}

		public void setEditMode(final boolean isEditMode) {

			// optimize
			if (_isTimeEditMode == isEditMode) {
				// nothing changed
				return;
			}

			_isTimeEditMode = isEditMode;

			_pageBook.showPage(isEditMode ? _pageEditMode : _pageReadMode);

			// hide drawing artefact, this do not work 100% correct on winXP
			_tab1Container.setRedraw(false);
			{
				_tab1Container.layout(true, true);
			}
			_tab1Container.setRedraw(true);
		}

		public void setTime(final int recordingTime) {

			final int hours = recordingTime / 3600;
			final int minutes = (recordingTime % 3600) / 60;
			final int seconds = (recordingTime % 3600) % 60;

			_txtTime.setText(String.format(timeFormat, hours, minutes, seconds));

			final boolean isBackup = _isSetField;
			_isSetField = true;
			{
				_spinHours.setSelection(hours);
				_spinMinutes.setSelection(minutes);
				_spinSeconds.setSelection(seconds);
			}
			_isSetField = isBackup;
		}

		public void setTime(final int hours, final int minutes, final int seconds) {

			_txtTime.setText(String.format(timeFormat, hours, minutes, seconds));

			final boolean isBackup = _isSetField;
			_isSetField = true;
			{
				_spinHours.setSelection(hours);
				_spinMinutes.setSelection(minutes);
				_spinSeconds.setSelection(seconds);
			}
			_isSetField = isBackup;
		}
	}

	private class TimeSlice_TourViewer implements ITourViewer2 {

		@Override
		public ColumnManager getColumnManager() {

			final CTabItem selectedTab = _tabFolder.getSelection();

			if (selectedTab == _tab_20_TimeSlices) {
				return _timeSlice_ColumnManager;
			}

			return null;
		}

		@Override
		public ColumnViewer getViewer() {

			final CTabItem selectedTab = _tabFolder.getSelection();

			if (selectedTab == _tab_20_TimeSlices) {
				return _timeSlice_Viewer;
			}

			return null;
		}

		@Override
		public boolean isColumn0Visible(final ColumnViewer columnViewer) {

			if (columnViewer == _timeSlice_Viewer) {
				// first column is hidden, this is a super hack that the second column can be right aligned
				return false;
			}

			return true;
		}

		@Override
		public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

			final ColumnViewer[] newColumnViewer = new ColumnViewer[1];

			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

				private void recreateTimeSliceViewer() {

					// preserve column width, selection and focus
					final ISelection selection = _timeSlice_Viewer.getSelection();

					final Table table = _timeSlice_Viewer.getTable();
					final boolean isFocus = table.isFocusControl();

					_timeSliceViewerContainer.setRedraw(false);
					{
						table.dispose();

						createUI_Tab_22_TimeSliceViewer(_timeSliceViewerContainer);

						_timeSliceViewerContainer.layout();

						// update the viewer
						_timeSlice_ViewerItems = getSliceViewerItems().__timeSlice_ViewerItems;
						_timeSlice_Viewer.setInput(_timeSlice_ViewerItems);
					}
					_timeSliceViewerContainer.setRedraw(true);

					_timeSlice_Viewer.setSelection(selection, true);

					if (isFocus) {
						_timeSlice_Viewer.getTable().setFocus();
					}

					newColumnViewer[0] = _timeSlice_Viewer;
				}

				@Override
				public void run() {

					if (columnViewer == _timeSlice_Viewer) {
						recreateTimeSliceViewer();
					}
				}
			});

			return newColumnViewer[0];
		}

		/**
		 * reload the content of the viewer
		 */
		@Override
		public void reloadViewer() {

			Display.getCurrent().asyncExec(new Runnable() {

				private void reloadTimeSliceViewer() {

					final ISelection previousSelection = _timeSlice_Viewer.getSelection();

					final Table table = _timeSlice_Viewer.getTable();
					if (table.isDisposed()) {
						return;
					}

					table.setRedraw(false);
					{
						/*
						 * update the viewer, show busy indicator when it's a large tour or the previous
						 * tour was large because it takes time to remove the old items
						 */
						if (((_tourData != null) && (_tourData.timeSerie != null)
								&& (_tourData.timeSerie.length > BUSY_INDICATOR_ITEMS))
								|| (table.getItemCount() > BUSY_INDICATOR_ITEMS)) {

							BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
								@Override
								public void run() {
									_timeSlice_ViewerItems = getSliceViewerItems().__timeSlice_ViewerItems;
									_timeSlice_Viewer.setInput(_timeSlice_ViewerItems);
								}
							});
						} else {
							_timeSlice_ViewerItems = getSliceViewerItems().__timeSlice_ViewerItems;
							_timeSlice_Viewer.setInput(_timeSlice_ViewerItems);
						}

						_timeSlice_Viewer.setSelection(previousSelection, true);
					}
					table.setRedraw(true);
				}

				@Override
				public void run() {

					final CTabItem selectedTab = _tabFolder.getSelection();

					if (selectedTab == _tab_20_TimeSlices) {
						reloadTimeSliceViewer();
					}
				}
			});
		}

		@Override
		public void updateColumnHeader(final ColumnDefinition colDef) {}
	}

	private class TimeSlice_ViewerContentProvider implements IStructuredContentProvider {

		public TimeSlice_ViewerContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return _timeSlice_ViewerItems;
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Compute distance values from the geo positions
	 * <p>
	 * Performs the run() method in {@link ActionComputeDistanceValues}
	 */
	void actionComputeDistanceValuesFromGeoPosition() {

		if (TourManager.computeDistanceValuesFromGeoPosition(getSelectedTours()) == false) {
			return;
		}

		updateUI_AfterDistanceModifications();
	}

	/**
	 * Creates a new manually created tour, editor must not be dirty before this action is called
	 */
	public void actionCreateTour() {
		actionCreateTour(null);
	}

	/**
	 * Creates a new manually created tour, editor must not be dirty before this action is called
	 *
	 * @param copyFromOtherTour
	 *           When not <code>null</code> then the new tour is partly copied from this tour.
	 */
	public void actionCreateTour(final TourData copyFromOtherTour) {

		// check if a person is selected
		final TourPerson activePerson = TourbookPlugin.getActivePerson();
		if (activePerson == null) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_create_tour_title,
					Messages.tour_editor_dlg_create_tour_message);
			return;
		}

		try {

			final TourData manualTourData = copyFromOtherTour != null

					// clone other tour
					? (TourData) copyFromOtherTour.clone()

					// create new empty tour
					: new TourData();

			/*
			 * Adjust some cloned data
			 */

			// set tour start date/time
			manualTourData.setTourStartTime(TimeTools.now());

			// tour id must be created after the tour date/time is set
			manualTourData.createTourId();

			manualTourData.setDeviceId(TourData.DEVICE_ID_FOR_MANUAL_TOUR);
			manualTourData.setTourPerson(activePerson);

			// ensure that the time zone is saved in the tour
			_isTimeZoneManuallyModified = true;

			// update UI
			_tourData = manualTourData;
			_tourChart = null;
			updateUI_FromModel(manualTourData, false, true);

			// set editor into edit mode
			_isEditMode = true;
			_actionToggleReadEditMode.setChecked(true);

			enableActions();
			enableControls();

			// select tour tab and first field
			_tabFolder.setSelection(_tab_10_Tour);
			_comboTitle.setFocus();

			// set tour dirty even when nothing is entered but the user can see that this tour must be saved or discarded
			setTourDirty();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	void actionCsvTimeSliceExport() {

		// get selected time slices
		final StructuredSelection selection = (StructuredSelection) _timeSlice_Viewer.getSelection();
		if (selection.size() == 0) {
			return;
		}

		/*
		 * get export filename
		 */
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_file_dialog_text);

		dialog.setFilterPath(_state.get(STATE_CSV_EXPORT_PATH));
		dialog.setFilterExtensions(new String[] { Util.CSV_FILE_EXTENSION });
		dialog.setFileName(
				net.tourbook.ui.UI.format_yyyymmdd_hhmmss(_tourData)
						+ UI.SYMBOL_DOT
						+ Util.CSV_FILE_EXTENSION);

		final String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			return;
		}

		final File exportFilePath = new Path(selectedFilePath).toFile();

		// keep export path
		_state.put(STATE_CSV_EXPORT_PATH, exportFilePath.getPath());

		if (exportFilePath.exists()) {
			if (net.tourbook.ui.UI.confirmOverwrite(exportFilePath) == false) {
				// don't overwrite file, nothing more to do
				return;
			}
		}

		/*
		 * write time slices into csv file
		 */
		Writer exportWriter = null;
		try {

			exportWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(selectedFilePath), UI.UTF_8));
			final StringBuilder sb = new StringBuilder();

			writeCSVHeader(exportWriter, sb);

			for (final Object selectedItem : selection.toArray()) {

				final int serieIndex = ((TimeSlice) selectedItem).serieIndex;

				// truncate buffer
				sb.setLength(0);

				// no.
				sb.append(Integer.toString(serieIndex + 0));
				sb.append(UI.TAB);

				// time hh:mm:ss
				if (_serieTime != null) {
					sb.append(UI.format_hh_mm_ss(_serieTime[serieIndex]));
				}
				sb.append(UI.TAB);

				// time in seconds
				if (_serieTime != null) {
					sb.append(Integer.toString(_serieTime[serieIndex]));
				}
				sb.append(UI.TAB);

				// distance
				if (_serieDistance != null) {
					sb.append(_nf6.format(_serieDistance[serieIndex] / 1000 / _unitValueDistance));
				}
				sb.append(UI.TAB);

				// altitude
				if (_serieAltitude != null) {
					sb.append(_nf3.format(_serieAltitude[serieIndex] / _unitValueAltitude));
				}
				sb.append(UI.TAB);

				// gradient
				if (_serieGradient != null) {
					sb.append(_nf3.format(_serieGradient[serieIndex]));
				}
				sb.append(UI.TAB);

				// pulse
				if (_seriePulse != null) {
					sb.append(_nf3.format(_seriePulse[serieIndex]));
				}
				sb.append(UI.TAB);

				// marker
				final TourMarker tourMarker = _markerMap.get(serieIndex);
				if (tourMarker != null) {
					sb.append(tourMarker.getLabel());
				}
				sb.append(UI.TAB);

				// temperature
				if (_serieTemperature != null) {

					final float temperature = UI.convertTemperatureFromMetric(_serieTemperature[serieIndex]);

					sb.append(_nf3.format(temperature));
				}
				sb.append(UI.TAB);

				// cadence
				if (_serieCadence != null) {
					sb.append(_nf3.format(_serieCadence[serieIndex]));
				}
				sb.append(UI.TAB);

				// speed
				if (_serieSpeed != null) {
					sb.append(_nf3.format(_serieSpeed[serieIndex]));
				}
				sb.append(UI.TAB);

				// pace
				if (_seriePace != null) {
					sb.append(UI.format_hhh_mm_ss((long) _seriePace[serieIndex]));
				}
				sb.append(UI.TAB);

				// power
				if (_seriePower != null) {
					sb.append(_nf3.format(_seriePower[serieIndex]));
				}
				sb.append(UI.TAB);

				// longitude
				if (_serieLongitude != null) {
					sb.append(Double.toString(_serieLongitude[serieIndex]));
				}
				sb.append(UI.TAB);

				// latitude
				if (_serieLatitude != null) {
					sb.append(Double.toString(_serieLatitude[serieIndex]));
				}
				sb.append(UI.TAB);

				// break time
				if (_serieBreakTime != null) {
					sb.append(_serieBreakTime[serieIndex] ? 1 : 0);
				}
				sb.append(UI.TAB);

				// end of line
				sb.append(net.tourbook.ui.UI.SYSTEM_NEW_LINE);
				exportWriter.write(sb.toString());
			}

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {

			if (exportWriter != null) {
				try {
					exportWriter.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void actionDeleteDistanceValues() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.TourEditor_Dialog_DeleteDistanceValues_Title,
				NLS.bind(Messages.TourEditor_Dialog_DeleteDistanceValues_Message, UI.UNIT_LABEL_DISTANCE)) == false) {
			return;
		}

		_tourData.distanceSerie = null;

		// reset distance in markers
		final Set<TourMarker> allTourMarker = _tourData.getTourMarkers();
		if (allTourMarker != null) {

			for (final TourMarker tourMarker : allTourMarker) {
				tourMarker.setDistance(0);
			}
		}

		updateUI_AfterDistanceModifications();
	}

	/**
	 * delete selected time slices
	 *
	 * @param isRemoveTime
	 */
	void actionDeleteTimeSlices(final boolean isRemoveTime) {

		// a tour with reference tours is currently not supported
		if (_isReferenceTourAvailable) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_delete_rows_title,
					Messages.tour_editor_dlg_delete_rows_message);

			return;
		}

		// swimming data series have a different number of time slices
		if (_tourData.swim_Time != null) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.Tour_Editor_Dialog_DeleteSwimTimeSlices_Title,
					Messages.Tour_Editor_Dialog_DeleteSwimTimeSlices_Message);
			return;
		}

		if (isRowSelectionMode() == false) {
			return;
		}

		// get selected time slices
		final StructuredSelection selection = (StructuredSelection) _timeSlice_Viewer.getSelection();
		if (selection.size() == 0) {
			return;
		}

		final Object[] selectedTimeSlices = selection.toArray();

		/*
		 * check if time slices have a successive selection
		 */
		int lastIndex = -1;
		int firstIndex = -1;

		for (final Object selectedItem : selectedTimeSlices) {

			final TimeSlice timeSlice = (TimeSlice) selectedItem;

			if (lastIndex == -1) {

				// first slice

				firstIndex = lastIndex = timeSlice.serieIndex;

			} else {

				// 2...n slices

				if (lastIndex - timeSlice.serieIndex == -1) {

					// successive selection

					lastIndex = timeSlice.serieIndex;

				} else {

					MessageDialog.openInformation(
							Display.getCurrent().getActiveShell(),
							Messages.tour_editor_dlg_delete_rows_title,
							Messages.tour_editor_dlg_delete_rows_not_successive);
					return;
				}
			}
		}

		// check if markers are within the selection
		if (canDeleteMarkers(firstIndex, lastIndex) == false) {
			return;
		}

		/*
		 * get first selection index to select a time slice after removal
		 */
		final Table table = (Table) _timeSlice_Viewer.getControl();
		final int[] indices = table.getSelectionIndices();
		Arrays.sort(indices);
		int lastSelectionIndex = indices[0];

		TourManager.removeTimeSlices(_tourData, firstIndex, lastIndex, isRemoveTime);

		getDataSeriesFromTourData();

		// update UI
		updateUI_Tab_1_Tour();
		updateUI_ReferenceTourRanges();

		// update slice viewer
		_timeSlice_ViewerItems = getRemainingSliceItems(_timeSlice_ViewerItems, firstIndex, lastIndex);

		_timeSlice_Viewer.getControl().setRedraw(false);
		{
			// update viewer
			_timeSlice_Viewer.remove(selectedTimeSlices);

			// update serie index label
			_timeSlice_Viewer.refresh(true);
		}
		_timeSlice_Viewer.getControl().setRedraw(true);

		setTourDirty();

		// notify other viewers
		fireModifyNotification();

		/*
		 * select next available time slice
		 */
		final int itemCount = table.getItemCount();
		if (itemCount > 0) {

			// adjust to array bounds
			lastSelectionIndex = Math.max(0, Math.min(lastSelectionIndex, itemCount - 1));

			table.setSelection(lastSelectionIndex);
			table.showSelection();

			// fire selection position
			_timeSlice_Viewer.setSelection(_timeSlice_Viewer.getSelection());
		}
	}

	void actionSaveTour() {

		// action is enabled when the tour is modified

		saveTourIntoDB();
	}

	void actionSetStartDistanceTo_0000() {

		// it is already checked if a valid data serie is available and first distance is > 0

		final float[] distanceSerie = _tourData.distanceSerie;
		final float distanceOffset = distanceSerie[0];

		// adjust distance data serie
		for (int serieIndex = 0; serieIndex < distanceSerie.length; serieIndex++) {
			final float sliceDistance = distanceSerie[serieIndex];
			distanceSerie[serieIndex] = sliceDistance - distanceOffset;
		}

		// adjust distance in markers
		final Set<TourMarker> allTourMarker = _tourData.getTourMarkers();
		if (allTourMarker != null) {

			for (final TourMarker tourMarker : allTourMarker) {
				final float markerDistance = tourMarker.getDistance();
				if (markerDistance > 0) {
					tourMarker.setDistance(markerDistance - distanceOffset);
				}
			}
		}

		updateUI_AfterDistanceModifications();
	}

	private void actionTimeZone_Remove() {

		_tourData.setTimeZoneId(null);

		// select default time zone
		_comboTimeZone.select(TimeTools.getTimeZoneIndex_Default());
		_isTimeZoneManuallyModified = false;

		updateModelFromUI();
		setTourDirty();

		updateUI_TimeZone();
	}

	private void actionTimeZone_SetDefault() {

		// select default time zone
		_comboTimeZone.select(TimeTools.getTimeZoneIndex_Default());
		_isTimeZoneManuallyModified = true;

		updateModelFromUI();
		setTourDirty();

		updateUI_TimeZone();
	}

	private void actionTimeZone_SetFromGeo() {

		if (_tourData.latitudeSerie == null || _tourData.latitudeSerie.length == 0) {
			return;
		}

		// select time zone from geo position
		final double lat0 = _tourData.latitudeSerie[0];
		final double lat1 = _tourData.longitudeSerie[0];

		final int timeZoneIndex = TimeTools.getTimeZoneIndex(lat0, lat1);

		_comboTimeZone.select(timeZoneIndex);
		_isTimeZoneManuallyModified = true;

		updateModelFromUI();
		setTourDirty();

		updateUI_TimeZone();
	}

	void actionToggleReadEditMode() {

		_isEditMode = _actionToggleReadEditMode.isChecked();

		enableActions();
		enableControls();
	}

	void actionToggleRowSelectMode() {

		_isRowEditMode = _actionToggleRowSelectMode.isChecked();

		recreateViewer();
	}

	void actionUndoChanges() {

		if (confirmUndoChanges()) {
			discardModifications();
		}
	}

	private void addPartListener() {

		// set the part listener
		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourDataEditorView.this) {
					_postSelectionProvider.setSelection(new SelectionTourData(null, _tourData));
				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourDataEditorView.this) {
					TourManager.setTourDataEditor(null);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourDataEditorView.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourDataEditorView.this) {

					// when part is opened it also should be visible
					_isPartVisible = true;

					TourManager.setTourDataEditor(TourDataEditorView.this);
				}
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourDataEditorView.this) {

					_isPartVisible = true;

					Display.getCurrent().asyncExec(new Runnable() {
						@Override
						public void run() {
							updateUI_FromModelRunnable();
						}
					});
				}
			}
		};

		// register the listener in the page
		getSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				if (_tourData == null) {
					return;
				}

				final String property = event.getProperty();
				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					/*
					 * tour data could have been changed but the changes are not reflected in the data
					 * model, the model needs to be updated from the UI
					 */
					if (isTourValid()) {
						updateModelFromUI();
					} else {
						MessageDialog.openInformation(
								Display.getCurrent().getActiveShell(),
								Messages.tour_editor_dlg_discard_tour_title,
								Messages.tour_editor_dlg_discard_tour_message);
						discardModifications();
					}

					if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

						// measurement system has changed

						net.tourbook.ui.UI.updateUnits();

						/*
						 * It is possible that the unit values in the UI class have been updated before
						 * the model was saved, this can happen when another view called the method
						 * UI.updateUnits(). Because of this race condition, only the internal units are
						 * used to calculate values which depend on the measurement system
						 */
						updateInternalUnitValues();

						recreateViewer();

						updateUI_FromModel(_tourData, false, true);

					} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

						// reload tour data

						updateUI_FromModel(_tourData, false, true);
					}

				} else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					// display renamed person

					// updateUITab4Info(); do NOT work
					//
					// tour data must be reloaded
				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);

		/*
		 * Common preferences
		 */
		_prefChangeListenerCommon = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

					// reload tour data

					updateUI_FromModel(_tourData, false, true);
				}
			}
		};

		// register the listener
		_prefStoreCommon.addPropertyChangeListener(_prefChangeListenerCommon);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourDataEditorView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourDataEditorView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else {

					if (_tourData == null) {
						return;
					}

					final long tourDataEditorTourId = _tourData.getTourId();

					if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

						final TourEvent tourEvent = (TourEvent) eventData;
						final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

						if (modifiedTours == null) {
							return;
						}

						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == tourDataEditorTourId) {

								// update modified tour

								if (tourEvent.tourDataEditorSavedTour == _tourData) {

									/*
									 * nothing to do because the tour is already saved (it was not modified
									 * before) and the UI is already updated
									 */
									return;
								}

								if (tourEvent.isReverted) {
									setTourClean();
								} else {
									setTourDirty();
								}

								updateUI_FromModel(tourData, true, tourEvent.isReverted);

								// nothing more to do, the editor contains only one tour
								return;
							}
						}

						// removed old tour data from the selection provider
						_postSelectionProvider.clearSelection();

					} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

						updateUI_FromModel(_tourData, false, true);

					} else if (eventId == TourEventId.MARKER_SELECTION && eventData instanceof SelectionTourMarker) {

						// ensure that the tour is displayed
						onSelectionChanged((ISelection) eventData);

						final SelectionTourMarker tourMarkerSelection = (SelectionTourMarker) eventData;

						onSelectionChanged_TourMarker(tourMarkerSelection);

					} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

						clearEditorContent();

					} else if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

						updateUI_FromModel(_tourData, true, true);

					} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

						updateUI_FromModel(_tourData, true, true);

					} else if (eventId == TourEventId.UPDATE_UI) {

						// check if this tour data editor contains a tour which must be updated

						// update editor
						if (net.tourbook.ui.UI.containsTourId(eventData, tourDataEditorTourId) != null) {

							// reload tour data
							_tourData = TourManager.getInstance().getTourData(_tourData.getTourId());

							updateUI_FromModel(_tourData, false, true);
						}

					} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

						onSelectionChanged((ISelection) eventData);
					}
				}

			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void addTourSaveListener() {

		_tourSaveListener = new ITourSaveListener() {
			@Override
			public boolean saveTour() {

				boolean isTourSaved;

				_isSavingInProgress = true;
				{
					isTourSaved = saveTourWithValidation();
				}
				_isSavingInProgress = false;

				return isTourSaved;
			}
		};

		TourManager.getInstance().addTourSaveListener(_tourSaveListener);
	}

	/**
	 * Checks if a marker is within the selected time slices
	 *
	 * @param firstSliceIndex
	 * @param lastSliceIndex
	 * @return Returns <code>true</code> when the marker can be deleted or there is no marker <br>
	 *         Returns <code>false</code> when the marker can not be deleted.
	 */
	private boolean canDeleteMarkers(final int firstSliceIndex, final int lastSliceIndex) {

		final Integer[] markerSerieIndex = _markerMap.keySet().toArray(new Integer[_markerMap.size()]);

		for (final Integer markerIndex : markerSerieIndex) {

			if ((markerIndex >= firstSliceIndex) && (markerIndex <= lastSliceIndex)) {

				// there is a marker within the deleted time slices

				if (MessageDialog.openConfirm(
						Display.getCurrent().getActiveShell(),
						Messages.tour_editor_dlg_delete_marker_title,
						Messages.tour_editor_dlg_delete_marker_message)) {
					return true;
				} else {
					return false;
				}
			}
		}

		// marker is not in the selection
		return true;
	}

	private void clearEditorContent() {

		if ((_tourData != null) && _isTourDirty) {

			/*
			 * in this case, nothing is done because the method which fires the event
			 * TourEventId.CLEAR_DISPLAYED_TOUR is reponsible to use the correct TourData
			 */

		} else {

			_tourData = null;

			// set slice viewer dirty
			_timeSlice_ViewerTourId = -1;
			_swimSlice_ViewerTourId = -1;

			_postSelectionProvider.clearSelection();

			setTourClean();

			_pageBook.showPage(_page_NoTourData);
		}
	}

	private boolean confirmUndoChanges() {

		// check if confirmation is disabled
		if (_prefStore.getBoolean(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR)) {

			return true;

		} else {

			final MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(//
					Display.getCurrent().getActiveShell(), //
					Messages.tour_editor_dlg_revert_tour_title, // title
					Messages.tour_editor_dlg_revert_tour_message, // message
					Messages.tour_editor_dlg_revert_tour_toggle_message, // toggle message
					false, // toggle default state
					null,
					null);

			_prefStore.setValue(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR, dialog.getToggleState());

			return dialog.getReturnCode() == Window.OK;
		}
	}

	private void createActions() {

		_actionSaveTour = new ActionSaveTour(this);
		_actionCreateTour = new ActionCreateTour(this);
		_actionUndoChanges = new ActionUndoChanges(this);
		_actionDeleteDistanceValues = new ActionDeleteDistanceValues(this);
		_actionComputeDistanceValues = new ActionComputeDistanceValues(this);
		_actionToggleRowSelectMode = new ActionToggleRowSelectMode(this);
		_actionToggleReadEditMode = new ActionToggleReadEditMode(this);
		_actionSetStartDistanceTo_0 = new ActionSetStartDistanceTo0(this);

		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this, true);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, false);

		_actionDeleteTimeSlicesKeepTime = new ActionDeleteTimeSlicesKeepTime(this);
		_actionDeleteTimeSlicesRemoveTime = new ActionDeleteTimeSlicesRemoveTime(this);

		_actionCreateTourMarker = new ActionCreateTourMarker(this);
		_actionExportTour = new ActionExport(this);
		_actionCsvTimeSliceExport = new ActionCSVTimeSliceExport(this);
		_actionSplitTour = new ActionSplitTour(this);
		_actionExtractTour = new ActionExtractTour(this);

		_actionViewSettings = new ActionViewSettings();

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);

		_actionModify_TimeSliceColumns = new ActionModifyColumns(_timeSlice_TourViewer);
		_actionModify_SwimSliceColumns = new ActionModifyColumns(_swimSlice_TourViewer);

		_tagMenuMgr = new TagMenuManager(this, false);
	}

	private void createFieldListener() {

		_modifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				updateModelFromUI();
				setTourDirty();
			}
		};

		_mouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				Util.adjustSpinnerValueOnMouseScroll(event);

				updateModelFromUI();
				setTourDirty();

				updateUI_Time(event.widget);
			}
		};

		_selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				updateModelFromUI();
				setTourDirty();
			}
		};

		_keyListener = new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				onModifyContent();
			}
		};

		/*
		 * listener for tour date/time
		 */
		_dateTimeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				setTourDirty();

				updateUI_Title();

				onModifyContent();
			}
		};

		/*
		 * listener for recording/driving/paused time
		 */
		_tourTimeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				updateModelFromUI();
				setTourDirty();

				updateUI_Time(event.widget);
			}
		};

		_verifyFloatValue = new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent event) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				final Text widget = (Text) event.widget;
				final String valueText = widget.getText().trim();

				if (valueText.length() > 0) {
					try {

						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
						//
						// Float.parseFloat() ignores localized strings therefore the databinding converter is used
						// which provides also a good error message
						//
						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

						StringToNumberConverter.toFloat(true).convert(valueText);

						_messageManager.removeMessage(widget.getData(WIDGET_KEY), widget);

					} catch (final IllegalArgumentException e) {

						// wrong characters are entered, display an error message

						_messageManager.addMessage(
								widget.getData(WIDGET_KEY),
								e.getLocalizedMessage(),
								null,
								IMessageProvider.ERROR,
								widget);
					}
				}

				/*
				 * tour dirty must be set after validation because an error can occure which enables
				 * actions
				 */
				if (_isTourDirty) {
					/*
					 * when an error occured previously and is now solved, the save action must be
					 * enabled
					 */
					enableActions();
				} else {
					setTourDirty();
				}
			}
		};

		_verifyIntValue = new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent event) {

				if (_isSetField || _isSavingInProgress) {
					return;
				}

				final Text widget = (Text) event.widget;
				final String valueText = widget.getText().trim();

				if (valueText.length() > 0) {
					try {

						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
						//
						// Float.parseFloat() ignores localized strings therefore the databinding converter is used
						// which provides also a good error message
						//
						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

						StringToNumberConverter.toInteger(true).convert(valueText);

						_messageManager.removeMessage(widget.getData(WIDGET_KEY), widget);

					} catch (final IllegalArgumentException e) {

						// wrong characters are entered, display an error message

						_messageManager.addMessage(
								widget.getData(WIDGET_KEY),
								e.getLocalizedMessage(),
								null,
								IMessageProvider.ERROR,
								widget);
					}
				}

				/*
				 * tour dirty must be set after validation because an error can occure which enables
				 * actions
				 */
				if (_isTourDirty) {
					/*
					 * when an error occured previously and is now solved, the save action must be
					 * enabled
					 */
					enableActions();
				} else {
					setTourDirty();
				}
			}
		};
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		MenuManager menuMgr;

		/*
		 * tour type menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, TourDataEditorView.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourTypePrefs);
			}
		});

		// set menu for the tour type link
		_linkTourType.setMenu(menuMgr.createContextMenu(_linkTourType));

		/*
		 * tag menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final Set<TourTag> tourTags = _tourData.getTourTags();
				final boolean isTagInTour = tourTags.size() > 0;

				_tagMenuMgr.fillTagMenu(menuMgr);
				_tagMenuMgr.enableTagActions(true, isTagInTour, tourTags);
			}
		});

		// set menu for the tag item

		final Menu tagContextMenu = menuMgr.createContextMenu(_linkTag);
		tagContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {

				final Rectangle rect = _linkTag.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = _linkTag.getParent().toDisplay(pt);

				_tagMenuMgr.onShowMenu(menuEvent, _linkTag, pt, null);
			}
		});

		_linkTag.setMenu(tagContextMenu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		updateInternalUnitValues();

		// define columns for the time slice viewer
		_timeSlice_ColumnManager = new ColumnManager(_timeSlice_TourViewer, _stateTimeSlice);
		_timeSlice_ColumnManager.setIsCategoryAvailable(true);
		defineAllColumns_TimeSlices();

		// define columns for the swim slice viewer
		_swimSlice_ColumnManager = new ColumnManager(_swimSlice_TourViewer, _stateSwimSlice);
		_swimSlice_ColumnManager.setIsCategoryAvailable(true);
		defineAllColumns_SwimSlices();

		restoreState_BeforeUI();

		// must be set before the UI is created
		createFieldListener();

		createUI(parent);
		createMenus();
		createActions();

		fillToolbar();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();
		addTourSaveListener();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		restoreState_WithUI();

		_pageBook.showPage(_page_NoTourData);

		displaySelectedTour();
	}

	private Section createSection(final Composite parent,
											final FormToolkit tk,
											final String title,
											final boolean isGrabVertical,
											final boolean isExpandable) {

		final int style = isExpandable ? //
				Section.TWISTIE //
						| Section.TITLE_BAR
				: Section.TITLE_BAR;

		final Section section = tk.createSection(parent, style);

		section.setText(title);
		GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

		final Composite sectionContainer = tk.createComposite(section);
		section.setClient(sectionContainer);

		section.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(final ExpansionEvent e) {
				onExpandSection();
			}
		});

		return section;
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_page_NoTourData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

		_tk = new FormToolkit(parent.getDisplay());

		_page_EditorForm = _tk.createForm(_pageBook);
		MTFont.setHeaderFont(_page_EditorForm);
		_tk.decorateFormHeading(_page_EditorForm);

		_messageManager = new MessageManager(_page_EditorForm);

		final Composite formBody = _page_EditorForm.getBody();
		GridLayoutFactory.fillDefaults().applyTo(formBody);
		formBody.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		_tabFolder = new CTabFolder(formBody, SWT.FLAT | SWT.BOTTOM);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabFolder);

		_tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTab();
			}
		});
		{
			_tab_10_Tour = new CTabItem(_tabFolder, SWT.FLAT);
			_tab_10_Tour.setText(Messages.tour_editor_tabLabel_tour);
			_tab_10_Tour.setControl(createUI_Tab_10_Tour(_tabFolder));

			_tab_20_TimeSlices = new CTabItem(_tabFolder, SWT.FLAT);
			_tab_20_TimeSlices.setText(Messages.tour_editor_tabLabel_tour_data);
			_tab_20_TimeSlices.setControl(createUI_Tab_20_TimeSlices(_tabFolder));

			_tab_30_SwimSlices = new CTabItem(_tabFolder, SWT.FLAT);
			_tab_30_SwimSlices.setText(Messages.Tour_Editor_TabLabel_SwimSlices);
			_tab_30_SwimSlices.setControl(createUI_Tab_30_SwimSlices(_tabFolder));
		}
	}

	private Label createUI_LabelSeparator(final Composite parent) {

		return _tk.createLabel(parent, UI.EMPTY_STRING);
	}

	private void createUI_Section_110_Title(final Composite parent) {

		Label label;

		_sectionTitle = createSection(parent, _tk, Messages.tour_editor_section_tour, true, true);
		final Composite container = (Composite) _sectionTitle.getClient();
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * title
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_tour_title);
			_firstColumnControls.add(label);
			// combo: tour title with history
			_comboTitle = new Combo(container, SWT.BORDER | SWT.FLAT);
			_comboTitle.setText(UI.EMPTY_STRING);

			_tk.adapt(_comboTitle, true, false);

			GridDataFactory
					.fillDefaults()//
					.grab(true, false)
					.hint(_hintTextColumnWidth, SWT.DEFAULT)
					.applyTo(_comboTitle);

			_comboTitle.addKeyListener(_keyListener);
			_comboTitle.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(final ModifyEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					_isTitleModified = true;
					setTourDirty();
				}
			});

			// fill combobox
			TreeSet<String> arr = TourDatabase.getAllTourTitles();
			for (final String string : arr) {
				_comboTitle.add(string);
			}
			new AutocompleteComboInput(_comboTitle);

			/*
			 * description
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_description);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			_firstColumnControls.add(label);

			_txtDescription = _tk.createText(
					container,
					UI.EMPTY_STRING,
					SWT.BORDER //
							| SWT.WRAP
							| SWT.V_SCROLL
							| SWT.H_SCROLL//
			);

			int descLines = _prefStore.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
			descLines = descLines == 0 ? 5 : descLines;

			// description will grab all vertical space in the tour tab
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					//
					// SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
					//
					.hint(_hintTextColumnWidth, _pc.convertHeightInCharsToPixels(descLines))
					.applyTo(_txtDescription);

			_txtDescription.addModifyListener(_modifyListener);

			/*
			 * start location
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_start_location);
			_firstColumnControls.add(label);

			_comboLocation_Start = new Combo(container, SWT.BORDER | SWT.FLAT);
			_comboLocation_Start.setText(UI.EMPTY_STRING);

			_tk.adapt(_comboLocation_Start, true, false);

			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.hint(_hintTextColumnWidth, SWT.DEFAULT)
					.applyTo(_comboLocation_Start);

			_comboLocation_Start.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					_isLocationStartModified = true;
					setTourDirty();
				}
			});

			// fill combobox
			arr = TourDatabase.getAllTourPlaceStarts();
			for (final String string : arr) {
				if (string != null) {
					_comboLocation_Start.add(string);
				}
			}
			new AutocompleteComboInput(_comboLocation_Start);

			/*
			 * end location
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_end_location);
			_firstColumnControls.add(label);

			_comboLocation_End = new Combo(container, SWT.BORDER | SWT.FLAT);
			_comboLocation_End.setText(UI.EMPTY_STRING);

			_tk.adapt(_comboLocation_End, true, false);

			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.hint(_hintTextColumnWidth, SWT.DEFAULT)
					.applyTo(_comboLocation_End);

			_comboLocation_End.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					_isLocationEndModified = true;
					setTourDirty();
				}
			});

			// fill combobox
			arr = TourDatabase.getAllTourPlaceEnds();
			for (final String string : arr) {
				if (string != null) {
					_comboLocation_End.add(string);
				}
			}
			new AutocompleteComboInput(_comboLocation_End);
		}
	}

	private void createUI_Section_120_DateTime(final Composite parent) {

		_sectionDateTime = createSection(parent, _tk, Messages.tour_editor_section_date_time, false, true);

		final Composite container = (Composite) _sectionDateTime.getClient();
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.spacing(COLUMN_SPACING, 5)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_Section_122_DateTime_Col1(container);
			createUI_Section_123_DateTime_Col2(container);

			createUI_Section_129_DateTime_TimeZone(container);

			final Label label = createUI_LabelSeparator(container);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

			createUI_Section_127_DateTime_Col1(container);
			createUI_Section_128_DateTime_Col2(container);
		}
	}

	/**
	 * 1. column
	 */
	private void createUI_Section_122_DateTime_Col1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
		{
			/*
			 * date
			 */
			final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_date);
			_firstColumnControls.add(label);

			_dtTourDate = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourDate);
			_tk.adapt(_dtTourDate, true, false);
			_dtTourDate.addSelectionListener(_dateTimeListener);

			//////////////////////////////////////
			createUI_LabelSeparator(container);
		}
	}

	/**
	 * 2. column
	 */
	private void createUI_Section_123_DateTime_Col2(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * start time
				 */
				_lblStartTime = _tk.createLabel(container, Messages.tour_editor_label_start_time);
				_secondColumnControls.add(_lblStartTime);

				_dtStartTime = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
				_tk.adapt(_dtStartTime, true, false);
				_dtStartTime.addSelectionListener(_dateTimeListener);
			}
		}
	}

	private void createUI_Section_127_DateTime_Col1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
		{
			{
				/*
				 * tour distance
				 */
				final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_distance);
				_firstColumnControls.add(label);

				_txtDistance = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
				_txtDistance.addModifyListener(_verifyFloatValue);
				_txtDistance.setData(WIDGET_KEY, WIDGET_KEY_TOURDISTANCE);
				_txtDistance.addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(final KeyEvent e) {
						_isDistManuallyModified = true;
					}

					@Override
					public void keyReleased(final KeyEvent e) {}
				});
				GridDataFactory.fillDefaults().hint(_hintValueFieldWidth, SWT.DEFAULT).applyTo(_txtDistance);

				_lblDistanceUnit = _tk.createLabel(container, UI.UNIT_LABEL_DISTANCE);
			}

			{
				/*
				 * altitude up
				 */
				final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_AltitudeUp);
				_firstColumnControls.add(label);

				_txtAltitudeUp = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
				_txtAltitudeUp.addModifyListener(_verifyIntValue);
				_txtAltitudeUp.setData(WIDGET_KEY, WIDGET_KEY_ALTITUDE_UP);
				_txtAltitudeUp.addKeyListener(new KeyListener() {

					@Override
					public void keyPressed(final KeyEvent e) {
						_isAltitudeManuallyModified = true;
					}

					@Override
					public void keyReleased(final KeyEvent e) {}
				});
				GridDataFactory.fillDefaults().hint(_hintValueFieldWidth, SWT.DEFAULT).applyTo(_txtAltitudeUp);

				_lblAltitudeUpUnit = _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);
			}

			{
				/*
				 * altitude down
				 */
				final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_AltitudeDown);
				_firstColumnControls.add(label);

				_txtAltitudeDown = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
				_txtAltitudeDown.addModifyListener(_verifyIntValue);
				_txtAltitudeDown.setData(WIDGET_KEY, WIDGET_KEY_ALTITUDE_DOWN);
				_txtAltitudeDown.addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(final KeyEvent e) {
						_isAltitudeManuallyModified = true;
					}

					@Override
					public void keyReleased(final KeyEvent e) {}
				});
				GridDataFactory.fillDefaults().hint(_hintValueFieldWidth, SWT.DEFAULT).applyTo(_txtAltitudeDown);

				_lblAltitudeDownUnit = _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);
			}
		}
	}

	/**
	 * 2. column
	 */
	private void createUI_Section_128_DateTime_Col2(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * recording time
				 */
				final Label label = _tk.createLabel(container, Messages.tour_editor_label_recording_time);
				_secondColumnControls.add(label);

				_timeRecording = new TimeDuration(container);
			}

			{
				/*
				 * paused time
				 */
				final Label label = _tk.createLabel(container, Messages.tour_editor_label_paused_time);
				_secondColumnControls.add(label);

				_timePaused = new TimeDuration(container);
			}

			{
				/*
				 * driving time
				 */
				final Label label = _tk.createLabel(container, Messages.tour_editor_label_driving_time);
				_secondColumnControls.add(label);

				_timeDriving = new TimeDuration(container);
			}
		}
	}

	private void createUI_Section_129_DateTime_TimeZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(false, false)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			/*
			 * Time zone
			 */

			{
				// label
				_lblTimeZone = _tk.createLabel(container, Messages.Tour_Editor_Label_TimeZone);
				_firstColumnControls.add(_lblTimeZone);
			}

			{
				// combo
				_comboTimeZone = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				_comboTimeZone.setVisibleItemCount(50);
				_comboTimeZone.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {

						_isTimeZoneManuallyModified = true;

						updateModelFromUI();
						setTourDirty();

						updateUI_TimeZone();
					}
				});

				_tk.adapt(_comboTimeZone, true, false);

				// fill combobox
				for (final TimeZoneData timeZone : TimeTools.getAllTimeZones()) {
					_comboTimeZone.add(timeZone.label);
				}

				/*
				 * Add decoration
				 */
				final Image infoImage = FieldDecorationRegistry
						.getDefault()
						.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
						.getImage();

				_decoTimeZone = new ControlDecoration(_comboTimeZone, SWT.TOP | SWT.LEFT);
				_decoTimeZone.hide();
				_decoTimeZone.setImage(infoImage);
				_decoTimeZone.setDescriptionText(Messages.Tour_Editor_Decorator_TimeZone_Tooltip);

				// indent the combo that the decorator is not truncated
				GridDataFactory
						.fillDefaults()//
						.indent(UI.DECORATOR_HORIZONTAL_INDENT, 0)
						.applyTo(_comboTimeZone);
			}

			{
				// spacer
				final Label label = createUI_LabelSeparator(container);
				_firstColumnControls.add(label);
			}

			{
				final Composite actionContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(false, false).applyTo(actionContainer);
				GridLayoutFactory.fillDefaults().numColumns(3).applyTo(actionContainer);
				{
					{
						// link: set default

						_linkDefaultTimeZone = new Link(actionContainer, SWT.NONE);
						_linkDefaultTimeZone.setText(Messages.Tour_Editor_Link_SetDefautTimeZone);
						_linkDefaultTimeZone.addSelectionListener(new SelectionAdapter() {

							@Override
							public void widgetSelected(final SelectionEvent e) {
								actionTimeZone_SetDefault();
							}
						});
						_tk.adapt(_linkDefaultTimeZone, true, true);
					}
					{
						// link: from geo

						_linkGeoTimeZone = new Link(actionContainer, SWT.NONE);
						_linkGeoTimeZone.setText(Messages.Tour_Editor_Link_SetGeoTimeZone);
						_linkGeoTimeZone.setToolTipText(Messages.Tour_Editor_Link_SetGeoTimeZone_Tooltip);
						_linkGeoTimeZone.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(final SelectionEvent e) {
								actionTimeZone_SetFromGeo();
							}
						});
						_tk.adapt(_linkGeoTimeZone, true, true);
					}
					{
						// link: remove

						_linkRemoveTimeZone = new Link(actionContainer, SWT.NONE);
						_linkRemoveTimeZone.setText(Messages.Tour_Editor_Link_RemoveTimeZone);
						_linkRemoveTimeZone.setToolTipText(Messages.Tour_Editor_Link_RemoveTimeZone_Tooltip);
						_linkRemoveTimeZone.addSelectionListener(new SelectionAdapter() {

							@Override
							public void widgetSelected(final SelectionEvent e) {
								actionTimeZone_Remove();
							}

						});
						_tk.adapt(_linkRemoveTimeZone, true, true);
					}
				}
			}
		}
	}

	private void createUI_Section_130_Personal(final Composite parent) {

		_sectionPersonal = createSection(parent, _tk, Messages.tour_editor_section_personal, false, true);
		final Composite container = (Composite) _sectionPersonal.getClient();
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.spacing(COLUMN_SPACING, 5)
				.applyTo(container);
		{
			createUI_Section_132_PersonalCol1(container);
			createUI_Section_134_PersonalCol2(container);
		}
	}

	/**
	 * 1. column
	 */
	private void createUI_Section_132_PersonalCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
		{
			{
				/*
				 * calories
				 */

				// label
				final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_calories);
				_firstColumnControls.add(label);

				// spinner
				_spinCalories = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_spinCalories);
				_spinCalories.setMinimum(0);
				_spinCalories.setMaximum(1_000_000_000);
				_spinCalories.setDigits(3);

				_spinCalories.addMouseWheelListener(_mouseWheelListener);
				_spinCalories.addSelectionListener(_selectionListener);

				// label: kcal
				_tk.createLabel(container, VALUE_UNIT_K_CALORIES);
			}

			{
				/*
				 * rest pulse
				 */
				// label: Rest pulse
				final Label label = _tk.createLabel(container, Messages.tour_editor_label_rest_pulse);
				label.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);
				_firstColumnControls.add(label);

				// spinner
				_spinRestPuls = new Spinner(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinRestPuls);
				_spinRestPuls.setMinimum(0);
				_spinRestPuls.setMaximum(200);
				_spinRestPuls.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);

				_spinRestPuls.addMouseWheelListener(_mouseWheelListener);
				_spinRestPuls.addSelectionListener(_selectionListener);

				// label: bpm
				_tk.createLabel(container, GRAPH_LABEL_HEARTBEAT_UNIT);
			}
		}
	}

	/**
	 * 2. column
	 */
	private void createUI_Section_134_PersonalCol2(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Body weight
				 */

				// label: Weight
				final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_BodyWeight);
				label.setToolTipText(Messages.Tour_Editor_Label_BodyWeight_Tooltip);
				_secondColumnControls.add(label);

				// spinner: weight
				_spinBodyWeight = new Spinner(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinBodyWeight);
				_spinBodyWeight.setDigits(1);
				_spinBodyWeight.setMinimum(0);
				_spinBodyWeight.setMaximum(3000); // 300.0 kg

				_spinBodyWeight.addMouseWheelListener(_mouseWheelListener);
				_spinBodyWeight.addSelectionListener(_selectionListener);

				// label: unit
				_tk.createLabel(container, UI.UNIT_WEIGHT_KG);
			}
			{
				/*
				 * FTP - Functional Threshold Power
				 */

				// label: FTP
				final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_FTP);
				label.setToolTipText(Messages.Tour_Editor_Label_FTP_Tooltip);
				_secondColumnControls.add(label);

				// spinner: FTP
				_spinFTP = new Spinner(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinFTP);
				_spinFTP.setMinimum(0);
				_spinFTP.setMaximum(10000);

				_spinFTP.addMouseWheelListener(_mouseWheelListener);

				// spacer
				_tk.createLabel(container, UI.EMPTY_STRING);
			}
		}
	}

	private void createUI_Section_140_Weather(final Composite parent) {

		_sectionWeather = createSection(parent, _tk, Messages.tour_editor_section_weather, false, true);
		final Composite container = (Composite) _sectionWeather.getClient();
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.spacing(COLUMN_SPACING, 5)
				.applyTo(container);
		{
			createUI_Section_141_Weather(container);
			createUI_Section_142_Weather(container);
			createUI_Section_144_WeatherCol1(container);
		}
	}

	private void createUI_Section_141_Weather(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * weather description
			 */
			final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Weather);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			_firstColumnControls.add(label);

			_txtWeather = _tk.createText(
					container, //
					UI.EMPTY_STRING,
					SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL//
			);
			_txtWeather.addModifyListener(_modifyListener);

			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					//
					// SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
					//
					.hint(_hintTextColumnWidth, _pc.convertHeightInCharsToPixels(2))
					.applyTo(_txtWeather);
		}
	}

	private void createUI_Section_142_Weather(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
		{

			/*
			 * wind speed
			 */

			// label
			Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
			label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
			_firstColumnControls.add(label);

			// spinner
			_spinWindSpeedValue = new Spinner(container, SWT.BORDER);
			GridDataFactory
					.fillDefaults()//
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinWindSpeedValue);
			_spinWindSpeedValue.setMinimum(0);
			_spinWindSpeedValue.setMaximum(120);
			_spinWindSpeedValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

			_spinWindSpeedValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedValue();
					setTourDirty();
				}
			});
			_spinWindSpeedValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedValue();
					setTourDirty();
				}
			});
			_spinWindSpeedValue.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedValue();
					setTourDirty();
				}
			});

			// label: km/h, mi/h
			_lblSpeedUnit = _tk.createLabel(container, UI.UNIT_LABEL_SPEED);

			// combo: wind speed with text
			_comboWindSpeedText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.indent(10, 0)
					.span(2, 1)
					.applyTo(_comboWindSpeedText);
			_tk.adapt(_comboWindSpeedText, true, false);
			_comboWindSpeedText.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
			_comboWindSpeedText.setVisibleItemCount(20);
			_comboWindSpeedText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedText();
					setTourDirty();
				}
			});

			/**
			 * this is not working correctly, the combo item is modified but an selection change event
			 * is not fired -> no updates are done :-((
			 */
//			_comboWindSpeedText.addMouseWheelListener(new MouseWheelListener() {
//				@Override
//				public void mouseScrolled(final MouseEvent event) {
//
//					final Combo combo = (Combo) event.widget;
//					final int itemCount = combo.getItemCount();
//
//					if (itemCount == 0) {
//						return;
//					}
//
//					// items are available
//
//					int selectedIndex = combo.getSelectionIndex();
//
//					// check if items are selected
//					if (selectedIndex == -1) {
//						// select first item
//						combo.select(0);
//						return;
//					}
//
//					if (event.count < 0) {
//
//						// select next item
//
//						if (selectedIndex < itemCount - 1) {
//							combo.select(++selectedIndex);
//						}
//					} else {
//
//						// select previous item
//
//						if (selectedIndex > 0) {
//							combo.select(--selectedIndex);
//						}
//					}
//				}
//			});

			// fill combobox
			for (final String speedText : IWeather.windSpeedText) {
				_comboWindSpeedText.add(speedText);
			}

			/*
			 * wind direction
			 */

			// label
			label = _tk.createLabel(container, Messages.tour_editor_label_wind_direction);
			label.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);
			_firstColumnControls.add(label);

			// combo: wind direction text
			_comboWindDirectionText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			_tk.adapt(_comboWindDirectionText, true, false);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_comboWindDirectionText);
			_comboWindDirectionText.setToolTipText(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
			_comboWindDirectionText.setVisibleItemCount(10);
			_comboWindDirectionText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindDirectionText();
					setTourDirty();
				}
			});

			// spacer
			new Label(container, SWT.NONE);

			// spinner: wind direction value
			_spinWindDirectionValue = new Spinner(container, SWT.BORDER);
			GridDataFactory
					.fillDefaults()//
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinWindDirectionValue);
			_spinWindDirectionValue.setMinimum(-1);
			_spinWindDirectionValue.setMaximum(360);
			_spinWindDirectionValue.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);

			_spinWindDirectionValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindDirectionValue();
					setTourDirty();
				}
			});
			_spinWindDirectionValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindDirectionValue();
					setTourDirty();
				}
			});
			_spinWindDirectionValue.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					onSelectWindDirectionValue();
					setTourDirty();
				}
			});

			// label: direction unit = degree
			_tk.createLabel(container, Messages.Tour_Editor_Label_WindDirection_Unit);

			// fill combobox
			for (final String windDirText : IWeather.windDirectionText) {
				_comboWindDirectionText.add(windDirText);
			}

		}
	}

	/**
	 * weather: 1. column
	 */
	private void createUI_Section_144_WeatherCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults()//
//				.spacing(2, 1)
				.numColumns(3)
				.applyTo(container);
//		_firstColumnContainerControls.add(container);
		{
			/*
			 * temperature
			 */

			// label
			Label label = _tk.createLabel(container, Messages.tour_editor_label_temperature);
			label.setToolTipText(Messages.tour_editor_label_temperature_Tooltip);
			_firstColumnControls.add(label);

			// spinner
			_spinTemperature = new Spinner(container, SWT.BORDER);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_spinTemperature);
			_spinTemperature.setToolTipText(Messages.tour_editor_label_temperature_Tooltip);

			// the min/max temperature has a large range because fahrenheit has bigger values than celcius
			_spinTemperature.setMinimum(-600);
			_spinTemperature.setMaximum(1500);

			_spinTemperature.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					_isTemperatureManuallyModified = true;
					setTourDirty();
				}
			});
			_spinTemperature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (_isSetDigits) {
						_isSetDigits = false;
						return;
					}

					if (_isSetField || _isSavingInProgress) {
						return;
					}
					_isTemperatureManuallyModified = true;
					setTourDirty();
				}
			});
			_spinTemperature.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isSetField || _isSavingInProgress) {
						return;
					}
					_isTemperatureManuallyModified = true;
					setTourDirty();
				}
			});

			// label: celcius, fahrenheit
			_lblTemperatureUnit = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

			/*
			 * clouds
			 */
			final Composite cloudContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(cloudContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(cloudContainer);
			{
				// label: clouds
				label = _tk.createLabel(cloudContainer, Messages.tour_editor_label_clouds);
				label.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);

				// icon: clouds
				_lblCloudIcon = new CLabel(cloudContainer, SWT.NONE);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.END, SWT.FILL)
						.grab(true, false)
						.applyTo(_lblCloudIcon);
			}
			_firstColumnControls.add(cloudContainer);

			// combo: clouds
			_comboClouds = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_comboClouds);
			_tk.adapt(_comboClouds, true, false);
			_comboClouds.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);
			_comboClouds.setVisibleItemCount(10);
			_comboClouds.addModifyListener(_modifyListener);
			_comboClouds.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					displayCloudIcon();
				}
			});

			// fill combobox
			for (final String cloudText : IWeather.cloudText) {
				_comboClouds.add(cloudText);
			}

			// force the icon to be displayed to ensure the width is correctly set when the size is computed
			_isSetField = true;
			{
				_comboClouds.select(0);
				displayCloudIcon();
			}
			_isSetField = false;
		}
	}

	private void createUI_Section_150_Characteristics(final Composite parent) {

		_sectionCharacteristics = createSection(parent, _tk, Messages.tour_editor_section_characteristics, false, true);
		final Composite container = (Composite) _sectionCharacteristics.getClient();
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * tags
				 */
				_linkTag = new Link(container, SWT.NONE);
				_linkTag.setText(Messages.tour_editor_label_tour_tag);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_linkTag);
				_linkTag.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						UI.openControlMenu(_linkTag);
					}
				});
				_tk.adapt(_linkTag, true, true);
				_firstColumnControls.add(_linkTag);

				_lblTags = _tk.createLabel(container, UI.EMPTY_STRING, SWT.WRAP);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						/*
						 * hint is necessary that the width is not expanded when the text is long
						 */
						.hint(2 * _hintTextColumnWidth, SWT.DEFAULT)
						.span(3, 1)
						.applyTo(_lblTags);
			}

			{
				/*
				 * tour type
				 */
				_linkTourType = new Link(container, SWT.NONE);
				_linkTourType.setText(Messages.tour_editor_label_tour_type);
				_linkTourType.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(final SelectionEvent e) {
						UI.openControlMenu(_linkTourType);
					}
				});
				_tk.adapt(_linkTourType, true, true);
				_firstColumnControls.add(_linkTourType);

				_lblTourType = new CLabel(container, SWT.NONE);
				GridDataFactory.swtDefaults()//
						.grab(true, false)
						.span(3, 1)
						.applyTo(_lblTourType);
			}

			{
				/*
				 * Cadence: rpm/spm
				 */

				// label
				final Label label = _tk.createLabel(container,
						Messages.Tour_Editor_Label_Cadence);
				label.setToolTipText(Messages.Tour_Editor_Label_Cadence_Tooltip);
				_firstColumnControls.add(label);

				final Composite radioContainer = new Composite(container, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(radioContainer);
				{
					// ratio: rpm
					_rdoCadence_Rpm = _tk.createButton(radioContainer, Messages.Tour_Editor_Radio_Cadence_Rpm, SWT.RADIO);
					_rdoCadence_Rpm.addSelectionListener(_selectionListener);

					// radio: spm
					_rdoCadence_Spm = _tk.createButton(radioContainer, Messages.Tour_Editor_Radio_Cadence_Spm, SWT.RADIO);
					_rdoCadence_Spm.addSelectionListener(_selectionListener);
				}
			}
		}
	}

	private void createUI_SectionSeparator(final Composite parent) {

		final Composite sep = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 5).applyTo(sep);
	}

	private Composite createUI_Tab_10_Tour(final Composite parent) {

		// scrolled container
		_tab1Container = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		_tab1Container.setExpandVertical(true);
		_tab1Container.setExpandHorizontal(true);
		_tab1Container.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeTab1();
			}
		});
		{
			_tourContainer = new Composite(_tab1Container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_tourContainer);
			_tk.adapt(_tourContainer);
			GridLayoutFactory.swtDefaults().applyTo(_tourContainer);
//			_tourContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));

			// set content for scrolled composite
			_tab1Container.setContent(_tourContainer);

			_tk.setBorderStyle(SWT.BORDER);
			{
				createUI_Section_110_Title(_tourContainer);
				createUI_SectionSeparator(_tourContainer);

				createUI_Section_120_DateTime(_tourContainer);
				createUI_SectionSeparator(_tourContainer);

				createUI_Section_130_Personal(_tourContainer);
				createUI_SectionSeparator(_tourContainer);

				createUI_Section_140_Weather(_tourContainer);
				createUI_SectionSeparator(_tourContainer);

				createUI_Section_150_Characteristics(_tourContainer);
			}
		}

		// with e4 the layouts are not yet set -> NPE's -> run async which worked
		parent.getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				// compute width for all controls and equalize column width for the different sections
				_tab1Container.layout(true, true);
				UI.setEqualizeColumWidths(_firstColumnControls);
				UI.setEqualizeColumWidths(_secondColumnControls);

				_tab1Container.layout(true, true);
				UI.setEqualizeColumWidths(_firstColumnContainerControls);

				/*
				 * Reduce width that the decorator is not truncated
				 */
				final GridData gd = (GridData) _lblTimeZone.getLayoutData();
				gd.widthHint -= UI.DECORATOR_HORIZONTAL_INDENT;
			}
		});

		return _tab1Container;
	}

	/**
	 * @param parent
	 * @return returns the controls for the tab
	 */
	private Control createUI_Tab_20_TimeSlices(final Composite parent) {

		_tab2_TimeSlice_Container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_tab2_TimeSlice_Container);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_tab2_TimeSlice_Container);
		{
			_timeSliceViewerContainer = new Composite(_tab2_TimeSlice_Container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_timeSliceViewerContainer);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_timeSliceViewerContainer);

			createUI_Tab_22_TimeSliceViewer(_timeSliceViewerContainer);

			_timeSlice_Label = new Label(_tab2_TimeSlice_Container, SWT.WRAP);
			_timeSlice_Label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			_timeSlice_Label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			_timeSlice_Label.setVisible(false);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_timeSlice_Label);
		}

		return _tab2_TimeSlice_Container;
	}

	/**
	 * @param parent
	 */
	private void createUI_Tab_22_TimeSliceViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);

		// header is disabled because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=536021
		// Table: right-aligned column header with own background color lacks margin
//		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		createUI_Tab_24_SliceViewerContextMenu(table);

//		table.addTraverseListener(new TraverseListener() {
//			public void keyTraversed(final TraverseEvent e) {
//				e.doit = e.keyCode != SWT.CR; // vetoes all CR traversals
//			}
//		});

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if ((_isEditMode == false) || (isTourInDb() == false)) {
					return;
				}

				if (e.keyCode == SWT.DEL) {
					actionDeleteTimeSlices(true);
				}
			}
		});

		_timeSlice_Viewer = new TableViewer(table);

		if (_isRowEditMode == false) {
			UI.setCellEditSupport(_timeSlice_Viewer);
		}

		/*
		 * create editing support after the viewer is created but before the columns are created.
		 */
		final TextCellEditor textCellEditor = new CellEditor_Text_Customized(_timeSlice_Viewer.getTable());

		_timeSlice_AltitudeEditingSupport = new SliceEditingSupport_Float(textCellEditor, _serieAltitude);
		_timeSlice_PulseEditingSupport = new SliceEditingSupport_Float(textCellEditor, _seriePulse);
		_timeSlice_TemperatureEditingSupport = new SliceEditingSupport_Float(textCellEditor, _serieTemperature);
		_timeSlice_CadenceEditingSupport = new SliceEditingSupport_Float(textCellEditor, _serieCadence);
		_timeSlice_LatitudeEditingSupport = new SliceEditingSupport_Double(textCellEditor, _serieLatitude);
		_timeSlice_LongitudeEditingSupport = new SliceEditingSupport_Double(textCellEditor, _serieLongitude);

		_timeSlice_ColDef_Altitude.setEditingSupport(_timeSlice_AltitudeEditingSupport);
		_timeSlice_ColDef_Pulse.setEditingSupport(_timeSlice_PulseEditingSupport);
		_timeSlice_ColDef_Temperature.setEditingSupport(_timeSlice_TemperatureEditingSupport);
		_timeSlice_ColDef_Cadence.setEditingSupport(_timeSlice_CadenceEditingSupport);
		_timeSlice_ColDef_Latitude.setEditingSupport(_timeSlice_LatitudeEditingSupport);
		_timeSlice_ColDef_Longitude.setEditingSupport(_timeSlice_LongitudeEditingSupport);

		_timeSlice_ColumnManager.createColumns(_timeSlice_Viewer);

		_timeSlice_Viewer.setContentProvider(new TimeSlice_ViewerContentProvider());
		_timeSlice_Viewer.setUseHashlookup(true);

		_timeSlice_Viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		// hide first column, this is a hack to align the "first" visible column to right
		table.getColumn(0).setWidth(0);
	}

	private void createUI_Tab_24_SliceViewerContextMenu(final Table table) {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillTimeSlice_ContextMenu(manager);
			}
		});

		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		_timeSlice_ColumnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	private Control createUI_Tab_30_SwimSlices(final Composite parent) {

		_pageBook_Swim = new PageBook(parent, SWT.NONE);

		_pageSwim_NoData = UI.createUI_PageNoData(_pageBook_Swim, Messages.Tour_Editor_NoSwimData);

		_pageSwim_Data = new Composite(_pageBook_Swim, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageSwim_Data);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_pageSwim_Data);
		{
			_swimSliceViewerContainer = new Composite(_pageSwim_Data, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_swimSliceViewerContainer);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_swimSliceViewerContainer);

			createUI_Tab_32_SwimSliceViewer(_swimSliceViewerContainer);
		}

		return _pageBook_Swim;
	}

	private void createUI_Tab_32_SwimSliceViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);

		// header is disabled because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=536021
		// Table: right-aligned column header with own background color lacks margin
//		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		createUI_Tab_34_SwimSliceViewerContextMenu(table);

//		table.addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyPressed(final KeyEvent e) {
//
//				if ((_isEditMode == false) || (isTourInDb() == false)) {
//					return;
//				}
//
//				if (e.keyCode == SWT.DEL) {
//					actionDeleteTimeSlices(true);
//				}
//			}
//		});

		_swimSlice_Viewer = new TableViewer(table);

		if (_isRowEditMode == false) {
			UI.setCellEditSupport(_swimSlice_Viewer);
		}

		/*
		 * create editing support after the viewer is created but before the columns are created.
		 */
		final TextCellEditor textCellEditor = new CellEditor_Text_Customized(_swimSlice_Viewer.getTable());
		final ComboBoxCellEditor strokeStyleCellEditor = new CellEditor_ComboBox_Customized(_swimSlice_Viewer.getTable(),
				SwimStrokeText.getAllStrokeText());

		_swimSlice_CadenceEditingSupport = new SliceEditingSupport_Short(textCellEditor, _swimSerie_Cadence);
		_swimSlice_StrokesEditingSupport = new SliceEditingSupport_Short(textCellEditor, _swimSerie_Strokes);
		_swimSlice_StrokeStyleEditingSupport = new SliceEditingSupport_ComboBox(strokeStyleCellEditor, _swimSerie_StrokeStyle);

		_swimSlice_ColDef_Cadence.setEditingSupport(_swimSlice_CadenceEditingSupport);
		_swimSlice_ColDef_Strokes.setEditingSupport(_swimSlice_StrokesEditingSupport);
		_swimSlice_ColDef_StrokeStyle.setEditingSupport(_swimSlice_StrokeStyleEditingSupport);

		_swimSlice_ColumnManager.createColumns(_swimSlice_Viewer);

		_swimSlice_Viewer.setContentProvider(new SwimSlice_ViewerContentProvider());
		_swimSlice_Viewer.setUseHashlookup(true);

		_swimSlice_Viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		// hide first column, this is a hack to align the "first" visible column to right
		table.getColumn(0).setWidth(0);
	}

	private void createUI_Tab_34_SwimSliceViewerContextMenu(final Table table) {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
//				fillSliceContextMenu(manager);
			}
		});

		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		_swimSlice_ColumnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	private void defineAllColumns_SwimSlices() {

		defineColumn_SwimSlice_Data_1_First();
		defineColumn_SwimSlice_Data_Sequence();

		defineColumn_SwimSlice_Time_TimeInHHMMSSRelative();
		defineColumn_SwimSlice_Time_TimeOfDay();
		defineColumn_SwimSlice_Time_TimeInSeconds();
		defineColumn_SwimSlice_Time_TimeDiff();

		defineColumn_SwimSlice_Swim_Strokes();
		defineColumn_SwimSlice_Swim_Cadence();
		defineColumn_SwimSlice_Swim_StrokeStyle();
	}

	private void defineAllColumns_TimeSlices() {

		defineColumn_TimeSlice_Data_1_First();
		defineColumn_TimeSlice_Data_Sequence();

		defineColumn_TimeSlice_Time_TimeInHHMMSSRelative();
		defineColumn_TimeSlice_Time_TimeOfDay();
		defineColumn_TimeSlice_Time_TimeInSeconds();
		defineColumn_TimeSlice_Time_TimeDiff();
		defineColumn_TimeSlice_Time_BreakTime();

		defineColumn_TimeSlice_Motion_Distance();
		defineColumn_TimeSlice_Motion_Speed();
		defineColumn_TimeSlice_Motion_Pace();
		defineColumn_TimeSlice_Motion_Latitude();
		defineColumn_TimeSlice_Motion_Longitude();
		defineColumn_TimeSlice_Motion_DistanceDiff();
		defineColumn_TimeSlice_Motion_SpeedDiff();

		defineColumn_TimeSlice_Altitude_Altitude();
		defineColumn_TimeSlice_Altitude_Gradient();

		defineColumn_TimeSlice_Body_Pulse();

		defineColumn_TimeSlice_Tour_Marker();

		defineColumn_TimeSlice_Weather_Temperature();

		defineColumn_TimeSlice_Powertrain_Cadence();
		defineColumn_TimeSlice_Powertrain_GearRatio();
		defineColumn_TimeSlice_Powertrain_GearTeeth();

		defineColumn_TimeSlice_Power();
	}

	/**
	 * 1. column will be hidden because the alignment for the first column is always to the left
	 */
	private void defineColumn_SwimSlice_Data_1_First() {

		final ColumnDefinition colDef = TableColumnFactory.DATA_FIRST_COLUMN.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setHideColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {}
		});
	}

	/**
	 * column: #
	 */
	private void defineColumn_SwimSlice_Data_Sequence() {

		final ColumnDefinition colDef = TableColumnFactory.DATA_SEQUENCE.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int logIndex = ((SwimSlice) cell.getElement()).uniqueCreateIndex;

				// the UI shows the time slice number starting with 1 and not with 0
				cell.setText(Integer.toString(logIndex + 1));
				cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		});
	}

	private void defineColumn_SwimSlice_Swim_Cadence() {

		TableColumnDefinition colDef;

		_swimSlice_ColDef_Cadence = colDef = TableColumnFactory.SWIM__SWIM_CADENCE.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_swimSerie_Cadence != null) {

					final SwimSlice timeSlice = (SwimSlice) cell.getElement();
					final short value = _swimSerie_Cadence[timeSlice.serieIndex];

					if (value == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Short.toString(value));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	private void defineColumn_SwimSlice_Swim_Strokes() {

		final ColumnDefinition colDef;

		_swimSlice_ColDef_Strokes = colDef = TableColumnFactory.SWIM__SWIM_STROKES.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_swimSerie_Strokes != null) {

					final SwimSlice timeSlice = (SwimSlice) cell.getElement();
					final short value = _swimSerie_Strokes[timeSlice.serieIndex];

					if (value == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Short.toString(value));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	private void defineColumn_SwimSlice_Swim_StrokeStyle() {

		final ColumnDefinition colDef;

		_swimSlice_ColDef_StrokeStyle = colDef = TableColumnFactory.SWIM__SWIM_STROKE_STYLE.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_swimSerie_StrokeStyle != null) {

					final SwimSlice timeSlice = (SwimSlice) cell.getElement();
					final short value = _swimSerie_StrokeStyle[timeSlice.serieIndex];

					if (value == Short.MIN_VALUE) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(SwimStrokeText.getStringFromValue(SwimStroke.getByValue(value)));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time difference in seconds to previous slice
	 */
	private void defineColumn_SwimSlice_Time_TimeDiff() {

		final ColumnDefinition colDef = TableColumnFactory.SWIM__TIME_TOUR_TIME_DIFF.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_swimSerie_Time != null) {
					final SwimSlice timeSlice = (SwimSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;
					if (serieIndex == 0) {
						cell.setText(Integer.toString(0));
					} else {
						cell.setText(Integer.toString(_swimSerie_Time[serieIndex] - _swimSerie_Time[serieIndex - 1]));
					}
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time hh:mm:ss relative to tour start
	 */
	private void defineColumn_SwimSlice_Time_TimeInHHMMSSRelative() {

		final ColumnDefinition colDef = TableColumnFactory.SWIM__TIME_TOUR_TIME_HH_MM_SS.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int serieIndex = ((SwimSlice) cell.getElement()).serieIndex;
				if (_swimSerie_Time != null) {
					cell.setText(UI.format_hh_mm_ss(_swimSerie_Time[serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time in seconds
	 */
	private void defineColumn_SwimSlice_Time_TimeInSeconds() {

		final ColumnDefinition colDef = TableColumnFactory.SWIM__TIME_TOUR_TIME.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_swimSerie_Time != null) {
					final SwimSlice timeSlice = (SwimSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;
					cell.setText(Integer.toString(_swimSerie_Time[serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time of day in hh:mm:ss
	 */
	private void defineColumn_SwimSlice_Time_TimeOfDay() {

		final ColumnDefinition colDef = TableColumnFactory.SWIM__TIME_TOUR_TIME_OF_DAY_HH_MM_SS.createColumn(_swimSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_swimSerie_Time == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					final int serieIndex = ((SwimSlice) cell.getElement()).serieIndex;

					cell.setText(UI.format_hh_mm_ss(_tourStartDayTime + _swimSerie_Time[serieIndex]));
				}
			}
		});
	}

	/**
	 * column: altitude
	 */
	private void defineColumn_TimeSlice_Altitude_Altitude() {

		ColumnDefinition colDef;

		_timeSlice_ColDef_Altitude = colDef = TableColumnFactory.ALTITUDE_ALTITUDE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieAltitude != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(_nf1.format(_serieAltitude[timeSlice.serieIndex] / _unitValueAltitude));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: gradient
	 */
	private void defineColumn_TimeSlice_Altitude_Gradient() {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_GRADIENT.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieGradient != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final float value = _serieGradient[timeSlice.serieIndex];

					colDef.printDetailValue(cell, value);

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: pulse
	 */
	private void defineColumn_TimeSlice_Body_Pulse() {

		ColumnDefinition colDef;

		_timeSlice_ColDef_Pulse = colDef = TableColumnFactory.BODY_PULSE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.disableValueFormatter();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_seriePulse != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString((int) _seriePulse[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * 1. column will be hidden because the alignment for the first column is always to the left
	 */
	private void defineColumn_TimeSlice_Data_1_First() {

		final ColumnDefinition colDef = TableColumnFactory.DATA_FIRST_COLUMN.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setHideColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {}
		});
	}

	/**
	 * column: #
	 */
	private void defineColumn_TimeSlice_Data_Sequence() {

		final ColumnDefinition colDef = TableColumnFactory.DATA_SEQUENCE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;
				final int logIndex = ((TimeSlice) cell.getElement()).uniqueCreateIndex;

				// the UI shows the time slice number starting with 1 and not with 0
				cell.setText(Integer.toString(logIndex + 1));

				// mark reference tour with a different background color
				boolean isBgSet = false;

				if (_refTourRange != null) {
					for (final int[] oneRange : _refTourRange) {
						if ((serieIndex >= oneRange[0]) && (serieIndex <= oneRange[1])) {
							cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
							isBgSet = true;
							break;
						}
					}
				}

				if (isBgSet == false) {
					cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				}
			}
		});
	}

	/**
	 * column: distance
	 */
	private void defineColumn_TimeSlice_Motion_Distance() {

		final ColumnDefinition colDef = TableColumnFactory.MOTION_DISTANCE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.disableValueFormatter();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieDistance != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;

					final float distance = _serieDistance[serieIndex] / 1000 / _unitValueDistance;

					cell.setText(_nf3.format(distance));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: distance difference in seconds to previous slice
	 */
	private void defineColumn_TimeSlice_Motion_DistanceDiff() {

		final ColumnDefinition colDef = TableColumnFactory.MOTION_DISTANCE_DIFF.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieDistance != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;

					float distanceDiff;

					if (serieIndex == 0) {
						// first time slice can contain a distance, occured in .fit files
						distanceDiff = _serieDistance[0] / 1000 / _unitValueDistance;
					} else {

						final float distancePrevious = _serieDistance[serieIndex - 1] / 1000 / _unitValueDistance;
						final float distance = _serieDistance[serieIndex] / 1000 / _unitValueDistance;

						distanceDiff = distance - distancePrevious;
					}

					if (distanceDiff < 0.001) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(_nf3.format(distanceDiff));
					}
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: latitude
	 */
	private void defineColumn_TimeSlice_Motion_Latitude() {

		ColumnDefinition colDef;

		_timeSlice_ColDef_Latitude = colDef = TableColumnFactory.MOTION_LATITUDE.createColumn(_timeSlice_ColumnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieLatitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();

					final double latitude = _serieLatitude[timeSlice.serieIndex];
					final String valueText = _nfLatLon.format(latitude);

					cell.setText(valueText);

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: longitude
	 */
	private void defineColumn_TimeSlice_Motion_Longitude() {

		ColumnDefinition colDef;
		_timeSlice_ColDef_Longitude = colDef = TableColumnFactory.MOTION_LONGITUDE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieLongitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();

					final double longitude = _serieLongitude[timeSlice.serieIndex];
					final String valueText = _nfLatLon.format(longitude);

					cell.setText(valueText);

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: pace
	 */
	private void defineColumn_TimeSlice_Motion_Pace() {

		final ColumnDefinition colDef = TableColumnFactory.MOTION_PACE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_seriePace == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final long pace = (long) _seriePace[timeSlice.serieIndex];

					cell.setText(UI.format_mm_ss(pace));
				}
			}
		});
	}

	/**
	 * column: speed
	 */
	private void defineColumn_TimeSlice_Motion_Speed() {

		final ColumnDefinition colDef = TableColumnFactory.MOTION_SPEED.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieSpeed != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final float speed = _serieSpeed[timeSlice.serieIndex];

					cell.setText(_nf1.format(speed));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: speed diff
	 */
	private void defineColumn_TimeSlice_Motion_SpeedDiff() {

		final ColumnDefinition colDef = TableColumnFactory.MOTION_SPEED_DIFF.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieTime != null && _serieDistance != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;

					if (serieIndex == 0) {
						cell.setText(Integer.toString(0));
					} else {

						final float timeDiff = (_serieTime[serieIndex] - _serieTime[serieIndex - 1]);

						final float distancePrevious = _serieDistance[serieIndex - 1] / 1000 / _unitValueDistance;
						final float distance = _serieDistance[serieIndex] / 1000 / _unitValueDistance;

						final float distDiff = distance - distancePrevious;
						final float speed = timeDiff == 0 ? 0 : distDiff * 3600f / timeDiff;

						cell.setText(_nf1.format(speed));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: power
	 */
	private void defineColumn_TimeSlice_Power() {

		final ColumnDefinition colDef = TableColumnFactory.POWER.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_seriePower != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString((int) _seriePower[timeSlice.serieIndex]));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: cadence
	 */
	private void defineColumn_TimeSlice_Powertrain_Cadence() {

		ColumnDefinition colDef;

		_timeSlice_ColDef_Cadence = colDef = TableColumnFactory.POWERTRAIN_CADENCE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieCadence != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(_nf1.format(_serieCadence[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Gear ratio
	 */
	private void defineColumn_TimeSlice_Powertrain_GearRatio() {

		final ColumnDefinition colDef = TableColumnFactory.POWERTRAIN_GEAR_RATIO.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieGears == null) {

					cell.setText(UI.EMPTY_STRING);

				} else {

					final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;
					final float gearRatio = _serieGears[0][serieIndex];

					cell.setText(_nf2.format(gearRatio));
				}
			}
		});
	}

	/**
	 * Column: Gear teeth
	 */
	private void defineColumn_TimeSlice_Powertrain_GearTeeth() {

		final ColumnDefinition colDef = TableColumnFactory.POWERTRAIN_GEAR_TEETH.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieGears == null) {

					cell.setText(UI.EMPTY_STRING);

				} else {

					final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;

					final long frontTeeth = (long) _serieGears[1][serieIndex];
					final long rearTeeth = (long) _serieGears[2][serieIndex];

					cell.setText(String.format(TourManager.GEAR_TEETH_FORMAT, frontTeeth, rearTeeth));
				}
			}
		});
	}

	/**
	 * column: cadence
	 */
	private void defineColumn_TimeSlice_Time_BreakTime() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_BREAK_TIME.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieBreakTime != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(
							_serieBreakTime[timeSlice.serieIndex]
									? net.tourbook.ui.UI.BREAK_TIME_MARKER
									: UI.EMPTY_STRING);
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time difference in seconds to previous slice
	 */
	private void defineColumn_TimeSlice_Time_TimeDiff() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_TIME_DIFF.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieTime != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;
					if (serieIndex == 0) {
						cell.setText(Integer.toString(0));
					} else {
						cell.setText(Integer.toString(_serieTime[serieIndex] - _serieTime[serieIndex - 1]));
					}
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time hh:mm:ss relative to tour start
	 */
	private void defineColumn_TimeSlice_Time_TimeInHHMMSSRelative() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_TIME_HH_MM_SS.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;
				if (_serieTime != null) {
					cell.setText(UI.format_hh_mm_ss(_serieTime[serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time in seconds
	 */
	private void defineColumn_TimeSlice_Time_TimeInSeconds() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_TIME.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieTime != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;
					cell.setText(Integer.toString(_serieTime[serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: time of day in hh:mm:ss
	 */
	private void defineColumn_TimeSlice_Time_TimeOfDay() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_TIME_OF_DAY_HH_MM_SS.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieTime == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;

					cell.setText(UI.format_hh_mm_ss(_tourStartDayTime + _serieTime[serieIndex]));
				}
			}
		});
	}

	/**
	 * column: marker
	 */
	private void defineColumn_TimeSlice_Tour_Marker() {

		ColumnDefinition colDef;
		colDef = TableColumnFactory.TOUR_MARKER.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TimeSlice timeSlice = (TimeSlice) cell.getElement();

				final TourMarker tourMarker = _markerMap.get(timeSlice.serieIndex);
				if (tourMarker != null) {

					cell.setText(tourMarker.getLabel());

					if (tourMarker.getType() == ChartLabel.MARKER_TYPE_DEVICE) {
						cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: temperature
	 */
	private void defineColumn_TimeSlice_Weather_Temperature() {

		final ColumnDefinition colDef;
		_timeSlice_ColDef_Temperature = colDef = TableColumnFactory.WEATHER_TEMPERATURE.createColumn(_timeSlice_ColumnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieTemperature != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();

					final float value = UI.convertTemperatureFromMetric(_serieTemperature[timeSlice.serieIndex]);

					colDef.printDetailValue(cell, value);

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Discard modifications and fire revert event
	 */
	private void discardModifications() {

		setTourClean();

		_postSelectionProvider.clearSelection();
		_messageManager.removeAllMessages();

		_tourData = reloadTourData();

		updateUI_FromModel(_tourData, true, true);

		fireRevertNotification();

		// a manually created tour can not be reloaded, find a tour in the workbench
		if (_tourData == null) {
			displaySelectedTour();
		}
	}

	private void displayCloudIcon() {

		final int selectionIndex = _comboClouds.getSelectionIndex();

		final String cloudKey = IWeather.cloudIcon[selectionIndex];
		final Image cloundIcon = UI.IMAGE_REGISTRY.get(cloudKey);

		_lblCloudIcon.setImage(cloundIcon);
	}

	/**
	 * try to get tourdata from the last selection or from a tour provider
	 */
	private void displaySelectedTour() {

		// show tour from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {

			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {

					if (_pageBook.isDisposed()) {
						return;
					}

					/*
					 * check if tour is set from a selection provider
					 */
					if (_tourData != null) {
						return;
					}

					final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
					if ((selectedTours != null) && (selectedTours.size() > 0)) {

						// get first tour, this view shows only one tour
						displayTour(selectedTours.get(0));

						setTourClean();
					}
				}
			});
		}
	}

	private void displayTour(final Long tourId) {

		if (tourId == null) {
			return;
		}

		// don't reload the same tour
		if (_tourData != null) {
			if (_tourData.getTourId().equals(tourId)) {
				return;
			}
		}

		final TourData tourData = TourManager.getInstance().getTourData(tourId);
		if (tourData != null) {
			_tourChart = null;
			updateUI_FromModel(tourData, false, true);
		}
	}

	private void displayTour(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		// don't reload the same tour
		if (_tourData == tourData) {
			return;
		}

		_tourChart = null;
		updateUI_FromModel(tourData, true, true);
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(_postSelectionListener);
		page.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);
		_prefStoreCommon.removePropertyChangeListener(_prefChangeListenerCommon);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
		TourManager.getInstance().removeTourSaveListener(_tourSaveListener);

		if (_tk != null) {
			_tk.dispose();
		}

		_firstColumnControls.clear();
		_secondColumnControls.clear();
		_firstColumnContainerControls.clear();

		super.dispose();
	}

	/**
	 * saving is done in the {@link #promptToSaveOnClose()} method
	 */
	@Override
	public void doSave(final IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	private void enableActions() {

		final boolean isTourInDb = isTourInDb();
		final boolean isTourValid = isTourValid() && isTourInDb;
		final boolean isNotManualTour = _isManualTour == false;
		final boolean canEdit = _isEditMode && isTourInDb;

		// all actions are disabled when a cell editor is activated
		final boolean isCellEditorInactive = _isCellEditorActive == false;

		final CTabItem selectedTab = _tabFolder.getSelection();
		final boolean isTimeSlice_ViewerTab = selectedTab == _tab_20_TimeSlices;
		final boolean isSwimSlice_ViewerTab = selectedTab == _tab_30_SwimSlices;
		final boolean isTourData = _tourData != null;

		final boolean canUseTool = _isEditMode && isTourValid && (_isManualTour == false);

		// at least 2 positions are necessary to compute the distance
		final boolean isGeoAvailable = isTourData
				&& _tourData.latitudeSerie != null
				&& _tourData.latitudeSerie.length >= 2;

		final boolean isDistanceAvailable = isTourData //
				&& _tourData.distanceSerie != null
				&& _tourData.distanceSerie.length > 0;

		final boolean isDistanceLargerThan0 = isTourData //
				&& isDistanceAvailable
				&& _tourData.distanceSerie[0] > 0;
		/*
		 * tour can only be saved when it's already saved in the database,except manual tours
		 */
		_actionSaveTour.setEnabled(isCellEditorInactive && _isTourDirty && isTourValid);

		_actionCreateTour.setEnabled(isCellEditorInactive && !_isTourDirty);
		_actionUndoChanges.setEnabled(isCellEditorInactive && _isTourDirty);

		_actionOpenAdjustAltitudeDialog.setEnabled(isCellEditorInactive && canUseTool);
		_actionOpenMarkerDialog.setEnabled(isCellEditorInactive && canUseTool);

		_actionToggleRowSelectMode.setEnabled(
				isCellEditorInactive
						&& (isTimeSlice_ViewerTab || isSwimSlice_ViewerTab)
						&& isTourValid
						&& (_isManualTour == false));
		_actionToggleReadEditMode.setEnabled(isCellEditorInactive && isTourInDb);

		_actionModify_TimeSliceColumns.setEnabled(isCellEditorInactive && isTimeSlice_ViewerTab);
		_actionModify_SwimSliceColumns.setEnabled(isCellEditorInactive && isSwimSlice_ViewerTab);

		_actionSetStartDistanceTo_0.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isDistanceLargerThan0);
		_actionDeleteDistanceValues.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isDistanceAvailable);
		_actionComputeDistanceValues.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isGeoAvailable);
	}

	/**
	 * enable actions
	 */
	private void enableActions_TimeSlices() {

		final StructuredSelection sliceSelection = (StructuredSelection) _timeSlice_Viewer.getSelection();

		final int numberOfSelectedSlices = sliceSelection.size();

		final boolean isSliceSelected = numberOfSelectedSlices > 0;
		final boolean isOneSliceSelected = numberOfSelectedSlices == 1;
		final boolean isTourInDb = isTourInDb();

		// deleting time slices with swim data is very complex
		final boolean isNoSwimData = _isTourWithSwimData == false;

		// check if a marker can be created
		boolean canCreateMarker = false;
		if (isOneSliceSelected) {
			final TimeSlice oneTimeSlice = (TimeSlice) sliceSelection.getFirstElement();
			canCreateMarker = _markerMap.containsKey(oneTimeSlice.serieIndex) == false;
		}
		// get selected Marker
		TourMarker selectedMarker = null;
		for (final Iterator<?> iterator = sliceSelection.iterator(); iterator.hasNext();) {
			final TimeSlice timeSlice = (TimeSlice) iterator.next();
			if (_markerMap.containsKey(timeSlice.serieIndex)) {
				selectedMarker = _markerMap.get(timeSlice.serieIndex);
				break;
			}
		}

		_actionCreateTourMarker.setEnabled(_isEditMode && isTourInDb && isOneSliceSelected && canCreateMarker);
		_actionOpenMarkerDialog.setEnabled(_isEditMode && isTourInDb);

		// select marker
		_actionOpenMarkerDialog.setTourMarker(selectedMarker);

		_actionDeleteTimeSlicesRemoveTime.setEnabled(_isEditMode && isTourInDb && isSliceSelected && isNoSwimData);
		_actionDeleteTimeSlicesKeepTime.setEnabled(_isEditMode && isTourInDb && isSliceSelected && isNoSwimData);

		_actionExportTour.setEnabled(true);
		_actionCsvTimeSliceExport.setEnabled(isSliceSelected);

		_actionSplitTour.setEnabled(isOneSliceSelected);
		_actionExtractTour.setEnabled(numberOfSelectedSlices >= 2);

		// set start/end position into the actions
		if (isSliceSelected) {

			final Object[] selectedSliceArray = sliceSelection.toArray();
			final int lastSliceIndex = selectedSliceArray.length - 1;

			final TimeSlice firstSelectedTimeSlice = (TimeSlice) selectedSliceArray[0];
			final TimeSlice lastSelectedTimeSlice = (TimeSlice) selectedSliceArray[lastSliceIndex];

			final int firstSelectedSerieIndex = firstSelectedTimeSlice.serieIndex;
			final int lastSelectedSerieIndex = lastSelectedTimeSlice.serieIndex;

			_actionExportTour.setTourRange(firstSelectedSerieIndex, lastSelectedSerieIndex);

			_actionSplitTour.setTourRange(firstSelectedSerieIndex);
			_actionExtractTour.setTourRange(firstSelectedSerieIndex, lastSelectedSerieIndex);

			/*
			 * prevent that the first and last slice is selected for the split which causes errors
			 */
			final int numberOfAllSlices = _timeSlice_ViewerItems.length;

			final boolean isSplitValid = firstSelectedSerieIndex > 0 && firstSelectedSerieIndex < numberOfAllSlices - 1;

			_actionSplitTour.setEnabled(isOneSliceSelected && isSplitValid);
		}
	}

	/**
	 * Delay enable/disable actions.
	 * <p>
	 * When a user traverses the edit fields in a viewer the actions are enabled and disable which
	 * flickers the UI, therefor it is delayed.
	 */
	private void enableActionsDelayed() {

		_enableActionCounter++;

		final UIJob uiJob = new UIJob(UI.EMPTY_STRING) {

			final int __runnableCounter = _enableActionCounter;

			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {

				// check if view is not disposed
				if (_pageBook.isDisposed()) {
					return Status.OK_STATUS;
				}

				// check if a newer runnable was created
				if (__runnableCounter != _enableActionCounter) {
					return Status.OK_STATUS;
				}

				enableActions();

				return Status.OK_STATUS;
			}
		};

		uiJob.setSystem(true);
		uiJob.schedule(10);
	}

	private void enableControls() {

		final boolean canEdit = _isEditMode && isTourInDb();
		final boolean isManualAndEdit = _isManualTour && canEdit;
		final boolean isDeviceTour = _isManualTour == false;

		final float[] serieDistance = _tourData == null ? null : _tourData.distanceSerie;
		final boolean isDistanceSerie = serieDistance != null && serieDistance.length > 0;
		final boolean isGeoAvailable = _tourData != null
				&& _tourData.latitudeSerie != null
				&& _tourData.latitudeSerie.length > 0;

		_comboTitle.setEnabled(canEdit);
		_txtDescription.setEnabled(canEdit);

		_comboLocation_Start.setEnabled(canEdit);
		_comboLocation_End.setEnabled(canEdit);

		_txtWeather.setEnabled(canEdit);
		_spinTemperature.setEnabled(canEdit && (_tourData != null && _tourData.temperatureSerie == null));
		_comboClouds.setEnabled(canEdit);
		_spinWindDirectionValue.setEnabled(canEdit);

		_spinWindSpeedValue.setEnabled(canEdit);
		_comboWindDirectionText.setEnabled(canEdit);
		_comboWindSpeedText.setEnabled(canEdit);

		_rdoCadence_Rpm.setEnabled(canEdit);
		_rdoCadence_Spm.setEnabled(canEdit);

		_dtTourDate.setEnabled(canEdit);
		_dtStartTime.setEnabled(canEdit);
		_comboTimeZone.setEnabled(canEdit);
		_linkDefaultTimeZone.setEnabled(canEdit);
		_linkRemoveTimeZone.setEnabled(canEdit);
		_linkGeoTimeZone.setEnabled(canEdit && isGeoAvailable);

		_timeRecording.setEditMode(isManualAndEdit);
		_timePaused.setEditMode(isManualAndEdit);
		_timeDriving.setEditMode(isManualAndEdit);

		// distance can be edited when no distance time slices are available
		_txtDistance.setEnabled(canEdit && isDistanceSerie == false);
		_txtAltitudeUp.setEnabled(isManualAndEdit);
		_txtAltitudeDown.setEnabled(isManualAndEdit);

		// Personal
		_spinBodyWeight.setEnabled(canEdit);
		_spinFTP.setEnabled(canEdit);
		_spinRestPuls.setEnabled(canEdit);
		_spinCalories.setEnabled(canEdit);

		_linkTag.setEnabled(canEdit);
		_linkTourType.setEnabled(canEdit);

		_timeSlice_Viewer.getTable().setEnabled(isDeviceTour);
	}

	private void fillTimeSlice_ContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionCreateTourMarker);
		menuMgr.add(_actionOpenMarkerDialog);

		menuMgr.add(new Separator());
		menuMgr.add(_actionDeleteTimeSlicesRemoveTime);
		menuMgr.add(_actionDeleteTimeSlicesKeepTime);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSetStartDistanceTo_0);
		menuMgr.add(_actionDeleteDistanceValues);
		menuMgr.add(_actionComputeDistanceValues);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSplitTour);
		menuMgr.add(_actionExtractTour);
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionCsvTimeSliceExport);

		enableActions_TimeSlices();
	}

	private void fillToolbar() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionSaveTour);

		tbm.add(new Separator());
		tbm.add(_actionOpenMarkerDialog);
		tbm.add(_actionOpenAdjustAltitudeDialog);

		tbm.add(new Separator());
		tbm.add(_actionToggleReadEditMode);
		tbm.add(_actionToggleRowSelectMode);

		tbm.add(new Separator());
		tbm.add(_actionCreateTour);
		tbm.add(_actionViewSettings);

		tbm.update(true);

		/*
		 * fill toolbar view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionUndoChanges);
		menuMgr.add(new Separator());

		menuMgr.add(_actionModify_TimeSliceColumns);
		menuMgr.add(_actionModify_SwimSliceColumns);
	}

	/**
	 * fire notification for changed tour data
	 */
	private void fireModifyNotification() {

		final ArrayList<TourData> modifiedTour = new ArrayList<>();
		modifiedTour.add(_tourData);

		final TourEvent tourEvent = new TourEvent(modifiedTour);
		tourEvent.isTourModified = true;

		TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent, TourDataEditorView.this);
	}

	/**
	 * fire notification for the reverted tour data
	 */
	private void fireRevertNotification() {

		final TourEvent tourEvent = new TourEvent(_tourData);
		tourEvent.isReverted = true;

		TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent, TourDataEditorView.this);
	}

	/**
	 * Select the chart/map slider(s) according to the selected slices
	 *
	 * @return
	 */
	private void fireSliderPosition(final StructuredSelection selection) {

		final Object[] selectedSlices = selection.toArray();
		if ((selectedSlices == null) || (selectedSlices.length == 0)) {
			return;
		}

		if (_tourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if ((tourChart != null) && (tourChart.isDisposed() == false)) {
				_tourChart = tourChart;
			}
		}

		final int timeSerieSize = _tourData.timeSerie.length;

		final int numSelectedSlices = selectedSlices.length;
		final Object slice1 = selectedSlices[0];

		int serieIndex0 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
		int serieIndex1 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
		int serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

		if (numSelectedSlices == 1) {

			// One slice is selected

			if (slice1 instanceof SwimSlice) {

				/*
				 * position slider at the beginning of the slice so that each slice borders has an
				 * slider
				 */
				final int swimSerieIndex1 = ((SwimSlice) slice1).serieIndex;
				final int timeSerieIndex1 = getSerieIndexFromSwimTime(swimSerieIndex1);

				if (timeSerieIndex1 > 1) {

					final int swimSerieIndex0 = swimSerieIndex1 - 1;
					final int timeSerieIndex0 = getSerieIndexFromSwimTime(swimSerieIndex0);

					serieIndex0 = timeSerieIndex0;
				}

				serieIndex1 = timeSerieIndex1 == timeSerieSize - 1

						// keep slider at the end of the chart
						? timeSerieSize - 1

						// adjust slider to the same stroke/swolf value as the left slider
						: timeSerieIndex1 - 1;

				serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

			} else if (slice1 instanceof TimeSlice) {

				final int serieIndexFirst = ((TimeSlice) slice1).serieIndex;

				/*
				 * position slider at the beginning of the slice so that each slice borders has an
				 * slider
				 */
				serieIndex0 = serieIndexFirst > 0
						? serieIndexFirst - 1
						: SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

				serieIndex1 = serieIndexFirst;
				serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
			}

		} else if (numSelectedSlices > 1) {

			// Two or more slices are selected, set the 2 sliders to the first and last selected slices

			if (slice1 instanceof SwimSlice) {

				final int swimSerieIndex1 = ((SwimSlice) slice1).serieIndex;
				final int swimSerieIndex2 = ((SwimSlice) selectedSlices[numSelectedSlices - 1]).serieIndex;

				final int timeSerieIndex2 = getSerieIndexFromSwimTime(swimSerieIndex2);

				/*
				 * Position slider at the beginning of the first slice
				 */
//				serieIndex1 = timeSerieIndex1 > 0 ? timeSerieIndex1 - 1 : 0;

				serieIndex1 = 0;

				if (swimSerieIndex1 > 0) {
					final int timeSerieIndex1 = getSerieIndexFromSwimTime(swimSerieIndex1 - 1);
					serieIndex1 = timeSerieIndex1;
				}

				serieIndex2 = timeSerieIndex2 == timeSerieSize - 1

						// keep slider at the end of the chart
						? timeSerieSize - 1

						// adjust slider to the same stroke/swolf value as the left slider
						: timeSerieIndex2 - 1;

			} else if (slice1 instanceof TimeSlice) {

				final int serieIndexFirst = ((TimeSlice) slice1).serieIndex;

				/*
				 * Position slider at the beginning of the first slice
				 */
				serieIndex1 = serieIndexFirst > 0 ? serieIndexFirst - 1 : 0;
				serieIndex2 = ((TimeSlice) selectedSlices[numSelectedSlices - 1]).serieIndex;

			}
		}

		ISelection sliderSelection = null;
		if (serieIndex1 != -1) {

			if (_tourChart == null) {

				// chart is not available, fire a map position

				if ((_serieLatitude != null) && (_serieLatitude.length > 0)) {

					// map position is available

					sliderSelection = new SelectionMapPosition(_tourData, serieIndex1, serieIndex2, true);
				}

			} else {

				final SelectionChartXSliderPosition xSliderSelection = new SelectionChartXSliderPosition(
						_tourChart,
						serieIndex0,
						serieIndex1,
						serieIndex2);

				xSliderSelection.setCenterSliderPosition(true);

				sliderSelection = xSliderSelection;
			}

			if (sliderSelection != null) {
				_postSelectionProvider.setSelection(sliderSelection);
			}
		}
	}

	private void getDataSeriesFromTourData() {

		_serieTime = _tourData.timeSerie;

		_serieDistance = _tourData.distanceSerie;
		_serieAltitude = _tourData.altitudeSerie;

		_serieCadence = _tourData.getCadenceSerie();
		_serieGears = _tourData.getGears();
		_seriePulse = _tourData.pulseSerie;

		_serieLatitude = _tourData.latitudeSerie;
		_serieLongitude = _tourData.longitudeSerie;

		_serieBreakTime = _tourData.getBreakTimeSerie();

		_serieGradient = _tourData.getGradientSerie();
		_serieSpeed = _tourData.getSpeedSerie();
		_seriePace = _tourData.getPaceSerieSeconds();
		_seriePower = _tourData.getPowerSerie();

		_serieTemperature = _tourData.temperatureSerie;

		_swimSerie_Cadence = _tourData.swim_Cadence;
		_swimSerie_LengthType = _tourData.swim_LengthType;
		_swimSerie_Strokes = _tourData.swim_Strokes;
		_swimSerie_StrokeStyle = _tourData.swim_StrokeStyle;
		_swimSerie_Time = _tourData.swim_Time;

		_swimSlice_CadenceEditingSupport.setDataSerie(_swimSerie_Cadence);
		_swimSlice_StrokesEditingSupport.setDataSerie(_swimSerie_Strokes);
		_swimSlice_StrokeStyleEditingSupport.setDataSerie(_swimSerie_StrokeStyle);

		_timeSlice_AltitudeEditingSupport.setDataSerie(_serieAltitude);
		_timeSlice_TemperatureEditingSupport.setDataSerie(_serieTemperature);
		_timeSlice_PulseEditingSupport.setDataSerie(_seriePulse);
		_timeSlice_CadenceEditingSupport.setDataSerie(_serieCadence);
		_timeSlice_LatitudeEditingSupport.setDataSerie(_serieLatitude);
		_timeSlice_LongitudeEditingSupport.setDataSerie(_serieLongitude);

		final ZonedDateTime tourStartTime = _tourData.getTourStartTime();

		_tourStartDayTime = tourStartTime.get(ChronoField.SECOND_OF_DAY);

		if (_isManualTour == false) {

			// tour is imported

			if ((_serieTime == null) || (_serieTime.length == 0)) {
				_tourData.setTourRecordingTime(0);
			} else {
				_tourData.setTourRecordingTime(_serieTime[_serieTime.length - 1]);
			}
			_tourData.computeTourDrivingTime();

			if ((_serieDistance == null) || (_serieDistance.length == 0)) {
				// disabled because distance can be edited when distance serie is not available
				// _tourData.setTourDistance(0);
			} else {
				// have no idea why tour distance is set because it should be already set during the tour import
				_tourData.setTourDistance(_serieDistance[_serieDistance.length - 1]);
			}

			_tourData.computeComputedValues();
		}
	}

	/**
	 * Converts a string into a float value
	 *
	 * @param valueText
	 * @return Returns the float value for the parameter valueText, return <code>0</code>
	 * @throws IllegalArgumentException
	 */
	private float getFloatValue(String valueText) throws IllegalArgumentException {

		valueText = valueText.trim();
		if (valueText.length() == 0) {

			return 0;

		} else {

			final Object convertedValue = StringToNumberConverter.toFloat(true).convert(valueText);
			if (convertedValue instanceof Float) {
				return ((Float) convertedValue).floatValue();
			}
		}

		return 0;
	}

	private TimeSlice[] getRemainingSliceItems(	final Object[] dataViewerItems,
																final int firstIndex,
																final int lastIndex) {

		final int oldSerieLength = dataViewerItems.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final TimeSlice[] newViewerItems = new TimeSlice[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(dataViewerItems, lastIndex + 1, newViewerItems, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// get items from start, delete until the end
			System.arraycopy(dataViewerItems, 0, newViewerItems, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(dataViewerItems, 0, newViewerItems, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(dataViewerItems, lastIndex + 1, newViewerItems, firstIndex, copyLength);
		}

		// update serie index
		int serieIndex = 0;
		for (final TimeSlice timeSlice : newViewerItems) {
			timeSlice.serieIndex = serieIndex++;
		}

		return newViewerItems;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> tourDataList = new ArrayList<>();
		tourDataList.add(_tourData);

		return tourDataList;
	}

	/**
	 * Get time serie index from swim time
	 */
	private int getSerieIndexFromSwimTime(final int swimSerieIndex) {

		// check bounds
		if (swimSerieIndex < 0 || swimSerieIndex >= _swimSerie_Time.length) {
			return 0;
		}

		final int swimTime = _swimSerie_Time[swimSerieIndex];

		for (int serieIndex = 0; serieIndex < _serieTime.length; serieIndex++) {

			final int serieTime = _serieTime[serieIndex];

			if (serieTime >= swimTime) {
				return serieIndex;
			}
		}

		return 0;
	}

//	/**
//	 * Converts a string into a int value
//	 *
//	 * @param valueText
//	 * @return Returns the float value for the parameter valueText, return <code>0</code>
//	 * @throws IllegalArgumentException
//	 */
//	private int getIntValue(String valueText) throws IllegalArgumentException {
//
//		valueText = valueText.trim();
//		if (valueText.length() == 0) {
//
//			return 0;
//
//		} else {
//
//			final Object convertedValue = StringToNumberConverter.toInteger(true).convert(valueText);
//			if (convertedValue instanceof Integer) {
//				return ((Integer) convertedValue).intValue();
//			}
//		}
//
//		return 0;
//	}

	TableViewer getSliceViewer() {
		return _timeSlice_Viewer;
	}

	private SliceViewerItems getSliceViewerItems() {

		if ((_tourData == null) || (_tourData.timeSerie == null) || (_tourData.timeSerie.length == 0)) {
			return new SliceViewerItems();
		}

		getDataSeriesFromTourData();

		/*
		 * create viewer elements (time slices), each viewer item contains the index into the data
		 * series
		 */
		final TimeSlice[] timeSlice_ViewerItems = new TimeSlice[_tourData.timeSerie.length];
		for (int serieIndex = 0; serieIndex < timeSlice_ViewerItems.length; serieIndex++) {
			timeSlice_ViewerItems[serieIndex] = new TimeSlice(serieIndex);
		}

		Object[] swimSlice_ViewerItems = new Object[0];
		if (_swimSerie_Time != null) {

			swimSlice_ViewerItems = new SwimSlice[_swimSerie_Time.length];
			for (int serieIndex = 0; serieIndex < swimSlice_ViewerItems.length; serieIndex++) {
				swimSlice_ViewerItems[serieIndex] = new SwimSlice(serieIndex);
			}
		}

		return new SliceViewerItems(timeSlice_ViewerItems, swimSlice_ViewerItems);
	}

	/**
	 * @return Returns {@link TourData} for the tour in the tour data editor or <code>null</code>
	 *         when a tour is not in the tour data editor
	 */
	public TourData getTourData() {
		return _tourData;
	}

	private TourData getTourData(final Long tourId) {

		TourData tourData = TourManager.getInstance().getTourData(tourId);
		if (tourData == null) {

			// tour is not in the database, try to get it from the raw data manager

			final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
			tourData = rawData.get(tourId);
		}

		return tourData;
	}

	/**
	 * @return Returns the title of the active tour
	 */
	private String getTourTitle() {

		if (_tourData.isMultipleTours()) {

			return TourManager.getTourTitleMultiple(_tourData);

		} else {

			final ZonedDateTime tourDate = _tourData.getTourStartTime();

			return TourManager.getTourTitle(tourDate);
		}
	}

	private int getWindDirectionTextIndex(final int degreeDirection) {

		final float degree = (degreeDirection + 22.5f) / 45.0f;

		final int directionIndex = ((int) degree) % 8;

		return directionIndex;
	}

	private int getWindSpeedTextIndex(final int speed) {

		// set speed to max index value
		int speedValueIndex = _unitValueWindSpeed.length - 1;

		for (int speedIndex = 0; speedIndex < _unitValueWindSpeed.length; speedIndex++) {

			final int speedMaxValue = _unitValueWindSpeed[speedIndex];

			if (speed <= speedMaxValue) {
				speedValueIndex = speedIndex;
				break;
			}
		}

		return speedValueIndex;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_hintDefaultSpinnerWidth = _isLinux //
				? SWT.DEFAULT
				: _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);

		_hintValueFieldWidth = _pc.convertWidthInCharsToPixels(10);
	}

	private void invalidateSliceViewers() {

		final CTabItem selectedTab = _tabFolder.getSelection();
		if (selectedTab != _tab_20_TimeSlices) {

			_timeSlice_ViewerTourId = -1;

		} else if (selectedTab != _tab_30_SwimSlices) {

			_swimSlice_ViewerTourId = -1;
		}
	}

	/**
	 * @return Returns <code>true</code> when the data have been modified and not saved, returns
	 *         <code>false</code> when tour is not modified or {@link TourData} is <code>null</code>
	 */
	@Override
	public boolean isDirty() {

		if (_tourData == null) {
			return false;
		}

		return _isTourDirty;
	}

	/**
	 * @return Returns <code>true</code> when the tour should be discarded<br>
	 *         returns <code>false</code> when the tour is invalid but should be saved<br>
	 */
	private boolean isDiscardTour() {

		final MessageDialog dialog = new MessageDialog(
				Display.getCurrent().getActiveShell(),
				Messages.tour_editor_dlg_save_tour_title,
				null,
				NLS.bind(Messages.tour_editor_dlg_save_invalid_tour, TourManager.getTourDateFull(_tourData)),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
				1);

		final int result = dialog.open();
		if (result == 0) {

			// discard modifications

			return true;

		} else {

			// save modifications

			return false;
		}
	}

	/**
	 * check row/cell mode, row mode must be set, it works with the cell mode but can be confusing
	 * because multiple rows can be selected but they are not visible
	 *
	 * @return
	 */
	private boolean isRowSelectionMode() {

		if (_isRowEditMode == false) {
			final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_delete_rows_title,
					Messages.tour_editor_dlg_delete_rows_mode_message,
					Messages.tour_editor_dlg_delete_rows_mode_toggle_message,
					true,
					null,
					null);

			if (dialog.getToggleState()) {
				_actionToggleRowSelectMode.setChecked(true);
				actionToggleRowSelectMode();
			}

			return false;
		}

		return true;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		return isDirty();
	}

	/**
	 * @return Returns <code>true</code> when the tour is saved in the database or when a manual tour
	 *         is created which also contains a person.
	 */
	private boolean isTourInDb() {

		if (_tourData != null && _tourData.getTourPerson() != null) {
			return true;
		}

		return false;
	}

	/**
	 * Checks if tour has no errors
	 *
	 * @return Returns <code>true</code> when all data for the tour are valid, <code>false</code>
	 *         otherwise
	 */
	private boolean isTourValid() {

		if (_tourData == null) {
			return false;
		}

		if (_isTourDirty) {

			if (_tourData.isValidForSave() == false) {
				return false;
			}

			if (_tourData.getTourPerson() == null) {

				// tour is modified but not yet saved in the database

				_messageManager.addMessage(
						WIDGET_KEY_PERSON,
						Messages.tour_editor_message_person_is_required,
						null,
						IMessageProvider.ERROR);

			} else {
				_messageManager.removeMessage(WIDGET_KEY_PERSON);
			}

			// tour is valid when there are no error messages

			return _messageManager.getErrorMessageCount() == 0;

		} else {

			// tour is not dirty

			return true;
		}
	}

	private void onExpandSection() {

		onResizeTab1();

//				form.reflow(false);
	}

	private void onModifyContent() {

		if (_tourData == null) {
			return;
		}

		// update modified data
		updateModelFromUI();

		enableActions();

		fireModifyNotification();
	}

	private void onResizeTab1() {
		_tab1Container.setMinSize(_tourContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private void onSelectionChanged(final ISelection selection) {

		if (_isSavingInProgress) {
			return;
		}

		if (selection instanceof SelectionDeletedTours) {

			clearEditorContent();

			return;
		}

		// ensure that the tour manager contains the same tour data
		if ((_tourData != null) && _isTourDirty) {
			try {
				TourManager.checkTourData(_tourData, getTourData(_tourData.getTourId()));
			} catch (final MyTourbookException e) {
				e.printStackTrace();
			}
		}

		if (onSelectionChanged_isTourInSelection(selection)) {

			/*
			 * tour in the selection is already displayed or a tour is not in the selection
			 */

			if (_isInfoInTitle) {
				showDefaultTitle();
			}

			return;

		} else {

			// another tour is selected, show info

			if (_isTourDirty) {

				if (_isInfoInTitle == false) {

					/*
					 * show info only when it is not yet displayed, this is an optimization because
					 * setting the message causes an layout and this is EXTREMLY SLOW because of the bad
					 * date time controls
					 */

					// hide title
					_page_EditorForm.setText(UI.EMPTY_STRING);

					// show info
					_messageManager.addMessage(
							MESSAGE_KEY_ANOTHER_SELECTION,
							NLS.bind(Messages.tour_editor_message_show_another_tour, getTourTitle()),
							null,
							IMessageProvider.ERROR);

					_isInfoInTitle = true;
				}

				return;
			}
		}

		if (_isInfoInTitle) {
			showDefaultTitle();
		}

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();
			if (tourData == null) {
				_tourChart = null;
			} else {

				final TourChart tourChart = selectionTourData.getTourChart();

				_tourChart = tourChart;
				updateUI_FromModel(tourData, false, true);
			}

		} else if (selection instanceof SelectionTourId) {

			displayTour(((SelectionTourId) selection).getTourId());

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				displayTour(tourIds.get(0));
			}

		} else if (selection instanceof SelectionTourMarker) {

			displayTour(((SelectionTourMarker) selection).getTourData());

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				displayTour(refItem.getTourId());
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData == null) {

							_tourData = tourData;
							_tourChart = null;
							updateUI_FromModel(tourData, false, true);

						} else {

							if (_tourData.getTourId().equals(tourData.getTourId())) {

								// a new tour id is in the selection
								_tourData = tourData;
								_tourChart = null;
								updateUI_FromModel(tourData, false, true);
							}
						}
					}
				}
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {

				displayTour(((TVICatalogComparedTour) firstElement).getTourId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				displayTour(((TVICompareResultComparedTour) firstElement).getComparedTourData().getTourId());
			}
		}
	}

	/**
	 * Checks the selection if it contains the current tour, {@link #_selectionTourId} contains the
	 * tour id which is within the selection
	 *
	 * @param selection
	 * @return Returns <code>true</code> when the current tour is within the selection
	 */
	private boolean onSelectionChanged_isTourInSelection(final ISelection selection) {

		boolean isCurrentTourSelected = false;

		if (_tourData == null) {
			return false;
		}

		TourData selectedTourData = null;
		final long currentTourId = _tourData.getTourId();

		if (selection instanceof SelectionTourData) {

			final TourData tourData = ((SelectionTourData) selection).getTourData();
			if (tourData == null) {
				return false;
			}

			_selectionTourId = tourData.getTourId();

			if ((tourData != null) && (currentTourId == _selectionTourId)) {
				isCurrentTourSelected = true;
				selectedTourData = tourData;
			}

		} else if (selection instanceof SelectionTourId) {

			_selectionTourId = ((SelectionTourId) selection).getTourId();

			if (currentTourId == _selectionTourId) {
				isCurrentTourSelected = true;
			}

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {

				_selectionTourId = tourIds.get(0);

				if (currentTourId == _selectionTourId) {
					isCurrentTourSelected = true;
				}
			}

		} else if (selection instanceof SelectionTourMarker) {

			final TourData tourData = ((SelectionTourMarker) selection).getTourData();
			if (tourData == null) {
				return false;
			}

			_selectionTourId = tourData.getTourId();

			if ((tourData != null) && (currentTourId == _selectionTourId)) {
				isCurrentTourSelected = true;
				selectedTourData = tourData;
			}

		} else if (selection instanceof SelectionChartInfo) {

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
			final ChartDataModel chartDataModel = chartInfo.chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = getTourData((Long) tourId);
					if (tourData != null) {

						_selectionTourId = tourData.getTourId();
						if (currentTourId == _selectionTourId) {

							isCurrentTourSelected = true;
							selectedTourData = tourData;

							// select time slices
							selectTimeSlice(chartInfo);
						}
					}
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) selection;

			final Chart chart = xSliderPosition.getChart();
			if (chart != null) {

				final ChartDataModel chartDataModel = chart.getChartDataModel();
				if (chartDataModel != null) {

					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						final TourData tourData = getTourData((Long) tourId);
						if (tourData != null) {

							_selectionTourId = tourData.getTourId();
							if (currentTourId == _selectionTourId) {

								isCurrentTourSelected = true;
								selectedTourData = tourData;

								// select time slices
								selectTimeSlice(xSliderPosition);
							}
						}
					}
				}
			}

		} else if (selection instanceof SelectionTourMarker) {

			final SelectionTourMarker markerSelection = (SelectionTourMarker) selection;

			final ArrayList<TourMarker> tourMarker = markerSelection.getSelectedTourMarker();
			final int numberOfTourMarkers = tourMarker.size();

			int leftSliderValueIndex = 0;
			int rightSliderValueIndex = 0;

			if (numberOfTourMarkers == 1) {

				leftSliderValueIndex = tourMarker.get(0).getSerieIndex();
				rightSliderValueIndex = leftSliderValueIndex;

			} else if (numberOfTourMarkers > 1) {

				leftSliderValueIndex = tourMarker.get(0).getSerieIndex();
				rightSliderValueIndex = tourMarker.get(numberOfTourMarkers - 1).getSerieIndex();
			}

			selectTimeSlice_InViewer(leftSliderValueIndex, rightSliderValueIndex);

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {
				_selectionTourId = ((TVICatalogComparedTour) firstElement).getTourId();
				if (currentTourId == _selectionTourId) {
					isCurrentTourSelected = true;
				}

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final long comparedTourTourId = ((TVICompareResultComparedTour) firstElement)
						.getComparedTourData()
						.getTourId();

				_selectionTourId = comparedTourTourId;
				if (currentTourId == _selectionTourId) {
					isCurrentTourSelected = true;
				}
			}
		}

		if (selectedTourData != null) {
			try {
				TourManager.checkTourData(selectedTourData, _tourData);
			} catch (final MyTourbookException e) {
				StatusUtil.log("Selection:" + selection, e);//$NON-NLS-1$
			}
		}

		return isCurrentTourSelected;
	}

	private void onSelectionChanged_TourMarker(final SelectionTourMarker markerSelection) {

		final ArrayList<TourMarker> tourMarker = markerSelection.getSelectedTourMarker();
		final int numberOfTourMarkers = tourMarker.size();

		int leftSliderValueIndex = 0;
		int rightSliderValueIndex = 0;

		if (numberOfTourMarkers == 1) {

			leftSliderValueIndex = tourMarker.get(0).getSerieIndex();
			rightSliderValueIndex = leftSliderValueIndex;

		} else if (numberOfTourMarkers > 1) {

			leftSliderValueIndex = tourMarker.get(0).getSerieIndex();
			rightSliderValueIndex = tourMarker.get(numberOfTourMarkers - 1).getSerieIndex();
		}

		selectTimeSlice_InViewer(leftSliderValueIndex, rightSliderValueIndex);
	}

	private void onSelectTab() {

		final CTabItem selectedTab = _tabFolder.getSelection();

		if (selectedTab == _tab_20_TimeSlices) {

			if (_timeSlice_ViewerTourId == -1L) {

				// load viewer when this was not yet done
				_timeSlice_ViewerTourId = _tourData.getTourId();

				_timeSlice_TourViewer.reloadViewer();
				updateStatusLine();

				// run asynch because relaodViewer is also running asynch
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {

						if (_timeSlice_Viewer.getTable().isDisposed()) {
							return;
						}

						_timeSlice_Viewer.getTable().setFocus();
					}
				});
			}

		} else if (selectedTab == _tab_30_SwimSlices) {

			if (_swimSlice_ViewerTourId == -1L) {

				// load viewer when this was not yet done
				_swimSlice_ViewerTourId = _tourData.getTourId();

				_swimSlice_TourViewer.reloadViewer();
//				updateStatusLine();

				// run asynch because relaodViewer is also running asynch
				Display.getCurrent().asyncExec(new Runnable() {

					@Override
					public void run() {

						if (_swimSlice_Viewer.getTable().isDisposed()) {
							return;
						}

						_swimSlice_Viewer.getTable().setFocus();
					}

				});
			}

		}

		enableActions();

	}

	private void onSelectWindDirectionText() {

		// N=0=0  NE=1=45  E=2=90  SE=3=135  S=4=180  SW=5=225  W=6=270  NW=7=315
		final int selectedIndex = _comboWindDirectionText.getSelectionIndex();

		// get degree from selected direction

		final int degree = selectedIndex * 45;

		_spinWindDirectionValue.setSelection(degree);
	}

	private void onSelectWindDirectionValue() {

		int degree = _spinWindDirectionValue.getSelection();

		if (degree == -1) {
			degree = 359;
			_spinWindDirectionValue.setSelection(degree);
		}
		if (degree == 360) {
			degree = 0;
			_spinWindDirectionValue.setSelection(degree);
		}

		_comboWindDirectionText.select(getWindDirectionTextIndex(degree));

	}

	private void onSelectWindSpeedText() {

		_isWindSpeedManuallyModified = true;

		final int selectedIndex = _comboWindSpeedText.getSelectionIndex();
		final int speed = _unitValueWindSpeed[selectedIndex];

		final boolean isBackup = _isSetField;
		_isSetField = true;
		{
			_spinWindSpeedValue.setSelection(speed);
		}
		_isSetField = isBackup;
	}

	private void onSelectWindSpeedValue() {

		_isWindSpeedManuallyModified = true;

		final int windSpeed = _spinWindSpeedValue.getSelection();

		final boolean isBackup = _isSetField;
		_isSetField = true;
		{
			_comboWindSpeedText.select(getWindSpeedTextIndex(windSpeed));
		}
		_isSetField = isBackup;
	}

	/*
	 * this method is called when the application is shut down to save dirty tours or to cancel the
	 * shutdown
	 * @see org.eclipse.ui.ISaveablePart2#promptToSaveOnClose()
	 */
	@Override
	public int promptToSaveOnClose() {

		int returnCode;

		if (_isTourDirty == false) {
			returnCode = ISaveablePart2.NO;
		}

		_isSavingInProgress = true;
		{
			if (saveTourWithValidation()) {
				returnCode = ISaveablePart2.NO;
			} else {
				returnCode = ISaveablePart2.CANCEL;
			}
		}
		_isSavingInProgress = false;

		return returnCode;
	}

	private void recreateViewer() {

		/*
		 * Recreate time slice viewer
		 */
		_timeSlice_ColumnManager.saveState(_stateTimeSlice);
		_timeSlice_ColumnManager.clearColumns();

		defineAllColumns_TimeSlices();
		_timeSlice_Viewer = (TableViewer) _timeSlice_TourViewer.recreateViewer(_timeSlice_Viewer);

		/*
		 * Recreate swim slice viewer
		 */
		_swimSlice_ColumnManager.saveState(_stateSwimSlice);
		_swimSlice_ColumnManager.clearColumns();

		defineAllColumns_SwimSlices();
		_swimSlice_Viewer = (TableViewer) _swimSlice_TourViewer.recreateViewer(_swimSlice_Viewer);
	}

	private TourData reloadTourData() {

		if (_tourData.getTourPerson() == null) {

			// tour is not saved, reloading tour data is not possible

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_reload_data_title,
					Messages.tour_editor_dlg_reload_data_message);

			return _tourData;
		}

		final Long tourId = _tourData.getTourId();
		final TourManager tourManager = TourManager.getInstance();

		tourManager.removeTourFromCache(tourId);

		return tourManager.getTourDataFromDb(tourId);
	}

	private void restoreState_BeforeUI() {

		_isRowEditMode = _state.getBoolean(STATE_ROW_EDIT_MODE);
		_isEditMode = _state.getBoolean(STATE_IS_EDIT_MODE);

		_latLonDigits = Util.getStateInt(_state, STATE_LAT_LON_DIGITS, DEFAULT_LAT_LON_DIGITS);
		setup_LatLonDigits();
	}

	private void restoreState_WithUI() {

		// select tab
		try {

			int tabIndex = _state.getInt(STATE_SELECTED_TAB);

			if (tabIndex >= _tabFolder.getItemCount()) {
				tabIndex = 0;
			}

			_tabFolder.setSelection(tabIndex);

		} catch (final NumberFormatException e) {
			_tabFolder.setSelection(_tab_10_Tour);
		}

		_actionToggleRowSelectMode.setChecked(_isRowEditMode);
		_actionToggleReadEditMode.setChecked(_isEditMode);

		_actionSetStartDistanceTo_0.setText(
				NLS.bind(
						Messages.TourEditor_Action_SetStartDistanceTo0,
						UI.UNIT_LABEL_DISTANCE));

//		_advMenuAddTag.setAutoOpen(
//		_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN),
//		_prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY));

		// expand/collapse sections
		_sectionCharacteristics.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_CHARACTERISTICS, true));
		_sectionDateTime.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_DATE_TIME, true));
		_sectionPersonal.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_PERSONAL, true));
		_sectionTitle.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_TITLE, true));
		_sectionWeather.setExpanded(Util.getStateBoolean(_state, STATE_SECTION_WEATHER, true));
	}

	@PersistState
	private void saveState() {

		// selected tab
		_state.put(STATE_SELECTED_TAB, _tabFolder.getSelectionIndex());

		// row/column edit mode
		_state.put(STATE_IS_EDIT_MODE, _actionToggleReadEditMode.isChecked());
		_state.put(STATE_ROW_EDIT_MODE, _actionToggleRowSelectMode.isChecked());

		// viewer state
		_timeSlice_ColumnManager.saveState(_stateTimeSlice);
		_swimSlice_ColumnManager.saveState(_stateSwimSlice);

		// editor state
		_state.put(STATE_SECTION_CHARACTERISTICS, _sectionCharacteristics.isExpanded());
		_state.put(STATE_SECTION_DATE_TIME, _sectionDateTime.isExpanded());
		_state.put(STATE_SECTION_PERSONAL, _sectionPersonal.isExpanded());
		_state.put(STATE_SECTION_TITLE, _sectionTitle.isExpanded());
		_state.put(STATE_SECTION_WEATHER, _sectionWeather.isExpanded());
	}

	/**
	 * @param isConfirmSave
	 * @return Returns <code>true</code> when the tour was saved, <code>false</code> when the tour is
	 *         not saved but canceled
	 */
	private boolean saveTourConfirmation() {

		if (_isTourDirty == false) {
			return true;
		}

		// show the tour data editor
		try {
			getSite().getPage().showView(ID, null, IWorkbenchPage.VIEW_VISIBLE);
		} catch (final PartInitException e) {
			e.printStackTrace();
		}

		// confirm save/discard/cancel
		final int returnCode = new MessageDialog(
				Display.getCurrent().getActiveShell(),
				Messages.tour_editor_dlg_save_tour_title,
				null,
				NLS.bind(Messages.tour_editor_dlg_save_tour_message, TourManager.getTourDateFull(_tourData)),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL },
				0)//
						.open();

		if (returnCode == 0) {

			// button YES: save tour

			saveTourIntoDB();

			return true;

		} else if (returnCode == 1) {

			// button NO: discard modifications

			discardModifications();

			return true;

		} else {

			// button CANCEL / dialog is canceled: tour is not saved and not discarded

			return false;
		}
	}

	/**
	 * saves the tour when it is dirty, valid and confirmation is done
	 */
	private boolean saveTourIntoDB() {

		_isSavingInProgress = true;

		updateModelFromUI();

		_tourData.computeAltitudeUpDown();
		_tourData.computeTourDrivingTime();
		_tourData.computeComputedValues();

		/*
		 * saveTour will check the tour editor dirty state, but when the tour is saved the dirty flag
		 * can be set before to prevent an out of synch error
		 */
		_isTourDirty = false;

		_tourData = TourDatabase.saveTour(_tourData, true);

		updateMarkerMap();

		// refresh combos

		if (_isTitleModified) {

			_comboTitle.clearSelection();
			_comboTitle.removeAll();

			// fill combobox
			final TreeSet<String> arr = TourDatabase.getAllTourTitles();
			for (final String string : arr) {
				_comboTitle.add(string);
			}
			_comboTitle.update();
			_isTitleModified = false;
		}

		if (_isLocationStartModified) {

			_comboLocation_Start.clearSelection();
			_comboLocation_Start.removeAll();

			// fill combobox
			final TreeSet<String> arr = TourDatabase.getAllTourPlaceStarts();
			for (final String string : arr) {
				_comboLocation_Start.add(string);
			}
			_comboLocation_Start.update();
			_isLocationStartModified = false;
		}

		if (_isLocationEndModified) {

			_comboLocation_End.clearSelection();
			_comboLocation_End.removeAll();

			// fill combobox
			final TreeSet<String> arr = TourDatabase.getAllTourPlaceEnds();
			for (final String string : arr) {
				_comboLocation_End.add(string);
			}
			_comboLocation_End.update();
			_isLocationEndModified = false;
		}

		setTourClean();

		// notify all views which display the tour type
		TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(_tourData), TourDataEditorView.this);

		_isSavingInProgress = false;

		return true;
	}

	/**
	 * saves the tour in the {@link TourDataEditorView}
	 *
	 * @return Returns <code>true</code> when the tour is saved or <code>false</code> when the tour
	 *         could not saved because the user canceled saving
	 */
	private boolean saveTourWithValidation() {

		if (_tourData == null) {
			return true;
		}

		if (isTourValid()) {

			return saveTourConfirmation();

		} else {

			// tour is invalid

			if (isDiscardTour()) {

				// discard modifications
				discardModifications();

				return true;

			} else {

				/*
				 * tour is not saved because the tour is invalid and should not be discarded
				 */

				return false;
			}
		}
	}

	private void selectTimeSlice(final SelectionChartInfo chartInfo) {

		final Table table = (Table) _timeSlice_Viewer.getControl();
		final int itemCount = table.getItemCount();

		// adjust to array bounds
		int valueIndex = chartInfo.selectedSliderValuesIndex;
		valueIndex = Math.max(0, Math.min(valueIndex, itemCount - 1));

		table.setSelection(valueIndex);
		table.showSelection();

		// fire slider position
//		fDataViewer.setSelection(fDataViewer.getSelection());
	}

	private void selectTimeSlice(final SelectionChartXSliderPosition sliderPosition) {

		if (sliderPosition == null) {
			return;
		}

		int valueIndexStart = sliderPosition.getLeftSliderValueIndex();
		final int valueIndexEnd = sliderPosition.getRightSliderValueIndex();

		final boolean isAdjustStartIndex = sliderPosition.isAdjustStartIndex();

		if (isAdjustStartIndex && (valueIndexStart < valueIndexEnd && valueIndexStart != 0)) {
			valueIndexStart++;
		}

		selectTimeSlice_InViewer(valueIndexStart, valueIndexEnd);
	}

	/**
	 * @param valueIndexStart
	 *           Can be {@link SelectionChartXSliderPosition#IGNORE_SLIDER_POSITION} when this
	 *           position should not be set.
	 * @param valueIndexEnd
	 *           Can be {@link SelectionChartXSliderPosition#IGNORE_SLIDER_POSITION} when this
	 *           position should not be set.
	 */
	private void selectTimeSlice_InViewer(final int valueIndexStart, final int valueIndexEnd) {

		final Table table = (Table) _timeSlice_Viewer.getControl();
		final int itemCount = table.getItemCount();

		// adjust to array bounds
		final int checkedValueIndex1 = Math.max(0, Math.min(valueIndexStart, itemCount - 1));
		final int checkedValueIndex2 = Math.max(0, Math.min(valueIndexEnd, itemCount - 1));

		if ((valueIndexStart == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION)
				&& (valueIndexStart == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION)) {
			return;
		}

		if (valueIndexStart == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			table.setSelection(checkedValueIndex2);
		} else if (valueIndexEnd == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			table.setSelection(checkedValueIndex1);
		} else {
			table.setSelection(checkedValueIndex1, checkedValueIndex2);
		}

		table.showSelection();
	}

	@Override
	public void setFocus() {

// !!! disabled because the first field gets the focus !!!
//		fTabFolder.setFocus();

		_page_EditorForm.setFocus();
	}

	/**
	 * removes the dirty state from the tour editor, updates the save/undo actions and updates the
	 * part name
	 */
	private void setTourClean() {

		_isTourDirty = false;

		_isAltitudeManuallyModified = false;
		_isDistManuallyModified = false;
		_isTemperatureManuallyModified = false;
		_isTimeZoneManuallyModified = false;
		_isWindSpeedManuallyModified = false;

		enableActions();
		enableControls();

		_messageManager.removeAllMessages();

		showDefaultTitle();

		/*
		 * this is not an eclipse editor part but the property change must be fired to hide the "*"
		 * marker in the part name
		 */
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * Set {@link TourData} for the editor, when the editor is dirty, nothing is done, the calling
	 * method must check if the tour editor is dirty
	 *
	 * @param tourDataForEditor
	 */
	public void setTourData(final TourData tourDataForEditor) {

		if (_isTourDirty) {
			return;
		}

		_tourChart = null;

		_postSelectionProvider.clearSelection();

		updateUI_FromModel(tourDataForEditor, false, true);
	}

	/**
	 * sets the tour editor dirty, updates the save/undo actions and updates the part name
	 */
	private void setTourDirty() {

		if (_isTourDirty) {
			return;
		}

		_isTourDirty = true;

		enableActions();

		/*
		 * this is not an eclipse editor part but the property change must be fired to show the start
		 * "*" marker in the part name
		 */
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * Setup the geo position formatter.
	 */
	private void setup_LatLonDigits() {

		_nfLatLon.setMinimumFractionDigits(_latLonDigits);
		_nfLatLon.setMaximumFractionDigits(_latLonDigits);
	}

	/**
	 * show the default title in the editor
	 */
	private void showDefaultTitle() {

		_messageManager.removeMessage(MESSAGE_KEY_ANOTHER_SELECTION);
		updateUI_Title();

		_isInfoInTitle = false;
	}

	@Override
	public void toursAreModified(final ArrayList<TourData> modifiedTours) {

		if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

			// check if it's the correct tour
			if (_tourData == modifiedTours.get(0)) {

				// tour type or tags can have been changed within this dialog
				updateUI_TourTypeTags();

				setTourDirty();
			}
		}
	}

	private void updateInternalUnitValues() {

		_unitValueDistance = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		_unitValueAltitude = net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

		_unitValueWindSpeed = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == 1
				? IWeather.windSpeedKmh
				: IWeather.windSpeedMph;
	}

	/**
	 * converts {@link TourMarker} from {@link #_tourData} into the map {@link #_markerMap}
	 */
	void updateMarkerMap() {

		_markerMap.clear();

		final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();

		for (final TourMarker tourMarker : tourMarkers) {
			_markerMap.put(tourMarker.getSerieIndex(), tourMarker);
		}
	}

	/**
	 * Update {@link TourData} from the UI fields.
	 */
	private void updateModelFromUI() {

		if (_tourData == null) {
			return;
		}

		try {

			_tourData.setTourTitle(_comboTitle.getText());
			_tourData.setTourDescription(_txtDescription.getText());

			_tourData.setTourStartPlace(_comboLocation_Start.getText());
			_tourData.setTourEndPlace(_comboLocation_End.getText());

			_tourData.setBodyWeight((float) (_spinBodyWeight.getSelection() / 10.0));
			_tourData.setPower_FTP(_spinFTP.getSelection());
			_tourData.setCalories(_spinCalories.getSelection());
			_tourData.setRestPulse(_spinRestPuls.getSelection());

			_tourData.setCadenceMultiplier(_rdoCadence_Rpm.getSelection() ? 1.0f : 2.0f);

			_tourData.setWeather(_txtWeather.getText().trim());
			_tourData.setWeatherWindDir(_spinWindDirectionValue.getSelection());
			if (_isWindSpeedManuallyModified) {
				/*
				 * update the speed only when it was modified because when the measurement is changed
				 * when the tour is being modified then the computation of the speed value can cause
				 * rounding errors
				 */
				_tourData.setWeatherWindSpeed((int) (_spinWindSpeedValue.getSelection() * _unitValueDistance));
			}

			final int cloudIndex = _comboClouds.getSelectionIndex();
			String cloudValue = IWeather.cloudIcon[cloudIndex];
			if (cloudValue.equals(UI.IMAGE_EMPTY_16)) {
				// replace invalid cloud key
				cloudValue = UI.EMPTY_STRING;
			}
			_tourData.setWeatherClouds(cloudValue);

			if (_isTemperatureManuallyModified) {

				final float temperature = (float) _spinTemperature.getSelection() / 10;

				_tourData.setAvgTemperature(UI.convertTemperatureToMetric(temperature));
			}

			// set time zone BEFORE the time is set
			if (_isTimeZoneManuallyModified) {

				// set time zone ONLY when manually modified

				final int selectedTimeZoneIndex = _comboTimeZone.getSelectionIndex();
				final TimeZoneData timeZoneData = TimeTools.getTimeZone_ByIndex(selectedTimeZoneIndex);
				final String timeZoneId = timeZoneData.zoneId;

				_tourData.setTimeZoneId(timeZoneId);
			}

			final ZonedDateTime tourStartTime = ZonedDateTime.of(
					_dtTourDate.getYear(),
					_dtTourDate.getMonth() + 1,
					_dtTourDate.getDay(),
					_dtStartTime.getHours(),
					_dtStartTime.getMinutes(),
					_dtStartTime.getSeconds(),
					0,
					_tourData.getTimeZoneIdWithDefault());

			_tourData.setTourStartTime(tourStartTime);

			// distance
			if (_isDistManuallyModified) {
				/*
				 * update the distance only when it was modified because when the measurement is changed
				 * when the tour is being modified then the computation of the distance value can cause
				 * rounding errors
				 */
				final float distanceValue = getFloatValue(_txtDistance.getText()) * _unitValueDistance * 1000;
				_tourData.setTourDistance(distanceValue);
			}

			// altitude
			if (_isAltitudeManuallyModified) {

				/*
				 * update the altitude only when it was modified because when the measurement is changed
				 * and the tour is modified, the computation of the distance value can cause rounding
				 * errors
				 */

				float altitudeUpValue = getFloatValue(_txtAltitudeUp.getText());
				float altitudeDownValue = getFloatValue(_txtAltitudeDown.getText());

				if (_unitValueAltitude != 1) {

					// none metric measurement system

					// ensure float is used
					float noneMetricValue = altitudeUpValue;
					altitudeUpValue = Math.round(noneMetricValue * _unitValueAltitude);

					noneMetricValue = altitudeDownValue;
					altitudeDownValue = Math.round(noneMetricValue * _unitValueAltitude);
				}

				_tourData.setTourAltUp((int) altitudeUpValue);
				_tourData.setTourAltDown((int) altitudeDownValue);
			}

			// manual tour
			if (_isManualTour) {

				_tourData.setTourRecordingTime(_timeRecording.getTime());
				_tourData.setTourDrivingTime(_timeDriving.getTime());
			}

		} catch (final IllegalArgumentException e) {

			// this should not happen (but it happend when developing the tour data editor :-)
			//
			// wrong characters are entered, display an error message

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", e.getLocalizedMessage());//$NON-NLS-1$

			StatusUtil.log(e);
		}
	}

	private void updateStatusLine() {

		final boolean isVisible = _timeSlice_Label.isVisible();
		boolean setVisible = false;

		if (_isReferenceTourAvailable) {

			// tour contains reference tours

			_timeSlice_Label.setText(Messages.TourDataEditorView_tour_editor_status_tour_contains_ref_tour);
			setVisible = true;

		} else {

			_timeSlice_Label.setText(UI.EMPTY_STRING);
		}

		if (isVisible != setVisible) {

			// changes visibility

			_timeSlice_Label.setVisible(setVisible);

			_tab2_TimeSlice_Container.layout(true, true);
		}
	}

	/**
	 * Updates the UI from {@link TourData}, dirty flag is not set
	 *
	 * @param tourData
	 */
	public void updateUI(final TourData tourData) {

		updateUI_FromModel(tourData, true, true);
	}

	/**
	 * @param tourData
	 * @param isDirty
	 *           When <code>true</code>, the tour is set to be dirty
	 */
	public void updateUI(final TourData tourData, final boolean isDirty) {

		updateUI(tourData);

		if (isDirty) {
			setTourDirty();
		}
	}

	private void updateUI_AfterDistanceModifications() {

		updateUI_AfterSliceEdit();

		// update distance in the UI, this must be done after updateUI_AfterSliceEdit()
		updateUI_Tab_1_Tour();

		/*
		 * Set slice viewer dirty when the time slice tab is not selected -> slice viewer was not
		 * updated in updateUI_AfterSliceEdit()
		 */
		invalidateSliceViewers();
	}

	private void updateUI_AfterSliceEdit() {

		setTourDirty();

		_tourData.clearComputedSeries();
		getDataSeriesFromTourData();

		// refresh the whole viewer because the computed data series could have been changed and the edited cell needs to be updated
		ColumnViewer viewer = _timeSlice_TourViewer.getViewer();
		if (viewer != null) {
			viewer.refresh();
		}
		viewer = _swimSlice_TourViewer.getViewer();
		if (viewer != null) {
			viewer.refresh();
		}

		// display modified time slices in other views
		fireModifyNotification();
	}

	/**
	 * updates the fields in the tour data editor and enables actions and controls
	 *
	 * @param tourData
	 * @param forceSliceReload
	 *           <code>true</code> will reload time slices
	 * @param isDirtyDisabled
	 */
	private void updateUI_FromModel(	final TourData tourData,
												final boolean forceSliceReload,
												final boolean isDirtyDisabled) {

		if (tourData == null) {
			_pageBook.showPage(_page_NoTourData);
			return;
		}

		_uiUpdateCounter++;

		/*
		 * set tour data because the TOUR_PROPERTIES_CHANGED event can occure before the runnable is
		 * executed, this ensures that tour data is already set even if the ui is not yet updated
		 */
		_tourData = tourData;

		// get manual/device mode
		_isManualTour = tourData.isManualTour();

		updateMarkerMap();

		Display.getDefault().asyncExec(new Runnable() {

			final int __runnableCounter = _uiUpdateCounter;

			@Override
			public void run() {

				/*
				 * update the UI
				 */

				// check if this is the last runnable
				if (__runnableCounter != _uiUpdateCounter) {
					// a new runnable was created
					return;
				}

				_uiRunnableTourData = tourData;
				_uiRunnableForce_TimeSliceReload = forceSliceReload;
				_uiRunnableForce_SwimSliceReload = forceSliceReload;
				_uiRunnableIsDirtyDisabled = isDirtyDisabled;

				// force reload
				_uiRunnableCounter = _uiUpdateCounter - 1;

				if (_isPartVisible) {
					updateUI_FromModelRunnable();
				}
			}
		});
	}

	private void updateUI_FromModelRunnable() {

		if (_uiRunnableCounter == _uiUpdateCounter) {
			// UI is updated
			return;
		}

		_uiRunnableCounter = _uiUpdateCounter;

		if (_page_EditorForm.isDisposed() || (_uiRunnableTourData == null)) {
			// widget is disposed or data is not set
			return;
		}

		_isSetField = _uiRunnableIsDirtyDisabled;

		// keep tour data
		_tourData = _uiRunnableTourData;
		_isTourWithSwimData = _tourData.swim_Time != null;

		updateMarkerMap();

		// a tour which is not saved has no tour references
		_isReferenceTourAvailable = _tourData.isContainReferenceTour();

		// show tour type image when tour type is set
		final TourType tourType = _uiRunnableTourData.getTourType();
		if (tourType == null) {
			_page_EditorForm.setImage(null);
		} else {
			_page_EditorForm.setImage(TourTypeImage.getTourTypeImage(tourType.getTypeId()));
		}

		updateUI_TitleAsynch(getTourTitle());

		updateUI_Tab_1_Tour();
		updateUI_Tab_2_TimeSlices();
		updateUI_Tab_3_SwimSlices();
		updateUI_ReferenceTourRanges();

		enableActions();
		enableControls();

		/*
		 * Cadence is edited in swim slices, cadence in time slices cannot be edited
		 */
		_timeSlice_CadenceEditingSupport.setCanEditSlices(_isTourWithSwimData == false);

		// this action displays selected unit label
		_actionSetStartDistanceTo_0.setText(
				NLS.bind(Messages.TourEditor_Action_SetStartDistanceTo0, UI.UNIT_LABEL_DISTANCE));

		// show editor page
		_pageBook.showPage(_page_EditorForm);
		_pageBook_Swim.showPage(_isTourWithSwimData ? _pageSwim_Data : _pageSwim_NoData);

		_isSetField = false;
	}

	void updateUI_LatLonDigits(final int selectedDigits) {

		if (selectedDigits == _latLonDigits) {
			// nothing has changed
			return;
		}

		_latLonDigits = selectedDigits;

		setup_LatLonDigits();

		_timeSlice_Viewer.getControl().setRedraw(false);
		{
			_timeSlice_Viewer.refresh(true);
		}
		_timeSlice_Viewer.getControl().setRedraw(true);
	}

	/**
	 * Reference tours
	 */
	private void updateUI_ReferenceTourRanges() {

		final Collection<TourReference> refTours = _tourData.getTourReferences();
		if (refTours.size() > 0) {
			final ArrayList<TourReference> refTourList = new ArrayList<>(refTours);

			// sort reference tours by start index
			Collections.sort(refTourList, new Comparator<TourReference>() {
				@Override
				public int compare(final TourReference refTour1, final TourReference refTour2) {
					return refTour1.getStartValueIndex() - refTour2.getStartValueIndex();
				}
			});

			final StringBuilder sb = new StringBuilder();
			int refCounter = 0;

			_refTourRange = new int[refTourList.size()][2];

			for (final TourReference refTour : refTourList) {

				if (refCounter > 0) {
					sb.append(UI.NEW_LINE);
				}

				sb.append(refTour.getLabel());

				sb.append(" ("); //$NON-NLS-1$
				sb.append(refTour.getStartValueIndex());
				sb.append(UI.DASH_WITH_SPACE);
				sb.append(refTour.getEndValueIndex());
				sb.append(")"); //$NON-NLS-1$

				final int[] oneRange = _refTourRange[refCounter];
				oneRange[0] = refTour.getStartValueIndex();
				oneRange[1] = refTour.getEndValueIndex();

				refCounter++;
			}

		} else {

			_refTourRange = null;
		}
	}

	private void updateUI_Tab_1_Tour() {

		/*
		 * tour/event
		 */
		// title/description
		_comboTitle.setText(_tourData.getTourTitle());
		_txtDescription.setText(_tourData.getTourDescription());

		// start/end location
		_comboLocation_Start.setText(_tourData.getTourStartPlace());
		_comboLocation_End.setText(_tourData.getTourEndPlace());

		/*
		 * personal details
		 */
		_spinBodyWeight.setSelection(Math.round(_tourData.getBodyWeight() * 10));
		_spinFTP.setSelection(_tourData.getPower_FTP());
		_spinRestPuls.setSelection(_tourData.getRestPulse());
		_spinCalories.setSelection(_tourData.getCalories());

		/*
		 * wind properties
		 */
		_txtWeather.setText(_tourData.getWeather());

		// wind direction
		final int weatherWindDirDegree = _tourData.getWeatherWindDir();
		_spinWindDirectionValue.setSelection(weatherWindDirDegree);
		_comboWindDirectionText.select(getWindDirectionTextIndex(weatherWindDirDegree));

		// wind speed
		final int windSpeed = _tourData.getWeatherWindSpeed();
		final int speed = (int) (windSpeed / _unitValueDistance);
		_spinWindSpeedValue.setSelection(speed);
		_comboWindSpeedText.select(getWindSpeedTextIndex(speed));

		// weather clouds
		_comboClouds.select(_tourData.getWeatherIndex());

		// icon must be displayed after the combobox entry is selected
		displayCloudIcon();

		/*
		 * avg temperature
		 */
		final float avgTemperature = UI.convertTemperatureFromMetric(_tourData.getAvgTemperature());

		/*
		 * on Linux .setDigit() fires an asynch selection event that the flag _isSetField is not
		 * recognized
		 */
		_isSetDigits = true;
		_spinTemperature.setDigits(1);
		_spinTemperature.setSelection((int) ((avgTemperature * 10) + 0.5));

		// set start date/time without time zone
		final ZonedDateTime tourStartTime = _tourData.getTourStartTime();
		_dtTourDate.setDate(tourStartTime.getYear(), tourStartTime.getMonthValue() - 1, tourStartTime.getDayOfMonth());
		_dtStartTime.setTime(tourStartTime.getHour(), tourStartTime.getMinute(), tourStartTime.getSecond());

		// tour distance
		final float tourDistance = _tourData.getTourDistance();
		if (tourDistance == 0) {
			_txtDistance.setText(Integer.toString(0));
		} else {

			final float distance = tourDistance / 1000 / _unitValueDistance;
			_txtDistance.setText(_nf3NoGroup.format(distance));
		}

		// altitude up/down
		final int altitudeUp = _tourData.getTourAltUp();
		final int altitudeDown = _tourData.getTourAltDown();
		_txtAltitudeUp.setText(Integer.toString((int) (altitudeUp / _unitValueAltitude)));
		_txtAltitudeDown.setText(Integer.toString((int) (altitudeDown / _unitValueAltitude)));

		// tour time's
		final int recordingTime = (int) _tourData.getTourRecordingTime();
		final int drivingTime = (int) _tourData.getTourDrivingTime();
		final int pausedTime = recordingTime - drivingTime;

		_timeRecording.setTime(recordingTime);
		_timeDriving.setTime(drivingTime);
		_timePaused.setTime(pausedTime);

		/*
		 * Time zone
		 */
		int timeZoneIndex;
		final String timeZoneId = _tourData.getTimeZoneId();
		if (timeZoneId == null) {

			// time zone is not set, try to get it from the geo position
			double lat0 = Double.MIN_VALUE;
			double lat1 = Double.MIN_VALUE;

			if (_tourData.latitudeSerie != null && _tourData.latitudeSerie.length > 0) {
				lat0 = _tourData.latitudeSerie[0];
				lat1 = _tourData.longitudeSerie[0];
			}

			timeZoneIndex = TimeTools.getTimeZoneIndex(lat0, lat1);

		} else {

			timeZoneIndex = TimeTools.getTimeZoneIndex_WithDefault(timeZoneId);
		}
		_comboTimeZone.select(timeZoneIndex);

		_linkDefaultTimeZone.setToolTipText(
				NLS.bind(
						Messages.Tour_Editor_Link_SetDefautTimeZone_Tooltip,
						TimeTools.getDefaultTimeZoneId()));

		updateUI_TimeZone();

		// tour type/tags
		net.tourbook.ui.UI.updateUI_TourType(_tourData, _lblTourType, true);
		net.tourbook.ui.UI.updateUI_Tags(_tourData, _lblTags);

		// measurement system
		_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
		_lblAltitudeUpUnit.setText(UI.UNIT_LABEL_ALTITUDE);
		_lblAltitudeDownUnit.setText(UI.UNIT_LABEL_ALTITUDE);
		_lblTemperatureUnit.setText(UI.UNIT_LABEL_TEMPERATURE);
		_lblSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		// cadence rpm/spm
		final float cadence = _tourData.getCadenceMultiplier();
		final boolean isSpm = cadence == 2.0f;
		_rdoCadence_Rpm.setSelection(!isSpm);
		_rdoCadence_Spm.setSelection(isSpm);

		/*
		 * layout container to resize labels
		 */
		_tourContainer.layout(true);

	}

	private void updateUI_Tab_2_TimeSlices() {

		if (_uiRunnableForce_TimeSliceReload) {
			_timeSlice_ViewerTourId = -1L;
		}

		if ((_tabFolder.getSelection() == _tab_20_TimeSlices) && (_timeSlice_ViewerTourId != _tourData.getTourId())) {

			/*
			 * Time slice tab is selected and the viewer is not yeat loaded
			 */

			_timeSlice_TourViewer.reloadViewer();
			_timeSlice_ViewerTourId = _tourData.getTourId();

			updateStatusLine();

		} else {

			if (_timeSlice_ViewerTourId != _tourData.getTourId()) {
				// force reload when it's not yet loaded
				_timeSlice_ViewerTourId = -1L;
			}
		}
	}

	private void updateUI_Tab_3_SwimSlices() {

		if (_uiRunnableForce_SwimSliceReload) {
			_swimSlice_ViewerTourId = -1L;
		}

		if ((_tabFolder.getSelection() == _tab_30_SwimSlices) && (_swimSlice_ViewerTourId != _tourData.getTourId())) {

			/*
			 * Swim slice tab is selected and the viewer is not yeat loaded
			 */

			_swimSlice_TourViewer.reloadViewer();
			_swimSlice_ViewerTourId = _tourData.getTourId();

			updateStatusLine();

		} else {

			if (_swimSlice_ViewerTourId != _tourData.getTourId()) {
				// force reload when it's not yet loaded
				_swimSlice_ViewerTourId = -1L;
			}
		}
	}

	/**
	 * Validate tour recording/driving/paused time
	 */
	private void updateUI_Time(final Widget widget) {

		/*
		 * check if a time control is currently used
		 */
		if ((widget == _timeRecording._spinHours
				|| widget == _timeRecording._spinMinutes
				|| widget == _timeRecording._spinSeconds
				|| widget == _timeDriving._spinHours
				|| widget == _timeDriving._spinMinutes
				|| widget == _timeDriving._spinSeconds
				|| widget == _timePaused._spinHours
				|| widget == _timePaused._spinMinutes || widget == _timePaused._spinSeconds) == false) {

			return;
		}

		int recTime = _timeRecording.getTime();
		int pausedTime = _timePaused.getTime();
		int driveTime = _timeDriving.getTime();

		if (recTime < 0) {
			recTime = -recTime - 1;
		}
		if (pausedTime < 0) {
			pausedTime = 0;
		}

		if (widget == _timeRecording._spinHours
				|| widget == _timeRecording._spinMinutes
				|| widget == _timeRecording._spinSeconds) {

			// recording time is modified

			if (pausedTime > recTime) {
				pausedTime = recTime;
			}

			driveTime = recTime - pausedTime;

		} else if (widget == _timePaused._spinHours
				|| widget == _timePaused._spinMinutes
				|| widget == _timePaused._spinSeconds) {

			// paused time is modified

			if (pausedTime > recTime) {
				recTime = pausedTime;
			}

			driveTime = recTime - pausedTime;

		} else if (widget == _timeDriving._spinHours
				|| widget == _timeDriving._spinMinutes
				|| widget == _timeDriving._spinSeconds) {

			// driving time is modified

			if (driveTime > recTime) {
				recTime = driveTime;
			}

			pausedTime = recTime - driveTime;
		}

		_timeRecording.setTime(recTime / 3600, ((recTime % 3600) / 60), ((recTime % 3600) % 60));
		_timeDriving.setTime(driveTime / 3600, ((driveTime % 3600) / 60), ((driveTime % 3600) % 60));
		_timePaused.setTime(pausedTime / 3600, ((pausedTime % 3600) / 60), ((pausedTime % 3600) % 60));
	}

	private void updateUI_TimeZone() {

		/*
		 * Update tooltip
		 */
		final ZonedDateTime tourStartTime = _tourData.getTourStartTime();
		final ZonedDateTime tourStartTimeUTC = tourStartTime.withZoneSameInstant(ZoneOffset.UTC);

		final String tourStartTooltip = NLS.bind( //
				Messages.Tour_Editor_Label_TourStartTime_Tooltip,
				tourStartTimeUTC.format(TimeTools.Formatter_DateTime_SM));

		_lblStartTime.setToolTipText(tourStartTooltip);
		_lblTimeZone.setToolTipText(tourStartTooltip);
		_comboTimeZone.setToolTipText(tourStartTooltip);

		/*
		 * Show/hide time zone decorator
		 */
		if (_tourData.hasATimeZone()) {

			// hide info
			_decoTimeZone.hide();

		} else {

			// show info that a time zone is not set
			_decoTimeZone.show();
		}
	}

	/**
	 * Update title of the view with the modified date/time
	 */
	private void updateUI_Title() {

		final ZoneId zoneId = _tourData == null //
				? TimeTools.getDefaultTimeZone()
				: _tourData.getTimeZoneIdWithDefault();

		final ZonedDateTime tourStartTime = ZonedDateTime.of(
				_dtTourDate.getYear(),
				_dtTourDate.getMonth() + 1,
				_dtTourDate.getDay(),
				_dtStartTime.getHours(),
				_dtStartTime.getMinutes(),
				_dtStartTime.getSeconds(),
				0,
				zoneId);

		final String tourTitle = TourManager.getTourTitle(tourStartTime);

		updateUI_TitleAsynch(tourTitle);
	}

	/**
	 * update the title is a really performance hog because of the date/time controls when they are
	 * layouted
	 */
	private void updateUI_TitleAsynch(final String title) {

		_uiUpdateTitleCounter++;

		Display.getCurrent().asyncExec(new Runnable() {

			final int runnableCounter = _uiUpdateTitleCounter;

			@Override
			public void run() {

				if (_page_EditorForm.isDisposed()) {
					return;
				}

				// check if this is the last runnable
				if (runnableCounter != _uiUpdateTitleCounter) {
					// a new runnable was created
					return;
				}

				_page_EditorForm.setText(title);
			}
		});
	}

	private void updateUI_TourTypeTags() {

		// tour type/tags
		net.tourbook.ui.UI.updateUI_TourType(_tourData, _lblTourType, true);
		net.tourbook.ui.UI.updateUI_Tags(_tourData, _lblTags);

		// reflow layout that the tags are aligned correctly
		_tourContainer.layout(true);
	}

	private void writeCSVHeader(final Writer exportWriter, final StringBuilder sb) throws IOException {

		// no.
		sb.append("#"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// time hh:mm:ss
		sb.append("hh:mm:ss"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// time in seconds
		sb.append("sec"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// distance
		sb.append(UI.UNIT_LABEL_DISTANCE);
		sb.append(UI.TAB);

		// altitude
		sb.append(UI.UNIT_LABEL_ALTITUDE);
		sb.append(UI.TAB);

		// gradient
		sb.append("%"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// pulse
		sb.append("bpm"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// marker
		sb.append("marker"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// temperature
		sb.append(UI.UNIT_LABEL_TEMPERATURE);
		sb.append(UI.TAB);

		// cadence
		sb.append("rpm"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// speed
		sb.append(UI.UNIT_LABEL_SPEED);
		sb.append(UI.TAB);

		// pace
		sb.append(UI.UNIT_LABEL_PACE);
		sb.append(UI.TAB);

		// power
		sb.append("W"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// longitude
		sb.append("longitude"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// latitude
		sb.append("latitude"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// break time
		sb.append("break"); //$NON-NLS-1$
		sb.append(UI.TAB);

		// end of line
		sb.append(net.tourbook.ui.UI.SYSTEM_NEW_LINE);
		exportWriter.write(sb.toString());
	}
}
