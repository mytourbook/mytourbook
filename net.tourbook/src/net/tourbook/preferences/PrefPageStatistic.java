/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.ChartUtil;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
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

public class PrefPageStatistic extends FieldEditorPreferencePage implements IWorkbenchPreferencePage,
		IPropertyChangeListener {

	private IntegerFieldEditor												fEditorDistanceLowValue;
	private IntegerFieldEditor												fEditorDistanceInterval;
	private IntegerFieldEditor												fEditorDistanceNumbers;
	private Label															fLblDistanceExampleLabel;

	private IntegerFieldEditor												fEditorAltitudeLowValue;
	private IntegerFieldEditor												fEditorAltitudeInterval;
	private IntegerFieldEditor												fEditorAltitudeNumbers;
	private Label															fLblAltitudeExampleLabel;

	private IntegerFieldEditor												fEditorDurationLowValue;
	private IntegerFieldEditor												fEditorDurationInterval;
	private IntegerFieldEditor												fEditorDurationNumbers;
	private Label															fLblDurationExampleLabel;

	private org.eclipse.core.runtime.Preferences.IPropertyChangeListener	fPrefChangeListener;

	public PrefPageStatistic() {
		super(GRID);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				if (event.getProperty().equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {
					computeAllExamples();
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void computeAllExamples() {

		fEditorDistanceInterval.setLabelText(NLS.bind(Messages.Pref_Statistic_Label_interval, UI.UNIT_LABEL_DISTANCE));

		computeExample(fLblDistanceExampleLabel,
				fEditorDistanceLowValue.getIntValue(),
				fEditorDistanceInterval.getIntValue(),
				fEditorDistanceNumbers.getIntValue(),
				UI.UNIT_LABEL_DISTANCE,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		fEditorAltitudeInterval.setLabelText(NLS.bind(Messages.Pref_Statistic_Label_interval, UI.UNIT_LABEL_ALTITUDE));
		computeExample(fLblAltitudeExampleLabel,
				fEditorAltitudeLowValue.getIntValue(),
				fEditorAltitudeInterval.getIntValue(),
				fEditorAltitudeNumbers.getIntValue(),
				UI.UNIT_LABEL_ALTITUDE,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		computeExample(fLblDurationExampleLabel,
				fEditorDurationLowValue.getIntValue(),
				fEditorDurationInterval.getIntValue(),
				fEditorDurationNumbers.getIntValue(),
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
				text.append(ChartUtil.formatValue((lowValue * 60) + (interval * number * 60), unitType));
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
	 * @see
	 * 	org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite
	 * 	)
	 */
	@Override
	protected Control createContents(final Composite parent) {

		final Control control = super.createContents(parent);

		addPrefListener();

		// set minimum width
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.hint(300, SWT.DEFAULT)
				.applyTo(fLblDurationExampleLabel);

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
		fLblAltitudeExampleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(200, SWT.DEFAULT)
				.applyTo(fLblAltitudeExampleLabel);

		// low value
		fEditorAltitudeLowValue = new IntegerFieldEditor(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
				Messages.Pref_Statistic_Label_altitude_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, fEditorAltitudeLowValue, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorAltitudeLowValue);

		// interval
		fEditorAltitudeInterval = new IntegerFieldEditor(ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
				Messages.Pref_Statistic_Label_interval,
				parent,
				4);
		UI.setFieldWidth(parent, fEditorAltitudeInterval, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorAltitudeInterval);

		// numbers
		fEditorAltitudeNumbers = new IntegerFieldEditor(ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
				Messages.Pref_Statistic_Label_altitude_quantity,
				parent,
				2);
		fEditorAltitudeNumbers.setValidRange(2, 99);
		UI.setFieldWidth(parent, fEditorAltitudeNumbers, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorAltitudeNumbers);

		fEditorAltitudeLowValue.setPropertyChangeListener(this);
		fEditorAltitudeInterval.setPropertyChangeListener(this);
		fEditorAltitudeNumbers.setPropertyChangeListener(this);
	}

	private void createFieldsDistance(final Composite parent) {

		Label label;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		label.setText(Messages.Pref_Statistic_Label_distance);

		// distance example
		fLblDistanceExampleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(200, SWT.DEFAULT)
				.applyTo(fLblDistanceExampleLabel);

		// low value
		fEditorDistanceLowValue = new IntegerFieldEditor(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
				Messages.Pref_Statistic_Label_distance_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, fEditorDistanceLowValue, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorDistanceLowValue);

		// interval
		fEditorDistanceInterval = new IntegerFieldEditor(ITourbookPreferences.STAT_DISTANCE_INTERVAL,
				Messages.Pref_Statistic_Label_interval,
				parent,
				4);
		UI.setFieldWidth(parent, fEditorDistanceInterval, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorDistanceInterval);

		// numbers
		fEditorDistanceNumbers = new IntegerFieldEditor(ITourbookPreferences.STAT_DISTANCE_NUMBERS,
				Messages.Pref_Statistic_Label_distance_quantity,
				parent,
				2);
		UI.setFieldWidth(parent, fEditorDistanceNumbers, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorDistanceNumbers);

		fEditorDistanceLowValue.setPropertyChangeListener(this);
		fEditorDistanceInterval.setPropertyChangeListener(this);
		fEditorDistanceNumbers.setPropertyChangeListener(this);
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
		fLblDurationExampleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(200, SWT.DEFAULT)
				.applyTo(fLblDurationExampleLabel);

		// low value
		fEditorDurationLowValue = new IntegerFieldEditor(ITourbookPreferences.STAT_DURATION_LOW_VALUE,
				Messages.Pref_Statistic_Label_duration_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, fEditorDurationLowValue, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorDurationLowValue);

		// interval
		fEditorDurationInterval = new IntegerFieldEditor(ITourbookPreferences.STAT_DURATION_INTERVAL,
				Messages.Pref_Statistic_Label_duration_interval,
				parent,
				4);
		UI.setFieldWidth(parent, fEditorDurationInterval, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorDurationInterval);

		// numbers
		fEditorDurationNumbers = new IntegerFieldEditor(ITourbookPreferences.STAT_DURATION_NUMBERS,
				Messages.Pref_Statistic_Label_duration_quantity,
				parent,
				2);
		UI.setFieldWidth(parent, fEditorDurationNumbers, UI.DEFAULT_FIELD_WIDTH);
		addField(fEditorDurationNumbers);

		fEditorDurationLowValue.setPropertyChangeListener(this);
		fEditorDurationInterval.setPropertyChangeListener(this);
		fEditorDurationNumbers.setPropertyChangeListener(this);
	}

	@Override
	public void dispose() {

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

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
			if (!fEditorAltitudeLowValue.isValid()
					|| !fEditorDistanceLowValue.isValid()
					|| !fEditorDistanceInterval.isValid()
					|| !fEditorDistanceNumbers.isValid()
					|| !fEditorAltitudeLowValue.isValid()
					|| !fEditorAltitudeInterval.isValid()
					|| !fEditorAltitudeNumbers.isValid()
					|| !fEditorDurationLowValue.isValid()
					|| !fEditorDurationInterval.isValid()
					|| !fEditorDurationNumbers.isValid()) {
				return;
			}

			computeAllExamples();
		}
	}
}
