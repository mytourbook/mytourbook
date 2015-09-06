/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

public class ChartMouseEvent {

	public long			eventTime;

	int					type;

	/**
	 * When <code>true</code> the event is done by the receiver, when <code>false</code> the
	 * receiver has done nothing, default is <code>false</code>.
	 * <p>
	 * <b> Because this flag is used by all {@link ChartMouseEvent} listener, setting this value to
	 * <code>false</code> will also disable other listeners. </b>
	 */
	public boolean		isWorked;

	public int			devXMouse;

	public int			devYMouse;
	/**
	 * This cursor is displayed when {@link #isWorked} is <code>true</code>, can be
	 * <code>null</code> which shows the default cursor.
	 */
	public ChartCursor	cursor;

	public int			stateMask;

	/**
	 * When <code>true</code> then dragging sliders will be stopped.
	 */
	public boolean		isDisableSliderDragging;

	@SuppressWarnings("unused")
	private ChartMouseEvent() {}

	public ChartMouseEvent(final int eventType, //
							final long time,
							final int eventMouseX,
							final int eventMouseY) {

		this(eventType, time, eventMouseX, eventMouseY, 0);
	}

	public ChartMouseEvent(	final int eventType,
							final long time,
							final int eventMouseX,
							final int eventMouseY,
							final int eventStateMask) {

		type = eventType;
		eventTime = time;

		devXMouse = eventMouseX;
		devYMouse = eventMouseY;

		stateMask = eventStateMask;
	}

	@Override
	public String toString() {
		return "ChartMouseEvent ["
				+ ("eventTime=" + eventTime + ", ")
				+ ("type=" + type + ", ")
				+ ("isWorked=" + isWorked + ", ")
				+ ("devXMouse=" + devXMouse + ", ")
				+ ("devYMouse=" + devYMouse + ", ")
				+ ("cursor=" + cursor)
				+ "]";
	}
}
