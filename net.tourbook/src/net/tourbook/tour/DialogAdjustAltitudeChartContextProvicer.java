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
package net.tourbook.tour;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;

import org.eclipse.jface.action.IMenuManager;

public class DialogAdjustAltitudeChartContextProvicer implements IChartContextProvider {

	private DialogAdjustAltitude	fDialogAdjustAltitude;

	private ActionCreateSplinePoint	fActionNewSplinePoint;

	public DialogAdjustAltitudeChartContextProvicer(final DialogAdjustAltitude dialogAdjustAltitude) {

		fDialogAdjustAltitude = dialogAdjustAltitude;
		
		fActionNewSplinePoint = new ActionCreateSplinePoint(dialogAdjustAltitude);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {

		fActionNewSplinePoint.fMouseDownDevPositionX = mouseDownDevPositionX;
		fActionNewSplinePoint.fMouseDownDevPositionY = mouseDownDevPositionY;

		fActionNewSplinePoint.setEnabled(fDialogAdjustAltitude.isActionEnabledCreateSplinePoint(mouseDownDevPositionX,
				mouseDownDevPositionY));
		
		menuMgr.add(fActionNewSplinePoint);

	}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {}

	public Chart getChart() {
		return null;
	}

	public ChartXSlider getLeftSlider() {
		return null;
	}

	public ChartXSlider getRightSlider() {
		return null;
	}

}
