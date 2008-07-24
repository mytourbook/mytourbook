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

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.menus.UIElement;

/**
 * Actionhandler for actions in a tour chart
 */
public abstract class TCActionHandler extends AbstractHandler implements IElementUpdater {

	String			fCommandId;
	TourChart		fTourChart;

	private boolean	fisEnabled;
	private boolean	fIsChecked;

	/**
	 * Update the UI enablement state for an action handler
	 */
	public void fireHandlerChanged() {
		fireHandlerChanged(new HandlerEvent(this, true, false));
	}

	public String getCommandId() {
		return fCommandId;
	}

	@Override
	public boolean isEnabled() {
		return fisEnabled;
	}

	/**
	 * Sets the internal check state for the action handler, to update the UI
	 * {@link ICommandService#refreshElements(String, Map)} method must be called
	 * 
	 * @param isEnabled
	 */
	public void setChecked(final boolean isChecked) {

		if (fIsChecked != isChecked) {
			fIsChecked = isChecked;
		}
	}

	/**
	 * Sets the intenal enablement state for the action handler, to update the UI,
	 * {@link #fireHandlerChanged()} must be called
	 * 
	 * @param isEnabled
	 */
	public void setEnabled(final boolean isEnabled) {

		if (fisEnabled != isEnabled) {
			fisEnabled = isEnabled;
		}
	}

	/**
	 * keep the handler activation token
	 * 
	 * @param handlerActivation
	 */
	public void setHandlerActivation(final IHandlerActivation handlerActivation) {

	/*
	 * handlerActivation is currently disabled because it's currently not used
	 */

	//		fHandlerActivation = handlerActivation;
	}

	public void setTourChart(final TourChart tourChart) {
		fTourChart = tourChart;
	}

	@SuppressWarnings("unchecked")
	public void updateElement(final UIElement element, final Map parameters) {

		// update check state in the UI
		element.setChecked(fIsChecked);
	}

}
