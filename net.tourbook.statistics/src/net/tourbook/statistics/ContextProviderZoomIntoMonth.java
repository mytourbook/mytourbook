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
/**
 * 
 */
package net.tourbook.statistics;

import net.tourbook.chart.ChartContextProvider;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;

class ContextProviderZoomIntoMonth implements ChartContextProvider {

	/**
	 * 
	 */
	private Chart			fChart;
	private IMonthProvider	fMonthProvider;

	private class ZoomMonth extends Action {

		public ZoomMonth(String text) {
			super(text);
		}

		public void run() {
			fChart.zoomWithParts(12, fMonthProvider.getSelectedMonth() - 1);
		}
	}

	public ContextProviderZoomIntoMonth(Chart chart, IMonthProvider monthProvider) {
		fChart = chart;
		fMonthProvider = monthProvider;
	}

	public void fillBarChartContextMenu(IMenuManager menuMgr) {
		menuMgr.add(new ZoomMonth(Messages.ACTION_ZOOM_INTO_MONTH));
	}

	public void fillXSliderContextMenu(	IMenuManager menuMgr,
										ChartXSlider leftSlider,
										ChartXSlider rightSlider) {}

}
