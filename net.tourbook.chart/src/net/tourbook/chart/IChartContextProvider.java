/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.action.IMenuManager;

/**
 * this interface will fill the context menus in the chart
 */
public interface IChartContextProvider {

	/**
	 * Will be called when the context menu in a bar chart will be opened
	 * 
	 * @param hoveredBarSerieIndex
	 *            contains the serie index for the hovered bar,<br>
	 *            or <code>-1</code> when a bar is not hovered
	 * @param hoveredBarValueIndex
	 * @param chartWidget
	 */
	public void fillBarChartContextMenu(IMenuManager menuMgr, int hoveredBarSerieIndex, int hoveredBarValueIndex);;

	/**
	 * Fills the context menu for the chart
	 * 
	 * @param menuMgr
	 * @param mouseDownDevPositionY
	 * @param mouseDownDevPositionX
	 */
	public void fillContextMenu(IMenuManager menuMgr, int mouseDownDevPositionX, int mouseDownDevPositionY);

	/**
	 * Fills the context menu for the chart slider
	 * 
	 * @param menuMgr
	 * @param leftSlider
	 * @param rightSlider
	 * @return
	 */
	public void fillXSliderContextMenu(IMenuManager menuMgr, ChartXSlider leftSlider, ChartXSlider rightSlider);

	/**
	 * @return Returns the chart where the context menu is created
	 */
	public Chart getChart();

	/**
	 * @return Returns the left slider
	 */
	public ChartXSlider getLeftSlider();

	/**
	 * @return Returns the right slider
	 */
	public ChartXSlider getRightSlider();

}
