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

public class ChartKeyEvent {

	int				type;

	/**
	 * When <code>true</code> the event is done by the receiver, when <code>false</code> the
	 * receiver has done nothing, default is <code>false</code>.
	 * <p>
	 * <b> Because this flag is used by all {@link ChartKeyEvent} listener, setting this value to
	 * <code>false</code> will also disable other listeners. </b>
	 */
	public boolean	isWorked;

	public int		stateMask;

	public int		keyCode;

	@SuppressWarnings("unused")
	private ChartKeyEvent() {}

	ChartKeyEvent(final int type, final int keyCode, final int stateMask) {

		this.type = type;
		this.keyCode = keyCode;
		this.stateMask = stateMask;
	}

	@Override
	public String toString() {
		return "ChartMouseEvent [" + ("type=" + type + ", ") + ("isWorked=" + isWorked + ", ") + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}
}
