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
import net.tourbook.data.TourData;

import org.eclipse.core.databinding.conversion.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.StringToNumberConverter;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
   private FormToolkit       _tk;
   private Form              _formContainer;

   private Button            _checkBox_NewValues;
   private Button            _checkBox_OffsetValues;
   private Button            _radioButton_Altitude_NewValue;
   private Button            _radioButton_Altitude_OffsetValue;
   private Button            _radioButton_Pulse_NewValue;
   private Button            _radioButton_Pulse_OffsetValue;
   private Button            _radioButton_Cadence_NewValue;
   private Button            _radioButton_Cadence_OffsetValue;
   private Button            _radioButton_Temperature_NewValue;
   private Button            _radioButton_Temperature_OffsetValue;

   private Text              _textAltitudeValue;
   private Text              _textPulseValue;
   private Text              _textCadenceValue;
   private Text              _textTemperatureValue;

   private boolean           _isAltitudeValueValid;
   private boolean           _isPulseValueValid;
   private boolean           _isCadenceValueValid;
   private boolean           _isTemperatureValueValid;

   private SelectionListener _radioButtonSelectionListener;

   private float             _newAltitudeValue;
   private boolean           _isAltitudeValueOffset;
   private float             _newPulseValue;
   private boolean           _isPulseValueOffset;
   private float             _newCadenceValue;
   private boolean           _isCadenceValueOffset;
   private float             _newTemperatureValue;
   private boolean           _isTemperatureValueOffset;

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

      _radioButtonSelectionListener = new SelectionListener() {

         @Override
         public void widgetDefaultSelected(final SelectionEvent arg0) {}

         @Override
         public void widgetSelected(final SelectionEvent arg0) {
            updateMainCheckBoxes();
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

   private void Create_110_AltitudeRadioButtons(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         _radioButton_Altitude_NewValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Altitude_NewValue);
         _radioButton_Altitude_NewValue.setSelection(true);
         _radioButton_Altitude_NewValue.addSelectionListener(_radioButtonSelectionListener);

         /*
          * radio button: offset
          */
         _radioButton_Altitude_OffsetValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Altitude_OffsetValue);
         _radioButton_Altitude_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
         _radioButton_Altitude_OffsetValue.addSelectionListener(_radioButtonSelectionListener);
      }
   }

   private void Create_120_PulseRadioButtons(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * radio button: new value
          */
         _radioButton_Pulse_NewValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Pulse_NewValue);
         _radioButton_Pulse_NewValue.setSelection(true);
         _radioButton_Pulse_NewValue.addSelectionListener(_radioButtonSelectionListener);

         /*
          * radio button: offset
          */
         _radioButton_Pulse_OffsetValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Pulse_OffsetValue);
         _radioButton_Pulse_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
         _radioButton_Pulse_OffsetValue.addSelectionListener(_radioButtonSelectionListener);
      }
   }

   private void Create_130_CadenceRadioButtons(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * radio button: new value
          */
         _radioButton_Cadence_NewValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Cadence_NewValue);
         _radioButton_Cadence_NewValue.setSelection(true);
         _radioButton_Cadence_NewValue.addSelectionListener(_radioButtonSelectionListener);

         /*
          * radio button: offset
          */
         _radioButton_Cadence_OffsetValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Cadence_OffsetValue);
         _radioButton_Cadence_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
         _radioButton_Cadence_OffsetValue.addSelectionListener(_radioButtonSelectionListener);
      }
   }

   private void Create_140_TemperatureRadioButtons(final Composite parent) {
      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * radio button: new value
          */
         _radioButton_Temperature_NewValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_newValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Temperature_NewValue);
         _radioButton_Temperature_NewValue.setSelection(true);
         _radioButton_Temperature_NewValue.addSelectionListener(_radioButtonSelectionListener);

         /*
          * radio button: offset
          */
         _radioButton_Temperature_OffsetValue = new Button(container, SWT.RADIO);
         GridDataFactory.fillDefaults().indent(_offsetValuesRadioButtonsIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(
               _radioButton_Temperature_OffsetValue);
         _radioButton_Temperature_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
         _radioButton_Temperature_OffsetValue.addSelectionListener(_radioButtonSelectionListener);
      }
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
          * Main checkboxes
          */
         {
            _checkBox_NewValues = new Button(container, SWT.CHECK);
            _checkBox_NewValues.setText(Messages.Dialog_Edit_Checkbox_NewValues);//"New values");
            GridDataFactory.fillDefaults().span(4, 1).indent(175, 0).align(SWT.END, SWT.FILL).applyTo(_checkBox_NewValues);
            _checkBox_NewValues.setSelection(true);
            _checkBox_NewValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {

                  if (_checkBox_NewValues.getSelection()) {
                     _checkBox_OffsetValues.setSelection(false);
                     ToggleRadioButtons(true);
                  }

                  enableControls();
               }
            });

            _checkBox_OffsetValues = new Button(container, SWT.CHECK);
            _checkBox_OffsetValues.setText(Messages.Dialog_Edit_Checkbox_OffsetValues);//"Offset values");
            GridDataFactory.fillDefaults().indent(20, 0).align(SWT.BEGINNING, SWT.FILL).applyTo(_checkBox_OffsetValues);
            _checkBox_OffsetValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {

                  if (_checkBox_OffsetValues.getSelection()) {
                     _checkBox_NewValues.setSelection(false);
                     ToggleRadioButtons(false);
                  }

                  enableControls();
               }
            });
         }

         /*
          * Altitude
          */
         {
            // label
            Label label = _tk.createLabel(container, Messages.Dialog_Edit_Label_Altitude);
            label.setToolTipText(Messages.Dialog_Edit_Label_Altitude_Tooltip);

            // text
            _textAltitudeValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textAltitudeValue);
            _textAltitudeValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {
                  _isAltitudeValueValid = isFloatValid(_textAltitudeValue.getText());
                  enableSaveButton();
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);

            /*
             * radio button: new value
             */
            Create_110_AltitudeRadioButtons(container);

         }

         /*
          * Heart rate
          */
         {
            // label
            Label label = _tk.createLabel(container, Messages.Dialog_Edit_Label_Pulse);
            label.setToolTipText(Messages.Dialog_Edit_Label_Pulse_Tooltip);

            // text
            _textPulseValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textPulseValue);
            _textPulseValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {

                  _isPulseValueValid = isIntegerValid(_textPulseValue.getText());
                  enableSaveButton();

               }
            });

            // label: bpm
            label = _tk.createLabel(container, "bpm"); //$NON-NLS-1$

            Create_120_PulseRadioButtons(container);

         }

         /*
          * Cadence
          */
         {
            // label
            Label label = _tk.createLabel(container, Messages.Dialog_Edit_Label_Cadence);
            label.setToolTipText(Messages.Dialog_Edit_Label_Cadence_Tooltip);

            // text
            _textCadenceValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textCadenceValue);
            _textCadenceValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {
                  _isCadenceValueValid = isFloatValid(_textCadenceValue.getText());
                  enableSaveButton();
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, "rpm");//$NON-NLS-1$

            Create_130_CadenceRadioButtons(container);

         }

         /*
          * Temperature
          */
         {
            // label
            Label label = _tk.createLabel(container, Messages.Dialog_Edit_Label_Temperature);
            label.setToolTipText(Messages.Dialog_Edit_Label_Temperature_Tooltip);

            // text
            _textTemperatureValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textTemperatureValue);
            _textTemperatureValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {
                  _isTemperatureValueValid = isFloatValid(_textTemperatureValue.getText());
                  enableSaveButton();
               }
            });

            // label: Celsius or Fahrenheit
            label = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

            Create_140_TemperatureRadioButtons(container);
         }
      }
   }

   private void enableAltitudeControls(final boolean enable) {
      _textAltitudeValue.setEnabled(enable);
      _radioButton_Altitude_NewValue.setEnabled(enable);
      _radioButton_Altitude_OffsetValue.setEnabled(enable);

   }

   private void enableCadenceControls(final boolean enable) {
      _textCadenceValue.setEnabled(enable);
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
      _textPulseValue.setEnabled(enable);
      _radioButton_Pulse_NewValue.setEnabled(enable);
      _radioButton_Pulse_OffsetValue.setEnabled(enable);
   }

   private void enableSaveButton() {
      final Button saveButton = getButton(IDialogConstants.OK_ID);
      if (saveButton != null) {

         final boolean enable = (_isAltitudeValueValid || _textAltitudeValue.getText().equals(UI.EMPTY_STRING)) &&
               (_isPulseValueValid || _textPulseValue.getText().equals(UI.EMPTY_STRING)) &&
               (_isCadenceValueValid || _textCadenceValue.getText().equals(UI.EMPTY_STRING)) &&
               (_isTemperatureValueValid || _textTemperatureValue.getText().equals(UI.EMPTY_STRING));

         saveButton.setEnabled(enable);
      }
   }

   private void enableTemperatureControls(final boolean enable) {
      _textTemperatureValue.setEnabled(enable);
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

   private boolean isFloatValid(final String floatValueText) {

      boolean isFloatValid = false;

      final String valueText = floatValueText.trim();

      try {
         StringToNumberConverter.toFloat(true).convert(valueText);

         isFloatValid = true;

      } catch (final IllegalArgumentException e) {}

      return isFloatValid;
   }

   private boolean isIntegerValid(final String integerValueText) {

      boolean isIntegerValid = false;

      final String valueText = integerValueText.trim();

      try {
         StringToNumberConverter.toInteger(true).convert(valueText);

         isIntegerValid = true;

      } catch (final IllegalArgumentException e) {}

      return isIntegerValid;
   }

   @Override
   protected void okPressed() {

      final String altitudeValue = _textAltitudeValue.getText();
      _newAltitudeValue = !altitudeValue.equals(UI.EMPTY_STRING) ? StringToNumberConverter.toFloat(true).convert(altitudeValue)
            * net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE : Float.MIN_VALUE;

      final String pulseValue = _textPulseValue.getText();
      _newPulseValue = !pulseValue.equals(UI.EMPTY_STRING) ? StringToNumberConverter.toInteger(true).convert(pulseValue) : Float.MIN_VALUE;

      final String cadenceValue = _textCadenceValue.getText();
      _newCadenceValue = !cadenceValue.equals(UI.EMPTY_STRING) ? StringToNumberConverter.toFloat(true).convert(cadenceValue) : Float.MIN_VALUE;

      final String temperatureValue = _textTemperatureValue.getText();
      _newTemperatureValue = !temperatureValue.equals(UI.EMPTY_STRING) ? StringToNumberConverter.toFloat(true).convert(temperatureValue)
            : Float.MIN_VALUE;

      _isAltitudeValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Altitude_OffsetValue.getSelection();
      _isPulseValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Pulse_OffsetValue.getSelection();
      _isCadenceValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Cadence_OffsetValue.getSelection();
      _isTemperatureValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Temperature_OffsetValue.getSelection();

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

   private void ToggleRadioButtons(final boolean checkAllNewValues) {

      _radioButton_Altitude_NewValue.setSelection(checkAllNewValues && _isAltitudeSerieValid);
      _radioButton_Pulse_NewValue.setSelection(checkAllNewValues && _isPulseSerieValid);
      _radioButton_Cadence_NewValue.setSelection(checkAllNewValues && _isCadenceSerieValid);
      _radioButton_Temperature_NewValue.setSelection(checkAllNewValues && _isTemperatureSerieValid);

      _radioButton_Altitude_OffsetValue.setSelection(!checkAllNewValues && _isAltitudeSerieValid);
      _radioButton_Pulse_OffsetValue.setSelection(!checkAllNewValues && _isPulseSerieValid);
      _radioButton_Cadence_OffsetValue.setSelection(!checkAllNewValues && _isCadenceSerieValid);
      _radioButton_Temperature_OffsetValue.setSelection(!checkAllNewValues && _isTemperatureSerieValid);

   }

   private void updateMainCheckBoxes() {
      if (!_checkBox_NewValues.getSelection() && !_checkBox_OffsetValues.getSelection()) {
         return;
      }

      final boolean allNewValuesRadioButtonsSelected = _radioButton_Altitude_NewValue.getSelection() &&
            _radioButton_Pulse_NewValue.getSelection() &&
            _radioButton_Cadence_NewValue.getSelection() &&
            _radioButton_Temperature_NewValue.getSelection();

      if (_checkBox_NewValues.getSelection() && !allNewValuesRadioButtonsSelected) {
         _checkBox_NewValues.setSelection(false);
      }

      final boolean allOffsetValuesRadioButtonsSelected = _radioButton_Altitude_OffsetValue.getSelection() &&
            _radioButton_Pulse_OffsetValue.getSelection() &&
            _radioButton_Cadence_OffsetValue.getSelection() &&
            _radioButton_Temperature_OffsetValue.getSelection();

      if (_checkBox_OffsetValues.getSelection() && !allOffsetValuesRadioButtonsSelected) {
         _checkBox_OffsetValues.setSelection(false);
      }
   }

   private void updateUIFromModel() {

      //If several rows were selected, we don't need to update the UI
      if (_selectedIndex == -1) {
         return;
      }

      if (_isAltitudeSerieValid) {
         float altitudeToUnit = _serieAltitude[_selectedIndex] / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
         altitudeToUnit = (float) (Math.round(altitudeToUnit * 10.0) / 10.0);
         _textAltitudeValue.setText(NumberToStringConverter.fromFloat(true).convert(altitudeToUnit));
      }

      if (_isPulseSerieValid) {
         final int pulse = (int) (_seriePulse[_selectedIndex]);
         _textPulseValue.setText(String.valueOf(pulse));
      }

      if (_isCadenceSerieValid) {
         final float cadence = _serieCadence[_selectedIndex];
         _textCadenceValue.setText(NumberToStringConverter.fromFloat(true).convert(cadence));
      }

      if (_isTemperatureSerieValid) {
         float temperatureToUnit = UI.convertTemperatureFromMetric(_serieTemperature[_selectedIndex]);
         temperatureToUnit = (float) (Math.round(temperatureToUnit * 10.0) / 10.0);
         _textTemperatureValue.setText(NumberToStringConverter.fromFloat(true).convert(temperatureToUnit));
      }

   }
}
