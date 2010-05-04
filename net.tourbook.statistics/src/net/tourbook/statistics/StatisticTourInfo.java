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
package net.tourbook.statistics;

import net.tourbook.chart.ChartComponents;
import net.tourbook.ui.tourChart.TourInfo;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

public class StatisticTourInfo extends TourInfo {

	public StatisticTourInfo(final Control control) {
		super(control);
	}

	@Override
	public void show(final Point point) {

		/*
		 * delay tooltip because first the bar must be selected that which selectes the new tour
		 */
		_infoControl.getDisplay().timerExec(ChartComponents.BAR_SELECTION_DELAY_TIME + 200, new Runnable() {
			public void run() {
				_tourInfoToolTip.show(point);
			}
		});

	}

}
