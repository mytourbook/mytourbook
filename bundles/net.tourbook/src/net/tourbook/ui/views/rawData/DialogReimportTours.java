/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.ui.views.rawData;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.TourValueType;
import net.tourbook.importdata.ReImportStatus;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

public class DialogReimportTours extends TitleAreaDialog {

   private static final String STATE_REIMPORT_TOURS_ALL                     = "STATE_REIMPORT_TOURS_ALL";                     //$NON-NLS-1$
   private static final String STATE_REIMPORT_TOURS_SELECTED                = "STATE_REIMPORT_TOURS_SELECTED";                //$NON-NLS-1$

   private static final String STATE_REIMPORT_TOURS_BETWEEN_DATES           = "STATE_REIMPORT_TOURS_BETWEEN_DATES";           //$NON-NLS-1$
   private static final String STATE_REIMPORT_TOURS_BETWEEN_DATES_FROM      = "STATE_REIMPORT_TOURS_BETWEEN_DATES_FROM";      //$NON-NLS-1$
   private static final String STATE_REIMPORT_TOURS_BETWEEN_DATES_UNTIL     = "STATE_REIMPORT_TOURS_BETWEEN_DATES_UNTIL";     //$NON-NLS-1$

   private static final String STATE_IS_IMPORT_ALL_TIME_SLICES              = "STATE_IS_IMPORT_ALL_TIME_SLICES";              //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_ENTIRE_TOUR                  = "STATE_IS_IMPORT_ENTIRE_TOUR";                  //$NON-NLS-1$

   private static final String STATE_IS_IMPORT_TOUR__CALORIES               = "STATE_IS_IMPORT_TOUR__CALORIES";               //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TOUR__FILE_LOCATION          = "STATE_IS_IMPORT_TOUR__FILE_LOCATION";          //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TOUR__MARKERS                = "STATE_IS_IMPORT_TOUR__MARKERS";                //$NON-NLS-1$

   private static final String STATE_IS_IMPORT_TIME_SLICE__BATTERY          = "STATE_IS_IMPORT_TIME_SLICE__BATTERY";          //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__CADENCE          = "STATE_IS_IMPORT_TIME_SLICE__CADENCE";          //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__ELEVATION        = "STATE_IS_IMPORT_TIME_SLICE__ELEVATION";        //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__GEAR             = "STATE_IS_IMPORT_TIME_SLICE__GEAR";             //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__POWER_AND_PULSE  = "STATE_IS_IMPORT_TIME_SLICE__POWER_AND_PULSE";  //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__POWER_AND_SPEED  = "STATE_IS_IMPORT_TIME_SLICE__POWER_AND_SPEED";  //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__RUNNING_DYNAMICS = "STATE_IS_IMPORT_TIME_SLICE__RUNNING_DYNAMICS"; //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__SWIMMING         = "STATE_IS_IMPORT_TIME_SLICE__SWIMMING";         //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__TEMPERATURE      = "STATE_IS_IMPORT_TIME_SLICE__TEMPERATURE";      //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__TRAINING         = "STATE_IS_IMPORT_TIME_SLICE__TRAINING";         //$NON-NLS-1$
   private static final String STATE_IS_IMPORT_TIME_SLICE__TIMER_PAUSES     = "STATE_IS_IMPORT_TIME_SLICE__TIMER_PAUSES";     //$NON-NLS-1$

   private static final String STATE_IS_LOG_DETAILS                         = "STATE_IS_LOG_DETAILS";                         //$NON-NLS-1$
   private static final String STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND = "STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND"; //$NON-NLS-1$

   private static final int    VERTICAL_SECTION_MARGIN                      = 10;

   //
   private static final IDialogSettings    _state          = TourbookPlugin.getState("DialogReimportTours"); //$NON-NLS-1$

   private static ThreadPoolExecutor       _reimport_Executor;
   private static CountDownLatch           _reimport_CountDownLatch;
   private static ArrayBlockingQueue<Long> _reimport_Queue = new ArrayBlockingQueue<>(
         RawDataManager.isSingleThreadTourImport()
               ? 1
               : Util.NUMBER_OF_PROCESSORS);

   static {

      final ThreadFactory threadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Re-importing tours");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _reimport_Executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
            RawDataManager.isSingleThreadTourImport()
                  ? 1
                  : Util.NUMBER_OF_PROCESSORS,
            threadFactory);
   }

   private final ITourViewer3 _tourViewer;

   private SelectionAdapter   _defaultListener;

   private PixelConverter     _pc;

   //
   private Image _imageLock_Closed = CommonActivator.getThemedImageDescriptor(CommonImages.Lock_Closed).createImage();
   private Image _imageLock_Open   = CommonActivator.getThemedImageDescriptor(CommonImages.Lock_Open).createImage();

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _btnDeselectAll;
   private Button    _btnUnlockAllToursSelection;
   private Button    _btnUnlockBetweenDatesSelection;

   private Button    _chkData_AllTimeSlices;
   private Button    _chkData_TimeSlice_Battery;
   private Button    _chkData_TimeSlice_Cadence;
   private Button    _chkData_Tour_Calories;
   private Button    _chkData_TimeSlice_Elevation;
   private Button    _chkData_TimeSlice_Gear;
   private Button    _chkData_Tour_ImportFileLocation;
   private Button    _chkData_TimeSlice_PowerAndPulse;
   private Button    _chkData_TimeSlice_PowerAndSpeed;
   private Button    _chkData_TimeSlice_RunningDynamics;
   private Button    _chkData_TimeSlice_Swimming;
   private Button    _chkData_TimeSlice_Temperature;
   private Button    _chkData_TimeSlice_Training;
   private Button    _chkData_Tour_Markers;
   private Button    _chkData_TimeSlice_TourTimerPauses;

   private Button    _chkLogDetails;
   private Button    _chkSkipTours_With_ImportFile_NotFound;

   private Button    _rdoData_EntireTour;
   private Button    _rdoData_PartOfATour;
   private Button    _rdoReimport_Tours_All;
   private Button    _rdoReimport_Tours_BetweenDates;
   private Button    _rdoReimport_Tours_Selected;

   private DateTime  _dtTourDate_From;
   private DateTime  _dtTourDate_Until;

   private Group     _groupTours;

   /**
    * @param parentShell
    */
   public DialogReimportTours(final Shell parentShell,
                              final ITourViewer3 tourViewer) {

      super(parentShell);

      _tourViewer = tourViewer;
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_ReimportTours_Dialog_Title);

      shell.addListener(SWT.Resize, event -> {

         // force shell default size

         final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

         shell.setSize(shellDefaultSize);
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_ReimportTours_Dialog_Title);
      setMessage(Messages.Dialog_ReimportTours_Dialog_Message);

      restoreState();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_ReimportTours_Button_ReImport);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      initUI();

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      // must be run async because the dark theme is overwriting colors after calling createDialogArea()
      _parent.getDisplay().asyncExec(this::updateUI_LockUnlockButtons);

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(container);
      {
         createUI_10_Tours(container);
         createUI_20_Data(container);
         createUI_30_Options(container);
      }
   }

   /**
    * UI to select either all the tours in the database or only the selected tours
    *
    * @param parent
    */
   private void createUI_10_Tours(final Composite parent) {

      _groupTours = new Group(parent, SWT.NONE);
      _groupTours.setText(Messages.Dialog_ReimportTours_Group_Tours);
      _groupTours.setToolTipText(Messages.Dialog_ReimportTours_Group_Tours_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupTours);
      GridLayoutFactory.swtDefaults().spacing(5, 7).numColumns(4).applyTo(_groupTours);
      {
         {
            /*
             * Re-import the SELECTED tours
             */
            _rdoReimport_Tours_Selected = new Button(_groupTours, SWT.RADIO);
            _rdoReimport_Tours_Selected.setText(Messages.Dialog_ModifyTours_Radio_SelectedTours);
            _rdoReimport_Tours_Selected.addSelectionListener(_defaultListener);
            _rdoReimport_Tours_Selected.setSelection(true);
            GridDataFactory.fillDefaults().span(4, 1).applyTo(_rdoReimport_Tours_Selected);
         }
         {
            /*
             * Re-import ALL tours in the database
             */
            _rdoReimport_Tours_All = new Button(_groupTours, SWT.RADIO);
            _rdoReimport_Tours_All.setText(Messages.Dialog_ModifyTours_Radio_AllTours);
            _rdoReimport_Tours_All.setSelection(false);
            _rdoReimport_Tours_All.setEnabled(false);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_rdoReimport_Tours_All);
         }
         {
            _btnUnlockAllToursSelection = new Button(_groupTours, SWT.PUSH);
            _btnUnlockAllToursSelection.setText(Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);
            _btnUnlockAllToursSelection.setImage(_imageLock_Closed);
            _btnUnlockAllToursSelection.addSelectionListener(
                  widgetSelectedAdapter(selectionEvent -> onSelect_Unlock_AllTours()));
         }
         {
            /*
             * Re-import between dates
             */
            _rdoReimport_Tours_BetweenDates = new Button(_groupTours, SWT.RADIO);
            _rdoReimport_Tours_BetweenDates.setText(Messages.Dialog_ModifyTours_Radio_BetweenDates);
            _rdoReimport_Tours_BetweenDates.setSelection(false);
            _rdoReimport_Tours_BetweenDates.setEnabled(false);
            {
               _dtTourDate_From = new DateTime(_groupTours, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
               _dtTourDate_From.addSelectionListener(_defaultListener);
               _dtTourDate_From.setEnabled(false);
            }
            {
               _dtTourDate_Until = new DateTime(_groupTours, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
               _dtTourDate_Until.addSelectionListener(_defaultListener);
               _dtTourDate_Until.setEnabled(false);
            }
            _btnUnlockBetweenDatesSelection = new Button(_groupTours, SWT.PUSH);
            _btnUnlockBetweenDatesSelection.setText(Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);
            _btnUnlockBetweenDatesSelection.setImage(_imageLock_Closed);
            _btnUnlockBetweenDatesSelection.addSelectionListener(
                  widgetSelectedAdapter(selectionEvent -> onSelect_Unlock_BetweenDates()));
         }
      }
   }

   /**
    * UI to select the data to re-import for the chosen tours
    *
    * @param parent
    */
   private void createUI_20_Data(final Composite parent) {

      final GridDataFactory gridDataTour = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .span(2, 1);

      /*
       * Group: data
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ReimportTours_Group_Data);
      group.setToolTipText(Messages.Dialog_ReimportTours_Group_Data_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         {
            /*
             * Radio: Entire Tour
             */
            _rdoData_EntireTour = new Button(group, SWT.RADIO);
            _rdoData_EntireTour.setText(Messages.Dialog_ReimportTours_Checkbox_EntireTour);
            _rdoData_EntireTour.addSelectionListener(_defaultListener);
            gridDataTour.applyTo(_rdoData_EntireTour);
         }
         {
            /*
             * Radio: Part of a tour
             */
            _rdoData_PartOfATour = new Button(group, SWT.RADIO);
            _rdoData_PartOfATour.setText(Messages.Dialog_ReimportTours_Radio_TourPart);
            _rdoData_PartOfATour.addSelectionListener(_defaultListener);
            gridDataTour.applyTo(_rdoData_PartOfATour);
         }

         createUI_22_PartOfATour(group);

         {
            /*
             * Button: Deselect all
             */
            _btnDeselectAll = new Button(group, SWT.PUSH);
            _btnDeselectAll.setText(Messages.App_Action_DeselectAll);
            _btnDeselectAll.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onDeselectAll_DataItems();
               }
            });

            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .grab(true, false)
                  .indent(0, _pc.convertVerticalDLUsToPixels(4)).applyTo(_btnDeselectAll);
         }
      }
   }

   private void createUI_22_PartOfATour(final Group parent) {

      final GridDataFactory gridDataItem = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      final Composite containerTour = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(16, 8)
            .applyTo(containerTour);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .spacing(32, LayoutConstants.getSpacing().y)
            .applyTo(containerTour);
      {
         {
            /*
             * Calories
             */
            _chkData_Tour_Calories = new Button(containerTour, SWT.CHECK);
            _chkData_Tour_Calories.setText(Messages.Dialog_ModifyTours_Checkbox_Calories);
            _chkData_Tour_Calories.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Tour_Calories);
         }
         {
            /*
             * Import file location
             */
            _chkData_Tour_ImportFileLocation = new Button(containerTour, SWT.CHECK);
            _chkData_Tour_ImportFileLocation.setText(Messages.Dialog_ReimportTours_Checkbox_ImportFileLocation);
            _chkData_Tour_ImportFileLocation.setToolTipText(Messages.Dialog_ReimportTours_Checkbox_ImportFileLocation_Tooltip);
            _chkData_Tour_ImportFileLocation.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Tour_ImportFileLocation);
         }
         {
            /*
             * Tour markers
             */
            _chkData_Tour_Markers = new Button(containerTour, SWT.CHECK);
            _chkData_Tour_Markers.setText(Messages.Dialog_ModifyTours_Checkbox_TourMarkers);
            _chkData_Tour_Markers.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Tour_Markers);
         }
      }

      {
         /*
          * Checkbox: All time slices
          */
         _chkData_AllTimeSlices = new Button(parent, SWT.CHECK);
         _chkData_AllTimeSlices.setText(Messages.Dialog_ReimportTours_Checkbox_TimeSlices);
         _chkData_AllTimeSlices.addSelectionListener(_defaultListener);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .indent(16, 8)
               .applyTo(_chkData_AllTimeSlices);
      }

      final Composite containerTimeSlices = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(32, 8)
            .applyTo(containerTimeSlices);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTimeSlices);
      {

         // row 1
         {
            /*
             * Battery
             */
            _chkData_TimeSlice_Battery = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Battery.setText(Messages.Dialog_ModifyTours_Checkbox_BatteryValues);
            _chkData_TimeSlice_Battery.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Battery);
         }
         {
            /*
             * Running Dynamics
             */
            _chkData_TimeSlice_RunningDynamics = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_RunningDynamics.setText(Messages.Dialog_ModifyTours_Checkbox_RunningDynamicsValues);
            _chkData_TimeSlice_RunningDynamics.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_RunningDynamics);
         }

         // row 2
         {
            /*
             * Cadence
             */
            _chkData_TimeSlice_Cadence = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Cadence.setText(Messages.Dialog_ModifyTours_Checkbox_CadenceValues);
            _chkData_TimeSlice_Cadence.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Cadence);
         }
         {
            /*
             * Swimming
             */
            _chkData_TimeSlice_Swimming = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Swimming.setText(Messages.Dialog_ModifyTours_Checkbox_SwimmingValues);
            _chkData_TimeSlice_Swimming.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Swimming);
         }

         // row 3
         {
            /*
             * Elevation
             */
            _chkData_TimeSlice_Elevation = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Elevation.setText(Messages.Dialog_ModifyTours_Checkbox_ElevationValues);
            _chkData_TimeSlice_Elevation.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Elevation);
         }
         {
            /*
             * Temperature
             */
            _chkData_TimeSlice_Temperature = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Temperature.setText(Messages.Dialog_ModifyTours_Checkbox_TemperatureValues);
            _chkData_TimeSlice_Temperature.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Temperature);
         }

         // row 4
         {
            /*
             * Gear
             */
            _chkData_TimeSlice_Gear = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Gear.setText(Messages.Dialog_ModifyTours_Checkbox_GearValues);
            _chkData_TimeSlice_Gear.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Gear);
         }
         {
            /*
             * Timer pauses
             */
            _chkData_TimeSlice_TourTimerPauses = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_TourTimerPauses.setText(Messages.Dialog_ModifyTours_Checkbox_TourTimerPauses);
            _chkData_TimeSlice_TourTimerPauses.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_TourTimerPauses);
         }

         // row 5
         {
            /*
             * Power And Pulse
             */
            _chkData_TimeSlice_PowerAndPulse = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_PowerAndPulse.setText(Messages.Dialog_ModifyTours_Checkbox_PowerAndPulseValues);
            _chkData_TimeSlice_PowerAndPulse.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_PowerAndPulse);
         }
         {
            /*
             * Training
             */
            _chkData_TimeSlice_Training = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_Training.setText(Messages.Dialog_ModifyTours_Checkbox_TrainingValues);
            _chkData_TimeSlice_Training.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_Training);
         }

         // row 6
         {
            /*
             * Power And Speed
             */
            _chkData_TimeSlice_PowerAndSpeed = new Button(containerTimeSlices, SWT.CHECK);
            _chkData_TimeSlice_PowerAndSpeed.setText(Messages.Dialog_ModifyTours_Checkbox_PowerAndSpeedValues);
            _chkData_TimeSlice_PowerAndSpeed.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TimeSlice_PowerAndSpeed);
         }
      }
   }

   private void createUI_30_Options(final Composite parent) {
      {
         /*
          * Checkbox: Skip tours for which the import file is not found
          */
         _chkSkipTours_With_ImportFile_NotFound = new Button(parent, SWT.CHECK);
         _chkSkipTours_With_ImportFile_NotFound.setText(Messages.Dialog_ReimportTours_Checkbox_SkipToursWithImportFileNotFound);
         GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(_chkSkipTours_With_ImportFile_NotFound);
      }
      {
         /*
          * Checkbox: Log Details
          */
         _chkLogDetails = new Button(parent, SWT.CHECK);
         _chkLogDetails.setText(Messages.Tour_Log_Checkbox_LogDetails);
      }
   }

   /**
    * Start the re-import process
    *
    * @param tourValueTypes
    *           A list of tour values to be re-imported
    */
   private void doReimport(final List<TourValueType> tourValueTypes) {

      /*
       * There maybe too much tour cleanup but it is very complex how all the caches/selection
       * provider work together
       */

      // prevent async error in the save tour method, cleanup environment
      _tourViewer.getPostSelectionProvider().clearSelection();

      Util.clearSelection();

      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, null);
      TourManager.getInstance().clearTourDataCache();

      final boolean isReimport_AllTours = _rdoReimport_Tours_All.getSelection();
      final boolean isReimport_BetweenDates = _rdoReimport_Tours_BetweenDates.getSelection();
      final boolean isSkipToursWithFileNotFound = _chkSkipTours_With_ImportFile_NotFound.getSelection();
      final boolean isLogDetails = _chkLogDetails.getSelection();

      ImportState_Process importState_Process = new ImportState_Process()

            .setIsSkipToursWithFileNotFound(isSkipToursWithFileNotFound);

      if (isLogDetails == false) {

         // ignore detail logging, this can be a performance hog

         importState_Process = importState_Process

               .setIsLog_DEFAULT(false)
               .setIsLog_INFO(false)
               .setIsLog_OK(false);
      }

      if (isReimport_AllTours || isReimport_BetweenDates) {

         doReimport_10_All_OR_BetweenDate_Tours(

               tourValueTypes,
               isReimport_AllTours,
               isReimport_BetweenDates,
               importState_Process);

      } else {

         doReimport_20_SelectedTours(

               tourValueTypes,
               importState_Process);
      }

      importState_Process.runPostProcess();
   }

   /**
    * Re-import ALL tours or BETWEEN tours
    *
    * @param tourValueTypes
    * @param isReimport_AllTours
    * @param isReimport_BetweenDates
    * @param importState_Process
    */
   private void doReimport_10_All_OR_BetweenDate_Tours(final List<TourValueType> tourValueTypes,
                                                       final boolean isReimport_AllTours,
                                                       final boolean isReimport_BetweenDates,
                                                       final ImportState_Process importState_Process) {

      if (isReimport_AllTours) {

         // The user MUST always confirm when the tool is running for ALL tours

         final MessageDialog dialog = new MessageDialog(
               Display.getDefault().getActiveShell(),
               Messages.Dialog_DatabaseAction_Confirmation_Title,
               null,
               Messages.Dialog_DatabaseAction_Confirmation_Message,
               MessageDialog.QUESTION,
               new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
               1 // default index
         );

         if (dialog.open() != Window.OK) {
            return;
         }
      }

      if (RawDataManager.getInstance().actionModifyTourValues_10_Confirm(tourValueTypes, true) == false) {
         return;
      }

      saveState();

      ArrayList<Long> allTourIDs = null;

      if (isReimport_BetweenDates) {

         // get tours between the dates

         final LocalDate dateFrom = LocalDate.of(
               _dtTourDate_From.getYear(),
               _dtTourDate_From.getMonth() + 1,
               _dtTourDate_From.getDay());

         final LocalDate dateUntil = LocalDate.of(
               _dtTourDate_Until.getYear(),
               _dtTourDate_Until.getMonth() + 1,
               _dtTourDate_Until.getDay());

         allTourIDs = TourDatabase.getAllTourIds_BetweenTwoDates(dateFrom, dateUntil);

         if (allTourIDs.isEmpty()) {

            MessageDialog.openInformation(getShell(),
                  Messages.Dialog_ReimportTours_Dialog_Title,
                  Messages.Dialog_ModifyTours_Dialog_ToursAreNotAvailable);

            return;
         }

      } else {

         allTourIDs = TourDatabase.getAllTourIds();
      }

      doReimport_50_TourIds(tourValueTypes, allTourIDs, importState_Process);
   }

   /**
    * @param tourValueTypes
    *           A list of tour values to be re-imported
    * @param importState_Process
    *           Indicates whether to re-import or not a tour for which the file is not found
    */
   private void doReimport_20_SelectedTours(final List<TourValueType> tourValueTypes,
                                            final ImportState_Process importState_Process) {

      final RawDataManager rawDataManager = RawDataManager.getInstance();

      if (rawDataManager.actionModifyTourValues_10_Confirm(tourValueTypes, true) == false) {
         return;
      }

      final Display display = Display.getDefault();
      final Shell activeShell = display.getActiveShell();

      // get selected tour IDs
      final Object[] allSelectedItems = TourManager.getTourViewerSelectedTourIds(_tourViewer);

      if (allSelectedItems == null || allSelectedItems.length == 0) {

         MessageDialog.openInformation(activeShell,
               Messages.Dialog_ReimportTours_Dialog_Title,
               Messages.Dialog_ModifyTours_Dialog_ToursAreNotSelected);

         return;
      }

      /*
       * Convert selection to long
       */
      final ArrayList<Long> allSelectedTourIds = new ArrayList<>();
      for (final Object selectedItem : allSelectedItems) {
         allSelectedTourIds.add((Long) selectedItem);
      }

      doReimport_50_TourIds(
            tourValueTypes,
            allSelectedTourIds,
            importState_Process);
   }

   private void doReimport_50_TourIds(final List<TourValueType> tourValueTypes,
                                      final ArrayList<Long> allTourIDs,
                                      final ImportState_Process importState_Process) {

      final long start = System.currentTimeMillis();

      TourLogManager.showLogView();

      final RawDataManager rawDataManager = RawDataManager.getInstance();

      final int numAllTourIDs = allTourIDs.size();

      /*
       * Setup concurrency
       */
      _reimport_CountDownLatch = new CountDownLatch(numAllTourIDs);
      _reimport_Queue.clear();

      final ReImportStatus reImportStatus = new ReImportStatus();
      final ConcurrentSkipListSet<Long> allReimportedTourIds = new ConcurrentSkipListSet<>();

      final IRunnableWithProgress reImportRunnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(Messages.Import_Data_Dialog_Reimport_Task, numAllTourIDs);

            final long startTime = System.currentTimeMillis();
            long lastUpdateTime = startTime;

            final AtomicInteger numWorkedTours = new AtomicInteger();
            int numLastWorked = 0;

            // loop: all selected tours in the viewer
            for (final long tourId : allTourIDs) {

               if (monitor.isCanceled() || reImportStatus.isCanceled_WholeReimport.get()) {

                  /*
                   * Count down all, that the re-import task can finish but process re-imported
                   * tours
                   */
                  long numCounts = _reimport_CountDownLatch.getCount();
                  while (numCounts-- > 0) {
                     _reimport_CountDownLatch.countDown();
                  }

                  break;
               }

               final long currentTime = System.currentTimeMillis();
               final long timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > 1000) {

                  lastUpdateTime = currentTime;

                  final int numWorked = numWorkedTours.get();

                  // "{0} / {1} - {2} % - {3} Δ"
                  UI.showWorkedInProgressMonitor(monitor, numWorked, numAllTourIDs, numLastWorked);

                  numLastWorked = numWorked;
               }

               doReimport_60_RunConcurrent(
                     tourId,
                     tourValueTypes,
                     allReimportedTourIds,
                     monitor,
                     numWorkedTours,
                     reImportStatus,
                     importState_Process);
            }

            // wait until all re-imports are performed
            _reimport_CountDownLatch.await();
         }
      };

      try {

         new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, reImportRunnable);

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
         Thread.currentThread().interrupt();

      } finally {

         // do post save actions for all re-imported tours, simalar to
         // net.tourbook.database.TourDatabase.saveTour_PostSaveActions(TourData)

         TourDatabase.saveTour_PostSaveActions_Concurrent_2_ForAllTours(
               allReimportedTourIds
                     .stream()
                     .collect(Collectors.toList()));
      }

      if (reImportStatus.isAnyTourReImported.get()) {

         rawDataManager.updateTourData_InImportView_FromDb(null);

         // reselect tours, run in UI thread
         Display.getDefault().asyncExec(_tourViewer::reloadViewer);
      }

      TourLogManager.log_DEFAULT(String.format(
            Messages.Log_Reimport_PreviousFiles_End,
            (System.currentTimeMillis() - start) / 1000.0));

      doReimport_70_FireModifyEvents(importState_Process);
   }

   private void doReimport_60_RunConcurrent(final long tourId,
                                            final List<TourValueType> tourValueTypes,
                                            final ConcurrentSkipListSet<Long> allReimportedTourIds,
                                            final IProgressMonitor monitor,
                                            final AtomicInteger numWorked,
                                            final ReImportStatus reImportStatus,
                                            final ImportState_Process importState_Process) {

      try {

         // put tour ID (queue item) into the queue AND wait when it is full

         _reimport_Queue.put(tourId);

      } catch (final InterruptedException e) {

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _reimport_Executor.submit(() -> {

         try {

            // get last added item
            final Long queueItem_TourId = _reimport_Queue.poll();

            if (queueItem_TourId != null) {

               final TourData oldTourData = TourManager.getTour(queueItem_TourId);

               if (oldTourData != null) {

                  final boolean isReimported = RawDataManager.getInstance().reimportTour(
                        oldTourData,
                        tourValueTypes,
                        reImportStatus,
                        importState_Process);

// this was used for debugging to speedup the process
//
//             final boolean isReimported = true;

                  if (isReimported) {
                     allReimportedTourIds.add(oldTourData.getTourId());
                  }
               }
            }

         } finally {

            monitor.worked(1);
            numWorked.incrementAndGet();

            _reimport_CountDownLatch.countDown();
         }
      });
   }

   private void doReimport_70_FireModifyEvents(final ImportState_Process importState_Process) {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // prevent re-importing in the import view
      RawDataManager.setIsReimportingActive(true);
      {
         // fire unique event for all changes
         TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
      }
      RawDataManager.setIsReimportingActive(false);
   }

   private void enableControls() {

      final boolean isValid = isDataValid();

      final boolean isPartOfATourSelected = _rdoData_PartOfATour.getSelection();
      final boolean isAllTimeSlicesSelected = _chkData_AllTimeSlices.getSelection();
      final boolean isTourBetweenDates = _rdoReimport_Tours_BetweenDates.getSelection();

      final boolean isTimeSlice = isPartOfATourSelected && isAllTimeSlicesSelected == false;

      final boolean isTourSelected = false ||

            _rdoReimport_Tours_All.getSelection() ||
            _rdoReimport_Tours_Selected.getSelection() ||
            isTourBetweenDates;

      final boolean isTimeSliceSelected = false ||

            _chkData_TimeSlice_Battery.getSelection() ||
            _chkData_TimeSlice_Cadence.getSelection() ||
            _chkData_TimeSlice_Elevation.getSelection() ||
            _chkData_TimeSlice_Gear.getSelection() ||
            _chkData_TimeSlice_PowerAndPulse.getSelection() ||
            _chkData_TimeSlice_PowerAndSpeed.getSelection() ||
            _chkData_TimeSlice_RunningDynamics.getSelection() ||
            _chkData_TimeSlice_Swimming.getSelection() ||
            _chkData_TimeSlice_Temperature.getSelection() ||
            _chkData_TimeSlice_Training.getSelection() ||
            _chkData_TimeSlice_TourTimerPauses.getSelection();

      final boolean isTourDataSelected = false ||

            _chkData_Tour_Calories.getSelection() ||
            _chkData_Tour_ImportFileLocation.getSelection() ||
            _chkData_Tour_Markers.getSelection();

      final boolean isDataSelected = false ||

            _rdoData_EntireTour.getSelection() ||
            _chkData_AllTimeSlices.getSelection() ||

            isTourDataSelected ||
            isTimeSliceSelected;

      _chkData_AllTimeSlices.setEnabled(isPartOfATourSelected);

      _chkData_Tour_ImportFileLocation.setEnabled(isPartOfATourSelected);
      _chkData_Tour_Markers.setEnabled(isPartOfATourSelected);
      _chkData_Tour_Calories.setEnabled(isPartOfATourSelected);

      _chkData_TimeSlice_Battery.setEnabled(isTimeSlice);
      _chkData_TimeSlice_Cadence.setEnabled(isTimeSlice);
      _chkData_TimeSlice_Elevation.setEnabled(isTimeSlice);
      _chkData_TimeSlice_Gear.setEnabled(isTimeSlice);
      _chkData_TimeSlice_PowerAndPulse.setEnabled(isTimeSlice);
      _chkData_TimeSlice_PowerAndSpeed.setEnabled(isTimeSlice);
      _chkData_TimeSlice_RunningDynamics.setEnabled(isTimeSlice);
      _chkData_TimeSlice_Swimming.setEnabled(isTimeSlice);
      _chkData_TimeSlice_Temperature.setEnabled(isTimeSlice);
      _chkData_TimeSlice_TourTimerPauses.setEnabled(isTimeSlice);
      _chkData_TimeSlice_Training.setEnabled(isTimeSlice);

      _dtTourDate_From.setEnabled(isTourBetweenDates);
      _dtTourDate_Until.setEnabled(isTourBetweenDates);

      _btnDeselectAll.setEnabled(isPartOfATourSelected
            && (isTimeSliceSelected || isTourDataSelected || isAllTimeSlicesSelected));

      // OK button
      getButton(IDialogConstants.OK_ID).setEnabled(isTourSelected && isDataSelected && isValid);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

      // Helpful during development
//    return null;
   }

   private void initUI() {

      _pc = new PixelConverter(_parent);

      _defaultListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableControls();
         }
      };

      _parent.addDisposeListener(disposeEvent -> {
         _imageLock_Closed.dispose();
         _imageLock_Open.dispose();
      });
   }

   private boolean isDataValid() {

      final LocalDate dtFrom = LocalDate.of(
            _dtTourDate_From.getYear(),
            _dtTourDate_From.getMonth() + 1,
            _dtTourDate_From.getDay());

      final LocalDate dtUntil = LocalDate.of(
            _dtTourDate_Until.getYear(),
            _dtTourDate_Until.getMonth() + 1,
            _dtTourDate_Until.getDay());

      if (dtUntil.toEpochDay() >= dtFrom.toEpochDay()) {

         setErrorMessage(null);
         return true;

      } else {

         setErrorMessage(Messages.Dialog_ModifyTours_Error_2ndDateMustBeLarger);
         return false;
      }
   }

   @Override
   protected void okPressed() {

      //We close the window so the user can see the import progress bar and log view
      _parent.getShell().setVisible(false);

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final List<TourValueType> tourValueTypes = new ArrayList<>();

         if (_rdoData_EntireTour.getSelection()) {

            tourValueTypes.add(TourValueType.ENTIRE_TOUR);

         } else {

            if (_chkData_AllTimeSlices.getSelection()) {

               tourValueTypes.add(TourValueType.ALL_TIME_SLICES);

            } else {

// SET_FORMATTING_OFF

               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Battery,           TourValueType.TIME_SLICES__BATTERY,          tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Cadence,           TourValueType.TIME_SLICES__CADENCE,          tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Elevation,         TourValueType.TIME_SLICES__ELEVATION,        tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Gear,              TourValueType.TIME_SLICES__GEAR,             tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_PowerAndPulse,     TourValueType.TIME_SLICES__POWER_AND_PULSE,  tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_PowerAndSpeed,     TourValueType.TIME_SLICES__POWER_AND_SPEED,  tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_RunningDynamics,   TourValueType.TIME_SLICES__RUNNING_DYNAMICS, tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Swimming,          TourValueType.TIME_SLICES__SWIMMING,         tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Temperature,       TourValueType.TIME_SLICES__TEMPERATURE,      tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_TourTimerPauses,   TourValueType.TIME_SLICES__TIMER_PAUSES,     tourValueTypes);
               DialogUtils.addTourValueTypeFromCheckbox(_chkData_TimeSlice_Training,          TourValueType.TIME_SLICES__TRAINING,         tourValueTypes);
            }

            DialogUtils.addTourValueTypeFromCheckbox(_chkData_Tour_Calories,           TourValueType.TOUR__CALORIES,                tourValueTypes);
            DialogUtils.addTourValueTypeFromCheckbox(_chkData_Tour_Markers,            TourValueType.TOUR__MARKER,                  tourValueTypes);
            DialogUtils.addTourValueTypeFromCheckbox(_chkData_Tour_ImportFileLocation, TourValueType.TOUR__IMPORT_FILE_LOCATION,    tourValueTypes);

// SET_FORMATTING_ON
         }

         doReimport(tourValueTypes);
      });

      super.okPressed();
   }

   private void onDeselectAll_DataItems() {

      _chkData_AllTimeSlices.setSelection(false);
      _chkData_TimeSlice_Elevation.setSelection(false);

      _chkData_Tour_Calories.setSelection(false);
      _chkData_Tour_ImportFileLocation.setSelection(false);
      _chkData_Tour_Markers.setSelection(false);

      _chkData_TimeSlice_Battery.setSelection(false);
      _chkData_TimeSlice_Cadence.setSelection(false);
      _chkData_TimeSlice_Gear.setSelection(false);
      _chkData_TimeSlice_PowerAndPulse.setSelection(false);
      _chkData_TimeSlice_PowerAndSpeed.setSelection(false);
      _chkData_TimeSlice_RunningDynamics.setSelection(false);
      _chkData_TimeSlice_Swimming.setSelection(false);
      _chkData_TimeSlice_Temperature.setSelection(false);
      _chkData_TimeSlice_TourTimerPauses.setSelection(false);
      _chkData_TimeSlice_Training.setSelection(false);

      enableControls();
   }

   private void onSelect_Unlock_AllTours() {

      // toggle radio
      _rdoReimport_Tours_All.setEnabled(!_rdoReimport_Tours_All.isEnabled());

      final boolean isEnabled = _rdoReimport_Tours_All.isEnabled();

      _btnUnlockAllToursSelection.setText(isEnabled
            ? Messages.Dialog_ModifyTours_Button_LockMultipleToursSelection_Text
            : Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);

      _btnUnlockAllToursSelection.setImage(isEnabled
            ? _imageLock_Open
            : _imageLock_Closed);

      if (!isEnabled) {
         _rdoReimport_Tours_All.setSelection(false);
         _rdoReimport_Tours_Selected.setSelection(true);
      }

      updateUI_LockUnlockButtons();
   }

   private void onSelect_Unlock_BetweenDates() {

      // toggle radio
      _rdoReimport_Tours_BetweenDates.setEnabled(!_rdoReimport_Tours_BetweenDates.isEnabled());

      final boolean isEnabled = _rdoReimport_Tours_BetweenDates.isEnabled();

      _dtTourDate_From.setEnabled(isEnabled);
      _dtTourDate_Until.setEnabled(isEnabled);

      _btnUnlockBetweenDatesSelection.setText(isEnabled
            ? Messages.Dialog_ModifyTours_Button_LockMultipleToursSelection_Text
            : Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);
      _btnUnlockBetweenDatesSelection.setImage(isEnabled
            ? _imageLock_Open
            : _imageLock_Closed);

      if (!isEnabled) {
         _rdoReimport_Tours_BetweenDates.setSelection(false);
         _rdoReimport_Tours_Selected.setSelection(true);
      }

      updateUI_LockUnlockButtons();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      // Tours to re-import
      _rdoReimport_Tours_All              .setSelection(_state.getBoolean(STATE_REIMPORT_TOURS_ALL));
      _rdoReimport_Tours_BetweenDates     .setSelection(_state.getBoolean(STATE_REIMPORT_TOURS_BETWEEN_DATES));
      _rdoReimport_Tours_Selected         .setSelection(_state.getBoolean(STATE_REIMPORT_TOURS_SELECTED));

      Util.getStateDate(_state, STATE_REIMPORT_TOURS_BETWEEN_DATES_FROM, LocalDate.now(), _dtTourDate_From);
      Util.getStateDate(_state, STATE_REIMPORT_TOURS_BETWEEN_DATES_UNTIL, LocalDate.now(), _dtTourDate_Until);

      // Data to re-import
      final boolean isReimportEntireTour = _state.getBoolean(STATE_IS_IMPORT_ENTIRE_TOUR);
      _chkData_AllTimeSlices              .setSelection(_state.getBoolean(STATE_IS_IMPORT_ALL_TIME_SLICES));
      _rdoData_EntireTour                 .setSelection(isReimportEntireTour);
      _rdoData_PartOfATour                .setSelection(isReimportEntireTour == false);

      _chkData_Tour_Calories              .setSelection(_state.getBoolean(STATE_IS_IMPORT_TOUR__CALORIES));
      _chkData_Tour_ImportFileLocation    .setSelection(_state.getBoolean(STATE_IS_IMPORT_TOUR__FILE_LOCATION));
      _chkData_Tour_Markers               .setSelection(_state.getBoolean(STATE_IS_IMPORT_TOUR__MARKERS));

      _chkData_TimeSlice_Battery          .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__BATTERY));
      _chkData_TimeSlice_Cadence          .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__CADENCE));
      _chkData_TimeSlice_Elevation        .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__ELEVATION));
      _chkData_TimeSlice_Gear             .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__GEAR));
      _chkData_TimeSlice_PowerAndPulse    .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__POWER_AND_PULSE));
      _chkData_TimeSlice_PowerAndSpeed    .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__POWER_AND_SPEED));
      _chkData_TimeSlice_RunningDynamics  .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__RUNNING_DYNAMICS));
      _chkData_TimeSlice_Swimming         .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__SWIMMING));
      _chkData_TimeSlice_Temperature      .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__TEMPERATURE));
      _chkData_TimeSlice_Training         .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__TRAINING));
      _chkData_TimeSlice_TourTimerPauses  .setSelection(_state.getBoolean(STATE_IS_IMPORT_TIME_SLICE__TIMER_PAUSES));

      // Skip tours for which the import file is not found
      _chkSkipTours_With_ImportFile_NotFound .setSelection(_state.getBoolean(STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND));
      _chkLogDetails                         .setSelection(_state.getBoolean(STATE_IS_LOG_DETAILS));

      enableControls();

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

      // Tours to re-import
      _state.put(STATE_REIMPORT_TOURS_ALL,                     _rdoReimport_Tours_All.getSelection());
      _state.put(STATE_REIMPORT_TOURS_BETWEEN_DATES,           _rdoReimport_Tours_BetweenDates.getSelection());
      _state.put(STATE_REIMPORT_TOURS_SELECTED,                _rdoReimport_Tours_Selected.getSelection());

      Util.setStateDate(_state, STATE_REIMPORT_TOURS_BETWEEN_DATES_FROM,   _dtTourDate_From);
      Util.setStateDate(_state, STATE_REIMPORT_TOURS_BETWEEN_DATES_UNTIL,  _dtTourDate_Until);

      // Data to re-import
      _state.put(STATE_IS_IMPORT_ENTIRE_TOUR,                  _rdoData_EntireTour.getSelection());

      _state.put(STATE_IS_IMPORT_TOUR__CALORIES,               _chkData_Tour_Calories.getSelection());
      _state.put(STATE_IS_IMPORT_TOUR__FILE_LOCATION,          _chkData_Tour_ImportFileLocation.getSelection());
      _state.put(STATE_IS_IMPORT_TOUR__MARKERS,                _chkData_Tour_Markers.getSelection());

      _state.put(STATE_IS_IMPORT_ALL_TIME_SLICES,              _chkData_AllTimeSlices.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__BATTERY,          _chkData_TimeSlice_Battery.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__CADENCE,          _chkData_TimeSlice_Cadence.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__ELEVATION,        _chkData_TimeSlice_Elevation.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__GEAR,             _chkData_TimeSlice_Gear.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__POWER_AND_PULSE,  _chkData_TimeSlice_PowerAndPulse.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__POWER_AND_SPEED,  _chkData_TimeSlice_PowerAndSpeed.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__RUNNING_DYNAMICS, _chkData_TimeSlice_RunningDynamics.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__SWIMMING,         _chkData_TimeSlice_Swimming.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__TEMPERATURE,      _chkData_TimeSlice_Temperature.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__TIMER_PAUSES,     _chkData_TimeSlice_TourTimerPauses.getSelection());
      _state.put(STATE_IS_IMPORT_TIME_SLICE__TRAINING,         _chkData_TimeSlice_Training.getSelection());

      // Skip tours for which the import file is not found
      _state.put(STATE_IS_SKIP_TOURS_WITH_IMPORTFILE_NOTFOUND, _chkSkipTours_With_ImportFile_NotFound.getSelection());
      _state.put(STATE_IS_LOG_DETAILS,                         _chkLogDetails.getSelection());

// SET_FORMATTING_ON
   }

   /**
    * The relayout is needed because when setting a text for a button to "Lock" or "Unlock" an the
    * initial text was "Lock", when setting "Unlock", it will be truncate.
    */
   private void updateUI_LockUnlockButtons() {

      final boolean isDarkTheme = net.tourbook.common.UI.isDarkTheme();

      // get default foreground color
      final Color unlockColor = _parent.getForeground();
      final Color lockColor = isDarkTheme ? DialogUtils.LOCK_COLOR_DARK : DialogUtils.LOCK_COLOR_LIGHT;

      _btnUnlockAllToursSelection.setForeground(_rdoReimport_Tours_All.isEnabled()
            ? unlockColor
            : lockColor);

      _btnUnlockBetweenDatesSelection.setForeground(_rdoReimport_Tours_BetweenDates.isEnabled()
            ? unlockColor
            : lockColor);

      // ensure the modified text is fully visible
      _groupTours.layout(true, true);
   }
}
