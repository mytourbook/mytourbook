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
package net.tourbook.tour;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;

public class ActionHandlerRevertTourEditor extends AbstractHandler {

	private boolean		fisEnabled	= false;
	private TourEditor	fTourEditor;

	public ActionHandlerRevertTourEditor(TourEditor tourEditor) {
		fTourEditor = tourEditor;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fTourEditor.revertTourData();

		return null;
	}

	/**
	 * Update the UI enablement state for the action handler
	 */
	public void fireHandlerChanged() {
		fireHandlerChanged(new HandlerEvent(this, true, false));
	}

	@Override
	public boolean isEnabled() {
		return fisEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		fisEnabled = isEnabled;
	}

}
