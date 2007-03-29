/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageStatistic extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage, IPropertyChangeListener {

	private static final int	DEFAULT_FIELD_WIDTH	= 40;

	private IntegerFieldEditor	distanceLowValue;
	private IntegerFieldEditor	distanceInterval;
	private IntegerFieldEditor	distanceNumbers;
	private Label				distanceExampleLabel;

	private IntegerFieldEditor	altitudeLowValue;
	private IntegerFieldEditor	altitudeInterval;
	private IntegerFieldEditor	altitudeNumbers;
	private Label				altitudeExampleLabel;

	private IntegerFieldEditor	durationLowValue;
	private IntegerFieldEditor	durationInterval;
	private IntegerFieldEditor	durationNumbers;
	private Label				durationExampleLabel;

	public PrefPageStatistic() {
		super(GRID);
	}

	protected void createFieldEditors() {

		Composite parent = getFieldEditorParent();

		createDistancePreferences(parent);
		UI.setHorizontalSpacer(parent, 2);

		createAltitudePreferences(parent);
		UI.setHorizontalSpacer(parent, 2);

		createDurationPreferences(parent);

		parent.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {}

			public void controlResized(ControlEvent e) {
				computeAllExamples();
			}
		});
	}

	private void createDistancePreferences(Composite parent) {

		Label label;
		GridData gridData;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources
				.getFontRegistry()
				.getBold(JFaceResources.DIALOG_FONT));
		label.setText(Messages.Pref_Statistic_Label_distance);

		// example
		distanceExampleLabel = new Label(parent, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 200;
		distanceExampleLabel.setLayoutData(gridData);

		// low value
		distanceLowValue = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
				Messages.Pref_Statistic_Label_distance_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, distanceLowValue, DEFAULT_FIELD_WIDTH);
		addField(distanceLowValue);

		// interval
		distanceInterval = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DISTANCE_INTERVAL,
				Messages.Pref_Statistic_Label_distance_interval,
				parent,
				4);
		UI.setFieldWidth(parent, distanceInterval, DEFAULT_FIELD_WIDTH);
		addField(distanceInterval);

		// numbers
		distanceNumbers = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DISTANCE_NUMBERS,
				Messages.Pref_Statistic_Label_distance_quantity,
				parent,
				2);
		UI.setFieldWidth(parent, distanceNumbers, DEFAULT_FIELD_WIDTH);
		addField(distanceNumbers);

		distanceLowValue.setPropertyChangeListener(this);
		distanceInterval.setPropertyChangeListener(this);
		distanceNumbers.setPropertyChangeListener(this);
	}

	private void createAltitudePreferences(Composite parent) {

		Label label;
		GridData gridData;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources
				.getFontRegistry()
				.getBold(JFaceResources.DIALOG_FONT));
		gridData = new GridData();
		label.setLayoutData(gridData);
		label.setText(Messages.Pref_Statistic_Label_altitude);

		// example
		altitudeExampleLabel = new Label(parent, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 200;
		altitudeExampleLabel.setLayoutData(gridData);

		// low value
		altitudeLowValue = new IntegerFieldEditor(
				ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
				Messages.Pref_Statistic_Label_altitude_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, altitudeLowValue, DEFAULT_FIELD_WIDTH);
		addField(altitudeLowValue);

		// interval
		altitudeInterval = new IntegerFieldEditor(
				ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
				Messages.Pref_Statistic_Label_altitude_interval,
				parent,
				4);
		UI.setFieldWidth(parent, altitudeInterval, DEFAULT_FIELD_WIDTH);
		addField(altitudeInterval);

		// numbers
		altitudeNumbers = new IntegerFieldEditor(
				ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
				Messages.Pref_Statistic_Label_altitude_quantity,
				parent,
				2);
		altitudeNumbers.setValidRange(2, 99);
		UI.setFieldWidth(parent, altitudeNumbers, DEFAULT_FIELD_WIDTH);
		addField(altitudeNumbers);

		altitudeLowValue.setPropertyChangeListener(this);
		altitudeInterval.setPropertyChangeListener(this);
		altitudeNumbers.setPropertyChangeListener(this);
	}

	private void createDurationPreferences(Composite parent) {

		Label label;
		GridData gridData;

		// title
		label = new Label(parent, SWT.NONE);
		label.setFont(JFaceResources
				.getFontRegistry()
				.getBold(JFaceResources.DIALOG_FONT));
		gridData = new GridData();
		label.setLayoutData(gridData);
		label.setText(Messages.Pref_Statistic_Label_duration);

		// example
		durationExampleLabel = new Label(parent, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 200;
		durationExampleLabel.setLayoutData(gridData);

		// low value
		durationLowValue = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DURATION_LOW_VALUE,
				Messages.Pref_Statistic_Label_duration_low_value,
				parent,
				4);
		UI.setFieldWidth(parent, durationLowValue, DEFAULT_FIELD_WIDTH);
		addField(durationLowValue);

		// interval
		durationInterval = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DURATION_INTERVAL,
				Messages.Pref_Statistic_Label_duration_interval,
				parent,
				4);
		UI.setFieldWidth(parent, durationInterval, DEFAULT_FIELD_WIDTH);
		addField(durationInterval);

		// numbers
		durationNumbers = new IntegerFieldEditor(
				ITourbookPreferences.STAT_DURATION_NUMBERS,
				Messages.Pref_Statistic_Label_duration_quantity,
				parent,
				2);
		UI.setFieldWidth(parent, durationNumbers, DEFAULT_FIELD_WIDTH);
		addField(durationNumbers);

		durationLowValue.setPropertyChangeListener(this);
		durationInterval.setPropertyChangeListener(this);
		durationNumbers.setPropertyChangeListener(this);
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {

		Control control = super.createContents(parent);

		// the example can only be computed when the fields have been
		// initialized
		parent.layout(true);
		computeAllExamples();

		return control;
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	/*
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			if (!altitudeLowValue.isValid()
					|| !distanceLowValue.isValid()
					|| !distanceInterval.isValid()
					|| !distanceNumbers.isValid()
					|| !altitudeLowValue.isValid()
					|| !altitudeInterval.isValid()
					|| !altitudeNumbers.isValid()
					|| !durationLowValue.isValid()
					|| !durationInterval.isValid()
					|| !durationNumbers.isValid()) {
				return;
			}

			computeAllExamples();
		}
	}

	private void computeAllExamples() {

		computeExample(
				distanceExampleLabel,
				distanceLowValue.getIntValue(),
				distanceInterval.getIntValue(),
				distanceNumbers.getIntValue(),
				Messages.Pref_Statistic_Label_km,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		computeExample(
				altitudeExampleLabel,
				altitudeLowValue.getIntValue(),
				altitudeInterval.getIntValue(),
				altitudeNumbers.getIntValue(),
				Messages.Pref_Statistic_Label_m,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		computeExample(
				durationExampleLabel,
				durationLowValue.getIntValue(),
				durationInterval.getIntValue(),
				durationNumbers.getIntValue(),
				Messages.Pref_Statistic_Label_h,
				ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
	}

	/**
	 * show an example of the entered values
	 */
	private void computeExample(Label label,
								int lowValue,
								int interval,
								int numbers,
								String unit,
								int unitType) {

		StringBuilder text = new StringBuilder();
		for (int number = 0; number < numbers; number++) {

			if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {
				text.append(ChartUtil.formatValue((lowValue * 60)
						+ (interval * number * 60), unitType));
			} else {
				text.append(lowValue + (interval * number));
			}

			if (number < numbers - 1) {
				text.append(Messages.Pref_Statistic_Label_separator);
			}
		}
		label.setText(Dialog.shortenText(text.toString() + " " + unit, label)); //$NON-NLS-1$
	}
}
