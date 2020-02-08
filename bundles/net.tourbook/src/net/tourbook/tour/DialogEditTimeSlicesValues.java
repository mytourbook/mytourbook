/*******************************************************************************
 *  Copyright (C) 2020 Frédéric Bard and Contributors
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

import java.time.ZonedDateTime;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DialogEditTimeSlicesValues extends TitleAreaDialog {

   private final boolean         _isOSX                          = UI.IS_OSX;
   private final boolean         _isLinux                        = UI.IS_LINUX;

   private final TourData        _tourData;
   private float[]               _serieAltitude;
   private float[]               _serieTemperature;
   private float[]               _serieCadence;
   private float[]               _seriePulse;
   private final boolean         _isAltitudeSerieValid;
   private final boolean         _isPulseSerieValid;
   private final boolean         _isCadenceSerieValid;
   private final boolean         _isTemperatureSerieValid;
   private final int             _selectedIndex;

   private final IDialogSettings _state;
   private PixelConverter        _pc;

   private int                   _hintDefaultTextWidth;
   private int                   _newValuesRadioButtonsIndent    = 40;
   private int                   _offsetValuesRadioButtonsIndent = 100;

   /*
    * UI controls
    */
   private FormToolkit        _tk;
   private Form               _formContainer;

   private Button             _checkBox_Altitude;
   private Button             _checkBox_Pulse;
   private Button             _checkBox_Cadence;
   private Button             _checkBox_Temperature;
   private Button             _radioButton_Altitude_NewValue;
   private Button             _radioButton_Altitude_OffsetValue;
   private Button             _radioButton_Pulse_NewValue;
   private Button             _radioButton_Pulse_OffsetValue;
   private Button             _radioButton_Cadence_NewValue;
   private Button             _radioButton_Cadence_OffsetValue;
   private Button             _radioButton_Temperature_NewValue;
   private Button             _radioButton_Temperature_OffsetValue;

   private Label              _label_NewValues;
   private Label              _label_OffsetValues;

   private Spinner            _spinnerAltitudeValue;
   private Spinner            _spinnerPulseValue;
   private Spinner            _spinnerCadenceValue;
   private Spinner            _spinnerTemperatureValue;

   private ModifyListener     _defaultModifyListener;
   private MouseWheelListener _defaultMouseWheelListener;
   private SelectionListener  _defaultSelectionListener;

   private float              _newAltitudeValue;
   private boolean            _isAltitudeValueOffset;
   private int                _newPulseValue;
   private boolean            _isPulseValueOffset;
   private int                _newCadenceValue;
   private boolean            _isCadenceValueOffset;
   private float              _newTemperatureValue;
   private boolean            _isTemperatureValueOffset;

   public DialogEditTimeSlicesValues(final Shell parentShell, final TourData tourData, final int selectedIndex) {

      super(parentShell);

      setShellStyle(getShellStyle());

      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

      _tourData = tourData;
      _serieAltitude = _tourData.altitudeSerie;
      _seriePulse = _tourData.pulseSerie;
      _serieCadence = _tourData.getCadenceSerie();
      _serieTemperature = _tourData.temperatureSerie;
      _isAltitudeSerieValid = _serieAltitude != null;
      _isPulseSerieValid = _seriePulse != null;
      _isCadenceSerieValid = _serieCadence != null;
      _isTemperatureSerieValid = _serieTemperature != null;
      _selectedIndex = selectedIndex;

      _defaultModifyListener = new ModifyListener() {

         @Override
         public void modifyText(final ModifyEvent arg0) {
            enableSaveButton();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
            enableSaveButton();
         }
      };

      _defaultSelectionListener = new SelectionListener() {

         @Override
         public void widgetDefaultSelected(final SelectionEvent arg0) {
            // TODO Auto-generated method stub

         }

         @Override
         public void widgetSelected(final SelectionEvent arg0) {
            enableSaveButton();

         }
      };

      _state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_Edit_Timeslices_Values_Title);

      shell.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_Edit_Timeslices_Values_Area_Title);

      final ZonedDateTime tourStart = _tourData.getTourStartTime();

      setMessage(
            tourStart.format(TimeTools.Formatter_Date_F)
                  + UI.SPACE2
                  + tourStart.format(TimeTools.Formatter_Time_S));
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      final String okText = net.tourbook.ui.UI.convertOKtoSaveUpdateButton(_tourData);

      final Button saveButton = getButton(IDialogConstants.OK_ID);
      saveButton.setText(okText);
      enableSaveButton();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      // create ui
      createUI(dlgAreaContainer);

      updateUIFromModel();
      enableControls();

      return dlgAreaContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _hintDefaultTextWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);

      _tk = new FormToolkit(parent.getDisplay());

      _formContainer = _tk.createForm(parent);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_formContainer);
      _tk.decorateFormHeading(_formContainer);
      _tk.setBorderStyle(SWT.BORDER);

      final Composite tourContainer = _formContainer.getBody();
      GridLayoutFactory.swtDefaults().applyTo(tourContainer);
      {
         createUI_100_Values_MainMenu(tourContainer);
      }

      final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      tourContainer.layout(true, true);
   }

   private void createUI_100_Values_MainMenu(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
      {
         /*
          * Main labels
          */
         {
            _label_NewValues = _tk.createLabel(container, Messages.Dialog_Edit_Checkbox_NewValues, SWT.NONE);
            GridDataFactory.fillDefaults().span(4, 1).indent(215, 0).align(SWT.END, SWT.FILL).applyTo(_label_NewValues);

            _label_OffsetValues = _tk.createLabel(container, Messages.Dialog_Edit_Checkbox_OffsetValues, SWT.NONE);
            GridDataFactory.fillDefaults().indent(55, 0).align(SWT.BEGINNING, SWT.FILL).applyTo(_label_OffsetValues);
         }

         /*
          * Altitude
          */
         {
            // label
            _checkBox_Altitude = _tk.createButton(container, Messages.Dialog_Edit_Label_Altitude, SWT.CHECK);
            _checkBox_Altitude.setToolTipText(Messages.Dialog_Edit_Label_Altitude_Tooltip);
            _checkBox_Altitude.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerAltitudeValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerAltitudeValue);
            _spinnerAltitudeValue.setDigits(1);
            _spinnerAltitudeValue.setMaximum(500000);
            _spinnerAltitudeValue.addModifyListener(_defaultModifyListener);
            _spinnerAltitudeValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: m or ft
            _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);

            /*
             * radio button: new value
             */
            CreateUI_110_AltitudeRadioButtons(container);

         }

         /*
          * Heart rate
          */
         {
            // label
            _checkBox_Pulse = _tk.createButton(container, Messages.Dialog_Edit_Label_Pulse, SWT.CHECK);
            _checkBox_Pulse.setToolTipText(Messages.Dialog_Edit_Label_Pulse_Tooltip);
            _checkBox_Pulse.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerPulseValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerPulseValue);
            _spinnerPulseValue.setMaximum(500);
            _spinnerPulseValue.addModifyListener(_defaultModifyListener);
            _spinnerPulseValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: bpm
            _tk.createLabel(container, "bpm"); //$NON-NLS-1$

            CreateUI_120_PulseRadioButtons(container);

         }

         /*
          * Cadence
          */
         {
            // label
            _checkBox_Cadence = _tk.createButton(container, Messages.Dialog_Edit_Label_Cadence, SWT.CHECK);
            _checkBox_Cadence.setToolTipText(Messages.Dialog_Edit_Label_Cadence_Tooltip);
            _checkBox_Cadence.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerCadenceValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerCadenceValue);
            _spinnerCadenceValue.setMaximum(1000);
            _spinnerCadenceValue.addModifyListener(_defaultModifyListener);
            _spinnerCadenceValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: "rpm"
            _tk.createLabel(container, "rpm");//$NON-NLS-1$

            CreateUI_130_CadenceRadioButtons(container);

         }

         /*
          * Temperature
          */
         {
            // label
            _checkBox_Temperature = _tk.createButton(container, Messages.Dialog_Edit_Label_Temperature, SWT.CHECK);
            _checkBox_Temperature.setToolTipText(Messages.Dialog_Edit_Label_Temperature_Tooltip);
            _checkBox_Temperature.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerTemperatureValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerTemperatureValue);
            _spinnerTemperatureValue.setDigits(1);
            _spinnerTemperatureValue.setMaximum(2100);
            _spinnerTemperatureValue.addModifyListener(_defaultModifyListener);
            _spinnerTemperatureValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: Celsius or Fahrenheit
            _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

            CreateUI_140_TemperatureRadioButtons(container);
         }
      }
   }

   private void CreateUI_110_AltitudeRadioButtons(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         _radioButton_Altitude_NewValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Altitude_NewValue);
         _radioButton_Altitude_NewValue.setSelection(true);

         /*
          * radio button: offset
          */
         _radioButton_Altitude_OffsetValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Altitude_OffsetValue);
      }
   }

   private void CreateUI_120_PulseRadioButtons(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * radio button: new value
          */
         _radioButton_Pulse_NewValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Pulse_NewValue);
         _radioButton_Pulse_NewValue.setSelection(true);

         /*
          * radio button: offset
          */
         _radioButton_Pulse_OffsetValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Pulse_OffsetValue);
      }
   }

   private void CreateUI_130_CadenceRadioButtons(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * radio button: new value
          */
         _radioButton_Cadence_NewValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Cadence_NewValue);
         _radioButton_Cadence_NewValue.setSelection(true);

         /*
          * radio button: offset
          */
         _radioButton_Cadence_OffsetValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Cadence_OffsetValue);
      }
   }

   private void CreateUI_140_TemperatureRadioButtons(final Composite parent) {
      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * radio button: new value
          */
         _radioButton_Temperature_NewValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Temperature_NewValue);
         _radioButton_Temperature_NewValue.setSelection(true);

         /*
          * radio button: offset
          */
         _radioButton_Temperature_OffsetValue = _tk.createButton(container, UI.EMPTY_STRING, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Temperature_OffsetValue);
      }
   }

   private void enableAltitudeControls(final boolean enable) {
      _checkBox_Altitude.setEnabled(enable);
      _spinnerAltitudeValue.setEnabled(enable);
      _radioButton_Altitude_NewValue.setEnabled(enable);
      _radioButton_Altitude_OffsetValue.setEnabled(enable);

   }

   private void enableCadenceControls(final boolean enable) {
      _checkBox_Cadence.setEnabled(enable);
      _spinnerCadenceValue.setEnabled(enable);
      _radioButton_Cadence_NewValue.setEnabled(enable);
      _radioButton_Cadence_OffsetValue.setEnabled(enable);

   }

   private void enableControls() {
      enableAltitudeControls(_isAltitudeSerieValid);

      enablePulseControls(_isPulseSerieValid);

      enableCadenceControls(_isCadenceSerieValid);

      enableTemperatureControls(_isTemperatureSerieValid);

   }

   private void enablePulseControls(final boolean enable) {
      _checkBox_Pulse.setEnabled(enable);
      _spinnerPulseValue.setEnabled(enable);
      _radioButton_Pulse_NewValue.setEnabled(enable);
      _radioButton_Pulse_OffsetValue.setEnabled(enable);
   }

   private void enableSaveButton() {
      final Button saveButton = getButton(IDialogConstants.OK_ID);

      if (saveButton != null) {

         final boolean isAltitudeToBeSaved = _checkBox_Altitude.isEnabled() && _checkBox_Altitude.getSelection();

         final boolean isPulseToBeSaved = _checkBox_Pulse.isEnabled() && _checkBox_Pulse.getSelection();

         final boolean isCadenceToBeSaved = _checkBox_Cadence.isEnabled() && _checkBox_Cadence.getSelection();

         final boolean isTemperatureToBeSaved = _checkBox_Temperature.isEnabled() && _checkBox_Temperature.getSelection();

         final boolean enable = isAltitudeToBeSaved ||
               isPulseToBeSaved ||
               isCadenceToBeSaved ||
               isTemperatureToBeSaved;

         saveButton.setEnabled(enable);
      }
   }

   private void enableTemperatureControls(final boolean enable) {
      _checkBox_Temperature.setEnabled(enable);
      _spinnerTemperatureValue.setEnabled(enable);
      _radioButton_Temperature_NewValue.setEnabled(enable);
      _radioButton_Temperature_OffsetValue.setEnabled(enable);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      return _state;
   }

   public boolean getIsAltitudeValueOffset() {
      return _isAltitudeValueOffset;
   }

   public boolean getIsCadenceValueOffset() {
      return _isCadenceValueOffset;
   }

   public boolean getIsPulseValueOffset() {
      return _isPulseValueOffset;
   }

   public boolean getIsTemperatureValueOffset() {
      return _isTemperatureValueOffset;
   }

   public float getNewAltitudeValue() {
      return _newAltitudeValue;
   }

   public float getNewCadenceValue() {
      return _newCadenceValue;
   }

   public float getNewPulseValue() {
      return _newPulseValue;
   }

   public float getNewTemperatureValue() {
      return _newTemperatureValue;
   }

   @Override
   protected void okPressed() {

      final float altitudeValue = _spinnerAltitudeValue.getSelection() / 10f;
      _newAltitudeValue = _checkBox_Altitude.isEnabled() ? altitudeValue * net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE : Float.MIN_VALUE;

      final int pulseValue = _spinnerPulseValue.getSelection();
      _newPulseValue = _checkBox_Pulse.isEnabled() ? pulseValue : Integer.MIN_VALUE;

      final int cadenceValue = _spinnerCadenceValue.getSelection();
      _newCadenceValue = _checkBox_Cadence.isEnabled() ? cadenceValue : Integer.MIN_VALUE;

      final float temperatureValue = _spinnerTemperatureValue.getSelection() / 10f;
      _newTemperatureValue = _checkBox_Temperature.isEnabled() ? temperatureValue : Float.MIN_VALUE;

      _isAltitudeValueOffset = _radioButton_Altitude_OffsetValue.getSelection();
      _isPulseValueOffset = _radioButton_Pulse_OffsetValue.getSelection();
      _isCadenceValueOffset = _radioButton_Cadence_OffsetValue.getSelection();
      _isTemperatureValueOffset = _radioButton_Temperature_OffsetValue.getSelection();

      if (!_isTemperatureValueOffset) {
         _newTemperatureValue = UI.convertTemperatureToMetric(_newTemperatureValue);
      }

      super.okPressed();
   }

   private void onDispose() {

      if (_tk != null) {
         _tk.dispose();
      }
   }

   private void updateUIFromModel() {

      //If several rows were selected, we don't need to update the UI
      if (_selectedIndex == -1) {
         return;
      }

      if (_isAltitudeSerieValid) {
         float altitudeToUnit = _serieAltitude[_selectedIndex] / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
         altitudeToUnit = Math.round(altitudeToUnit * 10.0);
         _spinnerAltitudeValue.setSelection((int) altitudeToUnit);
      }

      if (_isPulseSerieValid) {
         final int pulse = (int) (_seriePulse[_selectedIndex]);
         _spinnerPulseValue.setSelection(pulse);
      }

      if (_isCadenceSerieValid) {
         final float cadence = _serieCadence[_selectedIndex];
         _spinnerCadenceValue.setSelection((int) cadence);
      }

      if (_isTemperatureSerieValid) {
         float temperatureToUnit = UI.convertTemperatureFromMetric(_serieTemperature[_selectedIndex]);
         temperatureToUnit = Math.round(temperatureToUnit * 10.0);
         _spinnerTemperatureValue.setSelection((int) temperatureToUnit);
      }

   }
}
