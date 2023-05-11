/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static org.eclipse.ui.forms.events.IExpansionListener.expansionStateChangedAdapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.commands.AppCommands;
import net.tourbook.commands.ISaveAndRestorePart;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.swimming.StrokeStyle;
import net.tourbook.common.swimming.SwimStroke;
import net.tourbook.common.swimming.SwimStrokeManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TimeZoneData;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
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
import net.tourbook.map2.view.SelectionMapSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagContentLayout;
import net.tourbook.tag.TagManager;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.CadenceMultiplier;
import net.tourbook.tour.DialogEditTimeSlicesValues;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourSaveListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.SelectionTourPause;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ComboViewerCadence;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.MessageManager;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionExtractTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.action.ActionSplitTour;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourSegmenter.SelectedTourSegmenterSegments;

import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
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
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

// author: Wolfgang Schramm
// create: 24.08.2007

/**
 * This editor can edit data of a tour
 */
public class TourDataEditorView extends ViewPart implements ISaveablePart, ISaveAndRestorePart, ITourProvider2 {

   public static final String            ID                                               = "net.tourbook.views.TourDataEditorView";    //$NON-NLS-1$
   //
   private static final char             NL                                               = UI.NEW_LINE;
   //
   private static final int              COLUMN_SPACING                                   = 20;
   //
   private static final String           WIDGET_KEY                                       = "widgetKey";                                //$NON-NLS-1$
   private static final String           WIDGET_KEY_TOUR_DISTANCE                         = "tourDistance";                             //$NON-NLS-1$
   private static final String           WIDGET_KEY_ALTITUDE_UP                           = "altitudeUp";                               //$NON-NLS-1$
   private static final String           WIDGET_KEY_ALTITUDE_DOWN                         = "altitudeDown";                             //$NON-NLS-1$
   private static final String           WIDGET_KEY_PERSON                                = "tourPerson";                               //$NON-NLS-1$
   //
   private static final String           MESSAGE_KEY_ANOTHER_SELECTION                    = "anotherSelection";                         //$NON-NLS-1$
   //
   /**
    * shows the busy indicator to load the slice viewer when there are more items as this value
    */
   private static final int              BUSY_INDICATOR_ITEMS                             = 5000;
   //
   private static final String           STATE_SELECTED_TAB                               = "tourDataEditor.selectedTab";               //$NON-NLS-1$
   private static final String           STATE_ROW_EDIT_MODE                              = "tourDataEditor.rowEditMode";               //$NON-NLS-1$
   private static final String           STATE_IS_EDIT_MODE                               = "tourDataEditor.isEditMode";                //$NON-NLS-1$
   private static final String           STATE_CSV_EXPORT_PATH                            = "tourDataEditor.csvExportPath";             //$NON-NLS-1$
   //
   private static final String           STATE_SECTION_CHARACTERISTICS                    = "STATE_SECTION_CHARACTERISTICS";            //$NON-NLS-1$
   private static final String           STATE_SECTION_DATE_TIME                          = "STATE_SECTION_DATE_TIME";                  //$NON-NLS-1$
   private static final String           STATE_SECTION_PERSONAL                           = "STATE_SECTION_PERSONAL";                   //$NON-NLS-1$
   private static final String           STATE_SECTION_TITLE                              = "STATE_SECTION_TITLE";                      //$NON-NLS-1$
   private static final String           STATE_SECTION_WEATHER                            = "STATE_SECTION_WEATHER";                    //$NON-NLS-1$
   //
   static final String                   STATE_DESCRIPTION_NUMBER_OF_LINES                = "STATE_DESCRIPTION_NUMBER_OF_LINES";        //$NON-NLS-1$
   static final int                      STATE_DESCRIPTION_NUMBER_OF_LINES_DEFAULT        = 3;
   static final String                   STATE_IS_DELETE_KEEP_DISTANCE                    = "STATE_IS_DELETE_KEEP_DISTANCE";            //$NON-NLS-1$
   static final boolean                  STATE_IS_DELETE_KEEP_DISTANCE_DEFAULT            = false;
   static final String                   STATE_IS_DELETE_KEEP_TIME                        = "STATE_IS_DELETE_KEEP_TIME";                //$NON-NLS-1$
   static final boolean                  STATE_IS_DELETE_KEEP_TIME_DEFAULT                = false;
   static final String                   STATE_IS_ELEVATION_FROM_DEVICE                   = "STATE_IS_ELEVATION_FROM_DEVICE";           //$NON-NLS-1$
   static final boolean                  STATE_IS_ELEVATION_FROM_DEVICE_DEFAULT           = true;
   static final String                   STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN             = "STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN";     //$NON-NLS-1$
   static final boolean                  STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN_DEFAULT     = true;
   static final String                   STATE_LAT_LON_DIGITS                             = "STATE_LAT_LON_DIGITS";                     //$NON-NLS-1$
   static final int                      STATE_LAT_LON_DIGITS_DEFAULT                     = 5;
   public static final String            STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES         = "STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES"; //$NON-NLS-1$
   public static final int               STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES_DEFAULT = 6;
   //
   public static final String            STATE_TAG_CONTENT_LAYOUT                         = "STATE_TAG_CONTENT_LAYOUT";                 //$NON-NLS-1$
   public static final TagContentLayout  STATE_TAG_CONTENT_LAYOUT_DEFAULT                 = TagContentLayout.IMAGE_AND_DATA;
   public static final String            STATE_TAG_IMAGE_SIZE                             = "STATE_TAG_IMAGE_SIZE";                     //$NON-NLS-1$
   public static final int               STATE_TAG_IMAGE_SIZE_DEFAULT                     = 100;
   public static final int               STATE_TAG_IMAGE_SIZE_MIN                         = 10;
   public static final int               STATE_TAG_IMAGE_SIZE_MAX                         = 500;
   public static final String            STATE_TAG_TEXT_WIDTH                             = "STATE_TAG_TEXT_WIDTH";                     //$NON-NLS-1$
   public static final int               STATE_TAG_TEXT_WIDTH_DEFAULT                     = 200;
   public static final int               STATE_TAG_TEXT_WIDTH_MIN                         = 20;
   public static final int               STATE_TAG_TEXT_WIDTH_MAX                         = 1000;
   public static final String            STATE_TAG_NUM_CONTENT_COLUMNS                    = "STATE_TAG_NUM_CONTENT_COLUMNS";            //$NON-NLS-1$
   public static final int               STATE_TAG_NUM_CONTENT_COLUMNS_DEFAULT            = 2;
   public static final int               STATE_TAG_NUM_CONTENT_COLUMNS_MIN                = 1;
   public static final int               STATE_TAG_NUM_CONTENT_COLUMNS_MAX                = 100;
   //
   private static final String           COLUMN_ALTITUDE                                  = "ALTITUDE_ALTITUDE";                        //$NON-NLS-1$
   private static final String           COLUMN_CADENCE                                   = "POWERTRAIN_CADENCE";                       //$NON-NLS-1$
   private static final String           COLUMN_DATA_SEQUENCE                             = "DATA_SEQUENCE";                            //$NON-NLS-1$
   private static final String           COLUMN_POWER                                     = "POWER";                                    //$NON-NLS-1$
   private static final String           COLUMN_PACE                                      = "MOTION_PACE";                              //$NON-NLS-1$
   private static final String           COLUMN_PULSE                                     = "BODY_PULSE";                               //$NON-NLS-1$
   private static final String           COLUMN_TEMPERATURE                               = "WEATHER_TEMPERATURE";                      //$NON-NLS-1$
   //
   private static final IPreferenceStore _prefStore                                       = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common                                = CommonActivator.getPrefStore();
   private static final IDialogSettings  _state                                           = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _stateSwimSlice                                  = TourbookPlugin.getState(ID + ".swimSlice"); //$NON-NLS-1$
   private static final IDialogSettings  _stateTimeSlice                                  = TourbookPlugin.getState(ID + ".slice");     //$NON-NLS-1$
   //
   private static final boolean          IS_LINUX                                         = UI.IS_LINUX;
   private static final boolean          IS_OSX                                           = UI.IS_OSX;
   private static final boolean          IS_DARK_THEME                                    = UI.IS_DARK_THEME;
   /**
    * this width is used as a hint for the width of the description field, this value also
    * influences the width of the columns in this editor
    */
   private static final int              _hintTextColumnWidth                             = IS_OSX ? 200 : 150;
   //
   DecimalFormat                         _temperatureFormat                               = new DecimalFormat("###.0");                 //$NON-NLS-1$
   //
   private ZonedDateTime                 _tourStartTime;
   //
   /*
    * Data series which are displayed in the viewer, all are metric system
    */
   private int[]                   _serieTime;
   private float[]                 _serieDistance;
   private float[]                 _serieAltitude;
   private float[]                 _serieTemperature;
   private float[]                 _serieCadence;
   private float[]                 _serieGradient;
   private float[]                 _serieSpeed;
   private float[]                 _seriePace;
   private float[]                 _seriePower;
   private float[]                 _seriePulse;
   private float[]                 _seriePulse_RR_Bpm;
   private String[]                _seriePulse_RR_Intervals;
   private int[]                   _seriePulse_RR_Index;
   private double[]                _serieLatitude;
   private double[]                _serieLongitude;
   private float[][]               _serieGears;
   private boolean[]               _serieBreakTime;
   private boolean[]               _seriePausedTime;
   //
   private short[]                 _swimSerie_StrokeRate;
// private short[]                 _swimSerie_LengthType;
   private short[]                 _swimSerie_StrokesPerlength;
   private short[]                 _swimSerie_StrokeStyle;
   private int[]                   _swimSerie_Time;
   //
   private ColumnDefinition        _timeSlice_ColDef_Altitude;
   private ColumnDefinition        _timeSlice_ColDef_Cadence;
   private ColumnDefinition        _timeSlice_ColDef_Pulse;
   private ColumnDefinition        _timeSlice_ColDef_Temperature;
   private ColumnDefinition        _timeSlice_ColDef_Latitude;
   private ColumnDefinition        _timeSlice_ColDef_Longitude;
   //
   private ColumnDefinition        _swimSlice_ColDef_StrokeRate;
   private ColumnDefinition        _swimSlice_ColDef_Strokes;
   private ColumnDefinition        _swimSlice_ColDef_StrokeStyle;
   //
   private MessageManager          _messageManager;
   private PostSelectionProvider   _postSelectionProvider;
   private ISelectionListener      _postSelectionListener;
   private IPartListener2          _partListener;
   private IPropertyChangeListener _prefChangeListener;
   private IPropertyChangeListener _prefChangeListener_Common;
   private ITourEventListener      _tourEventListener;
   private ITourSaveListener       _tourSaveListener;
   //
   private final NumberFormat      _nf1        = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf1NoGroup = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf2        = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf3        = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf6        = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf3NoGroup = NumberFormat.getNumberInstance();
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
   private long                               _timeSlice_ViewerTourId = -1;
   private long                               _swimSlice_ViewerTourId = -1;
   //
   /**
    * <code>true</code>: rows can be selected in the viewer<br>
    * <code>false</code>: cell can be selected in the viewer
    */
   private boolean                            _isRowEditMode          = true;
   private boolean                            _isEditMode;
   private boolean                            _isTourDirty            = false;
   private boolean                            _isTourWithSwimData;
   //
   /**
    * is <code>true</code> when the tour is currently being saved to prevent a modify event or the
    * onSelectionChanged event
    */
   private boolean                            _isSavingInProgress     = false;

   /**
    * When <code>true</code> then data are loaded into fields
    */
   private boolean                            _isSetField             = false;

   /**
    * contains the tour id from the last selection event
    */
   private Long                               _selectionTourId;
   private ModifyListener                     _modifyListener;
   private ModifyListener                     _modifyListener_Temperature;
   private MouseWheelListener                 _mouseWheelListener;
   private MouseWheelListener                 _mouseWheelListener_Temperature;
   private SelectionListener                  _selectionListener;
   private SelectionListener                  _selectionListener_Temperature;
   private SelectionListener                  _columnSortListener;
   private SelectionListener                  _tourTimeListener;
   private ModifyListener                     _verifyFloatValue;
   private ModifyListener                     _verifyIntValue;
   //
   private PixelConverter                     _pc;
   private int                                _hintValueFieldWidth;
   private int                                _hintDefaultSpinnerWidth;

   /**
    * is <code>true</code> when {@link #_tourChart} contains reference tours
    */
   private boolean                            _isReferenceTourAvailable;

   /**
    * range for the reference tours, is <code>null</code> when reference tours are not available<br>
    * 1st index = ref tour<br>
    * 2nd index: 0:start, 1:end
    */
   private int[][]                            _refTourRange;

   private boolean                            _isPartVisible;

   /**
    * when <code>true</code> additional info is displayed in the title area
    */
   private boolean                            _isInfoInTitle;

   /**
    * Is <code>true</code> when a cell editor is active, otherwise <code>false</code>
    */
   private boolean                            _isCellEditorActive;

   /**
    * Current combobox cell editor or <code>null</code> when a cell editor is not active.
    */
   private CellEditor_ComboBox_Customized     _currentComboBox_CellEditor;

   /**
    * Current text cell editor or <code>null</code> when a cell editor is not active.
    */
   private CellEditor_Text_Customized         _currentTextEditor_CellEditor;

   /**
    * every requested UI update increased this counter
    */
   private int                                _uiUpdateCounter;

   /**
    * counter when the UI update runnable is run, this will optimize performance to not update the
    * UI when the part is hidden
    */
   private int                                _uiRunnableCounter      = 0;
   private int                                _uiUpdateTitleCounter   = 0;
   private TourData                           _uiRunnableTourData;
   private boolean                            _uiRunnableForce_TimeSliceReload;
   private boolean                            _uiRunnableForce_SwimSliceReload;
   private boolean                            _uiRunnableIsDirtyDisabled;
   //
   private SliceEditingSupport_Float          _timeSlice_AltitudeEditingSupport;
   private SliceEditingSupport_Float          _timeSlice_PulseEditingSupport;
   private SliceEditingSupport_Float          _timeSlice_TemperatureEditingSupport;
   private SliceEditingSupport_Float          _timeSlice_CadenceEditingSupport;
   private SliceEditingSupport_Double         _timeSlice_LatitudeEditingSupport;
   private SliceEditingSupport_Double         _timeSlice_LongitudeEditingSupport;
   //
   private SliceEditingSupport_Short          _swimSlice_StrokeRateEditingSupport;
   private SliceEditingSupport_Short          _swimSlice_StrokesEditingSupport;
   private SliceEditor_ComboBox_StrokeStyle   _swimSlice_StrokeStyleEditingSupport;
   //
   private int                                _enableActionCounter    = 0;

   /**
    * contains all markers with the data serie index as key
    */
   private final HashMap<Integer, TourMarker> _markerMap              = new HashMap<>();

   /**
    * When <code>true</code> the tour is created with the tour editor
    */
   private boolean                            _isManualTour;
   private boolean                            _isTitleModified;
   private boolean                            _isAltitudeManuallyModified;
   private boolean                            _isDistManuallyModified;
   private boolean                            _isLocationStartModified;
   private boolean                            _isLocationEndModified;
   private boolean                            _isTimeZoneManuallyModified;
   private boolean                            _isTemperatureManuallyModified;
   private boolean                            _isWindSpeedManuallyModified;

   /*
    * Measurement unit values
    */
   private float                                      _unitValueDistance;
   private float                                      _unitValueElevation;
   private int[]                                      _unitValueWindSpeed;
   //
   private MenuManager                                _swimViewer_MenuManager;
   private MenuManager                                _timeViewer_MenuManager;
   private IContextMenuProvider                       _swimViewer_ContextMenuProvider = new SwimSlice_ViewerContextMenuProvider();
   private IContextMenuProvider                       _timeViewer_ContextMenuProvider = new TimeSlice_ViewerContextMenuProvider();
   //
   private Action_RemoveSwimStyle                     _action_RemoveSwimStyle;
   private Action_SetSwimStyle_Header                 _action_SetSwimStyle_Header;
   private ActionComputeDistanceValues                _actionComputeDistanceValues;
   private ActionCreateTourMarker                     _actionCreateTourMarker;
   private ActionCSVTimeSliceExport                   _actionCsvTimeSliceExport;
   private ActionDeleteDistanceValues                 _actionDeleteDistanceValues;
   private ActionDeleteTimeSlices_AdjustTourStartTime _actionDeleteTimeSlices_AdjustTourStartTime;
   private ActionDeleteTimeSlices_KeepTime            _actionDeleteTimeSlices_KeepTime;
   private ActionDeleteTimeSlices_KeepTimeAndDistance _actionDeleteTimeSlices_KeepTimeAndDistance;
   private ActionDeleteTimeSlices_RemoveTime          _actionDeleteTimeSlices_RemoveTime;
   private ActionEditTimeSlicesValues                 _actionEditTimeSlicesValues;
   private ActionExport                               _actionExportTour;
   private ActionExtractTour                          _actionExtractTour;
   private ActionOpenAdjustAltitudeDialog             _actionOpenAdjustAltitudeDialog;
   private ActionOpenMarkerDialog                     _actionOpenMarkerDialog;
   private ActionSetStartDistanceTo0                  _actionSetStartDistanceTo_0;
   private ActionSplitTour                            _actionSplitTour;
   private ActionToggleReadEditMode                   _actionToggleReadEditMode;
   private ActionToggleRowSelectMode                  _actionToggleRowSelectMode;
   private ActionViewSettings                         _actionViewSettings;
   //
   private ArrayList<Action_SetSwimStyle>             _allSwimStyleActions;
   //
   private TagMenuManager                             _tagMenuMgr;

   /**
    * Number of digits for the lat/lon columns.
    */
   private int                                        _latLonDigits;

   /**
    * Number of lines for the tour's description text.
    */
   private int                                        _descriptionNumLines;

   /**
    * Number of lines for the weather's description text.
    */
   private int                                        weatherDescriptionNumLines;

   private final NumberFormat                         _nfLatLon                       = NumberFormat.getNumberInstance();

   private TourData                                   _tourData;
   private LocalDate                                  _lastDuplicateTourDateCheck;

   private Color                                      _foregroundColor_Default;
   private Color                                      _backgroundColor_Default;
   private Color                                      _foregroundColor_1stColumn_RefTour;
   private Color                                      _backgroundColor_1stColumn_RefTour;
   private Color                                      _foregroundColor_1stColumn_NoRefTour;
   private Color                                      _backgroundColor_1stColumn_NoRefTour;

   //
   // ################################################## UI controls ##################################################
   //

   private Composite                _parent;
   private PageBook                 _pageBook;
   private Composite                _page_NoTourData;
   private Form                     _page_EditorForm;

   private PageBook                 _pageBook_Swim;
   private Composite                _pageSwim_NoData;
   private Composite                _pageSwim_Data;
   //
   private CTabFolder               _tabFolder;
   private CTabItem                 _tab_10_Tour;
   private CTabItem                 _tab_20_TimeSlices;
   private CTabItem                 _tab_30_SwimSlices;
   //
   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control> _firstColumnControls          = new ArrayList<>();
   private final ArrayList<Control> _firstColumnContainerControls = new ArrayList<>();
   private final ArrayList<Control> _secondColumnControls         = new ArrayList<>();
   //
   private TourChart                _tourChart;

   private Composite                _tourContainer;
   //
   private ScrolledComposite        _tab1Container;
   private Composite                _tab2_TimeSlice_Container;
   //
   private Composite                _swimSliceViewerContainer;
   private Composite                _timeSliceViewerContainer;
   //
   private Section                  _sectionTitle;
   private Section                  _sectionDateTime;
   private Section                  _sectionPersonal;
   private Section                  _sectionWeather;
   private Section                  _sectionCharacteristics;
   //
   private Label                    _timeSlice_Label;
   private TableViewer              _timeSlice_Viewer;
   private AtomicInteger            _timeSlice_Viewer_RunningId   = new AtomicInteger();
   private TimeSliceComparator      _timeSlice_Comparator         = new TimeSliceComparator();
   private Object[]                 _timeSlice_ViewerItems;
   private ColumnManager            _timeSlice_ColumnManager;
   private TimeSlice_TourViewer     _timeSlice_TourViewer         = new TimeSlice_TourViewer();
   //
   private TableViewer              _swimSlice_Viewer;
   private Object[]                 _swimSlice_ViewerItems;
   private ColumnManager            _swimSlice_ColumnManager;
   private SwimSlice_TourViewer     _swimSlice_TourViewer         = new SwimSlice_TourViewer();

   private FormToolkit              _tk;

   /*
    * Tab: Tour
    */
   private Composite          _containerTags_Content;
   private ScrolledComposite  _containerTags_Scrolled;
   private PageBook           _pageBook_Tags;
   //
   private Combo              _comboTitle;
   //
   private ComboViewerCadence _comboCadence;
   //
   private CLabel             _lblTourType;
   //
   private ControlDecoration  _decoTimeZone;
   //
   private Combo              _comboLocation_Start;
   private Combo              _comboLocation_End;
   private Combo              _comboTimeZone;
   private Combo              _comboWeather_Clouds;
   private Combo              _comboWeather_Wind_DirectionText;
   private Combo              _comboWeather_WindSpeedText;
   //
   private DateTime           _dtStartTime;
   private DateTime           _dtTourDate;
   //
   private Label              _lblAltitudeUpUnit;
   private Label              _lblAltitudeDownUnit;
   private Label              _lblCloudIcon;
   private Label              _lblDistanceUnit;
   private Label              _lblNoTags;
   private Label              _lblPerson_BodyWeightUnit;
   private Label              _lblPerson_BodyFatUnit;
   private Label              _lblSpeedUnit;
   private Label              _lblStartTime;
   private Label              _lblTags;
   private Label              _lblTimeZone;
   private Label              _lblWeather_PrecipitationUnit;
   private Label              _lblWeather_PressureUnit;
   private Label              _lblWeather_SnowfallUnit;
   private Label              _lblWeather_TemperatureUnit_Avg;
   private Label              _lblWeather_TemperatureUnit_Avg_Device;
   private Label              _lblWeather_TemperatureUnit_Max;
   private Label              _lblWeather_TemperatureUnit_Max_Device;
   private Label              _lblWeather_TemperatureUnit_Min;
   private Label              _lblWeather_TemperatureUnit_Min_Device;
   private Label              _lblWeather_TemperatureUnit_WindChill;
   //
   private Link               _linkDefaultTimeZone;
   private Link               _linkGeoTimeZone;
   private Link               _linkRemoveTimeZone;
   private Link               _linkTag;
   private Link               _linkTourType;
   private Link               _linkWeather;
   //
   private Spinner            _spinPerson_BodyFat;
   private Spinner            _spinPerson_BodyWeight;
   private Spinner            _spinPerson_Calories;
   private Spinner            _spinPerson_FTP;
   private Spinner            _spinPerson_RestPulse;
   private Spinner            _spinWeather_Humidity;
   private Spinner            _spinWeather_PrecipitationValue;
   private Spinner            _spinWeather_PressureValue;
   private Spinner            _spinWeather_SnowfallValue;
   private Spinner            _spinWeather_Temperature_Average;
   private Spinner            _spinWeather_Temperature_Min;
   private Spinner            _spinWeather_Temperature_Max;
   private Spinner            _spinWeather_Temperature_WindChill;
   private Spinner            _spinWeather_Wind_DirectionValue;
   private Spinner            _spinWeather_Wind_SpeedValue;
   //
   private TableCombo         _tableComboWeather_AirQuality;
   //
   private Text               _txtAltitudeDown;
   private Text               _txtAltitudeUp;
   private Text               _txtDescription;
   private Text               _txtDistance;
   private Text               _txtWeather;
   private Text               _txtWeather_Temperature_Average_Device;
   private Text               _txtWeather_Temperature_Min_Device;
   private Text               _txtWeather_Temperature_Max_Device;
   //
   private TimeDuration       _deviceTime_Elapsed;                   // Total time of the activity
   private TimeDuration       _deviceTime_Recorded;                  // Time recorded by the device = Total time - paused times
   private TimeDuration       _deviceTime_Paused;                    // Time where the user deliberately paused the device
   private TimeDuration       _computedTime_Moving;                  // Computed time moving
   private TimeDuration       _computedTime_Break;                   // Computed time stopped

   private Menu               _swimViewer_ContextMenu;
   private Menu               _timeViewer_ContextMenu;

   private class Action_RemoveSwimStyle extends Action {

      public Action_RemoveSwimStyle() {

         super(Messages.TourEditor_Action_RemoveSwimStyle);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Remove));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Remove_Disabled));
      }

      @Override
      public void run() {
         setSwimStyle(null);
      }
   }

   /**
    * Swim style menu header item without action
    */
   private class Action_SetSwimStyle_Header extends Action {

      public Action_SetSwimStyle_Header() {

         super(Messages.TourEditor_Action_SetSwimStyle, AS_PUSH_BUTTON);
         setEnabled(false);
      }
   }

   private class ActionViewSettings extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTourEditor_Options(_pageBook, toolbar, TourDataEditorView.this);
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
         _currentComboBox_CellEditor = this;

         enableActionsDelayed();
      }

      @Override
      public void deactivate() {

         super.deactivate();

         _isCellEditorActive = false;
         _currentComboBox_CellEditor = null;

         enableActionsDelayed();
      }

      @Override
      protected void focusLost() {
         super.focusLost();
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
         _currentTextEditor_CellEditor = this;

         enableActionsDelayed();
      }

      @Override
      public void deactivate() {

         super.deactivate();

         _isCellEditorActive = false;
         _currentTextEditor_CellEditor = null;

         enableActionsDelayed();
      }

      @Override
      protected void focusLost() {
         super.focusLost();
      }
   }

   private final class SliceEditingSupport_Double extends EditingSupport {

      private final TextCellEditor __cellEditor;
      private double[]             __dataSerie;

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
         return Double.toString(__dataSerie[((TimeSlice) element).serieIndex]);
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
                   * World position has changed, this is an absolute overkill, when only one
                   * position has changed
                   */
                  _tourData.clearWorldPositions();

                  updateUI_AfterSliceEdit();
               }

            } catch (final Exception e) {
               // ignore invalid characters
            }
         }
      }
   }

   private final class SliceEditingSupport_Float extends EditingSupport {

      private final TextCellEditor __cellEditor;
      private float[]              __dataSerie;

      private boolean              __canEditSlice = true;

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

            if (_unitValueElevation != 1) {

               // none metric measurement system

               displayedValue /= _unitValueElevation;
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

                  if (_unitValueElevation != 1) {

                     // none metric measurement system

                     // ensure float is used
                     final float noneMetricValue = enteredValue;
                     metricValue = Math.round(noneMetricValue * _unitValueElevation);
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
            }
         }
      }
   }

   private final class SliceEditingSupport_Short extends EditingSupport {

      private final TextCellEditor __cellEditor;
      private short[]              __dataSerie;

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

         final short displayedValue = __dataSerie[((TimeSlice) element).serieIndex];

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

               final int serieIndex = ((TimeSlice) element).serieIndex;
               if (enteredValue != __dataSerie[serieIndex]) {

                  // value has changed

                  // update dataserie
                  __dataSerie[serieIndex] = enteredValue;

                  updateUI_AfterSliceEdit();

                  // when swim cadence is modified the time slice viewer cadence is wrong
                  // -> reload this viewer when tab is selected
                  invalidateSliceViewers();
               }

            } catch (final Exception e) {
               // ignore invalid characters
            }
         }
      }
   }

   private final class SliceEditor_ComboBox_StrokeStyle extends EditingSupport {

      private final ComboBoxCellEditor __cellEditor;
      private short[]                  __dataSerie;

      private boolean                  __canEditSlice = true;

      private SliceEditor_ComboBox_StrokeStyle(final ComboBoxCellEditor comboBoxCellEditor, final short[] dataSerie) {

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

         final short strokeValue = __dataSerie[((TimeSlice) element).serieIndex];

         final int labelIndex = SwimStrokeManager.getDefaultIndex(strokeValue);

         // combobox needs an integer value
         return labelIndex;
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

                  short strokeValue;

                  if (enteredValue == -1) {

                     // nothing is selected

                     strokeValue = Short.MIN_VALUE;

                  } else {

                     strokeValue = SwimStrokeManager.getStrokeValue(enteredValue);
                  }

                  // update dataserie
                  __dataSerie[serieIndex] = strokeValue;

                  updateUI_AfterSliceEdit();
               }

            } catch (final Exception e) {
               StatusUtil.log(e);
            }
         }
      }
   }

   private class SliceViewerItems {

      Object[] __timeSlice_ViewerItems;
      Object[] __swimSlice_ViewerItems;

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

                     BusyIndicator.showWhile(Display.getCurrent(), () -> {
                        _swimSlice_ViewerItems = getSliceViewerItems().__swimSlice_ViewerItems;
                        _swimSlice_Viewer.setInput(_swimSlice_ViewerItems);
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

   private class SwimSlice_ViewerContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_swimViewer_ContextMenu != null) {
            _swimViewer_ContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _swimViewer_ContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _swimViewer_ContextMenu = createUI_Tab_36_SwimSliceViewerContextMenu_Menu();

         return _swimViewer_ContextMenu;
      }
   }

   private class TimeDuration {

      private static final String timeFormat      = "%5d:%02d:%02d"; //$NON-NLS-1$

      private PageBook            _pageBook;
      private Composite           _pageReadMode;
      private Composite           _pageEditMode;

      private Text                _txtTime;
      private Spinner             _spinHours;
      private Spinner             _spinMinutes;
      private Spinner             _spinSeconds;

      private boolean             _isTimeEditMode = false;

      public TimeDuration(final Composite parent) {

         createUI(parent);
      }

      private void createUI(final Composite parent) {

         // fixed bug: https://sourceforge.net/tracker/index.php?func=detail&aid=3292465&group_id=179799&atid=890601
         // let the system decide which field width is used by setting min/max values
//       final int spinnerWidthHour = _pc.convertWidthInCharsToPixels(_isOSX ? 8 : 4);
//       final int spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 6 : 3);

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
            GridDataFactory.fillDefaults()
//                .hint(spinnerWidthHour, SWT.DEFAULT)
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
//                .hint(spinnerWidth, SWT.DEFAULT)
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
            GridDataFactory.fillDefaults()
//                .hint(spinnerWidth, SWT.DEFAULT)
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
         return (_spinHours.getSelection() * 3600)
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

         // hide drawing artifact, this do not work 100% correct on winXP
         _tab1Container.setRedraw(false);
         {
            _tab1Container.layout(true, true);
         }
         _tab1Container.setRedraw(true);
      }

      public void setTime(final int elapsedTime) {

         final int hours = elapsedTime / 3600;
         final int minutes = (elapsedTime % 3600) / 60;
         final int seconds = (elapsedTime % 3600) % 60;

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

                     BusyIndicator.showWhile(Display.getCurrent(), () -> {
                        _timeSlice_ViewerItems = getSliceViewerItems().__timeSlice_ViewerItems;
                        _timeSlice_Viewer.setInput(_timeSlice_ViewerItems);
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

   private class TimeSlice_ViewerContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_timeViewer_ContextMenu != null) {
            _timeViewer_ContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _timeViewer_ContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _timeViewer_ContextMenu = createUI_Tab_26_TimeSliceViewerContextMenu_Menu();

         return _timeViewer_ContextMenu;
      }
   }

   private class TimeSliceComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_DATA_SEQUENCE;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final boolean isDescending = __sortDirection == DESCENDING;

         final TimeSlice ts1 = (TimeSlice) e1;
         final TimeSlice ts2 = (TimeSlice) e2;

         long rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_DATA_SEQUENCE:

            rc = ts1.serieIndex - ts2.serieIndex;
            break;

         case COLUMN_ALTITUDE:
            if (_serieAltitude == null) {
               break;
            }

            final float altitude1 = _serieAltitude[ts1.serieIndex];
            final float altitude2 = _serieAltitude[ts2.serieIndex];
            rc = Float.compare(altitude1, altitude2);
            break;

         case COLUMN_PULSE:
            if (_seriePulse == null) {
               break;
            }

            final float pulse1 = _seriePulse[ts1.serieIndex];
            final float pulse2 = _seriePulse[ts2.serieIndex];
            rc = Float.compare(pulse1, pulse2);
            break;

         case COLUMN_CADENCE:
            if (_serieCadence == null) {
               break;
            }

            final float cadence1 = _serieCadence[ts1.serieIndex];
            final float cadence2 = _serieCadence[ts2.serieIndex];
            rc = Float.compare(cadence1, cadence2);
            break;

         case COLUMN_TEMPERATURE:
            if (_serieTemperature == null) {
               break;
            }

            final float temperature1 = _serieTemperature[ts1.serieIndex];
            final float temperature2 = _serieTemperature[ts2.serieIndex];
            rc = Float.compare(temperature1, temperature2);
            break;

         case COLUMN_POWER:
            if (_seriePower == null) {
               break;
            }

            final float power1 = _seriePower[ts1.serieIndex];
            final float power2 = _seriePower[ts2.serieIndex];
            rc = Float.compare(power1, power2);
            break;

         case COLUMN_PACE:
            if (_seriePace == null) {
               break;
            }

            final float pace1 = _seriePace[ts1.serieIndex];
            final float pace2 = _seriePace[ts2.serieIndex];
            rc = Float.compare(pace1, pace2);
            break;
         }

         // if descending order, flip the direction
         if (isDescending) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly.
          */
         return rc > 0 //
               ? 1
               : rc < 0 //
                     ? -1
                     : 0;
      }

      /**
       * @param sortColumnId
       * @return Returns the column widget by it's column id, when column id is not found then the
       *         first column is returned.
       */
      private TableColumn getSortColumn(final String sortColumnId) {

         final TableColumn[] allColumns = _timeSlice_Viewer.getTable().getColumns();

         for (final TableColumn column : allColumns) {

            final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

            if (columnId != null && columnId.equals(sortColumnId)) {
               return column;
            }
         }

         return allColumns[0];
      }

      @Override
      public final boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }

      public void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId == null) {
            return;
         }

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort -> select next sorting

            switch (__sortDirection) {
            case ASCENDING:
               __sortDirection = DESCENDING;
               break;

            case DESCENDING:
            default:
               __sortDirection = ASCENDING;
               break;
            }

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }

      /**
       * Set the sort column direction indicator for a column
       *
       * @param sortColumnId
       * @param isAscendingSort
       */
      private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

         final int direction =
               sortDirection == TimeSliceComparator.ASCENDING ? SWT.UP
                     : sortDirection == TimeSliceComparator.DESCENDING ? SWT.DOWN
                           : SWT.NONE;

         final Table table = _timeSlice_Viewer.getTable();
         final TableColumn tc = getSortColumn(sortColumnId);

         table.setSortColumn(tc);
         table.setSortDirection(direction);
      }
   }

   public static void onSelect_WindDirection_Text(final Spinner spinWeather_Wind_DirectionValue,
                                                  final Combo comboWeather_WindDirectionText) {

      // N=348.75=11.25   NNE=11.25=33.75    NE=33.75=56.25    ENE=56.25=78.75
      // E=78.75=101.25   ESE=101.25=123.75  SE=123.75=146.25  SSE=146.25=168.75
      // S=168.75=191.25  SSW=191.25=213.75  SW=213.75=236.25  WSW=236.25=258.75
      // W=258.75=281.25  WNW=281.25=303.75  NW=303.75=326.25  NNW=326.25=348.75

      int selectedIndex = comboWeather_WindDirectionText.getSelectionIndex();

      int windDirectionValue = 0;
      boolean isWindDirectionValueEnabled = true;

      //0 represents an empty value
      if (selectedIndex == 0) {

         isWindDirectionValueEnabled = false;
         windDirectionValue = 0;
      } else {

         //We decrement the index value to take into account the first element
         //that represents the empty value.
         --selectedIndex;

         // get degree from selected direction
         windDirectionValue = (int) (selectedIndex * 22.5f * 10f);
      }

      spinWeather_Wind_DirectionValue.setEnabled(isWindDirectionValueEnabled);
      spinWeather_Wind_DirectionValue.setSelection(windDirectionValue);
   }

   public static void onSelect_WindDirection_Value(final Spinner spinWeather_Wind_DirectionValue,
                                                   final Combo comboWeather_WindDirectionText) {

      int degree = spinWeather_Wind_DirectionValue.getSelection();

      // this tricky code is used to scroll before 0 which will overscroll and starts from the beginning
      if (degree == -1) {
         degree = 3599;
         spinWeather_Wind_DirectionValue.setSelection(degree);
      }

      if (degree == 3600) {
         degree = 0;
         spinWeather_Wind_DirectionValue.setSelection(degree);
      }

      int windDirectionTextIndex = 0;
      if (spinWeather_Wind_DirectionValue.isEnabled()) {
         windDirectionTextIndex = UI.getCardinalDirectionTextIndex((int) (degree / 10.0f));
      }

      comboWeather_WindDirectionText.select(windDirectionTextIndex);
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

      final Display currentDisplay = Display.getCurrent();
      final Shell activeShell = currentDisplay.getActiveShell();

      // check if a person is selected
      final TourPerson activePerson = TourbookPlugin.getActivePerson();
      if (activePerson == null) {

         MessageDialog.openInformation(activeShell,

               Messages.tour_editor_dlg_create_tour_title,
               Messages.tour_editor_dlg_create_tour_message);

         return;
      }

      TourData newTourData;

      if (copyFromOtherTour != null) {

         // clone other tour

         /*
          * Show warning that this feature is experimental, show once a day or when this view is
          * reopened
          */
         final LocalDate today = LocalDate.now();

         if (today.equals(_lastDuplicateTourDateCheck) == false) {

            _lastDuplicateTourDateCheck = today;

            // needs a timer otherwise it could not be displayed
            currentDisplay.timerExec(1000, () -> {

               MessageDialog.openWarning(activeShell,

                     "Experimental Feature", //$NON-NLS-1$

                     UI.EMPTY_STRING

                           + "Duplicating a tour is a new experimental feature in MyTourbook 23.3" + NL //$NON-NLS-1$
                           + NL
                           + "Use this feature with care, mainly the duplicated tours, as it is not yet fully tested." + NL //$NON-NLS-1$
                           + NL
                           + "One issue could be that when a duplicated tour is selected then it's data are not displayed " //$NON-NLS-1$
                           + "because the original tour was selected before. " //$NON-NLS-1$
                           + "This issue happened in the flat \"Tour Book\" view and is fixed. " + NL //$NON-NLS-1$
                           + NL
                           + "There are so many possibilities in MyTourbook that not all of them are tested now." //$NON-NLS-1$

               );
            });
         }

         newTourData = copyFromOtherTour.createDeepCopy();

         actionCreateTour_SetTourTitle(newTourData);

      } else {

         // create a new empty tour

         newTourData = new TourData();

         // set tour start date/time
         newTourData.setTourStartTime(TimeTools.now());

         // tour id must be created after the tour date/time is set
         newTourData.createTourId();

         newTourData.setDeviceId(TourData.DEVICE_ID_FOR_MANUAL_TOUR);
         newTourData.setTourPerson(activePerson);
      }

      // ensure that the time zone is saved in the tour
      _isTimeZoneManuallyModified = true;

      // update UI
      _tourData = newTourData;
      _tourChart = null;
      updateUI_FromModel(newTourData, false, true);

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
   }

   /**
    * Add a "copy" post fix to the tour title
    *
    * @param newTourData
    */
   private void actionCreateTour_SetTourTitle(final TourData newTourData) {

      final String tourTitle = newTourData.getTourTitle();

      final String newTourTitle;
      final String copyPostfix_Start = UI.SYMBOL_BRACKET_LEFT + Messages.Tour_Editor_TourTitle_CopyPostfix;

      final int lastIndexOfCopyPostfix = tourTitle.lastIndexOf(copyPostfix_Start);

      if (lastIndexOfCopyPostfix >= 0) {

         // title contains a "(copy" postfix -> add a number

         final String titleWithoutPostfix = tourTitle.substring(0, lastIndexOfCopyPostfix);

         final String copyPostfix_End = tourTitle.substring(lastIndexOfCopyPostfix);
         final boolean isNumberAvailable = copyPostfix_End.matches(".*\\d+.*"); //$NON-NLS-1$

         if (isNumberAvailable) {

            // this is copy number 2+, increment copy number

            // Regex source: https://stackoverflow.com/questions/12941362/is-it-possible-to-increment-numbers-using-regex-substitution#answer-12946132

            final String regex1 = "$"; //                                                                            //$NON-NLS-1$
            final String regex2 = "(?=\\d)(?:([0-8])(?=.*\\1(\\d)\\d*$)|(?=.*(1)))(?:(9+)(?=.*(~))|)(?!\\d)"; //     //$NON-NLS-1$
//          original + spaces      (?= \d)(?:([0-8])(?=.* \1( \d) \d*$)|(?=.*(1)))(?:(9+)(?=.*(~))|)(?! \d)
            final String regex3 = "9(?=9*~)(?=.*(0))|~| ~0123456789$"; //                                            //$NON-NLS-1$

            String newCopyText = copyPostfix_End;

            newCopyText = newCopyText.replaceAll(regex1, " ~0123456789"); //     //$NON-NLS-1$
            newCopyText = newCopyText.replaceAll(regex2, "$2$3$4$5"); //         //$NON-NLS-1$
            newCopyText = newCopyText.replaceAll(regex3, "$1"); //               //$NON-NLS-1$

            newTourTitle = titleWithoutPostfix + newCopyText;

         } else {

            // this is the 2nd copy

            newTourTitle = titleWithoutPostfix + copyPostfix_Start + " 2)"; //$NON-NLS-1$
         }

      } else {

         // this is the 1st copy

         newTourTitle = tourTitle + UI.SPACE + copyPostfix_Start + UI.SYMBOL_BRACKET_RIGHT;
      }

      newTourData.setTourTitle(newTourTitle);
   }

   void actionCsvTimeSliceExport() {

      // get selected time slices
      final StructuredSelection selection = (StructuredSelection) _timeSlice_Viewer.getSelection();
      if (selection.isEmpty()) {
         return;
      }

      /*
       * Get export filename
       */
      final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(Messages.dialog_export_file_dialog_text);

      dialog.setFilterPath(_state.get(STATE_CSV_EXPORT_PATH));
      dialog.setFilterExtensions(new String[] { Util.CSV_FILE_EXTENSION });
      dialog.setFileName(UI.EMPTY_STRING

            + net.tourbook.ui.UI.format_yyyymmdd_hhmmss(_tourData)
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
      try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            BufferedWriter exportWriter = new BufferedWriter(outputStreamWriter)) {

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
               sb.append(_nf3.format(_serieAltitude[serieIndex] / _unitValueElevation));
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
            sb.append(UI.SYSTEM_NEW_LINE);
            exportWriter.write(sb.toString());
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   void actionDelete_DistanceValues() {

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
    * @param isRemoveDistance
    * @param isAdjustTourStartTime
    */
   void actionDelete_TimeSlices(final boolean isRemoveTime, final boolean isRemoveDistance, final boolean isAdjustTourStartTime) {

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
      if (selection.isEmpty()) {
         return;
      }

      final Object[] selectedTimeSlices = selection.toArray();

      /*
       * Check if time slices have a successive selection
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

               // this is a successive selection which is currently supported

               lastIndex = timeSlice.serieIndex;

            } else {

               // this is not a successive selection -> show warning

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
       * Get first selection index to select a time slice after removal
       */
      final Table table = (Table) _timeSlice_Viewer.getControl();
      final int[] selectionIndices = table.getSelectionIndices();
      Arrays.sort(selectionIndices);
      final int lastTopIndex = selectionIndices[0];

      TourManager.removeTimeSlices(_tourData, firstIndex, lastIndex, isRemoveTime, isRemoveDistance, isAdjustTourStartTime);

      getDataSeriesFromTourData();

      // update UI
      _isSetField = true;
      {
         updateUI_Tab_1_Tour();
         updateUI_ReferenceTourRanges();

         // adjust tour editor start time in the UI
         if (isAdjustTourStartTime) {
            updateUI_Title();
         }
      }
      _isSetField = false;

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
      fireTourIsModified();

      /*
       * Keep position by selecting the next available time slice
       */
      final int numItems = table.getItemCount();
      if (numItems > 0) {

         // adjust to array bounds
         final int topIndex = Math.max(0, Math.min(lastTopIndex, numItems - 1));

         table.setSelection(topIndex);
         table.showSelection();

         // fire selection position
         _timeSlice_Viewer.setSelection(_timeSlice_Viewer.getSelection());
      }
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

      updateModel_FromUI();
      setTourDirty();

      updateUI_TimeZone();
   }

   private void actionTimeZone_SetDefault() {

      // select default time zone
      _comboTimeZone.select(TimeTools.getTimeZoneIndex_Default());
      _isTimeZoneManuallyModified = true;

      updateModel_FromUI();
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

      updateModel_FromUI();
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

   private void addPartListener() {

      // set the part listener
      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourDataEditorView.this) {

               _postSelectionProvider.setSelection(new SelectionTourData(null, _tourData));

               // update save icon
               final ICommandService cs = PlatformUI.getWorkbench().getService(ICommandService.class);
               cs.refreshElements(AppCommands.COMMAND_NET_TOURBOOK_TOUR_SAVE_TOUR, null);
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourDataEditorView.this) {

               TourManager.setTourDataEditor(null);
               onPart_Closed();
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

               Display.getCurrent().asyncExec(TourDataEditorView.this::updateUI_FromModelRunnable);
            }
         }
      };

      // register the listener in the page
      getSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         if (_tourData == null) {
            return;
         }

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            /*
             * Tour data could have been modified but the changes are not reflected in the data
             * model, the model needs to be updated from the UI
             */
            if (isTourValid()) {

// this has been disabled because the measurement has changed and it would update from the wrong measurement system
// it was also prevented that the measurement can be changed when the tour is modified
//
//                  updateModel_FromUI();

            } else {

               MessageDialog.openInformation(
                     Display.getCurrent().getActiveShell(),
                     Messages.tour_editor_dlg_discard_tour_title,
                     Messages.tour_editor_dlg_discard_tour_message);

               discardModifications();
            }

            if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

               // reload tour data

               updateUI_FromModel(_tourData, false, true);
            }

         } else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

            // display renamed person

            // updateUITab4Info(); do NOT work
            //
            // tour data must be reloaded

         } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _swimSlice_Viewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _swimSlice_Viewer.refresh();

            _timeSlice_Viewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _timeSlice_Viewer.refresh();

         } else if (property.equals(ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID)) {

            enableControls();
         }
      };

      /*
       * Common preferences
       */
      _prefChangeListener_Common = propertyChangeEvent -> {

         if (_tourData == null) {
            return;
         }

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

            // reload tour data

            updateUI_FromModel(_tourData, false, true);

         } else if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            /*
             * Tour data could have been modified but the changes are not reflected in the data
             * model, the model needs to be updated from the UI
             */
            if (isTourValid()) {

// this has been disabled because the measurement has changed and it would update from the wrong measurement system
// it was also prevented that the measurement can be changed when the tour is modified
//
//                  updateModel_FromUI();

            } else {

               MessageDialog.openInformation(
                     Display.getCurrent().getActiveShell(),
                     Messages.tour_editor_dlg_discard_tour_title,
                     Messages.tour_editor_dlg_discard_tour_message);

               discardModifications();
            }

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               /*
                * It is possible that the unit values in the UI class have been updated before
                * the model was saved, this can happen when another view called the method
                * UI.updateUnits(). Because of this race condition, only the internal units are
                * used to calculate values which depend on the measurement system
                */
               updateInternalUnitValues();

               recreateViewer();

               updateUI_FromModel(_tourData, false, true);
            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = (workbenchPart, selection) -> {

         if (workbenchPart == TourDataEditorView.this) {
            return;
         }

         onSelectionChanged(selection);
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == TourDataEditorView.this) {
            return;
         }

         if ((tourEventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

            onSelectionChanged((ISelection) eventData);

         } else {

            if (_tourData == null) {
               return;
            }

            final long tourDataEditorTourId = _tourData.getTourId();

            if ((tourEventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

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

            } else if (tourEventId == TourEventId.TAG_STRUCTURE_CHANGED) {

               if (_isTourDirty) {

                  updateUI_FromModel(_tourData, false, true);

               } else {

                  /**
                   * When tags are deleted, then the tour editor is not be dirty (this is
                   * previously checked)
                   * <p>
                   * -> reload tour with removed tags
                   */

                  reloadTourData();
               }

            } else if (tourEventId == TourEventId.TAG_CONTENT_CHANGED) {

               // redisplay tour tags

               updateUI_TagContent();

            } else if (tourEventId == TourEventId.MARKER_SELECTION && eventData instanceof SelectionTourMarker) {

               // ensure that the tour is displayed
               onSelectionChanged((ISelection) eventData);

               final SelectionTourMarker tourMarkerSelection = (SelectionTourMarker) eventData;

               onSelectionChanged_TourMarker(tourMarkerSelection);

            } else if (tourEventId == TourEventId.PAUSE_SELECTION && eventData instanceof SelectionTourPause) {

               // ensure that the tour is displayed
               onSelectionChanged((ISelection) eventData);
               onSelectionChanged_TourPause((SelectionTourPause) eventData);

            } else if (tourEventId == TourEventId.MAP_SELECTION && eventData instanceof SelectionMapSelection) {

               onSelectionChanged_MapSelection((SelectionMapSelection) eventData);

            } else if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearEditorContent();

            } else if (tourEventId == TourEventId.SEGMENT_LAYER_CHANGED) {

               updateUI_FromModel(_tourData, true, true);

            } else if (tourEventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

               updateUI_FromModel(_tourData, true, true);

            } else if (tourEventId == TourEventId.UPDATE_UI) {

               // check if this tour data editor contains a tour which must be updated

               // update editor
               if (net.tourbook.ui.UI.containsTourId(eventData, tourDataEditorTourId) != null) {

                  // reload tour data
                  _tourData = TourManager.getInstance().getTourData(_tourData.getTourId());

                  updateUI_FromModel(_tourData, false, true);
               }

            } else if (tourEventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);
            }
         }

      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void addTourSaveListener() {

      _tourSaveListener = () -> {

         boolean isTourSaved;

         _isSavingInProgress = true;
         {
            isTourSaved = saveTourWithValidation();
         }
         _isSavingInProgress = false;

         return isTourSaved;
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

      if (_tourData != null && _isTourDirty) {

         /*
          * In this case, nothing is done because the method, which fires the event
          * TourEventId.CLEAR_DISPLAYED_TOUR is responsible to use the correct TourData
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

// SET_FORMATTING_OFF

      _actionEditTimeSlicesValues      = new ActionEditTimeSlicesValues(this);

      _actionDeleteDistanceValues      = new ActionDeleteDistanceValues(this);
      _actionComputeDistanceValues     = new ActionComputeDistanceValues(this);
      _actionToggleRowSelectMode       = new ActionToggleRowSelectMode(this);
      _actionToggleReadEditMode        = new ActionToggleReadEditMode(this);
      _actionSetStartDistanceTo_0      = new ActionSetStartDistanceTo0(this);

      _actionOpenAdjustAltitudeDialog  = new ActionOpenAdjustAltitudeDialog(this, true);
      _actionOpenMarkerDialog          = new ActionOpenMarkerDialog(this, false);

      _actionCreateTourMarker          = new ActionCreateTourMarker(this);
      _actionExportTour                = new ActionExport(this);
      _actionCsvTimeSliceExport        = new ActionCSVTimeSliceExport(this);
      _actionSplitTour                 = new ActionSplitTour(this);
      _actionExtractTour               = new ActionExtractTour(this);

      _actionViewSettings              = new ActionViewSettings();

      _actionDeleteTimeSlices_AdjustTourStartTime  = new ActionDeleteTimeSlices_AdjustTourStartTime(this);
      _actionDeleteTimeSlices_KeepTime             = new ActionDeleteTimeSlices_KeepTime(this);
      _actionDeleteTimeSlices_KeepTimeAndDistance  = new ActionDeleteTimeSlices_KeepTimeAndDistance(this);
      _actionDeleteTimeSlices_RemoveTime           = new ActionDeleteTimeSlices_RemoveTime(this);

// SET_FORMATTING_ON

      _tagMenuMgr = new TagMenuManager(this, false);

      // swim style actions
      _action_SetSwimStyle_Header = new Action_SetSwimStyle_Header();
      _allSwimStyleActions = new ArrayList<>();
      for (final StrokeStyle strokeStyle : SwimStrokeManager.DEFAULT_STROKE_STYLES) {
         _allSwimStyleActions.add(new Action_SetSwimStyle(this, strokeStyle));
      }
      _action_RemoveSwimStyle = new Action_RemoveSwimStyle();

   }

   private void createFieldListener() {

      _modifyListener = modifyEvent -> {

         if (_isSetField || _isSavingInProgress) {
            return;
         }

         updateModel_FromUI();
         setTourDirty();
      };

      _modifyListener_Temperature = modifyEvent -> {

         if (UI.isLinuxAsyncEvent(modifyEvent.widget) || _isSetField || _isSavingInProgress) {
            return;
         }

         _isTemperatureManuallyModified = true;
         setTourDirty();
      };

      _mouseWheelListener = mouseEvent -> {

         if (_isSetField || _isSavingInProgress) {
            return;
         }

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);

         updateModel_FromUI();
         setTourDirty();

         updateUI_Time(mouseEvent.widget);
      };

      _mouseWheelListener_Temperature = mouseEvent -> {

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);

         if (_isSetField || _isSavingInProgress) {
            return;
         }

         _isTemperatureManuallyModified = true;
         setTourDirty();
      };

      _selectionListener = widgetSelectedAdapter(
            selectionEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }

               updateModel_FromUI();
               setTourDirty();
            });

      _selectionListener_Temperature = widgetSelectedAdapter(
            selectionEvent -> {

               if (UI.isLinuxAsyncEvent(selectionEvent.widget) || _isSetField || _isSavingInProgress) {
                  return;
               }

               _isTemperatureManuallyModified = true;
               setTourDirty();
            });

      /*
       * listener for elapsed/moving/paused time
       */
      _tourTimeListener = widgetSelectedAdapter(selectionEvent -> {

         if (_isSetField || _isSavingInProgress) {
            return;
         }

         updateModel_FromUI();
         setTourDirty();

         updateUI_Time(selectionEvent.widget);
      });

      _verifyFloatValue = modifyEvent -> {

         if (_isSetField || _isSavingInProgress) {
            return;
         }

         final Text widget = (Text) modifyEvent.widget;
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
          * tour dirty must be set after validation because an error can occur which enables
          * actions
          */
         if (_isTourDirty) {
            /*
             * when an error occurred previously and is now solved, the save action must be
             * enabled
             */
            enableActions();
         } else {
            setTourDirty();
         }
      };

      _verifyIntValue = modifyEvent -> {

         if (_isSetField || _isSavingInProgress) {
            return;
         }

         final Text widget = (Text) modifyEvent.widget;
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
          * tour dirty must be set after validation because an error can occur which enables
          * actions
          */
         if (_isTourDirty) {
            /*
             * when an error occurred previously and is now solved, the save action must be
             * enabled
             */
            enableActions();
         } else {
            setTourDirty();
         }
      };
   }

   private void createMenuManager() {

      /*
       * Swim slice viewer
       */
      _swimViewer_MenuManager = new MenuManager();

      _swimViewer_MenuManager.setRemoveAllWhenShown(true);
      _swimViewer_MenuManager.addMenuListener(this::fillContextMenu_SwimSlice);

      /*
       * Time slice viewer
       */
      _timeViewer_MenuManager = new MenuManager();

      _timeViewer_MenuManager.setRemoveAllWhenShown(true);
      _timeViewer_MenuManager.addMenuListener(this::fillContextMenu_TimeSlice);
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
      // set menu items
      menuMgr.addMenuListener(menuManager -> ActionSetTourTypeMenu.fillMenu(menuManager, TourDataEditorView.this, false));

      // set menu for the tour type link
      _linkTourType.setMenu(menuMgr.createContextMenu(_linkTourType));

      /*
       * tag menu
       */
      menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(menuManager -> {

         final Set<TourTag> tourTags = _tourData.getTourTags();
         final boolean isTagInTour = tourTags.size() > 0;

         _tagMenuMgr.fillTagMenu(menuManager, false);
         _tagMenuMgr.enableTagActions(true, isTagInTour, tourTags);
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
      createMenuManager();

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

      final int style = isExpandable
            ? Section.TWISTIE | Section.TITLE_BAR
            : Section.TITLE_BAR;

      final Section section = tk.createSection(parent, style);

      section.setText(title);
      GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

      final Composite sectionContainer = tk.createComposite(section);
      section.setClient(sectionContainer);

      section.addExpansionListener(expansionStateChangedAdapter(expansionEvent -> onExpandSection()));

      return section;
   }

   private void createUI(final Composite parent) {

      final Display display = parent.getDisplay();

      _pageBook = new PageBook(parent, SWT.NONE);

      _page_NoTourData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _tk = new FormToolkit(display);

      _page_EditorForm = _tk.createForm(_pageBook);
      MTFont.setHeaderFont(_page_EditorForm);
      _tk.decorateFormHeading(_page_EditorForm);

      _messageManager = new MessageManager(_page_EditorForm);

      final Composite formBody = _page_EditorForm.getBody();
      GridLayoutFactory.fillDefaults().applyTo(formBody);
      formBody.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

      _tabFolder = new CTabFolder(formBody, SWT.FLAT | SWT.BOTTOM);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabFolder);

      _tabFolder.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_Tab()));

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

      _foregroundColor_Default = formBody.getForeground(); // Color {0, 0, 0, 255}
      _backgroundColor_Default = formBody.getBackground(); // Color {41, 41, 41, 255}

      display.asyncExec(() -> {

         if (_pageBook.isDisposed()) {
            return;
         }

         _foregroundColor_Default = formBody.getForeground(); // Color {170, 170, 170, 255}    with dark mode
         _backgroundColor_Default = formBody.getBackground(); // Color {47, 47, 47, 255}       with dark mode

         updateUI_BackgroundColor();

         if (IS_DARK_THEME) {

            _foregroundColor_1stColumn_RefTour = display.getSystemColor(SWT.COLOR_YELLOW);
            _backgroundColor_1stColumn_RefTour = _backgroundColor_Default;

            _foregroundColor_1stColumn_NoRefTour = _foregroundColor_Default;
            _backgroundColor_1stColumn_NoRefTour = _backgroundColor_Default;

         } else {

            _foregroundColor_1stColumn_RefTour = _foregroundColor_Default;
            _backgroundColor_1stColumn_RefTour = display.getSystemColor(SWT.COLOR_YELLOW);

            _foregroundColor_1stColumn_NoRefTour = _foregroundColor_Default;
            _backgroundColor_1stColumn_NoRefTour = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
         }

      });
   }

   private Label createUI_LabelSeparator(final Composite parent) {

      return _tk.createLabel(parent, UI.EMPTY_STRING);
   }

   private void createUI_Section_110_Tour(final Composite parent) {

      _sectionTitle = createSection(parent, _tk, Messages.tour_editor_section_tour, true, true);

      final Composite container = (Composite) _sectionTitle.getClient();
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Title
             */
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_title);
            _firstColumnControls.add(label);

            // combo: tour title with history
            _comboTitle = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboTitle.setText(UI.EMPTY_STRING);

            _tk.adapt(_comboTitle, true, false);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .applyTo(_comboTitle);

            _comboTitle.addModifyListener(modifyEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }

               _isTitleModified = true;
               setTourDirty();

//                onModifyContent();
            });

            // fill combobox
            final ConcurrentSkipListSet<String> arr = TourDatabase.getCachedFields_AllTourTitles();
            arr.forEach(string -> _comboTitle.add(string));
            new AutocompleteComboInput(_comboTitle);
         }

         {
            /*
             * Description
             */
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_description);
            GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
            _firstColumnControls.add(label);

            _txtDescription = _tk.createText(
                  container,
                  UI.EMPTY_STRING,
                  SWT.BORDER
                        | SWT.WRAP
                        | SWT.V_SCROLL
                        | SWT.H_SCROLL //
            );

            // description will grab all vertical space in the tour tab
            GridDataFactory.fillDefaults()

                  .grab(true, true)

                  //
                  // SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
                  //
                  .hint(_hintTextColumnWidth, _pc.convertHeightInCharsToPixels(_descriptionNumLines))
                  .applyTo(_txtDescription);

            _txtDescription.addModifyListener(_modifyListener);
         }
         {
            /*
             * Start location
             */
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_start_location);
            _firstColumnControls.add(label);

            _comboLocation_Start = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboLocation_Start.setText(UI.EMPTY_STRING);

            _tk.adapt(_comboLocation_Start, true, false);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .applyTo(_comboLocation_Start);

            _comboLocation_Start.addModifyListener(modifyEvent -> {
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               _isLocationStartModified = true;
               setTourDirty();
            });

            // fill combobox
            final ConcurrentSkipListSet<String> arr = TourDatabase.getCachedFields_AllTourPlaceStarts();
            for (final String string : arr) {
               if (string != null) {
                  _comboLocation_Start.add(string);
               }
            }
            new AutocompleteComboInput(_comboLocation_Start);
         }
         {
            /*
             * End location
             */
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_end_location);
            _firstColumnControls.add(label);

            _comboLocation_End = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboLocation_End.setText(UI.EMPTY_STRING);

            _tk.adapt(_comboLocation_End, true, false);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_hintTextColumnWidth, SWT.DEFAULT)
                  .applyTo(_comboLocation_End);

            _comboLocation_End.addModifyListener(modifyEvent -> {
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               _isLocationEndModified = true;
               setTourDirty();
            });

            // fill combobox
            final ConcurrentSkipListSet<String> arr = TourDatabase.getCachedFields_AllTourPlaceEnds();
            for (final String string : arr) {
               if (string != null) {
                  _comboLocation_End.add(string);
               }
            }
            new AutocompleteComboInput(_comboLocation_End);
         }
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
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
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
         _dtTourDate.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

            if (UI.isLinuxAsyncEvent(selectionEvent.widget) || _isSetField || _isSavingInProgress) {
               return;
            }

            setTourDirty();

            updateUI_Title();

//          onModifyContent();

         }));

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
            _dtStartTime.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               if (UI.isLinuxAsyncEvent(selectionEvent.widget) || _isSetField || _isSavingInProgress) {
                  return;
               }

               setTourDirty();

               updateUI_Title();

//             onModifyContent();

            }));
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
            _txtDistance.setData(WIDGET_KEY, WIDGET_KEY_TOUR_DISTANCE);
            _txtDistance.addKeyListener(keyPressedAdapter(keyEvent -> _isDistManuallyModified = true));
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
            _txtAltitudeUp.addKeyListener(keyPressedAdapter(keyEvent -> _isAltitudeManuallyModified = true));

            GridDataFactory.fillDefaults().hint(_hintValueFieldWidth, SWT.DEFAULT).applyTo(_txtAltitudeUp);

            _lblAltitudeUpUnit = _tk.createLabel(container, UI.UNIT_LABEL_ELEVATION);
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
            _txtAltitudeDown.addKeyListener(keyPressedAdapter(keyEvent -> _isAltitudeManuallyModified = true));
            GridDataFactory.fillDefaults().hint(_hintValueFieldWidth, SWT.DEFAULT).applyTo(_txtAltitudeDown);

            _lblAltitudeDownUnit = _tk.createLabel(container, UI.UNIT_LABEL_ELEVATION);
         }
      }
   }

   /**
    * 2. column
    */
   private void createUI_Section_128_DateTime_Col2(final Composite section) {

      final Composite container = new Composite(section, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(COLUMN_SPACING, 0)
            .applyTo(container);
      {
         final Composite container_Left = _tk.createComposite(container);
         GridDataFactory.fillDefaults().applyTo(container_Left);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container_Left);
         {
            {
               /*
                * Elapsed time
                */
               final Label label = _tk.createLabel(container_Left, Messages.tour_editor_label_elapsed_time);
               _secondColumnControls.add(label);

               _deviceTime_Elapsed = new TimeDuration(container_Left);
            }
            {
               /*
                * Recorded time
                */
               final Label label = _tk.createLabel(container_Left, Messages.tour_editor_label_recorded_time);
               _secondColumnControls.add(label);

               _deviceTime_Recorded = new TimeDuration(container_Left);
            }
            {
               /*
                * Paused time
                */
               final Label label = _tk.createLabel(container_Left, Messages.tour_editor_label_paused_time);
               _secondColumnControls.add(label);

               _deviceTime_Paused = new TimeDuration(container_Left);
            }
         }

         final Composite container_Right = _tk.createComposite(container);
         GridDataFactory.fillDefaults()

               // align to the bottom that recorded/paused and moving/break time are at the same vertical position
               .align(SWT.FILL, SWT.END)

               .applyTo(container_Right);

         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container_Right);
         {
            /*
             * Moving time
             */
            final Label label = _tk.createLabel(container_Right, Messages.tour_editor_label_moving_time);
            _secondColumnControls.add(label);

            _computedTime_Moving = new TimeDuration(container_Right);
         }

         {
            /*
             * Break time
             */
            final Label label = _tk.createLabel(container_Right, Messages.tour_editor_label_break_time);
            _secondColumnControls.add(label);

            _computedTime_Break = new TimeDuration(container_Right);
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
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
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
            _comboTimeZone.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               _isTimeZoneManuallyModified = true;

               updateModel_FromUI();
               setTourDirty();

               updateUI_TimeZone();

            }));

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
                  _linkDefaultTimeZone.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionTimeZone_SetDefault()));
                  _tk.adapt(_linkDefaultTimeZone, true, true);
               }
               {
                  // link: from geo

                  _linkGeoTimeZone = new Link(actionContainer, SWT.NONE);
                  _linkGeoTimeZone.setText(Messages.Tour_Editor_Link_SetGeoTimeZone);
                  _linkGeoTimeZone.setToolTipText(Messages.Tour_Editor_Link_SetGeoTimeZone_Tooltip);
                  _linkGeoTimeZone.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionTimeZone_SetFromGeo()));
                  _tk.adapt(_linkGeoTimeZone, true, true);
               }
               {
                  // link: remove

                  _linkRemoveTimeZone = new Link(actionContainer, SWT.NONE);
                  _linkRemoveTimeZone.setText(Messages.Tour_Editor_Link_RemoveTimeZone);
                  _linkRemoveTimeZone.setToolTipText(Messages.Tour_Editor_Link_RemoveTimeZone_Tooltip);
                  _linkRemoveTimeZone.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionTimeZone_Remove()));
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
   private void createUI_Section_132_PersonalCol1(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
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
            _spinPerson_Calories = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_spinPerson_Calories);
            _spinPerson_Calories.setMinimum(0);
            _spinPerson_Calories.setMaximum(1_000_000_000);
            _spinPerson_Calories.setDigits(3);

            _spinPerson_Calories.addMouseWheelListener(_mouseWheelListener);
            _spinPerson_Calories.addSelectionListener(_selectionListener);

            // label: kcal
            _tk.createLabel(container, OtherMessages.VALUE_UNIT_K_CALORIES);
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
            _spinPerson_RestPulse = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinPerson_RestPulse);
            _spinPerson_RestPulse.setMinimum(0);
            _spinPerson_RestPulse.setMaximum(200);
            _spinPerson_RestPulse.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);

            _spinPerson_RestPulse.addMouseWheelListener(_mouseWheelListener);
            _spinPerson_RestPulse.addSelectionListener(_selectionListener);

            // label: bpm
            _tk.createLabel(container, OtherMessages.GRAPH_LABEL_HEARTBEAT_UNIT);
         }
         {
            /*
             * FTP - Functional Threshold Power
             */

            // label: FTP
            final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_FTP);
            label.setToolTipText(Messages.Tour_Editor_Label_FTP_Tooltip);
            _firstColumnControls.add(label);

            // spinner: FTP
            _spinPerson_FTP = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinPerson_FTP);
            _spinPerson_FTP.setMinimum(0);
            _spinPerson_FTP.setMaximum(10000);

            _spinPerson_FTP.addMouseWheelListener(_mouseWheelListener);
            _spinPerson_FTP.addSelectionListener(_selectionListener);

            // spacer
            _tk.createLabel(container, UI.EMPTY_STRING);
         }
      }
   }

   /**
    * 2. column
    */
   private void createUI_Section_134_PersonalCol2(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
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
            _spinPerson_BodyWeight = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinPerson_BodyWeight);
            _spinPerson_BodyWeight.setDigits(1);
            _spinPerson_BodyWeight.setMinimum(0);
            _spinPerson_BodyWeight.setMaximum(6614); // 300.0 kg, 661.4 lbs

            _spinPerson_BodyWeight.addMouseWheelListener(_mouseWheelListener);
            _spinPerson_BodyWeight.addSelectionListener(_selectionListener);

            // label: unit
            _lblPerson_BodyWeightUnit = _tk.createLabel(container, UI.UNIT_LABEL_WEIGHT);
         }
         {
            /*
             * Body fat
             */

            // label: fat
            final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_BodyFat);
            label.setToolTipText(Messages.Tour_Editor_Label_BodyFat_Tooltip);
            _secondColumnControls.add(label);

            // spinner: fat
            _spinPerson_BodyFat = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinPerson_BodyFat);
            _spinPerson_BodyFat.setDigits(1);
            _spinPerson_BodyFat.setMinimum(0);
            _spinPerson_BodyFat.setMaximum(1000); // 100%

            _spinPerson_BodyFat.addMouseWheelListener(_mouseWheelListener);
            _spinPerson_BodyFat.addSelectionListener(_selectionListener);

            // label: unit
            _lblPerson_BodyFatUnit = _tk.createLabel(container, UI.UNIT_PERCENT);
         }
      }
   }

   private void createUI_Section_140_Weather(final Composite parent) {

      _sectionWeather = createSection(parent, _tk, Messages.tour_editor_section_weather, false, true);
      final Composite container = (Composite) _sectionWeather.getClient();
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(COLUMN_SPACING, 7)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_Section_141_Weather_Description(container);

         createUI_Section_142_Weather_Wind_Col1(container);
         createUI_Section_143_Weather_Wind_Col2(container);

         createUI_Section_144_Weather_Temperature_Col1(container);
         createUI_Section_144_Weather_Temperature_Col2_Device(container);

         createUI_Section_147_Weather_Other_Col1(container);
         createUI_Section_148_Weather_Other_Col2(container);

         createUI_Section_149_Weather_Other_Col1(container);
      }
   }

   private void createUI_Section_141_Weather_Description(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         {
            /*
             * weather description
             */
            _linkWeather = new Link(container, SWT.NONE);
            _linkWeather.setText(Messages.Tour_Editor_Link_RetrieveWeather);
            _linkWeather.setToolTipText(Messages.Tour_Editor_Link_RetrieveWeather_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(_linkWeather);
            _linkWeather.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               //Retrieve the weather
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_Weather_Text();
            }));
            _tk.adapt(_linkWeather, true, true);
            _firstColumnControls.add(_linkWeather);

            _txtWeather = _tk.createText(
                  container,
                  UI.EMPTY_STRING,
                  SWT.BORDER //
                        | SWT.WRAP
                        | SWT.V_SCROLL
                        | SWT.H_SCROLL//
            );
            _txtWeather.addModifyListener(_modifyListener);

            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .span(2, 1)
                  //
                  // SWT.DEFAULT causes lots of problems with the layout therefore the hint is set
                  //
                  .hint(_hintTextColumnWidth, _pc.convertHeightInCharsToPixels(weatherDescriptionNumLines))
                  .applyTo(_txtWeather);
         }
         {
            /*
             * Clouds
             */
            final Composite cloudContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(cloudContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cloudContainer);
            {
               // label: clouds
               final Label label = _tk.createLabel(cloudContainer, Messages.tour_editor_label_clouds);
               label.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);

               // icon: clouds
               _lblCloudIcon = new Label(cloudContainer, SWT.NONE);
               GridDataFactory
                     .fillDefaults()//
                     .align(SWT.END, SWT.FILL)
                     .grab(true, false)
                     .applyTo(_lblCloudIcon);
            }

            // combo: clouds
            _comboWeather_Clouds = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboWeather_Clouds);
            _tk.adapt(_comboWeather_Clouds, true, false);
            _comboWeather_Clouds.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);
            _comboWeather_Clouds.setVisibleItemCount(10);
            _comboWeather_Clouds.addModifyListener(_modifyListener);
            _comboWeather_Clouds.addSelectionListener(widgetSelectedAdapter(selectionEvent -> displayCloudIcon()));

            // fill combobox
            for (final String cloudText : IWeather.cloudText) {
               _comboWeather_Clouds.add(cloudText);
            }

            // force the icon to be displayed to ensure the width is correctly set when the size is computed
            _isSetField = true;
            {
               _comboWeather_Clouds.select(0);
               displayCloudIcon();
            }
            _isSetField = false;
         }
      }
   }

   private void createUI_Section_142_Weather_Wind_Col1(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      _firstColumnContainerControls.add(container);
      {
         {
            /*
             * wind speed
             */

            // label
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinWeather_Wind_SpeedValue = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Wind_SpeedValue);
            _spinWeather_Wind_SpeedValue.setMinimum(0);
            _spinWeather_Wind_SpeedValue.setMaximum(120);
            _spinWeather_Wind_SpeedValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            _spinWeather_Wind_SpeedValue.addModifyListener(modifyEvent -> {
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindSpeedValue();
               setTourDirty();
            });
            _spinWeather_Wind_SpeedValue.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindSpeedValue();
               setTourDirty();
            }));
            _spinWeather_Wind_SpeedValue.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindSpeedValue();
               setTourDirty();
            });

            // label: km/h, mi/h
            _lblSpeedUnit = _tk.createLabel(container, UI.UNIT_LABEL_SPEED);
         }
         {
            /*
             * Wind direction
             */

            // label
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_direction);
            label.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);
            _firstColumnControls.add(label);

            // combo: wind direction text
            _comboWeather_Wind_DirectionText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _tk.adapt(_comboWeather_Wind_DirectionText, true, false);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_comboWeather_Wind_DirectionText);
            _comboWeather_Wind_DirectionText.setToolTipText(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
            _comboWeather_Wind_DirectionText.setVisibleItemCount(16);
            _comboWeather_Wind_DirectionText.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindDirection_Text(_spinWeather_Wind_DirectionValue, _comboWeather_Wind_DirectionText);
               setTourDirty();
            }));

            // fill combobox
            for (final String windDirText : IWeather.windDirectionText) {
               _comboWeather_Wind_DirectionText.add(windDirText);
            }

            // spacer
            new Label(container, SWT.NONE);
         }
      }

   }

   private void createUI_Section_143_Weather_Wind_Col2(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * wind speed
             */

            // combo: wind speed with text
            _comboWeather_WindSpeedText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .span(2, 1)
                  .applyTo(_comboWeather_WindSpeedText);
            _tk.adapt(_comboWeather_WindSpeedText, true, false);
            _comboWeather_WindSpeedText.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _comboWeather_WindSpeedText.setVisibleItemCount(20);
            _comboWeather_WindSpeedText.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindSpeedText();
               setTourDirty();
            }));

            // fill combobox
            for (final String speedText : IWeather.windSpeedText) {
               _comboWeather_WindSpeedText.add(speedText);
            }
         }
         {
            /*
             * wind direction
             */

            // spinner: wind direction value
            _spinWeather_Wind_DirectionValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Wind_DirectionValue);
            _spinWeather_Wind_DirectionValue.setMinimum(-1);
            _spinWeather_Wind_DirectionValue.setMaximum(3600);
            _spinWeather_Wind_DirectionValue.setDigits(1);
            _spinWeather_Wind_DirectionValue.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);

            _spinWeather_Wind_DirectionValue.addModifyListener(modifyEvent -> {
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindDirection_Value(_spinWeather_Wind_DirectionValue, _comboWeather_Wind_DirectionText);
               setTourDirty();
            });
            _spinWeather_Wind_DirectionValue.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindDirection_Value(_spinWeather_Wind_DirectionValue, _comboWeather_Wind_DirectionText);
               setTourDirty();
            }));

            _spinWeather_Wind_DirectionValue.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               if (_isSetField || _isSavingInProgress) {
                  return;
               }
               onSelect_WindDirection_Value(_spinWeather_Wind_DirectionValue, _comboWeather_Wind_DirectionText);
               setTourDirty();
            });

            // label: direction unit = degree
            _tk.createLabel(container, Messages.Tour_Editor_Label_WindDirection_Unit);
         }
      }
   }

   /**
    * Weather from device
    */
   private void createUI_Section_144_Weather_Temperature_Col1(final Composite parent) {

      Label label;

      final Composite container = _tk.createComposite(parent);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
//            .spacing(5, 5)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      _firstColumnContainerControls.add(container);
      {
         {
            /*
             * Average Temperature
             */

            // label
            label = _tk.createLabel(container, Messages.Tour_Editor_Label_Temperature);
            label.setToolTipText(Messages.Tour_Editor_Label_Temperature_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinWeather_Temperature_Average = new Spinner(container, SWT.BORDER);
            _spinWeather_Temperature_Average.setToolTipText(Messages.Tour_Editor_Label_Temperature_Avg_Tooltip);

            // the min/max temperature has a large range because fahrenheit has bigger values than celsius
            _spinWeather_Temperature_Average.setMinimum(-600);
            _spinWeather_Temperature_Average.setMaximum(1500);

            _spinWeather_Temperature_Average.addModifyListener(_modifyListener_Temperature);
            _spinWeather_Temperature_Average.addSelectionListener(_selectionListener_Temperature);
            _spinWeather_Temperature_Average.addMouseWheelListener(_mouseWheelListener_Temperature);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Temperature_Average);

            // label: celsius, fahrenheit
            _lblWeather_TemperatureUnit_Avg = _tk.createLabel(container, UI.SYMBOL_AVERAGE + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_Avg.setToolTipText(Messages.Tour_Editor_Label_Temperature_Avg_Tooltip);
         }
         {
            /*
             * Minimum Temperature
             */

            // spacer
            new Label(container, SWT.NONE);

            // spinner
            _spinWeather_Temperature_Min = new Spinner(container, SWT.BORDER);
            _spinWeather_Temperature_Min.setToolTipText(Messages.Tour_Editor_Label_Temperature_Min_Tooltip);
            _spinWeather_Temperature_Min.addModifyListener(_modifyListener_Temperature);
            _spinWeather_Temperature_Min.addSelectionListener(_selectionListener_Temperature);
            _spinWeather_Temperature_Min.addMouseWheelListener(_mouseWheelListener_Temperature);

            // the min/max temperature has a large range because fahrenheit has bigger values than celsius
            _spinWeather_Temperature_Min.setMinimum(-600);
            _spinWeather_Temperature_Min.setMaximum(1500);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Temperature_Min);

            // unit
            _lblWeather_TemperatureUnit_Min = _tk.createLabel(container, UI.SYMBOL_MIN + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_Min.setToolTipText(Messages.Tour_Editor_Label_Temperature_Min_Tooltip);
         }
         {
            /*
             * Maximum Temperature
             */

            // spacer
            new Label(container, SWT.NONE);

            // spinner
            _spinWeather_Temperature_Max = new Spinner(container, SWT.BORDER);
            _spinWeather_Temperature_Max.setToolTipText(Messages.Tour_Editor_Label_Temperature_Max_Tooltip);
            _spinWeather_Temperature_Max.addModifyListener(_modifyListener_Temperature);
            _spinWeather_Temperature_Max.addSelectionListener(_selectionListener_Temperature);
            _spinWeather_Temperature_Max.addMouseWheelListener(_mouseWheelListener_Temperature);

            // the min/max temperature has a large range because fahrenheit has bigger values than celsius
            _spinWeather_Temperature_Max.setMinimum(-600);
            _spinWeather_Temperature_Max.setMaximum(1500);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Temperature_Max);

            // unit
            _lblWeather_TemperatureUnit_Max = _tk.createLabel(container, UI.SYMBOL_MAX + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_Max.setToolTipText(Messages.Tour_Editor_Label_Temperature_Max_Tooltip);
         }
         {
            /*
             * Wind chill
             */

            // spacer
            new Label(container, SWT.NONE);

            // spinner
            _spinWeather_Temperature_WindChill = new Spinner(container, SWT.BORDER);
            _spinWeather_Temperature_WindChill.setToolTipText(Messages.Tour_Editor_Label_Temperature_WindChill_Tooltip);
            _spinWeather_Temperature_WindChill.addModifyListener(_modifyListener_Temperature);
            _spinWeather_Temperature_WindChill.addSelectionListener(_selectionListener_Temperature);
            _spinWeather_Temperature_WindChill.addMouseWheelListener(_mouseWheelListener_Temperature);

            // the min/max temperature has a large range because fahrenheit has bigger values than celsius
            _spinWeather_Temperature_WindChill.setMinimum(-600);
            _spinWeather_Temperature_WindChill.setMaximum(1500);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Temperature_WindChill);

            // unit
            _lblWeather_TemperatureUnit_WindChill = _tk.createLabel(container, UI.SYMBOL_TILDE + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_WindChill.setToolTipText(Messages.Tour_Editor_Label_Temperature_WindChill_Tooltip);
         }
      }
   }

   /**
    * weather
    */
   private void createUI_Section_144_Weather_Temperature_Col2_Device(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Average Temperature from device
             */

            // spinner
            _txtWeather_Temperature_Average_Device = _tk.createText(container, UI.EMPTY_STRING, SWT.READ_ONLY);
            _txtWeather_Temperature_Average_Device.setToolTipText(Messages.Tour_Editor_Label_Temperature_Avg_Device_Tooltip);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtWeather_Temperature_Average_Device);

            // label: celsius, fahrenheit
            _lblWeather_TemperatureUnit_Avg_Device = _tk.createLabel(container, UI.SYMBOL_AVERAGE + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_Avg_Device.setToolTipText(Messages.Tour_Editor_Label_Temperature_Avg_Device_Tooltip);
         }
         {
            /*
             * Minimum Temperature from device
             */

            // spinner
            _txtWeather_Temperature_Min_Device = _tk.createText(container, UI.EMPTY_STRING, SWT.READ_ONLY);
            _txtWeather_Temperature_Min_Device.setToolTipText(Messages.Tour_Editor_Label_Temperature_Min_Device_Tooltip);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtWeather_Temperature_Min_Device);

            // unit
            _lblWeather_TemperatureUnit_Min_Device = _tk.createLabel(container, UI.SYMBOL_MIN + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_Min_Device.setToolTipText(Messages.Tour_Editor_Label_Temperature_Min_Device_Tooltip);
         }
         {
            /*
             * Maximum Temperature from device
             */

            // spinner
            _txtWeather_Temperature_Max_Device = _tk.createText(container, UI.EMPTY_STRING, SWT.READ_ONLY);
            _txtWeather_Temperature_Max_Device.setToolTipText(Messages.Tour_Editor_Label_Temperature_Max_Device_Tooltip);

            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtWeather_Temperature_Max_Device);

            // unit
            _lblWeather_TemperatureUnit_Max_Device = _tk.createLabel(container, UI.SYMBOL_MAX + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);
            _lblWeather_TemperatureUnit_Max_Device.setToolTipText(Messages.Tour_Editor_Label_Temperature_Max_Device_Tooltip);
         }
      }
   }

   private void createUI_Section_147_Weather_Other_Col1(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      _firstColumnContainerControls.add(container);
      {
         /*
          * Pressure
          */
         // label
         Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_AirPressure);
         label.setToolTipText(Messages.Tour_Editor_Label_AirPressure_Tooltip);
         _firstColumnControls.add(label);

         // spinner: pressure value
         _spinWeather_PressureValue = new Spinner(container, SWT.BORDER);
         _spinWeather_PressureValue.setToolTipText(Messages.Tour_Editor_Label_AirPressure_Tooltip);
         //The highest barometric pressure ever recorded on Earth was 32.01 inches (1083.98), measured in Agata, U.S.S.R., on December 31, 1968.
         _spinWeather_PressureValue.setMaximum(110000);
         _spinWeather_PressureValue.addMouseWheelListener(_mouseWheelListener);
         _spinWeather_PressureValue.addSelectionListener(_selectionListener);

         GridDataFactory.fillDefaults()
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinWeather_PressureValue);

         // label: mb, inHg
         _lblWeather_PressureUnit = _tk.createLabel(container, UI.UNIT_LABEL_PRESSURE_MBAR_OR_INHG);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblWeather_PressureUnit);

         // spacer
         label = new Label(container, SWT.NONE);
      }
      {
         /*
          * Humidity
          */
         // label
         Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Humidity);
         label.setToolTipText(Messages.Tour_Editor_Label_Humidity_Tooltip);
         _firstColumnControls.add(label);

         // spinner: humidity value
         _spinWeather_Humidity = new Spinner(container, SWT.BORDER);
         _spinWeather_Humidity.setToolTipText(Messages.Tour_Editor_Label_Humidity_Tooltip);
         _spinWeather_Humidity.setMinimum(0);
         _spinWeather_Humidity.setMaximum(100);
         _spinWeather_Humidity.addMouseWheelListener(_mouseWheelListener);
         _spinWeather_Humidity.addSelectionListener(_selectionListener);

         GridDataFactory
               .fillDefaults()
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinWeather_Humidity);

         // label: mm, inches
         label = _tk.createLabel(container, UI.UNIT_PERCENT);
      }
   }

   private void createUI_Section_148_Weather_Other_Col2(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * Precipitation
          */
         // label
         final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Precipitation);
         label.setToolTipText(Messages.Tour_Editor_Label_Precipitation_Tooltip);
         _secondColumnControls.add(label);

         // spinner: precipitation value
         _spinWeather_PrecipitationValue = new Spinner(container, SWT.BORDER);
         _spinWeather_PrecipitationValue.setToolTipText(Messages.Tour_Editor_Label_Precipitation_Tooltip);
         _spinWeather_PrecipitationValue.setMaximum(10000);
         _spinWeather_PrecipitationValue.addMouseWheelListener(_mouseWheelListener);
         _spinWeather_PrecipitationValue.addSelectionListener(_selectionListener);

         GridDataFactory
               .fillDefaults()
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinWeather_PrecipitationValue);

         // label: mm, inches
         _lblWeather_PrecipitationUnit = _tk.createLabel(container, UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      }
      {
         /*
          * Snowfall
          */
         final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Snowfall);
         label.setToolTipText(Messages.Tour_Editor_Label_Snowfall_Tooltip);
         _secondColumnControls.add(label);

         // spinner: humidity value
         _spinWeather_SnowfallValue = new Spinner(container, SWT.BORDER);
         _spinWeather_SnowfallValue.setToolTipText(Messages.Tour_Editor_Label_Snowfall_Tooltip);
         _spinWeather_SnowfallValue.setMaximum(10000);
         _spinWeather_SnowfallValue.addMouseWheelListener(_mouseWheelListener);
         _spinWeather_SnowfallValue.addSelectionListener(_selectionListener);

         GridDataFactory
               .fillDefaults()
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinWeather_SnowfallValue);

         // label: mm, inches
         _lblWeather_SnowfallUnit = _tk.createLabel(container, UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      }
   }

   private void createUI_Section_149_Weather_Other_Col1(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
      _firstColumnContainerControls.add(container);
      {
         /*
          * Air Quality
          */

         // label
         final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_AirQuality);
         label.setToolTipText(Messages.Tour_Editor_Label_AirQuality_Tooltip);
         _firstColumnControls.add(label);

         // combo: Air quality
         _tableComboWeather_AirQuality = new TableCombo(container, SWT.READ_ONLY | SWT.BORDER);
         _tableComboWeather_AirQuality.setToolTipText(Messages.Tour_Editor_Label_AirQuality_Tooltip);
         _tableComboWeather_AirQuality.setShowTableHeader(false);
         _tableComboWeather_AirQuality.defineColumns(1);
         _tableComboWeather_AirQuality.addModifyListener(_modifyListener);

         // We update the model in the selection listener as the selected
         // index value comes back with -1 in the modify listener
         _tableComboWeather_AirQuality.addSelectionListener(
               widgetSelectedAdapter(selectionEvent -> onSelect_AirQuality()));

         _tk.adapt(_tableComboWeather_AirQuality, true, false);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_tableComboWeather_AirQuality);

         fillAirQualityCombo();

         // force the icon to be displayed to ensure the width is correctly set when the size is computed
         _isSetField = true;
         {
            _tableComboWeather_AirQuality.select(0);
         }
         _isSetField = false;
      }
   }

   private void createUI_Section_150_Characteristics(final Composite parent) {

      _sectionCharacteristics = createSection(parent, _tk, Messages.tour_editor_section_characteristics, false, true);
      final Composite container = (Composite) _sectionCharacteristics.getClient();
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Tag Menu
             */
            _linkTag = new Link(container, SWT.NONE);
            _linkTag.setText(Messages.tour_editor_label_tour_tag);
            _linkTag.addSelectionListener(widgetSelectedAdapter(selectionEvent -> UI.openControlMenu(_linkTag)));
            _tk.adapt(_linkTag, true, true);
            _firstColumnControls.add(_linkTag);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.BEGINNING)
                  .applyTo(_linkTag);

            {
               /*
                * Tag label/image
                */
               final GridDataFactory gdForTagContent = GridDataFactory.fillDefaults().grab(true, true)

                     /*
                      * Hint is necessary that the width is not expanded when the text is long
                      */
                     .hint(2 * _hintTextColumnWidth, SWT.DEFAULT);

               _pageBook_Tags = new PageBook(container, SWT.NONE);
//               _pageBook_Tags.setBackground(UI.SYS_COLOR_BLUE);
               gdForTagContent.grab(false, false).span(3, 1).applyTo(_pageBook_Tags);
               {
                  _lblTags = _tk.createLabel(_pageBook_Tags, UI.EMPTY_STRING, SWT.WRAP);
                  gdForTagContent.applyTo(_lblTags);
               }
               {

                  _containerTags_Scrolled = new ScrolledComposite(_pageBook_Tags, SWT.V_SCROLL | SWT.H_SCROLL);
                  _containerTags_Scrolled.setExpandVertical(true);
                  _containerTags_Scrolled.setExpandHorizontal(true);

                  _containerTags_Content = new Composite(_containerTags_Scrolled, SWT.NONE);

                  _containerTags_Scrolled.setContent(_containerTags_Content);
                  _containerTags_Scrolled.addControlListener(controlResizedAdapter(controlEvent -> onResize_TagContent()));

                  GridLayoutFactory.fillDefaults()
                        .numColumns(TagManager.getNumberOfTagContentColumns())
                        .applyTo(_containerTags_Content);

                  gdForTagContent.applyTo(_containerTags_Content);
               }
               {
                  _lblNoTags = UI.createLabel(_pageBook_Tags, UI.EMPTY_STRING);
               }
            }
         }

         {
            /*
             * Tour type
             */
            _linkTourType = new Link(container, SWT.NONE);
            _linkTourType.setText(Messages.tour_editor_label_tour_type);
            _linkTourType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> UI.openControlMenu(_linkTourType)));
            _tk.adapt(_linkTourType, true, true);
            _firstColumnControls.add(_linkTourType);

            _lblTourType = new CLabel(container, SWT.NONE);
            GridDataFactory.swtDefaults()
                  .grab(true, false)
                  .span(3, 1)
                  .applyTo(_lblTourType);
         }

         {
            /*
             * Cadence: rpm/spm
             */

            // label
            final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Cadence);
            label.setToolTipText(Messages.Tour_Editor_Label_Cadence_Tooltip);
            _firstColumnControls.add(label);

            _comboCadence = new ComboViewerCadence(container);
            _comboCadence.addSelectionChangedListener(selectionChangedEvent -> {

               if (_isSetField || _isSavingInProgress) {
                  return;
               }

               updateModel_FromUI();
               setTourDirty();
            });
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
      _tab1Container.addControlListener(controlResizedAdapter(controlEvent -> onResize_Tab1()));
      {
         _tourContainer = new Composite(_tab1Container, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(_tourContainer);
         _tk.adapt(_tourContainer);
         GridLayoutFactory.swtDefaults().applyTo(_tourContainer);
//       _tourContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));

         // set content for scrolled composite
         _tab1Container.setContent(_tourContainer);

         _tk.setBorderStyle(SWT.BORDER);
         {
            createUI_Section_110_Tour(_tourContainer);
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
      parent.getShell().getDisplay().asyncExec(() -> {

         if (_tab1Container.isDisposed()) {

            // this can occur when view is closed (very early) but not yet visible
            return;
         }

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
//    table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

//    table.addTraverseListener(new TraverseListener() {
//       public void keyTraversed(final TraverseEvent e) {
//          e.doit = e.keyCode != SWT.CR; // vetoes all CR traversals
//       }
//    });

      table.addKeyListener(keyPressedAdapter(keyEvent -> {

         if ((_isEditMode == false) || (isTourInDb() == false)) {
            return;
         }

         if (keyEvent.keyCode == SWT.DEL) {

            final boolean isKeepDistance = Util.getStateBoolean(_state,
                  STATE_IS_DELETE_KEEP_DISTANCE,
                  STATE_IS_DELETE_KEEP_DISTANCE_DEFAULT);

            final boolean isKeepTime = Util.getStateBoolean(_state,
                  STATE_IS_DELETE_KEEP_TIME,
                  STATE_IS_DELETE_KEEP_TIME_DEFAULT);

            final boolean isRemoveDistance = isKeepDistance == false;
            final boolean isRemoveTime = isKeepTime == false;

            actionDelete_TimeSlices(isRemoveTime, isRemoveDistance, false);
         }
      }));

      _timeSlice_Viewer = new TableViewer(table);

      if (_isRowEditMode == false) {
         UI.setCellEditSupport(_timeSlice_Viewer);
      }

      /*
       * create editing support after the viewer is created but before the columns are created.
       */
      final TextCellEditor textCellEditor = new CellEditor_Text_Customized(_timeSlice_Viewer.getTable());

// SET_FORMATTING_OFF

      _timeSlice_AltitudeEditingSupport      = new SliceEditingSupport_Float(textCellEditor,    _serieAltitude);
      _timeSlice_PulseEditingSupport         = new SliceEditingSupport_Float(textCellEditor,    _seriePulse);
      _timeSlice_TemperatureEditingSupport   = new SliceEditingSupport_Float(textCellEditor,    _serieTemperature);
      _timeSlice_CadenceEditingSupport       = new SliceEditingSupport_Float(textCellEditor,    _serieCadence);
      _timeSlice_LatitudeEditingSupport      = new SliceEditingSupport_Double(textCellEditor,   _serieLatitude);
      _timeSlice_LongitudeEditingSupport     = new SliceEditingSupport_Double(textCellEditor,   _serieLongitude);

      _timeSlice_ColDef_Altitude    .setEditingSupport(_timeSlice_AltitudeEditingSupport);
      _timeSlice_ColDef_Pulse       .setEditingSupport(_timeSlice_PulseEditingSupport);
      _timeSlice_ColDef_Temperature .setEditingSupport(_timeSlice_TemperatureEditingSupport);
      _timeSlice_ColDef_Cadence     .setEditingSupport(_timeSlice_CadenceEditingSupport);
      _timeSlice_ColDef_Latitude    .setEditingSupport(_timeSlice_LatitudeEditingSupport);
      _timeSlice_ColDef_Longitude   .setEditingSupport(_timeSlice_LongitudeEditingSupport);

// SET_FORMATTING_ON

      _timeSlice_ColumnManager.createColumns(_timeSlice_Viewer);

      _timeSlice_Viewer.setContentProvider(new TimeSlice_ViewerContentProvider());
      _timeSlice_Viewer.setComparator(_timeSlice_Comparator);
      _timeSlice_Viewer.setUseHashlookup(true);
      _timeSlice_Viewer.addSelectionChangedListener(this::onSelect_Slice);

      // hide first column, this is a hack to align the "first" visible column to right
      table.getColumn(0).setWidth(0);

      createUI_Tab_24_TimeSliceViewerContextMenu();
   }

   private void createUI_Tab_24_TimeSliceViewerContextMenu() {

      _timeViewer_ContextMenu = createUI_Tab_26_TimeSliceViewerContextMenu_Menu();

      final Table table = _timeSlice_Viewer.getTable();

      _timeSlice_ColumnManager.createHeaderContextMenu(table, _timeViewer_ContextMenuProvider);
   }

   private Menu createUI_Tab_26_TimeSliceViewerContextMenu_Menu() {

      final Table table = _timeSlice_Viewer.getTable();
      final Menu tableContextMenu = _timeViewer_MenuManager.createContextMenu(table);

      return tableContextMenu;
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

// table header is disabled because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=536021
// Table: right-aligned column header with own background color lacks margin
//    table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

//    table.addKeyListener(new KeyAdapter() {
//       @Override
//       public void keyPressed(final KeyEvent e) {
//
//          if ((_isEditMode == false) || (isTourInDb() == false)) {
//             return;
//          }
//
//          if (e.keyCode == SWT.DEL) {
//             actionDeleteTimeSlices(true);
//          }
//       }
//    });

      _swimSlice_Viewer = new TableViewer(table);

      if (_isRowEditMode == false) {
         UI.setCellEditSupport(_swimSlice_Viewer);
      }

      /*
       * create editing support after the viewer is created but before the columns are created.
       */
      final TextCellEditor textCellEditor = new CellEditor_Text_Customized(_swimSlice_Viewer.getTable());
      final ComboBoxCellEditor strokeStyleCellEditor = new CellEditor_ComboBox_Customized(
            _swimSlice_Viewer.getTable(),
            SwimStrokeManager.getAllSortedSwimStrokeLabel());

      _swimSlice_StrokeRateEditingSupport = new SliceEditingSupport_Short(textCellEditor, _swimSerie_StrokeRate);
      _swimSlice_StrokesEditingSupport = new SliceEditingSupport_Short(textCellEditor, _swimSerie_StrokesPerlength);
      _swimSlice_StrokeStyleEditingSupport = new SliceEditor_ComboBox_StrokeStyle(strokeStyleCellEditor, _swimSerie_StrokeStyle);

      _swimSlice_ColDef_StrokeRate.setEditingSupport(_swimSlice_StrokeRateEditingSupport);
      _swimSlice_ColDef_Strokes.setEditingSupport(_swimSlice_StrokesEditingSupport);
      _swimSlice_ColDef_StrokeStyle.setEditingSupport(_swimSlice_StrokeStyleEditingSupport);

      _swimSlice_ColumnManager.createColumns(_swimSlice_Viewer);

      _swimSlice_Viewer.setContentProvider(new SwimSlice_ViewerContentProvider());
      _swimSlice_Viewer.setUseHashlookup(true);
      _swimSlice_Viewer.addSelectionChangedListener(this::onSelect_Slice);

      createUI_Tab_34_SwimSliceViewerContextMenu();

      // hide first column, this is a hack to align the "first" visible column to right
      table.getColumn(0).setWidth(0);
   }

   private void createUI_Tab_34_SwimSliceViewerContextMenu() {

      _swimViewer_ContextMenu = createUI_Tab_36_SwimSliceViewerContextMenu_Menu();

      final Table table = _swimSlice_Viewer.getTable();

      _swimSlice_ColumnManager.createHeaderContextMenu(table, _swimViewer_ContextMenuProvider);
   }

   private Menu createUI_Tab_36_SwimSliceViewerContextMenu_Menu() {

      final Table table = _swimSlice_Viewer.getTable();

      final Menu tableContextMenu = _swimViewer_MenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns_SwimSlices() {

      defineColumn_SwimSlice_Data_1_First();
      defineColumn_SwimSlice_Data_Sequence();

      defineColumn_SwimSlice_Time_TimeInHHMMSSRelative();
      defineColumn_SwimSlice_Time_TimeOfDay();
      defineColumn_SwimSlice_Time_TimeInSeconds();
      defineColumn_SwimSlice_Time_TimeDiff();

      defineColumn_SwimSlice_Swim_StrokesPerLength();
      defineColumn_SwimSlice_Swim_StrokeRate();
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
      defineColumn_TimeSlice_Time_PausedTime();

      defineColumn_TimeSlice_Motion_Distance();
      defineColumn_TimeSlice_Motion_Speed();
      defineColumn_TimeSlice_Motion_Pace();
      defineColumn_TimeSlice_Motion_Latitude();
      defineColumn_TimeSlice_Motion_Longitude();
      defineColumn_TimeSlice_Motion_DistanceDiff();
      defineColumn_TimeSlice_Motion_SpeedDiff();

      defineColumn_TimeSlice_Elevation_Elevation();
      defineColumn_TimeSlice_Elevation_Gradient();

      defineColumn_TimeSlice_Body_Heartbeat_Device();
      defineColumn_TimeSlice_Body_Heartbeat_RR();
      defineColumn_TimeSlice_Body_Heartbeat_RR_Intervals();
      defineColumn_TimeSlice_Body_Heartbeat_RR_Index();

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

            cell.setForeground(_foregroundColor_1stColumn_NoRefTour);
            cell.setBackground(_backgroundColor_1stColumn_NoRefTour);

            // the UI shows the time slice number starting with 1 and not with 0
            cell.setText(Integer.toString(logIndex + 1));
         }
      });
   }

   /**
    * Stroke Rate or number of strokes per minute.
    */
   private void defineColumn_SwimSlice_Swim_StrokeRate() {

      TableColumnDefinition colDef;

      _swimSlice_ColDef_StrokeRate = colDef = TableColumnFactory.SWIM__SWIM_STROKE_RATE.createColumn(_swimSlice_ColumnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            if (_swimSerie_StrokeRate != null) {

               final SwimSlice timeSlice = (SwimSlice) cell.getElement();
               final short value = _swimSerie_StrokeRate[timeSlice.serieIndex];

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

   private void defineColumn_SwimSlice_Swim_StrokesPerLength() {

      final ColumnDefinition colDef;

      _swimSlice_ColDef_Strokes = colDef = TableColumnFactory.SWIM__SWIM_STROKES_PER_LENGTH.createColumn(_swimSlice_ColumnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            if (_swimSerie_StrokesPerlength != null) {

               final SwimSlice timeSlice = (SwimSlice) cell.getElement();
               final short value = _swimSerie_StrokesPerlength[timeSlice.serieIndex];

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

               final SwimSlice swimSlice = (SwimSlice) cell.getElement();
               final short value = _swimSerie_StrokeStyle[swimSlice.serieIndex];

               if (value == Short.MIN_VALUE || value == SwimStroke.INVALID.getValue()) {

                  // show nothing instead of "<Invalid Style>"

                  cell.setText(UI.EMPTY_STRING);

               } else {

                  final SwimStroke swimStroke = SwimStroke.getByValue(value);
                  cell.setText(SwimStrokeManager.getLabel(swimStroke));
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
    * Column: Time of day in hh:mm:ss or hh:mm:ss am/pm
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
               final int timeSliceSeconds = _swimSerie_Time[serieIndex];

               cell.setText(_tourStartTime.plusSeconds(timeSliceSeconds).format(TimeTools.Formatter_Time_M));
            }
         }
      });
   }

   /**
    * Column: Heartbeat from device
    */
   private void defineColumn_TimeSlice_Body_Heartbeat_Device() {

      ColumnDefinition colDef;

      _timeSlice_ColDef_Pulse = colDef = TableColumnFactory.BODY_PULSE.createColumn(_timeSlice_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

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
    * Column: Heartbeat from R-R intervals
    */
   private void defineColumn_TimeSlice_Body_Heartbeat_RR() {

      final TableColumnDefinition colDef = TableColumnFactory.BODY_PULSE_RR_AVG_BPM.createColumn(_timeSlice_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            if (_seriePulse_RR_Bpm != null) {

               final TimeSlice timeSlice = (TimeSlice) cell.getElement();
               final float value = _seriePulse_RR_Bpm[timeSlice.serieIndex];

               colDef.printDetailValue(cell, value);

            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: R-R index
    */
   private void defineColumn_TimeSlice_Body_Heartbeat_RR_Index() {

      final TableColumnDefinition colDef = TableColumnFactory.BODY_PULSE_RR_INDEX.createColumn(_timeSlice_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            if (_seriePulse_RR_Index != null) {

               final TimeSlice timeSlice = (TimeSlice) cell.getElement();
               final int value = _seriePulse_RR_Index[timeSlice.serieIndex];

               cell.setText(Integer.toString(value));

            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: R-R values
    */
   private void defineColumn_TimeSlice_Body_Heartbeat_RR_Intervals() {

      final TableColumnDefinition colDef = TableColumnFactory.BODY_PULSE_RR_INTERVALS.createColumn(_timeSlice_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            if (_seriePulse_RR_Intervals != null) {

               final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;

               cell.setText(_seriePulse_RR_Intervals[serieIndex]);

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
      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;
            final int logIndex = ((TimeSlice) cell.getElement()).uniqueCreateIndex;

            // mark reference tour with a different background color
            boolean isColorSet = false;

            if (_refTourRange != null) {
               for (final int[] oneRange : _refTourRange) {
                  if ((serieIndex >= oneRange[0]) && (serieIndex <= oneRange[1])) {

                     cell.setForeground(_foregroundColor_1stColumn_RefTour);
                     cell.setBackground(_backgroundColor_1stColumn_RefTour);

                     isColorSet = true;

                     break;
                  }
               }
            }

            if (isColorSet == false) {

               cell.setForeground(_foregroundColor_1stColumn_NoRefTour);
               cell.setBackground(_backgroundColor_1stColumn_NoRefTour);
            }

            // the UI shows the time slice number starting with 1 and not with 0
            cell.setText(Integer.toString(logIndex + 1));
         }
      });
   }

   /**
    * Column: Elevation
    */
   private void defineColumn_TimeSlice_Elevation_Elevation() {

      ColumnDefinition colDef;

      _timeSlice_ColDef_Altitude = colDef = TableColumnFactory.ALTITUDE_ALTITUDE.createColumn(_timeSlice_ColumnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            if (_serieAltitude != null) {

               final TimeSlice timeSlice = (TimeSlice) cell.getElement();
               cell.setText(_nf1.format(_serieAltitude[timeSlice.serieIndex] / _unitValueElevation));

            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });

   }

   /**
    * Column: Gradient
    */
   private void defineColumn_TimeSlice_Elevation_Gradient() {

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

                  // first time slice can contain a distance, occurred in .fit files
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
      colDef.setColumnSelectionListener(_columnSortListener);

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

               colDef.printDetailValue(cell, speed);

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

      final ColumnDefinition colDef = TableColumnFactory.POWER_TIME_SLICE.createColumn(_timeSlice_ColumnManager, _pc);
      colDef.setColumnSelectionListener(_columnSortListener);

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

      _timeSlice_ColDef_Cadence = colDef = TableColumnFactory.POWERTRAIN_CADENCE_TIME_SLICE.createColumn(_timeSlice_ColumnManager, _pc);
      colDef.setColumnSelectionListener(_columnSortListener);

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

      final ColumnDefinition colDef = TableColumnFactory.POWERTRAIN_GEAR_RATIO_TIME_SLICE.createColumn(_timeSlice_ColumnManager, _pc);

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
    * Column: Break time
    */
   private void defineColumn_TimeSlice_Time_BreakTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_IS_BREAK_TIME.createColumn(_timeSlice_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            if (_serieBreakTime != null) {

               final TimeSlice timeSlice = (TimeSlice) cell.getElement();

               // get break time state from the next slice, a break is between the current and the previous slice
               final int numSlices = _serieBreakTime.length;
               int serieIndex = timeSlice.serieIndex;
               serieIndex = serieIndex < numSlices - 2
                     ? serieIndex + 1
                     : serieIndex;

               cell.setText(_serieBreakTime[serieIndex]
                     ? UI.SYMBOL_BOX
                     : UI.EMPTY_STRING);
            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Paused time
    */
   private void defineColumn_TimeSlice_Time_PausedTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_IS_PAUSED_TIME.createColumn(_timeSlice_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            if (_seriePausedTime != null) {

               final TimeSlice timeSlice = (TimeSlice) cell.getElement();
               cell.setText(_seriePausedTime[timeSlice.serieIndex]
                     ? UI.SYMBOL_BOX
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
               final int numSlices = _serieTime.length;

               if (serieIndex == numSlices - 1) {

                  // the last time slice is displayed-> there is no difference to the next slice

                  cell.setText(Integer.toString(0));

               } else {

                  cell.setText(Integer.toString(_serieTime[serieIndex + 1] - _serieTime[serieIndex]));
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
    * Column: Time of day in hh:mm:ss or hh:mm:ss am/pm
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
               final int timeSliceSeconds = _serieTime[serieIndex];

               cell.setText(_tourStartTime.plusSeconds(timeSliceSeconds).format(TimeTools.Formatter_Time_M));
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

               if (tourMarker.getType() == ChartLabelMarker.MARKER_TYPE_DEVICE) {
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
      _timeSlice_ColDef_Temperature = colDef = TableColumnFactory.WEATHER_TEMPERATURE_TIME_SLICE.createColumn(_timeSlice_ColumnManager, _pc);
      colDef.setColumnSelectionListener(_columnSortListener);

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

      fireTourIsReverted();

      // a manually created tour can not be reloaded, find a tour in the workbench
      if (_tourData == null) {
         displaySelectedTour();
      }
   }

   private void displayCloudIcon() {

      final int selectionIndex = _comboWeather_Clouds.getSelectionIndex();

      final String cloudKey = IWeather.cloudIcon[selectionIndex];
      final Image cloudIcon = UI.IMAGE_REGISTRY.get(cloudKey);

      _lblCloudIcon.setImage(cloudIcon);
   }

   /**
    * try to get tourdata from the last selection or from a tour provider
    */
   private void displaySelectedTour() {

      // show tour from last selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_tourData == null) {

         Display.getCurrent().asyncExec(() -> {

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
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      TourManager.getInstance().removeTourSaveListener(_tourSaveListener);

      if (_tk != null) {
         _tk.dispose();
      }

      for (final Action_SetSwimStyle swimStyleAction : _allSwimStyleActions) {
         swimStyleAction.dispose();
      }

      _firstColumnControls.clear();
      _secondColumnControls.clear();
      _firstColumnContainerControls.clear();

      /*
       * Tour MUST be set clean otherwise a Ctrl+W would "close" the tour editor but closing the
       * app is asking to save the tour!
       */
      setTourClean();

      super.dispose();
   }

   @Override
   public void doRestore() {

      if (_isCellEditorActive) {

         /**
          * Ensure that the active cell editor is not in edit mode, because the command shortcut can
          * activate this restore action.
          */

         if (_currentComboBox_CellEditor != null) {
            _currentComboBox_CellEditor.focusLost();
         }
         if (_currentTextEditor_CellEditor != null) {
            _currentTextEditor_CellEditor.focusLost();
         }
      }

      if (confirmUndoChanges()) {
         discardModifications();
      }
   }

   @Override
   public void doSave() {
      doSave(null);
   }

   @Override
   public void doSave(final IProgressMonitor monitor) {

      if (_isCellEditorActive) {

         /**
          * Ensure that the content of the active cell editor is updated in the model and also
          * saved because the command shortcut can activate this save action.
          */

         if (_currentComboBox_CellEditor != null) {
            _currentComboBox_CellEditor.focusLost();
         }
         if (_currentTextEditor_CellEditor != null) {
            _currentTextEditor_CellEditor.focusLost();
         }
      }

      saveTourIntoDB();
   }

   @Override
   public void doSaveAs() {}

   /**
    * Edit time slices values :
    * Altitude, Heartbeat, Temperature, Cadence
    */
   void editTimeSlicesValues() {

      if (isRowSelectionMode() == false) {
         return;
      }

      // get selected time slices
      final StructuredSelection selection = (StructuredSelection) _timeSlice_Viewer.getSelection();
      if (selection.isEmpty()) {
         return;
      }
      final Object[] selectedTimeSlices = selection.toArray();

      final DialogEditTimeSlicesValues dialogEditTimeSlicesValues = new DialogEditTimeSlicesValues(Display.getCurrent().getActiveShell(), _tourData);

      if (dialogEditTimeSlicesValues.open() == Window.OK) {

         BusyIndicator.showWhile(Display.getCurrent(), () -> {

            final float newAltitudeValue = dialogEditTimeSlicesValues.getNewAltitudeValue();
            final float newCadenceValue = dialogEditTimeSlicesValues.getNewCadenceValue();
            final float newPulseValue = dialogEditTimeSlicesValues.getNewPulseValue();
            final float newTemperatureValue = dialogEditTimeSlicesValues.getNewTemperatureValue();

            final boolean isAltitudeValueOffset = dialogEditTimeSlicesValues.getIsAltitudeValueOffset();
            final boolean isCadenceValueOffset = dialogEditTimeSlicesValues.getIsCadenceValueOffset();
            final boolean isPulseValueOffset = dialogEditTimeSlicesValues.getIsPulseValueOffset();
            final boolean isTemperatureValueOffset = dialogEditTimeSlicesValues.getIsTemperatureValueOffset();

            final int[] selectedRows = new int[selectedTimeSlices.length];
            boolean tourDataModified = false;
            for (int index = 0; index < selectedTimeSlices.length; ++index) {

               final int currentTimeSliceIndex = ((TimeSlice) selectedTimeSlices[index]).serieIndex;

               selectedRows[index] = currentTimeSliceIndex;

               if (newAltitudeValue != Float.MIN_VALUE && _serieAltitude != null) {

                  if (isAltitudeValueOffset) {
                     _serieAltitude[currentTimeSliceIndex] += newAltitudeValue;
                  } else {
                     _serieAltitude[currentTimeSliceIndex] = newAltitudeValue;
                  }

                  tourDataModified = true;
               }

               if (newPulseValue != Integer.MIN_VALUE && _seriePulse != null) {

                  if (isPulseValueOffset) {
                     _seriePulse[currentTimeSliceIndex] += newPulseValue;
                  } else {
                     _seriePulse[currentTimeSliceIndex] = newPulseValue;
                  }

                  tourDataModified = true;
               }

               if (newCadenceValue != Integer.MIN_VALUE && _serieCadence != null) {

                  if (isCadenceValueOffset) {
                     _serieCadence[currentTimeSliceIndex] += newCadenceValue;
                  } else {
                     _serieCadence[currentTimeSliceIndex] = newCadenceValue;
                  }

                  _tourData.setCadenceSerie(_serieCadence);
                  tourDataModified = true;
               }

               if (newTemperatureValue != Float.MIN_VALUE && _serieTemperature != null) {
                  if (isTemperatureValueOffset) {

                     //If we are currently in imperial system, we can't just convert the offset as it will lead to errors.
                     // For example : If the user has asked for an offset of 1°F, then it could offset the metric temperature to -17.22222 °C!!!

                     if (UI.UNIT_IS_TEMPERATURE_FAHRENHEIT) {

                        final float currentTemperatureMetric = _serieTemperature[currentTimeSliceIndex];
                        float currentTemperatureFahrenheit = (currentTemperatureMetric * UI.UNIT_FAHRENHEIT_MULTI) + UI.UNIT_FAHRENHEIT_ADD;

                        currentTemperatureFahrenheit += newTemperatureValue;
                        final float newTemperatureMetric = UI.convertTemperatureToMetric(currentTemperatureFahrenheit);

                        _serieTemperature[currentTimeSliceIndex] = newTemperatureMetric;

                     } else {
                        _serieTemperature[currentTimeSliceIndex] += newTemperatureValue;
                     }

                  } else {
                     _serieTemperature[currentTimeSliceIndex] = newTemperatureValue;
                  }
                  tourDataModified = true;
               }
            }

            if (!tourDataModified) {
               return;
            }

            // force recompute values, e.g. gradient
            _tourData.clearComputedSeries();

            // recompute min/max values
            getDataSeriesFromTourData();

            // update UI
            updateUI_Tab_1_Tour();
            updateUI_ReferenceTourRanges();

            _timeSlice_Viewer.getControl().setRedraw(false);
            {
               _timeSlice_Viewer.refresh(true);
            }
            _timeSlice_Viewer.getControl().setRedraw(true);

            setTourDirty();

            // notify other viewers
            fireTourIsModified();

            /*
             * Select back the previously selected indices
             */
            final int[] mappedTimeSlicesIndices = mapTimeSlicesIndicesWithRows(selectedRows);
            final Table table = (Table) _timeSlice_Viewer.getControl();

            table.setSelection(mappedTimeSlicesIndices);
            table.showSelection();

         });

      }
   }

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

      _actionOpenAdjustAltitudeDialog.setEnabled(isCellEditorInactive && canUseTool);
      _actionOpenMarkerDialog.setEnabled(isCellEditorInactive && canUseTool);

      _actionToggleRowSelectMode.setEnabled(
            isCellEditorInactive
                  && (isTimeSlice_ViewerTab || isSwimSlice_ViewerTab)
                  && isTourValid
                  && (_isManualTour == false));
      _actionToggleReadEditMode.setEnabled(isCellEditorInactive && isTourInDb);

      _actionSetStartDistanceTo_0.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isDistanceLargerThan0);
      _actionDeleteDistanceValues.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isDistanceAvailable);
      _actionComputeDistanceValues.setEnabled(isCellEditorInactive && isNotManualTour && canEdit && isGeoAvailable);

      _actionEditTimeSlicesValues.setEnabled(isCellEditorInactive && canEdit);
   }

   private void enableActions_SwimSlices() {

      final StructuredSelection sliceSelection = (StructuredSelection) _swimSlice_Viewer.getSelection();

      final int numSelectedSlices = sliceSelection.size();

      final boolean isSliceSelected = numSelectedSlices > 0;
      final boolean isTourInDb = isTourInDb();

      final boolean canEditSwimSlices = _isEditMode && isTourInDb && isSliceSelected;

      for (final Action_SetSwimStyle swimStyleAction : _allSwimStyleActions) {
         swimStyleAction.setEnabled(canEditSwimSlices);
      }

      _action_RemoveSwimStyle.setEnabled(canEditSwimSlices);
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
      for (final Object name : sliceSelection) {
         final TimeSlice timeSlice = (TimeSlice) name;
         if (_markerMap.containsKey(timeSlice.serieIndex)) {
            selectedMarker = _markerMap.get(timeSlice.serieIndex);
            break;
         }
      }

      final boolean canDeleteTimeSliced = _isEditMode && isTourInDb && isSliceSelected && isNoSwimData;

      _actionCreateTourMarker.setEnabled(_isEditMode && isTourInDb && isOneSliceSelected && canCreateMarker);
      _actionOpenMarkerDialog.setEnabled(_isEditMode && isTourInDb && isOneSliceSelected && selectedMarker != null);

      // select marker
      _actionOpenMarkerDialog.setTourMarker(selectedMarker);

// SET_FORMATTING_OFF

      _actionDeleteTimeSlices_AdjustTourStartTime  .setEnabled(canDeleteTimeSliced);
      _actionDeleteTimeSlices_KeepTime             .setEnabled(canDeleteTimeSliced);
      _actionDeleteTimeSlices_KeepTimeAndDistance  .setEnabled(canDeleteTimeSliced);
      _actionDeleteTimeSlices_RemoveTime           .setEnabled(canDeleteTimeSliced);

      _actionExportTour          .setEnabled(true);
      _actionCsvTimeSliceExport  .setEnabled(isSliceSelected);

      _actionSplitTour           .setEnabled(isOneSliceSelected);
      _actionExtractTour         .setEnabled(numberOfSelectedSlices >= 2);

// SET_FORMATTING_ON

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
    * flickers the UI, therefore it is delayed.
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

      final Table timeSliceTable = _timeSlice_Viewer.getTable();
      if (timeSliceTable.isDisposed()) {
         return;
      }

      final boolean canEdit = _isEditMode && isTourInDb();
      final boolean isManualAndEdit = _isManualTour && canEdit;
      final boolean isDeviceTour = _isManualTour == false;

      final float[] serieDistance = _tourData == null ? null : _tourData.distanceSerie;
      final boolean isDistanceSerie = serieDistance != null && serieDistance.length > 0;
      final boolean isGeoAvailable = _tourData != null
            && _tourData.latitudeSerie != null
            && _tourData.latitudeSerie.length > 0;

      final boolean isWeatherRetrievalActivated = TourManager.isWeatherRetrievalActivated();
      final boolean isWindDirectionAvailable = _comboWeather_Wind_DirectionText.getSelectionIndex() > 0;

      _comboTitle.setEnabled(canEdit);
      _txtDescription.setEnabled(canEdit);
      _comboLocation_Start.setEnabled(canEdit);
      _comboLocation_End.setEnabled(canEdit);

      //Weather
      _linkWeather.setEnabled(canEdit && isWeatherRetrievalActivated);
      _tableComboWeather_AirQuality.setEnabled(canEdit);
      _comboWeather_Clouds.setEnabled(canEdit);
      _comboWeather_Wind_DirectionText.setEnabled(canEdit);
      _comboWeather_WindSpeedText.setEnabled(canEdit);
      _spinWeather_Humidity.setEnabled(canEdit);
      _spinWeather_PrecipitationValue.setEnabled(canEdit);
      _spinWeather_PressureValue.setEnabled(canEdit);
      _spinWeather_SnowfallValue.setEnabled(canEdit);
      _spinWeather_Wind_DirectionValue.setEnabled(canEdit && isWindDirectionAvailable);
      _spinWeather_Wind_SpeedValue.setEnabled(canEdit);
      _txtWeather.setEnabled(canEdit);
      _spinWeather_Temperature_Average.setEnabled(canEdit);
      _spinWeather_Temperature_Max.setEnabled(canEdit);
      _spinWeather_Temperature_Min.setEnabled(canEdit);
      _spinWeather_Temperature_WindChill.setEnabled(canEdit);

      _comboCadence.getCombo().setEnabled(canEdit);

      _dtTourDate.setEnabled(canEdit);
      _dtStartTime.setEnabled(canEdit);
      _comboTimeZone.setEnabled(canEdit);
      _linkDefaultTimeZone.setEnabled(canEdit);
      _linkRemoveTimeZone.setEnabled(canEdit);
      _linkGeoTimeZone.setEnabled(canEdit && isGeoAvailable);

      _deviceTime_Elapsed.setEditMode(isManualAndEdit);
      _deviceTime_Recorded.setEditMode(isManualAndEdit);
      _deviceTime_Paused.setEditMode(isManualAndEdit);
      _computedTime_Moving.setEditMode(isManualAndEdit);
      _computedTime_Break.setEditMode(isManualAndEdit);

      // distance can be edited when no distance time slices are available
      _txtDistance.setEnabled(canEdit && isDistanceSerie == false);
      _txtAltitudeUp.setEnabled(isManualAndEdit);
      _txtAltitudeDown.setEnabled(isManualAndEdit);

      // Personal
      _spinPerson_BodyWeight.setEnabled(canEdit);
      _spinPerson_BodyFat.setEnabled(canEdit);
      _spinPerson_FTP.setEnabled(canEdit);
      _spinPerson_RestPulse.setEnabled(canEdit);
      _spinPerson_Calories.setEnabled(canEdit);

      _linkTag.setEnabled(canEdit);
      _linkTourType.setEnabled(canEdit);

      timeSliceTable.setEnabled(isDeviceTour);
   }

   private void fillAirQualityCombo() {

      for (int index = 0; index < IWeather.airQualityTexts.length; index++) {

         final TableItem tableItem = new TableItem(_tableComboWeather_AirQuality.getTable(), SWT.READ_ONLY);

         // set the column text
         tableItem.setText(IWeather.airQualityTexts[index]);

         Color backgroundColor = null;
         Color foregroundColor = null;

         switch (index) {

         case 1: // Good - green

            backgroundColor = new Color(0, 128, 0);
            foregroundColor = UI.IS_DARK_THEME ? UI.SYS_COLOR_WHITE : new Color(203, 255, 203);

            break;

         case 2: // Fair - yellow

            backgroundColor = new Color(255, 255, 0);
            foregroundColor = UI.IS_DARK_THEME ? new Color(46, 46, 0) : new Color(46, 46, 0);

            break;

         case 3: // Moderate - dark yellow

            backgroundColor = new Color(128, 128, 0);
            foregroundColor = UI.IS_DARK_THEME ? UI.SYS_COLOR_WHITE : new Color(255, 255, 165);
            break;

         case 4: // Poor - dark red

            backgroundColor = new Color(128, 0, 0);
            foregroundColor = UI.IS_DARK_THEME ? UI.SYS_COLOR_WHITE : new Color(255, 170, 170);
            break;

         case 5: // Very poor - dark grey

            backgroundColor = new Color(128, 128, 128);
            foregroundColor = UI.IS_DARK_THEME ? UI.SYS_COLOR_WHITE : new Color(255, 255, 255);
            break;

         default:

            backgroundColor = ThemeUtil.getDefaultBackgroundColor_Combo();
            foregroundColor = ThemeUtil.getDefaultForegroundColor_Combo();

            break;
         }

         tableItem.setBackground(backgroundColor);
         tableItem.setForeground(foregroundColor);
      }
   }

   private void fillContextMenu_SwimSlice(final IMenuManager menuMgr) {

      menuMgr.add(_action_SetSwimStyle_Header);

      for (final Action_SetSwimStyle swimStyleAction : _allSwimStyleActions) {
         menuMgr.add(swimStyleAction);
      }

      menuMgr.add(_action_RemoveSwimStyle);

      enableActions_SwimSlices();
   }

   private void fillContextMenu_TimeSlice(final IMenuManager menuMgr) {

      menuMgr.add(_actionEditTimeSlicesValues);
      menuMgr.add(new Separator());

      menuMgr.add(_actionCreateTourMarker);
      menuMgr.add(_actionOpenMarkerDialog);

      menuMgr.add(new Separator());
      menuMgr.add(_actionDeleteTimeSlices_RemoveTime);
      menuMgr.add(_actionDeleteTimeSlices_KeepTime);
      menuMgr.add(_actionDeleteTimeSlices_KeepTimeAndDistance);
      menuMgr.add(_actionDeleteTimeSlices_AdjustTourStartTime);

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

      tbm.add(new Separator());
      tbm.add(_actionOpenMarkerDialog);
      tbm.add(_actionOpenAdjustAltitudeDialog);

      tbm.add(new Separator());
      tbm.add(_actionToggleReadEditMode);
      tbm.add(_actionToggleRowSelectMode);

      tbm.add(new Separator());
      tbm.add(_actionViewSettings);

      tbm.update(true);

      /*
       * fill toolbar view menu
       */
//      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

   }

   /**
    * Select the chart/map slider(s) according to the selected tour/swim slices
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

      final int numSlices = _tourData.timeSerie.length;

      final int numSelectedSlices = selectedSlices.length;
      final Object firstSelectedSlice = selectedSlices[0];

      int serieIndex0 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
      int serieIndex1 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
      int serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

      if (numSelectedSlices == 1) {

         // One slice is selected

         if (firstSelectedSlice instanceof SwimSlice) {

            /*
             * position slider at the beginning of the slice so that each slice borders has an
             * slider
             */
            final int swimSerieIndex1 = ((SwimSlice) firstSelectedSlice).serieIndex;
            final int timeSerieIndex1 = getSerieIndexFromSwimTime(swimSerieIndex1);

            if (timeSerieIndex1 > 1) {

               final int swimSerieIndex0 = swimSerieIndex1 - 1;
               final int timeSerieIndex0 = getSerieIndexFromSwimTime(swimSerieIndex0);

               serieIndex0 = timeSerieIndex0;
            }

            serieIndex1 = timeSerieIndex1 == numSlices - 1

                  // keep slider at the end of the chart
                  ? numSlices - 1

                  // adjust slider to the same stroke/swolf value as the left slider
                  : timeSerieIndex1 - 1;

            serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

         } else if (firstSelectedSlice instanceof TimeSlice) {

            final int sliceSerieIndex = ((TimeSlice) firstSelectedSlice).serieIndex;

            serieIndex0 = sliceSerieIndex;
            serieIndex1 = sliceSerieIndex < numSlices - 1
                  ? sliceSerieIndex + 1
                  : sliceSerieIndex;

            serieIndex2 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
         }

      } else if (numSelectedSlices > 1) {

         // Two or more slices are selected, set the 2 sliders to the first and last selected slices

         if (firstSelectedSlice instanceof SwimSlice) {

            final int swimSerieIndex1 = ((SwimSlice) firstSelectedSlice).serieIndex;
            final int swimSerieIndex2 = ((SwimSlice) selectedSlices[numSelectedSlices - 1]).serieIndex;

            final int timeSerieIndex2 = getSerieIndexFromSwimTime(swimSerieIndex2);

            /*
             * Position slider at the beginning of the first slice
             */
            serieIndex1 = 0;

            if (swimSerieIndex1 > 0) {
               final int timeSerieIndex1 = getSerieIndexFromSwimTime(swimSerieIndex1 - 1);
               serieIndex1 = timeSerieIndex1;
            }

            serieIndex2 = timeSerieIndex2 == numSlices - 1

                  // keep slider at the end of the chart
                  ? numSlices - 1

                  // adjust slider to the same stroke/swolf value as the left slider
                  : timeSerieIndex2 - 1;

         } else if (firstSelectedSlice instanceof TimeSlice) {

            final int serieIndexFirst = ((TimeSlice) firstSelectedSlice).serieIndex;

            /*
             * Position slider at the beginning of the first slice
             */
            serieIndex1 = serieIndexFirst;
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

   /**
    * Fire notification for changed tour data
    */
   private void fireTourIsModified() {

      final ArrayList<TourData> modifiedTour = new ArrayList<>();
      modifiedTour.add(_tourData);

      final TourEvent tourEvent = new TourEvent(modifiedTour);
      tourEvent.isTourModified = true;

      TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent, TourDataEditorView.this);
   }

   /**
    * Fire notification for the reverted tour data
    */
   private void fireTourIsReverted() {

      final TourEvent tourEvent = new TourEvent(_tourData);
      tourEvent.isReverted = true;

      TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent, TourDataEditorView.this);
   }

   private void getDataSeriesFromTourData() {

      _serieTime = _tourData.timeSerie;

      _serieDistance = _tourData.distanceSerie;
      _serieAltitude = _tourData.altitudeSerie;

      _serieCadence = _tourData.getCadenceSerie();
      _serieGears = _tourData.getGears();
      _seriePulse = _tourData.pulseSerie;

      _seriePulse_RR_Bpm = _tourData.getPulse_AvgBpmFromRRIntervals();
      _seriePulse_RR_Intervals = _tourData.getPulse_RRIntervals();

      // time serie which is containing the index for the first slice in the RR serie
      _seriePulse_RR_Index = _tourData.pulseTime_TimeIndex;

      _serieLatitude = _tourData.latitudeSerie;
      _serieLongitude = _tourData.longitudeSerie;

      _serieBreakTime = _tourData.getBreakTimeSerie();
      _seriePausedTime = _tourData.getPausedTimeSerie();

      _serieGradient = _tourData.getGradientSerie();
      _serieSpeed = _tourData.getSpeedSerie();
      _seriePace = _tourData.getPaceSerieSeconds();
      _seriePower = _tourData.getPowerSerie();

      _serieTemperature = _tourData.temperatureSerie;

      _swimSerie_StrokeRate = _tourData.swim_Cadence;
      _swimSerie_StrokesPerlength = _tourData.swim_Strokes;
      _swimSerie_StrokeStyle = _tourData.swim_StrokeStyle;
      _swimSerie_Time = _tourData.swim_Time;

      _swimSlice_StrokeRateEditingSupport.setDataSerie(_swimSerie_StrokeRate);
      _swimSlice_StrokesEditingSupport.setDataSerie(_swimSerie_StrokesPerlength);
      _swimSlice_StrokeStyleEditingSupport.setDataSerie(_swimSerie_StrokeStyle);

      _timeSlice_AltitudeEditingSupport.setDataSerie(_serieAltitude);
      _timeSlice_TemperatureEditingSupport.setDataSerie(_serieTemperature);
      _timeSlice_PulseEditingSupport.setDataSerie(_seriePulse);
      _timeSlice_CadenceEditingSupport.setDataSerie(_serieCadence);
      _timeSlice_LatitudeEditingSupport.setDataSerie(_serieLatitude);
      _timeSlice_LongitudeEditingSupport.setDataSerie(_serieLongitude);

      _tourStartTime = _tourData.getTourStartTime();

      if (_isManualTour == false) {

         // tour is imported

         long tourDeviceTimeElapsed;
         if ((_serieTime == null) || (_serieTime.length == 0)) {
            tourDeviceTimeElapsed = 0;
         } else {
            tourDeviceTimeElapsed = _serieTime[_serieTime.length - 1];
         }
         _tourData.setTourDeviceTime_Elapsed(tourDeviceTimeElapsed);

         final long[] pausedTime_Start = _tourData.getPausedTime_Start();
         if (pausedTime_Start != null && pausedTime_Start.length > 0) {

            final List<Long> listPausedTime_Start = Arrays.stream(pausedTime_Start)
                  .boxed()
                  .collect(Collectors.toList());
            final List<Long> listPausedTime_End = Arrays.stream(_tourData.getPausedTime_End())
                  .boxed()
                  .collect(Collectors.toList());

            List<Long> listPausedTime_Data = null;
            final long[] pausedTime_Data = _tourData.getPausedTime_Data();

            if (pausedTime_Data != null) {

               listPausedTime_Data = Arrays.stream(_tourData.getPausedTime_Data())
                     .boxed()
                     .collect(Collectors.toList());
            }

            _tourData.finalizeTour_TimerPauses(listPausedTime_Start,
                  listPausedTime_End,
                  listPausedTime_Data);
         } else {
            _tourData.setTourDeviceTime_Paused(0);
         }

         _tourData.setTourDeviceTime_Recorded(tourDeviceTimeElapsed - _tourData.getTourDeviceTime_Paused());

         _tourData.computeTourMovingTime();

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

   private TimeSlice[] getRemainingSliceItems(final Object[] dataViewerItems,
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

         final Map<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
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

      _parent = parent;

      _pc = new PixelConverter(parent);

      _hintDefaultSpinnerWidth = IS_LINUX //
            ? SWT.DEFAULT
            : _pc.convertWidthInCharsToPixels(IS_OSX ? 14 : 7);

      _hintValueFieldWidth = _pc.convertWidthInCharsToPixels(10);

      _columnSortListener = widgetSelectedAdapter(this::onSelect_SortColumn);

      parent.addDisposeListener(e -> onDispose());
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

   /**
    * For each time slice index, we retrieve the index in the _timeSlice_Viewer viewer table.
    *
    * @param selectedRows
    *           An array containing the index of several time slices.
    */
   private int[] mapTimeSlicesIndicesWithRows(final int[] selectedRows) {

      final int[] mappedTimeSlicesIndices = new int[selectedRows.length];

      for (int index = 0; index < selectedRows.length; ++index) {

         //The time slice index of a given row
         final int currentTimeSliceIndex = selectedRows[index];

         final TableItem[] tableItems = ((Table) _timeSlice_Viewer.getControl()).getItems();
         for (int tableIndex = 0; index < tableItems.length; ++tableIndex) {

            final TimeSlice timeSlice = (TimeSlice) tableItems[tableIndex].getData();
            if (timeSlice.serieIndex == currentTimeSliceIndex) {
               mappedTimeSlicesIndices[index] = tableIndex;
               break;
            }
         }
      }

      return mappedTimeSlicesIndices;
   }

   private void onDispose() {

      TagManager.disposeTagUIContent();
   }

   private void onExpandSection() {

      onResize_Tab1();

//    form.reflow(false);
   }

   /**
    * Update other views when tour title is modified
    * <p>
    * Deactivated because it slows down the text input in the title field (when this is also applied
    * to the title field modifyListener).
    * <p>
    * The bug https://sourceforge.net/p/mytourbook/bugs/115/ needed some UI behavior changes which
    * was introduced when Ctrl+S was introduced to save tour in tour editor.
    * <p>
    * Keep code to may be used later.
    */
   @SuppressWarnings("unused")
   private void onModifyContent() {

      if (_tourData == null) {
         return;
      }

      // update modified data
      updateModel_FromUI();

      enableActions();

      fireTourIsModified();
   }

   private void onPart_Closed() {

      if (_tourData == null) {
         return;
      }

      if (_isTourDirty == false) {
         return;
      }

      /*
       * Tour is dirty and part is closed -> discard invalid tour to fix
       * https://sourceforge.net/p/mytourbook/bugs/128/
       */
      discardModifications();
   }

   private void onResize_Tab1() {

      _tab1Container.setRedraw(false);
      {
         _tab1Container.setMinSize(_tourContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         _tab1Container.layout(true, true);

         updateUI_BackgroundColor();
      }
      _tab1Container.setRedraw(true);
   }

   private void onResize_TagContent() {

      if (_containerTags_Content == null || _containerTags_Content.isDisposed()) {
         return;
      }

      final Point contentSize = _containerTags_Content.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      _containerTags_Scrolled.setMinSize(contentSize);
   }

   private void onSelect_AirQuality() {

      final int airQuality_SelectionIndex = _tableComboWeather_AirQuality.getSelectionIndex();
      String airQualityValue = IWeather.airQualityTexts[airQuality_SelectionIndex];
      if (airQualityValue.equals(IWeather.airQualityIsNotDefined)) {
         // replace invalid value
         airQualityValue = UI.EMPTY_STRING;
      }
      _tourData.setWeather_AirQuality(airQualityValue);
   }

   private void onSelect_Slice(final SelectionChangedEvent selectionChangedEvent) {

      final StructuredSelection selection = (StructuredSelection) selectionChangedEvent.getSelection();

      if (selection != null && selection.isEmpty() == false) {
         fireSliderPosition(selection);
      }
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _timeSlice_Viewer.getTable().setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _timeSlice_Viewer.getSelection();

         // toggle sorting
         _timeSlice_Comparator.setSortColumn(e.widget);
         _timeSlice_Viewer.refresh();

         // reselect selection
         _timeSlice_Viewer.setSelection(selectionBackup, true);
         _timeSlice_Viewer.getTable().showSelection();
      }
      _timeSlice_Viewer.getTable().setRedraw(true);
   }

   private void onSelect_Tab() {

      final CTabItem selectedTab = _tabFolder.getSelection();

      if (selectedTab == _tab_20_TimeSlices) {

         if (_timeSlice_ViewerTourId == -1L) {

            // load viewer when this was not yet done
            _timeSlice_ViewerTourId = _tourData.getTourId();

            _timeSlice_TourViewer.reloadViewer();
            updateStatusLine();

            // run async because reloadViewer is also running async
            Display.getCurrent().asyncExec(() -> {

               if (_timeSlice_Viewer.getTable().isDisposed()) {
                  return;
               }

               _timeSlice_Viewer.getTable().setFocus();
            });
         }

      } else if (selectedTab == _tab_30_SwimSlices) {

         if (_swimSlice_ViewerTourId == -1L) {

            // load viewer when this was not yet done
            _swimSlice_ViewerTourId = _tourData.getTourId();

            _swimSlice_TourViewer.reloadViewer();
//          updateStatusLine();

            // run async because relaodViewer is also running async
            Display.getCurrent().asyncExec(() -> {

               if (_swimSlice_Viewer.getTable().isDisposed()) {
                  return;
               }

               _swimSlice_Viewer.getTable().setFocus();
            });
         }

      }

      enableActions();

   }

   private void onSelect_Weather_Text() {

      final List<TourData> modifiedTours = TourManager.retrieveWeatherData(List.of(_tourData));

      if (modifiedTours.size() == 0) {

         // tour is not modified which is caused when an error occurs -> show error log

         TourLogManager.showLogView();

      } else {

         // tour is modified

         setTourDirty();

         updateUI_FromModel(modifiedTours.get(0), false, true);
      }
   }

   private void onSelect_WindSpeedText() {

      _isWindSpeedManuallyModified = true;

      final int selectedIndex = _comboWeather_WindSpeedText.getSelectionIndex();
      final int speed = _unitValueWindSpeed[selectedIndex];

      final boolean isBackup = _isSetField;
      _isSetField = true;
      {
         _spinWeather_Wind_SpeedValue.setSelection(speed);
      }
      _isSetField = isBackup;
   }

   private void onSelect_WindSpeedValue() {

      _isWindSpeedManuallyModified = true;

      final int windSpeed = _spinWeather_Wind_SpeedValue.getSelection();

      final boolean isBackup = _isSetField;
      _isSetField = true;
      {
         _comboWeather_WindSpeedText.select(getWindSpeedTextIndex(windSpeed));
      }
      _isSetField = isBackup;
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

      if (onSelectionChanged_IsTourInSelection(selection)) {

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
                * Show info only when it is not yet displayed, this is an optimization because
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

            displayTour(((TVICompareResultComparedTour) firstElement).getTourId());
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
   private boolean onSelectionChanged_IsTourInSelection(final ISelection selection) {

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

         if (currentTourId == _selectionTourId) {
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

         if (currentTourId == _selectionTourId) {
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

         } else {

            final Object customData = xSliderPosition.getCustomData();
            if (customData instanceof SelectedTourSegmenterSegments) {

               final SelectedTourSegmenterSegments selectedSegments = (SelectedTourSegmenterSegments) customData;
               final TourData tourData = selectedSegments.tourData;

               _selectionTourId = tourData.getTourId();
               if (currentTourId == _selectionTourId) {

                  isCurrentTourSelected = true;
                  selectedTourData = tourData;

                  // select time slices
                  selectTimeSlice(xSliderPosition);
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

            final long comparedTourTourId = ((TVICompareResultComparedTour) firstElement).getTourId();

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

   private void onSelectionChanged_MapSelection(final SelectionMapSelection mapSelection) {

      selectTimeSlice_InViewer(mapSelection.getValueIndex1(), mapSelection.getValueIndex2());
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

   private void onSelectionChanged_TourPause(final SelectionTourPause pauseSelection) {

      final int leftSliderValueIndex = pauseSelection.getSerieIndex();

      // the serie index is the end of the pause but the start of the pause should be selected
      final int startPauseIndex = Math.max(0, leftSliderValueIndex - 1);

      selectTimeSlice_InViewer(startPauseIndex, startPauseIndex);
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

      _latLonDigits = Util.getStateInt(_state, STATE_LAT_LON_DIGITS, STATE_LAT_LON_DIGITS_DEFAULT);
      setup_LatLonDigits();

      _descriptionNumLines = Util.getStateInt(_state, STATE_DESCRIPTION_NUMBER_OF_LINES, STATE_DESCRIPTION_NUMBER_OF_LINES_DEFAULT);
      weatherDescriptionNumLines = Util.getStateInt(_state,
            STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES,
            STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES_DEFAULT);
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

//    _advMenuAddTag.setAutoOpen(
//    _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN),
//    _prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY));

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
            new String[] {
                  Messages.Tour_Editor_Button_SaveTour,
                  Messages.Tour_Editor_Button_DiscardModifications,
                  IDialogConstants.CANCEL_LABEL },
            0)
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
    * Saves the tour when it is dirty, valid and confirmation is done
    */
   private boolean saveTourIntoDB() {

      final boolean isRecomputeElevation = Util.getStateBoolean(_state,
            STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN,
            STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN_DEFAULT);

      final boolean isElevationFromDevice = Util.getStateBoolean(_state,
            STATE_IS_ELEVATION_FROM_DEVICE,
            STATE_IS_ELEVATION_FROM_DEVICE_DEFAULT);

      _isSavingInProgress = true;
      {
         updateModel_FromUI();

         if (isRecomputeElevation) {
            _tourData.computeAltitudeUpDown(isElevationFromDevice);
         }

         _tourData.computeTourMovingTime();
         _tourData.computeComputedValues();

         /*
          * saveTour() will check the tour editor dirty state, but when the tour is saved, the dirty
          * flag can be set before to prevent an out of sync error
          */
         _isTourDirty = false;

         _tourData = TourDatabase.saveTour(_tourData, true);

         updateMarkerMap();

         // refresh combos

         if (_isTitleModified) {

            _comboTitle.clearSelection();
            _comboTitle.removeAll();

            // fill combobox
            final ConcurrentSkipListSet<String> arr = TourDatabase.getCachedFields_AllTourTitles();
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
            final ConcurrentSkipListSet<String> arr = TourDatabase.getCachedFields_AllTourPlaceStarts();
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
            final ConcurrentSkipListSet<String> arr = TourDatabase.getCachedFields_AllTourPlaceEnds();
            for (final String string : arr) {
               _comboLocation_End.add(string);
            }
            _comboLocation_End.update();
            _isLocationEndModified = false;
         }

         setTourClean();

         // notify all views
         TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(_tourData), TourDataEditorView.this);
      }

      /*
       * Linux needs async, otherwise the tour is modified again when pressing Ctrl+S
       */
      _parent.getDisplay().asyncExec(() -> _isSavingInProgress = false);

      getDataSeriesFromTourData();

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

      if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES)) {

         final int runnableRunningId = _timeSlice_Viewer_RunningId.incrementAndGet();

         // delay the selection of multiple lines, moving the mouse can occur very often
         _parent.getDisplay().timerExec(50, new Runnable() {

            private int __runningId = runnableRunningId;

            @Override
            public void run() {

               if (_parent.isDisposed()) {
                  return;
               }

               final int currentId = _timeSlice_Viewer_RunningId.get();

               if (__runningId != currentId) {

                  // a newer runnable is created

                  return;
               }

               final int minSelectedValue = Math.min(chartInfo.leftSliderValuesIndex, chartInfo.rightSliderValuesIndex);
               final int maxSelectedValue = Math.max(chartInfo.leftSliderValuesIndex, chartInfo.rightSliderValuesIndex);

               table.setSelection(minSelectedValue, maxSelectedValue);
               table.showSelection();
            }
         });

      } else {

         final int runnableRunningId = _timeSlice_Viewer_RunningId.incrementAndGet();

         // delay the selection of multiple lines, moving the mouse can occur very often
         _parent.getDisplay().timerExec(20, new Runnable() {

            private int __runningId = runnableRunningId;

            @Override
            public void run() {

               if (_parent.isDisposed()) {
                  return;
               }

               final int currentId = _timeSlice_Viewer_RunningId.get();

               if (__runningId != currentId) {

                  // a newer runnable is created

                  return;
               }

               // adjust to array bounds
               int valueIndex = chartInfo.selectedSliderValuesIndex;
               valueIndex = Math.max(0, Math.min(valueIndex, itemCount - 1));

               table.setSelection(valueIndex);
               table.showSelection();
            }
         });
      }
   }

   private void selectTimeSlice(final SelectionChartXSliderPosition sliderPosition) {

      if (sliderPosition == null) {
         return;
      }

      final int valueIndex_Before = sliderPosition.getBeforeLeftSliderIndex();
      int valueIndex_Start;
      int valueIndex_End;

      if (valueIndex_Before != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

         valueIndex_Start = valueIndex_Before;
         valueIndex_End = sliderPosition.getLeftSliderValueIndex();

      } else {

         valueIndex_Start = sliderPosition.getLeftSliderValueIndex();
         valueIndex_End = sliderPosition.getRightSliderValueIndex();
      }

      selectTimeSlice_InViewer(valueIndex_Start, valueIndex_End);
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

      if (valueIndexStart == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
            && valueIndexEnd == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

         // both positions are ignored

         return;
      }

      final Table table = (Table) _timeSlice_Viewer.getControl();
      final int numItems = table.getItemCount();

      // adjust to array bounds
      final int checkedValueIndex1 = Math.max(0, Math.min(valueIndexStart, numItems - 1));
      final int checkedValueIndex2 = Math.max(0, Math.min(valueIndexEnd, numItems - 1));

      if (valueIndexStart == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

         table.setSelection(checkedValueIndex2);

      } else if (valueIndexEnd == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

         table.setSelection(checkedValueIndex1);

      } else {

         table.setSelection(checkedValueIndex1, checkedValueIndex2);
      }

      table.showSelection();
   }

   public void selectTimeSlicesTab() {
      _tabFolder.setSelection(_tab_20_TimeSlices);
   }

   @Override
   public void setFocus() {

// !!! disabled because the first field gets the focus !!!
//    fTabFolder.setFocus();

      _page_EditorForm.setFocus();

      _parent.getDisplay().asyncExec(() -> updateUI_BackgroundColor());
   }

   /**
    * Programmatically toggles the row select mode
    *
    * @param enabled
    *           True to activate the row select mode, false to deactivate it.
    */
   public void setRowEditModeEnabled(final boolean enabled) {

      _actionToggleRowSelectMode.setChecked(enabled);

      actionToggleRowSelectMode();
   }

   /**
    * Set stroke style for the selected swim slices
    *
    * @param strokeStyle
    *           Stroke style, can be <code>null</code> to remove the stroke style
    */
   void setSwimStyle(final StrokeStyle strokeStyle) {

      // check if stroke style data are available
      if (_swimSerie_StrokeStyle == null) {

         _swimSerie_StrokeStyle = _tourData.swim_StrokeStyle = new short[_swimSerie_Time.length];

         // setup all stroke styles with SwimStroke.INVALID, 0 is the swim stroke for freestyle !!!
         Arrays.fill(_swimSerie_StrokeStyle, SwimStroke.INVALID.getValue());
      }

      final StructuredSelection selection = (StructuredSelection) _swimSlice_Viewer.getSelection();

      final List<?> selectionList = selection.toList();

      for (final Object listItem : selectionList) {

         if (listItem instanceof SwimSlice) {

            final SwimSlice swimSlice = (SwimSlice) listItem;

            _swimSerie_StrokeStyle[swimSlice.serieIndex] = strokeStyle == null
                  ? Short.MIN_VALUE
                  : strokeStyle.swimStroke.getValue();

//          if (value == Short.MIN_VALUE) {
//
//             cell.setText(UI.EMPTY_STRING);
//
//          } else {
//
//             final SwimStroke swimStroke = SwimStroke.getByValue(value);
//             cell.setText(SwimStrokeManager.getLabel(swimStroke));
//          }
         }
      }

      updateUI_AfterSliceEdit();
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

      /**
       * This is not an eclipse editor part but the property change must be fired to hide the "*"
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

      /*
       * Ensure that this view is the active part. It is possible to set tour dirty with the mouse
       * wheel but the save/undo actions are not enabled.
       */

      final IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
      if (this != activePart) {
         Util.showView(ID, true);
      }

      if (_isTourDirty) {
         return;
      }

      _isTourDirty = true;

      enableActions();

      /**
       * This is not an eclipse editor part but the property change must be fired to show the start
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
            updateUI_TourTypeAndTags();

            setTourDirty();
         }
      }
   }

   private void updateInternalUnitValues() {

      _unitValueDistance = UI.UNIT_VALUE_DISTANCE;
      _unitValueElevation = UI.UNIT_VALUE_ELEVATION;

      _unitValueWindSpeed = IWeather.getAllWindSpeeds();
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
   private void updateModel_FromUI() {

      if (_tourData == null) {
         return;
      }

      try {

         _tourData.setTourTitle(_comboTitle.getText());
         _tourData.setTourDescription(_txtDescription.getText());

         _tourData.setTourStartPlace(_comboLocation_Start.getText());
         _tourData.setTourEndPlace(_comboLocation_End.getText());

         final float bodyWeight = UI.convertBodyWeightToMetric(_spinPerson_BodyWeight.getSelection());
         _tourData.setBodyWeight(bodyWeight / 10.0f);
         _tourData.setBodyFat(_spinPerson_BodyFat.getSelection() / 10.0f);
         _tourData.setPower_FTP(_spinPerson_FTP.getSelection());
         _tourData.setCalories(_spinPerson_Calories.getSelection());
         _tourData.setRestPulse(_spinPerson_RestPulse.getSelection());
         _tourData.setCadenceMultiplier(_comboCadence.getSelectedCadence().getMultiplier());

         /*
          * Weather
          */
         _tourData.setWeather(_txtWeather.getText().trim());
         final int weatherWindDirection = _comboWeather_Wind_DirectionText.getSelectionIndex() == 0
               ? -1
               : (int) (_spinWeather_Wind_DirectionValue.getSelection() / 10.0f);

         _tourData.setWeather_Wind_Direction(weatherWindDirection);

         _tourData.setWeather_Humidity((short) _spinWeather_Humidity.getSelection());

         float pressure = _spinWeather_PressureValue.getSelection();

         if (UI.UNIT_IS_PRESSURE_MILLIBAR) {
            pressure /= 10.0f;
         } else {
            pressure /= 100.0f;
         }

         _tourData.setWeather_Pressure(UI.convertPressure_ToMetric(pressure));

         final float precipitation = _spinWeather_PrecipitationValue.getSelection() / 100f;
         _tourData.setWeather_Precipitation(UI.convertPrecipitation_ToMetric(precipitation));

         final float snowfall = _spinWeather_SnowfallValue.getSelection() / 100f;
         _tourData.setWeather_Snowfall(UI.convertPrecipitation_ToMetric(snowfall));

         if (_isWindSpeedManuallyModified) {

            /*
             * Update the speed only when it was modified because when the measurement is changed
             * when the tour is being modified then the computation of the speed value can cause
             * rounding errors
             */

            _tourData.setWeather_Wind_Speed(Math.round((_spinWeather_Wind_SpeedValue.getSelection() * _unitValueDistance)));
         }

         final int cloudIndex = _comboWeather_Clouds.getSelectionIndex();
         String cloudValue = IWeather.cloudIcon[cloudIndex];
         if (cloudValue.equals(UI.IMAGE_EMPTY_16)) {
            // replace invalid cloud key
            cloudValue = UI.EMPTY_STRING;
         }
         _tourData.setWeather_Clouds(cloudValue);

         if (_isTemperatureManuallyModified) {

            final float temperature_Avg_Device = _spinWeather_Temperature_Average.getSelection() / 10.0f;
            final float temperature_Min = _spinWeather_Temperature_Min.getSelection() / 10.0f;
            final float temperature_Max = _spinWeather_Temperature_Max.getSelection() / 10.0f;
            final float temperature_WindChill = _spinWeather_Temperature_WindChill.getSelection() / 10.0f;

            _tourData.setWeather_Temperature_Average(UI.convertTemperatureToMetric(temperature_Avg_Device));
            _tourData.setWeather_Temperature_Min(UI.convertTemperatureToMetric(temperature_Min));
            _tourData.setWeather_Temperature_Max(UI.convertTemperatureToMetric(temperature_Max));
            _tourData.setWeather_Temperature_WindChill(UI.convertTemperatureToMetric(temperature_WindChill));
         }

         /*
          * Time
          */
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

            if (_unitValueElevation != 1) {

               // none metric measurement system

               // ensure float is used
               float noneMetricValue = altitudeUpValue;
               altitudeUpValue = Math.round(noneMetricValue * _unitValueElevation);

               noneMetricValue = altitudeDownValue;
               altitudeDownValue = Math.round(noneMetricValue * _unitValueElevation);
            }

            _tourData.setTourAltUp((int) altitudeUpValue);
            _tourData.setTourAltDown((int) altitudeDownValue);
         }

         // manual tour
         if (_isManualTour) {

            _tourData.setTourDeviceTime_Elapsed(_deviceTime_Elapsed.getTime());
            _tourData.setTourDeviceTime_Recorded(_deviceTime_Recorded.getTime());
            _tourData.setTourDeviceTime_Paused(_deviceTime_Paused.getTime());
            _tourData.setTourComputedTime_Moving(_computedTime_Moving.getTime());
         }

      } catch (final IllegalArgumentException e) {

         // this should not happen (but it happened when developing the tour data editor :-)
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
      fireTourIsModified();
   }

   /**
    * For some controls the background must be set otherwise a wrong background color is
    * displayed
    */
   private void updateUI_BackgroundColor() {

      if (_parent.isDisposed()) {
         return;
      }

      _parent.setRedraw(false);

// SET_FORMATTING_OFF

      if (IS_DARK_THEME) {

         _linkDefaultTimeZone    .setBackground(_backgroundColor_Default);
         _linkGeoTimeZone        .setBackground(_backgroundColor_Default);
         _linkRemoveTimeZone     .setBackground(_backgroundColor_Default);
         _linkTag                .setBackground(_backgroundColor_Default);
         _linkTourType           .setBackground(_backgroundColor_Default);
         _linkWeather            .setBackground(_backgroundColor_Default);
      }

// SET_FORMATTING_ON

      _containerTags_Content.setBackground(_backgroundColor_Default);

//    _containerTags_Content.setBackground(UI.SYS_COLOR_BLUE);

      _parent.setRedraw(true);
   }

   void updateUI_DescriptionNumLines(final int numTourDescriptionLines,
                                     final int numWeatherDescriptionLines) {

      if (numTourDescriptionLines == _descriptionNumLines &&
            numWeatherDescriptionLines == weatherDescriptionNumLines) {

         // nothing has changed
         return;
      }

      _descriptionNumLines = numTourDescriptionLines;
      weatherDescriptionNumLines = numWeatherDescriptionLines;

      // update layouts
      final GridData gd = (GridData) _txtDescription.getLayoutData();
      gd.heightHint = _pc.convertHeightInCharsToPixels(_descriptionNumLines);

      final GridData weatherGridData = (GridData) _txtWeather.getLayoutData();
      weatherGridData.heightHint = _pc.convertHeightInCharsToPixels(weatherDescriptionNumLines);

      onResize_Tab1();
   }

   /**
    * updates the fields in the tour data editor and enables actions and controls
    *
    * @param tourData
    * @param forceSliceReload
    *           <code>true</code> will reload time slices
    * @param isDirtyDisabled
    */
   private void updateUI_FromModel(final TourData tourData,
                                   final boolean forceSliceReload,
                                   final boolean isDirtyDisabled) {

      if (tourData == null) {
         _pageBook.showPage(_page_NoTourData);
         return;
      }

      _uiUpdateCounter++;

      /*
       * set tour data because the TOUR_PROPERTIES_CHANGED event can occur before the runnable is
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

      updateUI_Title_Async(getTourTitle());

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
      _actionSetStartDistanceTo_0.setText(NLS.bind(Messages.TourEditor_Action_SetStartDistanceTo0, UI.UNIT_LABEL_DISTANCE));

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
         Collections.sort(refTourList, (refTour1, refTour2) -> refTour1.getStartValueIndex() - refTour2.getStartValueIndex());

         final StringBuilder sb = new StringBuilder();
         int refCounter = 0;

         _refTourRange = new int[refTourList.size()][2];

         for (final TourReference refTour : refTourList) {

            if (refCounter > 0) {
               sb.append(NL);
            }

            sb.append(refTour.getLabel());

            sb.append(" ("); //$NON-NLS-1$
            sb.append(refTour.getStartValueIndex());
            sb.append(UI.DASH_WITH_SPACE);
            sb.append(refTour.getEndValueIndex());
            sb.append(UI.SYMBOL_BRACKET_RIGHT);

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
      final float bodyWeight = UI.convertBodyWeightFromMetric(_tourData.getBodyWeight());
      _spinPerson_BodyWeight.setSelection(Math.round(bodyWeight * 10));
      _spinPerson_BodyFat.setSelection(Math.round(_tourData.getBodyFat() * 10));

      _spinPerson_FTP.setSelection(_tourData.getPower_FTP());
      _spinPerson_RestPulse.setSelection(_tourData.getRestPulse());
      _spinPerson_Calories.setSelection(_tourData.getCalories());

      /*
       * wind properties
       */
      _txtWeather.setText(_tourData.getWeather());

      // wind direction
      final int weatherWindDirection = _tourData.getWeather_Wind_Direction();
      if (weatherWindDirection == -1) {
         _spinWeather_Wind_DirectionValue.setSelection(0);
         _comboWeather_Wind_DirectionText.select(0);
      } else {
         final int weatherWindDirectionDegree = weatherWindDirection * 10;
         _spinWeather_Wind_DirectionValue.setSelection(weatherWindDirectionDegree);
         _comboWeather_Wind_DirectionText.select(UI.getCardinalDirectionTextIndex((int) (weatherWindDirectionDegree / 10.0f)));
      }

      // wind speed
      final int windSpeed = _tourData.getWeather_Wind_Speed();
      final int speed = Math.round(windSpeed / _unitValueDistance);
      _spinWeather_Wind_SpeedValue.setSelection(speed);
      _comboWeather_WindSpeedText.select(getWindSpeedTextIndex(speed));

      // weather clouds
      _comboWeather_Clouds.select(_tourData.getWeatherIndex());

      // icon must be displayed after the combobox entry is selected
      displayCloudIcon();

      final boolean isTourTemperatureDeviceValid = _tourData.temperatureSerie != null && _tourData.temperatureSerie.length > 0;
      final boolean isTemperatureAvailable = _tourData.isTemperatureAvailable();

      /*
       * Avg temperature from Device
       */
      final float avgTemperature_Device = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_Average_Device());
      _txtWeather_Temperature_Average_Device.setText(isTourTemperatureDeviceValid
            ? _temperatureFormat.format(avgTemperature_Device)
            : UI.EMPTY_STRING);

      /*
       * Min temperature from device
       */
      final float minTemperature_Device = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_Min_Device());
      _txtWeather_Temperature_Min_Device.setText(isTourTemperatureDeviceValid
            ? _temperatureFormat.format(minTemperature_Device)
            : UI.EMPTY_STRING);

      /*
       * Max temperature from device
       */
      final float maxTemperature_Device = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_Max_Device());
      _txtWeather_Temperature_Max_Device.setText(isTourTemperatureDeviceValid
            ? _temperatureFormat.format(maxTemperature_Device)
            : UI.EMPTY_STRING);

      /*
       * Avg temperature
       */
      final float avgTemperature = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_Average());

      _spinWeather_Temperature_Average.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _spinWeather_Temperature_Average.setDigits(1);
      int avgTemperatureValue = 0;
      if (isTemperatureAvailable) {
         avgTemperatureValue = Math.round(avgTemperature * 10);

      }
      _spinWeather_Temperature_Average.setSelection(avgTemperatureValue);

      /*
       * Min temperature
       */
      final float minTemperature = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_Min());

      _spinWeather_Temperature_Min.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _spinWeather_Temperature_Min.setDigits(1);
      int minTemperatureValue = 0;
      if (isTemperatureAvailable) {
         minTemperatureValue = Math.round(minTemperature * 10);

      }
      _spinWeather_Temperature_Min.setSelection(minTemperatureValue);

      /*
       * Max temperature
       */
      final float maxTemperature = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_Max());

      _spinWeather_Temperature_Max.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _spinWeather_Temperature_Max.setDigits(1);
      int maxTemperatureValue = 0;
      if (isTemperatureAvailable) {
         maxTemperatureValue = Math.round(maxTemperature * 10);

      }
      _spinWeather_Temperature_Max.setSelection(maxTemperatureValue);

      /*
       * Wind Chill
       */
      final float avgWindChill = UI.convertTemperatureFromMetric(_tourData.getWeather_Temperature_WindChill());

      _spinWeather_Temperature_WindChill.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _spinWeather_Temperature_WindChill.setDigits(1);
      int avgWindChillValue = 0;
      if (isTemperatureAvailable) {
         avgWindChillValue = Math.round(avgWindChill * 10);

      }
      _spinWeather_Temperature_WindChill.setSelection(avgWindChillValue);

      /*
       * Humidity
       */
      final int humidity = _tourData.getWeather_Humidity();

      _spinWeather_Humidity.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _spinWeather_Humidity.setSelection(humidity);

      /*
       * Precipitation
       */
      final float precipitation = UI.convertPrecipitation_FromMetric(_tourData.getWeather_Precipitation());

      _spinWeather_PrecipitationValue.setDigits(2);
      _spinWeather_PrecipitationValue.setSelection(Math.round(precipitation * 100));
      _spinWeather_PrecipitationValue.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);

      /*
       * Snowfall
       */
      final float snowfall = UI.convertPrecipitation_FromMetric(_tourData.getWeather_Snowfall());

      _spinWeather_SnowfallValue.setDigits(2);
      _spinWeather_SnowfallValue.setSelection(Math.round(snowfall * 100));
      _spinWeather_SnowfallValue.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);

      /*
       * Pressure
       */
      final float pressure = UI.convertPressure_FromMetric(_tourData.getWeather_Pressure());

      if (UI.UNIT_IS_PRESSURE_MILLIBAR) {
         _spinWeather_PressureValue.setDigits(1);
         _spinWeather_PressureValue.setSelection(Math.round(pressure * 10));
      } else {
         _spinWeather_PressureValue.setDigits(2);
         _spinWeather_PressureValue.setSelection(Math.round(pressure * 100));
      }
      _spinWeather_PressureValue.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);

      // Air Quality
      _tableComboWeather_AirQuality.select(_tourData.getWeather_AirQuality_TextIndex());

      /*
       * Time
       */
      // set start date/time without time zone
      final ZonedDateTime tourStartTime = _tourData.getTourStartTime();
      _dtTourDate.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _dtTourDate.setData(UI.FIX_LINUX_ASYNC_EVENT_2, true);
      _dtStartTime.setData(UI.FIX_LINUX_ASYNC_EVENT_1, true);
      _dtStartTime.setData(UI.FIX_LINUX_ASYNC_EVENT_2, true);
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
      _txtAltitudeUp.setText(Integer.toString((int) (altitudeUp / _unitValueElevation)));
      _txtAltitudeDown.setText(Integer.toString((int) (altitudeDown / _unitValueElevation)));

      // tour times
      final int elapsedTime = (int) _tourData.getTourDeviceTime_Elapsed();
      final int movingTime = (int) _tourData.getTourComputedTime_Moving();
      final int recordedTime = (int) _tourData.getTourDeviceTime_Recorded();
      final int pausedTime = (int) _tourData.getTourDeviceTime_Paused();

      _deviceTime_Elapsed.setTime(elapsedTime);
      _deviceTime_Recorded.setTime(recordedTime);
      _deviceTime_Paused.setTime(pausedTime);
      _computedTime_Moving.setTime(movingTime);
      _computedTime_Break.setTime(elapsedTime - movingTime);

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
      updateUI_TagContent();

// SET_FORMATTING_OFF

      // measurement system
      final String averageTemperatureUnit = UI.SYMBOL_AVERAGE + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE;
      final String maxTemperatureUnit     = UI.SYMBOL_MAX + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE;
      final String minTemperatureUnit     = UI.SYMBOL_MIN + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE;

      _lblDistanceUnit                          .setText(UI.UNIT_LABEL_DISTANCE);
      _lblAltitudeUpUnit                        .setText(UI.UNIT_LABEL_ELEVATION);
      _lblAltitudeDownUnit                      .setText(UI.UNIT_LABEL_ELEVATION);
      _lblPerson_BodyWeightUnit                 .setText(UI.UNIT_LABEL_WEIGHT);
      _lblPerson_BodyFatUnit                    .setText(UI.UNIT_PERCENT);
      _lblSpeedUnit                             .setText(UI.UNIT_LABEL_SPEED);
      _lblWeather_PrecipitationUnit             .setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      _lblWeather_PressureUnit                  .setText(UI.UNIT_LABEL_PRESSURE_MBAR_OR_INHG);
      _lblWeather_SnowfallUnit                  .setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      _lblWeather_TemperatureUnit_Avg           .setText(averageTemperatureUnit);
      _lblWeather_TemperatureUnit_Avg_Device    .setText(averageTemperatureUnit);
      _lblWeather_TemperatureUnit_Max           .setText(maxTemperatureUnit);
      _lblWeather_TemperatureUnit_Max_Device    .setText(maxTemperatureUnit);
      _lblWeather_TemperatureUnit_Min           .setText(minTemperatureUnit);
      _lblWeather_TemperatureUnit_Min_Device    .setText(minTemperatureUnit);
      _lblWeather_TemperatureUnit_WindChill     .setText(UI.SYMBOL_TILDE + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE);

// SET_FORMATTING_ON

      // cadence rpm/spm
      final CadenceMultiplier cadence = CadenceMultiplier.getByValue((int) _tourData.getCadenceMultiplier());
      _comboCadence.setSelection(cadence);

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
          * Time slice tab is selected and the viewer is not yet loaded
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
          * Swim slice tab is selected and the viewer is not yet loaded
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

   private void updateUI_TagContent() {

      final Set<TourTag> tourTags = _tourData.getTourTags();

      if (tourTags.size() == 0) {

         // there are not tags

         _pageBook_Tags.showPage(_lblNoTags);

      } else {

         // show tag content

         final TagContentLayout tagContentLayout = TagManager.getTagContentLayout();

         if (TagContentLayout.IMAGE_AND_DATA.equals(tagContentLayout)) {

            // show tag with image

            _pageBook_Tags.showPage(_containerTags_Scrolled);

            TagManager.updateUI_TagsWithImage(_pc, tourTags, _containerTags_Content);

            // update scrolled tag content container
            onResize_TagContent();

         } else {

            // show tag name only

            _pageBook_Tags.showPage(_lblTags);

            TagManager.updateUI_Tags(_tourData, _lblTags);
         }
      }

      onResize_Tab1();
   }

   /**
    * Validate tour elapsed/recorded/paused/moving/break time
    */
   private void updateUI_Time(final Widget widget) {

      /*
       * check if a time control is currently used
       */
      if ((widget == _deviceTime_Elapsed._spinHours
            || widget == _deviceTime_Elapsed._spinMinutes
            || widget == _deviceTime_Elapsed._spinSeconds
            || widget == _deviceTime_Recorded._spinHours
            || widget == _deviceTime_Recorded._spinMinutes
            || widget == _deviceTime_Recorded._spinSeconds
            || widget == _deviceTime_Paused._spinHours
            || widget == _deviceTime_Paused._spinMinutes
            || widget == _deviceTime_Paused._spinSeconds
            || widget == _computedTime_Moving._spinHours
            || widget == _computedTime_Moving._spinMinutes
            || widget == _computedTime_Moving._spinSeconds
            || widget == _computedTime_Break._spinHours
            || widget == _computedTime_Break._spinMinutes
            || widget == _computedTime_Break._spinSeconds) == false) {

         return;
      }

      int elapsedTime = _deviceTime_Elapsed.getTime();
      int recordedTime = _deviceTime_Recorded.getTime();
      int pausedTime = _deviceTime_Paused.getTime();
      int breakTime = _computedTime_Break.getTime();
      int movingTime = _computedTime_Moving.getTime();

      if (elapsedTime < 0) {
         elapsedTime = -elapsedTime - 1;
      }
      if (breakTime < 0) {
         breakTime = 0;
      }

      if (widget == _deviceTime_Elapsed._spinHours
            || widget == _deviceTime_Elapsed._spinMinutes
            || widget == _deviceTime_Elapsed._spinSeconds) {
         // elapsed time is modified

         if (breakTime > elapsedTime) {
            breakTime = elapsedTime;
         }
         if (pausedTime > elapsedTime) {
            pausedTime = elapsedTime;
         }

         movingTime = elapsedTime - breakTime;
         recordedTime = elapsedTime - pausedTime;

      } else if (widget == _deviceTime_Recorded._spinHours
            || widget == _deviceTime_Recorded._spinMinutes
            || widget == _deviceTime_Recorded._spinSeconds) {
         // recorded time is modified

         if (recordedTime > elapsedTime) {
            elapsedTime = recordedTime;
            movingTime = elapsedTime - breakTime;
         }
         pausedTime = elapsedTime - recordedTime;

      } else if (widget == _deviceTime_Paused._spinHours
            || widget == _deviceTime_Paused._spinMinutes
            || widget == _deviceTime_Paused._spinSeconds) {
         // paused time is modified

         if (pausedTime > elapsedTime) {
            elapsedTime = pausedTime;
            movingTime = elapsedTime - breakTime;
         }

         recordedTime = elapsedTime - pausedTime;

      } else if (widget == _computedTime_Break._spinHours
            || widget == _computedTime_Break._spinMinutes
            || widget == _computedTime_Break._spinSeconds) {
         // break time is modified

         if (breakTime > elapsedTime) {
            elapsedTime = breakTime;
            recordedTime = elapsedTime - pausedTime;
         }

         movingTime = elapsedTime - breakTime;

      } else if (widget == _computedTime_Moving._spinHours
            || widget == _computedTime_Moving._spinMinutes
            || widget == _computedTime_Moving._spinSeconds) {
         // moving time is modified

         if (movingTime > elapsedTime) {
            elapsedTime = movingTime;
            recordedTime = elapsedTime - pausedTime;
         }
         breakTime = elapsedTime - movingTime;

      }

// SET_FORMATTING_OFF

      _deviceTime_Elapsed  .setTime(elapsedTime  / 3600, ((elapsedTime % 3600)  / 60), ((elapsedTime % 3600)  % 60));
      _deviceTime_Recorded .setTime(recordedTime / 3600, ((recordedTime % 3600) / 60), ((recordedTime % 3600) % 60));
      _deviceTime_Paused   .setTime(pausedTime   / 3600, ((pausedTime % 3600)   / 60), ((pausedTime % 3600)   % 60));
      _computedTime_Moving .setTime(movingTime   / 3600, ((movingTime % 3600)   / 60), ((movingTime % 3600)   % 60));
      _computedTime_Break  .setTime(breakTime    / 3600, ((breakTime % 3600)    / 60), ((breakTime % 3600)    % 60));

// SET_FORMATTING_ON
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

      if (_dtTourDate.isDisposed()) {
         return;
      }

      final ZoneId zoneId = _tourData == null
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

      updateUI_Title_Async(tourTitle);
   }

   /**
    * Update the title is a really performance hog because of the date/time controls when they are
    * layouted
    */
   private void updateUI_Title_Async(final String title) {

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

   private void updateUI_TourTypeAndTags() {

      // tour type/tags
      net.tourbook.ui.UI.updateUI_TourType(_tourData, _lblTourType, true);
      updateUI_TagContent();

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
      sb.append(UI.UNIT_LABEL_ELEVATION);
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
      sb.append(UI.SYSTEM_NEW_LINE);
      exportWriter.write(sb.toString());
   }
}
