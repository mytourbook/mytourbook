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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

/**
 * UI controls to modify hour/minute/seconds with spinners.
 */
class TimeDuration {

	private MouseWheelListener _mouseWheelListener;
	{
		_mouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};
	}

	/*
	 * UI controls
	 */
	private Spinner	_spinHours;
	private Spinner	_spinMinutes;
//	private Spinner	_spinSeconds;

	public TimeDuration(final Composite parent, final TourFilterProperty filterProperty, final int fieldNo) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).applyTo(container);
		{
			/*
			 * hour
			 */
			_spinHours = new Spinner(container, SWT.BORDER);
			_spinHours.setData(filterProperty);
			_spinHours.setData(SlideoutTourFilter.FIELD_NO, fieldNo);
			_spinHours.setMinimum(-1);
			_spinHours.setMaximum(999);
			_spinHours.setToolTipText(Messages.Tour_Editor_Label_Hours_Tooltip);

			_spinHours.addMouseWheelListener(_mouseWheelListener);

			GridDataFactory
					.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinHours);

			/*
			 * minute
			 */
			_spinMinutes = new Spinner(container, SWT.BORDER);
			_spinMinutes.setData(filterProperty);
			_spinMinutes.setData(SlideoutTourFilter.FIELD_NO, fieldNo);
			_spinMinutes.setMinimum(-1);
			_spinMinutes.setMaximum(60);
			_spinMinutes.setToolTipText(Messages.Tour_Editor_Label_Minutes_Tooltip);

			_spinMinutes.addMouseWheelListener(_mouseWheelListener);

			GridDataFactory
					.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinMinutes);

//			/*
//			 * seconds
//			 */
//			_spinSeconds = new Spinner(container, SWT.BORDER);
//			GridDataFactory
//					.fillDefaults()//
//					.align(SWT.BEGINNING, SWT.CENTER)
//					.applyTo(_spinSeconds);
//			_spinSeconds.setMinimum(-1);
//			_spinSeconds.setMaximum(60);
//			_spinSeconds.setToolTipText(Messages.Tour_Editor_Label_Seconds_Tooltip);
//
//			_spinSeconds.addMouseWheelListener(_mouseWheelListener);
		}
	}

	public void addSelectionListener(final SelectionListener selectionListener) {

		_spinHours.addSelectionListener(selectionListener);
		_spinMinutes.addSelectionListener(selectionListener);
	}

	/**
	 * @return Returns time in seconds
	 */
	public int getTime() {

		return (_spinHours.getSelection() * 3600) //
				+ (_spinMinutes.getSelection() * 60)
//				+ _spinSeconds.getSelection()
		;
	}

	/**
	 * @param time
	 *            Time in seconds
	 */
	public void setTime(final int time) {

		final int hours = time / 3600;
		final int minutes = (time % 3600) / 60;
//		final int seconds = (time % 3600) % 60;

		_spinHours.setSelection(hours);
		_spinMinutes.setSelection(minutes);
//		_spinSeconds.setSelection(seconds);
	}

}
