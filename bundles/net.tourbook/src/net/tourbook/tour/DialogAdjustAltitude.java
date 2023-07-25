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
package net.tourbook.tour;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartCursor;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.MouseAdapter;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.ElevationGainLoss;
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;
import net.tourbook.math.CubicSpline;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.srtm.IPreferences;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.SplineDrawingData;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

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
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour chart or from the
 * tree viewer
 */
public class DialogAdjustAltitude extends TitleAreaDialog implements I2ndAltiLayer {

   private static final String ID = "net.tourbook.tour.DialogAdjustAltitude"; //$NON-NLS-1$

   // 40 is the largest size that the mouse wheel can adjust the scale by 1 (windows)
   public static final int     MAX_ADJUST_GEO_POS_SLICES           = 40;

   private static final String WIDGET_DATA_ALTI_ID                 = "altiId";         //$NON-NLS-1$
   private static final String WIDGET_DATA_METRIC_ALTITUDE         = "metricAltitude"; //$NON-NLS-1$

   private static final int    ALTI_ID_START                       = 1;
   private static final int    ALTI_ID_END                         = 2;
   private static final int    ALTI_ID_MAX                         = 3;

   private static final int    ADJUST_TYPE_SRTM                    = 1010;
   private static final int    ADJUST_TYPE_SRTM_SPLINE             = 1020;
   private static final int    ADJUST_TYPE_WHOLE_TOUR              = 1030;
   private static final int    ADJUST_TYPE_START_AND_END           = 1040;
   private static final int    ADJUST_TYPE_MAX_HEIGHT              = 1050;
   private static final int    ADJUST_TYPE_END                     = 1060;
   private static final int    ADJUST_TYPE_HORIZONTAL_GEO_POSITION = 1100;

// SET_FORMATTING_OFF

   private static AdjustmentType[]      ALL_ADJUSTMENT_TYPES      = new AdjustmentType[] {

         new AdjustmentType(ADJUST_TYPE_SRTM_SPLINE,              Messages.adjust_altitude_type_srtm_spline),
         new AdjustmentType(ADJUST_TYPE_SRTM,                     Messages.adjust_altitude_type_srtm),
         new AdjustmentType(ADJUST_TYPE_START_AND_END,            Messages.adjust_altitude_type_start_and_end),
         new AdjustmentType(ADJUST_TYPE_MAX_HEIGHT,               Messages.adjust_altitude_type_adjust_height),
         new AdjustmentType(ADJUST_TYPE_END,                      Messages.adjust_altitude_type_adjust_end),
         new AdjustmentType(ADJUST_TYPE_WHOLE_TOUR,               Messages.adjust_altitude_type_adjust_whole_tour),
         new AdjustmentType(ADJUST_TYPE_HORIZONTAL_GEO_POSITION,  Messages.Adjust_Altitude_Type_HorizontalGeoPosition),
   };

   private static final String         PREF_ADJUST_TYPE           = "adjust.altitude.adjust_type";                //$NON-NLS-1$
   private static final String         PREF_KEEP_START            = "adjust.altitude.keep_start";                 //$NON-NLS-1$
   private static final String         PREF_SCALE_GEO_POSITION    = "Dialog_AdjustAltitude_GeoPositionScale";     //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final NumberFormat _nf0 = NumberFormat.getNumberInstance();

   static {

      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   private final IDialogSettings   _state     = TourbookPlugin.getState(ID);
   private final IPreferenceStore  _prefStore = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener _prefChangeListener;

   /*
    * data
    */
   private boolean                         _isSliderEventDisabled;
   private boolean                         _isTourSaved;
   private final boolean                   _isSaveTour;
   private final boolean                   _isCreateDummyElevation;
   private boolean                         _isSetEndElevation_To_SRTMValue;

   private final TourData                  _tourData;
   private SplineData                      _splineData;

   private float[]                         _backup_MetricAltitudeSerie;
   private float[]                         _backup_SrtmSerie;
   private float[]                         _backup_SrtmSerieImperial;
   private boolean                         _backup_IsSRTM1Values;

   private float[]                         _metricAdjustedAltitudeWithoutSRTM;

   private int                             _oldAdjustmentType        = -1;
   private final ArrayList<AdjustmentType> _availableAdjustmentTypes = new ArrayList<>();

   private int                             _pointHitIndex            = -1;

   /**
    * Elevation difference for the 1st time slice between tour elevation and SRTM elevation
    */
   private double                          _firstTimeSlice_ElevationDiff;
   private double                          _spline_BorderLeft_xValue;
   private double                          _spline_BorderRight_xValue;

   private boolean                         _canDeletePoint;
   private boolean                         _isDisableModifyListener;

   private float                           _initialAltiStart;
   private float                           _initialAltiMax;

   private float                           _altiMaxDiff;
   private float                           _altiStartDiff;

   private float                           _prevAltiMax;
   private float                           _prevAltiStart;

   private ChartLayer2ndAltiSerie          _chartLayer2ndAltiSerie;

   private PixelConverter                  _pc;

   private TourChart                       _tourChart;
   private TourChartConfiguration          _tcc;

   /*
    * UI controls
    */

   private Composite _dlgContainer;

   private PageBook  _pageBookOptions;
   private Label     _pageEmpty;
   private Composite _pageOption_SRTM_AndSpline;
   private Composite _pageOption_NoSRTM;
   private Composite _pageOption_SRTM;
   private Composite _pageOption_GeoPosition;

   private Combo     _comboAdjustmentType;

   private Button    _btnSRTMRemoveAllPoints;
   private Button    _btnResetAltitude;
   private Button    _btnUpdateAltitude;

   private Button    _rdoKeepBottom;
   private Button    _rdoKeepStart;

   private Label     _lblAdjustmentTypeInfo;
   private Label     _lblElevation_Up;
   private Label     _lblElevation_UpAdjusted;
   private Label     _lblElevation_UpAdjustedDiff;
   private Label     _lblElevation_Down;
   private Label     _lblElevation_DownAdjusted;
   private Label     _lblElevation_DownAdjustedDiff;
   private Label     _lblOldStartAlti;
   private Label     _lblOldMaxAlti;
   private Label     _lblOldEndAlti;
   private Label     _lblSliceValue;

   private Link      _linkSRTM_AdjustEndToStart;
   private Link      _linkSRTM_SelectWholeTour;

   private Scale     _scaleSlicePos;

   private Spinner   _spinnerNewStartAlti;
   private Spinner   _spinnerNewMaxAlti;
   private Spinner   _spinnerNewEndAlti;

   private static class AdjustmentType {

      int    __id;
      String __visibleName;

      AdjustmentType(final int id, final String visibleName) {
         __id = id;
         __visibleName = visibleName;
      }
   }

   DialogAdjustAltitude(final Shell parentShell,
                        final TourData tourData,
                        final boolean isSaveTour,
                        final boolean isCreateDummyAltitude) {

      super(parentShell);

      _tourData = tourData;
      _isSaveTour = isSaveTour;
      _isCreateDummyElevation = isCreateDummyAltitude;

      // set icon for the window
      setDefaultImage(TourbookPlugin.getThemedImageDescriptor(Images.AdjustElevation).createImage());

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      addPrefListener();
   }

   void actionCreateSplinePoint(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {

      if (splinePoint_NewPoint(mouseDownDevPositionX, mouseDownDevPositionY)) {
         onSelectAdjustmentType();
      }
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.GRAPH_IS_SHOW_SRTM_1_VALUES)) {

            // run delayed because this is called when the value is set in the pref store
            _dlgContainer.getDisplay().asyncExec(() -> {

               final boolean isUseSRTM1Values = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_SRTM_1_VALUES);

               onSelectSRTMResolution(isUseSRTM1Values);
            });
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   @Override
   public boolean close() {

      saveState();

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      if (_isTourSaved == false) {

         // tour is not saved, dialog is canceled, restore original values

         if (_isCreateDummyElevation) {
            _tourData.altitudeSerie = null;
         } else {
            _tourData.altitudeSerie = _backup_MetricAltitudeSerie;
         }
         _tourData.clearAltitudeSeries();

         _tourData.setSRTMValues(_backup_SrtmSerie, _backup_SrtmSerieImperial, _backup_IsSRTM1Values);
      }

      return super.close();
   }

   /**
    * adjust end altitude
    *
    * @param altiSrc
    * @param tourData
    * @param newEndAlti
    */
   private void computeElevation_End(final float[] altiSrc, final float[] altiDest, final float newEndAlti) {

      double[] xDataSerie = _tourData.getDistanceSerieDouble();
      if (xDataSerie == null) {
         xDataSerie = _tourData.getTimeSerieDouble();
      }

      final float altiEndDiff = newEndAlti - altiSrc[altiDest.length - 1];
      final double lastXValue = xDataSerie[xDataSerie.length - 1];

      for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {
         final double xValue = xDataSerie[serieIndex];
         final float altiDiff = (float) (xValue / lastXValue * altiEndDiff);// + 0.5f;
         altiDest[serieIndex] = altiSrc[serieIndex] + altiDiff;
      }
   }

   /**
    * adjust every altitude with the same difference
    *
    * @param altiSrc
    * @param altiDest
    * @param newStartAlti
    */
   private void computeElevation_Evenly(final float[] altiSrc, final float[] altiDest, final float newStartAlti) {

      final float altiStartDiff = newStartAlti - altiSrc[0];

      for (int altIndex = 0; altIndex < altiSrc.length; altIndex++) {
         altiDest[altIndex] = altiSrc[altIndex] + altiStartDiff;
      }
   }

   /**
    * Adjust max altitude, keep min value
    *
    * @param altiSrc
    * @param altiDest
    * @param maxAltiNew
    */
   private void computeElevation_Max(final float[] altiSrc, final float[] altiDest, final float maxAltiNew) {

      // calculate min/max altitude
      float minAltiSrc = altiSrc[0];
      float maxAltiSrc = altiSrc[0];
      for (final float altitude : altiSrc) {
         if (altitude > maxAltiSrc) {
            maxAltiSrc = altitude;
         }
         if (altitude < minAltiSrc) {
            minAltiSrc = altitude;
         }
      }

      // adjust altitude
      final float diffSrc = maxAltiSrc - minAltiSrc;
      final float diffNew = maxAltiNew - minAltiSrc;

      final float scaleDiff = diffSrc / diffNew;

      for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {

         final float altiDiff = altiSrc[serieIndex] - minAltiSrc;
         final float alti0Based = altiDiff / scaleDiff;

         altiDest[serieIndex] = alti0Based + minAltiSrc;
      }
   }

   private void computeElevation_SRTM() {

      // srtm values are available, otherwise this option is not available in the combo box

      final int serieLength = _tourData.timeSerie.length;

      final float[] adjustedAltiSerie = _tourData.dataSerieAdjustedAlti = new float[serieLength];
      final float[] diffTo2ndAlti = _tourData.dataSerieDiffTo2ndAlti = new float[serieLength];

      // get altitude diff serie
      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float srtmAltitude = _backup_SrtmSerie[serieIndex];

         diffTo2ndAlti[serieIndex] = 0;
         adjustedAltiSerie[serieIndex] = srtmAltitude / UI.UNIT_VALUE_ELEVATION;
      }
   }

   /**
    * Adjust start elevation until right slider
    */
   private void computeElevation_SRTM_WithSpline() {

      // srtm values are available, otherwise this option is not available in the combo box

      final SelectionChartXSliderPosition xSliderPosition = _tourChart.getXSliderPosition();
      final int spline_LeftPosIndex = xSliderPosition.getLeftSliderValueIndex();
      final int spline_RightPosIndex = xSliderPosition.getRightSliderValueIndex();
      final int serieLength = _tourData.timeSerie.length;

      final float[] metric_AdjustedElevationSerie = new float[serieLength];
      final float[] measurementSystem_AdjustedElevationSerie = _tourData.dataSerieAdjustedAlti = new float[serieLength];
      final float[] metric_DiffTo2ndElevation = _tourData.dataSerieDiffTo2ndAlti = new float[serieLength];
      final float[] splineElevationSerie = _tourData.dataSerieSpline = new float[serieLength];

      final double[] xDataSerie = _tcc.isShowTimeOnXAxis
            ? _tourData.getTimeSerieDouble()
            : _tourData.getDistanceSerieDouble();

      _spline_BorderLeft_xValue = xDataSerie[spline_LeftPosIndex];
      _spline_BorderRight_xValue = xDataSerie[spline_RightPosIndex];

      final int numXSlices = xDataSerie.length;
      final int lastXSliceIndex = numXSlices - 1;

      final float[] yDataElevationSerie = _tourData.altitudeSerie;

      _firstTimeSlice_ElevationDiff = _backup_SrtmSerie[0] - yDataElevationSerie[0];

      // ensure that a point can be moved with the mouse
      _firstTimeSlice_ElevationDiff = _firstTimeSlice_ElevationDiff == 0
            ? 1
            : _firstTimeSlice_ElevationDiff;

      /*
       * Get positions of the spline points in the graph
       */
      final double[] splineRelativePositionX = _splineData.posX_RelativeValues;
      final int numSplinePoints = splineRelativePositionX.length;
      _splineData.splinePoint_DataSerieIndex = new int[numSplinePoints];

      for (int pointIndex = 0; pointIndex < numSplinePoints; pointIndex++) {

         final double pointRelativePosX = splineRelativePositionX[pointIndex];
         final double pointAbsoluteValueX = _spline_BorderLeft_xValue + (_spline_BorderRight_xValue - _spline_BorderLeft_xValue) * pointRelativePosX;

         boolean isPointSet = false;

         for (int serieIndex = 0; serieIndex < numXSlices; serieIndex++) {

            final double graphX = xDataSerie[serieIndex];

            if (graphX >= pointAbsoluteValueX) {

               _splineData.splinePoint_DataSerieIndex[pointIndex] = serieIndex;

               isPointSet = true;

               break;
            }
         }

         if (isPointSet == false && pointAbsoluteValueX > xDataSerie[lastXSliceIndex]) {

            // set point for the last graph value
            _splineData.splinePoint_DataSerieIndex[pointIndex] = lastXSliceIndex;
         }
      }

      if (_isSetEndElevation_To_SRTMValue) {

         _isSetEndElevation_To_SRTMValue = false;

         spline_SetEndElevation_To_SRTMValue();
      }

      /*
       * Compute adjusted elevation serie
       */
      final CubicSpline splinePerformer = spline_CreateSplinePerformer();

      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float metric_OriginalElevation = yDataElevationSerie[serieIndex];
         final float metric_SrtmValue = _backup_SrtmSerie[serieIndex];

         if (serieIndex >= spline_LeftPosIndex && serieIndex <= spline_RightPosIndex && spline_RightPosIndex != 0) {

            // elevation is adjusted

            final double xValue = xDataSerie[serieIndex];
            final double distanceScale = 1 - (xValue / _spline_BorderRight_xValue);

            final float linearAdjusted_ElevationDiff = (float) (distanceScale * _firstTimeSlice_ElevationDiff);
            final float metric_NewElevation = metric_OriginalElevation + linearAdjusted_ElevationDiff;

            float splineElevation = 0;
            try {

               splineElevation = (float) splinePerformer.interpolate(xValue);

            } catch (final IllegalArgumentException e) {
               System.out.println(e.getMessage());
            }

            final float metric_AdjustedElevation = metric_NewElevation + splineElevation;
            final float measurementSystem_AdjustedElevation = metric_AdjustedElevation / UI.UNIT_VALUE_ELEVATION;

            splineElevationSerie[serieIndex] = splineElevation;

            metric_AdjustedElevationSerie[serieIndex] = metric_AdjustedElevation;
            metric_DiffTo2ndElevation[serieIndex] = metric_SrtmValue - metric_AdjustedElevation;
            measurementSystem_AdjustedElevationSerie[serieIndex] = measurementSystem_AdjustedElevation;

         } else {

            // elevation is not adjusted

            final float measurementSystem_AdjustedElevation = metric_OriginalElevation / UI.UNIT_VALUE_ELEVATION;

            metric_AdjustedElevationSerie[serieIndex] = metric_OriginalElevation;
            metric_DiffTo2ndElevation[serieIndex] = metric_SrtmValue - metric_OriginalElevation;
            measurementSystem_AdjustedElevationSerie[serieIndex] = measurementSystem_AdjustedElevation;
         }
      }

      /*
       * Update UI up/down values
       */
      final ElevationGainLoss adjustedElevationUpDown = _tourData.computeAltitudeUpDown(metric_AdjustedElevationSerie);

      final float measurementSystem_TourElevationUp = _tourData.getTourAltUp() / UI.UNIT_VALUE_ELEVATION;
      final float measurementSystem_TourElevationDown = _tourData.getTourAltDown() / UI.UNIT_VALUE_ELEVATION;

      final float adjustedElevationUp = adjustedElevationUpDown.getElevationGain() / UI.UNIT_VALUE_ELEVATION;
      final float adjustedElevationDown = adjustedElevationUpDown.getElevationLoss() / UI.UNIT_VALUE_ELEVATION;

      final float tourElevationUp_Diff = adjustedElevationUp - measurementSystem_TourElevationUp;
      final float tourElevationDown_Diff = adjustedElevationDown - measurementSystem_TourElevationDown;

      _lblElevation_Up.setText(_nf0.format(measurementSystem_TourElevationUp));
      _lblElevation_Down.setText(_nf0.format(measurementSystem_TourElevationDown));

      _lblElevation_UpAdjusted.setText(_nf0.format(adjustedElevationUp));
      _lblElevation_DownAdjusted.setText(_nf0.format(adjustedElevationDown));

      _lblElevation_UpAdjustedDiff.setText(_nf0.format(tourElevationUp_Diff));
      _lblElevation_DownAdjustedDiff.setText(_nf0.format(tourElevationDown_Diff));
   }

   /**
    * Adjust start and max at the same time
    * <p>
    * it took me several days to figure out this algorithim, 10.4.2007 Wolfgang
    */
   private void computeElevation_StartAndMax(final float[] altiSrc,
                                             final float[] altiDest,
                                             final float newAltiStart,
                                             final float newAltiMax) {

      /*
       * adjust max
       */
      _altiStartDiff = _altiStartDiff - (_prevAltiStart - newAltiStart);
      _altiMaxDiff = _altiMaxDiff - (_prevAltiMax - newAltiMax);

      final float oldStart = altiSrc[0];
      computeElevation_Max(altiSrc, altiDest, _initialAltiMax + _altiMaxDiff);
      final float newStart = altiDest[0];

      /*
       * adjust start
       */
      float startDiff;
      if (_rdoKeepStart.getSelection()) {
         startDiff = 0;
      } else {
         startDiff = newStart - oldStart;
      }
      computeElevation_Evenly(altiDest, altiDest, _initialAltiStart + _altiStartDiff + startDiff);
   }

   private void computeElevation_WithoutSRTM() {

      final float newAltiStart = (Float) _spinnerNewStartAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
      final float newAltiEnd = (Float) _spinnerNewEndAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
      final float newAltiMax = (Float) _spinnerNewMaxAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);

      final float[] metricAltitudeSerie = _tourData.altitudeSerie;

      // set adjustment type and enable the field(s) which can be modified
      switch (getSelectedAdjustmentType().__id) {

      case ADJUST_TYPE_WHOLE_TOUR:

         // adjust evenly
         computeElevation_Evenly(metricAltitudeSerie, _metricAdjustedAltitudeWithoutSRTM, newAltiStart);
         break;

      case ADJUST_TYPE_START_AND_END:

         // adjust start, end and max

         // first adjust end alti to start alti, secondly adjust max
         computeElevation_End(metricAltitudeSerie, _metricAdjustedAltitudeWithoutSRTM, metricAltitudeSerie[0]);
         computeElevation_StartAndMax(
               _metricAdjustedAltitudeWithoutSRTM,
               _metricAdjustedAltitudeWithoutSRTM,
               newAltiStart,
               newAltiMax);

         break;

      case ADJUST_TYPE_END:

         // adjust end
         computeElevation_End(metricAltitudeSerie, _metricAdjustedAltitudeWithoutSRTM, newAltiEnd);
         break;

      case ADJUST_TYPE_MAX_HEIGHT:

         // adjust max

         computeElevation_StartAndMax(
               metricAltitudeSerie,
               _metricAdjustedAltitudeWithoutSRTM,
               newAltiStart,
               newAltiMax);
         break;

      default:
         break;
      }

      _tourData.clearAltitudeSeries();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.adjust_altitude_dlg_shell_title);
   }

   @Override
   public void create() {

      createDataBackup();

      // create UI widgets
      super.create();

      restoreState();

      setTitle(Messages.adjust_altitude_dlg_dialog_title);
      setMessage(NLS.bind(Messages.adjust_altitude_dlg_dialog_message, TourManager.getTourTitle(_tourData)));

      updateTourChart();
   }

   @Override
   public ChartLayer2ndAltiSerie create2ndAltiLayer() {

      final double[] xDataSerie = _tcc.isShowTimeOnXAxis

            ? _tourData.getTimeSerieDouble()
            : _tourData.getDistanceSerieDouble();

      _chartLayer2ndAltiSerie = new ChartLayer2ndAltiSerie(_tourData, xDataSerie, _tcc, _splineData);

      return _chartLayer2ndAltiSerie;
   }

   /**
    * Create altitude spinner field
    *
    * @param startContainer
    * @return Returns the field
    */
   private Spinner createAltiField(final Composite startContainer) {

      final Spinner spinner = new Spinner(startContainer, SWT.BORDER);
      spinner.setMinimum(0);
      spinner.setMaximum(99999);
      spinner.setIncrement(1);
      spinner.setPageIncrement(1);
      net.tourbook.ui.UI.setWidth(spinner, convertWidthInCharsToPixels(6));

      spinner.addModifyListener(modifyEvent -> {

         if (_isDisableModifyListener) {
            return;
         }

         final Spinner spinner1 = (Spinner) modifyEvent.widget;

         if (UI.UNIT_IS_ELEVATION_FOOT) {

            /**
             * adjust the non metric (imperial) value, this seems to be complicate and it is
             * <p>
             * the altitude data are always saved in the database with the metric system
             * therefore
             * the altitude must always match to the metric system, changing the altitude in the
             * imperial system has always 3 or 4 value differences from one meter to the next
             * meter
             * <p>
             * after many hours of investigation this seems to work
             */

            final float modifiedAlti1 = spinner1.getSelection();
            final float metricAlti = (Float) spinner1.getData(WIDGET_DATA_METRIC_ALTITUDE);

            final float oldAlti = metricAlti / UI.UNIT_VALUE_ELEVATION;
            float newMetricAlti = modifiedAlti1 * UI.UNIT_VALUE_ELEVATION;

            if (modifiedAlti1 > oldAlti) {
               newMetricAlti++;
            }

            spinner1.setData(WIDGET_DATA_METRIC_ALTITUDE, newMetricAlti);

         } else {

            // adjust metric elevation

            final float modifiedAlti2 = spinner1.getSelection();

            spinner1.setData(WIDGET_DATA_METRIC_ALTITUDE, modifiedAlti2);

         }

         onChangeAltitude();
      });

      spinner.addMouseWheelListener(mouseEvent -> {

         if (_isDisableModifyListener) {
            return;
         }

         final Spinner spinner1 = (Spinner) mouseEvent.widget;

         int accelerator = (mouseEvent.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
         accelerator *= (mouseEvent.stateMask & SWT.SHIFT) != 0 ? 5 : 1;
         accelerator *= mouseEvent.count > 0 ? 1 : -1;

         float metricAltitude = (Float) mouseEvent.widget.getData(WIDGET_DATA_METRIC_ALTITUDE);
         metricAltitude = metricAltitude + accelerator;

         _isDisableModifyListener = true;
         {
            spinner1.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(metricAltitude));
            spinner1.setSelection((int) (metricAltitude / UI.UNIT_VALUE_ELEVATION));
         }
         _isDisableModifyListener = false;

         onChangeAltitude();
      });

      spinner.addFocusListener(FocusListener.focusLostAdapter(focusEvent -> onChangeAltitude()));

      return spinner;
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // rename OK button
      final Button buttonOK = getButton(IDialogConstants.OK_ID);

      if (_isSaveTour) {
         buttonOK.setText(Messages.adjust_altitude_btn_save_modified_tour);
      } else {
         buttonOK.setText(Messages.adjust_altitude_btn_update_modified_tour);
      }

      setButtonLayoutData(buttonOK);
   }

   private void createDataBackup() {

      float[] altitudeSerie;

      if (_isCreateDummyElevation) {

         altitudeSerie = new float[_tourData.timeSerie.length];

         for (int index = 0; index < altitudeSerie.length; index++) {

            /*
             * It's better to set a value instead of having 0, but the value should not be too high,
             * I had this idea on 28.08.2010 -> 88
             */
            altitudeSerie[index] = 88;
         }

         // altitude must be set because it's used
         _tourData.altitudeSerie = altitudeSerie;

      } else {

         altitudeSerie = _tourData.altitudeSerie;
      }

      /*
       * Keep a backup of the altitude data because these data will be changed in this dialog
       */
      _backup_MetricAltitudeSerie = Util.createFloatCopy(altitudeSerie);

      final float[][] srtmValues = _tourData.getSRTMValues(true);
      if (srtmValues != null) {

         _backup_SrtmSerie = srtmValues[0];
         _backup_SrtmSerieImperial = srtmValues[1];
         _backup_IsSRTM1Values = _tourData.isSRTM1Values();
      }
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgArea = (Composite) super.createDialogArea(parent);

      _pc = new PixelConverter(parent);

      createUI(dlgArea);

      spline_CreateDefaultSplineData();
      initializeAltitude(_backup_MetricAltitudeSerie);

      return dlgArea;
   }

   private void createUI(final Composite parent) {

      _dlgContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_dlgContainer);
      GridLayoutFactory.fillDefaults().margins(9, 0).applyTo(_dlgContainer);
      {
         createUI_10_AdjustmentType(_dlgContainer);
         createUI_20_TourChart(_dlgContainer);
         createUI_30_Options(_dlgContainer);
      }

      parent.getDisplay().asyncExec(() -> {

         // with the new e4 toolbar update the chart has it's default size (pack() is used) -> resize to window size
//            parent.layout(true, true);
      });
   }

   private void createUI_10_AdjustmentType(final Composite parent) {

      final Composite typeContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(typeContainer);
      GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(0, 0, 5, 0).applyTo(typeContainer);
      {
         // label: adjustment type
         final Label label = new Label(typeContainer, SWT.NONE);
         label.setText(Messages.adjust_altitude_label_adjustment_type);

         // combo: adjustment type
         _comboAdjustmentType = new Combo(typeContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
         _comboAdjustmentType.setVisibleItemCount(20);
         _comboAdjustmentType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectAdjustmentType()));

         // label: adjustment type info
         _lblAdjustmentTypeInfo = new Label(typeContainer, SWT.NONE);
         _lblAdjustmentTypeInfo.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         _lblAdjustmentTypeInfo.setText(UI.SPACE1);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.FILL, SWT.CENTER)
               .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
               .applyTo(_lblAdjustmentTypeInfo);
      }

      // fill combo
      for (final AdjustmentType adjustType : ALL_ADJUSTMENT_TYPES) {

         if (_backup_SrtmSerie == null &&
               (adjustType.__id == ADJUST_TYPE_SRTM_SPLINE
                     || adjustType.__id == ADJUST_TYPE_SRTM
                     || adjustType.__id == ADJUST_TYPE_HORIZONTAL_GEO_POSITION

               )) {

            // skip types which require srtm data
            continue;
         }

         _availableAdjustmentTypes.add(adjustType);

         _comboAdjustmentType.add(adjustType.__visibleName);
      }
   }

   private void createUI_20_TourChart(final Composite parent) {

      _tourChart = new TourChart(parent, SWT.FLAT, null, _state);

      GridDataFactory.fillDefaults()
            .grab(true, true)
            .indent(0, 0)
            .minSize(300, 100)
            .applyTo(_tourChart);

      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);
      _tourChart.setNoToolbarPack();

      _tourChart.setContextProvider(new DialogAdjustAltitudeChartContextProvider(this), true);

      // set title
      _tourChart.addDataModelListener(changedChartDataModel -> changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData)));

      _tourChart.addSliderMoveListener(selectionChartInfo -> {

         if (_isSliderEventDisabled) {
            return;
         }

         onSelectAdjustmentType();
      });

      _tourChart.addChartMouseListener(new MouseAdapter() {

         @Override
         public void mouseDown(final ChartMouseEvent event) {
            onMouseDown(event);
         }

         @Override
         public void mouseMove(final ChartMouseEvent event) {
            onMouseMove(event);
         }

         @Override
         public void mouseUp(final ChartMouseEvent event) {
            onMouseUp(event);
         }

      });

      _tourChart.addXAxisSelectionListener(showTimeOnXAxis -> {
         if (isAdjustmentType_SRTM_SPline()) {
            computeElevation_SRTM_WithSpline();
         }
      });

      /*
       * create chart configuration
       */
      _tcc = new TourChartConfiguration(_state);

      // set altitude visible
      _tcc.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

      // show srtm 1 values
      _tcc.isSRTMDataVisible = true;

      // overwrite x-axis from pref store
      _tcc.setIsShowTimeOnXAxis(
            _prefStore.getString(ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT).equals(TourManager.X_AXIS_TIME));

      // force to show hovered value point value
      _tcc.isShowValuePointValue = true;
   }

   /**
    * create options for each adjustment type in a pagebook
    */
   private void createUI_30_Options(final Composite parent) {

      _pageBookOptions = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageBookOptions);
      {
         _pageEmpty = new Label(_pageBookOptions, SWT.NONE);

         _pageOption_SRTM = createUI_40_Option_WithSRTM(_pageBookOptions);
         _pageOption_SRTM_AndSpline = createUI_50_Option_WithSRTM_AndSpline(_pageBookOptions);
         _pageOption_NoSRTM = createUI_60_Option_WithoutSRTM(_pageBookOptions);
         _pageOption_GeoPosition = createUI_70_Option_GeoPosition(_pageBookOptions);
      }
   }

   private Composite createUI_40_Option_WithSRTM(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * button: update altitude
          */
         final Button btnUpdateAltitude = new Button(container, SWT.NONE);
         btnUpdateAltitude.setText(Messages.adjust_altitude_btn_update_altitude);
         btnUpdateAltitude.setToolTipText(Messages.adjust_altitude_btn_update_altitude_tooltip);
         btnUpdateAltitude.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onUpdate_ElevationSRTM()));
         setButtonLayoutData(btnUpdateAltitude);

         /*
          * button: reset altitude
          */
         final Button btnResetAltitude = new Button(container, SWT.NONE);
         btnResetAltitude.setText(Messages.adjust_altitude_btn_reset_altitude);
         btnResetAltitude.setToolTipText(Messages.adjust_altitude_btn_reset_altitude_tooltip);
         btnResetAltitude.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onReset_Elevation_SRTM()));
         setButtonLayoutData(btnResetAltitude);
      }

      return container;
   }

   private Composite createUI_50_Option_WithSRTM_AndSpline(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_52_SRTMOptions(container);
         createUI_54_SRTMLinks(container);
         createUI_56_SRTMActions(container);
      }

      return container;
   }

   private void createUI_52_SRTMOptions(final Composite parent) {

      final int valueWidth = _pc.convertWidthInCharsToPixels(6);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
//         valueContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Label: Elevation UP
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_AdjustAltitude_Label_ElevationGain);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         }
         {
            /*
             * Value: Elevation UP
             */
            _lblElevation_Up = new Label(container, SWT.TRAIL);
            _lblElevation_Up.setText(UI.SPACE1);
            _lblElevation_Up.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationGain_Before_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .hint(valueWidth, SWT.DEFAULT)
                  .applyTo(_lblElevation_Up);
         }
         {
            /*
             * Label: ->
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(net.tourbook.common.UI.SYMBOL_ARROW_RIGHT);
            GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).indent(10, 0).applyTo(label);
         }
         {
            /*
             * Value: Adjusted elevation UP
             */
            _lblElevation_UpAdjusted = new Label(container, SWT.TRAIL);
            _lblElevation_UpAdjusted.setText(UI.SPACE1);
            _lblElevation_UpAdjusted.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationGain_After_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .hint(valueWidth, SWT.DEFAULT)
                  .applyTo(_lblElevation_UpAdjusted);
         }
         {
            /*
             * Value: Elevation UP delta
             */
            _lblElevation_UpAdjustedDiff = new Label(container, SWT.TRAIL);
            _lblElevation_UpAdjustedDiff.setText(UI.SPACE1);
            _lblElevation_UpAdjustedDiff.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationGain_Diff_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .hint(valueWidth, SWT.DEFAULT)
                  .applyTo(_lblElevation_UpAdjustedDiff);
         }
         {
            /*
             * Label: Unit
             */
            final String unitLabel = net.tourbook.common.UI.SYMBOL_DIFFERENCE_WITH_SPACE
                  + net.tourbook.common.UI.UNIT_LABEL_ELEVATION
                  + UI.SPACE + net.tourbook.common.UI.SYMBOL_ARROW_UP;

            final Label label = new Label(container, SWT.NONE);
            label.setText(unitLabel);
            label.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationGain_Diff_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////

         {
            /*
             * Label: Elevation DOWN
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_AdjustAltitude_Label_ElevationLoss);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         }
         {
            /*
             * Value: Elevation DOWN
             */
            _lblElevation_Down = new Label(container, SWT.TRAIL);
            _lblElevation_Down.setText(UI.SPACE1);
            _lblElevation_Down.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationLoss_Before_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .hint(valueWidth, SWT.DEFAULT)
                  .applyTo(_lblElevation_Down);
         }
         {
            /*
             * Label: ->
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(net.tourbook.common.UI.SYMBOL_ARROW_RIGHT);
            GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).indent(10, 0).applyTo(label);
         }
         {
            /*
             * Value: Adjusted elevation DOWN
             */
            _lblElevation_DownAdjusted = new Label(container, SWT.TRAIL);
            _lblElevation_DownAdjusted.setText(UI.SPACE1);
            _lblElevation_DownAdjusted.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationLoss_After_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .hint(valueWidth, SWT.DEFAULT)
                  .applyTo(_lblElevation_DownAdjusted);
         }
         {
            /*
             * Value: Elevation UP delta
             */
            _lblElevation_DownAdjustedDiff = new Label(container, SWT.TRAIL);
            _lblElevation_DownAdjustedDiff.setText(UI.SPACE1);
            _lblElevation_DownAdjustedDiff.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationLoss_Diff_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .hint(valueWidth, SWT.DEFAULT)
                  .applyTo(_lblElevation_DownAdjustedDiff);
         }
         {
            /*
             * Label: Unit
             */
            final String unitLabel = net.tourbook.common.UI.SYMBOL_DIFFERENCE_WITH_SPACE
                  + net.tourbook.common.UI.UNIT_LABEL_ELEVATION
                  + UI.SPACE + net.tourbook.common.UI.SYMBOL_ARROW_DOWN;

            final Label label = new Label(container, SWT.NONE);
            label.setText(unitLabel);
            label.setToolTipText(Messages.Dialog_AdjustAltitude_Label_ElevationLoss_Diff_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         }
      }
   }

   private void createUI_54_SRTMLinks(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Link: Adjust END to the START elevation
             */
            _linkSRTM_AdjustEndToStart = new Link(container, SWT.NONE);
            _linkSRTM_AdjustEndToStart.setText(Messages.Dialog_AdjustAltitude_Link_SetLastPointToSRTM);
            _linkSRTM_AdjustEndToStart.setToolTipText(Messages.Dialog_AdjustAltitude_Link_SetLastPointToSRTM_Tooltip);
            _linkSRTM_AdjustEndToStart.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSpline_SetEndElevationToSRTM()));
            GridDataFactory.swtDefaults().span(6, 1).applyTo(_linkSRTM_AdjustEndToStart);
         }
         {
            /*
             * Link: Select whole tour
             */
            _linkSRTM_SelectWholeTour = new Link(container, SWT.NONE);
            _linkSRTM_SelectWholeTour.setText(Messages.Dialog_AdjustAltitude_Link_ApproachWholeTour);
            _linkSRTM_SelectWholeTour.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSpline_SelectWholeTour()));
            GridDataFactory.swtDefaults().span(6, 1).applyTo(_linkSRTM_SelectWholeTour);
         }
      }
   }

   private Composite createUI_56_SRTMActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.BEGINNING)
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         /*
          * button: update altitude
          */
         final Button btnUpdateAltitude = new Button(container, SWT.NONE);
         btnUpdateAltitude.setText(Messages.adjust_altitude_btn_update_altitude);
         btnUpdateAltitude.setToolTipText(Messages.adjust_altitude_btn_update_altitude_tooltip);
         btnUpdateAltitude.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onUpdate_ElevationSRTMSpline()));
         setButtonLayoutData(btnUpdateAltitude);

         /*
          * button: reset altitude
          */
         final Button btnResetAltitude = new Button(container, SWT.NONE);
         btnResetAltitude.setText(Messages.adjust_altitude_btn_reset_altitude_and_points);
         btnResetAltitude.setToolTipText(Messages.adjust_altitude_btn_reset_altitude_and_points_tooltip);
         btnResetAltitude.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onReset_Elevation_SRTMSpline()));
         setButtonLayoutData(btnResetAltitude);

         /*
          * button: remove all points
          */
         _btnSRTMRemoveAllPoints = new Button(container, SWT.NONE);
         _btnSRTMRemoveAllPoints.setText(Messages.adjust_altitude_btn_srtm_remove_all_points);
         _btnSRTMRemoveAllPoints.setToolTipText(Messages.adjust_altitude_btn_srtm_remove_all_points_tooltip);
         _btnSRTMRemoveAllPoints.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            spline_CreateDefaultSplineData();
            onSelectAdjustmentType();
         }));
         setButtonLayoutData(_btnSRTMRemoveAllPoints);
      }
      return container;
   }

   private Composite createUI_60_Option_WithoutSRTM(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         createUI_61_StartEnd(container);
         createUI_62_Max(container);
         createUI_63_Actions(container);
      }

      return container;
   }

   private void createUI_61_StartEnd(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * field: start altitude
          */
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_AdjustAltitude_Label_start_altitude);
         label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

         _spinnerNewStartAlti = createAltiField(container);
         _spinnerNewStartAlti.setData(WIDGET_DATA_ALTI_ID, Float.valueOf(ALTI_ID_START));
         _spinnerNewStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

         _lblOldStartAlti = new Label(container, SWT.NONE);
         _lblOldStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

         /*
          * field: end altitude
          */
         label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_AdjustAltitude_Label_end_altitude);
         label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

         _spinnerNewEndAlti = createAltiField(container);
         _spinnerNewEndAlti.setData(WIDGET_DATA_ALTI_ID, Float.valueOf(ALTI_ID_END));
         _spinnerNewEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

         _lblOldEndAlti = new Label(container, SWT.NONE);
         _lblOldEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);
      }

   }

   private void createUI_62_Max(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .align(SWT.BEGINNING, SWT.FILL)
            .indent(40, 0)
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * field: max altitude
          */
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_AdjustAltitude_Label_max_altitude);
         label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

         _spinnerNewMaxAlti = createAltiField(container);
         _spinnerNewMaxAlti.setData(WIDGET_DATA_ALTI_ID, Float.valueOf(ALTI_ID_MAX));
         _spinnerNewMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

         _lblOldMaxAlti = new Label(container, SWT.NONE);
         _lblOldMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

         /*
          * group: keep start/bottom
          */
         final Group groupKeep = new Group(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(3, 1).applyTo(groupKeep);
         GridLayoutFactory.swtDefaults().applyTo(groupKeep);
         groupKeep.setText(Messages.Dlg_AdjustAltitude_Group_options);
         {
            final SelectionListener keepButtonSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeAltitude());

            _rdoKeepBottom = new Button(groupKeep, SWT.RADIO);
            _rdoKeepBottom.setText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude);
            _rdoKeepBottom.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude_tooltip);
            _rdoKeepBottom.setLayoutData(new GridData());
            _rdoKeepBottom.addSelectionListener(keepButtonSelectionListener);
            // fRadioKeepBottom.setSelection(true);

            _rdoKeepStart = new Button(groupKeep, SWT.RADIO);
            _rdoKeepStart.setText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude);
            _rdoKeepStart.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude_tooltip);
            _rdoKeepStart.setLayoutData(new GridData());
            _rdoKeepStart.addSelectionListener(keepButtonSelectionListener);
         }
      }
   }

   private void createUI_63_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(20, 0).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * button: reset altitude
          */
         _btnResetAltitude = new Button(container, SWT.NONE);
         _btnResetAltitude.setText(Messages.adjust_altitude_btn_reset_altitude);
         _btnResetAltitude.setToolTipText(Messages.adjust_altitude_btn_reset_altitude_tooltip);
         _btnResetAltitude.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onReset_Elevation()));
         setButtonLayoutData(_btnResetAltitude);

         /*
          * button: update altitude
          */
         _btnUpdateAltitude = new Button(container, SWT.NONE);
         _btnUpdateAltitude.setText(Messages.adjust_altitude_btn_update_altitude);
         _btnUpdateAltitude.setToolTipText(Messages.adjust_altitude_btn_update_altitude_tooltip);
         _btnUpdateAltitude.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onUpdate_Elevation()));
         setButtonLayoutData(_btnUpdateAltitude);
      }
   }

   private Composite createUI_70_Option_GeoPosition(final PageBook parent) {

      final int valueWidth = _pc.convertWidthInCharsToPixels(4);

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.Adjust_Altitude_Group_GeoPosition);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         /*
          * label: adjusted slices
          */
         final Label label = new Label(group, SWT.NONE);
         label.setText(Messages.Adjust_Altitude_Label_GeoPosition_Slices);

         /*
          * label: slice value
          */
         _lblSliceValue = new Label(group, SWT.TRAIL);
         GridDataFactory.fillDefaults()
               .align(SWT.END, SWT.CENTER)
               .hint(valueWidth, SWT.DEFAULT)
               .applyTo(_lblSliceValue);

         /*
          * scale: slice position
          */
         _scaleSlicePos = new Scale(group, SWT.HORIZONTAL);
         _scaleSlicePos.setMinimum(0);
         _scaleSlicePos.setMaximum(MAX_ADJUST_GEO_POS_SLICES * 2);
         _scaleSlicePos.setPageIncrement(5);
         _scaleSlicePos.addListener(SWT.MouseDoubleClick, event -> onDoubleClickGeoPos(event.widget));
         _scaleSlicePos.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectSlicePosition()));
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleSlicePos);
      }

      return group;
   }

   private void enableFieldsWithoutSRTM() {

      // set adjustment type and enable the field(s) which can be modified
      switch (getSelectedAdjustmentType().__id) {

      case ADJUST_TYPE_START_AND_END:

         _spinnerNewStartAlti.setEnabled(true);
         _spinnerNewEndAlti.setEnabled(false);
         _spinnerNewMaxAlti.setEnabled(true);
         _rdoKeepStart.setEnabled(true);
         _rdoKeepBottom.setEnabled(true);

         break;

      case ADJUST_TYPE_WHOLE_TOUR:

         _spinnerNewStartAlti.setEnabled(true);
         _spinnerNewEndAlti.setEnabled(false);
         _spinnerNewMaxAlti.setEnabled(false);
         _rdoKeepStart.setEnabled(false);
         _rdoKeepBottom.setEnabled(false);

         break;

      case ADJUST_TYPE_END:

         _spinnerNewStartAlti.setEnabled(false);
         _spinnerNewEndAlti.setEnabled(true);
         _spinnerNewMaxAlti.setEnabled(false);
         _rdoKeepStart.setEnabled(false);
         _rdoKeepBottom.setEnabled(false);

         break;

      case ADJUST_TYPE_MAX_HEIGHT:

         _spinnerNewStartAlti.setEnabled(false);
         _spinnerNewEndAlti.setEnabled(false);
         _spinnerNewMaxAlti.setEnabled(true);
         _rdoKeepStart.setEnabled(true);
         _rdoKeepBottom.setEnabled(true);

         break;

      default:
         break;
      }
   }

   private void enableFieldsWithSRTM() {

      /*
       * srtm options
       */
      if (_splineData != null && _splineData.isPointMovable != null) {
         _btnSRTMRemoveAllPoints.setEnabled(_splineData.isPointMovable.length > 3);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
   }

   @Override
   protected Point getInitialSize() {
      final Point calculatedSize = super.getInitialSize();
      if (calculatedSize.x < 600) {
         calculatedSize.x = 600;
      }
      if (calculatedSize.y < 600) {
         calculatedSize.y = 600;
      }
      return calculatedSize;
   }

   /**
    * @return the adjustment type which is selected in the combox
    */
   private AdjustmentType getSelectedAdjustmentType() {

      int comboIndex = _comboAdjustmentType.getSelectionIndex();

      if (comboIndex == -1) {
         comboIndex = 0;
         _comboAdjustmentType.select(comboIndex);
      }

      return _availableAdjustmentTypes.get(comboIndex);
   }

   /**
    * reset altitudes to it's original values
    *
    * @param metricAltitudeSerie
    */
   private void initializeAltitude(final float[] metricAltitudeSerie) {

      final int serieLength = metricAltitudeSerie.length;

      final float startAlti = metricAltitudeSerie[0];
      final float endAlti = metricAltitudeSerie[serieLength - 1];
      float maxAlti = startAlti;

      /*
       * get altitude from original data, calculate max altitude
       */
      _metricAdjustedAltitudeWithoutSRTM = new float[serieLength];

      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float altitude = metricAltitudeSerie[serieIndex];
         _metricAdjustedAltitudeWithoutSRTM[serieIndex] = altitude;

         if (altitude > maxAlti) {
            maxAlti = altitude;
         }
      }

      /*
       * update UI
       */
      _lblOldStartAlti.setText(Integer.toString((int) (startAlti / UI.UNIT_VALUE_ELEVATION)));
      _lblOldEndAlti.setText(Integer.toString((int) (endAlti / UI.UNIT_VALUE_ELEVATION)));
      _lblOldMaxAlti.setText(Integer.toString((int) (maxAlti / UI.UNIT_VALUE_ELEVATION)));

      _lblOldStartAlti.pack(true);
      _lblOldEndAlti.pack(true);
      _lblOldMaxAlti.pack(true);

      _isDisableModifyListener = true;
      {
         _spinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(startAlti));
         _spinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ELEVATION));

         _spinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(endAlti));
         _spinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ELEVATION));

         _spinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(maxAlti));
         _spinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ELEVATION));
      }
      _isDisableModifyListener = false;

   }

   boolean isActionEnabledCreateSplinePoint(final int mouseDownDevPositionX) {

      final SplineDrawingData drawingData = _chartLayer2ndAltiSerie.getDrawingData();

      final double scaleX = drawingData.scaleX;
      final float devX = drawingData.devGraphValueXOffset + mouseDownDevPositionX;
      final double graphX = devX / scaleX;

      final double graphXMin = _spline_BorderLeft_xValue;
      final double graphXMax = _spline_BorderRight_xValue;

      // check min/max value
      if (graphX <= graphXMin || graphX >= graphXMax) {

         // click is outside of the allowed area

         return false;

      } else {

         return true;
      }
   }

   private boolean isAdjustmentType_SRTM_SPline() {
      return getSelectedAdjustmentType().__id == ADJUST_TYPE_SRTM_SPLINE;
   }

   private boolean isSrtmDownloadValid() {

      final String password = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD);
      final String username = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME);
      if (password.trim().length() == 0 || username.trim().length() == 0) {
         return false;
      }

      final long validationDate = _prefStore.getLong(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE);
      if (validationDate < 0) {
         return false;
      }

      return true;
   }

   @Override
   protected void okPressed() {

      saveTour();

      super.okPressed();
   }

   private void onChangeAltitude() {

      // calcuate new altitude values
      computeElevation_WithoutSRTM();

      enableFieldsWithoutSRTM();

      // set new values into the fields which can change the altitude
      updateUI_ElevationFields();

      updateUI_2ndLayer();
   }

   private void onDoubleClickGeoPos(final Widget widget) {

      final Scale scale = (Scale) widget;
      final int max = scale.getMaximum();

      scale.setSelection(max / 2);

      onSelectSlicePosition();
   }

   private void onMouseDown(final ChartMouseEvent mouseEvent) {

      if (_chartLayer2ndAltiSerie == null) {
         return;
      }

      final Rectangle[] pointHitRectangles = _chartLayer2ndAltiSerie.getPointHitRectangels();
      if (pointHitRectangles == null) {
         return;
      }

      _pointHitIndex = -1;

      // check if the mouse hits a spline point
      for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

         if (pointHitRectangles[pointIndex].contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

            _pointHitIndex = pointIndex;

            mouseEvent.isWorked = true;

            return;
         }
      }
   }

   private void onMouseMove(final ChartMouseEvent mouseEvent) {

      if (_chartLayer2ndAltiSerie == null) {
         return;
      }

      final Rectangle[] pointHitRectangles = _chartLayer2ndAltiSerie.getPointHitRectangels();
      if (pointHitRectangles == null) {
         return;
      }

      if (_pointHitIndex != -1) {

         // point is moved

         splinePoint_MovePoint(mouseEvent);

         onSelectAdjustmentType();

         mouseEvent.isWorked = true;

      } else {

         // point is not moved, check if the mouse hits a spline point

         for (final Rectangle pointHitRectangle : pointHitRectangles) {

            if (pointHitRectangle.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {
               mouseEvent.isWorked = true;
               break;
            }
         }
      }

      if (mouseEvent.isWorked) {

         // show dragged cursor

         mouseEvent.cursor = ChartCursor.Dragged;
      }
   }

   private void onMouseUp(final ChartMouseEvent mouseEvent) {

      if (_pointHitIndex == -1) {
         return;
      }

      if (_canDeletePoint) {

         _canDeletePoint = false;

         splinePoint_DeletedPoint();

         // redraw layer to update the hit rectangles
         onSelectAdjustmentType();
      }

      mouseEvent.isWorked = true;
      _pointHitIndex = -1;
   }

   /**
    * display altitude with the original altitude data
    */
   private void onReset_Elevation() {

      _altiMaxDiff = 0;
      _altiStartDiff = 0;
      _prevAltiStart = 0;
      _prevAltiMax = 0;

      _tourData.altitudeSerie = Util.createFloatCopy(_backup_MetricAltitudeSerie);
      _tourData.clearAltitudeSeries();

      initializeAltitude(_backup_MetricAltitudeSerie);
      onChangeAltitude();

      updateTourChart();
   }

   private void onReset_Elevation_SRTM() {

      _tourData.altitudeSerie = Util.createFloatCopy(_backup_MetricAltitudeSerie);
      _tourData.clearAltitudeSeries();

      computeElevation_SRTM();
      updateTourChart();
   }

   private void onReset_Elevation_SRTMSpline() {

      _tourData.altitudeSerie = Util.createFloatCopy(_backup_MetricAltitudeSerie);
      _tourData.clearAltitudeSeries();

      /*
       * set all points to y=0
       */
      final double[] allPosYRelative = _splineData.posY_RelativeValues;
      for (int pointIndex = 0; pointIndex < allPosYRelative.length; pointIndex++) {
         allPosYRelative[pointIndex] = 0;
      }

      _isSetEndElevation_To_SRTMValue = true;

      computeElevation_SRTM_WithSpline();
      updateTourChart();
   }

   private void onSelectAdjustmentType() {

      // hide all 2nd data series
      _tourData.dataSerieAdjustedAlti = null;
      _tourData.dataSerieDiffTo2ndAlti = null;
      _tourData.dataSerie2ndAlti = null;
      _tourData.dataSerieSpline = null;
      _tourData.setSRTMValues(_backup_SrtmSerie, _backup_SrtmSerieImperial, _backup_IsSRTM1Values);

      // hide splines
      _splineData.splinePoint_DataSerieIndex = null;

      _lblAdjustmentTypeInfo.setText(UI.EMPTY_STRING);

      final int adjustmentType = getSelectedAdjustmentType().__id;
      switch (adjustmentType) {

      case ADJUST_TYPE_HORIZONTAL_GEO_POSITION:

         _pageBookOptions.showPage(_pageOption_GeoPosition);

         onSelectSlicePosition();

         break;

      case ADJUST_TYPE_SRTM:

         if (isSrtmDownloadValid() == false) {
            _lblAdjustmentTypeInfo.setText(Messages.Dialog_AdjustAltitude_Label_SrtmIsInvalid);
         }

         _pageBookOptions.showPage(_pageOption_SRTM);

         computeElevation_SRTM();

         break;

      case ADJUST_TYPE_SRTM_SPLINE:

         if (isSrtmDownloadValid() == false) {
            _lblAdjustmentTypeInfo.setText(Messages.Dialog_AdjustAltitude_Label_SrtmIsInvalid);
         }

         _pageBookOptions.showPage(_pageOption_SRTM_AndSpline);

         // display splines
         computeElevation_SRTM_WithSpline();

         break;

      case ADJUST_TYPE_WHOLE_TOUR:
      case ADJUST_TYPE_START_AND_END:
      case ADJUST_TYPE_END:
      case ADJUST_TYPE_MAX_HEIGHT:

         _pageBookOptions.showPage(_pageOption_NoSRTM);
         onReset_Elevation();

         break;

      default:
         _pageBookOptions.showPage(_pageEmpty);
         break;
      }

      /*
       * layout is a performance hog, optimize it
       */
      if (_oldAdjustmentType != adjustmentType) {
         _dlgContainer.layout(true);
      }

      _oldAdjustmentType = adjustmentType;

      updateUI_2ndLayer();
   }

   private void onSelectSlicePosition() {

      int diffGeoSlices = _scaleSlicePos.getSelection() - MAX_ADJUST_GEO_POS_SLICES;
      final int serieLength = _tourData.timeSerie.length;

      // adjust slices to bounds
      if (diffGeoSlices > serieLength) {
         diffGeoSlices = serieLength - 1;
      } else if (-diffGeoSlices > serieLength) {
         diffGeoSlices = -(serieLength - 1);
      }

      /*
       * adjust srtm data
       */
      final float[] adjustedSRTM = new float[serieLength];
      final float[] adjustedSRTMImperial = new float[serieLength];

      final int srcPos = diffGeoSlices >= 0 ? 0 : -diffGeoSlices;
      final int destPos = diffGeoSlices >= 0 ? diffGeoSlices : 0;
      final int adjustedLength = serieLength - (diffGeoSlices < 0 ? -diffGeoSlices : diffGeoSlices);

      System.arraycopy(_backup_SrtmSerie, srcPos, adjustedSRTM, destPos, adjustedLength);
      System.arraycopy(_backup_SrtmSerieImperial, srcPos, adjustedSRTMImperial, destPos, adjustedLength);

      _tourData.setSRTMValues(adjustedSRTM, adjustedSRTMImperial, _backup_IsSRTM1Values);

      final float[] metricAltiSerie = _tourData.altitudeSerie;
      final float[] diffTo2ndAlti = _tourData.dataSerieDiffTo2ndAlti = new float[serieLength];

      // get altitude diff serie
      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float srtmAltitude = adjustedSRTM[serieIndex];

         // ignore diffs which are outside of the adjusted srtm
         if ((diffGeoSlices >= 0 && serieIndex >= diffGeoSlices)
               || (diffGeoSlices < 0 && serieIndex < (serieLength - (-diffGeoSlices)))) {

            final float sliceDiff = metricAltiSerie[serieIndex] - srtmAltitude;
            diffTo2ndAlti[serieIndex] = sliceDiff;
         }
      }

      updateUI_GeoPos();
      updateTourChart();

      // this is not working, srtm data must be adjusted !!!
      // update only the second layer, this is much faster
//      _tourChart.update2ndAltiLayer(this, true);
   }

   /**
    * SRTM 1 or SRTM 3 resolution is selected -> recalculate elevation diff values
    *
    * @param isUseSRTM1Values
    */
   private void onSelectSRTMResolution(final boolean isUseSRTM1Values) {

      // reset existing SRTM series
      _tourData.setSRTMValues(null, null, isUseSRTM1Values);

      // recreate SRTM values
      final float[][] srtmValues = _tourData.getSRTMValues(isUseSRTM1Values);
      if (srtmValues != null) {

         _backup_SrtmSerie = srtmValues[0];
         _backup_SrtmSerieImperial = srtmValues[1];
         _backup_IsSRTM1Values = _tourData.isSRTM1Values();
      }

      onSelectSlicePosition();
   }

   private void onSpline_SelectWholeTour() {

      _tourChart.setXSliderPosition(new SelectionChartXSliderPosition(
            _tourChart,
            SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER,
            SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER));
   }

   private void onSpline_SetEndElevationToSRTM() {

      _isSetEndElevation_To_SRTMValue = true;

      // update UI
      computeElevation_SRTM_WithSpline();
      updateTourChart();
   }

   /**
    * display altitude with the adjusted altitude data
    */
   private void onUpdate_Elevation() {

      _tourData.altitudeSerie = Util.createFloatCopy(_metricAdjustedAltitudeWithoutSRTM);
      _tourData.clearAltitudeSeries();

      updateTourChart();
   }

   private void onUpdate_ElevationSRTM() {

      saveTour_10_AdjustSRTM();
      _tourData.clearAltitudeSeries();

      computeElevation_SRTM();

      updateTourChart();
   }

   private void onUpdate_ElevationSRTMSpline() {

      saveTour_10_AdjustSRTM();
      _tourData.clearAltitudeSeries();

      /*
       * set all points to y=0
       */
      final double[] posYRelative = _splineData.posY_RelativeValues;
      for (int pointIndex = 0; pointIndex < posYRelative.length; pointIndex++) {
         posYRelative[pointIndex] = 0;
      }

      computeElevation_SRTM_WithSpline();

      updateTourChart();
   }

   private void restoreState() {

      // get previous selected adjustment type, use first type if not found
      final int prefAdjustType = _prefStore.getInt(PREF_ADJUST_TYPE);
      int comboIndex = 0;
      int typeIndex = 0;
      for (final AdjustmentType availAdjustType : _availableAdjustmentTypes) {
         if (prefAdjustType == availAdjustType.__id) {
            comboIndex = typeIndex;
            break;
         }
         typeIndex++;
      }
      _comboAdjustmentType.select(comboIndex);

      // get max options
      boolean isKeepStart;
      if (_prefStore.contains(PREF_KEEP_START)) {
         isKeepStart = _prefStore.getBoolean(PREF_KEEP_START);
      } else {
         isKeepStart = true;
      }
      _rdoKeepStart.setSelection(isKeepStart);
      _rdoKeepBottom.setSelection(!isKeepStart);

      /*
       * scale: geo position
       */
      int scaleGeoPos;
      if (_prefStore.contains(PREF_SCALE_GEO_POSITION)) {
         scaleGeoPos = _prefStore.getInt(PREF_SCALE_GEO_POSITION);
      } else {
         scaleGeoPos = MAX_ADJUST_GEO_POS_SLICES;
      }
      _scaleSlicePos.setSelection(scaleGeoPos);

      /*
       * Delay selected adjustment type that the slider are positioned correctly
       */
      Display.getCurrent().timerExec(100, this::onSelectAdjustmentType);
   }

   private void saveState() {

      _prefStore.setValue(PREF_ADJUST_TYPE, getSelectedAdjustmentType().__id);
      _prefStore.setValue(ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT,
            _tcc.isShowTimeOnXAxis
                  ? TourManager.X_AXIS_TIME
                  : TourManager.X_AXIS_DISTANCE);

      _prefStore.setValue(PREF_KEEP_START, _rdoKeepStart.getSelection());
      _prefStore.setValue(PREF_SCALE_GEO_POSITION, _scaleSlicePos.getSelection());
   }

   /**
    * Tour is saved in the database after the dialog is closed, this method prepares the data.
    */
   private void saveTour() {

      _isTourSaved = true;

      switch (getSelectedAdjustmentType().__id) {
      case ADJUST_TYPE_SRTM:
      case ADJUST_TYPE_SRTM_SPLINE:
         saveTour_10_AdjustSRTM();
         break;

      case ADJUST_TYPE_HORIZONTAL_GEO_POSITION:
         saveTour_20_AdjustGeoSlicePosition();
         break;

      case ADJUST_TYPE_WHOLE_TOUR:
      case ADJUST_TYPE_START_AND_END:
      case ADJUST_TYPE_END:
      case ADJUST_TYPE_MAX_HEIGHT:
         _tourData.altitudeSerie = _metricAdjustedAltitudeWithoutSRTM;
         break;

      default:
         break;
      }

      // force the imperial altitude series to be recomputed
      _tourData.clearAltitudeSeries();

      // adjust altitude up/down values
      _tourData.computeAltitudeUpDown();

      // compute max altitude which requires other computed data series, e.g. speed (highly complex)
      _tourData.computeComputedValues();
   }

   private void saveTour_10_AdjustSRTM() {

      final float[] dataSerieAdjustedAlti = _tourData.dataSerieAdjustedAlti;
      final float[] newAltitudeSerie = _tourData.altitudeSerie = new float[dataSerieAdjustedAlti.length];

      for (int serieIndex = 0; serieIndex < dataSerieAdjustedAlti.length; serieIndex++) {
         newAltitudeSerie[serieIndex] = dataSerieAdjustedAlti[serieIndex] * UI.UNIT_VALUE_ELEVATION;
      }
   }

   private void saveTour_20_AdjustGeoSlicePosition() {

      int diffGeoSlices = _scaleSlicePos.getSelection() - MAX_ADJUST_GEO_POS_SLICES;
      final int serieLength = _tourData.timeSerie.length;

      // adjust slices to bounds
      if (diffGeoSlices > serieLength) {
         diffGeoSlices = serieLength - 1;
      } else if (-diffGeoSlices > serieLength) {
         diffGeoSlices = -(serieLength - 1);
      }

      final double[] oldLatSerie = _tourData.latitudeSerie;
      final double[] oldLonSerie = _tourData.longitudeSerie;

      final double[] newLatSerie = _tourData.latitudeSerie = new double[serieLength];
      final double[] newLonSerie = _tourData.longitudeSerie = new double[serieLength];

      final int srcPos = diffGeoSlices >= 0 ? 0 : -diffGeoSlices;
      final int destPos = diffGeoSlices >= 0 ? diffGeoSlices : 0;
      final int adjustedLength = serieLength - (diffGeoSlices < 0 ? -diffGeoSlices : diffGeoSlices);

      System.arraycopy(oldLatSerie, srcPos, newLatSerie, destPos, adjustedLength);
      System.arraycopy(oldLonSerie, srcPos, newLonSerie, destPos, adjustedLength);

      // fill gaps with starting/ending position
      if (diffGeoSlices >= 0) {

         final double startLat = oldLatSerie[0];
         final double startLon = oldLonSerie[0];

         for (int serieIndex = 0; serieIndex < diffGeoSlices; serieIndex++) {
            newLatSerie[serieIndex] = startLat;
            newLonSerie[serieIndex] = startLon;
         }

      } else {

         // diffGeoSlices < 0

         final int lastIndex = serieLength - 1;
         final int validEndIndex = lastIndex - (-diffGeoSlices);
         final double endLat = oldLatSerie[lastIndex];
         final double endLon = oldLonSerie[lastIndex];

         for (int serieIndex = validEndIndex; serieIndex < serieLength; serieIndex++) {
            newLatSerie[serieIndex] = endLat;
            newLonSerie[serieIndex] = endLon;
         }
      }

      _tourData.computeGeo_Bounds();
   }

   /**
    * Create spline values, these are 3 points at start/middle/end
    *
    * @param altiDiff
    * @param sliderDistance
    * @return
    */
   private void spline_CreateDefaultSplineData() {

      _splineData = new SplineData();

      final float borderValue_Left = -0.0000000000001f;
      final float borderValue_Right = 1.0000000000001f;

      final int numSplinePoints = 3;

      final boolean[] isMovable = _splineData.isPointMovable = new boolean[numSplinePoints];
      isMovable[0] = false;
      isMovable[1] = true;
      isMovable[2] = false;

      final double[] allRelativePosX = _splineData.posX_RelativeValues = new double[numSplinePoints];
      final double[] allRelativePosY = _splineData.posY_RelativeValues = new double[numSplinePoints];

      allRelativePosX[0] = borderValue_Left;
      allRelativePosX[1] = 0.5f;
      allRelativePosX[2] = borderValue_Right;

      allRelativePosY[0] = 0;
      allRelativePosY[1] = 0;
      allRelativePosY[2] = 0;

      final double[] allSplineMinX = _splineData.posX_GraphMinValues = new double[numSplinePoints];
      final double[] allSplineMaxX = _splineData.posX_GraphMaxValues = new double[numSplinePoints];
      allSplineMinX[0] = borderValue_Left;
      allSplineMaxX[0] = borderValue_Left;
      allSplineMinX[1] = 0;
      allSplineMaxX[1] = 0;
      allSplineMinX[2] = borderValue_Right;
      allSplineMaxX[2] = borderValue_Right;

      _splineData.posX_GraphValues = new double[numSplinePoints];
      _splineData.posY_GraphValues = new double[numSplinePoints];

      /*
       * Set elevation of the last point to SRTM elevation
       */
      final double[] xDataSerie = _tcc.isShowTimeOnXAxis
            ? _tourData.getTimeSerieDouble()
            : _tourData.getDistanceSerieDouble();

      final int lastTimeSliceIndex = xDataSerie.length - 1;
      final float[] yDataElevationSerie = _tourData.altitudeSerie;

      if (_backup_SrtmSerie == null || yDataElevationSerie == null) {

         /*
          * NPE occurred, it is likely that SRTM data were not available, could not verify it but
          * during this testing the SRTM server was sometimes not available
          */

         return;
      }

      final float firstTimeSlice_ElevationDiff = _backup_SrtmSerie[0] - yDataElevationSerie[0];
      final double lastTimeSlice_ElevationDiff = _backup_SrtmSerie[lastTimeSliceIndex] - yDataElevationSerie[lastTimeSliceIndex];
      final double graphRelative = firstTimeSlice_ElevationDiff == 0
            ? 0
            : lastTimeSlice_ElevationDiff / firstTimeSlice_ElevationDiff;

      _splineData.posY_RelativeValues[2] = graphRelative;
   }

   /**
    * @return Returns a cubic spline instance from the spline data which is performing the
    *         interpolation.
    */
   private CubicSpline spline_CreateSplinePerformer() {

      final double[] allSplineX = _splineData.posX_GraphValues;
      final double[] allSplineY = _splineData.posY_GraphValues;

      final double[] allSplineMinX = _splineData.posX_GraphMinValues;
      final double[] allSplineMaxX = _splineData.posX_GraphMaxValues;

      final double[] allRelativPosX = _splineData.posX_RelativeValues;
      final double[] allRelativePosY = _splineData.posY_RelativeValues;

      final int numPoints = _splineData.isPointMovable.length;

      for (int pointIndex = 0; pointIndex < numPoints; pointIndex++) {

         final double pointRelativePosX = allRelativPosX[pointIndex];
         final double pointAbsoluteValueX = _spline_BorderLeft_xValue + (_spline_BorderRight_xValue - _spline_BorderLeft_xValue) * pointRelativePosX;

         allSplineX[pointIndex] = pointAbsoluteValueX;
         allSplineY[pointIndex] = allRelativePosY[pointIndex] * _firstTimeSlice_ElevationDiff;

         allSplineMinX[pointIndex] = _spline_BorderLeft_xValue;
         allSplineMaxX[pointIndex] = _spline_BorderRight_xValue;
      }

      return new CubicSpline(allSplineX, allSplineY);
   }

   private void spline_SetEndElevation_To_SRTMValue() {

      final float[] yDataElevationSerie = _tourData.altitudeSerie;
      final int[] splinePointIndex = _splineData.splinePoint_DataSerieIndex;

      // get serie index from the last horizontal spline point, the serie index array is not sorted by value !!!
      int serieIndexAtTheEnd = 0;
      int relativePosYIndex = 0;

      for (int pointSerieIndex = 0; pointSerieIndex < splinePointIndex.length; pointSerieIndex++) {

         final int splineIndexValue = splinePointIndex[pointSerieIndex];

         if (splineIndexValue > serieIndexAtTheEnd) {

            serieIndexAtTheEnd = splineIndexValue;
            relativePosYIndex = pointSerieIndex;
         }
      }

      /*
       * Set new relative position
       */
      final double lastTimeSlice_ElevationDiff = _backup_SrtmSerie[serieIndexAtTheEnd] - yDataElevationSerie[serieIndexAtTheEnd];
      final double graphRelative = lastTimeSlice_ElevationDiff / _firstTimeSlice_ElevationDiff;

      _splineData.posY_RelativeValues[relativePosYIndex] = graphRelative;
   }

   private void splinePoint_DeletedPoint() {

      if (_splineData.isPointMovable.length <= 3) {
         // prevent deleting less than 3 points
         return;
      }

      final boolean[] oldIsPointMovable = _splineData.isPointMovable;
      final double[] oldPosX = _splineData.posX_RelativeValues;
      final double[] oldPosY = _splineData.posY_RelativeValues;
      final double[] oldXValues = _splineData.posX_GraphValues;
      final double[] oldYValues = _splineData.posY_GraphValues;
      final double[] oldXMinValues = _splineData.posX_GraphMinValues;
      final double[] oldXMaxValues = _splineData.posX_GraphMaxValues;

      final int newLength = oldIsPointMovable.length - 1;

      final boolean[] newIsPointMovable = _splineData.isPointMovable = new boolean[newLength];
      final double[] newPosX = _splineData.posX_RelativeValues = new double[newLength];
      final double[] newPosY = _splineData.posY_RelativeValues = new double[newLength];
      final double[] newXValues = _splineData.posX_GraphValues = new double[newLength];
      final double[] newYValues = _splineData.posY_GraphValues = new double[newLength];
      final double[] newXMinValues = _splineData.posX_GraphMinValues = new double[newLength];
      final double[] newXMaxValues = _splineData.posX_GraphMaxValues = new double[newLength];

      int srcPos, destPos, length;

      if (_pointHitIndex == 0) {

         // remove first point

         srcPos = 1;
         destPos = 0;
         length = newLength;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

      } else if (_pointHitIndex == newLength) {

         // remove last point

         srcPos = 0;
         destPos = 0;
         length = newLength;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

      } else {

         // remove points in the middle

         srcPos = 0;
         destPos = 0;
         length = _pointHitIndex;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

         srcPos = _pointHitIndex + 1;
         destPos = _pointHitIndex;
         length = newLength - _pointHitIndex;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);
      }
   }

   /**
    * Compute relative position of the moved point
    *
    * @param mouseEvent
    */
   private void splinePoint_MovePoint(final ChartMouseEvent mouseEvent) {

      if (_pointHitIndex == -1) {
         return;
      }

      final boolean isPointMovable = _splineData.isPointMovable[_pointHitIndex];

      final SplineDrawingData drawingData = _chartLayer2ndAltiSerie.getDrawingData();
      final double scaleX = drawingData.scaleX;
      final double scaleY = drawingData.scaleY;

      double devX = drawingData.devGraphValueXOffset + mouseEvent.devXMouse;
      final double devY = drawingData.devY0Spline - mouseEvent.devYMouse;

      final double graphXMin = _splineData.posX_GraphMinValues[_pointHitIndex];
      final double graphXMax = _splineData.posX_GraphMaxValues[_pointHitIndex];

      double graphX = devX / scaleX;

      _canDeletePoint = false;

      if (isPointMovable) {

         // point can be moved horizontal and vertical

         /*
          * When a point is moved to the left or right border, then it will be deleted
          */

         // check min value
         if (Double.isNaN(graphXMin) == false && graphX < graphXMin) {

            graphX = graphXMin;
            _canDeletePoint = true;
         }

         // check max value
         if (Double.isNaN(graphXMax) == false && graphX > graphXMax) {

            graphX = graphXMax;
            _canDeletePoint = true;
         }
      }

      /*
       * Overwrite new relative position with forced min/max values
       */
      devX = graphX * scaleX;

      final double graph0X = _spline_BorderLeft_xValue;
      final double graph1X = _spline_BorderRight_xValue;
      final double graph1Y = _firstTimeSlice_ElevationDiff;

      final double dev0X = scaleX * graph0X;
      final double dev1X = scaleX * graph1X;
      final double dev1Y = scaleY * graph1Y;

      // set horizontal position
      if (isPointMovable) {

         // horizontal moving is allowed

         _splineData.posX_RelativeValues[_pointHitIndex] = (devX - dev0X) / (dev1X - dev0X);
      }

      // set vertical position
      final double devYRelativ = devY / dev1Y;
      _splineData.posY_RelativeValues[_pointHitIndex] = devYRelativ;
   }

   /**
    * @param mouseDownDevPositionX
    * @param mouseDownDevPositionY
    * @return
    */
   private boolean splinePoint_NewPoint(final int mouseDownDevPositionX,
                                        final int mouseDownDevPositionY) {

      final SplineDrawingData drawingData = _chartLayer2ndAltiSerie.getDrawingData();

      final double scaleX = drawingData.scaleX;
      final double scaleY = drawingData.scaleY;

      final float newPoint_DevX = drawingData.devGraphValueXOffset + mouseDownDevPositionX;
      final float newPoint_DevY = drawingData.devY0Spline - mouseDownDevPositionY;

      final double graph0X = _spline_BorderLeft_xValue;
      final double graph1X = _spline_BorderRight_xValue;

      final float graphX = (float) (newPoint_DevX / scaleX);

      // check min/max value
      if (graphX <= graph0X || graphX >= graph1X) {

         // click is outside of the allowed area
         return false;
      }

      /*
       * Add new point at the end of the existing points, CubicSpline will resort them
       */
// SET_FORMATTING_OFF

      final boolean[]   oldIsPointMovable       = _splineData.isPointMovable;

      final double[]    oldPosX_Relative        = _splineData.posX_RelativeValues;
      final double[]    oldPosY_Relative        = _splineData.posY_RelativeValues;

      final double[]    oldPosX_GraphValues     = _splineData.posX_GraphValues;
      final double[]    oldPosY_GraphValues     = _splineData.posY_GraphValues;

      final double[]    oldPosX_GraphMinValues  = _splineData.posX_GraphMinValues;
      final double[]    oldPosX_GraphMaxValues  = _splineData.posX_GraphMaxValues;

      final int newLength = oldPosX_GraphValues.length + 1;
      final boolean[]   newIsPointMovable       = _splineData.isPointMovable        = new boolean[newLength];

      final double[]    newPosX_Relative        = _splineData.posX_RelativeValues   = new double[newLength];
      final double[]    newPosY_Relative        = _splineData.posY_RelativeValues   = new double[newLength];

      final double[]    newPosX_GraphValues     = _splineData.posX_GraphValues      = new double[newLength];
      final double[]    newPosY_GraphValues     = _splineData.posY_GraphValues      = new double[newLength];

      final double[]    newPosX_GraphMinValues  = _splineData.posX_GraphMinValues   = new double[newLength];
      final double[]    newPosX_GraphMaxValues  = _splineData.posX_GraphMaxValues   = new double[newLength];

      final int oldLength = oldPosX_GraphValues.length;

      // copy old values into new arrays
      System.arraycopy(oldIsPointMovable,       0, newIsPointMovable,   0, oldLength);

      System.arraycopy(oldPosX_Relative,        0, newPosX_Relative,     0, oldLength);
      System.arraycopy(oldPosY_Relative,        0, newPosY_Relative,     0, oldLength);

      System.arraycopy(oldPosX_GraphValues,     0, newPosX_GraphValues,     0, oldLength);
      System.arraycopy(oldPosY_GraphValues,     0, newPosY_GraphValues,     0, oldLength);

      System.arraycopy(oldPosX_GraphMinValues,  0, newPosX_GraphMinValues,  0, oldLength);
      System.arraycopy(oldPosX_GraphMaxValues,  0, newPosX_GraphMaxValues,  0, oldLength);

// SET_FORMATTING_ON

      final float dev0X = (float) (graph0X * scaleX);
      final float dev1X = (float) (graph1X * scaleX);
      final float dev1Y = (float) (_firstTimeSlice_ElevationDiff * scaleY);

      /*
       * Creat a new points
       */
      final float posXRelative = dev1X == 0 ? 0 : (newPoint_DevX - dev0X) / (dev1X - dev0X);
      final float posYRelative = dev1Y == 0 ? 0 : newPoint_DevY / dev1Y;

      final int lastIndex = newLength - 1;

      newIsPointMovable[lastIndex] = true;

      newPosX_Relative[lastIndex] = posXRelative;
      newPosY_Relative[lastIndex] = posYRelative;

      newPosX_GraphValues[lastIndex] = graphX;
      newPosY_GraphValues[lastIndex] = 0;
      newPosX_GraphMinValues[lastIndex] = graph0X;
      newPosX_GraphMaxValues[lastIndex] = graph1X;

      // don't move the point immediately
      _pointHitIndex = -1;

      return true;
   }

   private void updateTourChart() {

      _isSliderEventDisabled = true;
      {
         _tourChart.updateTourChart(_tourData, _tcc, true);
      }
      _isSliderEventDisabled = false;
   }

   private void updateUI_2ndLayer() {

      enableFieldsWithSRTM();
      _tourChart.updateLayer2ndAlti(this, true);
   }

   /**
    * set the altitude fields with the current altitude values
    */
   private void updateUI_ElevationFields() {

      final float[] metricAltitudeSerie = _metricAdjustedAltitudeWithoutSRTM;
      final float[] adjustedAltitude = _tourData.dataSerieAdjustedAlti = new float[metricAltitudeSerie.length];

      final float startAlti = metricAltitudeSerie[0];
      final float endAlti = metricAltitudeSerie[metricAltitudeSerie.length - 1];

      /*
       * get max and current measurement altitude
       */
      float maxAlti = metricAltitudeSerie[0];
      for (int serieIndex = 0; serieIndex < metricAltitudeSerie.length; serieIndex++) {

         final float metricAltitude = metricAltitudeSerie[serieIndex];

         if (metricAltitude > maxAlti) {
            maxAlti = metricAltitude;
         }

         adjustedAltitude[serieIndex] = metricAltitude / UI.UNIT_VALUE_ELEVATION;
      }

      // keep current start/max values
      _prevAltiStart = startAlti;
      _prevAltiMax = maxAlti;

      _spinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(startAlti));
      _spinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(endAlti));
      _spinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(maxAlti));

      /*
       * prevent to fire the selection event in the spinner when a selection is set, this would
       * cause endless loops
       */
      _isDisableModifyListener = true;
      {
         _spinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ELEVATION));
         _spinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ELEVATION));
         _spinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ELEVATION));
      }
      _isDisableModifyListener = false;

      getButton(IDialogConstants.OK_ID).setEnabled(true);
   }

   private void updateUI_GeoPos() {

      final int geoPosSlices = _scaleSlicePos.getSelection() - MAX_ADJUST_GEO_POS_SLICES;

      _lblSliceValue.setText(Float.toString(geoPosSlices));
   }

}
