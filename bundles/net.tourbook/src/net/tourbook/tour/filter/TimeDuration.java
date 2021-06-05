/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

/**
 * UI controls to modify hour/minute/seconds with spinners.
 */
class TimeDuration {

	private boolean					_isSetField;

	private TimeDurationListener	_timeDurationListener;
	private MouseWheelListener		_mouseWheelListener;
	private SelectionAdapter		_selectionListener;

	{
		_selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isSetField) {
					return;
				}

				onUpdateUI();
			}
		};

		_mouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent e) {

				if (_isSetField) {
					return;
				}

				UI.adjustSpinnerValueOnMouseScroll(e);
				onUpdateUI();
			}
		};
	}

	/*
	 * UI controls
	 */
	private Spinner	_spinHours;
	private Spinner	_spinMinutes;
	private Spinner	_spinSeconds;

	/**
	 * @param parent
	 * @param isShowUnit
	 */
	public TimeDuration(final Composite parent, final boolean isShowUnit) {

		int numColumns = 0;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		{
			{
				/*
				 * Spinner: Hour
				 */
				_spinHours = new Spinner(container, SWT.BORDER);
				_spinHours.setMinimum(-1);
				_spinHours.setMaximum(24);

				_spinHours.addSelectionListener(_selectionListener);
				_spinHours.addMouseWheelListener(_mouseWheelListener);

				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinHours);

				numColumns++;
			}

			{
				/*
				 * Spinner: Minute
				 */
				_spinMinutes = new Spinner(container, SWT.BORDER);
				_spinMinutes.setMinimum(-1);
				_spinMinutes.setMaximum(60);

				_spinMinutes.addSelectionListener(_selectionListener);
				_spinMinutes.addMouseWheelListener(_mouseWheelListener);

				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinMinutes);

				numColumns++;
			}
			{
				/*
				 * Spinner: Second
				 */
				_spinSeconds = new Spinner(container, SWT.BORDER);
				_spinSeconds.setMinimum(-1);
				_spinSeconds.setMaximum(60);

				_spinSeconds.addSelectionListener(_selectionListener);
				_spinSeconds.addMouseWheelListener(_mouseWheelListener);

				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinSeconds);

				numColumns++;
			}
			{
				/*
				 * Label: Unit
				 */
				if (isShowUnit) {

					final Label label = new Label(container, SWT.NONE);
					label.setText(Messages.App_Unit_HHMMSS);
					GridDataFactory
							.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(LayoutConstants.getSpacing().x, 0)
							.applyTo(label);

					numColumns++;
				}
			}
		}

		GridLayoutFactory
				.fillDefaults()//
				.numColumns(numColumns)
				.spacing(0, 0)
				.applyTo(container);
	}

	public Object getData() {
		return _spinHours.getData();
	}

	public Object getData(final String key) {
		return _spinHours.getData(key);
	}

	/**
	 * @return Returns time in seconds
	 */
	public int getTime() {

		return (_spinHours.getSelection() * 3600) //
				+ (_spinMinutes.getSelection() * 60)
				+ (_spinSeconds.getSelection());
	}

	private void onUpdateUI() {

		int time = getTime();

		if (time < 0) {
			time = 0;
		}

		setTime(time);

		if (_timeDurationListener != null) {
			_timeDurationListener.timeSelected(time);
		}
	}

	public void setData(final Object data) {
		_spinHours.setData(data);
	}

	public void setData(final String key, final Object data) {
		_spinHours.setData(key, data);
	}

	/**
	 * @param maxHours
	 *            Default is 24
	 */
	public void setMaxHours(final int maxHours) {
		_spinHours.setMaximum(maxHours);
	}

	/**
	 * @param time
	 *            Time in seconds
	 */
	public void setTime(final int time) {

		final int hours = time / 3600;
		final int minutes = (time % 3600) / 60;
		final int seconds = (time % 3600) % 60;

		_isSetField = true;
		{
			_spinHours.setSelection(hours);
			_spinMinutes.setSelection(minutes);
			_spinSeconds.setSelection(seconds);
		}
		_isSetField = false;
	}

	public void setTimeListener(final TimeDurationListener timeDurationListener) {
		_timeDurationListener = timeDurationListener;
	}

}
