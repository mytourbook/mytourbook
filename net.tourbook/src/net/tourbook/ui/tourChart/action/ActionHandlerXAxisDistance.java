/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import net.tourbook.ui.HandlerUtil;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerXAxisDistance extends TCActionHandler {

	public ActionHandlerXAxisDistance() {
		fCommandId = TourChart.COMMAND_ID_X_AXIS_DISTANCE;
	}

	public Object execute(final ExecutionEvent execEvent) throws ExecutionException {

		final Boolean isItemChecked = HandlerUtil.isItemChecked(execEvent);

		if (isItemChecked != null) {
			fTourChart.onExecuteXAxisDistance(isItemChecked);
		}

		return null;
	}

}
