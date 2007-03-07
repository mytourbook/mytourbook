/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import org.eclipse.jface.action.Action;

public class ChartAction extends Action {

	private Chart	chartWidget;

	private int			actionId;

	public ChartAction(Chart chartWidget, int actionId, String text,
			String toolTip, String[] image) {

		super(text);
		setToolTipText(toolTip);

		this.chartWidget = chartWidget;
		this.actionId = actionId;

		if (image != null && image[0] != null) {
			setImageDescriptor(Activator.getImageDescriptor(image[0]));
		}
		if (image != null && image[1] != null) {
			setDisabledImageDescriptor(Activator.getImageDescriptor(image[1]));
		}
	}

	public void run() {
		chartWidget.performZoomAction(actionId);
	}
}
