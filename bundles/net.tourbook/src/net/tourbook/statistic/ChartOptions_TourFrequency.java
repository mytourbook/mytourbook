/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.statistic;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ChartOptions_TourFrequency implements IStatisticOptions {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;

	/*
	 * UI controls
	 */
	private Spinner					_spinnerAltitude_Interval;
	private Spinner					_spinnerAltitude_Minimum;
	private Spinner					_spinnerAltitude_NumOfBars;

	private Spinner					_spinnerDistance_Interval;
	private Spinner					_spinnerDistance_Minimum;
	private Spinner					_spinnerDistance_NumOfBars;

	private Spinner					_spinnerDuration_Interval;
	private Spinner					_spinnerDuration_Minimum;
	private Spinner					_spinnerDuration_NumOfBars;

	@Override
	public void createUI(final Composite parent) {

		initUI(parent);

		final int leftPadding = 8;

		final Group group = new Group(parent, SWT.NONE);
//		group.setText(Messages.Pref_Graphs_Group_Grid);
		group.setText(Messages.Pref_Statistic_Group_TourFrequency);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(6).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				// spacer
				new Label(group, SWT.NONE);
			}
			{
				/*
				 * Label: Minimum
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_Statistic_Label_Minimum);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(leftPadding, 0)
						.span(2, 1)
						.applyTo(label);
			}
			{
				/*
				 * Label: Interval
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_Statistic_Label_Interval);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(leftPadding, 0)
						.span(2, 1)
						.applyTo(label);
			}
			{
				/*
				 * Label: Number of bars
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_Statistic_Label_NumberOfBars);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(leftPadding, 0)
						.applyTo(label);
			}

			/*
			 * Distance
			 */
			{
				{
					/*
					 * Label: Distance
					 */
					final Label label = new Label(group, SWT.NONE);
					label.setText(Messages.Pref_Statistic_Label_distance);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Minimum
					 */

					// spinner
					_spinnerDistance_Minimum = new Spinner(group, SWT.BORDER);
					_spinnerDistance_Minimum.setMinimum(0);
					_spinnerDistance_Minimum.setMaximum(1000);
					_spinnerDistance_Minimum.setPageIncrement(5);
					_spinnerDistance_Minimum.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerDistance_Minimum.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerDistance_Minimum);

					// unit
					final Label label = new Label(group, SWT.NONE);
					label.setText(UI.UNIT_LABEL_DISTANCE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Interval
					 */

					// spinner
					_spinnerDistance_Interval = new Spinner(group, SWT.BORDER);
					_spinnerDistance_Interval.setMinimum(1);
					_spinnerDistance_Interval.setMaximum(1000);
					_spinnerDistance_Interval.setPageIncrement(5);
					_spinnerDistance_Interval.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerDistance_Interval.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerDistance_Interval);

					// unit
					final Label label = new Label(group, SWT.NONE);
					label.setText(UI.UNIT_LABEL_DISTANCE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Number of bars
					 */

					// spinner
					_spinnerDistance_NumOfBars = new Spinner(group, SWT.BORDER);
					_spinnerDistance_NumOfBars.setMinimum(1);
					_spinnerDistance_NumOfBars.setMaximum(1000);
					_spinnerDistance_NumOfBars.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerDistance_NumOfBars.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerDistance_NumOfBars);
				}
			}

			/*
			 * Label: Altitude
			 */
			{
				{
					/*
					 * Label: Altitude
					 */
					final Label label = new Label(group, SWT.NONE);
					label.setText(Messages.Pref_Statistic_Label_altitude);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Minimum
					 */

					// spinner
					_spinnerAltitude_Minimum = new Spinner(group, SWT.BORDER);
					_spinnerAltitude_Minimum.setMinimum(0);
					_spinnerAltitude_Minimum.setMaximum(9999);
					_spinnerAltitude_Minimum.setPageIncrement(50);
					_spinnerAltitude_Minimum.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerAltitude_Minimum.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerAltitude_Minimum);

					// unit
					final Label label = new Label(group, SWT.NONE);
					label.setText(UI.UNIT_LABEL_ALTITUDE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Interval
					 */

					// spinner
					_spinnerAltitude_Interval = new Spinner(group, SWT.BORDER);
					_spinnerAltitude_Interval.setMinimum(1);
					_spinnerAltitude_Interval.setMaximum(9999);
					_spinnerAltitude_Interval.setPageIncrement(50);
					_spinnerAltitude_Interval.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerAltitude_Interval.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerAltitude_Interval);

					// unit
					final Label label = new Label(group, SWT.NONE);
					label.setText(UI.UNIT_LABEL_ALTITUDE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Number of bars
					 */

					// spinner
					_spinnerAltitude_NumOfBars = new Spinner(group, SWT.BORDER);
					_spinnerAltitude_NumOfBars.setMinimum(1);
					_spinnerAltitude_NumOfBars.setMaximum(1000);
					_spinnerAltitude_NumOfBars.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerAltitude_NumOfBars.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerAltitude_NumOfBars);
				}
			}

			/*
			 * Duration
			 */
			{
				{
					/*
					 * Label: Duration
					 */
					final Label label = new Label(group, SWT.NONE);
					label.setText(Messages.Pref_Statistic_Label_duration);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Minimum
					 */

					// spinner
					_spinnerDuration_Minimum = new Spinner(group, SWT.BORDER);
					_spinnerDuration_Minimum.setMinimum(0);
					_spinnerDuration_Minimum.setMaximum(9999);
					_spinnerDuration_Minimum.setPageIncrement(30);
					_spinnerDuration_Minimum.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerDuration_Minimum.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerDuration_Minimum);

					// unit
					final Label label = new Label(group, SWT.NONE);
					label.setText(Messages.App_Unit_Minute_Small);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Interval
					 */

					// spinner
					_spinnerDuration_Interval = new Spinner(group, SWT.BORDER);
					_spinnerDuration_Interval.setMinimum(1);
					_spinnerDuration_Interval.setMaximum(9999);
					_spinnerDuration_Interval.setPageIncrement(30);
					_spinnerDuration_Interval.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerDuration_Interval.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerDuration_Interval);

					// unit
					final Label label = new Label(group, SWT.NONE);
					label.setText(Messages.App_Unit_Minute_Small);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);
				}
				{
					/*
					 * Number of bars
					 */

					// spinner
					_spinnerDuration_NumOfBars = new Spinner(group, SWT.BORDER);
					_spinnerDuration_NumOfBars.setMinimum(1);
					_spinnerDuration_NumOfBars.setMaximum(1000);
					_spinnerDuration_NumOfBars.addMouseWheelListener(_defaultMouseWheelListener);
					_spinnerDuration_NumOfBars.addSelectionListener(_defaultSelectionListener);
					GridDataFactory.fillDefaults() //
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(leftPadding, 0)
							.applyTo(_spinnerDuration_NumOfBars);
				}
			}

		}
	}

	private void initUI(final Composite parent) {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};
	}

	private void onChangeUI() {

		// update chart async (which is done when a pref store value is modified) that the UI is updated immediately

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				saveState();
			}
		});
	}

	@Override
	public void resetToDefaults() {

		_spinnerAltitude_Interval.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_ALTITUDE_INTERVAL));
		_spinnerAltitude_Minimum.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE));
		_spinnerAltitude_NumOfBars.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_ALTITUDE_NUMBERS));

		_spinnerDistance_Interval.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DISTANCE_INTERVAL));
		_spinnerDistance_Minimum.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE));
		_spinnerDistance_NumOfBars.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DISTANCE_NUMBERS));

		_spinnerDuration_Interval.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DURATION_INTERVAL));
		_spinnerDuration_Minimum.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DURATION_LOW_VALUE));
		_spinnerDuration_NumOfBars.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DURATION_NUMBERS));

		onChangeUI();
	}

	@Override
	public void restoreState() {

		_spinnerAltitude_Interval.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_ALTITUDE_INTERVAL));
		_spinnerAltitude_Minimum.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE));
		_spinnerAltitude_NumOfBars.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_ALTITUDE_NUMBERS));

		_spinnerDistance_Interval.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DISTANCE_INTERVAL));
		_spinnerDistance_Minimum.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE));
		_spinnerDistance_NumOfBars.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DISTANCE_NUMBERS));

		_spinnerDuration_Interval.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DURATION_INTERVAL));
		_spinnerDuration_Minimum.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DURATION_LOW_VALUE));
		_spinnerDuration_NumOfBars.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DURATION_NUMBERS));
	}

	@Override
	public void saveState() {

		_prefStore.setValue(ITourbookPreferences.STAT_ALTITUDE_INTERVAL, //
				_spinnerAltitude_Interval.getSelection());
		_prefStore.setValue(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE, //
				_spinnerAltitude_Minimum.getSelection());
		_prefStore.setValue(ITourbookPreferences.STAT_ALTITUDE_NUMBERS, //
				_spinnerAltitude_NumOfBars.getSelection());

		_prefStore.setValue(ITourbookPreferences.STAT_DISTANCE_INTERVAL, //
				_spinnerDistance_Interval.getSelection());
		_prefStore.setValue(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE, //
				_spinnerDistance_Minimum.getSelection());
		_prefStore.setValue(ITourbookPreferences.STAT_DISTANCE_NUMBERS, //
				_spinnerDistance_NumOfBars.getSelection());

		_prefStore.setValue(ITourbookPreferences.STAT_DURATION_INTERVAL, //
				_spinnerDuration_Interval.getSelection());
		_prefStore.setValue(ITourbookPreferences.STAT_DURATION_LOW_VALUE, //
				_spinnerDuration_Minimum.getSelection());
		_prefStore.setValue(ITourbookPreferences.STAT_DURATION_NUMBERS, //
				_spinnerDuration_NumOfBars.getSelection());
	}
}
