/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartLabel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourType;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.export.ActionExport;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.mapping.SelectionMapPosition;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourSaveListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ImageComboLabel;
import net.tourbook.ui.MessageManager;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionExtractTour;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.action.ActionSplitTour;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.util.ColumnDefinition;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.Util;

import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

// author: Wolfgang Schramm
// create: 24.08.2007

/**
 * This editor can edit (when all is implemented) all data for a tour
 */
public class TourDataEditorView extends ViewPart implements ISaveablePart2, ITourViewer, ITourProvider {

	public static final String					ID								= "net.tourbook.views.TourDataEditorView";	//$NON-NLS-1$

	private static final String					CSV_FILE_EXTENSION				= "csv";									//$NON-NLS-1$

	private final IPreferenceStore				_prefStore						= TourbookPlugin
																						.getDefault()
																						.getPreferenceStore();

	private final IDialogSettings				_viewState						= TourbookPlugin
																						.getDefault()
																						.getDialogSettingsSection(ID);
	private final IDialogSettings				_viewStateSlice					= TourbookPlugin
																						.getDefault()
																						.getDialogSettingsSection(//
																								ID + ".slice");			//$NON-NLS-1$
	private final IDialogSettings				_viewStateMarker				= TourbookPlugin
																						.getDefault()
																						.getDialogSettingsSection(//
																								ID + ".marker");			//$NON-NLS-1$

	private static final String					WIDGET_KEY						= "widgetKey";								//$NON-NLS-1$
	private static final String					WIDGET_KEY_TOURDISTANCE			= "tourDistance";							//$NON-NLS-1$
	private static final String					WIDGET_KEY_PERSON				= "tourPerson";							//$NON-NLS-1$
	private static final String					MESSAGE_KEY_ANOTHER_SELECTION	= "anotherSelection";						//$NON-NLS-1$

	/**
	 * shows the busy indicator to load the slice viewer when there are more items as this value
	 */
	private static final int					BUSY_INDICATOR_ITEMS			= 5000;

	private static final String					STATE_SELECTED_TAB				= "tourDataEditor.selectedTab";			//$NON-NLS-1$
	private static final String					STATE_ROW_EDIT_MODE				= "tourDataEditor.rowEditMode";			//$NON-NLS-1$
	private static final String					STATE_IS_EDIT_MODE				= "tourDataEditor.isEditMode";				//$NON-NLS-1$
	private static final String					STATE_CSV_EXPORT_PATH			= "tourDataEditor.csvExportPath";			//$NON-NLS-1$

	/*
	 * data series which are displayed in the viewer
	 */
	private int[]								_serieTime;
	private int[]								_serieDistance;
	private int[]								_serieAltitude;
	private int[]								_serieTemperature;
	private int[]								_serieCadence;
	private int[]								_serieGradient;
	private int[]								_serieSpeed;
	private int[]								_seriePace;
	private int[]								_seriePower;
	private int[]								_seriePulse;
	private double[]							_serieLatitude;
	private double[]							_serieLongitude;

	// slice viewer
	private ColumnDefinition					_colDefAltitude;
	private ColumnDefinition					_colDefCadence;
	private ColumnDefinition					_colDefPulse;
	private ColumnDefinition					_colDefTemperature;
	private ColumnDefinition					_colDefSliceMarker;
	private ColumnDefinition					_colDefLatitude;
	private ColumnDefinition					_colDefLongitude;

	// marker viewer
	private ColumnDefinition					_colDefMarker;

	private MessageManager						_messageManager;

	private PostSelectionProvider				_postSelectionProvider;
	private ISelectionListener					_postSelectionListener;
	private IPartListener2						_partListener;
	private IPropertyChangeListener				_prefChangeListener;
	private ITourEventListener					_tourEventListener;
	private ITourSaveListener					_tourSaveListener;

	private final Calendar						_calendar						= GregorianCalendar.getInstance();

	private final DateTimeFormatter				_dtFormatter					= DateTimeFormat.mediumDateTime();

	private final NumberFormat					_nf1							= NumberFormat.getNumberInstance();
	private final NumberFormat					_nf1NoGroup						= NumberFormat.getNumberInstance();
	private final NumberFormat					_nf3							= NumberFormat.getNumberInstance();
	private final NumberFormat					_nf3NoGroup						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);

		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);

		_nf1NoGroup.setMinimumFractionDigits(1);
		_nf1NoGroup.setMaximumFractionDigits(1);
		_nf1NoGroup.setGroupingUsed(false);

		_nf3NoGroup.setMinimumFractionDigits(3);
		_nf3NoGroup.setMaximumFractionDigits(3);
		_nf3NoGroup.setGroupingUsed(false);
	}

	/**
	 * <code>true</code>: rows can be selected in the viewer<br>
	 * <code>false</code>: cell can be selected in the viewer
	 */
	private boolean								_isRowEditMode					= true;

	private boolean								_isEditMode;

	private long								_sliceViewerTourId				= -1;
	private SelectionChartXSliderPosition		_sliceViewerXSliderPosition;

	private boolean								_isTourDirty					= false;

	/**
	 * is <code>true</code> when the tour is currently being saved to prevent a modify event or the
	 * onSelectionChanged event
	 */
	private boolean								_isSavingInProgress				= false;

	/**
	 * when <code>true</code>, the tour dirty flag is disabled to load data into the fields
	 */
	private boolean								_isDirtyDisabled				= false;

	/**
	 * contains the tour id from the last selection event
	 */
	private Long								_selectionTourId;

	private KeyAdapter							_keyListener;
	private ModifyListener						_modifyListener;
	private MouseWheelListener					_mouseWheelListener;
	private SelectionAdapter					_selectionListener;
	private ModifyListener						_verifyFloatValue;
	private SelectionAdapter					_tourTimeListener;
	private SelectionAdapter					_dateTimeListener;

	private PixelConverter						_pc;

	/**
	 * this width is used as a hint for the width of the description field, this value also
	 * influences the width of the columns in this editor
	 */
	private final int							_textColumnWidth				= 150;

	/**
	 * is <code>true</code> when {@link #_tourChart} contains reference tours
	 */
	private boolean								_isReferenceTourAvailable;

	/**
	 * range for the reference tours, is <code>null</code> when reference tours are not available<br>
	 * 1st index = ref tour<br>
	 * 2nd index: 0:start, 1:end
	 */
	private int[][]								_refTourRange;

	private boolean								_isPartVisible					= false;

	/**
	 * when <code>true</code> additional info is displayed in the title area
	 */
	private boolean								_isInfoInTitle;

	/**
	 * is <code>true</code> when a cell editor is activ, otherwise <code>false</code>
	 */
	private boolean								_isCellEditorActive				= false;

	/**
	 * every requested UI update increased this counter
	 */
	private int									_uiUpdateCounter;

	/**
	 * counter when the UI update runnable is run, this will optimize performance to not update the
	 * UI when the part is hidden
	 */
	private int									_uiRunnableCounter				= 0;

	private int									_uiUpdateTitleCounter			= 0;
	private TourData							_uiRunnableTourData;
	private boolean								_uiRunnableForceTimeSliceReload;
	private boolean								_uiRunnableIsDirtyDisabled;

// disabled because tour data get corrupted, the tour date could be from another tour
//	private int							fUIUpdateCounterTabTour			= -1;
//	private int							fUIUpdateCounterTabInfo			= -1;
//	private int							fUIUpdateCounterTabMarker		= -1;
//	private int							fUIUpdateCounterTabSlices		= -1;

	private SliceIntegerEditingSupport			_altitudeEditingSupport;
	private SliceIntegerEditingSupport			_pulseEditingSupport;
	private SliceIntegerEditingSupport			_temperatureEditingSupport;
	private SliceIntegerEditingSupport			_cadenceEditingSupport;
	private SliceDoubleEditingSupport			_latitudeEditingSupport;
	private SliceDoubleEditingSupport			_longitudeEditingSupport;

	private int									_enableActionCounter			= 0;

	/**
	 * contains all markers with the data serie index as key
	 */
	private final HashMap<Integer, TourMarker>	_markerMap						= new HashMap<Integer, TourMarker>();

	/**
	 * When <code>true</code> the tour is created with the tour editor
	 */
	private boolean								_isManualTour;

	private boolean								_isDistManuallyModified;
	private boolean								_isWindSpeedManuallyModified;
	private boolean								_isTemperatureManuallyModified;

	/*
	 * measurement unit values
	 */
	private float								_unitValueDistance;
	private float								_unitValueAltitude;
	private float								_unitValueTemperature;
	private int[]								_unitValueWindSpeed;

	private int									_defaultSpinnerWidth;

	/*
	 * ##################################################
	 * UI controls
	 * ##################################################
	 */

//	private Display									_display;

	// pages
	private PageBook							_pageBook;
	private Label								_pageNoTour;
	private Form								_pageEditorForm;

	// tab folder
	private CTabFolder							_tabFolder;
	private CTabItem							_tabTour;
	private CTabItem							_tabMarker;
	private CTabItem							_tabSlices;
	private CTabItem							_tabInfo;

	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>			_firstColumnControls			= new ArrayList<Control>();
	private final ArrayList<Control>			_firstColumnContainerControls	= new ArrayList<Control>();
	private final ArrayList<Control>			_secondColumnControls			= new ArrayList<Control>();

	private TourChart							_tourChart;
	private TourData							_tourData;
	private Composite							_tourContainer;
	private ScrolledComposite					_scrolledTabInfo;

	private Composite							_infoContainer;
	private Composite							_markerViewerContainer;
	private Composite							_sliceContainer;

	private Composite							_sliceViewerContainer;
	private Label								_timeSliceLabel;
	private TableViewer							_sliceViewer;

	private Object[]							_sliceViewerItems;
	private ColumnManager						_sliceColumnManager;

	private TableViewer							_markerViewer;
	private ColumnManager						_markerColumnManager;

	private FormToolkit							_tk;

	/*
	 * tab: tour
	 */
	private Text								_txtTitle;
	private Text								_txtDescription;
	private Text								_txtStartLocation;
	private Text								_txtEndLocation;

	private Spinner								_spinRestPuls;
	private Spinner								_spinTourCalories;

	private Spinner								_spinTemperature;
	private Label								_lblTemperatureUnit;

	private Spinner								_spinWindDirectionValue;
	private Spinner								_spinWindSpeedValue;
	private Combo								_comboWindDirectionText;
	private Combo								_comboWindSpeedText;

	private CLabel								_lblCloudIcon;
	private Combo								_comboClouds;

	private Text								_txtTourDistance;
	private Label								_lblTourDistanceUnit;
	private Link								_linkTag;

	private Label								_lblTourTags;
	private Link								_linkTourType;
	private CLabel								_lblTourType;

	private DateTime							_dtTourDate;
	private DateTime							_dtStartTime;
	private DateTime							_dtRecordingTime;
	private DateTime							_dtDrivingTime;
	private DateTime							_dtPausedTime;

	private Label								_lblSpeedUnit;

	/*
	 * tab: info
	 */
	private Text								_txtRefTour;
	private Text								_txtTimeSlicesCount;
	private Text								_txtDeviceName;
	private Text								_txtDistanceSensor;
	private ImageComboLabel						_txtImportFilePath;
	private Text								_txtPerson;
	private Text								_txtTourId;
	private Text								_txtMergeFromTourId;
	private Text								_txtMergeIntoTourId;

	private Text								_txtDateTimeCreated;
	private Text								_txtDateTimeModified;

	/*
	 * actions
	 */
	private ActionSaveTour						_actionSaveTour;
	private ActionCreateTour					_actionCreateTour;
	private ActionUndoChanges					_actionUndoChanges;
	private ActionDeleteDistanceValues			_actionDeleteDistanceValues;
	private ActionSetStartDistanceTo0			_actionSetStartDistanceTo0;
	private ActionComputeDistanceValues			_actionComputeDistanceValues;
	private ActionToggleRowSelectMode			_actionToggleRowSelectMode;
	private ActionToggleReadEditMode			_actionToggleReadEditMode;
	private ActionOpenMarkerDialog				_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog		_actionOpenAdjustAltitudeDialog;
	private ActionDeleteTimeSlicesKeepTime		_actionDeleteTimeSlicesKeepTime;
	private ActionDeleteTimeSlicesRemoveTime	_actionDeleteTimeSlicesRemoveTime;
	private ActionCreateTourMarker				_actionCreateTourMarker;
	private ActionExport						_actionExportTour;
	private ActionCSVTimeSliceExport			_actionCsvTimeSliceExport;
	private ActionSplitTour						_actionSplitTour;
	private ActionExtractTour					_actionExtractTour;

	private ActionSetTourTag					_actionAddTag;
	private ActionSetTourTag					_actionRemoveTag;
	private ActionRemoveAllTags					_actionRemoveAllTags;
	private ActionOpenPrefDialog				_actionOpenTagPrefs;
	private ActionOpenPrefDialog				_actionOpenTourTypePrefs;

	private ActionDeleteTourMarker				_actionDeleteTourMarker;
	private ActionModifyColumns					_actionModifyColumns;

	/*
	 * ##################################################
	 * End of UI controls
	 * ##################################################
	 */

	private final class MarkerEditingSupport extends EditingSupport {

		private final TextCellEditor	__cellEditor;

		private MarkerEditingSupport(final TextCellEditor cellEditor) {
			super(_markerViewer);
			__cellEditor = cellEditor;
		}

		@Override
		protected boolean canEdit(final Object element) {

			if ((_isEditMode == false) || (isTourInDb() == false)) {
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
			return ((TourMarker) element).getLabel();
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				final TourMarker tourMarker = (TourMarker) element;
				final String newValue = (String) value;

				if (newValue.equals(tourMarker.getLabel()) == false) {

					// value has changed

					tourMarker.setLabel(newValue);

					setTourDirty();

					// update viewer
					super.getViewer().update(element, null);

					// display modified time slices in this editor and in other views/editors
					fireModifyNotification();
				}
			}
		}
	}

	private class MarkerViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			if (_tourData == null) {
				return new Object[0];
			} else {
				return _tourData.getTourMarkers().toArray();
			}
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Sort the markers by time
	 */
	private static class MarkerViewerSorter extends ViewerSorter {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
		}
	}

	private final class SliceDoubleEditingSupport extends EditingSupport {

		private final TextCellEditor	__cellEditor;
		private double[]				__dataSerie;

		private SliceDoubleEditingSupport(final TextCellEditor cellEditor, final double[] dataSerie) {
			super(_sliceViewer);
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
			return new Float(__dataSerie[((TimeSlice) element).serieIndex]).toString();
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
						 * worldposition has changed, this is an absolute overkill, wenn only one
						 * position has changed
						 */
						_tourData.clearWorldPositions();

						updateUIAfterSliceEdit();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				} finally {}
			}
		}
	}

	private final class SliceIntegerEditingSupport extends EditingSupport {

		private final TextCellEditor	__cellEditor;
		private int[]					__dataSerie;

		private SliceIntegerEditingSupport(final TextCellEditor cellEditor, final int[] dataSerie) {
			super(_sliceViewer);
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

			final int metricValue = __dataSerie[((TimeSlice) element).serieIndex];
			int displayedValue = metricValue;

			/*
			 * convert current measurement system into metric
			 */
			if (__dataSerie == _serieAltitude) {

				if (_unitValueAltitude != 1) {

					// none metric measurement systemm

					displayedValue /= _unitValueAltitude;
				}

			} else if (__dataSerie == _serieTemperature) {

				if (_unitValueTemperature != 1) {

					// none metric measurement systemm

					displayedValue = (int) (metricValue * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
				}
			}

			return new Integer(displayedValue).toString();
		}

		public void setDataSerie(final int[] dataSerie) {
			__dataSerie = dataSerie;
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				try {

					/*
					 * convert entered value into metric value
					 */
					final int enteredValue = Integer.parseInt((String) value);
					int metricValue = enteredValue;
					if (__dataSerie == _serieAltitude) {

						if (_unitValueAltitude != 1) {

							// none metric measurement systemm

							// ensure float is used
							final float noneMetricValue = enteredValue;
							metricValue = Math.round(noneMetricValue * _unitValueAltitude);
						}

					} else if (__dataSerie == _serieTemperature) {

						if (_unitValueTemperature != 1) {

							// none metric measurement systemm

							// ensure float is used
							final float noneMetricValue = enteredValue;
							metricValue = Math.round(((noneMetricValue - UI.UNIT_FAHRENHEIT_ADD))
									/ UI.UNIT_FAHRENHEIT_MULTI);
						}
					}

					final int serieIndex = ((TimeSlice) element).serieIndex;
					if (metricValue != __dataSerie[serieIndex]) {

						// value has changed

						// update dataserie
						__dataSerie[serieIndex] = metricValue;

						updateUIAfterSliceEdit();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				} finally {}
			}
		}
	}

	private final class SliceMarkerEditingSupport extends EditingSupport {

		private final TextCellEditor	__cellEditor;

		private SliceMarkerEditingSupport(final TextCellEditor cellEditor) {
			super(_sliceViewer);
			__cellEditor = cellEditor;
		}

		@Override
		protected boolean canEdit(final Object element) {

			if ((isTourInDb() == false) || (_isEditMode == false)) {
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

			final TourMarker tourMarker = _markerMap.get(((TimeSlice) element).serieIndex);
			if (tourMarker == null) {

				// marker is not yet available

				return UI.EMPTY_STRING;

			} else {

				// marker is available

				return tourMarker.getLabel();
			}
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				final TimeSlice timeSlice = (TimeSlice) element;
				final int serieIndex = timeSlice.serieIndex;
				final String markerLabel = (String) value;

				boolean isMarkerModified = false;

				TourMarker tourMarker = _markerMap.get(serieIndex);
				if (tourMarker == null) {

					// marker is not yet available

					// check if label is empty
					if (markerLabel.trim().length() == 0) {
						return;
					}

					/*
					 * create a new marker
					 */
					tourMarker = new TourMarker(_tourData, ChartLabel.MARKER_TYPE_CUSTOM);

					tourMarker.setSerieIndex(serieIndex);
					tourMarker.setTime(_tourData.timeSerie[serieIndex]);
					tourMarker.setLabel(markerLabel);
					tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

					final int[] distSerie = _tourData.getMetricDistanceSerie();
					if (distSerie != null) {
						tourMarker.setDistance(distSerie[serieIndex]);
					}

					_tourData.getTourMarkers().add(tourMarker);
					updateMarkerMap();

					// update marker viewer
					_markerViewer.setInput(new Object[0]);

					isMarkerModified = true;

				} else {

					// marker is available

					if (markerLabel.equals(tourMarker.getLabel()) == false) {

						// value has changed

						tourMarker.setLabel(markerLabel);

						isMarkerModified = true;
					}
				}

				if (isMarkerModified) {

					setTourDirty();

					// update slice/marker viewer
					_sliceViewer.update(timeSlice, null);
					_markerViewer.update(tourMarker, null);

					// display modified time slices in other views
					fireModifyNotification();
				}
			}
		}
	}

	private class SliceViewerContentProvider implements IStructuredContentProvider {

		public SliceViewerContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return _sliceViewerItems;
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	/**
	 * It took me hours to find this location where the editor is activated/deactivated
	 * without using TableViewerEditor which is activated in setCellEditingSupport but not
	 * in the row edit mode.
	 */
	private final class TextCellEditorCustomized extends TextCellEditor {

		private TextCellEditorCustomized(final Composite parent) {
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

	/**
	 * Compute distance values from the geo positions
	 * <p>
	 * Performs the run() method in {@link ActionComputeDistanceValues}
	 */
	void actionComputeDistanceValuesFromGeoPosition() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.TourEditor_Dialog_ComputeDistanceValues_Title,
				NLS.bind(Messages.TourEditor_Dialog_ComputeDistanceValues_Message, UI.UNIT_LABEL_DISTANCE)) == false) {
			return;
		}

		final double[] latSerie = _tourData.latitudeSerie;
		final double[] lonSerie = _tourData.longitudeSerie;

		final int[] distanceSerie = new int[latSerie.length];
		_tourData.distanceSerie = distanceSerie;

		double distance = 0;
		double latStart = latSerie[0];
		double lonStart = lonSerie[0];

		// compute distance for every time slice
		for (int serieIndex = 1; serieIndex < latSerie.length; serieIndex++) {

			final double latEnd = latSerie[serieIndex];
			final double lonEnd = lonSerie[serieIndex];

			/*
			 * haversine algorithm is much less accurate compared with vincenty
			 */
//			final double distDiff = Util.distanceHaversine(latStart, lonStart, latEnd, lonEnd);
			final double distDiff = Util.distanceVincenty(latStart, lonStart, latEnd, lonEnd);

			distance += distDiff;
			distanceSerie[serieIndex] = (int) distance;

			latStart = latEnd;
			lonStart = lonEnd;
		}

		// set distance in markers
		final Set<TourMarker> allTourMarker = _tourData.getTourMarkers();
		if (allTourMarker != null) {

			for (final TourMarker tourMarker : allTourMarker) {
				final int markerDistance = distanceSerie[tourMarker.getSerieIndex()];
				tourMarker.setDistance(markerDistance);
			}
		}

		updateUIAfterDistanceModifications();
	}

	/**
	 * Creates a new manually created tour, editor must not be dirty before this action is called
	 */
	public void actionCreateTour() {

		// check if a person is selected
		final TourPerson activePerson = TourbookPlugin.getActivePerson();
		if (activePerson == null) {
			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_create_tour_title,
					Messages.tour_editor_dlg_create_tour_message);
			return;
		}

		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		_calendar.setTimeInMillis(System.currentTimeMillis());

		tourData.setStartHour((short) _calendar.get(Calendar.HOUR_OF_DAY));
		tourData.setStartMinute((short) _calendar.get(Calendar.MINUTE));
		tourData.setStartSecond((short) _calendar.get(Calendar.SECOND));

		tourData.setStartYear((short) _calendar.get(Calendar.YEAR));
		tourData.setStartMonth((short) (_calendar.get(Calendar.MONTH) + 1));
		tourData.setStartDay((short) _calendar.get(Calendar.DAY_OF_MONTH));

		tourData.setWeek(tourData.getStartYear(), tourData.getStartMonth(), tourData.getStartDay());

		// tour id must be created after the tour date/time is set
		tourData.createTourId();

		tourData.setDeviceId(TourData.DEVICE_ID_FOR_MANUAL_TOUR);

// manual device name is translated in TourData
//		tourData.setDeviceName(TourData.DEVICE_NAME_FOR_MANUAL_TOUR);

		tourData.setTourPerson(activePerson);

		// update UI
		_tourData = tourData;
		_tourChart = null;
		updateUIFromModel(tourData, false, true);

		// set editor into edit mode
		_isEditMode = true;
		_actionToggleReadEditMode.setChecked(true);

		enableActions();
		enableControls();

		// select tour tab and first field
		_tabFolder.setSelection(_tabTour);
		_txtTitle.setFocus();
	}

	void actionCsvTimeSliceExport() {

		// get selected time slices
		final StructuredSelection selection = (StructuredSelection) _sliceViewer.getSelection();
		if (selection.size() == 0) {
			return;
		}

		/*
		 * get export filename
		 */
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_file_dialog_text);

		dialog.setFilterPath(_viewState.get(STATE_CSV_EXPORT_PATH));
		dialog.setFilterExtensions(new String[] { CSV_FILE_EXTENSION });
		dialog.setFileName(UI.format_yyyymmdd_hhmmss(_tourData) + UI.DOT + CSV_FILE_EXTENSION);

		final String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			return;
		}

		final File exportFilePath = new Path(selectedFilePath).toFile();

		// keep export path
		_viewState.put(STATE_CSV_EXPORT_PATH, exportFilePath.getPath());

		if (exportFilePath.exists()) {
			if (UI.confirmOverwrite(exportFilePath) == false) {
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
				sb.append(Integer.toString(serieIndex + 1));
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
					sb.append(_nf3.format(((float) _serieDistance[serieIndex]) / 1000 / _unitValueDistance));
				}
				sb.append(UI.TAB);

				// altitude
				if (_serieAltitude != null) {
					sb.append(Integer.toString((int) (_serieAltitude[serieIndex] / _unitValueAltitude)));
				}
				sb.append(UI.TAB);

				// gradient
				if (_serieGradient != null) {
					sb.append(_nf1.format((float) _serieGradient[serieIndex] / 10));
				}
				sb.append(UI.TAB);

				// pulse
				if (_seriePulse != null) {
					sb.append(Integer.toString(_seriePulse[serieIndex]));
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

					final int metricTemperature = _serieTemperature[serieIndex];

					if (_unitValueTemperature != 1) {
						// use imperial system
						final int imperialTemp = (int) (metricTemperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
						sb.append(Integer.toString(imperialTemp));
					} else {
						// use metric system
						sb.append(Integer.toString(metricTemperature));
					}
				}
				sb.append(UI.TAB);

				// cadence
				if (_serieCadence != null) {
					sb.append(Integer.toString(_serieCadence[serieIndex]));
				}
				sb.append(UI.TAB);

				// speed
				if (_serieSpeed != null) {
					sb.append(_nf1.format((float) _serieSpeed[serieIndex] / 10));
				}
				sb.append(UI.TAB);

				// pace
				if (_seriePace != null) {
					sb.append(UI.format_hhh_mm_ss(_seriePace[serieIndex]));
				}
				sb.append(UI.TAB);

				// power
				if (_seriePower != null) {
					sb.append(Integer.toString(_seriePower[serieIndex]));
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

				// end of line
				sb.append(UI.SYSTEM_NEW_LINE);
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

		updateUIAfterDistanceModifications();
	}

	/**
	 * delete selected time slices
	 * 
	 * @param removeTime
	 */
	void actionDeleteTimeSlices(final boolean removeTime) {

		// a tour with reference tours is currently not supported
		if (_isReferenceTourAvailable) {
			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_delete_rows_title,
					Messages.tour_editor_dlg_delete_rows_message);
			return;
		}

		if (isRowSelectionMode() == false) {
			return;
		}

		// get selected time slices
		final StructuredSelection selection = (StructuredSelection) _sliceViewer.getSelection();
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
		final Table table = (Table) _sliceViewer.getControl();
		final int[] indices = table.getSelectionIndices();
		Arrays.sort(indices);
		int lastSelectionIndex = indices[0];

		TourManager.removeTimeSlices(_tourData, firstIndex, lastIndex, removeTime);

		updateMarkerMap();

		getDataSeriesFromTourData();

		// update UI
		updateUITab1Tour();
		updateUITab2Marker();
		updateUITab4Info();

		// update slice viewer
		_sliceViewerItems = getRemainingSliceItems(_sliceViewerItems, firstIndex, lastIndex);

		_sliceViewer.getControl().setRedraw(false);
		{
			// update viewer
			_sliceViewer.remove(selectedTimeSlices);

			// update serie index label
			_sliceViewer.refresh(true);
		}
		_sliceViewer.getControl().setRedraw(true);

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
			_sliceViewer.setSelection(_sliceViewer.getSelection());
		}
	}

	void actionDeleteTourMarker() {

		if (isRowSelectionMode() == false) {
			return;
		}

		final StructuredSelection selection = (StructuredSelection) _markerViewer.getSelection();
		if (selection.size() == 0) {
			return;
		}

		// get last selected index
		final Table table = _markerViewer.getTable();
		final int[] indices = table.getSelectionIndices();
		Arrays.sort(indices);
		int lastSelectionIndex = indices[0];

		_tourData.getTourMarkers().removeAll(selection.toList());
		_markerViewer.remove(selection.toArray());

		updateMarkerMap();

		setTourDirty();

		// notify other viewers
		fireModifyNotification();

		// select next available marker
		final int itemCount = table.getItemCount();
		if (itemCount > 0) {

			// adjust to array bounds
			lastSelectionIndex = Math.max(0, Math.min(lastSelectionIndex, itemCount - 1));

			table.setSelection(lastSelectionIndex);
			table.showSelection();

			// fire slider position
			onMarkerViewerSelectionChanged();
		}
	}

	void actionSaveTour() {

		// action is enabled when the tour is modified

		saveTourIntoDB();
	}

	void actionSetStartDistanceTo0() {

		// it is already checked if a valid data serie is available and first distance is > 0

		final int[] distanceSerie = _tourData.distanceSerie;
		final int distanceOffset = distanceSerie[0];

		// adjust distance data serie
		for (int serieIndex = 0; serieIndex < distanceSerie.length; serieIndex++) {
			final int sliceDistance = distanceSerie[serieIndex];
			distanceSerie[serieIndex] = sliceDistance - distanceOffset;
		}

		// adjust distance in markers
		final Set<TourMarker> allTourMarker = _tourData.getTourMarkers();
		if (allTourMarker != null) {

			for (final TourMarker tourMarker : allTourMarker) {
				final int markerDistance = tourMarker.getDistance();
				if (markerDistance > 0) {
					tourMarker.setDistance(markerDistance - distanceOffset);
				}
			}
		}

		updateUIAfterDistanceModifications();
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
			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourDataEditorView.this) {
					_postSelectionProvider.setSelection(new SelectionTourData(null, _tourData));
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourDataEditorView.this) {

					saveState();
					TourManager.setTourDataEditor(null);
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourDataEditorView.this) {
					_isPartVisible = false;
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourDataEditorView.this) {
					TourManager.setTourDataEditor(TourDataEditorView.this);
				}
			}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourDataEditorView.this) {

					_isPartVisible = true;

					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							updateUIFromModelRunnable();
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
			public void propertyChange(final PropertyChangeEvent event) {

				if (_tourData == null) {
					return;
				}

				final String property = event.getProperty();
				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					/*
					 * tour data could have been changed but the changes are not reflected in the
					 * data model, the model needs to be updated from the UI
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

						UI.updateUnits();

						/*
						 * It is possible that the unit values in the UI class have been updated
						 * before the model was saved, this can happen when another view called the
						 * method UI.updateUnits(). Because of this race condition, only the
						 * internal units are used to calculate values which depend on the
						 * measurement system
						 */
						updateInternalUnitValues();

						recreateViewer();

						updateUIFromModel(_tourData, false, true);

					} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

						// reload tour data

						updateUIFromModel(_tourData, false, true);
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
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
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
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if ((_tourData == null) || (part == TourDataEditorView.this)) {
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
								 * nothing to do because the tour is already saved (when it was not
								 * modified before) and the UI is already updated
								 */
								return;
							}

							if (tourEvent.isReverted) {
								setTourClean();
							} else {
								setTourDirty();
							}

							updateUIFromModel(tourData, true, tourEvent.isReverted);

							// nothing more to do, the editor contains only one tour
							return;
						}
					}

					// removed old tour data from the selection provider
					_postSelectionProvider.clearSelection();

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					updateUIFromModel(_tourData, false, true);

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearEditorContent();

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if this tour data editor contains a tour which must be updated

					// update editor
					if (UI.containsTourId(eventData, tourDataEditorTourId) != null) {

						// reload tour data
						_tourData = TourManager.getInstance().getTourData(_tourData.getTourId());

						updateUIFromModel(_tourData, false, true);
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void addTourSaveListener() {

		_tourSaveListener = new ITourSaveListener() {
			public boolean saveTour() {

				boolean isTourSaved;

				_isSavingInProgress = true;
				{
					isTourSaved = saveTourValidation();
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
			_sliceViewerTourId = -1;

			_postSelectionProvider.clearSelection();

			setTourClean();

			_pageBook.showPage(_pageNoTour);
		}
	}

	private boolean confirmUndoChanges() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		// check if confirmation is disabled
		if (prefStore.getBoolean(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR)) {

			return true;

		} else {

			final MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(Display
					.getCurrent()
					.getActiveShell(),//
					Messages.tour_editor_dlg_revert_tour_title, // title
					Messages.tour_editor_dlg_revert_tour_message, // message
					Messages.tour_editor_dlg_revert_tour_toggle_message, // toggle message
					false, // toggle default state
					null,
					null);

			prefStore.setValue(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR, dialog.getToggleState());

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
		_actionSetStartDistanceTo0 = new ActionSetStartDistanceTo0(this);

		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this, true);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, false);

		_actionDeleteTimeSlicesKeepTime = new ActionDeleteTimeSlicesKeepTime(this);
		_actionDeleteTimeSlicesRemoveTime = new ActionDeleteTimeSlicesRemoveTime(this);

		_actionCreateTourMarker = new ActionCreateTourMarker(this);
		_actionDeleteTourMarker = new ActionDeleteTourMarker(this);
		_actionExportTour = new ActionExport(this);
		_actionCsvTimeSliceExport = new ActionCSVTimeSliceExport(this);
		_actionSplitTour = new ActionSplitTour(this);
		_actionExtractTour = new ActionExtractTour(this);

		_actionAddTag = new ActionSetTourTag(this, true, false);
		_actionRemoveTag = new ActionSetTourTag(this, false, false);
		_actionRemoveAllTags = new ActionRemoveAllTags(this, false);
		_actionOpenTagPrefs = new ActionOpenPrefDialog(
				Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);

		_actionModifyColumns = new ActionModifyColumns(this);
	}

	private void createFieldListener() {

		_modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {

				if (_isDirtyDisabled || _isSavingInProgress) {
					return;
				}

				setTourDirty();
			}
		};

		_mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				if (_isDirtyDisabled || _isSavingInProgress) {
					return;
				}
				setTourDirty();
			}
		};

		_selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isDirtyDisabled || _isSavingInProgress) {
					return;
				}
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

				if (_isDirtyDisabled || _isSavingInProgress) {
					return;
				}

				setTourDirty();

				updateUITitle();

				onModifyContent();
			}
		};

		/*
		 * listener for recording/driving/paused time
		 */
		_tourTimeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {

				if (_isDirtyDisabled || _isSavingInProgress) {
					return;
				}

				setTourDirty();

				/*
				 * ensure validation for all 3 times
				 */

				final DateTime dt = (DateTime) event.widget;

				int recTime = (_dtRecordingTime.getHours() * 3600)
						+ (_dtRecordingTime.getMinutes() * 60)
						+ _dtRecordingTime.getSeconds();

				int pausedTime = (_dtPausedTime.getHours() * 3600)
						+ (_dtPausedTime.getMinutes() * 60)
						+ _dtPausedTime.getSeconds();

				int driveTime = (_dtDrivingTime.getHours() * 3600)
						+ (_dtDrivingTime.getMinutes() * 60)
						+ _dtDrivingTime.getSeconds();

				if (dt == _dtRecordingTime) {

					// recording time is modified

					if (pausedTime > recTime) {
						pausedTime = recTime;
					}

					driveTime = recTime - pausedTime;

				} else if (dt == _dtPausedTime) {

					// paused time is modified

					if (pausedTime > recTime) {
						recTime = pausedTime;
					}

					driveTime = recTime - pausedTime;

				} else if (dt == _dtDrivingTime) {

					// driving time is modified

					if (driveTime > recTime) {
						recTime = driveTime;
					}

					pausedTime = recTime - driveTime;
				}

				_dtRecordingTime.setTime(recTime / 3600, ((recTime % 3600) / 60), ((recTime % 3600) % 60));
				_dtDrivingTime.setTime(driveTime / 3600, ((driveTime % 3600) / 60), ((driveTime % 3600) % 60));
				_dtPausedTime.setTime(pausedTime / 3600, ((pausedTime % 3600) / 60), ((pausedTime % 3600) % 60));
			}
		};

		_verifyFloatValue = new ModifyListener() {

			public void modifyText(final ModifyEvent event) {

				if (_isDirtyDisabled || _isSavingInProgress) {
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
				 * set tour dirty must be set after validation because an error can occure which
				 * enables actions
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
	 * @param parent
	 */
	private void createMarkerViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.MULTI);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
//		table.setLinesVisible(TourbookPlugin.getDefault()
//				.getPreferenceStore()
//				.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		/*
		 * create viewer
		 */
		_markerViewer = new TableViewer(table);
		if (_isRowEditMode == false) {
			setCellEditSupport(_markerViewer);
		}

		// create editing support after the viewer is created but before the columns are created
		final TextCellEditor cellEditor = new TextCellEditorCustomized(_markerViewer.getTable());
		_colDefMarker.setEditingSupport(new MarkerEditingSupport(cellEditor));

		_markerColumnManager.setColumnLayout(tableLayout);
		_markerColumnManager.createColumns(_markerViewer);

		_markerViewer.setUseHashlookup(true);
		_markerViewer.setContentProvider(new MarkerViewerContentProvicer());
		_markerViewer.setSorter(new MarkerViewerSorter());
		createMarkerViewerContextMenu(table);

		_markerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onMarkerViewerSelectionChanged();
			}
		});

		_markerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (_isEditMode == false) {
					return;
				}

				// edit selected marker
				final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
				if (selection.size() > 0) {
					_actionOpenMarkerDialog.setSelectedMarker((TourMarker) selection.getFirstElement());
					_actionOpenMarkerDialog.run();
				}
			}
		});

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if ((_isEditMode == false) || (isTourInDb() == false)) {
					return;
				}

				if (e.keyCode == SWT.DEL) {
					actionDeleteTourMarker();
				}
			}
		});
	}

	/**
	 * create the views context menu
	 * 
	 * @param table
	 */
	private void createMarkerViewerContextMenu(final Table table) {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillMarkerContextMenu(manager);
			}
		});

		table.setMenu(menuMgr.createContextMenu(table));

		getSite().registerContextMenu(menuMgr, _markerViewer);
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
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, TourDataEditorView.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		_linkTourType.setMenu(menuMgr.createContextMenu(_linkTourType));

		/*
		 * tag menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final boolean isTagInTour = _tourData.getTourTags().size() > 0;

				// enable actions
				_actionAddTag.setEnabled(true); // 			// !!! action enablement is overwritten
				_actionRemoveTag.setEnabled(isTagInTour);
				_actionRemoveAllTags.setEnabled(isTagInTour);

				// set menu items
				menuMgr.add(_actionAddTag);
				menuMgr.add(_actionRemoveTag);
				menuMgr.add(_actionRemoveAllTags);

				TagManager.fillMenuRecentTags(menuMgr, TourDataEditorView.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		_linkTag.setMenu(menuMgr.createContextMenu(_linkTag));
	}

	@Override
	public void createPartControl(final Composite parent) {

//		_display = parent.getDisplay();

		updateInternalUnitValues();

		// define columns for the viewers
		_sliceColumnManager = new ColumnManager(this, _viewStateSlice);
		defineSliceViewerColumns(parent);

		_markerColumnManager = new ColumnManager(this, _viewStateMarker);
		defineMarkerViewerColumns(parent);

		restoreStateBeforeUI();

		// must be set before the UI is created
		createFieldListener();

		createUI(parent);
		createMenus();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();
		addTourSaveListener();

		createActions();
		fillToolbar();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		restoreStateWithUI();

		_pageBook.showPage(_pageNoTour);

		displaySelectedTour();
	}

	private Composite createSection(final Composite parent,
									final FormToolkit tk,
									final String title,
									final boolean isGrabVertical) {

		final Section section = tk.createSection(parent,//
				//Section.TWISTIE |
//				Section.SHORT_TITLE_BAR
				Section.TITLE_BAR
		// | Section.DESCRIPTION
		// | Section.EXPANDED
				);

		section.setText(title);
		GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

		final Composite sectionContainer = tk.createComposite(section);
		section.setClient(sectionContainer);

//		section.addExpansionListener(new ExpansionAdapter() {
//			@Override
//			public void expansionStateChanged(final ExpansionEvent e) {
//				form.reflow(false);
//			}
//		});

		return sectionContainer;
	}

	/**
	 * @param parent
	 */
	private void createSliceViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		createSliceViewerContextMenu(table);

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

		_sliceViewer = new TableViewer(table);

		if (_isRowEditMode == false) {
			setCellEditSupport(_sliceViewer);
		}

		/*
		 * create editing support after the viewer is created but before the columns are created.
		 */
		final TextCellEditor cellEditor = new TextCellEditorCustomized(_sliceViewer.getTable());

		_altitudeEditingSupport = new SliceIntegerEditingSupport(cellEditor, _serieAltitude);
		_pulseEditingSupport = new SliceIntegerEditingSupport(cellEditor, _seriePulse);
		_temperatureEditingSupport = new SliceIntegerEditingSupport(cellEditor, _serieTemperature);
		_cadenceEditingSupport = new SliceIntegerEditingSupport(cellEditor, _serieCadence);
		_latitudeEditingSupport = new SliceDoubleEditingSupport(cellEditor, _serieLatitude);
		_longitudeEditingSupport = new SliceDoubleEditingSupport(cellEditor, _serieLongitude);

		_colDefAltitude.setEditingSupport(_altitudeEditingSupport);
		_colDefPulse.setEditingSupport(_pulseEditingSupport);
		_colDefTemperature.setEditingSupport(_temperatureEditingSupport);
		_colDefCadence.setEditingSupport(_cadenceEditingSupport);
		_colDefLatitude.setEditingSupport(_latitudeEditingSupport);
		_colDefLongitude.setEditingSupport(_longitudeEditingSupport);
		_colDefSliceMarker.setEditingSupport(new SliceMarkerEditingSupport(cellEditor));

		_sliceColumnManager.createColumns(_sliceViewer);

		_sliceViewer.setContentProvider(new SliceViewerContentProvider());
		_sliceViewer.setUseHashlookup(true);

		_sliceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		_sliceViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (_isEditMode == false) {
					return;
				}
////////////////////////////////////////
//
// disabled because editing is hard to do
//
////////////////////////////////////////
//
//				// create/open tour marker
//
//				final StructuredSelection sliceSelection = (StructuredSelection) event.getSelection();
//				final TimeSlice timeSlice = (TimeSlice) sliceSelection.getFirstElement();
//
//				// check if a marker can be created
//				final TourMarker tourMarker = fMarkerMap.get(timeSlice.serieIndex);
//				if (tourMarker == null) {
//
//					fActionCreateTourMarker.run();
//
//				} else {
//
//					fActionOpenMarkerDialog.setSelectedMarker(tourMarker);
//					fActionOpenMarkerDialog.run();
//				}
			}
		});

		// hide first column, this is a hack to align the "first" visible column to right
		table.getColumn(0).setWidth(0);
	}

	private void createSliceViewerContextMenu(final Table table) {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillSliceContextMenu(manager);
			}
		});

		table.setMenu(menuMgr.createContextMenu(table));
	}

	private void createUI(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		_defaultSpinnerWidth = pixelConverter.convertWidthInCharsToPixels(5);

		_pageBook = new PageBook(parent, SWT.NONE);
		_pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		_pageNoTour = new Label(_pageBook, SWT.NONE);
		_pageNoTour.setText(Messages.UI_Label_no_chart_is_selected);

		_tk = new FormToolkit(parent.getDisplay());

		_pageEditorForm = _tk.createForm(_pageBook);
		_tk.decorateFormHeading(_pageEditorForm);

		_messageManager = new MessageManager(_pageEditorForm);
		_pc = new PixelConverter(parent);

		final Composite formBody = _pageEditorForm.getBody();
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

		_tabTour = new CTabItem(_tabFolder, SWT.FLAT);
		_tabTour.setText(Messages.tour_editor_tabLabel_tour);
		_tabTour.setControl(createUITab10Tour(_tabFolder));

		_tabMarker = new CTabItem(_tabFolder, SWT.FLAT);
		_tabMarker.setText(Messages.tour_editor_tabLabel_tour_marker);
		_tabMarker.setControl(createUITab20Marker(_tabFolder));

		_tabSlices = new CTabItem(_tabFolder, SWT.FLAT);
		_tabSlices.setText(Messages.tour_editor_tabLabel_tour_data);
		_tabSlices.setControl(createUITab30Slices(_tabFolder));

		_tabInfo = new CTabItem(_tabFolder, SWT.FLAT);
		_tabInfo.setText(Messages.tour_editor_tabLabel_info);
		_tabInfo.setControl(createUITab40Info(_tabFolder));

	}

	private void createUISection110Title(final Composite parent) {

		Label label;

		final Composite section = createSection(parent, _tk, Messages.tour_editor_section_tour, true);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(section);
		{
			/*
			 * title
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_tour_title);
			_firstColumnControls.add(label);

			_txtTitle = _tk.createText(section, UI.EMPTY_STRING);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_txtTitle);
			_txtTitle.addKeyListener(_keyListener);
			_txtTitle.addModifyListener(_modifyListener);

			/*
			 * description
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_description);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			_firstColumnControls.add(label);

			_txtDescription = _tk.createText(section, UI.EMPTY_STRING, SWT.BORDER //
					| SWT.WRAP
					| SWT.V_SCROLL
					| SWT.H_SCROLL//
			);

			final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

			int descLines = store.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
			descLines = descLines == 0 ? 5 : descLines;

			GridDataFactory.fillDefaults()//
					.grab(true, true)
					//
					// SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
					//
					.hint(_textColumnWidth, _pc.convertHeightInCharsToPixels(descLines))
					.applyTo(_txtDescription);

			_txtDescription.addModifyListener(_modifyListener);

			/*
			 * start location
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_start_location);
			_firstColumnControls.add(label);

			_txtStartLocation = _tk.createText(section, UI.EMPTY_STRING);
			_txtStartLocation.addModifyListener(_modifyListener);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtStartLocation);

			/*
			 * end location
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_end_location);
			_firstColumnControls.add(label);

			_txtEndLocation = _tk.createText(section, UI.EMPTY_STRING);
			_txtEndLocation.addModifyListener(_modifyListener);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtEndLocation);
		}
	}

	private void createUISection120DateTime(final Composite parent) {

		final Composite section = createSection(parent, _tk, Messages.tour_editor_section_date_time, false);
		GridLayoutFactory.fillDefaults()//
//				.equalWidth(true)
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(section);
		{
			createUISection122DateTimeCol1(section);
			createUISection124DateTimeCol2(section);
		}
	}

	/**
	 * 1. column
	 */
	private void createUISection122DateTimeCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
		{
			/*
			 * date
			 */
			Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_date);
			_firstColumnControls.add(label);

			_dtTourDate = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourDate);
			_tk.adapt(_dtTourDate, true, false);
			_dtTourDate.addSelectionListener(_dateTimeListener);

			//////////////////////////////////////
			createUISeparator(container);

			/*
			 * start time
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_start_time);
			_firstColumnControls.add(label);

			_dtStartTime = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtStartTime);
			_tk.adapt(_dtStartTime, true, false);
			_dtStartTime.addSelectionListener(_dateTimeListener);

			//////////////////////////////////////
			createUISeparator(container);

			/*
			 * tour distance
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_tour_distance);
			_firstColumnControls.add(label);

			_txtTourDistance = _tk.createText(container, UI.EMPTY_STRING, SWT.TRAIL);
			GridDataFactory.fillDefaults().applyTo(_txtTourDistance);
			_txtTourDistance.addModifyListener(_verifyFloatValue);
			_txtTourDistance.setData(WIDGET_KEY, WIDGET_KEY_TOURDISTANCE);
			_txtTourDistance.addKeyListener(new KeyListener() {
				public void keyPressed(final KeyEvent e) {
					_isDistManuallyModified = true;
				}

				public void keyReleased(final KeyEvent e) {}
			});

			_lblTourDistanceUnit = _tk.createLabel(container, UI.UNIT_LABEL_DISTANCE);
		}
	}

	/**
	 * 2. column
	 */
	private void createUISection124DateTimeCol2(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * recording time
			 */
			Label label = _tk.createLabel(container, Messages.tour_editor_label_recording_time);
			_secondColumnControls.add(label);

			_dtRecordingTime = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
			_dtRecordingTime.addSelectionListener(_tourTimeListener);
			_tk.adapt(_dtRecordingTime, true, true);

			/*
			 * paused time
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_paused_time);
			_secondColumnControls.add(label);

			_dtPausedTime = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
			_tk.adapt(_dtPausedTime, true, true);
			_dtPausedTime.addSelectionListener(_tourTimeListener);

			/*
			 * driving time
			 */
			label = _tk.createLabel(container, Messages.tour_editor_label_driving_time);
			_secondColumnControls.add(label);

			_dtDrivingTime = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
			_tk.adapt(_dtDrivingTime, true, true);
			_dtDrivingTime.addSelectionListener(_tourTimeListener);
		}
	}

	private void createUISection130Personal(final Composite parent) {

		final Composite section = createSection(parent, _tk, Messages.tour_editor_section_personal, false);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(section);
		{
			createUISection132PersonalCol1(section);
			createUISection134PersonalCol2(section);
		}
	}

	/**
	 * 1. column
	 */
	private void createUISection132PersonalCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
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
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinRestPuls);
			_spinRestPuls.setMinimum(0);
			_spinRestPuls.setMaximum(200);
			_spinRestPuls.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);

			_spinRestPuls.addModifyListener(_modifyListener);
			_spinRestPuls.addMouseWheelListener(_mouseWheelListener);
			_spinRestPuls.addSelectionListener(_selectionListener);

			// label: bpm
			_tk.createLabel(container, Messages.Graph_Label_Heartbeat_unit);
		}
	}

	/**
	 * 2. column
	 */
	private void createUISection134PersonalCol2(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * calories
			 */

			// label
			final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_calories);
			_secondColumnControls.add(label);

			// spinner
			_spinTourCalories = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_spinTourCalories);
			_spinTourCalories.setMinimum(0);
			_spinTourCalories.setMaximum(1000000);
//			_spinTourCalories.setToolTipText();

			_spinTourCalories.addModifyListener(_modifyListener);
			_spinTourCalories.addMouseWheelListener(_mouseWheelListener);
			_spinTourCalories.addSelectionListener(_selectionListener);

			// label: cal
			_tk.createLabel(container, Messages.tour_editor_label_tour_calories_unit);
		}
	}

	private void createUISection140Weather(final Composite parent) {

		final Composite section = createSection(parent, _tk, Messages.tour_editor_section_weather, false);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(section);
		{
			createUISection142Weather(section);
			createUISection144WeatherCol1(section);
		}
	}

	private void createUISection142Weather(final Composite section) {

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
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinWindSpeedValue);
			_spinWindSpeedValue.setMinimum(0);
			_spinWindSpeedValue.setMaximum(120);
			_spinWindSpeedValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

			_spinWindSpeedValue.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedValue();
					setTourDirty();
				}
			});
			_spinWindSpeedValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedValue();
					setTourDirty();
				}
			});
			_spinWindSpeedValue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isDirtyDisabled || _isSavingInProgress) {
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
			GridDataFactory.fillDefaults()//
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

					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					onSelectWindSpeedText();
					setTourDirty();
				}
			});

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
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_comboWindDirectionText);
			_comboWindDirectionText.setToolTipText(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
			_comboWindDirectionText.setVisibleItemCount(10);
			_comboWindDirectionText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (_isDirtyDisabled || _isSavingInProgress) {
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
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinWindDirectionValue);
			_spinWindDirectionValue.setMinimum(-1);
			_spinWindDirectionValue.setMaximum(360);
			_spinWindDirectionValue.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);

			_spinWindDirectionValue.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					onSelectWindDirectionValue();
					setTourDirty();
				}
			});
			_spinWindDirectionValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					onSelectWindDirectionValue();
					setTourDirty();
				}
			});
			_spinWindDirectionValue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isDirtyDisabled || _isSavingInProgress) {
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
	private void createUISection144WeatherCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
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
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_spinTemperature);
			_spinTemperature.setToolTipText(Messages.tour_editor_label_temperature_Tooltip);

			// the min/max temperature has a large range because fahrenheit has bigger values than celcius
			_spinTemperature.setMinimum(-60);
			_spinTemperature.setMaximum(150);

			_spinTemperature.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					_isTemperatureManuallyModified = true;
					setTourDirty();
				}
			});
			_spinTemperature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isDirtyDisabled || _isSavingInProgress) {
						return;
					}
					_isTemperatureManuallyModified = true;
					setTourDirty();
				}
			});
			_spinTemperature.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isDirtyDisabled || _isSavingInProgress) {
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
				GridDataFactory.fillDefaults()//
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
			_isDirtyDisabled = true;
			{
				_comboClouds.select(0);
				displayCloudIcon();
			}
			_isDirtyDisabled = false;
		}
	}

	private void createUISection150Characteristics(final Composite parent) {

		final Composite section = createSection(parent, _tk, Messages.tour_editor_section_characteristics, false);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(section);
		{
			/*
			 * tags
			 */
			_linkTag = new Link(section, SWT.NONE);
			_linkTag.setText(Messages.tour_editor_label_tour_tag);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_linkTag);
			_linkTag.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_linkTag);
				}
			});
			_tk.adapt(_linkTag, true, true);
			_firstColumnControls.add(_linkTag);

			_lblTourTags = _tk.createLabel(section, UI.EMPTY_STRING, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					// hint is necessary that the width is not expanded when the text is long
					.hint(_textColumnWidth, SWT.DEFAULT)
					.span(3, 1)
					.applyTo(_lblTourTags);

			/*
			 * tour type
			 */
			_linkTourType = new Link(section, SWT.NONE);
			_linkTourType.setText(Messages.tour_editor_label_tour_type);
			_linkTourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_linkTourType);
				}
			});
			_tk.adapt(_linkTourType, true, true);
			_firstColumnControls.add(_linkTourType);

			_lblTourType = new CLabel(section, SWT.NONE);
			GridDataFactory.swtDefaults()//
					.grab(true, false)
					.span(3, 1)
					.applyTo(_lblTourType);
		}
	}

	private void createUISection410Info(final Composite parent) {

		Label label;

		// keep border style
		final int defaultBorderStyle = _tk.getBorderStyle();
		_tk.setBorderStyle(SWT.NULL);

		final Composite section = createSection(parent, _tk, Messages.tour_editor_section_info, false);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(section);
		{
			/*
			 * date/time created
			 */
			label = _tk.createLabel(section, Messages.Tour_Editor_Label_DateTimeCreated);

			_txtDateTimeCreated = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtDateTimeCreated);

			/*
			 * date/time modified
			 */
			label = _tk.createLabel(section, Messages.Tour_Editor_Label_DateTimeModified);

			_txtDateTimeModified = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtDateTimeModified);

			/*
			 * reference tours
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_ref_tour);
			GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

			_txtRefTour = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY | SWT.MULTI);

			/*
			 * number of time slices
			 */
			_tk.createLabel(section, Messages.tour_editor_label_datapoints);

			_txtTimeSlicesCount = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_txtTimeSlicesCount);

			/*
			 * device name
			 */
			_tk.createLabel(section, Messages.tour_editor_label_device_name);

			_txtDeviceName = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_txtDeviceName);

			/*
			 * distance sensor
			 */
			_tk.createLabel(section, Messages.tour_editor_label_DistanceSensor);

			_txtDistanceSensor = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			_txtDistanceSensor.setToolTipText(Messages.Tour_Editor_Label_DistanceSensor_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_txtDistanceSensor);

			/*
			 * import file path
			 */
			_tk.createLabel(section, Messages.tour_editor_label_import_file_path);

			_txtImportFilePath = new ImageComboLabel(section, SWT.NONE);
			_tk.adapt(_txtImportFilePath);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					//
					// adjust to the label controls
					.indent(2, 0)
					//
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_txtImportFilePath);

			/*
			 * person
			 */
			_tk.createLabel(section, Messages.tour_editor_label_person);

			_txtPerson = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtPerson);

			/*
			 * tour id
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_tour_id);
			label.setToolTipText(Messages.tour_editor_label_tour_id_tooltip);

			_txtTourId = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtTourId);

			/*
			 * merged from tour id
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_merge_from_tour_id);
			label.setToolTipText(Messages.tour_editor_label_merge_from_tour_id_tooltip);

			_txtMergeFromTourId = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtMergeFromTourId);

			/*
			 * merged into tour id
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_merge_into_tour_id);
			label.setToolTipText(Messages.tour_editor_label_merge_into_tour_id_tooltip);

			_txtMergeIntoTourId = _tk.createText(section, UI.EMPTY_STRING, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtMergeIntoTourId);
		}

		/*
		 * reset border style
		 */
		_tk.setBorderStyle(defaultBorderStyle);
	}

	private void createUISectionSeparator(final Composite parent) {
		final Composite sep = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 5).applyTo(sep);
	}

	private void createUISeparator(final Composite parent) {
		_tk.createLabel(parent, UI.EMPTY_STRING);
	}

	private Composite createUITab10Tour(final Composite parent) {

		// scrolled container
		final ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				sc.setMinSize(_tourContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		_tourContainer = new Composite(sc, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_tourContainer);
		_tk.adapt(_tourContainer);
		GridLayoutFactory.swtDefaults().applyTo(_tourContainer);

		// set content for scrolled composite
		sc.setContent(_tourContainer);

		_tk.setBorderStyle(SWT.BORDER);

		createUISection110Title(_tourContainer);
		createUISectionSeparator(_tourContainer);

		createUISection120DateTime(_tourContainer);
		createUISectionSeparator(_tourContainer);

		createUISection130Personal(_tourContainer);
		createUISectionSeparator(_tourContainer);

		createUISection140Weather(_tourContainer);
		createUISectionSeparator(_tourContainer);

		createUISection150Characteristics(_tourContainer);

		// compute width for all controls and equalize column width for the different sections
		sc.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnControls);
		UI.setEqualizeColumWidths(_secondColumnControls);

		sc.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnContainerControls);

		return sc;
	}

	/**
	 * @param parent
	 * @return returns the controls for the tab
	 */
	private Control createUITab20Marker(final Composite parent) {

		final Composite markerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(markerContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(markerContainer);

		_markerViewerContainer = new Composite(markerContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_markerViewerContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_markerViewerContainer);

		createMarkerViewer(_markerViewerContainer);

		return markerContainer;
	}

	/**
	 * @param parent
	 * @return returns the controls for the tab
	 */
	private Control createUITab30Slices(final Composite parent) {

		_sliceContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_sliceContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_sliceContainer);

		_sliceViewerContainer = new Composite(_sliceContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_sliceViewerContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_sliceViewerContainer);

		createSliceViewer(_sliceViewerContainer);

		_timeSliceLabel = new Label(_sliceContainer, SWT.WRAP);
		_timeSliceLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		_timeSliceLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		_timeSliceLabel.setVisible(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_timeSliceLabel);

		return _sliceContainer;
	}

	private Composite createUITab40Info(final Composite parent) {

		/*
		 * scrolled container
		 */
		_scrolledTabInfo = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		_scrolledTabInfo.setExpandVertical(true);
		_scrolledTabInfo.setExpandHorizontal(true);
		_scrolledTabInfo.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeTabInfo();
			}
		});

		_infoContainer = new Composite(_scrolledTabInfo, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_infoContainer);
		_tk.adapt(_infoContainer);
		GridLayoutFactory.swtDefaults().applyTo(_infoContainer);

		// set content for scrolled composite
		_scrolledTabInfo.setContent(_infoContainer);

		createUISection410Info(_infoContainer);

		return _scrolledTabInfo;
	}

	private void defineMarkerViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		ColumnDefinition colDef;

		/*
		 * column: time
		 */
		colDef = TableColumnFactory.TOUR_TIME_HH_MM_SS.createColumn(_markerColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(UI.format_hh_mm_ss(((TourMarker) cell.getElement()).getTime()));
			}
		});

		/*
		 * column: distance
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(_markerColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker marker = (TourMarker) cell.getElement();

				cell.setText(_nf3.format((marker.getDistance()) / (1000 * _unitValueDistance)));

				if (marker.getType() == ChartLabel.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});

		/*
		 * column: marker
		 */
		_colDefMarker = colDef = TableColumnFactory.MARKER.createColumn(_markerColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourMarker tourMarker = (TourMarker) cell.getElement();

				cell.setText(tourMarker.getLabel());
			}
		});
	}

	private void defineSliceViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		ColumnDefinition colDef;

		/*
		 * 1. column will be hidden because the alignment for the first column is always to the left
		 */
		colDef = TableColumnFactory.FIRST_COLUMN.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setHideColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {}
		});

		/*
		 * column: #
		 */
		colDef = TableColumnFactory.SEQUENCE.createColumn(_sliceColumnManager, pixelConverter);
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

		/*
		 * column: time hh:mm:ss
		 */
		colDef = TableColumnFactory.TOUR_TIME_HH_MM_SS.createColumn(_sliceColumnManager, pixelConverter);
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

		/*
		 * column: time in seconds
		 */
		colDef = TableColumnFactory.TOUR_TIME.createColumn(_sliceColumnManager, pixelConverter);
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

		/*
		 * column: distance
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (_serieDistance != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;

					final float distance = ((float) _serieDistance[serieIndex]) / 1000 / _unitValueDistance;

					cell.setText(_nf3.format(distance));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: altitude
		 */
		_colDefAltitude = colDef = TableColumnFactory.ALTITUDE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieAltitude != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString((int) (_serieAltitude[timeSlice.serieIndex] / _unitValueAltitude)));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: gradient
		 */
		colDef = TableColumnFactory.GRADIENT.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();

					cell.setText(_nf1.format((float) _serieGradient[timeSlice.serieIndex] / 10));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: pulse
		 */
		_colDefPulse = colDef = TableColumnFactory.PULSE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_seriePulse != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(_seriePulse[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: marker
		 */
		_colDefSliceMarker = colDef = TableColumnFactory.MARKER.createColumn(_sliceColumnManager, pixelConverter);
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

		/*
		 * column: temperature
		 */
		_colDefTemperature = colDef = TableColumnFactory.TEMPERATURE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieTemperature != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int metricTemperature = _serieTemperature[timeSlice.serieIndex];

					if (_unitValueTemperature != 1) {

						// use imperial system

						final int imperialTemp = (int) (metricTemperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
						cell.setText(Integer.toString(imperialTemp));

					} else {

						// use metric system
						cell.setText(Integer.toString(metricTemperature));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: cadence
		 */
		_colDefCadence = colDef = TableColumnFactory.CADENCE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieCadence != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(_serieCadence[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.SPEED.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieSpeed != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final float speed = (float) _serieSpeed[timeSlice.serieIndex] / 10;

					cell.setText(_nf1.format(speed));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: pace
		 */
		colDef = TableColumnFactory.PACE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_seriePace == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final long pace = _seriePace[timeSlice.serieIndex];

					cell.setText(UI.format_mm_ss(pace));
				}
			}
		});

		/*
		 * column: power
		 */
		colDef = TableColumnFactory.POWER.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_seriePower != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(_seriePower[timeSlice.serieIndex]));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: longitude
		 */
		_colDefLongitude = colDef = TableColumnFactory.LONGITUDE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieLongitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Double.toString(_serieLongitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: latitude
		 */
		_colDefLatitude = colDef = TableColumnFactory.LATITUDE.createColumn(_sliceColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (_serieLatitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Double.toString(_serieLatitude[timeSlice.serieIndex]));
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

		updateUIFromModel(_tourData, true, true);

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
	 * tries to get tour data from the last selection or from a tour provider
	 */
	private void displaySelectedTour() {

		// show tour from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {

			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {

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
			updateUIFromModel(tourData, false, true);
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
		updateUIFromModel(tourData, true, true);
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(_postSelectionListener);
		page.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

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
	public void doSave(final IProgressMonitor monitor) {}

	public void doSaveAs() {}

	private void enableActions() {

		final boolean isTourInDb = isTourInDb();
		final boolean isTourValid = isTourValid() && isTourInDb;
		final boolean isNotManualTour = _isManualTour == false;
		final boolean canEdit = _isEditMode && isTourInDb();

		// all actions are disabled when a cell editor is activated
		final boolean isCellEditorInactive = _isCellEditorActive == false;

		final CTabItem selectedTab = _tabFolder.getSelection();
		final boolean isTableViewerTab = (selectedTab == _tabSlices) || (selectedTab == _tabMarker);
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

		_actionToggleRowSelectMode.setEnabled(isCellEditorInactive
				&& isTableViewerTab
				&& isTourValid
				&& (_isManualTour == false));
		_actionToggleReadEditMode.setEnabled(isCellEditorInactive && isTourInDb);

		_actionModifyColumns.setEnabled(isCellEditorInactive && isTableViewerTab);// && isTourValid);

		_actionSetStartDistanceTo0.setEnabled(isCellEditorInactive
				&& isNotManualTour
				&& canEdit
				&& isDistanceLargerThan0);
		_actionDeleteDistanceValues.setEnabled(isCellEditorInactive
				&& isNotManualTour
				&& canEdit
				&& isDistanceAvailable);
		_actionComputeDistanceValues.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isGeoAvailable);
	}

	/**
	 * Dlay enable/disable actions.
	 * <p>
	 * When a user traverses the edit fields in a viewer the actions are enabled and disable which
	 * flickers the UI, therefor it is delayed.
	 */
	private void enableActionsDelayed() {

		_enableActionCounter++;

		final UIJob uiJob = new UIJob(UI.EMPTY_STRING) {

			final int	__runnableCounter	= _enableActionCounter;

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
		final boolean isNotManualTour = _isManualTour == false;

		_txtTitle.setEnabled(canEdit);
		_txtDescription.setEnabled(canEdit);

		_txtStartLocation.setEnabled(canEdit);
		_txtEndLocation.setEnabled(canEdit);

		_spinRestPuls.setEnabled(canEdit);

		_spinTemperature.setEnabled(canEdit && (_tourData.temperatureSerie == null));
		_comboClouds.setEnabled(canEdit);
		_spinWindDirectionValue.setEnabled(canEdit);
		_spinWindSpeedValue.setEnabled(canEdit);
		_comboWindDirectionText.setEnabled(canEdit);
		_comboWindSpeedText.setEnabled(canEdit);

		_dtTourDate.setEnabled(canEdit);
		_dtStartTime.setEnabled(canEdit);

		_dtRecordingTime.setEnabled(canEdit && _isManualTour);
		_dtDrivingTime.setEnabled(canEdit && _isManualTour);
		_dtPausedTime.setEnabled(canEdit && _isManualTour);

		_txtTourDistance.setEnabled(canEdit && _isManualTour);

		_spinTourCalories.setEnabled(canEdit && _isManualTour);

		_linkTag.setEnabled(canEdit);
		_linkTourType.setEnabled(canEdit);

		_sliceViewer.getTable().setEnabled(isNotManualTour);
		_markerViewer.getTable().setEnabled(isNotManualTour);
	}

	/**
	 * enable actions
	 */
	private void enableSliceActions() {

		final StructuredSelection sliceSelection = (StructuredSelection) _sliceViewer.getSelection();

		final boolean isOneSliceSelected = sliceSelection.size() == 1;
		final boolean isSliceSelected = sliceSelection.size() > 0;
		final boolean isTourInDb = isTourInDb();

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
		_actionOpenMarkerDialog.setSelectedMarker(selectedMarker);

		_actionDeleteTimeSlicesRemoveTime.setEnabled(_isEditMode && isTourInDb && isSliceSelected);
		_actionDeleteTimeSlicesKeepTime.setEnabled(_isEditMode && isTourInDb && isSliceSelected);

		_actionExportTour.setEnabled(true);
		_actionCsvTimeSliceExport.setEnabled(isSliceSelected);

		_actionSplitTour.setEnabled(isOneSliceSelected);
		_actionExtractTour.setEnabled(sliceSelection.size() >= 2);

		// set start/end position into the actions
		if (isSliceSelected) {

			final Object[] sliceArray = sliceSelection.toArray();
			final TimeSlice firstTimeSlice = (TimeSlice) sliceArray[0];
			final TimeSlice lastTimeSlice = (TimeSlice) sliceArray[sliceArray.length - 1];

			_actionExportTour.setTourRange(firstTimeSlice.serieIndex, lastTimeSlice.serieIndex);

			_actionSplitTour.setTourRange(firstTimeSlice.serieIndex);
			_actionExtractTour.setTourRange(firstTimeSlice.serieIndex, lastTimeSlice.serieIndex);
		}
	}

	private void fillMarkerContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionOpenMarkerDialog);

		menuMgr.add(new Separator());
		menuMgr.add(_actionDeleteTourMarker);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		// set marker which should be selected in the marker dialog
		final StructuredSelection markerSelection = (StructuredSelection) _markerViewer.getSelection();
		_actionOpenMarkerDialog.setSelectedMarker((TourMarker) markerSelection.getFirstElement());

		/*
		 * enable actions
		 */
		final boolean isMarkerSelected = markerSelection.size() > 0;
		final boolean isTourInDb = isTourInDb();

		_actionOpenMarkerDialog.setEnabled(_isEditMode && isTourInDb);
		_actionDeleteTourMarker.setEnabled(_isEditMode && isTourInDb && isMarkerSelected);
	}

	private void fillSliceContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionCreateTourMarker);
		menuMgr.add(_actionOpenMarkerDialog);

		menuMgr.add(new Separator());
		menuMgr.add(_actionDeleteTimeSlicesRemoveTime);
		menuMgr.add(_actionDeleteTimeSlicesKeepTime);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSplitTour);
		menuMgr.add(_actionExtractTour);
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionCsvTimeSliceExport);

		enableSliceActions();
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

		tbm.update(true);

		/*
		 * fill toolbar view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionUndoChanges);
		menuMgr.add(_actionSetStartDistanceTo0);
		menuMgr.add(_actionDeleteDistanceValues);
		menuMgr.add(_actionComputeDistanceValues);
		menuMgr.add(new Separator());

		menuMgr.add(_actionModifyColumns);
	}

	/**
	 * fire notification for changed tour data
	 */
	private void fireModifyNotification() {

		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(_tourData);

		final TourEvent propertyData = new TourEvent(modifiedTour);
		propertyData.isTourModified = true;

		TourManager.fireEvent(TourEventId.TOUR_CHANGED, propertyData, TourDataEditorView.this);
	}

	/**
	 * fire notification for the reverted tour data
	 */
	private void fireRevertNotification() {

		final TourEvent tourEvent = new TourEvent(_tourData);
		tourEvent.isReverted = true;

		TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent, TourDataEditorView.this);
	}

//	@Override
//	public Object getAdapter(final Class adapter) {
//
//		if (adapter == ColumnViewer.class) {
//			return _sliceViewer;
//		}
//
//		return Platform.getAdapterManager().getAdapter(this, adapter);
//	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 * 
	 * @return
	 */
	private ISelection fireSliderPosition(final StructuredSelection selection) {

		final Object[] selectedData = selection.toArray();
		if ((selectedData == null) || (selectedData.length == 0)) {
			return null;
		}

		if (_tourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if ((tourChart != null) && (tourChart.isDisposed() == false)) {
				_tourChart = tourChart;
			}
		}

		final Object firstItem = selectedData[0];

		int serieIndex1 = -1;
		int serieIndex2 = -1;

		if (selectedData.length > 1) {

			// two or more data are selected, set the 2 sliders to the first and last selected data

			if (firstItem instanceof TimeSlice) {

				serieIndex1 = ((TimeSlice) firstItem).serieIndex;
				serieIndex2 = ((TimeSlice) selectedData[selectedData.length - 1]).serieIndex;

			} else if (firstItem instanceof TourMarker) {

				serieIndex1 = ((TourMarker) firstItem).getSerieIndex();
				serieIndex2 = ((TourMarker) selectedData[selectedData.length - 1]).getSerieIndex();
			}

		} else if (selectedData.length > 0) {

			// one data is selected

			if (firstItem instanceof TimeSlice) {

				serieIndex1 = ((TimeSlice) firstItem).serieIndex;
				serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

			} else if (firstItem instanceof TourMarker) {

				serieIndex1 = ((TourMarker) firstItem).getSerieIndex();
				serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
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
				sliderSelection = new SelectionChartXSliderPosition(_tourChart, serieIndex1, serieIndex2, true);
			}

			_postSelectionProvider.setSelection(sliderSelection);
		}

		return sliderSelection;
	}

	public ColumnManager getColumnManager() {

		final CTabItem selectedTab = _tabFolder.getSelection();

		if (selectedTab == _tabSlices) {
			return _sliceColumnManager;
		} else if (selectedTab == _tabMarker) {
			return _markerColumnManager;
		}

		return null;
	}

	private void getDataSeriesFromTourData() {

		_serieTime = _tourData.timeSerie;

		_serieDistance = _tourData.distanceSerie;
		_serieAltitude = _tourData.altitudeSerie;
		_serieTemperature = _tourData.temperatureSerie;

		_serieCadence = _tourData.cadenceSerie;
		_seriePulse = _tourData.pulseSerie;

		_serieLatitude = _tourData.latitudeSerie;
		_serieLongitude = _tourData.longitudeSerie;

		_serieGradient = _tourData.getGradientSerie();
		_serieSpeed = _tourData.getSpeedSerie();
		_seriePace = _tourData.getPaceSerieSeconds();
		_seriePower = _tourData.getPowerSerie();

		_altitudeEditingSupport.setDataSerie(_serieAltitude);
		_temperatureEditingSupport.setDataSerie(_serieTemperature);
		_pulseEditingSupport.setDataSerie(_seriePulse);
		_cadenceEditingSupport.setDataSerie(_serieCadence);
		_latitudeEditingSupport.setDataSerie(_serieLatitude);
		_longitudeEditingSupport.setDataSerie(_serieLongitude);

		if (_isManualTour == false) {

			if ((_serieTime == null) || (_serieTime.length == 0)) {
				_tourData.setTourRecordingTime(0);
			} else {
				_tourData.setTourRecordingTime(_serieTime[_serieTime.length - 1]);
			}
			_tourData.computeTourDrivingTime();

			if ((_serieDistance == null) || (_serieDistance.length == 0)) {
				_tourData.setTourDistance(0);
			} else {
				_tourData.setTourDistance(_serieDistance[_serieDistance.length - 1]);
			}

			_tourData.computeComputedValues();
		}
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

	private TimeSlice[] getRemainingSliceItems(final Object[] dataViewerItems, final int firstIndex, final int lastIndex) {

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

	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();
		tourDataList.add(_tourData);

		return tourDataList;
	}

	TableViewer getSliceViewer() {
		return _sliceViewer;
	}

	private Object[] getSliceViewerItems() {

		if ((_tourData == null) || (_tourData.timeSerie == null) || (_tourData.timeSerie.length == 0)) {
			return new Object[0];
		}

		getDataSeriesFromTourData();

		/*
		 * create viewer elements (time slices), each viewer item contains the index into the data
		 * series
		 */
		final TimeSlice[] viewerItems = new TimeSlice[_tourData.timeSerie.length];
		for (int serieIndex = 0; serieIndex < viewerItems.length; serieIndex++) {
			viewerItems[serieIndex] = new TimeSlice(serieIndex);
		}

		if (viewerItems.length == 0) {
			return viewerItems;
		}
		return viewerItems;
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
	public String getTourTitle() {
		return TourManager.getTourTitle(_tourData);
	}

	public ColumnViewer getViewer() {

		final CTabItem selectedTab = _tabFolder.getSelection();

		if (selectedTab == _tabSlices) {
			return _sliceViewer;
		} else if (selectedTab == _tabMarker) {
			return _markerViewer;
		}

		return null;
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

	/**
	 * @return Returns <code>true</code> when the data have been modified and not saved, returns
	 *         <code>false</code> when tour is not modified or {@link TourData} is <code>null</code>
	 */
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

	public boolean isSaveAsAllowed() {
		return false;
	}

	public boolean isSaveOnCloseNeeded() {
		return isDirty();
	}

	/**
	 * @return <code>true</code> when the tour is saved in the database or when a manual tour is
	 *         created which also contains a person
	 */
	private boolean isTourInDb() {

		if (_tourData != null && _tourData.getTourPerson() != null) {
			return true;
		}

		return false;
	}

	/**
	 * Checks the selection if it contains the current tour, {@link #_selectionTourId} contains the
	 * tour id which is within the selection
	 * 
	 * @param selection
	 * @return Returns <code>true</code> when the current tour is within the selection
	 */
	private boolean isTourInSelection(final ISelection selection) {

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

		} else if (selection instanceof SelectionChartInfo) {

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
			final ChartDataModel chartDataModel = chartInfo.chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
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

					final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
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
				System.out.println("Selection:" + selection);//$NON-NLS-1$
				e.printStackTrace();
			}
		}

		return isCurrentTourSelected;
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
						IMessageProvider.ERROR,
						_txtPerson);

			} else {
				_messageManager.removeMessage(WIDGET_KEY_PERSON, _txtPerson);
			}

			// tour is valid when there are no error messages

			return _messageManager.getErrorMessageCount() == 0;

		} else {

			// tour is not dirty

			return true;
		}
	}

	/**
	 * fires a slider position for the marker viewer and select the corresponding time slice
	 */
	private void onMarkerViewerSelectionChanged() {

		final StructuredSelection selection = (StructuredSelection) _markerViewer.getSelection();
		if (selection != null) {

			final ISelection sliderSelection = fireSliderPosition(selection);
			if (sliderSelection instanceof SelectionChartXSliderPosition) {

				final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) sliderSelection;

				// position slice viewer to the marker position

				// keep position for the slice viewer when it was not yet displayed for the current tour
				_sliceViewerXSliderPosition = xSliderPosition;

				if (_sliceViewerTourId == _tourData.getTourId()) {
					selectTimeSlice(xSliderPosition);
				}
			}
		}
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

	private void onResizeTabInfo() {

		// horizontal scroll bar ishidden, only the vertical scrollbar can be displayed
		int infoContainerWidth = _scrolledTabInfo.getBounds().width;
		final ScrollBar vertBar = _scrolledTabInfo.getVerticalBar();
		if (vertBar != null) {
			// vertical bar is displayed
			infoContainerWidth -= vertBar.getSize().x;
		}

		final Point minSize = _infoContainer.computeSize(infoContainerWidth, SWT.DEFAULT);

		_scrolledTabInfo.setMinSize(minSize);
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

		if (isTourInSelection(selection)) {

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
					 * setting the message causes an layout and this is EXTREMLY SLOW because of the
					 * bad date time controls
					 */

					// hide title
					_pageEditorForm.setText(UI.EMPTY_STRING);

					// show info
					_messageManager.addMessage(
							MESSAGE_KEY_ANOTHER_SELECTION,
							NLS.bind(Messages.tour_editor_message_show_another_tour, getTourTitle()),
							null,
							IMessageProvider.WARNING);

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
				updateUIFromModel(tourData, false, true);
			}

		} else if (selection instanceof SelectionTourId) {

			displayTour(((SelectionTourId) selection).getTourId());

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				displayTour(tourIds.get(0));
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				displayTour(refItem.getTourId());
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData == null) {

							_tourData = tourData;
							_tourChart = null;
							updateUIFromModel(tourData, false, true);

						} else {

							if (_tourData.getTourId().equals(tourData.getTourId())) {

								// a new tour id is in the selection
								_tourData = tourData;
								_tourChart = null;
								updateUIFromModel(tourData, false, true);
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

	private void onSelectTab() {

		if (_tabFolder.getSelection() == _tabSlices) {

			if (_sliceViewerTourId == -1L) {

				// load viewer when this is was not yet done
				_sliceViewerTourId = _tourData.getTourId();

				reloadViewer();
				updateStatusLine();

				// run asynch because relaodViewer is also running asynch
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						selectTimeSlice(_sliceViewerXSliderPosition);
						_sliceViewer.getTable().setFocus();
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

		final boolean isBackup = _isDirtyDisabled;
		_isDirtyDisabled = true;
		{
			_spinWindSpeedValue.setSelection(speed);
		}
		_isDirtyDisabled = isBackup;
	}

	private void onSelectWindSpeedValue() {

		_isWindSpeedManuallyModified = true;

		final int windSpeed = _spinWindSpeedValue.getSelection();

		final boolean isBackup = _isDirtyDisabled;
		_isDirtyDisabled = true;
		{
			_comboWindSpeedText.select(getWindSpeedTextIndex(windSpeed));
		}
		_isDirtyDisabled = isBackup;
	}

	/*
	 * this method is called when the application is shut down to save dirty tours or to cancel the
	 * shutdown
	 * @see org.eclipse.ui.ISaveablePart2#promptToSaveOnClose()
	 */
	public int promptToSaveOnClose() {

		int returnCode;

		if (_isTourDirty == false) {
			returnCode = ISaveablePart2.NO;
		}

		_isSavingInProgress = true;
		{
			if (saveTourValidation()) {
				returnCode = ISaveablePart2.NO;
			} else {
				returnCode = ISaveablePart2.CANCEL;
			}
		}
		_isSavingInProgress = false;

		return returnCode;
	}

	private void recreateViewer() {

		// recreate slice viewer
		_sliceColumnManager.saveState(_viewStateSlice);
		_sliceColumnManager.clearColumns();

		defineSliceViewerColumns(_sliceViewerContainer);
		_sliceViewer = (TableViewer) recreateViewer(_sliceViewer);

		// recreate marker viewer
		_markerColumnManager.saveState(_viewStateMarker);
		_markerColumnManager.clearColumns();

		defineMarkerViewerColumns(_markerViewerContainer);
		_markerViewer = (TableViewer) recreateViewer(_markerViewer);
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		final ColumnViewer[] newColumnViewer = new ColumnViewer[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			private void recreateMarkerViewer() {

				// preserve column width, selection and focus
				final ISelection selection = _markerViewer.getSelection();

				final Table table = _markerViewer.getTable();
				final boolean isFocus = table.isFocusControl();

				_markerViewerContainer.setRedraw(false);
				{
					_markerViewerContainer.getChildren()[0].dispose();

					createMarkerViewer(_markerViewerContainer);

					_markerViewerContainer.layout();

					// update the viewer
					_markerViewer.setInput(new Object[0]);
				}
				_markerViewerContainer.setRedraw(true);

				_markerViewer.setSelection(selection, true);
				if (isFocus) {
					_markerViewer.getTable().setFocus();
				}

				newColumnViewer[0] = _markerViewer;
			}

			private void recreateSliceViewer() {

				// preserve column width, selection and focus
				final ISelection selection = _sliceViewer.getSelection();

				final Table table = _sliceViewer.getTable();
				final boolean isFocus = table.isFocusControl();

				_sliceViewerContainer.setRedraw(false);
				{
					table.dispose();

					createSliceViewer(_sliceViewerContainer);

					_sliceViewerContainer.layout();

					// update the viewer
					_sliceViewerItems = getSliceViewerItems();
					_sliceViewer.setInput(_sliceViewerItems);
				}
				_sliceViewerContainer.setRedraw(true);

				_sliceViewer.setSelection(selection, true);
				if (isFocus) {
					_sliceViewer.getTable().setFocus();
				}

				newColumnViewer[0] = _sliceViewer;
			}

			public void run() {

				if (columnViewer == _sliceViewer) {

					recreateSliceViewer();

				} else if (columnViewer == _markerViewer) {

					recreateMarkerViewer();
				}
			}
		});

		return newColumnViewer[0];
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

	/**
	 * reload the content of the viewer
	 */
	public void reloadViewer() {

		Display.getCurrent().asyncExec(new Runnable() {

			private void reloadMarkerViewer() {

				final ISelection previousSelection = _markerViewer.getSelection();

				final Table table = _markerViewer.getTable();
				if (table.isDisposed()) {
					return;
				}

				table.setRedraw(false);
				{
					_markerViewer.setInput(new Object[0]);
					_markerViewer.setSelection(previousSelection, true);
				}
				table.setRedraw(true);
			}

			private void reloadSliceViewer() {

				final ISelection previousSelection = _sliceViewer.getSelection();

				final Table table = _sliceViewer.getTable();
				if (table.isDisposed()) {
					return;
				}

				table.setRedraw(false);
				{
					/*
					 * update the viewer, show busy indicator when it's a large tour or the previous
					 * tour was large because it takes time to remove the old items
					 */
					if (((_tourData != null) && (_tourData.timeSerie != null) && (_tourData.timeSerie.length > BUSY_INDICATOR_ITEMS))
							|| (table.getItemCount() > BUSY_INDICATOR_ITEMS)) {

						BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
							public void run() {
								_sliceViewerItems = getSliceViewerItems();
								_sliceViewer.setInput(_sliceViewerItems);
							}
						});
					} else {
						_sliceViewerItems = getSliceViewerItems();
						_sliceViewer.setInput(_sliceViewerItems);
					}

					_sliceViewer.setSelection(previousSelection, true);
				}
				table.setRedraw(true);
			}

			public void run() {

				final CTabItem selectedTab = _tabFolder.getSelection();

				if (selectedTab == _tabSlices) {
					reloadSliceViewer();
				} else if (selectedTab == _tabMarker) {
					reloadMarkerViewer();
				}

			}
		});
	}

	private void restoreStateBeforeUI() {

		_isRowEditMode = _viewState.getBoolean(STATE_ROW_EDIT_MODE);
		_isEditMode = _viewState.getBoolean(STATE_IS_EDIT_MODE);
	}

	private void restoreStateWithUI() {

		// select tab
		try {
			_tabFolder.setSelection(_viewState.getInt(STATE_SELECTED_TAB));
		} catch (final NumberFormatException e) {
			_tabFolder.setSelection(_tabTour);
		}

		_actionToggleRowSelectMode.setChecked(_isRowEditMode);
		_actionToggleReadEditMode.setChecked(_isEditMode);

		_actionSetStartDistanceTo0.setText(NLS.bind(
				Messages.TourEditor_Action_SetStartDistanceTo0,
				UI.UNIT_LABEL_DISTANCE));
	}

	private void saveState() {

		// selected tab
		_viewState.put(STATE_SELECTED_TAB, _tabFolder.getSelectionIndex());

		// row/column edit mode
		_viewState.put(STATE_IS_EDIT_MODE, _actionToggleReadEditMode.isChecked());
		_viewState.put(STATE_ROW_EDIT_MODE, _actionToggleRowSelectMode.isChecked());

		// viewer state
		_sliceColumnManager.saveState(_viewStateSlice);
		_markerColumnManager.saveState(_viewStateMarker);
	}

	/**
	 * @param isConfirmSave
	 * @return Returns <code>true</code> when the tour was saved, <code>false</code> when the tour
	 *         is not saved but canceled
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
		 * saveTour will check the tour editor dirty state, but when the tour is saved the dirty
		 * flag can be set before to prevent an out of synch error
		 */
		_isTourDirty = false;

		_tourData = TourDatabase.saveTour(_tourData);
		updateMarkerMap();

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
	private boolean saveTourValidation() {

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

		final Table table = (Table) _sliceViewer.getControl();
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

		final Table table = (Table) _sliceViewer.getControl();
		final int itemCount = table.getItemCount();

		final int valueIndex1 = sliderPosition.getLeftSliderValueIndex();
		final int valueIndex2 = sliderPosition.getRightSliderValueIndex();

		// adjust to array bounds
		final int checkedValueIndex1 = Math.max(0, Math.min(valueIndex1, itemCount - 1));
		final int checkedValueIndex2 = Math.max(0, Math.min(valueIndex2, itemCount - 1));

		if ((valueIndex1 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION)
				&& (valueIndex1 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION)) {
			return;
		}

		if (valueIndex1 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			table.setSelection(checkedValueIndex2);
		} else if (valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			table.setSelection(checkedValueIndex1);
		} else {
			table.setSelection(checkedValueIndex1, checkedValueIndex2);
		}

		table.showSelection();
	}

	/**
	 * initialize cell editing
	 * 
	 * @param viewer
	 */
	private void setCellEditSupport(final TableViewer viewer) {

		final TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(
				viewer,
				new FocusCellOwnerDrawHighlighter(viewer));

		final ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
			@Override
			protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {

				return (event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL)
						|| (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION)
						|| ((event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED) && (event.keyCode == SWT.CR))
						|| (event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC);
			}
		};

		TableViewerEditor.create(//
				viewer,
				focusCellManager,
				actSupport,
				ColumnViewerEditor.TABBING_HORIZONTAL //
						| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR //
						| ColumnViewerEditor.TABBING_VERTICAL
						| ColumnViewerEditor.KEYBOARD_ACTIVATION);
	}

	@Override
	public void setFocus() {

// !!! disabled because the first field gets the focus !!!
//		fTabFolder.setFocus();

		_pageEditorForm.setFocus();
	}

	/**
	 * removes the dirty state from the tour editor, updates the save/undo actions and updates the
	 * part name
	 */
	private void setTourClean() {

		_isTourDirty = false;

		_isDistManuallyModified = false;
		_isWindSpeedManuallyModified = false;
		_isTemperatureManuallyModified = false;

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
		updateUIFromModel(tourDataForEditor, false, true);
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
		 * this is not an eclipse editor part but the property change must be fired to show the
		 * start "*" marker in the part name
		 */
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * show the default title in the editor
	 */
	private void showDefaultTitle() {

		_messageManager.removeMessage(MESSAGE_KEY_ANOTHER_SELECTION);
		updateUITitle();

		_isInfoInTitle = false;
	}

	private void updateInternalUnitValues() {

		_unitValueDistance = UI.UNIT_VALUE_DISTANCE;
		_unitValueAltitude = UI.UNIT_VALUE_ALTITUDE;
		_unitValueTemperature = UI.UNIT_VALUE_TEMPERATURE;

		_unitValueWindSpeed = UI.UNIT_VALUE_DISTANCE == 1 ? IWeather.windSpeedKmh : IWeather.windSpeedMph;
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
	 * update {@link TourData} from the UI fields
	 */
	private void updateModelFromUI() {

		try {

			_tourData.setTourTitle(_txtTitle.getText());
			_tourData.setTourDescription(_txtDescription.getText());

			_tourData.setTourStartPlace(_txtStartLocation.getText());
			_tourData.setTourEndPlace(_txtEndLocation.getText());

			_tourData.setRestPulse(_spinRestPuls.getSelection());
			_tourData.setCalories(_spinTourCalories.getSelection());

			_tourData.setWeatherWindDir(_spinWindDirectionValue.getSelection());
			if (_isWindSpeedManuallyModified) {
				/*
				 * update the speed only when it was modified because when the measurement is
				 * changed when the tour is being modified then the computation of the speed
				 * value can cause rounding errors
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
				int temperature = _spinTemperature.getSelection();
				if (_unitValueTemperature != 1) {
					temperature = (int) ((temperature - UI.UNIT_FAHRENHEIT_ADD) / UI.UNIT_FAHRENHEIT_MULTI);
				}
				_tourData.setAvgTemperature(temperature);
			}

			_tourData.setStartYear((short) _dtTourDate.getYear());
			_tourData.setStartMonth((short) (_dtTourDate.getMonth() + 1));
			_tourData.setStartDay((short) _dtTourDate.getDay());
			_tourData.setWeek(_tourData.getStartYear(), _tourData.getStartMonth(), _tourData.getStartDay());

			_tourData.setStartHour((short) _dtStartTime.getHours());
			_tourData.setStartMinute((short) _dtStartTime.getMinutes());
			_tourData.setStartSecond((short) _dtStartTime.getSeconds());

			if (_isDistManuallyModified) {
				/*
				 * update the distance only when it was modified because when the measurement is
				 * changed when the tour is being modified then the computation of the distance
				 * value can cause rounding errors
				 */
				final float distanceValue = getFloatValue(_txtTourDistance.getText()) * _unitValueDistance * 1000;
				_tourData.setTourDistance((int) distanceValue);
			}

			if (_isManualTour) {

				_tourData.setTourRecordingTime((_dtRecordingTime.getHours() * 3600)
						+ (_dtRecordingTime.getMinutes() * 60)
						+ _dtRecordingTime.getSeconds());

				_tourData.setTourDrivingTime((_dtDrivingTime.getHours() * 3600)
						+ (_dtDrivingTime.getMinutes() * 60)
						+ _dtDrivingTime.getSeconds());
			}

		} catch (final IllegalArgumentException e) {

			// this should not happen (but it happend when developing the tour data editor :-)
			//
			// wrong characters are entered, display an error message

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", e.getLocalizedMessage());//$NON-NLS-1$

			e.printStackTrace();
		}
	}

	private void updateRefTourInfo(final Collection<TourReference> refTours) {

		final ArrayList<TourReference> refTourList = new ArrayList<TourReference>(refTours);

		// sort reference tours by start index
		Collections.sort(refTourList, new Comparator<TourReference>() {
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

		_txtRefTour.setText(sb.toString());
		_txtRefTour.pack(true);
	}

	private void updateStatusLine() {

		final boolean isVisible = _timeSliceLabel.isVisible();
		boolean setVisible = false;

		if (_isReferenceTourAvailable) {

			// tour contains reference tours

			_timeSliceLabel.setText(Messages.TourDataEditorView_tour_editor_status_tour_contains_ref_tour);
			setVisible = true;

		} else {

			_timeSliceLabel.setText(UI.EMPTY_STRING);
		}

		if (isVisible != setVisible) {

			// changes visibility

			_timeSliceLabel.setVisible(setVisible);

			_sliceContainer.layout(true, true);
		}
	}

	/**
	 * Updates the UI from {@link TourData}, dirty flag is not set
	 * 
	 * @param tourData
	 */
	public void updateUI(final TourData tourData) {

		updateUIFromModel(tourData, true, true);
	}

	/**
	 * @param tourData
	 * @param isDirty
	 *            When <code>true</code>, the tour is set to be dirty
	 */
	public void updateUI(final TourData tourData, final boolean isDirty) {

		updateUI(tourData);

		if (isDirty) {
			setTourDirty();
		}
	}

	private void updateUIAfterDistanceModifications() {

		updateUIAfterSliceEdit();

		// update distance in the UI, this must be done after updateUIAfterSliceEdit()
		updateUITab1Tour();

		updateUITab2Marker();

		/*
		 * set slice viewer dirty when the time slice tab is not selected -> slice viewer was not
		 * updated in updateUIAfterSliceEdit()
		 */
		if (_tabFolder.getSelection() != _tabSlices) {
			_sliceViewerTourId = -1;
		}
	}

	private void updateUIAfterSliceEdit() {

		setTourDirty();

		_tourData.clearComputedSeries();
		getDataSeriesFromTourData();

		// refresh the whole viewer because the computed data series could have been changed
		final ColumnViewer viewer = getViewer();
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
	 * @param forceTimeSliceReload
	 *            <code>true</code> will reload time slices
	 * @param isDirtyDisabled
	 */
	private void updateUIFromModel(	final TourData tourData,
									final boolean forceTimeSliceReload,
									final boolean isDirtyDisabled) {

		if (tourData == null) {
			_pageBook.showPage(_pageNoTour);
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

			final int	_runnableCounter	= _uiUpdateCounter;

			public void run() {

				/*
				 * update the UI
				 */

				// check if this is the last runnable
				if (_runnableCounter != _uiUpdateCounter) {
					// a new runnable was created
					return;
				}

				_uiRunnableTourData = tourData;
				_uiRunnableForceTimeSliceReload = forceTimeSliceReload;
				_uiRunnableIsDirtyDisabled = isDirtyDisabled;

				// force reload
				_uiRunnableCounter = _uiUpdateCounter - 1;

				if (_isPartVisible) {
					updateUIFromModelRunnable();
				}
			}
		});
	}

	private void updateUIFromModelRunnable() {

		if (_uiRunnableCounter == _uiUpdateCounter) {
			// UI is updated
			return;
		}

		_uiRunnableCounter = _uiUpdateCounter;

		if (_pageEditorForm.isDisposed() || (_uiRunnableTourData == null)) {
			// widget is disposed or data is not set
			return;
		}

		_isDirtyDisabled = _uiRunnableIsDirtyDisabled;

		// keep tour data
		_tourData = _uiRunnableTourData;

		updateMarkerMap();

		// a tour which is not saved has no tour references
		_isReferenceTourAvailable = _tourData.isContainReferenceTour();

		// show tour type image when tour type is set
		final TourType tourType = _uiRunnableTourData.getTourType();
		if (tourType == null) {
			_pageEditorForm.setImage(null);
		} else {
			_pageEditorForm.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
		}

		updateUITitleAsynch(getTourTitle());

		updateUITab1Tour();
		updateUITab2Marker();
		updateUITab3Slices();
		updateUITab4Info();

		enableActions();
		enableControls();

		// this action displays selected unit label
		_actionSetStartDistanceTo0.setText(NLS.bind(
				Messages.TourEditor_Action_SetStartDistanceTo0,
				UI.UNIT_LABEL_DISTANCE));

		// show editor page
		_pageBook.showPage(_pageEditorForm);

		_isDirtyDisabled = false;
	}

	private void updateUITab1Tour() {

		final short tourYear = _tourData.getStartYear();
		final int tourMonth = _tourData.getStartMonth() - 1;
		final short tourDay = _tourData.getStartDay();

		/*
		 * tour/event
		 */
		// title/description
		_txtTitle.setText(_tourData.getTourTitle());
		_txtDescription.setText(_tourData.getTourDescription());

		// start/end location
		_txtStartLocation.setText(_tourData.getTourStartPlace());
		_txtEndLocation.setText(_tourData.getTourEndPlace());

		/*
		 * personal details
		 */
		_spinRestPuls.setSelection(_tourData.getRestPulse());
		_spinTourCalories.setSelection(_tourData.getCalories());

		/*
		 * wind properties
		 */
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

		// temperature
		int temperature = _tourData.getAvgTemperature();
		if (_unitValueTemperature != 1) {
			temperature = (int) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
		}
		_spinTemperature.setSelection(temperature);

		// tour date
		_dtTourDate.setDate(tourYear, tourMonth, tourDay);

		// start time
		_dtStartTime.setTime(_tourData.getStartHour(), _tourData.getStartMinute(), _tourData.getStartSecond());

		// tour distance
		final int tourDistance = _tourData.getTourDistance();
		if (tourDistance == 0) {
			_txtTourDistance.setText(Integer.toString(tourDistance));
		} else {

			final float distance = ((float) tourDistance) / 1000 / _unitValueDistance;
			_txtTourDistance.setText(_nf3NoGroup.format(distance));

		}

		// tour time's
		final int recordingTime = _tourData.getTourRecordingTime();
		final int drivingTime = _tourData.getTourDrivingTime();
		final int pausedTime = recordingTime - drivingTime;

		_dtRecordingTime.setTime(recordingTime / 3600, ((recordingTime % 3600) / 60), ((recordingTime % 3600) % 60));
		_dtDrivingTime.setTime(drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));
		_dtPausedTime.setTime(pausedTime / 3600, ((pausedTime % 3600) / 60), ((pausedTime % 3600) % 60));

		// tour type/tags
		UI.updateUITourType(_tourData, _lblTourType, true);
		UI.updateUITags(_tourData, _lblTourTags);

		// measurement system
		_lblTourDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
		_lblTemperatureUnit.setText(UI.UNIT_LABEL_TEMPERATURE);
		_lblSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		/*
		 * layout container to resize labels
		 */
		_tourContainer.layout(true);

	}

	private void updateUITab2Marker() {

		_markerViewer.setInput(new Object[0]);
	}

	private void updateUITab3Slices() {

		if (_uiRunnableForceTimeSliceReload) {
			_sliceViewerTourId = -1L;
		}

		if ((_tabFolder.getSelection() == _tabSlices) && (_sliceViewerTourId != _tourData.getTourId())) {

			/*
			 * time slice tab is selected and the viewer is not yeat loaded
			 */

			reloadViewer();
			_sliceViewerTourId = _tourData.getTourId();

			updateStatusLine();

		} else {

			if (_sliceViewerTourId != _tourData.getTourId()) {
				// force reload when it's not yet loaded
				_sliceViewerTourId = -1L;
			}
		}
	}

	private void updateUITab4Info() {

		// data points
		final int[] timeSerie = _tourData.timeSerie;
		if (timeSerie == null) {
			_txtTimeSlicesCount.setText(UI.EMPTY_STRING);
		} else {
			final int dataPoints = timeSerie.length;
			_txtTimeSlicesCount.setText(Integer.toString(dataPoints));
		}
		_txtTimeSlicesCount.pack(true);

		// device name
		_txtDeviceName.setText(_tourData.getDeviceName());
		_txtDeviceName.pack(true);

		// distance sensor
		_txtDistanceSensor.setText(_tourData.getIsDistanceFromSensor()
				? Messages.Tour_Editor_Label_DistanceSensor_Yes
				: Messages.Tour_Editor_Label_DistanceSensor_No);
		_txtDistanceSensor.pack(true);

		// import file path
		_txtImportFilePath.setText(_tourData.isTourImportFilePathAvailable()
				? _tourData.getTourImportFilePath()
				: UI.EMPTY_STRING);

		/*
		 * reference tours
		 */
		final Collection<TourReference> refTours = _tourData.getTourReferences();
		if (refTours.size() > 0) {
			updateRefTourInfo(refTours);
		} else {
			_txtRefTour.setText(Messages.tour_editor_label_ref_tour_none);
			_refTourRange = null;
		}

		/*
		 * person
		 */
		final TourPerson tourPerson = _tourData.getTourPerson();
		if (tourPerson == null) {
			_txtPerson.setText(UI.EMPTY_STRING);
		} else {
			_txtPerson.setText(tourPerson.getName());
		}

		/*
		 * tour ID
		 */
		final Long tourId = _tourData.getTourId();
		if (tourId == null) {
			_txtTourId.setText(UI.EMPTY_STRING);
		} else {
			_txtTourId.setText(Long.toString(tourId));
		}

		/*
		 * date/time created
		 */
		final org.joda.time.DateTime dtCreated = _tourData.getDateTimeCreated();
		_txtDateTimeCreated.setText(dtCreated == null ? //
				UI.EMPTY_STRING
				: _dtFormatter.print(dtCreated.getMillis()));

		/*
		 * date/time modified
		 */
		final org.joda.time.DateTime dtModified = _tourData.getDateTimeModified();
		_txtDateTimeModified.setText(dtModified == null ? //
				UI.EMPTY_STRING
				: _dtFormatter.print(dtModified.getMillis()));

		/*
		 * merge from tour ID
		 */
		final Long mergeFromTourId = _tourData.getMergeSourceTourId();
		if (mergeFromTourId == null) {
			_txtMergeFromTourId.setText(UI.EMPTY_STRING);
		} else {
			_txtMergeFromTourId.setText(Long.toString(mergeFromTourId));
		}

		/*
		 * merge into tour ID
		 */
		final Long mergeIntoTourId = _tourData.getMergeTargetTourId();
		if (mergeIntoTourId == null) {
			_txtMergeIntoTourId.setText(UI.EMPTY_STRING);
		} else {
			_txtMergeIntoTourId.setText(Long.toString(mergeIntoTourId));
		}

		/*
		 * layout container to resize the labels
		 */
		onResizeTabInfo();
	}

	private void updateUITitle() {

		updateUITitle(
				_dtTourDate.getYear(),
				_dtTourDate.getMonth(),
				_dtTourDate.getDay(),
				_dtStartTime.getHours(),
				_dtStartTime.getMinutes(),
				_dtStartTime.getSeconds());
	}

	/**
	 * update title of the view with the modified date/time
	 * 
	 * @param tourYear
	 * @param tourMonth
	 * @param tourDay
	 * @param hour
	 * @param minute
	 * @param seconds
	 */
	private void updateUITitle(	final int tourYear,
								final int tourMonth,
								final int tourDay,
								final int hour,
								final int minute,
								final int seconds) {

		_calendar.set(tourYear, tourMonth, tourDay, hour, minute, seconds);

		updateUITitleAsynch(TourManager.getTourTitle(_calendar.getTime()));
	}

	/**
	 * update the title is a really performance hog because of the date/time controls when they are
	 * layouted
	 */
	private void updateUITitleAsynch(final String title) {

		_uiUpdateTitleCounter++;

		Display.getCurrent().asyncExec(new Runnable() {

			final int	runnableCounter	= _uiUpdateTitleCounter;

			public void run() {

				if (_pageEditorForm.isDisposed()) {
					return;
				}

				// check if this is the last runnable
				if (runnableCounter != _uiUpdateTitleCounter) {
					// a new runnable was created
					return;
				}

				_pageEditorForm.setText(title);
			}
		});
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

		// end of line
		sb.append(UI.SYSTEM_NEW_LINE);
		exportWriter.write(sb.toString());
	}

}
