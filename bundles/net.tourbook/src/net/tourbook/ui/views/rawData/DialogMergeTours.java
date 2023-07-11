/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

public class DialogMergeTours extends TitleAreaDialog implements ITourProvider2, I2ndAltiLayer {

   private static final int              MAX_ADJUST_SECONDS     = 120;
   private static final int              MAX_ADJUST_MINUTES     = 120;                                                                      // x 60
   private static final int              MAX_ADJUST_ALTITUDE_1  = 20;
   private static final int              MAX_ADJUST_ALTITUDE_10 = 40;                                                                       // x 10

   private static final int              VH_SPACING             = 2;

   private static final IPreferenceStore _prefStore             = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                 = TourbookPlugin.getState("net.tourbook.ui.views.rawData.DialogMergeTours");//$NON-NLS-1$

   private TourData                      _sourceTour;
   private TourData                      _targetTour;

   private Composite                     _dlgContainer;

   private TourChart                     _tourChart;
   private TourChartConfiguration        _tourChartConfig;

   private PixelConverter                _pc;
   private boolean                       _isInUIInit;

   /*
    * vertical adjustment options
    */
   private Group _groupAltitude;

   private Label _lblAltitudeDiff1;
   private Label _lblAltitudeDiff10;

   private Scale _scaleAltitude1;
   private Scale _scaleAltitude10;

   /*
    * horizontal adjustment options
    */
   private Button _chkSynchStartTime;

   private Label  _lblAdjustMinuteValue;
   private Label  _lblAdjustSecondsValue;

   private Scale  _scaleAdjustMinutes;
   private Scale  _scaleAdjustSeconds;

   private Label  _lblAdjustMinuteUnit;
   private Label  _lblAdjustSecondsUnit;

   private Button _btnResetAdjustment;
   private Button _btnResetValues;

   /*
    * save actions
    */
   private Button _chkMergeAltitude;
   private Button _chkMergeCadence;
   private Button _chkMergePulse;
   private Button _chkMergeSpeed;
   private Button _chkMergeTemperature;

   private Button _chkAdjustAltiFromSource;
   private Button _chkAdjustAltiSmoothly;

   private Button _chkAdjustAltiFromStart;
   private Label  _lblAdjustAltiValueTimeUnit;
   private Label  _lblAdjustAltiValueDistanceUnit;
   private Label  _lblAdjustAltiValueTime;
   private Label  _lblAdjustAltiValueDistance;

   private Button _chkSetTourType;
   private Link   _linkTourType;
   private CLabel _lblTourType;

   private Button _chkKeepHVAdjustments;

   /*
    * display actions
    */
   private Button  _chkValueDiffScaling;

   private Button  _chkPreviewChart;

   private boolean _isTourSaved = false;
   private boolean _isMergeSourceTourModified;
   private boolean _isChartUpdated;

   /*
    * backup data
    */
   private int[]                         _backupSourceTimeSerie;
   private float[]                       _backupSourceDistanceSerie;
   private float[]                       _backupSourceAltitudeSerie;

   private TourType                      _backupSourceTourType;

   private float[]                       _backupTargetAltitudeSerie;
   private float[]                       _backupTargetCadenceSerie;
   private float[]                       _backupTargetPulseSerie;
   private float[]                       _backupTargetTemperatureSerie;
   private float[]                       _backupTargetSpeedSerie;
   private int[]                         _backupTargetTimeSerie;
   private long                          _backupTargetDeviceTime_Elapsed;

   private int                           _backupTargetTimeOffset;
   private int                           _backupTargetAltitudeOffset;

   private ActionOpenPrefDialog          _actionOpenTourTypePrefs;

   private final NumberFormat            _nf          = NumberFormat.getNumberInstance();

   private final Image                   _iconPlaceholder;
   private final HashMap<Integer, Image> _graphImages = new HashMap<>();

   private final int                     _tourStartTimeSynchOffset;
   private int                           _tourTimeOffsetBackup;

   private boolean                       _isAdjustAltiFromSourceBackup;
   private boolean                       _isAdjustAltiFromStartBackup;

   private IPropertyChangeListener       _prefChangeListener;

   /**
    * @param parentShell
    * @param mergeSourceTour
    *           {@link TourData} for the tour which is merge into the other tour
    * @param mergeTargetTour
    *           {@link TourData} for the tour into which the other tour is merged
    */
   public DialogMergeTours(final Shell parentShell, final TourData mergeSourceTour, final TourData mergeTargetTour) {

      super(parentShell);

      // make dialog resizable and display maximize button
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      // set icon for the window
      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.MergeTours).createImage());

      _iconPlaceholder = TourbookPlugin.getImageDescriptor(Images.App_EmptyIcon_Placeholder).createImage();

      _sourceTour = mergeSourceTour;
      _targetTour = mergeTargetTour;

      /*
       * synchronize start time
       */

      final long sourceStartTime = _sourceTour.getTourStartTimeMS();
      final long targetStartTime = _targetTour.getTourStartTimeMS();

      _tourStartTimeSynchOffset = (int) ((sourceStartTime - targetStartTime) / 1000);

      _nf.setMinimumFractionDigits(3);
      _nf.setMaximumFractionDigits(3);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            /*
             * tour data cache is cleared, reload tour data from the database
             */

            if (_sourceTour.getTourPerson() != null) {
               _sourceTour = TourManager.getInstance().getTourData(_sourceTour.getTourId());
            }

            _targetTour = TourManager.getInstance().getTourData(_targetTour.getTourId());

            onModifyProperties();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addVisibleGraph(final int graphId) {

      final ArrayList<Integer> visibleGraphs = _tourChartConfig.getVisibleGraphs();

      if (visibleGraphs.contains(graphId) == false) {
         visibleGraphs.add(graphId);
      }
   }

   /**
    * Set button width
    */
   private void adjustButtonWidth() {

      final int btnResetAdj = _btnResetAdjustment.getBounds().width;
      final int btnResetValues = _btnResetValues.getBounds().width;
      final int newWidth = Math.max(btnResetAdj, btnResetValues);

      GridData gridData;
      gridData = (GridData) _btnResetAdjustment.getLayoutData();
      gridData.widthHint = newWidth;

      gridData = (GridData) _btnResetValues.getLayoutData();
      gridData.widthHint = newWidth;
   }

   private void assignMergedSeries(final TourMerger tourMerger) {

      final boolean isSourceAltitude = _sourceTour.altitudeSerie != null;
      final boolean isTargetAltitude = _targetTour.altitudeSerie != null;

      _sourceTour.dataSerieAdjustedAlti = null;

      final float[] newSourceAltitudeSerie = tourMerger.getNewSourceAltitudeSerie();
      if (isSourceAltitude) {
         _sourceTour.dataSerie2ndAlti = newSourceAltitudeSerie;
      } else {
         _sourceTour.dataSerie2ndAlti = null;
      }

      if (isSourceAltitude && isTargetAltitude) {
         _sourceTour.dataSerieDiffTo2ndAlti = tourMerger.getNewSourceAltitudeDifferencesSerie();
      } else {
         _sourceTour.dataSerieDiffTo2ndAlti = null;
      }

      if (_chkMergeCadence.getSelection()) {
         _targetTour.setCadenceSerie(tourMerger.getNewTargetCadenceSerie());
      } else {
         _targetTour.setCadenceSerie(_backupTargetCadenceSerie);
      }

      if (_chkMergePulse.getSelection()) {
         _targetTour.pulseSerie = tourMerger.getNewTargetPulseSerie();
      } else {
         _targetTour.pulseSerie = _backupTargetPulseSerie;
      }

      if (_chkMergeTemperature.getSelection()) {
         _targetTour.temperatureSerie = tourMerger.getNewTargetTemperatureSerie();
      } else {
         _targetTour.temperatureSerie = _backupTargetTemperatureSerie;
      }

      //In both cases, we update the speed to trigger the recalculation of it
      if (_chkMergeSpeed.getSelection()) {
         final int[] newTargetTimeSerie = tourMerger.getNewTargetTimeSerie();
         _targetTour.timeSerie = tourMerger.getNewTargetTimeSerie();
         _targetTour.setSpeedSerie(null);
         _targetTour.setTourDeviceTime_Elapsed(newTargetTimeSerie[newTargetTimeSerie.length - 1] - newTargetTimeSerie[0] * 1L);
         _targetTour.setTourDeviceTime_Recorded(_targetTour.getTourDeviceTime_Elapsed());
         _targetTour.computeTourMovingTime();
      } else {
         _targetTour.timeSerie = _backupTargetTimeSerie;
         _targetTour.setSpeedSerie(_backupTargetSpeedSerie);
         _targetTour.setTourDeviceTime_Elapsed(_backupTargetDeviceTime_Elapsed);
         _targetTour.setTourDeviceTime_Recorded(_targetTour.getTourDeviceTime_Elapsed());
         _targetTour.computeTourMovingTime();
      }
   }

   @Override
   public boolean close() {

      saveState();

      if (_isTourSaved == false) {

         // tour is not saved, dialog is canceled, restore original values

         restoreDataBackup();
      }

      /**
       * this is a tricky thing:
       * <p>
       * when the tour is not saved, the tour must be reverted
       * <p>
       * when the tour is saved, reverting the tour sets the editor to not dirty, tour data have
       * already been saved
       */
      if (_isMergeSourceTourModified) {

         // revert modified tour type in the merge from tour

         final TourEvent tourEvent = new TourEvent(_sourceTour);
         tourEvent.isReverted = true;

         TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);
      }

      return super.close();
   }

   private float[] computeAdjustedAltitude(final TourMerger tourMerger) {

      final float[] targetDistanceSerie = _targetTour.distanceSerie;

      final boolean isSourceAltitude = _sourceTour.altitudeSerie != null;
      final boolean isTargetAltitude = _targetTour.altitudeSerie != null;
      final boolean isTargetDistance = targetDistanceSerie != null;

      final float[] altitudeDifferences = new float[2];

      if (!isSourceAltitude || !isTargetAltitude || !isTargetDistance) {
         return altitudeDifferences;
      }

      final int[] targetTimeSerie = tourMerger.getNewTargetTimeSerie();
      final int serieLength = targetTimeSerie.length;
      final float[] newSourceAltitudeDifferencesSerie = tourMerger.getNewSourceAltitudeDifferencesSerie();
      final float[] newSourceAltitudeSerie = tourMerger.getNewSourceAltitudeSerie();
      if (_chkAdjustAltiFromStart.getSelection()) {

         /*
          * adjust start altitude until left slider
          */

         final float[] adjustedTargetAltitudeSerie = new float[serieLength];

         float startAltitudeDifferences = newSourceAltitudeDifferencesSerie[0];
         final int endIndex = _tourChart.getXSliderPosition().getLeftSliderValueIndex();
         final float distanceDiff = targetDistanceSerie[endIndex];

         final float[] altitudeSerie = _targetTour.altitudeSerie;

         for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

            if (serieIndex < endIndex) {

               // add adjusted altitude

               final float targetDistance = targetDistanceSerie[serieIndex];
               final float distanceScale = 1 - targetDistance / distanceDiff;

               final float adjustedAltiDiff = startAltitudeDifferences * distanceScale;
               final float newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

               adjustedTargetAltitudeSerie[serieIndex] = newAltitude;
               newSourceAltitudeDifferencesSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;

            } else {

               // add altitude which are not adjusted

               adjustedTargetAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
            }
         }

         _sourceTour.dataSerieAdjustedAlti = adjustedTargetAltitudeSerie;

         startAltitudeDifferences /= UI.UNIT_VALUE_ELEVATION;

         final int targetEndTime = targetTimeSerie[endIndex];
         final float targetEndDistance = targetDistanceSerie[endIndex];

         // meter/min
         altitudeDifferences[0] = targetEndTime == 0 ? //
               0f
               : startAltitudeDifferences / targetEndTime * 60;

         // meter/meter
         altitudeDifferences[1] = targetEndDistance == 0 ? //
               0f
               : ((startAltitudeDifferences * 1000) / targetEndDistance) / UI.UNIT_VALUE_DISTANCE;

      } else if (_chkAdjustAltiFromSource.getSelection()) {

         /*
          * adjust target altitude from source altitude
          */
         _sourceTour.dataSerieAdjustedAlti = Arrays.copyOf(newSourceAltitudeSerie, serieLength);
      }

      return altitudeDifferences;
   }

   private void computeMergedData() {

      final TourMerger tourMerger = new TourMerger(
            _sourceTour,
            _targetTour,
            _chkAdjustAltiFromSource.getSelection(),
            _chkAdjustAltiSmoothly.getSelection(),
            _chkSynchStartTime.getSelection(),
            _tourStartTimeSynchOffset);

      tourMerger.computeMergedData(_chkMergeSpeed.getSelection());

      assignMergedSeries(tourMerger);

      final float[] altitudeDifferences = computeAdjustedAltitude(tourMerger);

      updateUI(altitudeDifferences);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.tour_merger_dialog_title);

      shell.addDisposeListener(disposeEvent -> onDispose());
   }

   @Override
   public void create() {

      createDataBackup();
      addPrefListener();

      // create UI widgets
      super.create();

      adjustButtonWidth();
      _dlgContainer.layout(true, true);

      createActions();
      _isInUIInit = true;
      {
         restoreState();
      }
      _isInUIInit = false;

      setTitle(NLS.bind(
            Messages.tour_merger_dialog_header_title,
            TourManager.getTourTitle(_targetTour),
            _targetTour.getDeviceName()));

      setMessage(NLS.bind(
            Messages.tour_merger_dialog_header_message,
            TourManager.getTourTitle(_sourceTour),
            _sourceTour.getDeviceName()));

      // must be done before the merged data are computed
      enableGraphActions();
      setMergedGraphsVisible();

      // set alti diff scaling
      _tourChartConfig.isRelativeValueDiffScaling = _chkValueDiffScaling.getSelection();
      _tourChart.updateLayer2ndAlti(this, true);

      updateUIFromTourData();

      if (_chkSynchStartTime.getSelection()) {
         _tourTimeOffsetBackup = _targetTour.getMergedTourTimeOffset();
         updateUITourTimeOffset(_tourStartTimeSynchOffset);
      }

      // update chart after the UI is updated from the tour
      updateTourChart();

      enableActions();

      validateFields();
   }

   @Override
   public ChartLayer2ndAltiSerie create2ndAltiLayer() {

      if (_targetTour == null) {
         return null;
      }

      final TourData mergeSourceTourData = _targetTour.getMergeSourceTourData();

      if (_targetTour.getMergeSourceTourId() == null && mergeSourceTourData == null) {
         return null;
      }

      TourData layerTourData;

      if (mergeSourceTourData != null) {
         layerTourData = mergeSourceTourData;
      } else {
         layerTourData = TourManager.getInstance().getTourData(_targetTour.getMergeSourceTourId());
      }

      if (layerTourData == null) {
         return null;
      }

      final double[] xDataSerie = _tourChartConfig.isShowTimeOnXAxis ? //
            _targetTour.getTimeSerieDouble()
            : _targetTour.getDistanceSerieDouble();

      return new ChartLayer2ndAltiSerie(layerTourData, xDataSerie, _tourChartConfig, null);
   }

   private void createActions() {

      _actionOpenTourTypePrefs = new ActionOpenPrefDialog(
            Messages.action_tourType_modify_tourTypes,
            ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // rename OK button
      final Button buttonOK = getButton(IDialogConstants.OK_ID);
      buttonOK.setText(Messages.tour_merger_save_target_tour);

      setButtonLayoutData(buttonOK);
   }

   private void createDataBackup() {

      /*
       * keep a backup of the altitude data because these data will be changed in this dialog
       */
      _backupSourceTimeSerie = Util.createIntegerCopy(_sourceTour.timeSerie);
      _backupSourceDistanceSerie = Util.createFloatCopy(_sourceTour.distanceSerie);
      _backupSourceAltitudeSerie = Util.createFloatCopy(_sourceTour.altitudeSerie);
      _backupSourceTourType = _sourceTour.getTourType();

      _backupTargetAltitudeSerie = Util.createFloatCopy(_targetTour.altitudeSerie);
      _backupTargetCadenceSerie = Util.createFloatCopy(_targetTour.getCadenceSerie());
      _backupTargetPulseSerie = Util.createFloatCopy(_targetTour.pulseSerie);
      _backupTargetTemperatureSerie = Util.createFloatCopy(_targetTour.temperatureSerie);
      _backupTargetSpeedSerie = Util.createFloatCopy(_targetTour.getSpeedSerie());
      _backupTargetTimeSerie = Util.createIntegerCopy(_targetTour.timeSerie);
      _backupTargetDeviceTime_Elapsed = _targetTour.getTourDeviceTime_Elapsed();

      _backupTargetTimeOffset = _targetTour.getMergedTourTimeOffset();
      _backupTargetAltitudeOffset = _targetTour.getMergedAltitudeOffset();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite dlgContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
      GridLayoutFactory.fillDefaults().margins(10, 0).numColumns(1).applyTo(dlgContainer);

      createUITourChart(dlgContainer);

      // column container
      final Composite columnContainer = new Composite(dlgContainer, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(columnContainer);
      GridLayoutFactory.fillDefaults().numColumns(3).spacing(10, 0).applyTo(columnContainer);

      /*
       * column: options
       */
      final Composite columnOptions = new Composite(columnContainer, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(columnOptions);
      GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(columnOptions);

      createUISectionSaveActions(columnOptions);

      /*
       * column: display/adjustments
       */
      final Composite columnDisplay = new Composite(columnContainer, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(columnDisplay);
      GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(columnDisplay);

      createUISectionAdjustments(columnDisplay);

      createUISectionResetButtons(columnContainer);
   }

   /**
    * group: adjust time
    */
   private void createUIGroupHorizontalAdjustment(final Composite parent) {

      final int valueWidth = _pc.convertWidthInCharsToPixels(4);
      Label label;

      final Group groupTime = new Group(parent, SWT.NONE);
      groupTime.setText(Messages.tour_merger_group_adjust_time);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(groupTime);
      GridLayoutFactory.swtDefaults()//
            .numColumns(1)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(groupTime);

      /*
       * Checkbox: keep horizontal and vertical adjustments
       */
      _chkSynchStartTime = new Button(groupTime, SWT.CHECK);
      GridDataFactory.fillDefaults().applyTo(_chkSynchStartTime);
      _chkSynchStartTime.setText(Messages.tour_merger_chk_use_synced_start_time);
      _chkSynchStartTime.setToolTipText(Messages.tour_merger_chk_use_synced_start_time_tooltip);
      _chkSynchStartTime.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

         // display synched time in the UI
         if (_chkSynchStartTime.getSelection()) {

            // set time offset for the synched tours

            _tourTimeOffsetBackup = getFromUITourTimeOffset();
            updateUITourTimeOffset(_tourStartTimeSynchOffset);

         } else {

            // set time offset manually

            updateUITourTimeOffset(_tourTimeOffsetBackup);
         }

         onModifyProperties();

         if (_chkPreviewChart.getSelection() == false) {

            // preview
            updateTourChart();
         }
      }));

      /*
       * container: seconds scale
       */
      final Composite timeContainer = new Composite(groupTime, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(timeContainer);
      GridLayoutFactory.fillDefaults().numColumns(4).spacing(0, 0).applyTo(timeContainer);

      /*
       * scale: adjust seconds
       */
      _lblAdjustSecondsValue = new Label(timeContainer, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(valueWidth, SWT.DEFAULT)
            .applyTo(_lblAdjustSecondsValue);

      label = new Label(timeContainer, SWT.NONE);
      label.setText(UI.SPACE1);

      _lblAdjustSecondsUnit = new Label(timeContainer, SWT.NONE);
      _lblAdjustSecondsUnit.setText(Messages.tour_merger_label_adjust_seconds);

      _scaleAdjustSeconds = new Scale(timeContainer, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAdjustSeconds);
      _scaleAdjustSeconds.setMinimum(0);
      _scaleAdjustSeconds.setMaximum(MAX_ADJUST_SECONDS * 2);
      _scaleAdjustSeconds.setPageIncrement(20);
      _scaleAdjustSeconds.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModifyProperties()));
      _scaleAdjustSeconds.addListener(SWT.MouseDoubleClick, event -> onScaleDoubleClick(event.widget));

      /*
       * scale: adjust minutes
       */
      _lblAdjustMinuteValue = new Label(timeContainer, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(valueWidth, SWT.DEFAULT)
            .applyTo(_lblAdjustMinuteValue);

      label = new Label(timeContainer, SWT.NONE);
      label.setText(UI.SPACE1);

      _lblAdjustMinuteUnit = new Label(timeContainer, SWT.NONE);
      _lblAdjustMinuteUnit.setText(Messages.tour_merger_label_adjust_minutes);

      _scaleAdjustMinutes = new Scale(timeContainer, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAdjustMinutes);
      _scaleAdjustMinutes.setMinimum(0);
      _scaleAdjustMinutes.setMaximum(MAX_ADJUST_MINUTES * 2);
      _scaleAdjustMinutes.setPageIncrement(20);
      _scaleAdjustMinutes.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModifyProperties()));
      _scaleAdjustMinutes.addListener(SWT.MouseDoubleClick, event -> onScaleDoubleClick(event.widget));
   }

   /**
    * group: adjust altitude
    */
   private void createUIGroupVerticalAdjustment(final Composite parent) {

      _groupAltitude = new Group(parent, SWT.NONE);
      _groupAltitude.setText(Messages.tour_merger_group_adjust_altitude);
      GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(_groupAltitude);
      GridLayoutFactory.swtDefaults().numColumns(4)
//				.extendedMargins(0, 0, 0, 0)
//				.spacing(0, 0)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(_groupAltitude);

      /*
       * scale: altitude 20m
       */
      _lblAltitudeDiff1 = new Label(_groupAltitude, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
            .applyTo(_lblAltitudeDiff1);

      _scaleAltitude1 = new Scale(_groupAltitude, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAltitude1);
      _scaleAltitude1.setMinimum(0);
      _scaleAltitude1.setMaximum(MAX_ADJUST_ALTITUDE_1 * 2);
      _scaleAltitude1.setPageIncrement(5);
      _scaleAltitude1.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModifyProperties()));
      _scaleAltitude1.addListener(SWT.MouseDoubleClick, event -> onScaleDoubleClick(event.widget));

      /*
       * scale: altitude 100m
       */
      _lblAltitudeDiff10 = new Label(_groupAltitude, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
            .applyTo(_lblAltitudeDiff10);

      _scaleAltitude10 = new Scale(_groupAltitude, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAltitude10);
      _scaleAltitude10.setMinimum(0);
      _scaleAltitude10.setMaximum(MAX_ADJUST_ALTITUDE_10 * 2);
      _scaleAltitude10.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModifyProperties()));
      _scaleAltitude10.addListener(SWT.MouseDoubleClick, event -> onScaleDoubleClick(event.widget));
   }

   private Button createUIMergeAction(final Composite parent,
                                      final int graphId,
                                      final String btnText,
                                      final String btnTooltip,
                                      final String imageEnabled,
                                      final boolean isEnabled) {

      final Button mergeButton = new Button(parent, SWT.CHECK);
      mergeButton.setText(btnText);
      mergeButton.setToolTipText(btnTooltip);

      mergeButton.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
         validateFields();
         onSelectMergeGraph(selectionEvent);
      }));

      if (isEnabled) {
         final Image image = TourbookPlugin.getImageDescriptor(imageEnabled).createImage();
         _graphImages.put(graphId, image);
         mergeButton.setImage(image);
      } else {
         mergeButton.setImage(_iconPlaceholder);
      }

      return mergeButton;
   }

   private void createUISectionAdjustments(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

      createUIGroupHorizontalAdjustment(container);
      createUIGroupVerticalAdjustment(container);
      createUISectionDisplayOptions(container);
   }

   private void createUISectionDisplayOptions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(container);
      GridLayoutFactory.fillDefaults()//
            .numColumns(1)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(container);

      /*
       * checkbox: display relative or absolute scale
       */
      _chkValueDiffScaling = new Button(container, SWT.CHECK);
      GridDataFactory.swtDefaults()/* .indent(5, 5) .span(4, 1) */.applyTo(_chkValueDiffScaling);
      _chkValueDiffScaling.setText(Messages.tour_merger_chk_alti_diff_scaling);
      _chkValueDiffScaling.setToolTipText(Messages.tour_merger_chk_alti_diff_scaling_tooltip);
      _chkValueDiffScaling.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModifyProperties()));

      /*
       * checkbox: preview chart
       */
      _chkPreviewChart = new Button(container, SWT.CHECK);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(_chkPreviewChart);
      _chkPreviewChart.setText(Messages.tour_merger_chk_preview_graphs);
      _chkPreviewChart.setToolTipText(Messages.tour_merger_chk_preview_graphs_tooltip);
      _chkPreviewChart.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

         if (_chkPreviewChart.getSelection()) {

            setMergedGraphsVisible();

            onModifyProperties();
            updateTourChart();
         }
      }));
   }

   private void createUISectionResetButtons(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).align(SWT.END, SWT.FILL).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

      /*
       * button: reset all adjustment options
       */
      _btnResetAdjustment = new Button(container, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(_btnResetAdjustment);
      _btnResetAdjustment.setText(Messages.tour_merger_btn_reset_adjustment);
      _btnResetAdjustment.setToolTipText(Messages.tour_merger_btn_reset_adjustment_tooltip);
      _btnResetAdjustment.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectResetAdjustments()));

      /*
       * button: show original values
       */
      _btnResetValues = new Button(container, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(_btnResetValues);
      _btnResetValues.setText(Messages.tour_merger_btn_reset_values);
      _btnResetValues.setToolTipText(Messages.tour_merger_btn_reset_values_tooltip);
      _btnResetValues.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectResetValues()));
   }

   /**
    * @param parent
    */
   private void createUISectionSaveActions(final Composite parent) {

      final int indentOption = _pc.convertHorizontalDLUsToPixels(20);
      final int indentOption2 = _pc.convertHorizontalDLUsToPixels(13);

      /*
       * group: save options
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.tour_merger_group_save_actions);
      group.setToolTipText(Messages.tour_merger_group_save_actions_tooltip);
      GridDataFactory.swtDefaults()//
            .grab(true, false)
            .align(SWT.BEGINNING, SWT.END)
            .applyTo(group);
      GridLayoutFactory.swtDefaults()//
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(group);

      /*
       * checkbox: merge pulse
       */
      _chkMergePulse = createUIMergeAction(
            group,
            TourManager.GRAPH_PULSE,
            Messages.merge_tour_source_graph_heartbeat,
            Messages.merge_tour_source_graph_heartbeat_tooltip,
            Images.Graph_Heartbeat,
            _sourceTour.pulseSerie != null);

      /*
       * checkbox: merge speed
       */
      _chkMergeSpeed = createUIMergeAction(
            group,
            TourManager.GRAPH_SPEED,
            Messages.merge_tour_source_graph_speed,
            Messages.merge_tour_source_graph_speed_tooltip,
            Images.Graph_Speed,
            _sourceTour.timeSerie != null && _sourceTour.distanceSerie != null);

      /*
       * checkbox: merge temperature
       */
      _chkMergeTemperature = createUIMergeAction(
            group,
            TourManager.GRAPH_TEMPERATURE,
            Messages.merge_tour_source_graph_temperature,
            Messages.merge_tour_source_graph_temperature_tooltip,
            Images.Graph_Temperature,
            _sourceTour.temperatureSerie != null);

      /*
       * checkbox: merge cadence
       */
      _chkMergeCadence = createUIMergeAction(
            group,
            TourManager.GRAPH_CADENCE,
            Messages.merge_tour_source_graph_cadence,
            Messages.merge_tour_source_graph_cadence_tooltip,
            Images.Graph_Cadence,
            _sourceTour.getCadenceSerie() != null);

      /*
       * checkbox: merge altitude
       */
      _chkMergeAltitude = createUIMergeAction(
            group,
            TourManager.GRAPH_ALTITUDE,
            Messages.merge_tour_source_graph_altitude,
            Messages.merge_tour_source_graph_altitude_tooltip,
            Images.Graph_Elevation,
            _sourceTour.altitudeSerie != null);

      /*
       * container: merge altitude
       */
      final Composite containerMergeGraph = new Composite(group, SWT.NONE);
      GridDataFactory.fillDefaults().indent(indentOption + 16, 0).applyTo(containerMergeGraph);
      GridLayoutFactory.fillDefaults()//
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(containerMergeGraph);

      /*
       * checkbox: adjust altitude from source
       */
      _chkAdjustAltiFromSource = new Button(containerMergeGraph, SWT.RADIO);
      _chkAdjustAltiFromSource.setText(Messages.tour_merger_chk_adjust_altitude_from_source);
      _chkAdjustAltiFromSource.setToolTipText(Messages.tour_merger_chk_adjust_altitude_from_source_tooltip);
//		fChkAdjustAltiFromSource.setImage(fIconPlaceholder);

      _chkAdjustAltiFromSource.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

         if (_chkAdjustAltiFromSource.getSelection()) {

            // check one adjust altitude
            if (_chkAdjustAltiFromSource.getSelection() == false
                  && _chkAdjustAltiFromStart.getSelection() == false) {
               _chkAdjustAltiFromSource.setSelection(true);
            }

            // only one altitude adjustment can be set
            _chkAdjustAltiFromStart.setSelection(false);

            _chkMergeAltitude.setSelection(true);

         } else {

            // disable altitude merge option
            _chkMergeAltitude.setSelection(false);
         }

         onModifyProperties();
      }));

      /*
       * checkbox: smooth altitude with linear interpolation
       */
      _chkAdjustAltiSmoothly = new Button(containerMergeGraph, SWT.CHECK);
      GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(_chkAdjustAltiSmoothly);
      _chkAdjustAltiSmoothly.setText(Messages.tour_merger_chk_adjust_altitude_linear_interpolition);
      _chkAdjustAltiSmoothly.setToolTipText(Messages.tour_merger_chk_adjust_altitude_linear_interpolition_tooltip);
      _chkAdjustAltiSmoothly.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModifyProperties()));

      /*
       * checkbox: adjust start altitude
       */
      _chkAdjustAltiFromStart = new Button(containerMergeGraph, SWT.RADIO);
      _chkAdjustAltiFromStart.setText(Messages.tour_merger_chk_adjust_start_altitude);
      _chkAdjustAltiFromStart.setToolTipText(Messages.tour_merger_chk_adjust_start_altitude_tooltip);
      _chkAdjustAltiFromStart.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

         if (_chkAdjustAltiFromStart.getSelection()) {

            // check one adjust altitude
            if (_chkAdjustAltiFromStart.getSelection() == false
                  && _chkAdjustAltiFromSource.getSelection() == false) {
               _chkAdjustAltiFromStart.setSelection(true);
            }

            // only one altitude adjustment can be done
            _chkAdjustAltiFromSource.setSelection(false);

            _chkMergeAltitude.setSelection(true);

         } else {

            // disable altitude merge option
            _chkMergeAltitude.setSelection(false);
         }

         onModifyProperties();
      }));

      /*
       * altitude adjustment values
       */
      final Composite aaContainer = new Composite(containerMergeGraph, SWT.NONE);
      GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(aaContainer);
      GridLayoutFactory.fillDefaults().numColumns(5).spacing(VH_SPACING, VH_SPACING).applyTo(aaContainer);

      _lblAdjustAltiValueTime = new Label(aaContainer, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
            .applyTo(_lblAdjustAltiValueTime);

      _lblAdjustAltiValueTimeUnit = new Label(aaContainer, SWT.NONE);
      _lblAdjustAltiValueTimeUnit.setText(UI.UNIT_LABEL_ELEVATION + "/min"); //$NON-NLS-1$

      _lblAdjustAltiValueDistance = new Label(aaContainer, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
            .applyTo(_lblAdjustAltiValueDistance);

      _lblAdjustAltiValueDistanceUnit = new Label(aaContainer, SWT.NONE);
      _lblAdjustAltiValueDistanceUnit.setText(UI.UNIT_LABEL_ELEVATION + "/" + UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$

      /*
       * checkbox: set tour type
       */
      _chkSetTourType = new Button(group, SWT.CHECK);
      GridDataFactory.fillDefaults().indent(0, 10).applyTo(_chkSetTourType);
      _chkSetTourType.setText(Messages.tour_merger_chk_set_tour_type);
      _chkSetTourType.setToolTipText(Messages.tour_merger_chk_set_tour_type_tooltip);
      _chkSetTourType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> enableActions()));

      final Composite ttContainer = new Composite(group, SWT.NONE);
      GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(ttContainer);
      GridLayoutFactory.fillDefaults()//
            .numColumns(2)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(ttContainer);

      /*
       * tour type
       */
      _linkTourType = new Link(ttContainer, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_linkTourType);
      _linkTourType.setText(Messages.tour_editor_label_tour_type);
      _linkTourType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> net.tourbook.common.UI.openControlMenu(_linkTourType)));

      /*
       * tour type menu
       */
      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(menuManager -> {

         // set menu items

         ActionSetTourTypeMenu.fillMenu(menuManager, DialogMergeTours.this, false);

         menuManager.add(new Separator());
         menuManager.add(_actionOpenTourTypePrefs);
      });

      // set menu for the tag item
      _linkTourType.setMenu(menuMgr.createContextMenu(_linkTourType));

      /*
       * label: tour type icon and text
       */
      _lblTourType = new CLabel(ttContainer, SWT.NONE);
      GridDataFactory.swtDefaults().grab(true, false).applyTo(_lblTourType);

      /*
       * checkbox: keep horizontal and vertical adjustments
       */
      _chkKeepHVAdjustments = new Button(group, SWT.CHECK);
      _chkKeepHVAdjustments.setText(Messages.tour_merger_chk_keep_horiz_vert_adjustments);
      _chkKeepHVAdjustments.setToolTipText(Messages.tour_merger_chk_keep_horiz_vert_adjustments_tooltip);
      _chkKeepHVAdjustments.setSelection(true);
      // this option cannot be deselected
      _chkKeepHVAdjustments.addSelectionListener(widgetSelectedAdapter(
            selectionEvent -> _chkKeepHVAdjustments.setSelection(true)));
   }

   private void createUITourChart(final Composite dlgContainer) {

      _tourChart = new TourChart(dlgContainer, SWT.BORDER, null, _state);
      GridDataFactory.fillDefaults().grab(true, true).minSize(300, 200).applyTo(_tourChart);

      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);

      // set altitude visible
      _tourChartConfig = new TourChartConfiguration(_state);

      // set one visible graph
      int visibleGraph = -1;
      if (_targetTour.altitudeSerie != null) {
         visibleGraph = TourManager.GRAPH_ALTITUDE;
      } else if (_targetTour.getCadenceSerie() != null) {
         visibleGraph = TourManager.GRAPH_CADENCE;
      } else if (_targetTour.pulseSerie != null) {
         visibleGraph = TourManager.GRAPH_PULSE;
      } else if (_targetTour.temperatureSerie != null) {
         visibleGraph = TourManager.GRAPH_TEMPERATURE;
      } else if (_targetTour.timeSerie != null && _targetTour.distanceSerie != null) {
         visibleGraph = TourManager.GRAPH_SPEED;
      }
      if (visibleGraph != -1) {
         _tourChartConfig.addVisibleGraph(visibleGraph);
      }

      // overwrite x-axis from pref store
      _tourChartConfig.setIsShowTimeOnXAxis(TourbookPlugin
            .getDefault()
            .getPreferenceStore()
            .getString(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS)
            .equals(TourManager.X_AXIS_TIME));

      // set title
      _tourChart.addDataModelListener(chartDataModel -> chartDataModel.setTitle(TourManager.getTourTitleDetailed(_targetTour)));

      _tourChart.addSliderMoveListener(selectionChartInfo -> {

         if (_isChartUpdated) {
            return;
         }

         onModifyProperties();
      });
   }

   private void enableActions() {

      final boolean isMergeAltitude = _chkMergeAltitude.getSelection();
      final boolean isAdjustAltitude = _chkAdjustAltiFromStart.getSelection() && isMergeAltitude;
      final boolean isSetTourType = _chkSetTourType.getSelection();

//		final boolean isMergeActionSelected = isMergeAltitude
//				|| fChkMergePulse.getSelection()
//				|| fChkMergeTemperature.getSelection()
//				|| fChkMergeCadence.getSelection();

      final boolean isAltitudeAvailable = _sourceTour.altitudeSerie != null && _targetTour.altitudeSerie != null;

      final boolean isSyncStartTime = _chkSynchStartTime.getSelection();
      final boolean isAdjustTime = isSyncStartTime == false;// && (isMergeActionSelected || isAltitudeAvailable);

      // adjust start altitude
      _chkAdjustAltiFromStart.setEnabled(isMergeAltitude && isAltitudeAvailable);
      _lblAdjustAltiValueDistance.setEnabled(isMergeAltitude && isAdjustAltitude);
      _lblAdjustAltiValueDistanceUnit.setEnabled(isMergeAltitude && isAdjustAltitude);
      _lblAdjustAltiValueTime.setEnabled(isMergeAltitude && isAdjustAltitude);
      _lblAdjustAltiValueTimeUnit.setEnabled(isMergeAltitude && isAdjustAltitude);

      // adjust from source altitude
      _chkAdjustAltiFromSource.setEnabled(isMergeAltitude && isAltitudeAvailable);
      _chkAdjustAltiSmoothly.setEnabled(isMergeAltitude && _chkAdjustAltiFromSource.getSelection());

      // set tour type
      _linkTourType.setEnabled(isSetTourType);

      /*
       * CLabel cannot be disabled, show disabled control with another color
       */
      if (isSetTourType) {
         _lblTourType.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
      } else {
         _lblTourType.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
      }

      /*
       * altitude adjustment controls
       */
      _scaleAltitude1.setEnabled(isAltitudeAvailable);
      _scaleAltitude10.setEnabled(isAltitudeAvailable);
      _lblAltitudeDiff1.setEnabled(isAltitudeAvailable);
      _lblAltitudeDiff10.setEnabled(isAltitudeAvailable);

      _chkValueDiffScaling.setEnabled(isAltitudeAvailable);

      /*
       * time adjustment controls
       */
      _chkSynchStartTime.setEnabled(true);

      _scaleAdjustMinutes.setEnabled(isAdjustTime);
      _lblAdjustMinuteValue.setEnabled(isAdjustTime);
      _lblAdjustMinuteUnit.setEnabled(isAdjustTime);

      _scaleAdjustSeconds.setEnabled(isAdjustTime);
      _lblAdjustSecondsValue.setEnabled(isAdjustTime);
      _lblAdjustSecondsUnit.setEnabled(isAdjustTime);

      /*
       * reset buttons
       */
      _btnResetAdjustment.setEnabled(true);
      _btnResetValues.setEnabled(true);
   }

   private void enableGraphActions() {

      final boolean isAltitude = _sourceTour.altitudeSerie != null && _targetTour.altitudeSerie != null;
      final boolean isSourceDistance = _sourceTour.distanceSerie != null;
      final boolean isSourcePulse = _sourceTour.pulseSerie != null;
      final boolean isSourceTime = _sourceTour.timeSerie != null;
      final boolean isSourceTemperature = _sourceTour.temperatureSerie != null;
      final boolean isSourceCadence = _sourceTour.getCadenceSerie() != null;

      _chkMergeAltitude.setEnabled(isAltitude);
      _chkMergePulse.setEnabled(isSourcePulse);
      _chkMergeSpeed.setEnabled(isSourceTime && isSourceDistance);
      _chkMergeTemperature.setEnabled(isSourceTemperature);
      _chkMergeCadence.setEnabled(isSourceCadence);

      /*
       * keep state from the pref store but uncheck graphs which are not available
       */
      if (isAltitude == false) {
         _chkMergeAltitude.setSelection(false);
      }
      if (isSourcePulse == false) {
         _chkMergePulse.setSelection(false);
      }
      if (isSourceTime == false || isSourceDistance == false) {
         _chkMergeSpeed.setSelection(false);
      }
      if (isSourceTemperature == false) {
         _chkMergeTemperature.setSelection(false);
      }
      if (isSourceCadence == false) {
         _chkMergeCadence.setSelection(false);
      }

      if (_chkMergeAltitude.getSelection()) {

         // ensure that one adjust altitude option is selected
         if (_chkAdjustAltiFromStart.getSelection() == false && _chkAdjustAltiFromSource.getSelection() == false) {
            _chkAdjustAltiFromSource.setSelection(true);
         }
      }
   }

   private void enableMergeButton(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
//		return null;
      return _state;
   }

   private int getFromUIAltitudeOffset() {

      final int altiDiff1 = _scaleAltitude1.getSelection() - MAX_ADJUST_ALTITUDE_1;
      final int altiDiff10 = (_scaleAltitude10.getSelection() - MAX_ADJUST_ALTITUDE_10) * 10;

      final float localAltiDiff1 = altiDiff1 / UI.UNIT_VALUE_ELEVATION;
      final float localAltiDiff10 = altiDiff10 / UI.UNIT_VALUE_ELEVATION;

      _lblAltitudeDiff1.setText(Integer.toString((int) localAltiDiff1) + UI.SPACE + UI.UNIT_LABEL_ELEVATION);
      _lblAltitudeDiff10.setText(Integer.toString((int) localAltiDiff10) + UI.SPACE + UI.UNIT_LABEL_ELEVATION);

      return altiDiff1 + altiDiff10;
   }

   /**
    * @return tour time offset which is set in the UI
    */
   private int getFromUITourTimeOffset() {

      final int seconds = _scaleAdjustSeconds.getSelection() - MAX_ADJUST_SECONDS;
      final int minutes = _scaleAdjustMinutes.getSelection() - MAX_ADJUST_MINUTES;

      _lblAdjustSecondsValue.setText(Integer.toString(seconds));
      _lblAdjustMinuteValue.setText(Integer.toString(minutes));

      return minutes * 60 + seconds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> selectedTours = new ArrayList<>();
      selectedTours.add(_sourceTour);

      return selectedTours;
   }

   @Override
   protected void okPressed() {

      saveTour();

      super.okPressed();
   }

   private void onDispose() {

      _iconPlaceholder.dispose();

      _graphImages.values().forEach(Image::dispose);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
   }

   private void onModifyProperties() {

      _targetTour.setMergedAltitudeOffset(getFromUIAltitudeOffset());
      _targetTour.setMergedTourTimeOffset(getFromUITourTimeOffset());

      // calculate merged data
      computeMergedData();

      _tourChartConfig.isRelativeValueDiffScaling = _chkValueDiffScaling.getSelection();

      if (_chkPreviewChart.getSelection()) {
         // update chart
         updateTourChart();
      } else {
         // update only the merge layer, this is much faster
         _tourChart.updateLayer2ndAlti(this, true);
      }

      enableActions();
   }

   private void onScaleDoubleClick(final Widget widget) {

      final Scale scale = (Scale) widget;
      final int max = scale.getMaximum();

      scale.setSelection(max / 2);

      onModifyProperties();
   }

   private void onSelectMergeGraph(final SelectionEvent event) {

      if (event.widget == _chkMergeAltitude) {

         // merge altitude is modified

         if (_chkMergeAltitude.getSelection()) {

            // merge altitude is selected

            _chkAdjustAltiFromSource.setSelection(_isAdjustAltiFromSourceBackup);
            _chkAdjustAltiFromStart.setSelection(_isAdjustAltiFromStartBackup);
         } else {

            // merge altitude is deselected

            _isAdjustAltiFromSourceBackup = _chkAdjustAltiFromSource.getSelection();
            _isAdjustAltiFromStartBackup = _chkAdjustAltiFromStart.getSelection();
         }
      }

      if (_chkMergeAltitude.getSelection()) {

         // ensure that one adjust altitude option is selected
         if (_chkAdjustAltiFromStart.getSelection() == false && _chkAdjustAltiFromSource.getSelection() == false) {
            _chkAdjustAltiFromSource.setSelection(true);
         }
      } else {

         // Uncheck all altitude adjustments
         _chkAdjustAltiFromSource.setSelection(false);
         _chkAdjustAltiFromStart.setSelection(false);
      }

      setMergedGraphsVisible();

      onModifyProperties();
      updateTourChart();
   }

   private void onSelectResetAdjustments() {

      _scaleAdjustSeconds.setSelection(MAX_ADJUST_SECONDS);
      _scaleAdjustMinutes.setSelection(MAX_ADJUST_MINUTES);
      _scaleAltitude1.setSelection(MAX_ADJUST_ALTITUDE_1);
      _scaleAltitude10.setSelection(MAX_ADJUST_ALTITUDE_10);

      onModifyProperties();
   }

   private void onSelectResetValues() {

      /*
       * Get original data from the backed up data
       */
      _sourceTour.timeSerie = Util.createIntegerCopy(_backupSourceTimeSerie);
      _sourceTour.distanceSerie = Util.createFloatCopy(_backupSourceDistanceSerie);
      _sourceTour.altitudeSerie = Util.createFloatCopy(_backupSourceAltitudeSerie);

      _targetTour.altitudeSerie = Util.createFloatCopy(_backupTargetAltitudeSerie);
      _targetTour.setCadenceSerie(Util.createFloatCopy(_backupTargetCadenceSerie));
      _targetTour.pulseSerie = Util.createFloatCopy(_backupTargetPulseSerie);
      _targetTour.temperatureSerie = Util.createFloatCopy(_backupTargetTemperatureSerie);
      _targetTour.setSpeedSerie(Util.createFloatCopy(_backupTargetSpeedSerie));
      _targetTour.timeSerie = Util.createIntegerCopy(_backupTargetTimeSerie);
      _targetTour.setTourDeviceTime_Elapsed(_backupTargetDeviceTime_Elapsed);

      _targetTour.setMergedTourTimeOffset(_backupTargetTimeOffset);
      _targetTour.setMergedAltitudeOffset(_backupTargetAltitudeOffset);

      updateUIFromTourData();

      enableActions();
   }

   /**
    * Restore values which have been modified in the dialog
    */
   private void restoreDataBackup() {

      _sourceTour.timeSerie = _backupSourceTimeSerie;
      _sourceTour.distanceSerie = _backupSourceDistanceSerie;
      _sourceTour.altitudeSerie = _backupSourceAltitudeSerie;
      _sourceTour.setTourType(_backupSourceTourType);

      _targetTour.altitudeSerie = _backupTargetAltitudeSerie;
      _targetTour.setCadenceSerie(_backupTargetCadenceSerie);
      _targetTour.pulseSerie = _backupTargetPulseSerie;
      _targetTour.setSpeedSerie(_backupTargetSpeedSerie);
      _targetTour.temperatureSerie = _backupTargetTemperatureSerie;
      _targetTour.timeSerie = _backupTargetTimeSerie;
      _targetTour.setTourDeviceTime_Elapsed(_backupTargetDeviceTime_Elapsed);

      _targetTour.setMergedTourTimeOffset(_backupTargetTimeOffset);
      _targetTour.setMergedAltitudeOffset(_backupTargetAltitudeOffset);
   }

   private void restoreState() {

      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

      _chkPreviewChart.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART));
      _chkValueDiffScaling.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING));

      _chkAdjustAltiSmoothly.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION));

      _chkSynchStartTime.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME));

      /*
       * set tour type
       */
      _chkSetTourType.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE));
      if (_sourceTour.getTourPerson() == null) {

         // tour is not saved, used tour type id from pref store

         final long tourTypeId = prefStore.getLong(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID);

         final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

         for (final TourType tourType : tourTypes) {
            if (tourType.getTypeId() == tourTypeId) {

               _sourceTour.setTourType(tourType);

               _isMergeSourceTourModified = true;

               break;
            }
         }
      }

      // restore merge graph state
      _chkMergeAltitude.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE));
      _chkMergeCadence.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE));
      _chkMergePulse.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE));
      _chkMergeTemperature.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE));
      _chkMergeSpeed.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_SPEED));
   }

   private void saveState() {

      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS,
            _tourChartConfig.isShowTimeOnXAxis
                  ? TourManager.X_AXIS_TIME
                  : TourManager.X_AXIS_DISTANCE);

      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, _chkPreviewChart.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, _chkValueDiffScaling.getSelection());

      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION, _chkAdjustAltiSmoothly.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE, _chkSetTourType.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME, _chkSynchStartTime.getSelection());

      // save tour type id
      final TourType sourceTourType = _sourceTour.getTourType();
      if (sourceTourType != null) {
         prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID, sourceTourType.getTypeId());
      }

      // save merged tour graphs
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE, _chkMergeAltitude.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE, _chkMergeCadence.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE, _chkMergePulse.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE, _chkMergeTemperature.getSelection());
      prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_SPEED, _chkMergeSpeed.getSelection());
   }

   private void saveTour() {

      boolean isMerged = false;

      if (_chkMergeAltitude.getSelection()) {

         // set target altitude values

         if ((_chkAdjustAltiFromStart.getSelection() || _chkAdjustAltiFromSource.getSelection())
               && _sourceTour.dataSerieAdjustedAlti != null) {

            isMerged = true;

            // update target altitude from adjusted source altitude
            _targetTour.altitudeSerie = _sourceTour.dataSerieAdjustedAlti;

            // adjust altitude up/down values
            _targetTour.computeAltitudeUpDown();
         }
      } else {
         _targetTour.altitudeSerie = _backupTargetAltitudeSerie;
      }

      if (_chkMergeCadence.getSelection()) {
         // cadence is already merged
         isMerged = true;
      } else {
         // restore original cadence values because these values should not be saved
         _targetTour.setCadenceSerie(_backupTargetCadenceSerie);
      }

      if (_chkMergePulse.getSelection()) {
         // pulse is already merged
         isMerged = true;
      } else {
         // restore original pulse values because these values should not be saved
         _targetTour.pulseSerie = _backupTargetPulseSerie;
      }

      if (_chkMergeTemperature.getSelection()) {
         // temperature is already merged
         isMerged = true;
      } else {
         // restore original temperature values because these values should not be saved
         _targetTour.temperatureSerie = _backupTargetTemperatureSerie;
      }

      if (_chkMergeSpeed.getSelection()) {
         // speed is already merged
         isMerged = true;
      } else {
         // restore original speed and time values because these values should not be saved
         _targetTour.setSpeedSerie(_backupTargetSpeedSerie);
         _targetTour.timeSerie = _backupTargetTimeSerie;
         _targetTour.setTourDeviceTime_Elapsed(_backupTargetDeviceTime_Elapsed);
         _targetTour.setTourDeviceTime_Recorded(_targetTour.getTourDeviceTime_Elapsed());
         _targetTour.computeTourMovingTime();
      }

      if (_chkSetTourType.getSelection() == false) {

         // restore backup values

         _sourceTour.setTourType(_backupSourceTourType);
      }

      if (isMerged) {
         _targetTour.computeComputedValues();
      }

      // set tour id into which the tour is merged
      _sourceTour.setTourPerson(_targetTour.getTourPerson());
      _sourceTour.setMergeTargetTourId(_targetTour.getTourId());
      _sourceTour.setMergeSourceTourId(null);

      // save modified tours
      final ArrayList<TourData> modifiedTours = new ArrayList<>();
      modifiedTours.add(_targetTour);
      modifiedTours.add(_sourceTour);

      TourManager.saveModifiedTours(modifiedTours);

      _isTourSaved = true;
   }

   private void setMergedGraphsVisible() {

      if (_chkMergeAltitude.getSelection()) {
         addVisibleGraph(TourManager.GRAPH_ALTITUDE);
      }

      if (_chkMergeCadence.getSelection()) {
         addVisibleGraph(TourManager.GRAPH_CADENCE);
      }
      if (_chkMergePulse.getSelection()) {
         addVisibleGraph(TourManager.GRAPH_PULSE);
      }

      if (_chkMergeTemperature.getSelection()) {
         addVisibleGraph(TourManager.GRAPH_TEMPERATURE);
      }

      if (_chkMergeSpeed.getSelection()) {
         addVisibleGraph(TourManager.GRAPH_SPEED);
      }
   }

   @Override
   public void toursAreModified(final ArrayList<TourData> modifiedTours) {

      // tour type was modified
      _isMergeSourceTourModified = true;

      updateUIFromTourData();
   }

   private void updateTourChart() {

      _isChartUpdated = true;

      _tourChart.updateTourChart(_targetTour, _tourChartConfig, true);

      _isChartUpdated = false;
   }

   private void updateUI(final float[] altitudeDifferences) {

      final float altiDiffTime = altitudeDifferences[0];
      final float altiDiffDist = altitudeDifferences[1];

      if (_chkAdjustAltiFromStart.getSelection()) {

         _lblAdjustAltiValueTime.setText(_nf.format(altiDiffTime));
         _lblAdjustAltiValueDistance.setText(_nf.format(altiDiffDist));

      } else {

         // adjusted altitude is disabled

         _lblAdjustAltiValueTime.setText("N/A"); //$NON-NLS-1$
         _lblAdjustAltiValueDistance.setText("N/A"); //$NON-NLS-1$
      }
   }

   /**
    * set data from the tour into the UI
    */
   private void updateUIFromTourData() {

      /*
       * show time offset
       */
      updateUITourTimeOffset(_targetTour.getMergedTourTimeOffset());

      /*
       * show altitude offset
       */
      final int mergedMetricAltitudeOffset = _targetTour.getMergedAltitudeOffset();

      final float altitudeOffset = mergedMetricAltitudeOffset;
      final int altitudeOffset1 = (int) (altitudeOffset % 10);
      final int altitudeOffset10 = (int) (altitudeOffset / 10);

      _scaleAltitude1.setSelection(altitudeOffset1 + MAX_ADJUST_ALTITUDE_1);
      _scaleAltitude10.setSelection(altitudeOffset10 + MAX_ADJUST_ALTITUDE_10);

      net.tourbook.ui.UI.updateUI_TourType(_sourceTour, _lblTourType, true);

      onModifyProperties();
   }

   private void updateUITourTimeOffset(final int tourTimeOffset) {

      final int seconds = tourTimeOffset % 60;
      final int minutes = tourTimeOffset / 60;

      _scaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
      _scaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      /*
       * validate fields
       */
      boolean enableMergeButton = false;

      if (_chkMergeAltitude.getSelection() ||
            _chkMergeCadence.getSelection() ||
            _chkMergePulse.getSelection() ||
            _chkMergeTemperature.getSelection() ||
            _chkMergeSpeed.getSelection()) {
         enableMergeButton = true;
      }

      enableMergeButton(enableMergeButton);
   }
}
