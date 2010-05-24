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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.Util;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageStatisticTourFrequency extends FieldEditorPreferencePage implements IWorkbenchPreferencePage,
		IPropertyChangeListener {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private IPropertyChangeListener	_prefChangeListener;

	/*
	 * UI constrols
	 */
	private IntegerFieldEditor		_editorDistanceLowValue;
	private IntegerFieldEditor		_editorDistanceInterval;
	private IntegerFieldEditor		_editorDistanceNumbers;
	private Label					_lblDistanceExampleLabel;

	private IntegerFieldEditor		_editorAltitudeLowValue;
	private IntegerFieldEditor		_editorAltitudeInterval;
	private IntegerFieldEditor		_editorAltitudeNumbers;
	private Label					_lblAltitudeExampleLabel;

	private IntegerFieldEditor		_editorDurationLowValue;
	private IntegerFieldEditor		_editorDurationInterval;
	private IntegerFieldEditor		_editorDurationNumbers;
	private Label					_lblDurationExampleLabel;

	public PrefPageStatisticTourFrequency() {
		super(GRID);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				if (event.getProperty().equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {
					computeAllExamples();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void computeAllExamples() {

		_editorDistanceInterval.setLabelText(NLS.bind(Messages.Pref_Statistic_Label_interval, UI.UNIT_LABEL_DISTANCE));

		computeExample(
				_lblDistanceExampleLabel,
				_editorDistanceLowValue.getIntValue(),
				_editorDistanceInterval.getIntValue(),
				_editorDistanceNumbers.getIntValue(),
				UI.UNIT_LABEL_DISTANCE,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		_editorAltitudeInterval.setLabelText(NLS.bind(Messages.Pref_Statistic_Label_interval, UI.UNIT_LABEL_ALTITUDE));
		computeExample(
				_lblAltitudeExampleLabel,
				_editorAltitudeLowValue.getIntValue(),
				_editorAltitudeInterval.getIntValue(),
				_editorAltitudeNumbers.getIntValue(),
				UI.UNIT_LABEL_ALTITUDE,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		computeExample(
				_lblDurationExampleLabel,
				_editorDurationLowValue.getIntValue(),
				_editorDurationInterval.getIntValue(),
				_editorDurationNumbers.getIntValue(),
				Messages.Pref_Statistic_Label_h,
				ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
	}

	/**
	 * show an example of the entered values
	 */
	private void computeExample(final Label label,
								final int lowValue,
								final int interval,
								final int numbers,
								final String unit,
								final int unitType) {

		final StringBuilder text = new StringBuilder();
		for (int number = 0; number < numbers; number++) {

			if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {
				text.append(Util.formatValue((lowValue * 60) + (interval * number * 60), unitType));
			} else {
				text.append(lowValue + (interval * number));
			}

			if (number < numbers - 1) {
				text.append(Messages.Pref_Statistic_Label_separator);
			}
		}
		label.setText(Dialog.shortenText(text.toString() + " " + unit, label)); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite
	 *      )
	 */
	@Override
	protected Control createContents(final Composite parent) {

		final Control control = super.createContents(parent);

		addPrefListener();

		// set minimum width
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.hint(300, SWT.DEFAULT)
				.applyTo(_lblDurationExampleLabel);

		parent.layout(true);

		// the example can only be computed when the fields have been initialized
		computeAllExamples();

		return control;
	}

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		createFieldsDistance(parent);
		createFieldsAltitude(parent);
		createFieldsDuration(parent);

		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				computeAllExamples();
			}
		});
	}

	private void createFieldsAltitude(final Composite parent) {

		Label label;
		GridData gridData;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		gridData = new GridData();
		label.setLayoutData(gridData);
		label.setText(Messages.Pref_Statistic_Label_altitude);

		// altitude example
		_lblAltitudeExampleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(200, SWT.DEFAULT)
				.applyTo(_lblAltitudeExampleLabel);

		// low value
		_editorAltitudeLowValue = new IntegerFieldEditor(
				ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
				Messages.Pref_Statistic_Label_altitude_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, _editorAltitudeLowValue, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorAltitudeLowValue);

		// interval
		_editorAltitudeInterval = new IntegerFieldEditor(
				ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
				Messages.Pref_Statistic_Label_interval,
				parent,
				4);
		UI.setFieldWidth(parent, _editorAltitudeInterval, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorAltitudeInterval);

		// numbers
		_editorAltitudeNumbers = new IntegerFieldEditor(
				ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
				Messages.Pref_Statistic_Label_altitude_quantity,
				parent,
				2);
		_editorAltitudeNumbers.setValidRange(2, 99);
		UI.setFieldWidth(parent, _editorAltitudeNumbers, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorAltitudeNumbers);

		_editorAltitudeLowValue.setPropertyChangeListener(this);
		_editorAltitudeInterval.setPropertyChangeListener(this);
		_editorAltitudeNumbers.setPropertyChangeListener(this);
	}

	private void createFieldsDistance(final Composite parent) {

		Label label;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		label.setText(Messages.Pref_Statistic_Label_distance);

		// distance example
		_lblDistanceExampleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(200, SWT.DEFAULT)
				.applyTo(_lblDistanceExampleLabel);

		// low value
		_editorDistanceLowValue = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
				Messages.Pref_Statistic_Label_distance_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, _editorDistanceLowValue, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorDistanceLowValue);

		// interval
		_editorDistanceInterval = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DISTANCE_INTERVAL,
				Messages.Pref_Statistic_Label_interval,
				parent,
				4);
		UI.setFieldWidth(parent, _editorDistanceInterval, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorDistanceInterval);

		// numbers
		_editorDistanceNumbers = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DISTANCE_NUMBERS,
				Messages.Pref_Statistic_Label_distance_quantity,
				parent,
				2);
		UI.setFieldWidth(parent, _editorDistanceNumbers, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorDistanceNumbers);

		_editorDistanceLowValue.setPropertyChangeListener(this);
		_editorDistanceInterval.setPropertyChangeListener(this);
		_editorDistanceNumbers.setPropertyChangeListener(this);
	}

	private void createFieldsDuration(final Composite parent) {

		Label label;
		GridData gridData;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		gridData = new GridData();
		label.setLayoutData(gridData);
		label.setText(Messages.Pref_Statistic_Label_duration);

		// duration example
		_lblDurationExampleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(200, SWT.DEFAULT)
				.applyTo(_lblDurationExampleLabel);

		// low value
		_editorDurationLowValue = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DURATION_LOW_VALUE,
				Messages.Pref_Statistic_Label_duration_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, _editorDurationLowValue, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorDurationLowValue);

		// interval
		_editorDurationInterval = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DURATION_INTERVAL,
				Messages.Pref_Statistic_Label_duration_interval,
				parent,
				4);
		UI.setFieldWidth(parent, _editorDurationInterval, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorDurationInterval);

		// numbers
		_editorDurationNumbers = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DURATION_NUMBERS,
				Messages.Pref_Statistic_Label_duration_quantity,
				parent,
				2);
		UI.setFieldWidth(parent, _editorDurationNumbers, UI.DEFAULT_FIELD_WIDTH);
		addField(_editorDurationNumbers);

		_editorDurationLowValue.setPropertyChangeListener(this);
		_editorDurationInterval.setPropertyChangeListener(this);
		_editorDurationNumbers.setPropertyChangeListener(this);
	}

	@Override
	public void dispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	/*
	 * @see
	 * org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util
	 * .PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			if (!_editorAltitudeLowValue.isValid()
					|| !_editorDistanceLowValue.isValid()
					|| !_editorDistanceInterval.isValid()
					|| !_editorDistanceNumbers.isValid()
					|| !_editorAltitudeLowValue.isValid()
					|| !_editorAltitudeInterval.isValid()
					|| !_editorAltitudeNumbers.isValid()
					|| !_editorDurationLowValue.isValid()
					|| !_editorDurationInterval.isValid()
					|| !_editorDurationNumbers.isValid()) {
				return;
			}

			computeAllExamples();
		}
	}
}
