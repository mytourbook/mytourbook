/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard and Contributors
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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
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

   private static final String   STATE_IS_ALTITUDE_MODIFIED    = "STATE_IS_ALTITUDE_MODIFIED";    //$NON-NLS-1$
   private static final String   STATE_IS_CADENCE_MODIFIED     = "STATE_IS_CADENCE_MODIFIED";     //$NON-NLS-1$
   private static final String   STATE_IS_PULSE_MODIFIED       = "STATE_IS_PULSE_MODIFIED";       //$NON-NLS-1$
   private static final String   STATE_IS_TEMPERATURE_MODIFIED = "STATE_IS_TEMPERATURE_MODIFIED"; //$NON-NLS-1$

   private static final String   STATE_IS_ALTITUDE_OFFSET      = "STATE_IS_ALTITUDE_OFFSET";      //$NON-NLS-1$
   private static final String   STATE_IS_CADENCE_OFFSET       = "STATE_IS_CADENCE_OFFSET";       //$NON-NLS-1$
   private static final String   STATE_IS_PULSE_OFFSET         = "STATE_IS_PULSE_OFFSET";         //$NON-NLS-1$
   private static final String   STATE_IS_TEMPERATURE_OFFSET   = "STATE_IS_TEMPERATURE_OFFSET";   //$NON-NLS-1$

   private static final String   STATE_ALTITUDE_VALUE          = "STATE_ALTITUDE_VALUE";          //$NON-NLS-1$
   private static final String   STATE_CADENCE_VALUE           = "STATE_CADENCE_VALUE";           //$NON-NLS-1$
   private static final String   STATE_PULSE_VALUE             = "STATE_PULSE_VALUE";             //$NON-NLS-1$
   private static final String   STATE_TEMPERATURE_VALUE       = "STATE_TEMPERATURE_VALUE";       //$NON-NLS-1$

   private final IDialogSettings _state;

   private final boolean         _isOSX                        = UI.IS_OSX;
   private final boolean         _isLinux                      = UI.IS_LINUX;

   private final TourData        _tourData;

   private float[]               _serieCadence;
   private float[]               _serieElevation;
   private float[]               _seriePulse;
   private float[]               _serieTemperature;

   private final boolean         _isCadenceSerieValid;
   private final boolean         _isElevationSerieValid;
   private final boolean         _isPulseSerieValid;
   private final boolean         _isTemperatureSerieValid;

   private int                   _hintDefaultTextWidth;

   private MouseWheelListener    _defaultMouseWheelListener;
   private SelectionListener     _defaultSelectionListener;

   private boolean               _isCadenceValueOffset;
   private boolean               _isElevationValueOffset;
   private boolean               _isPulseValueOffset;
   private boolean               _isTemperatureValueOffset;

   private int                   _newCadenceValue;
   private float                 _newElevationValue;
   private int                   _newPulseValue;
   private float                 _newTemperatureValue;

   /*
    * UI controls
    */
   private Form           _formContainer;
   private FormToolkit    _tk;

   private PixelConverter _pc;

   private Button         _checkbox_Cadence;
   private Button         _checkbox_Elevation;
   private Button         _checkbox_Pulse;
   private Button         _checkbox_Temperature;

   private Button         _radio_Cadence_NewValue;
   private Button         _radio_Cadence_OffsetValue;
   private Button         _radio_Elevation_NewValue;
   private Button         _radio_Elevation_OffsetValue;
   private Button         _radio_Pulse_NewValue;
   private Button         _radio_Pulse_OffsetValue;
   private Button         _radio_Temperature_NewValue;
   private Button         _radio_Temperature_OffsetValue;

   private Label          _label_CadenceUnit;
   private Label          _label_ElevationUnit;
   private Label          _label_PulseUnit;
   private Label          _label_TemperatureUnit;

   private Spinner        _spinner_CadenceValue;
   private Spinner        _spinner_ElevationValue;
   private Spinner        _spinner_PulseValue;
   private Spinner        _spinner_TemperatureValue;

   public DialogEditTimeSlicesValues(final Shell parentShell, final TourData tourData) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.App_Edit).createImage());

      _tourData = tourData;

      _serieCadence = _tourData.getCadenceSerie();
      _serieElevation = _tourData.altitudeSerie;
      _seriePulse = _tourData.pulseSerie;
      _serieTemperature = _tourData.temperatureSerie;

      _isCadenceSerieValid = _serieCadence != null;
      _isElevationSerieValid = _serieElevation != null;
      _isPulseSerieValid = _seriePulse != null;
      _isTemperatureSerieValid = _serieTemperature != null;

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
            enableSaveButton();
         }
      };

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent arg0) {
            enableControls();
         }
      };

      _state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_EditTimeslicesValues_Title);

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

      setTitle(Messages.Dialog_EditTimeslicesValues_Area_Title);

      final ZonedDateTime tourStart = _tourData.getTourStartTime();

      setMessage(
            tourStart.format(TimeTools.Formatter_Date_F)
                  + UI.SPACE2
                  + tourStart.format(TimeTools.Formatter_Time_S));

      restoreState();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      final Button saveButton = getButton(IDialogConstants.OK_ID);
      saveButton.setText(Messages.app_action_update);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      // create UI
      createUI(dlgAreaContainer);

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

      final Composite container = _formContainer.getBody();
      GridLayoutFactory.swtDefaults().applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_100_Values_MainMenu(container);
      }
//      container.layout(true, true);
   }

   private void createUI_100_Values_MainMenu(final Composite parent) {

      final String label_NewValues = Messages.Dialog_EditTimeslicesValues_Label_NewValues;
      final String label_OffsetValues = Messages.Dialog_EditTimeslicesValues_Label_OffsetValues;
      final int radioHorizSpacing = _pc.convertWidthInCharsToPixels(5);

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.swtDefaults()
            .numColumns(4)
            .spacing(10, 10)
            .applyTo(container);
      {
         {
            /*
             * Elevation
             */

            // label
            _checkbox_Elevation = _tk.createButton(container, Messages.Dialog_EditTimeslicesValues_Checkbox_Altitude, SWT.CHECK);
            _checkbox_Elevation.setToolTipText(Messages.Dialog_EditTimeslicesValues_Checkbox_Altitude_Tooltip);
            _checkbox_Elevation.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinner_ElevationValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinner_ElevationValue);
            _spinner_ElevationValue.setDigits(1);
            _spinner_ElevationValue.setMinimum(-500000);
            _spinner_ElevationValue.setMaximum(500000);
            _spinner_ElevationValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: m or ft
            _label_ElevationUnit = _tk.createLabel(container, UI.UNIT_LABEL_ELEVATION);

            final Composite containerElevation = _tk.createComposite(container);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(radioHorizSpacing, 0)
                  .applyTo(containerElevation);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerElevation);
            {
               /*
                * Radio: New value
                */
               _radio_Elevation_NewValue = _tk.createButton(containerElevation, label_NewValues, SWT.RADIO);

               /*
                * Radio: Offset
                */
               _radio_Elevation_OffsetValue = _tk.createButton(containerElevation, label_OffsetValues, SWT.RADIO);
            }
         }

         {
            /*
             * Heart rate
             */

            // label
            _checkbox_Pulse = _tk.createButton(container, Messages.Dialog_EditTimeslicesValues_Checkbox_Pulse, SWT.CHECK);
            _checkbox_Pulse.setToolTipText(Messages.Dialog_EditTimeslicesValues_Checkbox_Pulse_Tooltip);
            _checkbox_Pulse.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinner_PulseValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinner_PulseValue);
            _spinner_PulseValue.setMinimum(-500);
            _spinner_PulseValue.setMaximum(500);
            _spinner_PulseValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: bpm
            _label_PulseUnit = _tk.createLabel(container, OtherMessages.GRAPH_LABEL_HEARTBEAT_UNIT);

            final Composite containerPulse = _tk.createComposite(container);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(radioHorizSpacing, 0)
                  .applyTo(containerPulse);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerPulse);
            {
               /*
                * Radio: New value
                */
               _radio_Pulse_NewValue = _tk.createButton(containerPulse, label_NewValues, SWT.RADIO);

               /*
                * Radio: Offset
                */
               _radio_Pulse_OffsetValue = _tk.createButton(containerPulse, label_OffsetValues, SWT.RADIO);
            }
         }

         {
            /*
             * Cadence
             */

            // label
            _checkbox_Cadence = _tk.createButton(container, Messages.Dialog_EditTimeslicesValues_Checkbox_Cadence, SWT.CHECK);
            _checkbox_Cadence.setToolTipText(Messages.Dialog_EditTimeslicesValues_Checkbox_Cadence_Tooltip);
            _checkbox_Cadence.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinner_CadenceValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinner_CadenceValue);
            _spinner_CadenceValue.setMinimum(-1000);
            _spinner_CadenceValue.setMaximum(1000);
            _spinner_CadenceValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: "rpm"
            _label_CadenceUnit = _tk.createLabel(container, OtherMessages.GRAPH_LABEL_CADENCE_UNIT_RPM_SPM);

            final Composite containerCadence = _tk.createComposite(container);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(radioHorizSpacing, 0)
                  .applyTo(containerCadence);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerCadence);
            {
               /*
                * Radio: New value
                */
               _radio_Cadence_NewValue = _tk.createButton(containerCadence, label_NewValues, SWT.RADIO);

               /*
                * Radio: Offset
                */
               _radio_Cadence_OffsetValue = _tk.createButton(containerCadence, label_OffsetValues, SWT.RADIO);
            }
         }

         {
            /*
             * Temperature
             */

            // label
            _checkbox_Temperature = _tk.createButton(container, Messages.Dialog_EditTimeslicesValues_Checkbox_Temperature, SWT.CHECK);
            _checkbox_Temperature.setToolTipText(Messages.Dialog_EditTimeslicesValues_Checkbox_Temperature_Tooltip);
            _checkbox_Temperature.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinner_TemperatureValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultTextWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinner_TemperatureValue);
            _spinner_TemperatureValue.setDigits(1);
            _spinner_TemperatureValue.setMinimum(-2100);
            _spinner_TemperatureValue.setMaximum(2100);
            _spinner_TemperatureValue.addMouseWheelListener(_defaultMouseWheelListener);

            // label: Celsius or Fahrenheit
            _label_TemperatureUnit = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

            final Composite containerTemperature = _tk.createComposite(container);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(radioHorizSpacing, 0)
                  .applyTo(containerTemperature);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTemperature);
            {
               /*
                * Radio: New value
                */
               _radio_Temperature_NewValue = _tk.createButton(containerTemperature, label_NewValues, SWT.RADIO);

               /*
                * Radio: Offset
                */
               _radio_Temperature_OffsetValue = _tk.createButton(containerTemperature, label_OffsetValues, SWT.RADIO);
            }
         }
      }
   }

   private void enableControls() {

      final boolean canModifyCadence = _isCadenceSerieValid && _checkbox_Cadence.getSelection();
      final boolean canModifyElevation = _isElevationSerieValid && _checkbox_Elevation.getSelection();
      final boolean canModifyPulse = _isPulseSerieValid && _checkbox_Pulse.getSelection();
      final boolean canModifyTemperature = _isTemperatureSerieValid && _checkbox_Temperature.getSelection();

      _checkbox_Cadence.setEnabled(_isCadenceSerieValid);
      _checkbox_Elevation.setEnabled(_isElevationSerieValid);
      _checkbox_Pulse.setEnabled(_isPulseSerieValid);
      _checkbox_Temperature.setEnabled(_isTemperatureSerieValid);

      // cadence
      _label_CadenceUnit.setEnabled(canModifyCadence);
      _spinner_CadenceValue.setEnabled(canModifyCadence);
      _radio_Cadence_NewValue.setEnabled(canModifyCadence);
      _radio_Cadence_OffsetValue.setEnabled(canModifyCadence);

      // elevation
      _label_ElevationUnit.setEnabled(canModifyElevation);
      _spinner_ElevationValue.setEnabled(canModifyElevation);
      _radio_Elevation_NewValue.setEnabled(canModifyElevation);
      _radio_Elevation_OffsetValue.setEnabled(canModifyElevation);

      // pulse
      _label_PulseUnit.setEnabled(canModifyPulse);
      _spinner_PulseValue.setEnabled(canModifyPulse);
      _radio_Pulse_NewValue.setEnabled(canModifyPulse);
      _radio_Pulse_OffsetValue.setEnabled(canModifyPulse);

      // temperature
      _label_TemperatureUnit.setEnabled(canModifyTemperature);
      _spinner_TemperatureValue.setEnabled(canModifyTemperature);
      _radio_Temperature_NewValue.setEnabled(canModifyTemperature);
      _radio_Temperature_OffsetValue.setEnabled(canModifyTemperature);

      enableSaveButton();
   }

   private void enableSaveButton() {

      final Button updateButton = getButton(IDialogConstants.OK_ID);

      if (updateButton != null) {

         final boolean isCadenceToBeSaved = _checkbox_Cadence.isEnabled() && _checkbox_Cadence.getSelection();
         final boolean isElevationToBeSaved = _checkbox_Elevation.isEnabled() && _checkbox_Elevation.getSelection();
         final boolean isPulseToBeSaved = _checkbox_Pulse.isEnabled() && _checkbox_Pulse.getSelection();
         final boolean isTemperatureToBeSaved = _checkbox_Temperature.isEnabled() && _checkbox_Temperature.getSelection();

         final boolean isEnable = isElevationToBeSaved ||
               isPulseToBeSaved ||
               isCadenceToBeSaved ||
               isTemperatureToBeSaved;

         updateButton.setEnabled(isEnable);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
//      return null;
      return _state;
   }

   public boolean getIsAltitudeValueOffset() {
      return _isElevationValueOffset;
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
      return _newElevationValue;
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

      /*
       * Keep selected values that they can be retrieved after the dialog is closed
       */
      final float altitudeValue = _spinner_ElevationValue.getSelection() / 10f;
      _newElevationValue = _checkbox_Elevation.getSelection() ? altitudeValue * UI.UNIT_VALUE_ELEVATION : Float.MIN_VALUE;

      final int pulseValue = _spinner_PulseValue.getSelection();
      _newPulseValue = _checkbox_Pulse.getSelection() ? pulseValue : Integer.MIN_VALUE;

      final int cadenceValue = _spinner_CadenceValue.getSelection();
      _newCadenceValue = _checkbox_Cadence.getSelection() ? cadenceValue : Integer.MIN_VALUE;

      final float temperatureValue = _spinner_TemperatureValue.getSelection() / 10f;
      _newTemperatureValue = _checkbox_Temperature.getSelection() ? temperatureValue : Float.MIN_VALUE;

      _isElevationValueOffset = _radio_Elevation_OffsetValue.getSelection();
      _isPulseValueOffset = _radio_Pulse_OffsetValue.getSelection();
      _isCadenceValueOffset = _radio_Cadence_OffsetValue.getSelection();
      _isTemperatureValueOffset = _radio_Temperature_OffsetValue.getSelection();

      if (!_isTemperatureValueOffset) {
         _newTemperatureValue = UI.convertTemperatureToMetric(_newTemperatureValue);
      }

      saveState();

      super.okPressed();
   }

   private void onDispose() {

      if (_tk != null) {
         _tk.dispose();
      }
   }

   private void restoreState() {

      /*
       * Cadence
       */
      final boolean stateIsCadenceModified = Util.getStateBoolean(_state, STATE_IS_CADENCE_MODIFIED, false);
      final boolean stateIsCadenceOffset = Util.getStateBoolean(_state, STATE_IS_CADENCE_OFFSET, false);
      final int stateCadenceValue = Util.getStateInt(_state, STATE_CADENCE_VALUE, 0);

      _checkbox_Cadence.setSelection(stateIsCadenceModified);
      _spinner_CadenceValue.setSelection(stateCadenceValue);
      _radio_Cadence_NewValue.setSelection(!stateIsCadenceOffset);
      _radio_Cadence_OffsetValue.setSelection(stateIsCadenceOffset);

      /*
       * Elevation
       */
      final boolean stateIsElevationModified = Util.getStateBoolean(_state, STATE_IS_ALTITUDE_MODIFIED, false);
      final boolean stateIsElevationOffset = Util.getStateBoolean(_state, STATE_IS_ALTITUDE_OFFSET, false);
      final int stateElevationValue = Util.getStateInt(_state, STATE_ALTITUDE_VALUE, 0);

      _checkbox_Elevation.setSelection(stateIsElevationModified);
      _spinner_ElevationValue.setSelection(stateElevationValue);
      _radio_Elevation_NewValue.setSelection(!stateIsElevationOffset);
      _radio_Elevation_OffsetValue.setSelection(stateIsElevationOffset);

      /*
       * Pulse
       */
      final boolean stateIsPulseModified = Util.getStateBoolean(_state, STATE_IS_PULSE_MODIFIED, false);
      final boolean stateIsPulseOffset = Util.getStateBoolean(_state, STATE_IS_PULSE_OFFSET, false);
      final int statePulseValue = Util.getStateInt(_state, STATE_PULSE_VALUE, 0);

      _checkbox_Pulse.setSelection(stateIsPulseModified);
      _spinner_PulseValue.setSelection(statePulseValue);
      _radio_Pulse_NewValue.setSelection(!stateIsPulseOffset);
      _radio_Pulse_OffsetValue.setSelection(stateIsPulseOffset);

      /*
       * Temperature
       */
      final boolean stateIsTemperatureModified = Util.getStateBoolean(_state, STATE_IS_TEMPERATURE_MODIFIED, false);
      final boolean stateIsTemperatureOffset = Util.getStateBoolean(_state, STATE_IS_TEMPERATURE_OFFSET, false);
      final int stateTemperatureValue = Util.getStateInt(_state, STATE_TEMPERATURE_VALUE, 0);

      _checkbox_Temperature.setSelection(stateIsTemperatureModified);
      _spinner_TemperatureValue.setSelection(stateTemperatureValue);
      _radio_Temperature_NewValue.setSelection(!stateIsTemperatureOffset);
      _radio_Temperature_OffsetValue.setSelection(stateIsTemperatureOffset);

      enableControls();
   }

   private void saveState() {

      final boolean isCadenceModified = _checkbox_Cadence.getSelection();
      final boolean isElevationModified = _checkbox_Elevation.getSelection();
      final boolean isPulseModified = _checkbox_Pulse.getSelection();
      final boolean isTemperatureModified = _checkbox_Temperature.getSelection();

      // saving the checkboxes state
      _state.put(STATE_IS_ALTITUDE_MODIFIED, isElevationModified);
      _state.put(STATE_IS_PULSE_MODIFIED, isPulseModified);
      _state.put(STATE_IS_CADENCE_MODIFIED, isCadenceModified);
      _state.put(STATE_IS_TEMPERATURE_MODIFIED, isTemperatureModified);

      // saving the spinners value
      if (isElevationModified) {
         _state.put(STATE_ALTITUDE_VALUE, _spinner_ElevationValue.getSelection());
      }
      if (isPulseModified) {
         _state.put(STATE_PULSE_VALUE, _spinner_PulseValue.getSelection());
      }
      if (isCadenceModified) {
         _state.put(STATE_CADENCE_VALUE, _spinner_CadenceValue.getSelection());
      }
      if (isTemperatureModified) {
         _state.put(STATE_TEMPERATURE_VALUE, _spinner_TemperatureValue.getSelection());
      }

      // saving the radio buttons state
      _state.put(STATE_IS_ALTITUDE_OFFSET, _radio_Elevation_OffsetValue.getSelection());
      _state.put(STATE_IS_PULSE_OFFSET, _radio_Pulse_OffsetValue.getSelection());
      _state.put(STATE_IS_CADENCE_OFFSET, _radio_Cadence_OffsetValue.getSelection());
      _state.put(STATE_IS_TEMPERATURE_OFFSET, _radio_Temperature_OffsetValue.getSelection());
   }

}
