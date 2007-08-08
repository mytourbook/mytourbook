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
package net.tourbook.chart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Event;

public class ChartAction {

	private Chart	fChart;

	private Action	fAction;

	private boolean	fIsEnabled;

	public ChartAction(final Chart chartWidget, final int actionMapId, String actionId,
			String commandId, String text, String toolTip, String[] image) {

		fChart = chartWidget;

		fAction = new Action(text) {
			public void run() {
				chartWidget.performZoomAction(actionMapId);
			}

			public void runWithEvent(Event event) {
				chartWidget.performZoomAction(actionMapId);
			}
		};

		fAction.setToolTipText(toolTip);

		if (image != null && image[0] != null) {
			fAction.setImageDescriptor(Activator.getImageDescriptor(image[0]));
		}
		if (image != null && image[1] != null) {
			fAction.setDisabledImageDescriptor(Activator.getImageDescriptor(image[1]));
		}

		// set the command id for key binding
		fAction.setActionDefinitionId(commandId);

		// bind the action handler to the action button/menu
//		final IActionBars actionBars = fChart.getActionBars();
//		if (actionBars != null) {
//			actionBars.setGlobalActionHandler(actionId, fAction);
//		}
	}

	public IAction getAction() {
		return fAction;
	}

	public void setEnabled(boolean isEnabled) {

		// keep enablement state
		fIsEnabled = isEnabled;

		fAction.setEnabled(isEnabled);
	}

	/**
	 * restore the enablement status to the last state
	 */
	public void updateStatus() {
		fAction.setEnabled(fIsEnabled);
	}

}
