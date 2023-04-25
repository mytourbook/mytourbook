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
package net.tourbook.export;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.FileCollisionBehavior;

import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class DialogExportTour extends TitleAreaDialog {

   private static final String EXPORT_ID_GPX                     = "net.tourbook.export.gpx";           //$NON-NLS-1$
   private static final String EXPORT_ID_TCX                     = "net.tourbook.export.tcx";           //$NON-NLS-1$

   private static final String STATE_GPX_IS_ABSOLUTE_DISTANCE    = "STATE_GPX_IS_ABSOLUTE_DISTANCE";    //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_DESCRITION    = "STATE_GPX_IS_EXPORT_DESCRITION";    //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_MARKERS       = "STATE_GPX_IS_EXPORT_MARKERS";       //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_TOUR_DATA     = "STATE_GPX_IS_EXPORT_TOUR_DATA";     //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_SURFING_WAVES = "STATE_GPX_IS_EXPORT_SURFING_WAVES"; //$NON-NLS-1$
   private static final String STATE_GPX_IS_WITH_BAROMETER       = "STATE_GPX_IS_WITH_BAROMETER";       //$NON-NLS-1$

   private static final String STATE_TCX_ACTIVITY_TYPES          = "STATE_TCX_ACTIVITY_TYPES";          //$NON-NLS-1$
   private static final String STATE_TCX_ACTIVITY_TYPE           = "STATE_TCX_ACTIVITY_TYPE";           //$NON-NLS-1$
   private static final String STATE_TCX_IS_COURSES              = "STATE_TCX_IS_COURSES";              //$NON-NLS-1$
   private static final String STATE_TCX_IS_EXPORT_DESCRITION    = "STATE_TCX_IS_EXPORT_DESCRITION";    //$NON-NLS-1$
   private static final String STATE_TCX_IS_NAME_FROM_TOUR       = "STATE_TCX_IS_NAME_FROM_TOUR";       //$NON-NLS-1$
   private static final String STATE_TCX_COURSE_NAME             = "STATE_TCX_COURSE_NAME";             //$NON-NLS-1$

   private static final String STATE_CAMOUFLAGE_SPEED            = "camouflageSpeedValue";              //$NON-NLS-1$
   private static final String STATE_IS_CAMOUFLAGE_SPEED         = "isCamouflageSpeed";                 //$NON-NLS-1$
   private static final String STATE_IS_EXPORT_TOUR_RANGE        = "isExportTourRange";                 //$NON-NLS-1$
   private static final String STATE_IS_OVERWRITE_FILES          = "isOverwriteFiles";                  //$NON-NLS-1$
   private static final String STATE_IS_MERGE_ALL_TOURS          = "isMergeAllTours";                   //$NON-NLS-1$
   private static final String STATE_EXPORT_PATH_NAME            = "exportPathName";                    //$NON-NLS-1$
   private static final String STATE_EXPORT_FILE_NAME            = "exportFileName";                    //$NON-NLS-1$

   //$NON-NLS-1$

   private static final int VERTICAL_SECTION_MARGIN = 10;
   private static final int SIZING_TEXT_FIELD_WIDTH = 250;
   private static final int COMBO_HISTORY_LENGTH    = 20;

   //
   private static final DecimalFormat _nf3 = (DecimalFormat) NumberFormat.getInstance(Locale.US);
   static {

      _nf3.setMinimumFractionDigits(1);
      _nf3.setMaximumFractionDigits(3);
      _nf3.setGroupingUsed(false);
   }

   // Source: https://developers.strava.com/docs/uploads/#tcx-training-center-database-xml
   public static final String[]      StravaActivityTypes = new String[] {

         "Biking",                                                                                      //$NON-NLS-1$
         "Running",                                                                                     //$NON-NLS-1$
         "Hiking",                                                                                      //$NON-NLS-1$
         "Walking",                                                                                     //$NON-NLS-1$
         "Swimming",                                                                                    //$NON-NLS-1$
         "Other"                                                                                        //$NON-NLS-1$
   };

   private String                    _dlgDefaultMessage;

   private final IDialogSettings     _state              = TourbookPlugin.getState("DialogExportTour"); //$NON-NLS-1$

   private final ExportTourExtension _exportExtensionPoint;

   private final List<TourData>      _tourDataList;
   private final int                 _tourStartIndex;
   private final int                 _tourEndIndex;
   private TourExporter              _tourExporter;

   /**
    * Is <code>true</code> when multiple tours are selected and NOT merged into 1 file.
    */
   private boolean                   _isExport_MultipleToursWithMultipleFiles;

   private boolean                   _isInUIInit;

   /**
    * Is <code>true</code> when GPX export.
    */
   private boolean                   _isSetup_GPX;

   /**
    * Is <code>true</code> when TCX export.
    */
   private boolean                   _isSetup_TCX;
   private boolean                   _isGPXorTCX;

   /**
    * Is <code>true</code> when only a part is exported.
    */
   private boolean                   _isSetup_TourRange;

   /**
    * Is <code>true</code> when multiple tours are exported.
    */
   private boolean                   _isSetup_MultipleTours;

   private int[]                     _mergedDistance     = new int[1];
   private ZonedDateTime[]           _mergedTime         = new ZonedDateTime[1];

   private Point                     _shellDefaultSize;

   private FileCollisionBehavior     _exportState_FileCollisionBehaviour;
   private boolean                   _exportState_isAbsoluteDistance;
   private boolean                   _exportState_IsCamouflageSpeed;
   private boolean                   _exportState_IsDescription;
   private boolean                   _exportState_IsMergeTours;
   private boolean                   _exportState_IsRange;

   private boolean                   _exportState_GPX_IsExportAllTourData;
   private boolean                   _exportState_GPX_IsExportMarkers;
   private boolean                   _exportState_GPX_IsExportSurfingWaves;
   private boolean                   _exportState_GPX_IsExportWithBarometer;
   private String                    _exportState_TCX_CourseName;
   private String                    _exportState_TCX_ActivityType;

   private boolean                   _exportState_TCX_IsCourses;
   private boolean                   _exportState_TCX_IsActivities;

   private PixelConverter            _pc;
   private final String              _formatTemplate;

   /*
    * UI controls
    */
   private Button    _btnSelectFile;

   private Button    _chkCamouflageSpeed;
   private Button    _chkExportTourRange;
   private Button    _chkMergeAllTours;
   private Button    _chkOverwriteFiles;

   private Button    _chkGPX_Description;
   private Button    _rdoGPX_DistanceAbsolute;
   private Button    _rdoGPX_DistanceRelative;
   private Button    _chkGPX_Markers;
   private Button    _chkGPX_NoneGPXFields;
   private Button    _chkGPX_SurfingWaves;
   private Button    _chkGPX_WithBarometer;

   private Button    _chkTCX_Description;
   private Button    _rdoTCX_Activities;
   private Button    _rdoTCX_Courses;
   private Button    _rdoTCX_NameFromField;
   private Button    _rdoTCX_NameFromTour;

   private Combo     _comboFile;
   private Combo     _comboPath;
   private Combo     _comboTcxActivityTypes;
   private Combo     _comboTcxCourseName;

   private Composite _dlgContainer;
   private Composite _inputContainer;

   private Label     _lblCamouflageSpeedUnit;
   private Label     _lblTcxActivityType;
   private Label     _lblTcxCourseName;
   private Label     _lblTcxNameFrom;

   private Spinner   _spinnerCamouflageSpeed;

   private Text      _txtFilePath;

   /**
    * @param parentShell
    * @param exportExtensionPoint
    * @param tourDataList
    * @param tourStartIndex
    * @param tourEndIndex
    * @param formatTemplate
    */
   DialogExportTour(final Shell parentShell,
                    final ExportTourExtension exportExtensionPoint,
                    final List<TourData> tourDataList,
                    final int tourStartIndex,
                    final int tourEndIndex,
                    final String formatTemplate) {

      super(parentShell);

      int shellStyle = getShellStyle();

      shellStyle = //
            SWT.NONE //
                  | SWT.TITLE
                  | SWT.CLOSE
                  | SWT.MIN
//				| SWT.MAX
                  | SWT.RESIZE;

      // make dialog resizable
      setShellStyle(shellStyle);

      _exportExtensionPoint = exportExtensionPoint;
      _formatTemplate = formatTemplate;

      _tourDataList = tourDataList;
      _tourStartIndex = tourStartIndex;
      _tourEndIndex = tourEndIndex;

      _isSetup_GPX = _exportExtensionPoint.getExportId().equalsIgnoreCase(EXPORT_ID_GPX);
      _isSetup_TCX = _exportExtensionPoint.getExportId().equalsIgnoreCase(EXPORT_ID_TCX);
      _isGPXorTCX = _isSetup_GPX || _isSetup_TCX;

      _isSetup_MultipleTours = _tourDataList.size() > 1;
      _isSetup_TourRange = _tourDataList.size() == 1
            && _tourStartIndex >= 0
            && _tourEndIndex > -1;

      _dlgDefaultMessage = NLS.bind(Messages.dialog_export_dialog_message, _exportExtensionPoint.getVisibleName());

      // initialize velocity
      VelocityService.init();
   }

   private String appendSurfingParameters(final TourData minTourData) {

      String postFilename = UI.EMPTY_STRING;

      if (!_isSetup_GPX || !_chkGPX_SurfingWaves.getSelection()) {
         return postFilename;
      }

      postFilename = String.format(

            "__%d-%d-%d-%d", //$NON-NLS-1$

            // min start/stop speed
            minTourData.getSurfing_MinSpeed_StartStop(),

            // min surfing speed
            minTourData.getSurfing_MinSpeed_Surfing(),

            // min time duration
            minTourData.getSurfing_MinTimeDuration(),

            // min distance
            minTourData.isSurfing_IsMinDistance() ? minTourData.getSurfing_MinDistance() : 0

      );

      return postFilename;
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.dialog_export_shell_text);

      shell.addListener(SWT.Resize, event -> {

         // allow resizing the width but not the height

         if (_shellDefaultSize == null) {
            _shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
         }

         final Point shellSize = shell.getSize();

         /*
          * this is not working, the shell is flickering when the shell size is below min size
          * and I found no way to prevent a resize :-(
          */
//				if (shellSize.x < _shellDefaultSize.x) {
//					event.doit = false;
//				}

         shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
         shellSize.y = _shellDefaultSize.y;

         shell.setSize(shellSize);
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.dialog_export_dialog_title);
      setMessage(_dlgDefaultMessage);

      _isInUIInit = true;
      {
         restoreState();
      }
      _isInUIInit = false;

//		validateFields();
      enableFields();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.dialog_export_btn_export);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      _inputContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         if (_isGPXorTCX) {
            createUI_10_Options(_inputContainer);
         }

         createUI_90_ExportFile(_inputContainer);
      }
   }

   private void createUI_10_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         createUI_12_OptionsLeft(container);
         createUI_14_OptionsRight(container);
      }
   }

   private void createUI_12_OptionsLeft(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * What
          */
         final Group groupWhat = new Group(container, SWT.NONE);
         groupWhat.setText(Messages.Dialog_Export_Group_What);
         groupWhat.setToolTipText(Messages.Dialog_Export_Group_What_Tooltip);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(groupWhat);
         GridLayoutFactory.swtDefaults().applyTo(groupWhat);
//			groupWhat.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            createUI_20_Option_What(groupWhat);
            createUI_40_Option_TourRange(groupWhat);
         }

         /*
          * How
          */
         final Group groupHow = new Group(container, SWT.NONE);
         groupHow.setText(Messages.Dialog_Export_Group_How);
         groupHow.setToolTipText(Messages.Dialog_Export_Group_How_Tooltip);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(groupHow);
         GridLayoutFactory.swtDefaults().applyTo(groupHow);
//			groupHow.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            createUI_50_Option_How(groupHow);
         }
      }
   }

   private void createUI_14_OptionsRight(final Composite parent) {

      /*
       * Custom options
       */
      if (_isSetup_GPX) {

         final Group groupCustomGPX = new Group(parent, SWT.NONE);
         groupCustomGPX.setText(Messages.Dialog_Export_Group_Custom);
         groupCustomGPX.setToolTipText(Messages.Dialog_Export_Group_Custom_Tooltip);
         GridDataFactory.fillDefaults().grab(false, false).applyTo(groupCustomGPX);
         GridLayoutFactory.swtDefaults().applyTo(groupCustomGPX);
         {
            createUI_72_Option_GPX_Custom(groupCustomGPX);
         }
      }
   }

   private void createUI_20_Option_What(final Composite parent) {

      if (_isSetup_GPX) {

         {
            /*
             * checkbox: export description
             */
            _chkGPX_Description = new Button(parent, SWT.CHECK);
            _chkGPX_Description.setText(Messages.Dialog_Export_Checkbox_Description);
         }
         {
            /*
             * checkbox: export markers
             */
            _chkGPX_Markers = new Button(parent, SWT.CHECK);
            _chkGPX_Markers.setText(Messages.dialog_export_chk_exportMarkers);
            _chkGPX_Markers.setToolTipText(Messages.dialog_export_chk_exportMarkers_tooltip);
         }
         {
            /*
             * checkbox: export tour data
             */
            _chkGPX_NoneGPXFields = new Button(parent, SWT.CHECK);
            _chkGPX_NoneGPXFields.setText(Messages.Dialog_Export_Checkbox_TourFields);
            _chkGPX_NoneGPXFields.setToolTipText(Messages.Dialog_Export_Checkbox_TourFields_Tooltip);
         }
         {
            /*
             * checkbox: export tour data
             */
            _chkGPX_SurfingWaves = new Button(parent, SWT.CHECK);
            _chkGPX_SurfingWaves.setText(Messages.Dialog_Export_Checkbox_SurfingWaves);
            _chkGPX_SurfingWaves.setToolTipText(Messages.Dialog_Export_Checkbox_SurfingWaves_Tooltip);
            // setup filename
            _chkGPX_SurfingWaves.addSelectionListener(widgetSelectedAdapter(selectionEvent -> enableFields()));
         }

      } else if (_isSetup_TCX) {

         {
            /*
             * checkbox: export description
             */
            _chkTCX_Description = new Button(parent, SWT.CHECK);
            _chkTCX_Description.setText(Messages.dialog_export_chk_exportNotes);
            _chkTCX_Description.setToolTipText(Messages.dialog_export_chk_exportNotes_tooltip);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkTCX_Description);
         }
      }
   }

   private void createUI_40_Option_TourRange(final Composite parent) {

      if (_isSetup_TourRange == false) {
         return;
      }

      String tourRangeUI = null;

      final TourData tourData = _tourDataList.get(0);
      final int[] timeSerie = tourData.timeSerie;
      if (timeSerie != null) {

         final float[] distanceSerie = tourData.distanceSerie;
         final boolean isDistance = distanceSerie != null;

         final int startTime = timeSerie[_tourStartIndex];
         final int endTime = timeSerie[_tourEndIndex];

         final ZonedDateTime dtTour = tourData.getTourStartTime();

         final String uiStartTime = dtTour.plusSeconds(startTime).format(TimeTools.Formatter_Time_M);
         final String uiEndTime = dtTour.plusSeconds(endTime).format(TimeTools.Formatter_Time_M);

         if (isDistance) {

            tourRangeUI = NLS.bind(
                  Messages.dialog_export_chk_tourRangeWithDistance,
                  new Object[] {

                        uiStartTime,
                        uiEndTime,

                        _nf3.format(distanceSerie[_tourStartIndex]
                              / 1000
                              / UI.UNIT_VALUE_DISTANCE),
                        _nf3.format(distanceSerie[_tourEndIndex]
                              / 1000
                              / UI.UNIT_VALUE_DISTANCE),

                        UI.UNIT_LABEL_DISTANCE,

                        // adjust by 1 to corresponds to the number in the tour editor
                        _tourStartIndex + 1,
                        _tourEndIndex + 1 });

         } else {

            tourRangeUI = NLS.bind(Messages.dialog_export_chk_tourRangeWithoutDistance,
                  new Object[] {
                        uiStartTime,
                        uiEndTime,
                        _tourStartIndex + 1,
                        _tourEndIndex + 1 });
         }
      }

      /*
       * checkbox: tour range
       */
      _chkExportTourRange = new Button(parent, SWT.CHECK);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_chkExportTourRange);

      _chkExportTourRange.setText(tourRangeUI != null ? tourRangeUI : Messages.dialog_export_chk_tourRangeDisabled);

      _chkExportTourRange.addSelectionListener(widgetSelectedAdapter(selectionEvent -> enableFields()));
   }

   private void createUI_50_Option_How(final Composite parent) {

      if (_isSetup_MultipleTours) {

         /*
          * checkbox: merge all tours
          */
         _chkMergeAllTours = new Button(parent, SWT.CHECK);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkMergeAllTours);
         _chkMergeAllTours.setText(Messages.dialog_export_chk_mergeAllTours);
         _chkMergeAllTours.setToolTipText(Messages.dialog_export_chk_mergeAllTours_tooltip);
         _chkMergeAllTours.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            enableFields();
            setFileName();
         }));
      }

      createUI_60_Option_Speed(parent);

      if (_isSetup_GPX) {

         createUI_70_Option_GPX_Distance(parent);

      } else if (_isSetup_TCX) {

         createUI_80_Option_TCX_ActivitiesCourses(parent);
      }
   }

   private void createUI_60_Option_Speed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * checkbox: camouflage speed
          */
         _chkCamouflageSpeed = new Button(container, SWT.CHECK);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkCamouflageSpeed);
         _chkCamouflageSpeed.setText(Messages.dialog_export_chk_camouflageSpeed);
         _chkCamouflageSpeed.setToolTipText(Messages.dialog_export_chk_camouflageSpeed_tooltip);
         _chkCamouflageSpeed.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            validateFields();
            enableFields();

            if (_chkCamouflageSpeed.getSelection()) {
               _spinnerCamouflageSpeed.setFocus();
            }
         }));

         // text: speed
         _spinnerCamouflageSpeed = new Spinner(container, SWT.BORDER);
         GridDataFactory.fillDefaults() //
               .align(SWT.BEGINNING, SWT.FILL)
               .applyTo(_spinnerCamouflageSpeed);
         _spinnerCamouflageSpeed.setToolTipText(Messages.dialog_export_chk_camouflageSpeedInput_tooltip);
         _spinnerCamouflageSpeed.setPageIncrement(10);
         _spinnerCamouflageSpeed.setMinimum(1);
         _spinnerCamouflageSpeed.setMaximum(1000);
         _spinnerCamouflageSpeed.addMouseWheelListener(Util::adjustSpinnerValueOnMouseScroll);

         // label: unit
         _lblCamouflageSpeedUnit = UI.createLabel(container, UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_SPEED);
         GridDataFactory
               .fillDefaults()
               .grab(true, false)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblCamouflageSpeedUnit);
      }
   }

   private void createUI_70_Option_GPX_Distance(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         // label
         final Label label = UI.createLabel(container, Messages.Dialog_Export_Label_GPX_DistanceValues);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

         // radio
         {
            _rdoGPX_DistanceAbsolute = new Button(container, SWT.RADIO);
            _rdoGPX_DistanceAbsolute.setText(Messages.Dialog_Export_Radio_GPX_DistanceAbsolute);
            _rdoGPX_DistanceAbsolute.setToolTipText(Messages.Dialog_Export_Radio_GPX_DistanceAbsolute_Tooltip);

            _rdoGPX_DistanceRelative = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_rdoGPX_DistanceRelative);
            _rdoGPX_DistanceRelative.setText(Messages.Dialog_Export_Radio_GPX_DistanceRelative);
            _rdoGPX_DistanceRelative.setToolTipText(Messages.Dialog_Export_Radio_GPX_DistanceRelative_Tooltip);
         }
      }
   }

   private void createUI_72_Option_GPX_Custom(final Composite parent) {

      /*
       * checkbox: export with barometer
       */
      _chkGPX_WithBarometer = new Button(parent, SWT.CHECK);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkGPX_WithBarometer);
      _chkGPX_WithBarometer.setText(Messages.Dialog_Export_Checkbox_WithBarometer);
      _chkGPX_WithBarometer.setToolTipText(Messages.Dialog_Export_Checkbox_WithBarometer_Tooltip);
   }

   private void createUI_80_Option_TCX_ActivitiesCourses(final Composite parent) {

      final SelectionListener defaultSelectionListener = widgetSelectedAdapter(
            selectionEvent -> {
               enableFields();
               setFileName();
            });

      final SelectionListener nameSelectionListener = widgetSelectedAdapter(
            selectionEvent -> {

               updateUI_CourseName();
               enableFields();
               setFileName();
            });

      final ModifyListener nameModifyListener = modifyEvent -> validateFields();

      // container
      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * label: tcx type
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Export_Label_TCX_Type);
            GridDataFactory.fillDefaults().applyTo(label);

            final Composite containerActivities = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerActivities);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerActivities);
            {
               /*
                * radio: courses
                */
               _rdoTCX_Courses = new Button(containerActivities, SWT.RADIO);
               GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_Courses);
               _rdoTCX_Courses.setText(Messages.Dialog_Export_Radio_TCX_Courses);
               _rdoTCX_Courses.setToolTipText(Messages.Dialog_Export_Radio_TCX_Courses_Tooltip);
               _rdoTCX_Courses.addSelectionListener(defaultSelectionListener);

               /*
                * radio: activities
                */
               _rdoTCX_Activities = new Button(containerActivities, SWT.RADIO);
               GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_Activities);
               _rdoTCX_Activities.setText(Messages.Dialog_Export_Radio_TCX_Activities);
               _rdoTCX_Activities.setToolTipText(Messages.Dialog_Export_Radio_TCX_Activities_Tooltip);
               _rdoTCX_Activities.addSelectionListener(defaultSelectionListener);

               /*
                * label: Activity type
                */
               _lblTcxActivityType = UI.createLabel(container, Messages.Dialog_Export_Label_TCX_ActivityType);

               /*
                * combo: Activity types
                */
               _comboTcxActivityTypes = new Combo(container, SWT.SINGLE | SWT.BORDER);
               GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboTcxActivityTypes);
            }
         }

         {
            /*
             * label: course name from
             */
            _lblTcxNameFrom = UI.createLabel(container, Messages.Dialog_Export_Label_TCX_NameFrom);
            GridDataFactory.fillDefaults().applyTo(_lblTcxNameFrom);
            _lblTcxNameFrom.setToolTipText(Messages.Dialog_Export_Label_TCX_NameFrom_Tooltip);

            final Composite containerNameFrom = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerNameFrom);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerNameFrom);
            {
               /*
                * radio: from tour
                */
               _rdoTCX_NameFromTour = new Button(containerNameFrom, SWT.RADIO);
               GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_NameFromTour);
               _rdoTCX_NameFromTour.setText(Messages.Dialog_Export_Radio_TCX_NameFromTour);
               _rdoTCX_NameFromTour.addSelectionListener(nameSelectionListener);

               /*
                * radio: from text field
                */
               _rdoTCX_NameFromField = new Button(containerNameFrom, SWT.RADIO);
               GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_NameFromField);
               _rdoTCX_NameFromField.setText(Messages.Dialog_Export_Radio_TCX_NameFromField);
               _rdoTCX_NameFromField.addSelectionListener(nameSelectionListener);
            }
         }

         {
            /*
             * label: course name
             */
            _lblTcxCourseName = UI.createLabel(container, Messages.Dialog_Export_Label_TCX_CourseName);

            /*
             * combo: name
             */
            _comboTcxCourseName = new Combo(container, SWT.SINGLE | SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboTcxCourseName);
            _comboTcxCourseName.setVisibleItemCount(20);
            _comboTcxCourseName.addModifyListener(nameModifyListener);
         }
      }
   }

   private void createUI_90_ExportFile(final Composite parent) {

      final ModifyListener filePathModifyListener = modifyEvent -> validateFields();

      /*
       * group: filename
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.dialog_export_group_exportFileName);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         /*
          * label: filename
          */
         UI.createLabel(group, Messages.dialog_export_label_fileName);

         /*
          * combo: path
          */
         _comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
         ((GridData) _comboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
         _comboFile.setVisibleItemCount(20);
         _comboFile.addVerifyListener(UI.verifyFilenameInput());
         _comboFile.addModifyListener(filePathModifyListener);
         _comboFile.addSelectionListener(widgetSelectedAdapter(selectionEvent -> validateFields()));

         /*
          * button: browse
          */
         _btnSelectFile = new Button(group, SWT.PUSH);
         _btnSelectFile.setText(Messages.app_btn_browse);
         _btnSelectFile.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            onSelectBrowseFile();
            validateFields();
         }));
         setButtonLayoutData(_btnSelectFile);

         // -----------------------------------------------------------------------------

         /*
          * label: path
          */
         UI.createLabel(group, Messages.dialog_export_label_exportFilePath);

         /*
          * combo: path
          */
         _comboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPath);
         ((GridData) _comboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
         _comboPath.setVisibleItemCount(20);
         _comboPath.addModifyListener(filePathModifyListener);
         _comboPath.addSelectionListener(widgetSelectedAdapter(selectionEvent -> validateFields()));

         /*
          * button: browse
          */
         final Button btnSelectDirectory = new Button(group, SWT.PUSH);
         btnSelectDirectory.setText(Messages.app_btn_browse);
         btnSelectDirectory.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            onSelectBrowseDirectory();
            validateFields();
         }));
         setButtonLayoutData(btnSelectDirectory);

         // -----------------------------------------------------------------------------

         /*
          * label: file path
          */
         UI.createLabel(group, Messages.dialog_export_label_filePath);

         /*
          * text: filename
          */
         _txtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
         _txtFilePath.setToolTipText(Messages.dialog_export_txt_filePath_tooltip);
         _txtFilePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

         // -----------------------------------------------------------------------------

         /*
          * checkbox: overwrite files
          */
         _chkOverwriteFiles = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .span(3, 1)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkOverwriteFiles);
         _chkOverwriteFiles.setText(Messages.dialog_export_chk_overwriteFiles);
         _chkOverwriteFiles.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);
      }

   }

   private void doExport() {

      // disable buttons
      getButton(IDialogConstants.OK_ID).setEnabled(false);
      getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

      _exportState_FileCollisionBehaviour = new FileCollisionBehavior();

      if (_isSetup_GPX) {

         _exportState_isAbsoluteDistance = _rdoGPX_DistanceAbsolute.getSelection();

         _exportState_GPX_IsExportAllTourData = _chkGPX_NoneGPXFields.getSelection();
         _exportState_IsDescription = _chkGPX_Description.getSelection();
         _exportState_GPX_IsExportMarkers = _chkGPX_Markers.getSelection();
         _exportState_GPX_IsExportWithBarometer = _chkGPX_WithBarometer.getSelection();
         _exportState_GPX_IsExportSurfingWaves = _chkGPX_SurfingWaves.getSelection();

      } else if (_isSetup_TCX) {

         _exportState_IsDescription = _chkTCX_Description.getSelection();

         _exportState_TCX_IsActivities = _rdoTCX_Activities.getSelection();
         _exportState_TCX_ActivityType = _comboTcxActivityTypes.getText();

         _exportState_TCX_IsCourses = _rdoTCX_Courses.getSelection();
         _exportState_TCX_CourseName = _comboTcxCourseName.getText();
      }

      int exportState_CamouflageSpeed = 0;

      if (_isGPXorTCX) {

         if (_isSetup_TourRange) {
            _exportState_IsRange = _chkExportTourRange.getSelection();
         }

         if (_isSetup_MultipleTours) {
            _exportState_IsMergeTours = _chkMergeAllTours.getSelection();
         }

         _exportState_IsCamouflageSpeed = _chkCamouflageSpeed.getSelection();

         exportState_CamouflageSpeed = _spinnerCamouflageSpeed.getSelection();
         exportState_CamouflageSpeed *= UI.UNIT_VALUE_DISTANCE / 3.6f;
      }

      final String exportFileName = _txtFilePath.getText();

      boolean isOverwrite = true;
      final boolean exportState_IsOverwriteFiles = _chkOverwriteFiles.getSelection();

      final File exportFile = new File(exportFileName);
      if (exportFile.exists()) {
         if (exportState_IsOverwriteFiles) {
            // overwrite is enabled in the UI
         } else {
            isOverwrite = net.tourbook.ui.UI.confirmOverwrite(_exportState_FileCollisionBehaviour, exportFile);
         }
      }

      if (isOverwrite == false) {
         return;
      }

      net.tourbook.ui.UI.disableAllControls(_inputContainer);

      _tourExporter = new TourExporter(
            _formatTemplate,
            _exportState_IsCamouflageSpeed,
            exportState_CamouflageSpeed,
            _exportState_IsRange,
            _tourStartIndex,
            _tourEndIndex,
            _exportState_GPX_IsExportWithBarometer,
            _exportState_TCX_IsActivities,
            _exportState_TCX_ActivityType,
            _exportState_IsDescription,
            _exportState_GPX_IsExportSurfingWaves,
            _exportState_GPX_IsExportAllTourData,
            _exportState_TCX_IsCourses,
            _exportState_TCX_CourseName);

      if (_exportState_isAbsoluteDistance) {
         _tourExporter.setUseAbsoluteDistance(true);
      }

      if (_tourDataList.size() == 1) {

         // export one tour

         _tourExporter.useTourData(_tourDataList.get(0)).export(exportFileName);

      } else {

         exportMultipleTours(exportFileName);
      }
   }

   private void doExport_05_Runnable(final IProgressMonitor monitor, final String exportFileName) throws IOException {

      final int tourSize = _tourDataList.size();

      monitor.beginTask(UI.EMPTY_STRING, tourSize);

      if (_exportState_IsMergeTours) {

         mergeAllTours(monitor, exportFileName, tourSize);

      } else {

         exportEachTour(monitor, exportFileName, tourSize);
      }
   }

   private void doExport_52_Laps(final TourData tourData, final GarminLap tourLap) {

      /*
       * Calories
       */
      int calories = tourLap.getCalories();
      calories += tourData.getCalories();
      tourLap.setCalories(calories);

      /*
       * Description
       */
      if (_exportState_IsDescription) {

         final String notes = tourData.getTourDescription();

         if (StringUtils.hasContent(notes)) {

            final String lapNotes = tourLap.getNotes();

            final String tourLapNotes = StringUtils.isNullOrEmpty(lapNotes)
                  ? notes
                  : lapNotes + "\n" + notes; //$NON-NLS-1$
            tourLap.setNotes(tourLapNotes);
         }
      }
   }

   private void enableExportButton(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   private void enableFields() {

      final boolean isCamouflageSpeed = _isGPXorTCX ? _chkCamouflageSpeed.getSelection() : false;
      final boolean isSingleTour = _isSetup_MultipleTours == false;
      boolean isMergeIntoOneTour = false;

      if (_isGPXorTCX && _isSetup_MultipleTours) {

         isMergeIntoOneTour = _chkMergeAllTours.getSelection();
         _chkMergeAllTours.setEnabled(_isSetup_MultipleTours);
      }

      _isExport_MultipleToursWithMultipleFiles = _isSetup_MultipleTours && isMergeIntoOneTour == false;

      if (_isSetup_GPX) {

         final boolean isNoneGPX = isSingleTour || _isExport_MultipleToursWithMultipleFiles;
         _chkGPX_NoneGPXFields.setEnabled(isNoneGPX);
         if (!isNoneGPX) {
            // deselect when not checked
            _chkGPX_NoneGPXFields.setSelection(false);
         }

      } else if (_isSetup_TCX) {

         final boolean isCourse = _rdoTCX_Courses.getSelection();
         final boolean isActivity = _rdoTCX_Activities.getSelection();
         final boolean isFromField = _rdoTCX_NameFromField.getSelection();

         _lblTcxNameFrom.setEnabled(isCourse);
         _rdoTCX_NameFromTour.setEnabled(isCourse);
         _rdoTCX_NameFromField.setEnabled(isCourse);

         _lblTcxActivityType.setEnabled(isActivity);
         _comboTcxActivityTypes.setEnabled(isActivity);

         _lblTcxCourseName.setEnabled(isCourse && isFromField);
         _comboTcxCourseName.setEnabled(isCourse && isFromField);
      }

      _comboFile.setEnabled(isSingleTour || isMergeIntoOneTour);
      _btnSelectFile.setEnabled(isSingleTour || isMergeIntoOneTour);

      if (_isGPXorTCX) {

         _spinnerCamouflageSpeed.setEnabled(isCamouflageSpeed);
         _lblCamouflageSpeedUnit.setEnabled(isCamouflageSpeed);
      }

      setFileName();
   }

   /**
    * Export each tour separately
    *
    * @param monitor
    * @param exportFileName
    * @param exported
    * @param tourSize
    */
   private void exportEachTour(final IProgressMonitor monitor, final String exportFileName, final int tourSize) {

      int exported = 0;
      final IPath exportFilePath = new Path(exportFileName).addTrailingSeparator();
      final String fileExtension = _exportExtensionPoint.getFileExtension();

      for (int index = 0; index < _tourDataList.size() && !monitor.isCanceled(); ++index) {

         final TourData tourData = _tourDataList.get(index);

         // merge distance is also used as total distance for not merged tours
         _mergedDistance[0] = 0;

         // create file path name
         final String tourFileName = net.tourbook.ui.UI.format_yyyymmdd_hhmmss(tourData);

         final String exportFilePathName = exportFilePath
               .append(tourFileName)
               .addFileExtension(fileExtension)
               .toOSString();

         monitor.worked(1);
         monitor.subTask(NLS.bind(Messages.Dialog_Export_SubTask_Export,
               new Object[] {
                     ++exported,
                     tourSize,
                     exportFilePathName }));

         _tourExporter.useTourData(tourData);
         _tourExporter.export(exportFilePathName);

         // check if overwrite dialog was canceled
         if (_exportState_FileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
            break;
         }
      }
   }

   /**
    * Export multiple tours
    *
    * @param exportFileName
    */
   private void exportMultipleTours(final String exportFileName) {

      final String exportPathName;

      if (_exportState_IsMergeTours) {
         exportPathName = exportFileName;
      } else {
         exportPathName = getExportPathName();
      }

      try {

         final IRunnableWithProgress exportRunnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                  InterruptedException {

               try {

                  doExport_05_Runnable(monitor, exportPathName);

               } catch (final IOException e) {
                  StatusUtil.log(e);
               }
            }
         };

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, exportRunnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.showStatus(e);
         Thread.currentThread().interrupt();
      }
   }

   private String getActivityType() {
      return _comboTcxActivityTypes.getText().trim();
   }

   private String getCourseName() {
      return _comboTcxCourseName.getText().trim();
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      // keep window size and position
      return _state;
   }

   private String getExportFileName() {
      return _comboFile.getText().trim();
   }

   private String getExportPathName() {
      return _comboPath.getText().trim();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   /**
    * Merge all tours into one
    *
    * @param monitor
    * @param exportFileName
    * @param tourSize
    * @throws IOException
    */
   private void mergeAllTours(final IProgressMonitor monitor, final String exportFileName, final int tourSize) throws IOException {

      int exported = 0;
      _mergedTime[0] = _tourDataList.get(0).getTourStartTime();
      _mergedDistance[0] = 0;

      final ArrayList<GarminTrack> tracks = new ArrayList<>();
      final ArrayList<TourWayPoint> wayPoints = new ArrayList<>();
      final ArrayList<TourMarker> tourMarkers = new ArrayList<>();

      final GarminLap tourLap = new GarminLap();

      // create tracklist and lap
      for (final TourData tourData : _tourDataList) {

         if (monitor.isCanceled()) {
            return;
         }

         monitor.worked(1);
         monitor.subTask(NLS.bind(Messages.Dialog_Export_SubTask_Export,
               new Object[] {
                     ++exported,
                     tourSize,
                     TourManager.getTourTitle(tourData) }));

         doExport_52_Laps(tourData, tourLap);

         ZonedDateTime trackStartTime;
         if (_exportState_IsCamouflageSpeed) {
            trackStartTime = _mergedTime[0];
         } else {
            trackStartTime = tourData.getTourStartTime();
         }

         final GarminTrack track = _tourExporter.useTourData(tourData).doExport_60_TrackPoints(trackStartTime, _mergedTime, _mergedDistance);
         if (track != null) {
            tracks.add(track);
         }

         // get markers when this option is checked
         if (_exportState_GPX_IsExportMarkers) {

            _tourExporter.doExport_70_WayPoints(wayPoints, tourMarkers, trackStartTime);
         }
      }

      /*
       * There is currently no listener to stop the velocity evaluate method
       */
      monitor.subTask(NLS.bind(Messages.Dialog_Export_SubTask_CreatingExportFile, exportFileName));

      _tourExporter.doExport_10_Tour(tracks, wayPoints, tourMarkers, tourLap, exportFileName);
   }

   @Override
   protected void okPressed() {

      BusyIndicator.showWhile(Display.getCurrent(), this::doExport);

      if (_exportState_FileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
         getButton(IDialogConstants.OK_ID).setEnabled(true);
         getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
         return;
      }
      super.okPressed();
   }

   private void onSelectBrowseDirectory() {

      final DirectoryDialog dialog = new DirectoryDialog(_dlgContainer.getShell(), SWT.SAVE);
      dialog.setText(Messages.dialog_export_dir_dialog_text);
      dialog.setMessage(Messages.dialog_export_dir_dialog_message);

      dialog.setFilterPath(getExportPathName());

      final String selectedDirectoryName = dialog.open();

      if (selectedDirectoryName != null) {
         setErrorMessage(null);
         _comboPath.setText(selectedDirectoryName);
      }
   }

   private void onSelectBrowseFile() {

      final String fileExtension = _exportExtensionPoint.getFileExtension();

      final FileDialog dialog = new FileDialog(_dlgContainer.getShell(), SWT.SAVE);
      dialog.setText(Messages.dialog_export_file_dialog_text);

      dialog.setFilterPath(getExportPathName());
      dialog.setFilterExtensions(new String[] { fileExtension });
      dialog.setFileName("*." + fileExtension);//$NON-NLS-1$

      final String selectedFilePath = dialog.open();

      if (selectedFilePath != null) {
         setErrorMessage(null);
         _comboFile.setText(new Path(selectedFilePath).toFile().getName());
      }
   }

   private void restoreState() {

      if (_isSetup_GPX) {

         final boolean isAbsoluteDistance = Util.getStateBoolean(_state, STATE_GPX_IS_ABSOLUTE_DISTANCE, true);

         _chkGPX_Description.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_DESCRITION));
         _chkGPX_Markers.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_MARKERS));
         _chkGPX_NoneGPXFields.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_TOUR_DATA));
         _chkGPX_SurfingWaves.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_SURFING_WAVES));
         _chkGPX_WithBarometer.setSelection(_state.getBoolean(STATE_GPX_IS_WITH_BAROMETER));

         _rdoGPX_DistanceAbsolute.setSelection(isAbsoluteDistance);
         _rdoGPX_DistanceRelative.setSelection(!isAbsoluteDistance);

      } else if (_isSetup_TCX) {

         final boolean isCourses = Util.getStateBoolean(_state, STATE_TCX_IS_COURSES, true);
         final boolean isFromTour = Util.getStateBoolean(_state, STATE_TCX_IS_NAME_FROM_TOUR, true);

         _chkTCX_Description.setSelection(_state.getBoolean(STATE_TCX_IS_EXPORT_DESCRITION));

         _rdoTCX_Courses.setSelection(isCourses);
         _rdoTCX_Activities.setSelection(!isCourses);

         _rdoTCX_NameFromTour.setSelection(isFromTour);
         _rdoTCX_NameFromField.setSelection(!isFromTour);

         UI.restoreCombo(_comboTcxCourseName, _state.getArray(STATE_TCX_COURSE_NAME));

         final String[] activityTypes = _state.getArray(STATE_TCX_ACTIVITY_TYPES);
         if (activityTypes == null) {
            /*
             * Fill-up the default activity types
             */
            Arrays.asList(StravaActivityTypes).forEach(activityType -> _comboTcxActivityTypes.add(activityType));
         } else {
            UI.restoreCombo(_comboTcxActivityTypes, activityTypes);
         }

         final String lastSelected_ActivityType = _state.get(STATE_TCX_ACTIVITY_TYPE);
         if (lastSelected_ActivityType == null) {
            _comboTcxActivityTypes.select(0);
         } else {
            _comboTcxActivityTypes.select(_comboTcxActivityTypes.indexOf(lastSelected_ActivityType));
         }

         new AutocompleteComboInput(_comboTcxActivityTypes);

         updateUI_CourseName();
      }

      if (_isGPXorTCX) {

         // merge all tours
         if (_isSetup_MultipleTours) {
            _chkMergeAllTours.setSelection(_state.getBoolean(STATE_IS_MERGE_ALL_TOURS));
         }

         // export tour part
         if (_isSetup_TourRange) {
            _chkExportTourRange.setSelection(_state.getBoolean(STATE_IS_EXPORT_TOUR_RANGE));
         }

         // camouflage speed
         _chkCamouflageSpeed.setSelection(_state.getBoolean(STATE_IS_CAMOUFLAGE_SPEED));
         _spinnerCamouflageSpeed.setSelection(Util.getStateInt(_state, STATE_CAMOUFLAGE_SPEED, 10));
      }

      // export file/path
      UI.restoreCombo(_comboFile, _state.getArray(STATE_EXPORT_FILE_NAME));
      UI.restoreCombo(_comboPath, _state.getArray(STATE_EXPORT_PATH_NAME));
      _chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_FILES));
   }

   private void saveState() {

// SET_FORMATTING_OFF

      if (_isSetup_GPX) {

         _state.put(STATE_GPX_IS_EXPORT_DESCRITION,      _chkGPX_Description.getSelection());
         _state.put(STATE_GPX_IS_ABSOLUTE_DISTANCE,      _rdoGPX_DistanceAbsolute.getSelection());
         _state.put(STATE_GPX_IS_EXPORT_MARKERS,         _chkGPX_Markers.getSelection());
         _state.put(STATE_GPX_IS_EXPORT_TOUR_DATA,       _chkGPX_NoneGPXFields.getSelection());
         _state.put(STATE_GPX_IS_EXPORT_SURFING_WAVES,   _chkGPX_SurfingWaves.getSelection());
         _state.put(STATE_GPX_IS_WITH_BAROMETER,         _chkGPX_WithBarometer.getSelection());

      } else if (_isSetup_TCX) {

         _state.put(STATE_TCX_IS_COURSES,                _rdoTCX_Courses.getSelection());
         _state.put(STATE_TCX_IS_EXPORT_DESCRITION,      _chkTCX_Description.getSelection());
         _state.put(STATE_TCX_IS_NAME_FROM_TOUR,         _rdoTCX_NameFromTour.getSelection());
         _state.put(STATE_TCX_COURSE_NAME,               Util.getUniqueItems(_comboTcxCourseName.getItems(), getCourseName(), COMBO_HISTORY_LENGTH));

         final String currentText = _comboTcxActivityTypes.getText();
         final List<String> comboItems = Arrays.asList(_comboTcxActivityTypes.getItems());
         if (!comboItems.contains(currentText)) {
            _comboTcxActivityTypes.add(getActivityType());
         }
         _state.put(STATE_TCX_ACTIVITY_TYPES,            _comboTcxActivityTypes.getItems());
         _state.put(STATE_TCX_ACTIVITY_TYPE,             getActivityType());
      }

      // camouflage speed
      if (_isGPXorTCX) {

         _state.put(STATE_IS_CAMOUFLAGE_SPEED,           _chkCamouflageSpeed.getSelection());
         _state.put(STATE_CAMOUFLAGE_SPEED,              _spinnerCamouflageSpeed.getSelection());

         // merge all tours
         if (_isSetup_MultipleTours) {
            _state.put(STATE_IS_MERGE_ALL_TOURS,         _chkMergeAllTours.getSelection());
         }

         // export tour part
         if (_isSetup_TourRange) {
            _state.put(STATE_IS_EXPORT_TOUR_RANGE,       _chkExportTourRange.getSelection());
         }
      }

      // export file/path
      if (validateFilePath()) {
         _state.put(STATE_EXPORT_PATH_NAME,              Util.getUniqueItems(_comboPath.getItems(), getExportPathName(), COMBO_HISTORY_LENGTH));
         _state.put(STATE_EXPORT_FILE_NAME,              Util.getUniqueItems(_comboFile.getItems(), getExportFileName(), COMBO_HISTORY_LENGTH));
      }
      _state.put(STATE_IS_OVERWRITE_FILES,               _chkOverwriteFiles.getSelection());

// SET_FORMATTING_ON

   }

   private void setError(final String message) {
      setErrorMessage(message);
      enableExportButton(false);
   }

   /**
    * Set filename with the first tour date/time, when tour is merged "<#default>" is displayed
    */
   private void setFileName() {

      // search for the first tour
      TourData minTourData = _tourDataList.get(0);
      long minTourMillis = minTourData.getTourStartTime().toInstant().toEpochMilli();

      for (final TourData tourData : _tourDataList) {

         final long tourMillis = tourData.getTourStartTime().toInstant().toEpochMilli();

         if (tourMillis < minTourMillis) {
            minTourData = tourData;
            minTourMillis = minTourData.getTourStartTime().toInstant().toEpochMilli();
         }
      }

      if (_isExport_MultipleToursWithMultipleFiles) {

         // use default file name for each exported tour

         _comboFile.setText(Messages.dialog_export_label_DefaultFileName);

      } else if (_isSetup_TourRange) {

         // display the start date/time

         final ZonedDateTime dtTour = minTourData.getTourStartTime();

         // adjust start time
         final int startTime = minTourData.timeSerie[_tourStartIndex];
         final ZonedDateTime tourTime = dtTour.plusSeconds(startTime);

         _comboFile.setText(UI.format_yyyymmdd_hhmmss(
               tourTime.getYear(),
               tourTime.getMonthValue(),
               tourTime.getDayOfMonth(),
               tourTime.getHour(),
               tourTime.getMinute(),
               tourTime.getSecond()));
      } else {

         // display the tour date/time

         final String postFilename = appendSurfingParameters(minTourData);

         _comboFile.setText(net.tourbook.ui.UI.format_yyyymmdd_hhmmss(minTourData) + postFilename);
      }
   }

   private void updateUI_CourseName() {

      if (_isSetup_TCX == false) {
         return;
      }

      if (_rdoTCX_NameFromTour.getSelection()) {

         /*
          * set course name from tour
          */

         String courseName = UI.EMPTY_STRING;

         for (final TourData tourData : _tourDataList) {
            final String tourTitle = tourData.getTourTitle().trim();
            if (tourTitle.length() > 0) {
               courseName = tourTitle;
               break;
            }
         }
         _comboTcxCourseName.setText(courseName);
      }
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      /*
       * validate fields
       */

      if (_isSetup_TCX && _rdoTCX_Courses.getSelection() && getCourseName().length() == 0) {
         setError(Messages.Dialog_Export_Error_CourseNameIsInvalid);
         _comboTcxCourseName.setFocus();
         return;
      }

      if (validateFilePath() == false) {
         return;
      }

      setErrorMessage(null);
      enableExportButton(true);
   }

   private boolean validateFilePath() {

      // check path
      IPath filePath = new Path(getExportPathName());
      if (new File(filePath.toOSString()).exists() == false) {

         // invalid path
         setError(NLS.bind(Messages.dialog_export_msg_pathIsNotAvailable, filePath.toOSString()));
         return false;
      }

      boolean returnValue = false;

      if (_isExport_MultipleToursWithMultipleFiles) {

         // only the path is checked, the file name is created automatically for each exported tour

         setMessage(_dlgDefaultMessage);

         // build file path with extension
         filePath = filePath
               .addTrailingSeparator()
               .append(Messages.dialog_export_label_DefaultFileName)
               .addFileExtension(_exportExtensionPoint.getFileExtension());

         returnValue = true;

      } else {

         String fileName = getExportFileName();

         fileName = FileUtils.removeExtensions(fileName);

         // build file path with extension
         filePath = filePath
               .addTrailingSeparator()
               .append(fileName)
               .addFileExtension(_exportExtensionPoint.getFileExtension());

         final File newFile = new File(filePath.toOSString());

         if (fileName.length() == 0 || newFile.isDirectory()) {

            // invalid filename

            setError(Messages.dialog_export_msg_fileNameIsInvalid);

         } else if (newFile.exists()) {

            // file already exists

            setMessage(
                  NLS.bind(Messages.dialog_export_msg_fileAlreadyExists, filePath.toOSString()),
                  IMessageProvider.WARNING);
            returnValue = true;

         } else {

            setMessage(_dlgDefaultMessage);

            try {
               final boolean isFileCreated = newFile.createNewFile();

               // name is correct

               if (isFileCreated) {
                  // delete file because the file is created for checking validity
                  newFile.delete();
               }
               returnValue = true;

            } catch (final IOException ioe) {
               setError(Messages.dialog_export_msg_fileNameIsInvalid);
            }
         }
      }

      _txtFilePath.setText(filePath.toOSString());

      return returnValue;
   }
}
