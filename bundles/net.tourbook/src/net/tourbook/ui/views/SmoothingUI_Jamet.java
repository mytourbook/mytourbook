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
package net.tourbook.ui.views;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageComputedValues;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SmoothingUI_Jamet implements ISmoothingAlgorithm {

// SET_FORMATTING_OFF

   private static final String    URL_TOUR_CHART_SMOOTHING = "https://mytourbook.sourceforge.io/mytourbook/index.php/documentation/tools/tour-chart-smoothing"; //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final int       MAX_TAU    = 5000;

   private final IPreferenceStore _prefStore = TourbookPlugin.getDefault().getPreferenceStore();

   private final boolean          _isOSX     = net.tourbook.common.UI.IS_OSX;
   private final boolean          _isLinux   = net.tourbook.common.UI.IS_LINUX;

   private int                    _hintDefaultSpinnerWidth;

   private SmoothingUI            _smoothingUI;

   private SelectionListener      _selectionListener;
   private MouseWheelListener     _spinnerMouseWheelListener;

   private boolean                _isUpdateUI;

   /*
    * UI resources
    */
   private PixelConverter _pc;

   private Image          _imageAltitude = TourbookPlugin.getImageDescriptor(Images.Graph_Elevation).createImage();
   private Image          _imageGradient = TourbookPlugin.getImageDescriptor(Images.Graph_Gradient).createImage();
   private Image          _imagePulse    = TourbookPlugin.getImageDescriptor(Images.Graph_Heartbeat).createImage();
   private Image          _imageSpeed    = TourbookPlugin.getImageDescriptor(Images.Graph_Speed).createImage();

   /*
    * UI controls
    */
   private FormToolkit _tk;

   private Button      _chkIsSynchSmoothing;
   private Button      _chkIsAltitudeSmoothing;
   private Button      _chkIsPulseSmoothing;
   private Spinner     _spinnerGradientTau;
   private Spinner     _spinnerPulseTau;
   private Spinner     _spinnerSpeedTau;
   private Spinner     _spinnerRepeatedSmoothing;
   private Spinner     _spinnerRepeatedTau;
   private Spinner     _spinnerLastUsed;

   private Label       _lblRepeatedSmoothing;
   private Label       _lblRepeatedTau;

   private CLabel      _iconSpeed;
   private CLabel      _iconPulse;
   private CLabel      _iconGradient;
   private CLabel      _iconAltitude;

   public SmoothingUI_Jamet() {}

   @Override
   public Composite createUI(final SmoothingUI smoothingUI,
                             final Composite parent,
                             final FormToolkit tk,
                             final boolean isShowDescription,
                             final boolean isShowAdditionalActions) {

      _smoothingUI = smoothingUI;
      _tk = tk;

      initUI(parent);

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(5, 5).numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_10_Tau(container);
         createUI_20_SmoothSpeed(container);
         createUI_22_SmoothGradient(container);
         createUI_24_SmoothPulse(container);
         createUI_26_SmoothAltitude(container);
         createUI_30_Iterations(container);

         if (isShowAdditionalActions) {
            createUI_50_Actions(container);
         }
      }

      restoreState();
      enableControls();

      return container;
   }

   private void createUI_10_Tau(final Composite parent) {

      final Label label = _tk.createLabel(parent, Messages.TourChart_Smoothing_Label_TauParameter);
      GridDataFactory.fillDefaults().span(3, 1).grab(false, false).applyTo(label);
   }

   private void createUI_20_SmoothSpeed(final Composite parent) {

      /*
       * image: speed
       */
      _iconSpeed = new CLabel(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(16, 0).applyTo(_iconSpeed);
      _iconSpeed.setBackground(_tk.getColors().getBackground());
      _iconSpeed.setImage(_imageSpeed);

      /*
       * label: smooth speed and pace
       */
      final Label label = _tk.createLabel(parent, Messages.TourChart_Smoothing_Label_SpeedAndPaceSmoothing);
      GridDataFactory.fillDefaults() //
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(label);
      label.setToolTipText(Messages.TourChart_Smoothing_Label_SpeedAndPaceSmoothing_Tooltip);

      /*
       * spinner: tau
       */
      _spinnerSpeedTau = new Spinner(parent, SWT.BORDER);
      GridDataFactory.fillDefaults()//
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_spinnerSpeedTau);
      _spinnerSpeedTau.setDigits(1);
      _spinnerSpeedTau.setMinimum(1);
      _spinnerSpeedTau.setMaximum(MAX_TAU);
      _spinnerSpeedTau.addSelectionListener(_selectionListener);
      _spinnerSpeedTau.addMouseWheelListener(_spinnerMouseWheelListener);
   }

   private void createUI_22_SmoothGradient(final Composite parent) {

      /*
       * image: gradient
       */
      _iconGradient = new CLabel(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(16, 0).applyTo(_iconGradient);
      _iconGradient.setBackground(_tk.getColors().getBackground());
      _iconGradient.setImage(_imageGradient);

      /*
       * label: smooth gradient
       */
      final Label label = _tk.createLabel(parent, Messages.TourChart_Smoothing_Label_GradientSmoothing, SWT.CHECK);
      GridDataFactory.fillDefaults() //
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(label);
      label.setToolTipText(Messages.TourChart_Smoothing_Label_GradientSmoothing_Tooltip);

      /*
       * spinner: gradient tau
       */
      _spinnerGradientTau = new Spinner(parent, SWT.BORDER);
      GridDataFactory.fillDefaults()//
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_spinnerGradientTau);
      _spinnerGradientTau.setDigits(1);
      _spinnerGradientTau.setMinimum(1);
      _spinnerGradientTau.setMaximum(MAX_TAU);
      _spinnerGradientTau.addSelectionListener(_selectionListener);
      _spinnerGradientTau.addMouseWheelListener(_spinnerMouseWheelListener);
   }

   private void createUI_24_SmoothPulse(final Composite parent) {

      /*
       * image: pulse
       */
      _iconPulse = new CLabel(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(16, 0).applyTo(_iconPulse);
      _iconPulse.setBackground(_tk.getColors().getBackground());
      _iconPulse.setImage(_imagePulse);

      /*
       * checkbox: smooth speed
       */
      _chkIsPulseSmoothing = _tk.createButton(
            parent,
            Messages.TourChart_Smoothing_Checkbox_IsPulseSmoothing,
            SWT.CHECK);
      GridDataFactory.fillDefaults() //
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(_chkIsPulseSmoothing);
      _chkIsPulseSmoothing.addSelectionListener(_selectionListener);
      _chkIsPulseSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsPulseSmoothing_Tooltip);

      /*
       * spinner: speed tau
       */
      _spinnerPulseTau = new Spinner(parent, SWT.BORDER);
      GridDataFactory.fillDefaults()//
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_spinnerPulseTau);
      _spinnerPulseTau.setDigits(1);
      _spinnerPulseTau.setMinimum(1);
      _spinnerPulseTau.setMaximum(MAX_TAU);
      _spinnerPulseTau.addSelectionListener(_selectionListener);
      _spinnerPulseTau.addMouseWheelListener(_spinnerMouseWheelListener);
   }

   private void createUI_26_SmoothAltitude(final Composite parent) {

      /*
       * image: altitude
       */
      _iconAltitude = new CLabel(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(16, 0).applyTo(_iconAltitude);
      _iconAltitude.setBackground(_tk.getColors().getBackground());
      _iconAltitude.setImage(_imageAltitude);

      /*
       * checkbox: smooth altitude
       */
      _chkIsAltitudeSmoothing = _tk.createButton(
            parent,
            Messages.TourChart_Smoothing_Checkbox_IsAltitudeSmoothing,
            SWT.CHECK);
      GridDataFactory.fillDefaults() //
            .align(SWT.FILL, SWT.CENTER)
            .span(2, 1)
            .applyTo(_chkIsAltitudeSmoothing);
      _chkIsAltitudeSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsAltitudeSmoothing_Tooltip);
      _chkIsAltitudeSmoothing.addSelectionListener(_selectionListener);
   }

   private void createUI_30_Iterations(final Composite parent) {

      /*
       * sync smoothing
       */
      {
         _chkIsSynchSmoothing = _tk.createButton(
               parent,
               Messages.TourChart_Smoothing_Checkbox_IsSyncSmoothing,
               SWT.CHECK);
         GridDataFactory.fillDefaults() //
               .align(SWT.FILL, SWT.CENTER)
               .span(3, 1)
               .indent(0, 10)
               .applyTo(_chkIsSynchSmoothing);
         _chkIsSynchSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsSyncSmoothing_Tooltip);
         _chkIsSynchSmoothing.addSelectionListener(_selectionListener);
      }

      /*
       * repeated smoothing
       */
      {
         _lblRepeatedSmoothing = _tk.createLabel(parent, Messages.TourChart_Smoothing_Label_RepeatedSmoothing);
         GridDataFactory.fillDefaults() //
               .align(SWT.FILL, SWT.CENTER)
               .span(2, 1)
               .indent(0, 10)
               .applyTo(_lblRepeatedSmoothing);
         _lblRepeatedSmoothing.setToolTipText(Messages.TourChart_Smoothing_Label_RepeatedSmoothing_Tooltip);

         /*
          * spinner: repeated smoothing
          */
         _spinnerRepeatedSmoothing = new Spinner(parent, SWT.BORDER);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.FILL)
               .indent(0, 10)
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .applyTo(_spinnerRepeatedSmoothing);
         _spinnerRepeatedSmoothing.setMinimum(0);
         _spinnerRepeatedSmoothing.setMaximum(10);
         _spinnerRepeatedSmoothing.addSelectionListener(_selectionListener);
         _spinnerRepeatedSmoothing.addMouseWheelListener(_spinnerMouseWheelListener);
      }

      /*
       * repeated tau
       */
      {
         _lblRepeatedTau = _tk.createLabel(parent, Messages.TourChart_Smoothing_Label_RepeatedTau);
         GridDataFactory.fillDefaults() //
               .align(SWT.FILL, SWT.CENTER)
               .span(2, 1)
               .applyTo(_lblRepeatedTau);
         _lblRepeatedTau.setToolTipText(Messages.TourChart_Smoothing_Label_RepeatedTau_Tooltip);

         /*
          * spinner: repeated tau
          */
         _spinnerRepeatedTau = new Spinner(parent, SWT.BORDER);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.FILL)
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .applyTo(_spinnerRepeatedTau);
         _spinnerRepeatedTau.setDigits(1);
         _spinnerRepeatedTau.setMinimum(1);
         _spinnerRepeatedTau.setMaximum(10);
         _spinnerRepeatedTau.addSelectionListener(_selectionListener);
         _spinnerRepeatedTau.addMouseWheelListener(_spinnerMouseWheelListener);
      }
   }

   private void createUI_50_Actions(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         /*
          * button: compute smoothing values for all tours
          */
         final Button btnComputValues = _tk.createButton(
               container,
               Messages.Compute_Smoothing_Button_ForAllTours,
               SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).indent(0, 10).applyTo(btnComputValues);
         btnComputValues.setToolTipText(Messages.Compute_Smoothing_Button_ForAllTours_Tooltip);
         btnComputValues.addSelectionListener(widgetSelectedAdapter(selectionEvent -> _smoothingUI.computeSmoothingForAllTours()));

         /*
          * link: restore defaults
          */
         Link link = new Link(container, SWT.NONE);
         GridDataFactory.fillDefaults()//
               .align(SWT.FILL, SWT.END)
               .grab(false, true)
               .applyTo(link);
         link.setText(Messages.App_Link_RestoreDefaultValues);
         link.setEnabled(true);
         link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> performDefaults(true)));
         _tk.adapt(link, true, true);

         /*
          * link: break time
          */
         link = new Link(container, SWT.NONE);
         GridDataFactory.fillDefaults()//
               .align(SWT.FILL, SWT.END)
               .applyTo(link);
         link.setText(Messages.TourChart_Smoothing_Link_PrefBreakTime);
         link.setEnabled(true);
         link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> PreferencesUtil.createPreferenceDialogOn(
               parent.getShell(),
               PrefPageComputedValues.ID,
               null,
               PrefPageComputedValues.TAB_FOLDER_BREAK_TIME).open()));
         _tk.adapt(link, true, true);

         /*
          * link: smoothing online documentation
          */
         link = new Link(container, SWT.NONE);
         GridDataFactory.fillDefaults()//
               .align(SWT.FILL, SWT.END)
               .applyTo(link);
         link.setText(Messages.TourChart_Smoothing_Link_SmoothingOnlineDocumentation);
         link.setEnabled(true);
         link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            WEB.openUrl(URL_TOUR_CHART_SMOOTHING);
         }));
         _tk.adapt(link, true, true);
      }
   }

   @Override
   public void dispose() {

      Util.disposeResource(_imageAltitude);
      Util.disposeResource(_imageGradient);
      Util.disposeResource(_imagePulse);
      Util.disposeResource(_imageSpeed);
   }

   private void enableControls() {

      final boolean isRepeated = _spinnerRepeatedSmoothing.getSelection() != 0;

      _lblRepeatedTau.setEnabled(isRepeated);

      _spinnerPulseTau.setEnabled(_chkIsPulseSmoothing.getSelection());
      _spinnerRepeatedTau.setEnabled(isRepeated);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

      _selectionListener = widgetSelectedAdapter(selectionEvent -> {
         if (_isUpdateUI) {
            return;
         }
         onModifySmoothing(selectionEvent.widget, true);
      });

      _spinnerMouseWheelListener = mouseEvent -> {
         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
         if (_isUpdateUI) {
            return;
         }
         onModifySmoothing(mouseEvent.widget, true);
      };
   }

   private void onModifySmoothing(final Widget widget, final boolean isFireModifications) {

      updateSyncedSlider(widget);

      enableControls();

      saveState();

      if (isFireModifications) {

         // delete cached data that the smoothed data series are recreated when displayed
         TourManager.getInstance().removeAllToursFromCache();

         // fire unique event for all changes
         TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
      }
   }

   @Override
   public void performDefaults(final boolean isFireModifications) {

      _isUpdateUI = true;
      {
         _chkIsSynchSmoothing.setSelection(//
               _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_SYNCH_SMOOTHING));

         // repeated smoothing
         _spinnerRepeatedSmoothing.setSelection(//
               _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING));
         _spinnerRepeatedTau.setSelection(//
               (int) (_prefStore.getDefaultDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU) * 10));

         // altitude
         _chkIsAltitudeSmoothing.setSelection(//
               _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_ALTITUDE));

         // gradient
         _spinnerGradientTau.setSelection(//
               (int) (_prefStore.getDefaultDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_GRADIENT_TAU) * 10));

         // pulse
         _chkIsPulseSmoothing.setSelection(//
               _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_PULSE));
         _spinnerPulseTau.setSelection((int) (_prefStore
               .getDefaultDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_PULSE_TAU) * 10));

         // speed
         _spinnerSpeedTau.setSelection(//
               (int) (_prefStore.getDefaultDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_SPEED_TAU) * 10));
      }
      _isUpdateUI = false;

      onModifySmoothing(null, isFireModifications);
   }

   private void restoreState() {

      _isUpdateUI = true;
      {
         _chkIsSynchSmoothing.setSelection(//
               _prefStore.getBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_SYNCH_SMOOTHING));

         // repeated smoothing
         _spinnerRepeatedSmoothing.setSelection(//
               _prefStore.getInt(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING));
         _spinnerRepeatedTau.setSelection(//
               (int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU) * 10));

         // altitude
         _chkIsAltitudeSmoothing.setSelection(//
               _prefStore.getBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_ALTITUDE));

         // gradient
         _spinnerGradientTau.setSelection(//
               (int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_GRADIENT_TAU) * 10));

         // pulse
         _chkIsPulseSmoothing.setSelection(//
               _prefStore.getBoolean(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_PULSE));
         _spinnerPulseTau.setSelection(//
               (int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_PULSE_TAU) * 10));

         // speed
         _spinnerSpeedTau.setSelection(//
               (int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_SPEED_TAU) * 10));
      }
      _isUpdateUI = false;
   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_SYNCH_SMOOTHING, //
            _chkIsSynchSmoothing.getSelection());

      // repeated smoothing
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING, //
            _spinnerRepeatedSmoothing.getSelection());
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU, //
            _spinnerRepeatedTau.getSelection() / 10.0);

      // altitude
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_ALTITUDE, //
            _chkIsAltitudeSmoothing.getSelection());

      // gradient
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_GRADIENT_TAU, //
            _spinnerGradientTau.getSelection() / 10.0);

      // pulse
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_PULSE, //
            _chkIsPulseSmoothing.getSelection());
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_PULSE_TAU, //
            _spinnerPulseTau.getSelection() / 10.0);

      // speed smoothing
      _prefStore.setValue(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_SPEED_TAU, //
            _spinnerSpeedTau.getSelection() / 10.0);

   }

   private void updateSyncedSlider(final Widget widget) {

      if (widget == null) {
         return;
      }

      int synchValue = -1;

      if (widget == _chkIsSynchSmoothing && _chkIsSynchSmoothing.getSelection() && _spinnerLastUsed != null) {

         // synch is checked, set spinner to the last used values, when available

         synchValue = _spinnerLastUsed.getSelection();

      } else {

         if (widget == _spinnerGradientTau) {

            synchValue = _spinnerGradientTau.getSelection();
            _spinnerLastUsed = _spinnerGradientTau;

         } else if (widget == _spinnerPulseTau) {

            synchValue = _spinnerPulseTau.getSelection();
            _spinnerLastUsed = _spinnerPulseTau;

         } else if (widget == _spinnerSpeedTau) {

            synchValue = _spinnerSpeedTau.getSelection();
            _spinnerLastUsed = _spinnerSpeedTau;
         }
      }

      // set last used spinner if existing
      if (_chkIsSynchSmoothing.getSelection() == false) {
         // no synching
         return;
      }

      if (synchValue != -1) {
         _isUpdateUI = true;
         {
            _spinnerGradientTau.setSelection(synchValue);
            _spinnerPulseTau.setSelection(synchValue);
            _spinnerSpeedTau.setSelection(synchValue);
         }
         _isUpdateUI = false;
      }
   }

   @Override
   public void updateUIFromPrefStore() {
      restoreState();
      enableControls();
   }
}
