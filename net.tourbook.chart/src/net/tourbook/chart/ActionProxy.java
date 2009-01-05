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

import org.eclipse.jface.action.Action;

/**
 * A chart can contain an internal action bar or can use the global tour actions. This proxy keeps
 * the state for a chart action.
 */
public class ActionProxy {

	/**
	 * action for this command, when <code>null</code> an action handler is used
	 */
	private Action	fAction;

	private String	fCommandId;

	private boolean	fIsEnabled		= true;
	private boolean	fIsChecked;

	/**
	 * when <code>true</code> this proxy contains a graph action
	 */
	private boolean	fIsGraphAction	= false;

	public ActionProxy(String commandId, Action action) {

		fCommandId = commandId;
		fAction = action;
	}

	public Action getAction() {
		return fAction;
	}

	public String getCommandId() {
		return fCommandId;
	}

	public boolean isChecked() {
		return fIsChecked;
	}

	public boolean isEnabled() {
		return fIsEnabled;
	}

	public boolean isGraphAction() {
		return fIsGraphAction;
	}

	/**
	 * Set check state in the proxy and action/handler,this does not update the UÎ when the handler
	 * is used. To update the UI {@link ICommandService#refreshElements(*)} method must be called
	 * 
	 * @param isChecked
	 */
	public void setChecked(boolean isChecked) {

		// keep check state for this action 
		fIsChecked = isChecked;

		if (fAction != null) {
			fAction.setChecked(isChecked);
		}

		final ActionHandler actionHandler = ActionHandlerManager.getInstance()
				.getActionHandler(fCommandId);
		if (actionHandler != null) {
			actionHandler.setChecked(isChecked);
		}
	}

	/**
	 * Set the enablement state in the proxy and action/handler, this does not update the UÎ when
	 * the handler is used. To update the UI, {@link ActionHandler#fireHandlerChanged()} method must
	 * be called
	 * 
	 * @param isChecked
	 */
	public void setEnabled(boolean isEnabled) {

		// keep enabled state for this action 
		fIsEnabled = isEnabled;

		if (fAction != null) {
			// use action in the action bar
			fAction.setEnabled(isEnabled);
		}

		final ActionHandler actionHandler = ActionHandlerManager.getInstance()
				.getActionHandler(fCommandId);
		if (actionHandler != null) {
			actionHandler.setEnabled(isEnabled);
		}
	}

	public void setIsGraphAction() {
		fIsGraphAction = true;
	}

}
